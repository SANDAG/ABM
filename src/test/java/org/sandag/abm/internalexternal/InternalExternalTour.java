package org.sandag.abm.internalexternal;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Household;
import com.pb.common.math.MersenneTwister;

public class InternalExternalTour
        implements Serializable
{

    private MersenneTwister        random;
    private int                    ID;
	private int hhID;
    private int personID;
    private int pnum;

	// following variables set from household and person objects
    private int                    income;
    private int                    autos;
    private int                    age;
    private int                    female;
    private double					nonWorkTimeFactor;
    
    private boolean avAvailable;
    private float sampleRate;

    // private InternalExternalStop[] outboundStops;
    // private InternalExternalStop[] inboundStops;

    private InternalExternalTrip[] trips;

    private int                    departTime;
    private int                    arriveTime;

    private boolean                debugChoiceModels;

    // following variables chosen via choice models
    private int                    originMGRA;
    private int                    destinationMGRA;
    private int                    destinationTAZ;   // the external TAZ may be
                                                      // different from the
                                                      // external MGRA

    /**
     * Public constructor.
     * 
     * @param seed
     *            A seed for the random number generator.
     */
    public InternalExternalTour(long seed)
    {

        random = new MersenneTwister(seed);
    }

    /**
     * @return the destinationTAZ
     */
    public int getDestinationTAZ()
    {
        return destinationTAZ;
    }

    /**
     * @param destinationTAZ
     *            the destinationTAZ to set
     */
    public void setDestinationTAZ(int destinationTAZ)
    {
        this.destinationTAZ = destinationTAZ;
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

    public InternalExternalTrip[] getTrips()
    {
        return trips;
    }

    public void setTrips(InternalExternalTrip[] trips)
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

    public int getAutos()
    {
        return autos;
    }

    public void setAutos(int autos)
    {
        this.autos = autos;
    }

    public int getAge()
    {
        return age;
    }

    public void setAge(int age)
    {
        this.age = age;
    }

	public float getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
	}
	
    public int getFemale()
    {
        return female;
    }

    public void setFemale(int female)
    {
        this.female = female;
    }
    
    public int getHhID() {
		return hhID;
	}

	public void setHhID(int hhID) {
		this.hhID = hhID;
	}

	public int getPersonID() {
		return personID;
	}

	public void setPersonID(int personID) {
		this.personID = personID;
	}
	
    public int getPnum() {
		return pnum;
	}

	public void setPnum(int pnum) {
		this.pnum = pnum;
	}

    public double getNonWorkTimeFactor() {
		return nonWorkTimeFactor;
	}

	public void setNonWorkTimeFactor(double nonWorkTimeFactor) {
		this.nonWorkTimeFactor = nonWorkTimeFactor;
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
        Household.logHelper(logger, "tourOrigMgra: ", originMGRA, totalChars);
        Household.logHelper(logger, "tourDestMgra: ", destinationMGRA, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", departTime, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", arriveTime, totalChars);
       
    }
}
