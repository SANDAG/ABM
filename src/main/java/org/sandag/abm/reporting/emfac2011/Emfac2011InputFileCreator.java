package org.sandag.abm.reporting.emfac2011;

import static com.pb.sawdust.util.Range.range;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pb.sawdust.excel.tabledata.read.ExcelTableReader;
import com.pb.sawdust.excel.tabledata.write.ExcelTableWriter;
import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.TableIndex;
import com.pb.sawdust.tabledata.basic.BasicTableIndex;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.metadata.DataType;
import com.pb.sawdust.tabledata.metadata.TableSchema;
import com.pb.sawdust.tabledata.write.TableWriter;
import com.pb.sawdust.util.ProcessUtil;
import com.pb.sawdust.util.exceptions.RuntimeIOException;

/**
 * The {@code InputTemplateCreator} class is used to build an adjusted input
 * file used for running the EMFAC2011 SG model.
 * 
 * 
 * @author crf Started 2/7/12 1:48 PM
 */
public class Emfac2011InputFileCreator
{
    private static final Logger LOGGER                                             = LoggerFactory
                                                                                           .getLogger(Emfac2011Data.class);

    public static final String  EMFAC_2011_SCENARIO_TABLE_NAME                     = "Regional_Scenarios";
    public static final String  EMFAC_2011_SCENARIO_TABLE_GROUP_FIELD              = "Group";
    public static final String  EMFAC_2011_SCENARIO_TABLE_AREA_TYPE_FIELD          = "Area Type";
    public static final String  EMFAC_2011_SCENARIO_TABLE_AREA_FIELD               = "Area";
    public static final String  EMFAC_2011_SCENARIO_TABLE_YEAR_FIELD               = "CalYr";
    public static final String  EMFAC_2011_SCENARIO_TABLE_SEASON_FIELD             = "Season";

    public static final String  EMFAC_2011_VMT_TABLE_NAME                          = "Scenario_Base_Inputs";
    public static final String  EMFAC_2011_VMT_TABLE_GROUP_FIELD                   = "Group";
    public static final String  EMFAC_2011_VMT_TABLE_AREA_FIELD                    = "Area";
    public static final String  EMFAC_2011_VMT_TABLE_SCENARIO_FIELD                = "Scenario";
    public static final String  EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD                = "Sub-Area";
    public static final String  EMFAC_2011_VMT_TABLE_YEAR_FIELD                    = "CalYr";
    public static final String  EMFAC_2011_VMT_TABLE_SEASON_FIELD                  = "Season";
    public static final String  EMFAC_2011_VMT_TABLE_TITLE_FIELD                   = "Title";
    public static final String  EMFAC_2011_VMT_TABLE_VMT_PROFILE_FIELD             = "VMT Profile";
    public static final String  EMFAC_2011_VMT_TABLE_VMT_BY_VEH_FIELD              = "VMT by Vehicle Category";
    public static final String  EMFAC_2011_VMT_TABLE_SPEED_PROFILE_FIELD           = "Speed Profile";
    public static final String  EMFAC_2011_VMT_TABLE_VMT_FIELD                     = "New Total VMT";

    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_NAME                  = "Scenario_VMT_by_VehCat";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_GROUP_FIELD           = "Group";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_AREA_FIELD            = "Area";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_SCENARIO_FIELD        = "Scenario";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD        = "Sub-Area";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_YEAR_FIELD            = "CalYr";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_SEASON_FIELD          = "Season";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_TITLE_FIELD           = "Title";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD         = "Veh & Tech";
    public static final String  EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD             = "New VMT";

    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_NAME               = "Scenario_Speed_Profiles";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_GROUP_FIELD        = "Group";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_AREA_FIELD         = "Area";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_SCENARIO_FIELD     = "Scenario";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD     = "Sub-Area";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_YEAR_FIELD         = "CalYr";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_SEASON_FIELD       = "Season";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_TITLE_FIELD        = "Title";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD      = "Veh & Tech";
    public static final String  EMFAC_2011_SPEED_FRACTION_TABLE_2007_VEHICLE_FIELD = "EMFAC2007 Veh & Tech";

