package adk.team.action;

import adk.team.tactics.TacticsFire;
import rescuecore2.messages.Message;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.messages.AKExtinguish;
import rescuecore2.worldmodel.EntityID;

public class ActionExtinguish extends ActionTarget {
    
    private int power;
    
    public ActionExtinguish(TacticsFire tactics, EntityID targetID, int maxPower) {
        super(tactics, targetID);
        this.power = maxPower;
    }
    
    public ActionExtinguish(TacticsFire tactics, Building building, int maxPower) {
        this(tactics, building.getID(), maxPower);
    }

    public int getPower() {
        return this.power;
    }
    
    @Override
    public Message getCommand(EntityID agentID, int time) {
        return new AKExtinguish(agentID, time, this.target, this.power);
    }
    
    @Override
    public boolean equals(Object other){
    	if(other instanceof ActionExtinguish){
    		ActionExtinguish a = (ActionExtinguish) other;
    		return this.power == a.getPower() && this.getTarget() == a.getTarget();
    	}
    	return false;
    }
    
    @Override
    public String toString(){
    	return String.format("ActionExtinguish; target=%s, power=%d", getTarget(), getPower());
    }
}