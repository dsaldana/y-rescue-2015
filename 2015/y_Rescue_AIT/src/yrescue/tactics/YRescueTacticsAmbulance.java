package yrescue.tactics;

import java.util.List;

import rescuecore2.config.Config;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Civilian;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;
import yrescue.message.information.MessageBlockedArea;
import yrescue.search.ExploreRouteSearcher;
import yrescue.statemachine.ActionStates;
import yrescue.statemachine.StateMachine;
import adk.sample.basic.event.BasicAmbulanceEvent;
import adk.sample.basic.event.BasicCivilianEvent;
import adk.sample.basic.event.BasicFireEvent;
import adk.sample.basic.event.BasicPoliceEvent;
import adk.sample.basic.tactics.BasicTacticsAmbulance;
import adk.sample.basic.util.BasicRouteSearcher;
import adk.sample.basic.util.BasicVictimSelector;
import adk.team.action.Action;
import adk.team.action.ActionLoad;
import adk.team.action.ActionMove;
import adk.team.action.ActionRescue;
import adk.team.action.ActionRest;
import adk.team.action.ActionUnload;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.graph.RouteManager;

import comlib.manager.MessageManager;
import comlib.message.information.MessageAmbulanceTeam;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessagePoliceForce;

public class YRescueTacticsAmbulance extends BasicTacticsAmbulance {

	protected final int EXPLORE_TIME_STEP_TRESH = 10;
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
    	
    	
        for (EntityID next : updateWorldInfo.getChangedEntities()) {
            StandardEntity entity = this.getWorld().getEntity(next);
            /*if(entity instanceof Civilian) {
                this.victimSelector.add((Civilian) entity);
            }
            else*/
            if(entity instanceof Human) {
            	Human h = (Human) entity;
            	if (h.isBuriednessDefined() && h.getBuriedness() > 0) {
            		Logger.trace(String.format("Adding human %s to victimSelector. b'ness: %d, dmg: %d ", h, h.getBuriedness(), h.getDamage()));
            		
            		if(h instanceof Civilian){
            			this.victimSelector.add((Civilian) h);
            			//manager.addSendMessage(new MessageCivilian((Civilian) h));
            		}
            		else {
            			this.victimSelector.add(h);
            			/*
            			if (h instanceof FireBrigade){
            				manager.addSendMessage(new MessageFireBrigade((FireBrigade) h, MessageAmbulanceTeam.ACTION_RESCUE, h.getPosition()));
            			}
            			else if (h instanceof AmbulanceTeam) {
            				manager.addSendMessage(new MessageAmbulanceTeam((AmbulanceTeam) h, MessageAmbulanceTeam.ACTION_RESCUE, h.getPosition()));
            				
            			}
            			else if (h instanceof PoliceForce){
            				manager.addSendMessage(new MessagePoliceForce((PoliceForce) h, MessageAmbulanceTeam.ACTION_RESCUE, h.getPosition()));
            			}*/
            		}
            	}
            }
            else if(entity instanceof Building) {
                Building b = (Building)entity;
                if(b.isOnFire()) {
                    manager.addSendMessage(new MessageBuilding(b));
                }
            }
            /*else if(entity instanceof Blockade) {
                Blockade blockade = (Blockade) entity;
                
                if (! reportedRoads.contains(blockade.getPosition())) {
	                manager.addSendMessage(new MessageRoad((Road) this.world.getEntity(blockade.getPosition()), blockade, false));
	                reportedRoads.add(blockade.getPosition());
                }
            }*/
        }
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        Logger.info(String.format("----------- Timestep %d --------------", currentTime));
        //System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue());
        
        /* === -------- === *
         *   Basic actions  *
         * === -------- === */
        
        //Set<Civilian>
        BasicVictimSelector bvm = (BasicVictimSelector) this.victimSelector;
        Logger.debug("Buried ppl: " + bvm.civilianList.size() + " civilians and " + bvm.agentList.size() + " agents");
        for (Civilian civ : bvm.civilianList) {
        	Logger.debug(String.format(
    			"Civilian ID: %s, pos: %s, b'ness: %d, dmg : %d", 
    			civ.getID(), civ.getPosition(), + civ.getBuriedness(), civ.getDamage()
    		));
        }
        
        for (Human h : bvm.agentList) {
        	Logger.debug(String.format(
    			"Agent ID: %s, pos: %s, b'ness: %d, dmg : %d", 
    			h.getID(), h.getPosition(), + h.getBuriedness(), h.getDamage()
    		));
        }
        
        if(this.me.getBuriedness() > 0) {
            this.target = null;
            Logger.trace("I'm trying to rescue myself");
            return new ActionRescue(this, this.agentID);
        }
        
        if (this.tacticsAgent.stuck (currentTime)){
    		//manager.addSendMessage(new MessageRoad((Road)this.location(), BlockadeUtil.getClosestBlockadeInMyRoad(this), false) );
        	manager.addSendMessage(new MessageBlockedArea(this, this.location.getID()));
        	Logger.trace("I'm blocked. Added a MessageBlockedArea");
    		return new ActionRest(this);	//does nothing...
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
        	Logger.debug("Exploring..");
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
        	Logger.debug("Going to target..");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                return this.moveTarget(currentTime);
            }
        	
        	this.stateMachine.setState(ActionStates.Ambulance.RESCUING);
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.RESCUING)){
        	Logger.debug("Rescuing ...");
        	Human victim = (Human) this.world.getEntity(this.target);
        	if(victim.getBuriedness() > 0){
        		Logger.debug("B'ness: " + victim.getBuriedness() + " dmg: " + victim.getDamage() + " HP: " + victim.getHP());
        		return new ActionRescue(this, this.target);
        	}
        	
        	this.stateMachine.setState(ActionStates.Ambulance.GOING_TO_REFUGE);
        	Civilian civilian = (Civilian) victim;
            manager.addSendMessage(new MessageCivilian(civilian));
            this.victimSelector.remove(civilian);
            
            if(! (this.location instanceof Refuge)) {	//prevents loading targets at the refuge
            	return new ActionLoad(this, this.target);
        	}
        }
        if(this.stateMachine.getCurrentState().equals(ActionStates.Ambulance.GOING_TO_REFUGE)){
        	Logger.debug("Going to refugee..");
        	
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
                    if (! (this.location instanceof Refuge)){
                    	return new ActionLoad(this, this.target);
                    }
                    
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
        
        Logger.debug("Default behaviour, random walk ... ???");
        return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
    }
    
    public String toString(){
    	return "Ambulance:" + this.getID();
    }
}