package search;

import java.util.List;
import java.util.NoSuchElementException;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;

import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;


public class YSearchGraph {
	
	private SimpleWeightedGraph<YNode, YEdge> theGraph; 
	
	public YSearchGraph(StandardWorldModel worldModel){
		
		theGraph = new SimpleWeightedGraph<YNode, YEdge>(YEdge.class);
		
		//traverses all areas, adding nodes in midpoint of geometric edge frontiers
		for(StandardEntity e : worldModel.getEntitiesOfType(StandardEntityURN.ROAD, StandardEntityURN.BUILDING)){
			Area a = (Area) e;

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
				theGraph.addVertex(node);
				Logger.info(String.format("Added node %s on %s", node, frontier));
			}
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
		}
		
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
