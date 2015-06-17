package search.ysearch;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jgrapht.alg.DijkstraShortestPath;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import search.SearchResult;
import search.SearchStrategy;

public class YSearch implements SearchStrategy {
	
	YGraphWrapper graphWrapper;
	StandardWorldModel world;
	
	public YSearch(YGraphWrapper theGraphWrapper, StandardWorldModel theWorld){
		graphWrapper = theGraphWrapper;
		world = theWorld;
	}

	@Override
	public SearchResult shortestPath(Human origin, EntityID... goals) {
		return shortestPath(origin, Arrays.asList(goals));
	}

	@Override
	public SearchResult shortestPath(Human origin, Collection<EntityID> goals) {
		// TODO expand graph and retract it later
		Area originArea = (Area) world.getEntity(origin.getPosition());
		YNode start = new YNode(
			origin.getX(), origin.getY(), originArea, originArea
		);
		
		//temporarily expands graph, performs the search and un-expand it
		graphWrapper.expand(start);
		SearchResult result = shortestPath(start, goals);
		graphWrapper.unexpand(start);
		
		return result;
	}

	@Override
	public SearchResult shortestPath(EntityID start, EntityID... goals){
		return shortestPath(start, Arrays.asList(goals)); 
	}
	
	@Override
	public SearchResult shortestPath(EntityID start, Collection<EntityID> goals){
		
		YNode yStart = graphWrapper.getCentroids().get(start);
		List<YNode> yGoals = new LinkedList<>();
		
		
		//will calculate paths with area centroid as reference points
		for(EntityID goal : goals){
			yGoals.add(graphWrapper.getCentroids().get(goal));
		}
		
		return shortestPath(yStart, yGoals);
	}
	
	public SearchResult shortestPath(YNode start, Collection<YNode> goals){
		
		SearchResult best = null;
		
		//calculates paths with area centroid as reference points
		for(YNode goal : goals){
			
			SearchResult current = shortestPath(start, goal);
			
			if (best == null) best = current;
			else if (current != null && current.getPathCost() < best.getPathCost()){
				best = current;
			}
		}
		
		return best;
	}
	
	public SearchResult shortestPath(YNode start, YNode goal){
		DijkstraShortestPath<YNode, YEdge> pathFinder = new DijkstraShortestPath<YNode, YEdge>(graphWrapper.getGraph(), start, goal);
		
		return new YSearchResult(pathFinder.getPath());
		
		/*
		List<YEdge> path = pathFinder.getPathEdgeList();
		
		
		if (path == null) return null;
		
		List<EntityID> idPath = new LinkedList<EntityID>();
		Map<EntityID, Boolean> inserted = new HashMap<EntityID, Boolean>();

		//traverses each YEdge of path, building a list of EntityIDs without duplicates 
		for (YEdge yedge : path){
			if (! inserted.containsKey(yedge.getParentArea().getID())){
				inserted.put(yedge.getParentArea().getID(), true);
				idPath.add(yedge.getParentArea().getID());
			}
		}
		
		return idPath;
		*/
	}

}
