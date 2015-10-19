#!/bin/bash

PIDS=
classpath=.:bin:library/util/default/commons-logging-1.1.1.jar:library/util/default/dom4j.jar:library/util/default/jaxen-1.1.1.jar:library/util/default/jcommon-1.0.16.jar:library/util/default/jfreechart-1.0.13.jar:library/util/default/jscience-4.3.jar:library/util/default/jsi-1.0b2p1.jar:library/util/default/jts-1.11.jar:library/util/default/junit-4.5.jar:library/util/default/log4j-1.2.15.jar:library/util/default/rescuecore.jar:library/util/default/resq-fire.jar:library/util/default/resq-fire-core.jar:library/util/default/trove-0.1.8.jar:library/util/default/uncommons-maths-1.2.jar:library/util/default/xml-0.0.6.jar:library/util/guava-18.0.jar:library/rescue/core/adf-util.jar:library/rescue/core/clear.jar:library/rescue/core/collapse.jar:library/rescue/core/gis2.jar:library/rescue/core/handy.jar:library/rescue/core/human.jar:library/rescue/core/ignition.jar:library/rescue/core/kernel.jar:library/rescue/core/maps.jar:library/rescue/core/misc.jar:library/rescue/core/rescuecore2.jar:library/rescue/core/resq-fire.jar:library/rescue/core/sample.jar:library/rescue/core/standard.jar:library/rescue/core/traffic3.jar:library/rescue/core/sources/adf-util-sources.jar:library/rescue/core/sources/clear-sources.jar:library/rescue/core/sources/collapse-sources.jar:library/rescue/core/sources/gis2-sources.jar:library/rescue/core/sources/handy-sources.jar:library/rescue/core/sources/human-sources.jar:library/rescue/core/sources/ignition-sources.jar:library/rescue/core/sources/kernel-sources.jar:library/rescue/core/sources/maps-sources.jar:library/rescue/core/sources/misc-sources.jar:library/rescue/core/sources/rescuecore2-sources.jar:library/rescue/core/sources/resq-fire-sources.jar:library/rescue/core/sources/sample-sources.jar:library/rescue/core/sources/standard-sources.jar:library/rescue/core/sources/traffic3-sources.jar



#command: ./startxterms.sh [NUMBER OF FIREFIGHTERS] [NUMBER OF AMBULANCES] [NUMBER OF POLICEMAN]

if [ "$#" -ne 3 ]; then
    echo "Wrong parameters. Usage:"
    echo "./startxterms.sh firefighters ambulances policeforces"
    echo "All arguments must be integers."
    exit
fi

cd ..

echo "Will start $1 Firefighters, $2 Ambulances and $3 Policemen."

# $1 is the agent type ("firefighter", "ambulance", "policeman")
# $2 is the number of agents

function launchagents {
	echo "will launch $2 $1."
	i=0
	while [ $i -lt $2 ]; do
		
		if [ "$1" = "firefighter" ];then
			echo "launching $1 #$i"
			xterm -hold -geometry 120x32 -T $1 -e "java -Xms1G -Xmx2G -classpath $classpath adk.Main -fb:1 -at:0 -pf:0  2>&1" &
			PIDS="$PIDS $!"
			i=$[$i + 1]
		fi

		if [ "$1" = "ambulance" ];then
			echo "launching $1 #$i"
			xterm -hold -geometry 120x32 -T $1 -e "java -Xms1G -Xmx2G -classpath $classpath adk.Main -at:1 -fb:0 -pf:0 2>&1" &
			PIDS="$PIDS $!"
			i=$[$i + 1]
		fi

		if [ "$1" = "policeman" ];then
			echo "launching $1 #$i"
			xterm -hold -geometry 120x32 -T $1 -e "java -Xms1G -Xmx2G -classpath $classpath adk.Main -pf:1 -at:0 -fb:0 2>&1" &
			PIDS="$PIDS $!"
			i=$[$i + 1]
		fi
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
