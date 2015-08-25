package adk.team.yrescue.control;

import adk.team.control.ControlAmbulance;
import comlib.manager.MessageManager;
import rescuecore2.config.Config;
import rescuecore2.worldmodel.ChangeSet;

public class YrescueControlAmbulance extends ControlAmbulance {
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
