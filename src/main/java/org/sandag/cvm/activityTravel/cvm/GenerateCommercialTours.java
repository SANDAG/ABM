/*
 Travel Model Microsimulation library
 Copyright (C) 2005 John Abraham jabraham@ucalgary.ca and others


  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package org.sandag.cvm.activityTravel.cvm;

/**
 * @author John Abraham
 *
 * (c) 2003-2010 John Abraham
 */

import com.pb.common.datafile.CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.*;
import com.pb.common.util.ResourceUtil;
import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.activityTravel.cvm.CommercialTour.TripOutputMatrixSpec;
import org.sandag.cvm.common.emme2.*;
import org.sandag.cvm.common.skims.OMXMatrixCollectionReader;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;


public class GenerateCommercialTours implements Runnable, Callable<Object> {

	private static Logger logger = Logger.getLogger(GenerateCommercialTours.class);
	public static MatrixAndTAZTableCache matrixReader;
	public static ResourceBundle propsResource;

	final ArrayList<TripOutputMatrixSpec> tripOutputMatrices = new ArrayList<TripOutputMatrixSpec>();
	final String segmentString;
	final String segmentString1;
	final String segmentString2;


	public GenerateCommercialTours(String genColumn, boolean useTripModes) {
		segmentString = genColumn;
		String[] strings = genColumn.split("_");
		if (strings.length>0) segmentString1 = strings[0];
		else segmentString1 = "";
		if (strings.length>1) segmentString2 = strings[1];
		else segmentString2 = "";
		this.useTripModes = useTripModes;
	}

