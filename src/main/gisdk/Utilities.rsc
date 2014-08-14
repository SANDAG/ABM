Macro "ModifyOptionsOption" (options_array,option_key,key,value)
    spec = options_array.(option_key)
    spec.(key) = value
EndMacro

Macro "CloseAll"
    // close all files in workspace
    map_arr=GetMaps()
    if ArrayLength(map_arr)>0 then do
        open_maps=ArrayLength(map_arr[1])
        for mm=1 to open_maps do
            CloseMap(map_arr[1][mm])
        end
    end

    On NotFound goto no_more_eds
    still_more_eds:
    CloseEditor()
    goto still_more_eds

    no_more_eds:
    On NotFound default

    view_arr=GetViews()
    if ArrayLength(view_arr)>0 then do
        On NotFound goto cont_views
        open_views=ArrayLength(view_arr[1])
        for vv=1 to open_views do
            CloseView(view_arr[1][vv])
            cont_views:
        end
    end
endMacro

Macro "IsMapOpen" (map)
    maps = GetMapNames()
    for i = 1 to maps.length do
        if maps[i] = map then do
            return(True)
        end
    end
    return(False)
EndMacro

Macro "IsViewOpen" (view)
    views = GetViewNames()
    for i = 1 to views.length do
        if views[i] = view then do
            return(True)
        end
    end
    return(False)
EndMacro

Macro "SafeDeleteFile" (file)
    //just ignores any errors
    if GetFileInfo(file) <> null then do
        On Error goto safe_delete_error
        DeleteFile(file)
        safe_delete_error:
        On Error default
    end
EndMacro

Macro "DeleteFiles" (path)
    files = GetDirectoryInfo(path,"All")
    for i = 1 to files.length do
        DeleteFile(RunMacro("FormPath",{path,files[i][1]}))
    end
EndMacro

Macro "SafeDeleteFiles" (path)
    files = GetDirectoryInfo(path,"All")
    for i = 1 to files.length do
        RunMacro("SafeDeleteFile",RunMacro("FormPath",{path,files[i][1]}))
    end
EndMacro

Macro "SafeDeleteDatabase" (database_file)
    //just ignores any errors
    On Error goto safe_delete_database_error
    On NotFound goto safe_delete_database_error
    DeleteDatabase(file)
    safe_delete_database_error:
    On Error default
    On NotFound default
EndMacro

Macro "NormalizePath" (path)
    if Len(path) > 1 and path[2] = ":" then do
        path = Lower(path[1]) + Right(path,Len(path)-1)
    end
    return(Substitute(path,"/","\\",))
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

Macro "CreateMapForDatabase" (database_file,map_name)
    linfo=GetDBInfo(database_file)
    scope=linfo[1]
    maps = GetMapNames()
    map_name_not_ok = true
    while map_name_not_ok do
        map_name_not_ok = false
        for i = 1 to maps.length do
            if maps[i] = map_name then do
                map_name = map_name + " "
                map_name_not_ok = true
            end
        end
    end
    map=createMap(map_name,{{"Scope", scope},{"Auto Project","True"}})
    SetMapUnits("Miles")
EndMacro

Macro "OpenDatabaseInMap" (database_file,map)
    info=GetDBInfo(database_file)
    map_made = False
    if map <> null then do
        map_made = RunMacro("IsMapOpen",map)
    end
    else do
        map = info[2]
    end
    if not map_made then do
        RunMacro("CreateMapForDatabase",database_file,map)
    end
    NewLayer=GetDBLayers(database_file)
    layer = AddLayer(map,NewLayer[1],database_file,NewLayer[1])
    if NewLayer.length = 2 then do
        //assumes it is a network file, and hides the nodes and adds the lines
        SetLayerVisibility(map + "|" + layer,"False")
        AddLayer(map,NewLayer[2],database_file,NewLayer[2])
    end
    return(map)
EndMacro

Macro "OpenDatabase" (database_file)
    return(RunMacro("OpenDatabaseInMap",database_file,))
EndMacro

Macro "OpenRouteSystemInMap" (route_system_file,map)
    info = GetRouteSystemInfo(route_system_file)
    map_made = False
    if map <> null then do
        map_made = RunMacro("IsMapOpen",map)
    end
    else do
        map = info[3].Label
    end
    if not map_made then do
        RunMacro("CreateMapForDatabase",info[1],map)
    end
    RunMacro("Set Default RS Style",AddRouteSystemLayer(map,info[3].Label,route_system_file,),"TRUE","FALSE")
    return(map)
