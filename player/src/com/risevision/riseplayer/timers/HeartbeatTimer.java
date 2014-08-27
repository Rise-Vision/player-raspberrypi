package com.risevision.riseplayer.timers;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.risevision.riseplayer.Globals;
import com.risevision.riseplayer.Log;
import com.risevision.riseplayer.utils.Utils;

public class HeartbeatTimer {

	private static Date lastHearbeat = new Date();
	static class OnTimerTask extends TimerTask {

		@Override
		public void run() {
			Date now = new Date();
			if ((lastHearbeat.getTime() + Globals.MAX_HEARTBEAT_GAP_MS) < now.getTime()) {
				if ((lastHearbeat.getTime() + (Globals.MAX_HEARTBEAT_GAP_MS * 3)) < now.getTime()) {
					Log.error("Rise Viewer is not responding, clearing the browser cache and starting the viewer.");
					//DisplayErrors.getInstance().viewerNotResponding(1);
					Utils.cleanChromeCache();
					Utils.cleanChromeData();
					Utils.restartViewer();
				}
				else
					Utils.restartViewer();
			} else {
				//DisplayErrors.getInstance().viewerNotResponding(-1);
			}
		}

	}

	private static Timer timer;

	public static void start() {
		timer = new Timer();
		
		timer.schedule(new OnTimerTask(), Globals.HEARTBEAT_TIMER_INTERVAL_MS, Globals.HEARTBEAT_TIMER_INTERVAL_MS);
	}

	public static void recordHeartbeat() {
		lastHearbeat = new Date();		
	}

}

