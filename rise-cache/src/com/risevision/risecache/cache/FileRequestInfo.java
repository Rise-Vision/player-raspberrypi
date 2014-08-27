package com.risevision.risecache.cache;

import java.util.Date;

public class FileRequestInfo {

	public String url;
	public Date lastRequested;
	public boolean downloadComplete;
	
	public FileRequestInfo(String url) {
		this.url = url;
		setDownloadComplete(false);
	}

	public void setDownloadComplete(boolean value) {
		downloadComplete = value;
		if (!value) {
			this.lastRequested = new Date();
		}
	}

}
