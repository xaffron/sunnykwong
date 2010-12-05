package com.sunnykwong.omc;

import java.util.StringTokenizer;
import android.graphics.Color;
//import android.util.Log;

import java.util.ArrayList;

public class OMCTypedArray  {
	String[] mImportedArray;

	public OMCTypedArray(String[] strArray, int aWI) {
		mImportedArray = OMCTypedArray.findTokens(strArray, aWI);
	} 
	public OMCTypedArray(ArrayList<String> AL, int aWI) {
		String[] tempArray = new String[AL.size()];
    	AL.toArray(tempArray);
    	mImportedArray = OMCTypedArray.findTokens(tempArray, aWI);
	}
	public boolean getBoolean(int index, boolean defValue) {
		return Boolean.parseBoolean(mImportedArray[index].toString());
	}
	public int getColor(int index, int defValue) {
		return Color.parseColor(mImportedArray[index].toString());
	}
	public String getString(int index) {
		return mImportedArray[index].toString();
	}
	public int getInt(int index, int defValue) {
		return Integer.parseInt(mImportedArray[index].toString().replace(" ", ""));
	}
	public float getFloat(int index, float defValue) {
		return Float.parseFloat(mImportedArray[index].toString().replace(" ", ""));
	}
	public void recycle() {
		mImportedArray = null;
	}
	
	static public String[] findTokens(String[] sArray, int aWI) {
		String[] result = new String[sArray.length];
		for (int i = 0; i < sArray.length; i++) {
			result[i] = OMCTypedArray.resolveTokens(sArray[i], aWI);
		}
		return result;
	}
	
	static public String resolveOneToken(String sRawString, int aWI) {
		boolean isDynamic = false;
		if (sRawString.contains("[%")) isDynamic = true;
		// Strip brackets.
		String sBuffer = sRawString.replace("[%", "").replace("%]", "");
		//by default, return strftime'd string.
		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
			sBuffer = (OMC.TIME.format(
				OMC.PREFS.getBoolean("widgetLeadingZero", true)? 
						sBuffer : sBuffer.replaceAll("%H", "%k")
				)
			);
		} else {
			sBuffer = (OMC.TIME.format(
				OMC.PREFS.getBoolean("widgetLeadingZero", true)? 
						sBuffer.replaceAll("%H", "%I") : sBuffer.replaceAll("%H", "%l")
				)
			);
		}

		// If it's unbracketed text, we're done
		if (!isDynamic) return sBuffer;
		
		// If it is actually a dynamic element, resolve it
		// By default, 
		String result = "";

		StringTokenizer st = new StringTokenizer(sBuffer, "_");

