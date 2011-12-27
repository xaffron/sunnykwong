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
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;

public class TurnActivity extends Activity {

	
	public static ScreenAdapter SCREENADAPTER;
	public static boolean DEBUG;

	public Button mActionButton;
	public TextView mConsoleView, mBlowbyBlow;
	public View mAction;
	public Gallery mGallery;

	static GameMaster master;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
		mAction = inflater.inflate(R.layout.action, null);
        mBlowbyBlow = (TextView)mAction.findViewById(R.id.blowbyblow);
        
        DEBUG = Boolean.parseBoolean(getString(R.string.DEBUG));
        
        mActionButton = (Button)findViewById(R.id.Button01);
        mActionButton.setClickable(false);
        mConsoleView = (TextView)findViewById(R.id.console);
        mGallery = (Gallery)findViewById(R.id.details);
        if (TurnActivity.SCREENADAPTER==null) TurnActivity.SCREENADAPTER = new ScreenAdapter(mAction); 
        mGallery.setAdapter(TurnActivity.SCREENADAPTER);
        mGallery.setSelection(1);
    	mBlowbyBlow.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//nothing
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//nothing
				}
			
			public void afterTextChanged(Editable s) {
				if (s.length()<1000) return;
				s.delete(0, s.toString().indexOf("\n")+1);
			}
		});
        
    	mActionButton.setText("出發!!");

        mActionButton.setOnClickListener(new View.OnClickListener(){
        	public void onClick (View v) {
        		//what to do when clicked
        		GameMaster.mt.cancel();
        		master.nextTurn();
        	} 
        });
        
    	if (DEBUG) writeLog("Gui Setup Complete");

    	//		Game Setup
    	master = new GameMaster(this);
    	master.nextTurn();
    }
    
    public void writeLog(String comment) {
    	mBlowbyBlow.append(comment + "\n");
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

