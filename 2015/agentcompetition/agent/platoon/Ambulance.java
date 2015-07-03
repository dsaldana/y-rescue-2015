package agent.platoon;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardWorldModel;
import statemachine.ActionStates;
import util.DistanceSorter;
import util.HPSorter;
import util.LastVisitSorter;

/**
 *  RoboMedic agent. Implements a simple scheme to rescue Civilians.
 */
public class Ambulance extends AbstractPlatoon<AmbulanceTeam> {
	
	private Integer totalHP = 0;
    private Collection<EntityID> unexploredBuildings;
    
    private Integer assignedBuildingNumber = 0;
    
    Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
        @Override
        public Set<EntityID> createValue() {
            return new HashSet<EntityID>();
        }
    };

    @Override
    public String toString() {
        return String.format("Ambulance(%s)", me().getID());
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING);
        unexploredBuildings = new HashSet<EntityID>(buildingIDs);
        
        model.getBounds();
        
        for (Entity next : (StandardWorldModel) model) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area)next).getNeighbours();
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (EntityID entId : neighbours.keySet()){
        	sb.append(String.valueOf(entId.getValue()));
        	sb.append(" ");
        } 
        
        System.out.println("Area ids : " + sb.toString());
        
        totalHP = me().getHP();
        assignedBuildingNumber = unexploredBuildings.size();
    }

    @Override
    protected void doThink(int time, ChangeSet changed, Collection<Command> heard) throws Exception {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channel 1
            sendSubscribe(time, 1);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        
        throw new Exception("Testing failsafe behavior");
        
        /*
        System.out.println("\nTime ambulance: " + time);
        
        String statusString = "HP:" + me().getHP() + " Total HP:" + totalHP + " burriedness:" + me().getBuriedness() + " Damage:" + me().getDamage() + " Stamina:" + me().getStamina() + " unexploredBuildings:" + unexploredBuildings.size();
        System.out.println(statusString);
        
        String refugees = "";
        for(EntityID ent : refugeIDs){
        	refugees += ent.getValue() + " ";
        }
        
        String burningBuildingIds = "";
        Iterator burningIterator = burningBuildings.keySet().iterator();
        while(burningIterator.hasNext()){
        	burningBuildingIds += ((EntityID) burningIterator.next()).getValue() + " ";
        }
        
        String miscStatus = "Refugees IDs: "+ refugees + " burningIds:" + burningBuildingIds;
        System.out.println(miscStatus);
        
        updateUnexploredBuildings(changed);
        
        System.out.println("Unexplored buildings: " + unexploredBuildings.size());
        
        // Am I transporting a civilian to a refuge?
        if (someoneOnBoard()) {
            // Am I at a refuge?
            if (location() instanceof Refuge) {
                // Unload!
            	Logger.info("Unloading");
            	stateMachine.setState(ActionStates.Ambulance.UNLOADING);
                sendUnload(time);
                return;
            }
            else {
                // Move to a refuge
                List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), refugeIDs).getPath();
                if (path != null) {
                	stateMachine.setState(ActionStates.Ambulance.CARRYING_WOUNDED);
                	Logger.info("Moving to refuge");
                    sendMove(time, path);
                    return;
                }
                // What do I do now? Might as well carry on and see if we can dig someone else out.
                Logger.debug("Failed to plan path to refuge");
            }
        }
        else{
        	if (!(location() instanceof Refuge)) {
    			for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
    				if (((Human) next).getPosition().equals(location().getID()) ) {
    					if ((next instanceof Civilian) && ((Human) next).getBuriedness() == 0 && !(location() instanceof Refuge)) {
    	                    // Load
    	                    Logger.info("Loading now!" + next);
    	                    sendLoad(time, next.getID());
    	                    return;
    	                }
    				}
    			}
        	}
        }
        
        if (me().getDamage() >= 10){
        	Logger.debug("Receiving damage");
        	List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), refugeIDs).getPath();
            if (path != null) {
                Logger.info("Moving to refuge");
                sendMove(time, path);
                return;
            }
            // What do I do now? Might as well carry on and see if we can dig someone else out.
            Logger.debug("Failed to plan path to refuge");
        }
        
        if(unexploredBuildings.size() > assignedBuildingNumber * 0.1){
        	Logger.info("Not enough exploration yet...");
        	List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), unexploredBuildings).getPath();
            if (path != null) {
            	Logger.info("Searching buildings -50%");
                sendMove(time, path);
                return;
            }
        } 
        
        // Go through targets (sorted by distance) and check for things we can do
        List<Human> humanList = getTargets();
        System.out.println("HumanList size: " + humanList.size());
        
        for (Human next : getTargets()) {
        	
        	Logger.info("Pos " + next.getPosition() +  " My pos:" + me().getPosition());
        	
        	if(burningBuildings.containsKey(next.getPosition())){
                Logger.info("The building is burning! Next");
            	continue;
            }
        	
        	if(refugeIDs.contains(next.getPosition(model))){
        		Logger.info("The Civilian is on refugee! Next");
            	continue;
        	}
        	
            if (next.getPosition().equals(location().getID()) ) {
            	// Targets in the same place might need rescueing or loading
                if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
                    // Load
                	stateMachine.setState(ActionStates.Ambulance.LOADING);
                    Logger.info("Loading " + next);
                    sendLoad(time, next.getID());
                    return;
                }
                
                if (next.getBuriedness() > 0) {
                    // Rescue
                	String humanStatusString = "HP:" + next.getHP() + " burriedness:" + next.getBuriedness() + " Damage:" + next.getDamage() + " Stamina:" + next.getStamina();
                    Logger.info("Rescueing " + next + " " + humanStatusString);
                	stateMachine.setState(ActionStates.Ambulance.UNBURYING);
                    sendRescue(time, next.getID());
                    return;
                }
            }
            else {
                // Try to move to the target
                List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), next.getPosition()).getPath();
                if (path != null) {
                	stateMachine.setState(ActionStates.GOING_TO_TARGET);
                    Logger.info("Moving to target");
                    sendMove(time, path);
                    return;
                }
            }
        }
        
        // Nothing to do
        List<EntityID> entityIDList = new ArrayList<EntityID> ();
        for (EntityID subset : unexploredBuildings) {
        	entityIDList.add(subset);
        }
        Collections.shuffle(entityIDList);
        
        List<EntityID> path = searchStrategy.shortestPath(me().getPosition(), entityIDList).getPath();
        if (path != null) {
        	stateMachine.setState(ActionStates.Ambulance.SEARCHING_BUILDINGS);
            Logger.info("Searching buildings");
            sendMove(time, path);
            return;
        }
        
        stateMachine.setState(ActionStates.RANDOM_WALK);
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());
        */
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

    private boolean someoneOnBoard() {
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
            if (((Human) next).getPosition().equals(getID())) {
                Logger.debug(next + " is on board");
                return true;
            }
        }
        return false;
    }
    
    /**
     * Copied from sample agent. Do not change
     * @return
     */
    private boolean failSafeSomeoneOnBoard() {
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
            if (((Human)next).getPosition().equals(getID())) {
                Logger.debug(next + " is on board");
                return true;
            }
        }
        return false;
    }

    private List<Human> getTargets() {
        List<Human> targets = new ArrayList<Human>();
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
        	//System.out.println("\nHuman: " + next.getID());
        	
        	Human h = (Human) next;
            if (h == me()) {
                continue;
            }
            if (h.isHPDefined()
                && h.isBuriednessDefined()
                && h.isDamageDefined()
                && h.isPositionDefined()
                && h.getHP() > 0
                && (h.getBuriedness() > 0 || h.getDamage() > 0)
                ) {
                targets.add(h);
            }
        }
        
        //Collections.sort(targets, new DistanceSorter(location(), model));
        Collections.sort(targets, new HPSorter(location(), model));
        
        return targets;
    }
    
    /**
     * Copied from SampleAgent. Do not change
     * @return
     */
    private List<Human> failSafeGetTargets() {
        List<Human> targets = new ArrayList<Human>();
        for (StandardEntity next : model.getEntitiesOfType(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM)) {
            Human h = (Human)next;
            if (h == me()) {
                continue;
            }
            if (h.isHPDefined()
                && h.isBuriednessDefined()
                && h.isDamageDefined()
                && h.isPositionDefined()
                && h.getHP() > 0
                && (h.getBuriedness() > 0 || h.getDamage() > 0)) {
                targets.add(h);
            }
        }
        Collections.sort(targets, new DistanceSorter(location(), model));
        return targets;
    }

    private void updateUnexploredBuildings(ChangeSet changed) {
        for (EntityID next : changed.getChangedEntities()) {
            unexploredBuildings.remove(next);
        }
    }
    
    /**
     * Copied from SampleAgent. Do not change
     * @return
     */
    private void failSafeUpdateUnexploredBuildings(ChangeSet changed) {
        for (EntityID next : changed.getChangedEntities()) {
            unexploredBuildings.remove(next);
        }
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
        failSafeUpdateUnexploredBuildings(changed);
        // Am I transporting a civilian to a refuge?
        if (failSafeSomeoneOnBoard()) {
            // Am I at a refuge?
            if (location() instanceof Refuge) {
                // Unload!
                Logger.info("Unloading");
                sendUnload(time);
                return;
            }
            else {
                // Move to a refuge
                List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), refugeIDs);
                if (path != null) {
                    Logger.info("Moving to refuge");
                    sendMove(time, path);
                    return;
                }
                // What do I do now? Might as well carry on and see if we can dig someone else out.
                Logger.debug("Failed to plan path to refuge");
            }
        }
        // Go through targets (sorted by distance) and check for things we can do
        for (Human next : failSafeGetTargets()) {
            if (next.getPosition().equals(location().getID())) {
                // Targets in the same place might need rescueing or loading
                if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
                    // Load
                    Logger.info("Loading " + next);
                    sendLoad(time, next.getID());
                    return;
                }
                if (next.getBuriedness() > 0) {
                    // Rescue
                    Logger.info("Rescueing " + next);
                    sendRescue(time, next.getID());
                    return;
                }
            }
            else {
                // Try to move to the target
                List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), next.getPosition());
                if (path != null) {
                    Logger.info("Moving to target");
                    sendMove(time, path);
                    return;
                }
            }
        }
        // Nothing to do
        List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), unexploredBuildings);
        if (path != null) {
            Logger.info("Searching buildings");
            sendMove(time, path);
            return;
        }
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());

	}
}
