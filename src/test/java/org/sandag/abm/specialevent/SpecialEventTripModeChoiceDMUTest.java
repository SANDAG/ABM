package org.sandag.abm.specialevent;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.McLogsumsCalculator;

public class SpecialEventTripModeChoiceDMUTest
{

    public double error = .0001;

    @Test
    public void testCrossBorderTripModeChoiceDMU()
    {
        Logger logger = new FakeLogger();
        SandagModelStructure structure = new SandagModelStructure();
        SpecialEventTripModeChoiceDMU dmu = new SpecialEventTripModeChoiceDMU(structure, logger);
    }

    @Test
    public void testGetValueForIndexBasic()
    {
        Logger logger = new FakeLogger();
        SandagModelStructure structure = new SandagModelStructure();
        SpecialEventTripModeChoiceDMU dmu = new SpecialEventTripModeChoiceDMU(structure, logger);

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

        int parkingCost = 5;
        dmu.setParkingCost(parkingCost);
        value = dmu.getValueForIndex(3, 0);
        Assert.assertEquals(parkingCost, (int) value);

        int parkingTime = 6;
        dmu.setParkingTime(parkingTime);
        value = dmu.getValueForIndex(4, 0);
        Assert.assertEquals(parkingTime, (int) value);

        int tripOrigIsTourDest = 7;
        dmu.setTripOrigIsTourDest(tripOrigIsTourDest);
        value = dmu.getValueForIndex(5, 0);
        Assert.assertEquals(tripOrigIsTourDest, (int) value);

        int tripDestIsTourDest = 8;
        dmu.setTripDestIsTourDest(tripDestIsTourDest);
        value = dmu.getValueForIndex(6, 0);
        Assert.assertEquals(tripDestIsTourDest, (int) value);

        int income = 9;
        dmu.setIncome(income);
        value = dmu.getValueForIndex(7, 0);
        Assert.assertEquals(income, (int) value);

        int partySize = 10;
        dmu.setPartySize(partySize);
        value = dmu.getValueForIndex(8, 0);
        Assert.assertEquals(partySize, (int) value);

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

}
