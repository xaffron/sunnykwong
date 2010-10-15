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

public class OMPCActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

	ImageButton mBTButton, mWIFIButton, mConfButton;
	BluetoothAdapter mBTA = BluetoothAdapter.getDefaultAdapter();
	WifiManager mWFA; 
	int appWidgetId;
//	Intent mBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	getWindow().setWindowAnimations(android.R.style.Animation_Dialog);

    	if (getIntent().getData() == null)
			appWidgetId=-999;
		else
			appWidgetId = Integer.parseInt(getIntent().getData().getSchemeSpecificPart());

        mWFA = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        
        setContentView(R.layout.main);
        mBTButton = (ImageButton)this.findViewById(R.id.Button01);    
        if (mBTA!=null) {
        	mBTButton.setOnClickListener(this);
        	mBTButton.setOnTouchListener(this);
        	mBTButton.setBackgroundColor(mBTA.isEnabled()? Color.GREEN:Color.RED); 
        } else mBTButton.setEnabled(false);

        mWIFIButton = (ImageButton)this.findViewById(R.id.Button02);    
        mWIFIButton.setOnClickListener(this);
        mWIFIButton.setBackgroundColor(mWFA.isWifiEnabled()? Color.GREEN:Color.RED);

        mConfButton = (ImageButton)this.findViewById(R.id.Button03);    
        mConfButton.setOnClickListener(this);
        (this.findViewById(R.id.toplevel)).setOnClickListener(this);
    }
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v == mBTButton) {
    		System.out.println("BT clicked");
    		if (mBTA.isEnabled()) mBTA.disable();
    		else mBTA.enable();
    	}
    	if (v == mWIFIButton) {
    		System.out.println("WIFI clicked");
    		if (mWFA.isWifiEnabled()) mWFA.setWifiEnabled(false);
    		else mWFA.setWifiEnabled(true);
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

    
    // events when touching the screen
 
  
    @Override
    public boolean onTouch(View v, MotionEvent event) {
         int eventaction = event.getAction();
         int X = (int)event.getX();
         int Y = (int)event.getY();
 
//         switch (eventaction ) {
//         case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on a ball
//              break;
 //        case MotionEvent.ACTION_MOVE:   // touch drag with the ball
 
  
 
                 // move the balls the same as the finger
 
  
 
//             colorballs[balID-1].setX(X-25);
//             colorballs[balID-1].setY(Y-25);
//             break;
//        case MotionEvent.ACTION_UP:
//                // touch drop - just do things here after dropping
//              break;
//         }
//         // redraw the canvas
//         invalidate();
         return true;
     }
    
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	if (mWFA!=null) {
    		mWFA = null;
    	}
    	super.onDestroy();
    }
}