EndMacro

Macro "OpenRouteSystem" (route_system_file)
    return(RunMacro("OpenRouteSystemInMap",route_system_file,))
EndMacro

Macro "CleanRecordValuesOptionsArray" (options_array,view_name)
    for i = 1 to options_array.length do
        options_array[i][1] = Substitute(options_array[i][1],view_name + ".","",)
    end
EndMacro

Macro "FormFieldSpec" (view,field)
    //don't think the following is necessary
    //issue_chars = {":"}
    //fix = False
    //for i = 1 to issue_chars.length do
    //    if Position(field,issue_chars[i]) > 0 then do
    //        fix = True
    //    end
    //end
    //if fix then do
    //    field = "[" + field + "]"
    //end
    return(view + "." + field)
EndMacro

Macro "ToString" (value)
    type = TypeOf(value)
    if type = "string" then do
        return(value)
    end
    else if type = "int" then do
        return(i2s(value))
    end
    else if type = "double" then do
        return(r2s(value))
    end
    else if type = "null" then do
        return("")
    end
    ShowMessage("Type " + type + " not supported by ToString method")
EndMacro

Macro "GetArrayIndex" (array,value)
    //returns the index of value in array, or 0 if it is not found
    type = TypeOf(value)
    for i = 1 to array.length do
        if TypeOf(array[i]) = type and array[i] = value then do
            return(i)
        end
    end
    return(0)
EndMacro

Macro "ArraysEqual" (array1,array2)
    if array1.length <> array2.length then do
        return(False)
    end
    for i = 1 to array1.length do
        if TypeOf(array1[i]) = "array" then do
            if TypeOf(array2[i]) = "array" then do
                if not RunMacro("ArraysEqual",array1[i],array2[i]) then do
                    return(False)
                end
            end
            else do
                return(False)
            end
        end
        else if TypeOf(array2[i]) = "array" then do
            return(False)
        end
        else do
            if array1[i] <> array2[i] then do
                return(False)
            end
        end
    end
    return(True)
EndMacro

Macro "GetDatabaseColumns" (database_file,layer_name)
    columns = null
    if database_file <> null and GetFileInfo(database_file) <> null then do
        current_layer = GetLayer()
        current_view = GetView()
        lyr = AddLayerToWorkspace("__temp__",database_file,layer_name,{{"Shared","True"}}) 
        layer_in_use = lyr <> "__temp__"
        SetLayer(lyr)
        v = GetView()
        info = GetTableStructure(v)
        for i = 1 to info.length do
            columns = columns + {info[i][1]}
        end
        if not layer_in_use then do
            DropLayerFromWorkspace(lyr)
        end
        if current_layer <> null then do
            SetLayer(current_layer)
        end
        if current_view <> null then do
            SetView(current_view)
        end
    end
    return(columns)
EndMacro

//same as built in TC function, but with error checking for escape and for if a file is in use
Macro "ChooseFileName" (file_types,title,options)
    on escape do 
        fname = null
        goto cfn_done
    end
    openfile:
    fname = ChooseFileName(file_types,title,options)
    if FileCheckUsage({fname},) then do
        ShowMessage("File already in use.  Please choose again.")
        goto openfile
    end
    cfn_done:
    on escape default
    return(fname)
EndMacro

Macro "RunProgram" (program_with_arguments,working_directory) //can't get output file to work right now..boo hoo
    wd = ""
    if working_directory <> null then do
        wd = " /D" + working_directory
    end
    RunProgram("cmd /s /c \"start \"cmd\" " + wd + " /WAIT " + program_with_arguments + "\"",)
EndMacro

Macro "AddElementToSortedArraySet" (array,element)
    index = array.length + 1
    not_done = True
    for i = 1 to array.length do
        if not_done then do
            if element = array[i] then do
                index = -1
                not_done = False
            end
            else if element < array[i] then do
                index = i
                not_done = False
            end
        end
    end
    if index > 0 then do
        array = InsertArrayElements(array,index,{element})
    end
    return(array)
EndMacro

Macro "ClearAndDeleteDirectory" (path)
    //this doesn't do any error handling
    info = GetDirectoryInfo(RunMacro("FormPath",{path,"*"}),"All")
    for i = 1 to info.length do
        f = RunMacro("FormPath",{path,info[i][1]})
        if info[i][2] = "file" then do
            DeleteFile(f)
        end
        else if info[i][2] = "directory" then do
            RunMacro("ClearAndDeleteDirectory",f)
        end
    end
    RemoveDirectory(path)
