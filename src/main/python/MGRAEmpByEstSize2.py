# -*- coding: utf-8 -*-
"""
Create MGRA Employment by Establishment Size Group
Created on Sun Sep 17 21:54:49 2023
@author: jgliebe
Requires MGRAEmpByEstSizeFunctions.py

"""
import time
import numpy as np
import pandas as pd
import os
import sys
import random

# Read in functions for this program
from MGRAEmpByEstSizeFunctions import *
from ortools.linear_solver import pywraplp

arguments = sys.argv

root_dir = arguments[1]
output_dir = arguments[2]
luz_data_file = arguments[3]

# Import file paths from script
# from FilePaths import *
print("root_dir = \n", root_dir, "\n")
print("output_dir = \n", output_dir, "\n")

# Get current datetime
from datetime import datetime
current_dateTime = datetime.now()
date = current_dateTime.date()

# Set random seed
random.seed(17)
tstart = time.time()

# Set the maximum time to allow for optimized solutions in seconds
timeout = 150
print(f"\nSolver time limit set to: {timeout} seconds")

# Set a stopping gap limit for MIP solver
gap = 0.049
print(f"Solver stopping gap limit set to: {gap}\n")

outdata_fn1 = (f"MGRAEmpByEstSize.csv")
outdata_fn2 = (f"SynthEstablishments.csv")
outdata_fn3 = (f"SummarySynthEstabs.csv")
outrept_fn = (f"MGRAEmpByEstSize_Log.txt")

# Open Log file for writing
report = open(os.path.join(output_dir, outrept_fn), "wt")

# Read input data -- establishment file
df_LUZ = pd.read_excel(luz_data_file, sheet_name=r"Data", skiprows=4)

# Make sure columns are all lower case
luz_ren_cols = dict(zip(df_LUZ.columns, df_LUZ.columns.str.lower()))
df_LUZ = df_LUZ.rename(columns=luz_ren_cols)

df_LUZ.set_index('luz', inplace=True)

# Get employment sectors from column names
cols = df_LUZ.columns
emp_sectors = set([col[:-1] for col in cols])

# Read input data -- establishment file
#df_MGRA = pd.read_csv(os.path.join(root_dir, raw_data_dir, 
#                                      r"Land_Use\mgra15_based_input_2022_02_cvm.csv"))

#df_MGRA = pd.read_csv(f"{root_dir}\\input\\mgra15_based_input2022.csv")
df_MGRA = pd.read_csv(f"{root_dir}\\input\\land_use_taz.csv")
df_MGRA = df_MGRA.rename(columns={'luz_id': 'LUZ', 'emp_total': 'emp_tot'})
df_MGRA = df_MGRA.loc[:, df_MGRA.columns != 'TAZ']

# Make sure columns are all lower case
mgra_ren_cols = dict(zip(df_MGRA.columns, df_MGRA.columns.str.lower()))
df_MGRA = df_MGRA.rename(columns=mgra_ren_cols)

emp_cols = [col for col in df_MGRA.columns if col in emp_sectors]
df_empMGRA = df_MGRA.groupby(['luz','mgra','taz',])[emp_cols].sum().reset_index()

# Summaries
print(f"Create Synthetic Establishments")
print(f"Create Synthetic Establishments", file=report)

print(f"Use MGRA Employment by Establishment Size Group {date}\n")
print(f"Use MGRA Employment by Establishment Size Group {date}\n", file=report)

print(f"There are: \n\t{len(df_LUZ):>6,.0f} LUZs")
print(f"There are: \n\t{len(df_LUZ):>6,.0f} LUZs", file=report)

print(f"\t{len(df_empMGRA):>6,.0f} MGRAs")
print(f"\t{len(df_empMGRA):>6,.0f} MGRAs", file=report)

print(f"\t{len(emp_cols)-1:>6,.0f} industry sectors")
print(f"\t{len(emp_cols)-1:>6,.0f} industry sectors\n", file=report)

print(f"Total employment in all sectors is {df_empMGRA['emp_tot'].sum():,.0f}")
print(f"Total employment in all sectors is {df_empMGRA['emp_tot'].sum():,.0f}", \
                                            file=report)

