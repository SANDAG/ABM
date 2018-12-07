-- base schemas
EXECUTE [db_meta].[add_xp] 'dimension', 'MS_Description', 'schema to hold and manage ABM dimension tables and views'
EXECUTE [db_meta].[add_xp] 'fact', 'MS_Description', 'schema to hold and manage ABM fact tables'
EXECUTE [db_meta].[add_xp] 'dbo', 'MS_Description', 'workspace and junk default schema - should not be used'
EXECUTE [db_meta].[add_xp] 'staging', 'MS_Description', 'schema to hold intermediary loading data tables'
GO




-- database tables
-- [dimension].[bike_link]
EXECUTE [db_meta].[add_xp] 'dimension.bike_link', 'SUBSYSTEM', 'bike network'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link', 'MS_Description', 'SANGIS all streets network used for bike routing and walk access in the AT model'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.bike_link_id', 'MS_Description', 'bike_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.roadsegid', 'MS_Description', 'SANGIS all streets network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.nm', 'MS_Description', 'name of link'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.functional_class', 'MS_Description', 'type of road facility'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.bike2sep', 'MS_Description', 'indicator of physically-separated on-street bike lane'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.bike3blvd', 'MS_Description', 'indicator of bicycle boulevard'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.speed', 'MS_Description', 'bike speeds in miles per hour'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.distance', 'MS_Description', 'length of link in miles'
EXECUTE [db_meta].[add_xp] 'dimensionbike_link.scenicldx', 'MS_Description', 'scenic index represents the closeness to the ocean and parks'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link.shape', 'MS_Description', 'bike link linestring geometry'
GO


-- [dimension].[bike_link_ab]
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab', 'SUBSYSTEM', 'bike network'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab', 'MS_Description', 'ab specific attributes of SANGIS all streets network used for bike routing and walk access in the AT model'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.bike_link_ab_id', 'MS_Description', 'bike_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.bike_link_id', 'MS_Description', 'bike_link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.roadsegid', 'MS_Description', 'SANGIS all streets network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.ab', 'MS_Description', 'link ab direction indicator'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.from_node', 'MS_Description', 'from node number'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.to_node', 'MS_Description', 'to node number'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.gain', 'MS_Description', 'positive elevation gain in feet'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.bike_class', 'MS_Description', 'bike link classification'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.lanes', 'MS_Description', 'vehicle lanes present indicator'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.from_signal', 'MS_Description', 'indicator of signal at from node'
EXECUTE [db_meta].[add_xp] 'dimension.bike_link_ab.TO_signal', 'MS_Description', 'indicator of signal at to node'
GO


-- [dimension].[escort_stop_type]
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type', 'MS_Description', 'escort model stop types within individual model'
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type.escort_stop_type_id', 'MS_Description', 'escort_stop_type surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type.escort_stop_type_description', 'MS_Description', 'escort_stop_type description'
GO


