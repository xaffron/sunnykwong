package com.sunnykwong.omc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class OMCService extends Service {
	// First, a few flags for the service.
//	static boolean RUNNING=true;	// Am I (already) running?
//	static boolean STOPNOW5x4=false;		// Should I stop now? from 5x4 widgets
//	static boolean STOPNOW5x2=false;		// Should I stop now? from 5x2 widgets
//	static boolean STOPNOW5x1=false;		// Should I stop now? from 5x1 widgets
//	static boolean STOPNOW4x4=false;		// Should I stop now? from 4x4 widgets
//	static boolean STOPNOW4x2=false;		// Should I stop now? from 4x2 widgets
//	static boolean STOPNOW4x1=false;		// Should I stop now? from 4x1 widgets
//	static boolean STOPNOW3x3=false;		// Should I stop now? from 3x3 widgets
//	static boolean STOPNOW3x1=false;		// Should I stop now? from 3x1 widgets
//	static boolean STOPNOW2x2=false;		// Should I stop now? from 2x2 widgets
//	static boolean STOPNOW2x1=false;		// Should I stop now? from 2x1 widgets
//	static boolean STOPNOW1x3=false;		// Should I stop now? from 1x3 widgets
//    static Method mStartForeground, mStopForeground, mSetForeground;
//    static Object[] mStartForegroundArgs = new Object[2];
//    static Object[] mStopForegroundArgs = new Object[1];
//    static Object[] mSetForegroundArgs = new Object[1];
//
//	//	 Code for oncreate/ondestroy.
//	//	 Code stolen wholesale from api samples:
//	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
//	//	
//	//	When service is created,
//    public OMCService() {
//		super("OMCService");
//	}
//
//	@Override
//	public void onCreate() {
//		// If we're hogging the foreground, check APIs
//		// Grab the notification Service -
//		
//		// Use reflection to find the methods for starting foreground using
//		// newer API
//		try {
//			mStartForeground = getClass().getMethod("startForeground",
//					OMC.mStartForegroundSignature);
//			mStopForeground = getClass().getMethod("stopForeground",
//					OMC.mStopForegroundSignature);
//			mSetForeground = null;
//		} catch (NoSuchMethodException e) {
//			// Running on an older platform.
//			mStartForeground = mStopForeground = null;
//			try {
//				mSetForeground = getClass().getMethod("setForeground",OMC.mSetForegroundSignature);
//			} catch (Exception ee) {
//				// do nothing
//                Log.w("OMC", "Unable to find any setForeground?!", e);
//				mSetForeground = null;
//			}
//		}
//	}
//	
//    // Overdue switch from a plain Jane Service to an IntentService.
//
//	@Override
//    protected void onHandleIntent(Intent intent) {
//		//	Tell the widgets to refresh themselves.
//		OMCService.RUNNING=true;
//
//		if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "Svc","Starting Svc");
//		// v130 edit:  trying to stamp out sync loss.
//		// if the service was restarted after low memory... reregister all my receivers.
//		// Because of Android issue #26574, I cannot depend on START_FLAG_RETRY being accurate. 
//		if (intent==null) {
//			Log.w(OMC.OMCSHORT + "Svc","Null Intent - Reset Alarm.");
//			OMC.setServiceAlarm(System.currentTimeMillis()+500l, (System.currentTimeMillis()+500l)/1000l*1000l);
//		}
//
//		// These are two system intents that Android forces us to register programmatically
//		// We register everything we can through manifest because ICS's ActivityManager will
//		// randomly wipe out OMC's registered receivers when it kills OMC on low memory.
//		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
//		registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//
//		OMC.setServiceAlarm(System.currentTimeMillis()+500l, (System.currentTimeMillis()+500l)/1000l*1000l);
//
//		
//		getApplicationContext().sendBroadcast(OMC.WIDGETREFRESHINTENT);
//
//		handleCommand(intent);
//
//	}
//    	
// 	void handleCommand (Intent intent) {
//		// If we come back from a low memory state, all sorts of screwy stuff might happen.
//		// If the Intent itself is null, let's create one.
//		if (intent == null) {
//			OMC.FG=true;
//			OMC.SCREENON=true;
//		} else if (intent.getAction()==null) {
//		// the Intent action might be blank.
//		// In that case, we take an educated guess and say it's a foreground situation.
//			OMC.FG=true;
//			OMC.SCREENON=true;
//		}
//		
//	}
//
//   
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
