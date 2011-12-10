package com.xaffron.biaoju;

import java.util.Random;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class WorldMap {

	long seed1=1975;
	long seed2=101;
	Random xRan, yRan;
	KDTree<Village> distMap;
	Village[] poi;
	double tempX, tempY;
	
	public WorldMap() {
		xRan = new Random(seed1);
		yRan = new Random(seed2);
//		int i;
		poi = new Village[5];
		distMap = new KDTree<Village>(2);

		try {
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[0] = new Village("Chongqing",8,8,8,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[0]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[1] = new Village("Hong Kong",6,1,6,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[1]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[2] = new Village("Shanghai",5,7,2,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[2]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[3] = new Village("Beijing",2,4,3,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[3]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[4] = new Village("Urumqi",1,1,1,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[4]);
		}
		catch (KeyDuplicateException e) {
			// do nothing
		}
		catch (KeySizeException e) {
			// do nothing
		}
		
	}
	
}
