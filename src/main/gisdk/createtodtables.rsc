/**************************************************************                                                                          
   CreateAutoTables                                                                                                                        
                                                                                                                                         
 Inputs
   input\airportAutoTrips_XX.mtx
   input\autoTrips_XX.mtx
   input\extTrip_XX.mtx
   
   where XX is period = {_EA,_AM,_MD,_PM,_EV}
   
   input\commVehTODTrips.mtx
   output\dailyDistributionMatricesTruckXX.mtx

   
 Outputs
   output\Trip_XX.mtx  
                                                                                                                                         
*******************************************************************/
Macro "Create Auto Tables"
   
   shared path, inputDir, outputDir       
   
   periods={"_EA","_AM","_MD","_PM","_EV"}
   
/*   
   truckTables = {
      inputDir+"\\dailyDistributionMatricesTruckam.mtx",
      inputDir+"\\dailyDistributionMatricesTruckop.mtx",
      inputDir+"\\dailyDistributionMatricesTruckpm.mtx" }
   
   truckPeriods = {2, 1, 2, 3, 2}
   truckFactors = {0.1, 1.0, 0.65, 1.0, 0.25}
   truckMatrices ={"lhdn","lhdt","mhdn","mhdt","hhdn","hhdt"}
   
   dim truckMatrices[truckTables.length]
   dim truckCurrencies[truckTables.length]
   for i = 1 to truckTables.length do
        // create truck matrix currencies
      truckMatrices[i] = OpenMatrix(truckTables[i], )                                                               
	   truckCurrencies[i] = CreateMatrixCurrencies(truckMatrices[i], , , )
   end
*/  
   truckTables = {
      outputDir+"\\dailyDistributionMatricesTruck_EA.mtx",
      outputDir+"\\dailyDistributionMatricesTruck_AM.mtx",
      outputDir+"\\dailyDistributionMatricesTruck_MD.mtx",
      outputDir+"\\dailyDistributionMatricesTruck_PM.mtx",
      outputDir+"\\dailyDistributionMatricesTruck_EV.mtx" }
        
   extTables = {
      outputDir+"\\ExtTrip_EA.mtx",
      outputDir+"\\ExtTrip_AM.mtx",
      outputDir+"\\ExtTrip_MD.mtx",
      outputDir+"\\ExtTrip_PM.mtx",
      outputDir+"\\ExtTrip_EV.mtx"}
   extMatrices = {"SOV_GP","SOV_PAY","SR2_GP","SR2_HOV","SR2_PAY","SR3_GP","SR3_HOV","SR3_PAY"}   
      
   
   airportTables = {
      outputDir+"\\autoAirportTrips_EA.mtx",
      outputDir+"\\autoAirportTrips_AM.mtx",
      outputDir+"\\autoAirportTrips_MD.mtx",
      outputDir+"\\autoAirportTrips_PM.mtx",
      outputDir+"\\autoAirportTrips_EV.mtx"}
      
   personTables = {
      outputDir+"\\autoTrips_EA.mtx",
      outputDir+"\\autoTrips_AM.mtx",
      outputDir+"\\autoTrips_MD.mtx",
      outputDir+"\\autoTrips_PM.mtx",
      outputDir+"\\autoTrips_EV.mtx"}

   //the following table names have the period appended 
   CTRampMatrices = {"SOV_GP","SOV_PAY","SR2_GP","SR2_HOV","SR2_PAY","SR3_GP","SR3_HOV","SR3_PAY"}   

	//output files                                                                                                                             
	outMatrixNames = {"Trip_EA.mtx", "Trip_AM.mtx", "Trip_MD.mtx", "Trip_PM.mtx", "Trip_EV.mtx"}                                                                                                   
	outTableNames = {"SOV_GP", "SOV_PAY", "SR2_GP","SR2_HOV", "SR2_PAY", "SR3_GP","SR3_HOV","SR3_PAY","lhdn","mhdn","hhdn","lhdt","mhdt","hhdt"}

   commVehTable = outputDir+"\\commVehTODTrips.mtx"
   commVehMatrices = {"EA Trips","AM Trips","MD Trips","PM Trips","EV Trips"}

	// create comm vehicle matrix currencies
   commVehMatrix = OpenMatrix(commVehTable, )                                                               
   commVehCurrencies = CreateMatrixCurrencies(commVehMatrix, , , )

   for i = 1 to periods.length do
   
      // open truck matrix currencies
      trkMatrix = OpenMatrix(truckTables[i], )                                                               
      truckCurrencies = CreateMatrixCurrencies(trkMatrix, , , )
      
      //open person trip matrix currencies
      personMatrix = OpenMatrix(personTables[i], )                                                               
      personCurrencies = CreateMatrixCurrencies(personMatrix, , , )

      //open airport matrix currencies
      airportMatrix = OpenMatrix(airportTables[i], )                                                               
	    airportCurrencies = CreateMatrixCurrencies(airportMatrix, , , )

      // create external matrix currencies
      extMatrix = OpenMatrix(extTables[i], )                                                               
	    extCurrencies = CreateMatrixCurrencies(extMatrix, , , )
	 
	   dim curr_array[extMatrices.length]
	   for j = 1 to extMatrices.length do
         curr_array[j] = CreateMatrixCurrency(extMatrix, extMatrices[j], ,, )
	   end
        
            //create output trip table and matrix currencies for this time period
	   outMatrix = CopyMatrixStructure(curr_array, {{"File Name", outputDir+"\\"+outMatrixNames[i]},                                       
         {"Label", outMatrixNames[i]},   
         {"Tables",outTableNames},                                                                                                          
         {"File Based", "Yes"}})  
      SetMatrixCoreNames(outMatrix, outTableNames)
      
      outCurrencies= CreateMatrixCurrencies(outMatrix, , , )

      // calculate output matrices
      outCurrencies.SOV_GP :=  personCurrencies.("SOV_GP"+periods[i])
                            + airportCurrencies.("SOV_GP"+periods[i]) 
                            + extCurrencies.("SOV_GP")
                            + commVehCurrencies.(commVehMatrices[i]) 
                       
      outCurrencies.SOV_PAY := personCurrencies.("SOV_PAY"+periods[i]) 
                            + airportCurrencies.("SOV_PAY"+periods[i]) 
                            + extCurrencies.("SOV_PAY")
      
      outCurrencies.SR2_GP := personCurrencies.("SR2_GP"+periods[i]) 
                            + airportCurrencies.("SR2_GP"+periods[i]) 
                            + extCurrencies.("SR2_GP")
      
      outCurrencies.SR2_HOV := personCurrencies.("SR2_HOV"+periods[i]) 
                            + airportCurrencies.("SR2_HOV"+periods[i]) 
                            + extCurrencies.("SR2_HOV")
     
      outCurrencies.SR2_PAY := personCurrencies.("SR2_PAY"+periods[i]) 
                            + airportCurrencies.("SR2_PAY"+periods[i]) 
                            + extCurrencies.("SR2_PAY")
     
      outCurrencies.SR3_GP := personCurrencies.("SR3_GP"+periods[i]) 
                            + airportCurrencies.("SR3_GP"+periods[i]) 
                            + extCurrencies.("SR3_GP")
      
      outCurrencies.SR3_HOV := personCurrencies.("SR3_HOV"+periods[i]) 
                            + airportCurrencies.("SR3_HOV"+periods[i]) 
                            + extCurrencies.("SR3_HOV")
     
      outCurrencies.SR3_PAY := personCurrencies.("SR3_PAY"+periods[i]) 
                            + airportCurrencies.("SR3_PAY"+periods[i]) 
                            + extCurrencies.("SR3_PAY")
     
     /*
      truckPeriod = truckPeriods[i]
      outCurrencies.lhdn := truckCurrencies[truckPeriod].lhdn * truckFactors[i]
      outCurrencies.mhdn := truckCurrencies[truckPeriod].mhdn * truckFactors[i]
      outCurrencies.hhdn := truckCurrencies[truckPeriod].hhdn * truckFactors[i]
      outCurrencies.lhdt := truckCurrencies[truckPeriod].lhdt * truckFactors[i]
      outCurrencies.mhdt := truckCurrencies[truckPeriod].mhdt * truckFactors[i]
      outCurrencies.hhdt := truckCurrencies[truckPeriod].hhdt * truckFactors[i]
      */
      
      outCurrencies.lhdn := truckCurrencies.lhdn
      outCurrencies.mhdn := truckCurrencies.mhdn
      outCurrencies.hhdn := truckCurrencies.hhdn
      outCurrencies.lhdt := truckCurrencies.lhdt
      outCurrencies.mhdt := truckCurrencies.mhdt
      outCurrencies.hhdt := truckCurrencies.hhdt
      
   end
   RunMacro("close all" )
   quit:
      Return(1 )

