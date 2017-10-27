package org.sandag.abm.airport;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.ctramp.McLogsumsCalculator;

public class AirportModelDMUTest
{

    public double error = .0001;

    @Test
    public void testAirportModelDMU()
    {
        Logger logger = new FakeLogger();
        AirportModelDMU dmu = new AirportModelDMU(logger);
    }

    @Test
    public void testGetValueForIndexBasic()
    {
        Logger logger = new FakeLogger();
        AirportModelDMU dmu = new AirportModelDMU(logger);
        AirportParty party = new AirportParty(0);
        dmu.setAirportParty(party);

        byte direction = 1;
        party.setDirection(direction);
        double value = dmu.getValueForIndex(0, 0);
        Assert.assertEquals(direction, (int) value);

        byte purpose = 2;
        party.setPurpose(purpose);
        value = dmu.getValueForIndex(1, 0);
        Assert.assertEquals(purpose, (int) value);

        byte size = 3;
        party.setSize(size);
        value = dmu.getValueForIndex(2, 0);
        Assert.assertEquals(size, (int) value);

        byte income = 4;
        party.setIncome(income);
        value = dmu.getValueForIndex(3, 0);
        Assert.assertEquals(income, (int) value);

        byte departTime = 5;
        party.setDepartTime(departTime);
        value = dmu.getValueForIndex(4, 0);
        Assert.assertEquals(departTime, (int) value);

        byte nights = 6;
        party.setNights(nights);
        value = dmu.getValueForIndex(5, 0);
        Assert.assertEquals(nights, (int) value);

        int originMGRA = 6;
        party.setOriginMGRA(originMGRA);
        value = dmu.getValueForIndex(6, 0);
        Assert.assertEquals(originMGRA, (int) value);

        double tazAlt = 16;
        double[][] sizeTerms = new double[20][20];
        sizeTerms[16][0] = tazAlt;
        dmu.setSizeTerms(sizeTerms);
        value = dmu.getValueForIndex(7, 0);
        Assert.assertEquals(tazAlt, value, error);

        int[] zips = new int[1];
        int zip = 6;
        zips[0] = zip;
        dmu.setZips(zips);
        value = dmu.getValueForIndex(8, 0);
        Assert.assertEquals(zip, (int) value);

        double driveAloneLogsum = 17.1;
        dmu.setDriveAloneLogsum(driveAloneLogsum);
        value = dmu.getValueForIndex(90, 0);
        Assert.assertEquals(driveAloneLogsum, value);

        double shared2Logsum = 17.2;
        dmu.setShared2Logsum(shared2Logsum);
        value = dmu.getValueForIndex(91, 0);
        Assert.assertEquals(shared2Logsum, value);

        double shared3Logsum = 17.3;
        dmu.setShared3Logsum(shared3Logsum);
        value = dmu.getValueForIndex(92, 0);
        Assert.assertEquals(shared3Logsum, value);

        double transitLogsum = 17.4;
        dmu.setTransitLogsum(transitLogsum);
        value = dmu.getValueForIndex(93, 0);
        Assert.assertEquals(transitLogsum, value);
    }

    

    @Test
    public void testGetValueForIndexTAZComplex()
    {
        Logger logger = new FakeLogger();
        AirportModelDMU dmu = new AirportModelDMU(logger);
        AirportParty party = new AirportParty(0);
        dmu.setAirportParty(party);
        // dmu.setTransitSkim(accEgr, lbPrem, skimIndex, value);
        int offset = 150;
        for (int d = 0; d < 2; d++)
        {
            if (d == 0)
            {
                party.setDirection(AirportModelStructure.DEPARTURE);
            } else
            {
                party.setDirection(AirportModelStructure.ARRIVAL);
            }
        }
    }

  
    @Test(expected = RuntimeException.class)
    public void testBadIndex()
    {
        Logger logger = new FakeLogger();
        AirportModelDMU dmu = new AirportModelDMU(logger);
        dmu.getValueForIndex(13, 0);
    }

}
