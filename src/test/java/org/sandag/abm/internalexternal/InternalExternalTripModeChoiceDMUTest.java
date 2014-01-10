package org.sandag.abm.internalexternal;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.sandag.abm.application.FakeLogger;
import org.sandag.abm.crossborder.CrossBorderModelStructure;
import org.sandag.abm.crossborder.CrossBorderTripModeChoiceDMU;
import org.sandag.abm.ctramp.McLogsumsCalculator;

public class InternalExternalTripModeChoiceDMUTest {

	public double error = .0001;

	@Test
	public void testCrossBorderTripModeChoiceDMU() {
		Logger logger = new FakeLogger();
		InternalExternalModelStructure structure = new InternalExternalModelStructure();
		InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(
				structure, logger);
	}

	@Test
	public void testGetValueForIndexBasic() {
		Logger logger = new FakeLogger();
		InternalExternalModelStructure structure = new InternalExternalModelStructure();
		InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(
				structure, logger);

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

	@Test
	public void testGetValueForIndexTAZ() {
		Logger logger = new FakeLogger();
		InternalExternalModelStructure structure = new InternalExternalModelStructure();
		InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(
				structure, logger);
		// dmu.setTransitSkim(accEgr, lbPrem, skimIndex, value);
		int offset = 100;
		for (int x = 0; x < 50; x++) {
			// System.out.println(x);

			int[] p1Values = { McLogsumsCalculator.WTW };
			int p1 = p1Values[x / 100];
			int[] p2Values = { McLogsumsCalculator.LB, McLogsumsCalculator.EB,
					McLogsumsCalculator.BRT, McLogsumsCalculator.LR,
					McLogsumsCalculator.CR };
			int[] p2Sizes = { 8, 9, 10, 11, 12 };
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
					p3Finder += 1;
					if (afterP1Index - sum < p3Finder) {
						p3 = pvt[(int) ((afterP1Index - sum))];
					} else {
						int i = afterP1Index - sum - p3Finder;
						if (i < p3Values.length) {
							p3 = p3Values[i];
						}
					}
				}

				sum += p2Sizes[y];
				if (afterP1Index < sum && p2Index < 0) {
					p2Index = y;
				}
			}
			int p2 = p2Values[p2Index];

			dmu.setTransitSkim(p1, p2, p3, ((double) (x + 1)) / 100);
			TestTazParams(p1, p2, p3, x, offset, dmu);
		}
	}

	@Test
	public void testGetValueForIndexTAZComplex() {
		Logger logger = new FakeLogger();
		InternalExternalModelStructure structure = new InternalExternalModelStructure();
		InternalExternalTripModeChoiceDMU dmu = new InternalExternalTripModeChoiceDMU(
				structure, logger);
		int offset = 150;
		for (int d = 0; d < 2; d++) {
			if (d == 0) {
				dmu.outboundHalfTourDirection = 1;
			} else {
				dmu.outboundHalfTourDirection = 0;
			}
			for (int x = 0; x < 55; x++) {
				System.out.println(x);

				int[] p1Values = { McLogsumsCalculator.DTW,
						McLogsumsCalculator.WTD };
				int p1 = p1Values[d];
				int[] p2Values = { McLogsumsCalculator.LB,
						McLogsumsCalculator.EB, McLogsumsCalculator.BRT,
						McLogsumsCalculator.LR, McLogsumsCalculator.CR };
				int[] p2Sizes = { 9, 10, 11, 12, 13 };
				int afterP1Index = x % 100;
				int p2Index = -1;
				int p3Finder = 0;
				int sum = 0;
				int p3 = -1;
				int[] pvt = { McLogsumsCalculator.LB_IVT,
						McLogsumsCalculator.EB_IVT,
						McLogsumsCalculator.BRT_IVT,
						McLogsumsCalculator.LR_IVT, McLogsumsCalculator.CR_IVT };

				int[] p3Values = { McLogsumsCalculator.FWAIT,
						McLogsumsCalculator.XWAIT, McLogsumsCalculator.ACC,
						McLogsumsCalculator.EGR, -2, McLogsumsCalculator.AUX,
						McLogsumsCalculator.FARE, McLogsumsCalculator.XFERS };

				for (int y = 0; y < 5; y++) {
					if (p3 == -1) {
						p3Finder += 1;
						if (afterP1Index - sum < p3Finder) {
							p3 = pvt[(int) ((afterP1Index - sum))];
						} else {
							int i = afterP1Index - sum - p3Finder;
							if (i < p3Values.length) {
								p3 = p3Values[i];
							}
						}
					}

					sum += p2Sizes[y];
					if (afterP1Index < sum && p2Index < 0) {
						p2Index = y;
					}
				}
				int p2 = p2Values[p2Index];
				boolean dontChangeP3 = false;
				if (p3 == -2) {
					if (d == 0) {
						p3 = McLogsumsCalculator.ACC;
					} else if (d == 1) {
						p3 = McLogsumsCalculator.EGR;
					}
					dontChangeP3 = true;
				}

				// System.out.println("" + p1 + ", " + p2 + ", " + p3);
				dmu.setTransitSkim(p1, p2, p3, ((double) (x + 1)) / 100);

				double expected = 0;
				if (d == 0 && p3 == McLogsumsCalculator.ACC && !dontChangeP3) {
					TestTazParamsExpected(x, offset, dmu, 0);
				} else if (d == 1 && p3 == McLogsumsCalculator.EGR
						&& !dontChangeP3) {
					TestTazParamsExpected(x, offset, dmu, 0);
				}

				else {
					TestTazParams(p1, p2, p3, x, offset, dmu);
				}
			}
		}
	}

	private void TestTazParams(int p1, int p2, int p3, int i, int offset,
			InternalExternalTripModeChoiceDMU dmu) {
		double value = dmu.getValueForIndex(i + offset, 0);
		double expected = ((double) (i + 1)) / 100;
		Assert.assertEquals(expected, value);

	}

	private void TestTazParamsExpected(int i, int offset,
			InternalExternalTripModeChoiceDMU dmu, double expected) {
		double value = dmu.getValueForIndex(i + offset, 0);
		Assert.assertEquals(expected, value);

	}

	@Test(expected = RuntimeException.class)
	public void testBadIndex() {
		Logger logger = new FakeLogger();
		CrossBorderModelStructure structure = new CrossBorderModelStructure();
		CrossBorderTripModeChoiceDMU dmu = new CrossBorderTripModeChoiceDMU(
				structure, logger);
		dmu.getValueForIndex(21, 0);
	}

}
