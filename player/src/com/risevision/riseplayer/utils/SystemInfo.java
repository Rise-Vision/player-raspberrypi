package com.risevision.riseplayer.utils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import com.risevision.riseplayer.Config;

public class SystemInfo {

	private static boolean isInitialized = false;
	private static String os_name = null;
	private static String player_name = null;
	private static String installer_version = null;
	//private static String chromium_version = null;
	private static String java_version = null;
	private static String risePlayer_version = null;
	private static String riseCache_version = null;

	
//	public static String asJSON() {
//		
//		init();
//		
//		String res = toJsonObject("os", System.getProperty("os.name"), System.getProperty("os.version"));
////		res += ", " + toJsonObject("player", "Rise Player", Globals.APPLICATION_VERSION);
////		res += ", " + toJsonObject("java", "Java", System.getProperty("java.version"));
//		res += ", " + toJsonObject("iv", "Installer", installer_version);
//		res += ", " + toJsonObject("cv", "Chromium", chromium_version);
////		res += ", " + toJsonObject("fv", "Flash", flash_version);
//		res += ", " + toJsonObject("jv", "Java", java_version);
//		res += ", " + toJsonObject("pv", "Rise Player", risePlayer_version);
//		res += ", " + toJsonObject("ev", "Rise Cache", riseCache_version);
//			
//		return "{" + res + "}";
//	}

	public static String asUrlParam(boolean encode) {
		
		init();

		//example: "os={OS}&iv={InstallerVersion}&cv={CurrentChromiumVersion}&jv={CurrentJavaVersion}&ev={CurrentRiseCacheVersion}&pn={RisePlayerName}&pv={CurrentRisePlayerVersion}";
		
		StringBuilder sb = new StringBuilder();
		sb.append("os=" + os_name);
		sb.append("&iv=" + installer_version);
		//sb.append("&cv=" + chromium_version); //chrome version is added in Viewer
		if (java_version != null && !java_version.isEmpty())
			sb.append("&jv=" + java_version);
		sb.append("&pn=" + player_name);
		sb.append("&pv=" + risePlayer_version);
		sb.append("&ev=" + riseCache_version);
		//if(os_name.equals("lnx"))
		//	sb.append("&up=" + (Config.isLnxRoot ? "1" : "0") ); //user privileges up=1 "root" else up=0 any other user
			
		String res = sb.toString();
		
		if (encode) {
			try {
				res = URLEncoder.encode(sb.toString(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				res = "";
			}
		}
		
		return res;
	}

	public static void init() {
		if (!isInitialized) {
			isInitialized = true;
			//os_name = Config.isWindows ? "win" : "lnx";
			os_name = Config.playerOS;
			player_name = Config.getPlayerName();
			installer_version = readVersion("installer.ver");
			//chromium_version = readVersion("chromium.ver");
			java_version = readVersion("java.ver");
			risePlayer_version = readVersion("RisePlayer.ver");
			riseCache_version = readVersion("RiseCache.ver");
		}
	}

	private static String readVersion(String fileName) {
		String res = "";
		File file = new File(Config.appPath, fileName);
		if (file.exists()) {
			try {
				List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
				if (lines != null && lines.size() > 0)
					res = lines.get(0);
			} catch (IOException e) {
			}
		}
		return res;
	}

//	private static String toJsonObject(String className, String name, String version) {
//		return "\"" + className + "\": {\"name\": \"" + name + "\", \"version\": \"" + version + "\"}";
//	}

}
