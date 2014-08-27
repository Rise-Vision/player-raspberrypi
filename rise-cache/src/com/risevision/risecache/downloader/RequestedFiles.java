package com.risevision.risecache.downloader;

import java.util.HashMap;

public class RequestedFiles {

	private HashMap<String, String> list = new HashMap<>();

	synchronized public void add(String url) {
		if (!list.containsKey(url))
			list.put(url, url);
	}

	synchronized public void remove(String fullUrl) {
		list.remove(fullUrl);
	}

	synchronized public String getNext() {
		if (list.size() > 0) {
			for (String key : list.keySet()) {
				return list.get(key);
			}
		}

		return null;
	}

	synchronized public boolean requested(String url) {
		return list.containsKey(url);
	}

	synchronized public int size() {
		return list.size();
	}

}
