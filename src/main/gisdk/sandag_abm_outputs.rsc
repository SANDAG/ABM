Macro "_OpenTable" (file,view_name)
    ext = Lower(Right(file,3))
    if ext = "csv" then do
        type = "CSV"
    end
    else if ext = "bin" then do
        type = "FFB"
    end
    else do
        ShowMessage("Cannot open table of type " + ext)
        ShowMessage(2)
    end
    return(OpenTable(view_name,type,{file,}))
EndMacro

Macro "GetTodToken"
    return("%%TOD%%")
EndMacro

Macro "GetSkimToken"
    return("%%SKIM%%")
EndMacro

Macro "GetTodPeriods"
    return({"EA","AM","MD","PM","EV"})
EndMacro

Macro "GetSkimPeriods"
    return({"AM","PM","OP"})
EndMacro

Macro "GetTodSkimMapping"
    mapping = null
    mapping.EA = "OP"
    mapping.AM = "AM"
    mapping.MD = "OP"
    mapping.PM = "PM"
    mapping.EV = "OP"
    return(mapping)
EndMacro

Macro "GetHighwayModes"
    return({"SOV_GP","SOV_PAY","SR2_GP","SR2_HOV","SR2_PAY","SR3_GP","SR3_HOV","SR3_PAY","lhdn","mhdn","hhdn","lhdt","mhdt","hhdt"})
EndMacro

Macro "GetTransitModes"
    return({"LOC","LRT","EXP","CMR","BRT"})
EndMacro

Macro "GetTransitAccessModes"
    return({"WLK","PNR","KNR"})
EndMacro

Macro "ExportNetworkToCsv" (network_file,output_file_base)
    {node_layer,line_layer} = GetDBLayers(network_file)
    network_layer = AddLayerToWorkspace("network_line_layer",network_file,line_layer)
    SetLayer(network_layer)
    view = GetView()
    ExportView(view+"|","CSV",output_file_base+".csv",,{{"CSV Header", "True"},{"Force Numeric Type", "double"}})
    CloseView(view)
  
EndMacro


Macro "ExportHwyloadtoCSV"(input_file_base, output_file_base)

  tod_periods = RunMacro("GetTodPeriods")

  for t = 1 to tod_periods.length do
          tod = tod_periods[t]
          input_file= input_file_base+tod+".bin"
	  view = OpenTable("Binary Table","FFB",{input_file,}, {{"Shared", "True"}})
	  SetView(view)
	  ExportView(view+"|", "CSV", output_file_base+tod+".csv",
                       {"ID1",
			"AB_Flow_PCE",
			"BA_Flow_PCE",
			"AB_Time",
			"BA_Time",
			"AB_VOC",
			"BA_VOC",
			"AB_V_Dist_T",
			"BA_V_Dist_T",
			"AB_VHT",
			"BA_VHT",
			"AB_Speed",
			"BA_Speed",
			"AB_VDF",
			"BA_VDF",
			"AB_MSA_Flow",
			"BA_MSA_Flow",			
			"AB_MSA_Time",
			"BA_MSA_Time",
			"AB_Flow_SOV_GP",
			"BA_Flow_SOV_GP",
			"AB_Flow_SOV_PAY",
			"BA_Flow_SOV_PAY",
			"AB_Flow_SR2_GP",
			"BA_Flow_SR2_GP",
			"AB_Flow_SR2_HOV",
			"BA_Flow_SR2_HOV",
			"AB_Flow_SR2_PAY",
			"BA_Flow_SR2_PAY",
			"AB_Flow_SR3_GP",
			"BA_Flow_SR3_GP",
			"AB_Flow_SR3_HOV",
			"BA_Flow_SR3_HOV",
			"AB_Flow_SR3_PAY",
			"BA_Flow_SR3_PAY",
			"AB_Flow_lhdn",
			"BA_Flow_lhdn",
			"AB_Flow_mhdn",
			"BA_Flow_mhdn",
			"AB_Flow_hhdn",
			"BA_Flow_hhdn",
			"AB_Flow_lhdt",
			"BA_Flow_lhdt",
			"AB_Flow_mhdt",
			"BA_Flow_mhdt",
			"AB_Flow_hhdt",
			"BA_Flow_hhdt",
			"AB_Flow",
			"BA_Flow"},
                     {{"CSV Header", "True"},{"Force Numeric Type", "double"}})
	  CloseView(view)
  end
  ok=1
  quit:
    return(ok)
EndMacro





Macro "BuildTransitFlowOptions"
    //name,source_name,primary key column
    skim_token = RunMacro("GetSkimToken")
    topts = {{"ROUTE",                   "Route"                            ,True },
             {"FROM_STOP",                 "From_Stop"                        ,True },
             {"TO_STOP",                   "To_Stop"                          ,True },
             {"CENTROID",                "Centroid"                         ,False },
             {"FROMMP",                  "From_MP"                          ,False },
             {"TOMP",                    "To_MP"                            ,False },
             {"TRANSITFLOW",             "TransitFlow"                      ,False },
             {"BASEIVTT",                "BaseIVTT"                         ,False },
             {"COST",                    "Cost"                             ,False },
             {"VOC",                     "VOC"                              ,False }}
    fopts = {"flow",""}
    return({topts,fopts})
