SET NOCOUNT ON


-- create fact schema if it does not exist
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='fact')
EXEC (N'CREATE SCHEMA [fact]')


-- create bike_flow fact table
CREATE TABLE [fact].[bike_flow] (
	[scenario_id] int NOT NULL,
	[bike_flow_id] int IDENTITY(1,1) NOT NULL,
	[bike_link_id] int NOT NULL,
	[bike_link_ab_id] int NOT NULL,
	[time_id] int NOT NULL,
	[flow] decimal(8,4) NOT NULL,
	INDEX ccsi_bikeflow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create hwy_flow fact
CREATE TABLE [fact].[hwy_flow] (
	[scenario_id] int NOT NULL,
	[hwy_flow_id] int IDENTITY(1,1) NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[time_id] int NOT NULL,
	[flow_pce] decimal(12,6) NOT NULL,
	[time] decimal(10,6) NOT NULL,
	[voc] decimal(8,6) NOT NULL,
	[v_dist_t] decimal(10,6) NOT NULL,
	[vht] decimal(11,6) NOT NULL,
	[speed] decimal(9,6) NOT NULL,
	[vdf] decimal(10,6) NOT NULL,
	[msa_flow] decimal(12,6) NOT NULL,
	[msa_time] decimal(10,6) NOT NULL,
	[flow] decimal(12,6) NOT NULL,
	INDEX ccsi_hwyflow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create hwy_flow_mode fact
CREATE TABLE [fact].[hwy_flow_mode] (
	[scenario_id] int NOT NULL,
	[hwy_flow_mode_id] int IDENTITY(1,1) NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_id] tinyint NOT NULL,
	[flow] decimal(12,6) NOT NULL,
	INDEX ccsi_hwyflowmode CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create mgra_based_input fact
CREATE TABLE [fact].[mgra_based_input] (
	[scenario_id] int NOT NULL,
	[mgra_based_input_id] int IDENTITY(1,1) NOT NULL,
	[geography_id] int NOT NULL,
	[hs] integer NOT NULL,
	[hs_sf] smallint NOT NULL,
	[hs_mf] integer NOT NULL,
	[hs_mh] smallint NOT NULL,
	[hh] integer NOT NULL,
	[hh_sf] smallint NOT NULL,
	[hh_mf] integer NOT NULL,
	[hh_mh] smallint NOT NULL,
	[gq_civ] smallint NOT NULL,
	[gq_mil] smallint NOT NULL,
	[i1] smallint NOT NULL,
	[i2] smallint NOT NULL,
	[i3] smallint NOT NULL,
	[i4] smallint NOT NULL,
	[i5] smallint NOT NULL,
	[i6] smallint NOT NULL,
	[i7] smallint NOT NULL,
	[i8] smallint NOT NULL,
	[i9] smallint NOT NULL,
	[i10] smallint NOT NULL,
	[hhs] decimal(6,4) NOT NULL,
	[pop] integer NOT NULL,
	[hhp] integer NOT NULL,
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
	[adultschenrl] decimal(12,7) NOT NULL,
    [ech_dist] int NOT NULL,
    [hch_dist] int NOT NULL,
	[pseudomsa] tinyint NOT NULL,
    [parkarea] tinyint NOT NULL,
	[hstallsoth] decimal(12,7) NOT NULL,
    [hstallssam] decimal(12,7) NOT NULL,
	[hparkcost] decimal(4,2) NOT NULL,
	[numfreehrs] tinyint NOT NULL,
	[dstallsoth] decimal(12,7) NOT NULL,
    [dstallssam] decimal(12,7) NOT NULL,
    [dparkcost] decimal(4,2) NOT NULL,
    [mstallsoth] decimal(12,7) NOT NULL,
    [mstallssam] decimal(12,7) NOT NULL,
    [mparkcost] decimal(4,2) NOT NULL,
	[totint] decimal(9,5) NOT NULL,
	[duden] decimal(8,4) NOT NULL,
    [empden] decimal(8,4) NOT NULL,
    [popden] decimal(8,4) NOT NULL,
    [retempden] decimal(8,4) NOT NULL,
	[totintbin] tinyint NOT NULL,
    [empdenbin] tinyint NOT NULL,
    [dudenbin] tinyint NOT NULL,
	[zip09] int NOT NULL,
	[parkactive] decimal(9,4) NOT NULL,
    [openspaceparkpreserve] decimal(9,4) NOT NULL,
    [beachactive] decimal(9,4) NOT NULL,
	[budgetroom] decimal(12,7) NOT NULL,
    [economyroom] decimal(12,7) NOT NULL,
    [luxuryroom] decimal(12,7) NOT NULL,
    [midpriceroom] decimal(12,7) NOT NULL,
    [upscaleroom] decimal(12,7) NOT NULL,
    [hotelroomtotal] decimal(12,7) NOT NULL,
	[truckregiontype] tinyint NOT NULL,
    [district27] tinyint NOT NULL,
    [milestocoast] decimal(7,4) NOT NULL,
    [acres] decimal(10,5) NOT NULL,
    [effective_acres] decimal(10,5) NOT NULL,
    [land_acres] decimal(10,5) NOT NULL,
	INDEX ccsi_mgrabasedinput CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create transit_aggflow fact
CREATE TABLE [fact].[transit_aggflow] (
	[scenario_id] int NOT NULL,
	[transit_aggflow_id] int IDENTITY(1,1) NOT NULL,
	[transit_link_id] int NOT NULL,
	[ab] bit NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[transit_flow] decimal(11,6) NOT NULL,
	[non_transit_flow] decimal(11,6) NOT NULL,
	[total_flow] decimal(11,6) NOT NULL,
	[access_walk_flow] decimal(11,6) NOT NULL,
	[xfer_walk_flow] decimal(11,6) NOT NULL,
	[egress_walk_flow] decimal(11,6) NOT NULL,
	INDEX ccsi_transitaggflow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create transit_flow fact
CREATE TABLE [fact].[transit_flow] (
	[scenario_id] int NOT NULL,
	[transit_flow_id] int IDENTITY(1,1) NOT NULL,
	[transit_route_id] int NOT NULL,
	[transit_stop_from_id] int NOT NULL,
	[transit_stop_to_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[from_mp] decimal(9,6) NOT NULL,
	[to_mp] decimal(9,6) NOT NULL,
	[baseivtt] decimal(9,6) NOT NULL,
	[cost] decimal(9,6) NOT NULL,
	[transit_flow] decimal(11,6) NOT NULL,
	INDEX ccsi_transitflow CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create transit_onoff fact
CREATE TABLE [fact].[transit_onoff] (
	[scenario_id] int NOT NULL,
	[transit_onoff_id] int IDENTITY(1,1) NOT NULL,
	[transit_route_id] int NOT NULL,
	[transit_stop_id] int NOT NULL,
	[time_id] int NOT NULL,
	[mode_transit_id] tinyint NOT NULL,
	[mode_transit_access_id] tinyint NOT NULL,
	[boardings] decimal(11,6) NOT NULL,
	[alightings] decimal(11,6) NOT NULL,
	[walk_access_on] decimal(11,6) NOT NULL,
	[direct_transfer_on] decimal(11,6) NOT NULL,
	[direct_transfer_off] decimal(11,6) NOT NULL,
	[egress_off] decimal(11,6) NOT NULL,
	INDEX ccsi_transitonoff CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create transit_pnr fact
CREATE TABLE [fact].[transit_pnr] (
	[scenario_id] int NOT NULL,
	[transit_pnr_id] int IDENTITY(1,1) NOT NULL,
	[transit_tap_id] int NOT NULL,
	[lot_id] smallint NOT NULL,
	[geography_id] int NOT NULL,
	[time_id] int NOT NULL,
	[parking_type] nchar(60) NOT NULL,
	[capacity] smallint NOT NULL,
	[distance] smallint NOT NULL,
	[vehicles] smallint NOT NULL,
	INDEX ccsi_transitpnr CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])



-- create person trips fact
-- partitioned clustered columnstore
CREATE TABLE [fact].[person_trip] (
	[scenario_id] int NOT NULL,
	[person_trip_id] int IDENTITY(1,1) NOT NULL,
	[person_id] int NOT NULL,
	[household_id] int NOT NULL,
	[tour_id] int NOT NULL,
	[model_trip_id] tinyint NOT NULL,
	[mode_trip_id] tinyint NOT NULL,
	[purpose_trip_origin_id] tinyint NOT NULL,
	[purpose_trip_destination_id] tinyint NOT NULL,
	[inbound_id] tinyint NOT NULL,
	[time_trip_start_id] int NOT NULL,
	[time_trip_end_id] int NOT NULL,
	[geography_trip_origin_id] int NOT NULL,
	[geography_trip_destination_id] int NOT NULL,
	[geography_parking_destination_id] int NOT NULL,
	[transit_tap_boarding_id] int NOT NULL,
	[transit_tap_alighting_id] int NOT NULL,
	[person_escort_drive_id] int NOT NULL,
	[escort_stop_type_origin_id] tinyint NOT NULL,
	[person_escort_origin_id] int NOT NULL,
	[escort_stop_type_destination_id] tinyint NOT NULL,
	[person_escort_destination_id] int NOT NULL,
	[mode_airport_arrival_id] tinyint NOT NULL,
	[time_drive] decimal(10, 4) NOT NULL,
	[dist_drive] decimal(10, 4) NOT NULL,
	[toll_cost_drive] decimal(4,2) NOT NULL,
	[operating_cost_drive] decimal(4, 2) NOT NULL,
	[time_walk] decimal(10, 4) NOT NULL,
	[dist_walk] decimal(10, 4) NOT NULL,
	[time_bike] decimal(10, 4) NOT NULL,
	[dist_bike] decimal(10, 4) NOT NULL,
	[time_transit_in_vehicle_local] decimal(10,4) NOT NULL,
	[time_transit_in_vehicle_express] decimal(10,4) NOT NULL,
	[time_transit_in_vehicle_rapid] decimal(10,4) NOT NULL,
	[time_transit_in_vehicle_light_rail] decimal(10,4) NOT NULL,
	[time_transit_in_vehicle_commuter_rail] decimal(10,4) NOT NULL,
	[time_transit_in_vehicle] decimal(10, 4) NOT NULL,
	[dist_transit_in_vehicle] decimal(10, 4) NOT NULL,
	[cost_transit] decimal(4, 2) NOT NULL,
	[time_transit_wait] decimal(10, 4) NOT NULL,
	[transit_transfers] decimal(6,4) NOT NULL,
	[time_total] decimal(10, 4) NOT NULL,
	[dist_total] decimal(10, 4) NOT NULL,
	[cost_total] decimal(4, 2) NOT NULL,
	[value_of_time] decimal(8,2) NOT NULL,
	[value_of_time_drive_bin_id] tinyint NOT NULL,
	[weight_person_trip] decimal(8, 5) NOT NULL,
	[weight_trip] decimal(8, 5) NOT NULL,
	INDEX ccsi_persontrip CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])