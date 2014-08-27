package com.risevision.risecache.cache;

import java.util.Date;
import java.util.HashMap;

public class FileRequests {

	private static final long DATE_OFFSET_5_MINUTES = 5*60*1000; //5 minutes threshold to check if file is modified.
	private static HashMap<String, FileRequestInfo> map = new HashMap<>();
	
	synchronized public static boolean beginRequest(String url) {

		boolean res = false;
		
		FileRequestInfo fr = map.get(url);
		
		if (fr == null) {
			fr = new FileRequestInfo(url);
			map.put(url, fr);
			res = true;
		} else {
			//check if enough time (5 minutes) past since last request and that it's not downloading
			if (fr.downloadComplete) {
				Date dt5m = FileUtils.getDateOffset(- DATE_OFFSET_5_MINUTES);
				if (fr.lastRequested.before(dt5m)) {
					res = true;
					fr.setDownloadComplete(false);
				}
			} 
		}

		return res;
	}

	synchronized public static void endRequest(String url) {

		FileRequestInfo fr = map.get(url);
		
		if (fr != null) {
			fr.setDownloadComplete(true);
		}
		
	}

}
