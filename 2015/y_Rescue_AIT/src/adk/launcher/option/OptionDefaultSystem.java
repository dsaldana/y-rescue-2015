package adk.launcher.option;


import adk.launcher.ConfigKey;
import rescuecore2.config.Config;

public class OptionDefaultSystem extends Option{
    @Override
    public String getKey() {
        return "-ds";
    }

    @Override
    public void setValue(Config config, String[] datas) {
        if(datas.length == 2) {
            config.setValue(ConfigKey.KEY_RUN_DEFAULT_SYSTEM, datas[1]);
        }
    }
}
