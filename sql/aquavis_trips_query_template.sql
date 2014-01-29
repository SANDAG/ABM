SELECT [SCENARIO_ID]
      ,[origin_zone]
      ,[destination_zone]
      ,[hour]
      ,[time_period]
      ,[vehicle_class]
      ,[trips]
  FROM [abm].[aquavis_trips]
  WHERE [SCENARIO_ID] = @@SCENARIO@@ and origin_zone=destination_zone
