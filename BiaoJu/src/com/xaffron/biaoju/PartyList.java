package com.xaffron.biaoju;

import java.util.Iterator;
import java.util.LinkedList;

public class PartyList extends LinkedList<Character> {

	static final long serialVersionUID=1; 
	int weakestHP;
	Character weakest;
	
	
	public PartyList() {
		super();
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
}
