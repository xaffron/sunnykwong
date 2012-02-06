package com.sunnykwong.FlingWords;

import java.io.File;
import java.util.HashMap;

import org.json.JSONException;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class FWFlickActivity extends Activity // implements OnClickListener,
												// OnItemClickListener,
												// OnItemLongClickListener
{

	public static HashMap<String, String[]> WORDSETS;
	public static String tempText = "";
	public static File SDROOT, THEMEROOT;

	public View topLevel;
	public FWGallery gallery;
	static AlertDialog mAD;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(Activity.RESULT_CANCELED);

		setContentView(R.layout.flinglayout);
		topLevel = findViewById(R.id.toplvl);
		if (Build.VERSION.SDK_INT >= 11)
			topLevel.setSystemUiVisibility(View.STATUS_BAR_HIDDEN);

		gallery = (FWGallery) this.findViewById(R.id.gallery1);
		FlickAdapter fa = new FlickAdapter(this);
		int size = FW.CURRENTBATCH.optJSONArray("words").length();
		fa.addItem("");
		int[] seq = FW.generateRandomSequence(size);
		for (int i = 0; i < size; i++) {
			fa.addItem(FW.CURRENTBATCH.optJSONArray("words").optString(seq[i]));
		}
		fa.addItem("");
		try {
			FW.CURRENTBATCH.put("TotalUsedTimes",
					FW.CURRENTBATCH.optInt("TotalUsedTimes") + 1);
		} catch (JSONException e) {
		}

		gallery.setAdapter(fa);
		gallery.setEnabled(true);
		Toast.makeText(this,
				"Swipe right to left\n(Or tap right-hand side of screen)",
				Toast.LENGTH_SHORT).show();

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();

	}

}
