package com.sunnykwong.omc;

import android.util.Log;
import android.text.format.Time;
import android.graphics.Bitmap;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

import java.util.Map;
import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.ArrayList;

import java.lang.StringBuilder;

public class OMCImportedTheme implements java.io.Serializable {
    static public final long serialVersionUID = 0l;
	public HashMap<String,ArrayList<String>> arrays;
	public HashMap<String,Bitmap> bitmaps;
	public String name;
	public boolean valid;
	
	public OMCImportedTheme () {
		valid = false;
		name = null;
		arrays = new HashMap<String,ArrayList<String>>();
		bitmaps = new HashMap<String,Bitmap>();
	}
	
}
