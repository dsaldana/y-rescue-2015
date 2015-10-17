package yrescue.precompute;

import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.provider.RouteSearcherProvider;
import adk.team.util.provider.VictimSelectorProvider;
import adk.team.tactics.TacticsAmbulance;
import adk.team.action.Action;
import adk.team.precompute.PrecomputeAmbulance;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.worldmodel.ChangeSet;
import yrescue.search.YRescueRouteSearcher;
import yrescue.util.YRescueVictimSelector;

public class YRescuePrecomputeAmbulance extends PrecomputeAmbulance implements RouteSearcherProvider, VictimSelectorProvider {

    public VictimSelector victimSelector;

    public RouteSearcher routeSearcher;

    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.victimSelector = new YRescueVictimSelector(this);
        this.routeSearcher = new YRescueRouteSearcher(this, new RouteManager(this.world));
    }

    @Override
    public VictimSelector getVictimSelector() {
        return this.victimSelector;
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
}
