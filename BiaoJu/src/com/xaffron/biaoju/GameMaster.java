package com.xaffron.biaoju;

import java.util.Random;
import java.util.StringTokenizer;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;


public class GameMaster {

	WorldMap map;
	Hero protag;
	PartyList party;
	int location;
	TurnActivity tact;
	String[] goods;
	int[][] rawPrices;
	int[] mktPrices;
	int[] basePrices;
	int[] carry;
	static MugToast mt;
	int cash;
	
	public GameMaster(TurnActivity ta) {
		
		int i,j;
		String[] tempStr;
		StringTokenizer tk;
		
		Random rnd = new Random();
		map	= new WorldMap();
	    party = new PartyList();
	    protag = party.addToParty(Hero.chooseProtag());
	    location = rnd.nextInt(5);
	    cash = 1000;
	    tact=ta;

	    mt = new MugToast(tact);
	    
	    tempStr = tact.getResources().getStringArray(R.array.goods);
	    goods = new String[tempStr.length];
	    rawPrices = new int[tempStr.length][5];
	    mktPrices = new int[tempStr.length];
	    basePrices = new int[tempStr.length];
	    carry = new int[tempStr.length];
	    for (i=0;i<tempStr.length;i++) {
	    	tk = new StringTokenizer(tempStr[i]);
	    	goods[i]=(String) tk.nextElement();
	    	carry[i]=0;
	    	for (j=0;j<5;j++) {
	    		rawPrices[i][j]=Integer.parseInt((String)tk.nextElement());
	    	}
	    }
	    
	}
	
	public Village getLocation() {
		return map.poi[location];
	}
	
	public void nextTurn(){
		Random rnd;
		double turnLuck;

		// Roll the dice for this turn.
		rnd = new Random();

	    //TEMP: Roll the dice for the next location.
		location = rnd.nextInt(5);

	    // Determining protag's luck this turn.
		turnLuck = rnd.nextDouble() * protag.baseluck / 5. + 0.5;  //luck factor

		// Refresh Market Prices.
//		marketRefresh(turnLuck);

		// Where are we now?
		tact.writeLog("Arrived in " + getLocation().name + " with " + String.valueOf(cash) + " gold.");
		
		
		// Show the Toast
//		mt = new MugToast(tact);
		mt.setText(protag.name + ": 回到鏢局, 終於可以洗臉了!");
		mt.show();

	}

//	public void marketRefresh(double turnLuck) {
//		int i,j,k;
//		//TextView tempTV;
//		
//		ListView lv = (ListView)tact.findViewById(R.id.shoplist);
//		lv.setAdapter(new IconTextButtonAdapter(tact));
//		LinearLayout ll = (LinearLayout)tact.findViewById(R.id.leftscreen);
//		ll.setClickable(false);
//
////		lv.setOnItemClickListener(New OnItemClickListener l )
//		for (i=0;i<goods.length;i++){
//			for (j=0;j<5;j++){
//				mktPrices[i] = (int) Math.round(rawPrices[i][0] * turnLuck);
//			}
//		}
//		
//	}
	
}
