/**************************************************************
   CommVehTOD.rsc
 
  TransCAD Macro used to run truck commercial vehicle time-of-day factoring.  The very small truck generation model is based on the Phoenix 
  four-tire truck model documented in the TMIP Quick Response Freight Manual. 
 
 The diurnal factors are taken from the BAYCAST-90 model with adjustments made during calibration to the very
 small truck values to better match counts. 

 Input:   A production/attraction format trip table matrix of daily very small truck trips.

 Output: Five, time-of-day-specific trip table matrices containing very small trucks. 

 See also: (1) CommVehGen.rsc, which applies the generation model.
           (2) CommVehDist.rsc, which applies the distribution model.
           
 authors:  jef (2012 03 11) dto (2011 09 08); dto (2010 08 31); cp (date unknown)


**************************************************************/
Macro "Commercial Vehicle Time Of Day" (scenarioDirectory)
  shared path, inputDir, outputDir       

/*
	RunMacro("TCB Init")

	//inputs
	scenarioDirectory = "d:\\projects\\SANDAG\\AB_Model\\commercial_vehicles"
*/	
	
	commVehTripTable = "commVehTrips.mtx"	

	//outputs
	commVehTODTable = "commVehTODTrips.mtx"
	
	//open input table
	dailyMatrix = OpenMatrix(outputDir + "\\"+commVehTripTable, )
	dailyMC = CreateMatrixCurrency(dailyMatrix, "CommVeh", ,, )

	//create transposed daily trip table     
  tmat = TransposeMatrix(dailyMatrix, {{"File Name", outputDir + "\\"+commVehTripTable+"t"},
     {"Label", "commVehT"},
     {"Type", "Double"},
     {"Sparse", "No"},
     {"Column Major", "No"},
     {"File Based", "No"}})

	transMC = CreateMatrixCurrency(tmat, "commVeh", ,, )

	//create output matrix
	todMatrix = CopyMatrix(dailyMC, {{"File Name", outputDir+"\\"+commVehTODTable},
     {"Label", "CommVehTOD"},
     {"File Based", "Yes"}})

	Opts = null
	Opts.Input.[Input Matrix] =outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "OD Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit
      
	Opts = null
	Opts.Input.[Input Matrix] = outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "EA Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit

	Opts = null
	Opts.Input.[Input Matrix] = outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "AM Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit

	Opts = null
	Opts.Input.[Input Matrix] = outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "MD Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit

	Opts = null
	Opts.Input.[Input Matrix] = outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "PM Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit

	Opts = null
	Opts.Input.[Input Matrix] = outputDir+"\\"+commVehTODTable
	Opts.Input.[New Core] = "EV Trips" 
	ok = RunMacro("TCB Run Operation", "Add Matrix Core", Opts) 
   if !ok then goto quit

	odMC = CreateMatrixCurrency(todMatrix, "OD Trips", ,, )
	eaMC = CreateMatrixCurrency(todMatrix, "EA Trips", ,, )
	amMC = CreateMatrixCurrency(todMatrix, "AM Trips", ,, )
	mdMC = CreateMatrixCurrency(todMatrix, "MD Trips", ,, )
	pmMC = CreateMatrixCurrency(todMatrix, "PM Trips", ,, )
	evMC = CreateMatrixCurrency(todMatrix, "EV Trips", ,, )
	
	odMC := (0.5 * dailyMC) + (0.5 * transMC)

   //     - early AM
   eaMC := 0.0235 * odMC     
   
  
   //     - AM peak
   amMC := 0.1000 * odMC         

   
   //     - midday
   mdMC := 0.5080 * odMC       
   
   //     - PM peak
   pmMC := 0.1980 * odMC        

   
   //     - evening
   evMC := 0.1705 * odMC      
  
   RunMacro("close all")
   quit:
      Return(ok)

EndMacro    
