package org.sandag.abm.airport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.io.FileNotFoundException;

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
    private int 		   totalPassengerParty;

    private double[]       purposeDistribution;
    private double[][]     sizeDistribution;
    private double[][]     durationDistribution;
    private double[][]     incomeDistribution;
    private double[][]     departureDistribution;
    private double[][]     arrivalDistribution;
    
    private int			   airportMgra;

    SandagModelStructure   sandagStructure;
    
    private Map<String, List<Double>> 		  employeeParkingValuesMap = new HashMap<>();

    /**
     * Constructor. Reads properties file and opens/stores all probability
     * distributions for sampling. Estimates number of airport travel parties
     * and initializes parties[].
     * 
     * @param resourceFile
     *            Property file.
     */
    public AirportPartyManager(HashMap<String, String> rbMap, float sampleRate)
    {
        sandagStructure = new SandagModelStructure();

        String directory = Util.getStringValueFromPropertyMap(rbMap, "Project.Directory");
        String purposeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.purpose.file");
        String sizeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.size.file");
        String durationFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.duration.file");
        String incomeFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.income.file");
        String departFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.departureTime.file");
        String arriveFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.arrivalTime.file");
        String employeeParkFile = directory
                + Util.getStringValueFromPropertyMap(rbMap, "airport.employeePark.file");

        // Read the distributions
        setPurposeDistribution(purposeFile);
        sizeDistribution = setDistribution(sizeDistribution, sizeFile);
        durationDistribution = setDistribution(durationDistribution, durationFile);
        incomeDistribution = setDistribution(incomeDistribution, incomeFile);
        departureDistribution = setDistribution(departureDistribution, departFile);
        arrivalDistribution = setDistribution(arrivalDistribution, arriveFile);
        
        readCsvFile(employeeParkFile);

        // calculate total number of parties
        float enplanements = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport.enplanements").replace(",", ""));
        float connectingPassengers = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport.connecting").replace(",", ""));
        float annualFactor = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport.annualizationFactor"));
        float averageSize = new Float(Util.getStringValueFromPropertyMap(rbMap,
                "airport.averageSize"));
        
        airportMgra = Util.getIntegerValueFromPropertyMap(rbMap,
                "airport.airportMgra");
        
        int totalEmployees = 0;
        for ( String key : employeeParkingValuesMap.keySet()) {
			totalEmployees += employeeParkingValuesMap.get(key).get(2);
		}

        float directPassengers = (enplanements - connectingPassengers) / annualFactor;
        int totalParties = (int) (directPassengers / averageSize) * 2;
        parties = new AirportParty[(int)(totalParties*sampleRate) + totalEmployees * 2];
        totalPassengerParty = (int) (totalParties*sampleRate);

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

        int departures = totalPassengerParty / 2;
        int arrivals = departures;
        int employees = (parties.length - departures - arrivals) / 2;
        int totalParties = 0;
        int totalPassengers = 0;
        int totalEmployees = 0;
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
        
        for (String key : employeeParkingValuesMap.keySet())
        	{
        		double num = employeeParkingValuesMap.get(key).get(2);
        		int stallNum = (int) num;
        		for (int s = 0; s < stallNum; s++)
        		{
        			AirportParty party = new AirportParty(s*101 + 1000);
        			double stallMgra = employeeParkingValuesMap.get(key).get(1);
        			party.setOriginMGRA((int) stallMgra);
        			party.setDestinationMGRA(airportMgra);
        			
        			double transitToTerminalProb = employeeParkingValuesMap.get(key).get(3) * employeeParkingValuesMap.get(key).get(4);
        			if (party.getRandom() < transitToTerminalProb)
        			{
        				party.setMode((byte) SandagModelStructure.WALK_TRANSIT_ALTS[0]);
        			}
        			else
        			{
        				party.setMode((byte) SandagModelStructure.WALK_ALTS[0]);
        			}    
        			// simulate from distributions
                	byte period = (byte) chooseFromDistribution(AirportModelStructure.EMPLOYEE, departureDistribution,
                             party.getRandom());
                	 
                	party.setDirection(AirportModelStructure.DEPARTURE);
                	party.setPurpose(AirportModelStructure.EMPLOYEE);
                	party.setSize((byte) 1);
                	party.setNights((byte) -99);
                	party.setIncome((byte) -99);
                	party.setDepartTime(period);
                	party.setArrivalMode((byte) -99);
                	
                	parties[totalParties] = party;
                    ++totalParties;
                    party.setID(totalParties);
                    totalEmployees += 1;
        		}
        	}
        	
        
        for (String key : employeeParkingValuesMap.keySet())
        	{
        		double num = employeeParkingValuesMap.get(key).get(2);
        		int stallNum = (int) num;
        		for (int s = 0; s < stallNum; s++)
        		{
        			AirportParty party = new AirportParty(s*101 + 1001);
        			double stallMgra = employeeParkingValuesMap.get(key).get(1);
        			party.setDestinationMGRA((int) stallMgra);
        			party.setOriginMGRA(airportMgra);
        			
        			double transitToTerminalProb = employeeParkingValuesMap.get(key).get(3) * employeeParkingValuesMap.get(key).get(4);
        			if (party.getRandom() < transitToTerminalProb)
        			{
        				party.setMode((byte) SandagModelStructure.WALK_TRANSIT_ALTS[0]);
        			}	 
        			else
        			{
        				party.setMode((byte) SandagModelStructure.WALK_ALTS[0]);
        			}
        			// simulate from distributions
                	byte period = (byte) chooseFromDistribution(AirportModelStructure.EMPLOYEE, arrivalDistribution,
                             party.getRandom());
                	 
                	party.setDirection(AirportModelStructure.ARRIVAL);
                	party.setPurpose(AirportModelStructure.EMPLOYEE);
                	party.setSize((byte) 1);
                	party.setNights((byte) -99);
                	party.setIncome((byte) -99);
                	party.setDepartTime(period);
                	party.setArrivalMode((byte) -99);
                	
                	parties[totalParties] = party;
                    ++totalParties;
                    party.setID(totalParties);
                    totalEmployees += 0;
        		}
        	}
        logger.info("Total employees " + totalEmployees);

    }
    
    private void readCsvFile(String filePath) {
		
    	//Map<String, List<Double>> employeeParkingValuesMap = new HashMap<>();
    	
		String employeeParkIndexString = "Name";
		
		try (Scanner sc = new Scanner(new File(filePath))) {
			
			String[] record;

			// process the header record
			record = sc.nextLine().split(",", -1);
			Map<String, Integer> fieldIndexMap;
			fieldIndexMap = getFieldIndexMap( record );

			int employeeParkingIndex = fieldIndexMap.get( employeeParkIndexString );
			
			while (sc.hasNextLine()) 
			{
				
				record = sc.nextLine().split(",", -1);
				String employeeParkingName = record[employeeParkingIndex];
				
				// pre-allocate the ArrayList to hold values for this MAZ
				List<Double> valueList = new ArrayList<>();
				while (valueList.size() < fieldIndexMap.size())
					valueList.add(null);
				
				// convert the record[] values to double for each specified field value and save in the ArrayList.
				for ( String field : fieldIndexMap.keySet()) {
					int index = fieldIndexMap.get( field );
						valueList.set( index, Double.valueOf( record[index] ) ); 
					}
			
				employeeParkingValuesMap.put( employeeParkingName, valueList );

			}
		}
		catch (FileNotFoundException e) {
			throw new RuntimeException( "Exception caught reading csv file: " + filePath, e );
		}
		
	}

	/**
	 * @param record String[] of field names read from the csv file header record.
	 * @return Map<String, Integer> map of field names to field indices.
	 */
	private Map<String, Integer> getFieldIndexMap( String[] record ) {
		Map<String, Integer> fldIdxMap = new HashMap<>();
		for ( int i=0; i < record.length; i++ )
			fldIdxMap.put(record[i], i);
		return fldIdxMap;
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
                + Util.getStringValueFromPropertyMap(rbMap, "airport.output.file");

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
                "id,direction,purpose,size,income,nights,departTime,originMGRA,destinationMGRA,tripMode,arrivalMode,boardingTAP,alightingTAP\n");
        writer.print(headerString);

        // Iterate through the array, printing records to the file
        for (int i = 0; i < parties.length; ++i)
        {

        	// if employee
            if (parties[i].getPurpose() == AirportModelStructure.EMPLOYEE)
            {
            	String record = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                        + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                        + parties[i].getIncome() + "," + parties[i].getNights() + ","
                        + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                        + parties[i].getDestinationMGRA() + "," + parties[i].getMode() + ","
                        + parties[i].getArrivalMode() + "," + (int) 0 + "," + (int) 0 + "\n");
                
                writer.print(record);
                
                continue;
            }
            
            int[] taps = getTapPair(parties[i]);
            
            int airportAccessMgra = parties[i].getAirportAccessMGRA();
            int accMode_null = -99;
                     
            // if the arrival mode access point is transit, or it's an external trip
            if (airportAccessMgra <= 0)
            {
            	String record = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                        + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                        + parties[i].getIncome() + "," + parties[i].getNights() + ","
                        + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                        + parties[i].getDestinationMGRA() + "," + parties[i].getMode() + ","
                        + parties[i].getArrivalMode() + "," + taps[0] + "," + taps[1] + "\n");
                
                writer.print(record);
                
                continue;
            }
            
            // if the arrival mode access point is not transit, two trip legs will be printed out
            else
            {
            	String record_Origin2Access = new String();
            	String record_Access2Destination = new String();
            	
            	if (parties[i].getDirection() == AirportModelStructure.DEPARTURE)
            	{
            		record_Origin2Access = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                            + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                            + parties[i].getIncome() + "," + parties[i].getNights() + ","
                            + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                            + parties[i].getAirportAccessMGRA() + "," + parties[i].getMode() + ","
                            + parties[i].getArrivalMode() + "," + taps[0] + "," + taps[1] + "\n");
            		
            		// if access point is airport terminal, connection mode is walk
            		if (airportAccessMgra == airportMgra)
            		{     			
            			record_Access2Destination = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                                + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                                + parties[i].getIncome() + "," + parties[i].getNights() + ","
                                + parties[i].getDepartTime() + "," + parties[i].getAirportAccessMGRA() + ","
                                + parties[i].getDestinationMGRA() + "," + SandagModelStructure.WALK_ALTS[0] + ","
                                + accMode_null + "," + taps[0] + "," + taps[1] + "\n");
            		}
            		// else if access point is not airport terminal, connection mode is transit (APM)
            		else
            		{
            			record_Access2Destination = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                                + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                                + parties[i].getIncome() + "," + parties[i].getNights() + ","
                                + parties[i].getDepartTime() + "," + parties[i].getAirportAccessMGRA() + ","
                                + parties[i].getDestinationMGRA() + "," + SandagModelStructure.WALK_TRANSIT_ALTS[0] + ","
                                + accMode_null + "," + taps[0] + "," + taps[1] + "\n");
            		}
            		writer.print(record_Origin2Access);
            		writer.print(record_Access2Destination);
            	}
            	else
            	{
            		record_Access2Destination = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                            + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                            + parties[i].getIncome() + "," + parties[i].getNights() + ","
                            + parties[i].getDepartTime() + "," + parties[i].getAirportAccessMGRA() + ","
                            + parties[i].getDestinationMGRA() + "," + parties[i].getMode() + ","
                            + parties[i].getArrivalMode() + "," + taps[0] + "," + taps[1] + "\n");
            		// if access point is airport terminal, connection mode is walk
            		if (airportAccessMgra == airportMgra)
            		{     			
            			record_Origin2Access = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                                + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                                + parties[i].getIncome() + "," + parties[i].getNights() + ","
                                + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                                + parties[i].getAirportAccessMGRA() + "," + SandagModelStructure.WALK_ALTS[0] + ","
                                + accMode_null + "," + taps[0] + "," + taps[1] + "\n");
            		}
            		// else if access point is not airport terminal, connection mode is transit (APM)
            		else
            		{
            			record_Origin2Access = new String(parties[i].getID() + "," + parties[i].getDirection() + ","
                                + parties[i].getPurpose() + "," + parties[i].getSize() + ","
                                + parties[i].getIncome() + "," + parties[i].getNights() + ","
                                + parties[i].getDepartTime() + "," + parties[i].getOriginMGRA() + ","
                                + parties[i].getAirportAccessMGRA() + "," + SandagModelStructure.WALK_TRANSIT_ALTS[0] + ","
                                + accMode_null + "," + taps[0] + "," + taps[1] + "\n");
            		}
            		writer.print(record_Origin2Access);
            		writer.print(record_Access2Destination);
            	}
            }

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

    /**
     * A helper method that returns an array containing boarding tap (element 0)
     * and alighting tap (element 1) for the given trip mode. Returns an array
     * of zeroes if the trip modes are not transit.
     * 
     * @param party
     *            The airport travel party
     * @return An array containing boarding TAP and alighting TAP
     */
    public int[] getTapPair(AirportParty party)
    {

        int[] taps = new int[2];

        // ride mode will be -1 if not transit
        int rideMode = sandagStructure.getRideModeIndexForTripMode(party.getMode());

        int tripMode = party.getMode();

        if (sandagStructure.getTripModeIsWalkTransit(tripMode)) taps = party
                .getWtwTapPair(rideMode);
        else if (sandagStructure.getTripModeIsKnrTransit(tripMode))
            if (party.getDirection() == AirportModelStructure.ARRIVAL) taps = party
                    .getWtdTapPair(rideMode);
            else taps = party.getDtwTapPair(rideMode);

        return taps;
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
