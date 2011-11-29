package com.sunnykwong.FlingWords;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FWSetup extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setuplayout);
        LinearLayout topLevel = (LinearLayout)findViewById(R.id.setuptoplvl);
        
        int size = FW.VOCABINDEX.length();
        for (int i = 0; i < size; i++) {
        	final String sBatchName = FW.VOCABINDEX.optString(i);
        	TextView tv = new TextView(this) {
        		@Override
        		public boolean onTouchEvent(MotionEvent event) {
        			if (event.getAction()==MotionEvent.ACTION_UP) {
        				FW.CURRENTBATCH = FW.VOCABBATCHES.optJSONObject(sBatchName);
        				startActivity(new Intent(getApplicationContext(),FWFlickActivity.class));
        			}
        			return true;
        		}
        	};
        	tv.setPadding(20,20,20,20);
        	tv.setText(sBatchName);
        	topLevel.addView(tv);
        }
        topLevel.requestLayout();
    }
}