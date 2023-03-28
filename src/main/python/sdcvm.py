'''
Created on 2010-04-20

@author: Kevin
'''


# Python libraries
import copy
import csv
import math
import random
import time
from array import array
from optparse import OptionParser


# CSTDM libraries
import sdcvm_settings as settings


class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"




def logitNestToLogsums(nestDict):

    #print "Evaluating logit tree structure"
    # Logit tree evaluation bit
    #
    # What this does is start with a dictionary where each key corresponds to a node on a
    # nested logit tree, including the special code "top" which is the top of the tree
    # and the values for utility for each of the alternatives at the bottom of the tree.
    # When the dictionary starts out, the nest nodes should have a list of connections; 
    # each connection is itself a list of [lower node, nest coefficient]. 
    #
    # For instance, a classic mode choice nest would look like:
    # nestDict = {'top': [ ['auto', 0.8], ['transit', 0.5], ['walk', 1.0] ]
    #             'auto': [ ['sov', 1.0], ['hov', 0.75] ]
    #             'transit': [ ['bus', 0.2], ['lrt', 0.2] ]
    #             'hov': [ ['hov 2', 0.6], ['hov 3', 0.6] ]
    #             'walk': -4.2911
    #             'sov': 1.0109
    #             'hov 2': -1.201
    #             'hov 3': -1.415
    #             'bus': -3.504
    #             'lrt': -2.879 }
    #
    # The loop then iterates through the keys of the dictionary (nodes).
    #   - If the key contains only a float (e.g. 'walk' above), this is assumed to be the 
    #     utility or logsum, and nothing more needs to be done.
    #   - If the key contains any list elements (connections), the nodes they refer to will be checked;
    #     if the node it refers to contains only a float (e.g. the ['bus', 0.2] connection
    #     for the transit nest above, where 'bus' contains the float -3.504), then the nested utility
    #     will be multiplied by the coefficient and replace the list element. (e.g. -3.504 * 0.2 = -.7008)
    #     if the node it refers to contains a list (e.g. the ['hov', 0.75] connection above), nothing is done.  
    #   - If the key contains all values as floats (not the case right now with the example above, but after
    #     the first run through, the 'hov' and 'transit' nodes will be in this state), then the logsum is taken
    #     of all of the floats, and this replaces the node value.
    #
    # After the first iteration, the mode choice nest would have:
    # nestDict = {'top': [ ['auto', 0.8], ['transit', 0.5], -4.2911 ]
    #             'auto': [ 1.0109, ['hov', 0.75] ]
    #             'transit': [ -0.7008, -0.5758 ]
    #             'hov': [ -0.7206, -0.849 ]
    #             'walk': -4.2911
    #             'sov': 1.0109
    #             'hov 2': -1.201
    #             'hov 3': -1.415
    #             'bus': -3.504
    #             'lrt': -2.879 }
    #
    # After the second iteration, the mode choice nest would have:
    # nestDict = {'top': [ ['auto', 0.8], ['transit', 0.5], -4.2911 ]
    #             'auto': [ 1.0109, ['hov', 0.75] ]
    #             'transit': 0.0568
    #             'hov': -0.0896
    #             'walk': -4.2911
    #             'sov': 1.0109
    #             'hov 2': -1.201
    #             'hov 3': -1.415
    #             'bus': -3.504
    #             'lrt': -2.879 }
    #
    #        
    # In essence, what happens is that the utilities are passed up; when all of the utilities for a node 
    # have been calculated, then the logsum is taken at that node. The nest coefficients are always multiplied
    # so a value of 1 needs to be specified if no other value is to be used.
    #
    # Note that this, in the way it works, destroys the connection information, replacing it by logsums
    # at the node location instead. So a copy (not a reference, which nestDict = keepDict would do) is needed
    # if the connection information needs to be used again.
    
    x = 0
    nodeList = list(nestDict.keys())
    
    # Do this until the top node has been resolved into a float
    while isinstance(nestDict["top"], list) is True:
        #print 40 * "."
        for node in nodeList:
            #print node, nestDict[node]
            if isinstance(nestDict[node], float): # if it's already only a float, it's a utility or logsum; do nothing
                pass
            else: 
                countFloat = 0
                countSub = 0
                
                for sn in range(len(nestDict[node])):
                    if isinstance(nestDict[node][sn], float):
                        countFloat = countFloat + 1
                    elif isinstance(nestDict[node][sn], list):
                        subName = nestDict[node][sn][0]
                        if isinstance(nestDict[subName], float): # if the value this refers to is a float
                            #print "... ", node, subName, nestDict[subName],  nestDict[node][sn][1]
                            nestDict[node][sn] = nestDict[subName] * nestDict[node][sn][1]

                        countSub = countSub + 1
                        
                #print node, countFloat, countSub
                if countFloat == len(nestDict[node]): # all the values are floats; take their logsum
                    expsum = 0
                    for element in nestDict[node]:
                        expsum = expsum + math.exp(element)
                    if expsum > 0:
                        nestDict[node] = math.log(expsum)
                    else:
                        nestDict[node] = -99999.9               
        
        if x > 250: # Prevent nesting dictionary errors
            print(250 * "!")
            print(nestDict)
            raise RuntimeError("Can't resolve nesting structure!")
        x = x + 1
    return nestDict



