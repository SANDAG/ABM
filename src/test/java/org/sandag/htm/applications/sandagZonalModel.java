package org.sandag.htm.applications;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;
import org.sandag.htm.processFAF.disaggregateFlows;
import org.sandag.htm.processFAF.fafUtils;
import com.pb.sawdust.calculator.Function1;
import com.pb.sawdust.util.array.ArrayUtil;
import com.pb.sawdust.util.concurrent.ForkJoinPoolFactory;
import com.pb.sawdust.util.concurrent.IteratorAction;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * SANDAG external truck model
 * Class to disaggregate FAF flows from counties to SANDAG zones
 * Author: Rolf Moeckel, PB Albuquerque
 * Date:   6 March 2013 (Santa Fe, NM)
 * Version 1.0
 */

public class sandagZonalModel {

    private static Logger logger = Logger.getLogger(sandagZonalModel.class);
    private int[] zones;
    private HashMap<String, Float> useHshLocal;
    private HashMap<String, Float> makeHshLocal;
    private String[] industries;
    private float[][] zonalEmployment;
    private TableDataSet countyFlows;
    private int[] sandagExtStatID;
    private boolean[] sandagExtStat_border;
    private int[] sanDiegoNodes;
    private final HashMap<Integer, TableDataSet> disaggregatedFlows = new HashMap<>();
    private String[] todNames;
    private float[][] todSutShare;
    private float[][] todMutShare;


    public sandagZonalModel() {
        // constructor
    }


    public void runSandagZonalModel () {
        // run model to disaggregate flows from counties to SANDAG zones

        logger.info("Model to disaggregate flows from counties to zones");

        readInputData();
        disaggregateFlowsFromCountiesToZones();
        writeOutDisaggregatedFlows();
    }


