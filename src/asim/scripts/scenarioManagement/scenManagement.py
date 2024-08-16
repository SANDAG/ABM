# %%
import pandas as pd
import os
import sys
import utilities as util
_join = os.path.join

project_dir = sys.argv[1]
scenYear = int(sys.argv[2])
scenYearWithSuffix = str(sys.argv[3])

# %%
configs_dir = _join(project_dir, r'src\asim\configs')
paramByYear_dir = _join(project_dir, r'input\parametersByYears.csv')
sandag_abm_prop_dir = _join(project_dir, r'conf\sandag_abm.properties')
scripts_dir = _join(project_dir, r'src\asim\scripts')
res_ie_size_term_file = _join(project_dir, r'input\resident_ie_size_term.csv')

# %%
paramByYear = pd.read_csv(paramByYear_dir)

# %%
#update airport parameters
doc = util.open_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.CBX.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.CBX.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.CBX.airportMgra'].values[0])
util.write_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'), doc)

doc = util.open_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.SAN.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.SAN.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYearWithSuffix, 'airport.SAN.airportMgra'].values[0])
util.write_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'), doc)

doc = util.open_yaml(_join(configs_dir, 'common_airport', 'constants.yaml'))
doc['taxiCostInitial'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.baseFare'].values[0])
doc['costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.maintenance'].values[0])
doc['taxiCostPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMile'].values[0])
doc['taxiCostPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMinute'].values[0])
doc['ridehailCostInitial'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.baseFare'].values[0])
doc['ridehailCostPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMile'].values[0])
doc['ridehailCostPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMinute'].values[0])
doc['ridehailCostMinimum'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costMinimum'].values[0])
doc['ivt_brt_multiplier'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'rapid.factor.ivt'].values[0])
util.write_yaml(_join(configs_dir, 'common_airport', 'constants.yaml'), doc)

#update crossborder parameters
doc = util.open_yaml(_join(configs_dir, 'crossborder', 'constants.yaml'))
doc['scenarioYear'] = int(scenYear)
doc['taxi_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.baseFare'].values[0])
doc['cost_per_mile_fuel'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.fuel'].values[0])
doc['cost_per_mile_maint'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.maintenance'].values[0])
doc['taxi_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMile'].values[0])
doc['taxi_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMinute'].values[0])
doc['tnc_single_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.baseFare'].values[0])
doc['tnc_single_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMile'].values[0])
doc['tnc_single_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMinute'].values[0])
doc['tnc_single_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costMinimum'].values[0])
doc['tnc_shared_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.baseFare'].values[0])
doc['tnc_shared_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costPerMile'].values[0])
doc['tnc_shared_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costPerMinute'].values[0])
doc['tnc_shared_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costMinimum'].values[0])
doc['ivt_brt_multiplier'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'rapid.factor.ivt'].values[0])
util.write_yaml(_join(configs_dir, 'crossborder', 'constants.yaml'), doc)
doc = util.open_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'))
doc['tours']['num_tours'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'crossBorder.tours'].values[0])
doc['tours']['pass_shares']['sentri'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'crossBorder.sentriShare'].values[0])
doc['tours']['pass_shares']['ready'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'crossBorder.readyShare'].values[0])
doc['tours']['pass_shares']['no_pass'] = 1-(doc['tours']['pass_shares']['sentri']+doc['tours']['pass_shares']['ready'])
doc['poes'][3]['start_year'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'poe.OME.start.year'].values[0])
util.write_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'), doc)

res_ie_size_term = pd.read_csv(res_ie_size_term_file)
res_ie_size_term.loc[res_ie_size_term['OME_override'] == 1, ['start_year']] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'poe.OME.start.year'].values[0])
res_ie_size_term.to_csv(res_ie_size_term_file)

#resident/visitor model parameters
doc = util.open_yaml(_join(configs_dir, 'common', 'constants.yaml'))
doc['scenarioYear'] = int(scenYear)
doc['costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'aoc.maintenance'].values[0])
doc['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.baseFare'].values[0])
doc['Taxi_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMile'].values[0])
doc['Taxi_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'taxi.costPerMinute'].values[0])
doc['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.baseFare'].values[0])
doc['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMile'].values[0])
doc['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costPerMinute'].values[0])
doc['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.single.costMinimum'].values[0])
doc['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.baseFare'].values[0])
doc['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costPerMile'].values[0])
doc['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costPerMinute'].values[0])
doc['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'TNC.shared.costMinimum'].values[0])
doc['microVarCost'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'active.micromobility.variableCost'].values[0]*100) #cents
doc['microFixedCost'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'active.micromobility.fixedCost'].values[0]*100) #cents
doc['AV_OWNERSHIP_TARGET_PERCENT'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'Mobility.AV.Share'].values[0])
doc['ebikeownership'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'active.ebike.ownership'].values[0])
doc['WAIT_TIME_DISC'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'rapid.factor.wait'].values[0])
doc['ivt_brt_multiplier'] = float(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'rapid.factor.ivt'].values[0])
doc['hhTR_Vehyear'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'tr.veh.year'].values[0])
doc['LowIncomeBEVRebate'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'ev.rebate.lowinc.bev'].values[0])
doc['LowIncomePEVRebate'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'ev.rebate.lowinc.pev'].values[0])
doc['MedIncomeBEVRebate'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'ev.rebate.medinc.bev'].values[0])
doc['MedIncomePEVRebate'] = int(paramByYear.loc[paramByYear.year==scenYearWithSuffix, 'ev.rebate.medinc.pev'].values[0])
util.write_yaml(_join(configs_dir, 'common', 'constants.yaml'), doc)

doc = util.open_yaml(_join(configs_dir, 'resident', 'vehicle_type_choice.yaml'))
doc['FLEET_YEAR'] = int(scenYear)
doc['CONSTANTS']['scenarioYear'] = int(scenYear)
util.write_yaml(_join(configs_dir, 'resident', 'vehicle_type_choice.yaml'), doc)

sandag_abm_prop = util.load_properties(sandag_abm_prop_dir)
doc = util.open_yaml(_join(scripts_dir, 'resident', '2zoneSkim_params.yaml'))
doc['mmms']['max_maz_local_bus_stop_walk_dist_feet'] = float(sandag_abm_prop['microtransit.transit.connector.max.length'][0]) * 5280        # converting mile to feet
doc['mmms']['max_maz_premium_transit_stop_walk_dist_feet'] = float(sandag_abm_prop['microtransit.transit.connector.max.length'][1]) * 5280  # converting mile to feet
util.write_yaml(_join(scripts_dir, 'resident', '2zoneSkim_params.yaml'), doc)