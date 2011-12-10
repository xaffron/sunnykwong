package com.xaffron.biaoju;

public class Hero {

	String name;
	String title;
	String nick;
	String portrait;
	
	int level;
	int basestr;
	int basedex;
	int basewis;
	int baseluck;
	float strmod;
	float dexmod;
	float wismod;
	int str;
	int dex;
	int wis;
	int luck;
	int weapon;
	int armor;
	int artifact;
	int hp;
	int shield;
	
	int posInParty;
	
	public Hero(String a, String b, String c) {
		super();
		title=a;
		name=b;
		nick=c;
		portrait="generic";
		posInParty=0;
		level=1;
		weapon=0;
		armor=0;
		artifact=0;
		hp=100;
		shield=0;
	}
	public Hero changeTitle(String a) {
		title=a;
		return this;
	}
	public Hero changeNick(String a) {
		nick=a;
		return this;
	}
	public Hero initBase(int val){
		basestr=val;
		basedex=val;
		basewis=val;
		baseluck=val;
		str=val;
		dex=val;
		wis=val;
		strmod=1;
		dexmod=1;
		wismod=1;
		return this;
	}

	public static Hero chooseProtag(){
		Hero protag = new Hero("齊天大聖","孫悟空","從奇石中蹦出來的神猴").initBase(5);
		return protag;
	}
	
}