	/**
	 * Generate commercial tours for Calgary EMME/2 model
	 * @param args is six strings, location of databank, location of coefficients file, name of coefficients file, minZone,maxZone, anyOldMatrixName
	 */
	public static void main(String[] args) {

		// check arguments
		if (args.length != 1) {
			System.out.println("Usage: java "+GenerateCommercialTours.class.getCanonicalName()+" propertiesFileName");
			throw new RuntimeException("Usage: java "+GenerateCommercialTours.class.getCanonicalName()+" propertiesFileName");
		}
		try {

			try {
				// basic setup of connections, variables, readers and the like
				setupStaticData(args);

				//DurationModel2.durationInMinutes=ResourceUtil.getBooleanProperty(propsResource,"DurationInMinutes", false);
				//TourStartTimeModel.startTimeInMinutes=ResourceUtil.getBooleanProperty(propsResource, "StartTimeInMinutes", false);
				DurationModel2.durationInMinutes=silentlyCheckBooleanProperty(propsResource,"DurationInMinutes",false);
				TourStartTimeModel.startTimeInMinutes=silentlyCheckBooleanProperty(propsResource, "StartTimeInMinutes",false);

				zones = zonalAttributes.getColumnAsInt(zonalAttributes.checkColumnPosition("TAZ"));

				String[] vehTypes = ResourceUtil.checkAndGetProperty(propsResource, "FirstPart").split(",");
				String[] timePeriods = ResourceUtil.checkAndGetProperty(propsResource, "SecondPart").split(",");
				ArrayList<GenerateCommercialTours> segmentRunners = new ArrayList<GenerateCommercialTours>();

				if (ResourceUtil.getProperty(propsResource, "RunZones")!=null) {
					logger.info("Setting up Run Zones "+ResourceUtil.getProperty(propsResource, "RunZones"));
					setUpValidZones(ResourceUtil.getProperty(propsResource, "RunZones"));
				} 
				/*if (ResourceUtil.getProperty(propsResource, "ExcludeZones")!=null) {
				logger.info("Excluding Zones "+ResourceUtil.getProperty(propsResource, "ExcludeZones");
				excludeZones(ResourceUtil.getProperty(propsResource, "ExcludeZones"));
			}*/
				TreeMap<String,Matrix> createdMatrices = new TreeMap<String,Matrix>();
				for (String vehType : vehTypes) {
					for (String timePeriod : timePeriods) {
						String genColumn = vehType + "_"+ timePeriod;
						if (timePeriod.equals("")) {
							genColumn = vehType;
							timePeriod = null;
						}
						GenerateCommercialTours generator = new GenerateCommercialTours(genColumn, ResourceUtil.getBooleanProperty(propsResource,  "UseTripModes"));
						segmentRunners.add(generator);
						generator.buildModelStructure(zones,minZone,maxZone,propsResource);
						generator.generationEquation = new IndexLinearFunction();
						generator.prevGenerationEquation = new IndexLinearFunction();

						// read the coefficients and make sure all the data is read in.
						try {
							readCoefficientFiles(csvInputFileReader, vehType, timePeriod);
						} catch (IOException e) {
							String msg = "Error reading coefficeint files "+vehType+" and "+timePeriod+ " from "+ResourceUtil.checkAndGetProperty(propsResource,"CSVFileLocation");
							logger.fatal(msg,e);
							throw new RuntimeException(msg,e);
						}
						generator.setUpCoefficients(args, coefficients);
						if (coefficients2!=null) generator.setUpCoefficients(args, coefficients2);
						
						// TODO move this to coefficent file
						generator.setUpDisutilityGetters();

						if (ResourceUtil.getBooleanProperty(propsResource, "UseSegmentNameInGeneration",true)) {
							generator.generationEquation.addCoefficient(genColumn,1);
						}

						String tripLogPath = ResourceUtil.getProperty(propsResource, "TripLogPath");
						if (tripLogPath !=null) {
							if (!tripLogPath.equals("")) {
								String tripLogFile = tripLogPath +
										"Trip_"+genColumn+".csv";

								generator.openTripLog(new File(tripLogFile));
							}
						}
						generator.readMatrices();
						if (ResourceUtil.getBooleanProperty(propsResource,"ReadOutputMatrices",true)) {
							generator.readOutputMatrices(matrixReader);
						} else {
							generator.createEmptyOutputMatrices(matrixReader.getDim2MatrixSize(),matrixReader.getDim2MatrixExternalNumbers(),createdMatrices);
						}
						generator.landUseTypeMatrix = matrixReader.readMatrix(generator.landUseTypeMatrixName);


					}
				}

				int nThreads = ResourceUtil.getIntegerProperty(propsResource, "nThreads",8);

				threadpool = Executors.newFixedThreadPool(nThreads);

				List<Future<Object>> runners;
				try {
					runners = threadpool.invokeAll(segmentRunners);
					for (Future<Object> runner : runners) {
						runner.get();
					}
				} catch (InterruptedException e) {
					logger.fatal("Thread was interrupted",e);
					throw new RuntimeException("Thread was interrupted",e);
				} catch (ExecutionException e) {
					logger.fatal("Exception in one segment",e);
					throw new RuntimeException("Exception in one segment",e);
				} 

				int totalTours = 0;
				int totalTrips = 0;
				for (GenerateCommercialTours generator : segmentRunners) {
					totalTours += generator.totalTours;
					totalTrips += generator.totalTrips;
					logger.info("Trips for segment "+generator.segmentString);
					generator.vehicleTourTypeChoice.writeTourAndTripSummary();
				}


				logger.info("finished generating "+totalTours+" tours and "+totalTrips+" trips");

				MatrixWriter writer = null;
				if (ResourceUtil.getBooleanProperty(propsResource, "WriteEmmeApiMatrices",false)) {
					try {
						writer = setUpEmmeApiWriter();
						writeAllMatrices(writer,segmentRunners);
					} finally {
						if (writer !=null) {
							try {
								((EmmeApiMatrixWriter) writer).close();
							} catch (IOException e) {
								// oh well
							}
						}
					}
				} else  if (ResourceUtil.getBooleanProperty(propsResource, "WriteDirectEmmeMatrices",false)) {
					//writer = new Emme2MatrixWriter(file)
					//writeMatrices(writer);
					logger.error("WriteDirectEmmeMatrices is no longer supported, please use WriteEmmeApiMatrices instead.  Matrices not written out!");
				} else if (ResourceUtil.getProperty(propsResource, "CSVOutputFileLocation")!=null) {
					writer = new CSVMatrixWriter(new File(ResourceUtil.getProperty(propsResource, "CSVOutputFileLocation")));
					writeAllMatrices(writer, segmentRunners);
				} else if (ResourceUtil.getProperty(propsResource, "TranscadCVMMatrixFile")!=null) {
					writer = new TranscadMatrixWriter(new File(ResourceUtil.getProperty(propsResource, "TranscadCVMMatrixFile")));
					writeAllMatrices(writer, segmentRunners);
				} else {
					writer = new CSVMatrixWriter(new File(ResourceUtil.getProperty(propsResource, "CSVFileLocation")+File.pathSeparator+"TripMatrices.csv"));
					writeAllMatrices(writer,segmentRunners);
				}
				
				logger.info("Finished writing matrices");


			} catch (Throwable e) {
				logger.fatal("Error in CVM program", e);
			}
		} finally {
			if (threadpool!=null) {
				threadpool.shutdown();
			}
			if (matrixReader!=null) {
				MatrixReader actualReader = matrixReader.getActualReader();
				if (actualReader instanceof EmmeApiMatrixReader) {
					try {
						((EmmeApiMatrixReader) actualReader).close();
					} catch (IOException e) {
						logger.warn("IOException trying to close EmmeApiMatrixReader",e);
					}
				}
			}
		}
	}



