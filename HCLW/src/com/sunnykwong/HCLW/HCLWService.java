package com.sunnykwong.HCLW;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.VelocityTracker;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class HCLWService extends WallpaperService  {

	//	 Code for oncreate/ondestroy.
	//	 Code stolen wholesale from api samples:
	//	 http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//	
	//	When service is created,
	@Override
	public void onCreate() {
        super.onCreate();
	}

	@Override
	public Engine onCreateEngine() {
		// TODO Auto-generated method stub
		return new FlareEngine();
	}

	@Override
    public void onDestroy() {
		// Make sure our notification is gone.
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @Override
    public void onStart(Intent intent, int startId) {

    }

	public int onStartCommand(Intent intent, int flags, int startId) {
		//	Tell the widgets to refresh themselves.

		// We want intents redelivered and onStartCommand re-executed if the service is killed.
		return 1;  // Service.START_STICKY ; have to use literal because Donut is unaware of the constant
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		if (HCLW.CURRENTORIENTATION == newConfig.orientation) {
			// Orientation has not changed!  Do nothing.
		} else {
			HCLW.CURRENTORIENTATION = newConfig.orientation;
			((HCLW)getApplication()).adjustOrientationOffsets();
		}
	}
	
    class FlareEngine extends Engine {

        public final Runnable mDrawFlare = new Runnable() {
            public void run() {
                drawFrame();
            }
        };

        // FLARE ENGINE. THIS IS WHERE THE WALLPAPER RENDERING OCCURS.
        //
        FlareEngine() {

        	HCLW.StartTime = SystemClock.elapsedRealtime();

        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            HCLW.HANDLER.removeCallbacks(mDrawFlare);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
        	HCLW.Visible = visible;
            if (visible) {
//            	Log.i("HCLW","Start animating");
                drawFrame();
            } else {
//            	Log.i("HCLW","Stop animating");
            	HCLW.HANDLER.removeCallbacks(mDrawFlare);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            // If no colors are enabled, enable all of them!
            boolean bAllColorsDisabled=true;
            for (int i =0; i<5; i++) {
            	if (HCLW.PREFS.getBoolean("showcolor"+i, false)) {
            		bAllColorsDisabled = false;
            		break;
            	}
            }
            if (bAllColorsDisabled) {
            	HCLW.PREFS.edit()
        		.putBoolean("showcolor0", true)
        		.putBoolean("showcolor1", true)
        		.putBoolean("showcolor2", true)
        		.putBoolean("showcolor3", true)
        		.putBoolean("showcolor4", true)
        		.commit();
            }
            
            // store the center of the surface
            HCLW.CenterX = width/2.0f;
            HCLW.CenterY = height/2.0f;
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            HCLW.Visible = false;
            HCLW.HANDLER.removeCallbacks(mDrawFlare);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            HCLW.xPixels = xPixels;
            drawFrame();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                HCLW.TouchX = event.getX();
                HCLW.TouchY = event.getY();
            } else {
                HCLW.TouchX = -1;
                HCLW.TouchY = -1;
                HCLW.LightningFactor=1f;
            }
            super.onTouchEvent(event);
        }

       
        /*
         * Draw one frame of the animation. This method gets called repeatedly
         * by posting a delayed Runnable. You can do any drawing you want in
         * here.
         */
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            if (HCLW.TRIALOVERTIME!=0l && System.currentTimeMillis()>HCLW.TRIALOVERTIME) {
            	HCLW.resetTheme();
            }
            
            //  Redraw
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                	HCLW.OFFSETTHISFRAME = HCLW.xPixels;

                	drawFlares(c, HCLW.OFFSETTHISFRAME);
                  drawTouchPoint(c, HCLW.OFFSETTHISFRAME);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            HCLW.HANDLER.removeCallbacks(mDrawFlare);
        	HCLW.HANDLER.postDelayed(mDrawFlare, 1000 / HCLW.FPS);

        }

         void drawFlares(final Canvas c, final int iOffset) {
        	 HCLW.srcFullRect.offsetTo(-iOffset,-HCLW.YOFFSET);
        	 HCLW.srcFlareRect.offsetTo(-iOffset/3,0);

        	// Draw the "Channels" on the bottom.
        	// Default to channel bkgd (white for Sparks).
    		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
    			c.drawColor(Color.parseColor("#FFACACAC"));
    			HCLW.BUFFER.eraseColor(Color.TRANSPARENT);
    		} else if (HCLW.PREFS.getBoolean("Searchlight", false)) { 
    			HCLW.BUFFERCANVAS.drawColor(Color.parseColor("#441b1939"));
    		} else {

        		//Trail Length is an optical illusion actually driven by
    			//The opacity of each frame's screen erase
    			try {
    				HCLW.BUFFERCANVAS.drawColor(Color.parseColor(HCLW.PREFS.getString("TrailLength", "#051b1939")));
    			} catch (Exception e) {
    				HCLW.BUFFERCANVAS.drawColor(Color.parseColor("#051b1939"));
    			}
    		}
        	
        	// if Flares are to be above surface, draw the "Surface" now (and skip the "middle" mask).
        	if (HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
        		c.drawBitmap(HCLW.FG, HCLW.srcFullRect, HCLW.tgtFullRect, HCLW.PaintFg);
        	}
        	
        	// We're tracking each flare.
        	for (int i = 0; i < (HCLW.PREFS.getBoolean("Searchlight", false)?HCLW.DISPLACEMENTS.length:HCLW.DISPLACEMENTS.length-1); i++) {
        		// If a flare is done, reset. (the 1.1 is to make sure the flare goes offscreen first)
        		if (HCLW.DISPLACEMENTS[i]>1.1f) {
        			HCLW.DISPLACEMENTS[i]=0f;
        			HCLW.COLORS[i]=-1;
        		} else if (HCLW.DISPLACEMENTS[i]==0f) {
        			//Only relaunch a flare 1% of the time by default (can be customized)
        			if (Math.random() < (i==HCLW.DISPLACEMENTS.length-1?0.02d:0.01d * Double.parseDouble(HCLW.PREFS.getString("FlareFrequency", "1")))) {
    	        		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
    	        			HCLW.FLARESPEEDS[i]= (float)(HCLW.MINFLARESPEEDS[i]*(1+Math.random()));
    	        		} else {
    	        			HCLW.FLARESPEEDS[i]= (float)(HCLW.MINFLARESPEEDS[i]*(1+Math.random()));
    	        		}
    	        		HCLW.DISPLACEMENTS[i]+=HCLW.FLARESPEEDS[i];

    	        		//Slight acceleration.
    	        		HCLW.FLARESPEEDS[i]+=HCLW.FLAREACCEL[i]; 
    	        		
    	        		// Pick a color for each flare.
            			do {
            				HCLW.COLORS[i]=(int)(Math.random()*5.);
            			} while 
            				(
        						// If 
            					(HCLW.NUMBEROFFLARECOLORS>2 && i>0 && i<HCLW.DISPLACEMENTS.length-1 && (HCLW.COLORS[i]==HCLW.COLORS[i-1] || HCLW.COLORS[i]==HCLW.COLORS[i+1])) 
            					|| !HCLW.PREFS.getBoolean("showcolor"+HCLW.COLORS[i], true) 
            				);
        			}
        		} else {
        			HCLW.DISPLACEMENTS[i]+=HCLW.FLARESPEEDS[i];
        		}

        		//Flares
        		
        		//Render each flare
    			// If the flare head/tail will be offscreen, skip drawing that part
    			if (HCLW.DISPLACEMENTS[i]<0) continue;
    			if (HCLW.DISPLACEMENTS[i]>1) continue;

    			// For Spark Effect, we want the sparks to sparkle on the horizon;
    			// For flares/trails, we don't want the flares sitting around
        		if (HCLW.DISPLACEMENTS[i]==0) continue;

        			
    			HCLW.TEMPMATRIX.reset();
        		float xPos = floatInterpolate(HCLW.FLAREPATHINITX[i],HCLW.FLAREPATHMIDX[i],HCLW.FLAREPATHFINALX[i],HCLW.DISPLACEMENTS[i]) * HCLW.SCALEX;
        		float yPos = floatInterpolate(HCLW.FLAREPATHINITY[i],HCLW.FLAREPATHMIDY[i],HCLW.FLAREPATHFINALY[i],HCLW.DISPLACEMENTS[i]) * HCLW.SCALEY;
        		float zFactor;
        		//Sparks are white; trails are multicolored
        		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
        			zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i], HCLW.FLAREPATHMIDZ[i], 
        					HCLW.FLAREPATHFINALZ[i], HCLW.DISPLACEMENTS[i]) 
        					* (.5f + (float)(.5d*Math.random()));
            		HCLW.TEMPMATRIX.postScale(zFactor*HCLW.SCALEX*3, zFactor*HCLW.SCALEY*3);
            		HCLW.TEMPMATRIX.postTranslate(xPos-HCLW.FLARE[0].getWidth()/2f*zFactor*HCLW.SCALEX*3, 
            				yPos-HCLW.SCRNLONGEREDGELENGTH/2/3-HCLW.FLARE[HCLW.COLORS[i]].getHeight()/2f*zFactor*HCLW.SCALEY*3);

            		HCLW.BUFFERCANVAS.drawBitmap(HCLW.FLARE[0], HCLW.TEMPMATRIX, HCLW.PaintFlare);
        		} else {
        			zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i], HCLW.FLAREPATHMIDZ[i], 
        					HCLW.FLAREPATHFINALZ[i], HCLW.DISPLACEMENTS[i]);
            		HCLW.TEMPMATRIX.postScale(zFactor*HCLW.SCALEX, zFactor*HCLW.SCALEY);
            		HCLW.TEMPMATRIX.postTranslate(xPos-HCLW.FLARE[HCLW.COLORS[i]].getWidth()/2f*zFactor*HCLW.SCALEX, 
            				yPos-HCLW.SCRNLONGEREDGELENGTH/2/3-HCLW.FLARE[HCLW.COLORS[i]].getHeight()/2f*zFactor*HCLW.SCALEY);

            		HCLW.BUFFERCANVAS.drawBitmap(HCLW.FLARE[HCLW.COLORS[i]], HCLW.TEMPMATRIX, HCLW.PaintFlare);
        		}
    		}
        	c.drawBitmap(HCLW.BUFFER, HCLW.srcFlareRect, HCLW.tgtFlareRect, HCLW.PaintMid);
        	
        	if (HCLW.PREFS.getBoolean("LightningEffect", false)) {
        		if (Math.random()<0.05d) {
        			HCLW.LightningFactor=1f;
        		} else if (HCLW.LightningFactor<=0f) {
        			HCLW.LightningFactor=0f;
        		} else {
        			HCLW.LightningFactor-=0.05f;
        		}
    			HCLW.PaintFg.setAlpha((int)(255f*HCLW.LightningFactor));
        	} else {
        		HCLW.PaintFg.setAlpha(255);
        	}
           	// Draw the  "Middle" mask, then the "Surface".
        	if (!HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
        		if (HCLW.PaintFg.getAlpha()<255) {
        			c.drawBitmap(HCLW.MIDDLE, HCLW.srcFullRect, HCLW.tgtFullRect, HCLW.PaintMid);
        		}
        		if (HCLW.LightningFactor>0f) {
//        			HCLW.PaintFg.setAlpha(60);
        			c.drawBitmap(HCLW.FG, HCLW.srcFullRect, HCLW.tgtFullRect, HCLW.PaintFg);
        		}
        	}

        }

    	public float floatInterpolate (float n1, float n2, float n3, float gradient) {
    		if (gradient > 0.5f) return (n2+ (n3-n2)*(gradient-0.5f) * 2);
    		else return (n1 + (n2-n1) * gradient * 2);
    	}

        /*
         * Draw a flare around the current touch point.
         */
        void drawTouchPoint(final Canvas c, final int iOffset) {
        	if (System.currentTimeMillis()<HCLW.IGNORETOUCHUNTIL) return;
//            if (HCLW.TouchX >=0 && HCLW.TouchY >= 0) {
//            	HCLW.TEMPMATRIX2.reset();
//            	float zFactor = ((float)Math.random()*0.5f + 0.5f);
//            	HCLW.TEMPMATRIX2.postScale(zFactor, zFactor);
//            	HCLW.TEMPMATRIX2.postTranslate(HCLW.TouchX-HCLW.FLARE[0].getWidth()/2f*zFactor, 
//            			HCLW.TouchY-HCLW.FLARE[0].getHeight()/2f*zFactor);
//        		c.drawBitmap(HCLW.FLARE[0], HCLW.TEMPMATRIX2, HCLW.PaintFlare);
//            }
            HCLW.IGNORETOUCHUNTIL=System.currentTimeMillis()+50;
        }

    }
}
