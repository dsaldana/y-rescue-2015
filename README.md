# Y-Rescue 2015
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
Replace the file in the simulator: boot/start.sh by patch/start.sh


### Set up Y-Rescue code
Now close all this and set up Y-Rescue code:
 
- Get Eclipse: https://eclipse.org/downloads/
- Open Eclipse and create a Java project in `<robocup>/2015/agentcompetition/` (where robocup is the root of your Mercurial working copy)
- Right click in JRE System Library -> Configure build path -> Add external jars
-- add all .jar in `lib`
-- Go to "Add library" and select JUnit

Now you should be able to run our code. 
Open the terminal, go to `<RCR>/boot` and run the simulator:

`$ ./start.sh -c config/ -m ../maps/gml/test/`

- In the project, select `main/LaunchAgents.java`, click the Eclipse "Run" button and wait for connection to establish.
- Start the simulator kernel and watch the simulation. Agents' initial implementation outputs some info in the terminal.



