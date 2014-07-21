package org.sandag.abm.crossborder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class is the stop frequency model for cross border tours. It is
 * currently based on a static probability distribution stored in an input file,
 * and indexed into by tour purpose and duration.
 * 
 * @author Freedman
 * 
 */
public class CrossBorderStopFrequencyModel
{
    private transient Logger  logger = Logger.getLogger("crossBorderModel");

    private double[][]        cumProbability;                               // by
                                                                             // purpose,
                                                                             // alternative:
                                                                             // cumulative
                                                                             // probability
                                                                             // distribution
    private int[][]           lowerBoundDurationHours;                      // by
                                                                             // purpose,
                                                                             // alternative:
                                                                             // lower
                                                                             // bound
                                                                             // in
                                                                             // hours
    private int[][]           upperBoundDurationHours;                      // by
                                                                             // purpose,
                                                                             // alternative:
                                                                             // upper
                                                                             // bound
                                                                             // in
                                                                             // hours
    private int[][]           outboundStops;                                // by
                                                                             // purpose,
                                                                             // alternative:
                                                                             // number
                                                                             // of
                                                                             // outbound
                                                                             // stops
    private int[][]           inboundStops;                                 // by
                                                                             // purpose,
                                                                             // alternative:
                                                                             // number
                                                                             // of
                                                                             // inbound
                                                                             // stops
    CrossBorderModelStructure modelStructure;

    /**
     * Constructor.
     */
    public CrossBorderStopFrequencyModel(HashMap<String, String> rbMap)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String stopFrequencyFile = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.stop.frequency.file");
        stopFrequencyFile = directory + stopFrequencyFile;

        modelStructure = new CrossBorderModelStructure();

        readStopFrequencyFile(stopFrequencyFile);

    }

    /**
     * Read the stop frequency distribution in the file and populate the arrays.
     * 
     * @param fileName
     */
    private void readStopFrequencyFile(String fileName)
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

        logger.info("Begin calculating stop frequency probability distribution");

        int purposes = modelStructure.NUMBER_CROSSBORDER_PURPOSES; // start at 0

        int[] alts = new int[purposes];

        // take a pass through the data and see how many alternatives there are
        // for each purpose
        int rowCount = probabilityTable.getRowCount();
        for (int row = 1; row <= rowCount; ++row)
        {

            int purpose = (int) probabilityTable.getValueAt(row, "Purpose");
            ++alts[purpose];
        }

        // initialize all the arrays
        cumProbability = new double[purposes][];
        lowerBoundDurationHours = new int[purposes][];
        upperBoundDurationHours = new int[purposes][];
        outboundStops = new int[purposes][];
        inboundStops = new int[purposes][];

        for (int i = 0; i < purposes; ++i)
        {
            cumProbability[i] = new double[alts[i]];
            lowerBoundDurationHours[i] = new int[alts[i]];
            upperBoundDurationHours[i] = new int[alts[i]];
            outboundStops[i] = new int[alts[i]];
            inboundStops[i] = new int[alts[i]];
        }

        // fill up arrays
        int lastPurpose = 0;
        int lastLowerBound = 0;
        double cumProb = 0;
        int alt = 0;
        for (int row = 1; row <= rowCount; ++row)
        {

            int purpose = (int) probabilityTable.getValueAt(row, "Purpose");
            int lowerBound = (int) probabilityTable.getValueAt(row, "DurationLo");
            int upperBound = (int) probabilityTable.getValueAt(row, "DurationHi");
            int outStops = (int) probabilityTable.getValueAt(row, "Outbound");
            int inbStops = (int) probabilityTable.getValueAt(row, "Inbound");

            // reset cumulative probability if new purpose or lower-bound
            if (purpose != lastPurpose || lowerBound != lastLowerBound)
            {

                // log cumulative probability just in case
            	/*
                logger.info("Cumulative probability for purpose " + purpose + " lower bound "
                        + lowerBound + " is " + cumProb);
                */
                cumProb = 0;
            }

            if (purpose != lastPurpose) alt = 0;

            // calculate cumulative probability and store in array
            cumProb += probabilityTable.getValueAt(row, "Percent");
            cumProbability[purpose][alt] = cumProb;
            lowerBoundDurationHours[purpose][alt] = lowerBound;
            upperBoundDurationHours[purpose][alt] = upperBound;
            outboundStops[purpose][alt] = outStops;
            inboundStops[purpose][alt] = inbStops;

            ++alt;

            lastPurpose = purpose;
            lastLowerBound = lowerBound;
        }

        logger.info("End calculating stop frequency probability distribution");
