/**
 * 
 */
package com.xaffron.biaoju;

import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

/**
 * @author skwong01
 *
 */
public class IconTextButtonAdapter extends ArrayAdapter<String> {
	Context context;
	static String[] items={"lorem", "ipsum", "dolor", "sit", "amet"};
	
	public IconTextButtonAdapter(Context ctxt) {
		super(ctxt,R.layout.itbrow,IconTextButtonAdapter.items);
		context=ctxt;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View row = inflater.inflate(R.layout.itbrow, null);
		row.setOnLongClickListener(null);
		row.setOnTouchListener(null);
		TextView label = (TextView)row.findViewById(R.id.label);
		label.setText(items[position]);
		ImageView icon = (ImageView)row.findViewById(R.id.icon);
		icon.setImageResource(R.drawable.goku);
 		return row;
	}
	
}
