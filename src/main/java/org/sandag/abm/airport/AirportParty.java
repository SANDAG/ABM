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
    private int             originTAZ;
    private int             destinationTAZ;
    private byte            mode;
    private byte            arrivalMode;
    
    private float           valueOfTime;
    
    private int				airportAccessMGRA;
    
    private double[][]		APtoTermBestWtwTapPairs;
	private int				AP2TerminalTransitMode;

    private int     boardTap;
    private int     alightTap;
    private int     set = -1;
    
    private int		AP2TerminalBoardTap;
	private int     AP2TerminalAlightTap;
    private int     AP2TerminalSet = -1;

    private boolean avAvailable;
    
    private boolean APHasPublicTransit;
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

	public float getValueOfTime() {
		return valueOfTime;
	}

	public void setValueOfTime(float valueOfTime) {
		this.valueOfTime = valueOfTime;
	}

	public int getBoardTap() {
		return boardTap;
	}

	public void setBoardTap(int boardTap) {
		this.boardTap = boardTap;
	}

	public int getAlightTap() {
		return alightTap;
	}

	public void setAlightTap(int alightTap) {
		this.alightTap = alightTap;
	}

	public int getSet() {
		return set;
	}

	public void setSet(int set) {
		this.set = set;
	}

	public int getOriginTAZ() {
		return originTAZ;
	}

	public void setOriginTAZ(int originTAZ) {
		this.originTAZ = originTAZ;
	}

	public int getDestinationTAZ() {
		return destinationTAZ;
	}

	public void setDestinationTAZ(int destinationTAZ) {
		this.destinationTAZ = destinationTAZ;
	}

	public boolean getAvAvailable() {
		return avAvailable;
	}

	public void setAvAvailable(boolean avAvailable) {
		this.avAvailable = avAvailable;
	}
	
	public double[][] getAPtoTermBestWtwTapPairs() {
		return APtoTermBestWtwTapPairs;
	}

	public void setAPtoTermBestWtwTapPairs(double[][] aPtoTermBestWtwTapPairs) {
		APtoTermBestWtwTapPairs = aPtoTermBestWtwTapPairs;
	}

	public int getAP2TerminalTransitMode() {
		return AP2TerminalTransitMode;
	}

	public void setAP2TerminalTransitMode(int aP2TerminalTransitMode) {
		AP2TerminalTransitMode = aP2TerminalTransitMode;
	}
	
	public int getAP2TerminalBoardTap() {
		return AP2TerminalBoardTap;
	}

	public void setAP2TerminalBoardTap(int aP2TerminalBoardTap) {
		AP2TerminalBoardTap = aP2TerminalBoardTap;
	}

	public int getAP2TerminalAlightTap() {
		return AP2TerminalAlightTap;
	}

	public void setAP2TerminalAlightTap(int aP2TerminalAlightTap) {
		AP2TerminalAlightTap = aP2TerminalAlightTap;
	}

	public int getAP2TerminalSet() {
		return AP2TerminalSet;
	}

	public void setAP2TerminalSet(int aP2TerminalSet) {
		AP2TerminalSet = aP2TerminalSet;
	}
	
	public void setAPHasPublicTransit(boolean apHasPublicTransit) {
		this.APHasPublicTransit = apHasPublicTransit;
	}
	
	public boolean getAPHasPublicTransit() {
		return this.APHasPublicTransit;
	}


}
