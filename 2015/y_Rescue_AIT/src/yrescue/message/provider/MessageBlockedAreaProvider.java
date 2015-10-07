package yrescue.message.provider;

import comlib.event.MessageEvent;
import comlib.event.information.MessageRoadEvent;
import comlib.manager.RadioConfig;
import comlib.manager.VoiceConfig;
import comlib.message.CommunicationMessage;
import comlib.message.information.MessageRoad;
import comlib.provider.MapMessageProvider;
import comlib.provider.MessageProvider;
import comlib.util.BitOutputStream;
import comlib.util.BitStreamReader;
import comlib.util.BooleanHelper;
import yrescue.message.event.MessageBlockedAreaEvent;
import yrescue.message.information.MessageBlockedArea;

public class MessageBlockedAreaProvider extends MapMessageProvider<MessageBlockedArea, MessageBlockedAreaEvent>{

	public MessageBlockedAreaProvider(int id){
		super(id);
	}

	protected void writeMessage(RadioConfig config, BitOutputStream bos, MessageBlockedArea msg)
	{
		super.writeMessage(config, bos, msg);
		bos.writeBits(msg.roadID.getValue(), 32);
		bos.writeBits(msg.x, 32);
		bos.writeBits(msg.y, 32);
		//bos.writeBits(BooleanHelper.toInt(msg.isPassable()), 1);
	}

	protected void writeMessage(VoiceConfig config, StringBuilder sb, MessageRoad msg)
	{
		//config.appendData(sb, String.valueOf(msg.getValue()));
	}

	protected MessageBlockedArea createMessage(RadioConfig config, int time, BitStreamReader bsr)
	{
		return new MessageBlockedArea(
			bsr.getBits(32),
			bsr.getBits(32),
			bsr.getBits(32)
		);
	}

	protected MessageBlockedArea createMessage(VoiceConfig config, int time, int ttl, String[] data, int next)
	{
		return null;
		// return new MessageCivilian(time, ttl, Integer.parseInt(data[next]));
	}

//	@Override
//	public void trySetEvent(MessageRoadEvent ev) {
//		if (ev instanceof MessageRoadEvent) { this.event = ev; }
//
//	}

	@Override
	public Class<? extends MessageEvent> getEventClass() {
		return MessageBlockedAreaEvent.class;
	}

}
