package com.sunnykwong.omc;

import java.util.StringTokenizer;
import android.graphics.Color;

import java.util.ArrayList;

public class OMCTypedArray  {
	String[] mImportedArray;

	public OMCTypedArray(String[] strArray) {
		// TODO Auto-generated constructor stub		
		mImportedArray = strArray;
		OMCTypedArray.tokenize(mImportedArray);
	}
	public OMCTypedArray(ArrayList<String> AL) {
		// TODO Auto-generated constructor stub
		mImportedArray = new String[AL.size()];
    	AL.toArray(mImportedArray);
 		OMCTypedArray.tokenize(mImportedArray);
	}
	public boolean getBoolean(int index, boolean defValue) {
		// TODO Auto-generated method stub
		return Boolean.parseBoolean(mImportedArray[index]);
	}
	public int getColor(int index, int defValue) {
		// TODO Auto-generated method stub
		return Color.parseColor(mImportedArray[index]);
	}
	public String getString(int index) {
		// TODO Auto-generated method stub
		return mImportedArray[index];
	}
	public int getInt(int index, int defValue) {
		// TODO Auto-generated method stub
		return Integer.parseInt(mImportedArray[index]);
	}
	public float getFloat(int index, float defValue) {
		// TODO Auto-generated method stub
		return Float.parseFloat(mImportedArray[index]);
	}
	public void recycle() {
		mImportedArray = null;
	}
	static public void tokenize(String[] sArray) {
		for (String s:sArray) {
			if (!s.contains("^")) continue;
			StringBuilder result = new StringBuilder();
			StringTokenizer st = new StringTokenizer(s, "^");
			// Where are we in time?  intGradientSeconds starts the count from 6am (hence the +18)
			// and allows "every" to be calculated properly later.
			int intGradientSeconds = OMC.TIME.second + OMC.TIME.minute*60 + (OMC.TIME.hour+18)*3600;
			//Get the first element.
			String sub = st.nextToken();
			if (sub.startsWith("every")) {
				StringTokenizer sMacroToken = new StringTokenizer(sub.substring(6),"_^");
				String innerSub = sMacroToken.nextToken();
				float gradient = (intGradientSeconds % Integer.parseInt(innerSub))/(float)Integer.parseInt(innerSub);
				System.out.println(intGradientSeconds);
				System.out.println(gradient);
				
			} else {
				//unrecognized macro - ignore
			}
			result.append(sub);

			//For the remaining elements (if any).
			while (st.hasMoreElements()) {
				sub = st.nextToken();
				if (sub.startsWith("every")) {
					StringTokenizer sMacroToken = new StringTokenizer(sub.substring(6),"_^");
					String innerSub = sMacroToken.nextToken();
					System.out.println(innerSub);
					int interval = Integer.parseInt(innerSub);
					float gradient = (intGradientSeconds % interval)/(float)interval;
					System.out.println(intGradientSeconds);
					System.out.println(gradient);
					String type = sMacroToken.nextToken();
					System.out.println("TYPE: "+ type);
					if (type.equals("number")) {
						System.out.println("NUMBER");
						if (gradient<=0.5) {
							result.append(OMCTypedArray.numberInterpolate(Integer.parseInt(sMacroToken.nextToken()), Integer.parseInt(sMacroToken.nextToken()), gradient));
							//Throw away the third number
							sMacroToken.nextToken();
						} else {	
							//Throw away the first number
							sMacroToken.nextToken();
							result.append(OMCTypedArray.numberInterpolate(Integer.parseInt(sMacroToken.nextToken()), Integer.parseInt(sMacroToken.nextToken()), gradient-0.5f));
						}
					} else if (type.equals("color")) {
						System.out.println("COLOR");
						if (gradient<=0.5) {
							int color = OMCTypedArray.numberInterpolate(Color.parseColor(sMacroToken.nextToken()), Color.parseColor(sMacroToken.nextToken()), gradient);
							result.append("#" +
									Integer.toHexString(Color.alpha(color)) +
									Integer.toHexString(Color.red(color)) +
									Integer.toHexString(Color.green(color)) +
									Integer.toHexString(Color.blue(color))
									);
							//Throw away the third number
							sMacroToken.nextToken();
						} else {	
							//Throw away the first number
							sMacroToken.nextToken();
							int color = OMCTypedArray.numberInterpolate(Color.parseColor(sMacroToken.nextToken()), Color.parseColor(sMacroToken.nextToken()), gradient-0.5f);
							result.append("#" +
									Integer.toHexString(Color.alpha(color)) +
									Integer.toHexString(Color.red(color)) +
									Integer.toHexString(Color.green(color)) +
									Integer.toHexString(Color.blue(color))
									);
						}
					}
					
				} else {
					//unrecognized macro - ignore
					result.append("^");
					result.append(sub);
				}
			}
			System.out.println("PARSING RESULT: " + result.toString());
			s=result.toString();
			System.out.println("PARSING RESULT: " + s);
		}

		

	}

	static public int numberInterpolate (int n1, int n2, float gradient) {
		return (int)(n1+ (n2-n1)*gradient);
	}
	
}
