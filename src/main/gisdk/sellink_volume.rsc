/*
	Export daily select link volumes by direction
        Need to figure out vary by # of query
        author: Ziying Ouyang zou@sandag.org
        date: 12/16/2015
*/
Macro "ExportHwyloadtoCSV Select Link" 
 
  shared  path, input_path, output_path
  path="T:\\projects\\sr13\\version13_3_0\\abm_runs\\2012"
  input_path = path+"\\input"
  output_path = path+"\\output"

  query_list = RunMacro("Get SL Query # from QRY",input_path)  
  if query_list.length > 0 then do

	  Dim v_ab_flow_slk[query_list.length], v_ba_flow_slk[query_list.length]
          Dim v_ab_flow_slk_pk[2,query_list.length], v_ba_flow_slk_pk[2,query_list.length]
	  input_file = {"hwyload_EA.bin","hwyload_AM.bin","hwyload_MD.bin","hwyload_PM.bin","hwyload_EV.bin"}
	 
	  fields = {"ID1"}
	  for j = 1 to query_list.length do
	      fields = fields + {"AB_Flow_" + query_list[j]}
              fields = fields + {"BA_Flow_" + query_list[j]}
	  end
	
	  for i = 1 to input_file.length do	                 
	     view = OpenTable("Binary Table","FFB",{output_path+"\\"+input_file[i],}, {{"Shared", "True"}})
	     SetView(view)
	     v_lodselk = GetDataVectors(view+"|", fields, )
	     for j = 1 to query_list.length do
		  v_ab_flow_slk[j] =  Nz(v_ab_flow_slk[j]) + v_lodselk[2*(j-1)+2]
		  v_ba_flow_slk[j] =  Nz(v_ba_flow_slk[j]) + v_lodselk[2*(j-1)+3]	
	     end
             if i = 2 or i = 4 then do //save AM/PM select link ab/ba volumes (AM 1, PM 2)
                  for j = 1 to query_list.length do
                  	v_ab_flow_slk_pk[i/2][j] = v_lodselk[2*(j-1)+2]  
                        v_ba_flow_slk_pk[i/2][j] = v_lodselk[2*(j-1)+3] 
                  end
             end
             
          end

	  header = "ID1"
          for j = 1 to query_list.length do
                header = header + "," + "AB_Flow_" + query_list[j] + "," + "BA_Flow_" + query_list[j] + "," + "Tot_Flow_" + query_list[j]
          end 
 	
	  f = OpenFile(output_path+ "\\"+"loadselk.csv","w")
	  WriteLine(f,header)  
	  
	  for i = 1 to v_lodselk[1].length do
	     line = i2s(v_lodselk[1][i]) 
             for j = 1 to query_list.length do
             	line = line + "," + r2s(v_ab_flow_slk[j][i]) + "," + r2s(Nz(v_ba_flow_slk[j][i])) + "," + r2s(v_ab_flow_slk[j][i]+Nz(v_ba_flow_slk[j][i])) 
             end
	     WriteLine(f,line) 
	  end

          closeFile(f)
          //Peak Period Select Link Volumes
          header = "ID1"
          for j = 1 to query_list.length do
                header = header + "," + "AB_Flow_AM_" + query_list[j] + "," + "BA_Flow_AM_" + query_list[j] + "," + "Tot_Flow_AM_" + query_list[j] + "," + "AB_Flow_PM_" + query_list[j] + "," + "BA_Flow_PM_" + query_list[j] + "," + "Tot_Flow_PM_" + query_list[j]
          end 

          f = OpenFile(output_path+ "\\"+"loadselkpk.csv","w")
	  WriteLine(f,header)  

	  for i = 1 to v_lodselk[1].length do
	     line = i2s(v_lodselk[1][i]) 
             for j = 1 to query_list.length do
             	line = line + "," + r2s(v_ab_flow_slk_pk[1][j][i]) + "," + r2s(Nz(v_ba_flow_slk_pk[1][j][i])) + "," + r2s(v_ab_flow_slk_pk[1][j][i]+Nz(v_ba_flow_slk_pk[1][j][i])) 
                line = line + "," + r2s(v_ab_flow_slk_pk[2][j][i]) + "," + r2s(Nz(v_ba_flow_slk_pk[2][j][i])) + "," + r2s(v_ab_flow_slk_pk[2][j][i]+Nz(v_ba_flow_slk_pk[2][j][i])) 
             end
	     WriteLine(f,line) 
	  end

          CloseFile(f)
  end

  else ShowMessage("Number of select links is 0")
  RunMacro("close all")
 
EndMacro
