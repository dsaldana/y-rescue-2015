package yrescue.util;

import java.awt.Polygon;
import java.util.List;

import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.Edge;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

public class GeometricUtil {
    
	/**
	 * Calculate the area of a entity given their vertex list
	 * @param entity
	 * @param world
	 * @return
	 */
	
    public static Polygon getPolygon(int [] apexes){
    	Polygon p = new Polygon();
    	for(int i = 0; i < apexes.length; i++){
    		p.addPoint(apexes[i], apexes[i+1]);
    		i++;
    	}
    	return p;
    }
	
	public static int getAreaOfEntity(EntityID entity, StandardWorldModel world){
		Area area0 = (Area) world.getEntity(entity);
		List<Edge> edgeList = area0.getEdges();
    	
    	int n = edgeList.size();
    	int sum = 0;
    	for (int i = 0; i < n; i++) {
    	    sum += edgeList.get(i).getStartX() * (edgeList.get((i + 1) % n).getStartY() - edgeList.get((i + n - 1) % n).getStartY());
    	}
    	
    	return sum;
    }
	
}
