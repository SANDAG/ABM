package org.sandag.cvm.activityTravel;

import org.apache.log4j.Logger;

public class LoggingStopAlternative extends StopAlternative {
       

    static Logger logger = Logger.getLogger(LoggingStopAlternative.class);
    static double[] co = new double[10];

    static boolean loggedParts = false;

//    @Override
    public double getUtility() {
        if (!loggedParts) {
            logger.info("Parts of utility function are travel,destination,returnHomeTravel,returnHomeDisutility,travelDisutility,returnHomeTime,travelTime,angle,zoneType,sizeTerm");
            loggedParts=true;
        }
        
        co[0] = c.travelUtilityFunction.calcForIndex(c.myTour.getCurrentLocation(),location);
        co[1] = c.destinationUtilityFunction.calcForIndex(location,1);
        // TODO should be logsum of trip mode
        co[2] = c.returnHomeUtilityFunction.calcForIndex(location,c.myTour.getOriginZone());
        if (c.disutilityToOriginCoefficient!=0) {
            co[3] = c.disutilityToOriginCoefficient*c.myTour.getTravelDisutilityTracker().getTravelAttribute(location,c.myTour.getOriginZone(),c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
        } else co[3]=0;
        if (c.disutilityToNextStopCoefficient!=0) {
            co[4] = c.disutilityToNextStopCoefficient*c.myTour.getTravelDisutilityTracker().getTravelAttribute(c.myTour.getCurrentLocation(),location,c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
        } else co[4] = 0;
        if (c.timeToOriginCoefficient!=0) {
            double timeToOriginUtility = c.timeToOriginCoefficient*c.myTour.getElapsedTravelTimeCalculator().getTravelAttribute(location,c.myTour.getOriginZone(),c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
            // Doug and Kevin Hack of Jan 5 2004
//            if (myTour.getTotalElapsedTime()>240.0) timeToOriginUtility*=3;
            co[5] = timeToOriginUtility;
        } else co[5]=0;
        if (c.timeToNextStopCoefficient!=0) {
            double timeToNextStopUtility = c.timeToNextStopCoefficient*c.myTour.getElapsedTravelTimeCalculator().getTravelAttribute(c.myTour.getCurrentLocation(),location,c.myTour.getCurrentTimeHrs(),c.myTour.getMyVehicleTourType().vehicleType);
            // Doug and Kevin Hack of Jan 5 2004
//            if (myTour.getTotalElapsedTime()>240.0) timeToNextStopUtility*=3;
            co[6] = timeToNextStopUtility;
        } else co[6]=0;
        
        if (c.xMatrix !=null && c.yMatrix != null) {

            double xOrig = c.xMatrix.getValueAt(c.myTour.getOriginZone(),1);
            double yOrig = c.yMatrix.getValueAt(c.myTour.getOriginZone(),1);
            double xNow = c.xMatrix.getValueAt(c.myTour.getCurrentLocation(),1);
            double yNow = c.yMatrix.getValueAt(c.myTour.getCurrentLocation(),1);
            double xMaybe = c.xMatrix.getValueAt(location,1);
            double yMaybe = c.yMatrix.getValueAt(location,1);
            double angle1 = Math.atan2(yNow-yOrig,xNow-xOrig);
            double angle2 = Math.atan2(yMaybe-yNow,xMaybe-xNow);
            double angle = (angle2-angle1)+Math.PI;
            if (angle > Math.PI*2) angle -= Math.PI*2;
            if (angle <0) angle += Math.PI*2;
            if (angle > Math.PI) angle =2*Math.PI-angle;
            co[7]= c.angleCoefficient*angle*180/Math.PI;
        } else co[7]=0;
        co[8]= c.zoneTypeUtilityFunction.calcForIndex(c.myTour.getCurrentLocation(),location);
        if (c.sizeTermCoefficient !=0) {
            double sizeTermValue = Math.log(c.sizeTerm.calcForIndex(location,1));
            co[9]= c.sizeTermCoefficient*sizeTermValue;
        } else co[9]=0;
        StringBuffer logStatement = new StringBuffer("OD "+c.getTour().getCurrentLocation()+","+location+" :");
        double uti=0;
        for (int index=0;index<co.length;index++) {
            logStatement.append(","+co[index]);
            uti+=co[index];
        }
        logger.info(logStatement.toString());
        
        return uti;
    }

    public LoggingStopAlternative(StopChoice choice, int stopLocation) {
        super(choice, stopLocation);
    }

}