    private void readInputData() {
        // read input data

        logger.info("  Reading input data");
        // define TAZ and MGRA system
        TableDataSet zonesTbl = fafUtils.importTable(utilities.getRb().getString("local.zones"));
        zones = zonesTbl.getColumnAsInt("TAZ");
        TableDataSet mgraZones = fafUtils.importTable(utilities.getRb().getString("local.mgra.zones"));
        int[] mgras = mgraZones.getColumnAsInt("mgra13");
        int[] mgraTazReference = new int[fafUtils.getHighestVal(mgras) + 1];
        for (int row = 1; row <= mgraZones.getRowCount(); row++) {
            mgraTazReference[(int) mgraZones.getValueAt(row, "mgra13")] = (int) mgraZones.getValueAt(row, "taz13");
        }

        // process local make/use coefficients
        String useToken = "faf.use.coefficients.local";
        String makeToken = "faf.make.coefficients.local";
        useHshLocal = disaggregateFlows.createMakeUseHashMap(utilities.getRb(), useToken);
        makeHshLocal = disaggregateFlows.createMakeUseHashMap(utilities.getRb(), makeToken);
        TableDataSet useCoeff = fafUtils.importTable(utilities.getRb().getString(useToken));
        industries = useCoeff.getColumnAsString("Industry");

        // read local employment data
        TableDataSet employmentMGRA = fafUtils.importTable(utilities.getRb().getString("local.employment.data") +
                utilities.getYear() + ".csv");
        zonalEmployment = new float[utilities.getHighestVal(zones)+1][industries.length];
        for (int row = 1; row <= employmentMGRA.getRowCount(); row++) {
            int mgra = (int) employmentMGRA.getValueAt(row, "mgra");
            int taz = mgraTazReference[mgra];
            for (int ind = 0; ind < industries.length; ind++) {
                zonalEmployment[taz][ind] += employmentMGRA.getValueAt(row, industries[ind]);
            }
        }

        // read external station IDs
        TableDataSet extStations = fafUtils.importTable(utilities.getRb().getString("external.station.definition"));
        sandagExtStatID = new int[utilities.getHighestVal(extStations.getColumnAsInt("natExtStat")) + 1];
        sandagExtStat_border = new boolean[utilities.getHighestVal(zones) + 1];
        for (int row = 1; row <= extStations.getRowCount(); row++) {
            sandagExtStatID[(int) extStations.getValueAt(row, "natExtStat")] =
                    (int) extStations.getValueAt(row, "sandagExtStat");
            sandagExtStat_border[(int) extStations.getValueAt(row, "sandagExtStat")] =
                    extStations.getBooleanValueAt(row, "borderCrossing");
        }

        // read flows at external stations
        String fileName = utilities.getRb().getString("external.station.flows");
        countyFlows = utilities.importTableFromDBF(fileName);
        if (countyFlows.getColumnCount() != 16) logger.error("Excepted 16 but found " + countyFlows.getColumnCount() +
                " columns in " + fileName);
        String[] expectedLabels = {"SUBAREA_NO","SUBAREA_N1","DEMAND_SUT","DEMAND_SU1","DEMAND_SU2","DEMAND_SU3","DEMAND_SU4","DEMAND_SU5","DEMAND_EMP","DEMAND_MUT","DEMAND_MU1","DEMAND_MU2","DEMAND_MU3","DEMAND_MU4","DEMAND_MU5","DEMAND_EM1"};
        String[] actualLabels = countyFlows.getColumnLabels();
        boolean wrongHeader = false;
        for (int col = 0; col < countyFlows.getColumnCount(); col++) {
            if (!actualLabels[col].equalsIgnoreCase(expectedLabels[col]))
                wrongHeader = true;
        }
        if (wrongHeader) {
            logger.error("File " + fileName + " has unexpected headers:");
            logger.info("Column,ExpectedLabel,ActualLabel");
            for (int col = 0; col < countyFlows.getColumnCount(); col++)
                logger.info(col+1 + "," + expectedLabels[col] + "," + actualLabels[col] + "," + (expectedLabels[col].equals(actualLabels[col])));
            System.exit(1);
        }
        countyFlows.setColumnLabels(new String[]{"orig","dest","sut1","sut2","sut3","sut4","sut5","sut6","sutEmpty",
                "mut1","mut2","mut3","mut4","mut5","mut6","mutEmpty"});

        sanDiegoNodes = ResourceUtil.getIntegerArray(utilities.getRb(), "internal.nodes.san.diego");

        // read time-of-day shares
        TableDataSet TODValuesGeneral = fafUtils.importTable(utilities.getRb().getString("time.of.day.shares.general"));
        TableDataSet TODValuesBorder = fafUtils.importTable(utilities.getRb().getString("time.of.day.shares.border"));
        todNames = TODValuesGeneral.getColumnAsString("DESCRIPTION");
        todSutShare = new float[2][TODValuesGeneral.getRowCount()];
        todMutShare = new float[2][TODValuesGeneral.getRowCount()];
        float[] checkSum = new float[4] ;
        for (int row = 1; row <= TODValuesGeneral.getRowCount(); row++) {
            todSutShare[0][row-1] = TODValuesGeneral.getValueAt(row, "ShareSUT");
            todMutShare[0][row-1] = TODValuesGeneral.getValueAt(row, "ShareMUT");
            todSutShare[1][row-1] = TODValuesBorder.getValueAt(row, "ShareSUT");
            todMutShare[1][row-1] = TODValuesBorder.getValueAt(row, "ShareMUT");
            checkSum[0] += todSutShare[0][row-1];
            checkSum[1] += todMutShare[0][row-1];
            checkSum[2] += todSutShare[1][row-1];
            checkSum[3] += todMutShare[1][row-1];
        }
        if (checkSum[0] > 1.001 || checkSum[0] < 0.999) logger.warn("Time of day share for SUT (general) does not add up to 1 but " + checkSum[0]);
        if (checkSum[1] > 1.001 || checkSum[1] < 0.999) logger.warn("Time of day share for MUT (general) does not add up to 1 but " + checkSum[1]);
        if (checkSum[2] > 1.001 || checkSum[2] < 0.999) logger.warn("Time of day share for SUT (border) does not add up to 1 but " + checkSum[2]);
        if (checkSum[3] > 1.001 || checkSum[3] < 0.999) logger.warn("Time of day share for MUT (border) does not add up to 1 but " + checkSum[3]);
    }


