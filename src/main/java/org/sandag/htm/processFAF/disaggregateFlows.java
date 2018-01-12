package org.sandag.htm.processFAF;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.io.File;
import org.apache.log4j.Logger;


/**
 * This class disaggregates FAF flows from FAF zones to counties
 * User: Rolf Moeckel
 * Date: May 6, 2009
 * 
 * Updated 2017-12-27 JEF
 */

public class disaggregateFlows {

    static Logger logger = Logger.getLogger(disaggregateFlows.class);
    public static TableDataSet countyIDsWithEmployment;
    private HashMap<String, TableDataSet> countyShares;
    private TableDataSet truckTypeShares;
    private int[] truckTypeShareDistBin;
    private float[] truckTypeShareMT;
    private Matrix distCounties;
    private static int[] countyFips;
    private int FAFVersion=4;


    private static String[] empCats = {"Agriculture", "Construction Natural Resources and Mining", "Manufacturing",
            "Trade Transportation and Utilities", "Information", "Financial Activities",
            "Professional and Business Services", "Education and Health Services", "Leisure and Hospitality",
            "Other Services", "coalProduction"};  //, "Government"};    (no government employment available at county-level nationwide at this point


    public void prepareCountyData(ResourceBundle rb) {
        // prepare county data to provide total employment as a weight
        logger.info("Preparing county data for flow disaggregation");

        getUScountyEmployment(rb);
        if (readFAF2.domRegionList == null) readFAF2.readFAF2ReferenceLists(rb);
        //create HashMap that contains for every FAF region a TableDataSet of counties with their employment share
        countyShares = new HashMap<>();
        for (int fafNum = 1; fafNum <= readFAF2.domRegionList.getRowCount(); fafNum++) {
            for (String com: readFAF2.sctgCommodities) {
                TableDataSet CountyList = getCountySpecificDataByFAF2(fafNum, com, null, null);
                // dir is not really needed here, but kept for consistency reason as it is required
                // in prepareCountyDataForFAF(ResourceBundle rb, TableDataSet detailEmployment)
                String codeo = "orig_" + readFAF2.domRegionList.getStringValueAt(fafNum, "RegionName") + "_" + com;
                countyShares.put(codeo, CountyList);
                String coded = "dest_" + readFAF2.domRegionList.getStringValueAt(fafNum, "RegionName") + "_" + com;
                countyShares.put(coded, CountyList);
            }
        }
    }


    public void prepareCountyData(ResourceBundle rb, int yr, TableDataSet detailEmployment, String truckType) {
        // prepare county data to provide detailed employment given in detailEmployment and total employment elsewhere
        // as a weight
        logger.info("Preparing county data for flow disaggregation for year " + yr + "...");

        HashMap<String, Float> useC = createMakeUseHashMap(rb, "use.coefficients");
        HashMap<String, Float> makeC = createMakeUseHashMap(rb, "make.coefficients");

        truckTypeShares = fafUtils.importTable(rb.getString("truck.type.by.distance"));
        if (truckType.equalsIgnoreCase("weight")) createTruckShareArraysWeight();
        else createTruckShareArraysUnit();
        distCounties = MatrixReader.readMatrix(new File(rb.getString("county.distance.in.miles")), "Distance");

        getUScountyEmployment(rb);

        if (readFAF2.domRegionList == null) readFAF2.readFAF2ReferenceLists(rb);
        //create HashMap that contains for every FAF region a TableDataSet of counties with their employment share
        countyShares = new HashMap<>();
        String[] direction = {"orig", "dest"};
        for (String dir: direction) {
            HashMap<String, Float> factors;
            if (dir.equals("orig")) factors = makeC;
            else factors = useC;
            for (int fafNum = 1; fafNum <= readFAF2.domRegionList.getRowCount(); fafNum++) {
                for (String com: readFAF2.sctgCommodities) {
                    TableDataSet CountyList = getCountySpecificDataByFAF2(fafNum, com, detailEmployment, factors);
                    String code = dir + "_" + readFAF2.domRegionList.getStringValueAt(fafNum, "RegionName") + "_" + com;
                    countyShares.put(code, CountyList);
                }
            }
        }
    }


    private void createTruckShareArraysWeight() {
        // create array with distance bins and shares or medium-heavy trucks
        truckTypeShareDistBin = new int[truckTypeShares.getRowCount()];
        truckTypeShareMT = new float[truckTypeShares.getRowCount()];
        for (int row = 1; row <= truckTypeShares.getRowCount(); row++) {
            truckTypeShareDistBin[row - 1] = (int) truckTypeShares.getValueAt(row, "DistanceGreaterThan");
            truckTypeShareMT[row - 1] = truckTypeShares.getValueAt(row, "MT(<26k_lbs)");
            // data quality check
            float htShare = truckTypeShares.getValueAt(row, "HT(>26k_lbs)");
            if (htShare + truckTypeShareMT[row - 1] != 1) logger.warn("Shares of Medium and Heavy Trucks add up to " +
                    htShare + truckTypeShareMT[row - 1] + " instead of 1 for distance class " + truckTypeShareDistBin[row - 1]);
        }
    }


    private void createTruckShareArraysUnit() {
        // create array with distance bins and shares or Single-Unit/Multi-Unit trucks
        truckTypeShareDistBin = new int[truckTypeShares.getRowCount()];
        truckTypeShareMT = new float[truckTypeShares.getRowCount()];
        for (int row = 1; row <= truckTypeShares.getRowCount(); row++) {
            truckTypeShareDistBin[row - 1] = (int) truckTypeShares.getValueAt(row, "DistanceGreaterThan");
            truckTypeShareMT[row - 1] = truckTypeShares.getValueAt(row, "SUT");
            // data quality check
            float htShare = truckTypeShares.getValueAt(row, "MUT");
            if (htShare + truckTypeShareMT[row - 1] != 1) logger.warn("Shares of SUT and MUT Trucks add up to " +
                    htShare + truckTypeShareMT[row - 1] + " instead of 1 for distance class " + truckTypeShareDistBin[row - 1]);
        }
    }


