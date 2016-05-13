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
   
   //VOT bins for non resident models, 1->3
   votBinEE = 3
   votBinExternalInternal = 3
   votBinCommercialVehicles=3
   
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
   		{
      outputDir+"\\autoInternalExternalTrips_EA_low.mtx",
      outputDir+"\\autoInternalExternalTrips_AM_low.mtx",
      outputDir+"\\autoInternalExternalTrips_MD_low.mtx",
      outputDir+"\\autoInternalExternalTrips_PM_low.mtx",
      outputDir+"\\autoInternalExternalTrips_EV_low.mtx"},
   		{
      outputDir+"\\autoInternalExternalTrips_EA_med.mtx",
      outputDir+"\\autoInternalExternalTrips_AM_med.mtx",
      outputDir+"\\autoInternalExternalTrips_MD_med.mtx",
      outputDir+"\\autoInternalExternalTrips_PM_med.mtx",
      outputDir+"\\autoInternalExternalTrips_EV_med.mtx"
     },
   		{
      outputDir+"\\autoInternalExternalTrips_EA_high.mtx",
      outputDir+"\\autoInternalExternalTrips_AM_high.mtx",
      outputDir+"\\autoInternalExternalTrips_MD_high.mtx",
      outputDir+"\\autoInternalExternalTrips_PM_high.mtx",
      outputDir+"\\autoInternalExternalTrips_EV_high.mtx"
     }
  }   	
   	visitorTables = {
      {
      	outputDir+"\\autoVisitorTrips_EA_low.mtx",
        outputDir+"\\autoVisitorTrips_AM_low.mtx",
        outputDir+"\\autoVisitorTrips_MD_low.mtx",
        outputDir+"\\autoVisitorTrips_PM_low.mtx",
        outputDir+"\\autoVisitorTrips_EV_low.mtx"},
     {
      	outputDir+"\\autoVisitorTrips_EA_med.mtx",
        outputDir+"\\autoVisitorTrips_AM_med.mtx",
        outputDir+"\\autoVisitorTrips_MD_med.mtx",
        outputDir+"\\autoVisitorTrips_PM_med.mtx",
        outputDir+"\\autoVisitorTrips_EV_med.mtx"},
     {
      	outputDir+"\\autoVisitorTrips_EA_high.mtx",
        outputDir+"\\autoVisitorTrips_AM_high.mtx",
        outputDir+"\\autoVisitorTrips_MD_high.mtx",
        outputDir+"\\autoVisitorTrips_PM_high.mtx",
        outputDir+"\\autoVisitorTrips_EV_high.mtx"}
    }  		
   	crossBorderTables = {
   		{
      outputDir+"\\autoCrossBorderTrips_EA_low.mtx",
      outputDir+"\\autoCrossBorderTrips_AM_low.mtx",
      outputDir+"\\autoCrossBorderTrips_MD_low.mtx",
      outputDir+"\\autoCrossBorderTrips_PM_low.mtx",
      outputDir+"\\autoCrossBorderTrips_EV_low.mtx"},
  		{
      outputDir+"\\autoCrossBorderTrips_EA_med.mtx",
      outputDir+"\\autoCrossBorderTrips_AM_med.mtx",
      outputDir+"\\autoCrossBorderTrips_MD_med.mtx",
      outputDir+"\\autoCrossBorderTrips_PM_med.mtx",
      outputDir+"\\autoCrossBorderTrips_EV_med.mtx"},
  		{
      outputDir+"\\autoCrossBorderTrips_EA_high.mtx",
      outputDir+"\\autoCrossBorderTrips_AM_high.mtx",
      outputDir+"\\autoCrossBorderTrips_MD_high.mtx",
      outputDir+"\\autoCrossBorderTrips_PM_high.mtx",
      outputDir+"\\autoCrossBorderTrips_EV_high.mtx"}
    }    
        
   airportTables = {
      {
      outputDir+"\\autoAirportTrips_EA_low.mtx",
      outputDir+"\\autoAirportTrips_AM_low.mtx",
      outputDir+"\\autoAirportTrips_MD_low.mtx",
      outputDir+"\\autoAirportTrips_PM_low.mtx",
      outputDir+"\\autoAirportTrips_EV_low.mtx"},
     {
     	outputDir+"\\autoAirportTrips_EA_med.mtx",
      outputDir+"\\autoAirportTrips_AM_med.mtx",
      outputDir+"\\autoAirportTrips_MD_med.mtx",
      outputDir+"\\autoAirportTrips_PM_med.mtx",
      outputDir+"\\autoAirportTrips_EV_med.mtx"},
     {
     	outputDir+"\\autoAirportTrips_EA_high.mtx",
      outputDir+"\\autoAirportTrips_AM_high.mtx",
      outputDir+"\\autoAirportTrips_MD_high.mtx",
      outputDir+"\\autoAirportTrips_PM_high.mtx",
      outputDir+"\\autoAirportTrips_EV_high.mtx"}
    }
   personTables = {
      {
      outputDir+"\\autoTrips_EA_low.mtx",
      outputDir+"\\autoTrips_AM_low.mtx",
      outputDir+"\\autoTrips_MD_low.mtx",
      outputDir+"\\autoTrips_PM_low.mtx",
      outputDir+"\\autoTrips_EV_low.mtx"},
      {
      outputDir+"\\autoTrips_EA_med.mtx",
      outputDir+"\\autoTrips_AM_med.mtx",
      outputDir+"\\autoTrips_MD_med.mtx",
      outputDir+"\\autoTrips_PM_med.mtx",
      outputDir+"\\autoTrips_EV_med.mtx"},
      {
      outputDir+"\\autoTrips_EA_high.mtx",
      outputDir+"\\autoTrips_AM_high.mtx",
      outputDir+"\\autoTrips_MD_high.mtx",
      outputDir+"\\autoTrips_PM_high.mtx",
      outputDir+"\\autoTrips_EV_high.mtx"}
   }
   //the following table names have the period appended 
   CTRampMatrices = {"SOV_GP","SOV_PAY","SR2_GP","SR2_HOV","SR2_PAY","SR3_GP","SR3_HOV","SR3_PAY"}   

	//output files                                                                                                                             
	outMatrixNames = {"Trip"+periods[1]+"_VOT.mtx", "Trip"+periods[2]+"_VOT.mtx", "Trip"+periods[3]+"_VOT.mtx", "Trip"+periods[4]+"_VOT.mtx", "Trip"+periods[5]+"_VOT.mtx"}                                                                                                   
	outTableNames = {"SOV_GP_LOW", "SOV_PAY_LOW", "SR2_GP_LOW","SR2_HOV_LOW", "SR2_PAY_LOW", "SR3_GP_LOW","SR3_HOV_LOW","SR3_PAY_LOW",
		               "SOV_GP_MED", "SOV_PAY_MED", "SR2_GP_MED","SR2_HOV_MED", "SR2_PAY_MED", "SR3_GP_MED","SR3_HOV_MED","SR3_PAY_MED",
		               "SOV_GP_HIGH", "SOV_PAY_HIGH", "SR2_GP_HIGH","SR2_HOV_HIGH", "SR2_PAY_HIGH", "SR3_GP_HIGH","SR3_HOV_HIGH","SR3_PAY_HIGH",
		               "lhdn","mhdn","hhdn","lhdt","mhdt","hhdt"}

   commVehTable = outputDir+"\\commVehTODTrips.mtx"
   commVehMatrices = {"EA NonToll","AM NonToll","MD NonToll","PM NonToll","EV NonToll","EA Toll","AM Toll","MD Toll","PM Toll","EV Toll"}

	// create comm vehicle matrix currencies
   commVehMatrix = OpenMatrix(commVehTable, )                                                               
   commVehCurrencies = CreateMatrixCurrencies(commVehMatrix, , , )


   for i = 1 to periods.length do

      //open person trip matrix currencies
      personMatrixLow = OpenMatrix(personTables[1][i], )                                                               
      personCurrenciesLow = CreateMatrixCurrencies(personMatrixLow, , , )
        
      personMatrixMed = OpenMatrix(personTables[2][i], )                                                               
      personCurrenciesMed = CreateMatrixCurrencies(personMatrixMed, , , )
    
      personMatrixHigh = OpenMatrix(personTables[3][i], )                                                               
      personCurrenciesHigh = CreateMatrixCurrencies(personMatrixHigh, , , )
      
      totalOutMatrices = CTRampMatrices.length * 3
        
      counter = 0
 	    dim curr_array[totalOutMatrices]
	    for j = 1 to totalOutMatrices do
	      counter = counter + 1
        curr_array[j] = CreateMatrixCurrency(personMatrixLow, CTRampMatrices[counter]+periods[i], ,, )
	      if counter = CTRampMatrices.length then counter=0
	   
	   end

     //open internal-external matrix currencies
      internalExternalMatrixLow = OpenMatrix(internalExternalTables[1][i], )                                                               
	    internalExternalCurrenciesLow = CreateMatrixCurrencies(internalExternalMatrixLow, , , )

     internalExternalMatrixMed = OpenMatrix(internalExternalTables[2][i], )                                                               
	    internalExternalCurrenciesMed = CreateMatrixCurrencies(internalExternalMatrixMed, , , )

     internalExternalMatrixHigh = OpenMatrix(internalExternalTables[3][i], )                                                               
	    internalExternalCurrenciesHigh = CreateMatrixCurrencies(internalExternalMatrixHigh, , , )

     //open airport matrix currencies
      airportMatrixLow = OpenMatrix(airportTables[1][i], )                                                               
	    airportCurrenciesLow = CreateMatrixCurrencies(airportMatrixLow, , , )

      airportMatrixMed = OpenMatrix(airportTables[2][i], )                                                               
	    airportCurrenciesMed = CreateMatrixCurrencies(airportMatrixMed, , , )

      airportMatrixHigh = OpenMatrix(airportTables[3][i], )                                                               
	    airportCurrenciesHigh = CreateMatrixCurrencies(airportMatrixHigh, , , )

     //open visitor matrix currencies
      visitorMatrixLow = OpenMatrix(visitorTables[1][i], )                                                               
	    visitorCurrenciesLow = CreateMatrixCurrencies(visitorMatrixLow, , , )

      visitorMatrixMed = OpenMatrix(visitorTables[2][i], )                                                               
	    visitorCurrenciesMed = CreateMatrixCurrencies(visitorMatrixMed, , , )

      visitorMatrixHigh = OpenMatrix(visitorTables[3][i], )                                                               
	    visitorCurrenciesHigh = CreateMatrixCurrencies(visitorMatrixHigh, , , )

     //open cross border matrix currencies
      crossBorderMatrixLow = OpenMatrix(crossBorderTables[1][i], )                                                               
	    crossBorderCurrenciesLow = CreateMatrixCurrencies(crossBorderMatrixLow, , , )

      crossBorderMatrixMed = OpenMatrix(crossBorderTables[2][i], )                                                               
	    crossBorderCurrenciesMed = CreateMatrixCurrencies(crossBorderMatrixMed, , , )

      crossBorderMatrixHigh = OpenMatrix(crossBorderTables[3][i], )                                                               
	    crossBorderCurrenciesHigh = CreateMatrixCurrencies(crossBorderMatrixHigh, , , )

  
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

