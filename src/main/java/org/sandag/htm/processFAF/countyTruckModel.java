package org.sandag.htm.processFAF;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.sandag.htm.applications.ohio;
import org.sandag.htm.applications.yumaMPO;
import org.apache.log4j.Logger;
import com.pb.sawdust.util.concurrent.DnCRecursiveAction;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * Reads FAF3 data, disaggregates them to county-to-county flows and converts them into truck trips
 * Author: Rolf Moeckel, PB Albuquerque
 * Date: August 22, 2011 (Santa Fe NM)
 * Revised for Yuma County: March 29, 2012 (Albuquerque, NM)
 */

public class countyTruckModel {

    private static Logger logger = Logger.getLogger(countyTruckModel.class);
    private ResourceBundle appRb;
    private int year;
    private disaggregateFlows df;
    private HashMap<String, float[][]> cntFlows;
    private int[] countyFips;
    private int[] countyFipsIndex;
    private int[] commodityGrouping;
    private int numCommodityGroups;
    private boolean yumaSummary;


    public countyTruckModel(ResourceBundle rb, int yr) {
        this.appRb = rb;
        this.year = yr;
    }


    public static void main(String[] args) {
        // construct main model

        long startTime = System.currentTimeMillis();
        ResourceBundle appRb = fafUtils.getResourceBundle(args[0]);
        int year = Integer.parseInt(args[1]);
        countyTruckModel ctm = new countyTruckModel(appRb, year);
        ctm.run();
        logger.info("County Truck Model completed.");
        float endTime = fafUtils.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        logger.info("Runtime: " + endTime + " minutes.");
    }


    private void run() {
        // main run method

        logger.info("Started FAF4 model to generate county-to-county truck flows for " + year);
        ReadFAF4 faf4 = new ReadFAF4();
        df = new disaggregateFlows();
        df.getUScountyEmploymentByIndustry(appRb);
        faf4.readAllData(appRb, year, "tons");
        faf4.definePortsOfEntry(appRb);
        if (ResourceUtil.getBooleanProperty(appRb, "read.in.raw.faf.data", true)) extractTruckData(faf4);
        createZoneList();
        String truckTypeDefinition = "sut_mut";
        df.defineTruckTypes(truckTypeDefinition, appRb);
        if (ResourceUtil.getBooleanProperty(appRb, "save.results.for.ohio", false)) ohio.summarizeFAFData(appRb, countyFips);
        yumaSummary = ResourceUtil.getBooleanProperty(appRb, "save.results.for.yuma", false);
        if (yumaSummary) yumaMPO.initializeVariables(appRb);
        disaggregateFromFafToCounties();
        if (yumaSummary) yumaMPO.writeOutResults(appRb, year);
        if (ResourceUtil.getBooleanProperty(appRb, "report.by.employment")) {
            convertFromCommoditiesToTrucksByEmpType();
        } else {
            convertFromCommoditiesToTrucks();
        }
        if (ResourceUtil.getBooleanProperty(appRb, "analyze.commodity.groups", false)) {
            readCommodityGrouping();
            convertFromCommoditiesToTrucksByComGroup();
        }
    }

    private void extractTruckData(readFAF3 faf3) {
        // extract truck data and write flows to file
        logger.info("Extracting FAF3 truck data");
        String[] scaleTokens = ResourceUtil.getArray(appRb, "scaling.truck.trips.tokens");
        double[] scaleValues = ResourceUtil.getDoubleArray(appRb, "scaling.truck.trips.values");
        HashMap <String, Float> scaler = fafUtils.createScalerHashMap(scaleTokens, scaleValues);
        String truckFileNameT = ResourceUtil.getProperty(appRb, "temp.truck.flows.faf.zones") + "_" + year;

        // create output directory if it does not exist yet
        File file = new File ("output/temp");
        if (!file.exists()) {
            boolean outputDirectorySuccessfullyCreated = file.mkdir();
            if (!outputDirectorySuccessfullyCreated) logger.warn("Could not create scenario directory output/temp/");
        }

        faf3.writeFlowsByModeAndCommodity(truckFileNameT, ModesFAF.Truck, reportFormat.internat_domesticPart, scaler);
    }

