package com.sunnykwong.HCLW;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class HCLWEngine {
	static Bitmap drawFlares(final int iOffset, final boolean bFullRender) {

		final Canvas c = HCLW.SCRNBUFFERCANVAS;
		HCLW.srcFullRect.offsetTo(-iOffset, -HCLW.YOFFSET);
		HCLW.srcFlareRect.offsetTo((int) (-iOffset * 640f / HCLW.LWPWIDTH), 0);

		if (!bFullRender) {
			HCLW.PaintFg.setAntiAlias(false);
			HCLW.PaintFg.setDither(false);
			HCLW.PaintFg.setFilterBitmap(false);
			c.drawBitmap(HCLW.FG, HCLW.srcFullRect, HCLW.tgtFullRect,
					HCLW.PaintFg);
			HCLW.PaintFg.setDither(true);
			HCLW.PaintFg.setFilterBitmap(true);
			return HCLW.SCRNBUFFER;
		}
		// Draw the "Channels" on the bottom.
		// Default to channel bkgd (white for Sparks).
		// if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
		// c.drawColor(Color.parseColor("#FFACACAC"));
		// HCLW.BUFFER.eraseColor(Color.TRANSPARENT);
		// } else if (HCLW.PREFS.getBoolean("Searchlight", false)) {
		// HCLW.BUFFERCANVAS.drawColor(Color.parseColor("#441b1939"));
		// } else {
		//
		// //Trail Length is an optical illusion actually driven by
		// //The opacity of each frame's screen erase
		// try {
		// HCLW.BUFFERCANVAS.drawColor(Color.parseColor(HCLW.PREFS.getString("TrailLength",
		// "#051b1939")));
		// } catch (Exception e) {
		// HCLW.BUFFERCANVAS.drawColor(Color.parseColor("#051b1939"));
		// }
		// }
		if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
			c.drawColor(HCLW.SPARKEFFECTCOLOR);
			HCLW.BUFFER.eraseColor(Color.TRANSPARENT);
		} else if (HCLW.PREFS.getBoolean("Searchlight", false)) {
			HCLW.BUFFERCANVAS.drawColor(HCLW.SEARCHLIGHTEFFECTCOLOR);
		} else {

			// Trail Length is an optical illusion actually driven by
			// The opacity of each frame's screen erase
			HCLW.BUFFERCANVAS.drawColor(HCLW.DEFAULTEFFECTCOLOR);
		}

		// if Flares are to be above surface, draw the "Surface" now (and skip
		// the "middle" mask).
		if (HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
			c.drawBitmap(HCLW.FG, HCLW.srcFullRect, HCLW.tgtFullRect,
					HCLW.PaintFg);
		}

		// We're tracking each flare.
		for (int i = 0; i < (HCLW.PREFS.getBoolean("Searchlight", false) ? HCLW.DISPLACEMENTS.length
				: HCLW.DISPLACEMENTS.length - 1); i++) {
			// If a flare is done, reset. (the 1.1 is to make sure the flare
			// goes offscreen first)
			if (HCLW.DISPLACEMENTS[i] > 1.1f) {
				HCLW.DISPLACEMENTS[i] = 0f;
				HCLW.COLORS[i] = -1;
			} else if (HCLW.DISPLACEMENTS[i] == 0f) {
				// Only relaunch a flare 1% of the time by default (can be
				// customized)
				if (Math.random() < (i == HCLW.DISPLACEMENTS.length - 1 ? 0.02d
						: 0.01d * Double.parseDouble(HCLW.PREFS.getString(
								"FlareFrequency", "1")))) {
					if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
						HCLW.FLARESPEEDS[i] = (float) (HCLW.MINFLARESPEEDS[i] * (1 + Math
								.random()));
					} else {
						HCLW.FLARESPEEDS[i] = (float) (HCLW.MINFLARESPEEDS[i] * (1 + Math
								.random()));
					}
					HCLW.DISPLACEMENTS[i] += HCLW.FLARESPEEDS[i];

					// Slight acceleration.
					HCLW.FLARESPEEDS[i] += HCLW.FLAREACCEL[i];

					// Pick a color for each flare.
					do {
						HCLW.COLORS[i] = (int) (Math.random() * 5.);
					} while (
					// If
					(HCLW.NUMBEROFFLARECOLORS > 2 && i > 0
							&& i < HCLW.DISPLACEMENTS.length - 1 && (HCLW.COLORS[i] == HCLW.COLORS[i - 1] || HCLW.COLORS[i] == HCLW.COLORS[i + 1]))
							|| !HCLW.PREFS.getBoolean("showcolor"
									+ HCLW.COLORS[i], true));
				}
			} else {
				HCLW.DISPLACEMENTS[i] += HCLW.FLARESPEEDS[i];
			}

			// Flares

			// Render each flare
			// If the flare head/tail will be offscreen, skip drawing that part
			if (HCLW.DISPLACEMENTS[i] < 0)
				continue;
			if (HCLW.DISPLACEMENTS[i] > 1)
				continue;

			// For Spark Effect, we want the sparks to sparkle on the horizon;
			// For flares/trails, we don't want the flares sitting around
			if (HCLW.DISPLACEMENTS[i] == 0)
				continue;

			// Position each flare in the 640x480 space.
			HCLW.TEMPMATRIX.reset();
			float xPos = floatInterpolate(HCLW.FLAREPATHINITX[i],
					HCLW.FLAREPATHMIDX[i], HCLW.FLAREPATHFINALX[i],
					HCLW.DISPLACEMENTS[i]);
			float yPos = floatInterpolate(HCLW.FLAREPATHINITY[i],
					HCLW.FLAREPATHMIDY[i], HCLW.FLAREPATHFINALY[i],
					HCLW.DISPLACEMENTS[i]) - 240f;
			float zFactor;
			// Sparks are white; trails are multicolored
			if (HCLW.PREFS.getBoolean("SparkEffect", false)) {
				zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i],
						HCLW.FLAREPATHMIDZ[i], HCLW.FLAREPATHFINALZ[i],
						HCLW.DISPLACEMENTS[i])
						* (.5f + (float) (.5d * Math.random())) * 1.5f;
				HCLW.TEMPMATRIX.postScale(zFactor / HCLW.SCALEX, zFactor
						/ HCLW.SCALEY);
				HCLW.TEMPMATRIX.postTranslate(xPos - HCLW.FLARE[0].getWidth()
						* zFactor / HCLW.SCALEX / 2f,
						yPos - HCLW.FLARE[0].getHeight() * zFactor
								/ HCLW.SCALEY / 2f);

				HCLW.BUFFERCANVAS.drawBitmap(HCLW.FLARE[0], HCLW.TEMPMATRIX,
						HCLW.PaintFlare);
			} else {
				zFactor = floatInterpolate(HCLW.FLAREPATHINITZ[i],
						HCLW.FLAREPATHMIDZ[i], HCLW.FLAREPATHFINALZ[i],
						HCLW.DISPLACEMENTS[i]);
				HCLW.TEMPMATRIX.postScale(zFactor / HCLW.SCALEX, zFactor
						/ HCLW.SCALEY);
				HCLW.TEMPMATRIX.postTranslate(
						xPos - HCLW.FLARE[HCLW.COLORS[i]].getWidth() * zFactor
								/ HCLW.SCALEX / 2f, yPos
								- HCLW.FLARE[HCLW.COLORS[i]].getHeight()
								* zFactor / HCLW.SCALEY / 2f);

				HCLW.BUFFERCANVAS.drawBitmap(HCLW.FLARE[HCLW.COLORS[i]],
						HCLW.TEMPMATRIX, HCLW.PaintFlare);
			}
		}

		// System.out.println("LWP: " + HCLW.LWPWIDTH + " x " + HCLW.LWPHEIGHT);
		// System.out.println("SCRN:" + HCLW.SCRNWIDTH + " x " +
		// HCLW.SCRNHEIGHT);
		// System.out.println("SRCFLARE " + HCLW.srcFlareRect.toString());
		// System.out.println("TGTFLARE " + HCLW.tgtFlareRect.toString());
		c.drawBitmap(HCLW.BUFFER, HCLW.srcFlareRect, HCLW.tgtFlareRect,
				HCLW.PaintMid);

		if (HCLW.PREFS.getBoolean("LightningEffect", false)) {
			if (Math.random() < Double.parseDouble(HCLW.PREFS.getString(
					"LightnFrequency", "0.05"))) {
				HCLW.LightningFactor = 1f;
			} else if (HCLW.LightningFactor <= 0f) {
				HCLW.LightningFactor = 0f;
			} else {
				HCLW.LightningFactor -= 0.05f;
			}
			HCLW.PaintFg.setAlpha((int) (255f * HCLW.LightningFactor));
		} else {
			HCLW.PaintFg.setAlpha((int) (255f * HCLW.DEFAULTBRIGHTNESS / 100f));
		}
		// Draw the "Middle" mask, then the "Surface".
		if (!HCLW.PREFS.getBoolean("FlaresAboveSurface", false)) {
			if (HCLW.PaintFg.getAlpha() < 255) {
				c.drawBitmap(HCLW.MIDDLE, HCLW.srcFullRect, HCLW.tgtFullRect,
						HCLW.PaintMid);
			}
			if (HCLW.LightningFactor > 0f) {
				// HCLW.PaintFg.setAlpha(60);
				c.drawBitmap(HCLW.FG, HCLW.srcFullRect, HCLW.tgtFullRect,
						HCLW.PaintFg);
			}
		}
		return HCLW.SCRNBUFFER;
	}

	/* Utility Function (linear interpolation). */
	static public float floatInterpolate(float n1, float n2, float n3,
			float gradient) {
		if (gradient > 0.5f)
			return (n2 + (n3 - n2) * (gradient - 0.5f) * 2);
		else
			return (n1 + (n2 - n1) * gradient * 2);
	}

}
