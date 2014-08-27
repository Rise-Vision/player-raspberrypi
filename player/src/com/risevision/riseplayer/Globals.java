package com.risevision.riseplayer;

public class Globals {

	public static final String APPLICATION_NAME = "RisePlayer";
	public static final String APPLICATION_VERSION = "2.0.036";

	public static final int BASE_PORT = 9449;
	public static final int MAX_WORKERS = 10;

	public static final long HEARTBEAT_TIMER_INTERVAL_MS = 60 * 1000;
	public static final long MAX_HEARTBEAT_GAP_MS = 3 * 60 * 1000;
	
	public static final String CROSSDOMAIN_XML = "<?xml version=\"1.0\"?>" +
			"<!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">" +
			"<cross-domain-policy>" +
			"<allow-access-from domain=\"*\" />" +
			"</cross-domain-policy>";
	
	public static final String VIEWER_BASE_URL = "http://rvashow.appspot.com/Viewer.html";
	public static final String CORE_BASE_URL = "http://rvaserver2.appspot.com";
	//public static final String VIEWER_BASE_URL = "http://viewer-test.appspot.com/Viewer.html";
	//public static String CHROME_PACKAGED_APP_ID = "chdapkhijmihjejhbmdlfponohigcjai";

}
