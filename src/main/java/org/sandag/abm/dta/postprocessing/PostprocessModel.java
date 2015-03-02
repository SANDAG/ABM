package org.sandag.abm.dta.postprocessing;

import com.pb.common.util.ResourceUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

import org.sandag.abm.dta.postprocessing.dtaTrip;
import org.sandag.abm.ctramp.Util;

public class PostprocessModel {

	private static final String PROPERTIES_OUTPUTSPATH            = "dta.postprocessing.outputs.path";
	private static final String PROPERTIES_DISAGGPATHTOD          = "dta.postprocessing.disaggregateTOD.path";
	private static final String PROPERTIES_DISAGGPATHZONE         = "dta.postprocessing.disaggregateZone.path";
	private static final String PROPERTIES_TRIPOUT                = "dta.postprocessing.outputs.TripFile";
	private static final String PROPERTIES_HOUSEHOLD_TRACE_LIST   = "dta.postprocessing.debug.HouseholdIds";
	private static final String PROPERTIES_ORIGIN_TRACE_LIST      = "dta.postprocessing.debug.OriginZones";


    private HashMap<String,String> rbMap;
    public HashSet<Integer>        householdTraceSet;
    public HashSet<Integer>        originTraceSet;

	public String outputsPath;
	public String disaggTODPath;
	public String todType;
	public String outputFile;

	public String inputFile;
	public String marketSegment;
	public double SampleRate;

	public PrintWriter tripWriter;

    private static Logger           logger                         = Logger.getLogger("postprocessModel");


	/**
	 * Default constructor.
	 */
	public PostprocessModel(HashMap<String,String> rbMap, String timeType, double sampleRate, String inputFile, String marketSegment){

		this.rbMap = rbMap;
		this.SampleRate = sampleRate;
		this.inputFile = inputFile;
		this.marketSegment = marketSegment;
		this.todType = timeType;

 	}


