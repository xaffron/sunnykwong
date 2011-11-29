package com.sunnykwong.FlingWords;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class FWFlickActivity extends Activity //implements OnClickListener, OnItemClickListener, OnItemLongClickListener 
{

	public static HashMap<String,String[]> WORDSETS;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	
	public View topLevel;
	public FWGallery gallery;	
    static AlertDialog mAD;	

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		setResult(Activity.RESULT_CANCELED);

//		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
//			finish();
//        	return;
//        }
//        OMCThemePickerActivity.SDROOT = Environment.getExternalStorageDirectory();
//		if (!OMCThemePickerActivity.SDROOT.canRead()) {
//        	Toast.makeText(this, "SD Card missing or corrupt.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
//			finish();
//        	return;
//        } 
//
		
       setContentView(R.layout.flinglayout);
	   topLevel = (View)findViewById(R.id.toplvl);
       if (Build.VERSION.SDK_INT>=11) topLevel.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);
//
//        topLevel = findViewById(this.getResources().getIdentifier("PickerTopLevel", "id", OMC.PKGNAME));
//        topLevel.setEnabled(false);
//        
//        setTitle("Swipe to Select; click & hold to Delete");
//
//        btnReload = (Button)findViewById(this.getResources().getIdentifier("btnReload", "id", OMC.PKGNAME));
//        btnReload.setOnClickListener(this);
//
//        btnGetMore = (Button)findViewById(this.getResources().getIdentifier("btnMore", "id", OMC.PKGNAME));
//        btnGetMore.setOnClickListener(this);
        
      gallery = (FWGallery)this.findViewById(R.id.gallery1);
      FlickAdapter fa = new FlickAdapter(this);
      int size = FW.CURRENTBATCH.optJSONArray("words").length();
      fa.addItem("");
      int[] seq = FW.generateRandomSequence(size);
      for (int i = 0; i < size; i++) {
    	  fa.addItem(FW.CURRENTBATCH.optJSONArray("words").optString(seq[i]));
      }
      fa.addItem("");
      try {
    	  FW.CURRENTBATCH.put("TotalUsedTimes", FW.CURRENTBATCH.optInt("TotalUsedTimes")+1);
      } catch (JSONException e) {
      }

      gallery.setAdapter(fa);
      gallery.setEnabled(true);
    }
    
