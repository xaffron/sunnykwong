package com.sunnykwong.omc;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ClockWidget5x1 extends AppWidgetProvider {
	
	public ClockWidget5x1() {
		super();
	}

	//	When one or more widgets are removed...
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
			//Remove Prefs
			if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "Widget","Removed Widget #" + appWidgetIds[i]);
			OMC.removePrefs(appWidgetIds[i]);
	    	if (OMC.WIDGETBMPMAP.containsKey(appWidgetIds[i])) {
	    		if (!OMC.WIDGETBMPMAP.get(appWidgetIds[i]).isRecycled()) OMC.WIDGETBMPMAP.get(appWidgetIds[i]).recycle();
	    		OMC.WIDGETBMPMAP.remove(appWidgetIds[i]);
	    	}
		}
	}

//	When the very last widget is removed.
	public void onDisabled(Context context) {

		//Flag OMCService to stop.
		OMCService.STOPNOW5x1=true;
	}
		
//	This gets called when the very first widget is instantiated.
	public void onEnabled(Context context) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis()+500l, (System.currentTimeMillis()+500l)/1000l*1000l);
		}
		//Unflag the STOP FLAG for OMCService.
		OMCService.STOPNOW5x1=false;
	}
	
	// This fires when the OMC Service broadcasts the WIDGET_REFRESH intent.
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			Bundle extras = intent.getExtras();
			int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		}
		else {
			// Set time for output
	        OMCWidgetDrawEngine.updateAppWidget(context, OMC.WIDGET5x1CNAME);

			super.onReceive(context, intent);
		}
	}

	//	This fires when the homescreen requests an appwidget update.
	//  This is the final fallback when the clock lags.
	@Override
	public void onUpdate(Context context, AppWidgetManager aWM, int[] appWidgetIds) {
		
		// Restart the service if stopped.
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis()+500l, (System.currentTimeMillis()+500l)/1000l*1000l);
		}

		OMCWidgetDrawEngine.updateAppWidget(context, OMC.WIDGET5x1CNAME);

	}
	
}