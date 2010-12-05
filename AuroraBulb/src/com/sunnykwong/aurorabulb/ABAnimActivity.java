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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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

public class ABAnimActivity extends Activity {

	Handler mHandler;
	ImageView mScrn;
	List<Bitmap> mImagebuffer;
	
	final Runnable mFlip = new Runnable() {
		public void run() {
			System.out.println("Flipping");
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

		mImagebuffer = Collections.synchronizedList(new LinkedList<Bitmap>());
		AB.SCRNBUFFER = BitmapFactory.decodeResource(getResources(), R.drawable.llpreview);
		AB.SCRNCANVAS = new Canvas(AB.SCRNBUFFER);
		AB.PT1 = new Paint();
		
		mScrn.setImageBitmap(AB.SCRNBUFFER);
		mScrn.buildDrawingCache();
		mScrn.invalidate();
		
		bufferFrames();
	}
	
	public void bufferFrames() {
		
		Thread bufferThread = new Thread() {
			int buffered = 0;
			int i = 0;
			public void run() {
				System.out.println("outside while " + i);
				while (i < AB.SCRNBUFFER.getWidth()) {
					System.out.println("I " + i);
					while (buffered < 10) {
						Bitmap bmpTemp = Bitmap.createBitmap(320,AB.SCRNBUFFER.getHeight(),Bitmap.Config.ARGB_4444);
						bmpTemp.eraseColor(Color.BLACK);
						int[] iSliver = new int[800];
						AB.SCRNBUFFER.getPixels(iSliver, 0, 1, i, 0, 1, AB.SCRNBUFFER.getHeight());
						for (int j=0;j<320;j++) {
							bmpTemp.setPixels(iSliver, 0, 1, j, 0, 1, AB.SCRNBUFFER.getHeight());
						}
						mImagebuffer.add(bmpTemp);
						i++;
						buffered = mImagebuffer.size();
						System.out.println(buffered);
					}
					
					while (buffered >= 10) {
						try {
							
							Thread.sleep(100);
						} catch (InterruptedException e ) {
							e.printStackTrace();
						}
					}
				}
				
			}
		};
		bufferThread.start();
			
		Thread t = new Thread() {
			@Override
			public void run() {
				while (mImagebuffer.size()<10) {
					System.out.println("Waiting for buffer to fill " + mImagebuffer.size());
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				while (mImagebuffer.size() > 0) {
					System.out.println("Dequeuing " + mImagebuffer.size());
					Bitmap temp = mImagebuffer.remove(0);
					AB.SCRNCANVAS.drawBitmap(temp, 0f, 0f, AB.PT1);
					
					mHandler.post(mFlip);

					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}	
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		finish();
		
	}
	
}