#def zonalProperties(tazList=None, fileName = cvmZonalProperties):
def zonalProperties(fileName, tazList=None):
    #===========================================================================
    # Returns a dictionary of zonal properties, which are anything that applies
    # to a zone under all circumstances
    #
    # The dictionary uses the property names as keys (e.g. "PSE" or "Ag Employment")
    # and each key leads to a list of properties, in the same order as the tazList 
    #
    # Reads the zonal property file by default, but can also read in another file
    # in the same format if specified.
    #
    # If the tazList argument is blank, reads in all TAZ and returns both the
    # props dictionary and the tazList containing all zones.
    #
    #===========================================================================
    
    print("    Reading zonal property file", fileName)
    fin = open(fileName, "r")
    inFile = csv.reader(fin)
    header = next(inFile)
    
    
    tempTazDict = {}
    tempTazList = []
    for row in inFile:
        try:
            taz = int(row[header.index("TAZ")])
        except ValueError:
            print(120 * "#")
            print("Couldn't process a zone in zonal properties file.")
            raise
            
        tempTazList.append(taz)
        list = []
        for thing in row:
            try:
                list.append(float(thing))
            except ValueError:
                list.append(thing)
        tempTazDict[taz] = list 
    
    if tazList is None:
        tazList = tempTazList
        returnBoth = True
    else:
        returnBoth = False
    
    propsDict = {}
    for thing in header:
        propsDict[thing] = []
    for zone in tazList:
        for n in range(len(header)):
            propsDict[header[n]].append(tempTazDict[zone][n])
            
    if returnBoth == True:
        return tazList, propsDict
    else:
        return propsDict



def hdf5Skim(fromList, toList, fromZoneDict, toZoneDict, table, skimDict, skimList):
    # Reads a skim matrix; the from are the rows and the to are the columns
    # so skim references are skim[from][to].
    # In the application of the CVM, the from and to are the same, so the matrix is
    # symmetric (in that the 15th cell in the 4th row and 4th cell in 15th row refer
    # to the same two zones; one-way roads and congestion means that they won't have
    # symmetrical costs)

    
    # Create blank skims to hold all of the data
    
    for s in skimList:
        skimDict[s] = []
    for c in range(len(fromList)):
        for s in skimList:
            row = len(toList) * [99999.9]
            row = array('f', row)
            skimDict[s].append(row)
            
    # Read in the table row by row
    x = 0
    for row in table.iterrows():
        if row['origin'] in fromZoneDict:
            iTaz = fromZoneDict[row['origin']]
            if row['destination'] in toZoneDict:
                jTaz = toZoneDict[row['destination']]
                for s in skimList:
                    try:
                        skimDict[s][iTaz][jTaz] = row[s]
                    except:
                        print("Skim reading error!", iTaz, jTaz)
                    
        x = x + 1
        if x % 500000 == 0:
            pass
            print("    read", x/1000000.0, "million rows.")
            
    return skimDict


def csvSkim(fromList, toList, fromZoneDict, toZoneDict, skimFile, skimDict, skimName):
    # Reads a skim matrix; the from are the rows and the to are the columns
    # so skim references are skim[from][to].
    # In the application of the CVM, the from and to are the same, so the matrix is
    # symmetric (in that the 15th cell in the 4th row and 4th cell in 15th row refer
    # to the same two zones; one-way roads and congestion means that they won't have
    # symmetrical costs)
    # Uses TransCad GISDK output which is in "row format", i.e. each row is all destinations for a given origin.

    
    # Create blank skims to hold all of the data
    print(skimFile)
    skimDict[skimName] = []
    for c in range(len(fromList)):
        row = len(toList) * [99999.9]
        row = array('f', row)
        skimDict[skimName].append(row)



    # Open skim file and read header
    fin = open(skimFile, "r")
    inFile = csv.reader(fin)
