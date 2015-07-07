package util;

import java.util.Comparator;

import problem.WoundedHuman;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardWorldModel;

/**
   A comparator that sorts entities by distance to a reference point.
*/
public class WoundedHumanHPSorter implements Comparator<WoundedHuman> {

    /**
       Create a DistanceSorter.
       @param reference The reference point to measure distances from.
       @param world The world model.
    */
    public WoundedHumanHPSorter() {
    }

    @Override
    public int compare(WoundedHuman a, WoundedHuman b) {
        return (a.health - a.damage) - (b.health - b.damage);
    }
}
