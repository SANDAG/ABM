package org.sandag.abm.crossborder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Util;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class is the TOD choice model for cross border tours. It is currently
 * based on a static probability distribution stored in an input file, and
 * indexed into by purpose.
 * 
 * @author Freedman
 * 
 */
public class CrossBorderStopTimeOfDayChoiceModel
{
    private transient Logger          logger = Logger.getLogger("crossBorderModel");

    private double[][]                outboundCumProbability;                       // by
                                                                                     // alternative:
                                                                                     // outbound
                                                                                     // cumulative
                                                                                     // probability
                                                                                     // distribution
    private int[]                     outboundOffsets;                              // by
                                                                                     // alternative:
                                                                                     // offsets
                                                                                     // for
                                                                                     // outbound
                                                                                     // stop
                                                                                     // duration
                                                                                     // choice

    private double[][]                inboundCumProbability;                        // by
                                                                                     // alternative:
                                                                                     // inbound
                                                                                     // cumulative
                                                                                     // probability
                                                                                     // distribution
    private int[]                     inboundOffsets;                               // by
                                                                                     // alternative:
                                                                                     // offsets
                                                                                     // for
                                                                                     // inbound
                                                                                     // stop
                                                                                     // duration
                                                                                     // choice
    private CrossBorderModelStructure modelStructure;

    private HashMap<Integer, Integer> outboundElementMap;                           // Hashmap
                                                                                     // used
                                                                                     // to
                                                                                     // get
                                                                                     // the
                                                                                     // element
                                                                                     // number
                                                                                     // of
                                                                                     // the
                                                                                     // cumProbability
                                                                                     // array
                                                                                     // based
                                                                                     // on
                                                                                     // the
    // tour duration and stop number.

    private HashMap<Integer, Integer> inboundElementMap;                            // Hashmap
                                                                                     // used
                                                                                     // to
                                                                                     // get
                                                                                     // the
                                                                                     // element
                                                                                     // number
                                                                                     // of
                                                                                     // the
                                                                                     // cumProbability
                                                                                     // array
                                                                                     // based
                                                                                     // on
                                                                                     // the

    // tour duration and stop number.

    /**
     * Constructor.
     */
    public CrossBorderStopTimeOfDayChoiceModel(HashMap<String, String> rbMap)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String outboundDurationFile = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.stop.outbound.duration.file");
        String inboundDurationFile = Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.stop.inbound.duration.file");

        outboundDurationFile = directory + outboundDurationFile;
        inboundDurationFile = directory + inboundDurationFile;

        modelStructure = new CrossBorderModelStructure();

        outboundElementMap = new HashMap<Integer, Integer>();
        readOutboundFile(outboundDurationFile);

