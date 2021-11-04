# This file reformats and samples from the estimation data bundle prepared for tour OD choice so that
# Alogit can be used to estimate a combined model of tour OD and border crossing mode choice.
# The main input file to this script is tour_od_choice_alternatives_combined.csv. This file has a column for
# each combination of crossing station and maz, and rows that are dimensioned by tours*variables. Because
# mazs were sampled within each TAZ, for each tour there is only one MAZ for each of ~5k TAZs and 3 border
# crossing stations with data. The other MAZs within each TAZ have no data in that column for that tour.
#
# Other files include
#    tour_od_choice_choosers_combined.csv: One record per tour with columns of data such as chosen station, chosen MAZ, purpose, etc.
#    tour_od_choice_landuse.csv: A file of land-use data - one row per maz with land-use data variable columns.
#    tour_mode_choice_values_combined.csv: The estimation data bundle for tour mode choice, one record per tour with columns of data for the tour mode choice model including chosen mode.
#    crossborder_pointofentrywaittime48.csv: Wait times by ranges of start and end time period and POE.
#
# To run this script, all files must be in the same directory as the script, as well as the gawk.exe executable (http://gnuwin32.sourceforge.net/packages/gawk.htm). Type:
#   gawk -f processODchoicedata.awk tour_od_choice_alternatives_combined.csv > tour_od_choice_estdata.csv
#
# The program will take about 30 minutes to run on a standard laptop. The steps that the program executes are:
#  1. reads tour_od_choice_choosers_combined.csv file and stores data in an array choosersData[tour_id,variable]
#  2. reads tour_od_choice_landuse.csv file and stores data in an array zoneData[zone_id,variable]
#  3. reads tour_mode_choice_values_combined.csv and stores data in array modeData[tour_id,variable]
#  4. reads crossborder_pointofentrywaittime48.csv and stores data in array waitDataStartAndEnd[records,variable]. Then expands the data to be easily referenced by start time and stores in
#  array aitData[station,start,variable]
#  5. starts processing of main input file tour_od_choice_alternatives_combined.csv. Reads all available data for each tour (spanning multiple lines and skipping empty columns) into an array altData[altNumber,variable].
#         for each tour, samples (100) alternative combinations of station and MAZ. Writes data out for the tour including tour_od_choice_choosers data, mode choice data, wait time for each sampled station and mode/pass type, 
#         and land-use data for each sampled MAZ. Most of these variables are written out consecutively for each sampled alternative so that they can be read into arrays in ALOGIT.
#
# RSG JEF Nov 2021
		