	private static boolean silentlyCheckBooleanProperty(
			ResourceBundle propsResource2, String name, boolean defaultVal) {
		String str = ResourceUtil.getProperty(propsResource2, name);
		if (str==null) return defaultVal;
		if (str.equals("")) return defaultVal;
		if (str.equalsIgnoreCase("True")) return true;
		if (str.equalsIgnoreCase("False")) return false;
		logger.error(name + "property should be boolean, it is "+name);
		return defaultVal;
	}
	
	private HashMap<String, ChangingTravelAttributeGetter> travelDisutilityTrackers = null;

	private void readMatrices() {
		Iterator modelIterator = models.values().iterator();
		while (modelIterator.hasNext()) {
			ModelUsesMatrices model = (ModelUsesMatrices) modelIterator.next();
			model.readMatrices(matrixReader);
		}
		generationEquation.readMatrices(matrixReader);
		prevGenerationEquation.readMatrices(matrixReader);
	}

	VehicleTourTypeChoice vehicleTourTypeChoice;
	private final boolean useTripModes;
	/**
	 * @return Returns the vehicleTourTypeChoice.
	 */
	public VehicleTourTypeChoice getVehicleTourTypeChoice() {
		return vehicleTourTypeChoice;
	}

	/**
	 * @param vehicleTourTypeChoice The vehicleTourTypeChoice to set.
	 */
	public void setVehicleTourTypeChoice(VehicleTourTypeChoice vehicleTourTypeChoiceParam) {
		vehicleTourTypeChoice = vehicleTourTypeChoiceParam;
	}



	private void generateTheTours() {
		vehicleTourTypeChoice=(CommercialVehicleTourTypeChoice) models.get("VehicleTourType");
		setTourStartTimeModel((TourStartTimeModel) models.get("TourStartTime"));
		setElapsedTravelTimeCalculator((CommercialTravelTimeTracker) models.get("TravelTimeMatrix"));
		totalTours = 0;
		int oldTotalTours = 0;
		totalTrips = 0;
		System.out.println("Generating tours...");
		for (int z = 1;z<zones.length;z++) {
			int zone = zones[z];
			if (isValidZone(zone)) {
				int totalToursForZone = (int) Math.round(generationEquation.calcForIndex(zone,zones[1]));
				int prevToursForZone = (int) Math.round(prevGenerationEquation.calcForIndex(zone,zones[1]));
				int tours = totalToursForZone - prevToursForZone;
				totalTours += tours;
				if (totalTours - oldTotalTours > 100) {
					String msg = null;
					if (totalTours - oldTotalTours == tours)
						msg = "Generating "+tours+" "+segmentString+" tours in zone "+zone+" ... ";
					else 
						msg = "Generated "+(totalTours - oldTotalTours)+" "+segmentString+"tours, including "+tours+" currently being generated in zone "+zone;
					logger.info(msg);
					oldTotalTours = totalTours;
				}
				int tripCount = 0;
				for (int tour = 0; tour < tours; tour ++) {
					CommercialTour t = new CommercialTour(models,this,tripLog,getNextTourNumber());
					t.setOrigin(zone);
					t.sampleStartTime();
					t.sampleVehicleAndTourType();
					t.sampleStops();
					t.addTripsToMatrix();
					tripCount += t.getStopCounts()[0];
				}
				if (logger.isDebugEnabled()) logger.debug(tripCount+" trips generated from "+segmentString+" tours in zone "+zone+" ... ");
				totalTrips += tripCount;
			}

		}
	}

	private void setUpDisutilityGetters() {
		if (isUseTripModes()) {
			// TODO this shouldn't be hardcoded
			HashMap<String, ChangingTravelAttributeGetter> trackers = new HashMap<String, ChangingTravelAttributeGetter>();
			trackers.put("L", (ChangingTravelAttributeGetter) models.get("TripModeL"));
			trackers.put("I", (ChangingTravelAttributeGetter) models.get("TripModeI"));
			trackers.put("M", (ChangingTravelAttributeGetter) models.get("TripModeM"));
			trackers.put("H", (ChangingTravelAttributeGetter) models.get("TripModeH"));
			setTravelDisutilityTrackers(trackers);
		} else {
			HashMap<String, CommercialTravelTimeTracker> trackers = new HashMap<String, CommercialTravelTimeTracker>();
			trackers.put("",((CommercialTravelTimeTracker) models.get("TravelDisutilityMatrix")));
		}
	}

