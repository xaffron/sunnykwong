package com.sunnykwong.aurorabulb;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import java.util.HashMap;
import android.graphics.Paint;

import android.graphics.Color;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.Paint.Align;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;

import android.view.Display;

public class ABAnimActivity extends Activity {

	boolean mDone;
	Handler mHandler;
	ImageView mScrn;
	List<Bitmap> mImagebuffer;
	List<Bitmap> mBitmapBin;
	int[] mSliver;
	long startTime = 0l;
	long thisUpdateTime = 0l;
	long nextFrameTime = 0l;
	
	final Runnable mAnim = new Runnable() {
		public void run() {
			renderFrames();
		}
	};
	final Runnable mFlip = new Runnable() {
		public void run() {
			thisUpdateTime = System.currentTimeMillis();
			mScrn.invalidate();
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mScrn = (ImageView)findViewById(R.id.ScreenImage);
		mHandler = new Handler();
		mDone = false;
		AB.PT1.setTextAlign(Paint.Align.CENTER);
		AB.PT1.setTextScaleX(1f);

		AB.SCRNWIDTH = getWindowManager().getDefaultDisplay().getWidth();
		AB.SCRNHEIGHT = getWindowManager().getDefaultDisplay().getHeight();
		
		if (AB.SRCBUFFER== null)AB.SRCBUFFER = BitmapFactory.decodeResource(getResources(), R.drawable.llpreview);

		AB.ROLLBUFFER = Bitmap.createBitmap(AB.BUFFERWIDTH,AB.BUFFERHEIGHT,Bitmap.Config.ARGB_4444);
		AB.ROLLCANVAS = new Canvas(AB.ROLLBUFFER);
		AB.ROLLCANVAS.setDensity(DisplayMetrics.DENSITY_HIGH);

		AB.TEMPMATRIX.reset();
		AB.TEMPMATRIX.postScale((float)AB.SCRNWIDTH, (float)AB.ROLLBUFFER.getHeight()/AB.SRCBUFFER.getHeight());
		
		mScrn.setImageBitmap(AB.ROLLBUFFER);
		mScrn.buildDrawingCache();

		countDown();
	}
	public void countDown() {
		// Begin animation; use new thread for max precision
		Thread t = new Thread() {
			public void run() {
				// Countdown
				nextFrameTime = System.currentTimeMillis();
				for (int i=AB.COUNTDOWNSECONDS; i>0; i-- ) {
					nextFrameTime += 1000l;
		
					AB.ROLLCANVAS.drawColor(Color.BLACK);
					AB.ROLLCANVAS.drawText(String.valueOf(i), AB.BUFFERWIDTH/2, 2*AB.BUFFERHEIGHT/3, AB.PT1);
					mHandler.post(mFlip);
					
					while (System.currentTimeMillis() < nextFrameTime) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
		
				}
				mHandler.post(mAnim);
			}
		};		
		t.start();

	}
	public void renderFrames() {

		// Begin animation; use new thread for max precision
		Thread t = new Thread() {
			public void run() {
				Bitmap bmpTemp;
				startTime = System.currentTimeMillis();
				nextFrameTime = System.currentTimeMillis();
				for (int i=0; i<AB.SRCBUFFER.getWidth(); i++ ) {
					nextFrameTime += 1000l/AB.TARGETFPS;

					AB.ROLLCANVAS.drawColor(Color.BLACK);
					bmpTemp = Bitmap.createBitmap(AB.SRCBUFFER, i, 0, 1, AB.SRCBUFFER.getHeight());
					bmpTemp.prepareToDraw();
					AB.ROLLCANVAS.drawBitmap(bmpTemp, AB.TEMPMATRIX, AB.PT1);

					while (System.currentTimeMillis() < nextFrameTime) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					mHandler.post(mFlip);

					if (AB.DEBUG) Log.i("ABAnim","FPS: " + (i+1)*1000f/(float)(thisUpdateTime-startTime));

				}
				AB.ROLLCANVAS.drawColor(Color.BLACK);
				mHandler.post(mFlip);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				setResult(Activity.RESULT_OK);
				finish();
				
			}
		};
		t.start();
	}
}
