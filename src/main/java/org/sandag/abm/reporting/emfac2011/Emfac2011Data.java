package org.sandag.abm.reporting.emfac2011;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.TableIndex;
import com.pb.sawdust.tabledata.basic.BasicTableIndex;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.metadata.DataType;
import com.pb.sawdust.tabledata.metadata.TableSchema;
import com.pb.sawdust.tabledata.read.CsvTableReader;

/**
 * The {@code AbstractEmfac2011Data} class is used to provide data used to
 * modify the EMFAC2011 SG input file. It essentially reads in the generic
 * AquaVis data (which represents the travel demand model results) and refactors
 * it into a data table that is used (by {@link Emfac2011InputFileCreator}) to
 * create an adjusted EMFAC2011 input file.
 * 
 * @author crf Started 2/8/12 9:13 AM
 */
public abstract class Emfac2011Data
{
    private static final Logger LOGGER                               = LoggerFactory
                                                                             .getLogger(Emfac2011Data.class);

    /**
     * The name of the sub-area field used in the data table produced by this
     * class.
     */
    public static final String  EMFAC_2011_DATA_SUB_AREA_FIELD       = "subarea";

    /**
     * The name of the speed field used in the data table produced by this
     * class.
     */
    public static final String  EMFAC_2011_DATA_SPEED_FIELD          = "speed";

    /**
     * The name of the vehicle type field used in the data table produced by
     * this class.
     */
    public static final String  EMFAC_2011_DATA_VEHICLE_TYPE_FIELD   = "vehicle_type";

    /**
     * The name of the vmt field used in the data table produced by this class.
     */
    public static final String  EMFAC_2011_DATA_VMT_FIELD            = "vmt";

    /**
     * The name of the speed fraction field used in the data table produced by
     * this class.
     */
    public static final String  EMFAC_2011_DATA_SPEED_FRACTION_FIELD = "fraction";

    /**
     * Get a mapping from the vehicle types listed in a model's AquaVis results
     * to their corresponding EMFAC2011 vehicle types. The returned map should
     * have the (exact) names listed in the AquaVis results as keys, and the set
     * of EMFAC2011 vehicle types that the represent the AquaVis type. A single
     * EMFAC2011 may be used in the mappings of multiple AquaVis types (there is
     * no functional mapping requirement), and only mutable EMFAC2011 vehicle
     * types may be used in the mapping (see
     * {@link com.pb.aquavis.emfac2011.Emfac2011VehicleType#getMutableVehicleTypes()}
     * . Also, <i>all</i> aquavis vehicle types must be represented in the map
     * (even if it is an empty mapping).
     * 
     * @return a map representing the relationship between the AquaVis and
     *         EMFAC2011 vehicle types.
     */
    protected abstract Map<String, Set<Emfac2011VehicleType>> getAquavisVehicleTypeToEmfacTypeMapping();

    private Map<Emfac2011VehicleType, Map<String, Double>> buildVehicleFractioning(
            Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacTypeMapping)
    {
        // returns a map which says for every emfac vehicle type, what aquavis
        // vehicle types should have their vmt added
        // to it, and by what fraction

        Map<Emfac2011VehicleType, Map<String, Double>> vehicleFractionMap = new EnumMap<>(
                Emfac2011VehicleType.class);
        for (Emfac2011VehicleType type : Emfac2011VehicleType.getMutableVehicleTypes())
            vehicleFractionMap.put(type, new HashMap<String, Double>());
        for (String aquavisVehicleType : aquavisVehicleTypeToEmfacTypeMapping.keySet())
        {
            double fraction = 1.0 / aquavisVehicleTypeToEmfacTypeMapping.get(aquavisVehicleType)
                    .size();
            for (Emfac2011VehicleType type : aquavisVehicleTypeToEmfacTypeMapping
                    .get(aquavisVehicleType))
            {
                if (!vehicleFractionMap.containsKey(type))
                    throw new IllegalStateException("Emfac vehicle type is not mutable (" + type
                            + ") and should not be component for aquavis type "
                            + aquavisVehicleType);
                vehicleFractionMap.get(type).put(aquavisVehicleType, fraction);
            }
        }
        return vehicleFractionMap;
    }

