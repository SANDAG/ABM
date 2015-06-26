package org.sandag.abm.internalexternal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.util.ResourceUtil;

public class InternalExternalTourManager
{

    private static Logger          logger = Logger.getLogger("internalExternalModel");

    private InternalExternalTour[] tours;
    public static final String   PROPERTIES_DISTRIBUTED_TIME = "distributedTimeCoefficients";
    protected boolean 				readTimeFactors;
    public static final String        PERSON_TIMEFACTOR_NONWORK_FIELD_NAME                    = "timeFactorNonWork";

    InternalExternalModelStructure modelStructure;

    TableDataSet                   personData;

    private boolean                seek;
    private int                    traceId;

    private MersenneTwister        random;

    private class HouseholdClass
    {

        int autos;
        int income;
        int homeMGRA;
    }

    private HashMap<Long, HouseholdClass> householdData;

    /**
     * Constructor. Reads properties file and opens/stores all probability
     * distributions for sampling. Estimates number of airport travel parties
     * and initializes parties[].
     * 
     * @param resourceFile
     *            Property file.
     * 
     *            Creates the array of cross-border tours.
     */
    public InternalExternalTourManager(HashMap<String, String> rbMap, int iteration)
    {

        modelStructure = new InternalExternalModelStructure();

        // append _iteration to file
        String iterationString = "_" + new Integer(iteration).toString();

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");

        String personFile = Util.getStringValueFromPropertyMap(rbMap, "Results.PersonDataFile");
        // Remove extension from filename
        String extension = getFileExtension(personFile);
        personFile = removeFileExtension(personFile) + iterationString + extension;

        personFile = directory + personFile;

        String householdFile = Util.getStringValueFromPropertyMap(rbMap,
                "Results.HouseholdDataFile");

        householdFile = directory + householdFile;
        // Remove extension from filename
        extension = getFileExtension(householdFile);
        householdFile = removeFileExtension(householdFile) + iterationString + extension;

        readHouseholdFile(householdFile);
        personData = readFile(personFile);

        seek = new Boolean(Util.getStringValueFromPropertyMap(rbMap, "internalExternal.seek"));
        traceId = new Integer(Util.getStringValueFromPropertyMap(rbMap, "internalExternal.trace"));

        random = new MersenneTwister(1000001);
        //check if we want to read distributed time factors from the person file
        String readTimeFactorsString = rbMap.get(PROPERTIES_DISTRIBUTED_TIME);
        if (readTimeFactorsString != null)
        {
        	readTimeFactors = Boolean.valueOf(readTimeFactorsString);
        	logger.info("Distributed time coefficients = "+Boolean.toString(readTimeFactors));
        }

    }

    /**
     * Get the file extension
     * 
     * @param fileName
     *            with the extension
     * @return The extension
     */
    public String getFileExtension(String fileName)
    {

        int index = fileName.lastIndexOf(".");
        int length = fileName.length();

        String extension = fileName.substring(index, length);

        return extension;

    }

    /**
     * Get the file name without the extension
     * 
     * @param fileName
     *            The filename with the extension
     * @return The filename without the extension
     */
    public String removeFileExtension(String fileName)
    {
        int index = fileName.lastIndexOf(".");
        String name = fileName.substring(0, index);

        return name;

    }

    /**
     * Read household records and store autos owned.
     * 
     * @param fileName
     *            household file path/name.
     */
    public void readHouseholdFile(String fileName)
    {

        householdData = new HashMap<Long, HouseholdClass>();

        logger.info("Begin reading the data in file " + fileName);

        TableDataSet hhData;
        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            hhData = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // iterate through the table and save number of autos
        for (int i = 1; i <= hhData.getRowCount(); ++i)
        {
            long hhID = (long) hhData.getValueAt(i, "hh_id");
            int autos = (int) hhData.getValueAt(i, "autos");
            int income = (int) hhData.getValueAt(i, "income");
            int mgra = (int) hhData.getValueAt(i, "home_mgra");

            // new household
            HouseholdClass hh = new HouseholdClass();
            hh.autos = autos;
            hh.income = income;
            hh.homeMGRA = mgra;

            // store in HashMap
            householdData.put(hhID, hh);
        }
        logger.info("End reading the data in file " + fileName);
    }

