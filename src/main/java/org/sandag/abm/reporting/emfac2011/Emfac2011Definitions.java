package org.sandag.abm.reporting.emfac2011;

/**
 * 
 * @author Wu.Sun@sandag.org 1/20/2014
 * 
 */

public class Emfac2011Definitions {
   	
	//Aquavis table content defintions
    public static final String AQUAVIS_NETWORK_FROM_NODE_FIELD      = "from_node";
    public static final String AQUAVIS_NETWORK_TO_NODE_FIELD        = "to_node";
    public static final String AQUAVIS_NETWORK_LENGTH_FIELD         = "length";
    public static final String AQUAVIS_NETWORK_LINK_CLASS_FIELD     = "link_class";
    public static final String AQUAVIS_NETWORK_TIME_PERIOD_FIELD    = "time_period";
    public static final String AQUAVIS_NETWORK_VEHICLE_CLASS_FIELD  = "vehicle_class";
    public static final String AQUAVIS_NETWORK_SPEED_FIELD          = "assigned_speed";
    public static final String AQUAVIS_NETWORK_VOLUME_FIELD         = "volume";
    public static final String AQUAVIS_NETWORK_REGION_FIELD         = "region";

    public static final String AQUAVIS_INTRAZONAL_ZONE_FIELD        = "zone";
    public static final String AQUAVIS_INTRAZONAL_LENGTH_FIELD      = "distance";
    public static final String AQUAVIS_INTRAZONAL_SPEED_FIELD       = "speed";
    public static final String AQUAVIS_INTRAZONAL_REGION_FIELD      = "region";
    public static final String AQUAVIS_INTRAZONAL_AREA_TYPE_FIELD   = "area_type";

    public static final String AQUAVIS_TRIPS_ORIGIN_ZONE_FIELD      = "origin_zone";
    public static final String AQUAVIS_TRIPS_DESTINATION_ZONE_FIELD = "destination_zone";
    public static final String AQUAVIS_TRIPS_HOUR_FIELD             = "hour";
    public static final String AQUAVIS_TRIPS_TIME_PERIOD_FIELD      = "time_period";
    public static final String AQUAVIS_TRIPS_VEHICLE_CLASS_FIELD    = "vehicle_class";
    public static final String AQUAVIS_TRIPS_TRIPS_FIELD            = "trips";

	public static final String VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN = "EMFAC2011_MODE";

	// Emfac2011 Data Table Definitions
	public static final String EMFAC_2011_DATA_SUB_AREA_FIELD = "subarea";
	public static final String EMFAC_2011_DATA_SPEED_FIELD = "speed";
	public static final String EMFAC_2011_DATA_VEHICLE_TYPE_FIELD = "vehicle_type";
	public static final String EMFAC_2011_DATA_VMT_FIELD = "vmt";
	public static final String EMFAC_2011_DATA_SPEED_FRACTION_FIELD = "fraction";

	// Emfac2011 Excel Input Sheets definitions
	public static final String EMFAC_2011_SCENARIO_TABLE_NAME = "Regional_Scenarios";
	public static final String EMFAC_2011_SCENARIO_TABLE_GROUP_FIELD = "Group";
	public static final String EMFAC_2011_SCENARIO_TABLE_AREA_TYPE_FIELD = "Area Type";
	public static final String EMFAC_2011_SCENARIO_TABLE_AREA_FIELD = "Area";
	public static final String EMFAC_2011_SCENARIO_TABLE_YEAR_FIELD = "CalYr";
	public static final String EMFAC_2011_SCENARIO_TABLE_SEASON_FIELD = "Season";

	public static final String EMFAC_2011_VMT_TABLE_NAME = "Scenario_Base_Inputs";
	public static final String EMFAC_2011_VMT_TABLE_GROUP_FIELD = "Group";
	public static final String EMFAC_2011_VMT_TABLE_AREA_FIELD = "Area";
	public static final String EMFAC_2011_VMT_TABLE_SCENARIO_FIELD = "Scenario";
	public static final String EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD = "Sub-Area";
	public static final String EMFAC_2011_VMT_TABLE_YEAR_FIELD = "CalYr";
	public static final String EMFAC_2011_VMT_TABLE_SEASON_FIELD = "Season";
	public static final String EMFAC_2011_VMT_TABLE_TITLE_FIELD = "Title";
	public static final String EMFAC_2011_VMT_TABLE_VMT_PROFILE_FIELD = "VMT Profile";
	public static final String EMFAC_2011_VMT_TABLE_VMT_BY_VEH_FIELD = "VMT by Vehicle Category";
	public static final String EMFAC_2011_VMT_TABLE_SPEED_PROFILE_FIELD = "Speed Profile";
	public static final String EMFAC_2011_VMT_TABLE_VMT_FIELD = "New Total VMT";

	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_NAME = "Scenario_VMT_by_VehCat";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_GROUP_FIELD = "Group";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_AREA_FIELD = "Area";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_SCENARIO_FIELD = "Scenario";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD = "Sub-Area";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_YEAR_FIELD = "CalYr";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_SEASON_FIELD = "Season";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_TITLE_FIELD = "Title";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD = "Veh & Tech";
	public static final String EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD = "New VMT";

	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_NAME = "Scenario_Speed_Profiles";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_GROUP_FIELD = "Group";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_AREA_FIELD = "Area";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_SCENARIO_FIELD = "Scenario";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD = "Sub-Area";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_YEAR_FIELD = "CalYr";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_SEASON_FIELD = "Season";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_TITLE_FIELD = "Title";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD = "Veh & Tech";
	public static final String EMFAC_2011_SPEED_FRACTION_TABLE_2007_VEHICLE_FIELD = "EMFAC2007 Veh & Tech";
}
