package org.sandag.abm.reporting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;

/**
 * The {@code DataExporter} ...
 * 
 * @author crf Started 9/20/12 8:36 AM
 */
public final class DataExporter
{
    private static final Logger     LOGGER                      = Logger.getLogger(DataExporter.class);

    private static final String     NUMBER_FORMAT_NAME          = "NUMBER";
    private static final String     STRING_FORMAT_NAME          = "STRING";
    private static final String     PROJECT_PATH_PROPERTY_TOKEN = "%project.folder%";
    private static final String     TOD_TOKEN                   = "%TOD%";

    private final Properties        properties;
    private final TranscadMatrixDao mtxDao;
    private final File              projectPathFile;
    private final int               feedbackIterationNumber;
    private final Set<String>       tables;
    private final String[]          timePeriods                 = ModelStructure.MODEL_PERIOD_LABELS;

    public DataExporter(Properties theProperties, TranscadMatrixDao aMtxDao, String projectPath,
            int feedbackIterationNumber)
    {
        this.properties = theProperties;
        this.mtxDao = aMtxDao;

        projectPathFile = new File(theProperties.getProperty("Project.Directory"));
        this.feedbackIterationNumber = feedbackIterationNumber;

        tables = new LinkedHashSet<String>();
    }

    private void addTable(String table)
    {
        tables.add(table);
        LOGGER.info("exporting data: " + table);
    }

    private String getPath(String path)
    {
        if (properties.containsKey(path)) return getPathFromProperty(path);
        File ff = new File(path);
        if (!ff.exists()) ff = new File(projectPathFile, path);
        return ff.getAbsolutePath();
    }

    private String getPathFromProperty(String propertyToken)
    {
        String path = (String) properties.get(propertyToken);
        if (!path.startsWith(projectPathFile.getAbsolutePath()))
            path = new File(projectPathFile, path).getAbsolutePath();
        return path;
    }

    private String getOutputPath(String file)
    {
        return new File(properties.getProperty("report.path"), file).getAbsolutePath();
    }

    private String getData(TableDataSet data, int row, int column, FieldType type)
    {
        switch (type)
        {
            case INT:
                return "" + Math.round(data.getValueAt(row, column));
            case FLOAT:
                return "" + data.getValueAt(row, column);
            case STRING:
                return data.getStringValueAt(row, column);
            case BIT:
                return Boolean.parseBoolean(data.getStringValueAt(row, column)) ? "1" : "0";
            default:
                throw new IllegalStateException("undefined field type: " + type);
        }
    }

    private String getPreferredColumnName(String columnName)
    {
        if (columnName.equalsIgnoreCase("hh_id")) return "HH_ID";
        if (columnName.equalsIgnoreCase("person_id")) return "PERSON_ID";
        if (columnName.toLowerCase().contains("maz"))
            return columnName.toLowerCase().replace("maz", "mgra").toUpperCase();
        return columnName.toUpperCase();
    }

    private void exportData(TableDataSet data, String outputFileName,
            Map<String, String> outputMapping, Map<String, FieldType> outputTypes)
    {
        int[] outputIndices = new int[outputMapping.size()];
        FieldType[] outputFieldTypes = new FieldType[outputIndices.length];
        String[] header = new String[outputMapping.size()];

        int counter = 0;
        for (String column : outputMapping.keySet())
        {
            header[counter] = column;
            outputIndices[counter] = data.getColumnPosition(outputMapping.get(column));
            outputFieldTypes[counter++] = outputTypes.get(column);
        }

        BlockingQueue<CsvRow> queue = new LinkedBlockingQueue<CsvRow>();
        Thread writerProcess = null;
        try
        {
            CsvWriterThread writerThread = new CsvWriterThread(queue, new File(
                    getOutputPath(outputFileName + ".csv")), header);
            writerProcess = new Thread(writerThread);
            writerProcess.start();

            for (int i = 1; i <= data.getRowCount(); i++)
            {
                String[] row = new String[outputMapping.size()];
                row[0] = getData(data, i, outputIndices[0], outputFieldTypes[0]);

                for (int j = 1; j < outputIndices.length; j++)
                {
                    row[j] = getData(data, i, outputIndices[j], outputFieldTypes[j]);
                }
                queue.add(new CsvRow(row));
            }
        } finally
        {
            queue.add(CsvWriterThread.POISON_PILL);
            if (null != writerProcess)
            {
                try
                {
                    writerProcess.join();
                } catch (InterruptedException e)
                {
                    LOGGER.error(e);
                    System.exit(-1);
                }
            }
        }
    }

    private TableDataSet exportDataGeneric(String outputFileBase, String filePropertyToken,
            boolean includeFeedbackIteration, String[] formats, Set<String> floatColumns,
            Set<String> stringColumns, Set<String> intColumns, Set<String> bitColumns,
            FieldType defaultFieldType, Set<String> primaryKey,
            TripStructureDefinition tripStructureDefinition)
    {
        return exportDataGeneric(outputFileBase, filePropertyToken, includeFeedbackIteration,
                formats, floatColumns, stringColumns, intColumns, bitColumns, defaultFieldType,
                primaryKey, tripStructureDefinition, null);
    }

    private TableDataSet exportDataGeneric(String outputFileBase, String filePropertyToken,
            boolean includeFeedbackIteration, String[] formats, Set<String> floatColumns,
            Set<String> stringColumns, Set<String> intColumns, Set<String> bitColumns,
            FieldType defaultFieldType, Set<String> primaryKey,
            TripStructureDefinition tripStructureDefinition, JoinData joinData)
    {
        return exportDataGeneric(outputFileBase, filePropertyToken, includeFeedbackIteration,
                formats, floatColumns, stringColumns, intColumns, bitColumns, defaultFieldType,
                primaryKey, new HashMap<String, String>(), tripStructureDefinition, joinData);
    }

    private TableDataSet exportDataGeneric(String outputFileBase, String filePropertyToken,
            boolean includeFeedbackIteration, String[] formats, Set<String> floatColumns,
            Set<String> stringColumns, Set<String> intColumns, Set<String> bitColumns,
            FieldType defaultFieldType, Set<String> primaryKey,
            Map<String, String> overridingFieldMappings,
            TripStructureDefinition tripStructureDefinition)
    {
        return exportDataGeneric(outputFileBase, filePropertyToken, includeFeedbackIteration,
                formats, floatColumns, stringColumns, intColumns, bitColumns, defaultFieldType,
                primaryKey, overridingFieldMappings, tripStructureDefinition, null);
    }

