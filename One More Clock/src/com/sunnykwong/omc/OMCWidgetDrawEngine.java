package com.sunnykwong.omc;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Html;
import android.text.SpannedString;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.graphics.RectF;
import android.graphics.Rect;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import java.io.FileOutputStream;
import java.io.IOException;

public class OMCWidgetDrawEngine {
//	static final String TESTTHEME="IronIndicant";

	// Needs to be synchronized now that we have four different widget types 
	// calling this same method creating a potential race condition
	static synchronized void updateAppWidget(Context context, ComponentName cName) {
		// Set target time to be 2 seconds ahead to account for lag
		OMC.TIME.set(((System.currentTimeMillis()+2000l)*1000l)/1000l);

		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 10000);
		}
		AppWidgetManager aWM = AppWidgetManager.getInstance(context);

		final int N = aWM.getAppWidgetIds(cName)==null? 0: aWM.getAppWidgetIds(cName).length;

		for (int i=0; i<N; i++) {
             if (OMC.SCREENON && OMC.WIDGETBMPMAP.containsKey(aWM.getAppWidgetIds(cName)[i])) {
            	// Blit existing bitmap over first
            	RemoteViews rv = new RemoteViews(context.getPackageName(),context.getResources().getIdentifier("omcwidget", "layout", OMC.PKGNAME));
                rv.setImageViewBitmap(context.getResources().getIdentifier("omcIV", "id", OMC.PKGNAME),
                		OMC.WIDGETBMPMAP.get(aWM.getAppWidgetIds(cName)[i]));
                aWM.updateAppWidget(aWM.getAppWidgetIds(cName)[i], rv);                    
                rv = null;
              }

			OMCWidgetDrawEngine.updateAppWidget(context, aWM, aWM.getAppWidgetIds(cName)[i], cName);
		}
	}
	
	static synchronized void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId, ComponentName cName) { 
		long lStartTime = System.currentTimeMillis();

		if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "Engine", "Redrawing widget" + appWidgetId + " (" + OMC.PREFS.getString("widgetTheme"+appWidgetId, "")+ ") @ " + OMC.TIME.format("%T"));
		
		
		// Get theme.  (Nowadays, OMC.getTheme takes care of caching/importing.)

		String sTheme = OMC.PREFS.getString("widgetTheme"+appWidgetId,OMC.DEFAULTTHEME);
		//TODO
		// WHEN TESTING NEW THEME, UNCOMMENT THIS LINE 
		//String sTheme = TESTTHEME;

		
		JSONObject oTheme = OMC.getTheme(context, sTheme, OMC.THEMESFROMCACHE);

		// If we can't get theme (from memory, cache or SD), the theme is just gone.  
		// Revert to default to avoid a hard crash.

		if (oTheme==null) {
			Toast.makeText(context, "Cannot read " + sTheme + " theme.\n Reverting to stock...", Toast.LENGTH_LONG).show();
			OMC.PREFS.edit().putString("widgetTheme"+appWidgetId,OMC.DEFAULTTHEME).commit();
			return;
		}
		
		// OK, now actually render the widget on a bitmap.
		// Calling the actual drawing engine (below).
		// OMC.BUFFER (square, raw buffer) is updated and copy-returned.
		final Bitmap bitmap = OMCWidgetDrawEngine.drawBitmapForWidget(context,appWidgetId);

		//look for this size's custom scaling info
		OMC.STRETCHINFO = oTheme.optJSONObject("customscaling");
		String sWidgetSize = cName.toShortString().substring(cName.toShortString().length()-4,cName.toShortString().length()-1);
		int thisWidgetWidth = 480;
		int thisWidgetHeight = 480;
		if (sWidgetSize.equals("4x2")) {
			thisWidgetWidth = 480;
			thisWidgetHeight = 240;
		} else if (sWidgetSize.equals("4x1")) {
			thisWidgetWidth = 480;
			thisWidgetHeight = 120;
		} else if (sWidgetSize.equals("3x3")) {
			thisWidgetWidth = 360;
			thisWidgetHeight = 360;
		} else if (sWidgetSize.equals("3x1")) {
			thisWidgetWidth = 360;
			thisWidgetHeight = 120;
		} else if (sWidgetSize.equals("2x2")) {
			thisWidgetWidth = 240;
			thisWidgetHeight = 240;
		} else if (sWidgetSize.equals("2x1")) {
			thisWidgetWidth = 240;
			thisWidgetHeight = 120;
		} else if (sWidgetSize.equals("1x3")) {
			thisWidgetWidth = 120;
			thisWidgetHeight = 360;
		}
		
		//if no custom scaling info present, use default stretch info
		boolean bDefaultScaling=false;
		if (OMC.STRETCHINFO==null) {
			bDefaultScaling=true;
		} else {
			OMC.STRETCHINFO = OMC.STRETCHINFO.optJSONObject(sWidgetSize);
		}
		if (OMC.STRETCHINFO==null) {
			bDefaultScaling=true;
		}
		if (bDefaultScaling) {
			JSONObject oDefaultScaling = new JSONObject(); 
			try {
				if (sWidgetSize.equals("4x4")) {
					oDefaultScaling.put("horizontal_stretch", 1);
					oDefaultScaling.put("vertical_stretch", 1);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 0);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("4x2")) {
					oDefaultScaling.put("horizontal_stretch", 1);
					oDefaultScaling.put("vertical_stretch", 1);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 180);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("4x1")) {
					oDefaultScaling.put("horizontal_stretch", 0.8);
					oDefaultScaling.put("vertical_stretch", 0.7);
					oDefaultScaling.put("top_crop", 15);
					oDefaultScaling.put("bottom_crop", 195);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("3x3")) {
					oDefaultScaling.put("horizontal_stretch", 0.7);
					oDefaultScaling.put("vertical_stretch", 0.7);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 0);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("3x1")) {
					oDefaultScaling.put("horizontal_stretch", 0.8);
					oDefaultScaling.put("vertical_stretch", 0.7);
					oDefaultScaling.put("top_crop", 15);
					oDefaultScaling.put("bottom_crop", 195);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("2x2")) {
					oDefaultScaling.put("horizontal_stretch", 0.5);
					oDefaultScaling.put("vertical_stretch", 0.5);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 0);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("2x1")) {
					oDefaultScaling.put("horizontal_stretch", 0.5);
					oDefaultScaling.put("vertical_stretch", 0.5);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 180);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("1x3")) {
					oDefaultScaling.put("horizontal_stretch", 0.8);
					oDefaultScaling.put("vertical_stretch", 0.7);
					oDefaultScaling.put("top_crop", 15);
					oDefaultScaling.put("bottom_crop", 195);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", -90);
				} else {
					oDefaultScaling.put("horizontal_stretch", 1);
					oDefaultScaling.put("vertical_stretch", 1);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 0);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				}
				OMC.STRETCHINFO = oDefaultScaling;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		final Bitmap finalbitmap = OMC.WIDGETBMPMAP.containsKey(appWidgetId) ?
				OMC.WIDGETBMPMAP.get(appWidgetId) :
					Bitmap.createBitmap(thisWidgetWidth,thisWidgetHeight,Bitmap.Config.ARGB_4444);
		OMC.WIDGETBMPMAP.put(appWidgetId, finalbitmap);
		
		finalbitmap.setDensity(DisplayMetrics.DENSITY_HIGH);
		finalbitmap.eraseColor(Color.TRANSPARENT);

		Canvas finalcanvas = OMC.BMPTOCVAS.get(finalbitmap);
		if (finalcanvas==null) {
			finalcanvas = new Canvas(finalbitmap);
			OMC.BMPTOCVAS.put(finalbitmap, finalcanvas);
		}

		int width = bitmap.getWidth()- OMC.STRETCHINFO.optInt("left_crop") - OMC.STRETCHINFO.optInt("right_crop"); 
		int height = bitmap.getHeight() - OMC.STRETCHINFO.optInt("top_crop") - OMC.STRETCHINFO.optInt("bottom_crop"); 
		float hzStretch = (float)OMC.STRETCHINFO.optDouble("horizontal_stretch");
		float vtStretch = (float)OMC.STRETCHINFO.optDouble("vertical_stretch");
		double dScaledWidth = width * hzStretch;
		double dScaledHeight = height * vtStretch;
		double rot = (OMC.STRETCHINFO.optDouble("cw_rotate"));
		double rotRad = rot*Math.PI/180d;
		
		double rotHeight = Math.abs(dScaledHeight* Math.cos(rotRad)) + Math.abs(dScaledWidth*Math.sin(rotRad)) ;
		double rotWidth = Math.abs(dScaledHeight* Math.sin(rotRad)) + Math.abs(dScaledWidth*Math.cos(rotRad));
		System.out.println("WidgetWidth:" +thisWidgetWidth+ ", WidgetHt:"+thisWidgetHeight);
		System.out.println("rotWidth:" +rotWidth+ ", rotHt:"+rotHeight);
		float fFitGraphic = (float) Math.min(thisWidgetWidth/rotWidth, thisWidgetHeight/rotHeight);
		
		// Crop, Scale & Rotate the clock first
		
		finalcanvas.save();
		Matrix tempMatrix = OMC.getMatrix();
		tempMatrix.postScale(hzStretch, vtStretch);
		tempMatrix.postTranslate((float)((thisWidgetWidth-dScaledWidth)/2d), (float)((thisWidgetHeight-dScaledHeight)/2d));
		tempMatrix.postRotate((float)rot, thisWidgetWidth/2f, thisWidgetHeight/2f);
		tempMatrix.postScale(fFitGraphic, fFitGraphic, thisWidgetWidth/2f, thisWidgetHeight/2f);

		
		Paint pt = OMC.getPaint();
		pt.setAlpha(255);
		pt.setAntiAlias(true);
		pt.setFilterBitmap(true);
		pt.setDither(true);
		
		finalcanvas.setMatrix(tempMatrix);
		finalcanvas.drawBitmap(Bitmap.createBitmap(bitmap, 
				(int)(OMC.STRETCHINFO.optInt("left_crop")),
				(int)(OMC.STRETCHINFO.optInt("top_crop")),
				width, height),0,0, pt);

		bitmap.recycle();

		finalcanvas.restore();
		finalbitmap.prepareToDraw();
		OMC.returnMatrix(tempMatrix);
		OMC.returnPaint(pt);

		RemoteViews rv = new RemoteViews(context.getPackageName(),context.getResources().getIdentifier("omcwidget", "layout", OMC.PKGNAME));
		final int iViewID = context.getResources().getIdentifier("omcIV", "id", OMC.PKGNAME);

		System.out.println("finalbitmap width:" + finalbitmap.getWidth() + " ht:" + finalbitmap.getHeight());
		rv.setImageViewBitmap(iViewID, finalbitmap);
		
		// Do some fancy footwork here and adjust the average lag (so OMC's slowness is less apparent)
		// We will start at least 200 millis early.
		OMC.LEASTLAGMILLIS = Math.max(200l, (OMC.LEASTLAGMILLIS + (System.currentTimeMillis() - lStartTime))/2l);

		if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Engine","Calc. lead time for next tick: " + OMC.LEASTLAGMILLIS + "ms");

		// Blit the buffer over
		appWidgetManager.updateAppWidget(appWidgetId, rv);
		rv = new RemoteViews(context.getPackageName(),context.getResources().getIdentifier("omcwidget", "layout", OMC.PKGNAME));
		
		
    	// Now for overlay URIs
		OMC.OVERLAYURIS = new String[9];
		JSONObject temp = oTheme.optJSONObject("customURIs");
		if (temp == null) {
			//No custom URIs - always go to options screen
        	Intent intent = new Intent(OMC.CONTEXT, OMCPrefActivity.class);
        	intent.setData(Uri.parse("omc:"+appWidgetId));

			for (int i=0; i<9; i++){
				OMC.OVERLAYURIS[i] = intent.toUri(0);
			}
			
		} else {
			for (int i=0; i<9; i++){
				OMC.OVERLAYURIS[i] = temp.optString(OMC.COMPASSPOINTS[i]);
			}
		}

