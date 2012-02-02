package com.xaffron.biaoju;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;

public class Character {

	static final int BARBARIAN=0,BARD=1,CLERIC=2,DRUID=3,FIGHTER=4,MONK=5,PALADIN=6,RANGER=7,ROGUE=8,SORCERER=9,WIZARD=10;
	static final HashMap<String, Integer> OCCUPATIONMAP = new HashMap<String, Integer>(10);
	String name;
	String type;
	String desc;
	String portrait;
	
	static JSONObject BAREHANDS;
	
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
	int iOccupation;
	String sOccupation;
	int level;
	int hitdie;
	
	JSONObject weapon;
	int armor;
	int artifact;
	int hp;
	int shield;
	int exp;
	
	int posInParty;

	static {
		OCCUPATIONMAP.put("BARBARIAN", BARBARIAN);
		OCCUPATIONMAP.put("BARD", BARD);
		OCCUPATIONMAP.put("CLERIC", CLERIC);
		OCCUPATIONMAP.put("DRUID", DRUID);
		OCCUPATIONMAP.put("FIGHTER", FIGHTER);
		OCCUPATIONMAP.put("MONK", MONK);
		OCCUPATIONMAP.put("PALADIN", PALADIN);
		OCCUPATIONMAP.put("RANGER", RANGER);
		OCCUPATIONMAP.put("ROGUE", ROGUE);
		OCCUPATIONMAP.put("SORCEROR", SORCERER);
		OCCUPATIONMAP.put("WIZARD", WIZARD);
		try {
			BAREHANDS = new JSONObject ("{\"id\":0,\"name\":\"Bare Hands\",\"type\":\"weapon\",\"cost\": 2,\"dmg\": [1,2,0],\"critical\": [20,2],\"weight\": 0}");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Character(JSONObject obj, boolean friendly) {
		super();
		type=obj.optString("name");
		final int iFname=(int)(Math.random()*obj.optJSONArray("fnames").length());
		final int iLname=(int)(Math.random()*obj.optJSONArray("lnames").length());
		name = obj.optJSONArray("fnames").optString(iFname)
				+" "+
				obj.optJSONArray("lnames").optString(iLname);
		desc = "A beast thirsty for your blood.";

		portrait="generic";
		posInParty=0;
		level=1;
		
		sOccupation = obj.optString("class");
		iOccupation = OCCUPATIONMAP.get(sOccupation);
		hitdie = Character.getHitDie(iOccupation);
		
		isFriend = friendly;

		armor=0;
		artifact=0; 
		JSONArray hitdice = obj.optJSONArray("hitdice");
		hp=GM.diceRoll(hitdice.optInt(0), hitdice.optInt(1), hitdice.optInt(2));
		shield=0;
		
		if (friendly) {
			weapon=BJ.jaryEQUIPMENT.optJSONObject(0);
		} else {
			weapon=BAREHANDS;
		}
		
		ac=obj.optInt("ac");
		str=obj.optInt("str");
		dex=obj.optInt("dex");
		con=obj.optInt("con");
		intel=obj.optInt("int");
		wis=obj.optInt("wis");
		cha=obj.optInt("cha");


	}
	public boolean harm (Character victim) {
		int[] baseAttackBonuses = getBaseAttackBonus(iOccupation, level);
		boolean hit, critical;
		int damage=1; //Minimum Damage
		
		for (int baseAttack:baseAttackBonuses) {
			// Roll die
			hit=false;
			critical=false;
			damage=1;

			String sBlow="";
			
			//roll d20 + (Base attack bonus + Strength modifier + size modifier) 
			int attackRoll = GM.diceRoll(1, 20, 0);
			if (attackRoll==1) {
				hit=false;
				sBlow = name + " misses!";
				continue;
			} else if (attackRoll >= weapon.optJSONArray("critical").optInt(0)) {
					hit=true; 
					if (GM.diceRoll(1, 20, baseAttack + GM.getAbilityModifier(str)) > victim.ac) {
						critical = true;
						sBlow = "<Crit>";
					} else {
						critical = false;
					}
			} else if (attackRoll + baseAttack + GM.getAbilityModifier(str)> victim.ac) {
				hit=true;
			}
			
			JSONArray damageArray;
			damageArray = weapon.optJSONArray("dmg");
			
			int hitcount = 1;
			if (critical) {
				hitcount = weapon.optJSONArray("critical").optInt(1);
			}
					
			// If critical, combine multiple attacks
			for (int i =0; i< hitcount;i++) {
				damage += GM.diceRoll(damageArray.optInt(0), damageArray.optInt(1), damageArray.optInt(2) + GM.getAbilityModifier(str));
			}

			final String actorname,victimname;
			if (isFriend) {
				actorname=name;
				victimname=victim.type;
			} else {
				actorname=type;
				victimname=victim.name;
			}
			
			
			// Enforce minimum damage
			if (damage<1) damage=1;
			sBlow +=actorname + " hits " + victimname + " for "+damage+" damage!";
			BJ.TACT.writeBlow(sBlow);
			victim.hp-=damage;
			if (victim.hp<=0) {
				BJ.TACT.writeBlow(victimname + " is killed!");
				return true; //victim died
			} else if (victim.hp/(float)victim.hp < 0.2f) {
				BJ.TACT.writeBlow(victimname + " is critically injured.");
			}
		}
		return false;
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
		armor=0;
		artifact=0;
		shield=0;
		return this;
	}

	public static Character chooseProtag(){
		Character protag = new Character(BJ.jsonPROTAG, true);
		protag.hp=1000;
		return protag;
	}
	
	public static Character generateFoe(int iLevel) {
		
		final int iMonsterCount = BJ.jaryMONSTERS.length();
		final int iMonster = (int)(Math.random()*iMonsterCount);
		final JSONObject jsonMonster = BJ.jaryMONSTERS.optJSONObject(iMonster);
		return new Character(jsonMonster, false);
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
