package com.xaffron.biaoju;

import org.json.JSONObject;

public class Character {

	static final int BARBARIAN=0,BARD=1,CLERIC=2,DRUID=3,FIGHTER=4,MONK=5,PALADIN=6,RANGER=7,ROGUE=8,SORCERER=9,WIZARD=10;
	
	String name;
	String title;
	String desc;
	String portrait;
	
	//Combat-related
	boolean flatFooted;
	boolean enemyAware;
	boolean isFriend;
	int initiative;
	
	int str;
	int dex;
	int con;
	int intel;
	int wis;
	int cha;

	int ac;
	int baseAttackBonus;
	int occupation;
	int level;
	int hitdie;
	
	int weapon;
	int armor;
	int artifact;
	int hp;
	int shield;
	int exp;
	
	int posInParty;

	
	public Character(String a, String b, String c, int occup, boolean friendly) {
		super();
		title=a;
		name=b;
		desc=c;
		portrait="generic";
		posInParty=0;
		level=1;
		
		occupation = occup;
		hitdie = Character.getHitDie(occup);
		
		weapon=0;
		armor=0;
		artifact=0; 
		hp=100;
		shield=0;
		isFriend=friendly;
	}
	public boolean harm (Character victim) {
		int[] baseAttackBonuses = getBaseAttackBonus(occupation, level);
		boolean hit, critical;
		int damage=1; //Minimum Damage
		
		for (int baseAttack:baseAttackBonuses) {
			// Roll die
			hit=false;
			critical=false;
			damage=1;
			
			//roll d20 + (Base attack bonus + Strength modifier + size modifier) 
			int attackRoll = GM.diceRoll(1, 20, 0);
			if (attackRoll==1) {
				hit=false;
				BJ.TACT.writeBlow(name + " misses!");
				continue;
			}
			else if (attackRoll==20) {
				hit=true; 
				if (GM.diceRoll(1, 20, baseAttack + GM.getAbilityModifier(str)) > victim.ac) {
					critical = true;
					BJ.TACT.writeBlow(name + " scores a critical hit!");
				} else {
					critical = false;
				}
			} else if (attackRoll + baseAttack + GM.getAbilityModifier(str)> victim.ac) {
				hit=true;
			}
				
			damage = GM.diceRoll(1, 3, GM.getAbilityModifier(str));
			if (critical) {
				// If critical, combine two attacks
				damage += GM.diceRoll(1, 3, GM.getAbilityModifier(str));
			}
			// Enforce minimum damage
			if (damage<1) damage=1;

			BJ.TACT.writeBlow(name + " hits " + victim.name + " for "+damage+" damage!");
			victim.hp-=damage;
			if (victim.hp<=0) {
				BJ.TACT.writeBlow(victim.name + " is killed!");
				return true; //victim died
			} else if (victim.hp/(float)victim.hp < 0.2f) {
				BJ.TACT.writeBlow(victim.name + " is critically injured.");
			}
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
		Character protag = new Character("The","Great Hero", "That's you!", Character.FIGHTER, true).initBase(16);
		protag.hp=1000;
//		Character protag = new Character("齊天大聖","孫悟空","從奇石中蹦出來的神猴", Character.FIGHTER, true).initBase(11);
		return protag;
	}
	
	public static Character generateFoe(int iLevel) {
		
		final int iMonsterCount = BJ.jaryMONSTERS.length();
		final int iMonster = (int)(Math.random()*iMonsterCount);
		final JSONObject jsonMonster = BJ.jaryMONSTERS.optJSONObject(iMonster);
		
		final int iListLength=5;
		Character foe = new Character("An", jsonMonster.optJSONArray("fnames").optString(0)
				+" "+
				jsonMonster.optJSONArray("lnames").optString(0)
				+" the "+
				jsonMonster.optString("name"),
				"A beast thirsty for your blood.", Character.FIGHTER, false);
		foe.initBase(1);
		foe.ac=jsonMonster.optInt("ac");
		foe.str=jsonMonster.optInt("str");
		foe.dex=jsonMonster.optInt("dex");
		foe.con=jsonMonster.optInt("con");
		foe.intel=jsonMonster.optInt("int");
		foe.wis=jsonMonster.optInt("wis");
		foe.cha=jsonMonster.optInt("cha");

		return foe;
	}

	//Straight lookup functions.
	static public int getHitDie(int occup) {
		switch (occup) {
		case Character.BARBARIAN:
			return 12;
		case Character.FIGHTER:
		case Character.PALADIN:
			return 10;
		case Character.CLERIC:
		case Character.DRUID:
		case Character.MONK:
		case Character.RANGER:
			return 8;
		case Character.BARD:
		case Character.ROGUE:
			return 6;
		case Character.SORCERER:
		case Character.WIZARD:
			return 4;
		default:
			//do nothing
		}
		return 0;
	}
	
	static public int[] getBaseAttackBonus(int occup, int level) {

		switch (occup) {		
		case Character.BARBARIAN:
		case Character.FIGHTER:
		case Character.PALADIN:
		case Character.RANGER:
			switch (level) {
			case 1:
				return new int[]{1};
			case 2:
				return new int[]{2};
			case 3:
				return new int[]{3};
			case 4:
				return new int[]{4};
			case 5:
				return new int[]{5};
			case 6:
				return new int[]{6,1};
			case 7:
				return new int[]{7,2};
			case 8:
				return new int[]{8,3};
			case 9:
				return new int[]{9,4};
			case 10:
				return new int[]{10,5};
			case 11:
				return new int[]{11,6,1};
			case 12:
				return new int[]{12,7,2};
			case 13:
				return new int[]{13,8,3};
			case 14:
				return new int[]{14,9,4};
			case 15:
				return new int[]{15,10,5};
			case 16:
				return new int[]{16,11,6,1};
			case 17:
				return new int[]{17,12,7,2};
			case 18:
				return new int[]{18,13,8,3};
			case 19:
				return new int[]{19,14,9,4};
			case 20:
				return new int[]{20,15,10,5};
			default:
				return new int[]{0};
			}
		case Character.BARD:
		case Character.CLERIC:
		case Character.DRUID:
		case Character.MONK:
		case Character.ROGUE:
			switch (level) {
			case 1:
				return new int[]{0};
			case 2:
				return new int[]{1};
			case 3:
				return new int[]{2};
			case 4:
			case 5:
				return new int[]{3};
			case 6:
				return new int[]{4};
			case 7:
				return new int[]{5};
			case 8:
			case 9:
				return new int[]{6,1};
			case 10:
				return new int[]{7,2};
			case 11:
				return new int[]{8,3};
			case 12:
			case 13:
				return new int[]{9,4};
			case 14:
				return new int[]{10,5};
			case 15:
				return new int[]{11,6,1};
			case 16:
			case 17:
				return new int[]{12,7,2};
			case 18:
				return new int[]{13,8,3};
			case 19:
				return new int[]{14,9,4};
			case 20:
				return new int[]{15,10,5};
			default:
				return new int[]{0};
			}
		case Character.SORCERER:
		case Character.WIZARD:
			switch (level) {
			case 1:
				return new int[]{0};
			case 2:
			case 3:
				return new int[]{1};
			case 4:
			case 5:
				return new int[]{2};
			case 6:
			case 7:
				return new int[]{3};
			case 8:
			case 9:
				return new int[]{4};
			case 10:
			case 11:
				return new int[]{5};
			case 12:
			case 13:
				return new int[]{6,1};
			case 14:
			case 15:
				return new int[]{7,2};
			case 16:
			case 17:
				return new int[]{8,3};
			case 18:
			case 19:
				return new int[]{9,4};
			case 20:
				return new int[]{10,5};
			default:
				return new int[]{0};
			}
		default:
			//do nothing
		}
		return new int[]{0};
	}

}
