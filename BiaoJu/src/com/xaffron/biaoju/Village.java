package com.xaffron.biaoju;

import edu.wlu.cs.levy.CG.KeySizeException;
import java.util.List;

public class Village {
	int tech, pop, ord; //1-10
	double x,y;
	String name;
	String desc;
	static final String[] techStr = new String[] {"Backward ","Rural ","Simple ","Bustling ","Advanced "};
	static final String[] popStr  = new String[] {"Outpost ","Settlement ","Village ","City ","Metropolis "};
	static final String[] ordStr  = new String[] {"Serene ","Peaceful ","Dangerous ","Lawless ","Riotous "};
	public Village(String nm, int t, int p, int o, double xx, double yy) {
		name=nm;
		tech=t;
		pop=p;
		ord=o;
		x=xx;
		y=yy;
		desc="A " + techStr[tech/2] + ordStr[ord/2] + popStr[pop/2] + String.valueOf(xx) + " " + String.valueOf(yy);
	}
	
	public int distFrom(Village v2) {
		return (int)(Math.sqrt(Math.pow(Math.abs(v2.x-x),2.) + Math.pow(Math.abs(v2.y-y),2.)));
	}
	
	public String closestTwoCities (){
		String result;
		List<Village> list;
		try{
			list = TurnActivity.master.map.distMap.nearest(new double[] {x,y}, 3);
			result =list.get(0).name + " " + list.get(1).name;
		}
		catch (KeySizeException e) {
			result="";
		}
		return result;
	}

	

}
