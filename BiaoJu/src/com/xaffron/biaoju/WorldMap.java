package com.xaffron.biaoju;

import java.util.Random;
import edu.wlu.cs.levy.CG.KDTree;
import edu.wlu.cs.levy.CG.KeyDuplicateException;
import edu.wlu.cs.levy.CG.KeySizeException;

public class WorldMap {

	long seed1=1975;
	long seed2=101;
	Random xRan, yRan;
	KDTree<Landmark> distMap;
	Landmark[] poi;
	double tempX, tempY;
	
	public WorldMap() {
		xRan = new Random(seed1);
		yRan = new Random(seed2);
//		int i;
		poi = new Landmark[5];
		distMap = new KDTree<Landmark>(2);

		try {
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[0] = new Landmark("Chongqing",8,8,8,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[0]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[1] = new Landmark("Hong Kong",6,1,6,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[1]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[2] = new Landmark("Shanghai",5,7,2,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[2]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[3] = new Landmark("Beijing",2,4,3,tempX,tempY);
			distMap.insert(new double[] {tempX, tempY}, poi[3]);
			tempX=xRan.nextDouble()*256;
			tempY=yRan.nextDouble()*256;
			poi[4] = new Landmark("Urumqi",1,1,1,tempX,tempY);
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
