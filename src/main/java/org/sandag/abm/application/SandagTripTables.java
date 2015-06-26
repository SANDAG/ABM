package org.sandag.abm.application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixIO32BitJvm;
import com.pb.common.matrix.MatrixType;
import com.pb.common.matrix.MatrixWriter;
import com.pb.common.util.ResourceUtil;

public class SandagTripTables
{

    private static Logger           logger                  = Logger.getLogger("tripTables");

    public static final int         MATRIX_DATA_SERVER_PORT = 1171;
 
    private static final String VOT_THRESHOLD_LOW = "valueOfTime.threshold.low";
    private static final String VOT_THRESHOLD_MED = "valueOfTime.threshold.med";
    

    private TableDataSet            indivTripData;
    private TableDataSet            jointTripData;

    // Some parameters
    private int[]                   modeIndex;                                                      // an
                                                                                                     // index
                                                                                                     // array,
                                                                                                     // dimensioned
                                                                                                     // by
                                                                                                     // number
                                                                                                     // of
                                                                                                     // total
                                                                                                     // modes,
                                                                                                     // returns
                                                                                                     // 0=auto
                                                                                                     // modes,
                                                                                                     // 1=non-motor,
                                                                                                     // 2=transit,
                                                                                                     // 3=
                                                                                                     // other
    private int[]                   matrixIndex;                                                    // an
                                                                                                     // index
                                                                                                     // array,
                                                                                                     // dimensioned
                                                                                                     // by
                                                                                                     // number
                                                                                                     // of
                                                                                                     // modes,
                                                                                                     // returns
                                                                                                     // the
                                                                                                     // element
                                                                                                     // of
                                                                                                     // the
                                                                                                     // matrix
                                                                                                     // array
                                                                                                     // to
                                                                                                     // store
                                                                                                     // value

    // array modes: AUTO, NON-MOTORIZED, TRANSIT, OTHER
    private int                     autoModes               = 0;
    private int                     tranModes               = 0;
    private int                     nmotModes               = 0;
    private int                     othrModes               = 0;

    // one file per time period
    private int                     numberOfPeriods;

    private String[]                purposeName             = {"Work", "University", "School",
            "Escort", "Shop", "Maintenance", "EatingOut", "Visiting", "Discretionary", "WorkBased"};

    // matrices are indexed by modes (auto, non-mot,tran,other), valueoftime bins, and sub-modes(shared2gp, etc).
    private Matrix[][][]              matrix;

    private HashMap<String, String> rbMap;
    private MgraDataManager         mgraManager;
    private TazDataManager          tazManager;
    private TapDataManager          tapManager;
    private SandagModelStructure    modelStructure;

    private float[][]               CBDVehicles;                                                    // an
                                                                                                     // array
                                                                                                     // of
                                                                                                     // parked
                                                                                                     // vehicles
                                                                                                     // in
                                                                                                     // MGRAS
                                                                                                     // by
                                                                                                     // period
    private float[][]               PNRVehicles;                                                    // an
                                                                                                     // array
                                                                                                     // of
                                                                                                     // parked
                                                                                                     // vehicles
                                                                                                     // at
                                                                                                     // TAPs
                                                                                                     // by
                                                                                                     // period

    private float                   sampleRate;
    private int                     iteration;

    private MatrixType              mt;
    private MatrixDataServerRmi     ms;
    private static MatrixIO32BitJvm ioVm32Bit               = null;

    private String[]                indivColumns            = {"stop_period", "orig_mgra",
            "dest_mgra", "trip_mode", "inbound", "trip_board_tap", "trip_alight_tap",
            "parking_mgra", "tour_purpose", "valueOfTime"                  };

    private String[]                jointColumns            = {"stop_period", "orig_mgra",
            "dest_mgra", "trip_mode", "inbound", "trip_board_tap", "trip_alight_tap",
            "parking_mgra", "tour_purpose", "num_participants", "valueOfTime"};