	static TreeSet<Integer> zoneSet = null;
	
	private boolean isValidZone(int zone) {
		if (zone<minZone) return false;
		if (zone > maxZone) return false;
		if (zoneSet==null) return true;
		if (zoneSet.contains(zone)) return true;
		return false;
	}
	

	private static void setUpValidZones(String property) {
		String[] zoneIds = property.split(",");
		zoneSet = new TreeSet<Integer>();
		for (String zoneId : zoneIds) {
			zoneSet.add(Integer.valueOf(zoneId));
		}
	}


	
	private static void setupStaticData(String[] args) {
		try {
			propsResource = new PropertyResourceBundle(new FileInputStream(args[0]));
		} catch (FileNotFoundException e2) {
			logger.fatal("Can't find file "+args[0]);
			throw new RuntimeException("Can't find file "+args[0], e2);
		} catch (IOException e2) {
			logger.fatal("Can't read file "+args[1]);
			throw new RuntimeException("Can't read file "+args[0], e2);
		}
        csvInputFileReader = new CSVFileReader();
        csvInputFileReader.setPadNulls(true);
        csvInputFileReader.setMyDirectory(ResourceUtil.checkAndGetProperty(propsResource,"CSVFileLocation"));
        try {
        	logger.info("Reading ZonalProperties");
            zonalAttributes = csvInputFileReader.readTable(ResourceUtil.getProperty(propsResource, "ZonalPropertiesFileName", "ZonalProperties.csv	"));
            String file2 = ResourceUtil.getProperty(propsResource, "ZonalPropertiesFileName2");
            if (file2 !=null) {
                logger.info("Reading ZonalProperties file 2");
            	TableDataSet attributes2 = csvInputFileReader.readTable(file2);
            	appendNewDataSet(zonalAttributes, attributes2, "TAZ");
            } else {
            	logger.info("No ZonalPropertiesFileName2, just using one zonal properties file");
            }
        } catch (IOException e1) {
        	String msg = "Error reading zonal properties files from "+ResourceUtil.checkAndGetProperty(propsResource,"CSVFileLocation")+" this might be ok if your zonal properties are stored with your matrices";
        	logger.warn(msg,e1);
        }

        minZone = ResourceUtil.getIntegerProperty(propsResource, "StartZone");
        maxZone = ResourceUtil.getIntegerProperty(propsResource, "EndZone");

        if(ResourceUtil.getProperty(propsResource, "SkimDatabase") !=null) setUpDatabaseSkims();
		else if (ResourceUtil.getProperty(propsResource, "EmmeUserInitials") != null) setUpEmmeApiSkims();
		else if (ResourceUtil.getProperty(propsResource, "TranscadSkimLocation") != null) setUpTranscadSkims();
		else if (ResourceUtil.getProperty(propsResource, "OMXSkimLocation") != null) setUpOMXSkims();		
		 else setUpHDF5Skims();

	}
	
	private static void setUpTranscadSkims() {
		matrixReader = new MatrixAndTAZTableCache(
				new TranscadMatrixCollectionReader(new File(ResourceUtil.checkAndGetProperty(propsResource, "TranscadSkimLocation"))), zonalAttributes);
	}

	private static void setUpOMXSkims() {
		matrixReader = new MatrixAndTAZTableCache(
				new OMXMatrixCollectionReader(new File(ResourceUtil.checkAndGetProperty(propsResource, "OMXSkimLocation"))), zonalAttributes);
	}

	private static void setUpHDF5Skims() {
		String skimNameString = ResourceUtil.checkAndGetProperty(propsResource, "SkimNames");
		String[] skimNames = skimNameString.split(" *, *");
		
		String nodeNameString = ResourceUtil.checkAndGetProperty(propsResource, "SkimFileNodeNames");
		String[] nodeNames = nodeNameString.split(" *, *");
		matrixReader = new MatrixAndTAZTableCache(
				new HDF5MatrixReader(new File(ResourceUtil.checkAndGetProperty(propsResource, "SkimFile")),
						nodeNames,
						skimNames), zonalAttributes);
	}
	
