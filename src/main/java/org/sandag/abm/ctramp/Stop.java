package org.sandag.abm.ctramp;

import java.io.Serializable;
import org.apache.log4j.Logger;

public class Stop
        implements Serializable
{

	static final byte STOP_TYPE_PICKUP = 1;
	static final byte STOP_TYPE_DROPOFF = 2;
	static final byte STOP_TYPE_OTHER = 3;
    int     id;
    int     orig;
    int     dest;
    int     park;
    int     mode;
    private float   modeLogsum;
    
    int     stopPeriod;
    int     boardTap;
    int     alightTap;
    boolean inbound;
    int     set = -1;

    private int escorteePnumOrig;
    private byte escortStopTypeOrig;
    private int escorteePnumDest;
    private byte escortStopTypeDest;

    String  origPurpose;
    String  destPurpose;
    int     stopPurposeIndex;

    Tour    parentTour;

    public Stop(Tour parentTour, String origPurpose, String destPurpose, int id, boolean inbound,
            int stopPurposeIndex)
    {
        this.parentTour = parentTour;
        this.origPurpose = origPurpose;
        this.destPurpose = destPurpose;
        this.stopPurposeIndex = stopPurposeIndex;
        this.id = id;
        this.inbound = inbound;
    }

    public void setOrig(int orig)
    {
        this.orig = orig;
    }

    public void setDest(int dest)
    {
        this.dest = dest;
    }

    public void setPark(int park)
    {
        this.park = park;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public void setSet(int Skimset)
    {
        set = Skimset;
    }
    
    public void setBoardTap(int tap)
    {
        boardTap = tap;
    }

    public void setAlightTap(int tap)
    {
        alightTap = tap;
    }

    public void setStopPeriod(int period)
    {
        stopPeriod = period;
    }

    public int getOrig()
    {
        return orig;
    }

    public int getDest()
    {
        return dest;
    }

    public int getPark()
    {
        return park;
    }

    public String getOrigPurpose()
    {
        return origPurpose;
    }

    public String getDestPurpose()
    {
        return destPurpose;
    }

    public void setOrigPurpose(String purpose)
    {
        origPurpose=purpose;
    }
    public void setDestPurpose(String purpose){
        destPurpose = purpose;
    }
    public int getStopPurposeIndex()
    {
        return stopPurposeIndex;
    }

    public int getMode()
    {
        return mode;
    }
    public int getSet()
    {
        return set;
    }

    public int getBoardTap()
    {
        return boardTap;
    }

    public int getAlightTap()
    {
        return alightTap;
    }

    public int getStopPeriod()
    {
        return stopPeriod;
    }

    public Tour getTour()
    {
        return parentTour;
    }

    public boolean isInboundStop()
    {
        return inbound;
    }

    public int getStopId()
    {
        return id;
    }

    public int getEscorteePnumOrig() {
		return escorteePnumOrig;
	}

	public void setEscorteePnumOrig(int escorteePnum) {
		this.escorteePnumOrig = escorteePnum;
	}

	public byte getEscortStopTypeOrig() {
		return escortStopTypeOrig;
	}

	public void setEscortStopTypeOrig(byte stopType) {
		this.escortStopTypeOrig = stopType;
	}

	public int getEscorteePnumDest() {
		return escorteePnumDest;
	}

	public void setEscorteePnumDest(int escorteePnum) {
		this.escorteePnumDest = escorteePnum;
	}

	public byte getEscortStopTypeDest() {
		return escortStopTypeDest;
	}
	
    public void setEscortStopTypeDest(byte stopType) {
		this.escortStopTypeDest = stopType;
	}

    public float getModeLogsum() {
		return modeLogsum;
	}

	public void setModeLogsum(float modeLogsum) {
		this.modeLogsum = modeLogsum;
	}

	public void logStopObject(Logger logger, int totalChars)
    {

        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        Household.logHelper(logger, "stopId: ", id, totalChars);
        Household.logHelper(logger, "origPurpose: ", origPurpose, totalChars);
        Household.logHelper(logger, "destPurpose: ", destPurpose, totalChars);
        Household.logHelper(logger, "orig: ", orig, totalChars);
        Household.logHelper(logger, "dest: ", dest, totalChars);
        Household.logHelper(logger, "mode: ", mode, totalChars);
        Household.logHelper(logger, "boardTap: ", boardTap, totalChars);
        Household.logHelper(logger, "alightTap: ", alightTap, totalChars);
        Household.logHelper(logger, "TapSet: ", set, totalChars);
        Household.logHelper(logger, "direction: ", inbound ? "inbound" : "outbound", totalChars);
        Household.logHelper( logger, "stopPeriod: ", stopPeriod, totalChars );
        Household.logHelper( logger, "orig escort stop type: ",escortStopTypeOrig, totalChars);
        Household.logHelper( logger, "orig escortee pnum: ",escorteePnumOrig, totalChars);
        Household.logHelper( logger, "dest escort stop type: ",escortStopTypeDest, totalChars);
        Household.logHelper( logger, "dest escortee pnum: ",escorteePnumDest, totalChars);
        logger.info(separater);
        logger.info("");
        logger.info("");

    }

}
