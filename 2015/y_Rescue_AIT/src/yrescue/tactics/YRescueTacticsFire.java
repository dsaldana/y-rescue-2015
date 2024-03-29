package yrescue.tactics;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.MDC;

import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.FireStation;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityConstants;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.messages.AKMove;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.action.ActionRefill;
import yrescue.heatmap.HeatMap;
import yrescue.heatmap.HeatNode;
import yrescue.kMeans.KMeans;
import yrescue.message.event.MessageHydrantEvent;
import yrescue.message.information.MessageBlockedArea;
import yrescue.problem.blockade.BlockadeUtil;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.statemachine.StatusStates;
import yrescue.util.DistanceSorter;
import yrescue.util.PathUtil;
import yrescue.util.RouteCacheKey;
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
import comlib.message.information.MessageFireBrigade;

public class YRescueTacticsFire extends BasicTacticsFire {

    private List<StandardEntity> refugeIDs;
	private List<StandardEntity> hydrants;
	public Map<EntityID, Integer> busyHydrantIDs;
	
	public Map<EntityID, Integer> blockedBuildingIDs;
	
	private int lastWater;
	
	private int flagOnce;
	private int flagStuck;
	public EntityID targetHydrant;
	
	private List<EntityID> lastPath;
	
	private StateMachine actionStateMachine;
	private StateMachine statusStateMachine;
	
	protected List<EntityID> clusterToVisit;
	protected EntityID clusterCenter;
	
	public int stuckCounter;
	
	protected Map<RouteCacheKey, List<EntityID>> routeBreadthFirstCache;
	
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
    	return new BasicRouteSearcher(this, this.routeBreadthFirstCache);
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
    	long prepStart = System.currentTimeMillis();
    	
    	routeBreadthFirstCache = PathUtil.getRouteCache();
    	this.routeSearcher = new BasicRouteSearcher(this, routeBreadthFirstCache);
    	
        this.buildingSelector = this.initBuildingSelector();
        lastPath = new ArrayList<>();
        busyHydrantIDs = new HashMap<>();
        blockedBuildingIDs = new HashMap<>();
        lastWater = me.getWater();
        
        stuckCounter = 0;
        
        //Building the Lists of Refuge and Hydrant
        Collection<StandardEntity> refuge = this.world.getEntitiesOfType(StandardEntityURN.REFUGE);
        refugeIDs = new ArrayList<StandardEntity>();
        refugeIDs.addAll(refuge);
        Collection<StandardEntity> hydrant = this.world.getEntitiesOfType(StandardEntityURN.HYDRANT);
        hydrants = new ArrayList<StandardEntity>();
        hydrants.addAll(hydrant);
        
        this.hydrant_rate = this.config.getIntValue("fire.tank.refill_hydrant_rate");
        this.tank_maximum = this.config.getIntValue("fire.tank.maximum");
    	
		clusterToVisit = new LinkedList<EntityID>();
    	List<StandardEntity> fireBrigadeList = new ArrayList<StandardEntity>(this.getWorld().getEntitiesOfType(StandardEntityURN.FIRE_BRIGADE));
    	KMeans kmeans = new KMeans(fireBrigadeList.size());
    	Map<EntityID, EntityID> kmeansResult = kmeans.calculatePartitions(this.getWorld());
    	
    	List<EntityID> partitions = kmeans.getPartitions();
    	
    	fireBrigadeList.sort(new Comparator<StandardEntity>() {
			@Override
			public int compare(StandardEntity o1, StandardEntity o2) {
				return Integer.compare(o1.getID().getValue(), o2.getID().getValue());
			}
		});
    	
    	partitions.sort(new Comparator<EntityID>() {
			@Override
			public int compare(EntityID o1, EntityID o2) {
				return Integer.compare(o1.getValue(), o2.getValue());
			}
		});
    	
