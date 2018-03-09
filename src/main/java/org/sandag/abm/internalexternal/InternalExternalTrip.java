package org.sandag.abm.internalexternal;

import java.io.Serializable;
import org.sandag.abm.modechoice.MgraDataManager;

public class InternalExternalTrip
        implements Serializable
{

    private int originMgra;
    private int destinationMgra;

  
    private int     originTaz;
    private int     destinationTaz;

    private int     tripMode;
    private byte    period;
    private boolean inbound;
    private boolean firstTrip;
    private boolean lastTrip;
    private boolean originIsTourDestination;
    private boolean destinationIsTourDestination;

 	private int boardTap;
 	private int alightTap;
 	private int set = -1;
    
    private double valueOfTime;

    /**
     * Default constructor; nothing initialized.
     */
    public InternalExternalTrip()
    {

    }

    /**
     * Create a cross border trip from a tour leg (no stops).
     * 
     * @param tour
     *            The tour.
     * @param outbound
     *            Outbound direction
     */
    public InternalExternalTrip(InternalExternalTour tour, boolean outbound,
            MgraDataManager mgraManager)
    {

        initializeFromTour(tour, outbound, mgraManager);
    }

    /**
     * Initilize from the tour.
     * 
     * @param tour
     *            The tour.
     * @param outbound
     *            Outbound direction.
     */
    public void initializeFromTour(InternalExternalTour tour, boolean outbound,
            MgraDataManager mgraManager)
    {
        // Note: mode is unknown
        if (outbound)
        {
            this.originMgra = tour.getOriginMGRA();
            this.originTaz = mgraManager.getTaz(tour.getOriginMGRA());
            this.destinationMgra = tour.getDestinationMGRA();
            this.destinationTaz = tour.getDestinationTAZ();
            this.period = (byte) tour.getDepartTime();
            this.inbound = false;
            this.firstTrip = true;
            this.lastTrip = false;
            this.originIsTourDestination = false;
            this.destinationIsTourDestination = true;
        } else
        {
            this.originMgra = tour.getDestinationMGRA();
            this.originTaz = tour.getDestinationTAZ();
            this.destinationMgra = tour.getOriginMGRA();
            this.destinationTaz = mgraManager.getTaz(tour.getOriginMGRA());
            this.period = (byte) tour.getArriveTime();
            this.inbound = true;
            this.firstTrip = false;
            this.lastTrip = true;
            this.originIsTourDestination = true;
            this.destinationIsTourDestination = false;
        }

    }
    /**
     * @param destinationTaz
     *            the destinationTaz to set
     */
    public void setDestinationTaz(int destinationTaz)
    {
        this.destinationTaz = destinationTaz;
    }


    /**
     * @return the period
     */
    public byte getPeriod()
    {
        return period;
    }

    /**
     * @param period
     *            the period to set
     */
    public void setPeriod(byte period)
    {
        this.period = period;
    }

    /**
     * @return the originMgra
     */
    public int getOriginMgra()
    {
        return originMgra;
    }

    /**
     * @param originMgra
     *            the originMgra to set
     */
    public void setOriginMgra(int originMgra)
    {
        this.originMgra = originMgra;
    }

    /**
     * @return the destinationMgra
     */
    public int getDestinationMgra()
    {
        return destinationMgra;
    }

    /**
     * @param destinationMgra
     *            the destinationMgra to set
     */
    public void setDestinationMgra(int destinationMgra)
    {
        this.destinationMgra = destinationMgra;
    }

    /**
     * @return the tripMode
     */
    public int getTripMode()
    {
        return tripMode;
    }

    /**
     * @param tripMode
     *            the tripMode to set
     */
    public void setTripMode(int tripMode)
    {
        this.tripMode = tripMode;
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


    /**
     * @return the inbound
     */
    public boolean isInbound()
    {
        return inbound;
    }

    /**
     * @param inbound
     *            the inbound to set
     */
    public void setInbound(boolean inbound)
    {
        this.inbound = inbound;
    }

    /**
     * @return the firstTrip
     */
    public boolean isFirstTrip()
    {
        return firstTrip;
    }

    /**
     * @param firstTrip
     *            the firstTrip to set
     */
    public void setFirstTrip(boolean firstTrip)
    {
        this.firstTrip = firstTrip;
    }

    /**
     * @return the lastTrip
     */
    public boolean isLastTrip()
    {
        return lastTrip;
    }

    /**
     * @param lastTrip
     *            the lastTrip to set
     */
    public void setLastTrip(boolean lastTrip)
    {
        this.lastTrip = lastTrip;
    }

    /**
     * @return the originIsTourDestination
     */
    public boolean isOriginIsTourDestination()
    {
        return originIsTourDestination;
    }

    /**
     * @param originIsTourDestination
     *            the originIsTourDestination to set
     */
    public void setOriginIsTourDestination(boolean originIsTourDestination)
    {
        this.originIsTourDestination = originIsTourDestination;
    }

    /**
     * @return the destinationIsTourDestination
     */
    public boolean isDestinationIsTourDestination()
    {
        return destinationIsTourDestination;
    }

    /**
     * @param destinationIsTourDestination
     *            the destinationIsTourDestination to set
     */
    public void setDestinationIsTourDestination(boolean destinationIsTourDestination)
    {
        this.destinationIsTourDestination = destinationIsTourDestination;
    }

    /**
     * @return the originTaz
     */
    public int getOriginTaz()
    {
        return originTaz;
    }

    /**
     * @param originTaz
     *            the originTaz to set
     */
    public void setOriginTaz(int originTaz)
    {
        this.originTaz = originTaz;
    }

    /**
     * @return the destinationTaz
     */
    public int getDestinationTaz()
    {
        return destinationTaz;
    }

	public double getValueOfTime() {
		return valueOfTime;
	}

	public void setValueOfTime(double valueOfTime) {
		this.valueOfTime = valueOfTime;
	}

}