    private HashMap<String, Float>  averageOcc3Plus;                                                // a
                                                                                                     // HashMap
                                                                                                     // of
                                                                                                     // average
                                                                                                     // occupancies
                                                                                                     // for
                                                                                                     // 3+
                                                                                                     // vehicles
                                                                                                     // by
                                                                                                     // tour
                                                                                                     // purpose
    private float valueOfTimeThresholdLow = 0;
    private float valueOfTimeThresholdMed = 0;
    //value of time bins by mode group
    int[] votBins = {3,1,1,1};

    /**
     * Constructor.
     * 
     * @param rbMap
     *            HashMap formed from a property map, which includes environment
     *            variables and arguments passed in as -d to VM
     * @param sampleRate
     *            Sample rate 0->1.0
     * @param iteration
     *            Iteration number, program will look for trip file names with
     *            _iteration appended
     */
    public SandagTripTables(HashMap<String, String> rbMap, float sampleRate, int iteration)
    {

        this.rbMap = rbMap;

        tazManager = TazDataManager.getInstance(rbMap);
        tapManager = TapDataManager.getInstance(rbMap);
        mgraManager = MgraDataManager.getInstance(rbMap);

        modelStructure = new SandagModelStructure();

        // Time period limits
        numberOfPeriods = modelStructure.getNumberModelPeriods();

        // number of modes
        modeIndex = new int[modelStructure.MAXIMUM_TOUR_MODE_ALT_INDEX + 1];
        matrixIndex = new int[modeIndex.length];

        // set the mode arrays
        for (int i = 1; i < modeIndex.length; ++i)
        {
            if (modelStructure.getTourModeIsSovOrHov(i))
            {
                modeIndex[i] = 0;
                matrixIndex[i] = autoModes;
                ++autoModes;
            } else if (modelStructure.getTourModeIsNonMotorized(i))
            {
                modeIndex[i] = 1;
                matrixIndex[i] = nmotModes;
                ++nmotModes;
            } else if (modelStructure.getTourModeIsWalkTransit(i)
                    || modelStructure.getTourModeIsDriveTransit(i))
            {
                modeIndex[i] = 2;
                matrixIndex[i] = tranModes;
                ++tranModes;
            } else
            {
                modeIndex[i] = 3;
                matrixIndex[i] = othrModes;
                ++othrModes;
            }
        }
        readOccupancies();
        // Initialize arrays (need for all periods, so initialize here)
        CBDVehicles = new float[mgraManager.getMaxMgra() + 1][numberOfPeriods];
        PNRVehicles = new float[tapManager.getMaxTap() + 1][numberOfPeriods];

        setSampleRate(sampleRate);
        setIteration(iteration);
        
        //value of time thresholds
        valueOfTimeThresholdLow = new Float(rbMap.get(VOT_THRESHOLD_LOW));
        valueOfTimeThresholdMed = new Float(rbMap.get(VOT_THRESHOLD_MED));
        
    }

    /**
     * Read occupancies from the properties file and store in the
     * averageOcc3Plus HashMap
     */
    public void readOccupancies()
    {

        averageOcc3Plus = new HashMap<String, Float>();

        for (int i = 0; i < purposeName.length; ++i)
        {
            String searchString = "occ3plus.purpose." + purposeName[i];
            float occupancy = new Float(Util.getStringValueFromPropertyMap(rbMap, searchString));
            averageOcc3Plus.put(purposeName[i], occupancy);
        }
    }

