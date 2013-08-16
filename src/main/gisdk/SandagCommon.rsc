//****************************************************************
//****************************************************************
//Common Macros
//
//read properties
//Export Matrix to CSV
//Export Matrix
//Matrix Size
//Create Matrix
//Aggregate Matrices
//Go GetMatrixCoreNames
//Get SL Query #
//close all
//CloseViews
//date and time
//SDdeletefile
//SDcopyfile
//SDrenamefile
//HwycadLog
//ForecastYearStr
//ForecastYearInt
//DeleteInterimFiles
//FileCheckDelete
//****************************************************************
//****************************************************************

Macro "read properties"(file,key,ctype)
  //ctype as string - Character Type - Valid "I" or anything else
  //macro only reads integers and strings
  //reads property as string and returns either an integer or a string
  
  shared path, path_study

// Get file name
  dif2=GetDirectoryInfo(path+"\\"+file, "file")     
  if dif2.length>0  then do //use scenario file
    fptr=openfile(path+"\\"+file,"r")               
  end    
  else do //use study file from data directory
    fptr=openfile(path_study+"\\data\\"+file,"r")   
  end
                                                          
// Search key in properties file                       
  a = ReadArray(fptr)                                           
  for k=1 to a.length do   
    // search for the key (line number is stored as k value)                                   
    pos1 = position(a[k],key)                              
    if pos1 =1 then do 
     // gets the integer on the rightside of "=" 
      keyword=ParseString(a[k], "=")
      keyvaltrim=trim(keyword[2])
       if ctype = "I" then do  // integer
        keyval=S2I(keyvaltrim)
      end
      else do  // if not I then it's a string
        keyval = keyvaltrim   // gets the string on the rightside of "=" 
      end
    end                                                   
  end 
  CloseFile(fptr)  
  Return(keyval)
EndMacro

Macro "Export Matrix to CSV" (path,filename,corename,filenameout)
    m = OpenMatrix(path+"\\"+filename, "True")
    mc = CreateMatrixCurrency(m,corename,,,)
    rows = GetMatrixRowLabels(mc)
    ExportMatrix(mc, rows, "Rows", "CSV", path+"\\"+filenameout, )
    return(1)
EndMacro

Macro "Export Matrix" (path,filename,corename,filenameout,outputtype)
    //path as string - path="T:\\transnet2\\devel\\sr12\\sr12_byear\\byear"
    //filename as string - must be a matrix - filename="SLAgg.mtx"
    //corename as string - corename="DAN"
    //filenameout as string - filenameout="SLAgg.csv"
    //outputtype as string - ("dBASE", "FFA", "FFB" or "CSV")
    
    m = OpenMatrix(path+"\\"+filename, "True")
    mc = CreateMatrixCurrency(m,corename,,,)
    rows = GetMatrixRowLabels(mc)
    ExportMatrix(mc, rows, "Rows", outputtype, path+"\\"+filenameout, )
    return(1)
EndMacro

Macro "Matrix Size" (path, filename, corename)
  //gets the size (number of zones) in the matrix - useful for sr11 vs sr12 and for split zones
  m = OpenMatrix(path+"\\"+filename, "True")
  base_indicies = GetMatrixBaseIndex(m)
  mc = CreateMatrixCurrency(m, corename, base_indicies[1], base_indicies[2], )
  v = GetMatrixVector(mc, {{"Marginal", "Row Count"}})
  vcount = VectorStatistic(v, "Count", )
  return(vcount)
EndMacro

Macro "Create Matrix" (path, filename, label, corenames, zone)
  Opts = null
  Opts.[File Name] = (path+"\\"+filename)
  Opts.Label = label
  Opts.Type = "Float"
  Opts.Tables = corenames
  Opts.[Column Major] = "No"
  Opts.[File Based] = "Yes"
  Opts.Compression = 0
  m = CreateMatrixFromScratch(label, zone, zone, Opts)
  return(1)
