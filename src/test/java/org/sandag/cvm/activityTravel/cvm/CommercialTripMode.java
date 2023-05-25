package org.sandag.cvm.activityTravel.cvm;

import org.apache.log4j.Logger;

import org.sandag.cvm.activityTravel.AlternativeUsesMatrices;
import org.sandag.cvm.activityTravel.CodedAlternative;
import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.TripMode;
import org.sandag.cvm.activityTravel.TripModeChoice;
import org.sandag.cvm.activityTravel.TravelTimeTracker.TravelTimeMatrixSpec;
import org.sandag.cvm.common.emme2.IndexLinearFunction;
import org.sandag.cvm.common.emme2.MatrixCacheReader;

public class CommercialTripMode extends TripMode implements CodedAlternative , AlternativeUsesMatrices  {

	  CommercialTravelTimeTracker noTollDistance = new CommercialTravelTimeTracker();
	CommercialTravelTimeTracker noTollTime = new CommercialTravelTimeTracker();
	CommercialTravelTimeTracker tollDistance = new CommercialTravelTimeTracker();
	CommercialTravelTimeTracker totalDistance = new CommercialTravelTimeTracker();
	CommercialTravelTimeTracker tollTime = new CommercialTravelTimeTracker();
	CommercialTravelTimeTracker tollCost = new CommercialTravelTimeTracker();
	private static Logger logger = Logger.getLogger(CommercialTripMode.class);
	
	CommercialVehicleTripModeChoice getMyCM() {
		return (CommercialVehicleTripModeChoice) myChoiceModel;
	}

