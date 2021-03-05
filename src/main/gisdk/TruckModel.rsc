/**********************************************************************************************************
Runs truck model
About: 
    Script to run the SANDAG Truck Model in TransCAD
    Study Area: County of San Diego, California
    TransCAD version 4.8 Build 545
    Author: Parsons Brinckerhoff (R. Moeckel, S. Gupta, C. Frazier)
    Developed: June to December 2008

Modifications:
    1) Added truck toll diversion model to go from three truck types
       to six truck types ({lhd/mhd/hhd} * {toll/non-toll})
       Ben Stabler, stabler@pbworld.com, 12/02/10
    
    2) Modified to integrate with SANDAG ABM
       Amar Sarvepalli, sarvepalli@pbworld.com, 09/06/12

Steps: 
    1) Generates standard truck trip and special (military) truck trips
    2) Gets regional truck trips, IE trips, EI trips and EE trips and balances truck trips
    3) Distributes truck trips with congested skims and splits by time of day
    4) Applies truck toll diversion model with free-flow toll and non-toll skims
    
    Note: truck trip generation and free-flow skims are run only for the first iteration

**********************************************************************************************************/
Macro "truck model"(properties, iteration)
    shared path, inputDir, outputDir, inputTruckDir
    
   // read properties from sandag_abm.properties in /conf folder
   properties = "\\conf\\sandag_abm.properties"   
   startFromIteration = s2i(RunMacro("read properties",properties,"RunModel.startFromIteration", "S"))
    
    // Generate trips and free-flow truck skims for the first iteration
    if (iteration = startFromIteration) then do  
       // Generate daily truck trips
       RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","truck-tripgen"})
       ok = RunMacro("truck-tripgen",properties)
       if !ok then goto quit
    end

    // Build lhdn congested skims
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(lhdn)"})
    ok = RunMacro("hwy skim",{"lhdn"})
    if !ok then goto quit
      
    // Build mhdn congested skims
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(mhdn)"})
    ok = RunMacro("hwy skim",{"mhdn"}) 
    if !ok then goto quit
      
    // Build hhdn congested skims
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(hhdn)"})  
    ok = RunMacro("hwy skim",{"hhdn"}) 
    if !ok then goto quit
      
    // Build lhdt congested skims 
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(lhdt)"})  
    ok = RunMacro("hwy skim",{"lhdt"}) 
    if !ok then goto quit
      
    // Build mhdt congested skims
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(mhdt)"})  
    ok = RunMacro("hwy skim",{"mhdt"}) 
    if !ok then goto quit      
      
    // Build hhdt congested skims 
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","hwy skim,(hhdt)"})     
    ok = RunMacro("hwy skim",{"hhdt"}) 
    if !ok then goto quit
  
    // Distribute daily truck trips and split them by time period
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","trkDistribution,(properties)"})      
    ok = RunMacro("trkDistribution",properties)  
    if !ok then goto quit
        
    // Apply toll-diversion model
    RunMacro("HwycadLog",{"TruckModel.rsc: truckmodel","trk toll diversion model"})
    ok = RunMacro("trk toll diversion model")
    if !ok then goto quit  
       
    run_ok = 1
    RunMacro("close all")
    Return(run_ok)
    
    quit:
       RunMacro("close all")
       Return(ok) 
EndMacro



/**********************************************************************************************************
Generates daily truck trips (standard and special generations)
 
Inputs:
   sandag.properties
   
Outputs:
   output\gmTruckDataBalanced.bin
   output\regionalEEtrips.csv    
    
**********************************************************************************************************/
Macro "truck-tripgen"(properties)
    shared path, inputDir, outputDir, mxzone
    dim arrInterimYear[3]
    
    strFyear = RunMacro("read properties",properties,"truck.FFyear","S")
    intFyear = StringToInt(strFyear)
    arrInterimYear=RunMacro("InterimYearCheck",properties,intFyear)
    
    RunMacro("trkStdTripGen",strFyear,intFyear,arrInterimYear,properties)
    RunMacro("trkSpecialGen",strFyear,intFyear,arrInterimYear)
    RunMacro("trkBalance",strFyear,intFyear,arrInterimYear)
    run_ok=1
    
    exit:
       RunMacro("close all")
       Return(run_ok)
EndMacro




