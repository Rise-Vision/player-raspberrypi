package com.risevision.risecache.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.risevision.risecache.Config;
import com.risevision.risecache.Globals;
import com.risevision.risecache.cache.FileRequests;
import com.risevision.risecache.cache.FileUtils;
import com.risevision.risecache.downloader.DownloadManager;
import com.risevision.risecache.downloader.DownloadWorker;

class Worker extends WebServer implements HttpConstants, Runnable {
	final static int BUF_SIZE = 2048;

	static final byte[] EOL = { (byte) '\r', (byte) '\n' };

	/* buffer to use for requests */
	byte[] buf;
	/* Socket to client we're handling */
	private Socket s;

	public String httpErrorCode = HTTP_NOT_FOUND_TEXT;

	Worker() {
		buf = new byte[BUF_SIZE];
		s = null;
	}

	synchronized void setSocket(Socket s) {
		this.s = s;
		notify();
	}

	synchronized void syncedNotify() {
		//wait() and notify() have to be synchronized with the same object
		notify();
	}

	public synchronized void run() {
		while (true) {
			if (s == null) {
				/* nothing to do */
				try {
					wait();
				} catch (InterruptedException e) {
					/* should not happen */
					continue;
				}
			}
			try {
				handleClient();
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*
			 * go back in wait queue if there's fewer than numHandler
			 * connections.
			 */
			s = null;
			Vector<Worker> pool = WebServer.threads;
			synchronized (pool) {
				if (pool.size() >= WebServer.workers) {
					/* too many threads, exit this one */
					return;
				} else {
					pool.addElement(this);
				}
			}
		}
	}

	  /**
	   * @return the given string with any %20 sequences decoded
	   */
/*	  private String decodeWebChars(String line) {
	    // GET /dart/test%C3%BCuuuu/swipe.html HTTP/1.1
	    //   ==>
	    // GET /dart/testüuuuu/swipe.html HTTP/1.1

	    byte[] bytes = line.getBytes();
	    ByteArrayOutputStream out = new ByteArrayOutputStream();

	    for (int i = 0; i < bytes.length; i++) {
	      if (bytes[i] == '%' && (i + 2 < bytes.length)) {
	        int val = hex2Int((char) bytes[i + 1], (char) bytes[i + 2]);

	        out.write(val);

	        i += 2;
	      } else {
	        out.write(bytes[i]);
	      }
	    }

	    return new String(out.toByteArray(), StandardCharsets.UTF_8);
	    
	  }*/
	  

	  private HttpHeader parseHeader(InputStream inputStream) throws IOException {
	    HttpHeader header = new HttpHeader();
	    
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);  
        BufferedReader in =  new BufferedReader(inputStreamReader);//pas conseille pour xaland ne supporte pas buffereader  

	    String line = in.readLine();

	    if (line == null) {
	      return null;
	    }

	    // GET /index.html HTTP/1.1
	    String[] strs = line.split(" ");

	    if (strs.length > 2) {
	      header.method = strs[0];
	      header.file = strs[1]; //decodeWebChars(strs[1]);
	      header.version = strs[2];

	      if (header.file != null) {
	      //  header.file = URLDecoder.decode(header.file, CHARSET_ISO_8859_1);

	      //  header.parseGetParams();
	      }
	    }
	    
	    line = in.readLine();

	    while (line != null) {
	      if (line.isEmpty()) {
	        break;
	      }

	      int index = line.indexOf(':');

	      if (index == -1) {
	        header.headers.put(line, "");
	      } else {
	        header.headers.put(line.substring(0, index), line.substring(index + 1).trim());
	      }

	      line = in.readLine();
	    }
	    return header;
	  }
	  