        inboundElementMap = new HashMap<Integer, Integer>();
        readInboundFile(inboundDurationFile);
    }

    /**
     * Read the outbound stop duration file and store the cumulative probability
     * distribution as well as the offsets and set the key map to index into the
     * probability array.
     * 
     * @param fileName
     */
    public void readOutboundFile(String fileName)
    {
        TableDataSet outboundTable;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            outboundTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        int columns = outboundTable.getColumnCount();
        int rows = outboundTable.getRowCount();
        outboundCumProbability = new double[rows][columns - 3];

        // first three columns are index fields, rest are offsets
        outboundOffsets = new int[columns - 3];
        for (int i = 4; i <= columns; ++i)
        {
            String offset = outboundTable.getColumnLabel(i);
            outboundOffsets[i - 4] = new Integer(offset);
        }

        // now fill in cumulative probability array
        for (int row = 1; row <= rows; ++row)
        {

            int lowerBound = (int) outboundTable.getValueAt(row, "RemainingLow");
            int upperBound = (int) outboundTable.getValueAt(row, "RemainingHigh");
            int stopNumber = (int) outboundTable.getValueAt(row, "Stop");

            for (int duration = lowerBound; duration <= upperBound; ++duration)
            {
                int key = getKey(stopNumber, duration);
                outboundElementMap.put(key, row - 1);
            }

            // cumulative probability distribution
            double cumProb = 0;
            for (int col = 4; col <= columns; ++col)
            {
                cumProb += outboundTable.getValueAt(row, col);
                outboundCumProbability[row - 1][col - 4] = cumProb;
            }

        }

    }

    /**
     * Read the inbound stop duration file and store the cumulative probability
     * distribution as well as the offsets and set the key map to index into the
     * probability array.
     * 
     * @param fileName
     */
    public void readInboundFile(String fileName)
    {
        TableDataSet inboundTable;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            inboundTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        int columns = inboundTable.getColumnCount();
        int rows = inboundTable.getRowCount();
        inboundCumProbability = new double[rows][columns - 3];

        // first three columns are index fields, rest are offsets
        inboundOffsets = new int[columns - 3];
        for (int i = 4; i <= columns; ++i)
        {
            String offset = inboundTable.getColumnLabel(i);
            inboundOffsets[i - 4] = new Integer(offset);
        }

        // now fill in cumulative probability array
        for (int row = 1; row <= rows; ++row)
        {

            int lowerBound = (int) inboundTable.getValueAt(row, "RemainingLow");
            int upperBound = (int) inboundTable.getValueAt(row, "RemainingHigh");
            int stopNumber = (int) inboundTable.getValueAt(row, "Stop");

            for (int duration = lowerBound; duration <= upperBound; ++duration)
            {
                int key = getKey(stopNumber, duration);
                inboundElementMap.put(key, row - 1);
            }
            // cumulative probability distribution
            double cumProb = 0;
            for (int col = 4; col <= columns; ++col)
            {
                cumProb += inboundTable.getValueAt(row, col);
                inboundCumProbability[row - 1][col - 4] = cumProb;
            }

        }

    }

    /**
     * Get the key for the arrayElementMap.
     * 
     * @param stopNumber
     *            stop number
     * @param periodsRemaining
     *            Remaining time periods
     * @return arrayElementMap key.
     */
    private int getKey(int stopNumber, int periodsRemaining)
    {

        return periodsRemaining * 10 + stopNumber;
    }

    /**
     * Choose the stop time of day period.
     * 
     * @param tour
     * @param stop
     */
    public void chooseTOD(CrossBorderTour tour, CrossBorderStop stop)
    {

        boolean inbound = stop.isInbound();
        int stopNumber = stop.getId() + 1;
        int arrivalPeriod = tour.getArriveTime();

        if (!inbound)
        {

            // find the departure time
            int departPeriod = 0;
            if (stop.getId() == 0) departPeriod = tour.getDepartTime();
            else
            {
                CrossBorderStop[] stops = tour.getOutboundStops();
                departPeriod = stops[stop.getId() - 1].getStopPeriod();
            }

            int periodsRemaining = arrivalPeriod - departPeriod;

            int key = getKey(stopNumber, periodsRemaining);
            int element = outboundElementMap.get(key);
            double[] cumProb = outboundCumProbability[element];
            double random = tour.getRandom();

            // iterate through the offset distribution, choose an offset, and
            // set in the stop
            if (tour.getDebugChoiceModels())
            {
                logger.info("Stop TOD Choice Model for tour " + tour.getID() + " outbound stop "
                        + stop.getId() + " periods remaining " + periodsRemaining);
                logger.info(" random number " + random);
            }
            for (int i = 0; i < cumProb.length; ++i)
            {
                if (random < cumProb[i])
                {
                    int offset = outboundOffsets[i];
                    int period = departPeriod + offset;
                    stop.setPeriod(period);

                    if (tour.getDebugChoiceModels())
                    {
                        logger.info("***");
                        logger.info("Chose alt " + i + " offset " + offset + " from depart period "
                                + departPeriod);
                        logger.info("Stop period is " + stop.getStopPeriod());

                    }
                    break;

                }
            }
        } else
        {
            // inbound stop

            // find the departure time
            int departPeriod = 0;

            // first inbound stop
            if (stop.getId() == 0)
            {

                // there were outbound stops
                if (tour.getOutboundStops() != null)
                {
                    CrossBorderStop[] outboundStops = tour.getOutboundStops();
                    departPeriod = outboundStops[outboundStops.length - 1].getStopPeriod();
                } else
                {
                    // no outbound stops
                    departPeriod = tour.getDepartTime();
                }
            } else
            {
                // not first inbound stop
                CrossBorderStop[] stops = tour.getInboundStops();
                departPeriod = stops[stop.getId() - 1].getStopPeriod();
            }

            int periodsRemaining = arrivalPeriod - departPeriod;

            int key = getKey(stopNumber, periodsRemaining);
            int element = inboundElementMap.get(key);
            double[] cumProb = inboundCumProbability[element];
            double random = tour.getRandom();
            if (tour.getDebugChoiceModels())
            {
                logger.info("Stop TOD Choice Model for tour " + tour.getID() + " inbound stop "
                        + stop.getId() + " periods remaining " + periodsRemaining);
                logger.info("Random number " + random);
            }
            for (int i = 0; i < cumProb.length; ++i)
            {
                if (random < cumProb[i])
                {
                    int offset = inboundOffsets[i];
                    int arrivePeriod = tour.getArriveTime();
                    int period = arrivePeriod + offset;
                    stop.setPeriod(period);

                    if (tour.getDebugChoiceModels())
                    {
                        logger.info("***");
                        logger.info("Chose alt " + i + " offset " + offset + " from arrive period "
                                + arrivePeriod);
                        logger.info("Stop period is " + stop.getStopPeriod());

                    }
                    break;
                }
            }
        }

    }

}
