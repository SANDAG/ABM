package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.TNCAndTaxiWaitTimeCalculator;
import org.sandag.abm.ctramp.Util;
import com.pb.common.calculator.IndexValues;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.LogitModel;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.Tracer;

/**
 * This class is the UEC for MAAS
 * Joel Freedman
 * RSG 2019-07-08
 */
public class MaasUEC
        implements Serializable
{

    protected transient Logger          logger = Logger.getLogger(MaasUEC.class);
    private TazDataManager              tazs;
    private UtilityExpressionCalculator uec;
    private LogitModel                  model;
    private ChoiceModelApplication      modelApp;

    private IndexValues                 index  = new IndexValues();
    private int[]                       availFlag;
    private MaasDMU                     dmu;

    // seek and trace
    private boolean                     trace;
    private int[]                       traceOtaz;
    private int[]                       traceDtaz;
    protected Tracer                    tracer;
  
  


    /**
     * Constructor.
     * 
     * @param rb
     *            ResourceBundle
     * @param UECFileName
     *            The path/name of the UEC containing the auto model.
     * @param modelSheet
     *            The sheet (0-indexed) containing the model specification.
     * @param dataSheet
     *            The sheet (0-indexed) containing the data specification.
     */
    public MaasUEC(HashMap<String, String> rbHashMap, String uecFileName, int modelSheet,
            int dataSheet)
    {

        dmu = new MaasDMU();

        // use the choice model application to set up the model structure
        modelApp = new ChoiceModelApplication(uecFileName, modelSheet, dataSheet, rbHashMap, dmu);

        // but return the logit model itself, so we can use compound utilities
        model = modelApp.getRootLogitModel();
        uec = modelApp.getUEC();

        tazs = TazDataManager.getInstance();
        trace = Util.getBooleanValueFromPropertyMap(rbHashMap, "Trace");
        traceOtaz = Util.getIntegerArrayFromPropertyMap(rbHashMap, "Trace.otaz");
        traceDtaz = Util.getIntegerArrayFromPropertyMap(rbHashMap, "Trace.dtaz");

        // set up the tracer object
        tracer = Tracer.getTracer();
        tracer.setTrace(trace);
        if (trace)
        {
            for (int i = 0; i < traceOtaz.length; i++)
            {
                for (int j = 0; j < traceDtaz.length; j++)
                {
                    tracer.traceZonePair(traceOtaz[i], traceDtaz[j]);
                }
            }
        }
        
  
    }

    /**
     * Solve auto utilities for a given zone-pair
     * 
     * @param pTaz
     *            Production/Origin TAZ.
     * @param aTaz
     *            Attraction/Destination TAZ.
     * @return The root utility.
     */
    public double calculateUtilitiesForTazPair(int pTaz, int aTaz, float avgTaxiWaitTime, float avgSingleTNCWaitTime,float avgSharedTNCWaitTime)
    {

        trace = false;
        if (tracer.isTraceOn() && tracer.isTraceZonePair(pTaz, aTaz))
        {
            trace = true;
        }
        index.setOriginZone(pTaz);
        index.setDestZone(aTaz);
        availFlag = new int[uec.getNumberOfAlternatives() + 1];
        Arrays.fill(availFlag, 1);

        dmu.setWaitTimeTaxi(avgTaxiWaitTime);
        dmu.setWaitTimeSingleTNC(avgSingleTNCWaitTime);
        dmu.setWaitTimeSharedTNC(avgSharedTNCWaitTime);
        
        // log DMU values
        if (trace)
        {
            TapDataManager tapManager = TapDataManager.getInstance();
            if (Arrays.binarySearch(tapManager.getTaps(), pTaz) > 0
                    && Arrays.binarySearch(tapManager.getTaps(), aTaz) > 0)
                uec.logDataValues(logger, pTaz, aTaz, aTaz);
            dmu.logValues(logger);
        }

        modelApp.computeUtilities(dmu, index);
        double utility = modelApp.getLogsum();

        // logging
        if (trace)
        {
            uec.logAnswersArray(logger, "Maas UEC");
            uec.logResultsArray(logger, pTaz, aTaz);
            modelApp.logLogitCalculations("Maas UEC", "Trace");
            logger.info("Logsum = " + utility);
            trace = false;
        }

        return utility;
    }
}
