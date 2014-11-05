package com.sunnykwong.omc;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
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

public class OMCThemePickerActivity extends Activity {

	public static HashMap<String,String[]> ELEMENTS;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;
	public static ThemePickerAdapter THEMEARRAY;
	public static String RAWCONTROLFILE;
	
	public String sDefaultTheme;
	public int iAppWidgetID;
	
	public View topLevel;
	public Button btnReload,btnGetMore;
	public Gallery gallery;
	public TextView tvCredits;
	public AsyncTask<String, String, String> atLoading;
    static AlertDialog mAD;	
    OMCThemePickerListener mListener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    	if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"ThemePicker","OnCreate");
        getWindow().setWindowAnimations(android.R.style.Animation_Toast);
        getWindow().setFormat(PixelFormat.RGB_565);

        sDefaultTheme = getIntent().getStringExtra("default");
        iAppWidgetID = getIntent().getIntExtra("appWidgetID", 0);
        
		setResult(Activity.RESULT_CANCELED);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        	Toast.makeText(this, OMC.RString("sdcardNotDetected"), Toast.LENGTH_LONG).show();
			finish();
        	return;
        }
        OMCThemePickerActivity.SDROOT = new File(OMC.WORKDIR);
		if (!OMCThemePickerActivity.SDROOT.canRead()) {
        	Toast.makeText(this, OMC.RString("sdcardMissingOrCorrupt"), Toast.LENGTH_LONG).show();
			finish();
        	return;
        }

        setContentView(OMC.RLayoutId("themepickerlayout"));

        topLevel = findViewById(OMC.RId("PickerTopLevel"));
        topLevel.setEnabled(false);
        
        setTitle(OMC.RString("widgetThemeTitle"));

        mListener = new OMCThemePickerListener();
        
        btnReload = (Button)findViewById(OMC.RId("btnReload"));
        btnReload.setOnClickListener(mListener);

        btnGetMore = (Button)findViewById(OMC.RId("btnMore"));
        btnGetMore.setOnClickListener(mListener);
        
        gallery = (Gallery)this.findViewById(OMC.RId("gallery"));
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	// TODO Auto-generated method stub
        refreshThemeList();
        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
        gallery.setOnItemClickListener(mListener);
        gallery.setOnItemLongClickListener(mListener);
        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
        topLevel.setEnabled(true);
        
    }
    
	public void refreshThemeList() {

        topLevel.setEnabled(false);

		if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			try {
				Toast.makeText(this, OMC.RString("sdcardNotDetected"), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
			setResult(Activity.RESULT_OK);
			finish();
        	return;
        }

        OMCThemePickerActivity.THEMEROOT = new File(OMCThemePickerActivity.SDROOT.getAbsolutePath());
        if (!OMCThemePickerActivity.THEMEROOT.exists()) {
			try {
				Toast.makeText(this, OMC.RString("extractingStarterClockPack"), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
        	OMCThemePickerActivity.THEMEROOT.mkdir();
        	OMC.setupDefaultTheme();
			startActivity(OMC.GETSTARTERPACKINTENT);
			
			refreshThemeList();
        } else if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 0) {
			try {
				Toast.makeText(this, OMC.RString("extractingStarterClockPack"), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
        	OMCThemePickerActivity.THEMEROOT.mkdir();
        	OMC.setupDefaultTheme();
			startActivity(OMC.GETSTARTERPACKINTENT);
        } else if (OMCThemePickerActivity.THEMEROOT.listFiles().length == 1 && OMCThemePickerActivity.THEMEROOT.list()[0].equals(OMC.DEFAULTTHEME)) {
			try {
				Toast.makeText(this, OMC.RString("extractingStarterClockPack"), Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				e.printStackTrace();
			}
        	OMCThemePickerActivity.THEMEROOT.mkdir();
        	OMC.setupDefaultTheme();
			startActivity(OMC.GETSTARTERPACKINTENT);
        } else if (!OMC.STARTERPACKDLED) {

        	mAD = new AlertDialog.Builder(this)
			.setCancelable(true)
			.setTitle(OMC.RString("starterClockPack"))
			.setMessage(OMC.RString("starterClockPackText"))
			.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try {
						Toast.makeText(OMCThemePickerActivity.this, OMC.RString("extractingStarterClockPack"), Toast.LENGTH_LONG).show();
					} catch (Exception e) {
						e.printStackTrace();
					}
		        	OMCThemePickerActivity.THEMEROOT.mkdir();
		        	OMC.setupDefaultTheme();
					startActivity(OMC.GETSTARTERPACKINTENT);
					mAD.cancel();
				}
			})
			.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
				
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

        atLoading = new AsyncTask<String, String, String> () {
			@Override
			protected String doInBackground(String... params) {
		        for (File f:OMCThemePickerActivity.THEMEROOT.listFiles()) {
		        	if (isCancelled()) return null;
		        	if (!f.isDirectory()) continue;
		        	File ff = new File(f.getAbsolutePath()+"/00control.json");
		        	if (ff.exists()) {
		        		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Picker","Found theme: " + f.getName());
		        		if (OMCThemePickerActivity.THEMEARRAY!=null) 
		        			OMCThemePickerActivity.THEMEARRAY.addItem(f.getName());
		        	}
		        }
		        
				return null;
			}
			@Override
			protected void onPostExecute(String result) {
		        topLevel.setEnabled(true);
        		if (OMCThemePickerActivity.THEMEARRAY!=null) {
			        gallery.setAdapter(OMCThemePickerActivity.THEMEARRAY);
			        gallery.setSelection(OMCThemePickerActivity.THEMEARRAY.getPosition(sDefaultTheme));
        		}
			};
		}.execute("");
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
		if (atLoading!=null && !atLoading.isCancelled()) {
			atLoading.cancel(true);
		}
		if (OMCThemePickerActivity.THEMEARRAY!=null) {
			OMCThemePickerActivity.THEMEARRAY.dispose();
			OMCThemePickerActivity.THEMEARRAY=null;
		}
	}
	
    public class ThemePickerAdapter extends BaseAdapter {

    	public ArrayList<String> mThemes = new ArrayList<String>();
    	public ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>();
    	public HashMap<String, String> mCreds = new HashMap<String, String>();
    	public HashMap<String, String> mNames = new HashMap<String, String>();
    	public HashMap<String, Boolean> mTweaked = new HashMap<String, Boolean>();
    	public HashMap<String, Boolean> mTesterOnly = new HashMap<String, Boolean>();
    	public HashMap<String, String[]> mTags = new HashMap<String, String[]>();
    	

        public ThemePickerAdapter() {
        }

        public int addItem(final String sTheme){
        	int result=0;
        	if (mThemes.size()==0 || sTheme.compareTo(mThemes.get(mThemes.size()-1))>0) {
	        	mThemes.add(sTheme);
	        	mBitmaps.add(OMC.PLACEHOLDERBMP);
	        	result=0; //position of the add
        	} else {
        		for (int iPos = 0; iPos < mThemes.size(); iPos++) {
        			if (sTheme.compareTo(mThemes.get(iPos))>0) {
        				continue;
        			} else {
        				mThemes.add(iPos,sTheme);
        	        	mBitmaps.add(iPos,OMC.PLACEHOLDERBMP);
        	        	result= iPos;
        				break;
        			}
        		}
	    			
        	}

    		try {
				BufferedReader in = new BufferedReader(new FileReader(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+ "/" + sTheme+"/00control.json"),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
				JSONObject oResult = new JSONObject(sb.toString());
				sb.setLength(0);
    			mNames.put(sTheme,oResult.optString("name"));
    			mTweaked.put(sTheme, Boolean.valueOf(oResult.optBoolean("tweaked")));
    			mTesterOnly.put(sTheme, Boolean.valueOf(oResult.optBoolean("testeronly")));
    			mCreds.put(sTheme,"Author: " + oResult.optString("author") + "  (" +oResult.optString("date")+ ")\n" + oResult.optString("credits"));
    			if (oResult.has("tags")) {
    				final JSONArray tags = oResult.getJSONArray("tags");
    				final String[] tempStrArray = new String[tags.length()];
    				for (int i=0;i<tags.length();i++) {
    					tempStrArray[i]=tags.getString(i);
    				}
    				mTags.put(sTheme, tempStrArray);
    			} else {
    				mTags.put(sTheme, new String[0]);
    			}
    			
       			oResult = null;
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		return result;
        }        

        public void removeItem(int pos){
        	if (mThemes.size()==0)return;
        	String sTheme = mThemes.get(pos);
        	if (sTheme.equals(OMC.DEFAULTTHEME)) {
        		Toast.makeText(OMCThemePickerActivity.this, OMC.RString("defaultThemeNotRemovable"), Toast.LENGTH_LONG).show();
        		return;
        	}
        	mThemes.remove(pos);
        	mBitmaps.remove(pos);
        	mCreds.remove(sTheme);
        	mTweaked.remove(sTheme);
        	mTesterOnly.remove(sTheme);
        	File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + sTheme);
        	OMC.THEMEMAP.clear();
        	OMC.removeDirectory(f);
        	OMC.purgeBitmapCache();
        	OMC.purgeTypefaceCache();
        }        

        @Override
		public int getCount() {
            return mThemes.size();
        }

        @Override
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

        @Override
		public long getItemId(int position) {
            return position;
        }

        @Override
		public View getView(int position, View convertView, ViewGroup parent) {
        	LinearLayout ll = (LinearLayout)((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(OMC.RLayoutId("themepickerpreview"), null);
        	((TextView)ll.findViewById(OMC.RId("ThemeName"))).setTypeface(OMC.GEOFONT);
        	
        	//  If the theme list isn't loaded yet, just return a blank screen!
        	if (position < 0 || position > mThemes.size()) return ll;

        	// Tester only clocks are colored RED
        	if (mTesterOnly.get(mThemes.get(position)).booleanValue()) {
	    		((TextView)ll.findViewById(OMC.RId("ThemeName"))).setTextColor(Color.RED);
	    		((TextView)ll.findViewById(OMC.RId("ThemeCredits"))).setTextColor(Color.RED);
        	}
        	if (mNames.get(mThemes.get(position)) != null) {
        		((TextView)ll.findViewById(OMC.RId("ThemeName"))).setText(mNames.get(mThemes.get(position)));
        	} else {
        		((TextView)ll.findViewById(OMC.RId("ThemeName"))).setText(OMC.RString("corruptPlsDelete"));
        	}
        	try {
	        	if (mTweaked.get(mThemes.get(position)).booleanValue()) {
	        		ImageView iv = ((ImageView)ll.findViewById(OMC.RId("LikeFlag")));
	        		iv.setImageResource(OMC.RDrawableId("tweaked"));
	        		iv.setOnClickListener(new View.OnClickListener() {
	    				
	    				@Override
	    				public void onClick(View v) {
	    					
	    					final Dialog d = new Dialog(OMCThemePickerActivity.this);
	    					d.setContentView(OMC.RLayoutId("taglegend"));
	    					d.setTitle("Legend");
	    					d.setOnKeyListener(new DialogInterface.OnKeyListener() {
	    						
	    						@Override
	    						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
	    							// TODO Auto-generated method stub
	    							dialog.dismiss();
	    							return true;
	    						}
	    					});
	    					d.setCanceledOnTouchOutside(true);
	    					d.show();
	    				}
	    			});
	        		iv.bringToFront();
	        	}
        	} catch (NullPointerException e) {
        		//v1.2.8 fix issue where theme flag not set
        	}
        	LinearLayout tagll = ((LinearLayout)ll.findViewById(OMC.RId("tags")));
        	if (mTags.get(mThemes.get(position))!=null) {
	        	for (String sTag: mTags.get(mThemes.get(position))) {
	        		ImageView iv = new ImageView(OMCThemePickerActivity.this);
	        		iv.setAdjustViewBounds(true);
	        		try{
	        			iv.setImageResource(OMC.RDrawableId(sTag));
	            		tagll.addView(iv);
	        		} catch (Exception e) {
	        			
	        		}
	        	}
        	}
        	tagll.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					final Dialog d = new Dialog(OMCThemePickerActivity.this);
					d.setContentView(OMC.RLayoutId("taglegend"));
					d.setTitle("Legend");
					d.setOnKeyListener(new DialogInterface.OnKeyListener() {
						
						@Override
						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
							// TODO Auto-generated method stub
							dialog.dismiss();
							return true;
						}
					});
					d.setCanceledOnTouchOutside(true);
					d.show();
				}
			});
    		tagll.bringToFront();
        	
        	
        	BitmapFactory.Options bo = new BitmapFactory.Options();
        	bo.inDither=true;
        	bo.inPreferredConfig = Bitmap.Config.RGB_565;
        	Bitmap bmp = BitmapFactory.decodeFile(
    				OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" + mThemes.get(position) +"/000preview.jpg",bo);
        	if (mBitmaps.size()>=position) mBitmaps.set(position, bmp);
    		((ImageView)ll.findViewById(OMC.RId("ThemePreview")))
    				.setImageBitmap(bmp);
        	((TextView)ll.findViewById(OMC.RId("ThemeCredits"))).setText(mCreds.get(mThemes.get(position)));
        	// v140:  Was assuming that the user can't fling faster than the system can reload bitmaps -
        	// This is no longer true in newer faster phones, and we've been getting "bitmap already recycled" errors.
        	// So we're removing this assumption/optimization, and will let Android take care of cleaning up.
//        	if (position>5 && mThemes.size()-position>5) {
//	        	int leftclear = position-5 >= 0 ? position-5 : 0;
//	        	if (mBitmaps.get(leftclear)!=OMC.PLACEHOLDERBMP) {
//	        		if (mBitmaps.get(leftclear)!=null)	mBitmaps.get(leftclear).recycle();
//	        		mBitmaps.set(leftclear, OMC.PLACEHOLDERBMP);
//	        	}
//	        	int rightclear = position+5 < mThemes.size() ? position+5 : mThemes.size();
//	        	if (mBitmaps.get(rightclear)!=OMC.PLACEHOLDERBMP) {
//	        		if (mBitmaps.get(rightclear)!=null)	mBitmaps.get(rightclear).recycle();
//	        		mBitmaps.set(rightclear, OMC.PLACEHOLDERBMP);
//	        	}
//        	}
//
            return ll;
        }
        
        public void dispose() {
        	mThemes.clear();
        	mCreds.clear();
        	mNames.clear();
        	mTweaked.clear();
        	System.gc();
        }
    
    }

    class OMCThemePickerListener implements OnClickListener, OnItemClickListener, OnItemLongClickListener  {
        @Override
        public void onClick(View v) {
        	// TODO Auto-generated method stub
        	if (v==btnReload) {
    			startActivity(OMC.GETSTARTERPACKINTENT);
        		refreshThemeList();
        	}
        	if (v==btnGetMore) {
    			startActivity(OMC.GETEXTENDEDPACKINTENT);
        		refreshThemeList();
        	}
         }

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        	if (arg0==gallery) {

            	gallery.setVisibility(View.INVISIBLE);
        		btnReload.setVisibility(View.INVISIBLE);
        		btnGetMore.setVisibility(View.INVISIBLE);
        		
        		final String sThemeName = OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition());
        		Intent it = new Intent();
        		
        		it.putExtra("theme", sThemeName);
        		setResult(Activity.RESULT_OK, it);

           		JSONObject newTheme = OMC.getTheme(OMCThemePickerActivity.this, sThemeName, false);
//               	Toast.makeText(OMCThemePickerActivity.this, newTheme.optString("name") + OMC.RString("postselected"), Toast.LENGTH_SHORT).show();
                	
            	OMC.PREFS.edit()
            	.putString("widgetTheme"+iAppWidgetID, OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()))
            	.putString("widgetTheme", OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()))
            	.putString("widgetThemeLong"+iAppWidgetID, newTheme.optString("name"))
            	.putString("widgetThemeLong", newTheme.optString("name"))
        		.commit();

               	// Clear the cache for a clean slate
               	OMC.purgeBitmapCache();
               	OMC.purgeEmailCache();
           		OMC.purgeTypefaceCache();
               	OMC.THEMEMAP.clear();
               	OMC.WIDGETBMPMAP.clear();

            	OMCThemePickerActivity.this.finish();
        	}
        }

    @Override
    	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
    			long arg3) {
    		if (arg0==gallery) {
    			final CharSequence[] items = {OMC.RString("emailTheme"), OMC.RString("deleteTheme"), OMC.RString("submitThemeToDev")};
    			new AlertDialog.Builder(OMCThemePickerActivity.this)
    				.setTitle(OMC.RString("emailOrDeleteTheme"))
    				.setItems(items, new DialogInterface.OnClickListener() {
    						@Override
    						public void onClick(DialogInterface dialog, int item) {
    							switch (item) {
    								case 0: //Email
    						        	String sTheme = OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition());
    									String msg = OMC.RString("emailThemeText");
    									AlertDialog ad = new AlertDialog.Builder(OMCThemePickerActivity.this)
    									.setCancelable(true)
    									.setTitle(OMC.RString("emailThemeConfirm1") + 
    											(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())) 
    											+ OMC.RString("emailThemeConfirm2"))
    									.setMessage(msg)
    									.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
    										
    										@Override
    										public void onClick(DialogInterface dialog, int which) {
    											    //Build an ArrayList of the files in this theme
    											    ArrayList<Uri> uris = new ArrayList<Uri>();
    											    //convert from paths to Android friendly Parcelable Uri's

    											    File outzip = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath()+"/tmp/",OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())+".omc");
    											    if (outzip.exists()) outzip.delete();
    											    else outzip.mkdirs();

    											    File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" 
    									        			+ OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
    									        	
    									        	try {
    										        	ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outzip),8192));
    										        	for (File file : f.listFiles())
    												    {
    										        		ZipEntry ze = new ZipEntry(new ZipEntry(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()) + "/" + file.getName()));
    											        	zos.putNextEntry(ze);
    													    FileInputStream ffis = new FileInputStream(file);
    														try {
    															//Absolute luxury 1980 style!  Using an 8k buffer.
    															byte[] buffer = new byte[8192];
    															int iBytesRead=0;
    															while ((iBytesRead=ffis.read(buffer))!= -1){
    																zos.write(buffer, 0, iBytesRead);
    															}
    															zos.flush();
    															zos.closeEntry();
    														} catch (Exception e) {
    											        		Log.w(OMC.OMCSHORT + "Picker","cannot zip, zip error below");
    															e.printStackTrace();
    														}
    														ffis.close();
    											        	
    												    }
    								
    										        	zos.finish();
    										        	zos.close();

    												} catch (Exception e) {
    													// File exists and read-only?  Shouldn't happen
    									        		Log.w(OMC.OMCSHORT + "Picker","cannot zip, file already open or RO");
    													e.printStackTrace();
    												}
    									        	
    										        Uri u = Uri.fromFile(outzip);
    										        uris.add(u);
    										        if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Picker","uris:"+uris);
    										        if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Picker","outzip:"+outzip);
    										        if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Picker","outzipsize:"+outzip.length());
    												Intent it = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE)
    					    		   					.setType("plain/text")
    					    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()) + " for " + OMC.APPNAME)
    											    	.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);

    											    startActivity(Intent.createChooser(it, OMC.RString("emailThemeChooser")));  
    											    OMCThemePickerActivity.this.finish();
    										}
    									})
    									.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
    										
    										@Override
    										public void onClick(DialogInterface dialog, int which) {
    											// Do nothing
    										}
    									})
    									.create();
    									ad.show();
    					    		   	break;
    								case 1: //Delete
    									ad = new AlertDialog.Builder(OMCThemePickerActivity.this)
    									.setCancelable(true)
    									.setTitle(OMC.RString("deleteThemeConfirm1") + 
    											(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())) 
    											+ OMC.RString("deleteThemeConfirm2"))
    									.setMessage(OMC.RString("deleteThemeConfirm3"))
    									.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
    										
    										@Override
    										public void onClick(DialogInterface dialog, int which) {
    							        		// Purge all caches to eliminate "non-sticky" settings bug
    							            	OMC.purgeBitmapCache();
    							            	OMC.purgeImportCache();
    							            	OMC.purgeEmailCache();
    							        		OMC.purgeTypefaceCache();
    							        		OMC.THEMEMAP.clear();
    							            	OMC.WIDGETBMPMAP.clear();

    											OMCThemePickerActivity.this.sDefaultTheme=null;
    											OMCThemePickerActivity.THEMEARRAY.removeItem(gallery.getSelectedItemPosition());
    											OMCThemePickerActivity.this.refreshThemeList();
    										}
    									})
    									.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
    										
    										@Override
    										public void onClick(DialogInterface dialog, int which) {
    											// Do nothing
    										}
    									})
    									.create();
    									ad.show();
    									break;
    							case 2: //Submit to Dev
    					        	sTheme = OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition());
    								msg = OMC.RString("areYouAbsolutelySure");
    								if (!OMCThemePickerActivity.THEMEARRAY.mTweaked.get(sTheme)) {
    									msg = OMC.RString("areYouAbsolutelySureText")+msg;
    								}
    								ad = new AlertDialog.Builder(OMCThemePickerActivity.this)
    								.setCancelable(true)
    								.setTitle(OMC.RString("submitThemeToDevConfirm1") + 
    										(OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition())) 
    										+ OMC.RString("submitThemeToDevConfirm2"))
    								.setMessage(msg)
    								.setPositiveButton(OMC.RString("_yes"), new DialogInterface.OnClickListener() {
    									
    									@Override
    									public void onClick(DialogInterface dialog, int which) {
    										    //Build an ArrayList of the files in this theme
    										    ArrayList<Uri> uris = new ArrayList<Uri>();
    										    //convert from paths to Android friendly Parcelable Uri's
    										    
    								        	File f = new File(OMCThemePickerActivity.THEMEROOT.getAbsolutePath() + "/" 
    								        			+ OMCThemePickerActivity.THEMEARRAY.mThemes.get(gallery.getSelectedItemPosition()));
    	
    								        	for (File file : f.listFiles())
    										    {
    										        Uri u = Uri.fromFile(file);
    										        uris.add(u);
    										    }
    	
    											
    											Intent it = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE)
    				    		   					.setType("plain/text")
    				    		   					.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"skwong@consultant.com"})
    				    		   					.putExtra(android.content.Intent.EXTRA_SUBJECT, OMC.APPNAME + " Theme submission")
    										    	.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    	
    										    startActivity(Intent.createChooser(it, OMC.RString("emailThemeChooser")));  
    										    OMCThemePickerActivity.this.finish();
    									}
    								})
    								.setNegativeButton(OMC.RString("_no"), new DialogInterface.OnClickListener() {
    									
    									@Override
    									public void onClick(DialogInterface dialog, int which) {
    										// Do nothing
    									}
    								})
    								.create();
    								ad.show();
    								break;
    							default:
    								// do nothing
    						}

    					}
    				}).show();
    		}
    		return true;
    	}

    }
}