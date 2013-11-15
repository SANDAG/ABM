/*
 Create landuse skims(LUZ skims):

About: 
 Creates LUZ skims from the following TAZ skims including length, time, and cost:
 	impdat_AM.mtx (Length (Skim), *STM_AM (Skim), dat_AM – itoll_AM)
 	impdat_MD.mtx (Length (Skim), *STM_MD (Skim), dat_MD – itoll_MD)
 	impmhdt_AM.mtx (Length (Skim), *STM_AM (Skim),  mhdt – ITOLL2_AM)
 	impmhdt_MD.mtx (Length (Skim), *STM_MD (Skim),  mhdt – ITOLL2_MD)

Inputs:
	1) luzToTazSeries13.xls  (Luz to TAZ reference)  
	2) ExternalZones.xls     (Luz internal to external reference) 
 	3) impdat_AM.mtx 
 	4) impdat_MD.mtx 
 	5) impmhdt_AM.mtx 
 	6) impmhdt_MD.mtx  

Outputs:
 	1) impdat_AM.mtx (csv)
 	2) impdat_MD.mtx (csv)
 	3) impmhdt_AM.mtx (csv)
 	4) impmhdt_MD.mtx (csv)

  
*/

Macro "Create LUZ Skims"

  shared path, inputDir, outputDir

  // Input Files
  ext_luz_excel  = inputDir+"\\ExternalZones.xls"
  luz_taz_excel   = inputDir+"\\luzToTazSeries13.xls" 
  
  // Temp files
  ext_luz_file  = outputDir+"\\ExternalZones.bin"
  luz_taz_file  = outputDir+"\\luzToTazSeries13.bin" 
  tempfile      = outputDir+"\\temp_luz.bin"
  luzskims_bin  = outputDir+"\\temp_luz_export.bin" 
    
  // Convert excel to bin file
  ExportExcel(ext_luz_excel, "FFB", ext_luz_file, )
  ExportExcel(luz_taz_excel, "FFB", luz_taz_file, )

  // Open tables                                               
  luztaz_view  =  Opentable("luztaz", "FFB", {luz_taz_file})   
  ext_luz_view =  Opentable("luzIE", "FFB", {ext_luz_file})    

  /* ------------------------------------------------------------------------------------------------ 
  //  Step 0: Prepares input and output files 
  --------------------------------------------------------------------------------------------------*/
  // Create list of LUZ I+E zones
  LUZ_I = V2A(GetDataVector(luztaz_view+"|","luz_id",))
  LUZ_E = V2A(GetDataVector(ext_luz_view+"|","External LUZ",{{"Sort Order", {{"External LUZ", "Ascending"}}}}))
  LUZ_IE = LUZ_I + LUZ_E
  
  // Get the maximum internal zone
  max_LUZ_I = ArrayMax(LUZ_I)
  min_LUZ_E = ArrayMin(LUZ_E)
  max_LUZ_E = ArrayMax(LUZ_E)
      
  // Write the list to a file (temp luz zonal file)
  luz_vw = CreateTable("luz", tempfile, "FFB",{
                      {"luz_id", "Integer", 8, null, "Yes"}})
  SetView(view)
  for r = 1 to LUZ_IE.length do
    rh = AddRecord(luz_vw, {
         {"luz_id", LUZ_IE[r]}
         })
  end 
  	
   	        	  
  period  = {"AM","MD"}
  vehicle = {"dat","mhdt"}

  for v =1 to vehicle.length do
    for p= 1 to period.length do
 
       /* ------------------------------------------------------------------------------------------------  
       //Step 1: Create matrix with LUZ internal and external Zones
       --------------------------------------------------------------------------------------------------*/
        // Input taz skim
       	tazskims       = outputDir+"\\imp"+vehicle[v]+"_"+period[p]+".mtx"  
       	 
       	// Output luz skim (mtx and csv files)
       	luzskims      = outputDir+"\\luz_imp"+vehicle[v]+"_"+period[p]+".mtx" 
       	luzskims_csv  = outputDir+"\\luz_imp"+vehicle[v]+"_"+period[p]+".csv" 
       	  	      
        // List of cores (same core names for inputs and outputs)  
        m = OpenMatrix(tazskims, ) 
        coreNames = GetMatrixCoreNames(m)	
        coreNames = Subarray(coreNames,2,3)   // Only the distance, time, & toll skims
        
        // Open the temp luz zonal file
  	    luz_view  =  Opentable("luz", "FFB", {tempfile})  
  	      
        // Create output matrix file with both LUZ internal & external zones
       	luz_mat =CreateMatrix({luz_view+"|", "luz_id", "All"},
       	   										{luz_view+"|", "luz_id", "All"},
       	   										{{"File Name", luzskims}, {"Type", "Float"}, {"Tables",coreNames}})
       	
       	// Create LUZ II, IE/EI and EE indices
       	SetView(luz_view)
       	set_i = SelectByQuery("Internal", "Several", "Select * where luz_id <= "+ String(max_LUZ_I),) 
       	Internal = CreateMatrixIndex("Internal", luz_mat, "Both", luz_view +"|Internal", "luz_id", "luz_id" )
       	
       	set_e = SelectByQuery("External", "Several", "Select * where (luz_id >= "+ String(min_LUZ_E) +" & luz_id <= "+ String(max_LUZ_E)+")",)   
       	External = CreateMatrixIndex("External", luz_mat, "Both", luz_view +"|External", "luz_id", "luz_id" )  
        CloseView(luz_view)    
        
         // Create luz currencies for II, EI and IE (each array has 3 currencies; one of each core)
         mc_luz_II = CreateMatrixCurrencies(luz_mat,"Internal","Internal",)
         mc_luz_EI = CreateMatrixCurrencies(luz_mat,"External","Internal",)
         mc_luz_IE = CreateMatrixCurrencies(luz_mat,"Internal","External",)
         mc_luz_EE = CreateMatrixCurrencies(luz_mat,"External","External",)
             
      /* ------------------------------------------------------------------------------------------------  
       // Step 2: Create LUZ internal skims from TAZ skims   
       --------------------------------------------------------------------------------------------------*/
     	 // Create aggregate tables for each selected cores
     	 for c = 1 to coreNames.length do 
     	    
     	    // Create currency in the input file
         	mc = CreateMatrixCurrency(m, coreNames[c], , , )
         	row_names = {"luztaz.taz", "luztaz.luz_id"}                             
         	col_names = {"luztaz.taz", "luztaz.luz_id"}                             
         
         	// Create LUZ internal skims 
         	tempLuzMtx = outputDir+"\\temp"+vehicle[v]+period[p]+"_"+String(c)+".mtx"                                                                     
         	AggregateMatrix(mc, row_names, col_names,                             
         	{{"File Name", tempLuzMtx},                                
         	{"Label", "LUZ"+coreNames[c]},                                  
         	{"File Based", "Yes"}}) 
         
          // Add the aggregate table to the internal zones in the new core 
         	mat = OpenMatrix(tempLuzMtx, ) 
         	mc_temp = CreateMatrixCurrency(mat, coreNames[c], , , )
         	mc_luz_II.(coreNames[c]):=  mc_temp
         
          // Get the internal zones to the corresponding 
          intZones = GetDataVector(ext_luz_view+"|","Internal Cordon LUZ",)
          extZones = GetDataVector(ext_luz_view+"|","External LUZ",)   
          distance = GetDataVector(ext_luz_view+"|","Miles to be Added to Cordon Point",)
          time     = GetDataVector(ext_luz_view+"|","Minutes to be Added to Cordon Point",)   
           
          // Add the EI and IE mat values based on cordon data                              
          for e = 1 to extZones.length do
            // Get mat values for the corresponding internal zones
            vec_EI = GetMatrixVector(mc_temp, {{"Row",intZones[e]}})
            vec_IE = GetMatrixVector(mc_temp, {{"Column",intZones[e]}}) 
            
            // Add cordon time, distance (depending on the core)
            if (c = 1) then do  // length
            	vec_EI = vec_EI  + distance[e]
            	vec_IE = vec_IE  + distance[e]
            end
            if (c = 2) then do // time
            	 vec_EI = vec_EI + time[e]
            	 vec_IE = vec_IE + time[e]
            end
            
            // Set vectors to the matrix
            SetMatrixVector(mc_luz_EI.(coreNames[c]), vec_EI, {{"Row",extZones[e]}})
            SetMatrixVector(mc_luz_IE.(coreNames[c]), vec_IE, {{"Column",extZones[e]}})
          end  // ext zones	
          	
          // EE values are filled based on II, and IE values
          for i = 1 to extZones.length do   
             for j = 1 to extZones.length do
               // Get II value if Intrazonal
               if i=j then do
                 II_val = GetMatrixValue(mc_luz_II.(coreNames[c]), String(intZones[i]),String(intZones[j]))
                 SetMatrixValue(mc_luz_EE.(coreNames[c]),String(extZones[i]),String(extZones[j]),II_val)
               end 
               else do
               	IE_val = GetMatrixValue(mc_luz_IE.(coreNames[c]), String(intZones[i]),String(extZones[j]))
               	if (c=1) then IE_val = IE_val + distance[i]
               	if (c=2) then IE_val = IE_val + time[i]
               	SetMatrixValue(mc_luz_EE.(coreNames[c]),String(extZones[i]),String(extZones[j]),IE_val)
               end // if
            end  // j loop
          end  // i loop
     	 end  // cores 
     	 
        /* ------------------------------------------------------------------------------------------------   
        // Step 3: Export to CSV file
        --------------------------------------------------------------------------------------------------*/  
        // Export matrix values to a temp bin file
        SetMatrixIndex(luz_mat, "All", "All")
        CreateTableFromMatrix(luz_mat, luzskims_bin, "FFB", {{"Complete", "Yes"}})  
        
        // Add header and then export to csv file
        export_vw = OpenTable("luztaz", "FFB", {luzskims_bin})
        strct = GetTableStructure(export_vw)
        for s = 1 to strct.length do
           strct[s] = strct[s] + {strct[s][1]}
        end
           
        // Rename to first and second columns to Origin and Destination
        strct[1][1] = "origin LUZ"
        strct[2][1] = "destination LUZ"
        ModifyTable(export_vw, strct)   
        
        // Export to CSV file
        ExportView(export_vw+"|", "CSV", luzskims_csv, null,{{"CSV Header", "True"}})
        CloseView(export_vw)

    end // time period
  end // vehicle

  
  /* ------------------------------------------------------------------------------------------------   
  // Step 4: Close all views and delete temp files
   --------------------------------------------------------------------------------------------------*/
  vws = GetViewNames()
  if vws<> null then do
    for w = 1 to vws.length do
      CloseView(vws[w])
    end
  end

  // Close matrices
  mtxs = GetMatrices()
  if mtxs <> null then do
    handles = mtxs[1]
    for m = 1 to handles.length do
      handles[m] = null
    end
  end   	
  
  // Close rest     
  RunMacro("G30 File Close All")

  // Delete temp matrices 
    for v =1 to vehicle.length do
       for p= 1 to period.length do 
          for c = 1 to coreNames.length do 
             DeleteFile(outputDir+"\\temp"+vehicle[v]+period[p]+"_"+String(c)+".mtx")
          end
          // Also delete tcad headers for the CSV file (as the csv files have headers)
          DeleteFile(outputDir+"\\luz_imp"+vehicle[v]+"_"+period[p]+".DCC")
       end
    end   
  
  // Delete temp luz files
  delFiles = {"temp_luz.bin","temp_luz.BX","temp_luz.DCB","luzToTazSeries13.bin","luzToTazSeries13.DCB",
  	         "ExternalZones.bin","ExternalZones.DCB","temp_luz_export.bin","temp_luz_export.DCB"}
  for d =1 to delFiles.length do
  	 info = GetFileInfo(outputDir+"\\"+delFiles[d])
  	 if info[1] <> null then DeleteFile(outputDir+"\\"+delFiles[d])
  end
   
EndMacro                                    