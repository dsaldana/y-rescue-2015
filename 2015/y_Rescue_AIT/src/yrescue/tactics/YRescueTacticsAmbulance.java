package yrescue.tactics;

import java.awt.Polygon;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.MDC;

import com.infomatiq.jsi.Rectangle;

import adf.util.map.PositionUtil;
import adk.sample.basic.event.BasicAmbulanceEvent;
import adk.sample.basic.event.BasicCivilianEvent;
import adk.sample.basic.event.BasicFireEvent;
import adk.sample.basic.event.BasicPoliceEvent;
import adk.sample.basic.util.BasicImpassableSelector;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.sample.basic.util.BasicVictimSelector;
import adk.sample.basic.tactics.BasicTacticsAmbulance;
import adk.team.action.Action;
import adk.team.action.ActionLoad;
import adk.team.action.ActionMove;
import adk.team.action.ActionRescue;
import adk.team.action.ActionRest;
import adk.team.action.ActionUnload;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import comlib.message.information.MessageAmbulanceTeam;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessagePoliceForce;
import comlib.message.information.MessageRoad;
import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.misc.geometry.Line2D;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.misc.geometry.Vector2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.util.DistanceSorter;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import yrescue.util.GeometricUtil;
import yrescue.util.HeatMap;
import yrescue.util.HeatNode;
import yrescue.util.YRescueDistanceSorter;

public class YRescueTacticsAmbulance extends BasicTacticsAmbulance {

	protected final int EXPLORE_TIME_STEP_TRESH = 15;
	protected int EXPLORE_TIME_LIMIT = EXPLORE_TIME_STEP_TRESH;
	protected int LIMIT_TO_REACH_TARGET = 15;
	protected int target_step_counter = 0;
	protected StateMachine stateMachine = null;
	protected int timeoutAction = 0;
	protected Set<Road> impassableRoadList = new HashSet<>();
	protected HeatMap heatMap = null;
	protected int EXPLORE_AREA_SIZE_TRESH = 10000010;
	
	//protected ActionStates.Ambulance states = new ActionStates.Ambulance();
	
    @Override
    public String getTacticsName() {
        return "Y-Rescue Ambulance";
    }
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
    	this.stateMachine = new StateMachine(ActionStates.Ambulance.EXPLORING);
    	this.victimSelector = new BasicVictimSelector(this);
    	this.routeSearcher = new BasicRouteSearcher(this);
    	
    	MDC.put("agent", this);
    	