#    header = inFile.next()
#
#    for i in range(len(skimList)): #Replace column names in skim list with index of same-named header
#        try:
#            skimList[i] = header.index(skimList[i])
#        except ValueError:
#            print "Skim name", skimList[i], "nots found in skim file", skimFile
#            print "Header:", header
#            raise ValueError

    # Read in the table row by row
    x = 0
    err = 0
    for row in inFile:

        if int(row[0]) in fromZoneDict: #Orig
            iTaz = fromZoneDict[int(row[0])]
#            if skimName == "Time_Mid":

            while row.count("") > 0:
                row[row.index("")] = "0"
                err = err + 1
            floatrow = list(map(float, row[1:]))
            skimDict[skimName][iTaz] = array("f", floatrow)

#            else:
#                for jTaz in toList:
#                    if skimName == "Light_Mid":
#                        skimDict[skimName][iTaz][jTaz-1] = float(row[jTaz])
#                    else:
#                        skimDict[skimName][iTaz][jTaz-1] = float(row[jTaz]) *  -0.3
                
            x = x + 1
            if x % 500 == 0:
                pass
                print("      read", x, "rows.")
    print("Replaced", err, "null values.")       
    return skimDict




def bigrun():
    ts = time.perf_counter()

    # ===============================================================================
    #        Set Parser Options
    # ===============================================================================
    parser = OptionParser()
    parser.add_option("-s", "--scale",
                      action="store", dest="scale", default=1.0,
                      help="scale factor for multiple runs")
    parser.add_option("-p", "--path",
                      action="store", dest="path",
                      help="project scenario path")
    (options, args) = parser.parse_args()
    # ===============================================================================
    # Input File Names
    # ===============================================================================
    cvmInputPath = options.path + "/input/"

    cvmZonalProperties = cvmInputPath + "Zonal Properties CVM.csv"

    skimPath = options.path + "/output/"

    skimFileDict = {"Light_Mid": [skimPath + "impldt_MD_DU.TXT"],
                    "Medium_Mid": [skimPath + "impmhdt_MD_DU.TXT"],
                    "Heavy_Mid": [skimPath + "imphhdt_MD_DU.TXT"],
                    "Time_Mid": [skimPath + "impldt_MD_Time.TXT"]}
    # ===============================================================================
    #        Read in scale factor
    # ===============================================================================
