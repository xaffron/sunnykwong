package com.sunnykwong.omc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;

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
	public void onReceive(final Context context, final Intent intentt) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Rcvd " + intentt.toString());
		// Set the alarm for next tick first, so we don't lose sync
		// targettime = Time we are rendering for next tick
		// thisticktime = Time we are rendering for this tick
		long thisticktime, targettime;
		
		thisticktime = intentt.getLongExtra("target", (System.currentTimeMillis() + OMC.LEASTLAGMILLIS)/OMC.UPDATEFREQ * OMC.UPDATEFREQ);
		if (thisticktime < System.currentTimeMillis()) {
			thisticktime = System.currentTimeMillis()/OMC.UPDATEFREQ * OMC.UPDATEFREQ;
		}
		final long omctime = thisticktime;
		targettime = omctime + OMC.UPDATEFREQ;
		
		// If we come back from a low memory state, all sorts of screwy stuff might happen.

		final String action = intentt.getAction()==null?OMC.FGSTRING:intentt.getAction();
		
		if(action.equals("android.intent.action.BOOT_COMPLETED")) {
			OMC.setServiceAlarm(thisticktime+30*1000l, thisticktime+30*1000l);
		} else {
			OMC.setServiceAlarm(targettime - OMC.LEASTLAGMILLIS, targettime);
		}
		
		// the Intent action might be blank.
		// In that case, we take an educated guess and say it's a foreground situation.
		if (action==null) {
			OMC.FG=true;
			OMC.SCREENON=true;
			//OMC.SVCSTARTINTENT.setAction(OMC.FGSTRING);
		}
		
		// If user taps on notification, cancel FG mode.
		if (action.equals(OMC.CANCELFGSTRING)) {
			OMC.FG=false;
			//OMC.SVCSTARTINTENT.setAction(OMC.BGSTRING);
			OMC.LASTRENDEREDTIME.set(((omctime+OMC.LEASTLAGMILLIS)/1000l)*1000l);
//			OMC.CONTEXT.startService(OMC.SVCSTARTINTENT);
			OMC.CONTEXT.sendBroadcast(OMC.WIDGETREFRESHINTENT);
			return;
		}
				
		final Intent intent = new Intent(intentt);

		// Pop the weather refresh toast before we give up context
		if (action.equals(OMC.WEATHERREFRESHSTRING)) {
			Toast.makeText(context, OMC.RString("refreshWeatherNow"), Toast.LENGTH_LONG).show();
		}	
		
		//
		//v1.4.1 moving the bulk of processing to a separate thread to release the wakelock quickly
		//hopefully this will resolve most of the wakelock/kernel bug issues with battery drain
		//
		Thread t = new Thread() {
			public void run() {
				// Reset Alternate Rendering switch (may have gotten overridden when cache dir unavailable)
				OMC.ALTRENDERING = OMC.PREFS.getBoolean("AltRendering",true);
				// Battery-related responses.
				// If something about the battery changed, we need to record the changes.
				if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
					String sChargeStatus = "Discharging";
					OMC.CHARGESTATUSCODE = intent.getIntExtra("plugged", 0);
					switch (OMC.CHARGESTATUSCODE) {
					case BatteryManager.BATTERY_PLUGGED_AC: 
						sChargeStatus="AC Charging";
						break;
					case BatteryManager.BATTERY_PLUGGED_USB:
						sChargeStatus="USB Charging";
						break;
					case 4: // BATTERY_PLUGGED_WIRELESS
						sChargeStatus="Wireless Charging";
						break;
					case 0:
						break;
					default:
						break;
					}

					// Note that especially when the charge status changes 
					// from plugged to unplugged, we want to update the widget asap.
					// v1.3.3:   NOTE THAT WE *ONLY* WANT TO UPDATE ASAP if CHARGE STATUS CHANGED
					// to avoid serious battery drain on weaker batteries!!
					// v1.4.1:   Motorola battery fix.
					// Adapted from "The One-Percent Hack" by Josiah Barber of Darshan Computing, LLC.
					// Thanks, Josiah!
					File fChargeFile = new File("/sys/class/power_supply/battery/charge_counter");
					if (OMC.PREFS.getBoolean("battReporting",true)==false && fChargeFile.exists()) {
						try {
							FileInputStream fis = new FileInputStream(fChargeFile);
							OMC.BATTLEVEL = Math.min(Integer.parseInt(OMC.streamToString(fis).trim()),100);
							fis.close();
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","CHARGECOUNTER BATT%: " + OMC.BATTLEVEL);
							OMC.BATTSCALE = intent.getIntExtra("scale", 100);
							OMC.BATTPERCENT = (int)(100*OMC.BATTLEVEL/(float)intent.getIntExtra("scale", 100));
							OMC.CHARGESTATUS = sChargeStatus;
						} catch (IOException e) {
							e.printStackTrace();
							OMC.BATTLEVEL = intent.getIntExtra("level", 0);
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","REPORTED BATT%: " + OMC.BATTLEVEL);
							OMC.BATTSCALE = intent.getIntExtra("scale", 100);
							OMC.BATTPERCENT = (int)(100*OMC.BATTLEVEL/(float)intent.getIntExtra("scale", 100));
							OMC.CHARGESTATUS = sChargeStatus;
						}
					} else {
						OMC.BATTLEVEL = intent.getIntExtra("level", 0);
						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","REPORTED BATT%: " + OMC.BATTLEVEL);
						OMC.BATTSCALE = intent.getIntExtra("scale", 100);
						OMC.BATTPERCENT = (int)(100*OMC.BATTLEVEL/(float)intent.getIntExtra("scale", 100));
						OMC.CHARGESTATUS = sChargeStatus;
					}

					if (OMC.LASTBATTERYPLUGGEDSTATUS != OMC.CHARGESTATUSCODE) {
						// Update the current plugged status
						OMC.LASTBATTERYPLUGGEDSTATUS = OMC.CHARGESTATUSCODE;
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
					.putInt("ompc_chargestatuscode", OMC.CHARGESTATUSCODE)
					.commit();
					OMC.NEXTBATTSAVEMILLIS=omctime+900000l;
				}
				
				// Weather-related responses.
				// If user taps on hotspot for refresh weather, refresh weather.
				if (action.equals(OMC.WEATHERREFRESHSTRING)) {
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
				
				//if (action.equals(OMC.FGINTENT)) OMC.SVCSTARTINTENT.setAction(OMC.FGSTRING);
				//else OMC.SVCSTARTINTENT.setAction(OMC.BGSTRING);

				// When Clock Priority is Medium or above:
				// Do nothing if the screen turns off, but
				// Start working again if the screen turns on.
				// This obviously saves CPU cycles (and battery).
				// Thanks to ralfoide's code at http://code.google.com/p/24clock/ for the idea
				
				
				boolean bForceUpdate=false;
				OMC.IDLEMODE=false;
				
				// v149: Force full-res update when screen turns on; 
				// no ifs ands or buts
				if (action.equals(Intent.ACTION_SCREEN_ON)) {
					OMC.SCREENON=true;
					OMC.LASTUPDATEMILLIS=0l;
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn switched on.");
					OMC.IDLEMODE=false;
					bForceUpdate=true;

					
				} 
				// v149: Force full-res update when screen is on and launcher is on top
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","FG App is "+ OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName());
				if (OMC.SCREENON &&
					OMC.INSTALLEDLAUNCHERAPPS.contains(
							OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName()
						)
					) {
				if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Full update: Launcher "+ OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName() +" in FG.");

				}
				
				if (action.equals(Intent.ACTION_SCREEN_OFF)) {
					OMC.SCREENON=false;
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Scrn switched off.");
					// v149:  If clock priority is medium or lower, lower render resolution
					if (OMC.CURRENTCLOCKPRIORITY>=2) {
						OMC.IDLEMODE=true;
					}
				}
				
				//If OMC is set to highest priority, every tick is a forced update at full resolution.
				if (OMC.CURRENTCLOCKPRIORITY==0) {
					OMC.IDLEMODE=false;
					bForceUpdate=true;
				//If OMC is set to high priority, every minute mark is a forced update at full resolution.
				} else if (OMC.CURRENTCLOCKPRIORITY==1
						 && new Date(omctime).getSeconds()==0) {
					OMC.IDLEMODE=false;
					bForceUpdate=true;
				//If OMC is set to medium priority, lazy draw/minute when screen on OR at minute mark when in BG.
				} else if (OMC.CURRENTCLOCKPRIORITY==2) {
					if (OMC.SCREENON) OMC.IDLEMODE=false;
					if (new Date(omctime).getSeconds()==0 || OMC.SCREENON)
						bForceUpdate=true;
				// If at low clock priority, lazy draw/minute when (screen on & launcher in FG) OR at minute mark when in BG.
				} else if (OMC.CURRENTCLOCKPRIORITY==3) {
					if (OMC.SCREENON &&
							OMC.INSTALLEDLAUNCHERAPPS.contains(
								OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName()
							)) {
						OMC.IDLEMODE=false;
						bForceUpdate=true;
					} else if (new Date(omctime).getSeconds()==0) {	
						if (OMC.SCREENON) {
							OMC.IDLEMODE=false;
						} else {
							OMC.IDLEMODE=true;
						}
						bForceUpdate=true;
					}
				// If at lowest clock priority, lazy draw when screen on & launcher in FG.  No draw otherwise, period.
				} else if (OMC.CURRENTCLOCKPRIORITY==4) {
					if (OMC.SCREENON &&
							OMC.INSTALLEDLAUNCHERAPPS.contains(
									OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName()
								)
							) {
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Launcher "+ OMC.ACTM.getRunningTasks(1).get(0).topActivity.getPackageName() +" running.");
							OMC.IDLEMODE=false;
							bForceUpdate=true;
					} else {
						OMC.IDLEMODE=true;
					}
				}
				
				// If it is a forced update, update.
				if (bForceUpdate) {
					OMC.LASTRENDEREDTIME.set(omctime);
					//OMC.CONTEXT.startService(OMC.SVCSTARTINTENT);
					OMC.CONTEXT.sendBroadcast(OMC.WIDGETREFRESHINTENT);

				// If time/zone changed, also force update.
				} else if (action.equals(Intent.ACTION_TIME_CHANGED)
						||action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
					OMC.LASTRENDEREDTIME.set(omctime);
					//OMC.CONTEXT.startService(OMC.SVCSTARTINTENT);
					OMC.CONTEXT.sendBroadcast(OMC.WIDGETREFRESHINTENT);

				// Otherwise, skip the redraw.
				} else {
					OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
					if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Skipping refresh.");
				}
				
			};
		};
		t.start();
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Alarm","Thread launched - alarmreceiver exiting.");
	}
}
