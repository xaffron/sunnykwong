package com.sunnykwong.FlingWords;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.Gallery;
import android.util.AttributeSet;

public class FWGallery extends Gallery {

	public FWGallery(Context context) {
		super(context);
	}

	public FWGallery(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public FWGallery(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
    public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY)
    {
		return false;
    }
	
	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		if (e.getX() > FW.SCRNWIDTH/2) this.setSelection(Math.min(this.getAdapter().getCount()-1, this.getSelectedItemPosition()+1), true);
		else this.setSelection(Math.max(0, this.getSelectedItemPosition()-1), true);
		return true;
	}
	
}
