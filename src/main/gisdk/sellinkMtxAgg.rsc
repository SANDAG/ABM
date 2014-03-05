//Aggregate transit select link trip matrix
//Input: trn_sellinkxxxxx_xx.mtx select link trip table by access mode, line haul mode, and time of day
//       a text file with a list of TAPs to sum up trips
//Ouput: trn_sellink_total.csv
//Author: Ziying Ouyang
//        @ziying.ouyang@sandag.org
//Date:   Feb 5, 2014

 
Macro "Sum Up Select Link Transit Trips"
   shared path_study, path, outputDir
  // RunMacro("TCB Init")
  // path_study = "t:\\projects\\sr13\\sdf_projevl\\complete"
  // path = "t:\\projects\\sr13\\sdf_projevl\\complete\\tran_rpd2"

   outputDir = path+"\\output"
   sellink_file = outputDir+"\\trn_sellinkWLK_LRT_EA.mtx"
   if GetFileInfo(sellink_file) <> null then do  
	   sum_all = 0
	   sum_sg = 0
	   pct_sg = 0
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

           //open smart growth tap CSV file
	   in_file = path_study + "\\sg_tap.csv"
           if GetFileInfo(in_file) <> null then do  

	     view = OpenTable("SmartGrowthTap","CSV",{in_file,}, {{"Shared", "True"}})
	     SetView(view) 
	     vw_TAP = GetView()
	     view_set = vw_TAP +"|"

	     for per = 1 to periodName.length do
	        for mat = 1 to matrixCore.length do          
	        
	          sellkmtx =  outputDir+"\\trn_sellink"+matrixCore[mat]+periodName[per]+".mtx"
	          m = OpenMatrix(sellkmtx,)
	        
	          sg_index = CreateMatrixIndex("Sub Area Index", m, "Both",view_set, "sg_tap", "sg_tap" )
	          
	          mc =  CreateMatrixCurrency(m, , , , )   
	          mc_sg =  CreateMatrixCurrency(m, ,sg_index,sg_index, )      
		  sum_row = GetMatrixMarginals(mc, "Sum", "row" )
	          sum_col = GetMatrixMarginals(mc, "Sum", "Column" )
	          sum_row_sg = GetMatrixMarginals(mc_sg, "Sum", "row" )
	          sum_col_sg = GetMatrixMarginals(mc_sg, "Sum", "Column" )
	
	          sum_all= sum_all + Sum(sum_row) + Sum(sum_col)
	          sum_sg= sum_sg + Sum(sum_row_sg) + Sum(sum_col_sg)  
	        
	          DeleteMatrixIndex(m, "Sub Area Index")
	         
	          //close matrix
		  mc = null
	          m = null
	        
	      end
	  end

	  if sum_all > 0 then  pct_sg = sum_sg /sum_all
	
	  out_file = path_study + "\\trn_sellink_sg.csv"
	
	  dif2 = GetDirectoryInfo(out_file,"file")
	  if dif2.length <= 0 then do
		fpr = OpenFile(out_file,"w")  
		WriteLine(fpr, "Scenario, Total select link trips, smart growth select link trips, % of sg trips")
          end
	  else do
            fpr = OpenFile(out_file,"a")
	  end	 
	
	  WriteLine(fpr,path + "," + r2s(sum_all) + "," + r2s(sum_sg) + "," + r2s(pct_sg))
	  CloseFile(fpr)
    end
    else ShowMessage("Missing smart growth tap file in " + path_study)
  End
  RunMacro("close all")
  RunMacro("TCB Closing", ok, "False")
  return(1) 
EndMacro

