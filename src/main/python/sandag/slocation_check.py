import pandas as pd
import numpy as np
import sys
import os
import glob

scenario_path = os.path.split(os.path.split(os.path.abspath(os.getcwd()))[0])[0]

#elementary school district
ele_list = [410301, 410901, 411201, 411501, 411701, 412001, 412101, 412601, 412701, 413001, 414101, 414301, 414801, 414901, 415101, 416001, 417601, 418101, 418501, 418701, 418801, 419001, 419101, 419701, 431001,  431101, 431201, 431501, 431701, 432001, 432501, 432801, 432901, 433601, 433801, 433901]
#high school district
high_list = [421901, 422501, 423101, 423501, 425501, 425901, 431001, 431101, 431201, 431501, 431701, 432001, 432501, 432801, 432901, 433601, 433801, 433901]

mgra_file = glob.glob(os.path.join(scenario_path + "/input/", "mgra13*.csv"))[0]
mgra_input = pd.read_csv(mgra_file,sep=',')

def check_1_ele(mgra_input):
    df_ele = pd.pivot_table(mgra_input, values=['enrollgradekto8'], index=['ech_dist'], aggfunc=np.sum).reset_index()
    if len(df_ele.loc[df_ele['enrollgradekto8']!=0]) != len(ele_list):
        print (df_ele[df_ele['enrollgradekto8']==0].index.values)
    else:
        print ("all elementary school districts have enrollment")

def check_1_high(mgra_input):
    df_high = pd.pivot_table(mgra_input, values=['enrollgrade9to12'], index=['hch_dist'], aggfunc=np.sum).reset_index()
    if len(df_high.loc[df_high['enrollgrade9to12']!=0]) != len(high_list):
        print (df_high[df_high['enrollgrade9to12']==0].index.values)
    else:
        print ("all high school districts have enrollment")


check_1_ele(mgra_input)
check_1_high(mgra_input)
