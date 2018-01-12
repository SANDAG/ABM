'''
PURPOSE:
Commercial Vehicle Model (CVM) input file creation

INSTALL (LIBRARY):
Numpy
https://pypi.python.org/pypi/numpy

GDAL/OGR
https://pypi.python.org/pypi/GDAL/

HOW TO RUN:
[script_filepath] [Project Directory] [MGRA filename] [output filename]

example: cvm_input_create_v1.py "C:\Projects\SANDAG_CTM_Validation\_Tasks" "mgra13_based_input2012.csv" "Zonal Properties CVM.csv"

Note: The script name is sufficient if the run folder is the same as the script folder. Otherwise, a full path of the script would be needed. Also, if the output file does not contain spaces, the command line does not need to have quotation marks around the arguments. 

STEPS:
STEP 0: read mgra socio-economic file
STEP 1: find min and max tazids
STEP 2: read node bin file header
STEP 3: read node file for taz centroids. also transform coordinates.
STEP 4: calculate taz level variables
STEP 5. write to output

REFERENCES:
Following reference are used for calculations:
T:\devel\CVM\sr13\2012_calib5\input\mgra13_based_input2012_CVM.xlsx
T:\devel\CVM\sr13\2012_calib5\CVM\Zonal Properties SDCVM_SR13 KJS rcu_check.xlsx
The final example product: T:\devel\CVM\sr13\2012_calib5\CVM\Inputs\Zonal Properties CVM.csv

CREATED BY:
nagendra.dhakar@rsginc.com

LAST MODIFIED:
05/19/2016

'''

import sys
import os
import numpy as np
import datetime
import math
import csv
import osr
import ogr

class Constant:
    """ Represents constants in the script
    constants: FEET_TO_MILE, COORDS_EPSG_SOURCE,COORDS_EPSG_TARGET
    """
    ACRES_TO_SQMILE = 0.0015625
    COORDS_EPSG_SOURCE = 2230 #EPSG: 2230 - NAD83/ California zone 6 (ftUS)
    COORDS_EPSG_TARGET = 3310 #EPSG: 3310 - NAD83/ California Albers

class Header:
    """ Represents header for output files """
    temptazdatafile = ['taz','pop','hh','i1','i2','i3','i4','i5','i6','i7','i8','i9','i10','emp_total','emp_fed_mil','sqmile','land_sqmile','Emp_IN','Emp_RE','Emp_SV','Emp_TU','Emp_WH','Emp_OFF',
                      'HHIncome','EmpDens','PopDens','Per/Emp','Emp_ServRet_Pct','Ret_ServRet','Emp_Office','Low Density','Residential','Commercial','Industrial','Employment Node',
                      'Industrial_pct','TU_pct','Wholesale_pct','Retail_pct','Service_pct','Office_pct','E500_Industrial','E500_TU','E500_Wholesale','E500_Retail','E500_Service',
                      'E500_Office','RetailZone','ZoneType']
    
    temptazcentroidsfile = ["hnode","x_coord_spft","y_coord_spft","x_coord_albers","y_coord_albers"]
    
    outfile = ['TAZ','Pop','Income','Area_SqMi','x-meters','y-meters','EmpDens','PopDens','TotEmp','Military',
                'CVM_IN','CVM_RE','CVM_SV','CVM_TH','CVM_WH','CVM_GO','CVM_LU_Type','SqrtArea','CVM_LU_Low','CVM_LU_Res',
                'CVM_LU_Ret','CVM_LU_Ind','CVM_LU_Emp','Emp_LU_Lo','Emp_LU_Re','Emp_LU_RC','Emp_LU_In','Emp_LU_EN']

class TazId:
    """ Represents Taz Id range
    attributes: min, max
    """

class Input:
    """ Represents input settings
    attributes: dir, mgrafile, nodebinfile.
    """
class Output:
    """ Represents output settings
    attributes: dir, outfile, temp_centroidfile, temp_tazfile
    """

