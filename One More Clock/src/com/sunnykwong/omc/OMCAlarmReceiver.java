package com.sunnykwong.omc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

public class OMCAlarmReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Rcvd " + intent.toString());
		// Set the alarm for next tick first, so we don't lose sync
		// targettime = Time we are rendering for next tick
		// omctime = Time we are rendering for this tick
		boolean bForceUpdate=false;
		long omctime, targettime;
		omctime = intent.getLongExtra("target", (System.currentTimeMillis() + OMC.LEASTLAGMILLIS)/OMC.UPDATEFREQ * OMC.UPDATEFREQ);
		if (omctime < System.currentTimeMillis()) {
			omctime = System.currentTimeMillis()/OMC.UPDATEFREQ * OMC.UPDATEFREQ;
		}
		targettime = omctime + OMC.UPDATEFREQ;

		OMC.setServiceAlarm(targettime - OMC.LEASTLAGMILLIS, targettime);
		
		// If we come back from a low memory state, all sorts of screwy stuff might happen.

		final String action = intent.getAction();

		// the Intent action might be blank.
		// In that case, we take an educated guess and say it's a foreground situation.
		if (action==null) {
			OMC.FG=true;
			OMC.SCREENON=true;
			OMC.SVCSTARTINTENT.setAction(OMC.FGSTRING);
		}
		// If user taps on notification, cancel FG mode.
		if (action.equals(OMC.CANCELFGSTRING)) {
			OMC.FG=false;
			OMC.SVCSTARTINTENT.setAction(OMC.BGSTRING);
			OMC.LASTRENDEREDTIME.set(((omctime+OMC.LEASTLAGMILLIS)/1000l)*1000l);
			context.startService(OMC.SVCSTARTINTENT);
		}
				
		// Battery-related responses.
		// If something about the battery changed, we need to record the changes.
		if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
//			if (OMC.DEBUG) Log.i (OMC.OMCSHORT + "Alarm","Batt "+ intent.getIntExtra("level", 0) + "/" +intent.getIntExtra("scale", 10000));
//			if (OMC.DEBUG) Log.i (OMC.OMCSHORT + "Alarm","ChargeStatus: "+ action);
//			if (OMC.DEBUG) Log.i (OMC.OMCSHORT + "Alarm",""+intent.getIntExtra("plugged", -1));
			String sChargeStatus = "Discharging";
			final int iNewBatteryPluggedStatus = intent.getIntExtra("plugged", -1);
			switch (iNewBatteryPluggedStatus) {
			case BatteryManager.BATTERY_PLUGGED_AC: 
				sChargeStatus="AC Charging";
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				sChargeStatus="USB Charging";
				break;
			case -1:
				break;
			default:
				break;
			}

			// Note that especially when the charge status changes 
			// from plugged to unplugged, we want to update the widget asap.
			// v1.3.3:   NOTE THAT WE *ONLY* WANT TO UPDATE ASAP if CHARGE STATUS CHANGED
			// to avoid serious battery drain on weaker batteries!!
			OMC.BATTLEVEL = intent.getIntExtra("level", 0);
			OMC.BATTSCALE = intent.getIntExtra("scale", 100);
			OMC.BATTPERCENT = (int)(100*intent.getIntExtra("level", 0)/(float)intent.getIntExtra("scale", 100));
			OMC.CHARGESTATUS = sChargeStatus;

