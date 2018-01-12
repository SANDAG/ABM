package org.sandag.htm.processFAF;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.datafile.CSVFileReader;
import com.pb.common.util.ResourceUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;

/**
 * Utilities to process FAF2 data
 * User: Rolf Moeckel
 * Date: May 6, 2009
 */
public class fafUtils {

    private static  Logger logger = Logger.getLogger(fafUtils.class);
    private static ResourceBundle rb;
    private static TableDataSet payloadSTCC;
    private static TableDataSet payloadSCTG;

    public static TableDataSet importTable(String filePath) {
        // read a csv file into a TableDataSet
        TableDataSet tblData;
        CSVFileReader cfrReader = new CSVFileReader();
        try {
            tblData = cfrReader.readFile(new File( filePath ));
        } catch (Exception e) {
            throw new RuntimeException("File not found: <" + filePath + ">.", e);
        }
        cfrReader.close();
        return tblData;
    }


    public static ResourceBundle getResourceBundle(String pathToRb) {
        File propFile = new File(pathToRb);
        rb = ResourceUtil.getPropertyBundle(propFile);
        if (rb == null) logger.fatal ("Problem loading resource bundle: " + pathToRb);
        return rb;
    }


    public static void setResourceBundle (ResourceBundle appRb) {
        rb = appRb;
    }

    
    public static PrintWriter openFileForSequentialWriting(String fileName) {
        File outputFile = new File(fileName);
        FileWriter fw = null;
        try {
            fw = new FileWriter(outputFile);
        } catch (IOException e) {
            logger.error("Could not open file <" + fileName + ">.");
        }
        BufferedWriter bw = new BufferedWriter(fw);
        return new PrintWriter(bw);
    }


    public static HashMap<String, Float> createScalerHashMap (String[] tokens, double[] values) {
        // create HashMap with state O-D pairs that need to be scaled

        HashMap<String, Float> scaler = new HashMap<>();
        if (tokens.length != values.length) {
        	throw new RuntimeException("Error.  scaling.truck.trips.tokens must be same length as scaling.truck.trips.values");
        }
        for (int i=0; i<tokens.length; i++) {
        	scaler.put(tokens[i], (float) values[i]);
        }
        return scaler;
    }


    public static HashMap<String, Float> createScalerHashMap (TableDataSet scaleTable, String[] columnNames) {
        // create HashMap with state O-D pairs that need to be scaled

        HashMap<String, Float> scaler = new HashMap<>();
        for (int row = 1; row < scaleTable.getRowCount(); row++) {
            String token = String.valueOf((int) scaleTable.getValueAt(row, columnNames[0])) + "_" +
                    String.valueOf((int) scaleTable.getValueAt(row, columnNames[1]));
            scaler.put(token, scaleTable.getValueAt(row, columnNames[2]));
        }
        return scaler;
    }


    public static int isOnPosition (String txt, String[] txtArray) {
        // checks if txt is a value of txtArray
        int position = -1;
        for (int i = 0; i < txtArray.length; i++) if (txtArray[i].equals(txt)) position = i;
        return position;
    }


    public static int[] createCountyFipsArray (int[] specRegCodes) {
        // create array with county FIPS codes including special regions (such as airports)

        // Note: Used to readFAF2 employment from here, but should be done separately for each project
//        disaggregateFlows.getUScountyEmploymentFromOneFile(appRb);
        int[] countyFipsS = disaggregateFlows.countyIDsWithEmployment.getColumnAsInt(
                disaggregateFlows.countyIDsWithEmployment.getColumnPosition("COUNTYFIPS"));
        int[] countyFips = new int[countyFipsS.length + specRegCodes.length];
        System.arraycopy(countyFipsS, 0, countyFips, 0, countyFipsS.length);
        System.arraycopy(specRegCodes, 0, countyFips, countyFipsS.length, specRegCodes.length);
        return countyFips;
    }