    private void disaggregateFlowsFromCountiesToZones() {
        // calculate local weights and disaggregate flows from counties/external stations to zones/external stations

        final HashMap<String, double[]> weights = prepareZonalWeights();
        logger.info("  Disaggregating flows to zones");

        int[] listOfCommodityGroupsPlusEmpties = new int[utilities.getListOfCommodityGroups().length + 1];
        listOfCommodityGroupsPlusEmpties[0] = 0;       // category for empty trucks
        System.arraycopy(utilities.getListOfCommodityGroups(), 0, listOfCommodityGroupsPlusEmpties, 1, utilities.getListOfCommodityGroups().length);
        Integer[] list = new Integer[listOfCommodityGroupsPlusEmpties.length];
        for (int i = 0; i < listOfCommodityGroupsPlusEmpties.length; i++) list[i] = listOfCommodityGroupsPlusEmpties[i];
        Function1<Integer,Void> commodityDisaggregationFunctionFAF = new Function1<Integer,Void>() {
            public Void apply(Integer com) {
                processCommodityDisaggregation(com, weights);
                return null;
            }
        };

        Iterator<Integer> commodityIterator = ArrayUtil.getIterator(list);
        IteratorAction<Integer> itTask = new IteratorAction<>(commodityIterator, commodityDisaggregationFunctionFAF);
        ForkJoinPool pool = ForkJoinPoolFactory.getForkJoinPool();
        pool.execute(itTask);
        itTask.waitForCompletion();
    }




    private HashMap<String, double[]> prepareZonalWeights() {
        // prepare zonal weights based on employment by industry

        logger.info("  Calculating zonal weights");
        HashMap<String, double[]> weights = new HashMap<>();

        double[] makeEmpty = new double[zones.length];
        double[] useEmpty = new double[zones.length];

        for (int comGrp: utilities.getListOfCommodityGroups()) {
            double[] makeWeight = new double[zones.length];
            double[] useWeight = new double[zones.length];
            for (int com: utilities.getComGroupDefinition().get(comGrp)) {
                for (int iz = 0; iz < zones.length; iz++) {
                    int zn = zones[iz];
                    for (int ind = 0; ind < industries.length; ind++) {
                        String industry = industries[ind];
                        String code;
                        if (com <= 9) code = industry + "_SCTG0" + com;
                        else code = industry + "_SCTG" + com;
                        makeWeight[iz] += zonalEmployment[zn][ind] * makeHshLocal.get(code);
                        useWeight[iz] += zonalEmployment[zn][ind] * useHshLocal.get(code);
                        makeEmpty[iz] += zonalEmployment[zn][ind] * makeHshLocal.get(code);
                        useEmpty[iz] += zonalEmployment[zn][ind] * useHshLocal.get(code);
                    }
                }
            }
            String mCode = comGrp + "_make";
            weights.put(mCode, makeWeight);
            String uCode = comGrp + "_use";
            weights.put(uCode, useWeight);
        }
        weights.put("0_make", makeEmpty);
        weights.put("0_use", useEmpty);
        return weights;
    }



    private boolean checkIfCountyInSanDiego(int node) {
        // check if county is either San Diego County of San Diego Phantom County

        boolean insideSandag = false;
        for (int i: sanDiegoNodes) if (i == node) insideSandag = true;
        return insideSandag;
    }


