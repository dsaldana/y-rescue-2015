package adk.launcher.option;

import rescuecore2.Constants;
import rescuecore2.config.Config;

public class OptionHost extends Option {

    @Override
    public String getKey() {
        return "-h";
    }

    @Override
    public void setValue(Config config, String[] datas) {
    	//System.out.println("length: " + datas.length);
        if (datas.length == 2) {
        	//System.out.println("Host - here");
        	config.setValue(Constants.KERNEL_HOST_NAME_KEY, datas[1]);
        }
    }
}