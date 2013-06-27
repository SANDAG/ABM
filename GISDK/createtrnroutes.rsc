/******************************************************************************

Create all transit

update travel time with congested time from adjuststr.bin - Macro "update travel time"
then create transit network from selection set of routes - Macro"create transit networks"
(input: mode.dbf include fields: mode_id, mode_name, fare, fare_type, fare_field )

c********************************************************************************/

Macro "Create all transit"
   shared path, inputDir, outputDir, mxtap
 
    /* for testing
   path = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008"
   inputDir = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008\\input"                                                                        
   outputDir = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008\\output"                                                                        
   mxtap=2500
   RunMacro("TCB Init")     
   */ 
   
   ok=RunMacro("Import transit layer",{})
   if !ok then goto quit

   ok=RunMacro("Add transit time fields")  
   if !ok then goto quit

   ok=RunMacro("Update stop xy")  
   if !ok then goto quit
   
   ok=RunMacro("Create transit routes") 
   if !ok then goto quit

   ok=RunMacro("calc preload") 
   if !ok then goto quit
   
   ok=RunMacro("update preload fields")
   if !ok then goto quit
 
   RunMacro("close all")
 
   ok=1
   quit:
      return(ok)

EndMacro

/************************************************************************************

Import transit layer

import e00 and export to geo file

Inputs
   input\trcov.e00
   
Outputs
   output\transit.dbd
  

************************************************************************************/

Macro "Import transit layer"
   shared path, inputDir, outputDir, mxtap
      
   //check e00 file exists       
   di = GetDirectoryInfo(inputDir+"\\trcov.e00", "File")
   if di.length = 0 then do
      RunMacro("TCB Error", "trcov.e00 doesn't exist")
      return(0)
   end


   ImportE00(inputDir + "\\trcov.e00", outputDir + "\\trtmp.dbd","line",outputDir+ "\\trtmp.bin",{
                {"Label","transit line file"},
                {"Layer Name","transit"},
                {"optimize","True"},
                {"Median Split", "True"},
                {"Node Layer Name", "Endpoints"},
                {"Node Table", outputDir + "\\trtmp_.bin"},
                {"Projection","NAD83:406",{"_cdist=1000","_limit=1000","units=us-ft"}},
                })

   //open the intermediate transit line layer geo file
   map = RunMacro("G30 new map", outputDir + "\\trtmp.dbd","False")
   SetLayer("transit")
   allflds=GetFields("transit","All")
   fullflds=allflds[2]
   allnodeflds = GetFields("endpoints", "All")

   // need to specify full field specifications
   lineidfield = "transit.trcov-id"//arcinfo id field
   nodeidfield = "endpoints.tnode"//for centroids purposes

   opts = {{"Layer Name", "transit"},
          {"File Type", "FFB"},
          {"ID Field", lineidfield},
          {"Field Spec", fullflds},
          {"Indexed Fields", {fullflds[1]}},
          {"Label", "transit line file"},
          {"Node layer name","trnode"},
          {"Node ID Field", nodeidfield},
          {"Node Field Spec", allnodeflds[2]}}

   if node_idx > 1 then
         opts = opts + {{"Node ID Field", node_aflds[2][node_idx - 1]}}

   ExportGeography("transit",outputDir + "\\transit.dbd",opts)
  
   RunMacro("close all")
   DeleteDatabase(outputDir+"\\trtmp.dbd")
   
   return(1)
   
   quit:   
      return(0)
EndMacro

