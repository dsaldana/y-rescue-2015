package adk.sample.basic.tactics;

import adk.team.action.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.ImpassableSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import adk.team.util.provider.VictimSelectorProvider;
import comlib.manager.MessageManager;
import comlib.message.information.*;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

public abstract class BasicTacticsAmbulance extends TacticsAmbulance implements RouteSearcherProvider, VictimSelectorProvider {

    public VictimSelector victimSelector;
    public RouteSearcher routeSearcher;

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.victimSelector = this.initVictimSelector();
        this.routeSearcher = this.initRouteSearcher();
    }

    public abstract VictimSelector initVictimSelector();

    public abstract RouteSearcher initRouteSearcher();

    @Override
    public VictimSelector getVictimSelector() {
        return this.victimSelector;
    }

    @Override
    public RouteSearcher getRouteSearcher() {
        return this.routeSearcher;
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

    public abstract void organizeUpdateInfo(int currentTime, ChangeSet updateWorldInfo, MessageManager manager);

    public boolean someoneOnBoard() {
        return this.target != null && ((Human)this.world.getEntity(this.target)).getPosition().getValue() == this.me.getID().getValue();
    }

    public Action moveRefuge(int currentTime) {
        Refuge result = PositionUtil.getNearTarget(this.world, this.me, this.getRefuges());
        List<EntityID> path = this.routeSearcher.getPath(currentTime, this.me, result);
        return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime, this.me));
    }

    public Action moveTarget(int currentTime) {
        List<EntityID> path = getPathToTarget(currentTime);
        return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime, this.me));
    }

	/**
	 * @param currentTime
	 * @param path
	 * @return
	 */
	protected List<EntityID> getPathToTarget(int currentTime) {
		List<EntityID> path = null;
		
		if(this.target != null) {
        	if(this.world.getEntity(this.target) instanceof Human){
        		Human humanTarget = (Human) this.world.getEntity(this.target);
        		path = this.routeSearcher.getPath(currentTime, this.me.getPosition(), humanTarget.getPosition());
        	}
        	else{
        		path = this.routeSearcher.getPath(currentTime, this.me.getPosition(), this.target);
        	}
        }
		return path;
	}
}
