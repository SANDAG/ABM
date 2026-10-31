# Model Performance

## Runtime Summary

The table below summarizes runtime for the **2040 forecast-year scenario (scenario ID 584)**. Timings are taken from `output/runtime_summary.csv` for this run. Runtime can vary with scenario configuration, model version, hardware, and execution conditions. The totals below exclude the post-run copy of project data to the remote drive (2:22:51), since model execution is complete before that step.

**Total Model Run Time (excluding post-run copy): 24:09:21**

### Pre-Processing

| Step | Runtime |
|------|---------|
| Check free space on C | 0:00:01 |
| Copy project data to local drive | 0:00:15 |
| Setup and initialization | 0:43:32 |

### Iteration 1 — 7:37:17

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:36:58 |
| Transit assignments and skims | 0:59:19 |
| Converting skims to omxz format | 0:05:26 |
| Export results for transponder ownership model | 0:00:12 |
| Running Scenario Management | 0:00:06 |
| Creating all the required files to run the ActivitySim models | 0:04:17 |
| Running ActivitySim resident model | 2:16:32 |
| Running ActivitySim airport models | 0:29:50 |
| Running ActivitySim wait time models | 0:52:38 |
| Running ActivitySim crossborder model | 0:20:02 |
| Running ActivitySim visitor model | 0:22:45 |
| Python Taxi and TNC routing model + AV and TNC matrix builder | 0:00:51 |
| Commercial vehicle model establishment synthesis | 0:28:57 |
| Commercial vehicle model | 0:28:00 |
| Heavy truck model | 0:26:46 |
| External-internal model | 0:01:01 |
| External-external model | 0:00:09 |
| Create TOD auto trip tables | 0:03:17 |

### Iteration 2 — 6:00:26

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:38:30 |
| Transit assignments and skims | 0:42:31 |
| Converting skims to omxz format | 0:05:22 |
| Export results for transponder ownership model | 0:00:14 |
| Creating all the required files to run the ActivitySim models | 0:01:06 |
| Running ActivitySim resident model | 2:31:55 |
| Running ActivitySim airport models | 0:30:53 |
| Running ActivitySim crossborder model | 0:22:39 |
| Running ActivitySim visitor model | 0:32:23 |
| Python Taxi and TNC routing model + AV and TNC matrix builder | 0:01:20 |
| Commercial vehicle model | 0:29:58 |
| Create TOD auto trip tables | 0:03:28 |

### Iteration 3 — 6:52:59

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:38:04 |
| Transit assignments and skims | 0:42:16 |
| Converting skims to omxz format | 0:05:25 |
| Export results for transponder ownership model | 0:00:13 |
| Creating all the required files to run the ActivitySim models | 0:01:10 |
| Running ActivitySim resident model | 3:18:39 |
| Running ActivitySim airport models | 0:32:41 |
| Running ActivitySim crossborder model | 0:29:44 |
| Running ActivitySim visitor model | 0:26:40 |
| Python Taxi and TNC routing model + AV and TNC matrix builder | 0:02:49 |
| Commercial vehicle model | 0:31:31 |
| Create TOD auto trip tables | 0:03:42 |

### Iteration 4 (Final) — 2:55:14

| Step | Runtime |
|------|---------|
| Final traffic assignments | 0:40:19 |
| Create TOD transit trip tables | 0:03:58 |
| Final transit assignments | 0:42:39 |
| Export network results for Data Loader | 0:24:30 |
| Export matrices for Data Loader | 0:06:51 |
| Exporting highway shapefile | 0:00:10 |
| Exporting MGRA-level travel times | 0:38:00 |
| Deleting OMXZ skims | 0:00:06 |
| Writing model output to datalake | 0:18:41 |
