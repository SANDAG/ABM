# [Modified from original code provided by Ben Stabler]
#
# Author: 	Renee Alsup
# E-mail: 	alsuprm@pbworld.com
# Date: 	2014-08-11
#
#
# Modified 2021-04-19 JEF to remove absolute paths/filenames, remove walk barriers file.
#
# SUMMARY:
# READS SPATIAL DATA [NETWORK AND FEATURE DATASETS] STORED IN MICROSOFT ACCESS DATABASE, LOADS IT INTO MEMORY
# WRITES THE DATA INTO DICTIONARY OBJECTS, PROCESSES THEM AND WRITES OUT A TEXT FILE WITH K-NEAREST NEIGHBOR 
# FOR EACH NODE IN FEATURE DATASET

########################################################################################################
#Function definitions
########################################################################################################
import math
import os
import osgeo.ogr as ogr
import csv
import time
from rtree import index
import sys
from collections import defaultdict
import re

allStNodes = dict()
allStLinks = dict()

#def knn_nodes(netNode, featureNode, linkLayer, barrierLayer, fileName, featureOffset, walkBarOffset):
def knn_nodes(netNode, featureNode, linkLayer, fileName, featureOffset, walkBarOffset,externalStationNodeList,externalStationStartMgra):
 
    lines = []
    
    # open output file and write header
    probFile = open("./logFiles/DTAPostProcess_ProblemMGRAs.txt",'w')
    outFile = open(fileName, "ab")
    #outFile.write("MGRA,ANodeX,ANodeY,NodeID,BNodeX,BNodeY,LinkType,ControlType,TAZConnector,Distance")
    outFile.write("MGRA,NodeID,Probability")
    outFile.write(os.linesep)

    problems1 = list()
    problems2 = list()
    problems3 = list()

    for feature in featureNode:
        startMap = time.time()
        # Get the set of nodes nearest to the centroid.  The number of nodes in the set is equal to NUM_NEAREST_NODES 
        nearestNodes = list(spIndexMgra.nearest((featureNode[feature][0], featureNode[feature][1], featureNode[feature][0], featureNode[feature][1]), NUM_NEAREST_NODES))
        
        # Choose the nodes to map each MGRA to.  Nodes chosen will be 2 if no nodes are within the max distance and NUM_CHOSEN_NODES otherwise
        nodesAdded=0
        for i in range(0, len(nearestNodes)):
            # calculate the distance between the mgra centroid and the node
            distance = math.sqrt(math.pow(featureNode[feature][0] - netNode[nearestNodes[i]][0], 2.0) + math.pow(featureNode[feature][1] - netNode[nearestNodes[i]][1], 2.0))
            
            # only add the node if it is within the max distance or if there are less than two nodes already mapped for that mgra
            if (distance<=max_dist or nodesAdded<2):

                # Create a geometry object for the connector
                connector = ogr.Geometry(ogr.wkbLineString)
                connector.AddPoint(featureNode[feature][0], featureNode[feature][1])
                connector.AddPoint(netNode[nearestNodes[i]][0], netNode[nearestNodes[i]][1])

                envelope = connector.GetEnvelope()

                # Get the set of 20 links closest to the connector 
                intersectingLinks = list(spIndexLink.intersection(envelope))
