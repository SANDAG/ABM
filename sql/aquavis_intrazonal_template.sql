IF NOT EXISTS (SELECT TOP 1 * FROM [abm].[aquavis_intrazonal] WHERE [SCENARIO_ID] = @@SCENARIO@@)
BEGIN

INSERT INTO [abm].[aquavis_intrazonal]
SELECT	@@SCENARIO@@
		,[ORIG_TAZ] AS zone
		,[DIST_DRIVE_ALONE_FREE] AS distance
		,60*[DIST_DRIVE_ALONE_FREE]/[TIME_DRIVE_ALONE_FREE] AS speed
		,'SD' AS region
		,'urban' AS area_type
	FROM [abm].[TAZSKIM]
	WHERE [TAZSKIM].[SCENARIO_ID] = @@SCENARIO@@
	AND ORIG_TAZ > 12 AND [TOD_ID]= 3 AND [ORIG_TAZ]=[DEST_TAZ]

INSERT INTO [abm].[aquavis_intrazonal]
VALUES	(@@SCENARIO@@,1,15,35,'SD','urban'),
		(@@SCENARIO@@,2,15,35,'SD','urban'),
		(@@SCENARIO@@,3,15,35,'SD','urban'),
		(@@SCENARIO@@,4,15,35,'SD','urban'),
		(@@SCENARIO@@,5,15,35,'SD','urban'),
		(@@SCENARIO@@,6,15,35,'SD','urban'),
		(@@SCENARIO@@,7,15,35,'SD','urban'),
		(@@SCENARIO@@,8,15,35,'SD','urban'),
		(@@SCENARIO@@,9,15,35,'SD','urban'),
		(@@SCENARIO@@,10,15,35,'SD','urban'),
		(@@SCENARIO@@,11,15,35,'SD','urban'),
		(@@SCENARIO@@,12,15,35,'SD','urban');

END


