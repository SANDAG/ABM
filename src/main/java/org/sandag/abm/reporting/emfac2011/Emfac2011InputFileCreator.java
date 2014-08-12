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
 * @author crf Started 2/7/12 1:48 PM Modified by Wu.Sun@sandag.org 1/21/2014
 */
public class Emfac2011InputFileCreator
{
    private static final Logger   LOGGER  = LoggerFactory.getLogger(Emfac2011Data.class);

    private static final String[] SEASONS = {"ANNUAL", "SUMMER", "WINTER"};

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
        int oriYear = properties.getInt(Emfac2011Properties.YEAR_PROPERTY);
        int year = Math.min(oriYear, 2035);
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
        return createInputFile(areaType, region, areas, year, oriYear, emfacModelData,
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

    private Path createInputFile(String areaType, String region, Set<String> areas, int year,
            int oriYear, DataTable emfacModelData, boolean preserveEmfacVehicleFractions,
            boolean modelVmtIncludesNonMutableVehicleTypes, String inventoryDir, String outputDir,
            String converterProgram)
    {
        Path outputFile = Paths.get(outputDir, formOutputFileName(region, oriYear));
        try
        {
            Files.deleteIfExists(outputFile);
        } catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }

        TableWriter writer = new ExcelTableWriter(outputFile.toFile());
        LOGGER.debug("Initializing input excel file: " + outputFile);
        writer.writeTable(formScenarioTable(areaType, region, year, SEASONS));

        DataTable masterVmtTable = initVmtTable();
        DataTable masterVehicleVmtTable = initVehicleVmtTable();
        DataTable masterVmtSpeedTable = initVmtSpeedTable();

        for (int i=0;i<SEASONS.length;i++)
        {
            Map<String, DataTable> inputTables = new LinkedHashMap<>();
            for (String area : areas)
            {
                String inventoryFile = Paths.get(inventoryDir,
                        formInventoryFileName(area, SEASONS[i], year)).toString();
                Path outputInventoryFile = Paths.get(outputDir,
                        formInventoryFileName(area, SEASONS[i], year));
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

            LOGGER.debug("Building vmt table");
            DataTable vmtTable = extractVmtTables(inputTables, i+1, region, year, SEASONS[i]);
            LOGGER.debug("Building vmt by vehicle type table");
            DataTable vehicleVmtTable = extractVmtVehicleTables(inputTables, i+1, region, year, SEASONS[i]);
            LOGGER.debug("Building speed fraction table");
            DataTable vmtSpeedTable = extractVmtSpeedTables(inputTables, i+1, region, year, SEASONS[i]);
            LOGGER.debug("Shifting tables using model data");
            shiftVmtTables(vmtTable, vehicleVmtTable, areas, emfacModelData,
                    preserveEmfacVehicleFractions, modelVmtIncludesNonMutableVehicleTypes);
            shiftSpeedFractionTable(vmtSpeedTable, emfacModelData);
            LOGGER.debug("Writing tables");
            
            appendDataTable(masterVmtTable, vmtTable);
            appendDataTable(masterVehicleVmtTable, vehicleVmtTable);
            appendDataTable(masterVmtSpeedTable, vmtSpeedTable);
        }

        writer.writeTable(masterVmtTable);
        writer.writeTable(masterVehicleVmtTable);
        writer.writeTable(masterVmtSpeedTable);
        return outputFile;
    }
    
    private void appendDataTable(DataTable master, DataTable fragment)
    {
        for(DataRow row : fragment)
        {
            master.addRow(row);
        }
    }

    private String formOutputFileName(String region, int year)
    {
        return "EMFAC2011-" + region + "-" + year + ".xls";
    }

    private DataTable formScenarioTable(String areaType, String area, int year, String[] seasons)
    {
        TableSchema schema = new TableSchema(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_NAME);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_AREA_TYPE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SCENARIO_TABLE_SEASON_FIELD,
                DataType.STRING);

        DataTable table = new RowDataTable(schema);

        for (int i = 0; i < seasons.length; i++)
        {
            List<Object> row = new LinkedList<>();
            row.add(i+1);
            row.add(areaType);
            row.add(area);
            row.add(year);
            row.add(seasons[i]);
            table.addRow(row.toArray(new Object[row.size()]));
        }
        return table;
    }