-- [dimension].[geography]
EXECUTE [db_meta].[add_xp] 'dimension.geography', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography', 'MS_Description', 'geography dimension for ABM model including cross references'
EXECUTE [db_meta].[add_xp] 'dimension.geography.geography_id', 'MS_Description', 'geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.geography.mgra_13', 'MS_Description', 'series 13 MGRA geography zones - base geography unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.geography.mgra_13_shape', 'MS_Description', 'series 13 MGRA geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.taz_13', 'MS_Description', 'series 13 TAZ geography zones - base and secondary geography unit of ABM model - MGRA geograhy nests within excluding external TAZs'
EXECUTE [db_meta].[add_xp] 'dimension.geography.taz_13_shape', 'MS_Description', 'series 13 TAZ geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.luz_13', 'MS_Description', 'series 13 LUZ geography zones - for aggregation - abm geographies nest within excluding external TAZs'
EXECUTE [db_meta].[add_xp] 'dimension.geography.luz_13_shape', 'MS_Description', 'series 13 LUZ geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cicpa_2016', 'MS_Description', 'Community Planning Areas (City of San Diego) 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cicpa_2016_name', 'MS_Description', 'Community Planning Areas (City of San Diego) 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cicpa_2016_shape', 'MS_Description', 'Community Planning Areas (City of San Diego) 2016 geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cocpa_2016', 'MS_Description', 'Community Planning Areas (County of San Diego) 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cocpa_2016_name', 'MS_Description', 'Community Planning Areas (County of San Diego) 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.cocpa_2016_shape', 'MS_Description', 'Community Planning Areas (County of San Diego) 2016 geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.jurisdiction_2016', 'MS_Description', 'Jurisdictions Year 2016 geography zones - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.jurisdiction_2016_name', 'MS_Description', 'Jurisdictions Year 2016 geography names - for aggregation - abm geographies do not nest within - used centroid based lookup'
EXECUTE [db_meta].[add_xp] 'dimension.geography.jurisdiction_2016_shape', 'MS_Description', 'Jurisdictions Year 2016 geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.region_2004', 'MS_Description', '2004 San Diego Region geography zones - for aggregation - all geographies excluding external TAZs nest within'
EXECUTE [db_meta].[add_xp] 'dimension.geography.region_2004_name', 'MS_Description', '2004 San Diego Region geography names - for aggregation - all geographies excluding external TAZs nest within'
EXECUTE [db_meta].[add_xp] 'dimension.geography.region_2004_shape', 'MS_Description', '2004 San Diego Region geography geometry'
EXECUTE [db_meta].[add_xp] 'dimension.geography.external_zone', 'MS_Description', 'Non-San Diego Region External Zone names - map to Series 13 TAZ 1-12'
GO


-- [dimension].[household]
EXECUTE [db_meta].[add_xp] 'dimension.household', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.household', 'MS_Description', 'synthetic households and results of abm household choice models'
EXECUTE [db_meta].[add_xp] 'dimension.household.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'dimension.household.income', 'MS_Description', 'household income'
EXECUTE [db_meta].[add_xp] 'dimension.household.income_category', 'MS_Description', 'household income category'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_size', 'MS_Description', 'number of persons in the household'
EXECUTE [db_meta].[add_xp] 'dimension.household.household_workers', 'MS_Description', 'number of workers in the household'
EXECUTE [db_meta].[add_xp] 'dimension.household.bldgsz', 'MS_Description', 'Number of Units in Structure & Quality'
EXECUTE [db_meta].[add_xp] 'dimension.household.unittype', 'MS_Description', 'household unit type'
EXECUTE [db_meta].[add_xp] 'dimension.household.autos', 'MS_Description', 'ABM auto ownership model result'
EXECUTE [db_meta].[add_xp] 'dimension.household.transponder', 'MS_Description', 'ABM toll transponder ownership model result'
EXECUTE [db_meta].[add_xp] 'dimension.household.poverty', 'MS_Description', 'Household income divided by Federal Poverty Threshold (2010)'
EXECUTE [db_meta].[add_xp] 'dimension.household.geography_household_location_id', 'MS_Description', 'household location geography surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.household.version_household', 'MS_Description', 'synthetic population version number'
EXECUTE [db_meta].[add_xp] 'dimension.household.weight_household', 'MS_Description', 'weight to use if measuring number of households'
GO


-- [dimension].[hwy_link]
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link', 'SUBSYSTEM', 'highway network'
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


-- [dimension].[hwy_link_ab]
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab', 'SUBSYSTEM', 'highway network'
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


-- [dimension].[hwy_link_ab_tod]
EXECUTE [db_meta].[add_xp] 'dimension.hwy_link_ab_tod', 'SUBSYSTEM', 'highway network'
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


-- [dimension].[inbound]
EXECUTE [db_meta].[add_xp] 'dimension.inbound', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.inbound', 'MS_Description', 'trip inbound or outbound direction reference table'
EXECUTE [db_meta].[add_xp] 'dimension.inbound.inbound_id', 'MS_Description', 'inbound surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.inbound.inbound_description', 'MS_Description', 'inbound description'
GO


