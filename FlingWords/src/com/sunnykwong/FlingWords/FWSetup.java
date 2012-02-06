package com.sunnykwong.FlingWords;

import org.json.JSONException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Color;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FWSetup extends Activity {
	
	TextView mTextView;
	CheckBox mCheckBox;
	AlertDialog mAD;
	
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
        
		if (this.getPreferences(MODE_PRIVATE).getBoolean("SHOWHELP", true)) {
			LayoutInflater li = LayoutInflater.from(this);
			LinearLayout ll = (LinearLayout)(li.inflate(R.layout.faqdialog, null));
			mTextView = (TextView)ll.findViewById(R.id.splashtext);
			mTextView.setAutoLinkMask(Linkify.ALL);
			mTextView.setMinLines(3);
			mTextView.setText(FW.FAQS[FW.faqtoshow++]);
			FW.faqtoshow = FW.faqtoshow==FW.FAQS.length?0:FW.faqtoshow;
			
			mCheckBox = (CheckBox)ll.findViewById(R.id.splashcheck);
			mCheckBox.setChecked(!this.getPreferences(MODE_PRIVATE).getBoolean("SHOWHELP", true));
			mCheckBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// TODO Auto-generated method stub
					FWSetup.this.getPreferences(MODE_PRIVATE).edit().putBoolean("SHOWHELP", !isChecked).commit();
				}
			});

			((Button)ll.findViewById(R.id.faqOK)).setOnClickListener(new Button.OnClickListener() {
				
				@Override
				public void onClick(android.view.View v) {
					mAD.dismiss();
				}
			});
			((Button)ll.findViewById(R.id.faqNeutral)).setOnClickListener(new Button.OnClickListener() {
				
				@Override
				public void onClick(android.view.View v) {
					mTextView.setText(FW.FAQS[FW.faqtoshow++]);
					mTextView.invalidate();
					FW.faqtoshow = FW.faqtoshow==FW.FAQS.length?0:FW.faqtoshow;
				}
			});;
			
			mAD = new AlertDialog.Builder(this)
			.setTitle("Welcome to FlingWords!")
		    .setCancelable(true)
		    .setView(ll)
		    .setOnKeyListener(new OnKeyListener() {
		    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
		    		if (arg2.getKeyCode()==android.view.KeyEvent.KEYCODE_BACK) mAD.cancel();
		    		return true;
		    	};
		    })
		    .show();
		}

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