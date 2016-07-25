
dBox "Setup Scenario" title: "SANDAG ABM"
  init do 
  shared path, path_study
  path = "${workpath}"
enditem

// set model run parameters
button "Set Model Parameters" 0,0,30, 2 do
  RunMacro("TCB Init")
  runString = "T:\\ABM\\release\\ABM\\dist\\parameterEditor.exe "+path
  RunMacro("HwycadLog",{"gui.rsc:","Create a scenario"+" "+runString})
  ok = RunMacro("TCB Run Command", 1, "Create a scenario", runString)
enditem

// check inputs
button "Check Inputs" 0, 3, 30, 2 do
  RunMacro("TCB Init")
  runString = "T:\\ABM\\release\\ABM\\dist\\check.bat"
  RunMacro("HwycadLog",{"gui.rsc:","Create a scenario"+" "+runString})
  ok = RunMacro("TCB Run Command", 1, "Create a scenario", runString)
enditem

// run model
button "Run Model" 0, 6, 30, 2 do
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
button "Quit" 0, 9, 30, 2 do
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