# -*- coding: utf-8 -*-
"""
Synthetic Establishment Functions
Created on Thu Sep 21 10:58:51 2023
Revised Oct 16, 2023

@author: jgliebe
"""
# =============================================================================
# Size Class Definitions (number of employees)				
# 1	"< 5"			
# 2	"5 to 9"			
# 3	"10 to 19"			
# 4	"20 to 49"			
# 5	"50 to 99"			
# 6	"100 to 249"			
# 7	">= 250"
# =============================================================================

import numpy as np
import random

# Create sizeClass dictionary
def sizeClass(emp=0):
    try:
        emp = int(round(emp))
    except:
        emp = 0
    if emp in range(1, 5): return 1
    elif emp in range(5, 10): return 2
    elif emp in range(10, 20): return 3
    elif emp in range(20, 50): return 4
    elif emp in range(50, 100): return 5
    elif emp in range(100, 250): return 6
    elif emp in range(250, 99999): return 7
    else: return 0
    
# Create sizeClass Min, Max dictionary
def sizeClassMinMax(sizeClass=0):
    D = {
    1: (1, 4),
    2: (5, 9),
    3: (10, 19),
    4: (20, 49),
    5: (50, 99),
    6: (100, 249),
    7: (250, 99999),
    0: (0, 0) 
    }
    if sizeClass not in list(D.keys()):
        sizeClass = 0        
    return D[int(sizeClass)]

def getMidRangeValue(x=0, divisor=2):
    minn, maxx = sizeClassMinMax(x)
    mid = (maxx - minn)/float(divisor) + minn
    if x==7: 
        return minn + 10
    else:
        return min(maxx, mid)

def getMinRangeValue(x=0):
    minn, maxx = sizeClassMinMax(x)
    return minn

def getMaxRangeValue(x=0):
    minn, maxx = sizeClassMinMax(x)
    return maxx

def getRandomRangeValue(x=0):
    minn, maxx = sizeClassMinMax(x)
    mid = (maxx - minn)*random.random() + minn
    if x==7: 
        return minn + 10
    else:
        return min(maxx, mid)

# Bucket round function
def bucketRound(arr, thr=0.5):
    if isinstance(arr, list) or isinstance(arr, np.ndarray):
        arr = np.asarray(arr).astype('float')
        out = np.zeros(len(arr)).astype('int64')
        bucket = float(0)
        for i in range(len(arr)):
            out[i] = int(arr[i])
            bucket += arr[i]%1
            if bucket > thr:
                out[i] += 1
                bucket = 0
        return out
    else:
        print(arr)
        print("Error: Function requires inputs as an array or list of values.")

indus_lookup = {
  1: "agriculture_mining",
  2: "manufacturing",
  3: "industry_utilities",
  4: "retail",
  5: "wholesale",
  6: "construction",
  7: "transportation",
  8: "info_finance_insurance_realestate_professional",
  9: "education_public_other",
  10: "medical_health",
  11: "leisure_accommondation_food",
  12: "military"
}

# Create short names for industries
indus_abrv = {
    1: "AGM",
    2: "MFG",
    3: "IUT",
    4: "RET",
    5: "WHL",
    6: "CON",
    7: "TRN",
    8: "IFR",
    9: "EPO",
    10: "MHS",
    11: "LAF",
    12: "MIL"
}

# Reverse lookup
abrv_indus = dict(zip(list(indus_abrv.values()), list(indus_abrv.keys())))

# Cross walk between MGRA employment and model employment categories
emp_mgra_to_model = {
    1:["emp_ag_min"],
    2:["emp_mnf"],
    3:["emp_utl"],
    4:["emp_ret"],
    5:["emp_whl"],
    6:["emp_con"],
    7:["emp_trn_wrh"],
    8:["emp_fin_res_mgm","emp_bus_svcs"],
    9:["emp_educ","emp_gov","emp_oth"],
    10:["emp_hlth"],
    11:["emp_ent","emp_accm","emp_food"],
    12:["emp_mil"]
}



