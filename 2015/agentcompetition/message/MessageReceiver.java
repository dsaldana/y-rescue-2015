package message;

import java.io.UnsupportedEncodingException;

import message.misc.BroadCastRefillRateMessage;
import problem.BlockedArea;
import problem.BurningBuilding;
import problem.Problem;
import problem.Recruitment;
import problem.WoundedHuman;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.entities.StandardEntityConstants.Fieryness;
import rescuecore2.standard.messages.AKSay;
import rescuecore2.standard.messages.AKSpeak;
import rescuecore2.standard.messages.AKTell;
import rescuecore2.worldmodel.EntityID;

public class MessageReceiver {

	public MessageReceiver() {
	}
	
	public static ReceivedMessage decodeMessage(Command cmd){
		String msg = "";// = byteToString(theMessage);
		
        if (cmd instanceof AKSpeak) msg = byteToString(((AKSpeak) cmd).getContent()); //radio
        else if (cmd instanceof AKSay) msg = byteToString(((AKSay) cmd).getContent()); //voice
        else if (cmd instanceof AKTell) msg = byteToString(((AKTell) cmd).getContent()); //also voice
        
        //TODO: tratar 'Ouch' e 'Help' pois eles dao dicas de onde esta o ferido
        if (msg.equals("") || msg.equals("Ouch") ||  msg.equals("Help")) return null;
        //decode the parts of the Message
        
        Logger.info("Decoding: " + '"' + msg + '"');
        String[] parts = msg.split(",");
        int type = Integer.parseInt(parts[0]);
        
        if (type == MessageTypes.BROADCAST_REFILL_RATE.ordinal()){
        	return decodeBroadCastRefillRateMessage(cmd, parts);
        }
        
        return decodeTaskMessage(cmd, parts);
	}
	
	private static ReceivedMessage decodeBroadCastRefillRateMessage(Command cmd, String[] parts) {
		//not used: int type = Integer.parseInt(parts[0]);
		//not used: EntityID senderID = new EntityID(Integer.parseInt(parts[1])); //not used by now
        
		return new BroadCastRefillRateMessage(Integer.parseInt(parts[2]));
	}