    private void extractTruckData(ReadFAF4 faf4) {
        // extract truck data and write flows to file
        logger.info("Extracting FAF4 truck data");
        String[] scaleTokens = ResourceUtil.getArray(appRb, "scaling.truck.trips.tokens");
        double[] scaleValues = ResourceUtil.getDoubleArray(appRb, "scaling.truck.trips.values");
        HashMap <String, Float> scaler = fafUtils.createScalerHashMap(scaleTokens, scaleValues);
        String truckFileNameT = ResourceUtil.getProperty(appRb, "temp.truck.flows.faf.zones") + "_" + year;

        // create output directory if it does not exist yet
        File file = new File ("output/temp");
        if (!file.exists()) {
            boolean outputDirectorySuccessfullyCreated = file.mkdir();
            if (!outputDirectorySuccessfullyCreated) logger.warn("Could not create scenario directory output/temp/");
        }

        faf4.writeFlowsByModeAndCommodity(truckFileNameT, ModesFAF.Truck, reportFormat.internat_domesticPart, scaler);
    }

    private void createZoneList() {
        // Create array with specialRegions that serve as port of entry/exit

        TableDataSet poe = fafUtils.importTable(appRb.getString("ports.of.entry"));
        countyFips = fafUtils.createCountyFipsArray(poe.getColumnAsInt("pointOfEntry"));
        countyFipsIndex = new int[fafUtils.getHighestVal(countyFips) + 1];
        for (int i = 0; i < countyFips.length; i++) {
            countyFipsIndex[countyFips[i]] = i;
        }
    }


    private void disaggregateFromFafToCounties() {
        // disaggregates freight flows from FAF zoneArray to counties

        logger.info("Disaggregating FAF3 data from FAF zones to counties for year " + year + ".");

        cntFlows = new HashMap<>();

        String[] commodities;
        commodities = readFAF3.sctgStringCommodities;
        int matrixSize = countyFips.length;
        cntFlows = new HashMap<>();

        float globalScale = (float) ResourceUtil.getDoubleProperty(appRb, "overall.scaling.factor.truck");

        // regular method
        for (String com: commodities) {
            float[][] dummy = new float[matrixSize][matrixSize];
            cntFlows.put(com, dummy);
        }
        boolean keepTrackOfEmplType = ResourceUtil.getBooleanProperty(appRb, "report.by.employment", false);
        df.prepareCountyDataForFAFwithDetailedEmployment(appRb, year, keepTrackOfEmplType);

        java.util.concurrent.ForkJoinPool pool = new java.util.concurrent.ForkJoinPool();
        DnCRecursiveAction action = new DissaggregateFafAction(globalScale);
        pool.execute(action);
        action.getResult();

        if (ResourceUtil.getBooleanProperty(appRb, "summarize.by.ohio.rail.zones", false))
            ohio.sumFlowByRailZone(appRb, year, countyFips, countyFipsIndex, cntFlows);
    }


    private class DissaggregateFafAction extends DnCRecursiveAction {
        private final float globalScale;

        private DissaggregateFafAction(float globalScale) {
            super(0,readFAF3.sctgStringCommodities.length);
            this.globalScale = globalScale;
        }

        private DissaggregateFafAction(float globalScale, long start, long length, DnCRecursiveAction next) {
            super(start,length,next);
            this.globalScale = globalScale;
        }

