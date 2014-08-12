
dBox "Run ABM" title: "SANDAG ABM"
  init do 
  shared path, path_study
  shared scr

  path_study="${workpath}"
  scen_namefile = "\\scen_name.txt"
  scen_info = GetFileInfo(path_study + scen_namefile)
  if scen_info<>null then do
    fptr_from = OpenFile(path_study + scen_namefile, "r")
    scr=readarray(fptr_from)
  end
  else do
    scr=null
    while scr=Null do
      RunMacro("getpathdirectory") 
    end
    fptr_from = OpenFile(path_study + scen_namefile, "w")
    WriteArray(fptr_from,scr)
    closefile(fptr_from)
  end
//end of editing
  scen_num={1}

  path=scr[scen_num[1]] 
 
enditem

text  "Study Area:" 1,0,12
text 12,0,34,1 Framed variable: path_study

Button "Browse..." 48.5, 3, 16  do
  opts={{"Initial Directory", path_study}}
  path_study = ChooseDirectory("Choose a Study Area Directory",opts)
  path_fortran=path_study+"\\fortran"
  path_vb=path_study+"\\vb"
  scen_info = GetFileInfo(path_study + scen_namefile)
  if scen_info<>null then do
    fptr_from = OpenFile(path_study + scen_namefile, "r")
    scr=readarray(fptr_from)
  end
  else do
    scr=null
    while scr=Null do
      RunMacro("getpathdirectory") 
    end
    fptr_from = OpenFile(path_study + scen_namefile, "w")
    WriteArray(fptr_from,scr)
    closefile(fptr_from)
  end
  scen_num={1}
  path=scr[scen_num[1]]
 
enditem
//end of editing

text "Add Scenario pathes in sequence order" 1,1.5, 38
  scroll list "scens" 1, 2.5, 46, 9 list:scr variable: scen_num multiple
  help: "Select one or more scenarios to run" do
  if scr = null then do           // if any scenario chosen
    while scr=null do
      RunMacro("getpathdirectory")  
    end
    fptr_from = OpenFile(path_study + scen_namefile, "w")
    WriteArray(fptr_from,scr)
    closefile(fptr_from)
    scen_num ={1}     // first scenario chosen
    if tab_indx=1 then RunMacro("enable all1") else RunMacro("enable allfdlp") //enable all the "run" buttons
  end
  path=scr[scen_num[1]]
 
enditem

button "Quit" 48.5, 0, 16 do
  batch_run_mode= false
//added by JXu on Nov 1, 2006
  maps = GetMaps()
  if maps <> null then do
    for i = 1 to maps[1].length do
      SetMapSaveFlag(maps[1][i],"False")
    end
  end
  RunMacro("G30 File Close All")
  mtxs = GetMatrices()
  if mtxs <> null then do
    handles = mtxs[1]
    for i = 1 to handles.length do
      handles[i] = null
    end
  end
//end of editing
  Return()
enditem

button "Add" same , 1.5, 7 do
  RunMacro("getpathdirectory")
//added by JXu 
  fptr_from = OpenFile(path_study + scen_namefile, "w")
  WriteArray(fptr_from,scr)
  closefile(fptr_from)
//end of editing
  scen_num={1}
  path=scr[scen_num[1]]
  enableitem("Delete")
enditem

// delete a scenario
button "Delete" 57.5,1.5,7 do
  scr = ExcludeArrayElements(scr,scen_num[1],scen_num.length)
  while scr= null do
    RunMacro("getpathdirectory")
  end

  fptr_from = OpenFile(path_study + scen_namefile, "w")
  WriteArray(fptr_from,scr)
  closefile(fptr_from)
  scen_num={1}
  path=scr[scen_num[1]]

enditem


