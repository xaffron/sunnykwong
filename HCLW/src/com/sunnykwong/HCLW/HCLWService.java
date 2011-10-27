package com.sunnykwong.HCLW;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.ListView.FixedViewInfo;
import android.widget.Toast;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
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
import android.graphics.Region.Op;
import android.util.DisplayMetrics;

public class HCLWService extends WallpaperService {

	// Code for oncreate/ondestroy.
	// Code stolen wholesale from api samples:
	// http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/ForegroundService.html
	//
	// When service is created,
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
	// platform. On 2.0 or later we override onStartCommand() so this
	// method will not be called.
	@Override
	public void onStart(Intent intent, int startId) {

	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		// Tell the widgets to refresh themselves.

		// We want intents redelivered and onStartCommand re-executed if the
		// service is killed.
		return 1; // Service.START_STICKY ; have to use literal because Donut is
					// unaware of the constant
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		if (HCLW.CURRENTORIENTATION == newConfig.orientation) {
			// Orientation has not changed! Do something just in case.
			((HCLW) getApplication()).adjustOrientationOffsets();
			HCLW.FIRSTDRAW = true;
		} else {
			HCLW.CURRENTORIENTATION = newConfig.orientation;
			((HCLW) getApplication()).adjustOrientationOffsets();
			HCLW.FIRSTDRAW = true;
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
			// ASSUMPTION: If phone is HDPI or above, it has enough horsepower
			// to draw 32-bit
			// if (HCLW.LWPHEIGHT>480/HCLW.SCREENSCALEFACTOR) {
			// surfaceHolder.setFormat(PixelFormat.RGBA_8888);
			// } else {
			surfaceHolder.setFormat(PixelFormat.RGB_565);
			HCLW.FIRSTDRAW = true;
			// }
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
				// Log.i("HCLW","Start animating");
				HCLW.FIRSTDRAW = true;
				drawFrame();
			} else {
				// Log.i("HCLW","Stop animating");
				HCLW.FIRSTDRAW = true;
				HCLW.HANDLER.removeCallbacks(mDrawFlare);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			// If no colors are enabled, enable all of them!
			boolean bAllColorsDisabled = true;
			HCLW.FIRSTDRAW = true;
			for (int i = 0; i < 5; i++) {
				if (HCLW.PREFS.getBoolean("showcolor" + i, false)) {
					bAllColorsDisabled = false;
					break;
				}
			}
			if (bAllColorsDisabled) {
				HCLW.PREFS.edit().putBoolean("showcolor0", true)
						.putBoolean("showcolor1", true)
						.putBoolean("showcolor2", true)
						.putBoolean("showcolor3", true)
						.putBoolean("showcolor4", true).commit();
			}

			// store the center of the surface
			HCLW.CenterX = width / 2.0f;
			HCLW.CenterY = height / 2.0f;
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
		public void onOffsetsChanged(float xOffset, float yOffset, float xStep,
				float yStep, int xPixels, int yPixels) {
			HCLW.xPixels = xPixels;
			drawFrame();
		}

		/*
		 * Store the position of the touch event so we can use it for drawing
		 * later
		 */
		@Override
		public void onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE) {
				HCLW.TouchX = event.getX();
				HCLW.TouchY = event.getY();
			} else {
				HCLW.TouchX = -1;
				HCLW.TouchY = -1;
				if (HCLW.LIGHTNINGEFFECT) {
					HCLW.LightningFactor = 1f;
				} else {
					HCLW.LightningFactor = 0f;
				}
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

			if (HCLW.TRIALOVERTIME != 0l
					&& System.currentTimeMillis() > HCLW.TRIALOVERTIME) {
				HCLW.resetTheme();
			}

			// Reschedule the next redraw
			// If swiping, render at lower framerate
			boolean bFullRender = true;
			if (!HCLW.RENDERWHILESWIPING
					&& Math.abs(HCLW.OFFSETTHISFRAME - HCLW.xPixels) > HCLW.SCRNHEIGHT / 10) {
				bFullRender = false;
			} else {
			}
			if (HCLW.FIXEDOFFSET != -1) {
				HCLW.OFFSETTHISFRAME = -HCLW.FIXEDOFFSET;
			} else {
				if (HCLW.xPixels!=HCLW.OFFSETTHISFRAME) HCLW.FIRSTDRAW=true;

				if (Math.abs(HCLW.OFFSETTHISFRAME - HCLW.xPixels) <= 4) {
					HCLW.OFFSETTHISFRAME = HCLW.xPixels;
					HCLW.FIRSTDRAW=true;
				} else
					HCLW.OFFSETTHISFRAME = HCLW.OFFSETTHISFRAME
							+ (HCLW.xPixels - HCLW.OFFSETTHISFRAME - 1) / 4;
			}
			// Redraw
			try {
				Canvas c = holder.lockCanvas();

				if (c != null) {
					// draw something
					if (HCLW.FIRSTDRAW) {
						HCLW.FULLDRAW = true;
						c.clipRect(HCLW.FULLSCRNCLIP, Op.REPLACE);
						HCLW.FIRSTDRAW = false;
					} else {
						HCLW.FULLDRAW = false;
						c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
					}

					HCLW.TEMPMATRIX2.reset();
					HCLW.TEMPMATRIX2.postScale(HCLW.SCREENSCALEFACTOR,
							HCLW.SCREENSCALEFACTOR);
					HCLW.TEMPMATRIX2.postTranslate(HCLW.OFFSETTHISFRAME,
							HCLW.YOFFSET);

					if (!bFullRender) {
						HCLW.PaintFg.setAntiAlias(false);
						HCLW.PaintFg.setDither(false);
						HCLW.PaintFg.setFilterBitmap(false);
						c.drawBitmap(HCLW.FG, HCLW.TEMPMATRIX2, HCLW.PaintFg);
						HCLW.PaintFg.setDither(true);
						HCLW.PaintFg.setFilterBitmap(true);
					}
					// Draw the "Channels" on the bottom.
					// Default to channel bkgd (white for Sparks).
					if (HCLW.SPARKEFFECT) {
						c.drawColor(HCLW.SPARKEFFECTCOLOR);
						HCLW.BUFFER.eraseColor(Color.TRANSPARENT);
					} else if (HCLW.SPARKEFFECT) {
						HCLW.BUFFERCANVAS
								.drawColor(HCLW.SEARCHLIGHTEFFECTCOLOR);
					} else {
						HCLW.BUFFERCANVAS.drawColor(HCLW.DEFAULTEFFECTCOLOR);
					}

					// if Flares are to be above surface, draw the "Surface" now
					// (and skip the "middle" mask).
					if (HCLW.FLARESABOVESURFACE) {
						c.drawBitmap(HCLW.FG, HCLW.TEMPMATRIX2, HCLW.PaintFg);
					}

					// We're tracking each flare.
					final int iLength = (HCLW.SEARCHLIGHTEFFECT ? HCLW.DISPLACEMENTS.length
							: HCLW.DISPLACEMENTS.length - 1);
					for (int i = 0; i < iLength; i++) {
						// If a flare is done, reset. (the 1.1 is to make sure
						// the flare goes offscreen first)
						if (HCLW.DISPLACEMENTS[i] > 1.1f) {
							HCLW.DISPLACEMENTS[i] = 0f;
							HCLW.COLORS[i] = -1;
						} else if (HCLW.DISPLACEMENTS[i] == 0f) {
							// Only relaunch a flare 1% of the time by default
							// (can be customized)

							if (Math.random() < (i == HCLW.DISPLACEMENTS.length - 1 ? 0.02d
									: HCLW.FLAREFREQ)) {
								if (HCLW.SPARKEFFECT) {
									HCLW.FLARESPEEDS[i] = (float) (HCLW.MINFLARESPEEDS[i] * (1 + Math
											.random()));
								} else {
									HCLW.FLARESPEEDS[i] = (float) (HCLW.MINFLARESPEEDS[i] * (1 + Math
											.random()));
								}
								HCLW.DISPLACEMENTS[i] += HCLW.FLARESPEEDS[i];

								// Slight acceleration.
								HCLW.FLARESPEEDS[i] += HCLW.FLAREACCEL[i];

								// Pick a color for each flare.
								do {
									HCLW.COLORS[i] = (int) (Math.random() * 5.);
								} while (
								// If
								(HCLW.NUMBEROFFLARECOLORS > 2 && i > 0
										&& i < HCLW.DISPLACEMENTS.length - 1 && (HCLW.COLORS[i] == HCLW.COLORS[i - 1] || HCLW.COLORS[i] == HCLW.COLORS[i + 1]))
										|| !HCLW.SHOWCOLOR[HCLW.COLORS[i]]);
							}
						} else {
							HCLW.DISPLACEMENTS[i] += HCLW.FLARESPEEDS[i];
						}

						// Flares

						// Render each flare
						// If the flare head/tail will be offscreen, skip
						// drawing that part
						if (HCLW.DISPLACEMENTS[i] < 0)
							continue;
						if (HCLW.DISPLACEMENTS[i] > 1)
							continue;

						// For Spark Effect, we want the sparks to sparkle on
						// the horizon;
						// For flares/trails, we don't want the flares sitting
						// around
						if (HCLW.DISPLACEMENTS[i] == 0)
							continue;

						// Position each flare in the 640x480 space.
						float xPos, yPos, zFactor;
						if (HCLW.DISPLACEMENTS[i] > 0.5f) {
							xPos = (HCLW.FLAREPATHMIDX[i] + (HCLW.FLAREPATHFINALX[i] - HCLW.FLAREPATHMIDX[i])
									* (HCLW.DISPLACEMENTS[i] - 0.5f) * 2f);
							yPos = (HCLW.FLAREPATHMIDY[i] + (HCLW.FLAREPATHFINALY[i] - HCLW.FLAREPATHMIDY[i])
									* (HCLW.DISPLACEMENTS[i] - 0.5f) * 2f);
							zFactor = (HCLW.FLAREPATHMIDZ[i] + (HCLW.FLAREPATHFINALZ[i] - HCLW.FLAREPATHMIDZ[i])
									* (HCLW.DISPLACEMENTS[i] - 0.5f) * 2f);
						} else {
							xPos = (HCLW.FLAREPATHINITX[i] + (HCLW.FLAREPATHMIDX[i] - HCLW.FLAREPATHINITX[i])
									* HCLW.DISPLACEMENTS[i] * 2f);
							yPos = (HCLW.FLAREPATHINITY[i] + (HCLW.FLAREPATHMIDY[i] - HCLW.FLAREPATHINITY[i])
									* HCLW.DISPLACEMENTS[i] * 2f);
							zFactor = (HCLW.FLAREPATHINITZ[i] + (HCLW.FLAREPATHMIDZ[i] - HCLW.FLAREPATHINITZ[i])
									* HCLW.DISPLACEMENTS[i] * 2f);
						}
						// Sparks are white; trails are multicolored
						if (HCLW.SPARKEFFECT) {
							float sparkHalfWidth, sparkHalfHeight;
							zFactor *= (.5f + (float) (.5d * Math.random())) * 1.5f;
							sparkHalfWidth = -HCLW.FLARE[0].getWidth()
									* zFactor * 0.5f;
							sparkHalfHeight = -HCLW.FLARE[0].getHeight()
									* zFactor * 0.5f;
							HCLW.TEMPMATRIX.reset();
							HCLW.TEMPMATRIX.postScale(zFactor, zFactor);
							HCLW.TEMPMATRIX.postTranslate(sparkHalfWidth,
									sparkHalfHeight);
							HCLW.TEMPMATRIX.postTranslate(xPos, yPos - 240);
							HCLW.BUFFERCANVAS.drawBitmap(HCLW.FLARE[0],
									HCLW.TEMPMATRIX, HCLW.PaintFlare);
						} else {
							float sparkHalfWidth, sparkHalfHeight;
							sparkHalfWidth = -HCLW.FLARE[HCLW.COLORS[i]]
									.getWidth() * zFactor * 0.5f;
							sparkHalfHeight = -HCLW.FLARE[HCLW.COLORS[i]]
									.getHeight() * zFactor * 0.5f;
							HCLW.TEMPMATRIX.reset();
							HCLW.TEMPMATRIX.postScale(zFactor, zFactor);
							HCLW.TEMPMATRIX.postTranslate(sparkHalfWidth,
									sparkHalfHeight);
							HCLW.TEMPMATRIX.postTranslate(xPos, yPos - 240);
							HCLW.BUFFERCANVAS.drawBitmap(
									HCLW.FLARE[HCLW.COLORS[i]],
									HCLW.TEMPMATRIX, HCLW.PaintFlare);
						}
					}

					HCLW.TEMPMATRIX.reset();
					HCLW.TEMPMATRIX.postTranslate(0, 240);
					HCLW.TEMPMATRIX.postScale(HCLW.LWPWIDTH / 640f,
							HCLW.LWPHEIGHT / 480f);
					HCLW.TEMPMATRIX.postScale(HCLW.SCREENSCALEFACTOR,
							HCLW.SCREENSCALEFACTOR);
					HCLW.TEMPMATRIX.postTranslate(HCLW.OFFSETTHISFRAME,
							HCLW.YOFFSET);
					// HCLW.PaintMid.setXfermode(new
					// PorterDuffXfermode(Mode.SRC_ATOP));
					c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
					// c.drawBitmap(HCLW.FG, HCLW.TEMPMATRIX2, HCLW.PaintFg);
					c.drawBitmap(HCLW.BUFFER, HCLW.TEMPMATRIX, HCLW.PaintMid);
					if (HCLW.FULLDRAW) {
						c.clipRect(HCLW.FULLSCRNCLIP, Op.REPLACE);
					} else {
						c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
					}

					if (HCLW.LIGHTNINGEFFECT) {
						// If we are in lightning mode...
						if (Math.random() < HCLW.LIGHTNFREQUENCY) {
							// Start lightning
							HCLW.LightningFactor = 1f;
							HCLW.PaintFg
									.setAlpha((int) (255f * HCLW.LightningFactor));
						} else if (HCLW.LightningFactor <= 0f) {
							// If lightning is over, unset lightning
							HCLW.LightningFactor = 0f;
							HCLW.PaintFg
									.setAlpha((int) (255f * HCLW.LightningFactor));
						} else {
							// During lightning...
							// Decrement intensity.
							HCLW.LightningFactor -= 0.05f;
							HCLW.PaintFg
									.setAlpha((int) (255f * HCLW.LightningFactor));
							// Draw the full screen.
							HCLW.FULLDRAW = true;
						}
					}
				} else {
					HCLW.PaintFg
							.setAlpha((int) (255f * HCLW.DEFAULTBRIGHTNESS / 100f));
				}
				// Draw the "Middle" mask, then the "Surface".
				if (!HCLW.FLARESABOVESURFACE) {
					// If we're in the middle of lightning, alpha is less than 255
					// So we need a solid mask in the middle
					if (HCLW.PaintFg.getAlpha() < 255) {
						if (HCLW.FULLDRAW) {
							c.clipRect(HCLW.FULLSCRNCLIP, Op.REPLACE);
						} else {
							c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
						}
						c.drawBitmap(HCLW.MIDDLE, HCLW.TEMPMATRIX2,
								HCLW.PaintMid);
					}
					// If we're in the middle of lightning...
					if (HCLW.LightningFactor > 0f) {
						if (HCLW.LIGHTNINGEFFECT || HCLW.FULLDRAW) { 
							c.clipRect(HCLW.FULLSCRNCLIP, Op.REPLACE);
						} else {
							c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
						}

						Canvas cp = HCLW.PICTURE.beginRecording(HCLW.LWPWIDTH*HCLW.SCREENSCALEFACTOR,HCLW.LWPHEIGHT*HCLW.SCREENSCALEFACTOR);
						cp.drawBitmap(HCLW.FG, HCLW.TEMPMATRIX2, HCLW.PaintFg);
						HCLW.PICTURE.endRecording();
						c.drawPicture(HCLW.PICTURE);
						
					// If we're not in the middle of lightning, we can do regular scrolling
					} else {
						if (HCLW.FULLDRAW) { 
							c.clipRect(HCLW.FULLSCRNCLIP, Op.REPLACE);
						} else {
							c.clipRect(HCLW.SCRNCLIP, Op.REPLACE);
						}
						c.drawBitmap(HCLW.FG, HCLW.TEMPMATRIX2, HCLW.PaintFg);
					}
					// HCLW.PaintFg.setColorFilter(null);
				}

				HCLW.RENDERTIME = (System.currentTimeMillis() - HCLW.LASTUPDATEMILLIS);
				HCLW.fFPS = HCLW.fFPS * 0.9f + (100f / HCLW.RENDERTIME);
				if (HCLW.fFPS < 24f)
					HCLW.PaintFg.setColor(Color.RED);
				else
					HCLW.PaintFg.setColor(Color.WHITE);

				if (HCLW.DEBUG) {

					// 90% weighted to history, 10% weighted to last frame
					c.drawText("FPS: " + HCLW.fFPS, HCLW.LWPBORDER,
							HCLW.SCRNHEIGHT * HCLW.SCREENSCALEFACTOR - 150,
							HCLW.PaintFg);
					c.drawText("YOffset: " + HCLW.YOFFSET, HCLW.LWPBORDER,
							HCLW.SCRNHEIGHT * HCLW.SCREENSCALEFACTOR - 130,
							HCLW.PaintFg);
					c.drawText("SCRN: " + HCLW.SCRNWIDTH + " x "
							+ HCLW.SCRNHEIGHT, HCLW.LWPBORDER, HCLW.SCRNHEIGHT
							* HCLW.SCREENSCALEFACTOR - 110, HCLW.PaintFg);
					c.drawText("NextUpd: "
							+ (1000l / HCLW.FPS - HCLW.RENDERTIME),
							HCLW.LWPBORDER, HCLW.SCRNHEIGHT
									* HCLW.SCREENSCALEFACTOR - 90, HCLW.PaintFg);
				}

				holder.unlockCanvasAndPost(c);

				HCLW.LASTUPDATEMILLIS = System.currentTimeMillis();
				HCLW.HANDLER.removeCallbacks(mDrawFlare);
				HCLW.HANDLER.postDelayed(mDrawFlare, 1000l / HCLW.FPS
						- HCLW.RENDERTIME);

			} finally {
			}

		}

		/*
		 * Easter Egg fun.
		 */
		void easteregg() {
			if (HCLW.PREFS.getBoolean("Egg", false))
				return;
			if (HCLW.BONUSPHRASE.length() > 15)
				HCLW.BONUSPHRASE = "";
			if (System.currentTimeMillis() < HCLW.IGNORETOUCHUNTIL)
				return;
			switch ((int) HCLW.UpY * 5 / HCLW.SCRNHEIGHT) {
			case 0:
				switch ((int) HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
				case 0:
					Log.i("HCLW", "Reset");
					HCLW.BONUSPHRASE = "";
					break;
				case 2:
					Log.i("HCLW", "E");
					HCLW.BONUSPHRASE += "E";
					break;
				case 4:
					Log.i("HCLW", "A");
					HCLW.BONUSPHRASE += "A";
					break;
				default:
				}
				break;
			case 2:
				switch ((int) HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
				case 0:
					Log.i("HCLW", "S");
					HCLW.BONUSPHRASE += "S";
					break;
				case 2:
					Log.i("HCLW", "T");
					HCLW.BONUSPHRASE += "T";
					break;
				case 4:
					Log.i("HCLW", "R");
					HCLW.BONUSPHRASE += "R";
					break;
				default:
				}
				break;
			case 4:
				switch ((int) HCLW.UpX * 5 / HCLW.SCRNWIDTH) {
				case 0:
					Log.i("HCLW", "G");
					HCLW.BONUSPHRASE += "G";
					break;
				case 2:
					Log.i("HCLW", "<space>");
					HCLW.BONUSPHRASE += " ";
					break;
				case 4:
					Log.i("HCLW", "C");
					HCLW.BONUSPHRASE += "C";
					break;
				default:
				}
				break;
			default:
			}
			if (HCLW.BONUSPHRASE.equals("EASTER EGG")) {
				HCLW.BONUSPHRASE = "";
				HCLW.PREFS.edit().putBoolean("Egg", true).commit();
				Intent it = new Intent(getApplicationContext(),
						HCLWPrefsActivity.class);
				android.app.PendingIntent pi = PendingIntent.getActivity(
						getApplicationContext(), 0, it,
						Intent.FLAG_ACTIVITY_NEW_TASK);

				android.app.Notification note = new android.app.Notification(
						R.drawable.icon, "HCLW Easter Egg Activated!",
						System.currentTimeMillis());
				note.flags = note.flags
						| android.app.Notification.FLAG_AUTO_CANCEL;

				note.setLatestEventInfo(getApplicationContext(),
						"Honeycomb Live Wallpaper",
						"Tap to download Easter Egg!", pi);

				android.app.NotificationManager mNotificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
				mNotificationManager.notify(0, note);
			}
			HCLW.IGNORETOUCHUNTIL = System.currentTimeMillis() + 50;
		}

	}
}
