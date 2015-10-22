#! /usr/bin/python

# heat_low 	= 1.0
# heat_med 	= 1.0
# heat_high = 1.0

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
import pygame
import re
import sys
import json
import time
import math
import argparse
import os.path
from threading import Thread
import copy

sys.setrecursionlimit(1500)

BLACK = (0,   0,   0)
WHITE = (255, 255, 255)
GREEN = (0, 255,   0)
RED = (255,   0,   0)
BLUE = (0,  60,   255)
YELLOW = (255, 255,   0)

IMAGE_WIDTH = 800
IMAGE_HEIGHT = 600

agent_entity_id = ''
heatmap_file_name = ''

parser = argparse.ArgumentParser()

parser.add_argument('-id', action='store', dest='entity_id', type=int,
                    help='EntityID of the agent')

args = parser.parse_args()

if not (args.entity_id):
    parser.error('No agent ID defined!')

agent_entity_id = args.entity_id

heatmap_file_name = '/tmp/heatmap_' + str(agent_entity_id) + '.txt'
if(not os.path.exists(heatmap_file_name)):
    print "The file ", heatmap_file_name, "do not exists, verify the agent ID!"
    sys.exit(2)

pygame.init()
mono_font = pygame.font.SysFont("monospace", 12)
clock = pygame.time.Clock()

screen = pygame.display.set_mode( (IMAGE_WIDTH, IMAGE_HEIGHT) )
monoFont = pygame.font.SysFont("monospace", 14)

biggest_val_x = IMAGE_WIDTH
biggest_val_y = 250

smallest_val_x = 0
smallest_val_y = 0

map_centroid= (350, biggest_val_y)
movement_map = (0, 0)

background = pygame.Surface((biggest_val_x + (map_centroid[0] * 2), biggest_val_y + (map_centroid[1] * 2)))
background = background.convert()
background.fill(BLACK)

is_loop_active = True

heat_map = {}
heat_map['can_write'] = True

conversion_factor = 300

class update_heat_thread(Thread):
    def __init__ (self, file_name):
        Thread.__init__(self)
        self.file_name = file_name
        self.is_active = True
        
    def run(self):
        while(self.is_active):
            try:
                self.update_heat_map()
                print "Heat_map updated"
            except Exception:
                print "Exception..."
            time.sleep(1)
            pass
        print "Exiting thread"

    def rotate2d(self, degrees, point, origin):
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

    def update_heat_map(self):
        global heat_map
        global biggest_val_y
        global biggest_val_x
        global smallest_val_y
        global smallest_val_x

        if(heat_map['can_write'] == False):
            return

        heat_map = {}
        heat_map['complete'] = False
        heat_map['can_write'] = True

        heat_data = open(self.file_name)
        for line in heat_data:

            values = line.split()
            #print values

            x_pos = (-1 * int(int(values[1])/conversion_factor)) + conversion_factor*2
            y_pos = int(int(values[2])/conversion_factor)

            x0, y0 = self.rotate2d(180, (x_pos, y_pos), map_centroid)
            centroid = ( int(x0), int(y0))

            edge_number = values[3]
            edges_array = values[4:-1]
            edges = []
            for x in xrange(len(edges_array) / 2):
                x1 = (-1 * (int(edges_array[x * 2])/conversion_factor)) + conversion_factor*2
                y1 = (int(edges_array[(x * 2) + 1])/conversion_factor)

                x2, y2 = self.rotate2d(180, (x1, y1), map_centroid)

                edges.append( (int(x2), int(y2)) )

                if(int(x2) > biggest_val_x):
                    biggest_val_x = int(x2)
                if(int(y2) > biggest_val_y):
                    biggest_val_y = int(y2)

                if(int(y2) < smallest_val_x):
                    smallest_val_x = int(y2)
                if(int(y2) < smallest_val_y):
                    smallest_val_y = int(y2)
                

            info = {
                'edges' : edges,
                'heat_value' : values[-1],
                'entity_id' : values[0],
                'centroid' : centroid
            }

            heat_map[values[0]] = info
            pass

        heat_data.close()
        heat_map['complete'] = True

thread_01 = update_heat_thread(heatmap_file_name)
thread_01.daemon = True
thread_01.start()

local_heat_map = {}

while is_loop_active:

    if('complete' not in heat_map or heat_map['complete'] != True): 
        print "not complete"
    else:
        heat_map['can_write'] = False
        local_heat_map = copy.deepcopy(heat_map)
        heat_map['can_write'] = True
         

    #print local_heat_map

    background.fill(BLACK)
    screen.fill(BLACK)

    if(smallest_val_y < 0 and map_centroid[1] != IMAGE_HEIGHT):
        map_centroid = (map_centroid[0], IMAGE_HEIGHT)
        print "map centroid updated", map_centroid, "biggest_val_y", biggest_val_y

    #print "biggest_val_y", biggest_val_y
    #print "smallest_val_y", smallest_val_y

    if(background.get_width() < biggest_val_x or background.get_height() < biggest_val_y):
        background_size = (biggest_val_x + (map_centroid[0] * 2), biggest_val_y + (map_centroid[1] * 2))
        print "surface updated", background_size
        background = pygame.Surface(background_size)
    
    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            is_loop_active = False
            thread_01.is_active = False
        if event.type == pygame.MOUSEMOTION:
            if event.buttons[0]:
                rel = event.rel
                #if(movement_map[0] + rel[0] >= 0 or movement_map[0] + rel[0] < IMAGE_WIDTH):
                #    movement_map = ( (movement_map[0] + rel[0]), (movement_map[1]) )
                #if(movement_map[1] + rel[1] >= 0 or movement_map[1] + rel[1] < IMAGE_HEIGHT):
                #    movement_map = ( (movement_map[0]), (movement_map[1] + rel[1]) )

                movement_map = ( (movement_map[0] + rel[0]), (movement_map[1] + rel[1]) )

    for i in local_heat_map.keys():
        if i not in local_heat_map or i == 'can_write' or i == 'complete':
            continue

        heat_info = local_heat_map[i]

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
    #bg2 = background.subsurface(movement_map[0], movement_map[1], IMAGE_WIDTH, IMAGE_HEIGHT)
    #screen.blit(background, (0, 0))
    screen.blit(background, movement_map)

    pygame.display.update()
    pygame.display.set_caption('FPS: ' + str(clock.get_fps()))
    clock.tick(15)
    heat_map['can_write'] = True

    pass

thread_01.is_active = False
thread_01.join()