button "Change Study" 48.5, 4.5, 16 do
  scr = null
  RunMacro("getstudydirectory")
  scen_info = GetFileInfo(path_study + scen_namefile)
  if scen_info<>null then do
    fptr_from = OpenFile(path_study + scen_namefile, "r")
    scr=readarray(fptr_from)
    tmp_flag=0
    for i=1 to scr.length do
      if scr[i]=path then do
        i=scr.length+1
        tmp_flag=1
      end
      else i=i+1
    end
    if tmp_flag=0 then do
      scr=scr+{path}
      fptr_from = OpenFile(path_study + scen_namefile, "w")
      WriteArray(fptr_from,scr)
      closefile(fptr_from)
    end
  end
  else do
    scr={path}
    fptr_from = OpenFile(path_study + scen_namefile, "w")
    WriteArray(fptr_from,scr)
    closefile(fptr_from)
  end

  scen_num={1}

  path=scr[scen_num[1]] 
enditem


 button "Run ABM" same, 6, 16 do
       hideDbox()
       RunMacro("TCB Init")
       for sc = 1 to scen_num.length do
           path = scr[ scen_num[sc] ]
  	     ok = RunMacro("Run SANDAG ABM") 	
        if !ok then goto exit 
       end
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
       enditem

	   
 button "Run Assignment" same, 7.5, 16 do
       hideDbox()
       RunMacro("TCB Init")
       for sc = 1 to scen_num.length do
           path = scr[ scen_num[sc] ]
  	     ok = RunMacro("Run Assignment") 	
        if !ok then goto exit 
       end
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
       enditem	   
	   
   
 button "Export Data" same, 9, 16 do
       hideDbox()
       RunMacro("TCB Init")
       for sc = 1 to scen_num.length do
           path = scr[ scen_num[sc] ]
  	     ok = RunMacro("ExportSandagData") 	
        if !ok then goto exit 
       end
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
       enditem	   
	  
 button "Sum Transit Sellink" same, 10.5, 16 do
       hideDbox()
       RunMacro("TCB Init")
       for sc = 1 to scen_num.length do
           path = scr[ scen_num[sc] ]
  	     ok = RunMacro("Sum Up Select Link Transit Trips") 	
        if !ok then goto exit 
       end
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
       enditem	   

  button "Sum Hwy Sellink" same, 12, 16 do
       hideDbox()
       RunMacro("TCB Init")
       for sc = 1 to scen_num.length do
           path = scr[ scen_num[sc] ]
  	     ok = RunMacro("Sum Up Select Link Highway Trips") 	
        if !ok then goto exit 
       end
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
       enditem	   
     
 
EndDbox

// Macro "getpathdirectory" doesn't allow the selected path with different path_study.
Macro "getpathdirectory"
  shared path,path_study,scr
  opts={{"Initial Directory", path_study}}
  tmp_path=choosedirectory("Choose an alternative directory in the same study area", opts)
  strlen=len(tmp_path)                
  for i = 1 to strlen do
    tmp=right(tmp_path,i)
    tmpx=left(tmp,1)
    if tmpx="\\" then goto endfor 
  end 
  endfor:
    strlenx=strlen-i
    tmppath_study=left(tmp_path,strlenx)
    if path_study=tmppath_study then do
      path=tmp_path
      tmp_flag=0
      for i=1 to scr.length do
        if scr[i]=path then do
          tmp_flag=1
          i=scr.length+1
        end
        else i=i+1
      end
      if tmp_flag=0 then do
        tmp = CopyArray(scr)
        tmp = tmp + {tmp_path}
        scr = CopyArray(tmp)
      end
      //showmessage("write description of the alternative in the head file")
      //x=RunProgram("notepad "+path+"\\head",)
      mytime=GetDateAndTime()
    
    end
    else do
      path=null
      msg1="The alternative directory selected is invalid because it has different study area! "
      msg2="Please select again within the same study area " 
      msg3=" or use the Browse button to select a different study area."
      showMessage(msg1+msg2+path_study+msg3)
    end
EndMacro