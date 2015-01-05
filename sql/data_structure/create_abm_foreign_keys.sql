-- BIKE_FLOW
ALTER TABLE
	[abm].[bike_flow]
WITH CHECK ADD CONSTRAINT
	fk_bikeflow_bikelinkab
FOREIGN KEY
	([scenario_id],[bike_link_ab_id])
REFERENCES
	[abm].[bike_link_ab]
	([scenario_id],[bike_link_ab_id])

	
-- BIKE_LINK_AB
ALTER TABLE
	[abm].[bike_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_bikelinkab_bikelink
FOREIGN KEY
	([scenario_id],[bike_link_id])
REFERENCES
	[abm].[bike_link]
	([scenario_id],[bike_link_id])
	
	
-- HWY_FLOW
ALTER TABLE
	[abm].[hwy_flow]
WITH CHECK ADD CONSTRAINT
	fk_hwyflow_hwylinkabtod
FOREIGN KEY
	([scenario_id],[hwy_link_ab_tod_id]) 
REFERENCES
	[abm].[hwy_link_ab_tod]
	([scenario_id],[hwy_link_ab_tod_id])
	
	
-- HWY_FLOW_MODE
ALTER TABLE
	[abm].[hwy_flow_mode]
WITH CHECK ADD CONSTRAINT
	fk_hwyflowmode_hwyflow
FOREIGN KEY
	([scenario_id],[hwy_flow_id])
REFERENCES
	[abm].[hwy_flow]
	([scenario_id],[hwy_flow_id])
	

-- HWY_LINK_AB
ALTER TABLE
	[abm].[hwy_link_ab]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkab_hwylink
FOREIGN KEY
	([scenario_id],[hwy_link_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwy_link_id])
	

-- HWY_LINK_AB_TOD
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_hwylinkab
FOREIGN KEY
	([scenario_id],[hwy_link_ab_id]) 
REFERENCES
	[abm].[hwy_link_ab]
	([scenario_id],[hwy_link_ab_id])
	
ALTER TABLE
	[abm].[hwy_link_ab_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinkabtod_hwylinktod
FOREIGN KEY
	([scenario_id],[hwy_link_tod_id]) 
REFERENCES
	[abm].[hwy_link_tod]
	([scenario_id],[hwy_link_tod_id])


-- HWY_LINK_TOD
ALTER TABLE
	[abm].[hwy_link_tod]
WITH CHECK ADD CONSTRAINT
	fk_hwylinktod_hwylink
FOREIGN KEY
	([scenario_id],[hwy_link_id]) 
REFERENCES
	[abm].[hwy_link]
	([scenario_id],[hwy_link_id])


-- LU_PERSON
ALTER TABLE
	[abm].[lu_person]
WITH CHECK ADD CONSTRAINT
	fk_luperson_luhh
FOREIGN KEY
	([scenario_id],[lu_hh_id])
REFERENCES
	[abm].[lu_hh]
	([scenario_id],[lu_hh_id])
	
	
-- LU_PERSON_FP
ALTER TABLE
	[abm].[lu_person_fp]
WITH CHECK ADD CONSTRAINT
	fk_lupersonfp_luperson
FOREIGN KEY
	([scenario_id],[lu_person_id])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[lu_person_id])


-- LU_PERSON_LC
ALTER TABLE
	[abm].[lu_person_lc]
WITH CHECK ADD CONSTRAINT
	fk_lupersonlc_luperson
FOREIGN KEY
	([scenario_id],[lu_person_id])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[lu_person_id])


-- TOUR_IJ_PERSON
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_tourij
FOREIGN KEY
	([scenario_id],[tour_ij_id])
REFERENCES
	[abm].[tour_ij]
	([scenario_id],[tour_ij_id])
	
ALTER TABLE
	[abm].[tour_ij_person]
WITH CHECK ADD CONSTRAINT
	fk_tourijperson_person
FOREIGN KEY
	([scenario_id],[lu_person_id])
REFERENCES
	[abm].[lu_person]
	([scenario_id],[lu_person_id])
	
	
-- TRANSIT_AGGFLOW
ALTER TABLE
	[abm].[transit_aggflow]
WITH CHECK ADD CONSTRAINT
	fk_transitaggflow_link
FOREIGN KEY
	([scenario_id],[transit_link_id])
REFERENCES
	[abm].[transit_link]
	([scenario_id],[transit_link_id])
	
	