	public CommercialTripMode(
			CommercialVehicleTripModeChoice choiceModel,
			String type) {
		super(choiceModel, type);
		try {
			switch (myType) {
			case "M:T":
			case "M:NT": //uses light-heavy truck skims
				noTollDistance.addCoefficient("M", "default", "", "traffic_skims_MD:TRK_L_DIST__MD", 0);
				noTollDistance.addCoefficient("M", "", "6",       "traffic_skims_AM:TRK_L_DIST__AM", 9);
				noTollDistance.addCoefficient("M", "", "15.5",    "traffic_skims_PM:TRK_L_DIST__PM", 19);
				noTollTime.addCoefficient("M", "default", "",     "traffic_skims_MD:TRK_L_TIME__MD", 0);
				noTollTime.addCoefficient("M", "", "6",           "traffic_skims_AM:TRK_L_TIME__AM", 9);
				noTollTime.addCoefficient("M", "", "15.5",        "traffic_skims_PM:TRK_L_TIME__PM", 19);
				tollDistance.addCoefficient("M", "default", "",   "traffic_skims_MD:TRK_L_DIST__MD", 0);  
				tollDistance.addCoefficient("M", "", "6",         "traffic_skims_AM:TRK_L_DIST__AM", 9); 
				tollDistance.addCoefficient("M", "", "15.5",      "traffic_skims_PM:TRK_L_DIST__PM", 19); 
				totalDistance.addCoefficient("M", "default", "",  "traffic_skims_MD:TRK_L_DIST__MD", 0);
				totalDistance.addCoefficient("M", "", "6",        "traffic_skims_AM:TRK_L_DIST__AM", 9);
				totalDistance.addCoefficient("M", "", "15.5",     "traffic_skims_PM:TRK_L_DIST__PM", 19);
				tollTime.addCoefficient("M", "default", "",       "traffic_skims_MD:TRK_L_TIME__MD", 0);
				tollTime.addCoefficient("M", "", "6",             "traffic_skims_AM:TRK_L_TIME__AM", 9);
				tollTime.addCoefficient("M", "", "15.5",          "traffic_skims_PM:TRK_L_TIME__PM", 19);
				tollCost.addCoefficient("M", "default", "",       "traffic_skims_MD:TRK_L_TOLLCOST__MD", 0);
				tollCost.addCoefficient("M", "", "6",             "traffic_skims_AM:TRK_L_TOLLCOST__AM", 9);
				tollCost.addCoefficient("M", "", "15.5",          "traffic_skims_PM:TRK_L_TOLLCOST__PM", 19);
				break;
			case "H:T":
			case "H:NT": //uses heavy-heavy truck skims
				noTollDistance.addCoefficient("H", "default", "", "traffic_skims_MD:TRK_H_DIST__MD", 0);
				noTollDistance.addCoefficient("H", "", "6",       "traffic_skims_AM:TRK_H_DIST__AM", 9);
				noTollDistance.addCoefficient("H", "", "15.5",    "traffic_skims_PM:TRK_H_DIST__PM", 19);
				noTollTime.addCoefficient("H", "default", "",     "traffic_skims_MD:TRK_H_TIME__MD", 0);
				noTollTime.addCoefficient("H", "", "6",           "traffic_skims_AM:TRK_H_TIME__AM", 9);
				noTollTime.addCoefficient("H", "", "15.5",        "traffic_skims_PM:TRK_H_TIME__PM", 19);
				tollDistance.addCoefficient("H", "default", "",   "traffic_skims_MD:TRK_H_DIST__MD", 0); 
				tollDistance.addCoefficient("H", "", "6",         "traffic_skims_AM:TRK_H_DIST__AM", 9); 
				tollDistance.addCoefficient("H", "", "15.5",      "traffic_skims_PM:TRK_H_DIST__PM", 19); 
				totalDistance.addCoefficient("H", "default", "",  "traffic_skims_MD:TRK_H_DIST__MD", 0);
				totalDistance.addCoefficient("H", "", "6",        "traffic_skims_AM:TRK_H_DIST__AM", 9);
				totalDistance.addCoefficient("H", "", "15.5",     "traffic_skims_PM:TRK_H_DIST__PM", 19);
				tollTime.addCoefficient("H", "default", "",       "traffic_skims_MD:TRK_H_TIME__MD", 0);
				tollTime.addCoefficient("H", "", "6",             "traffic_skims_AM:TRK_H_TIME__AM", 9);
				tollTime.addCoefficient("H", "", "15.5",          "traffic_skims_PM:TRK_H_TIME__PM", 19);
				tollCost.addCoefficient("H", "default", "",       "traffic_skims_MD:TRK_H_TOLLCOST__MD", 0);
				tollCost.addCoefficient("H", "", "6",             "traffic_skims_AM:TRK_H_TOLLCOST__AM", 9);
				tollCost.addCoefficient("H", "", "15.5",          "traffic_skims_PM:TRK_H_TOLLCOST__PM", 19);
				break;
			case "I:T":
			case "I:NT": //use medium-heavy truck skims
				noTollDistance.addCoefficient("I", "default", "", "traffic_skims_MD:TRK_M_DIST__MD", 0);
				noTollDistance.addCoefficient("I", "", "6",       "traffic_skims_AM:TRK_M_DIST__AM", 9);
				noTollDistance.addCoefficient("I", "", "15.5",    "traffic_skims_PM:TRK_M_DIST__PM", 19);
				noTollTime.addCoefficient("I", "default", "",     "traffic_skims_MD:TRK_M_TIME__MD", 0);
				noTollTime.addCoefficient("I", "", "6",           "traffic_skims_AM:TRK_M_TIME__AM", 9);
				noTollTime.addCoefficient("I", "", "15.5",        "traffic_skims_PM:TRK_M_TIME__PM", 19);                                 
				tollDistance.addCoefficient("I", "default", "",   "traffic_skims_MD:TRK_M_DIST__MD", 0);    
				tollDistance.addCoefficient("I", "", "6",         "traffic_skims_AM:TRK_M_DIST__AM", 9);   
				tollDistance.addCoefficient("I", "", "15.5",      "traffic_skims_PM:TRK_M_DIST__PM", 19);   
				totalDistance.addCoefficient("I", "default", "",  "traffic_skims_MD:TRK_M_DIST__MD", 0);
				totalDistance.addCoefficient("I", "", "6",        "traffic_skims_AM:TRK_M_DIST__AM", 9);
				totalDistance.addCoefficient("I", "", "15.5",     "traffic_skims_PM:TRK_M_DIST__PM", 19);
				tollTime.addCoefficient("I", "default", "",       "traffic_skims_MD:TRK_M_TIME__MD", 0);
				tollTime.addCoefficient("I", "", "6",             "traffic_skims_AM:TRK_M_TIME__AM", 9);
				tollTime.addCoefficient("I", "", "15.5",          "traffic_skims_PM:TRK_M_TIME__PM", 19);
				tollCost.addCoefficient("I", "default", "",       "traffic_skims_MD:TRK_M_TOLLCOST__MD", 0);
				tollCost.addCoefficient("I", "", "6",             "traffic_skims_AM:TRK_M_TOLLCOST__AM", 9);
				tollCost.addCoefficient("I", "", "15.5",          "traffic_skims_PM:TRK_M_TOLLCOST__PM", 19);
				break;
			case "L:T":
			case "L:NT": //light vehicles use high-VOT auto skims
				noTollDistance.addCoefficient("L", "default", "", "traffic_skims_MD:SOV_NT_H_DIST__MD", 0);
				noTollDistance.addCoefficient("L", "", "6",       "traffic_skims_AM:SOV_NT_H_DIST__AM", 9);
				noTollDistance.addCoefficient("L", "", "15.5",    "traffic_skims_PM:SOV_NT_H_DIST__PM", 19);
				noTollTime.addCoefficient("L", "default", "",     "traffic_skims_MD:SOV_NT_H_TIME__MD", 0);
				noTollTime.addCoefficient("L", "", "6",           "traffic_skims_AM:SOV_NT_H_TIME__AM", 9);
				noTollTime.addCoefficient("L", "", "15.5",        "traffic_skims_PM:SOV_NT_H_TIME__PM", 19);
				tollDistance.addCoefficient("L", "default", "",   "traffic_skims_MD:SOV_TR_H_DIST__MD", 0);
				tollDistance.addCoefficient("L", "", "6",         "traffic_skims_AM:SOV_TR_H_DIST__AM", 9); 
				tollDistance.addCoefficient("L", "", "15.5",      "traffic_skims_PM:SOV_TR_H_DIST__PM", 19);  
				totalDistance.addCoefficient("L", "default", "",  "traffic_skims_MD:SOV_TR_H_DIST__MD", 0);
				totalDistance.addCoefficient("L", "", "6",        "traffic_skims_AM:SOV_TR_H_DIST__AM", 9);
				totalDistance.addCoefficient("L", "", "15.5",     "traffic_skims_PM:SOV_TR_H_DIST__PM", 19);
				tollTime.addCoefficient("L", "default", "",       "traffic_skims_MD:SOV_TR_H_TIME__MD", 0);
				tollTime.addCoefficient("L", "", "6",             "traffic_skims_AM:SOV_TR_H_TIME__AM", 9);
				tollTime.addCoefficient("L", "", "15.5",          "traffic_skims_PM:SOV_TR_H_TIME__PM", 19);
				tollCost.addCoefficient("L", "default", "",       "traffic_skims_MD:SOV_TR_H_TOLLCOST__MD", 0);
				tollCost.addCoefficient("L", "", "6",             "traffic_skims_AM:SOV_TR_H_TOLLCOST__AM", 9);
				tollCost.addCoefficient("L", "", "15.5",          "traffic_skims_PM:SOV_TR_H_TOLLCOST__PM", 19);
				break;
			default:
				logger.fatal("Invalid tNCVehicle type in trip mode choice model "+myType);
				throw new RuntimeException("Invalid tNCVehicle type in trip mode choice model "+myType);
			}
		} catch (CoefficientFormatError e) {
			logger.fatal("Problem setting up coefficeint for trip modes", e);
			throw new RuntimeException("Problem setting up coefficient for trip modes", e);
		}
	}