EndMacro

Macro "BuildOnOffOptions"
    //name,source_name,primary key column
    skim_token = RunMacro("GetSkimToken")
    topts = {{"ROUTE",                   "ROUTE"                            ,True },
             {"STOP",                     "STOP"                             ,True },
             {"BOARDINGS",               "On"                               ,False},
             {"ALIGHTINGS",              "Off"                              ,False},
             {"WALKACCESSON",            "WalkAccessOn"                     ,False},
             {"DIRECTTRANSFERON",        "DirectTransferOn"                 ,False},
             {"WALKTRANSFERON",          "WalkTransferOn"                   ,False},
             {"DIRECTTRANSFEROFF",       "DirectTransferOff"                ,False},
             {"WALKTRANSFEROFF",         "WalkTransferOff"                  ,False},
             {"EGRESSOFF",               "EgressOff"                        ,False}}
    fopts = {"ono",""}
    return({topts,fopts})
EndMacro


Macro "BuildAggFlowOptions"
    //name,source_name,primary key column
    skim_token = RunMacro("GetSkimToken")
    topts = {{"LINK_ID",                 "ID1"                            ,True },
             {"AB_TransitFlow",          "AB_TransitFlow"                 ,false},
             {"BA_TransitFlow",          "BA_TransitFlow"                 ,False},
             {"AB_NonTransit",           "AB_NonTransit"                  ,False},
             {"BA_NonTransit",           "BA_NonTransit"                  ,False},
             {"AB_TotalFlow",            "AB_TotalFlow"                   ,False},
             {"BA_TotalFlow",            "BA_TotalFlow"                   ,False},
             {"AB_Access_Walk_Flow",     "AB_Access_Walk_Flow"            ,False},
             {"BA_Access_Walk_Flow",     "BA_Access_Walk_Flow"            ,False},
             {"AB_Xfer_Walk_Flow",       "AB_Xfer_Walk_Flow"              ,False},
             {"BA_Xfer_Walk_Flow",       "BA_Xfer_Walk_Flow"              ,False},
             {"AB_Egress_Walk_Flow",     "AB_Egress_Walk_Flow"            ,False},
             {"BA_Egress_Walk_Flow",     "BA_Egress_Walk_Flow"            ,False}}
    fopts = {"agg",""}
    return({topts,fopts})
EndMacro



Macro "ExportTransitTablesToCsv" (results_dir,transit_options,output_file_base)
    fopts = transit_options[2]
    transit_options = transit_options[1]
    header = "MODE,ACCESSMODE,TOD"
    for i = 1 to transit_options.length do
        header = header + "," + transit_options[i][1]
    end
    
    f = OpenFile(output_file_base + ".csv","w")
    WriteLine(f,header)    
  
    table_name = Upper(Right(output_file_base,Len(output_file_base) - PositionTo(,output_file_base,"\\")))
 
    transit_modes = RunMacro("GetTransitModes")
    transit_access_modes = RunMacro("GetTransitAccessModes")
    tod_periods = RunMacro("GetTodPeriods")
    tod_skim_mapping = RunMacro("GetTodSkimMapping")
    skim_token = RunMacro("GetSkimToken")
    
    for i = 1 to tod_periods.length do
        tod = tod_periods[i]
        for t = 1 to transit_modes.length do
            transit_mode = transit_modes[t]
            for ta = 1 to transit_access_modes.length do
                transit_access_mode = transit_access_modes[ta]
                tf = RunMacro("FormPath",{results_dir,fopts[1] + transit_access_modes[ta] + "_" + transit_mode + "_" + tod + fopts[2] + ".bin"})
                if GetFileInfo(tf) <> null then do
                    view = RunMacro("_OpenTable",tf,"tview")
                    
                    rh = GetFirstRecord(view + "|",)
                    while rh <> null do
                        line = transit_mode + "," + transit_access_mode + "," + tod
                        for to = 1 to transit_options.length do
                            line = line + "," + RunMacro("ToString",view.(Substitute(transit_options[to][2],skim_token,tod_skim_mapping.(tod),)))
                        end
                        WriteLine(f,line)
                            structure = GetViewStructure(view)
                            for to = 1 to transit_options.length do
                                field_id = -1
                                field = Substitute(transit_options[to][2],skim_token,tod_skim_mapping.(tod),)
                                for s = 1 to structure.length do
                                    if structure[s][1] = field then do
                                        field_id = s
                                    end
                                end
                                if field_id < 1 then do
                                    ShowMessage("couldn't find field: " + field)
                                    ShowMessage(2)
                                end                              
     
                        end
                        rh = GetNextRecord(view + "|",rh,)
                    end
                    CloseView(view)
                end
            end
        end
    end
    
    CloseFile(f)

EndMacro

Macro "SaveMatrix" (matrix_in,file_out)
    m = OpenMatrix(matrix_in,)
    CreateTableFromMatrix(m,file_out,"CSV",{{"Complete","Yes"}})
EndMacro


