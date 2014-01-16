package org.sandag.abm.application;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.summit.ConcreteSummitRecord;
import com.pb.common.summit.SummitHeader;
import com.pb.common.summit.SummitRecordTable;
import com.pb.common.util.ResourceUtil;

public class SandagSummitFile
{

    private static Logger          logger       = Logger.getLogger(SandagSummitFile.class);

    private HashMap<Long, Integer> personsOver18;
    private HashMap<Long, Integer> autosOwned;

    // Summit record table (one per file)
    private SummitRecordTable      summitRecordTable;

    private TableDataSet           tourData;

    // Some parameters
    private int                    modes;
    private int                    upperEA;                                                // Upper
                                                                                            // limit
                                                                                            // on
                                                                                            // time
                                                                                            // of
                                                                                            // day
                                                                                            // for
                                                                                            // the
                                                                                            // Early
                                                                                            // morning
                                                                                            // time
                                                                                            // period
    private int                    upperAM;                                                // Upper
                                                                                            // limit
                                                                                            // on
                                                                                            // time
                                                                                            // of
                                                                                            // day
                                                                                            // for
                                                                                            // the
                                                                                            // AM
                                                                                            // peak
                                                                                            // time
                                                                                            // period
    private int                    upperMD;                                                // Upper
                                                                                            // limit
                                                                                            // on
                                                                                            // time
                                                                                            // of
                                                                                            // day
                                                                                            // for
                                                                                            // the
                                                                                            // Midday
                                                                                            // time
                                                                                            // period
    private int                    upperPM;                                                // Upper
                                                                                            // limit
                                                                                            // on
                                                                                            // time
                                                                                            // of
                                                                                            // day
                                                                                            // for
                                                                                            // the
                                                                                            // PM
                                                                                            // time
                                                                                            // peak
                                                                                            // period
    private int[]                  walkTransitModes;
    private int[]                  driveTransitModes;

    // an array of file numbers, one per purpose
    private int[]                  fileNumber;
    private int                    numberOfFiles;

    private static final String[]  PURPOSE_NAME = {"Work", "University", "School", "Escort",
            "Shop", "Maintenance", "EatingOut", "Visiting", "Discretionary", "WorkBased"};
    private float[]                ivtCoeff;
    private String[]               fileName;

    ResourceBundle                 rb;
    MgraDataManager                mdm;
    TazDataManager                 tdm;

    public SandagSummitFile(String resourceFile)
    {

        rb = ResourceUtil.getPropertyBundle(new File(resourceFile));
        mdm = MgraDataManager.getInstance(ResourceUtil.changeResourceBundleIntoHashMap(rb));
        tdm = TazDataManager.getInstance(ResourceUtil.changeResourceBundleIntoHashMap(rb));

        // Time period limits
        upperEA = Integer.valueOf(rb.getString("summit.upperEA"));
        upperAM = Integer.valueOf(rb.getString("summit.upperAM"));
        upperMD = Integer.valueOf(rb.getString("summit.upperMD"));
        upperPM = Integer.valueOf(rb.getString("summit.upperPM"));

        // Find what file to store each purpose in
        numberOfFiles = 0;
        fileNumber = new int[PURPOSE_NAME.length];
        for (int i = 0; i < PURPOSE_NAME.length; ++i)
        {
            String fileString = "summit.purpose." + PURPOSE_NAME[i];
            fileNumber[i] = Integer.valueOf(rb.getString(fileString)) - 1;
            numberOfFiles = Math.max(fileNumber[i] + 1, numberOfFiles);
        }

        // Get the name of each file
        fileName = new String[numberOfFiles];
        for (int i = 0; i < numberOfFiles; ++i)
        {
            String nameString = "summit.filename." + (i + 1);
            fileName[i] = rb.getString(nameString);
        }

        // Get the ivt coefficients for each file
        ivtCoeff = new float[numberOfFiles];
        for (int i = 0; i < numberOfFiles; ++i)
        {
            String ivtString = "summit.ivt.file." + (i + 1);
            ivtCoeff[i] = Float.valueOf(rb.getString(ivtString));
        }

        // set the arrays
        modes = Integer.valueOf(rb.getString("summit.modes"));
        walkTransitModes = new int[modes];
        driveTransitModes = new int[modes];

        String modeArray = rb.getString("summit.mode.array").replace(" ", "");
        StringTokenizer inToken = new StringTokenizer(modeArray, ",");
        int mode = 0;
        while (inToken.hasMoreElements())
        {
            int modeValue = Integer.valueOf(inToken.nextToken());
            logger.info("Mode " + mode + " value " + modeValue);
            if (modeValue == 1) walkTransitModes[mode] = 1;
            else if (modeValue == 2) driveTransitModes[mode] = 1;

            ++mode;
        }

    }

