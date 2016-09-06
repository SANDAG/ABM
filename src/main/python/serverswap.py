# Rick.Curry@sandag.org
# July 12, 2016

import os
import socket
import csv
from optparse import OptionParser


def swap_servers(infile, outfile, searchitem, sep_str, swap_value, skip_lines):
    found = 0
    print infile, outfile
    print searchitem, sep_str, swap_value
    lines = []
    with open(infile) as propInFile:
        for line in propInFile:
            if line.find(searchitem) > -1:
                if found == skip_lines:
                    line = line.split(sep_str, 1)[0] + sep_str + swap_value + "\n"
                else:
                    found += 1
            lines.append(line)
    with open(outfile, 'w') as propOutFile:
        for line in lines:
            propOutFile.write(line)


# Set Parser Options
parser = OptionParser()
parser.add_option("-p", "--path",
                  action="store", dest="path",
                  help="project scenario path")
(options, args) = parser.parse_args()

# Set Paths
dst_dir_bin = options.path + "/bin/"
dst_dir_conf = options.path + "/conf/"

# Get IP Address
ip = socket.gethostbyname(socket.gethostname())
print str(ip)

# Read Server Info
fileServer = options.path + "/conf/server-config.csv"
print fileServer
logFile = options.path + "/logFiles/serverswap.log"
if os.path.exists(logFile):
    os.remove(logFile)
dictServer = csv.DictReader(open(fileServer))

# Check for Matching IP Address and Stop on Row
match = 'false'
for row in dictServer:
    print row
    if row['ActualIP'] == str(ip):
        match = 'true'
        print match
        serverName = row['ServerName']
        print serverName
        modelIP = row['ModelIP']
        break

# Write error log if IP address not found
logWriteFile = open(logFile, "w")
if match == 'false':
    logWriteFile.write('FATAL, Head Node not found - check for ' + str(ip) + ' in server-config.csv')
    print 'Head Node not found'
else:
    # Update Files in serverswap_files.csv
    logWriteFile.write('MATCH, Head Node found - ' + str(ip) + ' in server-config.csv')
    skip = 0
    fileUpdate = options.path + "/conf/serverswap_files.csv"
    print fileUpdate
    filesToUpdate = csv.DictReader(open(fileUpdate))
    for update in filesToUpdate:
        print update
        # Special section for StopABM.cmd which does not have a property=value format
        if update['property'] == 'pskill':
            refValue = row[update['refValue']] + ' java.exe'
            print row[update['refValue']]
            print refValue
            swap_servers(options.path + update['fileName'], options.path + update['fileName'],
                         update['property'], update['separator'], refValue, skip)
            skip += 1
        else:
            # General section for file updates
            swap_servers(options.path + update['fileName'], options.path + update['fileName'],
                         update['property'], update['separator'], row[update['refValue']], 0)
