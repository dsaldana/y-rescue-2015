package yrescue.search;

import rescuecore2.worldmodel.EntityID;
import rescuecore2.worldmodel.Entity;
import rescuecore2.misc.collections.LazyMap;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.standard.entities.Area;
import yrescue.util.RouteCacheKey;

public final class PreComputeSearch {
	private Map<EntityID, Set<EntityID>> graph;
	private Set<EntityID> buildingSet;
	
	/**
	 * Construct a new PreComputeSearch.
	 * 
	 * @param world
	 *            The world model to construct the neighbourhood graph from.
	 */
	public PreComputeSearch(StandardWorldModel world) {
		Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
			@Override
			public Set<EntityID> createValue() {
				return new HashSet<EntityID>();
			}
		};
		buildingSet=new HashSet<EntityID>();
		for (Entity next : world) {
			if (next instanceof Area) {
				Collection<EntityID> areaNeighbours = ((Area) next).getNeighbours();
				neighbours.get(next.getID()).addAll(areaNeighbours);
				if(next instanceof Building)
					buildingSet.add(next.getID());
			}
		}
		setGraph(neighbours);
	}

	/**
	 * Construct a new ConnectionGraphSearch.
	 * 
	 * @param graph
	 *            The connection graph in the form of a map from EntityID to the set of neighbouring EntityIDs.
	 */
	public PreComputeSearch(Map<EntityID, Set<EntityID>> graph) {
		setGraph(graph);
	}

	/**
	 * Set the neighbourhood graph.
	 * 
	 * @param newGraph
	 *            The new neighbourhood graph.
	 */
	public void setGraph(Map<EntityID, Set<EntityID>> newGraph) {
		this.graph = newGraph;
	}

	/**
	 * Get the neighbourhood graph.
	 * 
	 * @return The neighbourhood graph.
	 */
	public Map<EntityID, Set<EntityID>> getGraph() {
		return graph;
	}

	/**
	 * Do a breadth first search from one location to the all the graph.
	 * 
	 * @param start
	 *            The location we start at.
	 * @return The costs from the start to all nodes.
	 */
	public Map<RouteCacheKey, Integer> computeAllCosts(EntityID start, Map<RouteCacheKey, Integer> mapDistanceCache) {
		//Map<RouteCacheKey, Integer> mapDistanceCache = new HashMap<RouteCacheKey, Integer>();
		
		List<EntityID> open = new LinkedList<EntityID>();
		EntityID next = null;
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		ancestors.put(start, start);
		
		do {
			next = open.remove(0);
			
			Collection<EntityID> neighbours = graph.get(next);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (!ancestors.containsKey(neighbour)) {
					open.add(neighbour);
					ancestors.put(neighbour, next);
				}
			}
			
			// Walk back from actual node to start
			int cost = 0;
			EntityID current = next;
			do {
				cost++;
				current = ancestors.get(current);
				if (current == null) {
					System.out.println("Found a node with no ancestor! Something is broken.");
					break;
				}
			} while (current != start);
			mapDistanceCache.put(new RouteCacheKey(start.getValue(), next.getValue()), cost);
			mapDistanceCache.put(new RouteCacheKey(next.getValue(), start.getValue()), cost);
		} while (!open.isEmpty());
		
		return mapDistanceCache;
	}
	
	/**
	 * Do a breadth first search from one location to the all the graph.
	 * 
	 * @param start
	 *            The location we start at.
	 * @return The costs from the start to all nodes.
	 */
	public Map<RouteCacheKey, List<EntityID>> computeAllPaths(EntityID start, Map<RouteCacheKey, List<EntityID>> integratedMap) {
		//Map<RouteCacheKey, List<EntityID>> mapDistanceCache = new HashMap<RouteCacheKey, List<EntityID>>();
		
		List<EntityID> open = new LinkedList<EntityID>();
		EntityID next = null;
		Map<EntityID, EntityID> ancestors = new HashMap<EntityID, EntityID>();
		open.add(start);
		ancestors.put(start, start);
		
		do {
			next = open.remove(0);
			
			Collection<EntityID> neighbours = graph.get(next);
			if (neighbours.isEmpty()) {
				continue;
			}
			for (EntityID neighbour : neighbours) {
				if (!ancestors.containsKey(neighbour)) {
					open.add(neighbour);
					ancestors.put(neighbour, next);
				}
			}
			
			// Walk back from actual node to start
			EntityID current = next;
			//StringBuffer sb = new StringBuffer();
			List<EntityID> path = new LinkedList<EntityID>(); 
			do {
				//sb.insert(0, " ");
				//sb.insert(0, current.getValue());
				path.add(0, current);
				current = ancestors.get(current);
				if (current == null) {
					path.clear();
					System.out.println("Found a node with no ancestor! Something is broken.");
					break;
				}
			} while (current != start);
			
			if(!path.isEmpty()){
				integratedMap.put(new RouteCacheKey(start.getValue(), next.getValue()), path);
			}
			
		} while (!open.isEmpty());
		
		return integratedMap;
	}

}
