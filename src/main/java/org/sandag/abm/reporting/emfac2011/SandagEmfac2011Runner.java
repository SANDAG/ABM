package org.sandag.abm.reporting.emfac2011;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.read.CsvTableReader;

/**
 * The {@code SandagEmfac2011Runner} ...
 * 
 * @author crf Started 12/11/12 6:27 AM
 */
public class SandagEmfac2011Runner
        extends Emfac2011Runner
{
    private static final Logger LOGGER                                             = LoggerFactory
                                                                                           .getLogger(Emfac2011Runner.class);
    public static final String  VEHICLE_CODE_MAPPING_FILE_PROPERTY                 = "emfac.2011.to.sandag.vehicle.code.mapping.file";
    public static final String  VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN = "EMFAC2011_MODE";

    /**
     * Constructor specifying the resources used to build the properties used
     * for the EMFAC2011 SG run.
     * 
     * @param propertyResource
     *            The first properties resource.
     * @param additionalResources
     *            Any additional properties resources.
     */
    public SandagEmfac2011Runner(String propertyResource, String... additionalResources)
    {
        super(propertyResource, additionalResources);
    }

    public void runEmfac2011()
    {
        LOGGER.info("Running Emfac2011 for SANDAG");
        SandagAquavisInputBuilder builder = new SandagAquavisInputBuilder(getProperties());
        LOGGER.info("Building Aquavis inputs from SANDAG database schema: "
                + getProperties().getString(SandagAquavisInputBuilder.SCHEMA_NAME_PROPERTY));
        builder.createAquavisInputs(Emfac2011Properties.AQUAVIS_NETWORK_FILE_PROPERTY,
                Emfac2011Properties.AQUAVIS_TRIPS_FILE_PROPERTY,
                Emfac2011Properties.AQUAVIS_INTRAZONAL_FILE_PROPERTY);
        LOGGER.info("Creating Emfac2011/SANDAG vehicle code correspondence");
        builder.writeTableToCsv("EMFACVEHCODE",
                getProperties().getString(SandagAquavisInputBuilder.SCHEMA_NAME_PROPERTY),
                getProperties().getPath(VEHICLE_CODE_MAPPING_FILE_PROPERTY));
        LOGGER.info("Running Emfac2011 process...");
        // have to call this first because it sets the mutable types, which are
        // used throughout the EMFAC2011 process
        final Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacMapping = buildAquavisVehicleTypeToEmfacMapping(getProperties()
                .getPath(VEHICLE_CODE_MAPPING_FILE_PROPERTY));
        runEmfac2011(new Emfac2011Data()
        {
            @Override
            protected Map<String, Set<Emfac2011VehicleType>> getAquavisVehicleTypeToEmfacTypeMapping()
            {
                return aquavisVehicleTypeToEmfacMapping;
            }
        });
    }

    private Map<String, Set<Emfac2011VehicleType>> buildAquavisVehicleTypeToEmfacMapping(
            Path vehicleCodeMappingFile)
    {
        Map<VehicleType, Set<Emfac2011VehicleType>> mapping = new EnumMap<>(VehicleType.class);
        for (VehicleType type : VehicleType.values())
            mapping.put(type, EnumSet.noneOf(Emfac2011VehicleType.class));

        // file has one column =
        // VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN
        // the rest have names which, when made uppercase, should match
        // VehicleType enum
        DataTable vehicleCodeMapping = new RowDataTable(new CsvTableReader(
                vehicleCodeMappingFile.toString()));
        vehicleCodeMapping.setDataCoersion(true);
        Set<String> vehicleCodeColumns = new LinkedHashSet<>();
        for (String column : vehicleCodeMapping.getColumnLabels())
        {
            try
            {
                VehicleType.valueOf(column.toUpperCase());
                vehicleCodeColumns.add(column);
            } catch (IllegalArgumentException e)
            {
                // absorb - not a valid type column
            }
        }
        Set<Emfac2011VehicleType> mutableVehicleType = EnumSet.noneOf(Emfac2011VehicleType.class);
        for (DataRow row : vehicleCodeMapping)
        {
            Emfac2011VehicleType emfac2011VehicleType = Emfac2011VehicleType.getVehicleType(row
                    .getCellAsString(VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN));
            // now dynamically setting mutable vehicle types, so we need to not
            // rely on the defaults
            // if (!emfac2011VehicleType.isMutableType())
            // continue; //skip any non-mutable types, as they can't be used
            for (String column : vehicleCodeColumns)
            {
                if (row.getCellAsBoolean(column))
                {
                    mutableVehicleType.add(emfac2011VehicleType); // if a
                                                                  // mapping
                                                                  // exists,
                                                                  // then the
                                                                  // EMFAC
                                                                  // vehicle
                                                                  // type is
                                                                  // assumed to
                                                                  // be mutable
                    mapping.get(VehicleType.valueOf(column.toUpperCase()))
                            .add(emfac2011VehicleType);
                }
            }
        }
        Emfac2011VehicleType.setMutableTypes(mutableVehicleType);
        Map<String, Set<Emfac2011VehicleType>> finalMapping = new HashMap<>();
        for (VehicleType type : mapping.keySet())
            finalMapping.put(type.name(), mapping.get(type));
        return finalMapping;
    }

    public static void main(String... args)
    {
        new SandagEmfac2011Runner(args[0], Arrays.copyOfRange(args, 1, args.length)).runEmfac2011();
        // new
        // SandagEmfac2011Runner("D:\\projects\\sandag\\emfac\\output_example\\sandag_emfac2011.properties").runEmfac2011();
    }
}
