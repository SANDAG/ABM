Macro "cv toll diversion model"
    shared path, inputDir, outputDir    
/*   RunMacro("TCB Init")
	  //inputs
    path = "D:\\projects\\sandag\\series13\\2012_test"
    inputDir = path+"\\input"
    outputDir = path+"\\output"
	  scenarioDirectory = "D:\\projects\\SANDAG\\series13\\2012_test"
*/  
    // Toll diversion curve settings
    nest_param = 10
    vot = 0.02                                      //(minutes/cent), currently $0.50 a minute
       
    periodName    = {"EA","AM","MD","PM","EV"}      // must be consistent with filename arrays below
    //periodName    = {"AM"}      // must be consistent with filename arrays below
    cvTollFactor = 1                 // cv toll factor
  
    // Loop by time period
    for period = 1 to periodName.length do
      
       // Open cv trips
       fileNameCV = outputDir + "\\commVehTODTrips.mtx"
       m = OpenMatrix(fileNameCV,)
       
          nontollmtx=outputDir+"\\impcv"+"n_"+periodName[period]+".mtx"   // non-toll commercial vehicle skims 
          tollmtx=outputDir+"\\impcv"+"t_"+periodName[period]+".mtx"      // toll commercial vehicle skims 
          OpenMatrix(nontollmtx,)
          OpenMatrix(tollmtx,)
                   
          // Add toll and non-toll matrix
          AddMatrixCore(m, periodName[period]+ " Toll")
          AddMatrixCore(m, periodName[period]+ " NonToll")
          
          // Diversion curve (time is in minutes, cost is in cents)
          // First scale the toll cost since the cost is scaled for SR125
          tollCost = "[Output Matrix:1].[cvt - ITOLL2_"+
                        periodName[period]+"]"
          utility = "(([Output Matrix].[*STM_"+periodName[period]+" (Skim)] - [Output Matrix:1].[*STM_"+periodName[period]+" (Skim)]) - " + 
                      String(vot) + " * " + tollCost + " * " + String(cvTollFactor) + " ) / " + String(nest_param)

          expression = "if(" + tollCost + "!=0) then ( 1 / ( 1 + exp(-" + utility + ") ) ) else " + tollCost

          // Calculate toll matrix
          Opts = null
          Opts.Input.[Matrix Currency]    = {fileNameCV, periodName[period]+ " Toll", "Row ID's", "Col ID's"}
          Opts.Input.[Formula Currencies] = {{nontollmtx, "*CVCST_"+periodName[period], "Origin", "Destination"}, {tollmtx, "*CVCST_"+periodName[period], "Origin", "Destination"}}
          Opts.Global.Method              = 11
          Opts.Global.[Cell Range]        = 2
          Opts.Global.[Expression Text]   = "[" + periodName[period] + " Trips] * " + expression
          Opts.Global.[Formula Labels]    = {"Output Matrix", "Output Matrix:1"}
          Opts.Global.[Force Missing]     = "Yes"
          ok = RunMacro("TCB Run Operation", "Fill Matrices", Opts)
          if !ok then goto quit
          
          // Calculate non-toll matrix
          mc_n = CreateMatrixCurrency(m, periodName[period]+ " NonToll", "Row ID's", "Col ID's",)
          mc_t = CreateMatrixCurrency(m, periodName[period]+ " Toll", "Row ID's", "Col ID's",)
          mc = CreateMatrixCurrency(m, periodName[period]+ " Trips" , "Row ID's", "Col ID's",)
          mc_n := mc - mc_t

    end

   //return 1 if macro completed
    run_ok = 1
    Return(run_ok)
    
    quit:
      Return(ok)
    
EndMacro
                                                                                                                            