-- [dimension].[mode]
EXECUTE [db_meta].[add_xp] 'dimension.mode', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode', 'MS_Description', 'travel mode dimension'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_description', 'MS_Description', 'mode description'
EXECUTE [db_meta].[add_xp] 'dimension.mode.mode_aggregate_description', 'MS_Description', 'aggregate mode category description'
GO


-- [dimension].[model]
EXECUTE [db_meta].[add_xp] 'dimension.model', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.model', 'MS_Description', 'ABM sub-model dimension'
EXECUTE [db_meta].[add_xp] 'dimension.model.model_id', 'MS_Description', 'model surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.model.model_description', 'MS_Description', 'model description'
GO


-- [dimension].[person]
EXECUTE [db_meta].[add_xp] 'dimension.person', 'SUBSYSTEM', 'land use'
EXECUTE [db_meta].[add_xp] 'dimension.person', 'MS_Description', 'synthetic persons and results of abm person choice models'
EXECUTE [db_meta].[add_xp] 'dimension.person.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.person_id', 'MS_Description', 'person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'dimension.person.age', 'MS_Description', 'person age'
EXECUTE [db_meta].[add_xp] 'dimension.person.sex', 'MS_Description', 'person sex'
EXECUTE [db_meta].[add_xp] 'dimension.person.military_status', 'MS_Description', 'military status'
EXECUTE [db_meta].[add_xp] 'dimension.person.employment_status', 'MS_Description', 'employment status'
EXECUTE [db_meta].[add_xp] 'dimension.person.student_status', 'MS_Description', 'student status'
EXECUTE [db_meta].[add_xp] 'dimension.person.abm_person_type', 'MS_Description', 'ABM model person type designation'
EXECUTE [db_meta].[add_xp] 'dimension.person.education', 'MS_Description', 'Educational Attainment'
EXECUTE [db_meta].[add_xp] 'dimension.person.grade', 'MS_Description', 'School Enrollment Grade'
EXECUTE [db_meta].[add_xp] 'dimension.person.weeks', 'MS_Description', 'Weeks worked in past 12 months'
EXECUTE [db_meta].[add_xp] 'dimension.person.hours', 'MS_Description', 'Typical hours worked per week in the past 12 months'
EXECUTE [db_meta].[add_xp] 'dimension.person.race', 'MS_Description', 'person race'
EXECUTE [db_meta].[add_xp] 'dimension.person.hispanic', 'MS_Description', 'hispanic ethnicity indicator'
EXECUTE [db_meta].[add_xp] 'dimension.person.version_person', 'MS_Description', 'synthetic population version number'
EXECUTE [db_meta].[add_xp] 'dimension.person.abm_activity_pattern', 'MS_Description', 'ABM activity pattern model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.freeparking_choice', 'MS_Description', 'ABM free parking model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.freeparking_reimbpct', 'MS_Description', 'ABM free parking reimbursement percentage model result'
EXECUTE [db_meta].[add_xp] 'dimension.person.work_segment', 'MS_Description', 'ABM work location choice model employment category'
EXECUTE [db_meta].[add_xp] 'dimension.person.school_segment', 'MS_Description', 'ABM school location choice model student category'
EXECUTE [db_meta].[add_xp] 'dimension.person.work_distance', 'MS_Description', 'ABM work location choice model distance from work location'
EXECUTE [db_meta].[add_xp] 'dimension.person.school_distance', 'MS_Description', 'ABM school location choice model distance from school location'
EXECUTE [db_meta].[add_xp] 'dimension.person.weight_person', 'MS_Description', 'weight to use if measuring number of persons'
GO


-- [dimension].[purpose]
EXECUTE [db_meta].[add_xp] 'dimension.purpose', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.purpose', 'MS_Description', 'purpose dimension'
EXECUTE [db_meta].[add_xp] 'dimension.purpose.purpose_id', 'MS_Description', 'purpose surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.purpose.purpose_description', 'MS_Description', 'purpose description'
GO