def read_node_header(node_file):
    """Returns name and type of the fields in the node bin file """
    
    # node header file
    nodeheaderfile = os.path.join(os.path.splitext(node_file)[0]+".DCB")

    # first row is blank
    # second row is total bytes in a row
    # start reading fields names from third row
    # header file format - field_name, type, start_byte, length, ..

    fields_info=[]
    with open(nodeheaderfile) as headerfile:
        reader = csv.reader(headerfile)
        i=0
        for row in reader:
            if i >= 2:
                field_name = row[0]
                field_length = row[3]

                # integer
                if row[1] == 'I':
                    field_type = int

                # character/string
                elif row[1] == 'C':
                    field_type = 'S' + field_length

                # first field
                if i == 2:
                    fields_info.append([field_name])
                    fields_info.append([field_type])

                # remaining fields
                else:
                    fields_info[0].append(field_name)
                    fields_info[1].append(field_type)
                
            i=i+1

    return fields_info

def get_tazid_range(data):
    """Returns min and maz taz ids"""

    # max and min tazid
    
    id_list = np.array(data.keys())
    id_list = id_list.astype(np.float)
    
    id_max = int(max(id_list))
    id_min = int(min(id_list))

    return([id_min,id_max])
    
def read_node_file(node_file, fields_info, taz_ids, outfile):
    """
    Reads taz centroids from node bin file
    Transforms them into the coordinate system expected by the CTM
    Returns transformed (projected) coordinates
    """

    # create data type    
    file_dtype = np.dtype(zip(*fields_info))

    # save data to an array
    data_array = np.fromfile(node_file,dtype=file_dtype)

    # sort by tazid
    data_array.sort(order='hnode')
    
    taz_id = data_array['hnode']
    x_coord = data_array['X-COORD']
    y_coord = data_array['Y-COORD']

    # current coordinate system (EPSG: 2230 - NAD83/ California zone 6 (ftUS))
    source = osr.SpatialReference()
    source.ImportFromEPSG(Constant.COORDS_EPSG_SOURCE)

    # target coordinate system (EPSG: 3310 - NAD83/ California Albers)
    target = osr.SpatialReference()
    target.ImportFromEPSG(Constant.COORDS_EPSG_TARGET)

    # coordinate system conversion
    transform = osr.CoordinateTransformation(source, target)
    point = ogr.Geometry(ogr.wkbPoint)
    
    coords_proj = {}
    with open(outfile,"wb") as csvfile:
        fieldnames = Header.temptazcentroidsfile
        writer = csv.writer(csvfile)
        writer.writerow(fieldnames)
        # iterate only for TAZ centroids - upto max_tazid
        for i in range(0,taz_ids.max):
            #add point
            point.AddPoint(float(x_coord[i]), float(y_coord[i]))

            # transform to a new coordinate system
            point.Transform(transform)

            # export coordinates to text
            point_text = point.ExportToWkt()

            # remove unwanted text, format ex: "POINT (279034.50343913469 -603302.04500659322 0)"
            point_text = point_text.replace("POINT (","")
            point_text = point_text.replace(" 0)","")

            # split into x and y coords, format ex: "279034.50343913469 -603302.04500659322"
            [x_coord_proj,y_coord_proj] = point_text.split(" ")
            
            coords_proj[str(taz_id[i])] = [x_coord_proj,y_coord_proj]
            
            writer.writerow([taz_id[i],x_coord[i],y_coord[i],x_coord_proj,y_coord_proj])

    return coords_proj