//        if (OMC.PREFS.getString("URI"+appWidgetId, "").equals("")) { 
////	Using a broadcast is more flexible, but less crash-proof. So we're not using it for now.
////        	Intent intent = new Intent("com.sunnykwong.omc.WIDGET_CONFIG");
////        	intent.setData(Uri.parse("omc:"+appWidgetId));
////        	PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//
//        	Intent intent = new Intent(context, OMCPrefActivity.class);
//        	intent.setData(Uri.parse("omc:"+appWidgetId));
//        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
//        	
//        	rv.setOnClickPendingIntent(context.getResources().getIdentifier("omcIV", "id", OMC.PKGNAME), pi);
//            for (int i = 0; i < 9; i++) {
//            	if (OMC.OVERLAYURI[i].equals("default")) {
//	            	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], pi);
//            	} else {
//    				Intent s = (new Intent(Intent.ACTION_VIEW,OMC.OVERLAYURI[i]));
//    				s.addCategory(Intent.CATEGORY_DEFAULT);
//
//            		rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i],            	
//            			PendingIntent.getActivity(context, 0, s,0));
//            	}
//            }
//        } else {
//
        	
        	try {
        		Intent intent;
	        	intent = new Intent(context, OMCPrefActivity.class);
	        	intent.setData(Uri.parse("omc:"+appWidgetId));
	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
            	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[0], pi);

            	if (OMC.PREFS.getString("URI"+appWidgetId, "").equals("")) {
    	        	intent = new Intent(context, OMCPrefActivity.class);
    	        	intent.setData(Uri.parse("omc:"+appWidgetId));
    	        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    	        	pi = PendingIntent.getActivity(context, 0, intent, 0);
        		} else if (OMC.PREFS.getString("URI"+appWidgetId, "").equals("noop")) {
                    // Kudos to Eric for solution to dummy out "unsetonlickpendingintent":
                    // http://groups.google.com/group/android-developers/browse_thread/thread/f9e80e5ce55bb1e0/78153eb730326488
                	// I'm not using it right now, but it's a useful hint nonetheless. Thanks!
    	        	pi = PendingIntent.getBroadcast(context, 0, OMC.DUMMYINTENT,
                		    PendingIntent.FLAG_UPDATE_CURRENT);
        		} else {
        			intent = Intent.parseUri(OMC.PREFS.getString("URI"+appWidgetId, ""), 0);
    	        	intent.setData(Uri.parse("omc:"+appWidgetId));
    	        	pi = PendingIntent.getActivity(context, 0, intent, 0);
        		}


	        	
	        	// NOTE BELOW:  We're only going from 1-9 (not 0-9)
	        	// Because we are skipping the NW corner.
	        	for (int i = 1; i < 9; i++) { 
		            	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], pi);
	            }
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
//        }
        	
        	if (OMC.PREFS.getBoolean("newbie" + appWidgetId, true)) {
        		if (OMC.DEBUG)Log.i(OMC.OMCSHORT+"Engine","Adding newbie ribbon to widget "+ appWidgetId + ".");
        		rv.setImageViewResource(context.getResources().getIdentifier("omcNB", "id", OMC.PKGNAME), context.getResources().getIdentifier("tapme", "drawable", OMC.PKGNAME));
        	} else {
        		rv.setImageViewResource(context.getResources().getIdentifier("omcNB", "id", OMC.PKGNAME), context.getResources().getIdentifier("transparent", "drawable", OMC.PKGNAME));
        	}

        	// OK, the IPC instructions are done; send them over to the homescreen.
        	appWidgetManager.updateAppWidget(appWidgetId, rv);
