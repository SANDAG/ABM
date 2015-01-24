import imp
import pyodbc
import sys
try:
    imp.find_module('_mssql')
except ImportError:
    print 'The current version of Python cannot find the _mssql <http://www.pymssql.org> package.\nPlease ensure _mssql package is properly installed.\nProgram Failed.'
    sys.exit(-1)
'''
try:
    imp.find_module('xlwt')
except ImportError:
    print 'The current version of Python cannot find the xlwt <https://github.com/python-excel/xlwt> package.\nPlease ensure xlwt package is properly installed.\nProgram Failed.'
    sys.exit(-1)
'''
import _mssql
from xlwt import Workbook

if len(sys.argv) != 3:
    print 'Correct Usage: emfac2011_abm.py <Scenario ID> <Output Path>'
    sys.exit(-1)
    

scenario_id = int(sys.argv[1])
saveLocation = sys.argv[2]

AreaType = 'MPO'
Area = 'SANDAG'
Scenario = 1
SubArea = 'San Diego (SD)'
Title = 'Group #1 (SANDAG), Scenario #1 - San Diego (SD) #YEAR# #SEASON#'
VMTProfile = 'User'
VMTVehCat = 'User'
SpeedProfile = 'User'
total_vmt = 0


seasons = ['ANNUAL', 'SUMMER', 'WINTER']
#conn = _mssql.connect(server='pele', user='emfac', password='3mf@c', database='abm_sd')
conn = pyodbc.connect(driver='{SQL Server}', server='sql2014a8', database='abm_13_2_3',trusted_connection='yes')
cursor = conn.cursor()

print 'Creating the EMFAC 2011 User Defined Input Workbook and Worksheets'
book = Workbook()
sheet_reg_scen = book.add_sheet('Regional_Scenarios')
sheet_base_scen = book.add_sheet('Scenario_Base_Inputs')
sheet_scen_vmt = book.add_sheet('Scenario_VMT_by_VehCat')
sheet_scen_speed = book.add_sheet('Scenario_Speed_Profiles')

####WRITE REGIONAL SCENARIOS SHEET####

#Write Column Headers for Regional Scenarios Sheet
print 'Writing header row for Regional_Scenarios worksheet'
sheet_reg_scen.write(0,0,'Group')
sheet_reg_scen.write(0,1,'Area Type')
sheet_reg_scen.write(0,2,'Area')
sheet_reg_scen.write(0,3,'CalYr')
sheet_reg_scen.write(0,4,'Season')

#Get Scenario Year
print 'Retrieving the scenario year'
yearQuery = 'SELECT s.SCENARIO_DESC as sName, s.SCENARIO_YEAR AS fileYear, CASE WHEN s.SCENARIO_YEAR > 2035 THEN 2035 ELSE s.SCENARIO_YEAR END as YR FROM ref.scenario s WHERE SCENARIO_ID = ?'
cursor.execute(yearQuery, scenario_id)
row = cursor.fetchone()
year = row.YR
fileYear = row.fileYear #different from year if 2036 and beyond scenario
scenarioName = row.sName
print 'Writing rows for Regional_Scenarios worksheet'
for idx, season in enumerate(seasons):
    sheet_reg_scen.row(idx + 1).write(0, idx+1)
    sheet_reg_scen.row(idx + 1).write(1, AreaType)
    sheet_reg_scen.row(idx + 1).write(2, Area)
    sheet_reg_scen.row(idx + 1).write(3, year)
    sheet_reg_scen.row(idx + 1).write(4, season)
    

#Write Column Headers for Scenario VMT by VehCat Sheet
print 'Writing header row for Scenario_VMT_by_VehCat worksheet'
sheet_scen_vmt.write(0,0,'Group')
sheet_scen_vmt.write(0,1,'Area')
sheet_scen_vmt.write(0,2,'Scenario')
sheet_scen_vmt.write(0,3,'Sub-Area')
sheet_scen_vmt.write(0,4,'CalYr')
sheet_scen_vmt.write(0,5,'Season')
sheet_scen_vmt.write(0,6,'Title')
sheet_scen_vmt.write(0,7,'Veh & Tech')
sheet_scen_vmt.write(0,8,'New VMT')


counter = 1
for idx, season in enumerate(seasons):
    print 'Querying VMT records for ' + season
    query = 'SELECT * FROM [emfac].[FN_EMFAC_2011_VMT] (?,?,?) order by [Veh & Tech]'
    cursor.execute(query,   scenario_id, idx+1, season)
    rows = cursor.fetchall()
    
    print 'Writing rows for Scenario_VMT_by_VehCat worksheet: ' + season
    for row in rows:
        sheet_scen_vmt.row(counter).write(0, row.Group)
        sheet_scen_vmt.row(counter).write(1, row.Area)
        sheet_scen_vmt.row(counter).write(2, row.Scenario)
        sheet_scen_vmt.row(counter).write(3, row[3])
        sheet_scen_vmt.row(counter).write(4, row.CalYr)
        sheet_scen_vmt.row(counter).write(5, row.Season)
        sheet_scen_vmt.row(counter).write(6, row.Title)
        sheet_scen_vmt.row(counter).write(7, row[7])
        sheet_scen_vmt.row(counter).write(8, row[8])
	counter += 1
        if idx == 0:
            total_vmt += row[8]      