	public void runModel(){

		disaggTODPath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_DISAGGPATHTOD);
		outputFile = disaggTODPath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_TRIPOUT);
		outputsPath = Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_OUTPUTSPATH);
		outputFile = outputsPath + Util.getStringValueFromPropertyMap(rbMap, PROPERTIES_TRIPOUT);

		setDebugHouseholdsFromPropertyMap();
		setDebugOrigZonesFromPropertyMap();

		logger.info("Trip file being written to "+outputFile);
		// Write the trip header to the output file
		boolean fileExists = new File(outputFile).isFile();

		FileWriter writer;

		if(fileExists){
			logger.info("Output file already exists.  New data will be appended.");
	        try {
				writer = new FileWriter(new File(outputFile), true);
				tripWriter = new PrintWriter(new BufferedWriter(writer));
			} catch (IOException e) {
	            logger.fatal(String.format("Exception occurred opening Postprocessing output file: %s.",
	                    outputFile));
	            throw new RuntimeException(e);
			}
		}else{
			logger.info("Output file does not exist.  New file being created.");
	        try {
				writer = new FileWriter(new File(outputFile));
				tripWriter = new PrintWriter(new BufferedWriter(writer));
			} catch (IOException e) {
	            logger.fatal(String.format("Exception occurred opening Postprocessing output file: %s.",
	                    outputFile));
	            throw new RuntimeException(e);
			}
			dtaTrip Trip = new dtaTrip();
			Trip.writeHeader(tripWriter);
		}

		if(todType.equalsIgnoreCase("broad")){
			logger.info("Processing Broad TOD Model");
			TableDataSet broadFiles = TableDataSet.readFile(disaggTODPath+inputFile);
			int numFiles = broadFiles.getRowCount();
			for (int i=0; i<numFiles; ++i){
				String inputFileName = broadFiles.getStringValueAt(i+1, "fileName");
				String marketSeg = broadFiles.getStringValueAt(i+1, "marketSegment");
				String matrixName = broadFiles.getStringValueAt(i+1, "matrixName");
				int tod = (int) broadFiles.getValueAt(i+1,"TOD");
				String vehType = broadFiles.getStringValueAt(i+1,"vehicleType");
				int occ = (int) broadFiles.getValueAt(i+1,"vehOcc");
				int toll = (int) broadFiles.getValueAt(i+1, "Toll");
				broadTODProcessing broadModel = new broadTODProcessing(rbMap, SampleRate, inputFile, marketSeg, originTraceSet, tripWriter);
				broadModel.createBroadTODTrips(outputsPath+inputFileName,marketSeg,matrixName,tod,vehType,occ,toll);
			}
		}

		if(todType.equalsIgnoreCase("detailed")){
			logger.info("Processing Detailed TOD Model: "+marketSegment);
			detailedTODProcessing detailModel = new detailedTODProcessing(rbMap, SampleRate, inputFile, marketSegment, householdTraceSet, originTraceSet, tripWriter);
			detailModel.createDetailedTODTrips(outputsPath+inputFile, marketSegment, rbMap);
		}

		tripWriter.close();
	}

	/**
	 * Set the HashSet for debugging households, which contains the IDs of the households to debug.
	 */
    private void setDebugHouseholdsFromPropertyMap()
    {
        householdTraceSet = new HashSet<Integer>();

        // get the household ids for which debug info is required
        String householdTraceStringList = rbMap.get(PROPERTIES_HOUSEHOLD_TRACE_LIST);

        if (householdTraceStringList != null)
        {
            StringTokenizer householdTokenizer = new StringTokenizer(householdTraceStringList, ",");
            while(householdTokenizer.hasMoreTokens())
            {
                String listValue = householdTokenizer.nextToken();
                int idValue = Integer.parseInt(listValue.trim());
                householdTraceSet.add(idValue);
            }
        }

    }

	/**
	 * Set the HashSet for debugging households, which contains the IDs of the households to debug.
	 */
    private void setDebugOrigZonesFromPropertyMap()
    {
        originTraceSet = new HashSet<Integer>();

        // get the household ids for which debug info is required
        String originTraceStringList = rbMap.get(PROPERTIES_ORIGIN_TRACE_LIST);

        if (originTraceStringList != null)
        {
            StringTokenizer originTokenizer = new StringTokenizer(originTraceStringList, ",");
            while(originTokenizer.hasMoreTokens())
            {
                String listValue = originTokenizer.nextToken();
                int idValue = Integer.parseInt(listValue.trim());
                originTraceSet.add(idValue);
            }
        }

    }

	/**
	 * Check if this is a trace household.
	 *
	 * @param householdId
	 * @return  True if a trace household, else false
	 */
	public boolean isTraceHousehold(int householdId){

		return householdTraceSet.contains(householdId);

	}

	/**
	 * Check if this is a trace origin.
	 *
	 * @param householdId
	 * @return  True if a trace household, else false
	 */
	public boolean isTraceOrigin(int origTAZ){

		return originTraceSet.contains(origTAZ);

	}

    /**
	 * @param args
	 */
	public static void main(String[] args) {

		String propertiesFile = null;
        HashMap<String,String> pMap;
        String todType = null;
        String inputFile = null;
        String marketSegment = null;
        double sampleRate = 1.0;

		if (args.length == 0) {
			logger.error( String.format("no properties file base name (without .properties extension) was specified as an argument.") );
	        return;
	    } else{
	    	propertiesFile = args[0];
	    }
		for (int i = 1; i< args.length;++i){
			if (args[i].equalsIgnoreCase("-todType")){
				todType = (String) args[i + 1];
			}
			if (args[i].equalsIgnoreCase("-sampleRate")){
				sampleRate = new Double(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("-inputFile")){
				inputFile = (String) args[i+1];
			}
			if (args[i].equalsIgnoreCase("-marketSegment")){
				marketSegment = (String) args[i+1];
			}
		}
        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);

		logger.info("Running SANDAG Trip TOD Disaggregation Model");

		logger.info("todType = "+todType);
        logger.info("Sample Rate = "+sampleRate);
        logger.info("Input File = "+inputFile);
        logger.info("Market Segment = "+marketSegment);

        PostprocessModel postprocessingModel = new PostprocessModel(pMap,todType,sampleRate,inputFile,marketSegment);
        postprocessingModel.runModel();
	}

}
