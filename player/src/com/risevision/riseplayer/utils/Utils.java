package com.risevision.riseplayer.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import com.risevision.riseplayer.Config;
import com.risevision.riseplayer.DisplayErrors;
import com.risevision.riseplayer.Log;

public class Utils {
			
	public static void reboot() {
		
		DisplayErrors.getInstance().writeErrorsToFile();
		
		String[] shutdownCmdWindows = new String[] {"shutdown", "-r", "-c", "Rise Player needs to reboot computer."};
		String[] shutdownCmdUbuntu = new String[] {"bash", "-c", "dbus-send --system --print-reply --dest=org.freedesktop.ConsoleKit /org/freedesktop/ConsoleKit/Manager org.freedesktop.ConsoleKit.Manager.Restart"};
		String[] shutdownCmdUbuntuRoot = new String[] {"shutdown", "-r", "0"};

		String shutdownCmd[] = Config.isWindows ? shutdownCmdWindows : (Config.isLnxRoot ? shutdownCmdUbuntuRoot : shutdownCmdUbuntu);
		executeCommand(shutdownCmd, false);
	}

	public static void restart() {
		
		//just run Auto-Update script. 
		runAutoUpdateScript();

	}

	public static void displayStandby(boolean status) {
		
		try {
			File f = new File(Config.getDisplayStandbyFile());
			if(f.exists()) {
				String[] cmdWindows = new String[] {Config.getDisplayStandbyFile(), "-c", (status ? "on" : "off")};
				String[] cmdLinux = new String[] {"bash", "-c", Config.getDisplayStandbyFile() + " --c ", (status ? "on" : "off")};

				String displayStandbyCmd[] = Config.isWindows ? cmdWindows : cmdLinux;
				executeCommand(displayStandbyCmd, false);
				
			}	
		}
		catch(Exception e) {}
		

	}
	
	public static void restartViewer() {

		stopViewer();
		
		//pause before starting new instance. Otherwise newly started instance of Chrome can be killed by Chrome shutdown sequence.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		flushChromePreferences(); //the main purpose is to reset "exit_type":"Normal"/"Crashed" and "exited_cleanly":true/false flags. See issue 866.
		
		startViewer();
	}

	private static void flushChromePreferences() {
		saveToFile(Config.getChromePreferencesFilePath(), Config.CHROME_PREFERENCES);
	}

	public static void cleanChromeCache() {
		deleteDirectory(Config.getChromeCachePath());
	}
	
	public static void cleanChromeData() {
		deleteDirectory(Config.getChromeDataPath());
	}
	
	public static void startViewer() {
		
		System.out.println("startViewer");

		if (Config.chromeAppId == null || Config.chromeAppId.isEmpty()) 
			startViewer_OpenPage();
		else
			startViewer_StartPackagedApp();
		
		System.out.println("Chrome started");
	}

