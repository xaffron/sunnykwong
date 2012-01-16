package com.xaffron.biaoju;

import java.util.LinkedList;

public class PartyList extends LinkedList<Character> {

	static final long serialVersionUID=1; 
	
	public PartyList() {
		super();
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
