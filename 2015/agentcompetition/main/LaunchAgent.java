package main;

import java.io.IOException;

import rescuecore2.Constants;
import rescuecore2.components.ComponentConnectionException;
import rescuecore2.components.ComponentLauncher;
import rescuecore2.components.TCPComponentLauncher;
import rescuecore2.config.Config;
import rescuecore2.config.ConfigException;
import rescuecore2.connection.ConnectionException;
import rescuecore2.log.Logger;
import rescuecore2.misc.CommandLineOptions;
import rescuecore2.registry.Registry;
import rescuecore2.standard.entities.StandardEntityFactory;
import rescuecore2.standard.entities.StandardPropertyFactory;
import rescuecore2.standard.messages.StandardMessageFactory;
import agent.center.YCenter;
import agent.platoon.AbstractPlatoon;
import agent.platoon.Ambulance;
import agent.platoon.Firefighter;
import agent.platoon.Policeman;

public class LaunchAgent {
	public static void main(String[] args) {
		Logger.setLogContext("sample");
		try {
			Registry.SYSTEM_REGISTRY
					.registerEntityFactory(StandardEntityFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerMessageFactory(StandardMessageFactory.INSTANCE);
			Registry.SYSTEM_REGISTRY
					.registerPropertyFactory(StandardPropertyFactory.INSTANCE);
			Config config = new Config();
			args = CommandLineOptions.processArgs(args, config);
			int port = config.getIntValue(Constants.KERNEL_PORT_NUMBER_KEY,
					Constants.DEFAULT_KERNEL_PORT_NUMBER);
			String host = config.getValue(Constants.KERNEL_HOST_NAME_KEY,
					Constants.DEFAULT_KERNEL_HOST_NAME);
			
			Class<? extends AbstractPlatoon<?>> agentClass = null;
			
			switch(args[0]){
			case "firefighter":
				agentClass = Firefighter.class;
			break;
			
			case "policeman":
				agentClass = Policeman.class;
			break;
			
			case "ambulance":
				agentClass = Ambulance.class;
			break;
			
			default:
				System.out.println("Unknown agent type " + args[1]);
				System.exit(0);
			}
			
			ComponentLauncher launcher = new TCPComponentLauncher(host, port, config);
			connect(agentClass, launcher, config);
		} catch (IOException e) {
			Logger.error("Error connecting agents", e);
		} catch (ConfigException e) {
			Logger.error("Configuration error", e);
		} catch (ConnectionException e) {
			Logger.error("Error connecting agents", e);
		} catch (InterruptedException e) {
			Logger.error("Error connecting agents", e);
		} catch (ComponentConnectionException e) {
			Logger.info("failed: " + e.getMessage());
			System.exit(1);
		}
	}

	public static void connect(Class<? extends AbstractPlatoon<?>> agentClass, ComponentLauncher launcher, 
			Config config) throws InterruptedException, ConnectionException, ComponentConnectionException 
	{
		
		try {
			String className = agentClass.getSimpleName();
			Logger.info("Connecting " + className +"...");
			launcher.connect(agentClass.newInstance());
			Logger.info(className + " connected");
			
		} 
		catch (InstantiationException | IllegalAccessException e) {
			Logger.error("Error occurred. ", e);
			System.exit(1);
		}
	}
}