    	// Prepare HeatMap
    	this.heatMap = new HeatMap(this.agentID, this.world);
        for (Entity next : this.getWorld()) {
            if (next instanceof Area) {
            	// Ignore very small areas to explore
            	if(GeometricUtil.getAreaOfEntity(next.getID(), this.world) < EXPLORE_AREA_SIZE_TRESH) continue;
            	
            	// Ignore non building areas
            	if(!(next instanceof Building)) continue;
            	
            	heatMap.addEntityID(next.getID(), HeatNode.PriorityLevel.LOW, 0);
            }
        }
        
    }

    @Override
    public VictimSelector initVictimSelector() {
        return new BasicVictimSelector(this);
    }

    @Override
    public RouteSearcher initRouteSearcher() {
        return new BasicRouteSearcher(this);
    }

    @Override
    public void registerEvent(MessageManager manager) {
        manager.registerEvent(new BasicCivilianEvent(this, this, this));
        manager.registerEvent(new BasicAmbulanceEvent(this, this));
        manager.registerEvent(new BasicFireEvent(this, this));
        manager.registerEvent(new BasicPoliceEvent(this, this));
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            Logger.debug("organizeUpdateInfo:" + entity.getClass());
            Logger.trace("I'm seeing: " + entity);
            if(entity instanceof Civilian) {
            	Civilian c = (Civilian) entity;
                this.victimSelector.add(c);
                manager.addSendMessage(new MessageCivilian(c));
                Logger.trace(String.format("It's a Civilian. HP: %d, B'ness: %d", c.getHP(), c.getBuriedness()));
            }
            else if(entity instanceof Human) {
            	Human h = (Human) entity;
                this.victimSelector.add(h);
                Logger.trace(String.format("It's a Human. HP: %d, B'ness: %d", h.getHP(), h.getBuriedness()));
            }
            else if(entity instanceof Building) {
                Building b = (Building)entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                }
            }
            else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                Road r = (Road) this.world.getEntity(blockade.getPosition());
                manager.addSendMessage(new MessageRoad(r, blockade, false));
                this.impassableRoadList.add(r);
            }
        }
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        Logger.info("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue());
        //this.refugeList.get(-1); //triggers exception to test failsafe
        
        heatMap.updateNode(this.location.getID(), currentTime);
        heatMap.writeMapToFile();

        // CHECK THIS
        /* === -------- === *
         *   Basic actions  *
         * === -------- === */
        
        Logger.debug("Civilians perceived:" + ((BasicVictimSelector) this.victimSelector).civilianList.size());
        for (Civilian civ : ((BasicVictimSelector) this.victimSelector).civilianList) {
        	Logger.debug("Civilian ID:" + civ.getID() + " pos:" + civ.getPosition() + " burriedness:" + civ.getBuriedness() + " damage:" + civ.getDamage());
        }
        
        if(this.me.getBuriedness() > 0) {
            this.target = null;
            return new ActionRescue(this, this.agentID);
        }
        
        // If we are not in the special condition exploring, update target or get a new one 
        if(!this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.EXPLORING)){
	        if(this.target != null){
	        	this.victimSelector.updateTarget(currentTime, this.target);
	        }
	        else{
	        	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	        }
	        
	        if(this.me.getDamage() >= 100) { //this.someoneOnBoard() || 
	        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
	        }
        }
        
        /* === ---------------------------------- === *
         *  Possible states and their implementation  *
         * === ---------------------------------- === */
        
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.EXPLORING)){
        	Logger.info("Exploring..");
        	if(currentTime < EXPLORE_TIME_LIMIT){
        		if(this.target == null || this.target.getValue() == this.location.getID().getValue()){
        			EntityID nodeToVisit = heatMap.getNodeToVisit();
        			this.target = nodeToVisit;
        			Logger.debug("Entity to visit:" + nodeToVisit.getValue());
        			Logger.debug("Area:" + String.valueOf(GeometricUtil.getAreaOfEntity(nodeToVisit, this.world)));
        			
        			//List<EntityID> result = this.exploreRouteSearcher.noTargetMove(currentTime, this.me);
        			//this.target = result.get(result.size() - 1);
        		}
        		
        		//((ExploreRouteSearcher) this.exploreRouteSearcher).addVisitedEntity(this.location.getID()); // TODO: Fix this, calling this class this way its breaking the inheritance
        		return this.moveTarget(currentTime);
        	}
        	
        	if(!this.tacticsAgent.stuck(time) && target_step_counter <= LIMIT_TO_REACH_TARGET && (this.target != null && this.target.getValue() != this.location.getID().getValue())){
        		this.target_step_counter++;
        		return this.moveTarget(currentTime);
        	}
        	else{
        		this.target_step_counter = 0;
        	}
        	
        	this.target = null;
        	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_TARGET)){
        	Logger.info("Going to target..");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                return this.moveTarget(currentTime);
            }
        	
        	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.RESCUING)){
        	Logger.info("Rescuing ...");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if(victim.getBuriedness() > 0){
        		Logger.info("Burriedness: " + victim.getBuriedness() + " Damage: " + victim.getDamage() + " HP: " + victim.getHP());
        		return new ActionRescue(this, this.target);
        	}
        	
        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
        	Civilian civilian = (Civilian) victim;
            manager.addSendMessage(new MessageCivilian(civilian));
            this.victimSelector.remove(civilian);
            return new ActionLoad(this, this.target);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_REFUGE)){
        	Logger.info("Going to refugee..");
        	if(!this.someoneOnBoard() && this.me.getDamage() <= 0){
        		Logger.info("Does not need to go to refugee, selecting new target..");
        		this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.SELECT_NEW_TARGET);
        	}
        	else{
	        	if(this.location instanceof Refuge) {
	                if(this.someoneOnBoard()) {
	                	this.target = null;
	                	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	                    return new ActionUnload(this);
	                }
	                
	                if(this.me.getDamage() > 0) {
	                    return new ActionRest(this);
	                }
	                else{
	                	this.target = null;
	                	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
	                }
	            }
	        	
	        	return this.moveRefuge(currentTime);
        	}
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.SELECT_NEW_TARGET)){
        	Logger.info("Select new target..");
        	
            this.target = this.victimSelector.getNewTarget(currentTime);
            if(this.target == null) {
            	Logger.info("Problem getting target, return random move ...");
            	EntityID nodeToVisit = heatMap.getNodeToVisit();
                if(nodeToVisit == null){
                	Logger.debug("Random walk");
                	return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
                }
                else{
                	Logger.debug("HeatmMap");
                	this.stateMachine.setState(ActionStates.Ambulance.EXPLORING);
                	this.target = nodeToVisit;
                	return this.moveTarget(currentTime);
                }
            }
            
            // Begin target basic processing
            do {
                Human victim = (Human) this.world.getEntity(this.target);
                if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_TARGET);
                    return this.moveTarget(currentTime);
                }
                
                if (victim.getHP() <= 0) continue;
                
                if (victim.getBuriedness() > 0) {
                	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
                    return new ActionRescue(this, this.target);
                }
                
                // In the case of rescue already
                if (victim instanceof Civilian) {
                	this.stateMachine.setState(ActionStates.Ambulance.CARRYING_WOUNDED);
                    Civilian civilian = (Civilian) victim;
                    manager.addSendMessage(new MessageCivilian(civilian));
                    this.victimSelector.remove(civilian);
                    return new ActionLoad(this, this.target);
                }
                
                // Disaster relief agent
                if (victim instanceof AmbulanceTeam) {
                    AmbulanceTeam ambulanceTeam = (AmbulanceTeam) victim;
                    manager.addSendMessage(new MessageAmbulanceTeam(ambulanceTeam, MessageAmbulanceTeam.ACTION_REST, null));
                    this.victimSelector.remove(ambulanceTeam);
                } else if (victim instanceof FireBrigade) {
                    FireBrigade fireBrigade = (FireBrigade) victim;
                    manager.addSendMessage(new MessageFireBrigade(fireBrigade, MessageFireBrigade.ACTION_REST, null));
                    this.victimSelector.remove(fireBrigade);
                } else if (victim instanceof PoliceForce) {
                    PoliceForce policeForce = (PoliceForce) victim;
                    manager.addSendMessage(new MessagePoliceForce(policeForce, MessagePoliceForce.ACTION_REST, null));
                    this.victimSelector.remove(policeForce);
                }
                // The target has already been rescued. Or in the case of excluded
                this.target = this.victimSelector.getNewTarget(currentTime);
            }while (this.target != null);
        }
        
        Logger.info("Default behaviour, random walk ... ???");
        EntityID nodeToVisit = heatMap.getNodeToVisit();
        if(nodeToVisit == null){
        	Logger.debug("Random walk");
        	return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        else{
        	Logger.debug("HeatmMap");
        	this.stateMachine.setState(ActionStates.Ambulance.EXPLORING);
        	this.target = nodeToVisit;
        	return this.moveTarget(currentTime);
        	//List<EntityID> etList = new LinkedList<EntityID>();
        	//etList.add(nodeToVisit);
        	//return new ActionMove(this, etList);
        }
    }

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		//failSafeUpdateUnexploredBuildings(changed);
		Logger.info("" + me + " going failsafe in timestep " + currentTime);
		
		if (failSafeSomeoneOnBoard()) {
			if (location() instanceof Refuge) {
				Logger.info("Unloading");
				return new ActionUnload(this);
			} else {
				return this.moveRefuge(currentTime);
			}
		}
		
		for (Human next : failSafeGetTargets()) {
			if (next.getPosition().equals(location().getID())) {
				if ((next instanceof Civilian) && next.getBuriedness() == 0 && !(location() instanceof Refuge)) {
					Logger.info("Loading " + next);
					return new ActionLoad(this, (Civilian) next);
				}
				if (next.getBuriedness() > 0) {
					Logger.info(String.format(
						"Rescueing %s. HP: %d, B'ness: %d", next, next.getHP(), next.getBuriedness()
					));
					return new ActionRescue(this, next.getID());
				}
			} else {
				List<EntityID> path = routeSearcher.getPath(currentTime, me().getPosition(), next.getPosition());
				if (path != null) {
					Logger.info("Moving to target with " + path);
					return new ActionMove(this, path);
				}
			}
		}
		/*List<EntityID> path = failSafeSearch.breadthFirstSearch(me().getPosition(), unexploredBuildings);
		if (path != null) {
			Logger.info("Searching buildings");
			sendMove(time, path);
			return;
		}*/
		Logger.info("Moving randomly");
		return new ActionMove(this, routeSearcher.noTargetMove(currentTime, me.getPosition()));
	}

	public boolean someoneOnBoard() {
		if(this.target == null) return false;
		return PositionUtil.equalsPoint(this.world.getEntity(this.target).getLocation(world), this.me.getLocation(world), 500);
        //return this.target != null && ((Human)this.world.getEntity(this.target)).getPosition().getValue() == this.me.getID().getValue();
    }
	
	/**
	 * Copied from SampleAgent. Do not change
	 * 
	 * @return
	 */
	private List<Human> failSafeGetTargets() {
		List<Human> targets = new ArrayList<Human>();
		for (StandardEntity next : model.getEntitiesOfType(
				StandardEntityURN.CIVILIAN, StandardEntityURN.FIRE_BRIGADE,
				StandardEntityURN.POLICE_FORCE,
				StandardEntityURN.AMBULANCE_TEAM)) {
			Human h = (Human) next;
			if (h == me()) {
				continue;
			}
			if (h.isHPDefined() && h.isBuriednessDefined()
					&& h.isDamageDefined() && h.isPositionDefined()
					&& h.getHP() > 0
					&& (h.getBuriedness() > 0 || h.getDamage() > 0)) {
				Logger.trace(String.format(
					"Adding %s to targets. HP: %d, B'ness: %d", h, h.getHP(), h.getBuriedness()
				));
				targets.add(h);
			}
		}
		Collections.sort(targets, new DistanceSorter(location(), model));
		return targets;
	}

	/**
	 * Copied from sample agent. Do not change
	 * 
	 * @return
	 */
	private boolean failSafeSomeoneOnBoard() {
		for (StandardEntity next : model
				.getEntitiesOfType(StandardEntityURN.CIVILIAN)) {
			if (((Human) next).getPosition().equals(getID())) {
				Logger.debug(next + " is on board");
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String toString(){
    	return "Ambulance:" + this.getID();
    }

	 /**
	  * Copied from SampleAgent. Do not change
	  * 
	  * @return
	  *
	private void failSafeUpdateUnexploredBuildings(ChangeSet changed) {
		for (EntityID next : changed.getChangedEntities()) {
			unexploredBuildings.remove(next);
		}
	}*/
}