#                barrierCheck = checkIntersection(intersectingLinks, linkLayer, barrierLayer, connector, walkBarOffset, False)
                barrierCheck = 0

                # If there are no barriers or freeway links intersected, add the node to the set for that mgra
                if barrierCheck == 0:
                    #lines.append(str(feature-featureOffset) + "," + str(featureNode[feature][0]) + ","+ str(featureNode[feature][1])+ "," + str(nearestNodes[i]) + "," + str(netNode[nearestNodes[i]][0])+ ","+ str(netNode[nearestNodes[i]][1]) + ","+ str(netNode[nearestNodes[i]][2])+ ","+ str(netNode[nearestNodes[i]][3]) + "," + str(nodeConnectors[nearestNodes[i]]) + "," + str(distance) + os.linesep)
                    lines.append(str(feature-featureOffset) + "," + str(nearestNodes[i]))
                    nodesAdded += 1
                else: 
                    continue
            else:
                continue
            # If a sufficient number of nodes have been chosen for the mgra, go onto the next mgra
            if nodesAdded>= NUM_CHOSEN_NODES:
                break

        # Re-run the selection without checking for intersection with node barriers if fewer than two connectors were created
        if nodesAdded<1:
            problems1.append(feature - featureOffset)

            for i in range(0, len(nearestNodes)):
                # calculate the distance between the mgra centroid and the node
                distance = math.sqrt(math.pow(featureNode[feature][0] - netNode[nearestNodes[i]][0], 2.0) + math.pow(featureNode[feature][1] - netNode[nearestNodes[i]][1], 2.0))
            
                # only add the node if it is within the max distance or if there are less than two nodes already mapped for that mgra
                if (distance<=max_dist or nodesAdded<1):

                    # Create a geometry object for the connector
                    connector = ogr.Geometry(ogr.wkbLineString)
                    connector.AddPoint(featureNode[feature][0], featureNode[feature][1])
                    connector.AddPoint(netNode[nearestNodes[i]][0], netNode[nearestNodes[i]][1])

                    envelope = connector.GetEnvelope()

                    # Get the set of 20 links closest to the connector 
                    intersectingLinks = list(spIndexLink.intersection(envelope))
#                    barrierCheck = checkIntersection(intersectingLinks, linkLayer, barrierLayer, connector, walkBarOffset, True)
                    barrierCheck = 0

                    # If there are no barriers or freeway links intersected, add the node to the set for that mgra
                    if barrierCheck == 0:
                        #lines.append(str(feature-featureOffset) + "," + str(featureNode[feature][0]) + ","+ str(featureNode[feature][1])+ "," + str(nearestNodes[i]) + "," + str(netNode[nearestNodes[i]][0])+ ","+ str(netNode[nearestNodes[i]][1]) + ","+ str(netNode[nearestNodes[i]][2])+ ","+ str(netNode[nearestNodes[i]][3]) + "," + str(nodeConnectors[nearestNodes[i]]) + "," + str(distance) + os.linesep)
                        lines.append(str(feature-featureOffset) + "," + str(nearestNodes[i]))
                        nodesAdded += 1
                    else: 
                        continue
                else:
                    continue
                # If a sufficient number of nodes have been chosen for the mgra, go onto the next mgra
                if nodesAdded>= 1:
                    break
        
        # Re-run the selection without any barrier checking if fewer than two connectors were created
        if nodesAdded<1:
            problems2.append(feature - featureOffset)
             
            for i in range(0, len(nearestNodes)):
                # calculate the distance between the mgra centroid and the node
                distance = math.sqrt(math.pow(featureNode[feature][0] - netNode[nearestNodes[i]][0], 2.0) + math.pow(featureNode[feature][1] - netNode[nearestNodes[i]][1], 2.0))
            
                # only add the node if it is within the max distance or if there are less than two nodes already mapped for that mgra
                if (distance<=max_dist or nodesAdded<2):
                    #lines.append(str(feature-featureOffset) + "," + str(featureNode[feature][0]) + ","+ str(featureNode[feature][1])+ "," + str(nearestNodes[i]) + "," + str(netNode[nearestNodes[i]][0])+ ","+ str(netNode[nearestNodes[i]][1]) + ","+ str(netNode[nearestNodes[i]][2])+ ","+ str(netNode[nearestNodes[i]][3]) + "," + str(nodeConnectors[nearestNodes[i]]) + "," + str(distance) + os.linesep)
                    lines.append(str(feature-featureOffset) + "," + str(nearestNodes[i]))
                    nodesAdded += 1
                    break
                else:
                    continue
        
        #print "Node mapping mapped %d nodes for MGRA %d in %s" % (nodesAdded, (feature - featureOffset), (time.time() - startMap))
        
        if nodesAdded<1:  
            problems3.append(feature - featureOffset)
    
    count = dict()
    for line in lines:
        mgra, node = line.split(",")
        if mgra in count:
            count[mgra] = count[mgra] + 1
        else:	
            count[mgra] = 1
            
    newlines = []
    for line in lines:
    	  mgra, node = line.split(",")
    	  newlines.append(line+","+"%5.2f" % (1.0/count.get(mgra))+os.linesep)
    
    # write external station nodes with probability of 1.0	
    mgra= int(externalStationStartMgra)
    externalStationList = externalStationNodeList.split(",")
    for node in externalStationList:
        newlines.append(str(mgra)+","+node+",1.0"+os.linesep)
        mgra = mgra + 1
    	
    outFile.writelines(newlines)
    outFile.close()
    probFile.write("Found %d MGRAs that can't find connections with both restrictions" % len(problems1))
    probFile.write(os.linesep)
    probFile.write(str(problems1))
    probFile.write(os.linesep)
    probFile.write("Found %d MGRAs that can't find connections with walk barriers restriction relaxed" % len(problems2))
    probFile.write(os.linesep)
    probFile.write(str(problems2))
    probFile.write(os.linesep)

