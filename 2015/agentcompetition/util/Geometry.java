package util;

import java.util.List;
import java.util.NoSuchElementException;

import rescuecore2.misc.geometry.Point2D;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;

public class Geometry {

	/**
	 * Returns the smallest edge on the frontier of two areas
	 * @param a1
	 * @param a2
	 * @return
	 */
	public static Edge findSmallestEdgeConnecting(Area a1, Area a2){
		
		//gets the edges that are the frontier in the two areas
		Edge frontier1_2 = Geometry.findFrontierOfArea(a1.getEdges(), a2);
		Edge frontier2_1 = Geometry.findFrontierOfArea(a2.getEdges(), a1);
		
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

	public static Edge findFrontierOfArea(List<Edge> edges, Area a) throws NoSuchElementException{
		
		for (Edge e : edges){
			if (e.getNeighbour() != null && e.getNeighbour().equals(a.getID())){
				return e;
			}
		}
		throw new NoSuchElementException(String.format("No frontier of %s was found in %s", a, edges));
		//return null;
	}
	
	public static Point2D midpoint(Edge e){
		int nodeX = Math.abs(e.getEndX() + e.getStartX()) / 2;
		int nodeY = Math.abs(e.getEndY() + e.getStartY()) / 2;
		
		return new Point2D(nodeX, nodeY);
		
		
	}

}
