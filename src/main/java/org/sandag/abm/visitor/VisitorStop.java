package org.sandag.abm.visitor;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;

public class VisitorStop
        implements Serializable
{

    private int     id;
    private int     mode;
    private int     period;
    private boolean inbound;
    private int     mgra;
    private byte    purpose;

    VisitorTour     parentTour;

    public VisitorStop(VisitorTour parentTour, int id, boolean inbound)
    {
        this.parentTour = parentTour;
        this.id = id;
        this.inbound = inbound;
    }

    /**
     * @return the mgra
     */
    public int getMgra()
    {
        return mgra;
    }

    /**
     * @param mgra
     *            the mgra to set
     */
    public void setMgra(int mgra)
    {
        this.mgra = mgra;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public void setPeriod(int period)
    {
        this.period = period;
    }

    /**
     * @return the id
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id)
    {
        this.id = id;
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
     * @return the parentTour
     */
    public VisitorTour getParentTour()
    {
        return parentTour;
    }

    /**
     * @param parentTour
     *            the parentTour to set
     */
    public void setParentTour(VisitorTour parentTour)
    {
        this.parentTour = parentTour;
    }

    /**
     * @param purpose
     *            the purpose to set
     */
    public void setPurpose(byte stopPurposeIndex)
    {
        this.purpose = stopPurposeIndex;
    }

    public byte getPurpose()
    {
        return purpose;
    }

    public int getMode()
    {
        return mode;
    }

    public int getStopPeriod()
    {
        return period;
    }

    public VisitorTour getTour()
    {
        return parentTour;
    }

    public int getStopId()
    {
        return id;
    }

    public void logStopObject(Logger logger, int totalChars)
    {

        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        String purposeString = VisitorModelStructure.VISITOR_PURPOSES[purpose];
        logHelper(logger, "stopId: ", id, totalChars);
        logHelper(logger, "mgra: ", mgra, totalChars);
        logHelper(logger, "mode: ", mode, totalChars);
        logHelper(logger, "purpose: ", purposeString, totalChars);
        logHelper(logger, "direction: ", inbound ? "inbound" : "outbound", totalChars);
        logHelper(logger, inbound ? "outbound departPeriod: " : "inbound arrivePeriod: ", period,
                totalChars);
        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public static void logHelper(Logger logger, String label, int value, int totalChars)
    {
        int labelChars = label.length() + 2;
        int remainingChars = totalChars - labelChars - 4;
        String formatString = String.format("     %%%ds %%%dd", label.length(), remainingChars);
        String logString = String.format(formatString, label, value);
        logger.info(logString);
    }

    public static void logHelper(Logger logger, String label, String value, int totalChars)
    {
        int labelChars = label.length() + 2;
        int remainingChars = totalChars - labelChars - 4;
        String formatString = String.format("     %%%ds %%%ds", label.length(), remainingChars);
        String logString = String.format(formatString, label, value);
        logger.info(logString);
    }
}
