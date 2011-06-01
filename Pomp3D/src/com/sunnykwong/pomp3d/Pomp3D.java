package com.sunnykwong.pomp3d;

/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;


/**
 * Wrapper activity demonstrating the use of {@link GLSurfaceView}, a view
 * that uses OpenGL drawing into a dedicated surface.
 */
public class Pomp3D extends Activity implements View.OnTouchListener {
	public CubeRenderer mCR;
	public Thread tBouncingBall;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create our Preview view and set it as the content of our
        // Activity
        mGLSurfaceView = new GLSurfaceView(this);
        mCR = new CubeRenderer(true);
        mGLSurfaceView.setRenderer(mCR);
        setContentView(mGLSurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        
        tBouncingBall = new Thread() {
        	@Override
        	public void run() {
        		//Bouncing square starts here
        		float[] velocity={0.1f,0.1f,0.1f};
        		float xwall=10f,ywall=10f,zwall=10f;

        		while(true) {
	        		if (Math.abs(mCR.mBX)>=xwall) velocity[0]*=-1f;
	        		if (Math.abs(mCR.mBY)>=ywall) velocity[1]*=-1f;
	        		if (Math.abs(mCR.mBZ)>=zwall) velocity[2]*=-1f;
	        		
	        		mCR.mBX+=velocity[0];
	        		mCR.mBY+=velocity[1];
	        		mCR.mBZ+=velocity[2];
	        		
	        		try {
	        			Thread.sleep(10);
	        		} catch (InterruptedException e) {
	        			break;
	        		}
        		}
        	}
        };
        tBouncingBall.start();
    }

    @Override
    public boolean onTouch(View arg0, MotionEvent arg1) {
    	System.out.println(arg1.getX() + " " + arg1.getY());
    	mCR.mDegrees=arg1.getY();
    	mCR.mXPos=(arg1.getX()-240f)/100f;
    	return true;
    };
    
    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mGLSurfaceView.onPause();
        if (tBouncingBall!=null && tBouncingBall.isAlive()) tBouncingBall.interrupt();
    }

    private GLSurfaceView mGLSurfaceView;
}
