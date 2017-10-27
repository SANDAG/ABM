package org.sandag.abm.visitor;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.application.FakeModelStructure;
import org.sandag.abm.crossborder.CrossBorderModelStructure;
import org.sandag.abm.crossborder.CrossBorderTripModeChoiceDMU;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class VisitorTourModeChoiceDMUTest
{

    @Test
    public void testVisitorTourModeChoiceDMU()
    {
        VisitorModelStructure structure = new VisitorModelStructure();
        VisitorTourModeChoiceDMU dmu = new VisitorTourModeChoiceDMU(structure, CreateFakeLogger());
    }

    @Test
    public void testGetValueForIndexTour()
    {
        VisitorModelStructure structure = new VisitorModelStructure();
        VisitorTourModeChoiceDMU dmu = new VisitorTourModeChoiceDMU(structure, CreateFakeLogger());

        double value;

        int tourDepart = 3;
        dmu.setTourDepartPeriod(tourDepart);
        value = dmu.getValueForIndex(0, 0);
        Assert.assertEquals(tourDepart, (int) value);

        int tourArrive = 4;
        dmu.setTourArrivePeriod(tourArrive);
        value = dmu.getValueForIndex(1, 0);
        Assert.assertEquals(tourArrive, (int) value);
    }

    @Test
    public void testGetValueForIndexSelf()
    {
        VisitorModelStructure structure = new VisitorModelStructure();
        VisitorTourModeChoiceDMU dmu = new VisitorTourModeChoiceDMU(structure, CreateFakeLogger());
        Household household = new Household(CreateFakeModelStructure());
        int tourID = 1;
        int primaryIndex = 1;
        int persNum = 1;
        Person person = new Person(household, persNum, CreateFakeModelStructure());
        Tour tour = new Tour(person, tourID, primaryIndex);
        double value;

        float terminalTime = 5.5f;
        dmu.setPTazTerminalTime(terminalTime);
        value = dmu.getValueForIndex(14, 0);
        Assert.assertEquals(terminalTime, (float) value);

        terminalTime = 4.5f;
        dmu.setATazTerminalTime(terminalTime);
        value = dmu.getValueForIndex(15, 0);
        Assert.assertEquals(terminalTime, (float) value);

        double originDuDen = 6.5;
        dmu.setOrigDuDen(originDuDen);
        value = dmu.getValueForIndex(16, 0);
        Assert.assertEquals(originDuDen, value);

        double originEmpDen = 7.5;
        dmu.setOrigEmpDen(originEmpDen);
        value = dmu.getValueForIndex(17, 0);
        Assert.assertEquals(originEmpDen, value);

        double oTotInt = 8.5;
        dmu.setOrigTotInt(oTotInt);
        value = dmu.getValueForIndex(18, 0);
        Assert.assertEquals(oTotInt, value);

        double dduDen = 8.5;
        dmu.setDestDuDen(dduDen);
        value = dmu.getValueForIndex(19, 0);
        Assert.assertEquals(dduDen, value);

        double dempDen = 9.5;
        dmu.setDestEmpDen(dempDen);
        value = dmu.getValueForIndex(20, 0);
        Assert.assertEquals(dempDen, value);

        double dToInt = 10.5;
        dmu.setDestTotInt(dToInt);
        value = dmu.getValueForIndex(21, 0);
        Assert.assertEquals(dToInt, value);

        double lsWgtAvgCostM = 11.5;
        dmu.setLsWgtAvgCostM(lsWgtAvgCostM);
        value = dmu.getValueForIndex(23, 0);
        Assert.assertEquals(lsWgtAvgCostM, value);

        double lsWgtAvgCostD = 12.5;
        dmu.setLsWgtAvgCostD(lsWgtAvgCostD);
        value = dmu.getValueForIndex(24, 0);
        Assert.assertEquals(lsWgtAvgCostD, value);

        double lsWgtAvgCostH = 13.5;
        dmu.setLsWgtAvgCostH(lsWgtAvgCostH);
        value = dmu.getValueForIndex(25, 0);
        Assert.assertEquals(lsWgtAvgCostH, value);

        double nmWalkTimeOut = .89;
        dmu.setNmWalkTimeOut(nmWalkTimeOut);
        value = dmu.getValueForIndex(90, 0);
        Assert.assertEquals(nmWalkTimeOut, value);

        double nmWalkTimeIn = .53;
        dmu.setNmWalkTimeIn(nmWalkTimeIn);
        value = dmu.getValueForIndex(91, 0);
        Assert.assertEquals(nmWalkTimeIn, value);

        double nmBikeTimeOut = .29;
        dmu.setNmBikeTimeOut(nmBikeTimeOut);
        value = dmu.getValueForIndex(92, 0);
        Assert.assertEquals(nmBikeTimeOut, value);

        double nmBikeTimeIn = .34;
        dmu.setNmBikeTimeIn(nmBikeTimeIn);
        value = dmu.getValueForIndex(93, 0);
        Assert.assertEquals(nmBikeTimeIn, value);

    }

   
    @Test(expected = RuntimeException.class)
    public void testBadIndex()
    {
        Logger logger = new FakeLogger();
        CrossBorderModelStructure structure = new CrossBorderModelStructure();
        CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(structure, logger);
        dmu.getValueForIndex(21, 0);
    }

    private ModelStructure CreateFakeModelStructure()
    {
        return new FakeModelStructure();
    }

    private Logger CreateFakeLogger()
    {
        return new FakeLogger();
    }

}