	public static ReceivedMessage decodeTaskMessage(Command cmd, String[] parts){
		
        //String[] parts = msg.split(",");
        
        Problem p = null;
        MessageTypes t = null;
        
        int type = Integer.parseInt(parts[0]);
        EntityID senderID = new EntityID(Integer.parseInt(parts[1])); //not used by now
        EntityID id = new EntityID(Integer.parseInt(parts[2]));
        
        //lots of if-else to test message types
        // --- blocked road message types
        if (type == MessageTypes.REPORT_BLOCKED_AREA.ordinal()){
        	int repairCost = Integer.parseInt(parts[3]);
        	
        	p = new BlockedArea(id, repairCost, cmd.getTime());
        	t = MessageTypes.REPORT_BLOCKED_AREA;
        }
        else if (type == MessageTypes.ENGAGE_BLOCKED_AREA.ordinal()){
        	int repairCost = Integer.parseInt(parts[3]);
        	
        	p = new BlockedArea(id, repairCost, cmd.getTime());
        	t = MessageTypes.ENGAGE_BLOCKED_AREA;
        }
        else if (type == MessageTypes.SOLVED_BLOCKED_AREAS.ordinal()){
        	
        	p = new BlockedArea(id, 0, cmd.getTime());
        	p.markSolved(cmd.getTime());
        	t = MessageTypes.SOLVED_BLOCKED_AREAS;
        	
        }
        
        //--- wounded human message types
        EntityID position;
        if (type == MessageTypes.REPORT_WOUNDED_HUMAN.ordinal()){
        	position = new EntityID(Integer.parseInt(parts[3]));
        	int buriedness = Integer.parseInt(parts[4]);
        	int health = Integer.parseInt(parts[5]);
        	int damage = Integer.parseInt(parts[6]);
        	
        	p = new WoundedHuman(id, position, buriedness, health, damage, cmd.getTime());
        	t = MessageTypes.REPORT_WOUNDED_HUMAN;
        }
        else if (type == MessageTypes.ENGAGE_WOUNDED_HUMAN.ordinal()){
        	position = new EntityID(Integer.parseInt(parts[3]));
        	int buriedness = Integer.parseInt(parts[4]);
        	int health = Integer.parseInt(parts[5]);
        	int damage = Integer.parseInt(parts[6]);
        	
        	p = new WoundedHuman(id, position, buriedness, health, damage, cmd.getTime());
        	t = MessageTypes.ENGAGE_WOUNDED_HUMAN;
        }
        else if (type == MessageTypes.SOLVED_WOUNDED_HUMAN.ordinal()){
        	
        	p = new WoundedHuman(id, null, 0, 10000, 0, cmd.getTime());
        	p.markSolved(cmd.getTime());
        	t = MessageTypes.SOLVED_WOUNDED_HUMAN;
        	
        }
        
        // --- burning building message types
        if (type == MessageTypes.REPORT_BURNING_BUILDING.ordinal()){
        	int brokenness = Integer.parseInt(parts[3]);
        	int fieryness = Integer.parseInt(parts[4]);
        	int temperature = Integer.parseInt(parts[5]);
        	
        	p = new BurningBuilding(id, brokenness, fieryness, temperature, cmd.getTime());
        	t = MessageTypes.REPORT_BURNING_BUILDING;
        }
        else if (type == MessageTypes.ENGAGE_BURNING_BUILDING.ordinal()){
        	int brokenness = Integer.parseInt(parts[3]);
        	int fieryness = Integer.parseInt(parts[4]);
        	int temperature = Integer.parseInt(parts[5]);
        	
        	p = new BurningBuilding(id, brokenness, fieryness, temperature, cmd.getTime());
        	t = MessageTypes.ENGAGE_BURNING_BUILDING;
        }
        else if (type == MessageTypes.SOLVED_BURNING_BUILDING.ordinal()){
        	
        	p = new BurningBuilding(id, 0, Fieryness.WATER_DAMAGE.ordinal(), 0, cmd.getTime());
        	p.markSolved(cmd.getTime());
        	t = MessageTypes.SOLVED_BURNING_BUILDING;
        }
        
        // --- Recruitment message types
        if (type == MessageTypes.RECRUITMENT_REQUEST.ordinal()){
        	int taskType = Integer.parseInt(parts[3]);
        	position = new EntityID(Integer.parseInt(parts[4]));
        	
        	TaskType enumTaskType = TaskType.getValue(taskType);
        	if(enumTaskType == null){
        		// TODO: check if this is going to work watchout with returning NULL !
        		return null;
        	}
        	
        	p = new Recruitment(senderID, id, position, enumTaskType, MessageTypes.RECRUITMENT_REQUEST, cmd.getTime());
        	t = MessageTypes.RECRUITMENT_REQUEST;
        }
        else if (type == MessageTypes.RECRUITMENT_COMMIT.ordinal()){
        	int taskType = Integer.parseInt(parts[3]);
        	position = new EntityID(Integer.parseInt(parts[4]));
        	
        	TaskType enumTaskType = TaskType.getValue(taskType);
        	if(enumTaskType == null){
        		// TODO: check if this is going to work watchout with returning NULL !
        		return null;
        	}
        	
        	p = new Recruitment(senderID, id, position, enumTaskType, MessageTypes.RECRUITMENT_COMMIT, cmd.getTime());
        	t = MessageTypes.RECRUITMENT_COMMIT;
        }
        else if (type == MessageTypes.RECRUITMENT_RELEASE.ordinal()){
        	int taskType = Integer.parseInt(parts[3]);
        	position = new EntityID(Integer.parseInt(parts[4]));
        	
        	TaskType enumTaskType = TaskType.getValue(taskType);
        	if(enumTaskType == null){
        		// TODO: check if this is going to work watchout with returning NULL !
        		return null;
        	}
        	
        	p = new Recruitment(senderID, id, position, enumTaskType, MessageTypes.RECRUITMENT_RELEASE, cmd.getTime());
        	t = MessageTypes.RECRUITMENT_RELEASE;
        }
        else if (type == MessageTypes.RECRUITMENT_ENGAGE.ordinal()){
        	int taskType = Integer.parseInt(parts[3]);
        	position = new EntityID(Integer.parseInt(parts[4]));
        	
        	TaskType enumTaskType = TaskType.getValue(taskType);
        	if(enumTaskType == null){
        		// TODO: check if this is going to work watchout with returning NULL !
        		return null;
        	}
        	
        	p = new Recruitment(senderID, id, position, enumTaskType, MessageTypes.RECRUITMENT_ENGAGE, cmd.getTime());
        	t = MessageTypes.RECRUITMENT_ENGAGE;
        	
        }
        else if (type == MessageTypes.RECRUITMENT_TIMEOUT.ordinal()){
        	int taskType = Integer.parseInt(parts[3]);
        	position = new EntityID(Integer.parseInt(parts[4]));
        	
        	TaskType enumTaskType = TaskType.getValue(taskType);
        	if(enumTaskType == null){
        		// TODO: check if this is going to work watchout with returning NULL !
        		return null;
        	}
        	
        	p = new Recruitment(senderID, id, position, enumTaskType, MessageTypes.RECRUITMENT_TIMEOUT, cmd.getTime());
        	t = MessageTypes.RECRUITMENT_TIMEOUT;

        }
        
        return new ReceivedMessage(t, p);
	}

	
	private static String byteToString(byte[] msg) {
        try {
            return new String(msg, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            return "";
        }
    }

}
