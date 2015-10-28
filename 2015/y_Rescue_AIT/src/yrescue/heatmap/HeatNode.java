package yrescue.heatmap;

import java.security.InvalidParameterException;

import rescuecore2.worldmodel.EntityID;

public class HeatNode {
	
	/**
	 * Priority levels used as the function to decrease heat
	 * @author h3ct0r
	 *
	 */
	public static enum PriorityLevel {
		HIGH(1),
		MEDIUM(2),
		LOW(3),
		VERY_SLOW(4),
		ULTRA_SLOW(5);
		
		private int priorityValue;

		PriorityLevel(int val) {
	        this.priorityValue = val;
	    }

	    public int getValue() {
	        return this.priorityValue;
	    }
	}
	
	public static final float MAX_HEAT = 1.0f;
	public static final float MIN_HEAT = 0.0f;
	
	public static final float HIGH_HEAT_FACTOR = 0.7f;
	public static final float MEDIUM_HEAT_FACTOR = 0.05f;
	public static final float LOW_HEAT_FACTOR = 0.03f;
	public static final float VERY_LOW_HEAT_FACTOR = 0.015f;
	public static final float ULTRA_SLOW_HEAT_FACTOR = 0.01f;
	
	private EntityID entity = null;
	private PriorityLevel priority = null;
	private Integer time = null;
	private float heat = 0.3f;
	private float heatFactor = 0.0f;
	
	public HeatNode(EntityID entity, PriorityLevel priority, Integer time) {
		if(entity == null || priority == null || time == null){
			throw new IllegalArgumentException("Some of the parameters are null");
		}
		
		this.entity = entity;
		this.priority = priority;
		this.time = time;
		
		// Define heatFactor early
		if(this.priority.equals(PriorityLevel.HIGH)) heatFactor = HIGH_HEAT_FACTOR;
		else if(this.priority.equals(PriorityLevel.MEDIUM)) heatFactor = MEDIUM_HEAT_FACTOR;
		else if(this.priority.equals(PriorityLevel.LOW)) heatFactor = LOW_HEAT_FACTOR;
		else if(this.priority.equals(PriorityLevel.VERY_SLOW)) heatFactor = VERY_LOW_HEAT_FACTOR;
		else if(this.priority.equals(PriorityLevel.ULTRA_SLOW)) heatFactor = ULTRA_SLOW_HEAT_FACTOR;
		
		if(heatFactor == 0.0f) heatFactor = LOW_HEAT_FACTOR;
	}
	
	/**
	 * Update the heat of the node, given the actual time and the previous time
	 * @param from
	 * @param actualTime
	 */
	public void updateHeat(EntityID from, Integer actualTime){
		updateHeatByTime(from, Math.abs(actualTime - this.time));
		this.time = actualTime;
	}
	
	/**
	 * Update the heat of the node, given a time and a heat
	 * @param from
	 * @param heat
	 */
	public void updateHeat(EntityID from, Integer actualTime, float heat){
		if(heat > HeatNode.MAX_HEAT || heat < HeatNode.MIN_HEAT){
			throw new InvalidParameterException("Invalid heat value");
		}
		
		this.heat = heat;
		this.time = actualTime;
	}
	
	/**
	 * Get the actual calculated heat of this node
	 * @return
	 */
	public float getHeat(){
		return this.heat;
	}
	
	/**
	 * Get the EntityID that represents this node
	 * @return
	 */
	public EntityID getEntity(){
		return this.entity;
	}
	
	/**
	 * Get the priorityLevel of this node
	 * @return
	 */
	public PriorityLevel getPriorityLevel(){
		return this.priority;
	}
	
	/**
	 * Private function to update the heat of this node, given an actual and previous time 
	 * @param from
	 * @param timeDiff
	 */
	private void updateHeatByTime(EntityID from, int timeDiff){
		if((from != null && entity != null) && (from.getValue() == entity.getValue())){
			this.heat = 1.0f;
		}
		else{
			for(int i = 0; i < timeDiff; i++){
				this.heat = this.heat - (this.heat * heatFactor);
				if(this.heat < 0.0f) this.heat = 0.0f;
			}
		}
	}
	
	public void setHeat(float heat){
		this.heat = heat;
	}
	
	public void setPriority(PriorityLevel priorityLevel){
		if(priorityLevel != null) this.priority = priorityLevel;
	}
}
