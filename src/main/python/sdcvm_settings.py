# Created on 2012-10-16
#
# @author: Kevin


# ===============================================================================
#  SDCVM properties
# ===============================================================================
cvmModes = ["Light", "Medium", "Heavy"]
cvmTimes = ["OE", "AM", "MD", "PM", "OL"]
# cvmTimes = ["OL"]
cvmSectors = ["GO", "SV", "IN", "RE", "TH", "WH", "FA"]  # sectors to run; actual industries are hardcoded into sdcvm.py
# cvmSectors =  ["WH"] # sectors to run; actual industries are hardcoded into sdcvm.py

opCostScale = 1.0  # Scale factor for operating cost

maxTaz = 4996  # highest taz number; assumes skims are 1-maxTaz without gaps


# Costs defined as [time, distance, money]. Not used right now.
cvmCostDict = {"Light": [-0.313, -0.138 * opCostScale, -1],
               "Medium": [-0.313, -0.492 * opCostScale, -1],
               "Heavy": [-0.302, -0.580 * opCostScale, -1]}

# Dictionary for creating accessibilities; [skim, property, lambda] 
cvmAccDict = {"Acc_LE": ["Light_Mid", "TotEmp", 3.0],
              "Acc_LP": ["Light_Mid", "Pop", 3.0],
              "Acc_ME": ["Medium_Mid", "TotEmp", 2.0],
              "Acc_MP": ["Medium_Mid", "Pop", 2.0],
              "Acc_HE": ["Heavy_Mid", "TotEmp", 1.0],
              "Acc_HP": ["Heavy_Mid", "Pop", 1.0]
              }

# Calibration adjustment scale factors for tour generation
# Factors by land use type:
#     [Low dens, Residential, Retail/Comm, Industrial, Emp Node] 
genCalibDict = {'FA': [0.4931, 1.8562, 2.3996, 1.7171, 2.4541],
                'GO': [11.8418, 4.5588, 4.1018, 5.1797, 14.1177],
                'IN': [0.6712, 0.7201, 0.7671, 0.7934, 0.2481],
                'RE': [0.6764, 1.2354, 1.7748, 1.2966, 0.1446],
                'SV': [4.1374, 2.2546, 2.6429, 3.2391, 4.2139],
                'TH': [0.7609, 0.4813, 0.4271, 1.1211, 0.5308],
                'WH': [1.2189, 0.5536, 0.5035, 0.4371, 0.2212]}
