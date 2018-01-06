package com.pb.sandag_tm;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.ColumnVector;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixBalancerRM;
import com.pb.common.matrix.RowVector;
import com.pb.common.util.ResourceUtil;
import org.sandag.htm.processFAF.*;
import com.pb.sawdust.util.concurrent.DnCRecursiveAction;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * SANDAG external truck model
 * Class to disaggregate FAF flows from FAF zones to counties
 * Author: Rolf Moeckel, PB Albuquerque
 * Date:   6 March 2013 (Santa Fe, NM)
 * Version 1.0
 * 
 * Modified 2017-12-28 to use FAF4
 */

public class SandagCountyModel {

    private static Logger logger = Logger.getLogger(SandagCountyModel.class);
    private ReadFAF4 faf4;
    private disaggregateFlows df;
    private HashMap<String, float[][]> cntFlows;
    private Matrix[] truckFlowsSUT;
    private Matrix[] truckFlowsMUT;
    private Matrix emptySUT;
    private Matrix emptyMUT;


    public SandagCountyModel(ReadFAF4 faf4, disaggregateFlows df) {
        // constructor
        this.faf4 = faf4;
        this.df = df;

    }


    public void runSandagCountyModel () {
        // run model to disaggregate flows from FAF zones to counties

        logger.info("Model to disaggregate flows from FAF zones to counties");

        df.getUScountyEmploymentByIndustry(utilities.getRb());
        utilities.createZoneList();
        faf4.readAllData(utilities.getRb(), utilities.getYear(), "tons");

        faf4.definePortsOfEntry(utilities.getRb());
        if (utilities.getBooleanProperty("read.in.raw.faf.data", true)) extractTruckData();
        disaggregateFromFafToCounties();

        convertTonsToTrucks cttt = new convertTonsToTrucks(utilities.getRb());
        cttt.readData();
        convertTonsToTrucks(cttt);
        addEmptyTrucks();
        writeCountyTripTables();
    }


    private void extractTruckData() {
        // extract truck data and write flows to file
        logger.info("Extracting FAF truck data");
        String[] scaleTokens = ResourceUtil.getArray(utilities.getRb(), "scaling.truck.trips.tokens");
        double[] scaleValues = ResourceUtil.getDoubleArray(utilities.getRb(), "scaling.truck.trips.values");
        HashMap<String, Float> scaler = fafUtils.createScalerHashMap(scaleTokens, scaleValues);
        String truckFileNameT = ResourceUtil.getProperty(utilities.getRb(), "processed.truck.faf.data") + "_" +
                utilities.getYear();

        // create output directory if it does not exist yet
        File file = new File ("output/temp");
        if (!file.exists()) {
            boolean outputDirectorySuccessfullyCreated = file.mkdir();
            if (!outputDirectorySuccessfullyCreated) logger.warn("Could not create scenario directory output/temp/");
        }
        faf4.writeFlowsByModeAndCommodity(truckFileNameT, ModesFAF.Truck, reportFormat.internat_domesticPart, scaler);
    }


    private void disaggregateFromFafToCounties() {
        // disaggregates freight flows from FAF zoneArray to counties

        logger.info("  Disaggregating FAF data from FAF zones to counties for year " + utilities.getYear() + ".");

        int matrixSize = utilities.countyFips.length;
        cntFlows = new HashMap<>();

        float globalScale = (float) ResourceUtil.getDoubleProperty(utilities.getRb(), "overall.scaling.factor.truck");

        // regular method
        for (String com: ReadFAF4.sctgStringCommodities) {
            float[][] dummy = new float[matrixSize][matrixSize];
            cntFlows.put(com, dummy);
        }
        df.prepareCountyDataForFAFwithDetailedEmployment(utilities.getRb(), utilities.getYear(), false);
        df.scaleSelectedCounties(utilities.getRb());

        java.util.concurrent.ForkJoinPool pool = new java.util.concurrent.ForkJoinPool();
        DnCRecursiveAction action = new DissaggregateFafAction(globalScale);
        pool.execute(action);
        action.getResult();
    }


