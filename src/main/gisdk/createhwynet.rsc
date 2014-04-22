//********************************************************************
//procedure to import e00 file to geo dbd file
//create highway network
//written on 4/19/01
//macro "import highway layer", macro"fill oneway streets",
//macro "createhwynet1"
//
//input files:  hwycov.e00 - hwy line layer ESRI exchange file
//output files: hwy.dbd - hwy line geographic file
//              hwycad.log- a log file
//              hwycad.err - error file with error info
//Oct 08, 2010: Added Lines 164-186, Create a copy of Toll fields
//Oct 08, 2010: Added Lines 284-287 Build Highway Network with ITOLL fields
//April 22, 2014: Wu checked all SR125 related changes are included
//********************************************************************

macro "run create hwy"
   shared path,inputDir,outputDir,mxzone
  
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","import highway layer"}) 
   ok=RunMacro("import highway layer") 
   if !ok then goto quit

   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","fill oneway streets"})
   ok=RunMacro("fill oneway streets") 
   if !ok then goto quit
   
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","add TOD attributes"})
   ok=RunMacro("add TOD attributes") 
   if !ok then goto quit
   
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","add preload attributes"})
   ok=RunMacro("add preload attributes") 
   if !ok then goto quit

   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","Code VDF fields"})
   ok=RunMacro("Code VDF fields") 
   if !ok then goto quit
      
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","create hwynet"})
   ok=RunMacro("create hwynet")  
   if !ok then goto quit
   
   quit:
       return(ok)
EndMacro

/**********************************************************************************************************
  import e00 file
   if numofzone=tdz then e00file=hwycovtdz.e00, hwytdz.dbd
   else e00file=hwy.e00, hwy.dbd
   
   Inputs:
      input\turns.csv
      input\turns.DCC
      input\hwycov.e00
      
   Outputs:
      output\turns.dbf
      output\hwytmp.dbd
      output\hwy.dbd
   
**********************************************************************************************************/
macro "import highway layer" 
   shared path, inputDir,outputDir
   ok=0

   RunMacro("close all")  

   di=GetDirectoryInfo(path + "\\tchc1.err","file")
   if di.length>0 then do
      ok=0
	   RunMacro("TCB Error","chech tchc1.err file!")
      goto exit
   end    
  
   di=GetDirectoryInfo(path + "\\tchc.err","file")
   if di.length>0 then do
      ok=0
      RunMacro("TCB Error","chech tchc.err file!")
      goto exit
   end
  
   di=GetDirectoryInfo(inputDir + "\\turns.csv","file")
   if di.length=0 then do
      ok=0
      RunMacro("TCB Error","turns.csv does not exist!")
      goto exit
   end
   
   // assume that the dictionary file exists in the input directory for the model run 
   //ok=RunMacro("SDcopyfile",{path_study+"\\data\\turns.DCC",path+"\\turns.DCC"})
   //if !ok then goto exit
  
   //export turns.csv to turns.dbf 
   vw = OpenTable("turns", "CSV", {inputDir+"\\turns.csv",})
   ExportView("turns|", "dbase", outputDir+"\\turns.dbf",,)

   //  writeline(fpr,mytime+", exporting turns.csv to turns.dbf")   

	// import e00 file
   e00file="hwycov.e00"
  
  //check e00 file exists 
   di = GetDirectoryInfo(inputDir +"\\"+e00file, "File")
   if di.length = 0 then do
      ok=0
      RunMacro("TCB Error",e00file+" does not exist!")
      goto exit 
   end
 
   ImportE00(inputDir +"\\"+e00file, outputDir + "\\hwytmp.dbd","line",outputDir + "\\hwytmp.bin",{
           {"Label","street line file"},
           {"Layer Name","hwyline"},
           {"optimize","True"},
           {"Median Split", "True"},
           {"Node Layer Name", "hwynode"},
           {"Node Table", outputDir + "\\hwytmp_.bin"},
           {"Projection","NAD83:406",{"_cdist=1000","_limit=1000","units=us-ft"}},
                })
	
	//writeline(fpr,mytime+", importing e00 file")
  
   //export geo file by specify the line id field and the node id field
   db_file=outputDir + "\\hwytmp.dbd"
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)  
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto exit     
  
   allflds=Getfields(link_lyr,"All")
   fullflds=allflds[2]
   allnodeflds = GetFields(node_lyr, "All")
  
   // need to specify full field specifications
   lineidfield = link_lyr+".hwycov-id"
   nodeidfield = "hwynode.hnode"//for centroids purposes

   opts = {{"Layer Name", "hwyline"},
          {"File Type", "FFB"},
          {"ID Field", lineidfield},
          {"Field Spec", fullflds},
          {"Indexed Fields", {fullflds[1]}},
          {"Label", "street line file"},
          {"Node layer name","hwynode"},
          {"Node ID Field", nodeidfield},
          {"Node Field Spec", allnodeflds[2]}}

   if node_idx > 1 then
   opts = opts + {{"Node ID Field", node_aflds[2][node_idx - 1]}}
   hwy_db=outputDir + "\\hwy.dbd"

   exportgeography(link_lyr,hwy_db,opts)

	//  writeline(fpr,mytime+", exporting e00 file")
  
   RunMacro("close all")    //before delete db_file, close it
   deleteDatabase(db_file)    
  
   ok=1
   exit:
      //if fpr<>null then closefile(fpr)
  	   return(ok)
endMacro

/**********************************************************************************************************
   fill oneway street with dir field, and calculate toll fields and change AOC and add reliability factor
  
   Inputs
      output\hwy.dbd         
      
   Outputs:
      output\hwy.dbd (modified)
      
   Adds fields to link layer (by period: _EA, _AM, _MD, _PM, _EV)
      ITOLL  - Toll + 100 *[0,1] if managed lane (I-15 tolls) 
      ITOLL2 - Toll
      ITOLL3 - Toll + AOC
      ITOLL4 - Toll * 1.03 + AOC
      ITOLL5 - Toll * 2.33 + AOC
  
  
**********************************************************************************************************/
macro "fill oneway streets" 
   shared path, inputDir, outputDir
   ok=0
 
   RunMacro("close all")
   
   aoc = RunMacro("set aoc")

//  fpr=openfile(path+"\\hwycad.log","a")
//  mytime=GetDateAndTime() 

   db_file=outputDir+"\\hwy.dbd"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr = db_file + "|" + link_lyr

