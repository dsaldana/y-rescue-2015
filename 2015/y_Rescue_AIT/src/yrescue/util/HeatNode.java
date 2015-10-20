package yrescue.util;

import rescuecore2.worldmodel.EntityID;

public class HeatNode {
	
	/**
	 * Priority levels used as the function to decrease heat
	 * @author h3ct0r
	 *
	 */
	public static enum PriorityLevel {
		LOW,
		MEDIUM,
		HIGH;
	}
	
	private EntityID entity = null;
	private PriorityLevel priority = null;
	private Integer time = null;
	private float heat = 0.3f;
	private float heatFactor = 0.0f;
	
	public HeatNode(EntityID entity, PriorityLevel priority, Integer time) {
		this.entity = entity;
		this.priority = priority;
		this.time = time;
		
		// Define heatFactor early
		if(this.priority.equals(PriorityLevel.LOW)) heatFactor = 0.03f;
		else if(this.priority.equals(PriorityLevel.MEDIUM)) heatFactor = 0.05f;
		else heatFactor = 0.07f;
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
}
