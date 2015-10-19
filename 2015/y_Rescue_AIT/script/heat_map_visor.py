#! /usr/bin/python

# heat_low 	= 1.0
# heat_med 	= 1.0
# heat_high 	= 1.0

# for i in xrange(50):
# 	heat_low = heat_low - (heat_low * 0.07)
# 	heat_med = heat_med - (heat_med * 0.05)
# 	heat_high = heat_high - (heat_high * 0.03)
# 	print i, "heat low  :",  heat_low
# 	print i, "heat med  :",  heat_med
# 	print i, "heat high :",  heat_high
# 	print "\n"

from math import sin, cos, pi, sqrt, atan2, degrees
import random
from shapely.geometry import *
from shapely import affinity
import pygame
import re
import sys
from shapely.geometry import *
from shapely import affinity
import json
import time
import math

sys.setrecursionlimit(1500)

BLACK = (0,   0,   0)
WHITE = (255, 255, 255)
GREEN = (0, 255,   0)
RED = (255,   0,   0)
BLUE = (0,  60,   255)
YELLOW = (255, 255,   0)

IMAGE_WIDTH = 800
IMAGE_HEIGHT = 600

pygame.init()
mono_font = pygame.font.SysFont("monospace", 12)
clock = pygame.time.Clock()

screen = pygame.display.set_mode( (IMAGE_WIDTH, IMAGE_HEIGHT) )
monoFont = pygame.font.SysFont("monospace", 14)

background = pygame.Surface(screen.get_size())
background = background.convert()
background.fill(BLACK)

map_centroid= (350, 250)

is_loop_active = True

heat_map = {}

conversion_factor = 300

def rotate2d(degrees,point,origin):
    """
    A rotation function that rotates a point around a point
    to rotate around the origin use [0,0]
    """
    x = point[0] - origin[0]
    yorz = point[1] - origin[1]
    newx = (x*math.cos(math.radians(degrees))) - (yorz*math.sin(math.radians(degrees)))
    newyorz = (x*math.sin(math.radians(degrees))) + (yorz*math.cos(math.radians(degrees)))
    newx += origin[0]
    newyorz += origin[1]

    return newx,newyorz

def update_heat_map():
    global heat_map

    heat_data = open('/tmp/heatmap_941331988.txt')
    for line in heat_data:

        values = line.split()
        print values

        x_pos = (-1 * int(int(values[1])/conversion_factor)) + conversion_factor*2
        y_pos = int(int(values[2])/conversion_factor)

        x0, y0 = rotate2d(180, (x_pos, y_pos), map_centroid)
        centroid = ( int(x0), int(y0))

        edge_number = values[3]
        edges_array = values[4:-1]
        edges = []
        for x in xrange(len(edges_array) / 2):
            x1 = (-1 * (int(edges_array[x * 2])/conversion_factor)) + conversion_factor*2
            y1 = (int(edges_array[(x * 2) + 1])/conversion_factor)

            x2, y2 = rotate2d(180, (x1, y1), map_centroid)

            edges.append( (int(x2), int(y2)) )

        info = {
            'edges' : edges,
            'heat_value' : values[-1],
            'entity_id' : values[0],
            'centroid' : centroid
        }

        heat_map[values[0]] = info
        pass

    heat_data.close()

update_heat_map()
print heat_map;

while is_loop_active:

    background.fill(BLACK)
    update_heat_map()

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            is_loop_active = False
        if event.type == pygame.MOUSEMOTION:
            if event.buttons[0]:
                rel = event.rel
                map_centroid = ( (map_centroid[0] + rel[0]), (map_centroid[1] + rel[1]) )

    for i in heat_map.keys():
        heat_info = heat_map[i]

        #print heat_info

        #heat_radius = float(heat_value) * 10
        #print "heat_radius", int(heat_radius)
        #pygame.draw.circle(background, WHITE, i, int(heat_radius))
        red_value = int(255 * float(heat_info['heat_value']))
        
        if red_value > 255:
            red_value = 255
        if red_value < 0:
            red_value = 0

        pygame.draw.polygon(background, (red_value,   0,   0), heat_info['edges'], 0)
        pygame.draw.polygon(background, WHITE, heat_info['edges'], 1)

        pygame.draw.circle(background, WHITE, heat_info['centroid'], 1)

        label = monoFont.render(str(heat_info['entity_id']), 2, YELLOW)
        background.blit(label, heat_info['centroid'])
        pass

    #background = pygame.transform.flip(background, 0, 1)
    screen.blit(background, (0, 0))

    pygame.display.update()
    pygame.display.set_caption('FPS: ' + str(clock.get_fps()))
    clock.tick(3)
    #time.sleep(1)

    pass