	private static boolean isExtenedModeEnabed() {
		try {
			File f = new File(Config.getExtendedModeFile());
			if(f.exists()) { 
				//Log.info("ExtenedMode is Enabed"); 
				return true;
			}	
		}
		catch(Exception e) {}
		
		return false;
	}
	public static void startViewer_OpenPage() {
		
	    //Windows: chrome.exe --kiosk --no-default-browser-check --noerrdialogs --no-message-box --disable-desktop-notifications --allow-running-insecure-content --user-data-dir="$INSTDIR\data" ${ViewerURL}$DisplayId'
		//Linux:       chrome --kiosk --no-default-browser-check --noerrdialogs --no-message-box --disable-desktop-notifications --allow-running-insecure-content --disk-cache-dir=$HOME/$CACHE_PATH --user-data-dir=$HOME/$CONFIG_PATH "$VIEWER_URL" 

		if (Config.isWindows) {
			if(isExtenedModeEnabed()) {
				int viewerDimensions[] = WindowUtils.getExtendedScreenSize();
				if(viewerDimensions[0] > 0 && viewerDimensions[1] > 0) {
					startViewer_ChromeApp_Windows(viewerDimensions);
					try {
						Thread.sleep(Config.getExtendedModeDelay());
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					run_extendedModeExe_Windows(viewerDimensions);
				}
				else
					startViewer_OpenPage_Windows();
			}
			else
				startViewer_OpenPage_Windows();
		}
		else
			startViewer_OpenPage_Linux();				
	}
	
	private static void startViewer_ChromeApp_Windows(int[] viewerDimensions) {
		
		Vector<String> cmd = new Vector<>();
		
		cmd.add(Config.chromePath);
		cmd.add("--flag-switches-begin");
		cmd.add("--noerrdialogs");
		cmd.add("--no-message-box");
		cmd.add("--disable-desktop-notifications");
		cmd.add("--allow-running-insecure-content");
		cmd.add("--always-authorize-plugins"); //this is for Java applets
		cmd.add("--allow-outdated-plugins");   //this is for Java applets
		cmd.add("--app-window-size=" + viewerDimensions[0] + "," + viewerDimensions[1]);
		cmd.add("--user-data-dir=" + Config.getChromeDataPath());
		cmd.add("--app=" + Config.getViewerUrl());
		cmd.add("--flag-switches-end");
		
		String[] sa = new String[cmd.size()];
		cmd.toArray(sa);

		executeCommand(sa, false);
		
	}

	private static void run_extendedModeExe_Windows(int[] viewerDimensions) {
		
		Vector<String> cmd = new Vector<>();
		
		cmd.add(Config.getExtendedModeFile());
		cmd.add("-title");
		cmd.add(Config.viewerAppTitle);
		cmd.add("-resize");
		cmd.add(viewerDimensions[0] + "*" + viewerDimensions[1]);
		cmd.add("-move");
		cmd.add("0*0");
		cmd.add("-naked"); //remove borders and screen title
		cmd.add("-top");   //
		cmd.add("-staytop");
		
		String[] sa = new String[cmd.size()];
		cmd.toArray(sa);

		executeCommand(sa, false);
		
	}
	
	private static void startViewer_OpenPage_Windows() {
		
		Vector<String> cmd = new Vector<>();
		
		cmd.add(Config.chromePath);
		cmd.add("--kiosk");
		cmd.add("--no-default-browser-check");
		cmd.add("--noerrdialogs");
		cmd.add("--no-message-box");
		cmd.add("--disable-desktop-notifications");
		cmd.add("--allow-running-insecure-content");
		cmd.add("--always-authorize-plugins"); //this is for Java applets
		cmd.add("--allow-outdated-plugins");   //this is for Java applets
		cmd.add("--user-data-dir=" + Config.getChromeDataPath());
		cmd.add(Config.getViewerUrl());

		String[] sa = new String[cmd.size()];
		cmd.toArray(sa);

		executeCommand(sa, false);
		
	}

	private static void startViewer_OpenPage_Linux() {

		StringBuilder cmd = new StringBuilder();
		
		cmd.append("'" + Config.chromePath + "'");
		cmd.append(" --kiosk");
		cmd.append(" --no-default-browser-check");
		cmd.append(" --noerrdialogs");
		cmd.append(" --no-message-box");
		cmd.append(" --disable-desktop-notifications");
		cmd.append(" --allow-running-insecure-content");
		cmd.append(" --always-authorize-plugins"); //this is for Java applets
		cmd.append(" --allow-outdated-plugins");   //this is for Java applets
		cmd.append(" --user-data-dir='" + Config.getChromeDataPath() + "'");
		cmd.append(" --disk-cache-dir='" + Config.getChromeCachePath() + "'");
		cmd.append(" --ppapi-flash-path='" + Config.getFlashPluginPath() + "'");
		cmd.append(" '" + Config.getViewerUrl() + "'");

		//output result into chromium.log file
		cmd.append(" > '" + Config.getChromeLogFilePath() + "' 2>&1");

		String[] sa = null;
		if(Config.isLnxRoot && !Config.playerOS.equals("rsp")) {
			sa = new String[] {"su", Config.linuxSudeUser, "-c", cmd.toString()};
		} else {
			sa = new String[] {"bash", "-c", cmd.toString()};
		}
		executeCommand(sa, false);
		
	}

	public static void startViewer_StartPackagedApp() {
				
		Config.saveViewerStartupUrlToFile();
		
		System.out.println("Saved Viewer Startup Url To File");
		
		//on linux: "/opt/google/chrome/google-chrome --app-id=cgojpamojdoefiffoopagdogpmiflbja --profile-directory=Default"
				
		String[] cmd = new String[] {Config.chromePath,
				"--profile-directory=Default",
				"--app-id=" + Config.chromeAppId,
				Config.getViewerPropertiesPath(),
				"--enable-experimental-extension-apis"};
		executeCommand(cmd, false);
		
		System.out.println("Chrome started");

		
	}

	public static void stopViewer() {

		if (Config.isWindows)
			killChrome_Windows();
		else
			killChrome_Linux();
	}

	public static void killChrome_Linux() {

		String[] cmd = new String[] {"killall", "chrome"};		
		executeCommand(cmd, true);
	}

	public static void killChrome_Windows() {
		// Problem:
		// "killall /im chrome.exe" kills only 1 instance of chrome at a time
		// "killall /f /im chrome.exe" forcefully kills all instances, but then Chrome shows "Chrome didn't shutdown correctly" message
		// Solution:
		// use tasklist command to check if chrome is running. Call "killall" repeatedly up to 3 times. Call "killall /f" after that.

		String[] cmdKillChrome = new String[] {"taskkill", "/im", "chrome.exe"};
		String[] cmdForceKillChrome = new String[] {"taskkill", "/f", "/im", "chrome.exe"};

		for (int i = 0; i < 3; i++) {
			if (findProcess("chrome.exe")) {
				executeCommand(cmdKillChrome, true);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				break;
			}
		}

		executeCommand(cmdForceKillChrome, true);
		
	}

	public static boolean findProcess(String processName) {

		String[] cmdTasklist = new String[] {"tasklist", "/FI", "IMAGENAME eq " + processName};
		ArrayList<String> cmdOut = executeCommand(cmdTasklist, true);
		
		for (String string : cmdOut) {
			if (string != null && string.toLowerCase().startsWith("chrome.exe")) {
				return true;
			}
		}

		return false;
	}

	public static void runAutoUpdateScript() {
		//Important: need to run script in background so when script closes Player it does not kill itself running as child process
		// "/S" = silent
		// "/C" = clear browser cache
		String[] cmd = new String[] {"bash", "-c", Config.appPath + File.separator + "rvplayer /S /C > " + Config.appPath + File.separator + "installer.log  2>&1 &"};
		if (Config.isWindows)
			cmd = new String[] {Config.appPath + File.separator + "RiseVisionPlayer.exe", "/S", "/C"};
		
		executeCommand(cmd, false);
	}

	public static ArrayList<String> executeCommand(String[] cmd, boolean waitForProcessToFinish) {

		ArrayList<String> res = new ArrayList<>();
		String strCmd = Arrays.toString(cmd);	
		Log.info("Exec: " + strCmd);

		Runtime run = Runtime.getRuntime();
		Process pr = null;

		try {
			pr = run.exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
			Log.error("Error: " + e.getMessage());
		}
		
		//check if process was created
		
		if (pr != null && waitForProcessToFinish) {

			try {
				//wait for process to finish
				pr.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log.error("Error: " + e.getMessage());
			}
			
			//read the output
			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = "";
			
			try {
				while ((line = buf.readLine()) != null) {
					res.add(line);
					System.out.println(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
				Log.error("Error: " + e.getMessage());
			}
			
		} else if (pr == null) {
			System.out.println("Execution has failed for command " + strCmd);
			Log.error("Execution has failed for command " + strCmd);
		}

		return res;
	}

	public static void setFlag_ClearCacheAfterReboot() {
		saveToFile(Config.getClearCacheFilePath(), "");		
	}

	public static void saveToFile(String fileName, String txt) {
		PrintWriter out;
		try {
			out = new PrintWriter(new FileWriter(fileName));
			out.print(txt);
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.error("error writtign to file : " + fileName);
			Log.error(e.getMessage());
		}
		
	}
	
	public static int StrToInt(String value, int defaultValue) {
		int res = defaultValue;
		try {
			res = Integer.parseInt(value);
		} catch (Exception e) {
		} 
		return res;
	}
	
	public static void deleteDirectory(String fileName)
    {	
 
    	File directory = new File(fileName);
 
    	//make sure directory exists
    	if(!directory.exists()){
    		//do nothing
        }else{
 
           try{
        	   deleteResursive(directory);
           }catch(IOException e){
               e.printStackTrace();
           }
        }
    }
	
	public static void deleteResursive(File file)
	    	throws IOException{
	 
	    	if(file.isDirectory()){
	 
	    		//directory is empty, then delete it
	    		if(file.list().length==0){
	    		   file.delete();
	    		}else{
	 
	    		   //list all the directory contents
	        	   String files[] = file.list();
	 
	        	   for (String temp : files) {
	        	      //construct the file structure
	        	      File fileDelete = new File(file, temp);
	        	      //recursive delete
	        	      deleteResursive(fileDelete);
	        	   }
	 
	        	   //check the directory again, if empty then delete it
	        	   if(file.list().length==0){
	           	     file.delete();
	        	   }
	    		}
	 
	    	}else{
	    		//if file, then delete it
	    		file.delete();
	    	}
	    }
	
//	private static String readFile(String fileName) {
//		String lineSeparator = System.getProperty("line.separator");
//		String res = "";
//		File file = new File(fileName);
//		if (file.exists()) {
//			try {
//				List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
//				if (lines != null && lines.size() > 0) {
//					for (String line : lines) {
//						res += (res.length() > 0 ? lineSeparator : "") + line;
//					}
//				}
//			} catch (IOException e) {
//			}
//		}
//		return res;
//	}

}