EndMacro

Macro "Aggregate Matrices" (path, xref, xrefcol1, xrefcol2, mtx, corenm, aggmtx)
  // Aggregate Matrix Options
  m = OpenMatrix(path+"\\"+mtx, "True")
  base_indicies = GetMatrixBaseIndex(m)
  Opts = null
  Opts.Input.[Matrix Currency] = {path+"\\"+mtx, corenm, base_indicies[1], base_indicies[2]}
  Opts.Input.[Aggregation View] = {xref, "xref"}
  Opts.Global.[Row Names] = {"xref."+xrefcol1, "xref."+xrefcol2}
  Opts.Global.[Column Names] = {"xref."+xrefcol1, "xref."+xrefcol2}
  Opts.Output.[Aggregated Matrix].Label = "AggMtx"+"_"+corenm
  Opts.Output.[Aggregated Matrix].Compression = 1
  Opts.Output.[Aggregated Matrix].[File Name] = path+"\\"+aggmtx

  ok = RunMacro("TCB Run Operation", 1, "Aggregate Matrix", Opts, )
  return(ok)
EndMacro

Macro "Go GetMatrixCoreNames" (path, matrix)
  m = OpenMatrix(path+"\\"+matrix, )
  core_names=GetMatrixCoreNames(m)
  return(core_names)
EndMacro

Macro "Get SL Query #" (path)
  //Modified from "Prepare queries for select link analysis, by JXu on Nov 29, 2006"
  selinkqry_file="\\selectlink_query.txt"
  fptr_from = OpenFile(path + selinkqry_file, "r")
  tmp_qry=readarray(fptr_from)
  index =1
  query=0
  selinkqry_name=null
  selink_qry=null
  subs=null
  while index <=ArrayLength(tmp_qry) do
    if left(trim(tmp_qry[index]),1)!="*" then do
      query=query+1
    end
    index = index + 1
  end
  return(query)
EndMacro

Macro "close all"
  maps = GetMaps()
  if maps <> null then do
    for k = 1 to maps[1].length do
      SetMapSaveFlag(maps[1][k],"False")
    end
  end
  RunMacro("G30 File Close All")
  mtxs = GetMatrices()
  if mtxs <> null then do
    handles = mtxs[1]
    for k = 1 to handles.length do
      handles[k] = null
    end
  end
  views = GetViews()
  if views <> null then do
    handles = views[1]
    for k = 1 to handles.length do
      handles[k] = null
    end
  end
EndMacro

Macro "CloseViews" 
    vws = GetViewNames()
    for i = 1 to vws.length do
  	  CloseView(vws[i])
  	end
EndMacro

// returns a nicely formatted day and time
Macro "date and time"
  date_arr = ParseString(GetDateAndTime(), " ")
  day = date_arr[1]
  mth = date_arr[2]
  num = date_arr[3]
  time = Left(date_arr[4], StringLength(date_arr[4])-3)
  year = SubString(date_arr[5], 1, 4)
  today = mth + "/" + num + "/" + year + " " + time
  //showmessage(today) 
  Return(today)
EndMacro

Macro "SDdeletefile"(arr)
  file=arr[1]
  dif2=GetDirectoryInfo(file,"file") 
  if dif2.length>0 then deletefile(file) 
  ok=1
  quit:
    return(ok) 
EndMacro

Macro "SDcopyfile"(arr)
  file1=arr[1]
  file2=arr[2]
  dif2=GetDirectoryInfo(file2,"file") 
  if dif2.length>0 then deletefile(file2)
  dif2=GetDirectoryInfo(file1,"file") 
  if dif2.length>0 then copyfile(file1,file2) 
  ok=1
  quit:
    return(ok)
EndMacro

