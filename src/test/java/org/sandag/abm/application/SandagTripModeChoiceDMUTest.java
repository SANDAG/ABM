package org.sandag.abm.application;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.crossborder.CrossBorderModelStructure;
import org.sandag.abm.crossborder.CrossBorderTripModeChoiceDMU;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.ParkingProvisionModel;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class SandagTripModeChoiceDMUTest {

	@Test
	public void testSandagTourModeChoiceDMU() {
		SandagTripModeChoiceDMU dmu = new SandagTripModeChoiceDMU(
				CreateFakeModelStructure(), CreateFakeLogger());
	}

	@Test
	public void testGetValueForIndexBasic() {
		Logger logger = new FakeLogger();
		ModelStructure structure = CreateFakeModelStructure();
		SandagTripModeChoiceDMU dmu = new SandagTripModeChoiceDMU(structure,
				logger);

		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		tour.setTourDestMgra(0);
		tour.setTourModeChoice(9);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);

		int[] mgraParkArea = new int[1];
		double[] lsWgtAvgCostM = new double[3];
		double[] lsWgtAvgCostD = new double[3];
		double[] lsWgtAvgCostH = new double[3];
		dmu.setParkingCostInfo(mgraParkArea, lsWgtAvgCostM, lsWgtAvgCostD,
				lsWgtAvgCostH);
		
		
		
		int autos = 1;
		dmu.setAutos(autos);
		double value = dmu.getValueForIndex(1, 0);
		Assert.assertEquals(autos, (int) value);

		int adults = 2;
		dmu.setAdults(adults);
		value = dmu.getValueForIndex(2, 0);
		Assert.assertEquals(adults, (int) value);

		int hhSize = 3;
		dmu.setHhSize(hhSize);
		value = dmu.getValueForIndex(3, 0);
		Assert.assertEquals(hhSize, (int) value);

		int female = 4;
		dmu.setPersonIsFemale(female);
		value = dmu.getValueForIndex(4, 0);
		Assert.assertEquals(female, (int) value);

		int income = 5;
		dmu.setIncomeInDollars(income);
		value = dmu.getValueForIndex(5, 0);
		Assert.assertEquals(income, (int) value);

		int departPeriod = 6;
		dmu.setDepartPeriod(departPeriod);
		value = dmu.getValueForIndex(6, 0);
		Assert.assertEquals(departPeriod, (int) value);

		int arrivePeriod = 7;
		dmu.setArrivePeriod(arrivePeriod);
		value = dmu.getValueForIndex(7, 0);
		Assert.assertEquals(arrivePeriod, (int) value);

		int period = 8;
		dmu.setTripPeriod(period);
		value = dmu.getValueForIndex(8, 0);
		Assert.assertEquals(period, (int) value);

		int jointTour = 9;
		dmu.setJointTour(jointTour);
		value = dmu.getValueForIndex(9, 0);
		Assert.assertEquals(jointTour, (int) value);

		int partySize = 10;
		dmu.setPartySize(partySize);
		value = dmu.getValueForIndex(10, 0);
		Assert.assertEquals(partySize, (int) value);

		int outboundStops = 11;
		dmu.setOutboundStops(outboundStops);
		value = dmu.getValueForIndex(11, 0);
		Assert.assertEquals(outboundStops, (int) value);

		int inboundStops = 12;
		dmu.setInboundStops(inboundStops);
		value = dmu.getValueForIndex(12, 0);
		Assert.assertEquals(inboundStops, (int) value);

		int firstTrip = 13;
		dmu.setFirstTrip(firstTrip);
		value = dmu.getValueForIndex(13, 0);
		Assert.assertEquals(firstTrip, (int) value);

		int lastTrip = 14;
		dmu.setLastTrip(lastTrip);
		value = dmu.getValueForIndex(14, 0);
		Assert.assertEquals(lastTrip, (int) value);

		dmu.setTourModeIsDA(1);
		tour.setTourModeChoice(1);
		value = dmu.getValueForIndex(15, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsS2(1);
		tour.setTourModeChoice(3);
		value = dmu.getValueForIndex(16, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsS3(1);
		tour.setTourModeChoice(5);
		value = dmu.getValueForIndex(17, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsWalk(1);
		tour.setTourModeChoice(7);
		value = dmu.getValueForIndex(18, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsBike(1);
		tour.setTourModeChoice(8);
		value = dmu.getValueForIndex(19, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsWTran(1);
		tour.setTourModeChoice(9);
		value = dmu.getValueForIndex(20, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsPnr(1);
		tour.setTourModeChoice(10);
		value = dmu.getValueForIndex(21, 0);
		Assert.assertEquals(1, (int) value);

		dmu.setTourModeIsKnr(1);
		tour.setTourModeChoice(11);
		value = dmu.getValueForIndex(22, 0);
		Assert.assertEquals(1, (int) value);

		int origDuDen = 23;
		dmu.setOrigDuDen(origDuDen);
		value = dmu.getValueForIndex(23, 0);
		Assert.assertEquals(origDuDen, (int) value);

		int origEmpDen = 24;
		dmu.setOrigEmpDen(origEmpDen);
		value = dmu.getValueForIndex(24, 0);
		Assert.assertEquals(origEmpDen, (int) value);

		int origTotInt = 25;
		dmu.setOrigTotInt(origTotInt);
		value = dmu.getValueForIndex(25, 0);
		Assert.assertEquals(origTotInt, (int) value);

		int destDuDen = 26;
		dmu.setDestDuDen(destDuDen);
		value = dmu.getValueForIndex(26, 0);
		Assert.assertEquals(destDuDen, (int) value);

		int destEmpDen = 27;
		dmu.setDestEmpDen(destEmpDen);
		value = dmu.getValueForIndex(27, 0);
		Assert.assertEquals(destEmpDen, (int) value);

		int destTotInt = 28;
		dmu.setDestTotInt(destTotInt);
		value = dmu.getValueForIndex(28, 0);
		Assert.assertEquals(destTotInt, (int) value);

		int pTazTime = 30;
		dmu.setPTazTerminalTime(pTazTime);
		value = dmu.getValueForIndex(30, 0);
		Assert.assertEquals(pTazTime, (int) value);

		int aTazTime = 31;
		dmu.setATazTerminalTime(aTazTime);
		value = dmu.getValueForIndex(31, 0);
		Assert.assertEquals(aTazTime, (int) value);

		int age = 32;
		dmu.setAge(age);
		value = dmu.getValueForIndex(32, 0);
		Assert.assertEquals(age, (int) value);

		int tourModeIsSchBus = 33;
		dmu.setTourModeIsSchBus(tourModeIsSchBus);
		value = dmu.getValueForIndex(33, 0);
		Assert.assertEquals(tourModeIsSchBus, (int) value);

		int escortTour = 34;
		dmu.setEscortTour(escortTour);
		value = dmu.getValueForIndex(34, 0);
		Assert.assertEquals(escortTour, (int) value);

		boolean autoModeRequired = true;
		dmu.setAutoModeRequiredForTripSegment(autoModeRequired);
		value = dmu.getValueForIndex(35, 0);
		Assert.assertEquals(autoModeRequired, ((int) value == 1));

		boolean walkModeRequired = true;
		dmu.setWalkModeAllowedForTripSegment(walkModeRequired);
		value = dmu.getValueForIndex(36, 0);
		Assert.assertEquals(walkModeRequired, ((int) value == 1));

		boolean segmentIsIk = true;
		dmu.setSegmentIsIk(segmentIsIk);
		value = dmu.getValueForIndex(37, 0);
		Assert.assertEquals(segmentIsIk, ((int) value == 1));

		int reimburseAmount = 38;
		dmu.setReimburseProportion(reimburseAmount);
		value = dmu.getValueForIndex(38, 0);
		Assert.assertEquals(reimburseAmount, (int) value);

		int monthlyParkingCost = 39;
		lsWgtAvgCostM[0] = monthlyParkingCost;
		value = dmu.getValueForIndex(39, 0);
		Assert.assertEquals(monthlyParkingCost, (int) value);

		int dailyParkingCost = 40;
		lsWgtAvgCostD[0] = dailyParkingCost;
		value = dmu.getValueForIndex(40, 0);
		Assert.assertEquals(dailyParkingCost, (int) value);

		int hourlyParkingCost = 41;
		lsWgtAvgCostH[0] = hourlyParkingCost;
		value = dmu.getValueForIndex(41, 0);
		Assert.assertEquals(hourlyParkingCost, (int) value);

		dmu.setDmuIndexValues(1, 1, 1, 2, false);

		dmu.setOriginMgra(1);
		dmu.setDestMgra(2);
		
		int originHourlyParkingCost = 42;
		lsWgtAvgCostH[1] = originHourlyParkingCost;
		value = dmu.getValueForIndex(42, 0);
		Assert.assertEquals(originHourlyParkingCost, (int) value);

		int destHourlyParkingCost = 43;
		lsWgtAvgCostH[2] = destHourlyParkingCost;
		value = dmu.getValueForIndex(43, 0);
		Assert.assertEquals(destHourlyParkingCost, (int) value);

		int tripOrigIsTourDest = 44;
		dmu.setTripOrigIsTourDest(tripOrigIsTourDest);
		value = dmu.getValueForIndex(44, 0);
		Assert.assertEquals(tripOrigIsTourDest, (int) value);

		int tripDestIsTourDest = 45;
		dmu.setTripDestIsTourDest(tripDestIsTourDest);
		value = dmu.getValueForIndex(45, 0);
		Assert.assertEquals(tripDestIsTourDest, (int) value);

		dmu.setPersonObject(person);

		boolean freeParking = true;
		person.setFreeParkingAvailableResult(ParkingProvisionModel.FP_MODEL_FREE_ALT);
		value = dmu.getValueForIndex(46, 0);
		Assert.assertEquals(1, (int) value);

		int personTypeCategory = 47;
		person.setPersonTypeCategory(personTypeCategory);
		value = dmu.getValueForIndex(47, 0);
		Assert.assertEquals(personTypeCategory, (int) value);

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
	public void testBadIndex() {
		Logger logger = new FakeLogger();
		CrossBorderModelStructure structure = new CrossBorderModelStructure();
		CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(
				structure, logger);
		dmu.getValueForIndex(21, 0);
	}

	private ModelStructure CreateFakeModelStructure() {
		return new FakeModelStructure();
	}

	private Logger CreateFakeLogger() {
		return new FakeLogger();
	}

}
