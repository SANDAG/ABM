package org.sandag.htm.processFAF;

import org.apache.log4j.Logger;

import java.util.ResourceBundle;
import java.util.HashMap;
import java.io.PrintWriter;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

/**
 * This class reads FAF4 data and stores data in a TableDataSet
 * Author: Joel Freedman, RSG - based on readFAF3 by Rolf Moeckel, PB
 * Date: Dec 27, 2017
 */

public class ReadFAF4 {
    Logger logger = Logger.getLogger(ReadFAF4.class);
    private int factor;
    private String[] valueColumnName;
    private TableDataSet faf4commodityFlows;
    public static TableDataSet fafRegionList;
    private String[] regionState;
    private static int[] domRegionIndex;
    static public int[] sctgCommodities;
    static public String[] sctgStringCommodities;
    static private int[] sctgStringIndex;
    private static HashMap<Integer, TableDataSet> portsOfEntry;
    private static HashMap<Integer, TableDataSet> marinePortsOfEntry;
    private static HashMap<Integer, TableDataSet> railPortsOfEntry;
    private static HashMap<Integer, TableDataSet> airPortsOfEntry;
    private static int[] listOfBorderPortOfEntries;


    public void readAllData (ResourceBundle appRb, int year, String unit) {
        // read input data

        if (ResourceUtil.getBooleanProperty(appRb, "read.in.raw.faf.data", true))
            readAllFAF4DataSets(appRb, unit, year);
        readCommodityList(appRb);
        readFAF4ReferenceLists(appRb);
    }


    public void readCommodityList(ResourceBundle appRb) {
        // read commodity names
        TableDataSet sctgComList = fafUtils.importTable(ResourceUtil.getProperty(appRb, "faf4.sctg.commodity.list"));
        sctgCommodities = new int[sctgComList.getRowCount()];
        sctgStringCommodities = new String[sctgCommodities.length];
        for (int i = 1; i <= sctgComList.getRowCount(); i++) {
            sctgCommodities[i-1] = (int) sctgComList.getValueAt(i, "SCTG");
            if (sctgCommodities[i-1] < 10) sctgStringCommodities[i-1] = "SCTG0" + sctgCommodities[i-1];
            else sctgStringCommodities[i-1] = "SCTG" + sctgCommodities[i-1];
        }
        sctgStringIndex = new int[fafUtils.getHighestVal(sctgCommodities) + 1];
        for (int num = 0; num < sctgCommodities.length; num++) sctgStringIndex[sctgCommodities[num]] = num;
    }


    public int getIndexOfCommodity (int commodity) {
        return sctgStringIndex[commodity];
    }


    public static String getSCTGname(int sctgInt) {
        // get String name from sctg number
        return sctgStringCommodities[sctgStringIndex[sctgInt]];
    }


    public static String getFAFzoneName(int fafInt) {
        // get String name from int FAF zone code number
        return fafRegionList.getStringValueAt(domRegionIndex[fafInt], "FAF4 Zones -Short Description");
    }


    public static String getFAFzoneState(int fafInt) {
        // get String two-letter abbreviation of state of fafInt
        return fafRegionList.getStringValueAt(domRegionIndex[fafInt], "State");
    }


