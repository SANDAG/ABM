Macro "Run SANDAG ABM"
  
   RunMacro("TCB Init")

   shared path, inputDir, outputDir, inputTruckDir, mxzone, mxtap, mgraDataFile,mxext,mxlink,mxrte
 
   // Stop residual Java processes on nodes
   runString = path+"\\bin\\stopABM.cmd"
   ok = RunMacro("TCB Run Command", 1, "Stop Nodes", runString) 

   sample_rate = { 0.2, 0.5, 1.0 }
   max_iterations=sample_rate.length    //number of feedback loops
  
   cpulist={1,2,3,4}
   selink_flag=0   //add by JXu on Nov 29, 2006
   selinkqry_file="\\selectlink_query.txt"   //add by JXu on Nov 29, 2006
   turn_flag=0
   turn_file="\\nodes.txt"
   NumofCPU=1
   saveIntfile=0
   tab_indx=2
   TAB1=1
   TAB2=2

   path = "${workpath}\\${year}"
   mgraDataFile      = "mgra13_based_input${year}.csv"

   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","*********Model Run Starting************"})
   
   saveIntfile=0
   RunCodes=null
   code=0
   newsetup = 0  
   
   path_parts = SplitPath(path)
   path_no_drive = path_parts[2]+path_parts[3]
   drive=path_parts[1]  
   path_forward_slash =  Substitute(path_no_drive, "\\", "/", )
   
   inputDir = path+"\\input"
   outputDir = path+"\\output"
   inputTruckDir = path+"\\input_truck"
 
   SetLogFileName(path+"\\logFiles\\tclog.xml")
   SetReportFileName(path+"\\logFiles\\tcreport.xml")
   
   // copy initial trip tables from input to output folder
   CopyFile(inputDir+"\\trip_EA.mtx", outputDir+"\\trip_EA.mtx")
   CopyFile(inputDir+"\\trip_AM.mtx", outputDir+"\\trip_AM.mtx")
   CopyFile(inputDir+"\\trip_MD.mtx", outputDir+"\\trip_MD.mtx")
   CopyFile(inputDir+"\\trip_PM.mtx", outputDir+"\\trip_PM.mtx")
   CopyFile(inputDir+"\\trip_EV.mtx", outputDir+"\\trip_EV.mtx")
      
   RunMacro("parameters")
	
  // Build highway network
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - run create hwy"})
   ok = RunMacro("TCB Run Macro", 1, "run create hwy",{}) 
   if !ok then goto quit

   // Create transit routes
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - run create all transit"})
   ok = RunMacro("TCB Run Macro", 1, "Create all transit",{}) 
   if !ok then goto quit

   // Factor headways
   ok = RunMacro("TCB Run Macro", 1, "update headways",{})
   if !ok then goto quit


   //Looping
   for iteration = 1 to max_iterations do        

      // Start matrix manager locally
      runString = path+"\\bin\\runMtxMgr.cmd "+drive+" "+path_no_drive
      ok = RunMacro("TCB Run Command", 1, "Start matrix manager", runString)
      if !ok then goto quit 
	
      // Start  JPPF driver 
      runString = path+"\\bin\\runDriver.cmd "+drive+" "+path_no_drive
      ok = RunMacro("TCB Run Command", 1, "Start JPPF Driver", runString)
      if !ok then goto quit  

      // Start HH Manager, and worker nodes
      runString = path+"\\bin\\StartHHAndNodes.cmd "+drive+" "+path_no_drive
      ok = RunMacro("TCB Run Command", 1, "Start HH Manager, JPPF Driver, and nodes", runString)
      if !ok then goto quit  

      // Run highway assignment 
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - hwy assignment"})
      ok = RunMacro("TCB Run Macro", 1, "hwy assignment",{iteration}) 
      if !ok then goto quit
   
      // Skim highway network
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Hwy skim all"})
      ok = RunMacro("TCB Run Macro", 1, "Hwy skim all",{}) 
      if !ok then goto quit
	
      // Skim transit network 
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Build transit skims"})
      ok = RunMacro("TCB Run Macro", 1, "Build transit skims",{}) 
      if !ok then goto quit

      // First move some trip matrices so model will crash if ctramp model doesn't produced csv/mtx files for assignment
      if (iteration > 1) then do
         fromDir = outputDir
         toDir = outputDir+"\\iter"+String(iteration-1)
         //check for directory of output
         if GetDirectoryInfo(toDir, "Directory")=null then do
                CreateDirectory( toDir)   
         end
         status = RunProgram("cmd.exe /c copy "+fromDir+"\\auto*.mtx "+toDir+"\\auto*.mtx",)
         status = RunProgram("cmd.exe /c copy "+fromDir+"\\tran*.mtx "+toDir+"\\tran*.mtx",)
         status = RunProgram("cmd.exe /c copy "+fromDir+"\\nmot*.mtx "+toDir+"\\nmot*.mtx",)
         status = RunProgram("cmd.exe /c copy "+fromDir+"\\othr*.mtx "+toDir+"\\othr*.mtx",)
         status = RunProgram("cmd.exe /c copy "+fromDir+"\\trip*.mtx "+toDir+"\\trip*.mtx",)
 
         status = RunProgram("cmd.exe /c del "+fromDir+"\\auto*.mtx",)
         status = RunProgram("cmd.exe /c del "+fromDir+"\\tran*.mtx",)
         status = RunProgram("cmd.exe /c del "+fromDir+"\\nmot*.mtx",)
         status = RunProgram("cmd.exe /c del "+fromDir+"\\othr*.mtx",)
         status = RunProgram("cmd.exe /c del "+fromDir+"\\trip*.mtx",)
        
      end

      // Run CT-RAMP, airport model, visitor model, cross-border model, internal-external model
      runString = path+"\\bin\\runSandagAbm.cmd "+drive+" "+path_forward_slash +" "+r2s(sample_rate[iteration])+" "+i2s(iteration)
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Java-Run CT-RAMP, airport model, visitor model, cross-border model, internal-external model"+" "+runString})
      ok = RunMacro("TCB Run Command", 1, "Run CT-RAMP", runString)
      if !ok then goto quit  
 
     //Commercial vehicle trip generation
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - run commercial vehicle generation"})
      ok = RunMacro("TCB Run Macro", 1, "Commercial Vehicle Generation",{}) 
      if !ok then goto quit

      //Commercial vehicle trip distribution
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - run commercial vehicle distribution"})
      ok = RunMacro("TCB Run Macro", 1, "Commercial Vehicle Distribution",{}) 
      if !ok then goto quit

      //Commercial vehicle time-of-day
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - run commercial vehicle Time Of Day"})
      ok = RunMacro("TCB Run Macro", 1, "Commercial Vehicle Time Of Day",{}) 
      if !ok then goto quit

      //Run External(U.S.)-Internal Model
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - US to SD External Trip Model"})
      ok = RunMacro("TCB Run Macro", 1, "US to SD External Trip Model",{}) 
      if !ok then goto quit

      //Run Truck Model
      properties = "\\conf\\sandag_abm.properties"
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - truck model"})
      ok = RunMacro("truck model",properties, iteration)
      if !ok then goto quit   

      //Construct trip tables
      RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Create Auto Tables"})
      ok = RunMacro("TCB Run Macro", 1, "Create Auto Tables",{}) 
      if !ok then goto quit

   end

  // Run final highway assignment
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - hwy assignment"})
   ok = RunMacro("TCB Run Macro", 1, "hwy assignment",{4}) 
   if !ok then goto quit

   //Construct transit trip tables
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Create Transit Tables"})
   ok = RunMacro("TCB Run Macro", 1, "Create Transit Tables",{}) 
   if !ok then goto quit
 
   //Run final and only transit assignment
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Assign Transit"})
   ok = RunMacro("TCB Run Macro", 1, "Assign Transit",{}) 
   if !ok then goto quit

   // Skim highway network based on final highway assignment
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Hwy skim all"})
   ok = RunMacro("TCB Run Macro", 1, "Hwy skim all",{}) 
   if !ok then goto quit
	
   // Skim transit network based on final transit assignemnt
   RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Build transit skims"})
   ok = RunMacro("TCB Run Macro", 1, "Build transit skims",{}) 
   if !ok then goto quit
  	
   //Create LUZ skims	
    RunMacro("HwycadLog",{"sandag_abm_master.rsc:","Macro - Create LUZ Skims"})
   ok = RunMacro("TCB Run Macro", 1, "Create LUZ Skims",{}) 
   if !ok then goto quit

   // copy final trip tables from output to input folder as warm start trip tables for next runs
   CopyFile(outputDir+"\\trip_EA.mtx", inputDir+"\\trip_EA.mtx")
   CopyFile(outputDir+"\\trip_AM.mtx", inputDir+"\\trip_AM.mtx")
   CopyFile(outputDir+"\\trip_MD.mtx", inputDir+"\\trip_MD.mtx")
   CopyFile(outputDir+"\\trip_PM.mtx", inputDir+"\\trip_PM.mtx")
   CopyFile(outputDir+"\\trip_EV.mtx", inputDir+"\\trip_EV.mtx")

   // delete trip table files in iteration sub folder.  
   for iteration = 1 to max_iterations-1 do  
      toDir = outputDir+"\\iter"+String(iteration-1)    
      status = RunProgram("cmd.exe /c del "+toDir+"\\auto*.mtx",)
      status = RunProgram("cmd.exe /c del "+toDir+"\\tran*.mtx",)
      status = RunProgram("cmd.exe /c del "+toDir+"\\nmot*.mtx",)
      status = RunProgram("cmd.exe /c del "+toDir+"\\othr*.mtx",)
      status = RunProgram("cmd.exe /c del "+toDir+"\\trip*.mtx",)    
   end

   RunMacro("TCB Closing", ok, "False")
   return(1)
   quit:
      return(0)
EndMacro