package agent.platoon;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.MDC;

import commands.AgentCommands;
import problem.BlockedArea;
import problem.BurningBuilding;
import problem.Recruitment;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Hydrant;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import statemachine.StateMachine;
import statemachine.ActionStates;
import util.DistanceSorter;
import yworld.YBuilding;
import yworld.YFireSimulator;
/**
 *  RoboFire agent. Implements a simple scheme to fight fires.
 */
public class Firefighter extends AbstractPlatoon<FireBrigade> {
    private static final String MAX_WATER_KEY = "fire.tank.maximum";
    private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";
    private static final String REFUGE_REFILL_RATE = "fire.tank.refill-rate";
    private static final String HYDRANT_REFILL_RATE = "fire.tank.refill_hydrant_rate";
    
    private int maxWater;		//water capacity
    private int maxDistance;	//max distance that water reaches
    private int maxPower;		//max amount of water launched by timestep
    private int refugeRefillRate, hydrantRefillRate;	//refill rates
    private int lastWater; 	//amount of water I had in last cycle
    
    private boolean refugeRefillRateIsCorrect; 	//indicates whether I have read or calculated refill rate
    
    private YFireSimulator fireSimulator;
    private Map<EntityID, YBuilding> yBuildings;
    
    private Map<EntityID, Integer> waterSourceBlockedIDs;
    private EntityID currentTarget; 

    @Override
    public String toString() {
        return String.format("FireFighter(%s)", me().getID());
    }
    
    @Override
    protected void postConnect() {
        super.postConnect();
        Logger.info("postConnect of FireFighter");
        lastWater = -1;
        
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION);
        maxWater = readConfigIntValue(MAX_WATER_KEY, 10000);
        maxDistance = readConfigIntValue(MAX_DISTANCE_KEY, 30000);
        maxPower = readConfigIntValue(MAX_POWER_KEY, 1000);
        refugeRefillRate = readConfigIntValue(REFUGE_REFILL_RATE, -1);
        
        if(refugeRefillRate == -1){ //could not read refugeRefillRate... assume default
        	refugeRefillRateIsCorrect = false;
        	refugeRefillRate = 1000;
        }
        else {
        	refugeRefillRateIsCorrect = true;
        }
        //refugeRefillRate = readConfigIntValue("resq-fire.water_refill_rate", 1000);
        //refugeRefillRate = readConfigIntValue("resq-fire.water-refill-rate", 1000);
        //refugeRefillRate = readConfigIntValue("resq-fire.water-refill-rate", 1000);
        
        hydrantRefillRate = readConfigIntValue(HYDRANT_REFILL_RATE, 150);
        
        Logger.info(String.format(
    		"FireFighter connected.: max extinguish distance = %d, "
    		+ "max power = %d, max tank = %d, refugeRefill = %d, "
    		+ "hydrantRefill = %d", maxDistance, maxPower, maxWater, refugeRefillRate, hydrantRefillRate
    	));
        
        // ---- constructs its 'small' fire simulator ----
        Logger.info("Creating own fire simulator...");

