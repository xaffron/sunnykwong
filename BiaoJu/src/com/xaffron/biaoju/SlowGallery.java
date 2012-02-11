package com.xaffron.biaoju;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.Gallery;

public class SlowGallery extends Gallery {

	public SlowGallery(Context c) {
		super(c);
	}
	
	public SlowGallery (Context a, AttributeSet b) {
		super(a,b);
	} 

	public SlowGallery (Context a, AttributeSet b, int c) {
		super(a,b,c);
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
      }

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}
@Override
public boolean onKeyUp(int keyCode, KeyEvent event) {
	// TODO Auto-generated method stub
	return false;
//	return super.onKeyDown(keyCode, event);
}

	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t) {
		// TODO Auto-generated method stub
		return true;
	}

}
