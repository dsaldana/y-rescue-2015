package search;

import java.util.List;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

public interface SearchResult {
	/**
	 * Gets the found path (a list of EntityID) 
	 * @return list of EntityID or null if path was not found
	 */
	public List<EntityID> getPath();
	
	/**
	 * Returns the calculated path cost
	 * @return
	 */
	public double getPathCost();
	
	/**
	 * Returns the final coordinates of the calculated path
	 * @return x,y
	 */
	public Pair<Integer, Integer> getFinalCoords();
}
