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

package org.sandag.cvm.calgary.weekend;

import com.pb.common.matrix.*;
import org.sandag.cvm.activityTravel.*;
import org.sandag.cvm.common.emme2.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.sandag.cvm.common.datafile.*;
import org.apache.log4j.Logger;

public class GenerateWeekendTours {
	
    private static Logger logger = Logger.getLogger("org.sandag.cvm.calgary.weekend");
    public static MatrixCacheReader matrixReader;
    public static TableDataSetCollection inputData;
    static Properties props = new Properties();
    
    
	/**
	 * Generate commercial tours for Calgary EMME/2 model
	 * @param args is six strings, location of databank, location of coefficients file, name of coefficients file, minZone,maxZone, anyOldMatrixName
	 */
	public static void main(String[] args) {
        
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(args[0]);
        } catch (FileNotFoundException e) {
            System.out.println("error: be sure to put the location of the properties file on the command line");
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            props.load(fin);
        } catch (IOException e) {
            System.out.println("Error reading properties file "+args[0]);
            System.exit(-1);
        }
        
        // get emme/2 matrix
		try {
			matrixReader = new MatrixCacheReader(new Emme2MatrixReader(new File(props.getProperty("databank"))));
        } catch (Exception e) {
            System.out.println("Error opening emme2 databank \""+props.getProperty("databank")+"\"");
            System.out.println(e);
            e.printStackTrace();
        }
        Matrix anyOldMatrix = matrixReader.readMatrix(props.getProperty("anyOldMatrixName"));
        minZone = Integer.valueOf(props.getProperty("minZone")).intValue();
        maxZone = Integer.valueOf(props.getProperty("maxZone")).intValue();
        //int[] zones = anyOldMatrix.getExternalNumbers();
        buildModelStructure(matrixReader,anyOldMatrix,minZone,maxZone);

        // These next lines are just for testing in the case that you don't have an emme2 databank to load
//        int[] zones = new int[10];
//        int minZone = 0;
//        int maxZone = 10;
//        buildModelStructure(null,minZone,maxZone);
        
        String inputLocation = props.getProperty("inputLocation");
        CSVFileReader inputDataReader = new CSVFileReader();
        inputDataReader.setMyDirectory(inputLocation);
        // no output location for now
        inputData = new TableDataSetCollection(inputDataReader, null);
        

        TableDataSet coefficients = inputData.getTableDataSet(props.getProperty("coefficientsTable"));
        
        
        try {
            for (int cn= 1; cn <= coefficients.getRowCount(); cn++) { //cn is "coefficientNumber"
                String modelName = coefficients.getStringValueAt(cn, "Model");
                String matrix = coefficients.getStringValueAt(cn,"Matrix");
                String alternative = coefficients.getStringValueAt(cn,"Alternative");
                String index1 = coefficients.getStringValueAt(cn,"Index1");
                String index2 = coefficients.getStringValueAt(cn,"Index2");
                double coefficient = coefficients.getValueAt(cn,"Value");
                ModelWithCoefficients model = (ModelWithCoefficients) models.get(modelName);
                if (model == null) throw new CoefficientFormatError("Bad model type in coefficients: "+modelName);
                model.addCoefficient(alternative,index1,index2,matrix,coefficient);
                
            }
        } catch (CoefficientFormatError e) {
            System.out.println("Coefficient format error -- you have an invalid coefficient");
            System.out.println(e.toString());
            e.printStackTrace();
            System.out.println("Aborting...");
            System.exit(-1);
        }
        
        Iterator modelIterator = models.values().iterator();
        while (modelIterator.hasNext()) {
            ModelWithCoefficients model = (ModelWithCoefficients) modelIterator.next();
            model.init();
        }
        
//        generationEquation.readMatrices(matrixReader);
//        WeekendTour.readMatrices(matrixReader);
//        
//        CommercialTour.tourTypeChoiceModel=(CommercialVehicleTourTypeChoice) models.get("VehicleTourType");
//        CommercialTour.setTourStartTimeModel((TourStartTimeModel) models.get("TourStartTime"));
//        CommercialTour.setElapsedTravelTimeCalculator((WeekendTravelTimeTracker) models.get("TravelTimeMatrix"));
//        CommercialTour.travelDisutilityTracker = (WeekendTravelTimeTracker) models.get("TravelDisutilityMatrix");
        
