package com.risevision.risecache;

public class Globals {

	public static final String APPLICATION_NAME = "RiseCache";
	public static final String APPLICATION_VERSION = "1.0.009";

	public static final int BASE_PORT = 9494;
	public static final int MAX_PORT = BASE_PORT + 20; //do not try to open ports above this number
	public static final int NUMBER_OF_PORTS_TO_OPEN = 6;  //how many additional ports to open to serve files
	public static final int MAX_CONNECTIONS_PER_PORT = 6; //Chrome can open up to  connections to one host
	//public static final long MAX_CONNECTION_WAIT_TIME = 60*60*1000; //1 hour in milliseconds
	
	public static final String FILE_EXT_DATA = "dat";
	public static final String FILE_EXT_HEADERS = "txt";
	
	public static final long FILE_KEEP_IN_CACHE_DURATION_DAYS = 30;
	public static final long FILE_KEEP_IN_CACHE_DURATION_MS = FILE_KEEP_IN_CACHE_DURATION_DAYS * 24 * 60 * 60 * 1000; //milliseconds
	
	public static final long DISK_FREE_THRESHOLD = 500 * 1024; //Always keep at-least 500MB free space
	public static final long FILE_KEEP_IN_CACHE_DURATION_DAYS_MINIMAL = 7;
	public static final long FILE_KEEP_IN_CACHE_DURATION_MS_MINIMAL = FILE_KEEP_IN_CACHE_DURATION_DAYS_MINIMAL * 24 * 60 * 60 * 1000; //milliseconds
	
	public static final long CHECK_EXPIRED_FREQUENCY_MS = 24 * 60 * 60 * 1000; //frequency to run delete expired job (once a day)
	public static final long CHECK_MODIFIED_FREQUENCY_MS = 5 * 60 * 1000; //frequency to check if file has been modified
	
	public static final String CROSSDOMAIN_XML = "<?xml version=\"1.0\"?>" +
			"<!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">" +
			"<cross-domain-policy>" +
			"<allow-access-from domain=\"*\" />" +
			"</cross-domain-policy>";

}
