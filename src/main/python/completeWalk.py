import os
import csv
import pandas as pd
import numpy as np
import sys

scenarioYear=sys.argv[1]
inputPath=sys.argv[2]
outputPath=sys.argv[3]
    
fullList = np.array(pd.read_csv(inputPath + 'mgra13_based_input' + str(scenarioYear) + '.csv')['mgra'])
workList = np.array(pd.read_csv(outputPath + 'walkMgraEquivMinutes.csv')['i'])
    
list_set = set(workList)
unique_list = (list(list_set))
notMatch = [x for x in fullList if x not in unique_list]
    
if notMatch:
    with open(outputPath + 'walkMgraEquivMinutes.csv', 'ab') as csvfile:
        spamwriter = csv.writer(csvfile)
        #spamwriter.writerow([])
        for item in notMatch:
            # pdb.set_trace()
            spamwriter.writerow([item,item,'30','30','30'])