    private class DissaggregateFafAction extends DnCRecursiveAction {
        private final float globalScale;

        private DissaggregateFafAction(float globalScale) {
            super(0,ReadFAF4.sctgStringCommodities.length);
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
                int cm = ReadFAF4.sctgCommodities[comm];

                String fileName = ResourceUtil.getProperty(utilities.getRb(), "processed.truck.faf.data") + "_" + utilities.getYear();
                if (cm < 10) fileName = fileName + "_SCTG0" + cm + ".csv";
                else fileName = fileName + "_SCTG" + cm + ".csv";
                logger.info("  Working on " + fileName);
                String sctg = ReadFAF4.getSCTGname(cm);
                float[][] values = cntFlows.get(sctg);
                TableDataSet tblFlows = fafUtils.importTable(fileName);
                for (int row = 1; row <= tblFlows.getRowCount(); row++) {
                    float shortTons = tblFlows.getValueAt(row, "shortTons");
                    if (shortTons == 0) continue;
                    String dir = tblFlows.getStringValueAt(row, "flowDirection");
                    int orig = (int) tblFlows.getValueAt(row, "originFAF");
                    int dest = (int) tblFlows.getValueAt(row, "destinationFAF");
                    TableDataSet singleFlow;
                    if (dir.startsWith("import") || dir.startsWith("export")) {
                        TableDataSet poe = null;
                        // Entry through land border
                        switch (dir) {
                            case "import":
                                poe = ReadFAF4.getPortsOfEntry(orig);
                                break;
                            // Entry through marine port
                            case "import_port":
                                poe = ReadFAF4.getMarinePortsOfEntry(orig);
                                break;
                            // Entry through airport
                            case "import_airport":
                                poe = ReadFAF4.getAirPortsOfEntry(orig);
                                break;
                            // Exit through land border
                            case "export":
                                poe = ReadFAF4.getPortsOfEntry(dest);
                                break;
                            // Exit through marine port
                            case "export_port":
                                poe = ReadFAF4.getMarinePortsOfEntry(dest);
                                break;
                            // Exit through airport
                            case "export_airport":
                                poe = ReadFAF4.getAirPortsOfEntry(dest);
                                break;
                        }
                        singleFlow = df.disaggregateSingleFAFFlowThroughPOE(dir, poe, orig, dest, sctg, shortTons, 1);
                    } else singleFlow = df.disaggregateSingleFAFFlow(orig, dest, sctg, shortTons, 1);
                    for (int i = 1; i <= singleFlow.getRowCount(); i++) {
                        int oFips = (int) singleFlow.getValueAt(i, "oFips");
                        int oZone = utilities.countyFipsIndex[oFips];
                        int dFips = (int) singleFlow.getValueAt(i, "dFips");
                        int dZone = utilities.countyFipsIndex[dFips];
                        float thisFlow = singleFlow.getValueAt(i, "Tons") * globalScale;
                        values[oZone][dZone] += thisFlow;
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


    private void convertTonsToTrucks (convertTonsToTrucks cttt) {
        // convert flows in tons into flows in trucks using average payload factors

        logger.info("  Converting tons into trucks");

        int highestGroupCode = utilities.getHighestVal(utilities.getCommodityGroupOfSCTG());
        truckFlowsSUT = new Matrix[highestGroupCode + 1];
        truckFlowsMUT = new Matrix[highestGroupCode + 1];
        float aawdtFactor = (float) ResourceUtil.getDoubleProperty(utilities.getRb(), "AADT.to.AAWDT.factor");
        for (int i = 0; i <= highestGroupCode; i++) {
            truckFlowsSUT[i] = createCountyMatrix();
            truckFlowsMUT[i] = createCountyMatrix();
        }

        for (String com: ReadFAF4.sctgStringCommodities) {
            int comGroup = utilities.getCommodityGroupOfSCTG()[Integer.parseInt(com.substring(4))];
            float[][] flowsThisCommodity = cntFlows.get(com);
            for (int oFips: utilities.countyFips) {
                for (int dFips: utilities.countyFips) {
                    int oZone = utilities.countyFipsIndex[oFips];
                    int dZone = utilities.countyFipsIndex[dFips];
                    float distance = df.getCountyDistance(oFips, dFips);
                    float truckByType[] = cttt.convertThisFlowFromTonsToTrucks(com, distance, flowsThisCommodity[oZone][dZone]);
                    float oldValueSUT = truckFlowsSUT[comGroup].getValueAt(oFips, dFips);
                    float newValueSut = truckByType[0] / 365.25f * aawdtFactor;
                    truckFlowsSUT[comGroup].setValueAt(oFips, dFips, oldValueSUT + newValueSut);
                    float oldValueMUT = truckFlowsMUT[comGroup].getValueAt(oFips, dFips);
                    float newValueMUT = (truckByType[1] + truckByType[2] + truckByType[3]) / 365.25f * aawdtFactor;
                    truckFlowsMUT[comGroup].setValueAt(oFips, dFips, oldValueMUT + newValueMUT);
                }
            }
        }
    }


    private Matrix createCountyMatrix() {
        Matrix mat = new Matrix(utilities.countyFips.length, utilities.countyFips.length);
        mat.setExternalNumbersZeroBased(utilities.countyFips);
        return mat;
    }

    private void addEmptyTrucks() {
        // Empty truck model to ensure balanced truck volumes entering and leaving every zone

        double emptyRate = 1f - ResourceUtil.getDoubleProperty(utilities.getRb(), "empty.truck.rate");

        int highestGroupCode = utilities.getHighestVal(utilities.getCommodityGroupOfSCTG());
        double[] balSut = new double[utilities.countyFips.length];
        double[] balMut = new double[utilities.countyFips.length];
        Matrix loadedSutTot = createCountyMatrix();
        Matrix loadedMutTot = createCountyMatrix();
        for (int orig = 0; orig < utilities.countyFips.length; orig++) {
            for (int dest = 0; dest < utilities.countyFips.length; dest++) {
                for (int comGroup = 0; comGroup <= highestGroupCode; comGroup++) {
                    float sut = truckFlowsSUT[comGroup].getValueAt(utilities.countyFips[orig], utilities.countyFips[dest]);
                    float mut = truckFlowsMUT[comGroup].getValueAt(utilities.countyFips[orig], utilities.countyFips[dest]);
                    balSut[orig] -= sut;
                    balSut[dest] += sut;
                    balMut[orig] -= mut;
                    balMut[dest] += mut;
                    loadedSutTot.setValueAt(utilities.countyFips[orig], utilities.countyFips[dest],
                            (loadedSutTot.getValueAt(utilities.countyFips[orig], utilities.countyFips[dest]) + sut));
                    loadedMutTot.setValueAt(utilities.countyFips[orig], utilities.countyFips[dest],
                            (loadedMutTot.getValueAt(utilities.countyFips[orig], utilities.countyFips[dest]) + mut));
                }
            }
        }
        Matrix emptyBalancedSut = balanceEmpties(balSut);
        Matrix emptyBalancedMut = balanceEmpties(balMut);
        double targetSut = loadedSutTot.getSum() / emptyRate;
        double targetMut = loadedMutTot.getSum() / emptyRate;
        double emptySutRetTot = emptyBalancedSut.getSum();
        double emptyMutRetTot = emptyBalancedMut.getSum();

        logger.info("  Trucks generated by commodity flows: " + Math.round(loadedSutTot.getSum()) + " SUT and " +
                Math.round(loadedMutTot.getSum()) + " MUT.");
        logger.info("  Empty trucks generated by balancing: " + Math.round((float) emptySutRetTot) + " SUT and " +
                Math.round((float) emptyMutRetTot) + " MUT.");
        double correctedEmptyTruckRate = emptyRate + (emptySutRetTot + emptyMutRetTot) / (targetSut + targetMut);
        if (correctedEmptyTruckRate < 0) logger.warn("Empty truck rate for returning trucks is with " +
                utilities.rounder(((emptySutRetTot + emptyMutRetTot) / (targetSut + targetMut)), 2) +
                " greater than global empty-truck rate of " + utilities.rounder(emptyRate, 2));
        logger.info("  Empty trucks added by statistics:    " + Math.round((float) ((1 - correctedEmptyTruckRate) * targetSut)) +
                " SUT and " + Math.round((float) ((1 - correctedEmptyTruckRate) * targetMut)) + " MUT.");

        emptySUT = createCountyMatrix();
        emptyMUT = createCountyMatrix();
        for (int origin : utilities.countyFips) {
            for (int destination : utilities.countyFips) {
                float emptySutReturn = emptyBalancedSut.getValueAt(destination, origin);  // note: orig and dest are switched to get return trip
                float emptyMutReturn = emptyBalancedMut.getValueAt(destination, origin);  // note: orig and dest are switched to get return trip
                double emptySutStat = (loadedSutTot.getValueAt(origin, destination) + emptySutReturn) / correctedEmptyTruckRate -
                        (loadedSutTot.getValueAt(origin, destination) + emptySutReturn);
                double emptyMutStat = (loadedMutTot.getValueAt(origin, destination) + emptyMutReturn) / correctedEmptyTruckRate -
                        (loadedMutTot.getValueAt(origin, destination) + emptyMutReturn);

                emptySUT.setValueAt(origin, destination, (float) (emptySutReturn + emptySutStat));
                emptyMUT.setValueAt(origin, destination, (float) (emptyMutReturn + emptyMutStat));
            }
        }
    }


    private Matrix balanceEmpties(double[] trucks) {
        // generate empty truck trips

        RowVector emptyTruckDest = new RowVector(utilities.countyFips.length);
        emptyTruckDest.setExternalNumbersZeroBased(utilities.countyFips);
        ColumnVector emptyTruckOrig = new ColumnVector(utilities.countyFips.length);
        emptyTruckOrig.setExternalNumbersZeroBased(utilities.countyFips);
        for (int zn = 0; zn < utilities.countyFips.length; zn++) {
            if (trucks[zn] > 0) {
                emptyTruckDest.setValueAt(utilities.countyFips[zn], (float) trucks[zn]);
                emptyTruckOrig.setValueAt(utilities.countyFips[zn], 0f);
            }
            else {
                emptyTruckOrig.setValueAt(utilities.countyFips[zn], (float) trucks[zn]);
                emptyTruckDest.setValueAt(utilities.countyFips[zn], 0f);
            }
        }
        Matrix seed = createCountyMatrix();
        for (int o: utilities.countyFips) {
            for (int d: utilities.countyFips) {
                float friction = (float) Math.exp(-0.001 * df.getCountyDistance(o, d));
                seed.setValueAt(o, d, friction);
            }
        }
        MatrixBalancerRM mb = new MatrixBalancerRM(seed, emptyTruckOrig, emptyTruckDest, 0.001, 10, MatrixBalancerRM.ADJUST.BOTH_USING_AVERAGE);
        return mb.balance();
    }


    private void writeCountyTripTables() {
        // write out county-to-county trip tables

        logger.info("  Writing county-to-county truck trip table");
        String fileName = utilities.getRb().getString("county.to.county.trip.table") + "_" + utilities.getYear() + ".csv";
        PrintWriter pw = fafUtils.openFileForSequentialWriting(fileName);
        pw.println("origFips,destFips,sut1,sut2,sut3,sut4,sut5,sut6,emptySut,mut1,mut2,mut3,mut4,mut5,mut6,emptyMut");
        for (int oFips: utilities.countyFips) {
            for (int dFips: utilities.countyFips) {
                pw.print(oFips+","+dFips);
                for (int i = 1; i < truckFlowsSUT.length; i++) pw.print("," + truckFlowsSUT[i].getValueAt(oFips,dFips));
                pw.print("," + emptySUT.getValueAt(oFips, dFips));
                for (int i = 1; i < truckFlowsMUT.length; i++) pw.print("," + truckFlowsMUT[i].getValueAt(oFips,dFips));
                pw.println("," + emptyMUT.getValueAt(oFips, dFips));
            }
        }
        pw.close();
    }



}