        //first, creates an YBuilding for each Building and populates the map
        yBuildings = new HashMap<EntityID, YBuilding>();
        for(StandardEntity s : model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE)){
        	YBuilding y = new YBuilding((Building)s);
        	yBuildings.put(s.getID(), y);
        }
        
        //second, creates the fire simulator
        fireSimulator = new YFireSimulator(model, yBuildings);
        
        waterSourceBlockedIDs = new HashMap<EntityID, Integer>(); 
        
        Logger.info("Fire simulator created.");
        
    }

    @Override
    protected void doThink(int time, ChangeSet changed, Collection<Command> heard) throws Exception {
        if (time == readConfigIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY, 3)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        
        /*for (Command next : heard) {
            Logger.debug("Heard " + next);
        }*/
        
        //calculates next step of fire simulation
        fireSimulator.step();
        
        if(stuck()){
        	//instantiates a BlockedArea about this agent
        	BlockedArea aroundMe = new BlockedArea(location().getID(), me().getX(), me().getY(), time);
        	problemsToReport.add(aroundMe);
        }
        
        //updates YBuilding data from observation (overrides predicted data)
        for (EntityID id : changed.getChangedEntities()){
        	YBuilding yb = yBuildings.get(id);
        	
        	if (yb != null){	//this means that ID refers to a building
        		Building b = (Building) model.getEntity(id); 
        		if (b == null){// || b.isTemperatureDefined() || b.isFierynessDefined()){
        			Logger.error("While updating YBuildings: Building "+id+" not found or has undefined temperature/fieryness");
        			continue;
        		}
        		yb.updateFromObservation(time, b.getTemperature(), b.getFieryness());
        		Logger.info("Updating " + yb + " from observation.");
        	}
        }
        
        FireBrigade me = me();
        
        // Check and update the water sources blocked
        for (Map.Entry<EntityID, Integer> entry : waterSourceBlockedIDs.entrySet()) {
    		if(time-entry.getValue() >= (maxWater/hydrantRefillRate)){
    			waterSourceIDs.add(entry.getKey());
    			waterSourceBlockedIDs.remove(entry);
    		}
    	}
        
        // Are we currently filling with water?
        if (me.isWaterDefined() && me.getWater() < maxWater && stateMachine.currentState() == ActionStates.FireFighter.REFILLING_WATER) {
        	Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Just got at the Refuge
        if (me.isWaterDefined() && me.getWater() < maxWater && location() instanceof Refuge) {
        	stateMachine.setState(ActionStates.FireFighter.REFILLING_WATER);
            Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Just got at the Hydrant
        if (me.isWaterDefined() && me.getWater() < maxPower && location() instanceof Hydrant) {
        	boolean flagAlone = true;
        	for (StandardEntity agent : firefighters) { 
        		if(agent.getID().getValue() > me.getID().getValue()){
        			if( model.getDistance(me.getID(), agent.getID()) < sightRange ){ 
						flagAlone = false;
    				}
        			if(flagAlone){
        				stateMachine.setState(ActionStates.FireFighter.REFILLING_WATER);
			            Logger.info("Filling with water at " + location());
			            sendRest(time);
			            return;
        			}
        		}
			}
			// Find a new source of water
			if(waterSourceIDs.contains(location().getID())){ 
				waterSourceIDs.remove(location().getID());
				waterSourceBlockedIDs.put(location().getID(),time);
				List<EntityID> pathWater = searchStrategy.shortestPath(me().getPosition(), waterSourceIDs).getPath();
    	        if (pathWater != null) {
    	            Logger.info("Moving to a new water source");
    	            sendMove(time, pathWater);
    	            return;
    	        }
			}
        }
        // Are we out of water?
    	if (me.isWaterDefined() && me.getWater() < maxPower) {
    		stateMachine.setState(ActionStates.FireFighter.OUT_OF_WATER);
            List<EntityID> pathWater = searchStrategy.shortestPath(me().getPosition(), waterSourceIDs).getPath();
            if (pathWater != null) {
                Logger.info("Moving to water source");
                sendMove(time, pathWater);
                return;
            }
        }
    	
    	// If the agent is on a fire zone and getting damaged, the agent moves to the refuge
        if (me().getDamage() >= 10){
        	Logger.debug("Receiving damage");
        	List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), refugeIDs).getPath();
            if (path != null) {
                Logger.info("Moving to refuge");
                sendMove(time, path);
                return;
            }else {
                Logger.debug("Couldn't plan a path to the refuge.");
                path = randomWalk();
                stateMachine.setState(ActionStates.RANDOM_WALK);
                Logger.info("Moving randomly");
                sendMove(time, path);
                return;
            }
        }
                
        // get ID of all buildings on fire that i know 
        Collection<EntityID> all = getBurningBuildings();
        
        //just to see the memory, delete later
        String str = "Burning Buildings Memory ";
        for (EntityID next : all) {
        	str += next + " ";
        }
        Logger.info(str);
        
        // Can we extinguish any right now?
        for (EntityID next : all) {
            if (model.getDistance(getID(), next) <= sightRange) {
            	stateMachine.setState(ActionStates.FireFighter.EXTINGUISHING);
            	Logger.info("Extinguishing " + next);
                sendExtinguish(time, next, maxPower);
                //TODO: send engage message!
                return;
            }
            Logger.info(String.format("Target %s out of sight range. Dist=%d", next, model.getDistance(getID(), next)));
        }
        
        // Going to a target
        if(stateMachine.currentState() == ActionStates.GOING_TO_TARGET){
        	if(model.getDistance(me.getID(), currentTarget) <= sightRange ){
        		Building yb = (Building) model.getEntity(currentTarget);
                if (yb != null && yb.isOnFire()) {
                	stateMachine.setState(ActionStates.FireFighter.EXTINGUISHING);
                	Logger.info("Extinguishing " + currentTarget);
                    sendExtinguish(time, currentTarget, maxPower);
                    return;
                }else{
                	currentTarget = null;
                	stateMachine.setState(ActionStates.RANDOM_WALK);
                }
        	}else{
        		List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), currentTarget).getPath();
                if (path != null) {
                    Logger.info("Moving to the target");
                    path.remove(path.size()-1);
                    sendMove(time, path);
                    return;
                }	
        	}
        }
        
        
        // Plan a path to a fire
        for (EntityID next : all) {
            List<EntityID> path = planPathToFire(next); 
            if (path != null) {
            	stateMachine.setState(ActionStates.GOING_TO_TARGET);
            	path.remove(path.size()-1);
                Logger.info("Moving to target " + next + " path " + path);
                sendMove(time, path);
                currentTarget = next;
                return;
            }
        }
        List<EntityID> path = null;
        Logger.debug("Couldn't plan a path to a fire.");
        path = randomWalk();
        stateMachine.setState(ActionStates.RANDOM_WALK);
        Logger.info("Moving randomly");
        sendMove(time, path);
    }
    

    /****** BEGIN: overrides send* methods to register lastWater property ******/ 
    @Override
    protected void sendMove(int time, List<EntityID> path){
    	super.sendMove(time, path);
    	lastWater = me().getWater();
    }
    
    @Override
    protected void sendRest(int time){
    	super.sendRest(time);
    	//calculates the water it has obtained; calculates refill rate and sends if necessary
    	if(! refugeRefillRateIsCorrect){
    		Logger.info("Will calculate refill rate!");
    		int deltaWater = me().getWater() - lastWater;
    		if (deltaWater > 0){
    			refugeRefillRate = deltaWater;
    			refugeRefillRateIsCorrect = true;
    			Logger.info("Refill rate calculation complete. Will communicate.");
    		}
    		else {
    			Logger.error("Negative refill rate on sendRest? Found: " + deltaWater);
    		}
    	}
    	lastWater = me().getWater();
    }
    
    @Override
    protected void sendExtinguish(int time, EntityID target, int water){
    	super.sendExtinguish(time, target, water);
    	lastWater = me().getWater();
    }
    /****** END: overrides send* methods to register lastWater property ******/
    
    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }

    private Collection<EntityID> getBurningBuildings() {
        //Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<Building>();
        
        for (BurningBuilding next : burningBuildings.values()) {        	
        	// Is the building visible by the agent?
        	if (model.getDistance(getID(), next.getEntityID()) <= sightRange) {
        		if (model.getEntity(next.getEntityID()) instanceof Building) {
	        		Building yb = (Building) model.getEntity(next.getEntityID());
	                if (yb != null && ( yb.isOnFire() && yb.getBrokenness() < 90)) {
	                	result.add(yb);
	                }
	                continue;
        		}
        	}   	
        	// Is it solved?
        	if (! next.isSolved()) {
        		Building b = (Building) model.getEntity(next.getEntityID());
        		result.add(b);
        	}
        }
        // Sort by distance
        Collections.sort(result, new DistanceSorter(location(), model));
        return objectsToIDs(result);
    }

    private List<EntityID> planPathToFire(EntityID target) {
    	//TODO melhorar isso, fazendo os bombeiros irem ate 'perto' do alvo
    	/*
        // Try to get to anything within sightRange of the target
        Collection<StandardEntity> targets = model.getObjectsInRange(target, sightRange);
        if (targets.isEmpty()) {
            return null;
        }*/
    	Set<EntityID> neighs = neighbours.get(target);
        return searchStrategy.shortestPath(me().getPosition(), neighs).getPath();
    }

	@Override
	protected void failsafe() {
		if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        
        FireBrigade me = me();
        // Are we currently filling with water?
        if (me.isWaterDefined() && me.getWater() < maxWater && location() instanceof Refuge) {
            Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Are we out of water?
        if (me.isWaterDefined() && me.getWater() == 0) {
            // Head for a refuge
            List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), refugeIDs);
            if (path != null) {
                Logger.info("Moving to refuge");
                sendMove(time, path);
                return;
            }
            else {
                Logger.debug("Couldn't plan a path to a refuge.");
                path = randomWalk();
                Logger.info("Moving randomly");
                sendMove(time, path);
                return;
            }
        }
        // Find all buildings that are on fire
        Collection<EntityID> all = failSafeGetBurningBuildings();
        // Can we extinguish any right now?
        for (EntityID next : all) {
            if (model.getDistance(getID(), next) <= maxDistance) {
                Logger.info("Extinguishing " + next);
                sendExtinguish(time, next, maxPower);
                //sendSpeak(time, 1, ("Extinguishing " + next).getBytes());
                return;
            }
        }
        // Plan a path to a fire
        for (EntityID next : all) {
            List<EntityID> path = failSafePlanPathToFire(next);
            if (path != null) {
                Logger.info("Moving to target");
                sendMove(time, path);
                return;
            }
        }
        List<EntityID> path = null;
        Logger.debug("Couldn't plan a path to a fire.");
        path = randomWalk();
        Logger.info("Moving randomly");
        sendMove(time, path);
		
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
        // Try to get to anything within maxDistance of the target
        Collection<StandardEntity> targets = model.getObjectsInRange(target, maxDistance);
        if (targets.isEmpty()) {
            return null;
        }
        return failSafeSearch.breadthFirstSearch(me().getPosition(), objectsToIDs(targets));
    }
	
	
}

