package util;

import java.util.Comparator;

import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/**
   A comparator that sorts entities by distance to a reference point.
*/
public class HPSorter implements Comparator<StandardEntity> {
    private StandardEntity reference;
    private StandardWorldModel world;

    /**
       Create a DistanceSorter.
       @param reference The reference point to measure distances from.
       @param world The world model.
    */
    public HPSorter(StandardEntity reference, StandardWorldModel world) {
        this.reference = reference;
        this.world = world;
    }

    @Override
    public int compare(StandardEntity a, StandardEntity b) {
    	Human h1 = (Human) a;
    	Human h2 = (Human) b;
        return (h1.getHP() - h1.getDamage()) - (h2.getHP() - h2.getDamage());
    }
}