def read_mgra_input(mgrafile):
    """
    Reads MGRA socia-economic file
    Calculates some variables at MGRA level
    Aggregates data by TAZ
    Returns TAZ level data
    """

    data={}
    
    with open (mgrafile) as csvfile:
        reader = csv.DictReader(csvfile) # read in dictionary format
        i=1
        for row in reader:
            # calculate new variables at MGRA
            row['sqmile'] = float(row['acres'])*Constant.ACRES_TO_SQMILE
            row['land_sqmile'] = float(row['land_acres'])*Constant.ACRES_TO_SQMILE

            row['CVM_IN'] = float(row['emp_ag']) + float(row['emp_const_non_bldg_prod']) + float(row['emp_const_non_bldg_office']) + \
                            float(row['emp_const_bldg_prod']) + float(row['emp_const_bldg_office']) + float(row['emp_mfg_prod']) + float(row['emp_mfg_office'])

            row['CVM_RE'] = float(row['emp_retail'])

            row['CVM_SV'] = float(row['emp_pvt_ed_k12']) + float(row['emp_pvt_ed_post_k12_oth']) + float(row['emp_health']) + \
                            float(row['emp_personal_svcs_office']) + float(row['emp_amusement']) + float(row['emp_hotel']) + \
                            float(row['emp_restaurant_bar']) + float(row['emp_personal_svcs_retail']) + float(row['emp_religious']) + \
                            float(row['emp_pvt_hh']) + float(row['emp_public_ed'])

            row['CVM_TH'] = float(row['emp_utilities_prod']) + float(row['emp_utilities_office']) + float(row['emp_trans'])

            row['CVM_WH'] = float(row['emp_whsle_whs'])
            
            row['CVM_OFF'] = float(row['emp_prof_bus_svcs']) + float(row['emp_prof_bus_svcs_bldg_maint']) + \
                             float(row['emp_state_local_gov_ent']) + float(row['emp_fed_non_mil']) + float(row['emp_state_local_gov_blue']) + \
                             float(row['emp_state_local_gov_white']) + float(row['emp_own_occ_dwell_mgmt'])

            # aggregate data by TAZ
            if row['taz'] not in data:
                data[row['taz']] = [int(row['taz']),int(row['pop']), int(row['hh']), float(row['i1']), float(row['i2']), float(row['i3']), float(row['i4']), float(row['i5']), float(row['i6']), float(row['i7']),
                                        float(row['i8']), float(row['i9']), float(row['i10']), float(row['emp_total']), float(row['emp_fed_mil']), float(row['sqmile']),
                                        float(row['land_sqmile']), float(row['CVM_IN']), float(row['CVM_RE']), float(row['CVM_SV']), float(row['CVM_TH']), float(row['CVM_WH']), float(row['CVM_OFF'])]
            
            else:
                #taz_data[row['TAZ']][0] = int(row['TAZ'])
                data[row['taz']][1] += int(row['pop'])
                data[row['taz']][2] += int(row['hh'])
                data[row['taz']][3] += float(row['i1'])
                data[row['taz']][4] += float(row['i2'])
                data[row['taz']][5] += float(row['i3'])
                data[row['taz']][6] += float(row['i4'])
                data[row['taz']][7] += float(row['i5'])
                data[row['taz']][8] += float(row['i6'])
                data[row['taz']][9] += float(row['i7'])
                data[row['taz']][10] += float(row['i8'])
                data[row['taz']][11] += float(row['i9'])
                data[row['taz']][12] += float(row['i10'])
                data[row['taz']][13] += float(row['emp_total'])
                data[row['taz']][14] += float(row['emp_fed_mil'])
                data[row['taz']][15] += float(row['sqmile'])
                data[row['taz']][16] += float(row['land_sqmile'])
                data[row['taz']][17] += float(row['CVM_IN'])
                data[row['taz']][18] += float(row['CVM_RE'])
                data[row['taz']][19] += float(row['CVM_SV'])
                data[row['taz']][20] += float(row['CVM_TH'])
                data[row['taz']][21] += float(row['CVM_WH'])
                data[row['taz']][22] += float(row['CVM_OFF'])

    return data

