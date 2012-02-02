package com.xaffron.biaoju;

import java.util.Random;
import java.util.StringTokenizer;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;


public class GM {

	static int ACTION;
	static final int PENDING=0, ATTACK=1, RECRUIT=2, RUN=3, ITEM=4;
	WorldMap map;
	Character protag;
	PartyList party;
	int location;
	String[] goods;
	int[][] rawPrices;
	int[] mktPrices;
	int[] basePrices;
	int[] carry;
	static MugToast mt;
	int cash;
	Combat currentFight;
	
	public GM(TurnActivity ta) {
		
		int i,j;
		String[] tempStr;
		StringTokenizer tk;
		
		Random rnd = new Random();
		map	= new WorldMap();
	    party = new PartyList();
	    protag = party.addToParty(Character.chooseProtag());
	    location = rnd.nextInt(5);
	    cash = 1000;

	    mt = new MugToast(BJ.TACT);
	    
	    tempStr = BJ.TACT.getResources().getStringArray(R.array.goods);
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
		// Refresh Market Prices.
//		marketRefresh(turnLuck);

		// Where are we now?
		if (currentFight!=null && currentFight.inProgress) {
			currentFight.keepGoing(GM.ACTION);
		} else {
			switch (party.move(1d)) {
				case BJ.PARTYARRIVED:
					int iNewTarget;
					do {
						iNewTarget = (int)(Math.random()*BJ.jaryTOWNES.length());
					} while (iNewTarget == party.iLocTarget);
					party.iLocTarget = iNewTarget;
					party.iTargetX = BJ.jaryTOWNES.optJSONObject(iNewTarget).optJSONArray("location").optInt(0);
					party.iTargetY = BJ.jaryTOWNES.optJSONObject(iNewTarget).optJSONArray("location").optInt(1);
					party.dDistFromTgt = Math.sqrt(Math.pow(party.iTargetY-party.dLocationY,2) + Math.pow(party.iTargetX-party.dLocationX,2));
					break;
				case BJ.PARTYENCOUNTER:
					BJ.TACT.writeBlow("Combat!");
					currentFight = new Combat(party);
					currentFight.keepGoing(GM.ACTION);
					break;
				case BJ.PARTYNOENCOUNTER:
					break;
			}
		}
		
//		tact.writeConsole("Arrived in " + getLocation().name + " with " + String.valueOf(cash) + " gold.");
		
		
		// Show the Toast
//		mt = new MugToast(tact);
//		mt.setText(protag.name + ": 回到鏢局, 終於可以洗臉了!");
//		mt.show();

	}
	static public int getAbilityModifier(int iAbilityScore) {
		return (iAbilityScore)/2-5;
	}

	static public boolean d20Roll(int[] iModifiers, int iTarget) {
		int iDiceRoll = (int)(Math.random()*20)+1;
		for (int imod:iModifiers) {
			iDiceRoll+=imod;
		}
		return iDiceRoll>=iTarget;
	}
	
	static public int diceRoll(int iNumDice, int iSides, int iMod) {
		int iResult=iMod;
		for (int i=0;i<iNumDice; i++) {
			iResult += (int)(Math.random()*iSides)+1;
		}
		return iResult;
	}
	
	static public int dicePercent(int iMod) {
		int iResult = ((int)(Math.random()*10))*10 + (int)(Math.random()*10) + iMod;
		return iResult==0?100:iResult;
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
