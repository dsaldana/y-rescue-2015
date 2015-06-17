package search;

import java.util.Collection;

import agent.platoon.AbstractPlatoon;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;

public interface SearchStrategy {
	public SearchResult shortestPath(Human origin, EntityID... goals);
	public SearchResult shortestPath(Human origin, Collection<EntityID> goals);
	
	public SearchResult shortestPath(EntityID start, EntityID... goals);
	public SearchResult shortestPath(EntityID start, Collection<EntityID> goals);

}
