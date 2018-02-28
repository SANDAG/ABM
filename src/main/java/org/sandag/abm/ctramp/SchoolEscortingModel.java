/*
* The school-escort model was designed by PB (Gupta, Vovsha, et al)
* as part of the Maricopa Association of Governments (MAG)
* Activity-based Travel Model Development project.  
* 
* This source code, which implements the school escort model,
* was written exclusively for and funded by MAG as part of the 
* same project; therefore, per their contract, the 
* source code belongs to MAG and can only be used with their 
* permission.      
*
* It is being adapted for the Southern Oregon ABM by PB & RSG
* with permission from MAG and all references to
* the school escort model as well as source code adapted from this 
* original code should credit MAG's role in its development.
*
* The escort model and source code should not be transferred to or 
* adapted for other agencies or used in other projects without 
* expressed permission from MAG. 
*
* The source code has been substantially revised to fit within the 
* SANDAG\MTC\ODOT CT-RAMP model structure by RSG (2015).
*/

package  org.sandag.abm.ctramp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.modechoice.MgraDataManager;

import com.pb.common.calculator.VariableTable;
import com.pb.common.calculator.IndexValues;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import com.pb.common.newmodel.ChoiceModelApplication;
import com.pb.common.newmodel.UtilityExpressionCalculator;
import com.pb.common.util.IndexSort;
import com.pb.common.util.PropertyMap;


public class SchoolEscortingModel {

    public static final String ALT_TABLE_BUNDLE1_NAME = "bundle1";
    public static final String ALT_TABLE_BUNDLE2_NAME = "bundle2";
    public static final String ALT_TABLE_BUNDLE3_NAME = "bundle3";
    public static final String ALT_TABLE_CHAUF1_NAME = "chauf1";
    public static final String ALT_TABLE_CHAUF2_NAME = "chauf2";
    public static final String ALT_TABLE_CHAUF3_NAME = "chauf3";
    public static final String ALT_TABLE_NBUNDLES_NAME = "nbundles";
    public static final String ALT_TABLE_NBUNDLES_RS1_NAME = "nrs1";
    public static final String ALT_TABLE_NBUNDLES_ES1_NAME = "npe1";
    public static final String ALT_TABLE_NBUNDLES_RS2_NAME = "nrs2";
    public static final String ALT_TABLE_NBUNDLES_ES2_NAME = "npe2";
    public static final String ALT_TABLE_BUNDLE_INCIDENCE_FIRST_COLUMN_NAME = "b1_rs1_1";
    

    public static final int NUM_ESCORTEES_PER_HH = 3;
    public static final int NUM_CHAUFFEURS_PER_HH = 2;
    public static final int NUM_ESCORT_TYPES = 2;
    public static final int NUM_BUNDLES = 3;
    
    
    public static final int ESCORT_ELIGIBLE = 1;
    
    public static final int CHAUFFEUR_1 = 1;
    public static final int CHAUFFEUR_2 = 2;
    
    public static final int DIR_OUTBOUND = 1;
    public static final int DIR_INBOUND = 2;
    
    public static final int RIDE_SHARING_CHAUFFEUR_1 = 1;
    public static final int PURE_ESCORTING_CHAUFFEUR_1 = 2;
    public static final int RIDE_SHARING_CHAUFFEUR_2 = 3;
    public static final int PURE_ESCORTING_CHAUFFEUR_2 = 4;
    
    
    
    // chauffeur priority lookup values determined by 100*PersonTypesOld(1-8) + 10*gender(1-2) + 1*age > 25
    private static final int PT_WEIGHT = 100;
    private static final int G_WEIGHT = 10;
    private static final int A_WEIGHT = 1;
    
	public static final int RESULT_CHILD_DIRECTION_FIELD = 0;
	public static final int RESULT_CHILD_CHOSEN_ALT_FIELD = 1;
	public static final int RESULT_CHILD_HHID_FIELD = 2;
	public static final int RESULT_CHILD_PNUM_FIELD = 3;
	public static final int RESULT_CHILD_PID_FIELD = 4;
	public static final int RESULT_CHILD_PERSON_TYPE_FIELD = 5;
	public static final int RESULT_CHILD_AGE_FIELD = 6;
	public static final int RESULT_CHILD_CDAP_FIELD = 7;
	public static final int RESULT_CHILD_SCHOOL_AT_HOME_FIELD = 8;
	public static final int RESULT_CHILD_SCHOOL_LOC_FIELD = 9;
	public static final int RESULT_CHILD_ESCORT_ELIGIBLE_FIELD = 10;
	public static final int RESULT_CHILD_DEPART_FROM_HOME_FIELD = 11;
	public static final int RESULT_CHILD_DEPART_TO_HOME_FIELD = 12;
	public static final int RESULT_CHILD_DIST_TO_SCHOOL_FIELD = 13;
	public static final int RESULT_CHILD_DIST_FROM_SCHOOL_FIELD = 14;
	public static final int RESULT_CHILD_ADULT1_DEPART_FROM_HOME_FIELD = 15;
	public static final int RESULT_CHILD_ADULT1_DEPART_TO_HOME_FIELD = 16;
	public static final int RESULT_CHILD_ADULT2_DEPART_FROM_HOME_FIELD = 17;
	public static final int RESULT_CHILD_ADULT2_DEPART_TO_HOME_FIELD = 18;
	public static final int RESULT_CHILD_ESCORT_TYPE_FIELD = 19;
	public static final int RESULT_CHILD_BUNDLE_ID_FIELD = 20;
	public static final int RESULT_CHILD_CHILD_ID_FIELD = 21;
	public static final int RESULT_CHILD_CHAUFFEUR_ID_FIELD = 22;
	public static final int RESULT_CHILD_CHAUFFEUR_PNUM_FIELD = 23;
	public static final int RESULT_CHILD_CHAUFFEUR_PID_FIELD = 24;
	public static final int RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD = 25;
	public static final int RESULT_CHILD_CHAUFFEUR_DEPART_HOME_FIELD = 26;
	public static final int RESULT_CHILD_CHAUFFEUR_DEPART_WORK_FIELD = 27;
	public static final int RESULT_CHILD_RANDOM_NUM_FIELD = 28;
	public static final int NUM_RESULTS_BY_CHILD_FIELDS = 29;

	
	public static final int RESULT_CHAUF_BUNDLE_ID_FIELD = 0;
	public static final int RESULT_CHAUF_DIRECTION_FIELD = 1;
	public static final int RESULT_CHAUF_CHOSEN_ALT_FIELD = 2;
	public static final int RESULT_CHAUF_HHID_FIELD = 3;
	public static final int RESULT_CHAUF_PNUM_FIELD = 4;
	public static final int RESULT_CHAUF_PID_FIELD = 5;
	public static final int RESULT_CHAUF_PERSON_TYPE_FIELD = 6;
	public static final int RESULT_CHAUF_AGE_FIELD = 7;
	public static final int RESULT_CHAUF_CDAP_FIELD = 8;
	public static final int RESULT_CHAUF_ESCORT_ELIGIBLE_FIELD = 9;
	public static final int RESULT_CHAUF_DEPART_HOME_FIELD = 10;
	public static final int RESULT_CHAUF_ID_FIELD = 11;
	public static final int RESULT_CHAUF_ESCORT_TYPE_FIELD = 12;
	public static final int RESULT_CHAUF_CHILD1_PNUM_FIELD = 13;
	public static final int RESULT_CHAUF_CHILD1_PERSON_TYPE_FIELD = 14;
	public static final int RESULT_CHAUF_CHILD1_DEPART_HOME_FIELD = 15;
	public static final int RESULT_CHAUF_CHILD2_PNUM_FIELD = 16;
	public static final int RESULT_CHAUF_CHILD2_PERSON_TYPE_FIELD = 17;
	public static final int RESULT_CHAUF_CHILD2_DEPART_HOME_FIELD = 18;
	public static final int RESULT_CHAUF_CHILD3_PNUM_FIELD = 19;
	public static final int RESULT_CHAUF_CHILD3_PERSON_TYPE_FIELD = 20;
	public static final int RESULT_CHAUF_CHILD3_DEPART_HOME_FIELD = 21;
	public static final int NUM_RESULTS_BY_CHAUF_FIELDS = 22;
	
	public static final int DRIVE_ALONE_MODE = 1;
	public static final int SHARED_RIDE_2_MODE = 3;
	public static final int SHARED_RIDE_3_MODE = 5;
	
    private Map<Integer,Integer> chauffeurPriorityOutboundMap;
    private Map<Integer,Integer> chauffeurPriorityInboundMap;
    
    	
	private TableDataSet altTable;
	private String[] altTableNames;
	private int[] altTableBundle1;
	private int[] altTableBundle2;
	private int[] altTableBundle3;
	private int[] altTableChauf1;
	private int[] altTableChauf2;
	private int[] altTableChauf3;
	private int[] altTableNumBundles;
	private int[] altTableNumRideSharing1Bundles;
	private int[] altTableNumPureEscort1Bundles;
	private int[] altTableNumRideSharing2Bundles;
	private int[] altTableNumPureEscort2Bundles;
 
	private float defaultVOT = 15; //default VOT for pure escort tours generated by this model.

	private int[][] altBundleIncidence;

	private int[] previousChoiceChauffeurs;
	
   private SchoolEscortingDmu decisionMaker;
   private transient Logger         logger                          = Logger.getLogger(SchoolEscortingModel.class);

 	private static final String UEC_PATH_KEY = "uec.path";
	private static final String OUTBOUND_UEC_FILENAME_KEY = "school.escort.uec.filename";
	private static final String OUTBOUND_UEC_MODEL_SHEET_KEY = "school.escort.outbound.model.sheet";
	private static final String OUTBOUND_UEC_DATA_SHEET_KEY = "school.escort.data.sheet";
	private static final String OUTBOUND_CHOICE_MODEL_DESCRIPTION = "School Escorting - Outbound unconditional Choice";

	private static final String INBOUND_CONDITIONAL_UEC_FILENAME_KEY = "school.escort.uec.filename";
	private static final String INBOUND_CONDITIONAL_UEC_MODEL_SHEET_KEY = "school.escort.inbound.conditonal.model.sheet";
	private static final String INBOUND_CONDITIONAL_UEC_DATA_SHEET_KEY = "school.escort.data.sheet";
	private static final String INBOUND_CONDITIONAL_CHOICE_MODEL_DESCRIPTION = "School Escorting - inbound Conditional Choice";

	private static final String OUTBOUND_CONDITIONAL_UEC_FILENAME_KEY = "school.escort.uec.filename";
	private static final String OUTBOUND_CONDITIONAL_UEC_MODEL_SHEET_KEY = "school.escort.outbound.conditonal.model.sheet";
	private static final String OUTBOUND_CONDITIONAL_UEC_DATA_SHEET_KEY = "school.escort.data.sheet";
	private static final String OUTBOUND_CONDITIONAL_CHOICE_MODEL_DESCRIPTION = "School Escorting - Outbound Conditional Choice";

   private static final String      PROPERTIES_MODEL_OFFSET   =    "school.escort.RNG.offset";

	private ChoiceModelApplication outboundModel;
	private ChoiceModelApplication inboundConditionalModel;
	private ChoiceModelApplication outboundConditionalModel;
	
	private MgraDataManager mgraDataManager;
    private double[][] distanceArray;
    
    private IndexValues indexValues;
	
	private long randomOffset = 110001;
	private MersenneTwister random;

	
	public SchoolEscortingModel( HashMap<String, String> propertyMap, MgraDataManager mgraDataManager, AutoTazSkimsCalculator tazDistanceCalculator ) {
		
		this.mgraDataManager = mgraDataManager;
		
		random = new MersenneTwister();
		randomOffset = PropertyMap.getLongValueFromPropertyMap(propertyMap,PROPERTIES_MODEL_OFFSET);

		//to do: set distance and time matrix
		
		createChauffeurPriorityOutboundMap();
		createChauffeurPriorityInboundMap();

		double[][][] storedFromTazToAllTazsDistanceSkims = tazDistanceCalculator.getStoredFromTazToAllTazsDistanceSkims();
    	distanceArray = storedFromTazToAllTazsDistanceSkims[ModelStructure.AM_SKIM_PERIOD_INDEX];
		decisionMaker = new SchoolEscortingDmu( mgraDataManager, distanceArray );

    	
        // Create the choice model applications
    	String uecPath = propertyMap.get(CtrampApplication.PROPERTIES_UEC_PATH);
        
    	//1. outbound model
        String outboundUecFile = uecPath + propertyMap.get(OUTBOUND_UEC_FILENAME_KEY);
        int outboundDataPage = Util.getIntegerValueFromPropertyMap(propertyMap, OUTBOUND_UEC_DATA_SHEET_KEY);
        int outboundModelPage = Util.getIntegerValueFromPropertyMap(propertyMap, OUTBOUND_UEC_MODEL_SHEET_KEY);

        // create the outbound choice model object
        outboundModel = new ChoiceModelApplication(outboundUecFile, outboundModelPage, outboundDataPage, propertyMap,
                (VariableTable) decisionMaker);

    	//2. inbound conditional model
        String inboundConditionalUecFile = uecPath + propertyMap.get(INBOUND_CONDITIONAL_UEC_FILENAME_KEY);
        int inboundConditionalDataPage = Util.getIntegerValueFromPropertyMap(propertyMap, INBOUND_CONDITIONAL_UEC_DATA_SHEET_KEY);
        int inboundConditionalModelPage = Util.getIntegerValueFromPropertyMap(propertyMap, INBOUND_CONDITIONAL_UEC_MODEL_SHEET_KEY);

        // create the inbound conditional choice model object
        inboundConditionalModel = new ChoiceModelApplication(inboundConditionalUecFile, inboundConditionalModelPage, 
        		inboundConditionalDataPage, propertyMap,
                (VariableTable) decisionMaker);
    	
    	//3. outbound conditional model
        String outboundConditionalUecFile = uecPath + propertyMap.get(OUTBOUND_CONDITIONAL_UEC_FILENAME_KEY);
        int outboundConditionalDataPage = Util.getIntegerValueFromPropertyMap(propertyMap, OUTBOUND_CONDITIONAL_UEC_DATA_SHEET_KEY);
        int outboundConditionalModelPage = Util.getIntegerValueFromPropertyMap(propertyMap, OUTBOUND_CONDITIONAL_UEC_MODEL_SHEET_KEY);

        // create the outbound conditional choice model object
        outboundConditionalModel = new ChoiceModelApplication(outboundConditionalUecFile, outboundConditionalModelPage, 
        		outboundConditionalDataPage, propertyMap,
                (VariableTable) decisionMaker);

        indexValues = new IndexValues();
        
    	// get the 0-index columns from the alternatives table, and save in 1-base indexed arrays for lookup by alternative number (1,...,numAlts).
    	UtilityExpressionCalculator outboundUEC = outboundModel.getUEC();
        altTable = outboundUEC.getAlternativeData();
		altTableNames = outboundUEC.getAlternativeNames();

		int[] temp = altTable.getColumnAsInt( ALT_TABLE_BUNDLE1_NAME );
		altTableBundle1 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableBundle1[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_BUNDLE2_NAME );
		altTableBundle2 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableBundle2[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_BUNDLE3_NAME );
		altTableBundle3 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableBundle3[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_CHAUF1_NAME );
		altTableChauf1 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableChauf1[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_CHAUF2_NAME );
		altTableChauf2 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableChauf2[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_CHAUF3_NAME );
		altTableChauf3 = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableChauf3[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_NBUNDLES_NAME );
		altTableNumBundles = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableNumBundles[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_NBUNDLES_RS1_NAME );
		altTableNumRideSharing1Bundles = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableNumRideSharing1Bundles[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_NBUNDLES_ES1_NAME );
		altTableNumPureEscort1Bundles = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableNumPureEscort1Bundles[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_NBUNDLES_RS2_NAME );
		altTableNumRideSharing2Bundles = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableNumRideSharing2Bundles[i+1] = temp[i];
		
		temp = altTable.getColumnAsInt( ALT_TABLE_NBUNDLES_ES2_NAME );
		altTableNumPureEscort2Bundles = new int[temp.length+1];
		for ( int i=0; i < temp.length; i++ )
			altTableNumPureEscort2Bundles[i+1] = temp[i];
		
		int index = altTable.getColumnPosition( ALT_TABLE_BUNDLE_INCIDENCE_FIRST_COLUMN_NAME );
		int numColumns = NUM_BUNDLES * NUM_CHAUFFEURS_PER_HH * NUM_ESCORT_TYPES * NUM_ESCORTEES_PER_HH;
		altBundleIncidence = new int[temp.length+1][ numColumns + 1 ];
		for ( int i=0; i < numColumns; i++ ) {
			temp = altTable.getColumnAsInt( index+i );
			for ( int j=0; j < temp.length; j++ )
				altBundleIncidence[j+1][i+1] = temp[j];
		}

		decisionMaker.setAltTableBundleIncidence( altBundleIncidence );
		
    	previousChoiceChauffeurs = new int[NUM_CHAUFFEURS_PER_HH+1];

	}
	
	
	
	/**
	 * Solve the school escort model for an array of households. The method sets DMU objects, calls the outbound choice model, the
	 * inbound conditional choice model, and another outbound conditional choice model. The method returns an ArrayList of results with
	 * 3 member lists:
	 *  0: results for escortees
	 *  1: results for chauffeurs
	 *  2: 
	 * @param logger
	 * @param hhs
	 * @throws Exception
	 */
    public void applyModel( Household household ) throws Exception {

        List<int[][]> childResultList = new ArrayList<int[][]>();
        List<int[][]> chaufResultList = new ArrayList<int[][]>();

    	// apply model only if at least 1 child goes to school
    	// output:
    	//		child - each direction, escortType(0=none, 1=ride sharing, 2=pure escort), pnum of chauffeur, bundle id, preferred departure time
    	// 		chauffeur - each direction, each bundle, pnums of children, preferred departure times
    	//		household - bundles
    	
    	//TODO: apply schedule synchronization step after choice according to rules
    	

    	int bundleListId = 0;
    	List<SchoolEscortingBundle> escortBundleList = new ArrayList<SchoolEscortingBundle>();
    	List<Integer> obPidList = new ArrayList<Integer>();
    	List<int[]> obPeList = new ArrayList<int[]>();
    	List<int[]> obRsList = new ArrayList<int[]>();
    	List<Integer> ibPidList = new ArrayList<Integer>();
    	List<int[]> ibPeList = new ArrayList<int[]>();
    	List<int[]> ibRsList = new ArrayList<int[]>();
    	
        try {
	     	//there has to be at least one child with a school tour and one active adult
            if ( (household.getNumChildrenWithSchoolTours() > 0) && (household.getNumberActiveAdults() > 0)) {

            	boolean debug = false;
            	if ( household.getDebugChoiceModels() ) {
            		household.logEntireHouseholdObject("Escort Model trace for Household "+household.getHhId(), logger);
            		debug = true;
            	}

            	long seed = household.getSeed() + randomOffset;
                random.setSeed(seed);
	         		        	
	        	previousChoiceChauffeurs[1] = 0;
	        	previousChoiceChauffeurs[2] = 0;
		        	
	            setDmuAttributesForChildren( household, SchoolEscortingModel.DIR_OUTBOUND );
	            setDmuAttributesForAdultsOutbound( household, null );	            
	            int[][][] ob0BundleResults = applyOutboundChoiceModel( logger, household, random, debug );
	
		            
            	int[][][] chaufExtentsReservedForIb = getEscortBundlesExtent( ob0BundleResults[1], SchoolEscortingModel.DIR_OUTBOUND, household.getSize() );           	
	            setDmuAttributesForChildren( household, SchoolEscortingModel.DIR_INBOUND );	
	            setDmuAttributesForAdultsInbound( household, chaufExtentsReservedForIb );
		            
	            //note: first dimension = escortees versus chauffeurs. Second dimension = size of household or size * bundles. Third dimension = results
	            int[][][] ibBundleResults = applyInboundConditionalChoiceModel( logger, household, random, debug );

	            try {
	        		bundleListId = createInboundEscortBundleObjects( ibBundleResults[1], bundleListId, escortBundleList, ibPidList, ibPeList, ibRsList );	            	
	            }
	            catch (Exception e) {
	            	logger.error ( "exception caught saving inbound school escort bundle objects for hhid = " + household.getHhId() + ".", e );
	            	throw new RuntimeException(e);
	            }
	
	
	            int[][][] chaufExtentsReservedForOb = getEscortBundlesExtent( ibBundleResults[1], SchoolEscortingModel.DIR_INBOUND, household.getHhSize() );           	
	            setDmuAttributesForChildren( household, SchoolEscortingModel.DIR_OUTBOUND );	
	            setDmuAttributesForAdultsOutbound( household, chaufExtentsReservedForOb );
	            //note: first dimension = escortees versus chauffeurs. Second dimension = size of household or size * bundles. Third dimension = results
	            int[][][] obBundleResults = applyOutboundConditionalChoiceModel( logger, household, debug );
		            
	            try {
	            	bundleListId = createOutboundEscortBundleObjects( obBundleResults[1], bundleListId, escortBundleList, obPidList, obPeList, obRsList );	            	
	            }
	            catch (Exception e) {
	            	logger.error ( "exception caught saving outbound school escort bundle objects for hhid = " + household.getHhId() + ".", e );
	            	throw new RuntimeException(e);
		        }
		            
		        for ( int j=1; j < ibBundleResults[0].length; j++ ) {
		            if ( ( ibBundleResults[0][j][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] == 4 || ibBundleResults[0][j][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] == 5 ) && ibBundleResults[0][j][RESULT_CHILD_ESCORT_TYPE_FIELD] == ModelStructure.RIDE_SHARING_TYPE )
		            	logger.info( "inbound child has ridesharing with non-working chauffeur, j=" + j );
		            else if ( ( obBundleResults[0][j][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] == 4 || obBundleResults[0][j][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] == 5 ) && obBundleResults[0][j][RESULT_CHILD_ESCORT_TYPE_FIELD] == ModelStructure.RIDE_SHARING_TYPE )
		            	logger.info( "outbound child has ridesharing with non-working chauffeur, j=" + j );
		            else if ( ( ibBundleResults[1][j][RESULT_CHAUF_PERSON_TYPE_FIELD] == 4 || ibBundleResults[1][j][RESULT_CHAUF_PERSON_TYPE_FIELD] == 5 ) && ibBundleResults[1][j][RESULT_CHAUF_ESCORT_TYPE_FIELD] == ModelStructure.RIDE_SHARING_TYPE )
		            	logger.info( "inbound non-working chauffeur has ridesharing, j=" + j );
		            else if ( ( obBundleResults[1][j][RESULT_CHAUF_PERSON_TYPE_FIELD] == 4 || obBundleResults[1][j][RESULT_CHAUF_PERSON_TYPE_FIELD] == 5 ) && obBundleResults[1][j][RESULT_CHAUF_ESCORT_TYPE_FIELD] == ModelStructure.RIDE_SHARING_TYPE )
		            	logger.info( "outbound non-working chauffeur has ridesharing, j=" + j );
	            }
		            
	            childResultList.add( obBundleResults[0] );  //outbound results for escortees
	            childResultList.add( ibBundleResults[0] );  //inbound results for escortees
	            chaufResultList.add( obBundleResults[1] );  //outbound results for chauffeurs
	            chaufResultList.add( ibBundleResults[1] );  //inbound results for chauffeurs
	            
		            
            }
    		createTours(household, escortBundleList);
        	recodeSchoolTours(household);

          }
          catch ( Exception e ) {
           	logger.error( "exception caught applying escort choice model for hh id:" + household.getHhId(), e );
       		household.logEntireHouseholdObject("Escort model trace for problem household", logger);
       		int bundleNumber=0;
       	    for(SchoolEscortingBundle bundle : escortBundleList){
           		logger.error("School Escort Bundle "+bundleNumber);
           		bundle.logBundle(logger);
           		++bundleNumber;
       	    }
       		throw new RuntimeException(e);
        }
        	
    	if(household.getDebugChoiceModels()){
    		logger.info("Logging escort model results for household: "+household.getHhId());
    		household.logEntireHouseholdObject("Escort model logging", logger);
    	}
    	
    }


    
    private int[][][] getEscortBundlesExtent( int[][] bundleResults, int dir, int numPersons ) {
    	
        Set<Integer> processedChauffSet = new TreeSet<Integer>();

    	int[][][] chaufExtent = new int[NUM_ESCORT_TYPES+1][numPersons+1][2];
        
		for ( int j=1; j < bundleResults.length; j++ ) {

            int chaufid = bundleResults[j][SchoolEscortingModel.RESULT_CHAUF_ID_FIELD];
    		if ( chaufid > 0 && !processedChauffSet.contains( chaufid ) ) {
    			
                int chaufPnum = bundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PNUM_FIELD];
        		SchoolEscortingBundle[] escortBundles = decisionMaker.getChosenBundles( bundleResults[j][RESULT_CHAUF_CHOSEN_ALT_FIELD], chaufid, dir );
	            
            	if ( escortBundles[0].getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
            		chaufExtent[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][0] = escortBundles[0].getDepartHome(); 
            		chaufExtent[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][1] = escortBundles[0].getArriveWork(); 
	            }
            	else if ( escortBundles[0].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
            		chaufExtent[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] = escortBundles[0].getDepartHome(); 
            		
            		int lastBundleIndex = 0;
            		for ( int i=0; i < escortBundles.length; i++ )
            			if ( escortBundles[i].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE )
            				lastBundleIndex = i;
            		chaufExtent[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] = escortBundles[lastBundleIndex].getArriveHome(); 
	            }

    			processedChauffSet.add( chaufid );

    		}
    		
		}

