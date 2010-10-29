package com.sunnykwong.omc;

import android.graphics.Bitmap;
import android.graphics.Typeface;

import java.util.HashMap;
import java.util.ArrayList;

public class OMCImportedTheme implements java.io.Serializable {
    static public final long serialVersionUID = 0l;
	public HashMap<String,ArrayList<String>> arrays;
	public HashMap<String,Bitmap> bitmaps;
	public HashMap<String,Typeface> typefaces;
	public String name;
	public boolean valid;
	
	public OMCImportedTheme () {
		valid = false;
		name = null;
		arrays = new HashMap<String,ArrayList<String>>();
		bitmaps = new HashMap<String,Bitmap>();
		typefaces = new HashMap<String,Typeface>();
	}
	
}
