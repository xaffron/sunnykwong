package com.sunnykwong.omc;

import android.content.res.TypedArray;
import android.graphics.Color;

import java.util.ArrayList;

public class OMCTypedArray  {
	String[] mImportedArray;

	public OMCTypedArray(TypedArray TA) {
		// TODO Auto-generated constructor stub		
		mImportedArray = new String[TA.length()];
		for (int i=0;i<TA.length();i++) {
			mImportedArray[i] = TA.getString();
		}
	}
	public OMCTypedArray(ArrayList<String> AL) {
		// TODO Auto-generated constructor stub
		 mImportedArray = new String[AL.size()];
    	 AL.toArray(mImportedArray);
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
}
