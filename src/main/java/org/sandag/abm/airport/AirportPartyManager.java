package org.sandag.abm.airport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.Util;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

public class AirportPartyManager
{

    private static Logger  logger = Logger.getLogger("SandagTourBasedModel.class");

    private AirportParty[] parties;

    private double[]       purposeDistribution;
    private double[][]     sizeDistribution;
    private double[][]     durationDistribution;
    private double[][]     incomeDistribution;
    private double[][]     departureDistribution;
    private double[][]     arrivalDistribution;

    
    SandagModelStructure   sandagStructure;
    private String airportCode;
    
    private float avShare;
    

    /**
     * Constructor. Reads properties file and opens/stores all probability
     * distributions for sampling. Estimates number of airport travel parties
     * and initializes parties[].
     * 
     * @param resourceFile
     *            Property file.
     */
    public AirportPartyManager(HashMap<String, String> rbMap, float sampleRate, String airportCode)
    {
        sandagStructure = new SandagModelStructure();
        this.airportCode = airportCode;

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String purposeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".purpose.file");
        String sizeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".size.file");
        String durationFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".duration.file");
        String incomeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".income.file");
        String departFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".departureTime.file");
        String arriveFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".arrivalTime.file");
 
        // Read the distributions
        setPurposeDistribution(purposeFile);
        sizeDistribution = setDistribution(sizeDistribution, sizeFile);
        durationDistribution = setDistribution(durationDistribution, durationFile);
        incomeDistribution = setDistribution(incomeDistribution, incomeFile);
        departureDistribution = setDistribution(departureDistribution, departFile);
        arrivalDistribution = setDistribution(arrivalDistribution, arriveFile);
    
        // calculate total number of parties
        float enplanements = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".enplanements").replace(",", ""));
        float connectingPassengers = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".connecting").replace(",", ""));
        float annualFactor = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".annualizationFactor"));
        float averageSize = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport."+airportCode+".averageSize"));

        
        avShare = Util.getFloatValueFromPropertyMap(rbMap, "Mobility.AV.Share");
        
        float directPassengers = (enplanements - connectingPassengers) / annualFactor;
        int totalParties = (int) (directPassengers / averageSize) * 2;
        parties = new AirportParty[(int)(totalParties*sampleRate)];

        logger.info("Total airport parties: " + totalParties);
    }

    /**
     * Create parties based upon total parties (calculated in constructor). Fill
     * parties[] with travel parties, assuming one-half are arriving and
     * one-half are departing. Simulate party characteristics (income, size,
     * duration, time departing from origin or arriving at airport) from
     * distributions, also read in during constructor.
     * 
     */
    public void generateAirportParties()
    {

        int departures = parties.length / 2;
        int arrivals = parties.length - departures;
        int totalParties = 0;
        int totalPassengers = 0;
        for (int i = 0; i < departures; ++i)
        {

            AirportParty party = new AirportParty(i * 101 + 1000);

            // simulate from distributions
            party.setDirection(AirportModelStructure.DEPARTURE);
            byte purpose = (byte) choosePurpose(party.getRandom());
            byte size = (byte) chooseFromDistribution(purpose, sizeDistribution, party.getRandom());
            byte nights = (byte) chooseFromDistribution(purpose, durationDistribution,
                    party.getRandom());
            byte income = (byte) chooseFromDistribution(purpose, incomeDistribution,
                    party.getRandom());
            byte period = (byte) chooseFromDistribution(purpose, departureDistribution,
                    party.getRandom());
            
             if(party.getRandom()<avShare)
            	party.setAvAvailable(true);
             else 
            	party.setAvAvailable(false);

            party.setPurpose(purpose);
            party.setSize(size);
            party.setNights(nights);
            party.setIncome(income);
            party.setDepartTime(period);

            parties[totalParties] = party;
            ++totalParties;
            totalPassengers += size;
            party.setID(totalParties);
        }

        for (int i = 0; i < arrivals; ++i)
        {

            AirportParty party = new AirportParty(i * 101 + 1000);

            // simulate from distributions
            party.setDirection(AirportModelStructure.ARRIVAL);
            byte purpose = (byte) choosePurpose(party.getRandom());
            byte size = (byte) chooseFromDistribution(purpose, sizeDistribution, party.getRandom());
            byte nights = (byte) chooseFromDistribution(purpose, durationDistribution,
                    party.getRandom());
            byte income = (byte) chooseFromDistribution(purpose, incomeDistribution,
                    party.getRandom());
            byte period = (byte) chooseFromDistribution(purpose, arrivalDistribution,
                    party.getRandom());

            party.setPurpose(purpose);
            party.setSize(size);
            party.setNights(nights);
            party.setIncome(income);
            party.setDepartTime(period);

            parties[totalParties] = party;
            ++totalParties;
            party.setID(totalParties);
            totalPassengers += size;
        }

        logger.info("Total passengers " + totalPassengers);

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
    protected void setPurposeDistribution(String fileName)
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

        int purposes = AirportModelStructure.PURPOSES;
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
    }

    /**
     * Read the probabilities in the file, convert to a cumulative probability
     * distribution, initialize the array, and store in the array.
     * 
     * This method is used for files that contain 2-dimensional probabilities by
     * airport purpose and some other dimension, such as income, party size, or
     * departure time.
     * 
     * @param probArray
     *            The array to store the cumulative probabilities in
     * @param fileName
     *            The name of the file containing the probability distribution.
     */
    protected double[][] setDistribution(double[][] probArray, String fileName)
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

        int rows = probabilityTable.getRowCount();
        int cols = probabilityTable.getColumnCount();

        int purposes = AirportModelStructure.PURPOSES;
        // check to make sure that there is one column for each purpose
        if (cols < (purposes + 1))
        {
            logger.fatal("Error:  Number of columns in " + fileName + " is less than " + (purposes
                    + 1));
            throw new RuntimeException();
        }

        // initialize the cumulative probability array
        probArray = new double[rows][purposes];

        // calculate and store cumulative probability distribution
        for (int purp = 0; purp < purposes; ++purp)
        {

            int col = probabilityTable.getColumnPosition("purp" + purp + "_perc");
            double total_prob = 0.0;
            for (int row = 0; row < rows; ++row)
            {

                double probability = probabilityTable.getValueAt(row + 1, col);
                total_prob += probability;

                probArray[row][purp] = total_prob;

            }
        }
        logger.info("End storing cumulative probabilies from file " + fileName);
        return probArray;
    }

    /**
     * Choose an alternative from one of the distributions.
     * 
     * @param purpose
     *            The airport model purpose, used to index into probability
     *            array.
     * @param probs
     *            A 2-dimensional array of cumulative probabilities, where the
     *            first dimension is number of alternative (0-based) and second
     *            column is airport model purpose.
     * @param random
     *            A uniform random number
     * @return The number of the alternative
     */
    protected int chooseFromDistribution(int purpose, double[][] probs, double random)
    {
        int alts = probs.length;

        // iterate through the probability array and choose
        for (int alt = 0; alt < alts; ++alt)
        {
            if (probs[alt][purpose] > random) return alt;
        }
        return -99;
    }

    /**
     * Choose a purpose.
     * 
     * @param random
     *            A uniform random number.
     * @return the purpose.
     */
    protected int choosePurpose(double random)
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
        String fileName = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport."+airportCode+".output.file");

        PrintWriter writer = null;
        try
        {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + fileName + " for writing\n");
            throw new RuntimeException();
        }
        String headerString = new String(
                "id,direction,purpose,size,income,nights,departTime,originMGRA,destinationMGRA,originTAZ,"
                + "destinationTAZ,tripMode,av_avail,arrivalMode,boardingTAP,alightingTAP,set,valueOfTime\n");
        writer.print(headerString);

        // Iterate through the array, printing records to the file
        for (int i = 0; i < parties.length; ++i)
        {

             String record = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                    + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                    + parties[i].getIncome() + "," + parties[i].getNights() + ","
                    + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                    + parties[i].getDestinationMGRA() + "," 
                    + parties[i].getOriginTAZ() + "," + parties[i].getDestinationTAZ() + ","
                    + parties[i].getMode() + ","
                    + (parties[i].getAvAvailable() ? 1 : 0) + ","
                    + parties[i].getArrivalMode() + "," + parties[i].getBoardTap() + "," + 
                    + parties[i].getAlightTap() + "," + parties[i].getSet() + "," + 
                    String.format("%9.2f", parties[i].getValueOfTime()) + "\n");
            writer.print(record);
        }
        writer.close();

    }

    /**
     * @return the parties
     */
    public AirportParty[] getParties()
    {
        return parties;
    }
    

  /*
    public static void main(String[] args)
    {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Activity Based Model using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Running Airport Model Party Manager"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
        AirportPartyManager apm = new AirportPartyManager(pMap);

        apm.generateAirportParties();

        apm.writeOutputFile(pMap);

        logger.info("Airport Model successfully completed!");

    }
*/
}