Macro "SDrenamefile"(arr)
  file1=arr[1]
  file2=arr[2]
  dif1=GetDirectoryInfo(file2,"file") 
  if dif1.length>0 then deletefile(file2)
  dif2=GetDirectoryInfo(file1,"file") 
  if dif2.length>0 then RenameFile(file1, file2)  
  ok=1
  quit:
    return(ok)    
EndMacro

Macro "HwycadLog"(arr)
  shared path
  fprlog=null
  log1=arr[1]
  log2=arr[2]
  dif2=GetDirectoryInfo(path+"\\hwycadx.log","file")
  if dif2.length>0 then fprlog=OpenFile(path+"\\hwycadx.log","a") 
  else fprlog=OpenFile(path+"\\hwycadx.log","w")
  mytime=GetDateAndTime() 
  writeline(fprlog,mytime+", "+log1+", "+log2)
  CloseFile(fprlog)
  fprlog = null
  return()
EndMacro

Macro "ForecastYearStr"
  shared path_study,path
  fptr = OpenFile(path+"\\year", "r")
   strYear = ReadLine(fptr)
  closefile(fptr)
  return(strYear)
EndMacro

Macro "ForecastYearInt"
  //usage: myyear=RunMacro("ForecastYearInt")
  shared path_study,path
  fptr = OpenFile(path+"\\year", "r")
   strFyear = ReadLine(fptr)
  closefile(fptr)
  intFyear=S2I(strFyear)
  return(intFyear)
EndMacro

Macro "DeleteInterimFiles" (path, FileNameArray,RscName,MacroName,FileDescription)
  RunMacro("HwycadLog",{RscName+": "+MacroName,"SDdeletefile, "+FileDescription})
  for i = 1 to FileNameArray.length do //delete existing files
    ok=RunMacro("SDdeletefile",{path+"\\"+FileNameArray[i]}) if !ok then goto quit
  end
  quit:
    return(ok)
EndMacro

Macro "FileCheckDelete" (path,filename)
  //usage: RunMacro("FileCheckDelete",path,filename) where path and filename are strings
  di = GetDirectoryInfo(path+"\\"+filename, "File")
  if di.length > 0 then do
    ok=RunMacro("SDdeletefile",{path+"\\"+filename}) 
    return(ok)
  end
EndMacro

// Macro "getpathdirectory" doesn't allow the selected path with different path_study.
Macro "GetPathDirectory"
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
      fptr=openfile(path+"\\tplog","a")
      WriteLine(fptr, mytime)
      closefile(fptr)
      //showmessage("type in the reason why you are doing the model run in tplog")    
      //x=RunProgram("notepad "+path+"\\tplog",)
    end
    else do
      path=null
      msg1="The alternative directory selected is invalid because it has different study area! "
      msg2="Please select again within the same study area " 
      msg3=" or use the Browse button to select a different study area."
      showMessage(msg1+msg2+path_study+msg3)
    end
EndMacro
/***********************************************************************************************************************************
*
* Run Program
* Runs the program for a set of control files 
*
***********************************************************************************************************************************/

Macro "Run Program" (scenarioDirectory, executableString, controlString)


				//drive letter
				path = SplitPath(scenarioDirectory)
				
        //open the batch file to run
        fileString = scenarioDirectory+"\\programs\\source.bat"
        ptr = OpenFile(fileString, "w")
        WriteLine(ptr,path[1])
        WriteLine(ptr,"cd "+scenarioDirectory )
    
        runString = "call "+executableString + " " + controlString
        WriteLine(ptr,runString)
        
        //write the return code check
        failString = "IF NOT ERRORLEVEL = 0 ECHO "+controlString+" > failed.txt"
        WriteLine(ptr,failString) 
        
        CloseFile(ptr)
        status = RunProgram(fileString, {{"Minimize", "True"}})
       
        info = GetFileInfo(scenarioDirectory+"\\failed.txt")
        if(info != null) then do
            ret_value=0
            goto quit
        end

    Return(1)
    quit:
    	Return( RunMacro("TCB Closing", ret_value, True ) )
EndMacro