    /**
     * Form a standardized data table from model/AquaVis results that can be
     * used to generate an adjusted EMFAC2011 SG input file. The data table will
     * have columns for area type, vehicle type, speed, vmt, and speed fraction
     * (amongst a single vehicle type and area type pair).
     * 
     * @param properties
     *            The properties specific to the run.
     * 
     * @return a data table holding the processed AquaVis model results.
     */
    public DataTable processAquavisData(Emfac2011Properties properties)
    {
        Path networkFile = Paths.get(properties
                .getString(Emfac2011Properties.AQUAVIS_NETWORK_FILE_PROPERTY));
        Path intrazonalFile = Paths.get(properties
                .getString(Emfac2011Properties.AQUAVIS_INTRAZONAL_FILE_PROPERTY));
        Path tripsFile = Paths.get(properties
                .getString(Emfac2011Properties.AQUAVIS_TRIPS_FILE_PROPERTY));
        Map<String, List<String>> areas = new HashMap<>(
                properties.<String, List<String>>getMap(Emfac2011Properties.AREAS_PROPERTY));
        Map<String, String> districtsToSubareas = new HashMap<>();
        for (String subarea : areas.keySet())
            for (String district : areas.get(subarea))
                districtsToSubareas.put(district, subarea);
        LOGGER.debug("Reading aquavis network file");
        DataTable network = new RowDataTable(new CsvTableReader(networkFile.toString()));
        LOGGER.debug("Reading aquavis intrazonal file");
        DataTable intrazonal = new RowDataTable(new CsvTableReader(intrazonalFile.toString()));
        LOGGER.debug("Reading aquavis trips file");
        DataTable trips = new RowDataTable(new CsvTableReader(tripsFile.toString()));

        // need to spread out speed fractions - by auto class
        // need to collect intrazonal vmt and add it to network vmt - by auto
        // class

        TableSchema schema = new TableSchema("Emfac Data");
        schema.addColumn(EMFAC_2011_DATA_SPEED_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_DATA_SUB_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_DATA_VEHICLE_TYPE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_DATA_VMT_FIELD, DataType.DOUBLE);
        schema.addColumn(EMFAC_2011_DATA_SPEED_FRACTION_FIELD, DataType.DOUBLE);
        DataTable outputTable = new RowDataTable(schema);

        // first, add rows for everything
        LOGGER.debug("Building EMFAC data table shell");
        for (Emfac2011VehicleType vehicle : Emfac2011VehicleType.getMutableVehicleTypes())
            for (String subArea : areas.keySet())
                for (Emfac2011SpeedCategory speed : Emfac2011SpeedCategory.values())
                    outputTable.addRow(speed.getName(), subArea, vehicle.getName(), 0.0, -1.0); // -1
                                                                                                // is
                                                                                                // for
                                                                                                // error
                                                                                                // checking
                                                                                                // -
                                                                                                // 0
                                                                                                // might
                                                                                                // pass
                                                                                                // through
                                                                                                // unnoticed
        TableIndex<String> index = new BasicTableIndex<>(outputTable, EMFAC_2011_DATA_SPEED_FIELD,
                EMFAC_2011_DATA_SUB_AREA_FIELD, EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
        index.buildIndex();

        Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacTypeMapping = getAquavisVehicleTypeToEmfacTypeMapping();
        Map<Emfac2011VehicleType, Map<String, Double>> vehicleFractions = buildVehicleFractioning(aquavisVehicleTypeToEmfacTypeMapping);

        LOGGER.debug("Aggregating aquavis network VMT data");
        for (DataRow row : network)
        {
            double vmt = row.getCellAsDouble(Emfac2011Constants.AQUAVIS_NETWORK_VOLUME_FIELD)
                    * row.getCellAsDouble(Emfac2011Constants.AQUAVIS_NETWORK_LENGTH_FIELD);
            double speed = row.getCellAsDouble(Emfac2011Constants.AQUAVIS_NETWORK_SPEED_FIELD);
            String vehicleType = row
                    .getCellAsString(Emfac2011Constants.AQUAVIS_NETWORK_VEHICLE_CLASS_FIELD);
            String district = row.getCellAsString(Emfac2011Constants.AQUAVIS_NETWORK_REGION_FIELD);
            if (districtsToSubareas.containsKey(district))
            {
                String subarea = districtsToSubareas.get(district);
                for (Emfac2011VehicleType emfacVehicle : aquavisVehicleTypeToEmfacTypeMapping
                        .get(vehicleType))
                {
                    double fraction = vehicleFractions.get(emfacVehicle).get(vehicleType);
                    for (int r : index.getRowNumbers(Emfac2011SpeedCategory.getSpeedCategory(speed)
                            .getName(), subarea, emfacVehicle.getName()))
                        outputTable.setCellValue(r, EMFAC_2011_DATA_VMT_FIELD,
                                (Double) outputTable.getCellValue(r, EMFAC_2011_DATA_VMT_FIELD)
                                        + fraction * vmt);
                }
            }
        }

        LOGGER.debug("Aggregating aquavis intrazonal VMT data");
        intrazonal.setPrimaryKey(Emfac2011Constants.AQUAVIS_INTRAZONAL_ZONE_FIELD);
        for (DataRow row : trips)
        {
            int zone = row.getCellAsInt(Emfac2011Constants.AQUAVIS_TRIPS_ORIGIN_ZONE_FIELD);
            if (zone == row.getCellAsInt(Emfac2011Constants.AQUAVIS_TRIPS_DESTINATION_ZONE_FIELD))
            {
                String district = (String) intrazonal.getCellValueByKey(zone,
                        Emfac2011Constants.AQUAVIS_INTRAZONAL_REGION_FIELD);
                if (districtsToSubareas.containsKey(district))
                {
                    String subarea = districtsToSubareas.get(district);
                    double speed = (Double) intrazonal.getCellValueByKey(zone,
                            Emfac2011Constants.AQUAVIS_INTRAZONAL_SPEED_FIELD);
                    double vmt = (Double) intrazonal.getCellValueByKey(zone,
                            Emfac2011Constants.AQUAVIS_INTRAZONAL_LENGTH_FIELD)
                            * row.getCellAsDouble(Emfac2011Constants.AQUAVIS_TRIPS_TRIPS_FIELD);
                    String vehicleType = row
                            .getCellAsString(Emfac2011Constants.AQUAVIS_TRIPS_VEHICLE_CLASS_FIELD);
                    for (Emfac2011VehicleType emfacVehicle : aquavisVehicleTypeToEmfacTypeMapping
                            .get(vehicleType))
                    {
                        double fraction = vehicleFractions.get(emfacVehicle).get(vehicleType);
                        for (int r : index.getRowNumbers(
                                Emfac2011SpeedCategory.getSpeedCategory(speed).getName(), subarea,
                                emfacVehicle.getName()))
                            outputTable.setCellValue(r, EMFAC_2011_DATA_VMT_FIELD,
                                    (Double) outputTable.getCellValue(r, EMFAC_2011_DATA_VMT_FIELD)
                                            + fraction * vmt);
                    }
                }
            }
        }

        LOGGER.debug("Building speed fractions");
        // build fractions
        index = new BasicTableIndex<>(outputTable, EMFAC_2011_DATA_SUB_AREA_FIELD,
                EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
        index.buildIndex();
        for (Emfac2011VehicleType emfacVehicle : Emfac2011VehicleType.getMutableVehicleTypes())
        {
            for (String subarea : areas.keySet())
            {
                double sum = 0.0;
                int count = 0;
                for (DataRow row : outputTable.getIndexedRows(index, subarea,
                        emfacVehicle.getName()))
                {
                    sum += row.getCellAsDouble(EMFAC_2011_DATA_VMT_FIELD);
                    count++;
                }
                for (int r : index.getRowNumbers(subarea, emfacVehicle.getName()))
                    outputTable.setCellValue(
                            r,
                            EMFAC_2011_DATA_SPEED_FRACTION_FIELD,
                            sum == 0.0 ? 1.0 / count : (Double) outputTable.getCellValue(r,
                                    EMFAC_2011_DATA_VMT_FIELD) / sum);
            }
        }

        return outputTable;
    }
}
