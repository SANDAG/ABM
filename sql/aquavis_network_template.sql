IF NOT EXISTS (SELECT TOP 1 * FROM [abm].[aquavis_network] WHERE [SCENARIO_ID] = @@SCENARIO@@)
BEGIN

WITH b AS
(     
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SOV_PAY' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SOV_PAY) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SOV_PAY > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SOV_GP' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SOV_GP) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SOV_GP > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR2_GP' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR2_GP) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR2_GP > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR2_HOV' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR2_HOV) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR2_HOV > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR2_PAY' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR2_PAY) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR2_PAY > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR3_GP' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR3_GP) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR3_GP > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR3_HOV' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR3_HOV) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR3_HOV > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'SR3_PAY' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_SR3_PAY) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_SR3_PAY > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'LHDN' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_LHDN) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_LHDN > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'MHDN' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_MHDN) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_MHDN > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'HHDN' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_HHDN) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_HHDN > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'LHDT' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_LHDT) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_LHDT > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'MHDT' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_MHDT) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_MHDT > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'HHDT' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(l.FLOW_HHDT) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND l.FLOW_HHDT > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID     
UNION ALL
SELECT
    @@SCENARIO@@ as scenario
      ,t.ANODE as from_node
      ,t.BNODE AS to_node
      ,t.length_mile AS length
      ,t.ihov AS link_class
      ,l.TOD_ID AS time_period
      ,'UBUS' AS vehicle_class
      ,SUM(l.DIST_T) / SUM(l.VHT) assigned_speed
      ,SUM(t.PRELOAD) as volume
      ,'SD' as region
FROM
      [abm].[HWY_LOAD] l
      JOIN [abm].[HWY_TCAD] t ON l.SCENARIO_ID = t.SCENARIO_ID AND l.HWYCOV_ID = t.HWYCOV_ID AND l.TOD_ID = t.TOD_ID AND l.AB = t.AB
WHERE
      l.SCENARIO_ID = @@SCENARIO@@
      AND l.VHT > 0
      AND t.PRELOAD > 0
GROUP BY
      t.ANODE, t.BNODE, t.length_mile, t.ihov, l.TOD_ID
)

INSERT INTO [abm].[aquavis_network]
SELECT
      *
FROM
      b

END
