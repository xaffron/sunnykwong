package com.sunnykwong.tmouth;

import android.app.Activity;
import android.os.Bundle;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;
import android.os.Handler;
import android.util.Log;
import android.app.AlertDialog;
import android.widget.ImageView;
import android.content.Intent;
import android.text.format.Time;

import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.media.AudioManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;




public class TMActivity extends Activity implements OnInitListener {

	public static final boolean DEBUG=false;
	public static final String YAHOOAPPID = "&appid=SEIMHqjV34FnoPLluYnjx5lVR_v6eitZBAZVxGDL0HGmRCFM5ktDPZIGgOxG0BO5";
	public static final String URLPREFIX = "http://local.yahooapis.com/MapsService/V1/trafficData?output=xml&radius=50";
	public static final int STALEMINUTES = 30;

	public static ArrayList<String> PREFWORDLIST;
	public static String[] lookups;
	public static String[] values;

	public static RS RUNSTAGE = RS.BEGIN;
	public static AlertDialog TVDIALOG;
	public static ImageView TVIMAGEVIEW;
	
	public static TextToSpeech TTSENGINE;

	enum RS {BEGIN, GETLOCATION, GETTRAFFIC, PARSEXML, REPORTTRAFFIC, TEXT, DONE};

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	public static ArrayList<HashMap<String,String>> ELEMENTS;
	public static String tempText = "";


