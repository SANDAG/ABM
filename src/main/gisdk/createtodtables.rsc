/**************************************************************                                                                          
   CreateAutoTables                                                                                                                        
                                                                                                                                         
 Inputs
   input\airportAutoTrips_XX.mtx
   input\autoTrips_XX.mtx
   input\extTrip_XX.mtx
   
   where XX is period = {_EA,_AM,_MD,_PM,_EV}
   
   input\commVehTODTrips.mtx
   input\dailyDistributionMatricesTruckam.mtx
   input\dailyDistributionMatricesTruckpm.mtx
   input\dailyDistributionMatricesTruckop.mtx
   
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
 */  
 
   truckTables = {
      outputDir+"\\dailyDistributionMatricesTruckEA.mtx",
      outputDir+"\\dailyDistributionMatricesTruckAM.mtx",
      outputDir+"\\dailyDistributionMatricesTruckMD.mtx",
      outputDir+"\\dailyDistributionMatricesTruckPM.mtx",
      outputDir+"\\dailyDistributionMatricesTruckEV.mtx" }
   
   dim truckMatrices[truckTables.length]
   dim truckCurrencies[truckTables.length]
   
   for i = 1 to truckTables.length do
        // create truck matrix currencies
      truckMatrices[i] = OpenMatrix(truckTables[i], )                                                               
	  truckCurrencies[i] = CreateMatrixCurrencies(truckMatrices[i], , , )
   end
   
    externalInternalTables = {
   	  outputDir+"\\usSdWrk",
   	  outputDir+"\\usSdNon"
    	}
    
    //create external-external matrix from csv input file	
    ok = RunMacro("TCB Run Macro", 1,"Create External-External Trip Matrix",{}) 
    if !ok then goto quit
    
    //create external-external currencies
    externalExternalMatrixName = outputDir + "\\externalExternalTrips.mtx"
    externalExternalMatrix = OpenMatrix(externalExternalMatrixName, )
    externalExternalCurrency = CreateMatrixCurrency(externalExternalMatrix,'Trips',,,)
	  externalExternalDiurnalFactors = { 0.074, 0.137, 0.472, 0.183, 0.133}
    externalExternalOccupancyFactors = {0.43, 0.42, 0.15 }
  	
	
   	internalExternalTables = {
      outputDir+"\\autoInternalExternalTrips_EA.mtx",
      outputDir+"\\autoInternalExternalTrips_AM.mtx",
      outputDir+"\\autoInternalExternalTrips_MD.mtx",
      outputDir+"\\autoInternalExternalTrips_PM.mtx",
      outputDir+"\\autoInternalExternalTrips_EV.mtx"}

   	
   	visitorTables = {
      outputDir+"\\autoVisitorTrips_EA.mtx",
      outputDir+"\\autoVisitorTrips_AM.mtx",
      outputDir+"\\autoVisitorTrips_MD.mtx",
      outputDir+"\\autoVisitorTrips_PM.mtx",
      outputDir+"\\autoVisitorTrips_EV.mtx"}
  		
   	crossBorderTables = {
      outputDir+"\\autoCrossBorderTrips_EA.mtx",
      outputDir+"\\autoCrossBorderTrips_AM.mtx",
      outputDir+"\\autoCrossBorderTrips_MD.mtx",
      outputDir+"\\autoCrossBorderTrips_PM.mtx",
      outputDir+"\\autoCrossBorderTrips_EV.mtx"}
 
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
   commVehMatrices = {"EA NonToll","AM NonToll","MD NonToll","PM NonToll","EV NonToll","EA Toll","AM Toll","MD Toll","PM Toll","EV Toll"}

	// create comm vehicle matrix currencies
   commVehMatrix = OpenMatrix(commVehTable, )                                                               
   commVehCurrencies = CreateMatrixCurrencies(commVehMatrix, , , )


   for i = 1 to periods.length do

      //open person trip matrix currencies
      personMatrix = OpenMatrix(personTables[i], )                                                               
      personCurrencies = CreateMatrixCurrencies(personMatrix, , , )
      
 	   dim curr_array[CTRampMatrices.length]
	   for j = 1 to CTRampMatrices.length do
         curr_array[j] = CreateMatrixCurrency(personMatrix, CTRampMatrices[j]+periods[i], ,, )
	   end


     //open airport matrix currencies
      airportMatrix = OpenMatrix(airportTables[i], )                                                               
	    airportCurrencies = CreateMatrixCurrencies(airportMatrix, , , )

     //open visitor matrix currencies
      visitorMatrix = OpenMatrix(visitorTables[i], )                                                               
	    visitorCurrencies = CreateMatrixCurrencies(visitorMatrix, , , )

     //open cross border matrix currencies
      crossBorderMatrix = OpenMatrix(crossBorderTables[i], )                                                               
	    crossBorderCurrencies = CreateMatrixCurrencies(crossBorderMatrix, , , )

     //open internal-external matrix currencies
      internalExternalMatrix = OpenMatrix(internalExternalTables[i], )                                                               
	    internalExternalCurrencies = CreateMatrixCurrencies(internalExternalMatrix, , , )
  
     //open external-internal work matrix currencies
      externalInternalWrkMatrix = OpenMatrix(externalInternalTables[1]+periods[i]+".mtx", ) 
	    externalInternalWrkCurrencies = CreateMatrixCurrencies(externalInternalWrkMatrix, , , )
     
     //open external-internal non-work matrix currencies
      externalInternalNonMatrix = OpenMatrix(externalInternalTables[2]+periods[i]+".mtx", ) 
	    externalInternalNonCurrencies = CreateMatrixCurrencies(externalInternalNonMatrix, , , )
     
          
     //create output trip table and matrix currencies for this time period
	   outMatrix = CopyMatrixStructure(curr_array, {{"File Name", outputDir+"\\"+outMatrixNames[i]},                                       
         {"Label", outMatrixNames[i]},   
         {"Tables",outTableNames},                                                                                                          
         {"File Based", "Yes"}})  
      SetMatrixCoreNames(outMatrix, outTableNames)
      
      outCurrencies= CreateMatrixCurrencies(outMatrix, , , )

      // calculate output matrices
      outCurrencies.SOV_GP :=Nz(outCurrencies.SOV_GP)
      outCurrencies.SOV_GP :=  Nz(personCurrencies.("SOV_GP"+periods[i]))
                            + Nz(airportCurrencies.("SOV_GP"+periods[i])) 
                            + Nz(visitorCurrencies.("SOV_GP"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SOV_GP"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SOV_GP"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("DAN"))
                            + Nz(externalInternalNonCurrencies.("DAN"))
                            + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[1]
                            + Nz(commVehCurrencies.(commVehMatrices[i])) 
                       
      outCurrencies.SOV_PAY :=Nz(outCurrencies.SOV_PAY)
      outCurrencies.SOV_PAY := Nz(personCurrencies.("SOV_PAY"+periods[i])) 
                            + Nz(airportCurrencies.("SOV_PAY"+periods[i])) 
                            + Nz(visitorCurrencies.("SOV_PAY"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SOV_PAY"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SOV_PAY"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("DAT"))
                            + Nz(externalInternalNonCurrencies.("DAT"))
                            + Nz(commVehCurrencies.(commVehMatrices[i+5]))

      outCurrencies.SR2_GP :=Nz(outCurrencies.SR2_GP)
      outCurrencies.SR2_GP := Nz(personCurrencies.("SR2_GP"+periods[i])) 
                            + Nz(airportCurrencies.("SR2_GP"+periods[i])) 
                            + Nz(visitorCurrencies.("SR2_GP"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR2_GP"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR2_GP"+periods[i])) 

      outCurrencies.SR2_HOV :=Nz(outCurrencies.SR2_HOV)  
      outCurrencies.SR2_HOV := Nz(personCurrencies.("SR2_HOV"+periods[i])) 
                            + Nz(airportCurrencies.("SR2_HOV"+periods[i])) 
                            + Nz(visitorCurrencies.("SR2_HOV"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR2_HOV"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR2_HOV"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("S2N"))
                            + Nz(externalInternalNonCurrencies.("S2N"))
                            + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[2]
 
      outCurrencies.SR2_PAY :=Nz(outCurrencies.SR2_PAY)
      outCurrencies.SR2_PAY := Nz(personCurrencies.("SR2_PAY"+periods[i])) 
                            + Nz(airportCurrencies.("SR2_PAY"+periods[i])) 
                            + Nz(visitorCurrencies.("SR2_PAY"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR2_PAY"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR2_PAY"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("S2T"))
                            + Nz(externalInternalNonCurrencies.("S2T"))

      outCurrencies.SR3_GP :=Nz(outCurrencies.SR3_GP)
      outCurrencies.SR3_GP := Nz(personCurrencies.("SR3_GP"+periods[i])) 
                            + Nz(airportCurrencies.("SR3_GP"+periods[i]))
                            + Nz(visitorCurrencies.("SR3_GP"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR3_GP"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR3_GP"+periods[i])) 

      outCurrencies.SR3_HOV :=Nz(outCurrencies.SR3_HOV) 
      outCurrencies.SR3_HOV := Nz(personCurrencies.("SR3_HOV"+periods[i])) 
                            + Nz(airportCurrencies.("SR3_HOV"+periods[i])) 
                            + Nz(visitorCurrencies.("SR3_HOV"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR3_HOV"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR3_HOV"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("S3N"))
                            + Nz(externalInternalNonCurrencies.("S3N"))
                            + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[3]

      outCurrencies.SR3_PAY :=Nz(outCurrencies.SR3_PAY)
      outCurrencies.SR3_PAY := Nz(personCurrencies.("SR3_PAY"+periods[i])) 
                            + Nz(airportCurrencies.("SR3_PAY"+periods[i])) 
                            + Nz(visitorCurrencies.("SR3_PAY"+periods[i])) 
                            + Nz(crossBorderCurrencies.("SR3_PAY"+periods[i])) 
                            + Nz(internalExternalCurrencies.("SR3_PAY"+periods[i])) 
                            + Nz(externalInternalWrkCurrencies.("S3T"))
                            + Nz(externalInternalNonCurrencies.("S3T"))
     /*
      truckPeriod = truckPeriods[i]
      outCurrencies.lhdn := truckCurrencies[truckPeriod].lhdn * truckFactors[i]
      outCurrencies.mhdn := truckCurrencies[truckPeriod].mhdn * truckFactors[i]
      outCurrencies.hhdn := truckCurrencies[truckPeriod].hhdn * truckFactors[i]
      outCurrencies.lhdt := truckCurrencies[truckPeriod].lhdt * truckFactors[i]
      outCurrencies.mhdt := truckCurrencies[truckPeriod].mhdt * truckFactors[i]
      outCurrencies.hhdt := truckCurrencies[truckPeriod].hhdt * truckFactors[i]
      */

	  outCurrencies.lhdn := truckCurrencies[i].lhdn
      outCurrencies.mhdn := truckCurrencies[i].mhdn
      outCurrencies.hhdn := truckCurrencies[i].hhdn
      outCurrencies.lhdt := truckCurrencies[i].lhdt
      outCurrencies.mhdt := truckCurrencies[i].mhdt
      outCurrencies.hhdt := truckCurrencies[i].hhdt 
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
   dim crossBorderFiles[periods.length]
   dim visitorFiles[periods.length]
   dim internalExternalFiles[periods.length]
   dim outFiles[periods.length]


   for i = 1 to periods.length do
      personFiles[i]  = outputDir+"\\tranTrips"+periods[i]+".mtx"
      airportFiles[i] = outputDir+"\\tranAirportTrips"+periods[i]+".mtx"
      crossBorderFiles[i] = outputDir+"\\tranCrossBorderTrips"+periods[i]+".mtx"
      visitorFiles[i] = outputDir+"\\tranVisitorTrips"+periods[i]+".mtx"
      internalExternalFiles[i] = outputDir+"\\tranInternalExternalTrips"+periods[i]+".mtx"
      outFiles[i]  = outputDir+"\\tranTotalTrips"+periods[i]+".mtx"
   end
   
 
   //the following table names have the period appended 
   tableNames = {"WLK_LOC","WLK_EXP","WLK_BRT","WLK_LRT","WLK_CMR","PNR_LOC","PNR_EXP","PNR_BRT","PNR_LRT","PNR_CMR","KNR_LOC","KNR_EXP","KNR_BRT","KNR_LRT","KNR_CMR"}   
    
   
   segments = {"CTRAMP","Airport","Visitor","CrossBorder","Int-Ext","Total"}
   numberSegments = segments.length
   dim statistics[numberSegments]
   dim totalTrips[numberSegments,tableNames.length]

    for i = 1 to periods.length do

      //open person trip matrix currencies
      personMatrix = OpenMatrix(personFiles[i], )                                                               
      personCurrencies = CreateMatrixCurrencies(personMatrix, , , )
     
      //open airport matrix currencies
      airportMatrix = OpenMatrix(airportFiles[i], )                                                               
      airportCurrencies = CreateMatrixCurrencies(airportMatrix, , , )

      //open visitor matrix currencies
      visitorMatrix = OpenMatrix(visitorFiles[i], )                                                               
      visitorCurrencies = CreateMatrixCurrencies(visitorMatrix, , , )

      //open crossBorder matrix currencies
      crossBorderMatrix = OpenMatrix(crossBorderFiles[i], )                                                               
      crossBorderCurrencies = CreateMatrixCurrencies(crossBorderMatrix, , , )

      //open internalExternal matrix currencies
      internalExternalMatrix = OpenMatrix(internalExternalFiles[i], )                                                               
      internalExternalCurrencies = CreateMatrixCurrencies(internalExternalMatrix, , , )
      
      
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
                            + visitorCurrencies.(tableNames[j]+periods[i])
                            + crossBorderCurrencies.(tableNames[j]+periods[i])
                            + internalExternalCurrencies.(tableNames[j]+periods[i])
                              
      end
      
      //for reporting
      statistics[1]        = MatrixStatistics(personMatrix,)
      statistics[2]        = MatrixStatistics(airportMatrix,)
      statistics[3]        = MatrixStatistics(visitorMatrix,)
      statistics[4]        = MatrixStatistics(crossBorderMatrix,)
      statistics[5]        = MatrixStatistics(internalExternalMatrix,)
      statistics[6]        = MatrixStatistics(outMatrix,)
      
      // calculate totals and save in arrays
      for j = 1 to tableNames.length do
      
         for k = 1 to numberSegments do
         
            // Sum the tables in the person trip file
            
            if k<6 then totalTrips[k][j] = statistics[k].(tableNames[j]+periods[i]).Sum else totalTrips[k][j] = statistics[k].(tableNames[j]).Sum
         end
      end

      //write the table for inputs to the report file
      AppendToReportFile(0, "Transit Factoring for Period "+periods[i], {{"Section", "True"}})
      fileColumn = { {"Name", "File"}, {"Percentage Width", 20}, {"Alignment", "Left"}}
      modeColumns = null
      for j = 1 to tableNames.length do
          modeColumns =   modeColumns + { { {"Name", tableNames[j]}, {"Percentage Width", (100-20)/tableNames.length}, {"Alignment", "Left"}, {"Decimals", 0} } }
      end
      columns = {fileColumn} + modeColumns
      AppendTableToReportFile( columns, {{"Title", "Transit Factor Input File Totals"}})

      for j = 1 to numberSegments do
        outRow = null
        for k = 1 to tableNames.length do
            outRow =  outRow  + {totalTrips[j][k] }
        end
        outRow = { segments[j] } + outRow  
        AppendRowToReportFile(outRow,)
    end

    CloseReportFileSection()
      
      
      
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
/***************************************************************************************************************************

Create external-external trip table

****************************************************************************************************************************/
Macro "Create External-External Trip Matrix"
   
   
   shared path, inputDir, outputDir, mxzone, mxext       
                           
  //TODO:  open external-external matrix here
   externalExternalFileName = inputDir+"\\externalExternalTrips.csv" 	    
	 externalExternalMatrixName = outputDir + "\\externalExternalTrips.mtx"
	 
   extExtView = OpenTable("extExt", "CSV", {externalExternalFileName}, {{"Shared", "True"}})
 
 
  opts = {}
  opts.Label = "Trips"
  opts.Type = "Float"
  opts.Tables = {'Trips'}
  
  opts.[File Name] = externalExternalMatrixName
  extMatrix = CreateMatrixFromScratch(externalExternalMatrixName,mxzone,mxzone,opts)
  extCurren = CreateMatrixCurrency(extMatrix,'Trips',,,)

  extCurren :=0
  	
   rec = GetFirstRecord(extExtView+"|", {{"originTaz", "Ascending"}}) 
   while rec <> null do
   
       rec_vals = GetRecordValues(extExtView, rec, {"originTaz","destinationTaz","Trips"})
       
       originTaz = rec_vals[1][2]
       destinationTaz = rec_vals[2][2]
       trips = rec_vals[3][2]
      
       SetMatrixValue(extCurren, i2s(originTaz), i2s(destinationTaz), trips)

    	 rec= GetNextrecord(extExtView+"|",rec ,{{"originTaz", "Ascending"}})
 
   end




   RunMacro("close all" )
   quit:
      Return(1 )
EndMacro