/**********************************************************************************************************
Produces standard truck trips 
 
Inputs:
   input\mgra13_based_input2010.csv
   input\TruckTripRates.csv
   output\hhByTaz.csv.csv
   output\empByTaz.csv.csv
   
   (optional, used for interpolation)
   input\hhByTaz<prev_year>.csv
   input\hhByTaz<next_year>.csv
   input\empByTaz<prev_year>.csv
   input\empByTaz<next_year>.csv
   
   (optional, used only for landuse override)
   input\lu.csv 
   output\EmpDistLUovrAgg.csv
   output\hhluagg.csv
   
Outputs:
   output\gmTruckDataII.csv   

**********************************************************************************************************/
Macro "trkStdTripGen" (strFyear,intFyear,arrInterimYear,properties)
    shared path, inputDir, outputDir, mxzone
    booInterimYear = arrInterimYear[1]
    
    // Creates household and employment data by taz from mgra data  
    RunMacro("Create hh and emp by taz")
    
    //--------------------------------------------------
    //This section checks available data and interpolates if doesn't exist
    //--------------------------------------------------
    // Do interpolate = True
    if booInterimYear = 2 then do 
       prevYear = arrInterimYear[2]
       nextYear = arrInterimYear[3]
    end
    
    // Override landuse data if option is "True"
    check_luOverride = RunMacro("read properties",properties,"truck.luOverRide", "S")
    if check_luOverride = "True" then do
       RunMacro("EmploymentLUOverride")
       RunMacro("EmploymentDistLUOverride")
       RunMacro("HouseholdLUOverride") 
    end
    
  
    // If data is not available then interpolate from closest available years
    if booInterimYear = 2 then do 
       // Copy prev and next year data files to output directory
       ok=RunMacro("SDcopyfile",{inputDir+"\\hhByTaz"+I2S(prevYear)+".csv",outputDir+"\\hhByTaz_prev.csv"})
       ok=RunMacro("SDcopyfile",{inputDir+"\\hhByTaz"+I2S(nextYear)+".csv",outputDir+"\\hhByTaz_next.csv"}) 
       ok=RunMacro("SDcopyfile",{inputDir+"\\empByTaz"+I2S(prevYear)+".csv",outputDir+"\\empByTaz_prev.csv"})
       ok=RunMacro("SDcopyfile",{inputDir+"\\empByTaz"+I2S(nextYear)+".csv",outputDir+"\\empByTaz_next.csv"}) 
       
       // Interpolate data from prev and next years
       ok=RunMacro("Interpolate",{"hhByTaz_prev.csv","hhByTaz_next.csv","hhByTaz.csv",intFyear,prevYear,nextYear}) 
       ok=RunMacro("Interpolate",{"empByTaz_prev.csv","empByTaz_next.csv","empByTaz.csv",intFyear,prevYear,nextYear}) 

       // Delete prev and next year data files
       ok=RunMacro("SDdeletefile",{outputDir+"\\hhByTaz_prev.csv"}) 
       ok=RunMacro("SDdeletefile",{outputDir+"\\hhByTaz_next.csv"})                    
       ok=RunMacro("SDdeletefile",{outputDir+"\\empByTaz_prev.csv"})   
       ok=RunMacro("SDdeletefile",{outputDir+"\\empByTaz_next.csv"})
    end

    // join data and parameter tables
    empView = Opentable("Employment", "CSV", {outputDir+"\\empByTaz.csv"})
    hhView  = Opentable("HouseHolds", "CSV", {outputDir+"\\hhByTaz.csv"})


    //--------------------------------------------------
    // This section overrides LU for employment and households
    //--------------------------------------------------
    if check_luOverride = "True" then do 
      
       //LU override file for Employment
       empAggDistLUovr_filecsv = outputDir+"\\EmpDistLUovrAgg.csv"
       di = GetDirectoryInfo(empAggDistLUovr_filecsv, "File")
    
       // Export employment by TAZ to binary file and get zones
       ExportView("Employment|", "FFB", outputDir+"\\empByTaz.BIN", null, )
       vwEmpBin = Opentable("EmploymentByZoneBin", "FFB", {outputDir+"\\empByTaz.BIN"})
       vwEmpDistLUovrAgg = Opentable("EmploymentDistributionOverride", "CSV", {empAggDistLUovr_filecsv})
       vectZonerecords = GetRecordCount(vwEmpDistLUovrAgg, null)  
       vectZone = GetDataVectors(vwEmpDistLUovrAgg+"|",{"zone"},)
       
       // Convert real to string zones
       for i = 1 to vectZonerecords do
          strZone = RealToString(vectZone[1][i])
          rh = LocateRecord("EmploymentByZoneBin|","TAZ",{strZone},{{"Exact", "True"}})
          x = GetView()
          DeleteRecord("EmploymentByZoneBin", rh)
       end
       
       // Get data from all fields in EmpDistLUovrAgg file  
       fldsEmpDistLUovrAgg = GetFields(vwEmpDistLUovrAgg, "All")
       vectEMPovr = GetDataVectors(vwEmpDistLUovrAgg+"|",fldsEmpDistLUovrAgg[1],)
       
       // Write fields to employment by TAZ 
       for i = 1 to vectZonerecords do
          rh = AddRecord(vwEmpBin, {
             {"TAZ", R2I(vectEMPovr[1][i])},
             {"First TAZ", R2I(vectEMPovr[2][i])},
             {fldsEmpDistLUovrAgg[1][3], vectEMPovr[3][i]},
             {fldsEmpDistLUovrAgg[1][4], vectEMPovr[4][i]},
             {fldsEmpDistLUovrAgg[1][5], vectEMPovr[5][i]},
             {fldsEmpDistLUovrAgg[1][6], vectEMPovr[6][i]},
             {fldsEmpDistLUovrAgg[1][7], vectEMPovr[7][i]},
             {fldsEmpDistLUovrAgg[1][8], vectEMPovr[8][i]},
             {fldsEmpDistLUovrAgg[1][9], vectEMPovr[9][i]},
             {fldsEmpDistLUovrAgg[1][10], vectEMPovr[10][i]},
             {fldsEmpDistLUovrAgg[1][11], vectEMPovr[11][i]}
            })
       end
       
       // LU override file for Households
       HHLUagg_filecsv = outputDir+"\\hhluagg.csv"
       di = GetDirectoryInfo(HHLUagg_filecsv, "File")
    
       // Export households by TAZ to binary file and get zones
       ExportView(hhView+"|", "FFB", outputDir+"\\hhByTaz.BIN", null, )
       vwHHBin = Opentable("HouseholdsByZoneBin", "FFB", {outputDir+"\\HHByTaz.BIN"})
       vwHHLUagg = Opentable("HouseholdsOverride", "CSV", {HHLUagg_filecsv})
       vectHHZoneRecords = GetRecordCount(vwHHLUagg, null)  
       vectHHZone = GetDataVectors(vwHHLUagg+"|",{"TAZ"},)
       
       // Convert real to string zones
       for i = 1 to vectHHZoneRecords do
          strZone = RealToString(vectHHZone[1][i])
          rh = LocateRecord("HouseholdsByZoneBin|","TAZ",{strZone},{{"Exact", "True"}})
          DeleteRecord("HouseholdsByZoneBin", rh)
       end
       
       // Get data from all fields in hhluagg file 
       fldsHHLUAgg = GetFields(vwHHLUagg, "All")
       vectHHovr = GetDataVectors(vwHHLUagg+"|",fldsHHLUAgg[1],)
       
       // Write fields to households by TAZ 
       for i = 1 to vectHHZoneRecords do
          rh = AddRecord(vwHHBin, {
            {"TAZ", R2I(vectHHovr[1][i])},
            {"First TAZ", R2I(vectHHovr[2][i])},
            {fldsHHLUAgg[1][3], vectHHovr[3][i]}
           })
       end
    end
   
   
    //--------------------------------------------------
    // This section applies truck trip production and attraction rates by truck type 
    //--------------------------------------------------
    // Open truck data file
    viewtripRates = Opentable("TruckTripRates", "CSV", {inputDir+"\\TruckTripRates.csv"})    
    
    // Join all data
    jv1 = Joinviews("JV1", hhView+".ZONE", empView+".ZONE",)
    jv9 = Joinviews("JV9", jv1+"."+hhView+".TruckRegionType", viewtripRates+".RegionType",)
    Setview(jv9)

    // Compute lhd truck productions "AGREMPN","CONEMPN","RETEMPN","GOVEMPN","MANEMPN","UTLEMPN","WHSEMPN","OTHEMPN"}
    lhd_p = CreateExpression(jv9, "LHD_ProductionsTemp", "Nz(emp_agmin + emp_cons) * [TG_L_Ag/Min/Constr] + Nz(emp_retrade) * TG_L_Retail + Nz(emp_gov) * TG_L_Government + Nz(emp_mfg) * TG_L_Manufacturing + Nz(emp_twu) * [TG_L_Transp/Utilities]", )
    lhd_p = CreateExpression(jv9, "LHD_Productions", "Nz(LHD_ProductionsTemp) + Nz(emp_whtrade) * TG_L_Wholesale + Nz(emp_other) * TG_L_Other + Nz(HH) * TG_L_Households", )
    
    // Compute lhd truck attractions    
    lhd_a = CreateExpression(jv9, "LHD_AttractionsTemp", "(emp_agmin + emp_cons) * [TA_L_Ag/Min/Constr] + emp_retrade * TA_L_Retail + emp_gov * TA_L_Government + emp_mfg * TA_L_Manufacturing + emp_twu * [TA_L_Transp/Utilities] + emp_whtrade * TA_L_Wholesale", )
    lhd_a = CreateExpression(jv9, "LHD_Attractions", "Nz(LHD_AttractionsTemp) + Nz(emp_other) * TA_L_Other + Nz(HH) * TA_L_Households", )

    // Compute mhd truck productions     
    mhd_p = CreateExpression(jv9, "MHD_ProductionsTemp", "(emp_agmin + emp_cons) * [TG_M_Ag/Min/Constr] + emp_retrade * TG_M_Retail + emp_gov * TG_M_Government + emp_mfg * TG_M_Manufacturing + emp_twu * [TG_M_Transp/Utilities] + emp_whtrade * TG_M_Wholesale ", )
    mhd_p = CreateExpression(jv9, "MHD_Productions", "Nz(MHD_ProductionsTemp) + Nz(emp_other) * TG_M_Other + Nz(HH) * TG_M_Households", )

    // Compute mhd truck attractions 
    mhd_a = CreateExpression(jv9, "MHD_AttractionsTemp", "(emp_agmin + emp_cons) * [TA_M_Ag/Min/Constr] + emp_retrade * TA_M_Retail + emp_gov * TA_M_Government + emp_mfg * TA_M_Manufacturing + emp_twu * [TA_M_Transp/Utilities] + emp_whtrade * TA_M_Wholesale", )
    mhd_a = CreateExpression(jv9, "MHD_Attractions", "Nz(MHD_AttractionsTemp) + Nz(emp_other) * TA_M_Other + Nz(HH) * TA_M_Households", )

    // Compute hhd truck productions
    hhd_p = CreateExpression(jv9, "HHD_ProductionsTemp", "(emp_agmin + emp_cons) * [TG_H_Ag/Min/Constr] + emp_retrade * TG_H_Retail + emp_gov * TG_H_Government + emp_mfg * TG_H_Manufacturing + emp_twu * [TG_H_Transp/Utilities] + emp_whtrade * TG_H_Wholesale ", )
    hhd_p = CreateExpression(jv9, "HHD_Productions", "Nz(HHD_ProductionsTemp) + Nz(emp_other) * TG_H_Other + Nz(HH) * TG_H_Households", )

    // Compute hhd truck attractions     
    hhd_a = CreateExpression(jv9, "HHD_AttractionsTemp", "(emp_agmin + emp_cons) * [TA_H_Ag/Min/Constr] + emp_retrade * TA_H_Retail + emp_gov * TA_H_Government + emp_mfg * TA_H_Manufacturing + emp_twu * [TA_H_Transp/Utilities] + emp_whtrade * TA_H_Wholesale", )  
    hhd_a = CreateExpression(jv9, "HHD_Attractions", "Nz(HHD_AttractionsTemp) + Nz(emp_other) * TA_H_Other + Nz(HH) * TA_H_Households", )
    
    // Export the productions and attractions to csv file
    RunMacro("HwycadLog",{"TruckModel.rsc: trkStdGen","ExportView P&A"})
    ExportView(jv9+"|", "CSV", outputDir+"\\gmTruckDataII.csv", {hhView+".ZONE", "LHD_Productions", "LHD_Attractions", "MHD_Productions", "MHD_Attractions", "HHD_Productions", "HHD_Attractions"}, {{"CSV Header"}})

 
    // Close all and delete temp files
    RunMacro("close all")
    ok=RunMacro("SDdeletefile",{outputDir+"\\hhdata.csv"}) 
    if!ok then goto exit
    ok=RunMacro("SDdeletefile",{outputDir+"\\emp.csv"}) 
    if!ok then goto exit
    
    run_ok=1
    exit:
      RunMacro("close all")
      Return(run_ok)
EndMacro



