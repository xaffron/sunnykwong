package com.sunnykwong.omc;

import com.sunnykwong.freeomc1.R;
import android.content.Intent;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Gallery;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import android.content.res.TypedArray;
import android.widget.BaseAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.view.View.OnClickListener;
import android.net.Uri;

public class OMCAdActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {

    	super.onCreate(savedInstanceState);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.adgallery);
        
        findViewById(R.id.TextView02).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
            	Intent myIntent = new Intent(Intent.ACTION_DEFAULT,
            			Uri.parse("http://xaffron.blogspot.com"));
            			startActivity(myIntent);             
			}
		});

        Gallery g = (Gallery) findViewById(R.id.gallery);
        g.setAdapter(new ImageAdapter(this));

        g.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
            	Intent myIntent = new Intent(Intent.ACTION_DEFAULT,
            			Uri.parse("market://search?q=pname:com.sunnykwong.omc"));
            			startActivity(myIntent);             }
        });
    }
    
    public class ImageAdapter extends BaseAdapter {
        int mGalleryItemBackground;
        private Context mContext;

        private Integer[] mImageIds = {
                R.drawable.wbwidget,
                R.drawable.llook,
                R.drawable.cclash,
                R.drawable.ddiary,
                R.drawable.ddigits,
                R.drawable.bbeauty,
                R.drawable.tticks,
                R.drawable.bbills
        };

        public ImageAdapter(Context c) {
            mContext = c;
            TypedArray a = obtainStyledAttributes(R.styleable.HelloGallery);
            mGalleryItemBackground = a.getResourceId(
                    R.styleable.HelloGallery_android_galleryItemBackground, 0);
            a.recycle();
        }

        public int getCount() {
            return mImageIds.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView i = new ImageView(mContext);

            i.setImageResource(mImageIds[position]);
            i.setLayoutParams(new Gallery.LayoutParams(240, 320));
            i.setScaleType(ImageView.ScaleType.FIT_XY);
            i.setBackgroundResource(mGalleryItemBackground);

            return i;
        }
    }
}
