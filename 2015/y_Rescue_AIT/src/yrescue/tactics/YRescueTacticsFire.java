package yrescue.tactics;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.MDC;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggerFactory;


import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.util.DistanceSorter;
import yrescue.util.GeometricUtil;
import yrescue.action.ActionRefill;
import yrescue.heatmap.HeatMap;
import yrescue.heatmap.HeatNode;
import yrescue.message.event.MessageHydrantEvent;
import yrescue.message.information.MessageBlockedArea;
import yrescue.message.information.MessageHydrant;
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
	private List<StandardEntity> hydrants;
	public Map<EntityID, Integer> busyHydrantIDs;
	private int lastWater;

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
        manager.registerEvent(new MessageHydrantEvent(this));
    }
    
    public void sendAfterEvent(){
    	this.lastWater = me.getWater();
    }
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.buildingSelector = this.initBuildingSelector();
        
        busyHydrantIDs = new HashMap<>();
        lastWater = me.getWater();
        
        //Building the Lists of Refuge and Hydrant
        Collection<StandardEntity> refuge = this.world.getEntitiesOfType(StandardEntityURN.REFUGE);
        refugeIDs = new ArrayList<StandardEntity>();
        refugeIDs.addAll(refuge);
        Collection<StandardEntity> hydrant = this.world.getEntitiesOfType(StandardEntityURN.HYDRANT);
        hydrants = new ArrayList<StandardEntity>();
        hydrants.addAll(hydrant);
        
        this.hydrant_rate = this.config.getIntValue("fire.tank.refill_hydrant_rate");
        this.tank_maximum = this.config.getIntValue("fire.tank.maximum");
        
        MDC.put("agent", this);
        MDC.put("location", location());
        /*
        Log4JLogger lf = new Log4JLogger();
        
        org.apache.log4j.Logger d_logger = lf.getLogger();
        String logFileName = "doSomething"+me.getID()+".log";

        Properties prop = new Properties();
        prop.setProperty("doSomething"+me.getID(),"TRACE, WORKLOG");
        prop.setProperty("log4j.appender.WORKLOG","org.apache.log4j.FileAppender");
        prop.setProperty("log4j.appender.WORKLOG.File", logFileName);
        prop.setProperty("log4j.appender.WORKLOG.layout","org.apache.log4j.PatternLayout");
        prop.setProperty("log4j.appender.WORKLOG.layout.ConversionPattern","%d %c{1} - %m%n");
        //prop.setProperty("log4j.appender.WORKLOG.Threshold","INFO"); 

        PropertyConfigurator.configure(prop);
        */
        
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
        
        Logger.info(String.format(
			"HP: %d, B'ness: %d, Damage: %d, Direction: %d, Water: %d, lastWater: %d", 
			me.getHP(), me.getBuriedness(), me.getDamage(), me.getDirection(), me.getWater(), lastWater
		));
        
        Logger.info("Busy Hydrants: " + busyHydrantIDs);
        
        // Check if the agent is stuck
        if (this.tacticsAgent.stuck(currentTime)){
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID(), this.target));
        	Logger.trace("I'm blocked. Added a MessageBlockedArea");
    		return new ActionRest(this);	//does nothing...
    	}
        
        if(this.stuckExtinguishLoop(currentTime)) {
        	if (!updateWorldData.getChangedEntities().contains(target)){
        		Logger.warn("Warning: extinguishing same building for more than 3 timesteps without seeing it.");
        		Logger.warn("Will move to target");
        		return moveTarget(currentTime);
        	}
        	else{
        		Logger.info("Extinguishing same building for more than 3 timesteps, but I'm seeing it. No problem (I hope).");
        	}
        }
        
        // Check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
        	Logger.info("I'm buried at " + me.getPosition());
            manager.addSendMessage(new MessageFireBrigade(this.me, MessageFireBrigade.ACTION_REST, this.agentID));
            for(StandardEntity entity : this.world.getObjectsInRange(this.me, this.maxDistance)) {
                if(entity instanceof Building) {
                    Building building = (Building)entity;
                    this.target = building.getID();
                    //if (building.isOnFire() && (this.world.getDistance(this.agentID, this.target) <= this.maxDistance)) {
                    if(building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() && building.getBrokenness() > 10) {
                        return new ActionExtinguish(this, this.target, this.maxPower);
                    }
                }
            }
            return new ActionRest(this);
        }
        
        YRescueBuildingSelector bs = (YRescueBuildingSelector) buildingSelector;
        Logger.info(String.format("I know %d buildings on fire", bs.buildingList.size()));
        Logger.debug("They are: " + bs.buildingList);
        
        // Check if the agent got inside a building on fire
        EntityID locationID = this.me.getPosition();
        StandardEntity location = this.world.getEntity(locationID);
        if(location instanceof Building) {
            Building b = (Building)location;
            if(b.isOnFire()) {
            	Logger.warn("I'm in a burning building! Will try to get out");
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
        
        if(onWaterSource() && isWaterLessThan(1.0)) {
        	if(me.isWaterDefined() && me.getWater() < maxPower){
        		if(location instanceof Hydrant)
        			manager.addSendMessage(new MessageHydrant(this, currentTime, me.getWater(),this.hydrant_rate, this.maxWater, me.getPosition()));
        	}
        	Logger.info("Refilling..." + me.getWater());
            return new ActionRest(this);
        }
        
        // Update BusyHydrants
        for(Entry<EntityID,Integer> e : busyHydrantIDs.entrySet()){
        	if(currentTime >= e.getValue()){
        		busyHydrantIDs.remove(e.getKey());
        	}
        }
        
        Logger.info("New Busy Hydrants:" + busyHydrantIDs);
        
        // Refill
        if(me.isWaterDefined() && me.getWater() < maxPower) {
        	Logger.info("Insufficient water, going to refill.");
        	List<StandardEntity> freeHydrants = new ArrayList<StandardEntity>();
        	for(StandardEntity next : hydrants) {
        		if (next instanceof Hydrant) {
    	            Hydrant h = (Hydrant)next;
    	            
    	            if(! busyHydrantIDs.containsKey(h.getID())){
    	            	freeHydrants.add(h);
    	            }
    	            
    	            /*else if(currentTime >= busyHydrantIDs.get(h.getID())){
    	            	freeHydrants.add(h);
    	            }*/
    	        }	
        	}
        	return new ActionRefill(this, refugeIDs, freeHydrants);
        }
        
        // Select new target
        this.target = this.target == null ? this.buildingSelector.getNewTarget(currentTime) : this.buildingSelector.updateTarget(currentTime, this.target);
        
        // If there is no target then walk randomly
        if(this.target == null) {
        	EntityID explorationTgt = heatMap.getNodeToVisit();
        	Logger.info("No target... Heatmapping to: " + explorationTgt);
            return new ActionMove(this, this.routeSearcher.getPath(currentTime, me, explorationTgt));
        }
        
        // Check if the robot is not close to the target then get closer
        // Also goes out of water source to throw water
        if(this.world.getDistance(this.agentID, this.target) > this.sightDistance || this.onWaterSource()) {
        	Logger.debug("Going to target " + target);
            return this.moveTarget(currentTime);
        }
        
        // Check if the target is still on fire
        // If it's not then select a new target
        do{
            Building building = (Building) this.world.getEntity(this.target);
            Logger.trace(String.format("%s, fierynessDefined=%s, onFire=%s", building, building.isFierynessDefined(), building.isOnFire()));
            //if (building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4/* && building.isBrokennessDefined() && building.getBrokenness() > 10*/){
            if(building.isOnFire()){
            	Logger.trace(String.format("%s on fire, I'll tackle it", building));
                return this.world.getDistance(this.agentID, this.target) <= this.sightDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
            } else {
            	//System.out.println(">>>>>> it's not on fire anymore. Target OK  = " + this.target.getValue());
            	Logger.trace(String.format("%s not on fire anymore, will remove from list.", building));
                this.buildingSelector.remove(this.target);
            }
            //EntityID newTarget = this.buildingSelector.getNewTarget(currentTime);
            this.target = this.buildingSelector.getNewTarget(currentTime);
            
            /*if(this.target != newTarget){
            	this.target = newTarget;
            	//if(this.target != null)
            	//	System.out.println(">>>>>> it's not on fire anymore. Target new = " + this.target.getValue());
            	//else
            	//	System.out.println(">>>>>> there's no target anymore.");
            	break;
            }*/
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
        EntityID explorationTgt = heatMap.getNodeToVisit();
    	Logger.info("End of think reached. Heatmapping to: " + explorationTgt);
        return new ActionMove(this, this.routeSearcher.getPath(currentTime, me, explorationTgt));
    }
    
    // Move to a target if it exists otherwise walk randomly
    public Action moveTarget(int currentTime) {
        if(this.target != null) {
            List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, this.target);
            Logger.debug("Target " + target + "; path: " + path);
            if(path != null) {
                path.remove(this.target);
                return new ActionMove(this, path);
            }
            this.target = null;
        }
        EntityID explorationTgt = heatMap.getNodeToVisit();
    	Logger.info("No target... Heatmapping to: " + explorationTgt);
        return new ActionMove(this, this.routeSearcher.getPath(currentTime, me, explorationTgt));
    }
    
    public String toString(){
    	return "Firefighter:" + this.getID();
    }
    
    private boolean stuckExtinguishLoop(int currentTime){
    	if (tacticsAgent.commandHistory.size() < 4){
    		Logger.info("Insufficient commands in history");
    		return false;
    	}
    	Action lastCmd = null;
    	for(int backtime = 1; backtime <= 4; backtime++){
    		if (lastCmd == null){
    			lastCmd = tacticsAgent.commandHistory.get(currentTime - backtime);
    			if(lastCmd == null){
    				Logger.warn("Failed to query lastCmd at history in time " + (currentTime - backtime));
    				return false;
        		}
    		}
    		
    		Logger.trace(String.format("backtime=%d, lastCmd=%s, currCmd=%s", backtime, lastCmd, tacticsAgent.commandHistory.get(currentTime - backtime)));
    		
    		if (!lastCmd.equals(tacticsAgent.commandHistory.get(currentTime - backtime))){
    			return false;
    		}
    		lastCmd = tacticsAgent.commandHistory.get(currentTime - backtime);
    	}
    	
    	return true;
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
	    if (me.isWaterDefined() && me.getWater() < maxPower) {
	        // Head for a refuge
	    	Logger.debug("FailSafe going to refuge...");
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

	@Override
	public HeatMap initializeHeatMap() {
    	HeatMap heatMap = new HeatMap(this.agentID, this.world);
        for (Entity next : this.getWorld()) {
            if (next instanceof Area) {
            	// Ignore very small areas to explore
            	//if(GeometricUtil.getAreaOfEntity(next.getID(), this.world) < EXPLORE_AREA_SIZE_TRESH) continue;
            	
            	// Ignore non building areas
            	if(!(next instanceof Building)) continue;
            	
            	heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.LOW, 0);
            }
        }
        
        return heatMap;
	}
}