        @Override
        protected void computeAction(long start, long length) {
            long end = start + length;
            for (int comm = (int) start; comm < end; comm++) {
                int cm = readFAF3.sctgCommodities[comm];
                String fileName = ResourceUtil.getProperty(appRb, "temp.truck.flows.faf.zones") + "_" + year;
                if (cm < 10) fileName = fileName + "_SCTG0" + cm + ".csv";
                else fileName = fileName + "_SCTG" + cm + ".csv";
                logger.info("  Working on " + fileName);
                String sctg = readFAF3.getSCTGname(cm);
                float[][] values = cntFlows.get(sctg);
                TableDataSet tblFlows = fafUtils.importTable(fileName);
                for (int row = 1; row <= tblFlows.getRowCount(); row++) {
                    float shortTons = tblFlows.getValueAt(row, "shortTons");
                    if (shortTons == 0) continue;
                    String dir = tblFlows.getStringValueAt(row, "flowDirection");
                    int orig = (int) tblFlows.getValueAt(row, "originFAF");
                    int dest = (int) tblFlows.getValueAt(row, "destinationFAF");
                    TableDataSet singleFlow;
                    if (dir.equals("import") || dir.equals("export")) {
                        TableDataSet poe;
                        if (dir.equals("import")) poe = readFAF3.getPortsOfEntry(orig);
                        else poe = readFAF3.getPortsOfEntry(dest);
                        singleFlow = df.disaggregateSingleFAFFlowThroughPOE(dir, poe, orig, dest, sctg, shortTons, 1);
                    } else singleFlow = df.disaggregateSingleFAFFlow(orig, dest, sctg, shortTons, 1);
                    for (int i = 1; i <= singleFlow.getRowCount(); i++) {
                        int oFips = (int) singleFlow.getValueAt(i, "oFips");
                        int oZone = getCountyId(oFips);
                        int dFips = (int) singleFlow.getValueAt(i, "dFips");
                        int dZone = getCountyId(dFips);
                        float thisFlow = singleFlow.getValueAt(i, "Tons") * globalScale;
                        values[oZone][dZone] += thisFlow;
                        if (yumaSummary) yumaMPO.saveForYuma(cm, oFips, dFips, thisFlow);
                    }
                }
            }
        }

        @Override
        protected DnCRecursiveAction getNextAction(long start, long length, DnCRecursiveAction next) {
            return new DissaggregateFafAction(globalScale,start,length,next);
        }

        @Override
        protected boolean continueDividing(long length) {
            return getSurplusQueuedTaskCount() < 3 && length > 1;
        }
    }


    private int getCountyId(int fips) {
        // Return region code of regName
        return countyFipsIndex[fips];
    }


    private void readCommodityGrouping () {
        // Read how commodities are grouped by SCTG cagegory

        TableDataSet comGroups = fafUtils.importTable(appRb.getString("commodity.grouping"));
        commodityGrouping = new int[fafUtils.getHighestVal(readFAF3.sctgCommodities) + 1];
        for (int row = 1; row <= comGroups.getRowCount(); row++) {
            int sctg = (int) comGroups.getValueAt(row, "SCTG");
            int group = (int) comGroups.getValueAt(row, "Group");
            commodityGrouping[sctg] = group;
        }
        numCommodityGroups = 0;
        for (int i = 0; i < commodityGrouping.length; i++) {
            if (commodityGrouping[i] == 0) continue;
            boolean alreadyExists = false;
            for (int j = 0; j < i; j++) {
                if (j == i) continue;
                if (commodityGrouping[j] == commodityGrouping[i]) alreadyExists = true;
            }
            if (!alreadyExists) numCommodityGroups++;
        }
    }


    private void convertFromCommoditiesToTrucks() {
        // generate truck flows based on commodity flows
        logger.info("Converting flows in tons into truck trips");
        float emptyTruckRate = (float) ResourceUtil.getDoubleProperty(appRb, "empty.truck.rate");
        float aawdtFactor = (float) ResourceUtil.getDoubleProperty(appRb, "AADT.to.AAWDT.factor");
        double[][]sluTrucks = new double[countyFips.length][countyFips.length];
        double[][]mtuTrucks = new double[countyFips.length][countyFips.length];
        for (String com: readFAF3.sctgStringCommodities) {
            double avPayload = fafUtils.findAveragePayload(com, "SCTG");
            double sutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.SUT.payload") * avPayload;
            double mutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.MUT.payload") * avPayload;
            float[][] tonFlows = cntFlows.get(com);
            for (int i: countyFips) {
                for (int j: countyFips) {
                    float dist = df.getCountyDistance(i, j);
                    if (dist < 0) continue;  // skip flows to Guam, Puerto Rico, Hawaii, Alaskan Islands etc.
                    int orig = getCountyId(i);
                    int dest = getCountyId(j);
                    if (tonFlows[orig][dest] == 0) continue;
                    double[] trucksByType = df.getTrucksByType(dist, sutPL, mutPL, tonFlows[orig][dest]);
                    // add empty trucks
                    trucksByType[0] += trucksByType[0] * (emptyTruckRate/100.0);
                    trucksByType[1] += trucksByType[1] * (emptyTruckRate/100.0);
                    // Annual cntFlows divided by 365.25 days plus AAWDT-over-AADT factor
                    trucksByType[0] = trucksByType[0] / 365.25f * (1 + (aawdtFactor / 100));
                    trucksByType[1] = trucksByType[1] / 365.25f * (1 + (aawdtFactor / 100));
                    sluTrucks[orig][dest] += trucksByType[0];
                    mtuTrucks[orig][dest] += trucksByType[1];
                }
            }
        }
        if (ResourceUtil.getBooleanProperty(appRb, "write.cnty.to.cnty.truck.trps", false))
            writeOutDisaggregatedTruckTrips(sluTrucks, mtuTrucks);
        if (ResourceUtil.getBooleanProperty(appRb, "write.ii.ei.ee.trips", false))
            writeOutDisaggregatedTruckTripsByDirection(sluTrucks, mtuTrucks);
    }


