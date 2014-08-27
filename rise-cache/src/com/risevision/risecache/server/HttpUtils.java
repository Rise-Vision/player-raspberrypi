package com.risevision.risecache.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HttpUtils implements HttpConstants {

	static final byte[] EOL = { (byte) '\r', (byte) '\n' };

	public static void printHeader(String headerText, PrintStream ps, boolean isLastHeader)  {
		
		if (ps != null) {
			try {
				ps.print(headerText);
				ps.write(EOL);

				// Finalize header section if required
				if (isLastHeader)
					ps.write(EOL);
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}
	
	}

	public static void printHeader_ResponseCode(String responseCode, PrintStream ps) {
		printHeader_ResponseCode(responseCode, ps, false);
	}

	public static void printHeader_ResponseCode(String responseCode, PrintStream ps, boolean isLastHeader) {
		printHeader("HTTP/1.1 " + responseCode, ps, isLastHeader);
	}

	public static String printHeader_ContentLength(long contentLength, PrintStream ps, boolean isLastHeader) {
		String header = HttpConstants.HEADER_CONTENT_LENGTH + ": " + contentLength;
		printHeader(header, ps, isLastHeader);
		return header;
	}
	
	public static String printHeader_ContentType(String contentType, PrintStream ps, boolean isLastHeader) {
		String header = HttpConstants.HEADER_CONTENT_TYPE + ": " + contentType;
		printHeader(header, ps, isLastHeader);
		return header;
	}

	public static String printHeader_ETag(String etag, PrintStream ps, boolean isLastHeader) {
		String header =  HttpConstants.HEADER_ETAG + ": " + etag;
		printHeader(header, ps, isLastHeader);
		return header;
	}

	public static void printHeadersCommon(String responseCode, PrintStream ps) throws IOException {
		
		if (ps != null) {
			printHeader_ResponseCode(responseCode, ps, false);
			
			ps.print("Date: " + formatDate(new Date()));
			ps.write(EOL);
			ps.print("Server: Rise Cache");
			ps.write(EOL);
			ps.print("Access-Control-Allow-Origin: *");
			ps.write(EOL);
			
			ps.print("Cache-Control: no-cache");
			ps.write(EOL);
			ps.print("Pragma: no-cache");
			ps.write(EOL);
			ps.print("Expires: -1");
			ps.write(EOL);

		}

	}

	
	public static void printHeadersCommon(PrintStream ps, String contentType, int contentLength) throws IOException {
		
		printHeadersCommon(HTTP_OK_TEXT, ps);
		printHeader_ContentType(contentType, ps, false);
		printHeader_ContentLength(contentLength, ps, true);
			
	}

	
	public static void printHeaders(File file, ArrayList<String> headers, PrintStream ps, String responseCode) throws IOException {
		if (headers == null) {
			printHeaders(file, ps, responseCode);
		} else {
			//printHeader_ResponseCode(HTTP_OK_TEXT, ps);
			printHeadersCommon(responseCode, ps);
			for (String header : headers) {
				printHeader(header, ps, false);
			}
		}
		
	}

	public static boolean printHeaders(File targ, PrintStream ps, String responseCode) throws IOException {
	
		boolean ret = false;
	
		if (!targ.exists()) {
			printHeadersCommon(HTTP_BAD_REQUEST_TEXT, ps);
			ret = false;
		} else {
			printHeadersCommon(responseCode, ps);
			ret = true;
		}
				
		if (ret) {
			if (!targ.isDirectory()) {
				 ps.print("Last Modified: " + formatDate(new Date(targ.lastModified())));
				 ps.write(EOL);
	
				String name = targ.getName();
				int ind = name.lastIndexOf('.');
				String ct = null;
				if (ind > 0) {
					ct = (String) map.get(name.substring(ind));
				}
				if (ct == null) {
					ct = "video/mp4";
					//ct = "unknown/unknown";
				}
				
				printHeader_ContentType(ct, ps, false);
				printHeader_ContentLength(targ.length(), ps, false);
				
				ps.print("Keep-Alive: timeout=5, max=99");
				ps.write(EOL);
				ps.print("Connection: Keep-Alive");
				ps.write(EOL);
	
				
			} else {
				printHeader_ContentType("text/html", ps, false);
			}
		}
		return ret;
	}

	public static void printHeader_ResponseRedirect(String newUrl, PrintStream ps) {
		try {
			printHeadersCommon(HTTP_MOVED_TEMP_TEXT, ps);
			printHeader("Location: " + newUrl, ps, false);
			printHeader_ContentType(CONTENT_TYPE_TEXT_PLAIN, ps, false);
			printHeader_ContentLength(0, ps, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//printHeader_ResponseCode(HTTP_MOVED_TEMP_TEXT, ps);
		//printHeader("Location: " + newUrl, ps, false);
		//printHeader("Connection: close", ps, true);
	}

	static public String formatDate(Date date) {
		SimpleDateFormat sd = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'zzz", Locale.US);
		sd.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sd.format(new Date());
	}
	
	/* mapping of file extensions to content-types */
	static java.util.Hashtable<String, String> map = new java.util.Hashtable<String, String>();

	static {
		fillMap();
	}

	static void setSuffix(String k, String v) {
		map.put(k, v);
	}

	static void fillMap() {
		setSuffix("", "content/unknown");
		setSuffix(".uu", "application/octet-stream");
		setSuffix(".exe", "application/octet-stream");
		setSuffix(".ddf", "application/octet-stream");
		setSuffix(".ps", "application/postscript");
		setSuffix(".zip", "application/zip");
		setSuffix(".sh", "application/x-shar");
		setSuffix(".tar", "application/x-tar");
		setSuffix(".snd", "audio/basic");
		setSuffix(".au", "audio/basic");
		setSuffix(".wav", "audio/x-wav");
		setSuffix(".gif", "image/gif");
		setSuffix(".jpg", "image/jpeg");
		setSuffix(".jpeg", "image/jpeg");
		setSuffix(".htm", "text/html");
		setSuffix(".html", "text/html");
		setSuffix(".text", "text/plain");
		setSuffix(".c", "text/plain");
		setSuffix(".cc", "text/plain");
		setSuffix(".c++", "text/plain");
		setSuffix(".h", "text/plain");
		setSuffix(".pl", "text/plain");
		setSuffix(".txt", "text/plain");
		setSuffix(".java", "text/plain");
		setSuffix(".mp4", "video/mp4");
		setSuffix(".xml", "text/xml; charset=utf-8");
	}
	
}