    /**
     * Initialize all the matrices for the given time period.
     * 
     * @param periodName
     *            The name of the time period.
     */
    public void initializeMatrices(String periodName)
    {

        /*
         * This won't work because external stations aren't listed in the MGRA
         * file int[] tazIndex = tazManager.getTazsOneBased(); int tazs =
         * tazIndex.length-1;
         */
        // Instead, use maximum taz number
        int maxTaz = tazManager.getMaxTaz();
        int[] tazIndex = new int[maxTaz + 1];

        // assume zone numbers are sequential
        for (int i = 1; i < tazIndex.length; ++i)
            tazIndex[i] = i;

        // get the tap index
        int[] tapIndex = tapManager.getTaps();
        int taps = tapIndex.length - 1;
        
  
        // Initialize matrices; one for each mode group (auto, non-mot, tran,
        // other) and value of time group
        // All matrices will be dimensioned by TAZs except for transit, which is
        // dimensioned by TAPs
        int numberOfModes = 4;
        matrix = new Matrix[numberOfModes][][];
        for (int i = 0; i < numberOfModes; ++i)
        {
        	matrix[i] = new Matrix[votBins[i]][];
            
        	for(int j = 0; j< votBins[i];++j){
            
        		String modeName;

        		if (i == 0)
        		{
        			matrix[i][j] = new Matrix[autoModes];
        			for (int k = 0; k < autoModes; ++k)
        			{
        				modeName = modelStructure.getModeName(k + 1);
                		matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
                		matrix[i][j][k].setExternalNumbers(tazIndex);
               		}
                		
                } else if (i == 1){
                
            		matrix[i][j] = new Matrix[nmotModes];
            		for (int k = 0; k < nmotModes; ++k)
            		{
            			modeName = modelStructure.getModeName(k + 1 + autoModes);
               			matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
               			matrix[i][j][k].setExternalNumbers(tazIndex);
               		}
               
                } else if (i == 2){
                	
                	matrix[i][j] = new Matrix[tranModes];
                	for (int k = 0; k < tranModes; ++k)
                	{
                		modeName = modelStructure.getModeName(k + 1 + autoModes + nmotModes);
               			matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", taps, taps);
               			matrix[i][j][k].setExternalNumbers(tapIndex);
               		}
                }else{
            
                	matrix[i][j] = new Matrix[othrModes];
                	for (int k = 0; k < othrModes; ++k)
                	{
                 
               			modeName = modelStructure
                            .getModeName(k + 1 + autoModes + nmotModes + tranModes);
               			matrix[i][j][k] = new Matrix(modeName + "_" + periodName, "", maxTaz, maxTaz);
               			matrix[i][j][k].setExternalNumbers(tazIndex);
               		}
                }
            }
        }
    }