-- [dimension].[scenario]
EXECUTE [db_meta].[add_xp] 'dimension.scenario', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.scenario', 'MS_Description', 'dimension table of loaded scenarios in the database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.scenario_id', 'MS_Description', 'scenario surrogate key and scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.name', 'MS_Description', 'base file path name of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.year', 'MS_Description', 'scenario year'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.iteration', 'MS_Description', 'scenario iteration loaded into database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.sample_rate', 'MS_Description', 'sample rate of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.abm_version', 'MS_Description', 'ABM model software version of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.path', 'MS_Description', 'full UNC file path of scenario'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.user_name', 'MS_Description', 'user who requested scenario be loaded into the database'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.complete', 'MS_Description', 'indicator scenario is completely loaded'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.date_loaded', 'MS_Description', 'date and time scenario was completely loaded'
EXECUTE [db_meta].[add_xp] 'dimension.scenario.load_failed', 'MS_Description', 'indicator scenario loading failed'
GO


-- [dimension].[time]
EXECUTE [db_meta].[add_xp] 'dimension.time', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.time', 'MS_Description', 'time dimension for ABM model including cross references'
EXECUTE [db_meta].[add_xp] 'dimension.time.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour', 'MS_Description', 'ABM half hour time period - base time unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour_period_start', 'MS_Description', 'start time of ABM half hour time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_half_hour_period_end', 'MS_Description', 'end time of ABM half hour time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod', 'MS_Description', 'ABM five time of day time period - base and secondary time unit of ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod_period_start', 'MS_Description', 'start time of ABM five time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.abm_5_tod_period_end', 'MS_Description', 'end time of ABM five time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.day', 'MS_Description', 'day time period - for aggregation'
EXECUTE [db_meta].[add_xp] 'dimension.time.day_period_start', 'MS_Description', 'start time of day time period'
EXECUTE [db_meta].[add_xp] 'dimension.time.day_period_end', 'MS_Description', 'end time of day time period'
GO


-- [dimension].[tour]
EXECUTE [db_meta].[add_xp] 'dimension.tour', 'SUBSYSTEM', 'tours and trips'
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
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_crossborder_sentri', 'MS_Description', 'indicator of sentri availability - only applicable to cross border model tours'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_visitor_auto', 'MS_Description', 'indicator of auto availability - only applicable to visitor model tours'
EXECUTE [db_meta].[add_xp] 'dimension.tour.tour_visitor_income', 'MS_Description', 'income category - only applicable to visitor model tours'
EXECUTE [db_meta].[add_xp] 'dimension.tour.weight_person_tour', 'MS_Description', 'weight to use if measuring number of person tours'
EXECUTE [db_meta].[add_xp] 'dimension.tour.weight_tour', 'MS_Description', 'weight to use if measuring number of tours'
GO


-- [dimension].[transit_link]
EXECUTE [db_meta].[add_xp] 'dimension.transit_link', 'SUBSYSTEM', 'transit network'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link', 'MS_Description', 'input transit network'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.transit_link_id', 'MS_Description', 'transit link surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.trcov_id', 'MS_Description', 'transit network link identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_link.transit_link_shape', 'MS_Description', 'transit link linestring geometry'
GO


-- [dimension].[transit_route]
EXECUTE [db_meta].[add_xp] 'dimension.transit_route', 'SUBSYSTEM', 'transit network'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route', 'MS_Description', 'input transit network routes'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.transit_route_id', 'MS_Description', 'transit route surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.route_id', 'MS_Description', 'transit network route identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.route_name', 'MS_Description', 'route name and configuration'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.mode_transit_route_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.am_headway', 'MS_Description', 'ABM am peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.pm_headway', 'MS_Description', 'ABM pm peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.op_headway', 'MS_Description', 'ABM mid-day off peak time period headway'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.nt_headway', 'MS_Description', 'night time period (7pm - 6am) time period headway, not used in ABM model'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.nt_hour', 'MS_Description', 'number of hours within the night time period (7pm - 6am) that the night headway applies, may not be exact, used in conjunction with night time period headways to get actual number of vehicles'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.config', 'MS_Description', 'combination of route, direction, and configuration XX(X)'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.fare', 'MS_Description', 'route fare in dollars'
EXECUTE [db_meta].[add_xp] 'dimension.transit_route.transit_route_shape', 'MS_Description', 'transit route linestring geometry'
GO


