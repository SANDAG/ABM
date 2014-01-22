package org.sandag.abm.reporting.emfac2011;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.read.CsvTableReader;
import com.pb.sawdust.util.ProcessUtil;
import com.pb.sawdust.util.exceptions.RuntimeIOException;

/**
 * The {@code Emfac2011Runner} class is used to generate an EMFAC2011 SG input
 * file adjusted for travel demand model results, and then run (via the end
 * user) the EMFAC2011 SG model using those inputs.
 * 
 * @author crf Started 2/9/12 9:17 AM
 * 
 *         Wu.Sun@sandag.org combined SandagEmfacRunner with this class cleaned
 *         some codes 1/16/2014
 */
public class Emfac2011Runner {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(Emfac2011Runner.class);
	private final Emfac2011Properties properties;
	private Emfac2011SqlUtil sqlUtil;;

	/**
	 * Constructor specifying the resources used to build the properties used
	 * for the EMFAC2011 SG run.
	 * 
	 * @param propertyResource
	 *            The first properties resource.
	 * 
	 * @param additionalResources
	 *            Any additional properties resources.
	 */
	public Emfac2011Runner(String scenario, String propertyResource,
			String... additionalResources) {
		properties = new Emfac2011Properties(propertyResource,
				additionalResources);
		sqlUtil = new Emfac2011SqlUtil(properties);
	}

	public void runEmfac2011(String scenario) {
		LOGGER.info("***************Running Emfac2011 for SANDAG***********************");
		LOGGER.info("Step 0: Setting up mutable vehicle types");
		// have to call this first because it sets the mutable types, which are,
		// used throughout the EMFAC2011 process
		final Map<String, Set<Emfac2011VehicleType>> aquavisVehicleTypeToEmfacMapping = buildAquavisVehicleTypeToEmfacMapping(properties
				.getPath(Emfac2011Definitions.VEHICLE_CODE_MAPPING_FILE_PROPERTY));

		runEmfac2011(scenario, new Emfac2011Data(properties, sqlUtil) {
			@Override
			protected Map<String, Set<Emfac2011VehicleType>> getAquavisVehicleTypeToEmfacTypeMapping() {
				return aquavisVehicleTypeToEmfacMapping;
			}
		});
	}

	/**
	 * Run the EMFAC2011 model. This method will process the model results (via
	 * AquaVis outputs), create an adjusted EMFAC2011 input file, and initiate
	 * the EMFAC2011 SG model. Because of the way it is set up, the user must
	 * actually set up and run the EMFAC2011 SG model, but this method will
	 * create a dialog window which will walk the user through the steps
	 * required to do that.
	 * 
	 * @param emfac2011Data
	 *            The {@code Emfac2011Data} instance corresponding to the model
	 *            results/run.
	 */
	public void runEmfac2011(String scenario, Emfac2011Data emfac2011Data) {
		LOGGER.info("Step 1: Building Aquavis inputs from SANDAG database schema: "
				+ properties
						.getString(Emfac2011Definitions.SCHEMA_NAME_PROPERTY));
		AquavisDataBuilder builder = new AquavisDataBuilder(properties, sqlUtil);
		builder.createAquavisInputs();		
		LOGGER.info("Step 2: Processing aquavis data");
		DataTable data = emfac2011Data.processAquavisData(scenario, properties);
		LOGGER.info("Step 3: Creating EMFAC2011 input file");
		Emfac2011InputFileCreator inputFileCreator = new Emfac2011InputFileCreator();
		Path inputfile = inputFileCreator.createInputFile(properties, data);
		LOGGER.info("Step 4: Initiating EMFAC2011");
		RunEmfacDialog.createAndShowGUI(inputfile, this);
		LOGGER.info("EMFAC2011 run finished");
	}

