package com.sunnykwong.omc;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ClockWidget3x1 extends AppWidgetProvider {
	
	public ClockWidget3x1() {
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
		}
	}

//	When the very last widget is removed.
	public void onDisabled(Context context) {

		//Flag OMCService to stop.
		OMCService.STOPNOW3x1=true;
	}
		
//	This gets called when the very first widget is instantiated.
	public void onEnabled(Context context) {

		//Unflag the STOP FLAG for OMCService.
		OMCService.STOPNOW3x1=false;
	}
	
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
	        OMC.TIME.setToNow();				        					

	        OMCWidgetDrawEngine.updateAppWidget(context, 0.8f, 0.7f, OMC.WIDGET3x1CNAME, 15, 15);

			super.onReceive(context, intent);
		}
	}

	//	This should never fire since I implemented onReceive.
	@Override
	public void onUpdate(Context context, AppWidgetManager aWM, int[] appWidgetIds) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 500);
		}

		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
		  	OMC.initPrefs(appWidgetIds[i]);
		  	OMCWidgetDrawEngine.updateAppWidget(context, aWM, appWidgetIds[i], 0.8f, 0.7f, OMC.WIDGET3x1CNAME, 15,15);
		}

	}
	
}