    public static float findAveragePayload(String comm, String comClass) {
        // returns average payload in tons per truck for commodity comm

        if (comm.equals("SCTG99")) comm = "SCTG42";   // FAF3 calls unknown SCTG99 instead of SCTG42
        TableDataSet payload = new TableDataSet();
        if (comClass.equals("STCC")) {
            if (payloadSTCC == null) payloadSTCC = importTable(ResourceUtil.getProperty(rb, "truck.commodity.payload"));
            payload = payloadSTCC;
        } else if (comClass.equals("SCTG")) {
            if (payloadSCTG == null) payloadSCTG = importTable(ResourceUtil.getProperty(rb, "truck.SCTG.commodity.payload"));
            payload = payloadSCTG;
        }
        int n = -1;
        for (int k = 1; k <= payload.getRowCount(); k++) {
            if (payload.getStringValueAt(k, "Commodity").equals(comm)) n = k;
        }
        if (n == -1) {
            logger.fatal("Commodity " + comm + " not found in payload factor file.");
            System.exit(1);
        }
        // weight of each truck type, using an average for all commodities, derived from fhwa website
        if (payload.containsColumn("Single Unit Trucks")) {
            float[] weights = new float[] {0.307f, 0.155f, 0.269f, 0.269f};   // including pickups, minivans, other light vans, SUVs: 0.93642251f,0.022471435f,0.010687433f,0.030418621f
            return (payload.getValueAt(n, "Single Unit Trucks") * weights[0]) +
                    (payload.getValueAt(n, "Semi Trailer") * weights[1]) +
                    (payload.getValueAt(n, "Double Trailers") * weights[2]) +
                    (payload.getValueAt(n, "Triples") * weights[3]);
        } else {
            return (payload.getValueAt(n, "Payload (lbs)") * (float) 0.0005);
        }
    }


    public static void readPayloadFactors (ResourceBundle appRb) {
        payloadSCTG = importTable(ResourceUtil.getProperty(appRb, "truck.SCTG.commodity.payload"));
    }


    public static float findAveragePayload(String comm) {
        // returns average payload in tons per truck for commodity comm

        if (comm.equals("SCTG99")) comm = "SCTG42";   // FAF3 calls unknown SCTG99 instead of SCTG42
        int n = -1;
        for (int k = 1; k <= payloadSCTG.getRowCount(); k++) {
            if (payloadSCTG.getStringValueAt(k, "Commodity").equals(comm)) n = k;
        }
        if (n == -1) {
            logger.fatal("Commodity " + comm + " not found in payload factor file.");
            System.exit(1);
        }
        return (payloadSCTG.getValueAt(n, "Payload (lbs)") * (float) 0.0005);
    }


    public static int getEnumOrderNumber(ModesFAF mode) {
        // return order number of mode
        int num = 1;
        for (ModesFAF thisMode: ModesFAF.values()) {
            if (thisMode == mode) return num;
            num++;
        }
        logger.warn("Could not find mode " + mode.toString());
        return 0;
    }


    public static ModesFAF getModeName (int mode) {
        return ModesFAF.values()[mode - 1];
    }


    public static int getHighestVal(int[] array) {
        // return highest number in array
        int high = Integer.MIN_VALUE;
        for (int num: array) high = Math.max(high, num);
        return high;
    }


    public static double sumArray (double[][] arr) {
        // sum a two-dimensional double array

        double sum = 0;
        for (double[] anArr : arr) {
            for (int j = 0; j < arr[1].length; j++) {
                sum += anArr[j];
            }
        }
        return sum;
    }