# calculate taz level variables
def calculate_taz_variables(data, coords, taz_ids, outfile):
    """
    Reads TAZ data stored in read_mgra_input
    Calculates variables
    Returns calculated taz variables that would be in the output file
    """

    data_calc = {}
    with open(outfile,"wb") as csvfile:
        fieldnames = Header.temptazdatafile
        
        writer = csv.writer(csvfile)
        writer.writerow(fieldnames)
    
        for taz in range(1, taz_ids.max+1):
            if taz>=taz_ids.min:
                    
                # initialize variables to 0
                emp_dens, pop_dens, cvm_emp_dens, emp_cvm_total=(0,)*4
                per_emp, emp_servret_pct, ret_servret, emp_office, low_dens, residential, commercial, industrial=(0,)*8
                industrial_pct, transport_pct, wholesale_pct, retail_pct, service_pct, office_pct=(0,)*6
                emp_office, retail_zone=(0,)*2
                industrial_e500, transport_e500, wholesale_e500, retail_e500, service_e500, office_e500=(0,)*6
                cvm_lu_low, cvm_lu_res, cvm_lu_ret, cvm_lu_ind, cvm_lu_emp=(0,)*5

                # get data
                [taz_id,pop,hh,inc1,inc2,inc3,inc4,inc5,inc6,inc7,inc8,inc9,inc10,emp_total,emp_fed_mil,sqmile,land_sqmile,cvm_in,cvm_re,cvm_sv,cvm_th,cvm_wh,cvm_off]=data[str(taz)]

                # average hh income
                if (hh>0):
                    hh_income = (inc1*7500+inc2*22500+inc3*37500+inc4*52500+inc5*67500+inc6*87500+inc7*112500+inc8*137500+inc9*175000+inc10*225)/hh
                else:
                    hh_income=64678

                # total CVM employment = industrial + retail + service + transport + wholesale + office
                emp_cvm_total = cvm_in + cvm_re + cvm_sv + cvm_th + cvm_wh + cvm_off

                # calculate densities
                if (land_sqmile > 0):
                    emp_dens = emp_total/land_sqmile
                    pop_dens = pop/land_sqmile
                    cvm_emp_dens = emp_cvm_total/land_sqmile

                # share of employment in each sector
                if (emp_total > 0):
                    per_emp = pop/emp_total
                    emp_servret_pct = (cvm_re + cvm_sv + cvm_off)/emp_total

                    if ((cvm_re+cvm_sv+cvm_off)/emp_total) < 0.8:
                        emp_office = 1

                    # additional shares/variables - not for the final output
                    industrial_pct = cvm_in/emp_total
                    transport_pct = cvm_th/emp_total
                    wholesale_pct = cvm_wh/emp_total
                    retail_pct = cvm_re/emp_total
                    service_pct = cvm_sv/emp_total
                    office_pct = cvm_off/emp_total
                    
                    if cvm_re/emp_total > 0.5:
                        retail_zone = 1
                        
                    # end of additional variables

                # calculate flags
                if ((cvm_re+cvm_sv) > 0):
                    if ((cvm_re/(cvm_re+cvm_sv+cvm_off)) > 0.25):
                        ret_servret = 1

                # landuse flags
                
                if (emp_dens < 250 and pop_dens < 250):
                    # low density
                    low_dens = 1

                if (low_dens == 0 and pop_dens > 250 and per_emp > 2):
                    # residential
                    residential = 1

                if (low_dens == 0 and residential == 0 and emp_servret_pct > 0.6 and emp_dens > 1500 and ret_servret == 1):
                    # retail/commercial
                    commercial = 1

                if (low_dens == 0 and residential == 0 and commercial == 0 and emp_dens < 15000 and emp_office == 1):
                    # industrial
                    industrial = 1

                if (low_dens == 1 or residential == 1 or commercial == 1 or industrial == 1):
                    employment_node = 0
                else:
                    # other
                    employment_node = 1 

                # employment more than 500 flags - additional, not for the final output
                if cvm_in > 500:
                    industrial_e500 = 1
                if cvm_th > 500:
                    transport_e500 = 1
                if cvm_wh > 500:
                    wholesale_e500 = 1
                if cvm_re > 500:
                    retail_e500 = 1
                if cvm_sv > 500:
                    service_e500 = 1
                if cvm_off > 500:
                    office_e500 = 1
                    
                # end of additional variables

                # zone type
                zone_type = 1*low_dens + 2*residential + 3*commercial + 4*industrial + 5*employment_node
                sqrt_area = math.sqrt(land_sqmile)

                # TAZ centroids
                taz_x_meters = coords[str(taz)][0]
                taz_y_meters = coords[str(taz)][1]

                # landuse flags
                if zone_type == 1:
                    # low density
                    cvm_lu_low = 1
                elif zone_type == 2:
                    # residential
                    cvm_lu_res = 1
                elif zone_type == 3:
                    # retail/commercial
                    cvm_lu_ret = 1
                elif zone_type == 4:
                    # industrial
                    cvm_lu_ind = 1
                elif zone_type == 5:
                    # other
                    cvm_lu_emp = 1

                # employment by land use
                emp_lu_low = cvm_lu_low * emp_cvm_total
                emp_lu_res = cvm_lu_res * emp_cvm_total
                emp_lu_ret = cvm_lu_ret * emp_cvm_total
                emp_lu_ind = cvm_lu_ind * emp_cvm_total
                emp_lu_emp = cvm_lu_emp * emp_cvm_total

                # write all taz data to a temp file
                data[str(taz)].extend([hh_income,emp_dens,pop_dens,per_emp,emp_servret_pct,ret_servret,emp_office,
                                           low_dens,residential,commercial,industrial,employment_node,industrial_pct,
                                           transport_pct,wholesale_pct,retail_pct,service_pct,office_pct,industrial_e500,
                                           transport_e500,wholesale_e500,retail_e500,service_e500,office_e500,retail_zone,zone_type])
                writer.writerow(data[str(taz)])

                # store calculated variables
                data_calc[str(taz)]=[taz_id,pop,hh_income,land_sqmile,taz_x_meters,taz_y_meters,cvm_emp_dens,pop_dens,emp_cvm_total,
                                         emp_fed_mil,cvm_in,cvm_re,cvm_sv,cvm_th,cvm_wh,cvm_off,zone_type,sqrt_area,
                                         cvm_lu_low,cvm_lu_res,cvm_lu_ret,cvm_lu_ind,cvm_lu_emp,
                                         emp_lu_low,emp_lu_res,emp_lu_ret,emp_lu_ind,emp_lu_emp]
    return data_calc

