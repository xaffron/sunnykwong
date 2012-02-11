package com.xaffron.biaoju;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.widget.ViewFlipper;
//import android.util.Log;
//import android.view.Window;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;

public class TurnActivity extends Activity implements OnItemSelectedListener {

	
	public static ScreenAdapter SCREENADAPTER;
	public static boolean DEBUG;

	public Button mAttack, mItem, mRecruit, mRun;
	public TextView mConsoleView, mBlowbyBlow;
	public SpannableStringBuilder sBlow;
	public View mAction, mStats, mMap;
	public Gallery mGallery;
@Override
protected void onRestart() {
	// TODO Auto-generated method stub
	super.onRestart();
}
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);

    	//		Game Setup
    	BJ.TACT = this;
    	BJ.MASTER = new GM(this);

    }
    protected void onStart() {
    	super.onStart();
    	
    	setContentView(R.layout.main);
        
        LayoutInflater inflater = LayoutInflater.from(this);
		mAction = inflater.inflate(R.layout.action, null);

		mStats = inflater.inflate(R.layout.stats, null);
		
		mMap = inflater.inflate(R.layout.map,null);
        mBlowbyBlow = (TextView)mAction.findViewById(R.id.blowbyblow);
        mBlowbyBlow.setTypeface(BJ.DEFAULTSCRIPTTYPEFACE);
        mBlowbyBlow.setTextSize(16f);
        mBlowbyBlow.setOnTouchListener(new View.OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				return false;
			}
		});
    	mBlowbyBlow.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//nothing
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//nothing
				}
			
			public void afterTextChanged(Editable s) {
				if (s.length()<300) return;
				s.delete(0, s.toString().indexOf("\n")+1);
			}
		});
    	mAttack = (Button)mAction.findViewById(R.id.btAttack);
    	mAttack.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
//				writeBlow("You Attack!");
				BJ.MASTER.ACTION = BJ.MASTER.ATTACK;
				BJ.MASTER.nextTurn();
			}
		});
    	mItem = (Button)mAction.findViewById(R.id.btItem);
    	mItem.setEnabled(false);
    	mItem.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
//				writeBlow("You Use an Item!");
				BJ.MASTER.nextTurn();
			}
		});
    	mRecruit = (Button)mAction.findViewById(R.id.btRecruit);
    	mRecruit.setEnabled(false);
    	mRecruit.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
//				writeBlow("You try to recruit your foe.");
			}
		});
    	mRun = (Button)mAction.findViewById(R.id.btRun);
    	mRun.setEnabled(false);
    	mRun.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
//				writeBlow("You try to Flee...");
			}
		});
        
        DEBUG = Boolean.parseBoolean(getString(R.string.DEBUG));
        
        mConsoleView = (TextView)findViewById(R.id.console);
        mConsoleView.setTypeface(BJ.DEFAULTSCRIPTTYPEFACE);
        mConsoleView.setTextSize(12f);
        mConsoleView.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//nothing
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//nothing
				}
			
			public void afterTextChanged(Editable s) {
				if (s.length()<80) return;
				s.delete(0, s.toString().indexOf("\n")+1);
			}
		});
        mGallery = (Gallery)findViewById(R.id.details);
        if (TurnActivity.SCREENADAPTER==null) TurnActivity.SCREENADAPTER = new ScreenAdapter(mStats,mAction,mMap); 
        mGallery.setAdapter(TurnActivity.SCREENADAPTER);
        mGallery.setSelection(1);
        mGallery.setOnItemSelectedListener(this);
    	if (DEBUG) writeConsole("Gui Setup Complete");

       	BJ.MASTER.nextTurn();
        
    }
    
    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
    		long arg3) {
    	
    }
    public void onNothingSelected(android.widget.AdapterView<?> arg0) {};
    
    public void writeBlow(String comment) {
    	if (comment==null) {
    		mBlowbyBlow.setText("");
    	} else {
    		mBlowbyBlow.append(comment + "\n");
    	}
//    	Log.i("XAFFRON",comment);
    }

    public void writeConsole(String comment) {
    	mConsoleView.setText(comment);
//    	mConsoleView.append(comment + "\n");
//    	Log.i("XAFFRON",comment);
    }
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		finish();
	}    
    public class ScreenAdapter extends BaseAdapter {

    	public View tvConsole;
    	public View ivImage;
    	public View ivImage2;
    	

        public ScreenAdapter(View stats, View console, View map) {
        	ivImage = stats;
        	tvConsole = console;
        	ivImage2 = map;
        }

        public int getCount() {
            return 3;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	switch (position) {
        	case 0: return ivImage;
        	case 1: return tvConsole;
        	default: return ivImage2;
        	}
        }

        public void dispose() {
        	System.gc();
        }
    
    }
   
}

