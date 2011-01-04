package com.sunnykwong.omc;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;

import java.util.Map;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.LogRecord;

import java.io.FileReader;
import java.lang.StringBuilder;

public class OMCJSONThemeParser  {
	public StringBuilder  sb;
	public JSONObject newTheme;
	public boolean doneParsing;
	public boolean valid;
	static public String controlPath;
	public Handler mHandler;
	final Runnable mDoneImporting = new Runnable() {
		public void run() {
			validateTheme();
		}
	};
	
	public OMCJSONThemeParser (String nm) {
		super();
		
		mHandler = new Handler();

        OMCJSONThemeParser.controlPath=nm;
		valid=false;

		if (sb == null) sb = new StringBuilder();
		sb.setLength(0);
	}

	public void importTheme() {
        
    	Thread t = new Thread () {
    		public void run() {
        		doneParsing=false;
        		// Setup JSON Parsing...

        		try { 
            		char[] cTemp = new char[100000];
        			FileReader fr = new FileReader(OMCJSONThemeParser.controlPath);
        			fr.read(cTemp);
        			newTheme = new JSONObject(String.valueOf(cTemp).trim());
        			fr.close();
        		} catch (Exception e) {
        			e.printStackTrace(); 
        		}

            	// This call will end up passing control to processJSONResults
        		doneParsing=true;
        		mHandler.post(mDoneImporting);
        		
    		}      	   
    	};
		t.start();

    } 
	
	public void validateTheme() {	
		this.valid = false;

		// Test theme for the required elements:
		// ID
		// Name
		// Layers
		
		try {
			newTheme.getString("id");
			newTheme.getString("name");
			newTheme.getJSONArray("layers_bottomtotop");
		} catch (JSONException e) {
			this.valid = false;
		}
		
		OMC.THEMEMAP.put(newTheme.optString("id"), newTheme);
		
		this.valid=true;

		newTheme=null;
		
    }
	
}
