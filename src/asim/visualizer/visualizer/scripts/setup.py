import os
from shutil import copy

#run = r'T:/ABM/ABM_FY19/model_runs/ABM2Plus/v1221/2016_1422new_sxu'
wd = os.path.dirname(os.path.dirname(os.path.realpath(__file__))).replace('\\', '/')
run = os.path.dirname(wd)

#Get max iteration number
#MAX_ITER = 0
#for f in os.listdir(os.path.join(run, 'output')):
#    if 'householdData_' in f and f[-4:] == '.csv':
#        MAX_ITER = int(f[-5])

#import pdb
#pdb.set_trace()

#Set up data pipeline settings
settings_file = os.path.join(wd, 'data_pipeliner', 'config', 'settings.yaml')
processor_file = os.path.join(wd, 'data_pipeliner', 'config', 'processor.csv')
config_path = os.path.dirname(settings_file)
copy(os.path.join(wd, 'config', 'settings.yaml'), config_path)
copy(os.path.join(wd, 'config', 'processor.csv'), config_path)
copy(os.path.join(wd, 'config', 'expressions.csv'), config_path)

#Settings
f = open(settings_file, 'r')
data = f.read()
f.close()

data = data.replace('[MODEL_RUN]', run)

f = open(settings_file, 'w')
f.write(data)
f.close()

#Processor
f = open(processor_file, 'r')
data = f.read()
f.close()

data = data.replace('[MODEL_RUN]', run)

f = open(processor_file, 'w')
f.write(data)
f.close()

#Set up combine settings
combine_file = os.path.join(wd, 'combine.yaml')
copy(os.path.join(wd, 'config', 'combine.yaml'), os.path.split(combine_file)[0])

f = open(combine_file, 'r')
data = f.read()
f.close()

data = data.replace('VISUALIZER_PATH', wd)

f = open(combine_file, 'w')
f.write(data)
f.close()