SELECT [SCENARIO_ID]
      ,[zone]
      ,[distance]
      ,[speed]
      ,[region]
      ,[area_type]
  FROM [abm].[aquavis_intrazonal]
  WHERE [SCENARIO_ID] = @@SCENARIO@@


