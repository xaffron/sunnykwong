package com.sunnykwong.omc;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import android.content.Intent;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView.OnItemClickListener;

public class OMCThemePickerActivity extends Activity implements OnClickListener, OnItemClickListener {

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
	public static ImageAdapter THEMEARRAY;
	public static Spinner THEMESPINNER;
	public static char[] THEMECREDITS;
	public static String CURRSELECTEDTHEME, RAWCONTROLFILE;
	
	public Button btnReload;
	public Gallery gallery;
	public TextView tvCredits;
	
    static AlertDialog mAD;	

	final Runnable mResult = new Runnable() {
		public void run() {
		// Back from XML importing...
			if (OMCXMLThemeParser.valid) {
	        	Toast.makeText(OMCThemePickerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme imported.", Toast.LENGTH_SHORT).show();
	        	OMC.PREFS.edit()
			        	.putString("widgetTheme", OMCXMLThemeParser.latestThemeName)
			        	.putBoolean("external", true)
			    		.commit();
	        	OMC.saveImportedThemeToCache(OMCThemePickerActivity.this,OMCXMLThemeParser.latestThemeName);
	        	Toast.makeText(OMCThemePickerActivity.this, OMC.IMPORTEDTHEMEMAP.get(OMCXMLThemeParser.latestThemeName).arrays.get("theme_options").get(0) + " theme cached and applied.", Toast.LENGTH_SHORT).show();
			} else {
	        	Toast.makeText(OMCThemePickerActivity.this, OMCThemePickerActivity.CURRSELECTEDTHEME + " theme did not pass validity checks!\nPlease check with the author of your theme.\nImport cancelled.", Toast.LENGTH_SHORT).show();
			}

			setResult(Activity.RESULT_OK);
        	finish();
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.themepickerlayout);

        setTitle("Swipe Left and Right to Select a Theme");

        mHandler = new Handler();
        
        OMCThemePickerActivity.CURRSELECTEDTHEME = null;

        btnReload = (Button)findViewById(R.id.btnReload);
        btnReload.setOnClickListener(this);

        gallery = (Gallery)this.findViewById(R.id.gallery);
        refreshThemeList();

        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setSelection(3, true);
        gallery.setOnItemClickListener(this);
        
//        ((Button)this.findViewById(R.id.buttonCancel)).setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				OMCThemePickerActivity.this.setResult(Activity.RESULT_OK);
//				OMCThemePickerActivity.this.finish();
//			}
//		});
//        
//        ((Button)this.findViewById(R.id.buttonOK)).setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//				if (OMCThemePickerActivity.CURRSELECTEDTHEME!=null) {
//					if (!OMC.IMPORTEDTHEMEMAP.containsKey(OMCThemePickerActivity.CURRSELECTEDTHEME)) importTheme();
//					else {
//						
//						Toast.makeText(OMCThemePickerActivity.this, 
//								"Theme already imported and cached.\n" + 
//								OMC.IMPORTEDTHEMEMAP.get(
//										OMCThemePickerActivity.CURRSELECTEDTHEME).arrays
//										.get("theme_options").get(0) + " theme applied."
//										, Toast.LENGTH_SHORT).show();
//						
//			        	OMC.PREFS.edit()
//			        	.putString("widgetTheme", OMCThemePickerActivity.CURRSELECTEDTHEME)
//			        	.putBoolean("external", true)
//			    		.commit();
//
//			        	setResult(Activity.RESULT_OK);
//						finish();
//					}
//
//				}
//			}
//		});

    }
    
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v==btnReload) {
    		if (OMC.DEBUG) Log.i("OMCSkin","Refreshing Themes");
    		refreshThemeList();
    	}
     }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
    	// TODO Auto-generated method stub
    	if (arg0==gallery) {
    		if (OMC.DEBUG) Log.i("OMCSkin","Selected Theme " + OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
    		Intent it = new Intent();
    		setResult(Activity.RESULT_OK, it);
    		it.putExtra("theme", OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
    		it.putExtra("external", true);
    		
    		
    		OMCThemePickerActivity.THEMEARRAY.dispose();
    		finish();
    	}
    }

    public void setThemePreview(String sThemeName) {
		OMCThemePickerActivity.CURRSELECTEDTHEME = sThemeName;
		if (sThemeName == null || sThemeName.equals("")) return;
		File root = OMCThemePickerActivity.THEMES.get(sThemeName);
		if (OMC.DEBUG) Log.i("OMCSkinner",root.getAbsolutePath() + "/preview.png");
		Bitmap bmpPreview = Bitmap.createScaledBitmap(BitmapFactory.decodeFile(root.getAbsolutePath() + "/preview.jpg"),320,200,false);
		((ImageView)this.findViewById(R.id.ImagePreview)).setImageBitmap(bmpPreview);
		OMCThemePickerActivity.THEMECREDITS = new char[3000];
		try {
			FileReader fr = new FileReader(root.getAbsolutePath() + "/00credits.txt");
			fr.read(OMCThemePickerActivity.THEMECREDITS);
			((TextView)this.findViewById(R.id.TextPreview)).setText(String.valueOf(OMCThemePickerActivity.THEMECREDITS).trim());
			this.findViewById(R.id.toplevel).invalidate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
	public void importTheme() {
		if (OMCThemePickerActivity.CURRSELECTEDTHEME == null) {
        	Toast.makeText(this, "Please select a theme first.", Toast.LENGTH_SHORT).show();
			return;
		}
		
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver"); 

		
		
    	Thread t = new Thread () {
    		public void run() {
            	try {
            		// Set SD OMC Root
            		File root = OMCThemePickerActivity.THEMES.get(OMCThemePickerActivity.CURRSELECTEDTHEME);
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

	public void refreshThemeList() {

		gallery.setVisibility(View.INVISIBLE);
		btnReload.setClickable(false);
		
		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCThemePickerActivity.SDROOT = Environment.getExternalStorageDirectory();
        OMCThemePickerActivity.THEMEROOT = new File(OMCThemePickerActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCThemePickerActivity.THEMEROOT.exists()) {
        	Toast.makeText(this, "OMC folder not found in your SD Card.\nCreating folder...", Toast.LENGTH_LONG).show();
        	OMCThemePickerActivity.THEMEROOT.mkdir();
        }
//        OMC.RES.getStringArray(R.array.theme_values);

        
        //        if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 0) {
//        	//No themes downloaded
//        	Toast.makeText(this, "No themes downloaded. Exiting!", Toast.LENGTH_LONG).show();
//			setResult(Activity.RESULT_OK);
//			finish();
//        	return;
//        }
        
        if (OMCThemePickerActivity.THEMEARRAY == null) {
        	OMCThemePickerActivity.THEMEARRAY = new ImageAdapter();
        } else {
        	OMCThemePickerActivity.THEMEARRAY.dispose();
            gallery.requestLayout();
        }
        OMCThemePickerActivity.THEMES =  new HashMap<String, File>();
        for (File f:OMCThemePickerActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.xml");
        	if (ff.exists()) {
        		
        		OMCThemePickerActivity.THEMEARRAY.addItem(f.getName(), true);
        		OMCThemePickerActivity.THEMES.put(f.getName(), f);
        	}
        }

		gallery.setVisibility(View.VISIBLE);
		btnReload.setClickable(true);
	}
	
    public class ImageAdapter extends BaseAdapter {

    	public ArrayList<String> mThemes = new ArrayList<String>();
    	public ArrayList<Boolean> mExternal = new ArrayList<Boolean>();
    	public HashMap<String,Bitmap> mBitmaps = new HashMap<String,Bitmap>();
    	public HashMap<String, String> mCreds = new HashMap<String, String>();
    	

        public ImageAdapter() {
        }

        public void addItem(String sTheme, boolean bExternal){
        	System.out.println("Adding " + sTheme);
        	if (mThemes.size()==0 || sTheme.compareTo(mThemes.get(mThemes.size()-1))>0) {
	        	mThemes.add(sTheme);
	        	mExternal.add(bExternal);
        	} else {
        		for (int iPos = 0; iPos < mThemes.size(); iPos++) {
        			if (sTheme.compareTo(mThemes.get(iPos))>0) {
        				continue;
        			} else {
        				mThemes.add(iPos,sTheme);
        	        	mExternal.add(iPos,bExternal);
        				break;
        			}
        		}
	    			
        	}
        	mBitmaps.put(sTheme, BitmapFactory.decodeFile(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme+"/preview.jpg"));
    		OMCThemePickerActivity.THEMECREDITS = new char[3000];
    		try {
    			FileReader fr = new FileReader(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00credits.txt");
    			fr.read(OMCThemePickerActivity.THEMECREDITS);
    			mCreds.put(sTheme,String.valueOf(OMCThemePickerActivity.THEMECREDITS).trim());
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        }        

        public int getCount() {
            return mThemes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	LinearLayout ll = (LinearLayout)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.themepickerpreview, null);
        	((TextView)ll.findViewById(R.id.ThemeName)).setText(OMCThemePickerActivity.THEMEARRAY.mThemes.get(position));
        	((ImageView)ll.findViewById(R.id.ThemePreview)).setImageBitmap(mBitmaps.get(mThemes.get(position)));
        	((TextView)ll.findViewById(R.id.ThemeCredits)).setText(OMCThemePickerActivity.THEMEARRAY.mCreds.get(mThemes.get(position)));
            return ll;
        }

        public void dispose() {
        	mThemes.clear();
        	mCreds.clear();
        	mBitmaps.clear();
        	mExternal.clear();
        }
    
    }

}