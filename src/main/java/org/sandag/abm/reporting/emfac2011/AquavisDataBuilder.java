package org.sandag.abm.reporting.emfac2011;

import java.nio.file.Path;
import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.read.CsvTableReader;
import com.pb.sawdust.tabledata.write.CsvTableWriter;
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

	public void createAquavisInputs(String aquavisNetworkPathProperty,
			String aquavisTripsPathProperty,
			String aquavisIntrazonalPathProperty) {
		createAquavisNetwork(properties.getPath(aquavisNetworkPathProperty));
		createAquavisTrips(properties.getPath(aquavisTripsPathProperty));
		createAquavisIntrazonal(properties
				.getPath(aquavisIntrazonalPathProperty));
	}

	private void createAquavisNetwork(Path outputFile) {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		// writeTableToCsv(AQUAVIS_NETWORK_TABLE, schema, outputFile);
	}

	private void createAquavisTrips(Path outputFile) {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		// writeTableToCsv(AQUAVIS_TRIPS_TABLE, schema, outputFile);
	}

	private void createAquavisIntrazonal(Path outputFile) {
		String schema = properties
				.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY);
		sqlUtil.detemplifyAndRunScript(
				properties
						.getPath(Emfac2011Definitions.CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY),
				schema,
				properties
						.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		// writeTableToCsv(AQUAVIS_INTRAZONAL_TABLE, schema, outputFile);
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
		new CsvTableWriter(intrazonalTablePath).writeTable(table);
	}

}