    /**
     * Create an input file that can be used with the EMFAC2011 SG model. The
     * constructed input file will use the default EMFAC2011
     * parameters/specifications, with adjustments based on a travel demand
     * model's results.
     * 
     * @param properties
     *            The properties specific to the model run.
     * 
     * @param emfacModelData
     *            A data table (obtained from
     *            {@link Emfac2011Data#processAquavisData(Emfac2011Properties)}
     *            )) holding the results of the travel demand model.
     * 
     * @return an EMFAC2011 SG input file, adjusted using {@code emfacModelData}
     *         .
     */
    public Path createInputFile(Emfac2011Properties properties, DataTable emfacModelData)
    {
        String areaType = properties.getString(Emfac2011Properties.AREA_TYPE_PROPERTY);
        String region = properties.getString(Emfac2011Properties.REGION_NAME_PROPERTY);
        // Set<String> areas = new
        // HashSet<>(properties.<String>getList(Emfac2011Properties.AREAS_PROPERTY));
        // Map<String,List<String>> areas = new
        // HashMap<>(properties.<String,List<String>>getMap(Emfac2011Properties.AREAS_PROPERTY));
        Set<String> areas = new HashMap<>(
                properties.<String, List<String>>getMap(Emfac2011Properties.AREAS_PROPERTY))
                .keySet();
        String season = properties.getString(Emfac2011Properties.SEASON_PROPERTY);
        int year = properties.getInt(Emfac2011Properties.YEAR_PROPERTY);
        String inventoryDir = Paths.get(
                properties.getString(Emfac2011Properties.EMFAC2011_INSTALLATION_DIR_PROPERTY),
                "Application Files/Inventory Files").toString();
        String outputDir = properties.getString(Emfac2011Properties.OUTPUT_DIR_PROPERTY);
        String converterProgram = properties
                .getString(Emfac2011Properties.XLS_CONVERTER_PROGRAM_PROPERTY);
        boolean preserveEmfacVehicleFractions = properties
                .getBoolean(Emfac2011Properties.PRESERVE_EMFAC_VEHICLE_FRACTIONS_PROPERTY);
        boolean modelVmtIncludesNonMutableVehicleTypes = properties
                .getBoolean(Emfac2011Properties.MODEL_VMT_INCLUDES_NON_MUTABLES_PROPERTY);
        return createInputFile(areaType, region, areas, season, year, emfacModelData,
                preserveEmfacVehicleFractions, modelVmtIncludesNonMutableVehicleTypes,
                inventoryDir, outputDir, converterProgram);
    }

    private String formInventoryFileName(String area, String season, int year)
    {
        return "EMFAC2011-SG Inventory - " + area + " - " + year + " (" + season + ").xls";
    }

    private void convertFile(String inventoryFile, String outputInventoryFile,
            String converterProgram)
    {
        ProcessUtil.runProcess(Arrays.asList(converterProgram, inventoryFile, outputInventoryFile));
    }

    private DataTable readInventoryTable(String inventoryFile)
    {
        return new RowDataTable(ExcelTableReader.excelTableReader(inventoryFile));
    }

