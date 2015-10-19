package yrescue.tactics;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.MDC;

import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.util.DistanceSorter;
import yrescue.action.ActionRefill;
import yrescue.message.information.MessageBlockedArea;
import yrescue.util.YRescueBuildingSelector;
//import yrescue.util.YRescueRouteSearcher;
import adk.sample.basic.event.BasicBuildingEvent;
import adk.sample.basic.tactics.BasicTacticsFire;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionExtinguish;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.util.BuildingSelector;
import adk.team.util.RouteSearcher;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;

public class YRescueTacticsFire extends BasicTacticsFire {

    private List<StandardEntity> refugeIDs;
	private List<StandardEntity> hydrantIDs;

	@Override
    public String getTacticsName() {
        return "Y-Rescue Firefighter";
    }

    @Override
    public BuildingSelector initBuildingSelector() {
        return new YRescueBuildingSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
    	return new BasicRouteSearcher(this);
        //return new YRescueRouteSearcher(this, new RouteManager(this.world));
    }


    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicBuildingEvent(this, this));
    }
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.buildingSelector = this.initBuildingSelector();
        
        //Building the Lists of Refuge and Hydrant
        Collection<StandardEntity> refuge = this.world.getEntitiesOfType(StandardEntityURN.REFUGE);
        refugeIDs = new ArrayList<StandardEntity>();
        refugeIDs.addAll(refuge);
        Collection<StandardEntity> hydrant = this.world.getEntitiesOfType(StandardEntityURN.HYDRANT);
        hydrantIDs = new ArrayList<StandardEntity>();
        hydrant.addAll(hydrant);
        
        this.refuge_rate = this.config.getIntValue("fire.tank.refill_hydrant_rate");
        this.hydrant_rate = this.config.getIntValue("fire.tank.refill_rate");
        this.tank_maximum = this.config.getIntValue("fire.tank.maximum");
        
        MDC.put("agent", this);
        MDC.put("location", location());
        
        Logger.debug(String.format("---- FireFighter ON. maxDistance=%d, sightDistance=%d ----", this.maxDistance, this.sightDistance));
    }

    public boolean onWaterSource() {
    	if (this.location instanceof Refuge) return true;
    	if (this.location instanceof Hydrant) return true;
    	return false;
    }
    
    private boolean isWaterLessThan(double percentage) {
    	return (this.me.getWater() < this.maxWater * percentage);
    }
    
    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            if(entity instanceof Building) {
            	Building b = (Building) entity;
                
                if (b.isOnFire()) {
                	this.getBuildingSelector().add(b);
                	manager.addSendMessage(new MessageBuilding(b));		//report to other firefighters the building i've seen
                    Logger.trace("Added outgoin' msg about burning building: " + b);
                }
                
            }
            else if(entity instanceof Civilian) {
                Civilian civilian = (Civilian)entity;
                if(civilian.getBuriedness() > 0) {
                    manager.addSendMessage(new MessageCivilian(civilian));
                }
            }
            /*else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                manager.addSendMessage(new MessageRoad((Road)this.world.getEntity(blockade.getPosition()), blockade, false));
            }*/
        }
    }
    
    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    }

    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        //this.refugeList.get(-1); //triggers exception to test failsafe
        MDC.put("location", location());
        
        // Check if the agent is stuck
        if (this.tacticsAgent.stuck(currentTime)){
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID(), this.target));
        	Logger.trace("I'm blocked. Added a MessageBlockedArea");
    		return new ActionRest(this);	//does nothing...
    	}
        
        // Check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
            manager.addSendMessage(new MessageFireBrigade(this.me, MessageFireBrigade.ACTION_REST, this.agentID));
            for(StandardEntity entity : this.world.getObjectsInRange(this.me, this.maxDistance)) {
                if(entity instanceof Building) {
                    Building building = (Building)entity;
                    this.target = building.getID();
                    //if (building.isOnFire() && (this.world.getDistance(this.agentID, this.target) <= this.maxDistance)) {
                    if(building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() && building.getBrokenness() > 10) {
                    	System.out.println(">>>>>>>>> Teste ");
                        return new ActionExtinguish(this, this.target, this.maxPower);
                    }
                }
            }
            return new ActionRest(this);
        }
        
        // Check if the agent got inside a building on fire
        EntityID locationID = this.me.getPosition();
        StandardEntity location = this.world.getEntity(locationID);
        if(location instanceof Building) {
            Building b = (Building)location;
            if(b.isOnFire()) {        	
                for(StandardEntity entity : this.world.getObjectsInRange(this.me, this.maxDistance/2)) {
                    if(entity instanceof Road) {
                        Road road = (Road)entity;
                        List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, road);
                        if(path != null) {
                            return new ActionMove(this, path);
                        }
                    }
                }
            }
        }

        // Max Distance
        //this.maxDistance = 25000;
        YRescueBuildingSelector bs = (YRescueBuildingSelector) buildingSelector;
        
        Logger.info(String.format("I know %d buildings on fire", bs.buildingList.size()));
        //FIXME soh enche de agua até 20% criar uma flag pra marcar q ta enchendo
        if(onWaterSource() && isWaterLessThan(1.0)) {
            this.target = null;
            Logger.info(">>>>>>> Refill = " + this.me.getWater());
            return new ActionRest(this);
        }
        
        // Refill
        if(me.isWaterDefined() && me.getWater() < maxPower) {
        	this.target = null;
        	return new ActionRefill(this,refugeIDs,hydrantIDs);
        }
        
        // Select new target
        this.target = this.target == null ? this.buildingSelector.getNewTarget(currentTime) : this.buildingSelector.updateTarget(currentTime, this.target);
        
        // If there is no target then walk randomly
        if(this.target == null) {
            return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        
        // Check if the robot is not close to the target then get closer
        // Also goes out of water source to throw water
        if(this.world.getDistance(this.agentID, this.target) > this.maxDistance || this.onWaterSource()) {
            return this.moveTarget(currentTime);
        }
        
        // Check if the target is still on fire
        // If it's not then select a new target
        do{
            Building building = (Building) this.world.getEntity(this.target);
            Logger.trace(String.format("%s, fierynessDefined=%s, onFire=%s", building, building.isFierynessDefined(), building.isOnFire()));
            if (building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() && building.getBrokenness() > 10){
            	System.out.println(">>>>>> Temperature = " + building.getTemperature());
                return this.world.getDistance(this.agentID, this.target) <= this.sightDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
            } else {
            	System.out.println(">>>>>> it's not on fire anymore. Target OK  = " + this.target.getValue());
                this.buildingSelector.remove(this.target);
            }
            EntityID newTarget = this.buildingSelector.getNewTarget(currentTime);
            if(this.target != newTarget){
            	this.target = newTarget;
            	if(this.target != null)
            		System.out.println(">>>>>> it's not on fire anymore. Target new = " + this.target.getValue());
            	else
            		System.out.println(">>>>>> there's no target anymore.");
            	break;
            }
        }while(this.target != null);
        
        /**teste antigo
         * //if (building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() && building.getBrokenness() > 10){
            if (building.isOnFire()){
            	Logger.debug(">>>>>> Building on fire, temperature = " + building.getTemperature());
                return this.world.getDistance(this.agentID, this.target) <= this.sightDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
            } 
            else if(building.isFierynessDefined() && this.world.getDistance(me, building) < this.sightDistance){
            	Logger.debug(">>>>>> it's not on fire anymore. Target OK  = " + this.target.getValue());
            	this.buildingSelector.remove(this.target);
            }
            else {
            	Logger.debug(">>>>>> building not in sight range, will move to it so that I can see and update");
            	return this.moveTarget(currentTime);
            	//return this.world.getDistance(this.agentID, this.target) <= this.maxDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
                
            }
         */
        
        // If none of the others action then walk randomly
        return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
    }
    
    // Move to a target if it exists otherwise walk randomly
    public Action moveTarget(int currentTime) {
        if(this.target != null) {
            List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
            if(path != null) {
                path.remove(this.target);
                return new ActionMove(this, path);
            }
            this.target = null;
        }
        return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
    }
    
    public String toString(){
    	return "Firefighter:" + this.getID();
    }

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		FireBrigade me = me();
	    // Are we currently filling with water?
	    if (me.isWaterDefined() && me.getWater() < maxWater && location() instanceof Refuge) {
	        Logger.info("Filling water at " + location()+ ". Now I have " + me().getWater());
	        return new ActionRest(this);
	    }
	    // Are we out of water?
	    if (me.isWaterDefined() && me.getWater() == 0) {
	        // Head for a refuge
	    	return this.moveRefuge(currentTime);
	    }
	    // Find all buildings that are on fire
	    Collection<EntityID> all = failSafeGetBurningBuildings();
	    // Can we extinguish any right now?
	    for (EntityID next : all) {
	        if (model.getDistance(getID(), next) <= sightDistance) {
	            Logger.info("Extinguishing " + next);
	            return new ActionExtinguish(this, next, maxPower);
	        }
	    }
	    // Plan a path to a fire
	    for (EntityID next : all) {
	        List<EntityID> path = failSafePlanPathToFire(next);
	        if (path != null) {
	            Logger.info("Moving to target");
	            return new ActionMove(this, path);
	        }
	    }
	    List<EntityID> path = null;
	    Logger.debug("Couldn't plan a path to a fire.");
	    path = this.routeSearcher.noTargetMove(currentTime, this.location.getID());
	    Logger.info("Moving randomly with: " + path);
	    return new ActionMove(this, path);
		
	}
	
	/**
	 * The getBurningBuildings of the sample agent
	 * @return
	 */
	private Collection<EntityID> failSafeGetBurningBuildings() {
	    Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
	    List<Building> result = new ArrayList<Building>();
	    for (StandardEntity next : e) {
	        if (next instanceof Building) {
	            Building b = (Building)next;
	            if (b.isOnFire()) {
	                result.add(b);
	            }
	        }
	    }
	    // Sort by distance
	    Collections.sort(result, new DistanceSorter(location(), model));
	    return objectsToIDs(result);
	}
	
	/**
	 * The planPathToFire of the sample agent
	 * @param target
	 * @return
	 */
	private List<EntityID> failSafePlanPathToFire(EntityID target) {
		return routeSearcher.getPath(getCurrentTime(), location.getID(), target);
		
	    /*Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
	    if (targets.isEmpty()) {
	        return null;
	    }
	    return failSafeSearch.breadthFirstSearch(me().getPosition(), objectsToIDs(targets));*/
	}
}