//LOW VOT
      // calculate output matrices
      outCurrencies.SOV_GP_LOW :=Nz(outCurrencies.SOV_GP_LOW)
      outCurrencies.SOV_GP_LOW :=  Nz(personCurrenciesLow.("SOV_GP"+periods[i]))
                                 + Nz(internalExternalCurrenciesLow.("SOV_GP"+periods[i]))
                                 + Nz(crossBorderCurrenciesLow.("SOV_GP"+periods[i]))  
                                 + Nz(airportCurrenciesLow.("SOV_GP"+periods[i]))
                                 + Nz(visitorCurrenciesLow.("SOV_GP"+periods[i])) 
                           
      if votBinExternalInternal=1 then outCurrencies.SOV_GP_LOW := outCurrencies.SOV_GP_LOW + Nz(externalInternalWrkCurrencies.("DAN")) + Nz(externalInternalNonCurrencies.("DAN"))
      if votBinExternalExternal=1 then outCurrencies.SOV_GP_LOW := outCurrencies.SOV_GP_LOW + (Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[1])
      if votBinCommercialVehicles=1 then outCurrencies.SOV_GP_LOW := outCurrencies.SOV_GP_LOW + Nz(commVehCurrencies.(commVehMatrices[i])) 
                       
      outCurrencies.SOV_PAY_LOW :=Nz(outCurrencies.SOV_PAY_LOW)
      outCurrencies.SOV_PAY_LOW := Nz(personCurrenciesLow.("SOV_PAY"+periods[i])) 
                            + Nz(internalExternalCurrenciesLow.("SOV_PAY"+periods[i])) 
                            + Nz(crossBorderCurrenciesLow.("SOV_PAY"+periods[i]))   
                            + Nz(airportCurrenciesLow.("SOV_PAY"+periods[i]))
                            + Nz(visitorCurrenciesLow.("SOV_PAY"+periods[i]))
    
      if votBinExternalInternal=1 then outCurrencies.SOV_PAY_LOW := outCurrencies.SOV_PAY_LOW + Nz(externalInternalWrkCurrencies.("DAT")) + Nz(externalInternalNonCurrencies.("DAT"))
      if votBinCommercialVehicles=1 then outCurrencies.SOV_PAY_LOW := outCurrencies.SOV_PAY_LOW + Nz(commVehCurrencies.(commVehMatrices[i+5]))

      outCurrencies.SR2_GP_LOW :=Nz(outCurrencies.SR2_GP_LOW)
      outCurrencies.SR2_GP_LOW := Nz(personCurrenciesLow.("SR2_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesLow.("SR2_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesLow.("SR2_GP"+periods[i]))   
                            + Nz(airportCurrenciesLow.("SR2_GP"+periods[i])) 
                            + Nz(visitorCurrenciesLow.("SR2_GP"+periods[i])) 
                              
      outCurrencies.SR2_HOV_LOW :=Nz(outCurrencies.SR2_HOV_LOW)  
      outCurrencies.SR2_HOV_LOW := Nz(personCurrenciesLow.("SR2_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesLow.("SR2_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesLow.("SR2_HOV"+periods[i]))                           
                            + Nz(airportCurrenciesLow.("SR2_HOV"+periods[i])) 
                            + Nz(visitorCurrenciesLow.("SR2_HOV"+periods[i])) 
                        
     if votBinExternalInternal=1 then outCurrencies.SR2_HOV_LOW := outCurrencies.SR2_HOV_LOW  + Nz(externalInternalWrkCurrencies.("S2N")) + Nz(externalInternalNonCurrencies.("S2N"))
     if votBinExternalExternal=1 then outCurrencies.SR2_HOV_LOW := outCurrencies.SR2_HOV_LOW + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[2]
 
      outCurrencies.SR2_PAY_LOW :=Nz(outCurrencies.SR2_PAY_LOW)
      outCurrencies.SR2_PAY_LOW := Nz(personCurrenciesLow.("SR2_PAY"+periods[i])) 
                           + Nz(internalExternalCurrenciesLow.("SR2_PAY"+periods[i])) 
                           + Nz(crossBorderCurrenciesLow.("SR2_PAY"+periods[i]))     
                           + Nz(airportCurrenciesLow.("SR2_PAY"+periods[i])) 
                           + Nz(visitorCurrenciesLow.("SR2_PAY"+periods[i])) 

      if votBinExternalInternal=1 then outCurrencies.SR2_PAY_LOW := outCurrencies.SR2_PAY_LOW + Nz(externalInternalWrkCurrencies.("S2T"))   + Nz(externalInternalNonCurrencies.("S2T"))

      outCurrencies.SR3_GP_LOW :=Nz(outCurrencies.SR3_GP_LOW)
      outCurrencies.SR3_GP_LOW := Nz(personCurrenciesLow.("SR3_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesLow.("SR3_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesLow.("SR3_GP"+periods[i]))      
                            + Nz(airportCurrenciesLow.("SR3_GP"+periods[i])) 
                            + Nz(visitorCurrenciesLow.("SR3_GP"+periods[i])) 

      outCurrencies.SR3_HOV_LOW :=Nz(outCurrencies.SR3_HOV_LOW) 
      outCurrencies.SR3_HOV_LOW := Nz(personCurrenciesLow.("SR3_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesLow.("SR3_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesLow.("SR3_HOV"+periods[i]))     
                            + Nz(airportCurrenciesLow.("SR3_HOV"+periods[i])) 
                            + Nz(visitorCurrenciesLow.("SR3_HOV"+periods[i]))
      
      if votBinExternalInternal=1 then outCurrencies.SR3_HOV_LOW := outCurrencies.SR3_HOV_LOW  + Nz(externalInternalWrkCurrencies.("S3N")) + Nz(externalInternalNonCurrencies.("S3N"))
      if votBinExternalExternal=1 then outCurrencies.SR3_HOV_LOW := outCurrencies.SR3_HOV_LOW + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[3]

      outCurrencies.SR3_PAY_LOW :=Nz(outCurrencies.SR3_PAY_LOW)
      outCurrencies.SR3_PAY_LOW := Nz(personCurrenciesLow.("SR3_PAY"+periods[i])) 
                              + Nz(internalExternalCurrenciesLow.("SR3_PAY"+periods[i])) 
                              + Nz(crossBorderCurrenciesLow.("SR3_PAY"+periods[i])) 
                              + Nz(airportCurrenciesLow.("SR3_PAY"+periods[i]))   
                              + Nz(visitorCurrenciesLow.("SR3_PAY"+periods[i])) 

       if votBinExternalInternal=1 then outCurrencies.SR3_PAY_LOW := outCurrencies.SR3_PAY_LOW  + Nz(externalInternalWrkCurrencies.("S3T"))   + Nz(externalInternalNonCurrencies.("S3T"))
    
//MED VOT
      // calculate output matrices
      outCurrencies.SOV_GP_MED :=Nz(outCurrencies.SOV_GP_MED)
      outCurrencies.SOV_GP_MED :=  Nz(personCurrenciesMed.("SOV_GP"+periods[i]))
                                 + Nz(internalExternalCurrenciesMed.("SOV_GP"+periods[i])) 
                                 + Nz(crossBorderCurrenciesMed.("SOV_GP"+periods[i])) 
                                 + Nz(airportCurrenciesMed.("SOV_GP"+periods[i])) 
                                 + Nz(visitorCurrenciesMed.("SOV_GP"+periods[i])) 
     
      if votBinExternalInternal=2 then outCurrencies.SOV_GP_MED := outCurrencies.SOV_GP_MED + Nz(externalInternalWrkCurrencies.("DAN")) + Nz(externalInternalNonCurrencies.("DAN"))
      if votBinExternalExternal=2 then outCurrencies.SOV_GP_MED := outCurrencies.SOV_GP_MED + (Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[1])
      if votBinCommercialVehicles=2 then outCurrencies.SOV_GP_MED := outCurrencies.SOV_GP_MED + Nz(commVehCurrencies.(commVehMatrices[i])) 
                       
      outCurrencies.SOV_PAY_MED :=Nz(outCurrencies.SOV_PAY_MED)
      outCurrencies.SOV_PAY_MED := Nz(personCurrenciesMed.("SOV_PAY"+periods[i])) 
                            + Nz(internalExternalCurrenciesMed.("SOV_PAY"+periods[i])) 
                            + Nz(crossBorderCurrenciesMed.("SOV_PAY"+periods[i]))       
                            + Nz(airportCurrenciesMed.("SOV_PAY"+periods[i])) 
                            + Nz(visitorCurrenciesMed.("SOV_PAY"+periods[i])) 
 
      if votBinExternalInternal=2 then outCurrencies.SOV_PAY_MED := outCurrencies.SOV_PAY_MED + Nz(externalInternalWrkCurrencies.("DAT")) + Nz(externalInternalNonCurrencies.("DAT"))
      if votBinCommercialVehicles=2 then outCurrencies.SOV_PAY_MED := outCurrencies.SOV_PAY_MED + Nz(commVehCurrencies.(commVehMatrices[i+5]))

      outCurrencies.SR2_GP_MED :=Nz(outCurrencies.SR2_GP_MED)
      outCurrencies.SR2_GP_MED := Nz(personCurrenciesMed.("SR2_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesMed.("SR2_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesMed.("SR2_GP"+periods[i]))    
                            + Nz(airportCurrenciesMed.("SR2_GP"+periods[i]))
                            + Nz(visitorCurrenciesMed.("SR2_GP"+periods[i]))
 
      outCurrencies.SR2_HOV_MED :=Nz(outCurrencies.SR2_HOV_MED)  
      outCurrencies.SR2_HOV_MED := Nz(personCurrenciesMed.("SR2_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesMed.("SR2_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesMed.("SR2_HOV"+periods[i]))    
                            + Nz(airportCurrenciesMed.("SR2_HOV"+periods[i])) 
                            + Nz(visitorCurrenciesMed.("SR2_HOV"+periods[i])) 
                        
     if votBinExternalInternal=2 then outCurrencies.SR2_HOV_MED := outCurrencies.SR2_HOV_MED  + Nz(externalInternalWrkCurrencies.("S2N")) + Nz(externalInternalNonCurrencies.("S2N"))
     if votBinExternalExternal=2 then outCurrencies.SR2_HOV_MED := outCurrencies.SR2_HOV_MED + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[2]
 
      outCurrencies.SR2_PAY_MED :=Nz(outCurrencies.SR2_PAY_MED)
      outCurrencies.SR2_PAY_MED := Nz(personCurrenciesMed.("SR2_PAY"+periods[i])) 
                           + Nz(internalExternalCurrenciesMed.("SR2_PAY"+periods[i])) 
                           + Nz(crossBorderCurrenciesMed.("SR2_PAY"+periods[i]))        
                           + Nz(airportCurrenciesMed.("SR2_PAY"+periods[i])) 
                           + Nz(visitorCurrenciesMed.("SR2_PAY"+periods[i]))         
 
      if votBinExternalInternal=2 then outCurrencies.SR2_PAY_MED := outCurrencies.SR2_PAY_MED + Nz(externalInternalWrkCurrencies.("S2T"))   + Nz(externalInternalNonCurrencies.("S2T"))

      outCurrencies.SR3_GP_MED :=Nz(outCurrencies.SR3_GP_MED)
      outCurrencies.SR3_GP_MED := Nz(personCurrenciesMed.("SR3_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesMed.("SR3_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesMed.("SR3_GP"+periods[i]))       
                             + Nz(airportCurrenciesMed.("SR3_GP"+periods[i])) 
                             + Nz(visitorCurrenciesMed.("SR3_GP"+periods[i])) 
      
      outCurrencies.SR3_HOV_MED :=Nz(outCurrencies.SR3_HOV_MED) 
      outCurrencies.SR3_HOV_MED := Nz(personCurrenciesMed.("SR3_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesMed.("SR3_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesMed.("SR3_HOV"+periods[i]))     
                            + Nz(airportCurrenciesMed.("SR3_HOV"+periods[i]))  
                            + Nz(visitorCurrenciesMed.("SR3_HOV"+periods[i])) 
     
      if votBinExternalInternal=2 then outCurrencies.SR3_HOV_MED := outCurrencies.SR3_HOV_MED  + Nz(externalInternalWrkCurrencies.("S3N")) + Nz(externalInternalNonCurrencies.("S3N"))
      if votBinExternalExternal=2 then outCurrencies.SR3_HOV_MED := outCurrencies.SR3_HOV_MED + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[3]

      outCurrencies.SR3_PAY_MED :=Nz(outCurrencies.SR3_PAY_MED)
      outCurrencies.SR3_PAY_MED := Nz(personCurrenciesMed.("SR3_PAY"+periods[i])) 
                              + Nz(internalExternalCurrenciesMed.("SR3_PAY"+periods[i])) 
                              + Nz(crossBorderCurrenciesMed.("SR3_PAY"+periods[i]))    
                              + Nz(airportCurrenciesMed.("SR3_PAY"+periods[i]))  
                              + Nz(visitorCurrenciesMed.("SR3_PAY"+periods[i]))

      if votBinExternalInternal=2 then outCurrencies.SR3_PAY_MED := outCurrencies.SR3_PAY_MED  + Nz(externalInternalWrkCurrencies.("S3T"))   + Nz(externalInternalNonCurrencies.("S3T"))

//HIGH VOT
      // calculate output matrices
      outCurrencies.SOV_GP_HIGH :=Nz(outCurrencies.SOV_GP_HIGH)
      outCurrencies.SOV_GP_HIGH :=  Nz(personCurrenciesHigh.("SOV_GP"+periods[i]))
                                 + Nz(internalExternalCurrenciesHigh.("SOV_GP"+periods[i])) 
                                 + Nz(crossBorderCurrenciesHigh.("SOV_GP"+periods[i]))   
                                  + Nz(airportCurrenciesHigh.("SOV_GP"+periods[i])) 
                                  + Nz(visitorCurrenciesHigh.("SOV_GP"+periods[i])) 
                           
      if votBinExternalInternal=3 then outCurrencies.SOV_GP_HIGH := outCurrencies.SOV_GP_HIGH + Nz(externalInternalWrkCurrencies.("DAN")) + Nz(externalInternalNonCurrencies.("DAN"))
      if votBinExternalExternal=3 then outCurrencies.SOV_GP_HIGH := outCurrencies.SOV_GP_HIGH + (Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[1])
      if votBinCommercialVehicles=3 then outCurrencies.SOV_GP_HIGH := outCurrencies.SOV_GP_HIGH + Nz(commVehCurrencies.(commVehMatrices[i])) 
                       
      outCurrencies.SOV_PAY_HIGH :=Nz(outCurrencies.SOV_PAY_HIGH)
      outCurrencies.SOV_PAY_HIGH := Nz(personCurrenciesHigh.("SOV_PAY"+periods[i])) 
                            + Nz(internalExternalCurrenciesHigh.("SOV_PAY"+periods[i])) 
                            + Nz(crossBorderCurrenciesHigh.("SOV_PAY"+periods[i]))       
                            + Nz(airportCurrenciesHigh.("SOV_PAY"+periods[i])) 
                            + Nz(visitorCurrenciesHigh.("SOV_PAY"+periods[i])) 
 
      if votBinExternalInternal=3 then outCurrencies.SOV_PAY_HIGH := outCurrencies.SOV_PAY_HIGH + Nz(externalInternalWrkCurrencies.("DAT")) + Nz(externalInternalNonCurrencies.("DAT"))
      if votBinCommercialVehicles=3 then outCurrencies.SOV_PAY_HIGH := outCurrencies.SOV_PAY_HIGH + Nz(commVehCurrencies.(commVehMatrices[i+5]))

      outCurrencies.SR2_GP_HIGH :=Nz(outCurrencies.SR2_GP_HIGH)
      outCurrencies.SR2_GP_HIGH := Nz(personCurrenciesHigh.("SR2_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesHigh.("SR2_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesHigh.("SR2_GP"+periods[i]))    
                            + Nz(airportCurrenciesHigh.("SR2_GP"+periods[i])) 
                            + Nz(visitorCurrenciesHigh.("SR2_GP"+periods[i])) 
 
      outCurrencies.SR2_HOV_HIGH :=Nz(outCurrencies.SR2_HOV_HIGH)  
      outCurrencies.SR2_HOV_HIGH := Nz(personCurrenciesHigh.("SR2_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesHigh.("SR2_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesHigh.("SR2_HOV"+periods[i]))   
                            + Nz(airportCurrenciesHigh.("SR2_HOV"+periods[i])) 
                            + Nz(visitorCurrenciesHigh.("SR2_HOV"+periods[i])) 
                        
     if votBinExternalInternal=3 then outCurrencies.SR2_HOV_HIGH := outCurrencies.SR2_HOV_HIGH  + Nz(externalInternalWrkCurrencies.("S2N")) + Nz(externalInternalNonCurrencies.("S2N"))
     if votBinExternalExternal=3 then outCurrencies.SR2_HOV_HIGH := outCurrencies.SR2_HOV_HIGH + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[2]
 
      outCurrencies.SR2_PAY_HIGH :=Nz(outCurrencies.SR2_PAY_HIGH)
      outCurrencies.SR2_PAY_HIGH := Nz(personCurrenciesHigh.("SR2_PAY"+periods[i])) 
                           + Nz(internalExternalCurrenciesHigh.("SR2_PAY"+periods[i])) 
                           + Nz(crossBorderCurrenciesHigh.("SR2_PAY"+periods[i]))    
                           + Nz(airportCurrenciesHigh.("SR2_PAY"+periods[i])) 
                           + Nz(visitorCurrenciesHigh.("SR2_PAY"+periods[i])) 
   
      if votBinExternalInternal=3 then outCurrencies.SR2_PAY_HIGH := outCurrencies.SR2_PAY_HIGH + Nz(externalInternalWrkCurrencies.("S2T"))   + Nz(externalInternalNonCurrencies.("S2T"))

      outCurrencies.SR3_GP_HIGH :=Nz(outCurrencies.SR3_GP_HIGH)
      outCurrencies.SR3_GP_HIGH := Nz(personCurrenciesHigh.("SR3_GP"+periods[i])) 
                            + Nz(internalExternalCurrenciesHigh.("SR3_GP"+periods[i])) 
                            + Nz(crossBorderCurrenciesHigh.("SR3_GP"+periods[i])) 
                            + Nz(airportCurrencies.("SR3_GP"+periods[i])) 
                            + Nz(visitorCurrencies.("SR3_GP"+periods[i]))
                             
      outCurrencies.SR3_HOV_HIGH :=Nz(outCurrencies.SR3_HOV_HIGH) 
      outCurrencies.SR3_HOV_HIGH := Nz(personCurrenciesHigh.("SR3_HOV"+periods[i])) 
                            + Nz(internalExternalCurrenciesHigh.("SR3_HOV"+periods[i])) 
                            + Nz(crossBorderCurrenciesHigh.("SR3_HOV"+periods[i]))  
                            + Nz(airportCurrenciesHigh.("SR3_HOV"+periods[i]))  
                            + Nz(visitorCurrenciesHigh.("SR3_HOV"+periods[i])) 
   
   
      if votBinExternalInternal=3 then outCurrencies.SR3_HOV_HIGH := outCurrencies.SR3_HOV_HIGH  + Nz(externalInternalWrkCurrencies.("S3N")) + Nz(externalInternalNonCurrencies.("S3N"))
      if votBinExternalExternal=3 then outCurrencies.SR3_HOV_HIGH := outCurrencies.SR3_HOV_HIGH + Nz(externalExternalCurrency) * externalExternalDiurnalFactors[i] * externalExternalOccupancyFactors[3]

      outCurrencies.SR3_PAY_HIGH :=Nz(outCurrencies.SR3_PAY_HIGH)
      outCurrencies.SR3_PAY_HIGH := Nz(personCurrenciesHigh.("SR3_PAY"+periods[i])) 
                              + Nz(internalExternalCurrenciesHigh.("SR3_PAY"+periods[i])) 
                              + Nz(crossBorderCurrenciesHigh.("SR3_PAY"+periods[i]))   
                              + Nz(airportCurrenciesHigh.("SR3_PAY"+periods[i]))   
                              + Nz(visitorCurrenciesHigh.("SR3_PAY"+periods[i])) 

      if votBinExternalInternal=3 then outCurrencies.SR3_PAY_HIGH := outCurrencies.SR3_PAY_HIGH  + Nz(externalInternalWrkCurrencies.("S3T"))   + Nz(externalInternalNonCurrencies.("S3T"))
   
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
  
  
     shared path, inputDir, outputDir, inputTruckDir, mxzone, mxtap, mxext,mxlink,mxrte    
  
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
	dim zones[mxzone]
	for i = 1 to zones.length do
	   zones[i]=i2s(i+mxext)
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

