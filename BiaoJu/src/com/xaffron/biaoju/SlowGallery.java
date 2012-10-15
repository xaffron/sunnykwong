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
		System.out.println("onFling");
		return super.onFling(null, null, 0, velocityY);
      }

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		System.out.println("ondown");
		System.out.println(this.getSelectedItemPosition());
		return true;
	}
	@Override
	public boolean onTouchEvent(MotionEvent me) {

		System.out.println("ontouch: " + me.getAction());

	    if(me.getAction() == 0){
	    	System.out.println("Down");
	    }
	    else if (me.getAction() == 1) {
	    	System.out.println("Up");
	    }
	    else if (me.getAction() == 2) {
	    	System.out.println("Scroll");
	    	System.out.println("History: " + me.getHistorySize());
	    }
	    return super.onTouchEvent(me);
//	    boolean detectedUp = me.getAction() == MotionEvent.ACTION_UP;
//	    if (!mGestureDetector.onTouchEvent(event) && detectedUp)
//	    {
//	        return onUp(event);
//	    }
	}
	@Override
public boolean onKeyUp(int keyCode, KeyEvent event) {
	// TODO Auto-generated method stub
	System.out.println("onup");
	return true;
//	return super.onKeyDown(keyCode, event);
}

	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t) {
		// TODO Auto-generated method stub
		return true;
	}
 
}
