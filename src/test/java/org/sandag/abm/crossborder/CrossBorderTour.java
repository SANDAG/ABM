package org.sandag.abm.crossborder;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;
import com.pb.common.math.MersenneTwister;

public class CrossBorderTour
        implements Serializable
{

    private MersenneTwister       random;
    private int                   ID;

    // following variables determined via simulation
    private byte                  purpose;
    private boolean               sentriAvailable;

    private CrossBorderStop[]     outboundStops;
    private CrossBorderStop[]     inboundStops;

    private CrossBorderTrip[]     trips;

    private int                   departTime;
    private int                   arriveTime;

    private boolean               debugChoiceModels;

    // following variables chosen via choice models
    private int                   poe;
    private int                   originMGRA;
    private int                   destinationMGRA;
    private int                   originTAZ;
    private int                   destinationTAZ;
    private byte                  tourMode;
    private float                workTimeFactor;
    private float                nonWorkTimeFactor;
    private float                valueOfTime;
    
    private boolean              avAvailable;

    /**
     * Public constructor.
     * 
     * @param seed
     *            A seed for the random number generator.
     */
    public CrossBorderTour(long seed)
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
     * @return the sentriAvailable
     */
    public boolean isSentriAvailable()
    {
        return sentriAvailable;
    }

    /**
     * @return the poe
     */
    public int getPoe()
    {
        return poe;
    }

    /**
     * @param poe
     *            the poe to set
     */
    public void setPoe(int poe)
    {
        this.poe = poe;
    }

    /**
     * @param sentriAvailable
     *            the sentriAvailable to set
     */
    public void setSentriAvailable(boolean sentriAvailable)
    {
        this.sentriAvailable = sentriAvailable;
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
    public CrossBorderStop[] getOutboundStops()
    {
        return outboundStops;
    }

    /**
     * @param outboundStops
     *            the outboundStops to set
     */
    public void setOutboundStops(CrossBorderStop[] outboundStops)
    {
        this.outboundStops = outboundStops;
    }

    /**
     * @return the inboundStops
     */
    public CrossBorderStop[] getInboundStops()
    {
        return inboundStops;
    }

    /**
     * @param inboundStops
     *            the inboundStops to set
     */
    public void setInboundStops(CrossBorderStop[] inboundStops)
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

    public CrossBorderTrip[] getTrips()
    {
        return trips;
    }

    public void setTrips(CrossBorderTrip[] trips)
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

    public int getOriginTAZ()
    {
        return originTAZ;
    }

    public void setOriginTAZ(int originTAZ)
    {
        this.originTAZ = originTAZ;
    }

    public int getDestinationTAZ()
    {
        return destinationTAZ;
    }

    public void setDestinationTAZ(int destinationTAZ)
    {
        this.destinationTAZ = destinationTAZ;
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

    public double getWorkTimeFactor() {
		return workTimeFactor;
	}

	public void setWorkTimeFactor(float workTimeFactor) {
		this.workTimeFactor = workTimeFactor;
	}

	public double getNonWorkTimeFactor() {
		return nonWorkTimeFactor;
	}

	public void setNonWorkTimeFactor(float nonWorkTimeFactor) {
		this.nonWorkTimeFactor = nonWorkTimeFactor;
	}

	public float getValueOfTime() {
		return valueOfTime;
	}

	public void setValueOfTime(float valueOfTime) {
		this.valueOfTime = valueOfTime;
	}

	public boolean isAvAvailable() {
		return avAvailable;
	}

	public void setAvAvailable(boolean avAvailable) {
		this.avAvailable = avAvailable;
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
        Household.logHelper(logger, "avAvailable:", (avAvailable ? 0 : 1), totalChars);
        
        String tempString = null;
      
        logger.info(tempString);

            logger.info(tempString);

    }
}
