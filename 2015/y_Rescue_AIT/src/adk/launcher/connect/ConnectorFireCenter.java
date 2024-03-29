package adk.launcher.connect;

import adk.launcher.ConfigKey;
import adk.launcher.TeamLoader;
import adk.launcher.station.FireBrigadeStation;
import adk.team.Team;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.connection.ConnectionException;

public class ConnectorFireCenter implements Connector {
    @Override
    public void connect(ComponentLauncher launcher, Config config, TeamLoader loader) {
        String name = config.getValue(ConfigKey.KEY_FIRE_STATION_NAME, "dummy");
        int count = config.getIntValue(ConfigKey.KEY_FIRE_STATION_COUNT, -1);
        System.out.println("[START] Connect Fire Center (teamName:" + name + ")");
        System.out.println("[INFO ] Load Fire Team (teamName:" + name + ")");
        Team team = loader.get(name);
        if(team == null) {
            System.out.println("[ERROR] Team is Null !!");
            if(TeamLoader.KEYWORD_RANDOM.equalsIgnoreCase(name)) {
                int limit = config.getIntValue(ConfigKey.KEY_LOAD_RETRY, loader.size());
                int i = 0;
                while (i < limit && team == null) {
                    System.out.println("[INFO ] Retry Load Team (teamName:" + name + ")");
                    team = loader.getRandomTeam();
                    i++;
                }
            }
            if(team == null) {
                if (config.getBooleanValue(ConfigKey.KEY_RUN_DEFAULT_SYSTEM, false)) {
                    System.out.println("[INFO ] Load Default System");
                    team = loader.getDefaultTeam();
                } else {
                    System.out.println("[ERROR] Cannot Load Team !!");
                    System.out.println("[END  ] Connect Fire Center (success:0)");
                    return;
                }
            }
        }
        if(team.getFireStationControl() == null) {
            System.out.println("[ERROR] Cannot Load Fire Station PreControl !!");
            if(TeamLoader.KEYWORD_RANDOM.equalsIgnoreCase(name)) {
                int limit = config.getIntValue(ConfigKey.KEY_LOAD_RETRY, loader.size());
                int i = 0;
                while (i < limit && team.getFireStationControl() == null) {
                    System.out.println("[INFO ] Retry Load Team (teamName:" + name + ")");
                    team = loader.getRandomTeam();
                    i++;
                }
            }
            if(team.getFireStationControl() == null) {
                if (config.getBooleanValue(ConfigKey.KEY_RUN_DEFAULT_SYSTEM, false)) {
                    System.out.println("[INFO ] Load Default System");
                    team = loader.getDefaultTeam();
                } else {
                    System.out.println("[END  ] Connect Fire Center (success:0)");
                    return;
                }
            }
        }
        System.out.println("[INFO ] Fire Station PreControl (teamName:" + team.getTeamName() + ")");
        name = "[INFO ] Connect FireBrigadeStation (teamName:" + team.getTeamName() + ")";
        int connectAgent = 0;
        try {
            for (int i = 0; i != count; ++i) {
                launcher.connect(new FireBrigadeStation(team.getFireStationControl(), config.getBooleanValue(ConfigKey.KEY_PRECOMPUTE, false)));
                System.out.println(name);
                connectAgent++;
            }
        } catch (ComponentConnectionException | InterruptedException | ConnectionException ignored) {
        }
        System.out.println("[END  ] Connect Fire Center (success:" + connectAgent + ")");
    }
}