# JEF 2021-04-19: now consistent with py3

    print "%d MGRAs with less than 1 connection with all restrictions: %s" % (len(problems1),problems1)
    print "%d MGRAs with less than 1 connection with only freeway restrictions: %s" % (len(problems2),problems2)
    print "%d MGRAs with less than 1 connection with no restrictions: %s" % (len(problems3),problems3)
    return 0

def checkIntersection(crossLinks, linkLayer, barrierLayer, connector, barrierOffset, barrierSkip):
    
    if len(crossLinks) == 0:
        return 0

    for j in range(0, len(crossLinks)):
        if crossLinks[j]>= barrierOffset:
            if barrierSkip is True:
                continue
            featNum = crossLinks[j] - barrierOffset -1
            feature = barrierLayer.GetFeature(featNum) 
        else:
            featNum = crossLinks[j] - 1
            feature = linkLayer.GetFeature(featNum)       

        if feature is not None:
            # create a geometry object from the current feature and check if it intersects with the line input
            feature_line = feature.GetGeometryRef()
            if connector.Intersects(feature_line):
                return 1 
    return 0

def writeCentroidDict(datasetName,offset,nField):
    # create the dictionary to be filled
    nodeDict = dict()

    # open the shapefile and import the layer
    dataset_in = ogr.Open(datasetName)
    if dataset_in is None:
        print "Open MGRA Centroid layer failed."
        sys.exit(2)
    layer_in = dataset_in.GetLayer(0)

    # get the first feature from the layer
    feature_in = layer_in.GetNextFeature()

    # loop over input features, calculate centroid and save the x and y coordinates to the dictionary
    while feature_in is not None:
        centroid = feature_in.GetGeometryRef()
        n = offset + feature_in.GetField(nField)
        xCoord = centroid.GetX()
        yCoord = centroid.GetY()
        nodeDict[n] = (xCoord, yCoord)
        feature_in = layer_in.GetNextFeature()
    dataset_in.Destroy()
    return nodeDict

def writeMGRADict(datasetName,offset,nField):

    # create the dictionary to be filled
    nodeDict = dict()

    # open the shapefile and import the layer
    dataset_in = ogr.Open(datasetName)
    if dataset_in is None:
        print "Open MGRA layer failed."
        sys.exit(2)
    layer_in = dataset_in.GetLayer(0)

    # get the first feature from the layer
    feature_in = layer_in.GetNextFeature()

    # loop over input features, calculate centroid and save the x and y coordinates to the dictionary
    while feature_in is not None:
        geom = feature_in.GetGeometryRef()
        centroid = geom.Centroid()
        n = offset + feature_in.GetField(nField)
        xCoord = centroid.GetX()
        yCoord = centroid.GetY()
        nodeDict[n] = (xCoord, yCoord)
        feature_in = layer_in.GetNextFeature()
    dataset_in.Destroy()
    return nodeDict

def get_property(properties_file_name, properties_file_contents, propname):
    """
    Return the string for this property.
    Exit if not found.
    """
    match           = re.search("\n%s[ \t]*=[ \t]*(\S*)[ \t]*" % propname, properties_file_contents)
    if match == None:
        print "Couldn't find %s in %s" % (propname, properties_file_name)
        sys.exit(2)
    return match.group(1)


########################################################################################################
#Main Program Area
########################################################################################################
#Definitions

args = sys.argv[1:]

