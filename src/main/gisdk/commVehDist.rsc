/**************************************************************
   CommVehTOD.rsc
 
  TransCAD Macro used to run truck commercial vehicle distribution model.  The very small truck generation model is based on the Phoenix 
  four-tire truck model documented in the TMIP Quick Response Freight Manual. 
 
  A simple gravity model is used to distribute the truck trips.  A blended travel time is used as the impedance measure, 
  specifically the weighted average of the AM travel time (one-third weight) and the midday travel time (two-thirds weight). 

 Input:  (1) Level-of-service matrices for the AM peak period (6 am to 10 am) and midday period (10 am to 3 pm)
             which contain truck-class specific estimates of congested travel time (in minutes) 
         (2) Trip generation results in ASCII format with the following fields 
               (a) TAZ:  zone number; 
               (b) PROD:  very small truck trip productions; 
               (c) ATTR:  very small truck trip attractions; 
         (4) A table of friction factors in ASCII format with the following fields (each 12 columns wide): (a)
             impedance measure (blended travel time); (b) friction factors for very small trucks; 

 Output: (1) A production/attraction trip table matrix of daily class-specific truck trips for very small trucks.
         (2) A blended travel time matrix

 See also: (1) CommVehGen.rsc, which applies the generation model.
           (2) CommVehTOD.rsc, which applies diurnal factors to the daily trips generated here. 

 authors:  jef (2012 03 11) dto (2011 09 08); dto (2010 08 31); cp (date unknown)


**************************************************************/
Macro "Commercial Vehicle Distribution"

  shared path, inputDir, outputDir       

   /* testing
   RunMacro("TCB Init")
   scenarioDirectory = "d:\\projects\\SANDAG\\AB_Model\\commercial_vehicles"
   */
   
   tazCommTripFile = "tazCommVeh.csv"
   amMatrixName = "impdan_AM.mtx"
   amTableName = "*STM_AM (Skim)"
   mdMatrixName = "impdan_MD.mtx"
   mdTableName = "*STM_MD (Skim)"
   
   frictionTable = "commVehFF.csv"
   pa_tb = outputDir+"\\"+tazCommTripFile
   ff_tb = inputDir+"\\"+frictionTable

   //outputs
   blendMatrixName = "blendMatrix.mtx"
   commVehTripTable = "commVehTrips.mtx"   

   //create blended skim
   amMatrix = OpenMatrix(outputDir + "\\"+amMatrixName, )
   amMC = CreateMatrixCurrency(amMatrix, amTableName, "Origin", "Destination", )
   mdMatrix = OpenMatrix(outputDir + "\\"+mdMatrixName, )
   mdMC = CreateMatrixCurrency(mdMatrix, mdTableName, "Origin", "Destination", )

   blendMatrix = CopyMatrix(amMC, {{"File Name", outputDir+"\\"+blendMatrixName},
     {"Label", "AMMDBlend"},
     {"Table", amTableName},
     {"File Based", "Yes"}})

   blendMC = CreateMatrixCurrency(blendMatrix, amTableName, "Origin", "Destination", )

   blendMC := 0.3333*amMC + 0.6666*mdMC

	 //prevent intrazonal
   EvaluateMatrixExpression(blendMC, "99999", null, null,{{"Diagonal","true"}} )


   ff_vw = RunMacro("TCB OpenTable",,, {ff_tb})
   ok = (ff_vw <> null) if !ok then goto quit

   pa_vw = RunMacro("TCB OpenTable",,, {pa_tb})
   ok = (pa_vw <> null) if !ok then goto quit

   //Gravity Application
   Opts = null
   Opts.Input.[PA View Set] = {pa_tb}
   Opts.Field.[Prod Fields] = {pa_vw + ".PROD"}
   Opts.Field.[Attr Fields] = {pa_vw + ".ATTR"}
   Opts.Input.[FF Matrix Currencies] = {{outputDir + "\\" + blendMatrixName, amTableName, "Origin", "Destination"}}
   Opts.Input.[Imp Matrix Currencies] = {{outputDir + "\\" + blendMatrixName, amTableName, "Origin", "Destination"}}
   Opts.Input.[FF Tables] = {{ff_tb}}
   Opts.Input.[KF Matrix Currencies] = {{outputDir + "\\" +blendMatrixName, amTableName, "Origin", "Destination"}}
   Opts.Field.[FF Table Fields] = {ff_vw +".FF"}
   Opts.Field.[FF Table Times] = {ff_vw +".TIME"}
   Opts.Global.[Purpose Names] = {"CommVeh"}
   Opts.Global.Iterations = {50}
   Opts.Global.Convergence = {0.1}
   Opts.Global.[Constraint Type] = {"Double"}
   Opts.Global.[Fric Factor Type] = {"Table"}
   Opts.Global.[A List] = {1}
   Opts.Global.[B List] = {0.3}
   Opts.Global.[C List] = {0.005}
   Opts.Flag.[Use K Factors] = {0}
   Opts.Output.[Output Matrix].Label = "Output Matrix"
   Opts.Output.[Output Matrix].[File Name] = outputDir + "\\" +commVehTripTable
   ok = RunMacro("TCB Run Procedure", "Gravity", Opts)
    
    
  RunMacro("close all")
   quit:
      Return(ok)

EndMacro    
