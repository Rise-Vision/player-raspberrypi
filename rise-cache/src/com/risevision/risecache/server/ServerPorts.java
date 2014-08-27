package com.risevision.risecache.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.risevision.risecache.Config;
import com.risevision.risecache.Globals;

public class ServerPorts {

	private static ArrayList<ConnectionInfo> redirects = new ArrayList<>();
	private static HashMap<Integer, Integer> counter = new HashMap<>(); // <local port, connection counter>


	public static void init() {

		// initialize counter
		for (int port : WebServer.ports) {
			if (port != Config.basePort)
				counter.put(port, 0);
		}
		
	}

	synchronized public static int getRedirectPort(String fileUrl) {
		
		int port = findAvailalePort(false);
		if (port == -1) {
			removeExpiredRedirects();
			port = findAvailalePort(true);
		}
		
		//add port and url
		if (port != -1) {
			addRedirect(port, fileUrl);
		}

		return port;
	}

	synchronized public static void setConnected(int port, String fileUrl) {
		
		//remove address from the list if it's already there
		releaseRedirect(port, fileUrl);
			
		//add port and address
		incCounter(port);
	}

	synchronized public static void setDisconnected(int port) {
		decCounter(port);
	}

	private static void addRedirect(int port, String fileUrl) {
		incCounter(port);
		redirects.add(new ConnectionInfo(port, fileUrl));
	}

	private static void releaseRedirect(int port, String fileUrl) {
		for (ConnectionInfo ci : redirects) {
			if (port == ci.localPort && fileUrl.equals(ci.fileUrl)) {
				redirects.remove(ci);
				decCounter(port);
				break;
			}			
		}
	}

	private static void incCounter(int localPort) {
		Integer count = counter.get(localPort);
		if (count != null) {
			counter.put(localPort, count + 1);
		}
	}

	private static void decCounter(int localPort) {
		Integer count = counter.get(localPort);
		if (count != null && count > 0) {
			counter.put(localPort, count - 1);
		}
	}

	private static int findAvailalePort(boolean canExceedLimit) {
		int bestPort = -1;
		int bestPortConnections = Integer.MAX_VALUE;
		
		for (Integer key : counter.keySet()) {
			int connectionCount = counter.get(key);
			
			if (connectionCount < Globals.MAX_CONNECTIONS_PER_PORT) {
				return key;
			} 
			
			if (connectionCount < bestPortConnections) {
				bestPort = key;
				bestPortConnections = connectionCount;
			}
			
		}

		if (canExceedLimit)
			return bestPort;
		else
			return -1;
	}
	
	private static void removeExpiredRedirects() {
		Date dt = new Date();
		Date dtExpriredRedirects = new Date( dt.getTime() - 1 * 1000); //expire redirect in 1 second
		
		for (ConnectionInfo ci : redirects) {
			if (ci.lastModified.before(dtExpriredRedirects)) {
				redirects.remove(ci);
				decCounter(ci.localPort);
			}
		}
	}


}