EndMacro

Macro "ReadPropertiesFile" (properties_file)
    props = null
    f = OpenFile(properties_file,"r")
    while not FileAtEOF(f) do
        line = Trim(ReadLine(f))
        if Len(line) > 0 then do
            subs = ParseString(line,"=", {{"Include Empty",True}})
            key = subs[1]
            value = JoinStrings(Subarray(subs,2,subs.length-1),"=")
            props.(Trim(key)) = Trim(value)
        end
    end
    CloseFile(f)
    return(props)
EndMacro

Macro "DetokenizePropertyValues" (properties,token_map)
    for i = 1 to properties.length do
        value = token_map[i][2]
        for j = 1 to token_map.length do
            value = Substitute(value,token_map[i][1],token_map[i][2],)
        end
        token_map[i][2] = value
    end
EndMacro

Macro "ComputeAreaBufferOverlayPercentages" (area_layer_file,centroid_layer_file,centroid_query,area_taz_field,node_taz_field,buffer_size)
    //assumes node layer holds centroids from area layer, and bases its buffer around this
    //returns array of percentage arrays, each holding {centroid_taz,overlay_taz,percentage}
    omap_name = GetMap()
    olayer_name = GetLayer()
    oview_name = GetView()
    
    map = RunMacro("OpenDatabase",area_layer_file)
    RunMacro("OpenDatabaseInMap",centroid_layer_file,map)
    node_layer = GetMapLayers(map,"Point")
    node_layer = node_layer[1][1]
    area_layer = GetMapLayers(map,"Area")
    area_layer = area_layer[1][1]
    
    SetLayer(node_layer)
    centroid_selection = "centroids"
    SelectByQuery(centroid_selection,"Several",centroid_query)
    node_ids = GetSetIDs(node_layer + "|" + centroid_selection)
    node_to_taz = null
    for i = 1 to node_ids.length do
        node_id = node_ids[i]
        value = GetRecordValues(node_layer,IDToRecordHandle(node_id),{node_taz_field})
        node_to_taz.(i2s(node_id)) = value[1][2]
    end
    
    percentages = null
    temp_dir = GetFileInfo(area_layer_file)
    temp_dir = Substring(area_layer_file,1,Len(area_layer_file) - Len(temp_dir[1]))
    intersection_file = "temp_buffers.dbd"
    percentages_file = "tempintersect"
    temp_intersection_file = RunMacro("FormPath",{temp_dir,intersection_file})
    temp_percentages_file = RunMacro("FormPath",{temp_dir,percentages_file})
    EnableProgressBar("Calculating Area Buffer Percentages (buffer = " + r2s(buffer_size) + ")", 1)     // Allow only a single progress bar
    CreateProgressBar("", "True")

    nlen = node_ids.length
    //for i = 1 to nlen do
    for i = 1 to 20 do
        node_id = node_ids[i]
        node_taz = node_to_taz.(i2s(node_id))
        stat = UpdateProgressBar("Zone: " + i2s(node_taz), r2i(i/nlen*100))
        if stat = "True" then do
            percentages = null
            goto quit_loop
        end
        SetLayer(node_layer)
        SelectByQuery("centroid","Several","SELECT * WHERE id=" + i2s(node_id))
        CreateBuffers(temp_intersection_file,"buffers",{"centroid"},"Value",{buffer_size},{{"Interior","Separate"},{"Exterior","Separate"}})
    
        NewLayer = GetDBLayers(temp_intersection_file)
        intersection_layer = AddLayer(map,"inter",temp_intersection_file,NewLayer[1])
        SetLayer(area_layer)
        n = SelectByVicinity("subtaz","several",node_layer+"|centroid",buffer_size,{{"Inclusion","Intersecting"}})
        if n > 0 then do
            ComputeIntersectionPercentages({intersection_layer, area_layer + "|subtaz"}, temp_percentages_file + ".bin",)
            t = OpenTable("int_table", "FFB", {temp_percentages_file + ".bin"},)
            tbar = t + "|"
            rh = GetFirstRecord(tbar,)
            while rh <> null do
                vals = GetRecordValues(t,rh,{"Area_1", "Area_2","Percent_2"})
                if vals[1][2] = 1 and vals[2][2] <> 0 then do
                    value = GetRecordValues(area_layer,IDToRecordHandle(vals[2][2]),{area_taz_field})
                    area_taz = node_to_taz.(i2s(value[1][2]))
                    percentages = percentages + {{node_taz,area_taz,vals[3][2]}}
                end
                rh = GetNextRecord(t+"|",,)
            end
            CloseView(t)
        end
        DropLayer(map,intersection_layer)
    end
    
    quit_loop:
    DestroyProgressBar()
    CloseMap(map)
    if omap_name <> null then do
        SetMap(omap_name)
        if olayer_name <> null then do
            SetLayer(olayer_name)
        end
    end
    if oview_name <> null then do
        SetView(oview_name)
    end
    DeleteDatabase(temp_intersection_file)
    DeleteFile(temp_percentages_file + ".bin")
    DeleteFile(temp_percentages_file + ".BX")
    DeleteFile(temp_percentages_file + ".dcb")
    
    return(percentages)
