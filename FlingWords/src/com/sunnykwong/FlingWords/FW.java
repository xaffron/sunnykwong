package com.sunnykwong.FlingWords;

import android.app.Application;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.media.MediaPlayer;

public class FW extends Application {
	static MediaPlayer BEEPER;
	static String THISVERSION;
	static final String PREFNAME = "com.sunnykwong.FlingWords";
	static final boolean DEBUG = false;
	static Typeface FONT;
	static AssetManager AM;
	
	public FW() {
		AM = this.getAssets();
		FONT = Typeface.createFromAsset(FW.AM, "Roboto-Bold.ttf");
	}
}