    	if(fireBrigadeList.size() == partitions.size()){
    		int pos = -1;
    		for(int i = 0; i < fireBrigadeList.size(); i++){
    			if(me.getID().getValue() == fireBrigadeList.get(i).getID().getValue()){
    				pos = i;
    				break;
    			}
    		}
    		
    		if(pos != -1){
    			clusterCenter = partitions.get(pos);
        		final Set<Map.Entry<EntityID, EntityID>> entries = kmeansResult.entrySet();

        		for (Map.Entry<EntityID, EntityID> entry : entries) {
        		    EntityID key = entry.getKey();
        		    EntityID partition= entry.getValue();

        		    if(partition.getValue() == clusterCenter.getValue()){
        		    	clusterToVisit.add(key);
        		    }
        		}	
    		}
    	}

    	Logger.debug("Cluster to visit :" + clusterToVisit);
    	if(clusterToVisit.size() > 0){
    		Building b = (Building) world.getEntity(clusterCenter);
    		this.target = b.getID();
    	}
    	
    	this.flagOnce = 0;
    	this.flagStuck = 0;
    	this.targetHydrant = null;
    	
    	this.actionStateMachine = new StateMachine(ActionStates.IDLE);
        this.statusStateMachine = new StateMachine(StatusStates.EXPLORING);
		
        MDC.put("agent", this);
        MDC.put("location", location());
        
        long secsToProcess = (System.currentTimeMillis() - prepStart);
        Logger.info(String.format(
    		">>>> FireFighter ON. maxDistance=%d, sightDistance=%d, prepTime=%d ms ----", 
    		this.maxDistance, this.sightDistance, secsToProcess
    	));
        
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
            	
            	Logger.trace(String.format(
        			"I'm seeing a %s. onFire=%s, fieryness=%s, fierynessEnum=%s", b, b.isOnFire(), b.getFieryness(), b.getFierynessEnum()
        		));
            	
                
                if (b.isOnFire()) {
                	this.getBuildingSelector().add(b);
                	manager.addSendMessage(new MessageBuilding(b));		//report to other firefighters the building i've seen
                    Logger.trace("Added outgoin' msg about burning building: " + b);
                }
                
