package com.sunnykwong.FlingWords;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.text.SpannedString;
import android.text.style.StyleSpan;

public class FW extends Application {
	static MediaPlayer BEEPER;
	static String THISVERSION;
	static final String PREFNAME = "com.sunnykwong.FlingWords";
	static final boolean DEBUG = false;
	static Typeface FONT;
	static AssetManager AM;
	static Paint PT;
	static float TEXTSIZE;
	static int SCRNWIDTH, SCRNHEIGHT, SCRNLONGEREDGELENGTH, SCRNSHORTEREDGELENGTH;
	static int TEXTCOLOR;

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
    	SCRNHEIGHT = getResources().getDisplayMetrics().heightPixels;
    	SCRNWIDTH = getResources().getDisplayMetrics().widthPixels;
    	SCRNLONGEREDGELENGTH = Math.max(SCRNHEIGHT, SCRNWIDTH);
    	SCRNSHORTEREDGELENGTH = Math.min(SCRNHEIGHT, SCRNWIDTH);
		TEXTSIZE = SCRNSHORTEREDGELENGTH/4;
		TEXTCOLOR = Color.RED;

		AM = getAssets();
		FONT = Typeface.createFromAsset(FW.AM, "Roboto-Bold.ttf");
		PT = new Paint();
		PT.setTextSize(TEXTSIZE);
		PT.setTextScaleX(1f);
		PT.setColor(TEXTCOLOR);
		PT.setTypeface(FONT);
	}
	
	static float getStringWidth(String ss) {
			float result = PT.measureText(ss);
		return result*1.8f;
	}

}