        //TODO the household dataset is probably too big to load in the whole thing, so instead step through the file one record at a time
        TableDataSet households = inputData.getTableDataSet(props.getProperty("householdsTable"));
        TableDataSet householdDetails = inputData.getTableDataSet(props.getProperty("householdDetailsTable"));
        householdDetails.buildIndex(householdDetails.checkColumnPosition("hh_ID"));
        // TODO read in person file
        
        int totalTours = 0;
        int totalTrips = 0;
        System.out.println("Generating tours...");
        for (int hhnum = 1;hhnum<=households.getRowCount();hhnum++) {
            if (hhnum %10 == 0) System.out.print(hhnum+" ");
            if (hhnum %200 == 0) System.out.println();
            
            
            WeekendHousehold household = new WeekendHousehold(households, hhnum, householdDetails);
            // TODO should add people by reading them from a dataset
            household.addPeople();
            
            int zone = household.getHomeZone();
            
            // TODO check if to generate tours from externals? Probably not.
            if (zone>=minZone && zone <= maxZone) {
                household.resetCurrentTime();
                WeekendTour tour = household.sampleNextWeekendTour();
                while (tour != null) {
                    totalTours++;
                    // TODO add the trips to the matrix;
    //                        t.addTripsToMatrix();
                    // TODO write the tour to the tour database
                    totalTrips += tour.getStopCounts()[0];
                    tour = household.sampleNextWeekendTour();
                }
            }
          
        } 
        System.out.println();
        