    /**
     * Create Summit files for all purposes and both individual and joint tour
     * files.
     * 
     */
    public void createSummitFiles()
    {

        // Read the household file
        String directory = rb.getString("Project.Directory");
        String hhFile = rb.getString("Results.HouseholdDataFile");
        readHouseholdFile(directory + hhFile);

        // Read the person file
        String perFile = rb.getString("Results.PersonDataFile");
        readPersonFile(directory + perFile);

        // Open the individual tour file and start processing
        String tourFile = rb.getString("Results.IndivTourDataFile");
        openTourFile(directory + tourFile);

        String outputDirectory = rb.getString("summit.output.directory");

        for (int i = 0; i < getNumberOfFiles(); ++i)
        {

            // Create the summit table
            createSummitFile(i);

            // Write the summit output file
            String purpose = getPurpose(i);
            writeFile(outputDirectory + purpose + ".bin", i);

        }

        // Open the joint tour file and start processing
        tourFile = rb.getString("Results.JointTourDataFile");
        openTourFile(directory + tourFile);

        for (int i = 0; i < getNumberOfFiles(); ++i)
        {

            // Create the summit table
            createSummitFile(i);

            // Write the summit output file
            String purpose = getPurpose(i);
            writeFile(outputDirectory + "jnt_" + purpose + ".bin", i);

        }

    }

    /**
     * Get the number of SUMMIT Files as set in the properties file.
     * 
     * @return The number of SUMMIT files.
     */
    public int getNumberOfFiles()
    {
        return numberOfFiles;
    }