#Write Column Headers for Scenario Base Inputs Sheet
print 'Writing header row for Scenario_Base_Inputs worksheet'
sheet_base_scen.write(0,0,'Group')
sheet_base_scen.write(0,1,'Area')
sheet_base_scen.write(0,2,'Scenario')
sheet_base_scen.write(0,3,'Sub-Area')
sheet_base_scen.write(0,4,'CalYr')
sheet_base_scen.write(0,5,'Season')
sheet_base_scen.write(0,6,'Title')
sheet_base_scen.write(0,7,'VMT Profile')
sheet_base_scen.write(0,8,'VMT by Vehicle Category')
sheet_base_scen.write(0,9,'Speed Profile')
sheet_base_scen.write(0,10,'New Total VMT')

print 'Writing rows for Scenario_Base_Inputs worksheet'
for idx, season in enumerate(seasons):
    sheet_base_scen.row(idx + 1).write(0, idx+1)
    sheet_base_scen.row(idx + 1).write(1, Area)
    sheet_base_scen.row(idx + 1).write(2, Scenario)
    sheet_base_scen.row(idx + 1).write(3, SubArea)
    sheet_base_scen.row(idx + 1).write(4, year)
    sheet_base_scen.row(idx + 1).write(5, season)
    sheet_base_scen.row(idx + 1).write(6, Title.replace('#YEAR#', str(year)).replace('#SEASON#', season))
    sheet_base_scen.row(idx + 1).write(7, VMTProfile)
    sheet_base_scen.row(idx + 1).write(8, VMTVehCat)
    sheet_base_scen.row(idx + 1).write(9, SpeedProfile)
    sheet_base_scen.row(idx + 1).write(10, total_vmt)

#Write Column Headers for Scenario Speed Profiles Sheet
print 'Writing header row for Scenario_Speed_Profiles worksheet'
sheet_scen_speed.write(0,0,'Group')
sheet_scen_speed.write(0,1,'Area')
sheet_scen_speed.write(0,2,'Scenario')
sheet_scen_speed.write(0,3,'Sub-Area')
sheet_scen_speed.write(0,4,'CalYr')
sheet_scen_speed.write(0,5,'Season')
sheet_scen_speed.write(0,6,'Title')
sheet_scen_speed.write(0,7,'Veh & Tech')
sheet_scen_speed.write(0,8,'EMFAC2007 Veh & Tech')
sheet_scen_speed.write(0,9,'5MPH')
sheet_scen_speed.write(0,10,'10MPH')
sheet_scen_speed.write(0,11,'15MPH')
sheet_scen_speed.write(0,12,'20MPH')
sheet_scen_speed.write(0,13,'25MPH')
sheet_scen_speed.write(0,14,'30MPH')
sheet_scen_speed.write(0,15,'35MPH')
sheet_scen_speed.write(0,16,'40MPH')
sheet_scen_speed.write(0,17,'45MPH')
sheet_scen_speed.write(0,18,'50MPH')
sheet_scen_speed.write(0,19,'55MPH')
sheet_scen_speed.write(0,20,'60MPH')
sheet_scen_speed.write(0,21,'65MPH')
sheet_scen_speed.write(0,22,'70MPH')

counter = 1
for idx, season in enumerate(seasons):
    query = 'SELECT * FROM emfac.FN_EMFAC_2011_VMT_SPEED (?,?,?) order by [Veh & Tech]'
    print 'Querying VMT Speed records for ' + season
    cursor.execute(query, scenario_id, idx+1, season)
    rows = cursor.fetchall()
    
    print 'Writing rows for Scenario_Speed_Profiles worksheet: ' + season
    for row in rows:
        sheet_scen_speed.row(counter).write(0,row.Group)
        sheet_scen_speed.row(counter).write(1,row.Area)
        sheet_scen_speed.row(counter).write(2,row.Scenario)
        sheet_scen_speed.row(counter).write(3,row[3])
        sheet_scen_speed.row(counter).write(4,row.CalYr)
        sheet_scen_speed.row(counter).write(5,row.Season)
        sheet_scen_speed.row(counter).write(6,row.Title)
        sheet_scen_speed.row(counter).write(7,row[7])
        #sheet_scen_speed.row(counter).write(8,row['']) #EMFAC2007 Codes
        sheet_scen_speed.row(counter).write(9,row[8])
        sheet_scen_speed.row(counter).write(10,row[9])
        sheet_scen_speed.row(counter).write(11,row[10])
        sheet_scen_speed.row(counter).write(12,row[11])
        sheet_scen_speed.row(counter).write(13,row[12])
        sheet_scen_speed.row(counter).write(14,row[13])
        sheet_scen_speed.row(counter).write(15,row[14])
        sheet_scen_speed.row(counter).write(16,row[15])
        sheet_scen_speed.row(counter).write(17,row[16])
        sheet_scen_speed.row(counter).write(18,row[17])
        sheet_scen_speed.row(counter).write(19,row[18])
        sheet_scen_speed.row(counter).write(20,row[19])
        sheet_scen_speed.row(counter).write(21,row[20])
        sheet_scen_speed.row(counter).write(22,row[21])
        counter += 1
outLocation = saveLocation + '\EMFAC2011-SANDAG-'+str(scenarioName)+'-'+str(fileYear)+'.xls'
print 'Saving worksheet to ' + outLocation
book.save(outLocation)
conn.close()
