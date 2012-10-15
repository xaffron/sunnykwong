package com.xaffron.biaoju;

import java.util.Random;
import java.util.StringTokenizer;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;

public class GM {

	static int ACTION;
	static final int PENDING=0, ATTACK=1, RECRUIT=2, RUN=3, ITEM=4;
	BaseActivity mBaseView;
	WorldMap map;
	Character protag;
	PartyList party;
	int location;
	String[] goods={"Rice","Wine","Wood","Gunpowder","Tea"};
	int[] rawPrices={5,10,10,100,15};
	int[] mktPrices=new int[5];
	int[] basePrices;
	int[] carry;
	static MugToast mMugToast;
	int cash;
	Combat currentFight;
	
	public GM() {
		Random rnd = new Random();
		map	= new WorldMap();
	    party = new PartyList();
	    protag = party.addToParty(Character.chooseProtag());
	    location = rnd.nextInt(5);
	    cash = 1000;
	    System.out.println(rawPrices.length);
	    System.out.println(mktPrices.length);
	}
	
	public void setView(BaseActivity ta) {
		mBaseView = ta;
	    mMugToast = new MugToast(ta);
	}
	
	public Landmark getLocation() {
		return map.poi[location];
	}
	
	
	public void nextTurn(){
		// Refresh Market Prices.
		marketRefresh();

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

	public void marketRefresh() {
		int i;
		
		for (i=0;i<goods.length;i++){
			System.out.println(rawPrices.length);
			System.out.println(goods.length);
			System.out.println(mktPrices.length);
			mktPrices[i] = (int) Math.round(rawPrices[i] * Math.random());
		}
		
	}
	
}