	void handleClient() throws IOException {
		//log("handleClient()");
		
		// Fetch response  
        InputStream inputStream = s.getInputStream();
        PrintStream ps = new PrintStream(s.getOutputStream());

		/*
		 * we will only block in read for this many milliseconds before we fail
		 * with java.io.InterruptedIOException, at which point we will abandon
		 * the connection.
		 */
		s.setSoTimeout(WebServer.timeout);
		s.setTcpNoDelay(true);
        HttpHeader header = parseHeader(inputStream);

    	/*
		 * We only support HTTP GET/HEAD, and don't support any fancy HTTP
		 * options, so we're only interested really in the first line.
		 */	
        try {
	        if (header == null) {
	        	//error parsing header, close socket
	        	s.close();
	        	return;
	        } else if (HttpHeader.METHOD_GET.equals(header.method) || HttpHeader.METHOD_HEAD.equals(header.method)) {
	        	
	        		/* are we doing a GET or just a HEAD */
	        		boolean isGetRequest = false;
	        		if (HttpHeader.METHOD_GET.equals(header.method)) isGetRequest = true;  
	    			    			
	    			
	    			String fullUrl = header.file;
	    			String fname = fullUrl;
	    			if (fname.startsWith("/")) {
	    				fname = fname.substring(1);
	    			}
	
	    			Map<String, String> queryMap = getQueryMap(fullUrl);
	    			String fileUrl = queryMap.get("url");
	    			fileUrl = fileUrl == null ? "" : URLDecoder.decode(fileUrl, "UTF-8").trim();
	    			
	    			//encode spaces in URL
	    			fileUrl = fileUrl.replace(" ", "%20");
	    			
	    			boolean isCrossDomain = "crossdomain.xml".equalsIgnoreCase(fname);
	    			boolean isPing = fname.startsWith("ping");
	    			boolean isShutdown = fname.startsWith("shutdown");
	    			boolean isVersion = fname.startsWith("version");
	    			boolean isLocalName = fname.startsWith("localname") && !fileUrl.isEmpty(); // convert file URL to local file name
	    			boolean isVideo = fname.startsWith("video") && !fileUrl.isEmpty();
	    			boolean isFile = fname.startsWith("?") && !fileUrl.isEmpty();  //this is to make it compatible with Image Proxy
	    			
	    			//do not serve video on base port.
	//    			if ((isVideo || isFile) && s.getLocalPort() == Config.basePort) {
	//    				int redirectPort = ServerPorts.getRedirectPort(fileUrl);
	//    				if (redirectPort != -1) {
	//    					System.out.println("Redirected to port " + redirectPort);
	//    					HttpUtils.printHeader_ResponseRedirect("http://localhost:" + redirectPort + fullUrl, ps);
	//    					return;
	//    				}
	//    			}
	    			
	    			if (isPing) {
	    				String callback = queryMap.get("callback");
	    				if (callback != null && !callback.isEmpty()) {
	    					String responseText = callback + "();";
	    					HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_JAVASCRIPT, responseText.length());
	    					ps.print(responseText);
	    				} else {
	    					HttpUtils.printHeader_ResponseCode(HTTP_BAD_REQUEST_TEXT, ps, true);
	    				}
	    			} else if (isShutdown) {
	    				log("shutdown command received");
	    				System.exit(0);
	    			} else if (isVersion) {
	    				HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_TEXT_PLAIN, Globals.APPLICATION_VERSION.length());
	    				ps.print(Globals.APPLICATION_VERSION);
	    			} else if (isCrossDomain) {
	    				HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_TEXT_XML, Globals.CROSSDOMAIN_XML.length());
	    				ps.print(Globals.CROSSDOMAIN_XML);
	//    				File targ = new File(Config.appPath, "crossdomain.xml");
	//    				HttpUtils.printHeaders(targ, ps);
	//    				sendFile(targ, ps);
	    			} else if (isLocalName) {
	    				HttpUtils.printHeader_ResponseCode(HTTP_OK_TEXT, ps, true);
	    				ps.print(DownloadManager.getFileName(fileUrl));
	    			} else if (isVideo || isFile) {
	    				log("video command received");
	    				processRequest_GetVideo(fileUrl, ps, isGetRequest, header);
	    			} else {
	    				HttpUtils.printHeader_ResponseCode(HTTP_BAD_REQUEST_TEXT, ps, true);
	    			}
	    			s.close();
	
	        	
	        }
	        else {
	        	ps.print("HTTP/1.1 " + HTTP_BAD_METHOD + " unsupported method type: " + header.method);
				ps.print(header.method);
				ps.write(EOL);
				ps.flush();
				s.close();
				return;
	        }
        } catch (Exception e) {
        	safeClose(s);
        }

	}

  private void safeClose(Socket socket) {
	    try {
	      socket.close();
	    } catch (IOException e) {

	    }
	  }

	private void processRequest_GetVideo(String fileUrl, PrintStream ps, boolean isGetRequest, HttpHeader header) throws IOException {
		
		try {
			 
			//log("processRequest_GetVideo() URL=" + fileUrl);
			ServerPorts.setConnected(s.getLocalPort(), fileUrl);
			
			String fileName = DownloadManager.getFileNameIfFileExists(fileUrl);
			boolean fileExists = fileName != null && !fileName.isEmpty();

			
			//return file if it was downloaded otherwise start download
			if (fileExists) {
				File file = new File(Config.cachePath, fileName);
				
				FileUtils.updateLastAccessed(file);
				
				File headerFile = new File(Config.cachePath, FileUtils.dataFileNameToHeaderFileName(fileName));
				ArrayList<String> fileHeaders = FileUtils.loadHeaders(headerFile);
				if (isGetRequest) {
					sendFile(file, ps, header, fileHeaders);
				}
				else {
					HttpUtils.printHeaders(file, fileHeaders, ps, HTTP_OK_TEXT);
					ps.write(EOL);
				}
				
				//check if file is modified async
				DownloadWorker.downloadFileIfModified(fileUrl);
				
			} else {
				try {
					if (DownloadManager.download(fileUrl, ps, null, isGetRequest))
						// simply record time when file was downloaded;
						// no need to check if modified before download.
						if (FileRequests.beginRequest(fileUrl))
							FileRequests.endRequest(fileUrl);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
		} finally {
			ServerPorts.setDisconnected(s.getLocalPort());
		}
		//log("processRequest_GetVideo()-end URL=" + fileUrl);
				
	}
	
	private byte[] readRangeData(File file, List<int[]> ranges) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();

	    RandomAccessFile f = new RandomAccessFile(file, "r");

	    int maxLength = (int) f.length() - 1;

	    for (int[] range : ranges) {
	      if (range[1] > maxLength || range[1] == -1) {
	        range[1] = maxLength;
	      }

	      if (range[0] >= range[1]) {
	        continue;
	      }

	      f.seek(range[0]);

	      int count = range[1] - range[0] + 1;

	      byte[] temp = new byte[count];

	      f.readFully(temp);

	      out.write(temp);
	    }

	    return out.toByteArray();
	  }
	
	void sendFile(File targ, PrintStream ps, HttpHeader header, ArrayList<String> fileHeaders) throws IOException {
		//log("sendFile() file=" + targ.getName());
		InputStream is = null;
		
		// Indicate that we support requesting a subset of the document.
		fileHeaders.add(HEADER_ACCEPT_RANGES + ": " + "bytes");	
		if (targ.isDirectory()) {
			HttpUtils.printHeaders(targ, fileHeaders, ps, HTTP_OK_TEXT);
			ps.write(EOL);
			listDirectory(targ, ps);
			return;
		} else {
			List<int[]> ranges = header.getRanges();
			if (ranges != null) {
				byte[] rangeData = readRangeData(targ, ranges);
				is = new ByteArrayInputStream(rangeData);  
				// Content-Range: bytes X-Y/Z
				int[] range = ranges.get(0);
				
				//remove line with HEADER_CONTENT_LENGTH (this was added upon loading headers from file
				for (String headerLine : fileHeaders) {
					if(headerLine.startsWith(HEADER_CONTENT_LENGTH)) {
						fileHeaders.remove(headerLine);
						break;
					}
				}
				fileHeaders.add(HEADER_CONTENT_LENGTH + " : " + rangeData.length);
				log("ranges header received. " + HEADER_CONTENT_RANGE + ": " + "bytes " + range[0] + "-" + range[1] + "/" + targ.length());
				fileHeaders.add(HEADER_CONTENT_RANGE + " : " + "bytes " + range[0] + "-" + range[1] + "/" + targ.length());
				HttpUtils.printHeaders(targ, fileHeaders, ps, HTTP_PARTIAL_CONTENT_TEXT);
			} else {
				HttpUtils.printHeaders(targ, fileHeaders, ps, HTTP_OK_TEXT);
				is = new FileInputStream(targ.getAbsolutePath());
			}			
					  
			
			ps.write(EOL);
		}

		try {
			int n;
			while ((n = is.read(buf)) > 0) {
				ps.write(buf, 0, n);
			}
		} finally {
			is.close();
		}
		//log("sendFile()-end file=" + targ.getName());
	}

	void listDirectory(File dir, PrintStream ps) throws IOException {
		ps.println("<TITLE>Directory listing</TITLE><P>\n");
		ps.println("<A HREF=\"..\">Parent Directory</A><BR>\n");
		String[] list = dir.list();
		for (int i = 0; list != null && i < list.length; i++) {
			File f = new File(dir, list[i]);
			if (f.isDirectory()) {
				ps.println("<A HREF=\"" + list[i] + "/\">" + list[i]
						+ "/</A><BR>");
			} else {
				ps.println("<A HREF=\"" + list[i] + "\">" + list[i] + "</A><BR");
			}
		}
		ps.println("<P><HR><BR><I>" + (new Date()) + "</I>");
	}

	static Map<String, String> getQueryMap(String fullUrl) {
		Map<String, String> map = new HashMap<String, String>();

		int i = fullUrl.indexOf('?');
		if (i > -1 && i + 1 < fullUrl.length()) {
			String queryStr = fullUrl.substring(i + 1);
			queryStr = queryStr.trim(); // trim is necessary here!
			//log("REQUEST:" + queryStr);

			String[] params = queryStr.split("&");
			for (String param : params) {
				String[] nv = param.split("=", 2);
				if (nv.length == 2)
					map.put(nv[0], nv[1]);
			}
		}

		return map;
	}

}