# =============================================================================
# Loop over employment sectors and land use zones (LUZs)
# =============================================================================
sectors_list = [x[4:] for x in emp_cols]
sectors_list.remove('tot')

# Initialize tables for outputs
df_allOut = df_empMGRA[['mgra','taz','luz']].copy()
synthEstabs = pd.DataFrame(columns=["Industry_Name","LUZ","MGRA","Employees","Size_Class"])

for sector in sectors_list:
    print(f"\nProcessing {str.upper(sector)} sector")
    print(f"\n{str.upper(sector)} sector\n", file=report)
    use_cols_luz = [col for col in df_LUZ.columns if sector in col]
    df_sectorOut = pd.DataFrame()
    discreps = 0
    noOptimal = 0
    noDistn = 0
    df_compDistn_all = pd.DataFrame()
    
    for luzn in list(df_LUZ.index):     
        print(f"\rLUZ {luzn}", end='')
        print(f"LUZ {luzn}", file=report)
        
        try:
            luz_emp = df_LUZ.loc[luzn][use_cols_luz]
        except IndexError:
            print(IndexError, file=report)
            print(luzn, file=report)
        
        # Get LUZ percent of establishments by size group
        luz_emp = df_LUZ.loc[luzn][use_cols_luz]
        
        # Use lowest common denominator to get plausible distribution
        inv = 1/luz_emp
        inv[np.isinf(inv)] = 0
        nestab = round(luz_emp*max(inv)).astype('int32')

        # Get MGRA employment for this LUZ and sector
        use_cols_mgra = [col for col in df_empMGRA.columns if sector in col]
        mgra_emp = df_empMGRA.loc[df_empMGRA['luz']==luzn][['mgra']+use_cols_mgra].set_index('mgra')
        luz_tot_emp = mgra_emp.values.sum()
        #print(f"The sum of MGRA {sector} employment in LUZ {luzn} is: {luz_tot_emp}", file=report)
        
        # Skip LUZ if zero employment in this sector
        altMethod = False        
        if (luz_tot_emp == 0):
            print(f"Skipping LUZ {luzn}.", file=report)
            print(f"LUZ {luzn} has {nestab.sum()} establishments distributed in sector {sector}.", file=report)
            print(f"The total {sector} employment of all MGRAs in LUZ {luzn} is {luz_tot_emp}.\n", file=report)
            continue
        elif (nestab.sum() < 1) and (luz_tot_emp > 0):
            altMethod = True
            print(f"WARNING: Alt Method used to allocate size class of total employment in each MGRA for LUZ {luzn}.", file=report)
            print(f"LUZ {luzn} has {nestab.sum()} establishments distributed in sector {sector}.", file=report)
            print(f"The total {sector} employment of all MGRAs in LUZ {luzn} is {luz_tot_emp}.\n", file=report)
            alt_weights = [float(w[0]) for w in mgra_emp[use_cols_mgra].values if w>0]
        else:
            pass

        # Get largest size class label and class index number        
        try:
            maxNestab = max(nestab[nestab>0].index)
            maxSClass = int(maxNestab[-1:])
        except:
            maxSClass = 0
        
        if altMethod == False:
            
            # Find nonzero MGRAs
            nonzero_MGRA_emp = [int(m) for m in mgra_emp.values if m>0]
            nonzero_MGRA_classes = [sizeClass(n) for n in nonzero_MGRA_emp]
    
            # Find the number of non-zero MGRAs if size class was total employment
            nzDict = dict(zip(range(1,8,1),np.zeros(7)))
            for nz in nonzero_MGRA_classes:
                nzDict[nz] += 1
            nzMGRAs = pd.Series(np.array(list(nzDict.values())), index = nestab.index)
            klass7 = nzMGRAs[-1]
            
            # If sum of MGRA employment is greater than the minimum total employment in establishment 
            # plus one additional multiple of establishment, then add another multiple of establishments
            check_est_emp = np.asarray([getMinRangeValue(i+1) for i in range(len(nestab))]) * nestab
            
            addestab = nestab.copy()
            add_est_emp = np.asarray([getMinRangeValue(i+1) for i in range(len(addestab))]) * addestab
            
            # Two cases
            if klass7 == 0:
                while luz_tot_emp > (check_est_emp.sum() + add_est_emp.sum()):
                    nestab += addestab
                    check_est_emp = np.asarray([getMinRangeValue(i+1) for i in range(len(nestab))]) * nestab
            elif klass7 > 0:
                count = 1
                while (luz_tot_emp > (check_est_emp.sum() + add_est_emp.sum())) & (klass7 > count):
                    nestab += addestab
                    check_est_emp = np.asarray([getMinRangeValue(i+1) for i in range(len(nestab))]) * nestab
                    count += 1
            else:
                pass
        
            if (sum(check_est_emp) > luz_tot_emp):
                print(f"WARNING: Implied LUZ {luzn} distribution cannot be satisfied by available {sector} employment in MGRAs.", file=report)
                temp = nzMGRAs.copy() 
                nestab = temp.astype('int32')
    
            # Establishments needed to fulfill distribution imnplied by LUZs
            estNeed = nestab - nzMGRAs
    
            # Identify feasible assignments by size class
            mgraSeed = mgra_emp.copy()
            mgraSeed.loc[:, 'max_class'] = mgraSeed[use_cols_mgra[0]].map(sizeClass)
            
            for col in use_cols_luz:
                sclass = int(col[-1])
                mgraSeed.loc[:, col] = (mgraSeed['max_class'] >= sclass)*1
            mgra_max_class = mgraSeed['max_class']
            mgraSeed = mgraSeed[use_cols_luz]
            
            # Identify classes that are constrained by the MGRA totals
            feasible_MGRAs = mgraSeed.sum(axis=0)
            constrained_MGRAs = feasible_MGRAs.copy()
            
            # Identify maximum number of MGRAs supporting each class
            y = [0] + [(feasible_MGRAs[i]-feasible_MGRAs[i+1]) for i in range(len(feasible_MGRAs)-2,-1,-1)]
            y.reverse()
            
            assert len(y) == len(constrained_MGRAs)
            for i in range(len(y)):
                constrained_MGRAs[i] = y[i]
                
            # Account for largest size class 7
            field7 = 'emp_' + sector + '7'
            if feasible_MGRAs.loc[field7] == 1:
                constrained_MGRAs.loc[field7] = 1
            constrained_MGRAs = constrained_MGRAs * (nestab > 0)
                        
            # Identify constrained classes that can only be placed in certain MGRAs
            constrClasses = (((nestab==constrained_MGRAs) & (nestab > 0))*1).reset_index(name="ones")
            constrClasses = list(constrClasses[constrClasses.ones==1].index + 1)
            constrClasses = [z for z in constrClasses if z <= maxSClass]
            try:
                # Do not include Class 1, which is handled separately
                constrClasses.remove(1)  
            except:
                pass
        