BEGIN{

field[1] =   otay_mesa_calibration_adjustment   
field[2] =   otay_mesa_mandatory                
field[3] =   otay_mesa_non_mandatory            
field[4] =   poe_accessibility                  
field[5] =   tecate_calibration_adjustment      
field[6] =   tecate_mandatory                   
field[7] =   tecate_non_mandatory               
field[8] =   util_dist                          
field[9] =   util_inbound_trip_logsum_tour_da   
field[10] =  util_inbound_trip_logsum_tour_s2   
field[11] =  util_inbound_trip_logsum_tour_s3   
field[12] =  util_inbound_trip_logsum_tour_walk 
field[13] =  util_mode_logsum                   
field[14] =  util_no_attractions                
field[15] =  util_outbound_trip_logsum_tour_da  
field[16] =  util_outbound_trip_logsum_tour_s2  
field[17] =  util_outbound_trip_logsum_tour_s3  
field[18] =  util_outbound_trip_logsum_tour_walk
field[19] =  util_sample_of_corrections_factor  
field[20] =  util_size_variable                 

#initialize some stuff
rownumber=0
chooserID=0
chooserID_last=-1
chooserNumber=-1
altNumber=0

fieldsPerChooser=20

stations[0] = 23003
stations[1] = 23004
stations[2] = 23005

sampleSize=100

    FS=","

	#read choosers data: tour_id,model_choice,override_choice,is_mandatory,tour_type,household_id
	choosers=-1
	while ((getline < "tour_od_choice_choosers_combined.csv") > 0){
		++choosers
	    if(choosers==0)
			continue
			
#	    printf("%s\n",$0)
		tour_id=$1	
		split($3, altarray, "_")
		station=altarray[1]
		maz=altarray[2]
		
		if(station==7090)
			station=23003
		else if(station==7066)
			station=23004
		else if(station==21895)
      station=23005
      
		mandatory=$4
		purpose=$5
		choosersData[tour_id,1]=station
		choosersData[tour_id,2]=maz
		choosersData[tour_id,3]=mandatory
		choosersData[tour_id,4]=purpose
		
	}
	#read land-use data
	zones=-1
	while ((getline < "tour_od_choice_landuse.csv") > 0){
		++zones
	    if(zones==0)
			continue
			
#	    printf("%s\n",$0)
		zone_id=$1	
		hhs=$7
		emp_ag                          =$26
		emp_const_non_bldg_prod         =$27
  	emp_const_non_bldg_office       =$28
  	emp_utilities_prod              =$29
  	emp_utilities_office            =$30
  	emp_const_bldg_prod             =$31
  	emp_const_bldg_office           =$32
  	emp_mfg_prod                    =$33
  	emp_mfg_office                  =$34
  	emp_whsle_whs                   =$35
  	emp_trans                       =$36
  	emp_retail                      =$37
  	emp_prof_bus_svcs               =$38
  	emp_prof_bus_svcs_bldg_maint    =$39
  	emp_pvt_ed_k12                  =$40
  	emp_pvt_ed_post_k12_oth         =$41
  	emp_health                      =$42
  	emp_personal_svcs_office        =$43
  	emp_amusement                   =$44
  	emp_hotel                       =$45
  	emp_restaurant_bar              =$46
  	emp_personal_svcs_retail        =$47
  	emp_religious                   =$48
  	emp_pvt_hh                      =$49
  	emp_state_local_gov_ent         =$50
  	emp_fed_non_mil                 =$51
  	emp_fed_mil                     =$52
  	emp_state_local_gov_blue        =$53
  	emp_state_local_gov_white       =$54
    emp_public_ed                   =$55
    emp_own_occ_dwell_mgmt          =$56
		emp_total                       =$60
		enrollgradekto8                 =$61
		enrollgrade9to12                =$62 
		collegeenroll                   =$63
		othercollegeenroll              =$64
		adultschenrl                    =$65
		parkactive                      =$81
		openspaceparkpreserve           =$82
		beachactive                     =$83
		hotelroomtotal                  =$89
		
		# ag emp
		zoneData[zone_id,1]=   emp_ag
		
		# construction, utilities, manufacturing, wholesale, transportation
		zoneData[zone_id,2]=  emp_const_non_bldg_prod + emp_utilities_prod + emp_const_bldg_prod + emp_mfg_prod + emp_whsle_whs + emp_trans
	  
	  # office
	  zoneData[zone_id,3]=  emp_const_non_bldg_office	+ emp_utilities_office + emp_const_bldg_office + emp_mfg_office + emp_personal_svcs_office + emp_personal_svcs_office
		
		# retail
		zoneData[zone_id,4] = emp_retail
		
		# amusement, hotel, restaurant & bar
		zoneData[zone_id,5] = emp_amusement + emp_hotel + emp_restaurant_bar      
		
		# other employment
		zoneData[zone_id,6] = emp_total - (zoneData[zone_id,1] + zoneData[zone_id,2]+ zoneData[zone_id,3] + zoneData[zone_id,4] + zoneData[zone_id,5])
		
		# total employment
		zoneData[zone_id,7] = emp_total
		
		#K-12 enrollment
		zoneData[zone_id,8] = enrollgradekto8 + enrollgrade9to12
		
		#college enrollment
		zoneData[zone_id,9] = collegeenroll + othercollegeenroll + adultschenrl
		
		#active public space
		zoneData[zone_id,10] = parkactive + beachactive
		
		# hhs
		zoneData[zone_id,11] = hhs
	}
	
	#read mode choice data
	tours=-1
	while ((getline < "tour_mode_choice_values_combined.csv") > 0){
		++tours
	    if(tours==0)
			continue
			
#	    printf("%s\n",$0)
	 tour_id = $1
	 mode_choice=$3
	 pass_type=$37
	 tour_type=$38
	 number_of_participants=$41
	 start=$47
   end=$48
   modeData[tour_id,1] = mode_choice
   modeData[tour_id,2] = pass_type
   modeData[tour_id,3] = tour_type
   modeData[tour_id,4] = number_of_participants
   modeData[tour_id,5] = start
   modeData[tour_id,6] = end
 }

	#read wait time data
	records=0
	while ((getline < "crossborder_pointofentrywaittime48.csv") > 0){
		++records
	    if(records==1)
			continue
			
#	    printf("%s\n",$0)
	 poe_number = $1
   start_period = $4
   end_period = $5
   ped_wait = $6
   ready_wait = $7
   standard_wait = $8
   sentri_wait = $9

   station = stations[poe_number]
   
   waitDataStartAndEnd[records,0] = station
	 waitDataStartAndEnd[records,1] = start_period
   waitDataStartAndEnd[records,2] = end_period
   waitDataStartAndEnd[records,3] = standard_wait
   waitDataStartAndEnd[records,4] = sentri_wait
   waitDataStartAndEnd[records,5] = ready_wait
   waitDataStartAndEnd[records,6] = ped_wait
   
   
 }
	
# expand wait data to 48 periods
  for(poe=0;poe<3;++poe)
     for(start=1;start<=48;++start){
     		
     		station = stations[poe]
     		
     		for(record=1;record<=records;++record){
     			recordStation =   waitDataStartAndEnd[record,0] 
     			recordStart =     waitDataStartAndEnd[record,1] 
     			recordEnd =       waitDataStartAndEnd[record,2] 
     			
     			if((start>=recordStart) && (start<= recordEnd) && (station==recordStation) ){
     				 waitData[station,start,1] = waitDataStartAndEnd[record,3]
     			   waitData[station,start,2] = waitDataStartAndEnd[record,4] 
     			   waitData[station,start,3] = waitDataStartAndEnd[record,5]
     			   waitData[station,start,4] = waitDataStartAndEnd[record,6]
     			   
#     			   printf("station %i start %i 1 = %9.2f\n", station, start, waitData[station,start,1])
#     			   printf("station %i start %i 1 = %9.2f\n", station, start, waitData[station,start,2])
#      			 printf("station %i start %i 1 = %9.2f\n", station, start, waitData[station,start,3])
#      			 printf("station %i start %i 1 = %9.2f\n", station, start, waitData[station,start,4])
      	    }
     		}
     }

}
{

#    if(rownumber<30|| (rownumber %100==0))
#		printf("Row %i\n",rownumber)
	
	# get alternative names
	if(rownumber==0){
#		printf("fields: %i\n", NF)
		for(i=1;i<=NF;++i){
			headerFields[i]=$i
#			printf("%s\n", headerFields[i])
		}
		++rownumber
		next
	}
  
    # set up chooser data
	chooserID=$1
	if(chooserID_last!=chooserID){
		chooserID_last=$1
		fieldnumber=1
	}else{
		++fieldnumber

	}
	
	# iterate through alternatives
	altNumber=0
	
	for(i=3;i<=NF;i++)
		if($i!=""){
#			printf("%s\n",$i)
			++altNumber
			split(headerFields[i], altarray, "_")
			station=altarray[1]
			maz=altarray[2]
 			
			altData[altNumber,1]=chooserID
			altData[altNumber,2]=station
			altData[altNumber,3]=maz
			altData[altNumber,4]=altNumber
			altData[altNumber,(fieldnumber+4)]=$i

			#only do this stuff once
			if(fieldnumber==1){
				if((station==choosersData[chooserID,1]) && (maz == choosersData[chooserID,2]))
					chosenAlt=altNumber

			}

		}
  
	#write results
	if(fieldnumber==fieldsPerChooser){
		
		if(chosenAlt==0)
			next
		
		
		sampledAlts=0
	  
	  #first observation is always chosen obs
		chosenData[ sampledAlts ] = chosenAlt
		++numberOfChosen[chosenAlt]
		++sampledAlts
	
	  #start sampling
		
		while(sampledAlts<sampleSize){
			
			random=rand()
			sampledAltNumber= sprintf("%i", ((random*altNumber) + .5))
			
			while(sampledAltNumber==0){
				random=rand()
				sampledAltNumber= sprintf("%i", (random*(altNumber) + 0.5))
			}
			
#			printf("Chooser %i choosing alt %i out of %i alts\n",chooserID, sampledAltNumber,altNumber)
			
			chosenData[ sampledAlts ] = sampledAltNumber
			++numberOfChosen[sampledAltNumber]
			++sampledAlts
		}
		
		#now write out data for chooser
		printf("%i,",chooserID)               #ID
		printf("%i,",choosersData[chooserID,1]) #chosen station
		printf("%i,",choosersData[chooserID,2]) #chosen maz
		printf("%i,",chosenAlt)
		
   #mode
	 if(modeData[chooserID,1]=="DRIVEALONE")
	 		printf("%i,",1)
	 else if(modeData[chooserID,1]=="SHARED2")
	 		printf("%i,",2)
	 else if(modeData[chooserID,1]=="SHARED3")
	 		printf("%i,",3)
	 else if(modeData[chooserID,1]=="WALK")
	 		printf("%i,",4)
	 else
	 		printf("%i,",-99)
	 	
	 	#pass type
	 if(modeData[chooserID,2]=="no_pass")
	 		printf("%i,",0)
	 else if(modeData[chooserID,2]=="sentri")
	 		printf("%i,",1)
	 else if(modeData[chooserID,2]=="ready")
	 		printf("%i,",2)
	 else
	 		printf("%i,",-99)
	 
	 # number participants
	 printf("%i,", modeData[chooserID,4])
	 
	 # start period
	 printf("%i,", modeData[chooserID,5])
	 start= modeData[chooserID,5]
	 
	 # end period
	 printf("%i,", modeData[chooserID,6])

		#purpose
		if(choosersData[chooserID,4]=="work")
			printf("1,")
		else if(choosersData[chooserID,4]=="school")
			printf("2,")
		else if(choosersData[chooserID,4]=="shop")
			printf("3,")
		else if(choosersData[chooserID,4]=="visit")
			printf("4,")
		else if(choosersData[chooserID,4]=="other")
			printf("5,")
		else
			printf("-99,")	
			
		#sampled MAZ
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%s,",altData[altNumber,3])
		}
		#sampled station
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%s,",altData[altNumber,2])
		}
		#sampled station wait time standard auto
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			station = altData[altNumber,2]
			printf("%9.2f,",waitData[station,start,1])
		}		
		#sampled station wait time sentry pass 
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			station = altData[altNumber,2]
			printf("%9.2f,",waitData[station,start,2])
		}		
		#sampled station wait time ready lane
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			station = altData[altNumber,2]
			printf("%9.2f,",waitData[station,start,3])
		}		
		
		#sampled station wait time pedestrian
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			station = altData[altNumber,2]
			printf("%9.2f,",waitData[station,start,4])
	}		
		
		#distance
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",altData[altNumber,(8+4)])
		}
		#da logsums
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",(altData[altNumber,(9+4)]+altData[altNumber,(15+4)]))
		}
		#s2 logsums
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",(altData[altNumber,(10+4)]+altData[altNumber,(16+4)]))
		}
		#s3 logsums
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",(altData[altNumber,(11+4)]+altData[altNumber,(17+4)]))
		}
		#walk logsums
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",(altData[altNumber,(12+4)]+altData[altNumber,(18+4)]))
		}
		#size
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			printf("%9.4f,",altData[altNumber,(20+4)])
		}
	  # ag emp
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,1])
		}
		
		# construction, utilities, manufacturing, wholesale, transportation
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,2])
		}
	  
	  # office
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,3])
		}
		
		# retail
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,4])
		}
		
		# amusement, hotel, restaurant & bar
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,5])
		}
		
		# other employment
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,6])
		}
		
		# total employment
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,7])
		}
		
		#K-12 enrollment
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,8])
		}
		
		#college enrollment
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%i,",zoneData[zone_id,9])
		}
		
		#active public space
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%9.2f,",zoneData[zone_id,10])
		}
		
		#hhs 
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			zone_id = altData[altNumber,3]
			printf("%9.2f,",zoneData[zone_id,11])
		}

		
		#number sampled
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",numberOfChosen[altNumber])
		}
		
		#sample of alts correction factor
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			printf("%9.4f,",altData[altNumber,(19+4)])
		}

		
		printf("\n")
		
		
		
		
		
	}
	++rownumber
}
	
	
	
