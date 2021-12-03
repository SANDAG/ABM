# This file reformats and samples from the estimation data bundle prepared for trip destination choice so that
# Alogit can be used to estimate the model.
# The main input file to this script is trip_destination_alternatives_combined.csv. This file has a column for
# each maz, and rows that are dimensioned by tours*variables. Because
# mazs were sampled within each TAZ, for each tour there is only one MAZ for each of ~5k TAZs with data. 
# The other MAZs within each TAZ have no data in that column for that tour.
#
# Other files include
#    trip_destination_choosers_combined.csv: One record per tour with columns of data such as chosen MAZ, purpose, etc.
#    trip_destination_landuse.csv: A file of land-use data - one row per maz with land-use data variable columns.
#
# To run this script, all files must be in the same directory as the script, as well as the gawk.exe executable (http://gnuwin32.sourceforge.net/packages/gawk.htm). Type:
#   gawk -f processTripDestChoicedata.awk trip_destination_alternatives_combined.csv > trip_dest_estdata.csv
#
# The program will take about 30 minutes to run on a standard laptop. The steps that the program executes are:
#  1. reads trip_destination_choosers_combined.csv file and stores data in an array choosersData[trip_id,variable]
#  2. reads trip_destination_landuse.csv file and stores data in an array zoneData[zone_id,variable]
#  3. starts processing of main input file trip_destination_alternatives_combined.csv. Reads all available data for each tour (spanning multiple lines and skipping empty columns) into an array altData[altNumber,variable].
#         for each tour, samples (500) alternatives (MAZs). Writes data out for the trip including trip_destination_choice_choosers data,  
#         and land-use data for each sampled MAZ. Most of these variables are written out consecutively for each sampled alternative so that they can be read into arrays in ALOGIT.
#
# RSG JEF Nov 2021
		