# =============================================================================
#       Create a pool of synthetic establishments for optimization
# =============================================================================

            # Create establishments for Size Group 1 as limited by MGRA totals
            estab_Size1 = (mgraSeed.sum(axis=1)==1) * np.ravel(mgra_emp.values)
            estab_Size1 = [float(x) for x in estab_Size1 if x>0]
            
            # Create establishments for classes with smallest representation as limited by MGRA totals
            estab_SizeS = []
            for s in constrClasses:
                temp = list((mgraSeed.sum(axis=1)==s) * np.ravel(mgra_emp.values))
                #temp = [float(getMinRangeValue(sizeClass(x))) for x in temp if x>0]
                estab_SizeS += temp
            estab_SizeS = np.ravel(np.asarray(estab_SizeS))
            estab_SizeS = estab_SizeS[estab_SizeS>0]
                   
            # Adjust total establishments in first and largest size classes
            adjNestab = nestab.copy()
            adjNestab[0] -= len(estab_Size1)
            for s in constrClasses:
                adjNestab[s-1] = estNeed[s-1]
    
            # Remove totals if MGRA employment does not support a larger establishment size
            largestClass = max(mgraSeed.sum(axis=1))
            for j in range(len(adjNestab)-largestClass):
                remClass = largestClass + j
                adjNestab[remClass] = 0
            
            # Adjust total employment in first and largest size classes
            mgra_adj_total = mgra_emp.values.sum() - sum(estab_Size1)
    
            # For unconstrained classes, use minimum of range to assign initial values
            margEstab = np.asarray([getMinRangeValue(i+1) for i in range(len(adjNestab))]) * adjNestab
                    
            # Check size class consistency
            check = (margEstab/adjNestab).fillna(0).map(sizeClass)       
            ok = len(check)
            while (ok > 0):
                ok = 0
                for x in range(len(check)):
                    if check[x] > 0:
                        if check[x] > (x+1):
                            adjNestab[x] += 1
                            ok += 1
                        elif check[x] < (x+1):
                            adjNestab[x] -= 1
                            ok += 1
                        else:
                            pass
                check = (margEstab/adjNestab).fillna(0).map(sizeClass)
            
            # Check size class consistency
            check = (margEstab/adjNestab).fillna(0).map(sizeClass)
            test = np.asarray([int(t[-1:]) for t in check.index.values]) * (check.values>0)
            if sum(check.values == test) != len(check):
                print("WARNING: Implied establishment size classes are not consistent with employment weights.", file=report)
                print(check.to_string(), file=report)
            else:
                pass
            report.flush()
            
            # Effective mean establishment size by employment class
            estabWeights = np.floor(margEstab / adjNestab).fillna(0)

