package adk.sample.basic.tactics;

import adk.team.action.*;
import adk.team.tactics.TacticsAmbulance;
import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.graph.PositionUtil;
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
        return new ActionMove(this, path != null ? path : this.routeSearcher.noTargetMove(currentTime, this.me));
    }
}
