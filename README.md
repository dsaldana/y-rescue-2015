# Y-Rescue 2015
## Quick and dirty execution
(assuming that you have already started the simulator)
Open the terminal at `<y-rescue>/2015/agentcompetition`

Compile the code: `./compile.sh`

Then use one of the scripts below, depending on your needs.

### To start all agents
Run:
`./start-all-agents.sh host`

Where host is the server IP address (localhost if it is the same machine of the agents).

### To start a specific number of agents:
Open the terminal at `<y-rescue>/2015/agentcompetition`

Run:
`./start.sh [f] [F] [a] [A] [p] [P] host`

The parameters are (you do not need to write the braces):
[f]: number of firefighters, [a]: number of ambulances, [p]: number of policemen.
[F]: number of Fire Stations, [A]: number of Ambulance Centers, [P]: number of  Police Offices.
host is the server IP address (localhost if it is the same machine of the agents).

## Tutorial

Little tutorial to get the simulator and our code up and running.

### Install the Simulator
Get the simulator and build it (tested with ant 1.9.3 and Java 1.7.0 OpenJDK):

`$ git clone git://git.code.sf.net/p/roborescue/roborescue roborescue`

`$ cd roborescue`

`$ ant`

(ant takes a lot of time and generates a ton of warnings)

Test the simulator:

`$ cd boot`

`$ ./start.sh -c config/ -m ../maps/gml/test/`

Click OK in "Setup kernel options" screen.
Open another terminal:
 
`$ ./sampleagent.sh`

Start the simulation in the Kernel Gui and watch the sample agents.

To start the simulator without unnecessary windows.


### Set up Y-Rescue code
Now close all this and set up Y-Rescue code:
 
- Get Eclipse: https://eclipse.org/downloads/
- Open Eclipse and create a Java project in `<robocup>/2015/agentcompetition/` (where robocup is the root of your Mercurial working copy)
- Right click in JRE System Library -> Configure build path -> Add external jars
-- add all .jar in `lib` (do not add a lost .txt file in `lib` altogether)
-- Go to "Add library" and select JUnit

Now you should be able to run our code. 
Open the terminal, go to `<RCR>/boot` and run the simulator:

`$ ./start.sh -c config/ -m ../maps/gml/test/`

- In the project, select `main/LaunchAgents.java`, click the Eclipse "Run" button and wait for connection to establish.
- Start the simulator kernel and watch the simulation. Agents' initial implementation outputs some info in the terminal.

### Next steps
Understand the simulator basics
Follow the turorial at: http://roborescue.sourceforge.net/2013/robocup_rescue_simulator-tutorial.pdf