# =============================================================================
#       Use multiple knapsack method to optimize allocation
# =============================================================================
            numEstabByGrp = adjNestab.values
            estabGrpWghts = estabWeights.values
            
            # Create pool of synthetic establishments to be placed
            pdf = feasible_MGRAs.values 
            nemp = range(1,len(pdf)+1,1)
            
            weights = []
            count = 0
            for n in numEstabByGrp:
                for p in range(n):
                    weights.append(estabGrpWghts[count])
                count += 1
            weights += estab_Size1
            weights += list(estab_SizeS)
            weights = list(map(float, weights))
            
            if (adjNestab.sum() < 1) and (luz_tot_emp > 0):
                altMethod = True
                print(f"WARNING: Alt Method used to allocate size class of total employment in each MGRA for LUZ {luzn}.", file=report)
                print(f"LUZ {luzn} has {nestab.sum()} establishments distributed in sector {sector}.", file=report)
                print(f"The total {sector} employment of all MGRAs in LUZ {luzn} is {luz_tot_emp}.\n", file=report)
                alt_weights = [float(w[0]) for w in mgra_emp[use_cols_mgra].values if w>0]
            
        if altMethod: 
            weights = alt_weights.copy()
            noDistn += 1
        else:
            pass
        
        # Random reordering of pool
        random.shuffle(weights)
        values = weights.copy()

        # Calculate the size class membership in weights
        weightsClasses = pd.DataFrame([sizeClass(w) for w in weights], columns=['sclass'])
        weightsClasses = weightsClasses.groupby('sclass').size().to_frame('estwghts')
        
        # Create optimization problem variables
        data = {}
        
        data['weights'] = weights
        data['values'] = values
        
        assert len(data['weights']) == len(data['values'])
        data['num_items'] = len(data['weights'])
        data['all_items'] = range(data['num_items'])
        
        # Set MGRA targets
        data['bin_capacities'] = mgra_emp.values.ravel()
        
        data['num_bins'] = len(data['bin_capacities'])
        data['all_bins'] = range(data['num_bins'])
        
        # The following code declares the MIP solver.
        solver = pywraplp.Solver.CreateSolver('SCIP') # CP_SAT, CBC
        # Set time limit in milliseconds
        solver.set_time_limit(timeout*1000)
        if solver is None:
            print('SCIP solver unavailable!', file=report)
            break
            
        # Set a stopping gap limit for MIP
        solverParams = pywraplp.MPSolverParameters()
        solverParams.SetDoubleParam(solverParams.RELATIVE_MIP_GAP, gap)   
        
        # The following code creates the variables for the problem.
        # Each x[(i, j)] is a 0-1 variable, where i is an item and j is a bin. 
        # In the solution, x[(i, j)] will be 1 if item i is placed in bin j, and 0 otherwise.
        # x[i, b] = 1 if item i is packed in bin b.
        x = {}
        for i in data['all_items']:
            for b in data['all_bins']:
                x[i, b] = solver.BoolVar(f'x_{i}_{b}')
            
        # The following code defines the constraints for the problem.
        # Each item is assigned to at most one bin.
        [solver.Add(sum(x[i, b] for b in data['all_bins']) <= 1) \
         for i in data['all_items']];
        
        # The amount packed in each bin cannot exceed its capacity.
        [solver.Add(sum(x[i, b] * data['weights'][i] for i in data['all_items']) <= data['bin_capacities'][b]) \
         for b in data['all_bins']];
         
        # Define the objective function
        # Note that x[i, j] * data['values'][i] adds the value of item i to the objective if the item is placed in bin j. 
        # If i is not placed in any bin, its value doesn't contribute to the objective.
        
        # Maximize total value of packed items.
        # 'all_items' = range assigning unique ID to each establishment in the pool (separate fields for weights and values)
        # 'all_bins' = range assigning unique ID to each MGRA (capacities)
        # x[i,b] are solution variables representing every possible combination of item and bin (establishment, zone)
        # x[i,b].solution_value() == 1 if part of the solution and 0 otherwise (establishment can only belong to one MGRA)
        
        objective = solver.Objective()
        for i in data['all_items']:
            for b in data['all_bins']:
                objective.SetCoefficient(x[i, b], data['values'][i]) 
        objective.SetMaximization()
        
        # Invoke the solver
        status = solver.Solve()
        if status == pywraplp.Solver.OPTIMAL:
            print("Optimal solution found.", file=report)
        else:
            print('The problem timed out before finding an optimal solution.\nAllocation may still be used.', file=report)
            noOptimal += 1
        
        # Collect and report results
        #print(f"Total packed value: {objective.Value()}\n")
        total_weight = 0
        bin_num_est = []
        bin_tot_emp = []
        bin_sclass_emp = []
        bin_sclass_est = []
        for b in data['all_bins']:
            #print(f'Bin {b}')
            bin_weight = 0
            bin_value = 0
            bin_count = 0
            bin_szemp = np.zeros(len(nestab))
            bin_szest = np.zeros(len(nestab))
            for i in data['all_items']:
                if x[i, b].solution_value() > 0:
                    sclass = sizeClass(data['weights'][i])
                    #print(f"Item {i} weight: {data['weights'][i]}, value: {data['values'][i]}, sizeclass: {sclass}")
                    bin_szemp[sclass-1] += data['weights'][i]
                    bin_szest[sclass-1] += 1
                    bin_weight += data['weights'][i]
                    bin_value += data['values'][i]
                    bin_count += 1
            #print(f'Packed bin weight: {bin_weight}')
            #print(f'Packed bin value: {bin_value}\n')
            total_weight += bin_weight
            bin_num_est.append(bin_count)
            bin_tot_emp.append(bin_value)
            bin_sclass_emp.append(bucketRound(bin_szemp, 0.50))
            bin_sclass_est.append(bin_szest)
        #print(f'Total packed weight: {total_weight}')
        
        # Get the employment summaries
        y = np.reshape(bin_sclass_emp, (len(bin_tot_emp), len(nestab)))
        outSums = y.sum(axis = 1)
        
        # Number of establishments
        z = np.reshape(bin_sclass_est, (len(bin_tot_emp), len(nestab)))
        outEstClass = z.sum(axis=0)
        outEstClass = outEstClass / sum(outEstClass)
        cols = [(f"est_{sector}{k+1}") for k in range(len(outEstClass))]
        df_estLUZOut = pd.DataFrame(z, columns=cols, index=mgra_emp.index).astype('int32')

        # Who did not get assigned
        items = {}
        notassigned = 0
        for i in data['all_items']:
            results = 0
            for b in data['all_bins']:
                if x[i, b].solution_value():
                    results += 1
            items[i] = results
            if items[i] == 0:
                notassigned += 1
                #print(f"Not assigned: Item {i} with weight {data['weights'][i]}")
        if notassigned == 0:
            print(f"Number of establishments not assigned using optimization method = {notassigned}", file=report)
        elif notassigned > 0:
            print(f"WARNING: Number of establishments not assigned using optimization method = {notassigned}", file=report) 
            print(f"The likely cause is an inconsistency between the LUZ distribution and MGRA capacities.", file=report)
        else:
            pass

        # MGRA Allocated
        df_luzOut = pd.DataFrame(y, columns=use_cols_luz, index=mgra_emp.index)
        
