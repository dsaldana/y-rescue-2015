package problem;

import rescuecore2.worldmodel.EntityID;
import message.TaskMessageEncoder;

public abstract class Problem implements TaskMessageEncoder {
	
	private boolean solved;
	private int lastUpdate;			//timestep where this problem were last updated
	
	/**
	 * Mark this problem as solved
	 * @param time the timestep where the problem was perceived as solved
	 */
	public void markSolved(int time){
		solved = true;
		setUpdateTime(time);
	}
	
	/**
	 * Mark this problem as unsolved
	 */
	public void markUnsolved(int time){
		solved = false;
		setUpdateTime(time);
	}
	
	/**
	 * Returns whether this problem was marked as solved
	 * @return
	 */
	public boolean isSolved(){
		return solved;
	}
	
	/**
	 * Sets the timestep in which this problem was last updated
	 * @param time
	 */
	public void setUpdateTime(int time) {
		lastUpdate = time;
	}
	
	/**
	 * Returns the timestep in which this problem was last updated
	 * @param time
	 * @return
	 */
	public int getUpdateTime() {
		return lastUpdate;
	}
	
	/**
	 * Methods from MessageEncoder interface that each problem 
	 * type must implement
	 */
	public abstract byte[] encodeEngageMessage(EntityID senderID);
	public abstract byte[] encodeSolvedMessage(EntityID senderID);
	public abstract byte[] encodeReportMessage(EntityID senderID);

	/**
	 * The EntityID that this problem refers to
	 * @return
	 */
	public abstract EntityID getEntityID();
	
	@Override
	public abstract String toString();
}
