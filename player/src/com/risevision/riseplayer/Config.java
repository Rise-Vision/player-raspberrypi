package com.risevision.riseplayer;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.risevision.riseplayer.timers.RestartTimer;
import com.risevision.riseplayer.utils.SystemInfo;
import com.risevision.riseplayer.utils.Utils;

public class Config {

	private static final String  FILE_APPLICATION_PROPERTIES = Globals.APPLICATION_NAME + ".properties";
	private static final String  FILE_DISPLAY_PROPERTIES = "RiseDisplayNetworkII.ini";
	
	private static final String PROPERTY_DISPLAY_ID = "displayid";
	private static final String PROPERTY_CLAIM_ID = "claimid";
	private static final String PROPERTY_VIEWER_URL = "viewerurl";
	private static final String PROPERTY_CORE_URL = "coreurl";
	
	public static final String CHROME_PREFERENCES = "{\"countryid_at_install\":0,\"default_search_provider\":{\"enabled\":false},\"geolocation\":{\"default_content_setting\":1},\"profile\":{\"content_settings\":{\"pref_version\":1},\"default_content_settings\":{\"geolocation\": 1},\"exited_cleanly\":true}}";
	
	public static String appPath;

	public static int basePort = Globals.BASE_PORT;

	public static boolean useProxy;
	public static String proxyAddr;
	public static int proxyPort;
	
	public static String viewerBaseUrl = Globals.VIEWER_BASE_URL;
	public static String coreBaseUrl = Globals.CORE_BASE_URL;
	
	public static boolean isWindows;
	public static boolean isLnxRoot = false;
	public static String linuxSudeUser;
	public static String userHome;
	public static String playerOS;
	public static String extendedModeExe = "RiseWindowUtility.exe";
	public static String displayStandbyExe = "RiseStandbyDisplay";
	public static String viewerAppTitle = "Viewer";
	
	public static String displayId = "";
	public static String claimId = "";
	public static String chromePath;
	public static String chromeAppId = null;
	
	private static Properties appProps = new Properties();
	private static Properties displayProps = new Properties();
	public static String upgradeInfo = "";

	public static int screenHeight = 0;
	public static int screenWidth = 0;
	private static Date restartTime;
	
	private static final String PLAYER_NAME_DEFAULT = "RisePlayer";
	private static String playerName = PLAYER_NAME_DEFAULT;
	
	private static final int EXTENDEDMODE_DELAY_DEFAULT = 5 * 1000; //5 secs
	private static int extendedModeDelay = EXTENDEDMODE_DELAY_DEFAULT;
	
	public static void init(Class<?> mainClass) {
		
		isWindows = System.getProperty("os.name").startsWith("Windows");
		userHome = System.getProperty( "user.home" );
		if(isWindows) 
			playerOS = "win";
		else {
			playerOS = "lnx";
			
			isLnxRoot = System.getProperty("user.name").equals("root");
			if(isLnxRoot) {
				linuxSudeUser = System.getenv("SUDO_USER");
				userHome = System.getenv("HOME");
				if(userHome.isEmpty())
					userHome = File.separator + "home" + File.separator + linuxSudeUser;
			}	
		}

		Log.info("userHome =" + userHome);
		
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
	
		calculateScreenResolution();

	}
	
	static void loadApplicationProperties() {

		String fileName = FILE_APPLICATION_PROPERTIES;
		try {
			File f = new File(appPath, fileName);
			if (f.exists()) {
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				appProps.load(is);
				is.close();
				Log.info("Loading application properties...");
				basePort = getPropertyInt("base.port", Globals.BASE_PORT);
				chromePath = getPropertyStr("chrome.path", getDefaultChromePath(), appProps);
				chromeAppId = getPropertyStr("chrome.app.id", null, appProps);
				playerName = getPropertyStr("playername", PLAYER_NAME_DEFAULT, appProps);
				extendedModeDelay = getPropertyInt("extendedmodedelay", EXTENDEDMODE_DELAY_DEFAULT);
				//proxy
				useProxy = "true".equalsIgnoreCase(getPropertyStr("proxy.enabled", "false", appProps));
				proxyAddr = getPropertyStr("proxy.address", null, appProps);
				proxyPort = getPropertyInt("proxy.port", 0);
				if(!isWindows) {
					playerOS = getPropertyStr("playeros", "lnx", appProps);
				}
			} else {
				Log.info("Application properties file is not found. Using default setting. File name: " + f.getName());
				chromePath = getDefaultChromePath();
			}
			
		} catch (Exception e) {
			Log.warn("Error loading application properties. File name: " + fileName + ". Error: " + e.getMessage());
		}
	}

	private static String getDefaultChromePath() {
		if (isWindows)
			return appPath + File.separator + "chromium" + File.separator + "chrome.exe";
		else
			return appPath + File.separator + "chrome-linux" + File.separator + "chrome";
	}

	public static String getFlashPluginPath() {
		if (isWindows)
			return appPath + File.separator + "chromium" + File.separator + "PepperFlash" + File.separator + "pepflashplayer.dll";
		else
			return appPath + File.separator + "chrome-linux" + File.separator + "libflashplayer.so";
	}

	public static String getChromeDataPath() {
		if (isWindows) 
			return appPath + File.separator + "data";
		else
			return userHome + File.separator + ".config/rvplayer";
	}

	public static String getChromeLogFilePath() {
		return appPath + File.separator + "chromium.log";
	}

	public static String getChromeCachePath() {
		if (isWindows) 
			return "";
		else
			return userHome + File.separator + ".cache/rvplayer";
	}

	public static String getChromePreferencesFilePath() {
		return getChromeDataPath() + File.separator + "Default" + File.separator + "Preferences";
	}