    /**
     * Read the file and return the TableDataSet.
     * 
     * @param fileName
     * @return data
     */
    private TableDataSet readFile(String fileName)
    {

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet data;
        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            data = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);
        return data;
    }

    /**
     * Generate and attribute IE tours
     */
    public void generateTours()
    {

        ArrayList<InternalExternalTour> tourList = new ArrayList<InternalExternalTour>();

        int rows = personData.getRowCount();

        int tourCount = 0;
        for (int i = 1; i <= rows; ++i)
        {

            // TODO: generate IE tours here
            if (((int) personData.getValueAt(i, "ie_choice")) == 2)
            {

                InternalExternalTour tour = new InternalExternalTour(i + 100001);
                tour.setID(i);

                // get the household for the person
                long ID = (long) personData.getValueAt(i, "hh_id");
                HouseholdClass hh = householdData.get(ID);
                tour.setHhID((int)ID);
                
                int pID = (int) personData.getValueAt(i, "person_id");
                tour.setPersonID(pID);
                
                int pnum=(int) personData.getValueAt(i, "person_num");
                tour.setPnum(pnum);

                int age = (int) personData.getValueAt(i, "age");
                String gender = (String) personData.getStringValueAt(i, "gender");

                tour.setOriginMGRA(hh.homeMGRA);
                tour.setIncome(hh.income);
                tour.setAutos(hh.autos);
                tour.setAge(age);

                if (gender.equals("f")) tour.setFemale(1);
                else tour.setFemale(0);
               
                double timeFactorNonWork = 1.0;
                if(readTimeFactors){
                   	timeFactorNonWork = (double) personData.getValueAt(i,
                            personData.getColumnPosition(PERSON_TIMEFACTOR_NONWORK_FIELD_NAME));
                }
                tour.setNonWorkTimeFactor(timeFactorNonWork);              
                           
                tourList.add(tour);

                ++tourCount;
            }

        }
        if (tourList.isEmpty())
        {
            logger.error("Internal-external tour list is empty!!");
            throw new RuntimeException();
        }

        tours = new InternalExternalTour[tourList.size()];
        for (int i = 0; i < tours.length; ++i)
            tours[i] = tourList.get(i);

        logger.info("Total IE tours: " + tourCount);

    }

    /**
     * Create a text file and write all records to the file.
     * 
     */
    public void writeOutputFile(HashMap<String, String> rbMap)
    {

        // Open file and print header

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String tripFileName = directory
                + Util.getStringValueFromPropertyMap(rbMap, "internalExternal.trip.output.file");

        logger.info("Writing IE trips to file " + tripFileName);

        PrintWriter tripWriter = null;
        try
        {
            tripWriter = new PrintWriter(new BufferedWriter(new FileWriter(tripFileName)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + tripFileName + " for writing\n");
            throw new RuntimeException();
        }
        String tripHeaderString = new String(
                "hhID,pnum,personID,tourID,originMGRA,destinationMGRA,originTAZ,destinationTAZ,inbound,originIsTourDestination,destinationIsTourDestination,period,tripMode,boardingTap,alightingTap,valueOfTime\n");
        tripWriter.print(tripHeaderString);

        for (int i = 0; i < tours.length; ++i)
        {
            InternalExternalTrip[] trips = tours[i].getTrips();
            for (int j = 0; j < trips.length; ++j)
                writeTrip(tours[i].getHhID(), tours[i].getPnum(),tours[i].getPersonID(), tours[i].getID(), trips[j], tripWriter);
        }

        tripWriter.close();

    }

    /**
     * Write the trip to the PrintWriter
     * 
     * @param tour
     * @param trip
     * @param tripNumber
     * @param writer
     */
    private void writeTrip(int hhID, int pnum, int personID, int tourID, InternalExternalTrip trip, PrintWriter writer)
    {

        int[] taps = getTapPair(trip);

        String record = new String(hhID+","+pnum+","+personID+","+tourID+","+trip.getOriginMgra() + "," + trip.getDestinationMgra() + ","
                + trip.getOriginTaz() + "," + trip.getDestinationTaz() + "," + trip.isInbound()
                + "," + trip.isOriginIsTourDestination() + ","
                + trip.isDestinationIsTourDestination() + "," + trip.getPeriod() + ","
                + trip.getTripMode() + "," + taps[0] + "," + taps[1] + ","
                +String.format("%9.2f",trip.getValueOfTime()) + "\n");
        writer.print(record);
    }

    /**
     * @return the trips
     */
    public InternalExternalTour[] getTours()
    {
        return tours;
    }

    /**
     * A helper method that returns an array containing boarding tap (element 0)
     * and alighting tap (element 1) for the given trip mode. Returns an array
     * of zeroes if the trip modes are not transit.
     * 
     * @param party
     *            The trip
     * @return An array containing boarding TAP and alighting TAP
     */
    public int[] getTapPair(InternalExternalTrip trip)
    {

        int[] taps = new int[2];

        // ride mode will be -1 if not transit
        int tripMode = trip.getTripMode();
        int rideMode = modelStructure.getRideModeIndexForTripMode(tripMode);

        if (modelStructure.getTripModeIsWalkTransit(tripMode)) taps = trip.getWtwTapPair(rideMode);
        else if (modelStructure.getTripModeIsKnrTransit(tripMode))
            if (trip.isInbound()) taps = trip.getWtdTapPair(rideMode);
            else taps = trip.getDtwTapPair(rideMode);

        return taps;
    }

    public static void main(String[] args)
    {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running IE Model Trip Manager"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        InternalExternalTourManager apm = new InternalExternalTourManager(pMap, 1);
        apm.generateTours();
        apm.writeOutputFile(pMap);

        logger.info("IE Trip Manager successfully completed!");

    }
}