start       = time.time()

#if len(args) != 6:
#    print "Node to MGRA Correspondence requires six arguments: node exclusions file, node filepath, link filepath, zone filepath, walk barriers filepath, and output csvfilepath"
#    sys.exit(1)

propertiesFileName = "./conf/sandag_abm.properties"
myfile = open( propertiesFileName, 'r' )
myfile_contents = myfile.read()
myfile.close()

nodeExclusions = get_property(propertiesFileName, myfile_contents, "dta.nodeExclusions.file")
nodeFilepath  = get_property(propertiesFileName, myfile_contents, "dta.node.file")
linkFilepath  = get_property(propertiesFileName, myfile_contents, "dta.link.file") 
mgraFilepath = get_property(propertiesFileName, myfile_contents, "dta.mgra.file") 
outFilepath = get_property(propertiesFileName, myfile_contents, "dta.postprocessing.NodeFile")

externalStationNodeList = get_property(propertiesFileName, myfile_contents, "dta.externalStation.nodes")
externalStationStartMgra = get_property(propertiesFileName, myfile_contents, "dta.externalStation.startMgra")


MGRANodeMap_FILE = outFilepath
if(os.path.exists(MGRANodeMap_FILE)): os.remove(MGRANodeMap_FILE)

NUM_NEAREST_NODES  = 20                             # Number of nearest network nodes to use for creating feature connectors
NUM_CHOSEN_NODES   = 4                              # Number of nodes to map each MGRA to
max_dist = 26400                                    # Maximum distance from node to centroid in feet 
spIndexMgra = index.Index()                         # Create spatial indexes for MGRA candidate nodes.
spIndexLink = index.Index(interleaved = False)      # Create spatial indexes for barrier links (freeway links and walk barriers)
mgraOffset = 100000                                 # Specify offsets to filter out MGRA links later on
walkBarOffset = 100000                              # Specify offset to keep walk barrier and freeway links separate




# Set functional classification priority for assigning functional class to nodes
priority1 = 1
priority2 = (1,8,9,11,12,13)

nodeExclude = []

nodeFacType = defaultdict(int)
nodeConnectors = defaultdict(int)

# Reading in the set of nodes not eligible for mapping MGRAs
print "Reading the node exclusions file..."

nodeExclusionsFile = open(nodeExclusions, mode="r")

for rowVal in csv.DictReader(nodeExclusionsFile):
    nodeId = int(rowVal['ExcludeNodeIds'])
    nodeExclude.append(nodeId)

nodeExclusionsFile.close()

# Opening the link, walk barriers, and node shapefiles.
print "Opening the link, walk barriers, and node shapefiles..."
# Open links shapefile
driverLinks = ogr.GetDriverByName('ESRI Shapefile')
datasetLinks_in = driverLinks.Open(linkFilepath+".shp",0)
if datasetLinks_in is None:
    print "Open Link layer failed."
    sys.exit(2)
layerLinks_in = datasetLinks_in.GetLayer(0)

# JEF 2021-04-19 remove
#Open walk barriers shapefile
#driverBar = ogr.GetDriverByName('ESRI Shapefile')
#datasetBar_in = driverBar.Open(walkBarFilepath+".shp",0)
#if datasetBar_in is None:
#    print "Open Walk Barriers layer failed."
#    sys.exit(2)
#layerBar_in = datasetBar_in.GetLayer(0)

# Open nodes shapefile
driverNode = ogr.GetDriverByName('ESRI Shapefile')
datasetNode_in = driverNode.Open(nodeFilepath+".shp",0)
if datasetNode_in is None:
    print "Open Node layer failed."
    sys.exit(2)
layerNode_in = datasetNode_in.GetLayer(0)


print "Reading links and tagging A and B node with IFC and adding freeway links to spatial index..."

