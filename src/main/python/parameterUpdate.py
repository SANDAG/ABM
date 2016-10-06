# Author:Yun.Ma@sandag.org
# Oct 5,2016

#import
import os
import csv
import string
import sys

# check if property file exists
if not os.path.isfile('..\\conf\\sandag_abm.properties'):
    print "Property File Not Found"
    raise sys.exit()

# search scenarioYear
TheYear=''
propFile = open('..\\conf\\sandag_abm.properties','r')
for line in propFile:
    if line.find('scenarioYear') > -1:
         TheYear = line.strip('\n').split('=')[1]
         break
else:
    print "scenarioYear Not Found"
propFile.close()

# read csv file
ParaDict={}
csvFileIn = open('..\\input\\parametersByYears.csv','rU')
reader = csv.DictReader(csvFileIn)
for row in reader:
    if row['year'] == TheYear:
        ParaDict=row.copy()
        break
csvFileIn.close()
ParaDict.pop('year',ParaDict['year'])

# read and update str in property file
OldVal=''
NewVal=''
Paralines=[]

propInFile = open('..\\conf\\sandag_abm.properties','r')
for line in propInFile:
    for key in ParaDict:
        if line.find(key) > -1:
            NewVal = ParaDict[key]
            print NewVal
            OldVal = line.strip('\n').split('=')[1]
            line = string.replace(line,OldVal,NewVal)
            print line
            break
    Paralines.append(line)
propInFile.close()

# write into property file
propOutFile = open('..\\conf\\sandag_abm.properties','w')
for line in Paralines:
    propOutFile.write(line)
propOutFile.close()

