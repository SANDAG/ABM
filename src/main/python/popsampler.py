#Synthetic Population Spatial Sampler Routine
#Ben Stabler, ben.stabler@rsginc.com, 08/29/16
#Modified by Justin Culp, justin.culp@rsginc.com 09/12/17
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
#
#
# This file samples households and persons from an input synthetic population. The sampling is based on 
# a file of sample rates by TAZ. Input and output files are specified via command line arguments
# 
# Input files:
#    
#   Argument 1:    Sample rate file.    'landuse/sampleRateByTaz.csv'
#   Argument 2:    Household file.      'landuse/households.csv'
#   Argument 3:    Person file.         'landuse/persons.csv'
#
# Output files
#   Argument 4:    Household file.      'landuse/households_sample.csv'
#   Argument 5:    Person file.         'landuse/persons.csv'
#

import os, sys
import pandas as pd
import numpy as np


# Define working functions
def sample_hhs(group):
    #sample using the taz sample rate with replacement and a stable group seed
    seed = int(group.taz.min()*100 + group.hhincbin.min()*10 + group.hhsizebin.min())
    sample = group.sample(frac=group.SampleRate.min(), replace=True, random_state=seed)
    
    if len(sample)==0:
        print 'mgra ',group.taz.min(),' inc ', group.hhincbin.min(), ' size ', group.hhsizebin.min(),' sample is empty. Sample rate is ',group.SampleRate.min(),' size is ',len(group)
        sample = group
        effectiveRate = 1.0
    else:
        #set hh expansion factor based on actual sample size since sampling is lumpy
        sample.hhexpfac = 1.0 / (len(sample)*1.0/len(group)) 
        effectiveRate = 1.0 * len(sample)/len(group)		
        print("mgra %i hhincbin %s hhsizebin %s sample rate %.2f effective rate %.2f" % (group.taz.min(), group.hhincbin.min(), group.hhsizebin.min(), group.SampleRate.min(), 1.0 / sample.hhexpfac))
    
    # replace the target sample rate with the actual sample rate
    sample['SampleRate'] = effectiveRate
    
    return(sample)


def runPopSampler(tazSampleRateFileName, hhFileName, perFileName):
    # Read in TAZ sample rate table as pandas dataframe
    sampleRates = pd.read_csv(tazSampleRateFileName, delimiter=',')
    
    # Read in pop syn household table as pandas dataframe
    households = pd.read_csv(hhFileName, delimiter=',')
    
    # Read in popsyn persons table as pandas dataframe
    persons = pd.read_csv(perFileName, delimiter=',')
    
    #join sample rate by home taz
    households = pd.merge(households, sampleRates, left_on='mgra', right_on='MGRA')
    
    #bin hhs by income and size
    incbins = [-99999, 50000, 100000, households['hinc'].max()+1]
    households['hhincbin'] = pd.cut(households['hinc'], incbins, labels=False) # Double check household income field
    sizebins = [-1, 1, 2, 3, households['persons'].max()+1]
    households['hhsizebin'] = pd.cut(households['persons'], sizebins, labels=False) # Double check househod size field
    
    #group hhs by taz, hhincbin, hhsizebin and sample and reset index
    hhsGrouped = households.groupby(["mgra","hhincbin","hhsizebin"])
    new_households = hhsGrouped.apply(sample_hhs)
    new_households = new_households.reset_index(drop=True)
    
    #update ids and expand persons
    new_households['hhno_new'] = range(1,len(new_households)+1)
    new_persons = pd.merge(persons, new_households[["hhid","hhno_new", "SampleRate"]], left_on="hhid", right_on="hhid", )
    new_households['hhid'] = new_households['hhno_new'].astype(np.int32)
    new_persons['hhid'] = new_persons['hhno_new'].astype(np.int32)
    
    #new_persons = pd.merge(new_persons, sampleRates, left_on='mgra', right_on='MGRA')

    #delete added fields
    del new_households['hhno_new']
    del new_households['MGRA']
#    del new_households['SampleRate']
    del new_households['hhincbin']
    del new_households['hhsizebin']
    del new_persons['hhno_new']
    #del new_persons['MGRA']
    #del new_persons['mgra']
	
	#sort data
    new_households.sort_values('hhid', ascending=True, inplace=True)
    new_persons.sort_values(['hhid','pnum'], ascending=[True,True], inplace=True)
	
	#reset perid to sequential number
    new_persons['perid'] = range(1,len(new_persons)+1)

    # Write new households file to output data folder
    new_households.to_csv('new_'+hhFileName, sep=',', index=False)
        
    # Write new persons file to output data folder
    new_persons.to_csv('new_'+perFileName, sep=',', index=False)
   

# Run main
if __name__== "__main__":

    #Get argument inputs
    taz_sample_rate_file = sys.argv[1] #'sampleRateByTaz.csv'
    hh_file_name = sys.argv[2] #'households.csv'
    per_file_name = sys.argv[3] #'persons.csv'
  
    print(taz_sample_rate_file)
    print(hh_file_name)
    print(per_file_name)

    runPopSampler(taz_sample_rate_file, 
                  hh_file_name, per_file_name)
    
    print "*** Finished ***"




