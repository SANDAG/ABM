package org.sandag.htm.applications;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.sandag.htm.processFAF.countyTruckModel;
import org.sandag.htm.processFAF.fafUtils;
import org.sandag.htm.processFAF.readFAF3;

import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Application of countyTruckModel for Ohio State
 * Author: Rolf Moeckel, PB Albuquerque
 * Date: June 15, 2012 (Chicago IL)

 */
public class ohio {

    private static Logger logger = Logger.getLogger(ohio.class);
    private static String[] listOfRailRegions;
    private static String[] railRegionReference;
    private static boolean[] relevantCommodities;

    public static void summarizeFAFData (ResourceBundle appRb, int[] countyFips) {
        // Summarize commodity flows of FAF data

//        if (!ResourceUtil.getBooleanProperty(appRb, "read.in.raw.faf.data")) {
//            logger.error("Cannot summarize data for Ohio, set \"read.in.raw.faf.data\" to true.");
//            return;
//        }
        if (ResourceUtil.getBooleanProperty(appRb, "summarize.by.ohio.rail.zones")) {
            readOhioRailRegions(appRb, countyFips);
            readRelevantCommodities(appRb);
        }

    }


    private static void readOhioRailRegions (ResourceBundle appRb, int[] countyFips) {
        // create reference between fips code and Ohio Rail Region

        logger.info("Reading Ohio Rail Regions");
        TableDataSet railRegions = fafUtils.importTable(appRb.getString("rail.zone.definition"));
        int highestFips = fafUtils.getHighestVal(countyFips);

        railRegionReference = new String[highestFips + 1];
        for (int row = 1; row <= railRegions.getRowCount(); row++) {
            int fips = (int) railRegions.getValueAt(row, "fips");
            String reg = railRegions.getStringValueAt(row, "ohioRailRegion");
            railRegionReference[fips] = reg;
        }
        listOfRailRegions = fafUtils.getUniqueListOfValues(railRegionReference);
    }


    private static void readRelevantCommodities (ResourceBundle appRb) {
        // Read how commodities are grouped by SCTG cagegory

        TableDataSet comGroups = fafUtils.importTable(appRb.getString("commodity.grouping"));
        relevantCommodities = new boolean[fafUtils.getHighestVal(readFAF3.sctgCommodities) + 1];
        for (int i = 0; i < relevantCommodities.length; i++) relevantCommodities[i] = false;
        for (int row = 1; row <= comGroups.getRowCount(); row++) {
            int sctg = (int) comGroups.getValueAt(row, "SCTG");
            String truckType = comGroups.getStringValueAt(row, "MainTruckType");
            relevantCommodities[sctg] = truckType.equals("Van");     // set relevantCommodities to true if truckType equals Van
        }
    }


    public static void sumFlowByRailZone(ResourceBundle appRb, int year, int[] countyFips, int[] countyIndex, HashMap<String, float[][]> cntFlows) {
        // summarize flows by rail regions

        // Step 1: Initialize counter
        HashMap<String, Integer> railRegionIndex = new HashMap<>();
        int regionCounter = 0;
        for (String txt: listOfRailRegions) {
            railRegionIndex.put(txt,regionCounter);
            regionCounter++;
        }
        double[][] summaryRailRegions = new double[listOfRailRegions.length][listOfRailRegions.length];

        // Step 2: Summarize flows
        String[] commodities = readFAF3.sctgStringCommodities;
        for (String com: commodities) {
            if (!relevantCommodities[Integer.parseInt(com.substring(4))]) continue;
            float[][] flows = cntFlows.get(com);
            for (int oFips: countyFips) {
                if (railRegionReference[oFips] != null) {
                    int origRailRegion = railRegionIndex.get(railRegionReference[oFips]);
                    for (int dFips: countyFips) {
                        if (railRegionReference[dFips] != null) {
                            int destRailRegion = railRegionIndex.get(railRegionReference[dFips]);
                            summaryRailRegions[origRailRegion][destRailRegion] += flows[countyIndex[oFips]][countyIndex[dFips]];
                        }
                    }
                }
            }
        }

        PrintWriter pw = fafUtils.openFileForSequentialWriting(appRb.getString("rail.zone.output") + "_" + year + ".csv");

        pw.print("Region");
        for (String txt: listOfRailRegions) pw.print("," + txt);
        pw.println();
        for (String orig: listOfRailRegions) {
            pw.print(orig);
            for (String dest: listOfRailRegions) {
                pw.print("," + summaryRailRegions[railRegionIndex.get(orig)][railRegionIndex.get(dest)]);
            }
            pw.println();
        }
        pw.close();
    }
}