    private void convertFromCommoditiesToTrucksByEmpType() {
        // generate truck flows based on commodity flows, keeping track of employment type generating/attracting trucks

        logger.info("Converting flows in tons into truck trips, keeping track of employment types generating trucks");
        float emptyTruckRate = (float) ResourceUtil.getDoubleProperty(appRb, "empty.truck.rate");
        float aawdtFactor = (float) ResourceUtil.getDoubleProperty(appRb, "AADT.to.AAWDT.factor");
        HashMap<Integer, float[]> countyWeights = getCountyWeightsWithDetEmpl();

        String[] emplCat = df.getEmpCats();

        double[][][]sluTrucks = new double[emplCat.length][countyFips.length][countyFips.length];
        double[][][]mtuTrucks = new double[emplCat.length][countyFips.length][countyFips.length];

        for (String com: readFAF3.sctgStringCommodities) {
            double avPayload = fafUtils.findAveragePayload(com, "SCTG");
            double sutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.SUT.payload") * avPayload;
            double mutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.MUT.payload") * avPayload;
            float[][] tonFlows = cntFlows.get(com);
            for (int i: countyFips) {
                float[] weights = countyWeights.get(i);
                for (int j: countyFips) {
                    float dist = df.getCountyDistance(i, j);
                    if (dist < 0) continue;  // skip flows to Guam, Puerto Rico, Hawaii, Alaskan Islands etc.
                    int orig = getCountyId(i);
                    int dest = getCountyId(j);
                    if (tonFlows[orig][dest] == 0) continue;
                    double[] trucksByType = df.getTrucksByType(dist, sutPL, mutPL, tonFlows[orig][dest]);
                    // add empty trucks
                    trucksByType[0] += trucksByType[0] * (emptyTruckRate/100.0);
                    trucksByType[1] += trucksByType[1] * (emptyTruckRate/100.0);
                    // Annual cntFlows divided by 365.25 days plus AAWDT-over-AADT factor
                    trucksByType[0] = trucksByType[0] / 365.25f * (1 + (aawdtFactor / 100));
                    trucksByType[1] = trucksByType[1] / 365.25f * (1 + (aawdtFactor / 100));
                    for (int eCat = 0; eCat < emplCat.length; eCat++) {
                        if (trucksByType[0] + trucksByType[1] == 0) continue;
                        try {
                            sluTrucks[eCat][orig][dest] += trucksByType[0] * weights[eCat];
                            mtuTrucks[eCat][orig][dest] += trucksByType[1] * weights[eCat];
                        } catch (Exception e){
                            logger.warn(eCat+" "+orig+" "+dest+" "+i+" "+j+" "+com+": "+trucksByType[0]+trucksByType[1]);
                        }
                    }
                }
            }
        }
        writeOutDisaggregatedTruckTripsDetEmpl(sluTrucks, mtuTrucks);
    }


