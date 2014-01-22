package org.sandag.abm.reporting.emfac2011;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.TableIndex;
import com.pb.sawdust.tabledata.basic.BasicTableIndex;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.metadata.DataType;
import com.pb.sawdust.tabledata.metadata.TableSchema;
import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * The {@code AbstractEmfac2011Data} class is used to provide data used to
 * modify the EMFAC2011 SG input file. It essentially reads in the generic
 * AquaVis data (which represents the travel demand model results) and refactors
 * it into a data table that is used (by {@link Emfac2011InputFileCreator}) to
 * create an adjusted EMFAC2011 input file.
 * 
 * @author crf Started 2/8/12 9:13 AM
 * Wu.Sun@sandag.org modified 1/21/2014
 */
public abstract class Emfac2011Data {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Emfac2011Data.class);

	/**
	 * Get a mapping from the vehicle types listed in a model's AquaVis results
	 * to their corresponding EMFAC2011 vehicle types. The returned map should
	 * have the (exact) names listed in the AquaVis results as keys, and the set
	 * of EMFAC2011 vehicle types that the represent the AquaVis type. A single
	 * EMFAC2011 may be used in the mappings of multiple AquaVis types (there is
	 * no functional mapping requirement), and only mutable EMFAC2011 vehicle
	 * types may be used in the mapping (see
	 * {@link com.pb.aquavis.emfac2011.Emfac2011VehicleType#getMutableVehicleTypes()}
	 * . Also, <i>all</i> aquavis vehicle types must be represented in the map
	 * (even if it is an empty mapping).
	 * 
	 * @return a map representing the relationship between the AquaVis and
	 *         EMFAC2011 vehicle types.
	 */
	protected abstract Map<String, Set<Emfac2011VehicleType>> getAquavisVehicleTypeToEmfacTypeMapping();

	private final PropertyDeluxe properties;
	private Emfac2011SqlUtil sqlUtil = null;

	public Emfac2011Data(PropertyDeluxe properties, Emfac2011SqlUtil sqlUtil) {
		this.sqlUtil = sqlUtil;
		this.properties = properties;
	}

	private Map<Emfac2011VehicleType, Map<String, Double>> buildVehicleFractioning(
			Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacTypeMapping) {
		// returns a map which says for every emfac vehicle type, what aquavis
		// vehicle types should have their vmt added
		// to it, and by what fraction

		Map<Emfac2011VehicleType, Map<String, Double>> vehicleFractionMap = new EnumMap<>(
				Emfac2011VehicleType.class);
		for (Emfac2011VehicleType type : Emfac2011VehicleType
				.getMutableVehicleTypes())
			vehicleFractionMap.put(type, new HashMap<String, Double>());
		for (String aquavisVehicleType : aquavisVehicleTypeToEmfacTypeMapping
				.keySet()) {
			double fraction = 1.0 / aquavisVehicleTypeToEmfacTypeMapping.get(
					aquavisVehicleType).size();
			for (Emfac2011VehicleType type : aquavisVehicleTypeToEmfacTypeMapping
					.get(aquavisVehicleType)) {
				if (!vehicleFractionMap.containsKey(type))
					throw new IllegalStateException(
							"Emfac vehicle type is not mutable ("
									+ type
									+ ") and should not be component for aquavis type "
									+ aquavisVehicleType);
				vehicleFractionMap.get(type).put(aquavisVehicleType, fraction);
			}
		}
		return vehicleFractionMap;
	}

	public DataTable processAquavisData(String scenario,
			Emfac2011Properties properties) {

		ResultSet network = queryNetwork(sqlUtil, scenario);
		ResultSet trips = queryTrips(sqlUtil, scenario);
		ResultSet intrazonal = queryIntrazonal(sqlUtil, scenario);

		Map<String, List<String>> areas = new HashMap<>(
				properties
						.<String, List<String>> getMap(Emfac2011Properties.AREAS_PROPERTY));
		Map<String, String> districtsToSubareas = new HashMap<>();
		for (String subarea : areas.keySet())
			for (String district : areas.get(subarea))
				districtsToSubareas.put(district, subarea);

		DataTable outputTable = buildEmfacDataTableShell(areas);
		TableIndex<String> index = new BasicTableIndex<>(outputTable,
				Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FIELD, Emfac2011Definitions.EMFAC_2011_DATA_SUB_AREA_FIELD,
				Emfac2011Definitions.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
		index.buildIndex();

		// need to spread out speed fractions - by auto class
		Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacTypeMapping = getAquavisVehicleTypeToEmfacTypeMapping();
		Map<Emfac2011VehicleType, Map<String, Double>> vehicleFractions = buildVehicleFractioning(aquavisVehicleTypeToEmfacTypeMapping);

		LOGGER.debug("Aggregating aquavis network VMT data");
		try {
			while (network.next()) {
				double vol = network.getDouble("volume");
				double len = network.getDouble("length");
				double speed = network.getDouble("speed");
				String vehicleType = network.getString("vClass");
				String district = network.getString("region");
				if (districtsToSubareas.containsKey(district)) {
					String subarea = districtsToSubareas.get(district);
					for (Emfac2011VehicleType emfacVehicle : aquavisVehicleTypeToEmfacTypeMapping
							.get(vehicleType)) {
						double fraction = vehicleFractions.get(emfacVehicle)
								.get(vehicleType);
						for (int r : index.getRowNumbers(Emfac2011SpeedCategory
								.getSpeedCategory(speed).getName(), subarea,
								emfacVehicle.getName()))
							outputTable.setCellValue(
									r,
									Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD,
									(Double) outputTable.getCellValue(r,
											Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD)
											+ fraction * vol * len);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Network VMT aggregation error:" + e.getMessage());
		}

		// need to collect intrazonal vmt and add it to network vmt - by auto
		// class
		LOGGER.debug("Aggregating aquavis intrazonal VMT data");
		HashMap<Integer, Emfac2011AquavisIntrazonal> intrazonalMap = convertIntrazonal(intrazonal);
		try {
			while (trips.next()) {
				int zone = trips.getInt("origin");
				if (zone == trips.getInt("destination")) {
					String district = (String) intrazonalMap.get(zone)
							.getRegion();
					if (districtsToSubareas.containsKey(district)) {
						String subarea = districtsToSubareas.get(district);
						double speed = intrazonalMap.get(zone).getSpeed();
						double vmt = intrazonalMap.get(zone).getDistance()
								* trips.getInt("trips");
						String vehicleType = trips.getString("vClass");
						for (Emfac2011VehicleType emfacVehicle : aquavisVehicleTypeToEmfacTypeMapping
								.get(vehicleType)) {
							double fraction = vehicleFractions
									.get(emfacVehicle).get(vehicleType);
							for (int r : index.getRowNumbers(
									Emfac2011SpeedCategory.getSpeedCategory(
											speed).getName(), subarea,
									emfacVehicle.getName()))
								outputTable.setCellValue(
										r,
										Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD,
										(Double) outputTable.getCellValue(r,
												Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD)
												+ fraction * vmt);
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Intrazonal VMT aggregation error:" + e.getMessage());
		}

		LOGGER.debug("Building speed fractions");
		// build fractions
		index = new BasicTableIndex<>(outputTable,
				Emfac2011Definitions.EMFAC_2011_DATA_SUB_AREA_FIELD,
				Emfac2011Definitions.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD);
		index.buildIndex();
		for (Emfac2011VehicleType emfacVehicle : Emfac2011VehicleType
				.getMutableVehicleTypes()) {
			for (String subarea : areas.keySet()) {
				double sum = 0.0;
				int count = 0;
				for (DataRow row : outputTable.getIndexedRows(index, subarea,
						emfacVehicle.getName())) {
					sum += row.getCellAsDouble(Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD);
					count++;
				}
				for (int r : index.getRowNumbers(subarea,
						emfacVehicle.getName()))
					outputTable.setCellValue(
							r,
							Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FRACTION_FIELD,
							sum == 0.0 ? 1.0 / count : (Double) outputTable
									.getCellValue(r, Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD)
									/ sum);
			}
		}

		return outputTable;
	}

	private DataTable buildEmfacDataTableShell(Map<String, List<String>> areas) {
		LOGGER.debug("Building EMFAC data table shell");
		TableSchema schema = new TableSchema("Emfac Data");
		schema.addColumn(Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FIELD, DataType.STRING);
		schema.addColumn(Emfac2011Definitions.EMFAC_2011_DATA_SUB_AREA_FIELD, DataType.STRING);
		schema.addColumn(Emfac2011Definitions.EMFAC_2011_DATA_VEHICLE_TYPE_FIELD, DataType.STRING);
		schema.addColumn(Emfac2011Definitions.EMFAC_2011_DATA_VMT_FIELD, DataType.DOUBLE);
		schema.addColumn(Emfac2011Definitions.EMFAC_2011_DATA_SPEED_FRACTION_FIELD, DataType.DOUBLE);
		DataTable outputTable = new RowDataTable(schema);

		// first, add rows for everything
		for (Emfac2011VehicleType vehicle : Emfac2011VehicleType
				.getMutableVehicleTypes())
			for (String subArea : areas.keySet())
				for (Emfac2011SpeedCategory speed : Emfac2011SpeedCategory
						.values())
					outputTable.addRow(speed.getName(), subArea,
							vehicle.getName(), 0.0, -1.0); // -1
															// is
															// for
															// error
															// checking
															// -
															// 0
															// might
															// pass
															// through
		return outputTable; // unnoticed
	}

	private HashMap<Integer, Emfac2011AquavisIntrazonal> convertIntrazonal(
			ResultSet intrazonal) {
		HashMap<Integer, Emfac2011AquavisIntrazonal> result = new HashMap<Integer, Emfac2011AquavisIntrazonal>();
		try {
			while (intrazonal.next()) {
				int zone = intrazonal.getInt("zone");
				Emfac2011AquavisIntrazonal rec = new Emfac2011AquavisIntrazonal();
				rec.setZone(zone);
				rec.setDistance(intrazonal.getDouble("distance"));
				rec.setRegion(intrazonal.getString("region"));
				rec.setSpeed(intrazonal.getDouble("speed"));
				rec.setaType(intrazonal.getString("aType"));
				result.put(zone, rec);
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage());
		}
		return result;
	}

	private ResultSet queryNetwork(Emfac2011SqlUtil sqlUtil, String schema) {
		ResultSet result = sqlUtil.queryAquavisTables(
				properties.getPath(Emfac2011Definitions.QUERY_AQUAVIS_NETWORK_TEMPLATE_PROPERTY),
				schema,
				properties.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		return result;
	}

	private ResultSet queryTrips(Emfac2011SqlUtil sqlUtil, String schema) {
		ResultSet result = sqlUtil.queryAquavisTables(
				properties.getPath(Emfac2011Definitions.QUERY_AQUAVIS_TRIPS_TEMPLATE_PROPERTY),
				schema,
				properties.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		return result;
	}

	private ResultSet queryIntrazonal(Emfac2011SqlUtil sqlUtil, String schema) {
		ResultSet result = sqlUtil.queryAquavisTables(
				properties.getPath(Emfac2011Definitions.QUERY_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY),
				schema,
				properties.getString(Emfac2011Definitions.AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
		return result;
	}
}
