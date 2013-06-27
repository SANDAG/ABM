package org.sandag.abm.modechoice;

import com.pb.common.calculator.IndexValues;
import com.pb.common.newmodel.LogitModel;
import com.pb.common.util.Tracer;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.newmodel.ChoiceModelApplication;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * This class is used for ...
 * 
 * @author Christi Willison
 * @version Mar 9, 2009
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class NonMotorUEC
        implements Serializable
{

    private transient Logger               logger = Logger.getLogger(NonMotorUEC.class);
    private TazDataManager              tazManager;
    private MgraDataManager             mgraManager;
    private UtilityExpressionCalculator uec;
    private IndexValues                 index  = new IndexValues();
    private int[]                       availFlag;
    private NonMotorDMU                 dmu;
    private LogitModel                  model;
    private ChoiceModelApplication      modelApp;

    // seek and trace
    private boolean                     trace;
    private int[]                       traceOtaz;
    private int[]                       traceDtaz;
    protected Tracer                    tracer;

    /**
     * Default Constructor.
     * 
     * @param rb
     * @param uecFileName
     * @param modelSheet
     * @param dataSheet
     */
    public NonMotorUEC(HashMap<String, String> rbHashMap, String uecFileName, int modelSheet,
            int dataSheet)
    {

        dmu = new NonMotorDMU();

        // use the choice model application to set up the model structure
        modelApp = new ChoiceModelApplication(uecFileName, modelSheet, dataSheet, rbHashMap, dmu);

        // but return the logit model itself, so we can use compound utilities
        model = modelApp.getRootLogitModel();
        uec = modelApp.getUEC();
        availFlag = new int[uec.getNumberOfAlternatives() + 1];

        tazManager = TazDataManager.getInstance();
        mgraManager = MgraDataManager.getInstance();

        trace = Util.getBooleanValueFromPropertyMap(rbHashMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbHashMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbHashMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        for (int i = 0; i < traceOtaz.length; i++)
        {
            for (int j = 0; j < traceDtaz.length; j++)
            {
                tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
            }
        }
    }

    /**
     * Calculate utilities for a given TAZ pair.
     * 
     * @param pTaz Production/Origin TAZ.
     * @param aTaz Attraction/Destination TAZ.
     * @return The root utility.
     */
    public double calculateUtilitiesForTazPair(int pTaz, int aTaz)
    {

        index.setOriginZone(pTaz);
        index.setDestZone(aTaz);

        Arrays.fill(availFlag, 1);
        dmu.setMgraWalkTime(0);
        dmu.setMgraBikeTime(0);

        trace = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz))
        {
            trace = true;
        }

        // log DMU values
        if (trace)
        {
            TapDataManager tapManager = TapDataManager.getInstance();
            if (Arrays.binarySearch(tapManager.getTaps(), pTaz) > 0
                    && Arrays.binarySearch(tapManager.getTaps(), aTaz) > 0)
                uec.logDataValues(logger, pTaz, aTaz, 0);
            dmu.logValues(logger);
        }

        modelApp.computeUtilities(dmu, index);
        double utility = modelApp.getLogsum();
        if (utility == 0) utility = -999;

        // logging
        if (trace)
        {
            uec.logAnswersArray(logger, "NonMotorized UEC");
            uec.logResultsArray(logger, pTaz, aTaz);
            modelApp.logLogitCalculations("NonMotorized UEC", "Zone Trace");
            logger.info("Logsum = " + utility);
            trace = false;
        }

        return utility;
    }

    /**
     * Calculate utilities for a given TAZ pair.
     * 
     * @param oMgra Production/Origin Mgra.
     * @param dMgra Attraction/Destination Mgra.
     * @return The root utility.
     */
    public double calculateUtilitiesForMgraPair(int oMgra, int dMgra)
    {

        Arrays.fill(availFlag, 1);

        trace = false;
        int pTaz = mgraManager.getTaz(oMgra);
        int aTaz = mgraManager.getTaz(dMgra);
        index.setOriginZone(pTaz);
        index.setDestZone(aTaz);

        if (tracer.isTraceOn() && tracer.isTraceZone(pTaz))
        {
            trace = true;
        }

        dmu.setMgraWalkTime(mgraManager.getMgraToMgraWalkTime(oMgra, dMgra));
        dmu.setMgraBikeTime(mgraManager.getMgraToMgraBikeTime(oMgra, dMgra));

        // log DMU values
        if (trace)
        {
            logger.info("MGRA-MGRA non-motorized calculations for " + oMgra + " to " + dMgra);
            dmu.logValues(logger);
        }

        modelApp.computeUtilities(dmu, index);
        double utility = modelApp.getLogsum();
        if (utility == 0) utility = -999;

        // logging
        if (trace)
        {
            uec.logAnswersArray(logger, "NonMotorized UEC");
            uec.logResultsArray(logger, pTaz, aTaz);
            modelApp.logLogitCalculations("NonMotorized UEC", "Mgra Trace");
            logger.info("Logsum = " + utility);
            trace = false;
        }
        dmu.setMgraWalkTime(0);
        dmu.setMgraWalkTime(0);
        return utility;
    }

}