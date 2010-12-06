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

	Matrix mMatrix;
	boolean mDone;
	Handler mHandler;
	ImageView mScrn;
	List<Bitmap> mImagebuffer;
	List<Bitmap> mBitmapBin;
	int[] mSliver;
	long lastUpdateTime = 0l;
	long thisUpdateTime = 0l;
	long nextFrameTime = 0l;
	
	final Runnable mFlip = new Runnable() {
		public void run() {
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

		AB.SCRNWIDTH = getWindowManager().getDefaultDisplay().getWidth();
		AB.SCRNHEIGHT = getWindowManager().getDefaultDisplay().getHeight();
		
		AB.SRCBUFFER = BitmapFactory.decodeResource(getResources(), R.drawable.llpreview);

		AB.ROLLBUFFER = Bitmap.createBitmap(240,320,Bitmap.Config.ARGB_4444);
		AB.ROLLCANVAS = new Canvas(AB.ROLLBUFFER);
		AB.ROLLCANVAS.setDensity(DisplayMetrics.DENSITY_HIGH);

		mMatrix = new Matrix();
		mMatrix.postScale((float)AB.SCRNWIDTH, (float)AB.ROLLBUFFER.getHeight()/AB.SRCBUFFER.getHeight());
		
		AB.PT1 = new Paint();

		mScrn.setImageBitmap(AB.ROLLBUFFER);
		mScrn.buildDrawingCache();

		renderFrames();
	}
	
	public void renderFrames() {

		Thread t = new Thread() {
			public void run() {
				Bitmap bmpTemp;
				nextFrameTime = System.currentTimeMillis();
				for (int i=0; i<AB.SRCBUFFER.getWidth(); i++ ) {
					nextFrameTime += 50; //20fps
					bmpTemp = Bitmap.createBitmap(AB.SRCBUFFER, i, 0, 1, AB.SRCBUFFER.getHeight());
					bmpTemp.prepareToDraw();
					AB.ROLLCANVAS.drawBitmap(bmpTemp, mMatrix, AB.PT1);

					while (System.currentTimeMillis() < nextFrameTime) {
						try {
							Thread.sleep(3);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					
					thisUpdateTime = System.currentTimeMillis();
					System.out.println("FPS: " + 1000f/(float)(thisUpdateTime-lastUpdateTime));
					mHandler.post(mFlip);
					lastUpdateTime = thisUpdateTime;

				}
				AB.ROLLCANVAS.drawColor(Color.BLACK);
				mHandler.post(mFlip);
				
				
			}
		};
		t.start();
	}
}