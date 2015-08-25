package yrescue.precompute;

import adk.team.util.ImpassableSelector;
import adk.team.util.RouteSearcher;
import adk.team.precompute.PrecomputePolice;
import adk.team.util.graph.RouteManager;
import adk.team.util.provider.ImpassableSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.PoliceForce;
import yrescue.util.YRescueImpassableSelector;
import yrescue.util.YRescueRouteSearcher;

public class YRescuePrecomputePolice extends PrecomputePolice implements RouteSearcherProvider, ImpassableSelectorProvider {

    public ImpassableSelector impassableSelector;

    public RouteSearcher routeSearcher;

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.routeSearcher = new YRescueRouteSearcher(this, new RouteManager(this.world));
        this.impassableSelector = new YRescueImpassableSelector(this);
    }

    @Override
    public RouteSearcher getRouteSearcher() {
        return this.routeSearcher;
    }

    @Override
    public ImpassableSelector getImpassableSelector() {
        return this.impassableSelector;
    }

    @Override
    public String getTacticsName() {
    	return "Y-Rescue pre-processing";
    }
}