    private TableDataSet exportDataGeneric(String outputFileBase, String filePropertyToken,
            boolean includeFeedbackIteration, String[] formats, Set<String> floatColumns,
            Set<String> stringColumns, Set<String> intColumns, Set<String> bitColumns,
            FieldType defaultFieldType, Set<String> primaryKey,
            Map<String, String> overridingFieldMappings,
            TripStructureDefinition tripStructureDefinition, JoinData joinData)
    {
        TableDataSet table;
        try
        {
            String f = includeFeedbackIteration ? getPath(filePropertyToken).replace(".csv",
                    "_" + feedbackIterationNumber + ".csv") : getPath(filePropertyToken);
            table = formats == null ? new CSVFileReader().readFile(new File(f))
                    : new CSVFileReader().readFileWithFormats(new File(f), formats);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        if (joinData != null) joinData.joinDataToTable(table);
        exportDataGeneric(table, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, defaultFieldType, primaryKey, overridingFieldMappings,
                tripStructureDefinition);
        return table;
    }

    private class JoinData
    {
        private final Map<String, Map<Integer, ?>> data;
        private final Map<String, FieldType>       dataType;
        private final String                       idColumn;

        public JoinData(String idColumn)
        {
            this.idColumn = idColumn;
            data = new LinkedHashMap<String, Map<Integer, ?>>();
            dataType = new HashMap<String, FieldType>();
        }

        public void addJoinData(Map<Integer, ?> joinData, FieldType type, String columnName)
        {
            data.put(columnName, joinData);
            dataType.put(columnName, type);
        }

        public void joinDataToTable(TableDataSet table)
        {
            int[] ids = table.getColumnAsInt(idColumn);
            for (String column : data.keySet())
                table.appendColumn(getData(ids, column), column);
        }

        private Object getData(int[] ids, String column)
        {
            switch (dataType.get(column))
            {
                case INT:
                {
                    int[] columnData = new int[ids.length];
                    @SuppressWarnings("unchecked")
                    // this is correct
                    Map<Integer, Integer> dataMap = (Map<Integer, Integer>) data.get(column);
                    for (int i = 0; i < ids.length; i++)
                        columnData[i] = dataMap.get(ids[i]);
                    return columnData;
                }
                case FLOAT:
                {
                    float[] columnData = new float[ids.length];
                    @SuppressWarnings("unchecked")
                    // this is correct
                    Map<Integer, Float> dataMap = (Map<Integer, Float>) data.get(column);
                    for (int i = 0; i < ids.length; i++)
                        columnData[i] = dataMap.get(ids[i]);
                    return columnData;
                }
                case STRING:
                {
                    String[] columnData = new String[ids.length];
                    @SuppressWarnings("unchecked")
                    // this is correct
                    Map<Integer, String> dataMap = (Map<Integer, String>) data.get(column);
                    for (int i = 0; i < ids.length; i++)
                        columnData[i] = dataMap.get(ids[i]);
                    return columnData;
                }
                case BIT:
                {
                    boolean[] columnData = new boolean[ids.length];
                    @SuppressWarnings("unchecked")
                    // this is correct
                    Map<Integer, Boolean> dataMap = (Map<Integer, Boolean>) data.get(column);
                    for (int i = 0; i < ids.length; i++)
                        columnData[i] = dataMap.get(ids[i]);
                    return columnData;
                }
                default:
                    throw new IllegalStateException("shouldn't be here: " + dataType.get(column));
            }
        }
    }

    private void exportDataGeneric(TableDataSet table, String outputFileBase,
            Set<String> floatColumns, Set<String> stringColumns, Set<String> intColumns,
            Set<String> bitColumns, FieldType defaultFieldType, Set<String> primaryKey,
            TripStructureDefinition tripStructureDefinition)
    {
        exportDataGeneric(table, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, defaultFieldType, primaryKey, new HashMap<String, String>(),
                tripStructureDefinition);

    }

    private void exportDataGeneric(TableDataSet table, String outputFileBase,
            Set<String> floatColumns, Set<String> stringColumns, Set<String> intColumns,
            Set<String> bitColumns, FieldType defaultFieldType, Set<String> primaryKey,
            Map<String, String> overridingFieldMappings,
            TripStructureDefinition tripStructureDefinition)
    {
        Map<String, String> fieldMappings = new LinkedHashMap<String, String>();
        Map<String, FieldType> fieldTypes = new HashMap<String, FieldType>();

        if (tripStructureDefinition != null)
        {
            appendTripData(table, tripStructureDefinition);
            floatColumns.add("TRIP_TIME");
            floatColumns.add("OUT_VEHICLE_TIME");
            floatColumns.add("TRIP_DISTANCE");
            floatColumns.add("TRIP_COST");
            stringColumns.add("TRIP_PURPOSE_NAME");
            stringColumns.add("TRIP_MODE_NAME");
            intColumns.add("RECID");
        }

        if (primaryKey.size() == 0)
        {
            // have to add in a key - call it ID
            int[] id = new int[table.getRowCount()];
            for (int i = 0; i < id.length; i++)
                id[i] = i + 1;
            table.appendColumn(id, "ID");

            primaryKey.add("ID");
            intColumns.add("ID");
        }

        outer: for (String column : table.getColumnLabels())
        {
            String c = overridingFieldMappings.containsKey(column) ? overridingFieldMappings
                    .get(column) : getPreferredColumnName(column);
            fieldMappings.put(c, column);
            for (String fc : floatColumns)
            {
                if (fc.equalsIgnoreCase(column))
                {
                    fieldTypes.put(c, FieldType.FLOAT);
                    continue outer;
                }
            }
            for (String sc : stringColumns)
            {
                if (sc.equalsIgnoreCase(column))
                {
                    fieldTypes.put(c, FieldType.STRING);
                    continue outer;
                }
            }
            for (String sc : intColumns)
            {
                if (sc.equalsIgnoreCase(column))
                {
                    fieldTypes.put(c, FieldType.INT);
                    continue outer;
                }
            }
            for (String sc : bitColumns)
            {
                if (sc.equalsIgnoreCase(column))
                {
                    fieldTypes.put(c, FieldType.BIT);
                    continue outer;
                }
            }
            fieldTypes.put(c, defaultFieldType);
        }
        Set<String> pKey = new LinkedHashSet<String>();
        for (String column : primaryKey)
            pKey.add(getPreferredColumnName(column));
        exportData(table, outputFileBase, fieldMappings, fieldTypes);
    }

    private PrintWriter getBufferedPrintWriter(String fileName) throws IOException
    {
        return new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
    }

    private void appendTripData(TableDataSet table, TripStructureDefinition tripStructureDefinition)
    {
        // id triptype recid partysize orig_mgra dest_mgra trip_board_tap
        // trip_alight_tap trip_depart_time trip_time trip_distance trip_cost
        // trip_purpose_name trip_mode_name
        int rowCount = table.getRowCount();
        // columns to add: trip_time, trip_distance, trip_cost,
        // trip_purpose_name, trip_mode_name, recid
        float[] tripTime = new float[rowCount];
        float[] outVehTime = new float[rowCount];
        float[] tripDistance = new float[rowCount];
        float[] tripCost = new float[rowCount];
        String[] tripPurpose = new String[rowCount];
        String[] tripMode = new String[rowCount];
        int[] tripId = new int[rowCount];
        int[] tripDepartTime = new int[rowCount];
        int[] tripBoardTaz = new int[rowCount];
        int[] tripAlightTaz = new int[rowCount];

        SkimBuilder skimBuilder = new SkimBuilder(properties);
        boolean hasPurposeColumn = tripStructureDefinition.originPurposeColumn > -1;
        for (int i = 0; i < rowCount; i++)
        {
            int row = i + 1;

            double epsilon = .000001;
            boolean inbound = tripStructureDefinition.booleanIndicatorVariables ? table
                    .getBooleanValueAt(row, tripStructureDefinition.inboundColumn) : Math.abs(table
                    .getValueAt(row, tripStructureDefinition.inboundColumn) - 1.0) < epsilon;
            SkimBuilder.TripAttributes attributes = skimBuilder.getTripAttributes(
                    (int) table.getValueAt(row, tripStructureDefinition.originMgraColumn),
                    (int) table.getValueAt(row, tripStructureDefinition.destMgraColumn),
                    (int) table.getValueAt(row, tripStructureDefinition.modeColumn),
                    (int) table.getValueAt(row, tripStructureDefinition.boardTapColumn),
                    (int) table.getValueAt(row, tripStructureDefinition.alightTapColumn),
                    (int) table.getValueAt(row, tripStructureDefinition.todColumn), inbound);
            tripTime[i] = attributes.getTripTime();
            outVehTime[i] = attributes.getOutVehicleTime();
            tripDistance[i] = attributes.getTripDistance();
            tripCost[i] = attributes.getTripCost();
            if (hasPurposeColumn)
            {
                tripPurpose[i] = table.getStringValueAt(row,
                        tripStructureDefinition.destinationPurposeColumn);
            } else
            {
                if (!inbound) // going out
                tripPurpose[i] = tripStructureDefinition.destinationName;
                else tripPurpose[i] = tripStructureDefinition.homeName;
            }
            tripMode[i] = attributes.getTripModeName();
            tripId[i] = i;
            tripDepartTime[i] = attributes.getTripStartTime();
            tripBoardTaz[i] = attributes.getTripBoardTaz();
            tripAlightTaz[i] = attributes.getTripAlightTaz();
        }
        table.appendColumn(tripTime, "TRIP_TIME");
        table.appendColumn(outVehTime, "OUT_VEHICLE_TIME");
        table.appendColumn(tripDistance, "TRIP_DISTANCE");
        table.appendColumn(tripCost, "TRIP_COST");
        table.appendColumn(tripPurpose, "TRIP_PURPOSE_NAME");
        table.appendColumn(tripMode, "TRIP_MODE_NAME");
        table.appendColumn(tripId, "RECID");
        table.appendColumn(tripBoardTaz, "TRIP_BOARD_TAZ");
        table.appendColumn(tripAlightTaz, "TRIP_ALIGHT_TAZ");
    }

    private void exportAccessibilities(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>(Arrays.asList("mgra"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("mgra"));
        exportDataGeneric(outputFileBase, "acc.output.file", false, null, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private void exportMazData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>(Arrays.asList("mgra", "TAZ", "ZIP09"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("mgra"));
        exportDataGeneric(outputFileBase, "mgra.socec.file", false, null, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private void nullifyFile(String file)
    {
        String tempFile = file + ".temp";
        File f = new File(file);
        if (!f.renameTo(new File(tempFile)))
            throw new RuntimeException("Couldn't rename to file: " + f);
        BufferedReader reader = null;
        PrintWriter writer = null;
        try
        {
            reader = new BufferedReader(new FileReader(tempFile));
            writer = getBufferedPrintWriter(file);
            String line;
            while ((line = reader.readLine()) != null)
                writer.println(line.replace(NULL_VALUE, ""));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException e)
                {
                    // ignore
                }
            }
            if (writer != null) writer.close();
        }
        new File(tempFile).delete();
    }

    public static int    NULL_INT_VALUE   = -98765;
    public static float  NULL_FLOAT_VALUE = NULL_INT_VALUE;
    public static String NULL_VALUE       = "" + NULL_FLOAT_VALUE;

    private void exportTapData(String outputFileBase)
    {
        addTable(outputFileBase);
        Map<String, float[]> ptype = readSpaceDelimitedData(getPath("tap.ptype.file"),
                Arrays.asList("TAP", "LOTID", "PTYPE", "TAZ", "CAPACITY", "DISTANCE"));
        Map<String, float[]> pelev = readSpaceDelimitedData(
                getPath("tap.ptype.file").replace("ptype", "elev"), Arrays.asList("TAP", "ELEV"));
        float[] taps = ptype.get("TAP");
        float[] etaps = pelev.get("TAP");
        ptype.put("ELEV", getPartialData(taps, etaps, pelev.get("ELEV")));

        TableDataSet finalData = new TableDataSet();
        for (String columnName : ptype.keySet())
            finalData.appendColumn(ptype.get(columnName), columnName);

        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("TAP"));
        exportDataGeneric(finalData, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, FieldType.INT, primaryKey, null);
        nullifyFile(getOutputPath(outputFileBase + ".csv"));
    }

    private void exportMgraToTapData(String outputFileBase)
    {
        addTable(outputFileBase);
        Map<String, float[]> mgraToTap = readSpaceDelimitedData(
                getPath("mgra.wlkacc.taps.and.distance.file"),
                Arrays.asList("MGRA", "TAP", "DISTANCE"));
        TableDataSet finalData = new TableDataSet();
        for (String columnName : mgraToTap.keySet())
            finalData.appendColumn(mgraToTap.get(columnName), columnName);

        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("MGRA", "TAP"));
        exportDataGeneric(finalData, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, FieldType.INT, primaryKey, null);
        nullifyFile(getOutputPath(outputFileBase + ".csv"));
    }

    private void exportMgraToMgraData(String outputFileBase)
    {
        addTable(outputFileBase);
        Map<String, float[]> mgraToMgra = readSpaceDelimitedData(getPath("mgra.walkdistance.file"),
                Arrays.asList("ORIG_MGRA", "DEST_MGRA", "DISTANCE"));

        Map<String, List<Number>> actualData = new LinkedHashMap<String, List<Number>>();
        for (String column : Arrays.asList("TAZ", "ORIG_MGRA", "DEST_MGRA", "DISTANCE"))
            actualData.put(column, new LinkedList<Number>());
        float[] dcolumn = mgraToMgra.get("DISTANCE");
        float[] origColumn = mgraToMgra.get("ORIG_MGRA");
        float[] destColumn = mgraToMgra.get("DEST_MGRA");
        for (int i = 0; i < dcolumn.length; i++)
        {
            int count = 0;
            if (dcolumn[i] < 0) count = (int) destColumn[i];
            int taz = (int) origColumn[i];
            while (count-- > 0)
            {
                i++;
                actualData.get("TAZ").add(taz);
                actualData.get("ORIG_MGRA").add((int) origColumn[i]);
                actualData.get("DEST_MGRA").add((int) destColumn[i]);
                actualData.get("DISTANCE").add(dcolumn[i]);
            }
        }

        TableDataSet finalData = new TableDataSet();
        for (String columnName : actualData.keySet())
        {
            Object data;
            if (columnName.equals("DISTANCE"))
            {
                float[] dd = new float[actualData.get(columnName).size()];
                int counter = 0;
                for (Number n : actualData.get(columnName))
                    dd[counter++] = n.floatValue();
                data = dd;
            } else
            {
                int[] dd = new int[actualData.get(columnName).size()];
                int counter = 0;
                for (Number n : actualData.get(columnName))
                    dd[counter++] = n.intValue();
                data = dd;
            }
            finalData.appendColumn(data, columnName);
        }

        Set<String> intColumns = new HashSet<String>(Arrays.asList("TAZ", "ORIG_MGRA", "DEST_MGRA"));
        Set<String> floatColumns = new HashSet<String>(Arrays.asList("DISTANCE"));
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("ORIG_MGRA", "DEST_MGRA"));
        exportDataGeneric(finalData, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, FieldType.INT, primaryKey, null);
        nullifyFile(getOutputPath(outputFileBase + ".csv"));
    }

    private void exportTazToTapData(String outputFileBase)
    {
        addTable(outputFileBase);
        Map<String, float[]> tazToTap = readSpaceDelimitedData(
                getPath("taz.driveaccess.taps.file"), Arrays.asList("A", "B", "C"));

        Map<String, List<Number>> actualData = new LinkedHashMap<String, List<Number>>();
        for (String column : Arrays.asList("TAZ", "TAP", "TIME", "DISTANCE"))
            actualData.put(column, new LinkedList<Number>());
        float[] nullOrDistance = tazToTap.get("C");
        float[] origTazOrDestTap = tazToTap.get("A");
        float[] countOrTime = tazToTap.get("B");
        for (int i = 0; i < nullOrDistance.length; i++)
        {
            int count = 0;
            if (nullOrDistance[i] < 0) count = (int) countOrTime[i];
            int taz = (int) origTazOrDestTap[i];
            while (count-- > 0)
            {
                i++;
                actualData.get("TAZ").add(taz);
                actualData.get("TAP").add((int) origTazOrDestTap[i]);
                actualData.get("TIME").add(countOrTime[i]);
                actualData.get("DISTANCE").add(nullOrDistance[i]);
            }
        }

        TableDataSet finalData = new TableDataSet();
        for (String columnName : actualData.keySet())
        {
            Object data;
            if (columnName.equals("DISTANCE") || columnName.equals("TIME"))
            {
                float[] dd = new float[actualData.get(columnName).size()];
                int counter = 0;
                for (Number n : actualData.get(columnName))
                    dd[counter++] = n.floatValue();
                data = dd;
            } else
            {
                int[] dd = new int[actualData.get(columnName).size()];
                int counter = 0;
                for (Number n : actualData.get(columnName))
                    dd[counter++] = n.intValue();
                data = dd;
            }
            finalData.appendColumn(data, columnName);
        }

        Set<String> intColumns = new HashSet<String>(Arrays.asList("TAZ", "TAP"));
        Set<String> floatColumns = new HashSet<String>(Arrays.asList("TIME", "DISTANCE"));
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("TAZ", "TAP"));
        exportDataGeneric(finalData, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, FieldType.INT, primaryKey, null);
        nullifyFile(getOutputPath(outputFileBase + ".csv"));
    }

