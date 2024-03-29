package search.ysearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import util.Geometry;


public class YGraphWrapper {
	
	private SimpleWeightedGraph<YNode, YEdge> theGraph; 
	
	/**
	 * Maps Areas to their child YNodes 
	 */
	Map<EntityID, List<YNode>> childYNodes;
	
	/**
	 * Maps Areas to their respective centroids
	 */
	private Map<EntityID, YNode> centroids;
	
	public YGraphWrapper(StandardWorldModel worldModel){
		
		theGraph = new SimpleWeightedGraph<YNode, YEdge>(YEdge.class);
		childYNodes = new HashMap<EntityID, List<YNode>>();
		centroids = new HashMap<>();
		
		//traverses all areas, adding nodes in midpoint of geometric edge frontiers
		for(StandardEntity e : worldModel.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.BUILDING, StandardEntityURN.REFUGE, StandardEntityURN.HYDRANT)){
			Area a = (Area) e;
			
			childYNodes.put(a.getID(), new ArrayList<YNode>());

			//traverses all edges, creating nodes in midpoint of frontiers
			for (Edge edge : a.getEdges()){
				if(! edge.isPassable() ) { 
					continue;	//ignores edges without neighbors
				}
				
				Edge frontier = Geometry.findSmallestEdgeConnecting(a, (Area)worldModel.getEntity(edge.getNeighbour()));
				
				int nodeX = Math.abs(frontier.getEndX() + frontier.getStartX()) / 2;
				int nodeY = Math.abs(frontier.getEndY() + frontier.getStartY()) / 2;
				
				//YNode lies in midpoint of frontier
				YNode node = new YNode(nodeX, nodeY, a, (Area)worldModel.getEntity(edge.getNeighbour()));
				
				if (theGraph.containsVertex(node)){
					continue;	//skips if this node was already added
				}
				
				//adds the node to the graph and to the list of child of the current area
				addVertex(a, node);
				Logger.debug(String.format("Added node %s on %s", node, frontier));
			}
			
			//adds a node in the centroid of the Area
			Pair<Integer, Integer> aPos = a.getLocation(worldModel);
			YNode centroid = new YNode(aPos.first(), aPos.second(), a, a);		//single parent, thus 'a' appears twice
			addVertex(a, centroid);
			centroids.put(a.getID(), centroid);
			Logger.debug(String.format("Added centroid %s of %s", centroid, a));
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
	
	public SimpleWeightedGraph<YNode, YEdge> getGraph() {
		return theGraph;
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
	
	/**
	 * Expands the graph with the given node by
	 * adding edges to all vertices in its parent area (must be a single one)
	 * @param node
	 */
	public void expand(YNode node){
		
		//node must have a single parent, throws exception otherwise
		if(node.getParentAreas().first() != node.getParentAreas().second()){
			throw new IllegalArgumentException(
				String.format(
					"%s has different parents: %s and %s", node, 
					node.getParentAreas().first(), node.getParentAreas().second()
				)
			);
		}
		
		Logger.debug("Will expand node "+ node);
		
		//adds the vertex and creates edges between all ynodes in parent area
		Area parentArea = node.getParentAreas().first();
		//Logger.debug("Parent Area: "+ parentArea);
		addVertex(parentArea, node);
		//Logger.debug("childYNodes of area: " + childYNodes.get(parentArea));
		for (YNode neighbor : childYNodes.get(parentArea.getID())){
			YEdge e = new YEdge(node, neighbor);
			
			if (neighbor.equals(node)) continue;		//self-loops are not allowed
			
			theGraph.addEdge(node, neighbor, e);
			theGraph.setEdgeWeight(e, e.getWeight());
		}
		
	}
	
	/**
	 * Removes the node from the graph and all edges that touch it
	 * @param node
	 */
	public void unexpand(YNode node){
		theGraph.removeVertex(node);	//this call already removes touching edges
	}
	
	public void removeVertex(YNode node){
		if (theGraph.removeVertex(node)){
			//performs necessary cleaning
			childYNodes.get(node.getParentAreas().first()).remove(node);
			childYNodes.get(node.getParentAreas().second()).remove(node);
			
			//TODO will perform checking if node is centroid?
		}
		else {
			Logger.warn("Attempted to remove non-existent node " + node);
		}
	}
	
	public Map<EntityID, YNode> getCentroids() {
		return centroids;
	}

	void setCentroids(Map<EntityID, YNode> centroids) {
		this.centroids = centroids;
	}

	public List<YNode> getChildren(Area a){
		return childYNodes.get(a.getID());
	}
	
	
	
	public String dumpNodes(){
		String s = "";
		
		for(YNode n : theGraph.vertexSet()){
			s += n +"\n";
		}
		
		return s;
	}

}
