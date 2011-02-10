package com.sunnykwong.buttontext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

public class ButtonTextActivity extends Activity {
	
	Button mButton1, mButton2, mButton3;
	TextView mTV1, mTV2, mTV3;
	JSONArray array1, array2, array3;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the buttons/textviews.
        setContentView(R.layout.main);
        mButton1 = (Button)findViewById(R.id.Button01);
        mButton2 = (Button)findViewById(R.id.Button02);
        mButton3 = (Button)findViewById(R.id.Button03);
        mTV1 = (TextView)findViewById(R.id.TextView01);
        mTV2 = (TextView)findViewById(R.id.TextView02);
        mTV3 = (TextView)findViewById(R.id.TextView03);
        
        // Check SD Card.
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	finish();
        }

        
        // Read the three files.
        File wordsrc1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/wordsrc1.json");
		if (!wordsrc1.canRead()) {
        	Toast.makeText(this, "Cannot read wordsrc1.json .\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	finish();
        } else {
        	array1 = readJSONFile(wordsrc1);
        }

        File wordsrc2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/wordsrc2.json");
		if (!wordsrc2.canRead()) {
        	Toast.makeText(this, "Cannot read wordsrc2.json .\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	finish();
        } else {
        	array2 = readJSONFile(wordsrc2);
        }

        File wordsrc3 = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/wordsrc3.json");
		if (!wordsrc3.canRead()) {
        	Toast.makeText(this, "Cannot read wordsrc3.json .\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
        	finish();
        } else {
        	array3 = readJSONFile(wordsrc3);
        }

		
		// Set up what each button does.
        mButton1.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTV1.setText(array1.optString((int)(Math.random()*array1.length())));
				mTV1.invalidate();
			}
		});
        mButton2.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTV2.setText(array2.optString((int)(Math.random()*array2.length())));
				mTV2.invalidate();
			}
		});
        mButton3.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTV3.setText(array3.optString((int)(Math.random()*array3.length())));
				mTV3.invalidate();
			}
		});
		
		
    }
    
	
	public JSONArray readJSONFile (File f){
		JSONArray oResult=null;
		try {
			BufferedReader in = new BufferedReader(new FileReader(f),8192);
			StringBuilder sb = new StringBuilder();
		    char[] buffer = new char[8192];
		    int iCharsRead = 0;
		    while ((iCharsRead=in.read(buffer))!= -1){
		    	sb.append(buffer, 0, iCharsRead);
		    }
		    in.close();
		    
			oResult = new JSONArray(sb.toString());
			sb.setLength(0);
		} catch (Exception e) {
			Log.i("App","error reloading " + f.getName());
			e.printStackTrace();
		}
		return oResult;
	}

}