	final Runnable mTrafficResult = new Runnable() {
		public void run() {
			
			switch (TMActivity.RUNSTAGE) {
			case GETLOCATION: 
				// Since locationchangedlistener should take care of this case, 
				// we should never reach this code
				TMActivity.RUNSTAGE = RS.GETTRAFFIC;
				break;
			case GETTRAFFIC:
				TMActivity.RUNSTAGE = RS.PARSEXML;
				processTrafficResults();
				break;
			case PARSEXML:
				TMActivity.RUNSTAGE = RS.REPORTTRAFFIC;
				processXMLResults();
				break;
			case TEXT:
				TMActivity.TEXT.setText(TMActivity.tempText);
				TMActivity.TEXT.invalidate();
				break;
			case DONE:
				finish();
				break;
			case BEGIN:
			default:
				//do nothing
			}

		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		if (lookups == null)TMActivity.lookups = getResources().getStringArray(R.array.Lookups);
		if (TMActivity.values == null)TMActivity.values = getResources().getStringArray(R.array.Values);

		TMActivity.TVIMAGEVIEW = new ImageView(this);
		TMActivity.TVIMAGEVIEW.setBackgroundResource(R.drawable.android);
		
//        FrameLayout fl = (FrameLayout) findViewById(android.R.id.);
 //       fl.addView(TMActivity.TVIMAGEVIEW, new LayoutParams(MATCH_PARENT, WRAP_CONTENT));
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        TMActivity.TVDIALOG = get

        mHandler = new Handler();
        if (TMActivity.ELEMENTS==null) TMActivity.ELEMENTS = new ArrayList<HashMap<String,String>>();
        TMActivity.ELEMENTS.clear();
        
        TMActivity.RUNSTAGE = RS.BEGIN;
        System.setProperty
        ("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 
        
        setContentView(R.layout.main);
        TMActivity.TEXT = (TextView)this.findViewById(R.id.tra);
        TMActivity.TEXT.setTextSize(28);
        TMActivity.TEXT.setTextColor(Color.WHITE);
        TMActivity.TEXT.setText("Waiting for Location...");
   
        TMActivity.LM = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        TMActivity.LL = new LocationListener() {
        	public void onLocationChanged(Location location) {
        		TMActivity.CURRLOCN=location;
        		TMActivity.TEXT.setText("Location Obtained.  Retrieving traffic information...".toString());
        		processLocationResults();
        	}
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };
        TMActivity.RUNSTAGE = RS.GETLOCATION;
        TMActivity.LM.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, TMActivity.LL);
    }

    private void processLocationResults(){
        TMActivity.RUNSTAGE = RS.GETTRAFFIC;
    	TMActivity.TEXT.invalidate();
    	TMActivity.LM.removeUpdates(TMActivity.LL);
    	Thread t = new Thread () {
    		public void run() {
        		BufferedReader in = null;
//    	    	TMActivity.TEXT.setText("Requesting traffic information...".toString());
        		try {
        			HttpClient client = new DefaultHttpClient();
        			HttpGet request = new HttpGet();
        			
        			request.setURI(new URI(TMActivity.URLPREFIX + TMActivity.YAHOOAPPID 
        					+ "&latitude=" + TMActivity.CURRLOCN.getLatitude() 
        					+ "&longitude=" + TMActivity.CURRLOCN.getLongitude()));
        			HttpResponse response = client.execute(request);
        			in = new BufferedReader (new InputStreamReader(response.getEntity().getContent()));
        			StringBuffer sb = new StringBuffer("");
        			String line = "";
        			String NL = System.getProperty("line.separator");
        			while ((line = in.readLine()) != null) {
        					sb.append(line + NL);
        			}
        			in.close();
        			TMActivity.TRAFFICRESULTS = sb.toString();
//        			TMActivity.TEXT.setText(TMActivity.TRAFFICRESULTS);

        		} catch (Exception e) {
//        	    	TMActivity.TEXT.setText("Error Parsing traffic information...".toString());
        	    	e.printStackTrace();
        		} finally {
        			if (in != null) {
        				try {
        					in.close();
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
        			}
        		}
//        		TMActivity.TEXT.setText("Parsing traffic information...".toString());
//            	Log.i("TMouth",TMActivity.TRAFFICRESULTS);
    			mHandler.post(mTrafficResult);
    		}
      	   
    	};
		t.start();
   }
 		
    private void processTrafficResults() {
    	TMActivity.TEXT.invalidate();

    	Thread t = new Thread () {
    		public void run() {
            	try {
            		XMLReader xr = XMLReaderFactory.createXMLReader();
            		TReport handler = new TReport();
            		xr.setContentHandler(handler);
            		StringReader sr = new StringReader(TMActivity.TRAFFICRESULTS);
            		xr.setErrorHandler(handler);
            		xr.parse(new InputSource(sr));
            	} catch (Exception e) {
            		
                	e.printStackTrace();
            	}

            	// This call will end up passing control to processXMLResults
                TMActivity.RUNSTAGE = RS.PARSEXML;
    			mHandler.post(mTrafficResult);
    		}
      	   
    	};
		t.start();

    } 

	public void processXMLResults() {

		Comparator<HashMap<String,String>> c = new Comparator<HashMap<String,String>>() {
    		Location tmpLocn1 = new Location("");
    		Location tmpLocn2 = new Location("");
    		@Override

    		public int compare(HashMap<String,String> hm1, HashMap<String,String> hm2) {
    			tmpLocn1.setLatitude(Double.parseDouble(hm1.get("Latitude")));
    			tmpLocn1.setLongitude(Double.parseDouble(hm1.get("Longitude")));
    			tmpLocn2.setLatitude(Double.parseDouble(hm2.get("Latitude")));
    			tmpLocn2.setLongitude(Double.parseDouble(hm1.get("Longitude")));

//    			//Simple logic - if there's an accident, always report first
//    			if (hm1.get("Title").contains("cident") || hm1.get("Title").contains("isabled vehicle")) {
//    				return -1;
//    			} else if (hm2.get("Title").contains("cident") || hm2.get("Title").contains("isabled vehicle")){
//    				return 1;
//    			}
//    			//Otherwise, report by severity and distance to phone
//    			else {
//    				return Math.round(Integer.parseInt(hm1.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn1)
//    					- (Integer.parseInt(hm2.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn2)));
//    			}

    			// Simplest logic - only sort by geographical distance
    			return Math.round(Integer.parseInt(hm1.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn1)
    					- (Integer.parseInt(hm2.get("Severity"))*-100000 + TMActivity.CURRLOCN.distanceTo(tmpLocn2)));
    		}
    	};
    	
    	Collections.sort(TMActivity.ELEMENTS,c);
 
    	// Initialize TTS Engine, pass 
    	Intent checkIntent = new Intent();
    	checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
    	startActivityForResult(checkIntent, 0);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == 0) {
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {

	            // Create the TTS instance; once TTS inits, control returns to the onInit method below
	            if (TMActivity.TTSENGINE==null) {
	            	TMActivity.TTSENGINE = new TextToSpeech(this, this);
	            }
	            
	        } else {
	            // missing data, install it

	        	Intent installIntent = new Intent();
	            installIntent.setAction(
	                TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	            startActivity(installIntent);
	        }
	        
	    }
	}
	
    @Override
    public void onInit(int status) {       
    	// OK, TTS Engine initialized successfully.
		// Set to a male voice (TODO:  customizable)
    	TMActivity.TTSENGINE.setLanguage(Locale.US);
		TMActivity.TTSENGINE.setSpeechRate((float)1.1);
		TMActivity.TTSENGINE.setPitch((float)0.95);

		
		// Start yammering!
    	if (status == TextToSpeech.SUCCESS) {
        	Thread t = new Thread () {
        		public void run() {
                	try {
                        HashMap<String, String> hashText = new HashMap<String, String>();
                        Iterator<HashMap<String,String>> i = TMActivity.ELEMENTS.iterator();
                    	while (i.hasNext()) {
                    		HashMap<String,String> element = i.next();

                    		//If data is stale, skip to the next element
                    		Time t = new Time();
                    		t.setToNow();
                    		if (t.toMillis(false)/60000 - Long.parseLong(element.get("UpdateDate"))/60 > TMActivity.STALEMINUTES) continue; 
                    		
                    		String s = element.get("Spoken");
                    		hashText.clear();
                            hashText.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,s);
                            TMActivity.tempText= element.get("Title") + "\n\n" + element.get("Description");
                            TMActivity.RUNSTAGE = RS.TEXT;
                            mHandler.post(mTrafficResult);
                            if (DEBUG) {
                            	Log.i("TMouth",s);
                            } else {
                            	TMActivity.TTSENGINE.speak(s, TextToSpeech.QUEUE_ADD, hashText);
                            	while (TMActivity.TTSENGINE.isSpeaking()) Thread.sleep(2000);
                            }
                    	}

                	} catch (Exception e) {
                		
                    	e.printStackTrace();
                	}

                	// This call will end up passing control to onPause
                    TMActivity.RUNSTAGE = RS.DONE;
        			mHandler.post(mTrafficResult);
        		}
          	   
        	};
    		t.start();

	    } else if (status == TextToSpeech.ERROR) {
	    	Log.e("TMouth","Error initializing TTS Engine.");
	    }
	}

    @Override
    public void onDestroy() {
    	if (TMActivity.TTSENGINE!=null) TMActivity.TTSENGINE.shutdown();
    	super.onDestroy();
    }

}