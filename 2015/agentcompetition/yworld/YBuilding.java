package yworld;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.GaussianGenerator;

import firesimulator.simulator.Simulator;
import firesimulator.world.Wall;
import firesimulator.world.World;
import rescuecore2.log.Logger;
import rescuecore2.standard.entities.Building;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.standard.entities.StandardWorldModel;
import rescuecore2.worldmodel.EntityID;

/**
 * Contains building properties that allow the estimation of 
 * fire progression
 * TODO implement some form of parameter learning
 */
public class YBuilding {
	
	private static final double STEFAN_BOLTZMANN_CONSTANT = 0.000000056704;
	
	//some coefficients (values taken from maps/Kobe2013/config/resq-fire.cfg)
	public static float woodCapacity = 1.1f;//4;
    public static float steelCapacity = 1.0f;//4;
    public static float concreteCapacity = 1.5f;//4;
    
    public static float woodIgnition = 47;//400;
    public static float steelIgnition = 47;//400;
    public static float concreteIgnition = 47;//400;
    
    public static float woodEnergie = 2400;//1;
    public static float steelEnergie = 800;//1;
    public static float concreteEnergie = 350;//1;
    
    public static float woodBurning = 800;
    public static float steelBurning = 850;//800;
    public static float concreteBurning = 800;
	
	//some properties
	float energy, capacity, initialFuel, fuel; //fieryness, brokenness, energy;
	int water, fieryness;
	double totalWallArea;
	
	NumberGenerator<Double> burnRate;
	
	/**
	 * this flag indicates that the fuel information was updated from agent information,
	 * since agent cannot perceive the actual fuel of building 
	 */
	boolean fuzzyFuel; 
	
	//the simulator entity that this YBuilding refers to
	Building referenced;
	
	int lastSeen;	//the timestep this building was last seen by the agent
	
	
	public YBuilding(Building theBuilding){
		referenced = theBuilding;
		
		energy = 0;
		water = 0;
		
		Random random = new Random();
		//mean and stdev from maps/Kobe2013/config/resq-fire.cfg
	    burnRate = new GaussianGenerator(.15, .02, random);
		
		fieryness = 0;
		
		lastSeen = -1;
		
		//assuming floor height of 3 meters
		int volume = referenced.getGroundArea() * referenced.getFloors()* 3;
		
		//calculations are performed as in firesimulator.world.Building
		capacity = volume * getThermoCapacity();
        fuel = initialFuel = (float)(getFuelDensity() * volume);
        fuzzyFuel = false;
        
        totalWallArea = calculateWallArea();
	}
	
	/**
	 * Updates information of this YBuilding. 
	 * Use this when the agent perceived the building or info was received
	 * via communication.
	 * @param time
	 * @param temperature
	 * @param fieryness
	 */
	public void updateFromObservation(int time, int temperature, int fieryness){
		lastSeen = time;
		updateEnergyFromTemperature(temperature);
		setFieryness(fieryness);
	}
	
	/**
	 * Estimates the progression of fire
	 */
	public void burnStep(){
		if (getPredictedTemperature() >= getIgnitionPoint() && fuel > 0) {
			
			if (getPredictedTemperature() >= getIgnitionPoint() && fuel > 0 && isInflammable() ){
				//ignites
				energy = (float) (capacity * getIgnitionPoint() * 1.5);
				
				//onFire = true;
			}
			
            float consumed = getConsum();
            if(consumed > fuel) {
                consumed = fuel;
            }
            /*double oldFuel = fuel;
            double oldEnergy = energy;
            double oldTemp = getPredictedTemperature();
            */
            addEnergy(consumed); //setEnergy(b.getEnergy() + consumed);
            consumeFuel(consumed);
            //b.setPrevBurned(consumed);
            /*
              LOG.debug("Building " + b.getID() + " burned " + consumed + " fuel.");
              LOG.debug("Old fuel: " + oldFuel + ", old energy: " + oldEnergy + ", old temperature: " + oldTemp);
              LOG.debug("New fuel: " + b.fuel + ", new energy: " + b.getEnergy() + ", new temperature: " + b.getTemperature());
            */
        }
        /*else {
            b.setPrevBurned(0f);
        }*/
		
	}
	
