#!/usr/bin/env python3
import sys
import os
import glob
from pathlib import Path

dirs = sys.argv[1]

traversal_type = {"dist1": [], "dist2": [], "fa": []}

for current_file in Path(dirs).rglob("*.txt"):
    filename = current_file.name
    cwd = str(current_file.resolve())
    if "Time" not in filename:
        with open(current_file) as f:
            for line in f:
                if "Root nodes Found" in line:
                    nodes = str(line.split(": ")[1]).rstrip()
                elif "Total execution time" in line:
                    if int(nodes) == 0:
                        continue
                    time = str(line.split(": ")[1]).rstrip()
                    if "d1" in cwd:
                        traversal_type["dist1"].append(nodes + " " + time)
                    elif "d2" in cwd:
                        traversal_type["dist2"].append(nodes + " " + time)
                    elif "Hits" in cwd:
                        traversal_type["fa"].append(nodes + " " + time)

for t in traversal_type:
    print(t)
    for s in traversal_type[t]:
        print(s)
    print("--------------------")
