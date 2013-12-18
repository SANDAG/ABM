package org.sandag.abm.application;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.SandagAppendMcLogsumDMU;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class SandagAppendMcLogsumDMUTest {

	@Test
	public void testAppendMcLogsumDMU() {
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(
				CreateFakeModelStructure(), CreateFakeLogger());
	}

	@Test
	public void testGetValueForIndexHousehold() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);

		double value;

		int hhIncome = 5;
		dmu.setIncomeInDollars(hhIncome);
		value = dmu.getValueForIndex(2, 0);
		Assert.assertEquals(hhIncome, (int) value);

		int nAdults = 4;
		dmu.setAdults(nAdults);
		value = dmu.getValueForIndex(3, 0);
		Assert.assertEquals(nAdults, (int) value);

		int householdSize = 6;
		dmu.setHhSize(householdSize);
		value = dmu.getValueForIndex(5, 0);
		Assert.assertEquals(householdSize, (int) value);

		int autos = 3;
		dmu.setAutos(autos);
		value = dmu.getValueForIndex(6, 0);
		Assert.assertEquals(autos, (int) value);

	}

	@Test
	public void testGetValueForIndexPersons() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);

		Person person1 = new Person(household, 1, CreateFakeModelStructure());
		person1.setPersAge(36);
		person1.setPersGender(1);

		Person person2 = new Person(household, 2, CreateFakeModelStructure());
		person2.setPersAge(33);
		person2.setPersGender(2);
		double value;

		int female = 1;
		dmu.setPersonIsFemale(female);
		value = dmu.getValueForIndex(4, 0);
		Assert.assertEquals(female, (int) value);

		female = 0;
		dmu.setPersonIsFemale(female);
		value = dmu.getValueForIndex(4, 0);
		Assert.assertEquals(female, (int) value);

		int age = person1.getAge();
		dmu.setAge(age);
		value = dmu.getValueForIndex(7, 0);
		Assert.assertEquals(age, (int) value);

	}

	@Test
	public void testGetValueForIndexTour() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);

		double value;

		int tourDepart = 3;
		dmu.setDepartPeriod(tourDepart);
		value = dmu.getValueForIndex(0, 0);
		Assert.assertEquals(tourDepart, (int) value);

		int tourArrive = 4;
		dmu.setArrivePeriod(tourArrive);
		value = dmu.getValueForIndex(1, 0);
		Assert.assertEquals(tourArrive, (int) value);

		dmu.setTourObject(tour);
		int t = 0;
		value = dmu.getValueForIndex(8, 0);
		Assert.assertEquals(t, (int) value);

		tour = new Tour(household, ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME,
				ModelStructure.JOINT_NON_MANDATORY_CATEGORY, primaryIndex);
		t = 1;
		dmu.setTourCategoryJoint(t);
		;
		value = dmu.getValueForIndex(8, 0);
		Assert.assertEquals(t, (int) value);

		int nParticipants = 1;
		dmu.setNumberOfParticipantsInJointTour(nParticipants);
		value = dmu.getValueForIndex(9, 0);
		Assert.assertEquals(nParticipants, (int) value);

		Tour workTour = tour = new Tour(household,
				ModelStructure.EAT_OUT_PRIMARY_PURPOSE_NAME,
				ModelStructure.JOINT_NON_MANDATORY_CATEGORY, primaryIndex);
		workTour.setTourModeChoice(0);
		dmu.setWorkTourObject(workTour);
		int isTourSOV = 0;
		value = dmu.getValueForIndex(10, 0);
		Assert.assertEquals(isTourSOV, (int) value);

		isTourSOV = 1;
		dmu.setWorkTourModeIsSOV(isTourSOV);
		value = dmu.getValueForIndex(10, 0);
		Assert.assertEquals(isTourSOV, (int) value);

		int isTourBike = 1;
		dmu.setWorkTourModeIsBike(isTourBike);
		value = dmu.getValueForIndex(11, 0);
		Assert.assertEquals(isTourBike, (int) value);

		int isTourHOV = 1;
		dmu.setWorkTourModeIsHOV(isTourHOV);
		value = dmu.getValueForIndex(12, 0);
		Assert.assertEquals(isTourHOV, (int) value);

		int tourCategoryEscort = 0;
		value = dmu.getValueForIndex(22, 0);
		Assert.assertEquals(tourCategoryEscort, (int) value);

		tourCategoryEscort = 1;
		dmu.setTourCategoryEscort(tourCategoryEscort);
		value = dmu.getValueForIndex(22, 0);
		Assert.assertEquals(tourCategoryEscort, (int) value);

	}

	@Test
	public void testGetValueForIndexSelf() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);
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

	@Test
	public void testGetValueForIndexTAZ() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		Household household = new Household(CreateFakeModelStructure());
		int tourID = 1;
		int primaryIndex = 1;
		int persNum = 1;
		Person person = new Person(household, persNum,
				CreateFakeModelStructure());
		Tour tour = new Tour(person, tourID, primaryIndex);
		dmu.setTourObject(tour);
		dmu.setHouseholdObject(household);

		for (int x = 0; x < 300; x++) {
			System.out.println(x);
			int[] p1Values = { McLogsumsCalculator.WTW,
					McLogsumsCalculator.WTD, McLogsumsCalculator.DTW };
			int p1 = p1Values[x / 100];
			int[] p2Values = { McLogsumsCalculator.LB, McLogsumsCalculator.EB,
					McLogsumsCalculator.BRT, McLogsumsCalculator.LR,
					McLogsumsCalculator.CR };
			int[] p2Sizes = { 16, 18, 20, 22, 24 };
			int afterP1Index = x % 100;
			int p2Index = -1;
			int p3Finder = 0;
			int sum = 0;
			int p3 = -1;
			int[] pvt = { McLogsumsCalculator.LB_IVT,
					McLogsumsCalculator.EB_IVT, McLogsumsCalculator.BRT_IVT,
					McLogsumsCalculator.LR_IVT, McLogsumsCalculator.CR_IVT };

			int[] p3Values = { McLogsumsCalculator.FWAIT,
					McLogsumsCalculator.XWAIT, McLogsumsCalculator.ACC,
					McLogsumsCalculator.EGR, McLogsumsCalculator.AUX,
					McLogsumsCalculator.FARE, McLogsumsCalculator.XFERS };

			for (int y = 0; y < 5; y++) {
				if (p3 == -1) {
					p3Finder += 2;
					if (afterP1Index - sum < p3Finder) {
						p3 = pvt[(int) ((afterP1Index - sum) / 2)];
					} else {
						int i = afterP1Index - sum - p3Finder;
						if (i < p3Values.length * 2) {
							p3 = p3Values[i / 2];
						}
					}
				}

				sum += p2Sizes[y];
				if (afterP1Index < sum && p2Index < 0) {
					p2Index = y;
				}
			}
			int p2 = p2Values[p2Index];
			int[] p4Values = { McLogsumsCalculator.OUT, McLogsumsCalculator.IN };
			int p4 = p4Values[x % 2];

			dmu.setTransitSkim(p1, p2, p3, p4, ((double) (x + 1)) / 100);
			TestTazParams(p1, p2, p3, p4, x, dmu);
		}
	}

	private void TestTazParams(int p1, int p2, int p3, int p4, int i,
			SandagAppendMcLogsumDMU dmu) {
		double value = dmu.getValueForIndex(i + 100, 0);
		double expected = ((double) (i + 1)) / 100;
		Assert.assertEquals(expected, value);

	}

	@Test(expected = RuntimeException.class)
	public void testBadIndex() {
		ModelStructure model = CreateFakeModelStructure();
		SandagAppendMcLogsumDMU dmu = new SandagAppendMcLogsumDMU(model,
				CreateFakeLogger());
		dmu.getValueForIndex(13, 0);
	}

	private ModelStructure CreateFakeModelStructure() {
		return new FakeModelStructure();
	}

	private Logger CreateFakeLogger() {
		return new FakeLogger();
	}

}