    private HashMap<Integer, float[]> getCountyWeightsWithDetEmpl() {
        // look up employment weight for each county

        HashMap<Integer, float[]> weights = new HashMap<>();
        TableDataSet counties = fafUtils.importTable(ResourceUtil.getProperty(appRb, "county.ID"));
        String[] emplCats = df.getEmpCats();
        for (int row = 1; row <= counties.getRowCount(); row++) {
            int fips = (int) counties.getValueAt(row, "COUNTYFIPS");
            int faf = (int) counties.getValueAt(row, "FAF3region");
            if (faf == -99) continue;
            for (String com: readFAF3.sctgStringCommodities) {
                TableDataSet countyWeightsInOrigInFafZone = df.getCountyWeights("orig", faf, com);
                for (int rw = 1; rw <= countyWeightsInOrigInFafZone.getRowCount(); rw++) {
                    if (countyWeightsInOrigInFafZone.getValueAt(rw, "COUNTYFIPS") == fips) {
                        float[] wght = new float[emplCats.length];
                        for (int eCat = 0; eCat < emplCats.length; eCat++) {
                            wght[eCat] = countyWeightsInOrigInFafZone.getValueAt(rw, emplCats[eCat]);
                        }
                        float sum = fafUtils.getSum(wght);
                        // normalize weights from 0 - 1
                        for (int eCat = 0; eCat < emplCats.length; eCat++) {
                            wght[eCat] = wght[eCat] / sum;
                        }
                        weights.put(fips, wght);
                    }
                }
            }
        }
        return weights;
    }


    private void writeOutDisaggregatedTruckTrips(double[][]sluTrucks, double[][]mtuTrucks) {
        // write out disaggregated truck trips

        double[] truckProd = new double[fafUtils.getHighestVal(countyFips) + 1];

        String fileName = appRb.getString("cnt.to.cnt.truck.flows") + year + ".csv";
        logger.info("Writing results to file " + fileName);
        PrintWriter pw = fafUtils.openFileForSequentialWriting(fileName);
        pw.println("OrigFips,DestFips,sut,mut");
        for (int i: countyFips) {
            for (int j: countyFips) {
                int orig = getCountyId(i);
                int dest = getCountyId(j);
                double slu = sluTrucks[orig][dest];
                double mtu = mtuTrucks[orig][dest];
                if (slu + mtu >= 0.00001) {
                    pw.format ("%d,%d,%.5f,%.5f", i, j, slu, mtu);
                    pw.println();
                    truckProd[i] += slu + mtu;
                }
            }
        }
        pw.close();

        if (ResourceUtil.getBooleanProperty(appRb, "report.truck.prod.by.county", false)) {
            String fileNameProd = appRb.getString("truck.prod.by.county") + year + ".csv";
            PrintWriter pwp = fafUtils.openFileForSequentialWriting(fileNameProd);
            pwp.println("CountyFips,TrucksGenerated");
            for (int i: countyFips) {
                if (truckProd[i] > 0) pwp.println(i + "," + truckProd[i]);
            }
            pwp.close();
        }
    }


    private void writeOutDisaggregatedTruckTripsDetEmpl(double[][][]sluTrucks, double[][][]mtuTrucks) {
        // write out disaggregated truck trips

        String fileName = appRb.getString("cnt.to.cnt.truck.flows") + "ByEmployment" + year + ".csv";
        logger.info("Writing results to file " + fileName);
        PrintWriter pw = fafUtils.openFileForSequentialWriting(fileName);
        pw.print("OrigFips,DestFips");
        String[] emplCats = df.getEmpCats();
        for (String emplCat : emplCats) {
            if (emplCat.contains("Trade") || emplCat.contains("Financial") ||
                    emplCat.contains("Education") || emplCat.contains("Other")) continue;
            pw.print("," + emplCat + "_SUT," + emplCat + "_MUT");
        }
        pw.println();
        for (int i: countyFips) {
            for (int j: countyFips) {
                int orig = getCountyId(i);
                int dest = getCountyId(j);
                pw.print(i + "," + j);
                for (int eCat = 0; eCat < emplCats.length; eCat++) {
                    if (emplCats[eCat].contains("Trade") || emplCats[eCat].contains("Financial") ||
                            emplCats[eCat].contains("Education") || emplCats[eCat].contains("Other")) continue;
                    double slu = sluTrucks[eCat][orig][dest];
                    double mtu = mtuTrucks[eCat][orig][dest];
                    pw.print("," + slu + "," + mtu);
                }
                pw.println();
            }
        }
        pw.close();
    }


