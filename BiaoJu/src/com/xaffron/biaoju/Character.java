package com.xaffron.biaoju;

public class Character {

	String name;
	String title;
	String desc;
	String portrait;
	
	//Combat-related
	boolean flatFooted;
	boolean enemyAware;
	int initiative;
	
	int level;
	int str;
	int dex;
	int con;
	int intel;
	int wis;
	int cha;

	int ac;
	
	int weapon;
	int armor;
	int artifact;
	int hp;
	int shield;
	
	int posInParty;
	
	public Character(String a, String b, String c) {
		super();
		title=a;
		name=b;
		desc=c;
		portrait="generic";
		posInParty=0;
		level=1;
		weapon=0;
		armor=0;
		artifact=0; 
		hp=100;
		shield=0;
	}
	public boolean harm (Character victim) {
		// How's my aim? If opponent is more dextrous than I am, I'll never hit
		// Luck counts into 10% of aim
		double myaim = Math.max(1, dex-victim.dex) * Math.random() ;
		BJ.TACT.writeBlow("myaim:" + myaim);
		int damage = (int)(str * myaim);
		boolean critical = (dex)*Math.random() + 0.01 > 1;
		if (critical) {
			BJ.TACT.writeBlow(name + "uses his finishing move!");
			damage*=3;
		}
		BJ.TACT.writeBlow(name + " hits " + victim.name + " for "+damage+" damage!");
		victim.hp-=damage;
		if (victim.hp<=0) {
			BJ.TACT.writeBlow(victim.name + " is killed!");
			return true; //victim died
		} else if (victim.hp/(float)victim.hp < 0.2f) {
			BJ.TACT.writeBlow(victim.name + " is critically injured.");
			return false;
		}
		return false;
	}
	public Character changeTitle(String a) {
		title=a;
		return this;
	}
	public Character changeDesc(String a) {
		desc=a;
		return this;
	}
	public Character initBase(int val){
		level=1;
		str=val;
		dex=val;
		wis=val;
		hp=100;
		weapon=0;
		armor=0;
		artifact=0;
		shield=0;
		return this;
	}

	public static Character chooseProtag(){
		Character protag = new Character("齊天大聖","孫悟空","從奇石中蹦出來的神猴").initBase(5);
		return protag;
	}
	
	public static Character generateFoe(int iLevel) {
		final String[] sFirstNames = {"Mog","Peter","Ug","Jojo","Gab"}; 
		final String[] sLastNames = {"Schog","Razog","Trog","Pog","Hog"}; 
		final int iListLength=5;
		Character foe = new Character("An", sFirstNames[(int)(Math.random()*iListLength)]+" "+sLastNames[(int)(Math.random()*iListLength)],"A beast thirsty for your blood.").initBase(5);
		return foe;
	}
}
