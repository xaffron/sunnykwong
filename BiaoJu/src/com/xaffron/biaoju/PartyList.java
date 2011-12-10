package com.xaffron.biaoju;

import java.util.LinkedList;

public class PartyList extends LinkedList<Hero> {

	static final long serialVersionUID=1; 
	
	public PartyList() {
		super();
	}

	public Hero getPlayer() {
		return getFirst();
	}
	
	public Hero changeProtag(Hero h) {
		if (size()>0) removeFirst();
		addFirst(h);
		return h;
	}
	
	public Hero addToParty(Hero h) {
		int sz = size();
		h.posInParty = sz;
		add(h);
		return h;
	}

	public int removeFromParty(Hero h) {
		remove(h.posInParty);
		h.posInParty=0;
		return h.posInParty;
	}
}