	private boolean isInflammable(){
		return ! (
			referenced.getStandardURN() == StandardEntityURN.AMBULANCE_CENTRE ||
			referenced.getStandardURN() == StandardEntityURN.POLICE_OFFICE ||
			referenced.getStandardURN() == StandardEntityURN.FIRE_STATION ||
			referenced.getStandardURN() == StandardEntityURN.REFUGE ||
			referenced.getStandardURN() == StandardEntityURN.HYDRANT
		);
	}
	
	public double getRadiationEnergy() {
        double t=getPredictedTemperature() + 293; // Assume ambient temperature is 293 Kelvin.
        double radEn = (t * t * t * t) * totalWallArea * YFireSimulator.RADIATION_COEFFICENT * STEFAN_BOLTZMANN_CONSTANT;
        
        if (radEn > energy) {
            radEn = energy;
        }
        return radEn;
    }
	
	private double calculateWallArea(){
        double totalWallArea=0;
        
        //walls = new LinkedList();
        int[] apexes = referenced.getApexList();
        
        int fx=apexes[0];
        int fy=apexes[1];
        int lx=fx;
        int ly=fy;
        
        for(int n = 2; n<apexes.length; n++){
            int tx=apexes[n];
            int ty=apexes[++n];
            //Wall w=new Wall(lx,ly,tx,ty,this);
            //if(w.validate()){
            totalWallArea += 3 * 1000 * Math.hypot(fx - lx, fy - ly); //3 = floor height, 1000 = unit conversion
            //}
            //else {
            //    LOG.warn("Ignoring odd wall at building "+getID());
            //}
            lx=tx;
            ly=ty;
        }
        return totalWallArea/1000000d;
    }

	/**
	 * Gains energy from nearby buildings
	 * @param world
	 * @param influenceRange
	 * @param yBuildings
	 */
	public void heatFromNeighborhood(StandardWorldModel world, int influenceRange, Map<EntityID, YBuilding> yBuildings){
		//Note: non-inflammable buildings also heat
		
		/*for(Iterator i=world.getBuildings().iterator();i.hasNext();){
        Building b=(Building)i.next();
        exchangeWithAir(b);
	    }*/
	    
	    Collection<StandardEntity> neighbors = world.getObjectsInRange(this.referenced.getID(), influenceRange);
	    
	    //estimates an exponential decay of energy transfer according to distance
	    //TODO might be necessary to normalize distances
	    //TODO calibrate decay factor 
	    
	    for (StandardEntity n : neighbors){
	    	if (n instanceof Building){
	    		Logger.info("Neighbor " + n.getID() + " at distance " + world.getDistance(referenced.getID(), n.getID()));
	    		
	    		double factor = Math.pow(Math.E, -2 * world.getDistance(referenced.getID(), n.getID()));
	    		
	    		YBuilding y = yBuildings.get(n.getID());
	    		if (y == null){
	    			Logger.error(String.format("Cannot recover YBuilding of id %s neighbor of %s", n.getID(), referenced.getID()));
	    			continue;
	    		}
	    		double radEn = Math.min(y.getRadiationEnergy(), y.getEnergy());
	    		
	    		addEnergy(factor * radEn);
	    	}
	    }
	}
	
	private double getEnergy() {
		return energy;
	}

	/**
	 * Processes the effect of water in the building. 
	 * Same code of firesimulator.simulator.Simulator 
	 * @param waterCoeff
	 * @param gamma
	 */
	public void cool(float waterCoeff, float gamma){
		
		double lWATER_COEFFICIENT=(fieryness > 0 && fieryness < 4 ? waterCoeff : waterCoeff * gamma);
        boolean cond = false;
        if(water > 0){
            /*double oldEnergy = energy;
            double oldTemp = getPredictedTemperature();
            double oldWater = water;*/
            double dE = getPredictedTemperature() * capacity;
            if (dE <= 0) {
                //                LOG.debug("Building already at or below ambient temperature");
                return;
            }
            double effect= water * lWATER_COEFFICIENT;
            int consumed = water;
            if(effect > dE){
                cond = true;
                double pc=1-((effect-dE)/effect);
                effect*=pc;
                consumed*=pc;
            }
            addWater(-consumed);
            addEnergy(-effect);
            //energyHistory.registerCool(b, effect);
            /*LOG.debug("Building " + b.getID() + " water cooling");
            LOG.debug("Old energy: " + oldEnergy + ", old temperature: " + oldTemp + ", old water: " + oldWater);
            LOG.debug("Consumed " + consumed + " water: effect = " + effect);
            LOG.debug("New energy: " + b.getEnergy() + ", new temperature: " + b.getTemperature() + ", new water: " + b.getWaterQuantity());
            */
        }
	}
	
