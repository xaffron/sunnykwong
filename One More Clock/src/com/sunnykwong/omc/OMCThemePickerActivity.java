package com.sunnykwong.omc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.DialogInterface;
import android.content.Intent;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class OMCThemePickerActivity extends Activity implements OnClickListener, OnItemClickListener, OnItemLongClickListener {

	public static HashMap<String,String[]> ELEMENTS;
	public static HashMap<String,File> THEMES;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ThemePickerAdapter THEMEARRAY;
	public static String RAWCONTROLFILE;
	
	public String sDefaultTheme;
	
	public View topLevel;
	public Button btnReload,btnGetMore;
	public Gallery gallery;
	public TextView tvCredits;
	
    static AlertDialog mAD;	

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setWindowAnimations(android.R.style.Animation_Toast);

        sDefaultTheme = getIntent().getStringExtra("default");
        
		setResult(Activity.RESULT_CANCELED);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			finish();
        	return;
        }
        OMCThemePickerActivity.SDROOT = Environment.getExternalStorageDirectory();
		if (!OMCThemePickerActivity.SDROOT.canRead()) {
        	Toast.makeText(this, "SD Card missing or corrupt.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			finish();
        	return;
        }

        setContentView(this.getResources().getIdentifier("themepickerlayout", "layout", OMC.PKGNAME));

        topLevel = findViewById(this.getResources().getIdentifier("PickerTopLevel", "id", OMC.PKGNAME));
        topLevel.setEnabled(false);
        
        setTitle("Swipe to Select; click & hold to Delete");

        btnReload = (Button)findViewById(this.getResources().getIdentifier("btnReload", "id", OMC.PKGNAME));
        btnReload.setOnClickListener(this);

        btnGetMore = (Button)findViewById(this.getResources().getIdentifier("btnMore", "id", OMC.PKGNAME));
        btnGetMore.setOnClickListener(this);
        
        gallery = (Gallery)this.findViewById(this.getResources().getIdentifier("gallery", "id", OMC.PKGNAME));
        refreshThemeList();

        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setOnItemClickListener(this);
        gallery.setOnItemLongClickListener(this);
        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
        topLevel.setEnabled(true);
        
    }
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
        refreshThemeList();
    	super.onResume();
    }
    
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v==btnReload) {
    		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Skin","Refreshing Themes");
    		refreshThemeList();
    	}
    	if (v==btnGetMore) {
    		Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("http://xaffron.blogspot.com/search/label/omctheme"));
    		startActivity(it);
    		finish();
    	}
     }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
    	// TODO Auto-generated method stub
    	if (arg0==gallery) {

        	gallery.setVisibility(View.INVISIBLE);
    		btnReload.setVisibility(View.INVISIBLE);
    		btnGetMore.setVisibility(View.INVISIBLE);
    		
    		Intent it = new Intent();
    		setResult(Activity.RESULT_OK, it);
    		it.putExtra("theme", OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
    		
    		finish();
    	}
    }

