SET NOCOUNT ON;


-- Create activity pattern table
IF OBJECT_ID('ref.activity_pattern','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[activity_pattern] (
		[activity_pattern_id] tinyint NOT NULL,
		[activity_pattern_desc] varchar(15) NOT NULL,
		CONSTRAINT pk_actpattern PRIMARY KEY ([activity_pattern_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[activity_pattern]
VALUES	
	(0,'Home'),
	(1,'Non-Mandatory'),
	(2,'Mandatory');
	
END


-- Create airport arrival mode table
IF OBJECT_ID('ref.airport_arrival_mode','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[ap_arrival_mode] (
		[ap_arrival_mode_id] tinyint NOT NULL,
		[ap_arrival_mode_desc] varchar(40) NOT NULL,
		CONSTRAINT pk_aparrivalmode PRIMARY KEY ([ap_arrival_mode_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[ap_arrival_mode]
VALUES	
	(0,'Airport Passing Through'),
	(1,'Park in Airport Terminal'),
	(2,'Park in Airport Remote Lot'),
	(3,'Park in Private Lot'),
	(4,'Dropped off at Parking Lot'),
	(5,'Curbside Drop Off'),
	(6,'Rental Car'),
	(7,'Taxi'),
	(8,'Shuttle'),
	(9,'Transit');
			
END


-- Create airport income category table
IF OBJECT_ID('ref.ap_income_cat','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[ap_income_cat] (
		[ap_income_cat_id] tinyint NOT NULL,
		[ap_income_cat_desc] varchar(10) NOT NULL,
		CONSTRAINT pk_apincomecat PRIMARY KEY ([ap_income_cat_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[ap_income_cat]
VALUES	
	(0,'< 25k'),
	(1,'25-50k'),
	(2,'50-75k'),
	(3,'75-100k'),
	(4,'100-125K'),
	(5,'125-150K'),
	(6,'150-200K'),
	(7,'200K+');

END


-- Create AT functional class table
IF OBJECT_ID('ref.at_func_class','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[at_func_class] (
		[at_func_class_id] smallint NOT NULL,
		[at_func_class_desc] varchar(100) NOT NULL,
		CONSTRAINT pk_atfuncclass PRIMARY KEY ([at_func_class_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[at_func_class]
VALUES	
	(-1,'P - Paper Streets and Q - Undocumented'),
	(0,'Pedestrian/bikeway, Recreational Parkway, Class I Bicycle Path'),
	(1,'Interstate'),
	(2,'Other Freeway and Expressway'),
	(3,'Other Principal Arterial'),
	(4,'Minor Arterial'),
	(5,'Major Collector'),
	(6,'Minor Collector'),
	(7,'Local'),
	(10,'Centroid Connector'),
	(11,'TAP Connector');

END


-- Create bike class
IF OBJECT_ID('ref.bike_class','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[bike_class] (
		[bike_class_id] tinyint NOT NULL,
		[bike_class_desc] varchar(100) NOT NULL,
		CONSTRAINT pk_bikeclass PRIMARY KEY ([bike_class_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[bike_class]
VALUES	
	(0,'Other suggested routes, Ferry, and Freeway shoulder'),
	(1,'Off-street path'),
	(2,'On-street lane'),
	(3,'On-street signed route'),
	(10,'Centroid Connector'),
	(11,'TAP Connector');

END


-- Create educational attainment table
IF OBJECT_ID('ref.educ','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[educ] (
		[educ_id] tinyint NOT NULL,
		[educ_desc] varchar(100) NOT NULL,
		CONSTRAINT pk_educ PRIMARY KEY ([educ_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[educ]
VALUES
	(0,'N/A (less than 3 years old)'),	
	(1,'No schooling completed'),
	(2,'Nursery school to grade 4'),
	(3,'Grade 5 or grade 6'),
	(4,'Grade 7 or grade 8'),
	(5,'Grade 9'),
	(6,'Grade 10'),
	(7,'Grade 11'),
	(8,'12th grade, no diploma'),
	(9,'High school graduate'),
	(10,'Some college, but less than 1 year'),
	(11,'One or more years of college, no degree'),
	(12,'Associates degree'),
	(13,'Bachelors degree'),
	(14,'Masters degree'),
	(15,'Professional school degree'),
	(16,'Doctorate degree');

END


-- Create free parking choice table
IF OBJECT_ID('ref.fp_choice','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[fp_choice] (
		[fp_choice_id] tinyint NOT NULL,
		[fp_choice_desc] varchar(10) NOT NULL,
		CONSTRAINT pk_fpchoice PRIMARY KEY ([fp_choice_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[fp_choice]
VALUES	
	(1,'Free'),
	(2,'Pay'),
	(3,'Reimburse');
			
END


-- GEOGRAPHY_TYPE
IF OBJECT_ID('ref.geography_type','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[geography_type] (
		[geography_type_id] tinyint NOT NULL,
		[geography_type_desc] varchar(50) NOT NULL,
		CONSTRAINT pk_geographytype PRIMARY KEY ([geography_type_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[geography_type]
VALUES
	(4, 'Region, Year 2008'),
	(34,'TAZ Series 13'),	
	(69, 'U.S. Census Public Use Microdata Areas, Year 2000'),
	(90,'MGRA Series 13');
			
END


-- GEOGRAPHY
IF OBJECT_ID('ref.geography_zone','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[geography_zone] (
		[geography_zone_id] int IDENTITY(1,1) NOT NULL,
		[geography_type_id] tinyint NOT NULL,
		[zone] smallint NOT NULL,
		[shape] geometry NOT NULL,
		[centroid] geometry NOT NULL,
		CONSTRAINT pk_geo PRIMARY KEY ([geography_zone_id]),
		CONSTRAINT ixuq_geo UNIQUE ([geography_type_id],[zone]) WITH (DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_geo_geotype FOREIGN KEY ([geography_type_id]) REFERENCES [ref].[geography_type]([geography_type_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO	[ref].[geography_zone]
SELECT 
	[geo_type_id]
	,[zone]
	,[shape]
	,[centroid]
FROM 
	OPENQUERY([pila\sdgintdb],'	SELECT 
									[geo_type_id]
									,[zone]
									,[shape]
									,[centroid]
								FROM
									[data_cafe].[dbo].[geography_zone] 
								WHERE 
									[geo_type_id] IN (4, 34, 69, 90) 
								ORDER BY 
									[geo_type_id], [zone]'
								)
			
END


-- Create grade level attending table
IF OBJECT_ID('ref.grade','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[grade] (
		[grade_id] tinyint NOT NULL,
		[grade_desc] varchar(50) NOT NULL,
		CONSTRAINT pk_grade PRIMARY KEY ([grade_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[grade]
VALUES
	(0,'N/A (not attending school)'),	
	(1,'Nursery school/preschool'),
	(2,'Kindergarten'),
	(3,'Grade 1 to grade 4'),
	(4,'Grade 5 to grade 8'),
	(5,'Grade 9 to grade 12'),
	(6,'College undergraduate'),
	(7,'Graduate or professional school');

END


-- Create hispanic table
IF OBJECT_ID('ref.hisp','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[hisp] (
		[hisp_id] tinyint NOT NULL,
		[hisp_desc] varchar(35) NOT NULL,
		CONSTRAINT pk_hisp PRIMARY KEY ([hisp_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[hisp]
VALUES	
	(1,'Not Spanish/Hispanic/Latino'),
	(2,'Mexican'),
	(3,'Puerto Rican'),
	(4,'Cuban'),
	(5,'Dominican'),
	(6,'Costa Rican'),
	(7,'Guatemalan'),
	(8,'Honduran'),
	(9,'Nicaraguan'),
	(10,'Panamanian'),
	(11,'Salvadoran'),
	(12,'Other Central American'),
	(13,'Argentinean'),
	(14,'Bolivian'),
	(15,'Chilean'),
	(16,'Colombian'),
	(17,'Ecuadorian'),
	(18,'Paraguayan'),
	(19,'Peruvian'),
	(20,'Uruguayan'),
	(21,'Venezuelan'),
	(22,'Other South American'),
	(23,'Spaniard'),
	(24,'All Other Spanish/Hispanic/Latino');

END


-- Create household income category table
IF OBJECT_ID('ref.hh_income_cat','U') IS NULL

BEGIN
CREATE TABLE 
	[ref].[hh_income_cat] (
		[hh_income_cat_id] tinyint NOT NULL,
		[hh_income_cat_desc] varchar(10) NOT NULL,
		CONSTRAINT pk_hhincomecat PRIMARY KEY ([hh_income_cat_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[hh_income_cat]
VALUES	
	(1,'< 30k'),
	(2,'30-60k'),
	(3,'60-100k'),
	(4,'100-150k'),
	(5,'150k+');

END


-- Create loc_choice table
IF OBJECT_ID('ref.loc_choice','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[loc_choice] (
		[loc_choice_id] tinyint NOT NULL,
		[loc_choice_desc] varchar(50) NOT NULL,
		CONSTRAINT pk_lc PRIMARY KEY ([loc_choice_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[loc_choice]
VALUES	
	(1,'Work Location Choice'),
	(2,'School Location Choice');

END


-- Create loc_choice_segment table
IF OBJECT_ID('ref.loc_choice_segment','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[loc_choice_segment] (
		[loc_choice_segment_id] int IDENTITY(1,1) NOT NULL,
		[loc_choice_id] tinyint NOT NULL,
		[loc_choice_segment_number] tinyint NOT NULL,
		[loc_choice_segment_desc] varchar(55) NOT NULL,
		CONSTRAINT pk_lcsegment PRIMARY KEY ([loc_choice_segment_id]),
		CONSTRAINT ixuq_lcsegment UNIQUE ([loc_choice_id],[loc_choice_segment_number]) WITH (DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_lcsegment_lc FOREIGN KEY ([loc_choice_id]) REFERENCES [ref].[loc_choice] ([loc_choice_id]) 
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[loc_choice_segment]
VALUES	
	(1,0,'Management Business Science and Arts Labor'),
	(1,1,'Services Labor'),
	(1,2,'Sales and Office Labor'),
	(1,3,'Natural Resources Construction and Maintenance Labor'),
	(1,4,'Production Transportation and Material Moving Labor'),
	(1,5,'Military Labor'),
	(1,99,'Work From Home'),
	(2,0,''),
	(2,1,''),
	(2,2,''),
	(2,3,''),
	(2,4,''),
	(2,5,''),
	(2,6,''),
	(2,7,''),
	(2,8,''),
	(2,9,''),
	(2,10,''),
	(2,11,''),
	(2,12,''),
	(2,13,''),
	(2,14,''),
	(2,15,''),
	(2,16,''),
	(2,17,''),
	(2,18,''),
	(2,19,''),
	(2,20,''),
	(2,21,''),
	(2,22,''),
	(2,23,''),
	(2,24,''),
	(2,25,''),
	(2,26,''),
	(2,27,''),
	(2,28,''),
	(2,29,''),
	(2,30,''),
	(2,31,''),
	(2,32,''),
	(2,33,''),
	(2,34,''),
	(2,35,''),
	(2,36,''),
	(2,37,''),
	(2,38,''),
	(2,39,''),
	(2,40,''),
	(2,41,''),
	(2,42,''),
	(2,43,''),
	(2,44,''),
	(2,45,''),
	(2,46,''),
	(2,47,''),
	(2,48,''),
	(2,49,''),
	(2,50,''),
	(2,51,''),
	(2,52,''),
	(2,53,''),
	(2,54,''),
	(2,55,''),
	(2,56,''),
	(2,88,'Home Schooled');
	
END


-- Create military table
IF OBJECT_ID('ref.military','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[military] (
		[military_id] tinyint NOT NULL,
		[military_desc] varchar(50) NOT NULL,
		CONSTRAINT pk_military PRIMARY KEY ([military_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[military]
VALUES	
	(0,'N/A Less than 17 Years Old'),
	(1,'Yes, Now on Active Duty'),
	(2,'Yes, on Active Duty in Past, but Not Now'),
	(3,'No, Training for Reserves/National Guard Only'),
	(4,'No, Never Served in the Military');

END



/* Create MGRA-TAZ xref table */
IF OBJECT_ID('ref.mgra13_xref_taz13','U') IS NULL
BEGIN

CREATE TABLE [ref].[mgra13_xref_taz13](
	[mgra_geography_zone_id] int NOT NULL,
	[taz_geography_zone_id] int NOT NULL,
	CONSTRAINT pk_mgra13xreftaz13 PRIMARY KEY ([mgra_geography_zone_id]),
	CONSTRAINT fk_mgra13xreftaz13_mgra FOREIGN KEY ([mgra_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
	CONSTRAINT fk_mgra13xreftaz13_taz FOREIGN KEY ([taz_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) 
ON 
	[ref_fg]
WITH
	(DATA_COMPRESSION = PAGE);

INSERT INTO	
	[ref].[mgra13_xref_taz13]
SELECT
	mgra_zone.[geography_zone_id]
	,taz_zone.[geography_zone_id]
FROM 
	OPENQUERY([pila\sdgintdb],'SELECT [mgra], [taz13] FROM [data_cafe].[dbo].[xref_mgra_sr13]') AS xref
INNER JOIN
	[ref].[geography_zone] AS mgra_zone
ON
	mgra_zone.[geography_type_id] = 90
	AND xref.[mgra] = mgra_zone.[zone]
INNER JOIN
	[ref].[geography_zone] AS taz_zone
ON
	taz_zone.[geography_type_id] = 34
	AND xref.[taz13] = taz_zone.[zone]
	
END


-- Create mode table
IF OBJECT_ID('ref.mode','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[mode] (
		[mode_id] tinyint NOT NULL,
		[mode_desc] varchar(40) NOT NULL,
		CONSTRAINT pk_mode PRIMARY KEY ([mode_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[mode]
VALUES	
	(0,'Airport Passing-Through'),
	(1,'Auto SOV (Non-Toll)'),
	(2,'Auto SOV (Toll)'),
	(3,'Auto 2 Person (Non-Toll, Non-HOV)'),
	(4,'Auto 2 Person (Non-Toll, HOV)'),
	(5,'Auto 2 Person (Toll, HOV)'),
	(6,'Auto 3+ Person (Non-Toll, Non-HOV)'),
	(7,'Auto 3+ Person (Non-Toll, HOV)'),
	(8,'Auto 3+ Person (Toll, HOV)'),
	(9,'Walk'),
	(10,'Bike'),
	(11,'Walk-Local Bus'),
	(12,'Walk-Express Bus'),
	(13,'Walk-Bus Rapid Transit'),
	(14,'Walk-Light Rail'),
	(15,'Walk-Heavy Rail'),
	(16,'PNR-Local Bus'),
	(17,'PNR-Express Bus'),
	(18,'PNR-Bus Rapid Transit'),
	(19,'PNR-Light Rail'),
	(20,'PNR-Heavy Rail'),
	(21,'KNR-Local Bus'),
	(22,'KNR-Express Bus'),
	(23,'KNR-Bus Rapid Transit'),
	(24,'KNR-Light Rail'),
	(25,'KNR-Heavy Rail'),
	(26,'School Bus'),
	(27,'Taxi'),
	(28, 'Cross Border Drive Alone'),
	(29, 'Cross Border Shared 2'),
	(30, 'Cross Border Shared 3'),
	(31, 'Cross Border Walk'),
	(32, 'Light Heavy Duty Truck (Non-Toll)'),
	(33, 'Light Heavy Duty Truck (Toll)'),
	(34, 'Medium Heavy Duty Truck (Non-Toll)'),
	(35, 'Medium Heavy Duty Truck (Toll)'),
	(36, 'Heavy Heavy Duty Truck (Non-Toll)'),
	(37, 'Heavy Heavy Duty Truck (Toll)');

END


-- Create model type table
IF OBJECT_ID('ref.model_type','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[model_type] (
		[model_type_id] tinyint NOT NULL,
		[model_type_desc] varchar(25) NOT NULL,
		CONSTRAINT pk_modeltype PRIMARY KEY ([model_type_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[model_type]
VALUES	
	(0,'Individual'),
	(1,'Joint'),
	(2,'Visitor'),
	(3,'Internal-External'),
	(4,'Cross Border'),
	(5,'Airport'),
	(6,'Commercial Vehicle'),
	(7,'External-External'),
	(8,'External-Internal'),
	(9,'Truck');

END


-- Create poe table
IF OBJECT_ID('ref.poe','U') IS NULL
BEGIN

CREATE TABLE 
	#tt (
		[poe_id] tinyint NOT NULL,
		[poe_desc] varchar(20) NOT NULL,
		[mgra_13_entry] smallint NOT NULL,
		[mgra_13_return] smallint NOT NULL,
		[taz_13] smallint NOT NULL
	)

INSERT INTO 
	#tt
VALUES
	(0,'San Ysidro',7090,7090,1),
	(1,'Otay Mesa',7066,7066,2),
	(2,'Tecate',21895,21895,4),
	(3,'Otay Mesa East',7123,7123,3),
	(4,'Jacumba',22094,22094,5)

CREATE TABLE 
	[ref].[poe] (
		[poe_id] tinyint NOT NULL,
		[poe_desc] varchar(20) NOT NULL,
		[mgra_entry_geography_zone_id] int NOT NULL,
		[mgra_return_geography_zone_id] int NOT NULL,
		[taz_geography_zone_id] int NOT NULL,
		CONSTRAINT pk_poe PRIMARY KEY ([poe_id]),
		CONSTRAINT fk_poe_mgraentry FOREIGN KEY ([mgra_entry_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_poe_mgrareturn FOREIGN KEY ([mgra_return_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id]),
		CONSTRAINT fk_poe_taz FOREIGN KEY ([taz_geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO
	[ref].[poe]
SELECT
	[poe_id]
	,[poe_desc]
	,mgra_entry.[geography_zone_id]
	,mgra_return.[geography_zone_id]
	,taz.[geography_zone_id]
FROM
	#tt
INNER JOIN
	[ref].[geography_zone] AS mgra_entry
ON
	mgra_entry.[geography_type_id] = 90
	AND #tt.[mgra_13_entry] = mgra_entry.[zone]
INNER JOIN
	[ref].[geography_zone] AS mgra_return
ON
	mgra_return.[geography_type_id] = 90
	AND #tt.[mgra_13_return] = mgra_return.[zone]
INNER JOIN
	[ref].[geography_zone] AS taz
ON
	taz.[geography_type_id] = 34
	AND #tt.[taz_13] = taz.[zone]

DROP TABLE 
	#tt
	
END


-- Create employment table
IF OBJECT_ID('ref.pemploy','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[pemploy] (
		[pemploy_id] tinyint NOT NULL,
		[pemploy_desc] varchar(35) NOT NULL,
		CONSTRAINT pk_pemploy PRIMARY KEY ([pemploy_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[pemploy]
VALUES	
	(1,'Employed Full-Time'),
	(2,'Employed Part-Time'),
	(3,'Unemployed or Not in Labor Force'),
	(4,'Less than 16 Years Old');		

END


-- Create person type table
IF OBJECT_ID('ref.pytpe','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[ptype] (
		[ptype_id] tinyint NOT NULL,
		[ptype_desc] varchar(25) NOT NULL,
		[age_range] varchar(10) NOT NULL,
		[work_status] varchar(20) NOT NULL,
		[school_status] varchar(20) NOT NULL,
		CONSTRAINT pk_ptype PRIMARY KEY ([ptype_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[ptype]
VALUES	
	(1,'Full-time Worker','18+','Full-time','None'),
	(2,'Part-time Worker','18+','Part-time','None'),
	(3,'College Student','18+','Any','College+'),
	(4,'Non-working Adult','18-64','Unemployed','None'),
	(5,'Non-working Senior','65+','Unemployed','None'),
	(6,'Driving Age Student','16-17','Any','Pre-college'),
	(7,'Non-driving Student','6-16','None','Pre-college'),
	(8,'Pre-school','0-5','None','None');

END


-- Create student table
IF OBJECT_ID('ref.pstudent','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[pstudent] (
		[pstudent_id] tinyint NOT NULL,
		[pstudent_desc] varchar(40) NOT NULL,
		CONSTRAINT pk_pstudent PRIMARY KEY ([pstudent_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[pstudent]
VALUES	
	(1,'Pre K-12'),
	(2,'College Undergrad+Grad and Prof. School'),
	(3,'Not Attending School');	

END
		

-- Create purpose table		
IF OBJECT_ID('ref.purpose','U') IS NULL
BEGIN

CREATE TABLE
	[ref].[purpose] (
		[purpose_id] int IDENTITY(1,1) NOT NULL,
		[model_type_id] tinyint NOT NULL,
		[purpose_number] tinyint NOT NULL,
		[purpose_desc] varchar(20) NOT NULL,
		CONSTRAINT pk_purpose PRIMARY KEY ([purpose_id]),
		CONSTRAINT ixuq_purpose UNIQUE ([model_type_id],[purpose_number]) WITH (DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_purpose_model FOREIGN KEY ([model_type_id]) REFERENCES [ref].[model_type] ([model_type_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[purpose]
VALUES	
	(0,0,'Work'),
	(0,1,'University'),
	(0,2,'School'),
	(0,3,'Escort'),
	(0,4,'Shop'),
	(0,5,'Maintenance'),
	(0,6,'Eating Out'),
	(0,7,'Visiting'),
	(0,8,'Discretionary'),
	(0,9,'Work-Based'),
	(0,10,'work related'),
	(0,11,'Home'),
	(1,0,'Escort'),
	(1,1,'Shop'),
	(1,2,'Maintenance'),
	(1,3,'Eating Out'),
	(1,4,'Visiting'),
	(1,5,'Discretionary'),
	(1,6,'Home'),
	(2,0,'Work'),
	(2,1,'Other'),
	(2,2,'Dining'),
	(2,3,'Return to Origin'),
	(3,0,'Home'),
	(3,1,'External'),
	(4,0,'Work'),
	(4,1,'School'),
	(4,2,'Cargo'),
	(4,3,'Shop'),
	(4,4,'Visit'),
	(4,5,'Other'),
	(4,6,'Return to Origin'),
	(5,0,'Resident-Business'),
	(5,1,'Resident-Personal'),
	(5,2,'Visitor-Business'),
	(5,3,'Visitor-Personal'),
	(5,4,'External'),
	(6,0,'None'),
	(7,0,'None'),
	(8,0,'Work'),
	(8,1,'Non-Work'),
	(9,0,'None');

END


-- Create race table
IF OBJECT_ID('ref.race','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[race] (
		[race_id] tinyint NOT NULL,
		[race_desc] varchar(150) NOT NULL,
		CONSTRAINT pk_race PRIMARY KEY ([race_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[race]
VALUES	
	(1,'White Alone'),
	(2,'Black or African American Alone'),
	(3,'American Indian Alone'),
	(4,'Alaska Native Alone'),
	(5,'American Indian and Alaska Native Tribes specified; or American Indian or Alaska Native, not specified and no other races'),
	(6,'Asian Alone'),
	(7,'Native Hawaiian and Other Pacific Islander Alone'),
	(8,'Some Other Race Alone'),
	(9,'Two or More Major Race Groups');

END


-- SCENARIO
IF OBJECT_ID('ref.scenario','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[scenario] (
		[scenario_id] smallint NOT NULL,
		[scenario_year] smallint NOT NULL,
		[scenario_desc] varchar(50) NOT NULL,
		[path] varchar(200) NOT NULL,
		[iteration] tinyint NOT NULL,
		[sample_rate] decimal(6,4) NOT NULL,
		[user_name] varchar(100) NOT NULL,
		[date_loaded] smalldatetime,
		[complete] bit,
		CONSTRAINT pk_scenario PRIMARY KEY ([scenario_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

END


-- Create sex table
IF OBJECT_ID('ref.sex','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[sex] (
		[sex_id] tinyint NOT NULL,
		[sex_desc] varchar(10) NOT NULL,
		CONSTRAINT pk_sex PRIMARY KEY ([sex_id])
	)
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[sex]
VALUES	
	(1,'Male'),
	(2,'Female');
			
END
		

-- Create skim table
IF OBJECT_ID('ref.skim','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[skim] (
		[skim_id] tinyint NOT NULL,
		[skim_desc] varchar(20) NOT NULL,
		CONSTRAINT pk_skim PRIMARY KEY ([skim_id])
	)
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[skim]
VALUES	
	(1,'Distance Actual'),
	(2,'Time Actual'),
	(3,'Cost Actual'),
	(4,'Time Perceived'),
	(5,'Gain');
			
END


-- Create time resolution table
IF OBJECT_ID('ref.time_resolution','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[time_resolution] (
		[time_resolution_id] tinyint NOT NULL,
		[time_resolution_desc] varchar(50) NOT NULL,
		CONSTRAINT pk_timeresolution PRIMARY KEY ([time_resolution_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[time_resolution]
VALUES
	(0,'Daily'),
	(1,'ABM 5 TOD.'),
	(2,'ABM half-hour period.');
	
END


-- Create time period table
IF OBJECT_ID('ref.time_period','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[time_period] (
		[time_period_id] int IDENTITY(1,1) NOT NULL,
		[time_resolution_id] tinyint NOT NULL,
		[time_period_number] tinyint NOT NULL,
		[time_period_start] time(0),
		[time_period_end] time(0)
		CONSTRAINT pk_timeperiod PRIMARY KEY ([time_period_id]),
		CONSTRAINT ixuq_timeperiod UNIQUE ([time_resolution_id],[time_period_number]) WITH (DATA_COMPRESSION = PAGE),
		CONSTRAINT fk_timeperiod_res FOREIGN KEY ([time_resolution_id]) REFERENCES [ref].[time_resolution] ([time_resolution_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[time_period]
VALUES
	(0,1,'0:00','23:59'),
	(1,1,'3:00','5:59'),
	(1,2,'6:00','8:59'),
	(1,3,'9:00','15:29'),
	(1,4,'15:30','18:59'),
	(1,5,'19:00','2:59'),	
	(2,0,NULL,NULL),
	(2,1,'4:30','4:59'),
	(2,2,'5:00','5:29'),
	(2,3,'5:30','5:59'),
	(2,4,'6:00','6:29'),
	(2,5,'6:30','6:59'),
	(2,6,'7:00','7:29'),
	(2,7,'7:30','7:59'),
	(2,8,'8:00','8:29'),
	(2,9,'8:30','8:59'),
	(2,10,'9:00','9:29'),
	(2,11,'9:30','9:59'),
	(2,12,'10:00','10:29'),
	(2,13,'10:30','10:59'),
	(2,14,'11:00','11:29'),
	(2,15,'11:30','11:59'),
	(2,16,'12:00','12:29'),
	(2,17,'12:30','12:59'),
	(2,18,'13:00','13:29'),
	(2,19,'13:30','13:59'),
	(2,20,'14:00','14:29'),
	(2,21,'14:30','14:59'),
	(2,22,'15:00','15:29'),
	(2,23,'15:30','15:59'),
	(2,24,'16:00','16:29'),
	(2,25,'16:30','16:59'),
	(2,26,'17:00','17:29'),
	(2,27,'17:30','17:59'),
	(2,28,'18:00','18:29'),
	(2,29,'18:30','18:59'),
	(2,30,'19:00','19:29'),
	(2,31,'19:30','19:59'),
	(2,32,'20:00','20:29'),
	(2,33,'20:30','20:59'),
	(2,34,'21:00','21:29'),
	(2,35,'21:30','21:59'),
	(2,36,'22:00','22:29'),
	(2,37,'22:30','22:59'),
	(2,38,'23:00','23:29'),
	(2,39,'23:30','23:59'),
	(2,40,'0:00','0:30')

END


-- Create tour category table		
IF OBJECT_ID('ref.tour_cat','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[tour_cat] (
		[tour_cat_id] tinyint NOT NULL,
		[tour_cat_desc] varchar(35) NOT NULL,
		CONSTRAINT pk_tourcat PRIMARY KEY ([tour_cat_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[tour_cat]
VALUES	
	(0,'Mandatory'),
	(1,'Non-Mandatory'),
	(2,'At-Work'),
	(3,'Business'),
	(4,'Personal');

END
		
	
-- Create transit access mode table	
IF OBJECT_ID('ref.transit_access_mode','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[transit_access_mode] (
		[transit_access_mode_id] tinyint NOT NULL,
		[transit_access_mode_desc] varchar(25) NOT NULL,
		CONSTRAINT pk_transitaccessmode PRIMARY KEY ([transit_access_mode_id])
	)
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[transit_access_mode]
VALUES	
	(1,'Walk to Transit'),
	(2,'Park and Ride to Transit'),
	(3,'Kiss and Ride to TRansit');

END


-- Create transit mode table
IF OBJECT_ID('ref.transit_mode','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[transit_mode] (
		[transit_mode_id] tinyint NOT NULL,
		[transit_mode_desc] varchar(25) NOT NULL,
		CONSTRAINT pk_transitmode PRIMARY KEY ([transit_mode_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[transit_mode]
VALUES	
	(4,'Commuter Rail'),
	(5,'Light Rail'),
	(6,'Regional BRT (Yellow)'),
	(7,'Regional BRT (Red)'),
	(8,'Limited Express Bus'),
	(9,'Express Bus'),
	(10,'Local Bus');	

END
	

-- Create unit type table
IF OBJECT_ID('ref.unit_type','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[unit_type] (
		[unit_type_id] tinyint NOT NULL,
		[unit_type_desc] varchar(40) NOT NULL,
		CONSTRAINT pk_unittype PRIMARY KEY ([unit_type_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);

INSERT INTO 
	[ref].[unit_type]
VALUES	
	(0,'Household'),
	(1,'Non-Institutional Group Quarterss'),
	(2,'Institutional Group Quarters');	

END	


-- Create weeks worked table
IF OBJECT_ID('ref.weeks_worked','U') IS NULL
BEGIN

CREATE TABLE 
	[ref].[weeks_worked] (
		[weeks_worked_id] tinyint NOT NULL,
		[weeks_worked_desc] varchar(100) NOT NULL,
		CONSTRAINT pk_weeks PRIMARY KEY ([weeks_worked_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
	
INSERT INTO 
	[ref].[weeks_worked]
VALUES
	(0,'N/A (less than 16 years old/did not work in past 12 months)'),	
	(1,'50 to 52 weeks'),
	(2,'48 to 49 weeks'),
	(3,'40 to 47 weeks'),
	(4,'27 to 39 weeks'),
	(5,'14 to 26 weeks'),
	(6,'13 weeks or less');

END
		
		