        System.out.println("finished generating "+totalTours+" tours and "+totalTrips+" trips , now writing trip matrices out to emme2 databank");
//        WeekendTour.tourTypeChoiceModel.writeTourAndTripSummary();
        Emme2MatrixWriter matrixWriter = new Emme2MatrixWriter(new File(props.getProperty("databank")));
        // TODO write out all of the trip matrices back into the emme2 databank
//        WeekendTour.writeMatrices(matrixWriter);
        System.out.println("done!");
	}

	/**
	 * Method buildModelStructure.
	 */
	private static void buildModelStructure(MatrixCacheReader matrixReader2, Matrix anyOldMatrix, int lowNumber, int highNumber) {
        
        TourInTimeBand titb = new TourInTimeBand();
        WeekendTour.setTourInTimeBand(titb);
        models.put("tourInBand",titb);
        NextWeekendTourStartTime ttnt = new NextWeekendTourStartTime();
        WeekendTour.setTourStartTimeModel(ttnt);
        models.put("tourStart", ttnt);
        WeekendTourTypeChoice wttc = new WeekendTourTypeChoice();
        wttc.setMatrixReader(matrixReader2);
        models.put("tourType", wttc);
        WeekendTour.tourTypeChoiceModel= wttc;
        models.put("workPrimaryStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone) );
        models.put("schoolPrimaryStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("relCivicPrimaryStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("exercisePrimaryStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("outOfTownPrimaryStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("workIntermediateStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone) );
        models.put("schoolIntermediateStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("relCivicIntermediateStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("exerciseIntermediateStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("outOfTownIntermediateStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("workReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone) );
        models.put("schoolReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("relCivicReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("exerciseReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("outOfTownReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("SELSEReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("chaufReturnStop", new WeekendStopChoice(anyOldMatrix,minZone,maxZone));
        models.put("workStopType", new WeekendStopPurposeChoice("workStopType"));
        models.put("schoolStopType", new WeekendStopPurposeChoice("schoolStopType"));
        models.put("exerciseStopType", new WeekendStopPurposeChoice("exerciseStopType"));
        models.put("relCivicStopType", new WeekendStopPurposeChoice("relCivicStopType"));
        models.put("outOfTownStopType", new WeekendStopPurposeChoice("outOfTownStopType"));
        models.put("chaufStopType", new WeekendStopPurposeChoice("chaufStopType"));
        models.put("SELSEStopType", new WeekendStopPurposeChoice("SELSEStopType"));
        models.put("workDuration", new DurationModel());
        models.put("shopDuration", new DurationModel());
        models.put("relCivicDuration", new DurationModel());
        models.put("eatDuration", new DurationModel());
        models.put("entLeisureDuration", new DurationModel());
        models.put("socialDuration", new DurationModel());
        models.put("exerciseDuration", new DurationModel());
        models.put("schoolDuration", new DurationModel());
        models.put("outOfTownDuration", new DurationModel());
        models.put("pickUpDuration", new DurationModel());
        models.put("dropOffDuration", new DurationModel());
        
        //        models.put("VehicleTourType",new CommercialVehicleTourTypeChoice());
////        models.put("LTour", new TourTypeChoice());
////        models.put("MTour", new TourTypeChoice());
////        models.put("HTour", new TourTypeChoice());
//        models.put("LSStopType", new CommercialNextStopPurposeChoice('S'));
//        models.put("LGStopType", new CommercialNextStopPurposeChoice('G'));
//        models.put("LOStopType", new CommercialNextStopPurposeChoice('O'));
//        models.put("MSStopType", new CommercialNextStopPurposeChoice('S'));
//        models.put("MGStopType", new CommercialNextStopPurposeChoice('G'));
//        models.put("MOStopType", new CommercialNextStopPurposeChoice('O'));
//        models.put("HSStopType", new CommercialNextStopPurposeChoice('S'));
//        models.put("HGStopType", new CommercialNextStopPurposeChoice('G'));
//        models.put("HOStopType", new CommercialNextStopPurposeChoice('O'));
//        models.put("LSSStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("LSOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("LGGStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("LGOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("LOOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("MSSStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("MSOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("MGGStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("MGOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("MOOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("HSSStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("HSOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("HGGStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("HGOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("HOOStopLocation", new WeekendStopChoice(anyOldMatrix, lowNumber, highNumber));
//        models.put("LODuration", new DurationModel());
//        models.put("LSDuration", new DurationModel());
//        models.put("LGDuration", new DurationModel());
//        models.put("MODuration", new DurationModel());
//        models.put("MSDuration", new DurationModel());
//        models.put("MGDuration", new DurationModel());
//        models.put("HODuration", new DurationModel());
//        models.put("HSDuration", new DurationModel());
//        models.put("HGDuration", new DurationModel());
        
        WeekendTravelTimeTracker timeTracker = new WeekendTravelTimeTracker();
        models.put("TravelTimeMatrix",timeTracker);
        WeekendTour.setElapsedTravelTimeCalculator(timeTracker);
        
        WeekendTravelTimeTracker disutilTracker = new WeekendTravelTimeTracker();
        models.put("TravelDisutilityMatrix", disutilTracker);
        WeekendTour.setTravelDisutilityTracker(disutilTracker);
	}

    
    static final Hashtable models = new Hashtable();
    private static int minZone;
    private static int maxZone;
    
//    public static ResultSet readCoefficients(String parameterFileLocation, String tableName) {
//        ResultSet results = null;
//        try {
//            Class.forName("org.relique.jdbc.csv.CsvDriver");
//            Connection conn = DriverManager.getConnection("jdbc:relique:csv:" + parameterFileLocation);
//            Statement stmt = conn.createStatement();
//            results = stmt.executeQuery("SELECT * FROM "+tableName);
//            return results;
//        }
//        catch(Exception e)
//        {
//            System.out.println("JDBC Error connecting to "+parameterFileLocation+" "+ e);
//            e.printStackTrace();
//            System.exit(-1);
//        }
//        return results;
//    }
}
