package search;

import java.util.List;

import rescuecore2.misc.Pair;
import rescuecore2.worldmodel.EntityID;

public interface SearchResult {
	public List<EntityID> getPath();
	public double getPathCost();
	public Pair<Integer, Integer> getFinalCoords();
}
