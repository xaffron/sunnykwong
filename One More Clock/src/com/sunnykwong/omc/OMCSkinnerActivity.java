package com.sunnykwong.omc;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.content.Intent;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
public class OMCSkinnerActivity extends Activity {

	public Handler mHandler;
	public static TextView TEXT;
	public static LocationManager LM;
	public static Location CURRLOCN;
	public static LocationListener LL;
	public static String TRAFFICRESULTS;
	public static HashMap<String,String[]> ELEMENTS;
	public static HashMap<String,File> THEMES;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ArrayAdapter<String> THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;
	
    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
		// Back from XML importing...
			if (OMCXMLThemeParser.valid) {
	        	Toast.makeText(OMCSkinnerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme imported.", Toast.LENGTH_SHORT).show();
	        	OMC.PREFS.edit()
			        	.putString("widgetTheme", OMCXMLThemeParser.latestThemeName)
			        	.putBoolean("external", true)
			    		.commit();
	        	OMC.saveImportedThemeToCache(OMCSkinnerActivity.this,OMCXMLThemeParser.latestThemeName);
	        	Toast.makeText(OMCSkinnerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme cached and applied.", Toast.LENGTH_SHORT).show();
			} else {
	        	Toast.makeText(OMCSkinnerActivity.this, OMCSkinnerActivity.CURRSELECTEDTHEME + " theme did not pass validity checks!\nPlease check with the author of your theme.\nImport cancelled.", Toast.LENGTH_SHORT).show();
			}

			setResult(Activity.RESULT_OK);
        	finish();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skinnertool);

        mHandler = new Handler();

        Intent it = new Intent(this,OMCThemePickerActivity.class);
        startActivityForResult(it, 0);

    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	// TODO Auto-generated method stub
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	String sTheme = (String)(data.getExtras().get("theme"));
    	boolean bExternal = (Boolean)(data.getExtras().get("external"));
    	boolean bCustomStretch = true;

    	OMC.PREFS.edit().putString("widgetTheme-1", (String)(data.getExtras().get("theme")))
		.putBoolean("widget24HrClock-1", OMC.PREFS.getBoolean("widget24HrClock", true))
		.putBoolean("external-1", (Boolean)(data.getExtras().get("external")))
		.putString("URI-1", OMC.PREFS.getString("URI", ""))
		.commit();
    	
    	ImageView FourByTwo = (ImageView)this.findViewById(R.id.FourByTwo);
    	Bitmap bmpRender = OMCWidgetDrawEngine.drawBitmapForWidget(this, -1);
    	FourByTwo.setImageBitmap(bmpRender);
    	ImageView FourByOne = (ImageView)this.findViewById(R.id.FourByOne);
    	ImageView ThreeByOne = (ImageView)this.findViewById(R.id.ThreeByOne);

		String[] FourByOneStretch = new String[4];
		String[] ThreeByOneStretch = new String[4];

		try {
			if (bExternal) {
				OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme + "_4x1SqueezeInfo").toArray(FourByOneStretch);
				OMC.IMPORTEDTHEMEMAP.get(sTheme).arrays.get(sTheme + "_3x1SqueezeInfo").toArray(ThreeByOneStretch);
			} else {
				int iLayerID = this.getResources().getIdentifier(sTheme + "_4x1SqueezeInfo", "array", "com.sunnykwong.omc");
				FourByOneStretch = this.getResources().getStringArray(iLayerID);
				iLayerID = this.getResources().getIdentifier(sTheme + "_3x1SqueezeInfo", "array", "com.sunnykwong.omc");
				ThreeByOneStretch = this.getResources().getStringArray(iLayerID);
			}
		} catch (android.content.res.Resources.NotFoundException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for seeded clock.");
			bCustomStretch=false;
		} catch (java.lang.NullPointerException e) {
			// OMC.STRETCHINFO stays null; do nothing
			if (OMC.DEBUG) Log.i("OMCEngine","No stretch info found for imported clock.");
			bCustomStretch=false;
		}