/*
        for (int purp = 0; purp < purposes; ++purp)
        {
            for (int a = 0; a < cumProbability[purp].length; ++a)
            {
                logger.info("Purpose " + purp + " lower " + lowerBoundDurationHours[purp][a]
                        + " upper " + upperBoundDurationHours[purp][a] + " cumProb "
                        + cumProbability[purp][a]);
            }
        }
*/
    }

    /**
     * Calculate tour time of day for the tour.
     * 
     * @param tour
     *            A cross border tour (with purpose)
     */
    public void calculateStopFrequency(CrossBorderTour tour)
    {

        int purpose = tour.getPurpose();
        double random = tour.getRandom();

        if (tour.getDebugChoiceModels())
        {
            logger.info("Choosing stop frequency for purpose "
                    + modelStructure.CROSSBORDER_PURPOSES[purpose] + " using random number "
                    + random);
            tour.logTourObject(logger, 100);
        }

        for (int i = 0; i < cumProbability[purpose].length; ++i)
        {

            if (!tourIsInRange(tour, lowerBoundDurationHours[purpose][i],
                    upperBoundDurationHours[purpose][i])) continue;

            if (random < cumProbability[purpose][i])
            {
                int outStops = outboundStops[purpose][i];
                int inbStops = inboundStops[purpose][i];

                if (outStops > 0)
                {
                    CrossBorderStop[] stops = generateOutboundStops(tour, outStops);
                    tour.setOutboundStops(stops);
                }

                if (inbStops > 0)
                {
                    CrossBorderStop[] stops = generateInboundStops(tour, inbStops);
                    tour.setInboundStops(stops);
                }
                if (tour.getDebugChoiceModels())
                {
                    logger.info("");
                    logger.info("Chose " + outStops + " outbound stops and  " + inbStops
                            + " inbound stops");
                    logger.info("");
                }
                break;
            }
        }

    }

    /**
     * Check if the tour duration is in range
     * 
     * @param tour
     * @param lowerBound
     * @param upperBound
     * @return True if tour duration is greater than or equal to lower and
     */
    private boolean tourIsInRange(CrossBorderTour tour, int lowerBound, int upperBound)
    {

        float depart = (float) tour.getDepartTime();
        float arrive = (float) tour.getArriveTime();

        float halfHours = arrive + 1 - depart; // at least 30 minutes
        float tourDurationInHours = halfHours * (float) 0.5;

        if (tourDurationInHours >= lowerBound && tourDurationInHours <= upperBound) return true;

        return false;
    }

    /**
     * Generate an array of outbound stops, from tour origin to primary
     * destination, in order.
     * 
     * @param tour
     *            The parent tour.
     * @param numberOfStops
     *            Number of stops from stop frequency model.
     * @return The array of outbound stops.
     */
    private CrossBorderStop[] generateOutboundStops(CrossBorderTour tour, int numberOfStops)
    {

        CrossBorderStop[] stops = new CrossBorderStop[numberOfStops];

        for (int i = 0; i < stops.length; ++i)
        {
            CrossBorderStop stop = new CrossBorderStop(tour, i, false);
            stops[i] = stop;
            stop.setInbound(false);
            stop.setParentTour(tour);
        }

        return stops;
    }

    /**
     * Generate an array of inbound stops, from primary dest back to tour
     * origin, in order.
     * 
     * @param tour
     *            Parent tour.
     * @param numberOfStops
     *            Number of stops from stop frequency model.
     * @return The array of inbound stops.
     */
    private CrossBorderStop[] generateInboundStops(CrossBorderTour tour, int numberOfStops)
    {

        CrossBorderStop[] stops = new CrossBorderStop[numberOfStops];

        for (int i = 0; i < stops.length; ++i)
        {
            CrossBorderStop stop = new CrossBorderStop(tour, i, true);
            stops[i] = stop;
            stop.setInbound(true);
            stop.setParentTour(tour);

        }

        return stops;
    }

}
