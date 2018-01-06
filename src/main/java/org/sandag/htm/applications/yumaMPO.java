package org.sandag.htm.applications;

import com.pb.common.datafile.TableDataSet;
import org.sandag.htm.processFAF.fafUtils;
import org.sandag.htm.processFAF.readFAF3;

import java.io.PrintWriter;
import java.util.ResourceBundle;

/**
 * Application of countyTruckModel for Yuma, AZ MPO
 * Author: Rolf Moeckel, PB Albuquerque
 * Date: April 5, 2012 (Santa Fe NM)

 */
public class yumaMPO {

    private static float[][][] tons_yuma_border;
    private static float[][][] tons_yuma_fafzones;
    private static float[][][] tons_yuma_counties;
    private static float[][] tons_yuma_mex;
    private static TableDataSet counties;
    private static int[] countyIndex;


    public static void initializeVariables(ResourceBundle appRb) {
        // set up variables for summaries
        int maxCom = fafUtils.getHighestVal(readFAF3.sctgCommodities);
        tons_yuma_border = new float[2][560 + 1][maxCom + 1];    // by direction, FAF zone and commodity
        tons_yuma_fafzones = new float[2][560 + 1][maxCom + 1];  // by direction, FAF zone and commodity
        tons_yuma_counties = new float[2][15 + 1][maxCom + 1];       // by direction, county and commodity
        counties = fafUtils.importTable(appRb.getString("county.ID"));
        counties.buildIndex(counties.getColumnPosition("COUNTYFIPS"));
        countyIndex = new int[fafUtils.getHighestVal(counties.getColumnAsInt("COUNTYFIPS")) + 1];
        for (int i = 0; i < countyIndex.length; i++) countyIndex[i] = -1;
        countyIndex[4001] = 0;     // index counties within Arizona
        countyIndex[4003] = 1;
        countyIndex[4005] = 2;
        countyIndex[4007] = 3;
        countyIndex[4009] = 4;
        countyIndex[4011] = 5;
        countyIndex[4012] = 6;
        countyIndex[4013] = 7;
        countyIndex[4015] = 8;
        countyIndex[4017] = 9;
        countyIndex[4019] = 10;
        countyIndex[4021] = 11;
        countyIndex[4023] = 12;
        countyIndex[4025] = 13;
        countyIndex[4027] = 14;
        countyIndex[6025] = 15;    // index Imperial County in California
        tons_yuma_mex = new float[2][2];
    }


    public static void saveForYuma(int com, int oFips, int dFips, float tons) {
        // save relevant data for Yuma-specific summaries

//        try {
        int oFAF = 0;
        if (oFips < 60000) oFAF = (int) counties.getIndexedValueAt(oFips, "FAF3region");
        int dFAF = 0;
        if (dFips < 60000) dFAF = (int) counties.getIndexedValueAt(dFips, "FAF3region");

        // Flows through Yuma border crossing
        if (oFips == 61934 && dFAF != 0) {
            tons_yuma_border[0][dFAF][com] += tons;
        } else if (dFips == 61934 && oFAF != 0) {
            tons_yuma_border[1][oFAF][com] += tons;
        }
        // Flows to/from Yuma
        if (oFips == 4027 && dFAF != 0) {
            tons_yuma_fafzones[0][dFAF][com] += tons;
            if (countyIndex[dFips] >= 0) tons_yuma_counties[0][countyIndex[dFips]][com] += tons;
        } else if (dFips == 4027 && oFAF != 0) {
            tons_yuma_fafzones[1][oFAF][com] += tons;
            if (countyIndex[oFips] >= 0) tons_yuma_counties[1][countyIndex[oFips]][com] += tons;
        }
        // Flows between Yuma and Mexico
        if (oFips == 4027) tons_yuma_mex[0][0] += tons;
        if (oFips == 4027 && dFips > 60000) tons_yuma_mex[0][1] += tons;
        if (dFips == 4027) tons_yuma_mex[1][0] += tons;
        if (dFips == 4027 && oFips > 60000) tons_yuma_mex[1][1] += tons;

//        } catch (Exception e) {
//            System.out.println("Error: " + com+" "+oFips+" "+dFips);
//        }
    }


    public static void writeOutResults(ResourceBundle appRb, int year) {
        // write results to summary file

        PrintWriter pw = fafUtils.openFileForSequentialWriting(appRb.getString("yuma.summary") + year + ".csv");

        pw.println("Total tons leaving Yuma : " + tons_yuma_mex[0][0]);
        pw.println("Tons from Yuma to Mexico: " + tons_yuma_mex[0][1]);
        pw.println("Total tons entering Yuma: " + tons_yuma_mex[1][0]);
        pw.println("Tons from Mexico to Yuma: " + tons_yuma_mex[1][1]);
        pw.println();

        pw.println("SUMMARY BY FAF ZONE");
        pw.print("FlowsFromYumaToFAFZone");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int faf = 1; faf <= 560; faf++) {
            float sum = 0;
            for (int i: readFAF3.sctgCommodities) sum += tons_yuma_fafzones[0][faf][i];
            if (sum > 0) {
                pw.print(faf);
                for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_fafzones[0][faf][i]);
                pw.println();
            }
        }
        pw.println();
        pw.print("FlowsToYumaFromFAFZone");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int faf = 1; faf <= 560; faf++) {
            float sum = 0;
            for (int i: readFAF3.sctgCommodities) sum += tons_yuma_fafzones[1][faf][i];
            if (sum > 0) {
                pw.print(faf);
                for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_fafzones[1][faf][i]);
                pw.println();
            }
        }
        pw.println();

        pw.println("SUMMARY BY COUNTY");
        pw.print("FlowsFromYumaToCounty");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int county: counties.getColumnAsInt("COUNTYFIPS")) {
            if (countyIndex[county] == -1) continue;
            pw.print(county);
            for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_counties[0][countyIndex[county]][i]);
            pw.println();
        }
        pw.println();
        pw.print("FlowsToYumaFromCounty");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int county: counties.getColumnAsInt("COUNTYFIPS")) {
            if (countyIndex[county] == -1) continue;
            pw.print(county);
            for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_counties[1][countyIndex[county]][i]);
            pw.println();
        }
        pw.println();

        pw.println("SUMMARY BY BORDER ZONE");
        pw.print("FlowsFromYumaBorderToFAFZone");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int faf = 1; faf <= 560; faf++) {
            float sum = 0;
            for (int i: readFAF3.sctgCommodities) sum += tons_yuma_border[0][faf][i];
            if (sum > 0) {
                pw.print(faf);
                for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_border[0][faf][i]);
                pw.println();
            }
        }
        pw.println();
        pw.print("FlowsToYumaBorderFromFAFZone");
        for (int i: readFAF3.sctgCommodities) pw.print(",SCTG" + i);
        pw.println();
        for (int faf = 1; faf <= 560; faf++) {
            float sum = 0;
            for (int i: readFAF3.sctgCommodities) sum += tons_yuma_border[1][faf][i];
            if (sum > 0) {
                pw.print(faf);
                for (int i: readFAF3.sctgCommodities) pw.print("," + tons_yuma_border[1][faf][i]);
                pw.println();
            }
        }
        pw.close();
    }
}
