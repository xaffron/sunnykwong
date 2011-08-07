package com.sunnykwong.HCLW;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.jayway.opengl.tutorial.mesh.SimplePlane;

import net.rbgrn.android.glwallpaperservice.*;


// Original code provided by Robert Green
// http://www.rbgrn.net/content/354-glsurfaceview-adapted-3d-live-wallpapers
public class HCLWGLService extends GLWallpaperService {
	Thread renderThread;
    public HCLWGLService() {
        super();
    }
    
    public Engine onCreateEngine() {
        MyEngine engine = new MyEngine();
        return engine;
    }
    
    class MyEngine extends GLEngine {
        HCLWGLRenderer renderer;
        public MyEngine() {
            super();
            // handle prefs, other initialization
    		// Create a new plane.
    		HCLW.plane = new SimplePlane(1, 1);

    		// Move and rotate the plane.
    		HCLW.plane.z = 1.7f;
    		HCLW.plane.rx = 0;

    		// Load the texture.
    		HCLW.plane.loadBitmap(HCLW.SCRNBUFFER);

            renderer = new HCLWGLRenderer();
    		setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
    		// Add the plane to the renderer.
    		renderer.addMesh(HCLW.plane);
   
        	HCLWEngine.drawFlares(0,true);
    		HCLW.plane.loadBitmap(HCLW.SCRNBUFFER);
    		
    		renderThread = new Thread() {
    			public synchronized void start() {
    				while (true) {
    					HCLWEngine.drawFlares(0,true);
    					HCLW.plane.loadBitmap(HCLW.SCRNBUFFER);
    					try {
    						Thread.sleep(1000l/60);
    					} catch (InterruptedException e) {}
    				}
    			};
    		};
    		renderThread.run();

        }
        
        public void onDestroy() {
            super.onDestroy();
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
            renderThread.interrupt();
        }
    }
}
