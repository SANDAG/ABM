package org.sandag.abm.reporting.emfac2011;

import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.read.CsvTableReader;
import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * The {@code SandagAquavisInputBuilder} ...
 * 
 * @author crf Started 12/7/12 11:08 AM modified by Wu.Sun@sandag.org 1/21/2014
 * Modified by Wu.Sun@sandag.org 1/21/2014
 */
public class AquavisDataBuilder {
	private final PropertyDeluxe properties;
	private Emfac2011SqlUtil sqlUtil = null;

	public AquavisDataBuilder(PropertyDeluxe properties,
			Emfac2011SqlUtil sqlUtil) {
		this.sqlUtil = sqlUtil;
		this.properties = properties;
	}

	public void createAquavisInputs() {
		createAquavisNetwork();
		createAquavisTrips();
		createAquavisIntrazonal(Emfac2011Properties.AQUAVIS_INTRAZONAL_FILE_PROPERTY);
	}

	private void createAquavisNetwork() {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
	}

	private void createAquavisTrips() {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
	}

	private void createAquavisIntrazonal(String outputFile) {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		addExternalIntrazonalData(
				outputFile.toString(),
				properties
						.getString(Emfac2011Definitions.AQUAVIS_EXTERNAL_INTRAZONAL_TABLE_PROPERTY));
	}

	private void addExternalIntrazonalData(String intrazonalTablePath,
			String externalIntrazonalTablePath) {
		DataTable table = new RowDataTable(new CsvTableReader(
				intrazonalTablePath));
		DataTable externalTable = new RowDataTable(new CsvTableReader(
				externalIntrazonalTablePath));
		for (DataRow row : externalTable)
			table.addRow(row);
	}
}
