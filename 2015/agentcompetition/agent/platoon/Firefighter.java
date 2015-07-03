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

import problem.BurningBuilding;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.FireBrigade;
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
    
    private YFireSimulator fireSimulator;
    private Map<EntityID, YBuilding> yBuildings;
    

    @Override
    public String toString() {
        return String.format("FireFighter(%s)", me().getID());
    }
    
    @Override
    protected void postConnect() {
        super.postConnect();
        Logger.info("postConnect of FireFighter");
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION);
        maxWater = readConfigIntValue(MAX_WATER_KEY, 10000);
        maxDistance = readConfigIntValue(MAX_DISTANCE_KEY, 30000);
        maxPower = readConfigIntValue(MAX_POWER_KEY, 1000);
        refugeRefillRate = readConfigIntValue(REFUGE_REFILL_RATE, 1000);
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
        //TODO: refuge is not listed as YBuilding
        yBuildings = new HashMap<EntityID, YBuilding>();
        for(StandardEntity s : model.getEntitiesOfType(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE)){
        	YBuilding y = new YBuilding((Building)s);
        	yBuildings.put(s.getID(), y);
        }
        
        //second, creates the fire simulator
        fireSimulator = new YFireSimulator(model, yBuildings);
        
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
        
        //throw new Exception("Testing fail safe-ness");
        
        FireBrigade me = me();
        // Are we currently filling with water?
        if (me.isWaterDefined() && me.getWater() < maxWater && location() instanceof Refuge) {
        	stateMachine.setState(ActionStates.FireFighter.REFILLING_WATER);
            Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Are we out of water?
        if (me.isWaterDefined() && me.getWater() < maxPower) {
            // Head for a refuge
        	stateMachine.setState(ActionStates.FireFighter.OUT_OF_WATER);
        	
            List<EntityID> pathRefuge = searchStrategy.shortestPath(me().getPosition(), waterSourceIDs).getPath();
            
            if (pathRefuge != null) {
                Logger.info("Moving to water source");
                sendMove(time, pathRefuge);
                return;
            }
            else {
                Logger.debug("Couldn't plan a path to a refuge.");
                pathRefuge = randomWalk();
                stateMachine.setState(ActionStates.RANDOM_WALK);
                Logger.info("Moving randomly");
                sendMove(time, pathRefuge);
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
        
        
        // Plan a path to a fire
        for (EntityID next : all) {
            List<EntityID> path = planPathToFire(next);
            if (path != null) {
            	stateMachine.setState(ActionStates.GOING_TO_TARGET);
                Logger.info("Moving to target " + next + " path " + path);
                sendMove(time, path);
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

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.FIRE_BRIGADE);
    }

    private Collection<EntityID> getBurningBuildings() {
        //Collection<StandardEntity> e = model.getEntitiesOfType(StandardEntityURN.BUILDING);
        List<Building> result = new ArrayList<Building>();
        
        
        for (BurningBuilding next : burningBuildings.values()) {
            //if (next instanceof Building) {
            //    Building b = (Building)next;
            //    if (b.isOnFire()) {
            //        result.add(b);
            //    }
            //}
        	
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
                sendSpeak(time, 1, ("Extinguishing " + next).getBytes());
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