    private void writeOutDisaggregatedTruckTripsByDirection(double[][]sluTrucks, double[][]mtuTrucks) {
        // write out disaggregated truck trips distinguishing II, EI/IE and EE trips

        TableDataSet counties = disaggregateFlows.countyIDsWithEmployment;
        boolean[] relevantCounty = new boolean[fafUtils.getHighestVal(countyFips) + 1];
        String state = appRb.getString("ii.state");

        for (int row = 1; row <= counties.getRowCount(); row++) {
            relevantCounty[(int) counties.getValueAt(row, "COUNTYFIPS")] = counties.getStringValueAt(row, "StateCode").equals(state);
        }

        String fileName = appRb.getString("cnt.to.cnt.truck.flows.by.dir") + year + ".csv";
        logger.info("Writing ii/ei/ie/ee flows to file " + fileName);
        PrintWriter pw = fafUtils.openFileForSequentialWriting(fileName);
        pw.println("OrigFips,DestFips,iiSut,iiMut,eiSut,eiMut,eeSut,eeMut,totSut,totMut");
        for (int i: countyFips) {
            for (int j: countyFips) {
                int orig = getCountyId(i);
                int dest = getCountyId(j);
                double slu = sluTrucks[orig][dest];
                double mtu = mtuTrucks[orig][dest];
                if (slu + mtu >= 0.00001) {
                    if (relevantCounty[i] && relevantCounty[j]) {
                        pw.format ("%d,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f", i, j, slu, mtu, 0., 0., 0., 0., slu, mtu);
                        pw.println();
                    } else if ((relevantCounty[i] && !relevantCounty[j]) || (!relevantCounty[i] && relevantCounty[j])) {
                        pw.format ("%d,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f", i, j, 0., 0., slu, mtu, 0., 0., slu, mtu);
                        pw.println();
                    } else {
                        pw.format ("%d,%d,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f,%.5f", i, j, 0., 0., 0., 0., slu, mtu, slu, mtu);
                        pw.println();
                    }
                }
            }
        }
        pw.close();
    }


    private void convertFromCommoditiesToTrucksByComGroup () {
        // read flows in tons and convert into flows in trucks, distinguishing commodity groups

        float emptyTruckRate = (float) ResourceUtil.getDoubleProperty(appRb, "empty.truck.rate");
        float aawdtFactor = (float) ResourceUtil.getDoubleProperty(appRb, "AADT.to.AAWDT.factor");
        double[][][] sluTrucks = new double[numCommodityGroups+1][countyFips.length][countyFips.length];
        double[][][] mtuTrucks = new double[numCommodityGroups+1][countyFips.length][countyFips.length];
        for (String com: readFAF3.sctgStringCommodities) {
            int iCom = Integer.parseInt(com.substring(4, 6));  // convert "SCTG00" into "00"
            double avPayload = fafUtils.findAveragePayload(com, "SCTG");
            double sutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.SUT.payload") * avPayload;
            double mutPL = ResourceUtil.getDoubleProperty(appRb, "multiplier.MUT.payload") * avPayload;
            float[][] tonFlows = cntFlows.get(com);
            for (int i: countyFips) {
                for (int j: countyFips) {
                    int orig = getCountyId(i);
                    int dest = getCountyId(j);
                    if (tonFlows[orig][dest] == 0) continue;
                    float dist = df.getCountyDistance(i, j);
                    if (dist < 0) continue;  // skip flows to Guam, Puerto Rico, Hawaii, Alaskan Islands etc.
                    double[] trucksByType = df.getTrucksByType(dist, sutPL, mutPL, tonFlows[orig][dest]);
                    // add empty trucks
                    trucksByType[0] += trucksByType[0] * (emptyTruckRate/100.0);
                    trucksByType[1] += trucksByType[1] * (emptyTruckRate/100.0);
                    // Annual cntFlows divided by 365.25 days plus AAWDT-over-AADT factor
                    trucksByType[0] = trucksByType[0] / 365.25f * (1 + (aawdtFactor / 100));
                    trucksByType[1] = trucksByType[1] / 365.25f * (1 + (aawdtFactor / 100));
                    sluTrucks[commodityGrouping[iCom]][orig][dest] += trucksByType[0];
                    mtuTrucks[commodityGrouping[iCom]][orig][dest] += trucksByType[1];
                }
            }
        }
        writeOutDisaggregatedTruckTrips(sluTrucks, mtuTrucks);
    }


