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


*************************************************************************************************/
Macro "Code VDF fields"
   shared path, inputDir, outputDir
   
   /* for testing
   path = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008"
   inputDir = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008\\input"                                                                        
   outputDir = "d:\\projects\\sandag\\ab_model\\application\\series12\\base2008\\output"                                                                        
   RunMacro("TCB Init")     
   */ end testing


   db_file=outputDir+"\\hwy.dbd"

   {node_lyr, link_lyr} = RunMacro("TCB Add DB Layers", db_file)
   ok = (node_lyr <> null && link_lyr <> null)
   if !ok then goto quit
  
   // Add AB_Cycle, AB_PF, BA_Cycle, and BA_PF
   vw = SetView(link_lyr)
   strct = GetTableStructure(vw)
   for i = 1 to strct.length do
      strct[i] = strct[i] + {strct[i][1]}
   end
   
   strct = strct + {{"AB_Cycle", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_Cycle", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"AB_PF", "Real", 14, 6, "True", , , , , , , null}}
   strct = strct + {{"BA_PF", "Real", 14, 6, "True", , , , , , , null}}
   ModifyTable(view1, strct)

   // Now set the view to the node layer
   SetLayer(node_lyr)
       
   nodes = GetDataVector(node_lyr+"|", "ID",)

   //create from/end node field in the line layer
   start_fld = CreateNodeField(line_lyr, "start_node", node_lyr+".ID", "From", )
   end_fld   = CreateNodeField(line_lyr, "end_node",   node_lyr+".ID", "To",    )
       
   //get the count of records for both line and node layer
   tot_line_count = GetRecordCount(line_lyr, )   //get number of records in linelayer
   tot_node_count = GetRecordCount(node_lyr, )   //get number of records in nodelayer
        
   //initilize several vectors
//   linkclass = {Vector(tot_line_count, "Float", {{"Constant", null}}),Vector(tot_line_count, "Float", {{"Constant", null}})} //line link-classes
        
   //pass the attibutes to the vectors
//   linkclass = GetDataVector(line_lyr+"|", "IFC",)
   
//   lineId = GetDataVector(line_lyr+"|", "ID",)
        
   
   //go node by node
   for i = 1 to  tot_node_count do
  
      //find the IDs of the links that connect at the node, put the effective links to vector 'link_list'
      link_list = null
      rec_handles = null

      all_link_list = GetNodeLinks(nodes[i])                                          //get all links connecting at this node
      link_list =     Vector(all_link_list.length, "Short", {{"Constant", 0}})        //to contain non-connector/ramp links coming to the node
      
      all_rec_handles = Vector(all_link_list.length, "String", {{"Constant", null}})  //to contain the record handle of all links coming to the node
      rec_handles =     Vector(all_link_list.length, "String", {{"Constant", null}})  //to contain the record handle of the non-connector/ramp links coming to the node
      
      //count how many non-centroid connector lines coming at the node
      link_count = 0 //used to count the number of non-centroid-connector/ramp lines
      two_oneway = 0
      
      for j = 1 to all_link_list.length do
         record_handle = LocateRecord(line_lyr+"|","ID",{all_link_list[j]}, {{"Exact", "True"}})
         all_rec_handles[j] = record_handle
         
         if line_lyr.[IFC]<>10 then do  //not a centroid connector
            
            //only count the links that have approach toward the node
            if((line_lyr.end_node = nodes[i] and line_lyr.Dir = 1) or line_lyr.Dir = 0 or 
               (line_lyr.start_node = nodes[i] and line_lyr.Dir = -1)) then do
               link_count = link_count + 1
               link_list[link_count] = all_link_list[j]
               rec_handles[link_count] = record_handle
               if line_lyr.Dir <> 0 then two_oneway = two_oneway+1
            end
         end
      end
      
      //if more than 2 non-centroid connector / non walk links links intersect, highest and lowest linkclass of entering links need to be found 
      if (link_count > 2 or (link_count = 2 and two_oneway = 2)) then do //when more than 2 lines intersect
         min_lc = 999
         max_lc = 0
         //process the non-centroid connector links and find the lowest and highest linkclasses
         for j = 1 to link_count do
            //find the line record that owns the line ID
            SetRecord(line_lyr, rec_handles[j]) //set the current record with the record handle stored in vector 'rec_handles'

            if ((line_lyr.end_node = nodes[i] and (line_lyr.Dir = 1 or line_lyr.Dir = 0)) or 
                 (line_lyr.start_node = nodes[i] and line_lyr.Dir = -1))then do 
               if line_lyr.[IFC] <> null and line_lyr.[IFC] > 1 then do  //don't count freeways
                  if line_lyr.[IFC] > max_lc then max_lc = line_lyr.[IFC]
                  if line_lyr.[IFC] < min_lc then min_lc = line_lyr.[IFC]
               end
            end
         
         end
      end

      //iterate through all links at this node and set cycle length
      for j = 1 to all_link_list.length do

         SetRecord(line_lyr, all_rec_handles[j]) //set the current record with the record handle stored in vector 'all_rec_handles'

         // Set AB fields for links whose end node is this node and are coded in the A->B direction
			if (line_lyr.end_node = nodes[i] and (line_lyr.Dir = 1 or line_lyr.Dir = 0)) then do
      
            //defaults are 1.0 minute cycle length and 1.0 progression factor
            c_len = 1.0
            p_factor = 1.0
               
            //set up the cycle length for AB direction if there is a gc ratio and more than 2 links 
            if (line_lyr.[ABGC]<>0 and (link_count > 2 or (link_count = 2 and two_oneway = 2))) then do 
            
               if (line_lyr.[IFC] = 2) then do
                  if (max_lc = 2)      then c_len = 2.0       //Prime arterial & Prime arterial
                  else                      c_len = 1.5       //Prime arterial & anything lower
               end
               
               if (line_lyr.[IFC] = 3) then do
                  if (max_lc > 3)      then c_len = 1.0       //Major arterial & anything lower than a Major arterial
                  else                      c_len = 1.5       //Major arterial & Prime arterial or Major arterial
               end
               
               if (line_lyr.[IFC] > 3) then do
                  if (max_lc > 2)      then c_len = 1.0       //Anything lower than a Major arterial & anything lower than a Prime arterial 
                  else                      c_len = 1.5       //Anything lower than a Major arterial & Prime arterial
               end
               
               //update attributes                  
               line_lyr.[AB_Cycle] = c_len
               line_lyr.[AB_PF] = p_factor
            
            end
         
         end // end for AB links

         // Set BA fields for links whose start node is this node and are coded in the A->B direction
         if (line_lyr.start_node = nodes[i] and (line_lyr.Dir = 0 or line_lyr.Dir = -1) ) then do

            // Only code links with an existing GC ratio (indicating a signalized intersection) 
            if (line_lyr.[BAGC]<>0 and (link_count > 2 or (link_count = 2 and two_oneway = 2))) then do 

               //defaults are 0 cycle length and 1.0 progression factor
               c_len = 0
               p_factor = 1.0
                  
               //set up the cycle length for AB direction if there is a gc ratio and more than 2 links 
               if (line_lyr.[ABGC]<>0 and (link_count > 2 or (link_count = 2 and two_oneway = 2))) then do 
               
                  if (line_lyr.[IFC] = 2) then do
                     if (max_lc = 2)      then c_len = 2.0       //Prime arterial & Prime arterial
                     else                      c_len = 1.5       //Prime arterial & anything lower
                  end
                  
                  if (line_lyr.[IFC] = 3) then do
                     if (max_lc > 3)      then c_len = 1.0       //Major arterial & anything lower than a Major arterial
                     else                      c_len = 1.5       //Major arterial & Prime arterial or Major arterial
                  end
                  
                  if (line_lyr.[IFC] > 3) then do
                     if (max_lc > 2)      then c_len = 1.0       //Anything lower than a Major arterial & anything lower than a Prime arterial 
                     else                      c_len = 1.5       //Anything lower than a Major arterial & Prime arterial
                  end
                 
                 //update attributes                  
                 line_lyr.[BA_Cycle] = c_len
                 line_lyr.[BA_PF] = p_factor

               end
            end
            
         end  // end for BA links
         
      end   // end for links
      
   end // end for nodes

   quit:
      return(ok)
EndMacro
//*****************************************************************************************************************************************************************
