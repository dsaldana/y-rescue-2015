#!/bin/bash

PIDS=
classpath=.:bin:library/util/default/commons-logging-1.1.1.jar:library/util/default/dom4j.jar:library/util/default/jaxen-1.1.1.jar:library/util/default/jcommon-1.0.16.jar:library/util/default/jfreechart-1.0.13.jar:library/util/default/jscience-4.3.jar:library/util/default/jsi-1.0b2p1.jar:library/util/default/jts-1.11.jar:library/util/default/junit-4.5.jar:library/util/default/log4j-1.2.15.jar:library/util/default/rescuecore.jar:library/util/default/resq-fire.jar:library/util/default/resq-fire-core.jar:library/util/default/trove-0.1.8.jar:library/util/default/uncommons-maths-1.2.jar:library/util/default/xml-0.0.6.jar:library/util/guava-18.0.jar:library/rescue/core/adf-util.jar:library/rescue/core/clear.jar:library/rescue/core/collapse.jar:library/rescue/core/gis2.jar:library/rescue/core/handy.jar:library/rescue/core/human.jar:library/rescue/core/ignition.jar:library/rescue/core/kernel.jar:library/rescue/core/maps.jar:library/rescue/core/misc.jar:library/rescue/core/rescuecore2.jar:library/rescue/core/resq-fire.jar:library/rescue/core/sample.jar:library/rescue/core/standard.jar:library/rescue/core/traffic3.jar:library/rescue/core/sources/adf-util-sources.jar:library/rescue/core/sources/clear-sources.jar:library/rescue/core/sources/collapse-sources.jar:library/rescue/core/sources/gis2-sources.jar:library/rescue/core/sources/handy-sources.jar:library/rescue/core/sources/human-sources.jar:library/rescue/core/sources/ignition-sources.jar:library/rescue/core/sources/kernel-sources.jar:library/rescue/core/sources/maps-sources.jar:library/rescue/core/sources/misc-sources.jar:library/rescue/core/sources/rescuecore2-sources.jar:library/rescue/core/sources/resq-fire-sources.jar:library/rescue/core/sources/sample-sources.jar:library/rescue/core/sources/standard-sources.jar:library/rescue/core/sources/traffic3-sources.jar:library/util/sqlite-jdbc-3.8.11.2.jar

echo "Will connect to: $1"

#command: ./start-precompute.sh [SERVER IP]
 
java -Xms3G -Xmx7G -classpath $classpath adk.Main -h:$1 -pre:true -fb:1 -fs:0 -pf:1 -po:0 -at:1 -ac:0

echo "Precompute agents launched. To terminate them, hit Ctrl+C in this terminal."