    public void definePortsOfEntry(ResourceBundle appRb) {
        // read data to translate ports of entry in network links

        // Border crossings
        portsOfEntry = new HashMap<>();
        TableDataSet poe = fafUtils.importTable(appRb.getString("ports.of.entry"));
        for (int row = 1; row <= poe.getRowCount(); row++) {
            int fafID = (int) poe.getValueAt(row, "faf4id");
            int node = (int) poe.getValueAt(row, "pointOfEntry");
            float weight = poe.getValueAt(row, "weight");
            TableDataSet newPortsOfEntry = new TableDataSet();
            if (portsOfEntry.containsKey(fafID)) {
                TableDataSet existingNodes = portsOfEntry.get(fafID);
                int[] nodes = existingNodes.getColumnAsInt("COUNTYFIPS");         // use same column labels as for
                float[] weights = existingNodes.getColumnAsFloat("Employment");   //county TableDataSets to ease disaggregation
                int[] newNodes = fafUtils.expandArrayByOneElement(nodes, node);
                float[] newWeights = fafUtils.expandArrayByOneElement(weights, weight);
                newPortsOfEntry.appendColumn(newNodes, "COUNTYFIPS");
                newPortsOfEntry.appendColumn(newWeights, "Employment");
            } else {
                newPortsOfEntry.appendColumn(new int[]{node}, "COUNTYFIPS");
                newPortsOfEntry.appendColumn(new float[]{weight}, "Employment");
            }
            portsOfEntry.put(fafID, newPortsOfEntry);
        }
        listOfBorderPortOfEntries = poe.getColumnAsInt("pointOfEntry");

        // Marine ports
        marinePortsOfEntry = new HashMap<>();
        if (appRb.containsKey("marine.ports.of.entry")) {
            TableDataSet mpoe = fafUtils.importTable(appRb.getString("marine.ports.of.entry"));
            for (int row = 1; row <= mpoe.getRowCount(); row++) {
                int fafID = (int) mpoe.getValueAt(row, "faf4id");
                int node = (int) mpoe.getValueAt(row, "pointOfEntry");
                float weight = mpoe.getValueAt(row, "weight");
                TableDataSet newPortsOfEntry = new TableDataSet();
                if (marinePortsOfEntry.containsKey(fafID)) {
                    TableDataSet existingNodes = marinePortsOfEntry.get(fafID);
                    int[] nodes = existingNodes.getColumnAsInt("COUNTYFIPS");         // use same column labels as for
                    float[] weights = existingNodes.getColumnAsFloat("Employment");   //county TableDataSets to ease disaggregation
                    int[] newNodes = fafUtils.expandArrayByOneElement(nodes, node);
                    float[] newWeights = fafUtils.expandArrayByOneElement(weights, weight);
                    newPortsOfEntry.appendColumn(newNodes, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(newWeights, "Employment");
                } else {
                    newPortsOfEntry.appendColumn(new int[]{node}, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(new float[]{weight}, "Employment");
                }
                marinePortsOfEntry.put(fafID, newPortsOfEntry);
            }
        }
        // Rail ports (railyards)
        railPortsOfEntry = new HashMap<>();
        if (appRb.containsKey("rail.ports.of.entry")) {
            TableDataSet rpoe = fafUtils.importTable(appRb.getString("rail.ports.of.entry"));
            for (int row = 1; row <= rpoe.getRowCount(); row++) {
                int fafID = (int) rpoe.getValueAt(row, "faf4id");
                int node = (int) rpoe.getValueAt(row, "pointOfEntry");
                float weight = rpoe.getValueAt(row, "weight");
                TableDataSet newPortsOfEntry = new TableDataSet();
                if (railPortsOfEntry.containsKey(fafID)) {
                    TableDataSet existingNodes = railPortsOfEntry.get(fafID);
                    int[] nodes = existingNodes.getColumnAsInt("COUNTYFIPS");         // use same column labels as for
                    float[] weights = existingNodes.getColumnAsFloat("Employment");   //county TableDataSets to ease disaggregation
                    int[] newNodes = fafUtils.expandArrayByOneElement(nodes, node);
                    float[] newWeights = fafUtils.expandArrayByOneElement(weights, weight);
                    newPortsOfEntry.appendColumn(newNodes, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(newWeights, "Employment");
                } else {
                    newPortsOfEntry.appendColumn(new int[]{node}, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(new float[]{weight}, "Employment");
                }
                railPortsOfEntry.put(fafID, newPortsOfEntry);
            }
        }
        // Airports
        airPortsOfEntry = new HashMap<>();
        if (appRb.containsKey("air.ports.of.entry")) {
            TableDataSet apoe = fafUtils.importTable(appRb.getString("air.ports.of.entry"));
            for (int row = 1; row <= apoe.getRowCount(); row++) {
                int fafID = (int) apoe.getValueAt(row, "faf4id");
                int node = (int) apoe.getValueAt(row, "pointOfEntry");
                float weight = apoe.getValueAt(row, "weight");
                TableDataSet newPortsOfEntry = new TableDataSet();
                if (airPortsOfEntry.containsKey(fafID)) {
                    TableDataSet existingNodes = airPortsOfEntry.get(fafID);
                    int[] nodes = existingNodes.getColumnAsInt("COUNTYFIPS");         // use same column labels as for
                    float[] weights = existingNodes.getColumnAsFloat("Employment");   //county TableDataSets to ease disaggregation
                    int[] newNodes = fafUtils.expandArrayByOneElement(nodes, node);
                    float[] newWeights = fafUtils.expandArrayByOneElement(weights, weight);
                    newPortsOfEntry.appendColumn(newNodes, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(newWeights, "Employment");
                } else {
                    newPortsOfEntry.appendColumn(new int[]{node}, "COUNTYFIPS");
                    newPortsOfEntry.appendColumn(new float[]{weight}, "Employment");
                }
                airPortsOfEntry.put(fafID, newPortsOfEntry);
            }
        }
    }


    public static int[] getListOfBorderPortOfEntries() {
        return listOfBorderPortOfEntries;
    }



    public static TableDataSet getPortsOfEntry (int fafZone) {
        // return list of ports of entry if available, otherwise return fafZone
        if (portsOfEntry.containsKey(fafZone)) return portsOfEntry.get(fafZone);
        else return null;
    }


    public static TableDataSet getMarinePortsOfEntry (int fafZone) {
        // return list of ports of entry if available, otherwise return fafZone
        if (marinePortsOfEntry.containsKey(fafZone)) return marinePortsOfEntry.get(fafZone);
        else return null;
    }


    public static TableDataSet getAirPortsOfEntry (int fafZone) {
        // return list of ports of entry if available, otherwise return fafZone
        if (airPortsOfEntry.containsKey(fafZone)) return airPortsOfEntry.get(fafZone);
        else return null;
    }


    public void readAllFAF4dataSets2015(ResourceBundle appRb, String unit) {
        // read all FAF4 data into TableDataSets in unit (= tons or dollars)

        logger.info ("Reading domestic FAF4 data in " + unit);
        if (unit.equals("tons")) {
            factor = 1000;    // tons are in 1,000s
            valueColumnName = new String[]{"tons_2015"};
        } else if (unit.equals("dollars")) {
            factor = 1000000;  // dollars are in 1,000,000s
            valueColumnName = new String[]{"value_2015"};
        } else {
            logger.fatal ("Wrong token " + unit + " in method readAllFAF4dataSets2015. Use tons or dollars.");
        }
        faf4commodityFlows = readFAF4CommodityFlows(appRb, unit);
    }


    public double[] summarizeFlowByCommodity (ModesFAF fafMode) {
        // sum commodity flows by commodity and return array with total tons

        double[] totalFlows = new double[sctgCommodities.length];
        int modeNum = fafUtils.getEnumOrderNumber(fafMode);
        for (int row = 1; row <= faf4commodityFlows.getRowCount(); row++) {
            if (faf4commodityFlows.getValueAt(row, "dms_mode") != modeNum) continue;
            int com = sctgStringIndex[(int) faf4commodityFlows.getValueAt(row, "sctg2")];
            if (valueColumnName.length == 1) {
                // use year provided by user
                totalFlows[com] += faf4commodityFlows.getValueAt(row, valueColumnName[0]);
            } else {
                // interpolate between two years
                float val1 = faf4commodityFlows.getValueAt(row, valueColumnName[0]);
                float val2 = faf4commodityFlows.getValueAt(row, valueColumnName[1]);
                totalFlows[com] += val1 + (val2 - val1) * Float.parseFloat(valueColumnName[2]);
            }
        }
        return totalFlows;
    }


    public void readAllFAF4DataSets(ResourceBundle appRb, String unit, int year) {
        // read all FAF4 data into TableDataSets in unit (= tons or dollars)

        logger.info ("  Reading FAF4 data in " + unit);
        switch (unit) {
            case "tons":
                factor = 1000;    // tons are provided in 1,000s
                break;
            case "dollars":
                factor = 1000000;  // dollars are provided in 1,000,000s
                break;
            default:
                logger.fatal("Wrong token " + unit + " in method readAllFAF4DataSets. Use tons or dollars.");
                throw new RuntimeException();
        }
        int[] availYears = {2012, 2013, 2014, 2015, 2020, 2025, 2030, 2035, 2040, 2045};
        boolean yearInFaf = false;
        for (int y: availYears) if (year == y) yearInFaf = true;
        if (!yearInFaf) {  // interpolate between two years
            logger.info("  Year " + year + " does not exist in FAF4 data.");
            int year1 = availYears[0];
            int year2 = availYears[availYears.length-1];
            for (int availYear : availYears) if (availYear < year) year1 = availYear;
            for (int i = availYears.length - 1; i >= 0; i--) if (availYears[i] > year) year2 = availYears[i];
            logger.info("  FAF4 data are interpolated between " + year1 + " and " + year2 + ".");
            // first position: lower year, second position: higher year, third position: steps away from lower year
            valueColumnName = new String[]{unit + "_" + year1, unit + "_" + year2, String.valueOf((1f * (year - year1)) / (1f * (year2 - year1)))};
        } else {           // use year provided by user
            valueColumnName = new String[]{unit + "_" + year};
        }
        faf4commodityFlows = readFAF4CommodityFlows(appRb, unit);
    }


    private TableDataSet readFAF4CommodityFlows(ResourceBundle appRb, String unit) {
        // read FAF4 data and return TableDataSet with flow data
        String fileName = ResourceUtil.getProperty(appRb, ("faf4.data"));
        return fafUtils.importTable(fileName);
    }


    public HashMap<String,Float> createScaler(String[] tokens, double[] values) {
        // create HashMap with state O-D pairs that need to be scaled

        HashMap<String, Float> scaler = new HashMap<String, Float>();
        if (tokens.length != values.length) {
            throw new RuntimeException("Error.  scaling.truck.trips.tokens must be same length as scaling.truck.trips.values");
        }
        for (int i=0; i<tokens.length; i++) {
            scaler.put(tokens[i], (float) values[i]);
        }
        return scaler;
    }


    public void getFlowsByMode(String outFileName, ModesFAF mode, reportFormat repF, HashMap<String, Float> scaler) {
        // extract truck flows for year yr and scale flows according to scaler HashMap (no special regions specified)

        PrintWriter outFile = fafUtils.openFileForSequentialWriting(outFileName);
        outFile.println("originFAF,destinationFAF,flowDirection," + commodityClassType.SCTG + "_commodity,shortTons");
        int modeNum = fafUtils.getEnumOrderNumber(mode);
        for (int row = 1; row <= faf4commodityFlows.getRowCount(); row++) {
            int type = (int) faf4commodityFlows.getValueAt(row, "trade_type");
            double val;
            if (valueColumnName.length == 1) {
                // use year provided by user
                val = faf4commodityFlows.getValueAt(row, valueColumnName[0]);
            } else {
                // interpolate between two years
                float val1 = faf4commodityFlows.getValueAt(row, valueColumnName[0]);
                float val2 = faf4commodityFlows.getValueAt(row, valueColumnName[1]);
                val = val1 + (val2 - val1) * Float.parseFloat(valueColumnName[2]);
            }
            val *= factor * odScaler(row, type, scaler);
            if (val == 0) continue;
            if (type == 1) writeDomesticFlow(modeNum, val, row, outFile);
            else if (type == 2) writeImportFlow(modeNum, val, row, outFile, repF);
            else if (type == 3) writeExportFlow(modeNum, val, row, outFile, repF);
            else if (type == 4) writeThroughFlow(modeNum, val, row, outFile, repF);
            else{
            	logger.info("Invalid trade_type in FAF4 dataset in row " + row + ": " + type);
            }
        }
        outFile.close();
    }


    public void writeFlowsByModeAndCommodity (String outFileName, ModesFAF mode, reportFormat repF,
                                              HashMap<String, Float> scaler) {
        // extract truck flows for year yr and scale flows according to scaler HashMap, including special regions

        PrintWriter outFile[] = new PrintWriter[sctgCommodities.length];
        for (int com: sctgCommodities) {
            String fileName;
            if (com < 10) fileName = outFileName + "_SCTG0" + com + ".csv";
            else fileName = outFileName + "_SCTG" + com + ".csv";
            outFile[sctgStringIndex[com]] = fafUtils.openFileForSequentialWriting(fileName);
            outFile[sctgStringIndex[com]].println("originFAF,destinationFAF,flowDirection,SCTG_commodity,shortTons");
        }
        int modeNum = fafUtils.getEnumOrderNumber(mode);
        for (int row = 1; row <= faf4commodityFlows.getRowCount(); row++) {
            int type = (int) faf4commodityFlows.getValueAt(row, "trade_type");
            double val;
            if (valueColumnName.length == 1) {
                // use year provided by user
                val = faf4commodityFlows.getValueAt(row, valueColumnName[0]);
            } else {
                // interpolate between two years
                float val1 = faf4commodityFlows.getValueAt(row, valueColumnName[0]);
                float val2 = faf4commodityFlows.getValueAt(row, valueColumnName[1]);
                val = val1 + (val2 - val1) * Float.parseFloat(valueColumnName[2]);
            }
            val *= factor * odScaler(row, type, scaler);
            if (val == 0) continue;
            int comIndex = getIndexOfCommodity((int) faf4commodityFlows.getValueAt(row, "sctg2"));
            if (type == 1) writeDomesticFlow(modeNum, val, row, outFile[comIndex]);
            else if (type == 2) writeImportFlow(modeNum, val, row, outFile[comIndex], repF);
            else if (type == 3) writeExportFlow(modeNum, val, row, outFile[comIndex], repF);
            else if (type == 4) writeThroughFlow(modeNum, val, row, outFile[comIndex], repF);
            else logger.info("Invalid trade_type in FAF4 dataset in row " + row + ": " + type);
        }
        for (int com: sctgCommodities) outFile[sctgStringIndex[com]].close();
    }


    public float odScaler (int row, int type, HashMap<String, Float> scaler) {
        // find scaler for origin destination pair in row

        int orig;
        int dest;
        if (type == 1) {
            orig = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
            dest = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
        } else if (type == 2) {
            orig = (int) faf4commodityFlows.getValueAt(row, "fr_orig");
            dest = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
        } else if (type == 3) {
            orig = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
            dest = (int) faf4commodityFlows.getValueAt(row, "fr_dest");
        } else {
            orig = (int) faf4commodityFlows.getValueAt(row, "fr_orig");
            dest = (int) faf4commodityFlows.getValueAt(row, "fr_dest");
        }
        String stateLevelToken = regionState[orig] + "_" + regionState[dest];
        String combo1Token = orig + "_" + regionState[dest];
        String combo2Token = regionState[orig] + "_" + dest;
        String fafLevelToken = orig + "_" + dest;
        float adj = 1;
        if (scaler.containsKey(stateLevelToken)) adj = scaler.get(stateLevelToken);
        if (scaler.containsKey(combo1Token)) adj = scaler.get(combo1Token);
        if (scaler.containsKey(combo2Token)) adj = scaler.get(combo2Token);
        if (scaler.containsKey(fafLevelToken)) adj = scaler.get(fafLevelToken);
        return adj;
    }


    private int tryGettingThisValue(int row, String token) {
        // for some flows, international zones/modes are empty -> catch this case and set zone to 0

        int region;
        try {
            region = (int) faf4commodityFlows.getValueAt(row, token);
        } catch (Exception e) {
            region = 0;
        }
        return region;
    }


    public void writeDomesticFlow (int modeNum, double val, int row, PrintWriter outFile) {
        // internal US flow
        if (faf4commodityFlows.getValueAt(row, "dms_mode") == modeNum) {
            int orig = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
            int dest = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
            int comm = (int) faf4commodityFlows.getValueAt(row, "sctg2");
            outFile.println(orig + "," + dest + ",domestic," + comm + "," + val);
        }
    }


    public void writeImportFlow (int modeNum, double val, int row, PrintWriter outFile, reportFormat repF) {
        // from abroad to US

        int frInMode = (int) faf4commodityFlows.getValueAt(row, "fr_inmode");
        int borderZone = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
        int comm = (int) faf4commodityFlows.getValueAt(row, "sctg2");
        if (frInMode == modeNum && repF != reportFormat.internat_domesticPart) {
            int orig = tryGettingThisValue(row, "fr_orig");
            outFile.println(orig + "," + borderZone + ",import," + comm + "," + val);
        }
        if (faf4commodityFlows.getValueAt(row, "dms_mode") == modeNum) {
            int dest = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
            String txt;
            if (frInMode == fafUtils.getEnumOrderNumber(ModesFAF.Water)) txt = ",import_port,";
            else if (frInMode == fafUtils.getEnumOrderNumber(ModesFAF.Rail)) txt = ",import_rail,";
            else if (frInMode == fafUtils.getEnumOrderNumber(ModesFAF.Air)) txt = ",import_airport,";
            else txt = ",import,";
            outFile.println(borderZone + "," + dest + txt + comm + "," + val);
        }
    }


    public void writeExportFlow (int modeNum, double val, int row, PrintWriter outFile, reportFormat repF) {
        // from US to abroad
        int frOutMode = tryGettingThisValue(row, "fr_outmode");
        int borderZone = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
        int comm = (int) faf4commodityFlows.getValueAt(row, "sctg2");
        if (frOutMode == modeNum && repF != reportFormat.internat_domesticPart) {
            int dest = tryGettingThisValue(row, "fr_dest");
            outFile.println(borderZone + "," + dest + ",export," + comm + "," + val);
        }
        if (faf4commodityFlows.getValueAt(row, "dms_mode") == modeNum) {
            int orig = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
            String txt = ",export,";
            if (frOutMode == fafUtils.getEnumOrderNumber(ModesFAF.Water)) txt = ",export_port,";
            if (frOutMode == fafUtils.getEnumOrderNumber(ModesFAF.Rail)) txt = ",export_rail,";
            if (frOutMode == fafUtils.getEnumOrderNumber(ModesFAF.Air)) txt = ",export_airport,";
            outFile.println(orig + "," + borderZone + txt + comm + "," + val);
        }
    }


    public void writeThroughFlow(int modeNum, double val, int row, PrintWriter outFile, reportFormat repF) {
        // flows in transit through US
        if ((int) faf4commodityFlows.getValueAt(row, "dms_mode") != modeNum) return;
        int borderInZone = (int) faf4commodityFlows.getValueAt(row, "dms_orig");
        int borderOutZone = (int) faf4commodityFlows.getValueAt(row, "dms_dest");
        logger.warn("Through flows not yet implemented. This flow from " + borderInZone + " to " + borderOutZone + " is lost.");
    }


    public void readFAF4ReferenceLists(ResourceBundle rb) {
        // read list of regions for FAF4
        String regFileName = rb.getString("faf4.region.list");
        fafRegionList = fafUtils.importTable(regFileName);
        int[] reg = fafRegionList.getColumnAsInt(fafRegionList.getColumnPosition("ZoneID"));
        domRegionIndex = new int[fafUtils.getHighestVal(reg) + 1];
        for (int num = 0; num < reg.length; num++) domRegionIndex[reg[num]] = num + 1;
        regionState = new String[fafUtils.getHighestVal(reg) + 1];
        for (int row = 1; row <= fafRegionList.getRowCount(); row++) {
            int zone = (int) fafRegionList.getValueAt(row, "ZoneID");
            regionState[zone] = fafRegionList.getStringValueAt(row, "State");
        }
    }


    public int[] getFAFzoneIDs () {
        return fafRegionList.getColumnAsInt("ZoneID");
    }


    public TableDataSet getFAF4Flows() {
        return faf4commodityFlows;
    }


    public int getFactor() {
        return factor;
    }


    public TableDataSet getFAF4CommodityFlows() {
        return faf4commodityFlows;
    }


    public String[] getValueColumnName() {
        return valueColumnName;
    }

}
