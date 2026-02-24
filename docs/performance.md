# Model Performance

## Runtime Summary

Full model runs usually take 36-40 hours depending on the scenario year. Here is a model runtime summary from a recent 2022 base year run.

**Total Run Time: 38:01:17**

### Pre-Processing

| Step | Runtime |
|------|---------|
| Check free space on C | 0:00:01 |
| Copy project data to local drive | 0:00:10 |
| Setup and initialization | 1:11:23 |

### Iteration 1 — 11:21:47

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:37:03 |
| Transit assignments and skims | 0:56:15 |
| Converting skims to omxz format | 0:05:10 |
| Export results for transponder ownership model | 0:00:12 |
| Running Scenario Management | 0:00:05 |
| Creating all required files to run the ActivitySim models | 0:04:05 |
| Running ActivitySim resident model | 2:38:29 |
| Running ActivitySim airport models | 0:35:13 |
| Running ActivitySim wait time models | 0:52:53 |
| Running ActivitySim crossborder model | 0:22:30 |
| Running ActivitySim visitor model | 0:29:59 |
| Start matrix manager | 0:00:09 |
| Java — AV allocation model and TNC routing model | 0:33:40 |
| Commercial vehicle model establishment synthesis | 0:26:37 |
| Commercial vehicle model | 3:15:43 |
| Heavy truck model | 0:18:06 |
| External-internal model | 0:01:06 |
| External-external model | 0:00:09 |
| Create TOD auto trip tables | 0:04:14 |

### Iteration 2 — 10:30:19

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:37:40 |
| Transit assignments and skims | 0:42:11 |
| Converting skims to omxz format | 0:05:10 |
| Export results for transponder ownership model | 0:00:11 |
| Creating all required files to run the ActivitySim models | 0:01:08 |
| Running ActivitySim resident model | 3:36:18 |
| Running ActivitySim airport models | 0:35:35 |
| Running ActivitySim crossborder model | 0:23:54 |
| Running ActivitySim visitor model | 0:29:10 |
| Start matrix manager | 0:00:09 |
| Java — AV allocation model and TNC routing model | 0:38:22 |
| Commercial vehicle model | 3:15:56 |
| Create TOD auto trip tables | 0:04:27 |

### Iteration 3 — 11:51:28

| Step | Runtime |
|------|---------|
| Traffic assignment and skims | 0:38:34 |
| Transit assignments and skims | 0:42:04 |
| Converting skims to omxz format | 0:05:08 |
| Export results for transponder ownership model | 0:00:12 |
| Creating all required files to run the ActivitySim models | 0:01:12 |
| Running ActivitySim resident model | 4:53:00 |
| Running ActivitySim airport models | 0:36:33 |
| Running ActivitySim crossborder model | 0:30:06 |
| Running ActivitySim visitor model | 0:34:08 |
| Start matrix manager | 0:00:09 |
| Java — AV allocation model and TNC routing model | 0:49:38 |
| Commercial vehicle model | 2:56:05 |
| Create TOD auto trip tables | 0:04:32 |

### Iteration 4 (Final) — 3:06:33

| Step | Runtime |
|------|---------|
| Final traffic assignments | 0:40:05 |
| Create TOD transit trip tables | 0:04:05 |
| Final transit assignments | 0:42:13 |
| Export network results for Data Loader | 0:18:23 |
| Export matrices for Data Loader | 0:07:00 |
| Exporting highway shapefile | 0:00:08 |
| Exporting MGRA-level travel times | 0:41:45 |
| Deleting OMXZ skims | 0:00:06 |
| Validation | 0:00:34 |
| Writing model output to datalake | 0:31:55 |
| Copy project data to remote drive | 0:00:19 |