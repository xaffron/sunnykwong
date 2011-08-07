package com.sunnykwong.HCLW;

import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.content.res.Configuration;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;

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

	@Override
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
            @Override
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
            // ASSUMPTION:  If phone is HDPI or above, it has enough horsepower to draw 32-bit
            if (HCLW.LWPHEIGHT>480) {
            	surfaceHolder.setFormat(PixelFormat.RGBA_8888);
            } else {
            	surfaceHolder.setFormat(PixelFormat.OPAQUE);
            }
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
        	HCLW.targetXPixels = xPixels;
//            HCLW.xPixels = xPixels;
//            drawFrame();
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
            if (event.getAction() == MotionEvent.ACTION_UP) {
            	HCLW.UpX = event.getX();
            	HCLW.UpY = event.getY();
            	easteregg();
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

//    		HCLW.RENDERWHILESWIPING = HCLW.PREFS.getBoolean("RenderWhileSwiping", true);

            if (HCLW.TRIALOVERTIME!=0l && System.currentTimeMillis()>HCLW.TRIALOVERTIME) {
            	HCLW.resetTheme();
            }

            // Reschedule the next redraw
            // If swiping, render at lower framerate
            boolean bFullRender = true;
            if (!HCLW.RENDERWHILESWIPING && Math.abs(HCLW.OFFSETTHISFRAME - HCLW.xPixels) > HCLW.SCRNHEIGHT/20 ) {
            	bFullRender=false;
            } else {
            }
            if (HCLW.FIXEDOFFSET!=-1) {
            	HCLW.OFFSETTHISFRAME=-HCLW.FIXEDOFFSET;
            } else {
            	if (HCLW.SLOWPAN) {
	            	if (Math.abs(HCLW.targetXPixels-HCLW.xPixels)<HCLW.SLOWPANSPEED) HCLW.xPixels = HCLW.targetXPixels;
	            	else HCLW.xPixels+=(HCLW.targetXPixels-HCLW.xPixels>0?HCLW.SLOWPANSPEED:-HCLW.SLOWPANSPEED);
            	} else {
            		HCLW.xPixels = HCLW.targetXPixels;
            	}
            	HCLW.OFFSETTHISFRAME = HCLW.xPixels;
            }
            //  Redraw
            Canvas c = null;
            try {
            	c = holder.lockCanvas();
            	Bitmap bmp = HCLWEngine.drawFlares(HCLW.OFFSETTHISFRAME, true);
            	if (c != null) {
            		// draw something
            		c.drawBitmap(bmp,0,0,HCLW.PaintBuf);
            	//	c.drawBitmap(HCLW.FG, HCLW.OFFSETTHISFRAME,0,HCLW.PaintBuf);
            	}
            } finally {
            	if (c != null) holder.unlockCanvasAndPost(c);
            }

            if (bFullRender) {
            	HCLW.HANDLER.removeCallbacks(mDrawFlare);
            	HCLW.HANDLER.postDelayed(mDrawFlare, 1000 / HCLW.FPS);
            } else {
            	HCLW.HANDLER.removeCallbacks(mDrawFlare);
            	HCLW.HANDLER.postDelayed(mDrawFlare, 3000 / HCLW.FPS);
            }
        }

        /*
         * Easter Egg fun.
         */
        void easteregg() {
        	if (HCLW.PREFS.getBoolean("Egg", false)) return;
        	if (HCLW.BONUSPHRASE.length()>15) HCLW.BONUSPHRASE="";
        	if (System.currentTimeMillis()<HCLW.IGNORETOUCHUNTIL) return;
        	switch ((int)HCLW.UpY * 5 / HCLW.SCRNHEIGHT) {
        		case 0:
        			switch ((int)HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
        			case 0:
        				Log.i("HCLW","Reset");
        				HCLW.BONUSPHRASE="";
        				break;
        			case 2:
        				Log.i("HCLW","E");
        				HCLW.BONUSPHRASE+="E";
        				break;
        			case 4:
        				Log.i("HCLW","A");
        				HCLW.BONUSPHRASE+="A";
        				break;
        			default:	
        			}
        			break;
        		case 2:
        			switch ((int)HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
        			case 0:
        				Log.i("HCLW","S");
        				HCLW.BONUSPHRASE+="S";
        				break;
        			case 2:
        				Log.i("HCLW","T");
        				HCLW.BONUSPHRASE+="T";
        				break;
        			case 4:
        				Log.i("HCLW","R");
        				HCLW.BONUSPHRASE+="R";
        				break;
        			default:	
        			}
        			break;
        		case 4:
        			switch ((int)HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
        			case 0:
        				Log.i("HCLW","G");
        				HCLW.BONUSPHRASE+="G";
        				break;
        			case 2:
        				Log.i("HCLW","<space>");
        				HCLW.BONUSPHRASE+=" ";
        				break;
        			case 4:
        				Log.i("HCLW","C");
        				HCLW.BONUSPHRASE+="C";
        				break;
        			default:	
        			}
        			break;
        		default:
        	}
        	if (HCLW.BONUSPHRASE.equals("EASTER EGG")) {
        		HCLW.BONUSPHRASE="";
    			HCLW.PREFS.edit().putBoolean("Egg", true).commit();
	    		Intent it = new Intent(getApplicationContext(), HCLWPrefsActivity.class);
	    		android.app.PendingIntent pi = PendingIntent.getActivity(
	    				getApplicationContext(), 
	    				0, 
	    				it, 
	    				Intent.FLAG_ACTIVITY_NEW_TASK) ;
	    		
	    		android.app.Notification note =  new android.app.Notification(
	    				getResources().getIdentifier("icon", "drawable", HCLW.PKGNAME),
        				"HCLW Easter Egg Activated!",
        				System.currentTimeMillis()
        				);
	    		note.flags = note.flags|android.app.Notification.FLAG_AUTO_CANCEL;

	    		note.setLatestEventInfo(getApplicationContext(), 
	    				"Honeycomb Live Wallpaper", 
	    				"Tap to download Easter Egg!", 
	    				pi);

	    		android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        		mNotificationManager.notify(0,note);
        	}
            HCLW.IGNORETOUCHUNTIL=System.currentTimeMillis()+50;
        }

    }
}
