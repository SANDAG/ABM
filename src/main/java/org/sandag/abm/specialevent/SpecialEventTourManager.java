package org.sandag.abm.specialevent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Util;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;

public class SpecialEventTourManager {

	private TableDataSet eventData;
	private TableDataSet incomeData;
	private TableDataSet partySizeData;
	private HashMap<String, String> rbMap;
	private static Logger logger = Logger.getLogger("specialEventModel");

	private SpecialEventTour[] tours;
	private MersenneTwister randomGenerator;
	private SandagModelStructure sandagStructure;
	private boolean saveUtilsAndProbs;

	/**
	 * Default Constructor.
	 */
	public SpecialEventTourManager(HashMap<String, String> rbMap,
			TableDataSet eventData) {
		this.rbMap = rbMap;
		randomGenerator = new MersenneTwister(10001);
		saveUtilsAndProbs = Util.getBooleanValueFromPropertyMap(rbMap,
				"specialEvent.saveUtilsAndProbs");
		this.eventData = eventData;
		sandagStructure = new SandagModelStructure();

	}

	/**
	 * Generate special event tours.
	 */
	public void generateTours() {

		// read the party size data
		String partySizeFile = Util.getStringValueFromPropertyMap(rbMap,
				"specialEvent.partySize.file");
		String directory = Util.getStringValueFromPropertyMap(rbMap,
				"Project.Directory");
		partySizeFile = directory + partySizeFile;
		partySizeData = readFile(partySizeFile);

		// read the income data
		String incomeFile = Util.getStringValueFromPropertyMap(rbMap,
				"specialEvent.income.file");
		incomeFile = directory + incomeFile;
		incomeData = readFile(incomeFile);

		ArrayList<SpecialEventTour> eventTourList = new ArrayList<SpecialEventTour>();

		int eventTours = 0;
		for (int i = 1; i <= eventData.getRowCount(); ++i) {

			int eventMgra = (int) eventData.getValueAt(i, "MGRA");
			int attendance = (int) eventData.getValueAt(i, "Attendance");
			String eventType = eventData.getStringValueAt(i, "EventType");
			int startPeriod = (int) eventData.getValueAt(i, "StartPeriod");
			int endPeriod = (int) eventData.getValueAt(i, "EndPeriod");

			// generate tours for the event
			for (int j = 0; j < attendance; ++j) {

				++eventTours;

				long randomSeed = getRandomSeed(eventTours);
				SpecialEventTour tour = new SpecialEventTour(randomSeed);
				tour.setID(eventTours);
				tour.setEventNumber((byte) i);
				tour.setDestinationMGRA(eventMgra);
				tour.setDepartTime(startPeriod);
				tour.setArriveTime(endPeriod);
				tour.setEventType(eventType);

				// choose income and party size for the tour
				int income = chooseIncome(randomGenerator.nextDouble(),
						eventType);
				int partySize = choosePartySize(randomGenerator.nextDouble(),
						eventType);

				tour.setIncome(income);
				tour.setPartySize(partySize);

				eventTourList.add(tour);
			}
		}

		// convert the ArrayList to an array
		tours = new SpecialEventTour[eventTourList.size()];
		for (int i = 0; i < tours.length; ++i)
			tours[i] = eventTourList.get(i);

	}

	/**
	 * Simulate income from the income data table.
	 * 
	 * @param random
	 *            a uniformly-distributed random number.
	 * @param eventType
	 *            a string identifying the type of event, which should be a
	 *            column in the income data table
	 * @return income chosen
	 */
	public int chooseIncome(double random, String eventType) {

		int income = -1;
		double cumProb = 0;
		for (int i = 1; i <= incomeData.getRowCount(); ++i) {
			cumProb += incomeData.getValueAt(i, eventType);
			if (random < cumProb) {
				income = (int) incomeData.getValueAt(i, "Income");
				break;
			}
		}
		return income;
	}

	/**
	 * Simulate party size from the party size data table.
	 * 
	 * @param random
	 *            a uniformly-distributed random number.
	 * @param eventType
	 *            a string identifying the type of event, which should be a
	 *            column in the party size data table
	 * @return party size chosen
	 */
	public int choosePartySize(double random, String eventType) {

		int partySize = -1;
		double cumProb = 0;
		for (int i = 1; i <= partySizeData.getRowCount(); ++i) {
			cumProb += partySizeData.getValueAt(i, eventType);
			if (random < cumProb) {
				partySize = (int) partySizeData.getValueAt(i, "PartySize");
				break;
			}
		}
		return partySize;
	}