	@Override
	public double getUtility() {
		double tollOptTotalDistance = totalDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
		double tollPortion = 0;
		if (tollOptTotalDistance >0) {
			//tollPortion = tollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)/tollOptTotalDistance;
			tollPortion=0;
		}
		double tollDisutility = 0;
		switch (myType) {
		// FIXME parameterize them in the .CSV file
		case "L:NT":
			return getMyCM().dispersionParam*(
					-0.313*noTollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.138*noTollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.01 *tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType));
			
		
		case "I:NT":
		case "M:NT":
			return getMyCM().dispersionParam*(
					-0.313*noTollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.492*noTollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.01 *tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType));
					
		case "H:NT":
			return getMyCM().dispersionParam*(
					-0.313*noTollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.580*noTollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					-0.01 *tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType));
		//	return -999.0;
		case "L:T":
			
			//if(toll<0.01 || toll>99999)
			return -999.0;
			/*
			double toll = tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
			
			tollDisutility = 
			-0.313*tollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			-0.138*tollOptTotalDistance+
			-0.01 * toll;
			return getMyCM().dispersionParam * tollDisutility + 
					getMyCM().portionParam * tollPortion; */
		case "I:T":
		case "M:T":
			
			//if(toll<0.01 || toll>99999)
			return -999.0;
			/*
			 toll = tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
			 
			tollDisutility = 
			-0.313*tollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			-0.492*tollOptTotalDistance+
			-0.01 * tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
			return getMyCM().dispersionParam * tollDisutility + 
					getMyCM().portionParam * tollPortion;
		*/
		case "H:T":
			
			//if(toll<0.01 || toll>99999)
			return -999.0;
		/*	toll = tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
				
			tollDisutility = 
			-0.313*tollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			-0.580*tollOptTotalDistance+
			-0.01 * tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
			return getMyCM().dispersionParam * tollDisutility + 
					getMyCM().portionParam * tollPortion;
		*/
		}
		String msg = "Invalid tNCVehicle toll trip mode "+myType;
		logger.fatal(msg);
		throw new RuntimeException(msg);
	}

	@Override
	public void readMatrices(MatrixCacheReader matrixReader) {
		tollCost.readMatrices(matrixReader);
		noTollTime.readMatrices(matrixReader);
		noTollDistance.readMatrices(matrixReader);
		tollDistance.readMatrices(matrixReader);
		totalDistance.readMatrices(matrixReader);
		tollTime.readMatrices(matrixReader);
	}

	@Override
	public void addCoefficient(String index1, String index2, String matrix,
			double coefficient) throws CoefficientFormatError {
			logger.warn("Ignoring coefficeiint "+index1+" "+index2+" "+matrix+" "+coefficient+", trip mode matrix coefficients are still hardcoded");
	}

	public double getTollDistance() {
		return  tollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
	}

	public Double getTollTime() {
		return tollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
	}

	public Double getNonTollTime() {
		return noTollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
	}

	public String reportAttributes() {
		switch (myType) {
		// FIXME parameterize them in the .CSV file
		case "L:NT":
		case "I:NT":
		case "M:NT":
		case "H:NT":
			return "time:"+noTollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
					" dist:"+noTollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
		case "L:T":
		case "I:T":
		case "M:T":
		case "H:T":
			return "time:"+tollTime.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			" dist:"+totalDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			" tolldist:"+tollDistance.getTravelAttribute(origin, destination, timeOfDay, vehicleType)+
			" toll:"+tollCost.getTravelAttribute(origin, destination, timeOfDay, vehicleType);
		}
		logger.fatal("Oops invalid trip mode for reportATtributes()");
		throw new RuntimeException("Oops invalid trip mode for reportATtributes()");
	}

}
