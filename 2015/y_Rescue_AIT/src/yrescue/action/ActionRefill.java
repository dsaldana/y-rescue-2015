package yrescue.action;

import adk.team.action.Action;
import rescuecore2.log.Logger;
import rescuecore2.messages.Message;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKMove;
import rescuecore2.worldmodel.EntityID;
import yrescue.tactics.YRescueTacticsFire;

import java.util.ArrayList;
import java.util.List;

public class ActionRefill extends Action {
    
    private List<EntityID> path;
    private YRescueTacticsFire tactics;
    private StandardEntity destination;
    
    public ActionRefill(YRescueTacticsFire tactics, List<StandardEntity> refugeIDs, List<StandardEntity> hydrantIDs){
        super(tactics);
        this.tactics = tactics;
        
        Logger.trace("ActionRefill. refuges=" + refugeIDs + ", hydrants=" + hydrantIDs);
        
        // Search for the closest water source
        int dist = Integer.MAX_VALUE;
        List<EntityID> pathTemp = new ArrayList<EntityID>();
        for(StandardEntity next: refugeIDs){
        	pathTemp = tactics.routeSearcher.getPath(tactics.getCurrentTime(), tactics.me().getPosition(), next.getID());
        	if(pathTemp.size() < dist){
        		dist = pathTemp.size();
        		this.path = pathTemp;
        		this.destination = next;
        	}
        }
        if(hydrantIDs != null){
	        for(StandardEntity next: hydrantIDs){
	        	pathTemp = tactics.routeSearcher.getPath(tactics.getCurrentTime(), tactics.me.getPosition(), next.getID());
	        	if (pathTemp == null){
	        		Logger.warn("\nnull path to hydrant! " + next + ". It will be ignored.\n");
	        	}
	        	else if(pathTemp.size() < dist){
	        		dist = pathTemp.size();
	        		this.path = pathTemp;
	        		this.destination = next;
	        	}
	        }
        }
        Logger.trace("Selected destination to refill: " + this.destination);
    }
    

    public List<EntityID> getPath() {
        return this.path;
    }
    
    @Override
    public Message getCommand(EntityID agentID, int time) {
    	tactics.targetHydrant = this.destination.getID();
        return new AKMove(agentID, time, this.path);
    }
}