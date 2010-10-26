package com.sunnykwong.ompc;


import android.app.Activity;
import android.view.WindowManager.LayoutParams;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.os.Bundle;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.widget.ImageButton;
import android.view.View;
import android.bluetooth.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.MotionEvent;
import android.widget.AbsoluteLayout;
import android.content.ComponentName;

public class OMPCActivity extends Activity implements View.OnClickListener, View.OnTouchListener, View.OnLongClickListener {

	ImageButton mBTButton, mWIFIButton, mAlarmButton, mConfButton;
	View mWholePanel;
	static BluetoothAdapter BTA = BluetoothAdapter.getDefaultAdapter();;
	static WifiManager WFA; 
	int appWidgetId;
	static int X, Y;
	static boolean bHELD;
//	Intent mBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	getWindow().setWindowAnimations(android.R.style.Animation_Toast);

    	OMPCActivity.bHELD = false;
    	
    	OMPCActivity.X= getPreferences(MODE_PRIVATE).getInt("X", 1);
    	OMPCActivity.Y= getPreferences(MODE_PRIVATE).getInt("Y", 200);
    	
    	
    	if (getIntent().getData() == null)
			appWidgetId=-999;
		else
			appWidgetId = Integer.parseInt(getIntent().getData().getSchemeSpecificPart());

    	setContentView(R.layout.main);
    	(this.findViewById(R.id.OMPCPanel)).setLayoutParams(new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,AbsoluteLayout.LayoutParams.WRAP_CONTENT,OMPCActivity.X, OMPCActivity.Y ));
        (this.findViewById(R.id.toplevel)).requestLayout();        
        
        mBTButton = (ImageButton)OMPCActivity.this.findViewById(R.id.Button01);    
    	mBTButton.setOnClickListener(OMPCActivity.this);

    	mWIFIButton = (ImageButton)OMPCActivity.this.findViewById(R.id.Button02);    
        mWIFIButton.setOnClickListener(OMPCActivity.this);

    	mAlarmButton = (ImageButton)OMPCActivity.this.findViewById(R.id.Button02a);    
        mAlarmButton.setOnClickListener(OMPCActivity.this);

        mConfButton = (ImageButton)OMPCActivity.this.findViewById(R.id.Button03);    
        mConfButton.setOnClickListener(OMPCActivity.this);

        
        mWholePanel = (View)OMPCActivity.this.findViewById(R.id.OMPCPanel);
        mWholePanel.setOnLongClickListener(OMPCActivity.this);
        mWholePanel.setOnTouchListener(OMPCActivity.this);
        ((View)OMPCActivity.this.findViewById(R.id.toplevel)).setOnClickListener(this);

    	Thread t = new Thread () {
    		public void run() {
    			if (OMPCActivity.WFA == null) OMPCActivity.WFA = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	        
    	        mBTButton = (ImageButton)OMPCActivity.this.findViewById(R.id.Button01);    
    	        if (OMPCActivity.BTA!=null) {
    	        	mBTButton.setBackgroundResource(OMPCActivity.BTA.isEnabled()? R.drawable.icon_on:R.drawable.icon_off); 
    	        } else mBTButton.setEnabled(false);
    	        mWIFIButton.setBackgroundResource(OMPCActivity.WFA.isWifiEnabled()? R.drawable.icon_on:R.drawable.icon_off); 

    		}
      	   
    	};
		t.start();

    }
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v == mBTButton) {
    		System.out.println("BT clicked");
        	Thread t = new Thread () {
        		public void run() {
            		if (OMPCActivity.BTA.isEnabled()) OMPCActivity.BTA.disable();
            		else OMPCActivity.BTA.enable();
        		}
          	   
        	};
    		t.start();

    	}
    	if (v == mWIFIButton) {
    		System.out.println("WIFI clicked");
        	Thread t = new Thread () {
        		public void run() {
            		if (OMPCActivity.WFA.isWifiEnabled()) OMPCActivity.WFA.setWifiEnabled(false);
            		else OMPCActivity.WFA.setWifiEnabled(true);
        		}
          	   
        	};
    		t.start();

    	}
    	if (v == mAlarmButton) {
    		System.out.println("Alarm clicked");
        	try {
        		Intent intent = Intent.parseUri("#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;component=com.android.deskclock/.DeskClock;end", 0);
            	PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
            	pi.send();
            } catch (Exception e) {
                	System.out.println("Error firing alarm activity");
            		e.printStackTrace();
            }

    	}
    	if (v == mConfButton) {
    		System.out.println("Config clicked");

    		Intent intent = new Intent("com.sunnykwong.omc.WIDGET_INDIRECT");
            intent.setData(Uri.parse("omc:"+appWidgetId));
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            try {
            	pi.send();
            } catch (CanceledException e) {
            	System.out.println("Error firing broadcast to OMC config");
            	e.printStackTrace();
            }
            
    	}
    	if (v == this.findViewById(R.id.toplevel)) {
    		System.out.println("Background clicked");
    	}
    	finish();

    }
    
    @Override
    public boolean onLongClick(View arg0) {
		System.out.println("longclicked");
    	OMPCActivity.bHELD = true;
    	return true;
    }
   
    // events when touching the screen
 
  
    @Override
    public boolean onTouch(View v, MotionEvent event) {
         int eventaction = event.getAction();
         OMPCActivity.X = 0;//(int)event.getRawX()-30;
         OMPCActivity.Y = (int)event.getRawY()-30;
 		System.out.println("ontouch");
         if (!OMPCActivity.bHELD) return false;
         switch (eventaction) {
         case MotionEvent.ACTION_MOVE:   // touch drag with the ball
        	System.out.println("action move x " + OMPCActivity.X + " y " + OMPCActivity.Y);
  
        	(this.findViewById(R.id.OMPCPanel)).setLayoutParams(new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,AbsoluteLayout.LayoutParams.WRAP_CONTENT,OMPCActivity.X, OMPCActivity.Y ));

             break;
        case MotionEvent.ACTION_UP:
                // touch drop - just do things here after dropping
        	(this.findViewById(R.id.OMPCPanel)).setLayoutParams(new AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.WRAP_CONTENT,AbsoluteLayout.LayoutParams.WRAP_CONTENT,OMPCActivity.X, OMPCActivity.Y ));
        	getPreferences(MODE_PRIVATE).edit()
        		.putInt("X", OMPCActivity.X)
        		.putInt("Y", OMPCActivity.Y)
        		.commit();
        	OMPCActivity.bHELD =false;
            break;
         }
         // redraw the canvas
         (this.findViewById(R.id.toplevel)).requestLayout();        
         return true;
     }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	if (OMPCActivity.WFA!=null) {
    		OMPCActivity.WFA = null;
    	}
    	super.onDestroy();
    }
}