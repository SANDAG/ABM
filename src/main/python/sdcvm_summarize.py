import csv, time
import sdcvm
# CSTDM libraries
import sdcvm_settings as settings
from optparse import OptionParser

# External libraries
ts = time.clock()


class excelOne(csv.excel):
    # define CSV dialect for Excel to avoid blank lines from default \r\n
    lineterminator = "\n"

# ===============================================================================
#        Set Parser Options
# ===============================================================================
parser = OptionParser()
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
cvmPath = options.path + "/output/"

tazList = range(1, settings.maxTaz+1)
tazList, zonals = sdcvm.zonalProperties(fileName=cvmZonalProperties)
tazDict = {}
# tazList = tazList[:25]
for t in range(len(tazList)):
    tazDict[tazList[t]] = t

# Read in skims

print "Reading in CVM skims. Time:", round(time.clock()-ts, 2)    
skimDict = {}
skimList = []


print "... Midday distance",  round(time.clock()-ts, 2) 
skimList.append("Dist_Mid")
skimDict = sdcvm.csvSkim(tazList, tazList, tazDict, tazDict,
                         skimPath + "impldt_MD_Dist.TXT", skimDict, "Dist_Mid")


bigDict = {}
for ind in settings.cvmSectors:
    for tim in settings.cvmTimes:
        print ind, tim,  round(time.clock()-ts, 2) 
        fin = open(cvmPath + "Trip_" + ind + "_" + tim + ".csv", "r")
        inFile = csv.reader(fin)
        header = inFile.next()
        for row in inFile:
            mode = row[header.index("Mode")]
            trip = int(row[header.index("Trip")])
            purp = row[header.index("TourType")]
            toll = row[header.index("TripMode")]
            tollAv = row[header.index("TollAvailable")]
            home = int(row[header.index("HomeZone")])
            iTaz = int(row[header.index("I")])
            jTaz = int(row[header.index("J")])
            tim = row[header.index("TripTime")]

            iIdx = tazList.index(iTaz)
            jIdx = tazList.index(jTaz)
            newjIdx = jIdx  # new index created to add 12 to include external zones 1-12 in the skim matrix

            key = (ind, mode, purp, toll, tollAv, tim, home)
            if bigDict.has_key(key):
                pass
            else:
                bigDict[key] = [0, 0, 0, 0]

            bigDict[key][1] = bigDict[key][1] + 1
            bigDict[key][2] = bigDict[key][2] + skimDict["Dist_Mid"][iIdx][newjIdx]
            if trip == 1:
                bigDict[key][0] = bigDict[key][0] + 1
            if iTaz == jTaz:
                bigDict[key][3] = bigDict[key][3] + 1

fout = open(cvmPath + "Gen and trip sum.csv", "w")
outFile = csv.writer(fout, excelOne)

keyList = bigDict.keys()
keyList.sort()
header = ["Industry", "Mode", "Purpose", "Toll", "TollAv", "Time", "TAZ", "Tours", "Trips", "Dist", "Intra"]
outFile.writerow(header)


for key in keyList:
    outRow = list(key)
    outRow.extend(bigDict[key])
    outFile.writerow(outRow)
fout.close()
