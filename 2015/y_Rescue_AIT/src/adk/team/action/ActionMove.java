package adk.team.action;

import adk.team.tactics.Tactics;
import rescuecore2.log.Logger;
import rescuecore2.messages.Message;
import rescuecore2.standard.messages.AKMove;
import rescuecore2.worldmodel.EntityID;

import java.util.List;

public class ActionMove extends Action {
    
    private List<EntityID> path;

    private boolean usePosition;
    private int posX;
    private int posY;
    
    public ActionMove(Tactics tactics, List<EntityID> movePath) {
        super(tactics);
        this.usePosition = false;
        this.path = movePath;
        Logger.debug("ActionMove created. " + this);
    }
    
    public ActionMove(Tactics tactics, List<EntityID> movePath, int destinationX, int destinationY) {
        super(tactics);
        this.usePosition = true;
        this.path = movePath;
        this.posX = destinationX;
        this.posY = destinationY;
        Logger.debug("ActionMove created. " + this);
    }

    public List<EntityID> getPath() {
        return this.path;
    }

    public boolean getUsePosition() {
        return this.usePosition;
    }

    public int getPosX() {
        return this.posX;
    }

    public int getPosY() {
        return this.posY;
    }
    
    @Override
    public Message getCommand(EntityID agentID, int time) {
		Logger.trace(this + ", agentID="+agentID + ", time=" + time);
        return this.usePosition ? new AKMove(agentID, time, this.path, this.posX, this.posY) : new AKMove(agentID, time, this.path);
    }
    
    @Override
    public String toString(){
    	return String.format("ActionMove: usePosition=%s, path=%s, coords=(%d, %d) ", usePosition, path, posX, posY);
    }
}