package comlib.provider.information;

import comlib.event.MessageEvent;
import comlib.event.information.MessageBuildingEvent;
import comlib.manager.RadioConfig;
import comlib.manager.VoiceConfig;
import comlib.message.information.MessageBuilding;
import comlib.provider.MapMessageProvider;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import rescuecore2.log.Logger;


public class MessageBuildingProvider extends MapMessageProvider<MessageBuilding, MessageBuildingEvent>
{
	public MessageBuildingProvider(int id)
	{
		super(id);
	}

	protected void writeMessage(RadioConfig config, BitOutputStream bos, MessageBuilding msg)
	{
		super.writeMessage(config, bos, msg);
		bos.writeBits(msg.getBuildingID().getValue(), config.getSizeOfBuildingID());
		bos.writeBits(msg.getBrokenness(), config.getSizeOfBuildingBrokenness());
		bos.writeBits(msg.getFieryness(), config.getSizeOfBuildingFieryness());
		bos.writeBits(msg.getTemperature(), config.getSizeOfBuildingTemperature());		
	}

	protected void writeMessage(VoiceConfig config, StringBuilder sb, MessageBuilding msg)
	{
		//config.appendData(sb, String.valueOf(msg.getValue()));
	}

	protected MessageBuilding createMessage(RadioConfig config, int time, BitStreamReader bsr)
	{
		int rawID = bsr.getBits(config.getSizeOfBuildingID());
		int rawBrokenness = bsr.getBits(config.getSizeOfBuildingBrokenness());
		int rawFieryness = bsr.getBits(config.getSizeOfBuildingFieryness()); 
		int rawTemperature = bsr.getBits(config.getSizeOfBuildingTemperature());
		
		Logger.trace(String.format(
			"Decoding! rawID=%d, rawBrk=%d, rawFyeri=%d, rawTemp=%d", 
			rawID, rawBrokenness, rawFieryness, rawTemperature
		));
		return new MessageBuilding(time, -1,
			rawID,
			rawBrokenness,
			rawFieryness,
			rawTemperature
		);
	}

	protected MessageBuilding createMessage(VoiceConfig config, int time, int ttl, String[] data, int next)
	{
		return null;
		// return new MessageCivilian(
		// 		time, ttl,
		// 		Integer.parseInt(data[next])
		// 		);
	}

//	@Override
//	public void trySetEvent(MessageBuildingEvent ev) {
//
//		if (ev instanceof MessageBuildingEvent) { this.event = ev; }
//	}

	@Override
	public Class<? extends MessageEvent> getEventClass() {
		return MessageBuildingEvent.class;
	}
}