//			OMC.BATTVOLTAGESCALE.put(OMC.BATTPERCENT, intent.getIntExtra("voltage",0));
//			if (OMC.BATTVOLTAGESCALE.size()>1) {
//				int lowx=0, lowy=0, highx=0, highy=0;
//				boolean foundvoltage=false;
//				for (int i=0;i<=100;i+=10) {
//					if (OMC.BATTVOLTAGESCALE.containsKey(i) && !foundvoltage) {
//						lowx = i;
//						lowy = OMC.BATTVOLTAGESCALE.get(i);
//					} else if (OMC.BATTVOLTAGESCALE.containsKey(i) && foundvoltage) {
//						highx = i;
//						highy = OMC.BATTVOLTAGESCALE.get(i);
//						break;
//					}
//					if (OMC.BATTPERCENT - i < 10) {
//						foundvoltage=true;
//					}
//				}
//				OMC.battpercent = 
//			}
			
			if (OMC.LASTBATTERYPLUGGEDSTATUS != iNewBatteryPluggedStatus) {
				// Update the current plugged status
				OMC.LASTBATTERYPLUGGEDSTATUS = iNewBatteryPluggedStatus;
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Battery now "+ sChargeStatus +" - refresh widget");
			} else {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","BattLevel now " + OMC.BATTLEVEL + " - no refresh");
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Batt Level Change Only - no refresh");
				return;
			}	
			
		}

		// v1.3.4:  We don't want to write to flash/SD every 5 seconds either.  Batch up the edits and write out
		// every fifteen minutes.
		if (omctime > OMC.NEXTBATTSAVEMILLIS) {
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Flushing BattLevels to SharedPrefs");
	    	OMC.PREFS.edit()
    		.putInt("ompc_battlevel", OMC.BATTLEVEL)
			.putInt("ompc_battscale", OMC.BATTSCALE)
			.putInt("ompc_battpercent", OMC.BATTPERCENT)
			.putString("ompc_chargestatus", OMC.CHARGESTATUS)
			.commit();
			OMC.NEXTBATTSAVEMILLIS=omctime+900000l;
		}
		
		// Weather-related responses.
		// If user taps on hotspot for refresh weather, refresh weather.
		if (action.equals(OMC.WEATHERREFRESHSTRING)) {
			Toast.makeText(OMC.CONTEXT, OMC.RString("refreshWeatherNow"), Toast.LENGTH_LONG).show();
			OMC.updateWeather();
		// If we just set the clock or switched timezones, we definitely want to refresh weather right now.
		} else if (action.equals(Intent.ACTION_TIME_CHANGED)
				|| action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
			if (Integer.parseInt(OMC.PREFS.getString("sWeatherFreq", "60"))!=0)
				OMC.updateWeather();
		} else {
			// Otherwise, we can be more polite about updating weather.
			// First, are we due for a weather update?
			if (omctime>OMC.NEXTWEATHERREFRESH && Integer.parseInt(OMC.PREFS.getString("sWeatherFreq", "60"))!=0) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","OMCTIME > NEXTWEATHER");
				// If the last weather try has been recent, don't try yet
				if (omctime-OMC.LASTWEATHERTRY < Long.parseLong(OMC.PREFS.getString("sWeatherFreq", "60"))/4l * 60000l) {
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Too frequent, wait it out");
					// do nothing
				} else {
					// Get weather updates
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Get Weather Udpates");
					OMC.updateWeather();
				}
			}
		}
		
		if (action.equals(OMC.FGINTENT)) OMC.SVCSTARTINTENT.setAction(OMC.FGSTRING);
		else OMC.SVCSTARTINTENT.setAction(OMC.BGSTRING);

		// When Clock Priority is Medium or above:
		// Do nothing if the screen turns off, but
		// Start working again if the screen turns on.
		// This obviously saves CPU cycles (and battery).
		// Thanks to ralfoide's code at http://code.google.com/p/24clock/ for the idea
		
		
		if (action.equals(Intent.ACTION_SCREEN_ON)) {
			OMC.SCREENON=true;
			OMC.LASTUPDATEMILLIS=0l;
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn switched on.");
		}
	
		if (action.equals(Intent.ACTION_SCREEN_OFF)) {
			OMC.SCREENON=false;
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn switched off.");
		}

		//If OMC is set to highest priority, every tick is a forced update.
		if (OMC.CURRENTCLOCKPRIORITY==0) {
			bForceUpdate=true;
		//If OMC is set to high priority, every minute mark is a forced update.
		} else if (OMC.CURRENTCLOCKPRIORITY==1
				 && new Date(omctime).getSeconds()==0) {
			bForceUpdate=true;
		//If OMC is set to medium priority, lazy draw/minute when screen on OR at minute mark when in BG.
		} else if (OMC.CURRENTCLOCKPRIORITY==2
				 && (new Date(omctime).getSeconds()==0
				 	|| OMC.SCREENON)) {
			bForceUpdate=true;
		// If at low clock priority, lazy draw/minute when (screen on & launcher in FG) OR at minute mark when in BG.
		} else if (OMC.CURRENTCLOCKPRIORITY==3 && (
				new Date(omctime).getSeconds()==0 || (
					OMC.SCREENON &&
					OMC.INSTALLEDLAUNCHERAPPS.contains(
						OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName()
					)
				)
			)) {
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Launcher "+ OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName() +" running.");
			bForceUpdate=true;
		// If at lowest clock priority, lazy draw when screen on & launcher in FG.  No draw otherwise, period.
		} else if (OMC.CURRENTCLOCKPRIORITY==4 && (
				OMC.SCREENON &&
				OMC.INSTALLEDLAUNCHERAPPS.contains(
					OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName()
				)
			)) {
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Launcher "+ OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName() +" running.");
			bForceUpdate=true;
		}
		
		// If it is a forced update, update.
		if (bForceUpdate) {
			OMC.LASTRENDEREDTIME.set(omctime);
			context.startService(OMC.SVCSTARTINTENT);

		// If time/zone changed, also force update.
		} else if (action.equals(Intent.ACTION_TIME_CHANGED)
				||action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
			OMC.LASTRENDEREDTIME.set(omctime);
			context.startService(OMC.SVCSTARTINTENT);

		// Otherwise, skip the redraw.
		} else {
			OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
			if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Skipping refresh.");
		}
	}
}
