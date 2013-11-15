package org.sandag.abm.visitor;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;
import com.pb.common.math.MersenneTwister;

public class VisitorTour
        implements Serializable
{

    private MersenneTwister       random;
    private int                   ID;

    // following variables determined via simulation
    private byte                  segment;                                            // 0
                                                                                       // =
                                                                                       // business,
                                                                                       // 1
                                                                                       // =
                                                                                       // personal
    private byte                  purpose;
    private byte                  numberOfParticipants;
    private int                   income;
    private int                   autoAvailable;

    private VisitorStop[]         outboundStops;
    private VisitorStop[]         inboundStops;

    private VisitorTrip[]         trips;

    private int                   departTime;
    private int                   arriveTime;

    private boolean               debugChoiceModels;

    // following variables chosen via choice models
    private int                   originMGRA;
    private int                   destinationMGRA;
    private byte                  tourMode;

    // best tap pairs for transit path; dimensioned by ride mode, then boarding
    // (0) and alighting (1)
    private int[][]               bestWtwTapPairsOut;
    private int[][]               bestWtwTapPairsIn;
    private int[][]               bestWtdTapPairsOut;
    private int[][]               bestWtdTapPairsIn;
    private int[][]               bestDtwTapPairsOut;
    private int[][]               bestDtwTapPairsIn;

    private static final String[] RIDE_MODE_LABELS = {"CR", "LRT", "BRT", "EB", "LB"};

    /**
     * Public constructor.
     * 
     * @param seed
     *            A seed for the random number generator.
     */
    public VisitorTour(long seed)
    {

        random = new MersenneTwister(seed);
    }

    /**
     * @return the iD
     */
    public int getID()
    {
        return ID;
    }

    /**
     * @param iD
     *            the iD to set
     */
    public void setID(int iD)
    {
        ID = iD;
    }

    /**
     * @return the purpose
     */
    public byte getPurpose()
    {
        return purpose;
    }

    /**
     * @return the outboundStops
     */
    public VisitorStop[] getOutboundStops()
    {
        return outboundStops;
    }

    /**
     * @param outboundStops
     *            the outboundStops to set
     */
    public void setOutboundStops(VisitorStop[] outboundStops)
    {
        this.outboundStops = outboundStops;
    }

    /**
     * @return the inboundStops
     */
    public VisitorStop[] getInboundStops()
    {
        return inboundStops;
    }

    /**
     * @param inboundStops
     *            the inboundStops to set
     */
    public void setInboundStops(VisitorStop[] inboundStops)
    {
        this.inboundStops = inboundStops;
    }

    /**
     * @param purpose
     *            the purpose to set
     */
    public void setPurpose(byte purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return the departTime
     */
    public int getDepartTime()
    {
        return departTime;
    }

    /**
     * @param departTime
     *            the departTime to set
     */
    public void setDepartTime(int departTime)
    {
        this.departTime = departTime;
    }

    public VisitorTrip[] getTrips()
    {
        return trips;
    }

    public void setTrips(VisitorTrip[] trips)
    {
        this.trips = trips;
    }

    /**
     * @return the originMGRA
     */
    public int getOriginMGRA()
    {
        return originMGRA;
    }

    /**
     * @param originMGRA
     *            the originMGRA to set
     */
    public void setOriginMGRA(int originMGRA)
    {
        this.originMGRA = originMGRA;
    }

    /**
     * @return the tour mode
     */
    public byte getTourMode()
    {
        return tourMode;
    }

    /**
     * @param mode
     *            the tour mode to set
     */
    public void setTourMode(byte mode)
    {
        this.tourMode = mode;
    }

    /**
     * Get a random number from the parties random class.
     * 
     * @return A random number.
     */
    public double getRandom()
    {
        return random.nextDouble();
    }

    /**
     * @return the debugChoiceModels
     */
    public boolean getDebugChoiceModels()
    {
        return debugChoiceModels;
    }

    /**
     * @param debugChoiceModels
     *            the debugChoiceModels to set
     */
    public void setDebugChoiceModels(boolean debugChoiceModels)
    {
        this.debugChoiceModels = debugChoiceModels;
    }

    public void setBestWtwTapPairsOut(int[][] tapPairArray)
    {
        bestWtwTapPairsOut = tapPairArray;
    }

    public void setBestWtwTapPairsIn(int[][] tapPairArray)
    {
        bestWtwTapPairsIn = tapPairArray;
    }

    public void setBestWtdTapPairsOut(int[][] tapPairArray)
    {
        bestWtdTapPairsOut = tapPairArray;
    }

    public void setBestWtdTapPairsIn(int[][] tapPairArray)
    {
        bestWtdTapPairsIn = tapPairArray;
    }

    public void setBestDtwTapPairsOut(int[][] tapPairArray)
    {
        bestDtwTapPairsOut = tapPairArray;
    }

    public void setBestDtwTapPairsIn(int[][] tapPairArray)
    {
        bestDtwTapPairsIn = tapPairArray;
    }

    public int[][] getBestWtwTapPairsOut()
    {
        return bestWtwTapPairsOut;
    }

    public int[][] getBestWtwTapPairsIn()
    {
        return bestWtwTapPairsIn;
    }

    public int[][] getBestWtdTapPairsOut()
    {
        return bestWtdTapPairsOut;
    }

    public int[][] getBestWtdTapPairsIn()
    {
        return bestWtdTapPairsIn;
    }

    public int[][] getBestDtwTapPairsOut()
    {
        return bestDtwTapPairsOut;
    }

    public int[][] getBestDtwTapPairsIn()
    {
        return bestDtwTapPairsIn;
    }

    /**
     * Get the number of outbound stops
     * 
     * @return 0 if not initialized, else number of stops
     */
    public int getNumberOutboundStops()
    {
        if (outboundStops == null) return 0;
        else return outboundStops.length;

    }

    /**
     * Get the number of return stops
     * 
     * @return 0 if not initialized, else number of stops
     */
    public int getNumberInboundStops()
    {
        if (inboundStops == null) return 0;
        else return inboundStops.length;

    }

    /**
     * @return the destinationMGRA
     */
    public int getDestinationMGRA()
    {
        return destinationMGRA;
    }

    /**
     * @param destinationMGRA
     *            the destinationMGRA to set
     */
    public void setDestinationMGRA(int destinationMGRA)
    {
        this.destinationMGRA = destinationMGRA;
    }

    public void setArriveTime(int arriveTime)
    {
        this.arriveTime = arriveTime;
    }

    public int getArriveTime()
    {
        return arriveTime;
    }

    /**
     * @return the numberOfParticipants
     */
    public byte getNumberOfParticipants()
    {
        return numberOfParticipants;
    }

    /**
     * @param numberOfParticipants
     *            the numberOfParticipants to set
     */
    public void setNumberOfParticipants(byte numberOfParticipants)
    {
        this.numberOfParticipants = numberOfParticipants;
    }

    /**
     * @return the income
     */
    public int getIncome()
    {
        return income;
    }

    /**
     * @param income
     *            the income to set
     */
    public void setIncome(int income)
    {
        this.income = income;
    }

    /**
     * @return the autoAvailable
     */
    public int getAutoAvailable()
    {
        return autoAvailable;
    }

    /**
     * @param autoAvailable
     *            the autoAvailable to set
     */
    public void setAutoAvailable(int autoAvailable)
    {
        this.autoAvailable = autoAvailable;
    }

    /**
     * @return the segment
     */
    public byte getSegment()
    {
        return segment;
    }

    /**
     * @param segment
     *            the segment to set
     */
    public void setSegment(byte segment)
    {
        this.segment = segment;
    }

    public void logTourObject(Logger logger, int totalChars)
    {

        Household.logHelper(logger, "tourId: ", ID, totalChars);
        Household.logHelper(logger, "tourPurpose: ", purpose, totalChars);
        Household.logHelper(logger, "tourOrigMgra: ", originMGRA, totalChars);
        Household.logHelper(logger, "tourDestMgra: ", destinationMGRA, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", departTime, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", arriveTime, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
        // Household.logHelper(logger, "stopFreqChoice: ", stopFreqChoice,
        // totalChars);

        String tempString = null;
        /*
         * String tempString = String.format("outboundStops[%s]:", outboundStops
         * == null ? "" : String.valueOf(outboundStops.length));
         * logger.info(tempString);
         * 
         * tempString = String.format("inboundStops[%s]:", inboundStops == null
         * ? "" : String.valueOf(inboundStops.length)); logger.info(tempString);
         */

        if (bestWtwTapPairsOut == null)
        {
            tempString = "bestWtwTapPairsOut: no tap pairs saved";
        } else
        {
            if (bestWtwTapPairsOut[0] == null) tempString = "bestWtwTapPairsOut: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestWtwTapPairsOut: " + RIDE_MODE_LABELS[0] + "["
                    + bestWtwTapPairsOut[0][0] + "," + bestWtwTapPairsOut[0][1] + "]";
            for (int i = 1; i < bestWtwTapPairsOut.length; i++)
                if (bestWtwTapPairsOut[i] == null) tempString += ", " + RIDE_MODE_LABELS[i] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestWtwTapPairsOut[i][0]
                        + "," + bestWtwTapPairsOut[i][1] + "]";
        }
        logger.info(tempString);

        if (bestWtwTapPairsIn == null)
        {
            tempString = "bestWtwTapPairsIn: no tap pairs saved";
        } else
        {
            if (bestWtwTapPairsIn[0] == null) tempString = "bestWtwTapPairsIn: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestWtwTapPairsIn: " + RIDE_MODE_LABELS[0] + "["
                    + bestWtwTapPairsIn[0][0] + "," + bestWtwTapPairsIn[0][1] + "]";
            for (int i = 1; i < bestWtwTapPairsIn.length; i++)
                if (bestWtwTapPairsIn[i] == null) tempString += ", " + RIDE_MODE_LABELS[0] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestWtwTapPairsIn[i][0] + ","
                        + bestWtwTapPairsIn[i][1] + "]";
        }
        logger.info(tempString);

        if (bestWtdTapPairsOut == null)
        {
            tempString = "bestWtdTapPairsOut: no tap pairs saved";
        } else
        {
            if (bestWtdTapPairsOut[0] == null) tempString = "bestWtdTapPairsOut: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestWtdTapPairsOut: " + RIDE_MODE_LABELS[0] + "["
                    + bestWtdTapPairsOut[0][0] + "," + bestWtdTapPairsOut[0][1] + "]";
            for (int i = 1; i < bestWtdTapPairsOut.length; i++)
                if (bestWtdTapPairsOut[i] == null) tempString += ", " + RIDE_MODE_LABELS[i] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestWtdTapPairsOut[i][0]
                        + "," + bestWtdTapPairsOut[i][1] + "]";
        }
        logger.info(tempString);

        if (bestWtdTapPairsIn == null)
        {
            tempString = "bestWtdTapPairsIn: no tap pairs saved";
        } else
        {
            if (bestWtdTapPairsIn[0] == null) tempString = "bestWtdTapPairsIn: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestWtdTapPairsIn: " + RIDE_MODE_LABELS[0] + "["
                    + bestWtdTapPairsIn[0][0] + "," + bestWtdTapPairsIn[0][1] + "]";
            for (int i = 1; i < bestWtdTapPairsIn.length; i++)
                if (bestWtdTapPairsIn[i] == null) tempString += ", " + RIDE_MODE_LABELS[0] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestWtdTapPairsIn[i][0] + ","
                        + bestWtdTapPairsIn[i][1] + "]";
        }
        logger.info(tempString);

        if (bestDtwTapPairsOut == null)
        {
            tempString = "bestDtwTapPairsOut: no tap pairs saved";
        } else
        {
            if (bestDtwTapPairsOut[0] == null) tempString = "bestDtwTapPairsOut: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestDtwTapPairsOut: " + RIDE_MODE_LABELS[0] + "["
                    + bestDtwTapPairsOut[0][0] + "," + bestDtwTapPairsOut[0][1] + "]";
            for (int i = 1; i < bestDtwTapPairsOut.length; i++)
                if (bestDtwTapPairsOut[i] == null) tempString += ", " + RIDE_MODE_LABELS[i] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestDtwTapPairsOut[i][0]
                        + "," + bestDtwTapPairsOut[i][1] + "]";
        }
        logger.info(tempString);

        if (bestDtwTapPairsIn == null)
        {
            tempString = "bestDtwTapPairsIn: no tap pairs saved";
        } else
        {
            if (bestDtwTapPairsIn[0] == null) tempString = "bestDtwTapPairsIn: "
                    + RIDE_MODE_LABELS[0] + "[" + "none" + "," + "none" + "]";
            else tempString = "bestDtwTapPairsIn: " + RIDE_MODE_LABELS[0] + "["
                    + bestDtwTapPairsIn[0][0] + "," + bestDtwTapPairsIn[0][1] + "]";
            for (int i = 1; i < bestDtwTapPairsIn.length; i++)
                if (bestDtwTapPairsIn[i] == null) tempString += ", " + RIDE_MODE_LABELS[0] + "["
                        + "none" + "," + "none" + "]";
                else tempString += ", " + RIDE_MODE_LABELS[i] + "[" + bestDtwTapPairsIn[i][0] + ","
                        + bestDtwTapPairsIn[i][1] + "]";
        }
        logger.info(tempString);

    }
}
