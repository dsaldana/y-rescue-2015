package yrescue.tactics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import adk.launcher.agent.TacticsAgent;
import adk.sample.basic.event.BasicAmbulanceEvent;
import adk.sample.basic.event.BasicCivilianEvent;
import adk.sample.basic.event.BasicFireEvent;
import adk.sample.basic.event.BasicPoliceEvent;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.sample.basic.util.BasicVictimSelector;
import adk.sample.basic.tactics.BasicTacticsAmbulance;
import adk.team.action.Action;
import adk.team.action.ActionLoad;
import adk.team.action.ActionMove;
import adk.team.action.ActionRescue;
import adk.team.action.ActionRest;
import adk.team.action.ActionUnload;
import adk.team.tactics.Tactics;
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
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.search.ExploreRouteSearcher;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;

public class YRescueTacticsAmbulance extends BasicTacticsAmbulance {

	protected final int EXPLORE_TIME_STEP_TRESH = 30;
	protected int EXPLORE_TIME_LIMIT = EXPLORE_TIME_STEP_TRESH;
	protected RouteSearcher exploreRouteSearcher = null;
	protected StateMachine stateMachine = null;
	protected int timeoutAction = 0;
	
	//protected ActionStates.Ambulance states = new ActionStates.Ambulance();
	
    @Override
    public String getTacticsName() {
        return "Y-Rescue Ambulance";
    }
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
        // TODO: fill this method if needed
    	this.exploreRouteSearcher = new ExploreRouteSearcher(this, new RouteManager(this.world));
    	this.stateMachine = new StateMachine(ActionStates.Ambulance.EXPLORING);
    	this.victimSelector = new BasicVictimSelector(this);
    	this.routeSearcher = new BasicRouteSearcher(this);
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
        manager.registerEvent(new BasicCivilianEvent(this, this));
        manager.registerEvent(new BasicAmbulanceEvent(this, this));
        manager.registerEvent(new BasicFireEvent(this, this));
        manager.registerEvent(new BasicPoliceEvent(this, this));
    }

    @Override
    public void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager) {
    	
    	Set<EntityID> reportedRoads = new HashSet<>();
    	
    	if (this.tacticsAgent.stuck (currentTime)){
    		manager.addSendMessage(new Mes ); //What Message should i send?
    	}
    	
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            if(entity instanceof Civilian) {
                this.victimSelector.add((Civilian) entity);
            }
            else if(entity instanceof Human) {
                this.victimSelector.add((Human)entity);
            }
            else if(entity instanceof Building) {
                Building b = (Building)entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                }
            }
            else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                
                if (! reportedRoads.contains(blockade.getPosition())) {
	                manager.addSendMessage(new MessageRoad((Road) this.world.getEntity(blockade.getPosition()), blockade, false));
	                reportedRoads.add(blockade.getPosition());
                }
            }
        }
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        System.out.println("\n");
        System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue());
        
        /* === -------- === *
         *   Basic actions  *
         * === -------- === */
        
        //Set<Civilian>
        System.out.println("Civilians perceived:" + ((BasicVictimSelector) this.victimSelector).civilianList.size());
        for (Civilian civ : ((BasicVictimSelector) this.victimSelector).civilianList) {
        	System.out.println("Civilian ID:" + civ.getID() + " pos:" + civ.getPosition() + " burriedness:" + civ.getBuriedness() + " damage:" + civ.getDamage());
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
	        
	        if(this.someoneOnBoard() || this.me.getDamage() >= 300) {
	        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
	        }
        }
        
        /* === ---------------------------------- === *
         *  Possible states and their implementation  *
         * === ---------------------------------- === */
        
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.EXPLORING)){
        	System.out.println("Exploring..");
        	if(currentTime < EXPLORE_TIME_LIMIT){
        		if(this.target == null || this.target.getValue() == this.location.getID().getValue()){
        			List<EntityID> result = this.exploreRouteSearcher.noTargetMove(currentTime, this.me);
        			this.target = result.get(result.size() - 1);
        		}
        		
        		//((ExploreRouteSearcher) this.exploreRouteSearcher).addVisitedEntity(this.location.getID()); // TODO: Fix this, calling this class this way its breaking the inheritance
        		return this.moveTarget(currentTime);
        	}
        	
        	this.stateMachine.setState(ActionStates.Ambulance.SELECT_NEW_TARGET);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_TARGET)){
        	System.out.println("Going to target..");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                return this.moveTarget(currentTime);
            }
        	
        	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.RESCUING)){
        	System.out.println("Rescuing ...");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if(victim.getBuriedness() > 0){
        		System.out.println("Burriedness: " + victim.getBuriedness() + " Damage: " + victim.getDamage() + " HP: " + victim.getHP());
        		return new ActionRescue(this, this.target);
        	}
        	
        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
        	Civilian civilian = (Civilian) victim;
            manager.addSendMessage(new MessageCivilian(civilian));
            this.victimSelector.remove(civilian);
            return new ActionLoad(this, this.target);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_REFUGE)){
        	System.out.println("Going to refugee..");
        	
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
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.SELECT_NEW_TARGET)){
        	System.out.println("Select new target..");
        	
            this.target = this.victimSelector.getNewTarget(currentTime);
            if(this.target == null) {
            	System.out.println("Problem getting target, return random move ...");
                return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
            }
            
            // Begin target basic processing
            do {
                Human victim = (Human) this.world.getEntity(this.target);
                if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_TARGET);
                    return this.moveTarget(currentTime);
                }
                
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
        
        System.out.println("Default behaviour, random walk ... ???");
        return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
    }
    
    public String toString(){
    	return "Ambulance:" + this.getID();
    }
}