package org.sandag.abm.specialevent;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;
import com.pb.common.math.MersenneTwister;

public class SpecialEventTour
        implements Serializable
{

    private MersenneTwister       random;
    private int                   ID;

    private byte                  eventNumber;
    private String                eventType;
    // following variables determined via simulation
    private int                   income;
    private int                   partySize;

    private SpecialEventTrip[]    trips;

    private int                   departTime;
    private int                   arriveTime;

    private boolean               debugChoiceModels;

    // following variables chosen via choice models
    private int                   originMGRA;
    private int                   destinationMGRA;
    private byte                  tourMode;

    private float valueOfTime;
    
    /**
     * Public constructor.
     * 
     * @param seed
     *            A seed for the random number generator.
     */
    public SpecialEventTour(long seed)
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
     * @return the eventNumber
     */
    public byte getEventNumber()
    {
        return eventNumber;
    }

    /**
     * @param eventNumber
     *            the eventNumber to set
     */
    public void setEventNumber(byte eventNumber)
    {
        this.eventNumber = eventNumber;
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

    public SpecialEventTrip[] getTrips()
    {
        return trips;
    }

    /**
     * @return the eventType
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * @param eventType
     *            the eventType to set
     */
    public void setEventType(String eventType)
    {
        this.eventType = eventType;
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
     * @return the partySize
     */
    public int getPartySize()
    {
        return partySize;
    }

    /**
     * @param partySize
     *            the partySize to set
     */
    public void setPartySize(int partySize)
    {
        this.partySize = partySize;
    }

    public void setTrips(SpecialEventTrip[] trips)
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

  

    /**
     * Get the number of outbound stops
     * 
     * @return 0 if not initialized, else number of stops
     */
    public int getNumberOutboundStops()
    {
        return 0;

    }

    /**
     * Get the number of return stops
     * 
     * @return 0 if not initialized, else number of stops
     */
    public int getNumberInboundStops()
    {
        return 0;

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

    public float getValueOfTime() {
		return valueOfTime;
	}

	public void setValueOfTime(float valueOfTime) {
		this.valueOfTime = valueOfTime;
	}

	public void logTourObject(Logger logger, int totalChars)
    {

        Household.logHelper(logger, "tourId: ", ID, totalChars);
        Household.logHelper(logger, "Event type: ", eventType, totalChars);
        Household.logHelper(logger, "tourOrigMgra: ", originMGRA, totalChars);
        Household.logHelper(logger, "tourDestMgra: ", destinationMGRA, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", departTime, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", arriveTime, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
      
    }
}
