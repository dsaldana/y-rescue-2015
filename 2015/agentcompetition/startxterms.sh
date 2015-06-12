#!/bin/bash

PIDS=
classpath=.:lib/clear.jar:lib/jfreechart-1.0.13.jar:lib/rescuecore2.jar:lib/collapse.jar:lib/jscience-4.3.jar:lib/rescuecore.jar:lib/commons-logging-1.1.1.jar:lib/jscience_license.txt:lib/resq-fire-jar.jar:lib/dom4j.jar:lib/jsi-1.0b2p1.jar:lib/resq-fire-lib.jar:lib/gis2.jar:lib/jts-1.11.jar:lib/sample.jar:lib/handy.jar:lib/junit-4.5.jar:lib/standard.jar:lib/human.jar:lib/kernel.jar:lib/traffic3.jar:lib/ignition.jar:lib/log4j-1.2.15.jar:lib/trove-0.1.8.jar:lib/jaxen-1.1.1.jar:lib/maps.jar:lib/uncommons-maths-1.2.jar:lib/jcommon-1.0.16.jar:lib/misc.jar:lib/xml-0.0.6.jar

echo "Will start $1 Firefighters, $2 Ambulances and $3 Policemen."

# $1 is the agent type ("firefighter", "ambulance", "policeman")
# $2 is the number of agents
function launchagents {
	echo "will launch $2 $1."
	i=0
	while [ $i -lt $2 ]; do
		echo "launching $1 #$i"
		xterm -geometry 120x32 -T $1 -e "java -Xms1G -Xmx2G -classpath $classpath main.LaunchAgent $1 2>&1" &
		PIDS="$PIDS $!"
		i=$[$i + 1]
	done
}

launchagents "firefighter" $1
launchagents "ambulance" $2
launchagents "policeman" $3

trap 'kill $(jobs -p); exit' SIGHUP SIGINT SIGTERM EXIT

echo "Agents launched. To terminate them, hit Ctrl+C in this terminal."

while true; do
	sleep 1
done
