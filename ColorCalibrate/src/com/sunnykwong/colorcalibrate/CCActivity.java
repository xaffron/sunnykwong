package com.sunnykwong.colorcalibrate;
import android.content.pm.ActivityInfo;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import java.util.Iterator;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.View;
import android.graphics.Paint;
import android.graphics.Path;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

public class CCActivity extends Activity implements View.OnClickListener {
	static final String COLORDATASRC = "{\"NW\":255,\"N\":255,\"NE\":255,\"W\":255,\"C\":255,\"E\":255,\"SW\":255,\"S\":255,\"SE\":255}";
	static JSONObject COLORDATA;
	static final String[] COMPASSPOINTS = {"NW","N","NE","W","C","E","SW","S","SE"};
	static String[] MARKEDTEXT = {"","","","","","","","",""};
	
	static final int PREVIEWWIDTH = 176;
	static final int PREVIEWHEIGHT = 144;
	
	static final Intent CAMINTENT = new Intent("android.media.action.IMAGE_CAPTURE");
	Handler mHandler;
    TextView resultText;
    SurfaceView mSurfacePreview;
    ImageView resultImage;
    Bitmap colorChip, photo;
    Canvas colorChipCanvas;
    Camera mCam;
    ToggleButton markButton;
    
    YUVDecodeThread mThread;

    // mLatestPreviewRGB always contains the latest preview buffer
    // as decoded by decodeYUV
    static final int[] LatestPreviewRGB = new int[PREVIEWWIDTH*PREVIEWHEIGHT];

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        
        colorChip = Bitmap.createBitmap(30,30,Bitmap.Config.ARGB_4444);
        colorChipCanvas = new Canvas(colorChip);
        
        //Restore last COLORDATA
        String sTemp = this.getPreferences(Activity.MODE_PRIVATE).getString("JSONData", "EMPTY");
        try {
	        if (sTemp.equals("EMPTY")) { 
		        // Initialize our JSON; this should never error out
		        	COLORDATA = new JSONObject();
		        	COLORDATA.put("reference", new JSONObject(COLORDATASRC));
		        	COLORDATA.put("test", new JSONObject(COLORDATASRC));
	        } else {
	        	COLORDATA = new JSONObject(sTemp);
	        }
        } catch (JSONException e) {
        	Toast.makeText(this, "Unexpected Data Error!  Aborting.", Toast.LENGTH_LONG).show();
        	finish();
        }
        
        //Hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.main);

        // mSurfaceCallback is defined below - the bulk of processing code is there.
        
        mSurfacePreview = (SurfaceView)findViewById(R.id.surfacepreview);
        mSurfacePreview.getHolder().setSizeFromLayout();
        mSurfacePreview.getHolder().addCallback(mSurfaceCallback);
        mSurfacePreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        resultImage = (ImageView)findViewById(R.id.overlay);
        
        markButton = (ToggleButton)findViewById(R.id.Mark);
        markButton.setOnClickListener(this);
    }
    
	//  The Surfaceholder callback code governs the camera overlay.
	//  The camera initialization and release is also handled here.
	public SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {

		// What happens when surface is created
		public void surfaceCreated(SurfaceHolder holder) {
	        mCam=Camera.open();

	        try {
	        	mCam.setPreviewDisplay(mSurfacePreview.getHolder());
	        }
	        catch (Exception e) {
	                Log.e("PictureDemo-surfaceCallback",
	                                        "Exception in setPreviewDisplay()", e);
	                Toast.makeText(CCActivity.this, e.getMessage(), Toast.LENGTH_LONG)
	                        .show();
	        }
	    }
		
		// What happens when surface is ready to display
	    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	        Camera.Parameters camParams=mCam.getParameters();

	        // We are setting a low-res, low-fps preview
	        // because we only care about colors in nine test points.
	        camParams.setPreviewSize(PREVIEWWIDTH,PREVIEWHEIGHT);
	        camParams.setPreviewFrameRate(5);
	        camParams.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_DAYLIGHT);
	        camParams.setAntibanding(Camera.Parameters.ANTIBANDING_AUTO);
