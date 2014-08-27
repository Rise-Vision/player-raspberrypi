package com.risevision.risecache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

public class Config {

	public static String appPath;
	public static String cachePath; //folder for downloaded files
	public static String downloadPath; //folder for downloading files

	public static int basePort = Globals.BASE_PORT;
	public static int maxPort = Globals.MAX_PORT;

	public static boolean useProxy;
	public static String proxyAddr;
	public static int proxyPort;

	public static String debugUrl = null;
	
	
	private static Properties props;
	
	public static void init(Class<?> mainClass) {
		appPath = mainClass.getProtectionDomain().getCodeSource().getLocation().getPath();
		if (System.getProperty("os.name").startsWith("Windows")) {
			try {
				appPath = URLDecoder.decode(appPath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			appPath = appPath.substring(1).replace("/", File.separator);
		}
	
		if (appPath.endsWith(".jar") || appPath.endsWith(File.separator)) {
			appPath = appPath.substring(0, appPath.lastIndexOf(File.separator));
		}
	
		cachePath = appPath + File.separator + "cache";
		downloadPath = appPath + File.separator + "download";
	
		//create download folder if missing
		File downloadDir = new File(downloadPath);
		if (!downloadDir.exists()) {
			downloadDir.mkdir();
			//throw new Error(dataPath + " doesn't exist as server root");
		}
	
		//create cache folder if missing
		File cacheDir = new File(cachePath);
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
			//throw new Error(cacheDir + " doesn't exist as server root");
		}
	
	}
	
	static void loadProps() {

		String fileName = appPath + File.separator + Globals.APPLICATION_NAME + ".properties";
		try {
			File f = new File(fileName);
			if (f.exists()) {
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				props = new Properties();
				props.load(is);
				is.close();
				Log.info("Loading application properties...");
				basePort = getPropertyInt("base.port", Globals.BASE_PORT);
				maxPort = getPropertyInt("max.port", Globals.MAX_PORT);
				if (maxPort <= basePort)
					maxPort = basePort + 20;
				debugUrl = getPropertyStr("debug.url", null);
				//proxy
				useProxy = "true".equalsIgnoreCase(getPropertyStr("proxy.enabled", "false"));
				proxyAddr = getPropertyStr("proxy.address", null);
				proxyPort = getPropertyInt("proxy.port", 0);
			} else {
				Log.info("Application properties file is not found. Using default setting. File name: " + fileName);
			}
			
		} catch (Exception e) {
			Log.warn("Error loading application properties. File name: " + fileName + ". Error: " + e.getMessage());
		}
	}

	private static int getPropertyInt(String name, int defaultValue) {
		String s = props.getProperty(name);
		int res = defaultValue;
		if (s != null) {
			try {
				res = Integer.parseInt(s);
			} catch (Exception e) {
				Log.warn("property " + name + " is not a number.");
			}
		}
		return res;
	}

	private static String getPropertyStr(String name, String defaultValue) {
		String s = props.getProperty(name);
		String res = defaultValue;
		if (s != null) {
			res = s;
		}
		return res;
	}


}
