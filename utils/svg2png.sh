#!/usr/bin/python3
import sys
import os

sizes = {'drawable': 32, 'drawable-hdpi': 48, 'drawable-xhdpi':64, 'drawable-xxhdpi': 96, 'drawable-xxxhdpi': 128 }

inputArgs = sys.argv[1:]

def convert (svg):
  for key in sizes:
    print(key, '->', sizes[key])
    filename = os.path.splitext(os.path.basename(svg))[0]
    cmd = f"inkscape -z -w {sizes[key]} -h {sizes[key]} {svg} -e orion-viewer/src/main/res/{key}/{filename}.png"
    #cmd = f"convert  -resize {sizes[key]}x{sizes[key]} {svg} orion-viewer/src/main/res/{key}/help.png"
    print(cmd)
    os.system(cmd)

for i in inputArgs:
    convert(i)