# =============================================================================
#        Cleanup
# ============================================================================
        # Find differences
        testDiff = (mgra_emp.values.T - outSums)[0]
        
        # Get maximum total employment by class
        mgra_est_emp_cap = [getMaxRangeValue(j+1)*df_estLUZOut.values[i][j] \
                            for i in range(len(df_estLUZOut)) \
                            for j in range(len(nestab))]
        mgra_est_emp_cap = np.asarray(mgra_est_emp_cap).reshape(len(df_estLUZOut),len(nestab))
        
        # Find remaining capacity in each MGRA by size column/class
        df_remCap = mgra_est_emp_cap - df_luzOut.copy()
        
        # Allocate differences in proportion to available capacity 
        df_propClass = df_luzOut.copy()
        for col in df_luzOut.columns:
            df_propClass[col] = testDiff * df_remCap[col] / df_remCap.sum(axis = 1)
        df_propClass.fillna(0, inplace=True)
        
        # Bucket Round
        indx = df_propClass.index
        for i in indx:
            df_propClass.loc[i] = bucketRound(np.asarray(df_propClass.loc[i]), 0.5)
        
        # Add additional
        df_luzOut = df_luzOut + df_propClass.astype('int32')
        
        # If initial assignment is zero, add one establishment to size class indicated by missing employment
        missing = (outSums == 0) * testDiff
        klass = np.array(list(map(sizeClass, missing)))
        
        # Update both employment and number of establishments
        prefix_emp = df_luzOut.columns[0][:-1]
        prefix_est = df_estLUZOut.columns[0][:-1]
        for i in range(len(df_luzOut)):
            if klass[i] > 0:
                df_luzOut.loc[indx[i], prefix_emp +str(klass[i])] += missing[i]
                df_estLUZOut.loc[indx[i], prefix_est +str(klass[i])] += 1
        
        # Find small differences due to rounding
        testDiff = (mgra_emp.values.T[0] - df_luzOut.sum(axis=1).values)
        
        # Find the largest used size class field for each zone to add differences
        sclasses = np.asarray([col[-1] for col in df_luzOut.columns]).astype('int32')
        df_maxClass = (df_luzOut > 0) * sclasses
        prefix = df_luzOut.columns[0][:-1]
        use_max_cols = list(df_maxClass.max(axis=1).values)
        use_max_cols = [max(x,1) for x in use_max_cols]
        
        indx = df_luzOut.index
        for i in range(len(df_luzOut)):
            df_luzOut.loc[indx[i], prefix + str(use_max_cols[i])] += testDiff[i]
        
        # Check to make sure total MGRA employment has been fulfilled
        df_checkEmp = pd.DataFrame(zip(mgra_emp.values.ravel(), df_luzOut.sum(axis=1)), columns = ['Target', 'Allocated'])
        df_checkEmp.loc[:,'TestDiff'] = abs(df_checkEmp['Target'] - df_checkEmp['Allocated'])
        print(f"LUZ {luzn}: total employment of MGRA inputs: {df_checkEmp['Target'].sum()}", file=report)
        print(f"LUZ {luzn}: total employment allocated: {df_checkEmp['Allocated'].sum()}", file=report)
        print(f"LUZ {luzn}: individual MGRAs with an employment discrepancy: {df_checkEmp['TestDiff'].sum()}\n", file=report)
        discreps += (df_checkEmp['TestDiff'].sum()>0)*1
        
        # Concatenate dataframes by LUZ within same sector
        df_sectorOut = pd.concat([df_sectorOut, df_luzOut], ignore_index=False)
        report.flush()
        
        # Reset bin values for the next LUZ-Industry
        total_weight = 0
        bin_num_est = []
        bin_tot_emp = []
        bin_sclass_emp = []
        bin_sclass_est = []
