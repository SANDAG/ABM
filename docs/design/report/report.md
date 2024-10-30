# Reporting Framework

<<<<<<< HEAD
**Reporting Process Overview:**

1. **ABM3 model output files are stored to data lake:**
    - Model outputs are written to the data lake immediately following ABM3 model run completion.
     - Output CSV files are converted to Parquet format before writing to the data lake.
    - Each model run is assigned a unique scenario ID.

2. **Data lake files are loaded to Delta tables:**
    - Each output file in the data lake is loaded into its corresponding Delta table. For example, the trips output file is loaded into the trips Delta table, the persons output file is loaded into the persons Delta table, etc.
    - Delta tables store the results from all model runs, organized by scenario ID.

3. **Delta Tables are processed in Databricks:**
    - Delta tables are read, transformed, and aggregated as needed to support analysis and reporting requirements.
    - Once transformations are complete, the resulting data is written back to the data lake as new Delta tables or used to update existing tables.
    - These new Delta tables are also organized by scenario ID, making it easier to manage and query specific versions of processed data.

4. **Delta tables are ingested by Power BI:**
    - Power BI reads the data from the Delta tables.
    - Power BI report templates with various metrics of interest are automatically refreshed with new model run outputs.
    - Metrics can easily be compared across different scenario IDs.
=======
Details of reporting components.
>>>>>>> 188e3fd0 (Merge docs folders from ABM3_develop)
