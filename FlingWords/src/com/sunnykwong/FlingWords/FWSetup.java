package com.sunnykwong.FlingWords;

import org.json.JSONException;

import com.android.settings.activities.ColorPickerDialog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FWSetup extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setuplayout);
        refreshViews();
    }

    public void refreshViews() {
        LinearLayout topLevel = (LinearLayout)findViewById(R.id.setuptoplvl);
        topLevel.setEnabled(false);
        topLevel.removeAllViews();
        
        final int size = FW.VOCABINDEX.length();
        int iBkColor = Color.BLACK;
        for (int i = 0; i < size; i++) {
        	final String sBatchName = FW.VOCABINDEX.optString(i);
        	if (!FW.VIEWRETIREDBATCHES && FW.VOCABBATCHES.optJSONObject(sBatchName).optBoolean("Retired")) continue;
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
        	if (iBkColor==Color.DKGRAY) iBkColor=Color.GRAY;
        	else iBkColor=Color.DKGRAY;
        	tv.setBackgroundColor(iBkColor);
        	tv.setPadding(20,20,20,20);
        	tv.setText(sBatchName);
        	topLevel.addView(tv);
        }
        topLevel.requestLayout();
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.vocabmenu, menu);
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	
		menu.findItem(R.id.toggleRetiredBatches).setEnabled(true);
		menu.findItem(R.id.importVocab).setEnabled(true);
		menu.findItem(R.id.exportVocab).setEnabled(true);
    	
    	return super.onPrepareOptionsMenu(menu);
    }
    
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		// TODO Auto-generated method stub
    	if (item.getItemId()==R.id.toggleRetiredBatches) {
			FW.VIEWRETIREDBATCHES=!FW.VIEWRETIREDBATCHES;
			refreshViews();
    	}
//    	if (item.getItemId()==getResources().getIdentifier("tweakmenupickColor1", "id", OMC.PKGNAME)) {
//    		int initialColor;
//    		if (oActiveLayer.optString("fgcolor")==null) {
//    			initialColor = Color.BLACK;
//    		} else {
//	    		try {
//	    			initialColor = Color.parseColor(oActiveLayer.optString("fgcolor"));
//	    		} catch (IllegalArgumentException e) {
//	    			initialColor = Color.BLACK;
//	    		}
//    		}
//    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
//				
//				@Override
//				public void colorUpdate(int color) {
//					// TODO Auto-generated method stub
//				}
//				
//				@Override
//				public void colorChanged(int color) {
//					// TODO Auto-generated method stub
//					try {
//						oActiveLayer.put("fgcolor",String.format("#%X", color));
//						refreshViews();
//					} catch (JSONException e) {
//						e.printStackTrace();
//					}
//				}
//			}, initialColor);
//    		cpd.show();
//    	}
//    	if (item.getItemId()==getResources().getIdentifier("tweakmenupickColor2", "id", OMC.PKGNAME)) {
//    		int initialColor;
//    		if (oActiveLayer.optString("bgcolor")==null) {
//    			initialColor = Color.BLACK;
//    		} else {
//	    		try {
//	    			initialColor = Color.parseColor(oActiveLayer.optString("bgcolor"));
//	    		} catch (IllegalArgumentException e) {
//	    			initialColor = Color.BLACK;
//	    		}
//    		}
//    		ColorPickerDialog cpd = new ColorPickerDialog(this, new ColorPickerDialog.OnColorChangedListener() {
//				
//				@Override
//				public void colorUpdate(int color) {
//					// TODO Auto-generated method stub
//				}
//				
//				@Override
//				public void colorChanged(int color) {
//					// TODO Auto-generated method stub
//					try {
//						oActiveLayer.put("bgcolor",String.format("#%X", color));
//						refreshViews();
//					} catch (JSONException e) {
//						e.printStackTrace();
//					}
//				}
//			}, initialColor);
//    		cpd.show();
//    	}
		return true;
	}

}