	private static void setUpEmmeApiSkims() {
		String initials = ResourceUtil.checkAndGetProperty(propsResource, "EmmeUserInitials");
		String emmeBank = ResourceUtil.checkAndGetProperty(propsResource, "EmmeBank");
		String iks = ResourceUtil.getProperty(propsResource, "EmmeIKS");
		try {
			if (iks==null) {
				matrixReader = new MatrixAndTAZTableCache(
					new EmmeApiMatrixReader(initials, emmeBank), zonalAttributes);
			} else {
				matrixReader = new MatrixAndTAZTableCache(
						new EmmeApiMatrixReader(initials, emmeBank, iks, "", false), zonalAttributes);
			}
		} catch (IOException e) {
			String msg = "Couldn't open the emme 2 databank "+emmeBank;
			logger.fatal(msg,e);
			throw new RuntimeException(msg,e);
		}
	}

	private static EmmeApiMatrixWriter setUpEmmeApiWriter() {
		if (matrixReader.getActualReader() instanceof EmmeApiMatrixReader) {
			return new EmmeApiMatrixWriter((EmmeApiMatrixReader) matrixReader.getActualReader());
		} else {
			EmmeApiMatrixWriter writer;
			String initials = ResourceUtil.checkAndGetProperty(propsResource, "EmmeUserInitials");
			String emmeBank = ResourceUtil.checkAndGetProperty(propsResource, "EmmeBank");
			String iks = ResourceUtil.getProperty(propsResource, "EmmeIKS");
			try {
				if (iks==null) {
					writer = new EmmeApiMatrixWriter(initials, emmeBank);
				} else {
					writer = new EmmeApiMatrixWriter(initials, emmeBank, iks, "", false);
				}
			} catch (IOException e) {
				String msg = "Couldn't open the emme 2 databank "+emmeBank;
				logger.fatal(msg,e);
				throw new RuntimeException(msg,e);
			}
			return writer;
		}
	}

	
	private static void setUpDatabaseSkims() {
		Connection conn;
		try {
			conn = conn=DriverManager.getConnection(
					ResourceUtil.checkAndGetProperty(propsResource, "SkimDatabase"),
					ResourceUtil.checkAndGetProperty(propsResource, "SkimDatabaseUser"),
					ResourceUtil.checkAndGetProperty(propsResource, "SkimDatabasePassword"));
		} catch (SQLException e1) {
			String msg = "Can't open skim database";
			logger.fatal(msg,e1);
			throw new RuntimeException(msg,e1);
		}
		logger.info("Reading SQL Matrices");
		matrixReader=new MatrixAndTAZTableCache(new SQLMatrixReader(conn, 
				ResourceUtil.checkAndGetProperty(propsResource, "SkimQuery"),
				ResourceUtil.checkAndGetProperty(propsResource, "OriginQuery"),
				ResourceUtil.checkAndGetProperty(propsResource, "DestinationQuery")
		), zonalAttributes);
	}

	private static void readCoefficientFiles(CSVFileReader reader, String name1, String name2)
	throws IOException {
		coefficients = reader.readTable(name1);
		if (name2 != null) {
			coefficients2 = reader.readTable(name2);
		} else {
			logger.info("No CoefficientFileName2, just using one coefficient file");
		}
	}

	/**
	 * Takes the columns from one dataset and appends them to another dataset based on a common integer index column.  
	 * This uses the indexing feature in the TableDataSet and so creates a new row index on the dataset using
	 * the common column name.  If you are relying on the row indexing feature from a different column, you'll 
	 * need to reindex it after.
	 * @param datasetToAppendTo the dataset to be modified by appending the new columns
	 * @param newData the dataset containing the new data to be appended to the other data set
	 * @param commonIndexColumn the name of the common index column
	 */
	private static void appendNewDataSet(TableDataSet datasetToAppendTo, TableDataSet newData, String commonIndexColumn) {
		int originalTazColumnNum = datasetToAppendTo.checkColumnPosition(commonIndexColumn);
		datasetToAppendTo.buildIndex(originalTazColumnNum);
		int tazColumn = newData.checkColumnPosition(commonIndexColumn);
		for (int column = 1; column <= newData.getColumnCount(); column++) {
			if (column != tazColumn) {
				String columnName = newData.getColumnLabel(column);
				if (datasetToAppendTo.getColumnPosition(columnName)!=-1) {
					String msg = "Duplicate column name "+columnName+" in first and second dataset";
					logger.fatal(msg);
					throw new RuntimeException(msg);
				}
				float[] newColumnVals = new float[datasetToAppendTo.getRowCount()];
				datasetToAppendTo.appendColumn(newColumnVals, columnName);
				int newColumnNumber = datasetToAppendTo.checkColumnPosition(columnName);
				for (int row = 1; row <= newData.getRowCount();row++) {
					datasetToAppendTo.setIndexedValueAt(((int) newData.getValueAt(row, tazColumn)),newColumnNumber,newData.getValueAt(row,column));
				}
			}
		}
	}

