/***********************************************
Hwy skim all

This macro calls macro "Update highway network", which updates highway network with times
from last highway assignment, and then skims highway network by calling macro "hwy skim" for
the following modes:

dant  Drive-alone non-toll
dat   Drive-alone toll
s2nh  Shared-2 non-toll HOV
s2th  Shared-2 toll HOV
s3nh  Shared-3 non-toll HOV
s3th  Shared-3 toll HOV
truck Truck

***********************************************/
Macro "Hwy skim all" (args)

   skimByVOT= args[1]
 
   if skimByVOT="false" then do 
   
     da_vot=67.00 // $0.67 cents per minute VOT ($40.2 per hour)
     s2_vot=67.00
     s3_vot=67.00
     lh_vot=67.00
     mh_vot=68.00
     hh_vot=89.00
     cv_vot=67.00
  
     vot_array = {da_vot, s2_vot, s3_vot, lh_vot, mh_vot, hh_vot, cv_vot}
     
     ok=RunMacro("Update highway network", vot_array)
     if !ok then goto quit
      
     ok=RunMacro("hwy skim",{"dant",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"dat",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"s2nh",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"s2th",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"s3nh",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"s3th",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"cvn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"cvt",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"lhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"lhdt",}) 
     if !ok then goto quit
     
     ok=RunMacro("hwy skim",{"mhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"mhdt",}) 
     if !ok then goto quit
     
     ok=RunMacro("hwy skim",{"hhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"hhdt",}) 
     if !ok then goto quit
     

    ok=RunMacro("hwy skim",{"truck",}) 
     if !ok then goto quit

   end
   else do

    vot_bins = {"low", "med", "high"}
                  //  da,   s2,   s3,   lh,   mh,   hh,   cv}
     vot_by_bin = {{16.6, 16.6, 16.6, 67.0, 68.0, 89.0, 67.0},
                   {33.3, 33.3, 33.3, 67.0, 68.0, 89.0, 67.0},
                   { 100,  100,  100, 67.0, 68.0, 89.0, 67.0}
                  }
				  
     for i = 1 to vot_bins.length do
     
       ok=RunMacro("Update highway network", vot_by_bin[i])
       if !ok then goto quit
      
       ok=RunMacro("hwy skim",{"dant",vot_bins[i]}) 
       if !ok then goto quit
			 
       ok=RunMacro("hwy skim",{"dat",vot_bins[i]}) 
       if !ok then goto quit

       ok=RunMacro("hwy skim",{"s2nh",vot_bins[i]}) 
       if !ok then goto quit

       ok=RunMacro("hwy skim",{"s2th",vot_bins[i]}) 
       if !ok then goto quit

       ok=RunMacro("hwy skim",{"s3nh",vot_bins[i]})
       if !ok then goto quit

       ok=RunMacro("hwy skim",{"s3th",vot_bins[i]}) 
       if !ok then goto quit

/*	   
			 // reliability for time periods (15-mins)
			 for tod=1 to 96 do
				
				 if (tod=25 or tod=26 or tod=35 or tod=36) then do //AM Period - 30 min shoulders
					 ok=RunMacro("hwy skim time bins",{"dant",vot_bins[i],tod}) 
					 if !ok then goto quit

					 ok=RunMacro("hwy skim time bins",{"dat",vot_bins[i],tod}) 
					 if !ok then goto quit

					 ok=RunMacro("hwy skim time bins",{"s2nh",vot_bins[i],tod}) 
					 if !ok then goto quit

					 ok=RunMacro("hwy skim time bins",{"s2th",vot_bins[i],tod}) 
					 if !ok then goto quit

					 ok=RunMacro("hwy skim time bins",{"s3nh",vot_bins[i],tod})
					 if !ok then goto quit

					 ok=RunMacro("hwy skim time bins",{"s3th",vot_bins[i],tod}) 
					 if !ok then goto quit
				 end

			 end
 */    
     end

     // don't skim commercial vehicles or trucks by vot
     ok=RunMacro("hwy skim",{"cvn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"cvt",}) 
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"lhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"lhdt",}) 
     if !ok then goto quit
     
     ok=RunMacro("hwy skim",{"mhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"mhdt",}) 
     if !ok then goto quit
     
     ok=RunMacro("hwy skim",{"hhdn",})
     if !ok then goto quit

     ok=RunMacro("hwy skim",{"hhdt",}) 
     if !ok then goto quit
    
     ok=RunMacro("hwy skim",{"truck",}) 
     if !ok then goto quit

	 end   

   return(1)
   
   quit:
      return(0)
  
EndMacro

/********************************************************************************

Update highway network

Updates highway line layer and network with fields from latest assignment flow tables.  

 Arguments
   1 drive-alone value-of-time (cents/min)
   2 shared 2  value-of-time (cents/min)
   3 shared 3+ value-of-time (cents/min)
   4 light-heavy truck value-of-time (cents/min)
   5 medium-heavy truck value-of-time (cents/min)
   6 heavy-heavy truck value-of-time (cents/min)
   7 commercial vehicle value-of-time (cents/min)


The following fields are updated on the line layer:

Field       Description
-------     ---------------
STM         SOV time
HTM         HOV time
SCST        SOV generalized cost
H2CST       Shared-2 generalized cost
H3CST       Shared-3 generalized cost
LHCST       Light-heavy truck generalized cost
MHCST       Medium-heavy truck generalized cost
HHCST       Heavy-heavy truck generalized cost
CVCST       Heavy-heavy truck generalized cost


Each field is xxField_yy where:

 xx is AB or BA indicating direction
 yy is period, as follows:
   EA: Early AM
   AM: AM peak
   MD: Midday
   PM: PM peak
   EV: Evening
   
Inputs:
   input\hwy.dbd           Highway line layer
   input\hwy.net           Highway network
   output\hwyload_yy.bin   Loaded flow table from assignment, one per period (yy)

Outputs:
   output\hwy.dbd          Updated highway line layer
   output\hwy.net          Updated highway network

********************************************************************************/
Macro "Update highway network" (args) 

   shared path, inputDir, outputDir
   
   da_vot= args[1]
   s2_vot= args[2]
   s3_vot= args[3]
   lh_vot= args[4]
   mh_vot= args[5]
   hh_vot= args[6]
   cv_vot= args[7]
  
    // input files
   db_file = outputDir + "\\hwy.dbd"
   net_file = outputDir + "\\hwy.net" 

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   db_node_lyr = db_file + "|" + node_lyr

   periods = {"_EA", "_AM", "_MD", "_PM", "_EV"}
  
   da_vot=da_vot*60/100   //Convert to dollars per hour VOT so don't have to change gen cost function below
   s2_vot=s2_vot*60/100
   s3_vot=s3_vot*60/100
   lh_vot=lh_vot*60/100
   mh_vot=mh_vot*60/100
   hh_vot=hh_vot*60/100
   cv_vot=cv_vot*60/100
    
   //Recompute generalized cost using MSA cost in flow table,
   for i = 1 to periods.length do
    
      flowTable = outputDir+"\\hwyload"+periods[i]+".bin"
        
      // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"sovtime"+periods[i] }   
      Opts.Global.Fields = {"ABSTM"+periods[i],"BASTM"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost"  ,
                               "BA_MSA_Cost"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
         
       // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"hovtime"+periods[i] }   
      Opts.Global.Fields = {"ABHTM"+periods[i],"BAHTM"+periods[i]}                                // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost"  ,
                               "BA_MSA_Cost"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
      
      // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"dajoin"+periods[i] }   
      Opts.Global.Fields = {"ABSCST"+periods[i],"BASCST"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(da_vot)+"*60)"  ,
                               "BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(da_vot)+"*60)"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
         
     // Light-Heavy truck cost
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"dajoin"+periods[i] }   
      Opts.Global.Fields = {"ABLHCST"+periods[i],"BALHCST"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(lh_vot)+"*60)"  ,
                               "BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(lh_vot)+"*60)"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
    
    // Medium-Heavy truck cost
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"dajoin"+periods[i] }   
      Opts.Global.Fields = {"ABMHCST"+periods[i],"BAMHCST"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(mh_vot)+"*60)"  ,
                               "BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(mh_vot)+"*60)"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
    
      // Heavy-Heavy truck cost
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"dajoin"+periods[i] }   
      Opts.Global.Fields = {"ABHHCST"+periods[i],"BAHHCST"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(hh_vot)+"*60)"  ,
                               "BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(hh_vot)+"*60)"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
      
      // Commercial vehicle cost
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"dajoin"+periods[i] }   
      Opts.Global.Fields = {"ABCVCST"+periods[i],"BACVCST"+periods[i]}                                  // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(cv_vot)+"*60)"  ,
                               "BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(cv_vot)+"*60)"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
        
           
       // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"s2join"+periods[i] }   
      Opts.Global.Fields = {"ABH2CST"+periods[i],"BAH2CST"+periods[i]}                                // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"if (IHOV=3 or IHOV=4) then (AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(s2_vot)+"*60)) else AB_MSA_Cost + (COST/100)/"+String(s2_vot)+"*60"  ,
                               "if (IHOV=3 or IHOV=4) then (BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(s2_vot)+"*60)) else BA_MSA_Cost + (COST/100)/"+String(s2_vot)+"*60"  }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
      
       // The Dataview Set is a joined view of the link layer and the flow table, based on link ID
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"s3join"+periods[i] }   
      Opts.Global.Fields = {"ABH3CST"+periods[i],"BAH3CST"+periods[i]}                                // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"if IHOV=4 then (AB_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(s3_vot)+"*60)) else AB_MSA_Cost + (COST/100)/"+String(s2_vot)+"*60"   ,
                               "if IHOV=4 then (BA_MSA_Cost + ((ITOLL3"+periods[i]+"/100)/"+String(s3_vot)+"*60)) else BA_MSA_Cost + (COST/100)/"+String(s2_vot)+"*60"   }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit
			
			// Total Reliability - The Dataview Set is a joined view of the link layer and the flow table, based on link ID
			// calculate as square of link reliability - after skimming take square root of the total reliability
      Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"reliabilityjoin"+periods[i] }   
      Opts.Global.Fields = {"AB_TOTREL"+periods[i],"BA_TOTREL"+periods[i]}                                // the field to fill
      Opts.Global.Method = "Formula"                                                                  // the fill method
      Opts.Global.Parameter = {"pow(AB_MSA_Cost - AB_MSA_Time,2)",
                               "pow(BA_MSA_Cost - BA_MSA_Time,2)" }   
      ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
      if !ret_value then goto quit	
      
      //Now update the network with the calculated cost fields
       
      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*STM"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABSTM"+periods[i],link_lyr+".BASTM"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit
 

      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*HTM"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABHTM"+periods[i],link_lyr+".BAHTM"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit
       
      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*SCST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABSCST"+periods[i],link_lyr+".BASCST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit
 

      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*H2CST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABH2CST"+periods[i],link_lyr+".BAH2CST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit
         

      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*H3CST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABH3CST"+periods[i],link_lyr+".BAH3CST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit                

     Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*LHCST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABLHCST"+periods[i],link_lyr+".BALHCST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit                

     Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*MHCST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABMHCST"+periods[i],link_lyr+".BAMHCST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit                

     Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*HHCST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABHHCST"+periods[i],link_lyr+".BAHHCST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit                

     Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*CVCST"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".ABCVCST"+periods[i],link_lyr+".BACVCST"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit                

			// update total reliability fields 
      Opts = null
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file
      Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
      Opts.Global.[Fields Indices] = "*_TOTREL"+periods[i]
      Opts.Global.Options.[Link Fields] = { {link_lyr+".AB_TOTREL"+periods[i],link_lyr+".BA_TOTREL"+periods[i] } }
      Opts.Global.Options.Constants = {1}
      ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
      if !ret_value then goto quit    			
		 
    end   
   
 
   ok=1
   runmacro("close all")
   quit:   

   return(ok)   
   