#    fin = open(settings.scaleFactorSource, "r")
#    for row in fin:
#        if len(row) > 15: # key word has 15 chars, so don't need to look at shorter lines
#            if row[0:15] == "cvm.scaleFactor":
#                s = row.index("=")
#                scale = float(row[s+1:])
#                scaleName = row

    scale = float(options.scale)
    print(40*"-")
    print("Scaling tour gen with scale factor", scale)
    print(40*"-")
    print()


    testZones = [1578, 88, 971, 2178, 3798, 2711, 4286]
    fout = open("AccessVals.csv", "w")
    outFileTest = csv.writer(fout, excelOne)
    outFileTest.writerow(["I", "J", "AccType", "Cost", "Attr", "AccVal"])


    # ===============================================================================
    #        Produce accessibilities and other CVM-specific derived attributes 
    # ===============================================================================
    # Read in zonal properties file
    tazList, zonals = zonalProperties(fileName=cvmZonalProperties)
    tazDict = {}
    #tazList = tazList[:25]
    for t in range(len(tazList)):
        tazDict[tazList[t]] = t
    
    # Calculate accessibilities
    print("Calculating accessibilities", round(time.clock(), 2))
    
    cvmZonals = {} # This is a zonal properties style dictionary, indexed by thing and containing a list in order of properties
    accDict = settings.cvmAccDict # Dictionary for creating accessibilities; [skim, property, lambda] 
    accList = list(accDict.keys())
    
    
    skimList = []
    for accType in accList:
        cvmZonals[accType] = []
        if skimList.count(accDict[accType][0]) == 0:
            skimList.append(accDict[accType][0])
    cvmZonals["LnJobs30"] = []
    cvmZonals["REZone"] = []
    
    # Calculate percentage employment by industry; binary over 3000 flag
    sectors = ["SV", "IN", "RE", "TH", "WH", "GO"]
    for sect in sectors:
        cvmZonals["Pct" + sect] = []
        cvmZonals["Over3K_" + sect] = []
    for taz in tazList:
        idx = tazDict[taz]
        for sect in sectors:
            cvmZonals["Pct" + sect].append(zonals["CVM_" + sect][idx] / (zonals["TotEmp"][idx] + 0.0001))
            if zonals["CVM_" + sect][idx] > 3000:
                cvmZonals["Over3K_" + sect].append(1)
            else:
                cvmZonals["Over3K_" + sect].append(0)
        if cvmZonals["PctRE"][idx] > 0.5:
            cvmZonals["REZone"].append(1)
        else:
            cvmZonals["REZone"].append(0)    
    
    # Calculate employment and population density and cap if necessary
    cvmZonals["PopDensCap"] = []
    cvmZonals["EmpDensCap"] = []
    for taz in tazList:
        idx = tazDict[taz]
        pop = zonals["Pop"][idx]
        emp = zonals["TotEmp"][idx]
        area = zonals["Area_SqMi"][idx]
        if area > 0:
            cvmZonals["PopDensCap"].append(min(pop/area, 50000))
            cvmZonals["EmpDensCap"].append(min(emp/area, 100000))
        else:
            cvmZonals["PopDensCap"].append(0)
            cvmZonals["EmpDensCap"].append(0)
            

    # Read in skims
 
    print("Reading in CVM skims. Time:", round(time.clock()-ts, 2))    
    skimDict = {}

    for skimName in list(skimFileDict.keys()):
        print("...", skimName,  round(time.clock()-ts, 2)) 
        skimList.append(skimName)
        skimDict = csvSkim(tazList, tazList, tazDict, tazDict,
                           skimFileDict[skimName][0], skimDict, skimName)

    print(list(skimDict.keys()))
    print(len(list(tazDict.keys())))
    print(len(tazList))
    #print tazDict
    
    print("Skims read in. Time:", round(time.clock()-ts, 2))
    idx = 0
    for iTaz in tazList:
        currAcc = len(accList) * [0]
        jobs30 = 0
#        maxCost = [999999, -1, -1]
        for jTaz in tazList:
            iIdx = tazDict[iTaz]
            jIdx = tazDict[jTaz]
            for accType in accList:
                skimType = accDict[accType][0]
                try:
                    cost = skimDict[skimType][iIdx][jIdx]
                except:
                    print(skimType, iTaz, jTaz, iIdx, jIdx)
                    print(len(skimDict[skimType]))
                    print(len(skimDict[skimType][0]))
                    crash
#                if accType == "Acc_LE" and cost < maxCost[0]:
#                    maxCost[0] = cost
#                    maxCost[1] = jTaz
#                    maxCost[2] = jIdx
                attr = zonals[accDict[accType][1]][jIdx]
                lam = accDict[accType][2]
                accVal = attr * math.exp(cost * lam)
                #print accType, accList
                currAcc[accList.index(accType)] = currAcc[accList.index(accType)] + accVal
                if testZones.count(iTaz) > 0:
                    outFileTest.writerow([iTaz, jTaz, accType, cost, attr, accVal])

                
            cost = skimDict["Time_Mid"][iIdx][jIdx]
            if cost < 30:
                jobs30 = jobs30 +  zonals["TotEmp"][jIdx]        
    
            if idx % 250000 == 0:
                print("Processed", idx, "OD pairs, most recently", iTaz, jTaz, round(time.clock()-ts,2))
    
            idx = idx + 1