	private void setUpCoefficients(String[] args,
			TableDataSet coefficients) {
		try {
			for (int row=1;row<=coefficients.getRowCount();row++) {
				String modelName = coefficients.getStringValueAt(row,"Model");
				String matrix = coefficients.getStringValueAt(row,"Matrix");
				String alternative = coefficients.getStringValueAt(row,"Alternative");
				String index1 = coefficients.getStringValueAt(row,"Index1");
				String index2 = coefficients.getStringValueAt(row,"Index2");
				double coefficient = coefficients.getValueAt(row,"Value");
				if (modelName.equals("Generation")) {
					if (index1.equalsIgnoreCase("origin")) {
						generationEquation.addCoefficient(matrix,coefficient);
					} else if (index1.equalsIgnoreCase("moms")) {
						generationEquation.addCoefficient(matrix,index2);
					} else {
						throw new CoefficientFormatError("generation equations must have origin or MOMS in Index1");
					}
				} else if (modelName.equalsIgnoreCase("PrevGeneration")) {
					if (index1.equalsIgnoreCase("origin")) {
						prevGenerationEquation.addCoefficient(matrix,coefficient);
					} else if (index1.equalsIgnoreCase("moms")) {
						prevGenerationEquation.addCoefficient(matrix,index2);
					} else throw new CoefficientFormatError("generation equations must have origin or MOMS in Index1");

				} else if (modelName.equals("TripMatrix")) {
					if (alternative.contains(":")) {
						String[] vehicleTour = alternative.split(":");
						tripOutputMatrices.add(new CommercialTour.TripOutputMatrixSpec(matrix, index1, Float.valueOf(index2).floatValue(), (float) coefficient, vehicleTour[0].charAt(0),vehicleTour[1]));
					} else {
						tripOutputMatrices.add(new CommercialTour.TripOutputMatrixSpec(matrix, index1, Float.valueOf(index2).floatValue(), (float) coefficient, alternative.charAt(0),""));
					}
				} else if (modelName.equals("LandUseTypeCode")) {
					setLandUseTypeMatrixName(matrix);
				} else {
					ModelUsesMatrices model = (ModelUsesMatrices) models.get(modelName);
					if (model == null) throw new CoefficientFormatError("Bad model type in coefficients: "+modelName);
					model.addCoefficient(alternative,index1,index2,matrix,coefficient);
				}

			}
		} catch (CoefficientFormatError e) {
			System.out.println("Coefficient format error -- you have an invalid coefficient");
			System.out.println(e.toString());
			System.out.println("Aborting...");
			throw new RuntimeException("Coefficient format error -- you have an invalid coefficient",e);
		}
	}

	void setLandUseTypeMatrixName(String landUseTypeMatrixName) {
		this.landUseTypeMatrixName = landUseTypeMatrixName;
	}


