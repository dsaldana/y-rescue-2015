package agent.platoon;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.messages.Command;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.Refuge;
import statemachine.States;
import util.DistanceSorter;

/**
 *  RoboMedic agent. Implements a simple scheme to rescue Civilians.
 */
public class Ambulance extends AbstractPlatoon<AmbulanceTeam> {
    private Collection<EntityID> unexploredBuildings;

    @Override
    public String toString() {
        return String.format("Ambulance(%s)", me().getID());
    }

    @Override
    protected void postConnect() {
        super.postConnect();
        model.indexClass(StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE, StandardEntityURN.POLICE_FORCE, StandardEntityURN.AMBULANCE_TEAM, StandardEntityURN.REFUGE,StandardEntityURN.HYDRANT,StandardEntityURN.GAS_STATION, StandardEntityURN.BUILDING);
        unexploredBuildings = new HashSet<EntityID>(buildingIDs);
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
        
        updateUnexploredBuildings(changed);
        // Am I transporting a civilian to a refuge?
        if (someoneOnBoard()) {
            // Am I at a refuge?
            if (location() instanceof Refuge) {
                // Unload!
            	Logger.info("Unloading");
            	stateMachine.setState(States.Ambulance.UNLOADING);
                sendUnload(time);
                return;
            }
            else {
                // Move to a refuge
                List<EntityID> path = search.breadthFirstSearch(me().getPosition(), refugeIDs);
                if (path != null) {
                	stateMachine.setState(States.Ambulance.CARRYING_WOUNDED);
                	Logger.info("Moving to refuge");
                    sendMove(time, path);
                    return;
                }
                // What do I do now? Might as well carry on and see if we can dig someone else out.
                Logger.debug("Failed to plan path to refuge");
            }
        }
        // Go through targets (sorted by distance) and check for things we can do
        for (Human next : getTargets()) {
            if (next.getPosition().equals(location().getID())) {
                // Targets in the same place might need rescueing or loading
                if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
                    // Load
                	stateMachine.setState(States.Ambulance.LOADING);
                    Logger.info("Loading " + next);
                    sendLoad(time, next.getID());
                    return;
                }
                if (next.getBuriedness() > 0) {
                    // Rescue
                	stateMachine.setState(States.Ambulance.UNBURYING);
                    Logger.info("Rescueing " + next);
                    sendRescue(time, next.getID());
                    return;
                }
            }
            else {
                // Try to move to the target
                List<EntityID> path = search.breadthFirstSearch(me().getPosition(), next.getPosition());
                if (path != null) {
                	stateMachine.setState(States.GOING_TO_TARGET);
                    Logger.info("Moving to target");
                    sendMove(time, path);
                    return;
                }
            }
        }
        // Nothing to do
        List<EntityID> path = search.breadthFirstSearch(me().getPosition(), unexploredBuildings);
        if (path != null) {
        	stateMachine.setState(States.Ambulance.SEARCHING_BUILDINGS);
            Logger.info("Searching buildings");
            sendMove(time, path);
            return;
        }
        stateMachine.setState(States.RANDOM_WALK);
        Logger.info("Moving randomly");
        sendMove(time, randomWalk());
    }

    @Override
    protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
        return EnumSet.of(StandardEntityURN.AMBULANCE_TEAM);
    }

    private boolean someoneOnBoard() {
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
}
