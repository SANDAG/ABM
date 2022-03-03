import os
import glob
import pandas as pd
import pyodbc

toolpath = os.getcwd()[3:-7]

conn = pyodbc.connect("DRIVER={SQL Server};"
                      "SERVER=DDAMWSQL16;"
                      "DATABASE=abm_14_2_0;" 
                      "Trusted_Connection=yes;")                       
sql = ("SELECT * FROM [abm_14_2_0].[dimension].[scenario] where RIGHT(path, len(path)-23) = '%s'" % toolpath)  
df_sql = pd.read_sql_query(sql, conn)
scenid = df_sql['scenario_id'].max()
list = glob.glob(os.getcwd()[:-6]+'report\\hwyload*')
list_shape = glob.glob(os.getcwd()[:-6]+'report\\hwyload*.shp')

if len(list_shape) and len(df_sql):
    for item in list:
        if 'csv' not in item:
            try:
                os.rename(item, os.getcwd()[:-6]+'report\\hwyLoad_'+ str(scenid) + item[-4:])
            except Exception as error:
                print('Caught this error: ' + repr(error))
    print ('The scenaio ID has been added to the shapefile.')
else:
    print ("Cannot find the scenario in the SQ database or hwyloadshape file is not available. Please check...")