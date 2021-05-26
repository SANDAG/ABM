
import csv
import time
import pandas as pd
import random
import re
import sys

def setVOT(vehtype):
    votdict = {'passengerCar': 0.67000001, 
    	         'DriveTransit': 0.67000001, 
    	         'heavy-light': 0.67000001,
               'heavy-medium': 0.68000001, 
               'heavy-heavy': 0.89000001,
               'Light Heavy Duty Truck': 0.67000001,	
               'Medium Heavy Duty Truck': 0.68000001,
               'Heavy Heavy Duty Truck': 0.89000001,
               'lhdt': 0.67000001,
               'mhdt': 0.68000001,		
               'hhdt': 0.89000001}
    vot = votdict[vehtype]
    return vot


def setAimsunVehType(vehtype, vehocc, tolleligible):
    vehtypedict = {('passengerCar', '1', '0'): ('Drive Alone No Toll', '136457'),
                   ('passengerCar', '1', '1'): ('Drive Alone Toll', '136455'),
                   ('passengerCar', '2', '0'): ('HOV2 No Toll + HOV', '136460'),
                   ('passengerCar', '2', '1'): ('HOV2 Toll + HOV', '136459'),
                   ('passengerCar', '3', '0'): ('HOV3 No Toll + HOV', '136464'),
                   ('passengerCar', '3', '1'): ('HOV3 Toll + HOV', '136463'),
                   ('heavy-light', '1', '0'): ('Light Duty Truck No Toll', '136467'),
                   ('heavy-light', '1', '1'): ('Light Duty Truck Toll', '136466'),
                   ('heavy-medium', '1', '0'): ('Medium Duty Truck No Toll', '136469'),
                   ('heavy-medium', '1', '1'): ('Medium Duty Truck Toll', '136468'),
                   ('heavy-heavy', '1', '0'): ('Heavy Duty Truck No Toll', '136471'),
                   ('heavy-heavy', '1', '1'): ('Heavy Duty Truck Toll', '136470'),
                   ('DriveTransit', '1', '0'): ('Drive Alone No Toll', '136457'),
                   ('Light Heavy Duty Truck' , '1', '0'): ('Light Duty Truck No Toll', '136467'),
                   ('Light Heavy Duty Truck' , '1', '1'): ('Light Duty Truck Toll', '136466'),
                   ('Medium Heavy Duty Truck', '1', '0'): ('Medium Duty Truck No Toll', '136469'),
                   ('Medium Heavy Duty Truck', '1', '1'): ('Medium Duty Truck Toll', '136468'),
                   ('Heavy Heavy Duty Truck' , '1', '0'): ('Heavy Duty Truck No Toll', '136471'),
                   ('Heavy Heavy Duty Truck' , '1', '1'): ('Heavy Duty Truck Toll', '136470'),
                   ('lhdt', '1', '0'): ('Light Duty Truck No Toll', '136467'),    
                   ('lhdt', '1', '1'): ('Light Duty Truck Toll', '136466'),       
                   ('mhdt', '1', '0'): ('Medium Duty Truck No Toll', '136469'),   
                   ('mhdt', '1', '1'): ('Medium Duty Truck Toll', '136468'),      
                   ('hhdt', '1', '0'): ('Heavy Duty Truck No Toll', '136471'),    
                   ('hhdt', '1', '1'): ('Heavy Duty Truck Toll', '136470')   
                   }
    aimsunVehType = vehtypedict[(vehtype, vehocc, tolleligible)]
    return aimsunVehType


