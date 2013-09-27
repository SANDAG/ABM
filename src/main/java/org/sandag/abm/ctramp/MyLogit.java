package org.sandag.abm.ctramp;

import com.pb.common.math.MathUtil;
import com.pb.common.model.Alternative;
import com.pb.common.model.LogitModel;
import com.pb.common.model.ModelException;

public class MyLogit extends LogitModel {

	private static final int MAX_EXP_ARGUMENT = 400;

	private double[] utilities;
	private double[] util;
	private double[] constant;
	private String[] altName;

	public MyLogit(String n, int numberOfAlternatives) {
		super(n, numberOfAlternatives);

		utilities = new double[numberOfAlternatives];
		util = new double[numberOfAlternatives];
		constant = new double[numberOfAlternatives];
		altName = new String[numberOfAlternatives];

		nf.setMaximumFractionDigits(8);
		nf.setMinimumFractionDigits(8);
	}

	/**
	 * Overrides the base class getUtility() method to call a method to return
	 * the array of exponentiated utilities, having passed to it an array of
	 * utilities.
	 * 
	 * @return The composite utility (logsum value) of all the alternatives.
	 */
	public double getUtility() throws ModelException {

		double sum = 0;
		double base = 0;

		// get the array of utility values to be exponentiated from the
		// alternatives
		// objects.
		int i = 0;
		for (int alt = 0; alt < alternatives.size(); ++alt) {
			Alternative thisAlt = (Alternative) alternatives.get(alt);
			if (thisAlt.isAvailable()) {

				// assign attributes of the alternatives
				util[i] = thisAlt.getUtility();
				constant[i] = thisAlt.getConstant();
				altName[i] = thisAlt.getName();

				// if alternative has a very large negative utility, it isn't
				// available
				if (util[i] + constant[i] < -MAX_EXP_ARGUMENT) {
					utilities[i] = -MAX_EXP_ARGUMENT;
				} else {
					utilities[i] = dispersionParameter
							* (util[i] + constant[i]);
					setAvailability(true);
				}

				i++;
			} else {
				utilities[i++] = -MAX_EXP_ARGUMENT;
			}
		}

		// exponentiate the utilities array and save result in expUtilities.
		MathUtil.expArray(utilities, expUtilities);

		// sum the exponentiated utilities
		for (i = 0; i < expUtilities.length; i++)
			sum += expUtilities[i];

		// if debug, and the alternatives is elemental, log the utility values
		if (debug) {
			for (i = 0; i < expUtilities.length; i++) {
				Boolean elemental = (Boolean) isElementalAlternative.get(i);
				if (elemental.equals(Boolean.TRUE))
					logger.info(String.format("%-20s", altName[i]) + "\t\t"
							+ nf.format(util[i]) + "\t\t\t"
							+ nf.format(constant[i]) + "\t\t\t"
							+ nf.format(Math.exp(utilities[i])));
			}
		}

		if (isAvailable()) {
			base = (1 / dispersionParameter) * MathUtil.log(sum);

			if (Double.isNaN(base))
				throw new ModelException(ModelException.INVALID_UTILITY);

			if (debug)
				logger.info(String.format("%-20s", getName() + " logsum:")
						+ "\t\t" + nf.format(base));

			return base;
		}

		// if nothing avaiable, return a bad utilty
		return -999;
	}

}
