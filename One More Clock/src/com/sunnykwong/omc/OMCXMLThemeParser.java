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

public class OMCXMLThemeParser extends DefaultHandler {
    static public final long serialVersionUID = 0l;
	public Stack<String[]> tree;
	public HashMap<String,ArrayList<String>> arrays;
	public HashMap<String,Bitmap> bitmaps;
	public StringBuilder  sb;
	public static final String DELIMITERS = " .,;-";
	public static String name;
	
	public OMCXMLThemeParser () {
		super();	
		OMCXMLThemeParser.name = OMCThemeImportActivity.CURRSELECTEDTHEME;
		if (tree == null) tree = new Stack<String[]>();
		tree.clear();
		if (arrays == null) arrays = new HashMap<String,ArrayList<String>>();
		arrays.clear();
		if (bitmaps == null) bitmaps = new HashMap<String,Bitmap>();
		bitmaps.clear();
		if (sb == null) sb = new StringBuilder();
		sb.setLength(0);
	}

	@Override
    public void startDocument () {
    	if (OMC.DEBUG) Log.i("OMCTheme","start document");
    }

	@Override
    public void characters(char[] ch, int start, int length) {

//		If the text is not of result, then keep building the hashmap
		if (tree.peek()[0].equals("item")) {
			sb=sb.append(ch, start, length);
		}

    }
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		System.out.println(localName);
		tree.push(new String[] {localName, atts.getValue("name")});
		
//		If this is an array element, then start building the hashmap
		if (localName.equals("array") || localName.equals("string-array")) {
			arrays.put(atts.getValue("name"), new ArrayList<String>(3));
		}

	}
	@Override
	public void endElement (String uri, String name, String qName) {

//		OK, so an element ended.
//		If it's an item subtag, then add to the end of the array.
		if (tree.peek()[0].equals("item")) {
			//	Pop the stack.
			tree.pop();
			arrays.get(tree.peek()[1]).add(sb.toString());
			sb.setLength(0);
		}
//		If it's a results tag, post-process the data.
		if (tree.peek()[0].equals("resources")) {
			if (OMC.DEBUG) {
				Log.i("ELEMENT", "---BEGINELEMENT---");
				Iterator<Map.Entry<String, ArrayList<String>>> i = arrays.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<String,ArrayList<String>> e = (Map.Entry<String, ArrayList<String>>) i.next();
					Log.i("ELEMENT", e.getKey() + "-->");
					Iterator<String> j = e.getValue().iterator();
					while (j.hasNext()) {
						Log.i("ELEMENT","   - " + j.next());
					}
					Log.i("ELEMENT", "<-- DONE -->");
					//	Pop the stack.
					tree.pop();
				}
			}
			
		}

		
    }

	public boolean isNumber (String s) {
        for (int i = 0; i < s.length(); i++) {

            //If we find a non-digit character we return false.
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
	}

	@Override
    public void endDocument ()
    {
		System.out.println("Doc done.");
		if (OMC.DEBUG) {
			Log.i("ELEMENT", "---BEGINELEMENT---");
			Iterator<Map.Entry<String, ArrayList<String>>> i = arrays.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<String,ArrayList<String>> e = (Map.Entry<String, ArrayList<String>>) i.next();
				Log.i("ELEMENT", e.getKey() + "-->");
				Iterator<String> j = e.getValue().iterator();
				while (j.hasNext()) {
					Log.i("ELEMENT","   - " + j.next());
				}
				Log.i("ELEMENT", "<-- DONE -->");
				//	Pop the stack.
				tree.pop();
			}
		}
    	//OK we're done parsing the whole document.
    	//Since the parse() method is synchronous, we don't need to do anything - just basic cleanup.
    	tree.clear();
    	tree = null;
    	sb.setLength(0);
    	sb = null;
    }
	
}
