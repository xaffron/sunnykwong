package com.sunnykwong.FlingWords;

import java.util.ArrayList;

import android.graphics.Color;
import android.text.Html;
import android.text.SpannedString;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class FlickAdapter extends BaseAdapter {

	public ArrayList<String> mWords = new ArrayList<String>();
	public FWFlickActivity parentActivity = null;
	

    public FlickAdapter(FWFlickActivity parentAv) {
    	this.parentActivity=parentAv;
    }

    public int addItem(final String str){
    	int result = getCount();
    	mWords.add(result,str);
		return result+1;
    }        

    public void removeItem(int pos){
    	if (mWords.size()==0)return;
    	String sTheme = mWords.get(pos);
    	mWords.remove(pos);
    	//mBitmaps.remove(sTheme);
    }        

    public int getCount() {
    	return mWords.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public int getPosition(String sTheme) {
    	if (sTheme==null) return 0;
    	for (int position = 0; position < mWords.size(); position ++) {
    		if (mWords.get(position).equals(sTheme)){
    			return position;
    		}
    	}
    	return 0;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
    	
    	TextView tvFling = new TextView(parentActivity.getApplicationContext());
    	tvFling.setMinWidth(FW.SCRNLONGEREDGELENGTH);
    	tvFling.setTypeface(FW.FONT);
    	tvFling.setBackgroundColor(Color.TRANSPARENT);
    	tvFling.setCursorVisible(false);
    	tvFling.setGravity(Gravity.CENTER);
    	tvFling.setTextColor(FW.TEXTCOLOR);
    	tvFling.setTextSize(FW.TEXTSIZE);
    	float fScaleFactor = Math.min(1f,FW.SCRNLONGEREDGELENGTH/FW.getStringWidth(mWords.get(position)));
    	tvFling.setTextScaleX(fScaleFactor);
    	tvFling.setText(mWords.get(position));
        return tvFling;
    }

    public void dispose() {
    	mWords.clear();
    	parentActivity=null;
    	System.gc();
    }

}

