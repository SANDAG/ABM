-- AT_SKIMS
ALTER TABLE
	[abm].[at_skims]
WITH CHECK ADD CONSTRAINT
	fk_atskims_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	

-- BIKE_FLOW
ALTER TABLE
	[abm].[bike_flow]
WITH CHECK ADD CONSTRAINT
	fk_bikeflow_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[bike_flow]
WITH CHECK ADD CONSTRAINT
	fk_bikeflow_bikelink
FOREIGN KEY
	([scenario_id],[roadsegid])
REFERENCES
	[abm].[bike_link]
	([scenario_id],[roadsegid])
	
ALTER TABLE
	[abm].[bike_flow]
WITH CHECK ADD CONSTRAINT
	fk_bikeflow_bikelinkab
FOREIGN KEY
	([scenario_id],[roadsegid],[ab])
REFERENCES
	[abm].[bike_link_ab]
	([scenario_id],[roadsegid],[ab])
	
	
-- BIKE_LINK
ALTER TABLE
	[abm].[bike_link]
WITH CHECK ADD CONSTRAINT
	fk_bikelink_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
	
-- BIKE_LINK_AB
ALTER TABLE
	[abm].[bike_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_bikelinkab_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[bike_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_bikelinkab_bikelink
FOREIGN KEY
	([scenario_id],[roadsegid])
REFERENCES
	[abm].[bike_link]
	([scenario_id],[roadsegid])
	
	
-- CBD_VEHICLES
ALTER TABLE
	[abm].[cbd_vehicles]
WITH CHECK ADD CONSTRAINT
	fk_cbdvehicles_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[cbd_vehicles]
WITH CHECK ADD CONSTRAINT
	fk_cbdvehicles_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[zone])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
	
-- HWY_FLOW
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_hwylink
FOREIGN KEY
	([scenario_id],[hwycov_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwycov_id])
	
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_hwylinkab
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab]) 
REFERENCES
	[abm].[hwy_link_ab]
	([scenario_id],[hwycov_id],[ab])
	
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_hwylinktod
FOREIGN KEY
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id]) 
REFERENCES
	[abm].[hwy_link_tod]
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id])
	
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_hwylinkabtod
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id]) 
REFERENCES
	[abm].[hwy_link_ab_tod]
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id])
	
	
-- HWY_FLOW_MODE
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwyflow
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id])
REFERENCES
	[abm].[hwy_flow]
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id])
	
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwylink
FOREIGN KEY
	([scenario_id],[hwycov_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwycov_id])
	
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwylinkab
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab]) 
REFERENCES
	[abm].[hwy_link_ab]
	([scenario_id],[hwycov_id],[ab])
	
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwylinktod
FOREIGN KEY
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id]) 
REFERENCES
	[abm].[hwy_link_tod]
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id])
	
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwylinkabtod
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id]) 
REFERENCES
	[abm].[hwy_link_ab_tod]
	([scenario_id],[hwycov_id],[ab],[time_resolution_id],[time_period_id])
	

-- HWY_LINK
ALTER TABLE
	[abm].[hwy_link]
WITH CHECK ADD CONSTRAINT
	fk_hwylink_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	

-- HWY_LINK_AB
ALTER TABLE
	[abm].[hwy_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkab_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[hwy_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkab_hwylink
FOREIGN KEY
	([scenario_id],[hwycov_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwycov_id])
	
		
-- HWY_LINK_TOD
ALTER TABLE
	[abm].[hwy_link_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinktod_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[hwy_link_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinktod_hwylink
FOREIGN KEY
	([scenario_id],[hwycov_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwycov_id])
	

-- HWY_LINK_AB_TOD
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_hwylink
FOREIGN KEY
	([scenario_id],[hwycov_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwycov_id])
	
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_hwylinkab
FOREIGN KEY
	([scenario_id],[hwycov_id],[ab]) 
REFERENCES
	[abm].[hwy_link_ab]
	([scenario_id],[hwycov_id],[ab])
	
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_hwylinktod
FOREIGN KEY
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id]) 
REFERENCES
	[abm].[hwy_link_tod]
	([scenario_id],[hwycov_id],[time_resolution_id],[time_period_id])
	
	
-- HWY_SKIMS
ALTER TABLE
	[abm].[hwy_skims]
WITH CHECK ADD CONSTRAINT
	fk_hwyskims_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
	
-- LU_HH
ALTER TABLE
	[abm].[lu_hh]
WITH CHECK ADD CONSTRAINT
	fk_luhh_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[lu_hh]
WITH CHECK ADD CONSTRAINT
	fk_luhh_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[zone])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_luhh_zone
ON
	[abm].[lu_hh] ([scenario_id],[geography_type_id],[zone])
WITH
	(DATA_COMPRESSION = PAGE);


-- LU_MGRA_INPUT
ALTER TABLE
	[abm].[lu_mgra_input]
WITH CHECK ADD CONSTRAINT
	fk_lumgrainput_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])


