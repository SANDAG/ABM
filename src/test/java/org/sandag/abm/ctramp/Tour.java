package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.ArrayList;

import org.apache.log4j.Logger;

public class Tour
        implements Serializable
{

    private Person                perObj;
    private Household             hhObj;

    private String                tourCategory;
    private String                tourPurpose;
    private String                subtourPurpose;

    // use this array to hold personNum (i.e. index values for Household.persons
    // array) for persons in tour.
    // for individual tour types, this array is null.
    // for joint tours, there will be an entry for each participating person.
    private int[]                 personNumArray;

    // alternative number chosen by the joint tour composition model ( 1=adults,
    // 2=children, 3=mixed ).
    private int                   jointTourComposition;

    private int                   tourId;
    private int                   tourOrigMgra;
    private int                   tourDestMgra;
    private int                   tourOrigWalkSubzone;
    private int                   tourDestWalkSubzone;
    private int                   tourDepartPeriod;
    private int                   tourArrivePeriod;
    private int                   tourMode;
    private int                   subtourFreqChoice;
    private int                   tourParkMgra;
    
    private float                 timeOfDayLogsum;
    private float                 tourModeLogsum;
    private float                 subtourFreqLogsum;
    private float                 tourDestinationLogsum;
    private float                 stopFreqLogsum;
    

    private int                   tourPrimaryPurposeIndex;

    private float[]               tourModalProbabilities;
    private float[]               tourModalUtilities;

    private int                   stopFreqChoice;
    private Stop[]                outboundStops;
    private Stop[]                inboundStops;
   
    private ArrayList<Float>      outboundStopDestinationLogsums;
    private ArrayList<Float>      inboundStopDestinationLogsums;

    //Dimension N-path by 3 - 0=btap, 1=atap, 2=skim set , 3=utility
    private double[][]   bestWtwTapPairsOut;
   

    private double[][]   bestWtwTapPairsIn;
    private double[][]   bestWtdTapPairsOut;
    private double[][]   bestWtdTapPairsIn;
    private double[][]   bestDtwTapPairsOut;
    private double[][]   bestDtwTapPairsIn;
    
    private int       choosenTransitPathIn;

    private int       choosenTransitPathOut;
    
    private boolean   useOwnedAV;

    private double valueOfTime;

    private int escortTypeOutbound;
    private int escortTypeInbound;
    private int driverPnumOutbound;
    private int driverPnumInbound;
	
	private float sampleRate;

    // this constructor used for mandatory tour creation
    public Tour(Person perObj, int tourId, int primaryIndex)
    {
        hhObj = perObj.getHouseholdObject();
        this.perObj = perObj;
        this.tourId = tourId;
        tourCategory = ModelStructure.MANDATORY_CATEGORY;
        tourPrimaryPurposeIndex = primaryIndex;
		if(hhObj!=null)
			this.sampleRate = perObj.getSampleRate();
        
        outboundStopDestinationLogsums = new ArrayList<Float>();
        inboundStopDestinationLogsums = new ArrayList<Float>();
   }

    // this constructor used for joint tour creation
    public Tour(Household hhObj, String tourPurpose, String category, int primaryIndex)
    {
        this.hhObj = hhObj;
        this.tourPurpose = tourPurpose;
        tourCategory = category;
        tourPrimaryPurposeIndex = primaryIndex;
		if(hhObj!=null)
			this.sampleRate = hhObj.getSampleRate();
		
        outboundStopDestinationLogsums = new ArrayList<Float>();
        inboundStopDestinationLogsums = new ArrayList<Float>();
    }

    // this constructor used for individual non-mandatory or at-work subtour
    // creation
    public Tour(int id, Household hhObj, Person persObj, String tourPurpose, String category,
            int primaryIndex)
    {
        this.hhObj = hhObj;
        this.perObj = persObj;
        tourId = id;
        this.tourPurpose = tourPurpose;
        tourCategory = category;
        tourPrimaryPurposeIndex = primaryIndex;
		if(hhObj!=null)
			this.sampleRate = hhObj.getSampleRate();
		
        outboundStopDestinationLogsums = new ArrayList<Float>();
        inboundStopDestinationLogsums = new ArrayList<Float>();
    }

    public Person getPersonObject()
    {
        return perObj;
    }

    public void setPersonObject(Person p)
    {
        perObj = p;
    }

    public void setPersonNumArray(int[] personNums)
    {
        personNumArray = personNums;
    }

    public int[] getPersonNumArray()
    {
        return personNumArray;
    }

    public boolean getPersonInJointTour(Person person)
    {
        boolean inTour = false;
        for (int num : personNumArray)
        {
            if (person.getPersonNum() == num)
            {
                inTour = true;
                break;
            }
        }
        return inTour;
    }

    public void setJointTourComposition(int compositionAlternative)
    {
        jointTourComposition = compositionAlternative;
    }

    public int getJointTourComposition()
    {
        return jointTourComposition;
    }

    public void setTourPurpose(String name)
    {
        tourPurpose = name;
    }

    public void setSubTourPurpose(String name)
    {
        subtourPurpose = name;
    }

    public String getSubTourPurpose()
    {
        return subtourPurpose;
    }

    public String getTourCategory()
    {
        return tourCategory;
    }

    public String getTourPurpose()
    {
        return tourPurpose;
    }

    public String getTourPrimaryPurpose()
    {
        int index = tourPurpose.indexOf('_');
        if (index < 0) return tourPurpose;
        else return tourPurpose.substring(0, index);
    }

    // public int getTourPurposeIndex() {
    // return tourPurposeIndex;
    // }

    public int getTourPrimaryPurposeIndex()
    {
        return tourPrimaryPurposeIndex;
    }

    public int getTourModeChoice()
    {
        return tourMode;
    }

    public void setTourId(int id)
    {
        tourId = id;
    }

    public void setTourOrigMgra(int origMgra)
    {
        tourOrigMgra = origMgra;
    }

    public void setTourDestMgra(int destMgra)
    {
        tourDestMgra = destMgra;
    }

    public void setTourOrigWalkSubzone(int subzone)
    {
        tourOrigWalkSubzone = subzone;
    }

    public void setTourDestWalkSubzone(int subzone)
    {
        tourDestWalkSubzone = subzone;
    }

    public void setTourDepartPeriod(int departPeriod)
    {
        tourDepartPeriod = departPeriod;
    }

    public void setTourArrivePeriod(int arrivePeriod)
    {
        tourArrivePeriod = arrivePeriod;
    }

    public void setTourModeChoice(int modeIndex)
    {
        tourMode = modeIndex;
    }

    public void setTourParkMgra(int parkMgra)
    {
        tourParkMgra = parkMgra;
    }

    // methods DMU will use to get info from household object

    public int getTourOrigMgra()
    {
        return tourOrigMgra;
    }

    public int getTourDestMgra()
    {
        return tourDestMgra;
    }

    public int getTourOrigWalkSubzone()
    {
        return tourOrigWalkSubzone;
    }

    public int getTourDestWalkSubzone()
    {
        return tourDestWalkSubzone;
    }

    public int getTourDepartPeriod()
    {
        return tourDepartPeriod;
    }

    public int getTourArrivePeriod()
    {
        return tourArrivePeriod;
    }

    public int getTourParkMgra()
    {
        return tourParkMgra;
    }

    public int getHhId()
    {
        return hhObj.getHhId();
    }

    public int getHhMgra()
    {
        return hhObj.getHhMgra();
    }

    public int getTourId()
    {
        return tourId;
    }

    public int getWorkTourIndexFromSubtourId(int subtourIndex)
    {
        // when subtour was created, it's purpose index was set to 10*work
        // purpose
        // index + at-work subtour index
        return subtourIndex / 10;
    }

    public int getSubtourIndexFromSubtourId(int subtourIndex)
    {
        // when subtour was created, it's purpose index was set to 10*work
        // purpose
        // index + at-work subtour index
        int workTourIndex = subtourIndex / 10;
        return subtourIndex - 10 * workTourIndex;
    }

    public void setSubtourFreqChoice(int choice)
    {
        subtourFreqChoice = choice;
    }

    public int getSubtourFreqChoice()
    {
        return subtourFreqChoice;
    }

    public void setStopFreqChoice(int chosenAlt)
    {
        stopFreqChoice = chosenAlt;
    }

    public int getStopFreqChoice()
    {
        return stopFreqChoice;
    }

    public void createOutboundStops(String[] stopOrigPurposes, String[] stopDestPurposes,
            int[] stopPurposeIndex)
    {
        outboundStops = new Stop[stopOrigPurposes.length];
        for (int i = 0; i < stopOrigPurposes.length; i++)
            outboundStops[i] = new Stop(this, stopOrigPurposes[i], stopDestPurposes[i], i, false,
                    stopPurposeIndex[i]);
    }

    public void createInboundStops(String[] stopOrigPurposes, String[] stopDestPurposes,
            int[] stopPurposeIndex)
    {
        // needs outbound stops to be created first to get id numbering correct

        inboundStops = new Stop[stopOrigPurposes.length];
        for (int i = 0; i < stopOrigPurposes.length; i++)
            inboundStops[i] = new Stop(this, stopOrigPurposes[i], stopDestPurposes[i], i, true,
                    stopPurposeIndex[i]);
    }

    /**
     * Create a Stop object to represent a half-tour where no stops were
     * generated. The id for the stop is set to -1 so that trips for half-tours
     * without stops can be distinguished in the output trip files from turs
     * that have stops. Trips for these tours come from stop objects with ids in
     * the range 0,...,3.
     * 
     * @param origPurp
     *            is "home" or "work" (for at-work subtours) if outbound, or the
     *            primary tour purpose if inbound
     * @param destPurp
     *            is "home" or "work" (for at-work subtours) if inbound, or the
     *            primary tour purpose if outbound
     * @param inbound
     *            is true if the half-tour is inbound, or false if outbound.
     * @return the created Stop object.
     */
    public Stop createStop(String origPurp, String destPurp,
            boolean inbound, boolean subtour)
    {
        Stop stop = null;
        int id = -1;
        if (inbound)
        {
            inboundStops = new Stop[1];
            inboundStops[0] = new Stop(this, origPurp, destPurp, id, inbound, 0);
            stop = inboundStops[0];
        } else
        {
            outboundStops = new Stop[1];
            outboundStops[0] = new Stop(this, origPurp, destPurp, id, inbound, 0);
            stop = outboundStops[0];
        }
        return stop;
    }

    public int getNumOutboundStops()
    {
        if (outboundStops == null) return 0;
        else return outboundStops.length;
    }

    public int getNumInboundStops()
    {
        if (inboundStops == null) return 0;
        else return inboundStops.length;
    }

    public Stop[] getOutboundStops()
    {
        return outboundStops;
    }

    public Stop[] getInboundStops()
    {
        return inboundStops;
    }

    public void clearStopModelResults()
    {
        stopFreqChoice = 0;
        outboundStops = null;
        inboundStops = null;
    }

    public String getTourWindow(String purposeAbbreviation)
    {
        String returnString = String.format("      %5s:     |", purposeAbbreviation);
        short[] windows = perObj.getTimeWindows();
        for (int i = 1; i < windows.length; i++)
        {
            String tempString = String.format("%s",
                    i >= tourDepartPeriod && i <= tourArrivePeriod ? purposeAbbreviation : "    ");
            if (tempString.length() == 2 || tempString.length() == 3)
                tempString = " " + tempString;
            returnString += String.format("%4s|", tempString);
        }
        return returnString;
    }

    public int getEscortTypeOutbound() {
		return escortTypeOutbound;
	}
	public void setEscortTypeOutbound(int escortType) {
		this.escortTypeOutbound = escortType;
	}
	public int getEscortTypeInbound() {
		return escortTypeInbound;
	}
	public void setEscortTypeInbound(int escortType) {
		this.escortTypeInbound = escortType;
	}
	public int getDriverPnumOutbound() {
		return driverPnumOutbound;
	}
	public void setDriverPnumOutbound(int driverPnum) {
		this.driverPnumOutbound = driverPnum;
	}
	public int getDriverPnumInbound() {
		return driverPnumInbound;
	}
	public void setDriverPnumInbound(int driverPnum) {
		this.driverPnumInbound = driverPnum;
	}
    public void logTourObject(Logger logger, int totalChars)
    {

        String personNumArrayString = "-";
        if (personNumArray != null)
        {
            personNumArrayString = "[ ";
            personNumArrayString += String.format("%d", personNumArray[0]);
            for (int i = 1; i < personNumArray.length; i++)
                personNumArrayString += String.format(", %d", personNumArray[i]);
            personNumArrayString += " ]";
        }

        Household.logHelper(logger, "tourId: ", tourId, totalChars);
        Household.logHelper(logger, "tourCategory: ", tourCategory, totalChars);
        Household.logHelper(logger, "tourPurpose: ", tourPurpose, totalChars);
        Household.logHelper(logger, "tourPurposeIndex: ", tourPrimaryPurposeIndex, totalChars);
        Household.logHelper(logger, "personNumArray: ", personNumArrayString, totalChars);
        Household.logHelper(logger, "jointTourComposition: ", jointTourComposition, totalChars);
        Household.logHelper(logger, "tourOrigMgra: ", tourOrigMgra, totalChars);
        Household.logHelper(logger, "tourDestMgra: ", tourDestMgra, totalChars);
        Household.logHelper(logger, "tourOrigWalkSubzone: ", tourOrigWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDestWalkSubzone: ", tourDestWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", tourDepartPeriod, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", tourArrivePeriod, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
        Household.logHelper(logger, "escortTypeOutbound: ", escortTypeOutbound, totalChars);
        Household.logHelper(logger, "driverPnumOutbound: ", driverPnumOutbound, totalChars);
        Household.logHelper(logger, "escortTypeInbound: ", escortTypeInbound, totalChars);
        Household.logHelper(logger, "driverPnumInbound: ", driverPnumInbound, totalChars);
        Household.logHelper(logger, "stopFreqChoice: ", stopFreqChoice, totalChars);

        String tempString = String.format("outboundStops[%s]:",
                outboundStops == null ? "" : String.valueOf(outboundStops.length));
        logger.info(tempString);

        tempString = String.format("inboundStops[%s]:",
                inboundStops == null ? "" : String.valueOf(inboundStops.length));
        logger.info(tempString);

        if ( bestWtwTapPairsOut == null ) {
            tempString = "bestWtwTapPairsOut: no tap pairs saved";
        }
        else {
            if ( bestWtwTapPairsOut[0] == null )
                tempString = "bestWtwTapPairsOut: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestWtwTapPairsOut: " + 0 + "[" + bestWtwTapPairsOut[0][0] + "," + bestWtwTapPairsOut[0][1] + "," + bestWtwTapPairsOut[0][2] + "]";
            for (int i=1; i < bestWtwTapPairsOut.length; i++)
                if ( bestWtwTapPairsOut[i] == null )
                    tempString += ", " + i + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestWtwTapPairsOut[i][0] + "," + bestWtwTapPairsOut[i][1] + "," + bestWtwTapPairsOut[0][2] + "]";
                        
            tempString += ", choosenTransitPathOut: " + choosenTransitPathOut;
        }
        logger.info(tempString);

        if ( bestWtwTapPairsIn == null ) {
            tempString = "bestWtwTapPairsIn: no tap pairs saved";
        }
        else {
            if ( bestWtwTapPairsIn[0] == null )
                tempString = "bestWtwTapPairsIn: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestWtwTapPairsIn: " + 0 + "[" + bestWtwTapPairsIn[0][0] + "," + bestWtwTapPairsIn[0][1] + "," + bestWtwTapPairsIn[0][2] + "]";
            for (int i=1; i < bestWtwTapPairsIn.length; i++)
                if ( bestWtwTapPairsIn[i] == null )
                    tempString += ", " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestWtwTapPairsIn[i][0] + "," + bestWtwTapPairsIn[i][1] + "," + bestWtwTapPairsIn[0][2] + "]";
            
            tempString += ", choosenTransitPathIn: " + choosenTransitPathIn;
        }
        logger.info(tempString);

        if ( bestWtdTapPairsOut == null ) {
            tempString = "bestWtdTapPairsOut: no tap pairs saved";
        }
        else {
            if ( bestWtdTapPairsOut[0] == null )
                tempString = "bestWtdTapPairsOut: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestWtdTapPairsOut: " + 0 + "[" + bestWtdTapPairsOut[0][0] + "," + bestWtdTapPairsOut[0][1] + "," + bestWtdTapPairsOut[0][2] + "]";
            for (int i=1; i < bestWtdTapPairsOut.length; i++)
                if ( bestWtdTapPairsOut[i] == null )
                    tempString += ", " + i + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestWtdTapPairsOut[i][0] + "," + bestWtdTapPairsOut[i][1] + "," + bestWtdTapPairsOut[0][2] + "]";
            
            tempString += ", choosenTransitPathOut: " + choosenTransitPathOut;
        }
        logger.info(tempString);

        if ( bestWtdTapPairsIn == null ) {
            tempString = "bestWtdTapPairsIn: no tap pairs saved";
        }
        else {
            if ( bestWtdTapPairsIn[0] == null )
                tempString = "bestWtdTapPairsIn: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestWtdTapPairsIn: " + 0 + "[" + bestWtdTapPairsIn[0][0] + "," + bestWtdTapPairsIn[0][1] + "," + bestWtdTapPairsIn[0][2] + "]";
            for (int i=1; i < bestWtdTapPairsIn.length; i++)
                if ( bestWtdTapPairsIn[i] == null )
                    tempString += ", " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestWtdTapPairsIn[i][0] + "," + bestWtdTapPairsIn[i][1] + "," + bestWtdTapPairsIn[0][2] + "]";
            
            tempString += ", choosenTransitPathIn: " + choosenTransitPathIn;
        }
        logger.info(tempString);

        if ( bestDtwTapPairsOut == null ) {
            tempString = "bestDtwTapPairsOut: no tap pairs saved";
        }
        else {
            if ( bestDtwTapPairsOut[0] == null )
                tempString = "bestDtwTapPairsOut: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestDtwTapPairsOut: " + 0 + "[" + bestDtwTapPairsOut[0][0] + "," + bestDtwTapPairsOut[0][1] + "," + bestDtwTapPairsOut[0][2] + "]";
            for (int i=1; i < bestDtwTapPairsOut.length; i++)
                if ( bestDtwTapPairsOut[i] == null )
                    tempString += ", " + i + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestDtwTapPairsOut[i][0] + "," + bestDtwTapPairsOut[i][1] + "," + bestDtwTapPairsOut[0][2] + "]";
            
            tempString += ", choosenTransitPathOut: " + choosenTransitPathOut;
        }
        logger.info(tempString);

        if ( bestDtwTapPairsIn == null ) {
            tempString = "bestDtwTapPairsIn: no tap pairs saved";
        }
        else {
            if ( bestDtwTapPairsIn[0] == null )
                tempString = "bestDtwTapPairsIn: " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
            else
                tempString = "bestDtwTapPairsIn: " + 0 + "[" + bestDtwTapPairsIn[0][0] + "," + bestDtwTapPairsIn[0][1] + "," + bestDtwTapPairsIn[0][2] + "]";
            for (int i=1; i < bestDtwTapPairsIn.length; i++)
                if ( bestDtwTapPairsIn[i] == null )
                    tempString += ", " + 0 + "[" + "none" + "," + "none" + "," + "none" + "]";
                else
                    tempString += ", " + i + "[" + bestDtwTapPairsIn[i][0] + "," + bestDtwTapPairsIn[i][1] + "," + bestDtwTapPairsIn[0][2] + "]";
            
            tempString += ", choosenTransitPathIn: " + choosenTransitPathIn;
        }
        logger.info(tempString);

    }

    public void logEntireTourObject(Logger logger)
    {

        int totalChars = 60;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        String personNumArrayString = "-";
        if (personNumArray != null)
        {
            personNumArrayString = "[ ";
            personNumArrayString += String.format("%d", personNumArray[0]);
            for (int i = 1; i < personNumArray.length; i++)
                personNumArrayString += String.format(", %d", personNumArray[i]);
            personNumArrayString += " ]";
        }

        Household.logHelper(logger, "tourId: ", tourId, totalChars);
        Household.logHelper(logger, "tourCategory: ", tourCategory, totalChars);
        Household.logHelper(logger, "tourPurpose: ", tourPurpose, totalChars);
        Household.logHelper(logger, "tourPurposeIndex: ", tourPrimaryPurposeIndex, totalChars);
        Household.logHelper(logger, "personNumArray: ", personNumArrayString, totalChars);
        Household.logHelper(logger, "jointTourComposition: ", jointTourComposition, totalChars);
        Household.logHelper(logger, "tourOrigMgra: ", tourOrigMgra, totalChars);
        Household.logHelper(logger, "tourDestMgra: ", tourDestMgra, totalChars);
        Household.logHelper(logger, "tourOrigWalkSubzone: ", tourOrigWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDestWalkSubzone: ", tourDestWalkSubzone, totalChars);
        Household.logHelper(logger, "tourDepartPeriod: ", tourDepartPeriod, totalChars);
        Household.logHelper(logger, "tourArrivePeriod: ", tourArrivePeriod, totalChars);
        Household.logHelper(logger, "driverPnumOutbound: ", driverPnumOutbound, totalChars);
        Household.logHelper(logger, "escortTypeInbound: ", escortTypeInbound, totalChars);
        Household.logHelper(logger, "driverPnumInbound: ", driverPnumInbound, totalChars);
        Household.logHelper(logger, "tourMode: ", tourMode, totalChars);
        Household.logHelper(logger, "stopFreqChoice: ", stopFreqChoice, totalChars);

        if (outboundStops != null)
        {
            logger.info("Outbound Stops:");
            if (outboundStops.length > 0)
            {
                for (int i = 0; i < outboundStops.length; i++)
                    outboundStops[i].logStopObject(logger, totalChars);
            } else
            {
                logger.info("     No outbound stops");
            }
        } else
        {
            logger.info("     No outbound stops");
        }

        if (inboundStops != null)
        {
            logger.info("Inbound Stops:");
            if (inboundStops.length > 0)
            {
                for (int i = 0; i < inboundStops.length; i++)
                    inboundStops[i].logStopObject(logger, totalChars);
            } else
            {
                logger.info("     No inbound stops");
            }
        } else
        {
            logger.info("     No inbound stops");
        }

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void setTourModalUtilities(float[] utils)
    {
        tourModalUtilities = utils;
    }

    public float[] getTourModalUtilities()
    {
        return tourModalUtilities;
    }

    public void setTourModalProbabilities(float[] probs)
    {
        tourModalProbabilities = probs;
    }

    public float[] getTourModalProbabilities()
    {
        return tourModalProbabilities;
    }

    public void setBestWtwTapPairsOut(double[][] tapPairArray)
    {
        bestWtwTapPairsOut = tapPairArray;
    }

    public void setBestWtwTapPairsIn(double[][] tapPairArray)
    {
        bestWtwTapPairsIn = tapPairArray;
    }

    public void setBestWtdTapPairsOut(double[][] tapPairArray)
    {
        bestWtdTapPairsOut = tapPairArray;
    }

    public void setBestWtdTapPairsIn(double[][] tapPairArray)
    {
        bestWtdTapPairsIn = tapPairArray;
    }

    public void setBestDtwTapPairsOut(double[][] tapPairArray)
    {
        bestDtwTapPairsOut = tapPairArray;
    }

    public void setBestDtwTapPairsIn(double[][] tapPairArray)
    {
        bestDtwTapPairsIn = tapPairArray;
    }

    public void setChoosenTransitPathIn( int path )
    {
    	choosenTransitPathIn = path;
    }
    public void setChoosenTransitPathOut( int path )
    {
    	choosenTransitPathOut = path;
    }
    public double[][] getBestWtwTapPairsOut()
   {
        return bestWtwTapPairsOut;
    }

    public double[][] getBestWtwTapPairsIn()
    {
        return bestWtwTapPairsIn;
    }

    public double[][] getBestWtdTapPairsOut()
    {
        return bestWtdTapPairsOut;
    }

    public double[][] getBestWtdTapPairsIn()
    {
        return bestWtdTapPairsIn;
    }

    public double[][] getBestDtwTapPairsOut()
    {
        return bestDtwTapPairsOut;
    }

    public double[][] getBestDtwTapPairsIn()
    {
        return bestDtwTapPairsIn;
    }

	public double getValueOfTime() {
		return valueOfTime;
	}

	public void setValueOfTime(double valueOfTime) {
		this.valueOfTime = valueOfTime;
	}

	public float getTimeOfDayLogsum() {
		return timeOfDayLogsum;
	}

	public void setTimeOfDayLogsum(float timeOfDayLogsum) {
		this.timeOfDayLogsum = timeOfDayLogsum;
	}

	public float getTourModeLogsum() {
		return tourModeLogsum;
	}

	public void setTourModeLogsum(float tourModeLogsum) {
		this.tourModeLogsum = tourModeLogsum;
	}

	public float getSubtourFreqLogsum() {
		return subtourFreqLogsum;
	}

	public void setSubtourFreqLogsum(float subtourFreqLogsum) {
		this.subtourFreqLogsum = subtourFreqLogsum;
	}

	public float getTourDestinationLogsum() {
		return tourDestinationLogsum;
	}

	public void setTourDestinationLogsum(float tourDestinationLogsum) {
		this.tourDestinationLogsum = tourDestinationLogsum;
	}

	public float getStopFreqLogsum() {
		return stopFreqLogsum;
	}

	public void setStopFreqLogsum(float stopFreqLogsum) {
		this.stopFreqLogsum = stopFreqLogsum;
	}

	public ArrayList<Float> getOutboundStopDestinationLogsums(){
		return outboundStopDestinationLogsums;
	}
	public ArrayList<Float> getInboundStopDestinationLogsums(){
		return inboundStopDestinationLogsums;
	}
	
	public void addOutboundStopDestinationLogsum(float logsum){
		outboundStopDestinationLogsums.add(logsum);
	}

	public void addInboundStopDestinationLogsum(float logsum){
		inboundStopDestinationLogsums.add(logsum);
	}
	
	public boolean getUseOwnedAV() {
		return useOwnedAV;
	}

	public void setUseOwnedAV(boolean useOwnedAV) {
		this.useOwnedAV = useOwnedAV;
	}

	/**
	 * Iterate through persons on tour and return non-work time factor
	 * for oldest person. If the person array is null then return 1.0.
	 * 
	 * @return Time factor for oldest person on joint tour.
	 */
	public double getJointTourTimeFactor() {
		int[] personNumArray = getPersonNumArray();
		int oldestAge = -999;
		Person oldestPerson = null;
		for (int num : personNumArray){
			Person p = 	hhObj.getPerson(num);
			if(p.getAge() > oldestAge){
				oldestPerson = p;
				oldestAge = p.getAge();
			}
	    }
		if(oldestPerson != null)
			return oldestPerson.getTimeFactorNonWork();
		
		return 1.0;
	}
	
	public float getSampleRate()
    {
    	return this.sampleRate;
    }
    
    public void setSampleRate(float aSampleRate) 
    {
    	this.sampleRate = aSampleRate;
    }


}