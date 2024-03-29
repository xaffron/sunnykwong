package com.sunnykwong.omc;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;

import android.content.ContentProvider;

import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;

public class OMCProvider extends ContentProvider {
        private static final String myURI = "content://com.sunnykwong.omc/widgets";
        public static final Uri CONTENT_URI = Uri.parse(myURI);

        @Override
        public boolean onCreate() {
        // TODO: Construct the underlying database.
        return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
                // TODO Auto-generated method stub
                return null;
        }
       
        @Override
        public Uri insert(Uri uri, ContentValues values) {
                // TODO Auto-generated method stub
                return null;
        }
       
        @Override
        public String getType(Uri uri) {
                // TODO Auto-generated method stub
                return null;
        }
       
        @Override
        public int update(Uri uri, ContentValues values, String selection,
                        String[] selectionArgs) {
                // TODO Auto-generated method stub
                return 0;
        }
       
        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
                // TODO Auto-generated method stub
                return 0;
        }
       
        @Override
        public AssetFileDescriptor openAssetFile(Uri uri, String mode)
                        throws FileNotFoundException {
                int aWI = Integer.parseInt(uri.getQueryParameter("awi"));
                
                if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Provider","Ready to render widget " +aWI);
                File f = new File(OMC.CACHEPATH + "/" + aWI +"cache.png");
                if (f.exists()&&f.canRead()){
                        if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Provider","Reading png for widget"+aWI);
                        return new AssetFileDescriptor(ParcelFileDescriptor.open(f,
                                ParcelFileDescriptor.MODE_READ_ONLY), 0, AssetFileDescriptor.UNKNOWN_LENGTH);
                } else {
                        if (OMC.DEBUG) Log.i(OMC.OMCSHORT+"Provider","widget png missing - return transparent png");
                    	//return OMC.RES.openRawResourceFd(OMC.RDrawableId("clockicon"));
                        //return OMC.RES.openRawResourceFd(OMC.RDrawableId("transparent"));
                        return null;
                }

        }
}
