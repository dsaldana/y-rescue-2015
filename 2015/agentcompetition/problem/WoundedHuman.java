package problem;

import message.MessageTypes;
import rescuecore2.worldmodel.EntityID;

/**
 * Encapsulates a problem related to a wounded civilian
 * Attributes are public
 * @author anderson
 *
 */
public class WoundedHuman extends Problem {

	public EntityID civilianID;	//civilian id
	public EntityID position;		//id of the place where the civilian is
	
	public int buriedness;	//how much the civilian is buried
	public int health;		//the civilian's HP
	public int damage;		//the ammount of damage the civilian has
	
	/**
	 * Constructs a WoundedCivilian and sets its attributes 
	 * @param humanID
	 * @param humanPosition
	 * @param buriedness
	 * @param hp
	 * @param dmg
	 * @param lastUpdate
	 */
	public WoundedHuman(EntityID humanID, EntityID humanPosition, int buriedness, int hp, int dmg, int lastUpdate){
		this.civilianID = humanID;
		
		update(humanPosition, buriedness, hp, dmg, lastUpdate);
		markUnsolved(lastUpdate);
	}
	
	@Override
	public int hashCode() {
		return civilianID.getValue();
	}
	
	/**
	 * Returns whether the ID of the WoundedCivilians are equal
	 * @param other
	 * @return boolean
	 */
	public boolean equals(WoundedHuman other){
		return civilianID.equals(other.civilianID);
	}
	
	/**
	 * Updates the attributes of this wounded civilian
	 * @param buriedness
	 * @param hp
	 * @param dmg
	 * @param time
	 */
	public void update(EntityID position, int buriedness, int hp, int dmg, int time){
		this.position = position;
		this.buriedness = buriedness;
		this.health = hp;
		this.damage = dmg;
		setUpdateTime(time);
	}

	@Override
	public byte[] encodeReportMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d,%d,%d,%d,%d", 
			MessageTypes.REPORT_WOUNDED_HUMAN.ordinal(), senderID.getValue(), civilianID.getValue(),position.getValue(),
			buriedness,health,damage
		);
		
		return message.getBytes();
	}
	
	@Override
	public byte[] encodeEngageMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d,%d,%d,%d,%d", 
			MessageTypes.ENGAGE_WOUNDED_HUMAN.ordinal(), senderID.getValue(), civilianID.getValue(),position.getValue(),
			buriedness,health,damage
		);
		
		return message.getBytes();
	}

	@Override
	public byte[] encodeSolvedMessage(EntityID senderID) {
		String message = String.format(
			"%d,%d,%d", MessageTypes.SOLVED_WOUNDED_HUMAN.ordinal(), senderID.getValue(), civilianID.getValue()
		);
		
		return message.getBytes();
	}

	@Override
	public EntityID getEntityID() {
		return civilianID;
	}

	@Override
	public String toString() {
		return String.format("WoundedHuman: id=%s, pos=%s, bur=%d, dmg=%d, hp=%d, time=%d", getEntityID(), position, buriedness, damage, health, getUpdateTime());
	}

	

}