//        }
	}
	
	static Bitmap drawBitmapForWidget(final Context context, final int aWI) {
		final Bitmap resultBitmap;
		if (OMC.SCREENON) {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_8888);
		} else {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		}
		final Canvas resultCanvas = new Canvas(resultBitmap);
		resultCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);

		final String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME);
		//TODO
		// WHEN TESTING NEW THEME, UNCOMMENT THIS LINE 
		//String sTheme = TESTTHEME;

		JSONObject oTheme = OMC.getTheme(context, sTheme, OMC.THEMESFROMCACHE);
		if (oTheme==null) {
			Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.commit();
			return null;
		}
		try {
			oTheme = OMC.renderThemeObject(oTheme, aWI);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		final JSONArray layerlist = oTheme.optJSONArray("layers_bottomtotop");

		for (int i = 0; i < layerlist.length(); i++) {
			JSONObject layer = layerlist.optJSONObject(i);
			if (layer==null) {
				Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
				OMC.PREFS.edit()
						.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
						.commit();
				return null;
			}
			// Clear the text buffer first.
			String sTextBuffer="";
			
			//Skip any disabled layers
			if (layer.optBoolean("enabled")==false) continue;
			
			String sType = layer.optString("type");
			
			if (sType.equals("text")) {
				sTextBuffer = layer.optString("text");
				OMCWidgetDrawEngine.drawTextLayer(context, layer, sTheme, aWI, sTextBuffer, resultCanvas);
			}
			else if (sType.equals("panel"))OMCWidgetDrawEngine.drawPanelLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("flare"))OMCWidgetDrawEngine.drawFlareLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("quote"))OMCWidgetDrawEngine.drawQuoteLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("image"))OMCWidgetDrawEngine.drawBitmapLayer(context, layer, sTheme, aWI, resultCanvas);

		}
		
		return resultBitmap;
		
	}

	static Bitmap drawLayerForWidget(final Context context, final int aWI, final JSONObject oTheme, final String sLayer) {
		final Bitmap resultBitmap;
		if (OMC.SCREENON) {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_8888);
		} else {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		}
		final Canvas resultCanvas = new Canvas(resultBitmap);
		resultCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);

		final String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME);
		//TODO
		// WHEN TESTING NEW THEME, UNCOMMENT THIS LINE 
		//String sTheme = TESTTHEME;

		if (oTheme==null) {
			Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.commit();
			return null;
		}

		final JSONArray layerlist = oTheme.optJSONArray("layers_bottomtotop");

		for (int i = 0; i < layerlist.length(); i++) {
			JSONObject layer = layerlist.optJSONObject(i);
			if (!layer.optString("name").equals(sLayer)) continue;
			if (layer==null) {
				Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
				OMC.PREFS.edit()
						.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
						.commit();
				return null;
			}
			// Clear the text buffer first.
			String sTextBuffer="";
			
			//Skip any disabled layers
			if (layer.optBoolean("enabled")==false) continue;
			
			String sType = layer.optString("type");
			
			if (sType.equals("text")) {
				sTextBuffer = layer.optString("text");
				OMCWidgetDrawEngine.drawTextLayer(context, layer, sTheme, aWI, sTextBuffer, resultCanvas);
			}
			else if (sType.equals("panel"))OMCWidgetDrawEngine.drawPanelLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("flare"))OMCWidgetDrawEngine.drawFlareLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("quote"))OMCWidgetDrawEngine.drawQuoteLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("image"))OMCWidgetDrawEngine.drawBitmapLayer(context, layer, sTheme, aWI, resultCanvas);
		}
		
		return resultBitmap;
		
	}

	// This is where the theme-specific tweaks (regardless of layer) are processed.
	// Tweaks = hacks, but at least all the hacks are in one block of code.
	static void layerThemeTweaks(final Context context, final JSONObject layer, final String sTheme, final int aWI) {
		// do nothing
	}

	// This layer is pretty much dedicated to lens flare (bokeh beauty), 
	// but will need more tweaking for realism 
	static void drawFlareLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {
		Paint pt1= OMC.getPaint(), pt2 = OMC.getPaint();
		pt1.setAntiAlias(true);
		pt1.setStyle(Paint.Style.FILL_AND_STROKE);
		pt2.setAntiAlias(true);
		pt2.setARGB(32, 255, 255, 255);
		pt2.setStyle(Paint.Style.STROKE);

		//Depending on time of day, endpoints.
		final float ratio = (((OMC.TIME.hour+6)*60 + OMC.TIME.minute) % (12*60)) / (12f*60);
		final float x1 = OMC.WIDGETWIDTH * ratio;  
		final float y1 = OMC.WIDGETHEIGHT;
		final float x2 = OMC.WIDGETWIDTH - x1;
		final float y2 = 0;
		
		float dist = -0.45f;
		for (int i = 0; i < layer.optInt("number_circles"); i++) {
			pt1.setColor(OMC.FLARECOLORS[i]);
			dist += (float)(1.f/layer.optInt("number_circles")); 
			float x = (x2-x1) * dist + OMC.WIDGETWIDTH/2f;
			float y = (y2-y1) * dist + OMC.WIDGETHEIGHT/2f;
			cvas.drawCircle(x, y, OMC.FLARERADII[i], pt1);
			cvas.drawCircle(x, y, OMC.FLARERADII[i]+1, pt2);
		}
		
    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, layer, sTheme, aWI);
		OMC.returnPaint(pt1);
		OMC.returnPaint(pt2);
	}

	// Static rectangular panel.
	static void drawPanelLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {
		final RectF tempFGRect = new RectF();
		final RectF tempBGRect = new RectF();
		try {
			
			tempFGRect.left = (float)layer.getDouble("left");
			tempFGRect.top = (float)layer.getDouble("top");
			tempFGRect.right = (float)layer.getDouble("right");
			tempFGRect.bottom = (float)layer.getDouble("bottom");
		} catch (JSONException e) {
			Log.w(OMC.OMCSHORT + "Engine", " (panel) is missing left/top/right/bottom values!  Giving up.");
			if (OMC.DEBUG) e.printStackTrace();
			return;
		}
		final Paint pt1 = OMC.getPaint();
		pt1.setAntiAlias(true);
		try {
			pt1.setColor(Color.parseColor(layer.optString("fgcolor")));
		} catch (java.lang.IllegalArgumentException e) {
			// JSON has unknown color; maybe # is missing?
			try {
				pt1.setColor(Color.parseColor("#" + layer.optString("fgcolor")));
				Log.w(OMC.OMCSHORT+"Engine","Color missing #");
			} catch (java.lang.IllegalArgumentException ee) {
				// Still unknown color; default to white
				Log.w(OMC.OMCSHORT+"Engine","Color invalid");
				pt1.setColor(Color.WHITE);
				ee.printStackTrace();
			}
		}
		final Paint pt2 = OMC.getPaint();
		pt2.setAntiAlias(true);
		try {
			pt2.setColor(Color.parseColor(layer.optString("bgcolor")));
		} catch (java.lang.IllegalArgumentException e) {
			// JSON has unknown color; maybe # is missing?
			try {
				pt2.setColor(Color.parseColor("#" + layer.optString("bgcolor")));
				Log.w(OMC.OMCSHORT+"Engine","Color missing #");
			} catch (java.lang.IllegalArgumentException ee) {
				// Still unknown color; default to white
				Log.w(OMC.OMCSHORT+"Engine","Color invalid");
				pt2.setColor(Color.WHITE);
				ee.printStackTrace();
			}
		}

    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, layer, sTheme, aWI);
    	
		//Draw the SFX
		if (layer.optString("render_style").equals("emboss")) {
			tempBGRect.left = tempFGRect.left-1;
			tempBGRect.top = tempFGRect.top-1;
			tempBGRect.right = tempFGRect.right-1;
			tempBGRect.bottom = tempFGRect.bottom-1;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner"), layer.optInt("ycorner"), pt2);
			tempBGRect.left+=2;
			tempBGRect.top+=2;
			tempBGRect.right+=2;
			tempBGRect.bottom+=2;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner"), layer.optInt("ycorner"), pt2);
		} else if (layer.optString("render_style").equals("shadow")) {
			tempBGRect.left = tempFGRect.left+3;
			tempBGRect.top = tempFGRect.top+3;
			tempBGRect.right = tempFGRect.right+3;
			tempBGRect.bottom = tempFGRect.bottom+3;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner"), layer.optInt("ycorner"), pt2);
		}
		//Either way, draw the proper panel
		cvas.drawRoundRect(tempFGRect, layer.optInt("xcorner"), layer.optInt("ycorner"), pt1);
		OMC.returnPaint(pt1);
		OMC.returnPaint(pt2);
	}

	//Bitmap layer.
	static void drawBitmapLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {
		final Paint pt1 = OMC.getPaint();
		pt1.reset();
		pt1.setAntiAlias(true);
		pt1.setFlags(Paint.FILTER_BITMAP_FLAG);

    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, layer, sTheme, aWI);

		
		// Blit the buffer over
		Bitmap tempBitmap = OMC.getBitmap(sTheme, layer.optString("filename"));
		if (tempBitmap==null) {
			Toast.makeText(context, "Theme not found in memory or in cache directory.\nPlease go to settings screen and re-select your theme.", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.commit();
			return;
		}

		// Prepare the transformation matrix.

		Matrix tempMatrix = OMC.getMatrix();
		tempMatrix.postTranslate(-tempBitmap.getWidth()/2f, -tempBitmap.getHeight()/2f);
		tempMatrix.postScale((float)layer.optDouble("horizontal_stretch"),(float)layer.optDouble("vertical_stretch"));
		tempMatrix.postRotate((float)layer.optDouble("cw_rotate"));

		tempMatrix.postTranslate((tempBitmap.getWidth()*(float)layer.optDouble("horizontal_stretch"))/2f
				+ layer.optInt("x"), 
				(tempBitmap.getHeight()*(float)layer.optDouble("vertical_stretch"))/2f
				+ layer.optInt("y"));

		tempBitmap.setDensity(DisplayMetrics.DENSITY_HIGH);
		cvas.drawBitmap(tempBitmap,tempMatrix,pt1);
		OMC.returnPaint(pt1);
		OMC.returnMatrix(tempMatrix);
	}

	// Quote layer.  Set the Text to be shown before passing to drawTextLayer.
	static void drawQuoteLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {
		final JSONArray talkbacks = OMC.loadStringArray(sTheme, aWI, layer.optString("array"));
		String resultText;
		if (talkbacks == null) resultText="Tap me to add quotes!";
		else resultText = talkbacks.optString(OMC.RND.nextInt(talkbacks.length()));
		OMCWidgetDrawEngine.drawTextLayer(context, layer, sTheme, aWI, resultText, cvas);
	}

	// Text layer.  Written this way so we can have as many as we want with minimal effort.
	static void drawTextLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final String text, final Canvas cvas) {
		final Paint pt1 = OMC.getPaint();
		final Paint pt2 = OMC.getPaint();
		pt1.setAntiAlias(true);
		final Typeface tempTypeface = OMC.getTypeface(sTheme, layer.optString("filename"));
		if (tempTypeface==null) {
			Toast.makeText(context, "Error loading theme typeface.\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.commit();
			return;
		}
		pt1.setTypeface(tempTypeface);
		pt1.setTextSize(layer.optInt("text_size"));
		pt1.setTextSkewX((float)layer.optDouble("text_skew"));
		String sTemp = layer.optString("text_stretch");
		if (sTemp==null) {
			pt1.setTextScaleX(1f);
		} else {
			int iTemp;
			if (sTemp.startsWith("f")) {
				pt1.setTextScaleX(1f);
				float fFactor = Float.parseFloat(sTemp.substring(1))/OMCWidgetDrawEngine.getSpannedStringWidth(new SpannedString(Html.fromHtml(text)),pt1);
				pt1.setTextScaleX(fFactor);
			} else if ((iTemp = sTemp.indexOf("m"))!= -1) {
				pt1.setTextScaleX(Float.parseFloat(sTemp.substring(0,iTemp)));
				int iMax = Integer.parseInt(sTemp.substring(iTemp+1));
				int iLength = OMCWidgetDrawEngine.getSpannedStringWidth(new SpannedString(Html.fromHtml(text)),pt1); 
				if (iLength <= iMax){
					//do nothing, PT1 properly set
				} else {
					pt1.setTextScaleX(((float)iMax)/iLength);
				}
			} else {
				pt1.setTextScaleX((float)layer.optDouble("text_stretch"));
			}
		}
		
		try {
			pt1.setColor(Color.parseColor(layer.optString("fgcolor")));
		} catch (java.lang.IllegalArgumentException e) {
			// JSON has unknown color; maybe # is missing?
			try {
				pt1.setColor(Color.parseColor("#" + layer.optString("fgcolor")));
				Log.w(OMC.OMCSHORT+"Engine","Color missing #");
			} catch (java.lang.IllegalArgumentException ee) {
				// Still unknown color; default to white
				Log.w(OMC.OMCSHORT+"Engine","Color invalid");
				pt1.setColor(Color.WHITE);
				ee.printStackTrace();
			}
		}
		
		float fRot = (float)layer.optDouble("cw_rotate");

		if (layer.optString("text_align").equals("center")) {
			pt1.setTextAlign(Paint.Align.CENTER);
		} else if (layer.optString("text_align").equals("left")) {
			pt1.setTextAlign(Paint.Align.LEFT);
		} else if (layer.optString("text_align").equals("right")) {
			pt1.setTextAlign(Paint.Align.RIGHT);
		};

		pt2.reset();
		pt2.setAntiAlias(true);
		pt2.setTypeface(tempTypeface);
		pt2.setTextSize(pt1.getTextSize());
		pt2.setTextSkewX(pt1.getTextSkewX());
		pt2.setTextScaleX(pt1.getTextScaleX());
		try {
			pt2.setColor(Color.parseColor(layer.optString("bgcolor")));
		} catch (java.lang.IllegalArgumentException e) {
			// JSON has unknown color; maybe # is missing?
			try {
				pt2.setColor(Color.parseColor("#" + layer.optString("bgcolor")));
				Log.w(OMC.OMCSHORT+"Engine","Color missing #");
			} catch (java.lang.IllegalArgumentException ee) {
				// Still unknown color; default to white
				Log.w(OMC.OMCSHORT+"Engine","Color invalid");
				pt2.setColor(Color.WHITE);
				ee.printStackTrace();
			}
		}
		pt2.setTextAlign(pt1.getTextAlign());

    	// theme-specific tweaks.
		OMCWidgetDrawEngine.layerThemeTweaks(context, layer, sTheme, aWI);
    	
    	// Draw the layer.
		OMCWidgetDrawEngine.fancyDrawText(
    			layer.optString("render_style"),
    			cvas,
    			text,
    			layer.optInt("x"),
    			layer.optInt("y"),
    			pt1,
    			pt2,
    			fRot);
		OMC.returnPaint(pt1);
		OMC.returnPaint(pt2);
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
	
	static synchronized void fancyDrawSpanned(final Canvas cvas, final String str, final int x, final int y, final Paint pt, final float fRot) {
		final SpannedString ss = new SpannedString(Html.fromHtml(str));
		Paint ptTemp = new Paint(pt);
		ptTemp.setTextAlign(Paint.Align.LEFT);
		int bufferWidth = Math.max(OMCWidgetDrawEngine.getSpannedStringWidth(ss, pt),1);
		int bufferHeight = Math.max(pt.getFontMetricsInt().bottom - pt.getFontMetricsInt().top,1);
		int iCursor = 0;
		
		OMC.ROTBUFFER.eraseColor(Color.TRANSPARENT);
		OMC.ROTBUFFER.setDensity(DisplayMetrics.DENSITY_HIGH);
		final Canvas rotCANVAS = OMC.BMPTOCVAS.get(OMC.ROTBUFFER);
			
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

			rotCANVAS.drawText(ss.subSequence(iStart, iEnd).toString(), iCursor, 0-pt.getFontMetricsInt().top, ptTemp);
			iCursor+=ptTemp.measureText(ss.toString().substring(iStart, iEnd));
			iStart = iEnd;
		}
		final Matrix tempMatrix = OMC.getMatrix();
		tempMatrix.postTranslate(-bufferWidth/2f, pt.getFontMetricsInt().top);
		tempMatrix.postRotate(fRot);
		if (pt.getTextAlign() == Paint.Align.LEFT) {
			// Do nothing
			tempMatrix.postTranslate(bufferWidth/2f+x, y);
		} else if (pt.getTextAlign() == Paint.Align.CENTER) {
			tempMatrix.postTranslate(x, y);
		} else if (pt.getTextAlign() == Paint.Align.RIGHT) {
			tempMatrix.postTranslate(-bufferWidth/2f+x, y);
		} else {
			// Huh? do nothing
			tempMatrix.postTranslate(bufferWidth/2f+x, y);
		}
		pt.setFilterBitmap(true);
		cvas.drawBitmap(OMC.ROTBUFFER, tempMatrix, pt);
		pt.setFilterBitmap(false);

		OMC.returnMatrix(tempMatrix);
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



}