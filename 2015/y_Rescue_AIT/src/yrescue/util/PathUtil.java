package yrescue.util;

import java.util.List;

import adk.team.tactics.Tactics;
import adk.team.util.RouteSearcher;
import adk.team.util.provider.RouteSearcherProvider;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.worldmodel.EntityID;

public class PathUtil {

	private PathUtil() {	//cannot instantiate
	}
	
	
	/**
	 * Removes a building from the destination of a path if it is on fire
	 * @param agent
	 * @param path
	 */
	public static void makeSafePath(Tactics<?> agent, List<EntityID> path){
		if(path.size() > 1) {
			StandardEntity e = agent.world.getEntity(path.get(path.size() - 1 ));
    		if(e instanceof Building && ((Building) e).isOnFire()) {
    			Logger.info("Last path item is a building on fire, will remove it from path");
    			path.remove(path.size() - 1);
    		}
    	}
    	else {
    		Logger.info("Path is too short... won't change it");
    	}
	}
	
}
