package search.ysearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;

import rescuecore2.log.Logger;
import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;
import search.SearchResult;


public class YSearchGraph {
	
	private SimpleWeightedGraph<YNode, YEdge> theGraph; 
	
	/**
	 * Maps Areas to their child YNodes 
	 */
	Map<EntityID, List<YNode>> childYNodes;
	
	/**
	 * Maps Areas to their respective centroids
	 */
	Map<EntityID, YNode> centroids;
	
	public YSearchGraph(StandardWorldModel worldModel){
		
		theGraph = new SimpleWeightedGraph<YNode, YEdge>(YEdge.class);
		Map<EntityID, List<YNode>> childYNodes = new HashMap<>();
		
		//traverses all areas, adding nodes in midpoint of geometric edge frontiers
		for(StandardEntity e : worldModel.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.BUILDING)){
			Area a = (Area) e;
			
			childYNodes.put(a.getID(), new ArrayList<YNode>());

			//traverses all edges, creating nodes in midpoint of frontiers
			for (Edge edge : a.getEdges()){
				if(! edge.isPassable() ) { 
					continue;	//ignores edges without neighbors
				}
				
				Edge frontier = findSmallestEdgeConnecting(a, (Area)worldModel.getEntity(edge.getNeighbour()));
				
				int nodeX = Math.abs(frontier.getEndX() + frontier.getStartX()) / 2;
				int nodeY = Math.abs(frontier.getEndY() + frontier.getStartY()) / 2;
				
				//YNode lies in midpoint of frontier
				YNode node = new YNode(nodeX, nodeY, a, (Area)worldModel.getEntity(edge.getNeighbour()));
				
				if (theGraph.containsVertex(node)){
					continue;	//skips if this node was already added
				}
				
				//adds the node to the graph and to the list of child of the current area
				addVertex(a, node);
				Logger.info(String.format("Added node %s on %s", node, frontier));
			}
			
			//adds a node in the centroid of the Area
			Pair<Integer, Integer> aPos = a.getLocation(worldModel);
			YNode centroid = new YNode(aPos.first(), aPos.second(), a, a);		//single parent, thus 'a' appears twice
			addVertex(a, centroid);
			centroids.put(a.getID(), centroid);
			Logger.info(String.format("Added centroid %s of %s", centroid, a));
		}
		
		//nodes were added, now go for the graph edges
		for (YNode head : theGraph.vertexSet()){
			for (YNode tail : theGraph.vertexSet()){
				if (head != tail && head.isInSameAreaOf(tail)){
					//found nodes in same area, put edge joining them
					YEdge yedge = new YEdge(head, tail);
					theGraph.addEdge(head, tail, yedge);
					theGraph.setEdgeWeight(yedge, yedge.getWeight());
				}
			}
		}//finished adding YEdges
	}

	/**
	 * Adds a YNode to the graph and as a child of the given area
	 * @param a
	 * @param node
	 */
	private void addVertex(Area a, YNode node) {
		theGraph.addVertex(node);
		childYNodes.get(a.getID()).add(node);
	}
	
	public List<YNode> getChildren(Area a){
		return childYNodes.get(a.getID());
	}
	
	public SearchResult shortestPath(EntityID start, EntityID... goals){
		return shortestPath(start, Arrays.asList(goals)); 
	}
	
	public SearchResult shortestPath(EntityID start, Collection<EntityID> goals){
		
		SearchResult best = null;
		
		YNode yStart = centroids.get(start);
		
		//calculates paths with area centroid as reference points
		for(EntityID goal : goals){
			YNode yGoal = centroids.get(goal);
			//DijkstraShortestPath<YNode, YEdge> pathFinder = new DijkstraShortestPath<YNode, YEdge>(theGraph, yStart, yGoal);
			
			SearchResult current = shortestPath(yStart, yGoal);
			
			if (best == null) best = current;
			else if (current != null && current.getPathCost() < best.getPathCost()){
				best = current;
			}
		}
		
		return best;
	}
	
	public SearchResult shortestPath(YNode start, YNode goal){
		DijkstraShortestPath<YNode, YEdge> pathFinder = new DijkstraShortestPath<YNode, YEdge>(theGraph, start, goal);
		
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
	
	public String dumpNodes(){
		String s = "";
		
		for(YNode n : theGraph.vertexSet()){
			s += n +"\n";
		}
		
		return s;
	}
	
	/**
	 * Returns the smallest edge on the frontier of two areas
	 * @param a1
	 * @param a2
	 * @return
	 */
	private static Edge findSmallestEdgeConnecting(Area a1, Area a2){
		
		//gets the edges that are the frontier in the two areas
		Edge frontier1_2 = findFrontierOfArea(a1.getEdges(), a2);
		Edge frontier2_1 = findFrontierOfArea(a2.getEdges(), a1);
		
		//calculates x and y lengths of edges to find which is smaller
		int frontier1_2_xLength = frontier1_2.getEndX() - frontier1_2.getStartX();
		int frontier1_2_yLength = frontier1_2.getEndY() - frontier1_2.getStartY();
		
		int frontier2_1_xLength = frontier2_1.getEndX() - frontier2_1.getStartX();
		int frontier2_1_yLength = frontier2_1.getEndY() - frontier2_1.getStartY();
		
		
		if (Math.hypot(frontier1_2_xLength, frontier1_2_yLength) < Math.hypot(frontier2_1_xLength, frontier2_1_yLength)){
			return frontier1_2;
		}
		
		return frontier2_1;
	}
	
	private static Edge findFrontierOfArea(List<Edge> edges, Area a) throws NoSuchElementException{
		
		for (Edge e : edges){
			if (e.getNeighbour() != null && e.getNeighbour().equals(a.getID())){
				return e;
			}
		}
		throw new NoSuchElementException(String.format("No frontier of %s was found in %s", a, edges));
		//return null;
	}
}
