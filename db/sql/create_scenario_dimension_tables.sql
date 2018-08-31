SET NOCOUNT ON


-- create bike_link dimension
CREATE TABLE [dimension].[bike_link] (
	[scenario_id] int NOT NULL,
	[bike_link_id] int NOT NULL,
	[roadsegid] int NOT NULL,
	[nm] nchar(50) NOT NULL,
	[functional_class] nchar(100) NOT NULL,
	[bike2sep] tinyint NOT NULL,
	[bike3blvd] tinyint NOT NULL,
	[speed] smallint NOT NULL,
	[distance] decimal(14,9) NOT NULL,
	[scenicldx] decimal(12, 9) NOT NULL,
	[shape] geometry NOT NULL,
	CONSTRAINT pk_bikelink PRIMARY KEY ([scenario_id], [bike_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_bikelink UNIQUE ([scenario_id], [roadsegid]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_bikelink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create bike_link_ab dimension
CREATE TABLE [dimension].[bike_link_ab] (
	[scenario_id] int NOT NULL,
	[bike_link_ab_id] int NOT NULL,
	[bike_link_id] int NOT NULL,
	[roadsegid] int NOT NULL,
	[ab] tinyint NOT NULL,
	[from_node] int NOT NULL,
	[to_node] int NOT NULL,
	[gain] smallint NOT NULL,
	[bike_class] nchar(100) NOT NULL,
	[lanes] tinyint NOT NULL,
	[from_signal] bit NOT NULL,
	[to_signal] bit NOT NULL,
	CONSTRAINT pk_bikelinkab PRIMARY KEY ([scenario_id], [bike_link_ab_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_bikelinkab UNIQUE ([scenario_id], [roadsegid], [ab]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_bikelinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_bikelinkab_bikelink FOREIGN KEY ([scenario_id], [bike_link_id]) REFERENCES [dimension].[bike_link] ([scenario_id], [bike_link_id]),
	INDEX ix_bikelinkab_bikelink NONCLUSTERED ([scenario_id], [bike_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create hwy_link dimension
CREATE TABLE [dimension].[hwy_link] (
	[scenario_id] int NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwycov_id] int NOT NULL,
	[length_mile] decimal(20,16) NOT NULL,
	[sphere] smallint NOT NULL,
	[nm] nchar(50) NOT NULL,
	[cojur] tinyint NOT NULL,
	[costat] smallint NOT NULL,
	[coloc] tinyint NOT NULL,
	[rloop] smallint NOT NULL,
	[adtlk] smallint NOT NULL,
	[adtvl] smallint NOT NULL,
	[aspd] tinyint NOT NULL,
    [iyr] smallint NOT NULL,
	[iproj] smallint NOT NULL,
	[ijur] tinyint NOT NULL,
	[ifc] tinyint NOT NULL,
	[ihov] tinyint NOT NULL,
	[itruck] tinyint NOT NULL,
	[ispd] tinyint NOT NULL,
	[iway] tinyint NOT NULL,
	[imed] tinyint NOT NULL,
	[shape] geometry NOT NULL,
	CONSTRAINT pk_hwylink PRIMARY KEY ([scenario_id], [hwy_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_hwylink UNIQUE ([scenario_id], [hwycov_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_hwylink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create hwy_link_ab dimension
CREATE TABLE [dimension].[hwy_link_ab] (
	[scenario_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwycov_id] int NOT NULL,
	[ab] bit NOT NULL,
	[from_node] int NOT NULL,
	[to_node] int NOT NULL,
	[from_nm] nchar(50) NOT NULL,
	[to_nm] nchar(50) NOT NULL,
	[au] tinyint NOT NULL,
	[pct] tinyint NOT NULL,
	[cnt] tinyint NOT NULL,
	CONSTRAINT pk_hwylinkab PRIMARY KEY ([scenario_id], [hwy_link_ab_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_hwylinkab UNIQUE ([scenario_id], [hwycov_id], [ab]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_hwylinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwylinkab_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	INDEX ix_hwylinkab_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create hwy_link_tod dimension
CREATE TABLE [dimension].[hwy_link_tod] (
	[scenario_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwycov_id] int NOT NULL,
	[time_id] int NOT NULL,
	[itoll] decimal(12,6) NOT NULL,
	[itoll2] decimal(12,6) NOT NULL,
	[itoll3] decimal(12,6) NOT NULL,
	[itoll4] decimal(12,6) NOT NULL,
	[itoll5] decimal(12,6) NOT NULL,
	CONSTRAINT pk_hwylinktod PRIMARY KEY ([scenario_id], [hwy_link_tod_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_hwylinktod UNIQUE ([scenario_id], [hwycov_id], [time_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_hwylinktod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwylinktod_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_hwylinktod_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	INDEX ix_hwylinktod_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create hwy_link_ab_tod dimension
CREATE TABLE [dimension].[hwy_link_ab_tod] (
	[scenario_id] int NOT NULL,
	[hwy_link_ab_tod_id] int NOT NULL,
	[hwy_link_id] int NOT NULL,
	[hwy_link_ab_id] int NOT NULL,
	[hwy_link_tod_id] int NOT NULL,
	[hwycov_id] int NOT NULL,
	[ab] bit NOT NULL,
	[time_id] int NOT NULL,
	[cp] decimal(12,6) NOT NULL,
	[cx] decimal(12,6) NOT NULL,
	[tm] decimal(12,6) NOT NULL,
	[tx] decimal(12,6) NOT NULL,
	[ln] decimal(12,6) NOT NULL,
	[stm] decimal(12,6) NOT NULL,
	[htm] decimal(12,6) NOT NULL,
	CONSTRAINT pk_hwylinkabtod PRIMARY KEY ([scenario_id], [hwy_link_ab_tod_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_hwylinkabtod UNIQUE ([scenario_id], [hwycov_id], [ab], [time_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_hwylinkabtod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_hwylinkabtod_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
	CONSTRAINT fk_hwylinkabtod_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
	CONSTRAINT fk_hwylinkabtod_hwylinkab FOREIGN KEY ([scenario_id], [hwy_link_ab_id]) REFERENCES [dimension].[hwy_link_ab] ([scenario_id], [hwy_link_ab_id]),
	CONSTRAINT fk_hwylinkabtod_hwylinktod FOREIGN KEY ([scenario_id], [hwy_link_tod_id]) REFERENCES [dimension].[hwy_link_tod] ([scenario_id], [hwy_link_tod_id]),
	INDEX ix_hwylinkabtod_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	INDEX ix_hwylinkabtod_hwylinkab NONCLUSTERED ([scenario_id], [hwy_link_ab_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	INDEX ix_hwylinkabtod_hwylinktod NONCLUSTERED ([scenario_id], [hwy_link_tod_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create household dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[household] (
	[scenario_id] int NOT NULL,
	[household_id] int NOT NULL, -- insert NULL record as 0
	[income] int NULL,
	[income_category] nchar(20) NOT NULL,
	[household_size] nchar(20) NOT NULL,
	[household_workers] nchar(20) NOT NULL,
	[bldgsz] nchar(35) NOT NULL,
	[unittype] nchar(35) NOT NULL,
	[autos] nchar(20) NOT NULL,
	[transponder] nchar(20) NOT NULL,
	[poverty] decimal(7,4) NULL,
	[geography_household_location_id] int NOT NULL,
	[version_household] nchar(20) NOT NULL,
	[weight_household] tinyint NOT NULL,
	INDEX ccsi_household CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create person dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[person] (
	[scenario_id] int NOT NULL,
	[person_id] int NOT NULL, -- insert NULL record as 0
	[household_id] int NOT NULL, -- insert NULL record as 0
	[age] smallint NULL,
	[sex] nchar(20) NOT NULL,
	[military_status] nchar(20) NOT NULL,
	[employment_status] nchar(35) NOT NULL,
	[student_status] nchar(40) NOT NULL,
	[abm_person_type] nchar(40) NOT NULL,
	[education] nchar(45) NOT NULL,
	[grade] nchar(45) NOT NULL,
	[weeks] nchar(50) NOT NULL,
	[hours] nchar(40) NOT NULL,
	[race] nchar(130) NOT NULL,
	[hispanic] nchar(20) NOT NULL,
	[version_person] nchar(20) NOT NULL,
	[abm_activity_pattern] nchar(20) NOT NULL,
	[freeparking_choice] nchar(35) NOT NULL,
	[freeparking_reimbpct] float NULL,
	[work_segment] nchar(55) NOT NULL,
	[school_segment] nchar(20) NOT NULL,
	[geography_work_location_id] int NOT NULL,
	[geography_school_location_id] int NOT NULL,
	[work_distance] int NULL,
	[school_distance] int NULL,
	[weight_person] tinyint NOT NULL,
	INDEX ccsi_person CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create tour dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[tour] (
	[scenario_id] int NOT NULL,
	[tour_id] int IDENTITY(0,1) NOT NULL, -- insert NULL record as 0
	[model_tour_id] tinyint NOT NULL,
	[abm_tour_id] int NOT NULL,
	[time_tour_start_id] int NOT NULL,
	[time_tour_end_id] int NOT NULL,
	[geography_tour_origin_id] int NOT NULL,
	[geography_tour_destination_id] int NOT NULL,
	[mode_tour_id] tinyint NOT NULL,
	[purpose_tour_id] tinyint NOT NULL,
	[tour_category] nchar(50) NOT NULL,
	[tour_crossborder_point_of_entry] nchar(30) NOT NULL,
	[tour_crossborder_sentri] nchar(20) NOT NULL,
	[tour_visitor_auto] nchar(20) NOT NULL,
	[tour_visitor_income] nchar(15) NOT NULL,
	[weight_person_tour] decimal(4,2) NOT NULL,
	[weight_tour] smallint NOT NULL,
	INDEX ccsi_tour CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])


-- create transit_link dimension
CREATE TABLE [dimension].[transit_link] (
	[scenario_id] int NOT NULL,
	[transit_link_id] int NOT NULL,
	[trcov_id] int NOT NULL,
	[transit_link_shape] geometry NOT NULL,
	CONSTRAINT pk_transitlink PRIMARY KEY ([scenario_id], [transit_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_transitlink UNIQUE ([scenario_id], [trcov_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitlink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create transit_route dimension
CREATE TABLE [dimension].[transit_route] (
	[scenario_id] int NOT NULL,
	[transit_route_id] int NOT NULL,
	[route_id] int NOT NULL,
	[route_name] int NOT NULL,
	[mode_transit_route_id] tinyint NOT NULL,
	[am_headway] smallint NOT NULL,
	[pm_headway] smallint NOT NULL,
	[op_headway] smallint NOT NULL,
	[nt_headway] smallint NOT NULL,
	[nt_hour] tinyint NOT NULL,
	[config] int NOT NULL,
	[fare] decimal(4,2) NOT NULL,
	[transit_route_shape] geometry NOT NULL,
	CONSTRAINT pk_transitroute PRIMARY KEY ([scenario_id], [transit_route_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_transitroute UNIQUE ([scenario_id], [route_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitroute_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitroute_mode FOREIGN KEY ([mode_transit_route_id]) REFERENCES [dimension].[mode] ([mode_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create transit_stop dimension
CREATE TABLE [dimension].[transit_stop] (
	[scenario_id] int NOT NULL,
	[transit_stop_id] int NOT NULL,
	[transit_route_id] int NOT NULL,
	[transit_link_id] int NOT NULL,
	[stop_id] int NOT NULL,
	[route_id] int NOT NULL,
	[trcov_id] int NOT NULL,
	[mp] decimal(9,6) NOT NULL,
	[near_node] int NOT NULL,
	[fare_zone] smallint NOT NULL,
	[stop_name] nchar(100) NOT NULL,
	[transit_stop_shape] geometry NOT NULL,
	CONSTRAINT pk_transitstop PRIMARY KEY ([scenario_id], [transit_stop_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_transitstop UNIQUE ([scenario_id], [stop_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transitstop_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
	CONSTRAINT fk_transitstop_transitroute FOREIGN KEY ([scenario_id], [transit_route_id]) REFERENCES [dimension].[transit_route] ([scenario_id], [transit_route_id]),
	CONSTRAINT fk_transitstop_transitlink FOREIGN KEY ([scenario_id], [transit_link_id]) REFERENCES [dimension].[transit_link] ([scenario_id], [transit_link_id]),
	INDEX ix_transitstop_transitroute NONCLUSTERED ([scenario_id], [transit_route_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	INDEX ix_transitstop_transitlink NONCLUSTERED ([scenario_id], [transit_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)


-- create transit_tap dimension
CREATE TABLE [dimension].[transit_tap] (
	[scenario_id] int NOT NULL,
	[transit_tap_id] int NOT NULL,
	[tap] nchar(20) NOT NULL,
	[transit_tap_shape] geometry NULL,
	CONSTRAINT pk_transittap PRIMARY KEY ([scenario_id], [transit_tap_id]) WITH (STATISTICS_INCREMENTAL = ON),
	CONSTRAINT ixuq_transittap UNIQUE ([scenario_id], [tap]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
	CONSTRAINT fk_transittap_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