#        if idx < 2000000:
#            print iTaz, iIdx, maxCost
        for c in range(len(accList)):
            cvmZonals[accList[c]].append(currAcc[c])
        if jobs30 > 0:
            cvmZonals["LnJobs30"].append(math.log(jobs30))
        else:
            cvmZonals["LnJobs30"].append(0)

    
    # First set is ship/no ship, second set is tours/emp
    rangeDict = {'FA': [[0.01, 0.05], [1.22, 2.09]],
                 'IN': [[0.37, 0.59], [0.13, 0.52]],
                 'WH': [[0.35, 0.50], [0.17, 0.47]],
                 'RE': [[0.10, 0.48], [0.33, 0.65]],
                 'SV': [[0.10, 0.33], [0.08, 0.33]],
                 'GO': [[0.10, 0.33], [0.08, 0.33]],
                 'TH': [[0.12, 0.55], [0.25, 0.55]]}
    

    adjDict = {'FA': [[-3.4677, -0.9635, -0.6686, -0.3527, 0.0091],[5.0449, 1.6675, 2.3762, 2.0193, 2.0415]],
               'IN': [[-0.6544, -1.8124, -1.3965, -1.2595, 1.094], [-0.2966, 0.9763, 0.9086, 1.0257, 1.1646]],
               'RE': [[-0.456, -1.4629, -1.6433, -0.666, 1.2942], [-0.1883, 0.4861, 1.1003, 0.8668, 0.8273]],
               'SV': [[-0.6344, -0.5504, -0.2762, -0.0793, 0.0627], [0.6254, 1.8833, 2.5968, 2.3156, 2.2625]],
               'GO': [[-0.6344, -0.5504, -0.2762, -0.0793, 0.0627], [0.6254, 1.8833, 2.5968, 2.3156, 2.2625]],
               'TH': [[-1.9161, -1.623, -1.5503, -1.1891, 0.0705], [0.5195, 0.3715, 0.8332, 0.6878, 0.3449]],
               'WH': [[0.1322, -1.0171, -1.1068, -0.8744, 1.2775], [-1.3517, 0.5526, 1.1136, 0.7629, 0.794]]}

    # Scale to match proportions for SANDAG
    adjSandagDict = settings.genCalibDict



#    cout = open("e:/sjvitm_sdcvm_calibration.csv", "w")
#    calibOut = csv.writer(cout, excelOne)
#    calibOut.writerow(['Sector', 'Model', 'Iteration', 'Below', 'Within', 'Above', 'Score', 'Average', 'Param', 'Tours'])
    
    for iter in range(1):
        print(15 * "-", "Iteration", iter, 15 * "-") 
    
        # ===========================================================================
        # Big loop: iterate through each industry and create generation
        # ===========================================================================
        for sector in settings.cvmSectors:
            print("Calculating tour generation for sector", sector, round(time.clock()-ts, 2))
            # Read in control file for this sector
            fin = open(cvmInputPath + sector + ".csv", "r")
            inFile = csv.reader(fin)
            
            paramDict = {}
            paramDict["ShipNoShip"] = {}
            paramDict["GenPerEmployee"] = {}
            paramDict["TourTOD"] ={}
            paramDict["VehicleTourType"] = {}
    
            paramDict["TourTOD_nest"] = {}
            paramDict["VehicleTourType_nest"] = {}
            
            for row in inFile:
                model = row[0]
                if model in paramDict:
                    alt = row[1]
                    type = row[2]
                    nest = row[3]
                    param = float(row[5])
                    if nest == "nest":
                        # put into nesting dictionary
                        if type in paramDict[model+"_nest"]:
                            pass
                        else:
                            paramDict[model+"_nest"][type] = []
                            
                        if param == 0:
                            paramDict[model+"_nest"][type].append([alt, 1])
                        else:
                            paramDict[model+"_nest"][type].append([alt, param])
                        
                    else:
                        if alt in paramDict[model]:
                            pass
                        else:
                            paramDict[model][alt] = []
                        parSet = (type, param)
                        paramDict[model][alt].append(parSet)
    
            #print paramDict
            
            # add to CVM zonals file / clear out values
            for timePer in settings.cvmTimes:
                cvmZonals[sector + "_" + timePer] = []
            cvmZonals[sector + "_Ship"] = []
            cvmZonals[sector + "_ToursEmp"] = []
                 
        
            # Medium loop: iterate through each TAZ
            for tazNum in tazList:
                taz = tazDict[tazNum]
                lu = int(round(float(zonals["CVM_LU_Type"][taz])))-1
                
                # ===================================================================
                # Phase One: Pass logsums up nested logit structure
                # ===================================================================
             
                # --------------------------------- Tour vehicle type / purpose nest
                
                model = "VehicleTourType"
                #print "Processing:", model, round(time.clock(), 2)
               
                altList = list(paramDict[model].keys())
                utilDict = {}
                
                # Begin by calculating the utilities for each alternative
                for alt in altList:
                    util = 0
                    for name, par in paramDict[model][alt]:
                        if name == '':
                            util = util + par
                            val = "const"
                        else:
                            if name in cvmZonals:
                                util = util + cvmZonals[name][taz] * par
                                val = cvmZonals[name][taz]
                            elif name in zonals:
                                util = util + zonals[name][taz] * par
                                val = zonals[name][taz]
                            else:
                                raise LookupError("Couldn't find value " + name + " in zonal properties files, for alternative " + alt)
                        utilDict[alt] = util
