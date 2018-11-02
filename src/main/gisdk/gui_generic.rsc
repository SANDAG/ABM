
dBox "Setup Scenario" title: "SANDAG ABM"
  init do 
  shared path, path_study
  path = "${workpath}"
enditem

// set model run parameters
button "Set Model Parameters" 0,0,30, 2 do
  RunMacro("TCB Init")
   path_parts = SplitPath(path)
   path_no_drive = path_parts[2]+path_parts[3]
   drive=path_parts[1]  
   path_forward_slash =  Substitute(path_no_drive, "\\", "/", )
  //runString = "T:\\ABM\\release\\ABM\\${version}\\dist\\parameterEditor.exe "+path
  runString = path+"\\python\\parameterEditor.bat "+drive+" "+path_no_drive+" "+path_forward_slash
  RunMacro("HwycadLog",{"gui.rsc:","Create a scenario"+" "+runString})
  ok = RunMacro("TCB Run Command", 1, "Create a scenario", runString)
enditem

// run model
button "Run Model" 0, 3, 30, 2 do
       //hideDbox()
       RunMacro("TCB Init")
       RunMacro("getpathdirectory")
       pFile_info = GetFileInfo(path+'\\conf\\sandag_abm.properties')
       if pFile_info=null then do
	   		CopyFile(path+"\\conf\\sandag_abm_standard.properties", path+"\\conf\\sandag_abm.properties")
       end
       ok = RunMacro("Run SANDAG ABM") 	
       if !ok then goto exit 
       exit:
       showdbox()   
       RunMacro("TCB Closing", run_ok, "False")
enditem

//exit
button "Quit" 0, 6, 30, 2 do
  RunMacro("G30 File Close All")
  Return()
enditem

EndDbox

// Macro "getpathdirectory" doesn't allow the selected path with different path_study.
Macro "getpathdirectory"
  shared path,path_study,scr
  opts={{"Initial Directory", path}}
  path=choosedirectory("Choose a scenario folder", opts)
EndMacro