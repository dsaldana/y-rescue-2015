package search;

import java.util.List;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;

public class YEdge {
	YNode endPoint1, endpoint2;
	Area parent;
	
	public YEdge(YNode end1, YNode end2){
		endPoint1 = end1;
		endPoint1 = end2;
		
		//TODO infer parent Area
		
	}
	
	public float getWeight(){
		//TODO perform magic calculations
		//Edge has neighbor info!
		
		//List<Edge> p = parent.getEdges();
		//p.get(0).getNeighbour();
		return 1;
	}
}
