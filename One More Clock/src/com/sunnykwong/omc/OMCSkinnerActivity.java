package com.sunnykwong.omc;

import java.io.FileReader;
import java.io.File;
import java.util.HashMap;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.AdapterView.OnItemSelectedListener;
import android.os.Bundle;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Environment;

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
        setContentView(R.layout.importlayout);

        mHandler = new Handler();

        OMCSkinnerActivity.CURRSELECTEDTHEME = null;
        
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCSkinnerActivity.SDROOT = Environment.getExternalStorageDirectory();
        OMCSkinnerActivity.THEMEROOT = new File(OMCSkinnerActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCSkinnerActivity.THEMEROOT.exists()) {
        	Toast.makeText(this, "OMC folder not found in your SD Card.\nCreating folder...", Toast.LENGTH_LONG).show();
        	OMCSkinnerActivity.THEMEROOT.mkdir();
        }
        if (OMCSkinnerActivity.THEMEROOT.listFiles().length == 0) {
        	//No themes downloaded
        	Toast.makeText(this, "No themes downloaded.\nCannot import...", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }
        
        OMCSkinnerActivity.mAD = new AlertDialog.Builder(this)
		.setTitle("OMC - Theme Import")
		.setMessage("One More Clock! lets you load your own custom-designed theme.  Remember that your theme files should be unzipped and stored on your SD Card for OMC to find them.")
	    .setCancelable(true)
	    .setIcon(R.drawable.fredicon_mdpi)
	    .setPositiveButton("Okay", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
	       		OMCSkinnerActivity.mAD.dismiss();
			}
		})
	    .setOnKeyListener(new OnKeyListener() {
	    	public boolean onKey(DialogInterface arg0, int arg1, android.view.KeyEvent arg2) {
	       		OMCSkinnerActivity.mAD.dismiss();
	    		return true;
	    	};
	    }).create();

        OMCSkinnerActivity.mAD.show();

        OMCSkinnerActivity.THEMEARRAY =  new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line);
        OMCSkinnerActivity.THEMES =  new HashMap<String, File>();
        for (File f:OMCSkinnerActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.txt");
        	if (ff.exists()) {
        		OMCSkinnerActivity.THEMEARRAY.add(f.getName());
        		OMCSkinnerActivity.THEMES.put(f.getName(), f);
        	}
        }
        OMCSkinnerActivity.THEMESPINNER = (Spinner)this.findViewById(R.id.spinner);
        OMCSkinnerActivity.THEMESPINNER.setAdapter(OMCSkinnerActivity.THEMEARRAY);
        OMCSkinnerActivity.THEMESPINNER.setOnItemSelectedListener(new OnItemSelectedListener() {
        	@Override
        	public void onItemSelected(AdapterView<?> arg0, View arg1,
        			int position, long id) {
        		// TODO Auto-generated method stub
        		setThemePreview(OMCSkinnerActivity.THEMEARRAY.getItem(position));
        		
        	}
        	@Override
        	public void onNothingSelected(AdapterView<?> arg0) {
        		// do nothing
       		
        	}
		});
        OMCSkinnerActivity.THEMESPINNER.invalidate();

        ((Button)this.findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				OMCSkinnerActivity.this.setResult(Activity.RESULT_OK);
				OMCSkinnerActivity.this.finish();
			}
		});
        
        ((Button)this.findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (OMCSkinnerActivity.CURRSELECTEDTHEME!=null) {
					if (!OMC.IMPORTEDTHEMEMAP.containsKey(OMCSkinnerActivity.CURRSELECTEDTHEME)) importTheme();
					else {
						
						Toast.makeText(OMCSkinnerActivity.this, 
								"Theme already imported and cached.\n" + 
								OMC.IMPORTEDTHEMEMAP.get(
										OMCSkinnerActivity.CURRSELECTEDTHEME).arrays
										.get("theme_options").get(0) + " theme applied."
										, Toast.LENGTH_SHORT).show();
						
			        	OMC.PREFS.edit()
			        	.putString("widgetTheme", OMCSkinnerActivity.CURRSELECTEDTHEME)
			        	.putBoolean("external", true)
			    		.commit();

			        	setResult(Activity.RESULT_OK);
						finish();
					}

				}
			}
		});

    }
	public void setThemePreview(String sThemeName) {
		OMCSkinnerActivity.CURRSELECTEDTHEME = sThemeName;
		if (sThemeName == null || sThemeName.equals("")) return;
		File root = OMCSkinnerActivity.THEMES.get(sThemeName);
		System.out.println(root.getAbsolutePath() + "/preview.png");
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
            		FileReader fr = new FileReader(root.getAbsolutePath() + "/00control.txt");
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