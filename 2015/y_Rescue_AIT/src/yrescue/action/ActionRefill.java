package yrescue.action;

import adk.team.action.Action;
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
        
        // Search for the closest water source
        double dist = -1;
        List<EntityID> pathTemp = new ArrayList<EntityID>();
        for(StandardEntity next: refugeIDs){
        	pathTemp = tactics.routeSearcher.getPath(tactics.getCurrentTime(), tactics.me().getPosition(), next.getID());
        	if(dist == -1 || dist < pathTemp.size()){
        		dist = pathTemp.size();
        		this.path = pathTemp;
        		this.destination = next;
        	}
        }
        for(StandardEntity next: hydrantIDs){
        	pathTemp = tactics.routeSearcher.getPath(tactics.getCurrentTime(), tactics.getOwnerID(), next.getID());
        	if(dist == -1 || dist < pathTemp.size()){
        		dist = pathTemp.size();
        		this.path = pathTemp;
        		this.destination = next;
        	}
        }
    }
    

    public List<EntityID> getPath() {
        return this.path;
    }
    
    @Override
    public Message getCommand(EntityID agentID, int time) {
        return new AKMove(agentID, time, this.path);
    }
}