-- TRANSIT_FLOW
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_route
FOREIGN KEY
	([scenario_id],[transit_route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[transit_route_id])
		
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_fromstop
FOREIGN KEY
	([scenario_id],[from_transit_stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[transit_stop_id])
	
ALTER TABLE
	[abm].[transit_flow]
WITH CHECK ADD CONSTRAINT
	fk_transitflow_tostop
FOREIGN KEY
	([scenario_id],[to_transit_stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[transit_stop_id])
	
	
-- TRANSIT_ONOFF
ALTER TABLE
	[abm].[transit_onoff]
WITH CHECK ADD CONSTRAINT
	fk_transitonoff_route
FOREIGN KEY
	([scenario_id],[transit_route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[transit_route_id])
		
ALTER TABLE
	[abm].[transit_onoff]
WITH CHECK ADD CONSTRAINT
	fk_transitonoff_stop
FOREIGN KEY
	([scenario_id],[transit_stop_id])
REFERENCES
	[abm].[transit_stop]
	([scenario_id],[transit_stop_id])
	
	
-- TRANSIT_PNR
ALTER TABLE
	[abm].[transit_pnr]
WITH CHECK ADD CONSTRAINT
	fk_transitpnr_tap
FOREIGN KEY
	([scenario_id],[transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])

	
-- TRANSIT_STOP
ALTER TABLE
	[abm].[transit_stop]
WITH CHECK ADD CONSTRAINT
	fk_transitstop_route
FOREIGN KEY
	([scenario_id],[transit_route_id])
REFERENCES
	[abm].[transit_route]
	([scenario_id],[transit_route_id])
	
ALTER TABLE
	[abm].[transit_stop]
WITH CHECK ADD CONSTRAINT
	fk_transitstop_link
FOREIGN KEY
	([scenario_id],[transit_link_id])
REFERENCES
	[abm].[transit_link]
	([scenario_id],[transit_link_id])

CREATE NONCLUSTERED INDEX
	ix_transitstop_route
ON
	[abm].[transit_stop] ([scenario_id],[transit_route_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_transitstop_link
ON
	[abm].[transit_stop] ([scenario_id],[transit_link_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);
		
	
-- TRANSIT_TAP_SKIMS
ALTER TABLE
	[abm].[transit_tap_skims]
WITH CHECK ADD CONSTRAINT
	fk_transittapskims_origtap
FOREIGN KEY
	([scenario_id],[orig_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[transit_tap_skims]
WITH CHECK ADD CONSTRAINT
	fk_transittapskims_desttap
FOREIGN KEY
	([scenario_id],[dest_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
	
-- TRANSIT_TAP_WALK
ALTER TABLE
	[abm].[transit_tap_walk]
WITH CHECK ADD CONSTRAINT
	fk_transittapwalk_tap
FOREIGN KEY
	([scenario_id],[transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])


-- TRIP_AP
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_boardtap
FOREIGN KEY
	([scenario_id],[board_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[trip_ap]
WITH CHECK ADD CONSTRAINT
	fk_tripap_alighttap
FOREIGN KEY
	([scenario_id],[alight_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
CREATE NONCLUSTERED INDEX
	ix_tripap_boardtap
ON
	[abm].[trip_ap] ([scenario_id],[board_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripap_alighttap
ON
	[abm].[trip_ap] ([scenario_id],[alight_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);
	
	
-- TRIP_CB
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_tour
FOREIGN KEY
	([scenario_id],[tour_cb_id])
REFERENCES
	[abm].[tour_cb]
	([scenario_id],[tour_cb_id])

ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_boardtap
FOREIGN KEY
	([scenario_id],[board_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[trip_cb]
WITH CHECK ADD CONSTRAINT
	fk_tripcb_alighttap
FOREIGN KEY
	([scenario_id],[alight_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
CREATE NONCLUSTERED INDEX
	ix_tripcb_boardtap
ON
	[abm].[trip_cb] ([scenario_id],[board_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripcb_alighttap
ON
	[abm].[trip_cb] ([scenario_id],[alight_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);


-- TRIP_IE
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_boardtap
FOREIGN KEY
	([scenario_id],[board_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[trip_ie]
WITH CHECK ADD CONSTRAINT
	fk_tripie_alighttap
FOREIGN KEY
	([scenario_id],[alight_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
CREATE NONCLUSTERED INDEX
	ix_tripie_boardtap
ON
	[abm].[trip_ie] ([scenario_id],[board_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripie_alighttap
ON
	[abm].[trip_ie] ([scenario_id],[alight_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);
	

-- TRIP_IJ
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_tour
FOREIGN KEY
	([scenario_id],[tour_ij_id])
REFERENCES
	[abm].[tour_ij]
	([scenario_id],[tour_ij_id])

ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_boardtap
FOREIGN KEY
	([scenario_id],[board_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[trip_ij]
WITH CHECK ADD CONSTRAINT
	fk_tripij_alighttap
FOREIGN KEY
	([scenario_id],[alight_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
CREATE NONCLUSTERED INDEX
	ix_tripij_boardtap
ON
	[abm].[trip_ij] ([scenario_id],[board_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripij_alighttap
ON
	[abm].[trip_ij] ([scenario_id],[alight_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);
	
	
-- TRIP_VIS
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_tour
FOREIGN KEY
	([scenario_id],[tour_vis_id])
REFERENCES
	[abm].[tour_vis]
	([scenario_id],[tour_vis_id])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_boardtap
FOREIGN KEY
	([scenario_id],[board_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
ALTER TABLE
	[abm].[trip_vis]
WITH CHECK ADD CONSTRAINT
	fk_tripvis_alighttap
FOREIGN KEY
	([scenario_id],[alight_transit_tap_id])
REFERENCES
	[abm].[transit_tap]
	([scenario_id],[transit_tap_id])
	
CREATE NONCLUSTERED INDEX
	ix_tripvis_boardtap
ON
	[abm].[trip_vis] ([scenario_id],[board_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);

CREATE NONCLUSTERED INDEX
	ix_tripvis_alighttap
ON
	[abm].[trip_vis] ([scenario_id],[alight_transit_tap_id])
WITH
	(STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE);