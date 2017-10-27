package org.sandag.abm.internalexternal;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.crossborder.CrossBorderModelStructure;
import org.sandag.abm.crossborder.CrossBorderTripModeChoiceDMU;
import org.sandag.abm.ctramp.McLogsumsCalculator;

public class InternalExternalTripModeChoiceDMUTest
{

    public double error = .0001;

    @Test
    public void testCrossBorderTripModeChoiceDMU()
    {
        Logger logger = new FakeLogger();
        InternalExternalModelStructure structure = new InternalExternalModelStructure();
        InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(structure,
                logger);
    }

    @Test
    public void testGetValueForIndexBasic()
    {
        Logger logger = new FakeLogger();
        InternalExternalModelStructure structure = new InternalExternalModelStructure();
        InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(structure,
                logger);

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

        int income = 9;
        dmu.setIncome(income);
        value = dmu.getValueForIndex(9, 0);
        Assert.assertEquals(income, (int) value);

        int female = 10;
        dmu.setFemale(female);
        value = dmu.getValueForIndex(10, 0);
        Assert.assertEquals(female, (int) value);

        int autos = 11;
        dmu.setAutos(autos);
        value = dmu.getValueForIndex(11, 0);
        Assert.assertEquals(autos, (int) value);

        int hhSize = 12;
        dmu.setHhSize(hhSize);
        value = dmu.getValueForIndex(12, 0);
        Assert.assertEquals(hhSize, (int) value);

        int age = 13;
        dmu.setAge(age);
        value = dmu.getValueForIndex(13, 0);
        Assert.assertEquals(age, (int) value);

        int tripOrigIsTourDest = 14;
        dmu.setTripOrigIsTourDest(tripOrigIsTourDest);
        value = dmu.getValueForIndex(23, 0);
        Assert.assertEquals(tripOrigIsTourDest, (int) value);

        int tripDestIsTourDest = 15;
        dmu.setTripDestIsTourDest(tripDestIsTourDest);
        value = dmu.getValueForIndex(24, 0);
        Assert.assertEquals(tripDestIsTourDest, (int) value);

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
