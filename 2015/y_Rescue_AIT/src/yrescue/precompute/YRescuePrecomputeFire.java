package yrescue.precompute;

import adk.team.precompute.PrecomputeFire;
import adk.team.util.BuildingSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.provider.BuildingSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.FireBrigade;
import yrescue.util.YRescueBuildingSelector;
import yrescue.util.YRescueRouteSearcher;

public class YRescuePrecomputeFire extends PrecomputeFire implements RouteSearcherProvider, BuildingSelectorProvider {

    public BuildingSelector buildingSelector;

    public RouteSearcher routeSearcher;

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.buildingSelector = new YRescueBuildingSelector(this);
        this.routeSearcher = new YRescueRouteSearcher(this, new RouteManager(this.world));
    }

    @Override
    public BuildingSelector getBuildingSelector() {
        return this.buildingSelector;
    }

    @Override
    public RouteSearcher getRouteSearcher() {
        return this.routeSearcher;
    }

    @Override
    public String getTacticsName() {
    	return "Y-Rescue pre-processing";
    }
}