EndMacro

/********************************************************************************************

hwy skim

Skim highway network for the following modes (passed as argument)


Mode     Description                  Cost attribute
----     ---------------------        --------------
truck    Truck                        SCST
lhdn     Light-heavy-duty non-toll    LHCST
mhdn     Medium-heavy-duty non-toll   MHCST
hhdn     Heavy-heavy-duty non-toll    HHCST
lhdt     Light-heavy-duty toll        LHCST
mhdt     Medium-heavy-duty toll       MHCST
hhdt     Heavy-heavy-duty toll        HHCST
cvn      Commercial vehicle non-toll  CVCST
cvt      Commercial vehicle toll      CVCST
dant     Drive-alone non-toll         SCST
dat      Drive-alone toll             SCST
s2nh     Shared-2 non-toll HOV        H2CST
s2th     Shared-2 toll non-HOV        H2CST
s3nh     Shared-3 non-toll HOV        H3CST
s3th     Shared-3 toll HOV            H3CST

Note:  dant skims also apply to shared-2 non-toll, non-HOV and shared 3+ non-toll, non-HOV

v1.0  jef 3/30/2012
v2.0  jef 5/10/2015 added value-of-time bins and commercial vehicle modes

*/
Macro "hwy skim" (args)

   shared path, inputDir, outputDir, mxzone
  
   mode=args[1]
   
   //vot_bin is the value-of-time bin that will be appended to each skim name; prepend "_"
   if args[2]=null then vot_bin="" else vot_bin="_"+args[2]
   
   dim skimbyset1[3],skimbyset2[3]


   // input files
   db_file = outputDir + "\\hwy.dbd"
   net_file = outputDir + "\\hwy.net" 

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   db_node_lyr = db_file + "|" + node_lyr
   net = ReadNetwork(net_file)

   periods = {"_EA", "_AM", "_MD", "_PM", "_EV"}

   for i = 1 to periods.length do

		  skimbyset1 = null                                          // second skim varaible (in addition to LENGTH)
		  skimbyset2 = null                                          // third skim variable
		  skimbyset3 = null																					 // fourth skim variable
	 
			// The truck skim is used for heavy trip distribution
      if mode = "truck" then do

         CostFld =  "*SCST"+periods[i]                               // minimizing cost field
         SkimVar1 = "*STM" +periods[i]                               // first skim varaible (in addition to LENGTH)
         
         excl_qry =  "!((ihov=1|ihov=4|ifc>7)&(ITRUCK=1|ITRUCK>4))"  // query for exclusion link set

         set = "TrkToll"+periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (ihov=4 and (ITRUCK=1|ITRUCK>4))",)
         if n > 0 then skimbyset1={vw_set, {"itoll"+periods[i]}}
         
				 // skimbyset2 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset2={vw_set, {"*_TOTREL" + periods[i]}}
         
         skimmat = "imptrk"+periods[i]+vot_bin+".mtx"                        // output skim matrices

      end
      
      // The skims by weight class are used for truck toll diversion
      else if mode = "lhdn" then do                                  // light duty truck non-toll 
                                                                                    
         CostFld =  "*LHCST"+periods[i]                               // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                               // first skim varaible (in addition to LENGTH)
         
         excl_qry =  "!((ihov=1|ifc>7)&(ITRUCK<4|ITRUCK=7))"             // query for lhd non-toll exclusion link set
 
         // skimbyset1 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {"*_TOTREL" + periods[i]}}				 				 
 
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                      // output skim matrices        
      end    
      else if mode = "mhdn" then do                                  // medium duty truck non-toll 
                                                                                    
         CostFld =  "*MHCST"+periods[i]                               // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                               // first skim varaible (in addition to LENGTH)
        
         excl_qry =  "!((ihov=1|ifc>7)&(ITRUCK<3|ITRUCK>5))"             // query for mhd non-toll exclusion link set

         // skimbyset1 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {"*_TOTREL" + periods[i]}}				 				 
         
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                      // output skim matrices        
      end   
      else if mode = "hhdn" then do                                  // heavy duty truck non-toll 
                                                                                    
         CostFld =  "*HHCST"+periods[i]                               // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                               // first skim varaible (in addition to LENGTH)
         
         excl_qry =  "!((ihov=1|ifc>7)&(ITRUCK=1|ITRUCK>4))"             // query for hhd non-toll exclusion link set

         // skimbyset1 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {"*_TOTREL" + periods[i]}}				 				 
         
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                      // output skim matrices        
      end 
      
      else if mode = "lhdt" then do   
         CostFld =  "*SCST"+periods[i]                                                  // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                                                  // first skim varaible (in addition to LENGTH)
                                                                                                           
         excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7) & (ITRUCK<4|ITRUCK=7))" // query for lhd toll exclusion link set

         tollfield = "ITOLL2"                                                                                   // toll value
          
         set =  mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where " + excl_qry,)
         if n = 0 then excl_qry=null                                                    // reset value if no selection records 
         
         // skimbyset1 = toll
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {tollfield + periods[i] }}

         // skimbyset2 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset2={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                    // output skim matrices        
      end
      else if mode = "mhdt" then do   
         CostFld =  "*SCST"+periods[i]                                                  // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                                                  // first skim varaible (in addition to LENGTH)
                                                                                                          
         excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&(ITRUCK<3|ITRUCK>5))" // query for mhd toll exclusion link set

         tollfield = "ITOLL2"                                                                                   // toll value
          
         set =  mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where " + excl_qry,)
         if n = 0 then excl_qry=null                                                    // reset value if no selection records 
         
         // skimbyset1 = toll
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {tollfield + periods[i] }}

         // skimbyset2 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset2={vw_set, {"*_TOTREL" + periods[i]}}				 				 

				 
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                    // output skim matrices        
      end
      else if mode = "hhdt" then do   
         CostFld =  "*SCST"+periods[i]                                                  // minimizing cost field 
         SkimVar1 = "*STM" +periods[i]                                                  // first skim varaible (in addition to LENGTH)
                                                                                                          
         excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&(ITRUCK=1|ITRUCK>4))" // query for hhd toll exclusion link set

         tollfield = "ITOLL2"                                                                                   // toll value
          
         set =  mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where " + excl_qry,)
         if n = 0 then excl_qry=null                                                    // reset value if no selection records 
         
         // skimbyset1 = toll
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {tollfield + periods[i] }}

         // skimbyset2 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset2={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat = "imp"+mode+periods[i]+vot_bin+".mtx"                    // output skim matrices        
      end
                                                                           
      else if mode = "cvn" then do                                  // commercial vehicles non-toll
         CostFld =  "*CVCST"+periods[i]                               // minimizing cost field
         SkimVar1 = "*STM" +periods[i]                               // first skim varaible (in addition to LENGTH)                                                         
         
         excl_qry =  "!((ihov=1|ifc>7)&(ITRUCK=1|ITRUCK>4))"             // query for hhd non-toll exclusion link set

         // skimbyset1 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {"*_TOTREL" + periods[i]}}				 				 
         
         skimmat = "impcvn"+periods[i]+vot_bin+".mtx"                      // output skim matrices        
      end 

     else if mode = "cvt" then do                                    //  commercial vehicle toll skims; uses same selection set as drive-alone toll
         CostFld =  "*CVCST"+periods[i]                              // minimizing cost field                                                                                  
         SkimVar1 = "*STM"+periods[i]                                // first skim varaible (in addition to LENGTH)

         excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&ITRUCK<5)"
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several","Select * where "+excl_qry,)
         if n = 0 then excl_qry=null   //reset value if no selection records
         
         tollfield = "ITOLL2"                                                                                   // toll value
 
        // skimbyset1 = toll
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {tollfield + periods[i] }}

         // skimbyset2 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset2={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat = "impcvt"+periods[i]+vot_bin+".mtx"
      end 
      else if mode = "dant" then do

         CostFld =  "*SCST"+periods[i]                             // minimizing cost field
         SkimVar1 = "*STM"+periods[i]                              // first skim varaible (in addition to LENGTH)
   
         excl_qry = "!(ihov=1&ITRUCK<5)"                           // query for exclusion link set

         // skimbyset1 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset1={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat =  "impdan"+periods[i]+vot_bin+".mtx"                     // output skim matrices
      end
      else if mode = "dat" then do
         
         CostFld = "*SCST"+periods[i]                                // minimizing cost field
         SkimVar1 = "*STM"+periods[i]                                // first skim varaible (in addition to LENGTH)

         //excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&ITRUCK<5)"
		 excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(abln"+periods[i]+"<9)))|ifc>7)&ITRUCK<5)"
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several","Select * where "+excl_qry,)
         if n = 0 then excl_qry=null   //reset value if no selection records
         
         // skimbyset1 = length on toll lanes
         set = "datdst"+periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         //n = SelectByQuery(set, "Several", "Select * where ((ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))&ITRUCK<5)",)
		 n = SelectByQuery(set, "Several", "Select * where ((ihov=4|((ihov=2|ihov=3)&(abln"+periods[i]+"<9)))&ITRUCK<5)",)
         if n > 0 then skimbyset1={vw_set, {"Length"}}
   
         // skimbyset2 = cost
         set = mode + periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)   // for all links
         if n > 0 then skimbyset2={vw_set, {"itoll"+periods[i]}}

				 // skimbyset3 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset3={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat = "impdat"+periods[i]+vot_bin+".mtx"
      end
      else if mode = "s2nh" then do
      
         CostFld =  "*H2CST"+periods[i]            // minimizing cost field
         SkimVar1 = "*HTM"+periods[i]
         
         excl_qry ="!((ihov=1|(ihov=2&abln"+periods[i]+" <9)|ifc>7)&ITRUCK<5)"//initialize the value
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several","Select * where "+excl_qry,)
         if n = 0 then excl_qry=null   //reset value if no selection records
        
         set = "s2hdst" + periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (abln"+periods[i]+"<9 and ihov=2 and ITRUCK<5)",)
         if n > 0 then skimbyset2={vw_set, {"Length"}}

         // skimbyset3 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset3={vw_set, {"*_TOTREL" + periods[i]}}				 				 				 
         
         skimmat = "imps2nh"+periods[i]+vot_bin+".mtx"
      end
      else if mode = "s2th" then do
         
         CostFld = "*H2CST"+periods[i]            // minimizing cost field
         SkimVar1 ="*HTM"+periods[i]

         excl_qry = "!(((ihov=1|(ihov=2&abln"+periods[i]+"<9)|ihov=4|(ihov=3&itoll"+periods[i]+">0&abln"+periods[i]+"<9))|ifc>7)&ITRUCK<5)"
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several","Select * where " + excl_qry,)
         if n = 0 then excl_qry=null   //reset value if no selection records
         
         set = "s2tdst"+periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (((abln"+periods[i]+"<9 & ihov=2) | (ihov=4 | (ihov=3 & itoll"+periods[i]+" >0 & abln"+periods[i]+"< 9)))& ITRUCK<5)",)
         if n > 0 then skimbyset1={vw_set, {"Length"}}         
         
         set = "s2t"+periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where ((ihov=4|(ihov=3 & itoll"+periods[i]+" >0 & abln"+periods[i]+" < 9)) & ITRUCK<5)",)
         if n > 0 then skimbyset2={vw_set, {"itoll"+periods[i]}}

         // skimbyset3 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset3={vw_set, {"*_TOTREL" + periods[i]}}				 				 
         
         skimmat = "imps2th"+periods[i]+vot_bin+".mtx"
      
      end
      else if mode = "s3nh" then do
      
         CostFld =  "*H3CST"+periods[i]            // minimizing cost field
         SkimVar1 = "*HTM" +periods[i]

         excl_qry = "!((ihov=1|((ihov=2|ihov=3)&abln"+periods[i]+"<9)|ifc>7)& ITRUCK<5)"
         set =  mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where " + excl_qry,)
         if n = 0 then excl_qry=null
         
         set = "s3hdst"+periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (abln"+periods[i]+"<9 & (ihov=2 | ihov=3) & ITRUCK<5)",)
         if n > 0 then skimbyset2={vw_set, {"Length"}}

         // skimbyset3 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset3={vw_set, {"*_TOTREL" + periods[i]}}				 				 
         
         skimmat = "imps3nh"+periods[i]+vot_bin+".mtx"
         
      end
      else if mode = "s3th" then do
 
         CostFld = "*H3CST" + periods[i]                         // minimizing cost field
         SkimVar1 = "*HTM" + periods[i]
      
         excl_qry = "(abln"+periods[i]+"=9 | ITRUCK >4)"     
         set =  mode + periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where " + excl_qry,)
         if n = 0 then excl_qry=null
      
         set = "s3tdst" + periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (((abln"+periods[i]+"<9 and (ihov=2 or ihov=3)) or ihov=4)and ITRUCK <5)",)
         if n > 0 then skimbyset1={vw_set, {"Length"}}
         
         set = "s3t" + periods[i]
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where (ihov=4 and ITRUCK <5)",)
         if n > 0 then skimbyset2={vw_set, {"itoll"+periods[i]}}         

         // skimbyset3 = reliability
         set = mode
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where 1=1",)                       // for all links     
         if n > 0 then skimbyset3={vw_set, {"*_TOTREL" + periods[i]}}				 				 
				 
         skimmat = "imps3th"+periods[i]+vot_bin+".mtx"
      end
      
      
      //delete existing skim file
      ok=RunMacro("SDdeletefile",{outputDir+"\\"+skimmat}) 
      if !ok then goto quit
   
      //skim network
      set = "AllLinks"
      vw_set = link_lyr + "|" + set
      SetLayer(link_lyr)
      n = SelectAll(set) 
      NetOpts = null
      NetOpts.[Link ID] = link_lyr+".ID"
      NetOpts.[Type] = "Enable"
      ChangeLinkStatus(net,vw_set, NetOpts)               // first enable all links
   
      if excl_qry<>null then do
         set = "toll"
         vw_set = link_lyr + "|" + set
         SetLayer(link_lyr)
         n = SelectByQuery(set, "Several", "Select * where "+excl_qry,) 
        NetOpts = null
        NetOpts.[Link ID] = link_lyr+".ID"
        NetOpts.[Type] = "Disable"
        ChangeLinkStatus(net,vw_set, NetOpts)               // disable exclusion query
      end

      Opts = null
      Opts.Input.Network = net_file
      Opts.Input.[Origin Set]   = {db_node_lyr, node_lyr, "Centroids", "select * where ID <= " + i2s(mxzone)}
      Opts.Input.[Destination Set] = {db_node_lyr, node_lyr, "Centroids"}
      Opts.Input.[Via Set]     = {db_node_lyr, node_lyr}
      Opts.Field.Minimize     = CostFld
      Opts.Field.Nodes = node_lyr + ".ID"
      Opts.Field.[Skim Fields]={{"Length","All"},{SkimVar1,"All"}}
			
			// provide skimset
      if skimbyset1 <> null then do
         if skimbyset2 <> null then do
						if skimbyset3 <> null then 
							Opts.Field.[Skim by Set]={skimbyset1,skimbyset2,skimbyset3}
						else
							Opts.Field.[Skim by Set]={skimbyset1,skimbyset2}
				 end
				 else if skimbyset3 <> null then
            Opts.Field.[Skim by Set]={skimbyset1,skimbyset3}
				 else
						Opts.Field.[Skim by Set]={skimbyset1}
      end
      else if skimbyset2 <> null then do
				if skimbyset3 <> null then
					Opts.Field.[Skim by Set]={skimbyset2,skimbyset3}
				else 
					Opts.Field.[Skim by Set]={skimbyset2}
			end
			else if skimbyset3 <> null then
        Opts.Field.[Skim by Set]={skimbyset3}
				
        //end of previous if string
      if (mode = "lhdn" | mode = "mhdn" | mode = "hhdn" | mode = "lhdt" | mode = "mhdt" | mode = "hhdt") then 
        if (mode = "lhdn" | mode = "mhdn" | mode = "hhdn") then
         Opts.Output.[Output Matrix].Label = "impedance truck"                                 
        else if (mode = "lhdt" | mode = "mhdt" | mode = "hhdt") then
          Opts.Output.[Output Matrix].Label = "impedance truck toll"  
        else 
         Opts.Output.[Output Matrix].Label = "congested " + mode + " impedance"  
      Opts.Output.[Output Matrix].Compression = 0 //uncompressed, for version 4.8 plus    
      Opts.Output.[Output Matrix].[File Name] = outputDir + "\\"+skimmat
      
      RunMacro("HwycadLog",{"hwyskim.rsc: hwy skim","TCSPMAT: "+skimmat+"; "+CostFld})
      ok = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts)
      if !ok then goto quit

      // STEP 2: Intrazonal added by Ziying Ouyang, June 3, 2009
      // mtxcore={"Length (Skim)"}+{SkimVar1[i]+" (Skim)"}+{SkimVar2[i]+" (Skim)"}+{SkimVar3[i]+" (Skim)"}
      mtxcore={"Length (Skim)"}+{SkimVar1+" (Skim)"}
      for j = 1 to mtxcore.length do
         Opts = null
         Opts.Global.Factor = 0.5
         Opts.Global.Neighbors = 3
         Opts.Global.Operation = 1
         Opts.Global.[Treat Missing] = 2
         Opts.Input.[Matrix Currency] = {outputDir + "\\"+skimmat,mtxcore[j], , }
         RunMacro("HwycadLog",{"hwyskim.rsc: hwy skim","Intrazonal: "+skimmat+"; "+mtxcore[j]})
         ok = RunMacro("TCB Run Procedure", j, "Intrazonal", Opts)
         if !ok then goto quit
      end

			// take square root of the reliability which is sum of square of link reliability - write code after generating skims - todo
			mtxcore = mode+" - *_TOTREL"+periods[i]
			m=OpenMatrix(outputDir + "\\"+skimmat,)
			mc=CreateMatrixCurrency(m,mtxcore,,,)
			mc:=Nz(mc) // zero out intra-zonal values
			mc:=sqrt(mc) // take square root
			
   end
 
   ok=1
   runmacro("close all")
   quit:   

   return(ok)   
  
