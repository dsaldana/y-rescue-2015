#!/bin/bash

#args:
#$1 simulator root; $2 scenario dir; $3 map name (name of directory in maps/gml/)

if (cp "$2/scenario.xml" "$1/maps/gml/$3/scenario.xml"); then
	echo "$2/scenario.xml copied to $1/maps/gml/$3/scenario.xml"
else
	echo "$2/scenario.xml NOT copied to $1/maps/gml/$3/scenario.xml"
fi

if (cp -v "$2"/*.cfg "$1/boot/config/"); then
	echo "$2/*.cfg copied to $1/$1/boot/config/"
else
	echo "$2/*.cfg NOT copied to $1/$1/boot/config/"
fi