EndMacro                      
/**************************************************************                                                                          
   CreateTransitTables                                                                                                                        
                                                                                                                                         
 Inputs
   input\tranTrips_XX.mtx
   input\tranAirportTrips_XX.mtx
   
   where XX is period = {_EA,_AM,_MD,_PM,_EV}
   
   
 Outputs
   output\tranTotalTrips_XX.mtx  
                        
   TODO: Add Mexican resident and visitor trips
                                                                                                                 
*******************************************************************/
Macro "Create Transit Tables"
   
   shared path, inputDir, outputDir       
   
   periods={"_EA","_AM","_MD","_PM","_EV"}
   
   dim personFiles[periods.length]
   dim airportFiles[periods.length]
   dim outFiles[periods.length]

   for i = 1 to periods.length do
      personFiles[i]  = outputDir+"\\tranTrips"+periods[i]+".mtx"
      airportFiles[i] = outputDir+"\\tranAirportTrips"+periods[i]+".mtx"
      outFiles[i]  = outputDir+"\\tranTotalTrips"+periods[i]+".mtx"
   end


   //the following table names have the period appended 
   tableNames = {"WLK_LOC","WLK_EXP","WLK_BRT","WLK_LRT","WLK_CMR","PNR_LOC","PNR_EXP","PNR_BRT","PNR_LRT","PNR_CMR","KNR_LOC","KNR_EXP","KNR_BRT","KNR_LRT","KNR_CMR"}   

    for i = 1 to periods.length do

      //open person trip matrix currencies
      personMatrix = OpenMatrix(personFiles[i], )                                                               
      personCurrencies = CreateMatrixCurrencies(personMatrix, , , )

      //open airport matrix currencies
      airportMatrix = OpenMatrix(airportFiles[i], )                                                               
      airportCurrencies = CreateMatrixCurrencies(airportMatrix, , , )

      dim curr_array[tableNames.length]
      for j = 1 to curr_array.length do
         curr_array[j] = CreateMatrixCurrency(personMatrix, tableNames[j]+periods[i], ,, )
      end
        
      //create output trip table and matrix currencies for this time period
      outMatrix = CopyMatrixStructure(curr_array, {{"File Name", outFiles[i]},                                       
         {"Label", outFiles[i]},   
         {"Tables",tableNames},
         {"Compression",0},                                                                                                          
         {"File Based", "Yes"}})  
      SetMatrixCoreNames(outMatrix, tableNames)
      
      outCurrencies= CreateMatrixCurrencies(outMatrix, , , )

      // calculate output matrices
      for j = 1 to tableNames.length do
         outCurrencies.(tableNames[j]) :=  personCurrencies.(tableNames[j]+periods[i])
                            + airportCurrencies.(tableNames[j]+periods[i])
      end
   end
   RunMacro("close all" )
   quit:
      Return(1 )

