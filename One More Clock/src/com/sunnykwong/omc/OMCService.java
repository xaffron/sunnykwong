package com.sunnykwong.omc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class OMCService extends Service {
	// First, a few flags for the service.
	static boolean RUNNING=false;	// Am I (already) running?
	static boolean STOPNOW4x2=false;		// Should I stop now? from 4x2 widgets
	static boolean STOPNOW4x1=false;		// Should I stop now? from 4x1 widgets
	static boolean STOPNOW3x1=false;		// Should I stop now? from 3x1 widgets
	static boolean STOPNOW2x1=false;		// Should I stop now? from 2x1 widgets
    static Method mStartForeground;
    static Method mStopForeground;
    static Object[] mStartForegroundArgs = new Object[2];
    static Object[] mStopForegroundArgs = new Object[1];

	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {
		if (OMC.DEBUG) Log.i("OMCSvc","---ONCREATE---");
		
		// If we're hogging the foreground, check APIs
		// Grab the notification Service -
		
		// Use reflection to find the methods for starting foreground using
		// newer API
		try {
			mStartForeground = getClass().getMethod("startForeground",
					OMC.mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					OMC.mStopForegroundSignature);
		} catch (NoSuchMethodException e) {
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}
	}
	
	@Override
    public void onDestroy() {
		// Make sure our notification is gone.
		stopForegroundCompat(OMC.SVCNOTIFICATIONID);
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {
		
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIME_TICK));
    	((OMC)getApplication()).widgetClicks();

    	handleCommand(intent);

		if (!OMCService.RUNNING || !OMC.FG) {
			// If we're not in FG, then go to sleep and let the alarm wake us up again
			// If we're supposed to stop, then just stop
			if (OMC.DEBUG)Log.i("OMCSvc","Stopping Svc");
			this.stopSelf();
		}
    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_ON));
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_SCREEN_OFF));
		getApplicationContext().registerReceiver(OMC.aRC, new IntentFilter(Intent.ACTION_TIME_TICK));
    	((OMC)getApplication()).widgetClicks();

		handleCommand(intent);

		if (!OMCService.RUNNING || !OMC.FG) {
			// If we're not in FG, then go to sleep and let the alarm wake us up again
			// If we're supposed to stop, then just stop
			if (OMC.DEBUG)Log.i("OMCSvc","Stopping Svc");
			this.stopSelf();
		}

		// We want intents redelivered and onStartCommand re-executed if the service is killed.
//		return 1;  // Service.START_STICKY ; have to use literal because Donut is unaware of the constant
		return 3;  // Service.START_REDELIVER_INTENT ; have to use literal because Donut is unaware of the constant
	}
	
	void handleCommand (Intent intent) {
		// If we come back from a low memory state, all sorts of screwy stuff might happen.
		// If the Intent itself is null, let's create one.
		if (intent == null) {
			OMC.FG=true;
			OMC.SCREENON=true;
		} else if (intent.getAction()==null) {
		// the Intent action might be blank.
		// In that case, we take an educated guess and say it's a foreground situation.
			OMC.FG=true;
			OMC.SCREENON=true;
		}
		
		// Take care of the notification business if we're running in FG
		if (OMC.FG) {

            // Set the info for the views that show in the notification panel.
			OMC.FGNOTIFICIATION.icon = R.drawable.transparent;
			
	        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.omc_notification);
	        contentView.setTextViewText(R.id.notf_text, "- One More Clock! in Foreground Mode -");
	        OMC.FGNOTIFICIATION.contentView = contentView;
			OMC.FGNOTIFICIATION.contentIntent = OMC.PREFSPENDING;
	        
	        startForegroundCompat(OMC.SVCNOTIFICATIONID, OMC.FGNOTIFICIATION);

		}
		
		// Either way...
		if (OMCService.STOPNOW4x2 && OMCService.STOPNOW4x1 && OMCService.STOPNOW3x1 && OMCService.STOPNOW2x1) {
			// If the widget tells me to stop, then stop setting the alarm and bail.
			// Do not refresh the widget
			// Do not fire off the Alarm
			// Do not pass GO
			// Do not collect $200
			OMCService.RUNNING=false;
		} else {
			//  Refresh at the next minute mark
			final long timeToRefresh = ((System.currentTimeMillis()+ OMC.UPDATEFREQ)/OMC.UPDATEFREQ) * OMC.UPDATEFREQ;

			//  But if it coincides with a time tick, don't worry about it
			if (timeToRefresh % 60000 == 0) {
				// Do nothing
				System.out.println("skipping alarm and letting time tick work.");
			} else {
				OMC.setServiceAlarm(timeToRefresh);
			}
		
			//	Tell the widgets to refresh themselves.
			OMCService.RUNNING=true;
			sendBroadcast(OMC.WIDGETREFRESHINTENT);
		}
	}

	// Sad, but I don't know what this is used for.  Someday.
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	//	 Code for starting/stopping the service in the foreground.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
    void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            try {
                mStartForeground.invoke(this, mStartForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
                if(OMC.DEBUG)Log.w("OMC", "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
            	if(OMC.DEBUG)Log.w("OMC", "Unable to invoke startForeground", e);
            }
            return;
        }

        // Fall back on the old API.
        setForeground(true);
        OMC.NM.notify(id, notification);
    }

    void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            try {
                mStopForeground.invoke(this, mStopForegroundArgs);
            } catch (InvocationTargetException e) {
                // Should not happen.
            	if(OMC.DEBUG)Log.w("OMC", "Unable to invoke stopForeground", e);
            } catch (IllegalAccessException e) {
                // Should not happen.
            	if(OMC.DEBUG)Log.w("OMC", "Unable to invoke stopForeground", e);
            }
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        OMC.NM.cancel(id);
        setForeground(false);
    }

}
