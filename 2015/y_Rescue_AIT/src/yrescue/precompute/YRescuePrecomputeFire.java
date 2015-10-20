package yrescue.precompute;

import adk.team.action.Action;
import adk.team.precompute.PrecomputeFire;
import adk.team.util.BuildingSelector;
import adk.team.util.RouteSearcher;
import adk.team.util.provider.BuildingSelectorProvider;
import adk.team.util.provider.RouteSearcherProvider;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.FireBrigade;
import rescuecore2.worldmodel.ChangeSet;
import yrescue.heatmap.HeatMap;
import yrescue.search.YRescueRouteSearcher;
import yrescue.util.YRescueBuildingSelector;

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

	@Override
	public Action failsafeThink(int currentTime, ChangeSet updateWorldData,
			MessageManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HeatMap initializeHeatMap() {
		// TODO Auto-generated method stub
		return null;
	}
}
