package com.sunnykwong.omc;

import java.util.StringTokenizer;
import android.graphics.Color;
import android.util.Log;

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
		return Integer.parseInt(mImportedArray[index].toString());
	}
	public float getFloat(int index, float defValue) {
		return Float.parseFloat(mImportedArray[index].toString());
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
		String sBuffer;
		//by default, return strftime'd string.
		if (OMC.PREFS.getBoolean("widget24HrClock"+aWI, true)) {
			sBuffer = (OMC.TIME.format(
				OMC.PREFS.getBoolean("widgetLeadingZero", true)? 
					sRawString : sRawString.replaceAll("%H", "%k")
				)
			);
		} else {
			sBuffer = (OMC.TIME.format(
				OMC.PREFS.getBoolean("widgetLeadingZero", true)? 
					sRawString.replaceAll("%I", "%k") : sRawString.replaceAll("%H", "%l")
				)
			);
		}
		
		String result = sBuffer;
		
		// If it is actually a dynamic element, resolve it

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
				result = String.valueOf("#" + 								
						Integer.toHexString(Color.alpha(color)) +
						Integer.toHexString(Color.red(color)) +
						Integer.toHexString(Color.green(color)) +
						Integer.toHexString(Color.blue(color))
						);
			} else {
				//Unknown - do nothing
			}

		} else if (sToken.equals("ap24")) { // must be color
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
		} else if (sToken.equals("fullenglishtime")){
			// full english time
			String sType = st.nextToken();
			String sTemp = "";
			if (OMC.TIME.minute == 0) {
				sTemp = OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + " o'Clock.";
			} else if (OMC.TIME.minute == 30) {
				sTemp = "** half past " + OMC.WORDNUMBERS[Integer.parseInt(OMC.TIME.format("%I"))] + "."; 
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
				result = ("#" + 								
						Integer.toHexString(Color.alpha(color)) +
						Integer.toHexString(Color.red(color)) +
						Integer.toHexString(Color.green(color)) +
						Integer.toHexString(Color.blue(color))
						);
			} else {
				//Unknown - do nothing
			}
			

		} else {
			//unrecognized macro - ignore
		}
		
		return result;
		
	}

	static public String resolveTokens(String sRawString, int aWI) {
		System.out.println("Parsing " + sRawString);

		StringBuilder result = new StringBuilder();

		// If token contains dynamic elements, recursively resolve them
		
		if (sRawString.contains("[%")) {
			System.out.println("Found [% ");
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
				
				System.out.println("Appended. Now at " + result.toString());

				iCursor = sRawString.indexOf("[%",iCursor);

				int iMarker1, iMarker2;
				do {
				// Mark where the next [% and %] are.  Which is closer?
				iMarker1 = sRawString.indexOf("[%",iCursor+2);
				iMarker2 = sRawString.indexOf("%]",iCursor+2);

				System.out.println("Markers: " +iCursor+" " + iMarker1+ " " + iMarker2);
				// No markers found? bad parsing occurred.
				// exit gracefully (try to)
				if (iMarker1 == -1 && iMarker2 == -1) {
					Log.i("OMCArray","Improper markup. Bailing...");
					break;
					
				} else if (iMarker1 == -1) {
					// No more start markers found, but we have an end marker.
					// Dive into this substring.
					Log.i("OMCArray","Detected directive: " + sRawString.substring(iCursor+2, iMarker2));
					result.append(OMCTypedArray.resolveOneToken(sRawString.substring(iCursor+2, iMarker2), aWI));
				} else if (iMarker1 < iMarker2) {
					//if [% is closer, keep going until we find the innermost [%
					Log.i("OMCArray","Found nested. ");
					result.append(sRawString.substring(iCursor,iMarker1));
					Log.i("OMCArray","Now at: " + result.toString());
					iCursor = iMarker1;
				} else if (iMarker2 < iMarker1) {
					//if %] is closer, we have an end marker.
					//Dive into this substring.
					Log.i("OMCArray","Detected directive: " + sRawString.substring(iCursor+2, iMarker2));
					result.append(OMCTypedArray.resolveOneToken(sRawString.substring(iCursor+2, iMarker2), aWI));
					Log.i("OMCArray","Sending down: " + sRawString.substring(iCursor+2, iMarker2));
					Log.i("OMCArray","Now at: " + result.toString());
					//Start looking for the next directive.
					iCursor = iMarker2+2;
				}
				} while (iMarker1 != -1);
				
				// Pass it up to resolve nested stuff.
				return OMCTypedArray.resolveTokens(result.toString(), aWI);
			}
		}
		// If no tags found, then just return the raw string; we're done! 
		return sRawString;

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
