package com.sunnykwong.omc;

import java.util.Arrays;
import java.util.Collection;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class CreditRollActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        ListView lv = getListView();
        CreditRollAdapter la = new CreditRollAdapter(this);
        if (getIntent().getIntExtra("type", 1)==1) {
            la.addAll(Arrays.asList(OMC.RStringArray("testers")));
        } else {
            la.addAll(Arrays.asList(OMC.RStringArray("translators")));
        }
        lv.setAdapter(la);
    }

}

class CreditRollAdapter extends ArrayAdapter<String> {
	Activity activity;
	public CreditRollAdapter(Activity ctxt) {
		super(ctxt,OMC.RLayoutId("credit"), OMC.RId("testerraw"));
		activity = ctxt;
	}
	
	@Override
	public void add(String object) {
		// TODO Auto-generated method stub
		super.add(object);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View cell = super.getView(position, convertView, parent);
		String[] pieces = ((String)((TextView)cell.findViewById(OMC.RId("testerraw"))).getText()).split("\\|");
		((TextView)cell.findViewById(OMC.RId("testername"))).setText(pieces[0]);
		if (pieces.length==2) {
			((TextView)cell.findViewById(OMC.RId("testerquote"))).setText(pieces[1]);
		} else {
			((TextView)cell.findViewById(OMC.RId("testerquote"))).setText("");
		}
		return cell;
	}
}
