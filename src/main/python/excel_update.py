# __author__ = 'yma'
# This file is to force to update links in excel, 1/23/2019

import os
import sys
import win32com.client
from win32com.client import Dispatch

# usage = ("Correct Usage: excel_update.py <project_directory> <scenario_year> <scenario_id> ")

# check if too few/many arguments passed raise error
if len(sys.argv) != 4:
    sys.exit(-1)

output_path = str(sys.argv[1])
sceYear = int(sys.argv[2])
scenario = str(sys.argv[3])


path_name_s = output_path + "\\analysis\\summary\\"
file_names_s = ["ModelResultSummary"]

path_name_v = output_path + "\\analysis\\validation\\"
file_names_v = ["HighwayAssignmentValidation_2016_AllClass",
                "HighwayAssignmentValidation_2016_Truck",
                "HighwayAssignmentValidation_2016_Speed_FreewayCorridor_AMPM",
                "HighwayAssignmentValidation_2016_FreewayCorridor_Daily",
                "HighwayAssignmentValidation_2016_FreewayCorridor_AM",
                "HighwayAssignmentValidation_2016_FreewayCorridor_PM",
                "TransitAssignmentValidation_2016_General",
                "TransitAssignmentValidation_2016_Hub",
                ]
ext = ".xlsm"

# file list of sensitivity
list_s = []
list_s_new = []
for file in file_names_s:
    file_s = path_name_s + file + ext
    file_s_new = path_name_s + file + "_" + scenario + ext
    list_s.append(file_s)
    list_s_new.append(file_s_new)

# file list of validation
list_v = []
list_v_new = []
for file in file_names_v:
    file_v = path_name_v + file + ext
    file_v_new = path_name_v + file + "_" + scenario + ext
    list_v.append(file_v)
    list_v_new.append(file_v_new)


file_list = list_s
file_list_new = list_s_new

if sceYear == 2016 or sceYear == 2018:
    file_list = file_list + list_v
    file_list_new = file_list_new + list_v_new


xl = Dispatch("Excel.Application")
xl.Visible = False
xl.DisplayAlerts = False
xl.AskToUpdateLinks = False

for file in file_list:
    wb = xl.workbooks.open(file)
    xl.Application.Run("UpdateLinks")
    wb.Close(True)

xl.Quit()

for i in range(len(file_list)):
    os.rename(file_list[i], file_list_new[i])
