package com.sunnykwong.omc;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public class ClockWidget extends AppWidgetProvider {
	
	static String sCHINESETIME;
	
	public ClockWidget() {
		super();
	}

	// This is where the theme-specific tweaks (regardless of layer) are processed.
	// Tweaks = hacks, but at least all the hacks are in one block of code.
	static void layerThemeTweaks(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		
		OMC.TXTBUF="";
		switch (iLayerID) {
		case R.array.BBClock:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
    		}
    		int iShade = 255 - Math.abs((OMC.TIME.hour*60 + OMC.TIME.minute) - (12*60))/720 * 255;
    		OMC.PT2.setARGB(255, iShade, iShade, iShade);
    		break;
		case R.array.BaBClock:
		case R.array.CCClock:
		case R.array.DDClock:
		case R.array.LLClock:
		case R.array.MMClock:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
    		}
    		break;
		case R.array.BBDate:
			OMC.TXTBUF = OMC.TIME.format("%a %e %b");
    		iShade = 255 - Math.abs((OMC.TIME.hour*60 + OMC.TIME.minute) - (12*60))/720 * 255;
    		OMC.PT2.setARGB(255, iShade, iShade, iShade);
    		break;
		case R.array.CCDate:
		case R.array.DDDate:
		case R.array.LLDate:
			OMC.TXTBUF = OMC.TIME.format("%A, %B %e");
    		break;
		case R.array.BBFlare:
    		break;
		case R.array.CCChinese:
			OMC.TXTBUF = ClockWidget.sCHINESETIME;
			break;
		case R.array.CCMeridiem:
			// Nice easter egg for those who prefer am/pm
			if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
				OMC.TXTBUF = "";
			} else if (OMC.TIME.hour < 12) {
				OMC.TXTBUF = "ante  meridiem";
			} else {
				OMC.TXTBUF = "post  meridiem";
			}
			break;
		case R.array.DDAPM:
			OMC.TXTBUF = OMC.TIME.format("%p").substring(0, 1);
			break;
		case R.array.WWBubble:
			// The thought bubbles, shadowed
			OMC.CANVAS.drawCircle(15f+3, 140f+3, 10, OMC.PT2);
    		OMC.CANVAS.drawCircle(15f, 140f, 10, OMC.PT1);
    		OMC.CANVAS.drawCircle(24f+3, 125f+3, 15, OMC.PT2);
    		OMC.CANVAS.drawCircle(24f, 125f, 15, OMC.PT1);
    		break;
		case R.array.WWOMG:
			OMC.TXTBUF = "omg, it's";
			break;
		case R.array.WWClock:
	    	// 12 or 24 hours? Leading 0 or no leading 0?
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M!") : OMC.TIME.format("%k:%M!");
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M!") : OMC.TIME.format("%l:%M!");
    		}
			break;
		case R.array.WWTalkback:
			// The talkbacks are in the arrays.xml file, feel free to add/change
			OMC.TXTBUF = OMC.TALKBACKS[OMC.RND.nextInt(OMC.TALKBACKS.length)];
			break;
		case R.array.MMSplatter:
			OMC.TXTBUF = "abi";
			break;
		case R.array.MMDate:
			OMC.TXTBUF = OMC.TIME.format("%A.");
			break;
		case R.array.BaBSun:
			OMC.TXTBUF = OMC.TIME.hour >= 6 || OMC.TIME.hour < 18 ? "M":"E";
    		iShade = (int)(70 + ((OMC.TIME.hour*60 + OMC.TIME.minute) % (12*60))/(12*60f) * 250);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
			break;
		case R.array.BaBCloud1:
			OMC.TXTBUF = "m";
    		iShade = (int)(70 + ((OMC.TIME.hour*60 + OMC.TIME.minute) % (3*60))/(3*60f) * 250);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
    		break;
		case R.array.BaBCloud2:
			OMC.TXTBUF = "m";
    		iShade = (int)(70 + ((OMC.TIME.hour*60 + OMC.TIME.minute) % (4*60))/(4*60f) * 250);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
			break;
		case R.array.BaBDuck:
			OMC.TXTBUF = OMC.TIME.format("U");
    		iShade = (int)(70 + 200 - ((OMC.TIME.hour*60 + OMC.TIME.minute) % (6*60))/(6*60f) * 200) ;
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
    		break;
		case R.array.BaBDucklings:
			OMC.TXTBUF = OMC.TIME.format("j");
    		iShade = (int)(45 + 230 - OMC.TIME.minute%60 / 60f * 230) ;
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
    		break;
		default:
			
		}
		
	}

	// This layer is pretty much dedicated to lens flare (bokeh beauty), 
	// but will need more tweaking for realism 
	static void drawFlareLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		OMC.PT1.reset();
		OMC.PT1.setAntiAlias(true);
		OMC.PT1.setStyle(Paint.Style.FILL_AND_STROKE);
		OMC.PT2.reset();
		OMC.PT2.setAntiAlias(true);
		OMC.PT2.setARGB(32, 255, 255, 255);
		OMC.PT2.setStyle(Paint.Style.STROKE);

		//Depending on time of day, determine angle.
		double angle = (OMC.TIME.hour*60 + OMC.TIME.minute) *Math.PI / (24.*60.);
		double y1 = OMC.WIDGETHEIGHT;
		double x1 = OMC.WIDGETWIDTH/2. - y1/2./Math.tan(angle);
		double y2 = 0.;
		double x2 = OMC.WIDGETWIDTH/2. + y1/2./Math.tan(angle);
		float dist = -0.45f;
		for (int i = 0; i < OMC.LAYERATTRIBS.getInt(1, 5); i++) {
			OMC.PT1.setColor(OMC.FLARECOLORS[i]);
			dist += (float)(1.f/OMC.LAYERATTRIBS.getInt(1, 5)); 
			double x = (x2-x1) * dist + OMC.WIDGETWIDTH/2.;
			double y = (y2-y1) * dist + OMC.WIDGETHEIGHT/2.;
			OMC.CANVAS.drawCircle((float)x, (float)y, OMC.FLARERADII[i], OMC.PT1);
			OMC.CANVAS.drawCircle((float)x, (float)y, OMC.FLARERADII[i]+1, OMC.PT2);
		}
		
    	// theme-specific tweaks.
    	ClockWidget.layerThemeTweaks(context, iLayerID, sTheme, aWI);
	}

	// Static rectangular panel.
	static void drawPanelLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		OMC.FGRECT.left = OMC.LAYERATTRIBS.getFloat(1, 0);
		OMC.FGRECT.top = OMC.LAYERATTRIBS.getFloat(2, 0);
		OMC.FGRECT.right = OMC.LAYERATTRIBS.getFloat(3, 0);
		OMC.FGRECT.bottom = OMC.LAYERATTRIBS.getFloat(4, 0);
		OMC.PT1.reset();
		OMC.PT1.setAntiAlias(true);
		OMC.PT1.setColor(OMC.LAYERATTRIBS.getColor(7, 0));
		OMC.PT2.reset();
		OMC.PT2.setAntiAlias(true);
		OMC.PT2.setColor(OMC.LAYERATTRIBS.getColor(8, 0));

    	// theme-specific tweaks.
    	ClockWidget.layerThemeTweaks(context, iLayerID, sTheme, aWI);
    	
		//Draw the SFX
		if (OMC.LAYERATTRIBS.getString(9).equals("emboss")) {
			OMC.BGRECT.left = OMC.FGRECT.left-1;
			OMC.BGRECT.top = OMC.FGRECT.top-1;
			OMC.BGRECT.right = OMC.FGRECT.right-1;
			OMC.BGRECT.bottom = OMC.FGRECT.bottom-1;
			OMC.CANVAS.drawRoundRect(OMC.BGRECT, OMC.LAYERATTRIBS.getFloat(5, 0), OMC.LAYERATTRIBS.getFloat(6, 0), OMC.PT2);
			OMC.BGRECT.left+=2;
			OMC.BGRECT.top+=2;
			OMC.BGRECT.right+=2;
			OMC.BGRECT.bottom+=2;
			OMC.CANVAS.drawRoundRect(OMC.BGRECT, OMC.LAYERATTRIBS.getFloat(5, 0), OMC.LAYERATTRIBS.getFloat(6, 0), OMC.PT2);
		} else if (OMC.LAYERATTRIBS.getString(9).equals("shadow")) {
			OMC.BGRECT.left = OMC.FGRECT.left+3;
			OMC.BGRECT.top = OMC.FGRECT.top+3;
			OMC.BGRECT.right = OMC.FGRECT.right+3;
			OMC.BGRECT.bottom = OMC.FGRECT.bottom+3;
			OMC.CANVAS.drawRoundRect(OMC.BGRECT, OMC.LAYERATTRIBS.getFloat(5, 0), OMC.LAYERATTRIBS.getFloat(6, 0), OMC.PT2);
		}
		//Either way, draw the proper panel
		OMC.CANVAS.drawRoundRect(OMC.FGRECT, OMC.LAYERATTRIBS.getFloat(5, 0), OMC.LAYERATTRIBS.getFloat(6, 0), OMC.PT1);

	}

	// Text layer.  Written this way so we can have as many as we want with minimal effort.
	static void drawTextLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {

		OMC.PT1.reset();
		OMC.PT1.setAntiAlias(true);
		OMC.PT1.setTypeface(OMC.getTypeface(
				OMC.LAYERATTRIBS.getString(1),
				OMC.LAYERATTRIBS.getString(2)));
		OMC.PT1.setTextSize(OMC.LAYERATTRIBS.getInt(3, 100));
		OMC.PT1.setTextSkewX(OMC.LAYERATTRIBS.getFloat(4, 0.f));
		OMC.PT1.setTextScaleX(OMC.LAYERATTRIBS.getFloat(5, 1.f));
		OMC.PT1.setFakeBoldText(OMC.LAYERATTRIBS.getBoolean(6, false));
		OMC.PT1.setColor(OMC.LAYERATTRIBS.getColor(8, 0));
		
		if (OMC.LAYERATTRIBS.getString(12).equals("center")) {
			OMC.PT1.setTextAlign(Paint.Align.CENTER);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("left")) {
			OMC.PT1.setTextAlign(Paint.Align.LEFT);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("right")) {
			OMC.PT1.setTextAlign(Paint.Align.RIGHT);
		};

		OMC.PT2.reset();
		OMC.PT2.setAntiAlias(true);
		OMC.PT2.setTypeface(OMC.getTypeface(
				OMC.LAYERATTRIBS.getString(1),
				OMC.LAYERATTRIBS.getString(2)));
		OMC.PT2.setTextSize(OMC.LAYERATTRIBS.getInt(3, 100));
		OMC.PT2.setTextSkewX(OMC.LAYERATTRIBS.getFloat(4, 0.f));
		OMC.PT2.setTextScaleX(OMC.LAYERATTRIBS.getFloat(5, 1.f));
		OMC.PT2.setFakeBoldText(OMC.LAYERATTRIBS.getBoolean(6, false));
		OMC.PT2.setColor(OMC.LAYERATTRIBS.getColor(9, 0));
		
		if (OMC.LAYERATTRIBS.getString(12).equals("center")) {
			OMC.PT2.setTextAlign(Paint.Align.CENTER);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("left")) {
			OMC.PT2.setTextAlign(Paint.Align.LEFT);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("right")) {
			OMC.PT2.setTextAlign(Paint.Align.RIGHT);
		};

    	// theme-specific tweaks.
    	ClockWidget.layerThemeTweaks(context, iLayerID, sTheme, aWI);
    	
    	// Draw the layer.
    	ClockWidget.fancyDrawText(
    			OMC.LAYERATTRIBS.getString(7),
    			OMC.CANVAS,
    			OMC.TXTBUF,
    			OMC.LAYERATTRIBS.getInt(10, 0),
    			OMC.LAYERATTRIBS.getInt(11, 0),
    			OMC.PT1,
    			OMC.PT2);

	}
	
	static void drawBitmapForWidget(final Context context, final int aWI) {
		OMC.BUFFER.eraseColor(Color.TRANSPARENT);

		String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME);

		//		Set LAF based on prefs
		OMC.LAYERLIST = context.getResources().getStringArray(context.getResources().getIdentifier(sTheme, "array", "com.sunnykwong.omc"));
		for (String layer:OMC.LAYERLIST) {
			String sType = layer.substring(0,5);
			int iLayerID = context.getResources().getIdentifier(layer.substring(6), "array", "com.sunnykwong.omc");

			OMC.LAYERATTRIBS = context.getResources().obtainTypedArray(iLayerID);

			if (sType.equals("text ")) ClockWidget.drawTextLayer(context, iLayerID, sTheme, aWI);
			else if (sType.equals("panel"))ClockWidget.drawPanelLayer(context, iLayerID, sTheme, aWI);
			else if (sType.equals("flare"))ClockWidget.drawFlareLayer(context, iLayerID, sTheme, aWI);
			else if (sType.equals("image"));
		}
	}
	

	static void fancyDrawText(final String style, final Canvas cvas, final String text, final int x, final int y, final Paint pt1, final Paint pt2)  {
		//Draw the SFX
		if (style.equals("emboss")) {
			cvas.drawText(text, x-1, y-1, pt2);
			cvas.drawText(text, x+1, y+1, pt2);
		} else if (style.equals("shadow")) {
			cvas.drawText(text, x+3, y+3, pt2);
		}
		//Either way, draw the proper text
		cvas.drawText(text, x, y, pt1);
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
        Intent intent = new Intent("com.sunnykwong.omc.WIDGET_CONFIG");
        intent.setData(Uri.parse("omc:"+appWidgetId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        rv.setOnClickPendingIntent(R.id.omcIV, pi);

        appWidgetManager.updateAppWidget(appWidgetId, rv);
	}
}