    public void prepareCountyDataForFAF(ResourceBundle rb, int yr, TableDataSet detailEmployment,
                                        TableDataSet specialRegions, int fafVersion) {
        // prepare county data to provide detailed employment given in detailEmployment and total employment elsewhere
        // as a weight using the "fafVersion" zone system
        logger.info("Preparing county data for FAF" + fafVersion + " flow disaggregation for year " + yr + "...");

        String useToken = "faf" + fafVersion + ".use.coefficients";
        String makeToken = "faf" + fafVersion + ".make.coefficients";
        HashMap<String, Float> useC = createMakeUseHashMap(rb, useToken);
        HashMap<String, Float> makeC = createMakeUseHashMap(rb, makeToken);

        distCounties = MatrixReader.readMatrix(new File(rb.getString("county.distance.in.miles")), "Distance");

        if (fafVersion == 2 && readFAF2.domRegionList == null) readFAF2.readFAF2ReferenceLists(rb);
//        if (fafVersion == 3 && readFAF3.fafRegionList == null) readFAF3.readFAF3referenceLists(rb);  // has been read earlier for FAF3

        //create HashMap that contains for every FAF region a TableDataSet of counties with their employment share
        countyShares = new HashMap<>();
        String[] direction = {"orig", "dest"};
        for (String dir: direction) {
            HashMap<String, Float> factors;
            if (dir.equals("orig")) factors = makeC;
            else factors = useC;

            if (fafVersion == 2) {
                for (int fafNum = 1; fafNum <= readFAF2.domRegionList.getRowCount(); fafNum++) {
                    for (String com: readFAF2.sctgCommodities) {
                        TableDataSet CountyList = getCountySpecificDataByFAF2(fafNum, com, detailEmployment, factors);
                        String code = dir + "_" + readFAF2.domRegionList.getStringValueAt(fafNum, "RegionName") + "_" + com;
                        countyShares.put(code, CountyList);
                    }
                }
            } else if (fafVersion==3){  // fafVersion == 3
                for (int fafNum = 1; fafNum <= readFAF3.fafRegionList.getRowCount(); fafNum++) {
                    for (String com: readFAF3.sctgStringCommodities) {
                        int zoneNum = (int) readFAF3.fafRegionList.getValueAt(fafNum, "ZoneID");
                        TableDataSet CountyList = getCountySpecificDataByFAF(zoneNum, com, detailEmployment, factors);
                        String code = dir + "_" + zoneNum + "_" + com;
                        countyShares.put(code, CountyList);
                    }
                }
            }
            else{  // fafVersion == 4
                for (int fafNum = 1; fafNum <= ReadFAF4.fafRegionList.getRowCount(); fafNum++) {
                    for (String com: ReadFAF4.sctgStringCommodities) {
                        int zoneNum = (int) ReadFAF4.fafRegionList.getValueAt(fafNum, "ZoneID");
                        TableDataSet CountyList = getCountySpecificDataByFAF(zoneNum, com, detailEmployment, factors);
                        String code = dir + "_" + zoneNum + "_" + com;
                        countyShares.put(code, CountyList);
                    }
                }
            }
        }

        // create fake county lists for special regions such as airports or seaports that shall be kept separate from the FAF regions
        if (specialRegions == null) return;
        for (int row = 1; row <= specialRegions.getRowCount(); row++) {
            int[] fips = {(int) specialRegions.getValueAt(row, "modelCode")};
            String[] name;
            if (fafVersion == 2) {
                name = new String[]{specialRegions.getStringValueAt(row, "Region")};
            }else if(fafVersion==3){
                name = new String[]{specialRegions.getStringValueAt(row, "faf3code")};
            }else{
            	name = new String[]{specialRegions.getStringValueAt(row, "faf4code")};
            }
            	
            TableDataSet CountyList = new TableDataSet();
            float[] emplDummy = {1};
            CountyList.appendColumn(name, "Name");
            CountyList.appendColumn(fips, "COUNTYFIPS");
            CountyList.appendColumn(name, "FAFRegion");
            CountyList.appendColumn(emplDummy, "Employment");
            for (String dir: direction) {
                if (fafVersion == 2) {
                    for (String com: readFAF2.sctgCommodities) {
                        String code = dir + "_" + name[0] + "_" + com;
                        countyShares.put(code, CountyList);
                    }
                } else if (fafVersion == 3){
                    for (String com: readFAF3.sctgStringCommodities) {
                        String code = dir + "_" + fips[0] + "_" + com;
                        countyShares.put(code, CountyList);
                    }
                }else{
                    for (String com: ReadFAF4.sctgStringCommodities) {
                        String code = dir + "_" + fips[0] + "_" + com;
                        countyShares.put(code, CountyList);
                    }
             	
                }
                	
            }
        }
    }


    public static HashMap<String, Float> createMakeUseHashMap (ResourceBundle rb, String token) {
        // create HashMap with make/use coefficients
        TableDataSet coeff = fafUtils.importTable(ResourceUtil.getProperty(rb, token));
        HashMap<String, Float> hsm = new HashMap<>();
        for (int i = 1; i <= coeff.getRowCount(); i++) {
            String industry = coeff.getStringValueAt(i, "Industry");
            for (int j = 2; j <= coeff.getColumnCount(); j++) {
                String sctg = coeff.getColumnLabel(j);
                String code = industry + "_" + sctg;
                hsm.put(code, coeff.getValueAt(i, j));
            }
        }
        return hsm;
    }


