package com.sunnykwong.omc;

import android.os.Bundle;
import android.preference.PreferenceActivity;
public class OMCCreditsActivity extends PreferenceActivity { 

    @Override
    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
       	addPreferencesFromResource(OMC.RXmlId("credits"));

    }

} 