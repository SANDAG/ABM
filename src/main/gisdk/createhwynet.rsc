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
//Feb 02, 2016: Added reliability fields
//********************************************************************

macro "run create hwy"
   shared path,inputDir,outputDir,mxzone

/* exported highway layer is copied manually to the output folder (I15 SB toll entry/exit links are modified by RSG)

   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","import highway layer"}) 
   ok=RunMacro("import highway layer") 
   if !ok then goto quit
*/

   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","copy highway database"}) 
   ok=RunMacro("copy database") 
   if !ok then goto quit
   
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","fill oneway streets"})
   ok=RunMacro("fill oneway streets") 
   if !ok then goto quit
  
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","add TOD attributes"})
   ok=RunMacro("add TOD attributes") 
   if !ok then goto quit

   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","calculate distance to/from major interchange"})
   ok=RunMacro("DistanceToInterchange") 
   if !ok then goto quit	
	 
   RunMacro("HwycadLog",{"createhwynet.rsc: run create hwy","add reliability fields"})
   ok=RunMacro("add reliability fields") 
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

/*
copies database (hwy.dbd) and turns file (TURNS.DBF)

this is required after edits made to the transcad highway database for I15 SB managed lane links

*/

macro "copy database"
	shared path, inputDir, outputDir
	
	hwy_db_in = inputDir + "\\hwy.dbd"
	hwy_db_out = outputDir + "\\hwy.dbd"
	
	/// copye highway database
	CopyDatabase(hwy_db_in, hwy_db_out)
	
	// copy turns file
	CopyFile(inputDir+"\\TURNS.DBF", outputDir+"\\TURNS.DBF")
	
	ok=1
	return(ok)

endMacro

/**********************************************************************************************************
   fill oneway street with dir field, and calculate toll fields and change AOC and add reliability factor
  
   Inputs
      output\hwy.dbd         
      
   Outputs:
      output\hwy.dbd (modified)
      
   Adds fields to link layer (by period: _EA, _AM, _MD, _PM, _EV)
      ITOLL  - Toll + 10000 *[0,1] if SR125 toll lane
      ITOLL2 - Toll
      ITOLL3 - Toll + AOC
      ITOLL4 - Toll * 1.03 + AOC
      ITOLL5 - Toll * 2.33 + AOC
      
  Note: Link operation type (IHOV) where:
                                1 = General purpose
                                2 = 2+ HOV (Managed lanes if lanes > 1)
                                3 = 3+ HOV (Managed lanes if lanes > 1)
                                4 = Toll lanes

  
  
**********************************************************************************************************/
macro "fill oneway streets" 
   shared path, inputDir, outputDir
   ok=0
 
   RunMacro("close all")
   
   properties = "\\conf\\sandag_abm.properties"   
   aoc_f = RunMacro("read properties",properties,"aoc.fuel", "S")
   aoc_m = RunMacro("read properties",properties,"aoc.maintenance", "S")
   aoc=S2R(aoc_f)+S2R(aoc_m)

   db_file=outputDir+"\\hwy.dbd"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr = db_file + "|" + link_lyr

//  writeline(fpr,mytime+", fill one way streets")
//  closefile(fpr)

/*  
  //oneway streets, dir = 1
   Opts = null
   Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection", "Select * where iway = 1"}
   Opts.Global.Fields = {"Dir"}
   Opts.Global.Method = "Value"
   Opts.Global.Parameter = {1}
   ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
   if !ok then goto quit
*/   
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
//      Opts.Global.Parameter = "if ihov=4  & ifc=1 then 0.65 else 1"
    //since we now have reliability fields, setting all reliability factors to 1
      Opts.Global.Parameter = "1"
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
	 
	// changed field types to real (for I15 tolls) - by nagendra.dhakar@rsginc.com
   strct = strct + {{"ITOLL2_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ITOLL2_EV", "Real", 14, 6, "True", , , , , , , null}}

   ModifyTable(view1, strct)

   tollfld={{"ITOLL2_EA"},{"ITOLL2_AM"},{"ITOLL2_MD"},{"ITOLL2_PM"},{"ITOLL2_EV"}}  
   tollfld_flg={{"ITOLLO"},{"ITOLLA"},{"ITOLLO"},{"ITOLLP"},{"ITOLLO"}}               //note - change this once e00 file contains fields for each of 5 periods
   
	 // set SR125 tolls
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
	 
	 // clear I15 tolls from previous step
	 // set other link tolls to 0 - creates a problem in skimming if left to null
	 // added by nagendra.dhakar@rsginc.com
	 for i=1 to tollfld.length do
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection", "Select *where ihov=2"}     
      Opts.Global.Fields = tollfld[i]
      Opts.Global.Method = "Value"
      Opts.Global.Parameter = {0}
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end      
   
   
		// set I15 tolls - added by nagendra.dhakar@rsginc.com
		RunMacro("set I15 tolls", link_lyr, tollfld)
	 
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
   tollfld_flg={{"if ihov=4 then ITOLL2_EA+10000 else ITOLL2_EA"},
                {"if ihov=4 then ITOLL2_AM+10000 else ITOLL2_AM"},
                {"if ihov=4 then ITOLL2_MD+10000 else ITOLL2_MD"},
                {"if ihov=4 then ITOLL2_PM+10000 else ITOLL2_PM"},
                {"if ihov=4 then ITOLL2_EV+10000 else ITOLL2_EV"}}  //note - change this once e00 file contains fields for each of 5 periods
				
				// modified by nagendra.dhakar@rsginc.com to calculate every toll field from itoll2, which are set to the tolls fields in tcoved 
								
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

/*********************************************************************************************************
add i-15 tolls by direction and period

	link ids and corresponding tolls are inputs
	toll values are coded by link ids
	tolls are determined by gate-to-gate toll optimization, solved using excel solver
	tolls from two methods are used
		-traversed links (NB PM and SB AM)
		-entry and exit links (remaining)

by: nagendra.dhakar@rsginc.com
**********************************************************************************************************/

Macro "set I15 tolls" (lyr, toll_fields)
	shared path, inputDir, outputDir
	
	direction = {"NB","SB"}
	periods={"EA","AM","MD","PM","EV"}
	
	toll_links = {}
	tolls = {}
	
	// NB toll links and corresponding tolls
	toll_links.NB = {}

	toll_links.NB.traverse = {29716,460,526,23044,459,463,512,464,469,470,510,29368,9808}
	toll_links.NB.entryexit = {31143,29472,52505,52507,52508,475,34231,52511,52512,34229,34228,38793,29765,29766,52513,29764,26766}
	
	tolls.NB = {}
	
	tolls.NB.traverse = {}
	tolls.NB.entryexit = {}
	
	// tolls are in cents
	tolls.NB.entryexit.EA = {35.00,35.00,35.00,35.00,35.00,15.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00}
	tolls.NB.entryexit.AM = {45.05,42.43,31.54,30.00,30.00,20.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,17.41,32.59,32.59,32.59}
	tolls.NB.entryexit.MD = {69.91,73.91,70.42,66.12,51.61,0.00,26.88,25.00,25.00,25.00,12.07,37.93,47.51,3.35,46.65,60.66,65.74}
	tolls.NB.traverse.PM = {21.83,31.11,50.00,55.34,113.23,50.00,50.00,50.00,50.00,50.00,50.00,50.00,0.00}
	tolls.NB.entryexit.EV = {41.73,36.26,32.01,30.00,30.00,20.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,17.77,32.23,32.23,32.23}
	
	// SB toll links and corresponding tolls
	toll_links.SB = {}