EndMacro

Macro "ExportBintoCSV"(input_file_base, output_file_base)

  view = OpenTable("Binary Table","FFB",{input_file_base+".bin",}, {{"Shared", "True"}})
  SetView(view)
  ExportView(view+"|", "CSV", output_file_base+".csv",,{{"CSV Header", "True"}, {"Force Numeric Type", "double"}})
  CloseView(view)
  ok=1
  quit:
    return(ok)
EndMacro


Macro "ComputeAreaOverlayPercentages" (area_layer_file,overlay_layer_file,area_id_field,overlay_id_field)
    //returns percentage array, each element holding {area_id,overlay_id,% of overlay in area}
    omap_name = GetMap()
    olayer_name = GetLayer()
    oview_name = GetView()
    
    map = RunMacro("OpenDatabase",area_layer_file)
    area_layer = GetMapLayers(map,"Area")
    area_layer = area_layer[1][1]
    RunMacro("OpenDatabaseInMap",overlay_layer_file,map)
    overlay_layer = GetMapLayers(map,"Area")
    if overlay_layer[1][1] = area_layer then do
        overlay_layer = overlay_layer[1][2]
    end
    else do
        overlay_layer = overlay_layer[1][1]
    end
    
    area_ids = GetSetIDs(area_layer + "|")
    
    percentages = null
    temp_dir = GetFileInfo(area_layer_file)
    temp_dir = Substring(area_layer_file,1,Len(area_layer_file) - Len(temp_dir[1]))
    percentages_file = "tempintersect"
    temp_percentages_file = RunMacro("FormPath",{temp_dir,percentages_file})
    EnableProgressBar("Calculating Area Intersections", 1)     // Allow only a single progress bar
    CreateProgressBar("", "True")

    nlen = area_ids.length
    for i = 1 to nlen do
        area_id = area_ids[i]
        stat = UpdateProgressBar("Area id: " + i2s(area_id), r2i(i/nlen*100))
        if stat = "True" then do
            percentages = null
            goto quit_loop
        end
        SetLayer(area_layer)
        SelectByQuery("select","Several","SELECT * WHERE id=" + i2s(area_id))
        area_sid = GetRecordValues(area_layer,IDToRecordHandle(area_id),{area_id_field})
        area_sid = area_sid[1][2]
        SetLayer(overlay_layer)
        n = SelectByVicinity("subtaz","several",area_layer+"|select",0,{{"Inclusion","Intersecting"}})
        if n > 0 then do
            ComputeIntersectionPercentages({area_layer+"|select",overlay_layer + "|subtaz"}, temp_percentages_file + ".bin",)
            t = OpenTable("int_table", "FFB", {temp_percentages_file + ".bin"},)
            tbar = t + "|"
            rh = GetFirstRecord(tbar,)
            while rh <> null do
                vals = GetRecordValues(t,rh,{"Area_1", "Area_2","Percent_2"})
                if vals[1][2] > 0 and vals[2][2] <> 0 and vals[3][2] > 0.0 then do
                    value = GetRecordValues(overlay_layer,IDToRecordHandle(vals[2][2]),{overlay_id_field})
                    percentages = percentages + {{area_sid,value[1][2],vals[3][2]}}
                end
                rh = GetNextRecord(t+"|",,)
            end
            CloseView(t)
        end
    end
    
    quit_loop:
    DestroyProgressBar()
    CloseMap(map)
    if omap_name <> null then do
        SetMap(omap_name)
        if olayer_name <> null then do
            SetLayer(olayer_name)
        end
    end
    if oview_name <> null then do
        SetView(oview_name)
    end
    DeleteFile(temp_percentages_file + ".bin")
    DeleteFile(temp_percentages_file + ".BX")
    DeleteFile(temp_percentages_file + ".dcb")
    
    return(percentages)
EndMacro



