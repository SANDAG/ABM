package org.sandag.abm.ctramp;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.task.storage.DataProvider;

import com.pb.common.calculator.MatrixDataServerIf;

public class SchoolLocationChoiceTaskJppfNew
        extends AbstractTask<String>
{

    private static String                     VERSION   = "Task.1.0.3";

    private transient HashMap<String, String> propertyMap;
    private transient MatrixDataServerIf      ms;
    private transient HouseholdDataManagerIf  hhDataManager;
    private transient ModelStructure          modelStructure;
    private transient String                  tourCategory;
    private transient DestChoiceSize          dcSizeObj;
    private transient String                  dcUecFileName;
    private transient String                  soaUecFileName;
    private transient int                     soaSampleSize;
    private transient CtrampDmuFactoryIf      dmuFactory;
    private transient String                  restartModelString;

    private int                               iteration;
    private int                               startIndex;
    private int                               endIndex;
    private int                               taskIndex = -1;

    public SchoolLocationChoiceTaskJppfNew(int taskIndex, int startIndex, int endIndex,
            int iteration)
    {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.taskIndex = taskIndex;
        this.iteration = iteration;
    }

    public void run()
    {

        String start = (new Date()).toString();
        long startTime = System.currentTimeMillis();

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

        // logger.info( String.format(
        // "startTime=%d, task=%d run(), thread=%s, start=%d, end=%d.",
        // startTime,
        // taskIndex, threadName, startIndex,
        // endIndex ) );

        try
        {
            DataProvider dataProvider = getDataProvider();

            this.propertyMap = (HashMap<String, String>) dataProvider.getParameter("propertyMap");
            this.ms = (MatrixDataServerIf) dataProvider.getParameter("ms");
            this.hhDataManager = (HouseholdDataManagerIf) dataProvider.getParameter("hhDataManager");
            this.modelStructure = (ModelStructure) dataProvider.getParameter("modelStructure");
            this.tourCategory = (String) dataProvider.getParameter("tourCategory");
            this.dcSizeObj = (DestChoiceSize) dataProvider.getParameter("dcSizeObj");
            this.dcUecFileName = (String) dataProvider.getParameter("dcUecFileName");
            this.soaUecFileName = (String) dataProvider.getParameter("soaUecFileName");
            this.soaSampleSize = (Integer) dataProvider.getParameter("soaSampleSize");
            this.dmuFactory = (CtrampDmuFactoryIf) dataProvider.getParameter("dmuFactory");
            this.restartModelString = (String) dataProvider.getParameter("restartModelString");

        } catch (Exception e)
        {
            e.printStackTrace();
        }

        // HouseholdChoiceModelsManager hhModelManager =
        // HouseholdChoiceModelsManager.getInstance(
        // propertyMap, restartModelString, modelStructure, dmuFactory);
        // hhModelManager.clearHhModels();
        // hhModelManager = null;

        // get the factory object used to create and recycle dcModel objects.
        DestChoiceModelManager modelManager = DestChoiceModelManager.getInstance();

        // one of tasks needs to initialize the manager object by passing
        // attributes
        // needed to create a destination choice model object.
        modelManager.managerSetup(propertyMap, modelStructure, ms, dcUecFileName, soaUecFileName,
                soaSampleSize, dmuFactory, restartModelString);

        // get a dcModel object from manager, which either creates one or
        // returns one
        // for re-use.
        SchoolLocationChoiceModel dcModel = modelManager.getSchoolLocModelObject(taskIndex,
                iteration, dcSizeObj);

        // logger.info( String.format(
        // "%s, task=%d run(), thread=%s, start=%d, end=%d.", VERSION,
        // taskIndex,
        // threadName, startIndex, endIndex ) );
        System.out.println(String.format("%s: %s, task=%d run(), thread=%s, start=%d, end=%d.",
                new Date(), VERSION, taskIndex, threadName, startIndex, endIndex));

        long setup1 = (System.currentTimeMillis() - startTime) / 1000;

        Household[] householdArray = hhDataManager.getHhArray(startIndex, endIndex);

        long setup2 = (System.currentTimeMillis() - startTime) / 1000;
        // logger.info( String.format(
        // "task=%d processing households[%d:%d], thread=%s, setup1=%d, setup2=%d.",
        // taskIndex, startIndex, endIndex,
        // threadName, setup1, setup2 ) );
        System.out.println(String.format("%s: task=%d processing households[%d:%d], thread=%s.",
                new Date(), taskIndex, startIndex, endIndex, threadName));

        int i = -1;
        try
        {

            boolean runDebugHouseholdsOnly = Util.getBooleanValueFromPropertyMap(propertyMap,
                    HouseholdDataManager.DEBUG_HHS_ONLY_KEY);

            for (i = 0; i < householdArray.length; i++)
            {
                // for debugging only - process only household objects specified
                // for debugging, if property key was set to true
                if (runDebugHouseholdsOnly && !householdArray[i].getDebugChoiceModels()) continue;

                dcModel.applySchoolLocationChoice(householdArray[i]);
            }

            hhDataManager.setHhArray(householdArray, startIndex);
            
            if(!HouseholdValidator.validateHouseholds(householdArray))
            {
                setResubmit(true);
            }

        } catch (Exception e)
        {
            if (i >= 0 && i < householdArray.length) System.out
                    .println(String
                            .format("exception caught in taskIndex=%d applying dc model for i=%d, hhId=%d, startIndex=%d.",
                                    taskIndex, i, householdArray[i].getHhId(), startIndex));
            else System.out.println(String.format(
                    "exception caught in taskIndex=%d applying dc model for i=%d, startIndex=%d.",
                    taskIndex, i, startIndex));
            System.out.println("Exception caught:");
            e.printStackTrace();
            System.out.println("Throwing new RuntimeException() to terminate.");
            throw new RuntimeException(e);
        }

        long getHhs = ((System.currentTimeMillis() - startTime) / 1000) - setup1;
        long processHhs = ((System.currentTimeMillis() - startTime) / 1000) - setup2 - getHhs;
        // logger.info( String.format(
        // "task=%d finished, thread=%s, getHhs=%d, processHhs=%d.", taskIndex,
        // threadName, getHhs, processHhs ) );
        System.out.println(String.format("%s: task=%d finished, thread=%s.", new Date(), taskIndex,
                threadName));

        long total = (System.currentTimeMillis() - startTime) / 1000;
        String resultString = String
                .format("result for thread=%s, task=%d, startIndex=%d, endIndex=%d, startTime=%s, endTime=%s, setup1=%d, setup2=%d, getHhs=%d, run=%d, total=%d.",
                        threadName, taskIndex, startIndex, endIndex, start, new Date(), setup1,
                        setup2, getHhs, processHhs, total);
        // logger.info( resultString );
        setResult(resultString);

        modelManager.returnSchoolLocModelObject(dcModel, taskIndex, startIndex, endIndex);

        clearClassAttributes();
    }

    public String getId()
    {
        return Integer.toString(taskIndex);
    }

    private void clearClassAttributes()
    {
        propertyMap = null;
        ms = null;
        hhDataManager = null;
        modelStructure = null;
        tourCategory = null;
        dcSizeObj = null;
        dcUecFileName = null;
        soaUecFileName = null;
        dmuFactory = null;
        restartModelString = null;
    }
}
