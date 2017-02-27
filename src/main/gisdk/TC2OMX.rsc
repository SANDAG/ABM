Macro "TC to OMX"

    p="T:\\ABM\\ActivitySim\\SANDAG_ActivitySim\\data\\skims"

    files = GetDirectoryInfo(RunMacro("FormPath",{p,"*"}),"All")
    for i = 1 to files.length do
        f = RunMacro("FormPath",{p,files[i][1]})      
        subs = ParseString(f,".", {{"Include Empty",True}})
	if files[i][2] = "file" then do
           if subs[2]="mtx" then do
	      RunMacro("ExportMatrix",subs[1]+".mtx")
	   end
        end
    end
EndMacro

Macro "ExportMatrix" (matrix)
    subs = ParseString(matrix,".", {{"Include Empty",True}})
    m = OpenMatrix(matrix, "True")
    mc = CreateMatrixCurrency(m,,,,)
    CopyMatrix(mc, {
        {"File Name", subs[1]+".omx"},
	{"OMX", "True"}
	} 
    )
EndMacro

Macro "FormPath" (path_elements)
    if TypeOf(path_elements) <> "array" then do
        ShowMessage("Must form a path out of a list of elements, not: " + TypeOf(path_elements))
        ShowMessage(2)
    end
    //path_elements is an array of elements
    path = ""
    for i = 1 to path_elements.length do
        //change / to \
        p = RunMacro("NormalizePath",path_elements[i])
        if Right(p,1) = "\\" then do
            if Len(p) > 1 then do
                p = Substring(p,1,Len(p)-1)
            end
            else do
                p = ""
            end
        end
        if Left(p,1) = "\\" then do
            if Len(p) > 1 then do
                p = Substring(p,2,Len(p))
            end
            else do
                p = ""
            end
        end
        if path = "" then do
            path = p
        end
        else do
            path = path + "\\" + p
        end
    end
    return(path)
EndMacro

Macro "NormalizePath" (path)
    if Len(path) > 1 and path[2] = ":" then do
        path = Lower(path[1]) + Right(path,Len(path)-1)
    end
    return(Substitute(path,"/","\\",))
EndMacro