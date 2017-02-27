import ogr2ogr
import os
import pymssql
import re
import subprocess
import sys


fname = 'shapefiles.txt'
scen_no_regex = re.compile('\d+(?=.shp)')
path_regex = re.compile('[\s\S]*(?=hwy_load)')

with open(fname) as f:
    shp_paths = f.readlines()

    for paths in shp_paths:
        original_file_path = paths[:-5]

        #for ext in ['dbf', 'prj', 'shp', 'shx']:
        #    if os.path.isfile(original_file_path + '.' + ext):
        #        os.rename(original_file_path + '.' + ext, original_file_path + '_wrong_preload.' + ext)

        scenario_id = scen_no_regex.search(paths).group(0)
        scenario_path = path_regex.search(paths).group(0)

        conn = pymssql.connect(host='sql2014a8', database='abm_13_2_3', as_dict=True)
        cursor = conn.cursor()
        cursor.execute('SELECT scenario_id, path, scenario_year FROM ref.scenario WHERE scenario_id = %s' % (scenario_id))
        row = cursor.fetchone()

        if row:
            print scenario_path
            ogr2ogr.main(['E:\\OSGeo4W64\\bin\\ogr2ogr.exe', '-f', 'ESRI Shapefile',
                      '{0}hwy_load_{1}.shp'.format(scenario_path, row['scenario_id']),
                      'MSSQL:server=sql2014a8;database=abm_13_2_3;trusted_connection=yes;', '-sql',
                      'SELECT scenario_id as scen_id,scenario_year as scen_yr,abm_version as abm_ver,hwy_link_id as hwy_link,hwycov_id,GEOMETRY::STGeomFromWKB(shape.STAsBinary(), 4326) as shape,link_name,length_mile as len_mile,count_jur,count_stat,count_loc,ifc,ifc_desc,ihov,itruck,post_speed,iway,imed,from_node,from_nm,to_node,to_nm,COALESCE(total_flow,0) as total_flow,COALESCE(ab_tot_flow,0) as abTotFlow,COALESCE(ba_tot_flow,0) as baTotFlow,COALESCE(ab_tot_flow, 0) * length_mile as ab_vmt, COALESCE(ba_tot_flow, 0) * length_mile as ba_vmt,COALESCE(ab_tot_flow,0) * length_mile + COALESCE(ba_tot_flow, 0) * length_mile as vmt,COALESCE(ROUND(((ab_ea_min * ab_ea_flow) + (ab_am_min * ab_am_flow) + (ab_md_min * ab_md_flow) + (ab_pm_min * ab_pm_flow) + (ab_ev_min * ab_ev_flow)) / 60, 3), 0) as ab_vht,COALESCE(ROUND(((ba_ea_min * ba_ea_flow) + (ba_am_min * ba_am_flow) + (ba_md_min * ba_md_flow) + (ba_pm_min * ba_pm_flow) + (ba_ev_min * ba_ev_flow)) / 60, 3), 0) as ba_vht,COALESCE(ROUND(((ab_ea_min * ab_ea_flow) + (ab_am_min * ab_am_flow) + (ab_md_min * ab_md_flow) + (ab_pm_min * ab_pm_flow) + (ab_ev_min * ab_ev_flow) + (ba_ea_min * ba_ea_flow) + (ba_am_min * ba_am_flow) + (ba_md_min * ba_md_flow) + (ba_pm_min * ba_pm_flow) + (ba_ev_min * ba_ev_flow)) / 60, 3), 0) as vht,COALESCE(ab_ea_flow, 0) as ab_ea_flow,COALESCE(ba_ea_flow, 0) as ba_ea_flow,COALESCE(ab_am_flow, 0) as ab_am_flow,COALESCE(ba_am_flow, 0) as ba_am_flow,COALESCE(ab_md_flow, 0) as ab_md_flow,COALESCE(ba_md_flow, 0) as ba_md_flow,COALESCE(ab_pm_flow, 0) as ab_pm_flow,COALESCE(ba_pm_flow, 0) as ba_pm_flow,COALESCE(ab_ev_flow, 0) as ab_ev_flow,COALESCE(ba_ev_flow, 0) as ba_ev_flow,COALESCE(ab_auto_flow, 0) as abAutoFlow,COALESCE(ba_auto_flow, 0) as baAutoFlow,COALESCE(ab_sov_flow, 0) as abSovFlow,COALESCE(ba_sov_flow, 0) as baSovFlow,COALESCE(ab_hov2_flow, 0) as abHov2Flow,COALESCE(ba_hov2_flow, 0) as baHov2Flow,COALESCE(ab_hov3_flow, 0) as abHov3Flow,COALESCE(ba_hov3_flow, 0) as baHov3Flow,COALESCE(ab_truck_flow, 0) as abTrucFlow,COALESCE(ba_truck_flow, 0) as baTrucFlow,COALESCE(ab_bus_flow, 0) as abBusFlow,COALESCE(ba_bus_flow, 0) as baBusFlow,ab_ea_mph,ba_ea_mph,ab_am_mph,ba_am_mph,ab_md_mph,ba_md_mph,ab_pm_mph,ba_pm_mph,ab_ev_mph,ba_ev_mph,ab_ea_min,ba_ea_min,ab_am_min,ba_am_min,ab_md_min,ba_md_min,ab_pm_min,ba_pm_min,ab_ev_min,ba_ev_min,ab_ea_lane,ba_ea_lane,ab_am_lane,ba_am_lane,ab_md_lane,ba_md_lane,ab_pm_lane,ba_pm_lane,ab_ev_lane,ba_ev_lane,ab_ea_voc,ba_ea_voc,ab_am_voc,ba_am_voc,ab_md_voc,ba_md_voc,ab_pm_voc,ba_pm_voc,ab_ev_voc,ba_ev_voc FROM abm.fn_hwy_vol_by_mode_and_tod ({0})'.format(
                          row['scenario_id']),
                      '-overwrite', '-s_srs', 'EPSG:4326', '-t_srs', 'EPSG:2230'])
            #subprocess.check_call(os_command))
            #os_command = ['E:\\OSGeo4W64\\bin\\ogr2ogr.exe', '-f', 'ESRI Shapefile',
            #          '{0}hwy_load_{1}.shp'.format(scenario_path, row['scenario_id']),
            #          'MSSQL:server=sql2014a8;database=abm_13_2_3;trusted_connection=yes;', '-sql',
            #          'SELECT scenario_id as scen_id,scenario_year as scen_yr,abm_version as abm_ver,hwy_link_id as hwy_link,hwycov_id,GEOMETRY::STGeomFromWKB(shape.STAsBinary(), 4326) as shape,link_name,length_mile as len_mile,count_jur,count_stat,count_loc,ifc,ifc_desc,ihov,itruck,post_speed,iway,imed,from_node,from_nm,to_node,to_nm,COALESCE(total_flow,0) as total_flow,COALESCE(ab_tot_flow,0) as abTotFlow,COALESCE(ba_tot_flow,0) as baTotFlow,COALESCE(ab_tot_flow, 0) * length_mile as ab_vmt, COALESCE(ba_tot_flow, 0) * length_mile as ba_vmt,COALESCE(ab_tot_flow,0) * length_mile + COALESCE(ba_tot_flow, 0) * length_mile as vmt,COALESCE(ROUND(((ab_ea_min * ab_ea_flow) + (ab_am_min * ab_am_flow) + (ab_md_min * ab_md_flow) + (ab_pm_min * ab_pm_flow) + (ab_ev_min * ab_ev_flow)) / 60, 3), 0) as ab_vht,COALESCE(ROUND(((ba_ea_min * ba_ea_flow) + (ba_am_min * ba_am_flow) + (ba_md_min * ba_md_flow) + (ba_pm_min * ba_pm_flow) + (ba_ev_min * ba_ev_flow)) / 60, 3), 0) as ba_vht,COALESCE(ROUND(((ab_ea_min * ab_ea_flow) + (ab_am_min * ab_am_flow) + (ab_md_min * ab_md_flow) + (ab_pm_min * ab_pm_flow) + (ab_ev_min * ab_ev_flow) + (ba_ea_min * ba_ea_flow) + (ba_am_min * ba_am_flow) + (ba_md_min * ba_md_flow) + (ba_pm_min * ba_pm_flow) + (ba_ev_min * ba_ev_flow)) / 60, 3), 0) as vht,COALESCE(ab_ea_flow, 0) as ab_ea_flow,COALESCE(ba_ea_flow, 0) as ba_ea_flow,COALESCE(ab_am_flow, 0) as ab_am_flow,COALESCE(ba_am_flow, 0) as ba_am_flow,COALESCE(ab_md_flow, 0) as ab_md_flow,COALESCE(ba_md_flow, 0) as ba_md_flow,COALESCE(ab_pm_flow, 0) as ab_pm_flow,COALESCE(ba_pm_flow, 0) as ba_pm_flow,COALESCE(ab_ev_flow, 0) as ab_ev_flow,COALESCE(ba_ev_flow, 0) as ba_ev_flow,COALESCE(ab_auto_flow, 0) as abAutoFlow,COALESCE(ba_auto_flow, 0) as baAutoFlow,COALESCE(ab_sov_flow, 0) as abSovFlow,COALESCE(ba_sov_flow, 0) as baSovFlow,COALESCE(ab_hov2_flow, 0) as abHov2Flow,COALESCE(ba_hov2_flow, 0) as baHov2Flow,COALESCE(ab_hov3_flow, 0) as abHov3Flow,COALESCE(ba_hov3_flow, 0) as baHov3Flow,COALESCE(ab_truck_flow, 0) as abTrucFlow,COALESCE(ba_truck_flow, 0) as baTrucFlow,COALESCE(ab_bus_flow, 0) as abBusFlow,COALESCE(ba_bus_flow, 0) as baBusFlow,ab_ea_mph,ba_ea_mph,ab_am_mph,ba_am_mph,ab_md_mph,ba_md_mph,ab_pm_mph,ba_pm_mph,ab_ev_mph,ba_ev_mph,ab_ea_min,ba_ea_min,ab_am_min,ba_am_min,ab_md_min,ba_md_min,ab_pm_min,ba_pm_min,ab_ev_min,ba_ev_min,ab_ea_lane,ba_ea_lane,ab_am_lane,ba_am_lane,ab_md_lane,ba_md_lane,ab_pm_lane,ba_pm_lane,ab_ev_lane,ba_ev_lane,ab_ea_voc,ba_ea_voc,ab_am_voc,ba_am_voc,ab_md_voc,ba_md_voc,ab_pm_voc,ba_pm_voc,ab_ev_voc,ba_ev_voc FROM abm.fn_hwy_vol_by_mode_and_tod ({0})'.format(
            #              row['scenario_id']),
            #          '-overwrite', '-s_srs', 'EPSG:4326', '-t_srs', 'EPSG:2230']
            #subprocess.check_call(os_command)
        else:
            print 'Scenario Not Available: %s, %s' % (scenario_id, scenario_path)