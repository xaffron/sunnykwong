package com.sunnykwong.omc;

import android.content.res.TypedArray;
import android.graphics.Color;

import java.util.ArrayList;

public class OMCTypedArray  {
	TypedArray mTArray;
	String[] mImportedArray;
	boolean bExternal;

	public OMCTypedArray(TypedArray TA) {
		// TODO Auto-generated constructor stub
		mTArray = TA;
		mImportedArray = null;
		bExternal = false;
	}
	public OMCTypedArray(ArrayList<String> AL) {
		// TODO Auto-generated constructor stub
		 mTArray = null;
		 mImportedArray = new String[50];
    	 AL.toArray(mImportedArray);
		 bExternal = true;
	}
	public boolean getBoolean(int index, boolean defValue) {
		// TODO Auto-generated method stub
		if (bExternal) return Boolean.parseBoolean(mImportedArray[index]);
		return mTArray.getBoolean(index, defValue);
	}
	public int getColor(int index, int defValue) {
		// TODO Auto-generated method stub
		if (bExternal) return Color.parseColor(mImportedArray[index]);
		return mTArray.getColor(index, defValue);
	}
	public String getString(int index) {
		// TODO Auto-generated method stub
		if (bExternal) return mImportedArray[index];
		return mTArray.getString(index);
	}
	public int getInt(int index, int defValue) {
		// TODO Auto-generated method stub
		if (bExternal) return Integer.parseInt(mImportedArray[index]);
		return mTArray.getInt(index, defValue);
	}
	public float getFloat(int index, float defValue) {
		// TODO Auto-generated method stub
		if (bExternal) return Float.parseFloat(mImportedArray[index]);
		return mTArray.getFloat(index, defValue);
	}
	public void recycle() {
		if (mTArray != null) {
			mTArray.recycle();
			mTArray=null;
		}
		mImportedArray = null;
	}
}