def write_output(data, taz_ids, outfile):
    """
    Writes taz data to an output
    """

    with open(outfile,"wb") as csvfile:
        fieldnames = Header.outfile
        
        writer = csv.writer(csvfile)
        writer.writerow(fieldnames)

        for taz in range(1, taz_ids.max+1):
            if taz<taz_ids.min:
                writer.writerow([taz,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0])
            else:
                writer.writerow(data[str(taz)])

def main(argv):
    """
    Main program
    Calls functions to perform calculations
    """
    try:   
        # instantiate objects
        inputs = Input()
        outputs = Output()
        tazid_info = TazId()

        # store arguments
        inputs.projdir = argv[0]    # project directory
        mgra_filename = argv[1]     # mgra filename
        out_filename = argv[2]      # output filename

        print "Project Directory: " + inputs.projdir
        print "MGRA File: " + mgra_filename
        print "Calculting ..."    

        # input and output settings
        outputs.dir = os.path.join(inputs.projdir,"CVM\inputs")
        outputs.outfile = os.path.join(outputs.dir,out_filename)
        outputs.temptazdatafile = os.path.join(outputs.dir,"temp_tazdata.csv")
        outputs.tempcentroidfile = os.path.join(outputs.dir,"temp_tazcentroids.csv")
        
        inputs.mgrafile = os.path.join(inputs.projdir,"input",mgra_filename)
        inputs.nodebinfile = os.path.join(inputs.projdir,"output","hwy_.bin")

        # read mgra socio-economic file
        taz_data = read_mgra_input(inputs.mgrafile)

        # find min and max tazid
        [tazid_info.min, tazid_info.max] = get_tazid_range(taz_data)

        # read node bin file header
        node_fields_info = read_node_header(inputs.nodebinfile)
        
        # read node file for taz centroids. also transform coordinates
        taz_coords_proj = read_node_file(inputs.nodebinfile, node_fields_info, tazid_info, outputs.tempcentroidfile)

        # calculate taz level variables
        taz_data_calc = calculate_taz_variables(taz_data, taz_coords_proj, tazid_info, outputs.temptazdatafile)

        # write final output
        write_output(taz_data_calc, tazid_info, outputs.outfile)

    except Exception as e:
        print "Error: " + str(e)

    else:
        print "Finished."
        print "Output File: " + outputs.outfile
        
# Run
if __name__ == "__main__":
    main(sys.argv[1:])
    
    #test=["abm_to_cvm_v1.py", "C:\Projects\SANDAG_CTM_Validation\_Tasks", "mgra13_based_input2012.csv", "Zonal Properties CVM.csv"]
    #main(test[1:])
