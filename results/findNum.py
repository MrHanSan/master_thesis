#!/usr/bin/env python
import sys
import os
import glob

dirs = sys.argv[1:]

root = 0
time = 0
queries = 0
accuracy = 0
total_accuracy = 0
results = 0
total_nodes = 0
highscore = 0
score_list = []
query_miss = 0

for d in dirs:
    os.chdir(d)
    for filename in glob.glob("*.txt"):
        if "Time" not in filename:
            with open(filename) as f:
                for line in f:
                    if "Root nodes Found" in line:
                        root += int(line.split(" ")[-1])
                    elif "Total execution time" in line:
                        time += float(line.split(" ")[-1])
                        if highscore == 0:
                            query_miss += 1
                        else:
                            score_list.append(highscore)
                        highscore = 0
                        queries += 1
                    elif "Score:" in line and "NaN" not in line:
                        accuracy = float(line.split(" ")[-1])
                        total_accuracy += accuracy
                        results += 1
                        if accuracy > highscore:
                            highscore = accuracy
                    elif "Nodes visited" in line:
                        total_nodes += float(line.split(" ")[-1])
    os.chdir("../")

print("avg time/query: " + str(time/queries))
#print("avg time/root: " + str(time/root))
print("avg root/query: " + str(root/queries))
print("avg nodes/root: " + str(total_nodes/root))
print("avg nodes visited/query: " + str(total_nodes/queries))
#print("avg accuracy/root: " + str(total_accuracy/root))
print("avg accuracy/result: " + str(total_accuracy/results))
print("avg top score/query: " + str(sum(score_list)/len(score_list)))
print("")
print("total missed queries: " + str(query_miss))
print("Total queries: " + str(queries))
print("miss percent: " + str(float(query_miss)/queries))
