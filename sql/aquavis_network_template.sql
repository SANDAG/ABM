IF NOT EXISTS (SELECT TOP 1 * FROM [abm].[aquavis_network] WHERE [SCENARIO_ID] = @@SCENARIO@@)
BEGIN

INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SOV_PAY' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SOV_PAY) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SOV_GP' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SOV_GP) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR2_GP' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR2_GP) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR2_HOV' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR2_HOV) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];

INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR2_PAY' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR2_PAY) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR3_GP' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR3_GP) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR3_HOV' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR3_HOV) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'SR3_PAY' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_SR3_PAY) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];

	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'LHDN' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_LHDN) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'MHDN' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_MHDN) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'HHDN' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_HHDN) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'LHDT' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_LHDT) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'MHDT' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_MHDT) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'HHDT' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(FLOW_HHDT) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];
	
INSERT INTO [abm].[aquavis_network]
SELECT	@@SCENARIO@@
		,MAX([ANODE]) AS from_node
		,MAX([BNODE]) AS to_node
		,MAX([length_mile]) AS length
		,MAX([ihov]) AS link_class
		,[HWY_LOAD].[TOD_ID] AS time_period
		,'UBUS' AS vehicle_class
		,(MAX([length_mile])/AVG(TIME)*60) AS assigned_speed
		,SUM(PRELOAD) AS volume
		,'SD' AS region
	FROM [abm].[HWY_LOAD]
	INNER JOIN [abm].[HWY_TCAD]
	ON [HWY_LOAD].[HWYCOV_ID] = [HWY_TCAD].[HWYCOV_ID]
	AND [HWY_LOAD].[TOD_ID] = [HWY_TCAD].[TOD_ID]
	AND [HWY_LOAD].[AB] = [HWY_TCAD].[AB]
	WHERE [HWY_LOAD].[SCENARIO_ID] = @@SCENARIO@@
	AND [HWY_TCAD].[SCENARIO_ID] = @@SCENARIO@@
	GROUP BY [HWY_LOAD].[HWYCOV_ID], [HWY_LOAD].[TOD_ID];

END