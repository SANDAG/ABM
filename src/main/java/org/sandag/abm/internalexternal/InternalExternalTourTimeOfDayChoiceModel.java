package org.sandag.abm.internalexternal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class is the TOD choice model for IE tours. It is currently based on a
 * static probability distribution stored in an input file, and indexed into by
 * purpose. Since there are no IE purposes, the purpose is 0.
 * 
 * @author Freedman
 * 
 */
public class InternalExternalTourTimeOfDayChoiceModel
{
    private transient Logger       logger = Logger.getLogger("internalExternalModel");

    private double[][]             cumProbability;                                    // by
                                                                                       // purpose,
                                                                                       // alternative:
                                                                                       // cumulative
                                                                                       // probability
                                                                                       // distribution
    private int[][]                outboundPeriod;                                    // by
                                                                                       // purpose,
                                                                                       // alternative:
                                                                                       // outbound
                                                                                       // period
    private int[][]                returnPeriod;                                      // by
                                                                                       // purpose,
                                                                                       // alternative:
                                                                                       // return
                                                                                       // period
    InternalExternalModelStructure modelStructure;

    /**
     * Constructor.
     */
    public InternalExternalTourTimeOfDayChoiceModel(HashMap<String, String> rbMap)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String stationDiurnalFile = Util.getStringValueFromPropertyMap(rbMap,
                "internalExternal.tour.tod.file");
        stationDiurnalFile = directory + stationDiurnalFile;

        modelStructure = new InternalExternalModelStructure();

        readTODFile(stationDiurnalFile);

    }

    /**
     * Read the TOD distribution in the file and populate the arrays.
     * 
     * @param fileName
     */
    private void readTODFile(String fileName)
    {

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet probabilityTable;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            probabilityTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);

        logger.info("Begin calculating tour TOD probability distribution");

        int purposes = 1; // start at 0
        int periods = modelStructure.TIME_PERIODS; // start at 1
        int periodCombinations = periods * (periods + 1) / 2;

        cumProbability = new double[purposes][periodCombinations]; // by
                                                                   // purpose,
                                                                   // alternative:
                                                                   // cumulative
                                                                   // probability
                                                                   // distribution
        outboundPeriod = new int[purposes][periodCombinations]; // by purpose,
                                                                // alternative:
                                                                // outbound
                                                                // period
        returnPeriod = new int[purposes][periodCombinations]; // by purpose,
                                                              // alternative:
                                                              // return period

        // fill up arrays
        int rowCount = probabilityTable.getRowCount();
        int lastPurpose = -99;
        double cumProb = 0;
        int alt = 0;
        for (int row = 1; row <= rowCount; ++row)
        {

            int purpose = (int) probabilityTable.getValueAt(row, "Purpose");
            int outPer = (int) probabilityTable.getValueAt(row, "EntryPeriod");
            int retPer = (int) probabilityTable.getValueAt(row, "ReturnPeriod");

            // continue if return period before outbound period
            if (retPer < outPer) continue;

            // reset if new purpose
            if (purpose != lastPurpose)
            {

                // log cumulative probability just in case
                if (lastPurpose != -99)
                    logger.info("Cumulative probability for purpose " + purpose + " is " + cumProb);
                cumProb = 0;
                alt = 0;
            }

            // calculate cumulative probability and store in array
            cumProb += probabilityTable.getValueAt(row, "Percent");
            cumProbability[purpose][alt] = cumProb;
            outboundPeriod[purpose][alt] = outPer;
            returnPeriod[purpose][alt] = retPer;

            ++alt;

            lastPurpose = purpose;
        }

        logger.info("End calculating tour TOD probability distribution");

    }

    /**
     * Calculate tour time of day for the tour.
     * 
     * @param tour
     *            An IE tour
     */
    public void calculateTourTOD(InternalExternalTour tour)
    {

        int purpose = 0;
        double random = tour.getRandom();

        if (tour.getDebugChoiceModels())
        {
            logger.info("Choosing tour time of day for tour ID " + tour.getID()
                    + " using random number " + random);
            tour.logTourObject(logger, 100);
        }

        for (int i = 0; i < cumProbability[purpose].length; ++i)
        {

            if (random < cumProbability[purpose][i])
            {
                int depart = outboundPeriod[purpose][i];
                int arrive = returnPeriod[purpose][i];
                tour.setDepartTime(depart);
                tour.setArriveTime(arrive);
                break;
            }
        }

        if (tour.getDebugChoiceModels())
        {
            logger.info("");
            logger.info("Chose depart period " + tour.getDepartTime() + " and arrival period "
                    + tour.getArriveTime());
            logger.info("");
        }
    }

}