	private void consumeFuel(float amount){
		fuel -= amount;
		if (fuel < 0) {
			fuel = 0;
		}
	}
	
	private void addWater(int w) {
		water += w;
		if (water < 0) water = 0;
	}
	
	private void addEnergy(double e){
		energy += e;
		if (energy < 0) energy = 0;
	}

	/**
	 * Estimates the ammount of fuel to be consumed of this building
	 * @return
	 */
	public float getConsum() {
        if(fuel==0){
            return 0;
        }
        float tf = (float) (getPredictedTemperature()/1000f);
        float lf = fuel / initialFuel;
        float f = (float)(tf * lf * burnRate.nextValue());
        
        if (f<0.005f)
            f=0.005f;
        
        return initialFuel * f;
    }

	/**
	 * Updates fieryness according to observation. Sets fuel accordingly if
	 * necessary.
	 * @param fieryness
	 */
	private void setFieryness(int fieryness) {
		if (fieryness != this.fieryness){
			this.fieryness = fieryness;
			fuzzyFuel = true;
			Logger.info("Fieryness updated from agent info. Fuzzy 'mode' on.");
			updateFuel();
		}
	}
	
	/**
	 * Updates fuel according to fieryness 
	 */
	private void updateFuel() {
		if (!fuzzyFuel){
			Logger.warn("Won't update fuel, because 'fuzzy' mode is off");
			return;
		}
		switch(fieryness){
		
		case 0:
		case 4:	//no burns
			fuel = initialFuel;
			fuzzyFuel = false;	//this information is precise
			Logger.info("Precise fieryness information processed. Fuzzy 'mode' off.");
			break;
		
		case 1:
		case 5: //slight damage
			fuel = .75f * initialFuel;
			break;
			
		case 2:
		case 6: //moderate damage
			fuel = .50f * initialFuel;
			break;
			
		case 3:
		case 7: //severe damage
			fuel = .25f * initialFuel;
			break;
			
		case 8: //burnt down
			fuel = 0;
			fuzzyFuel = false;	//this information is precise
			Logger.info("Precise fieryness information processed. Fuzzy 'mode' off.");
			break;
			
		default:
				Logger.error("Invalid fieryness at updateFuel! Value: " + fieryness);
		}
	}

	/**
	 * Returns the temperature predicted by the capacity
	 * and current energy of the building
	 * @return
	 */
	public float getPredictedTemperature(){
		return energy / capacity;
	}
	
	
	private void updateEnergyFromTemperature(int temperature){
		energy = capacity * temperature;
	}
	
	/**
	 * Returns the ignition point. Same code as firesimulator.world.Building
	 * @return
	 */
	public float getIgnitionPoint(){
        switch(referenced.getBuildingCode()){
        case 0:
            return woodIgnition;
        case 1:
            return steelIgnition;
        default:
            return concreteIgnition;
        }
    }
	
	/**
	 * Returns the fuel density. Same code as firesimulator.world.Building
	 * @return
	 */
	private float getFuelDensity(){
        switch(referenced.getBuildingCode()){
        case 0:
            return woodEnergie;
        case 1:
            return steelEnergie;
        default:
            return concreteEnergie;
        }
    }

	/**
	 * Returns the thermo capacity. Same code as firesimulator.world.Building
	 * @return
	 */
    private float getThermoCapacity(){
        switch(referenced.getBuildingCode()){
        case 0:
            return woodCapacity;
        case 1:
            return steelCapacity;
        default:
            return concreteCapacity;
        }
    }
    
    @Override
    public String toString(){
    	return "YBuilding of "+ referenced;
    }
	

}
