package yrescue.tactics;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

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
import comlib.message.information.MessageRoad;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.action.ActionRefill;

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
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            if(entity instanceof Building) {
            	Building b = (Building) entity;
                this.buildingSelector.add(b);
                manager.addSendMessage(new MessageBuilding(b));		//report to other firefighters the building i've seen
            }
            else if(entity instanceof Civilian) {
                Civilian civilian = (Civilian)entity;
                if(civilian.getBuriedness() > 0) {
                    manager.addSendMessage(new MessageCivilian(civilian));
                }
            }
            else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                manager.addSendMessage(new MessageRoad((Road)this.world.getEntity(blockade.getPosition()), blockade, false));
            }
        }
    }

    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue() + " - FireBrigade agent");

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
}