def setExternalMgra(exttaz, odflag, vehtype):
    if int(exttaz) > 5:
        vehtype = 'all'
    if int(exttaz) > 1:
        odflag = 'Origin'
    ext_taz_mgra_dict = {('1', 'Origin', 'passengerCar'): '23003',
                         ('1', 'Destination', 'passengerCar'): '23003',
                         ('1', 'Origin', 'Heavy Heavy Duty Truck'): '23003',
                         ('1', 'Origin', 'Medium Heavy Duty Truck'): '23003',
                         ('1', 'Origin', 'Light Heavy Duty Truck'): '23003',
                         ('1', 'Origin', 'hhdt'): '23003', 	
                         ('1', 'Origin', 'mhdt'): '23003',	
                         ('1', 'Origin', 'lhdt'): '23003', 	
                         ('1', 'Origin', 'DriveTransit'): '23003', 	
                         ('1', 'Destination', 'Heavy Heavy Duty Truck'): '23003',
                         ('1', 'Destination', 'Medium Heavy Duty Truck'): '23003',
                         ('1', 'Destination', 'Light Heavy Duty Truck'): '23003',
                         ('1', 'Destination', 'hhdt'): '23003', 	
                         ('1', 'Destination', 'mhdt'): '23003',	
                         ('1', 'Destination', 'lhdt'): '23003', 	
                         ('1', 'Destination', 'DriveTransit'): '23003', 	
                         ('2', 'Origin', 'passengerCar'): '23004',
                         ('2', 'Origin', 'heavy-heavy'): '23004',
                         ('2', 'Origin', 'heavy-medium'): '23004',
                         ('2', 'Origin', 'heavy-light'): '23004',
                         ('2', 'Origin', 'heavy-heavy'): '23004',
                         ('2', 'Origin', 'heavy-medium'): '23004',
                         ('2', 'Origin', 'heavy-light'): '23004',
                         ('2', 'Origin', 'Heavy Heavy Duty Truck'): '23004',
                         ('2', 'Origin', 'Medium Heavy Duty Truck'): '23004',
                         ('2', 'Origin', 'Light Heavy Duty Truck'): '23004',
                         ('2', 'Origin', 'DriveTransit'): '23004',
                         ('2', 'Origin', 'hhdt'): '23004', 	
                         ('2', 'Origin', 'mhdt'): '23004',	
                         ('2', 'Origin', 'lhdt'): '23004', 	
                         ('3', 'Origin', 'passengerCar'): '23005',  # need to verify
                         ('3', 'Origin', 'heavy-heavy'): '23005',
                         ('3', 'Origin', 'heavy-medium'): '23005',
                         ('3', 'Origin', 'heavy-light'): '23005',
                         ('3', 'Origin', 'Heavy Heavy Duty Truck'): '23005',
                         ('3', 'Origin', 'Medium Heavy Duty Truck'): '23005',
                         ('3', 'Origin', 'Light Heavy Duty Truck'): '23005',
                         ('3', 'Origin', 'hhdt'): '23005', 	
                         ('3', 'Origin', 'mhdt'): '23005',	
                         ('3', 'Origin', 'lhdt'): '23005', 	
                         ('3', 'Origin', 'DriveTransit'): '23005', 	
                         ('4', 'Origin', 'passengerCar'): '23006',
                         ('4', 'Origin', 'heavy-heavy'): '23006',
                         ('4', 'Origin', 'heavy-medium'): '23006',
                         ('4', 'Origin', 'heavy-light'): '23006',
                         ('4', 'Origin', 'Heavy Heavy Duty Truck'): '23006',
                         ('4', 'Origin', 'Medium Heavy Duty Truck'): '23006',
                         ('4', 'Origin', 'Light Heavy Duty Truck'): '23006',
                         ('4', 'Origin', 'hhdt'): '23006', 	
                         ('4', 'Origin', 'mhdt'): '23006',	
                         ('4', 'Origin', 'lhdt'): '23006', 	
                         ('4', 'Origin', 'DriveTransit'): '23006', 	
                         ('5', 'Origin', 'passengerCar'): '23007',  # need to verify
                         ('5', 'Origin', 'heavy-heavy'): '23007',
                         ('5', 'Origin', 'heavy-medium'): '23007',
                         ('5', 'Origin', 'heavy-light'): '23007',
                         ('5', 'Origin', 'Heavy Heavy Duty Truck'): '23007',
                         ('5', 'Origin', 'Medium Heavy Duty Truck'): '23007',
                         ('5', 'Origin', 'Light Heavy Duty Truck'): '23007',
                         ('5', 'Origin', 'hhdt'): '23007', 	
                         ('5', 'Origin', 'mhdt'): '23007',	
                         ('5', 'Origin', 'lhdt'): '23007', 	
                         ('5', 'Origin', 'DriveTransit'): '23007', 	
                         ('6', 'Origin', 'all'): '23008',
                         ('7', 'Origin', 'all'): '23009',
                         ('8', 'Origin', 'all'): '23010',
                         ('9', 'Origin', 'all'): '23011',
                         ('10', 'Origin', 'all'): '23012',
                         ('11', 'Origin', 'all'): '23013',  # need to verify
                         ('12', 'Origin', 'all'): '23014'
                         }

    extmgra = ext_taz_mgra_dict[(exttaz, odflag, vehtype)]
    return extmgra


def setnewdtaperiod(str_dtaperiod):
    dtaperiod = int(str_dtaperiod)
    if dtaperiod >= 288:
        new_dta_period = dtaperiod - 288
    else:
        new_dta_period = dtaperiod
    return str(new_dta_period)


def columnswap(value1, value2):
    temp1 = value1
    temp2 = value2
    new_value = (temp2, temp1)
    return new_value


def montecarlo(criteria):
    draw = random.randint(1,100)
    if draw <= criteria:
        return True
    elif draw > criteria:
        return False

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
# Main Program Area
########################################################################################################
# Start Script Timer
start = time.time()
print "Started"

propertiesFileName = "./conf/sandag_abm.properties"
myfile = open( propertiesFileName, 'r' )
myfile_contents = myfile.read()
myfile.close()

