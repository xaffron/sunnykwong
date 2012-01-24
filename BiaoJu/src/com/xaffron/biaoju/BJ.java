package com.xaffron.biaoju;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Application;
import android.os.Environment;
import android.widget.Toast;

public class BJ extends Application {
	
	static GM MASTER;
	static TurnActivity TACT;
	static Combat CURRENTFIGHT;
	
	@Override
	public void onCreate() {
		super.onCreate();
		loadFlaresFromJSON();
	}
	
	static public boolean loadFlaresFromJSON() {
		try {
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				return false;
		}	catch (Exception e) {}
		return true;
/*				
//			Environment.getExternalStoragePublicDirectory();
			File f = new File("/mnt/sdcard/hclw_settings.json");
			// Look in SD path
			JSONObject oObj;
			JSONArray oResult;
			if (f.exists()) {
				Toast.makeText(this, "hclw_settings.json file found on SD card.  Applying advanced settings...", Toast.LENGTH_LONG).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    oObj = new JSONObject(sb.toString());
				oResult = oObj.getJSONArray("flarepositions");
				sb.setLength(0);
				TOPSURF_FILE = oObj.getString("topsurface_file");
				if (!new File(TOPSURF_FILE).exists()) TOPSURF_FILE=null;
				FLARE_FILE = oObj.getString("flare_file");
				if (!new File(FLARE_FILE).exists()) FLARE_FILE=null;
			} else {
				// Look in assets
				InputStreamReader in = new InputStreamReader(this.getAssets().open("hclw_settings.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    oObj = new JSONObject(sb.toString());
				oResult = oObj.getJSONArray("flarepositions");
				sb.setLength(0);
				TOPSURF_FILE = null;
				FLARE_FILE = null;
			}
			
			LWPSURF_32BIT = oObj.getBoolean("livewallpaper_surface_32bit");
			FIXEDOFFSET = oObj.getInt("lwp_fixed_offset");
			PAINTFG_DITHER = oObj.getBoolean("paint_dither");
			PAINTFG_AA = oObj.getBoolean("paint_antialias");
			PAINTFG_FILTERBMP = oObj.getBoolean("paint_filterbitmap");
			TOPSURF_HUE = Color.parseColor(oObj.getString("topsurface_hue"));
			TOPSURF_DITHER = oObj.getBoolean("topsurface_dither");
			TOPSURF_32BIT = oObj.getBoolean("topsurface_32bit");
			FLARE_USEHUES = oObj.getBoolean("flare_use_hues");
			DEFAULTBRIGHTNESS = oObj.getInt("lwp_baseline_brightness");
			FLAREHUES = new int[5];
			for (int i=0;i<5;i++) {
				FLAREHUES[i] = Color.parseColor(oObj.getJSONArray("flare_hues_WRGBY").getString(i));
			}
			oObj=null;
			
			// now read flare data to our format
			for (int i=0;i<12;i++) {
				JSONObject flare = oResult.getJSONObject(i);
				HCLW.FLAREPATHINITX[i] = (float)(flare.optJSONArray("initial").getDouble(0));
				HCLW.FLAREPATHINITY[i] = (float)(flare.optJSONArray("initial").getDouble(1));
				HCLW.FLAREPATHINITZ[i] = (float)(flare.optJSONArray("initial").getDouble(2));

				HCLW.FLAREPATHMIDX[i] = (float)(flare.optJSONArray("middle").getDouble(0));
				HCLW.FLAREPATHMIDY[i] = (float)(flare.optJSONArray("middle").getDouble(1));
				HCLW.FLAREPATHMIDZ[i] = (float)(flare.optJSONArray("middle").getDouble(2));

				HCLW.FLAREPATHFINALX[i] = (float)(flare.optJSONArray("final").getDouble(0));
				HCLW.FLAREPATHFINALY[i] = (float)(flare.optJSONArray("final").getDouble(1));
				HCLW.FLAREPATHFINALZ[i] = (float)(flare.optJSONArray("final").getDouble(2));
				
				HCLW.MINFLARESPEEDS[i] = (float)(flare.optDouble("minimumspeed"));
				HCLW.FLAREACCEL[i] = (float)(flare.optDouble("accel"));
				
			}
			oResult = null;
//			prepareBitmaps();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//System.gc();
		}
		
		HCLW.FPS = Integer.parseInt(PREFS.getString("FrameRates", "25"));
		HCLW.TARGETTIME=1000l/HCLW.FPS;
		HCLW.RENDERWHILESWIPING = PREFS.getBoolean("RenderWhileSwiping", true);
		HCLW.FLARESABOVESURFACE=HCLW.PREFS.getBoolean("FlaresAboveSurface", false);
		HCLW.LIGHTNINGEFFECT=HCLW.PREFS.getBoolean("LightningEffect", false);
		HCLW.SPARKEFFECT=HCLW.PREFS.getBoolean("SparkEffect", false);
		HCLW.SEARCHLIGHTEFFECT=HCLW.PREFS.getBoolean("Searchlight", false);
		HCLW.LIGHTNFREQUENCY=Double.parseDouble(HCLW.PREFS.getString("LightnFrequency","0.05"));

	*/
	}
	
}
