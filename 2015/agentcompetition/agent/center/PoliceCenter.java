package agent.center;

import java.util.Collection;
import java.util.EnumSet;

import rescuecore2.log.Logger;
import rescuecore2.messages.Command;
import rescuecore2.standard.components.StandardAgent;
import rescuecore2.standard.entities.PoliceOffice;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.ChangeSet;

public class PoliceCenter extends StandardAgent<PoliceOffice> {

	public PoliceCenter() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
    public String toString() {
        return "RoboCopCenter " + me().getID();
    }

    @Override
    protected void think(int time, ChangeSet changed, Collection<Command> heard) {
        if (time == config.getIntValue(kernel.KernelConstants.IGNORE_AGENT_COMMANDS_KEY)) {
            // Subscribe to channels 1 and 2
            sendSubscribe(time, 1, 2);
        }
        for (Command next : heard) {
            Logger.debug("Heard " + next);
        }
        sendRest(time);
    }

	
	@Override
	protected EnumSet<StandardEntityURN> getRequestedEntityURNsEnum() {
		return EnumSet.of(StandardEntityURN.POLICE_OFFICE);
	}
	
	
}