#                        if tazNum < 20:
#                            print alt, util, name, par, val
#                    if tazNum < 20:
#                        print 20 * "-"
                    
                # Add the parameters to the nested model and pass it to the logsumerizer
                modNest = "VehicleTourType_nest"
    
                for alt in altList:
                    paramDict[modNest][alt] = utilDict[alt]
    
                nestDict = copy.deepcopy(paramDict[modNest])
                #print taz, nestDict
                vehAndPurpNest = logitNestToLogsums(nestDict)
                CUPurpVeh = vehAndPurpNest["top"]
                #print CUPurpVeh
                        
            
                # ------------------------------------------------------ Time Of Day
                model = "TourTOD"
                #print "Processing:", model, round(time.clock(), 2)
               
                altList = list(paramDict[model].keys())
                utilDict = {}
                
                # Begin by calculating the utilities for each alternative
                for alt in altList:
                    util = 0
                    for name, par in paramDict[model][alt]:
                        if name == '':
                            util = util + par
                            val = "const"
                        else:
                            if name in cvmZonals:
                                util = util + cvmZonals[name][taz] * par
                                val = cvmZonals[name][taz]
                            elif name in zonals:
                                util = util + zonals[name][taz] * par
                                val = zonals[name][taz]
                            elif name == "CUPurpVeh":
                                util = util + CUPurpVeh * par
                                val = CUPurpVeh
                            else:
                                raise LookupError("Couldn't find value " + name + " in zonal properties files, for alternative " + alt)
                        utilDict[alt] = util
                        #print alt, util, name, par, val
                    #print 20 * "-"
                    
                # Add the parameters to the nested model and pass it to the logsumerizer
                modNest = "TourTOD_nest"
    
                for alt in altList:
                    paramDict[modNest][alt] = utilDict[alt]
    
                nestDict = copy.deepcopy(paramDict[modNest])
                #print nestDict
                tourTODNest = logitNestToLogsums(nestDict)
                CUTimeOD = tourTODNest["top"]
                #print tourTODNest
                #print CUTimeOD
            
            
                # ----------------------------------------------- Trips per employee
                model = "GenPerEmployee"
                #print "Processing:", model, round(time.clock(), 2)
               
                altList = list(paramDict[model].keys())
                utilDict = {}
                # Begin by calculating the utilities for each alternative
                for alt in altList:
                    util = 0
                    for name, par in paramDict[model][alt]:
                        if name == '':
                            util = util + par
                            val = "const"
                        else:
                            if name in cvmZonals:
                                util = util + cvmZonals[name][taz] * par
                                val = cvmZonals[name][taz]
                            elif name in zonals:
                                util = util + zonals[name][taz] * par
                                val = zonals[name][taz]
                            elif name == "CUTimeOD":
                                util = util + CUTimeOD * par
                                val = CUTimeOD
                            else:
                                raise LookupError("Couldn't find value " + name + " in zonal properties files, for alternative " + alt)
                        utilDict[alt] = util
                        #print alt, util, name, par, val
                    #print 20 * "-"
                    
                # Calibration Adjustments; form: xn, xn-1, fn, fn-1
                utilDict["Gen"] = utilDict["Gen"] + adjDict[sector][1][lu]
                                 
                genUtil = utilDict["Gen"]
                CUGen = math.log(math.exp(genUtil) + math.exp(0)) # 0 is utility for no tours
        
                # ---------------------------------------------------- Ship / No Ship
                model = "ShipNoShip"
                #print "Processing:", model, round(time.clock(), 2)
               
                altList = list(paramDict[model].keys())
                utilDict = {}
                
                # Begin by calculating the utilities for each alternative
                for alt in altList:
                    util = 0
                    for name, par in paramDict[model][alt]:
                        if name == '':
                            util = util + par
                            val = "const"
                        else:
                            if name in cvmZonals:
                                util = util + cvmZonals[name][taz] * par
                                val = cvmZonals[name][taz]
                            elif name in zonals:
                                util = util + zonals[name][taz] * par
                                val = zonals[name][taz]
                            elif name == "CUGen":
                                util = util + CUGen * par
                                val = CUGen
                            else:
                                raise LookupError("Couldn't find value " + name + " in zonal properties files, for alternative " + alt)
                        utilDict[alt] = util
                        #print alt, util, name, par, val
                # Calibration Adjustments; form: xn, xn-1, fn, fn-1
                utilDict["Ship"] = utilDict["Ship"] + adjDict[sector][0][lu]


                # ===================================================================
                # Phase The Second: Calculate tours by time period
                # ===================================================================
    
                # Get base employment (total employment for fleet allocators)
                if sector == "FA":
                    baseEmp = zonals["TotEmp"][taz]
                else:
                    baseEmp = zonals["CVM_"+sector][taz]
                
                #print "Base Employment:", baseEmp
                
                # Ship/No Ship proportions
                
                uShip = utilDict["Ship"]
                uNoShip = utilDict["NoShip"]
                
                propShip = math.exp(uShip) / (math.exp(uShip) + math.exp(uNoShip))
                
                shipEmp = baseEmp * propShip
                
