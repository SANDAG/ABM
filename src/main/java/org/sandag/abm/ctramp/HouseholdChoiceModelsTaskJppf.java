package org.sandag.abm.ctramp;

import java.net.UnknownHostException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.task.storage.DataProvider;
import com.pb.common.calculator.MatrixDataServerIf;

public class HouseholdChoiceModelsTaskJppf
        extends AbstractTask<String>
{

    private transient HashMap<String, String> propertyMap;
    private transient MatrixDataServerIf      ms;
    private transient HouseholdDataManagerIf  hhDataManager;
    private transient ModelStructure          modelStructure;
    private transient CtrampDmuFactoryIf      dmuFactory;
    private transient String                  restartModelString;

    private int                               startIndex;
    private int                               endIndex;
    private int                               taskIndex;

    private int                               maxAlts;

    private boolean                           runWithTiming;

    public HouseholdChoiceModelsTaskJppf(int taskIndex, int startIndex, int endIndex)
    {

        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.taskIndex = taskIndex;

        runWithTiming = true;
    }

    public void run()
    {

        long startTime = System.nanoTime();

        Logger logger = Logger.getLogger(this.getClass());

        String threadName = null;
        try
        {
            threadName = "[" + java.net.InetAddress.getLocalHost().getHostName() + "] "
                    + Thread.currentThread().getName();
        } catch (UnknownHostException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        try
        {

            DataProvider dataProvider = getDataProvider();

            propertyMap = (HashMap<String, String>) dataProvider.getParameter("propertyMap");
            ms = (MatrixDataServerIf) dataProvider.getParameter("ms");
            hhDataManager = (HouseholdDataManagerIf) dataProvider.getParameter("hhDataManager");
            modelStructure = (ModelStructure) dataProvider.getParameter("modelStructure");
            dmuFactory = (CtrampDmuFactoryIf) dataProvider.getParameter("dmuFactory");
            restartModelString = (String) dataProvider.getParameter("restartModelString");

        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // get the factory object used to create and recycle
        // HouseholdChoiceModels objects.
        HouseholdChoiceModelsManager modelManager = HouseholdChoiceModelsManager.getInstance();
        modelManager.managerSetup(ms, hhDataManager, propertyMap, restartModelString,
                modelStructure, dmuFactory);

        HouseholdChoiceModels hhModel = modelManager.getHouseholdChoiceModelsObject(taskIndex);

        long setup1 = 0;
        long setup2 = 0;
        long setup3 = 0;
        long setup4 = 0;
        long setup5 = 0;

        setup1 = (System.nanoTime() - startTime) / 1000000;

        Household[] householdArray = hhDataManager.getHhArray(startIndex, endIndex);

        setup2 = (System.nanoTime() - startTime) / 1000000;

        boolean runDebugHouseholdsOnly = Util.getBooleanValueFromPropertyMap(propertyMap,
                HouseholdDataManager.DEBUG_HHS_ONLY_KEY);

        if (runWithTiming) hhModel.zeroTimes();
        for (int i = 0; i < householdArray.length; i++)
        {

            // for debugging only - process only household objects specified for
            // debugging, if property key was set to true
            if (runDebugHouseholdsOnly && !householdArray[i].getDebugChoiceModels()) continue;

            try
            {
                if (runWithTiming) hhModel.runModelsWithTiming(householdArray[i]);
                else hhModel.runModels(householdArray[i]);
            } catch (RuntimeException e)
            {
                logger.fatal(String
                        .format("exception caught in taskIndex=%d hhModel index=%d applying hh model for i=%d, hhId=%d.",
                                taskIndex, hhModel.getModelIndex(), i, householdArray[i].getHhId()));
                logger.fatal("Exception caught:", e);
                logger.fatal("Throwing new RuntimeException() to terminate.");
                throw new RuntimeException();
            }

        }

        long[] componentTimes = hhModel.getTimes();
        long[] partialStopTimes = hhModel.getPartialStopTimes();

        if (hhModel.getMaxAlts() > maxAlts) maxAlts = hhModel.getMaxAlts();

        setup3 = (System.nanoTime() - startTime) / 1000000;

        hhDataManager.setHhArray(householdArray, startIndex);

        setup4 = (System.nanoTime() - startTime) / 1000000;

        logger.info(String
                .format("end of household choice model thread=%s, task[%d], hhModel[%d], startIndex=%d, endIndex=%d",
                        threadName, taskIndex, hhModel.getModelIndex(), startIndex, endIndex));

        setResult(String.format("taskIndex=%d, hhModelInstance=%d, startIndex=%d, endIndex=%d",
                taskIndex, hhModel.getModelIndex(), startIndex, endIndex));

        setup5 = (System.nanoTime() - startTime) / 1000000;

        logger.info("task=" + taskIndex + ", setup=" + setup1 + ", getHhs=" + (setup2 - setup1)
                + ", processHhs=" + (setup3 - setup2) + ", putHhs=" + (setup4 - setup3)
                + ", return model=" + (setup5 - setup4) + ".");

        if (runWithTiming)
            logModelComponentTimes(componentTimes, partialStopTimes, logger,
                    hhModel.getModelIndex());

        // this has to be the last statement in this method.
        // add this DestChoiceModel instance to the static queue shared by other
        // tasks of this type
        modelManager.returnHouseholdChoiceModelsObject(hhModel, startIndex, endIndex);

    }

    private void logModelComponentTimes(long[] componentTimes, long[] partialStopTimes,
            Logger logger, int modelIndex)
    {

        String[] label1 = {"AO", "FP", "IE", "CDAP", "IMTF", "IMTOD", "IMMC", "JTF", "JTDC",
                "JTTOD", "JTMC", "INMTF", "INMTDCSOA", "INMTDCTOT", "INMTTOD", "INMTMC", "AWTF",
                "AWTDCSOA", "AWTDCTOT", "AWTTOD", "AWTMC", "STF", "STDTM"};

        logger.info("Household choice model component runtimes (in milliseconds) for task: "
                + taskIndex + ", modelIndex: " + modelIndex + ", startIndex: " + startIndex
                + ", endIndex: " + endIndex);

        float total = 0;
        for (int i = 0; i < componentTimes.length; i++)
        {
            float time = (componentTimes[i] / 1000000);
            logger.info(String.format("%-6d%30s:%15.1f", (i + 1), label1[i], time));
            total += time;
        }
        logger.info(String.format("%-6s%30s:%10.1f", "Total", "Total all components", total));
        logger.info("");

        String[] label2 = {"SLC SOA AUTO", "SLC SOA OTHER", "SLC LS", "SLC DIST", "SLC", "SLC TOT",
                "S TOD", "S MC", "TOTAL"};

        logger.info("");
        logger.info("Times for parts of intermediate stop models:");
        for (int i = 0; i < partialStopTimes.length; i++)
        {
            float time = (partialStopTimes[i] / 1000000);
            logger.info(String.format("%-6d%30s:%15.1f", (i + 1), label2[i], time));
        }

    }

    public String getId()
    {
        return Integer.toString(taskIndex);
    }

    public int getMaxAlts()
    {
        return maxAlts;
    }

}