-- LU_PERSON
ALTER TABLE
	[abm].[lu_person]
WITH CHECK ADD CONSTRAINT
	fk_luperson_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[lu_person]
WITH CHECK ADD CONSTRAINT
	fk_luperson_luhh
FOREIGN KEY
	([scenario_id],[hh_id])
REFERENCES
	[abm].[lu_hh]
	([scenario_id],[hh_id])
	
	
-- LU_PERSON_FP
ALTER TABLE
	[abm].[lu_person_fp]
WITH CHECK ADD CONSTRAINT
	fk_lupersonfp_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[lu_person_fp]
WITH CHECK ADD CONSTRAINT
	fk_lupersonfp_luhh
FOREIGN KEY
	([scenario_id],[hh_id])
REFERENCES
	[abm].[lu_hh]
	([scenario_id],[hh_id])
	
ALTER TABLE
	[abm].[lu_person_fp]
WITH CHECK ADD CONSTRAINT
	fk_lupersonfp_luperson
FOREIGN KEY
	([scenario_id],[hh_id],[pnum])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[hh_id],[pnum])


-- LU_PERSON_LC
ALTER TABLE
	[abm].[lu_person_lc]
WITH CHECK ADD CONSTRAINT
	fk_lupersonlc_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[lu_person_lc]
WITH CHECK ADD CONSTRAINT
	fk_lupersonlc_luhh
FOREIGN KEY
	([scenario_id],[hh_id])
REFERENCES
	[abm].[lu_hh]
	([scenario_id],[hh_id])
	
ALTER TABLE
	[abm].[lu_person_lc]
WITH CHECK ADD CONSTRAINT
	fk_lupersonlc_luperson
FOREIGN KEY
	([scenario_id],[hh_id],[pnum])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[hh_id],[pnum])
	
ALTER TABLE
	[abm].[lu_person_lc]
WITH CHECK ADD CONSTRAINT
	fk_lupersonlc_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[zone])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_lupersonlc_zone
ON
	[abm].[lu_person_lc] ([scenario_id],[geography_type_id],[zone])
WITH
	(DATA_COMPRESSION = PAGE);
	
	
-- TOUR_CB
ALTER TABLE
	[abm].[tour_cb]
WITH CHECK ADD CONSTRAINT
	fk_tourcb_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[tour_cb]
WITH CHECK ADD CONSTRAINT
	fk_tourcb_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[tour_cb]
WITH CHECK ADD CONSTRAINT
	fk_tourcb_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_tourcb_orig
ON
	[abm].[tour_cb] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tourcb_dest
ON
	[abm].[tour_cb] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
	
-- TOUR_IJ
ALTER TABLE
	[abm].[tour_ij]
WITH CHECK ADD CONSTRAINT
	fk_tourij_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[tour_ij]
WITH CHECK ADD CONSTRAINT
	fk_tourij_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[tour_ij]
WITH CHECK ADD CONSTRAINT
	fk_tourij_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_tourij_orig
ON
	[abm].[tour_ij] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tourij_dest
ON
	[abm].[tour_ij] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);

-- TOUR_IJ_PERSON
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_tourij
FOREIGN KEY
	([scenario_id],[model_type_id],[tour_id])
REFERENCES
	[abm].[tour_ij]
	([scenario_id],[model_type_id],[tour_id])
	
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_luhh
FOREIGN KEY
	([scenario_id],[hh_id])
REFERENCES
	[abm].[lu_hh]
	([scenario_id],[hh_id])
	
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_person
FOREIGN KEY
	([scenario_id],[hh_id],[pnum])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[hh_id],[pnum])
	
	
-- TOUR_VIS
ALTER TABLE
	[abm].[tour_vis]
WITH CHECK ADD CONSTRAINT
	fk_tourvis_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[tour_vis]
WITH CHECK ADD CONSTRAINT
	fk_tourvis_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[tour_vis]
WITH CHECK ADD CONSTRAINT
	fk_tourvis_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_tourvis_orig
ON
	[abm].[tour_vis] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tourvis_dest
