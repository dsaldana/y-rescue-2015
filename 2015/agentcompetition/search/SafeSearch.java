package search;

import java.util.Collection;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Human;
import rescuecore2.worldmodel.EntityID;

public class SafeSearch implements SearchStrategy {
	SearchStrategy smart;
	SearchStrategy failSafe;
	
	public SafeSearch(SearchStrategy smart, SearchStrategy failSafe) {
		this.smart = smart;
		this.failSafe = failSafe;
	}

	@Override
	public SearchResult shortestPath(Human origin, EntityID... goals) {
		if (smart != null){
			try {
				return smart.shortestPath(origin, goals);
			} catch (Exception e) {
				Logger.error("Smart search error. Using failsafe search", e);
			}
		}
		return failSafe.shortestPath(origin, goals);
	}

	@Override
	public SearchResult shortestPath(Human origin, Collection<EntityID> goals) {
		if (smart != null){
			try {
				return smart.shortestPath(origin, goals);
			} catch (Exception e) {
				Logger.error("Smart search error. Using failsafe search", e);
			}
		}
		return failSafe.shortestPath(origin.getPosition(), goals);
	}

	@Override
	public SearchResult shortestPath(EntityID start, EntityID... goals) {
		if (smart != null){
			try {
				return smart.shortestPath(start, goals);
			} catch (Exception e) {
				Logger.error("Smart search error. Using failsafe search", e);
			}
		}
		return failSafe.shortestPath(start, goals);
	}

	@Override
	public SearchResult shortestPath(EntityID start, Collection<EntityID> goals) {
		if (smart != null){
			try {
				return smart.shortestPath(start, goals);
			} catch (Exception e) {
				Logger.error("Smart search error. Using failsafe search", e);
			}
		}
		return failSafe.shortestPath(start, goals);
	}

}
