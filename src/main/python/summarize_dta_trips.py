# Initial revision 2014 June 17 Renee Alsup
# Used to summarize output from SANDAG tod disaggregation

#from tables import openFile, IsDescription, Int32Col, Float32Col, Filters
from collections import defaultdict
import time
import csv
import os, shutil, sys

if __name__ == '__main__':

    args = sys.argv[1:]

    if len(args) != 2:
        print "Two arguments required: input file and output file."
        sys.exit(1)

    inFileName = args[0]
    outFileName = args[1]
    
    dtaTrips = defaultdict(float)
    dtaPeriod = defaultdict(float)

    inputFile = open(inFileName, mode="r")
    outputFile = open(outFileName,'w')

    for row in csv.DictReader(inputFile):

        expFac = float(row['expansionFactor'])
        
        perKey = (row['dtaPeriod'],row['marketSegment'],row['vehicleType'])
        dtaPeriod[perKey] += expFac
        tod = "None"
        if(int(row['dtaPeriod'])>=1 and int(row['dtaPeriod'])<=36):
            tod = "EA"
        elif(int(row['dtaPeriod'])>36 and int(row['dtaPeriod'])<=72):
            tod = "AM"
        elif(int(row['dtaPeriod'])>72 and int(row['dtaPeriod'])<=150):
            tod = "MD"
        elif(int(row['dtaPeriod'])>150 and int(row['dtaPeriod'])<=192):
            tod = "PM"
        elif(int(row['dtaPeriod'])>192):
            tod = "EV"


        keyTrips = (row['marketSegment'],tod,row['vehicleType'],int(row['vehicleOccupancy']),int(row['tollEligibility']))
        dtaTrips[keyTrips] += expFac 
    

           
    inputFile.close()
    
    outputFile.write("marketSegment,TOD,vehicleType,vehicleOccupancy,tollEligibility,dtaTrips")
    outputFile.write("\n")
    for key,val in dtaTrips.iteritems():
        outputFile.write("%s,%s,%s,%d,%d,%0.8f" % (key[0],key[1],key[2],key[3],key[4],val))
        outputFile.write("\n")

    outputFile.write("***********************************************************************")
    outputFile.write("\n")
    outputFile.write("marketSegment,dtaPeriod,vehicleType,numTrips")
    outputFile.write("\n")
    for key,val in dtaPeriod.iteritems():
        outputFile.write("%s,%s,%s,%0.8f" % (key[1], key[0], key[2], val))
        outputFile.write("\n")
    outputFile.close()

