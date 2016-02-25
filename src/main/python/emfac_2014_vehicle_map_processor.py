"""
The module processes a EMFAC default annual VMT report and creates an SANDAG to EMFAC vehicle
map for loading into the database.

Example:
    $ python emfac_2014_vehicle_map_processor.py default_san_diego_2017_annual_vmt.csv

Attributes:
    input_file (string): Path to a default annual output from EMFAC of VMT by vehicle class.

Author:
    Clint Daniels (San Diego Association of Governments)
"""

import pandas as pd
import sys

from pysandag.database import get_connection_string
from sqlalchemy import create_engine

input_path = sys.argv[1]

emfac_vehicle_class_sql = """
    SELECT emfac_vehicle_class_id, UPPER(emfac_vehicle_class) AS emfac_vehicle_class
        FROM emfac.emfac_vehicle_class
        WHERE emfac_vehicle_class_id > 51
"""

class_map_sql = """
    SELECT sandag_vehicle_class_id,emfac_vehicle_class_id
        FROM emfac.emfac_vehicle_map
        WHERE year = 2012 and emfac_vehicle_class_id > 51
"""

default_emfac_vmt = pd.read_csv(input_path)
default_emfac_vmt['emfac_vehicle_class'] = default_emfac_vmt.apply(
        lambda x: x.vehicle_class.upper() + ' - ' + x.fuel.upper()
        , axis=1)
calendar_year = default_emfac_vmt['calendar_year'].min()

# RESET THE INDEX
default_emfac_vmt = default_emfac_vmt[['emfac_vehicle_class', 'vmt']].set_index('emfac_vehicle_class')

default_emfac_vmt.ix['LDA - GAS']['vmt'] = \
    default_emfac_vmt.ix['LDA - GAS']['vmt'] + default_emfac_vmt.ix['LDA - ELEC']['vmt']
default_emfac_vmt.ix['LDT1 - GAS']['vmt'] = \
    default_emfac_vmt.ix['LDT1 - GAS']['vmt'] + default_emfac_vmt.ix['LDT1 - ELEC']['vmt']

# READ IN THE VEHICLE CLASSES FROM THE DATABASE
sql_in_engine = create_engine(get_connection_string("dbconfig.yml", 'in_db'))

emfac_class_mapping = pd.read_sql(emfac_vehicle_class_sql, sql_in_engine, index_col='emfac_vehicle_class')

# JOIN DATA AND CLEAN-UP ELECTRIC VEHICLE
emfac_vmt = default_emfac_vmt.join(emfac_class_mapping, how='outer')
emfac_vmt = emfac_vmt[~emfac_vmt.index.isin(['LDA - ELEC', 'LDT1 - ELEC'])]
emfac_vmt.fillna(0, inplace=True)
emfac_vmt.reset_index(inplace=True)
emfac_vmt.sort(columns="emfac_vehicle_class_id", inplace=True)
emfac_vmt.set_index('emfac_vehicle_class_id', inplace=True)

# GROUP BY AND GET PERCENTAGE BY GROUP
class_map = pd.read_sql(class_map_sql, sql_in_engine, index_col='emfac_vehicle_class_id')
class_map = class_map.join(emfac_vmt, how='left')
class_map.reset_index(inplace=True)
class_map.sort(['sandag_vehicle_class_id', 'emfac_vehicle_class_id'], inplace=True)
class_map.set_index(['sandag_vehicle_class_id', 'emfac_vehicle_class_id'], inplace=True)
class_map = class_map['vmt'].groupby(level=0).apply(lambda x: x / float(x.sum())).to_frame('value')
class_map['year'] = calendar_year

# EXPORT THE RESULT
class_map.to_csv('emfac_vehicle_map_{0}.csv'.format(calendar_year))
