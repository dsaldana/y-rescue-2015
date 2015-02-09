package qlearning;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents the Q-table of an agent
 */
public class QTable {
	/**
	 * Maps an action (which corresponds to "clear blockade with the given State") 
	 * to its value
	 */
    Map<State, Double> theTable;
    
    public QTable(int tableSize){
        theTable = new HashMap<State, Double> ();
    }
    
    /**
     * Updates an entry on the table using the Q-learning update rule
     * @param roadId
     * @param reward
     * @return 
     */
    public QTable update(State theState, double reward){
        float alpha = QLearningParams.getInstance().getAlpha();
        float gamma = QLearningParams.getInstance().getGamma();
        
        //initializes q-value as zero - init. occurs when key is not on the q-table
        double oldQ = theTable.containsKey(theState) ? theTable.get(theState) : 0;
        
        double newQ = (1 - alpha) * oldQ + alpha * reward; /*(
                reward //+ gamma * maxQForNextState(roadId) //we will not use 'future' actions now
        );*/
        
        //inserts the updated value in the table
        theTable.put(theState, newQ);
        
        return this;
    }
    
    
    
    /**
     * Returns the State which corresponds to the action
     * with the highest Q-value
     * @return State
     */
    public State greedyAction(){
        if(theTable.isEmpty()) return null;
        
        State best = null;
        double bestQ = -10000000; 
        
        //traverses the Q-table to find the action with highest value
        for(Entry<State, Double> candidate : theTable.entrySet()){
            
            if(candidate.getValue() > bestQ)
                best = candidate.getKey();
        }
        return best;
    }
    
    /**
     * Returns a random State from the Q-table
     * @return
     */
    public State randomAction(){
    	if(theTable.isEmpty()) return null;
    	
    	int index = (int) Math.random() * theTable.size(); 
    	
    	return (State) theTable.keySet().toArray()[index];
    }
    
    /*
    @Override
    public String toString(){
        String qTable = "Action\tQvalue\n";
        
        for(State key : theTable.keySet()){
            qTable += key + "\t" + theTable.get(key) + "\n";
        }
        
        return qTable;
    }*/
}
