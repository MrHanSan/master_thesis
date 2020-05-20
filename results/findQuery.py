#!/usr/bin/env python3
import sys
import os
import glob
from pathlib import Path

dirs = sys.argv[1]

queries = {}
current = ""

for current_file in Path(dirs).rglob("*.txt"):
    filename = current_file.name
    if "Time" not in filename:
        with open(current_file) as f:
            for line in f:
                if "Query Words" in line:
                    #current = filename.split(".")[0] + line.split(":")[1]
                    current = line.split(":")[1]
                    if current not in queries:
                        queries[current] = 0
                elif "Score:" in line:
                    queries[current] += 1

print(queries)