/*******************************************************************************************

Add transit time fields

Adds fields to the transit line layer. Local and premium time fields are added for
each period and direction.  The field names are

xxField_yy where 

Field is LOCTIME (local transit time) or PRETIME (premium transit time)
Field is TM

xx is AB or BA
yy is period 
   EA: Early AM
   AM: AM peak
   MD: Midday
   PM: PM peak
   EV: Evening

Note: this should be replaced by a revised transit network

*******************************************************************************************/
Macro "Add transit time fields"

   shared path, inputDir, outputDir
   db_file = outputDir+"\\transit.dbd"
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   db_link_lyr = db_file + "|" + link_lyr
   SetLayer(link_lyr)
   vw = GetView()
   strct = GetTableStructure(vw)

   // Copy the current name to the end of strct
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
  
   // Add fields to the output table
   new_struct = strct + {
                {"ABLOCTIME_EA", "real", 14, 6, "False",,,,,,, null},
                {"BALOCTIME_EA", "real", 14, 6, "False",,,,,,, null},
                {"ABPRETIME_EA", "real", 14, 6, "False",,,,,,, null},
                {"BAPRETIME_EA", "real", 14, 6, "False",,,,,,, null},
                {"ABLOCTIME_AM", "real", 14, 6, "False",,,,,,, null},
                {"BALOCTIME_AM", "real", 14, 6, "False",,,,,,, null},
                {"ABPRETIME_AM", "real", 14, 6, "False",,,,,,, null},
                {"BAPRETIME_AM", "real", 14, 6, "False",,,,,,, null},
                {"ABLOCTIME_MD", "real", 14, 6, "False",,,,,,, null},
                {"BALOCTIME_MD", "real", 14, 6, "False",,,,,,, null},
                {"ABPRETIME_MD", "real", 14, 6, "False",,,,,,, null},
                {"BAPRETIME_MD", "real", 14, 6, "False",,,,,,, null},
                {"ABLOCTIME_PM", "real", 14, 6, "False",,,,,,, null},
                {"BALOCTIME_PM", "real", 14, 6, "False",,,,,,, null},
                {"ABPRETIME_PM", "real", 14, 6, "False",,,,,,, null},
                {"BAPRETIME_PM", "real", 14, 6, "False",,,,,,, null},
                {"ABLOCTIME_EV", "real", 14, 6, "False",,,,,,, null},
                {"BALOCTIME_EV", "real", 14, 6, "False",,,,,,, null},
                {"ABPRETIME_EV", "real", 14, 6, "False",,,,,,, null},
                {"BAPRETIME_EV", "real", 14, 6, "False",,,,,,, null}}

   // Modify table structure
   ModifyTable(vw, new_struct)

   // Add fields to the output table
   new_struct = new_struct + {
                {"ABTM_EA", "real", 14, 6, "False",,,,,,, null},
                {"BATM_EA", "real", 14, 6, "False",,,,,,, null},
                {"ABTM_AM", "real", 14, 6, "False",,,,,,, null},
                {"BATM_AM", "real", 14, 6, "False",,,,,,, null},
                {"ABTM_MD", "real", 14, 6, "False",,,,,,, null},
                {"BATM_MD", "real", 14, 6, "False",,,,,,, null},
                {"ABTM_PM", "real", 14, 6, "False",,,,,,, null},
                {"BATM_PM", "real", 14, 6, "False",,,,,,, null},
                {"ABTM_EV", "real", 14, 6, "False",,,,,,, null},
                {"BATM_EV", "real", 14, 6, "False",,,,,,, null}}
              
   // Modify table structure
   ModifyTable(vw, new_struct)
   vw = GetView()
  
   // Set time fields to their respective input fields (especially needed for transit-only links)
   periods = {"_EA","_AM","_MD","_PM","_EV"}
   orig_periods = {"O", "A", "O", "P", "O"}
   for i = 1 to periods.length do
   
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"ABTM"+periods[i]}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = {"ABTM"+orig_periods[i]}
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
 
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
   Opts.Global.Fields = {"BATM"+periods[i]}
      Opts.Global.Method = "Formula"
   Opts.Global.Parameter = {"BATM"+orig_periods[i]}
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
 
   
   end
   
   
   RunMacro("close all")

   ok=1
   quit:
      return(ok)

EndMacro

/***************************************************************************************************************************
Update stop xy

Update transit node file with longitude and latitude of nearest node(?)

Input files:
   input\trstop.bin
   
Output files:
   output\transit.dbd

***************************************************************************************************************************/
Macro "Update stop xy"
   shared path, inputDir, outputDir
 
   Opts = null
   Opts.Input.[Dataview Set] = {{inputDir+"\\trstop.bin", outputDir+"\\transit.dbd|trnode", "NearNode", "ID"}, "trstop+trnode"}
   Opts.Global.Fields = {"trstop.Longitude"}
   Opts.Global.Method = "Formula"
   Opts.Global.Parameter = "trnode.Longitude"

   ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)

   if !ok then goto quit

   // STEP 2: Fill Dataview
   Opts = null
   Opts.Input.[Dataview Set] = {{inputDir+"\\trstop.bin", outputDir+"\\transit.dbd|trnode", "NearNode", "ID"}, "trstop+trnode"}
   Opts.Global.Fields = {"trstop.Latitude"}
   Opts.Global.Method = "Formula"
   Opts.Global.Parameter = "trnode.Latitude"

   ok = RunMacro("TCB Run Operation", 2, "Fill Dataview", Opts)
   if !ok then goto quit
   ok=1
   quit:
      Return(ok)