    private float[] toFloatArray(int[] data)
    {
        float[] f = new float[data.length];
        for (int i = 0; i < f.length; i++)
            f[i] = data[i];
        return f;
    }

    private float[] getPartialData(float[] fullKey, float[] partialKey, float[] partialData)
    {
        float[] data = new float[fullKey.length];
        Arrays.fill(data, NULL_FLOAT_VALUE);
        int counter = 0;
        for (float key : fullKey)
        {
            for (int i = 0; i < partialKey.length; i++)
            {
                if (partialKey[i] == key)
                {
                    data[counter] = partialData[i];
                }
            }
            counter++;
        }
        return data;
    }

    private void exportTazData(String outputFileBase)
    {
        addTable(outputFileBase);
        int[] tazs = getTazList();
        TableDataSet data = new TableDataSet();
        data.appendColumn(tazs, "TAZ");
        Map<String, float[]> term = readSpaceDelimitedData(getPath("taz.terminal.time.file"),
                Arrays.asList("TAZ", "TERM"));
        Map<String, float[]> park = readSpaceDelimitedData(getPath("taz.parkingtype.file"),
                Arrays.asList("TAZ", "PARK"));
        data.appendColumn(getPartialData(toFloatArray(tazs), term.get("TAZ"), term.get("TERM")),
                "TERM");
        data.appendColumn(getPartialData(toFloatArray(tazs), park.get("TAZ"), park.get("PARK")),
                "PARK");

        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("TAZ"));
        exportDataGeneric(data, outputFileBase, floatColumns, stringColumns, intColumns,
                bitColumns, FieldType.INT, primaryKey, null);
        nullifyFile(getOutputPath(outputFileBase + ".csv"));
    }

    private int[] getTazList()
    {
        Set<Integer> tazs = new TreeSet<Integer>();
        TableDataSet mgraData;
        try
        {
            mgraData = new CSVFileReader().readFile(new File(getPath("mgra.socec.file")));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        boolean first = true;
        for (int taz : mgraData.getColumnAsInt("taz"))
        {
            if (first)
            {
                first = false;
                continue;
            }
            tazs.add(taz);
        }
        int[] finalTazs = new int[tazs.size()];
        int counter = 0;
        for (int taz : tazs)
            finalTazs[counter++] = taz;
        return finalTazs;
    }

    private Map<String, float[]> readSpaceDelimitedData(String location, List<String> columnNames)
    {
        Map<String, List<Integer>> data = new LinkedHashMap<String, List<Integer>>();
        for (String columnName : columnNames)
            data.put(columnName, new LinkedList<Integer>());
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(location));
            String line;
            while ((line = reader.readLine()) != null)
            {
                String[] d = line.trim().split("\\s+");
                int counter = 0;
                for (String columnName : columnNames)
                {
                    if (counter < d.length)
                    {
                        data.get(columnName).add(Integer.parseInt(d[counter++]));
                    } else
                    {
                        data.get(columnName).add(NULL_INT_VALUE); // if missing
                                                                  // entry/entries,
                                                                  // then put in
                                                                  // null value
                    }
                }
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException e)
                {
                    // ignore
                }
            }
        }
        Map<String, float[]> d = new LinkedHashMap<String, float[]>();
        for (String columnName : columnNames)
        {
            float[] f = new float[data.get(columnName).size()];
            int counter = 0;
            for (Integer i : data.get(columnName))
                f[counter++] = i;
            d.put(columnName, f);
        }
        return d;
    }

    private void exportHouseholdData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // home_mgra
                NUMBER_FORMAT_NAME, // income
                NUMBER_FORMAT_NAME, // autos
                NUMBER_FORMAT_NAME, // transponder
                STRING_FORMAT_NAME, // cdap_pattern
                NUMBER_FORMAT_NAME, // jtf_choice
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("cdap_pattern"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("hh_id"));
        exportDataGeneric(outputFileBase, "Results.HouseholdDataFile", true, formats, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey, null);
    }

    private void exportPersonData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // person_id
                NUMBER_FORMAT_NAME, // person_num
                NUMBER_FORMAT_NAME, // age
                STRING_FORMAT_NAME, // gender
                STRING_FORMAT_NAME, // type
                NUMBER_FORMAT_NAME, // value_of_time (float)
                STRING_FORMAT_NAME, // activity_pattern
                NUMBER_FORMAT_NAME, // imf_choice
                NUMBER_FORMAT_NAME, // inmf_choice
                NUMBER_FORMAT_NAME, // fp_choice
                NUMBER_FORMAT_NAME, // reimb_pct (float)
                NUMBER_FORMAT_NAME, // ie_choice
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>(Arrays.asList("value_of_time", "reimb_pct"));
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("gender", "type",
                "activity_pattern"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("person_id"));
        exportDataGeneric(outputFileBase, "Results.PersonDataFile", true, formats, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey, null);
    }

    private void exportSyntheticHouseholdData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("HHID"));
        exportDataGeneric(outputFileBase, "PopulationSynthesizer.InputToCTRAMP.HouseholdFile",
                false, null, floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT,
                primaryKey, null);
    }

    private void exportSyntheticPersonData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // HHID
                NUMBER_FORMAT_NAME, // PERID
                NUMBER_FORMAT_NAME, // household_serial_no
                NUMBER_FORMAT_NAME, // PNUM
                NUMBER_FORMAT_NAME, // AGE
                NUMBER_FORMAT_NAME, // SEX
                NUMBER_FORMAT_NAME, // MILTARY
                NUMBER_FORMAT_NAME, // PEMPLOY
                NUMBER_FORMAT_NAME, // PSTUDENT
                NUMBER_FORMAT_NAME, // PTYPE
                NUMBER_FORMAT_NAME, // EDUC
                NUMBER_FORMAT_NAME, // GRADE
                NUMBER_FORMAT_NAME, // OCCCEN5
                STRING_FORMAT_NAME, // OCCSOC5
                NUMBER_FORMAT_NAME, // INDCEN
                NUMBER_FORMAT_NAME, // WEEKS
                NUMBER_FORMAT_NAME, // HOURS
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("OCCSOC5"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("PERID"));
        exportDataGeneric(outputFileBase, "PopulationSynthesizer.InputToCTRAMP.PersonFile", false,
                formats, floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT,
                primaryKey, null);
    }

    private void exportWorkSchoolLocation(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>(Arrays.asList("WorkLocationDistance",
                "WorkLocationLogsum", "SchoolLocation", "SchoolLocationDistance",
                "SchoolLocationLogsum"));
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("PERSON_ID"));
        Map<String, String> overridingNames = new HashMap<String, String>();
        overridingNames.put("PersonID", "PERSON_ID");
        exportDataGeneric(outputFileBase, "Results.UsualWorkAndSchoolLocationChoice", true, null,
                floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                overridingNames, null);
    }

    private void exportIndivToursData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // person_id
                NUMBER_FORMAT_NAME, // person_num
                NUMBER_FORMAT_NAME, // person_type
                NUMBER_FORMAT_NAME, // tour_id
                STRING_FORMAT_NAME, // tour_category
                STRING_FORMAT_NAME, // tour_purpose
                NUMBER_FORMAT_NAME, // orig_mgra
                NUMBER_FORMAT_NAME, // dest_mgra
                NUMBER_FORMAT_NAME, // start_period
                NUMBER_FORMAT_NAME, // end_period
                NUMBER_FORMAT_NAME, // tour_mode
                NUMBER_FORMAT_NAME, // tour_distance
                NUMBER_FORMAT_NAME, // atWork_freq
                NUMBER_FORMAT_NAME, // num_ob_stops
                NUMBER_FORMAT_NAME, // num_ib_stops
                NUMBER_FORMAT_NAME, // util_1
                NUMBER_FORMAT_NAME, // util_2
                NUMBER_FORMAT_NAME, // util_3
                NUMBER_FORMAT_NAME, // util_4
                NUMBER_FORMAT_NAME, // util_5
                NUMBER_FORMAT_NAME, // util_6
                NUMBER_FORMAT_NAME, // util_7
                NUMBER_FORMAT_NAME, // util_8
                NUMBER_FORMAT_NAME, // util_9
                NUMBER_FORMAT_NAME, // util_10
                NUMBER_FORMAT_NAME, // util_11
                NUMBER_FORMAT_NAME, // util_12
                NUMBER_FORMAT_NAME, // util_13
                NUMBER_FORMAT_NAME, // util_14
                NUMBER_FORMAT_NAME, // util_15
                NUMBER_FORMAT_NAME, // util_16
                NUMBER_FORMAT_NAME, // util_17
                NUMBER_FORMAT_NAME, // util_18
                NUMBER_FORMAT_NAME, // util_19
                NUMBER_FORMAT_NAME, // util_20
                NUMBER_FORMAT_NAME, // util_21
                NUMBER_FORMAT_NAME, // util_22
                NUMBER_FORMAT_NAME, // util_23
                NUMBER_FORMAT_NAME, // util_24
                NUMBER_FORMAT_NAME, // util_25
                NUMBER_FORMAT_NAME, // util_26
                NUMBER_FORMAT_NAME, // prob_1
                NUMBER_FORMAT_NAME, // prob_2
                NUMBER_FORMAT_NAME, // prob_3
                NUMBER_FORMAT_NAME, // prob_4
                NUMBER_FORMAT_NAME, // prob_5
                NUMBER_FORMAT_NAME, // prob_6
                NUMBER_FORMAT_NAME, // prob_7
                NUMBER_FORMAT_NAME, // prob_8
                NUMBER_FORMAT_NAME, // prob_9
                NUMBER_FORMAT_NAME, // prob_10
                NUMBER_FORMAT_NAME, // prob_11
                NUMBER_FORMAT_NAME, // prob_12
                NUMBER_FORMAT_NAME, // prob_13
                NUMBER_FORMAT_NAME, // prob_14
                NUMBER_FORMAT_NAME, // prob_15
                NUMBER_FORMAT_NAME, // prob_16
                NUMBER_FORMAT_NAME, // prob_17
                NUMBER_FORMAT_NAME, // prob_18
                NUMBER_FORMAT_NAME, // prob_19
                NUMBER_FORMAT_NAME, // prob_20
                NUMBER_FORMAT_NAME, // prob_21
                NUMBER_FORMAT_NAME, // prob_22
                NUMBER_FORMAT_NAME, // prob_23
                NUMBER_FORMAT_NAME, // prob_24
                NUMBER_FORMAT_NAME, // prob_25
                NUMBER_FORMAT_NAME // prob_26
        };
        Set<String> intColumns = new HashSet<String>(Arrays.asList("hh_id", "person_id",
                "person_num", "person_type", "tour_id", "orig_mgra", "dest_mgra", "start_period",
                "end_period", "tour_mode", "atWork_freq", "num_ob_stops", "num_ib_stops"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("tour_category",
                "tour_purpose"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("hh_id", "person_id",
                "tour_category", "tour_id", "tour_purpose"));
        exportDataGeneric(outputFileBase, "Results.IndivTourDataFile", true, formats, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private void exportJointToursData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // tour_id
                STRING_FORMAT_NAME, // tour_category
                STRING_FORMAT_NAME, // tour_purpose
                NUMBER_FORMAT_NAME, // tour_composition
                STRING_FORMAT_NAME, // tour_participants
                NUMBER_FORMAT_NAME, // orig_mgra
                NUMBER_FORMAT_NAME, // dest_mgra
                NUMBER_FORMAT_NAME, // start_period
                NUMBER_FORMAT_NAME, // end_period
                NUMBER_FORMAT_NAME, // tour_mode
                NUMBER_FORMAT_NAME, // tour_distance
                NUMBER_FORMAT_NAME, // num_ob_stops
                NUMBER_FORMAT_NAME, // num_ib_stops
                NUMBER_FORMAT_NAME, // util_1
                NUMBER_FORMAT_NAME, // util_2
                NUMBER_FORMAT_NAME, // util_3
                NUMBER_FORMAT_NAME, // util_4
                NUMBER_FORMAT_NAME, // util_5
                NUMBER_FORMAT_NAME, // util_6
                NUMBER_FORMAT_NAME, // util_7
                NUMBER_FORMAT_NAME, // util_8
                NUMBER_FORMAT_NAME, // util_9
                NUMBER_FORMAT_NAME, // util_10
                NUMBER_FORMAT_NAME, // util_11
                NUMBER_FORMAT_NAME, // util_12
                NUMBER_FORMAT_NAME, // util_13
                NUMBER_FORMAT_NAME, // util_14
                NUMBER_FORMAT_NAME, // util_15
                NUMBER_FORMAT_NAME, // util_16
                NUMBER_FORMAT_NAME, // util_17
                NUMBER_FORMAT_NAME, // util_18
                NUMBER_FORMAT_NAME, // util_19
                NUMBER_FORMAT_NAME, // util_20
                NUMBER_FORMAT_NAME, // util_21
                NUMBER_FORMAT_NAME, // util_22
                NUMBER_FORMAT_NAME, // util_23
                NUMBER_FORMAT_NAME, // util_24
                NUMBER_FORMAT_NAME, // util_25
                NUMBER_FORMAT_NAME, // util_26
                NUMBER_FORMAT_NAME, // prob_1
                NUMBER_FORMAT_NAME, // prob_2
                NUMBER_FORMAT_NAME, // prob_3
                NUMBER_FORMAT_NAME, // prob_4
                NUMBER_FORMAT_NAME, // prob_5
                NUMBER_FORMAT_NAME, // prob_6
                NUMBER_FORMAT_NAME, // prob_7
                NUMBER_FORMAT_NAME, // prob_8
                NUMBER_FORMAT_NAME, // prob_9
                NUMBER_FORMAT_NAME, // prob_10
                NUMBER_FORMAT_NAME, // prob_11
                NUMBER_FORMAT_NAME, // prob_12
                NUMBER_FORMAT_NAME, // prob_13
                NUMBER_FORMAT_NAME, // prob_14
                NUMBER_FORMAT_NAME, // prob_15
                NUMBER_FORMAT_NAME, // prob_16
                NUMBER_FORMAT_NAME, // prob_17
                NUMBER_FORMAT_NAME, // prob_18
                NUMBER_FORMAT_NAME, // prob_19
                NUMBER_FORMAT_NAME, // prob_20
                NUMBER_FORMAT_NAME, // prob_21
                NUMBER_FORMAT_NAME, // prob_22
                NUMBER_FORMAT_NAME, // prob_23
                NUMBER_FORMAT_NAME, // prob_24
                NUMBER_FORMAT_NAME, // prob_25
                NUMBER_FORMAT_NAME // prob_26
        };
        Set<String> intColumns = new HashSet<String>(Arrays.asList("hh_id", "tour_id",
                "tour_composition", "orig_mgra", "dest_mgra", "start_period", "end_period",
                "tour_mode", "num_ob_stops", "num_ib_stops"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("tour_category",
                "tour_purpose", "tour_participants"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("hh_id", "tour_category",
                "tour_id", "tour_purpose"));
        exportDataGeneric(outputFileBase, "Results.JointTourDataFile", true, formats, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private void exportIndivTripData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // person_id
                NUMBER_FORMAT_NAME, // person_num
                NUMBER_FORMAT_NAME, // tour_id
                NUMBER_FORMAT_NAME, // stop_id
                NUMBER_FORMAT_NAME, // inbound
                STRING_FORMAT_NAME, // tour_purpose
                STRING_FORMAT_NAME, // orig_purpose
                STRING_FORMAT_NAME, // dest_purpose
                NUMBER_FORMAT_NAME, // orig_mgra
                NUMBER_FORMAT_NAME, // dest_mgra
                NUMBER_FORMAT_NAME, // parking_mgra
                NUMBER_FORMAT_NAME, // stop_period
                NUMBER_FORMAT_NAME, // trip_mode
                NUMBER_FORMAT_NAME, // trip_board_tap
                NUMBER_FORMAT_NAME, // trip_alight_tap
                NUMBER_FORMAT_NAME // tour_mode
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("tour_purpose",
                "orig_purpose", "dest_purpose"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("hh_id", "person_id",
                "tour_id", "tour_purpose", "inbound", "stop_id"));
        exportDataGeneric(
                outputFileBase,
                "Results.IndivTripDataFile",
                true,
                formats,
                floatColumns,
                stringColumns,
                intColumns,
                bitColumns,
                FieldType.INT,
                primaryKey,
                new TripStructureDefinition(10, 11, 8, 9, 13, 14, 15, 16, -1, 17, "INDIV", 6, false));
    }

    private void exportJointTripData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // hh_id
                NUMBER_FORMAT_NAME, // tour_id
                NUMBER_FORMAT_NAME, // stop_id
                NUMBER_FORMAT_NAME, // inbound
                STRING_FORMAT_NAME, // tour_purpose
                STRING_FORMAT_NAME, // orig_purpose
                STRING_FORMAT_NAME, // dest_purpose
                NUMBER_FORMAT_NAME, // orig_mgra
                NUMBER_FORMAT_NAME, // dest_mgra
                NUMBER_FORMAT_NAME, // parking_mgra
                NUMBER_FORMAT_NAME, // stop_period
                NUMBER_FORMAT_NAME, // trip_mode
                NUMBER_FORMAT_NAME, // num_participants
                NUMBER_FORMAT_NAME, // trip_board_tap
                NUMBER_FORMAT_NAME, // trip_alight_tap
                NUMBER_FORMAT_NAME // tour_mode
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("tour_purpose",
                "orig_purpose", "dest_purpose"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("hh_id", "tour_id",
                "tour_purpose", "inbound", "stop_id"));
        exportDataGeneric(outputFileBase, "Results.JointTripDataFile", true, formats, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                new TripStructureDefinition(8, 9, 6, 7, 11, 12, 14, 15, 13, 16, "JOINT", 4, false));
    }

    private void exportAirportTrips(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("id"));
        Map<String, String> overridingNames = new HashMap<String, String>();
        // overridingNames.put("id","PARTYID");
        exportDataGeneric(outputFileBase, "airport.output.file", false, null, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey, overridingNames,
                new TripStructureDefinition(8, 9, 7, 10, 12, 13, 4, 13, "AIRPORT", "HOME",
                        "AIRPORT", 2, false));
    }

    private void exportCrossBorderTourData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // id
                NUMBER_FORMAT_NAME, // purpose
                STRING_FORMAT_NAME, // sentri
                NUMBER_FORMAT_NAME, // poe
                NUMBER_FORMAT_NAME, // departTime
                NUMBER_FORMAT_NAME, // arriveTime
                NUMBER_FORMAT_NAME, // originMGRA
                NUMBER_FORMAT_NAME, // destinationMGRA
                NUMBER_FORMAT_NAME, // origTaz
                NUMBER_FORMAT_NAME, // destTaz
                NUMBER_FORMAT_NAME // tourMode
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>(Arrays.asList("sentri"));
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("TOURID"));
        Map<String, String> overridingNames = new HashMap<String, String>();
        overridingNames.put("id", "TOURID");
        exportDataGeneric(outputFileBase, "crossBorder.tour.output.file", false, formats,
                floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                overridingNames, null);
    }

    private void exportCrossBorderTripData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // tourID
                NUMBER_FORMAT_NAME, // tripID
                NUMBER_FORMAT_NAME, // originPurp
                NUMBER_FORMAT_NAME, // destPurp
                NUMBER_FORMAT_NAME, // originMGRA
                NUMBER_FORMAT_NAME, // destinationMGRA
                NUMBER_FORMAT_NAME, // originTAZ
                NUMBER_FORMAT_NAME, // destinationTAZ
                STRING_FORMAT_NAME, // inbound
                STRING_FORMAT_NAME, // originIsTourDestination
                STRING_FORMAT_NAME, // destinationIsTourDestination
                NUMBER_FORMAT_NAME, // period
                NUMBER_FORMAT_NAME, // tripMode
                NUMBER_FORMAT_NAME, // boardingTap
                NUMBER_FORMAT_NAME, // alightingTap
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("inbound",
                "originIsTourDestination", "destinationIsTourDestination"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("tourID", "tripID"));
        Map<String, String> overridingNames = new HashMap<String, String>();
        overridingNames.put("id", "TOURID");
        exportDataGeneric(outputFileBase, "crossBorder.trip.output.file", false, formats,
                floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                overridingNames, new TripStructureDefinition(5, 6, 3, 4, 12, 13, 14, 15, -1, 15,
                        "CB", 9, true));
    }

    private void exportVisitorData(String outputTourFileBase, String outputTripFileBase)
    {
        TableDataSet tourData = exportVisitorTourData(outputTourFileBase);
        String tourIdField = "id";
        String partySizeField = "partySize";
        Map<Integer, Integer> tourIdToPartySize = new HashMap<Integer, Integer>();
        int[] ids = tourData.getColumnAsInt(tourIdField);
        int[] partySize = tourData.getColumnAsInt(partySizeField);
        for (int i = 0; i < ids.length; i++)
            tourIdToPartySize.put(ids[i], partySize[i]);
        exportVisitorTripData(outputTripFileBase, tourIdToPartySize);
    }

    private TableDataSet exportVisitorTourData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("id", "segment"));
        Map<String, String> overridingNames = new HashMap<String, String>();
        // overridingNames.put("id","PARTYID");
        return exportDataGeneric(outputFileBase, "visitor.tour.output.file", false, null,
                floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                overridingNames, null);
    }

    private void exportVisitorTripData(String outputFileBase, Map<Integer, Integer> tourIdToPartyMap)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // tourID
                NUMBER_FORMAT_NAME, // tripID
                NUMBER_FORMAT_NAME, // originPurp
                NUMBER_FORMAT_NAME, // destPurp
                NUMBER_FORMAT_NAME, // originMGRA
                NUMBER_FORMAT_NAME, // destinationMGRA
                STRING_FORMAT_NAME, // inbound
                STRING_FORMAT_NAME, // originIsTourDestination
                STRING_FORMAT_NAME, // destinationIsTourDestination
                NUMBER_FORMAT_NAME, // period
                NUMBER_FORMAT_NAME, // tripMode
                NUMBER_FORMAT_NAME, // boardingTap
                NUMBER_FORMAT_NAME, // alightingTap
                NUMBER_FORMAT_NAME // partySize (added)
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("inbound",
                "originIsTourDestination", "destinationIsTourDestination"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("tourID", "tripId"));
        primaryKey = new LinkedHashSet<String>(Arrays.asList("RECID")); // todo:
                                                                        // temporary
                                                                        // until
                                                                        // bugfix
        JoinData joinData = new JoinData("tourID");
        joinData.addJoinData(tourIdToPartyMap, FieldType.INT, "partySize");
        exportDataGeneric(
                outputFileBase,
                "visitor.trip.output.file",
                false,
                formats,
                floatColumns,
                stringColumns,
                intColumns,
                bitColumns,
                FieldType.INT,
                primaryKey,
                new TripStructureDefinition(5, 6, 3, 4, 10, 11, 12, 13, 14, 14, "VISITOR", 7, true),
                joinData);
    }

    private void exportInternalExternalTripData(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] formats = {NUMBER_FORMAT_NAME, // originMGRA
                NUMBER_FORMAT_NAME, // destinationMGRA
                NUMBER_FORMAT_NAME, // originTAZ
                NUMBER_FORMAT_NAME, // destinationTAZ
                STRING_FORMAT_NAME, // inbound
                STRING_FORMAT_NAME, // originIsTourDestination
                STRING_FORMAT_NAME, // destinationIsTourDestination
                NUMBER_FORMAT_NAME, // period
                NUMBER_FORMAT_NAME, // tripMode
                NUMBER_FORMAT_NAME, // boardingTap
                NUMBER_FORMAT_NAME, // alightingTap
        };
        Set<String> intColumns = new HashSet<String>();
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>(Arrays.asList("inbound",
                "originIsTourDestination", "destinationIsTourDestination"));
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>();
        exportDataGeneric(outputFileBase, "internalExternal.trip.output.file", false, formats,
                floatColumns, stringColumns, intColumns, bitColumns, FieldType.INT, primaryKey,
                new TripStructureDefinition(1, 2, 8, 9, 10, 11, -1, 11, "IE", "HOME", "EXTERNAL",
                        5, true));
    }

    private Set<Integer> getExternalZones()
    {
        Set<Integer> externalZones = new LinkedHashSet<Integer>();
        for (String zone : ((String) properties.get("external.tazs")).trim().split(","))
            externalZones.add(Integer.parseInt(zone.trim()));
        return externalZones;
    }

    private void exportCommVehData(String outputFileBase) throws IOException
    {
        addTable(outputFileBase);
        Set<Integer> internalZones = new LinkedHashSet<Integer>();
        DecimalFormat formatter = new DecimalFormat("#.######");

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(new File(getOutputPath(outputFileBase
                    + ".csv"))), 1024 * 1024 * 1024);

            CsvRow headerRow = new CsvRow(new String[] {"ORIG_TAZ", "DEST_TAZ", "TOD",
                    "TRIPS_COMMVEH"});
            writer.write(headerRow.getRow());

            for (String period : timePeriods)
            {
                Matrix matrixData = mtxDao.getMatrix("commVehTODTrips", period + " Trips");

                // This doesn't make sense
                if (internalZones.isEmpty()) for (int zone : matrixData.getExternalColumnNumbers())
                    internalZones.add(zone);

                for (int i : internalZones)
                {
                    for (int j : internalZones)
                    {
                        float value = matrixData.getValueAt(i, j);
                        if (value > .00001)
                        {
                            String[] rowValue = new String[4];
                            rowValue[0] = String.valueOf(i);
                            rowValue[1] = String.valueOf(j);
                            rowValue[2] = period;
                            rowValue[3] = formatter.format(value);
                            CsvRow dataRow = new CsvRow(rowValue);
                            writer.write(dataRow.getRow());
                        }
                    }
                }
            }
        } finally
        {
            if (writer != null) writer.close();
        }
    }

    private void exportExternalInternalTripData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<Integer> internalZones = new LinkedHashSet<Integer>();
        Set<Integer> externalZones = getExternalZones();
        List<String> cores = Arrays.asList("DAN", "S2N", "S3N", "DAT", "S2T", "S3T");
        Map<String, String> purposeMap = new HashMap<String, String>();
        purposeMap.put("WORK", "Wrk");
        purposeMap.put("NONWORK", "Non");

        Matrix[] matrixData = new Matrix[cores.size()];

        PrintWriter writer = null;
        try
        {
            writer = getBufferedPrintWriter(getOutputPath(outputFileBase + ".csv"));

            StringBuilder sb = new StringBuilder();
            sb.append("ORIG_TAZ,DEST_TAZ,TOD,PURPOSE");
            for (String core : cores)
                sb.append(",").append("TRIPS_").append(core);
            writer.println(sb.toString());

            for (String period : timePeriods)
            {
                for (String purpose : purposeMap.keySet())
                {
                    int counter = 0;
                    for (String core : cores)
                        matrixData[counter++] = mtxDao.getMatrix("usSd" + purposeMap.get(purpose)
                                + "_" + period, core);

                    if (internalZones.size() == 0)
                    { // only need to form internal zones once
                        for (int zone : matrixData[0].getExternalColumnNumbers())
                            internalZones.add(zone);
                        internalZones.removeAll(externalZones);
                    }

                    for (int i : internalZones)
                    {
                        for (int e : externalZones)
                        {
                            StringBuilder sbie = new StringBuilder();
                            StringBuilder sbei = new StringBuilder();
                            sbie.append(i).append(",").append(e).append(",").append(period)
                                    .append(",").append(purpose);
                            sbei.append(e).append(",").append(i).append(",").append(period)
                                    .append(",").append(purpose);
                            float ie = 0;
                            float ei = 0;

                            for (Matrix matrix : matrixData)
                            {
                                float vie = matrix.getValueAt(i, e);
                                float vei = matrix.getValueAt(e, i);
                                ie += vie;
                                ei += vei;
                                sbie.append(",").append(vie);
                                sbei.append(",").append(vei);
                            }
                            if (ie > 0) writer.println(sbie.toString());
                            if (ei > 0) writer.println(sbei.toString());
                        }
                    }
                }
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (writer != null) writer.close();
        }
    }

    private void exportExternalExternalTripData(String outputFileBase) throws IOException
    {
        addTable(outputFileBase);
        Set<Integer> externalZones = getExternalZones();

        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(new File(getOutputPath(outputFileBase
                    + ".csv"))), 1024 * 1024 * 1024);

            CsvRow headerRow = new CsvRow(new String[] {"ORIG_TAZ", "DEST_TAZ", "TRIPS_EE"});
            writer.write(headerRow.getRow());

            Matrix m = mtxDao.getMatrix("externalExternalTrips", "Trips");

            for (int o : externalZones)
            {
                for (int d : externalZones)
                {
                    String[] values = new String[3];
                    values[0] = String.valueOf(o);
                    values[1] = String.valueOf(d);
                    values[2] = String.valueOf(m.getValueAt(o, d));
                    CsvRow dataRow = new CsvRow(values);
                    writer.write(dataRow.getRow());
                }
            }
        } finally
        {
            if (writer != null) writer.close();
        }
    }

    private Map<String, String> getVehicleSkimFileNameMapping()
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("impdat_" + TOD_TOKEN, "DRIVE_ALONE_TOLL");
        map.put("impdan_" + TOD_TOKEN, "DRIVE_ALONE_FREE");
        map.put("imps2th_" + TOD_TOKEN, "HOV2_TOLL");
        map.put("imps2nh_" + TOD_TOKEN, "HOV2_FREE");
        map.put("imps3th_" + TOD_TOKEN, "HOV3_TOLL");
        map.put("imps3nh_" + TOD_TOKEN, "HOV3_FREE");
        map.put("imphhdt_" + TOD_TOKEN, "TRUCK_HH_TOLL");
        map.put("imphhdn_" + TOD_TOKEN, "TRUCK_HH_FREE");

        return map;
    }

    private Map<String, String[]> getVehicleSkimFileCoreNameMapping()
    { // distance,time,cost
        Map<String, String[]> map = new LinkedHashMap<String, String[]>();
        map.put("impdat_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*STM_" + TOD_TOKEN + " (Skim)", "dat_" + TOD_TOKEN + " - itoll_" + TOD_TOKEN});
        map.put("impdan_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*STM_" + TOD_TOKEN + " (Skim)"});
        map.put("imps2th_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*HTM_" + TOD_TOKEN + " (Skim)", "s2t_" + TOD_TOKEN + " - itoll_" + TOD_TOKEN});
        map.put("imps2nh_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*HTM_" + TOD_TOKEN + " (Skim)"});
        map.put("imps3th_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*HTM_" + TOD_TOKEN + " (Skim)", "s3t_" + TOD_TOKEN + " - itoll_" + TOD_TOKEN});
        map.put("imps3nh_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*HTM_" + TOD_TOKEN + " (Skim)"});
        map.put("imphhdt_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*STM_" + TOD_TOKEN + " (Skim)", "hhdt - ITOLL2_" + TOD_TOKEN});
        map.put("imphhdn_" + TOD_TOKEN, new String[] {"Length (Skim)",
                "*STM_" + TOD_TOKEN + " (Skim)"});
        return map;
    }

    private void exportAutoSkims(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] includedTimePeriods = getTimePeriodsForSkims();
        Set<Integer> internalZones = new LinkedHashSet<Integer>();

        BlockingQueue<CsvRow> queue = new LinkedBlockingQueue<CsvRow>();
        try
        {
            Map<String, String> vehicleSkimFiles = getVehicleSkimFileNameMapping();
            Map<String, String[]> vehicleSkimCores = getVehicleSkimFileCoreNameMapping();
            Set<String> modeNames = new LinkedHashSet<String>();
            for (String n : vehicleSkimFiles.keySet())
                modeNames.add(vehicleSkimFiles.get(n));
            boolean first = true;

            for (String period : includedTimePeriods)
            {
                Map<String, Matrix> lengthMatrix = new LinkedHashMap<String, Matrix>();
                Map<String, Matrix> timeMatrix = new LinkedHashMap<String, Matrix>();
                Map<String, Matrix> fareMatrix = new LinkedHashMap<String, Matrix>();

                for (String key : vehicleSkimCores.keySet())
                {
                    String name = vehicleSkimFiles.get(key);
                    String[] cores = vehicleSkimCores.get(key);
                    String file = key.replace(TOD_TOKEN, period);
                    lengthMatrix.put(name,
                            mtxDao.getMatrix(file, cores[0].replace(TOD_TOKEN, period)));
                    timeMatrix.put(name,
                            mtxDao.getMatrix(file, cores[1].replace(TOD_TOKEN, period)));
                    if (cores.length > 2)
                        fareMatrix.put(name,
                                mtxDao.getMatrix(file, cores[2].replace(TOD_TOKEN, period)));
                    if (internalZones.size() == 0)
                    {
                        boolean f = true;
                        for (int zone : lengthMatrix.get(name).getExternalColumnNumbers())
                        {
                            if (f)
                            {
                                f = false;
                                continue;
                            }
                            internalZones.add(zone);
                        }
                    }
                }

                // put data into arrays for faster access
                Matrix[] orderedData = new Matrix[lengthMatrix.size() + timeMatrix.size()
                        + fareMatrix.size()];
                int counter = 0;
                for (String mode : modeNames)
                {
                    orderedData[counter++] = lengthMatrix.get(mode);
                    orderedData[counter++] = timeMatrix.get(mode);
                    if (fareMatrix.containsKey(mode))
                        orderedData[counter++] = fareMatrix.get(mode);
                }

                if (first)
                {
                    List<String> header = new ArrayList<String>();
                    header.add("ORIG_TAZ");
                    header.add("DEST_TAZ");
                    header.add("TOD");

                    for (String modeName : modeNames)
                    {
                        header.add("DIST_" + modeName);
                        header.add("TIME_" + modeName);
                        if (fareMatrix.containsKey(modeName))
                        {
                            header.add("COST_" + modeName);
                        }
                    }

                    CsvWriterThread writerThread = new CsvWriterThread(queue, new File(
                            getOutputPath(outputFileBase + ".csv")),
                            header.toArray(new String[header.size()]));
                    new Thread(writerThread).start();
                    first = false;
                }

                int rowSize = 3 + orderedData.length;

                for (int i : internalZones)
                {
                    for (int j : internalZones)
                    {
                        String[] values = new String[rowSize];
                        values[0] = String.valueOf(i);
                        values[1] = String.valueOf(j);
                        values[2] = period;
                        int position = 3;
                        for (Matrix matrix : orderedData)
                            values[position++] = DoubleFormatUtil.formatDouble(
                                    matrix.getValueAt(i, j), 4, 4);
                        queue.add(new CsvRow(values));
                    }
                }
            }

        } finally
        {
            queue.add(CsvWriterThread.POISON_PILL);
        }
    }

    private Map<String, String> getTransitSkimFileNameMapping()
    {
        Map<String, String> map = new LinkedHashMap<String, String>();
        // map.put("implocl_" + TOD_TOKEN + "o", "LOCAL_TRANSIT");
        map.put("impprem_" + TOD_TOKEN + "o", "PREMIUM_TRANSIT");
        return map;
    }

    private String getTransitSkimFileFareCoreName()
    {
        return "Fare";
    }

    private Map<String, String[]> getTransitSkimFileInVehicleTimeCoreNameMapping()
    { // distance,time,cost
        Map<String, String[]> map = new LinkedHashMap<String, String[]>();
        map.put("impprem_" + TOD_TOKEN + "o", new String[] {"IVT:CR", "IVT:LR", "IVT:BRT",
                "IVT:EXP", "IVT:LB"});
        return map;
    }

    private String[] getTimePeriodsForSkims()
    {
        return IExporter.TOD_TOKENS;
    }

    private void exportTransitSkims(String outputFileBase)
    {
        addTable(outputFileBase);
        String[] includedTimePeriods = getTimePeriodsForSkims();

        Set<Integer> internalZones = new LinkedHashSet<Integer>();

        BlockingQueue<CsvRow> queue = new LinkedBlockingQueue<CsvRow>();
        try
        {
            Map<String, String> transitSkimFiles = getTransitSkimFileNameMapping();
            Map<String, String[]> transitSkimTimeCores = getTransitSkimFileInVehicleTimeCoreNameMapping();
            String fareCore = getTransitSkimFileFareCoreName();
            String initialWaitCore = "Initial Wait Time";
            String transferTimeCore = "Transfer Wait Time";
            String walkTimeCore = "Walk Time";
            Set<String> modeNames = new LinkedHashSet<String>();
            for (String n : transitSkimFiles.keySet())
                modeNames.add(transitSkimFiles.get(n));
            boolean first = true;
            int numOfColumns = 3 + 5 * modeNames.size();
            for (String period : includedTimePeriods)
            {
                Map<String, Matrix[]> timeMatrix = new LinkedHashMap<String, Matrix[]>();
                Map<String, Matrix> fareMatrix = new LinkedHashMap<String, Matrix>();
                Map<String, Matrix> initialMatrix = new LinkedHashMap<String, Matrix>();
                Map<String, Matrix> transferMatrix = new LinkedHashMap<String, Matrix>();
                Map<String, Matrix> walkTimeMatrix = new LinkedHashMap<String, Matrix>();

                for (String key : transitSkimFiles.keySet())
                {
                    String name = transitSkimFiles.get(key);
                    String[] timeCores = transitSkimTimeCores.get(key);
                    String file = key.replace(TOD_TOKEN, period);
                    Matrix[] timeMatrices = new Matrix[timeCores.length];
                    for (int i = 0; i < timeCores.length; i++)
                        timeMatrices[i] = mtxDao.getMatrix(file,
                                timeCores[i].replace(TOD_TOKEN, period));
                    timeMatrix.put(name, timeMatrices);
                    fareMatrix.put(name,
                            mtxDao.getMatrix(file, fareCore.replace(TOD_TOKEN, period)));
                    initialMatrix.put(name, mtxDao.getMatrix(file, initialWaitCore));
                    transferMatrix.put(name, mtxDao.getMatrix(file, transferTimeCore));
                    walkTimeMatrix.put(name, mtxDao.getMatrix(file, walkTimeCore));
                    if (internalZones.size() == 0)
                    {
                        boolean f = true;
                        for (int zone : fareMatrix.get(name).getExternalColumnNumbers())
                        {
                            if (f)
                            {
                                f = false;
                                continue;
                            }
                            internalZones.add(zone);
                        }
                    }
                }

                // put data into arrays for faster access
                Matrix[][] orderedTimeData = new Matrix[timeMatrix.size()][];
                Matrix[] fareData = new Matrix[orderedTimeData.length];
                Matrix[] initialWaitData = new Matrix[orderedTimeData.length];
                Matrix[] transferTimeData = new Matrix[orderedTimeData.length];
                Matrix[] walkTimeData = new Matrix[orderedTimeData.length];

                int counter = 0;
                for (String mode : modeNames)
                {
                    orderedTimeData[counter] = timeMatrix.get(mode);
                    fareData[counter] = fareMatrix.get(mode);
                    initialWaitData[counter] = initialMatrix.get(mode);
                    transferTimeData[counter] = transferMatrix.get(mode);
                    walkTimeData[counter++] = walkTimeMatrix.get(mode);
                }

                if (first)
                {
                    String[] header = new String[numOfColumns];

                    header[0] = "ORIG_TAP";
                    header[1] = "DEST_TAP";
                    header[2] = "TOD";
                    int column = 3;

                    for (String modeName : modeNames)
                    {
                        header[column++] = "TIME_INIT_WAIT_" + modeName;
                        header[column++] = "TIME_IVT_TIME_" + modeName;
                        header[column++] = "TIME_WALK_TIME_" + modeName;
                        header[column++] = "TIME_TRANSFER_TIME_" + modeName;
                        header[column++] = "FARE_" + modeName;
                    }

                    CsvWriterThread writerThread = new CsvWriterThread(queue, new File(
                            getOutputPath(outputFileBase + ".csv")), header);
                    new Thread(writerThread).start();

                    first = false;
                }

                for (int i : internalZones)
                {
                    for (int j : internalZones)
                    {
                        String[] values = new String[numOfColumns];
                        values[0] = String.valueOf(i);
                        values[1] = String.valueOf(j);
                        values[2] = period;

                        int column = 3;
                        float runningTotal = 0.0f;

                        for (int m = 0; m < orderedTimeData.length; m++)
                        {
                            float time = 0.0f;
                            float initTime = initialWaitData[m].getValueAt(i, j);
                            for (Matrix tm : orderedTimeData[m])
                                time += tm.getValueAt(i, j);
                            float walkTime = walkTimeData[m].getValueAt(i, j);
                            float transferTime = transferTimeData[m].getValueAt(i, j);
                            float fare = fareData[m].getValueAt(i, j);
                            runningTotal += fare + time;
                            values[column++] = DoubleFormatUtil.formatDouble(initTime, 4, 4);
                            values[column++] = DoubleFormatUtil.formatDouble(time, 4, 4);
                            values[column++] = DoubleFormatUtil.formatDouble(walkTime, 4, 4);
                            values[column++] = DoubleFormatUtil.formatDouble(transferTime, 4, 4);
                            values[column++] = DoubleFormatUtil.formatDouble(fare, 2, 2);
                        }
                        if (runningTotal > 0.0f) queue.add(new CsvRow(values));
                    }
                }
            }

        } finally
        {
            queue.add(CsvWriterThread.POISON_PILL);
        }
    }

    private void exportDefinitions(String outputFileBase)
    {
        addTable(outputFileBase);
        Map<String, String> tripPurposes = new LinkedHashMap<String, String>();
        Map<String, String> modes = new LinkedHashMap<String, String>();
        Map<String, String> ejCategories = new LinkedHashMap<String, String>();

        PrintWriter writer = null;
        try
        {
            writer = getBufferedPrintWriter(getOutputPath(outputFileBase + ".csv"));
            writer.println("type,code,description");
            writer.println("nothing,placeholder,this describes nothing");
            for (String tripPurpose : tripPurposes.keySet())
                writer.println("trip_purpose," + tripPurpose + "," + tripPurposes.get(tripPurpose));
            for (String mode : modes.keySet())
                writer.println("mode," + mode + "," + modes.get(mode));
            for (String ejCategory : ejCategories.keySet())
                writer.println("ej_category," + ejCategory + "," + ejCategories.get(ejCategory));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (writer != null) writer.close();
        }
    }

    private void exportPnrVehicleData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>(Arrays.asList("TAP"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("TAP"));
        exportDataGeneric(outputFileBase, "Results.PNRFile", false, null, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private void exportCbdVehicleData(String outputFileBase)
    {
        addTable(outputFileBase);
        Set<String> intColumns = new HashSet<String>(Arrays.asList("MGRA"));
        Set<String> floatColumns = new HashSet<String>();
        Set<String> stringColumns = new HashSet<String>();
        Set<String> bitColumns = new HashSet<String>();
        Set<String> primaryKey = new LinkedHashSet<String>(Arrays.asList("MGRA"));
        exportDataGeneric(outputFileBase, "Results.CBDFile", false, null, floatColumns,
                stringColumns, intColumns, bitColumns, FieldType.FLOAT, primaryKey, null);
    }

    private static enum FieldType
    {
        INT, FLOAT, STRING, BIT
    }

    private final class TripStructureDefinition
    {
        private final int     originMgraColumn;
        private final int     destMgraColumn;
        private final int     originPurposeColumn;
        private final int     destinationPurposeColumn;
        private final int     todColumn;
        private final int     modeColumn;
        private final int     boardTapColumn;
        private final int     alightTapColumn;

        private final String  homeName;
        private final String  destinationName;
        private final int     inboundColumn;
        private final boolean booleanIndicatorVariables;

        private TripStructureDefinition(int originMgraColumn, int destMgraColumn,
                int originPurposeColumn, int destinationPurposeColumn, int todColumn,
                int modeColumn, int boardTapColumn, int alightTapColumn, int partySizeColumn,
                int tripTimeColumn, int outVehicleTimeColumn, int tripDistanceColumn,
                int tripCostColumn, int tripPurposeNameColumn, int tripModeNameColumn,
                int recIdColumn, int boardTazColumn, int alightTazColumn, String tripType,
                String homeName, String destinationName, int inboundColumn,
                boolean booleanIndicatorVariables)
        {
            this.originMgraColumn = originMgraColumn;
            this.destMgraColumn = destMgraColumn;
            this.originPurposeColumn = originPurposeColumn;
            this.destinationPurposeColumn = destinationPurposeColumn;
            this.todColumn = todColumn;
            this.modeColumn = modeColumn;
            this.boardTapColumn = boardTapColumn;
            this.alightTapColumn = alightTapColumn;
            this.homeName = homeName;
            this.destinationName = destinationName;
            this.inboundColumn = inboundColumn;

            this.booleanIndicatorVariables = booleanIndicatorVariables;
        }

        private TripStructureDefinition(int originMgraColumn, int destMgraColumn,
                int originPurposeColumn, int destinationPurposeColumn, int todColumn,
                int modeColumn, int boardTapColumn, int alightTapColumn, int partySizeColumn,
                int columnCount, String tripType, int inboundColumn,
                boolean booleanIndicatorVariables)
        {
            this(originMgraColumn, destMgraColumn, originPurposeColumn, destinationPurposeColumn,
                    todColumn, modeColumn, boardTapColumn, alightTapColumn, partySizeColumn,
                    columnCount + 1, columnCount + 2, columnCount + 3, columnCount + 4,
                    columnCount + 5, columnCount + 6, columnCount + 7, columnCount + 8,
                    columnCount + 9, tripType, "", "", inboundColumn, booleanIndicatorVariables);
        }

        private TripStructureDefinition(int originMgraColumn, int destMgraColumn, int todColumn,
                int modeColumn, int boardTapColumn, int alightTapColumn, int partySizeColumn,
                int columnCount, String tripType, String homeName, String destinationName,
                int inboundColumn, boolean booleanIndicatorVariables)
        {
            this(originMgraColumn, destMgraColumn, -1, -1, todColumn, modeColumn, boardTapColumn,
                    alightTapColumn, partySizeColumn, columnCount + 1, columnCount + 2,
                    columnCount + 3, columnCount + 4, columnCount + 5, columnCount + 6,
                    columnCount + 7, columnCount + 8, columnCount + 9, tripType, homeName,
                    destinationName, inboundColumn, booleanIndicatorVariables);
        }
    }

    public static void main(String... args) throws Exception
    {
        Properties properties = new Properties();
        properties.load(new FileInputStream("conf/sandag_abm.properties"));

        int feedbackIteration = Integer.valueOf(properties.getProperty("Report.iteration").trim());

        List<String> definedTables = new ArrayList<String>();
        for (String table : properties.getProperty("Report.tables").trim().split(","))
            definedTables.add(table.trim().toLowerCase());

        String path = ClassLoader.getSystemResource("").getPath();
        path = path.substring(0, path.length() - 2);
        String appPath = path.substring(0, path.lastIndexOf("/"));

        for (Object key : properties.keySet())
        {
            String value = (String) properties.get(key);
            properties.setProperty((String) key,
                    value.replace(PROJECT_PATH_PROPERTY_TOKEN, appPath));
        }

        TranscadMatrixDao mtxDao = new TranscadMatrixDao(properties);

        DataExporter dataExporter = new DataExporter(properties, mtxDao, appPath, feedbackIteration);

        if (definedTables.contains("accessibilities"))
            dataExporter.exportAccessibilities("accessibilities");
        if (definedTables.contains("mgra")) dataExporter.exportMazData("mgra");
        if (definedTables.contains("taz")) dataExporter.exportTazData("taz");
        if (definedTables.contains("tap")) dataExporter.exportTapData("tap");
        if (definedTables.contains("mgratotap")) dataExporter.exportMgraToTapData("mgratotap");
        if (definedTables.contains("mgratomgra")) dataExporter.exportMgraToMgraData("mgratomgra");
        if (definedTables.contains("taztotap")) dataExporter.exportTazToTapData("taztotap");
        if (definedTables.contains("hhdata")) dataExporter.exportHouseholdData("hhdata");
        if (definedTables.contains("persondata")) dataExporter.exportPersonData("persondata");
        if (definedTables.contains("wslocation"))
            dataExporter.exportWorkSchoolLocation("wslocation");
        if (definedTables.contains("synhh")) dataExporter.exportSyntheticHouseholdData("synhh");
        if (definedTables.contains("synperson"))
            dataExporter.exportSyntheticPersonData("synperson");
        if (definedTables.contains("indivtours")) dataExporter.exportIndivToursData("indivtours");
        if (definedTables.contains("jointtours")) dataExporter.exportJointToursData("jointtours");
        if (definedTables.contains("indivtrips")) dataExporter.exportIndivTripData("indivtrips");
        if (definedTables.contains("jointtrips")) dataExporter.exportJointTripData("jointtrips");
        if (definedTables.contains("airporttrips"))
            dataExporter.exportAirportTrips("airporttrips");
        if (definedTables.contains("cbtours")) dataExporter.exportCrossBorderTourData("cbtours");
        if (definedTables.contains("cbtrips")) dataExporter.exportCrossBorderTripData("cbtrips");
        if (definedTables.contains("visitortours") && definedTables.contains("visitortrips"))
            dataExporter.exportVisitorData("visitortours", "visitortrips");
        if (definedTables.contains("ietrip"))
            dataExporter.exportInternalExternalTripData("ietrip");
        if (definedTables.contains("commtrip")) dataExporter.exportCommVehData("commtrip");
        if (definedTables.contains("trucktrip"))
        {

            IExporter truckExporter = new TruckCsvExporter(properties, mtxDao, "trucktrip");
            truckExporter.export();
        }
        if (definedTables.contains("eetrip"))
            dataExporter.exportExternalExternalTripData("eetrip");
        if (definedTables.contains("eitrip"))
            dataExporter.exportExternalInternalTripData("eitrip");
        if (definedTables.contains("tazskim")) dataExporter.exportAutoSkims("tazskim");
        if (definedTables.contains("tapskim")) dataExporter.exportTransitSkims("tapskim");
        if (definedTables.contains("definition")) dataExporter.exportDefinitions("definition");
        if (definedTables.contains("pnrvehicles"))
            dataExporter.exportPnrVehicleData("pnrvehicles");
        if (definedTables.contains("cbdvehicles"))
            dataExporter.exportCbdVehicleData("cbdvehicles");

    }

}