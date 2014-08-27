package com.risevision.risecache.cache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import com.risevision.risecache.Config;
import com.risevision.risecache.Globals;
import com.risevision.risecache.Log;
import com.risevision.risecache.server.HttpConstants;

public class FileUtils {
			
	synchronized public static FileInfo getFileInfo(String name) {
		
		int version  = getCurrentVersion(name, null);
		
		Path dataFilePath = Paths.get(Config.cachePath, getDataFileName(name, version));
		Path headerFilePath = Paths.get(Config.cachePath, getHeadersFileName(name, version));

		Date fileLastModifiedTime; 
    	BasicFileAttributes attrs;
    	
		try {
			attrs = Files.readAttributes(dataFilePath, BasicFileAttributes.class);
        	fileLastModifiedTime = new Date(attrs.lastModifiedTime().toMillis());
		} catch (Exception e) {
			Log.error("Cannot read file attributes. " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		//read headers

		ArrayList<String> headers = loadHeaders(headerFilePath.toFile());
		
		String url = null;
		String etag = null;
		
		for (String header : headers) {
			if (header.startsWith(HttpConstants.HEADER_FILE_URL + ":")) {
				url = header.split(":", 2)[1].trim();
			} else if (header.startsWith(HttpConstants.HEADER_ETAG + ":")) {
				etag = header.split(":", 2)[1].trim();
			}
		}

		if (url != null)
			return new FileInfo(url, fileLastModifiedTime, etag);
		else
			return null;
	}

	synchronized public static void deleteExpired() {
		
		Date expiryDate;
		long freeSpace = getFreeUserSpace();
		if(freeSpace < Globals.DISK_FREE_THRESHOLD)
			expiryDate = new Date(new Date().getTime() - Globals.FILE_KEEP_IN_CACHE_DURATION_MS_MINIMAL);
		else
			expiryDate = new Date(new Date().getTime() - Globals.FILE_KEEP_IN_CACHE_DURATION_MS);
		
		Log.warn("running Delete Expired job. expiryDate=" + expiryDate.toString() + " , freeSpace=" + freeSpace);
		File folder = new File(Config.cachePath);
		for (final File file : folder.listFiles()) {
		    if (file.isFile() && file.getName().endsWith(Globals.FILE_EXT_DATA)) {
		    	BasicFileAttributes attrs;
				try {
					attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		        	Date fileLastAccessTime = new Date(attrs.lastAccessTime().toMillis());
					if (fileLastAccessTime.before(expiryDate))
						deleteDataAndHeaderFiles(file);
				} catch (Exception e) {
					Log.error("Cannot read file attributes. " + e.getMessage());
					e.printStackTrace();
				}
		    }
		}
	}

	private static long getFreeUserSpace() {
		File folder = new File(Config.cachePath);
		try {
			//return (int)((folder.getUsableSpace() * 100.0f) / folder.getTotalSpace());
			return folder.getUsableSpace();
		}
		catch(Exception e) {
			Log.error("Cannot calculate free space percentage. " + e.getMessage());
			e.printStackTrace();
			return 0;
		}
	}
	synchronized public static void deleteIncompleteDownloads() {
		try {
			File directory = new File(Config.downloadPath);
			File[] files = directory.listFiles();
			for (File file : files)
			{
				try {
					if (!file.delete())
						Log.info("Cannnot delete temp file: " + file.getName());
				} catch (Exception e) {
					Log.info("Cannnot delete temp file: " + file.getName() + ". Error:" + e.getMessage());
				}
			}
		} catch (Exception e) {
			Log.error("Error in deleteIncompleteDownloads(): " + e.getMessage());
		}
	}

	//do not make updateLastAccessed synchronized
	public static void updateLastAccessed(File file) {
		try {
			Files.setAttribute(file.toPath(), "lastAccessTime", FileTime.fromMillis(new Date().getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	synchronized public static void deleteAllDuplicates() {
		HashSet<String> names = new HashSet<>();
		HashSet<String> duplicates = new HashSet<>();
				
		File folder = new File(Config.cachePath);
		for (final File file : folder.listFiles()) {
		    if (file.isFile() && file.getName().endsWith(Globals.FILE_EXT_DATA)) {
		    	String name = extractFileName(file.getName());
		    	if (!names.add(name)) {
		    		duplicates.add(name);
		    	} 
		    }
		}
		
		for (String name: duplicates) {
			deleteFileDuplicates(name);
		}	    
	}

	synchronized public static void deleteFileDuplicates(String name) {
		HashSet<Integer> versions = new HashSet<>();
		int maxVersion = getCurrentVersion(name, versions);
		
		for (int version: versions) {
			if (version != maxVersion) 
				deleteDataAndHeaderFiles(name, version);
		}
	}

	synchronized public static void moveDownloadedFileToCache(String name, ArrayList<String> headers) {
		int version = getCurrentVersion(name, null);
		version++;

		Path src = Paths.get(Config.downloadPath, name);
		Path dest = Paths.get(Config.cachePath, getDataFileName(name, version));
		Path destHeaders = Paths.get(Config.cachePath, getHeadersFileName(name, version));
		
		try {
			Files.move(src, dest);
			saveHeaders(destHeaders, headers);
		} catch (IOException e) {
			Log.error("Error moving file " + src + " to " + dest + ". " + e.getMessage());
			e.printStackTrace();
		}
		
		if (version != 0)
			deleteFileDuplicates(name);
	}

	synchronized public static void saveHeaders(Path path, ArrayList<String> headers) {		
	    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)){
	      for(String line : headers) {
	        writer.write(line);
	        writer.newLine();
	      }
	    } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> loadHeaders(File headerFile) {
		Path path = headerFile.toPath();
	    try {
			return (ArrayList<String>) Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return null;
	}


	public static String getCurrentDataFileName(String name) {

		int version = getCurrentVersion(name, null);
		
		if (version == -1)
			return "";
		else 
			return getDataFileName(name, version);

	}

	public static String getDataFileName(String name, int version) {

		return name + "." + intToHex8(version) + "." + Globals.FILE_EXT_DATA;

	}

	public static String getHeadersFileName(String name, int version) {

		return name + "." + intToHex8(version) + "." + Globals.FILE_EXT_HEADERS;

	}
	
	private static int getCurrentVersion(String name, HashSet<Integer> versions) {
		int maxVersion = -1;

		File folder = new File(Config.cachePath);
	    for (final File file : folder.listFiles()) {
	        if (file.isFile() && file.getName().startsWith(name) && file.getName().endsWith(Globals.FILE_EXT_DATA)) {
	        	int version = extractFileVersionAsInt(file.getName());
	        	if (versions != null)
	        		versions.add(version);
        		if (version > maxVersion)
        			maxVersion = version;
	        }
	    }
		return maxVersion;
	}

	private static void deleteDataAndHeaderFiles(File dataFile) {
		deleteFile(dataFile);
		String headerFileName = dataFileNameToHeaderFileName(dataFile.getName());
		deleteFile(new File(headerFileName));
	}

	private static void deleteDataAndHeaderFiles(String name, int version) {
		
		String dataFileName = getDataFileName(name, version);
		String headerFileName = getHeadersFileName(name, version);

		File dataFile = new File(Config.cachePath, dataFileName);
		File headerFile = new File(Config.cachePath, headerFileName);
		
		deleteFile(dataFile);
		deleteFile(headerFile);

	}

	private static void deleteFile(File file) {
		
		try {
			
			if (file.exists()) {
				file.delete(); 
				Log.info("File " + file.getName() + " is deleted.");
			}
			
		} catch (Exception e) {
			Log.info("Error deleting file " + file.getName() + ". Error:" + e.getMessage());
		}
	}

	public static String intToHex8(int value) {
		return String.format("%08X", value);		
	}

	public static String extractFileName(String name) {
		return name.substring(0, 8);
	}

	public static String extractFileVersion(String name) {
		return name.substring(9, 17);
	}

	public static int extractFileVersionAsInt(String name) {
		return Integer.parseInt(extractFileVersion(name), 16);
	}

	public static String dataFileNameToHeaderFileName(String dataFileName) {
		return dataFileName.substring(0, dataFileName.length()-3) + Globals.FILE_EXT_HEADERS;
	}

	public static Date getDateOffset(long offset) {
		return new Date(new Date().getTime() + offset);
	}
	
}
