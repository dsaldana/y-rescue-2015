package agent.platoon;

import static rescuecore2.misc.Handy.objectsToIDs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
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
import statemachine.States;
import util.DistanceSorter;
/**
 *  RoboFire agent. Implements a simple scheme to fight fires.
 */
public class Firefighter extends AbstractPlatoon<FireBrigade> {
    private static final String MAX_WATER_KEY = "fire.tank.maximum";
    private static final String MAX_DISTANCE_KEY = "fire.extinguish.max-distance";
    private static final String MAX_POWER_KEY = "fire.extinguish.max-sum";

    private int maxWater;
    private int maxDistance;
    private int maxPower;
    

    @Override
    public String toString() {
        return String.format("FireFighter(%s)", me().getID());
    }
    
    @Override
    protected void postConnect() {
        super.postConnect();
        Logger.info("postConnect of FireFighter");
        model.indexClass(StandardEntityURN.BUILDING, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION);
        maxWater = config.getIntValue(MAX_WATER_KEY);
        maxDistance = config.getIntValue(MAX_DISTANCE_KEY);
        maxPower = config.getIntValue(MAX_POWER_KEY);
        Logger.info("FireFighter connected: max extinguish distance = " + maxDistance + ", max power = " + maxPower + ", max tank = " + maxWater);
        
    }

    @Override
    protected void doThink(int time, ChangeSet changed, Collection<Command> heard) {
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
        	stateMachine.setState(States.FireFighter.REFILLING_WATER);
            Logger.info("Filling with water at " + location());
            sendRest(time);
            return;
        }
        // Are we out of water?
        if (me.isWaterDefined() && me.getWater() < maxPower) {
            // Head for a refuge
        	stateMachine.setState(States.FireFighter.OUT_OF_WATER);
        	
            List<EntityID> pathRefuge = search.breadthFirstSearch(me().getPosition(), waterSourceIDs);
            
            if (pathRefuge != null) {
                Logger.info("Moving to water source");
                sendMove(time, pathRefuge);
                return;
            }
            else {
                Logger.debug("Couldn't plan a path to a refuge.");
                pathRefuge = randomWalk();
                stateMachine.setState(States.RANDOM_WALK);
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
            	stateMachine.setState(States.FireFighter.EXTINGUISHING);
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
            	stateMachine.setState(States.GOING_TO_TARGET);
                Logger.info("Moving to target " + next + " path " + path);
                sendMove(time, path);
                return;
            }
        }
        List<EntityID> path = null;
        Logger.debug("Couldn't plan a path to a fire.");
        path = randomWalk();
        stateMachine.setState(States.RANDOM_WALK);
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
    	Set<EntityID> neighs = search.getGraph().get(target);
        return search.breadthFirstSearch(me().getPosition(), neighs);
    }
}
