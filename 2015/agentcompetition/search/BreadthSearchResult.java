package search;

import java.util.List;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class BreadthSearchResult implements SearchResult {

	private List<EntityID> path;
	private StandardWorldModel world;
	
	public BreadthSearchResult(List<EntityID> thePath, StandardWorldModel worldModel){
		path = thePath;
		world = worldModel;
	}
	@Override
	public List<EntityID> getPath() {
		return path;
	}

	@Override
	public double getPathCost() {
		if (path == null) return Double.MAX_VALUE;
		
		// TODO return the actual length of the areas
		return path.size();
	}

	@Override
	/**
	 * Returns the centroid of the last Area in path
	 */
	public Pair<Integer, Integer> getFinalCoords() {
		
		if (path == null) return null;
		
		Area a = (Area) world.getEntity(path.get(path.size() - 1));
		
		return a.getLocation(world);
	}

}