Macro "Sum Up Select Link Highway Trips"

 shared path_study, path, outputDir
   shared path_study, path, outputDir
   //RunMacro("TCB Init")
   //path_study = "t:\\projects\\sr13\\sdf_projevl\\complete"
   //path = "t:\\projects\\sr13\\sdf_projevl\\complete\\hwy01fwy5"
   
   outputDir = path+"\\output"
   sellink_file = outputDir+"\\select_EA.mtx"
   if GetFileInfo(sellink_file) <> null then do  

          m = OpenMatrix(sellink_file,)
	  coreNames = GetMatrixCoreNames(m)
          m=null

          //hard coded the occupancy rate, could be improved based on coreNames
          occupancy = {1, 1, 2, 2, 2, 3.34, 3.34, 3.34, 1, 1, 1, 1, 1, 1}
    
	  sum_all = 0
	  sum_sg = 0
	  pct_sg = 0
          sum_ind = 0
	  pct_ind = 0
        
 	  periodName = {"_EA","_AM","_MD","_PM","_EV"}
      
	  //open smart growth TAZ CSV file
	  in_file = path_study + "\\sg_taz.csv"
          in_file_indian =   path_study + "\\indian_reservation_taz.csv"

          if GetFileInfo(in_file) <> null then do  
	       view = OpenTable("SmartGrowthTAZ","CSV",{in_file,}, {{"Shared", "True"}})
	       SetView(view) 
	       vw_TAZ = GetView()
	       view_set = vw_TAZ +"|"	
               
               view_ind = OpenTable("IndianReservationTAZ","CSV",{in_file_indian,}, {{"Shared", "True"}})
	       SetView(view_ind) 
	       vw_TAZ_ind = GetView()
	       view_set_ind = vw_TAZ_ind +"|"
               
               for per = 1 to periodName.length do    
	        
	           sellkmtx =  outputDir + "\\select" + periodName[per] +".mtx"
 		   m = OpenMatrix(sellkmtx,)
	       
	           sg_index = CreateMatrixIndex("SG Index", m, "Both",view_set,"SG_TAZ" , "SG_TAZ" )
                   ind_index = CreateMatrixIndex("Indian Index", m, "Both",view_set_ind,"Indian_TAZ" , "Indian_TAZ" )

		   mc =  CreateMatrixCurrencies(m, "Rows", "Columns", )   
		   mc_sg =  CreateMatrixCurrencies(m, sg_index, sg_index,)  	
                   mc_ind =  CreateMatrixCurrencies(m, ind_index, ind_index,)  	

                   // select link assignment trip matrix includes the total trips as the last core
	           for core = 1 to coreNames.length - 1 do
			  sum_row = GetMatrixMarginals(mc.(coreNames[core]), "Sum", "row" )
		          sum_col = GetMatrixMarginals(mc.(coreNames[core]), "Sum", "Column" )

		          sum_row_sg = GetMatrixMarginals(mc_sg.(coreNames[core]), "Sum", "row" )
		          sum_col_sg = GetMatrixMarginals(mc_sg.(coreNames[core]), "Sum", "Column" )
		
                          sum_row_ind = GetMatrixMarginals(mc_ind.(coreNames[core]), "Sum", "row" )
		          sum_col_ind = GetMatrixMarginals(mc_ind.(coreNames[core]), "Sum", "Column" )

		          sum_all = sum_all + (Sum(sum_row) + Sum(sum_col)) * occupancy[core] 
		          sum_sg = sum_sg + (Sum(sum_row_sg) + Sum(sum_col_sg)) * occupancy[core]  
                          sum_ind = sum_ind + (Sum(sum_row_ind) + Sum(sum_col_ind)) * occupancy[core]  
		         
		          
	           end

	          //close matrix
		    DeleteMatrixIndex(m, "SG Index")
                    DeleteMatrixIndex(m, "Indian Index")
                    mc = null
                    mc_sg = null
                    mc_ind = null
	            m = null
               end
  
	        if sum_all > 0 then  do
			pct_sg = sum_sg /sum_all
		        pct_ind = sum_ind/sum_all
                end

		out_file = path_study + "\\hwy_sellink.csv"
		
		dif2 = GetDirectoryInfo(out_file,"file")
		if dif2.length <= 0 then do
		    fpr = OpenFile(out_file,"w")  
		    WriteLine(fpr, "Scenario, Total select link trips, smart growth select link trips, % of sg trips, indian reservation trips, % of indian reservation trips")
	        end
		else do
	            fpr = OpenFile(out_file,"a")
		end	 
		
		WriteLine(fpr,path + "," + r2s(sum_all) + "," + r2s(sum_sg) + "," + r2s(pct_sg) + "," + r2s(sum_ind) + "," + r2s(pct_ind))
		CloseFile(fpr)

       end
       else ShowMessage("Missing smart growth TAZ file in " + path_study)

 end
  RunMacro("close all")
  RunMacro("TCB Closing", ok, "False")
  return(1) 

EndMacro