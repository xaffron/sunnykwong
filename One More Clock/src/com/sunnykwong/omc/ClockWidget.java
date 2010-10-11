package com.sunnykwong.omc;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Style;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {
	
	static String sCHINESETIME;
	
	public ClockWidget() {
		super();
	}
	
	static void drawBitmapForWidget(final Context context, final int aWI) {
		OMC.BUFFER.eraseColor(Color.TRANSPARENT);

		String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,"CultureClash");
		//		Set LAF based on prefs
		if (sTheme.equals("CultureClash")) {
			OMC.CACHEDATTRIBS = context.getResources().obtainTypedArray(R.array.CultureClash);
		} else if (sTheme.equals("LockscreenLook")) {
			OMC.CACHEDATTRIBS = context.getResources().obtainTypedArray(R.array.LockscreenLook);
		} else if (sTheme.equals("DigitalDigits")) {
			OMC.CACHEDATTRIBS = context.getResources().obtainTypedArray(R.array.DigitalDigits);
		} else if (sTheme.equals("WhamBamWidget")) {
			OMC.CACHEDATTRIBS = context.getResources().obtainTypedArray(R.array.WhamBamWidget);
		} else if (sTheme.equals("BokehBeauty")) {
			OMC.CACHEDATTRIBS = context.getResources().obtainTypedArray(R.array.BokehBeauty);
		}
		
		Typeface widTypeface = OMC.getTypeface(OMC.CACHEDATTRIBS.getString(0),OMC.CACHEDATTRIBS.getString(1));

		// The huge Chinese Text in the back.
		OMC.BGPT1.reset();
		OMC.BGPT1.setAntiAlias(true);
		OMC.BGPT1.setTypeface(widTypeface);
		OMC.BGPT1.setTextSize(190);
		OMC.BGPT1.setTextAlign(Paint.Align.CENTER);
		OMC.BGPT1.setTextSkewX((float)-0.25);
		OMC.BGPT1.setTextScaleX((float)1.3);

		OMC.FGPT1.reset();
		OMC.FGPT1.setAntiAlias(true);
		OMC.FGPT1.setTypeface(widTypeface);
		OMC.FGPT1.setTextSize((float)OMC.CACHEDATTRIBS.getInt(2,75));

		OMC.FGPT2.reset();
		OMC.FGPT2.setAntiAlias(true);
		OMC.FGPT2.setTypeface(widTypeface);

    	String sTime="";
    	String sByLineText = "";
    
    	// 12 or 24 hours? Leading 0 or no leading 0?
    	if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    		sTime = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
			sByLineText = ClockWidget.sCHINESETIME + "時";
    	} else {
    		sTime = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
    		sByLineText = OMC.TIME.format("%p");
    	}
    	
    	//Finally, theme-specific tweaks.
    	if (sTheme.equals("BokehBeauty")) {
    		OMC.FGPT1.setTextAlign(Paint.Align.CENTER);
    		OMC.FGPT2.setTextSize(14);
    		OMC.FGPT2.setTextAlign(Paint.Align.LEFT);
    		OMC.FGPT2.setTextSkewX((float)-0.25);

    		OMC.BGPT1.setColor(OMC.CACHEDATTRIBS.getColor(6, 0xffffffff));
    		OMC.BGPT1.setStyle(Style.FILL_AND_STROKE);
    		OMC.CANVAS.drawCircle((float)50, (float)50, 20, OMC.BGPT2);
    		OMC.CANVAS.drawCircle((float)60, (float)70, 10, OMC.BGPT2);
    		
    	} else if (sTheme.equals("CultureClash")) {
    		OMC.FGPT1.setTextAlign(Paint.Align.CENTER);
    		OMC.FGPT2.setTextSize(14);
    		OMC.FGPT2.setTextAlign(Paint.Align.LEFT);
    		OMC.FGPT2.setTextSkewX((float)-0.25);
    	} else if (sTheme.equals("DigitalDigits")) {
    		OMC.FGPT1.setTextAlign(Paint.Align.CENTER);
    		OMC.FGPT1.setTextSkewX((float)-0.25);
    		OMC.FGPT1.setTextScaleX((float)0.70);
    		OMC.FGPT2.setTextSize(14);
    		OMC.FGPT2.setTextAlign(Paint.Align.LEFT);
    		sByLineText = OMC.TIME.format("%A, %B %e");
    	} else if (sTheme.equals("LockscreenLook")) {

    		sByLineText = OMC.TIME.format("%A, %B %e");
        	
    		OMC.FGPT1.setTextAlign(Paint.Align.CENTER);
    		OMC.FGPT2.setTextSize(16);
    		OMC.FGPT2.setTextAlign(Paint.Align.LEFT);
    		OMC.FGPT2.setTextSkewX((float)0.);
    		
    	} else if (sTheme.equals("WhamBamWidget")) {

    		sTime = sTime + "!";
    		switch (OMC.RND.nextInt(9)) {
    		case 0:
        		sByLineText = "what will this Android do?";
    			break;
    		case 1:
        		sByLineText = "my middle name is awesome.";
    			break;
    		case 2:
        		sByLineText = "never a dull moment with me.";
    			break;
    		case 3:
        		sByLineText = "but it's five o'clock for me.";
    			break;
    		case 4:
        		sByLineText = "i will serve you to the death!";
    			break;
    		case 5:
        		sByLineText = "don't trade me in yet!";
    			break;
    		case 6:
        		sByLineText = "you look fabulous today!";
    			break;
    		case 7:
        		sByLineText = "no, i swear i didn't miss that call.";
    			break;
    		case 8:
        		sByLineText = "stop staring at me.";
    			break;
    		default:
    		}
    		
    		OMC.FGPT1.setTextAlign(Paint.Align.CENTER);
    		OMC.FGPT1.setTextSkewX((float)-0.25);
        	OMC.FGPT2.setTextSize(12);
        	OMC.FGPT2.setTextAlign(Paint.Align.LEFT);
        	OMC.FGPT2.setTextSkewX((float)-0.25);
    	}

    	
    	// Draw the Chinese bkgd.
    	OMC.BGPT1.setColor(OMC.CACHEDATTRIBS.getColor(4, 0xffffffff));
    	OMC.CANVAS.drawText(ClockWidget.sCHINESETIME, 225, 145, OMC.BGPT1);

		// Draw the background panel.
		OMC.BGPT1.setColor(OMC.CACHEDATTRIBS.getColor(6, 0xffffffff));
		OMC.CANVAS.drawRoundRect(OMC.BGRECT, (float)5, (float)5, OMC.BGPT1);
		OMC.BGPT1.setColor(OMC.CACHEDATTRIBS.getColor(5, 0xffffffff));
		OMC.CANVAS.drawRoundRect(OMC.FGRECT, (float)5, (float)5, OMC.BGPT1);
		
		// Draw the Clock digits.
		ClockWidget.fancyDrawText(OMC.CACHEDATTRIBS.getString(9), OMC.CANVAS, sTime, OMC.WIDGETWIDTH/2, OMC.WIDGETHEIGHT*4/5-20, OMC.FGPT1, OMC.CACHEDATTRIBS.getColor(7, 0x00000000),OMC.CACHEDATTRIBS.getColor(8, 0xffffffff));
    	
		// Draw the byline.
		ClockWidget.fancyDrawText(OMC.CACHEDATTRIBS.getString(9), OMC.CANVAS, sByLineText, 45, 145-20, OMC.FGPT2, OMC.CACHEDATTRIBS.getColor(7, 0x00000000),OMC.CACHEDATTRIBS.getColor(8, 0xffffffff));