ON
	[abm].[tour_vis] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
-- TRANSIT_AGGFLOW
ALTER TABLE
	[abm].[transit_aggflow]
WITH CHECK ADD CONSTRAINT
	fk_transitaggflow_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[transit_aggflow]
WITH CHECK ADD CONSTRAINT
	fk_transitaggflow_link
FOREIGN KEY
	([scenario_id],[trcov_id])
REFERENCES
	[abm].[transit_link]
	([scenario_id],[trcov_id])
	
	
-- TRANSIT_FLOW
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])

ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_route
FOREIGN KEY
	([scenario_id],[route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[route_id])
		
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_fromstop
FOREIGN KEY
	([scenario_id],[from_stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[stop_id])
	
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_tostop
FOREIGN KEY
	([scenario_id],[to_stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[stop_id])
	

-- TRANSIT_LINK
ALTER TABLE
	[abm].[transit_link]
WITH CHECK ADD CONSTRAINT
	fk_transitlink_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
	
-- TRANSIT_ONOFF
ALTER TABLE
	[abm].[transit_onoff]
WITH CHECK ADD CONSTRAINT
	fk_transitonoff_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])

ALTER TABLE
	[abm].[transit_onoff]
WITH CHECK ADD CONSTRAINT
	fk_transitonoff_route
FOREIGN KEY
	([scenario_id],[route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[route_id])
		
ALTER TABLE
	[abm].[transit_onoff]
WITH CHECK ADD CONSTRAINT
	fk_transitonoff_stop
FOREIGN KEY
	([scenario_id],[stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[stop_id])
	
	
-- TRANSIT_PNR
ALTER TABLE
	[abm].[transit_pnr]
WITH CHECK ADD CONSTRAINT
	fk_transitpnr_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[transit_pnr]
WITH CHECK ADD CONSTRAINT
	fk_transitpnr_tap
FOREIGN KEY
	([scenario_id],[tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
CREATE NONCLUSTERED INDEX
	ix_transitpnr_tap
ON
	[abm].[transit_pnr] ([scenario_id],[tap])
WITH
	(DATA_COMPRESSION = PAGE);
	
	
-- TRANSIT_ROUTE
ALTER TABLE
	[abm].[transit_route]
WITH CHECK ADD CONSTRAINT
	fk_transitroute_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
	
-- TRANSIT_STOP
ALTER TABLE
	[abm].[transit_stop]
WITH CHECK ADD CONSTRAINT
	fk_transitstop_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[transit_stop]
WITH CHECK ADD CONSTRAINT
	fk_transitstop_route
FOREIGN KEY
	([scenario_id],[route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[route_id])
	
ALTER TABLE
	[abm].[transit_stop]
WITH CHECK ADD CONSTRAINT
	fk_transitstop_link
FOREIGN KEY
	([scenario_id],[trcov_id])
REFERENCES
	[abm].[transit_link]
	([scenario_id],[trcov_id])


-- TRANSIT_TAP
ALTER TABLE
	[abm].[transit_tap]
WITH CHECK ADD CONSTRAINT
	fk_transittap_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
		
	
-- TRANSIT_TAP_SKIMS
ALTER TABLE
	[abm].[transit_tap_skims]
WITH CHECK ADD CONSTRAINT
	fk_transittapskims_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[transit_tap_skims]
WITH CHECK ADD CONSTRAINT
	fk_transittapskims_origtap
FOREIGN KEY
	([scenario_id],[orig_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[transit_tap_skims]
WITH CHECK ADD CONSTRAINT
	fk_transittapskims_desttap
FOREIGN KEY
	([scenario_id],[dest_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
	
-- TRANSIT_TAP_WALK
ALTER TABLE
	[abm].[transit_tap_walk]
WITH CHECK ADD CONSTRAINT
	fk_transittapwalk_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[transit_tap_walk]
WITH CHECK ADD CONSTRAINT
	fk_transittapwalk_zone_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[zone])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])

ALTER TABLE
	[abm].[transit_tap_walk]
WITH CHECK ADD CONSTRAINT
	fk_transittapwalk_tap
FOREIGN KEY
	([scenario_id],[tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
	
-- TRIP_AGG
ALTER TABLE
	[abm].[trip_agg]
WITH CHECK ADD CONSTRAINT
	fk_tripagg_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])


-- TRIP_AP
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])

ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_boardtap
FOREIGN KEY
	([scenario_id],[trip_board_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_alighttap
FOREIGN KEY
	([scenario_id],[trip_alight_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
CREATE NONCLUSTERED INDEX
	ix_tripap_orig
ON
	[abm].[trip_ap] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripap_dest
ON
	[abm].[trip_ap] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripap_boardtap
ON
	[abm].[trip_ap] ([scenario_id],[trip_board_tap])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripap_alighttap
ON
	[abm].[trip_ap] ([scenario_id],[trip_alight_tap])
WITH
	(DATA_COMPRESSION = PAGE);
	
	
-- TRIP_CB
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_tour
FOREIGN KEY
	([scenario_id],[model_type_id],[tour_id])
REFERENCES
	[abm].[tour_cb]
	([scenario_id],[model_type_id],[tour_id])

ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_boardtap
FOREIGN KEY
	([scenario_id],[trip_board_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_alighttap
FOREIGN KEY
	([scenario_id],[trip_alight_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
CREATE NONCLUSTERED INDEX
	ix_tripcb_orig
ON
	[abm].[trip_cb] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripcb_dest
ON
	[abm].[trip_cb] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripcb_boardtap
ON
	[abm].[trip_cb] ([scenario_id],[trip_board_tap])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripcb_alighttap
ON
	[abm].[trip_cb] ([scenario_id],[trip_alight_tap])
WITH
	(DATA_COMPRESSION = PAGE);

-- TRIP_IE
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])

ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_boardtap
FOREIGN KEY
	([scenario_id],[trip_board_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_alighttap
FOREIGN KEY
	([scenario_id],[trip_alight_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
CREATE NONCLUSTERED INDEX
	ix_tripie_orig
ON
	[abm].[trip_ie] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripie_dest
ON
	[abm].[trip_ie] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripie_boardtap
ON
	[abm].[trip_ie] ([scenario_id],[trip_board_tap])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripie_alighttap
ON
	[abm].[trip_ie] ([scenario_id],[trip_alight_tap])
WITH
	(DATA_COMPRESSION = PAGE);
	

-- TRIP_IJ
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_tour
FOREIGN KEY
	([scenario_id],[model_type_id],[tour_id])
REFERENCES
	[abm].[tour_ij]
	([scenario_id],[model_type_id],[tour_id])

ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_boardtap
FOREIGN KEY
	([scenario_id],[trip_board_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_alighttap
FOREIGN KEY
	([scenario_id],[trip_alight_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_parking_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[parking_zone])
REFERENCES
	[abm].[lu_mgra_input] 
	([scenario_id],[geography_type_id],[zone])
	
CREATE NONCLUSTERED INDEX
	ix_tripij_orig
ON
	[abm].[trip_ij] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripij_dest
ON
	[abm].[trip_ij] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripij_boardtap
ON
	[abm].[trip_ij] ([scenario_id],[trip_board_tap])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripij_alighttap
ON
	[abm].[trip_ij] ([scenario_id],[trip_alight_tap])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripij_parking
ON
	[abm].[trip_ij] ([scenario_id],[geography_type_id],[parking_zone])
WITH
	(DATA_COMPRESSION = PAGE);
	
	
-- TRIP_VIS
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_scenario
FOREIGN KEY
	([scenario_id])
REFERENCES
	[ref].[scenario]
	([scenario_id])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_tour
FOREIGN KEY
	([scenario_id],[model_type_id],[tour_id])
REFERENCES
	[abm].[tour_vis]
	([scenario_id],[model_type_id],[tour_id])

ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_orig_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[orig])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_dest_lumgra
FOREIGN KEY
	([scenario_id],[geography_type_id],[dest])
REFERENCES
	[abm].[lu_mgra_input]
	([scenario_id],[geography_type_id],[zone])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_boardtap
FOREIGN KEY
	([scenario_id],[trip_board_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_alighttap
FOREIGN KEY
	([scenario_id],[trip_alight_tap])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[tap])
	
CREATE NONCLUSTERED INDEX
	ix_tripvis_orig
ON
	[abm].[trip_vis] ([scenario_id],[geography_type_id],[orig])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripvis_dest
ON
	[abm].[trip_vis] ([scenario_id],[geography_type_id],[dest])
WITH
	(DATA_COMPRESSION = PAGE);
	
CREATE NONCLUSTERED INDEX
	ix_tripvis_boardtap
ON
	[abm].[trip_vis] ([scenario_id],[trip_board_tap])
WITH
	(DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripvis_alighttap
ON
	[abm].[trip_vis] ([scenario_id],[trip_alight_tap])
WITH
	(DATA_COMPRESSION = PAGE);