EndMacro                      
/**************************************************************                                                                          
Create TOD Tables From 4Step Model                             
                                                                                                                                         
  TransCAD Macro used to create trip tables from 4-step model
  for assignment to 5 tod networks for first iteration of AB
  model.  Only needs to be run once for any given scenario year.                                           
                                                                                                                                         
 Inputs
   input\trptollam2.mtx
   input\trptollop2.mtx
   input\trptollpm2.mtx
                   
 Outputs
   output\Trip_EA.mtx
   output\Trip_AM.mtx
   output\Trip_MD.mtx
   output\Trip_PM.mtx
   output\Trip_EV.mtx
                                
Each matrix has the following cores:
    SOV_GP    SOV General purpose lanes
    SOV_PAY   SOV Toll eligible
    SR2_GP    SR2 General purpose lanes
    SR2_HOV   SR2 HOV lanes
    SR2_PAY   SR2 Toll eligible
    SR3_GP    SR3 General purpose lanes
    SR3_HOV   SR3 HOV lanes
    SR3_PAY   SR3 Toll eligible
    lhdn      Light heavy duty general purpose lanes
    mhdn      Medium heavy duty general purpose lanes
    hhdn      Heavy heavy duty general purpose lanes
    lhdt      Light heavy duty toll eligibl
    mhdt      Medium heavy duty general purpose lanes
    hhdt      Heavy heavy duty general purpose lanes                            
                                                                                                    
**************************************************************/                                                                          
Macro "Create TOD Tables From 4Step Model" 

   shared path, inputDir, outputDir       
   
   /*
   inputDir = "c:\\projects\\sandag\\series12\\base2008\\input"                                                                        
   outputDir = "c:\\projects\\sandag\\series12\\base2008\\output"                                                                        
    */
                                                                                                                                         
	//inputs                                                                                                                              
	amMatrixName = "trptollam2.mtx"                                                                                                 
	opMatrixName = "trptollop2.mtx"                                                                                                 
	pmMatrixName = "trptollpm2.mtx"                                                                                                 
	       
   inTableNames = {"dan", "dat", "s2nn", "s2nh", "s2th", "M1", "M2", "M3", "lhdn","mhdn","hhdn", "lhdt","mhdt","hhdt"}       
   
                                                                                                                                       
	//output files                                                                                                                             
	outMatrixNames = {"Trip_EA.mtx", "Trip_AM.mtx", "Trip_MD.mtx", "Trip_PM.mtx", "Trip_EV.mtx"}                                                                                                   
	outTableNames = {"SOV_GP", "SOV_PAY", "SR2_GP","SR2_HOV", "SR2_PAY", "SR3_GP","SR3_HOV","SR3_PAY","lhdn","mhdn","hhdn","lhdt","mhdt","hhdt"}
	                                                                                                                                      
   // open input matrices
   amMatrix = OpenMatrix(inputDir+"\\"+amMatrixName, )                                                               
   opMatrix = OpenMatrix(inputDir+"\\"+opMatrixName, )                                                               
   pmMatrix = OpenMatrix(inputDir+"\\"+pmMatrixName, )                                                               
	
	// create input matrix currencies
	dim inCurrencies[outMatrixNames.length,inTableNames.length]
	for i = 1 to inTableNames.length do
      inCurrencies[1][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
      inCurrencies[2][i] = CreateMatrixCurrency(amMatrix, inTableNames[i], ,, )
      inCurrencies[3][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
      inCurrencies[4][i] = CreateMatrixCurrency(pmMatrix, inTableNames[i], ,, )
      inCurrencies[5][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
	end
	
	
	// create the output matrices, copying the matrix structure of the input matrices.  Then rename the matrices.
	dim outMatrices[outMatrixNames.length]
	dim outCurrencies[outMatrixNames.length,outTableNames.length]
	for i = 1 to outMatrixNames.length do

	   outMatrices[i] = CopyMatrixStructure(inCurrencies[i], {{"File Name", outputDir+"\\"+outMatrixNames[i]},                                       
         {"Label", outMatrixNames[i]},   
         {"Tables",outTableNames},                                                                                                          
         {"File Based", "Yes"}})  
      SetMatrixCoreNames(outMatrices[i], outTableNames)
      
      for j = 1 to outTableNames.length do
         outCurrencies[i][j] = CreateMatrixCurrency(outMatrices[i], outTableNames[j], ,, )
      end
   end                                                                                                 
                         
   // factor the off-peak input table to 3 time periods (1=EA,3=MD,5=PM, and the factors are generic across input tables) 
   for i = 1 to outTableNames.length do
      outCurrencies[1][i] := inCurrencies[1][i] * 0.10
      outCurrencies[2][i] := inCurrencies[2][i]
      outCurrencies[3][i] := inCurrencies[3][i] * 0.65
      outCurrencies[4][i] := inCurrencies[4][i]
      outCurrencies[5][i] := inCurrencies[5][i] * 0.25
   end
   
   Return(1)
   
	
	
EndMacro	                                                             


/***************************************************************************************************************************



****************************************************************************************************************************/
Macro "Create EE & EI Trips"
  
  
     shared path, inputDir, outputDir       
  
	//inputs                                                                                                                              
	amMatrixName = "trptollam2.mtx"                                                                                                 
	opMatrixName = "trptollop2.mtx"                                                                                                 
	pmMatrixName = "trptollpm2.mtx"   
	       
   inTableNames = {"dan", "dat", "s2nn", "s2nh", "s2th", "M1", "M2", "M3", "lhdn","mhdn","hhdn", "lhdt","mhdt","hhdt"}       

 	//output files                                                                                                                             
	outMatrixNames = {"ExtTrip_EA.mtx", "ExtTrip_AM.mtx", "ExtTrip_MD.mtx", "ExtTrip_PM.mtx", "ExtTrip_EV.mtx"}                                                                                                   
	outTableNames = {"SOV_GP", "SOV_PAY", "SR2_GP","SR2_HOV", "SR2_PAY", "SR3_GP","SR3_HOV","SR3_PAY","lhdn","mhdn","hhdn","lhdt","mhdt","hhdt"}
  
   // open input matrices
   amMatrix = OpenMatrix(inputDir+"\\"+amMatrixName, )                                                               
   opMatrix = OpenMatrix(inputDir+"\\"+opMatrixName, )                                                               
   pmMatrix = OpenMatrix(inputDir+"\\"+pmMatrixName, )                                                               

	// create input matrix currencies
	dim inCurrencies[outMatrixNames.length,inTableNames.length]
	for i = 1 to inTableNames.length do
      inCurrencies[1][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
      inCurrencies[2][i] = CreateMatrixCurrency(amMatrix, inTableNames[i], ,, )
      inCurrencies[3][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
      inCurrencies[4][i] = CreateMatrixCurrency(pmMatrix, inTableNames[i], ,, )
      inCurrencies[5][i] = CreateMatrixCurrency(opMatrix, inTableNames[i], ,, )
	end
	
	//create an array of internal zone ids
	dim zones[4670]
	for i = 1 to zones.length do
	   zones[i]=i2s(i+12)
	end
	// create the output matrices, copying the matrix structure of the input matrices.  Then rename the matrices.
	dim outMatrices[outMatrixNames.length]
	dim outCurrencies[outMatrixNames.length,outTableNames.length]
	for i = 1 to outMatrixNames.length do

	   outMatrices[i] = CopyMatrixStructure(inCurrencies[i], {{"File Name", outputDir+"\\"+outMatrixNames[i]},                                       
         {"Label", outMatrixNames[i]},   
         {"Tables",outTableNames},                                                                                                          
         {"File Based", "Yes"}})  
      SetMatrixCoreNames(outMatrices[i], outTableNames)

      for j = 1 to outTableNames.length do
         outCurrencies[i][j] = CreateMatrixCurrency(outMatrices[i], outTableNames[j], ,, )
         
         // set the output matrix to the input matrix
         outCurrencies[i][j] := inCurrencies[i][j]
            
         //set the internal-internal values to 0
         FillMatrix(outCurrencies[i][j], zones, zones, {"Copy", 0.0},)

      end
   end                                                                                                 

   // factor the off-peak input table to 3 time periods (1=EA,3=MD,5=PM, and the factors are generic across input tables) 
   for i = 1 to outTableNames.length do
      outCurrencies[1][i] := outCurrencies[1][i] * 0.10
      outCurrencies[2][i] := outCurrencies[2][i]
      outCurrencies[3][i] := outCurrencies[3][i] * 0.65
      outCurrencies[4][i] := outCurrencies[4][i]
      outCurrencies[5][i] := outCurrencies[5][i] * 0.25
   end 
   RunMacro("close all" )
   quit:
      Return(1 )


EndMacro
                                        