    public static TableDataSet createSpecialRegions(String[] specRegNames, String[] specRegModes, int[] specRegCodes,
                                                    int [] specRegZones, int[] specRegFAFCodes) {
        // create TableDataSet with special regions

        if (specRegCodes.length != specRegNames.length || specRegCodes.length != specRegModes.length|| specRegCodes.length != specRegFAFCodes.length) {
            logger.error ("Names of special regions and modes of special regions and codes of special regions and " +
                    "FAF codes of special regions all need to be of same length. No special regions created.");
            return null;
        }
        TableDataSet specRegions = new TableDataSet();
        specRegions.appendColumn(specRegNames, "Name");
        specRegions.appendColumn(specRegCodes, "modelCode");
        specRegions.appendColumn(specRegZones, "modelZone");
        specRegions.appendColumn(specRegFAFCodes, "faf3code");
        specRegions.appendColumn(specRegModes, "mode");
        float[] dummyEmployment = new float[specRegCodes.length];
        for (int i = 0; i < dummyEmployment.length; i++) dummyEmployment[i] = 1;
        specRegions.appendColumn(dummyEmployment, "Employment");
        return specRegions;
    }


    public static boolean countyFlowConnectsWithHawaii (int orig, int dest) {
        // check if flow connects with Hawaii county
        int oState = (int) (orig / 1000f);
        int dState = (int) (dest / 1000f);
        return oState == 15 || dState == 15;
    }


    public static boolean arrayContainsElement (String element, String[] array) {
        // Check if Array contains Element

        boolean result = false;
        for (String t: array) if (t.equals(element)) result = true;
        return result;
    }


    public static int[] expandArrayByOneElement (int[] existing, int addElement) {
        // create new array that has length of existing.length + 1 and copy values into new array
        int[] expanded = new int[existing.length + 1];
        System.arraycopy(existing, 0, expanded, 0, existing.length);
        expanded[expanded.length - 1] = addElement;
        return expanded;
    }


    public static float[] expandArrayByOneElement (float[] existing, float addElement) {
        // create new array that has length of existing.length + 1 and copy values into new array
        float[] expanded = new float[existing.length + 1];
        System.arraycopy(existing, 0, expanded, 0, existing.length);
        expanded[expanded.length - 1] = addElement;
        return expanded;
    }


    public static String[] expandArrayByOneElement (String[] existing, String addElement) {
        // create new array that has length of existing.length + 1 and copy values into new array
        String[] expanded = new String[existing.length + 1];
        System.arraycopy(existing, 0, expanded, 0, existing.length);
        expanded[expanded.length - 1] = addElement;
        return expanded;
    }


    public static float rounder(float value, int digits) {
        // rounds value to digits behind the decimal point
        return Math.round(value * Math.pow(10, digits) + 0.5)/(float) Math.pow(10, digits);
    }


    public static String[] findUniqueElements (String[] list) {
        // find unique elements in list[] and return string[] with these elements

        ArrayList<String> unique = new ArrayList<String>();
        for (String txt: list) {
            if (!unique.contains(txt)) unique.add(txt);
        }
        String[] elements = new String[unique.size()];
        for (int i = 0; i < unique.size(); i++) elements[i] = unique.get(i);
        return elements;
    }


    public static float getSum (float[] array) {
        float sum = 0;
        for (float val: array) sum += val;
        return sum;
    }

    public static double getSum (double[] array) {
        double sum = 0;
        for (double val: array) sum += val;
        return sum;
    }

    public static float getSum (float[][] array) {
        float sum = 0;
        for (float[] anArray : array) {
            for (int j = 0; j < array[0].length; j++) sum += anArray[j];
        }
        return sum;
    }

    public static double getSum (double[][] array) {
        double sum = 0;
        for (double[] anArray : array) {
            for (int j = 0; j < array[0].length; j++) sum += anArray[j];
        }
        return sum;
    }

    public static String[] getUniqueListOfValues (String[] list) {
        // itentify unique list of value in list[] and return as shortened string list
        ArrayList<String> al = new ArrayList<>();
        for (String txt: list) {
            if (!al.contains(txt) && txt != null) al.add(txt);
        }
        String[] shortenedList = new String[al.size()];
        for (int i = 0; i < al.size(); i++) {
            shortenedList[i] = al.get(i);
        }
        return shortenedList;
    }
}
