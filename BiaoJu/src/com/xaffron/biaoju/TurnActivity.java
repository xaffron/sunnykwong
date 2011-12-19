package com.xaffron.biaoju;

import android.app.Activity;
import android.widget.ViewFlipper;
//import android.util.Log;
//import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;

public class TurnActivity extends Activity {

	private static boolean DEBUG;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	private Animation slideLeftIn;
	private Animation slideLeftOut;
	private Animation slideRightIn;
	private Animation slideRightOut;

	private Button mActionButton;
	private TextView mConsoleView;
	private ViewFlipper mViewFlipper;

	static GameMaster master;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        DEBUG = Boolean.parseBoolean(getString(R.string.DEBUG));
        
        mViewFlipper = (ViewFlipper)findViewById(R.id.details);
        mViewFlipper.setDisplayedChild(1);
        mActionButton = (Button)findViewById(R.id.Button01);
        mActionButton.setClickable(false);
        mConsoleView = (TextView)findViewById(R.id.console);
    	mConsoleView.addTextChangedListener(new TextWatcher() {
			
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				//nothing
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				//nothing
				}
			
			public void afterTextChanged(Editable s) {
				if (s.length()<1000) return;
				s.delete(0, s.toString().indexOf("\n")+1);
			}
		});
        
    	mActionButton.setText("出發!!");

        slideLeftIn = AnimationUtils.loadAnimation(this, R.anim.slide_left_in);
        slideLeftOut = AnimationUtils.loadAnimation(this, R.anim.slide_left_out);
        slideRightIn = AnimationUtils.loadAnimation(this, R.anim.slide_right_in);
        slideRightOut = AnimationUtils.loadAnimation(this, R.anim.slide_right_out);
        
        gestureDetector = new GestureDetector(new MyGestureDetector());
        gestureListener = new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event)) {
                    return true;
                }
                return false;
            }
        };

        mActionButton.setOnClickListener(new View.OnClickListener(){
        	public void onClick (View v) {
        		//what to do when clicked
        		GameMaster.mt.cancel();
        		master.nextTurn();
        	}
        });
        
    	if (DEBUG) writeLog("Gui Setup Complete");

    	//		Game Setup
    	master = new GameMaster(this);
    	master.nextTurn();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event))
            return true;
        else
        	return false;
    }

    class MyGestureDetector extends SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
                    return false;
                // right to left swipe
                if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY && mViewFlipper.getDisplayedChild() != 2) {
                	mViewFlipper.setInAnimation(slideLeftIn);
                	mViewFlipper.setOutAnimation(slideLeftOut);
                	mViewFlipper.showNext();
                // left to right swipe
                }  else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY && mViewFlipper.getDisplayedChild() != 0) {
                	mViewFlipper.setInAnimation(slideRightIn);
                	mViewFlipper.setOutAnimation(slideRightOut);
                	mViewFlipper.showPrevious();
                }
            } catch (Exception e) {
                // nothing
            }
            return false;
        }
    }

    public void writeLog(String comment) {
    	mConsoleView.append(comment + "\n");
//    	Log.i("XAFFRON",comment);
    }

    
}

