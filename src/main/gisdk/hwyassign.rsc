/*********************************************************************************
Multi-model Multi-class Assignement
macro "hwy assignment" 

input files: hwy.dbd
   hwy.net
   Trip_EA.mtx: Early AM auto trip matrix file
   Trip_AM.mtx: AM Peak  auto trip matrix file 
   Trip_MD.mtx: Midday   auto trip matrix file 
   Trip_PM.mtx: PM Peak  auto trip matrix file 
   Trip_EV.mtx: Evening  auto trip matrix file 

each file has 14 cores:

 Name     Description
 -------  ---------------------------------------
 SOV_GP   Drive Alone Non-Toll
 SOV_PAY  Drive Alone Toll
 SR2_GP   Shared-ride 2 Person Non-HOV Non-Toll
 SR2_HOV  Shared-ride 2 Person HOV Non-Toll
 SR2_PAY  Shared-ride 2 Person HOV Toll Eligible
 SR3_GP   Shared-ride 3+ Person Non-HOV Non-Toll
 SR2_HOV  Shared-ride 3+ Person HOV Non-Toll
 SR2_PAY  Shared-ride 3+ Person HOV Toll Eligible
 lhdn     Light heavy-duty Truck Non-Toll
 mhdn     Medium heavy-duty Truck Non-Toll
 hhdnv    Heavy heavy-duty Truck Non-Toll
 lhdt     Light heavy-duty Truck Toll
 mhdt     Medium heavy-duty Truck Toll
 hhdt     Heavy heavy-duty Truck Toll

 Functions are added by J Xu between Dec 2006 and March 2007
 (1) Select Link Analysis and split the resulting flow table by each select link inquiries;
 (2) Enhanced highway assignment for four different cases, involving toll.
 (3) Turning movement

output files:

   hwyload_EA.bin: Early AM loaded network binary file  
   hwyload_AM.bin: Am Peak  loaded network binary file
   hwyload_MD.bin: Midday   loaded network binary file
   hwyload_PM.bin: PM Peak  loaded network binary file
   hwyload_EV.bin: Evening  loaded network binary file
   
Optionally (for select link and turning movements):
  
   turns_EA.bin: Early AM turning movement file  
   turns_AM.bin: Am Peak  turning movement file
   turns_MD.bin: Midday   turning movement file
   turns_PM.bin: PM Peak  turning movement file
   turns_EV.bin: Evening  turning movement file
   
   select_EA.mtx: Early AM select link trip matrix file  
   select_AM.mtx: Am Peak  select link trip matrix file
   select_MD.mtx: Midday   select link trip matrix file
   select_PM.mtx: PM Peak  select link trip matrix file
   select_EV.mtx: Evening  select link trip matrix file
   

SANDAG ABM Version 1.0  
 JEF 2012-03-20 
 changed linktypeturnscst.dbf to linktypeturns.dbf as Joel suggested.
*************************************************************************************/

Macro "hwy assignment" (args)

   Shared path, inputDir, outputDir, mxzone

 
   turn_file="\\nodes.txt"
   turn_flag=0
   NumofCPU = 8
   iteration = args[1]
   
   periods = {"_EA","_AM","_MD","_PM","_EV"}
   
   RunMacro("close all") 
   dim excl_qry[periods.length],excl_toll[periods.length],excl_dat[periods.length],excl_s2nh[periods.length],excl_s2th[periods.length],excl_s3nh[periods.length]
   dim excl_s3th[periods.length],excl_lhdn[periods.length],excl_mhdn[periods.length],excl_hhdn[periods.length],excl_lhdt[periods.length],excl_mhdt[periods.length]
   dim excl_hhdt[periods.length],toll_fld[periods.length],toll_fld2[periods.length]
   
   linkt=  {"*TM_EA","*TM_AM","*TM_MD","*TM_PM","*TM_EV"}
   linkcap={"*CP_EA","*CP_AM","*CP_MD","*CP_PM","*CP_EV"}
