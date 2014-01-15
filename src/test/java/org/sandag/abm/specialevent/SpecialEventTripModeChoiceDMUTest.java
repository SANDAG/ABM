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

    @Test
    public void testGetValueForIndexTAZ()
    {
        Logger logger = new FakeLogger();
        SandagModelStructure structure = new SandagModelStructure();
        SpecialEventTripModeChoiceDMU dmu = new SpecialEventTripModeChoiceDMU(structure, logger);
        // dmu.setTransitSkim(accEgr, lbPrem, skimIndex, value);
        int offset = 100;
        for (int x = 0; x < 50; x++)
        {
            // System.out.println(x);

            int[] p1Values = {McLogsumsCalculator.WTW};
            int p1 = p1Values[x / 100];
            int[] p2Values = {McLogsumsCalculator.LB, McLogsumsCalculator.EB,
                    McLogsumsCalculator.BRT, McLogsumsCalculator.LR, McLogsumsCalculator.CR};
            int[] p2Sizes = {8, 9, 10, 11, 12};
            int afterP1Index = x % 100;
            int p2Index = -1;
            int p3Finder = 0;
            int sum = 0;
            int p3 = -1;
            int[] pvt = {McLogsumsCalculator.LB_IVT, McLogsumsCalculator.EB_IVT,
                    McLogsumsCalculator.BRT_IVT, McLogsumsCalculator.LR_IVT,
                    McLogsumsCalculator.CR_IVT};

            int[] p3Values = {McLogsumsCalculator.FWAIT, McLogsumsCalculator.XWAIT,
                    McLogsumsCalculator.ACC, McLogsumsCalculator.EGR, McLogsumsCalculator.AUX,
                    McLogsumsCalculator.FARE, McLogsumsCalculator.XFERS};

            for (int y = 0; y < 5; y++)
            {
                if (p3 == -1)
                {
                    p3Finder += 1;
                    if (afterP1Index - sum < p3Finder)
                    {
                        p3 = pvt[(int) ((afterP1Index - sum))];
                    } else
                    {
                        int i = afterP1Index - sum - p3Finder;
                        if (i < p3Values.length)
                        {
                            p3 = p3Values[i];
                        }
                    }
                }

                sum += p2Sizes[y];
                if (afterP1Index < sum && p2Index < 0)
                {
                    p2Index = y;
                }
            }
            int p2 = p2Values[p2Index];

            dmu.setTransitSkim(p1, p2, p3, ((double) (x + 1)) / 100);
            TestTazParams(p1, p2, p3, x, offset, dmu);
        }
    }

    @Test
    public void testGetValueForIndexTAZComplex()
    {
        Logger logger = new FakeLogger();
        SandagModelStructure structure = new SandagModelStructure();
        SpecialEventTripModeChoiceDMU dmu = new SpecialEventTripModeChoiceDMU(structure, logger);
        int offset = 150;
        for (int d = 0; d < 2; d++)
        {
            if (d == 0)
            {
                dmu.outboundHalfTourDirection = 1;
            } else
            {
                dmu.outboundHalfTourDirection = 0;
            }
            for (int x = 0; x < 55; x++)
            {
                System.out.println(x);

                int[] p1Values = {McLogsumsCalculator.DTW, McLogsumsCalculator.WTD};
                int p1 = p1Values[d];
                int[] p2Values = {McLogsumsCalculator.LB, McLogsumsCalculator.EB,
                        McLogsumsCalculator.BRT, McLogsumsCalculator.LR, McLogsumsCalculator.CR};
                int[] p2Sizes = {9, 10, 11, 12, 13};
                int afterP1Index = x % 100;
                int p2Index = -1;
                int p3Finder = 0;
                int sum = 0;
                int p3 = -1;
                int[] pvt = {McLogsumsCalculator.LB_IVT, McLogsumsCalculator.EB_IVT,
                        McLogsumsCalculator.BRT_IVT, McLogsumsCalculator.LR_IVT,
                        McLogsumsCalculator.CR_IVT};

                int[] p3Values = {McLogsumsCalculator.FWAIT, McLogsumsCalculator.XWAIT,
                        McLogsumsCalculator.ACC, McLogsumsCalculator.EGR, -2,
                        McLogsumsCalculator.AUX, McLogsumsCalculator.FARE,
                        McLogsumsCalculator.XFERS};

                for (int y = 0; y < 5; y++)
                {
                    if (p3 == -1)
                    {
                        p3Finder += 1;
                        if (afterP1Index - sum < p3Finder)
                        {
                            p3 = pvt[(int) ((afterP1Index - sum))];
                        } else
                        {
                            int i = afterP1Index - sum - p3Finder;
                            if (i < p3Values.length)
                            {
                                p3 = p3Values[i];
                            }
                        }
                    }

                    sum += p2Sizes[y];
                    if (afterP1Index < sum && p2Index < 0)
                    {
                        p2Index = y;
                    }
                }
                int p2 = p2Values[p2Index];
                boolean dontChangeP3 = false;
                if (p3 == -2)
                {
                    if (d == 0)
                    {
                        p3 = McLogsumsCalculator.ACC;
                    } else if (d == 1)
                    {
                        p3 = McLogsumsCalculator.EGR;
                    }
                    dontChangeP3 = true;
                }

                // System.out.println("" + p1 + ", " + p2 + ", " + p3);
                dmu.setTransitSkim(p1, p2, p3, ((double) (x + 1)) / 100);

                double expected = 0;
                if (d == 0 && p3 == McLogsumsCalculator.ACC && !dontChangeP3)
                {
                    TestTazParamsExpected(x, offset, dmu, 0);
                } else if (d == 1 && p3 == McLogsumsCalculator.EGR && !dontChangeP3)
                {
                    TestTazParamsExpected(x, offset, dmu, 0);
                }

                else
                {
                    TestTazParams(p1, p2, p3, x, offset, dmu);
                }
            }
        }
    }

    private void TestTazParams(int p1, int p2, int p3, int i, int offset,
            SpecialEventTripModeChoiceDMU dmu)
    {
        double value = dmu.getValueForIndex(i + offset, 0);
        double expected = ((double) (i + 1)) / 100;
        Assert.assertEquals(expected, value);

    }

    private void TestTazParamsExpected(int i, int offset, SpecialEventTripModeChoiceDMU dmu,
            double expected)
    {
        double value = dmu.getValueForIndex(i + offset, 0);
        Assert.assertEquals(expected, value);

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
