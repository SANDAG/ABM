package org.sandag.abm.ctramp;

import java.util.Arrays;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;
import com.pb.common.newmodel.ChoiceModelApplication;

public class DestChoiceTwoStageSoaProbabilitiesCalculator {

	private transient Logger soaTwoStageProbsLogger = Logger
			.getLogger("soaTwoStageProbsLogger");

	private TazDataManager tdm;
	private int maxTaz;

	private ChoiceModelApplication cm;

	public DestChoiceTwoStageSoaProbabilitiesCalculator(
			HashMap<String, String> propertyMap, CtrampDmuFactoryIf dmuFactory,
			String soaDistUECPropertyKey,
			String soaDistUECModelSheetPropertyKey,
			String soaDistUECDataSheetPropertyKey) {

		tdm = TazDataManager.getInstance(propertyMap);
		maxTaz = tdm.getMaxTaz();

		String uecFileDirectory = propertyMap
				.get(CtrampApplication.PROPERTIES_UEC_PATH);
		String soaDistUecFileName = propertyMap.get(soaDistUECPropertyKey);
		soaDistUecFileName = uecFileDirectory + soaDistUecFileName;
		int soaModelPage = Integer.parseInt(propertyMap
				.get(soaDistUECModelSheetPropertyKey));
		int soaDataPage = Integer.parseInt(propertyMap
				.get(soaDistUECDataSheetPropertyKey));

		DestChoiceTwoStageSoaTazDistanceUtilityDMU tazDistUtilityDmu = dmuFactory
				.getDestChoiceSoaTwoStageTazDistUtilityDMU();

		// create a ChoiceModelApplication object for the filename, model page
		// and data page.
		cm = new ChoiceModelApplication(soaDistUecFileName, soaModelPage,
				soaDataPage, propertyMap, (VariableTable) tazDistUtilityDmu);

	}

	/**
	 * @param dmuObject
	 *            is the distance utility DMU object
	 */
	public double[][] computeDistanceUtilities(
			DestChoiceTwoStageSoaTazDistanceUtilityDMU dmuObject) {

		double[][] tazDistUtils = new double[maxTaz][maxTaz];
		IndexValues iv = new IndexValues();
		dmuObject.setIndexValuesObject(iv);

		// Loop through combinations of orig/dest TAZs and compute OD utilities
		for (int i = 0; i < tazDistUtils.length; i++) {
			iv.setOriginZone(i + 1);
			iv.setZoneIndex(i + 1);
			cm.computeUtilities(dmuObject, iv);
			tazDistUtils[i] = Arrays.copyOf(cm.getUtilities(),
					tazDistUtils.length);
		}

		return tazDistUtils;
	}

	/**
	 * @param dmuObject
	 *            is the distance utility DMU object
	 * @param distUtilityIndex
	 *            is the distance utility segment index This method signature is
	 *            the default, assuming that no distance probabilities logging
	 *            is required
	 */
	public double[][] computeDistanceProbabilities(
			DestChoiceTwoStageSoaTazDistanceUtilityDMU dmuObject) {

		double[][] tazDistProbs = new double[maxTaz][maxTaz];
		IndexValues iv = new IndexValues();
		dmuObject.setIndexValuesObject(iv);

		// Loop through combinations of orig/dest TAZs and compute OD utilities
		for (int i = 0; i < tazDistProbs.length; i++) {
			iv.setOriginZone(i + 1);
			iv.setZoneIndex(i + 1);
			cm.computeUtilities(dmuObject, iv);
			double[] tempArray = Arrays.copyOf(cm.getCumulativeProbabilities(),
					tazDistProbs.length);
			tazDistProbs[i] = tempArray;
		}

		return tazDistProbs;
	}

	/**
	 * @param dmuObject
	 *            is the distance utility DMU object
	 * @param distUtilityIndex
	 *            is the distance utility segment index This alternative method
	 *            signature allows distance probabilities logging to be written
	 */
	public double[][] computeDistanceProbabilities(int traceOrig,
			DestChoiceTwoStageSoaTazDistanceUtilityDMU dmuObject) {

		double[][] tazDistProbs = new double[maxTaz][maxTaz];
		IndexValues iv = new IndexValues();
		dmuObject.setIndexValuesObject(iv);

		// Loop through combinations of orig/dest TAZs and compute OD utilities
		for (int i = 0; i < tazDistProbs.length; i++) {

			iv.setOriginZone(i + 1);
			iv.setZoneIndex(i + 1);
			cm.computeUtilities(dmuObject, iv);

			if (i == traceOrig - 1) {
				int[] altsToLog = { 0, 500, 1000, 2000, 2500, 3736, 3737, 3738,
						3739, 3500, 4000 };
				cm.logUECResultsSpecificAlts(soaTwoStageProbsLogger,
						"Two stage SOA Dist Utilities from TAZ = " + (i + 1),
						altsToLog);

				double[] probs = cm.getProbabilities();
				double[] utils = cm.getUtilities();
				double total = 0;
				for (int k = 0; k < probs.length; k++)
					total += Math.exp(utils[k]);

				soaTwoStageProbsLogger.info("");
				for (int k = 1; k < altsToLog.length; k++)
					soaTwoStageProbsLogger.info("alt=" + (altsToLog[k] - 1)
							+ ", util=" + utils[altsToLog[k] - 1] + ", prob="
							+ probs[altsToLog[k] - 1]);

				soaTwoStageProbsLogger.info("total exponentiated utility = "
						+ total);
				soaTwoStageProbsLogger.info("");
				soaTwoStageProbsLogger.info("");

			}

			tazDistProbs[i] = Arrays.copyOf(cm.getCumulativeProbabilities(),
					tazDistProbs.length);

		}

		for (int i = 0; i < tazDistProbs.length; i++) {
			soaTwoStageProbsLogger.info("orig=" + (i + 1)
					+ ", dest=3738, cumProb[3737]=" + tazDistProbs[i][3736]
					+ ", cumProb[3738]=" + tazDistProbs[i][3737]
					+ ", prob[3738]="
					+ (tazDistProbs[i][3737] - tazDistProbs[i][3736]));
		}

		return tazDistProbs;

	}

}