//   xt=     {"*TX_EA","*TX_AM","*TX_MD","*TX_PM","*TX_EV"}
   xcap=   {"*CX_EA","*CX_AM","*CX_MD","*CX_PM","*CX_EV"}

   cycle={"*CYCLE_EA","*CYCLE_AM","*CYCLE_MD","*CYCLE_PM","*CYCLE_EV"}
   pfact={"*PF_EA","*PF_AM","*PF_MD","*PF_PM","*PF_EV"}
   gcrat={"*GCRATIO_EA","*GCRATIO_AM","*GCRATIO_MD","*GCRATIO_PM","*GCRATIO_EV"}
   alpha1={"*ALPHA1_EA","*ALPHA1_AM","*ALPHA1_MD","*ALPHA1_PM","*ALPHA1_EV"}
   beta1={"*BETA1_EA","*BETA1_AM","*BETA1_MD","*BETA1_PM","*BETA1_EV"}
   alpha2={"*ALPHA2_EA","*ALPHA2_AM","*ALPHA2_MD","*ALPHA2_PM","*ALPHA2_EV"}
   beta2={"*BETA2_EA","*BETA2_AM","*BETA2_MD","*BETA2_PM","*BETA2_EV"}
   preload={"*PRELOAD_EA","*PRELOAD_AM","*PRELOAD_MD","*PRELOAD_PM","*PRELOAD_EV"}

   db_file = outputDir + "\\hwy.dbd" 
   net_file= outputDir+"\\hwy.net"

   trip={"Trip_EA.mtx","Trip_AM.mtx","Trip_MD.mtx","Trip_PM.mtx","Trip_EV.mtx"}
   turn={"turns_EA.bin","turns_AM.bin","turns_MD.bin","turns_PM.bin","turns_EV.bin"}
   selectlink_mtx={"select_EA.mtx","select_AM.mtx","select_MD.mtx","select_PM.mtx","select_EV.mtx"}  //added for select link analysis by JXu
   selinkqry_file="selectlink_query.txt"
   if GetFileInfo(inputDir + "\\"+ selinkqry_file) <> null then do	//select link analysis is only available in stage II
        selink_flag =1
        fptr_from = OpenFile(inputDir + "\\"+selinkqry_file, "r")
    	tmp_qry=readarray(fptr_from)
    	index =1
  	selinkqry_name=null
    	selink_qry=null
    	subs=null
    	while index <=ArrayLength(tmp_qry) do
    	    if left(trim(tmp_qry[index]),1)!="*" then do
    	        subs=ParseString(trim(tmp_qry[index]),",")
    	    	if subs!=null then do
    	            query=subs[3]
    	            if ArrayLength(subs)>3 then do
    	                for i=4 to ArrayLength(subs) do
    	                    query=query+" "+subs[2]+" "+subs[i]
    	                end
    	            end    	       	    
    	            selinkqry_name=selinkqry_name+{subs[1]}
    	            selink_qry=selink_qry+{query}
    	    	end
    	    end
    	    index = index + 1
        end
    end

  asign = {"hwyload_EA.bin","hwyload_AM.bin","hwyload_MD.bin","hwyload_PM.bin","hwyload_EV.bin"}
  oue_path = {"oue_path_EA.obt", "oue_path_AM.obt","oue_path_MD.obt","oue_path_PM.obt","oue_path_EV.obt"}
  {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
   db_link_lyr=db_file+"|"+link_lyr
   db_node_lyr=db_file+"|"+node_lyr
   
   // drive-alone non-toll exclusion set
   excl_dan={db_link_lyr, link_lyr, "dan", "Select * where !(ihov = 1)"}
   
   // shared-2 non-toll non-HOV exclusion set 
   excl_s2nn=excl_dan

   // shared 3+ non-toll non-HOV exclusion set
   excl_s3nn=excl_dan

   
   for i = 1 to periods.length do
        // drive-alone toll exclusion set
      excl_dat[i]={db_link_lyr, link_lyr, "dat", "Select * where !(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&ITRUCK<5)"} 
      
      // shared-2 non-toll HOV exclusion set
      excl_s2nh[i]={db_link_lyr, link_lyr, "s2nh", "Select * where !((ihov=1|(ihov=2&abln"+periods[i]+" <9)|ifc>7)&ITRUCK<5)"}
   
      // shared-2 toll HOV exclusion set
      excl_s2th[i]={db_link_lyr, link_lyr, "s2th", "Select * where !(((ihov=1|(ihov=2&abln"+periods[i]+"<9)|ihov=4|(ihov=3&itoll"+periods[i]+">0&abln"+periods[i]+"<9))|ifc>7)&ITRUCK<5)"}
   
      // shared=3+ non-toll non-HOV exclusion set
      excl_s3nh[i]={db_link_lyr, link_lyr, "s3nh", "Select * where !((ihov=1|((ihov=2|ihov=3)&abln"+periods[i]+"<9)|ifc>7)& ITRUCK<5)"}
   
      // shared=3+ toll HOV exclusion set
      excl_s3th[i]={db_link_lyr, link_lyr, "s3th", "Select * where abln"+periods[i]+"=9|ITRUCK>4"}
   
      // light-heavy truck non-toll exclusion set
      excl_lhdn[i]={db_link_lyr, link_lyr, "lhdn", "Select * where !((ihov=1|ifc>7)&(ITRUCK<4|ITRUCK=7))"}
  
      // medium-heavy truck non-toll exclusion set
      excl_mhdn[i]={db_link_lyr, link_lyr, "mhdn", "Select * where !((ihov=1|ifc>7)&(ITRUCK<3|ITRUCK>5))"}
      
      // heavy-heavy truck non-toll exclusion set
      excl_hhdn[i]={db_link_lyr, link_lyr, "hhdn", "Select * where !((ihov=1|ifc>7)&(ITRUCK=1|ITRUCK>4))"}
   
      // light-heavy truck toll exclusion set
      excl_lhdt[i]={db_link_lyr, link_lyr, "lhd", "Select * where !(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7) & (ITRUCK<4|ITRUCK=7))"}
 
      // medium-heavy truck toll exclusion set
      excl_mhdt[i]={db_link_lyr, link_lyr, "mhd", "Select * where !(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&(ITRUCK<3|ITRUCK>5))"}
   
      // heavy-heavy truck toll exclusion set
      excl_hhdt[i]={db_link_lyr, link_lyr, "hhd", "Select * where !(((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)&(ITRUCK=1|ITRUCK>4))"}
   
   end

   //reset exclusion array value based on the selection set results 

   set = "dat"
   vw_set = link_lyr + "|" + set
   SetLayer(link_lyr)
   for i = 1 to periods.length do
      n = SelectByQuery(set, "Several","Select * where !((ihov=1|ihov=4|((ihov=2|ihov=3)&(itoll"+periods[i]+">0&abln"+periods[i]+"<9)))|ifc>7)",)
      if n = 0 then excl_dat[i]=null
   end
          
   set = "s2nh"
   vw_set = link_lyr + "|" + set
   SetLayer(link_lyr)
   for i = 1 to periods.length do
      n = SelectByQuery(set, "Several","Select * where !(ihov=1|(ihov=2&abln"+periods[i]+"<9)|ifc>7)",)
      if n = 0 then excl_s2nh[i]=null 
   end
          
   set = "s2th"
   vw_set = link_lyr + "|" + set
   SetLayer(link_lyr)
   for i = 1 to periods.length do
      n = SelectByQuery(set, "Several", "Select * where !((ihov=1|(ihov=2&abln"+periods[i]+"<9)|ihov=4|(ihov=3&itoll"+periods[i]+">0&abln"+periods[i]+"<9))|ifc>7)",)
      if n = 0 then excl_s2th[i]=null   
   end  
      
   set = "s3nh"
   vw_set = link_lyr + "|" + set
   SetLayer(link_lyr)
   for i = 1 to periods.length do
      n = SelectByQuery(set, "Several","Select * where !(ihov=1|((ihov=2|ihov=3)&abln"+periods[i]+"<9)|ifc>7)",)
      if n = 0 then excl_s3nh[i]=null 
   end         
   
   set = "s3th"
   vw_set = link_lyr + "|" + set
   SetLayer(link_lyr)
   for i = 1 to periods.length do
      n = SelectByQuery(set, "Several", "Select * where abln"+periods[i]+"=9",)
      if n = 0 then excl_s3th[i]=null   
   end                                                                    
   
   for i = 1 to periods.length do
      // to use 14 classes
      excl_qry[i]={excl_dan,excl_dat[i],excl_s2nn,excl_s2nh[i],excl_s2th[i],excl_s3nn,excl_s3nh[i],excl_s3th[i],excl_lhdn[i],excl_mhdn[i],excl_hhdn[i],excl_lhdt[i],excl_mhdt[i],excl_hhdt[i]}
   end
     
   vehclass={1,2,3,4,5,6,7,8,9,10,11,12,13,14}
   
   for i = 1 to periods.length do
      // to use 14 classes  
      toll_fld2[i]= {"COST","ITOLL3"+periods[i],"COST","COST","ITOLL3"+periods[i],"COST","COST","ITOLL3"+periods[i],"COST","COST","COST","ITOLL3"+periods[i],"ITOLL4"+periods[i],"ITOLL5"+periods[i]}
   end
   
   num_class=14
   
   class_PCE={1,1,1,1,1,1,1,1,1.3,1.5,2.5,1.3,1.5,2.5}
   VOT={67,67,67,67,67,67,67,67,67,68,89,67,68,89}

   //Prepare selection set for turning movement report, by JXu
   if (turn_flag=1 & iteration=4) then do
      if GetFileInfo(inputDir+turn_file)!=null then do
         fptr_turn = OpenFile(inputDir + turn_file,"r")
         tmp_qry=readarray(fptr_turn)
         turn_qry=null
         index=1
         while index <=ArrayLength(tmp_qry) do
            if index=1 then 
               turn_qry = "select * where " + "ID=" + tmp_qry[1]
            else 
               turn_qry = turn_qry + " OR " + "ID=" + tmp_qry[index]
               if tmp_qry[index]="all" or tmp_qry[index]="All" or tmp_qry[index]="ALL" then do
                  turn_qry = "select * where ID>"+i2s(mxzone)   //select all nodes except centroids
                  index=ArrayLength(tmp_qry)
               end
               index=index+1
            end
            closefile(fptr_turn)
         end
      else turn_qry = "select * where temp=1"
      if GetFileInfo(path+"\\turn.err")!=null then do
         ok=RunMacro("SDdeletefile",{path+"\\turn.err"}) 
         if !ok then goto quit
      end

      tmpset = "turn"
      vw_set = node_lyr + "|" + tmpset 
      SetLayer(node_lyr)
      n = SelectByQuery(tmpset , "Several", turn_qry,)
      if n = 0 then do
         showmessage("Warning!!! No intersections selected for turning movement.")
         fp_tmp = OpenFile(path + "\\turn.err","w")
         WriteArray(fp_tmp,{"No intersections have been selected for turning movement."})
         closefile(fp_tmp)
         return(1)
      end
   end

  
   //set hwy.net with turn penalty of time in minutes
    d_tp_tb = inputDir + "\\linktypeturns.dbf"
    s_tp_tb = outputDir + "\\turns.dbf"
    Opts = null
    Opts.Input.Database = db_file
    Opts.Input.Network = net_file
    Opts.Input.[Toll Set] = {db_link_lyr, link_lyr}
    Opts.Input.[Centroids Set] = {db_node_lyr, node_lyr, "Selection", "select * where ID <="+i2s(mxzone)}
    Opts.Input.[Def Turn Pen Table] = {d_tp_tb}
    Opts.Input.[Spc Turn Pen Table] = {s_tp_tb}
    Opts.Field.[Link type] = "IFC"
    Opts.Global.[Global Turn Penalties] = {0, 0, 0, 0}
    Opts.Flag.[Use Link Types] = "True"
    RunMacro("HwycadLog",{"hwyassign.rsc: hwy assignment","Highway Network Setting"})  
    ok = RunMacro("TCB Run Operation", 1, "Highway Network Setting", Opts)
    if !ok then goto quit

   // STEP 1: MMA
   for i = 1 to periods.length do   
      net = ReadNetwork(net_file)
      NetOpts = null
      NetOpts.[Link ID] = link_lyr+".ID"
      NetOpts.[Type] = "Enable"
      NetOpts.[Write to file] = "Yes"
      ChangeLinkStatus(net,, NetOpts)  
      
      // Open the trip table to assign, and get the first table name
      ODMatrix = outputDir + "\\"+trip[i]
      m = OpenMatrix(ODMatrix,)
      matrixCores = GetMatrixCoreNames(GetMatrix())
      coreName = matrixCores[1]
        

      //settings for highway assignment
      Opts = null
      Opts.Global.[Force Threads] = 2
      Opts.Input.Database = db_file
      Opts.Input.Network = net_file

      Opts.Input.[OD Matrix Currency] = {ODMatrix, coreName, , }
      Opts.Input.[Exclusion Link Sets] = excl_qry[i]
      Opts.Field.[Vehicle Classes] = vehclass
      Opts.Field.[Fixed Toll Fields] = toll_fld2[i]
      Opts.Field.[VDF Fld Names] = {linkt[i], linkcap[i], xcap[i], cycle[i],pfact[i], gcrat[i], alpha1[i], beta1[i], alpha2[i], beta2[i], preload[i]}
      Opts.Global.[Number of Classes] = num_class
      Opts.Global.[Class PCEs] = class_PCE
      Opts.Global.[Class VOIs] = VOT
      Opts.Global.[Load Method] = "NCFW" 
      Opts.Global.[N Conjugate] = 2
      Opts.Global.[Loading Multiplier] = 1     
      Opts.Global.Convergence = 0.0005
      Opts.Global.[Cost Function File] = "tucson_vdf_rev.vdf"
      Opts.Global.[VDF Defaults] = {, , ,1.5 ,1 , 0.4 , 0.15, 4, 0.15, 4, 0 }
      Opts.Global.[Iterations]=1000
      Opts.Flag.[Do Share Report] = 1     
      Opts.Output.[Flow Table] = outputDir+"\\"+asign[i]  
      if (turn_flag=1 & iteration=4) then Opts.Input.[Turning Movement Node Set] = {db_node_lyr, node_lyr, "Selection", turn_qry}
      if (turn_flag=1 & iteration=4) then Opts.Flag.[Do Turn Movement] = 1
      if (turn_flag=1 & iteration=4) then Opts.Output.[Movement Table] = outputDir+"\\"+turn[i]
      Opts.Field.[MSA Flow] = "_MSAFlow" + periods[i]
      Opts.Field.[MSA Cost] = "_MSACost" + periods[i]
      Opts.Global.[MSA Iteration] = iteration
      if (selink_flag = 1 & iteration = 4) then do
            Opts.Global.[Critical Queries] = selink_qry
            Opts.Global.[Critical Set names] = selinkqry_name
            Opts.Output.[Critical Matrix].Label = "Select Link Matrix"
            Opts.Output.[Critical Matrix].Compression = 1
            Opts.Output.[Critical Matrix].[File Name] = outputDir +"\\"+selectlink_mtx[i]
     	end
      RunMacro("HwycadLog",{"hwyassign.rsc: hwy assignment","MMA: "+asign[i]}) 
      ok = RunMacro("TCB Run Procedure", i, "MMA", Opts)
      if !ok then goto quit
   end  
   if!ok then goto quit

   ok=1
   quit:
      RunMacro("close all")
      return(ok)
EndMacro

//added by JXu to split the flow table by queries.

Macro "Selink Flow Split" (arr_selink)
    shared path, inputDir, outputDir
    asign=arr_selink[1]
    selinkqry_name=arr_selink[2]
    m=ArrayLength(selinkqry_name)+1
    dim new_flowtb[ArrayLength(asign),ArrayLength(selinkqry_name)+1]         //All new flow table names after splitting for OP, AM and PM period assignments (3x5=15 names)
    for i=1 to ArrayLength(asign) do
        for j=1 to ArrayLength(selinkqry_name) do
            new_flowtb[i][j] = outputDir+"\\"+left(asign[i],len(asign[i])-4)+"sl"+ i2s(j) +".bin"
        end
        new_flowtb[i][ArrayLength(selinkqry_name)+1]=outputDir+"\\"+asign[i]
    end
//rename the original flow table file name to avoid the file name conflit with the splitted flow table.
    for i=1 to ArrayLength(asign) do
        new_file=left(asign[i],len(asign[i])-4)+"_orig.bin"
        dict_nm = left(asign[i],len(asign[i])-4)+".dcb"
        newdict_nm = left(asign[i],len(asign[i])-4)+"_orig.dcb"
        ok=RunMacro("SDrenamefile",{outputDir+"\\"+asign[i],outputDir+"\\"+new_file}) if!ok then goto quit
        ok=RunMacro("SDrenamefile",{outputDir+"\\"+dict_nm,outputDir+"\\"+newdict_nm}) if!ok then goto quit
        asign[i]=new_file
    end

// This loop closes all views:
    tmp = GetViews()
    if tmp<>null then
        for i = 1 to ArrayLength(tmp[1]) do
       CloseView(tmp[1][i])
        end

    flowtb_vw = OpenTable("Flow Table", "FFB", {outputDir+"\\"+asign[1],})
    flowtb_fldinfo = GetViewStructure(flowtb_vw)   //Get all the fields info from the flow table
    dim flds_flag[ArrayLength(flowtb_fldinfo),ArrayLength(selinkqry_name)+1]
//flds_flag[i][j]=1 if flowtb_fldinfo[i][1] will be exported to .bin file for query j; 
//if j is more than number of queries, then flds_flag[i][j] decides if flowtb_fldinfo[i][1] will be exported to .bin flow table without query info.
    for i=1 to ArrayLength(flowtb_fldinfo) do
        flag_tot = 0
        for j=1 to ArrayLength(selinkqry_name) do
            if Position(flowtb_fldinfo[i][1],selinkqry_name[j])=0  then do
                flds_flag[i][j]=0
                flag_tot=flag_tot+1
            end
            else flds_flag[i][j]=1
        end
        if flag_tot = ArrayLength(selinkqry_name) then do
            for j=1 to ArrayLength(selinkqry_name) do
                flds_flag[i][j]=1
            end
            flds_flag[i][ArrayLength(selinkqry_name)+1]=1    //This will be exported to the flow table of original format without any query info
        end
        else flds_flag[i][ArrayLength(selinkqry_name)+1]=0
    end

    dim newflds[ArrayLength(selinkqry_name)+1]
    for j=1 to ArrayLength(selinkqry_name)+1 do
        for i=1 to ArrayLength(flowtb_fldinfo) do
            if flds_flag[i][j]=1 then
                newflds[j]=newflds[j]+{flowtb_fldinfo[i][1]}
        end
    end

    for i=1 to arraylength(asign) do
        flow_vw = OpenTable("All Flow", "FFB", {outputDir+"\\"+asign[i],})
        for j=1 to arraylength(selinkqry_name) do
            ExportView(flow_vw+"|", "FFB", new_flowtb[i][j],newflds[j], 
        {{"Additional Fields",{{"AB_Flow_"+selinkqry_name[j],"Real",15,4,"No"},
                             {"BA_Flow_"+selinkqry_name[j],"Real",15,4,"No"},
                             {"Tot_Flow_"+selinkqry_name[j],"Real",15,4,"No"}}}
                  })
        end
        ExportView(flow_vw+"|", "FFB", new_flowtb[i][m],newflds[m],)
    end

    //Fill in the newly added fields in the splitted flow table for each query
    for i=1 to arraylength(asign) do
        newflow_vw=null
        newflow_fldinfo=null
        For j=1 to arraylength(selinkqry_name) do
            newflow_vw = OpenTable("Splitted Flow", "FFB", {new_flowtb[i][j],})
            newflow_fldinfo = GetViewStructure(newflow_vw)   
            AB_qry_flds=null
            BA_qry_flds=null
            for k=1 to ArrayLength(newflow_fldinfo) do
                if Position(newflow_fldinfo[k][1],selinkqry_name[j])<>0 then do
                    if Position(newflow_fldinfo[k][1],"AB")<>0 then
                        AB_qry_flds=AB_qry_flds+{newflow_fldinfo[k][1]}
                    if Position(newflow_fldinfo[k][1],"BA")<>0 then
                        BA_qry_flds=BA_qry_flds+{newflow_fldinfo[k][1]}
                end 
            end
            order = {{"ID1", "Ascending"}}
            rh = GetFirstRecord(newflow_vw+ "|", order)
            while rh <> null do
                AB_vals=0
                BA_vals=0
                for k=1 to ArrayLength(AB_qry_flds) do
                    vals=GetRecordValues(newflow_vw, rh, {AB_qry_flds[k],BA_qry_flds[k]})
                    AB_vals=AB_vals+NZ(vals[1][2])
                    BA_vals=BA_vals+NZ(vals[2][2])
                    Tot_vals=AB_vals+BA_vals
                end
                SetRecordValues(newflow_vw, rh, {{"AB_Flow_"+selinkqry_name[j], AB_vals}, 
                             {"BA_Flow_"+selinkqry_name[j], BA_vals}, 
                             {"Tot_Flow_"+selinkqry_name[j],Tot_vals}}) 
                setRecord(newflow_vw, rh)
                rh = GetNextRecord(newflow_vw + "|", rh, order)
            end
        end 
    end 
    
    ok=1
    quit:
         RunMacro("close all")
         return(ok)
 
endMacro
     
     
/**********************************************************************************************************

  combine truck tt_nt assign
  
  
**********************************************************************************************************/
Macro "combine truck tt_nt assign"(arr)
   shared path, inputDir, outputDir
   stage = arr[1]

   asignbin={"lodtollop2.bin","lodtollam2.bin","lodtollpm2.bin"}  
   asigndcb={"lodtollop2.DCB","lodtollam2.DCB","lodtollpm2.DCB"}    
   copybin={"lodtollclassop2.bin","lodtollclassam2.bin","lodtollclasspm2.bin"}  
   copydcb={"lodtollclassop2.DCB","lodtollclassam2.DCB","lodtollclasspm2.DCB"}   
   viewNames ={"lodtollop2","lodtollam2","lodtollpm2"} 

   // Copy files 
   for k=1 to 3 do
      // check if copy files already exist, if exist delete 
      file=outputDir+"\\"+copybin[k]
      dif2=GetDirectoryInfo(file,"file") 
      if dif2.length>0 then deletefile(file) 
      ok=1

      CopyTableFiles(null,"FFB", outputDir+"\\"+asignbin[k], outputDir+"\\"+asigndcb[k],outputDir+"\\"+copybin[k], outputDir+"\\"+copydcb[k])
 
      // delete the original highway files once copied
      // check if copy files already exist, if exist delete 
      file=outputDir+"\\"+asignbin[k]
      dif2=GetDirectoryInfo(file,"file") 
      if dif2.length>0 then deletefile(file) 
      ok=1

      // Get copied files
      view = OpenTable("assignment", "FFB", {outputDir+"\\"+copybin[k],} )
      ok1 = (view1 != null)
    
      // number of records                                                                                                                            
      records = GetRecordCount(view, null)                                                                                                           

      hov3_info = GetFileInfo(inputDir+"\\hov3")
      hov3out_info = GetFileInfo(inputDir+"\\hov3out")  
      if (hov3_info=null & hov3out_info=null) then do                                                                                                   
      //get fields
      fvector = GetDataVectors(view+"|",{"ID1", "AB_Flow_PCE", "BA_Flow_PCE", "Tot_Flow_PCE", "AB_Time",                                           
                                                "BA_Time", "Max_Time","AB_VOC","BA_VOC","Max_VOC","AB_V_Dist_T",                                          
                                                "BA_V_Dist_T","Tot_V_Dist_T","AB_VHT","BA_VHT","Tot_VHT",                                                 
                                                "AB_Speed","BA_Speed","AB_VDF","BA_VDF","Max_VDF",                                                        
                                                "AB_Flow_dan","BA_Flow_dan","AB_Flow_dat","BA_Flow_dat",                                                  
                                                "AB_Flow_s2nn","BA_Flow_s2nn","AB_Flow_s2nh","BA_Flow_s2nh","AB_Flow_s2th","BA_Flow_s2th",          
                                                "AB_Flow_M1","BA_Flow_M1","AB_Flow_M2","BA_Flow_M2","AB_Flow_M3","BA_Flow_M3",  
                                                "AB_Flow_lhdn","BA_Flow_lhdn","AB_Flow_lhdt","BA_Flow_lhdt",                                              
                                                "AB_Flow_mhdn","BA_Flow_mhdn","AB_Flow_mhdt","BA_Flow_mhdt",                                              
                                                "AB_Flow_hhdn","BA_Flow_hhdn","AB_Flow_hhdt","BA_Flow_hhdt",
                                                "AB_Flow","BA_Flow","Tot_Flow"},) 
      //create output file
      view = CreateTable(viewNames[k], outputDir+"\\"+asignbin[k], "FFB", {
           {"ID1"            ,  "Integer (4 bytes)" , 10, null,"No",                                                 },
              {"AB_Flow_PCE"    ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Flow                                "},                             
              {"BA_Flow_PCE"    ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Flow                                "}, 
              {"Tot_Flow_PCE"   ,  "Real (8 bytes)"    , 15, 4   ,"No","Link Total Flow                             "}, 
              {"AB_Time"        ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Loaded Travel Time                       "}, 
              {"BA_Time"        ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Loaded Travel Time                       "}, 
              {"Max_Time"       ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Loaded Time                         "}, 
              {"AB_VOC"         ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Volume to Capacity Ratio                 "}, 
              {"BA_VOC"         ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Volume to Capacity Ratio                 "}, 
              {"Max_VOC"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Volume to Capacity Ratio            "}, 
              {"AB_V_Dist_T"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB vehicle miles or km of travel            "}, 
              {"BA_V_Dist_T"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA vehicle miles or km of travel            "}, 
              {"Tot_V_Dist_T"   ,  "Real (8 bytes)"    , 15, 4   ,"No","Total vehicle miles or km of travel         "}, 
              {"AB_VHT"         ,  "Real (8 bytes)"    , 15, 4   ,"No","AB vehicle hours of travel                  "}, 
              {"BA_VHT"         ,  "Real (8 bytes)"    , 15, 4   ,"No","BA vehicle hours of travel                  "}, 
              {"Tot_VHT"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Total vehicle hours of travel               "}, 
              {"AB_Speed"       ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Loaded Speed                             "}, 
              {"BA_Speed"       ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Loaded Speed                             "},       
              {"AB_VDF"         ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Volume Delay Function               "}, 
              {"BA_VDF"         ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Volume Delay Function               "}, 
              {"Max_VDF"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Link Volume Delay Function Value    "}, 
              {"AB_Flow_dan"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  dan                            "}, 
              {"BA_Flow_dan"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for dan                             "}, 
              {"AB_Flow_dat"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  dat                            "}, 
              {"BA_Flow_dat"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for dat                             "}, 
              {"AB_Flow_s2nn"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2nn                          "}, 
              {"BA_Flow_s2nn"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2nn                           "}, 
              {"AB_Flow_s2nh"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2nh                          "}, 
              {"BA_Flow_s2nh"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2nh                           "}, 
              {"AB_Flow_s2th"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2th                          "}, 
              {"BA_Flow_s2th"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2th                           "}, 
              {"AB_Flow_M1"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  M1                             "}, 
              {"BA_Flow_M1"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for M1                              "}, 
              {"AB_Flow_M2"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  M2                             "}, 
              {"BA_Flow_M2"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for M2                              "}, 
              {"AB_Flow_M3"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  M3                             "}, 
              {"BA_Flow_M3"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for M3                              "}, 
              {"AB_Flow_lhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  lhd                            "}, 
              {"BA_Flow_lhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for lhd                             "}, 
              {"AB_Flow_mhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  mhd                            "}, 
              {"BA_Flow_mhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for mhd                             "}, 
              {"AB_Flow_hhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  hhd                            "}, 
              {"BA_Flow_hhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for hhd                             "}, 
              {"AB_Flow"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Veh Flow                            "}, 
              {"BA_Flow"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Veh Flow                            "}, 
              {"Tot_Flow"       ,  "Real (8 bytes)"    , 15, 4   ,"No","Link Total Veh Flow                         "}
               })                                                                                                                
      SetView(view)                                        
                                                                                                
      //calculate and set values                                                                                                                       
      for i = 1 to records do                                                                                                                        
         rh = AddRecord(view, {  
                              {"ID1"           , fvector[1 ][i]},  
                              {"AB_Flow_PCE"   , fvector[2 ][i]},
                              {"BA_Flow_PCE"   , fvector[3 ][i]},
                              {"Tot_Flow_PCE"  , fvector[4 ][i]},
                              {"AB_Time"       , fvector[5 ][i]},
                              {"BA_Time"       , fvector[6 ][i]},
                              {"Max_Time"      , fvector[7 ][i]},
                              {"AB_VOC"        , fvector[8 ][i]},
                              {"BA_VOC"        , fvector[9 ][i]},
                              {"Max_VOC"       , fvector[10][i]},
                              {"AB_V_Dist_T"   , fvector[11][i]},
                              {"BA_V_Dist_T"   , fvector[12][i]},
                              {"Tot_V_Dist_T"  , fvector[13][i]},
                              {"AB_VHT"        , fvector[14][i]},
                              {"BA_VHT"        , fvector[15][i]},
                              {"Tot_VHT"       , fvector[16][i]},
                              {"AB_Speed"      , fvector[17][i]},
                              {"BA_Speed"      , fvector[18][i]},
                              {"AB_VDF"        , fvector[19][i]},
                              {"BA_VDF"        , fvector[20][i]},
                              {"Max_VDF"       , fvector[21][i]},
                              {"AB_Flow_dan"   , fvector[22][i]},
                              {"BA_Flow_dan"   , fvector[23][i]},
                              {"AB_Flow_dat"   , fvector[24][i]},
                              {"BA_Flow_dat"   , fvector[25][i]},
                              {"AB_Flow_s2nn" , fvector[26][i]},
                              {"BA_Flow_s2nn" , fvector[27][i]},
                              {"AB_Flow_s2nh" , fvector[28][i]},
                              {"BA_Flow_s2nh" , fvector[29][i]},
                              {"AB_Flow_s2th" , fvector[30][i]},
                              {"BA_Flow_s2th" , fvector[31][i]},
                              {"AB_Flow_M1"    , fvector[32][i]},
                              {"BA_Flow_M1"    , fvector[33][i]},
                              {"AB_Flow_M2"    , fvector[34][i]},
                              {"BA_Flow_M2"    , fvector[35][i]},
                              {"AB_Flow_M3"    , fvector[36][i]},
                              {"BA_Flow_M3"    , fvector[37][i]},
                              {"AB_Flow_lhd"   , fvector[38][i] + fvector[40][i]},
                              {"BA_Flow_lhd"   , fvector[39][i] + fvector[41][i]},
                              {"AB_Flow_mhd"   , fvector[42][i] + fvector[44][i]},
                              {"BA_Flow_mhd"   , fvector[43][i] + fvector[45][i]},
                              {"AB_Flow_hhd"   , fvector[46][i] + fvector[48][i]},
                              {"BA_Flow_hhd"   , fvector[47][i] + fvector[49][i]},
                              {"AB_Flow"       , fvector[50][i]},
                              {"BA_Flow"       , fvector[51][i]}, 
                              {"Tot_Flow"      , fvector[52][i]} 
                              })
         end
      end                        
    else do      
      fvector = GetDataVectors(view+"|",{"ID1", "AB_Flow_PCE", "BA_Flow_PCE", "Tot_Flow_PCE", "AB_Time",                                           
                                                "BA_Time", "Max_Time","AB_VOC","BA_VOC","Max_VOC","AB_V_Dist_T",                                          
                                                "BA_V_Dist_T","Tot_V_Dist_T","AB_VHT","BA_VHT","Tot_VHT",                                                 
                                                "AB_Speed","BA_Speed","AB_VDF","BA_VDF","Max_VDF",                                                        
                                                "AB_Flow_dan","BA_Flow_dan","AB_Flow_dat","BA_Flow_dat",                                                  
                                                "AB_Flow_s2nn","BA_Flow_s2nn","AB_Flow_s2nh","BA_Flow_s2nh","AB_Flow_s2th","BA_Flow_s2th",          
                                                "AB_Flow_s3nn","BA_Flow_s3nn","AB_Flow_s3nh","BA_Flow_s3nh","AB_Flow_s3th","BA_Flow_s3th",  
                                                "AB_Flow_lhdn","BA_Flow_lhdn","AB_Flow_lhdt","BA_Flow_lhdt",                                              
                                                "AB_Flow_mhdn","BA_Flow_mhdn","AB_Flow_mhdt","BA_Flow_mhdt",                                              
                                                "AB_Flow_hhdn","BA_Flow_hhdn","AB_Flow_hhdt","BA_Flow_hhdt",
                                                "AB_Flow","BA_Flow","Tot_Flow"},)
      //create output file
      view = CreateTable(viewNames[k], path+"\\"+asignbin[k], "FFB", {
           {"ID1"            ,  "Integer (4 bytes)" , 10, null,"No",                                                 },
              {"AB_Flow_PCE"    ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Flow                                "},                             
              {"BA_Flow_PCE"    ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Flow                                "}, 
              {"Tot_Flow_PCE"   ,  "Real (8 bytes)"    , 15, 4   ,"No","Link Total Flow                             "}, 
              {"AB_Time"        ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Loaded Travel Time                       "}, 
              {"BA_Time"        ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Loaded Travel Time                       "}, 
              {"Max_Time"       ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Loaded Time                         "}, 
              {"AB_VOC"         ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Volume to Capacity Ratio                 "}, 
              {"BA_VOC"         ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Volume to Capacity Ratio                 "}, 
              {"Max_VOC"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Volume to Capacity Ratio            "}, 
              {"AB_V_Dist_T"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB vehicle miles or km of travel            "}, 
              {"BA_V_Dist_T"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA vehicle miles or km of travel            "}, 
              {"Tot_V_Dist_T"   ,  "Real (8 bytes)"    , 15, 4   ,"No","Total vehicle miles or km of travel         "}, 
              {"AB_VHT"         ,  "Real (8 bytes)"    , 15, 4   ,"No","AB vehicle hours of travel                  "}, 
              {"BA_VHT"         ,  "Real (8 bytes)"    , 15, 4   ,"No","BA vehicle hours of travel                  "}, 
              {"Tot_VHT"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Total vehicle hours of travel               "}, 
              {"AB_Speed"       ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Loaded Speed                             "}, 
              {"BA_Speed"       ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Loaded Speed                             "},       
              {"AB_VDF"         ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Volume Delay Function               "}, 
              {"BA_VDF"         ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Volume Delay Function               "}, 
              {"Max_VDF"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Maximum Link Volume Delay Function Value    "}, 
              {"AB_Flow_dan"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  dan                            "}, 
              {"BA_Flow_dan"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for dan                             "}, 
              {"AB_Flow_dat"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  dat                            "}, 
              {"BA_Flow_dat"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for dat                             "}, 
              {"AB_Flow_s2nn"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2nn                          "}, 
              {"BA_Flow_s2nn"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2nn                           "}, 
              {"AB_Flow_s2nh"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2nh                          "}, 
              {"BA_Flow_s2nh"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2nh                           "}, 
              {"AB_Flow_s2th"  ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s2th                          "}, 
              {"BA_Flow_s2th"  ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s2th                           "}, 
              {"AB_Flow_s3nn"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s3nn                             "}, 
              {"BA_Flow_s3nn"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s3nn                              "}, 
              {"AB_Flow_s3nh"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s3nh                             "}, 
              {"BA_Flow_s3nh"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s3nh                              "}, 
              {"AB_Flow_s3th"     ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  s3th                             "}, 
              {"BA_Flow_s3th"     ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for s3th                              "}, 
              {"AB_Flow_lhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  lhd                            "}, 
              {"BA_Flow_lhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for lhd                             "}, 
              {"AB_Flow_mhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  mhd                            "}, 
              {"BA_Flow_mhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for mhd                             "}, 
              {"AB_Flow_hhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","AB Flow for  hhd                            "}, 
              {"BA_Flow_hhd"    ,  "Real (8 bytes)"    , 15, 4   ,"No","BA Flow for hhd                             "}, 
              {"AB_Flow"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Link AB Veh Flow                            "}, 
              {"BA_Flow"        ,  "Real (8 bytes)"    , 15, 4   ,"No","Link BA Veh Flow                            "}, 
              {"Tot_Flow"       ,  "Real (8 bytes)"    , 15, 4   ,"No","Link Total Veh Flow                         "}
               })                                                                                                                
      SetView(view)                                        
                                                                                                
      //calculate and set values                                                                                                                       
      for i = 1 to records do                                                                                                                        
         rh = AddRecord(view, {  
                              {"ID1"           , fvector[1 ][i]},  
                              {"AB_Flow_PCE"   , fvector[2 ][i]},
                              {"BA_Flow_PCE"   , fvector[3 ][i]},
                              {"Tot_Flow_PCE"  , fvector[4 ][i]},
                              {"AB_Time"       , fvector[5 ][i]},
                              {"BA_Time"       , fvector[6 ][i]},
                              {"Max_Time"      , fvector[7 ][i]},
                              {"AB_VOC"        , fvector[8 ][i]},
                              {"BA_VOC"        , fvector[9 ][i]},
                              {"Max_VOC"       , fvector[10][i]},
                              {"AB_V_Dist_T"   , fvector[11][i]},
                              {"BA_V_Dist_T"   , fvector[12][i]},
                              {"Tot_V_Dist_T"  , fvector[13][i]},
                              {"AB_VHT"        , fvector[14][i]},
                              {"BA_VHT"        , fvector[15][i]},
                              {"Tot_VHT"       , fvector[16][i]},
                              {"AB_Speed"      , fvector[17][i]},
                              {"BA_Speed"      , fvector[18][i]},
                              {"AB_VDF"        , fvector[19][i]},
                              {"BA_VDF"        , fvector[20][i]},
                              {"Max_VDF"       , fvector[21][i]},
                              {"AB_Flow_dan"   , fvector[22][i]},
                              {"BA_Flow_dan"   , fvector[23][i]},
                              {"AB_Flow_dat"   , fvector[24][i]},
                              {"BA_Flow_dat"   , fvector[25][i]},
                              {"AB_Flow_s2nn" , fvector[26][i]},
                              {"BA_Flow_s2nn" , fvector[27][i]},
                              {"AB_Flow_s2nh" , fvector[28][i]},
                              {"BA_Flow_s2nh" , fvector[29][i]},
                              {"AB_Flow_s2th" , fvector[30][i]},
                              {"BA_Flow_s2th" , fvector[31][i]},
                              {"AB_Flow_s3nn"    , fvector[32][i]},
                              {"BA_Flow_s3nn"    , fvector[33][i]},
                              {"AB_Flow_s3nh"    , fvector[34][i]},
                              {"BA_Flow_s3nh"    , fvector[35][i]},
                              {"AB_Flow_s3th"    , fvector[36][i]},
                              {"BA_Flow_s3th"    , fvector[37][i]},
                              {"AB_Flow_lhd"   , fvector[38][i] + fvector[40][i]},
                              {"BA_Flow_lhd"   , fvector[39][i] + fvector[41][i]},
                              {"AB_Flow_mhd"   , fvector[42][i] + fvector[44][i]},
                              {"BA_Flow_mhd"   , fvector[43][i] + fvector[45][i]},
                              {"AB_Flow_hhd"   , fvector[46][i] + fvector[48][i]},
                              {"BA_Flow_hhd"   , fvector[47][i] + fvector[49][i]},
                              {"AB_Flow"       , fvector[50][i]},
                              {"BA_Flow"       , fvector[51][i]}, 
                              {"Tot_Flow"      , fvector[52][i]} 
                              })
         end
      end                                                                                                             
   end   // end for loop

   vws = GetViewNames()
   for p = 1 to vws.length do
      CloseView(vws[p])
   end
      return(1)
    
    quit:   
      return(0)
EndMacro




