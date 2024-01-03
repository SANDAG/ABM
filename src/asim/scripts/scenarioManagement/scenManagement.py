# %%
import pandas as pd
import ruamel.yaml as yamlru
import sys
import os
_join = os.path.join

project_dir = sys.argv[1]
scenYear = int(sys.argv[2])

# %%
def open_yaml(yaml_file):
    with open(yaml_file, 'r') as stream:
        contents = stream.read()
        print(f"Contents of {yaml_file}: {contents}")
        stream.seek(0)  # Reset the stream position to the start of the file
        try:
            return yamlru.load(stream, Loader=yamlru.RoundTripLoader)
        except yamlru.YAMLError as exc:
            print(exc)

def write_yaml(yaml_file, yaml_dict):
    with open(yaml_file, 'w') as outfile:
        yamlru.dump(yaml_dict, outfile, Dumper=yamlru.RoundTripDumper)

# %%
configs_dir = _join(project_dir, r'src\asim\configs')
paramByYear_dir = _join(project_dir, r'input\parametersByYears.csv')

# %%
paramByYear = pd.read_csv(paramByYear_dir)

# %%
#update airport parameters
doc = open_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.CBX.airportMgra'].values[0])
write_yaml(_join(configs_dir, 'airport.CBX', 'preprocessing.yaml'), doc)
doc = open_yaml(_join(configs_dir, 'airport.CBX', 'trip_mode_choice.yaml'))
doc['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['Taxi_perMileFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['Taxi_perMinuteFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
write_yaml(_join(configs_dir, 'airport.CBX', 'trip_mode_choice.yaml'), doc)

doc = open_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'))
doc['tours']['num_enplanements'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.enplanements'].values[0])
doc['tours']['connecting'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.connecting'].values[0])
doc['tours']['airport_mgra'] = int(paramByYear.loc[paramByYear['year'] == scenYear, 'airport.SAN.airportMgra'].values[0])
write_yaml(_join(configs_dir, 'airport.SAN', 'preprocessing.yaml'), doc)
doc = open_yaml(_join(configs_dir, 'airport.SAN', 'trip_mode_choice.yaml'))
doc['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['Taxi_perMileFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['Taxi_perMinuteFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['TNC_single_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['TNC_single_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['TNC_single_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['TNC_single_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['TNC_shared_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['TNC_shared_costPerMile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['TNC_shared_costPerMinute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['TNC_shared_costMinimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
write_yaml(_join(configs_dir, 'airport.SAN', 'trip_mode_choice.yaml'), doc)

#update crossborder parameters
doc = open_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'))
doc['tours']['num_tours'] = int(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.tours'].values[0])
doc['tours']['pass_shares']['sentri'] = float(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.sentriShare'].values[0])
doc['tours']['pass_shares']['ready'] = float(paramByYear.loc[paramByYear.year==scenYear, 'crossBorder.readyShare'].values[0])
doc['tours']['pass_shares']['no_pass'] = 1-(doc['tours']['pass_shares']['sentri']+doc['tours']['pass_shares']['ready'])
write_yaml(_join(configs_dir, 'crossborder', 'preprocessing.yaml'), doc)
doc = open_yaml(_join(configs_dir, 'crossborder', 'trip_mode_choice.yaml'))
doc['taxi_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['taxi_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['taxi_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
doc['tnc_single_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.baseFare'].values[0])
doc['tnc_single_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMile'].values[0])
doc['tnc_single_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costPerMinute'].values[0])
doc['tnc_single_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.single.costMinimum'].values[0])
doc['tnc_shared_base_fare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.baseFare'].values[0])
doc['tnc_shared_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMile'].values[0])
doc['tnc_shared_cost_per_minute'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costPerMinute'].values[0])
doc['tnc_shared_cost_minimum'] = float(paramByYear.loc[paramByYear.year==scenYear, 'TNC.shared.costMinimum'].values[0])
write_yaml(_join(configs_dir, 'crossborder', 'trip_mode_choice.yaml'), doc)

#resident model parameters
doc = open_yaml(_join(configs_dir, 'common', 'constants.yaml'))
doc['scenarioYear'] = int(scenYear)
doc['c_auto_operating_cost_per_mile'] = float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.fuel'].values[0]) + float(paramByYear.loc[paramByYear.year==scenYear, 'aoc.maintenance'].values[0])
doc['Taxi_baseFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.baseFare'].values[0])
doc['Taxi_perMileFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMile'].values[0])
doc['Taxi_perMinuteFare'] = float(paramByYear.loc[paramByYear.year==scenYear, 'taxi.costPerMinute'].values[0])
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
write_yaml(_join(configs_dir, 'common', 'constants.yaml'), doc)
doc = open_yaml(_join(configs_dir, 'resident', 'av_ownership.yaml'))
doc['AV_OWNERSHIP_TARGET_PERCENT'] = float(paramByYear.loc[paramByYear.year==scenYear, 'Mobility.AV.Share'].values[0])
write_yaml(_join(configs_dir, 'resident', 'av_ownership.yaml'), doc)