-- [dimension].[transit_stop]
EXECUTE [db_meta].[add_xp] 'dimension.transit_stop', 'SUBSYSTEM', 'transit network'
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


-- [dimension].[transit_tap]
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap', 'SUBSYSTEM', 'transit network'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap', 'MS_Description', 'input transit network transit access point nodes'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.transit_tap_id', 'MS_Description', 'transit tap surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.tap', 'MS_Description', 'transit access point identifier'
EXECUTE [db_meta].[add_xp] 'dimension.transit_tap.transit_tap_shape', 'MS_Description', 'transit access point point geometry'
GO


-- [dimension].[value_of_time_drive_bin]
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_drive_bin', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_drive_bin', 'MS_Description', 'dimension table of passenger auto skim value of time categories'
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_drive_bin.value_of_time_drive_bin_id', 'MS_Description', 'passenger auto skim value of time surrogate key'
EXECUTE [db_meta].[add_xp] 'dimension.value_of_time_drive_bin.value_of_time_drive_bin_description', 'MS_Description', 'passenger auto skim value of time description'
GO


-- [fact].[bike_flow]
EXECUTE [db_meta].[add_xp] 'fact.bike_flow', 'SUBSYSTEM', 'bike network'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow', 'MS_Description', 'bike volumes by roadsegid, ab, and time period'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.bike_flow_id', 'MS_Description', 'bike_flow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.bike_link_id', 'MS_Description', 'bike_link surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.bike_link_ab_id', 'MS_Description', 'bike_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.bike_flow.flow', 'MS_Description', 'total volume'
GO


-- [fact].[hwy_flow]
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow', 'SUBSYSTEM', 'highway network'
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


-- [fact].[hwy_flow_mode]
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode', 'SUBSYSTEM', 'highway network'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode', 'MS_Description', 'loaded highway network by hwycov_id, ab, time period, and mode'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_flow_mode_id', 'MS_Description', 'hwy_flow surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_id', 'MS_Description', 'hwy_link surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_ab_id', 'MS_Description', 'hwy_link_ab surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_tod_id', 'MS_Description', 'hwy_link_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.hwy_link_ab_tod_id', 'MS_Description', 'hwy_link_ab_tod surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.mode_id', 'MS_Description', 'mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.hwy_flow_mode.flow', 'MS_Description', 'total volume'
GO


-- [fact].[mgra_based_input]
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input', 'SUBSYSTEM', 'land_use'
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
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.totint', 'MS_Description', 'intersection count in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.duden', 'MS_Description', 'dwelling unit density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.empden', 'MS_Description', 'employment density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.popden', 'MS_Description', 'population density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.retempden', 'MS_Description', 'retail employment density in 1/2 mile radius'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.totintbin', 'MS_Description', 'totint bin (0-80,90-130,130+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.empdenbin', 'MS_Description', 'empden bin (0-10,11-30,30+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.dudenbin', 'MS_Description', 'duden bin (0-5,5-10,10+)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.zip09', 'MS_Description', 'zip code'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.parkactive', 'MS_Description', 'park acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.openspaceparkpreserve', 'MS_Description', 'open space acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.beachactive', 'MS_Description', 'beach acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.budgetroom', 'MS_Description', 'budget hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.economyroom', 'MS_Description', 'economy hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.luxuryroom', 'MS_Description', 'luxury hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.midpriceroom', 'MS_Description', 'mid price hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.upscaleroom', 'MS_Description', 'upscale hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.hotelroomtotal', 'MS_Description', 'total hotel rooms'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.truckregiontype', 'MS_Description', 'truck region type'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.district27', 'MS_Description', 'district 27 indicator'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.milestocoast', 'MS_Description', 'miles to the coast'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.acres', 'MS_Description', 'acres'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.effective_acres', 'MS_Description', 'acres of developable land (or available for development)'
EXECUTE [db_meta].[add_xp] 'fact.mgra_based_input.land_acres', 'MS_Description', 'acres of land area excluding water such as bay, lagoon, lake, reservoir, or large pond'
GO