BEGIN{

writeHeader="False"

field[1] =stopDest_dist
field[2] =originStop_dist
field[3] =logSize
field[4] =logProb
field[5] =noSize
field[6] =logsum


#initialize some stuff
rownumber=0
chooserID=0
chooserID_last=-1
chooserNumber=-1
altNumber=0

fieldsPerChooser=6

sampleSize=500

    FS=","

 	#read choosers data: trip_id,model_choice,override_choice,person_id,household_id,primary_purpose,trip_num,outbound,trip_count,destination,origin,failed,tour_id,purpose,depart,next_trip_id,tour_mode,tour_duration,tour_leg_dest,trip_period

	choosers=-1
	while ((getline < "trip_destination_choosers_combined.csv") > 0){
		++choosers
	    if(choosers==0)
			continue
			
#	    printf("%s\n",$0)
		trip_id=$1	
		chosen_maz=$3      
		tour_purpose=$6
		trip_num=$7
		outbound=$8
		trip_count=$9
		dest=$10
		orig=$11
		failed=$12
		trip_purpose=$14
		depart=$15
		tour_mode=$17
		duration=$18
		choosersData[trip_id,1]=chosen_maz
		choosersData[trip_id,2]=tour_purpose
		choosersData[trip_id,3]=trip_num
		choosersData[trip_id,4]=outbound
		choosersData[trip_id,5]=trip_count
		choosersData[trip_id,6]=dest
		choosersData[trip_id,7]=orig
		choosersData[trip_id,8]=failed
		choosersData[trip_id,9]=trip_purpose
		choosersData[trip_id,10]=depart
		choosersData[trip_id,11]=tour_mode
		choosersData[trip_id,12]=duration
			
	}
	#read land-use data
	zones=-1
	while ((getline < "trip_destination_landuse.csv") > 0){
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
		if(writeHeader=="True"){
					printf("chooserID,chosenMaz,chosenAlt,tourPurp,tripNumber,outbound,tripCount,orig,dest,failed,tripPurp,depart,mode")
			for(i=0;i<sampleSize;++i)
				printf(",maz_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",osDist_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",sdDist_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",logsum_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",sizeTerm_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp1_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp2_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp3_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp4_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp5_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emp6_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",emptot_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",k12_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",coll_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",active_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",hhs_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",noSampled_%i", (i+1))
			for(i=0;i<sampleSize;++i)
				printf(",soaFactor_%i", (i+1))
			printf("\n")
	}
		
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
			maz=headerFields[i]
 			
			altData[altNumber,1]=chooserID
			altData[altNumber,2]=maz
			altData[altNumber,3]=altNumber
			altData[altNumber,(fieldnumber+3)]=$i

			#only do this stuff once
			if(fieldnumber==1){
				if(maz == choosersData[chooserID,1])
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
		++numberOfChosen[chooserID, chosenAlt]
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
			++numberOfChosen[chooserID, sampledAltNumber]
			++sampledAlts
		}
		trip_id=chooserID
		
		#now write out data for chooser (1-3)
		printf("%i,",chooserID)               #ID
		printf("%i,",choosersData[chooserID,1]) #chosen maz
		printf("%i,",chosenAlt)
		
		#tour purpose 4
		if(choosersData[chooserID,2]=="work")
			printf("1,")
		else if(choosersData[chooserID,2]=="school")
			printf("2,")
		else if(choosersData[chooserID,2]=="shop")
			printf("3,")
		else if(choosersData[chooserID,2]=="visit")
			printf("4,")
		else if(choosersData[chooserID,2]=="other")
			printf("5,")
		else
			printf("-99,")	
			
	# trip number 5
	 printf("%i,",choosersData[trip_id,3])

	#outbound 6
	if(choosersData[trip_id,4]=="True")
		printf("1,")
	else
		printf("0,")
		
	# trip_count 7
	printf("%i,", choosersData[trip_id,5])
		
	# origin 8
	printf("%s,",	choosersData[trip_id,7])
	
	# dest 9
	printf("%s,",	choosersData[trip_id,6])

	#failed 10
	if(choosersData[trip_id,8]=="True")
		printf("1,")
	else
		printf("0,")

	#trip purpose 11
		if(choosersData[chooserID,9]=="work")
			printf("1,")
		else if(choosersData[chooserID,9]=="school")
			printf("2,")
		else if(choosersData[chooserID,9]=="shop")
			printf("3,")
		else if(choosersData[chooserID,9]=="visit")
			printf("4,")
		else if(choosersData[chooserID,9]=="other")
			printf("5,")
		else
			printf("-99,")	

	# depart period 12
	printf("%i,",choosersData[trip_id,10] )
	
   #mode 13
	 if(choosersData[trip_id,11]=="DRIVEALONE")
	 		printf("%i,",1)
	 else if(choosersData[trip_id,11]=="SHARED2")
	 		printf("%i,",2)
	 else if(choosersData[trip_id,11]=="SHARED3")
	 		printf("%i,",3)
	 else if(choosersData[trip_id,11]=="WALK")
	 		printf("%i,",4)
	 else
	 		printf("%i,",-99)
	 		
	 # duration
	 printf("%i,",choosersData[trip_id,12])
	 	
		#sampled MAZ
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%s,",altData[altNumber,3])
		}
		
		#os distance
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",altData[altNumber,(1+3)])
		}
		
		#sd distance
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,",altData[altNumber,(2+3)])
		}

		
		#logsums
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]		
			printf("%9.4f,", altData[altNumber,(6+3)])
		}
		
		#size
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			printf("%9.4f,",altData[altNumber,(3+3)])
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
			printf("%i,",numberOfChosen[chooserID,altNumber])
		}
		
		#sample of alts correction factor
		for( k=0;k<sampleSize;++k){
			altNumber=chosenData[ k ]	
			printf("%9.4f,",altData[altNumber,(4+3)])
		}

		
		printf("\n")
		
		
		
		
		
	}
	++rownumber
}
	
	
	
