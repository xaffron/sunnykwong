package com.sunnykwong.colorcalibrate;
import android.content.pm.ActivityInfo;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import java.util.Iterator;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CCActivity extends Activity {

	public SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
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
	    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
	        Camera.Parameters camParams=mCam.getParameters();

	        camParams.setPreviewSize(176,144);
	        camParams.setPreviewFrameRate(10);
	        camParams.setPictureFormat(android.graphics.PixelFormat.JPEG);
	        mCam.setParameters(camParams);
	        mCam.startPreview();
	    }

	    public void surfaceDestroyed(SurfaceHolder holder) {
	    	mCam.stopPreview();
	    	mCam.release();
	    	mCam=null;
	    	System.out.println("cam released");
	    }
	};

	
	static Intent CAMINTENT = new Intent("android.media.action.IMAGE_CAPTURE");
    TextView resultText;
    SurfaceView mSurfacePreview;
    ImageView resultImage;
    Bitmap photo;
    Camera mCam;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSurfacePreview = (SurfaceView)findViewById(R.id.surfacepreview);
        mSurfacePreview.getHolder().addCallback(mSurfaceCallback);
        mSurfacePreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    
	@Override
	protected void onPause() {
		super.onPause();
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

}