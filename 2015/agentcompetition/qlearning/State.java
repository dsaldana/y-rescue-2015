package qlearning;

import rescuecore2.worldmodel.EntityID;

/**
 * Tuple that represents a State for the Q-learning
 */
public class State {
	/**
	 * The ID of the blockade. This value is not used as an index 
	 * in the Q-table
	 */
    public EntityID blockID;
    
    /**
     * The road where the blockade is. This value is not used as an index 
	 * in the Q-table
     */
    public EntityID roadID;
    
    /**
     * Normalized distance from the agent to the blockade
     * The normalization is: 
     * #of graph edges from police to blockade / largest path of the graph
     */
    public float distance;
    
    /**
     * Indicates if the blockade was reported by other agent (true)
     * or discovered by the police forces (false)
     */
    public boolean wasComplaint;
    
    /**
     * The number of other RoboCops that are committed in cleaning
     * this blockade 
     */
    public int numberOfCommits;
    
    /**
     * The Q-value of this blockade
     */
    public int QValue;
    
    public boolean equals(State other){
    	return wasComplaint == other.wasComplaint &&
    			numberOfCommits == other.numberOfCommits &&
    			Math.abs(distance - other.distance) <= 0.05; //admits a 5% tolerance in the distance
    }
    
}