		//Get the first element (command).
		String sToken = st.nextToken();
		if (sToken.startsWith("shift")) {

			// value that changes linearly and repeats every X seconds from 6am.
			String sType = st.nextToken();
			int iIntervalSeconds =  Integer.parseInt(st.nextToken());
			int iGradientSeconds = 0;

			// Where are we in time?  intGradientSeconds starts the count from either 12am or 6am
			if (sToken.equals("shift12")) {
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + OMC.TIME.hour*3600;
			} else { 
				// (sToken.equals("shift6"))
				iGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + ((OMC.TIME.hour+18)%24)*3600;
			} 

			float gradient = (iGradientSeconds % iIntervalSeconds)/(float)iIntervalSeconds;
			
			if (sType.equals("number")) {
				result = String.valueOf(OMCTypedArray.numberInterpolate(
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				int color = OMCTypedArray.colorInterpolate(
						st.nextToken(), 
						st.nextToken(), 
						st.nextToken(), 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else {
				//Unknown - do nothing
			}
		} else if (sToken.equals("circle")) {
			// Specifies a point at angle/radius from point.
			int iOriginVal = Integer.parseInt(st.nextToken());
			String sType = st.nextToken();
			int iAngle = Integer.parseInt(st.nextToken());
			int iRadius =  Integer.parseInt(st.nextToken());

			if (sType.equals("cos")) {
				result = String.valueOf(iOriginVal + (int)(iRadius * Math.cos(iAngle*Math.PI/180d)));
			} else if (sType.equals("sin")) {
				result = String.valueOf(iOriginVal + (int)(iRadius * Math.sin(iAngle*Math.PI/180d)));
			} else {
				//Unknown - do nothing
			}
			
		} else if (sToken.equals("ap24")) {
			if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
				st.nextToken();
				st.nextToken();
				result = (st.nextToken());
			} else if (OMC.TIME.hour < 12) {
				result = (st.nextToken());
			} else {
				st.nextToken();
				result = (st.nextToken());
			}
		} else if (sToken.equals("array")) {

			result = OMC.loadStringArray(OMC.PREFS.getString("widgetTheme"+aWI,OMC.DEFAULTTHEME), aWI, st.nextToken())[Integer.parseInt(st.nextToken().replace(" ",""))];
			String sTemp = st.nextToken();
			if (result == null) result = "ERROR";
			if (sTemp.equals("lower")) result = result.toLowerCase();
			else if (sTemp.equals("upper")) result = result.toUpperCase();

		} else if (sToken.equals("flipformat")) {
			int iApply = Integer.parseInt(st.nextToken());
			String sType = st.nextToken("_ ");
			if (sType.equals("bold")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append("<B>"+st.nextToken()+"</B> ");
					else sb.append(st.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("case")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append(st.nextToken().toUpperCase()+" ");
					else sb.append(st.nextToken().toLowerCase()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			} else if (sType.equals("italics")) {
				StringBuilder sb = new StringBuilder();
				while (st.hasMoreTokens()) {
					if (iApply==1) sb.append("<I>"+st.nextToken()+"</I> ");
					else sb.append(st.nextToken()+" ");
					iApply*=-1;
				}
				result = sb.toString();
			}
		} else if (sToken.equals("stripspaces")){
			result = st.nextToken().replace(" ", "");
		} else if (sToken.equals("fit")){
			result = "f"+st.nextToken();
		} else if (sToken.equals("maxfit")){
			result = st.nextToken()+"m"+st.nextToken();
		} else if (sToken.equals("fullenglishtime")){
			// full english time
			String sType = st.nextToken();
			String sTemp = "";
			if (OMC.TIME.minute == 0) {
				sTemp = OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + " o'Clock.";
			} else if (OMC.TIME.minute == 30) {
				sTemp = "half past " + OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + "."; 
			} else if (OMC.TIME.minute == 15) {
				sTemp = "A Quarter past " + OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + "."; 
			} else if (OMC.TIME.minute == 45) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					sTemp = "A Quarter to Twelve.";
				} else {
					sTemp = "A Quarter to " 
					+ OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))+1] + ".";
				}
			} else if (OMC.TIME.minute > 30) {
				if (OMC.TIME.hour == 11 || OMC.TIME.hour == 23) {
					sTemp = OMC.WORDNUMBERS[60-Integer.parseInt(OMC.TIME.format("%M"))] + " to Twelve.";
				} else if (OMC.TIME.hour == 0) {
						sTemp = OMC.WORDNUMBERS[60-Integer.parseInt(OMC.TIME.format("%M"))] + " to One.";
				} else {
					sTemp = OMC.WORDNUMBERS[60-Integer.parseInt(OMC.TIME.format("%M"))] + " to " 
					+ OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))+1] + ".";
				}
			} else {
				sTemp = OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%M"))] + " past " 
				+ OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + ".";
			}
			if (sType.equals("diary")) result = (sTemp);
			else if (sType.equals("upper")) result = (sTemp.toUpperCase());
			else if (sType.equals("lower")) result = (sTemp.toLowerCase());
			else result = (sTemp);

		} else if (sToken.equals("digit")) { // must be color
			String sTemp = st.nextToken();

    		int iOffset = Integer.parseInt(st.nextToken());
    		result = (sTemp.substring(iOffset-1,iOffset));
			
			
		} else if (sToken.equals("day")){
			// value that switches between two fixed symbols - day (6a-6p) and night (6p-6a).
			if (OMC.TIME.hour >= 6 && OMC.TIME.hour < 18) {
				//Day
				result = (st.nextToken());
			} else {
				//Night - throw away the day token + the night indicator
				st.nextToken();
				st.nextToken();
				result = (st.nextToken());
			}
		} else if (sToken.equals("random")){
			// value that randomly jumps between two values.
			String sType = st.nextToken();
			float gradient = OMC.RND.nextFloat();
			if (sType.equals("number")) {
				result = String.valueOf(OMCTypedArray.numberInterpolate(
						Integer.parseInt(st.nextToken()), 
						Integer.parseInt(st.nextToken()), 
						gradient));
			} else if (sType.equals("color")) { // must be color
				int color = OMCTypedArray.colorInterpolate(
						st.nextToken(), 
						st.nextToken(), 
						gradient);
				result = String.valueOf("#" + Integer.toHexString(color));
			} else {
				//Unknown - do nothing
			}
			

		} else {
			//unrecognized macro - ignore
		}
		
		return result;
		
	}

	static public String resolveTokens(String sRawString, int aWI) {
//		if (OMC.DEBUG) Log.i("OMCArray","Parsing "+sRawString);
		StringBuilder result = new StringBuilder();

		// If token contains dynamic elements, recursively resolve them
		
		if (sRawString.contains("[%")) {
			int iCursor = 0;
			while (iCursor <= sRawString.length()) {
				//Cruise through the text before the first [% marker
				result.append(sRawString.substring(
						iCursor,sRawString.indexOf(
							"[%",iCursor)==-1? 
							sRawString.length() : 
							sRawString.indexOf("[%",iCursor)
					)
				);
				
				iCursor = sRawString.indexOf("[%",iCursor);

				int iMarker1, iMarker2;
				do {
				// Mark where the next [% and %] are.  Which is closer?
				iMarker1 = sRawString.indexOf("[%",iCursor+1);
				iMarker2 = sRawString.indexOf("%]",iCursor);

//				if (OMC.DEBUG) Log.i("OMCArray","Markers: " +iCursor+" " + iMarker1+ " " + iMarker2);
				// No markers found? bad parsing occurred.
				// exit gracefully (try to)
				if (iMarker1 == -1 && iMarker2 == -1) {
//					if (OMC.DEBUG) Log.i("OMCArray","Improper markup. Bailing...");
					break;
					
				} else if (iMarker1 == -1) {
					// No more start markers found, but we have an end marker.
					// Dive into this substring.
//					if (OMC.DEBUG) Log.i("OMCArray","Sending down(-1): " + sRawString.substring(iCursor, iMarker2+2));
					result.append(OMCTypedArray.resolveOneToken(sRawString.substring(iCursor, iMarker2+2), aWI));
					result.append(sRawString.substring(iMarker2+2));
//					if (OMC.DEBUG) Log.i("OMCArray","Now at: " + result.toString());
				} else if (iMarker1 < iMarker2) {
					//if [% is closer, keep going until we find the innermost [%
//					if (OMC.DEBUG) Log.i("OMCArray","Found nested. (1<2)");
					result.append(sRawString.substring(iCursor,iMarker1));
//					if (OMC.DEBUG) Log.i("OMCArray","Now at: " + result.toString());
					iCursor = iMarker1;
				} else if (iMarker2 < iMarker1) {
					//if %] is closer, we have an end marker.
					//Dive into this substring.
//					if (OMC.DEBUG) Log.i("OMCArray","Sending down(2<1): " + sRawString.substring(iCursor, iMarker2+2));
					result.append(OMCTypedArray.resolveOneToken(sRawString.substring(iCursor, iMarker2+2), aWI));
//					if (OMC.DEBUG) Log.i("OMCArray","Now at: " + result.toString());
					//Move all markers to the next [% (we only deal with one layer here).
					//Start looking for the next directive.
					result.append(sRawString.substring(iMarker2+2, iMarker1));
					iCursor = iMarker1;
				}
				} while (iMarker1 != -1);
				
				// Pass it up to resolve nested stuff.
				return OMCTypedArray.resolveTokens(result.toString(), aWI);
			}
		}
		// If no tags found, then just run strftime; we're done! 
		return OMCTypedArray.resolveOneToken(sRawString, aWI);

	}
	
	static public int numberInterpolate (int n1, int n2, int n3, float gradient) {
		if (gradient > 0.5f) return (int)(n2+ (n3-n2)*(gradient-0.5f) * 2);
		else return (int)(n1 + (n2-n1) * gradient * 2);
	}
	
	static public int colorInterpolate (String c1, String c2, String c3, float gradient) {
		int a, r, g, b;
		int maxval, minval;
		if (gradient > 0.5f) {
			minval = Color.parseColor(c2);
			maxval = Color.parseColor(c3);
			gradient = (gradient - 0.5f) * 2;
		} else {
			minval = Color.parseColor(c1);
			maxval = Color.parseColor(c2);
			gradient = gradient * 2;
		}
		a = numberInterpolate(minval >>> 24, maxval >>> 24, gradient); 
		r = numberInterpolate((minval >> 16) & 0xFF, (maxval >> 16) & 0xFF, gradient);
		g = numberInterpolate((minval >> 8) & 0xFF, (maxval >> 8) & 0xFF, gradient);
		b = numberInterpolate(minval & 0xFF , maxval & 0xFF , gradient);

		return Color.argb(a,r,g,b);
	}
	
	static public int numberInterpolate (int n1, int n2, float gradient) {
		return (int)(n1 + (n2-n1) * gradient);
	}
	
	static public int colorInterpolate (String c1, String c2, float gradient) {
		int minval = Color.parseColor(c1);
		int maxval = Color.parseColor(c2);
		return Color.argb(numberInterpolate(minval >>> 24, maxval >>> 24, gradient),
				numberInterpolate((minval >> 16) & 0xFF, (maxval >> 16) & 0xFF, gradient),
				numberInterpolate((minval >> 8) & 0xFF, (maxval >> 8) & 0xFF, gradient),
				numberInterpolate(minval & 0xFF , maxval & 0xFF , gradient)
				);
	}
	
}
