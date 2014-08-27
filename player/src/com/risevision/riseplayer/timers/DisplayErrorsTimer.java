package com.risevision.riseplayer.timers;

import java.util.Timer;
import java.util.TimerTask;

import com.risevision.riseplayer.DisplayErrors;

public class DisplayErrorsTimer {

	public static final long DISPLAYERROR_TIMER_INTERVAL_MS = 60 * 60 * 1000;
	
	static class OnTimerTask extends TimerTask {

		@Override
		public void run() {
			DisplayErrors.getInstance().reportDisplayErrorstoCore();
		}

	}

	private static Timer timer;

	public static void start() {
		timer = new Timer();
		
		timer.schedule(new OnTimerTask(), 0, DISPLAYERROR_TIMER_INTERVAL_MS);
	}
	
}
