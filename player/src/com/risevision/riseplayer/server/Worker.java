package com.risevision.riseplayer.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.risevision.riseplayer.Config;
import com.risevision.riseplayer.DisplayErrors;
import com.risevision.riseplayer.Globals;
import com.risevision.riseplayer.timers.HeartbeatTimer;
import com.risevision.riseplayer.utils.SystemInfo;
import com.risevision.riseplayer.utils.Utils;

class Worker extends WebServer implements HttpConstants, Runnable {
	final static int BUF_SIZE = 2048;

	static final byte[] EOL = { (byte) '\r', (byte) '\n' };

	/* buffer to use for requests */
	byte[] buf;
	/* Socket to client we're handling */
	private Socket s;

	public String httpErrorCode = HTTP_NOT_FOUND_TEXT;
	public String requestedUrl = "";

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

	void handleClient() throws IOException {
		requestedUrl = "";
		InputStream is = new BufferedInputStream(s.getInputStream());
		PrintStream ps = new PrintStream(s.getOutputStream());
		
		/*
		 * we will only block in read for this many milliseconds before we fail
		 * with java.io.InterruptedIOException, at which point we will abandon
		 * the connection.
		 */
		s.setSoTimeout(WebServer.timeout);
		s.setTcpNoDelay(true);
		/* zero out the buffer from last time */
		for (int i = 0; i < BUF_SIZE; i++) {
			buf[i] = 0;
		}
		try {
			/*
			 * We only support HTTP GET/HEAD, and don't support any fancy HTTP
			 * options, so we're only interested really in the first line.
			 */
			int nread = 0, r = 0;

			outerloop: while (nread < BUF_SIZE) {
				r = is.read(buf, nread, BUF_SIZE - nread);
				if (r == -1) {
					/* EOF */
					return;
				}
				int i = nread;
				nread += r;
				for (; i < nread; i++) {
					if (buf[i] == (byte) '\n' || buf[i] == (byte) '\r') {
						/* read one line */
						break outerloop;
					}
				}
			}

			/* are we doing a GET or just a HEAD */
			/* beginning of file name */
			int index = 0;
			if (buf[0] == (byte) 'G' && buf[1] == (byte) 'E'
					&& buf[2] == (byte) 'T' && buf[3] == (byte) ' ') {
				index = 4;
			} else if (buf[0] == (byte) 'H' && buf[1] == (byte) 'E'
					&& buf[2] == (byte) 'A' && buf[3] == (byte) 'D'
					&& buf[4] == (byte) ' ') {
				index = 5;
			} else {
				ps.print("HTTP/1.0 " + HTTP_BAD_METHOD + " unsupported method type: " + new String(buf, 0, 5));
				ps.write(buf, 0, 5);
				ps.write(EOL);
				ps.flush();
				//s.close();
				return;
			}

			//Log.info("REQUEST:" + new String(buf));
			
			int i = 0;
			/*
			 * find the file name, from: GET /foo/bar.html HTTP/1.0 extract
			 * "/foo/bar.html"
			 */
			for (i = index; i < nread; i++) {
				if (buf[i] == (byte) ' ') {
					break;
				}
			}
			String fullUrl = new String(buf, index, i - index);
			String fname = fullUrl;
			if (fname.startsWith("/")) {
				fname = fname.substring(1);
			}

			Map<String, String> queryMap = getQueryMap(fullUrl);
			String fileUrl = queryMap.get("url");
			fileUrl = fileUrl == null ? "" : URLDecoder.decode(fileUrl, "UTF-8").trim();
			requestedUrl = fileUrl;

			boolean isCrossDomain = "crossdomain.xml".equalsIgnoreCase(fname);
			boolean isPing = fname.startsWith("ping");
			boolean isHeartbeat = fname.startsWith("heartbeat");
			boolean isSaveProperty = fname.startsWith("save_property");
			boolean isSetProperty = fname.startsWith("set_property");
			boolean isGetSystemInfo = fname.startsWith("get_system_info");
			boolean isGetUpgradeInfo = fname.startsWith("get_upgrade_info");
			boolean isRestart = fname.startsWith("restart");
			boolean isReboot = fname.startsWith("reboot");
			boolean isShutdown = fname.startsWith("shutdown");
			boolean isVersion = fname.startsWith("version");
						
			if (isPing) {
				String callback = queryMap.get("callback");
				if (callback != null && !callback.isEmpty()) {
					String responseText = callback + "();";
					HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_JAVASCRIPT, responseText.length());
					ps.print(responseText);
				} else {
					HttpUtils.printHeader_ResponseCode(HTTP_BAD_REQUEST_TEXT, ps, true);
				}
			} else if (isHeartbeat) {
				HeartbeatTimer.recordHeartbeat();
				String callback = queryMap.get("callback");
				if (callback != null && !callback.isEmpty()) {
					String responseText = callback + "();";
					HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_JAVASCRIPT, responseText.length());
					ps.print(responseText);
				} else {
					HttpUtils.printHeader_ResponseCode(HTTP_OK_TEXT, ps, true);
				}
			} else if (isGetSystemInfo) {
				String callback = queryMap.get("callback");
				if (callback != null && !callback.isEmpty()) {
					String responseText = callback + "(" + getSystemInfo() + ");";
					HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_JAVASCRIPT, responseText.length());
					ps.print(responseText);
				} else {
					HttpUtils.printHeader_ResponseCode(HTTP_OK_TEXT, ps, true);
				}
			} else if (isGetUpgradeInfo) {
				HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_TEXT_PLAIN, Config.upgradeInfo.length());
				ps.print(Config.upgradeInfo);
			} else if (isSetProperty) {
				
				if ("true".equalsIgnoreCase(queryMap.get("reboot_required"))) {
					log("reboot_required received");
					Utils.reboot();
				} else if ("true".equalsIgnoreCase(queryMap.get("restart_required"))) {
					log("restart_required received");
					Utils.restart();
				} else if ("true".equalsIgnoreCase(queryMap.get("update_required"))) {
					log("update_required received");
					Utils.restart(); 
				} else	if ("true".equalsIgnoreCase(queryMap.get("reboot_enabled"))) {
					Config.setRestartTime(queryMap.get("reboot_time")); //time to restart (not reboot)
				} else	if ("off".equalsIgnoreCase(queryMap.get("display_command"))) {
					//log("display displayStandby off received");
					Utils.displayStandby(false);
				} else	if ("on".equalsIgnoreCase(queryMap.get("display_command"))) {
					//log("display displayStandby on received");
					Utils.displayStandby(true);
				}
				
				HttpUtils.printHeadersCommon(HTTP_OK_TEXT, ps);
				ps.write(EOL); //close header section after last header

			} else if (isSaveProperty) {
				String displayId = queryMap.get("display_id");
				String claimId = queryMap.get("claim_id");
				String restartViewer = queryMap.get("restart_viewer");
				
				if (displayId != null && !displayId.isEmpty()) {
					Config.displayId = displayId;
				}
				
				if (claimId != null && !claimId.isEmpty()) {
					Config.claimId = claimId;
				} 

				Config.saveDisplayProperties();
				HttpUtils.printHeader_ResponseCode(HTTP_OK_TEXT, ps, true);
				
				if ("true".equals(restartViewer)) {
					Utils.restartViewer();
				} 
				
			} else if (isRestart) {
				log("restart command received");
				Utils.restart();
			} else if (isReboot) {
				log("reboot command received");
				Utils.reboot();
			} else if (isShutdown) {
				log("shutdown command received");
				Utils.stopViewer();
				DisplayErrors.getInstance().writeErrorsToFile();
				System.exit(0);
			} else if (isVersion) {
				HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_TEXT_PLAIN, Globals.APPLICATION_VERSION.length());
				ps.print(Globals.APPLICATION_VERSION);
			} else if (isCrossDomain) {
				HttpUtils.printHeadersCommon(ps, CONTENT_TYPE_TEXT_XML, Globals.CROSSDOMAIN_XML.length());
				ps.print(Globals.CROSSDOMAIN_XML);
			} else {
				HttpUtils.printHeader_ResponseCode(HTTP_BAD_REQUEST_TEXT, ps, true);
			}

		} finally {
			s.close();
		}
	}

	private static String getSystemInfo() {
		return SystemInfo.asUrlParam(false);
	}

	void sendFile(File targ, PrintStream ps) throws IOException {
		InputStream is = null;
		ps.write(EOL);
		if (targ.isDirectory()) {
			listDirectory(targ, ps);
			return;
		} else {
			is = new FileInputStream(targ.getAbsolutePath());
		}

		try {
			int n;
			while ((n = is.read(buf)) > 0) {
				ps.write(buf, 0, n);
			}
		} finally {
			is.close();
		}
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