    /**
     * Read household records and store autos owned.
     * 
     * @param fileName
     *            household file path/name.
     */
    public void readHouseholdFile(String fileName)
    {

        autosOwned = new HashMap<Long, Integer>();

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
            long hhID = (long) hhData.getValueAt(i, "hh_ID");
            int autos = (int) hhData.getValueAt(i, "autos");
            autosOwned.put(hhID, autos);
        }
        logger.info("End reading the data in file " + fileName);
    }

    /**
     * Read person file and store persons >= 18.
     * 
     * @param fileName
     *            Person file path/name.
     */
    public void readPersonFile(String fileName)
    {
        personsOver18 = new HashMap<Long, Integer>();

        logger.info("Begin reading the data in file " + fileName);

        TableDataSet personData;
        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            personData = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // iterate through the table and save number of persons>=18
        int personCount = 0;
        long hhID_last = -99;
        for (int i = 1; i <= personData.getRowCount(); ++i)
        {
            long hhID = (long) personData.getValueAt(i, "hh_ID");
            int age = (int) personData.getValueAt(i, "age");

            // this record is a new household
            if (hhID != hhID_last && i > 1)
            {
                personsOver18.put(hhID_last, personCount);
                personCount = 0;
            }

            if (age >= 18) ++personCount;

            hhID_last = hhID;
        }
        // save the last household
        personsOver18.put(hhID_last, personCount);

        logger.info("End reading the data in file " + fileName);

    }

    /**
     * Open a tour file for subsequent reading.
     */
    public void openTourFile(String fileName)
    {

        logger.info("Begin reading the data in file " + fileName);

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            tourData = csvFile.readFile(new File(fileName));
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);
    }

    /**
     * This is the main workhorse method in this class. It creates a
     * SummitRecordTable, and then iterates over records in the tour file. If
     * the tour purpose for the record is mapped to the fileNumber argument, a
     * ConcreteSummitRecord is created and the attributes are set, and the
     * record is added to the SummitRecordTable. After all tour records have
     * been read, the table is finalized.
     * 
     * After this method is run, the file header information can be set and the
     * SummitRecordTable can be written to a SUMMIT file.
     * 
     * @param fileNumber
     */
    public void createSummitFile(int fileNumber)
    {

        String purpose = getPurpose(fileNumber);
        logger.info("Begin creating SUMMIT record table for purpose " + purpose);

        // get column for start of utilities, probabilities
        int start_util = tourData.getColumnPosition("util_1");
        int start_prob = tourData.getColumnPosition("prob_1");

        boolean jointTour = tourData.containsColumn("tour_participants");
        int participantsCol = 0;
        if (jointTour)
        {
            participantsCol = tourData.getColumnPosition("tour_participants");
        }

        // arrays of utilities, probabilities
        float[] util = new float[modes];
        float[] prob = new float[modes];

        // Instantiate a new SummitRecordTable
        summitRecordTable = new SummitRecordTable();

        // iterate through the tour data and save summit record tables
        for (int i = 1; i <= tourData.getRowCount(); ++i)
        {

            if (i <= 5 || i % 1000 == 0) logger.info("Reading record " + i);

            String tourPurpose = tourData.getStringValueAt(i, "tour_purpose");
            int tableNumber = calculateSummitTable(tourPurpose);

            if (tableNumber != fileNumber) continue;

            long hhID = (long) tourData.getValueAt(i, "hh_id");

            int originMGRA = (int) tourData.getValueAt(i, "orig_mgra");
            int destinationMGRA = (int) tourData.getValueAt(i, "dest_mgra");

            int departPeriod = (int) tourData.getValueAt(i, "start_period");
            int arrivePeriod = (int) tourData.getValueAt(i, "end_period");

            // get utilities, probabilities
            for (int j = 0; j < modes; ++j)
            {
                util[j] = tourData.getValueAt(i, start_util + j);
                prob[j] = tourData.getValueAt(i, start_prob + j);
            }

            // calculate some necessary information for the SUMMIT record
            int autoSufficiency = calculateAutoSufficiency(hhID);
            short originTAZ = (short) mdm.getTaz(originMGRA);
            short destinationTAZ = (short) mdm.getTaz(destinationMGRA);
            int periodMarket = calculatePeriodMarket(departPeriod, arrivePeriod);
            short marketSegment = (short) calculateMarketSegment(autoSufficiency, periodMarket);
            float expUtility = calculateNonTransitExpUtility(util); // not used
            float wtAvailShare = calculateWalkTransitAvailableShare(prob);
            float dtAvailShare = calculateDriveTransitOnlyShare(prob, wtAvailShare);
            float wtProb = calculateTransitShareOfWalkTransit(prob, wtAvailShare);
            float dtProb = calculateTransitShareOfDriveTransitOnly(prob, dtAvailShare);
            float aggExpUtility = calculateAggregateExpUtility(util);

            float participants = 1.0f;
            if (jointTour)
            {
                String participantString = tourData.getStringValueAt(i, participantsCol);
                for (int j = 0; j < participantString.length(); ++j)
                    if (participantString.charAt(j) == ' ') participants += 1;
            }
            // Create a new summit record, and set all attributes
            ConcreteSummitRecord summitRecord = new ConcreteSummitRecord();

            summitRecord.setPtaz(originTAZ);
            summitRecord.setAtaz(destinationTAZ);
            summitRecord.setMarket(marketSegment);
            summitRecord.setTrips(participants);
            summitRecord.setMotorizedTrips(participants);
            summitRecord.setExpAuto(aggExpUtility);
            summitRecord.setWalkTransitAvailableShare(wtAvailShare);
            summitRecord.setDriveTransitOnlyShare(dtAvailShare);
            summitRecord.setTransitShareOfWalkTransit(wtProb);
            summitRecord.setTransitShareOfDriveTransitOnly(dtProb);

            // Insert the record into the record table
            summitRecordTable.insertRecord(summitRecord);

        }

        logger.info("End creating SUMMIT record table for purpose " + purpose);

        logger.info("Begin finalizing table");
        summitRecordTable.finalizeTable();
        logger.info("End finalizing table");

    }

    /**
     * Calculate and return the market for the departure and arrival time
     * periods.
     * 
     * Market 0 = Departure & arrival in peak Market 1 = Departure & arrival in
     * mixed periods (peak and off-peak) Market 2 = Departure & arrival in
     * off-peak
     * 
     * @param departPeriod
     *            1-39 representing time in 30 min. increments, starting at 5 AM
     * @param arrivePeriod
     *            1-39 representing time in 30 min. increments, starting at 5 AM
     * @return Market, as defined above.
     */
    public int calculatePeriodMarket(int departPeriod, int arrivePeriod)
    {

        int departPeak = 0;
        int arrivePeak = 0;

        // check if departure is in peak period
        if (departPeriod > upperEA && departPeriod <= upperAM) departPeak = 1;
        else if (departPeriod > upperMD && departPeriod <= upperPM) departPeak = 1;

        // check if arrival is in peak period
        if (arrivePeriod > upperEA && arrivePeriod <= upperAM) arrivePeak = 1;
        else if (arrivePeriod > upperMD && arrivePeriod <= upperPM) arrivePeak = 1;

        /*
         * Arrival & departure = peak, period = 0 Mixed peak & off-peak, period
         * = 1 Arrival & departure = off-peal, period = 2
         */
        if (departPeak == 1 && arrivePeak == 1) return 0;
        else if (departPeak == 0 && arrivePeak == 0) return 2;

        return 1;
    }

    /**
     * Calculate and return market segment based on auto sufficiency and time
     * period combination.
     * 
     * Market AutoSuff Period 1 0 0 Peak 2 0 1 Mixed 2 0 2 Off-Peak 3 1 0 Peak 4
     * 1 1 Mixed 4 1 2 Off-Peak 5 2 0 Peak 6 2 1 Mixed 6 2 2 Off-Peak
     * 
     * 
     * @param autoSufficiency
     *            0 = 0 autos, 1=autos < adults, 2 = autos >= adults
     * @param periodMarket
     *            0 = Peak, 1 = Mixed, 2 = Off-Peak
     * @return Market for Summit record, as per above table.
     */
    public int calculateMarketSegment(int autoSufficiency, int periodMarket)
    {

        int market = 0;

        switch (autoSufficiency)
        {
            case 0:
                market = (periodMarket == 0) ? 1 : 2;
                break;
            case 1:
                market = (periodMarket == 0) ? 3 : 4;
                break;
            case 2:
                market = (periodMarket == 0) ? 5 : 6;
                break;
            default:
                logger.fatal("Error:  Could not calculate market segment auto sufficiency "
                        + autoSufficiency);
                throw new RuntimeException();
        }

        return market;
    }

    /**
     * Determine what table to use for tour purpose, based on fileNumber array
     * set from properties file.
     * 
     * @param tourPurpose
     * @return Table for tour purpose
     */
    public int calculateSummitTable(String tourPurpose)
    {

        if (tourPurpose.contentEquals("Work")) return fileNumber[0];
        else if (tourPurpose.contentEquals("University")) return fileNumber[1];
        else if (tourPurpose.contentEquals("School")) return fileNumber[2];
        else if (tourPurpose.contentEquals("Escort")) return fileNumber[3];
        else if (tourPurpose.contentEquals("Shop")) return fileNumber[4];
        else if (tourPurpose.contentEquals("Maintenance")) return fileNumber[5];
        else if (tourPurpose.contentEquals("Eating Out")) return fileNumber[6];
        else if (tourPurpose.contentEquals("Visiting")) return fileNumber[7];
        else if (tourPurpose.contentEquals("Discretionary")) return fileNumber[8];
        else if (tourPurpose.contentEquals("Work-Based")) return fileNumber[9];
        else
        {
            logger.error("Error: Tour purpose " + tourPurpose + " not recognized");
        }

        return 99;
    }

    /**
     * Look up the purpose string based on the file number, for use in SUMMIT
     * file header.
     * 
     * @param fileNumber
     * @return A string for the purpose (see above).
     */
    public String getPurpose(int fileNumber)
    {

        return fileName[fileNumber];
    }

    /**
     * Calculate market segment (auto sufficiency)
     * 
     * 0 = 0 autos owned 1 = autos > 0 & autos < adults (persons 18+) 2 = autos
     * > adults
     * 
     * @param hhID
     *            Household ID
     * @return marketSegment
     */
    public int calculateAutoSufficiency(long hhID)
    {

        int drivers = personsOver18.get(hhID);
        int autos = autosOwned.get(hhID);

        if (autos > 0) if (autos < drivers) return 1;
        else return 2;

        return 0;
    }

    /**
     * Calculate the total non-transit exponentiated utility. The method uses
     * the walkTransitModes array and the driveTransitModes array to determine
     * which modes are non-transit, and the sum of their exponentiated utilities
     * is calculated and returned.
     * 
     * @param util
     *            An array of utilities, by mode. -999 indicates mode not
     *            available.
     * @return Sum of exponentiated utilities of non-transit modes.
     */
    public float calculateNonTransitExpUtility(float[] util)
    {

        float expUtility = 0.0f;

        for (int i = 0; i < modes; ++i)
            if (walkTransitModes[i] != 1 && driveTransitModes[i] != 1)
                expUtility += (float) Math.exp(util[i]);
        return expUtility;
    }

    /**
     * Calculate the share of walk-transit available: 1 if any walk-transit mode
     * is available, as indicated by a non-zero probability, else 0. The method
     * iterates through the probability array and returns a 1 if the probability
     * is non-zero for any walk-transit mode, as indicated by the
     * walkTransitModes array.
     * 
     * @param prob
     *            An array of probabilities, dimensioned by modes.
     * @return 1 if walk-transit is available for the record, else 0.
     */
    public float calculateWalkTransitAvailableShare(float[] prob)
    {

        // iterate through the probability array
        for (int i = 0; i < modes; ++i)
            if (walkTransitModes[i] == 1 && prob[i] > 0) return 1.0f;

        // no walk-transit modes with non-zero probability
        return 0.0f;
    }

    /**
     * Calculate the share of drive-transit only available: 1 if any
     * drive-transit mode is available, as indicated by a non-zero probability,
     * and all walk-transit modes are not available. The method iterates through
     * the probability array and returns a 1 if the probability is non-zero for
     * any drive-transit mode, as indicated by the driveTransitModes array.
     * 
     * @param prob
     *            An array of probabilities, dimensioned by modes.
     * @param walkTransitAvailableShare
     *            1 if walk-transit available, else 0.
     * @return 1 if drive-transit only is available for the record, else 0.
     */

    public float calculateDriveTransitOnlyShare(float[] prob, float walkTransitAvailableShare)
    {

        // if walk-transit is available, then drive-transit only share is 0.
        if (walkTransitAvailableShare > 0) return 0.0f;

        // iterate through the probability array
        for (int i = 0; i < modes; ++i)
            if (driveTransitModes[i] == 1 && prob[i] > 0) return 1.0f;

        // no drive-transit modes with non-zero probability
        return 0.0f;

    }

    /**
     * Calculate the total transit probability for records with walk-transit
     * available. The method returns 0 if walk-transit is not available. If
     * walk-transit is available, the method iterates through the probability
     * array, adding all transit mode probabilities. The sum is returned.
     * 
     * @param prob
     *            An array of probabilities, one per mode.
     * @param walkTransitAvailableShare
     *            1 if walk-transit is available, else 0.
     * @return The total transit probability if walk-transit is available, else
     *         0.
     */
    public float calculateTransitShareOfWalkTransit(float[] prob, float walkTransitAvailableShare)
    {

        float transitShare = 0.0f;

        // if walk-transit is unavailable, then walk-transit share is 0.
        if (walkTransitAvailableShare == 0) return transitShare;

        // iterate through the probability array
        for (int i = 0; i < modes; ++i)
            if (walkTransitModes[i] == 1 || driveTransitModes[i] == 1) transitShare += prob[i];

        return transitShare;

    }

    /**
     * Calculate the total transit probability for records where only
     * drive-transit available. The method returns 0 if only drive-transit is
     * not available. If only drive-transit is available, the method iterates
     * through the probability array, adding all drive-transit mode
     * probabilities. The sum is returned.
     * 
     * @param prob
     *            An array of probabilities, one per mode.
     * @param driveTransitOnlyAvailableShare
     *            1 if only drive-transit is available, else 0.
     * @return The total transit probability if only drive-transit is available,
     *         else 0.
     */
    public float calculateTransitShareOfDriveTransitOnly(float[] prob,
            float driveTransitOnlyAvailableShare)
    {

        float transitShare = 0.0f;

        // if drive-transit is unavailable, then walk-transit share is 0.
        if (driveTransitOnlyAvailableShare == 0) return transitShare;

        // iterate through the probability array
        for (int i = 0; i < modes; ++i)
            if (driveTransitModes[i] == 1) transitShare += prob[i];

        return transitShare;

    }

    /**
     * Calculate the logsum by taking ln[Sum (exp(utility)).
     * 
     * @param util
     *            Array of utilities
     * @return Logsum
     */
    public float calculateAggregateExpUtility(float[] util)
    {

        float aggExpUtility = 0.0f;

        for (int i = 0; i < util.length; ++i)
            aggExpUtility += Math.exp(util[i]);

        return aggExpUtility;
    }

    /**
     * Get the in-vehicle time coefficient for the file, based on the values
     * read in the properties file.
     * 
     * @param fileNumber
     * @return The in-vehicle time coefficient for the file.
     */
    public float getIVTCoefficient(int fileNumber)
    {
        return ivtCoeff[fileNumber];
    }

    /**
     * Create a Summit file header.
     * 
     * @param fileNumber
     * @return Header record for Summit file.
     */
    public SummitHeader createSummitHeader(int fileNumber)
    {

        SummitHeader header = new SummitHeader();

        int zones = tdm.getMaxTaz();
        String purpose = getPurpose(fileNumber);
        header.setZones(zones);
        header.setMarketSegments(6);

        float ivt = getIVTCoefficient(fileNumber);
        header.setTransitInVehicleTime(ivt);
        header.setAutoInVehicleTime(ivt);

        header.setPurpose(purpose);
        header.setTimeOfDay("ALL");
        header.setTitle("SANDAG CT-RAMP MODEL SUMMIT FILE");
        return header;
    }

    public void writeFile(String fileName, int fileNumber)
    {

        SummitHeader header = createSummitHeader(fileNumber);
        summitRecordTable.writeTable(fileName, header);

    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {

        // Create a new SandagSummitFile
        String propertiesFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\sandag_abm.properties";
        SandagSummitFile summitFile = new SandagSummitFile(propertiesFile);

        // Read the household file
        String hhFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\householdData_1.csv";
        summitFile.readHouseholdFile(hhFile);

        // Read the person file
        String perFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\personData_1.csv";
        summitFile.readPersonFile(perFile);

        // Open the individual tour file and start processing
        String tourFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\indivTourData_1.csv";
        summitFile.openTourFile(tourFile);

        for (int i = 0; i < summitFile.getNumberOfFiles(); ++i)
        {

            // Create the summit table
            summitFile.createSummitFile(i);

            // Write the summit output file
            String outputFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\";
            String purpose = summitFile.getPurpose(i);
            summitFile.writeFile(outputFile + purpose + ".bin", i);

        }

        // Open the joint tour file and start processing
        tourFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\jointTourData_1.csv";
        summitFile.openTourFile(tourFile);

        for (int i = 0; i < summitFile.getNumberOfFiles(); ++i)
        {

            // Create the summit table
            summitFile.createSummitFile(i);

            // Write the summit output file
            String outputFile = "D:\\projects\\SANDAG\\AB_Model\\SUMMIT\\jnt_";
            String purpose = summitFile.getPurpose(i);
            summitFile.writeFile(outputFile + purpose + ".bin", i);

        }

    }

}
