package search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rescuecore2.misc.collections.LazyMap;
import rescuecore2.standard.entities.Area;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.Entity;
import rescuecore2.worldmodel.EntityID;

public class NeighborhoodGraph {

	private NeighborhoodGraph() { } //one shall not instantiate this
	
	
	/**
	 * Constructs a neighborhood graph that reflects the WorldModel
	 * @param world
	 * @return
	 */
	public static Map<EntityID, Set<EntityID>> buildNeighborhoodGraph(StandardWorldModel world){
		Map<EntityID, Set<EntityID>> neighbours = new LazyMap<EntityID, Set<EntityID>>() {
            @Override
            public Set<EntityID> createValue() {
                return new HashSet<EntityID>();
            }
        };
        for (Entity next : world) {
            if (next instanceof Area) {
                Collection<EntityID> areaNeighbours = ((Area)next).getNeighbours();
                neighbours.get(next.getID()).addAll(areaNeighbours);
            }
        }
        return neighbours;
	}
}