package org.sandag.abm.ctramp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.MandatoryAccessibilitiesDMU;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.IndexValues;
import com.pb.common.newmodel.UtilityExpressionCalculator;

// import com.pb.common.util.ObjectUtil;

/**
 * @author crf <br/>
 *         Started: Dec 31, 2008 11:46:36 AM
 */
public class HouseholdDataWriter
{

    private transient Logger            logger                          = Logger.getLogger(HouseholdDataWriter.class);

    private static final String         PROPERTIES_HOUSEHOLD_DATA_FILE  = "Results.HouseholdDataFile";
    private static final String         PROPERTIES_PERSON_DATA_FILE     = "Results.PersonDataFile";
    private static final String         PROPERTIES_INDIV_TOUR_DATA_FILE = "Results.IndivTourDataFile";
    private static final String         PROPERTIES_JOINT_TOUR_DATA_FILE = "Results.JointTourDataFile";
    private static final String         PROPERTIES_INDIV_TRIP_DATA_FILE = "Results.IndivTripDataFile";
    private static final String         PROPERTIES_JOINT_TRIP_DATA_FILE = "Results.JointTripDataFile";

    private static final String         PROPERTIES_HOUSEHOLD_TABLE      = "Results.HouseholdTable";
    private static final String         PROPERTIES_PERSON_TABLE         = "Results.PersonTable";
    private static final String         PROPERTIES_INDIV_TOUR_TABLE     = "Results.IndivTourTable";
    private static final String         PROPERTIES_JOINT_TOUR_TABLE     = "Results.JointTourTable";
    private static final String         PROPERTIES_INDIV_TRIP_TABLE     = "Results.IndivTripTable";
    private static final String         PROPERTIES_JOINT_TRIP_TABLE     = "Results.JointTripTable";
    
    private static final String         PROPERTIES_WRITE_LOGSUMS        = "Results.WriteLogsums";

    private static final int            NUM_WRITE_PACKETS               = 2000;

    private final String                intFormat                       = "%d";
    private final String                floatFormat                     = "%f";
    private final String                doubleFormat                    = "%f";
    private final String                fileStringFormat                = "%s";
    private final String                databaseStringFormat            = "'%s'";
    private String                      stringFormat                    = fileStringFormat;

    private boolean                     saveUtilsProbsFlag              = false;
    private boolean                     writeLogsums                    = false;
    private int                 setNA                           = -1;


    private HashMap<String, String>     rbMap;

    private MandatoryAccessibilitiesDMU dmu;
    private UtilityExpressionCalculator autoSkimUEC;
    private IndexValues                 iv;
    private MgraDataManager             mgraManager;

    private ModelStructure              modelStructure;
    private int                         iteration;

    private HashMap<Integer, String>    purposeIndexNameMap;

    public HouseholdDataWriter(HashMap<String, String> rbMap, ModelStructure modelStructure,
            int iteration)
    {
        logger.info("Writing data structures to files.");
        this.modelStructure = modelStructure;
        this.iteration = iteration;
        this.rbMap = rbMap;

        // create a UEC to get highway distance traveled for tours
        String uecFileName = rbMap.get("acc.mandatory.uec.file");
        int dataPage = Integer.parseInt(rbMap.get("acc.mandatory.data.page"));
        int autoSkimPage = Integer.parseInt(rbMap.get("acc.mandatory.auto.page"));
        File uecFile = new File(uecFileName);
        dmu = new MandatoryAccessibilitiesDMU();
        autoSkimUEC = new UtilityExpressionCalculator(uecFile, autoSkimPage, dataPage, rbMap, dmu);
        iv = new IndexValues();
        mgraManager = MgraDataManager.getInstance(rbMap);

        purposeIndexNameMap = this.modelStructure.getIndexPrimaryPurposeNameMap();

        // default is to not save the tour mode choice utils and probs for each
        // tour
        String saveUtilsProbsString = rbMap
                .get(CtrampApplication.PROPERTIES_SAVE_TOUR_MODE_CHOICE_UTILS);
        if (saveUtilsProbsString != null)
        {
            if (saveUtilsProbsString.equalsIgnoreCase("true")) saveUtilsProbsFlag = true;
        }
        
        String writeLogsumsString = rbMap.get(PROPERTIES_WRITE_LOGSUMS);
        writeLogsums = Boolean.valueOf(writeLogsumsString);

    }

    // NOTE - this method should not be called simultaneously with the file one
    // one
    // as the string format is changed
    public void writeDataToDatabase(HouseholdDataManagerIf householdData, String dbFileName)
    {
        logger.info("Writing data structures to database.");
        long t = System.currentTimeMillis();
        stringFormat = databaseStringFormat;
        writeData(householdData, new DatabaseDataWriter(dbFileName));
        float delta = ((Long) (System.currentTimeMillis() - t)).floatValue() / 60000.0f;
        logger.info("Finished writing data structures to database (" + delta + " minutes).");
    }

    // NOTE - this method should not be called simultaneously with the database
    // one
    // one as the string format is changed
    public void writeDataToFiles(HouseholdDataManagerIf householdData)
    {
        logger.info("Writing data structures to csv file.");
        stringFormat = fileStringFormat;
        FileDataWriter fdw = new FileDataWriter();
        writeData(householdData, fdw);
    }

    private void writeData(HouseholdDataManagerIf householdDataManager, DataWriter writer)
    {
        int hhid = 0;
        int persNum = 0;
        int tourid = 0;
        try
        {

            ArrayList<int[]> startEndTaskIndicesList = getWriteHouseholdRanges(householdDataManager
                    .getNumHouseholds());

            long maxSize = 0;
            for (int[] startEndIndices : startEndTaskIndicesList)
            {

                int startIndex = startEndIndices[0];
                int endIndex = startEndIndices[1];

                // get the array of households
                Household[] householdArray = householdDataManager.getHhArray(startIndex, endIndex);

                for (Household hh : householdArray)
                {
                    if (hh == null) continue;
                    hhid = hh.getHhId();

                    // long size = ObjectUtil.sizeOf(hh);
                    // if (size > maxSize) maxSize = size;

                    writer.writeHouseholdData(formHouseholdDataEntry(hh));
                    for (Person p : hh.getPersons())
                    {
                        if (p == null) continue;
                        persNum = p.getPersonNum();

                        writer.writePersonData(formPersonDataEntry(p));
                        for (Tour t : p.getListOfWorkTours())
                            writeIndivTourData(t, writer);
                        for (Tour t : p.getListOfSchoolTours())
                            writeIndivTourData(t, writer);
                        for (Tour t : p.getListOfIndividualNonMandatoryTours())
                            writeIndivTourData(t, writer);
                        for (Tour t : p.getListOfAtWorkSubtours())
                            writeIndivTourData(t, writer);
                    }
                    Tour[] jointTours = hh.getJointTourArray();
                    if (jointTours != null) for (Tour t : jointTours)
                    {
                        if (t == null) continue;
                        writeJointTourData(t, writer);
                    }
                }
            }

            // logger.info("max size for all Household objects after writing output files is "
            // + maxSize + " bytes.");

        } catch (RuntimeException e)
        {
            logger.error(String.format("error writing hh=%d, persNum=%d", hhid, persNum), e);
            throw new RuntimeException();
        } finally
        {
            writer.finishActions();
        }
    }

