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
	int whoseTurn;
	
	PartyList friends;
	PartyList foes;
	ArrayList<Character> initiativeOrder;

	public Combat(PartyList f) {
		friends = f;

		//Generate random foe
		foes = new PartyList();
		Character headlineFoe = Character.generateFoe(1);
		
		BJ.TACT.writeBlow("You see: " + headlineFoe.desc);

		foes.add(headlineFoe);
		
		// Flag inProgress:  Once this var is set to false, the combat is considered over.
		inProgress=true;

		calculateInitOrder();

		// Person with the most initiative is first to move.
		whoseTurn=0;
		friends.getWeakest();
		foes.getWeakest();
		keepGoing(GM.PENDING);
		
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
	
	public void keepGoing(int action){
		//Endless loop unless broken
		while (true) {
			// Start turn; keep going down initiative list from marker
			Character actor = initiativeOrder.get(whoseTurn);
			if (actor.isFriend && action==GM.PENDING) {
				break;
			}
			whoseTurn++;
			if (whoseTurn==initiativeOrder.size()) whoseTurn=0;
			if (actor.isFriend) {
				switch (action) {
				case GM.ATTACK:
					boolean foedead = actor.harm(foes.getFirst());
					if (foedead) {
						foes.remove(0);
					}
					break;
				case GM.ITEM:
					BJ.TACT.writeBlow(actor.name + " uses an item... fails!");
					break;
				case GM.RECRUIT:
					BJ.TACT.writeBlow(actor.name + " cannot be recruited!");
					break;
				case GM.RUN:
					BJ.TACT.writeBlow(actor.name + " flees successfully!");
					break;
				default:
					//do nothing
				}
				action=GM.PENDING;
			}
			if (!actor.isFriend) {
//				switch (action) {
//					case GM.ATTACK:
						boolean dead = actor.harm(friends.weakest);
						if (dead) {
							friends.remove(friends.weakest);
						}
//						break;
//					case GM.ITEM:
//						BJ.TACT.writeBlow(actor.name + " uses an item... fails!");
//						break;
//					case GM.RECRUIT:
//						BJ.TACT.writeBlow(actor.name + " cannot be recruited!");
//						break;
//					case GM.RUN:
//						BJ.TACT.writeBlow(actor.name + " flees successfully!");
//						break;
//					default:
//						//do nothing
//				}
			}
			if (friends.size()==0) {
				BJ.TACT.writeBlow("You have been defeated...");
				inProgress=false;
				return;
			}
			if (foes.size()==0) {
				BJ.TACT.writeBlow("You are victorious!");
				inProgress=false;
				return;
			}
			friends.getWeakest();
			foes.getWeakest();
			BJ.TACT.writeConsole(
					"You are " + Math.round(friends.dDistFromTgt) + " leagues from " + BJ.jaryTOWNES.optJSONObject(friends.iLocTarget).optString("name") + ".\n" +
					friends.getPlayer().name + "'s HP:" + friends.getPlayer().hp + "\t" + foes.getFirst().name + "'s HP:" + foes.getFirst().hp );
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
