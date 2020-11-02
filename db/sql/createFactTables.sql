SET NOCOUNT ON;


-- create fact schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='fact')
BEGIN
    EXECUTE (N'CREATE SCHEMA [fact]')
    EXECUTE [db_meta].[add_xp] 'fact', 'MS_Description', 'schema to hold and manage ABM fact tables'
END


-- create hwy_flow fact
CREATE TABLE [fact].[hwy_flow] (
    [scenario_id] int NOT NULL,
    [hwy_flow_id] int IDENTITY(1,1) NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwy_link_ab_tod_id] int NOT NULL,
    [time_id] int NOT NULL,
    [flow_pce] float NOT NULL,
    [time] float NOT NULL,
    [voc] float NOT NULL,
    [v_dist_t] float NOT NULL,
    [vht] float NOT NULL,
    [speed] float NOT NULL,
    [vdf] float NOT NULL,
    [msa_flow] float NOT NULL,
    [msa_time] float NOT NULL,
    [flow] float NOT NULL,
    INDEX ccsi_hwyFlow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.hwy_flow', 'MS_Description', 'loaded highway network by hwycov_id, ab, and time period'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.hwy_flow_id', 'MS_Description', 'hwy_flow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.hwy_link_ab_id', 'MS_Description', 'hwy_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.hwy_link_tod_id', 'MS_Description', 'hwy_link_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.hwy_link_ab_tod_id', 'MS_Description', 'hwy_link_ab_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.flow_pce', 'MS_Description', 'volume of passenger car equivalent'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.time', 'MS_Description', 'loaded travel time'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.voc', 'MS_Description', 'volume to capacity ratio'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.v_dist_t', 'MS_Description', 'vehicle distance'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.vht', 'MS_Description', 'vehicle hours'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.speed', 'MS_Description', 'speed calculated as (length/time)*60'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.vdf', 'MS_Description', 'loaded cost (result from link performance function)'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.msa_flow', 'MS_Description', 'calculated msa volume'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.msa_time', 'MS_Description', 'calculated msa time'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow.flow', 'MS_Description', 'total volume'
GO