EndMacro

/**************************************************************************
Create transit routes

Create transit routes from table, and add the following fields to route table

mode
headway
real route
fare
configdir
              

input files: 
    input\trlink.bin    binary file for routes with link-id numbers
                        rte_number: sequential route number
                        link_id: node id
                        direction: +/-
    input\trstop.bin    stop table
    input\trrt.bin      route table
    output\transit.dbd  transit line layer with the updated congested travel time

output files: 
   output\transitrt.rts transit route file
   
***************************************************************************/
Macro "Create transit routes"
   shared path, inputDir, outputDir       
   
   //check input bin files exist
   lk_tb=inputDir+"\\trlink.bin"
   stp_tb=inputDir+"\\trstop.bin"
   rte_tb=inputDir+"\\trrt.bin"

   fnm=lk_tb
   di = GetDirectoryInfo(fnm, "File")
   if di.length = 0 then do
      ok=0
      RunMacro("TCB Error",fnm +"does not exist!")
      goto quit
   end
  
   fnm=stp_tb
   di = GetDirectoryInfo(fnm, "File")
   if di.length = 0 then do
      ok=0
      RunMacro("TCB Error",fnm +"does not exist!")
      goto quit
   end
   
   fnm=rte_tb
   di = GetDirectoryInfo(fnm, "File")
   if di.length = 0 then do
      ok=0
      RunMacro("TCB Error",fnm +"does not exist!")
      goto quit
   end
  
   // delete any old index file left from last time
   fnm=outputDir+"\\trlink.bx"
   ok=RunMacro("SDdeletefile",{fnm}) if !ok then goto quit
  
   fnm=outputDir+"\\trstop.bx"
   ok=RunMacro("SDdeletefile",{fnm}) if !ok then goto quit
   
   fnm=outputDir+"\\trrt.bx"
   ok=RunMacro("SDdeletefile",{fnm}) if !ok then goto quit

   Opts = null     
   Opts = {{"Routes Table" , rte_tb},
          {"Stops Table", stp_tb},
          {"Stops", "Route Stops",}}
   Opts.Label = "Transit Routes"
   Opts.Name = "Transit Routes"    
   geo_path = outputDir+"\\transit.dbd"
   geo_layer = "transit"
   rte_file=outputDir+"\\transitrt.rts"
   info = CreateRouteSystemFromTables(lk_tb, geo_path, geo_layer,rte_file , Opts)
   map = RunMacro("G30 new rt map", rte_file, "False", "False",)

   RunMacro("close all")
 
   ok=1
   quit:    
      return(ok)    
EndMacro
/********************************************************************

Create pre-load volumes on highway network from bus routes in
transit line layer.



input files:  hwycov.e00 - hwy line layer ESRI exchange file
output files: hwy.dbd - hwy line geographic file
              hwycad.log- a log file
              hwycad.err - error file with error info

v0.1 5/28/2012 jef

********************************************************************/

