package search;

import java.util.List;
import java.util.NoSuchElementException;

import org.jgrapht.graph.DefaultWeightedEdge;

import rescuecore2.misc.Pair;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;

public class YEdge extends DefaultWeightedEdge {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private YNode endPoint1;
	private YNode endPoint2;
	private Area parent;
	
	public YEdge(YNode end1, YNode end2){
		setEndPoint1(end1);
		setEndPoint2(end2);
		
		parent = endPoint1.areaInCommonWith(endPoint2);
		
		if (parent == null) {
			throw new IllegalArgumentException(String.format("%s and %s don't have an Area in common to be connected via YEdge"));
		}
		
	}
	
	@Override
	public boolean equals(Object other){
		if(other == null) return false;
		
		if (!(other instanceof YEdge)) return false;
		
		YEdge otherEdge = (YEdge) other;
		
		return endPoint1.equals(otherEdge.getEndPoint1()) && endPoint2.equals(otherEdge.getEndPoint2());
	}
	
	@Override
	public int hashCode(){
		return String.format("%s%s", endPoint1.hashCode(), endPoint2.hashCode()).hashCode();
	}
	
	@Override
	public String toString(){
		return String.format("YEdge from %s to %s with weight %.3f", endPoint1, endPoint2, getWeight());
	}
	
	/**
	 * Returns the weight of this graph edge (currently, the euclidean distance between its endpoints)
	 * TODO calculate manhattan distance
	 * TODO return infinite if there is a blockade 
	 * @return
	 */
	public double getWeight(){
		//finds which geometric edges this graph edge connects
		/*Edge frontier1 = findSmallestEdgeConnecting(endPoint1.getParentAreas());
		Edge frontier2 = findSmallestEdgeConnecting(endPoint2.getParentAreas());
		
		//calculates the midpoints of the frontiers
		Point2D midpoint1 = new Point2D(
			Math.abs(frontier1.getEndX() - frontier1.getStartX()) / 2, 
			Math.abs(frontier1.getEndY() - frontier1.getStartY()) / 2
		);
		
		Point2D midpoint2 = new Point2D(
			Math.abs(frontier2.getEndX() - frontier2.getStartX()) / 2, 
			Math.abs(frontier2.getEndY() - frontier2.getStartY()) / 2
		);
		
		return Math.hypot(midpoint2.getX() - midpoint1.getX(), midpoint2.getY() - midpoint1.getY());
		*/
		return Math.hypot(getEndPoint1().getX() - getEndPoint2().getX(), getEndPoint1().getY() - getEndPoint2().getY());
	}
	
	Area getParentArea() {
		return parent;
	}

	/**
	 * Returns the smallest edge on the frontier of two areas
	 * @param areas
	 * @return
	 */
	public Edge findSmallestEdgeConnecting(Pair<Area, Area> areas){
		return findSmallestEdgeConnecting(areas.first(), areas.second());
	}
	
	/**
	 * Returns the smallest edge on the frontier of two areas
	 * @param a1
	 * @param a2
	 * @return
	 */
	public Edge findSmallestEdgeConnecting(Area a1, Area a2){
		
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
	
	public Edge findFrontierOfArea(List<Edge> edges, Area a) throws NoSuchElementException{
		
		for (Edge e : edges){
			if (e.getNeighbour() == a.getID()){
				return e;
			}
		}
		throw new NoSuchElementException(String.format("No frontier of %s was found in $s", a, edges));
		//return null;
	}

	public YNode getEndPoint1() {
		return endPoint1;
	}

	public void setEndPoint1(YNode endPoint1) {
		this.endPoint1 = endPoint1;
	}

	public YNode getEndPoint2() {
		return endPoint2;
	}

	public void setEndPoint2(YNode endPoint2) {
		this.endPoint2 = endPoint2;
	}
}