	/**
	 * Read the file and return the TableDataSet.
	 * 
	 * @param fileName
	 * @return data
	 */
	private TableDataSet readFile(String fileName) {

		logger.info("Begin reading the data in file " + fileName);
		TableDataSet data;
		try {
			OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
			data = csvFile.readFile(new File(fileName));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		logger.info("End reading the data in file " + fileName);
		return data;
	}

	/**
	 * Create a text file and write all records to the file.
	 * 
	 */
	public void writeOutputFile(HashMap<String, String> rbMap) {

		// Open file and print header

		String directory = Util.getStringValueFromPropertyMap(rbMap,
				"Project.Directory");
		String tourFileName = directory
				+ Util.getStringValueFromPropertyMap(rbMap,
						"specialEvent.tour.output.file");
		String tripFileName = directory
				+ Util.getStringValueFromPropertyMap(rbMap,
						"specialEvent.trip.output.file");

		logger.info("Writing special event tours to file " + tourFileName);
		logger.info("Writing special event trips to file " + tripFileName);

		PrintWriter tourWriter = null;
		try {
			tourWriter = new PrintWriter(new BufferedWriter(new FileWriter(
					tourFileName)));
		} catch (IOException e) {
			logger.fatal("Could not open file " + tourFileName
					+ " for writing\n");
			throw new RuntimeException();
		}
		String tourHeaderString = new String(
				"id,eventNumber,eventType,income,partySize,departTime,arriveTime,originMGRA,destinationMGRA,tourMode\n");
		tourWriter.print(tourHeaderString);

		PrintWriter tripWriter = null;
		try {
			tripWriter = new PrintWriter(new BufferedWriter(new FileWriter(
					tripFileName)));
		} catch (IOException e) {
			logger.fatal("Could not open file " + tripFileName
					+ " for writing\n");
			throw new RuntimeException();
		}
		String tripHeaderString = new String(
				"tourID,tripID,originMGRA,destinationMGRA,inbound,originIsTourDestination,destinationIsTourDestination,period,tripMode,boardingTap,alightingTap");

		// Iterate through the array, printing records to the file
		for (int i = 0; i < tours.length; ++i) {

			SpecialEventTour tour = tours[i];

			SpecialEventTrip[] trips = tours[i].getTrips();

			if (trips == null)
				continue;

			writeTour(tour, tourWriter);

			// if this is the first record, and we are saving utils and probs,
			// append a line to the trip file header
			if (i == 0) {

				if (saveUtilsAndProbs) {
					float[] utils = trips[0].getModeUtilities();
					String header = "";
					for (int j = 0; j < utils.length; ++j)
						header += ",util_" + j;
					for (int j = 0; j < utils.length; ++j)
						header += ",prob_" + j;
					tripWriter.print(tripHeaderString + header + "\n");
				} else
					tripWriter.print(tripHeaderString + "\n");
			}
			for (int j = 0; j < trips.length; ++j) {
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
	private void writeTour(SpecialEventTour tour, PrintWriter writer) {
		String record = new String(tour.getID() + "," + tour.getEventNumber()
				+ "," + tour.getEventType() + "," + tour.getIncome() + ","
				+ tour.getPartySize() + "," + tour.getDepartTime() + ","
				+ tour.getArriveTime() + "," + tour.getOriginMGRA() + ","
				+ tour.getDestinationMGRA() + "," + tour.getTourMode() + "\n");
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
	private void writeTrip(SpecialEventTour tour, SpecialEventTrip trip,
			int tripNumber, PrintWriter writer) {

		int[] taps = getTapPair(trip);

		String record = new String(tour.getID() + "," + tripNumber + ","
				+ trip.getOriginMgra() + "," + trip.getDestinationMgra() + ","
				+ trip.isInbound() + "," + trip.isOriginIsTourDestination()
				+ "," + trip.isDestinationIsTourDestination() + ","
				+ trip.getPeriod() + "," + trip.getTripMode() + "," + taps[0]
				+ "," + taps[1]);

		if (saveUtilsAndProbs) {

			String utilRecord = new String();
			float[] utils = trip.getModeUtilities();
			for (int i = 0; i < utils.length; ++i)
				utilRecord += ("," + String.format("%9.5f", utils[i]));
			float[] probs = trip.getModeProbabilities();
			for (int i = 0; i < probs.length; ++i)
				utilRecord += ("," + String.format("%9.5f", probs[i]));
			record = record + utilRecord;
		}
		writer.print(record + "\n");
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
	public int[] getTapPair(SpecialEventTrip trip) {

		int[] taps = new int[2];

		// ride mode will be -1 if not transit
		int tripMode = trip.getTripMode();
		int rideMode = sandagStructure.getRideModeIndexForTripMode(tripMode);

		if (sandagStructure.getTripModeIsWalkTransit(tripMode))
			taps = trip.getWtwTapPair(rideMode);
		else if (sandagStructure.getTripModeIsPnrTransit(tripMode))
			if (trip.isInbound())
				taps = trip.getWtdTapPair(rideMode);
			else
				taps = trip.getDtwTapPair(rideMode);
		else if (sandagStructure.getTripModeIsKnrTransit(tripMode))
			if (trip.isInbound())
				taps = trip.getWtdTapPair(rideMode);
			else
				taps = trip.getDtwTapPair(rideMode);

		return taps;
	}

	/**
	 * get special event tours.
	 * 
	 * @return
	 */
	public SpecialEventTour[] getTours() {
		return tours;
	}

	/**
	 * Calculate and return a random number seed for the tour.
	 * 
	 * @param eventID
	 * @return
	 */
	public long getRandomSeed(int eventID) {

		long seed = (eventID * 10 + 100001);
		return seed;
	}

}
