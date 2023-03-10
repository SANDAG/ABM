# This python script reads an ActivitySim model utility specification, an ActivitySim coefficient file, and optionally, a template file
# It then replaces the coefficient names in the utility specification with the actual coefficient values from the coefficient file
# and writes out the result. If a template file is specified a series of output files will be written, one for each purpose or market.
#
# Usage: python gen_coeff_file.py utility_file_name coefficient_file_name False template_file_name
# 
# Note: do not specify file name extension ".csv" on the command line

import pandas as pd
import numpy as np
import sys
 
# total arguments
n = len(sys.argv)
print("Total arguments passed:", n)
 
# Arguments passed
print("\nArguments passed:", end = " ")
for i in range(1, n):
    print(sys.argv[i], end = " ")
     
if len(sys.argv) < 4:
  print("Error: Must specify name of utility file and name of coefficients file at a minimum (no .csv extension)")
  exit(1)
template_file=True
if len(sys.argv) < 5:
	print("Warning: No template file specified, will look for coefficients in coefficients file")
	template_file=False
	
if sys.argv[3] == 'True':
  multiple_files=True
elif sys.argv[3] == 'False':
  multiple_files=False
else:
  printf("Error: Multiple coefficient files parameter must be either True or False")
  exit(1)
	
specification_file = sys.argv[1]
coefficients_file = sys.argv[2]

spec_df = pd.read_csv(specification_file+'.csv')

if multiple_files==False:
  coef_df = pd.read_csv(coefficients_file+'.csv')
  coef_df.set_index('coefficient_name', inplace=True)


if template_file:
  template_file = sys.argv[4]
  temp_df = pd.read_csv(template_file+'.csv')
  temp_df.set_index('coefficient_name', inplace=True)
  # create purpose list by skipping 'coefficient_name' in template header record
  temp_headers = temp_df.keys()
  purposes = temp_headers[0:]


# create alternatives list by skipping 'Label','Description','Expression' in specification header record
spec_headers = spec_df.keys()
alt_names = spec_headers[3:]


if template_file:
  for purpose in purposes:
    spec_purp_df = spec_df.copy(deep='True')
    for i in range(len(spec_purp_df.index)):
      if isinstance(spec_df.loc[i,'Label'], str)==True:
        if '#' in spec_df.loc[i,'Label']:
          continue                                                                                           
      #print(i)
      for alt in alt_names:
        # 1. getting generic name from spec file
        #print(alt)
        cell = spec_df.at[i,alt]
        #print(cell)
        if cell in (None,np.nan,'','0',0):
          continue
        cellIsString = False                                                                                     
        try:
          x = float(cell)
        except ValueError:
          cellIsString = True
        if not cellIsString:
          continue                                                                    
        cell = cell.lstrip(' ')
        # 2. getting purpose-specific name from coefficient template file
        #print('looking for row ',cell,' in col ', purpose)
        template_variable = temp_df.loc[cell,purpose]
        # 3. getting coefficient value from coefficient file
        #print(template_variable)
        coefficient = coef_df.loc[template_variable,'value']
        # 4. replace coefficient name with coefficient value in output dataframe for the purpose
        #print('coefficient', coefficient)
        spec_purp_df.replace(cell, coefficient, inplace=True)
    spec_purp_df.to_csv(specification_file+purpose+'.csv', sep=',')
elif multiple_files==False:
  spec_copy_df = spec_df.copy(deep='True')
  for i in range(len(spec_copy_df.index)):
    # print(i)                                                                                            
    if isinstance(spec_df.loc[i,'Label'], str)==True:
      if '#' in spec_df.loc[i,'Label']:
        continue                                                                                           
    for alt in alt_names:                                                                                
      # 1. getting generic name from spec file                                                           
      #print(alt)                                                                                        
      cell = spec_df.at[i,alt]                                                                           
      #print(cell)                                                                                       
      if cell in (None,np.nan,'','0',0,'-','-999'):                                                                   
        continue
      cellIsString = False                                                                                     
      try:
        x = float(cell)
      except ValueError:
        cellIsString = True
      if not cellIsString:
        continue                                                                    
      # 2. getting coefficient value from coefficient file                                               
      #print(template_variable)                                                                          
      coefficient = coef_df.loc[cell,'value']                                               
      # 3. replace coefficient name with coefficient value in output dataframe for the purpose           
      #print('coefficient', coefficient)                                                                 
      spec_copy_df.replace(cell, coefficient, inplace=True)                                              
  spec_copy_df.to_csv(specification_file+'with_coeffs.csv', sep=',')        
else:
  for alt in alt_names:          
    spec_copy_df = spec_df.copy(deep='True')
    for i in range(len(spec_copy_df.index)):
      # print(i)                                                                                            
      if isinstance(spec_df.loc[i,'Label'], str)==True:
        if '#' in spec_df.loc[i,'Label']:
          continue                                                                                           
      # 0. since we have multiple coefficient files need to open the right one
      coef_df = pd.read_csv(coefficients_file+'_'+alt+'.csv')
      coef_df.set_index('coefficient_name', inplace=True)
      # 1. getting generic name from spec file                                                           
      #print(alt)                                                                                        
      cell = spec_df.at[i,alt]                                                                           
      #print(cell)                                                                                       
      if cell in (None,np.nan,'','0',0,'-','-999'):                                                                   
        continue
      cellIsString = False                                                                                     
      try:
        x = float(cell)
      except ValueError:
        cellIsString = True
      if not cellIsString:
        continue                                                                    
      # 2. getting coefficient value from coefficient file                                               
      #print(template_variable)                                                                          
      coefficient = coef_df.loc[cell,'value']                                               
      # 3. replace coefficient name with coefficient value in output dataframe for the purpose           
      #print('coefficient', coefficient)                                                                 
      spec_copy_df.replace(cell, coefficient, inplace=True)      
    for col in alt_names:
      if col!=alt:
        spec_copy_df.drop(columns=[col], inplace=True)                                        
    spec_copy_df.to_csv(specification_file+'_'+alt+'with_coeffs.csv', sep=',')        
                                 