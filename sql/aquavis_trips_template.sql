IF NOT EXISTS (SELECT TOP 1 * FROM [abm].[aquavis_trips] WHERE [SCENARIO_ID] = @@SCENARIO@@)
BEGIN

 
SELECT	@@SCENARIO@@ AS SCENARIO_ID
		,[mgra1].[TAZ] AS origin_zone
		,[mgra2].[TAZ] AS destination_zone
		,DATEPART(hh,[PERIOD_START]) AS [hour]
		,CASE	WHEN TOD_ID = 1 THEN 'EA'
				WHEN TOD_ID = 2 THEN 'AM'
				WHEN TOD_ID = 3 THEN 'MD'
				WHEN TOD_ID = 4 THEN 'PM'
				WHEN TOD_ID = 5 THEN 'EV'
				END AS time_period
		,CASE	WHEN [MODE_ID] = 1 THEN 'SOV_GP'
				WHEN [MODE_ID] = 2 THEN 'SOV_PAY'
				WHEN [MODE_ID] = 3 THEN 'SR2_GP'
				WHEN [MODE_ID] = 4 THEN 'SR2_HOV'
				WHEN [MODE_ID] = 5 THEN 'SR2_PAY'
				WHEN [MODE_ID] = 6 THEN 'SR3_GP'
				WHEN [MODE_ID] = 7 THEN 'SR3_HOV'
				WHEN [MODE_ID] = 8 THEN 'SR3_GP'
				WHEN [MODE_ID] = 27 THEN 'SR2_HOV'
				END AS vehicle_class
		,COUNT(*) AS trips
	INTO [dbo].[aquavis_trips_temp]
	FROM [abm].[TRIP_MICRO_SIMUL]
	INNER JOIN [abm].[MGRA] mgra1
	ON [TRIP_MICRO_SIMUL].[ORIG_MGRA] = mgra1.[MGRA]
	INNER JOIN [abm].[MGRA] mgra2
	ON [TRIP_MICRO_SIMUL].[DEST_MGRA] = mgra2.[MGRA]
	INNER JOIN [ref].[PERIOD]
	ON [TRIP_MICRO_SIMUL].[PERIOD_ID] = [PERIOD].[PERIOD_ID]
	WHERE [TRIP_MICRO_SIMUL].[SCENARIO_ID] = @@SCENARIO@@
	AND mgra1.[SCENARIO_ID] = @@SCENARIO@@
	AND mgra2.[SCENARIO_ID] = @@SCENARIO@@ 
	AND [MODE_ID] IN (1,2,3,4,5,6,7,8,27)
	AND [mgra1].[TAZ] = [mgra2].[TAZ]
	GROUP BY [mgra1].[TAZ],[mgra2].[TAZ],DATEPART(hh,[PERIOD_START]),[TOD_ID],[MODE_ID]
	
	

INSERT INTO [dbo].[aquavis_trips_temp]
SELECT	@@SCENARIO@@
		,[MGRA].[TAZ] AS origin_zone
		,[TAP].[TAZ] AS destination_zone
		,DATEPART(hh,[PERIOD_START]) AS [hour]
		,CASE	WHEN TOD_ID = 1 THEN 'EA'
				WHEN TOD_ID = 2 THEN 'AM'
				WHEN TOD_ID = 3 THEN 'MD'
				WHEN TOD_ID = 4 THEN 'PM'
				WHEN TOD_ID = 5 THEN 'EV'
				END AS time_period
		,CASE	WHEN [PARTYSIZE] = 1 THEN 'SOV_GP'
				WHEN [PARTYSIZE] = 2 THEN 'SR2_GP'
				ELSE 'SR3_GP'
				END AS vehicle_class
		,COUNT(*) AS trips
	FROM [abm].[TRIP_MICRO_SIMUL]
	INNER JOIN [abm].[MGRA]
	ON [TRIP_MICRO_SIMUL].[ORIG_MGRA] = [MGRA].[MGRA]
	INNER JOIN [abm].[TAP]
	ON [TRIP_MICRO_SIMUL].[TRIP_BOARD_TAP] = [TAP].[TAP]
	INNER JOIN [ref].[PERIOD]
	ON [TRIP_MICRO_SIMUL].[PERIOD_ID] = [PERIOD].[PERIOD_ID]
	WHERE [TRIP_MICRO_SIMUL].[SCENARIO_ID] = @@SCENARIO@@
	AND [MGRA].[SCENARIO_ID] = @@SCENARIO@@
	AND [TAP].[SCENARIO_ID] = @@SCENARIO@@
	AND [MODE_ID] BETWEEN 16 AND 25
	AND [MGRA].[TAZ] = [TAP].[TAZ]
	GROUP BY [MGRA].[TAZ],[TAP].[TAZ],DATEPART(hh,[PERIOD_START]),[TOD_ID],[PARTYSIZE]
	
	