-- [fact].[person_trip]
EXECUTE [db_meta].[add_xp] 'fact.person_trip', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'fact.person_trip', 'MS_Description', 'person trip fact table'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_trip_id', 'MS_Description', 'person_trip surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_id', 'MS_Description', 'person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.household_id', 'MS_Description', 'household surrogate key and ABM household identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.tour_id', 'MS_Description', 'tour surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.model_trip_id', 'MS_Description', 'ABM sub-model surrogate key'
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
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_escort_drive_id', 'MS_Description', 'escort model driver person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.escort_stop_type_origin_id', 'MS_Description', 'trip origin escort_stop_type surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_escort_origin_id', 'MS_Description', 'escort model trip origin passenger person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.escort_stop_type_destination_id', 'MS_Description', 'trip destination escort_stop_type surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.person_escort_destination_id', 'MS_Description', 'escort model trip destination passenger person surrogate key and ABM person identifier'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.mode_airport_arrival_id', 'MS_Description', 'airport model arrival mode surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_drive', 'MS_Description', 'trip drive time in minutes - includes auto portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.dist_drive', 'MS_Description', 'trip drive distance in miles - includes auto portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.toll_cost_drive', 'MS_Description', 'trip drive toll cost in dollars - includes auto portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.operating_cost_drive', 'MS_Description', 'trip auto operating cost in dollars - includes auto portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_walk', 'MS_Description', 'trip walk time in minutes - includes walk portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.dist_walk', 'MS_Description', 'trip walk distance in miles - includes walk portion of transit trips where applicable'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle_local', 'MS_Description', 'local transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle_express', 'MS_Description', 'express transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle_rapid', 'MS_Description', 'brt transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle_light_rail', 'MS_Description', 'light rail transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle_commuter_rail', 'MS_Description', 'commuter rail transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_in_vehicle', 'MS_Description', 'transit in vehicle time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_transit', 'MS_Description', 'transit fare cost in dollars'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_auxiliary', 'MS_Description', 'auxiliary transit time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_transit_wait', 'MS_Description', 'initial transit wait time and first transfer wait time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.transit_transfers', 'MS_Description', 'number of transit transfers'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.time_total', 'MS_Description', 'total trip time in minutes'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.dist_total', 'MS_Description', 'total trip distance in miles'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.cost_total', 'MS_Description', 'total trip cost in dollars - includes operating cost'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.value_of_time', 'MS_Description', 'person trip value of time in dollars per hour'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.value_of_time_drive_bin_id', 'MS_Description', 'person trip auto skim value of time category surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.weight_person_trip', 'MS_Description', 'weight to use if measuring number of person trips'
EXECUTE [db_meta].[add_xp] 'fact.person_trip.weight_trip', 'MS_Description', 'weight to use if measuring number of trips'
GO


-- [fact].[transit_aggflow]
EXECUTE [db_meta].[add_xp] 'fact.transit_aggflow', 'SUBSYSTEM', 'transit network'
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


-- [fact].[transit_flow]
EXECUTE [db_meta].[add_xp] 'fact.transit_flow', 'SUBSYSTEM', 'transit network'
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


-- [fact].[transit_onoff]
EXECUTE [db_meta].[add_xp] 'fact.transit_onoff', 'SUBSYSTEM', 'transit network'
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


