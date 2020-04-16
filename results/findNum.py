#!/usr/bin/env python
import sys, os, glob


dir  = sys.argv[1]

os.chdir(dir)
root = 0
time = 0
queries = 0

for filename in glob.glob("*.txt"):
    if "Time" not in filename:
        print(filename)
        with open(filename) as f:
            for line in f:
                if "Root nodes Found" in line:
                    root += int(line.split(" ")[-1])
                if "Total execution time" in line:
                    queries += 1
                    time += int(line.split(" ")[-1])
    print(root)
    print(time)
    print(queries)

print("\ntotals: ")
print("avg time/query: " + str(time/queries))
print("avg time/root: " + str(time/root))
print("avg root/query: " + str(root/queries))
