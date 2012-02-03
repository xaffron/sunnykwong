package com.sunnykwong.omc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OMCAlarmReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// Set the alarm for next tick first, so we don't lose sync
//		OMC.setServiceAlarm(((System.currentTimeMillis()+ 5000 + OMC.UPDATEFREQ )/OMC.UPDATEFREQ) * OMC.UPDATEFREQ - OMC.LEASTLAGMILLIS);
		OMC.setServiceAlarm(((System.currentTimeMillis() + OMC.UPDATEFREQ )/OMC.UPDATEFREQ) * OMC.UPDATEFREQ - OMC.LEASTLAGMILLIS);

		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Rcvd " + intent.getAction());
		
		
		// If we come back from a low memory state, all sorts of screwy stuff might happen.
		// If the Intent itself is null, let's create one.
		if (intent == null) {
			OMC.FG=true;
			OMC.SCREENON=true;
			OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.FGSERVICE");
		}

		final String action = intent.getAction();

		// the Intent action might be blank.
		// In that case, we take an educated guess and say it's a foreground situation.
		if (action==null) {
			OMC.FG=true;
			OMC.SCREENON=true;
			OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.FGSERVICE");
		}

		if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
			if (OMC.DEBUG) Log.i (OMC.OMCSHORT + "Alarm","Batt "+ intent.getIntExtra("level", 0) + "/" +intent.getIntExtra("scale", 10000));
			OMC.PREFS.edit()
				.putInt("ompc_battlevel", intent.getIntExtra("level", 0))
				.putInt("ompc_battscale", intent.getIntExtra("scale", 100))
				.putInt("ompc_battpercent", (int)(100*intent.getIntExtra("level", 0)/(float)intent.getIntExtra("scale", 100)))
				.commit();
			return;
		}
		
		if (action.equals(OMC.FGINTENT)) OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.FGSERVICE");
		else OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.BGSERVICE");
		
		// Do nothing if the screen turns off, but
		// Start working again if the screen turns on.
		// This obviously saves CPU cycles (and battery).
		// Thanks to ralfoide's code at http://code.google.com/p/24clock/ for the idea
		if (action.equals(Intent.ACTION_SCREEN_ON)) {
			OMC.SCREENON=true;
			OMC.LASTUPDATEMILLIS=0l;
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn on - Refreshing");
		}
	
		if (action.equals(Intent.ACTION_SCREEN_OFF)) {
			OMC.SCREENON=false;
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn off - not refreshing");
		}
		
		
		// If the screen is on, honor the update frequency.
		if (OMC.SCREENON) {
			// Prevent abusive updates - update no more than every 5 secs.
//			if (System.currentTimeMillis()-OMC.LASTUPDATEMILLIS < 5000 && (action.equals(OMC.FGINTENT) || action.equals(OMC.BGINTENT))) {
//				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Abusive; aborting update " + System.currentTimeMillis() + " " + OMC.LASTUPDATEMILLIS);
//				return;
//			}
			OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
			context.startService(OMC.SVCSTARTINTENT);
		// If the screen is off, update bare minimum to mimic foreground mode.
		} else if (action.equals(Intent.ACTION_TIME_TICK)
				||action.equals(Intent.ACTION_TIME_CHANGED)
				||action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
			OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
			context.startService(OMC.SVCSTARTINTENT);
		} else {
			OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","I think scrn is off... no refresh");
		}
	}
}