	public static String getClearCacheFilePath() {
		return appPath + File.separator + "clear_cache";
	}

	public static String getExtendedModeFile() {
		if (isWindows) 
			return appPath + File.separator + extendedModeExe;
		else
			return "";
	}
	
	public static String getDisplayStandbyFile() {
			return appPath + File.separator + displayStandbyExe;
	}
	
	private static int getPropertyInt(String name, int defaultValue) {
		String s = appProps.getProperty(name);
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

	private static String getPropertyStr(String name, String defaultValue, Properties properties) {
		String s = properties.getProperty(name);
		String res = defaultValue;
		if (s != null) {
			res = s;
		}
		return res;
	}

	public static void loadDisplayProperties() {

		String fileName = FILE_DISPLAY_PROPERTIES;
		try {
			File f = new File(appPath, fileName);
			if (f.exists()) {
				InputStream is = new BufferedInputStream(new FileInputStream(f));
				displayProps.load(is);
				is.close();
				Log.info("Loading display properties...");
				displayId = getPropertyStr(PROPERTY_DISPLAY_ID, "", displayProps);
				claimId = getPropertyStr(PROPERTY_CLAIM_ID, "", displayProps);
				viewerBaseUrl = getPropertyStr(PROPERTY_VIEWER_URL, Globals.VIEWER_BASE_URL, displayProps);
				coreBaseUrl = getPropertyStr(PROPERTY_CORE_URL, Globals.CORE_BASE_URL, displayProps);
			} else {
				Log.info("Display properties file is not found. Using default setting. File name: " + f.getName());
			}
			
		} catch (Exception e) {
			Log.warn("Error loading display properties. File name: " + fileName + ". Error: " + e.getMessage());
		}
	}

	public static void saveDisplayProperties() {
		String fileName = new File(appPath, FILE_DISPLAY_PROPERTIES).getPath();
		String txt ="[RDNII]\r\ndisplayid="+displayId+"\r\nclaimid="+claimId+"\r\nviewerurl="+viewerBaseUrl+"\r\ncoreurl="+coreBaseUrl+"\r\n";
		Utils.saveToFile(fileName, txt);
	}

//	public static void saveDisplayProperties() {
//
//		String fileName = FILE_DISPLAY_PROPERTIES;
//		try {
//			//displayProps.store(new FileOutputStream(new File(appPath, fileName)), null);
//			displayProps.setProperty(PROPERTY_DISPLAY_ID, displayId);
//			displayProps.setProperty(PROPERTY_CLAIM_ID, claimId);
//			FileOutputStream out = new FileOutputStream(new File(appPath, fileName));
//			//FileOutputStream out = new FileOutputStream(appPath + File.separator + fileName);
//			displayProps.store(out, null);
//			out.close();
//		} catch (Exception e) {
//			Log.warn("Error saving display properties. File name: " + fileName + ". Error: " + e.getMessage());
//		}
//	}

	public static String getViewerPropertiesPath() {
		return appPath + File.separator + "viewer.ini.txt"; //keep the "txt" extension for web intent filter to pass the file
	}

	public static String getViewerUrl() {
		return viewerBaseUrl + "?type=display&player=true&id=" + displayId + "&claimId=" + claimId + "&sysinfo=" + SystemInfo.asUrlParam(true);

	}

	public static void saveViewerStartupUrlToFile() {
		String cmd = "# This file is auto generated. Manual changes will be overwritten.";
		cmd += "\r\n" + "url=" + getViewerUrl();
		cmd += "\r\n" + "width=" + screenWidth;
		cmd += "\r\n" + "height=" + screenHeight;
		String fileName =  getViewerPropertiesPath();
		Utils.saveToFile(fileName, cmd);
	}

	private static void calculateScreenResolution() {
		try {
	        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	        GraphicsDevice[] screenDevices = ge.getScreenDevices();

	        for (GraphicsDevice screenDevice : screenDevices)
			{
				GraphicsConfiguration gc = screenDevice.getDefaultConfiguration();
				Rectangle gcBounds = gc.getBounds();
				if (screenWidth < gcBounds.x + gcBounds.width)
					screenWidth = gcBounds.x + gcBounds.width;
				if (screenHeight < gcBounds.y + gcBounds.height)
					screenHeight = gcBounds.y + gcBounds.height;
				System.out.println(screenDevice.getIDstring() + " " + gcBounds.toString());
			}
			
			System.out.println("screenWidth = " + screenWidth + "; screenHeight=" + screenHeight);
	        Log.info("screenWidth = " + screenWidth + "; screenHeight=" + screenHeight);
		} catch (Exception e) {
			Log.info("Erorr in calculateScreenResolution(): " + e.getMessage());
		}
		
	}

	public static Date getRestartTime() {
		return restartTime;
	}

	public static void setRestartTime(Date restartTime) {
		Config.restartTime = restartTime;
	}

	@SuppressWarnings("deprecation")
	public static void setRestartTime(String restartTime) {
		
		try {
			SimpleDateFormat df = new SimpleDateFormat("HH:mm");
			Date time = df.parse(restartTime);
			Date dt = new Date();
			dt.setHours(time.getHours());
			dt.setMinutes(time.getMinutes());
			dt.setSeconds(0);
			
			if (dt.before(new Date()))
				dt.setDate(dt.getDate() + 1);
			if (!dt.equals(Config.restartTime)) {
				Config.restartTime = dt;
				RestartTimer.restartIfTimeChanged();
			}

		} catch (Exception e) {
			//if problem parsing time, don't set restart time
			Log.error("Error parsing restart time \"" + restartTime + "\"");
		}
		
	}

	public static String getPlayerName() {
		return playerName;
	}
	
	public static int getExtendedModeDelay() {
		return extendedModeDelay;
	}
}