		if (bCustomStretch){
			System.out.println("CustomStretch" + FourByOneStretch[0] + FourByOneStretch[1]);
			//Custom scaling
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preScale(Float.parseFloat(FourByOneStretch[0]), 
					Float.parseFloat(FourByOneStretch[1]));
			FourByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(FourByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(FourByOneStretch[2]) - Integer.parseInt(FourByOneStretch[3]), OMC.TEMPMATRIX, true));
			OMC.TEMPMATRIX.reset();
			OMC.TEMPMATRIX.preScale(Float.parseFloat(ThreeByOneStretch[0]), 
					Float.parseFloat(ThreeByOneStretch[1]));
			ThreeByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(ThreeByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(ThreeByOneStretch[2]) - Integer.parseInt(ThreeByOneStretch[3]), OMC.TEMPMATRIX, true));
		} else {
			//Default scaling
			OMC.TEMPMATRIX.reset();
//			OMC.TEMPMATRIX.preTranslate(0, 0-iCutTop);
//			OMC.TEMPMATRIX.preScale(fScaleX, fScaleY);
			FourByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(FourByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(FourByOneStretch[2]) - Integer.parseInt(FourByOneStretch[3]), OMC.TEMPMATRIX, true));
			ThreeByOne.setImageBitmap(Bitmap.createBitmap(bmpRender, 0, Integer.parseInt(ThreeByOneStretch[2]), OMC.BUFFER.getWidth(), OMC.BUFFER.getHeight() - Integer.parseInt(ThreeByOneStretch[2]) - Integer.parseInt(ThreeByOneStretch[3]), OMC.TEMPMATRIX, true));
		}
	
	    	
    	this.findViewById(R.id.FourByTwo).invalidate();
    	this.findViewById(R.id.FourByOne).invalidate();
    	this.findViewById(R.id.ThreeByOne).invalidate();
	    	
	//    	finish();
    }
    
	public void setThemePreview(String sThemeName) {
		OMCSkinnerActivity.CURRSELECTEDTHEME = sThemeName;
		if (sThemeName == null || sThemeName.equals("")) return;
		File root = OMCSkinnerActivity.THEMES.get(sThemeName);
		if (OMC.DEBUG) Log.i("OMCTheme",root.getAbsolutePath() + "/preview.png");
		Bitmap bmpPreview = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(root.getAbsolutePath() + "/preview.jpg"),320,200,false);
		((ImageView)this.findViewById(R.id.ImagePreview)).setImageBitmap(bmpPreview);
		OMCSkinnerActivity.THEMECREDITS = new char[3000];
		try {
			FileReader fr = new FileReader(root.getAbsolutePath() + "/00credits.txt");
			fr.read(OMCSkinnerActivity.THEMECREDITS);
			((TextView)this.findViewById(R.id.TextPreview)).setText(String.valueOf(OMCSkinnerActivity.THEMECREDITS).trim());
			this.findViewById(R.id.toplevel).invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	public void importTheme() {
		if (OMCSkinnerActivity.CURRSELECTEDTHEME == null) {
        	Toast.makeText(this, "Please select a theme first.", Toast.LENGTH_SHORT).show();
			return;
		}
		
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 

		
		
    	Thread t = new Thread () {
    		public void run() {
            	try {
            		// Set SD OMC Root
            		File root = OMCSkinnerActivity.THEMES.get(OMCSkinnerActivity.CURRSELECTEDTHEME);
            		// Setup XML Parsing...
            		XMLReader xr = XMLReaderFactory.createXMLReader();
            		OMCXMLThemeParser parser = new OMCXMLThemeParser(root.getAbsolutePath());
            		xr.setContentHandler(parser);
            		// Feed data from control file to XML Parser.
            		// XML Parser will populate OMC.IMPORTEDTHEME.
            		FileReader fr = new FileReader(root.getAbsolutePath() + "/00control.xml");
            		xr.setErrorHandler(parser);
            		xr.parse(new InputSource(fr));
            		// When we're done, remove all references to parser.
                	parser = null;
                	fr.close();

            	} catch (Exception e) {
            		
                	e.printStackTrace();
            	}

            	// This call will end up passing control to processXMLResults
    			mHandler.post(mResult);
    		}
      	   
    	};
		t.start();

    } 

	
}