/*********************************************************************************
Transit Assignment
 
  input files: transitrt.rts
     CT-RAMP trip tables ("tranTrips_period.mtx", where period = EA, AM, MD, PM, and NT)
        each file has 15 cores, named as follows:  ACC_LHM_PER
        where:
          ACC = access mode - WLK,PNR,KNR
          LHM = line-haul mode - LOC,EXP,BRT,LRT,CMR
          PER = period - EA, AM, MD, PM, NT
     Transit networks (localpk.tnw,localop,tnw,prempk.tnw,premop.tnw)
		 Transit route file (transitrt.rts)
  output files:
    75 flow bin file (3 access modes * 5 line-haul modes * 5 time periods)
    75 walk bin file 
    75 onoff bin file 
    75 collapsed onoff files in both binary and csv format
    
*********************************************************************************/

Macro "Assign Transit" (args)

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      
  shared path,inputDir, outputDir, mxtap
 
iteration = args[1]
rt_file=outputDir + "\\transitrt.rts"

periodName = {"_EA", "_AM", "_MD", "_PM", "_EV"}

matrixCore = {
  "WLK_LOC",
  "WLK_EXP",
  "WLK_BRT",
  "WLK_LRT",
  "WLK_CMR",
  "PNR_LOC",
  "PNR_EXP",
  "PNR_BRT",
  "PNR_LRT",
  "PNR_CMR",
  "KNR_LOC",
  "KNR_EXP",
  "KNR_BRT",
  "KNR_LRT",
  "KNR_CMR" }

   network={"locl","prem","prem","prem","prem","locl","prem","prem","prem","prem","locl","prem","prem","prem","prem"}
   dim onOffTables[matrixCore.length * periodName.length]

  selinkqry = inputDir + "\\"+"sellink_transit.qry"
  if GetFileInfo(selinkqry) <> null then  sellink_flag = 1 //select link analysis is only for last iteration (4)
       
   i = 0
   k = 1
   for per = 1 to periodName.length do
     for mat = 1 to matrixCore.length do
  
        i = i + 1     
     
        networkFile = outputDir+"\\"+network[mat]+periodName[per]+".tnw"
        matrixFile =  outputDir+"\\tranTotalTrips"+periodName[per]+".mtx"
        matrixName =  matrixCore[mat]
        flowFile =    outputDir+"\\flow"+matrixCore[mat]+periodName[per]+".bin"
        walkFile =    outputDir+"\\ntl"+matrixCore[mat]+periodName[per]+".bin"
        onOffFile =   outputDir+"\\ono"+matrixCore[mat]+periodName[per]+".bin"
        aggFile =     outputDir+"\\agg"+matrixCore[mat]+periodName[per]+".bin"

        onOffTables[k] = onOffFile
        k = k +1

        if sellink_flag = 1 & iteration = 4 then sellkmtx =  outputDir+"\\trn_sellink"+matrixCore[mat]+periodName[per]+".mtx"

       
        // STEP 1: Transit Assignment
        Opts = null
        Opts.Input.[Transit RS] = rt_file
        Opts.Input.Network = networkFile
        Opts.Input.[OD Matrix Currency] = {matrixFile,matrixName,,}  

        Opts.Output.[Flow Table] = flowFile
        Opts.Output.[Walk Flow Table] = walkFile
        Opts.Output.[OnOff Table] = onOffFile
        Opts.Output.[Aggre Table] = aggFile
        Opts.Flag.[Do Maximum Fare] = 1 //added for 4.8 build 401
        if sellink_flag = 1 & iteration = 4 then do
                Opts.Flag.critFlag = 1
	        Opts.Global.[Critical Query File] = selinkqry
	        Opts.Output.[Critical Matrix].Label = "Critical Matrix"
	        Opts.Output.[Critical Matrix].[File Name] = sellkmtx
        end 
       RunMacro("HwycadLog",{"trassigns.rsc: transit assigns","Transit Assignment PF: "+matrixCore[mat]+periodName[per]})
       ok = RunMacro("TCB Run Procedure", (per*100+mat), "Transit Assignment PF", Opts)
           
       if !ok then goto quit    
	
     end
   end
   
   properties = "\\conf\\sandag_abm.properties" 
   collapseOnOffByRoute = RunMacro("read properties",properties,"RunModel.collapseOnOffByRoute", "S")
   if collapseOnOffByRoute = "true" then do
	   ok = RunMacro("Collapse OnOffs By Route", onOffTables, rt_file)
	   if !ok then goto quit
   end

   quit:
    RunMacro("close all")
    Return( ok )

EndMacro
/*************************************************************
*
* A macro that will collapse transit on-offs by route and append
* route name.
* 
* Arguments
*   onOffTables     An array of on-off tables
*   rtsfile         A transit route file
*
*************************************************************/
Macro "Collapse OnOffs By Route" (onOffTables,rtsfile)
      
    {rte_lyr,stp_lyr,} = RunMacro("TCB Add RS Layers", rtsfile, "ALL", )   

    fields = {
        {"On","Sum",},
        {"Off","Sum",},
        {"DriveAccessOn","Sum",},
        {"WalkAccessOn","Sum",},
        {"DirectTransferOn","Sum",},
        {"WalkTransferOn","Sum",},
        {"DirectTransferOff","Sum",},
        {"WalkTransferOff","Sum",},
        {"EgressOff","Sum",}
    }
    
    // for all on off tables
    for i = 1 to onOffTables.length do

        onOffView = OpenTable("OnOffTable", "FFB", {onOffTables[i], null})
        path = SplitPath(onOffTables[i])
        outFile = path[1]+path[2]+path[3]+"_COLL.bin"
        
        fields = GetFields(onOffView, "All")
        
        //include all fields in each table except for STOP and ROUTE
        collFields = null
        for j = 1 to fields[1].length do 
            
            if(fields[1][j] !="STOP" and fields[1][j]!= "ROUTE") then do
            
                collFields = collFields + {{fields[1][j],"Sum",}}
            
            end
       end 
        
        // Collapse stops out of the table by collapsing on ROUTE
        rslt = AggregateTable("CollapsedView", onOffView+"|", "FFB", outFile, "ROUTE", collFields, )

        CloseView(onOffView)
        
        // Join the route layer for route name and other potentially useful data
        onOffCollView = OpenTable("OnOffTableColl", "FFB", {outFile})
        joinedView = JoinViews("OnOffJoin", onOffCollView+".Route", rte_lyr+".Route_ID",)

        // Write the joined data to a binary file
        outJoinFile = path[1]+path[2]+path[3]+"_COLL_JOIN.bin"
        ExportView(joinedView+"|","FFB", outJoinFile , , )
        outJoinFile = path[1]+path[2]+path[3]+"_COLL_JOIN.csv"
        ExportView(joinedView+"|","CSV", outJoinFile , , )
    end

    Return(1)
    quit:
        Return( RunMacro("TCB Closing", ret_value, True ) )
EndMacro
