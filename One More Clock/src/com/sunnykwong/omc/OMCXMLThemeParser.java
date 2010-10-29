package com.sunnykwong.omc;

import android.util.Log;
import android.widget.Toast;
import android.text.format.Time;
import android.graphics.Bitmap;
import android.graphics.Typeface;

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
	public Stack<String[]> tree;
	public StringBuilder  sb;
	public OMCImportedTheme newTheme;
	static public boolean valid;
	static public String latestThemeName;
	
	public OMCXMLThemeParser (String nm) {
		super();
		OMCXMLThemeParser.valid=false;
		newTheme = new OMCImportedTheme();
		if (tree == null) tree = new Stack<String[]>();
		tree.clear();
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
		tree.push(new String[] {localName, atts.getValue("name")});
		
//		If this is an array element, then start building the hashmap
		if (localName.equals("array") || localName.equals("string-array")) {
			newTheme.arrays.put(atts.getValue("name"), new ArrayList<String>(3));
		}

	}
	@Override
	public void endElement (String uri, String name, String qName) {

//		OK, so an element ended.
//		If it's an item subtag, then add to the end of the array.
		if (tree.peek()[0].equals("item")) {
			//	Pop the stack.
			tree.pop();
			newTheme.arrays.get(tree.peek()[1]).add(sb.toString());
			sb.setLength(0);
		}
//		If it's a results tag, post-process the data.
		if (tree.peek()[0].equals("resources")) {
			if (OMC.DEBUG) {
				Log.i("ELEMENT", "---BEGINELEMENT---");
				Iterator<Map.Entry<String, ArrayList<String>>> i = newTheme.arrays.entrySet().iterator();
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
		newTheme.name = null;
		
		//Assume valid until proven otherwise.
		newTheme.valid = true;
		
		// First, if theme does not have a name or desc, not valid
		if (!newTheme.arrays.containsKey("theme_options")) newTheme.valid=false;
		if (!newTheme.arrays.containsKey("theme_values")) {
			newTheme.valid=false;
		} else {
			newTheme.name = newTheme.arrays.get("theme_values").get(0);
		}
		
		// If the theme does not have layer specification, not valid
		if (!newTheme.arrays.containsKey(newTheme.arrays.get("theme_values").get(0))) newTheme.valid=false;
		
		// Check layer/talk references
		Iterator<String> k = newTheme.arrays.keySet().iterator();
		while (k.hasNext()){
			String sKey = k.next();
			//If any of the layers do not exist in the same control file, not valid
			if (sKey.equals(newTheme.name)){
				for (Object oTemp:newTheme.arrays.get(sKey).toArray()) {
					String sTemp = (String)oTemp;
					System.out.println(sTemp.substring(6));
					if (!newTheme.arrays.containsKey(sTemp.substring(6))) {
						if (OMC.DEBUG) Log.i("OMCXML","layer invalid");
						newTheme.valid=false;
						break;
					}
					if (sTemp.startsWith("quote:")) {
						if (!newTheme.arrays.containsKey(newTheme.arrays.get(sTemp.substring(6)).get(13))) {
							if (OMC.DEBUG) Log.i("OMCXML","talkback invalid");
							newTheme.valid=false;
							break;
						}
					}

					// cache the bitmaps/fonts mentioned in the control file.  If any of these fail, 
					// Mark theme as invalid.
					if (sTemp.startsWith("text :")) {
						Typeface tf = OMC.getTypeface(newTheme.arrays.get(sTemp.substring(6)).get(2), newTheme.arrays.get(sTemp.substring(6)).get(3));
						if (tf==null) {
							if (OMC.DEBUG) Log.i("OMCXML","typeface "+ newTheme.arrays.get(sTemp.substring(6)).get(3) +" not found");
							newTheme.valid=false;
							break;
						} else {
							newTheme.typefaces.put(newTheme.arrays.get(sTemp.substring(6)).get(3), tf);
						}
					}
					if (sTemp.startsWith("image:")) {
						Bitmap bmp = OMC.getBitmap(newTheme.arrays.get(sTemp.substring(6)).get(2), newTheme.arrays.get(sTemp.substring(6)).get(3));
						if (bmp==null) {
							if (OMC.DEBUG) Log.i("OMCXML","image "+ newTheme.arrays.get(sTemp.substring(6)).get(3) +" not found");
							newTheme.valid=false;
							break;
						} else {
							newTheme.bitmaps.put(newTheme.arrays.get(sTemp.substring(6)).get(3), bmp);
						}
					}
				}
			}
		}

		
		
		if (newTheme.valid) {
			OMC.IMPORTEDTHEMEMAP.put(newTheme.name, newTheme);
			OMCXMLThemeParser.latestThemeName = newTheme.name;
		}

		OMCXMLThemeParser.valid = newTheme.valid;
		
		
		//OK we're done parsing the whole document.
    	//Since the parse() method is synchronous, we don't need to do anything - just basic cleanup.
    	tree.clear();
    	tree = null;
    	sb.setLength(0);
    	sb = null;
		newTheme=null;
		
    }
	
}
