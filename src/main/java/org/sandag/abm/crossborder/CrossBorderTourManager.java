package org.sandag.abm.crossborder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.application.SandagTourBasedModel;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.TimeCoefficientDistributions;
import org.sandag.abm.ctramp.Util;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class CrossBorderTourManager
{

    private static Logger     logger = Logger.getLogger(SandagTourBasedModel.class);

    private CrossBorderTour[] tours;
    private int               totalTours;

    private double            sentriShare;

    private double[]          sentriPurposeDistribution;
    private double[]          nonSentriPurposeDistribution;

    CrossBorderModelStructure modelStructure;
    SandagModelStructure      sandagStructure;
    private boolean           seek;
    private int               traceId;
    
    TimeCoefficientDistributions timeDistributions;
    
    boolean distributedTimeCoefficients = false;

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
    public CrossBorderTourManager(HashMap<String, String> rbMap)
    {

        modelStructure = new CrossBorderModelStructure();
        sandagStructure = new SandagModelStructure();

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String nonSentriPurposeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "crossBorder.purpose.nonsentri.file");
        String sentriPurposeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "crossBorder.purpose.sentri.file");

        // the share of cross-border tours that are sentri is an input
        sentriShare = new Double(Util.getStringValueFromPropertyMap(rbMap,
                "crossBorder.sentriShare"));

        // Read the distributions
        sentriPurposeDistribution = setPurposeDistribution(sentriPurposeFile,
                sentriPurposeDistribution);
        nonSentriPurposeDistribution = setPurposeDistribution(nonSentriPurposeFile,
                nonSentriPurposeDistribution);
        totalTours = new Integer(Util.getStringValueFromPropertyMap(rbMap, "crossBorder.tours")
                .replace(",", ""));

        seek = new Boolean(Util.getStringValueFromPropertyMap(rbMap, "crossBorder.seek"));
        traceId = new Integer(Util.getStringValueFromPropertyMap(rbMap, "crossBorder.trace"));

        distributedTimeCoefficients = new Boolean(Util.getStringValueFromPropertyMap(rbMap, "distributedTimeCoefficients"));
        
        if(distributedTimeCoefficients) {
        	timeDistributions = new TimeCoefficientDistributions();
        	timeDistributions.createTimeDistributions(rbMap);
        }
    }

    /**
     * Generate and attribute cross border tours
     */
    public void generateCrossBorderTours()
    {

        // calculate total number of cross border tours
        tours = new CrossBorderTour[totalTours];

        logger.info("Total cross border tours: " + totalTours);

        for (int i = 0; i < tours.length; ++i)
        {

            long seed = i * 10 + 1001;
            CrossBorderTour tour = new CrossBorderTour(seed);

            tours[i] = tour;

            tour.setID(i + 1);

            // determine if tour is sentri, and calculate tour purpose
            if (tour.getRandom() < sentriShare)
            {
                tour.setSentriAvailable(true);
                int purpose = choosePurpose(tour.getRandom(), sentriPurposeDistribution);
                tour.setPurpose((byte) purpose);
            } else
            {
                tour.setSentriAvailable(false);
                int purpose = choosePurpose(tour.getRandom(), nonSentriPurposeDistribution);
                tour.setPurpose((byte) purpose);
            }
            
            //set time factors
            double workTimeFactor = 1.0;
            double nonWorkTimeFactor = 1.0;
            
            if(distributedTimeCoefficients){
            	double rnum = tour.getRandom();
            	workTimeFactor = timeDistributions.sampleFromWorkDistribution(rnum);
            	nonWorkTimeFactor = timeDistributions.sampleFromNonWorkDistribution(rnum);
            
            }
            tour.setWorkTimeFactor((float)workTimeFactor);
            tour.setNonWorkTimeFactor((float)nonWorkTimeFactor);
            
        }
    }

    /**
     * Read file containing probabilities by purpose. Store cumulative
     * distribution in purposeDistribution.
     * 
     * @param fileName
     *            Name of file containing two columns, one row for each purpose.
     *            First column has purpose number, second column has
     *            probability.
     */
    protected double[] setPurposeDistribution(String fileName, double[] purposeDistribution)
    {
        logger.info("Begin reading the data in file " + fileName);
        TableDataSet probabilityTable;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            probabilityTable = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);

        int purposes = modelStructure.NUMBER_CROSSBORDER_PURPOSES;
        purposeDistribution = new double[purposes];

        double total_prob = 0.0;
        // calculate and store cumulative probability distribution
        for (int purp = 0; purp < purposes; ++purp)
        {

            double probability = probabilityTable.getValueAt(purp + 1, 2);

            total_prob += probability;
            purposeDistribution[purp] = total_prob;

        }
        logger.info("End storing cumulative probabilies from file " + fileName);

        return purposeDistribution;
    }

    /**
     * Choose a purpose.
     * 
     * @param random
     *            A uniform random number.
     * @return the purpose.
     */
    protected int choosePurpose(double random, double[] purposeDistribution)
    {
        // iterate through the probability array and choose
        for (int alt = 0; alt < purposeDistribution.length; ++alt)
        {
            if (purposeDistribution[alt] > random) return alt;
        }
        return -99;
    }

    /**
     * Create a text file and write all records to the file.
     * 
     */
    public void writeOutputFile(HashMap<String, String> rbMap)
    {

        // Open file and print header

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String tourFileName = directory
                + Util.getStringValueFromPropertyMap(rbMap, "crossBorder.tour.output.file");
        String tripFileName = directory
                + Util.getStringValueFromPropertyMap(rbMap, "crossBorder.trip.output.file");

        logger.info("Writing cross border tours to file " + tourFileName);
        logger.info("Writing cross border trips to file " + tripFileName);

        PrintWriter tourWriter = null;
        try
        {
            tourWriter = new PrintWriter(new BufferedWriter(new FileWriter(tourFileName)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + tourFileName + " for writing\n");
            throw new RuntimeException();
        }
        String tourHeaderString = new String(
                "id,purpose,sentri,poe,departTime,arriveTime,originMGRA,destinationMGRA,originTAZ,destinationTAZ,tourMode,workTimeFactor,nonWorkTimeFactor,valueOfTime\n");
        tourWriter.print(tourHeaderString);

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
                "tourID,tripID,originPurp,destPurp,originMGRA,destinationMGRA,originTAZ,destinationTAZ,inbound,originIsTourDestination,destinationIsTourDestination,period,tripMode,boardingTap,alightingTap,set,workTimeFactor,nonWorkTimeFactor,valueOfTime\n");
        tripWriter.print(tripHeaderString);

        // Iterate through the array, printing records to the file
        for (int i = 0; i < tours.length; ++i)
        {

            CrossBorderTour tour = tours[i];

            if (seek && tour.getID() != traceId) continue;

            CrossBorderTrip[] trips = tours[i].getTrips();

            if (trips == null) continue;

            writeTour(tour, tourWriter);

            for (int j = 0; j < trips.length; ++j)
            {
                writeTrip(tour, trips[j], j + 1, tripWriter);
            }
        }

        tourWriter.close();
        tripWriter.close();

    }

    /**
     * Write the tour to the PrintWriter
     * 
     * @param tour
     * @param writer
     */
    private void writeTour(CrossBorderTour tour, PrintWriter writer)
    {
        String record = new String(tour.getID() + "," + tour.getPurpose() + ","
                + tour.isSentriAvailable() + "," + tour.getPoe() + "," + tour.getDepartTime() + ","
                + tour.getArriveTime() + "," + tour.getOriginMGRA() + ","
                + tour.getDestinationMGRA() + "," + tour.getOriginTAZ() + ","
                + tour.getDestinationTAZ() + "," + tour.getTourMode() + ","
                + String.format("%9.2f",tour.getWorkTimeFactor()) + "," 
                + String.format("%9.2f",tour.getNonWorkTimeFactor()) + ","
                + String.format("%9.2f", tour.getValueOfTime()) +"\n");
        writer.print(record);

    }

    /**
     * Write the trip to the PrintWriter
     * 
     * @param tour
     * @param trip
     * @param tripNumber
     * @param writer
     */
    private void writeTrip(CrossBorderTour tour, CrossBorderTrip trip, int tripNumber,
            PrintWriter writer)
    {

        String record = new String(tour.getID() + "," + tripNumber + "," + trip.getOriginPurpose()
                + "," + trip.getDestinationPurpose() + "," + trip.getOriginMgra() + ","
                + trip.getDestinationMgra() + "," + trip.getOriginTAZ() + ","
                + trip.getDestinationTAZ() + "," + trip.isInbound() + ","
                + trip.isOriginIsTourDestination() + "," + trip.isDestinationIsTourDestination()
                + "," + trip.getPeriod() + "," + trip.getTripMode() + "," 
                + trip.getBoardTap() + "," + trip.getAlightTap() + "," 
                + trip.getSet() + ","
                        + String.format("%9.2f",tour.getWorkTimeFactor()) + "," 
                        + String.format("%9.2f",tour.getNonWorkTimeFactor()) + ","
                        + String.format("%9.2f", tour.getValueOfTime()) +"\n");
        writer.print(record);
    }
    /**
     * @return the parties
     */
    public CrossBorderTour[] getTours()
    {
        return tours;
    }

    public static void main(String[] args)
    {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running Cross Border Model Tour Manager"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        CrossBorderTourManager apm = new CrossBorderTourManager(pMap);
        apm.generateCrossBorderTours();
        apm.writeOutputFile(pMap);

        logger.info("Cross-Border Tour Manager successfully completed!");

    }
}
