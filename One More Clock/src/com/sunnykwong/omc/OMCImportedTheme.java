package com.sunnykwong.omc;

import java.util.HashMap;
import java.util.ArrayList;

public class OMCImportedTheme implements java.io.Serializable {
    static public final long serialVersionUID = 0l;
	public HashMap<String,ArrayList<String>> arrays;
	public String name;
	public boolean valid;
	
	public OMCImportedTheme () {
		valid = false;
		name = null;
		arrays = new HashMap<String,ArrayList<String>>();
	}
	
}
