package com.risevision.risecache;

import java.net.BindException;

import com.risevision.risecache.cache.FileUtils;
import com.risevision.risecache.jobs.CheckExpiredJob;
import com.risevision.risecache.server.WebServer;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Config.init(Main.class);
		Log.init(Config.appPath, Globals.APPLICATION_NAME);
		Log.info("***** " + Globals.APPLICATION_NAME +" version " + Globals.APPLICATION_VERSION + " *****");
		Config.loadProps();
		
		try {
			
			//use socket to test if another instance is running
			java.net.ServerSocket ss = WebServer.createServerSocket();//new java.net.ServerSocket(Config.basePort); 
			
			//FileUtils.deleteExpired();
			FileUtils.deleteAllDuplicates();
			FileUtils.deleteIncompleteDownloads();
			
			CheckExpiredJob.start();
			
			ss.close();
			
			WebServer.main(args);
		} catch (BindException e) {
			Log.error("Cannot start application. Cannot open port " + Config.basePort + ". You can only run one instance of " + Globals.APPLICATION_NAME + ".");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
