package org.sandag.abm.application;

import com.pb.common.matrix.Matrix;

public class SandagTestSOA {

	private Matrix distanceMatrix;
	private Matrix expDistanceMatrix;
	private float[] size;
	private float[] lnSize;

	public SandagTestSOA() {

	}

	/**
	 * Create the distance matrix
	 * 
	 * @param zones
	 *            The number of zones
	 */
	public void createDistanceMatrix(int zones) {

		distanceMatrix = new Matrix(zones, zones);

		for (int i = 1; i <= zones; ++i)
			for (int j = 1; j <= zones; ++j) {
				float distance = (float) Math.random() * 100;
				distanceMatrix.setValueAt(i, j, distance);
			}
	}

	/**
	 * Create the exponentiated distance matrix
	 * 
	 * @param zones
	 *            The number of zones
	 */
	public void createExpDistanceMatrix(float distParam, int zones) {

		long createTime = -System.currentTimeMillis();
		expDistanceMatrix = new Matrix(zones, zones);

		for (int i = 1; i <= zones; ++i)
			for (int j = 1; j <= zones; ++j) {
				float expDist = (float) distParam
						* distanceMatrix.getValueAt(i, j);
				expDist = (float) Math.exp(expDist);
				expDistanceMatrix.setValueAt(i, j, expDist);
			}
		createTime += System.currentTimeMillis();
		System.out
				.println("Time to exponentiate distance matrix " + createTime);
	}

	/**
	 * Create the size terms
	 */
	public void createSizeTerms(int zones) {

		size = new float[zones + 1];
		lnSize = new float[zones + 1];

		for (int i = 1; i <= zones; ++i) {
			size[i] = (float) Math.random() * 1000;
			lnSize[i] = (float) Math.log(size[i]);
		}
	}

	public void calculateProbabilitiesOldWay(int observations, int zones,
			float distParam) {

		long oldWayTime = -System.currentTimeMillis();

		float[] prob = new float[zones + 1];
		float[] expUtil = new float[zones + 1];
		float sumExp = 0;

		for (int obs = 0; obs < observations; ++obs) {

			int origin = (int) (Math.random() * (zones - 1)) + 1;
			int destination = (int) (Math.random() * (zones - 1)) + 1;

			float odDist = distanceMatrix.getValueAt(origin, destination);

			// calculate utilities
			for (int stop = 1; stop <= zones; ++stop) {
				float osDist = distanceMatrix.getValueAt(origin, stop);
				float sdDist = distanceMatrix.getValueAt(stop, destination);

				float util = distParam * (osDist + sdDist - odDist)
						+ lnSize[stop];
				expUtil[stop] = (float) Math.exp(util);
				sumExp += expUtil[stop];
			}

			// calculate probabilities
			for (int stop = 1; stop <= zones; ++stop)
				prob[stop] = expUtil[stop] / sumExp;
		}
		oldWayTime += System.currentTimeMillis();
		System.out.println("Time to calculate probabilities old way tazs "
				+ oldWayTime);

	}

	public void calculateProbabilitiesOldWayMGRAs(int observations, int zones,
			float distParam, int mgras) {

		long oldWayMGRATime = -System.currentTimeMillis();

		float[] prob = new float[mgras + 1];
		float[] expUtil = new float[mgras + 1];
		float sumExp = 0;

		for (int obs = 0; obs < observations; ++obs) {

			int origin = (int) (Math.random() * (zones - 1)) + 1;
			int destination = (int) (Math.random() * (zones - 1)) + 1;

			float odDist = distanceMatrix.getValueAt(origin, destination);

			int stopIndex = 1;
			// calculate utilities
			for (int stop = 1; stop <= mgras; ++stop) {

				float osDist = distanceMatrix.getValueAt(origin, stopIndex);
				float sdDist = distanceMatrix
						.getValueAt(stopIndex, destination);

				float util = distParam * (osDist + sdDist - odDist)
						+ lnSize[stopIndex];
				expUtil[stopIndex] = (float) Math.exp(util);
				sumExp += expUtil[stopIndex];

				++stopIndex;
				if (stopIndex > zones)
					stopIndex = 1;
			}

			// calculate probabilities
			for (int stop = 1; stop <= mgras; ++stop)
				prob[stop] = expUtil[stop] / sumExp;

		}
		oldWayMGRATime += System.currentTimeMillis();
		System.out.println("Time to calculate probabilities old way mgras "
				+ oldWayMGRATime);

	}

	public void calculateProbabilitiesNewWay(int observations, int zones) {

		long newWayTime = -System.currentTimeMillis();

		float[] prob = new float[zones + 1];
		float[] expUtil = new float[zones + 1];
		float sumExp = 0;

		for (int obs = 0; obs < observations; ++obs) {

			int origin = (int) (Math.random() * (zones - 1)) + 1;
			int destination = (int) (Math.random() * (zones - 1)) + 1;

			float odExpDist = expDistanceMatrix.getValueAt(origin, destination);

			// calculate utilities
			for (int stop = 1; stop <= zones; ++stop) {
				float osExpDist = expDistanceMatrix.getValueAt(origin, stop);
				float sdExpDist = expDistanceMatrix.getValueAt(stop,
						destination);

				expUtil[stop] = osExpDist * sdExpDist / odExpDist * size[stop];
				sumExp += expUtil[stop];
			}

			// calculate probabilities
			for (int stop = 1; stop <= zones; ++stop)
				prob[stop] = expUtil[stop] / sumExp;
		}

		newWayTime += System.currentTimeMillis();
		System.out.println("Time to calculate probabilities new way tazs "
				+ newWayTime);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		int zones = 4600;
		int observations = 500000;
		float distParam = (float) -0.05;
		int mgras = 32000;

		SandagTestSOA soa = new SandagTestSOA();

		soa.createDistanceMatrix(zones);
		soa.createExpDistanceMatrix(distParam, zones);
		soa.createSizeTerms(zones);

		soa.calculateProbabilitiesOldWayMGRAs(observations, zones, distParam,
				mgras);
		soa.calculateProbabilitiesOldWay(observations, zones, distParam);
		soa.calculateProbabilitiesNewWay(observations, zones);

	}

}
