package org.sandag.abm.application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class SandagCreateTripGenerationFiles
{

    private static Logger         logger                                        = Logger.getLogger(SandagCreateTripGenerationFiles.class);

    private static final String   SANDAG_TRIP_GEN_FILE_KEY                      = "trip.model.trips.file";
    private static final String   ABM_TRIP_GEN_FILE_KEY                         = "output.trips.file";
    private static final String   ABM_INDIV_TRIP_FILE_KEY                       = "abm.individual.trip.file";
    private static final String   TAZ_TDZ_CORRESP_KEY                           = "taz.tdz.corresp.file";
    private static final String   SCALE_NHB_KEY                                 = "scale.nhb";

    private static final String   TAZ_COLUMN_HEADING                            = "taz";
    private static final String   TDZ_COLUMN_HEADING                            = "tdz";

    private static final String   TRIP_ORIG_PURPOSE_FIELD_NAME                  = "orig_purpose";
    private static final String   TRIP_DEST_PURPOSE_FIELD_NAME                  = "dest_purpose";
    private static final String   TRIP_ORIG_MGRA_FIELD_NAME                     = "orig_mgra";
    private static final String   TRIP_DEST_MGRA_FIELD_NAME                     = "dest_mgra";
    private static final String[] HH_HEADINGS                                   = {
            TRIP_ORIG_PURPOSE_FIELD_NAME, TRIP_DEST_PURPOSE_FIELD_NAME, TRIP_ORIG_MGRA_FIELD_NAME,
            TRIP_DEST_MGRA_FIELD_NAME                                           };

    private static final int      MAX_PURPOSE_INDEX                             = 10;

    private static final int      MIN_EXTERNAL_TDZ                              = 1;
    private static final int      MAX_EXTERNAL_TDZ                              = 12;

    private static final String   TAZ_FIELD_HEADING                             = "zone";

    private static final String[] TRIP_MODEL_HOME_BASED_ATTRACTION_HEADINGS     = {"a1", "a2",
            "a3", "a4", "a5", "a8"                                              };
    private static final String[] TRIP_MODEL_HOME_BASED_PRODUCTION_HEADINGS     = {"p1", "p2",
            "p3", "p4", "p5", "p8"                                              };
    private static final String[] TRIP_MODEL_NON_HOME_BASED_ATTRACTION_HEADINGS = {"a6", "a7"};
    private static final String[] TRIP_MODEL_NON_HOME_BASED_PRODUCTION_HEADINGS = {"p6", "p7"};
    private static final String[] TRIP_MODEL_OTHER_BASED_PRODUCTION_HEADINGS    = {"p9", "p10"};
    private static final String[] TRIP_MODEL_OTHER_BASED_ATTRACTION_HEADINGS    = {"a9", "a10"};

    private static final int[]    AB_MODEL_HOME_BASED_PRODUCTION_INDICES        = {1, 2, 3, 4, 5, 8};
    private static final int[]    AB_MODEL_NON_HOME_BASED_PRODUCTION_INDICES    = {6, 7};
    private static final int[]    AB_MODEL_OTHER_PRODUCTION_INDICES             = {9, 10};

    private float[][]             ieTrips;

    private static final String[] TABLE_HEADINGS                                = {"zone", "p1",
            "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9", "p10", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "a8", "a9", "a10"                                       };
    private static final String[] TABLE_HEADING_DESCRIPTIONS                    = {"zone",
            "home based work", "home based university", "home based school", "home based shop",
            "home based other", "non home based work related", "non home based other",
            "home based escort", "home based visitor", "home based airport", "home based work",
            "home based university", "home based school", "home based shop", "home based other",
            "non home based work related", "non home based other", "home based escort",
            "home based visitor", "home based airport"                          };

    private MgraDataManager       mgraManager;
    private int                   maxTdz;

    public SandagCreateTripGenerationFiles(HashMap<String, String> rbMap)
    {

        mgraManager = MgraDataManager.getInstance(rbMap);

    }

    public void createTripGenFile(HashMap<String, String> rbMap)
    {

        String tgInputFile = rbMap.get(SANDAG_TRIP_GEN_FILE_KEY);
        if (tgInputFile == null)
        {
            logger.error("Error getting the filename from the properties file for the input Sandag Trip Prods/Attrs by MDZ file.");
            logger.error("Properties file target: " + SANDAG_TRIP_GEN_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + SANDAG_TRIP_GEN_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        String tgOutputFile = rbMap.get(ABM_TRIP_GEN_FILE_KEY);
        if (tgOutputFile == null)
        {
            logger.error("Error getting the filename from the properties file to use for the new Trip Prods/Attrs by MDZ file created.");
            logger.error("Properties file target: " + ABM_TRIP_GEN_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + ABM_TRIP_GEN_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        String abmTripFile = rbMap.get(ABM_INDIV_TRIP_FILE_KEY);
        if (abmTripFile == null)
        {
            logger.error("Error getting the filename from the properties file to use for the ABM Model individual trips file.");
            logger.error("Properties file target: " + ABM_INDIV_TRIP_FILE_KEY + " not found.");
            logger.error("Please specify a filename value for the " + ABM_INDIV_TRIP_FILE_KEY
                    + " property.");
            throw new RuntimeException();
        }

        String correspFile = rbMap.get(TAZ_TDZ_CORRESP_KEY);
        if (correspFile == null)
        {
            logger.error("Error getting the filename from the properties file to use for the TAZ / TDZ correspondence file.");
            logger.error("Properties file target: " + TAZ_TDZ_CORRESP_KEY + " not found.");
            logger.error("Please specify a filename value for the " + TAZ_TDZ_CORRESP_KEY
                    + " property.");
            throw new RuntimeException();
        }

        // default is false
        boolean scaleNhbToAbm = false;
        String scaleNhbToAbmString = rbMap.get(SCALE_NHB_KEY);
        if (scaleNhbToAbmString != null && scaleNhbToAbmString.equalsIgnoreCase("true"))
            scaleNhbToAbm = true;
        logger.info("parameter to enable scaling NHB prods/attrs has a value of: " + scaleNhbToAbm);

        HashMap<Integer, Integer> tazTdzMap = createTazTdzMap(correspFile);

        TableDataSet inTgTds = readInputTripGenFile(tgInputFile);

        int[][] tdzTrips = readInputAbmIndivTripFile(abmTripFile, tazTdzMap);

        TableDataSet outAbmTds = produceAbmTripTableDataSet(inTgTds, tdzTrips, scaleNhbToAbm);

        writeAbmTripGenFile(tgOutputFile, outAbmTds);

        logger.info("");
        logger.info("");
        logger.info("finished producing new trip generation files from the ABM trip data.");
    }

    private TableDataSet readInputTripGenFile(String fileName)
    {

        TableDataSet inTgTds = null;

        try
        {
            logger.info("");
            logger.info("");
            logger.info("reading input trip generation file.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            inTgTds = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading input trip generation data file: %s into TableDataSet object.",
                            fileName));
            throw new RuntimeException(e);
        }

        // create TDZ by purpose arrays for IE Prods and IE attrs from the trip
        // model
        // these will be added into the home-based AB model prods and attrs
        // use the first dimension 0 element to accumulate totals by purpose for
        // logging
        ieTrips = new float[maxTdz + 1][2 * MAX_PURPOSE_INDEX + 1];
        for (int i = 0; i < inTgTds.getRowCount(); i++)
        {
            int tdz = (int) inTgTds.getValueAt(i + 1, TAZ_FIELD_HEADING);
            for (int j = 1; j < inTgTds.getColumnCount(); j++)
            {
                if (tdz >= MIN_EXTERNAL_TDZ && tdz <= MAX_EXTERNAL_TDZ)
                {
                    ieTrips[i + 1][j] = inTgTds.getValueAt(i + 1, j + 1);
                    ieTrips[0][j] += inTgTds.getValueAt(i + 1, j + 1);
                }
            }
        }

        // log column totals
        logger.info("");
        logger.info("");
        logger.info("\t" + inTgTds.getRowCount() + " rows in input file.");
        logger.info("\t" + inTgTds.getColumnCount() + " columns in input file.");
        logger.info("");
        logger.info(String.format("\t%-15s %-30s %15s %15s", "Column Name", "Column Purpose",
                "Column Total", "Int-Ext"));

        String[] headings = inTgTds.getColumnLabels();
        logger.info(String.format("\t%-15s %-30s %15s %15s", headings[0], "N/A", "N/A", "N/A"));
        float totProd = 0;
        float totAttr = 0;
        float totalIeProds = 0;
        float totalIeAttrs = 0;
        float columnSum = 0;
        for (int i = 1; i < inTgTds.getColumnCount(); i++)
        {

            columnSum = inTgTds.getColumnTotal(i + 1);

            // 1st 10 fields after zone are production fields, next 10 are
            // attraction
            // fields
            if (i <= 10)
            {
                totProd += columnSum;
                totalIeProds += ieTrips[0][i];
            } else
            {
                totAttr += columnSum;
                totalIeAttrs += ieTrips[0][i];
            }

            logger.info(String.format("\t%-15s %-30s %15.1f %15.1f", headings[i],
                    TABLE_HEADING_DESCRIPTIONS[i], columnSum, ieTrips[0][i]));

        }

        logger.info("");
        logger.info("");
        logger.info(String.format("\ttotal productions = %15.1f", totProd));
        logger.info(String.format("\ttotal attractions = %15.1f", totAttr));
        logger.info(String.format("\ttotal IE productions = %12.1f", totalIeProds));
        logger.info(String.format("\ttotal IE attractions = %12.1f", totalIeAttrs));
        logger.info("");

        return inTgTds;
    }

    private HashMap<Integer, Integer> createTazTdzMap(String correspFile)
    {

        TableDataSet tazTdzTds = null;

        try
        {
            logger.info("");
            logger.info("");
            logger.info("reading input taz-tdz correspondence file.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            tazTdzTds = reader.readFile(new File(correspFile));
        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading input taz-tdz correspondence file: %s into TableDataSet object.",
                            correspFile));
            throw new RuntimeException(e);
        }

        maxTdz = 0;
        HashMap<Integer, Integer> tazTdzMap = new HashMap<Integer, Integer>();
        for (int r = 1; r <= tazTdzTds.getRowCount(); r++)
        {
            int taz = (int) tazTdzTds.getValueAt(r, TAZ_COLUMN_HEADING);
            int tdz = (int) tazTdzTds.getValueAt(r, TDZ_COLUMN_HEADING);
            tazTdzMap.put(taz, tdz);

            if (tdz > maxTdz) maxTdz = tdz;
        }

        // a trip record with origin or destination mgra=0 or mgra=-1 (location
        // not
        // determined) should map to tdz=0
        // if a trip record mgra is not greater than 0, its taz will be 0 - then
        // the
        // following entry will map it to tdz=0.
        tazTdzMap.put(0, 0);

        return tazTdzMap;

    }

    private int[][] readInputAbmIndivTripFile(String fileName, HashMap<Integer, Integer> tazTdzMap)
    {

        String origPurpose = "";
        String destPurpose = "";
        int origMgra = 0;
        int destMgra = 0;
        int homeTdz = 0;
        int tripPurposeIndex = 0;

        // open the file for reading
        String delimSet = ",\t\n\r\f\"";
        BufferedReader inputStream = null;
        try
        {
            inputStream = new BufferedReader(new FileReader(new File(fileName)));
        } catch (FileNotFoundException e)
        {
            logger.fatal(String.format("Exception occurred reading input abm indiv trip file: %s.",
                    fileName));
            throw new RuntimeException(e);
        }

        // first parse the trip file field names from the first record and
        // associate
        // column position with fields specified to be read
        HashMap<Integer, String> columnIndexHeadingMap = new HashMap<Integer, String>();
        String line = "";
        try
        {
            line = inputStream.readLine();
        } catch (IOException e)
        {
            logger.fatal(String.format(
                    "Exception occurred reading header record of input abm indiv trip file: %s.",
                    fileName));
            logger.fatal(String.format("line = %s.", line));
            throw new RuntimeException(e);
        }
        StringTokenizer st = new StringTokenizer(line, delimSet);
        int col = 0;
        while (st.hasMoreTokens())
        {
            String label = st.nextToken();
            for (String heading : HH_HEADINGS)
            {
                if (heading.equalsIgnoreCase(label))
                {
                    columnIndexHeadingMap.put(col, heading);
                    break;
                }
            }
            col++;
        }

        // dimension the array to hold trips summarized by tdz and trip model
        // purpose
        int[][] abmTdzTrips = new int[maxTdz + 1][MAX_PURPOSE_INDEX + 1];
        int[] abmTdzTotalTrips = new int[MAX_PURPOSE_INDEX + 1];

        // read the trip records from the file
        int lineCount = 0;
        try
        {

            while ((line = inputStream.readLine()) != null)
            {

                lineCount++;

                // get the values for the fields specified.
                col = 0;
                st = new StringTokenizer(line, delimSet);
                while (st.hasMoreTokens())
                {
                    String fieldValue = st.nextToken();
                    if (columnIndexHeadingMap.containsKey(col))
                    {
                        String fieldName = columnIndexHeadingMap.get(col++);

                        if (fieldName.equalsIgnoreCase(TRIP_ORIG_PURPOSE_FIELD_NAME))
                        {
                            origPurpose = fieldValue;
                        } else if (fieldName.equalsIgnoreCase(TRIP_DEST_PURPOSE_FIELD_NAME))
                        {
                            destPurpose = fieldValue;
                        } else if (fieldName.equalsIgnoreCase(TRIP_ORIG_MGRA_FIELD_NAME))
                        {
                            origMgra = Integer.parseInt(fieldValue);
                        } else if (fieldName.equalsIgnoreCase(TRIP_DEST_MGRA_FIELD_NAME))
                        {
                            destMgra = Integer.parseInt(fieldValue);

                            // don't need to process any more fields
                            break;

                        }

                    } else
                    {
                        col++;
                    }

                }

                int homeTaz = 0;
                try
                {
                    if (origPurpose.equalsIgnoreCase("Home") && origMgra > 0) homeTaz = mgraManager
                            .getTaz(origMgra);
                    else if (destPurpose.equalsIgnoreCase("Home") && destMgra > 0)
                        homeTaz = mgraManager.getTaz(destMgra);
                } catch (Exception e)
                {
                    logger.error("error getting home taz from mgraManager for origPurpose = "
                            + origPurpose + ", origMgra = " + origMgra + ", destPurpose = "
                            + destPurpose + ", destMgra = " + destMgra + ", lineCount = "
                            + lineCount);
                    throw new RuntimeException(e);
                }

                try
                {
                    homeTdz = tazTdzMap.get(homeTaz);
                } catch (Exception e)
                {
                    logger.error("error getting home tdz from tazTdzMap for homeTaz = " + homeTaz
                            + ", lineCount = " + lineCount);
                    throw new RuntimeException(e);
                }

                try
                {
                    // get the trip based model purpose index for this abm model
                    // trip
                    tripPurposeIndex = getTripModelPurposeForAbmTrip(origPurpose, destPurpose);
                } catch (Exception e)
                {
                    logger.error("error getting tripPurposeIndex for origPurpose = " + origPurpose
                            + ", destPurpose = " + destPurpose + ", lineCount = " + lineCount);
                    throw new RuntimeException(e);
                }

                // accumulate trips in table
                if (tripPurposeIndex >= 1 && tripPurposeIndex <= 5 || tripPurposeIndex == 8)
                {
                    if (homeTdz > 0) abmTdzTrips[homeTdz][tripPurposeIndex]++;
                    else
                    {
                        logger.error("home tdz is le 0 for home-based trip.");
                        throw new RuntimeException();
                    }
                } else
                {
                    if (homeTdz > 0)
                    {
                        logger.error("home tdz is gt 0 for non-home-based trip.");
                        throw new RuntimeException();
                    }
                    abmTdzTrips[homeTdz][tripPurposeIndex]++;
                }
                abmTdzTotalTrips[tripPurposeIndex]++;

            }

        } catch (NumberFormatException e)
        {
            logger.fatal(String
                    .format("NumberFormatException occurred reading record of input abm indiv trip file: %s.",
                            fileName));
            logger.fatal(String.format("last record number read = %d.", lineCount));
        } catch (IOException e)
        {
            logger.fatal(String.format(
                    "IOException occurred reading record of input abm indiv trip file: %s.",
                    fileName));
            logger.fatal(String.format("last record number read = %d.", lineCount));
        }

        logger.info(lineCount + " trip records read from " + fileName);

        // log a summary report of trips by trip model purpose
        logger.info("");
        logger.info("");
        logger.info("ABM Trip file trips by TM purpose");
        logger.info(String.format("\t%-15s %-30s %15s", "Column Name", "Column Purpose",
                "Column Total"));
        String[] headings = {"", "hbw", "hbu", "hbc", "hbs", "hbo", "nhw", "nho", "hbp"};
        int total = 0;
        for (int i = 1; i < headings.length; i++)
        {
            logger.info(String.format("\t%-15s %-30s %15d", headings[i],
                    TABLE_HEADING_DESCRIPTIONS[i], abmTdzTotalTrips[i]));
            total += abmTdzTotalTrips[i];
        }
        logger.info(String.format("\t%-15s %-30s %15d", "Total", "", total));

        return abmTdzTrips;

    }

    private int getTripModelPurposeForAbmTrip(String origPurpose, String destPurpose)
    {

        /*
         * assignment rules: replace tpurp4s = 1 if orig_purpose=="Home" &
         * dest_purpose=="Work"; replace tpurp4s = 1 if orig_purpose=="Work" &
         * dest_purpose=="Home"; replace tpurp4s = 2 if orig_purpose=="Home" &
         * dest_purpose=="University"; replace tpurp4s = 2 if
         * orig_purpose=="University" & dest_purpose=="Home"; replace tpurp4s =
         * 3 if orig_purpose=="Home" & dest_purpose=="School"; replace tpurp4s =
         * 3 if orig_purpose=="School" & dest_purpose=="Home"; replace tpurp4s =
         * 4 if orig_purpose=="Home" & dest_purpose=="Shop"; replace tpurp4s = 4
         * if orig_purpose=="Shop" & dest_purpose=="Home"; replace tpurp4s = 5
         * if orig_purpose=="Home" & (dest_purpose=="Maintenance" |
         * dest_purpose=="Eating Out" | dest_purpose=="Visiting" |
         * dest_purpose=="Discretionary"); replace tpurp4s = 5 if
         * dest_purpose=="Home" & (orig_purpose=="Maintenance" |
         * orig_purpose=="Eating Out" | orig_purpose=="Visiting" |
         * orig_purpose=="Discretionary"); replace tpurp4s = 8 if
         * orig_purpose=="Home" & dest_purpose=="Escort"; replace tpurp4s = 8 if
         * orig_purpose=="Escort" & dest_purpose=="Home"; replace tpurp4s = 6 if
         * orig_purpose=="Work" & dest_purpose!="Home"; replace tpurp4s = 6 if
         * orig_purpose!="Home" & dest_purpose=="Work"; replace tpurp4s = 6 if
         * orig_purpose=="Work-Based" | dest_purpose=="Work-Based"; replace
         * tpurp4s = 7 if tpurp4s==0;
         */

        int tripPurposeIndex = 0;
        if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("Work")) tripPurposeIndex = 1;
        else if (origPurpose.equalsIgnoreCase("Work") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 1;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("University")) tripPurposeIndex = 2;
        else if (origPurpose.equalsIgnoreCase("University") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 2;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("School")) tripPurposeIndex = 3;
        else if (origPurpose.equalsIgnoreCase("School") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 3;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("Shop")) tripPurposeIndex = 4;
        else if (origPurpose.equalsIgnoreCase("Shop") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 4;
        else if (origPurpose.equalsIgnoreCase("Home")
                && destPurpose.equalsIgnoreCase("Maintenance")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Maintenance")
                && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("Eating Out")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Eating Out") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("Visiting")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Visiting") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Home")
                && destPurpose.equalsIgnoreCase("Discretionary")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Discretionary")
                && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Home")
                && destPurpose.equalsIgnoreCase("Work Related")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Work Related")
                && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 5;
        else if (origPurpose.equalsIgnoreCase("Home") && destPurpose.equalsIgnoreCase("Escort")) tripPurposeIndex = 8;
        else if (origPurpose.equalsIgnoreCase("Escort") && destPurpose.equalsIgnoreCase("Home")) tripPurposeIndex = 8;
        else if (origPurpose.equalsIgnoreCase("Work") && (!destPurpose.equalsIgnoreCase("Home"))) tripPurposeIndex = 6;
        else if ((!destPurpose.equalsIgnoreCase("Home")) && destPurpose.equalsIgnoreCase("Work")) tripPurposeIndex = 6;
        else if (origPurpose.equalsIgnoreCase("Work-Based")
                || destPurpose.equalsIgnoreCase("Work-Based")) tripPurposeIndex = 6;
        else tripPurposeIndex = 7;

        return tripPurposeIndex;

    }

    private TableDataSet produceAbmTripTableDataSet(TableDataSet inTgTds, int[][] tdzTrips,
            boolean scaleNhbToAbm)
    {

        float[][] newTrips = new float[maxTdz][2 * MAX_PURPOSE_INDEX + 1];

        saveAbmHbProdsAndScaleTmAttrs(inTgTds, tdzTrips, newTrips);

        saveTmNhbProdsAsAbmProds(inTgTds, tdzTrips, scaleNhbToAbm, newTrips);

        saveTmNhbAttrsAsAbmProds(inTgTds, tdzTrips, scaleNhbToAbm, newTrips);

        addTmIeAttrsToAbmHbProds(inTgTds, tdzTrips, newTrips);

        addTmIeProdsToAbmHbAttrs(inTgTds, tdzTrips, newTrips);

        addTmAirportAndVisitorProdsToAbmProds(inTgTds, tdzTrips, newTrips);

        addTmAirportAndVisitorAttrsToAbmAttrs(inTgTds, tdzTrips, newTrips);

        saveZoneField(inTgTds, newTrips);

        TableDataSet abmTds = createFinalTableDataset(newTrips);

        return abmTds;
    }

    private void saveAbmHbProdsAndScaleTmAttrs(TableDataSet inTgTds, int[][] tdzTrips,
            float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("transferring ABM home-based productions and scaling TM attractions:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s   %15s   %15s", "TM Heading",
                "TM Purpose", "TM Attrs", "ABM Attrs", "Scale Factor", "New ABM Prods",
                "New ABM Attrs"));
        int index = 0;
        for (String heading : TRIP_MODEL_HOME_BASED_ATTRACTION_HEADINGS)
        {

            // get the trip model attractions, the TableDataSet returns a 0s
            // based
            // array
            float[] values = inTgTds.getColumnAsFloat(heading);

            // add up the total TM attractions for this purpose
            float total = 0;
            for (int i = 0; i < values.length; i++)
                total += values[i];

            // add up the total ABM productions for this purpose
            int abTotal = 0;
            int abProdIndex = AB_MODEL_HOME_BASED_PRODUCTION_INDICES[index];
            for (int i = 1; i < tdzTrips.length; i++)
                abTotal += tdzTrips[i][abProdIndex];

            // get the scale factor to scale trip model atractions to ABM
            // productions
            // by purpose
            double scaleFactor = 0.0;
            if (total > 0)
            {
                scaleFactor = abTotal / total;
            } else
            {
                logger.error("attempting to scale an array which sums to 0.0.");
                throw new RuntimeException();
            }

            // get the scaled attractions for the purpose
            double[] scaledAttrs = getScaledValues(values, scaleFactor);

            // determine the final array column index into which to store the
            // scaled
            // attractions
            int abAttrIndex = abProdIndex + MAX_PURPOSE_INDEX;

            // save the scaled attractions in the final array
            float abmAttrs = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][abAttrIndex] = (float) scaledAttrs[i];
                abmAttrs += newTrips[i][abAttrIndex];
            }

            // save the ABM productions in the final array
            float abmProds = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][abProdIndex] = tdzTrips[i + 1][abProdIndex];
                abmProds += newTrips[i][abProdIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15d   %15.6f   %15.1f   %15.1f",
                    heading, TABLE_HEADING_DESCRIPTIONS[abAttrIndex], total, abTotal, scaleFactor,
                    abmProds, abmAttrs));

            index++;

        }
    }

    private void saveTmNhbProdsAsAbmProds(TableDataSet inTgTds, int[][] tdzTrips,
            boolean scaleNhbToAbm, float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("non-home-based TM productions to total ABM productions:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s   %15s", "TM Heading",
                "TM Purpose", "TM Prods", "ABM Prods", "Scale Factor", "New ABM Prods"));
        int index = 0;
        for (String heading : TRIP_MODEL_NON_HOME_BASED_PRODUCTION_HEADINGS)
        {

            // get the trip model nhb productions, the TableDataSet returns a 0s
            // based array
            float[] values = inTgTds.getColumnAsFloat(heading);

            // add up total TM productions by purpose
            float total = 0;
            for (int i = 0; i < values.length; i++)
                total += values[i];

            // get the total ab model productions for this purpose
            int abProdIndex = AB_MODEL_NON_HOME_BASED_PRODUCTION_INDICES[index];
            int abTotal = tdzTrips[0][abProdIndex];

            // get the scale factor to scale trip model productions to ABM
            // productions by purpose
            double scaleFactor = 0.0;
            if (scaleNhbToAbm)
            {
                if (total > 0)
                {
                    scaleFactor = abTotal / total;
                } else
                {
                    logger.error("attempting to scale an array which sums to 0.0.");
                    throw new RuntimeException();
                }
            } else
            {
                scaleFactor = 1.0;
            }

            // get the scaled productions for the purpose
            double[] scaledProds = getScaledValues(values, scaleFactor);

            // save the scaled attractions in the final array
            float abmProds = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][abProdIndex] = (float) scaledProds[i];
                abmProds += newTrips[i][abProdIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15d   %15.6f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[abProdIndex], total, abTotal, scaleFactor, abmProds));

            index++;

        }
    }

    private void saveTmNhbAttrsAsAbmProds(TableDataSet inTgTds, int[][] tdzTrips,
            boolean scaleNhbToAbm, float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("non-home-based TM attractions to total ABM productions:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s   %15s", "TM Heading",
                "TM Purpose", "TM Attrs", "ABM Prods", "Scale Factor", "New ABM Attrs"));
        int index = 0;
        for (String heading : TRIP_MODEL_NON_HOME_BASED_ATTRACTION_HEADINGS)
        {

            // get the trip model nhb attractions, the TableDataSet returns a 0s
            // based array
            float[] values = inTgTds.getColumnAsFloat(heading);

            // add up total TM attracctions by purpose
            float total = 0;
            for (int i = 0; i < values.length; i++)
                total += values[i];

            // get the total ab model productions for this purpose
            int abProdIndex = AB_MODEL_NON_HOME_BASED_PRODUCTION_INDICES[index];
            int abTotal = tdzTrips[0][abProdIndex];

            // get the scale factor to scale trip model atractions to ABM
            // productions
            // by purpose
            double scaleFactor = 0.0;
            if (scaleNhbToAbm)
            {
                if (total > 0)
                {
                    scaleFactor = abTotal / total;
                } else
                {
                    logger.error("attempting to scale an array which sums to 0.0.");
                    throw new RuntimeException();
                }
            } else
            {
                scaleFactor = 1.0;
            }

            // get the scaled attractions for the purpose
            double[] scaledAttrs = getScaledValues(values, scaleFactor);

            // determine the final array column index into which to store the
            // scaled
            // attractions
            int abAttrIndex = abProdIndex + MAX_PURPOSE_INDEX;

            // save the scaled attractions in the final array
            float abmAttrs = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][abAttrIndex] = (float) scaledAttrs[i];
                abmAttrs += newTrips[i][abAttrIndex];
            }

            index++;

            logger.info(String.format("%10s   %-30s   %15.1f   %15d   %15.6f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[abAttrIndex], total, abTotal, scaleFactor, abmAttrs));

        }
    }

    private void addTmIeAttrsToAbmHbProds(TableDataSet inTgTds, int[][] tdzTrips, float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("adding IE attrs from trip model to home-based ABM prods:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s", "TM Heading", "TM Purpose",
                "ABM Prods", "IE attrs", "new ABM Prods"));
        int index = 0;
        for (String heading : TRIP_MODEL_HOME_BASED_PRODUCTION_HEADINGS)
        {

            int abProdIndex = AB_MODEL_HOME_BASED_PRODUCTION_INDICES[index];
            int abAttrIndex = abProdIndex + MAX_PURPOSE_INDEX;

            // save the new ABM productions in the final array
            float abTotal = 0;
            float newTotal = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                abTotal += newTrips[i][abProdIndex];
                newTrips[i][abProdIndex] += ieTrips[i + 1][abAttrIndex];
                newTotal += newTrips[i][abProdIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15.1f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[abProdIndex], abTotal, ieTrips[0][abAttrIndex],
                    newTotal));

            index++;

        }
    }

    private void addTmIeProdsToAbmHbAttrs(TableDataSet inTgTds, int[][] tdzTrips, float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("adding IE prods from trip model to home-based ABM attrs:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s", "TM Heading", "TM Purpose",
                "ABM Attrs", "IE prods", "new ABM Attrs"));
        int index = 0;
        for (String heading : TRIP_MODEL_HOME_BASED_ATTRACTION_HEADINGS)
        {

            int abProdIndex = AB_MODEL_HOME_BASED_PRODUCTION_INDICES[index];
            int abAttrIndex = AB_MODEL_HOME_BASED_PRODUCTION_INDICES[index] + MAX_PURPOSE_INDEX;

            // save the new ABM attractions in the final array
            float abTotal = 0;
            float newTotal = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                abTotal += newTrips[i][abAttrIndex];
                newTrips[i][abAttrIndex] += ieTrips[i + 1][abProdIndex];
                newTotal += newTrips[i][abAttrIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15.1f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[abAttrIndex], abTotal, ieTrips[0][abProdIndex],
                    newTotal));

            index++;

        }
    }

    private void addTmAirportAndVisitorProdsToAbmProds(TableDataSet inTgTds, int[][] tdzTrips,
            float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("airport and visitor TM to ABM productions:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s", "TM Heading", "TM Purpose",
                "TM Prods", "Scale Factor", "New ABM Prods"));
        int index = 0;
        for (String heading : TRIP_MODEL_OTHER_BASED_PRODUCTION_HEADINGS)
        {

            // get the trip model productions
            float[] pValues = inTgTds.getColumnAsFloat(heading);

            // determine the final array column index into which to store the
            // scaled
            // attractions
            int prodIndex = AB_MODEL_OTHER_PRODUCTION_INDICES[index];

            // save the productions in the final array
            // add up the original values
            float pTotal = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][prodIndex] = pValues[i];
                pTotal += newTrips[i][prodIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15.6f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[prodIndex], pTotal, 1.0, pTotal));

            index++;

        }
    }

    private void addTmAirportAndVisitorAttrsToAbmAttrs(TableDataSet inTgTds, int[][] tdzTrips,
            float[][] newTrips)
    {
        logger.info("");
        logger.info("");
        logger.info("airport and visitor TM to ABM attractions:");
        logger.info(String.format("%10s   %-30s   %15s   %15s   %15s", "TM Heading", "TM Purpose",
                "TM Attrs", "Scale Factor", "New ABM Attrs"));
        int index = 0;
        for (String heading : TRIP_MODEL_OTHER_BASED_ATTRACTION_HEADINGS)
        {

            // get the trip model attractions
            float[] aValues = inTgTds.getColumnAsFloat(heading);

            // determine the final array column index into which to store the
            // scaled
            // attractions
            int attrIndex = AB_MODEL_OTHER_PRODUCTION_INDICES[index] + MAX_PURPOSE_INDEX;

            // save the attractions in the final array
            float aTotal = 0;
            for (int i = 0; i < newTrips.length; i++)
            {
                newTrips[i][attrIndex] = aValues[i];
                aTotal += newTrips[i][attrIndex];
            }

            logger.info(String.format("%10s   %-30s   %15.1f   %15.6f   %15.1f", heading,
                    TABLE_HEADING_DESCRIPTIONS[attrIndex], aTotal, 1.0, aTotal));

            index++;

        }
    }

    private void saveZoneField(TableDataSet inTgTds, float[][] newTrips)
    {
        // save the zone field in final table
        float[] values = inTgTds.getColumnAsFloat(TAZ_FIELD_HEADING);
        int tazFieldIndex = inTgTds.getColumnPosition(TAZ_FIELD_HEADING) - 1;
        for (int i = 0; i < newTrips.length; i++)
            newTrips[i][tazFieldIndex] = values[i];
    }

    private TableDataSet createFinalTableDataset(float[][] newTrips)
    {
        TableDataSet abmTds = TableDataSet.create(newTrips, TABLE_HEADINGS);

        float[] newIeTrips = new float[2 * MAX_PURPOSE_INDEX + 1];
        for (int i = 0; i < abmTds.getRowCount(); i++)
        {
            int tdz = (int) abmTds.getValueAt(i + 1, TAZ_FIELD_HEADING);
            for (int j = 1; j < abmTds.getColumnCount(); j++)
            {
                if (tdz >= MIN_EXTERNAL_TDZ && tdz <= MAX_EXTERNAL_TDZ)
                {
                    newIeTrips[j] += abmTds.getValueAt(i + 1, j + 1);
                }
            }
        }

        logger.info("");
        logger.info("");
        logger.info("summary of newly created trip generation file.");
        logger.info("\t" + abmTds.getRowCount() + " rows in output file.");
        logger.info("\t" + abmTds.getColumnCount() + " columns in output file.");
        logger.info("");
        logger.info(String.format("\t%-15s %-30s %15s %15s", "Column Name", "Column Purpose",
                "Column Total", "Int-Ext"));

        String[] headings = abmTds.getColumnLabels();
        logger.info(String.format("\t%-15s %-30s %15s %15s", headings[0], "N/A", "N/A", "N/A"));
        float totProd = 0;
        float totAttr = 0;
        float totalIeProds = 0;
        float totalIeAttrs = 0;
        float columnSum = 0;
        for (int i = 1; i < abmTds.getColumnCount(); i++)
        {

            columnSum = abmTds.getColumnTotal(i + 1);

            // 1st 10 fields after zone are production fields, next 10 are
            // attraction
            // fields
            if (i <= 10)
            {
                totProd += columnSum;
                totalIeProds += newIeTrips[i];
            } else
            {
                totAttr += columnSum;
                totalIeAttrs += newIeTrips[i];
            }

            logger.info(String.format("\t%-15s %-30s %15.1f %15.1f", headings[i],
                    TABLE_HEADING_DESCRIPTIONS[i], columnSum, newIeTrips[i]));

        }

        logger.info("");
        logger.info("");
        logger.info(String.format("\ttotal productions = %15.1f", totProd));
        logger.info(String.format("\ttotal attractions = %15.1f", totAttr));
        logger.info(String.format("\ttotal IE productions = %12.1f", totalIeProds));
        logger.info(String.format("\ttotal IE attractions = %12.1f", totalIeAttrs));
        logger.info("");

        return abmTds;
    }

    private void writeAbmTripGenFile(String tgOutputFile, TableDataSet outAbmTds)
    {

        CSVFileWriter writer = new CSVFileWriter();
        try
        {
            writer.writeFile(outAbmTds, new File(tgOutputFile));
        } catch (IOException e)
        {
            logger.fatal(String
                    .format("Exception occurred writing new trip generation data file = %s from TableDataSet object.",
                            tgOutputFile));
            throw new RuntimeException(e);
        }
    }

    private double[] getScaledValues(float[] values, double scaleFactor)
    {

        double[] scaledValues = new double[values.length];
        for (int i = 0; i < values.length; i++)
            scaledValues[i] = values[i] * scaleFactor;

        return scaledValues;
    }

    public static void main(String[] args) throws Exception
    {

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else
        {

            String baseName;
            if (args[0].endsWith(".properties"))
            {
                int index = args[0].indexOf(".properties");
                baseName = args[0].substring(0, index);
            } else
            {
                baseName = args[0];
            }

            ResourceBundle rb = ResourceBundle.getBundle(baseName);
            HashMap<String, String> rbMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

            SandagCreateTripGenerationFiles mainObject = new SandagCreateTripGenerationFiles(rbMap);

            // pass true as an argument if NHB trips from the trip model are to
            // be
            // scaled to the number from the activity-based model
            mainObject.createTripGenFile(rbMap);

        }

    }

}
