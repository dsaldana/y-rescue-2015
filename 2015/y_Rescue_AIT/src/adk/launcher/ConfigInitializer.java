package adk.launcher;

import adk.launcher.option.*;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigInitializer {

    public static Config getConfig(File configPath, String[] args) {
        Config commandLine = analysis(args);
        //File configDir = new File(System.getProperty("user.dir"), "config");
        if (!configPath.exists()) {
            if(!configPath.mkdir()) {
                return commandLine;
            }
        }
        try {
            Config config = new Config(configPath);
            config.merge(commandLine);
            return config;
        } catch (ConfigException e) {
            e.printStackTrace();
        }
        return commandLine;
    }

    public static Config analysis(String[] args) {
    	Config config = new Config();
    	//System.out.println("Here");
        Map<String, Option> options = initOption();
    	//System.out.println("Here");
        
        //System.out.println(args); 
       
        for(String str : args) {
        	//System.out.println("Here");
        	String[] strArray = str.split(":");
            //System.out.println("args " + strArray[0]);
            Option option = options.get(strArray[0]);
            //System.out.println("option " + option);
            if(option != null) {
            	System.out.println(strArray);
            	//System.out.println(strArray);
                option.setValue(config, strArray);
            }
        }
        //System.out.println("args0 " + strArray[0]);
        //System.out.println("args1 " + strArray[1]);
        //System.out.println("args2 " + strArray[2]);
        //System.out.println("args3 " + strArray[3]);
        //System.out.println("args4 " + strArray[4]);
        //System.out.println("args5 " + strArray[5]);
        //System.out.println("args6 " + strArray[6]);
        //System.out.println("args7 " + strArray[7]);
        return config;
    }

    private static Map<String, Option> initOption() {
        Map<String, Option> options = new HashMap<>();
        registerOption(options, new OptionServer());
        registerOption(options, new OptionHost());
        registerOption(options, new OptionPort());
        registerOption(options, new OptionRetry());
        registerOption(options, new OptionDefaultSystem());
        registerOption(options, new OptionTeam());
        registerOption(options, new OptionAmbulance());
        registerOption(options, new OptionFire());
        registerOption(options, new OptionPolice());
        registerOption(options, new OptionPrecompute());
        return options;
    }

    private static void registerOption(Map<String, Option> options, Option option) {
        options.put(option.getKey(), option);
    }
}