    private void writeIndivTourData(Tour t, DataWriter writer)
    {
        writer.writeIndivTourData(formIndivTourDataEntry(t));

        Stop[] outboundStops = t.getOutboundStops();
        if (outboundStops != null)
        {
            for (int i = 0; i < outboundStops.length; i++)
            {
                writer.writeIndivTripData(formIndivTripDataEntry(outboundStops[i]));
            }
        } else
        {
            writer.writeIndivTripData(formTourAsIndivTripDataEntry(t, false));
        }

        Stop[] inboundStops = t.getInboundStops();
        if (inboundStops != null)
        {
            for (Stop s : inboundStops)
                writer.writeIndivTripData(formIndivTripDataEntry(s));
        } else
        {
            writer.writeIndivTripData(formTourAsIndivTripDataEntry(t, true));
        }

    }

    private void writeJointTourData(Tour t, DataWriter writer)
    {
        writer.writeJointTourData(formJointTourDataEntry(t));

        Stop[] outboundStops = t.getOutboundStops();
        if (outboundStops != null)
        {
            for (Stop s : outboundStops)
                writer.writeJointTripData(formJointTripDataEntry(s));
        } else
        {
            writer.writeJointTripData(formTourAsJointTripDataEntry(t, false));
        }

        Stop[] inboundStops = t.getInboundStops();
        if (inboundStops != null)
        {
            for (Stop s : inboundStops)
                writer.writeJointTripData(formJointTripDataEntry(s));
        } else
        {
            writer.writeJointTripData(formTourAsJointTripDataEntry(t, true));
        }

    }

    private String string(int value)
    {
        return String.format(intFormat, value);
    }

    private String string(float value)
    {
        return String.format(floatFormat, value);
    }

    private String string(double value)
    {
        return String.format(doubleFormat, value);
    }

    private String string(String value)
    {
        return String.format(stringFormat, value);
    }

    private List<String> formHouseholdColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("home_mgra");
        data.add("income");
        data.add("autos");
        data.add("HVs");
        data.add("AVs");
        data.add("transponder");
        data.add("cdap_pattern");
        data.add("out_escort_choice");
        data.add("inb_escort_choice");       
        data.add("jtf_choice");
		data.add("sampleRate");
        
        if(writeLogsums){
        	data.add("aoLogsum");
            data.add("transponderLogsum");
            data.add("cdapLogsum");
            data.add("jtfLogsum");
        }
        