# =============================================================================
#       Create Table of Synthetic Establishments
# =============================================================================
        # loop over rows in output file, find non-zeros, and create establishment records
        for ndx, row in df_luzOut.iterrows():
            estb = df_estLUZOut.loc[ndx]
            for col1 in df_luzOut.columns:
                col2 = col1.replace('emp','est')
                industry = col1[:-1]
                mgra = ndx
                empl = row[col1]
                if (empl>0) & (estb[col2]>0):
                    synEstab = [int(empl/estb[col2]) for j in range(estb[col2])]
                    addEmp = empl - sum(synEstab)
                    for r in range(addEmp):
                        k = random.randint(0,len(synEstab)-1)
                        synEstab[k] += 1
                    for emps in synEstab:
                        new_rec = pd.Series([industry, luzn, mgra, emps, sizeClass(emps)], index=synthEstabs.columns)
                        synthEstabs = pd.concat([synthEstabs, pd.DataFrame(new_rec).T], ignore_index=True)
                elif (empl>0) & (estb[col2]==0):
                    new_rec = pd.Series([industry, luzn, mgra, empl, sizeClass(empl)], index=synthEstabs.columns)
                    synthEstabs = pd.concat([synthEstabs, pd.DataFrame(new_rec).T], ignore_index=True)
                    
        # Get final modeled size distribution
        totEstab = len(synthEstabs[(synthEstabs.LUZ==luzn) & (synthEstabs.Industry_Name==industry)])
        outEstClass = np.zeros(len(outEstClass))
        temp = synthEstabs[(synthEstabs.LUZ==luzn) & (synthEstabs.Industry_Name==industry)].groupby(['Size_Class']).size() / totEstab
        for t in temp.index:            
            outEstClass[t-1] = temp[t]
        
        # Calculate coincidence ratio between observed and modeled distributions of establishments by size
        df_compDistn = pd.DataFrame(zip(outEstClass, luz_emp.values), columns=['model','target'])
        df_compDistn['minn'] = df_compDistn[['model', 'target']].min(axis=1)
        df_compDistn['maxx'] = df_compDistn[['model', 'target']].max(axis=1)
        coincidence = round(df_compDistn['minn'].sum() / df_compDistn['maxx'].sum(), 3)
        
        print(f"Total establishments: {totEstab:.0f}", file=report)
        print(f"LUZ {luzn} fit to '{sector}' target distribution by employment size group.", file=report)
        print(f"Coincidence Ratio: {coincidence:.3f}\n", file=report)
        
        df_compDistn['EstabSizeClass'] = pd.Series(range(1,8,1))
        df_compDistn.set_index('EstabSizeClass', inplace=True)
        if not(df_compDistn_all.empty):
            df_compDistn_all += df_compDistn * totEstab
        else:
            df_compDistn_all = df_compDistn * totEstab
        
        df_compDistn['model'] = df_compDistn['model'].map('{:.4f}'.format)
        df_compDistn['target'] = df_compDistn['target'].map('{:.4f}'.format)
        print(df_compDistn[['model', 'target']].to_string(), file=report)
        print("\n", file=report)
                