//        pt.setARGB(64,0,0,0);
//pt.setColor(Color.DKGRAY);
//        pt.setARGB(164,255,255,255);
//		bufferCanvas.drawText(t.format("%R"), ClockWidget.WIDTH/2+1, ClockWidget.HEIGHT*4/5+1-20, pt);  	// Shadow in 24-hr HH:MM format CLOCKOPIA
//		bufferCanvas.drawText(t.format("%R"), ClockWidget.WIDTH/2-1, ClockWidget.HEIGHT*4/5-1-20, pt);  	// Shadow in 24-hr HH:MM format CLOCKOPIA
//        pt.setARGB(164,0,0,0);
//		bufferCanvas.drawText(t.format("%R"), ClockWidget.WIDTH/2, ClockWidget.HEIGHT*4/5-20, pt);  	// Shadow in 24-hr HH:MM format CLOCKOPIA
//		bufferCanvas.drawText(t.format("%R"), 05, 120, pt);  	// Shadow in 24-hr HH:MM format CLOCKOPIA
//		bufferCanvas.drawText(t.format("%R"), 40, 130, pt);  	// Shadow in 24-hr HH:MM format GOLONG
//		pt.setARGB(192,0,0,0);
//		pt.setARGB(216,255,255,255);
//		bufferCanvas.drawText(t.format("%R"), ClockWidget.WIDTH/2, ClockWidget.HEIGHT*4/5-20, pt);  	// Text in 24-hr HH:MM format CLOCKOPIA
//		bufferCanvas.drawText(t.format("%R"), 00, 115, pt);  	// Text in 24-hr HH:MM format CLOCKOPIA
//		bufferCanvas.drawText(t.format("%R"), 35, 125, pt);  	// Text in 24-hr format GOLONG

		//WIFI Piece

