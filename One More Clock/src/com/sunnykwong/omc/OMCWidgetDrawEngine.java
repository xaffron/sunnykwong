package com.sunnykwong.omc;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.content.ComponentName;
import android.graphics.Typeface;

public class OMCWidgetDrawEngine {
	// This is where the theme-specific tweaks (regardless of layer) are processed.
	// Tweaks = hacks, but at least all the hacks are in one block of code.
	static void layerThemeTweaks(final Context context, final int iLayerID, final String sTheme, final int aWI) {
		switch (iLayerID) {
		case R.array.BBClock:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
    		}
    		int iShade = (int)(255 - Math.abs((OMC.TIME.hour*60 + OMC.TIME.minute) - (12*60))/720f * 255);
    		OMC.PT2.setARGB(255, iShade, iShade, iShade);
    		break;
		case R.array.BaBClock:
		case R.array.CCClock:
		case R.array.CEClock:
		case R.array.DDClock:
		case R.array.LLClock:
		case R.array.MMClock:
		case R.array.TTClock:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
    		}
    		break;
		case R.array.NNClock1:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H").substring(0,1) : OMC.TIME.format("%k").substring(0,1);
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I").substring(0,1) : OMC.TIME.format("%l").substring(0,1);
    		}
    		break;
		case R.array.NNClock2:
    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H").substring(1,2) : OMC.TIME.format("%k").substring(1,2);
    		} else {
    			OMC.TXTBUF = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I").substring(1,2) : OMC.TIME.format("%l").substring(1,2);
    		}
    		break;
		case R.array.NNClock3:
			OMC.TXTBUF = OMC.TIME.format("%M").substring(0,1);
    		break;
		case R.array.NNClock4:
			OMC.TXTBUF = OMC.TIME.format("%M").substring(1,2);
    		break;
		case R.array.DiaryClock:
			OMC.TALKBACKS = context.getResources().getStringArray(R.array.WordNumbers);
			if (OMC.TIME.minute == 0) {
				OMC.TXTBUF = OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))] + " o'Clock.";
			} else if (OMC.TIME.minute == 30) {
				OMC.TXTBUF = "** half past " + OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))] + "."; 
			} else if (OMC.TIME.minute == 15) {
				OMC.TXTBUF = "A Quarter past " + OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))] + "."; 
			} else if (OMC.TIME.minute == 45) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					OMC.TXTBUF = "A Quarter to Twelve.";
				} else {
					OMC.TXTBUF = "A Quarter to " 
					+ OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))+1] + ".";
				}
			} else if (OMC.TIME.minute > 30) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					OMC.TXTBUF = OMC.TALKBACKS[60-Integer.parseInt(OMC.TIME.format("%M"))] + " to Twelve.";
				} else {
					OMC.TXTBUF = OMC.TALKBACKS[60-Integer.parseInt(OMC.TIME.format("%M"))] + " to " 
					+ OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))+1] + ".";
				}
			} else {
				OMC.TXTBUF = OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%M"))] + " past " 
				+ OMC.TALKBACKS[Integer.parseInt(OMC.TIME.format("%I"))] + ".";
			}
    		break;
		case R.array.BBDate:
			OMC.TXTBUF = OMC.TIME.format("%a %e %b");
    		iShade = (int)(255 - Math.abs((OMC.TIME.hour*60 + OMC.TIME.minute) - (12*60))/720f * 255);
    		OMC.PT2.setARGB(255, iShade, iShade, iShade);
    		break;
		case R.array.BaBDate:
		case R.array.CCDate:
		case R.array.DDDate:
		case R.array.LLDate:
		case R.array.TTDate:
			OMC.TXTBUF = OMC.TIME.format("%A, %B %e");
    		break;
		case R.array.NNDate:
		case R.array.DiaryDate:
			OMC.TALKBACKS = context.getResources().getStringArray(R.array.WordNumbers);
			OMC.TXTBUF = OMC.TIME.format("*%e %B, %G. *");
    		break;
		case R.array.CEDate:
			OMC.TXTBUF = OMC.TIME.format("%Y . %m . %d");
			break;
		case R.array.CEEclipse:
    		iShade = (int)(14f + (((OMC.TIME.hour+6)*60f + OMC.TIME.minute) % (12*60f))/(12*60f) * 150f);
    		OMC.TEMPMATRIX.reset();
    		if (OMC.LAYERATTRIBS.getBoolean(3, true)) OMC.TEMPMATRIX.postScale(OMC.LAYERATTRIBS.getFloat(4, 1f), OMC.LAYERATTRIBS.getFloat(5, 1f));
    		OMC.TEMPMATRIX.postTranslate(iShade, OMC.LAYERATTRIBS.getFloat(7, 0f));
    		OMC.TXTBUF = "";
			break;
		case R.array.BBFlare:
    		break;
		case R.array.CCChinese:
			OMC.TXTBUF = String.valueOf(OMC.CHINESETIME.charAt((OMC.TIME.hour + 1)/2));

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
    		OMC.CANVAS.drawCircle(24f+3, 130f+3, 15, OMC.PT2);
    		OMC.CANVAS.drawCircle(24f, 130f, 15, OMC.PT1);
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
		case R.array.MMSplatter:
			OMC.TXTBUF = "abi";
			break;
		case R.array.MMDate:
			OMC.TXTBUF = OMC.TIME.format("%A.");
			break;
		case R.array.BaBCanvas:
    		float ratio = 1f - Math.abs((OMC.TIME.hour*60 + OMC.TIME.minute) - (12*60) )/(12*60f);
			OMC.PT1.setARGB(255, (int)(200*ratio), (int)(230*ratio), (int)(255*ratio));
			break;
		case R.array.BaBSun:
			if (OMC.TIME.hour >= 6 && OMC.TIME.hour < 18) {
				OMC.TXTBUF="M";
			} else {
				OMC.TXTBUF="E";
				OMC.PT1.setARGB(255, 255, 255, 0);
			}
    		iShade = (int)(70 + (((OMC.TIME.hour+6)*60 + OMC.TIME.minute) % (12*60))/(12*60f) * 180);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
			break;
		case R.array.BaBCloud1:
			OMC.TXTBUF = "m";
    		iShade = (int)(70 + ((OMC.TIME.hour*60 + OMC.TIME.minute) % (3*60))/(3*60f) * 180);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
    		break;
		case R.array.BaBCloud2:
			OMC.TXTBUF = "m";
    		iShade = (int)(40 + ((OMC.TIME.hour*60 + OMC.TIME.minute) % (4*60))/(4*60f) * 240);
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
			break;
		case R.array.BaBDuck:
			OMC.TXTBUF = OMC.TIME.format("U");
    		iShade = (int)(70 + 180 - ((OMC.TIME.hour*60 + OMC.TIME.minute) % (6*60))/(6*60f) * 180) ;
    		fancyDrawText(OMC.LAYERATTRIBS.getString(7), OMC.CANVAS, OMC.TXTBUF, iShade, OMC.LAYERATTRIBS.getInt(11, 1), OMC.PT1, OMC.PT2);
    		OMC.TXTBUF = "";
    		break;
		case R.array.BaBDucklings:
			OMC.TXTBUF = OMC.TIME.format("U U");
    		iShade = (int)(50 + 220 - OMC.TIME.minute%60 / 60f * 220) ;
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

		//Depending on time of day, enpoints.
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

	//Bitmap layer.  This is really only for emergencies (and for skinning).
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
		boolean bExternal = OMC.PREFS.getBoolean("external"+aWI,false);
		if (bExternal) {
			OMCImportedTheme oTheme = OMC.IMPORTEDTHEMEMAP.get(sTheme);
			ArrayList<String> tempAL = oTheme.arrays.get(OMC.LAYERATTRIBS.getString(13));
			OMC.TALKBACKS = tempAL.toArray(new String[tempAL.size()]);
		} else {
			OMC.TALKBACKS = context.getResources().getStringArray(context.getResources().getIdentifier(OMC.LAYERATTRIBS.getString(13), "array", "com.sunnykwong.omc"));
		}
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
				OMC.LAYERATTRIBS = new OMCTypedArray(OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(layer.substring(6)));
				String sNowTime;
				if (sType.equals("text ")){
		    		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
		    			sNowTime = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%H:%M") : OMC.TIME.format("%k:%M");
		    		} else {
		    			sNowTime = OMC.PREFS.getBoolean("widgetLeadingZero", true)? OMC.TIME.format("%I:%M") : OMC.TIME.format("%l:%M");
		    		}
					OMC.TXTBUF = OMC.LAYERATTRIBS.getString(13).replaceAll("%TIME%", sNowTime);
					OMC.TXTBUF = OMC.TXTBUF.replaceAll("%DATE%", OMC.TIME.format("%a %e %b"));

				}
			} else {
				//		Set LAF based on prefs
				iLayerID = context.getResources().getIdentifier(layer.substring(6), "array", "com.sunnykwong.omc");
				OMC.LAYERATTRIBS = new OMCTypedArray(context.getResources().obtainTypedArray(iLayerID));
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

	// Needs to be synchronized now that we have three different widget types 
	// calling this same method creating a potential race condition
	static synchronized void updateAppWidget(Context context, float fScaleX, float fScaleY, ComponentName cName, int iCutTop, int iCutBottom) {
		if (!OMCService.RUNNING) {
			OMC.setServiceAlarm(System.currentTimeMillis() + 10000);
		}
		AppWidgetManager aWM = AppWidgetManager.getInstance(context);

		final int N = aWM.getAppWidgetIds(cName).length;

		for (int i=0; i<N; i++) {
			OMCWidgetDrawEngine.updateAppWidget(context, aWM, aWM.getAppWidgetIds(cName)[i],Bitmap.createBitmap(OMC.BUFFER));
		}
		System.gc();
	}
	
	static synchronized void updateAppWidget(final Context context,
			final AppWidgetManager appWidgetManager,
			final int appWidgetId, final Bitmap bmp) {
		if (OMC.DEBUG)Log.i("OMCWidget", "Redrawing widget" + appWidgetId + " @ " + OMC.TIME.format("%T"));

		drawBitmapForWidget(context,appWidgetId);

		// Blit the buffer over
		final RemoteViews rv = new RemoteViews(context.getPackageName(),R.layout.omcwidget);
        rv.setImageViewBitmap(R.id.omcIV,bmp);

        if (OMC.PREFS.getString("URI"+appWidgetId, "").equals("")) { 
        	Intent intent = new Intent("com.sunnykwong.omc.WIDGET_CONFIG");
        	intent.setData(Uri.parse("omc:"+appWidgetId));
        	PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.omcIV, pi);
        } else {
        	if (OMC.DEBUG) Log.i("OMCWidget","INTENT " + OMC.PREFS.getString("URI"+appWidgetId, "")) ;
        	try {
        	Intent intent = Intent.parseUri(OMC.PREFS.getString("URI"+appWidgetId, ""), 0);
        	PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
            rv.setOnClickPendingIntent(R.id.omcIV, pi);
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
        }
        

        appWidgetManager.updateAppWidget(appWidgetId, rv);
	}


}
