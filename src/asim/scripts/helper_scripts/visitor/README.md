# SANDAG Visitor Model ActivitySim Implementation

## To run
1. Install ActivitySim from the `ABM3_develop` branch of the [SANDAG fork](https://github.com/SANDAG/ABM/tree/ABM3_develop) (as of 06/02/2022)
2. Configure the preprocessor settings in **configs/preprocessing.yaml**
3. Run the preprocessor modules with `python visitor_model.py -s`
      - `-s` (setup configs) converts inputs from CTRAMP to ActivitySim format. Only needs to be run once.
4. Run tour enumeration: `python cross_border_model.py -t`
      - `-t` (tour enumeration) runs tour enumeration, only needs to be run once unless new results are desired.
      - If land use data does not yet have wait time columns (see below), you'll have to first run in preprocessing mode in order to generate the first set of wait times.
6. Configure the main ActivitySim settings in **configs/settings.yaml**

7. Run ActivitySim `python visitor_model.py -a`

## Helpful tips:
- You can execute any or all of the above processes at once by using multiple flags: `python visitor_model.py -s -t -a`
- Each time you run with tour enumeration mode activated (`-t`), the tours list `tours_visitor.csv` file is updated. Results can vary due to probabilistic estimation.

## Required inputs:

### ActivitySim
 - **land use** (e.g. "land_use.csv")
    - custom columns (created by preprocessor):
       - `hh`: Number of households per zone
       - `hotelroomtotal`: Number of hotel rooms per zone
       - `MAZ`: MAZ id
 - **tours** (e.g. "tours_visitor.csv")
    - tour_id, tour_type, visitor_travel_type, number_of_participants, auto_available, income, tour_category, tour_num, tour_count, origin, purpose_id, household_id, person_id, home_zone_id
    - hardcoded one person per tour
 - **households** (e.g. "households_xborder.csv")
   - list of successive integers from 0 to the number of tours (one household per tour)
 - **persons** (e.g. "persons_xborder.csv")
    - list of successive integers from 0 to the number of tours (one household per tour), and household_id
    - hardcoded one person per household
 - **skims**
    - traffic (e.g. "traffic_skims_\<TOD\>.omx")
    - transit (e.g. "transit_skims.omx")
    - transit access (e.g. "maz_tap_walk.csv", "taps.csv", and "tap_lines.csv")
    - microzone (e.g. "maz_maz_walk.csv")

### Preprocessor
The preprocessor is mainly designed to convert old CTRAMP-formatted input/survey data into the formats needed by ActivitySim.

| CTRAMP input filenames | ActivitySim inputs created| ActivitySim location | Description | Preprocessor operation |
|---|---|---|---|---|
|---|---|---|---|---|
