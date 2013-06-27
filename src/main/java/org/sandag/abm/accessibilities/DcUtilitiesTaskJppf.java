package org.sandag.abm.accessibilities;

import com.pb.common.util.Tracer;
import com.pb.common.datafile.TableDataSet;

import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.Util;
import com.pb.common.newmodel.UtilityExpressionCalculator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TransitWalkAccessUEC;

public class DcUtilitiesTaskJppf
        implements Callable<List<Object>>
{

    private static final int MIN_EXP_FUNCTION_ARGUMENT = -500;
    
	private static final String[] LOGSUM_SEGMENTS = {
		"SOV       ", "HOV       ", "Transit   ", "NMotorized", "SOVLS_0   ", "SOVLS_1   ",
		"SOVLS_2   ", "HOVLS_0_OP", "HOVLS_1_OP", "HOVLS_2_OP", "HOVLS_0_PK", "HOVLS_1_PK",
		"HOVLS_2_PK", "TOTAL"
	};
	
	public static final String[] LU_LOGSUM_SEGMENTS = {
		"LS_0_PK", "LS_1_PK", "LS_2_PK", "LS_0_OP", "LS_1_OP", "LS_2_OP", "All_PK "
	};

	
	// setting to -1 will prevent debug files from being written
	private static final int DEBUG_ILUZ = -1; 
	private static final int DEBUG_JLUZ = -1;
	//private static final int DEBUG_ILUZ = 66; 
	//private static final int DEBUG_JLUZ = 82;

    
    private static final int MAX_LU_SIZE_TERM_INDEX = 23;
    private static final int MAX_LU_NONMAN_SIZE_TERM_INDEX = 12;
    private static final int MAX_LU_WORK_SIZE_TERM_INDEX = 18;
    private static final int MAX_LU_SCHOOL_SIZE_TERM_INDEX = 23;
	
    private MgraDataManager             mgraManager;
    private boolean[]                   hasSizeTerm;
    private double[][]                  expConstants;
    private double[][]                  sizeTerms;
    private double[][] 					luSizeTerms;
    
    // store taz-taz exponentiated utilities (period, from taz, to taz)
    private double[][][]                sovExpUtilities;
    private double[][][]                hovExpUtilities;
    private double[][][]                nMotorExpUtilities;


    
    private float[][]                   accessibilities;
    private float[][]                   luAccessibilities;

    private int                         startRange;
    private int                         endRange;
    private int                         taskIndex;

    private boolean                     seek;
    private Tracer                      tracer;
    private boolean                     trace;
    private int[]                       traceOtaz;
    private int[]                       traceDtaz;

    private BestTransitPathCalculator bestPathCalculator;
    
    private UtilityExpressionCalculator dcUEC;
    private AccessibilitiesDMU          aDmu;
    
    private UtilityExpressionCalculator luUEC;
    private AccessibilitiesDMU          luDmu;
    
    private HashMap<String, String> rbMap;

	private int[][] externalLuzsForCordonLuz;
	private int[] cordonLuzForExternalLuz;
	private int[] cordonLuzMinutesForExternalLuz;
    
    private boolean calculateLuAccessibilities;
    private PrintWriter outStream;

    public DcUtilitiesTaskJppf( int taskIndex, int startRange, int endRange,
            double[][][] mySovExpUtilities, double[][][] myHovExpUtilities, double[][][] myNMotorExpUtilities,
            boolean[] hasSizeTerm, double[][] expConstants, double[][] sizeTerms, double[][] workSizeTerms, double[][] schoolSizeTerms,
            int[][] myExternalLuzsForCordonLuz, int[] myCordonLuzForExternalLuz, int[] myCordonLuzMinutesForExternalLuz,            
            HashMap<String, String> myRbMap, boolean myCalculateLuAccessibilities )
    {

        rbMap = myRbMap;
        sovExpUtilities = mySovExpUtilities;
        hovExpUtilities = myHovExpUtilities;
        nMotorExpUtilities = myNMotorExpUtilities;
        externalLuzsForCordonLuz = myExternalLuzsForCordonLuz;
        cordonLuzForExternalLuz = myCordonLuzForExternalLuz;
        cordonLuzMinutesForExternalLuz = myCordonLuzMinutesForExternalLuz;
        calculateLuAccessibilities = myCalculateLuAccessibilities;
        
        
        mgraManager = MgraDataManager.getInstance(rbMap);
                
        aDmu = new AccessibilitiesDMU();

        String dcUecFileName = Util.getStringValueFromPropertyMap(rbMap, "acc.dcUtility.uec.file");
        int dcDataPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.dcUtility.data.page");
        int dcUtilityPage = Util.getIntegerValueFromPropertyMap(rbMap, "acc.dcUtility.page");
        
        File dcUecFile = new File(dcUecFileName);
        dcUEC = new UtilityExpressionCalculator(dcUecFile, dcUtilityPage, dcDataPage, rbMap, aDmu);

        accessibilities = new float[mgraManager.getMaxMgra() + 1][];

        if ( calculateLuAccessibilities ) {

            luDmu = new AccessibilitiesDMU();

            dcUecFileName = Util.getStringValueFromPropertyMap(rbMap, "lu.acc.dcUtility.uec.file");
            dcDataPage = Util.getIntegerValueFromPropertyMap(rbMap, "lu.acc.dcUtility.data.page");
            dcUtilityPage = Util.getIntegerValueFromPropertyMap(rbMap, "lu.acc.dcUtility.page");
            
            File luUecFile = new File(dcUecFileName);
            luUEC = new UtilityExpressionCalculator(luUecFile, dcUtilityPage, dcDataPage, rbMap, luDmu);

            TableDataSet luAltData = luUEC.getAlternativeData();
	        luDmu.setAlternativeData(luAltData);
	        int luAlts = luUEC.getNumberOfAlternatives();

	        luAccessibilities = new float[mgraManager.getMaxMgra() + 1][luAlts+1];

	        // combine non-mandatory, work, and school size terms into one array to be indexed into as follows
	        // 0-12 non-mandatory, 13-18 work, 19-23 school
	        luSizeTerms = new double[sizeTerms.length][MAX_LU_SIZE_TERM_INDEX+1];
	        for ( int c=0; c <= MAX_LU_NONMAN_SIZE_TERM_INDEX; c++ )
		        for ( int r=0; r < sizeTerms.length; r++ )
		        	luSizeTerms[r][c] = sizeTerms[r][c];

	        for ( int c=0; c < workSizeTerms.length; c++ )
		        for ( int r=0; r < workSizeTerms[c].length; r++ )
		        	luSizeTerms[r][c+MAX_LU_NONMAN_SIZE_TERM_INDEX+1] = workSizeTerms[c][r];

	        for ( int c=0; c < schoolSizeTerms.length; c++ )
		        for ( int r=0; r < schoolSizeTerms[c].length; r++ )
		        	luSizeTerms[r][c+MAX_LU_WORK_SIZE_TERM_INDEX+1] = schoolSizeTerms[c][r];
	        
	        
	        
//	        for ( int c=MAX_LU_NONMAN_SIZE_TERM_INDEX+1; c <= MAX_LU_WORK_SIZE_TERM_INDEX; c++ )
//		        for ( int r=0; r < workSizeTerms.length; r++ )
//		        	luSizeTerms[r][c] = workSizeTerms[r][c-MAX_LU_NONMAN_SIZE_TERM_INDEX-1];
//
//	        for ( int c=MAX_LU_WORK_SIZE_TERM_INDEX+1; c <= MAX_LU_SCHOOL_SIZE_TERM_INDEX; c++ )
//		        for ( int r=0; r < schoolSizeTerms.length; r++ )
//		        	luSizeTerms[r][c] = schoolSizeTerms[r][c-MAX_LU_WORK_SIZE_TERM_INDEX-1];

	        
//	        try {
//	            outStream = new PrintWriter( new BufferedWriter( new FileWriter( "landUseModeChoiceLogsumCheck" + "_" + taskIndex + ".csv" ) ) );
//	        }
//	        catch (IOException e) {
//	            System.out.println("IO Exception writing file for checking integerizing procedure: " );
//	            e.printStackTrace();
//	            System.exit(-1);
//	        }

        }
        

        
        this.taskIndex = taskIndex;
        this.startRange = startRange;
        this.endRange = endRange;
        this.hasSizeTerm = hasSizeTerm;
        this.expConstants = expConstants;
        this.sizeTerms = sizeTerms;
        

        
        trace = Util.getBooleanValueFromPropertyMap(rbMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if ( trace )
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        seek = Util.getBooleanValueFromPropertyMap(rbMap, "Seek");
        
    }

    
    
    public String getId()
    {
        return Integer.toString(taskIndex);
    }

    public List<Object> call()
    {

        Logger logger = Logger.getLogger(this.getClass());

        String threadName = null;
        try
        {
            threadName = "[" + java.net.InetAddress.getLocalHost().getHostName() + ", task:"
                    + taskIndex + "] " + Thread.currentThread().getName();
        } catch (UnknownHostException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        logger.info(threadName + " - Calculating Accessibilities");

        NonTransitUtilities ntUtilities = new NonTransitUtilities(rbMap, sovExpUtilities, hovExpUtilities, nMotorExpUtilities);
//        ntUtilities.setAllUtilities(ntUtilitiesArrays);
//        ntUtilities.setNonMotorUtilsMap(ntUtilitiesMap);

        McLogsumsCalculator logsumHelper = new McLogsumsCalculator();
        logsumHelper.setupSkimCalculators(rbMap);
        bestPathCalculator = logsumHelper.getBestTransitPathCalculator();

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if ( trace )
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }

        // get the accessibilities alternatives
        int alts = dcUEC.getNumberOfAlternatives();
        TableDataSet altData = dcUEC.getAlternativeData();
        aDmu.setAlternativeData(altData);



        int luAlts = -1;
        double[] luUtilities = null;
    	ArrayList<float[]> luUtilityList = null;
        float[][][][][] accumulatedLandUseLogsums = null;
        int[][] accumulatedLandUseLogsumsCount = null;
    	
        if ( calculateLuAccessibilities ) {
	        // get the land use accessibilities alternatives
	        luAlts = luUEC.getNumberOfAlternatives();
	        TableDataSet luAltData = luUEC.getAlternativeData();
	        luDmu.setAlternativeData(luAltData);

	        //declare logsums array for LU accessibility
	        luUtilities = new double[LU_LOGSUM_SEGMENTS.length];

        	luUtilityList = new ArrayList<float[]>();

	        accumulatedLandUseLogsums = new float[BuildAccessibilities.NUM_AVG_METHODS][BuildAccessibilities.NUM_PERIODS][BuildAccessibilities.NUM_SUFFICIENCY_SEGMENTS][BuildAccessibilities.MAX_LUZ+1][BuildAccessibilities.MAX_LUZ+1];;
	        accumulatedLandUseLogsumsCount = new int[BuildAccessibilities.MAX_LUZ+1][BuildAccessibilities.MAX_LUZ+1];;

        }
        
        

        //declare logsums array for ABM
        double[] logsums = new double[LOGSUM_SEGMENTS.length];

        float[] luUtilityResult = new float[LU_LOGSUM_SEGMENTS.length + 2];

        
        // LOOP OVER RANGE OF ORIGIN MGRA
        ArrayList<Integer> mgraValues = mgraManager.getMgras();
        for (int i = startRange; i <= endRange; i++)
        { // Origin MGRA

            int iMgra = mgraValues.get(i);

            accessibilities[iMgra] = new float[alts+1];
            
            // pre-calculate the hov, sov, and non-motorized exponentiated utilities for the origin MGRA.
            // the method called returns cached values if they were already calculated.
            ntUtilities.buildUtilitiesForOrigMgraAndPeriod( iMgra, NonTransitUtilities.PEAK_PERIOD_INDEX );
            ntUtilities.buildUtilitiesForOrigMgraAndPeriod( iMgra, NonTransitUtilities.OFFPEAK_PERIOD_INDEX );
            
            // if(originMgras<=10 || (originMgras % 500) ==0 )
            // logger.info("...Origin MGRA "+iMgra);

            int iTaz = mgraManager.getTaz(iMgra);
            boolean trace = false;
            
            if(tracer.isTraceOn() && tracer.isTraceZone(iTaz)){
            	
             	logger.info("origMGRA, destMGRA, OPSOV, OPHOV, WTRAN, NMOT, SOV0OP, SOV1OP, SOV2OP, HOV0OP, HOV1OP, HOV2OP, HOV0PK, HOV1PK, HOV2PK,  ALL");
             	trace = true;
            }
            //for tracing accessibility and logsum calculations
            String accString = null;

            // LOOP OVER DESTINATION MGRA
            for (Integer jMgra : mgraManager.getMgras())
            { // Destination MGRA

                if (!hasSizeTerm[jMgra]) continue;

                int jTaz = mgraManager.getTaz(jMgra);

                if (seek && !trace) continue;

                double opSovExpUtility = 0;
                double opHovExpUtility = 0;
                try
                {
                    opSovExpUtility = ntUtilities.getSovExpUtility(iTaz, jTaz, NonTransitUtilities.OFFPEAK_PERIOD_INDEX);
                    opHovExpUtility = ntUtilities.getHovExpUtility(iTaz, jTaz, NonTransitUtilities.OFFPEAK_PERIOD_INDEX);
                    //opSovExpUtility = ntUtilities.getAllUtilities()[0][0][iTaz][jTaz];
                    //opHovExpUtility = ntUtilities.getAllUtilities()[1][0][iTaz][jTaz];
                } catch (Exception e)
                {
                    logger.error("exception for op sov/hov utilitiy taskIndex=" + taskIndex
                            + ", i=" + i + ", startRange=" + startRange + ", endRange=" + endRange, e);
                    System.exit(-1);
                }

                // calculate OP walk-transit exponentiated utility
                // determine the best transit path, which also stores the best utilities array and the best mode
                bestPathCalculator.findBestWalkTransitWalkTaps( TransitWalkAccessUEC.MD, iMgra, jMgra, false, logger);
                
                // sum the exponentiated utilities over modes
                double opWTExpUtility = 0;
                double[] transitUtilities = bestPathCalculator.getBestUtilities();
                for (int k=0; k < transitUtilities.length; k++){
                    if ( transitUtilities[k] > MIN_EXP_FUNCTION_ARGUMENT )
                        opWTExpUtility += Math.exp(transitUtilities[k]);
                }


                // calculate OP drive-transit exponentiated utility
                // determine the best transit path, which also stores the best utilities array and the best mode
                bestPathCalculator.findBestDriveTransitWalkTaps( TransitWalkAccessUEC.MD, iMgra, jMgra, false, logger);
                
                // sum the exponentiated utilities over modes
                double opDTExpUtility = 0;
                transitUtilities = bestPathCalculator.getBestUtilities();
                for (int k=0; k < transitUtilities.length; k++){
                    if ( transitUtilities[k] > MIN_EXP_FUNCTION_ARGUMENT )
                        opDTExpUtility += Math.exp(transitUtilities[k]);
                }


                double pkSovExpUtility = 0;
                double pkHovExpUtility = 0;
                try
                {
                    pkSovExpUtility = ntUtilities.getSovExpUtility(iTaz, jTaz, NonTransitUtilities.PEAK_PERIOD_INDEX);
                    pkHovExpUtility = ntUtilities.getHovExpUtility(iTaz, jTaz, NonTransitUtilities.PEAK_PERIOD_INDEX);
                    //pkSovExpUtility = ntUtilities.getAllUtilities()[0][1][iTaz][jTaz];
                    //pkHovExpUtility = ntUtilities.getAllUtilities()[1][1][iTaz][jTaz];
                } catch (Exception e)
                {
                    logger.error("exception for pk sov/hov utility taskIndex=" + taskIndex + ", i="
                            + i + ", startRange=" + startRange + ", endRange=" + endRange, e);
                    System.exit(-1);
                }

                
                // calculate PK walk-transit exponentiated utility
                // determine the best WTW transit path, which also stores the best utilities array and the best mode
                bestPathCalculator.findBestWalkTransitWalkTaps( TransitWalkAccessUEC.AM, iMgra, jMgra, false, logger);
                
                // sum the exponentiated utilities over modes
                double pkWTExpUtility = 0;
                transitUtilities = bestPathCalculator.getBestUtilities();
                for (int k=0; k < transitUtilities.length; k++){
                    if ( transitUtilities[k] > MIN_EXP_FUNCTION_ARGUMENT )
                        pkWTExpUtility += Math.exp(transitUtilities[k]);
                }

                
                // calculate PK drive-transit exponentiated utility
                // determine the best DTW transit path, which also stores the best utilities array and the best mode
                bestPathCalculator.findBestDriveTransitWalkTaps( TransitWalkAccessUEC.AM, iMgra, jMgra, false, logger);
                
                // sum the exponentiated utilities over modes
                double pkDTExpUtility = 0;
                transitUtilities = bestPathCalculator.getBestUtilities();
                for (int k=0; k < transitUtilities.length; k++){
                    if ( transitUtilities[k] > MIN_EXP_FUNCTION_ARGUMENT )
                        pkDTExpUtility += Math.exp(transitUtilities[k]);
                }

                
                double nmExpUtility = 0;
                try
                {
                    nmExpUtility = ntUtilities.getNMotorExpUtility(iMgra, jMgra, NonTransitUtilities.OFFPEAK_PERIOD_INDEX);
                } catch (Exception e)
                {
                    logger.error("exception for non-motorized utilitiy taskIndex=" + taskIndex
                            + ", i=" + i + ", startRange=" + startRange + ", endRange=" + endRange, e);
                    System.exit(-1);
                }

                Arrays.fill(logsums, -999f);

                // 0: OP SOV
                logsums[0] = Math.log(opSovExpUtility);

                // 1: OP HOV
                logsums[1] = Math.log(opHovExpUtility);

                // 2: Walk-Transit
                if (opWTExpUtility > 0) logsums[2] = Math.log(opWTExpUtility);

                // 3: Non-Motorized
                if (nmExpUtility > 0) logsums[3] = Math.log(nmExpUtility);

                // 4: SOVLS_0
                logsums[4] = Math.log(opSovExpUtility * expConstants[0][0] + opWTExpUtility
                        * expConstants[0][2] + nmExpUtility * expConstants[0][3]);
                // 5: SOVLS_1
                logsums[5] = Math.log(opSovExpUtility * expConstants[1][0] + opWTExpUtility
                        * expConstants[1][2] + nmExpUtility * expConstants[1][3]);

                // 6: SOVLS_2
                logsums[6] = Math.log(opSovExpUtility * expConstants[2][0] + opWTExpUtility
                        * expConstants[2][2] + nmExpUtility * expConstants[2][3]);

                // 7: HOVLS_0_OP
                logsums[7] = Math.log(opHovExpUtility * expConstants[0][1] + opWTExpUtility
                        * expConstants[0][2] + nmExpUtility * expConstants[0][3]);

                // 8: HOVLS_1_OP
                logsums[8] = Math.log(opHovExpUtility * expConstants[1][1] + opWTExpUtility
                        * expConstants[1][2] + nmExpUtility * expConstants[1][3]);

                // 9: HOVLS_2_OP
                logsums[9] = Math.log(opHovExpUtility * expConstants[2][1] + opWTExpUtility
                        * expConstants[2][2] + nmExpUtility * expConstants[2][3]);

                // 10: HOVLS_0_PK
                logsums[10] = Math.log(pkHovExpUtility * expConstants[0][1] + pkWTExpUtility
                        * expConstants[0][2] + nmExpUtility * expConstants[0][3]);

                // 11: HOVLS_1_PK
                logsums[11] = Math.log(pkHovExpUtility * expConstants[1][1] + pkWTExpUtility
                        * expConstants[1][2] + nmExpUtility * expConstants[1][3]);

                // 12: HOVLS_2_PK
                logsums[12] = Math.log(pkHovExpUtility * expConstants[2][1] + pkWTExpUtility
                        * expConstants[2][2] + nmExpUtility * expConstants[2][3]);

                // 13: ALL
                logsums[13] = Math.log(pkSovExpUtility * expConstants[3][0] + pkHovExpUtility
                        * expConstants[3][1] + pkWTExpUtility * expConstants[3][2] + nmExpUtility
                        * expConstants[3][3]);

                aDmu.setLogsums(logsums);
                aDmu.setSizeTerms(sizeTerms[jMgra]);
                // double[] utilities = dcUEC.solve(iv, aDmu, null);

                if (trace)
                {
                    String printString = new String();
                    printString += (iMgra + "," + jMgra);
                    for(int j =0;j<14;++j){
                    	printString += ","+String.format("%9.2f", logsums[j]);
                    }
                    logger.info(printString);	
                    
                    accString = new String();
                    accString = "iMgra, jMgra, Alternative, Logsum, SizeTerm, Accessibility\n";
                    
                }
                // add accessibilities for origin mgra
                for (int alt = 0; alt < alts; ++alt)
                {

                    double logsum = aDmu.getLogsum(alt + 1);
                    double sizeTerm = aDmu.getSizeTerm(alt + 1);

                    accessibilities[iMgra][alt] += (Math.exp(logsum) * sizeTerm);

                    if (trace)
                    {
                        accString += iMgra +"," + alt + "," + logsum + ","
                                + sizeTerm + "," + accessibilities[iMgra][alt] + "\n";
                    }
                    
                }
                

                // if luModeChoiceLogsums is null, is has not been initialized, meaning that LU accessibility calculations are not needed
                if ( calculateLuAccessibilities ) {
                                	
	                // 0: AM Mode Choice utility for 0-autos auto sufficiency
	                luUtilities[0] =
	                		pkSovExpUtility * expConstants[0][0] +
	                		pkHovExpUtility * expConstants[0][1] +
	                		pkWTExpUtility * expConstants[0][2] +
	                		pkDTExpUtility * expConstants[0][4] +
	                		nmExpUtility * expConstants[0][3];
	
	                // 1: AM Mode Choice utility for autos<adults auto sufficiency
	                luUtilities[1] =
	                		pkSovExpUtility * expConstants[1][0] +
	                		pkHovExpUtility * expConstants[1][1] +
	                		pkWTExpUtility * expConstants[1][2] +
	                		pkDTExpUtility * expConstants[1][4] +
	                		nmExpUtility * expConstants[1][3];
	
	                // 2: AM Mode Choice utility for autos>=adults auto sufficiency
	                luUtilities[2] =
	                		pkSovExpUtility * expConstants[2][0] +
	                		pkHovExpUtility * expConstants[2][1] +
	                		pkWTExpUtility * expConstants[2][2] +
	                		pkDTExpUtility * expConstants[2][4] +
	                		nmExpUtility * expConstants[2][3];
	
	                // 3: MD Mode Choice utility for 0-autos auto sufficiency
	                luUtilities[3] =
	                		opSovExpUtility * expConstants[0][0] +
	                		opHovExpUtility * expConstants[0][1] +
	                		opWTExpUtility * expConstants[0][2] +
	                		opDTExpUtility * expConstants[0][4] +
	                		nmExpUtility * expConstants[0][3];
	
	                // 4: MD Mode Choice utility for autos<adults auto sufficiency
	                luUtilities[4] =
	                		opSovExpUtility * expConstants[1][0] +
	                		opHovExpUtility * expConstants[1][1] +
	                		opWTExpUtility * expConstants[1][2] +
	                		opDTExpUtility * expConstants[1][4] +
	                		nmExpUtility * expConstants[1][3];
	
	                // 5: MD Mode Choice utility for autos>=adults auto sufficiency
	                luUtilities[5] =
	                		opSovExpUtility * expConstants[2][0] +
	                		opHovExpUtility * expConstants[2][1] +
	                		opWTExpUtility * expConstants[2][2] +
	                		opDTExpUtility * expConstants[2][4] +
	                		nmExpUtility * expConstants[2][3];
	
	                // 6: AM Mode Choice utility for all households
	                luUtilities[6] =
	                		pkSovExpUtility * expConstants[3][0] +
	                		pkHovExpUtility * expConstants[3][1] +
	                		pkWTExpUtility * expConstants[3][2] +
	                		pkDTExpUtility * expConstants[3][4] +
	                		nmExpUtility * expConstants[3][3];
	

	                // calculate non-mandatory destination choice logsums
	                
	                luDmu.setLogsums(luUtilities);
	                luDmu.setSizeTerms(luSizeTerms[jMgra]);

	                if (trace)
	                {
	                    String printString = new String();
	                    printString += (iMgra + "," + jMgra);
	                    for(int j =0;j<luUtilities.length;++j){
	                    	printString += ","+String.format("%9.2f", luUtilities[j]);
	                    }
	                    logger.info(printString);	
	                    
	                    accString = new String();
	                    accString = "Non-mandatory: iMgra, jMgra, Alternative, LU_Logsum, SizeTerm, LU_Accessibility\n";
	                    
	                }
	                // add accessibilities for origin mgra
	                for (int alt = 0; alt < luAlts; ++alt)
	                {

	                    double logsum = luDmu.getLogsum(alt + 1);
	                    double sizeTerm = luDmu.getSizeTerm(alt + 1);

	                    luAccessibilities[iMgra][alt] += ( logsum * sizeTerm );

	                    if (trace)
	                    {
	                        accString += iMgra +"," + alt + "," + logsum + ","
	                                + sizeTerm + "," + luAccessibilities[iMgra][alt] + "\n";
	                    }
	                }

	                
	                // save calculated utilities in a table to return tot the calling method for accumulating
	                luUtilityResult[0] = iMgra;
	                luUtilityResult[1] = jMgra;
	                for ( int k=0; k < luUtilities.length; k++ )
	                	luUtilityResult[2+k] = (float)luUtilities[k];
	                          
	                accumulateLandUseModeChoiceLogsums( luUtilityResult, accumulatedLandUseLogsumsCount, accumulatedLandUseLogsums );
	                
                }
                
                
            } //end for destinations
            
            if(trace)
            {
            	logger.info(accString);
            }


            // calculate the logsum
            for (int alt = 0; alt < alts; ++alt){
                if (accessibilities[iMgra][alt] > 0)
                    accessibilities[iMgra][alt] = (float) Math.log(accessibilities[iMgra][alt]);            	
            }
            accessibilities[iMgra][alts] = iMgra;
            

            if ( calculateLuAccessibilities ) {
		        // calculate the land use accessibility logsums
		        for (int alt = 0; alt < luAlts; ++alt){
		            if (luAccessibilities[iMgra][alt] > 0)
		            	luAccessibilities[iMgra][alt] = (float) Math.log(luAccessibilities[iMgra][alt]);
		        	
		        }
                luAccessibilities[iMgra][luAlts] = iMgra;
            }
            
        }

        
        List<Object> resultBundle = new ArrayList<Object>(7);
        resultBundle.add(taskIndex);
        resultBundle.add(startRange);
        resultBundle.add(endRange);
        resultBundle.add(accessibilities);

        if ( calculateLuAccessibilities ) {
	        resultBundle.add(luAccessibilities);
	        resultBundle.add(accumulatedLandUseLogsums);
	        resultBundle.add(accumulatedLandUseLogsumsCount);
        }
        else {
	        resultBundle.add(null);
	        resultBundle.add(null);
	        resultBundle.add(null);
        }

//        outStream.close();
        
        return resultBundle;

    }
    

//	private void debugLandUseModeChoiceLogsums( int iMgra, int jMgra, int iLuz, int jLuz, float[] luUtilities ) {
//		
//        String record = ( iLuz + "," + jLuz + "," + iMgra + "," + jMgra + "," + luUtilities[0] );
//        // don't need to report the last logsum (not used for mode choice logsums)
//        for( int j=1; j < luUtilities.length - 1; j++ )
//            record += ( "," + luUtilities[j] );
//        outStream.println ( record );
//            
//	}

	
    private void accumulateLandUseModeChoiceLogsums( float[] luUtilitiesValues, int[][] accumulatedLandUseLogsumsCount, float[][][][][] accumulatedLandUseLogsums ) {
    	
    	float[] luUtilities = new float[DcUtilitiesTaskJppf.LU_LOGSUM_SEGMENTS.length];
    	
		int iMgra = (int) luUtilitiesValues[0];
		int jMgra = (int) luUtilitiesValues[1];
    
		int iLuz = mgraManager.getMgraLuz( iMgra );
        int jLuz = mgraManager.getMgraLuz( jMgra );

        for ( int i=0; i < luUtilities.length; i++ )
        	luUtilities[i] = luUtilitiesValues[i+2];
        
        accumulatedLandUseLogsumsCount[iLuz][jLuz]++;
        
        accumulateSimple( iLuz, jLuz, luUtilities, accumulatedLandUseLogsumsCount, accumulatedLandUseLogsums );        
        accumulateLogit( iLuz, jLuz, luUtilities, accumulatedLandUseLogsumsCount, accumulatedLandUseLogsums );
        
//            if ( iLuz == DEBUG_ILUZ && jLuz == DEBUG_JLUZ )
//        	debugLandUseModeChoiceLogsums( iMgra, jMgra, iLuz, jLuz, luUtilities );
        
        
    }

    
    private void accumulateSimple( int iLuz, int jLuz, float[] luUtilities, int[][] accumulatedLandUseLogsumsCount, float[][][][][] accumulatedLandUseLogsums ) {
    	
        // simple averaging uses accumulated logsum values 
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS0][iLuz][jLuz] += Math.log( luUtilities[0] );
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS1][iLuz][jLuz] += Math.log( luUtilities[1] );
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS2][iLuz][jLuz] += Math.log( luUtilities[2] );
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS0][iLuz][jLuz] += Math.log( luUtilities[3] );
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS1][iLuz][jLuz] += Math.log( luUtilities[4] );
        accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS2][iLuz][jLuz] += Math.log( luUtilities[5] );
    
        // calculate logsums from external LUZs to all destination LUZs if the origin LUZ is a cordon LUZ 
        if ( externalLuzsForCordonLuz[iLuz] != null ) {
        	
        	for( int exLuz : externalLuzsForCordonLuz[iLuz] ) {
        		
        		double additionalUtility = Math.exp( cordonLuzMinutesForExternalLuz[exLuz] * BuildAccessibilities.TIME_COEFFICIENT );
        		
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS0][exLuz][jLuz] += Math.log( luUtilities[0] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS1][exLuz][jLuz] += Math.log( luUtilities[1] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS2][exLuz][jLuz] += Math.log( luUtilities[2] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS0][exLuz][jLuz] += Math.log( luUtilities[3] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS1][exLuz][jLuz] += Math.log( luUtilities[4] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS2][exLuz][jLuz] += Math.log( luUtilities[5] + additionalUtility );

                accumulatedLandUseLogsumsCount[exLuz][jLuz]++;

        	}
        	
        }

        
        // calculate logsums to external LUZs from all origin LUZs if the destination LUZ is a cordon LUZ 
        if ( externalLuzsForCordonLuz[jLuz] != null ) {
        	
        	for( int exLuz : externalLuzsForCordonLuz[jLuz] ) {
        		
        		double additionalUtility = Math.exp( cordonLuzMinutesForExternalLuz[exLuz] * BuildAccessibilities.TIME_COEFFICIENT );
        		
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS0][iLuz][exLuz] += Math.log( luUtilities[0] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS1][iLuz][exLuz] += Math.log( luUtilities[1] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.PK][BuildAccessibilities.LS2][iLuz][exLuz] += Math.log( luUtilities[2] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS0][iLuz][exLuz] += Math.log( luUtilities[3] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS1][iLuz][exLuz] += Math.log( luUtilities[4] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.SIMPLE][BuildAccessibilities.OP][BuildAccessibilities.LS2][iLuz][exLuz] += Math.log( luUtilities[5] + additionalUtility );

                accumulatedLandUseLogsumsCount[iLuz][exLuz]++;

        	}
        	
        }
            
    }
    
    
    private void accumulateLogit( int iLuz, int jLuz, float[] luUtilities, int[][] accumulatedLandUseLogsumsCount, float[][][][][] accumulatedLandUseLogsums ) {
    	
        // logit averaging uses accumulated utility values 
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS0][iLuz][jLuz] += luUtilities[0];
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS1][iLuz][jLuz] += luUtilities[1];
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS2][iLuz][jLuz] += luUtilities[2];
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS0][iLuz][jLuz] += luUtilities[3];
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS1][iLuz][jLuz] += luUtilities[4];
        accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS2][iLuz][jLuz] += luUtilities[5];
    
        // calculate logsums from external LUZs to all destination LUZs if the origin LUZ is a cordon LUZ 
        if ( externalLuzsForCordonLuz[iLuz] != null ) {
        	
        	for( int exLuz : externalLuzsForCordonLuz[iLuz] ) {
        		
        		double additionalUtility = Math.exp( cordonLuzMinutesForExternalLuz[exLuz] * BuildAccessibilities.TIME_COEFFICIENT );
        		
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS0][exLuz][jLuz] += ( luUtilities[0] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS1][exLuz][jLuz] += ( luUtilities[1] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS2][exLuz][jLuz] += ( luUtilities[2] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS0][exLuz][jLuz] += ( luUtilities[3] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS1][exLuz][jLuz] += ( luUtilities[4] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS2][exLuz][jLuz] += ( luUtilities[5] + additionalUtility );

                accumulatedLandUseLogsumsCount[exLuz][jLuz]++;

        	}
        	
        }

        
        // calculate logsums to external LUZs from all origin LUZs if the destination LUZ is a cordon LUZ 
        if ( externalLuzsForCordonLuz[jLuz] != null ) {
        	
        	for( int exLuz : externalLuzsForCordonLuz[jLuz] ) {
        		
        		double additionalUtility = Math.exp( cordonLuzMinutesForExternalLuz[exLuz] * BuildAccessibilities.TIME_COEFFICIENT );
        		
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS0][iLuz][exLuz] += ( luUtilities[0] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS1][iLuz][exLuz] += ( luUtilities[1] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.PK][BuildAccessibilities.LS2][iLuz][exLuz] += ( luUtilities[2] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS0][iLuz][exLuz] += ( luUtilities[3] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS1][iLuz][exLuz] += ( luUtilities[4] + additionalUtility );
                accumulatedLandUseLogsums[BuildAccessibilities.LOGIT][BuildAccessibilities.OP][BuildAccessibilities.LS2][iLuz][exLuz] += ( luUtilities[5] + additionalUtility );

                accumulatedLandUseLogsumsCount[iLuz][exLuz]++;

        	}
        	
        }
            
    }
    
    
}
