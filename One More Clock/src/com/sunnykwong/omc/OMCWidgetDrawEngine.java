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
		case R.array.CCChinese:
			OMC.TXTBUF = String.valueOf(OMC.CHINESETIME.charAt((OMC.TIME.hour + 1)/2));

			break;
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

		// Prepare the transformation matrix.
		
		OMC.TEMPMATRIX.reset();
		if (OMC.LAYERATTRIBS.getBoolean(3, true)) OMC.TEMPMATRIX.postScale(OMC.LAYERATTRIBS.getFloat(4, 1f), OMC.LAYERATTRIBS.getFloat(5, 1f));
		OMC.TEMPMATRIX.postTranslate(OMC.LAYERATTRIBS.getFloat(6, 0f), OMC.LAYERATTRIBS.getFloat(7, 0f));

    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
		
		// Blit the buffer over
		Bitmap tempBitmap = OMC.getBitmap(
				OMC.LAYERATTRIBS.getString(1),
				OMC.LAYERATTRIBS.getString(2));
		if (tempBitmap==null) tempBitmap = OMC.getBitmap(
				OMC.LAYERATTRIBS.getString(1),
				OMC.CACHEPATH + sTheme + OMC.LAYERATTRIBS.getString(2));
		if (tempBitmap==null) {
			Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
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
		OMC.TXTBUF = OMC.TALKBACKS[OMC.RND.nextInt(OMC.TALKBACKS.length)];
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
			Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.putBoolean("external"+aWI,false)
					.commit();
			return;
		}
		OMC.PT1.setTypeface(tempTypeface);
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
		OMC.PT2.setTypeface(tempTypeface);
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
		OMCWidgetDrawEngine.layerThemeTweaks(context, iLayerID, sTheme, aWI);
    	
    	// Draw the layer.
		OMCWidgetDrawEngine.fancyDrawText(
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
		boolean bExternal = OMC.PREFS.getBoolean("external"+aWI,false);

		if (bExternal) {
			OMCImportedTheme oTheme = OMC.getImportedTheme(context, sTheme);
			if (oTheme==null) {
				Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
				OMC.PREFS.edit()
						.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
						.putBoolean("external"+aWI,false)
						.commit();
				return;
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
	}

	static int getSpannedStringWidth(SpannedString ss, final Paint pt) {
		int result = 0;
		int iStart=0;
		Paint ptTemp = new Paint(pt);
		ptTemp.setTextAlign(Paint.Align.LEFT);
		while (iStart < ss.length()){
			int iEnd = ss.nextSpanTransition(iStart, ss.length(), StyleSpan.class);
			if (iEnd == -1) return 0;
			ptTemp.setFakeBoldText(false);
			ptTemp.setTextSkewX(0f);
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
			result+=ptTemp.measureText(ss.toString().substring(iStart, iEnd));
			iStart = iEnd;
		}
		return result;
	}
	
	static void fancyDrawSpanned(final Canvas cvas, final String str, final int x, final int y, final Paint pt) {
		final SpannedString ss = new SpannedString(Html.fromHtml(str));
		Paint ptTemp = new Paint(pt);
		ptTemp.setTextAlign(Paint.Align.LEFT);
		int iCursor = 0;

		if (pt.getTextAlign() == Paint.Align.LEFT) {
			iCursor = x;
		} else if (pt.getTextAlign() == Paint.Align.CENTER) {
			iCursor = x - OMCWidgetDrawEngine.getSpannedStringWidth(ss, pt)/2;
		} else if (pt.getTextAlign() == Paint.Align.RIGHT) {
			iCursor = x - OMCWidgetDrawEngine.getSpannedStringWidth(ss, pt);
		} else {
			// Huh? do nothing
		}

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
			cvas.drawText(ss.subSequence(iStart, iEnd).toString(), iCursor, y, ptTemp);
			iCursor+=ptTemp.measureText(ss.toString().substring(iStart, iEnd));
			iStart = iEnd;
		}
		
	}
	
	static void fancyDrawText(final String style, final Canvas cvas, final String text, final int x, final int y, final Paint pt1, final Paint pt2)  {
		//Draw the SFX
		if (style.equals("emboss")) {
			
			//SpannableStringBuilder ssb = new SpannableStringBuilder(Html.fromHtml(text));
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x-1, y-1, pt2);
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+1, y+1, pt2);
//			cvas.drawText(text, x-1, y-1, pt2);
//			cvas.drawText(text, x+1, y+1, pt2);
		} else if (style.equals("shadow")) {
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+3, y+3, pt2);
//			cvas.drawText(text, x+3, y+3, pt2);
		}
		//Either way, draw the proper text
		OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1);
//		cvas.drawText(text, x, y, pt1);
	}

	// Needs to be synchronized now that we have three different widget types 
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
		if (cName.equals(OMC.WIDGET4x2CNAME) || cName.equals(OMC.WIDGET2x1CNAME)) {
			//Correct aspect ratio already; do nothing
		} else {
			String sStretch="";
			if (cName.equals(OMC.WIDGET4x1CNAME)) {
				sStretch = sTheme + "_4x1SqueezeInfo";
			} else if (cName.equals(OMC.WIDGET3x1CNAME)) {
				sStretch = sTheme + "_3x1SqueezeInfo";
			}
			OMC.STRETCHINFO = null;
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
				OMC.OVERLAYURL = (String)(OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme+"_Link").toArray()[0]);
			} else {
				int iLayerID = context.getResources().getIdentifier(sTheme+"_Link", "array", "com.sunnykwong.omc");
				OMC.OVERLAYURL = context.getResources().getStringArray(iLayerID)[0];
			}
		} catch (android.content.res.Resources.NotFoundException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No link URL info found for seeded clock.");
		} catch (java.lang.NullPointerException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No link URL info found for imported clock.");
			OMC.OVERLAYURL = null;
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
        	Intent intent = new Intent("com.sunnykwong.omc.WIDGET_CONFIG");
        	intent.setData(Uri.parse("omc:"+appWidgetId));
        	PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.omcIV, pi);
            rv.setOnClickPendingIntent(R.id.omcLink, pi);
        } else {
        	if (OMC.DEBUG) Log.i("OMCWidget","INTENT " + OMC.PREFS.getString("URI"+appWidgetId, "")) ;
        	try {
        	Intent intent = Intent.parseUri(OMC.PREFS.getString("URI"+appWidgetId, ""), 0);
        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
            rv.setOnClickPendingIntent(R.id.omcIV, pi);
            rv.setOnClickPendingIntent(R.id.omcLink, pi);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        
        // Set overlay URL if present, else set to dummy class.
        if (OMC.OVERLAYURL!=null) {
        	rv.setOnClickPendingIntent(R.id.omcLink, PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_DEFAULT,Uri.parse(OMC.OVERLAYURL)), 0));
        } else {
            // Kudos to Eric for solution to dummy out "unsetonlickpendingintent":
            // http://groups.google.com/group/android-developers/browse_thread/thread/f9e80e5ce55bb1e0/78153eb730326488
        	// I'm not using it right now, but it's a useful hint nonetheless. Thanks!
//        	rv.setOnClickPendingIntent(R.id.omcLink, PendingIntent.getBroadcast(context, 0, OMC.DUMMYINTENT,
//        		    PendingIntent.FLAG_UPDATE_CURRENT));
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, rv);
	}


}