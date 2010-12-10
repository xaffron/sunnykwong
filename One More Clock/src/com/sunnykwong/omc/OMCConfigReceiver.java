package com.sunnykwong.omc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.app.PendingIntent;
public class OMCConfigReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "Config","Rcvd " + intent.getAction());
		String action = intent.getAction();
		if (action.equals("com.sunnykwong.omc.WIDGET_CONFIG") || action.equals("com.sunnykwong.omc.WIDGET_INDIRECT") ) {
			Intent intent2 = new Intent(Intent.ACTION_EDIT,intent.getData(),context,OMCPrefActivity.class);
	        PendingIntent pi = PendingIntent.getActivity(context, 0, intent2, 0);
	        try {
	        	pi.send();
	        } catch (PendingIntent.CanceledException e) {
	        	e.printStackTrace();
	        }
		}
	}
}