/*	
	// old network
	toll_links.SB.traverse = {12193,25749,29442,23128,515,31204,520,22275,524,525,553,528,29415}
	toll_links.SB.entryexit = {52514,38796,29768,38794,29763,52510,52509,52506,52510,34227,34233,29407,26398,29767,34226,34232,29471,52515}
*/	
	// new network
	toll_links.SB.traverse = {12193,25749,52567,23128,515,31204,52569,52550,524,525,52555,52559,52561,52565}
	toll_links.SB.entryexit = {52568,52570,29768,38794,29763,52560,52562,52566,52556,34227,34233,29407,26398,52571,52572,29767,52575,52576,52574,34226,34232,29471,52573}
	
	tolls.SB = {}
	
	tolls.SB.traverse = {}
	tolls.SB.entryexit = {}
/*	
	// old network
	tolls.SB.entryexit.EA = {26.69,25.54,26.96,25.54,39.23,25.54,24.46,25.54,25.54,25.54,24.46,24.46,36.30,23.31,24.46,24.46,28.04,0.00}
	tolls.SB.traverse.AM = {0.00,50.00,50.00,89.82,50.00,50.00,63.74,0.00,0.11,76.40,38.80,63.58,83.28}
	tolls.SB.entryexit.MD = {26.39,25.00,25.00,25.00,35.00,25.47,22.74,27.26,25.47,25.00,24.53,25.00,35.61,23.61,25.00,25.00,32.65,0.00}
	tolls.SB.entryexit.PM = {25.00,25.00,25.00,25.00,35.00,25.00,24.34,25.66,25.00,25.00,25.00,25.00,35.00,25.00,25.00,25.00,26.69,0.00}
	tolls.SB.entryexit.EV = {25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,25.00,35.00,25.00,25.00,25.00,25.00,0.00}	
*/	
	// new network
	tolls.SB.traverse.AM = {0.00,59.74,50.00,80.42,50.00,50.00,69.24,0.00,3.18,50.00,50.17,19.06,50.00,84.26}
	tolls.SB.entryexit.EA = {25.00,25.00,25.00,25.00,35.00,7.29,7.29,7.29,17.30,25.00,17.30,17.30,35.00,15.00,25.00,25.00,32.70,42.71,32.70,25.00,25.00,42.71,25.00}
	tolls.SB.entryexit.MD = {32.80,25.00,25.00,25.00,32.80,11.43,11.43,10.65,18.85,25.00,18.85,18.85,32.80,17.20,25.00,17.20,31.15,38.57,31.15,25.00,25.00,39.35,25.00}
	tolls.SB.entryexit.PM = {27.78,25.00,25.00,25.00,35.00,12.67,12.67,12.67,19.36,25.00,19.36,19.36,35.00,15.00,25.00,22.22,30.64,37.33,30.64,25.00,25.00,37.33,25.00}
	tolls.SB.entryexit.EV = {29.12,25.00,25.00,25.00,35.00,13.14,13.14,13.14,21.56,25.00,21.56,21.56,35.00,15.00,25.00,20.88,28.44,36.86,28.44,25.00,25.00,36.86,25.00}
	
	for dir=1 to 2 do
		for per=1 to periods.length do
			// locate record
			
			if (direction[dir]="NB" and periods[per] = "PM") or (direction[dir]="SB" and periods[per] = "AM") then method = "traverse"
			else method = "entryexit"
			
			links_array = toll_links.(direction[dir]).(method)
			tolls_array = tolls.(direction[dir]).(method).(periods[per])		
			
			// set toll values
			for i=1 to links_array.length do
				record_handle = LocateRecord (lyr+"|", "ID", {links_array[i]},{{"Exact", "True"}})
				SetRecordValues(lyr, record_handle, {{toll_fields[per][1],tolls_array[i]}})
			end
		end
	end

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

   // AB Drive-alone cost
   strct = strct + {{"ABSCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABSCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABSCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Drive-alone cost
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
   
   // AB Light-Heavy truck cost
   strct = strct + {{"ABLHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABLHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABLHCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Light-Heavy truck cost
   strct = strct + {{"BALHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BALHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BALHCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
    // AB Medium-Heavy truck cost
   strct = strct + {{"ABMHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABMHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABMHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABMHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABMHCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Medium-Heavy truck cost
   strct = strct + {{"BAMHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAMHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAMHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAMHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAMHCST_EV", "Real", 14, 6, "True", , , , , , , null}}

   // AB Heavy-Heavy truck cost
   strct = strct + {{"ABHHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABHHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABHHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABHHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABHHCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Heavy-Heavy truck cost
   strct = strct + {{"BAHHCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAHHCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAHHCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BAHHCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BAHHCST_EV", "Real", 14, 6, "True", , , , , , , null}}
  
     // AB Commercial vehicle cost
   strct = strct + {{"ABCVCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCVCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABCVCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABCVCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"ABCVCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // BA Commercial vehicle cost
   strct = strct + {{"BACVCST_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACVCST_AM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BACVCST_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BACVCST_PM", "Real", 14, 6, "True", , , , , , , null}}               
   strct = strct + {{"BACVCST_EV", "Real", 14, 6, "True", , , , , , , null}}
   
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
                {"BAHTM_EA"},{"BAHTM_AM"},{"BAHTM_MD"},{"BAHTM_PM"},{"BAHTM_EV"},
                {"ABLHCST_EA"},{"ABLHCST_AM"},{"ABLHCST_MD"},{"ABLHCST_PM"},{"ABLHCST_EV"},  
                {"BALHCST_EA"},{"BALHCST_AM"},{"BALHCST_MD"},{"BALHCST_PM"},{"BALHCST_EV"},  
                {"ABMHCST_EA"},{"ABMHCST_AM"},{"ABMHCST_MD"},{"ABMHCST_PM"},{"ABMHCST_EV"},  
                {"BAMHCST_EA"},{"BAMHCST_AM"},{"BAMHCST_MD"},{"BAMHCST_PM"},{"BAMHCST_EV"},  
                {"ABHHCST_EA"},{"ABHHCST_AM"},{"ABHHCST_MD"},{"ABHHCST_PM"},{"ABHHCST_EV"},  
                {"BAHHCST_EA"},{"BAHHCST_AM"},{"BAHHCST_MD"},{"BAHHCST_PM"},{"BAHHCST_EV"},  
                {"ABCVCST_EA"},{"ABCVCST_AM"},{"ABCVCST_MD"},{"ABCVCST_PM"},{"ABCVCST_EV"},  
                {"BACVCST_EA"},{"BACVCST_AM"},{"BACVCST_MD"},{"BACVCST_PM"},{"BACVCST_EV"} 
               }  
                
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

/**********************************************************************************************************
	Add link fields for reliability
	 
		v/c factor fields:
			{"ABLOSC_FACT"},{"ABLOSD_FACT"},{"ABLOSE_FACT"},{"ABLOSFL_FACT"},{"ABLOSFH_FACT"},
			{"BALOSC_FACT"},{"BALOSD_FACT"},{"BALOSE_FACT"},{"BALOSFL_FACT"},{"BALOSFH_FACT"},
		 
		static reliability fields:
			{"ABSTATREL_EA"},{"ABSTATREL_AM"},{"ABSTATREL_MD"},{"ABSTATREL_PM"},{"ABSTATREL_EV"},
			{"BASTATREL_EA"},{"BASTATREL_AM"},{"BASTATREL_MD"},{"BASTATREL_PM"},{"BASTATREL_EV"}	 
		 
		interchange fields - used in static reliability calculations
			{"INTDIST_UP"},{"INTDIST_DOWN"}

	Regression equations for static reliability:

		static reliability(freeway) = intercept + coeff1*ISPD70 + coeff2*1/MajorUpstream + coeff3*1/MajorDownstream
		static reliability(arterial) = intercept + coeff1*NumLanesOneLane + coeff2*NumLanesCatTwoLane + coeff3*NumLanesCatThreeLane + coeff4*NumLanesCatFourLanes + coeff5*NumLanesFiveMoreLane +
																		coeff6*ISPD.CatISPD35Less + coeff7*ISPD.CatISPD35 + coeff8*ISPD.CatISPD40 + coeff9*ISPD.CatISPD45 + coeff1*ISPD.CatISPD50 + coeff10*ISPD.CatISPD50More +
																		coeff11*ICNT.EstSignal + coeff12*ICNT.EstStop + coeff13*ICNT.EstRailRoad

		Where;
		ISPD70: 							1 if ISPD=70 else 0 (ISPD is posted speed)
		MajorUpstream: 				distance to major interchange upstream (miles)
		MajorDownstream: 			distance to major interchange downstream (miles)
		NumLanesOneLane: 			1 if lane=1 else 0
		NumLanesCatTwoLane: 	1 if lane=2 else 0
		NumLanesCatThreeLane: 1 if lane=3 else 0
		NumLanesCatFourLanes: 1 if lane=4 else 0
		NumLanesFiveMoreLane: 1 if lane>=5 else 0
		ISPD.CatISPD35Less: 	1 if ISPD <35 else 0
		ISPD.CatISPD35: 			1 if ISPD =35 else 0
		ISPD.CatISPD40: 			1 if ISPD =40 else 0
		ISPD.CatISPD45: 			1 if ISPD =45 else 0
		ISPD.CatISPD50: 			1 if ISPD =50 else 0
		ISPD.CatISPD50More: 	1 if ISPD >=50 else 0
		ICNT.EstSignal: 			1 if ICNT=1 else 0 (ICNT is intersection control type); signal-controlled
		ICNT.EstStop: 				1 if ICNT=2 or ICNT=3 else 0; stop-controlled
		ICNT.EstRailRoad: 		1 if ICNT>3 else 0; other - railroad etc.

	Steps:
	1. add new fields
	2. populate with default values
	3. calculate v/c factor fields by setting them to estimated coefficients by facility type - freeway and arterial. Ramp and other use arterial coefficients.
	4. pupulate interchange fields by joining highway database with major interchange distance file (output from distance to interchange macro).
	5. calculate static reliability fields for freeway
	6. calculate static reliability fields for arterial, ramp, and other

	Inputs
		output\hwy.dbd
		output\MajorInterchangeDistance.csv 
		
	Outputs:
		output\hwy.dbd (modified)
 
by: nagendra.dhakar@rsginc.com 
**********************************************************************************************************/
Macro "add reliability fields"

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
   
   // **** step 1. add new fields
   
   // AB v/c factors 
   strct = strct + {{"ABLOSC_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLOSD_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLOSE_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLOSFL_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"ABLOSFH_FACT", "Real", 14, 6, "True", , , , , , , null}}
 
  // BA v/c factors 
   strct = strct + {{"BALOSC_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALOSD_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALOSE_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALOSFL_FACT", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BALOSFH_FACT", "Real", 14, 6, "True", , , , , , , null}}

   // AB Static Reliability
   strct = strct + {{"ABSTATREL_EA", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"ABSTATREL_AM", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"ABSTATREL_MD", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"ABSTATREL_PM", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"ABSTATREL_EV", "Real", 14, 6, "True", , , , , , , null}}

	 // BA Static Reliability
   strct = strct + {{"BASTATREL_EA", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"BASTATREL_AM", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"BASTATREL_MD", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"BASTATREL_PM", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"BASTATREL_EV", "Real", 14, 6, "True", , , , , , , null}}
	 
	 // interchange distance
   strct = strct + {{"INTDIST_UP", "Real", 14, 6, "True", , , , , , , null}}
	 strct = strct + {{"INTDIST_DOWN", "Real", 14, 6, "True", , , , , , , null}}

   // AB total reliability 
   strct = strct + {{"AB_TOTREL_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_TOTREL_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_TOTREL_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_TOTREL_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_TOTREL_EV", "Real", 14, 6, "True", , , , , , , null}}	 
	 
	 // BA total reliability
   strct = strct + {{"BA_TOTREL_EA", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_TOTREL_AM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_TOTREL_MD", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_TOTREL_PM", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_TOTREL_EV", "Real", 14, 6, "True", , , , , , , null}}
   
   // reliability fields
   reliability_fld = {{"ABLOSC_FACT"},{"ABLOSD_FACT"},{"ABLOSE_FACT"},{"ABLOSFL_FACT"},{"ABLOSFH_FACT"},
						{"BALOSC_FACT"},{"BALOSD_FACT"},{"BALOSE_FACT"},{"BALOSFL_FACT"},{"BALOSFH_FACT"},	 
						{"ABSTATREL_EA"},{"ABSTATREL_AM"},{"ABSTATREL_MD"},{"ABSTATREL_PM"},{"ABSTATREL_EV"},
						{"BASTATREL_EA"},{"BASTATREL_AM"},{"BASTATREL_MD"},{"BASTATREL_PM"},{"BASTATREL_EV"},
						{"AB_TOTREL_EA"},{"AB_TOTREL_AM"},{"AB_TOTREL_MD"},{"AB_TOTREL_PM"},{"AB_TOTREL_EV"},
						{"BA_TOTREL_EA"},{"BA_TOTREL_AM"},{"BA_TOTREL_MD"},{"BA_TOTREL_PM"},{"BA_TOTREL_EV"}}

	 ModifyTable(view1, strct)
	 
   // for debug
   //RunMacro("TCB Init")
   
   // **** step 2. populate with default value of 0
   for i=1 to reliability_fld.length do
   
      calcString = {"0"}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = reliability_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end 
   
   // **** step 3. calculate v/c factor fields
   
   los_fld    ={{"ABLOSC_FACT"},{"ABLOSD_FACT"},{"ABLOSE_FACT"},{"ABLOSFL_FACT"},{"ABLOSFH_FACT"},  //BA link time
								{"BALOSC_FACT"},{"BALOSD_FACT"},{"BALOSE_FACT"},{"BALOSFL_FACT"},{"BALOSFH_FACT"}}  //AB intersection time      
                
   factor_freeway     ={"0.2429","0.1705","-0.2278","-0.1983","1.022",             
												"0.2429","0.1705","-0.2278","-0.1983","1.022"} 

   factor_arterial     ={"0.1561","0.0","0.0","-0.1449","0",             
												 "0.1561","0.0","0.0","-0.1449","0"}

	facility_type = {"freeway","arterial","ramp","other"} // freeway (IFC=1), arterial (IFC=2,3), ramp (IFC=8,9), other (IFC=4,5,6,7)

	// lower and upper bounds of IFC for respective facility type = {freeway, arterial, ramp, other}											 
	lwr_bound = {"1","2","8","4"}
	upr_bound = {"1","3","9","7"}
	
   // now calculate v/c factor fields
	 for fac_type=1 to facility_type.length do
	 
		 // set factors (coefficients) for facility type
		 if fac_type=1 then factor=factor_freeway
		 else factor=factor_arterial	 
		
		 for i=1 to los_fld.length do

				query = "Select * where IFC >= " + lwr_bound[fac_type] + " and IFC <= "+upr_bound[fac_type]
				
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = los_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = factor[i]
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit
		 end
	 end
	 
	 //  **** step 4. populate interchange fields (upstream/downstream distance to major interchange)
	 
	 distance_file = outputDir+"\\MajorInterchangeDistance.csv"
	 distance_fld = {"updistance","downdistance"} 

	// interchange distance fields
	interchange_fld = {{"INTDIST_UP"},{"INTDIST_DOWN"}}

   // set initial value to 9999
   for i=1 to interchange_fld.length do
   
      calcString = {"9999"}
      
      Opts = null
      Opts.Input.[View Set] = {db_link_lyr, link_lyr}
      Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr}
      Opts.Global.Fields = interchange_fld[i]
      Opts.Global.Method = "Formula"
      Opts.Global.Parameter = calcString
      ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
      if !ok then goto quit
   end
	
	// now set to distances - in miles
	for i=1 to interchange_fld.length do
		Opts = null
		Opts.Input.[Dataview Set] = {{db_link_lyr, distance_file,{"ID"},{"LinkID"}},"JoinedView"}
		Opts.Global.Fields = interchange_fld[i]
		Opts.Global.Method = "Formula"
		Opts.Global.Parameter = distance_fld[i]
		ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
		if !ok then goto quit
	end	
	 
    //  **** step 5. calculate static reliability fields for freeway

   	// static reliability(freeway) = intercept + coeff1*ISPD70 + coeff2*1/MajorUpstream + coeff3*1/MajorDownstream

	static_fld = {{"ABSTATREL_EA"},{"ABSTATREL_AM"},{"ABSTATREL_MD"},{"ABSTATREL_PM"},{"ABSTATREL_EV"},
					{"BASTATREL_EA"},{"BASTATREL_AM"},{"BASTATREL_MD"},{"BASTATREL_PM"},{"BASTATREL_EV"}} 
	 
	 // Freeway coefficients
	 intercept = {"0.1078"}
	 speed_factor = {"0.01393"} // ISPD70
	 interchange_factor = {"0.011","0.0005445"} //MajorUpstream.Inverse, MajorDownstream.Inverse
	 
	 fac_type=1
	 factor=factor_freeway

	 for i=1 to static_fld.length do
			query = "Select * where IFC >= " + lwr_bound[fac_type] + " and IFC <= "+upr_bound[fac_type]
			
			// intercept
			
			Opts = null
			Opts.Input.[View Set] = {db_link_lyr, link_lyr}
			Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
			Opts.Global.Fields = static_fld[i]
			Opts.Global.Method = "Formula"
			Opts.Global.Parameter = intercept[1]
			ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
			if !ok then goto quit					
			
			// ISPD - add to intercept
			calcString = {"if ISPD=70 then " + static_fld[i][1] + "+" +speed_factor[1] + " else " + static_fld[i][1]}
			
			Opts = null
			Opts.Input.[View Set] = {db_link_lyr, link_lyr}
			Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
			Opts.Global.Fields = static_fld[i]
			Opts.Global.Method = "Formula"
			Opts.Global.Parameter = calcString
			ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
			if !ok then goto quit
			
			// Upstream interchange distance - apply inverse and add to intercept and ISPD
			for j=1 to interchange_factor.length do

				calcString = {"if " + interchange_fld[j][1] + "<>null then " + static_fld[i][1] + "+" +interchange_factor[j]+"*1/"+interchange_fld[j][1] + " else " + static_fld[i][1]}
				
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = static_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = calcString
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit
			end
			
	 end

	 //  **** step 6. calculate static reliability fields for arterial, ramp, and other
	 
	 // Factors (coefficients)
	 intercept = {"0.0546552"}
	 lane_factor = {"0.0","0.0103589","0.0361211","0.0446958","0.0"} //{NumLanesOneLane, NumLanesCatTwoLane, NumLanesCatThreeLane, NumLanesCatFourLanes, NumLanesFiveMoreLane}
	 speed_factor = {"0.0","0.0075674","0.0091012","0.0080996","-0.0022938","-0.0046211"} //{ISPD.CatISPD35Less (base), ISPD.CatISPD35, ISPD.CatISPD40, ISPD.CatISPD45, ISPD.CatISPD50, ISPD.CatISPD50More}
	 intersection_factor = {"0.0030973","-0.0063281","0.0127692"} //{ICNT.EstSignal, ICNT.EstStop, ICNT.EstRailRoad}
	 
	// lane fields in network
	lane_fld    ={{"ABLN_EA"},{"ABLN_AM"},{"ABLN_MD"},{"ABLN_PM"},{"ABLN_EV"},  //AB lanes
                {"BALN_EA"},{"BALN_AM"},{"BALN_MD"},{"BALN_PM"},{"BALN_EV"}}  //BA lanes    
	
	// intersection fields in network	
	intersection_fld = {{"ABCNT"},{"ABCNT"},{"ABCNT"},{"ABCNT"},{"ABCNT"},
						{"BACNT"},{"BACNT"},{"BACNT"},{"BACNT"},{"BACNT"}}
	 
	 // static reliability(arterial) = intercept + coeff1*NumLanesOneLane + coeff2*NumLanesCatTwoLane + coeff3*umLanesCatThreeLane + coeff4*NumLanesCatFourLanes + coeff5*NumLanesFiveMoreLane+
	 // 							coeff6*ISPD.CatISPD35Less + coeff7*ISPD.CatISPD35 + coeff8*ISPD.CatISPD40 + coeff9*ISPD.CatISPD45 + coeff1*ISPD.CatISPD50 + coeff10*ISPD.CatISPD50More+
	 // 							coeff11*ICNT.EstSignal + coeff12*ICNT.EstStop + coeff13*ICNT.EstRailRoad
	 
	 for fac_type=2 to facility_type.length do
		
		// selection query - to identify links with a facility type
		 query = "Select * where IFC >= " + lwr_bound[fac_type] + " and IFC <= "+upr_bound[fac_type]
			
		 for i=1 to static_fld.length do
				
				// intercept
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = static_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = intercept[1]
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit					
				
				// NumLanes Factors
				calcString = {"if " + lane_fld[i][1] + "=1 then "+static_fld[i][1] + "+" +lane_factor[1]+
											" else if " + lane_fld[i][1] + "=2 then "+static_fld[i][1] + "+" +lane_factor[2]+
											" else if " + lane_fld[i][1] + "=3 then "+static_fld[i][1] + "+" +lane_factor[3]+
											" else if " + lane_fld[i][1] + "=4 then "+static_fld[i][1] + "+" +lane_factor[4]+ 
											" else "+static_fld[i][1] + "+" +lane_factor[5]}
				
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = static_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = calcString
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit

				// Speed Factors - 
				calcString = {"if ISPD<35 then "+static_fld[i][1] + "+" +speed_factor[1]+
											" else if ISPD=35 then "+static_fld[i][1] + "+" +speed_factor[2]+
											" else if ISPD=40 then "+static_fld[i][1] + "+" +speed_factor[3]+
											" else if ISPD=45 then "+static_fld[i][1] + "+" +speed_factor[4]+ 
											" else if ISPD=50 then "+static_fld[i][1] + "+" +speed_factor[5]+ 
											" else "+static_fld[i][1] + "+" +speed_factor[6]}
				
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = static_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = calcString
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit				

				// Intersection Factors
				calcString = {"if " + intersection_fld[i][1]+ "=1 then " + static_fld[i][1] + "+" +intersection_factor[1]+
											" else if " + intersection_fld[i][1]+ "=2 or " + intersection_fld[i][1]+ "=3 then " + static_fld[i][1] + "+" + intersection_factor[2]+
											" else if " + intersection_fld[i][1]+ ">3 then " + static_fld[i][1] + "+" + intersection_factor[3] +
											" else " + static_fld[i][1]}
				
				Opts = null
				Opts.Input.[View Set] = {db_link_lyr, link_lyr}
				Opts.Input.[Dataview Set] = {db_link_lyr, link_lyr, "Selection" , query}
				Opts.Global.Fields = static_fld[i]
				Opts.Global.Method = "Formula"
				Opts.Global.Parameter = calcString
				ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
				if !ok then goto quit	
				
		 end
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
2     Prime Arterial     2.5              2               2           2                2                 2             2                  2
3     Major Arterial     2                2               2           2                2                 2             2                  2
4     Collector          2                2               1.5         1.5              1.5               1.5           1.5                1.5
5     Local Collector    2                2               1.5         1.25             1.25              1.25          1.25               1.25
6     Rural Collector    2                2               1.5         1.25             1.25              1.25          1.25               1.25
7     Local Road         2                2               1.5         1.25             1.25              1.25          1.25               1.25
8     Freeway connector  2                2               1.5         1.25             1.25              1.25          1.25               1.25
9     Local Ramp         2                2               1.5         1.25             1.25              1.25          1.25               1.25

Ramp with meter (abcnt = 4 or 5)    
   Cycle length = 2.5
   GC ratio 0.42 (adjusted down from 0.5 for yellow)

Stop controlled intersection
   Cycle length =1.25
   GC ratio 0.42 (adjusted down from 0.5 for yellow)

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
      
            //defaults are 1.25 minute cycle length and 1.0 progression factor
            c_len = 1.25
            p_factor = 1.0
               
            //set up the cycle length for AB direction if there is a gc ratio and more than 2 links 
            if (link_lyr.[ABGC]<>0 and signal > 0) then do 
            
               if (link_lyr.[IFC] = 2) then do
                  if (max_lc = 2)      then c_len = 2.5       //Prime arterial & Prime arterial
                  else                      c_len = 2.0       //Prime arterial & anything lower
               end
               else if (link_lyr.[IFC] = 3) then do
                  if (max_lc > 3)      then c_len = 2.0       //Major arterial & anything lower than a Major arterial
                  else                      c_len = 2.0       //Major arterial & Prime arterial or Major arterial
               end
               else if (link_lyr.[IFC] = 4) then do
                  if (min_lc < 4)      then c_len = 2.0       //Anything lower than a Major arterial & Prime arterial 
                  else                      c_len = 1.5       //Anything lower than a Major arterial & anything lower than a Prime arterial
               end
               else if (link_lyr.[IFC] > 4) then do
                  if (min_lc < 4)      then c_len = 2.0
                  if (min_lc = 4)      then c_len = 1.5
                  if (min_lc > 4)      then c_len = 1.25
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

               //defaults are 0.4 gc ratio, 1.25 minute cycle length and 1.0 progression factor
               gc_ratio = 0.4
               c_len = 1.25
               p_factor = 1.0
                  
               if (link_lyr.[IFC] = 2) then do
                  if (max_lc = 2)      then c_len = 2.5       //Prime arterial & Prime arterial
                  else                      c_len = 2.0       //Prime arterial & anything lower
               end
               else if (link_lyr.[IFC] = 3) then do
                  if (max_lc > 3)      then c_len = 2.0       //Major arterial & anything lower than a Major arterial
                  else                      c_len = 2.0       //Major arterial & Prime arterial or Major arterial
               end
               else if (link_lyr.[IFC] = 4) then do
                  if (min_lc < 4)      then c_len = 2.0       //Anything lower than a Major arterial & Prime arterial 
                  else                      c_len = 1.5       //Anything lower than a Major arterial & anything lower than a Prime arterial
               end
               else if (link_lyr.[IFC] > 4) then do
                  if (min_lc < 4)      then c_len = 2.0
                  if (min_lc = 4)      then c_len = 1.5
                  if (min_lc > 4)      then c_len = 1.25
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
            link_lyr.[AB_Cycle] = 2.5
            link_lyr.[AB_GCRatio] = 0.42
            link_lyr.[AB_PF] = 1.0
         end
         
          //code metered ramps BA Direction
         if(ends_at_node_BA_direction = 1 and (link_lyr.[BACNT]= 4 or link_lyr.[BACNT] = 5)) then do
            link_lyr.[BA_Cycle] = 2.5
            link_lyr.[BA_GCRatio] = 0.42
            link_lyr.[BA_PF] = 1.0
         end

        //code stops AB Direction
         if(ends_at_node_AB_direction = 1 and (link_lyr.[ABCNT]= 2 or link_lyr.[ABCNT] = 3)) then do
            link_lyr.[AB_Cycle] = 1.25
            link_lyr.[AB_GCRatio] = 0.42
            link_lyr.[AB_PF] = 1.0
         end         
         
         //code stops BA Direction
         if(ends_at_node_BA_direction = 1 and (link_lyr.[BACNT]= 2 or link_lyr.[BACNT] = 3)) then do
            link_lyr.[BA_Cycle] = 1.25
            link_lyr.[BA_GCRatio] = 0.42
            link_lyr.[BA_PF] = 1.0
         end         
         
      end   // end for links
      
   end // end for nodes

      
   // set alpha1 and beta1 fields, which are based upon free-flow speed to match POSTLOAD loaded time factors
   lwr_bound  = {  " 0",  "25",  "30",  "35",  "40",  "45",  "50",  "55",  "60",  "65",  "70",  "75"}   
   upr_bound  = {  "24",  "29",  "34",  "39",  "44",  "49",  "54",  "59",  "64",  "69",  "74",  "99"}  
   alpha1     = {"0.8","0.8","0.8","0.8","0.8","0.8","0.8","0.8","0.8","0.8","0.8","0.8"}  
   beta1      = {   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4",   "4"  , "4",   "4"}  

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
   alpha2_default = "4.5"
   beta2_default = "2.0"
   alpha2_meter = "6.0"
   beta2_meter = "2.0"
      
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
        {"*LN_EA",   link_lyr + ".ABLN_EA",   link_lyr + ".BALN_EA"},
        {"*LN_AM",   link_lyr + ".ABLN_AM",   link_lyr + ".BALN_AM"},
        {"*LN_MD",   link_lyr + ".ABLN_MD",   link_lyr + ".BALN_MD"},
        {"*LN_PM",   link_lyr + ".ABLN_PM",   link_lyr + ".BALN_PM"},
        {"*LN_EV",   link_lyr + ".ABLN_EV",   link_lyr + ".BALN_EV"},
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
        {"*LHCST_EA", link_lyr+".ABLHCST_EA", link_lyr+".BALHCST_EA"}, 
        {"*LHCST_AM", link_lyr+".ABLHCST_AM", link_lyr+".BALHCST_AM"}, 
        {"*LHCST_MD", link_lyr+".ABLHCST_MD", link_lyr+".BALHCST_MD"}, 
        {"*LHCST_PM", link_lyr+".ABLHCST_PM", link_lyr+".BALHCST_PM"}, 
        {"*LHCST_EV", link_lyr+".ABLHCST_EV", link_lyr+".BALHCST_EV"}, 
        {"*MHCST_EA", link_lyr+".ABMHCST_EA", link_lyr+".BAMHCST_EA"}, 
        {"*MHCST_AM", link_lyr+".ABMHCST_AM", link_lyr+".BAMHCST_AM"}, 
        {"*MHCST_MD", link_lyr+".ABMHCST_MD", link_lyr+".BAMHCST_MD"}, 
        {"*MHCST_PM", link_lyr+".ABMHCST_PM", link_lyr+".BAMHCST_PM"}, 
        {"*MHCST_EV", link_lyr+".ABMHCST_EV", link_lyr+".BAMHCST_EV"}, 
        {"*HHCST_EA", link_lyr+".ABHHCST_EA", link_lyr+".BAHHCST_EA"}, 
        {"*HHCST_AM", link_lyr+".ABHHCST_AM", link_lyr+".BAHHCST_AM"}, 
        {"*HHCST_MD", link_lyr+".ABHHCST_MD", link_lyr+".BAHHCST_MD"}, 
        {"*HHCST_PM", link_lyr+".ABHHCST_PM", link_lyr+".BAHHCST_PM"}, 
        {"*HHCST_EV", link_lyr+".ABHHCST_EV", link_lyr+".BAHHCST_EV"}, 
        {"*CVCST_EA", link_lyr+".ABCVCST_EA", link_lyr+".BACVCST_EA"}, 
        {"*CVCST_AM", link_lyr+".ABCVCST_AM", link_lyr+".BACVCST_AM"}, 
        {"*CVCST_MD", link_lyr+".ABCVCST_MD", link_lyr+".BACVCST_MD"}, 
        {"*CVCST_PM", link_lyr+".ABCVCST_PM", link_lyr+".BACVCST_PM"}, 
        {"*CVCST_EV", link_lyr+".ABCVCST_EV", link_lyr+".BACVCST_EV"}, 
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
        {"*PRELOAD_EV", link_lyr+".ABPRELOAD_EV", link_lyr+".BAPRELOAD_EV"},
				{"*LOSC_FACT", link_lyr+".ABLOSC_FACT", link_lyr+".BALOSC_FACT"},          // added for reliability - 02/02/2016
				{"*LOSD_FACT", link_lyr+".ABLOSD_FACT", link_lyr+".BALOSD_FACT"},          // added for reliability - 02/02/2016
				{"*LOSE_FACT", link_lyr+".ABLOSE_FACT", link_lyr+".BALOSE_FACT"},          // added for reliability - 02/02/2016
				{"*LOSFL_FACT", link_lyr+".ABLOSFL_FACT", link_lyr+".BALOSFL_FACT"},       // added for reliability - 02/02/2016
				{"*LOSFH_FACT", link_lyr+".ABLOSFH_FACT", link_lyr+".BALOSFH_FACT"},       // added for reliability - 02/02/2016
				{"*STATREL_EA", link_lyr+".ABSTATREL_EA", link_lyr+".BASTATREL_EA"},       // added for reliability - 02/02/2016
				{"*STATREL_AM", link_lyr+".ABSTATREL_AM", link_lyr+".BASTATREL_AM"},       // added for reliability - 02/02/2016
				{"*STATREL_MD", link_lyr+".ABSTATREL_MD", link_lyr+".BASTATREL_MD"},       // added for reliability - 02/02/2016
				{"*STATREL_PM", link_lyr+".ABSTATREL_PM", link_lyr+".BASTATREL_PM"},       // added for reliability - 02/02/2016
				{"*STATREL_EV", link_lyr+".ABSTATREL_EV", link_lyr+".BASTATREL_EV"},
				{"*_TOTREL_EA", link_lyr+".AB_TOTREL_EA", link_lyr+".BA_TOTREL_EA"},
				{"*_TOTREL_AM", link_lyr+".AB_TOTREL_AM", link_lyr+".BA_TOTREL_AM"},
				{"*_TOTREL_MD", link_lyr+".AB_TOTREL_MD", link_lyr+".BA_TOTREL_MD"},
				{"*_TOTREL_PM", link_lyr+".AB_TOTREL_PM", link_lyr+".BA_TOTREL_PM"},
				{"*_TOTREL_EV", link_lyr+".AB_TOTREL_EV", link_lyr+".BA_TOTREL_EV"}}       // added for reliability - 02/02/2016    
        
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
   Opts.Global.[Spc Turn Pen Method] = 3
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
/*************************************************************************************
Calculate upstream and downsstream distances to major interchanges for freeway segments

steps:
1. identify major interchange nodes
2. for each freeway segment, calculate upstream and downstream segment

input:
	output\\hwy.dbd
	
output:
	output\\MajorInterchangeDistance.csv
	
by: nagendra.dhakar@rsginc.com
*************************************************************************************/
Macro "DistanceToInterchange"
   shared path, inputDir, outputDir
	 shared interchanges, freeways, linklayer, nodelayer
	 
   ok=0
	
	// input highway database
   db_file=outputDir+"\\hwy.dbd"
	 hwy_dbd=db_file

	 // output settings
	 out_file = outputDir+"\\MajorInterchangeDistance.csv"

	// add layers
	layers = GetDBLayers(hwy_dbd)
	linklayer=layers[2]
	nodelayer=layers[1]

	db_linklayer=hwy_dbd+"|"+linklayer
	db_nodelayer=hwy_dbd+"|"+nodelayer

	info = GetDBInfo(hwy_dbd)
	temp_map = CreateMap("temp",{{"scope",info[1]}})

	temp_layer = AddLayer(temp_map,linklayer,hwy_dbd,linklayer)
	temp_layer = AddLayer(temp_map,nodelayer,hwy_dbd,nodelayer)

	// Identify Interchanges
	SetLayer(linklayer)

	// Major interchange
	query_ramps = 'Select * where (IFC=8) AND position(NM,"HOV")=0' // Major interchange - HOV access connectors are removed
	MaxLinks = 50

	on Error  do ShowMessage("The SQL query: (" + query_ramps + ") is not correct.") end
	VerifyQuery(query_ramps)

	nramps = SelectByQuery("Ramp Set", "Several", query_ramps,)

	query_freeways = "Select * where IFC=1"
	on Error do ShowMessage("The SQL query: (" + query_freeways + ") is not correct.") end
	VerifyQuery(query_freeways)

	nfreeways = SelectByQuery("Freeway Set", "Several",query_freeways,)

	// get freeway segments ids, IHOV, ANode BNode
	freeways = GetDataVectors("Freeway Set", {"ID","IHOV"},)

	// intersect with nodes to select connected nodes
	SetLayer(nodelayer)
	nnodes = SelectByLinks("Interchange Set", "Several", "Ramp Set")
	ninterchanges = SelectByLinks("Interchange Set", "Subset", "Freeway Set")

	// get interchange ids
	interchanges = GetDataVector("Interchange Set","ID",)

	outfile = OpenFile(out_file, "w")
	WriteLine(outfile, JoinStrings({"LinkID","Length","updistance","downdistance","ihov","UpLinks","DownLinks"},","))

	on Error goto quit
	
	// loop through the freeway segments
	dim upstreamlinkset[freeways[1].Length,MaxLinks-1], downstreamlinkset[freeways[1].Length,MaxLinks-1]

	CreateProgressBar("Calculating Interchange Distances", "True")

	for i=1 to freeways[1].Length do
		perc=RealToInt(100*i/freeways[1].Length)
		
		UpdateProgressBar("Calculating Interchange Distances for LinkID: " +string(freeways[1][i]), perc)
		
		SetLayer(linklayer)
		linkid = freeways[1][i]
		ihov = freeways[2][i]
		
		query = "Select * where ID="+String(linkid)
		count = SelectByQuery("Selection", "Several", query,)
		linklength = GetDataVector("Selection", "Length",)
		
		nodes = GetEndPoints(linkid)
		FromNode=nodes[1]
		ToNode=nodes[2]
		
		// upstream - from node
		isInterchange = RunMacro("NodeIsInterchange", FromNode)
			
		BaseLink = linkid
		j=1
		
		query1="n/a"
		query2="n/a"
		upstreamdistance=linklength[1]*0.5
		downstreamdistance=linklength[1]*0.5
		numupstreamlinks=0
		numdownstreamlinks=0

		if (ihov<>2) then do
			while (isInterchange=0) do
				SetLayer(nodelayer)
				links = GetNodeLinks(FromNode)  // links set that meet at the node
				
				coordinates_base=GetCoordsFromLinks(linklayer, , {{BaseLink,1}})
				
				// find the freeway link that is not the current link
				upstreamlink = RunMacro("FindNextLink",links,BaseLink)
				
				if (upstreamlink <> null) then do
				
					coordinates_upstream=GetCoordsFromLinks(linklayer, , {{upstreamlink,1}})
					opposite = RunMacro("IsOppositeDirection",coordinates_base[1],coordinates_upstream[1]) // 1- true, 0- false
					
					if (opposite=0 and j<MaxLinks) then do
					
						SetLayer(linklayer)
						nodes = GetEndPoints(upstreamlink)    // node pairs - from and to nodes
						FromNode = nodes[1] // ToNode is always going to be the previous node
						
						isInterchange = RunMacro("NodeIsInterchange", FromNode)		
						BaseLink = upstreamlink
						
						// if no new freeway link then end the process - happens when freeway connects to other facility type
						if (j>1 and upstreamlink= linkid) then isInterchange=1
						else do 
							upstreamlinkset[i][j] = upstreamlink
							j=j+1
						end
					end
					else do 
						isInterchange=1
						if (j=MaxLinks) then upstreamdistance=99
					end
				end
				else do
					isInterchange=1
					if (j=MaxLinks) then upstreamdistance=99
				end
				
			end
			
			numupstreamlinks =j-1
			
			// calculate upstream distance
			SetLayer(linklayer)
			
			if j>=2 then do
				query1 = "Select * where ID="+String(upstreamlinkset[i][1])
				
				if j>2 then do
					for iter=2 to numupstreamlinks do
						query1 = JoinStrings({query1," or ID=",r2s(upstreamlinkset[i][iter])},"")
					end
				end
				
				count = SelectByQuery("Selection", "Several", query1,)
				lengths = GetDataVector("Selection", "Length",)
				upstreamdistance=VectorStatistic(lengths,"Sum",)
				
				// add half of the current link length to make the distance from midpoint
				upstreamdistance = upstreamdistance + (linklength[1]*0.5)
			end

			
			// downstream - to node
			isInterchange = RunMacro("NodeIsInterchange", ToNode)
			
			BaseLink = linkid
			j=1
			while (isInterchange=0) do
				SetLayer(nodelayer)
				links = GetNodeLinks(ToNode)
				
				coordinates_base=GetCoordsFromLinks(linklayer, , {{BaseLink,-1}})
				
				// find the freeway link that is not the current link
				downstreamlink = RunMacro("FindNextLink",links,BaseLink)
				
				if (downstreamlink <> null) then do
					coordinates_downstream=GetCoordsFromLinks(linklayer, , {{downstreamlink,-1}})
					opposite = RunMacro("IsOppositeDirection",coordinates_base[1],coordinates_downstream[1]) // 1- true, 0- false
					
					if (opposite=0 and j<MaxLinks) then do
					
						SetLayer(linklayer)
						nodes = GetEndPoints(downstreamlink)
						ToNode = nodes[2] // FromNode is always going to be the previous node
						
						isInterchange = RunMacro("NodeIsInterchange", ToNode)
						BaseLink = downstreamlink

						// if no new freeway link then end the process - happens when freeway connects to other facility type
						if (j>1 and downstreamlink=linkid) then isInterchange=1
						else do 
							downstreamlinkset[i][j] = downstreamlink
							j=j+1
						end
						
					end
					else do 
						isInterchange=1
						if (j=MaxLinks) then downstreamdistance=99
					end
				end
				else do
					isInterchange=1
					if (j=MaxLinks) then downstreamdistance=99
				end
				
			end	
			numdownstreamlinks =j-1
			
			// calculate downstream distance
			SetLayer(linklayer)
			
			if j>=2 then do
				query2 = "Select * where ID="+String(downstreamlinkset[i][1])
				
				if j>2 then do
					for iter=2 to numdownstreamlinks do
						query2 = JoinStrings({query2," or ID=",r2s(downstreamlinkset[i][iter])},"")
					end
				end
				
				count = SelectByQuery("Selection", "Several", query2,)
				lengths = GetDataVector("Selection", "Length",)
				downstreamdistance=VectorStatistic(lengths,"Sum",)
				
				// add half of the current link length to make the distance from midpoint
				downstreamdistance = downstreamdistance + (linklength[1]*0.5)
				
			end
			
		end
		
		else do
			// HOV segments - set default value of 9999 miles. Distances are not calculated as HOV segments are pretty reliable.
			upstreamdistance = 9999
			downstreamdistance = 9999
			numupstreamlinks = 9999
			numdownstreamlinks = 9999		
			
		end
		
		WriteLine(outfile, JoinStrings({i2s(linkid),r2s(linklength[1]),r2s(upstreamdistance),r2s(downstreamdistance),i2s(ihov),i2s(numupstreamlinks),i2s(numdownstreamlinks)},","))

	end
	
	CloseFile(outfile)

	DestroyProgressBar()
	ok=1
	return(ok)

	quit:
	showmessage("Error, i: " + string(i) + ", j: " + string(j) + ", linkid: " + string(linkid))

EndMacro
/*************************************************************************************
Check if the nodes is an interchange: 0-No, 1- Yes
**************************************************************************************/
Macro "NodeIsInterchange" (nodeid)
	shared interchanges

	isInter = 0

	for i=1 to interchanges.Length do
		if nodeid=interchanges[i] then isInter = 1
	end

	return (isInter)

EndMacro
/*************************************************************************************
Identify forward links:
	Identify links that are not the previous link (linkid)
	Selects the link that is also a freeway and assign that as the next link
	Assumption: there is only one next freeway link
**************************************************************************************/	
Macro "FindNextLink" (linkset,linkid)
	shared linklayer
	
	nextlink = null

	for i=1 to linkset.Length do
		if linkid <> linkset[i] then do
			IsFreeway = RunMacro("LinkIsFreeway",linkset[i])
			if (IsFreeway=1) then do 
				IsWrongDirection = RunMacro("WrongDirection",linkset[i],linkid)
				IsHov = RunMacro("LinkIsHov",linkset[i])
				if (IsHov=0 & IsWrongDirection=0) then nextlink = linkset[i]
			end
		end
	end

	return (nextlink)

EndMacro
/*************************************************************************************
Check if the link is in the wrong direction
**************************************************************************************/
Macro "WrongDirection" (link,linkid)
	shared linklayer
	
	SetLayer(linklayer)
	nodes1 = GetEndPoints(linkid)
	nodes2 = GetEndPoints(link)
	
	tonode1 = nodes1[2]    // base link
	tonode2 = nodes2[2]    // next link
	
	// compare ToNode - if they are same then wrong direction
	if tonode1 = tonode2 then return(1)
	else return(0)

EndMacro

/*************************************************************************************
Check if the links is a freeway link: 0-No, 1- Yes
**************************************************************************************/
Macro "LinkIsFreeway" (link)
	shared freeways

	freeway=0
	for j=1 to freeways[1].Length do
		if link = freeways[1][j] then freeway=1
	end

	return (freeway)

EndMacro
/*************************************************************************************
Check if link is a HOV segment
**************************************************************************************/
Macro "LinkIsHov" (link)
	shared freeways

	hov=0
	for j=1 to freeways[1].Length do
		if link = freeways[1][j] then do
			if freeways[2][j]=2 then hov=1
		end
	end

	return(hov)

EndMacro
/*************************************************************************************
Find link direction (coordinates as input)
**************************************************************************************/
Macro "GetLinkDirection" (coordinates)

	maxnum = coordinates.length 

	nodeA = coordinates[1]
	nodeB = coordinates[maxnum]

	deltaX = nodeB.lon-nodeA.lon
	deltaY = nodeB.lat-nodeA.lat

	if deltaY>0 then slope1 = deltaX/deltaY
	else slope1 = deltaX

	if deltaX>0 then slope2 = deltaY/deltaX
	else slope2 = deltaY

	direction = ""
	if abs(slope1) > abs(slope2) then do
		//pre_dir = "EW"
		if deltaX<0 then direction = "WB"
		else direction = "EB"
	end
	else do 
		//pre_dir = "NS"
		if deltaY<0 then direction = "SB"
		else direction = "NB"
	end

	return(direction)

EndMacro
/*************************************************************************************
Check if the two segments (coordinates as input) have opposite direction
**************************************************************************************/
Macro "IsOppositeDirection" (coordinates1, coordinates2)

	direction1 = RunMacro("GetLinkDirection",coordinates1)
	direction2 = RunMacro("GetLinkDirection",coordinates2)

	if direction1="NB" and direction2="SB" then return(1)
	else if direction2="NB" and direction1="SB" then return(1)
	else if direction1="EB" and direction2="WB" then return(1)
	else if direction2="EB" and direction1="WB" then return(1)
	else return(0)

EndMacro
