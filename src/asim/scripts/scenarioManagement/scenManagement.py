# %%
import pandas as pd
import os
import sys
import utilities as util
_join = os.path.join

project_dir = sys.argv[1]
scenYear = int(sys.argv[2])

# %%
configs_dir = _join(project_dir, r'src\asim\configs')
paramByYear_dir = _join(project_dir, r'input\parametersByYears.csv')
sandag_abm_prop_dir = _join(project_dir, r'conf\sandag_abm.properties')
scripts_dir = _join(project_dir, r'src\asim\scripts')

# %%
paramByYear = pd.read_csv(paramByYear_dir)

# %%
#update airport parameters
doc = util.open_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.airportMgra'].values[0])
util.write_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'), doc)
doc = util.open_yaml(_join(configs_dir, 'airport.CBX', 'trip_mode_choice.yaml'))
doc['CONSTANTS']['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['CONSTANTS']['costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.maintenance'].values[0])
doc['CONSTANTS']['Taxi_perMileFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['CONSTANTS']['Taxi_perMinuteFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['CONSTANTS']['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['CONSTANTS']['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['CONSTANTS']['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['CONSTANTS']['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['CONSTANTS']['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
util.write_yaml(_join(configs_dir, 'airport.CBX', 'trip_mode_choice.yaml'), doc)

doc = util.open_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.airportMgra'].values[0])
util.write_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'), doc)
doc = util.open_yaml(_join(configs_dir, 'airport.SAN', 'trip_mode_choice.yaml'))
doc['CONSTANTS']['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['CONSTANTS']['costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.maintenance'].values[0])
doc['CONSTANTS']['Taxi_perMileFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['CONSTANTS']['Taxi_perMinuteFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['CONSTANTS']['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['CONSTANTS']['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['CONSTANTS']['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['CONSTANTS']['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['CONSTANTS']['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['CONSTANTS']['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
util.write_yaml(_join(configs_dir, 'airport.SAN', 'trip_mode_choice.yaml'), doc)

#update crossborder parameters
doc = util.open_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'))
doc['tours']['num_tours'] = int(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.tours'].values[0])
doc['tours']['pass_shares']['sentri'] = float(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.sentriShare'].values[0])
doc['tours']['pass_shares']['ready'] = float(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.readyShare'].values[0])
doc['tours']['pass_shares']['no_pass'] = 1-(doc['tours']['pass_shares']['sentri']+doc['tours']['pass_shares']['ready'])
util.write_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'), doc)
doc = util.open_yaml(_join(configs_dir, 'crossborder', 'trip_mode_choice.yaml'))
doc['CONSTANTS']['taxi_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['CONSTANTS']['cost_per_mile_fuel'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.fuel'].values[0])
doc['CONSTANTS']['cost_per_mile_maint'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.maintenance'].values[0])
doc['CONSTANTS']['taxi_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['CONSTANTS']['taxi_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['CONSTANTS']['tnc_single_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['CONSTANTS']['tnc_single_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['CONSTANTS']['tnc_single_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['CONSTANTS']['tnc_single_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['CONSTANTS']['tnc_shared_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['CONSTANTS']['tnc_shared_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['CONSTANTS']['tnc_shared_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['CONSTANTS']['tnc_shared_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
util.write_yaml(_join(configs_dir, 'crossborder', 'trip_mode_choice.yaml'), doc)

#resident/visitor model parameters
doc = util.open_yaml(_join(configs_dir, 'common', 'constants.yaml'))
doc['scenarioYear'] = int(scenYear)
doc['costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.maintenance'].values[0])
doc['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['Taxi_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['Taxi_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
doc['microVarCost'] = float(paramByYear.loc[paramByYear.year==scenYear, 'active.micromobility.variableCost'].values[0]*100) #cents
doc['microFixedCost'] = float(paramByYear.loc[paramByYear.year==scenYear, 'active.micromobility.fixedCost'].values[0]*100) #cents
util.write_yaml(_join(configs_dir, 'common', 'constants.yaml'), doc)
doc = util.open_yaml(_join(configs_dir, 'resident', 'av_ownership.yaml'))
doc['AV_OWNERSHIP_TARGET_PERCENT'] = float(paramByYear.loc[paramByYear.year==scenYear, 'Mobility.AV.Share'].values[0])
util.write_yaml(_join(configs_dir, 'resident', 'av_ownership.yaml'), doc)

doc = util.open_yaml(_join(configs_dir, 'resident', 'vehicle_type_choice.yaml'))
doc['FLEET_YEAR'] = int(scenYear)
util.write_yaml(_join(configs_dir, 'resident', 'vehicle_type_choice.yaml'), doc)

sandag_abm_prop = util.load_properties(sandag_abm_prop_dir)
doc = util.open_yaml(_join(scripts_dir, 'resident', '2zoneSkim_params.yaml'))
doc['mmms']['max_maz_local_bus_stop_walk_dist_feet'] = float(sandag_abm_prop['microtransit.transit.connector.max.length'][0]) * 5280        # converting mile to feet
doc['mmms']['max_maz_premium_transit_stop_walk_dist_feet'] = float(sandag_abm_prop['microtransit.transit.connector.max.length'][1]) * 5280  # converting mile to feet
util.write_yaml(_join(scripts_dir, 'resident', '2zoneSkim_params.yaml'), doc)