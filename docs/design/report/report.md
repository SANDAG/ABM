# Reporting Framework

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


Links to maps

<!-- Dropdown menu to select HTML files -->
<select id="htmlSelector">
    <option value="html/beach_bike_ebike_30min_MD.html">beach_bike_ebike_30min_MD</option>
    <option value="html/beach_bike_ebike_45min_MD.html">beach_bike_ebike_45min_MD</option>
    <option value="html/beach_drive_30min_MD.html">beach_drive_30min_MD</option>
    <option value="html/beach_drive_45min_MD.html">beach_drive_45min_MD</option>
    <option value="html/beach_microtransit_nev_30min_MD.html">beach_microtransit_nev_30min_MD</option>
    <option value="html/beach_microtransit_nev_45min_MD.html">beach_microtransit_nev_45min_MD</option>
    <option value="html/beach_transit_30min_MD.html">beach_transit_30min_MD</option>
    <option value="html/beach_transit_45min_MD.html">beach_transit_45min_MD</option>
    <option value="html/beach_walk_30min_MD.html">beach_walk_30min_MD</option>
    <option value="html/beach_walk_45min_MD.html">beach_walk_45min_MD</option>
    <option value="html/emp_all_tier_transit_30min_AM.html">emp_all_tier_transit_30min_AM</option>
    <option value="html/emp_all_tier_transit_45min_AM.html">emp_all_tier_transit_45min_AM</option>
    <option value="html/emp_tier1_transit_30min_AM.html">emp_tier1_transit_30min_AM</option>
    <option value="html/emp_tier1_transit_45min_AM.html">emp_tier1_transit_45min_AM</option>
    <option value="html/emp_tier2_transit_30min_AM.html">emp_tier2_transit_30min_AM</option>
    <option value="html/emp_tier2_transit_45min_AM.html">emp_tier2_transit_45min_AM</option>
    <option value="html/high_school_transit_30min_AM.html">high_school_transit_30min_AM</option>
    <option value="html/higher_edu_transit_30min_AM.html">higher_edu_transit_30min_AM</option>
    <option value="html/higher_edu_transit_45min_AM.html">higher_edu_transit_45min_AM</option>
    <option value="html/medical_bike_ebike_30min_MD.html">medical_bike_ebike_30min_MD</option>
    <option value="html/medical_drive_30min_MD.html">medical_drive_30min_MD</option>
    <option value="html/medical_microtransit_nev_30min_MD.html">medical_microtransit_nev_30min_MD</option>
    <option value="html/medical_transit_30min_MD.html">medical_transit_30min_MD</option>
    <option value="html/medical_walk_30min_MD.html">medical_walk_30min_MD</option>
    <option value="html/parks_bike_ebike_15min_MD.html">parks_bike_ebike_15min_MD</option>
    <option value="html/parks_drive_15min_MD.html">parks_drive_15min_MD</option>
    <option value="html/parks_microtransit_nev_15min_MD.html">parks_microtransit_nev_15min_MD</option>
    <option value="html/parks_transit_15min_MD.html">parks_transit_15min_MD</option>
    <option value="html/parks_walk_15min_MD.html">parks_walk_15min_MD</option>
    <option value="html/retail_bike_ebike_15min_MD.html">retail_bike_ebike_15min_MD</option>
    <option value="html/retail_drive_15min_MD.html">retail_drive_15min_MD</option>
    <option value="html/retail_microtransit_nev_15min_MD.html">retail_microtransit_nev_15min_MD</option>
    <option value="html/retail_transit_15min_MD.html">retail_transit_15min_MD</option>
    <option value="html/retail_walk_15min_MD.html">retail_walk_15min_MD</option>
</select>


<!-- Button to open the selected file in a new tab -->
<button id="openFileButton" style="
    padding: 10px 20px;
    border: 2px solid #4CAF50;
    background-color: #4CAF50;
    color: white;
    font-size: 16px;
    cursor: pointer;
    border-radius: 5px;
    transition: background-color 0.3s, border-color 0.3s;
">
    Open in New Tab
</button>

<script>
    document.getElementById('openFileButton').addEventListener('click', function() {
        var selectedFile = document.getElementById('htmlSelector').value;
        window.open(selectedFile, '_blank');  // Open the selected HTML file in a new tab
    });
</script>
