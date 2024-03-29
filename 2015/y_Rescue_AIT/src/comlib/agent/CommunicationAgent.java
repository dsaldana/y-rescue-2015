package comlib.agent;


import comlib.manager.MessageManager;
import comlib.message.CommunicationMessage;
import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.messages.Message;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.Human;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.messages.AKSubscribe;
import rescuecore2.worldmodel.ChangeSet;

import java.util.Collection;
import java.util.List;


public abstract class CommunicationAgent<E extends StandardEntity> extends StandardAgent<E> {

    public MessageManager manager;

    public int ignoreTime;

    public long startProcessTime;

    public CommunicationAgent()
    {
        super();
    }

    public abstract void registerEvent(MessageManager manager);

    public abstract void think(int time, ChangeSet changed);

    public void sendSpeak(CommunicationMessage msg)
    {
        this.manager.addSendMessage(msg);
    }

    public void sendSay(CommunicationMessage msg)
    {
        this.manager.addVoiceSendMessage(msg);
    }

    @Override
    public void postConnect()
    {
        super.postConnect();
        this.manager = new MessageManager(this.config, this.me().getID());
        this.registerProvider(this.manager);
        this.registerEvent(this.manager);
        this.ignoreTime = config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY);
    }

    public void registerProvider(MessageManager manager){}

    @Override
    protected final void think(int time, ChangeSet changed, Collection<Command> heard)
    {
    	this.startProcessTime = System.currentTimeMillis();

    	System.out.println(String.format("----------- Start of Timestep %d --------------", time));
    	Logger.info(String.format("----------- Start of Timestep %d --------------", time));
    	try{

			if (time <= this.ignoreTime) {
				send(new AKSubscribe(getID(), time, 1));
			} else {
				this.receiveBeforeEvent(time, changed);
				try {
					this.manager.receiveMessage(time, heard);
				} 
				catch (Exception s) {
					Logger.error("Error while receiving message!", s);
				}
			}
			this.think(time, changed);

			if (time > this.ignoreTime) {
				this.send(this.manager.createSendMessage(super.getID()));
				this.sendAfterEvent(time, changed);
			}
    	}
    	catch (Exception e){
    		Logger.error("This is bad! An odd error occurred during think of CommunicationAgent!.", e);
    	}
    	
    	long secsToProcess = (System.currentTimeMillis() - this.startProcessTime);
    	
        System.out.println(String.format("----------- End of Timestep %d, %s Time to process [%d] msecs --------------\n", time, this.getName(), secsToProcess));
        Logger.info(String.format("----------- End of Timestep %d, %s Time to process [%d] msecs --------------\n", time, this.getName(), secsToProcess));
    }

    public void receiveBeforeEvent(int time, ChangeSet changed) {
    }

    public void sendAfterEvent(int time, ChangeSet changed) {
    }

    public void send(Message[] msgs)
    {
        for(Message msg : msgs) super.send(msg);
    }

    public void send(List<Message> msgs)
    {
    	Logger.trace("___ Will send messages: " + msgs);
        for(Message msg : msgs) {
        	
        	super.send(msg);
        }
    }

    // temp Leave ---
    /*
         @Override
         protected final void sendSpeak(int time, int channel, byte[] data) {
//super.sendSpeak(time, channel, data);
         }

         @Override
         protected final void sendSay(int time, byte[] data) {
//super.sendSay(time, data);
         }
         */
}