-- create hwy_flow_mode fact
CREATE TABLE [fact].[hwy_flow_mode] (
    [scenario_id] int NOT NULL,
    [hwy_flow_mode_id] int IDENTITY(1,1),
    [hwy_link_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwy_link_ab_tod_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_id] int NOT NULL,
    [value_of_time_category_id] int NOT NULL,
    transponder_available_id int NOT NULL,
    [flow] float NOT NULL,
    INDEX ccsi_hwyFlowMode CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode', 'MS_Description', 'loaded highway network by hwycov_id, ab, time period, and mode'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_flow_mode_id', 'MS_Description', 'hwy_flow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_ab_id', 'MS_Description', 'hwy_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_tod_id', 'MS_Description', 'hwy_link_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_ab_tod_id', 'MS_Description', 'hwy_link_ab_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.mode_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.value_of_time_category_id', 'MS_Description', 'ABM assignment class value of time category surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.transponder_available_id', 'MS_Description', 'transponder availability surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.flow', 'MS_Description', 'total volume'
GO


-- create mgra_based_input fact
CREATE TABLE [fact].[mgra_based_input] (
    [scenario_id] int NOT NULL,
    [mgra_based_input_id] int NOT NULL,
    [geography_id] int NOT NULL,
    [hs] int NOT NULL,
    [hs_sf] int NOT NULL,
    [hs_mf] int NOT NULL,
    [hs_mh] int NOT NULL,
    [hh] int NOT NULL,
    [hh_sf] int NOT NULL,
    [hh_mf] int NOT NULL,
    [hh_mh] int NOT NULL,
    [gq_civ] int NOT NULL,
    [gq_mil] int NOT NULL,
    [i1] int NOT NULL,
    [i2] int NOT NULL,
    [i3] int NOT NULL,
    [i4] int NOT NULL,
    [i5] int NOT NULL,
    [i6] int NOT NULL,
    [i7] int NOT NULL,
    [i8] int NOT NULL,
    [i9] int NOT NULL,
    [i10] int NOT NULL,
    [hhs] float NOT NULL,
    [pop] int NOT NULL,
    [hhp] int NOT NULL,
    [emp_ag] int NOT NULL,
    [emp_const_non_bldg_prod] int NOT NULL,
    [emp_const_non_bldg_office] int NOT NULL,
    [emp_utilities_prod] int NOT NULL,
    [emp_utilities_office] int NOT NULL,
    [emp_const_bldg_prod] int NOT NULL,
    [emp_const_bldg_office] int NOT NULL,
    [emp_mfg_prod] int NOT NULL,
    [emp_mfg_office] int NOT NULL,
    [emp_whsle_whs] int NOT NULL,
    [emp_trans] int NOT NULL,
    [emp_retail] int NOT NULL,
    [emp_prof_bus_svcs] int NOT NULL,
    [emp_prof_bus_svcs_bldg_maint] int NOT NULL,
    [emp_pvt_ed_k12] int NOT NULL,
    [emp_pvt_ed_post_k12_oth] int NOT NULL,
    [emp_health] int NOT NULL,
    [emp_personal_svcs_office] int NOT NULL,
    [emp_amusement] int NOT NULL,
    [emp_hotel] int NOT NULL,
    [emp_restaurant_bar] int NOT NULL,
    [emp_personal_svcs_retail] int NOT NULL,
    [emp_religious] int NOT NULL,
    [emp_pvt_hh] int NOT NULL,
    [emp_state_local_gov_ent] int NOT NULL,
    [emp_fed_non_mil] int NOT NULL,
    [emp_fed_mil] int NOT NULL,
    [emp_state_local_gov_blue] int NOT NULL,
    [emp_state_local_gov_white] int NOT NULL,
    [emp_public_ed] int NOT NULL,
    [emp_own_occ_dwell_mgmt] int NOT NULL,
    [emp_fed_gov_accts] int NOT NULL,
    [emp_st_lcl_gov_accts] int NOT NULL,
    [emp_cap_accts] int NOT NULL,
    [emp_total] int NOT NULL,
    [enrollgradekto8] int NOT NULL,
    [enrollgrade9to12] int NOT NULL,
    [collegeenroll] int NOT NULL,
    [othercollegeenroll] int NOT NULL,
    [adultschenrl] float NOT NULL,
    [ech_dist] int NOT NULL,
    [hch_dist] int NOT NULL,
    [pseudomsa] int NOT NULL,
    [parkarea] int NOT NULL,
    [hstallsoth] float NOT NULL,
    [hstallssam] float NOT NULL,
    [hparkcost] float NOT NULL,
    [numfreehrs] int NOT NULL,
    [dstallsoth] float NOT NULL,
    [dstallssam] float NOT NULL,
    [dparkcost] float NOT NULL,
    [mstallsoth] float NOT NULL,
    [mstallssam] float NOT NULL,
    [mparkcost] float NOT NULL,
    [zip09] int NOT NULL,
    [parkactive] float NOT NULL,
    [openspaceparkpreserve] float NOT NULL,
    [beachactive] float NOT NULL,
    [hotelroomtotal] float NOT NULL,
    [truckregiontype] int NOT NULL,
    [district27] int NOT NULL,
    [milestocoast] float NOT NULL,
    [acres] float NOT NULL,
    [effective_acres] float NOT NULL,
    [land_acres] float NOT NULL,
    [MicroAccessTime] int NOT NULL,
    [remoteAVParking] int NOT NULL,
    [refueling_stations] int NOT NULL,
    [totint] float NOT NULL,
    [duden] float NOT NULL,
    [empden] float NOT NULL,
    [popden] float NOT NULL,
    [retempden] float NOT NULL,
    [totintbin] int NOT NULL,
    [empdenbin] int NOT NULL,
    [dudenbin] int NOT NULL,
    [PopEmpDenPerMi] float NOT NULL,
    INDEX ccsi_mgraBasedInput CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input', 'MS_Description', 'mgra based input file'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.mgra_based_input_id', 'MS_Description', 'mgra_based_input surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.geography_id', 'MS_Description', 'geography surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hs', 'MS_Description', 'housing structures'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hs_sf', 'MS_Description', 'single family structures'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hs_mf', 'MS_Description', 'multi family structures'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hs_mh', 'MS_Description', 'mobile homes'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hh', 'MS_Description', 'households'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hh_sf', 'MS_Description', 'single family households'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hh_mf', 'MS_Description', 'multi family households'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hh_mh', 'MS_Description', 'mobile home households'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.gq_civ', 'MS_Description', 'civilian group quarters'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.gq_mil', 'MS_Description', 'military group quarters'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i1', 'MS_Description', 'households income group 1'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i2', 'MS_Description', 'households income group 2'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i3', 'MS_Description', 'households income group 3'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i4', 'MS_Description', 'households income group 4'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i5', 'MS_Description', 'households income group 5'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i6', 'MS_Description', 'households income group 6'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i7', 'MS_Description', 'households income group 7'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i8', 'MS_Description', 'households income group 8'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i9', 'MS_Description', 'households income group 9'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.i10', 'MS_Description', 'households income group 10'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hhs', 'MS_Description', 'average household size'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.pop', 'MS_Description', 'population'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hhp', 'MS_Description', 'household population'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_ag', 'MS_Description', 'employment, agriculture'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_const_non_bldg_prod', 'MS_Description', 'employment, construction production'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_const_non_bldg_office', 'MS_Description', 'employment, construction support'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_utilities_prod', 'MS_Description', 'employment, utilities production'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_utilities_office', 'MS_Description', 'employment, utilities office'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_mfg_prod', 'MS_Description', 'employment, manufacturing production'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_mfg_office', 'MS_Description', 'employment, manufacturing office'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_whsle_whs', 'MS_Description', 'employment, wholesale and warehousing'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_trans', 'MS_Description', 'employment, transportation activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_retail', 'MS_Description', 'employment, retail activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_prof_bus_svcs', 'MS_Description', 'employment, professional and business services'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_prof_bus_svcs_bldg_maint', 'MS_Description', 'employment, professional and business building maintenance'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_pvt_ed_k12', 'MS_Description', 'employment, private education elementary k-12'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_pvt_ed_post_k12_oth', 'MS_Description', 'employment, private education post secondary'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_health', 'MS_Description', 'employment, health services'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_personal_svcs_office', 'MS_Description', 'employment, personal services office based'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_amusement', 'MS_Description', 'employment, amusement services'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_hotel', 'MS_Description', 'employment, hotels activity (479,480)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_restaurant_bar', 'MS_Description', 'employment, restaurants and bars'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_personal_svcs_retail', 'MS_Description', 'employment, personal services retail based'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_religious', 'MS_Description', 'employment, religious activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_pvt_hh', 'MS_Description', 'employment, private households'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_state_local_gov_ent', 'MS_Description', 'employment, state and local government enterprises activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_fed_non_mil', 'MS_Description', 'employment, federal non-military activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_fed_mil', 'MS_Description', 'employment, federal military activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_state_local_gov_blue', 'MS_Description', 'employment, state and local government blue collar'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_state_local_gov_white', 'MS_Description', 'employment, state and local government white collar'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_public_ed', 'MS_Description', 'employment, public education (k-12)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_own_occ_dwell_mgmt', 'MS_Description', 'employment, owner-occupied dwellings management and maintenance activity'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_fed_gov_accts', 'MS_Description', 'employment, federal government accounts'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_st_lcl_gov_accts', 'MS_Description', 'employment, state and local government accounts'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_cap_accts', 'MS_Description', 'employment, capital accounts'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.emp_total', 'MS_Description', 'employment, total'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.enrollgradekto8', 'MS_Description', 'elementary and middle school enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.enrollgrade9to12', 'MS_Description', 'high school enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.collegeenroll', 'MS_Description', 'university enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.othercollegeenroll', 'MS_Description', 'other college enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.adultschenrl', 'MS_Description', 'adult school enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.ech_dist', 'MS_Description', 'grade school enrollment'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hch_dist', 'MS_Description', 'high school district'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.pseudomsa', 'MS_Description', 'pseudo msa'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.parkarea', 'MS_Description', 'park area type'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hstallsoth', 'MS_Description', 'number of hourly stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hstallssam', 'MS_Description', 'number of hourly stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hparkcost', 'MS_Description', 'hourly parking cost, cents'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.numfreehrs', 'MS_Description', 'number of free parking hours available'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.dstallsoth', 'MS_Description', 'number of daily stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.dstallssam', 'MS_Description', 'number of daily stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.dparkcost', 'MS_Description', 'daily parking cost, cents'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.mstallsoth', 'MS_Description', 'number of monthly stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.mstallssam', 'MS_Description', 'number of monthly stalls'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.mparkcost', 'MS_Description', 'monthly parking cost, cents'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.zip09', 'MS_Description', 'zip code'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.parkactive', 'MS_Description', 'park acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.openspaceparkpreserve', 'MS_Description', 'open space acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.beachactive', 'MS_Description', 'beach acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hotelroomtotal', 'MS_Description', 'total hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.truckregiontype', 'MS_Description', 'truck region type'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.district27', 'MS_Description', 'district 27 indicator'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.milestocoast', 'MS_Description', 'miles to the coast'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.acres', 'MS_Description', 'acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.effective_acres', 'MS_Description', 'acres of developable land (or available for development)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.land_acres', 'MS_Description', 'acres of land area excluding water such as bay, lagoon, lake, reservoir, or large pond'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.MicroAccessTime', 'MS_Description', 'micro-mobility access time (mins)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.remoteAVParking', 'MS_Description', 'indicator if remote av parking available'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.refueling_stations', 'MS_Description', 'number of refueling stations'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.totint', 'MS_Description', 'intersection count in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.duden', 'MS_Description', 'dwelling unit density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.empden', 'MS_Description', 'employment density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.popden', 'MS_Description', 'population density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.retempden', 'MS_Description', 'retail employment density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.totintbin', 'MS_Description', 'totint bin (0-80,90-130,130+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.empdenbin', 'MS_Description', 'empden bin (0-10,11-30,30+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.dudenbin', 'MS_Description', 'duden bin (0-5,5-10,10+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.PopEmpDenPerMi', 'MS_Description', 'population and employment density per mile'
GO


-- create transit_aggflow fact
CREATE TABLE [fact].[transit_aggflow] (
    [scenario_id] int NOT NULL,
    [transit_aggflow_id] int IDENTITY(1,1) NOT NULL,
    [transit_link_id] int NOT NULL,
    [ab] bit NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [transit_flow] float NOT NULL,
    [non_transit_flow] float NOT NULL,
    [total_flow] float NOT NULL,
    [access_walk_flow] float NOT NULL,
    [xfer_walk_flow] float NOT NULL,
    [egress_walk_flow] float NOT NULL,
    INDEX ccsi_transitAggflow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow', 'MS_Description', 'link based transit flow by mode, access mode, ab, and time period'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.transit_aggflow_id', 'MS_Description', 'transit_aggflow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.transit_link_id', 'MS_Description', 'transit_link surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.ab', 'MS_Description', 'link ab indicator'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.mode_transit_id', 'MS_Description', 'transit mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.mode_transit_access_id', 'MS_Description', 'transit access mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.transit_flow', 'MS_Description', 'total transit flow'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.non_transit_flow', 'MS_Description', 'total non transit flow.'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.total_flow', 'MS_Description', 'total flow'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.access_walk_flow', 'MS_Description', 'total walk access flow'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.xfer_walk_flow', 'MS_Description', 'total transfer flow'
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow.egress_walk_flow', 'MS_Description', 'total egress flow'
GO


-- create transit_flow fact
CREATE TABLE [fact].[transit_flow] (
    [scenario_id] int NOT NULL,
    [transit_flow_id] int IDENTITY(1,1) NOT NULL,
    [transit_route_id] int NOT NULL,
    [transit_stop_from_id] int NOT NULL,
    [transit_stop_to_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [from_mp] float NOT NULL,
    [to_mp] float NOT NULL,
    [baseivtt] float NOT NULL,
    [cost] float NOT NULL,
    [transit_flow] float NOT NULL,
    INDEX ccsi_transitFlow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.transit_flow', 'MS_Description', 'route and stop based transit flow by mode, access mode, and time period'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.transit_flow_id', 'MS_Description', 'transit_flow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.transit_route_id', 'MS_Description', 'transit_route surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.transit_stop_from_id', 'MS_Description', 'from transit_stop surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.transit_stop_to_id', 'MS_Description', 'to transit_stop surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.time_id', 'MS_Description', 'time period surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.mode_transit_id', 'MS_Description', 'transit mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.mode_transit_access_id', 'MS_Description', 'transit access surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.from_mp', 'MS_Description', 'from milepost'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.to_mp', 'MS_Description', 'to milepost'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.baseivtt', 'MS_Description', 'base in-vehicle time for transit in minutes'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.cost', 'MS_Description', 'link cost in dollars'
EXECUTE [db_meta].[add_xp] 'fact.transit_flow.transit_flow', 'MS_Description', 'total transit flow'
GO


-- create transit_onoff fact
CREATE TABLE [fact].[transit_onoff] (
    [scenario_id] int NOT NULL,
    [transit_onoff_id] int IDENTITY(1,1) NOT NULL,
    [transit_route_id] int NOT NULL,
    [transit_stop_id] int NOT NULL,
    [time_id] int NOT NULL,
    [mode_transit_id] int NOT NULL,
    [mode_transit_access_id] int NOT NULL,
    [boardings] float NOT NULL,
    [alightings] float NOT NULL,
    [walk_access_on] float NOT NULL,
    [direct_transfer_on] float NOT NULL,
    [direct_transfer_off] float NOT NULL,
    [egress_off] float NOT NULL,
    INDEX ccsi_transitOnoff CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.transit_onoff', 'MS_Description', 'route and stop based on and off movements by mode, access mode, and time period'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.transit_onoff_id', 'MS_Description', 'transit_onoff surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.transit_route_id', 'MS_Description', 'transit_route surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.transit_stop_id', 'MS_Description', 'transit_stop surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.mode_transit_id', 'MS_Description', 'transit mode identifer'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.mode_transit_access_id', 'MS_Description', 'transit access mode identifer'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.boardings', 'MS_Description', 'boardings'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.alightings', 'MS_Description', 'alightings'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.walk_access_on', 'MS_Description', 'walk access boardings'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.direct_transfer_on', 'MS_Description', 'direct access boardings'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.direct_transfer_off', 'MS_Description', 'direct transfer alightings'
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff.egress_off', 'MS_Description', 'egress alightings'
GO


-- create transit_pnr fact
CREATE TABLE [fact].[transit_pnr] (
    [scenario_id] int NOT NULL,
    [transit_pnr_id] int NOT NULL,
    [transit_tap_id] int NOT NULL,
    [lot_id] int NOT NULL,
    [geography_id] int NOT NULL,
    [time_id] int NOT NULL,
    [parking_type] nvarchar(60) NOT NULL,
    [capacity] int NOT NULL,
    [distance] float NOT NULL,
    [vehicles] float NOT NULL,
    INDEX ccsi_transitPnr CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.transit_pnr', 'MS_Description', 'park and ride tap lot details and vehicles'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.transit_pnr_id', 'MS_Description', 'transit_pnr surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.transit_tap_id', 'MS_Description', 'transit_tap surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.lot_id', 'MS_Description', 'lot id in tap.ptype input file'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.geography_id', 'MS_Description', 'geography surrogate key where pnr lot is located'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.parking_type', 'MS_Description', 'parking type'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.capacity', 'MS_Description', 'number of stalls in the parking lot'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.distance', 'MS_Description', 'distance from lot to transit access point in miles'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.vehicles', 'MS_Description', 'number of vehicles parked in lot'
GO


-- create person trips fact
-- partitioned clustered columnstore
CREATE TABLE [fact].[person_trip] (
    [scenario_id] int NOT NULL,
    [person_trip_id] int IDENTITY(1,1) NOT NULL,
    [model_trip_id] int NOT NULL,
    [abm_trip_id] int NOT NULL,
    [tour_id] int NOT NULL,
    [person_id] int NOT NULL,
    [household_id] int NOT NULL,
    [mode_trip_id] int NOT NULL,
    [purpose_trip_origin_id] int NOT NULL,
    [purpose_trip_destination_id] int NOT NULL,
    [inbound_id] int NOT NULL,
    [time_trip_start_id] int NOT NULL,
    [time_trip_end_id] int NOT NULL,
    [geography_trip_origin_id] int NOT NULL,
    [geography_trip_destination_id] int NOT NULL,
    [geography_parking_destination_id] int NOT NULL,
    [transit_tap_boarding_id] int NOT NULL,
    [transit_tap_alighting_id] int NOT NULL,
    [mode_airport_arrival_id] int NOT NULL,
    [value_of_time_category_id] int NOT NULL,
    [transponder_available_id] int NOT NULL,
    [av_used_id] int NOT NULL,
    [weight_trip] float NOT NULL,
    [weight_person_trip] float NOT NULL,
    [cost_parking] float NOT NULL,
    [time_drive] float NOT NULL,
    [distance_drive] float NOT NULL,
    [cost_toll_drive] float NOT NULL,
    [cost_operating_drive] float NOT NULL,
    [cost_fare_drive] float NOT NULL,
    [time_wait_drive] float NOT NULL,
    [time_transit_in_vehicle] float NOT NULL,
    [time_tier1_transit_in_vehicle] float NOT NULL,
    [time_freeway_rapid_transit_in_vehicle] float NOT NULL,
    [time_arterial_rapid_transit_in_vehicle] float NOT NULL,
    [time_express_bus_transit_in_vehicle] float NOT NULL,
    [time_local_bus_transit_in_vehicle] float NOT NULL,
    [time_light_rail_transit_in_vehicle] float NOT NULL,
    [time_commuter_rail_transit_in_vehicle] float NOT NULL,
    [time_transit_initial_wait] float NOT NULL,
    [time_transit_wait] float NOT NULL,
    [distance_transit_in_vehicle] float NOT NULL,
    [cost_fare_transit] float NOT NULL,
    [transfers_transit] float NOT NULL,
    [time_walk] float NOT NULL,
    [distance_walk] float NOT NULL,
    [time_mm] float NOT NULL,
    [distance_mm] float NOT NULL,
    [cost_fare_mm] float NOT NULL,
    [time_mt] float NOT NULL,
    [distance_mt] float NOT NULL,
    [cost_fare_mt] float NOT NULL,
    [time_bike] float NOT NULL,
    [distance_bike] float NOT NULL,
    [time_total] float NOT NULL,
    [distance_total] float NOT NULL,
    [cost_total] float NOT NULL,
    INDEX ccsi_personTrip CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'fact.person_trip', 'MS_Description', 'person trip fact table'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_trip_id', 'MS_Description', 'person_trip surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.model_trip_id', 'MS_Description', 'ABM sub-model surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.abm_trip_id', 'MS_Description', 'ABM trip surrogate key within ABM sub-model'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.tour_id', 'MS_Description', 'tour surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_id', 'MS_Description', 'person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.mode_trip_id', 'MS_Description', 'trip mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.purpose_trip_origin_id', 'MS_Description', 'trip origin purpose surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.purpose_trip_destination_id', 'MS_Description', 'trip destination purpose surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.inbound_id', 'MS_Description', 'inbound indicator surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_trip_start_id', 'MS_Description', 'trip start time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_trip_end_id', 'MS_Description', 'trip end time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.geography_trip_origin_id', 'MS_Description', 'trip origin geography surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.geography_trip_destination_id', 'MS_Description', 'trip destination geography surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.geography_parking_destination_id', 'MS_Description', 'trip destination parking geography surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.transit_tap_boarding_id', 'MS_Description', 'boarding transit tap surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.transit_tap_alighting_id', 'MS_Description', 'alighting transit tap surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.mode_airport_arrival_id', 'MS_Description', 'airport model arrival mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.value_of_time_category_id', 'MS_Description', 'trip value of time category surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.transponder_available_id', 'MS_Description', 'transponder availability surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.av_used_id', 'MS_Description', 'autonomous vehicle used surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.weight_trip', 'MS_Description', 'weight to use if measuring number of trips'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.weight_person_trip', 'MS_Description', 'weight to use if measuring number of person trips'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_parking', 'MS_Description', 'trip parking cost in dollars'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_drive', 'MS_Description', 'trip drive time in minutes - includes auto portion of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_drive', 'MS_Description', 'trip drive distance in miles - includes auto portion of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_toll_drive', 'MS_Description', 'trip drive toll cost in dollars - includes auto portion of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_operating_drive', 'MS_Description', 'trip auto operating cost in dollars - includes auto portion of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_fare_drive', 'MS_Description', 'auto trip fare cost in dollars - includes auto portion of of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_wait_drive', 'MS_Description', 'auto trip wait time in minutes - includes auto portion of of non-auto trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle', 'MS_Description', 'transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_tier1_transit_in_vehicle', 'MS_Description', 'tier1 transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_freeway_rapid_transit_in_vehicle', 'MS_Description', 'freeway rapid transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_arterial_rapid_transit_in_vehicle', 'MS_Description', 'arterial rapid transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_express_bus_transit_in_vehicle', 'MS_Description', 'express bus transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_local_bus_transit_in_vehicle', 'MS_Description', 'local bus transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_light_rail_transit_in_vehicle', 'MS_Description', 'light rail transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_commuter_rail_transit_in_vehicle', 'MS_Description', 'commuter rail transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_initial_wait', 'MS_Description', 'initial transit wait time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_wait', 'MS_Description', 'first transfer transit wait time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_transit_in_vehicle', 'MS_Description', 'transit in vehicle distance in miles'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_fare_transit', 'MS_Description', 'transit fare cost in dollars'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.transfers_transit', 'MS_Description', 'number of transit transfers'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_walk', 'MS_Description', 'trip walk time in minutes - includes walk portion of non-walk trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_walk', 'MS_Description', 'trip walk distance in miles - includes walk portion of non-walk trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_mm', 'MS_Description', 'trip micro-mobility time in minutes - includes micro-mobility portion of non-micro-mobility trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_mm', 'MS_Description', 'trip micro-mobility distance in miles - includes micro-mobility portion of non-micro-mobility trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_fare_mm', 'MS_Description', 'trip micro-mobility fare cost in dollars - includes micro-mobility portion of non-micro-mobility trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_mt', 'MS_Description', 'trip micro-transit time in minutes - includes micro-transit portion of non-micro-transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_mt', 'MS_Description', 'trip micro-transit distance in miles - includes micro-transit portion of non-micro-transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_fare_mt', 'MS_Description', 'trip micro-transit fare cost in dollars - includes micro-transit portion of non-micro-transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_bike', 'MS_Description', 'trip bicycle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_bike', 'MS_Description', 'trip bicycle distance in miles'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_total', 'MS_Description', 'total trip time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.distance_total', 'MS_Description', 'total trip distance in miles'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_total', 'MS_Description', 'total trip cost in dollars - includes operating cost'
GO
