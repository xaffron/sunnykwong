package com.sunnykwong.omc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.MemoryFile;
import android.text.Html;
import android.text.SpannedString;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class OMCWidgetDrawEngine {
	static final String TESTTHEME="";

	// Needs to be synchronized now that we have different widget sizes 
	// calling this same method creating a potential race condition
	static synchronized void updateAppWidget(Context context, ComponentName cName) {
		// Set target time slightly ahead to account for lag
		OMC.TIME.set(((System.currentTimeMillis()+OMC.LEASTLAGMILLIS)/1000l)*1000l);
		
		AppWidgetManager aWM = AppWidgetManager.getInstance(context);

		for (int i=0; i<(aWM.getAppWidgetIds(cName)==null? 0: aWM.getAppWidgetIds(cName).length); i++) {

			// v139: Fix strange arrayoutofboundsexception (race condition?)
			 if (i >= aWM.getAppWidgetIds(cName).length) break;
			 
//             if (OMC.SCREENON && OMC.WIDGETBMPMAP.containsKey(aWM.getAppWidgetIds(cName)[i]) && OMC.LEASTLAGMILLIS > 500l) {
//            	// If this clock takes more than 0.5 sec to render, then blit cached bitmap over first
//            	RemoteViews rv = new RemoteViews(context.getPackageName(),OMC.RLayoutId("omcwidget"));
//        		if (OMC.ALTRENDERING) {
//       	        	File outTemp = new File(OMC.CACHEPATH + aWM.getAppWidgetIds(cName)[i] +"cache.png");
//       		        if (outTemp.exists()&&outTemp.canRead()) {
//       		        	String sUriString = OMC.FREEEDITION?
//       		        			"content://com.sunnykwong.freeomc/widgets?random="+Math.random()+"&awi="+aWM.getAppWidgetIds(cName)[i]
//       		        			:"content://com.sunnykwong.omc/widgets?random="+Math.random()+"&awi="+aWM.getAppWidgetIds(cName)[i];
//       		        	rv.setImageViewUri(OMC.RId("omcIV"), Uri.parse(sUriString));
//       		        } else {
//       		        	// Do nothing
//       					//rv.setImageViewBitmap(OMC.RId("omcIV"), OMC.WIDGETBMPMAP.get(aWM.getAppWidgetIds(cName)[i]));
//       		        }
//        		} else {
//	                rv.setImageViewBitmap(OMC.RId("omcIV"),
//	                		OMC.WIDGETBMPMAP.get(aWM.getAppWidgetIds(cName)[i]));
//        		}
//                aWM.updateAppWidget(aWM.getAppWidgetIds(cName)[i], rv);                    
//                rv = null;
//              }

			// v139: Fix strange arrayoutofboundsexception (race condition?)
			// v141: Cram in clock adjustment here, too
			 if (i >= aWM.getAppWidgetIds(cName).length) break;
			 
     		if (!OMC.PREFS.getString("sTimeZone"+aWM.getAppWidgetIds(cName)[i],"default").equals("default")) {
     			OMC.TIME.switchTimezone(OMC.PREFS.getString("sTimeZone"+aWM.getAppWidgetIds(cName)[i],TimeZone.getDefault().getID()));
     		} else {
     			OMC.TIME.switchTimezone(TimeZone.getDefault().getID());
     		}
     		if (!OMC.PREFS.getString("clockAdjustment"+aWM.getAppWidgetIds(cName)[i],"0").equals("0")) {
     			int offset = Integer.parseInt(OMC.PREFS.getString("clockAdjustment"+aWM.getAppWidgetIds(cName)[i],"0"));
     			OMC.TIME.minute+=offset;
     			OMC.TIME.normalize(false);
     		}
			 // v139: Fix strange arrayoutofboundsexception (race condition?)
			 if (i >= aWM.getAppWidgetIds(cName).length) break;
			 
			OMCWidgetDrawEngine.updateAppWidget(context, aWM, aWM.getAppWidgetIds(cName)[i], cName);
		}

		OMC.LASTUPDATEMILLIS = System.currentTimeMillis();
		

	}
	
	static synchronized void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId, ComponentName cName) { 
		
		long lStartTime = System.currentTimeMillis();

		if (OMC.DEBUG)Log.i(OMC.OMCSHORT + "Engine", "Redrawing widget" + appWidgetId + " (" + OMC.PREFS.getString("widgetTheme"+appWidgetId, "")+ ") at " + OMC.TIME.format("%T"));
		
		
		// Get theme.  (Nowadays, OMC.getTheme takes care of caching/importing.)
		String sTheme = OMC.PREFS.getString("widgetTheme"+appWidgetId,OMC.DEFAULTTHEME);

		if (TESTTHEME.length()>0) sTheme = TESTTHEME;
		
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
		// OMC.BUFFER (square, bitmap buffer) is updated and copy-returned.
		
		final Bitmap bitmap = OMCWidgetDrawEngine.drawBitmapForWidget(context,appWidgetId, !OMC.IDLEMODE);

		//
		//Step 1: 
		// look for this size's custom scaling info. 
		// Scaling info is in 00control.json: CustomScaling.  This section is optional in the JSON for legacy clocks.
		//
		OMC.STRETCHINFO = oTheme.optJSONObject("customscaling");
		String sWidgetSize = cName.toShortString().substring(cName.toShortString().length()-4,cName.toShortString().length()-1);

		//Step 2: 
		// Depending on the widget size, we make assumptions on the widget shape.
		// These assumptions are necessary to prevent the homescreen from scaling our widget down
		// when/if the final widget is rotated via "customscaling"->"cw_rotate".
		//
		int thisWidgetWidth = OMC.WIDGETWIDTH;
		int thisWidgetHeight = (int)(OMC.WIDGETWIDTH*1.2);
		if (sWidgetSize.equals("4x2") || sWidgetSize.equals("5x2")) {
			thisWidgetHeight /= 2;
		} else if (sWidgetSize.equals("4x1") || sWidgetSize.equals("5x1")) {
			thisWidgetHeight /= 4;
		} else if (sWidgetSize.equals("3x3")) {
			thisWidgetWidth=(int)(thisWidgetWidth*0.75);
			thisWidgetHeight = (int)(thisWidgetHeight*0.75);
		} else if (sWidgetSize.equals("3x1")) {
			thisWidgetWidth=(int)(thisWidgetWidth*0.75);
			thisWidgetHeight /= 4;
		} else if (sWidgetSize.equals("2x2")) {
			thisWidgetWidth /= 2;
			thisWidgetHeight /= 2;
		} else if (sWidgetSize.equals("2x1")) {
			thisWidgetWidth /= 2;
			thisWidgetHeight /= 4;
		} else if (sWidgetSize.equals("1x3")) {
			thisWidgetWidth /= 4;
			thisWidgetHeight = (int)(thisWidgetHeight*0.75);
		}

		//Step 3:
		// if no custom scaling info present, use default stretch info
		//
		boolean bDefaultScaling=false;
		if (OMC.STRETCHINFO==null) {
			bDefaultScaling=true;
		} else {
			String sUseWidgetSize="";
			if (sWidgetSize.startsWith("5x")) {
				sUseWidgetSize = "4"+ sWidgetSize.substring(1);
			} else {
				sUseWidgetSize = sWidgetSize;
			}
			OMC.STRETCHINFO = OMC.STRETCHINFO.optJSONObject(sUseWidgetSize);
		}
		if (OMC.STRETCHINFO==null) {
			bDefaultScaling=true;
		}
		if (bDefaultScaling) {
			JSONObject oDefaultScaling = new JSONObject(); 
			try {
				if (sWidgetSize.equals("4x4") || sWidgetSize.equals("5x4")) {
					oDefaultScaling.put("horizontal_stretch", 1);
					oDefaultScaling.put("vertical_stretch", 1);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 0);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("4x2") || sWidgetSize.equals("5x2")) {
					oDefaultScaling.put("horizontal_stretch", 1);
					oDefaultScaling.put("vertical_stretch", 1);
					oDefaultScaling.put("top_crop", 0);
					oDefaultScaling.put("bottom_crop", 180);
					oDefaultScaling.put("left_crop", 0);
					oDefaultScaling.put("right_crop", 0);
					oDefaultScaling.put("cw_rotate", 0);
				} else if (sWidgetSize.equals("4x1") || sWidgetSize.equals("5x1")) {
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

		//Step 4:
		// Now, based on the full canvas size, we estimate the size of the final widget canvas
		// after cropping, stretching and rotating.
		// Note that rotation in android grows the size of the bitmap because android doesn't throw away pixels
		//
		int width = (int)(bitmap.getWidth()- OMC.STRETCHINFO.optInt("left_crop")*OMC.fFinalScaling - OMC.STRETCHINFO.optInt("right_crop")*OMC.fFinalScaling); 
		int height = (int)(bitmap.getHeight() - OMC.STRETCHINFO.optInt("top_crop")*OMC.fFinalScaling - OMC.STRETCHINFO.optInt("bottom_crop")*OMC.fFinalScaling); 
		System.out.println("width: " + width);
		System.out.println("height: " + height);
		System.out.println("bmpwidth: " + bitmap.getWidth());
		System.out.println("bmpheight: " + bitmap.getHeight());
		System.out.println("scaling: " + OMC.fFinalScaling);
		float hzStretch = (float)OMC.STRETCHINFO.optDouble("horizontal_stretch");
		float vtStretch = (float)OMC.STRETCHINFO.optDouble("vertical_stretch");
		double dScaledWidth = width * hzStretch;
		double dScaledHeight = height * vtStretch;
		double rot = (OMC.STRETCHINFO.optDouble("cw_rotate"));
		double rotRad = rot*Math.PI/180d;
		
		double rotWidth = Math.abs(dScaledHeight* Math.sin(rotRad)) + Math.abs(dScaledWidth*Math.cos(rotRad));
		double rotHeight = Math.abs(dScaledHeight* Math.cos(rotRad)) + Math.abs(dScaledWidth*Math.sin(rotRad)) ;

		float fSqueezeFactor=1f;
		if (rotWidth > thisWidgetWidth) fSqueezeFactor = thisWidgetWidth/(float)rotWidth;
		if (rotHeight > thisWidgetHeight) fSqueezeFactor = Math.min(fSqueezeFactor, thisWidgetHeight/(float)rotHeight);

		hzStretch *= fSqueezeFactor;
		vtStretch *= fSqueezeFactor;
		dScaledWidth = width * hzStretch;
		dScaledHeight = height * vtStretch;
		rotWidth = Math.abs(dScaledHeight* Math.sin(rotRad)) + Math.abs(dScaledWidth*Math.cos(rotRad));
		rotHeight = Math.abs(dScaledHeight* Math.cos(rotRad)) + Math.abs(dScaledWidth*Math.sin(rotRad)) ;
		
		//Step 5:
		// Now that we know the rotated size of the canvas, we want to constrain it to the actual widget size.  
		// Again, this is necessary to prevent the homescreen from scaling our widget down
		// when/if the final widget is rotated via "customscaling"->"cw_rotate".
		// We will figure out the size ratios of height and width, then select the less destructive (closer to 1x) scaling
		// This results in center-cropped display of the OMC canvas.
		//
		thisWidgetWidth = Math.min((int)rotWidth, thisWidgetWidth);
		thisWidgetHeight = Math.min((int)rotHeight, thisWidgetHeight);
		
		float fFitGraphic = (float) Math.max(thisWidgetWidth/rotWidth, thisWidgetHeight/rotHeight);

		//Step 6:
		// <LEGACY>
		// Create the final bitmap to be sent over to the homescreen app.  We are using 16-bit because 
		// sending a 480x480x32 bitmap over IPC will choke half the time... 
		// <Alternate>
		// We are actually writing the 32bit bitmap to flash and decompressing immediately.
		// Flash wear should be minimal if the user doesn't keep the screen on 24x7.
		//
		// Note that the bitmaps are put in a hashmap keyed by the appwidgetID.
		//
		final Bitmap finalbitmap;
		if (!OMC.WIDGETBMPMAP.containsKey(appWidgetId)) {
			
			finalbitmap=Bitmap.createBitmap(thisWidgetWidth,thisWidgetHeight,OMC.ALTRENDERING?Bitmap.Config.ARGB_8888:Bitmap.Config.ARGB_4444);
			OMC.WIDGETBMPMAP.put(appWidgetId, finalbitmap);
					} else {
			if (OMC.WIDGETBMPMAP.get(appWidgetId).getWidth() != thisWidgetWidth ||
					OMC.WIDGETBMPMAP.get(appWidgetId).getHeight() != thisWidgetHeight) {
	    		if (!OMC.WIDGETBMPMAP.get(appWidgetId).isRecycled()) OMC.WIDGETBMPMAP.get(appWidgetId).recycle();
	    		OMC.WIDGETBMPMAP.remove(appWidgetId);
				finalbitmap=Bitmap.createBitmap(thisWidgetWidth,thisWidgetHeight,OMC.ALTRENDERING?Bitmap.Config.ARGB_8888:Bitmap.Config.ARGB_4444);
				OMC.WIDGETBMPMAP.put(appWidgetId, finalbitmap);				
			} else {
				finalbitmap=OMC.WIDGETBMPMAP.get(appWidgetId);
			}
		}

		finalbitmap.setDensity(DisplayMetrics.DENSITY_HIGH);
		finalbitmap.eraseColor(Color.TRANSPARENT);

		Canvas finalcanvas = OMC.BMPTOCVAS.get(finalbitmap);
		if (finalcanvas==null) {
			finalcanvas = new Canvas(finalbitmap);
			OMC.BMPTOCVAS.put(finalbitmap, finalcanvas);
		}

		//Step 7:
		// Now we do the actual Crop, Scale & Rotate.
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

		//Step 8:
		// The actual draw to final bitmap.
		
		finalcanvas.setMatrix(tempMatrix);
		finalcanvas.drawBitmap(Bitmap.createBitmap(bitmap, 
				((int)(OMC.STRETCHINFO.optInt("left_crop")*OMC.fFinalScaling)),
				((int)(OMC.STRETCHINFO.optInt("top_crop")*OMC.fFinalScaling)),
				width, height),0,0, pt);


		//Step 9:
		// Cleaning up.  The bitmap used for scaling is recycled, and matrix+paint returned to pool.
		bitmap.recycle();

		finalcanvas.restore();
		finalbitmap.prepareToDraw();
		OMC.returnMatrix(tempMatrix);
		OMC.returnPaint(pt);

        //Step 10:
		// Instructing the final bitmap to be sent over to the remote view (specifically, the homescreen).
		// Note that the final bitmap isn't actually sent until Step XX below.
		//
		
		RemoteViews rv = new RemoteViews(context.getPackageName(),OMC.RLayoutId("omcwidget"));
		final int iViewID = OMC.RId("omcIV");

		// Alternative Rendering for pre-JellyBean devices - large widgets will cause
		// Failed Binder Transaction errors, so need to compress/write to sd card, then 
		// ask launcher to retrieve from sd card.  Waste of CPU time and battery, but needed.
		if (OMC.ALTRENDERING) {
	        try {
	        	File outTemp = new File(OMC.CACHEPATH + appWidgetId +"cache.png");
                FileOutputStream fos = new FileOutputStream(outTemp);
	        	finalbitmap.compress(CompressFormat.PNG, 100, fos);
		        fos.close();

		        if (outTemp.exists()&&outTemp.canRead()) {
   		        	String sUriString = OMC.FREEEDITION?
   		        			"content://com.sunnykwong.freeomc/widgets?random="+Math.random()+"&awi="+appWidgetId
   		        			:"content://com.sunnykwong.omc/widgets?random="+Math.random()+"&awi="+appWidgetId;
   		        	// If we lag too much, the file might not finish writing.
   		        	// Wait a bit for the file to finish writing, up to a max of .5 seconds.
   		        	int iMaxWaitMillis = 500;
   		        	int iWaitSoFar=0;
   		        	if (!outTemp.canRead() && iWaitSoFar<iMaxWaitMillis){
   		        		iWaitSoFar+=50;
   		        		Thread.sleep(50);
   		        	}
   		        	rv.setImageViewUri(iViewID, Uri.parse(sUriString));

		        } else {
					rv.setImageViewBitmap(iViewID, finalbitmap);
		        }
	        } catch (Exception e) {
	        	e.printStackTrace();
	        }
	    // Regular Rendering for Jelly Bean and later devices - the bitmaps (usually much larger than 1MB) are
	    // simply sent through IPC without issue.
		} else {
			rv.setImageViewBitmap(iViewID, finalbitmap);
		}

		// v141 Removing multiple sets of RV instructions because Apex launcher chokes

		// Do some fancy footwork here and adjust the average lag (so OMC's slowness is less apparent)
		long lDuration = (System.currentTimeMillis() - lStartTime);
		OMC.LEASTLAGMILLIS = (long)(OMC.LEASTLAGMILLIS * 0.8) + (long)(lDuration *0.2);
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Engine","Draw Time: " + lDuration + "ms");

		if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Engine","Calc. lead time for next tick: " + OMC.LEASTLAGMILLIS + "ms");

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

    	try {
    		Intent intent;
        	intent = new Intent(context, OMCPrefActivity.class);
        	intent.setData(Uri.parse("omc:"+appWidgetId));
        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
        	rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[0], pi);

        	// NOTE BELOW:  We're only going from 1-9 (not 0-9)
        	// Because we are skipping the NW corner.
        	for (int i = 1; i < 9; i++) { 
        		final String sPrefString = OMC.PREFS.getString("URI"+OMC.COMPASSPOINTS[i]+appWidgetId, "");
        		if (sPrefString.equals("")||sPrefString.equals("default")) {
		        	intent = new Intent(context, OMCPrefActivity.class);
		        	intent.setData(Uri.parse("omc:"+appWidgetId));
		        	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		        	pi = PendingIntent.getActivity(context, 0, intent, 0);
	    		} else if (sPrefString.equals("noop")) {
	                // Kudos to Eric for solution to dummy out "unsetonlickpendingintent":
	                // http://groups.google.com/group/android-developers/browse_thread/thread/f9e80e5ce55bb1e0/78153eb730326488
		        	pi = PendingIntent.getBroadcast(context, 0, OMC.DUMMYINTENT,
	            		    PendingIntent.FLAG_UPDATE_CURRENT);
	    		} else if (sPrefString.equals("alarms")) {
		        	pi = OMC.ALARMCLOCKPENDING;
	    		} else if (sPrefString.equals("batt")) {
		        	pi = OMC.BATTUSAGEPENDING;
	    		} else if (sPrefString.equals("wrefresh")) {
		        	pi = OMC.WEATHERREFRESHPENDING;
	    		} else if (sPrefString.equals("weather")) {
		        	pi = OMC.WEATHERFORECASTPENDING;
	    		} else {
	    			intent = Intent.parseUri(sPrefString, 0);
		        	intent.setData(Uri.parse("omc:"+appWidgetId));
		        	pi = PendingIntent.getActivity(context, 0, intent, 0);
	    		}
	            rv.setOnClickPendingIntent(OMC.OVERLAYRESOURCES[i], pi);
            }
    	} catch (Exception e) {
    		e.printStackTrace();
    	}

    	//  If we're a new install or a new widget, add a "newbie ribbon" so the user knows how to get to options
    	if (OMC.PREFS.getBoolean("newbie" + appWidgetId, true)) {
    		if (OMC.DEBUG)Log.i(OMC.OMCSHORT+"Engine","Adding newbie ribbon to widget "+ appWidgetId + ".");
    		rv.setImageViewResource(OMC.RId("omcNB"), OMC.RDrawableId("tapme"));
    	} else {
    		rv.setImageViewResource(OMC.RId("omcNB"), OMC.RDrawableId("transparent"));
    	}

    	//Step XX:
    	// OK, the IPC instructions are done; send them over to the homescreen.
    	//
		appWidgetManager.updateAppWidget(appWidgetId, rv);

	}
	
	static Bitmap drawBitmapForWidget(final Context context, final int aWI, final boolean bHighResDraw) {
		final Bitmap resultBitmap;

		final String sTheme = TESTTHEME.length()==0?
				OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME):
				TESTTHEME;

		JSONObject oTheme = OMC.getTheme(context, sTheme, OMC.THEMESFROMCACHE);
		if (oTheme==null) {
			Toast.makeText(context, "Error loading theme.\nRestoring default look...", Toast.LENGTH_SHORT).show();
			OMC.PREFS.edit()
					.putString("widgetTheme"+aWI,OMC.DEFAULTTHEME)
					.commit();
			return null;
		}
		// v1.4.1: HD Rendering.
		final int iWidgetWidth = oTheme.optInt("canvaswidth",480);
		final int iWidgetHeight = oTheme.optInt("canvasheight",480);

		//v148: Tweak widget widths.
		OMC.setWidgetWidths(OMC.CONTEXT,bHighResDraw);
		
		//v147: HD scaling.
		OMC.dFinalScaling=1;
		OMC.fFinalScaling = 1f;
		if (iWidgetWidth==480) {
			OMC.dFinalScaling = OMC.WIDGETWIDTH/480d;
			OMC.fFinalScaling = OMC.WIDGETWIDTH/480f;
		}

		if (OMC.SCREENON) {
			 resultBitmap= Bitmap.createBitmap((int)(iWidgetWidth*OMC.fFinalScaling),(int)(iWidgetHeight*OMC.fFinalScaling),Bitmap.Config.ARGB_8888);
		} else {
			 resultBitmap= Bitmap.createBitmap((int)(iWidgetWidth*OMC.fFinalScaling),(int)(iWidgetHeight*OMC.fFinalScaling),Bitmap.Config.ARGB_4444);
		}
		final Canvas resultCanvas = new Canvas(resultBitmap);
		resultCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);


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
			else if (sType.equals("arc"))OMCWidgetDrawEngine.drawArcLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("flare"))OMCWidgetDrawEngine.drawFlareLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("quote"))OMCWidgetDrawEngine.drawQuoteLayer(context, layer, sTheme, aWI, resultCanvas);
			else if (sType.equals("image"))OMCWidgetDrawEngine.drawBitmapLayer(context, layer, sTheme, aWI, resultCanvas);

		}
		
		return resultBitmap;
		
	}

	static Bitmap drawLayerForWidget(final Context context, final int aWI, final JSONObject oTheme, final String sLayer, final boolean bHighResDraw) {
		final Bitmap resultBitmap;
		if (OMC.SCREENON) {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_8888);
		} else {
			 resultBitmap= Bitmap.createBitmap(OMC.WIDGETWIDTH,OMC.WIDGETHEIGHT,Bitmap.Config.ARGB_4444);
		}
		final Canvas resultCanvas = new Canvas(resultBitmap);
		resultCanvas.setDensity(DisplayMetrics.DENSITY_HIGH);

		final String sTheme = OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME);
		
		// WHEN TESTING NEW THEME, UNCOMMENT THIS LINE 
		// String sTheme = TESTTHEME;

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
			else if (sType.equals("arc"))OMCWidgetDrawEngine.drawArcLayer(context, layer, sTheme, aWI, resultCanvas);
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
			dist += (1.f/layer.optInt("number_circles")); 
			float x = (x2-x1) * dist + OMC.WIDGETWIDTH/2f;
			float y = (y2-y1) * dist + OMC.WIDGETHEIGHT/2f;
			cvas.drawCircle(x, y, OMC.FLARERADII[i]*OMC.fFinalScaling, pt1);
			cvas.drawCircle(x, y, OMC.FLARERADII[i]*OMC.fFinalScaling+OMC.fFinalScaling, pt2);
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
			
			tempFGRect.left = (float)layer.getDouble("left") * OMC.fFinalScaling;
			tempFGRect.top = (float)layer.getDouble("top") * OMC.fFinalScaling;
			tempFGRect.right = (float)layer.getDouble("right") * OMC.fFinalScaling;
			tempFGRect.bottom = (float)layer.getDouble("bottom") * OMC.fFinalScaling;
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
			tempBGRect.left = tempFGRect.left - OMC.fFinalScaling;
			tempBGRect.top = tempFGRect.top - OMC.fFinalScaling;
			tempBGRect.right = tempFGRect.right-OMC.fFinalScaling;
			tempBGRect.bottom = tempFGRect.bottom-OMC.fFinalScaling;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner")* OMC.fFinalScaling, layer.optInt("ycorner")* OMC.fFinalScaling, pt2);
			tempBGRect.left+=2* OMC.fFinalScaling;
			tempBGRect.top+=2* OMC.fFinalScaling;
			tempBGRect.right+=2* OMC.fFinalScaling;
			tempBGRect.bottom+=2* OMC.fFinalScaling;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner")* OMC.fFinalScaling, layer.optInt("ycorner")* OMC.fFinalScaling, pt2);
		} else if (layer.optString("render_style").equals("shadow")) {
			tempBGRect.left = tempFGRect.left+3* OMC.fFinalScaling;
			tempBGRect.top = tempFGRect.top+3* OMC.fFinalScaling;
			tempBGRect.right = tempFGRect.right+3* OMC.fFinalScaling;
			tempBGRect.bottom = tempFGRect.bottom+3* OMC.fFinalScaling;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner")* OMC.fFinalScaling, layer.optInt("ycorner")* OMC.fFinalScaling, pt2);
		} else if (layer.optString("render_style").startsWith("shadow")) {
			int iShadowWidth = (int)(Integer.parseInt(layer.optString("render_style").substring(7))* OMC.fFinalScaling);
			tempBGRect.left = tempFGRect.left+iShadowWidth;
			tempBGRect.top = tempFGRect.top+iShadowWidth;
			tempBGRect.right = tempFGRect.right+iShadowWidth;
			tempBGRect.bottom = tempFGRect.bottom+iShadowWidth;
			cvas.drawRoundRect(tempBGRect, layer.optInt("xcorner")* OMC.fFinalScaling, layer.optInt("ycorner")* OMC.fFinalScaling, pt2);
		} else if (layer.optString("render_style").startsWith("glow")) {
			pt1.setShadowLayer(Float.parseFloat(layer.optString("render_style").substring(5))* OMC.fFinalScaling, 0f, 0f, pt2.getColor());
		} else if (layer.optString("render_style").startsWith("porterduff")) {
			String sType = layer.optString("render_style").substring(11);
			if (sType.equals("XOR"))
				pt1.setXfermode(OMC.PORTERDUFF_XOR);
			else if (sType.equals("SRC_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_ATOP);
			else if (sType.equals("DST_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_ATOP);
			else if (sType.equals("SRC_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_IN);
			else if (sType.equals("DST_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_IN);
			else if (sType.equals("SRC_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OUT);
			else if (sType.equals("DST_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OUT);
			else if (sType.equals("MULTIPLY"))
				pt1.setXfermode(OMC.PORTERDUFF_MULTIPLY);
			else if (sType.equals("SRC_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OVER);
			else if (sType.equals("DST_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OVER);
		}
		//Either way, draw the proper panel
		cvas.drawRoundRect(tempFGRect, layer.optInt("xcorner")* OMC.fFinalScaling, layer.optInt("ycorner")* OMC.fFinalScaling, pt1);
		OMC.returnPaint(pt1);
		OMC.returnPaint(pt2);
	}

	// Static arc.
	static void drawArcLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {


		final Paint pt1 = OMC.getPaint();

		final RectF tempFGRect = new RectF();
		final RectF tempBGRect = new RectF();
		
		try {
			pt1.setAntiAlias(true);
			pt1.setStyle(Paint.Style.STROKE);
			pt1.setStrokeCap(Paint.Cap.BUTT);
			final float fInnerRadius = (float)layer.optDouble("inner_radius", 0);
			final float fStrokeWidth = Math.abs(((float)layer.getDouble("radius"))-fInnerRadius);
			pt1.setStrokeWidth(OMC.fFinalScaling * fStrokeWidth);
			tempFGRect.left = OMC.fFinalScaling * ((float)layer.getDouble("x")-fInnerRadius-fStrokeWidth/2f);
			tempFGRect.top = OMC.fFinalScaling * ((float)layer.getDouble("y")-fInnerRadius-fStrokeWidth/2f);
			tempFGRect.right = OMC.fFinalScaling * ((float)layer.getDouble("x")+fInnerRadius+fStrokeWidth/2f);
			tempFGRect.bottom = OMC.fFinalScaling * ((float)layer.getDouble("y")+fInnerRadius+fStrokeWidth/2f);
		} catch (JSONException e) {
			Log.w(OMC.OMCSHORT + "Engine", " (arc) is missing left/top/right/bottom values!  Giving up.");
			if (OMC.DEBUG) e.printStackTrace();
			return;
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
		final Paint pt2 = OMC.getPaint();

		try {
			pt2.setAntiAlias(true);
			pt2.setStyle(Paint.Style.STROKE);
			pt2.setStrokeCap(Paint.Cap.BUTT);
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
		float fStartAngle = (float)layer.optDouble("cw_rotate",0d)+layer.optInt("cw_start_angle");
		float fSweepAngle = layer.optInt("cw_end_angle")-layer.optInt("cw_start_angle");
		if (fSweepAngle>360)fSweepAngle = fSweepAngle % 360f;
		//Draw the SFX
		if (layer.optString("render_style").equals("emboss")) {
			tempBGRect.left = tempFGRect.left-OMC.fFinalScaling;
			tempBGRect.top = tempFGRect.top-OMC.fFinalScaling;
			tempBGRect.right = tempFGRect.right-OMC.fFinalScaling;
			tempBGRect.bottom = tempFGRect.bottom-OMC.fFinalScaling;
			cvas.drawArc(tempBGRect, fStartAngle, fSweepAngle, true, pt2);
			tempBGRect.left+=2*OMC.fFinalScaling;
			tempBGRect.top+=2*OMC.fFinalScaling;
			tempBGRect.right+=2*OMC.fFinalScaling;
			tempBGRect.bottom+=2*OMC.fFinalScaling;
			cvas.drawArc(tempBGRect, fStartAngle, fSweepAngle, true, pt2);
		} else if (layer.optString("render_style").equals("shadow")) {
			tempBGRect.left = tempFGRect.left+3*OMC.fFinalScaling;
			tempBGRect.top = tempFGRect.top+3*OMC.fFinalScaling;
			tempBGRect.right = tempFGRect.right+3*OMC.fFinalScaling;
			tempBGRect.bottom = tempFGRect.bottom+3*OMC.fFinalScaling;
			cvas.drawArc(tempBGRect, fStartAngle, fSweepAngle, true, pt2);
		} else if (layer.optString("render_style").startsWith("shadow")) {
			int iShadowWidth = (int)(Float.parseFloat(layer.optString("render_style").substring(7))*OMC.fFinalScaling);
			tempBGRect.left = tempFGRect.left+iShadowWidth;
			tempBGRect.top = tempFGRect.top+iShadowWidth;
			tempBGRect.right = tempFGRect.right+iShadowWidth;
			tempBGRect.bottom = tempFGRect.bottom+iShadowWidth;
			cvas.drawArc(tempBGRect, fStartAngle, fSweepAngle, true, pt2);
		} else if (layer.optString("render_style").startsWith("glow")) {
			pt1.setShadowLayer(Float.parseFloat(layer.optString("render_style").substring(5))*OMC.fFinalScaling, 0f, 0f, pt2.getColor());
		} else if (layer.optString("render_style").startsWith("porterduff")) {
			String sType = layer.optString("render_style").substring(11);
			if (sType.equals("XOR"))
				pt1.setXfermode(OMC.PORTERDUFF_XOR);
			else if (sType.equals("SRC_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_ATOP);
			else if (sType.equals("DST_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_ATOP);
			else if (sType.equals("SRC_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_IN);
			else if (sType.equals("DST_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_IN);
			else if (sType.equals("SRC_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OUT);
			else if (sType.equals("DST_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OUT);
			else if (sType.equals("SRC_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OVER);
			else if (sType.equals("DST_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OVER);
		}
		//Either way, draw the proper panel
		cvas.drawArc(tempFGRect, fStartAngle, fSweepAngle, false, pt1);
		OMC.returnPaint(pt1);
		OMC.returnPaint(pt2);
	}

	//Bitmap layer.
	static void drawBitmapLayer(final Context context, final JSONObject layer, final String sTheme, final int aWI, final Canvas cvas) {
		final Paint pt1 = OMC.getPaint();
		pt1.reset();
		pt1.setAntiAlias(true);
		pt1.setFlags(Paint.FILTER_BITMAP_FLAG);
		if (layer.has("tint")) {
			pt1.setColor(Color.parseColor(layer.optString("tint")));
		}
		if (layer.has("render_style")) {
			if (layer.optString("render_style").startsWith("porterduff")) {
					String sType = layer.optString("render_style").substring(11);
					if (sType.equals("XOR"))
						pt1.setXfermode(OMC.PORTERDUFF_XOR);
					else if (sType.equals("SRC_ATOP"))
						pt1.setXfermode(OMC.PORTERDUFF_SRC_ATOP);
					else if (sType.equals("DST_ATOP"))
						pt1.setXfermode(OMC.PORTERDUFF_DST_ATOP);
					else if (sType.equals("SRC_IN"))
						pt1.setXfermode(OMC.PORTERDUFF_SRC_IN);
					else if (sType.equals("DST_IN"))
						pt1.setXfermode(OMC.PORTERDUFF_DST_IN);
					else if (sType.equals("SRC_OUT"))
						pt1.setXfermode(OMC.PORTERDUFF_SRC_OUT);
					else if (sType.equals("DST_OUT"))
						pt1.setXfermode(OMC.PORTERDUFF_DST_OUT);
					else if (sType.equals("SRC_OVER"))
						pt1.setXfermode(OMC.PORTERDUFF_SRC_OVER);
					else if (sType.equals("DST_OVER"))
						pt1.setXfermode(OMC.PORTERDUFF_DST_OVER);
			}
		}

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
		tempMatrix.postScale((float)layer.optDouble("horizontal_stretch")*OMC.fFinalScaling,(float)layer.optDouble("vertical_stretch")*OMC.fFinalScaling);
		tempMatrix.postRotate((float)layer.optDouble("cw_rotate"));

		tempMatrix.postTranslate((tempBitmap.getWidth()*OMC.fFinalScaling*(float)layer.optDouble("horizontal_stretch"))/2f
				+ (int)(layer.optInt("x")*OMC.fFinalScaling), 
				(tempBitmap.getHeight()*OMC.fFinalScaling*(float)layer.optDouble("vertical_stretch"))/2f
				+ (int)(layer.optInt("y")*OMC.fFinalScaling));

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
		pt1.setTextSize(layer.optInt("text_size")*OMC.fFinalScaling);
		pt1.setTextSkewX((float)layer.optDouble("text_skew"));
		String sTemp = layer.optString("text_stretch");
		if (sTemp==null) {
			pt1.setTextScaleX(1f);
		} else {
			int iTemp;
			if (sTemp.startsWith("f")) {
				pt1.setTextScaleX(1f);
				float fFactor = Float.parseFloat(sTemp.substring(1))*OMC.fFinalScaling/OMCWidgetDrawEngine.getSpannedStringWidth(new SpannedString(Html.fromHtml(text)),pt1);
				pt1.setTextScaleX(fFactor);
			} else if ((iTemp = sTemp.indexOf("m"))!= -1) {
				pt1.setTextScaleX(Float.parseFloat(sTemp.substring(0,iTemp)));
				int iMax = (int)(Integer.parseInt(sTemp.substring(iTemp+1))*OMC.fFinalScaling);
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
    			(int)(layer.optInt("x")*OMC.fFinalScaling),
    			(int)(layer.optInt("y")*OMC.fFinalScaling),
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
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		} else if (style.equals("shadow")) {
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+3, y+3, pt2, fRot);
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		} else if (style.startsWith("shadow")) {
			int iShadowWidth = Integer.parseInt(style.substring(7));
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x+iShadowWidth, y+iShadowWidth, pt2, fRot);
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		} else if (style.startsWith("glow")) {
			pt1.setShadowLayer(Float.parseFloat(style.substring(5)), 0f, 0f, pt2.getColor());
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		} else if (style.startsWith("porterduff")) {
			String sType = style.substring(11);
			if (sType.equals("XOR"))
				pt1.setXfermode(OMC.PORTERDUFF_XOR);
			else if (sType.equals("SRC_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_ATOP);
			else if (sType.equals("DST_ATOP"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_ATOP);
			else if (sType.equals("SRC_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_IN);
			else if (sType.equals("DST_IN"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_IN);
			else if (sType.equals("SRC_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OUT);
			else if (sType.equals("DST_OUT"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OUT);
			else if (sType.equals("SRC_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_SRC_OVER);
			else if (sType.equals("DST_OVER"))
				pt1.setXfermode(OMC.PORTERDUFF_DST_OVER);
				OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		} else if (style.startsWith("normal")) {
			OMCWidgetDrawEngine.fancyDrawSpanned(cvas, text, x, y, pt1, fRot);
		}
	}



}