package com.sunnykwong.FlingWords;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.text.SpannedString;
import android.text.style.StyleSpan;
import android.widget.Toast;

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
	static JSONArray VOCABINDEX;
	static JSONObject VOCABBATCHES;
	static JSONObject CURRENTBATCH;
	static String PKGNAME;
	static boolean VIEWRETIREDBATCHES=false;
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
    	SCRNHEIGHT = getResources().getDisplayMetrics().heightPixels;
    	SCRNWIDTH = getResources().getDisplayMetrics().widthPixels;
    	SCRNLONGEREDGELENGTH = Math.max(SCRNHEIGHT, SCRNWIDTH);
    	SCRNSHORTEREDGELENGTH = Math.min(SCRNHEIGHT, SCRNWIDTH);
		TEXTSIZE = SCRNSHORTEREDGELENGTH/3;
		TEXTCOLOR = Color.RED;

		PKGNAME=getPackageName();
		AM = getAssets();
		FONT = Typeface.createFromAsset(FW.AM, "Roboto-Bold.ttf");
		PT = new Paint();
		PT.setTextSize(TEXTSIZE);
		PT.setTextScaleX(1f);
		PT.setColor(TEXTCOLOR);
		PT.setTypeface(FONT);
		loadVocabFromJSON();
	}
	
	public void loadVocabFromJSON() {
		try {
			File f = new File("/mnt/sdcard/vocabulary.json");
			// Look in SD path
			if (f.exists()) {
				Toast.makeText(this, "vocabulary.json file found on SD card.  Importing...", Toast.LENGTH_LONG).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    VOCABBATCHES = new JSONObject(sb.toString());
			} else {
				// Look in assets
				InputStreamReader in = new InputStreamReader(this.getAssets().open("vocabulary.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    VOCABBATCHES = new JSONObject(sb.toString());
			}
			VOCABINDEX = VOCABBATCHES.getJSONArray("00index");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.gc();
		}
	}

	static int[] generateRandomSequence(int size) {
		int[] result = new int[size];
		for (int i=0;i<size;i++) {
			result[i]=i;
		}
		for (int i=0;i<size;i++) {
			int randompos = (int)(Math.random()*size);
			int temp = result[i];
			result[i]=result[randompos];
			result[randompos]=temp;
		}
		return result;
	}

	static float getStringWidth(String ss) {
			float result = PT.measureText(ss);
		return result*1.8f;
	}

}
