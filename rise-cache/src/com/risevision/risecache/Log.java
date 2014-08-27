package com.risevision.risecache;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log {
	private static Logger log = null;
	private static FileHandler fh;
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("MMM d hh:mm:ss.SSS");
	private static int fileSizeLimit = 1000000; //1MB
	private static int numberOfFilesToRotate = 1;
	
	public static void init(String path, String loggerName) {
		
		if (log != null) return;

		try {
			
			log = Logger.getLogger(loggerName);
			fh = new FileHandler(path + File.separator + loggerName + ".log", fileSizeLimit, numberOfFilesToRotate, true);

			java.util.logging.Formatter myFormatter = new Formatter() {
				@Override
				public String format(LogRecord record) {
					return sdf.format(new Date()) + " " + record.getLevel() + ": " + record.getMessage() + "\r\n";
				};
			};

			fh.setFormatter(myFormatter);
			//fh.setFormatter(new SimpleFormatter());
			
			log.addHandler(fh);
			log.setLevel(Level.INFO);
			
			
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void info(String msg) {
		if (log != null)
			log.info(msg);
	}

	public static void warn(String msg) {
		if (log != null)
			log.warning(msg);
	}

	public static void error(String msg) {
		if (log != null)
			log.severe(msg);
	}
	
}