macro "calc preload"
   shared path,inputDir,outputDir,mxzone

   db_file=outputDir+"\\transit.dbd"
   rte_file=outputDir+"\\transitrt.rts"


   periods = {"_EA","_AM","_MD","_PM","_EV"}
   hours =   {   3,    3,   6.5,  3.5,    5} 
   headway_flds = {"OP_Headway","AM_Headway","OP_Headway","PM_Headway","OP_Headway"}

   bus_pce = 3.0
      
   //load transit line layer and route file
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)   // line database
   LinkIDField = link_lyr+".ID"
   {rs_lyr, stop_lyr, ph_lyr} = RunMacro("TCB Add RS Layers", rte_file, "ALL",)  // route system
   if rs_lyr = null then goto quit
    
   //add fields for transit pce
   SetLayer(link_lyr)
   vw = GetView()
   strct = GetTableStructure(vw)

   // Copy the current name to the end of strct
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
  
   // Add fields to the output table
   new_struct = strct + {
                {"ABPRELOAD_EA", "real", 14, 6, "False",,,,,,, null},
                {"BAPRELOAD_EA", "real", 14, 6, "False",,,,,,, null},
                {"ABPRELOAD_AM", "real", 14, 6, "False",,,,,,, null},
                {"BAPRELOAD_AM", "real", 14, 6, "False",,,,,,, null},
                {"ABPRELOAD_MD", "real", 14, 6, "False",,,,,,, null},
                {"BAPRELOAD_MD", "real", 14, 6, "False",,,,,,, null},
                {"ABPRELOAD_PM", "real", 14, 6, "False",,,,,,, null},
                {"BAPRELOAD_PM", "real", 14, 6, "False",,,,,,, null},
                {"ABPRELOAD_EV", "real", 14, 6, "False",,,,,,, null},
                {"BAPRELOAD_EV", "real", 14, 6, "False",,,,,,, null}}

   // Modify table structure
   ModifyTable(vw, new_struct)

   // query to determine valid routes
   SetLayer(rs_lyr)
   qry = "Select * where Mode > 0"
   sel="All"

   n = SelectByQuery(sel, "Several", qry,)
   CreateProgressBar("Loading...", "True")
   RT_ViewSet = rs_lyr+"|"
   
   rh = GetFirstRecord(RT_ViewSet, null)
   nRecords = GetRecordCount(rs_lyr, sel )
   count = 1
   
   // loop through each route
   while rh <> null do  


      // loop through periods
      for i = 1 to periods.length do  // for time period

   
         hdwyvals = GetRecordValues(rs_lyr, rh, {"Route_ID", headway_flds[i]})  // get route headway
         rtnm = GetRouteNam(rs_lyr, hdwyvals[1][2]) 
      
         // get the links for each route
         rt_links = GetRouteLinks(rs_lyr, rtnm)
         msg = "Loading Route " + rtnm + " ..."
         if UpdateProgressBar(msg, r2i(count/nRecords*100)) = "True" then do
            ShowMessage("Execution stopped by user.")
            DestroyProgressBar()
            Return()
         end
      
         // calculate bus frequency based on headway 
         veh_per_hour=0.0
         if (hdwyvals[2][2] <> null and hdwyvals[2][2]>0) then veh_per_hour = 60.0 / hdwyvals[2][2]     // 60 / HDWY  
      
         if veh_per_hour > 0 then do
            View_Set = link_lyr + "|"
         
            // loop for every link along the route
            for link = 1 to rt_links.length do
            
               // set record for the link
               rh2 = LocateRecord(View_Set, LinkIDField, {rt_links[link][1], rt_links[link][2]},)
               if rh2 <> null then do
            
              
                  ABFillField = "ABPRELOAD"+periods[i]
                  BAFillField = "BAPRELOAD"+periods[i]
                  
                  // get bus flow
                  fldvals = GetRecordValues(link_lyr, rh2, {ABFillField, BAFillField})
                  ab_val = fldvals[1][2]
                  ba_val = fldvals[2][2]
                 
                  transit_pce = veh_per_hour * hours[i] * bus_pce
                  
                  if rt_links[link][2] = 1 then do     // FORWARD 
                     
                     if fldvals[1][2] = null then do 
                        ab_val = transit_pce 
                     end
                     else do 
                        ab_val = fldvals[1][2] + transit_pce
                     end  
                  end
                  else do     // REVERSE
                     if fldvals[2][2] = null then do
                        ba_val = transit_pce
                     end
                     else do
                        ba_val = fldvals[2][2] + transit_pce  
                     end  
                  end
                  
                  // set the proper link id with the preload value
                  SetRecordValues(link_lyr, rh2, {{ABFillField, ab_val}, {BAFillField, ba_val}})
               
               end
              
            end
         end
      end

      count = count + 1
      next_rcd:
      rh = GetNextRecord(RT_ViewSet, null, null)
   end
   DestroyProgressBar()

  RunMacro("close all")
 
   ok=1
   quit:    
      return(ok)    
