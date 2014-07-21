Macro "US to SD External Trip Model"

	shared path, inputDir, outputDir, mxext  

  controlTotals = "externalInternalControlTotals.csv"
  
  controlTotalsView = OpenTable("Control Totals", "CSV", {inputDir+"\\"+controlTotals}, {{"Shared", "True"}})
  mgraView = OpenTable("MGRA View", "CSV",{inputDir+"\\mgra13_based_input${year}.csv"}, {{"Shared", "True"}})
  
  eaDanMatrix = OpenMatrix(outputDir+"\\"+"impdan_EA.mtx", )
  eaDanMC = CreateMatrixCurrencies(eaDanMatrix,,,) 
  eaDatMatrix = OpenMatrix(outputDir+"\\"+"impdat_EA.mtx", )
  eaDatMC = CreateMatrixCurrencies(eaDatMatrix,,,) 
  eaS2nhMatrix = OpenMatrix(outputDir+"\\"+"imps2nh_EA.mtx", )
  eaS2nhMC = CreateMatrixCurrencies(eaS2nhMatrix,,,) 
  eaS2thMatrix = OpenMatrix(outputDir+"\\"+"imps2th_EA.mtx", )
  eaS2thMC = CreateMatrixCurrencies(eaS2thMatrix,,,)
  eaS3nhMatrix = OpenMatrix(outputDir+"\\"+"imps3nh_EA.mtx", )
  eaS3nhMC = CreateMatrixCurrencies(eaS3nhMatrix,,,) 
  eaS3thMatrix = OpenMatrix(outputDir+"\\"+"imps3th_EA.mtx", )
  eaS3thMC = CreateMatrixCurrencies(eaS3thMatrix,,,)
  
  amDanMatrix = OpenMatrix(outputDir+"\\"+"impdan_AM.mtx", )
  amDanMC = CreateMatrixCurrencies(amDanMatrix,,,) 
  amDatMatrix = OpenMatrix(outputDir+"\\"+"impdat_AM.mtx", )
  amDatMC = CreateMatrixCurrencies(amDatMatrix,,,) 
  amS2nhMatrix = OpenMatrix(outputDir+"\\"+"imps2nh_AM.mtx", )
  amS2nhMC = CreateMatrixCurrencies(amS2nhMatrix,,,) 
  amS2thMatrix = OpenMatrix(outputDir+"\\"+"imps2th_AM.mtx", )
  amS2thMC = CreateMatrixCurrencies(amS2thMatrix,,,)
  amS3nhMatrix = OpenMatrix(outputDir+"\\"+"imps3nh_AM.mtx", )
  amS3nhMC = CreateMatrixCurrencies(amS3nhMatrix,,,) 
  amS3thMatrix = OpenMatrix(outputDir+"\\"+"imps3th_AM.mtx", )
  amS3thMC = CreateMatrixCurrencies(amS3thMatrix,,,)
  
  mdDanMatrix = OpenMatrix(outputDir+"\\"+"impdan_MD.mtx", )
  mdDanMC = CreateMatrixCurrencies(mdDanMatrix,,,) 
  mdDatMatrix = OpenMatrix(outputDir+"\\"+"impdat_MD.mtx", )
  mdDatMC = CreateMatrixCurrencies(mdDatMatrix,,,) 
  mdS2nhMatrix = OpenMatrix(outputDir+"\\"+"imps2nh_MD.mtx", )
  mdS2nhMC = CreateMatrixCurrencies(mdS2nhMatrix,,,) 
  mdS2thMatrix = OpenMatrix(outputDir+"\\"+"imps2th_MD.mtx", )
  mdS2thMC = CreateMatrixCurrencies(mdS2thMatrix,,,)
  mdS3nhMatrix = OpenMatrix(outputDir+"\\"+"imps3nh_MD.mtx", )
  mdS3nhMC = CreateMatrixCurrencies(mdS3nhMatrix,,,) 
  mdS3thMatrix = OpenMatrix(outputDir+"\\"+"imps3th_MD.mtx", )
  mdS3thMC = CreateMatrixCurrencies(mdS3thMatrix,,,)
  
  pmDanMatrix = OpenMatrix(outputDir+"\\"+"impdan_PM.mtx", )
  pmDanMC = CreateMatrixCurrencies(pmDanMatrix,,,) 
  pmDatMatrix = OpenMatrix(outputDir+"\\"+"impdat_PM.mtx", )
  pmDatMC = CreateMatrixCurrencies(pmDatMatrix,,,) 
  pmS2nhMatrix = OpenMatrix(outputDir+"\\"+"imps2nh_PM.mtx", )
  pmS2nhMC = CreateMatrixCurrencies(pmS2nhMatrix,,,) 
  pmS2thMatrix = OpenMatrix(outputDir+"\\"+"imps2th_PM.mtx", )
  pmS2thMC = CreateMatrixCurrencies(pmS2thMatrix,,,)
  pmS3nhMatrix = OpenMatrix(outputDir+"\\"+"imps3nh_PM.mtx", )
  pmS3nhMC = CreateMatrixCurrencies(pmS3nhMatrix,,,) 
  pmS3thMatrix = OpenMatrix(outputDir+"\\"+"imps3th_PM.mtx", )
  pmS3thMC = CreateMatrixCurrencies(pmS3thMatrix,,,)
  
  evDanMatrix = OpenMatrix(outputDir+"\\"+"impdan_EV.mtx", )
  evDanMC = CreateMatrixCurrencies(evDanMatrix,,,) 
  evDatMatrix = OpenMatrix(outputDir+"\\"+"impdat_EV.mtx", )
  evDatMC = CreateMatrixCurrencies(evDatMatrix,,,) 
  evS2nhMatrix = OpenMatrix(outputDir+"\\"+"imps2nh_EV.mtx", )
  evS2nhMC = CreateMatrixCurrencies(evS2nhMatrix,,,) 
  evS2thMatrix = OpenMatrix(outputDir+"\\"+"imps2th_EV.mtx", )
  evS2thMC = CreateMatrixCurrencies(evS2thMatrix,,,)
  evS3nhMatrix = OpenMatrix(outputDir+"\\"+"imps3nh_EV.mtx", )
  evS3nhMC = CreateMatrixCurrencies(evS3nhMatrix,,,) 
  evS3thMatrix = OpenMatrix(outputDir+"\\"+"imps3th_EV.mtx", )
  evS3thMC = CreateMatrixCurrencies(evS3thMatrix,,,)
  
  freeTimeCurrencies = {eaDanMC.("*STM_EA (Skim)"),eaS2nhMC.("*HTM_EA (Skim)"),eaS3nhMC.("*HTM_EA (Skim)"),
  											amDanMC.("*STM_AM (Skim)"),amS2nhMC.("*HTM_AM (Skim)"),amS3nhMC.("*HTM_AM (Skim)"),
  											mdDanMC.("*STM_MD (Skim)"),mdS2nhMC.("*HTM_MD (Skim)"),mdS3nhMC.("*HTM_MD (Skim)"),
  											pmDanMC.("*STM_PM (Skim)"),pmS2nhMC.("*HTM_PM (Skim)"),pmS3nhMC.("*HTM_PM (Skim)"),
  											evDanMC.("*STM_EV (Skim)"),evS2nhMC.("*HTM_EV (Skim)"),evS3nhMC.("*HTM_EV (Skim)")}
  											
  tollTimeCurrencies = {eaDatMC.("*STM_EA (Skim)"),eaS2thMC.("*HTM_EA (Skim)"),eaS3thMC.("*HTM_EA (Skim)"),
  											amDatMC.("*STM_AM (Skim)"),amS2thMC.("*HTM_AM (Skim)"),amS3thMC.("*HTM_AM (Skim)"),
  											mdDatMC.("*STM_MD (Skim)"),mdS2thMC.("*HTM_MD (Skim)"),mdS3thMC.("*HTM_MD (Skim)"),
  											pmDatMC.("*STM_PM (Skim)"),pmS2thMC.("*HTM_PM (Skim)"),pmS3thMC.("*HTM_PM (Skim)"),
  											evDatMC.("*STM_EV (Skim)"),evS2thMC.("*HTM_EV (Skim)"),evS3thMC.("*HTM_EV (Skim)")}
  											                                                                      
  											                                                                      //mod(eaS3thMC.("s3th_EA - itoll_EA"),10000),
  											                                                                      
  tollCostCurrencies = {eaDatMC.("dat_EA - itoll_EA"),eaS2thMC.("s2t_EA - itoll_EA"),eaS3thMC.("s3t_EA - itoll_EA"),
  											amDatMC.("dat_AM - itoll_AM"),amS2thMC.("s2t_AM - itoll_AM"),amS3thMC.("s3t_AM - itoll_AM"),
  											mdDatMC.("dat_MD - itoll_MD"),mdS2thMC.("s2t_MD - itoll_MD"),mdS3thMC.("s3t_MD - itoll_MD"),
  											pmDatMC.("dat_PM - itoll_PM"),pmS2thMC.("s2t_PM - itoll_PM"),pmS3thMC.("s3t_PM - itoll_PM"),
  											evDatMC.("dat_EV - itoll_EV"),evS2thMC.("s2t_EV - itoll_EV"),evS3thMC.("s3t_EV - itoll_EV")}									
  
  controlTaz = GetDataVector(controlTotalsView+"|", "taz", {{"Sort Order", {{"taz", "Ascending"}}}} )
  controlWrk  = GetDataVector(controlTotalsView+"|", "work", {{"Sort Order", {{"taz", "Ascending"}}}} )
  controlNon 	= GetDataVector(controlTotalsView+"|", "nonwork", {{"Sort Order", {{"taz", "Ascending"}}}} )
  
  // create mgra size vectrors
  
  mgraView = OpenTable("MGRA View", "CSV", {inputDir+"\\mgra13_based_input${year}.csv"}, {{"Shared", "True"}})
   
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
  emp_public_ed                   = GetDataVector(mgraView+"|", "emp_public_ed", {{"Sort Order", {{"mgra", "Ascending"}}}} )  

  emp_blu = emp_const_non_bldg_prod + emp_const_non_bldg_office + emp_utilities_prod + emp_utilities_office + emp_const_bldg_prod + emp_const_bldg_office + emp_mfg_prod + emp_mfg_office + emp_whsle_whs + emp_trans
  emp_svc = emp_prof_bus_svcs + emp_prof_bus_svcs_bldg_maint + emp_personal_svcs_office + emp_personal_svcs_retail
  emp_edu = emp_pvt_ed_k12 + emp_pvt_ed_post_k12_oth + emp_public_ed
  emp_gov = emp_state_local_gov_ent + emp_fed_non_mil + emp_fed_non_mil + emp_state_local_gov_blue + emp_state_local_gov_white
  emp_ent = emp_amusement + emp_hotel + emp_restaurant_bar
  emp_oth = emp_religious + emp_pvt_hh + emp_fed_mil
  
  wrk_size_mgra = 			(	emp_blu +
  					1.364 * emp_retail +
  					4.264 * emp_ent +
  					0.781 * emp_svc +
  					1.403 * emp_edu +
  					1.779 * emp_health +
  					0.819 * emp_gov +
  					0.708 * emp_oth )
  					
  non_size_mgra =			(	hh +
  					1.069 * emp_blu +
  					4.001 * emp_retail +
  					6.274 * emp_ent +
  					0.901 * emp_svc +
  					1.129 * emp_edu +
  					2.754 * emp_health +
  					1.407 * emp_gov +
  					0.304 * emp_oth )
  
  // aggregate to TAZ
  
  rowLabels = GetMatrixRowLabels(mdDatMC.("Length (Skim)"))
  numZones = rowLabels.length
  dim wrkSizeTaz[numZones]
  dim nonSizeTaz[numZones]

   for i=1 to numZones do
  	wrkSizeTaz[i] = 0
  	nonSizeTaz[i] = 0
  end	
  
  for i=1 to mgra.length do
  	tazNumber = taz[i]
  	wrkSizeTaz[tazNumber] = wrkSizeTaz[tazNumber] + wrk_size_mgra[i]
  	nonSizeTaz[tazNumber] = nonSizeTaz[tazNumber] + non_size_mgra[i]
  end
  
  wrkSizeVector = ArrayToVector(wrkSizeTaz,{{"Row Based","False"},{"Type","Float"}} )
  nonSizeVector = ArrayToVector(nonSizeTaz,{{"Row Based","False"},{"Type","Float"}})
  
  
  // Initialize matrices
  
  opts = {}
  opts.Label = "Trips"
  opts.Type = "Double"
  opts.Tables = {'Trips'}
  
  opts.[File Name] = outputDir+"\\"+"usSdWrkPA.mtx"
  wrkMatrixPA = CreateMatrixFromScratch("wrkMatrixPA",numZones,numZones,opts)
  wrkCurrenPA = CreateMatrixCurrency(wrkMatrixPA,'Trips',,,)
  
  opts.[File Name] = outputDir+"\\"+"usSdNonPA.mtx"
  nonMatrixPA = CreateMatrixFromScratch("nonMatrixPA",numZones,numZones,opts)
  nonCurrenPA = CreateMatrixCurrency(nonMatrixPA,'Trips',,,)
  
  //create exponentiated distance impedance matrices
   opts.Label = "Prob"
   opts.Type = "Double"
   opts.Tables = {'Prob'}
   
   opts.[File Name] = outputDir+"\\"+"wrkProb.mtx"
   wrkProbMatrix = CreateMatrixFromScratch("wrkProb",numZones,numZones,opts)
   wrkProb = CreateMatrixCurrency(wrkProbMatrix,'Prob',,,)
   
   opts.[File Name] = outputDir+"\\"+"nonProb.mtx"
   nonProbMatrix = CreateMatrixFromScratch("nonProb",numZones,numZones,opts)
   nonProb = CreateMatrixCurrency(nonProbMatrix,'Prob',,,)

   wrkDistCoef = -0.029
   nonDistCoef = -0.006
  
   wrkProb := wrkSizeVector * exp( wrkDistCoef * mdDatMC.("Length (Skim)"))
   nonProb := nonSizeVector * exp( nonDistCoef * mdDatMC.("Length (Skim)"))
   	
   wrkSumVector = GetMatrixVector(wrkProb, {{"Marginal", "Row Sum"}})
   wrkProb := wrkProb/wrkSumVector

   nonSumVector  = GetMatrixVector(nonProb, {{"Marginal", "Row Sum"}})
   nonProb := nonProb/nonSumVector
  
  wrkCurrenPA := 0
  nonCurrenPA := 0
  	
  // Loop over external zones and set values in output matrix
  for i=1 to controlTaz.length do
  	
    wrkTotal = controlWrk[i]
    nonTotal = controlNon[i]
    
    wrkTripVector = wrkTotal * GetMatrixVector(wrkProb,{{"Row", controlTaz[i]}}) 
    nonTripVector = nonTotal * GetMatrixVector(wrkProb,{{"Row", controlTaz[i]}})
    	
    SetMatrixVector(wrkCurrenPA, wrkTripVector, {{"Row",controlTaz[i]}})
    SetMatrixVector(nonCurrenPA, nonTripVector, {{"Row",controlTaz[i]}})
  
  end
               
  // Convert PA to OD and Apply Diurnal Factors
   opts.Label = "Trips"
   opts.Type = "Float"
   opts.Tables = {'Trips'}

  opts.[File Name] = outputDir+"\\"+"usSdWrkDaily.mtx"
	wrkMatrixAP = TransposeMatrix(wrkMatrixPA,opts)
	wrkCurrenAP = CreateMatrixCurrency(wrkMatrixAP,'Trips',,,)
	
	opts.[File Name] = outputDir+"\\"+"usSdNonDaily.mtx"
	nonMatrixAP = TransposeMatrix(nonMatrixPA,opts)
	nonCurrenAP = CreateMatrixCurrency(nonMatrixAP,'Trips',,,)
	
	wrkCurrenPA := 0.5 * wrkCurrenPA
	nonCurrenPA := 0.5 * nonCurrenPA
	wrkCurrenAP := 0.5 * wrkCurrenAP
	nonCurrenAP := 0.5 * nonCurrenAP
    
  // Apply Occupancy and Diurnal Factors
  
  opts.Tables = {"DAN","S2N","S3N","DAT","S2T","S3T"}
  
  opts.[File Name] = outputDir+"\\"+"usSdWrk_EA.mtx"
  wrkMatrixEA = CreateMatrixFromScratch("wrkMatrixEA",numZones,numZones,opts)
  wrkCurrenEA = CreateMatrixCurrencies(wrkMatrixEA,,,)
  opts.[File Name] = outputDir+"\\"+"usSdWrk_AM.mtx"
  wrkMatrixAM = CreateMatrixFromScratch("wrkMatrixAM",numZones,numZones,opts)
  wrkCurrenAM = CreateMatrixCurrencies(wrkMatrixAM,,,)
  opts.[File Name] = outputDir+"\\"+"usSdWrk_MD.mtx"
  wrkMatrixMD = CreateMatrixFromScratch("wrkMatrixMD",numZones,numZones,opts)
  wrkCurrenMD = CreateMatrixCurrencies(wrkMatrixMD,,,)
  opts.[File Name] = outputDir+"\\"+"usSdWrk_PM.mtx"
  wrkMatrixPM = CreateMatrixFromScratch("wrkMatrixPM",numZones,numZones,opts)
  wrkCurrenPM = CreateMatrixCurrencies(wrkMatrixPM,,,)
  opts.[File Name] = outputDir+"\\"+"usSdWrk_EV.mtx"
  wrkMatrixEV = CreateMatrixFromScratch("wrkMatrixEV",numZones,numZones,opts)
  wrkCurrenEV = CreateMatrixCurrencies(wrkMatrixEV,,,)
  
  opts.[File Name] = outputDir+"\\"+"usSdNon_EA.mtx"
  nonMatrixEA = CreateMatrixFromScratch("nonMatrixEA",numZones,numZones,opts)
  nonCurrenEA = CreateMatrixCurrencies(nonMatrixEA,,,)
  opts.[File Name] = outputDir+"\\"+"usSdNon_AM.mtx"
  nonMatrixAM = CreateMatrixFromScratch("nonMatrixAM",numZones,numZones,opts)
  nonCurrenAM = CreateMatrixCurrencies(nonMatrixAM,,,)
  opts.[File Name] = outputDir+"\\"+"usSdNon_MD.mtx"
  nonMatrixMD = CreateMatrixFromScratch("nonMatrixMD",numZones,numZones,opts)
  nonCurrenMD = CreateMatrixCurrencies(nonMatrixMD,,,)
  opts.[File Name] = outputDir+"\\"+"usSdNon_PM.mtx"
  nonMatrixPM = CreateMatrixFromScratch("nonMatrixPM",numZones,numZones,opts)
  nonCurrenPM = CreateMatrixCurrencies(nonMatrixPM,,,)
  opts.[File Name] = outputDir+"\\"+"usSdNon_EV.mtx"
  nonMatrixEV = CreateMatrixFromScratch("nonMatrixEV",numZones,numZones,opts)
  nonCurrenEV = CreateMatrixCurrencies(nonMatrixEV,,,)
  
  wrkCurrenAll = {wrkCurrenEA,wrkCurrenAM,wrkCurrenMD,wrkCurrenPM,wrkCurrenEV}
  nonCurrenAll = {nonCurrenEA,nonCurrenAM,nonCurrenMD,nonCurrenPM,nonCurrenEV}
  
  wrkDiurnalPA = {0.26,0.26,0.41,0.06,0.02}
  wrkDiurnalAP = {0.08,0.07,0.41,0.42,0.02}
  
  nonDiurnalPA = {0.25,0.39,0.30,0.04,0.02}
  nonDiurnalAP = {0.12,0.11,0.37,0.38,0.02}
  
  wrkOccupancy = {0.58,0.31,0.11}
  nonOccupancy = {0.55,0.29,0.15}
  
  matrixNames = {"DAN","S2N","S3N","DAT","S2T","S3T"}
  
  for periodIdx=1 to 5 do
  	for occupIdx = 1 to 3 do
  	
  		wrkCurrenAll[periodIdx].(matrixNames[occupIdx]) := wrkOccupancy[occupIdx] * ( wrkDiurnalPA[periodIdx] * wrkCurrenPA + wrkDiurnalAP[periodIdx] * wrkCurrenAP )
  		nonCurrenAll[periodIdx].(matrixNames[occupIdx]) := nonOccupancy[occupIdx] * ( nonDiurnalPA[periodIdx] * nonCurrenPA + nonDiurnalAP[periodIdx] * nonCurrenAP )
 		
 		end
 	end 
  
  // Toll choice split
   
  // values of time is cents per minute (toll cost is in cents)
  votWork			= 15.00 //  $9.00/hr
  votNonwork	= 22.86 // $13.70/hr
  ivtCoef			= -0.03
  
  for periodIdx=1 to 5 do
  	for occupIdx = 1 to 3 do
  	
  	  currIndex = (periodIdx - 1) * 3 + occupIdx
  	  
  	  //wrkProb is work toll probability
  	  wrkProb := exp(ivtCoef * ( tollTimeCurrencies[ currIndex] - freeTimeCurrencies[ currIndex ] + mod(tollCostCurrencies[ currIndex ],10000) / votWork  ) - 3.39) 
  	  wrkProb := if tollCostCurrencies[ currIndex ] > 0 then wrkProb else 0
  	  wrkProb := wrkProb / ( 1 + wrkProb )     
  	  	
   	  
  	  wrkCurrenAll[periodIdx].(matrixNames[occupIdx+3]) := wrkCurrenAll[periodIdx].(matrixNames[occupIdx]) * wrkProb
  	  wrkCurrenAll[periodIdx].(matrixNames[occupIdx])	  := wrkCurrenAll[periodIdx].(matrixNames[occupIdx])	* (1.0 - wrkProb)	
  	  
  	  //nonProb is non-work toll probability
  	  nonProb := exp(ivtCoef * ( tollTimeCurrencies[ currIndex ] - freeTimeCurrencies[ currIndex] + mod(tollCostCurrencies[ currIndex],10000) / votNonwork )- 3.39) 
  	  nonProb := if tollCostCurrencies[ currIndex ] > 0 then nonProb else 0 
  	  nonProb := nonProb / ( 1 + nonProb )
  	  nonCurrenAll[periodIdx].(matrixNames[occupIdx+3]) := nonCurrenAll[periodIdx].(matrixNames[occupIdx]) * nonProb
  	  nonCurrenAll[periodIdx].(matrixNames[occupIdx])	  := nonCurrenAll[periodIdx].(matrixNames[occupIdx])	* (1.0 - nonProb)	
  	  
 		end
 	end
 	
   RunMacro("close all")    
   Return(1)
 	quit:
 		Return(0)
EndMacro