@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		if (arg0==gallery) {
			AlertDialog ad = new AlertDialog.Builder(this)
								.setCancelable(true)
								.setTitle("Delete this theme from SD card?")
								.setMessage("You'll have to download the theme again to use it.  Are you sure?")
								.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										Toast.makeText(getApplicationContext(), "No-Op right now", Toast.LENGTH_SHORT).show();
										OMCThemePickerActivity.this.sDefaultTheme=null;
										OMCThemePickerActivity.THEMEARRAY.removeItem(gallery.getSelectedItemPosition());
										OMCThemePickerActivity.this.refreshThemeList();
									}
								})
								.setNegativeButton("No", new DialogInterface.OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// Do nothing
									}
								})
								.create();
			ad.show();
		}
		return true;
	}

	public void refreshThemeList() {

        topLevel.setEnabled(false);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCThemePickerActivity.THEMEROOT = new File(OMCThemePickerActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCThemePickerActivity.THEMEROOT.exists()) {
        	Toast.makeText(this, "Downloading starter clock pack...", Toast.LENGTH_LONG).show();
        	OMCThemePickerActivity.THEMEROOT.mkdir();

			startActivity(OMC.GETSTARTERPACKINTENT);
			
			finish();
        } else if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 0) {
	        	Toast.makeText(this, "Downloading starter clock pack...", Toast.LENGTH_LONG).show();
	        	OMCThemePickerActivity.THEMEROOT.mkdir();

				startActivity(OMC.GETSTARTERPACKINTENT);
				
				finish();
        } else if (!OMC.STARTERPACKDLED) {

        	mAD = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle("Updated Starter Clock Pack")
			.setMessage("Any theme customizations you have made in your sdcard's OMC folder will be overwriten.  Are you sure?\n(If not sure, tap Yes)")
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
		        	Toast.makeText(OMCThemePickerActivity.this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
		        	OMCThemePickerActivity.THEMEROOT.mkdir();
					startActivity(OMC.GETSTARTERPACKINTENT);
					mAD.cancel();
					finish();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					OMC.STARTERPACKDLED = true;
					OMC.PREFS.edit().putBoolean("starterpack", true).commit();
					mAD.cancel();
				}
			})
			.create();
        	mAD.show();
        	
        }
        
        if (OMCThemePickerActivity.THEMEARRAY == null) {
        	OMCThemePickerActivity.THEMEARRAY = new ThemePickerAdapter();
        } else {
        	OMCThemePickerActivity.THEMEARRAY.dispose();
            gallery.requestLayout();
        }

        OMCThemePickerActivity.THEMES =  new HashMap<String, File>();

        for (File f:OMCThemePickerActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.json");
        	if (ff.exists()) {
        		
        		OMCThemePickerActivity.THEMEARRAY.addItem(f.getName());
        		OMCThemePickerActivity.THEMES.put(f.getName(), f);
        	}
        }
        topLevel.setEnabled(true);
        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if (OMCThemePickerActivity.THEMEARRAY!=null) {
			OMCThemePickerActivity.THEMEARRAY.dispose();
			OMCThemePickerActivity.THEMEARRAY=null;
		}
	}
	
    public class ThemePickerAdapter extends BaseAdapter {

    	public ArrayList<String> mThemes = new ArrayList<String>();
    	public HashMap<String, String> mCreds = new HashMap<String, String>();
    	public HashMap<String, String> mNames = new HashMap<String, String>();
    	
    	

        public ThemePickerAdapter() {
        }

        public int addItem(final String sTheme){
        	int result=0;
        	if (mThemes.size()==0 || sTheme.compareTo(mThemes.get(mThemes.size()-1))>0) {
	        	mThemes.add(sTheme);
	        	result=0; //position of the add
        	} else {
        		for (int iPos = 0; iPos < mThemes.size(); iPos++) {
        			if (sTheme.compareTo(mThemes.get(iPos))>0) {
        				continue;
        			} else {
        				mThemes.add(iPos,sTheme);
        	        	result= iPos;
        				break;
        			}
        		}
	    			
        	}

    		try {
				BufferedReader in = new BufferedReader(new FileReader(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00control.json"),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[16384];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
				JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
    			mNames.put(sTheme,oResult.optString("name"));
    			mCreds.put(sTheme,"Author: " + oResult.optString("author") + "  (" +oResult.optString("date")+ ")\n" + oResult.optString("credits"));
    			
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		return result;
        }        

        public void removeItem(int pos){
        	if (mThemes.size()==0)return;
        	String sTheme = mThemes.get(pos);
        	if (sTheme.equals("LockscreenLook")) {
        		Toast.makeText(OMCThemePickerActivity.this, "The default theme is not removable!", Toast.LENGTH_LONG).show();
        		return;
        	}
        	mThemes.remove(pos);
        	//mBitmaps.remove(sTheme);
        	mCreds.remove(sTheme);
        	File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme);
        	OMC.removeDirectory(f);
        	OMC.purgeBitmapCache();
        	OMC.purgeTypefaceCache();
        }        

        public int getCount() {
            return mThemes.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public int getPosition(String sTheme) {
        	if (sTheme==null) return 0;
        	for (int position = 0; position < mThemes.size(); position ++) {
        		if (mThemes.get(position).equals(sTheme)){
        			return position;
        		}
        	}
        	return 0;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
        	LinearLayout ll = (LinearLayout)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(getResources().getIdentifier("themepickerpreview", "layout", OMC.PKGNAME), null);
        	((TextView)ll.findViewById(getResources().getIdentifier("ThemeName", "id", OMC.PKGNAME))).setText(mNames.get(mThemes.get(position)));
        	if (mThemes.get(position).equals("LockscreenLook")) {
        		((ImageView)ll.findViewById(getResources().getIdentifier("ThemePreview", "id", OMC.PKGNAME)))
        				.setImageBitmap(BitmapFactory.decodeResource(
        				OMC.RES, getResources().getIdentifier("llpreview", "drawable", OMC.PKGNAME)));
        	} else {
        		((ImageView)ll.findViewById(getResources().getIdentifier("ThemePreview", "id", OMC.PKGNAME)))
        				.setImageBitmap(BitmapFactory.decodeFile(
        				OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + mThemes.get(position) +"/000preview.jpg"));
        	}
        	((TextView)ll.findViewById(getResources().getIdentifier("ThemeCredits", "id", OMC.PKGNAME))).setText(mCreds.get(mThemes.get(position)));
            return ll;
        }

        public void dispose() {
        	mThemes.clear();
        	mCreds.clear();
        	mNames.clear();
        	System.gc();
        }
    
    }

}