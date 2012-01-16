package com.xaffron.biaoju;

import android.app.Application;

public class BJ extends Application {
	
	static GM MASTER;
	static TurnActivity TACT;
	static Combat CURRENTFIGHT;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
}
