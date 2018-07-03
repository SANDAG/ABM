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
    double                              OrigDestDistance;
    double                              tapToMgraWalkTime;
    double                              mgraToTapWalkTime;
    double                              carToStationWalkTime;
    double                              escalatorTime;
    int                                 accessMode;
    int 								period;
    int 								set;
    
    //default values for generic application
    int                                 applicationType = 0;
    int                                 personType = 1;
    float                               ivtCoeff;
    float                               costCoeff;


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
    
    public double getOrigDestDistance() {
		return OrigDestDistance;
	}

	public void setOrigDestDistance(double origDestDistance) {
		OrigDestDistance = origDestDistance;
	}

	public void setTOD(int period) {
    	this.period = period;
    }
    
    public int getTOD() {
    	return period;
    }
    
    public void setSet(int set) {
    	this.set = set;
    }
    
    public int getSet() {
    	return set;
    }
    
    
    public void setApplicationType(int applicationType) {
    	this.applicationType = applicationType;
    }
    
    public int getApplicationType() {
    	return applicationType;
    }
    
    public void setPersonType(int personType) {
    	this.personType = personType;
    }
    
    public int getPersonType() {
    	return personType;
    }
    
    public void setIvtCoeff(float ivtCoeff) {
    	this.ivtCoeff = ivtCoeff;
    }
    
    public void setCostCoeff(float costCoeff) {
    	this.costCoeff = costCoeff;
    }
    
    public float getIvtCoeff() {
    	return ivtCoeff;
    }

    public float getCostCoeff() {
    	return costCoeff;
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
        localLogger.info(String.format("Period:                   %9s", period));
        localLogger.info(String.format("Set:                      %9s", set));
        localLogger.info(String.format("applicationType:          %9s", applicationType));
        localLogger.info(String.format("personType:               %9s", personType));
        localLogger.info(String.format("ivtCoeff                  %9.4f", ivtCoeff));
        localLogger.info(String.format("costCoeff                 %9.4f", costCoeff));
        localLogger.info(String.format("origDestDistance          %9.4f, origDestDistance"));


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
        methodIndexMap.put("getTOD", 9);
        methodIndexMap.put("getSet", 10);
        
        methodIndexMap.put("getApplicationType", 12);
        methodIndexMap.put("getTourCategoryIsJoint", 13);
        methodIndexMap.put("getPersonType", 14);
        methodIndexMap.put("getIvtCoeff", 15);
        methodIndexMap.put("getCostCoeff", 16);
        methodIndexMap.put("getOrigDestDistance",17);

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
            case 9:
                return getTOD();
            case 10:
                return getSet();
                
            case 12:
                return getApplicationType();
            case 14:
                return getPersonType();
            case 15:
                return getIvtCoeff();
            case 16:
            	return getCostCoeff();
            case 17:
            	return getOrigDestDistance();


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
