package org.sandag.abm.visitor;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.specialevent.SpecialEventTripModeChoiceDMU;

public class VisitorTripModeChoiceDMUTest
{

    @Test
    public void testVisitorTripModeChoiceDMU()
    {
        VisitorModelStructure structure = new VisitorModelStructure();
        VisitorTripModeChoiceDMU dmu = new VisitorTripModeChoiceDMU(structure, CreateFakeLogger());
    }

    @Test
    public void testGetValueForIndexBasic()
    {
        VisitorModelStructure structure = new VisitorModelStructure();
        VisitorTripModeChoiceDMU dmu = new VisitorTripModeChoiceDMU(structure, CreateFakeLogger());

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

        int segment = 5;
        dmu.setSegment(segment);
        value = dmu.getValueForIndex(3, 0);
        Assert.assertEquals(segment, (int) value);

        int tourPurpose = 6;
        dmu.setTourPurpose(tourPurpose);
        value = dmu.getValueForIndex(4, 0);
        Assert.assertEquals(tourPurpose, (int) value);

        int outboundStops = 7;
        dmu.setOutboundStops(outboundStops);
        value = dmu.getValueForIndex(5, 0);
        Assert.assertEquals(outboundStops, (int) value);

        int returnStops = 8;
        dmu.setReturnStops(returnStops);
        value = dmu.getValueForIndex(6, 0);
        Assert.assertEquals(returnStops, (int) value);

        int firstTrip = 9;
        dmu.setFirstTrip(firstTrip);
        value = dmu.getValueForIndex(7, 0);
        Assert.assertEquals(firstTrip, (int) value);

        int lastTrip = 10;
        dmu.setLastTrip(lastTrip);
        value = dmu.getValueForIndex(8, 0);
        Assert.assertEquals(lastTrip, (int) value);

        int tourModeIsDA = 9;
        dmu.setTourModeIsDA(tourModeIsDA);
        value = dmu.getValueForIndex(9, 0);
        Assert.assertEquals(tourModeIsDA, (int) value);

        int tourModeIsS2 = 11;
        dmu.setTourModeIsS2(tourModeIsS2);
        value = dmu.getValueForIndex(10, 0);
        Assert.assertEquals(tourModeIsS2, (int) value);

        int tourModeIsS3 = 12;
        dmu.setTourModeIsS3(tourModeIsS3);
        value = dmu.getValueForIndex(11, 0);
        Assert.assertEquals(tourModeIsS3, (int) value);

        int tourModeIsWalk = 13;
        dmu.setTourModeIsWalk(tourModeIsWalk);
        value = dmu.getValueForIndex(12, 0);
        Assert.assertEquals(tourModeIsWalk, (int) value);

        int tourModeIsBike = 14;
        dmu.setTourModeIsBike(tourModeIsBike);
        value = dmu.getValueForIndex(13, 0);
        Assert.assertEquals(tourModeIsBike, (int) value);

        int tourModeIsWalkTransit = 15;
        dmu.setTourModeIsWalkTransit(tourModeIsWalkTransit);
        value = dmu.getValueForIndex(14, 0);
        Assert.assertEquals(tourModeIsWalkTransit, (int) value);

        int tourModeIsPNRTransit = 16;
        dmu.setTourModeIsPNRTransit(tourModeIsPNRTransit);
        value = dmu.getValueForIndex(15, 0);
        Assert.assertEquals(tourModeIsPNRTransit, (int) value);

        int tourModeIsKNRTransit = 17;
        dmu.setTourModeIsKNRTransit(tourModeIsKNRTransit);
        value = dmu.getValueForIndex(16, 0);
        Assert.assertEquals(tourModeIsKNRTransit, (int) value);

        int tourModeIsTaxi = 18;
        dmu.setTourModeIsTaxi(tourModeIsTaxi);
        value = dmu.getValueForIndex(17, 0);
        Assert.assertEquals(tourModeIsTaxi, (int) value);

        int hourlyParkingCostTourDest = 21;
        dmu.setHourlyParkingCostTourDest(hourlyParkingCostTourDest);
        value = dmu.getValueForIndex(20, 0);
        Assert.assertEquals(hourlyParkingCostTourDest, (int) value);

        int dailyParkingCostTourDest = 22;
        dmu.setDailyParkingCostTourDest(dailyParkingCostTourDest);
        value = dmu.getValueForIndex(21, 0);
        Assert.assertEquals(dailyParkingCostTourDest, (int) value);

        int monthlyParkingCostTourDest = 23;
        dmu.setMonthlyParkingCostTourDest(monthlyParkingCostTourDest);
        value = dmu.getValueForIndex(22, 0);
        Assert.assertEquals(monthlyParkingCostTourDest, (int) value);

        int tripOrigIsTourDest = 24;
        dmu.setTripOrigIsTourDest(tripOrigIsTourDest);
        value = dmu.getValueForIndex(23, 0);
        Assert.assertEquals(tripOrigIsTourDest, (int) value);

        int tripDestIsTourDest = 25;
        dmu.setTripDestIsTourDest(tripDestIsTourDest);
        value = dmu.getValueForIndex(24, 0);
        Assert.assertEquals(tripDestIsTourDest, (int) value);

        int hourlyParkingCostTripOrig = 26;
        dmu.setHourlyParkingCostTripOrig(hourlyParkingCostTripOrig);
        value = dmu.getValueForIndex(25, 0);
        Assert.assertEquals(hourlyParkingCostTripOrig, (int) value);

        int hourlyParkingCostTripDest = 27;
        dmu.setHourlyParkingCostTripDest(hourlyParkingCostTripDest);
        value = dmu.getValueForIndex(26, 0);
        Assert.assertEquals(hourlyParkingCostTripDest, (int) value);

        int partySize = 31;
        dmu.setPartySize(partySize);
        value = dmu.getValueForIndex(30, 0);
        Assert.assertEquals(partySize, (int) value);

        int autoAvailable = 32;
        dmu.setAutoAvailable(autoAvailable);
        value = dmu.getValueForIndex(31, 0);
        Assert.assertEquals(autoAvailable, (int) value);

        int income = 33;
        dmu.setIncome(income);
        value = dmu.getValueForIndex(32, 0);
        Assert.assertEquals(income, (int) value);

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
        SandagModelStructure structure = new SandagModelStructure();
        SpecialEventTripModeChoiceDMU dmu = new SpecialEventTripModeChoiceDMU(structure, logger);
        dmu.getValueForIndex(21, 0);
    }

    private Logger CreateFakeLogger()
    {
        return new FakeLogger();
    }

}