    public TableDataSet disaggregateSingleFAF2flow(String orig, String dest, String sctg, float tons) {
        // disaggregate tons from orig FAFzone to dest FAFzone to the county level for FAF2

        TableDataSet flows = new TableDataSet();
        // get county specific data for origin FAF and destination FAF
        TableDataSet CountyDatI = getCountyTableDataSet("orig", orig, sctg);
        TableDataSet CountyDatJ = getCountyTableDataSet("dest", dest, sctg);

        // walk through every county combination ic/jc within current FAF Region combination origFaf/destFaf
        double EmplICplusJCtotal = 0;
        int count = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                EmplICplusJCtotal += CountyDatI.getValueAt(origCounty, "Employment") +
                        CountyDatJ.getValueAt(destCounty, "Employment");
                count++;
            }
        }
        int[] oFips = new int[count];
        int[] dFips = new int[count];
        String[] codes = new String[count];
        float[] tonShare = new float[count];
        int k = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            int origFips = (int) CountyDatI.getValueAt(origCounty, "COUNTYFIPS");
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                int destFips = (int) CountyDatJ.getValueAt(destCounty, "COUNTYFIPS");
                double EmplICplusJC = CountyDatI.getValueAt(origCounty, "Employment") +
                        CountyDatJ.getValueAt(destCounty, "Employment");
                tonShare[k] = (float) (tons * EmplICplusJC / EmplICplusJCtotal);
                oFips[k] = origFips;
                dFips[k] = destFips;
                codes[k] = String.valueOf(origFips) + "_" + String.valueOf(destFips);
                k++;
            }
        }
        flows.appendColumn(oFips, "oFips");
        flows.appendColumn(dFips, "dFips");
        flows.appendColumn(codes, "Codes");
        flows.appendColumn(tonShare, "Tons");
        return flows;
    }


    public TableDataSet disaggregateSingleFAFFlow(int orig, int dest, String sctg, float tons, float centerDamper) {
        // disaggregate tons from orig FAFzone to dest FAFzone to the county level for FAF3

        TableDataSet flows = new TableDataSet();
        // get county specific data for origin FAF and destination FAF

        TableDataSet CountyDatI = getCountyWeights("orig", orig, sctg);
        TableDataSet CountyDatJ = getCountyWeights("dest", dest, sctg);
        if (CountyDatI == null) {
            logger.error("Could not find table for disaggregating county " + orig + " with commodity " + sctg);
            return null;
        }
        if (CountyDatJ == null) {
            logger.error("Could not find table for disaggregating county " + dest + " with commodity " + sctg);
            return null;
        }
        // walk through every county combination ic/jc within current FAF Region combination origFaf/destFaf
        double EmplICplusJCtotal = 0;
        int count = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                EmplICplusJCtotal += Math.pow(CountyDatI.getValueAt(origCounty, "Employment") *
                        CountyDatJ.getValueAt(destCounty, "Employment"), centerDamper);
                count++;
            }
        }
        int[] oFips = new int[count];
        int[] dFips = new int[count];
        String[] codes = new String[count];
        float[] tonShare = new float[count];
        int k = 0;

        if (EmplICplusJCtotal == 0) logger.error("Could not find weight for FAF zone " + orig + " to " + dest + " for " + sctg);

        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            int origFips = (int) CountyDatI.getValueAt(origCounty, "COUNTYFIPS");
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                int destFips = (int) CountyDatJ.getValueAt(destCounty, "COUNTYFIPS");
                double EmplICplusJC = Math.pow(CountyDatI.getValueAt(origCounty, "Employment") *
                        CountyDatJ.getValueAt(destCounty, "Employment"), centerDamper);
                tonShare[k] = (float) (tons * EmplICplusJC / EmplICplusJCtotal);

                oFips[k] = origFips;
                dFips[k] = destFips;
                codes[k] = origFips + "_" + destFips;
                k++;
            }
        }

        flows.appendColumn(oFips, "oFips");
        flows.appendColumn(dFips, "dFips");
        flows.appendColumn(codes, "Codes");
        flows.appendColumn(tonShare, "Tons");
        return flows;
    }


    public TableDataSet disaggregateSingleFAFFlowThroughPOE(String dir, TableDataSet poe, int orig, int dest, String sctg,
                                                             float tons, float centerDamper) {
        // disaggregate tons from orig FAFzone to dest FAFzone to the county level for FAF3 for import/exports through ports
        // of entry/exit

        // get county specific data for origin FAF and destination FAF
        TableDataSet CountyDatI = getCountyWeights("orig", orig, sctg);
        if (dir.startsWith("import") && poe != null) CountyDatI = poe;
        TableDataSet CountyDatJ = getCountyWeights("dest", dest, sctg);
        if (dir.startsWith("export") && poe != null) CountyDatJ = poe;
        if (CountyDatI == null) {
            logger.error("Could not find table for " + dir + " disaggregating county " + orig + " with commodity " + sctg);
            return null;
        }
        if (CountyDatJ == null) {
            logger.error("Could not find table for " + dir + " disaggregating county " + dest + " with commodity " + sctg);
            return null;
        }
        // walk through every county combination ic/jc within current FAF Region combination origFaf/destFaf
        double EmplICplusJCtotal = 0;
        int count = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                EmplICplusJCtotal += Math.pow(CountyDatI.getValueAt(origCounty, "Employment") *
                        CountyDatJ.getValueAt(destCounty, "Employment"), centerDamper);
                count++;
            }
        }
        int[] oFips = new int[count];
        int[] dFips = new int[count];
        String[] codes = new String[count];
        float[] tonShare = new float[count];
        int k = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            int origFips = (int) CountyDatI.getValueAt(origCounty, "COUNTYFIPS");
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                int destFips = (int) CountyDatJ.getValueAt(destCounty, "COUNTYFIPS");
                double EmplICplusJC = Math.pow(CountyDatI.getValueAt(origCounty, "Employment") *
                        CountyDatJ.getValueAt(destCounty, "Employment"), centerDamper);
                tonShare[k] = (float) (tons * EmplICplusJC / EmplICplusJCtotal);
                oFips[k] = origFips;
                dFips[k] = destFips;
                codes[k] = origFips + "_" + destFips;
                k++;
            }
        }
        TableDataSet flows = new TableDataSet();
        flows.appendColumn(oFips, "oFips");
        flows.appendColumn(dFips, "dFips");
        flows.appendColumn(codes, "Codes");
        flows.appendColumn(tonShare, "Tons");
        return flows;
    }


    public float getCountyDistance(int origFips, int destFips) {
        // return distance from origFips to destFips

        try {
            return distCounties.getValueAt(origFips, destFips);
        } catch (Exception e){
            return -1;
        }
    }


    public float[] splitToTruckTypes(int origFips, int destFips, float trucks) {
        // split trucks for this OD pair into two truck types
        float dist;
        try {
            dist = distCounties.getValueAt(origFips, destFips);
        } catch (Exception e) {
            dist = 999;
        }
        float[] splitTrucks = {0, 0};
        for (int dClass = truckTypeShareDistBin.length - 1; dClass >= 0; dClass--) {
            if (dist >= truckTypeShareDistBin[dClass]) {
                splitTrucks[0] = trucks * truckTypeShareMT[dClass];
                splitTrucks[1] = trucks * (1 - truckTypeShareMT[dClass]);
                return splitTrucks;                                          }
        }
        logger.warn("Could not find truck share for distance " + dist);
        return null;
    }


    public double[] splitToTruckTypes(int origFips, int destFips, double trucks) {
        // split trucks for this OD pair into two truck types
        int dist;
        try {
            dist = (int) distCounties.getValueAt(origFips, destFips);
            if (dist == -999) dist = 9999;   // destination unreachable on network, therefore skim is negative, but there might be trips in reality, including ferry 
        } catch (Exception e) {
            dist = 999;
        }
        double[] splitTrucks = {0, 0};
        for (int dClass = truckTypeShareDistBin.length - 1; dClass >= 0; dClass--)
            if (dist >= truckTypeShareDistBin[dClass]) {
                splitTrucks[0] = trucks * truckTypeShareMT[dClass];
                splitTrucks[1] = trucks * (1 - truckTypeShareMT[dClass]);
                return splitTrucks;
            }
        logger.warn("Could not find truck share for distance " + dist);
        return null;
    }


    public double[] splitToTruckTypes(int origFips, int destFips) {
        // split trucks for this OD pair into two truck types
        int dist;
        try {
            dist = (int) distCounties.getValueAt(origFips, destFips);
            if (dist == -999) dist = 9999;   // destination unreachable on network, therefore skim is negative, but there might be trips in reality, including ferry
        } catch (Exception e) {
            dist = 999;
        }
        double[] splitTrucks = {0, 0};
        for (int dClass = truckTypeShareDistBin.length - 1; dClass >= 0; dClass--)
            if (dist >= truckTypeShareDistBin[dClass]) {
                splitTrucks[0] = truckTypeShareMT[dClass];
                splitTrucks[1] = (1 - truckTypeShareMT[dClass]);
                return splitTrucks;
            }
        logger.warn("Could not find truck share for distance " + dist);
        return null;
    }


    public double[] getTrucksByType(float dist, double sutPL, double mutPL, double tonFlow) {
        // convert tons into trucks and split trucks into single-unit and multi-unit trucks

        double[] truckShares = splitToTruckTypes(dist);
        double[] trucksByType = new double[2];
        trucksByType[0] = tonFlow / (sutPL + truckShares[1]/truckShares[0] * mutPL);
        trucksByType[1] = tonFlow / (truckShares[0]/truckShares[1] * sutPL + mutPL);
        return trucksByType;
    }


    public double[] getTrucksByType(float dist, double sutPL, double mutPL, double tonFlow, float adjustmentSUT) {
        // convert tons into trucks and split trucks into single-unit and multi-unit trucks

        double[] truckShares = splitToTruckTypes(dist);
        truckShares[0] = truckShares[0] * (1. + adjustmentSUT);
        truckShares[1] = 1. - truckShares[0];
        double[] trucksByType = new double[2];
        trucksByType[0] = tonFlow / (sutPL + truckShares[1]/truckShares[0] * mutPL);
        trucksByType[1] = tonFlow / (truckShares[0]/truckShares[1] * sutPL + mutPL);
        return trucksByType;
    }


    public double[] splitToTruckTypes(float distance) {
        // split trucks for this OD pair into two truck types
        double[] splitTrucks = {0, 0};
        for (int dClass = truckTypeShareDistBin.length - 1; dClass >= 0; dClass--)
            if (distance >= truckTypeShareDistBin[dClass]) {
                splitTrucks[0] = truckTypeShareMT[dClass];
                splitTrucks[1] = (1 - truckTypeShareMT[dClass]);
                return splitTrucks;
            }
        logger.warn("Could not find truck share for distance " + distance);
        return null;
    }


    public float[] splitToTruckTypes(String code, float trucks) {
        // split trucks for this OD pair into two truck types
        String[] origDestFips = code.split("_");
        int origFips = Integer.parseInt(origDestFips[0]);
        int destFips = Integer.parseInt(origDestFips[1]);
        float dist;
        try {
            dist = distCounties.getValueAt(origFips, destFips);
        } catch (Exception e) {
            dist = 1;
        }
        int distClass = 1;
        for (int row = 1; row <= truckTypeShares.getRowCount(); row++)
            if (dist > truckTypeShares.getValueAt(row, "DistanceGreaterThan")) distClass = row;
        float[] splitTrucks = {0, 0};
        splitTrucks[0] = trucks * truckTypeShares.getValueAt(distClass, "MT(<26k_lbs)");
        splitTrucks[1] = trucks * truckTypeShares.getValueAt(distClass, "HT(>26k_lbs)");
        return splitTrucks;
    }


    private TableDataSet getCountyTableDataSet(String direction, String FAFregion, String sctgCode) {
        // check if this FAFregion equals a special generator
        TableDataSet tbl;
        if (FAFregion.equals("OR Portl_Airport") || FAFregion.equals("OR Portl_Port") ||
                FAFregion.equals("OR rem_Airport") || FAFregion.equals("OR rem_Port") || FAFregion.equals("Other")) {
            tbl = new TableDataSet();
            String[] a = {FAFregion};
            float[] b = new float[1];
            if (FAFregion.equals("OR Portl_Airport")) b[0] = 90001f;
            else if (FAFregion.equals("OR Portl_Port")) b[0] = 90002f;
            else if (FAFregion.equals("OR rem_Airport")) b[0] = 90003f;
            else if (FAFregion.equals("OR rem_Port")) b[0] = 90004f;
            else if (FAFregion.equals("Other")) b[0] = 90005f;
            float[] c = {1f};
            tbl.appendColumn(a, "Name");
            tbl.appendColumn(b, "COUNTYFIPS");
            tbl.appendColumn(a, "FAFRegion");
            tbl.appendColumn(c, "Employment");
            tbl.setName(FAFregion);
        } else {
            String code = direction + "_" + FAFregion + "_" + sctgCode;
            tbl = countyShares.get(code);
        }
        return tbl;
    }


    public TableDataSet getCountyWeights(String direction, int FAFregion, String sctgCode) {
        // check if this FAFregion equals a special generator
        String code = direction + "_" + FAFregion + "_" + sctgCode;
        return countyShares.get(code);
    }


    public TableDataSet disaggregateSingleFlowInOregon(String orig, String dest, String sctg, float trucks) {
        // disaggregate trucks from orig FAFzone to dest FAFzone to the county level

        if (!orig.contains("OR Portl") && !orig.contains("OR rem")) orig = "Other";
        if (!dest.contains("OR Portl") && !dest.contains("OR rem")) dest = "Other";
        TableDataSet flows = new TableDataSet();
        // get county specific data for origin FAF and destination FAF
        TableDataSet CountyDatI = getCountyTableDataSet("orig", orig, sctg);
        TableDataSet CountyDatJ = getCountyTableDataSet("dest", dest, sctg);

        // walk through every county combination ic/jc within current FAF Region combination origFaf/destFaf
        double EmplICplusJCtotal = 0;
        int count = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                EmplICplusJCtotal += CountyDatI.getValueAt(origCounty, "Employment") +
                        CountyDatJ.getValueAt(destCounty, "Employment");
                count++;
            }
        }
        String[] codes = new String[count];
        float[] tonShare = new float[count];
        int k = 0;
        for (int origCounty = 1; origCounty <= CountyDatI.getRowCount(); origCounty++) {
            int origFips = (int) CountyDatI.getValueAt(origCounty, "COUNTYFIPS");
            for (int destCounty = 1; destCounty <= CountyDatJ.getRowCount(); destCounty++) {
                int destFips = (int) CountyDatJ.getValueAt(destCounty, "COUNTYFIPS");
                double EmplICplusJC = CountyDatI.getValueAt(origCounty, "Employment") +
                        CountyDatJ.getValueAt(destCounty, "Employment");
                tonShare[k] = (float) (trucks * EmplICplusJC / EmplICplusJCtotal);
                codes[k] = String.valueOf(origFips) + "_" + String.valueOf(destFips);
                k++;
            }
        }
        flows.appendColumn(codes, "Codes");
        flows.appendColumn(tonShare, "ShortTons");
        return flows;
    }


    public static void getUScountyEmployment(ResourceBundle rb) {
        // read employment and county id data

        logger.info("Reading County Employment Data...");
        countyIDsWithEmployment = fafUtils.importTable(ResourceUtil.getProperty(rb, "county.ID"));

        TableDataSet StatesTable = fafUtils.importTable(ResourceUtil.getProperty(rb, "state.list"));
        String[] StateNames = StatesTable.getColumnAsString("StateName");
        TableDataSet[] StateEmployment = new TableDataSet[StateNames.length];
        for (int st = 0; st < StateNames.length; st++) {
            String fileName = ResourceUtil.getProperty(rb, "state.employment.prefix") + StateNames[st] + ".csv";
            StateEmployment[st] = fafUtils.importTable(fileName);
            StateEmployment[st].setName(StateNames[st]);
        }

        String[] tempString = new String[countyIDsWithEmployment.getRowCount()];
        int[] tempInt = new int[countyIDsWithEmployment.getRowCount()];
        countyIDsWithEmployment.appendColumn(tempString, "stateName");
        countyIDsWithEmployment.appendColumn(tempInt, "Employment");

        // assign employment to every county
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (!countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("County") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Legal County Equivalent")&&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Independent City") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Borough")  &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Census Area") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("City and Borough") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Parish")) continue;
            String CountyStateAbbreviation = countyIDsWithEmployment.getStringValueAt(i, "StateCode");
            String CountyName = countyIDsWithEmployment.getStringValueAt(i, "NAME");

            // find full state name
            String StateNameOfCounty = " ";
            for (int j = 1; j <= StatesTable.getRowCount(); j++)
                if (StatesTable.getStringValueAt(j, "StateAbbreviation").equals(CountyStateAbbreviation))
                    StateNameOfCounty = StatesTable.getStringValueAt(j, "stateName");

            // find right employment table for this state
            int StateNumber = 0;
            for (int j = 0; j < StateEmployment.length; j++) {
                if (StateEmployment[j].getName().equals(StateNameOfCounty)) StateNumber = j;
            }

            // delete state code from county name
            StringBuffer sb = new StringBuffer();
            sb.append(CountyName);
            int n = sb.lastIndexOf(CountyStateAbbreviation);
            sb.delete(n-1, n+2);
            // add ", StateAbbreviation"
            sb.append(", ");
            sb.append(CountyStateAbbreviation);
            CountyName = sb.toString();

            // find employment of current county
            boolean found = false;
            for (int k = 1; k <= StateEmployment[StateNumber].getRowCount(); k++) {
                if (StateEmployment[StateNumber].getStringValueAt(k, "Region").equalsIgnoreCase(CountyName)) {
                    countyIDsWithEmployment.setValueAt
                            (i, "Employment", StateEmployment[StateNumber].getValueAt(k, "Employment"));
                    countyIDsWithEmployment.setStringValueAt
                            (i, countyIDsWithEmployment.getColumnPosition("stateName"), StateNameOfCounty);
                    found = true;
                }
            }

            // County Broomfield, Colorado has no polygon in the county layer. Employment is added to Boulder County:
            if (CountyName.equals("BOULDER, CO")) {
                for (int k = 1; k <= StateEmployment[StateNumber].getRowCount(); k++) {
                    if (StateEmployment[StateNumber].getStringValueAt(k, "Region").equals("Broomfield, CO")) {
                        float BoulderBroomfieldEmpl = countyIDsWithEmployment.getValueAt(i, "Employment") +
                                StateEmployment[StateNumber].getValueAt(k, "Employment");
                        countyIDsWithEmployment.setValueAt(i, "Employment", BoulderBroomfieldEmpl);
                    }
                }
            }

            // write error message (unless it is an island of UM or Guam)
            if (!found && !CountyStateAbbreviation.equals("UM") && !CountyStateAbbreviation.equals("GU"))
                logger.warn("Not found: " + CountyName + " (" +
                        countyIDsWithEmployment.getStringValueAt(i, "TYPE") + ")");
        }
    }


    public static void getUScountyEmploymentFromOneFile (ResourceBundle rb) {
        // read file with county employment in the US

        logger.info("Reading County Employment Data...");
        countyIDsWithEmployment = fafUtils.importTable(ResourceUtil.getProperty(rb, "county.ID"));
        TableDataSet countyEmployment = fafUtils.importTable(ResourceUtil.getProperty(rb, "us.county.employment"));

        String[] tempString = new String[countyIDsWithEmployment.getRowCount()];
        int[] tempInt = new int[countyIDsWithEmployment.getRowCount()];
        countyIDsWithEmployment.appendColumn(tempString, "stateName");
        countyIDsWithEmployment.appendColumn(tempInt, "Employment");

        // assign employment to every county
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (!countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("County") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Legal County Equivalent")&&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Independent City") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Borough")  &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Census Area") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("City and Borough") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Parish")) continue;
            // skip Guam and United States Minor Outlying Islands
            if (countyIDsWithEmployment.getStringValueAt(i, "StateCode").equals("GU") ||
                    countyIDsWithEmployment.getStringValueAt(i, "StateCode").equals("UM")) continue;
            float fips = countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
            // find employment of current county
            boolean found = false;
            for (int row = 1; row <= countyEmployment.getRowCount(); row++) {
                if (countyEmployment.getValueAt(row, "fips") == fips) {
                    countyIDsWithEmployment.setValueAt
                            (i, "Employment", countyEmployment.getValueAt(row, "totalAnnualAverageEmployment"));
                    found = true;
                }
            }

            // write error message if county was not found
            if (!found) {
                String CountyName = countyIDsWithEmployment.getStringValueAt(i, "NAME");
                logger.warn("Not found: " + CountyName + " (" +
                        countyIDsWithEmployment.getStringValueAt(i, "TYPE") + ")");
            }
        }
    }


    public void getUScountyEmploymentByIndustry (ResourceBundle rb) {
        // read file with employment by county by industry

        logger.info("  Reading County Employment Data...");
        countyIDsWithEmployment = fafUtils.importTable(ResourceUtil.getProperty(rb, "county.ID"));
        TableDataSet countyEmployment = fafUtils.importTable(ResourceUtil.getProperty(rb, "us.county.employment.by.ind"));
        countyEmployment.buildIndex(countyEmployment.getColumnPosition("FIPS"));
        TableDataSet agEmpl = fafUtils.importTable(rb.getString("us.county.employment.agricult"));
        agEmpl.buildIndex(agEmpl.getColumnPosition("FIPS"));

        String[] tempString = new String[countyIDsWithEmployment.getRowCount()];
        int[] tempInt = new int[countyIDsWithEmployment.getRowCount()];
        countyIDsWithEmployment.appendColumn(tempString, "stateName");
        for (String emp: empCats) countyIDsWithEmployment.appendColumn(tempInt, emp);

        // assign employment to every county
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (!countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("County") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Legal County Equivalent")&&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Independent City") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Borough")  &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Census Area") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("City and Borough") &&
                    !countyIDsWithEmployment.getStringValueAt(i, "TYPE").equals("Parish")) continue;
            // skip Guam and United States Minor Outlying Islands
            if (countyIDsWithEmployment.getStringValueAt(i, "StateCode").equals("GU") ||
                    countyIDsWithEmployment.getStringValueAt(i, "StateCode").equals("UM")) continue;
            int fips = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
            // find employment of current county
            float consNatResMinEmployment = countyEmployment.getIndexedValueAt(fips, "Natural Resources and Mining") +
                    countyEmployment.getIndexedValueAt(fips, "Construction");
            countyIDsWithEmployment.setValueAt(i, "Construction Natural Resources and Mining", consNatResMinEmployment);
            countyIDsWithEmployment.setValueAt(i, "Manufacturing", countyEmployment.getIndexedValueAt(fips, "Manufacturing"));
            countyIDsWithEmployment.setValueAt(i, "Trade Transportation and Utilities", countyEmployment.getIndexedValueAt(fips, "Trade, Transportation, and Utilities"));
            countyIDsWithEmployment.setValueAt(i, "Information", countyEmployment.getIndexedValueAt(fips, "Information"));
            countyIDsWithEmployment.setValueAt(i, "Financial Activities", countyEmployment.getIndexedValueAt(fips, "Financial Activities"));
            countyIDsWithEmployment.setValueAt(i, "Professional and Business Services", countyEmployment.getIndexedValueAt(fips, "Professional and Business Services"));
            countyIDsWithEmployment.setValueAt(i, "Education and Health Services", countyEmployment.getIndexedValueAt(fips, "Education and Health Services"));
            countyIDsWithEmployment.setValueAt(i, "Leisure and Hospitality", countyEmployment.getIndexedValueAt(fips, "Leisure and Hospitality"));
            countyIDsWithEmployment.setValueAt(i, "Other Services", countyEmployment.getIndexedValueAt(fips, "Other Services"));

            // agricultural employment is missing in most of Alaska and in independent cities (deemed not to be relevant)
            try {
                countyIDsWithEmployment.setValueAt(i, "Agriculture", agEmpl.getIndexedValueAt(fips, "agEmployment"));
            } catch (Exception e) {
                // Set to minor value to ensure that all FAF ag production can be allocated. If the FAF zone of this
                // county has other counties with ag production, this miniWeight will be irrelevant as it is small.
                // If no county in this FAF zone has ag production, the employment "Construction Natural Resources and Mining"
                // will be used to disaggregate flows (with all counties being multiplied by 0.0001, which doesn't affect the result.
                float miniWeight = 0.0001f * countyIDsWithEmployment.getValueAt(i, "Construction Natural Resources and Mining");
                countyIDsWithEmployment.setValueAt(i, "Agriculture", miniWeight);

            }
        }
        // try to add coal production as a weight
        try {
            TableDataSet coalProd = fafUtils.importTable(rb.getString("mining.production"));
            coalProd.buildIndex(coalProd.getColumnPosition("FIPS"));
            countyIDsWithEmployment.appendColumn(tempInt, "coalProduction");
            for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
                int fips = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
                try {
                    countyIDsWithEmployment.setValueAt(i, "coalProduction", coalProd.getIndexedValueAt(fips, "Production_total"));
                } catch (Exception e) {
                    // Set to minor value to ensure that all FAF coal production can be allocated. If the FAF zone of this
                    // county has other counties with coal production, this miniWeight will be irrelevant as it is small.
                    // If no county in this FAF zone has coal production, the employment "Construction Natural Resources and Mining"
                    // will be used to disaggregate flows (with all counties being multiplied by 0.0001, which doesn't affect the result.
                    float miniWeight = 0.0001f * countyIDsWithEmployment.getValueAt(i, "Construction Natural Resources and Mining");
                    countyIDsWithEmployment.setValueAt(i, "coalProduction", miniWeight);
                }
            }
        } catch (Exception e) {
            logger.warn("Coal production not defined.");
        }
        countyFips = countyIDsWithEmployment.getColumnAsInt(
                countyIDsWithEmployment.getColumnPosition("COUNTYFIPS"));
    }


    public int[] getCountyFips () {
        return countyFips;
    }


    public int getStateNumberOfCounty (int fips) {
        for (int row = 1; row <= countyIDsWithEmployment.getRowCount(); row ++) {
            if (countyIDsWithEmployment.getValueAt(row, "COUNTYFIPS") == fips) return (int) countyIDsWithEmployment.getValueAt(row, "State");
        }
        logger.warn ("State of county " + fips + " was not found.");
        return -1;
    }


    public void defineTruckTypes (String truckType, ResourceBundle rb) {
        // define truck types by weight or size

        truckTypeShares = fafUtils.importTable(rb.getString("truck.type.by.distance"));
        if (truckType.equalsIgnoreCase("weight")) createTruckShareArraysWeight();
        else createTruckShareArraysUnit();
    }


    private TableDataSet getCountySpecificDataByFAF2(int FAFNumber, String com, TableDataSet detEmpl,
                                                     HashMap<String, Float> factors) {
        // get employment as a weight for FAF zone FAFNumber in FAF2
        // where detailed employment is available in detEmpl, use make/use factors for commodity-specific weights

        TableDataSet CountySpecifics = new TableDataSet();
        String nameOfFAF = readFAF2.domRegionList.getStringValueAt(FAFNumber, "RegionName");
        CountySpecifics.setName(nameOfFAF);

        int NoCountiesInFAF = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getStringValueAt(i, "FafRegion").equals(nameOfFAF)) NoCountiesInFAF += 1;
        }
        String[] CountyName = new String[NoCountiesInFAF];
        int[] CountyFips = new int[NoCountiesInFAF];
        String[] CountyFAFRegion = new String[NoCountiesInFAF];
        float[] countyEmpl = new float[NoCountiesInFAF];
        boolean hasDetailedEmployment = checkIfFAFzoneHasDetailedEmployment(nameOfFAF, NoCountiesInFAF, detEmpl, 2);

        int k = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getStringValueAt(i, "FafRegion").equals(nameOfFAF)){
                CountyName[k] = countyIDsWithEmployment.getStringValueAt(i, "NAME");
                CountyFips[k] = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
                CountyFAFRegion[k] = countyIDsWithEmployment.getStringValueAt(i, "FafRegion");
                if (hasDetailedEmployment) {
                    for (int row = 1; row <= detEmpl.getRowCount(); row++)
                        if (detEmpl.getValueAt(row, "CountyFips") == CountyFips[k])
                            countyEmpl[k] = getWeightedEmpl(row, com, detEmpl, factors);
                } else {
                    countyEmpl[k] = countyIDsWithEmployment.getValueAt(i, "Employment");
                }
                k++;
            }
        }
        CountySpecifics.appendColumn(CountyName, "Name");
        CountySpecifics.appendColumn(CountyFips, "COUNTYFIPS");
        CountySpecifics.appendColumn(CountyFAFRegion, "FAFRegion");
        CountySpecifics.appendColumn(countyEmpl, "Employment");
        return CountySpecifics;
    }


    private TableDataSet getCountySpecificDataByFAF(int FAFNumber, String com, TableDataSet detEmployment,
                                                     HashMap<String, Float> factors) {
    	
    	String fieldName = "FAF3region";
    	if(FAFVersion==4)
    		fieldName = "FAF4region";
    	
        // get employment as a weight for FAF zone FAFNumber in FAF3
        // where detailed employment is available in detEmpl, use make/use factors for commodity-specific weights

        if (FAFNumber > 800) {
            // foreign FAF zone
            TableDataSet foreignCountry = new TableDataSet();
            String[] countyName = {String.valueOf(FAFNumber)};
            int[] countyFips = {-1};
            String[] countyFAFRegion = {String.valueOf(FAFNumber)};
            int[] countyEmpl = {1};
            foreignCountry.appendColumn(countyName, "Name");
            foreignCountry.appendColumn(countyFips, "COUNTYFIPS");
            foreignCountry.appendColumn(countyFAFRegion, "FAFRegion");
            foreignCountry.appendColumn(countyEmpl, "Employment");
            return foreignCountry;
        }
        TableDataSet CountySpecifics = new TableDataSet();
        String nameOfFAF = null;
        if(FAFVersion==3)
        	nameOfFAF = readFAF3.getFAFzoneName(FAFNumber);
        else
        	nameOfFAF = ReadFAF4.getFAFzoneName(FAFNumber);
        CountySpecifics.setName(nameOfFAF);

        int NoCountiesInFAF = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber) NoCountiesInFAF += 1;
        }
        if (NoCountiesInFAF == 0) logger.warn("No counties for FAF zone " + FAFNumber + " have been found.");
        String[] countyName = new String[NoCountiesInFAF];
        int[] countyFips = new int[NoCountiesInFAF];
        String[] countyFAFRegion = new String[NoCountiesInFAF];
        float[] countyEmpl = new float[NoCountiesInFAF];
        boolean hasDetailedEmployment = checkIfFAFzoneHasDetailedEmployment(nameOfFAF, NoCountiesInFAF, detEmployment, 3);

        int k = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber){
                countyName[k] = countyIDsWithEmployment.getStringValueAt(i, "NAME");
                countyFips[k] = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
                countyFAFRegion[k] = countyIDsWithEmployment.getStringValueAt(i, fieldName);
                if (hasDetailedEmployment) {
                    for (int row = 1; row <= detEmployment.getRowCount(); row++)
                        if (detEmployment.getValueAt(row, "CountyFips") == countyFips[k])
                            countyEmpl[k] = getWeightedEmpl(row, com, detEmployment, factors);
                } else {
                    countyEmpl[k] = countyIDsWithEmployment.getValueAt(i, "Employment");
                }
                k++;
            }
        }
        CountySpecifics.appendColumn(countyName, "Name");
        CountySpecifics.appendColumn(countyFips, "COUNTYFIPS");
        CountySpecifics.appendColumn(countyFAFRegion, "FAFRegion");
        CountySpecifics.appendColumn(countyEmpl, "Employment");
        return CountySpecifics;
    }


    private TableDataSet getCountySpecificDataByFAFwithDetailedEmployment (int FAFNumber, String com,
                                                                            HashMap<String, Float> factors, float officeReduction) {
        // get employment as a weight for FAF zone FAFNumber in FAF3, use make/use factors for commodity-specific weights
    	String fieldName = "FAF3region";
    	if(FAFVersion==4)
    		fieldName = "FAF4region";

        if (FAFNumber > 800) {
            // foreign FAF zone
            TableDataSet foreignCountry = new TableDataSet();
            String[] countyName = {String.valueOf(FAFNumber)};
            int[] countyFips = {-1};
            String[] countyFAFRegion = {String.valueOf(FAFNumber)};
            int[] countyEmpl = {1};
            foreignCountry.appendColumn(countyName, "Name");
            foreignCountry.appendColumn(countyFips, "COUNTYFIPS");
            foreignCountry.appendColumn(countyFAFRegion, "FAFRegion");
            foreignCountry.appendColumn(countyEmpl, "Employment");
            return foreignCountry;
        }
        TableDataSet CountySpecifics = new TableDataSet();
        String nameOfFAF = null;
        
        if(FAFVersion==3)
        	nameOfFAF = readFAF3.getFAFzoneName(FAFNumber);
        else
        	nameOfFAF = ReadFAF4.getFAFzoneName(FAFNumber);

        CountySpecifics.setName(nameOfFAF);

        int NoCountiesInFAF = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber) NoCountiesInFAF += 1;
        }
        if (NoCountiesInFAF == 0) logger.warn("No counties for FAF zone " + FAFNumber + " have been found.");
        String[] countyName = new String[NoCountiesInFAF];
        int[] countyFips = new int[NoCountiesInFAF];
        String[] countyFAFRegion = new String[NoCountiesInFAF];
        float[] countyEmpl = new float[NoCountiesInFAF];

        int k = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber){
                countyName[k] = countyIDsWithEmployment.getStringValueAt(i, "NAME");
                countyFips[k] = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
                countyFAFRegion[k] = countyIDsWithEmployment.getStringValueAt(i, fieldName);
                countyEmpl[k] = getWeightedEmpl(i, com, factors, officeReduction);
//                if(countyFips[k]==37082 || countyFips[k]==37058 || countyFips[k]==37152 ){//|| destFips==37082 || destFips==37058 || destFips==37152){
//                	logger.info("FIPS: ("+countyFips[k]+") WeightedEmp: "+countyEmpl[k]);
//                }
                k++;
            }
        }
        CountySpecifics.appendColumn(countyName, "Name");
        CountySpecifics.appendColumn(countyFips, "COUNTYFIPS");
        CountySpecifics.appendColumn(countyFAFRegion, "FAFRegion");
        CountySpecifics.appendColumn(countyEmpl, "Employment");
        return CountySpecifics;
    }


    private TableDataSet getCountySpecificDataByFAFwithDetailedEmploymentByType (int FAFNumber, String com,
                                                                                  HashMap<String, Float> factors, float officeReduction) {
        // get employment as a weight for FAF zone FAFNumber in FAF3, use make/use factors for commodity-specific weights
        // keep track of single employment types

    	String fieldName = "FAF3region";
    	if(FAFVersion==4)
    		fieldName = "FAF4region";
    	
        if (FAFNumber > 800) {
            // foreign FAF zone
            TableDataSet foreignCountry = new TableDataSet();
            String[] countyName = {String.valueOf(FAFNumber)};
            int[] countyFips = {-1};
            String[] countyFAFRegion = {String.valueOf(FAFNumber)};
            int[] countyEmpl = {1};
            foreignCountry.appendColumn(countyName, "Name");
            foreignCountry.appendColumn(countyFips, "COUNTYFIPS");
            foreignCountry.appendColumn(countyFAFRegion, "FAFRegion");
            foreignCountry.appendColumn(countyEmpl, "Employment");
            return foreignCountry;
        }
        TableDataSet CountySpecifics = new TableDataSet();
        
        String nameOfFAF=null;
        if(FAFVersion==3)
        	nameOfFAF = readFAF3.getFAFzoneName(FAFNumber);
        else
        	nameOfFAF = ReadFAF4.getFAFzoneName(FAFNumber);

        CountySpecifics.setName(nameOfFAF);

        int NoCountiesInFAF = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber) NoCountiesInFAF += 1;
        }
        if (NoCountiesInFAF == 0) logger.warn("No counties for FAF zone " + FAFNumber + " have been found.");
        String[] countyName = new String[NoCountiesInFAF];
        int[] countyFips = new int[NoCountiesInFAF];
        String[] countyFAFRegion = new String[NoCountiesInFAF];
        float[][] countyEmpl = new float[NoCountiesInFAF][empCats.length+1];

        int k = 0;
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++) {
            if (countyIDsWithEmployment.getValueAt(i, fieldName) == FAFNumber){
                countyName[k] = countyIDsWithEmployment.getStringValueAt(i, "NAME");
                countyFips[k] = (int) countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS");
                countyFAFRegion[k] = countyIDsWithEmployment.getStringValueAt(i, fieldName);
                HashMap<String, Float> detEmpl = getWeightedEmplByEmpl(i, com, factors, officeReduction);
                countyEmpl[k][empCats.length] = detEmpl.get("total");
                for (int emp = 0; emp < empCats.length; emp++) countyEmpl[k][emp] = detEmpl.get(empCats[emp]);
                k++;
            }
        }
        CountySpecifics.appendColumn(countyName, "Name");
        CountySpecifics.appendColumn(countyFips, "COUNTYFIPS");
        CountySpecifics.appendColumn(countyFAFRegion, "FAFRegion");
        for (int emp = 0; emp <= empCats.length; emp++) {
            float[] copy = new float[NoCountiesInFAF];
            for (int i = 0; i < copy.length; i++) copy[i] = countyEmpl[i][emp];
            if (emp < empCats.length) CountySpecifics.appendColumn(copy, empCats[emp]);
            else CountySpecifics.appendColumn(copy, "Employment");
        }
        return CountySpecifics;
    }


    public void prepareCountyDataForFAFwithDetailedEmployment(ResourceBundle rb, int yr, TableDataSet specialRegions) {
        // prepare county data to provide detailed employment as a weight using the "fafVersion" zone system

        logger.info("Preparing county data with detailed employment for FAF flow disaggregation for year " + yr + "...");
        HashMap<String, Float> useC = createMakeUseHashMap(rb, "faf.use.coefficients");
        HashMap<String, Float> makeC = createMakeUseHashMap(rb, "faf.make.coefficients");
        distCounties = MatrixReader.readMatrix(new File(rb.getString("county.distance.in.miles")), "Distance");
        float reduction = (float) ResourceUtil.getDoubleProperty(rb, "reduction.local.office.weight", 1d);

        //create HashMap that contains for every FAF region a TableDataSet of counties with their employment share
        countyShares = new HashMap<>();
        String[] direction = {"orig", "dest"};
        for (String dir: direction) {
            HashMap<String, Float> factors;
            if (dir.equals("orig")) factors = makeC;
            else factors = useC;

            for (int fafNum = 1; fafNum <= readFAF3.fafRegionList.getRowCount(); fafNum++) {
                for (String com: readFAF3.sctgStringCommodities) {
                    int zoneNum = (int) readFAF3.fafRegionList.getValueAt(fafNum, "ZoneID");
                    TableDataSet CountyList = getCountySpecificDataByFAFwithDetailedEmployment(zoneNum, com, factors, reduction);
                    String code = dir + "_" + zoneNum + "_" + com;
                    countyShares.put(code, CountyList);
                }
            }
        }

        // create fake county lists for special regions such as airports or seaports that shall be kept separate from the FAF regions
        if (specialRegions == null) return;  // OK truck flows
        for (int row = 1; row <= specialRegions.getRowCount(); row++) {
            int[] fips = {(int) specialRegions.getValueAt(row, "modelCode")};
            String[] name = new String[]{specialRegions.getStringValueAt(row, "faf3code")};
            TableDataSet CountyList = new TableDataSet();
            float[] emplDummy = {1};
            CountyList.appendColumn(name, "Name");
            CountyList.appendColumn(fips, "COUNTYFIPS");
            CountyList.appendColumn(name, "FAFRegion");
            CountyList.appendColumn(emplDummy, "Employment");
            for (String dir: direction) {
                for (String com: readFAF3.sctgStringCommodities) {
                    String code = dir + "_" + fips[0] + "_" + com;
                    countyShares.put(code, CountyList);
                }
            }
        }
    }


    public void prepareCountyDataForFAFwithDetailedEmployment(ResourceBundle rb, int yr, boolean keepTrackOfEmpType) {
        // prepare county data to provide detailed employment as a weight

        logger.info("  Preparing county data with detailed employment for FAF flow disaggregation for year " + yr + "...");
        HashMap<String, Float> useC = createMakeUseHashMap(rb, "faf.use.coefficients");
        HashMap<String, Float> makeC = createMakeUseHashMap(rb, "faf.make.coefficients");
        distCounties = MatrixReader.readMatrix(new File(rb.getString("county.distance.in.miles")), "Distance");
        float reduction = (float) ResourceUtil.getDoubleProperty(rb, "reduction.local.office.weight", 1d);

        //create HashMap that contains for every FAF region a TableDataSet of counties with their employment share
        countyShares = new HashMap<>();
        String[] direction = {"orig", "dest"};
        for (String dir: direction) {
            HashMap<String, Float> factors;
            if (dir.equals("orig")) factors = makeC;
            else factors = useC;

            Object readFAF = null;
            if(FAFVersion==3){
         
            	for (int fafNum = 1; fafNum <= readFAF3.fafRegionList.getRowCount(); fafNum++) {
            		for (String com: readFAF3.sctgStringCommodities) {
            			int zoneNum = (int) readFAF3.fafRegionList.getValueAt(fafNum, "ZoneID");
            			TableDataSet CountyList;
            			if (!keepTrackOfEmpType) {
            				CountyList = getCountySpecificDataByFAFwithDetailedEmployment(zoneNum, com, factors, reduction);
            			} else {
            				CountyList = getCountySpecificDataByFAFwithDetailedEmploymentByType(zoneNum, com, factors, reduction);
            			}
            			String code = dir + "_" + zoneNum + "_" + com;
            			countyShares.put(code, CountyList);
            		}
            	}
            }else{
               	for (int fafNum = 1; fafNum <= ReadFAF4.fafRegionList.getRowCount(); fafNum++) {
            		for (String com: ReadFAF4.sctgStringCommodities) {
            			int zoneNum = (int) ReadFAF4.fafRegionList.getValueAt(fafNum, "ZoneID");
            			TableDataSet CountyList;
            			if (!keepTrackOfEmpType) {
            				CountyList = getCountySpecificDataByFAFwithDetailedEmployment(zoneNum, com, factors, reduction);
            			} else {
            				CountyList = getCountySpecificDataByFAFwithDetailedEmploymentByType(zoneNum, com, factors, reduction);
            			}
            			String code = dir + "_" + zoneNum + "_" + com;
            			countyShares.put(code, CountyList);
            		}
            	}
            	
            }
        }
    }


    public void scaleSelectedCounties (ResourceBundle rb) {
        // scale weight of employment up or down for selected counties
    	String fieldName = "FAF3region";
    	if(FAFVersion==4)
    		fieldName = "FAF4region";

    	TableDataSet countyScaler = fafUtils.importTable(rb.getString("county.scaler"));
        for (int row = 1; row <= countyScaler.getRowCount(); row++) {
            int fips= (int) countyScaler.getValueAt(row, "countyFips");
            float scaler = countyScaler.getValueAt(row, "scaler");
            // find FAF zone
            int fafZone = 0;
            for (int countyRow = 1; countyRow <= countyIDsWithEmployment.getRowCount(); countyRow++) {
                if (countyIDsWithEmployment.getValueAt(countyRow, "COUNTYFIPS") == fips) fafZone =
                        (int) countyIDsWithEmployment.getValueAt(countyRow, fieldName);
            }
            if (fafZone == 0) logger.error("Could not find FAF zone of FIPS " + fips + " in file " + rb.getString("county.ID"));
            String[] direction = {"orig", "dest"};
            for (String dir: direction) {
            	
            	if(FAFVersion==3){
            		for (String com: readFAF3.sctgStringCommodities) {
                    	String code = dir + "_" + fafZone + "_" + com;
                    	TableDataSet scalerThisCounty = countyShares.get(code);
                    	for (int scalerRow = 1; scalerRow <= scalerThisCounty.getRowCount(); scalerRow++) {
                    		if (scalerThisCounty.getValueAt(scalerRow, "COUNTYFIPS") == fips) {
                            	float value = scalerThisCounty.getValueAt(scalerRow, "Employment") * scaler;
                            	scalerThisCounty.setValueAt(scalerRow, "Employment", value);
                        	}
                    	}
                	}
            	}else{
            		for (String com: ReadFAF4.sctgStringCommodities) {
                    	String code = dir + "_" + fafZone + "_" + com;
                    	TableDataSet scalerThisCounty = countyShares.get(code);
                    	for (int scalerRow = 1; scalerRow <= scalerThisCounty.getRowCount(); scalerRow++) {
                    		if (scalerThisCounty.getValueAt(scalerRow, "COUNTYFIPS") == fips) {
                            	float value = scalerThisCounty.getValueAt(scalerRow, "Employment") * scaler;
                            	scalerThisCounty.setValueAt(scalerRow, "Employment", value);
                        	}
                    	}
                	}
            		
            		
            	}
            }
        }
    }


    private boolean checkIfFAFzoneHasDetailedEmployment(String nameOfFAF, int noCountiesInFAF, TableDataSet detEmpl,
                                                        int fafVersion) {
        // Check if detEmpl has detailed employment for all counties in FAF zone FAFNumber

        if (detEmpl == null) return false;
        boolean[] countyHasDet = new boolean[noCountiesInFAF];
        int k = 0;
        String colLabel;
        if (fafVersion == 2) colLabel = "FafRegion";
        else colLabel = "FAF3region";
        for (int i = 1; i <= countyIDsWithEmployment.getRowCount(); i++)
            if (countyIDsWithEmployment.getStringValueAt(i, colLabel).equals(nameOfFAF))
                for (int row = 1; row <= detEmpl.getRowCount(); row++) {
                    if (countyIDsWithEmployment.getValueAt(i, "COUNTYFIPS") == detEmpl.getValueAt(row, "CountyFips")) {
                        countyHasDet[k] = true;
                        k++;
                    }
                }
        int count = 0;
        for (boolean county: countyHasDet) if (county) count++;
        if (count == 0) return false;
        else if (count == noCountiesInFAF) return true;
        else logger.fatal("Detailed employment covers FAF zone " + nameOfFAF + " only partly.");
        return false;
    }


    private float getWeightedEmpl(int row, String comFAF2, TableDataSet detEmpl, HashMap<String, Float> factors) {
        // calculate weighted employment based on make/use coefficients

        float empl = 0;
        String[] emplCategoriesTemp = detEmpl.getColumnLabels();
        String[] emplCategories = new String[emplCategoriesTemp.length-2];
        System.arraycopy(emplCategoriesTemp, 2, emplCategories, 0, emplCategories.length);
        for (String emplCat: emplCategories) {
            String code = emplCat + "_" + comFAF2;
            float coeff = factors.get(code);
            for (int col = 3; col <= detEmpl.getColumnCount(); col++) {
                empl += detEmpl.getValueAt(row, col) * coeff;
            }
        }
        return empl;
    }


    private float getWeightedEmpl(int row, int comFAF3, TableDataSet detEmpl, HashMap<String, Float> factors) {
        // calculate weighted employment based on make/use coefficients

        float empl = 0;
        String[] emplCategoriesTemp = detEmpl.getColumnLabels();
        String[] emplCategories = new String[emplCategoriesTemp.length-2];
        System.arraycopy(emplCategoriesTemp, 2, emplCategories, 0, emplCategories.length);
        for (String emplCat: emplCategories) {
            String code = emplCat + "_" + String.valueOf(comFAF3);
            float coeff = factors.get(code);
            for (int col = 3; col <= detEmpl.getColumnCount(); col++) {
                empl += detEmpl.getValueAt(row, col) * coeff;
            }
        }
        return empl;
    }


    private float getWeightedEmpl(int row, String comFAF, HashMap<String, Float> factors, float officeReduction) {
        // calculate weighted employment based on make/use coefficients
        float empl = 0;
        for (String emp: empCats) {
            String code = emp + "_" + comFAF;
            float coeff = factors.get(code);
            float factor = 1;
            if (fafUtils.arrayContainsElement(emp, new String[]{"Information", "Financial Activities",
                    "Professional and Business Services", "Education and Health Services",
                    "Leisure and Hospitality"})) factor = officeReduction;
            empl += countyIDsWithEmployment.getValueAt(row, emp) * coeff * factor;
        }
        return empl;
    }


    private HashMap<String, Float> getWeightedEmplByEmpl(int row, String comFAF, HashMap<String, Float> factors, float officeReduction) {
        // calculate weighted employment based on make/use coefficients, keep information by employment type

        HashMap<String, Float> empl = new HashMap<>();
        float total = 0;
        for (String emp: empCats) {
            String code = emp + "_" + comFAF;
            float coeff = factors.get(code);
            float factor = 1;
            if (fafUtils.arrayContainsElement(emp, new String[]{"Information", "Financial Activities",
                    "Professional and Business Services", "Education and Health Services",
                    "Leisure and Hospitality"})) factor = officeReduction;
            float thisVal = countyIDsWithEmployment.getValueAt(row, emp) * coeff * factor;
            empl.put(emp, thisVal);
            total += thisVal;
        }
        empl.put("total", total);
        return empl;
    }


    public double[][] disaggCountyToZones(float flow, double[] weightsA, double[] weightsB) {
        // disaggregate flow from orig county A to orig zones i and dest county B to dest zones j

        // sum up weight products
        double sm = 0;
        for (double thisWeightsA : weightsA) {
            for (double thisWeightsB : weightsB) {
                sm += thisWeightsA * thisWeightsB;
            }
        }
        // disaggregate flow
        double[][] disFlow = new double[weightsA.length][weightsB.length];
        for (int i = 0; i < weightsA.length; i++) {
            for (int j = 0; j < weightsB.length; j++) {
                disFlow[i][j] = flow * weightsA[i] * weightsB[j] / sm;
            }
        }
        return disFlow;
    }


    public static int getCountyId(int fips) {
        // Return region code of regName
        for (int i = 0; i < countyFips.length; i++) {
            if (countyFips[i] == fips) return i;
        }
        logger.error("Could not find county FIPS code " + fips);
        return -1;
    }


    public String[] getEmpCats() {
        return empCats;
    }

}