    /**
     * Create trip tables for all time periods and modes. This is the main entry
     * point into the class; it should be called after instantiating the
     * SandagTripTables object.
     * 
     */
    public void createTripTables(MatrixType mt)
    {

        String directory = Util.getStringValueFromPropertyMap(rbMap, "scenario.path");

        // append _iteration to file
        String iterationString = "_" + new Integer(iteration).toString();

        // Open the individual trip file
        String indivTripFile = Util.getStringValueFromPropertyMap(rbMap,
                "Results.IndivTripDataFile");

        // Remove extension from filename
        String extension = getFileExtension(indivTripFile);
        indivTripFile = removeFileExtension(indivTripFile) + iterationString + extension;

        indivTripData = openTripFile(directory + indivTripFile, indivColumns);

        // Open the joint trip file
        String jointTripFile = Util.getStringValueFromPropertyMap(rbMap,
                "Results.JointTripDataFile");
        String jntExtension = getFileExtension(jointTripFile);
        jointTripFile = removeFileExtension(jointTripFile) + iterationString + jntExtension;
        jointTripData = openTripFile(directory + jointTripFile, jointColumns);

        // Iterate through periods so that we don't have to keep
        // trip tables for all periods in memory.
        for (int i = 0; i < numberOfPeriods; ++i)
        {

            // Initialize the matrices
            initializeMatrices(modelStructure.getModelPeriodLabel(i));

            // process trips
            processTrips(i, indivTripData);

            processTrips(i, jointTripData);

            logger.info("Begin writing matrices");
            writeTrips(i, mt);
            logger.info("End writingMatrices");

        }

        // write the vehicles by parking-constrained MGRA
        String CBDFile = Util.getStringValueFromPropertyMap(rbMap, "Results.CBDFile");
        writeCBDFile(directory + CBDFile);

        // write the vehicles by PNR lot TAP
        String PNRFile = Util.getStringValueFromPropertyMap(rbMap, "Results.PNRFile");
        writePNRFile(directory + PNRFile);
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
     * Open a trip file and return the Tabledataset.
     * 
     * @fileName The name of the trip file
     * @return The tabledataset
     */
    public TableDataSet openTripFile(String fileName, String[] columns)
    {

        logger.info("Begin reading the data in file " + fileName);
        TableDataSet tripData;

        try
        {
            OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
            tripData = csvFile.readFile(new File(fileName), columns);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("End reading the data in file " + fileName);
        return tripData;
    }

    /**
     * This is the main workhorse method in this class. It iterates over records
     * in the trip file. Attributes for the trip record are read, and the trip
     * record is accumulated in the relevant matrix.
     * 
     * @param timePeriod
     *            The time period to process
     * @param tripData
     *            The trip data file to process
     */
    public void processTrips(int timePeriod, TableDataSet tripData)
    {

        logger.info("Begin processing trips for period " + timePeriod);

        boolean jointTour = tripData.containsColumn("num_participants");
        int participantsCol = 0;
        if (jointTour)
        {
            participantsCol = tripData.getColumnPosition("num_participants");
        }

        int valueOfTimeCol = tripData.getColumnPosition("valueOfTime");
        
        // iterate through the trip data and save trips in arrays
        for (int i = 1; i <= tripData.getRowCount(); ++i)
        {

            if (i <= 5 || i % 1000 == 0) logger.info("Reading record " + i);
        	
            int departTime = (int) tripData.getValueAt(i, "stop_period");
            int period = modelStructure.getModelPeriodIndex(departTime);
            if (period != timePeriod) continue;

            int originMGRA = (int) tripData.getValueAt(i, "orig_mgra");
            int destinationMGRA = (int) tripData.getValueAt(i, "dest_mgra");
            int tripMode = (int) tripData.getValueAt(i, "trip_mode");

            int originTAZ = mgraManager.getTaz(originMGRA);
            int destinationTAZ = mgraManager.getTaz(destinationMGRA);
            int inbound = (int) tripData.getValueAt(i, "inbound");
            
            //value of time
            float valueOfTime = tripData.getValueAt(i,valueOfTimeCol);

            // transit trip - get boarding and alighting tap
            int boardTap = 0;
            int alightTap = 0;
            int parkingTaz = 0;
            int parkingMGRA = 0;

            if (modelStructure.getTourModeIsWalkTransit(tripMode)
                    || modelStructure.getTourModeIsDriveTransit(tripMode))
            {
                boardTap = (int) tripData.getValueAt(i, "trip_board_tap");
                alightTap = (int) tripData.getValueAt(i, "trip_alight_tap");
            } else
            {
                parkingMGRA = (int) tripData.getValueAt(i, "parking_mgra");
            }

            // scale individual person trips by occupancy for vehicle trips
            // (auto modes only)
            float vehicleTrips = 1;

            if (modelStructure.getTourModeIsS2(tripMode) && !jointTour)
            {
                vehicleTrips = 0.5f;
            } else if (modelStructure.getTourModeIsS3(tripMode) && !jointTour)
            {
                String tourPurpose = tripData.getStringValueAt(i, "tour_purpose");
                tourPurpose = tourPurpose.replace(" ", "");
                tourPurpose = tourPurpose.replace("-", "");
                float occ = averageOcc3Plus.get(tourPurpose);
                vehicleTrips = 1 / occ;
            }

            // calculate person trips for all other modes
            float personTrips = 1;
            if (jointTour)
            {
                personTrips = (int) tripData.getValueAt(i, participantsCol);
            }

            // apply sample rate
            vehicleTrips = vehicleTrips * 1 / sampleRate;
            personTrips = personTrips * 1 / sampleRate;

            // Store in matrix
            int mode = modeIndex[tripMode];
            int mat = matrixIndex[tripMode];
            

            int votBin=0;
            if(votBins[mode]>1)
            	votBin = getValueOfTimeBin(valueOfTime);
            
            if (mode == 0)
            {
                // look up what taz the parking mgra is in, and re-assign the
                // trip destination to the parking taz
                if (parkingMGRA > 0)
                {
                    parkingTaz = mgraManager.getTaz(parkingMGRA);
                    destinationTAZ = parkingTaz;
                    CBDVehicles[parkingMGRA][period] = CBDVehicles[parkingMGRA][period]
                            + vehicleTrips;
                }
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + vehicleTrips));
            } else if (mode == 1)
            {
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + personTrips));
            } else if (mode == 2)
            {

                if (boardTap == 0 || alightTap == 0) continue;

                float value = matrix[mode][votBin][mat].getValueAt(boardTap, alightTap);
                matrix[mode][votBin][mat].setValueAt(boardTap, alightTap, (value + personTrips));

                // Store PNR transit trips in SOV free mode skim (mode 0 mat 0)
                if (modelStructure.getTourModeIsDriveTransit(tripMode))
                {

                    // add the vehicle trip portion to the trip table
                    if (inbound == 0)
                    { // from origin to lot (boarding tap)
                        int PNRTAZ = tapManager.getTazForTap(boardTap);
                        value = matrix[0][votBin][0].getValueAt(originTAZ, PNRTAZ);
                        matrix[0][votBin][0].setValueAt(originTAZ, PNRTAZ, (value + vehicleTrips));

                        // and increment up the array of parked vehicles at the
                        // lot
                        ++PNRVehicles[boardTap][period];

                    } else
                    { // from lot (alighting tap) to destination
                        int PNRTAZ = tapManager.getTazForTap(alightTap);
                        value = matrix[0][votBin][0].getValueAt(PNRTAZ, destinationTAZ);
                        matrix[0][votBin][0].setValueAt(PNRTAZ, destinationTAZ, (value + vehicleTrips));
                    }

                }
            } else
            {
                float value = matrix[mode][votBin][mat].getValueAt(originTAZ, destinationTAZ);
                matrix[mode][votBin][mat].setValueAt(originTAZ, destinationTAZ, (value + personTrips));
            }
        }
        logger.info("End creating trip tables for period " + timePeriod);
    }
    
    /**
     * Return the value of time bin 0 through 2 based on the thresholds provided in the property map
     * @param valueOfTime
     * @return value of time bin 0 through 2
     */
    public int getValueOfTimeBin(float valueOfTime){
    	
    	if(valueOfTime<valueOfTimeThresholdLow)
    		return 0;
    	else if (valueOfTime<valueOfTimeThresholdMed)
    		return 1;
    	else
    		return 2;
    }

    /**
     * Get the output trip table file names from the properties file, and write
     * trip tables for all modes for the given time period.
     * 
     * @param period
     *            Time period, which will be used to find the period time string
     *            to append to each trip table matrix file
     */
    public void writeTrips(int period, MatrixType mt)
    {

        String dir = Util.getStringValueFromPropertyMap(rbMap, "scenario.path");

        String per = modelStructure.getModelPeriodLabel(period);
        String[][] end = new String[4][];
        String[] fileName = new String[4];

        fileName[0] = dir + Util.getStringValueFromPropertyMap(rbMap, "Results.AutoTripMatrix");
        fileName[1] = dir + Util.getStringValueFromPropertyMap(rbMap, "Results.NMotTripMatrix");
        fileName[2] = dir + Util.getStringValueFromPropertyMap(rbMap, "Results.TranTripMatrix");
        fileName[3] = dir + Util.getStringValueFromPropertyMap(rbMap, "Results.OthrTripMatrix");
        
        //the end of the name depends on whether there are multiple vot bins or not
        String[] votBinName = {"low","med","high"};
        
        for(int i = 0; i<4;++i){
        	end[i] = new String[votBins[i]];
        	for(int j = 0; j < votBins[i];++j){
        		if(votBins[i]>1)
        			end[i][j] = "_" + per + "_"+ votBinName[j]+ ".mtx";
        		else
        			end[i][j] = "_" + per + ".mtx";
        	}
        }

        for (int i = 0; i < 4; ++i)
        {
        	for(int j = 0; j < votBins[i];++j){
        		try
        		{
        			if (ms != null) ms.writeMatrixFile(fileName[i]+end[i][j], matrix[i][j], mt);
        			else writeMatrixFile(fileName[i]+end[i][j], matrix[i][j]);
        		} catch (Exception e)
        		{
        			logger.error("exception caught writing " + mt.toString() + " matrix file = "
                        + fileName[i] +end[i][j] + ", for mode index = " + i, e);
        			throw new RuntimeException();
        		}
        	}
        }

    }

   /**
     * Utility method to write a set of matrices to disk.
     * 
     * @param fileName
     *            The file name to write to.
     * @param m
     *            An array of matrices
     */
    public void writeMatrixFile(String fileName, Matrix[] m)
    {

        // auto trips
        MatrixWriter writer = MatrixWriter.createWriter(fileName);
        String[] names = new String[m.length];

        for (int i = 0; i < m.length; i++)
        {
            names[i] = m[i].getName();
            logger.info(m[i].getName() + " has " + m[i].getRowCount() + " rows, "
                    + m[i].getColumnCount() + " cols, and a total of " + m[i].getSum());
        }

        writer.writeMatrices(names, m);
    }

    /**
     * Start a 32-bit matrix server to write matrices.
     * 
     * @param mType
     *            Matrix type
     */
    private void start32BitMatrixIoServer(MatrixType mType)
    {

        // start the matrix I/O server process
        ioVm32Bit = MatrixIO32BitJvm.getInstance();
        ioVm32Bit.setSizeInMegaBytes(1024);
        ioVm32Bit.startJVM32();

        // establish that matrix reader and writer classes will use the RMI
        // versions
        ioVm32Bit.startMatrixDataServer(mType);
        logger.info("matrix data server 32 bit process started.");

    }

    /**
     * Stop the 32-bit matrix server.
     */
    private void stop32BitMatrixIoServer()
    {

        // stop the matrix I/O server process
        ioVm32Bit.stopMatrixDataServer();

        // close the JVM in which the RMI reader/writer classes were running
        ioVm32Bit.stopJVM32();
        logger.info("matrix data server 32 bit process stopped.");

    }

    /**
     * Write a file of vehicles parking in parking-constrained areas by MGRA.
     * 
     * @param fileName
     *            The name of the csv file to write to.
     */
    public void writeCBDFile(String fileName)
    {

        try
        {
            FileWriter writer = new FileWriter(fileName);

            // write header
            writer.append("MGRA,");

            for (int j = 0; j < numberOfPeriods; ++j)
                writer.append(modelStructure.getModelPeriodLabel(j) + ",");

            writer.append("Total\n");

            // iterate through mgras
            for (int i = 0; i < CBDVehicles.length; ++i)
            {

                float totalVehicles = 0;
                for (int j = 0; j < numberOfPeriods; ++j)
                {
                    totalVehicles += CBDVehicles[i][j];
                }

                // only write the mgra if there are vehicles parked there
                if (totalVehicles > 0)
                {

                    writer.append(Integer.toString(i));

                    // iterate through periods
                    for (int j = 0; j < numberOfPeriods; ++j)
                        writer.append("," + Float.toString(CBDVehicles[i][j]));

                    writer.append("," + Float.toString(totalVehicles) + "\n");
                    writer.flush();
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Write a file of vehicles parking in PNR lots by TAP.
     * 
     * @param fileName
     *            The name of the csv file to write to.
     */
    public void writePNRFile(String fileName)
    {

        try
        {
            FileWriter writer = new FileWriter(fileName);

            // write header
            writer.append("TAP,");

            for (int j = 0; j < numberOfPeriods; ++j)
                writer.append(modelStructure.getModelPeriodLabel(j) + ",");

            writer.append("Total\n");

            // iterate through taps
            for (int i = 0; i < PNRVehicles.length; ++i)
            {

                float totalVehicles = 0;
                for (int j = 0; j < numberOfPeriods; ++j)
                {
                    totalVehicles += PNRVehicles[i][j];
                }

                // only write the tap if there are vehicles parked there
                if (totalVehicles > 0)
                {

                    writer.append(Integer.toString(i));

                    // iterate through periods
                    for (int j = 0; j < numberOfPeriods; ++j)
                        writer.append("," + Float.toString(PNRVehicles[i][j]));

                    writer.append("," + Float.toString(totalVehicles) + "\n");
                    writer.flush();
                }
            }
            writer.flush();
            writer.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Set the sample rate
     * 
     * @param sampleRate
     *            The sample rate, used for expanding trips
     */
    public void setSampleRate(float sampleRate)
    {
        this.sampleRate = sampleRate;
    }

    /**
     * Set the iteration number
     * 
     * @param sampleRate
     *            The iteration number, should be appended to trip files as
     *            _iteration
     */
    public void setIteration(int iteration)
    {
        this.iteration = iteration;
    }

    /**
     * Helper method for establishing connection to the matrix server. If "none"
     * was specified, default server is started on localhost. Otherwise, a
     * server at the IP address and port specified is assumed to be running, and
     * this code will start the server and stop it when finished.
     * 
     * @param pMap
     *            is the proprty file HashMap
     */
    private void setupMatrixServer(HashMap<String, String> pMap)
    {

        String matrixServerAddress = "";
        int serverPort = 0;
        try
        {
            // get matrix server address. if "none" is specified, no server will
            // be
            // started, and matrix io will ocurr within the current process.
            matrixServerAddress = Util.getStringValueFromPropertyMap(pMap,
                    "RunModel.MatrixServerAddress");
            try
            {
                // get matrix server port.
                serverPort = Util.getIntegerValueFromPropertyMap(pMap, "RunModel.MatrixServerPort");
            } catch (RuntimeException e)
            {
                serverPort = MATRIX_DATA_SERVER_PORT;
            }
        } catch (RuntimeException e)
        {
            matrixServerAddress = "localhost";
            serverPort = MATRIX_DATA_SERVER_PORT;
        }

        String matrixTypeName = Util.getStringValueFromPropertyMap(pMap, "Results.MatrixType");
        mt = MatrixType.lookUpMatrixType(matrixTypeName);

        try
        {

            if (!matrixServerAddress.equalsIgnoreCase("none"))
            {

                if (matrixServerAddress.equalsIgnoreCase("localhost"))
                {

                    try
                    {
                        // create the concrete data server object
                        start32BitMatrixIoServer(mt);
                    } catch (RuntimeException e)
                    {
                        logger.error(
                                "RuntimeException caught starting 64 bit matrix server on localhost -- exiting.",
                                e);
                        stop32BitMatrixIoServer();
                    }

                } else
                {
                    try
                    {
                        // create the RMI data server object
                        ms = new MatrixDataServerRmi(matrixServerAddress, serverPort,
                                MatrixDataServer.MATRIX_DATA_SERVER_NAME);
                        ms.testRemote("SandagTripTables");
                        ms.start32BitMatrixIoServer(mt, "SandagTripTables");
                    } catch (RuntimeException e)
                    {
                        logger.error("RuntimeException caught starting matrix server: "
                                + matrixServerAddress + ":" + serverPort
                                + " from RMI object -- exiting.", e);
                        throw new RuntimeException();
                    }
                }

            }

        } catch (Exception e)
        {

            if (!matrixServerAddress.equalsIgnoreCase("localhost"))
            {
                ms.stop32BitMatrixIoServer();
            } else
            {
                stop32BitMatrixIoServer();
            }

            logger.error(String.format("exception caught setting up matrix server -- exiting."), e);
            throw new RuntimeException();

        }

    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {

        String propertiesFile = null;
        HashMap<String, String> pMap;

        logger.info(String.format("SANDAG Trip Table Generation Program using CT-RAMP version %s",
                CtrampApplication.VERSION));

        logger.info(String.format("Building trip tables"));

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else propertiesFile = args[0];

        pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);

        float sampleRate = 1.0f;
        int iteration = 1;

        for (int i = 1; i < args.length; ++i)
        {
            if (args[i].equalsIgnoreCase("-sampleRate"))
            {
                sampleRate = Float.parseFloat(args[i + 1]);
            }
            if (args[i].equalsIgnoreCase("-iteration"))
            {
                iteration = Integer.parseInt(args[i + 1]);
            }
        }

        logger.info(String.format("-sampleRate %.4f.", sampleRate));
        logger.info("-iteration  " + iteration);

        SandagTripTables tripTables = new SandagTripTables(pMap, sampleRate, iteration);

        tripTables.setupMatrixServer(pMap);

        tripTables.createTripTables(tripTables.mt);

        // We're done with the matrix server conection, so close it
        if (tripTables.ms == null)
        {
            tripTables.stop32BitMatrixIoServer();
        } else
        {
            tripTables.ms.stop32BitMatrixIoServer("SandagTripTables");
        }

    }

}
