package com.sunnykwong.omc;

import java.util.Arrays;
import java.util.Collection;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class CreditRollActivity extends ListActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        System.out.println(this.getIntent().getIntExtra("type", 0));
        ListView lv = getListView();
        CreditRollAdapter la = new CreditRollAdapter(this);
        String[] credits = OMC.RStringArray("testers");
        la.addAll(Arrays.asList(OMC.RStringArray("testers")));
        
        lv.setAdapter(la);
    }

}

class CreditRollAdapter extends ArrayAdapter<String> {
	public CreditRollAdapter(Context ctxt) {
//		super(ctxt, OMC.RLayoutId("credit"));
		super(ctxt, OMC.RLayoutId("tweakerlayerdropdown"));
	}
	
	@Override
	public void add(String object) {
		// TODO Auto-generated method stub
		super.add(object);
	}
}