    private void writeOutDisaggregatedTruckTrips(double sluTrucks[][][], double[][][] mutTrucks) {
        // Write out truck trips by commodity group

        TableDataSet counties = disaggregateFlows.countyIDsWithEmployment;
        boolean[] relevantCounty = new boolean[fafUtils.getHighestVal(countyFips) + 1];
        String state = appRb.getString("ii.state");
        double[][] prodByCounty = new double[countyFips.length][numCommodityGroups + 1];
        double[][] attrByCounty = new double[countyFips.length][numCommodityGroups + 1];

        for (int row = 1; row <= counties.getRowCount(); row++) {
            relevantCounty[(int) counties.getValueAt(row, "COUNTYFIPS")] =
                    counties.getStringValueAt(row, "StateCode").equals(state);
        }

        boolean seperateEEflows = ResourceUtil.getBooleanProperty(appRb, "report.ext.flows.separately");
        String cgFile = appRb.getString("trucks.by.commodity.group") + year + ".csv";
        PrintWriter pwc = fafUtils.openFileForSequentialWriting(cgFile);
        pwc.print("orig,dest");
        for (int cg = 1; cg <= numCommodityGroups; cg++) pwc.print(",com" + cg);
        if (seperateEEflows) {
            pwc.println(",external");
        } else {
            pwc.println();
        }
        for (int i = 0; i < countyFips.length; i++) {
            for (int j = 0; j < countyFips.length; j++) {

                double sm = 0.;
                for (int cg = 1; cg <= numCommodityGroups; cg++) sm += sluTrucks[cg][i][j] + mutTrucks[cg][i][j];

                if (sm > 0) {
                    pwc.print(countyFips[i] + "," + countyFips[j]);
                    if (seperateEEflows) {
                        if (!relevantCounty[countyFips[i]] && !relevantCounty[countyFips[j]]) {
                            // E-E flows
                            for (int cg = 1; cg <= numCommodityGroups; cg++) pwc.print(",0");
                            pwc.println("," + sm);
                        } else {
                            // I-I, I-E and E-I flows
                            for (int cg = 1; cg <= numCommodityGroups; cg++) pwc.print("," +
                                    (sluTrucks[cg][i][j] + mutTrucks[cg][i][j]));
                            pwc.println(",0");
                        }
                    } else {
                            // I-I, I-E, E-I and E-E flows
                            for (int cg = 1; cg <= numCommodityGroups; cg++) pwc.print("," +
                                    (sluTrucks[cg][i][j] + mutTrucks[cg][i][j]));
                            pwc.println();
                    }
                    for (int cg = 1; cg <= numCommodityGroups; cg++) {
                        prodByCounty[i][cg] += sluTrucks[cg][i][j] + mutTrucks[cg][i][j];
                        attrByCounty[j][cg] += sluTrucks[cg][i][j] + mutTrucks[cg][i][j];
                    }
                }
            }
        }
        pwc.close();

        String cgFileAgg = appRb.getString("trucks.by.commodity.group") + year + "_prodAttr.csv";
        PrintWriter pwca = fafUtils.openFileForSequentialWriting(cgFileAgg);
        pwca.print("countyFips");
        for (int cg = 1; cg <= numCommodityGroups; cg++) pwca.print(",prod_" + cg + ",attr_" + cg);
        pwca.println();
        for (int i = 0; i < countyFips.length; i++) {
            float sm = 0;
            for (int cg = 1; cg <= numCommodityGroups; cg++) {
                sm += prodByCounty[i][cg] + attrByCounty[i][cg];
            }
            if (sm > 0) {
                pwca.print(countyFips[i]);
                for (int cg = 1; cg <= numCommodityGroups; cg++) pwca.print("," + prodByCounty[i][cg] + "," +
                        attrByCounty[i][cg]);
                pwca.println();
            }
        }
        pwca.close();
    }
}
