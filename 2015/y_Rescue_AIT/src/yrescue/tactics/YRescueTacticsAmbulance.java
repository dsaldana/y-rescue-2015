package yrescue.tactics;

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
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import comlib.manager.MessageManager;
import comlib.message.information.MessageAmbulanceTeam;
import comlib.message.information.MessageBuilding;
import comlib.message.information.MessageCivilian;
import comlib.message.information.MessageFireBrigade;
import comlib.message.information.MessagePoliceForce;
import comlib.message.information.MessageRoad;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

public class YRescueTacticsAmbulance extends BasicTacticsAmbulance {

	protected final int EXPLORE_TIME_STEP_TRESH = 10;
	
    @Override
    public String getTacticsName() {
        return "Y-Rescue Ambulance";
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
                manager.addSendMessage(new MessageRoad((Road)this.world.getEntity(blockade.getPosition()), blockade, false));
            }
        }
    }
    
    @Override
    public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
        this.organizeUpdateInfo(currentTime, updateWorldData, manager);
        
        System.out.println("Y-Rescue Time:" + currentTime + " Id:" + this.agentID.getValue());
        
        // Basic state check
        if(this.me.getBuriedness() > 0) {
            this.target = null;
            return new ActionRescue(this, this.agentID);
        }
        
        // Refugee actions
        if(this.location instanceof Refuge) {
            if(this.someoneOnBoard()) {
            	this.target = null;
                return new ActionUnload(this);
            }
            if(this.me.getDamage() > 0) {
            	this.target = null;
                return new ActionRest(this);
            }
        }
        
        // Movement conditions to shelter
        if(this.someoneOnBoard() || this.me.getDamage() >= 50) {
            return this.moveRefuge(currentTime);
        }
        
        if(currentTime < EXPLORE_TIME_STEP_TRESH){
        	return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        
        // Selecting and switching target
        this.target = this.target == null ? this.victimSelector.getNewTarget(currentTime) : this.victimSelector.updateTarget(currentTime, this.target);
        if(this.target == null) {
            return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
        }
        
        // Begin rescue
        do {
            Human victim = (Human) this.world.getEntity(this.target);
            if (victim.getPosition().getValue() != this.location.getID().getValue()) {
                return this.moveTarget(currentTime);
            }
            
            if (victim.getBuriedness() > 0) {
                return new ActionRescue(this, this.target);
            }
            
            // In the case of rescue already
            if (victim instanceof Civilian) {
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
        
        return new ActionMove(this, this.routeSearcher.noTargetMove(currentTime, this.me));
    }
}