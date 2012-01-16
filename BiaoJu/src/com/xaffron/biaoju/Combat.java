package com.xaffron.biaoju;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.StringTokenizer;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ListView;


public class Combat {

	boolean inProgress=false;
	
	PartyList friends;
	PartyList foes;
	ArrayList<Character> initiativeOrder;

	TurnActivity tact;
	
	public Combat(PartyList f, TurnActivity ta) {
		tact = ta;
		friends = f;

		//Generate random foe
		foes = new PartyList();
		Character headlineFoe = Character.generateFoe(1);
		
		BJ.TACT.writeBlow("You see: " + headlineFoe.desc);

		foes.add(headlineFoe);
		inProgress=true;

		calculateInitOrder();
		
		boolean frienddead = foes.getFirst().harm(friends.getFirst());
		if (frienddead) {
			tact.writeBlow("Game over!");
			inProgress = false;
		}
		
	}

	public void calculateInitOrder() {
		initiativeOrder = new ArrayList<Character>(friends.size()+foes.size());
		
		// Calculate Initiatives
		for (Character c:friends) {
			c.initiative = GM.diceRoll(1, 20, GM.getAbilityModifier(c.dex));
			if (initiativeOrder.size()==0) {
				initiativeOrder.add(c);
			} else if (c.initiative <= initiativeOrder.get(initiativeOrder.size()-1).initiative){
				initiativeOrder.add(c);
			} else {
				for (int i=0; i<initiativeOrder.size();i++) {
					if (c.initiative > initiativeOrder.get(i).initiative) {
						initiativeOrder.add(i,c);
						break;
					}
				}
			}
		}

		for (Character c:foes) {
			c.initiative = GM.diceRoll(1, 20, GM.getAbilityModifier(c.dex));
			if (initiativeOrder.size()==0) {
				initiativeOrder.add(c);
			} else if (c.initiative <= initiativeOrder.get(initiativeOrder.size()-1).initiative){
				initiativeOrder.add(c);
			} else {
				for (int i=0; i<initiativeOrder.size();i++) {
					if (c.initiative > initiativeOrder.get(i).initiative) {
						initiativeOrder.add(i,c);
						break;
					}
				}
			}
		}
	}
	
	public void nextTurn(int action){
		// Roll the dice for this turn.
		double turnLuck = Math.random() *1. + 0.5;   //luck factor
		switch (action) {
			case GM.ATTACK:
			
				boolean foedead = friends.getFirst().harm(foes.getFirst());
				if (foedead) {
					foes.remove(0);
				}
				
				break;
			case GM.ITEM:
				tact.writeBlow(friends.getFirst().name + " uses an item... fails!");
				break;
			case GM.RECRUIT:
				tact.writeBlow(foes.getFirst().name + " cannot be recruited!");
				break;
			case GM.RUN:
				tact.writeBlow(friends.getFirst().name + " flees successfully!");
				break;
		default:
		}
		if (foes.size()==0) {
			tact.writeBlow("You are victorious!");
			inProgress=false;
		}
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
