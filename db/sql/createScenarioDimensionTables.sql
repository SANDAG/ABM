SET NOCOUNT ON;


-- create hwy_link dimension
CREATE TABLE [dimension].[hwy_link] (
    [scenario_id] int NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwycov_id] int NOT NULL,
    [length_mile] float NOT NULL,
    [sphere] int NOT NULL,
    [nm] nvarchar(50) NOT NULL,
    [cojur] int NOT NULL,
    [costat] int NOT NULL,
    [coloc] int NOT NULL,
    [rloop] int NOT NULL,
    [adtlk] int NOT NULL,
    [adtvl] int NOT NULL,
    [aspd] int NOT NULL,
    [iyr] int NOT NULL,
    [iproj] int NOT NULL,
    [ijur] int NOT NULL,
    [ifc] int NOT NULL,
    [ihov] int NOT NULL,
    [itruck] int NOT NULL,
    [ispd] int NOT NULL,
    [iway] int NOT NULL,
    [imed] int NOT NULL,
    [shape] geometry NOT NULL,
    CONSTRAINT pk_hwylink PRIMARY KEY ([scenario_id], [hwy_link_id]) WITH (STATISTICS_INCREMENTAL = ON),
    CONSTRAINT ixuq_hwylink UNIQUE ([scenario_id], [hwycov_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
    CONSTRAINT fk_hwylink_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
GO

EXECUTE [db_meta].[add_xp] 'dimension.hwy_link', 'MS_Description', 'input highway network'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.hwycov_id', 'MS_Description', 'highway network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.length_mile', 'MS_Description', 'length of link in miles'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.sphere', 'MS_Description', 'jurisdiction sphere of influence'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.nm', 'MS_Description', 'street name'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.cojur', 'MS_Description', 'count jurisdiction code'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.costat', 'MS_Description', 'count station number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.coloc', 'MS_Description', 'indicator if count was taken on this link'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.rloop', 'MS_Description', 'freeway count station number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.adtlk', 'MS_Description', 'adt link number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.adtvl', 'MS_Description', 'observed base year ground count (in hundreds)'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.aspd', 'MS_Description', 'adjusted link speed'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.iyr', 'MS_Description', 'year the link opened to traffic'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.iproj', 'MS_Description', 'project number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.ijur', 'MS_Description', 'link jurisdiction type'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.ifc', 'MS_Description', 'initial functional classification'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.ihov', 'MS_Description', 'link operation type'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.itruck', 'MS_Description', 'truck restriction code'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.ispd', 'MS_Description', 'initial posted speed'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.iway', 'MS_Description', 'initial one or two-way operation'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.imed', 'MS_Description', 'initial median condition'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link.shape', 'MS_Description', 'highway link linestring geometry'
GO


-- create hwy_link_ab dimension
CREATE TABLE [dimension].[hwy_link_ab] (
    [scenario_id] int NOT NULL,
    [hwy_link_ab_id] int NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwycov_id] int NOT NULL,
    [ab] bit NOT NULL,
    [from_node] int NOT NULL,
    [to_node] int NOT NULL,
    [from_nm] nvarchar(50) NOT NULL,
    [to_nm] nvarchar(50) NOT NULL,
    [au] int NOT NULL,
    [pct] int NOT NULL,
    [cnt] int NOT NULL,
    CONSTRAINT pk_hwylinkab PRIMARY KEY ([scenario_id], [hwy_link_ab_id]) WITH (STATISTICS_INCREMENTAL = ON),
    CONSTRAINT ixuq_hwylinkab UNIQUE ([scenario_id], [hwycov_id], [ab]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
    CONSTRAINT fk_hwylinkab_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
    CONSTRAINT fk_hwylinkab_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
    INDEX ix_hwylinkab_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
GO

EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab', 'MS_Description', 'ab specific attributes of input highway network'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.hwy_link_ab_id', 'MS_Description', 'hwy_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.hwycov_id', 'MS_Description', 'highway network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.ab', 'MS_Description', 'link ab direction indicator'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.from_node', 'MS_Description', 'from node number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.to_node', 'MS_Description', 'to node number'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.from_nm', 'MS_Description', 'cross street name at the from end of the link'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.to_nm', 'MS_Description', 'cross street name at the to end of the link'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.au', 'MS_Description', 'initial auxilary lanes in the from-to direction'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.pct', 'MS_Description', 'directional split'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab.cnt', 'MS_Description', 'initial intersection control type at to end'
GO


-- create hwy_link_tod dimension
CREATE TABLE [dimension].[hwy_link_tod] (
    [scenario_id] int NOT NULL,
    [hwy_link_tod_id] int NOT NULL,
    [hwy_link_id] int NOT NULL,
    [hwycov_id] int NOT NULL,
    [time_id] int NOT NULL,
    [itoll] float NOT NULL,
    [itoll2] float NOT NULL,
    [itoll3] float NOT NULL,
    [itoll4] float NOT NULL,
    [itoll5] float NOT NULL,
    CONSTRAINT pk_hwylinktod PRIMARY KEY ([scenario_id], [hwy_link_tod_id]) WITH (STATISTICS_INCREMENTAL = ON),
    CONSTRAINT ixuq_hwylinktod UNIQUE ([scenario_id], [hwycov_id], [time_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
    CONSTRAINT fk_hwylinktod_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
    CONSTRAINT fk_hwylinktod_time FOREIGN KEY ([time_id]) REFERENCES [dimension].[time] ([time_id]),
    CONSTRAINT fk_hwylinktod_hwylink FOREIGN KEY ([scenario_id], [hwy_link_id]) REFERENCES [dimension].[hwy_link] ([scenario_id], [hwy_link_id]),
    INDEX ix_hwylinktod_hwylink NONCLUSTERED ([scenario_id], [hwy_link_id])  WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
GO

-- [dimension].[hwy_link_tod]
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod', 'SUBSYSTEM', 'highway network'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod', 'MS_Description', 'time period specific attributes of input highway network'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.hwy_link_tod_id', 'MS_Description', 'hwy_link_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.hwycov_id', 'MS_Description', 'highway network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.itoll', 'MS_Description', 'toll + 100*[01] if managed lane'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.itoll2', 'MS_Description', 'link toll cost in cents'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.itoll3', 'MS_Description', 'link toll cost plus auto operating cost'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.itoll4', 'MS_Description', 'link toll cost plus 1.03*auto operating cost applies to lhd and mhd trucks only'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_tod.itoll5', 'MS_Description', 'link toll cost plus 2.33*auto operating cost applies to hhd trucks only'
GO


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
    [cp] float NOT NULL,
    [cx] float NOT NULL,
    [tm] float NOT NULL,
    [tx] float NOT NULL,
    [ln] float NOT NULL,
    [stm] float NULL,  -- allow NULLs
    [htm] float NULL,  -- allow NULLs
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
GO

EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod', 'MS_Description', 'ab and time period specific attributes of input highway network'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.hwy_link_ab_tod_id', 'MS_Description', 'hwy_link_ab_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.hwy_link_ab_id', 'MS_Description', 'hwy_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.hwy_link_tod_id', 'MS_Description', 'hwy_link_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.hwycov_id', 'MS_Description', 'highway network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.ab', 'MS_Description', 'link ab direction indicator'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.cp', 'MS_Description', 'link capacity, 999999 used as missing value'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.cx', 'MS_Description', 'intersection approach capacity, 999999 used as missing value'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.tm', 'MS_Description', 'link time in minutes in the from-to direction, 999 used as missing value'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.tx', 'MS_Description', 'intersection delay time'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.ln', 'MS_Description', 'number of lanes in the from-to direction, 9 used as missing value'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.stm', 'MS_Description', 'single occupancy vehicle time, 999.25 used as missing value'
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod.htm', 'MS_Description', 'high occupancy vehicle time, 999.25 used as missing value'
GO


-- create household dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[household] (
    [scenario_id] int NOT NULL,
    [household_id] int NOT NULL,  -- insert NULL record as 0
    [autos] int NULL,  -- allow NULLs
    [autos_human_vehicles] int NULL,  -- allow NULLs
    [autos_autonomous_vehicles] int NULL,  -- allow NULLs
    [transponder_available] nvarchar(25) NOT NULL,
    [geography_household_location_id] int NOT NULL,
    [household_income_category] nvarchar(20) NOT NULL,
    [household_income] int NULL,  -- allow NULLs
    [household_workers] nvarchar(20) NOT NULL,
    [household_persons] nvarchar(20) NOT NULL,
    [building_category] nvarchar(35) NOT NULL,
    [unit_type] nvarchar(35) NOT NULL,
    [poverty] float NULL,  -- allow NULLs
    INDEX ccsi_household CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'dimension.household', 'MS_Description', 'synthetic households and results of abm household choice models'
EXECUTE [db_meta].[add_xp] 'dimension.household.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'dimension.household.autos', 'MS_Description', 'ABM auto ownership model result'
EXECUTE [db_meta].[add_xp] 'dimension.household.autos_human_vehicles', 'MS_Description', 'ABM auto ownership model result for human-driven vehicles'
EXECUTE [db_meta].[add_xp] 'dimension.household.autos_autonomous_vehicles', 'MS_Description', 'ABM auto ownership model result for autonomous vehicles'
EXECUTE [db_meta].[add_xp] 'dimension.household.transponder_available', 'MS_Description', 'ABM toll transponder ownership model result'
EXECUTE [db_meta].[add_xp] 'dimension.household.geography_household_location_id', 'MS_Description', 'household location geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_income_category', 'MS_Description', 'household income category'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_income', 'MS_Description', 'household income'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_workers', 'MS_Description', 'number of workers in the household'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_persons', 'MS_Description', 'number of persons in the household'
EXECUTE [db_meta].[add_xp] 'dimension.household.building_category', 'MS_Description', 'number of units in structure and quality'
EXECUTE [db_meta].[add_xp] 'dimension.household.unit_type', 'MS_Description', 'household unit type'
EXECUTE [db_meta].[add_xp] 'dimension.household.poverty', 'MS_Description', 'household income divided by federal poverty threshold'
GO


-- create person dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[person] (
    [scenario_id] int NOT NULL,
    [person_id] int NOT NULL,  -- insert NULL record as 0
    [household_id] int NOT NULL,  -- insert NULL record as 0
    [person_number] int NOT NULL,
    [age] int NULL,
    [sex] nvarchar(15) NOT NULL,
    [military_status] nvarchar(20) NOT NULL,
    [employment_status] nvarchar(35) NOT NULL,
    [student_status] nvarchar(40) NOT NULL,
    [abm_person_type] nvarchar(35) NOT NULL,
    [education] nvarchar(45) NOT NULL,
    [grade] nvarchar(35) NOT NULL,
    [weeks] nvarchar(35) NOT NULL,
    [hours] nvarchar(45) NOT NULL,
    [race] nvarchar(125) NOT NULL,
    [hispanic] nvarchar(20) NOT NULL,
    [abm_activity_pattern] nvarchar(15) NOT NULL,
    [free_parking_choice] nvarchar(35) NOT NULL,
    [parking_reimbursement_pct] float NULL,  -- allow NULLs
    [telecommute_choice] nvarchar(25) NOT NULL,
    [work_segment] nvarchar(55) NOT NULL,
    [school_segment] nvarchar(15) NOT NULL,
    [geography_work_location_id] int NOT NULL,
    [geography_school_location_id] int NOT NULL,
    INDEX ccsi_person CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'dimension.person', 'MS_Description', 'synthetic persons and results of abm person choice models'
EXECUTE [db_meta].[add_xp] 'dimension.person.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.person_id', 'MS_Description', 'person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.person_number', 'MS_Description', 'person surrogate key within ABM household identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.age', 'MS_Description', 'person age'
EXECUTE [db_meta].[add_xp] 'dimension.person.sex', 'MS_Description', 'person sex'
EXECUTE [db_meta].[add_xp] 'dimension.person.military_status', 'MS_Description', 'military status'
EXECUTE [db_meta].[add_xp] 'dimension.person.employment_status', 'MS_Description', 'employment status'
EXECUTE [db_meta].[add_xp] 'dimension.person.student_status', 'MS_Description', 'student status'
EXECUTE [db_meta].[add_xp] 'dimension.person.abm_person_type', 'MS_Description', 'ABM model person type designation'
EXECUTE [db_meta].[add_xp] 'dimension.person.education', 'MS_Description', 'educational attainment'
EXECUTE [db_meta].[add_xp] 'dimension.person.grade', 'MS_Description', 'school enrollment grade'
EXECUTE [db_meta].[add_xp] 'dimension.person.weeks', 'MS_Description', 'weeks worked in past 12 months'
EXECUTE [db_meta].[add_xp] 'dimension.person.hours', 'MS_Description', 'typical hours worked per week in the past 12 months'
EXECUTE [db_meta].[add_xp] 'dimension.person.race', 'MS_Description', 'person race'
EXECUTE [db_meta].[add_xp] 'dimension.person.hispanic', 'MS_Description', 'hispanic ethnicity indicator'
EXECUTE [db_meta].[add_xp] 'dimension.person.abm_activity_pattern', 'MS_Description', 'ABM activity pattern model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.free_parking_choice', 'MS_Description', 'ABM free parking model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.parking_reimbursement_pct', 'MS_Description', 'ABM free parking reimbursement percentage model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.telecommute_choice', 'MS_Description', 'ABM telecommute model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.work_segment', 'MS_Description', 'ABM work location choice model employment category'
EXECUTE [db_meta].[add_xp] 'dimension.person.school_segment', 'MS_Description', 'ABM school location choice model student category'
EXECUTE [db_meta].[add_xp] 'dimension.person.geography_work_location_id', 'MS_Description', 'ABM work location choice model work location geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.person.geography_school_location_id', 'MS_Description', 'ABM school location choice model school location geography surrogate key'
GO


-- create tour dimension
-- partitioned clustered columnstore
CREATE TABLE [dimension].[tour] (
    [scenario_id] int NOT NULL,
    [tour_id] int IDENTITY(0,1) NOT NULL,  -- insert NULL record as 0
    [model_tour_id] int NOT NULL,
    [abm_tour_id] int NOT NULL,
    [time_tour_start_id] int NOT NULL,
    [time_tour_end_id] int NOT NULL,
    [geography_tour_origin_id] int NOT NULL,
    [geography_tour_destination_id] int NOT NULL,
    [mode_tour_id] int NOT NULL,
    [purpose_tour_id] int NOT NULL,
    [tour_category] nvarchar(50) NOT NULL,
    [tour_crossborder_point_of_entry] nvarchar(20) NOT NULL,
    [tour_crossborder_sentri] nvarchar(15) NOT NULL,
    INDEX ccsi_tour CLUSTERED COLUMNSTORE)
ON scenario_scheme([scenario_id])
GO

EXECUTE [db_meta].[add_xp] 'dimension.tour', 'MS_Description', 'dimension table of tours for ABM sub-models where applicable'
EXECUTE [db_meta].[add_xp] 'dimension.tour.scenario_id', 'MS_Description', 'tour surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_id', 'MS_Description', 'tour surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.model_tour_id', 'MS_Description', 'ABM sub-model surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.abm_tour_id', 'MS_Description', 'ABM sub-model tour identifier surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.time_tour_start_id', 'MS_Description', 'tour start time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.time_tour_end_id', 'MS_Description', 'tour end time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.geography_tour_origin_id', 'MS_Description', 'tour origin geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.geography_tour_destination_id', 'MS_Description', 'tour destination geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.mode_tour_id', 'MS_Description', 'tour mode surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.purpose_tour_id', 'MS_Description', 'tour purpose surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_category', 'MS_Description', 'tour category'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_crossborder_point_of_entry', 'MS_Description', 'mexican border point of entry - only applicable to cross border model tours'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_crossborder_sentri', 'MS_Description', 'indicator of SENTRI availability - only applicable to cross border model tours'
GO


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
GO

EXECUTE [db_meta].[add_xp] 'dimension.transit_link', 'MS_Description', 'input transit network'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.transit_link_id', 'MS_Description', 'transit link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.trcov_id', 'MS_Description', 'transit network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.transit_link_shape', 'MS_Description', 'transit link linestring geometry'
GO


-- create transit_route dimension
CREATE TABLE [dimension].[transit_route] (
    [scenario_id] int NOT NULL,
    [transit_route_id] int NOT NULL,
    [route_id] int NOT NULL,
    [route_name] int NOT NULL,
    [mode_transit_route_id] int NOT NULL,
    [am_headway] float NOT NULL,
    [pm_headway] float NOT NULL,
    [op_headway] float NOT NULL,
    [nt_headway] float NOT NULL,
    [nt_hour] int NOT NULL,
    [config] int NOT NULL,
    [fare] float NOT NULL,
    [transit_route_shape] geometry NOT NULL,
    CONSTRAINT pk_transitroute PRIMARY KEY ([scenario_id], [transit_route_id]) WITH (STATISTICS_INCREMENTAL = ON),
    CONSTRAINT ixuq_transitroute UNIQUE ([scenario_id], [route_id]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
    CONSTRAINT fk_transitroute_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]),
    CONSTRAINT fk_transitroute_mode FOREIGN KEY ([mode_transit_route_id]) REFERENCES [dimension].[mode] ([mode_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
GO
EXECUTE [db_meta].[add_xp] 'dimension.transit_route', 'MS_Description', 'input transit network routes'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.transit_route_id', 'MS_Description', 'transit route surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.route_id', 'MS_Description', 'transit network route identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.route_name', 'MS_Description', 'route name and configuration'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.mode_transit_route_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.am_headway', 'MS_Description', 'ABM am peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.pm_headway', 'MS_Description', 'ABM pm peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.op_headway', 'MS_Description', 'ABM mid-day off peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.nt_headway', 'MS_Description', 'night hour headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.nt_hour', 'MS_Description', 'night hour'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.config', 'MS_Description', 'combination of route, direction, and configuration XX(X)'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.fare', 'MS_Description', 'route fare in dollars'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.transit_route_shape', 'MS_Description', 'transit route linestring geometry'
GO


-- create transit_stop dimension
CREATE TABLE [dimension].[transit_stop] (
    [scenario_id] int NOT NULL,
    [transit_stop_id] int NOT NULL,
    [transit_route_id] int NOT NULL,
    [transit_link_id] int NOT NULL,
    [stop_id] int NOT NULL,
    [route_id] int NOT NULL,
    [trcov_id] int NOT NULL,
    [mp] float NOT NULL,
    [near_node] int NOT NULL,
    [fare_zone] int NOT NULL,
    [stop_name] nvarchar(100) NOT NULL,
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
GO

EXECUTE [db_meta].[add_xp] 'dimension.transit_stop', 'MS_Description', 'input transit network stop nodes'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.transit_stop_id', 'MS_Description', 'transit stop surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.transit_route_id', 'MS_Description', 'transit route surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.transit_link_id', 'MS_Description', 'transit link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.stop_id', 'MS_Description', 'transit network stop identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.route_id', 'MS_Description', 'transit network route identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.trcov_id', 'MS_Description', 'transit network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.mp', 'MS_Description', 'milepost'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.near_node', 'MS_Description', 'nearest node number matches to tnode and trcov_id in trcov/node.'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.fare_zone', 'MS_Description', 'fare zone for coaster and light rail'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.stop_name', 'MS_Description', 'stop name'
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop.transit_stop_shape', 'MS_Description', 'transit stop point geometry'
GO


-- create transit_tap dimension
CREATE TABLE [dimension].[transit_tap] (
    [scenario_id] int NOT NULL,
    [transit_tap_id] int NOT NULL,
    [tap] nvarchar(15) NOT NULL,
    [transit_tap_shape] geometry NULL,
    CONSTRAINT pk_transittap PRIMARY KEY ([scenario_id], [transit_tap_id]) WITH (STATISTICS_INCREMENTAL = ON),
    CONSTRAINT ixuq_transittap UNIQUE ([scenario_id], [tap]) WITH (STATISTICS_INCREMENTAL = ON, DATA_COMPRESSION = PAGE),
    CONSTRAINT fk_transittap_scenario FOREIGN KEY ([scenario_id]) REFERENCES [dimension].[scenario] ([scenario_id]))
ON scenario_scheme([scenario_id])
WITH (DATA_COMPRESSION = PAGE)
GO

EXECUTE [db_meta].[add_xp] 'dimension.transit_tap', 'MS_Description', 'input transit network transit access point nodes'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.transit_tap_id', 'MS_Description', 'transit tap surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.tap', 'MS_Description', 'transit access point identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.transit_tap_shape', 'MS_Description', 'transit access point point geometry'
GO
