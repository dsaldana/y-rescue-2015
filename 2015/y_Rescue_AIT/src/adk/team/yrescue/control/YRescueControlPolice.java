package adk.team.yrescue.control;

import adk.team.control.ControlPolice;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.ChangeSet;

public class YRescueControlPolice extends ControlPolice {
    @Override
    public String getControlName() {
        return "Yrescue System";
    }

    @Override
    public void preparation(Config config) {

    }

    @Override
    public void registerEvent(MessageManager manager) {

    }

    @Override
    public void think(int currentTime, ChangeSet updateWorldData, MessageManager manager) {

    }
}
