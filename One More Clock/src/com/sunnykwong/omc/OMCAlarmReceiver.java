package com.sunnykwong.omc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OMCAlarmReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		//if (OMC.DEBUG) Log.i("OMCAlarm","Rcvd " + intent.getAction());
		
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

		if (action.equals(OMC.FGINTENT)) OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.FGSERVICE");
		else OMC.SVCSTARTINTENT.setAction("com.sunnykwong.omc.BGSERVICE");
		
		if (action.equals(Intent.ACTION_SCREEN_ON)) {
			OMC.SCREENON=true;
			if (OMC.DEBUG) Log.i("OMCAlarm","Scrn on - Refreshing");
		}
	
		if (action.equals(Intent.ACTION_SCREEN_OFF)) {
			OMC.SCREENON=false;
			if (OMC.DEBUG) Log.i("OMCAlarm","Scrn off - not refreshing");
		}
		
		if (OMC.SCREENON) {
			context.startService(OMC.SVCSTARTINTENT);
		}
	}
}