    private Path createInputFile(String areaType, String region, Set<String> areas, String season,
            int year, DataTable emfacModelData, boolean preserveEmfacVehicleFractions,
            boolean modelVmtIncludesNonMutableVehicleTypes, String inventoryDir, String outputDir,
            String converterProgram)
    {
        Map<String, DataTable> inputTables = new LinkedHashMap<>();
        for (String area : areas)
        {
            String inventoryFile = Paths.get(inventoryDir,
                    formInventoryFileName(area, season, year)).toString();
            Path outputInventoryFile = Paths.get(outputDir,
                    formInventoryFileName(area, season, year));
            convertFile(inventoryFile, outputInventoryFile.toString(), converterProgram);
            inputTables.put(area, readInventoryTable(outputInventoryFile.toString()));
            try
            {
                Files.delete(outputInventoryFile);
            } catch (IOException e)
            {
                throw new RuntimeIOException(e);
            }
        }
        Path outputFile = Paths.get(outputDir, formOutputFileName(region, season, year));
        try
        {
            Files.deleteIfExists(outputFile);
        } catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }
        TableWriter writer = new ExcelTableWriter(outputFile.toFile());
        LOGGER.debug("Initializing input excel file: " + outputFile);
        writer.writeTable(formScenarioTable(areaType, region, year, season));
        LOGGER.debug("Building vmt table");
        DataTable vmtTable = extractVmtTables(inputTables, region, year, season);
        LOGGER.debug("Building vmt by vehicle type table");
        DataTable vehicleVmtTable = extractVmtVehicleTables(inputTables, region, year, season);
        LOGGER.debug("Building speed fraction table");
        DataTable vmtSpeedTable = extractVmtSpeedTables(inputTables, region, year, season);
        LOGGER.debug("Shifting tables using model data");
        shiftVmtTables(vmtTable, vehicleVmtTable, areas, emfacModelData,
                preserveEmfacVehicleFractions, modelVmtIncludesNonMutableVehicleTypes);
        shiftSpeedFractionTable(vmtSpeedTable, emfacModelData);
        LOGGER.debug("Writing tables");
        writer.writeTable(vmtTable);
        writer.writeTable(vehicleVmtTable);
        writer.writeTable(vmtSpeedTable);
        return outputFile;
    }

    private String formOutputFileName(String region, String season, int year)
    {
        return region + "-" + season + "-" + year + ".xls";
    }

    private DataTable formScenarioTable(String areaType, String area, int year, String season)
    {
        TableSchema schema = new TableSchema(EMFAC_2011_SCENARIO_TABLE_NAME);
        schema.addColumn(EMFAC_2011_SCENARIO_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_SCENARIO_TABLE_AREA_TYPE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SCENARIO_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SCENARIO_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_SCENARIO_TABLE_SEASON_FIELD, DataType.STRING);
        DataTable table = new RowDataTable(schema);
        List<Object> row = new LinkedList<>();
        row.add(1);
        row.add(areaType);
        row.add(area);
        row.add(year);
        row.add(season);
        table.addRow(row.toArray(new Object[row.size()]));
        return table;
    }

    private DataTable extractVmtTables(Map<String, DataTable> inputTables, String area, int year,
            String season)
    {
        TableSchema schema = new TableSchema(EMFAC_2011_VMT_TABLE_NAME);
        schema.addColumn(EMFAC_2011_VMT_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VMT_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_SCENARIO_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VMT_TABLE_SEASON_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_TITLE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_VMT_PROFILE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_VMT_BY_VEH_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_SPEED_PROFILE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VMT_TABLE_VMT_FIELD, DataType.DOUBLE);
        DataTable vmtTable = new RowDataTable(schema);

        int counter = 1;
        for (String subArea : inputTables.keySet())
        {
            List<Object> row = new LinkedList<>();
            row.add(1);
            row.add(area);
            row.add(counter);
            row.add(subArea);
            row.add(year);
            row.add(season);
            row.add(String.format("Group #1 (%s), Scenario #%d - %s %d %s", area, counter++,
                    subArea, year, season));
            row.add("User");
            row.add("User");
            row.add("User");

            double totalVmt = 0.0;
            for (DataRow r : inputTables.get(subArea))
                if (r.getCellAsString("Tech").equals("TOT")) totalVmt += r.getCellAsDouble("VMT");
            row.add(totalVmt);
            vmtTable.addRow(row.toArray(new Object[row.size()]));
        }
        return vmtTable;
    }

    private DataTable extractVmtVehicleTables(Map<String, DataTable> inputTables, String area,
            int year, String season)
    {
        TableSchema schema = new TableSchema(EMFAC_2011_VEHICLE_VMT_TABLE_NAME);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_SCENARIO_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_SEASON_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_TITLE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD, DataType.DOUBLE);
        DataTable vehicleVmtTable = new RowDataTable(schema);

        int counter = 1;
        for (String subArea : inputTables.keySet())
        {
            for (DataRow r : inputTables.get(subArea))
            {
                String tech = r.getCellAsString("Tech");
                if (tech.equals("DSL") || tech.equals("GAS"))
                {
                    List<Object> row = new LinkedList<>();
                    row.add(1);
                    row.add(area);
                    row.add(counter);
                    row.add(subArea);
                    row.add(year);
                    row.add(season);
                    row.add(String.format("Group #1 (%s), Scenario #%d - %s %d %s", area, counter,
                            subArea, year, season));
                    row.add(r.getCellAsString("Veh & Tech"));
                    row.add(r.getCellAsDouble("VMT"));
                    vehicleVmtTable.addRow(row.toArray(new Object[row.size()]));
                }
            }
            counter++;
        }
        return vehicleVmtTable;
    }

    private DataTable extractVmtSpeedTables(Map<String, DataTable> inputTables, String area,
            int year, String season)
    {
        TableSchema schema = new TableSchema(EMFAC_2011_SPEED_FRACTION_TABLE_NAME);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_SCENARIO_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_SEASON_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_TITLE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD, DataType.STRING);
        schema.addColumn(EMFAC_2011_SPEED_FRACTION_TABLE_2007_VEHICLE_FIELD, DataType.STRING);
        for (Emfac2011SpeedCategory category : Emfac2011SpeedCategory.values())
            schema.addColumn(category.getName(), DataType.DOUBLE);
        DataTable vmtSpeedTable = new RowDataTable(schema);

        int counter = 1;
        for (String subArea : inputTables.keySet())
        {
            // loop over everything once to get sums and types, and then second
            // time to generate fractions
            Map<String, Double> techTotals = new LinkedHashMap<>();
            for (DataRow r : inputTables.get(subArea))
            {
                String tech = r.getCellAsString("Tech");
                if (tech.startsWith("Spd") && !tech.endsWith("TOT"))
                {
                    String vehNTech = r.getCellAsString("Veh & Tech").toLowerCase();
                    if (!techTotals.containsKey(vehNTech)) techTotals.put(vehNTech, 0.0);
                    techTotals.put(vehNTech, techTotals.get(vehNTech) + r.getCellAsDouble("VMT"));
                }
            }
            Map<String, Integer> techRows = new HashMap<>();
            // loop over each type and add in the rows
            for (Emfac2011VehicleType type : Emfac2011VehicleType.values())
            {
                List<Object> row = new LinkedList<>();
                row.add(1);
                row.add(area);
                row.add(counter);
                row.add(subArea);
                row.add(year);
                row.add(season);
                row.add(String.format("Group #1 (%s), Scenario #%d - %s %d %s", area, counter,
                        subArea, year, season));
                row.add(type.getName());
                row.add(type.getEmfac2007Name());
                for (int i : range(5, 71, 5))
                    row.add(0.0);
                vmtSpeedTable.addRow(row.toArray(new Object[row.size()]));
                techRows.put(type.getName().toLowerCase(), vmtSpeedTable.getRowCount() - 1);
            }
            // now reloop over table to get fractions
            for (DataRow r : inputTables.get(subArea))
            {
                String tech = r.getCellAsString("Tech");
                if (tech.startsWith("Spd") && !tech.endsWith("TOT"))
                {
                    String vehNTech = r.getCellAsString("Veh & Tech").toLowerCase();
                    double fraction = techTotals.get(vehNTech) == 0.0 ? 0.0 : r
                            .getCellAsDouble("VMT") / techTotals.get(vehNTech);
                    vmtSpeedTable.setCellValue(techRows.get(vehNTech),
                            Integer.parseInt(tech.substring(3, 5)) + "MPH", fraction);
                }
            }
            counter++;
        }
        // ensure that we sum up to one
        counter = 0;
        for (DataRow r : vmtSpeedTable)
        {
            double sum = 1.0;
            Emfac2011SpeedCategory[] speeds = Emfac2011SpeedCategory.values();
            for (int i : range(speeds.length - 1))
                sum -= r.getCellAsDouble(speeds[i].getName());
            vmtSpeedTable.setCellValue(counter++, Emfac2011SpeedCategory.SPEED_65_70plus.getName(),
                    sum);
        }
        return vmtSpeedTable;
    }

    // private void shiftSpeedFractionTable(DataTable speedVmtTable, DataTable
    // modelData) {
    private void shiftSpeedFractionTable(DataTable speedVmtTable, DataTable modelData)
    {
        TableIndex<String> index = new BasicTableIndex<>(speedVmtTable,
                EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD,
                EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD);
        index.buildIndex();

        for (DataRow row : modelData)
        {
            String subArea = row.getCellAsString(Emfac2011Data.EMFAC_2011_DATA_SUB_AREA_FIELD);
            String vehicleType = row
                    .getCellAsString(Emfac2011Data.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
            String category = Emfac2011SpeedCategory.getTypeForName(
                    row.getCellAsString(Emfac2011Data.EMFAC_2011_DATA_SPEED_FIELD)).getName();
            speedVmtTable.setCellValue(index.getRowNumbers(subArea, vehicleType).iterator().next(),
                    category,
                    row.getCellAsDouble(Emfac2011Data.EMFAC_2011_DATA_SPEED_FRACTION_FIELD));
        }
    }

    private void shiftVmtTables(DataTable vmtTable, DataTable vehicleVmtTable, Set<String> areas,
            DataTable modelData, boolean preserveEmfacVehicleFractions,
            boolean modelVmtIncludesNonMutableVehicleTypes)
    {
        Map<String, Map<Emfac2011VehicleType, Double>> modelVmtByAreaAndVehicleType = new HashMap<>();
        Map<String, Map<Emfac2011VehicleType, Double>> emfacMutableVmtByAreaAndVehicleType = new HashMap<>();
        Map<String, Map<Emfac2011VehicleType, Double>> emfacImmutableVmtByAreaAndVehicleType = new HashMap<>();

        Set<Emfac2011VehicleType> mutableVehicleTypes = Emfac2011VehicleType
                .getMutableVehicleTypes();

        for (String subarea : areas)
        {
            Map<Emfac2011VehicleType, Double> m1 = new EnumMap<>(Emfac2011VehicleType.class);
            Map<Emfac2011VehicleType, Double> m2 = new EnumMap<>(Emfac2011VehicleType.class);
            Map<Emfac2011VehicleType, Double> m3 = new EnumMap<>(Emfac2011VehicleType.class);
            for (Emfac2011VehicleType vehicleType : Emfac2011VehicleType.values())
            {
                if (mutableVehicleTypes.contains(vehicleType))
                {
                    m1.put(vehicleType, 0.0);
                    m2.put(vehicleType, 0.0);
                } else
                {
                    if (modelVmtIncludesNonMutableVehicleTypes) m1.put(vehicleType, 0.0);
                    m3.put(vehicleType, 0.0);
                }
            }
            modelVmtByAreaAndVehicleType.put(subarea, m1);
            emfacMutableVmtByAreaAndVehicleType.put(subarea, m2);
            emfacImmutableVmtByAreaAndVehicleType.put(subarea, m3);
        }

        for (DataRow row : vehicleVmtTable)
        {
            String subArea = row.getCellAsString(EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType.getVehicleType(row
                    .getCellAsString(EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD));
            double vmt = row.getCellAsDouble(EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD);
            if (mutableVehicleTypes.contains(vehicleType)) emfacMutableVmtByAreaAndVehicleType.get(
                    subArea).put(vehicleType,
                    emfacMutableVmtByAreaAndVehicleType.get(subArea).get(vehicleType) + vmt);
            else emfacImmutableVmtByAreaAndVehicleType.get(subArea).put(vehicleType,
                    emfacImmutableVmtByAreaAndVehicleType.get(subArea).get(vehicleType) + vmt);
        }

        for (DataRow row : modelData)
        {
            String subArea = row.getCellAsString(Emfac2011Data.EMFAC_2011_DATA_SUB_AREA_FIELD);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType.getVehicleType(row
                    .getCellAsString(Emfac2011Data.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD));
            double vmt = row.getCellAsDouble(Emfac2011Data.EMFAC_2011_DATA_VMT_FIELD);
            modelVmtByAreaAndVehicleType.get(subArea).put(vehicleType,
                    modelVmtByAreaAndVehicleType.get(subArea).get(vehicleType) + vmt);
        }

        if (preserveEmfacVehicleFractions)
        {
            // need to reshift data
            for (String subarea : modelVmtByAreaAndVehicleType.keySet())
            {
                double totalVmt = 0.0;
                Map<Emfac2011VehicleType, Double> modelVmt = modelVmtByAreaAndVehicleType
                        .get(subarea);
                for (Emfac2011VehicleType vehicleType : modelVmt.keySet())
                    totalVmt += modelVmt.get(vehicleType);
                double totalEmfacVmt = 0.0;
                Map<Emfac2011VehicleType, Double> emfacMutableVmt = emfacMutableVmtByAreaAndVehicleType
                        .get(subarea);
                Map<Emfac2011VehicleType, Double> emfacImutableVmt = emfacImmutableVmtByAreaAndVehicleType
                        .get(subarea);
                for (Emfac2011VehicleType vehicleType : emfacMutableVmt.keySet())
                    totalEmfacVmt += emfacMutableVmt.get(vehicleType);
                if (modelVmtIncludesNonMutableVehicleTypes)
                    for (Emfac2011VehicleType vehicleType : emfacImutableVmt.keySet())
                        totalEmfacVmt += emfacImutableVmt.get(vehicleType);
                for (Emfac2011VehicleType vehicleType : modelVmt.keySet())
                {
                    if (emfacMutableVmt.containsKey(vehicleType)) modelVmt.put(vehicleType,
                            totalVmt * emfacMutableVmt.get(vehicleType) / totalEmfacVmt);
                    else modelVmt.put(vehicleType, totalVmt * emfacImutableVmt.get(vehicleType)
                            / totalEmfacVmt);
                }
            }
        }

        // shift overall vmt
        vmtTable.setPrimaryKey(EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD);
        for (DataRow row : vmtTable)
        {
            String subArea = row.getCellAsString(EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD);
            double originalVmt = row.getCellAsDouble(EMFAC_2011_VMT_TABLE_VMT_FIELD);
            Map<Emfac2011VehicleType, Double> modelVmt = modelVmtByAreaAndVehicleType.get(subArea);
            Map<Emfac2011VehicleType, Double> emfacMutableVmt = emfacMutableVmtByAreaAndVehicleType
                    .get(subArea);
            Map<Emfac2011VehicleType, Double> emfacImutableVmt = emfacImmutableVmtByAreaAndVehicleType
                    .get(subArea);
            for (Emfac2011VehicleType vehicleType : modelVmt.keySet())
            {
                originalVmt += modelVmt.get(vehicleType); // add corrected vmt
                originalVmt -= emfacMutableVmt.containsKey(vehicleType) ? emfacMutableVmt
                        .get(vehicleType) : emfacImutableVmt.get(vehicleType); // subtract
                                                                               // old
                                                                               // vmt
            }
            vmtTable.setCellValueByKey(subArea, EMFAC_2011_VMT_TABLE_VMT_FIELD, originalVmt);
        }

        // replace vehicle vmts
        TableIndex<String> index = new BasicTableIndex<>(vehicleVmtTable,
                EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD,
                EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD);
        index.buildIndex();
        for (DataRow row : vehicleVmtTable)
        {
            String subArea = row.getCellAsString(EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
            Map<Emfac2011VehicleType, Double> modelVmt = modelVmtByAreaAndVehicleType.get(subArea);
            Map<Emfac2011VehicleType, Double> emfacMutableVmt = emfacMutableVmtByAreaAndVehicleType
                    .get(subArea);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType.getVehicleType(row
                    .getCellAsString(EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD));
            if (modelVmt.containsKey(vehicleType))
            {
                double vmt = row.getCellAsDouble(EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD);
                if (modelVmtIncludesNonMutableVehicleTypes) vmt = modelVmt.get(vehicleType);
                else vmt += modelVmt.get(vehicleType) - emfacMutableVmt.get(vehicleType);
                vehicleVmtTable.setCellValue(index.getRowNumbers(subArea, vehicleType.getName())
                        .iterator().next(), EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD, vmt);
            }
        }
    }

    // private void shiftVmtTables_old(DataTable vmtTable, DataTable
    // vehicleVmtTable, Set<String> areas, DataTable modelData, boolean
    // preserveEmfacVehicleFractions, boolean
    // modelVmtIncludesNonMutableVehicleTypes) {
    // // private void shiftVmtTables(DataTable vmtTable, DataTable
    // vehicleVmtTable, DataTable modelData, boolean
    // preserveEmfacVehicleFractions, boolean
    // modelVmtIncludesNonMutableVehicleTypes, Map<String,List<String>> areas) {
    // //todo: separate vmts by areas
    // TableIndex<String> index = new
    // BasicTableIndex<>(vehicleVmtTable,EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD,EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
    // index.buildIndex();
    // Map<String,Double> vmtFractions = new HashMap<>(); //fraction of total
    // VMT for a given area
    // Map<String,Double> mutableVmtSum = new HashMap<>(); //total VMT in a
    // given area which is mutable
    // Map<String,Double> mutableVmtFraction = new HashMap<>(); //fraction of
    // VMT for a given area which is mutable
    //
    // double fullMutableSum = 0.0;
    // Map<String,Map<Emfac2011VehicleType,Double>> vehicleVmtAreaFractions =
    // new HashMap<>();
    // for (String area : areas) {
    // double totalMutableSum = 0.0;
    // double totalSum = 0.0;
    // Map<Emfac2011VehicleType,Double> vvaf = new
    // EnumMap<>(Emfac2011VehicleType.class);
    // Set<Emfac2011VehicleType> mutableVehicles =
    // Emfac2011VehicleType.getMutableVehicleTypes();
    // for (Emfac2011VehicleType type : Emfac2011VehicleType.values()) {
    // if (mutableVehicles.contains(type))
    // vvaf.put(type,0.0);
    // if (index.getIndexCount(type.getName(),area) > 0) { //if missing, then
    // just move on...
    // for (DataRow row :
    // vehicleVmtTable.getIndexedRows(index,type.getName(),area)) {
    // double vmt = row.getCellAsDouble(EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD);
    // if (!mutableVehicles.contains(type)) {
    // if (modelVmtIncludesNonMutableVehicleTypes)
    // totalSum += vmt; //add in non-mutable vmt to proportionalize correctly
    // continue;
    // }
    // totalSum += vmt;
    // totalMutableSum += vmt;
    // vvaf.put(type,vvaf.get(type)+vmt);
    // fullMutableSum += vmt;
    // }
    // }
    // }
    // vehicleVmtAreaFractions.put(area,vvaf);
    // mutableVmtFraction.put(area,totalMutableSum/totalSum);
    // mutableVmtSum.put(area,totalMutableSum);
    // }
    //
    // for (String area : vehicleVmtAreaFractions.keySet()) {
    // Map<Emfac2011VehicleType,Double> vvaf =
    // vehicleVmtAreaFractions.get(area);
    // for (Emfac2011VehicleType vehicleType : vvaf.keySet())
    // vvaf.put(vehicleType,vvaf.get(vehicleType)/fullMutableSum);
    // }
    //
    // for (String area : vehicleVmtAreaFractions.keySet()) {
    // Map<Emfac2011VehicleType,Double> vvaf =
    // vehicleVmtAreaFractions.get(area);
    // Double fractionSum = 0.0;
    // for (Emfac2011VehicleType vehicleType : vvaf.keySet())
    // fractionSum += vvaf.get(vehicleType);
    // vmtFractions.put(area,fractionSum);
    // }
    //
    // Map<Emfac2011VehicleType,Double> vehicleVmt = new
    // EnumMap<>(Emfac2011VehicleType.class);
    // for (Emfac2011VehicleType type :
    // Emfac2011VehicleType.getMutableVehicleTypes())
    // vehicleVmt.put(type,0.0);
    // for (DataRow row : modelData) {
    // Emfac2011VehicleType type =
    // Emfac2011VehicleType.getVehicleType(row.getCellAsString(Emfac2011Data.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD));
    // vehicleVmt.put(type,vehicleVmt.get(type)+row.getCellAsDouble(Emfac2011Data.EMFAC_2011_DATA_VMT_FIELD));
    // }
    // double totalVmt = 0.0;
    // for (Double vmt : vehicleVmt.values())
    // totalVmt += vmt;
    //
    //
    // //shift overall vmt
    // int rowCounter = 0;
    // for (DataRow row : vmtTable) {
    // String subArea =
    // row.getCellAsString(EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD);
    // double shiftedVmt = row.getCellAsDouble(EMFAC_2011_VMT_TABLE_VMT_FIELD) -
    // mutableVmtSum.get(subArea) +
    // totalVmt*vmtFractions.get(subArea)*mutableVmtFraction.get(subArea);
    // //take off mutable VMT from EMFAC, and replace with model sum
    // vmtTable.setCellValue(rowCounter++,EMFAC_2011_VMT_TABLE_VMT_FIELD,shiftedVmt);
    // }
    // //replace vehicle vmts
    // index = new
    // BasicTableIndex<>(vehicleVmtTable,EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD);
    // index.buildIndex();
    // for (Emfac2011VehicleType type :
    // Emfac2011VehicleType.getMutableVehicleTypes())
    // for (int row : index.getRowNumbers(type.getName())) {
    // String subArea = (String)
    // vehicleVmtTable.getCellValue(row,EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
    // Emfac2011VehicleType vehicleType =
    // Emfac2011VehicleType.getVehicleType((String)
    // vehicleVmtTable.getCellValue(row,EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD));
    // double shiftedVmt = (preserveEmfacVehicleFractions ?
    // vehicleVmtAreaFractions.get(subArea).get(vehicleType)*totalVmt :
    // vmtFractions.get(subArea)*vehicleVmt.get(vehicleType))*mutableVmtFraction.get(subArea);
    // vehicleVmtTable.setCellValue(row,EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD,shiftedVmt);
    // }
    //
    // }
}