//  writeline(fpr,mytime+", fill one way streets")
//  closefile(fpr)
  
  //oneway streets, dir = 1
   Opts = null
   Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection", "Select * where iway = 1"}
   Opts.Global.Fields = {"Dir"}
   Opts.Global.Method = "Value"
   Opts.Global.Parameter = {1}
   ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
   if !ok then goto quit
   
   //CHANGE SR125 TOLL SPEED TO 70MPH (ISPD=70) DELETE THIS SECTION AFTER TESTING
      Opts = null
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"ISPD"}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = "if ihov=4 and IFC=1 then 70 else ISPD"
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
 

  // Create RELIABILITY OF FACILITY (TOLL) field
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"relifac", "Real", 10, 2, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   //change reliability field for SR125 to 0.65, and all other facilities are 0
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"relifac"}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = "if ihov=4  & ifc=1 then 0.65 else 1"
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit  

   //change AOC to appropriate value for year and cents per mile in COST field and add reliability factor to COST calc.
      Opts = null
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"COST"}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = "Length * "+R2S(aoc)+" * relifac"
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit

   // Create copy of ITOLL fields to preserved original settings
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"ITOLL2_EA", "Integer", 4, 0, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_AM", "Integer", 4, 0, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_MD", "Integer", 4, 0, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_PM", "Integer", 4, 0, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_EV", "Integer", 4, 0, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   tollfld={{"ITOLL2_EA"},{"ITOLL2_AM"},{"ITOLL2_MD"},{"ITOLL2_PM"},{"ITOLL2_EV"}}  
   tollfld_flg={{"ITOLLO"},{"ITOLLA"},{"ITOLLO"},{"ITOLLP"},{"ITOLLO"}}               //note - change this once e00 file contains fields for each of 5 periods
   for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = tollfld_flg[i]
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end   

   // Create ITOLL3 fields with ITOLL2A and COST
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"ITOLL3_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL3_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL3_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL3_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL3_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   tollfld={{"ITOLL3_EA"},{"ITOLL3_AM"},{"ITOLL3_MD"},{"ITOLL3_PM"},{"ITOLL3_EV"}}  
   tollfld_flg={{"ITOLL2_EA+COST"},{"ITOLL2_AM+COST"},{"ITOLL2_MD+COST"},{"ITOLL2_PM+COST"},{"ITOLL2_EV+COST"}} //note - change this once e00 file contains fields for each of 5 periods
   for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = tollfld_flg[i]
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end  
 
   // Create ITOLL4 fields with 1.03*(ITOLL2) and COST 
   // ITOLL4 =  is applied to LHD and MHD only
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"ITOLL4_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL4_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL4_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL4_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL4_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   tollfld={{"ITOLL4_EA"},{"ITOLL4_AM"},{"ITOLL4_MD"},{"ITOLL4_PM"},{"ITOLL4_EV"}}  
   tollfld_flg={{"1.03*ITOLL2_EA+COST"},{"1.03*ITOLL2_AM+COST"},{"1.03*ITOLL2_MD+COST"},{"1.03*ITOLL2_PM+COST"},{"1.03*ITOLL2_EV+COST"}} //note - change this once e00 file contains fields for each of 5 periods
   for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = tollfld_flg[i]
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end  
     
   // Create ITOLL5 fields with 2.33*(ITOLL2) and COST 
   // ITOLL5 =  is applied to HHD only
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"ITOLL5_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL5_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL5_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL5_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL5_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   tollfld={{"ITOLL5_EA"},{"ITOLL5_AM"},{"ITOLL5_MD"},{"ITOLL5_PM"},{"ITOLL5_EV"}}  
   tollfld_flg={{"2.33*ITOLL2_EA+COST"},{"2.33*ITOLL2_AM+COST"},{"2.33*ITOLL2_MD+COST"},{"2.33*ITOLL2_PM+COST"},{"2.33*ITOLL2_EV+COST"}}  //note - change this once e00 file contains fields for each of 5 periods
   for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = tollfld_flg[i]
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end
   
   // Create ITOLL fields  
   vw = GetView()
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   strct = strct + {{"ITOLL_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   //adding $100 to toll fields to flag toll values from manage lane toll values in skim matrix
   tollfld={{"ITOLL_EA"},{"ITOLL_AM"},{"ITOLL_MD"},{"ITOLL_PM"},{"ITOLL_EV"}}
   tollfld_flg={{"if ihov=4 then ITOLLO+10000 else ITOLLO"},
                {"if ihov=4 then ITOLLA+10000 else ITOLLA"},
                {"if ihov=4 then ITOLLO+10000 else ITOLLO"},
                {"if ihov=4 then ITOLLP+10000 else ITOLLP"},
                {"if ihov=4 then ITOLLO+10000 else ITOLLO"}}  //note - change this once e00 file contains fields for each of 5 periods
   for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = tollfld_flg[i]
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end

   RunMacro("close all")    
   
   ok=1
   quit: 
      return(ok)     
EndMacro

 
/**********************************************************************************************************
   add link attributes for tod periods
  
   Inputs
      output\hwy.dbd         
      
   Outputs:
      output\hwy.dbd (modified)
      
   
  
**********************************************************************************************************/
Macro "add TOD attributes"

   shared path, inputDir, outputDir
   ok=0
 
    db_file=outputDir+"\\hwy.dbd"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr = db_file + "|" + link_lyr
 
   vw = SetView(link_lyr)
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   
   // AB Link capacity 
   strct = strct + {{"ABCP_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCP_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCP_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCP_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCP_EV", "Real", 14, 6, "True", , , , , , , null}}
 
  // BA Link capacity 
   strct = strct + {{"BACP_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACP_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACP_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACP_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACP_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Intersection capacity
   strct = strct + {{"ABCX_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCX_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCX_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCX_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCX_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Intersection capacity
   strct = strct + {{"BACX_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACX_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACX_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACX_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACX_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Link time
   strct = strct + {{"ABTM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTM_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTM_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTM_EV", "Real", 14, 6, "True", , , , , , , null}}
 
   // BA Link time
   strct = strct + {{"BATM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATM_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATM_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATM_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Intersection time  
   strct = strct + {{"ABTX_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTX_AM", "Real", 14, 6, "True", , , , , , , null}}                                                                  
   strct = strct + {{"ABTX_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABTX_PM", "Real", 14, 6, "True", , , , , , , null}}                                                                  
   strct = strct + {{"ABTX_EV", "Real", 14, 6, "True", , , , , , , null}}
 
   // BA Intersection time  
   strct = strct + {{"BATX_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATX_AM", "Real", 14, 6, "True", , , , , , , null}}                                                                  
   strct = strct + {{"BATX_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BATX_PM", "Real", 14, 6, "True", , , , , , , null}}                                                                  
   strct = strct + {{"BATX_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Lanes
   strct = strct + {{"ABLN_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLN_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABLN_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLN_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABLN_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Lanes
   strct = strct + {{"BALN_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALN_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BALN_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALN_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BALN_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Drive-alone non-toll cost
   strct = strct + {{"ABSCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Drive-alone non-toll cost
   strct = strct + {{"BASCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BASCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BASCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BASCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BASCST_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Shared 2 cost
   strct = strct + {{"ABH2CST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABH2CST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABH2CST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABH2CST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABH2CST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Shared 2 cost
   strct = strct + {{"BAH2CST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAH2CST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAH2CST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAH2CST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAH2CST_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Shared-3 cost
   strct = strct + {{"ABH3CST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABH3CST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABH3CST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABH3CST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABH3CST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Shared-3 cost
   strct = strct + {{"BAH3CST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAH3CST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAH3CST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAH3CST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAH3CST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // AB SOV Time
   strct = strct + {{"ABSTM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSTM_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSTM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSTM_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSTM_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA SOV Time
   strct = strct + {{"BASTM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BASTM_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BASTM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BASTM_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BASTM_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB HOV Time
   strct = strct + {{"ABHTM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABHTM_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABHTM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABHTM_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABHTM_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA HOV Time
   strct = strct + {{"BAHTM_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAHTM_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAHTM_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAHTM_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAHTM_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)
     
   // initialize time and cost fields to 999999
   tod_fld =   {{"ABSCST_EA"},{"ABSCST_AM"},{"ABSCST_MD"},{"ABSCST_PM"},{"ABSCST_EV"},  
                {"BASCST_EA"},{"BASCST_AM"},{"BASCST_MD"},{"BASCST_PM"},{"BASCST_EV"},  
                {"ABH2CST_EA"},{"ABH2CST_AM"},{"ABH2CST_MD"},{"ABH2CST_PM"},{"ABH2CST_EV"},  
                {"BAH2CST_EA"},{"BAH2CST_AM"},{"BAH2CST_MD"},{"BAH2CST_PM"},{"BAH2CST_EV"},  
                {"ABH3CST_EA"},{"ABH3CST_AM"},{"ABH3CST_MD"},{"ABH3CST_PM"},{"ABH3CST_EV"},  
                {"BAH3CST_EA"},{"BAH3CST_AM"},{"BAH3CST_MD"},{"BAH3CST_PM"},{"BAH3CST_EV"},  
                {"ABSTM_EA"},{"ABSTM_AM"},{"ABSTM_MD"},{"ABSTM_PM"},{"ABSTM_EV"},  
                {"BASTM_EA"},{"BASTM_AM"},{"BASTM_MD"},{"BASTM_PM"},{"BASTM_EV"},  
                {"ABHTM_EA"},{"ABHTM_AM"},{"ABHTM_MD"},{"ABHTM_PM"},{"ABHTM_EV"},  
                {"BAHTM_EA"},{"BAHTM_AM"},{"BAHTM_MD"},{"BAHTM_PM"},{"BAHTM_EV"}}  
                
   // now calculate fields
   for i=1 to tod_fld.length do
   
      calcString = {"999999"}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tod_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end 
   
      
   // set capacity fields
   tod_fld    ={{"ABCP_EA"},{"ABCP_AM"},{"ABCP_MD"},{"ABCP_PM"},{"ABCP_EV"},  //BA link capacity
                {"BACP_EA"},{"BACP_AM"},{"BACP_MD"},{"BACP_PM"},{"BACP_EV"},  //AB link capacity
                {"ABCX_EA"},{"ABCX_AM"},{"ABCX_MD"},{"ABCX_PM"},{"ABCX_EV"},  //BA intersection capacity
                {"BACX_EA"},{"BACX_AM"},{"BACX_MD"},{"BACX_PM"},{"BACX_EV"}}  //AB intersection capacity     
                     
   org_fld    ={"ABCPO","ABCPA","ABCPO","ABCPP","ABCPO",                  
                "BACPO","BACPA","BACPO","BACPP","BACPO", 
                "ABCXO","ABCXA","ABCXO","ABCXP","ABCXO", 
                "BACXO","BACXA","BACXO","BACXP","BACXO"}   
                
   factor     ={"3/12","1","6.5/12","3.5/3","8/12",             
                "3/12","1","6.5/12","3.5/3","8/12", 
                "3/12","1","6.5/12","3.5/3","8/12", 
                "3/12","1","6.5/12","3.5/3","8/12"} 
                     
   // now calculate capacity
   for i=1 to tod_fld.length do
   
      calcString = {"if "+org_fld[i]+ " != 999999 then " + factor[i] + " * " + org_fld[i] + " else 999999"}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tod_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end 
   
   
  // set time fields
   tod_fld    ={{"ABTM_EA"},{"ABTM_AM"},{"ABTM_MD"},{"ABTM_PM"},{"ABTM_EV"},  //BA link time
                {"BATM_EA"},{"BATM_AM"},{"BATM_MD"},{"BATM_PM"},{"BATM_EV"},  //AB link time
                {"ABTX_EA"},{"ABTX_AM"},{"ABTX_MD"},{"ABTX_PM"},{"ABTX_EV"},  //BA intersection time
                {"BATX_EA"},{"BATX_AM"},{"BATX_MD"},{"BATX_PM"},{"BATX_EV"}}  //AB intersection time     
                     
   org_fld    ={"ABTMO","ABTMA","ABTMO","ABTMP","ABTMO",                  
                "BATMO","BATMA","BATMO","BATMP","BATMO", 
                "ABTXO","ABTXA","ABTXO","ABTXP","ABTXO", 
                "BATXO","BATXA","BATXO","BATXP","BATXO"}   
                
   factor     ={"1","1","1","1","1",             
                "1","1","1","1","1", 
                "1","1","1","1","1", 
                "1","1","1","1","1"} 
               
   // now calculate time      
   for i=1 to tod_fld.length do
   
      calcString = { factor[i] + " * " + org_fld[i]}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tod_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end 
      
  // set lane fields
   tod_fld    ={{"ABLN_EA"},{"ABLN_AM"},{"ABLN_MD"},{"ABLN_PM"},{"ABLN_EV"},  //AB lanes
                {"BALN_EA"},{"BALN_AM"},{"BALN_MD"},{"BALN_PM"},{"BALN_EV"}}  //BA lanes    
                     
   org_fld    ={"ABLNO","ABLNA","ABLNO","ABLNP","ABLNO",                  
                "BALNO","BALNA","BALNO","BALNP","BALNO"}   
                
   factor     ={"1","1","1","1","1",             
                "1","1","1","1","1"} 
               
   // now calculate time      
   for i=1 to tod_fld.length do
   
      calcString = { factor[i] + " * " + org_fld[i]}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = tod_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end 
   
   RunMacro("close all")    
   
   ok=1
   quit: 
      return(ok)     
EndMacro
/************************************************************************************************
add preload attributes

Adds fields to highway line layer for storing preload volumes (currently bus volumes)

************************************************************************************************/
Macro "add preload attributes"

   shared path, inputDir, outputDir
  
   db_file=outputDir+"\\hwy.dbd"

   periods={"_EA","_AM","_MD","_PM","_EV"}

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr = db_file + "|" + link_lyr
  
   vw = SetView(link_lyr)
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
 
   // initialize to 0
   for i = 1 to periods.length do

        ABField = "ABPRELOAD"+periods[i]
	BAField = "BAPRELOAD"+periods[i]

        //initialize to 0
         Opts = null
         Opts.Input.[View Set] = {db_link_lyr, link_lyr}
         Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
         Opts.Global.Fields = {ABField}
         Opts.Global.Method = "Value"
         Opts.Global.Parameter = {0}
         ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
         if !ok then goto quit
               
         Opts = null
         Opts.Input.[View Set] = {db_link_lyr, link_lyr}
         Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
         Opts.Global.Fields = {BAField}
         Opts.Global.Method = "Value"
         Opts.Global.Parameter = {0}
         ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
         if !ok then goto quit
      
   end 
  

   RunMacro("close all")    

   ok=1
   quit: 
      return(ok)     
 	
EndMacro
/**************************************************************************************************

Macro Code VDF fields

This macro codes fields for the Tucson volume-delay function on the input highway line layer.  It
should be run prior to constructing highway networks for skim-building & assignment.  Eventually 
the logic in this macro will be replaced by GIS code.

Functional class (IFC)

    1 = Freeway 
    2 = Prime arterial
    3 = Major arterial
    4 = Collector
    5 = Local collector
    6 = Rural collector
    7 = Local (non Circulation Element) road
    8 = Freeway connector ramps
    9 = Local ramps
    10 = Zone connectors

Cycle length matrix

      Intersecting Link                     
Approach Link            2                3                4           5                6                7            8                   9
IFC   Description       Prime Arterial   Major Arterial   Collector   Local Collector  Rural Collector   Local Road   Freeway connector  Local Ramp
2     Prime Arterial     2.0              1.5               1.5         1.5            1.5               1.5           1.5                1.5
3     Major Arterial     1.5              1.5               1.0         1.0            1.0               1.0           1.0                1.0
4     Collector          1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0
5     Local Collector    1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0
6     Rural Collector    1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0
7     Local Road         1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0
8     Freeway connector  1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0
9     Local Ramp         1.5              1.0               1.0         1.0            1.0               1.0           1.0                1.0

Ramp with meter (abcnt = 4 or 5)    
   Cycle length = 0.5
   GC ratio 0.5 

Stop controlled intersection (

*************************************************************************************************/
Macro "Code VDF fields"
   shared path, inputDir, outputDir
   
   db_file=outputDir+"\\hwy.dbd"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr = db_file + "|" + link_lyr

  
   // Add AB_Cycle, AB_PF, BA_Cycle, and BA_PF
   vw = SetView(link_lyr)
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   
   strct = strct + {{"AB_GCRatio", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_GCRatio", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_Cycle", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_Cycle", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_PF", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_PF", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ALPHA1", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BETA1", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ALPHA2", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BETA2", "Real", 14, 6, "True", , , , , , , null}}
   ModifyTable(view1, strct)

   // Now set the view to the node layer
   SetLayer(node_lyr)
       
   nodes = GetDataVector(node_lyr+"|", "ID",)

   //create from/end node field in the line layer
   start_fld = CreateNodeField(link_lyr, "start_node", node_lyr+".ID", "From", )
   end_fld   = CreateNodeField(link_lyr, "end_node",   node_lyr+".ID", "To",    )
       
   //get the count of records for both line and node layer
   tot_line_count = GetRecordCount(link_lyr, )   //get number of records in linelayer
   tot_node_count = GetRecordCount(node_lyr, )   //get number of records in nodelayer
        
   //initilize several vectors
//   linkclass = {Vector(tot_line_count, "Float", {{"Constant", null}}),Vector(tot_line_count, "Float", {{"Constant", null}})} //line link-classes
        
   //pass the attibutes to the vectors
//   linkclass = GetDataVector(link_lyr+"|", "IFC",)
   
//   lineId = GetDataVector(link_lyr+"|", "ID",)
        
   
   //go node by node
   for i = 1 to  tot_node_count do
  
      //find the IDs of the links that connect at the node, put the effective links to vector 'link_list'
      link_list = null
      rec_handles = null

      all_link_list = GetNodeLinks(nodes[i])                                          //get all links connecting at this node
      link_list =     Vector(all_link_list.length, "Short", {{"Constant", 0}})        //to contain non-connector/ramp links coming to the node
      
      all_rec_handles = Vector(all_link_list.length, "String", {{"Constant", null}})  //to contain the record handle of all links coming to the node
      rec_handles =     Vector(all_link_list.length, "String", {{"Constant", null}})  //to contain the record handle of the non-connector/ramp links coming to the node
      
      //count how many links entering the node
      link_count = 0 
      two_oneway = 0
      signal = 0
      
      for j = 1 to all_link_list.length do
         record_handle = LocateRecord(link_lyr+"|","ID",{all_link_list[j]}, {{"Exact", "True"}})
         all_rec_handles[j] = record_handle
         
         ends_at_node_AB_direction = 0
         ends_at_node_BA_direction = 0
         
         signal_at_end = 0
         
         if(link_lyr.end_node = nodes[i] and (link_lyr.Dir = 1 or link_lyr.Dir = 0))   then ends_at_node_AB_direction = 1
         if(link_lyr.start_node = nodes[i] and link_lyr.Dir = -1)                      then ends_at_node_AB_direction = 1
         if(link_lyr.start_node = nodes[i] and (link_lyr.Dir = 1 or link_lyr.Dir = 0)) then ends_at_node_BA_direction = 1
         
         if ( ends_at_node_AB_direction = 1 and (link_lyr.[ABGC] <> null and link_lyr.[ABGC] > 0)) then signal_at_end = 1
         if ( ends_at_node_BA_direction = 1 and (link_lyr.[BAGC] <> null and link_lyr.[BAGC] > 0)) then signal_at_end = 1
            
         
         //only count the links that have approach toward the node
         if( ends_at_node_AB_direction = 1 and signal_at_end = 1) then do
            signal = signal + 1
         end
         else if( ends_at_node_BA_direction and signal_at_end = 1) then do
            signal = signal + 1
         end
            
         link_count = link_count + 1
         link_list[link_count] = all_link_list[j]
         rec_handles[link_count] = record_handle
         if link_lyr.Dir <> 0 then two_oneway = two_oneway+1

         
      end
      
      // if at least one incoming link has a gc ratio
      if (signal>0) then do 
         min_lc = 999
         max_lc = 0
         //process the links and find the lowest and highest linkclasses
         for j = 1 to link_count do
            //find the line record that owns the line ID
            SetRecord(link_lyr, rec_handles[j]) //set the current record with the record handle stored in vector 'rec_handles'

            if ((link_lyr.end_node = nodes[i] and (link_lyr.Dir = 1 or link_lyr.Dir = 0)) or 
                 (link_lyr.start_node = nodes[i] and link_lyr.Dir = -1))then do 
               if link_lyr.[IFC] <> null and link_lyr.[IFC] > 1 and link_lyr.[IFC] < 10 then do  //don't count freeways or centroid connectors
                  if link_lyr.[IFC] > max_lc then max_lc = link_lyr.[IFC]
                  if link_lyr.[IFC] < min_lc then min_lc = link_lyr.[IFC]
               end
            end
         
         end
      end

      //iterate through all links at this node and set cycle length
      for j = 1 to all_link_list.length do

         SetRecord(link_lyr, all_rec_handles[j]) //set the current record with the record handle stored in vector 'all_rec_handles'

         if(link_lyr.end_node = nodes[i] and (link_lyr.Dir = 1 or link_lyr.Dir = 0))   then ends_at_node_AB_direction = 1
         if(link_lyr.start_node = nodes[i] and link_lyr.Dir = -1)                      then ends_at_node_AB_direction = 1
         if(link_lyr.start_node = nodes[i] and (link_lyr.Dir = 1 or link_lyr.Dir = 0)) then ends_at_node_BA_direction = 1
 
         // Set AB fields for links whose end node is this node and are coded in the A->B direction
			if (ends_at_node_AB_direction = 1) then do
      
            //defaults are 1.0 minute cycle length and 1.0 progression factor
            c_len = 1.0
            p_factor = 1.0
               
            //set up the cycle length for AB direction if there is a gc ratio and more than 2 links 
            if (link_lyr.[ABGC]<>0 and signal > 0) then do 
            
               if (link_lyr.[IFC] = 2) then do
                  if (min_lc = 2)      then c_len = 2.0       //Prime arterial & Prime arterial
                  else                      c_len = 1.5       //Prime arterial & anything lower
               end
               else if (link_lyr.[IFC] = 3) then do
                  if (max_lc > 3)      then c_len = 1.0       //Major arterial & anything lower than a Major arterial
                  else                      c_len = 1.5       //Major arterial & Prime arterial or Major arterial
               end
               else if (link_lyr.[IFC] > 3) then do
                  if (max_lc > 2)      then c_len = 1.0       //Anything lower than a Major arterial & anything lower than a Prime arterial 
                  else                      c_len = 1.5       //Anything lower than a Major arterial & Prime arterial
               end
                              
               //update attributes                  
               if( link_lyr.[ABGC] > 10) then link_lyr.[AB_GCRatio] = link_lyr.[ABGC]/100
               if( link_lyr.[AB_GCRatio] > 1.0) then link_lyr.[AB_GCRatio] = 1.0
               
               link_lyr.[AB_Cycle] = c_len
               link_lyr.[AB_PF] = p_factor
            
            end
         
         end // end for AB links

         // Set BA fields for links whose start node is this node and are coded in the A->B direction
         if (ends_at_node_BA_direction = 1 ) then do

            // Only code links with an existing GC ratio (indicating a signalized intersection) 
            if (link_lyr.[BAGC]<>0 and signal > 0) then do 

               //defaults are 0.4 gc ratio, 1.0 minute cycle length and 1.0 progression factor
               gc_ratio = 0.4
               c_len = 1.0
               p_factor = 1.0
                  
               if (link_lyr.[IFC] = 2) then do
                  if (min_lc = 2)      then c_len = 2.0       //Prime arterial & Prime arterial
                  else                      c_len = 1.5       //Prime arterial & anything lower
               end
               else if (link_lyr.[IFC] = 3) then do
                  if (max_lc > 3)      then c_len = 1.0       //Major arterial & anything lower than a Major arterial
                  else                      c_len = 1.5       //Major arterial & Prime arterial or Major arterial
               end
               else if (link_lyr.[IFC] > 3) then do
                  if (max_lc > 2)      then c_len = 1.0       //Anything lower than a Major arterial & anything lower than a Prime arterial 
                  else                      c_len = 1.5       //Anything lower than a Major arterial & Prime arterial
               end
              
               //update attributes                  
               if( link_lyr.[BAGC] > 10) then link_lyr.[BA_GCRatio] = link_lyr.[BAGC]/100
               if( link_lyr.[BA_GCRatio] > 1.0) then link_lyr.[BA_GCRatio] = 1.0
               link_lyr.[BA_Cycle] = c_len
               link_lyr.[BA_PF] = p_factor

            end
            
         end  // end for BA links

         //code metered ramps AB Direction
         if(ends_at_node_AB_direction = 1 and (link_lyr.[ABCNT]= 4 or link_lyr.[ABCNT] = 5)) then do
            link_lyr.[AB_Cycle] = 2.0
            link_lyr.[AB_GCRatio] = 0.5
            link_lyr.[AB_PF] = 1.0
         end
         
          //code metered ramps BA Direction
         if(ends_at_node_BA_direction = 1 and (link_lyr.[BACNT]= 4 or link_lyr.[BACNT] = 5)) then do
            link_lyr.[BA_Cycle] = 2.0
            link_lyr.[BA_GCRatio] = 0.5
            link_lyr.[BA_PF] = 1.0
         end

        //code stops AB Direction
         if(ends_at_node_AB_direction = 1 and (link_lyr.[ABCNT]= 2 or link_lyr.[ABCNT] = 3)) then do
            link_lyr.[AB_Cycle] = 0.5
            link_lyr.[AB_GCRatio] = 0.5
            link_lyr.[AB_PF] = 1.0
         end         
         
         //code stops BA Direction
         if(ends_at_node_BA_direction = 1 and (link_lyr.[ABCNT]= 2 or link_lyr.[ABCNT] = 3)) then do
            link_lyr.[BA_Cycle] = 0.5
            link_lyr.[BA_GCRatio] = 0.5
            link_lyr.[BA_PF] = 1.0
         end         
         
      end   // end for links
      
   end // end for nodes

      
   // set alpha1 and beta1 fields, which are based upon free-flow speed to match POSTLOAD loaded time factors
   lwr_bound  = {  " 0",  "25",  "30",  "35",  "40",  "45",  "50",  "55",  "60",  "65",  "70",  "75"}   
   upr_bound  = {  "24",  "29",  "34",  "39",  "44",  "49",  "54",  "59",  "64",  "69",  "74",  "99"}  
   alpha1     = {"0.90","0.90","0.90","0.90","0.90","0.90","0.90","0.90","0.90","0.90","0.90","0.90"}  
   beta1      = {   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4"  , "4",   "4"}  


//   alpha1     = {"0.10","0.10","0.15","0.20","0.20","0.20","0.20","0.20","0.25","0.30","0.30","0.35"}  
//   beta1      = {   "5",   "5",   "5",   "5",   "6",   "6",   "7",   "7",   "8",   "8"  , "9",   "9"}  

   for j = 1 to lwr_bound.length do
   
      //alpha1
      calcString = { "if ISPD >= " + lwr_bound[j] + " and ISPD <= "+upr_bound[j] + " then "+alpha1[j]+" else ALPHA1"}

      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"ALPHA1"}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   
      //beta1
      calcString = { "if ISPD >= " + lwr_bound[j] + " and ISPD <= "+upr_bound[j] + " then "+beta1[j]+" else BETA1"}

      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"BETA1"}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit

   end 

   //set alpha2 and beta2 fields (note that signalized intersections and stop-controlled intersections have same parameters, only meters vary)
   alpha2_default = "0.15"
   beta2_default = "3.75"
   alpha2_meter = "2.5"
   beta2_meter = "2.25"
      
   Opts = null
   Opts.Input.[View Set] = {db_link_lyr, link_lyr}
   Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
   Opts.Global.Fields = {"ALPHA2","BETA2"}
   Opts.Global.Method = "Formula"
   Opts.Global.Parameter = {alpha2_default, beta2_default} 
   ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
   if !ok then goto quit
            
   Opts = null
   Opts.Input.[View Set] = {db_link_lyr, link_lyr}
   Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr,"Selection", "Select * where (abcnt=4 or abcnt=5)"}
   Opts.Global.Fields = {"ALPHA2","BETA2"}
   Opts.Global.Method = "Formula"
   Opts.Global.Parameter = {alpha2_meter,beta2_meter} 
   ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
   if !ok then goto quit

   // replicate across all time periods and add fields for vdf parameters
   periods = {"_EA", "_AM", "_MD", "_PM", "_EV"}
   meters =  {    0,     1,     0,     1,     0}
     
   for i = 1 to periods.length do
      
      // Add AB_Cycle, AB_PF, BA_Cycle, and BA_PF
      vw = SetView(link_lyr)
      strct = GetTableStructure(vw)
      for j = 1 to strct.length do
         strct[j] = strct[j] + {strct[j][1]}
      end
   
      strct = strct + {{"AB_GCRatio"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"BA_GCRatio"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"AB_Cycle"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"BA_Cycle"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"AB_PF"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"BA_PF"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"ALPHA1"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"BETA1"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"ALPHA2"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      strct = strct + {{"BETA2"+periods[i], "Real", 14, 6, "True", , , , , , , null}}
      
      ModifyTable(view1, strct)
   
      in_fld    ={"AB_GCRatio", "BA_GCRatio", "AB_Cycle","BA_Cycle","AB_PF","BA_PF"}
                     
      out_fld    ={"AB_GCRatio"+periods[i],"BA_GCRatio"+periods[i],"AB_Cycle"+periods[i],"BA_Cycle"+periods[i],"AB_PF"+periods[i],"BA_PF"+periods[i]}
                                     

      values = {0.0,0.0,0.0,0.0,1.0,1.0}
      // set GCRatio, Cycle length, PF
      for j=1 to out_fld.length do
          
         //initialize to 0
         Opts = null
         Opts.Input.[View Set] = {db_link_lyr, link_lyr}
         Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
         Opts.Global.Fields = {out_fld[j]}
         Opts.Global.Method = "Value"
         Opts.Global.Parameter = {values[j]}
         ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
         if !ok then goto quit
               
         Opts = null
         Opts.Input.[View Set] = {db_link_lyr, link_lyr}
         Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection", "Select * where "+in_fld[j]+"> 0 and "+in_fld[j]+"<>null"}
         Opts.Global.Fields = {out_fld[j]}
         Opts.Global.Method = "Formula"
         Opts.Global.Parameter = {in_fld[j]}
         ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
         if !ok then goto quit
      
      end 
  
      // reset GCRatio, cycle length and PF fields to 0 if metered ramp and off-peak period
      if(meters[i] = 0) then do

         for j=1 to out_fld.length do
            Opts = null
            Opts.Input.[View Set] = {db_link_lyr, link_lyr}
            Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr,"Selection", "Select * where (abcnt=4 or abcnt=5)"}
         Opts.Global.Fields = {out_fld[j]}
            Opts.Global.Method = "Value"
            Opts.Global.Parameter = {values[j]}
            ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
            if !ok then goto quit
         end
      end
      
      
      //set alpha1 and beta1 fields, which currently do not vary by time period
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"ALPHA1"+periods[i], "BETA1"+periods[i]}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = {"ALPHA1","BETA1"}
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
            
      //set alpha2 and beta2 fields, which currently do not vary by time period
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = {"ALPHA2"+periods[i],"BETA2"+periods[i]}
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = {"ALPHA2","BETA2"} 
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit

   end // end for periods
   
   quit:
      return(ok)
EndMacro
//*****************************************************************************************************************************************************************

/************************************************************************8
build highway network in TransCAD format
input file:   
   hwy.dbd - hwy line geographic file
      turns.csv - turn prohibitor csv file, fields: from (link), to (link), penalty (null)
      inktypeturns.dbf - dbf file between freeways and ramp added 0.5 min penalty
      linktypelog.dbf - link type look up binary file, format for MMA assignment 

output file:  hwy.net - hwy network file
      link field included in network file:
              Length (in miles)
              IFC: functional classification, also used as link type look up field
              *TM_EA (ABTM_EA/BATM_EA): Early AM period link travel time
              *TM_AM (ABTM_AM/BATM_AM): AM Peak period link travel time
              *TM_MD (ABTM_MD/BATM_MD): Midday  period link travel time
              *TM_PM (ABTM_PM/BATM_PM): PM Peak period link travel time
              *TM_EV (ABTM_EV/BATM_EV): Evening period link travel time
              *CP_EA (ABCP_EA/BACP_EA): Early AM period link capacity 
              *CP_AM (ABCP_AM/BACP_AM): AM Peak period link capacity 
              *CP_MD (ABCP_MD/BACP_MD): Midday  period link capacity 
              *CP_PM (ABCP_PM/BACP_PM): PM Peak period link capacity 
              *CP_EV (ABCP_EV/BACP_EV): Evening period link capacity 
              *TX_EA (ABTX_EA/BATX_EA): Early AM period intersection travel time
              *TX_AM (ABTX_AM/BATX_AM): AM Peak period intersection travel time
              *TX_MD (ABTX_MD/BATX_MD): Midday  period intersection travel time
              *TX_PM (ABTX_PM/BATX_PM): PM Peak period intersection travel time
              *TX_EV (ABTX_EV/BATX_EV): Evening period intersection travel time
              *CX_EA (ABCX_EA/BACX_EA): Early AM period intersection capacity 
              *CX_AM (ABCX_AM/BACX_AM): AM Peak period intersection capacity 
              *CX_MD (ABCX_MD/BACX_MD): Midday  period intersection capacity 
              *CX_PM (ABCX_PM/BACX_PM): PM Peak period intersection capacity 
              *CX_EV (ABCX_EV/BACX_EV): Evening period intersection capacity 
              COST: cost of distance (in cents) 19cents/mile
              *CST (ABCST/BACST): generalized cost (in cents) of 19cents/mile + 35cents/minute
              *SCST
              *H2CST
              *H3CST 
               ID: link ID of hwycad-id 
                                                                                       
   specify zone centroids, and create network                                           
   change network settings with linktype lookup table                                   
   turn prohibitors and link type turn penalty file                                                   

************************************************************************************/





macro "create hwynet"
   shared path, mxzone, inputDir, outputDir    
   ok = 0
   
   RunMacro("close all")
 

   //  fpr=openfile(path+"\\hwycad.log","a")
   //  mytime=GetDateAndTime()

   //input file
   db_file = outputDir + "\\hwy.dbd"  
   d_tp_tb = inputDir + "\\linktypeturns.dbf" //turn penalty in cents
   s_tp_tb = outputDir + "\\turns.dbf"

   //output files
   net_file = outputDir+"\\hwy.net"

   di2= GetDirectoryInfo(d_tp_tb, "file")
   di3= GetDirectoryInfo(s_tp_tb, "file")
   //check for files
   if di2.length=0 and di3.length=0 then do
      RunMacro("TCB Error",d_tp_tb+" "+s_tp_tb+" does not exist!")
      goto quit
   end 

   // RunMacro("TCB Init")
   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file,,)  
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit

   db_link_lyr=db_file+"|"+link_lyr
   db_node_lyr=db_file+"|"+node_lyr
   
   // STEP 1: Build Highway Network
   Opts = null
   Opts.Input.[Link Set] = {db_link_lyr,link_lyr}
   Opts.Global.[Network Options].[Node ID] = node_lyr+ ".ID"
   Opts.Global.[Network Options].[Link ID] = link_lyr + ".ID"
   Opts.Global.[Network Options].[Turn Penalties] = "Yes"
   Opts.Global.[Network Options].[Keep Duplicate Links] = "FALSE"
   Opts.Global.[Network Options].[Ignore Link Direction] = "FALSE"
   Opts.Global.[Link Options] = {{"Length", link_lyr+".Length", link_lyr+".Length"}, 
        {"ID", link_lyr+".ID", link_lyr+".ID"}, 
        {"IFC", link_lyr+".IFC", link_lyr+".IFC"}, 
        {"IHOV", link_lyr+".IHOV", link_lyr+".IHOV"},
        {"COST", link_lyr+".COST", link_lyr+".COST"},
        {"ABLN_EA",   link_lyr + ".ABLN_EA",   link_lyr + ".ABLN_EA"},
        {"ABLN_AM",   link_lyr + ".ABLN_AM",   link_lyr + ".ABLN_AM"},
        {"ABLN_MD",   link_lyr + ".ABLN_MD",   link_lyr + ".ABLN_MD"},
        {"ABLN_PM",   link_lyr + ".ABLN_PM",   link_lyr + ".ABLN_PM"},
        {"ABLN_EV",   link_lyr + ".ABLN_EV",   link_lyr + ".ABLN_EV"},
        {"ITOLL_EA",  link_lyr + ".ITOLL_EA",  link_lyr + ".ITOLL_EA"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL_AM",  link_lyr + ".ITOLL_AM",  link_lyr + ".ITOLL_AM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL_MD",  link_lyr + ".ITOLL_MD",  link_lyr + ".ITOLL_MD"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL_PM",  link_lyr + ".ITOLL_PM",  link_lyr + ".ITOLL_PM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL_EV",  link_lyr + ".ITOLL_EV",  link_lyr + ".ITOLL_EV"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL2_EA", link_lyr + ".ITOLL2_EA", link_lyr + ".ITOLL2_EA"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL2_AM", link_lyr + ".ITOLL2_AM", link_lyr + ".ITOLL2_AM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL2_MD", link_lyr + ".ITOLL2_MD", link_lyr + ".ITOLL2_MD"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL2_PM", link_lyr + ".ITOLL2_PM", link_lyr + ".ITOLL2_PM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL2_EV", link_lyr + ".ITOLL2_EV", link_lyr + ".ITOLL2_EV"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL3_EA", link_lyr + ".ITOLL3_EA", link_lyr + ".ITOLL3_EA"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL3_AM", link_lyr + ".ITOLL3_AM", link_lyr + ".ITOLL3_AM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL3_MD", link_lyr + ".ITOLL3_MD", link_lyr + ".ITOLL3_MD"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL3_PM", link_lyr + ".ITOLL3_PM", link_lyr + ".ITOLL3_PM"},  // Oct-08-2010, added to include toll+cost
        {"ITOLL3_EV", link_lyr + ".ITOLL3_EV", link_lyr + ".ITOLL3_EV"},  // Oct-08-2010, added to include toll+cost  
        {"ITOLL4_EA", link_lyr + ".ITOLL4_EA", link_lyr + ".ITOLL4_EA"},  // Nov-3-2010, added lhd & mhd toll=2*toll+cost               
        {"ITOLL4_AM", link_lyr + ".ITOLL4_AM", link_lyr + ".ITOLL4_AM"},  // Nov-3-2010, added lhd & mhd toll=2*toll+cost         
        {"ITOLL4_MD", link_lyr + ".ITOLL4_MD", link_lyr + ".ITOLL4_MD"},  // Nov-3-2010, added lhd & mhd toll=2*toll+cost         
        {"ITOLL4_PM", link_lyr + ".ITOLL4_PM", link_lyr + ".ITOLL4_PM"},  // Nov-3-2010, added lhd & mhd toll=2*toll+cost         
        {"ITOLL4_EV", link_lyr + ".ITOLL4_EV", link_lyr + ".ITOLL4_EV"},  // Nov-3-2010, added lhd & mhd toll=2*toll+cost         
        {"ITOLL5_EA", link_lyr + ".ITOLL5_EA", link_lyr + ".ITOLL5_EA"},  // Nov-3-2010, added hhd toll = 3*toll+cost         
        {"ITOLL5_AM", link_lyr + ".ITOLL5_AM", link_lyr + ".ITOLL5_AM"},  // Nov-3-2010, added hhd toll = 3*toll+cost         
        {"ITOLL5_MD", link_lyr + ".ITOLL5_MD", link_lyr + ".ITOLL5_MD"},  // Nov-3-2010, added hhd toll = 3*toll+cost     
        {"ITOLL5_PM", link_lyr + ".ITOLL5_PM", link_lyr + ".ITOLL5_PM"},  // Nov-3-2010, added hhd toll = 3*toll+cost                    
        {"ITOLL5_EV", link_lyr + ".ITOLL5_EV", link_lyr + ".ITOLL5_EV"},  // Nov-3-2010, added hhd toll = 3*toll+cost                    
        {"ITRUCK", link_lyr + ".ITRUCK", link_lyr + ".ITRUCK"},     // Sep-30-2011, added ITRUCK to the network           
        {"*CP_EA", link_lyr+".ABCP_EA", link_lyr+".BACP_EA"}, 
        {"*CP_AM", link_lyr+".ABCP_AM", link_lyr+".BACP_AM"}, 
        {"*CP_MD", link_lyr+".ABCP_MD", link_lyr+".BACP_MD"}, 
        {"*CP_PM", link_lyr+".ABCP_PM", link_lyr+".BACP_PM"}, 
        {"*CP_EV", link_lyr+".ABCP_EV", link_lyr+".BACP_EV"}, 
        {"*CX_EA", link_lyr+".ABCX_EA", link_lyr+".BACX_EA"}, 
        {"*CX_AM", link_lyr+".ABCX_AM", link_lyr+".BACX_AM"}, 
        {"*CX_MD", link_lyr+".ABCX_MD", link_lyr+".BACX_MD"}, 
        {"*CX_PM", link_lyr+".ABCX_PM", link_lyr+".BACX_PM"}, 
        {"*CX_EV", link_lyr+".ABCX_EV", link_lyr+".BACX_EV"}, 
        {"*TM_EA", link_lyr+".ABTM_EA", link_lyr+".BATM_EA"}, 
        {"*TM_AM", link_lyr+".ABTM_AM", link_lyr+".BATM_AM"}, 
        {"*TM_MD", link_lyr+".ABTM_MD", link_lyr+".BATM_MD"}, 
        {"*TM_PM", link_lyr+".ABTM_PM", link_lyr+".BATM_PM"}, 
        {"*TM_EV", link_lyr+".ABTM_EV", link_lyr+".BATM_EV"}, 
        {"*TX_EA", link_lyr+".ABTX_EA", link_lyr+".BATX_EA"}, 
        {"*TX_AM", link_lyr+".ABTX_AM", link_lyr+".BATX_AM"}, 
        {"*TX_MD", link_lyr+".ABTX_MD", link_lyr+".BATX_MD"}, 
        {"*TX_PM", link_lyr+".ABTX_PM", link_lyr+".BATX_PM"}, 
        {"*TX_EV", link_lyr+".ABTX_EV", link_lyr+".BATX_EV"}, 
        {"*CST",   link_lyr+".ABCST",   link_lyr+".BACST"},
        {"*SCST_EA", link_lyr+".ABSCST_EA", link_lyr+".BASCST_EA"}, 
        {"*SCST_AM", link_lyr+".ABSCST_AM", link_lyr+".BASCST_AM"}, 
        {"*SCST_MD", link_lyr+".ABSCST_MD", link_lyr+".BASCST_MD"}, 
        {"*SCST_PM", link_lyr+".ABSCST_PM", link_lyr+".BASCST_PM"}, 
        {"*SCST_EV", link_lyr+".ABSCST_EV", link_lyr+".BASCST_EV"}, 
        {"*H2CST_EA", link_lyr+".ABH2CST_EA", link_lyr+".BAH2CST_EA"}, 
        {"*H2CST_AM", link_lyr+".ABH2CST_AM", link_lyr+".BAH2CST_AM"}, 
        {"*H2CST_MD", link_lyr+".ABH2CST_MD", link_lyr+".BAH2CST_MD"}, 
        {"*H2CST_PM", link_lyr+".ABH2CST_PM", link_lyr+".BAH2CST_PM"}, 
        {"*H2CST_EV", link_lyr+".ABH2CST_EV", link_lyr+".BAH2CST_EV"}, 
        {"*H3CST_EA", link_lyr+".ABH3CST_EA", link_lyr+".BAH3CST_EA"}, 
        {"*H3CST_AM", link_lyr+".ABH3CST_AM", link_lyr+".BAH3CST_AM"}, 
        {"*H3CST_MD", link_lyr+".ABH3CST_MD", link_lyr+".BAH3CST_MD"}, 
        {"*H3CST_PM", link_lyr+".ABH3CST_PM", link_lyr+".BAH3CST_PM"}, 
        {"*H3CST_EV", link_lyr+".ABH3CST_EV", link_lyr+".BAH3CST_EV"}, 
        {"*STM_EA", link_lyr+".ABSTM_EA", link_lyr+".BASTM_EA"}, 
        {"*STM_AM", link_lyr+".ABSTM_AM", link_lyr+".BASTM_AM"}, 
        {"*STM_MD", link_lyr+".ABSTM_MD", link_lyr+".BASTM_MD"}, 
        {"*STM_PM", link_lyr+".ABSTM_PM", link_lyr+".BASTM_PM"}, 
        {"*STM_EV", link_lyr+".ABSTM_EV", link_lyr+".BASTM_EV"},                                   
        {"*HTM_EA", link_lyr+".ABHTM_EA", link_lyr+".BAHTM_EA"},    
        {"*HTM_AM", link_lyr+".ABHTM_AM", link_lyr+".BAHTM_AM"},    
        {"*HTM_MD", link_lyr+".ABHTM_MD", link_lyr+".BAHTM_MD"},    
        {"*HTM_PM", link_lyr+".ABHTM_PM", link_lyr+".BAHTM_PM"},    
        {"*HTM_EV", link_lyr+".ABHTM_EV", link_lyr+".BAHTM_EV"},    
        {"*GCRATIO_EA", link_lyr+".AB_GCRatio_EA", link_lyr+".BA_GCRatio_EA"}, 
        {"*GCRATIO_AM", link_lyr+".AB_GCRatio_AM", link_lyr+".BA_GCRatio_AM"}, 
        {"*GCRATIO_MD", link_lyr+".AB_GCRatio_MD", link_lyr+".BA_GCRatio_MD"}, 
        {"*GCRATIO_PM", link_lyr+".AB_GCRatio_PM", link_lyr+".BA_GCRatio_PM"}, 
        {"*GCRATIO_EV", link_lyr+".AB_GCRatio_EV", link_lyr+".BA_GCRatio_EV"}, 
        {"*CYCLE_EA", link_lyr+".AB_Cycle_EA", link_lyr+".BA_Cycle_EA"}, 
        {"*CYCLE_AM", link_lyr+".AB_Cycle_AM", link_lyr+".BA_Cycle_AM"}, 
        {"*CYCLE_MD", link_lyr+".AB_Cycle_MD", link_lyr+".BA_Cycle_MD"}, 
        {"*CYCLE_PM", link_lyr+".AB_Cycle_PM", link_lyr+".BA_Cycle_PM"}, 
        {"*CYCLE_EV", link_lyr+".AB_Cycle_EV", link_lyr+".BA_Cycle_EV"}, 
        {"*PF_EA", link_lyr+".AB_PF_EA", link_lyr+".BA_PF_EA"}, 
        {"*PF_AM", link_lyr+".AB_PF_AM", link_lyr+".BA_PF_AM"}, 
        {"*PF_MD", link_lyr+".AB_PF_MD", link_lyr+".BA_PF_MD"}, 
        {"*PF_PM", link_lyr+".AB_PF_PM", link_lyr+".BA_PF_PM"}, 
        {"*PF_EV", link_lyr+".AB_PF_EV", link_lyr+".BA_PF_EV"}, 
        {"*ALPHA1_EA", link_lyr+".ALPHA1_EA", link_lyr+".ALPHA1_EA"}, 
        {"*ALPHA1_AM", link_lyr+".ALPHA1_AM", link_lyr+".ALPHA1_AM"}, 
        {"*ALPHA1_MD", link_lyr+".ALPHA1_MD", link_lyr+".ALPHA1_MD"}, 
        {"*ALPHA1_PM", link_lyr+".ALPHA1_PM", link_lyr+".ALPHA1_PM"}, 
        {"*ALPHA1_EV", link_lyr+".ALPHA1_EV", link_lyr+".ALPHA1_EV"}, 
        {"*BETA1_EA", link_lyr+".BETA1_EA", link_lyr+".BETA1_EA"}, 
        {"*BETA1_AM", link_lyr+".BETA1_AM", link_lyr+".BETA1_AM"}, 
        {"*BETA1_MD", link_lyr+".BETA1_MD", link_lyr+".BETA1_MD"}, 
        {"*BETA1_PM", link_lyr+".BETA1_PM", link_lyr+".BETA1_PM"}, 
        {"*BETA1_EV", link_lyr+".BETA1_EV", link_lyr+".BETA1_EV"}, 
        {"*ALPHA2_EA", link_lyr+".ALPHA2_EA", link_lyr+".ALPHA2_EA"}, 
        {"*ALPHA2_AM", link_lyr+".ALPHA2_AM", link_lyr+".ALPHA2_AM"}, 
        {"*ALPHA2_MD", link_lyr+".ALPHA2_MD", link_lyr+".ALPHA2_MD"}, 
        {"*ALPHA2_PM", link_lyr+".ALPHA2_PM", link_lyr+".ALPHA2_PM"}, 
        {"*ALPHA2_EV", link_lyr+".ALPHA2_EV", link_lyr+".ALPHA2_EV"}, 
        {"*BETA2_EA", link_lyr+".BETA2_EA", link_lyr+".BETA2_EA"}, 
        {"*BETA2_AM", link_lyr+".BETA2_AM", link_lyr+".BETA2_AM"}, 
        {"*BETA2_MD", link_lyr+".BETA2_MD", link_lyr+".BETA2_MD"}, 
        {"*BETA2_PM", link_lyr+".BETA2_PM", link_lyr+".BETA2_PM"}, 
        {"*BETA2_EV", link_lyr+".BETA2_EV", link_lyr+".BETA2_EV"}, 
        {"*PRELOAD_EA", link_lyr+".ABPRELOAD_EA", link_lyr+".BAPRELOAD_EA"}, 
        {"*PRELOAD_AM", link_lyr+".ABPRELOAD_AM", link_lyr+".BAPRELOAD_AM"}, 
        {"*PRELOAD_MD", link_lyr+".ABPRELOAD_MD", link_lyr+".BAPRELOAD_MD"}, 
        {"*PRELOAD_PM", link_lyr+".ABPRELOAD_PM", link_lyr+".BAPRELOAD_PM"}, 
        {"*PRELOAD_EV", link_lyr+".ABPRELOAD_EV", link_lyr+".BAPRELOAD_EV"}} 
         
        
        
   // add two node fields into the network for turning movement purposes, by JXu
   Opts.Global.[Node Options].ID = node_lyr + ".ID"
   Opts.Global.[Node Options].DATA = node_lyr + ".temp"
   Opts.Output.[Network File] = net_file
   RunMacro("HwycadLog",{"createhwynet_turn.rsc: create hwynet1","Build Highway Network"})
   ok = RunMacro("TCB Run Operation", 1, "Build Highway Network", Opts)
   if !ok then goto quit

   // STEP 2: Highway Network Setting
   Opts = null
   Opts.Input.Database = db_file
   Opts.Input.Network = net_file
   Opts.Input.[Centroids Set] = {db_node_lyr, node_lyr, "Selection", "select * where ID <="+i2s(mxzone)}
   Opts.Input.[Def Turn Pen Table] = {d_tp_tb}
   Opts.Input.[Spc Turn Pen Table] = {s_tp_tb}
   Opts.Field.[Link type] = "IFC"
   Opts.Global.[Global Turn Penalties] = {0, 0, 0, 0}
   Opts.Flag.[Use Link Types] = "True"
   RunMacro("HwycadLog",{"createhwynet_turn.rsc: create hwynet1","Highway Network Setting"})
   ok = RunMacro("TCB Run Operation", 2, "Highway Network Setting", Opts)
   if !ok then goto quit

   mytime=GetDateAndTime()

   RunMacro("close all")    //before delete db_file, close it


   //writeline(fpr,mytime+", network setting") 
   ok=1
   quit: 
      //if fpr<>null then closefile(fpr)
      return(ok)
endMacro

/*  Utility macro to look up auto operating costs for year. 
    File names / directory structure are hard-coded.
*/
Macro "set aoc"
    shared path, inputDir
    
    properties = "\\conf\\"+"sandag_abm.properties"
    aocfile = path+"\\uec\\"+"AutoOperatingCost.xls"    
    tempfile = inputDir+"\\"+"aoc.bin"
    
    year = RunMacro("read properties",properties,"aoc.year","S")

    // workaround to read excel file (using csv makes updates within uecs manual)
    ExportExcel(aocfile, "FFB", tempfile, {"AOC", })
    OpenTable("aoc", "FFB", {tempfile, })
    
    // open view then get aoc by year index
    view = Opentable("aoc", "FFB", {tempfile})
    v = GetDataVectors(view+"|", {"Year", "Total Auto Operating Cost"}, )
    
    for k=1 to v[1].length do
        y = position(I2S(v[1][k]),year)
        if y = 1 then
            aoc = v[2][k]
            aoc = Round(aoc,2)
    end
    
    // delete temporary files
    CloseView(view)
    DeleteFile(tempfile)
    DeleteFile(Substitute(tempfile, ".bin", ".dcb", ))
    
    return(aoc)  
EndMacro 