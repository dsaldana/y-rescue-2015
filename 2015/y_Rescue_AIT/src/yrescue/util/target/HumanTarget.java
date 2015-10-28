package yrescue.util.target;

import java.security.InvalidParameterException;
import java.util.List;

import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.WorldProvider;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.standard.entities.Building;

public class HumanTarget {
	
	/**
	 * Human types
	 * @author h3ct0r
	 *
	 */
	public static enum HumanTypes {
		CIVILIAN(1),
		AMBULANCE(2),
		POLICE(3),
		FIREMAN(4);
		
		private int priorityValue;

		HumanTypes(int val) {
	        this.priorityValue = val;
	    }

	    public int getValue() {
	        return this.priorityValue;
	    }
	}
	
	private Human human;
	private float priorityWeight = 0;
	private float utility = 0.0f;
	private int lastTimeUpdated = 0;
	private HumanTypes hType;
	private RouteSearcher routeSearcher;
	private WorldProvider<? extends Human> provider;
	
	public final int AVERAGE_MOVE_TO_TARGET = 4;
	
	public HumanTarget(Human h, HumanTypes hType, WorldProvider<? extends Human> provider){
		if(h == null || hType == null) throw new InvalidParameterException("Some parameter is null");
		this.human = h;
		this.hType = hType;
		this.provider = provider;
		this.routeSearcher = new BasicRouteSearcher(this.provider);
		
		if(hType.equals(HumanTypes.CIVILIAN)){
			this.priorityWeight = 0.55f;
		}
		else if(hType.equals(HumanTypes.AMBULANCE)){
			this.priorityWeight = 0.75f;
		}
		else if(hType.equals(HumanTypes.POLICE)){
			this.priorityWeight = 0.65f;
		}
		else{
			this.priorityWeight = 0.55f;
		}
	}
	
	private int getDeathTime(int time) {
		return Math.min(getFireDeathTime(time), estimatedDeathTime(this.human.getHP(), this.human.getDamage(), time));
	}
	
	public Human getHuman(){
		return this.human;
	}
	
	public HumanTypes getHumanType(){
		return this.hType;
	}
	
	public float getUtility(){
		return this.utility;
	}
	
	public void updateUtility(int time, Human ambulance, List<HumanTarget> ambulanceList){
		if(time > 120) this.priorityWeight = 1.33f;
		float actualUtility = 0;
		
		try {
			
			int duration = getTimeToPerformTask(time, ambulance, ambulanceList);
			int deathTime = getDeathTime(time);

			int durationFactor = 0;
			int deathTimFactor = 0;

			switch (time / 100) {
			case 0:
				durationFactor = 2;
				deathTimFactor = 1;
				break;
			case 1:
				durationFactor = 1;
				deathTimFactor = 2;
				break;
			case 2:
				durationFactor = 0;
				deathTimFactor = 3;
				break;
			default:
				durationFactor = 1;
				deathTimFactor = 1;
				break;
			}

			actualUtility = (duration * durationFactor) * 10 + ((deathTime - time) * deathTimFactor) * 5;
			actualUtility /= 1000;
			actualUtility *= this.priorityWeight;

			Logger.trace(String.format("Utility for Human: %s timeToPerformTask: %d, deathTime: %d, Utility: %f", human.toString(), duration, deathTime, actualUtility));

		} catch (ArithmeticException ae) {
			actualUtility = Integer.MAX_VALUE;
			System.err.println("Division by zero in AmbulanceDecision.CostOfrescuingHuman()");
		}

		this.lastTimeUpdated = time;
		this.utility = actualUtility;
	}
	
	private int getTimeToPerformTask(int time, Human ambulance, List<HumanTarget> ambulanceList) {
		int cycle = 0;
		try {
			// Get ambulances in this specific spot
			int ambulancesInThisPos = 0;
			for(HumanTarget ht : ambulanceList){
				if(ht.getHuman().getPosition() == this.human.getPosition()){
					ambulancesInThisPos++;
				}
			}
			
			// Cycles needed to remove burriedness
			if(ambulancesInThisPos > 0) cycle += Math.ceil(this.human.getBuriedness() / (float) ambulancesInThisPos);
			else cycle += this.human.getBuriedness();
				
			// Cycles needed to load and carry Civilian to the nearest refugee
			if (this.hType.equals(HumanTarget.HumanTypes.CIVILIAN)) {
				cycle++;
				Refuge result = PositionUtil.getNearTarget(this.provider.getWorld(), this.provider.getWorld().getEntity(this.human.getID()), this.provider.getRefuges());
				List<EntityID> path = this.routeSearcher.getPath(time, this.human.getPosition(), result);
				if(path != null) cycle += path.size();
			}
			
			// Cycles to move to target
			List<EntityID> path = this.routeSearcher.getPath(time, this.human.getPosition(), ambulance.getPosition());
			long moveWeight = path.size();
			cycle += moveWeight;

		} catch (Exception ex) {
			cycle += this.human.getBuriedness() + 2;
			cycle += (2 * AVERAGE_MOVE_TO_TARGET);
			ex.printStackTrace();
		}
		
		return cycle;
	}
	
	private int getFireDeathTime(int time) {
		StandardEntity humanStandardE = this.provider.getWorld().getEntity(this.human.getPosition());
		
		if (!this.human.isPositionDefined() 
				|| humanStandardE instanceof Refuge 
				|| !(humanStandardE instanceof Building))
			return 1000;

		if (humanStandardE instanceof Building && !((Building) humanStandardE).isOnFire())
			return 1000;
		
		if (!(humanStandardE instanceof Civilian) && this.human.getBuriedness() == 0)
			return 1000;
		
		Building civPosition = (Building) humanStandardE;
		int extra = getEstimatedHp(time) / (500 + getEstimatedDamage(time));

		if (civPosition.isOnFire()) {
			return extra + time;
		}
		
		return extra;
	}
	
	private int estimatedDeathTime(int hp, double dmg,int updatetime) {
		int agenttime = 1000;
		int count = agenttime - updatetime;
		if (count <= 0 || dmg == 0) return hp;

		double kbury = 0.000035;
		double kcollapse = 0.00025;
		double darsadbury = -0.0014 * updatetime + 0.64;
		double burydamage = dmg * darsadbury;
		double collapsedamage = dmg - burydamage;

		while (count > 0) {
			int time = agenttime - count;
			burydamage += kbury * burydamage * burydamage + 0.11 ;
			collapsedamage += kcollapse * collapsedamage * collapsedamage+0.11 ;
			dmg=burydamage+collapsedamage;
			count--;
			hp -= dmg;
			if (hp <= 0) return time;
		}
		
		return 1000;
	}
	
	private int getEstimatedDamage(int time) {
		int count = this.lastTimeUpdated - time;
		if (count <= 0)
			return this.human.getDamage();
		double dmg = this.human.getDamage();
		double k = 0.00025;
		while (count > 0) {
			dmg = dmg + k * dmg * dmg;
			count--;
		}
		return (int) Math.round(dmg);
	}

	private int getEstimatedHp(int time) {
		int count = this.lastTimeUpdated - time;
		if (count <= 0 || this.human.getDamage() == 0)
			return this.human.getHP();
		double hp = this.human.getHP();
		double dmg = this.human.getDamage();
		double k = 0.00025;
		while (count > 0) {
			dmg = dmg + k * dmg * dmg;
			count--;
			hp -= dmg;
		}
		if (hp <= 0)
			return 0;
		return (int) Math.round(hp);
	}
}