        return data;
    }

    private List<SqliteDataTypes> formHouseholdColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
		data.add(SqliteDataTypes.REAL);
        
        if(writeLogsums){
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        }
        return data;
    }

    private List<String> formHouseholdDataEntry(Household hh)
    {
        List<String> data = new LinkedList<String>();
        data.add(string(hh.getHhId()));
        data.add(string(hh.getHhMgra()));
        data.add(string(hh.getIncomeInDollars()));
        data.add(string(hh.getAutosOwned()));
        data.add(string(hh.getConventionalVehicles()));
        data.add(string(hh.getAutomatedVehicles()));
        data.add(string(hh.getTpChoice()));
        data.add(string(hh.getCoordinatedDailyActivityPattern()));
        data.add(string(hh.getOutboundEscortChoice()));
        data.add(string(hh.getInboundEscortChoice()));
        data.add(string(hh.getJointTourFreqChosenAlt()));
		data.add(string(hh.getSampleRate()));
        
        if(writeLogsums){
        	data.add(string(hh.getAutoOwnershipLogsum()));
            data.add(string(hh.getTransponderLogsum()));
            data.add(string(hh.getCdapLogsum()));
            data.add(string(hh.getJtfLogsum()));
        }
        return data;
    }

    private List<String> formPersonColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("person_id");
        data.add("person_num");
        data.add("age");
        data.add("gender");
        data.add("type");
        data.add("value_of_time");
        data.add("activity_pattern");
        data.add("imf_choice");
        data.add("inmf_choice");
        data.add("fp_choice");
        data.add("reimb_pct");
        data.add("tele_choice");
        data.add("ie_choice");
        data.add("timeFactorWork");
        data.add("timeFactorNonWork");
		data.add("sampleRate");
        
        if(writeLogsums){
        	data.add("wfhLogsum");
        	data.add("wlLogsum");
        	data.add("slLogsum");
            data.add("fpLogsum");
            data.add("tcLogsum");
        	data.add("ieLogsum");
            data.add("cdapLogsum");
            data.add("imtfLogsum");
            data.add("inmtfLogsum");
       }

        return data;
    }

    private List<SqliteDataTypes> formPersonColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.REAL);
		data.add(SqliteDataTypes.REAL);
        
        if(writeLogsums){
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
       	
        }
        return data;
    }

    private List<String> formPersonDataEntry(Person p)
    {
        List<String> data = new LinkedList<String>();
        data.add(string(p.getHouseholdObject().getHhId()));
        data.add(string(p.getPersonId()));
        data.add(string(p.getPersonNum()));
        data.add(string(p.getAge()));
        data.add(string(p.getPersonIsMale() == 1 ? "m" : "f"));
        data.add(string(p.getPersonType()));
        data.add(string(p.getValueOfTime()));
        data.add(string(p.getCdapActivity()));
        data.add(string(p.getImtfChoice()));
        data.add(string(p.getInmtfChoice()));
        data.add(string(p.getFreeParkingAvailableResult()));
        data.add(string(p.getParkingReimbursement()));
        data.add(string(p.getTelecommuteChoice()));
        data.add(string(p.getInternalExternalTripChoiceResult()));
        data.add(string(p.getTimeFactorWork()));
        data.add(string(p.getTimeFactorNonWork()));
		data.add(string(p.getSampleRate()));
        
        if(writeLogsums){
        	data.add(string(p.getWorksFromHomeLogsum()));
          	data.add(string(p.getWorkLocationLogsum()));
        	data.add(string(p.getSchoolLocationLogsum()));        	
        	data.add(string(p.getParkingProvisionLogsum()));
        	data.add(string(p.getTelecommuteLogsum()));
        	data.add(string(p.getIeLogsum()));
            data.add(string(p.getCdapLogsum()));
            data.add(string(p.getImtfLogsum()));
            data.add(string(p.getInmtfLogsum()));
       	
        }
        return data;
    }

    private List<String> formIndivTourColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("person_id");
        data.add("person_num");
        data.add("person_type");
        data.add("tour_id");
        data.add("tour_category");
        data.add("tour_purpose");
        data.add("orig_mgra");
        data.add("dest_mgra");
        data.add("start_period");
        data.add("end_period");
        data.add("tour_mode");
        data.add("av_avail");
        data.add("tour_distance");
        data.add("atWork_freq");
        data.add("num_ob_stops");
        data.add("num_ib_stops");
        data.add("valueOfTime");

        data.add("escort_type_out");
        data.add("escort_type_in");
      	data.add("driver_num_out");
      	data.add("driver_num_in");
		data.add("sampleRate");
 
        if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            for (int i = 1; i <= numModeAlts; i++)
            {
                String colName = String.format("util_%d", i);
                data.add(colName);
            }

            for (int i = 1; i <= numModeAlts; i++)
            {
                String colName = String.format("prob_%d", i);
                data.add(colName);
            }
        }
        
        if(writeLogsums){
            data.add("timeOfDayLogsum");
            data.add("tourModeLogsum");
            data.add("subtourFreqLogsum");
            data.add("tourDestinationLogsum");
            data.add("stopFreqLogsum");

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            for(int i = 1; i<= numStopAlts;++i){
                String colName = String.format("outStopDCLogsum_%d", i);
            	data.add(colName);
            }
            for(int i = 1; i<= numStopAlts;++i){
                String colName = String.format("inbStopDCLogsum_%d", i);
            	data.add(colName);
            }
        }

        return data;
    }

    private List<String> formJointTourColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("tour_id");
        data.add("tour_category");
        data.add("tour_purpose");
        data.add("tour_composition");
        data.add("tour_participants");
        data.add("orig_mgra");
        data.add("dest_mgra");
        data.add("start_period");
        data.add("end_period");
        data.add("tour_mode");
        data.add("av_avail");
        data.add("tour_distance");
        data.add("num_ob_stops");
        data.add("num_ib_stops");
        data.add("valueOfTime");
		data.add("sampleRate");

        if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            for (int i = 1; i <= numModeAlts; i++)
            {
                String colName = String.format("util_%d", i);
                data.add(colName);
            }

            for (int i = 1; i <= numModeAlts; i++)
            {
                String colName = String.format("prob_%d", i);
                data.add(colName);
            }
        }
        if(writeLogsums){
            data.add("timeOfDayLogsum");
            data.add("tourModeLogsum");
            data.add("subtourFreqLogsum");
            data.add("tourDestinationLogsum");
            data.add("stopFreqLogsum");

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            for(int i = 1; i<= numStopAlts;++i){
                String colName = String.format("outStopDCLogsum_%d", i);
            	data.add(colName);
            }
            for(int i = 1; i<= numStopAlts;++i){
                String colName = String.format("inbStopDCLogsum_%d", i);
            	data.add(colName);
            }
        }

        return data;
    }

    private List<SqliteDataTypes> formIndivTourColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);

        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER); 
		data.add(SqliteDataTypes.REAL);
        
        if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            for (int i = 1; i <= numModeAlts; i++)
            {
                data.add(SqliteDataTypes.REAL);
            }

            for (int i = 1; i <= numModeAlts; i++)
            {
                data.add(SqliteDataTypes.REAL);
            }
        }
        if(writeLogsums){
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            for(int i = 1; i<= numStopAlts;++i){
                data.add(SqliteDataTypes.REAL);
            }
            for(int i = 1; i<= numStopAlts;++i){
                data.add(SqliteDataTypes.REAL);
            }
        }


        return data;
    }

    private List<SqliteDataTypes> formJointTourColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
		data.add(SqliteDataTypes.REAL);

        if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            for (int i = 1; i <= numModeAlts; i++)
            {
                data.add(SqliteDataTypes.REAL);
            }

            for (int i = 1; i <= numModeAlts; i++)
            {
                data.add(SqliteDataTypes.REAL);
            }
        }

        if(writeLogsums){
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);
            data.add(SqliteDataTypes.REAL);

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            for(int i = 1; i<= numStopAlts;++i){
                data.add(SqliteDataTypes.REAL);
            }
            for(int i = 1; i<= numStopAlts;++i){
                data.add(SqliteDataTypes.REAL);
            }
        }
        return data;
    }

    private List<String> formIndivTourDataEntry(Tour t)
    {

        List<String> data = new LinkedList<String>();
        data.add(string(t.getHhId()));
        data.add(string(t.getPersonObject().getPersonId()));
        data.add(string(t.getPersonObject().getPersonNum()));
        data.add(string(t.getPersonObject().getPersonTypeNumber()));
        data.add(string(t.getTourId()));
        data.add(string(t.getTourCategory()));
        data.add(string(t.getTourPurpose()));
        data.add(string(t.getTourOrigMgra()));
        data.add(string(t.getTourDestMgra()));
        data.add(string(t.getTourDepartPeriod()));
        data.add(string(t.getTourArrivePeriod()));
        data.add(string(t.getTourModeChoice()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));
        data.add(string(calculateDistancesForAllMgras(t.getTourOrigMgra(), t.getTourDestMgra())));
        data.add(string(t.getSubtourFreqChoice()));
        data.add(string(t.getNumOutboundStops() == 0 ? 0 : t.getNumOutboundStops() - 1));
        data.add(string(t.getNumInboundStops() == 0 ? 0 : t.getNumInboundStops() - 1));
        data.add(string(t.getValueOfTime()));

        data.add(string(t.getEscortTypeOutbound()));
        data.add(string(t.getEscortTypeInbound()));
        data.add(string(t.getDriverPnumOutbound()));
        data.add(string(t.getDriverPnumInbound()));
		data.add(string(t.getSampleRate()));

       if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            float[] utils = t.getTourModalUtilities();

            if (utils != null){

            	for (int i = 0; i < utils.length; i++)
            		data.add(string(utils[i]));
            	for (int i = utils.length; i < numModeAlts; i++)
            		data.add("-999");
            
            }else{
            	for(int i =0;i<numModeAlts;++i)
            		data.add("-999");
            }
            
            float[] probs = t.getTourModalProbabilities();
           
            if(probs != null){
            	for (int i = 0; i < probs.length; i++)
            		data.add(string(probs[i]));
            	for (int i = probs.length; i < numModeAlts; i++)
            		data.add("0.0");
            }else{
            	for(int i =0;i<numModeAlts;++i)
            		data.add("0.0");
            }
        }

        if(writeLogsums){
            data.add(string(t.getTimeOfDayLogsum()));
            data.add(string(t.getTourModeLogsum()));
            data.add(string(t.getSubtourFreqLogsum()));
            data.add(string(t.getTourDestinationLogsum()));
            data.add(string(t.getStopFreqLogsum()));

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            ArrayList<Float> outboundStopDCLogsums = t.getOutboundStopDestinationLogsums();
            for(int i = 0; i<outboundStopDCLogsums.size();++i){
                data.add(string(outboundStopDCLogsums.get(i)));
            }
            for(int i=outboundStopDCLogsums.size();i<numStopAlts;++i)
                data.add(string(0));
                        	
            ArrayList<Float> inboundStopDCLogsums = t.getInboundStopDestinationLogsums();
            for(int i = 0; i<inboundStopDCLogsums.size();++i){
                data.add(string(inboundStopDCLogsums.get(i)));
            }
            for(int i=inboundStopDCLogsums.size();i<numStopAlts;++i)
                data.add(string(0));
        }

        return data;
    }

    private List<String> formJointTourDataEntry(Tour t)
    {
        List<String> data = new LinkedList<String>();
        data.add(string(t.getHhId()));
        data.add(string(t.getTourId()));
        data.add(string(t.getTourCategory()));
        data.add(string(t.getTourPurpose()));
        data.add(string(t.getJointTourComposition()));
        data.add(string(formTourParticipationEntry(t)));
        data.add(string(t.getTourOrigMgra()));
        data.add(string(t.getTourDestMgra()));
        data.add(string(t.getTourDepartPeriod()));
        data.add(string(t.getTourArrivePeriod()));
        data.add(string(t.getTourModeChoice()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));
        data.add(string(calculateDistancesForAllMgras(t.getTourOrigMgra(), t.getTourDestMgra())));
        data.add(string(t.getNumOutboundStops() == 0 ? 0 : t.getNumOutboundStops() - 1));
        data.add(string(t.getNumInboundStops() == 0 ? 0 : t.getNumInboundStops() - 1));
        data.add(string(t.getValueOfTime()));
		data.add(string(t.getSampleRate()));

        if (saveUtilsProbsFlag)
        {
            int numModeAlts = modelStructure.getMaxTourModeIndex();
            float[] utils = t.getTourModalUtilities();

            int dummy = 0;
            if (utils == null) dummy = 1;

            for (int i = 0; i < utils.length; i++)
                data.add(string(utils[i]));
            for (int i = utils.length; i < numModeAlts; i++)
                data.add("-999");

            float[] probs = t.getTourModalProbabilities();
            for (int i = 0; i < probs.length; i++)
                data.add(string(probs[i]));
            for (int i = probs.length; i < numModeAlts; i++)
                data.add("0.0");
        }

        if(writeLogsums){
            data.add(string(t.getTimeOfDayLogsum()));
            data.add(string(t.getTourModeLogsum()));
            data.add(string(t.getSubtourFreqLogsum()));
            data.add(string(t.getTourDestinationLogsum()));
            data.add(string(t.getStopFreqLogsum()));

            int numStopAlts = modelStructure.MAX_STOPS_PER_DIRECTION;
            ArrayList<Float> outboundStopDCLogsums = t.getOutboundStopDestinationLogsums();
            for(int i = 0; i<outboundStopDCLogsums.size();++i){
                data.add(string(outboundStopDCLogsums.get(i)));
            }
            for(int i=outboundStopDCLogsums.size();i<numStopAlts;++i)
                data.add(string(0));
                        	
            ArrayList<Float> inboundStopDCLogsums = t.getInboundStopDestinationLogsums();
            for(int i = 0; i<inboundStopDCLogsums.size();++i){
                data.add(string(inboundStopDCLogsums.get(i)));
            }
            for(int i=inboundStopDCLogsums.size();i<numStopAlts;++i)
                data.add(string(0));
        }
        return data;
    }

    private String formTourParticipationEntry(Tour t)
    {
        int[] persons = t.getPersonNumArray();
        if (persons == null)
            throw new RuntimeException("null Person[] object for joint tour, hhid=" + t.getHhId());
        if (persons.length == 1)
            throw new RuntimeException("Person[] object has length=1 for joint tour, hhid="
                    + t.getHhId());
        String participation = Integer.toString(persons[0]);
        for (int i = 1; i < persons.length; i++)
        {
            participation += " ";
            participation += persons[i];
        }
        return participation;
    }

    /*
     * private int getBitMask(int number) { switch (number) { case 1 : return 1;
     * case 2 : return 2; case 3 : return 4; case 4 : return 8; case 5 : return
     * 16; case 6 : return 32; case 7 : return 64; case 8 : return 128; case 9 :
     * return 256; case 10 : return 512; case 11 : return 1024; case 12 : return
     * 2048; case 13 : return 4096; case 14 : return 8192; case 15 : return
     * 16384; case 16 : return 32768; case 17 : return 65536; case 18 : return
     * 131072; case 19 : return 262144; case 20 : return 524288; case 21 :
     * return 1048576; case 22 : return 2097152; case 23 : return 4194304; case
     * 24 : return 8388608; case 25 : return 16777216; case 26 : return
     * 33554432; case 27 : return 67108864; case 28 : return 134217728; case 29
     * : return 268435456; case 30 : return 536870912; default : throw new
     * RuntimeException("Participation array value unknown: " + number); } }
     */

    private List<String> formIndivTripColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("person_id");
        data.add("person_num");
        data.add("tour_id");
        data.add("stop_id");
        data.add("inbound");
        data.add("tour_purpose");
        data.add("orig_purpose");
        data.add("dest_purpose");
        data.add("orig_mgra");
        data.add("dest_mgra");
        data.add("parking_mgra");
        data.add("stop_period");
        data.add("trip_mode");
        data.add("av_avail");
        data.add("trip_board_tap");
        data.add("trip_alight_tap");
        data.add("set");
        data.add("tour_mode");
        data.add("driver_pnum");
        data.add("orig_escort_stoptype");
        data.add("orig_escortee_pnum");
        data.add("dest_escort_stoptype");
        data.add("dest_escortee_pnum");
        data.add("valueOfTime");
        data.add("transponder_avail");
        data.add("micro_walkMode");
        data.add("micro_trnAcc");
        data.add("micro_trnEgr");
        data.add("parkingCost");
		data.add("sampleRate");
        
        if(writeLogsums) {
        	data.add("tripModeLogsum");
        	data.add("microWalkModeLogsum");
        	data.add("microTrnAccLogsum");
        	data.add("microTrnEgrLogsum");
        }
        return data;
    }

    private List<String> formJointTripColumnNames()
    {
        List<String> data = new LinkedList<String>();
        data.add("hh_id");
        data.add("tour_id");
        data.add("stop_id");
        data.add("inbound");
        data.add("tour_purpose");
        data.add("orig_purpose");
        data.add("dest_purpose");
        data.add("orig_mgra");
        data.add("dest_mgra");
        data.add("parking_mgra");
        data.add("stop_period");
        data.add("trip_mode");
        data.add("av_avail");
        data.add("num_participants");
        data.add("trip_board_tap");
        data.add("trip_alight_tap");
        data.add("set");        
        data.add("tour_mode");
        data.add("valueOfTime");
        data.add("transponder_avail");
        //wsu remove micromobility columns, not applicable to joint trips
        //data.add("micro_walkMode");
        //data.add("micro_trnAcc");
        //data.add("micro_trnEgr");
        data.add("parkingCost");
		data.add("sampleRate");
        
        if(writeLogsums) {
        	data.add("tripModeLogsum");
        	data.add("microWalkModeLogsum");
        	data.add("microTrnAccLogsum");
        	data.add("microTrnEgrLogsum");
        }

        return data;
    }

    private List<SqliteDataTypes> formIndivTripColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
		data.add(SqliteDataTypes.REAL);
 
        if(writeLogsums) {
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        }
        return data;
    }

    private List<SqliteDataTypes> formJointTripColumnTypes()
    {
        List<SqliteDataTypes> data = new LinkedList<SqliteDataTypes>();
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.TEXT);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.INTEGER);
        data.add(SqliteDataTypes.REAL);
		data.add(SqliteDataTypes.REAL);

        if(writeLogsums) {
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        	data.add(SqliteDataTypes.REAL);
        }       
        return data;
    }

    private List<String> formIndivTripDataEntry(Stop s)
    {
        Tour t = s.getTour();
        Household h = t.getPersonObject().getHouseholdObject();
        
        List<String> data = new LinkedList<String>();
        data.add(string(t.getHhId()));
        data.add(string(t.getPersonObject().getPersonId()));
        data.add(string(t.getPersonObject().getPersonNum()));
        data.add(string(t.getTourId()));
        data.add(string(s.getStopId()));
        data.add(string(s.isInboundStop() ? 1 : 0));
        data.add(string(t.getTourPurpose()));

        if (s.getStopId() == 0)
        {
            if (s.isInboundStop())
            {
                // first trip on inbound half-tour with stops
                data.add(s.getOrigPurpose());
                data.add(s.getDestPurpose());
                data.add(string(t.getTourDestMgra()));
                data.add(string(s.getDest()));
            } else
            {
                // first trip on outbound half-tour with stops
                if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                {
                    data.add("Work");
                    data.add(s.getDestPurpose());
                } else
                {
                    data.add("Home");
                    data.add(s.getDestPurpose());
                }
                data.add(string(t.getTourOrigMgra()));
                data.add(string(s.getDest()));
            }
        } else if (s.isInboundStop() && s.getStopId() == t.getNumInboundStops() - 1)
        {
            // last trip on inbound half-tour with stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(s.getOrigPurpose());
                data.add("Work");
            } else
            {
                data.add(s.getOrigPurpose());
                data.add("Home");
            }
            data.add(string(s.getOrig()));
            data.add(string(t.getTourOrigMgra()));
        } else if (!s.isInboundStop() && s.getStopId() == t.getNumOutboundStops() - 1)
        {
            // last trip on outbound half-tour with stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(s.getOrigPurpose());
                data.add(t.getTourPurpose());
            } else
            {
                data.add(s.getOrigPurpose());
                data.add(t.getTourPurpose());
            }
            data.add(string(s.getOrig()));
            data.add(string(t.getTourDestMgra()));
        } else
        {
            data.add(s.getOrigPurpose());
            data.add(s.getDestPurpose());
            data.add(string(s.getOrig()));
            data.add(string(s.getDest()));
        }

        data.add(string(s.getPark()));
        data.add(string(s.getStopPeriod()));
        data.add(string(s.getMode()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));
        data.add(string(s.getBoardTap()));
        data.add(string(s.getAlightTap()));
        int set = setNA;
        if(modelStructure.getTripModeIsTransit(s.getMode())) {
        	set = s.getSet();
        }
        data.add(string(set));
        data.add(string(t.getTourModeChoice()));
        data.add(string(s.isInboundStop() ? t.getDriverPnumInbound() : t.getDriverPnumOutbound()));
        data.add(string(s.getEscortStopTypeOrig()));
        data.add(string(s.getEscorteePnumOrig()));
        data.add(string(s.getEscortStopTypeDest()));
        data.add(string(s.getEscorteePnumDest()));
        data.add(string(s.getValueOfTime()));
        data.add(string(h.getTpChoice()));
        data.add(string(s.getMicromobilityWalkMode()));
        data.add(string(s.getMicromobilityAccessMode()));
        data.add(string(s.getMicromobilityEgressMode()));
        data.add(string(s.getParkingCost()));
		data.add(string(t.getSampleRate()));
        
        if(writeLogsums) {
        	data.add(string(s.getModeLogsum()));
           	data.add(string(s.getMicromobilityWalkLogsum()));
           	data.add(string(s.getMicromobilityAccessLogsum()));
           	data.add(string(s.getMicromobilityEgressLogsum()));

        }
        return data;
    }

    private List<String> formJointTripDataEntry(Stop s)
    {
        Tour t = s.getTour();
        Household h = t.getPersonObject().getHouseholdObject();
        List<String> data = new LinkedList<String>();
        data.add(string(t.getHhId()));
        data.add(string(t.getTourId()));
        data.add(string(s.getStopId()));
        data.add(string(s.isInboundStop() ? 1 : 0));
        data.add(string(t.getTourPurpose()));

        if (s.getStopId() == 0)
        {
            if (s.isInboundStop())
            {
                // first trip on inbound half-tour with stops
                data.add(s.getOrigPurpose());
                data.add(s.getDestPurpose());
            } else
            {
                // first trip on outbound half-tour with stops
                if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
                {
                    data.add("Work");
                    data.add(s.getDestPurpose());
                } else
                {
                    data.add("Home");
                    data.add(s.getDestPurpose());
                }
            }
        } else if (s.isInboundStop() && s.getStopId() == t.getNumInboundStops() - 1)
        {
            // last trip on inbound half-tour with stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(s.getOrigPurpose());
                data.add("Work");
            } else
            {
                data.add(s.getOrigPurpose());
                data.add("Home");
            }
        } else if (!s.isInboundStop() && s.getStopId() == t.getNumOutboundStops() - 1)
        {
            // last trip on outbound half-tour with stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(s.getOrigPurpose());
                data.add(t.getTourPurpose());
            } else
            {
                data.add(s.getOrigPurpose());
                data.add(t.getTourPurpose());
            }
        } else
        {
            data.add(s.getOrigPurpose());
            data.add(s.getDestPurpose());
        }

        data.add(string(s.getOrig()));
        data.add(string(s.getDest()));
        data.add(string(s.getPark()));
        data.add(string(s.getStopPeriod()));
        data.add(string(s.getMode()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));

        int[] participants = t.getPersonNumArray();
        if (participants == null)
        {
            logger.error("tour participants array is null, hhid=" + t.getHhId() + ".");
            throw new RuntimeException();
        }
        if (participants.length < 2)
        {
            logger.error("length of tour participants array is not null, but is < 2; should be >= 2 for joint tour, hhid="
                    + t.getHhId() + ".");
            throw new RuntimeException();
        }

        data.add(string(participants.length));
        data.add(string(s.getBoardTap()));
        data.add(string(s.getAlightTap()));
        int set = setNA;
        if(modelStructure.getTripModeIsTransit(s.getMode())) {
        	set = s.getSet();
        }
        data.add(string(set));
        data.add(string(t.getTourModeChoice()));
        data.add(string(s.getValueOfTime()));
        data.add(string(h.getTpChoice()));
        //wsu, remove micromobility columns, not applicable to joint trips
        //data.add(string(s.getMicromobilityWalkMode()));
        //data.add(string(s.getMicromobilityAccessMode()));
        //data.add(string(s.getMicromobilityEgressMode()));
        data.add(string(s.getParkingCost()));
		data.add(string(t.getSampleRate()));
       
        if(writeLogsums) {
        	data.add(string(s.getModeLogsum()));
           	data.add(string(s.getMicromobilityWalkLogsum()));
           	data.add(string(s.getMicromobilityAccessLogsum()));
           	data.add(string(s.getMicromobilityEgressLogsum()));

        }

        
        if(writeLogsums)
        	data.add(string(s.getModeLogsum()));

        return data;
    }

    private List<String> formTourAsIndivTripDataEntry(Tour t, boolean inbound)
    {
        List<String> data = new LinkedList<String>();
        Household h = t.getPersonObject().getHouseholdObject();

        data.add(string(t.getHhId()));
        data.add(string(t.getPersonObject().getPersonId()));
        data.add(string(t.getPersonObject().getPersonNum()));
        data.add(string(t.getTourId()));
        data.add(string(-1));
        data.add(string((inbound ? 1 : 0)));
        data.add(string(t.getTourPurpose()));

        if (inbound)
        {
            // inbound trip on half-tour with no stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(t.getTourPurpose());
                data.add("Work");
            } else
            {
                data.add(t.getTourPurpose());
                data.add("Home");
            }
        } else
        {
            // outbound trip on half-tour with no stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add("Work");
                data.add(t.getTourPurpose());
            } else
            {
                data.add("Home");
                data.add(t.getTourPurpose());
            }
        }

        data.add(string((inbound ? t.getTourDestMgra() : t.getTourOrigMgra())));
        data.add(string((inbound ? t.getTourOrigMgra() : t.getTourDestMgra())));
        data.add(string(t.getTourParkMgra()));
        data.add(string(0));
        data.add(string(inbound ? t.getTourArrivePeriod() : t.getTourDepartPeriod()));
        data.add(string(t.getTourModeChoice()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));
        data.add(string(inbound ? t.getDriverPnumInbound() : t.getDriverPnumOutbound()));
        
  /*      //outbound chauffeured school tour no stops; origin stop type and escortee is zero, dest stop type is dropoff, escortee is pnum.
        if(!inbound && t.getTourPurpose().equals("School") && t.getDriverPnumOutbound()>0){
        
        	data.add(string(0));
        	data.add(string(0));
        	data.add(string(ModelStructure.ESCORT_STOP_TYPE_DROPOFF));
        	data.add(string(t.getPersonObject().getPersonNum()));
       
        }else if(inbound && t.getTourPurpose().equals("School") && t.getDriverPnumInbound()>0){
        	
        	data.add(string(ModelStructure.ESCORT_STOP_TYPE_PICKUP));
        	data.add(string(t.getPersonObject().getPersonNum()));
           	data.add(string(0));
        	data.add(string(0));
        }else 
        */
        if (!inbound && t.getDriverPnumOutbound()>0){ //outbound
        	data.add(string(0));                                          //origin = home
        	data.add(string(0));                                          //origin = home
           	Stop[] stops = t.getInboundStops();                           //there must be stops in inbound direction
           	int stopType = stops[0].getEscortStopTypeOrig();              //first inbound stop
           	int pnum = stops[0].getEscorteePnumOrig();                    //first inbound stop
           	data.add(string(stopType));                                   //destination
           	data.add(string(pnum));                                       //destination
        }else if (inbound && t.getDriverPnumInbound()>0){ //inbound
           	Stop[] stops = t.getOutboundStops();
           	int stopType = stops[stops.length-1].getEscortStopTypeOrig(); //last outbound stop
           	int pnum = stops[stops.length-1].getEscorteePnumOrig();       //last outbound stop
           	data.add(string(stopType));                                   //origin
           	data.add(string(pnum));        	                              //origin
           	data.add(string(0));                                          //destination = home
        	  data.add(string(0));                                          //destination = home
        }
        else{
        	data.add(string(0));
        	data.add(string(0));
        	data.add(string(0));
        	data.add(string(0));
        }
     
        data.add(string(t.getTourModeChoice()));
       	
       /* if(true){logger.error("Trying to write a tour as a trip");
        
        logger.info("HHID: " +t.getHhId());
        logger.info("PERSNUM: "+ t.getPersonObject().getPersonNum());
        logger.info("TOURID: "+t.getTourId());
        logger.info(inbound ? "inbound" : "outbound");
        }
    	*/
        
        data.add(string(t.getValueOfTime()));
        data.add(string(h.getTpChoice()));
        
        if(writeLogsums)
        	data.add(string(t.getTourModeLogsum()));

        return data;
    }

    private List<String> formTourAsJointTripDataEntry(Tour t, boolean inbound)
    {
        List<String> data = new LinkedList<String>();
        data.add(string(t.getHhId()));
        data.add(string(t.getTourId()));
        data.add(string(-1));
        data.add(string((inbound ? 1 : 0)));
        data.add(string(t.getTourPurpose()));

        if (inbound)
        {
            // inbound trip on half-tour with no stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add(t.getTourPurpose());
                data.add("Work");
            } else
            {
                data.add(t.getTourPurpose());
                data.add("Home");
            }
        } else
        {
            // outbound trip on half-tour with no stops
            if (t.getTourCategory().equalsIgnoreCase(ModelStructure.AT_WORK_CATEGORY))
            {
                data.add("Work");
                data.add(t.getTourPurpose());
            } else
            {
                data.add("Home");
                data.add(t.getTourPurpose());
            }
        }

        data.add(string((inbound ? t.getTourDestMgra() : t.getTourOrigMgra())));
        data.add(string((inbound ? t.getTourOrigMgra() : t.getTourDestMgra())));
        data.add(string(t.getTourParkMgra()));
        data.add(string(inbound ? t.getTourArrivePeriod() : t.getTourDepartPeriod()));
        data.add(string(t.getTourModeChoice()));
        data.add(string(t.getUseOwnedAV() ? 1 : 0));

        int[] participants = t.getPersonNumArray();
        if (participants == null)
        {
            logger.error("tour participants array is null, hhid=" + t.getHhId() + ".");
            throw new RuntimeException();
        }
        if (participants.length < 2)
        {
            logger.error("length of tour participants array is not null, but is < 2; should be >= 2 for joint tour, hhid="
                    + t.getHhId() + ".");
            throw new RuntimeException();
        }

        data.add(string(participants.length));
        data.add(string(t.getTourModeChoice()));
        data.add(string(t.getValueOfTime()));

        if(writeLogsums)
        	data.add(string(t.getTourModeLogsum()));

        return data;
    }

    private static enum SqliteDataTypes
    {
        INTEGER, TEXT, REAL
    }

    private interface DataWriter
    {
        void writeHouseholdData(List<String> data);

        void writePersonData(List<String> data);

        void writeIndivTourData(List<String> data);

        void writeJointTourData(List<String> data);

        void writeIndivTripData(List<String> data);

        void writeJointTripData(List<String> data);

        void finishActions();
    }

    private class DatabaseDataWriter
            implements DataWriter
    {
        private final String      householdTable             = rbMap.get(PROPERTIES_HOUSEHOLD_TABLE);
        private final String      personTable                = rbMap.get(PROPERTIES_PERSON_TABLE);
        private final String      indivTourTable             = rbMap.get(PROPERTIES_INDIV_TOUR_TABLE);
        private final String      jointTourTable             = rbMap.get(PROPERTIES_JOINT_TOUR_TABLE);
        private final String      indivTripTable             = rbMap.get(PROPERTIES_INDIV_TRIP_TABLE);
        private final String      jointTripTable             = rbMap.get(PROPERTIES_JOINT_TRIP_TABLE);
        private Connection        connection                 = null;
        private PreparedStatement hhPreparedStatement        = null;
        private PreparedStatement personPreparedStatement    = null;
        private PreparedStatement indivTourPreparedStatement = null;
        private PreparedStatement jointTourPreparedStatement = null;
        private PreparedStatement indivTripPreparedStatement = null;
        private PreparedStatement jointTripPreparedStatement = null;

        public DatabaseDataWriter(String dbFileName)
        {
            initializeTables(dbFileName);
        }

        private void initializeTables(String dbFileName)
        {
            Statement s = null;
            try
            {
                connection = ConnectionHelper.getConnection(dbFileName);
                s = connection.createStatement();
                s.addBatch(getTableInitializationString(householdTable, formHouseholdColumnNames(),
                        formHouseholdColumnTypes()));
                s.addBatch(getTableInitializationString(personTable, formPersonColumnNames(),
                        formPersonColumnTypes()));
                s.addBatch(getTableInitializationString(indivTourTable, formIndivTourColumnNames(),
                        formIndivTourColumnTypes()));
                s.addBatch(getTableInitializationString(jointTourTable, formJointTourColumnNames(),
                        formJointTourColumnTypes()));
                s.addBatch(getTableInitializationString(indivTripTable, formIndivTripColumnNames(),
                        formIndivTripColumnTypes()));
                s.addBatch(getTableInitializationString(jointTripTable, formJointTripColumnNames(),
                        formJointTripColumnTypes()));
                s.addBatch(getClearTableString(householdTable));
                s.addBatch(getClearTableString(personTable));
                s.addBatch(getClearTableString(indivTourTable));
                s.addBatch(getClearTableString(jointTourTable));
                s.addBatch(getClearTableString(indivTripTable));
                s.addBatch(getClearTableString(jointTripTable));
                s.executeBatch();
            } catch (SQLException e)
            {
                try
                {
                    if (connection != null) connection.close();
                } catch (SQLException ee)
                {
                    // swallow
                }
                throw new RuntimeException(e);
            } finally
            {
                closeStatement(s);
            }
            setupPreparedStatements();
        }

        private void setupPreparedStatements()
        {
            String psStart = "INSERT INTO ";
            String psMiddle = " VALUES (?";
            StringBuilder hhp = new StringBuilder(psStart);
            hhp.append(householdTable).append(psMiddle);
            for (int i = 1; i < formHouseholdColumnNames().size(); i++)
                hhp.append(",?");
            hhp.append(");");
            StringBuilder pp = new StringBuilder(psStart);
            pp.append(personTable).append(psMiddle);
            for (int i = 1; i < formPersonColumnNames().size(); i++)
                pp.append(",?");
            pp.append(");");
            StringBuilder itp = new StringBuilder(psStart);
            itp.append(indivTourTable).append(psMiddle);
            for (int i = 1; i < formIndivTourColumnNames().size(); i++)
                itp.append(",?");
            itp.append(");");
            StringBuilder jtp = new StringBuilder(psStart);
            jtp.append(jointTourTable).append(psMiddle);
            for (int i = 1; i < formJointTourColumnNames().size(); i++)
                jtp.append(",?");
            jtp.append(");");
            StringBuilder itp2 = new StringBuilder(psStart);
            itp2.append(indivTripTable).append(psMiddle);
            for (int i = 1; i < formIndivTripColumnNames().size(); i++)
                itp2.append(",?");
            itp2.append(");");
            StringBuilder jtp2 = new StringBuilder(psStart);
            jtp2.append(jointTripTable).append(psMiddle);
            for (int i = 1; i < formJointTripColumnNames().size(); i++)
                jtp2.append(",?");
            jtp2.append(");");
            try
            {
                hhPreparedStatement = connection.prepareStatement(hhp.toString());
                personPreparedStatement = connection.prepareStatement(pp.toString());
                indivTourPreparedStatement = connection.prepareStatement(itp.toString());
                jointTourPreparedStatement = connection.prepareStatement(jtp.toString());
                indivTripPreparedStatement = connection.prepareStatement(itp2.toString());
                jointTripPreparedStatement = connection.prepareStatement(jtp2.toString());
                connection.setAutoCommit(false);
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }

        private String getTableInitializationString(String table, List<String> columns,
                List<SqliteDataTypes> types)
        {
            StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            sb.append(table).append(" (");
            Iterator<String> cols = columns.iterator();
            Iterator<SqliteDataTypes> tps = types.iterator();
            sb.append(cols.next()).append(" ").append(tps.next().name());
            while (cols.hasNext())
                sb.append(",").append(cols.next()).append(" ").append(tps.next().name());
            sb.append(");");
            return sb.toString();
        }

        private String getClearTableString(String table)
        {
            return "DELETE FROM " + table + ";";
        }

        private void writeToTable(PreparedStatement ps, List<String> values)
        {
            try
            {
                int counter = 1;
                for (String value : values)
                    ps.setString(counter++, value);
                ps.executeUpdate();
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }

        // private void writeToTable(String table, List<String> values) {
        // StringBuilder sb = new StringBuilder("INSERT INTO");
        // sb.append(" ").append(table).append(" VALUES(");
        // Iterator<String> vls = values.iterator();
        // sb.append(vls.next());
        // while (vls.hasNext())
        // sb.append(",").append(vls.next());
        // sb.append(");");
        // try {
        // s.addBatch(sb.toString());
        // } catch (SQLException e) {
        // try {
        // throw new RuntimeException(e);
        // } finally {
        // try {
        // if (s != null)
        // s.close();
        // } catch (SQLException ee) {
        // //swallow
        // }
        // try {
        // if (connection != null)
        // connection.close();
        // } catch (SQLException ee) {
        // //swallow
        // }
        // }
        // }
        // }

        public void writeHouseholdData(List<String> data)
        {
            writeToTable(hhPreparedStatement, data);
        }

        public void writePersonData(List<String> data)
        {
            writeToTable(personPreparedStatement, data);
        }

        public void writeIndivTourData(List<String> data)
        {
            writeToTable(indivTourPreparedStatement, data);
        }

        public void writeJointTourData(List<String> data)
        {
            writeToTable(jointTourPreparedStatement, data);
        }

        public void writeIndivTripData(List<String> data)
        {
            writeToTable(indivTripPreparedStatement, data);
        }

        public void writeJointTripData(List<String> data)
        {
            writeToTable(jointTripPreparedStatement, data);
        }

        public void finishActions()
        {

            try
            {
                connection.commit();
            } catch (SQLException e)
            {
                throw new RuntimeException(e);
            } finally
            {
                closeStatement(hhPreparedStatement);
                closeStatement(personPreparedStatement);
                closeStatement(indivTourPreparedStatement);
                closeStatement(jointTourPreparedStatement);
                closeStatement(indivTripPreparedStatement);
                closeStatement(jointTripPreparedStatement);
                try
                {
                    if (connection != null) connection.close();
                } catch (SQLException ee)
                {
                    // swallow
                }
            }
        }

        private void closeStatement(Statement s)
        {
            try
            {
                if (s != null) s.close();
            } catch (SQLException e)
            {
                // swallow
            }
        }
    }

    private class FileDataWriter
            implements DataWriter
    {
        private final PrintWriter hhWriter;
        private final PrintWriter personWriter;
        private final PrintWriter indivTourWriter;
        private final PrintWriter jointTourWriter;
        private final PrintWriter indivTripWriter;
        private final PrintWriter jointTripWriter;

        public FileDataWriter()
        {
            String baseDir = rbMap.get(CtrampApplication.PROPERTIES_PROJECT_DIRECTORY);

            String hhFile = formFileName(rbMap.get(PROPERTIES_HOUSEHOLD_DATA_FILE), iteration);
            String personFile = formFileName(rbMap.get(PROPERTIES_PERSON_DATA_FILE), iteration);
            String indivTourFile = formFileName(rbMap.get(PROPERTIES_INDIV_TOUR_DATA_FILE),
                    iteration);
            String jointTourFile = formFileName(rbMap.get(PROPERTIES_JOINT_TOUR_DATA_FILE),
                    iteration);
            String indivTripFile = formFileName(rbMap.get(PROPERTIES_INDIV_TRIP_DATA_FILE),
                    iteration);
            String jointTripFile = formFileName(rbMap.get(PROPERTIES_JOINT_TRIP_DATA_FILE),
                    iteration);

            try
            {
                hhWriter = new PrintWriter(new File(baseDir + hhFile));
                personWriter = new PrintWriter(new File(baseDir + personFile));
                indivTourWriter = new PrintWriter(new File(baseDir + indivTourFile));
                jointTourWriter = new PrintWriter(new File(baseDir + jointTourFile));
                indivTripWriter = new PrintWriter(new File(baseDir + indivTripFile));
                jointTripWriter = new PrintWriter(new File(baseDir + jointTripFile));
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            writeHouseholdData(formHouseholdColumnNames());
            writePersonData(formPersonColumnNames());
            writeIndivTourData(formIndivTourColumnNames());
            writeJointTourData(formJointTourColumnNames());
            writeIndivTripData(formIndivTripColumnNames());
            writeJointTripData(formJointTripColumnNames());
        }

        private String formFileName(String originalFileName, int iteration)
        {
            int lastDot = originalFileName.lastIndexOf('.');

            String returnString = "";
            if (lastDot > 0)
            {
                String base = originalFileName.substring(0, lastDot);
                String ext = originalFileName.substring(lastDot);
                returnString = String.format("%s_%d%s", base, iteration, ext);
            } else
            {
                returnString = String.format("%s_%d.csv", originalFileName, iteration);
            }

            logger.info("writing household csv file to " + returnString);

            return returnString;
        }

        public void writeHouseholdData(List<String> data)
        {
            writeEntryToCsv(hhWriter, data);
        }

        public void writePersonData(List<String> data)
        {
            writeEntryToCsv(personWriter, data);
        }

        public void writeIndivTourData(List<String> data)
        {
            writeEntryToCsv(indivTourWriter, data);
        }

        public void writeJointTourData(List<String> data)
        {
            writeEntryToCsv(jointTourWriter, data);
        }

        public void writeIndivTripData(List<String> data)
        {
            writeEntryToCsv(indivTripWriter, data);
        }

        public void writeJointTripData(List<String> data)
        {
            writeEntryToCsv(jointTripWriter, data);
        }

        private void writeEntryToCsv(PrintWriter pw, List<String> data)
        {
            pw.println(formCsvString(data));
        }

        private String formCsvString(List<String> data)
        {
            char delimiter = ',';
            Iterator<String> it = data.iterator();
            StringBuilder sb = new StringBuilder(it.next());
            while (it.hasNext())
                sb.append(delimiter).append(it.next());
            return sb.toString();
        }

        public void finishActions()
        {
            try
            {
                hhWriter.flush();
                personWriter.flush();
                indivTourWriter.flush();
                jointTourWriter.flush();
                indivTripWriter.flush();
                jointTripWriter.flush();
            } finally
            {
                hhWriter.close();
                personWriter.close();
                indivTourWriter.close();
                jointTourWriter.close();
                indivTripWriter.close();
                jointTripWriter.close();
            }

        }
    }

    private ArrayList<int[]> getWriteHouseholdRanges(int numberOfHouseholds)
    {

        ArrayList<int[]> startEndIndexList = new ArrayList<int[]>();

        int startIndex = 0;
        int endIndex = 0;

        while (endIndex < numberOfHouseholds - 1)
        {
            endIndex = startIndex + NUM_WRITE_PACKETS - 1;
            if (endIndex + NUM_WRITE_PACKETS > numberOfHouseholds)
                endIndex = numberOfHouseholds - 1;

            int[] startEndIndices = new int[2];
            startEndIndices[0] = startIndex;
            startEndIndices[1] = endIndex;
            startEndIndexList.add(startEndIndices);

            startIndex += NUM_WRITE_PACKETS;
        }

        return startEndIndexList;

    }

    /**
     * Calculate auto skims for a given origin to all destination mgras, and
     * return auto distance.
     * 
     * @param oMgra
     *            The origin mgra
     * @return An array of distances
     */
    private double calculateDistancesForAllMgras(int oMgra, int dMgra)
    {

        int oTaz = mgraManager.getTaz(oMgra);
        int dTaz = mgraManager.getTaz(dMgra);

        iv.setOriginZone(oTaz);
        iv.setDestZone(dTaz);

        // sov time in results[0] and distance in resuls[1]
        double[] results = autoSkimUEC.solve(iv, dmu, null);

        return results[1];
    }
}