//        if (ClockWidget.WFM==null) ClockWidget.WFM = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//        switch (ClockWidget.WFM.getWifiState()){
//        case WifiManager.WIFI_STATE_ENABLED: case WifiManager.WIFI_STATE_ENABLING: case WifiManager.WIFI_STATE_DISABLING:
//        	tmpStr = "WiFi ON";
//        	break;
//        case WifiManager.WIFI_STATE_DISABLED:
//        	tmpStr = "WiFi OFF";
//        	break;
//	    default:
//	    	tmpStr = "WiFi UNK";
//        }
        //tmpStr = String.valueOf(wfm.getWifiState());

//        Get Map of prefs
//        java.util.Map<String,?> prefs = ClockModel.getPrefMap(context, aWI);
        
        widTypeface = null;
	}
	

	static void fancyDrawText(final String style, final Canvas cvas, final String text, final int x, final int y, final Paint pt, int color1, int color2)  {
		//Draw the SFX
		if (style.equals("emboss")) {
			pt.setColor(color2);
			cvas.drawText(text, x-1, y-1, pt);
			cvas.drawText(text, x+1, y+1, pt);
			pt.setColor(color1);
			cvas.drawText(text, x, y, pt);
		} else if (style.equals("shadow")) {
			pt.setColor(color2);
			cvas.drawText(text, x+3, y+3, pt);
		}
		//Either way, draw the proper text
		pt.setColor(color1);
		cvas.drawText(text, x, y, pt);
	}

	
	//	When one or more widgets are removed...
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
			//Remove Prefs
			if (OMC.DEBUG)Log.i("OMCWidget","Removed Widget #" + appWidgetIds[i]);
			OMC.removePrefs(appWidgetIds[i]);
		}
	}

//	When the very last widget is removed.
	public void onDisabled(Context context) {

		//Flag OMCService to stop.
		OMCService.STOPNOW=true;
	}
		
//	This gets called when the very first widget is instantiated.
	public void onEnabled(Context context) {

		//Unflag the STOP FLAG for OMCService.
		OMCService.STOPNOW=false;
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
	        ClockWidget.sCHINESETIME = String.valueOf(OMC.CHINESETIME.charAt((OMC.TIME.hour + 1)/2));

	        ClockWidget.updateAppWidget(context);

			super.onReceive(context, intent);
		}
	}

	//	This should never fire since I implemented onReceive.
	@Override
	public void onUpdate(Context context, AppWidgetManager aWM, int[] appWidgetIds) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 10000);
		}
		final int N = appWidgetIds.length;
		for (int i=0; i<N; i++) {
		  	OMC.initPrefs(appWidgetIds[i]);
			updateAppWidget(context, aWM, appWidgetIds[i]);
		}

	}
	
	static void updateAppWidget(Context context) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 10000);
		}
		AppWidgetManager aWM = AppWidgetManager.getInstance(context);

		final int N = aWM.getAppWidgetIds(OMC.WIDGETCNAME).length;

		for (int i=0; i<N; i++) {
			ClockWidget.updateAppWidget(context, aWM, aWM.getAppWidgetIds(OMC.WIDGETCNAME)[i]);
		}
		System.gc();
	}
	
	static void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId) {
		if (OMC.DEBUG)Log.i("OMCWidget", "Redrawing widget" + appWidgetId + " @ " + OMC.TIME.format("%T"));

		drawBitmapForWidget(context,appWidgetId);

		// Blit the buffer over
		final RemoteViews rv = new RemoteViews(context.getPackageName(),R.layout.omcwidget);
        rv.setImageViewBitmap(R.id.omcIV, OMC.BUFFER);

        // Set oTime
        OMC.OTIME.set(OMC.TIME);
        Intent intent = new Intent(Intent.ACTION_EDIT,Uri.parse("timer:"+appWidgetId),context,OMCPrefActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
		rv.setOnClickPendingIntent(R.id.omcIV, pi);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
	}
}
