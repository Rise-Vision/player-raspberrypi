package com.risevision.risecache.server;

public interface HttpConstants {
	
	/** 2XX: generally "OK" */
	public static final int HTTP_OK = 200;
	public static final int HTTP_CREATED = 201;
	public static final int HTTP_ACCEPTED = 202;
	public static final int HTTP_NOT_AUTHORITATIVE = 203;
	public static final int HTTP_NO_CONTENT = 204;
	public static final int HTTP_RESET = 205;
	public static final int HTTP_PARTIAL = 206;

	/** 3XX: relocation/redirect */
	public static final int HTTP_MULT_CHOICE = 300;
	public static final int HTTP_MOVED_PERM = 301;
	public static final int HTTP_MOVED_TEMP = 302;
	public static final int HTTP_SEE_OTHER = 303;
	public static final int HTTP_NOT_MODIFIED = 304;
	public static final int HTTP_USE_PROXY = 305;

	/** 4XX: client error */
	public static final int HTTP_BAD_REQUEST = 400;
	public static final int HTTP_UNAUTHORIZED = 401;
	public static final int HTTP_PAYMENT_REQUIRED = 402;
	public static final int HTTP_FORBIDDEN = 403;
	public static final int HTTP_NOT_FOUND = 404;
	public static final int HTTP_BAD_METHOD = 405;
	public static final int HTTP_NOT_ACCEPTABLE = 406;
	public static final int HTTP_PROXY_AUTH = 407;
	public static final int HTTP_CLIENT_TIMEOUT = 408;
	public static final int HTTP_CONFLICT = 409;
	public static final int HTTP_GONE = 410;
	public static final int HTTP_LENGTH_REQUIRED = 411;
	public static final int HTTP_PRECON_FAILED = 412;
	public static final int HTTP_ENTITY_TOO_LARGE = 413;
	public static final int HTTP_REQ_TOO_LONG = 414;
	public static final int HTTP_UNSUPPORTED_TYPE = 415;

	/** 5XX: server error */
	public static final int HTTP_SERVER_ERROR = 500;
	public static final int HTTP_INTERNAL_ERROR = 501;
	public static final int HTTP_BAD_GATEWAY = 502;
	public static final int HTTP_UNAVAILABLE = 503;
	public static final int HTTP_GATEWAY_TIMEOUT = 504;
	public static final int HTTP_VERSION = 505;

	/** text constants */
	public static final String HTTP_OK_TEXT = "200 OK";
	public static final String HTTP_PARTIAL_CONTENT_TEXT = "206 Partial Content";
	public static final String HTTP_MOVED_TEMP_TEXT = "302 Found";
	public static final String HTTP_BAD_REQUEST_TEXT = "400 Bad Request. Rise Cache.";
	public static final String HTTP_NOT_FOUND_TEXT = "404 Not Found. Rise Cache.";
	public static final String HTTP_CONNECTION_REFUSED_TEXT = "404 Connection refused.Rise Cache.";
	public static final String HTTP_CLIENT_TIMEOUT_TEXT = "408 Request Timeout. Rise Cache.";
	public static final String HTTP_INSUFFICIENT_SPACE_TEXT = "500 Internal Server Error. Insufficient Space. Rise Cache.";
	public static final String HTTP_INTERNAL_ERROR_TEXT = "500 Internal Server Error. Rise Cache.";
	
	public static final String HEADER_IF_NONE_MATCH = "If-None-Match";
	public static final String HEADER_IF_MODIFIED_SINCE = "If-Modified-Since";
	public static final String HEADER_LAST_MODIFIED = "Last-Modified";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String HEADER_CONTENT_LENGTH = "Content-Length";
	public static final String HEADER_CACHE_CONTROL = "Cache-Control";
	public static final String HEADER_USER_AGENT = "User-Agent";
	public static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
	public static final String HEADER_CONTENT_RANGE = "Content-Range";	
	public static final String HEADER_DATE = "Date";
	public static final String HEADER_SERVER = "Server";
	public static final String HEADER_ETAG = "ETag";
	public static final String HEADER_FILE_URL = "File-URL";
	
	public static final String CONTENT_TYPE_JAVASCRIPT = "text/javascript; charset=UTF-8";
	public static final String CONTENT_TYPE_TEXT_XML = "text/xml; charset=UTF-8";
	public static final String CONTENT_TYPE_TEXT_PLAIN = "text/plain";
	public static final String CONTENT_TYPE_TEXT_HTML = "text/html";
	
}