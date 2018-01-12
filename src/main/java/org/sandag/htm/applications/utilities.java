package com.pb.sandag_tm;

import com.pb.common.datafile.DBFFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.sandag.htm.processFAF.disaggregateFlows;
import org.sandag.htm.processFAF.fafUtils;
import org.sandag.htm.processFAF.readFAF3;
import org.sandag.htm.processFAF.ReadFAF4;

import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Utility methods for SANDAG external truck model
 * Author: Rolf Moeckel, PB Albuquerque
 * Date:   6 March 2013 (Santa Fe, NM)
 * Version 1.0
 */

public class utilities {


    private static ResourceBundle rb;
    private static String model;
    private static int year;
    public static int[] countyFips;
    public static int[] countyFipsIndex;
    private static int[] commodityGroupOfSCTG;
    private static HashMap<Integer, int[]> comGroupDefinition;
    private static int[] listOfCommodityGroups;
    private static Logger logger = Logger.getLogger(utilities.class);


    public static void truckModelInitialization(String[] args, readFAF3 faf3, disaggregateFlows df) {
        // read in properties file and define basic variables, such as model year

        String rbName = args[0];
        File propFile = new File(rbName);
        rb = ResourceUtil.getPropertyBundle(propFile);
        model = args[1];
        if (!model.equalsIgnoreCase("counties") && !model.equalsIgnoreCase("zones")) {
            logger.error("Call program with parameters <properties file> <counties||zones> <year>");
            logger.error("Geography " + args[1] + " not understood. Choose \"counties\" or \"zones\"");
            System.exit(1);
        }
        year = Integer.parseInt(args[2]);
        logger.info("Starting SANDAG Truck Model for year " + utilities.getYear());
        readCommodityGrouping();
    }


    public static void truckModelInitialization(String[] args, ReadFAF4 faf4, disaggregateFlows df) {
        // read in properties file and define basic variables, such as model year

        String rbName = args[0];
        File propFile = new File(rbName);
        rb = ResourceUtil.getPropertyBundle(propFile);
        model = args[1];
        if (!model.equalsIgnoreCase("counties") && !model.equalsIgnoreCase("zones")) {
            logger.error("Call program with parameters <properties file> <counties||zones> <year>");
            logger.error("Geography " + args[1] + " not understood. Choose \"counties\" or \"zones\"");
            System.exit(1);
        }
        year = Integer.parseInt(args[2]);
        logger.info("Starting SANDAG Truck Model for year " + utilities.getYear());
        readCommodityGrouping();
    }

    private static void readCommodityGrouping () {
        // read in commodity grouping

        TableDataSet commodityGrouping = fafUtils.importTable(utilities.getRb().getString("commodity.grouping"));
        int highestValue = utilities.getHighestVal(commodityGrouping.getColumnAsInt("SCTG"));
        commodityGroupOfSCTG = new int[highestValue + 1];
        comGroupDefinition = new HashMap<>();
        for (int row = 1; row <= commodityGrouping.getRowCount(); row++) {
            int sctg = (int) commodityGrouping.getValueAt(row, "SCTG");
            int grp = (int) commodityGrouping.getValueAt(row, "CommodityGroup");
            commodityGroupOfSCTG[sctg] = grp;
            if (comGroupDefinition.containsKey(grp)) {
                int[] commodities = comGroupDefinition.get(grp);
                comGroupDefinition.put(grp, expandArrayByOneElement(commodities, sctg));
            } else {
                comGroupDefinition.put(grp, new int[]{sctg});
            }
        }
        listOfCommodityGroups = new int[comGroupDefinition.size()];
        int count = 0;
        for (int comGrp: comGroupDefinition.keySet()) {
            listOfCommodityGroups[count] = comGrp;
            count++;
        }
    }


    public static int[] getCommodityGroupOfSCTG() {
        return commodityGroupOfSCTG;
    }


    public static HashMap<Integer, int[]> getComGroupDefinition() {
        return comGroupDefinition;
    }


    public static int[] getListOfCommodityGroups() {
        return listOfCommodityGroups;
    }


    public static float rounder(float value, int digits) {
        // rounds value to digits behind the decimal point
        return Math.round(value * Math.pow(10, digits) + 0.5)/(float) Math.pow(10, digits);
    }


    public static int getYear() {
        return year;
    }

    public static ResourceBundle getRb() {
        return rb;
    }

    public static String getModel() {
        return model;
    }

    public static boolean getBooleanProperty (String token, boolean defaultIfNotAvailable) {
        return ResourceUtil.getBooleanProperty(rb, token, defaultIfNotAvailable);
    }


    public static int getHighestVal(int[] array) {
        // return highest number in array

        int high = Integer.MIN_VALUE;
        for (int num: array) high = Math.max(high, num);
        return high;
    }


    public static float rounder(double value, int digits) {
        // rounds value to digits behind the decimal point
        return Math.round(value * Math.pow(10, digits) + 0.5)/(float) Math.pow(10, digits);
    }


    public static int[] expandArrayByOneElement (int[] existing, int addElement) {
        // create new array that has length of existing.length + 1 and copy values into new array
        int[] expanded = new int[existing.length + 1];
        System.arraycopy(existing, 0, expanded, 0, existing.length);
        expanded[expanded.length - 1] = addElement;
        return expanded;
    }


    public static void createZoneList() {
        // Create array with specialRegions that serve as port of entry/exit

        int[] poeLand = fafUtils.importTable(rb.getString("ports.of.entry")).getColumnAsInt("pointOfEntry");
        int[] poeSea = fafUtils.importTable(rb.getString("marine.ports.of.entry")).getColumnAsInt("pointOfEntry");
        int[] poeAir = fafUtils.importTable(rb.getString("air.ports.of.entry")).getColumnAsInt("pointOfEntry");
        int[] list = new int[poeLand.length + poeSea.length + poeAir.length];
        System.arraycopy(poeLand, 0, list, 0, poeLand.length);
        System.arraycopy(poeSea, 0, list, poeLand.length, poeSea.length);
        System.arraycopy(poeAir, 0, list, poeLand.length + poeSea.length, poeAir.length);
        countyFips = fafUtils.createCountyFipsArray(list);
        countyFipsIndex = new int[fafUtils.getHighestVal(countyFips) + 1];
        for (int i = 0; i < countyFips.length; i++) {
            countyFipsIndex[countyFips[i]] = i;
        }
    }


    public static TableDataSet importTableFromDBF(String filePath) {
        // read a dbf file into a TableDataSet

        TableDataSet tblData;
        DBFFileReader dbfReader = new DBFFileReader();
        try {
            tblData = dbfReader.readFile(new File( filePath ));
        } catch (Exception e) {
            throw new RuntimeException("File not found: <" + filePath + ">.", e);
        }
        dbfReader.close();
        return tblData;
    }


    public static double getSum (double[] array) {
        // return sum of all elements in array
        double sum = 0;
        for (double val: array) sum += val;
        return sum;
    }


    public static float[] convertIntArrayListToArray(ArrayList<Integer> al) {
        float[] array = new float[al.size()];
        for (int i = 0; i < al.size(); i++) array[i] = al.get(i);
        return array;
    }


    public static float[] convertFloatArrayListToArray(ArrayList<Float> al) {
        float[] array = new float[al.size()];
        for (int i = 0; i < al.size(); i++) array[i] = al.get(i);
        return array;
    }

}
