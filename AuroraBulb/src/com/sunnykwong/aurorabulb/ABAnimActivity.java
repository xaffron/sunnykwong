package com.sunnykwong.aurorabulb;

import java.util.List;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

public class ABAnimActivity extends Activity {

	Thread currentThread, beepThread;
	boolean mDone;
	Handler mHandler;
	ImageView mScrn;
	List<Bitmap> mImagebuffer;
	List<Bitmap> mBitmapBin;
	int[] mSliver;
	long startTime = 0l;
	long thisUpdateTime = 0l;
	long nextFrameTime = 0l;
	long nextSecond = 0l;
	
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
		
		if (AB.SRCBUFFER== null)AB.SRCBUFFER = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(), R.drawable.transparent),320,170,false);

		AB.ROLLBUFFER = Bitmap.createBitmap(AB.BUFFERWIDTH,AB.BUFFERHEIGHT,Bitmap.Config.RGB_565);
		AB.ROLLBUFFER.eraseColor(Color.BLACK);
		AB.ROLLCANVAS = new Canvas(AB.ROLLBUFFER);

		if (AB.PREFS.getString("whatToShow", "text").equals("bitmap")) {

			AB.TEMPMATRIX.reset();
			AB.TEMPMATRIX.postScale(20, (float)AB.ROLLBUFFER.getHeight()/AB.SRCBUFFER.getHeight());
			AB.TEMPMATRIX.postTranslate((float)AB.ROLLBUFFER.getWidth()-30, 0f);
			
		} else {
		
			AB.TEMPMATRIX.reset();
			AB.TEMPMATRIX.postScale((float)AB.ROLLBUFFER.getWidth()-20, (float)AB.ROLLBUFFER.getHeight()/AB.SRCBUFFER.getHeight());
	//		AB.TEMPMATRIX.postTranslate(f, 0f);
			AB.TEMPMATRIX2.reset();
			AB.TEMPMATRIX2.postScale((float)AB.ROLLBUFFER.getWidth(), (float)AB.ROLLBUFFER.getHeight()/AB.SRCBUFFER.getHeight());

		}
			
//		AB.bmpTemp..createBitmap(AB.SRCBUFFER, i, 0, 1, AB.SRCBUFFER.getHeight());
//		AB.bmpTemp2 = Bitmap.createBitmap(AB.SRCBUFFER2, i, 0, 1, AB.SRCBUFFER.getHeight());

		mScrn.setImageBitmap(AB.ROLLBUFFER);
		mScrn.buildDrawingCache();

		countDown();
	}
	public void countDown() {
		// Begin animation; use new thread for max precision
		currentThread = new Thread() {
			public void run() {
				// Countdown
				nextFrameTime = System.currentTimeMillis();
				for (int i=AB.COUNTDOWNSECONDS; i>0; i-- ) {
					nextFrameTime += 1000l;
		
					AB.ROLLBUFFER.eraseColor(Color.BLACK);

					if (i>1) {
						AB.ROLLCANVAS.drawText(String.valueOf(i), AB.BUFFERWIDTH/2, 0-AB.PT2.getFontMetricsInt().ascent, AB.PT2);
					}

					AB.ROLLBUFFER.prepareToDraw();
					mHandler.post(mFlip);
					AB.BEEPER.start();

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
		currentThread.start();

	}
	public void renderFrames() {

		// Begin animation; use new thread for max precision
		beepThread = new Thread() {
			public void run() {
				// Countdown
				nextSecond = System.currentTimeMillis();
				for (int i=0; i<Integer.parseInt(AB.PREFS.getString("timeShutterDuration", "10")); i++ ) {
					nextSecond += 1000l;
		
					AB.BEEPER.start();

					while (System.currentTimeMillis() < nextSecond) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
		
				}
				AB.BEEPER.start();
			}
		};		
		beepThread.start();

		currentThread = new Thread() {
			public void run() {
				Bitmap bmpTemp, bmpTemp2;
				startTime = System.currentTimeMillis();
				nextFrameTime = System.currentTimeMillis();
				for (int i=0; i<AB.SRCBUFFER.getWidth(); i++ ) {
					nextFrameTime += 1000l/AB.TARGETFPS;
					
					AB.ROLLCANVAS.save();
					bmpTemp = Bitmap.createBitmap(AB.SRCBUFFER, i, 0, 1, AB.SRCBUFFER.getHeight());
					bmpTemp.prepareToDraw();
					bmpTemp2 = Bitmap.createBitmap(AB.SRCBUFFER2, i, 0, 1, AB.SRCBUFFER.getHeight());
					bmpTemp2.prepareToDraw();
					if (AB.PREFS.getString("whatToShow", "text").equals("text")) {
						AB.ROLLCANVAS.drawBitmap(bmpTemp2, AB.TEMPMATRIX2, AB.PT1);
					}
					AB.ROLLCANVAS.drawBitmap(bmpTemp, AB.TEMPMATRIX, AB.PT1);

					while (System.currentTimeMillis() < nextFrameTime) {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					mHandler.post(mFlip);
					AB.ROLLCANVAS.restore();
					

					if (AB.DEBUG) Log.i("ABAnim","FPS: " + (i+1)*1000f/(float)(thisUpdateTime-startTime));

				}
				mHandler.post(mFlip);
				AB.ROLLBUFFER.eraseColor(Color.BLACK);
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
		currentThread.start();
	}
	@Override
	protected void onPause() {
		super.onPause();
		if (currentThread !=null && currentThread.isAlive()) currentThread.interrupt();
		if (beepThread !=null && beepThread.isAlive()) beepThread.interrupt();
	}
}
