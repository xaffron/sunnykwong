package com.xaffron.biaoju;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.widget.Toast;

public class BJ extends Application {
	
	static GM MASTER;
	static TurnActivity TACT;
	static Combat CURRENTFIGHT;
	static Bitmap bmpCOMPERE;
	static JSONObject jsonMONSTERS, jsonITEMS, jsonTOWNES, jsonEQUIPMENT;
	static JSONArray jaryMONSTERS, jaryEQUIPMENT, jaryTOWNES, jaryITEMS;
	
	static final int PARTYARRIVED=0, PARTYENCOUNTER=1, PARTYNOENCOUNTER=2;
	
	@Override
	public void onCreate() {
		super.onCreate();
		initFromJSON();
	}
	
	public boolean initFromJSON() {
		try {
			if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
				return false;
			String sdpath = getApplicationContext().getExternalFilesDir(null).getAbsolutePath();
			File f = new File(sdpath + "/.nomedia");
			if (!f.exists()) {
				f.createNewFile();
			}
			
			f = new File(sdpath + "/00compere.png");
			if (f.exists()) {
				BJ.bmpCOMPERE = BitmapFactory.decodeFile(f.getAbsolutePath());
			} else {
				BJ.bmpCOMPERE = BitmapFactory.decodeStream(this.getAssets().open("portraits/00compere.png"));
			}

			//
			//Loading Equipment.
			//
			f = new File(sdpath + "/equipment.json");

			// Look in SD path
			if (f.exists()) {
				MugToast.makeText(this, "Loading equipment...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonEQUIPMENT = new JSONObject(sb.toString());
			// Look in assets
			} else {
				MugToast.makeText(this, "Using default equipment...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				
				InputStreamReader in = new InputStreamReader(this.getAssets().open("json/equipment.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonEQUIPMENT = new JSONObject(sb.toString());
			    jaryEQUIPMENT = jsonEQUIPMENT.optJSONArray("equipment");
			}
			

			//
			//Loading items.
			//
			f = new File(sdpath + "/items.json");

			// Look in SD path
			if (f.exists()) {
				MugToast.makeText(this, "Loading items...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonITEMS = new JSONObject(sb.toString());
			// Look in assets
			} else {
				MugToast.makeText(this, "Using default items...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				
				InputStreamReader in = new InputStreamReader(this.getAssets().open("json/items.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonITEMS = new JSONObject(sb.toString());
			}
			

			//
			//Loading Monsters.
			//
			f = new File(sdpath + "/monsters.json");

			// Look in SD path
			if (f.exists()) {
				MugToast.makeText(this, "Loading bestiary...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonMONSTERS = new JSONObject(sb.toString());
			// Look in assets
			} else {
				MugToast.makeText(this, "Using default bestiary...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				
				InputStreamReader in = new InputStreamReader(this.getAssets().open("json/monsters.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonMONSTERS = new JSONObject(sb.toString());
			}
			jaryMONSTERS = jsonMONSTERS.optJSONArray("monsters");
			

			//
			//Loading Townes.
			//
			f = new File(sdpath + "/townes.json");

			// Look in SD path
			if (f.exists()) {
				MugToast.makeText(this, "Loading townes...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				BufferedReader in = new BufferedReader(new FileReader(f),8192);
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonTOWNES = new JSONObject(sb.toString());
			// Look in assets
			} else {
				MugToast.makeText(this, "Using default townes...", BJ.bmpCOMPERE, Toast.LENGTH_SHORT).show();
				
				InputStreamReader in = new InputStreamReader(this.getAssets().open("json/townes.json"));
				StringBuilder sb = new StringBuilder();
			    char[] buffer = new char[8192];
			    int iCharsRead = 0;
			    while ((iCharsRead=in.read(buffer))!= -1){
			    	sb.append(buffer, 0, iCharsRead);
			    }
			    in.close();
			    jsonTOWNES = new JSONObject(sb.toString());
			    jaryTOWNES = jsonTOWNES.optJSONArray("townes");
			}
			
		}	catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