	/**
	 * Method buildModelStructure.
	 * @param propsResource2 
	 */
	private void buildModelStructure(int[] zoneNums, int lowNumber, int highNumber, ResourceBundle propsResource) {
		models.put("VehicleTourType",new CommercialVehicleTourTypeChoice(
				ResourceUtil.getProperty(propsResource,"VehicleTourTypes",
				"LS,LG,LO,MS,MG,MO,IS,IG,IO,HS,HG,HO").split(","),
				ResourceUtil.getBooleanProperty(propsResource,"NestingVTTChoice",true)));

		// don't use these anymore because vehicle and tour type are joint in the same alternative.
		//        models.put("LTour", new TourTypeChoice());
		//        models.put("MTour", new TourTypeChoice());
		//        models.put("HTour", new TourTypeChoice());
		String stopTypeModelString = ResourceUtil.getProperty(propsResource, "StopTypeModels", 
				"LSStopType, LGStopType, LOStopType, MSStopType, MGStopType, MOStopType, ISStopType, IGStopType, IOStopType, HSStopType, HGStopType, HOStopType");
		String[] stopTypeModels = stopTypeModelString.split(",");
		for (String stopTypeModel : stopTypeModels) {
			models.put(stopTypeModel.trim(), new CommercialNextStopPurposeChoice(stopTypeModel.trim().charAt(1)));
		}

		String[] stopLocationModels = ResourceUtil.getProperty(propsResource, "StopLocationModels",
				"LSSStopLocation, LSOStopLocation, LGGStopLocation,LGOStopLocation,LOOStopLocation,MSSStopLocation,MSOStopLocation,MGGStopLocation,MGOStopLocation,MGOStopLocation,MOOStopLocation,ISSStopLocation,ISOStopLocation,IGGStopLocation,IGOStopLocation,IOOStopLocation,HSSStopLocation,HSOStopLocation,HGGStopLocation,HGOStopLocation,HOOStopLocation")
				.split(",");
		for (String stopLocationModel : stopLocationModels) {
			models.put(stopLocationModel.trim(), new CommercialNextStopChoice(zoneNums, lowNumber-1, highNumber+1, stopLocationModel.trim()));
		}

		String[] durationModels = ResourceUtil.getProperty(propsResource, "DurationModels",
				"LODuration,LSDuration,LGDuration,MODuration,MSDuration,MGDuration,IODuration,ISDuration,IGDuration,HODuration,HSDuration,HGDuration")
				.split(",");
		for (String durationModel : durationModels) {
			models.put(durationModel.trim(), new DurationModel2());
		}
		
		//TODO here is the place where we parameterize the trip mode choice
		if (isUseTripModes()) {
			/*String[] tripModeChoiceModels = ResourceUtil.getProperty(propsResource, "TripModeL,TripModelM,TripModeI,TripModeH")
					.split(",");
			for (String tripModeChoiceModel : tripModeChoiceModels) {
				models.put(tripModeChoiceModel.trim(),  new CommercialVehicleTripModeChoice(tripModeChoiceModels));
			}*/
			models.put("TripModeL", new CommercialVehicleTripModeChoice(new String[] {"L:T", "L:NT"}));
			models.put("TripModeM", new CommercialVehicleTripModeChoice(new String[] {"M:T", "M:NT"}));
			models.put("TripModeI", new CommercialVehicleTripModeChoice(new String[] {"I:T", "I:NT"}));
			models.put("TripModeH", new CommercialVehicleTripModeChoice(new String[] {"H:T", "H:NT"}));
			
		}

		models.put("TourStartTime", new TourStartTimeModel());
		models.put("TravelTimeMatrix", new CommercialTravelTimeTracker());
		models.put("TravelDisutilityMatrix", new CommercialTravelTimeTracker());

		ModelUsesMatrices dummyModel = new ModelUsesMatrices() {
			public void readMatrices(MatrixCacheReader matrixReader) {
				// nothing
			}
			public void addCoefficient(String alternative, String index1,
					String index2, String matrixName, double coefficient)
			throws CoefficientFormatError {
				// nothing
			}
			public void init() {
				// nothing
			}

		};

		// dummy models, placeholders for coefficients handled elsewhere (in python)
		models.put("ShipNoShip", dummyModel);
		models.put("GenPerEmployee", dummyModel);
		models.put("TourTOD", dummyModel);

	}

	final Hashtable models = new Hashtable();
	private static int minZone;
	private static int maxZone;
	static TableDataSet coefficients;
	static TableDataSet coefficients2;
	static TableDataSet zonalAttributes;
	private int totalTours;
	private int totalTrips;
	private IndexLinearFunction generationEquation;
	private IndexLinearFunction prevGenerationEquation;
	private static int globalTourNumber = 0;
	private static int[] zones;
	private static CSVFileReader csvInputFileReader;
	private PrintWriter tripLog;


	public static int getNextTourNumber() {
		synchronized(GenerateCommercialTours.class){ 
			return globalTourNumber ++;
		}
	}

	void openTripLog(File tripLogFile) {
		try {
			logger.info("Opening log file "+tripLogFile);
			tripLog = new PrintWriter(new FileWriter(tripLogFile));
			tripLog.println("Model,SerialNo,Person,Trip,Tour,HomeZone,ActorType,OPurp,DPurp,I,J,Time,Mode,StartTime,EndTime,StopDuration,TourType,OriginalTimePeriod,TripMode,TollAvailable");
		} catch (IOException e) {
			logger.fatal("Can't open trip log file "+tripLogFile,e);
			throw new RuntimeException("Can't open trip log file "+tripLogFile, e);
		}
	}

	void closeTripLog() {
		if (tripLog !=null)  {
			tripLog.close();
			logger.info("Closing trip log file for "+segmentString);
		}
	}


	String landUseTypeMatrixName;
	Matrix landUseTypeMatrix;
	private TourStartTimeModel tourStartTimeModel;
	private ChangingTravelAttributeGetter elapsedTravelTimeCalculator;
	private static ExecutorService threadpool;


