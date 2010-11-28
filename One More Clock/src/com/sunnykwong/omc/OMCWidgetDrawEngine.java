package com.sunnykwong.omc;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.text.SpannedString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class OMCWidgetDrawEngine {
	// This is where the theme-specific tweaks (regardless of layer) are processed.
	// Tweaks = hacks, but at least all the hacks are in one block of code.
	static void layerThemeTweaks(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		switch (iLayerID) {
		default:
			// do nothing
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

		//Depending on time of day, endpoints.
		final float ratio = (((OMC.TIME.hour+6)*60 + OMC.TIME.minute) % (12*60)) / (12f*60);
		final float x1 = OMC.WIDGETWIDTH * ratio;  
		final float y1 = OMC.WIDGETHEIGHT;
		final float x2 = OMC.WIDGETWIDTH - x1;
		final float y2 = 0;
		
		float dist = -0.45f;
		for (int i = 0; i < OMC.LAYERATTRIBS.getInt(1, 5); i++) {
			OMC.PT1.setColor(OMC.FLARECOLORS[i]);
			dist += (float)(1.f/OMC.LAYERATTRIBS.getInt(1, 5)); 
			float x = (x2-x1) * dist + OMC.WIDGETWIDTH/2f;
			float y = (y2-y1) * dist + OMC.WIDGETHEIGHT/2f;
			OMC.CANVAS.drawCircle(x, y, OMC.FLARERADII[i], OMC.PT1);
			OMC.CANVAS.drawCircle(x, y, OMC.FLARERADII[i]+1, OMC.PT2);
		}
		
    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
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
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
    	
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

	//Bitmap layer.  This is really only for skinning.
	static void drawBitmapLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		OMC.PT1.reset();
		OMC.PT1.setAntiAlias(true);
		OMC.PT1.setColor(Color.BLACK);

    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
		
		// Blit the buffer over
		Bitmap tempBitmap = OMC.getBitmap(
				OMC.LAYERATTRIBS.getString(1),
				OMC.LAYERATTRIBS.getString(2));
		if (tempBitmap==null) tempBitmap = OMC.getBitmap(
				OMC.LAYERATTRIBS.getString(1),
				OMC.CACHEPATH + sTheme + OMC.LAYERATTRIBS.getString(2));

		// Prepare the transformation matrix.
		
		OMC.TEMPMATRIX.reset();
		OMC.TEMPMATRIX.postTranslate(-tempBitmap.getWidth()/2f, -tempBitmap.getHeight()/2f);
		if (OMC.LAYERATTRIBS.mImportedArray.length > 8) {
			if (OMC.LAYERATTRIBS.getBoolean(8, false)) {
				OMC.TEMPMATRIX.postRotate(OMC.LAYERATTRIBS.getFloat(9, 0f));
			}
		}
		if (OMC.LAYERATTRIBS.getBoolean(3, true)) {
			OMC.TEMPMATRIX.postScale(OMC.LAYERATTRIBS.getFloat(4, 1f), OMC.LAYERATTRIBS.getFloat(5, 1f));
			OMC.TEMPMATRIX.postTranslate((tempBitmap.getWidth()*OMC.LAYERATTRIBS.getFloat(4, 1f))/2f
				+ OMC.LAYERATTRIBS.getFloat(6, 0f), 
				(tempBitmap.getHeight()*OMC.LAYERATTRIBS.getFloat(5, 1f))/2f
				+ OMC.LAYERATTRIBS.getFloat(7, 0f));
		} else {
			OMC.TEMPMATRIX.postTranslate((tempBitmap.getWidth())/2f
					+ OMC.LAYERATTRIBS.getFloat(6, 0f), 
					(tempBitmap.getHeight())/2f
					+ OMC.LAYERATTRIBS.getFloat(7, 0f));
		}
		if (tempBitmap==null) {
			Toast.makeText(context, "Error loading theme bitmap " + OMC.LAYERATTRIBS.getString(2) + ".\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.putBoolean("external"+aWI,false)
					.commit();
			return;
		}
		tempBitmap.setDensity(DisplayMetrics.DENSITY_HIGH);
		OMC.CANVAS.drawBitmap(tempBitmap,OMC.TEMPMATRIX,OMC.PT1);
	}

	// Quote layer.  Set the Text to be shown before passing to drawTextLayer.
	static void drawQuoteLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		OMC.TALKBACKS = OMC.loadStringArray(sTheme, aWI, OMC.LAYERATTRIBS.getString(13));
//		System.out.println("Talkbacks found:");
//		for (String sTemp: OMC.TALKBACKS) {
//			System.out.println(sTemp);
//		}
		if (OMC.TALKBACKS == null) OMC.TXTBUF="ERROR";
		else OMC.TXTBUF = OMC.TALKBACKS[OMC.RND.nextInt(OMC.TALKBACKS.length)];
		OMCWidgetDrawEngine.drawTextLayer(context, iLayerID, sTheme, aWI);
		OMC.TALKBACKS=null;
	}

	// Text layer.  Written this way so we can have as many as we want with minimal effort.
	static void drawTextLayer(final Context context, final int iLayerID, final String sTheme, final int aWI) {

		OMC.PT1.reset();
		OMC.PT1.setAntiAlias(true);
		Typeface tempTypeface = OMC.getTypeface(
				OMC.LAYERATTRIBS.getString(1),
				OMC.LAYERATTRIBS.getString(2));
		if (tempTypeface==null) tempTypeface = OMC.getTypeface(
				OMC.LAYERATTRIBS.getString(1),
				OMC.CACHEPATH + sTheme + OMC.LAYERATTRIBS.getString(2));
		if (tempTypeface==null) {
			Toast.makeText(context, "Error loading theme typeface " + OMC.LAYERATTRIBS.getString(2) + ".\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.putBoolean("external"+aWI,false)
					.commit();
			return;
		}
		OMC.PT1.setTypeface(tempTypeface);
		OMC.PT1.setTextSize(OMC.LAYERATTRIBS.getInt(3, 100));
		OMC.PT1.setTextSkewX(OMC.LAYERATTRIBS.getFloat(4, 0.f));
		int iTemp;
		if (OMC.LAYERATTRIBS.getString(5).startsWith("f")) {
			OMC.PT1.setTextScaleX(1f);
			float fFactor = Float.parseFloat(OMC.LAYERATTRIBS.getString(5).substring(1))/OMCWidgetDrawEngine.getSpannedStringWidth(new SpannedString(Html.fromHtml(OMC.TXTBUF)),OMC.PT1);
			OMC.PT1.setTextScaleX(fFactor);
		} else if ((iTemp = OMC.LAYERATTRIBS.getString(5).indexOf("m"))!= -1) {
			OMC.PT1.setTextScaleX(Float.parseFloat(OMC.LAYERATTRIBS.getString(5).substring(0,iTemp)));
			int iMax = Integer.parseInt(OMC.LAYERATTRIBS.getString(5).substring(iTemp+1));
			int iLength = OMCWidgetDrawEngine.getSpannedStringWidth(new SpannedString(Html.fromHtml(OMC.TXTBUF)),OMC.PT1); 
			if (iLength <= iMax){
				//do nothing, PT1 properly set
			} else {
				OMC.PT1.setTextScaleX(((float)iMax)/iLength);
			}
		} else {
			OMC.PT1.setTextScaleX(OMC.LAYERATTRIBS.getFloat(5, 1.f));
    	}
		
		OMC.PT1.setFakeBoldText(OMC.LAYERATTRIBS.getBoolean(6, false));
		OMC.PT1.setColor(OMC.LAYERATTRIBS.getColor(8, 0));
		
		float fRot = 0f;
		if (OMC.LAYERATTRIBS.mImportedArray.length > 14) {
			if (OMC.LAYERATTRIBS.getBoolean(14, false)) {
				fRot = OMC.LAYERATTRIBS.getFloat(15, 0f);
			}
		}
		

		if (OMC.LAYERATTRIBS.getString(12).equals("center")) {
			OMC.PT1.setTextAlign(Paint.Align.CENTER);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("left")) {
			OMC.PT1.setTextAlign(Paint.Align.LEFT);
		} else if (OMC.LAYERATTRIBS.getString(12).equals("right")) {
			OMC.PT1.setTextAlign(Paint.Align.RIGHT);
		};

		OMC.PT2.reset();
		OMC.PT2.setAntiAlias(true);
		OMC.PT2.setTypeface(tempTypeface);
		OMC.PT2.setTextSize(OMC.PT1.getTextSize());
		OMC.PT2.setTextSkewX(OMC.PT1.getTextSkewX());
		OMC.PT2.setTextScaleX(OMC.PT1.getTextScaleX());
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
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
    	
    	// Draw the layer.
		OMCWidgetDrawEngine.fancyDrawText(
    			OMC.LAYERATTRIBS.getString(7),
    			OMC.CANVAS,
    			OMC.TXTBUF,
    			OMC.LAYERATTRIBS.getInt(10, 0),
    			OMC.LAYERATTRIBS.getInt(11, 0),
    			OMC.PT1,
    			OMC.PT2,
    			fRot);

	}
	
	static synchronized Bitmap drawBitmapForWidget(final Context context, final int aWI) {
		OMC.BUFFER.eraseColor(Color.TRANSPARENT);

		final String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME);
		boolean bExternal = OMC.PREFS.getBoolean("external"+aWI,false);

		if (bExternal) {
			OMCImportedTheme oTheme = OMC.getImportedTheme(context, sTheme);
			if (oTheme==null) {
				Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
				OMC.PREFS.edit()
						.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
						.putBoolean("external"+aWI,false)
						.commit();
				return null;
			}
  			ArrayList<String> tempAL = oTheme.arrays.get(oTheme.name);
  			OMC.LAYERLIST = tempAL.toArray(new String[tempAL.size()]);
		} else {
			//		Set LAF based on prefs
			OMC.LAYERLIST = context.getResources().getStringArray(context.getResources().getIdentifier(sTheme, "array", "com.sunnykwong.omc"));
		}
		for (String layer:OMC.LAYERLIST) {
			// Clear the text buffer first.
			OMC.TXTBUF="";
			
			String sType = layer.substring(0,5);

			int iLayerID=0;
			
			if (bExternal) {
				OMC.LAYERATTRIBS = new OMCTypedArray(OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(layer.substring(6)),aWI);
			} else {
				//		Set LAF based on prefs
				iLayerID = context.getResources().getIdentifier(layer.substring(6), "array", "com.sunnykwong.omc");
				OMC.LAYERATTRIBS = new OMCTypedArray(context.getResources().getStringArray(iLayerID), aWI);
			}

			if (sType.equals("text ")){
				OMC.TXTBUF = OMC.LAYERATTRIBS.getString(13);
			}

			if (OMC.LAYERATTRIBS.getBoolean(0, true)){
				if (sType.equals("text ")) OMCWidgetDrawEngine.drawTextLayer(context, iLayerID, sTheme, aWI);
				else if (sType.equals("panel"))OMCWidgetDrawEngine.drawPanelLayer(context, iLayerID, sTheme, aWI);
				else if (sType.equals("flare"))OMCWidgetDrawEngine.drawFlareLayer(context, iLayerID, sTheme, aWI);
				else if (sType.equals("quote"))OMCWidgetDrawEngine.drawQuoteLayer(context, iLayerID, sTheme, aWI);
				else if (sType.equals("image"))OMCWidgetDrawEngine.drawBitmapLayer(context, iLayerID, sTheme, aWI);
			}
			OMC.LAYERATTRIBS.recycle();
		}
		
		return Bitmap.createBitmap(OMC.BUFFER);
		
	}

	static int getSpannedStringWidth(SpannedString ss, final Paint pt) {
		int result = 0;
		int iStart=0;
		float fDefaultskew;
		Paint ptTemp = new Paint(pt);
		ptTemp.setTextAlign(Paint.Align.LEFT);
		fDefaultskew = pt.getTextSkewX();
		while (iStart < ss.length()){
			int iEnd = ss.nextSpanTransition(iStart, ss.length(), StyleSpan.class);
			if (iEnd == -1) return 0;
			if (!pt.isFakeBoldText()) ptTemp.setFakeBoldText(false);
			ptTemp.setTextSkewX(fDefaultskew);
			for (Object o:ss.getSpans(iStart, iEnd, StyleSpan.class)) {
				StyleSpan style = (StyleSpan)o;
				switch (style.getStyle()) {
					case Typeface.BOLD:
						ptTemp.setFakeBoldText(true);
						break;
					case Typeface.BOLD_ITALIC:
						ptTemp.setFakeBoldText(true);
						ptTemp.setTextSkewX(fDefaultskew-0.25f);
						break;
					case Typeface.ITALIC:
						ptTemp.setTextSkewX(fDefaultskew-0.25f);
						break;
					default:
						ptTemp.setFakeBoldText(false);
						ptTemp.setTextSkewX(fDefaultskew);
				}
			}
			result+=ptTemp.measureText(ss.toString().substring(iStart, iEnd));
			iStart = iEnd;
		}
		return result;
	}
	
	static void fancyDrawSpanned(final Canvas cvas, final String str, final int x, final int y, final Paint pt, final float fRot) {
		final SpannedString ss = new SpannedString(Html.fromHtml(str));
		Paint ptTemp = new Paint(pt);
		ptTemp.setTextAlign(Paint.Align.LEFT);
		int bufferWidth = Math.max(OMCWidgetDrawEngine.getSpannedStringWidth(ss, pt),1);
		int bufferHeight = Math.max(pt.getFontMetricsInt().bottom - pt.getFontMetricsInt().top,1);
		int iCursor = 0;
		OMC.rotBUFFER = Bitmap.createBitmap((int)(bufferWidth*1.2f), bufferHeight, Bitmap.Config.ARGB_4444);
		OMC.rotBUFFER.setDensity(DisplayMetrics.DENSITY_HIGH);
		OMC.rotBUFFER.eraseColor(Color.TRANSPARENT);
		OMC.rotCANVAS = new Canvas(OMC.rotBUFFER);

		int iStart=0;
		while (iStart < ss.length()){
			int iEnd = ss.nextSpanTransition(iStart, ss.length(), StyleSpan.class);
			if (iEnd == -1) return;
			ptTemp.set(pt);
			ptTemp.setTextAlign(Paint.Align.LEFT);
			for (Object o:ss.getSpans(iStart, iEnd, StyleSpan.class)) {
				StyleSpan style = (StyleSpan)o;
				switch (style.getStyle()) {
					case Typeface.BOLD:
						ptTemp.setFakeBoldText(true);
						break;
					case Typeface.BOLD_ITALIC:
						ptTemp.setFakeBoldText(true);
						ptTemp.setTextSkewX(-0.25f);
						break;
					case Typeface.ITALIC:
						ptTemp.setTextSkewX(-0.25f);
						break;
					default:
				}

			}
			OMC.rotCANVAS.drawText(ss.subSequence(iStart, iEnd).toString(), iCursor, 0-pt.getFontMetricsInt().top, ptTemp);
			iCursor+=ptTemp.measureText(ss.toString().substring(iStart, iEnd));
			iStart = iEnd;
		}
		OMC.TEMPMATRIX.reset();
		OMC.TEMPMATRIX.postTranslate(-bufferWidth/2f, pt.getFontMetricsInt().top);
		OMC.TEMPMATRIX.postRotate(fRot);
		if (pt.getTextAlign() == Paint.Align.LEFT) {
			// Do nothing
			OMC.TEMPMATRIX.postTranslate(bufferWidth/2f+x, y);
		} else if (pt.getTextAlign() == Paint.Align.CENTER) {
			OMC.TEMPMATRIX.postTranslate(x, y);
		} else if (pt.getTextAlign() == Paint.Align.RIGHT) {
			OMC.TEMPMATRIX.postTranslate(-bufferWidth/2f+x, y);
		} else {
			// Huh? do nothing
			OMC.TEMPMATRIX.postTranslate(bufferWidth/2f+x, y);
		}
		cvas.drawBitmap(OMC.rotBUFFER, OMC.TEMPMATRIX, pt);
		OMC.rotCANVAS = null;
		OMC.rotBUFFER.recycle();
		

		
	}
	
	static void fancyDrawText(final String style, final Canvas cvas, final String text, final int x, final int y, final Paint pt1, final Paint pt2, final float fRot)  {
		//Draw the SFX
		if (style.equals("emboss")) {
			
			//SpannableStringBuilder ssb = new SpannableStringBuilder(Html.fromHtml(text));
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x-1, y-1, pt2, fRot);
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+1, y+1, pt2, fRot);
		} else if (style.equals("shadow")) {
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+3, y+3, pt2, fRot);
		}
		//Either way, draw the proper text
		OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
	}

	// Needs to be synchronized now that we have four different widget types 
	// calling this same method creating a potential race condition
	static synchronized void updateAppWidget(Context context, float fScaleX, float fScaleY, ComponentName cName, int iCutTop, int iCutBottom) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 10000);
		}
		AppWidgetManager aWM = AppWidgetManager.getInstance(context);

		final int N = aWM.getAppWidgetIds(cName).length;

		for (int i=0; i<N; i++) {
			OMCWidgetDrawEngine.updateAppWidget(context, aWM, aWM.getAppWidgetIds(cName)[i],fScaleX, fScaleY, cName, iCutTop, iCutBottom);
		}
		System.gc();
	}
	
	static synchronized void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId, float fScaleX, float fScaleY, ComponentName cName, int iCutTop, int iCutBottom) {

		if (OMC.DEBUG)Log.i("OMCWidget", "Redrawing widget" + appWidgetId + " @ " + OMC.TIME.format("%T"));

		String sTheme = OMC.PREFS.getString("widgetTheme"+appWidgetId,OMC.DEFAULTTHEME);
		boolean bExternal = OMC.PREFS.getBoolean("external"+appWidgetId,false);

		OMC.STRETCHINFO = null;

		if (cName.equals(OMC.WIDGET4x2CNAME) || cName.equals(OMC.WIDGET2x1CNAME)) {
			//Correct aspect ratio already; do nothing
		} else {
			String sStretch="";
			if (cName.equals(OMC.WIDGET4x1CNAME)) {
				sStretch = sTheme + "_4x1SqueezeInfo";
			} else if (cName.equals(OMC.WIDGET3x1CNAME)) {
				sStretch = sTheme + "_3x1SqueezeInfo";
			}

			try {
				if (bExternal) {
					OMC.STRETCHINFO = new String[4]; 
					OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sStretch).toArray(OMC.STRETCHINFO);
				} else {
					int iLayerID = context.getResources().getIdentifier(sStretch, "array", "com.sunnykwong.omc");
					OMC.STRETCHINFO = context.getResources().getStringArray(iLayerID);
				}
			} catch (android.content.res.Resources.NotFoundException e) {
				// OMC.STRETCHINFO stays null; do nothing
				if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for seeded clock. Using default.");
			} catch (java.lang.NullPointerException e) {
				// OMC.STRETCHINFO stays null; do nothing
				if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for imported clock. Using default.");
				OMC.STRETCHINFO = null;
			}
		}
		OMC.OVERLAYURL = null;
		try {
			if (bExternal) {
				ArrayList<String> alTemp = OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme+"_Links");
				OMC.OVERLAYURL = (alTemp.toArray(new String[alTemp.size()]));
			} else {
				int iLayerID = context.getResources().getIdentifier(sTheme+"_Links", "array", "com.sunnykwong.omc");
				OMC.OVERLAYURL = context.getResources().getStringArray(iLayerID);
			}
		} catch (android.content.res.Resources.NotFoundException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No link URL info found for seeded clock.");
			OMC.OVERLAYURL = new String[] {"default","default","default","default","default","default","default","default","default"};
		} catch (java.lang.NullPointerException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No link URL info found for imported clock.");
			OMC.OVERLAYURL = new String[] {"default","default","default","default","default","default","default","default","default"};
		}

		OMCWidgetDrawEngine.drawBitmapForWidget(context,appWidgetId);

		// Blit the buffer over
		final RemoteViews rv = new RemoteViews(context.getPackageName(),R.layout.omcwidget);

		if (fScaleX==1f && fScaleY==1f) rv.setImageViewBitmap(R.id.omcIV, OMC.BUFFER);
		else if (OMC.STRETCHINFO != null){
			//Custom scaling
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preScale(Float.parseFloat(OMC.STRETCHINFO[0]), 
					Float.parseFloat(OMC.STRETCHINFO[1]));
			rv.setImageViewBitmap(R.id.omcIV,Bitmap.createBitmap(OMC.BUFFER, 0, Integer.parseInt(OMC.STRETCHINFO[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(OMC.STRETCHINFO[2]) - Integer.parseInt(OMC.STRETCHINFO[3]), OMC.TEMPMATRIX, true));
		} else {
			//Default scaling
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preTranslate(0, 0-iCutTop);
			OMC.TEMPMATRIX.preScale(fScaleX, fScaleY);
			rv.setImageViewBitmap(R.id.omcIV,Bitmap.createBitmap(OMC.BUFFER, 0, 0, OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight(), OMC.TEMPMATRIX, true));
		}
		
        if (OMC.PREFS.getString("URI"+appWidgetId, "").equals("")) { 
//	Using a broadcast is more flexible, but less crash-proof. So we're not using it for now.
//        	Intent intent = new Intent("com.sunnykwong.omc.WIDGET_CONFIG");
//        	intent.setData(Uri.parse("omc:"+appWidgetId));
//        	PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        	Intent intent = new Intent(context, OMCPrefActivity.class);
        	intent.setData(Uri.parse("omc:"+appWidgetId));
        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
        	
        	rv.setOnClickPendingIntent(R.id.omcIV, pi);
            for (int i = 0; i < 9; i++) {
            	if (OMC.OVERLAYURL[i].equals("default")) {
	            	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], pi);
            	} else {
    				Intent s = (new Intent(Intent.ACTION_VIEW,Uri.parse(OMC.OVERLAYURL[i])));
    				s.addCategory(Intent.CATEGORY_DEFAULT);
    				//System.out.println(s.get)
            		rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i],            	
            			PendingIntent.getActivity(context, 0, s,0));
            	}
            }
        } else {
        	if (OMC.DEBUG) Log.i("OMCWidget","INTENT " + OMC.PREFS.getString("URI"+appWidgetId, "")) ;
        	try {
	        	Intent intent = Intent.parseUri(OMC.PREFS.getString("URI"+appWidgetId, ""), 0);
	        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
	            rv.setOnClickPendingIntent(R.id.omcIV, pi);
	            for (int i = 0; i < 9; i++) {
	            	if (OMC.OVERLAYURL[i].equals("default")) {
		            	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], pi);
	            	} else rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], 
	            			PendingIntent.getBroadcast(context, 0, new Intent(
	            					Intent.ACTION_VIEW,Uri.parse(OMC.OVERLAYURL[i])), 0));
	            }
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
            // Kudos to Eric for solution to dummy out "unsetonlickpendingintent":
            // http://groups.google.com/group/android-developers/browse_thread/thread/f9e80e5ce55bb1e0/78153eb730326488
        	// I'm not using it right now, but it's a useful hint nonetheless. Thanks!
//        	rv.setOnClickPendingIntent(R.id.omcLink, PendingIntent.getBroadcast(context, 0, OMC.DUMMYINTENT,
//        		    PendingIntent.FLAG_UPDATE_CURRENT));
        
        appWidgetManager.updateAppWidget(appWidgetId, rv);
	}


}