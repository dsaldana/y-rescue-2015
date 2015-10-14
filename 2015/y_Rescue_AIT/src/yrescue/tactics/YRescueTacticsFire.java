package yrescue.tactics;

import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.action.ActionRefill;
import yrescue.message.information.MessageBlockedArea;
import yrescue.util.YRescueBuildingSelector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.MDC;

//import yrescue.util.YRescueRouteSearcher;
import adk.sample.basic.event.BasicBuildingEvent;
import adk.sample.basic.tactics.BasicTacticsFire;
import adk.sample.basic.util.BasicBuildingSelector;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.team.action.Action;
import adk.team.action.ActionExtinguish;
import adk.team.action.ActionMove;
import adk.team.action.ActionRest;
import adk.team.util.BuildingSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessageRoad;

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
        
        MDC.put("agent", this);
        MDC.put("location", location());
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
        MDC.put("location", location());
        
        System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue() + " - FireBrigade agent");
        
        // Check if the agent is stuck
        if (this.tacticsAgent.stuck (currentTime)){
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID()));
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
        
        // Check if the agent got inside the building in fire
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
        BasicBuildingSelector bs = (BasicBuildingSelector) buildingSelector;
        
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
        if(this.world.getDistance(this.agentID, this.target) > this.maxDistance) {
            return this.moveTarget(currentTime);
        }
        
        // Check if the target is still on fire
        // If it's not then select a new target
        do{
            Building building = (Building) this.world.getEntity(this.target);
            if (building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() && building.getBrokenness() > 10){
            	System.out.println(">>>>>> Temperature = " + building.getTemperature());
                return this.world.getDistance(this.agentID, this.target) <= this.maxDistance ? new ActionExtinguish(this, this.target, this.maxPower) : this.moveTarget(currentTime);
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
