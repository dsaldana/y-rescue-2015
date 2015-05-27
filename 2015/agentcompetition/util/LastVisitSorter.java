package util;

import java.util.Comparator;

import agent.platoon.AbstractPlatoon;
import rescuecore2.worldmodel.EntityID;


public class LastVisitSorter implements Comparator<EntityID> {
	private AbstractPlatoon<?> querier;
	int current_time;
	
    /**
       Create a LastVisitSorter.
       @param querier The agent performing the query
       
    */
    public LastVisitSorter(AbstractPlatoon<?> querier){ //), int current_time) {
        this.querier = querier;
        //this.current_time = current_time;
    }

    @Override
    /**
     * Returns positive if a was visited more recently than b,
     * zero if visit times are the same,
     * negative if b was visited mor recently than a
     */
    public int compare(EntityID a, EntityID b) {
    	//int timeA = current_time - querier.lastVisit(a);
    	//int timeB = current_time - querier.lastVisit(b);
        //return timeA - timeB;
    	return querier.lastVisit(a) - querier.lastVisit(b);
    }

}
