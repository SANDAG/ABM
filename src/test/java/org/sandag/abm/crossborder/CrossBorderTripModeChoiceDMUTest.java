package org.sandag.abm.crossborder;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.ctramp.McLogsumsCalculator;

public class CrossBorderTripModeChoiceDMUTest
{

    public double error = .0001;

    @Test
    public void testCrossBorderTripModeChoiceDMU()
    {
        Logger logger = new FakeLogger();
        CrossBorderModelStructure structure = new CrossBorderModelStructure();
        CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(structure, logger);
    }

    @Test
    public void testGetValueForIndexBasic()
    {
        Logger logger = new FakeLogger();
        CrossBorderModelStructure structure = new CrossBorderModelStructure();
        CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(structure, logger);

        int tourDepartPeriod = 1;
        dmu.setTourDepartPeriod(tourDepartPeriod);
        double value = dmu.getValueForIndex(0, 0);
        Assert.assertEquals(tourDepartPeriod, (int) value);

        int tourArrivePeriod = 2;
        dmu.setTourArrivePeriod(tourArrivePeriod);
        value = dmu.getValueForIndex(1, 0);
        Assert.assertEquals(tourArrivePeriod, (int) value);

        int tripPeriod = 3;
        dmu.setTripPeriod(tripPeriod);
        value = dmu.getValueForIndex(2, 0);
        Assert.assertEquals(tripPeriod, (int) value);

        int workTour = 4;
        dmu.setWorkTour(workTour);
        value = dmu.getValueForIndex(4, 0);
        Assert.assertEquals(workTour, (int) value);

        int outboundStops = 5;
        dmu.setOutboundStops(outboundStops);
        value = dmu.getValueForIndex(5, 0);
        Assert.assertEquals(outboundStops, (int) value);

        int returnStops = 6;
        dmu.setReturnStops(returnStops);
        value = dmu.getValueForIndex(6, 0);
        Assert.assertEquals(returnStops, (int) value);

        int firstTrip = 7;
        dmu.setFirstTrip(firstTrip);
        value = dmu.getValueForIndex(7, 0);
        Assert.assertEquals(firstTrip, (int) value);

        int lastTrip = 8;
        dmu.setLastTrip(lastTrip);
        value = dmu.getValueForIndex(8, 0);
        Assert.assertEquals(lastTrip, (int) value);

        int tourModeIsDA = 9;
        dmu.setTourModeIsDA(tourModeIsDA);
        value = dmu.getValueForIndex(9, 0);
        Assert.assertEquals(tourModeIsDA, (int) value);

        int tourModeIsS2 = 10;
        dmu.setTourModeIsS2(tourModeIsS2);
        value = dmu.getValueForIndex(10, 0);
        Assert.assertEquals(tourModeIsS2, (int) value);

        int tourModeIsS3 = 11;
        dmu.setTourModeIsS3(tourModeIsS3);
        value = dmu.getValueForIndex(11, 0);
        Assert.assertEquals(tourModeIsS3, (int) value);

        int tourModeIsWalk = 12;
        dmu.setTourModeIsWalk(tourModeIsWalk);
        value = dmu.getValueForIndex(12, 0);
        Assert.assertEquals(tourModeIsWalk, (int) value);

        int tourCrossingIsSentri = 13;
        dmu.setTourCrossingIsSentri(tourCrossingIsSentri);
        value = dmu.getValueForIndex(13, 0);
        Assert.assertEquals(tourCrossingIsSentri, (int) value);

        int hourlyParkingCostTourDest = 14;
        dmu.setHourlyParkingCostTourDest(hourlyParkingCostTourDest);
        value = dmu.getValueForIndex(14, 0);
        Assert.assertEquals(hourlyParkingCostTourDest, (int) value);

        int dailyParkingCostTourDest = 15;
        dmu.setDailyParkingCostTourDest(dailyParkingCostTourDest);
        value = dmu.getValueForIndex(15, 0);
        Assert.assertEquals(dailyParkingCostTourDest, (int) value);

        int monthlyParkingCostTourDest = 16;
        dmu.setMonthlyParkingCostTourDest(monthlyParkingCostTourDest);
        value = dmu.getValueForIndex(16, 0);
        Assert.assertEquals(monthlyParkingCostTourDest, (int) value);

        int tripOrigIsTourDest = 17;
        dmu.setTripOrigIsTourDest(tripOrigIsTourDest);
        value = dmu.getValueForIndex(17, 0);
        Assert.assertEquals(tripOrigIsTourDest, (int) value);

        int tripDestIsTourDest = 18;
        dmu.setTripDestIsTourDest(tripDestIsTourDest);
        value = dmu.getValueForIndex(18, 0);
        Assert.assertEquals(tripDestIsTourDest, (int) value);

        int hourlyParkingCostTripOrig = 19;
        dmu.setHourlyParkingCostTripOrig(hourlyParkingCostTripOrig);
        value = dmu.getValueForIndex(19, 0);
        Assert.assertEquals(hourlyParkingCostTripOrig, (int) value);

        int hourlyParkingCostTripDest = 20;
        dmu.setHourlyParkingCostTripDest(hourlyParkingCostTripDest);
        value = dmu.getValueForIndex(20, 0);
        Assert.assertEquals(hourlyParkingCostTripDest, (int) value);
        
        float workTimeFactor = 3.1f;
        dmu.setWorkTimeFactor(workTimeFactor);
        value = dmu.getValueForIndex(50, 0);
        Assert.assertEquals(workTimeFactor, (float) value);
       
        float nonWorkTimeFactor = 2.1f;
        dmu.setNonWorkTimeFactor(nonWorkTimeFactor);
        value = dmu.getValueForIndex(51, 0);
        Assert.assertEquals(nonWorkTimeFactor, (float) value);
              
        double nmWalkTime = 17.1;
        dmu.setNonMotorizedWalkTime(nmWalkTime);
        value = dmu.getValueForIndex(90, 0);
        Assert.assertEquals(nmWalkTime, value);

        double nmBikeTime = 17.2;
        dmu.setNonMotorizedBikeTime(nmBikeTime);
        value = dmu.getValueForIndex(91, 0);
        Assert.assertEquals(nmBikeTime, value);
    }

   
   
    @Test(expected = RuntimeException.class)
    public void testBadIndex()
    {
        Logger logger = new FakeLogger();
        CrossBorderModelStructure structure = new CrossBorderModelStructure();
        CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(structure, logger);
        dmu.getValueForIndex(21, 0);
    }

}