                if(b.getFierynessEnum().equals(StandardEntityConstants.Fieryness.BURNT_OUT)){
                	Logger.trace("Removing completely burnt Building from heatMap" + b);
                	heatMap.removeEntityID(b.getID());
                }
                else {
                	heatMap.updateNode(b.getID(), currentTime);
                }
                
            }
            else if(entity instanceof Civilian) {
                this.reportCivilian((Civilian) entity, manager, currentTime);
            }
        }
    }
    
    public void ignoreTimeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
    	Logger.debug("\nRadio channel: " + manager.getRadioConfig().getChannel());
    	
    	// Check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
        	Logger.info("I'm buried at " + me.getPosition());
            this.buriednessAction(manager);
        }
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
        
        //heatMap.writeMapToFile();
       
        // Check for buriedness and tries to extinguish fire in a close building
        if(this.me.getBuriedness() > 0) {
        	Logger.info("I'm buried at " + me.getPosition());
            return this.buriednessAction(manager);
        }
        
        // Update buildingOnFire List
        Iterator<Entry<EntityID, Integer>> iter = blockedBuildingIDs.entrySet().iterator();
        while(iter.hasNext()){
        	Entry<EntityID, Integer> e = iter.next();
        	if(currentTime >= e.getValue()){
        		if(blockedBuildingIDs.containsKey(e.getKey())) 
        			blockedBuildingIDs.remove(e.getKey());
        		buildingSelector.add(e.getKey());
        	}
        }
        
        // Check if the agent is stuck
        if (this.fireFighterStuck(currentTime)){
        	
        	blockedBuildingIDs.put(target, currentTime+30);
        	buildingSelector.remove(target);
        	
        	if(statusStateMachine.getCurrentState().equals(StatusStates.STUCK) || 
        			statusStateMachine.getCurrentState().equals(StatusStates.STUCK_NAVIGATION)){
        		
        		stuckCounter++;
        		Logger.debug("incrementing stuck counter");
        	}
        	else{
        		Logger.debug("setting stuck counter to 1");
        		stuckCounter = 1;
        	}
        	Logger.info("I'm stuck for " + stuckCounter + " timesteps =/");
        	
        	statusStateMachine.setState(StatusStates.STUCK);
        	Logger.info("Adding a MessageBlockedArea");
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID(), this.target));
        	
        	if(stuckCounter > 5){
        		Logger.info("I'll change my target // switch task. Stuck for too long");
        		stuckCounter = 0;
            	return this.switchTask();
        	}
        	
        	Point2D navTgt = BlockadeUtil.calculateNavigationMove(this);
        	if (navTgt != null){
        		
        		List<EntityID> fooPath = new ArrayList<>();
        		fooPath.add(location.getID());
        		
        		statusStateMachine.setState(StatusStates.STUCK_NAVIGATION);
        		Logger.info(String.format("Will attempt stuck-move to %s of %s", navTgt, fooPath));
        		return new ActionMove(this, fooPath, (int)navTgt.getX(), (int)navTgt.getY());
        	}
        	else{
        		Logger.info("All attempts to shake while stuck failed... going failsafe");
        		return this.failsafeThink(currentTime, updateWorldData, manager);
        	}
        	
        	/*
        	this.flagOnce = 1;
        	this.target = this.buildingSelector.getNewTarget(currentTime);
        	this.flagStuck = 1;
        	try{
	        	Action a = this.tacticsAgent.commandHistory.get(currentTime - 1);
	        	if(a instanceof ActionMove){
	        		List<EntityID> previousPath = ((ActionMove) a).getPath();
	        		//System.out.println("previous action move: " + ((ActionMove)a));
	        		//System.out.println("previousPath " + previousPath);
		        	//System.out.println(String.format("Stuck! location=%s, previousPath.get(0)=%s", location, previousPath.get(0)));
		        	//isStuck = true;
	        		if(location instanceof Area){
			        	Point2D destinationStuck = BlockadeUtil.calculateStuckMove((Area)location, (Area)this.world.getEntity(lastPath.get(0)), this,currentTime);
			        	List<EntityID> newPath = new ArrayList<>();
			        	newPath.add(location.getID());
			        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID(), this.target));
			        	Logger.trace("I'm blocked. Added a MessageBlockedArea");
			        	return new ActionMove(this,newPath,(int)destinationStuck.getX(),(int)destinationStuck.getY());
	        		}
	        	}
        	}
        	catch(Exception e){
        		Logger.error("ERROR on attempting stuck move.");
        		target = buildingSelector.getNewTarget(currentTime);
        	}*/
    	}
        else {
        	statusStateMachine.setState(StatusStates.ACTING);
        	stuckCounter = 0;
        }
        
        if(this.me.getDamage() >= 50) { //|| this.someoneOnBoard()
        	statusStateMachine.setState(StatusStates.HURT);
        	return moveRefuge(currentTime);
        }
        
        if(this.stuckExtinguishLoop(currentTime)) {
        	if (!updateWorldData.getChangedEntities().contains(target)){
        		Logger.warn("Warning: extinguishing same building for more than 2 timesteps without seeing it.");
        		Logger.warn("Will move to target");
        		actionStateMachine.setState(ActionStates.MOVING_TO_TARGET);
        		statusStateMachine.setState(StatusStates.ACTING);
        		return moveTarget(currentTime);
        	}
        	else{
        		Logger.info("Extinguishing same building for more than 2 timesteps, but I'm seeing it. No problem (I hope).");
        	}
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
                Logger.warn("This should not happen! I'm in a burning building and can't get out!");
            }
        }
                
        // Check if the last step building is not on fire anymore and then send a message to the others update it
        Action cmd = tacticsAgent.commandHistory.get(currentTime-1); 
        Action cmd2 = tacticsAgent.commandHistory.get(currentTime-2);
        if (cmd instanceof ActionExtinguish){
        	ActionExtinguish ext = (ActionExtinguish)cmd;
        	EntityID buildLastTargetID = ext.getTarget();
        	StandardEntity buildEntity = this.getWorld().getEntity(buildLastTargetID);
        	if(buildEntity instanceof Building){
        		Building buildLastTarget = (Building)buildEntity;
        		if(!buildLastTarget.isOnFire()){
        			manager.addSendMessage(new MessageBuilding(buildLastTarget));
            	}
        	}
        }else if (cmd2 instanceof ActionExtinguish){
        	ActionExtinguish ext = (ActionExtinguish)cmd2;
        	EntityID buildLastTargetID = ext.getTarget();
        	StandardEntity buildEntity = this.getWorld().getEntity(buildLastTargetID);
        	if(buildEntity instanceof Building){
        		Building buildLastTarget = (Building)buildEntity;
        		if(!buildLastTarget.isOnFire()){
        			manager.addSendMessage(new MessageBuilding(buildLastTarget));
            	}
        	}
        }
        
        // Update BusyHydrants
        iter = busyHydrantIDs.entrySet().iterator();
        while(iter.hasNext()){
        	Entry<EntityID, Integer> e = iter.next();
        	if(currentTime >= e.getValue()){
        		if(busyHydrantIDs.containsKey(e.getKey()))
        			busyHydrantIDs.remove(e.getKey());
        	}
        }
        
        /*for(Entry<EntityID,Integer> e : busyHydrantIDs.entrySet()){
        	if(currentTime >= e.getValue()){
        		busyHydrantIDs.remove(e.getKey());
        	}
        }*/
        /*if(flagStuck == 1){
    		busyHydrantIDs.put(this.targetHydrant, currentTime+20);
        	flagStuck = 0;
        }*/
        
        Logger.debug("New Busy Hydrants:" + busyHydrantIDs);
                
        // Check if the agent should look for water even though the tank is not empty
        if(this.statusStateMachine.currentState() == StatusStates.ACTING && this.actionStateMachine.currentState() == ActionStates.FireFighter.REFILLING_WATER_ANYWAY){
        	Logger.info("I have water, but I'm going to refill anyway.");
        	if(me().getWater() == maxWater){
        		actionStateMachine.setState(ActionStates.IDLE);
        		statusStateMachine.setState(StatusStates.EXPLORING);
        	}else if(onWaterSource()){
        		this.target = this.buildingSelector.getNewTarget(currentTime);
        		return new ActionRest(this);
        	}
        }
        
        // Check if there is another agent refilling water at the same hydrant
        if(this.statusStateMachine.currentState() == StatusStates.ACTING && this.actionStateMachine.currentState() == ActionStates.FireFighter.REFILLING_WATER){
        	if(me().getWater() == maxWater){
        		actionStateMachine.setState(ActionStates.IDLE);
        		statusStateMachine.setState(StatusStates.EXPLORING);
        	}else if(this.location instanceof Refuge){
        		this.target = this.buildingSelector.getNewTarget(currentTime);
        		return new ActionRest(this);
        	}else{
        		Collection<StandardEntity> objects = world.getObjectsInRange(getOwnerID(), sightDistance);
            	boolean flagAgentsCloser = false;
            	for(StandardEntity objH : objects){
            		if(objH instanceof Hydrant){
            			for(StandardEntity objA : objects){
                    		if(objA instanceof FireBrigade){
                        		if(objA.getID().getValue() < this.me().getID().getValue()){
                        			flagAgentsCloser = true;
                        			busyHydrantIDs.put(objH.getID(), currentTime + (this.maxWater-me.getWater() / this.hydrant_rate));
                        			break;
                        		}
                        	}
                    	}
            			if(flagAgentsCloser){
            				List<StandardEntity> freeHydrants = new ArrayList<StandardEntity>();
            	        	for(StandardEntity next : hydrants) {
            	        		if (next instanceof Hydrant) {
            	    	            Hydrant h = (Hydrant)next;
            	    	            if(! busyHydrantIDs.containsKey(h.getID())){
            	    	            	freeHydrants.add(h);
            	    	            }
            	    	        }	
            	        	}
            	        	return new ActionRefill(this, refugeIDs, freeHydrants);
            			}else if(this.location instanceof Hydrant){
            				this.target = this.buildingSelector.getNewTarget(currentTime);
                    		return new ActionRest(this);
                    	}
            			break;
                	}
            	}
        	}
        }
        
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
    	        }	
        	}
    		actionStateMachine.setState(ActionStates.FireFighter.REFILLING_WATER);
    		statusStateMachine.setState(StatusStates.ACTING);
        	return new ActionRefill(this, refugeIDs, freeHydrants);
        }
        
        // Going to cluster
        bs = (YRescueBuildingSelector) buildingSelector;
        if(bs.buildingList.size() == 0){
        	if(flagOnce == 0){
	    		this.target = this.clusterCenter;
	    		// Check if the agent is already on cluster
	        	Collection<StandardEntity> objects = world.getObjectsInRange(getOwnerID(), sightDistance);
	        	int flagDestination = 0;
	        	for(StandardEntity objB : objects){
	        		if(objB instanceof Building){
	        			Building b = (Building)objB;
	        			if(b.getID().getValue() == target.getValue()){
	        	    		flagDestination = 1;
	        	    		flagOnce = 1;
	        			}
	        		}
	        	}
	        	if(flagDestination == 0){
	        		return this.moveTarget(currentTime);
	        	}
	        }
        }
        
        this.target = this.target == null ? this.buildingSelector.getNewTarget(currentTime) : this.buildingSelector.updateTarget(currentTime, this.target);
        
        // If there is no target then walk randomly
        if(this.target == null) {
        	List<EntityID> path;
        	// Refuge is close and the agent has less water than 0.6 of the tank and more water than the maxPower 
        	if(me().getWater() > maxPower && me().getWater() < (maxWater*0.7)){
        		Collection<StandardEntity> objects = world.getObjectsInRange(getOwnerID(), sightDistance);
            	for(StandardEntity obj : objects){
            		if(obj instanceof Refuge){ // Only Refuge
            			path = this.routeSearcher.getPath(currentTime, me, obj.getID());
                		actionStateMachine.setState(ActionStates.FireFighter.REFILLING_WATER_ANYWAY);
                		statusStateMachine.setState(StatusStates.ACTING);
                    	// Only Refuge 
                		
                    	return new ActionRefill(this, refugeIDs, null);
            		}
            	}	
        	}
        	
        	EntityID explorationTgt = heatMap.getNodeToVisit();
        	Logger.info("No target... Heatmapping to: " + explorationTgt);
        	path = this.routeSearcher.getPath(currentTime, me, explorationTgt);
        	
        	if(path.size() > 1) {
        		if(world.getEntity(path.get(path.size() - 1 )) instanceof Building) {
        			Logger.debug("Last path item is a building, I'll go to its door");
        			path.remove(path.size() - 1);
        		}
        	}
        	else {
        		Logger.debug("Path is too short... but I'll follow it anyway");
        	}       	
        	lastPath = path;
            return new ActionMove(this, path);
        }
        
        // Check if the robot is not close to the target then get closer
        // Also goes out of water source to throw water
        if(this.world.getDistance(this.agentID, this.target) > this.sightDistance || this.onWaterSource()) {
        	Logger.debug("Going to target " + target);
        	Building b = (Building) this.getWorld().getEntity(target);
    		actionStateMachine.setState(ActionStates.MOVING_TO_TARGET);
    		statusStateMachine.setState(StatusStates.ACTING);
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
            	//return new ActionExtinguish(this, this.target, this.maxPower);
            	if(this.world.getDistance(this.agentID, this.target) <= this.sightDistance || updateWorldData.getChangedEntities().contains(this.target)){
            		return  new ActionExtinguish(this, this.target, this.maxPower);
            	}
            	else{
            		return this.moveTarget(currentTime);
            	}
            } else {
            	Logger.trace(String.format("%s not on fire anymore, will remove from list.", building));
                this.buildingSelector.remove(this.target);
            }
            EntityID newTarget = this.buildingSelector.getNewTarget(currentTime);
            
            if(this.target != newTarget){
            	this.target = newTarget;
            	break;
            }
        }while(this.target != null);
        
        
        // If none of the others action then walk randomly
        EntityID explorationTgt = heatMap.getNodeToVisit();
    	Logger.info("End of think reached. Heatmapping to: " + explorationTgt);
    	List<EntityID> path = this.routeSearcher.getPath(currentTime, me, explorationTgt);
    	
    	if(path.size() > 1) {
    		if(world.getEntity(path.get(path.size() - 1 )) instanceof Building) {
    			Logger.debug("Last path item is a building, I'll go to its door");
    			path.remove(path.size() - 1);
    		}
    	}
    	else {
    		Logger.debug("Path is too short... but I'll follow it anyway");
    	}
    	lastPath = path;
        return new ActionMove(this, path);
    }

	private Action switchTask() {
		if(me.isWaterDefined() && me.getWater() < maxPower) {
        	Logger.info("SWITCH: setting targetHydrant as busy");
        	if(targetHydrant != null){
        		busyHydrantIDs.put(this.targetHydrant, getCurrentTime() + 20);
        	}
        	flagStuck = 0;
        	return new ActionRefill(this, refugeIDs, hydrants);
        }
		else {
			Logger.info("SWITCH: new target");
			this.target = buildingSelector.getNewTarget(getCurrentTime());
			return moveTarget(getCurrentTime());
		}
	}

	private Action buriednessAction(MessageManager manager) {
		manager.addSendMessage(new MessageFireBrigade(this.me, MessageFireBrigade.ACTION_REST, this.agentID));
		for(StandardEntity entity : this.world.getObjectsInRange(this.me, this.maxDistance)) {
		    if(entity instanceof Building) {
		        Building building = (Building)entity;
		        this.target = building.getID();
		        //if (building.isOnFire() && (this.world.getDistance(this.agentID, this.target) <= this.maxDistance)) {
		        if(building.isOnFire() && building.isTemperatureDefined() && building.getTemperature() > 40 && building.isFierynessDefined() && building.getFieryness() < 4 && building.isBrokennessDefined() /*&& building.getBrokenness() > 10*/) {
		    		actionStateMachine.setState(ActionStates.FireFighter.EXTINGUISHING);
		    		statusStateMachine.setState(StatusStates.ACTING);
		        	return new ActionExtinguish(this, this.target, this.maxPower);
		        }
		    }
		}
		return new ActionRest(this);
	}
	
	public boolean fireFighterStuck(int time){
		if (tacticsAgent.stuck(time)) return true;
		
		int tolerance = 500;	//if agent moved less than this, will be considered as stuck
		double distance;
		
		//agents cannot issue move commands in beginning
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
			return false;
		}
	
		if (tacticsAgent.commandHistory.size() == 0) {
			return false;
		}
	
		Pair<Integer, Integer> currentPos = me().getLocation(model);
		
		distance = Math.hypot(currentPos.first() - tacticsAgent.lastPosition.first(), currentPos.second() - tacticsAgent.lastPosition.second());
		
		Logger.debug("Stuckness test - distance from last position:" + distance);
	
		if (tacticsAgent.commandHistory.containsKey(time -1)) {
			Action cmd = tacticsAgent.commandHistory.get(time -1); 
		
			Logger.debug(String.format(
				"Firefighter stuckness test: AKcmd: %s, last position (%d, %d), curr position (%d, %d)", 
				cmd.getCommand(agentID, time), tacticsAgent.lastPosition.first(), tacticsAgent.lastPosition.second(), currentPos.first(), currentPos.second()
			));
		
			//if i'm not at refuge and I traversed small distance, I'm stuck
			if ( (cmd.getCommand(agentID, time) instanceof AKMove) && (distance  < tolerance)) {
				//ActionMove action = (ActionMove)cmd;
				
				Logger.info("Dammit, I'm stuck!");
				return true;
			}
		}
		Logger.info("Not stuck!");
		return false;
		
		
		
		
	}
    
    // Move to a target if it exists otherwise walk randomly
    public Action moveTarget(int currentTime) {
    	List<EntityID> path;
    	
    	if(this.target != null) {
            path = this.safePathToBuilding(target);
            //this.routeSearcher.getPath(currentTime, this.me, this.target);
            Logger.debug("Target " + target + "; path: " + path);
            
            if(path == null){
            	Logger.debug("Couldn't plan a path to target. Will assign null to target. " + target );
            	this.target = null;
            }
            else {
            	if(path.size() > 1) {
            		if(world.getEntity(path.get(path.size() - 1 )) instanceof Building) {
            			Logger.debug("Last path item is a building, I'll go to its door");
            			path.remove(path.size() - 1);
            		}
            	}
            	else {
            		Logger.debug("Path is too short... but I'll follow it anyway");
            	}            	
                return new ActionMove(this, path);
            }
        }
        
        EntityID explorationTgt = heatMap.getNodeToVisit();
    	Logger.info("Target is null... Heatmapping to: " + explorationTgt);
    	path = this.safePathToBuilding(explorationTgt);
    	if(path.size() > 1) {
    		if(world.getEntity(path.get(path.size() - 1 )) instanceof Building) {
    			Logger.debug("Last path item is a building, I'll go to its door");
    			path.remove(path.size() - 1);
    		}
    	}
    	else {
    		Logger.debug("Path is too short... but I'll follow it anyway");
    	}
        return new ActionMove(this, path);
    }
    
    public List<EntityID> safePathToBuilding(EntityID target){
    	List<EntityID> path = this.routeSearcher.getPath(this.getCurrentTime(), this.me, this.target);
    	
    	if(path == null || !(world.getEntity(target) instanceof Building)){
    		Logger.warn("No path to target " + target + " or path is null. Returning path 'as is'");
    		return path;
    	}
    	
    	if(path.size() > 1) {
    		if(world.getEntity(path.get(path.size() - 1 )) instanceof Building) {
    			Logger.debug("Last path item is a building, I'll go to its door");
    			path.remove(path.size() - 1);
    		}
    	}
    	else {
    		Logger.debug("Path is too short... but I'll follow it anyway");
    	}
    	
    	return path;
    }
    
    public String toString(){
    	return "Firefighter:" + this.getID();
    }
    
    private boolean stuckExtinguishLoop(int currentTime){
    	if (tacticsAgent.commandHistory.size() < 2){
    		Logger.debug("Insufficient commands in history");
    		return false;
    	}
    	Action lastCmd = null;
    	for(int backtime = 1; backtime <= 2; backtime++){
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
	        Logger.info("FAILSAFE: Filling water at " + location()+ ". Now I have " + me().getWater());
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
	            Logger.info("FAILSAFE: Extinguishing " + next);
        		return new ActionExtinguish(this, next, maxPower);
	        }
	    }
	    // Plan a path to a fire
	    for (EntityID next : all) {
	        List<EntityID> path = failSafePlanPathToFire(next);
	        if (path != null) {
	            Logger.info("FAILSAFE: Moving to target");
	            lastPath = path;
	            return new ActionMove(this, path);
	        }
	    }
	    List<EntityID> path = null;
	    Logger.debug("FAILSAFE: Couldn't plan a path to a fire.");
	    path = this.routeSearcher.noTargetMove(currentTime, this.location.getID());
	    Logger.info("FAILSAFE: Moving randomly with: " + path);
	    lastPath = path;
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
            	if(!(next instanceof Building)) continue;	//ignore non building areas
            	
            	HeatNode.PriorityLevel priority = HeatNode.PriorityLevel.LOW;
            	
            	//prioritizes FireStations
            	if(next instanceof FireStation) {
            		priority = HeatNode.PriorityLevel.HIGH;
            	}
            	
            	heatMap.addEntityID(next.getID(), priority, 0);
            }
        }
        
        return heatMap;
	}
}
