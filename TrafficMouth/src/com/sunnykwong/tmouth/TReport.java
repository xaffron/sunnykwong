package com.sunnykwong.tmouth;

import android.util.Log;
import android.text.format.Time;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;

import java.util.Map;
import java.util.Stack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import java.lang.StringBuilder;

public class TReport extends DefaultHandler {

	public Stack<String[]> tree;
	public HashMap<String,String> element;
	public StringBuilder  sb;
	public static final String DELIMITERS = " .,;-";
	
	public TReport () {
		super();	
		if (tree == null) tree = new Stack<String[]>();
		tree.clear();
		if (element == null) element = new HashMap<String,String>();
		element.clear();
		if (sb == null) sb = new StringBuilder();
		sb.setLength(0);
	}

    public void startDocument () {
    	if (TMActivity.DEBUG) Log.i("TMouth","start document");
    }


    public void characters(char[] ch, int start, int length) {

//		If the text is not of result, then keep building the hashmap
		if (!tree.peek()[0].equals("Result") && element != null) {
			sb=sb.append(ch, start, length);
		}

    }
	
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) {
		tree.push(new String[] {localName});
		
//		If this is a Result element, then start building an incident hashmap
		if (localName.equals("Result")) {
			element = new HashMap<String,String>();
			element.put("type", atts.getValue("type"));
		}

	}

	public void endElement (String uri, String name, String qName) {

//		OK, so an element ended.
//		If it's a subtag, then add to the data element.
		if (!tree.peek()[0].equals("Result")) {
			element.put(tree.peek()[0],sb.toString());
			sb.setLength(0);
		}
//		If it's a results tag, post-process the data.
		if (tree.peek()[0].equals("Result")) {
			if (TMActivity.DEBUG) {
				Log.i("ELEMENT", "---BEGINELEMENT---");
				Iterator<Map.Entry<String, String>> i = element.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<String,String> e = (Map.Entry<String, String>) i.next();
					Log.i("ELEMENT", e.getKey() + "-->" + e.getValue());
				}
			}
			
			TMActivity.ELEMENTS.add(element);
			
			//Expand and clean up the report element.
			cleanup(element);
		}
//		Either way, pop the stack.
		tree.pop();
		
    }

	public void cleanup(HashMap<String,String> element) {
		StringTokenizer stk;
		StringBuilder sb;
		String s, sTemp, sSrc;
		long lPOSIXTime, lUTC;


		// Get the last updated date (crude conversion from POSIX to UTC)
		lPOSIXTime = Long.parseLong(element.get("UpdateDate"));
		lUTC = (long)(lPOSIXTime *1000); 
		Time t = new Time();
		t.set(lUTC);

		// Fine-tune the type.
		sSrc = element.get("Title");
		sTemp = sSrc.replaceFirst("Slow traffic,", "");
		if (!sTemp.equals(sSrc)) element.put("type", "traffic");
		else {
			sTemp = sSrc.replaceFirst("Accident,", "");
			if (!sTemp.equals(sSrc)) element.put("type", "accident");
			else {
				sTemp = sSrc.replaceFirst("Disabled vehicle,", "");
				if (!sTemp.equals(sSrc)) element.put("type", "disabled");
			}
		}
		sSrc = sTemp;
		
		stk = new StringTokenizer(sSrc,TReport.DELIMITERS);
		sb = new StringBuilder(sSrc.length());

		sb = sb.append("As of " + t.format("%l %M") + ", ");
		if (TMActivity.DEBUG) Log.i("TMouth",t.format("%D %T") + ": " + element.get("Title"));
		
		while (stk.hasMoreTokens()){
			s = stk.nextToken();
			sTemp="";
			//If it's a number...
			//Split it up into twos.
			if (isNumber(s)) {
				if (s.length()<=2) sTemp = s;
				else sTemp = s.substring(0,s.length()-2) + ' ' + s.substring(s.length()-2);
			} else {
				//Otherwise, check for substitutions.
				sTemp = s;
				for (int i=0; i < TMActivity.lookups.length; i++) {
					if (s.equals(TMActivity.lookups[i])) {
						sTemp = TMActivity.values[i];
						break;
					}
				}
			}
			//Finally, append the translated stuff to the Spoken element.
			sb=sb.append(" " + sTemp);
		}
		
		// Parse the Direction Tag
		sSrc = element.get("Direction");

		//If Direction tag is missing, then skip this entire chunk of code
		if (!sSrc.equals("N/A")) {
			stk = new StringTokenizer(sSrc,TReport.DELIMITERS);
			while (stk.hasMoreTokens()){
				s = stk.nextToken();
				sTemp = s;
				for (int i=0; i < TMActivity.lookups.length; i++) {
					if (s.equals(TMActivity.lookups[i])) {
						sTemp = TMActivity.values[i];
						break;
					}
				}
				//Finally, append the translated stuff to the Spoken element.
				sb=sb.append(" on the " + sTemp + " side");
			}
		}
		//The semicolon is added for pause.
		sb=sb.append(";");
		
		// Now, do the same for the description tag
		sSrc = element.get("Description");
		stk = new StringTokenizer(sSrc,TReport.DELIMITERS);
		
		while (stk.hasMoreTokens()){
			s = stk.nextToken();
			sTemp="";
			//If it's a number...
			//Split it up into twos.
			if (isNumber(s)) {
				if (s.length()<=2) sTemp = s;
				else sTemp = s.substring(0,s.length()-2) + ' ' + s.substring(s.length()-2);
			} else {
				//Otherwise, check for substitutions.
				sTemp = s;
				for (int i=0; i < TMActivity.lookups.length; i++) {
					if (s.equals(TMActivity.lookups[i])) {
						sTemp = TMActivity.values[i];
						break;
					}
				}
			}
			//Finally, append the translated stuff to the Spoken element.
			sb=sb.append(" " + sTemp);
		}

		// Now that we're done parsing relevant tags, 
		// put the Spoken element into the object.
		element.put("Spoken", sb.toString().toLowerCase());

	}

	public boolean isNumber (String s) {
        for (int i = 0; i < s.length(); i++) {

            //If we find a non-digit character we return false.
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
	}
	
    public void endDocument ()
    {
    	//OK we're done parsing the whole document.
    	//Since the parse() method is synchronous, we don't need to do anything - just basic cleanup.
    	tree.clear();
    	tree = null;
    	sb.setLength(0);
    	sb = null;
    }
	
}