#                tempOut.writerow([sector, tazList[taz], 'ShipNoShip', 'PropShip', propShip])
                cvmZonals[sector + "_Ship"].append(propShip)
                #print "Shipping employees:", shipEmp, propShip
                
                # Generation: tours per employee
                toursPerEmp = (10 * math.exp(genUtil)) / (1 + math.exp(genUtil))
                toursAllDay = shipEmp * toursPerEmp

                # Scale for SANDAG behaviours
                toursAllDay = toursAllDay * adjSandagDict[sector][lu]
                
                
                #print "Tours per employee:", toursPerEmp, toursAllDay
#                tempOut.writerow([sector, tazList[taz], 'Generation', 'ToursPerEmp', toursPerEmp])
                cvmZonals[sector + "_ToursEmp"].append(toursPerEmp)
                
                # Time period: calculate probabilities of time period
                linkDict = paramDict["TourTOD_nest"]
                utilDict = tourTODNest
                
                #print linkDict
                #print utilDict
    
                #print 20 * "-"
                probDict = {}
                
                # First calculate probabilities for each node with subnodes
                for node in list(linkDict.keys()):
                    if isinstance(linkDict[node], float):
                        pass
                    else:
                        #print node
                        expSum = 0
                        for subNode in linkDict[node]:
                            expSum = expSum + math.exp(utilDict[subNode[0]] * subNode[1])
                            #print subNode[0], utilDict[subNode[0]], subNode[1], expSum
                        
                        for subNode in linkDict[node]:
                            probDict[subNode[0]] = math.exp(utilDict[subNode[0]] * subNode[1]) / expSum
                #print probDict
                
                # Now go down from top node and scale probabilities of any subnests by the nest prob
                # (note: this only goes down one level; this works for the existing time of day nests, but needs to be changed for reuse
                for node, coeff in linkDict["top"]:
                    if isinstance(linkDict[node], float):
                        pass
                    else:
                        for subNode, subcoeff in linkDict[node]:
                            probDict[subNode] = probDict[subNode] * probDict[node]
                #print probDict 

                
                # Divide into time periods and write out               
                for timePer in settings.cvmTimes:
                    
                    try:
                        tours = probDict[timePer] * toursAllDay * scale
                    except:
                        print("Error in scaling tours:", probDict[timePer], toursAllDay, scale)
                        tours = 0
                    cvmZonals[sector + "_" + timePer].append(round(tours, 2))
                    

# ==========================================================================
# Output CVM Gen and Accessibility file
# ==========================================================================
    print("Writing the data out...", round(time.clock(),2))
    fout = open(cvmInputPath + "CVMToursAccess.csv", "w")
    outFile = csv.writer(fout, excelOne)
    
    header = ["Taz"]
    keyList = list(cvmZonals.keys())
    keyList.sort()
    header.extend(keyList)
    outFile.writerow(header)
    
    for c in range(len(tazList)):    
        rowOut = [tazList[c]]
        for keyType in keyList:
            rowOut.append(cvmZonals[keyType][c])
        outFile.writerow(rowOut)
    fout.close()
    
#    tout.close()
#    cout.close()
    print("DonE!")
    
if __name__ == '__main__':
    bigrun()