    private DataTable initVmtTable()
    {
        TableSchema schema = new TableSchema(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_NAME);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_GROUP_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_AREA_FIELD, DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SCENARIO_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD, DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SEASON_FIELD, DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_TITLE_FIELD, DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_VMT_PROFILE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_VMT_BY_VEH_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SPEED_PROFILE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_VMT_FIELD, DataType.DOUBLE);
        return new RowDataTable(schema);
    }

    private DataTable initVehicleVmtTable()
    {
        TableSchema schema = new TableSchema(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_NAME);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_GROUP_FIELD,
                DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_AREA_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SCENARIO_FIELD,
                DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_YEAR_FIELD, DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SEASON_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_TITLE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD,
                DataType.DOUBLE);
        return new RowDataTable(schema);
    }

    private DataTable initVmtSpeedTable()
    {
        TableSchema schema = new TableSchema(
                Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_NAME);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_GROUP_FIELD,
                DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_AREA_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_SCENARIO_FIELD,
                DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_YEAR_FIELD,
                DataType.INT);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_SEASON_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_TITLE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD,
                DataType.STRING);
        schema.addColumn(Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_2007_VEHICLE_FIELD,
                DataType.STRING);
        for (Emfac2011SpeedCategory category : Emfac2011SpeedCategory.values())
            schema.addColumn(category.getName(), DataType.DOUBLE);
        return new RowDataTable(schema);
    }

    private DataTable extractVmtTables(Map<String, DataTable> inputTables, int group, String area, int year,
            String season)
    {
        DataTable vmtTable = initVmtTable();

        int counter = 1;
        for (String subArea : inputTables.keySet())
        {
            List<Object> row = new LinkedList<>();
            row.add(group);
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

    private DataTable extractVmtVehicleTables(Map<String, DataTable> inputTables, int group, String area,
            int year, String season)
    {
        DataTable vehicleVmtTable = initVehicleVmtTable();

        int counter = 1;
        for (String subArea : inputTables.keySet())
        {
            for (DataRow r : inputTables.get(subArea))
            {
                String tech = r.getCellAsString("Tech");
                if (tech.equals("DSL") || tech.equals("GAS"))
                {
                    List<Object> row = new LinkedList<>();
                    row.add(group);
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

    private DataTable extractVmtSpeedTables(Map<String, DataTable> inputTables, int group, String area, 
            int year, String season)
    {
        DataTable vmtSpeedTable = initVmtSpeedTable();
        
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
                row.add(group);
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
                Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_SUB_AREA_FIELD,
                Emfac2011Definitions.EMFAC_2011_SPEED_FRACTION_TABLE_VEHICLE_FIELD);
        index.buildIndex();

        for (DataRow row : modelData)
        {
            String subArea = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_DATA_SUB_AREA_FIELD);
            String vehicleType = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
            String category = Emfac2011SpeedCategory.getTypeForName(
                    row.getCellAsString(Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FIELD))
                    .getName();
            speedVmtTable.setCellValue(index.getRowNumbers(subArea, vehicleType).iterator().next(),
                    category,
                    row.getCellAsDouble(Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FRACTION_FIELD));
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
            String subArea = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType
                    .getVehicleType(row
                            .getCellAsString(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD));
            double vmt = row
                    .getCellAsDouble(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD);
            if (mutableVehicleTypes.contains(vehicleType)) emfacMutableVmtByAreaAndVehicleType.get(
                    subArea).put(vehicleType,
                    emfacMutableVmtByAreaAndVehicleType.get(subArea).get(vehicleType) + vmt);
            else emfacImmutableVmtByAreaAndVehicleType.get(subArea).put(vehicleType,
                    emfacImmutableVmtByAreaAndVehicleType.get(subArea).get(vehicleType) + vmt);
        }

        for (DataRow row : modelData)
        {
            String subArea = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_DATA_SUB_AREA_FIELD);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType.getVehicleType(row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD));
            double vmt = row.getCellAsDouble(Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD);
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
        vmtTable.setPrimaryKey(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD);
        for (DataRow row : vmtTable)
        {
            String subArea = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_SUB_AREA_FIELD);
            double originalVmt = row
                    .getCellAsDouble(Emfac2011Definitions.EMFAC_2011_VMT_TABLE_VMT_FIELD);
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
            vmtTable.setCellValueByKey(subArea,
                    Emfac2011Definitions.EMFAC_2011_VMT_TABLE_VMT_FIELD, originalVmt);
        }

        // replace vehicle vmts
        TableIndex<String> index = new BasicTableIndex<>(vehicleVmtTable,
                Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD,
                Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD);
        index.buildIndex();
        for (DataRow row : vehicleVmtTable)
        {
            String subArea = row
                    .getCellAsString(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_SUB_AREA_FIELD);
            Map<Emfac2011VehicleType, Double> modelVmt = modelVmtByAreaAndVehicleType.get(subArea);
            Map<Emfac2011VehicleType, Double> emfacMutableVmt = emfacMutableVmtByAreaAndVehicleType
                    .get(subArea);
            Emfac2011VehicleType vehicleType = Emfac2011VehicleType
                    .getVehicleType(row
                            .getCellAsString(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VEHICLE_FIELD));
            if (modelVmt.containsKey(vehicleType))
            {
                double vmt = row
                        .getCellAsDouble(Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD);
                if (modelVmtIncludesNonMutableVehicleTypes) vmt = modelVmt.get(vehicleType);
                else vmt += modelVmt.get(vehicleType) - emfacMutableVmt.get(vehicleType);
                vehicleVmtTable.setCellValue(index.getRowNumbers(subArea, vehicleType.getName())
                        .iterator().next(),
                        Emfac2011Definitions.EMFAC_2011_VEHICLE_VMT_TABLE_VMT_FIELD, vmt);
            }
        }
    }
}
