package yrescue.precompute;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import adk.team.util.RouteSearcher;
import adk.team.util.VictimSelector;
import adk.team.util.provider.RouteSearcherProvider;
import adk.team.util.provider.VictimSelectorProvider;
import adk.team.tactics.TacticsAmbulance;
import adk.team.action.Action;
import adk.team.action.ActionRest;
import adk.team.precompute.PrecomputeAmbulance;
import adk.team.util.graph.RouteManager;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.worldmodel.ChangeSet;
import yrescue.heatmap.HeatMap;
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

	@Override
	public HeatMap initializeHeatMap() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		PrintWriter writer;
		try {
			writer = new PrintWriter("/tmp/ambulance_pre_compute.txt", "UTF-8");
			writer.println("The first line");
			writer.println("The second line");
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ActionRest(this);
	}
}
