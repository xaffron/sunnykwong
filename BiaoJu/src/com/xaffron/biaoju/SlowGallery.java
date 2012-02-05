package com.xaffron.biaoju;

import android.content.Context;
import android.util.AttributeSet;
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
		return true;
//        return super.onFling(e1, e2, 0, velocityY);
      }
	
	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	protected boolean getChildStaticTransformation(View child, Transformation t) {
		// TODO Auto-generated method stub
		return true;
	}

}