featNum = 0
for feature in layerLinks_in:
    featNum += 1
    # Set node keys, link Id, and functional class
    nodeKeyA = int(feature.GetField("AN"))
    nodeKeyB = int(feature.GetField("BN"))
    IdNum = int(feature.GetField("HWYCOV_ID"))
    IFC = int(feature.GetField("IFC"))  

    # If the link is a freeway, the node is tagged as freeway 
    # and the link start and end nodes are added to the link spatial index
    if IFC==1:
        nodeFacType[nodeKeyA]=1
        nodeFacType[nodeKeyB]=1  
        
        feature_line = feature.GetGeometryRef()
        envelope = feature_line.GetEnvelope()       
        spIndexLink.insert(featNum, envelope)

    # If the link is a prime arterial
    # and the node's current classification is not any of the priority classifications, the node is tagged as prime arterial
    elif IFC==2:
        if not(nodeFacType[nodeKeyA] in priority2):
            nodeFacType[nodeKeyA] = 2
        if not(nodeFacType[nodeKeyB] in priority2):
            nodeFacType[nodeKeyB] = 2
    # If the link is an arterial, collector, or local road
    # and the node's current classification is not any of the priority classifications
    # and the link facility type is less than than the current node facility type, the node is tagged with that link's facility type
    elif IFC>=3 and IFC<=7:
        if not(nodeFacType[nodeKeyA] in priority2) and (IFC<nodeFacType[nodeKeyA] or nodeFacType[nodeKeyA]==0):
            nodeFacType[nodeKeyA] = IFC
        if not(nodeFacType[nodeKeyB] in priority2) and (IFC<nodeFacType[nodeKeyB] or nodeFacType[nodeKeyB]==0):
            nodeFacType[nodeKeyB] = IFC

    # If the link is a ramp or bus link and the node is not already tagged as a freeway, the node is taggedwith its 
    elif IFC==8 or IFC==9 or IFC==12:
        if not(nodeFacType[nodeKeyA]==priority1):
            nodeFacType[nodeKeyA] = IFC
        if not(nodeFacType[nodeKeyB]==priority1):
            nodeFacType[nodeKeyB] = IFC
    # If the link is a zone connector and the node has not been tagged with any other classification, it is tagged as a connector
    elif IFC==10:
        nodeConnectors[nodeKeyA]=1
        nodeConnectors[nodeKeyB]=1
        if not(nodeFacType[nodeKeyA]>0):
            nodeFacType[nodeKeyA]=10
        if not(nodeFacType[nodeKeyB]>0):
            nodeFacType[nodeKeyB]=10

print "Reading nodes and creating spatial index..."
        
for feature in layerNode_in:
    IdNum = int(feature.GetField("HWYCOV_ID"))
    controlType = int(feature.GetField("ICNT"))
    xCoord = feature.GetField("X_COORD")
    yCoord = feature.GetField("Y_COORD")        
    linkType = nodeFacType[IdNum]
    allStNodes[IdNum] = (xCoord, yCoord,linkType,controlType)

    # Create spatial index for nodes that are not tagged as freeway, ramp, or connectors, are not signalized, and are not in the exclusions file
    if not(linkType in (1,8,9,10,12)) and not(controlType in (1,4,5,6)) and not(IdNum in nodeExclude):
        spIndexMgra.insert(IdNum, (xCoord, yCoord, xCoord, yCoord))

#print "Reading barrier links and adding to link spatial index ..."
#
#IdNum = walkBarOffset
#for feature in layerBar_in:
#    IdNum += 1
#    feature_line = feature.GetGeometryRef()
#    envelope = feature_line.GetEnvelope()       
#    # Add barrier link to spatial index
#    spIndexLink.insert(IdNum, envelope)

# Create a dictionary for the MGRA layer
#mgra = writeMGRADict(mgraFilepath+".shp",mgraOffset,0)
mgra = writeCentroidDict(mgraFilepath+".shp",mgraOffset,"MGRA")

# Find k-nearest neighbor and write to file.
print "Evaluating nearest nodes..."
#knn_nodes(allStNodes, mgra, layerLinks_in, layerBar_in, MGRANodeMap_FILE, mgraOffset, walkBarOffset)
knn_nodes(allStNodes, mgra, layerLinks_in, MGRANodeMap_FILE, mgraOffset, walkBarOffset,externalStationNodeList,externalStationStartMgra)

datasetLinks_in.Destroy()
#datasetBar_in.Destroy()
datasetNode_in.Destroy()

print "Created MGRA Connections in %5.2f mins" % ((time.time() - start)/60.0)
