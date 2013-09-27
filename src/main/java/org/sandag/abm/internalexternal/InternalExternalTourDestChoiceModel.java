package org.sandag.abm.internalexternal;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;

import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.newmodel.ChoiceModelApplication;

/**
 * This class is used for external station destination choice model for IE
 * tours.
 * 
 * 
 * @author Freedman
 * 
 */
public class InternalExternalTourDestChoiceModel {

	private transient Logger logger = Logger.getLogger("internalExternalModel");

	private TazDataManager tazManager;
	private MgraDataManager mgraManager;

	private ChoiceModelApplication destModel;

	private HashMap<String, String> rbMap;

	private InternalExternalTourDestChoiceDMU dcDmu;

	private Matrix tazProbabilities;
	private TableDataSet altData;

	/**
	 * Constructor
	 * 
	 * @param propertyMap
	 *            Resource properties file map.
	 * @param dmuFactory
	 *            Factory object for creation of airport model DMUs
	 */
	public InternalExternalTourDestChoiceModel(HashMap<String, String> rbMap,
			InternalExternalModelStructure modelStructure,
			InternalExternalDmuFactoryIf dmuFactory) {

		this.rbMap = rbMap;

		tazManager = TazDataManager.getInstance(rbMap);
		mgraManager = MgraDataManager.getInstance(rbMap);

		String uecFileDirectory = Util.getStringValueFromPropertyMap(rbMap,
				CtrampApplication.PROPERTIES_UEC_PATH);

		// initiate a DMU
		dcDmu = dmuFactory.getInternalExternalTourDestChoiceDMU();

		// create the full model UECs
		// read the model pages from the property file, create one choice model
		// for each full model
		String internalExternalDCFileName = Util.getStringValueFromPropertyMap(
				rbMap, "internalExternal.dc.uec.file");
		internalExternalDCFileName = uecFileDirectory
				+ internalExternalDCFileName;
		int dataPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"internalExternal.dc.uec.data.page");
		int destModelPage = Util.getIntegerValueFromPropertyMap(rbMap,
				"internalExternal.dc.uec.model.page");
		destModel = new ChoiceModelApplication(internalExternalDCFileName,
				destModelPage, dataPage, rbMap, (VariableTable) dcDmu);

	}

	/**
	 * Calculate taz probabilities. This method initializes and calculates the
	 * tazProbabilities array.
	 */
	public void calculateTazProbabilities(
			InternalExternalDmuFactoryIf dmuFactory) {

		logger.info("Calculating IE Model TAZ Probabilities Arrays");

		// iterate through the alternatives in the alternatives file and set the
		// size term and station logsum for each alternative
		UtilityExpressionCalculator soaModelUEC = destModel.getUEC();
		altData = soaModelUEC.getAlternativeData();

		// initialize the arrays
		int maxTaz = tazManager.getMaxTaz();

		tazProbabilities = new Matrix("Prob_Matrix", "Probability Matrix",
				maxTaz + 1, maxTaz + 1);

		// iterate through origin zones, solve the UEC and store the results in
		// the matrix
		for (int taz = 1; taz <= maxTaz; ++taz) {

			int originTaz = taz;

			// set origin taz in dmu (destination set in UEC by alternative)
			dcDmu.setDmuIndexValues(originTaz, originTaz, originTaz, originTaz,
					false);

			// Calculate utilities & probabilities
			destModel.computeUtilities(dcDmu, dcDmu.getDmuIndexValues());

			// Store probabilities (by purpose)
			double[] probabilities = destModel.getCumulativeProbabilities();

			for (int i = 0; i < probabilities.length; ++i) {

				double cumProb = probabilities[i];
				int destTaz = (int) altData.getValueAt(i + 1, "taz");
				tazProbabilities
						.setValueAt(originTaz, destTaz, (float) cumProb);
			}
		}
		logger.info("Finished Calculating IE Model TAZ Probabilities Arrays");
	}

	/**
	 * Choose a destination TAZ and MGRA for the tour.
	 * 
	 * @param tour
	 *            An IE tour with a tour origin.
	 */
	public void chooseDestination(InternalExternalTour tour) {

		double random = tour.getRandom();
		int originTaz = mgraManager.getTaz(tour.getOriginMGRA());

		if (tour.getDebugChoiceModels()) {
			logger.info("***");
			logger.info("Choosing destination alternative");
			tour.logTourObject(logger, 1000);

		}

		// cycle through probability array for origin taz and find destination
		// station & corresponding MGRA
		int chosenTaz = -1;
		int chosenMgra = -1;
		for (int i = 1; i <= altData.getRowCount(); ++i) {
			int destTaz = (int) altData.getValueAt(i, "taz");
			if (random < tazProbabilities.getValueAt(originTaz, destTaz)) {
				chosenTaz = destTaz;
				chosenMgra = (int) altData.getValueAt(i, "mgraOut");
				break;
			}
		}

		if (chosenTaz == -1) {
			logger.error("Error: IE Tour Destination Choice Model for tour "
					+ tour.getID());
			throw new RuntimeException();
		}

		tour.setDestinationMGRA(chosenMgra);
		tour.setDestinationTAZ(chosenTaz);

		if (tour.getDebugChoiceModels())
			logger.info("Chose taz " + chosenTaz + " mgra " + chosenMgra
					+ " with random " + random);
	}

}
