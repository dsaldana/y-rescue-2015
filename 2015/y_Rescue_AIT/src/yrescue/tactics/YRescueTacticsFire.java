package yrescue.tactics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.MDC;

import java.util.Collection;
import java.util.HashSet;

import adk.sample.basic.event.BasicBuildingEvent;
import adk.sample.basic.util.BasicBuildingSelector;
import adk.sample.basic.tactics.BasicTacticsFire;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionExtinguish;
import adk.team.action.ActionMove;
import adk.team.action.ActionRescue;
import adk.team.action.ActionRest;
import adk.team.util.BuildingSelector;
import adk.team.util.RouteSearcher;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessageRoad;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.action.ActionRefill;
import yrescue.message.information.MessageBlockedArea;
import yrescue.problem.blockade.BlockadeUtil;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;

public class YRescueTacticsFire extends BasicTacticsFire {

    @Override
    public String getTacticsName() {
        return "Y-Rescue Firefighter";
    }

    @Override
    public BuildingSelector initBuildingSelector() {
        return new BasicBuildingSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
        return new BasicRouteSearcher(this);
    }


    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicBuildingEvent(this, this));
    }
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = this.initRouteSearcher();
        this.buildingSelector = this.initBuildingSelector();
        MDC.put("agent", this);
        MDC.put("location", location());
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
    	
    	Set<EntityID> reportedRoads = new HashSet<>(); 
    	
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            if(entity instanceof Building) {
            	Building b = (Building) entity;
                this.getBuildingSelector().add(b);
                if (b.isOnFire()) {
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
                
                if (! reportedRoads.contains(blockade.getPosition())) {
	                manager.addSendMessage(new MessageRoad((Road) this.world.getEntity(blockade.getPosition()), blockade, false));
	                Logger.trace("Added outgoin' msg about blockade:" + blockade + " in road " + this.world.getEntity(blockade.getPosition()));
	                
	                reportedRoads.add(blockade.getPosition());
                }
            }*/
        }
    }
    
    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    }

    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        MDC.put("location", location());
        
        System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue() + " - FireBrigade agent");
        
        if (this.tacticsAgent.stuck (currentTime)){
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID()));
        	Logger.trace("I'm blocked. Added a MessageBlockedArea");
    		return new ActionRest(this);	//does nothing...
    	}
        
        //check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
            manager.addSendMessage(new MessageFireBrigade(this.me, MessageFireBrigade.ACTION_REST, this.agentID));
            for(StandardEntity entity : this.world.getObjectsInRange(this.me, this.maxDistance)) {
                if(entity instanceof Building) {
                    Building building = (Building)entity;
                    this.target = building.getID();
                    //if (building.isOnFire() && (this.world.getDistance(this.agentID, this.target) <= this.maxDistance)) {
                    if(building.isOnFire()) {
                        return new ActionExtinguish(this, this.target, this.maxPower);
                    }
                }
            }
            return new ActionRest(this);
        }
        
        

        // Building the Lists of Refuge and Hydrant
        Collection<StandardEntity> refuge = this.world.getEntitiesOfType(StandardEntityURN.REFUGE);
        List<StandardEntity> refugeIDs = new ArrayList<StandardEntity>();
        refugeIDs.addAll(refuge);
        Collection<StandardEntity> hydrant = this.world.getEntitiesOfType(StandardEntityURN.HYDRANT);
        List<StandardEntity> hydrantIDs = new ArrayList<StandardEntity>();
        hydrant.addAll(hydrant);
        
        // Max Distance
        this.maxDistance = 15000;
        BasicBuildingSelector bs = (BasicBuildingSelector) buildingSelector;
        
        Logger.info(String.format("I know %d buildings on fire", bs.buildingList.size()));
        
        // Out of Water
        // But it's already refilling then rest
        if((this.location instanceof Refuge || this.location instanceof Hydrant) && (this.me.getWater() < this.maxWater)) {
            this.target = null;
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
        // Check if the robot is not close to the target
        if(this.world.getDistance(this.agentID, this.target) > this.maxDistance) {
            return this.moveTarget(currentTime);
        }
        // Check if the target is still on fire
        // If it's not then select a new target
        do{
            Building building = (Building) this.world.getEntity(this.target);
            if (building.isOnFire()) {
                return this.world.getDistance(this.agentID, this.target) <= this.maxDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
            } else {
                this.buildingSelector.remove(this.target);
            }
            this.target = this.buildingSelector.getNewTarget(currentTime);
        }while(this.target != null);
        
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
}
