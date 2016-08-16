import os
import pymssql
import sys
import urllib2

from ntlm import HTTPNtlmAuthHandler
from time import strftime

if len(sys.argv) != 4:
    sys.exit("Must provide user and password")

#Windows username and password to access report server
scenario_id = sys.argv[1]
user = sys.argv[2]
password = sys.argv[3]

#Database User Name and Password
db_server = 'sql2014a8'
db_name = 'abm_13_2_3'
db_user = user
db_password = password
 

output_path = 'abm_reports_{0}_{1}'.format(scenario_id, strftime('%Y_%m_%d'))

if not (os.access(output_path, os.F_OK)):
    os.mkdir(os.getcwd() + '/' + output_path)

if not (os.access(output_path + '/pdf', os.F_OK)):
    os.mkdir(os.getcwd() + '/' + output_path + '/pdf')
    
if not (os.access(output_path + '/xlsx', os.F_OK)):
    os.mkdir(os.getcwd() + '/' + output_path + '/xlsx')

conn = pymssql.connect(host=db_server, user=db_user, password=db_password, database=db_name)
cur = conn.cursor()

mgra_sql = "SELECT zone FROM ref.geography_zone WHERE geography_type_id = 90 ORDER BY zone"

cur.execute(mgra_sql)

for row in cur:
    for ftype, x in {'PDF':'pdf'}.iteritems(): #'PDF':'pdf','EXCELOPENXML':'xlsx'
        url = 'http://sql2014a8/ReportServer?%2fabm_reporting%2fService+Bureau%2factivity_report&scenario_id={0}&mgra={1}&rs:format={2}'.format(scenario_id, row[0],ftype)

        print url
    
        passman = urllib2.HTTPPasswordMgrWithDefaultRealm()
        passman.add_password(None, url, user, password)

        auth_NLTM = HTTPNtlmAuthHandler.HTTPNtlmAuthHandler(passman)

        opener = urllib2.build_opener(auth_NLTM)

        urllib2.install_opener(opener)

        pagehandle = urllib2.urlopen(url)

        file = '{0}\\{1}\\report_{2}_{3}.{4}'.format(output_path, x, scenario_id, row[0], x)
        
        localFile = open(file, 'wb')
        localFile.write(pagehandle.read())
        localFile.close()