# =============================================================================
#   Merge completed sector dataframes
# =============================================================================
    print("\rComplete")
    print(f"\n", file=report)
    print(f"*"*50, file=report)
    print(f"\r{str.upper(sector)} MGRAs with employment discrepancies = {discreps}")
    print(f"{str.upper(sector)} MGRAs with employment discrepancies = {discreps}\n", file=report)
    print(f"\r{str.upper(sector)} LUZs without an input distribution but non-zero MGRA employment = {noDistn}")
    print(f"{str.upper(sector)} LUZs without an input distribution but non-zero MGRA employment = {noDistn}\n", file=report)
    print(f"\r{str.upper(sector)} LUZs timing out before an optimal solution found = {noOptimal}")
    print(f"{str.upper(sector)} LUZs timing out before an optimal solution found = {noOptimal}\n", file=report)
    
    df_compDistn_all_pct = df_compDistn_all / df_compDistn_all.sum()
    df_compDistn_all_pct['minn'] = df_compDistn_all_pct[['model', 'target']].min(axis=1)
    df_compDistn_all_pct['maxx'] = df_compDistn_all_pct[['model', 'target']].max(axis=1)
    coincidence_all = round(df_compDistn_all_pct['minn'].sum() / df_compDistn_all_pct['maxx'].sum(), 3)
    
    df_compDistn_all['model'] = df_compDistn_all['model'].map('{:,.0f}'.format)
    df_compDistn_all['target'] = df_compDistn_all['target'].map('{:,.0f}'.format)
    df_compDistn_all_pct['model'] = df_compDistn_all_pct['model'].map('{:.4f}'.format)
    df_compDistn_all_pct['target'] = df_compDistn_all_pct['target'].map('{:.4f}'.format)
    
    print(f"Total Distributions for Sector {sector}")
    print(df_compDistn_all[['model', 'target']].to_string(), file=report)
    print(f"\nCoincidence Ratio: {coincidence_all:.3f}\n", file=report)
    print(df_compDistn_all_pct[['model', 'target']].to_string(), file=report)
    
    df_allOut = df_allOut.merge(df_sectorOut.reset_index(), \
                                how='left', on=['mgra'])
    df_allOut.fillna(0, inplace=True)
    df_allOut.sort_values(['mgra'], inplace=True)
    report.flush()