EndMacro
/**********************************************************************************************************
Update preload fields

Updates preload fields on the highway line layer from the transit line layer
**********************************************************************************************************/
Macro "update preload fields"

   shared path, inputDir, outputDir
  
   periods = {"_EA","_AM","_MD","_PM","_EV"}
   db_file=outputDir+"\\hwy.dbd"
   net_file = outputDir + "\\hwy.net" 

  {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   
   vw = SetView(link_lyr)
   
   for i = 1 to periods.length do
    
      transitTable = outputDir+"\\transit.bin"
      ABField = "ABPRELOAD"+periods[i]
      BAField = "BAPRELOAD"+periods[i]       
      
      // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
			Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, transitTable, {"ID"}, {"[TRCOV-ID]"}}, ABField }   
      Opts.Global.Fields = {"hwyline."+ABField}                                    // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"if transit."+ABField+" <>null then transit."+ABField+" else 0.0" }   
      ok = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ok then goto quit

     // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, transitTable, {"ID"}, {"[TRCOV-ID]"}}, BAField}   
      Opts.Global.Fields = {"hwyline."+BAField}                                    // the field to fill
      Opts.Global.Method = "Formula"                                              // the fill method
      Opts.Global.Parameter = {"if transit."+BAField+" <>null then transit."+BAField+" else 0.0" }   
      ok = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ok then goto quit

   end
	

   //update the highway network with the new fields
   for i = 1 to periods.length do
   
      field = "*PRELOAD"+periods[i]
 
      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*PRELOAD"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABPRELOAD"+periods[i],link_lyr+".BAPRELOAD"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ok = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ok then goto quit
   end
      
   RunMacro("close all")
 
   ok=1
   quit:    
      return(ok)    
 	
EndMacro

/**********************************************************************************************************
Update headway fields based upon Vovsha headway function

**********************************************************************************************************/
Macro "update headways"

   shared path, inputDir, outputDir
  
   db_file=outputDir+"\\transit.dbd"
   rte_file=outputDir+"\\transitrt.rts"
   
   headway_flds = {"AM_Headway","OP_Headway","PM_Headway"}
   
   dim rev_headway[headway_flds.length]
   
   //load transit line layer and route file
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)   // line database
   LinkIDField = link_lyr+".ID"
   {rs_lyr, stop_lyr, ph_lyr} = RunMacro("TCB Add RS Layers", rte_file, "ALL",)  // route system
   if rs_lyr = null then goto quit
    
   SetLayer(rs_lyr)
   vw = GetView()    

   RT_ViewSet = rs_lyr+"|"
   
   rh = GetFirstRecord(RT_ViewSet, null)
   
   // loop through each route
   while rh <> null do  

       hdwyvals = GetRecordValues(rs_lyr, rh, headway_flds)  // get route headway

       //for each headway
       for i = 1 to hdwyvals.length do
           
           //calculate revised headway
           rev_headway[i] = RunMacro("calculate revised headway",hdwyvals[i][2])
           
           // set the headways back in the route record
           SetRecordValues(rs_lyr, rh, {{headway_flds[i], rev_headway[i]}})
      end
       
      rh = GetNextRecord(RT_ViewSet, null, null)
   end
    
  
   RunMacro("close all")
 
   ok=1
   quit:    
      Return(ok)    
EndMacro      


/*****************************************************************************

   Revised headways

*****************************************************************************/
Macro "calculate revised headway" (headway)

    // CALCULATE REVISED HEADWAY
    
    slope_1=1.0     //slope for 1st segment (high frequency) transit
    slope_2=0.8     //slope for 2nd segemnt (med frequency)  transit
    slope_3=0.7     //slope for 3rd segment (low frequency)  transit
    slope_4=0.2     //slope for 4th segment (very low freq)  transit
    
    break_1=10      //breakpoint of 1st segment, min 
    break_2=20      //breakpoint of 2nd segment, min 
    break_3=30      //breakpoint of 3rd segment, min 
    
    
    if headway < break_1 then do
    	rev_headway = headway * slope_1
    	end
    else if headway < break_2 then do
    	part_1_headway = break_1 * slope_1
    	part_2_headway = (headway - break_1) * slope_2
    	rev_headway = part_1_headway + part_2_headway
    	end
    else if headway < break_3 then do
      part_1_headway = break_1 * slope_1
    	part_2_headway = (break_2 - break_1) * slope_2
    	part_3_headway = (headway - break_2) * slope_3
    	rev_headway = part_1_headway + part_2_headway + part_3_headway
    	end
    else do
      part_1_headway = break_1 * slope_1
    	part_2_headway = (break_2 - break_1) * slope_2
    	part_3_headway = (break_3 - break_2) * slope_3
    	part_4_headway = (headway - break_3) * slope_4
    	rev_headway = part_1_headway + part_2_headway + part_3_headway + part_4_headway
    end
    		
    Return(rev_headway)

EndMacro

 