	/**
	 * Method readMatrices.
	 * @param matrixReader
	 */
	public void readOutputMatrices(MatrixCacheReader matrixReader) {
		for (int i =0; i<tripOutputMatrices.size(); i++) {
			TripOutputMatrixSpec s = (TripOutputMatrixSpec) tripOutputMatrices.get(i);
			s.readMatrices(matrixReader);
		}

	}

	
	/**
	 * Create empty matrix specification
	 */
	private void createEmptyOutputMatrices(int size, int[] externalNumbers, TreeMap<String,Matrix> createdOnes) {
		for (TripOutputMatrixSpec s : tripOutputMatrices) {
			if (s.write) {
				Matrix m = createdOnes.get(s.name);
				if (m==null) {
					s.createMatrix(size, externalNumbers);
					createdOnes.put(s.name, s.matrix);
				} else {
					s.matrix = m;
				}
			}
		}
	}

	/**
	 * Method writeMatrices.
	 * @param matrixWriter
	 */
	void writeMatrices(MatrixWriter matrixWriter) {
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<Matrix> m = new ArrayList<Matrix>();
		for (int i =0; i<tripOutputMatrices.size(); i++) {
			TripOutputMatrixSpec s = (TripOutputMatrixSpec) tripOutputMatrices.get(i);
			if (s.write) {
				names.add(s.name);
				m.add(s.matrix);
			}
		}
		if (names.size()>0) {
			matrixWriter.writeMatrices(names.toArray(new String[names.size()]), m.toArray(new Matrix[m.size()]));
		}
	}

	static void writeAllMatrices(MatrixWriter matrixWriter, ArrayList<GenerateCommercialTours> segmentRunners ) {
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<Matrix> m = new ArrayList<Matrix>();
		for (GenerateCommercialTours segment : segmentRunners) {
			ArrayList<TripOutputMatrixSpec> tripSpecs = segment.tripOutputMatrices;
			for (int i =0; i<tripSpecs.size(); i++) {
				TripOutputMatrixSpec s = (TripOutputMatrixSpec) tripSpecs.get(i);
				if (s.write) {
					if (m.contains(s.matrix)) {
						if (logger.isDebugEnabled()) logger.debug("Already dealing with output matrix "+s.matrix.getName()+" from another segment");
					} else {
						names.add(s.name);
						m.add(s.matrix);
					}
				}
			}
		}
		if (names.size()>0) {
			StringBuffer msg = new StringBuffer("Writing out matrices ");
			for (String name : names) msg.append(","+name);
			logger.info(msg);
			matrixWriter.writeMatrices(names.toArray(new String[names.size()]), m.toArray(new Matrix[m.size()]));
		}
	}


	
	TripOutputMatrixSpec getTripOutputMatrixSpec(char vehicleType, String tripMode, float time) {
		while (time>=24.00) time-=24.00;
		for (int i =0; i<tripOutputMatrices.size(); i++) {
			TripOutputMatrixSpec s = (TripOutputMatrixSpec) tripOutputMatrices.get(i);
			if (time>=s.startTime && time < s.endTime && s.vehicleType==vehicleType && s.tripMode.equals(tripMode)) return s;
		}
		return null;
	}

	@Override
	public void run() {
		logger.info("Starting segment "+segmentString);
		generateTheTours();
		closeTripLog();
		System.out.println("DonE segement "+segmentString +"!");
		logger.info("DonE segement "+segmentString +"!");
		if (tripLog!=null) tripLog.close();
	}

	private void setElapsedTravelTimeCalculator(
			// TODO should use trip modes like getTravelDisutilityTracker does
			ChangingTravelAttributeGetter elapsedTravelTimeCalculator) {
		this.elapsedTravelTimeCalculator = elapsedTravelTimeCalculator;
	}

	public ChangingTravelAttributeGetter getElapsedTravelTimeCalculator() {
		return elapsedTravelTimeCalculator;
	}

	private void setTourStartTimeModel(TourStartTimeModel tourStartTimeModel) {
		this.tourStartTimeModel = tourStartTimeModel;
	}

	public TourStartTimeModel getTourStartTimeModel() {
		return tourStartTimeModel;
	}

	@Override
	public Object call() throws Exception {
		run();
		return null;
	}

	ChangingTravelAttributeGetter getTravelDisutilityTracker() {
		if (!isUseTripModes())
		return travelDisutilityTrackers.get("");
		else {
			String msg = "Trip mode is being used, no defualt travel disutility tracker";
			logger.fatal(msg);
			throw new RuntimeException(msg);
		}
	}

	void setTravelDisutilityTrackers(HashMap<String, ChangingTravelAttributeGetter> trackers) {
		this.travelDisutilityTrackers = trackers;
	}

	public ChangingTravelAttributeGetter getTravelDisutilityTracker(
			String vehicleCode) {
		return travelDisutilityTrackers.get(vehicleCode);

	}

	public boolean isUseTripModes() {
		return useTripModes;
	}


}