		return chaufExtent;
		
    }
    

    
    /**
     * Create outbound escort bundle objects and return the bundle list id incremented by the number of new bundles created. The method also adds the bundles to the escortBundleList and 
     * increments bundleListId by the number of outbound escort bundles.
     * 
     * @param obBundleResults  An integer array of results for escortees dimensioned by: household size + 1, escort results fields.
     * @param bundleListId     The starting bundle ID; will be incremented for each additional bundle, first for rideshare bundles, then escort bundles.
     * @param escortBundleList A List of SchoolEscortingBundle objects that will be added to by this method.
     * @param obPidList        A List of chauffeur IDs that will be added to by this method, one for each outbound bundle
     * @param obPeList         A List of the outbound pure escort bundle IDs, one for each outbound pure escort bundle
     * @param obRsList         A List of the outbound rideshare bundle IDs, one for each outbound rideshare bundle
     * @return                 The updated number of chosen bundles (last ID set in the ID lists) 
     */
    private int createOutboundEscortBundleObjects( int[][] obBundleResults, int bundleListId, List<SchoolEscortingBundle> escortBundleList, List<Integer> obPidList, List<int[]> obPeList, List<int[]> obRsList ) {
    	
		int[] obRsIds = null;
        int[] obPeIds = null;
    	
        Set<Integer> processedChauffSet = new TreeSet<Integer>();

		for ( int j=1; j < obBundleResults.length; j++ ) {

            int chaufid = obBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_ID_FIELD];
    		if ( chaufid > 0 && !processedChauffSet.contains( chaufid ) ) {
    			
        		int chaufPid = obBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PID_FIELD];
        		int chaufPnum = obBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PNUM_FIELD];
        		int chaufPtype = obBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PERSON_TYPE_FIELD];

        		SchoolEscortingBundle[] obEscortBundles = decisionMaker.getChosenBundles( obBundleResults[j][RESULT_CHAUF_CHOSEN_ALT_FIELD], chaufid, SchoolEscortingModel.DIR_OUTBOUND );
	            
	            int numRs = 0;
	            int numPe = 0;
	            for ( int k=0; k < obEscortBundles.length; k++ ) {
	            	if ( obEscortBundles[k].getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
	            		numRs++;
	            	else if ( obEscortBundles[k].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE )
	            		numPe++;
	            }
	            
            	obRsIds = new int[numRs];
            	obPeIds = new int[numPe];
            	
	            int n = 0;
	            for ( int k=0; k < obEscortBundles.length; k++ ) {
	            	if ( obEscortBundles[k].getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
	            		obEscortBundles[k].setId(bundleListId);
	            		obEscortBundles[k].setChaufPnum( chaufPnum );
	            		obEscortBundles[k].setChaufPersType( chaufPtype );
	            		obEscortBundles[k].setChaufPid( chaufPid );
	            		escortBundleList.add( obEscortBundles[k] );
	            		obRsIds[n++] = bundleListId++;
	            	}
	            }
	            n = 0;
	            for ( int k=0; k < obEscortBundles.length; k++ ) {
	            	if ( obEscortBundles[k].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
	            		obEscortBundles[k].setId(bundleListId);
	            		obEscortBundles[k].setChaufPnum( chaufPnum );
	            		obEscortBundles[k].setChaufPersType( chaufPtype );
	            		obEscortBundles[k].setChaufPid( chaufPid );
	            		escortBundleList.add( obEscortBundles[k] );
	            		obPeIds[n++] = bundleListId++;
	            	}
	            }

				obPidList.add( chaufPid );
				obPeList.add( obPeIds );
				obRsList.add( obRsIds );

    			processedChauffSet.add( chaufid );

    		}
    		
		}

		return bundleListId;
		
    }
    

    /**
     * Create inbound escort bundle objects and return the bundle list id incremented by the number of new bundles created. The method also adds the bundles to the escortBundleList and 
     * increments bundleListId by the number of inbound escort bundles.
     * 
     * @param ibBundleResults  An integer array of results for escortees dimensioned by: household size + 1, escort results fields.
     * @param bundleListId     The starting bundle ID; will be incremented for each additional bundle, first for rideshare bundles, then escort bundles.
     * @param escortBundleList A List of SchoolEscortingBundle objects that will be added to by this method.
     * @param ibPidList        A List of chauffeur IDs that will be added to by this method, one for each inbound bundle
     * @param ibPeList         A List of the inbound pure escort bundle IDs, one for each inbound pure escort bundle
     * @param ibRsList         A List of the inbound rideshare bundle IDs, one for each inbound rideshare bundle
     * @return                 The updated number of chosen bundles (last ID in the ID lists)
     */
    private int createInboundEscortBundleObjects( int[][] ibBundleResults, int bundleListId, List<SchoolEscortingBundle> escortBundleList, List<Integer> ibPidList, List<int[]> ibPeList, List<int[]> ibRsList ) {
    	
        int[] ibRsIds = null;
        int[] ibPeIds = null;
        
        Set<Integer> processedChauffSet = new TreeSet<Integer>();

        //for each person in the household
		for ( int j=1; j < ibBundleResults.length; j++ ) {

			//the id of the chauffeur for this escortee
            int chaufid = ibBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_ID_FIELD];
            
            // If this household member is escorted and we haven't created a bundle for the chauffeur yet
    		if ( chaufid > 0 && !processedChauffSet.contains( chaufid ) ) {
    			
        		int chaufPid = ibBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PID_FIELD];
        		int chaufPnum = ibBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PNUM_FIELD];
        		int chaufPtype = ibBundleResults[j][SchoolEscortingModel.RESULT_CHAUF_PERSON_TYPE_FIELD];

        		//get the chosen bundles for this chauffeur in the inbound direction
        		SchoolEscortingBundle[] ibEscortBundles = decisionMaker.getChosenBundles( ibBundleResults[j][RESULT_CHAUF_CHOSEN_ALT_FIELD], chaufid, SchoolEscortingModel.DIR_INBOUND );
	            
	            int numRs = 0; //number rideshare bundles
	            int numPe = 0; //number pure escort bundles
	            for ( int k=0; k < ibEscortBundles.length; k++ ) {
	            	if ( ibEscortBundles[k].getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
	            		numRs++;
	            	else if ( ibEscortBundles[k].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE )
	            		numPe++;
	            }
	            
	            ibRsIds = new int[numRs]; //id of each inbound rideshare bundle
            	ibPeIds = new int[numPe]; //id of each inbound pure escort bundle
            	
            	//for each inbound bundle for this chauffeur, set the bundle chauffeur attributes
	            int n = 0;
	            for ( int k=0; k < ibEscortBundles.length; k++ ) {
	            	if ( ibEscortBundles[k].getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
	            		ibEscortBundles[k].setId(bundleListId);
	            		ibEscortBundles[k].setChaufPnum( chaufPnum );
	            		ibEscortBundles[k].setChaufPersType( chaufPtype );
	            		ibEscortBundles[k].setChaufPid( chaufPid );
	            		escortBundleList.add( ibEscortBundles[k] );
	            		ibRsIds[n++] = bundleListId++;
	            	}
	            }
	            n = 0;
	            for ( int k=0; k < ibEscortBundles.length; k++ ) {
	            	if ( ibEscortBundles[k].getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
	            		ibEscortBundles[k].setId(bundleListId);
	            		ibEscortBundles[k].setChaufPnum( chaufPnum );
	            		ibEscortBundles[k].setChaufPersType( chaufPtype );
	            		ibEscortBundles[k].setChaufPid( chaufPid );
	            		escortBundleList.add( ibEscortBundles[k] );
	            		ibPeIds[n++] = bundleListId++;
	            	}
	            }
	            
    			ibPidList.add( chaufPid );
    			ibPeList.add( ibPeIds );
    			ibRsList.add( ibRsIds );

    			processedChauffSet.add( chaufid );
    			
    		}

		}
		
		return bundleListId;
		
    }
    
    
    /**
     * Apply the outbound choice model for the household. Returns a three-dimensional integer array where:
     *   dimension 1: sized 2, 0 = results for escortees in household, 1 = results for chauffeurs
     *   dimension 2: if d1 = 0, sized by household size + 1, if d1 = 1, sized by household size * max bundles (3) + 1
     *   dimension 3: if d1 = 0, size by number of result fields for escortees, if d1 = 1, sized by number of result fields for chauffeurs.
     *   Ever hear of an object?
     * @param logger A logger to write messages and debug statements to.
     * @param hh The household to solve the outbound model for.
     * @param debug If true calculations will be logged to the logger.
     * @return A ragged 3d integer array containing results for the outbound choice model for both escortees and chauffeurs.
     * @throws Exception Will be thrown if no alternative is chosen.
     */
    private int[][][] applyOutboundChoiceModel( Logger logger, Household hh, Random randObject, boolean debug ) throws Exception {

    	
    	double rn = randObject.nextDouble();
    	outboundModel.computeUtilities(decisionMaker, indexValues);
    	//double[] utilities = outboundModel.			
    	int chosenObAlt = outboundModel.getChoiceResult(rn);
    	if ( chosenObAlt < 0 || debug ) {
    		
       		logger.info("Logging Escort Outbound Model Results for household "+hh.getHhId());
   
			//int[] altvaluesToLog = new int[]{ 1, 7, 40, 70, 105, 138, 161, 188 };
			int[] altvaluesToLog = new int[50];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+1;
			outboundModel.logUECResultsSpecificAlts( logger, "Escort model outbound UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+51;
			outboundModel.logUECResultsSpecificAlts( logger, "Escort model outbound UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+101;
			outboundModel.logUECResultsSpecificAlts( logger, "Escort model outbound UEC", altvaluesToLog );
			altvaluesToLog = new int[altTableNames.length - 150];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+151;
			outboundModel.logUECResultsSpecificAlts( logger, "Escort model outbound UEC", altvaluesToLog );
			if ( chosenObAlt < 0 ) {
	    		logger.error( hh.toString() );
	    		throw new Exception( "chosenObAlt = " + chosenObAlt + " for hhid=" + hh.getHhId() );
	    	}

	    	//outboundModel.logInfo("outbound unconditional model", "HHID "+ hh.getHhId(), logger);
	    	
	    	if(debug)
	    		logger.info("Chose outbound unconditional alternative "+ chosenObAlt);
	   }

    	// get field valuess from alternatives table associated with chosen alternative
		int chosenObBundle1 = altTableBundle1[chosenObAlt];
		int chosenObBundle2 = altTableBundle2[chosenObAlt];
		int chosenObBundle3 = altTableBundle3[chosenObAlt];
		int chosenObChauf1 = altTableChauf1[chosenObAlt];
		int chosenObChauf2 = altTableChauf2[chosenObAlt];
		int chosenObChauf3 = altTableChauf3[chosenObAlt];

    	
    	// set the dmu attributes associated with the chosen alternative needed by the Inbound Conditional Choice model
    	decisionMaker.setOutboundEscortType1( chosenObChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenObChauf1 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenObChauf1 == PURE_ESCORTING_CHAUFFEUR_1 || chosenObChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setOutboundEscortType2( chosenObChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenObChauf2 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenObChauf2 == PURE_ESCORTING_CHAUFFEUR_1 || chosenObChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setOutboundEscortType3( chosenObChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenObChauf3 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenObChauf3 == PURE_ESCORTING_CHAUFFEUR_1 || chosenObChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setOutboundChauffeur1( chosenObChauf1 );
    	decisionMaker.setOutboundChauffeur2( chosenObChauf2 );
    	decisionMaker.setOutboundChauffeur3( chosenObChauf3 );
    	
    	int[][][] results = new int[2][][]; 
    	results[0] = getResultsByChildArray( hh, decisionMaker, DIR_OUTBOUND, chosenObAlt, (int)(rn*1000000000), chosenObBundle1, chosenObBundle2, chosenObBundle3, chosenObChauf1, chosenObChauf2, chosenObChauf3 );    	
    	results[1] = getResultsByChauffeurArray( hh, decisionMaker, DIR_OUTBOUND, chosenObAlt, chosenObBundle1, chosenObBundle2, chosenObBundle3, chosenObChauf1, chosenObChauf2, chosenObChauf3 );
        
    	return results;
        
    }


    /**
     * Apply the inbound conditional choice model for the household. Returns a three-dimensional integer array where:
     *   dimension 1: sized 2, 0 = results for escortees in household, 1 = results for chauffeurs
     *   dimension 2: if d1 = 0, sized by household size + 1, if d1 = 1, sized by household size * max bundles (3) + 1
     *   dimension 3: if d1 = 0, size by number of result fields for escortees, if d1 = 1, sized by number of result fields for chauffeurs.
     *   Ever hear of an object?
     * @param logger A logger to write messages and debug statements to.
     * @param hh The household to solve the inbound conditional model for.
     * @param debug If true calculations will be logged to the logger.
     * @return A ragged 3d integer array containing results for the inbound conditional choice model for both escortees and chauffeurs.
     * @throws Exception Will be thrown if no alternative is chosen.
     */
    private int[][][] applyInboundConditionalChoiceModel( Logger logger, Household hh, Random randObject, boolean debug ) throws Exception {

    	double rn = randObject.nextDouble();
    	inboundConditionalModel.computeUtilities(decisionMaker, indexValues);			
		int chosenIbAlt  = inboundConditionalModel.getChoiceResult(rn);
    	if ( chosenIbAlt < 0 || debug ) {
    		
    		logger.info("Logging Escort Inbound Conditional Model Results for household "+hh.getHhId());
     		
			//int[] altvaluesToLog = new int[]{ 1, 7, 40, 70, 105, 138, 161, 188 };
			int[] altvaluesToLog = new int[50];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+1;
			inboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model inbound conditional UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+51;
			inboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model inbound conditional UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+101;
			inboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model inbound conditional UEC", altvaluesToLog );
			altvaluesToLog = new int[altTableNames.length - 150];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+151;
			inboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model inbound conditional UEC", altvaluesToLog );
	    	if ( chosenIbAlt < 0 ) {
	    		logger.error( hh.toString() );
	    		throw new Exception( "chosenIbAlt = " + chosenIbAlt + " for hhid=" + hh.getHhId() );
	    	}

	    	//inboundConditionalModel.logInfo("inbound conditional model", "HHID "+ hh.getHhId(), logger);
	    	
	    	if(debug)
	    		logger.info("Chose inbound conditional alternative "+ chosenIbAlt);
    	}
    	
    	hh.setInboundEscortChoice(chosenIbAlt);
    	
    	// get field values from alternatives table associated with chosen alternative
		int chosenIbBundle1 = altTableBundle1[chosenIbAlt];
		int chosenIbBundle2 = altTableBundle2[chosenIbAlt];
		int chosenIbBundle3 = altTableBundle3[chosenIbAlt];
		int chosenIbChauf1 = altTableChauf1[chosenIbAlt];
		int chosenIbChauf2 = altTableChauf2[chosenIbAlt];
		int chosenIbChauf3 = altTableChauf3[chosenIbAlt];

		if ( debug ) {
			int[] escortees = decisionMaker.getEscorteePnums();
			String escorteeString = String.format( "[%s%s%s]", String.valueOf(escortees[1]), (escortees[2] > 0 ? "," + String.valueOf(escortees[2]) : ""), (escortees[3] > 0 ? "," + String.valueOf(escortees[3]) : "") );
			int[] chaufs = decisionMaker.getChauffeurPnums();
			String chaufString = String.format( "[%s%s]", String.valueOf(chaufs[1]), (chaufs[2] > 0 ? "," + String.valueOf(chaufs[2]) : "") );
			logger.info( "hhid=" + hh.getHhId() + ", escortees=" + escorteeString + ", chaufs=" + chaufString );
			logger.info( "chosenIbAlt=" + chosenIbAlt + ", chosenIbChauf1=" + chosenIbChauf1 + ", chosenIbChauf2=" + chosenIbChauf2 + ", chosenIbChauf3=" + chosenIbChauf3 );
			logger.info( "chosenIbBundle1=" + chosenIbBundle1 + ", chosenIbBundle2=" + chosenIbBundle2 + ", chosenIbBundle3=" + chosenIbBundle3 );
		}
    	

    	// set the dmu attributes associated with the chosen alternative needed by the Outbound Conditional Choice model
    	decisionMaker.setInboundEscortType1( chosenIbChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenIbChauf1 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenIbChauf1 == PURE_ESCORTING_CHAUFFEUR_1 || chosenIbChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setInboundEscortType2( chosenIbChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenIbChauf2 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenIbChauf2 == PURE_ESCORTING_CHAUFFEUR_1 || chosenIbChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setInboundEscortType3( chosenIbChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenIbChauf3 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenIbChauf3 == PURE_ESCORTING_CHAUFFEUR_1 || chosenIbChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0 );
    	decisionMaker.setInboundChauffeur1( chosenIbChauf1 );
    	decisionMaker.setInboundChauffeur2( chosenIbChauf2 );
    	decisionMaker.setInboundChauffeur3( chosenIbChauf3 );

    	    	
    	int[][][] results = new int[2][][]; 
    	results[0] = getResultsByChildArray( hh, decisionMaker, DIR_INBOUND, chosenIbAlt, (int)(rn*1000000000), chosenIbBundle1, chosenIbBundle2, chosenIbBundle3, chosenIbChauf1, chosenIbChauf2, chosenIbChauf3 );
    	results[1] = getResultsByChauffeurArray( hh, decisionMaker, DIR_INBOUND, chosenIbAlt, chosenIbBundle1, chosenIbBundle2, chosenIbBundle3, chosenIbChauf1, chosenIbChauf2, chosenIbChauf3 );
        
    	return results;

    }


    /**
     * Apply the outbound conditional choice model for the household. Returns a three-dimensional integer array where:
     *   dimension 1: sized 2, 0 = results for escortees in household, 1 = results for chauffeurs
     *   dimension 2: if d1 = 0, sized by household size + 1, if d1 = 1, sized by household size * max bundles (3) + 1
     *   dimension 3: if d1 = 0, size by number of result fields for escortees, if d1 = 1, sized by number of result fields for chauffeurs.
     *   Ever hear of an object?
     * @param logger A logger to write messages and debug statements to.
     * @param hh The household to solve the outbound conditional model for.
     * @param debug If true calculations will be logged to the logger.
     * @return A ragged 3d integer array containing results for the outbound conditional choice model for both escortees and chauffeurs.
     * @throws Exception Will be thrown if no alternative is chosen.
     */
    private int[][][]  applyOutboundConditionalChoiceModel( Logger logger, Household hh, boolean debug ) throws Exception {

    	double rn = random.nextDouble();
    	outboundConditionalModel.computeUtilities(decisionMaker, indexValues);			
		int chosenObAlt  = outboundConditionalModel.getChoiceResult(rn);
		if ( chosenObAlt < 0 || debug ) {
			
	   		logger.info("Logging Escort Outbound Conditional Model Results for household "+hh.getHhId());
 
			//int[] altvaluesToLog = new int[]{ 1, 7, 40, 70, 105, 138, 161, 188 };
			int[] altvaluesToLog = new int[50];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+1;
			outboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model outbound conditional UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+51;
			outboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model outbound conditional UEC", altvaluesToLog );
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+101;
			outboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model outbound conditional UEC", altvaluesToLog );
			altvaluesToLog = new int[altTableNames.length - 150];
			for ( int i=0; i < altvaluesToLog.length; i++ )
				altvaluesToLog[i] = i+151;
			outboundConditionalModel.logUECResultsSpecificAlts( logger, "Escort model outbound conditional UEC", altvaluesToLog );
	    	if ( chosenObAlt < 0 ) {
	    		logger.error( hh.toString() );
	    		throw new Exception( "chosenObAlt = " + chosenObAlt + " for hhid=" + hh.getHhId() );
	    	}

	    	//outboundConditionalModel.logInfo("outbound conditional model", "HHID "+ hh.getHhId(), logger);
	    	
	    	if(debug)
	    		logger.info("Chose outbound conditional alternative "+ chosenObAlt);
    	}
    	
		hh.setOutboundEscortChoice(chosenObAlt);
    	
    	// get field valuess from alternatives table associated with chosen alternative
		int chosenObBundle1 = altTableBundle1[chosenObAlt];
		int chosenObBundle2 = altTableBundle2[chosenObAlt];
		int chosenObBundle3 = altTableBundle3[chosenObAlt];
		int chosenObChauf1 = altTableChauf1[chosenObAlt];
		int chosenObChauf2 = altTableChauf2[chosenObAlt];
		int chosenObChauf3 = altTableChauf3[chosenObAlt];

		if ( debug ) {
			int[] escortees = decisionMaker.getEscorteePnums();
			String escorteeString = String.format( "[%s%s%s]", String.valueOf(escortees[1]), (escortees[2] > 0 ? "," + String.valueOf(escortees[2]) : ""), (escortees[3] > 0 ? "," + String.valueOf(escortees[3]) : "") );
			int[] chaufs = decisionMaker.getChauffeurPnums();
			String chaufString = String.format( "[%s%s]", String.valueOf(chaufs[1]), (chaufs[2] > 0 ? "," + String.valueOf(chaufs[2]) : "") );
			logger.info( "hhid=" + hh.getHhId() + ", escortees=" + escorteeString + ", chaufs=" + chaufString );
			logger.info( "chosenObAlt=" + chosenObAlt + ", chosenObChauf1=" + chosenObChauf1 + ", chosenObChauf2=" + chosenObChauf2 + ", chosenObChauf3=" + chosenObChauf3 );
			logger.info( "chosenObBundle1=" + chosenObBundle1 + ", chosenObBundle2=" + chosenObBundle2 + ", chosenObBundle3=" + chosenObBundle3 );
		}
    	
    	int[][][] results = new int[2][][]; 
    	results[0] = getResultsByChildArray( hh, decisionMaker, DIR_OUTBOUND, chosenObAlt, (int)(rn*1000000000), chosenObBundle1, chosenObBundle2, chosenObBundle3, chosenObChauf1, chosenObChauf2, chosenObChauf3 );    	
    	results[1] = getResultsByChauffeurArray( hh, decisionMaker, DIR_OUTBOUND, chosenObAlt, chosenObBundle1, chosenObBundle2, chosenObBundle3, chosenObChauf1, chosenObChauf2, chosenObChauf3 );
        
    	return results;
        
    }

    
    private void setDmuAttributesForChildren( Household hh, int dir ) {
    	
    	List<Person> children = hh.getChildPersons();
    	List<Person> cList = new ArrayList<Person>();
    	for ( Person child : children ) {
			if (child.getUsualSchoolLocation() > 0 && child.getNumSchoolTours() > 0  )
				cList.add( child );
    	}
    	Person[] escortees = getOrderedSchoolChildrenForEscorting( cList, dir );

    	int[] schoolAtHome = new int[NUM_ESCORTEES_PER_HH+1];
    	int[] schoolMazs = new int[NUM_ESCORTEES_PER_HH+1];
    	int[] schoolDeparts = new int[NUM_ESCORTEES_PER_HH+1];
    	int[] schoolReturns = new int[NUM_ESCORTEES_PER_HH+1];
    	for ( int i=1; i < escortees.length; i++ ) {
    		if ( escortees[i] == null )
    			continue;
    		ArrayList<Tour> schoolTours = escortees[i].getListOfSchoolTours();
    		if ( schoolTours != null ) {
    			Tour tour = schoolTours.get(0);
    			schoolAtHome[i] = 0;
    			schoolMazs[i] = tour.getTourDestMgra();
    			schoolDeparts[i] = tour.getTourDepartPeriod();
    			schoolReturns[i] = tour.getTourArrivePeriod();
    		}
    	}
    	
    	decisionMaker.setEscorteeAttributes( cList.size(), escortees, schoolAtHome, schoolMazs, schoolDeparts, schoolReturns );

    }

    
    private void setDmuAttributesForAdultsInbound( Household hh, int[][][] chaufExtents ) {
    	
    	List<Person> activeAdults = hh.getActiveAdultPersons();
    	
    	Person[] chauffers = getOrderedAdultsForChauffeuringInbound( activeAdults );
    	
    	int[] mandatoryMazs = new int[chauffers.length];
    	int[] mandatoryDeparts = new int[chauffers.length];
    	int[] mandatoryReturns = new int[chauffers.length];
    	for ( int i=1; i < chauffers.length; i++ ) {
    		if ( chauffers[i] == null )
    			continue;
    		
      		ArrayList<Tour> mandatoryTours = getMandatoryTours(chauffers[i]);
 
    		if ( ! mandatoryTours.isEmpty() ) {
    			Tour tour2 = mandatoryTours.size() == 2 ? mandatoryTours.get(1) : mandatoryTours.get(0);
    			mandatoryMazs[i] = tour2.getTourDestMgra();
    			mandatoryDeparts[i] = tour2.getTourDepartPeriod();
    			mandatoryReturns[i] = tour2.getTourArrivePeriod();
    		}
    	}
    	
    	decisionMaker.setChaufferAttributes( activeAdults.size(), chauffers, mandatoryMazs, mandatoryDeparts, mandatoryReturns, chaufExtents );
    	decisionMaker.setDistanceTimeAttributes( hh, distanceArray );

    	// set the potential chauffeur pnums from the outbound unconditional choice for use with the inbound conditional choice
    	decisionMaker.setOutboundPotentialChauffeur1( previousChoiceChauffeurs[1] );
    	decisionMaker.setOutboundPotentialChauffeur2( previousChoiceChauffeurs[2] );
    	
    	// set the potential chauffeur pnum values in the inbound direction for eventual use in the outbound conditional choice
    	for ( int i=1; i < chauffers.length; i++ ) {
			if ( chauffers[i] == null )
				previousChoiceChauffeurs[i] = 0;
			else
				previousChoiceChauffeurs[i] = chauffers[i].getPersonNum();
    	}
    	
    }


    /**
     * Get a list of mandatory tours for this person.
     * 
     * @param p A person.
     * @return An ArrayList of mandatory tours. Empty if no mandatory tours.
     */
    ArrayList<Tour> getMandatoryTours(Person p){
    	
   		ArrayList<Tour> mandatoryTours = new ArrayList<Tour>();

		if(p.getListOfWorkTours()!=null)
			mandatoryTours.addAll(p.getListOfWorkTours());
		
		if(p.getListOfSchoolTours()!=null)
			mandatoryTours.addAll(p.getListOfSchoolTours());

		return mandatoryTours;
    	
    }
    /**
     * Set DMU attributes for adults in the outbound direction.
     * 
     * @param hh
     * @param chaufExtents
     */
    private void setDmuAttributesForAdultsOutbound( Household hh, int[][][] chaufExtents ) {
    	
    	List<Person> activeAdults = hh.getActiveAdultPersons();
    	
    	Person[] chauffers = getOrderedAdultsForChauffeuringOutbound( activeAdults );
    	
    	int[] mandatoryMazs = new int[NUM_CHAUFFEURS_PER_HH+1];
    	int[] mandatoryDeparts = new int[NUM_CHAUFFEURS_PER_HH+1];
    	int[] mandatoryReturns = new int[NUM_CHAUFFEURS_PER_HH+1];
    	
    	
    	for ( int i=1; i < chauffers.length; i++ ) {
    		if ( chauffers[i] == null )
    			continue;
        
    		ArrayList<Tour> mandatoryTours = getMandatoryTours(chauffers[i]);

    		if ( ! mandatoryTours.isEmpty() ) {
    			Tour tour1 = mandatoryTours.get(0);
    			Tour tour2 = mandatoryTours.size() == 2 ? mandatoryTours.get(1) : mandatoryTours.get(0);
    			mandatoryMazs[i] = tour1.getTourDestMgra();
    			mandatoryDeparts[i] = tour1.getTourDepartPeriod();
    			mandatoryReturns[i] = tour2.getTourArrivePeriod();
    		}
    		
    	}
    	
    	decisionMaker.setChaufferAttributes( activeAdults.size(), chauffers, mandatoryMazs, mandatoryDeparts, mandatoryReturns, chaufExtents );
    	decisionMaker.setDistanceTimeAttributes( hh, distanceArray );
    	
    	// set the potential chauffeur pnums from the inbound conditional choice for use with the outbound conditional choice
    	decisionMaker.setInboundPotentialChauffeur1( previousChoiceChauffeurs[1] );
    	decisionMaker.setInboundPotentialChauffeur2( previousChoiceChauffeurs[2] );
    	
    	// set the potential chauffeur pnum values in the outbound direction for eventual use in the inbound conditional choice
    	for ( int i=1; i < chauffers.length; i++ ) {
			if ( chauffers[i] == null )
				previousChoiceChauffeurs[i] = 0;
			else
				previousChoiceChauffeurs[i] = chauffers[i].getPersonNum();
    	}
    	
    }


    /**
     * Order children in list according to age, distance and time period, and return the result in an array.
     * 
     * @param childList  List of children to escort
     * @param dir Direction of travel (outbound or return)
     * @return
     */
    private Person[] getOrderedSchoolChildrenForEscorting( List<Person> childList, int dir) {

		Household hh = childList.get( 0 ).getHouseholdObject();
		int homeTaz = mgraDataManager.getTaz( hh.getHhMgra() );
		
		// sort the eligible children by age so that the 3 youngest are the final candidates
    	Collections.sort( childList, 
    		new Comparator<Person>() {
    			@Override
    			public int compare( Person p1, Person p2 ) {
    				return p1.getAge() - p2.getAge();
    			}
    		}
    	);

    	Person[] returnArray = new Person[NUM_ESCORTEES_PER_HH+1];
    	
    	//if there is only one child to escort, its easy - just return him/her.
		if ( childList.size() == 1 ) {
			returnArray[1] = childList.get( 0 );
		}
	    //if there are two or three children sort them according to distance and departure time time
		else if ( childList.size() == 2 ) {
			
			Person child0 = childList.get( 0 );
			ArrayList<Tour> schoolTours = child0.getListOfSchoolTours();
			Tour tour0 = schoolTours.get(0);
			int timeInterval0 = 0;
			int schoolTaz = mgraDataManager.getTaz( tour0.getTourDestMgra());
			float dist0 = (float) distanceArray[homeTaz][ schoolTaz];
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				timeInterval0 = tour0.getTourDepartPeriod();
			}
			else {
				timeInterval0 = tour0.getTourArrivePeriod();
			}

			Person child1 = childList.get( 1 );
			schoolTours = child1.getListOfSchoolTours();
			Tour tour1 = schoolTours.get(0);
			int timeInterval1 = 0;
			schoolTaz = mgraDataManager.getTaz( tour1.getTourDestMgra());
			float dist1 = (float) distanceArray[homeTaz][ schoolTaz];
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				timeInterval1 = tour1.getTourDepartPeriod();
			}
			else {
				timeInterval1 = tour1.getTourArrivePeriod();
			}
				
			int[] sortData = new int[2];
			sortData[0] = timeInterval0*10000000 + (int)(dist0*1000); 
			sortData[1] = timeInterval1*10000000 + (int)(dist1*1000);
			int[] sortIndices = IndexSort.indexSort( sortData );

			returnArray[1] = childList.get( sortIndices[0] );
			returnArray[2] = childList.get( sortIndices[1] );

		}
		else if ( childList.size() >= 3 ) {
			
			Person child0 = childList.get( 0 );
			ArrayList<Tour> schoolTours = child0.getListOfSchoolTours();
			Tour tour0 = schoolTours.get(0);
			int timeInterval0 = 0;
			int schoolTaz = mgraDataManager.getTaz( tour0.getTourDestMgra());
			float dist0 = (float) distanceArray[homeTaz][ schoolTaz];
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				timeInterval0 = tour0.getTourDepartPeriod();
			}
			else {
				timeInterval0 = tour0.getTourArrivePeriod();
			}

			Person child1 = childList.get( 1 );
			schoolTours = child1.getListOfSchoolTours();
			Tour tour1 = schoolTours.get(0);
			int timeInterval1 = 0;
			schoolTaz = mgraDataManager.getTaz( tour1.getTourDestMgra());
			float dist1 = (float) distanceArray[homeTaz][ schoolTaz];
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				timeInterval1 = tour1.getTourDepartPeriod();
			}
			else {
				timeInterval1 = tour1.getTourArrivePeriod();
			}
				
			Person child2 = childList.get( 2 );
			schoolTours = child2.getListOfSchoolTours();
			Tour tour2 = schoolTours.get(0);
			int timeInterval2 = 0;
			schoolTaz = mgraDataManager.getTaz( tour2.getTourDestMgra());
			float dist2 = (float)   distanceArray[homeTaz][ schoolTaz];

			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				timeInterval2 = tour2.getTourDepartPeriod();
			}
			else {
				timeInterval2 = tour2.getTourArrivePeriod();
			}
				
			int[] sortData = new int[3];
			sortData[0] = timeInterval0*10000000 + (int)(dist0*1000); 
			sortData[1] = timeInterval1*10000000 + (int)(dist1*1000);
			sortData[2] = timeInterval2*10000000 + (int)(dist2*1000);
			int[] sortIndices = IndexSort.indexSort( sortData );

			returnArray[1] = childList.get( sortIndices[0] );
			returnArray[2] = childList.get( sortIndices[1] );
			returnArray[3] = childList.get( sortIndices[2] );
						
		}
 	
    	return returnArray;
    	
    }
    
    
    private Person[] getOrderedAdultsForChauffeuringOutbound( List<Person> adultList ) {
        
    	Collections.sort( adultList, 
    		new Comparator<Person>() {
    			@Override
    			public int compare( Person p1, Person p2 ) {
    				int p1LookupValue = PT_WEIGHT*p1.getPersonTypeNumber() + G_WEIGHT*p1.getGender() + A_WEIGHT*(p1.getAge() > 25 ? 1 : 0);
    				int p2LookupValue = PT_WEIGHT*p2.getPersonTypeNumber() + G_WEIGHT*p2.getGender() + A_WEIGHT*(p2.getAge() > 25 ? 1 : 0);
    				if(!chauffeurPriorityOutboundMap.containsKey(p1LookupValue)){
    					logger.fatal("Cannot find lookup value "+p1LookupValue+" in outbound chauffeur priority map");
    					throw new RuntimeException();
    				}
       				if(!chauffeurPriorityOutboundMap.containsKey(p2LookupValue)){
    					logger.fatal("Cannot find lookup value "+p2LookupValue+" in outbound chauffeur priority map");
    					throw new RuntimeException();
       				}
    				int p1Score = chauffeurPriorityOutboundMap.get(p1LookupValue);
    				int p2Score = chauffeurPriorityOutboundMap.get(p2LookupValue);
    				return p1Score - p2Score; 
    			}
    		}
    	);

    	Person[] returnArray = new Person[NUM_CHAUFFEURS_PER_HH+1];
    	for ( int i=0; i < adultList.size() && i < NUM_CHAUFFEURS_PER_HH; i++  )
    		returnArray[i+1] = adultList.get( i );
    	
    	return returnArray;
    	
    }
    

    /**
     * Orders the list of adults passed to the method by person type, gender, and age combination.
     * 
     * @param adultList A list of potential chauffeurs
     * @return An ordered array of the chauffeurs.
     */
    private Person[] getOrderedAdultsForChauffeuringInbound( List<Person> adultList ) {
        
    	Collections.sort( adultList, 
    		new Comparator<Person>() {
    			@Override
    			public int compare( Person p1, Person p2 ) {
    				int p1LookupValue = PT_WEIGHT*p1.getPersonTypeNumber() + G_WEIGHT*p1.getGender() + A_WEIGHT*(p1.getAge() >= 25 ? 1 : 0);
    				int p2LookupValue = PT_WEIGHT*p2.getPersonTypeNumber() + G_WEIGHT*p2.getGender() + A_WEIGHT*(p2.getAge() >= 25 ? 1 : 0);
    				return chauffeurPriorityInboundMap.get(p1LookupValue) - chauffeurPriorityInboundMap.get(p2LookupValue);
    			}
    		}
    	);

    	Person[] returnArray = new Person[NUM_CHAUFFEURS_PER_HH+1];
    	for ( int i=0; i < adultList.size() && i < NUM_CHAUFFEURS_PER_HH; i++  )
    		returnArray[i+1] = adultList.get( i );
    	
    	return returnArray;
    	
    }
    
    /**
     * Create the priority map for outbound chauffeurs, according to person type, gender, and age bin.
     */
    private void createChauffeurPriorityOutboundMap() {
    	
    	chauffeurPriorityOutboundMap = new HashMap<Integer,Integer>();
    	
    	int lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 1 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 2 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 3 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX  + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 4 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX  + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 5 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 6 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 7 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 8 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 9 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 10 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX  + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 11 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 12 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 13 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 13 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX  + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 14 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX  + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 14 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 15 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 15 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityOutboundMap.put( lookupValue, 16 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityOutboundMap.put( lookupValue, 16 );
    	
    }
    
    
    /**
     * Create the priority map for inbound chauffeurs, according to person type, gender, and age bin.
     */
   private void createChauffeurPriorityInboundMap() {
    	
    	chauffeurPriorityInboundMap = new HashMap<Integer,Integer>();
    	
    	int lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 1 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 1 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 2 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX  + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 2 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 3 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 3 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 4 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 5 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_PART_TIME_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 5 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 6 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_FULL_TIME_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 6 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 7 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 8 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_NON_WORKER_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 8 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 9 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_UNIVERSITY_STUDENT_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 9 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 10 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.FEMALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 10 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*0;
    	chauffeurPriorityInboundMap.put( lookupValue, 11 );
    	
    	lookupValue = PT_WEIGHT*Person.PERSON_TYPE_RETIRED_INDEX + G_WEIGHT*Person.MALE_INDEX + A_WEIGHT*1;
    	chauffeurPriorityInboundMap.put( lookupValue, 11 );
    	
    }
    
    /**
     * Gets the results for each child in the household. The method returns a two dimensional array where the first
     * dimension is sized by persons in household + 1, and the second dimension is the total number of fields for child results.
     * The first dimension is indexed into by person number and the second dimension is indexed into by result field number. 
     * Results include household and person attributes for the child being escorted and the chauffeur who is escorting them, 
     * as well as the attributes of the choice.
     *  
     * @param hh
     * @param decisionMaker
     * @param direction
     * @param chosenAlt Number of chosen alternative.
     * @param intRandNum
     * @param chosenBundle1 The bundle for child 1: 0 = not escorted. Max 3 bundles
     * @param chosenBundle2 The bundle for child 2: 0 = not escorted. Max 3 bundles
     * @param chosenBundle3 The bundle for child 3: 0 = not escorted. Max 3 bundles
     * @param chosenChauf1 The chauffeur for child 1: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @param chosenChauf2 The chauffeur for child 2: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @param chosenChauf3 The chauffeur for child 3: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @return An integer array of results for each person in the household.
     */
    private int[][] getResultsByChildArray( Household hh, SchoolEscortingDmu decisionMaker, int direction, int chosenAlt, int intRandNum, int chosenBundle1, int chosenBundle2, int chosenBundle3, int chosenChauf1, int chosenChauf2, int chosenChauf3 ) {
    	
    	int[][] resultsByChild = new int[hh.getHhSize()+1][NUM_RESULTS_BY_CHILD_FIELDS];

    	
    	int[] adultPnums = decisionMaker.getChauffeurPnums();
    	int[] childPnums = decisionMaker.getEscorteePnums();
    	
    	
        // set result attributes for all children in the Household
        for ( int pnum=1; pnum <= hh.getHhSize(); pnum++ ) {
        	Person person = hh.getPerson( pnum );
            resultsByChild[pnum][RESULT_CHILD_HHID_FIELD] = hh.getHhId();
            resultsByChild[pnum][RESULT_CHILD_PNUM_FIELD] = pnum;
            resultsByChild[pnum][RESULT_CHILD_PID_FIELD] = person.getPersonId();
            resultsByChild[pnum][RESULT_CHILD_PERSON_TYPE_FIELD] = person.getPersonTypeNumber();
            resultsByChild[pnum][RESULT_CHILD_AGE_FIELD] = person.getAge();
            resultsByChild[pnum][RESULT_CHILD_CDAP_FIELD] = person.getCdapIndex();
            resultsByChild[pnum][RESULT_CHILD_SCHOOL_AT_HOME_FIELD] = 0;
            resultsByChild[pnum][RESULT_CHILD_SCHOOL_LOC_FIELD] = person.getPersonSchoolLocationZone();
        }

        // set result attributes for children that are to be escorted
        for ( Person child : hh.getChildPersons() ) {
        	int pnum = child.getPersonNum();
            resultsByChild[pnum][RESULT_CHILD_DIRECTION_FIELD] = direction;
            resultsByChild[pnum][RESULT_CHILD_CHOSEN_ALT_FIELD] = chosenAlt;
            resultsByChild[pnum][RESULT_CHILD_RANDOM_NUM_FIELD] = intRandNum;
        }
        
        // for each escorted child
        for ( int i=1; i < childPnums.length; i++ ) {
        	if ( childPnums[i] > 0 ) {
                resultsByChild[childPnums[i]][RESULT_CHILD_ESCORT_ELIGIBLE_FIELD] = ESCORT_ELIGIBLE;
                resultsByChild[childPnums[i]][RESULT_CHILD_DEPART_FROM_HOME_FIELD] = decisionMaker.getEscorteeDepartForSchool()[i];
                resultsByChild[childPnums[i]][RESULT_CHILD_DEPART_TO_HOME_FIELD] = decisionMaker.getEscorteeDepartFromSchool()[i];
                resultsByChild[childPnums[i]][RESULT_CHILD_DIST_TO_SCHOOL_FIELD] = decisionMaker.getEscorteeDistToSchool()[i];
                resultsByChild[childPnums[i]][RESULT_CHILD_DIST_FROM_SCHOOL_FIELD] = decisionMaker.getEscorteeDistFromSchool()[i];
            	if ( adultPnums[1] > 0 ) {
                    resultsByChild[childPnums[i]][RESULT_CHILD_ADULT1_DEPART_FROM_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[1];
                    resultsByChild[childPnums[i]][RESULT_CHILD_ADULT1_DEPART_TO_HOME_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[1];
            	}
            	if ( adultPnums[2] > 0 ) {
                    resultsByChild[childPnums[i]][RESULT_CHILD_ADULT2_DEPART_FROM_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[2];
                    resultsByChild[childPnums[i]][RESULT_CHILD_ADULT2_DEPART_TO_HOME_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[2];
            	}
        	}
        }

        // for each adult
        for ( int i=1; i < adultPnums.length; i++ ) {
        	if ( adultPnums[i] > 0 ) {
                resultsByChild[adultPnums[i]][RESULT_CHILD_DEPART_FROM_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[i];
                resultsByChild[adultPnums[i]][RESULT_CHILD_DEPART_TO_HOME_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[i];
        	}
        }

        if ( chosenChauf1 > 0 ) {
        	int childid = 1;
        	int chaufid = chosenChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf1 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf1 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
            resultsByChild[childPnums[childid]][RESULT_CHILD_ESCORT_TYPE_FIELD] = escortType;
            resultsByChild[childPnums[childid]][RESULT_CHILD_BUNDLE_ID_FIELD] = chosenBundle1;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHILD_ID_FIELD] = childid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_ID_FIELD] = chaufid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PNUM_FIELD] = adultPnums[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PID_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonId();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonTypeNumber();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_WORK_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[chaufid];
        }
        
        if ( chosenChauf2 > 0 ) {
        	int childid = 2;
        	int chaufid = chosenChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf2 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf2 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
            resultsByChild[childPnums[childid]][RESULT_CHILD_ESCORT_TYPE_FIELD] = escortType;
            resultsByChild[childPnums[childid]][RESULT_CHILD_BUNDLE_ID_FIELD] = chosenBundle2;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHILD_ID_FIELD] = childid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_ID_FIELD] = chaufid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PNUM_FIELD] = adultPnums[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PID_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonId();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonTypeNumber();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_WORK_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[chaufid];
        }
        
        if ( chosenChauf3 > 0 ) {
        	int childid = 3;
        	int chaufid = chosenChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf3 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf3 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
            resultsByChild[childPnums[childid]][RESULT_CHILD_ESCORT_TYPE_FIELD] = escortType;
            resultsByChild[childPnums[childid]][RESULT_CHILD_BUNDLE_ID_FIELD] = chosenBundle3;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHILD_ID_FIELD] = childid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_ID_FIELD] = chaufid;
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PNUM_FIELD] = adultPnums[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PID_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonId();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_PERSON_TYPE_FIELD] = hh.getPerson( adultPnums[chaufid] ).getPersonTypeNumber();
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_HOME_FIELD] = decisionMaker.getChauffeurDepartForMandatory()[chaufid];
            resultsByChild[childPnums[childid]][RESULT_CHILD_CHAUFFEUR_DEPART_WORK_FIELD] = decisionMaker.getChauffeurDepartFromMandatory()[chaufid];
        }

        return resultsByChild;
        
    }
    

    /**
     * Get the results for each chauffeur in the household and escort bundles undertaken. The method returns a two dimensional integer array
     * where the first dimension is sized by household size * total possible bundles (3) + 1, and the second dimension is sized by number of 
     * results fields for chauffeurs. 
     *
     * @param hh
     * @param decisionMaker
     * @param direction
     * @param chosenAlt
     * @param chosenBundle1 The bundle for child 1: 0 = not escorted. Max 3 bundles
     * @param chosenBundle2 The bundle for child 2: 0 = not escorted. Max 3 bundles
     * @param chosenBundle3 The bundle for child 3: 0 = not escorted. Max 3 bundles
     * @param chosenChauf1 The chauffeur for child 1: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @param chosenChauf2 The chauffeur for child 2: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @param chosenChauf3 The chauffeur for child 3: 0 = not escorted; 1 = driver 1, rideshare; 2 = driver 1, pure escort; 3 = driver 2, rideshare; 4 = driver 2, pure escort
     * @return
     */
    private int[][] getResultsByChauffeurArray( Household hh, SchoolEscortingDmu decisionMaker, int direction, int chosenAlt, int chosenBundle1, int chosenBundle2, int chosenBundle3, int chosenChauf1, int chosenChauf2, int chosenChauf3 ) {
    	
    	int[][] resultsByChauffeurBundle = new int[hh.getHhSize()*NUM_BUNDLES+1][NUM_RESULTS_BY_CHAUF_FIELDS];

    	
    	int[] adultPnums = decisionMaker.getChauffeurPnums();
    	int[] childPnums = decisionMaker.getEscorteePnums();
    	
    	
        // set result attributes for chauffeurs
        for ( int pnum=1; pnum <= hh.getHhSize(); pnum++ ) {
        	Person person = hh.getPerson( pnum );
        	for ( int i=1; i <= NUM_BUNDLES; i++ ) {
        		int rowIndex = (pnum-1)*NUM_BUNDLES+i;
            	resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_BUNDLE_ID_FIELD] = i;
            	resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_HHID_FIELD] = hh.getHhId();
            	resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_PNUM_FIELD] = pnum;
                resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_PID_FIELD] = person.getPersonId();
                resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_PERSON_TYPE_FIELD] = person.getPersonTypeNumber();
                resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_AGE_FIELD] = person.getAge();
                resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CDAP_FIELD] = person.getCdapIndex();
        	}
        }

        for ( Person adult : hh.getAdultPersons() ) {
        	int pnum = adult.getPersonNum();
        	for ( int i=1; i <= NUM_BUNDLES; i++ ) {
        		int rowIndex = (pnum-1)*NUM_BUNDLES+i;
        		resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_DIRECTION_FIELD] = direction;
        		resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHOSEN_ALT_FIELD] = chosenAlt;
        	}
        }
        
        for ( int i=1; i < adultPnums.length; i++ ) {
        	if ( adultPnums[i] > 0 ) {
            	for ( int j=1; j <= NUM_BUNDLES; j++ ) {
            		int rowIndex = (adultPnums[i]-1)*NUM_BUNDLES+j;
            		resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ESCORT_ELIGIBLE_FIELD] = ESCORT_ELIGIBLE;
            		resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_DEPART_HOME_FIELD] = direction == DIR_OUTBOUND ? decisionMaker.getChauffeurDepartForMandatory()[i] : decisionMaker.getChauffeurDepartFromMandatory()[i];
            	}
        	}
        }


        if ( chosenChauf1 > 0 ) {
        	int childid = 1;
        	int chaufid = chosenChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf1 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf1 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf1 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf1 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
        	int bundle = chosenBundle1;
    		int rowIndex = (adultPnums[chaufid]-1)*NUM_BUNDLES + bundle;
        	
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ID_FIELD] = chaufid;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ESCORT_TYPE_FIELD] = escortType;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD1_PNUM_FIELD] = childPnums[childid];
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD1_PERSON_TYPE_FIELD] = hh.getPerson( childPnums[childid] ).getPersonTypeNumber();
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD1_DEPART_HOME_FIELD] = direction == DIR_OUTBOUND ? decisionMaker.getEscorteeDepartForSchool()[childid] : decisionMaker.getEscorteeDepartFromSchool()[childid];
        }
        
        if ( chosenChauf2 > 0 ) {
        	int childid = 2;
        	int chaufid = chosenChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf2 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf2 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf2 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf2 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
        	int bundle = chosenBundle2;
    		int rowIndex = (adultPnums[chaufid]-1)*NUM_BUNDLES + bundle;
        	
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ID_FIELD] = chaufid;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ESCORT_TYPE_FIELD] = escortType;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD2_PNUM_FIELD] = childPnums[childid];
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD2_PERSON_TYPE_FIELD] = hh.getPerson( childPnums[childid] ).getPersonTypeNumber();
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD2_DEPART_HOME_FIELD] = direction == DIR_OUTBOUND ? decisionMaker.getEscorteeDepartForSchool()[childid] : decisionMaker.getEscorteeDepartFromSchool()[childid];
        }
        
        if ( chosenChauf3 > 0 ) {
        	int childid = 3;
        	int chaufid = chosenChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_1 ? CHAUFFEUR_1 : chosenChauf3 == RIDE_SHARING_CHAUFFEUR_2 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? CHAUFFEUR_2 : 0;
        	int escortType = chosenChauf3 == RIDE_SHARING_CHAUFFEUR_1 || chosenChauf3 == RIDE_SHARING_CHAUFFEUR_2 ? ModelStructure.RIDE_SHARING_TYPE : chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_1 || chosenChauf3 == PURE_ESCORTING_CHAUFFEUR_2 ? ModelStructure.PURE_ESCORTING_TYPE : 0;
        	int bundle = chosenBundle3;
    		int rowIndex = (adultPnums[chaufid]-1)*NUM_BUNDLES + bundle;
        	
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ID_FIELD] = chaufid;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_ESCORT_TYPE_FIELD] = escortType;
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD3_PNUM_FIELD] = childPnums[childid];
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD3_PERSON_TYPE_FIELD] = hh.getPerson( childPnums[childid] ).getPersonTypeNumber();
            resultsByChauffeurBundle[rowIndex][RESULT_CHAUF_CHILD3_DEPART_HOME_FIELD] = direction == DIR_OUTBOUND ? decisionMaker.getEscorteeDepartForSchool()[childid] : decisionMaker.getEscorteeDepartFromSchool()[childid];
        }

    	return resultsByChauffeurBundle;
    	
    }
    
    /**
     * Modify and/or create tours based on the results of the school escort model.
     * 
     * @param household
     * @param escortBundleList      An array of escort bundles.
     */
    public void createTours(Household household, List<SchoolEscortingBundle> escortBundleList){
    	
    	
    	//for each bundle
    	for(SchoolEscortingBundle escortBundle : escortBundleList){
    		
    		//get chauffeur
    		int chauffeurPnum = escortBundle.getChaufPnum();
    		if(chauffeurPnum==0)
    			continue;
    		Person chauffeur = household.getPerson(chauffeurPnum);
    		
    		//get the list of ordered children in the bundle
    		int[] childPnums = escortBundle.getChildPnums();
    		int[] schoolMAZs = escortBundle.getSchoolMazs();
    		
    		Tour chauffeurTour = null;
    		int escortType = escortBundle.getEscortType();
    		int numStops = 0;
       		//**************************************************************************************************************
    		//
    		// Pure escort tour : Need to create the chauffeur tour
    		//                    Also, number of stops(trips) is equal to children
    		//
    		//**************************************************************************************************************
    		if(escortType==ModelStructure.PURE_ESCORTING_TYPE){

    			ArrayList<Tour> existingTours = chauffeur.getListOfIndividualNonMandatoryTours();
    			int id=0;
    			if(existingTours!=null)
    				id=existingTours.size();
    			else
    				existingTours = new ArrayList<Tour>();

    			//generate a non-mandatory escort tour
    			chauffeurTour = new Tour(id++, household, chauffeur, "Escort",
                        ModelStructure.INDIVIDUAL_NON_MANDATORY_CATEGORY, ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX);
    	           
    			chauffeurTour.setTourOrigMgra(household.getHhMgra());
    			chauffeurTour.setTourPurpose("Escort");

    			if(escortBundle.getDir() == DIR_OUTBOUND){
    				//the destination of the outbound tour is the school MAZ of the last child to drop off
    				int destMAZ = schoolMAZs[schoolMAZs.length-1];
    				chauffeurTour.setTourDestMgra(destMAZ);
     			}else{
    				//the destination of the inbound tour is the school MAZ of the first child to pick up
    				int destMAZ = schoolMAZs[0];
    				chauffeurTour.setTourDestMgra(destMAZ);
    			}
    				
    			int departPeriod = escortBundle.getDepartHome();
    			int arrivePeriod = escortBundle.getArriveHome();
				chauffeurTour.setTourDepartPeriod(departPeriod);
				chauffeurTour.setTourArrivePeriod(arrivePeriod);
    	        chauffeur.scheduleWindow(departPeriod, arrivePeriod);
    	        chauffeurTour.setValueOfTime(defaultVOT);
    	        existingTours.add(chauffeurTour);
    	        numStops = escortBundle.getChildPnums().length;
     
    		}
    		//**************************************************************************************************************
    		//
    		// Ridesharing tour: Need to find chauffeur tour in existing mandatory tour array
    		//                   Also, number of stops is equal to children + 1
    		//
    		//**************************************************************************************************************
    		if(escortType==ModelStructure.RIDE_SHARING_TYPE){
    			
    			// ********************************************************************************************
    			// Change the mandatory tour of the chauffeur
    			// ********************************************************************************************
    			ArrayList<Tour> chauffeurMandatoryTours = getMandatoryTours(chauffeur);
     			if(chauffeurMandatoryTours.isEmpty()){
    				logger.fatal("Error: trying to get mandatory tours for person "+chauffeurPnum+" in household "+household.getHhId()+" for ride-sharing bundle");
    				household.logEntireHouseholdObject("Escort model debug", logger);
    				throw new RuntimeException();
    			}
    			
     			//number of stops is number of children needing to be dropped off plus one for the primary destination/tour origin
     			numStops = childPnums.length+1;
     			
     			//get tour and find existing tour mode (if already set by this method)
     			if(escortBundle.getDir()==DIR_OUTBOUND)
    				chauffeurTour = chauffeurMandatoryTours.get(0);
      			else{
      				chauffeurTour = chauffeurMandatoryTours.get(chauffeurMandatoryTours.size() - 1 );
      			}
    		} // end if rideshare type
    		
  			//set tour mode to max of existing tour mode and mode for occupancy
  			int occupancy = childPnums.length + 1;
  			
  			//check for stops in the opposite direction, in order to set the occupancy to the max for the tour
  			int occupancyInOppositeDirection = 0;
  			if(escortBundle.getDir()==DIR_OUTBOUND){
  				Stop[] stops = chauffeurTour.getInboundStops();
  				if(stops!=null)
  					occupancyInOppositeDirection = stops.length;
  			}else{
 				Stop[] stops = chauffeurTour.getOutboundStops();
  				if(stops!=null)
  					occupancyInOppositeDirection = stops.length;
  			}
  				
			if(Math.max(occupancy,occupancyInOppositeDirection)==2)
				chauffeurTour.setTourModeChoice(SHARED_RIDE_2_MODE);
			else
				chauffeurTour.setTourModeChoice(SHARED_RIDE_3_MODE);
			
			
  			//create stops for each child on the chauffeurs tour if ride-share type, or children - 1 if pure escort
			if(escortBundle.getDir()==DIR_OUTBOUND){

				chauffeurTour.setEscortTypeOutbound(escortType);
    			chauffeurTour.setDriverPnumOutbound(chauffeurPnum);

    			//if this is a pure rideshare tour, and there's only one child, create one stop for the outbound direction and move on.
				if(escortType==ModelStructure.PURE_ESCORTING_TYPE && numStops == 1){ 
					Stop stop = chauffeurTour.createStop( "Home", "Escort", false, false);
			        stop.setOrig(household.getHhMgra());
			        stop.setDest(chauffeurTour.getTourDestMgra());
			        stop.setStopPeriod(chauffeurTour.getTourDepartPeriod());    
					stop.setMode(SHARED_RIDE_2_MODE);
			        stop.setEscorteePnumDest(childPnums[0]);
			        stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_DROPOFF);
				}//more than one stop on the outbound direction on pure escort or its a rideshare tour
				else{
   					//insert stops on tour for each child to be escorted
					String[] stopOrigPurposes = new String[numStops];
		            String[] stopDestPurposes = new String[numStops];
		            int[] stopPurposeIndices = new int[numStops];
		            stopOrigPurposes[0] = "Home"; 				
					
		            for(int i = 0; i < numStops-1; ++i){
		                if (i > 0)
		                    stopOrigPurposes[i] = stopDestPurposes[i - 1];
		                stopPurposeIndices[i] = ModelStructure.ESCORT_STOP_PURPOSE_INDEX;
		                stopDestPurposes[i] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
	
		            }
	 	            stopOrigPurposes[numStops-1] = stopDestPurposes[numStops - 2];
		            stopDestPurposes[numStops-1] = chauffeurTour.getTourPrimaryPurpose();
	   	            chauffeurTour.createOutboundStops(stopOrigPurposes, stopDestPurposes, stopPurposeIndices);
	   	            
	   	            Stop[] stops = chauffeurTour.getOutboundStops();
	   	            int origMAZ = household.getHhMgra();
	   	            int escorteePnumOrig=0;
	   	            byte escortStopTypeOrig=0;
	   	            for (int i = 0; i < stops.length; ++i) {
	   	            	Stop stop = stops[i]; 
	   	            	
	   	            	//mode to stop is the occupancy - number of stops so far
	   	            	int mode = 0;
	   	            	if(occupancy==1)
	   						mode = DRIVE_ALONE_MODE;
	  	            	else if(occupancy==2)
	   						mode = SHARED_RIDE_2_MODE;
	   					else
	   						mode = SHARED_RIDE_3_MODE;
	   	  				stop.setMode(mode);
	   	            	
	   	  				//decrement the occupancy
	   	  				occupancy--;
	   	            
	   	  				//set other information for the stop (really the trip to the school)
	  	  				stop.setEscorteePnumOrig(escorteePnumOrig);
	   	  				stop.setEscortStopTypeOrig(escortStopTypeOrig);
	   	  				stop.setOrig(origMAZ);
	  	  				stop.setStopPeriod(chauffeurTour.getTourDepartPeriod());
	  	  			      	  				
	   	  				if(i<stops.length-1){
	   	  					stop.setEscorteePnumDest(childPnums[i]);
	   	  					stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_DROPOFF);
	       	  				stop.setDest(schoolMAZs[i]);
	       	  				escorteePnumOrig = childPnums[i]; //origin pnum of next stop to this child pnum
	       	  				escortStopTypeOrig = ModelStructure.ESCORT_STOP_TYPE_DROPOFF; //origin stop type of next stop to this stop type
	      	  				origMAZ = schoolMAZs[i];  //set the origin of the next stop to this child's school MAZ
	 	  				}else{
	 	  					stop.setDest(chauffeurTour.getTourDestMgra());
	 	  					if(escortType==ModelStructure.PURE_ESCORTING_TYPE){
	 	   	  					stop.setEscorteePnumDest(childPnums[i]);
	 	   	  					stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_DROPOFF);
	 	  					}
	 	  				}
	    	        }
				}
			} //end if outbound
   
    		if(escortBundle.getDir()==DIR_INBOUND){
    			
      			chauffeurTour.setEscortTypeInbound(escortType);
        		chauffeurTour.setDriverPnumInbound(chauffeurPnum);

       			//if this is a pure rideshare tour, and there's only one child, create one stop for the inbound direction and move on.
    			if(escortType==ModelStructure.PURE_ESCORTING_TYPE && numStops == 1){ 
    				Stop stop = chauffeurTour.createStop( "Escort", "Home", true, false);
       			    stop.setOrig(chauffeurTour.getTourDestMgra());
       			 	stop.setDest(household.getHhMgra());
    			    stop.setStopPeriod(chauffeurTour.getTourArrivePeriod());    
    				stop.setMode(SHARED_RIDE_2_MODE);
    			    stop.setEscorteePnumOrig(childPnums[0]);
    			    stop.setEscortStopTypeOrig(ModelStructure.ESCORT_STOP_TYPE_PICKUP);
    			}//more than one stop on the outbound direction on pure escort or its a rideshare tour
    			else{
	      			//insert stops on tour for each child to be escorted on the chauffeurs mandatory tour
					String[] stopOrigPurposes = new String[numStops];
		            String[] stopDestPurposes = new String[numStops];
		            int[] stopPurposeIndices = new int[numStops];
		            stopOrigPurposes[0] = chauffeurTour.getTourPrimaryPurpose();			
					
		            for(int i = 0; i < numStops-1; ++i){
		                if (i > 0)
		                    stopOrigPurposes[i] = stopDestPurposes[i - 1];
		                stopPurposeIndices[i] = ModelStructure.ESCORT_STOP_PURPOSE_INDEX;
		                stopDestPurposes[i] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
	
		            }
	 	            stopOrigPurposes[numStops-1] = stopDestPurposes[numStops - 2];
		            stopDestPurposes[numStops-1] = "HOME";
	   	            chauffeurTour.createInboundStops(stopOrigPurposes, stopDestPurposes, stopPurposeIndices);
				    
	   	            Stop[] stops = chauffeurTour.getInboundStops();
	   	            int origMAZ = chauffeurTour.getTourDestMgra();
	   	            occupancy = 2; //driver plus child picked up at origin
	   	            
	   	            //if it is a pure escort tour, the origin of the first inbound stop is escort
	   	            int escorteePnumOrig=0;
	   	            byte escortStopTypeOrig=0;
	   	            if(escortType==ModelStructure.PURE_ESCORTING_TYPE){
		   	  			escorteePnumOrig = childPnums[0];
		   	  			escortStopTypeOrig = ModelStructure.ESCORT_STOP_TYPE_PICKUP;
		  			}
	
	   	            for (int i = 0; i < stops.length; ++i) {
	   	            	Stop stop = stops[i]; 
	   	            	int mode = 0;
	   	            	if(occupancy==2)
	   						mode = SHARED_RIDE_2_MODE;
	   					else
	   						mode = SHARED_RIDE_3_MODE;
	   	  				stop.setMode(mode);
	   	            	
	   	  				//increment the occupancy
	   	  				occupancy++;
	   	            
	   	  				//set the person being escorted
	   	  				stop.setEscorteePnumOrig(escorteePnumOrig);
	   	  				stop.setEscortStopTypeOrig(escortStopTypeOrig);
	 	  				stop.setStopPeriod(chauffeurTour.getTourArrivePeriod());
	    	  			escortStopTypeOrig = ModelStructure.ESCORT_STOP_TYPE_PICKUP; //origin stop type of next stop to this stop type
	 	  				stop.setOrig(origMAZ);
	 	  				
	 	  				
	 	  				int childIndex = i;
	 	  				if(escortType==ModelStructure.PURE_ESCORTING_TYPE)
	 	  					++childIndex;
	 	  				
	   	  				if(i<stops.length-1){ // for all stops but the last stop
	      	  				origMAZ = schoolMAZs[childIndex];  //set the origin of the next stop to this child's school MAZ
	     	  				escorteePnumOrig = childPnums[childIndex]; //origin pnum of next stop to this child pnum
	      	  			  	stop.setEscorteePnumDest(childPnums[childIndex]);
	       	  				stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_PICKUP);
	      	  				stop.setDest(schoolMAZs[childIndex]);
	      	  			}else{
	      	  				stop.setDest(household.getHhMgra());
	      	  			}
	   	            } //end for inbound stops
    			} //end check on more than one inbound stop\pure escort
			} //end if inbound

    	} //end each bundle
    }
    
    
    
    public void recodeSchoolTours(Household household){
    	
    	List<Person> adults = household.getActiveAdultPersons();
    	List<Person> children = household.getChildPersons();
    	
    	if(adults == null || children == null)
    		return;
    	
    	//cycle thru adults in household
    	for(Person adult : adults){
    		ArrayList<Tour> mandatoryTours = getMandatoryTours(adult);
       		ArrayList<Tour> nonMandatoryTours = adult.getListOfIndividualNonMandatoryTours();
       	 
       		ArrayList<Tour> chauffeurTours = new ArrayList<Tour>();
       		
       		//add mandatory tours to the potential list of chauffeur tours
       		if(mandatoryTours!=null)
       			if(mandatoryTours.size()>0)
       				chauffeurTours.addAll(mandatoryTours);
    		
       		//add non-mandatory tours to the potential list of chauffeur tours
    		if(nonMandatoryTours!=null)
    			if(nonMandatoryTours.size()>0)
    				chauffeurTours.addAll(nonMandatoryTours);
    		
    		//if there are no possible chauffeur tours, we're done.
    		if(chauffeurTours.size()==0){
    			return;
    		}
    		
    		//cycle thru mandatory tours for each adult
    		for(Tour chauffeurTour : chauffeurTours){
    			
    			if(chauffeurTour.getEscortTypeOutbound()==ModelStructure.RIDE_SHARING_TYPE||chauffeurTour.getEscortTypeOutbound()==ModelStructure.PURE_ESCORTING_TYPE){
    				
    				//cycle thru children in household
    				for(Person child : children){
    					
    					if(child.getListOfSchoolTours() !=null)
    						recodeSchoolTour(household, child.getPersonNum(), chauffeurTour, DIR_OUTBOUND);
    				}
    			}
    			if(chauffeurTour.getEscortTypeInbound()==ModelStructure.RIDE_SHARING_TYPE||chauffeurTour.getEscortTypeInbound()==ModelStructure.PURE_ESCORTING_TYPE){
    				
    				//cycle thru children in household
    				for(Person child : children){
    					
    					if(child.getListOfSchoolTours() !=null)
    						recodeSchoolTour(household, child.getPersonNum(), chauffeurTour, DIR_INBOUND);
    				}
    			}
    				
    		}
    	}
    }
    
    /**
     * Recode the child's school tour to be consistent with the chauffeurTour for the given direction. If the
     * child does not have any school tours, the method will simply return. If there is a school tour, and the 
     * chauffeur is escorting the child, then the child's stop sequence, tour and trip modes, and other
     * relevant data is made consistent with the chauffeur's tour. 
     * 
     * @param household 	Household object for the given child pnum.
     * @param childPnum     The person number of the child.
     * @param chauffeurTour The chauffeurTour to use for checking\coding.
     * @param direction     Outbound or inbound direction.
     */
    public void recodeSchoolTour(Household household, int childPnum, Tour chauffeurTour, int direction){
    	
    	//get child's school tour
    	Person child = household.getPerson(childPnum);
    	ArrayList<Tour> schoolTours = child.getListOfSchoolTours();
    	
    	// no school tours for this child
    	if(schoolTours.isEmpty()){
			return;
    	}
    	Tour schoolTour = schoolTours.get(0);
    	
    	if(direction==DIR_OUTBOUND){
    		Stop[] chauffeurStops = chauffeurTour.getOutboundStops();

        	int driverPnum = chauffeurTour.getDriverPnumOutbound();
     
    		//loop through chauffeur tour stops
    		for(int i = 0; i < chauffeurStops.length; ++i){
    			Stop chauffeurStop = chauffeurStops[i];
    			
    			int occupancy=0;
    			if(chauffeurTour.getTourPurpose().equals("Escort"))
    				occupancy = chauffeurStops.length+1; //occupancy of last trip is equal to number of stops + 1 for first child picked up
    			else
    				occupancy = chauffeurStops.length;

    			if(chauffeurStop.getEscorteePnumDest()==childPnum){
    				 
    				int existingTourMode = schoolTour.getTourModeChoice();
    				schoolTour.setTourModeChoice(Math.max(existingTourMode,chauffeurTour.getTourModeChoice()));
    	        	schoolTour.setDriverPnumOutbound(driverPnum);
    	        	schoolTour.setEscortTypeOutbound(chauffeurTour.getEscortTypeOutbound());

    				//child is first stop; no intermediate stops on this child's school tour in the outbound direction 
    				if(i==0){ 
    					Stop stop = schoolTour.createStop( "Home", "School", false, false);
    			        stop.setOrig(household.getHhMgra());
    			        stop.setDest(schoolTour.getTourDestMgra());
    			        stop.setStopPeriod(schoolTour.getTourDepartPeriod());    
    					if(occupancy==2)
    			        	stop.setMode(SHARED_RIDE_2_MODE);
    			        else
    			        	stop.setMode(SHARED_RIDE_3_MODE);
    			        stop.setEscorteePnumDest(childPnum);
    			        stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_DROPOFF);
    			        break;
    				}
    				
    				//child is second or third stop; create outbound stops array with one or two previous stops.
    				if(i>0){
    	   				//insert stops on tour for each child to be escorted
        				String[] stopOrigPurposes = new String[i + 1];
        	            String[] stopDestPurposes = new String[i + 1];
        	            int[] stopPurposeIndices = new int[i + 1];
        	            stopOrigPurposes[0] = "Home"; 				
        				
        	            for(int j = 0; j < i; ++j){
        	                if (j > 0)
        	                    stopOrigPurposes[j] = stopDestPurposes[j - 1];
        	                stopPurposeIndices[j] = ModelStructure.ESCORT_STOP_PURPOSE_INDEX;
        	                stopDestPurposes[j] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
        	            }
         	            stopOrigPurposes[i] = stopDestPurposes[i - 1];
        	            stopDestPurposes[i] = schoolTour.getTourPrimaryPurpose();
           	            schoolTour.createOutboundStops(stopOrigPurposes, stopDestPurposes, stopPurposeIndices);
           	            
           	            Stop[] stops = schoolTour.getOutboundStops();
           	            int origMAZ = household.getHhMgra();
           				int escorteePnumOrig=0;
           	            byte escortStopTypeOrig=0;
           				for (int j = 0; j < stops.length; ++j) {
           	            	Stop stop = stops[j]; 
           			        stop.setOrig(origMAZ);
        			        stop.setDest(chauffeurStops[j].getDest());
        			        origMAZ = stop.getDest();
        			        stop.setStopPeriod(schoolTour.getTourDepartPeriod());    
          			        if(occupancy==2)
        			        	stop.setMode(SHARED_RIDE_2_MODE);
        			        else
        			        	stop.setMode(SHARED_RIDE_3_MODE);
          			        stop.setEscorteePnumOrig(escorteePnumOrig);
          			        stop.setEscortStopTypeOrig(escortStopTypeOrig);
          			        stop.setEscorteePnumDest(chauffeurStops[j].getEscorteePnumDest());
        			        stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_DROPOFF);
        			        escorteePnumOrig = chauffeurStops[j].getEscorteePnumDest();
        			        escortStopTypeOrig = ModelStructure.ESCORT_STOP_TYPE_DROPOFF;
           				}
           	         break;
           	         }
           	        
    			} //end if found child in chauffeur stop array
			    --occupancy;
			        
    		} //end cycling through stops in outbound direction
    		
    		
    	} //end if in outbound direction
    	
    	// things are more complicated in the inbound direction. in this case, the child who is the last to be 
    	// picked up has the simple tour.
       	if(direction==DIR_INBOUND){
    		Stop[] chauffeurStops = chauffeurTour.getInboundStops();
  	  	
        	int driverPnum = chauffeurTour.getDriverPnumInbound();
 	    	
    		//loop through chauffeur tour stops from last to first
    		for(int i = chauffeurStops.length - 1; i >=0; --i){
    			Stop chauffeurStop = chauffeurStops[i];
    			if(chauffeurStop.getEscorteePnumOrig()==childPnum){
    				
    				int existingTourMode = schoolTour.getTourModeChoice();
    				schoolTour.setTourModeChoice(Math.max(existingTourMode,chauffeurTour.getTourModeChoice()));
    	        	schoolTour.setDriverPnumInbound(driverPnum);
    	        	schoolTour.setEscortTypeInbound(chauffeurTour.getEscortTypeInbound());

    	        	//child is last stop; no intermediate stops on this child's school tour in the inbound direction 
    				if(i==chauffeurStops.length-1){ 
    					Stop stop = schoolTour.createStop( "School", "Home", true, false);
     			        stop.setOrig(schoolTour.getTourDestMgra());
       			        stop.setDest(household.getHhMgra());
    			        stop.setStopPeriod(schoolTour.getTourArrivePeriod());    
    			       	int occupancy=0;
    					if(chauffeurTour.getTourPurpose().equals("Escort"))
    						occupancy = chauffeurStops.length+1; //occupancy of last trip is equal to number of stops + 1 for first child picked up
    					else
    						occupancy = chauffeurStops.length;
    					if(occupancy==2)
    			        	stop.setMode(SHARED_RIDE_2_MODE);
    			        else
    			        	stop.setMode(SHARED_RIDE_3_MODE);
      			        stop.setEscorteePnumOrig(childPnum);
    			        stop.setEscortStopTypeOrig(ModelStructure.ESCORT_STOP_TYPE_PICKUP);
    			        break;
    				}
    				
    				//child is not last stop; create inbound stops array with one or two subsequent stops.
    				if(i<chauffeurStops.length-1){
    	   				//insert stops on tour for each child to be escorted
    					int numberOfOtherChildrenToPickup = chauffeurStops.length - 1 - i;
        				String[] stopOrigPurposes = new String[numberOfOtherChildrenToPickup + 1];
        	            String[] stopDestPurposes = new String[numberOfOtherChildrenToPickup + 1];
        	            int[] stopPurposeIndices = new int[numberOfOtherChildrenToPickup + 1];
        	            stopOrigPurposes[0] = "School"; 				
        				
        	            for(int j = 0; j < numberOfOtherChildrenToPickup; ++j){
        	                if (j > 0)
        	                    stopOrigPurposes[j] = stopDestPurposes[j - 1];
        	                stopPurposeIndices[j] = ModelStructure.ESCORT_STOP_PURPOSE_INDEX;
        	                stopDestPurposes[j] = ModelStructure.ESCORT_PRIMARY_PURPOSE_NAME;
        	            }
         	            stopOrigPurposes[numberOfOtherChildrenToPickup] = stopDestPurposes[numberOfOtherChildrenToPickup - 1];
        	            stopDestPurposes[numberOfOtherChildrenToPickup] = "Home";
           	            schoolTour.createInboundStops(stopOrigPurposes, stopDestPurposes, stopPurposeIndices);
           	            
           	            Stop[] stops = schoolTour.getInboundStops();
           	            int origMAZ = chauffeurTour.getTourDestMgra();
       				    int escorteePnumOrig=0;
           	            byte escortStopTypeOrig=0;
           	            for (int j = 0; j < stops.length; ++j) {
           	            	Stop stop = stops[j]; 
           			        stop.setOrig(origMAZ);
        			        stop.setDest(chauffeurStops[j].getDest());
        			        origMAZ = stop.getDest();
        			        stop.setStopPeriod(schoolTour.getTourDepartPeriod());    
        					stop.setMode(chauffeurStops[j].getMode());
        			        stop.setEscorteePnumOrig(escorteePnumOrig);
        			        stop.setEscortStopTypeOrig(escortStopTypeOrig);
        					stop.setEscorteePnumDest(chauffeurStops[j].getEscorteePnumDest());
        			        stop.setEscortStopTypeDest(ModelStructure.ESCORT_STOP_TYPE_PICKUP);
        			        escorteePnumOrig = chauffeurStops[j].getEscorteePnumDest();
        			        escortStopTypeOrig = ModelStructure.ESCORT_STOP_TYPE_PICKUP;
        			    }
           	         break;
           	         }
           	        
    			} //end if found child in chauffeur stop array
    		} //end cycling through stops in inbound direction
    		
       	} //end if inbound direction
    }
}