EndMacro

/*

hwy skim 15 mins time slices

Skim reliability with following shift variables:

Variable        Time Bin      Estimate-Freeway      Estimate-Arterial
--------        --------      ----------------      -----------------
BeforeAM.Step1    32            	-0.0183							   -0.0054
BeforeAM.Step2    29            	 0.0092							   -0.0032
BeforeAM.Step3    26            	 0.0107							    0.0030
BeforeAM.Step4    20            	-0.0019							    0.0055
AfterAM.Step1     32            	-0.0082							   -0.0009
AfterAM.Step2     36            	 0.0000							    0.0000
AfterAM.Step3     39            	 0.0000							    0.0000
BeforePM.Step1    70            	-0.0067							    0.0011
BeforePM.Step2    66            	-0.0028							    0.0000
BeforePM.Step3    62            	 0.0094							   -0.0018
BeforePM.Step4    58            	 0.0000							    0.0000
AfterPM.Step1     70            	-0.0077							   -0.0079
AfterPM.Step2     71            	 0.0000							    0.0025
AfterPM.Step3     79            	 0.0075							    0.0037

{"EA","AM","MD","PM","EV1","EV2"} = {{15,24},{25,36},{37,62},{63,76},{77,96},{0,14}} 

*/

macro "hwy skim time bins" (args)
		
		shared path, inputDir, outputDir, mxzone
		
		mode = args[1]
		
		//vot_bin is the value-of-time bin that will be appended to each skim name; prepend "_"
		if args[2]=null then vot_bin="" else vot_bin="_"+args[2]
		timebin=args[3]
		 
		// period thresholds
		Peak_AM = 32
		Low_MD = 41
		Peak_PM = 70
		
		// shift variable settings
		// {beforeAM_step1, beforeAM_step2,beforeAM_step3,beforeAM_step4,afterAM_step1,afterAM_step2,afterAM_step3,beforePM_step1,beforePM_step2,beforePM_step3,beforePM_step4,afterPM_step1,afterPM_step2,afterPM_step3}
		time_bins = {Peak_AM,29,26,20,Peak_AM,36,39,Peak_PM,66,62,58,Peak_PM,71,79}
		factor_freeway = {-0.0183,0.0092,0.0107,-0.0019,-0.0082,0.0000,0.0000,-0.0067,-0.0028,0.0094,0.0000,-0.0077,0.0000,0.0075}
		factor_arterial = {-0.0054,-0.0032,0.0030,0.0055,-0.0009,0.0000,0.0000,0.0011,0.0000,-0.0018,0.0000,-0.0079,0.0025,0.0037}
		
		facility_type = {"freeway","arterial","ramp","other"} // freeway (IFC=1), arterial (IFC=2,3), ramp (IFC=8,9), other (IFC=4,5,6,7)
		periods = {"_EA", "_AM", "_MD", "_PM", "_EV"}

		// lower and upper bounds of IFC for respective facility type = {freeway, arterial, ramp, other}											 
		lwr_bound = {"1","2","8","4"}
		upr_bound = {"1","3","9","7"}
	
   // input files
   db_file = outputDir + "\\hwy.dbd"
   net_file = outputDir + "\\hwy.net" 

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   db_node_lyr = db_file + "|" + node_lyr
   net = ReadNetwork(net_file)		
		
   //Recompute total reliability for 15 mins time slices
	 for fac_type=1 to facility_type.length do
	 
			if fac_type=1 then factor=factor_freeway
			else factor=factor_arterial
			
			// corresponding model time period
			if timebin>=15 and timebin<=24 then period = periods[1]  // EA
			if timebin>=25 and timebin<=36 then period = periods[2]  // AM
			if timebin>=37 and timebin<=62 then period = periods[3]  // MD
			if timebin>=63 and timebin<=76 then period = periods[4]  // PM
			if (timebin>=1 and timebin<=14) or (timebin>=77 and timebin<=96) then period = periods[5]  // EV
			
			// initialize shift variables
			beforeAM_step1=0
			beforeAM_step2=0
			beforeAM_step3=0
			beforeAM_step4=0
			afterAM_step1=0
			afterAM_step2=0
			afterAM_step3=0
			beforePM_step1=0
			beforePM_step2=0
			beforePM_step3=0
			beforePM_step4=0
			afterPM_step1=0
			afterPM_step2=0
			afterPM_step3=0
			
			// calculate shift reliability
			
			// before AM
			if timebin<time_bins[1] then beforeAM_step1=factor[1]*(time_bins[1]-timebin)
			if timebin<time_bins[2] then beforeAM_step2=factor[2]*(time_bins[2]-timebin)
			if timebin<time_bins[3] then beforeAM_step3=factor[3]*(time_bins[3]-timebin)
			if timebin<time_bins[4] then beforeAM_step4=factor[4]*(time_bins[4]-timebin)
			// after AM
			if (timebin>time_bins[5] and timebin<Low_MD) then afterAM_step1=factor[5]*(timebin-time_bins[5])
			if (timebin>time_bins[6] and timebin<Low_MD) then afterAM_step2=factor[6]*(timebin-time_bins[6])
			if (timebin>time_bins[7] and timebin<Low_MD) then afterAM_step3=factor[7]*(timebin-time_bins[7])
			// before PM
			if (timebin>=Low_MD and timebin<time_bins[8]) then beforePM_step1=factor[8]*(time_bins[8]-timebin)
			if (timebin>=Low_MD and timebin<time_bins[9]) then beforePM_step2=factor[9]*(time_bins[9]-timebin)
			if (timebin>=Low_MD and timebin<time_bins[10]) then beforePM_step3=factor[10]*(time_bins[10]-timebin)
			if (timebin>=Low_MD and timebin<time_bins[11]) then beforePM_step4=factor[11]*(time_bins[11]-timebin)
			// after PM
			if timebin>time_bins[12] then afterPM_step1=factor[12]*(timebin-time_bins[12])
			if timebin>time_bins[13] then afterPM_step2=factor[13]*(timebin-time_bins[13])
			if timebin>time_bins[14] then afterPM_step3=factor[14]*(timebin-time_bins[14])
			
			// calculate total shift reliability factors
			shift_factor = beforeAM_step1 + beforeAM_step2 + beforeAM_step3 + beforeAM_step4 + 
										 afterAM_step1 + afterAM_step2 + afterAM_step3 + 
										 beforePM_step1 + beforePM_step2 + beforePM_step3 + beforePM_step4 + 
										 afterPM_step1 + afterPM_step2 + afterPM_step3
			
			// expressions to calculate AB/BA shift reliability - squareof link reliability																		
			expression_AB = "AB_TOTREL" + period + "+ pow(" + String(shift_factor) + "*Length*AB_Time,2)"
			expression_BA = "BA_TOTREL" + period + "+ pow(" + String(shift_factor) + "*Length*BA_Time,2)"
			
			flowTable = outputDir+"\\hwyload"+period+".bin"

			query = "Select * where IFC >= " + lwr_bound[fac_type] + " and IFC <= "+upr_bound[fac_type]				
			
			// Total Reliability - The Dataview Set is a joined view of the link layer and the flow table, based on link ID
			Opts.Input.[Dataview Set] = {{db_file+"|"+link_lyr, flowTable, {"ID"}, {"ID1"}},"reliabilityjoin"+period, "selection", query}   
			Opts.Global.Fields = {"AB_TOTREL"+period,"BA_TOTREL"+period}                                // the field to fill
			Opts.Global.Method = "Formula"                                                                  // the fill method
			Opts.Global.Parameter = {expression_AB,
															 expression_BA }   
			ret_value = RunMacro("TCB Run Operation", "Fill Dataview", Opts, &Ret)
			if !ret_value then goto quit
	end
							 
	// update total reliability fields 
	Opts = null
	Opts.Input.Database = db_file
	Opts.Input.Network = net_file
	Opts.Input.[Link Set] = {db_file+"|"+link_lyr, link_lyr}
	Opts.Global.[Fields Indices] = "*_TOTREL"+period
	Opts.Global.Options.[Link Fields] = { {link_lyr+".AB_TOTREL"+period,link_lyr+".BA_TOTREL"+period} }
	Opts.Global.Options.Constants = {1}
	ret_value = RunMacro("TCB Run Operation",  "Update Network Field", Opts) 
	if !ret_value then goto quit     
	
	// settings for skim
	if mode = "dant" then do
		 CostFld =  "*SCST"+period                             // minimizing cost field
		 excl_qry = "!(ihov=1&ITRUCK<5)"                           // query for exclusion link set
		 skimmat =  "impdan_"+String(timebin)+vot_bin+".mtx"                     // output skim matrices
	end
	else if mode = "dat" then do
		 CostFld = "*SCST"+period                                // minimizing cost field
		 excl_qry = "!(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+period+">0&abln"+period+"<9)))|ifc>7)&ITRUCK<5)"				 
		 skimmat = "impdat_"+String(timebin)+vot_bin+".mtx"
	end
	else if mode = "s2nh" then do
		 CostFld =  "*H2CST"+period            						  // minimizing cost field
		 excl_qry ="!((ihov=1|(ihov=2&abln"+period+" <9)|ifc>7)&ITRUCK<5)"//initialize the value
		 skimmat = "imps2nh"+String(timebin)+vot_bin+".mtx"
	end
	else if mode = "s2th" then do
		 CostFld = "*H2CST"+period            							// minimizing cost field
		 excl_qry = "!(((ihov=1|(ihov=2&abln"+period+"<9)|ihov=4|(ihov=3&itoll"+period+">0&abln"+period+"<9))|ifc>7)&ITRUCK<5)"
		 skimmat = "imps2th"+String(timebin)+vot_bin+".mtx"
	end
	else if mode = "s3nh" then do
		 CostFld =  "*H3CST"+period            						  // minimizing cost field
		 excl_qry = "!((ihov=1|((ihov=2|ihov=3)&abln"+period+"<9)|ifc>7)& ITRUCK<5)"
		 skimmat = "imps3nh"+String(timebin)+vot_bin+".mtx"
	end
	else if mode = "s3th" then do
		 CostFld = "*H3CST"+period                          // minimizing cost field
		 excl_qry = "(abln"+period+"=9 | ITRUCK >4)"     
		 skimmat = "imps3th"+String(timebin)+vot_bin+".mtx"
	end
 
	//skim network
	set = "AllLinks"
	vw_set = link_lyr + "|" + set
	SetLayer(link_lyr)
	n = SelectAll(set) 
	NetOpts = null
	NetOpts.[Link ID] = link_lyr+".ID"
	NetOpts.[Type] = "Enable"
	ChangeLinkStatus(net,vw_set, NetOpts)               // first enable all links

	if excl_qry<>null then do
		 set = "toll"
		 vw_set = link_lyr + "|" + set
		 SetLayer(link_lyr)
		 n = SelectByQuery(set, "Several", "Select * where "+excl_qry,) 
		NetOpts = null
		NetOpts.[Link ID] = link_lyr+".ID"
		NetOpts.[Type] = "Disable"
		ChangeLinkStatus(net,vw_set, NetOpts)               // disable exclusion query
	end
				 
	Opts = null
	Opts.Input.Network = net_file
	Opts.Input.[Origin Set]   = {db_node_lyr, node_lyr, "Centroids", "select * where ID <= " + i2s(mxzone)}
	Opts.Input.[Destination Set] = {db_node_lyr, node_lyr, "Centroids"}
	Opts.Input.[Via Set]     = {db_node_lyr, node_lyr}
	Opts.Field.Minimize     = CostFld
	Opts.Field.Nodes = node_lyr + ".ID"
	//Opts.Field.[Skim Fields].["*_TOTREL"+period]="All"
	Opts.Field.[Skim Fields]={{"*_TOTREL"+period,"All"}}
	Opts.Output.[Output Matrix].Label = "reliability " + mode
	
	Opts.Output.[Output Matrix].Compression = 0 //uncompressed, for version 4.8 plus    
	Opts.Output.[Output Matrix].[File Name] = outputDir + "\\"+skimmat
	
	RunMacro("HwycadLog",{"hwyskim.rsc: hwy skim time bins","TCSPMAT: "+skimmat+"; "+CostFld})
	ok = RunMacro("TCB Run Procedure", 1, "TCSPMAT", Opts, &Ret)
	if !ok then goto quit
			
	// update skims by taking square root of reliability
	mtxcore = "*_TOTREL"+period + " (Skim)"
	m=OpenMatrix(outputDir + "\\"+skimmat,)
	mc=CreateMatrixCurrency(m,mtxcore,,,)
	mc:=Nz(mc) // zero out intra-zonal values
	mc:=sqrt(mc) // take square root			
			 
   ok=1
   runmacro("close all")
   quit:   

   return(ok)   

EndMacro