/**********************************************************************************************************
Creates households by taz and employment by taz files to use in the truck trip generation model
 
Inputs:
   sandag.properties
   input\mgra13_based_input2012.csv

Outputs:
   output\empByTaz.csv
   output\hhByTaz.csv  
    
**********************************************************************************************************/
Macro "Create hh and emp by taz"
    shared path, inputDir, outputDir, mxzone  , scenarioYear    
    mgraDataFile      = "mgra13_based_input"+scenarioYear+".csv"
    empbytaz          = "empByTaz.csv"
    hhbytaz           = "hhByTaz.csv"
    
    RunMacro("SDcopyfile",{inputDir+"\\"+mgraDataFile,outputDir+"\\"+mgraDataFile}) 
    mgraView = OpenTable("MGRA View", "CSV", {outputDir+"\\"+mgraDataFile}, {{"Shared", "True"}})
    
    // Get data fields into vectors    
    mgra                            = GetDataVector(mgraView+"|", "mgra", {{"Sort Order", {{"mgra", "Ascending"}}}} )
    taz                             = GetDataVector(mgraView+"|", "TAZ", {{"Sort Order", {{"mgra", "Ascending"}}}} )
    hh                              = GetDataVector(mgraView+"|", "hh", {{"Sort Order", {{"mgra", "Ascending"}}}} )
    emp_ag                          = GetDataVector(mgraView+"|", "emp_ag", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_const_non_bldg_prod         = GetDataVector(mgraView+"|", "emp_const_non_bldg_prod", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_const_non_bldg_office       = GetDataVector(mgraView+"|", "emp_const_non_bldg_office", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_utilities_prod              = GetDataVector(mgraView+"|", "emp_utilities_prod", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_utilities_office            = GetDataVector(mgraView+"|", "emp_utilities_office", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_const_bldg_prod             = GetDataVector(mgraView+"|", "emp_const_bldg_prod", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_const_bldg_office           = GetDataVector(mgraView+"|", "emp_const_bldg_office", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_mfg_prod                    = GetDataVector(mgraView+"|", "emp_mfg_prod", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_mfg_office                  = GetDataVector(mgraView+"|", "emp_mfg_office", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_whsle_whs                   = GetDataVector(mgraView+"|", "emp_whsle_whs", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_trans                       = GetDataVector(mgraView+"|", "emp_trans", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_retail                      = GetDataVector(mgraView+"|", "emp_retail", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_prof_bus_svcs               = GetDataVector(mgraView+"|", "emp_prof_bus_svcs", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_prof_bus_svcs_bldg_maint    = GetDataVector(mgraView+"|", "emp_prof_bus_svcs_bldg_maint", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_pvt_ed_k12                  = GetDataVector(mgraView+"|", "emp_pvt_ed_k12", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_pvt_ed_post_k12_oth         = GetDataVector(mgraView+"|", "emp_pvt_ed_post_k12_oth", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_health                      = GetDataVector(mgraView+"|", "emp_health", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_personal_svcs_office        = GetDataVector(mgraView+"|", "emp_personal_svcs_office", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_amusement                   = GetDataVector(mgraView+"|", "emp_amusement", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_hotel                       = GetDataVector(mgraView+"|", "emp_hotel", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_restaurant_bar              = GetDataVector(mgraView+"|", "emp_restaurant_bar", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_personal_svcs_retail        = GetDataVector(mgraView+"|", "emp_personal_svcs_retail", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_religious                   = GetDataVector(mgraView+"|", "emp_religious", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_pvt_hh                      = GetDataVector(mgraView+"|", "emp_pvt_hh", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_state_local_gov_ent         = GetDataVector(mgraView+"|", "emp_state_local_gov_ent", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_fed_non_mil                 = GetDataVector(mgraView+"|", "emp_fed_non_mil", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_fed_mil                     = GetDataVector(mgraView+"|", "emp_fed_mil", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_state_local_gov_blue        = GetDataVector(mgraView+"|", "emp_state_local_gov_blue", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_state_local_gov_white       = GetDataVector(mgraView+"|", "emp_state_local_gov_white", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
    emp_total                       = GetDataVector(mgraView+"|", "emp_total", {{"Sort Order", {{"mgra", "Ascending"}}}} )  
 
 
    // Combine employment fields that match to the truck trip rate classification
    TOTEMP      = emp_total                                                                                                         // Total employment
    emp_agmin   = emp_ag                                                                                                            // Agriculture + Mining 
    emp_cons    = emp_const_bldg_prod + emp_const_bldg_office                                                                       // Construction
    emp_retrade = emp_retail + emp_personal_svcs_retail                                                                             // Retail
    emp_gov     = emp_state_local_gov_ent + emp_state_local_gov_blue + emp_state_local_gov_white + emp_fed_non_mil + emp_fed_mil    // Government 
    emp_mfg     = emp_mfg_prod + emp_mfg_office                                                                                     // Manufacturing
    emp_twu     = emp_trans + emp_utilities_office + emp_utilities_prod                                                             // Transportation + Utilities
    emp_whtrade = emp_whsle_whs                                                                                                     // Wholesale
    emp_other   = TOTEMP - (emp_agmin +emp_cons+ emp_retrade + emp_gov + emp_mfg + emp_twu + emp_whtrade)                           // Other

    
    // Add fields employment by taz
    strct = GetTableStructure(mgraView)
    for i = 1 to strct.length do
       strct[i] = strct[i] + {strct[i][1]}
    end
    strct = strct + {{"emp_agmin"   , "Float", 8, 4, "True", , , , , , , null}}  
    strct = strct + {{"emp_cons"    , "Float", 8, 4, "True", , , , , , , null}}   
    strct = strct + {{"emp_retrade" , "Float", 8, 0, "True", , , , , , , null}}
    strct = strct + {{"emp_gov"     , "Float", 8, 0, "True", , , , , , , null}}
    strct = strct + {{"emp_mfg"     , "Float", 8, 0, "True", , , , , , , null}}
    strct = strct + {{"emp_twu"     , "Float", 8, 0, "True", , , , , , , null}}
    strct = strct + {{"emp_whtrade" , "Float", 8, 0, "True", , , , , , , null}}
    strct = strct + {{"emp_other"   , "Float", 8, 4, "True", , , , , , , null}}
    ModifyTable(mgraView, strct)

    ExportView(mgraView+"|","FFB",outputDir+"\\temp.bin",{"MGRA","TAZ","HH","TruckRegionType","emp_agmin","emp_cons","emp_retrade","emp_gov","emp_mfg","emp_twu","emp_whtrade","emp_other"},)
    MgraTazView = OpenTable("MGRA View", "FFB", {outputDir+"\\temp.bin"},)
    
    // Set data into new fields
    SetDataVectors(MgraTazView+"|",{{"emp_agmin"  ,emp_agmin  },
                                    {"emp_cons"   ,emp_cons   },
                                    {"emp_retrade",emp_retrade},
                                    {"emp_gov"    ,emp_gov    },
                                    {"emp_mfg"    ,emp_mfg    },                             
                                    {"emp_twu"    ,emp_twu    },
                                    {"emp_whtrade",emp_whtrade},                              
                                    {"emp_other"  ,emp_other  }},)

    // Aggregate employment fields by taz
    emp = AggregateTable("employment", MgraTazView+"|","FFB", outputDir+"\\emp_temp.bin","TAZ", {
    	                         {"TruckRegionType","avg","TruckRegionType"},
                               {"emp_agmin"      ,"sum","emp_agmin"      },
                               {"emp_cons"       ,"sum","emp_cons"       },
                               {"emp_retrade"    ,"sum","emp_retrade"    },
                               {"emp_gov"        ,"sum","emp_gov"        },
                               {"emp_mfg"        ,"sum","emp_mfg"        },                             
                               {"emp_twu"        ,"sum","emp_twu"        },
                               {"emp_whtrade"    ,"sum","emp_whtrade"    },                              
                               {"emp_other"      ,"sum","emp_other"      }
                              },null)
    
    emp_view = OpenTable("emp_view", "FFB", {outputDir+"\\emp_temp.bin"},)
    RenameField(emp_view+".Avg TruckRegionType", "TruckRegionType") 
        
     // Create a temp file with all zones (internal + external zones) from the highway network file
    db_file = outputDir+"\\hwy.dbd"
    {node_lyr,} = RunMacro("TCB Add DB Layers", db_file,,)
    SetLayer(node_lyr)
    n= SelectByQuery("Zones", "Several", "Select * where ID <= "+ String(mxzone),)
    zones = GetDataVector(node_lyr+"|Zones", "ID",{{"Sort Order", {{"ID", "Ascending"}}}})
    
    // Create a temp file with all zones
    all_vw = CreateTable("allzones", outputDir+"\\temp_zones.bin", "FFB",{
                        {"ZONE", "Integer", 8, null, "Yes"}})
    SetView(all_vw)
    for i = 1 to zones.length do
      rh = AddRecord(all_vw, {{"ZONE", zones[i]}})
    end
    
    join_vw = JoinViews("joined_view1", all_vw+".ZONE",emp_view+".TAZ",)
    ExportView(join_vw+"|","CSV",outputDir+"\\"+empbytaz,,)
    CloseView(join_vw)
                             
    
    // Aggregate households by taz
    hh = AggregateTable("households", MgraTazView+"|","FFB", outputDir+"\\hh_temp.bin","TAZ", {
    													{"TruckRegionType","avg","TruckRegionType"},
                              {"HH" ,"sum","HH"}
                             },null)
    
    hh_view = OpenTable("hh_temp", "FFB", {outputDir+"\\hh_temp.bin"},)
    RenameField(hh_view+".Avg TruckRegionType", "TruckRegionType") 
    join_vw = JoinViews("joined_view1", all_vw+".ZONE",hh_view+".TAZ",)
    ExportView(join_vw+"|","CSV",outputDir+"\\"+hhbytaz,,)

    RunMacro("close all")
    
    DeleteFile(outputDir+"\\temp_zones.bin")
    DeleteFile(outputDir+"\\emp_temp.bin")
    DeleteFile(outputDir+"\\hh_temp.bin")
EndMacro



/**********************************************************************************************************
Creates household override file from a land use file LU.CSV                                                                                       
                                                                                                           
Inputs:                                                                                                    
   input\lu.csv
                                                                                 
Outputs:                                                                                                   
   output\hhlu.csv                                                                                         
   output\hhluagg.csv                                                                                      
                                                                                                           
**********************************************************************************************************/
Macro "HouseholdLUOverride"
    // Creates Household Override file from a Land Use based LU.csv
    shared path, inputDir, outputDir
    RunMacro("TCB Init")
      
    // Copy LU.csv, Open Copy, and Rename Fields - no header line in lu.csv
    ok=RunMacro("SDcopyfile",{inputDir+"\\lu.csv",outputDir+"\\hhlu.csv"}) if!ok then goto quit
    vwHHLUovr = Opentable("HHLUOverride", "CSV",{outputDir+"\\hhlu.csv"})
    RenameField(vwHHLUovr+".FIELD_1", "TAZ")
    RenameField(vwHHLUovr+".FIELD_2", "RateType")
    RenameField(vwHHLUovr+".FIELD_3", "LUCode")
    RenameField(vwHHLUovr+".FIELD_4", "HH")
    Setview(vwHHLUovr)
    
    // Select HH's From LU.csv
    // Ratetype=DU=1
    qry1 = "Select * where RateType = 1"
    DUqry = SelectByQuery("HHSelection", "Several", qry1,)
    
    // Aggregate HH's to TAZ Level (might have SF & MF as separate codes)
    RunMacro("HwycadLog",{"TruckModel.rsc: HouseholdLUOverride","AggregateTable"})
    aggtable = AggregateTable("AggHHLUOvr","HHLUOverride|HHSelection", "CSV", outputDir+"\\hhluagg.csv", "TAZ", {
       {"TAZ","dominant"},
       {"HH","sum", }
       }, {"Missing as zero"})
       
    done:
      RunMacro("close all") 
      Return( RunMacro("TCB Closing", 1, "FALSE" ) )
    
    quit:
      RunMacro("close all") 
      Return( RunMacro("TCB Closing", 0, "FALSE" ) )
EndMacro


/**********************************************************************************************************
Creates Employment Override File from a land use file LU.CSV

Inputs:
   input\lu.csv
   input\emplbylu.csv
   input\Zone_sphere.csv
   input\emp_lu_ksf.csv
   input\emp_lu_rm.csv
   input\emp_lu_site.csv

Outputs:
   output\EmpLUovr.bin"
   output\EmpLUovr.csv"
**********************************************************************************************************/
Macro "EmploymentLUOverride"
    shared path,inputDir, outputDir
  
    // Remove dcc & dcb files if already exist
    ok=RunMacro("SDdeletefile",{inputDir+"\\emplbylu.dcc"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{inputDir+"\\Zone_sphere.dcc"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{inputDir+"\\emp_lu_ksf.dcc"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{inputDir+"\\emp_lu_site.dcc"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{inputDir+"\\lu.dcc"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{outputDir+"\\EmpLUovr.dcb"}) 
    if!ok then goto quit
    ok=RunMacro("SDdeletefile",{outputDir+"\\EmpLUovr.dcc"}) 
    if!ok then goto quit    
      
    // Open data files and LU.CSV 
    vwEmpbyLU    = Opentable("EmploymentByLU", "CSV", {inputDir+"\\emplbylu.csv"})
    vwZoneSphere = Opentable("XREF_ZoneSphere","CSV", {inputDir+"\\Zone_sphere.csv"})
    vwAcreToKSF  = Opentable("Conv_Acre_KSF",  "CSV", {inputDir+"\\emp_lu_ksf.csv"})
    vwAcreToRM   = Opentable("Conv_Acre_RM",   "CSV", {inputDir+"\\emp_lu_rm.csv"})
    vwEmpSite    = Opentable("SiteEmploymentbySphere", "CSV", {inputDir+"\\emp_lu_site.csv"})
    vwLUovr      = Opentable("LUOverride", "CSV", {inputDir+"\\lu.csv"})
    
    // Rename fields for LU.CSV - no header line in file
    RenameField(vwLUovr+".FIELD_1", "TAZ")
    RenameField(vwLUovr+".FIELD_2", "RateType")
    RenameField(vwLUovr+".FIELD_3", "LUCode")
    RenameField(vwLUovr+".FIELD_4", "Amt")
    
    // Join data files to LU.CSV
    Setview(vwLUovr)
    jvwLUovrSphere = Joinviews("jvwLUovrSphere", vwLUovr+".TAZ", vwZoneSphere+".Zone",)  
    jvwLUovrSphereEmp = Joinviews("jvwLUovrSphereEmp", jvwLUovrSphere+".LUCode", vwEmpbyLU+".LU",)
    jvwLUovrSphereEmpKSF = Joinviews("jvwLUovrSphereEmpKSF", jvwLUovrSphereEmp+".LUCode", vwAcreToKSF+".LU",)
    jvwLUovrSphereEmpKSFSite = Joinviews("jvwLUovrSphereEmpKSFSite", jvwLUovrSphereEmpKSF+".LUCode", vwEmpSite+".LU",)
    jvwLUovrSphereEmpKSFSiteRM = Joinviews("jvwLUovrSphereEmpKSFSiteRM", jvwLUovrSphereEmpKSFSite+".LUCode", vwAcreToRM+".LU",)
      
    // Set Output Files
    empLUovr_file = outputDir+"\\EmpLUovr.BIN"
    empLUovr_filecsv = outputDir+"\\EmpLUovr.csv"
    
    // Export Joined View and Add Employment Fields
    Setview(jvwLUovrSphereEmpKSFSiteRM)   
    ExportView(jvwLUovrSphereEmpKSFSiteRM+"|", "FFB", empLUovr_file, null, {
      {"Additional Fields",{ {"empbyacres", "Real", 16, 4, },{"empdirect", "Real", 16, 4, },{"empbysite", "Real", 16, 4, },{"empbyksf", "Real", 16, 4, },{"empbyrm", "Real", 16, 4, },{"emp", "Real", 16, 4, }} },
      } ) 
    
    // Apply Acre Based Employment Rates  
    // RateType=2=Acre
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbyacres"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere=1404 and RateType=2) then nz(sph1404acre*Amt) else if (Sphere=1441 and RateType=2) then nz(sph1441acre*Amt) else if (Sphere>=1900 and RateType=2) then nz(sph1900acre*Amt) else if RateType=2 then nz(sphOtheracre*Amt)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Add Directly Entered Employment
    // RateType=3=Employee
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empdirect"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if RateType=3 then nz(Amt)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Apply Site Based Employment Rates
    // RateType=4=Site
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbysite"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere=1404 and RateType=4) then nz(sph1404site*Amt) else if (Sphere=1441 and RateType=4) then nz(sph1441site*Amt) else if (Sphere>=1900 and RateType=4) then nz(sph1900site*Amt) else if RateType=4 then nz(sphOthersite*Amt)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Convert KSF to Acres and Apply Acre Based Employment Rates
    // RateType=6=KSF
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbyksf"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere=1404 and RateType=6) then nz(sph1404acre*Amt*ksf2acre) else if (Sphere=1441 and RateType=6) then nz(sph1441acre*Amt*ksf2acre)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbyksf"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere>=1900 and RateType=6) then nz(sph1900acre*Amt*ksf2acre) else if RateType=6 then nz(sphOtheracre*Amt*ksf2acre)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Convert KSF to Acres and Apply Acre Based Employment Rates
    // RateType=7=Hotel Room
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbyRM"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere=1404 and RateType=7) then nz(sph1404acre*Amt*rm2acre) else if (Sphere=1441 and RateType=7) then nz(sph1441acre*Amt*rm2acre)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"empbyRM"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if (Sphere>=1900 and RateType=7) then nz(sph1900acre*Amt*rm2acre) else if RateType=7 then nz(sphOtheracre*Amt*rm2acre)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Combine Acre, KSF, and Site Employment into EMP Field
    Opts = null
    Opts.Input.[Dataview Set] = {empLUovr_file, "jvwLUovrSphereEmpKSFSiteRM"}
    Opts.Global.Fields = {"emp"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"nz(empbyacres)+nz(empdirect)+nz(empbysite)+nz(empbyksf)+nz(empbyrm)"}
    ret_value = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    if !ret_value then goto quit
    
    // Export Employment Override File
    RunMacro("HwycadLog",{"TruckModel.rsc: EmploymentLUOverride","ExportView"})
    CloseView(jvwLUovrSphereEmpKSFSiteRM)
    jvwLUovrSphereEmpKSFSiteRM=opentable("jvwLUovrSphereEmpKSFSiteRM", "FFB", {empLUovr_file,})
    Setview(jvwLUovrSphereEmpKSFSiteRM)   
    ExportView(jvwLUovrSphereEmpKSFSiteRM+"|", "CSV", empLUovr_filecsv, 
      {"TAZ","RateType","LUCode","Amt","emp"}, 
      {
      {"CSV Header", "False"},
      {"CSV Drop Quotes", "True"}
      } )
    
    done:
      RunMacro("close all") 
      Return( RunMacro("TCB Closing", 1, "FALSE" ) )
    
    quit:
      RunMacro("close all") 
      Return( RunMacro("TCB Closing", 0, "FALSE" ) )
EndMacro


/**********************************************************************************************************
Takes employment override file and creates employment distribution to employment categories

Inputs:
   output\EmpLUovr.csv"
   input\empldistbylu.csv

Outputs:
   output\EmpDistLUovrAgg.csv
   output\EmpDistLUovr.csv"
   
**********************************************************************************************************/
Macro "EmploymentDistLUOverride"
    shared path,inputDir, outputDir
    RunMacro("TCB Init")

    // Open Employment Override and Employment Distribution Files
    empLUovr_filecsv = outputDir+"\\EmpLUovr.csv"
    vwEmpLUovr=opentable("EmploymentLUoverride", "CSV", {empLUovr_filecsv,})
    vwEmpDistbyLU = Opentable("EmploymentDistribution", "CSV", {inputDir+"\\empldistbylu.csv"})
    
    // Join Files
    Setview(vwEmpLUovr)
    jvwEmpLUovrDist = Joinviews("EmploymentOverrideWithDistribution", vwEmpLUovr+".LUCode", vwEmpDistbyLU+".plu",)  
    
    // Set Output Files
    empAggDistLUovr_filecsv = outputDir+"\\EmpDistLUovrAgg.csv"
    empDistLUovr_filecsv = outputDir+"\\EmpDistLUovr.csv"

    // Calculate Employment Distribution
    expZone = CreateExpression(jvwEmpLUovrDist, "zone", "TAZ", )
    expEmp_mil = CreateExpression(jvwEmpLUovrDist, "emp_mil", "Nz(emp*mil)", )
    expEmp_agmin = CreateExpression(jvwEmpLUovrDist, "emp_agmin", "Nz(emp*ag)", )
    expEmp_cons = CreateExpression(jvwEmpLUovrDist, "emp_cons", "Nz(emp*con)", )
    expEmp_mfg = CreateExpression(jvwEmpLUovrDist, "emp_mfg", "Nz(emp*mfg)", )
    expEmp_whtrade = CreateExpression(jvwEmpLUovrDist, "emp_whtrade", "Nz(emp*whtrade)", )
    expEmp_retrade = CreateExpression(jvwEmpLUovrDist, "emp_retrade", "Nz(emp*retrade)", )
    expEmp_twu = CreateExpression(jvwEmpLUovrDist, "emp_twu", "Nz(emp*twu)", )
    expEmp_fre = CreateExpression(jvwEmpLUovrDist, "emp_fre", "Nz(emp*fre)", )
    expEmp_info = CreateExpression(jvwEmpLUovrDist, "emp_info", "Nz(emp*info)", )
    expEmp_pbs = CreateExpression(jvwEmpLUovrDist, "emp_pbs", "Nz(emp*pbs)", )
    expEmp_lh = CreateExpression(jvwEmpLUovrDist, "emp_lh", "Nz(emp*lh)", )
    expEmp_os = CreateExpression(jvwEmpLUovrDist, "emp_os", "Nz(emp*os)", )
    expEmp_edhs = CreateExpression(jvwEmpLUovrDist, "emp_edhs", "Nz(emp*edhs)", )
    expEmp_gov = CreateExpression(jvwEmpLUovrDist, "emp_gov", "Nz(emp*gov)", )
    expEmp_sedw = CreateExpression(jvwEmpLUovrDist, "emp_sedw", "Nz(emp*sedw)", )
    expEmp_civ = CreateExpression(jvwEmpLUovrDist, "emp_civ", "emp_agmin + emp_cons + emp_mfg + emp_whtrade + emp_retrade + emp_twu + emp_fre + emp_info + emp_pbs + emp_lh + emp_os + emp_edhs + emp_gov + emp_sedw", )

    // Export employment distribution before aggregation
    ExportView(jvwEmpLUovrDist+"|", "CSV", empDistLUovr_filecsv, {"zone", "emp", "emp_civ", "emp_mil", "emp_agmin", "emp_cons", "emp_mfg", "emp_whtrade", "emp_retrade", "emp_twu", "emp_fre", "emp_info", "emp_pbs", "emp_lh", "emp_os", "emp_edhs", "emp_gov", "emp_sedw"}, {{"CSV Header", "True"},{"CSV Drop Quotes", "True"}})
    
    // Aggregate employment distribution by LU Code by TAZ to employment distribution by TAZ
    RunMacro("HwycadLog",{"TruckModel.rsc: EmploymentDistLUOverride","AggregateTable"})
    vwEmpDistLUovr=opentable("DistributedEmploymentOverride", "CSV", {empDistLUovr_filecsv,})
    Setview(vwEmpDistLUovr)
    AggAll = CreateSet("AggregatedZones")
    SelectAll(AggAll)
    sets_list = GetSets(vwEmpDistLUovr)
    aggtable = AggregateTable("ZoneAggEmpDistLUOvr","DistributedEmploymentOverride|AggregatedZones", "CSV", empAggDistLUovr_filecsv, "zone", {
       {"zone","dominant"},
       {"emp_agmin","sum", },
       {"emp_cons","sum", },
       {"emp_retrade","sum", },
       {"emp_gov","sum", },
       {"emp_mfg","sum", },     
       {"emp_twu","sum", },
       {"emp_whtrade","sum", },
       {"emp_os","sum", },
       {"emp_sedw","sum", }
       }, {"Missing as zero"})
    
    done:
      RunMacro("close all")
      Return( RunMacro("TCB Closing", 1, "FALSE" ) )
    
    quit:
      RunMacro("close all")
      Return( RunMacro("TCB Closing", 0, "FALSE" ) )
EndMacro


/**********************************************************************************************************
Checks whether data is available for forecast year and if not then interpolates from closet available years
 
Inputs:
   forecast year
   
Outputs:
   returns an array {availability of data, previous data year, next data year}
**********************************************************************************************************/
Macro "InterimYearCheck" (properties,intFyear)
    dim arrInterimYear[3]

    // Reads all years for which data is available   
    DFyear =RunMacro("read properties",properties,"truck.DFyear","S")   
 
    // Lists data year with delimiter ","
    arrAllDFyears = ParseString(DFyear, ",")                              
    for i = 1 to arrAllDFyears.length do
       intTargetYear = s2i(trim(arrAllDFyears[i]))
       if i < arrAllDFyears.length then do 
          intNextTargetYear = s2i(trim(arrAllDFyears[i+1]))
       end
       
       // Forecast year has data available
       if intFyear = intTargetYear then do  
          // Do interpolate = False  
          arrInterimYear[1]=1 
       end
       
       // Forecast year has no data
       else do  
          // Check for previous and next closest years
          if (intFyear > intTargetYear & intFyear < intNextTargetYear) then do 
             // Do interpolate = True   
             arrInterimYear[1] = 2   
             // Gets previous closest year               
             arrInterimYear[2] = intTargetYear    
             // Gets next closest year   
             arrInterimYear[3] = intNextTargetYear     
          end
       end
    end
    
    Return(arrInterimYear)  
EndMacro



/**********************************************************************************************************
Adds trucks generated by special generators, such as military sites, mail to/from airport, cruise ships, etc
 
Inputs:
   input\specialGenerators.csv
   output\gmTruckDataII.csv
   
Outputs:
    output\gmTruckDataIISP.csv
    
**********************************************************************************************************/
Macro "trkSpecialGen" (strFyear,intFyear,arrInterimYear)
    shared path, inputDir, outputDir
  
    booInterimYear = arrInterimYear[1]
    // 1 = Forecast year, 2 = Interim year and needs interpolation
    if booInterimYear = 2 then do 
       prevYear = arrInterimYear[2]
       nextYear = arrInterimYear[3]
    end
    
    // Open truck trips and truck special generators
    baseTrucks = Opentable("TruckGeneration", "CSV", {outputDir+"\\gmTruckDataII.csv"})
    specGenerators = Opentable("SpecGenMilit", "CSV",{inputDir+"\\specialGenerators.csv"})
    jv1 = Joinviews("JV1", baseTrucks+".ZONE", specGenerators+".TAZ", )
    Setview(jv1)
    
    // Forecast year has data available
    if booInterimYear = 1 then do      
       col_name="Y"+strFyear
       lhd_a = CreateExpression(jv1, "LHD_Attr", "LHD_Attractions + Nz("+col_name+" * trkAttraction * lhdShare)", )
       lhd_p = CreateExpression(jv1, "LHD_Prod", "LHD_Productions + Nz("+col_name+" * trkProduction * lhdShare)", )
       mhd_a = CreateExpression(jv1, "MHD_Attr", "MHD_Attractions + Nz("+col_name+" * trkAttraction * mhdShare)", )
       mhd_p = CreateExpression(jv1, "MHD_Prod", "MHD_Productions + Nz("+col_name+" * trkProduction * mhdShare)", )
       hhd_a = CreateExpression(jv1, "HHD_Attr", "HHD_Attractions + Nz("+col_name+" * trkAttraction * hhdShare)", )
       hhd_p = CreateExpression(jv1, "HHD_Prod", "HHD_Productions + Nz("+col_name+" * trkProduction * hhdShare)", )
       RunMacro("HwycadLog",{"TruckModel.rsc: trkSpecialGen","ExportView P&A + Special Generators"})
       ExportView(jv1+"|", "CSV", outputDir+"\\gmTruckDataIISP.csv", {baseTrucks+".ZONE", "LHD_Prod", "LHD_Attr", "MHD_Prod", "MHD_Attr", "HHD_Prod", "HHD_Attr"}, {{"CSV Header"}})
    end
    
    // If data is not available then interpolate from closest available years
    else do  
       // Get previous and next closest year    
       dim IY_Year[2]
       IY_Year[1] = prevYear 
       IY_Year[2] = nextYear
        
       for j = 1 to 2 do
          view="jv"+I2S(j)
          Setview(view)
          col_name="Y"+I2S(IY_Year[j])
          lhd_a = CreateExpression(view, "LHD_Attr", "LHD_Attractions + Nz("+col_name+" * trkAttraction * lhdShare)", )
          lhd_p = CreateExpression(view, "LHD_Prod", "LHD_Productions + Nz("+col_name+" * trkProduction * lhdShare)", )
          mhd_a = CreateExpression(view, "MHD_Attr", "MHD_Attractions + Nz("+col_name+" * trkAttraction * mhdShare)", )
          mhd_p = CreateExpression(view, "MHD_Prod", "MHD_Productions + Nz("+col_name+" * trkProduction * mhdShare)", )
          hhd_a = CreateExpression(view, "HHD_Attr", "HHD_Attractions + Nz("+col_name+" * trkAttraction * hhdShare)", )
          hhd_p = CreateExpression(view, "HHD_Prod", "HHD_Productions + Nz("+col_name+" * trkProduction * hhdShare)", )
          RunMacro("HwycadLog",{"TruckModel.rsc: trkSpecialGen","ExportView P&A + Special Generators"})
          ExportView(view+"|", "CSV", outputDir+"\\gmTruckDataIISP"+I2S(IY_Year[j])+".csv", {"ZONE", "LHD_Prod", "LHD_Attr", "MHD_Prod", "MHD_Attr", "HHD_Prod", "HHD_Attr"}, {{"CSV Header"}})
          Closeview(view)
          jv2 = Joinviews("JV2", baseTrucks+".ZONE", specGenerators+".TAZ", )
       end 
       
       ok=RunMacro("Interpolate",{"gmTruckDataIISP"+I2S(prevYear)+".csv","gmTruckDataIISP"+I2S(nextYear)+".csv","gmTruckDataIISP.csv",intFyear,prevYear,nextYear}) 
    end 

EndMacro


/**********************************************************************************************************
Balances total production and attraction for lhd, mhd, hhd, ei and ie trips
 
Inputs:
   input\specialGenerators.csv
   output\gmTruckDataII.csv    
   
   inputTruckDir\regionalEItrips<prev_year>.csv
   inputTruckDir\regionalEItrips<next_year>.csv 
   inputTruckDir\regionalIEtrips<prev_year>.csv  
   inputTruckDir\regionalIEtrips<next_year>.csv  
   inputTruckDir\regionalEEtrips<prev_year>.csv  
   inputTruckDir\regionalEEtrips<next_year>.csv  

Outputs:
  output\gmTruckDataBalanced.bin
       
**********************************************************************************************************/
Macro "trkBalance" (strFyear,intFyear,arrInterimYear)
    shared path, inputDir, outputDir, inputTruckDir

    // Check if interim year and interpolate trip files if it is
    booInterimYear = arrInterimYear[1]
    
    // 1 = Forecast year, 2 = Interim year and needs interpolation
    if booInterimYear = 2 then do 
       prevYear = arrInterimYear[2]
       nextYear = arrInterimYear[3]
       
       // copy files to scenario directory
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalEItrips"+I2S(prevYear)+".csv",outputDir+"\\regionalEItrips_prev.csv"})
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalEItrips"+I2S(nextYear)+".csv",outputDir+"\\regionalEItrips_next.csv"})
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalIEtrips"+I2S(prevYear)+".csv",outputDir+"\\regionalIEtrips_prev.csv"})
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalIEtrips"+I2S(nextYear)+".csv",outputDir+"\\regionalIEtrips_next.csv"})
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalEEtrips"+I2S(prevYear)+".csv",outputDir+"\\regionalEEtrips_prev.csv"})
       ok=RunMacro("SDcopyfile",{inputTruckDir+"\\regionalEEtrips"+I2S(nextYear)+".csv",outputDir+"\\regionalEEtrips_next.csv"})
       
       // Call Macro Interpolate //arr = {"previous year data file","next year data file","new year data file name(macro will create this)}
       ok=RunMacro("Interpolate",{"regionalEItrips_prev.csv","regionalEItrips_next.csv","regionalEItrips.csv",intFyear,prevYear,nextYear}) 
       ok=RunMacro("Interpolate",{"regionalIEtrips_prev.csv","regionalIEtrips_next.csv","regionalIEtrips.csv",intFyear,prevYear,nextYear})
       ok=RunMacro("Interpolate",{"regionalEEtrips_prev.csv","regionalEEtrips_next.csv","regionalEEtrips.csv",intFyear,prevYear,nextYear})
       
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEItrips_prev.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEItrips_next.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalIEtrips_prev.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalIEtrips_next.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEEtrips_prev.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEEtrips_next.csv"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEItrips_prev.dcc"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEItrips_next.dcc"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalIEtrips_prev.dcc"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalIEtrips_next.dcc"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEEtrips_prev.dcc"})
       ok=RunMacro("SDdeletefile",{outputDir+"\\regionalEEtrips_next.dcc"})
    end
    
    // If not interim year then copy EE to output folder
    if booInterimYear = 1 then do
       RunMacro("SDcopyfile",{inputTruckDir+"\\regionalEEtrips"+strFyear+".csv",outputDir+"\\regionalEEtrips.csv"})
    end
    // Balance each truck type (except EE [external-external], which is already balanced)
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","Balance LHD"})
    RunMacro("trkBalanceOneType", "LHD")
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","Balance MHD"})
    RunMacro("trkBalanceOneType", "MHD")
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","Balance HHD"})
    RunMacro("trkBalanceOneType", "HHD")
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","Balance EI"})
    RunMacro("trkBalanceRegionalTrucks", "EI", strFyear,booInterimYear)
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","Balance IE"})
    RunMacro("trkBalanceRegionalTrucks", "IE", strFyear,booInterimYear)
     
    // Combine single balanced files
    vw1 = Opentable("BALANCE_LHD", "FFB", {outputDir+"\\gmTruckDataBal_LHD.bin",})
    vw2 = Opentable("BALANCE_MHD", "FFB", {outputDir+"\\gmTruckDataBal_MHD.bin",})
    vw3 = Opentable("BALANCE_HHD", "FFB", {outputDir+"\\gmTruckDataBal_HHD.bin",})
    vw4 = Opentable("BALANCE_EI", "FFB",  {outputDir+"\\regionalEItripsBalanced.bin",})
    RenameField("BALANCE_EI.HHD_Attr" ,"EI_Attr")
    RenameField("BALANCE_EI.EItrucks" ,"EI_Prod")
    
    Opts = null
    Opts.Input.[Dataview Set] = {outputDir+"\\regionalEItripsBalanced.bin" , vw4}
    Opts.Global.Fields = {"EI_Prod"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if EI_Prod=null then 0.00 else EI_Prod"}
    ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    
    vw5 = Opentable("BALANCE_IE", "FFB", {outputDir+ "\\regionalIEtripsBalanced.bin",})
    RenameField("BALANCE_IE.IEtrucks" ,"IE_Attr")
    RenameField("BALANCE_IE.HHD_Prod" ,"IE_Prod")
    Opts = null
    Opts.Input.[Dataview Set] = {outputDir+ "\\regionalIEtripsBalanced.bin" , vw5}
    Opts.Global.Fields = {"IE_Attr"}
    Opts.Global.Method = "Formula"
    Opts.Global.Parameter = {"if IE_Attr=null then 0.00 else IE_Attr"}
    ok = RunMacro("TCB Run Operation", 1, "Fill Dataview", Opts)
    
    jv1 = Joinviews("JV1", vw1 + ".ID1", vw2 + ".ID1",)
    jv2 = Joinviews("JV2", jv1 + ".BALANCE_LHD.ID1", vw3 + ".ID1",)
    jv3 = Joinviews("JV3", jv2 + ".BALANCE_LHD.ID1", vw4 + ".ID1",)
    jv4 = Joinviews("JV4", jv3 + ".BALANCE_LHD.ID1", vw5 + ".ID1",)
    RenameField("BALANCE_LHD.ID1", "ID")

    Setview(jv4)
    n = SelectByQuery("notzeros", "several", "Select * where ID <> 0",)
    
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalance","ExportView - Combined Balanced Flow"})
    ExportView("JV4|notzeros", "FFB", outputDir+"\\gmTruckDataBalanced.bin", {"ID","LHD_Prod", "LHD_Attr", "MHD_Prod", "MHD_Attr", "HHD_Prod", "HHD_Attr", "IE_Prod", "IE_Attr", "EI_Prod","EI_Attr"},)

    Closeview(jv1)
    Closeview(jv2)
    Closeview(jv3)
    Closeview(jv4)
    Closeview(vw1)
    Closeview(vw2)
    Closeview(vw3)
    Closeview(vw4)
    Closeview(vw5)
EndMacro


/**********************************************************************************************************
Balances total production and attraction for truck type (type = lhd, mhd or hhd)                              
                                                                                                           
Inputs:                                                                                                    
   output\gmTruckDataIISP.csv                                                                               
                                                                 
Outputs:                                                                                                   
   output\gmTruckDataBal_lhd.bin                                                                        
   output\gmTruckDataBal_mhd.bin
   output\gmTruckDataBal_hhd.bin                                                                                                          
**********************************************************************************************************/
Macro "trkBalanceOneType" (type)
    shared path, inputDir, outputDir
    
    RunMacro("TCB Init")
    
    Opts = null
    Opts.Input.[Data View Set] = {outputDir+"\\gmTruckDataIISP.csv", "gmTruckDataIISP"}
    Opts.Field.[Vector 1] = {"[gmTruckDataIISP]." + type + "_Prod"}
    Opts.Field.[Vector 2] = {"[gmTruckDataIISP]." + type + "_Attr"}
    Opts.Global.[Holding Method] = {"Weighted Sum"}
    Opts.Global.[Percent Weight] = {50}
    Opts.Global.[Sum Weight] = {100}
    Opts.Global.[Store Type] = 1
    Opts.Output.[Output Table] = outputDir+"\\gmTruckDataBal_" + type + ".bin"
    
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalanceOneType", "Balance "+type})
    ok=RunMacro("TCB Run Procedure", "Balance", Opts, &Ret)
EndMacro


/********************************************************************************************************** 
Balances EI trips where sum of E is fixed and I of truck attraction is adjusted or
IE trips where sum of E is fixed and I of truck production is adjusted                           
                                                                                                            
Inputs:   
   output\regionalIEtrips.csv   
   output\regionalEItrips.csv                                                                                             
   inputTruckDir\regionalIEtrips<prev_year>.csv                                                                               
   inputTruckDir\regionalIEtrips<next_year>.csv
   inputTruckDir\regionalEItrips<prev_year>.csv
   inputTruckDir\regionalEItrips<next_year>.csv
   output\gmTruckDataBal_HHD.bin 
                                                                                                                 
Outputs:                                                                                                    
   output\regional<IE/EI>tripsBalanced.bin
                                                                                                                 
**********************************************************************************************************/ 
Macro "trkBalanceRegionalTrucks" (direction,strFyear,booInterimYear)
    shared path, inputDir, outputDir, inputTruckDir
      
    // Not an interim year, read regional trips from inputTruckDir
    if booInterimYear=1 then do 
       strPathRegTrips=inputTruckDir+"\\regional"+direction+"trips"+strFyear+".csv"
    end
    
    //Interim year, read regional trips from output directory
    else do 
       strPathRegTrips=outputDir+"\\regional"+direction+"trips.csv"
    end
    
    Opts = null
    if direction = "EI" then do
       Opts.Input.[Data Set] = {{outputDir+"\\gmTruckDataBal_HHD.bin", strPathRegTrips, "ID1", "fromZone"}, "BALANCE_HHD+regionalEItrips"+strFyear}
       Opts.Input.[Data View] = {{outputDir+"\\gmTruckDataBal_HHD.bin", strPathRegTrips, "ID1", "fromZone"}, "BALANCE_HHD+regionalEItrips"+strFyear}
    end
    else do
       Opts.Input.[Data Set] = {{outputDir+"\\gmTruckDataBal_HHD.bin", strPathRegTrips, "ID1", "toZone"}, "BALANCE_HHD+regionalIEtrips"+strFyear}
       Opts.Input.[Data View] = {{outputDir+"\\gmTruckDataBal_HHD.bin", strPathRegTrips, "ID1", "toZone"}, "BALANCE_HHD+regionalIEtrips"+strFyear}
    end
    
    Opts.Input.[V1 Holding Sets] = {}
    Opts.Input.[V2 Holding Sets] = {}
    if direction = "EI" then do
       Opts.Field.[Vector 1] = {"[BALANCE_HHD+regionalEItrips"+strFyear+"].HHD_Attr"}
       Opts.Field.[Vector 2] = {"[BALANCE_HHD+regionalEItrips"+strFyear+"].EITrucks"}
    end
    else do
       Opts.Field.[Vector 1] = {"[BALANCE_HHD+regionalIEtrips"+strFyear+"].HHD_Prod"}
       Opts.Field.[Vector 2] = {"[BALANCE_HHD+regionalIEtrips"+strFyear+"].IETrucks"}
    end
    
    Opts.Global.Pairs = 1
    Opts.Global.[Holding Method] = {2}
    Opts.Global.[Percent Weight] = {50}
    Opts.Global.[Sum Weight] = {100}
    Opts.Global.[V1 Options] = {1}
    Opts.Global.[V2 Options] = {1}
    Opts.Global.[Store Type] = 1
    Opts.Output.[Output Table] = outputDir+"\\regional"+direction+"tripsBalanced.bin"
    
    RunMacro("HwycadLog",{"TruckModel.rsc: trkBalanceRegionalTrucks","Balance "+direction})
    RunMacro("TCB Run Procedure", 1, "Balance", Opts)
EndMacro



/********************************************************************************************************** 
Distributes truck lhd, mhd, hhd, ie and ei truck trips                                     
                                                                                                            
Inputs:                                                                                                     
   output\gmTruckDataBalanced.bin                                                            
   output\regionalEEtrips.csv
   output\imptrk_EA.mtx
   output\imptrk_AM.mtx
   output\imptrk_MD.mtx
   output\imptrk_PM.mtx
   output\imptrk_EV.mtx
                                                                                                   
Outputs: 
   output\distributionMatricesTruck.mtx
   outputDir\regionalEEtrips.mtx                                                                                               
   outputDir\dailyDistributionMatricesTruckAll.mtx                                                                   
   outputDir\dailyDistributionMatricesTruckEA.mtx
   outputDir\dailyDistributionMatricesTruckAM.mtx
   outputDir\dailyDistributionMatricesTruckMD.mtx
   outputDir\dailyDistributionMatricesTruckPM.mtx
   outputDir\dailyDistributionMatricesTruckEV.mtx
      
**********************************************************************************************************/ 

Macro "trkDistribution" (properties)
    shared path, inputDir, outputDir
   
    // Get forecast year
    strFyear=RunMacro("read properties",properties,"truck.FFyear","S")
    
    //--------------------------------------------------
    // This section does the truck trip distribution 
    //--------------------------------------------------
    // Use mid-day (md) truck skim for distribution
    periods = {"_EA", "_AM", "_MD", "_PM", "_EV"} 
    p = 3
    md_truck_skim = outputDir +"\\imptrk"+periods[p]+".mtx"
    md_truck_FF   = "*SCST"+periods[p]  
    md_truck_IM   = "*STM"+periods[p]+" (Skim)" 
    md_truck_KF   = "*STM"+periods[p]+" (Skim)"   
    
    // Open md truck skims
    Opts.Input.[FF Matrix Currencies]  = {{md_truck_skim, md_truck_FF,,},{md_truck_skim, md_truck_FF,,}, {md_truck_skim, md_truck_FF,,}, {md_truck_skim, md_truck_FF,,},{md_truck_skim, md_truck_FF,,}}
    Opts.Input.[Imp Matrix Currencies] = {{md_truck_skim, md_truck_IM,,},{md_truck_skim, md_truck_IM,,}, {md_truck_skim, md_truck_IM,,}, {md_truck_skim, md_truck_IM,,},{md_truck_skim, md_truck_IM,,}}  
    Opts.Input.[KF Matrix Currencies]  = {{md_truck_skim, md_truck_KF,,},{md_truck_skim, md_truck_KF,,}, {md_truck_skim, md_truck_KF,,}, {md_truck_skim, md_truck_KF,,},{md_truck_skim, md_truck_KF,,}}  

    // Open truck trips
    Opts.Input.[PA View Set] = {outputDir+"\\gmTruckDataBalanced.bin", "gmTruckDataBalanced"}

    // Set gravity model settings 
    Opts.Input.[FF Tables] = {{outputDir+"\\gmTruckDataBalanced.bin"}, {outputDir+"\\gmTruckDataBalanced.bin"}, {outputDir+"\\gmTruckDataBalanced.bin"}, {outputDir+"\\gmTruckDataBalanced.bin"}, {outputDir+"\\gmTruckDataBalanced.bin"}}
    Opts.Field.[Prod Fields] = {"gmTruckDataBalanced.LHD_Prod", "gmTruckDataBalanced.MHD_Prod", "gmTruckDataBalanced.HHD_Prod", "gmTruckDataBalanced.IE_Prod", "gmTruckDataBalanced.EI_Prod"}
    Opts.Field.[Attr Fields] = {"gmTruckDataBalanced.LHD_Attr", "gmTruckDataBalanced.MHD_Attr", "gmTruckDataBalanced.HHD_Attr", "gmTruckDataBalanced.IE_Attr", "gmTruckDataBalanced.EI_Attr"}
    Opts.Field.[FF Table Fields] = {"gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID"}
    Opts.Field.[FF Table Times] = {"gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID", "gmTruckDataBalanced.ID"}
    Opts.Global.[Purpose Names] = {"lhd", "mhd", "hhd", "IE", "EI"}
    Opts.Global.Iterations = {100, 100, 100, 100, 100}
    Opts.Global.Convergence = {0.01, 0.01, 0.01, 0.01, 0.01}
    Opts.Global.[Constraint Type] = {"Double", "Double", "Double", "Columns", "Rows"}
    Opts.Global.[Fric Factor Type] = {"Exponential", "Exponential", "Exponential", "Exponential", "Exponential"}
    Opts.Global.[A List] = {1, 1, 1, 1, 1}
    Opts.Global.[B List] = {0.3, 0.3, 0.3, 0.3, 0.3}
    Opts.Global.[C List] = {0.045, 0.03, 0.03, 0.03, 0.03}
    Opts.Flag.[Use K Factors] = {0, 0, 0, 0, 0}
    Opts.Output.[Output Matrix].Label = "Distribution Matrix"
    Opts.Output.[Output Matrix].Compression = 1
    Opts.Output.[Output Matrix].[File Name] = outputDir + "\\distributionMatricesTruck.mtx"
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","Gravity"})
    RunMacro("TCB Run Procedure", 1, "Gravity", Opts)


    //--------------------------------------------------
    // Adds IE, EI and EE trips to the truck trips by type
    //--------------------------------------------------
    // Create EE trip matrix
    viewEE = Opentable("EEtrips", "CSV", {outputDir + "\\regionalEEtrips.csv"})
    matrixEE = CreateMatrixFromView("EEmatrix", "EEtrips|", "fromZone", "toZone", {"EEtrucks"}, {{"File Name", outputDir + "\\regionalEEtrips.mtx"}, {"Type", "Float"}, {"Sparse", "No"}, {"Column Major", "No"}, {"File Based", "Yes"}})
    Closeview(viewEE)
    
    // Split truck trips by type
    trkShare = {0.307, 0.155, 0.538}
    trkTypes = {"lhd", "mhd", "hhd"}
    
    Opts = null
    Opts.Input.[Matrix Currencies] = {{outputDir + "\\distributionMatricesTruck.mtx", "lhd", "Row ID's", "Col ID's"}, 
                                      {outputDir + "\\distributionMatricesTruck.mtx", "mhd", "Row ID's", "Col ID's"}, 
                                      {outputDir + "\\distributionMatricesTruck.mtx", "hhd", "Row ID's", "Col ID's"}, 
                                      {outputDir + "\\distributionMatricesTruck.mtx", "IE", "Row ID's", "Col ID's"}, 
                                      {outputDir + "\\distributionMatricesTruck.mtx", "EI", "Row ID's", "Col ID's"}, 
                                      {outputDir + "\\regionalEEtrips.mtx", "EEtrucks", "fromZone", "toZone"}}
    Opts.Global.Operation = "Union"
    Opts.Output.[Combined Matrix].Label = "Union Combine"
    Opts.Output.[Combined Matrix].Compression = 1
    Opts.Output.[Combined Matrix].[File Name] = outputDir + "\\dailyDistributionMatricesTruckAll.mtx"
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","Combine Matrix Files"})
    RunMacro("TCB Run Operation", 1, "Combine Matrix Files", Opts)

 
    // Aportion and add IE, EI and EE trips to LHD, MHD and HHD 
    for i = 1 to trkTypes.length do
       Opts = null
       Opts.Input.[Matrix Currency] = {outputDir + "\\dailyDistributionMatricesTruckAll.mtx", trkTypes[i], "Rows", "Columns"}
       Opts.Input.[Core Currencies] = {{outputDir + "\\dailyDistributionMatricesTruckAll.mtx",trkTypes[i], "Rows", "Columns"}, 
                                       {outputDir + "\\dailyDistributionMatricesTruckAll.mtx","EI", "Rows", "Columns"}, 
                                       {outputDir + "\\dailyDistributionMatricesTruckAll.mtx","IE", "Rows", "Columns"}, 
                                       {outputDir + "\\dailyDistributionMatricesTruckAll.mtx","EEtrucks", "Rows", "Columns"}}
       Opts.Global.Method = 7
       Opts.Global.[Cell Range] = 2
       Opts.Global.[Matrix K] = {1, trkShare[i], trkShare[i], trkShare[i]}
       Opts.Global.[Force Missing] = "No"
       RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","Fill Matrices"})
       ok = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
    end

    // Drop IE matrix core
    Opts = null
    Opts.input.[Input Matrix] = outputDir + "\\dailyDistributionMatricesTruckAll.mtx"
    Opts.global.[Drop Core] = {"IE"}
    RunMacro("TCB Run Operation", 1, "Drop Matrix Core", Opts)

    // Drop EI matrix core    
    Opts = null
    Opts.input.[Input Matrix] = outputDir + "\\dailyDistributionMatricesTruckAll.mtx"
    Opts.global.[Drop Core] = {"EI"}
    RunMacro("TCB Run Operation", 1, "Drop Matrix Core", Opts)

    // Drop EE matrix core
    Opts = null
    Opts.input.[Input Matrix] = outputDir + "\\dailyDistributionMatricesTruckAll.mtx"
    Opts.global.[Drop Core] = {"EEtrucks"}
    RunMacro("TCB Run Operation", 1, "Drop Matrix Core", Opts)


    //--------------------------------------------------
    // Set intrazonal truck trips to 0. Note: intrazonal truck trips are not necessarily 0 in reality, but they are
    // not simulated in this model. Having an undefined value for intrazonal trips provides problems in the emission
    // estimation of the EMFAC2007 model. Therefore, intrazonals are artificially set to 0.
    //--------------------------------------------------     
    Opts = null
    Opts.Input.[Matrix Currency] = {outputDir + "\\dailyDistributionMatricesTruckAll.mtx", "lhd", "Rows", "Columns"}
    Opts.Global.Method = 1
    Opts.Global.Value = 0
    Opts.Global.[Cell Range] = 3
    Opts.Global.[Matrix Range] = 3
    Opts.Global.[Matrix List] = {"lhd", "mhd", "hhd"}
    ok = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
    if !ok then goto quit
      
    // Split into time of day
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","splitIntoTimeOfDay, EA"})
    RunMacro("splitIntoTimeOfDay", 1)  // EA Off-peak
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","splitIntoTimeOfDay, AM"})
    RunMacro("splitIntoTimeOfDay", 2)  // AM Peak
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","splitIntoTimeOfDay, MD"})
    RunMacro("splitIntoTimeOfDay", 3)  // MD Off-peak
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","splitIntoTimeOfDay, PM"})
    RunMacro("splitIntoTimeOfDay", 4)  // PM Peak
    RunMacro("HwycadLog",{"TruckModel.rsc: trkDistribution","splitIntoTimeOfDay, EV"})
    RunMacro("splitIntoTimeOfDay", 5)  // EV Off-peak
    
    run_ok=1
    Return(run_ok)
    
    quit:
       Return(ok)
EndMacro


/********************************************************************************************************** 
Splits daily truck trips to five periods EA, AM, MD, PM and EV                                                    
                                                                                                            
Inputs:                                                                                                     
   outputDir\dailyDistributionMatricesTruckAll.mtx                                                                          
   timeShare       = {0.1018, 0.1698, 0.4284, 0.1543, 0.1457}                                                                        
   borderTimeShare = {0.0188, 0.1812, 0.4629, 0.2310, 0.1061}                                                                                     
                        
Outputs:                                                                                                    
   outputDir\dailyDistributionMatricesTruckEA.mtx                                                           
   outputDir\dailyDistributionMatricesTruckAM.mtx                                                           
   outputDir\dailyDistributionMatricesTruckMD.mtx                                                           
   outputDir\dailyDistributionMatricesTruckPM.mtx                                                           
   outputDir\dailyDistributionMatricesTruckEV.mtx                                                           
                                                                                                            
**********************************************************************************************************/ 
Macro "splitIntoTimeOfDay" (period)
    shared path, inputDir, outputDir
    
    // Split lhd, mhd and hhd truck trips into time-of-day periods
    periodName      = {"EA","AM","MD","PM","EV"}
    mode            = {"lhd", "mhd", "hhd"}
    timeShare       = {0.1018, 0.1698, 0.4284, 0.1543, 0.1457}   // share of truck trips per time period for all zones except border crossings
    borderTimeShare = {0.0188, 0.1812, 0.4629, 0.2310, 0.1061}   // share of truck trips per time period for border crossings
    dim borderCorrection[timeShare.length]                       // correct values at border after multiplication with timeShare
    for i = 1 to borderCorrection.length do
       borderCorrection[i] = borderTimeShare[i] / timeShare[i]
    end

    // Copy original matrix into time-of-day matrix
    mat = OpenMatrix(outputDir + "\\dailyDistributionMatricesTruckAll.mtx", )
    mc = CreateMatrixCurrency(mat, "lhd", "Rows", "Columns", )
    newFile = outputDir + "\\dailyDistributionMatricesTruck" + periodName[period] + ".mtx"
    label = "Truck Table " + periodName[period] + " Peak"
    new_mat = CopyMatrix(mc, {{"File Name", newFile}, {"Label", label}, {"File Based", "Yes"}})
  
    // Multiply entire matrix with time-of-day share
    Opts = null
    Opts.Input.[Matrix Currency] = {newFile, "lhd", "Rows", "Columns"}
    Opts.Global.Method = 5
    Opts.Global.Value = timeShare[period]
    Opts.Global.[Cell Range] = 2
    Opts.Global.[Matrix Range] = 3
    Opts.Global.[Matrix List] = {"lhd", "mhd", "hhd"}
    RunMacro("HwycadLog",{"TruckModel.rsc: splitIntoTimeOfDay","Fill Matrices"})
    ok = RunMacro("TCB Run Operation", 1, "Fill Matrices", Opts)
    if !ok then goto quit
      
    // Correct time-of-day share for destination zones 1 through 5 (border zones)
    for i = 1 to mode.length do
       mat = OpenMatrix(newFile,)
       mc = CreateMatrixCurrency(mat, mode[i], "Rows", "Columns", )
       cols = {"1", "2", "3", "4", "5"}
       operation = {"Multiply", borderCorrection[period]}
       FillMatrix(mc, null, cols, operation, )
       
       mat = OpenMatrix(newFile,)
       mc = CreateMatrixCurrency(mat, mode[i], "Rows", "Columns", )
       rows = {"1", "2", "3", "4", "5"}
       operation = {"Multiply", borderCorrection[period]}
       ok = FillMatrix(mc, rows, null, operation, )
       if !ok then goto quit
    end

    run_ok=1
    Return(run_ok)
    
    quit:
      Return(ok)  
EndMacro




/********************************************************************************************************** 
  Truck Toll Diversion Model
  Splits Truck Demand to Non-Toll and Toll
  called after truck model but before combine trktrips
  Ben Stabler, stabler@pbworld.com, 12/02/10                                  
                                                                                                            
Inputs:
   Each skim matrix is suffixed with xx, yy where:
   xx is mode indicating following truck types:
     lhdn, mhdn, hhdn, lhdt, mhdt, and hhdt
     
   yy is period, as follows:
     EA: Early AM
     AM: AM peak
     MD: Midday
     PM: PM peak
     EV: Evening
   
   output\impXXYY.mtx                                                                                       
   outputDir\dailyDistributionMatricesTruckYY.mtx
                                                                                                   
Outputs:
   Adds toll and non-toll cores to 
   outputDir\dailyDistributionMatricesTruckYY.mtx

**********************************************************************************************************/ 
Macro "trk toll diversion model"
    shared path, inputDir, outputDir    
    RunMacro("TCB Init")
  
    // Toll diversion curve settings
    nest_param = 10
    vot = 0.02                                      //(minutes/cent)
       
    periodName    = {"EA","AM","MD","PM","EV"}      // must be consistent with filename arrays below
    trkTypes      = {"lhd", "mhd", "hhd"}           // truck types
    trkTollFactor = {1, 1.03, 2.33}                 // truck toll factor
  
    // Loop by time period
    for period = 1 to periodName.length do
      
       // Open truck trips
       fileNameTruck = outputDir + "\\dailyDistributionMatricesTruck" + periodName[period] + ".mtx"
       m = OpenMatrix(fileNameTruck,)
       
       // Loop by truck class
       for trkType = 1 to trkTypes.length do
          nontollmtx=outputDir+"\\imp"+trkTypes[trkType]+"n_"+periodName[period]+".mtx"   // non-toll skims 
          tollmtx=outputDir+"\\imp"+trkTypes[trkType]+"t_"+periodName[period]+".mtx"      // toll skims 
          
          // Check and if exist, drop toll and non-toll matrix cores by truck type 
          coreNames = GetMatrixCoreNames(m)
          for c = 1 to coreNames.length do
             if (coreNames[c] = trkTypes[trkType] + "t") then DropMatrixCore(m, trkTypes[trkType] + "t")
             if (coreNames[c] = trkTypes[trkType] + "n") then DropMatrixCore(m, trkTypes[trkType] + "n")
          end 
          
          // Add toll and non-toll matrix
          AddMatrixCore(m, trkTypes[trkType] + "t")
          AddMatrixCore(m, trkTypes[trkType] + "n")
          
          // Diversion curve (time is in minutes, cost is in cents)
          utility = "(([impedance truck].[*STM_"+periodName[period]+" (Skim)] - [impedance truck toll].[*STM_"+periodName[period]+" (Skim)]) - " + 
                      String(vot) + " * " + "[impedance truck toll].["+trkTypes[trkType]+"t - "+"ITOLL2_"+periodName[period]+"] * " + String(trkTollFactor[trkType]) + " ) / " + String(nest_param)
          
          expression = "if([impedance truck toll].["+trkTypes[trkType]+"t - "+"ITOLL2_"+periodName[period]+"]!=0) then ( 1 / ( 1 + exp(-" + utility + ") ) ) else [impedance truck toll].["+trkTypes[trkType]+"t - "+"ITOLL2_"+periodName[period]+"]"
          
          // Calculate toll matrix
          Opts = null
          Opts.Input.[Matrix Currency]    = {fileNameTruck, trkTypes[trkType] + "t", "Rows", "Columns"}
          Opts.Input.[Formula Currencies] = {{nontollmtx, "*STM_"+periodName[period]+" (Skim)", "Origin", "Destination"}, {tollmtx, "*STM_"+periodName[period]+" (Skim)", "Origin", "Destination"}}
          Opts.Global.Method              = 11
          Opts.Global.[Cell Range]        = 2
          Opts.Global.[Expression Text]   = "[" + trkTypes[trkType] + "] * " + expression
          Opts.Global.[Formula Labels]    = {"impedance truck", "impedance truck toll"}
          Opts.Global.[Force Missing]     = "Yes"
          ok = RunMacro("TCB Run Operation", "Fill Matrices", Opts)
          if !ok then goto quit
          
          // Calculate non-toll matrix
          mc_n = CreateMatrixCurrency(m, trkTypes[trkType] + "n", "Rows", "Columns",)
          mc_t = CreateMatrixCurrency(m, trkTypes[trkType] + "t", "Rows", "Columns",)
          mc = CreateMatrixCurrency(m, trkTypes[trkType], "Rows", "Columns",)
          mc_n := mc - mc_t
       end
    end
    
    //return 1 if macro completed
    run_ok = 1
    Return(run_ok)
    
    quit:
      Return(ok)
    
EndMacro


/********************************************************************************************************** 
Used to gerenarte data for the forecast years from the years where data is avaialble  
Interpolates data from the closest previous year and next years
All files are from read and written to output directory

Inputs:
  output\file<prevYear>.csv
  output\file<nextYear>.csv
                                                                                                     
Outputs:
  output\file.csv

**********************************************************************************************************/
Macro "Interpolate" (arr)
    shared outputDir
    prevfile= arr[1] 
    nextfile= arr[2]
    newfile = arr[3] 
    Fyear   = arr[4]  
    prevYear= arr[5]
    nextYear= arr[6]
    
    newview = ParseString(newfile, ".")           // Get file name and use it as view name
    pview = ParseString(prevfile, ".")            // Get file name
    nview = ParseString(nextfile, ".")            // Get file name
    
    // Delete dictionary files if exist
    di = GetDirectoryInfo(outputDir+"\\"+pview[1]+".dcc", "File")
    if di.length > 0 then do
       ok=RunMacro("SDdeletefile",{outputDir+"\\"+pview[1]+".dcc"}) 
    end
    di = GetDirectoryInfo(outputDir+"\\"+nview[1]+".dcc", "File")
    if di.length > 0 then do
       ok=RunMacro("SDdeletefile",{outputDir+"\\"+nview[1]+".dcc"}) 
    end
    di = GetDirectoryInfo(outputDir+"\\"+newview[1]+".dcc", "File")
    if di.length > 0 then do
       ok=RunMacro("SDdeletefile",{outputDir+"\\"+newview[1]+".dcc"}) 
    end
    
    // Open previous year data
    prevview = Opentable("prevview", "CSV", {outputDir+"\\"+prevfile,})
    
    // Open next year data             
    nextview = Opentable("nextview", "CSV", {outputDir+"\\"+nextfile,})
    
    // Number of zones  (assuming both prev and next year contain same number of zones)                                                                                                                          
    zones = GetRecordCount(prevview, null)  
    
    // Get data table structure
    str = GetTableStructure(prevview)
    dim pfieldName[str.length],nfieldName[str.length]
    for i =1 to str.length do
       pfieldName[i] = prevview+"."+str[i][1]     // gets field names from previous year data 
       nfieldName[i] = nextview+"."+str[i][1]     // gets field names from previous year data
    end                     
                
    // Get fields from previous and next years
    prevf = GetDataVectors(prevview+"|",pfieldName,)  
    nextf = GetDataVectors(nextview+"|",nfieldName,)                                                                                                  

    // Create a Fyear table and do interpolation
    view = CreateTable(newview[1], outputDir+"\\"+ newfile, "CSV", str)                                                                                                              
    SetView(view) 
    
    // Interpolate and set values for Fyear 
    for i = 1 to zones do                        // row loop                                                                                                   
       dim v[str.length]                          // array to hold new field computation
       v[1] = {str[1][1], prevf[1][i]}            // fill new fields, first field is zone and no interpolation
                                                  
       for j = 1 to str.length do                 // field loop 
          // interpolate 
          v[j] = {str[j][1]  , (prevf[j][i]+((Fyear-prevYear)*((nextf[j][i]-prevf[j][i])/(nextYear-prevYear))))}
       end
       rh = AddRecord(view,v) 
    end

    // Close all opened views
    vws = GetViewNames()
    for p = 1 to vws.length do
       CloseView(vws[p])
    end      
EndMacro

