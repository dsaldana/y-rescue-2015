package yrescue.problem.blockade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adk.team.tactics.Tactics;
import adk.team.util.graph.PositionUtil;
import adk.team.util.provider.WorldProvider;
import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.PoliceForce;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;
import yrescue.tactics.YRescueTacticsPolice;

public class BlockedAreaSelector {
	public Map<EntityID, BlockedArea> blockedAreas;
	YRescueTacticsPolice tactics;
	
	
	public BlockedAreaSelector(YRescueTacticsPolice tactics){
		this.tactics = tactics;
		this.blockedAreas = new HashMap<>();
	}
	
	public void add(BlockedArea a){
		blockedAreas.put(a.originID, a);
	}
	
	public void remove(BlockedArea a){
		blockedAreas.remove(a.originID);
		Logger.trace("Removed an item from blockedAreas: " + a );
		Logger.trace("Status of blockedAreas: " + blockedAreas );
	}
	
	public BlockedArea getNewTarget(int time) {
		Set<Area> areas = new HashSet<>();
		for(BlockedArea b : blockedAreas.values()){
			areas.add((Area)tactics.getWorld().getEntity(b.originID));
		}
	    Pair<Integer, Integer> position = new Pair<Integer, Integer>(tactics.me().getX(), tactics.me().getY());
        Area closest = PositionUtil.getNearTarget(this.tactics.getWorld(), position, areas);
        return closest == null ? null : blockedAreas.get(closest.getID()); 
    }

    public BlockedArea updateTarget(int time, BlockedArea target) {
    	
    	//if policeman has arrived in target position, then problem solved! (tolerance: 1 meter)
    	if (PositionUtil.equalsPoint(tactics.me.getX(), tactics.me.getY(), target.xOrigin, target.yOrigin, 1000)){	//milimeter is the unit of distance 
    		Logger.debug("Policeman has arrived to target " + target);
    		this.remove(target);
    		target = getNewTarget(time);
    	}
    	
        return target;
    }
}
