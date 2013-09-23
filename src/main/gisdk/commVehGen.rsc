/**************************************************************
   CommVehGen.rsc
 
  TransCAD Macro used to run truck trip generation model.  The very small truck generation model is based on the Phoenix 
  four-tire truck model documented in the TMIP Quick Response Freight Manual. 
 
  Linear regression models generate trip ends, balancing attractions to productions. 
 
  Input:  (1) MGRA file in CSV format with the following fields: (a) TOTEMP, total employment (same regardless
              of classification system); (b) RETEMPN, retail trade employment per the NAICS classification system; 
              (c) FPSEMPN, financial and professional services employment per the NAICS classification system; (d) 
              HEREMPN, health, educational, and recreational employment per the NAICS classification system; (e) 
              OTHEMPN, other employment per the NAICS classification system; (f) AGREMPN, agricultural employment
              per the NAICS classificatin system; (g) MWTEMPN, manufacturing, warehousing, and transportation 
              emp;loyment per the NAICS classification system; and, (h) TOTHH, total households.
 
  Output: (1) An ASCII file containing the following fields: (a) zone number; (b) very small truck trip productions;
              (c) very small truck trip attractions
 
  See also: (1) TruckTripDistribution.job, which applies the distribution model. 
            (2) TruckTimeOfDay.job, which applies diurnal factors to the daily trips generated here. 
            (3) TruckTollChoice.job, which applies a toll/no toll choice model for trucks.
 
  version:  0.1
  authors:  dto (2010 08 31); jef (2012 03 07)
 
     	5-2013 wsu fixed indexing bug
 	7-2013 jef reduced truck rate for military employment to 0.3
 
**************************************************************/
Macro "Commercial Vehicle Generation" 

/*
    RunMacro("TCB Init")

      scenarioDirectory = "d:\\projects\\SANDAG\\AB_Model\\commercial_vehicles"
*/   
   
   shared path, inputDir, outputDir, mgraDataFile       
   
   mgraCommTripFile  = "mgraCommVeh.csv"
   tazCommTripFile   = "tazCommVeh.csv"
   
   writeMgraData = false
   calibrationFactor = 1.75
   
       
   // read in the mgra data in CSV format
   mgraView = OpenTable("MGRA View", "CSV", {inputDir+"\\"+mgraDataFile}, {{"Shared", "True"}})
   
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

   RETEMPN = emp_retail + emp_personal_svcs_retail
   FPSEMPN = emp_prof_bus_svcs 
   HEREMPN = emp_health  + emp_pvt_ed_k12 + emp_pvt_ed_post_k12_oth + emp_amusement 
   AGREMPN = emp_ag  
   MWTEMPN = emp_const_non_bldg_prod + emp_const_bldg_prod + emp_mfg_prod +  emp_trans 
   MILITARY = emp_fed_mil 
   TOTEMP  = emp_total 
   OTHEMPN = TOTEMP - (RETEMPN + FPSEMPN + HEREMPN + AGREMPN + MWTEMPN + MILITARY)
   TOTHH   = hh
   
   verySmallP = calibrationFactor * (0.95409 * RETEMPN + 0.54333 * FPSEMPN + 0.50769 * HEREMPN + 
                              0.63558 * OTHEMPN + 1.10181 * AGREMPN + 0.81576 * MWTEMPN +
                              0.30000 * MILITARY +
                              0.1 * TOTHH)
   verySmallA = verySmallP
   
   if writeMgraData = true then do
      //create a table with the mgra trips
      truckTripsMgra = CreateTable("truckTripsMgra",outputDir+"\\"+mgraCommTripFile, "CSV",  {
         {"MGRA", "Integer", 8, null, },
         {"PROD", "Real", 12, 4, },
         {"ATTR", "Real", 12, 4, }
      })
   end
       
   //create a table with the taz trips
   truckTripsTaz= CreateTable("truckTripsTaz",outputDir+"\\"+tazCommTripFile, "CSV",  {
      {"TAZ", "Integer", 8, null, },
      {"PROD", "Real", 12, 4, },
      {"ATTR", "Real", 12, 4, }
   })

   //now aggregate by TAZ
   maxTaz = 0
   for i=1 to taz.length do
      if taz[i] > maxTaz  then maxTaz = taz[i]
     end

     //arrays for holding productions and attractions by TAZ
   dim tazProd[maxTaz]
   dim tazAttr[maxTaz]
   
   //initialize arrays to 0
   for i = 1 to tazProd.length do
      tazProd[i]=0
      tazAttr[i]=0
   end
   
   //aggregate mgra to taz arrays
   for i = 1 to mgra.length do
       
       tazNumber = taz[i]
       tazProd[tazNumber] =  tazProd[tazNumber] + verySmallP[i]
       tazAttr[tazNumber] =  tazAttr[tazNumber] + verySmallA[i]
   
      if writeMgraData = true then do
         AddRecord("truckTripsMgra", {
             {"MGRA", mgra[i]},
             {"PROD", verySmallP[i]},
               {"ATTR", verySmallA[i]}
        })
        end   
     end
       
     if writeMgraData = true then CloseView(truckTripsMgra)

   //add taz data to table
   for i = 1 to maxTaz do
         
         AddRecord("truckTripsTaz", {
              {"TAZ", i},
              {"PROD", tazProd[i]},
                {"ATTR", tazAttr[i]}
         })
      end   

   RunMacro("close all")    
    Return(1)
EndMacro    