# =============================================================================
# Create final synthetic establishment table
# =============================================================================
for indNum, modSect in indus_abrv.items():
    mgraFields = emp_mgra_to_model[indNum]
    for mf in mgraFields:
        synthEstabs.loc[synthEstabs['Industry_Name'] == mf,'Industry_Name'] = indus_abrv[indNum]
synthEstabs.insert(0, 'Industry_No', synthEstabs['Industry_Name'].map(abrv_indus))
synthEstabs.insert(0, 'Establishment_ID', synthEstabs.index + 1)

summarySynthEstabs = synthEstabs.groupby(['Industry_No','Industry_Name','Size_Class']).size().to_frame('count').reset_index()
summarySynthEstabs['Industry_Name'] = summarySynthEstabs['Industry_No'].map(indus_lookup)
# =============================================================================
# Transform MGRA land use file categories to Model Employment Categories
# (use for diagnostic purposes)
# =============================================================================
#df_modOut = df_allOut[["mgra","taz","LUZ"]].copy()
#
#for indNum, modSect in indus_abrv.items():
#    mgraFields = emp_mgra_to_model[indNum]
#    for estbSize in range(1,8,1):
#        fieldname = "emp_" + modSect + str(estbSize)
#        df_modOut[fieldname] = 0
#        for mf in mgraFields:
#            df_modOut[fieldname] += df_allOut[mf+str(estbSize)]
#df_modOut = df_modOut.astype('int64')

# =============================================================================
# Write results to file
# =============================================================================
out_fpath1 = os.path.join(output_dir, outdata_fn1)
df_allOut.to_csv(out_fpath1, index=False)

out_fpath2 = os.path.join(output_dir, outdata_fn2)
synthEstabs.to_csv(out_fpath2, index=False)

out_fpath3 = os.path.join(output_dir, outdata_fn3)
summarySynthEstabs.to_csv(out_fpath3, index=False)

print(f"\nAll Sector Processing complete.")
print(f"Output files written to:\n {output_dir}\n")
print(f"Processing time: {(time.time()-tstart)/60.0:,.2f} minutes\n")

print(f"\nProcessing complete.", file=report)
print(f"Output file written to:\n {output_dir}\n", file=report)
print(f"Processing time: {(time.time()-tstart)/60.0:,.2f} minutes\n", file=report)

report.flush()
report.close()
# =============================================================================
# End
# =============================================================================