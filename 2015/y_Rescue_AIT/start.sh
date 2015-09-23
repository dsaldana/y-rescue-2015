#!/bin/bash

PIDS=
classpath=.:bin:library/util/default/commons-logging-1.1.1.jar:library/util/default/dom4j.jar:library/util/default/jaxen-1.1.1.jar:library/util/default/jcommon-1.0.16.jar:library/util/default/jfreechart-1.0.13.jar:library/util/default/jscience-4.3.jar:library/util/default/jsi-1.0b2p1.jar:library/util/default/jts-1.11.jar:library/util/default/junit-4.5.jar:library/util/default/log4j-1.2.15.jar:library/util/default/rescuecore.jar:library/util/default/resq-fire.jar:library/util/default/resq-fire-core.jar:library/util/default/trove-0.1.8.jar:library/util/default/uncommons-maths-1.2.jar:library/util/default/xml-0.0.6.jar:library/util/guava-18.0.jar:library/rescue/core/adf-util.jar:library/rescue/core/clear.jar:library/rescue/core/collapse.jar:library/rescue/core/gis2.jar:library/rescue/core/handy.jar:library/rescue/core/human.jar:library/rescue/core/ignition.jar:library/rescue/core/kernel.jar:library/rescue/core/maps.jar:library/rescue/core/misc.jar:library/rescue/core/rescuecore2.jar:library/rescue/core/resq-fire.jar:library/rescue/core/sample.jar:library/rescue/core/standard.jar:library/rescue/core/traffic3.jar:library/rescue/core/sources/adf-util-sources.jar:library/rescue/core/sources/clear-sources.jar:library/rescue/core/sources/collapse-sources.jar:library/rescue/core/sources/gis2-sources.jar:library/rescue/core/sources/handy-sources.jar:library/rescue/core/sources/human-sources.jar:library/rescue/core/sources/ignition-sources.jar:library/rescue/core/sources/kernel-sources.jar:library/rescue/core/sources/maps-sources.jar:library/rescue/core/sources/misc-sources.jar:library/rescue/core/sources/rescuecore2-sources.jar:library/rescue/core/sources/resq-fire-sources.jar:library/rescue/core/sources/sample-sources.jar:library/rescue/core/sources/standard-sources.jar:library/rescue/core/sources/traffic3-sources.jar

echo "Host: $1."
echo "Will start:"
echo "$2 Firefighters,"
echo "$3 Fire Stations,"
echo "$4 Policemen,"
echo "$5 Police Offices,"
echo "$6 Ambulances and"
echo "$7 Ambulance Centers."

#command: ./start.sh [SERVER IP] [TEAM NAME] [NUMBER OF FIREFIGHTERS] [NUMBER OF FIRESTATIONS] [NUMBER OF POLICEMEN] [NUMBER OF POLICE OFFICES] [NUMBER OF AMBULANCES] [NUMBER OF AMBULANCE CENTERS]
#number of AGENTS (-1 = Max)
 
java -Xms1G -Xmx2G -classpath $classpath adk.Main -h:$1 -fb:$2 -fs:$3 -pf:$4 -po:$5 -at:$6 -ac:$7

echo "Agents launched. To terminate them, hit Ctrl+C in this terminal."

