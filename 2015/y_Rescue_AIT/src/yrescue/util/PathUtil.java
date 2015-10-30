package yrescue.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	
	public static final String NODE_CACHE_FILE_NAME = "/tmp/breadth_first_all_to_all.txt";
	
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
	
	public static Map<EntityID, Map<EntityID, List<EntityID>>> getRouteCache(){
		Map<EntityID, Map<EntityID, List<EntityID>>> routeCache = new HashMap<EntityID, Map<EntityID, List<EntityID>>>();
		
		try (BufferedReader br = new BufferedReader(new FileReader(PathUtil.NODE_CACHE_FILE_NAME))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	String[] parts = line.split(" ");
		    	if(parts.length > 2){
		    		try{
		    			int from = Integer.parseInt(parts[0]);
		    			int to = Integer.parseInt(parts[1]);
		    			
		    			List<EntityID> nodeList = new LinkedList<EntityID>();
		    			for(int i = 2; i < parts.length; i++){
		    				int node = Integer.parseInt(parts[i]);
		    				nodeList.add(new EntityID(node));
		    			}
		    			
		    			Map<EntityID, List<EntityID>> lMap = new HashMap<EntityID, List<EntityID>>();
		    			lMap.put(new EntityID(to), nodeList);
		    			routeCache.put(new EntityID(to), lMap);
		    		}
		    		catch(Exception e){}
		    	}
		    }
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		return routeCache;
	}
	
}
