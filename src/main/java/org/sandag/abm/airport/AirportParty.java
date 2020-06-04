package org.sandag.abm.airport;

import java.io.Serializable;
import com.pb.common.math.MersenneTwister;

public class AirportParty
        implements Serializable
{

    private MersenneTwister random;
    private int             ID;

    // following variables determined via simulation
    private byte            direction;
    private byte            purpose;
    private byte            size;
    private byte            income;
    private int             departTime;
    private byte            nights;

    private boolean         debugChoiceModels;

    // following variables chosen via choice models
    private int             originMGRA;
    private int             destinationMGRA;
    private int				airportAccessMGRA;

	private byte            mode;
    private byte            arrivalMode;

    // best tap pairs for transit path; dimensioned by ride mode, then boarding
    // (0) and alighting (1)
    private int[][]         bestWtwTapPairs;
    private int[][]         bestWtdTapPairs;
    private int[][]         bestDtwTapPairs;

    /**
     * Public constructor.
     * 
     * @param seed
     *            A seed for the random number generator.
     */
    public AirportParty(long seed)
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
     * @param purpose
     *            the purpose to set
     */
    public void setPurpose(byte purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return the size
     */
    public byte getSize()
    {
        return size;
    }

    /**
     * @param size
     *            the size to set
     */
    public void setSize(byte size)
    {
        this.size = size;
    }

    /**
     * @return the income
     */
    public byte getIncome()
    {
        return income;
    }

    /**
     * @param income
     *            the income to set
     */
    public void setIncome(byte income)
    {
        this.income = income;
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

    /**
     * @return the direction
     */
    public byte getDirection()
    {
        return direction;
    }

    /**
     * @param direction
     *            the direction to set
     */
    public void setDirection(byte direction)
    {
        this.direction = direction;
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
    
    public int getAirportAccessMGRA() {
		return airportAccessMGRA;
	}

	public void setAirportAccessMGRA(int airportAccessMGRA) {
		this.airportAccessMGRA = airportAccessMGRA;
	}

    /**
     * @return the trip mode
     */
    public byte getMode()
    {
        return mode;
    }

    /**
     * @param mode
     *            the trip mode to set
     */
    public void setMode(byte mode)
    {
        this.mode = mode;
    }

    /**
     * @return the arrivalMode
     */
    public byte getArrivalMode()
    {
        return arrivalMode;
    }

    /**
     * @param arrivalMode
     *            the arrivalMode to set
     */
    public void setArrivalMode(byte arrivalMode)
    {
        this.arrivalMode = arrivalMode;
    }

    /**
     * @return the nights
     */
    public byte getNights()
    {
        return nights;
    }

    /**
     * @param nights
     *            the nights to set
     */
    public void setNights(byte nights)
    {
        this.nights = nights;
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
     * @return the bestWtwTapPairs
     */
    public int[][] getBestWtwTapPairs()
    {
        return bestWtwTapPairs;
    }

    /**
     * Return an array of boarding and alighting tap for the ride mode
     * 
     * @param rideMode
     * @return
     */
    public int[] getWtwTapPair(int rideMode)
    {
        return bestWtwTapPairs[rideMode];
    }

    /**
     * @param bestWtwTapPairs
     *            the bestWtwTapPairs to set
     */
    public void setBestWtwTapPairs(int[][] bestWtwTapPairs)
    {
        this.bestWtwTapPairs = bestWtwTapPairs;
    }

    /**
     * @return the bestWtdTapPairs
     */
    public int[][] getBestWtdTapPairs()
    {
        return bestWtdTapPairs;
    }

    /**
     * Return an array of boarding and alighting tap for the ride mode
     * 
     * @param rideMode
     * @return
     */
    public int[] getWtdTapPair(int rideMode)
    {
        return bestWtdTapPairs[rideMode];
    }

    /**
     * @param bestWtdTapPairs
     *            the bestWtdTapPairs to set
     */
    public void setBestWtdTapPairs(int[][] bestWtdTapPairs)
    {
        this.bestWtdTapPairs = bestWtdTapPairs;
    }

    /**
     * @return the bestDtwTapPairs
     */
    public int[][] getBestDtwTapPairs()
    {
        return bestDtwTapPairs;
    }

    /**
     * Return an array of boarding and alighting tap for the ride mode
     * 
     * @param rideMode
     * @return
     */
    public int[] getDtwTapPair(int rideMode)
    {
        return bestDtwTapPairs[rideMode];
    }

    /**
     * @param bestDtwTapPairs
     *            the bestDtwTapPairs to set
     */
    public void setBestDtwTapPairs(int[][] bestDtwTapPairs)
    {
        this.bestDtwTapPairs = bestDtwTapPairs;
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

}
