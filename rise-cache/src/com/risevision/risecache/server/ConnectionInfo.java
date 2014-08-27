package com.risevision.risecache.server;

import java.util.Date;

public class ConnectionInfo {
	
	public int  localPort;
	public Date lastModified;
	public String fileUrl;

	public ConnectionInfo(int localPort, String fileUrl) {
		super();
		this.localPort = localPort;
		this.fileUrl = fileUrl;
		lastModified = new Date();		
	}

}
