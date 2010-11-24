package com.sunnykwong.omc;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

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
	
	public boolean bExternalOnly;
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

        bExternalOnly = getIntent().getBooleanExtra("externalonly", false);
        
        sDefaultTheme = getIntent().getStringExtra("default");
        
		setResult(Activity.RESULT_CANCELED);

        setContentView(R.layout.themepickerlayout);

        topLevel = findViewById(R.id.PickerTopLevel);
        topLevel.setEnabled(false);
        
        setTitle("Swipe to Select; click & hold to Delete");

        btnReload = (Button)findViewById(R.id.btnReload);
        btnReload.setOnClickListener(this);

        btnGetMore = (Button)findViewById(R.id.btnMore);
        btnGetMore.setOnClickListener(this);
        
        gallery = (Gallery)this.findViewById(R.id.gallery);
        refreshThemeList();

        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setOnItemClickListener(this);
        gallery.setOnItemLongClickListener(this);
        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
        topLevel.setEnabled(true);
        
    }
    
    @Override
    public void onClick(View v) {
    	// TODO Auto-generated method stub
    	if (v==btnReload) {
    		if (OMC.DEBUG) Log.i("OMCSkin","Refreshing Themes");
    		refreshThemeList();
    	}
    	if (v==btnGetMore) {
    		Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("http://xaffron.blogspot.com/search/label/omctheme"));
    		startActivityForResult(it, 999);
    		return;
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
    		it.putExtra("external", OMCThemePickerActivity.THEMEARRAY.mExternal.get(gallery.getSelectedItemPosition()));
    		
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
										OMC.deleteOneThemeFromCache(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
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

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) && bExternalOnly) {
        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCThemePickerActivity.SDROOT = Environment.getExternalStorageDirectory();
        OMCThemePickerActivity.THEMEROOT = new File(OMCThemePickerActivity.SDROOT.getAbsolutePath()+"/OMC");
        if (!OMCThemePickerActivity.THEMEROOT.exists() || !OMC.STARTERPACKDLED) {
        	Toast.makeText(this, "Downloading starter clock pack...", Toast.LENGTH_LONG).show();
        	OMCThemePickerActivity.THEMEROOT.mkdir();

			startActivity(OMC.GETSTARTERPACKINTENT);
			
			finish();
        }
        
        if (OMCThemePickerActivity.THEMEARRAY == null) {
        	OMCThemePickerActivity.THEMEARRAY = new ThemePickerAdapter();
        } else {
        	OMCThemePickerActivity.THEMEARRAY.dispose();
            gallery.requestLayout();
        }

        OMCThemePickerActivity.THEMES =  new HashMap<String, File>();
        if (!bExternalOnly) {
        	OMCThemePickerActivity.THEMEARRAY.addItem("LockscreenLook", false);
		}

        for (File f:OMCThemePickerActivity.THEMEROOT.listFiles()) {
        	if (!f.isDirectory()) continue;
        	File ff = new File(f.getAbsolutePath()+"/00control.xml");
        	if (ff.exists()) {
        		
        		OMCThemePickerActivity.THEMEARRAY.addItem(f.getName(), true);
        		OMCThemePickerActivity.THEMES.put(f.getName(), f);
        	}
        }
        topLevel.setEnabled(true);
        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
	}

//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//		if (requestCode==999) {
//			refreshThemeList();
//		}
//	}	

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
    	public ArrayList<Boolean> mExternal = new ArrayList<Boolean>();
    	public HashMap<String,Bitmap> mBitmaps = new HashMap<String,Bitmap>();
    	public HashMap<String, String> mCreds = new HashMap<String, String>();
    	public HashMap<String, String> mNames = new HashMap<String, String>();
    	
    	

        public ThemePickerAdapter() {
        }

        public int addItem(final String sTheme, boolean bExternal){
        	int result=0;
        	if (mThemes.size()==0 || sTheme.compareTo(mThemes.get(mThemes.size()-1))>0) {
	        	mThemes.add(sTheme);
	        	mExternal.add(bExternal);
	        	result=0; //position of the add
        	} else {
        		for (int iPos = 0; iPos < mThemes.size(); iPos++) {
        			if (sTheme.compareTo(mThemes.get(iPos))>0) {
        				continue;
        			} else {
        				mThemes.add(iPos,sTheme);
        	        	mExternal.add(iPos,bExternal);
        	        	result= iPos;
        				break;
        			}
        		}
	    			
        	}
        	if (!bExternal) {
        		mBitmaps.put(sTheme, BitmapFactory.decodeResource(OMC.RES, R.drawable.llpreview));
        		mNames.put(sTheme, "Lockscreen Look");
    			mCreds.put(sTheme, "LOCKSCREEN LOOK (S. Kwong, 18 Oct 2010)\nThe default Android Lockscreen Look.");
        		return result;
        	}
        	mBitmaps.put(sTheme, BitmapFactory.decodeFile(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme+"/000preview.jpg"));
    		char[] cCredits = new char[3000];
    		try {
    			FileReader fr = new FileReader(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00name.txt");
    			fr.read(cCredits);
    			mNames.put(sTheme,String.valueOf(cCredits).trim());
    			fr.close();
    			
    			fr = new FileReader(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00credits.txt");
    			fr.read(cCredits);
    			mCreds.put(sTheme,String.valueOf(cCredits).trim());
    			fr.close();
    			
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
        	mExternal.remove(pos);
        	mBitmaps.remove(sTheme);
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
        	LinearLayout ll = (LinearLayout)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.themepickerpreview, null);
        	((TextView)ll.findViewById(R.id.ThemeName)).setText(mNames.get(mThemes.get(position)));
        	((ImageView)ll.findViewById(R.id.ThemePreview)).setImageBitmap(mBitmaps.get(mThemes.get(position)));
        	((TextView)ll.findViewById(R.id.ThemeCredits)).setText(mCreds.get(mThemes.get(position)));
            return ll;
        }

        public void dispose() {
        	mThemes.clear();
        	mCreds.clear();
        	mBitmaps.clear();
        	mExternal.clear();
        	mNames.clear();
        }
    
    }

}