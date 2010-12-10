package com.sunnykwong.omc;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.Typeface;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.*;

import java.util.Map;
import java.util.Stack;
import java.util.Iterator;
import java.util.ArrayList;

import java.io.FileReader;
import java.lang.StringBuilder;

public class OMCXMLThemeParser extends DefaultHandler {
	public Stack<String[]> tree;
	public StringBuilder  sb;
	public OMCImportedTheme newTheme;
	public boolean doneParsing;
	public boolean valid;
	static public String sdRootPath;
	
	public OMCXMLThemeParser (String nm) {
		super();
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 

        OMCXMLThemeParser.sdRootPath=nm;
		valid=false;
		newTheme = new OMCImportedTheme();
		if (tree == null) tree = new Stack<String[]>();
		tree.clear();
		if (sb == null) sb = new StringBuilder();
		sb.setLength(0);
	}

	public void importTheme() {
        
    	Thread t = new Thread () {
    		public void run() {
            	try {
            		doneParsing=false;
            		// Setup XML Parsing...
            		XMLReader xr = XMLReaderFactory.createXMLReader();
            		xr.setContentHandler(OMCXMLThemeParser.this);
            		// Feed data from control file to XML Parser.
            		FileReader fr = new FileReader(OMCXMLThemeParser.sdRootPath + "/00control.xml");
            		xr.setErrorHandler(OMCXMLThemeParser.this);
            		
            		xr.parse(new InputSource(fr));

                	fr.close();

            	} catch (Exception e) {
            		
                	e.printStackTrace();
            	}

            	// This call will end up passing control to processXMLResults
        		doneParsing=true;
    		}      	   
    	};
		t.start();

    } 
	
	@Override
    public void startDocument () {
//		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "TParser","Start document.");
    }

	@Override
    public void characters(char[] ch, int start, int length) {

//		If the text is not of result, then keep building the hashmap
		String sTemp = tree.peek()[0].toLowerCase();
		if (sTemp.equals("item") || sTemp.equals("b") || sTemp.equals("i") || sTemp.equals("u")) {
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
		if (localName.toLowerCase().equals("b") || localName.toLowerCase().equals("i") || localName.toLowerCase().equals("u")) {
			sb=sb.append("<" + localName + ">");
		}

	}
	@Override
	public void endElement (String uri, String name, String qName) {

//		OK, so an element ended.
		String sTemp = tree.peek()[0];
//		If it's just a formatting tag, then append the endtag and pop the stack.
		if (name.toLowerCase().equals("b") || name.toLowerCase().equals("i") || name.toLowerCase().equals("u")) {
			tree.pop();
			sb=sb.append("</" + sTemp + ">");
		}
//		If it's an item subtag, then add to the end of the array.
		if (sTemp.equals("item")) {
			//	Pop the stack.
			tree.pop();
			newTheme.arrays.get(tree.peek()[1]).add(sb.toString());
			sb.setLength(0);
		}
//		If it's a results tag, post-process the data.
		if (sTemp.equals("resources")) {
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
//		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "TParser","Doc done.");
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
		
		// If theme valid so far, Check layer/talk references
		if (newTheme.valid) {
			Iterator<String> k = newTheme.arrays.keySet().iterator();
			while (k.hasNext()){
				String sKey = k.next();
				//If any of the layers do not exist in the same control file, not valid
				if (sKey.equals(newTheme.name)){
					for (Object oTemp:newTheme.arrays.get(sKey).toArray()) {
						String sTemp = (String)oTemp;
//						if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "TParser",sTemp.substring(6));
						if (!newTheme.arrays.containsKey(sTemp.substring(6))) {
							if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "XML","layer invalid");
							newTheme.valid=false;
							break;
						}
						if (sTemp.startsWith("quote:")) {
							if (!newTheme.arrays.containsKey(newTheme.arrays.get(sTemp.substring(6)).get(13))) {
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "XML","quote talkback invalid");
								newTheme.valid=false;
								break;
							}
							Typeface tf = OMC.getTypeface(newTheme.arrays.get(sTemp.substring(6)).get(1), OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2));
							if (tf==null) {
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "XML","quote typeface "+ newTheme.arrays.get(sTemp.substring(6)).get(2) +" not found");
								newTheme.valid=false;
								break;
							} else {
								OMC.copyFile(OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2), OMC.CACHEPATH + newTheme.name + newTheme.arrays.get(sTemp.substring(6)).get(2));
							}
						}
	
						// cache the bitmaps/fonts mentioned in the control file.  If any of these fail, 
						// Mark theme as invalid.
						if (sTemp.startsWith("text :")) {
							Typeface tf = OMC.getTypeface(newTheme.arrays.get(sTemp.substring(6)).get(1), OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2));
							if (tf==null) {
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "XML","typeface "+ newTheme.arrays.get(sTemp.substring(6)).get(2) +" not found");
								newTheme.valid=false;
								break;
							} else {
								OMC.copyFile(OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2), OMC.CACHEPATH + newTheme.name + newTheme.arrays.get(sTemp.substring(6)).get(2));
							}
						}
						if (sTemp.startsWith("image:")) {
							Bitmap bmp = OMC.getBitmap(newTheme.arrays.get(sTemp.substring(6)).get(1), OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2));
							if (bmp==null) {
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "XML","image "+ newTheme.arrays.get(sTemp.substring(6)).get(2) +" not found");
								newTheme.valid=false;
								break;
							} else {
								OMC.copyFile(OMCXMLThemeParser.sdRootPath + "/" + newTheme.arrays.get(sTemp.substring(6)).get(2), OMC.CACHEPATH + newTheme.name + newTheme.arrays.get(sTemp.substring(6)).get(2));
							}
						}
					}
				}
			}
		}		
		
		if (newTheme.valid) {
			OMC.IMPORTEDTHEMEMAP.put(newTheme.name, newTheme);
		}

		valid = newTheme.valid;
		
		//OK we're done parsing the whole document.
    	//Since the parse() method is synchronous, we don't need to do anything - just basic cleanup.
    	tree.clear();
    	tree = null;
    	sb.setLength(0);
    	sb = null;
		newTheme=null;
		
    }
	
}
