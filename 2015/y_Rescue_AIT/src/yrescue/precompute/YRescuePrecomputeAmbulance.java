package yrescue.precompute;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import adk.sample.basic.util.BasicRouteSearcher;
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
import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.AmbulanceTeam;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Refuge;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.ChangeSet;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;
import yrescue.heatmap.HeatMap;
import yrescue.search.YRescueRouteSearcher;
import yrescue.util.PathUtil;
import yrescue.util.YRescueVictimSelector;

public class YRescuePrecomputeAmbulance extends PrecomputeAmbulance implements RouteSearcherProvider, VictimSelectorProvider {

    public VictimSelector victimSelector;
    public RouteSearcher routeSearcher;
    private List<EntityID> allNodes;
    
    
    @Override
    public void preparation(Config config, MessageManager messageManager) {
        this.victimSelector = new YRescueVictimSelector(this);
        this.routeSearcher = new BasicRouteSearcher(this);
        
        try{
    		File file = new File(PathUtil.NODE_CACHE_FILE_NAME);
    		if(file.delete()){
    			System.out.println(file.getName() + " is deleted!");
    		}else{
    			System.out.println("Delete operation is failed.");
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
        
        allNodes = new LinkedList<EntityID>();
        for (StandardEntity next : this.world.getAllEntities()) {
            if (next instanceof Area) {
                allNodes.add(next.getID());
            }
        }
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
		System.out.println("FAILSAFETHINK PRECOMPUTE AMBULANCE");
		return null;
	}

	@Override
	public HeatMap initializeHeatMap() {
		System.out.println("HEATMAP PRECOMPUTE AMBULANCE");
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Action think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {
		System.out.println("PRECOMPUTE THINK AMBULANCE");
		
		PrintWriter writer;
		try {
			writer = new PrintWriter(PathUtil.NODE_CACHE_FILE_NAME, "UTF-8");
			
	        for(EntityID ent1 : allNodes){
	        	for(EntityID ent2 : allNodes){
	        		if(ent1.getValue() == ent2.getValue()) continue;
	        		StringBuilder sb = new StringBuilder();
	        		
	        		sb.append(ent1.getValue());
	        		sb.append(" ");
	        		sb.append(ent2.getValue());
	        		sb.append(" ");
	        		
	        		List<EntityID> path = this.routeSearcher.getPath(currentTime, ent1, ent2);
	        		if(path != null && path.size() > 0){
		        		for(EntityID pathEnt : path){
		        			sb.append(pathEnt.getValue());
			        		sb.append(" ");
		        		}
		        		writer.write(sb.toString());
	        		}
	        	}
	        }
		
			writer.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new ActionRest(this);
	}
}
