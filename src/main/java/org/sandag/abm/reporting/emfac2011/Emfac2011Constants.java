package org.sandag.abm.reporting.emfac2011;

/**
 * The {@code AquavisUtils} class provides utilities for use with the AquaVis
 * model framework.
 * 
 * @author crf Started 2/8/12 1:11 PM
 */
public final class Emfac2011Constants
{
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

    private Emfac2011Constants()
    {
    }
}
