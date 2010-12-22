package com.sunnykwong.colorcalibrate;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
public class CCActivity extends Activity {

	static Intent CAMINTENT = new Intent("android.media.action.IMAGE_CAPTURE");
    TextView resultText;
    ImageView resultImage;
    Bitmap photo;
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resultText = (TextView)findViewById(R.id.resultText);
        resultImage = (ImageView)findViewById(R.id.resultImage);
        resultText.setText("Starting Camera...");
        resultText.invalidate();
        
    	startActivityForResult(CCActivity.CAMINTENT, 0);

    }
    
    @Override
    protected void onResume() {
    	super.onResume();
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