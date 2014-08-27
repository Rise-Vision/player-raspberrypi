package com.risevision.risecache.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;

import com.risevision.risecache.Config;
import com.risevision.risecache.Log;
import com.risevision.risecache.cache.FileInfo;
import com.risevision.risecache.cache.FileRequests;
import com.risevision.risecache.cache.FileUtils;
import com.risevision.risecache.jobs.CheckExpiredJob;
import com.risevision.risecache.server.HttpConstants;
import com.risevision.risecache.server.HttpUtils;

public class DownloadManager {
	
	public static String getFileName(String fullUrl) {

		if (fullUrl != null) 
			return FileUtils.intToHex8(fullUrl.hashCode());
		else
			return null;
		
	}

	public static void onDownloadComplete(String fullUrl, ArrayList<String> headers) {
		String localFileName = getFileName(fullUrl);	
		FileUtils.moveDownloadedFileToCache(localFileName, headers);
	}

	public static String getFileNameIfFileExists(String fileUrl) {
		return FileUtils.getCurrentDataFileName(getFileName(fileUrl));
	}

	public static boolean download(String fileUrl, PrintStream ps, FileInfo fileInfo, boolean isGetRequest) {
		
		BufferedInputStream in = null;
		RandomAccessFile out = null;
		boolean fileDownloaded = false;
		boolean needToTryAgain = false;
		ArrayList<String> headers = new ArrayList<>();
		
		try {
			String destPath = Config.downloadPath + File.separator + getFileName(fileUrl);
			
			URL url = new java.net.URL(Config.debugUrl == null ? fileUrl : Config.debugUrl);
			HttpURLConnection connection;
			
			if (Config.useProxy) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Config.proxyAddr, Config.proxyPort));
				connection = (HttpURLConnection) url.openConnection(proxy);
			} else {
				connection = (HttpURLConnection) url.openConnection();
			}
			
			if (!isGetRequest)
				connection.setRequestMethod("HEAD");

			//if file was downloaded before, then add request headers to check if it is modified
			if (fileInfo != null) {
				boolean etagExist = fileInfo.etag != null && !fileInfo.etag.isEmpty();
				if (etagExist) {
					//connection.setRequestProperty(HttpConstants.HEADER_ETAG, fileInfo.etag);
					connection.setRequestProperty(HttpConstants.HEADER_IF_NONE_MATCH, fileInfo.etag);
				} else {
					//connection.setRequestProperty(HttpConstants.HEADER_LAST_MODIFIED, HttpUtils.formatDate(fileInfo.lastModified));
					connection.setRequestProperty(HttpConstants.HEADER_IF_MODIFIED_SINCE, HttpUtils.formatDate(fileInfo.lastModified));
				}
			}
			
			//log all headers
			StringBuilder allHeaders = new StringBuilder();
			for (String key: connection.getHeaderFields().keySet()) {
				allHeaders.append("\r\n" + key +": "+connection.getHeaderField(key));
				//Log.info(key +": "+connection.getHeaderField(key));
			}
			//Log.info("Response headers for URL " + fileUrl + allHeaders);

			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				Log.info("Download cancelled. Response code " + responseCode + " received for URL " + fileUrl);
				HttpUtils.printHeader_ResponseCode(HttpConstants.HTTP_NOT_FOUND_TEXT + " Server respeonse: " + connection.getResponseMessage(), ps, true);
				return false;
			}
			
			HttpUtils.printHeadersCommon(HttpConstants.HTTP_OK_TEXT, ps);
			String etag = connection.getHeaderField(HttpConstants.HEADER_ETAG);
			if (etag != null) {
				headers.add(HttpUtils.printHeader_ETag(etag, ps, false));
			}
			headers.add(HttpUtils.printHeader_ContentType(connection.getContentType(), ps, false));
			int contentLength = connection.getContentLength();
			headers.add(HttpUtils.printHeader_ContentLength(contentLength, ps, true));
						
			headers.add(HttpConstants.HEADER_FILE_URL + ": " + fileUrl);
			
			if (isGetRequest) {

				//Initiate Expired job
				CheckExpiredJob.start();
				
				in = new BufferedInputStream(connection.getInputStream());
				
				//makes sure out file does not exist. It can exist if: 
				// 1) file is requested twice at ~ same time in which case there is no need to save file to the disk because it's being saved by the first request
				// 2) as a result of incomplete (interrupted) download in which case it has to be deleted and re-downloaded
				File outFile = new File(destPath);
				if (outFile.exists()) {
					try {
						outFile.delete();
					} catch (Exception e) {
						// TODO: handle exception
					} 
				}
					
				if (!outFile.exists()) {
					try {
						out = new RandomAccessFile(destPath, "rw");
					} catch (Exception e) {
					}
				}
		
				byte data[] = new byte[4096];
				int numRead;
				while ((numRead = in.read(data)) >= 0) {
					if (out != null)
						out.write(data, 0, numRead); // save to file
					if (ps != null)
						ps.write(data, 0, numRead); // output to the requested stream
				}
				
				if (out != null) {
					if (contentLength != out.length() && ps != null) {
						Log.info("Download failed - restarting... Length=" + out.length() + ". URL=" + fileUrl);
						//There are situations when ps.write blocks download and causes connection timeout
						//this happens when browser buffers first 20MB of the next video and then waits.
						//The work around is to restart download with ps=null
						needToTryAgain = true;
					} else {
						fileDownloaded = true;
						Log.info("Download complete. Length=" + out.length() + ". URL=" + fileUrl);
					}
				}				
			}

		} catch (ConnectException e) {
			e.printStackTrace();
			Log.error(e.getMessage());
			HttpUtils.printHeader_ResponseCode(HttpConstants.HTTP_CONNECTION_REFUSED_TEXT, ps, true);
		} catch (Exception e) {
			e.printStackTrace();
			Log.error(e.getMessage());
			HttpUtils.printHeader_ResponseCode(HttpConstants.HTTP_INTERNAL_ERROR_TEXT, ps, true);
		} finally {
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (fileDownloaded) {
			DownloadManager.onDownloadComplete(fileUrl, headers);
		} else if (needToTryAgain) {
			DownloadWorker.downloadFileIfModified(fileUrl);
		}
		
		return fileDownloaded;

	}


	public static void downloadFileIfModified(String fileUrl) {
		
		boolean okToDownload = FileRequests.beginRequest(fileUrl);
		
		if (okToDownload) {
			try {
				String name = getFileName(fileUrl);
				FileInfo fileInfo = FileUtils.getFileInfo(name);
				download(fileUrl, null, fileInfo, true);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileRequests.endRequest(fileUrl);
			}
		}
	}

	public synchronized void run() {
		
	}


}