dtaTripsIn = get_property(propertiesFileName, myfile_contents, "dta.postprocessing.outputs.TripFile")
fileTripListIn = 'output/' + dtaTripsIn

# Set File Paths
fileTripListOut = get_property(propertiesFileName, myfile_contents, "dta.processed.outputs.TripFile")
fileMgraTazLU = get_property(propertiesFileName, myfile_contents, "dta.mgraTaz.file")
fileDemandDamper = get_property(propertiesFileName, myfile_contents, "dta.demandDamper.file")

# Set Trips to Filter
filterTrips = ['WalkTransit', 'SchoolBus', 'nonMotorized']

# Read in MGRA Lookup for TAZ disaggregation
df_ZoneLU = pd.read_csv(fileMgraTazLU)

# Read in TAZ Lookup for Demand Dampening
df_TazDampen = pd.read_csv(fileDemandDamper)

# Filter Trips and Add Generic VOT
with open(fileTripListIn, 'rb') as inp, open(fileTripListOut, 'wb') as out:
    writer = csv.writer(out)
    reader = csv.reader(inp)
    # read header
    header = reader.next()
    # append columns in header and write to new file
    #header[0] = 'ID'
    header.insert(0,'ID')
    # swap mgra and taz headers
    newheader = columnswap(header[5], header[7])
    header[5] = newheader[0]
    header[7] = newheader[1]
    newheader = columnswap(header[6], header[8])
    header[6] = newheader[0]
    header[8] = newheader[1]
    header.append("VOT")
    # header.append("aimsunVehType") #comment out to remove vehicle type name
    header.append("aimsunVehTypeID")
    writer.writerow(header)
    # loop through rows, filter trips to remove walk/bike/school bus, add new columns for VOT & Vehicle Type,
    # update external mgra zone #'s, update dta period to convert from 3am-3am to 0-24hr
    # row 5 = origin TAZ --> change to row 7
    # row 6 = destination TAZ --> change to row 8
    # row 7 = origin MGRA --> change to row 5
    # row 8 = destination MGRA --> change to row 6
    # row11 = vehicle type
    # row12 = veh occupancy
    # row13 = toll eligibility
    # row 17 = dta period
    i=0
    for row in reader:
        i=i+1
        row.insert(0,i)
        # dampen demand for specific TAZs and filter out walk/bike/school bus
        dampen_taz_o = df_TazDampen.loc[df_TazDampen['TAZ'] == int(row[5])]
        dampen_taz_d = df_TazDampen.loc[df_TazDampen['TAZ'] == int(row[6])]
        if not dampen_taz_o.empty:
            for index, zone in dampen_taz_o.iterrows():
                dampen_factor = zone[1]
        elif not dampen_taz_d.empty:
            for index, zone in dampen_taz_d.iterrows():
                dampen_factor = zone[1]
        else:
            dampen_factor = 1.00
        if montecarlo(dampen_factor*100) and not any(filterTrip in row[11] for filterTrip in filterTrips):
            # add VOT based on vehicle type
            row.append(setVOT(row[11]))
            # add vehicle type and vehicle type ID
            dtavehtype = setAimsunVehType(row[11], row[12], row[13])
            # row.append(dtavehtype[0]) #comment out to remove vehicle type name
            row.append(dtavehtype[1])
            # update external MGRA zone numbers
            if int(row[5]) < 13:
                row[7] = setExternalMgra(row[5], 'Origin', row[11])
            if int(row[6]) < 13:
                row[8] = setExternalMgra(row[6], 'Destination', row[11])
            # update dta time period to convert from 3am-3am to 0-24hr
            if int(row[17]) >= 288:
                row[17] = setnewdtaperiod(row[17])
            elif int(row[17]) == 0:
				row[17]=287
            # update aggregate model MGRA's that are 0
            if int(row[5]) > 0 and int(row[7]) == 0:
                mymgra = df_ZoneLU.loc[df_ZoneLU['TAZ'] == int(row[5])]
                for index, zone in mymgra.iterrows():
                    row[7] = zone[1]
            if int(row[6]) > 0 and int(row[8]) == 0:
                mymgra = df_ZoneLU.loc[df_ZoneLU['TAZ'] == int(row[6])]
                for index, zone in mymgra.iterrows():
                    row[8] = zone[1]
            # swap mgra and taz columns
            newvalue = columnswap(row[5], row[7])
            row[5] = newvalue[0]
            row[7] = newvalue[1]
            newvalue = columnswap(row[6], row[8])
            row[6] = newvalue[0]
            row[8] = newvalue[1]
            # write out updated row
            writer.writerow(row)
        # print processing progress
        if (int(row[0]) % 1000000) == 0:
            print "Row {0}, {1:5.2f} min".format(int(row[0]), ((time.time() - start) / 60.0))

# Print Script Execution Time
print "Finished in %5.2f mins" % ((time.time() - start)/60.0)