-- [fact].[transit_pnr]
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr', 'SUBSYSTEM', 'transit network'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr', 'MS_Description', 'park and ride tap lot details and vehicles'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.scenario_id', 'MS_Description', 'scenario identifier'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.transit_pnr_id', 'MS_Description', 'transit_pnr surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.transit_tap_id', 'MS_Description', 'transit_tap surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.lot_id', 'MS_Description', 'lot id in tap.ptype input file'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.geography_id', 'MS_Description', 'geography surrogate key where pnr lot is located'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.time_id', 'MS_Description', 'time surrogate key'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.parking_type', 'MS_Description', 'parking type'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.capacity', 'MS_Description', 'number of stalls in the parking lot'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.distance', 'MS_Description', 'distance from lot to transit access point in feet'
EXECUTE [db_meta].[add_xp] 'fact.transit_pnr.vehicles', 'MS_Description', 'number of vehicles parked in lot'
GO




-- views
-- [dimension].[escort_stop_type_destination]
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type_destination', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type_destination', 'MS_Description', '[dimension].[escort_stop_type] role playing view'
GO


-- [dimension].[escort_stop_type_origin]
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type_origin', 'SUBSYSTEM', 'tours and trips'
EXECUTE [db_meta].[add_xp] 'dimension.escort_stop_type_origin', 'MS_Description', '[dimension].[escort_stop_type] role playing view'
GO


-- [dimension].[geography_household_location]
EXECUTE [db_meta].[add_xp] 'dimension.geography_household_location', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_household_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_school_location]
EXECUTE [db_meta].[add_xp] 'dimension.geography_school_location', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_school_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_tour_destination]
EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_destination', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_destination', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_tour_origin]
EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_origin', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_tour_origin', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_trip_destination]
EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_destination', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_destination', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_trip_origin]
EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_origin', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_trip_origin', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[geography_work_location]
EXECUTE [db_meta].[add_xp] 'dimension.geography_work_location', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.geography_work_location', 'MS_Description', '[dimension].[geography] role playing view'
GO


-- [dimension].[mode_airport_arrival]
EXECUTE [db_meta].[add_xp] 'dimension.mode_airport_arrival', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_airport_arrival', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[mode_tour]
EXECUTE [db_meta].[add_xp] 'dimension.mode_tour', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_tour', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[mode_transit]
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[mode_transit_access]
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_access', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_access', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[mode_transit_route]
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_route', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_transit_route', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[mode_trip]
EXECUTE [db_meta].[add_xp] 'dimension.mode_trip', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.mode_trip', 'MS_Description', '[dimension].[mode] role playing view'
GO


-- [dimension].[model_tour]
EXECUTE [db_meta].[add_xp] 'dimension.model_tour', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.model_tour', 'MS_Description', '[dimension].[model] role playing view'
GO


-- [dimension].[model_trip]
EXECUTE [db_meta].[add_xp] 'dimension.model_trip', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.model_trip', 'MS_Description', '[dimension].[model] role playing view'
GO


-- [dimension].[purpose_tour]
EXECUTE [db_meta].[add_xp] 'dimension.purpose_tour', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.purpose_tour', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- [dimension].[purpose_trip_destination]
EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_destination', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_destination', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- [dimension].[purpose_trip_origin]
EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_origin', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.purpose_trip_origin', 'MS_Description', '[dimension].[purpose] role playing view'
GO


-- [dimension].[time_tour_end]
EXECUTE [db_meta].[add_xp] 'dimension.time_tour_end', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.time_tour_end', 'MS_Description', '[dimension].[time] role playing view'
GO


-- [dimension].[time_tour_start]
EXECUTE [db_meta].[add_xp] 'dimension.time_tour_start', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.time_tour_start', 'MS_Description', '[dimension].[time] role playing view'
GO


-- [dimension].[time_trip_end]
EXECUTE [db_meta].[add_xp] 'dimension.time_trip_end', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.time_trip_end', 'MS_Description', '[dimension].[time] role playing view'
GO


-- [dimension].[time_trip_start]
EXECUTE [db_meta].[add_xp] 'dimension.time_trip_start', 'SUBSYSTEM', 'reference'
EXECUTE [db_meta].[add_xp] 'dimension.time_trip_start', 'MS_Description', '[dimension].[time] role playing view'
GO