    private void processCommodityDisaggregation(Integer comGroup, Map<String,double[]> weights) {
        // Disaggregate a single commodity from county-to-county flows to zone-to-zone flows
        logger.info("  Processing commodity group " + comGroup);

        ArrayList<Integer> origAL = new ArrayList<>();
        ArrayList<Integer> destAL = new ArrayList<>();
        ArrayList<Float> flowSutAL = new ArrayList<>();
        ArrayList<Float> flowMutAL = new ArrayList<>();
        for (int row = 1; row <= countyFlows.getRowCount(); row ++) {
            int orig = (int) countyFlows.getValueAt(row, "orig");
            int dest = (int) countyFlows.getValueAt(row, "dest");
            String sutLabel;
            if (comGroup == 0) sutLabel = "sutEmpty";
            else sutLabel = "sut" + comGroup;
            float sutTrk = countyFlows.getValueAt(row, sutLabel);
            String mutLabel;
            if (comGroup == 0) mutLabel = "mutEmpty";
            else mutLabel = "mut" + comGroup;
            float mutTrk = countyFlows.getValueAt(row, mutLabel);

            if (checkIfCountyInSanDiego(orig) && checkIfCountyInSanDiego(dest)) {
                // flows from San Diego County to San Diego Phantom County or vice versa
                // ignore as these flows are internal to SANDAG and known to be underestimated
            } else if (checkIfCountyInSanDiego(orig)) {
                // flows from San Diego to elsewhere
                double[] makeShare = weights.get(comGroup + "_make");
                double makeShareSum = utilities.getSum(makeShare);
                for (int zone = 0; zone < zones.length; zone++) {
                    origAL.add(zones[zone]);
                    destAL.add(sandagExtStatID[dest]);
                    flowSutAL.add((float) (sutTrk * makeShare[zone] / makeShareSum));
                    flowMutAL.add((float) (mutTrk * makeShare[zone] / makeShareSum));
                }
            } else if (checkIfCountyInSanDiego(dest)) {
                // flows from elsewhere to San Diego
                double[] useShare = weights.get(comGroup + "_use");
                double useShareSum = utilities.getSum(useShare);
                for (int zone = 0; zone < zones.length; zone++) {
                    origAL.add(sandagExtStatID[orig]);
                    destAL.add(zones[zone]);
                    flowSutAL.add((float) (sutTrk * useShare[zone] / useShareSum));
                    flowMutAL.add((float) (mutTrk * useShare[zone] / useShareSum));
                }
            } else {
                // through flows through San Diego
                origAL.add(sandagExtStatID[orig]);
                destAL.add(sandagExtStatID[dest]);
                flowSutAL.add(sutTrk);
                flowMutAL.add(mutTrk);
            }

            TableDataSet disFlows = new TableDataSet();
            disFlows.appendColumn(utilities.convertIntArrayListToArray(origAL), "orig");
            disFlows.appendColumn(utilities.convertIntArrayListToArray(destAL), "dest");
            disFlows.appendColumn(utilities.convertFloatArrayListToArray(flowSutAL), "sut");
            disFlows.appendColumn(utilities.convertFloatArrayListToArray(flowMutAL), "mut");

            synchronized (disaggregatedFlows) {
                disaggregatedFlows.put(comGroup, disFlows);
            }
        }
    }


    private void writeOutDisaggregatedFlows () {
        // write out disaggregated flows to csv file

        logger.info("  Writing zone-to-external station truck trip table");
        String fileName = utilities.getRb().getString("zone.to.ext.stat.trip.table") + "_" + utilities.getYear() + ".csv";
        PrintWriter pw = fafUtils.openFileForSequentialWriting(fileName);
        pw.print("orig,dest");
        for (String tod: todNames) pw.print(",SUT " + tod);
        for (String tod: todNames) pw.print(",MUT " + tod);
        pw.println();

        HashMap<String, float[]> summarizedFlows = new HashMap<>();

        for (int comGrp = 0; comGrp <= utilities.getHighestVal(utilities.getListOfCommodityGroups()); comGrp++) {
            TableDataSet flows = disaggregatedFlows.get(comGrp);

            for (int row = 1; row <= flows.getRowCount(); row++) {
                String key = (int) flows.getValueAt(row, "orig") + "," + (int) flows.getValueAt(row, "dest");
                float[] values;
                if (summarizedFlows.containsKey(key)) {
                    values = summarizedFlows.get(key);
                } else {
                    values = new float[]{0,0};
                }
                values[0] += flows.getValueAt(row, "sut");
                values[1] += flows.getValueAt(row, "mut");
                summarizedFlows.put(key, values);
            }
        }

        for (String key: summarizedFlows.keySet()) {
            float[] values = summarizedFlows.get(key);
            pw.print(key);

            // check if flow crosses border with Mexico, in which case different time-of-day split is used
            String[] odPair = key.split(",");
            int border = 0;
            if (sandagExtStat_border[Integer.parseInt(odPair[0])] ||
                    sandagExtStat_border[Integer.parseInt(odPair[1])]) border = 1;

            for (int tod = 0; tod < todNames.length; tod++) pw.print("," + values[0] * todSutShare[border][tod]);
            for (int tod = 0; tod < todNames.length; tod++) pw.print("," + values[1] * todMutShare[border][tod]);
            pw.println();
        }
        pw.close();
    }
}
