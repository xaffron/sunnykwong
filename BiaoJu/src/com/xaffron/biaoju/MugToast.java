package com.xaffron.biaoju;

import android.content.Context;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.LayoutInflater;

public class MugToast extends Toast {

	ImageView img;
	LinearLayout ll;
	TextView txt;
	LayoutInflater inflater;
	
	public MugToast(Context context) {
		
		super(context);

		inflater = LayoutInflater.from(context);
		ll = (LinearLayout)inflater.inflate(R.layout.mugtoast, null);
		setView(ll);
		
		img = (ImageView) ll.findViewById(R.id.toastportrait);
		txt = (TextView) ll.findViewById(R.id.toastmsg);
		
        setDuration(Toast.LENGTH_LONG);


	}

	@Override
	public void setText(CharSequence s) {
		txt.setText(s);
	}
}