	private Map<String, Set<Emfac2011VehicleType>> buildAquavisVehicleTypeToEmfacMapping(
			Path vehicleCodeMappingFile) {
		Map<SandagAutoModes, Set<Emfac2011VehicleType>> mapping = new EnumMap<>(
				SandagAutoModes.class);
		for (SandagAutoModes type : SandagAutoModes.values())
			mapping.put(type, EnumSet.noneOf(Emfac2011VehicleType.class));

		// file has one column =
		// VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN
		// the rest have names which, when made uppercase, should match
		// VehicleType enum
		DataTable vehicleCodeMapping = new RowDataTable(new CsvTableReader(
				vehicleCodeMappingFile.toString()));
		vehicleCodeMapping.setDataCoersion(true);
		Set<String> vehicleCodeColumns = new LinkedHashSet<>();
		for (String column : vehicleCodeMapping.getColumnLabels()) {
			try {
				SandagAutoModes.valueOf(column.toUpperCase());
				vehicleCodeColumns.add(column);
			} catch (IllegalArgumentException e) {
				// absorb - not a valid type column
			}
		}
		Set<Emfac2011VehicleType> mutableVehicleType = EnumSet
				.noneOf(Emfac2011VehicleType.class);
		for (DataRow row : vehicleCodeMapping) {
			Emfac2011VehicleType emfac2011VehicleType = Emfac2011VehicleType
					.getVehicleType(row
							.getCellAsString(Emfac2011Definitions.VEHICLE_CODE_MAPPING_EMFAC2011_VEHICLE_NAME_COLUMN));
			// now dynamically setting mutable vehicle types, so we need to not
			// rely on the defaults
			// if (!emfac2011VehicleType.isMutableType())
			// continue; //skip any non-mutable types, as they can't be used
			for (String column : vehicleCodeColumns) {
				if (row.getCellAsBoolean(column)) {
					mutableVehicleType.add(emfac2011VehicleType); // if a
																	// mapping
																	// exists,
																	// then the
																	// EMFAC
																	// vehicle
																	// type is
																	// assumed
																	// to
																	// be
																	// mutable
					mapping.get(SandagAutoModes.valueOf(column.toUpperCase()))
							.add(emfac2011VehicleType);
				}
			}
		}
		Emfac2011VehicleType.setMutableTypes(mutableVehicleType);
		Map<String, Set<Emfac2011VehicleType>> finalMapping = new HashMap<>();
		for (SandagAutoModes type : mapping.keySet())
			finalMapping.put(type.name(), mapping.get(type));
		return finalMapping;
	}
	
	void runEmfac2011Program() {
		final Path emfacInstallationDir = Paths
				.get(properties
						.getString(Emfac2011Properties.EMFAC2011_INSTALLATION_DIR_PROPERTY));
		final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(
				"glob:*.lnk");
		final List<Path> link = new LinkedList<>();
		FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				Path name = file.getFileName();
				if (name != null && matcher.matches(name)) {
					link.add(file);
					return FileVisitResult.TERMINATE;
				}
				return FileVisitResult.CONTINUE;
			}
		};

		try {
			Files.walkFileTree(emfacInstallationDir,
					Collections.<FileVisitOption> emptySet(), 1, visitor);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		if (link.size() == 0)
			throw new IllegalStateException(
					"Cannot find Emfac2011 shortcut in " + emfacInstallationDir);
		ProcessUtil.runProcess(Arrays.asList("cmd", "/c", link.get(0)
				.toString()));
	}

	public static void main(String... args) {
		double startTime = System.currentTimeMillis();
		String scenario = "2010";

		// do work
		new Emfac2011Runner(scenario, args[0], Arrays.copyOfRange(args, 1,
				args.length)).runEmfac2011(scenario);
		// SandagEmfac2011Runner("D:\\projects\\sandag\\emfac\\output_example\\sandag_emfac2011.properties").runEmfac2011();
		// time stamp
		LOGGER.info("Completed in: "
				+ (float) (((System.currentTimeMillis() - startTime) / 1000.0) / 60.0)
				+ " minutes.");
	}

}
