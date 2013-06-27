package org.sandag.abm.modechoice;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.Modes.AccessMode;
import com.pb.common.calculator.VariableTable;

/**
 * This class is used for ...
 * 
 * @author Joel Freedman
 * @version Mar 20, 2009
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public class TransitDriveAccessDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger = Logger.getLogger(TransitDriveAccessDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    double                              driveTimeToTap;
    double                              driveDistToTap;
    double                              driveDistFromTap;
    double                              driveTimeFromTap;
    double                              tapToMgraWalkTime;
    double                              mgraToTapWalkTime;
    double                              carToStationWalkTime;
    double                              escalatorTime;
    int                                accessMode;

    public TransitDriveAccessDMU()
    {
        setupMethodIndexMap();
    }

    /**
     * Get the walk time from the alighting TAP to the destination MGRA.
     * 
     * @return The walk time from the alighting TAP to the destination MGRA.
     */
    public double getTapMgraWalkTime()
    {
        return tapToMgraWalkTime;
    }

    /**
     * Set the walk time from the alighting TAP to the destination MGRA.
     * 
     * @param walkTime The walk time from the alighting TAP to the destination MGRA.
     */
    public void setTapMgraWalkTime(double walkTime)
    {
        tapToMgraWalkTime = walkTime;
    }

    /**
     * Get the walk time to the boarding TAP from the origin MGRA.
     * 
     * @return The walk time from the origin MGRA to the boarding TAP.
     */
    public double getMgraTapWalkTime()
    {
        return mgraToTapWalkTime;
    }

    /**
     * Set the walk time to the boarding TAP from the origin MGRA
     * 
     * @param walkTime The walk time to the boarding TAP from the origin MGRA.
     */
    public void setMgraTapWalkTime(double walkTime)
    {
        mgraToTapWalkTime = walkTime;
    }

    /**
     * Get the walk time from the lot to the station.
     * 
     * @return The time in minutes.
     */
    public double getCarToStationWalkTime()
    {
        return carToStationWalkTime;
    }

    /**
     * Set the walk time from the lot to the station.
     * 
     * @param carToStationWalkTime The time in minutes.
     */
    public void setCarToStationWalkTime(double carToStationWalkTime)
    {
        this.carToStationWalkTime = carToStationWalkTime;
    }

    /**
     * Get the time to get to the platform.
     * 
     * @return The time in minutes.
     */
    public double getEscalatorTime()
    {
        return escalatorTime;
    }

    /**
     * Set the time to get to the platform.
     * 
     * @param escalatorTime The time in minutes.
     */
    public void setEscalatorTime(double escalatorTime)
    {
        this.escalatorTime = escalatorTime;
    }

    /**
     * Get the access mode for this DMU.
     * 
     * @return The access mode.
     */
    public int getAccessMode()
    {
        return accessMode;
    }

    /**
     * Set the access mode for this DMU.
     * 
     * @param accessMode The access mode.
     */
    public void setAccessMode(int accessMode)
    {
        this.accessMode = accessMode;
    }

    /**
     * Get the drive time from the origin/production TDZ/TAZ to the TAP.
     * 
     * @return The drive time in minutes.
     */
    public double getDriveTimeToTap()
    {
        return driveTimeToTap;
    }

    /**
     * Set the drive time from the origin/production TDZ/TAZ to the TAP.
     * 
     * @param driveTimeToTap The drive time in minutes.
     */
    public void setDriveTimeToTap(double driveTimeToTap)
    {
        this.driveTimeToTap = driveTimeToTap;
    }

    /**
     * Get the drive distance from the origin/production TDZ/TAZ to the TAP.
     * 
     * @return The drive distance in miles.
     */
    public double getDriveDistToTap()
    {
        return driveDistToTap;
    }

    /**
     * Set the drive distance from the origin/production TDZ/TAZ to the TAP.
     * 
     * @param driveDistToTap The drive distance in miles.
     */
    public void setDriveDistToTap(double driveDistToTap)
    {
        this.driveDistToTap = driveDistToTap;
    }

    /**
     * Get the drive time from the TAP to the destination/attraction TDZ/TAZ.
     * 
     * @return The drive time in minutes.
     */
    public double getDriveTimeFromTap()
    {
        return driveTimeFromTap;
    }

    /**
     * Set the drive time from the TAP to the destination/attraction TDZ/TAZ.
     * 
     * @param driveTime The drive time in minutes.
     */
    public void setDriveTimeFromTap(double driveTime)
    {
        driveTimeFromTap = driveTime;
    }

    /**
     * Get the drive distance from the TAP to the destination/attraction TDZ/TAZ.
     * 
     * @return The drive distance in miles.
     */
    public double getDriveDistFromTap()
    {
        return driveDistFromTap;
    }

    /**
     * Set the drive distance from the TAP to the destination/attraction TDZ/TAZ.
     * 
     * @param driveDist The drive distance in miles.
     */
    public void setDriveDistFromTap(double driveDist)
    {
        driveDistFromTap = driveDist;
    }

    /**
     * Log the DMU values.
     * 
     * @param localLogger The logger to use.
     */
    public void logValues(Logger localLogger)
    {

        localLogger.info("");
        localLogger.info("Drive-Transit Auto Access DMU Values:");
        localLogger.info("");
        localLogger.info(String.format("Drive Time To Tap:     %9.4f", driveTimeToTap));
        localLogger.info(String.format("Drive Dist To Tap:     %9.4f", driveDistToTap));
        localLogger.info(String.format("Drive Time From Tap:     %9.4f", driveTimeFromTap));
        localLogger.info(String.format("Drive Dist From Tap:     %9.4f", driveDistFromTap));
        localLogger.info(String.format("TAP to MGRA walk time:    %9.4f", tapToMgraWalkTime));
        localLogger.info(String.format("MGRA to TAP walk time:    %9.4f", mgraToTapWalkTime));
        localLogger.info(String.format("Car to station walk time: %9.4f", carToStationWalkTime));
        localLogger.info(String.format("Escalator time:           %9.4f", escalatorTime));

        AccessMode[] accessModes = AccessMode.values();
        localLogger.info(String.format("Access Mode:              %5s", accessModes[accessMode]
                .toString()));
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getAccessMode", 0);
        methodIndexMap.put("getCarToStationWalkTime", 1);
        methodIndexMap.put("getDriveDistToTap", 2);
        methodIndexMap.put("getDriveTimeToTap", 3);
        methodIndexMap.put("getDriveDistFromTap", 4);
        methodIndexMap.put("getDriveTimeFromTap", 5);
        methodIndexMap.put("getEscalatorTime", 6);
        methodIndexMap.put("getTapMgraWalkTime", 7);
        methodIndexMap.put("getMgraTapWalkTime", 8);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getAccessMode();
            case 1:
                return getCarToStationWalkTime();
            case 2:
                return getDriveDistToTap();
            case 3:
                return getDriveTimeToTap();
            case 4:
                return getDriveDistFromTap();
            case 5:
                return getDriveTimeFromTap();
            case 6:
                return getEscalatorTime();
            case 7:
                return getTapMgraWalkTime();
            case 8:
                return getMgraTapWalkTime();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