//	        camParams.setExposureCompensation(0);
	        
	        mCam.setParameters(camParams);
	        mCam.setPreviewCallback(mCamPreviewCallback);
	        mCam.startPreview();
	    }

	    // What happens when we shut down
	    public void surfaceDestroyed(SurfaceHolder holder) {
	    	mCam.stopPreview();
	    	mCam.release();
	    	mCam=null;
	    	System.out.println("cam released");
	    }
	};

    // The PreviewCallback is called whenever the camera generates a preview.
	// We are basically passing the YUV byte buffer to decodeYUV, but
	// since the proc is slow we have to spawn a thread to do this.
    Camera.PreviewCallback mCamPreviewCallback = new Camera.PreviewCallback() {
		
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			mThread = new YUVDecodeThread(data, camera);
			mThread.start();
		}
	};

	//  Here's the thread that gets called.  Mucho thanks to David Manpearl for his
	// DecodeYUV code - I'd be completely lost for weeks without it
    public class YUVDecodeThread extends Thread {
    	
    	byte[] mInputBytes;
    	Camera camera;
    	
    	public YUVDecodeThread (byte[] inputB, Camera cam) {
    		super();
    		mInputBytes = inputB;
    		camera = cam;
    	}
    	
    	@Override
    	public void run() {
    		CCActivity.decodeYUV(CCActivity.LatestPreviewRGB, mInputBytes, PREVIEWWIDTH, PREVIEWHEIGHT);
    		mHandler.post(mPostDecodeYUV);
    	}
    }
    
	//  Finally, when the thread is done decoding the YUV into RGB, we get our
    // grubby hands on the data and populate the overlay data.
    Runnable mPostDecodeYUV = new Runnable() {
		
		@Override
		public void run() {
			photo = Bitmap.createBitmap(CCActivity.LatestPreviewRGB, PREVIEWWIDTH, PREVIEWHEIGHT, Bitmap.Config.ARGB_4444);
	    	int width = photo.getWidth();
	    	int height = photo.getHeight();
	    	String buffer = "";
			System.out.println(height);
			int k=0;
	    	for (int j=height/4; 4*j<=3*height; j+=height/4) {
	    		for (int i=width/4; 4*i<=3*width; i+=width/4) {
	        		System.out.println("i"+i + " j" + j);
	            	int color = photo.getPixel(i, j);
	            	try {
	            		JSONObject RGB = COLORDATA.getJSONObject("test");
	            		RGB.put(COMPASSPOINTS[k], color);
		            	
		            	TextView tv = (TextView)findViewById(getResources().getIdentifier(COMPASSPOINTS[k], "id", getPackageName()));
		            	tv.setText("R:"+Color.red(color)
		            				+" G:"+Color.green(color)
		            				+" B:"+Color.blue(color)
		            				+MARKEDTEXT[k]);
		            	;
		            	
		        		if (!markButton.isChecked()) {
			            	fillChipTriangle(colorChipCanvas, color);
			            	tv.setBackgroundDrawable(new BitmapDrawable(colorChip.copy(Bitmap.Config.ARGB_4444, false)));
		        		}
		        		
		            	k++;
	            	} catch (Exception e) {
	            		e.printStackTrace();
	            	}
	        		
	        	}

	    	}

		}
	};

	// When the user toggles the mark/unmark button, we freeze/unfreeze the color updates.
	public void onClick(View arg0) {
		// Mark
		if (markButton.isChecked()) {
        	try {
        		JSONObject RGB = COLORDATA.getJSONObject("test");
        		COLORDATA.put("mark", new JSONObject(RGB.toString()));
        		for (int k = 0; k < 9 ; k++) {
            		int color = RGB.getInt(COMPASSPOINTS[k]);
	            	TextView tv = (TextView)findViewById(getResources().getIdentifier(COMPASSPOINTS[k], "id", getPackageName()));
		            fillChipTriangle(colorChipCanvas, color);
		            tv.setBackgroundDrawable(new BitmapDrawable(colorChip.copy(Bitmap.Config.ARGB_4444, false)));
		            MARKEDTEXT[k] = "\nMARKED:\nR:"
		            		+Color.red(color)
		            		+" G:"+Color.green(color) 
		            		+" B:"+Color.blue(color)
		            		;
        		}
        	} catch (Exception e) {
        		e.printStackTrace();
        	}
    	// Unmark
		} else {
			// do nothing
    		for (int k = 0; k < 9 ; k++) {
    			MARKEDTEXT[k]="";
    		}
		}
	};
	
	// Cleanup code ; when we leave the app, make sure we leave.  The mSurfaceCallback
	// takes care of shutting down the camera.
	@Override
	protected void onPause() {
		super.onPause();
		if (mThread !=null && mThread.isAlive()) mThread.interrupt();
		try {
			this.getPreferences(Activity.MODE_PRIVATE).edit().putString("JSONData", COLORDATA.toString()).commit();
		} catch (Exception e) {
			// we're finishing regardless
		}
		if (!isFinishing()) finish();
	}

	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode!=0) finish();
    	if (data.getExtras().get("data")==null) finish();
    	photo = (Bitmap)(data.getExtras().get("data"));
    	int width = photo.getWidth();
    	int height = photo.getHeight();
    	String buffer = "";
		System.out.println(height);
    	for (int j=height/4; 4*j<=3*height; j+=height/4) {
    		for (int i=width/4; 4*i<=3*width; i+=width/4) {
        		System.out.println("i"+i + " j" + j);
            	int color = photo.getPixel(i, j);

            	buffer += "at test point ("+i+","+j+"): R:" + Color.red(color);
            	buffer += "; G:" + Color.green(color);
            	buffer += "; B:" + Color.blue(color) + "\n";
        		
        	}

    	}
    	resultText.setText(buffer);
    	resultImage.setBackgroundDrawable(new BitmapDrawable(photo));
    	resultText.invalidate();
    	resultImage.invalidate();
    }

 // decode Y, U, and V values on the YUV 420 buffer described as YCbCr_422_SP by Android
 // David Manpearl 081201
    
    public static void decodeYUV(int[] out, byte[] fg, int width, int
    height) throws NullPointerException, IllegalArgumentException {
            final int sz = width * height;
            if(out == null) throw new NullPointerException("buffer 'out' is null");
            if(out.length < sz) throw new IllegalArgumentException("buffer 'out' size " + out.length + " < minimum " + sz);
            if(fg == null) throw new NullPointerException("buffer 'fg' is null");
            if(fg.length < sz) throw new IllegalArgumentException("buffer 'fg' size " + fg.length + " < minimum " + sz * 3/ 2);
            int i, j;
            int Y, Cr = 0, Cb = 0;
            for(j = 0; j < height; j++) {
                    int pixPtr = j * width;
                    final int jDiv2 = j >> 1;
                    for(i = 0; i < width; i++) {
                            Y = fg[pixPtr]; if(Y < 0) Y += 255;
                            if((i & 0x1) != 1) {
                                    final int cOff = sz + jDiv2 * width + (i >> 1) * 2;
                                    Cb = fg[cOff];
                                    if(Cb < 0) Cb += 127; else Cb -= 128;
                                    Cr = fg[cOff + 1];
                                    if(Cr < 0) Cr += 127; else Cr -= 128;
                            }
                            int R = Y + Cr + (Cr >> 2) + (Cr >> 3) + (Cr >> 5);
                            if(R < 0) R = 0; else if(R > 255) R = 255;
                            int G = Y - (Cb >> 2) + (Cb >> 4) + (Cb >> 5) - (Cr >> 1) + (Cr >>
    3) + (Cr >> 4) + (Cr >> 5);
                            if(G < 0) G = 0; else if(G > 255) G = 255;
                            int B = Y + Cb + (Cb >> 1) + (Cb >> 2) + (Cb >> 6);
                            if(B < 0) B = 0; else if(B > 255) B = 255;
                            out[pixPtr++] = 0xff000000 + (B << 16) + (G << 8) + R;
                    }
            }

    }    
 
    public static void fillChipTriangle(Canvas canvas, int color) {
    	Paint pt = new Paint();
    	pt.setColor(color);
    	pt.setStyle(Paint.Style.FILL);
    	
    	Path path = new Path();
    	path.moveTo(0f, 0f);
    	path.lineTo(30f, 30f);
    	path.lineTo(0f, 30f);
    	path.close();
    	
    	canvas.drawPath(path, pt);
    }
    
}