INSERT INTO [dbo].[aquavis_trips_temp]
SELECT	@@SCENARIO@@
		,[TAP].[TAZ] AS origin_zone
		,[MGRA].[TAZ] AS destination_zone
		,DATEPART(hh,[PERIOD_START]) AS [hour]
		,CASE	WHEN TOD_ID = 1 THEN 'EA'
				WHEN TOD_ID = 2 THEN 'AM'
				WHEN TOD_ID = 3 THEN 'MD'
				WHEN TOD_ID = 4 THEN 'PM'
				WHEN TOD_ID = 5 THEN 'EV'
				END AS time_period
		,CASE	WHEN [PARTYSIZE] = 1 THEN 'SOV_GP'
				WHEN [PARTYSIZE] = 2 THEN 'SR2_GP'
				ELSE 'SR3_GP'
				END AS vehicle_class
		,COUNT(*) AS trips
	FROM [abm].[TRIP_MICRO_SIMUL]
	INNER JOIN [abm].[MGRA]
	ON [TRIP_MICRO_SIMUL].[DEST_MGRA] = [MGRA].[MGRA]
	INNER JOIN [abm].[TAP]
	ON [TRIP_MICRO_SIMUL].[TRIP_ALIGHT_TAP] = [TAP].[TAP]
	INNER JOIN [ref].[PERIOD]
	ON [TRIP_MICRO_SIMUL].[PERIOD_ID] = [PERIOD].[PERIOD_ID]
	WHERE [TRIP_MICRO_SIMUL].[SCENARIO_ID] = @@SCENARIO@@
	AND [MGRA].[SCENARIO_ID] = @@SCENARIO@@
	AND [TAP].[SCENARIO_ID] = @@SCENARIO@@
	AND [MODE_ID] BETWEEN 16 AND 25
	AND [MGRA].[TAZ] = [TAP].[TAZ]
	GROUP BY [MGRA].[TAZ],[TAP].[TAZ],DATEPART(hh,[PERIOD_START]),[TOD_ID],[PARTYSIZE]
	
	
	
INSERT INTO [dbo].[aquavis_trips_temp] 
SELECT	@@SCENARIO@@
		,[ORIG_TAZ] AS origin_zone
		,[DEST_TAZ] AS destination_zone
		,-1 AS [hour]
		,CASE	WHEN TOD_ID = 1 THEN 'EA'
				WHEN TOD_ID = 2 THEN 'AM'
				WHEN TOD_ID = 3 THEN 'MD'
				WHEN TOD_ID = 4 THEN 'PM'
				WHEN TOD_ID = 5 THEN 'EV'
				END AS time_period
		,CASE	WHEN [MODE_ID] = 1 THEN 'SOV_GP'
				WHEN [MODE_ID] = 2 THEN 'SOV_PAY'
				WHEN [MODE_ID] = 3 THEN 'SR2_GP'
				WHEN [MODE_ID] = 4 THEN 'SR2_HOV'
				WHEN [MODE_ID] = 5 THEN 'SR2_PAY'
				WHEN [MODE_ID] = 6 THEN 'SR3_GP'
				WHEN [MODE_ID] = 7 THEN 'SR3_HOV'
				WHEN [MODE_ID] = 8 THEN 'SR3_GP' 
				END AS vehicle_class
		,[TRIPS] AS trips
	FROM [abm].[TRIP_AGGREGATE]
	WHERE [SCENARIO_ID] = @@SCENARIO@@
	AND [MODEL_TYPE_ID] IN (7,8)
	AND [ORIG_TAZ]=[DEST_TAZ]

INSERT INTO [abm].[aquavis_trips]
SELECT	@@SCENARIO@@ AS [SCENARIO_ID]
		,[origin_zone]
		,[destination_zone]
		,[hour]
		,[time_period]
		,[vehicle_class]
		,SUM([trips]) AS trips
FROM [dbo].[aquavis_trips_temp]
WHERE [SCENARIO_ID] = @@SCENARIO@@
GROUP BY [origin_zone],[destination_zone],[hour],[time_period],[vehicle_class]



/*
--add in bus and truck trips as zeros, since we don't know these
--only do intrazonal, as that is all that is needed for emfac
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,time_period
			,'UBUS' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'LHDN' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'MHDN' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'HHDN' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'LHDT' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'MHDT' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
              
INSERT INTO [abm].[aquavis_trips]
    SELECT	[SCENARIO_ID]
			,[origin_zone]
			,[destination_zone]
			,[hour]
			,[time_period]
			,'HHDT' AS vehicle_class
			,0 AS trips
    FROM [dbo].[aquavis_trips_temp]
    WHERE [aquavis_trips_temp].[vehicle_class]='SOV_GP' 
    AND [aquavis_trips_temp].[origin_zone] = [aquavis_trips_temp].[destination_zone]
        
 */    

DROP TABLE [dbo].[aquavis_trips_temp]

END