//    @Override
//    protected void onResume() {
//    	// TODO Auto-generated method stub
//        refreshThemeList();
//        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
//        gallery.setOnItemClickListener(this);
//        gallery.setOnItemLongClickListener(this);
//        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
//        topLevel.setEnabled(true);
//        
//    	super.onResume();
//    }
//    
//    @Override
//    public void onClick(View v) {
//    	// TODO Auto-generated method stub
//    	if (v==btnReload) {
//			startActivity(OMC.GETSTARTERPACKINTENT);
//    		refreshThemeList();
//    	}
//    	if (v==btnGetMore) {
//			startActivity(OMC.GETEXTENDEDPACKINTENT);
//    		refreshThemeList();
//    	}
//    	if (v==btnGetMore) {
//			startActivity(OMC.GETEXTENDEDPACKINTENT);
//    		refreshThemeList();
//    	}
//     }
//
//    @Override
//    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
//    	// TODO Auto-generated method stub
//    	if (arg0==gallery) {
//
//        	gallery.setVisibility(View.INVISIBLE);
//    		btnReload.setVisibility(View.INVISIBLE);
//    		btnGetMore.setVisibility(View.INVISIBLE);
//    		
//    		Intent it = new Intent();
//    		setResult(Activity.RESULT_OK, it);
//    		it.putExtra("theme", OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
//    		
//    		finish();
//    	}
//    }
//
//@Override
//	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
//			long arg3) {
//		if (arg0==gallery) {
//			final CharSequence[] items = {"Email Theme", "Delete Theme"};
//			new AlertDialog.Builder(this)
//				.setTitle("Email or Delete Theme")
//				.setItems(items, new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int item) {
//							switch (item) {
//								case 0: //Email
//						        	String sTheme = OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition());
//									String msg = "Are you absolutely sure?";
//									if (!OMCThemePickerActivity.THEMEARRAY.mTweaked.get(sTheme)) {
//										msg = "This theme does not have the 'Tweaked' flag set; it looks like a stock theme.\n"+msg;
//									}
//									AlertDialog ad = new AlertDialog.Builder(OMCThemePickerActivity.this)
//									.setCancelable(true)
//									.setTitle("Submit " + 
//											(String)(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())) 
//											+ " to Xaffron as a new theme?")
//									.setMessage(msg)
//									.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//										
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											    //Build an ArrayList of the files in this theme
//											    ArrayList<Uri> uris = new ArrayList<Uri>();
//											    //convert from paths to Android friendly Parcelable Uri's
//											    
//									        	File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" 
//									        			+ OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
//
//									        	for (File file : f.listFiles())
//											    {
//											        Uri u = Uri.fromFile(file);
//											        uris.add(u);
//											    }
//
//												
//												Intent it = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE)
//					    		   					.setType("plain/text")
//					    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
//					    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " Theme submission")
//											    	.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
//
//											    startActivity(Intent.createChooser(it, "Packaging your theme files for email."));  
//								    		   	finish();
//										}
//									})
//									.setNegativeButton("No", new DialogInterface.OnClickListener() {
//										
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											// Do nothing
//										}
//									})
//									.create();
//									ad.show();
//					    		   	break;
//								case 1: //Delete
//									ad = new AlertDialog.Builder(OMCThemePickerActivity.this)
//									.setCancelable(true)
//									.setTitle("Delete " + 
//											(String)(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())) 
//											+ " from SD card?")
//									.setMessage("You'll have to download/extract the theme again to use it.  Are you sure?")
//									.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//										
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											OMCThemePickerActivity.this.sDefaultTheme=null;
//											OMCThemePickerActivity.THEMEARRAY.removeItem(gallery.getSelectedItemPosition());
//											OMCThemePickerActivity.this.refreshThemeList();
//										}
//									})
//									.setNegativeButton("No", new DialogInterface.OnClickListener() {
//										
//										@Override
//										public void onClick(DialogInterface dialog, int which) {
//											// Do nothing
//										}
//									})
//									.create();
//									ad.show();
//									}
//							}
//				})
//				.show();
//		}
//		return true;
//	}
//
//	public void refreshThemeList() {
//
//        topLevel.setEnabled(false);
//
//		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
//
//        	Toast.makeText(this, "SD Card not detected.\nRemember to turn off USB storage if it's still connected!", Toast.LENGTH_LONG).show();
//			setResult(Activity.RESULT_OK);
//			finish();
//        	return;
//        }
//
//        OMCThemePickerActivity.THEMEROOT = new File(OMCThemePickerActivity.SDROOT.getAbsolutePath()+"/.OMCThemes");
//        if (!OMCThemePickerActivity.THEMEROOT.exists()) {
//        	Toast.makeText(this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
//        	OMCThemePickerActivity.THEMEROOT.mkdir();
//
//			startActivity(OMC.GETSTARTERPACKINTENT);
//			
//			refreshThemeList();
//        } else if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 0) {
//        	Toast.makeText(this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
//        	OMCThemePickerActivity.THEMEROOT.mkdir();
//
//			startActivity(OMC.GETSTARTERPACKINTENT);
//        } else if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 1 && OMCThemePickerActivity.THEMEROOT.list()[0].equals(OMC.DEFAULTTHEME)) {
//        	Toast.makeText(this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
//        	OMCThemePickerActivity.THEMEROOT.mkdir();
//
//			startActivity(OMC.GETSTARTERPACKINTENT);
//        } else if (!OMC.STARTERPACKDLED) {
//
//        	mAD = new AlertDialog.Builder(this)
//			.setCancelable(true)
//			.setTitle("Starter Clock Pack")
//			.setMessage("Files in your sdcard's .OMCThemes folder will be overwritten.  Are you sure?\n(If not sure, tap Yes)")
//			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//				
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//		        	Toast.makeText(OMCThemePickerActivity.this, "Extracting starter clock pack...", Toast.LENGTH_LONG).show();
//		        	OMCThemePickerActivity.THEMEROOT.mkdir();
//					startActivity(OMC.GETSTARTERPACKINTENT);
//					mAD.cancel();
//				}
//			})
//			.setNegativeButton("No", new DialogInterface.OnClickListener() {
//				
//				@Override
//				public void onClick(DialogInterface dialog, int which) {
//					OMC.STARTERPACKDLED = true;
//					OMC.PREFS.edit().putBoolean("starterpack", true).commit();
//					mAD.cancel();
//				}
//			})
//			.create();
//        	mAD.show();
//        	
//        }
//        
//        if (OMCThemePickerActivity.THEMEARRAY == null) {
//        	OMCThemePickerActivity.THEMEARRAY = new ThemePickerAdapter();
//        } else {
//        	OMCThemePickerActivity.THEMEARRAY.dispose();
//            gallery.requestLayout();
//        }
//
//        for (File f:OMCThemePickerActivity.THEMEROOT.listFiles()) {
//        	if (!f.isDirectory()) continue;
//        	File ff = new File(f.getAbsolutePath()+"/00control.json");
//        	if (ff.exists()) {
//        		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Picker","Found theme: " + f.getName());
//        		OMCThemePickerActivity.THEMEARRAY.addItem(f.getName());
//        	}
//        }
//        topLevel.setEnabled(true);
//        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
//        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
//	}
//
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
	}
//
//	@Override
//	protected void onDestroy() {
//		// TODO Auto-generated method stub
//		super.onDestroy();
//		if (OMCThemePickerActivity.THEMEARRAY!=null) {
//			OMCThemePickerActivity.THEMEARRAY.dispose();
//			OMCThemePickerActivity.THEMEARRAY=null;
//		}
//	}
//	
}


