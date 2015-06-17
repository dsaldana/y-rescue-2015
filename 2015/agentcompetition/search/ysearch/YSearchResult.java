package search.ysearch;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgrapht.GraphPath;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;
import search.SearchResult;

public class YSearchResult implements SearchResult {

	GraphPath<YNode, YEdge> path;
	
	public YSearchResult(GraphPath<YNode, YEdge> foundPath){
		this.path = foundPath; 
	}
	
	@Override
	/**
	 * Returns the path as required by the simulator's sendMove
	 */
	public List<EntityID> getPath() {
		if (path == null) return null;
		
		List<EntityID> idPath = new LinkedList<EntityID>();
		Map<EntityID, Boolean> inserted = new HashMap<EntityID, Boolean>();

		//traverses each YEdge of path, building a list of EntityIDs without duplicates 
		for (YEdge yedge : path.getEdgeList()){
			if (! inserted.containsKey(yedge.getParentArea().getID())){
				inserted.put(yedge.getParentArea().getID(), true);
				idPath.add(yedge.getParentArea().getID());
			}
		}
		
		return idPath;
	}

	@Override
	public Pair<Integer, Integer> getFinalCoords() {
		if (path == null)	return null;
		
		return path.getEndVertex().getCoords();
	}

	@Override
	public double getPathCost() {
		if (path == null)	return Double.MAX_VALUE;
		
		return path.getWeight();
	}

}
