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
import android.widget.ViewFlipper;
//import android.util.Log;
//import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;

public class TurnActivity extends Activity {

	
	public static ScreenAdapter SCREENADAPTER;
	public static boolean DEBUG;

	public Button mActionButton, mAttack, mItem, mRecruit, mRun;
	public TextView mConsoleView, mBlowbyBlow;
	public View mAction;
	public Gallery mGallery;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);

    	//		Game Setup
    	BJ.MASTER = new GM(this);
    	BJ.TACT = this;
    	
    	setContentView(R.layout.main);
        
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		mAction = inflater.inflate(R.layout.action, null);
        mBlowbyBlow = (TextView)mAction.findViewById(R.id.blowbyblow);
    	mBlowbyBlow.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//nothing
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//nothing
				}
			
			public void afterTextChanged(Editable s) {
				if (s.length()<500) return;
				s.delete(0, s.toString().indexOf("\n")+1);
			}
		});
    	mAttack = (Button)mAction.findViewById(R.id.btAttack);
    	mAttack.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				writeBlow("You Attack!");
				BJ.MASTER.ACTION = BJ.MASTER.ATTACK;
				BJ.MASTER.nextTurn();
			}
		});
    	mItem = (Button)mAction.findViewById(R.id.btItem);
    	mItem.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				writeBlow("You Use an Item!");
				BJ.MASTER.nextTurn();
			}
		});
    	mRecruit = (Button)mAction.findViewById(R.id.btRecruit);
    	mRecruit.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				writeBlow("You try to recruit your foe.");
			}
		});
    	mRun = (Button)mAction.findViewById(R.id.btRun);
    	mRun.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				writeBlow("You try to Flee...");
			}
		});
        
        DEBUG = Boolean.parseBoolean(getString(R.string.DEBUG));
        
        mActionButton = (Button)findViewById(R.id.Button01);
        mActionButton.setClickable(false);
        mConsoleView = (TextView)findViewById(R.id.console);
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
        if (TurnActivity.SCREENADAPTER==null) TurnActivity.SCREENADAPTER = new ScreenAdapter(mAction); 
        mGallery.setAdapter(TurnActivity.SCREENADAPTER);
        mGallery.setSelection(1);
        mGallery.setSelected(false);
        mGallery.setClickable(false);
        
    	mActionButton.setText("Go!!");

        mActionButton.setOnClickListener(new View.OnClickListener(){
        	public void onClick (View v) {
        		//what to do when clicked
        		GM.mt.cancel();
        		mActionButton.setEnabled(false);
        		BJ.MASTER.nextTurn();
        		mActionButton.setEnabled(true);
        	} 
        });
        
    	if (DEBUG) writeConsole("Gui Setup Complete");

       	BJ.MASTER.nextTurn();
        
    }
    
    public void writeBlow(String comment) {
    	mBlowbyBlow.append(comment + "\n");
//    	Log.i("XAFFRON",comment);
    }

    public void writeConsole(String comment) {
    	mConsoleView.append(comment + "\n");
//    	Log.i("XAFFRON",comment);
    }
    public class ScreenAdapter extends BaseAdapter {

    	public View tvConsole;
    	public View ivImage;
    	public View ivImage2;
    	

        public ScreenAdapter(View tv) {
        	tvConsole = tv;
        	ivImage = tv;
        	ivImage2 = tv;
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

