package com.sunnykwong.omc;

import com.sunnykwong.freeomc1.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class OMCCreditsActivity extends PreferenceActivity { 

    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
       	addPreferencesFromResource(R.xml.credits);

    }

} 