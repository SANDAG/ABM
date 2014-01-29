package org.sandag.abm.reporting.emfac2011;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * The {@code SandagAquavisInputBuilder} ...
 * 
 * @author Wu.Sun@sandag.org 1/21/2014
 */
public class AquavisDataBuilder {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(AquavisDataBuilder.class);
	private final PropertyDeluxe properties;
	private Emfac2011SqlUtil sqlUtil = null;

	public AquavisDataBuilder(PropertyDeluxe properties,
			Emfac2011SqlUtil sqlUtil) {
		this.sqlUtil = sqlUtil;
		this.properties = properties;
	}

	public void createAquavisInputs() {
		String scenarioId = properties
				.getString(Emfac2011Properties.SCENARIO_ID);
		String scenarioToken = properties
				.getString(Emfac2011Properties.AQUAVIS_TEMPLATE_SCENARIOID_TOKEN_PROPERTY);

		LOGGER.info("Step 1.1: Creating intrazonal Aquavis table...");
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Properties.CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY),
				scenarioId, scenarioToken);
		LOGGER.info("Step 1.2: Creating network Aquavis table...");
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Properties.CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY),
				scenarioId, scenarioToken);
		LOGGER.info("Step 1.3: Creating trips Aquavis table...");
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Properties.CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY),
				scenarioId, scenarioToken);
	}
}
