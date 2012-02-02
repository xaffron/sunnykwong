package com.xaffron.biaoju;

import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;

public class PartyList extends LinkedList<Character> {

	static final long serialVersionUID=1; 
	int weakestHP;
	Character weakest;
	int iLocTarget, iTargetX, iTargetY;
	double dLocationX, dLocationY, dDistFromTgt;
	
	public PartyList() {
		super();
		iLocTarget=0;
		dDistFromTgt=0;
		JSONArray temp = BJ.jaryTOWNES.optJSONObject(0).optJSONArray("location");
		iTargetX = temp.optInt(0);
		iTargetY = temp.optInt(1);
		dLocationX = iTargetX;
		dLocationY = iTargetY;
	}

	public Character getWeakest() {
		weakestHP=getPlayer().hp;
		weakest = getPlayer();
		Iterator<Character> i = iterator();
		while (i.hasNext()){
			Character ch = i.next();
			if (ch.hp < weakestHP) {
				weakestHP = ch.hp;
				weakest = ch;
			}
		}
		return weakest;
	}
	
	public Character getSlowest() {
		int slowest=getPlayer().dex;
		Character result = getPlayer();
		Iterator<Character> i = iterator();
		while (i.hasNext()){
			Character ch = i.next();
			if (ch.dex < slowest) {
				slowest = ch.dex;
				result = ch;
			}
		}
		return result;
	}
	
	public Character getPlayer() {
		return getFirst();
	}
	
	public Character changeProtag(Character h) {
		if (size()>0) removeFirst();
		addFirst(h);
		return h;
	}
	
	public Character addToParty(Character h) {
		int sz = size();
		h.posInParty = sz;
		add(h);
		return h;
	}

	public int removeFromParty(Character h) {
		remove(h.posInParty);
		h.posInParty=0;
		return h.posInParty;
	}
	
	public int move(double dDistance) {
			dDistFromTgt-=dDistance;
			if (dDistFromTgt<=0) {
				dLocationX = iTargetX;
				dLocationY = iTargetY;
				dDistFromTgt=0;
				BJ.TACT.writeBlow(null);
				BJ.TACT.writeConsole(
						"You arrive at " + BJ.jaryTOWNES.optJSONObject(iLocTarget).optString("name") + ".\n" +
						getPlayer().name + "'s HP:" + getPlayer().hp);

				return BJ.PARTYARRIVED;
			} else {
				double angle = Math.atan((iTargetY-dLocationY)/(iTargetX-dLocationX));
				dLocationX += dDistance * Math.cos(angle);
				dLocationY += dDistance * Math.sin(angle);
				
			    // Will we encounter combat?
				// 1d20 + dex modifier
				if (GM.diceRoll(1,20,GM.getAbilityModifier(getSlowest().dex)) > 10) {
					BJ.TACT.writeBlow(null);
					BJ.TACT.writeConsole(
							"You are " + Math.round(dDistFromTgt) + " leagues from " + BJ.jaryTOWNES.optJSONObject(iLocTarget).optString("name") + ".\n" +
							getPlayer().name + "'s HP:" + getPlayer().hp);
					return BJ.PARTYNOENCOUNTER;
				} else {
					return BJ.PARTYENCOUNTER;
				}
			}
			
	}
}
