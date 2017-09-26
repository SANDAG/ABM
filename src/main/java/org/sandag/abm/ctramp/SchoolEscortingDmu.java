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
*/

package  org.sandag.abm.ctramp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pb.common.calculator.VariableTable;
import com.pb.common.matrix.Matrix;
import com.pb.common.util.IndexSort;




import org.apache.log4j.Logger;
import org.sandag.abm.modechoice.MgraDataManager;



public class SchoolEscortingDmu implements VariableTable {

    private Logger logger = Logger.getLogger( SchoolEscortingDmu.class );

    private static final float DROP_OFF_DURATION = 5.0f;
    private static final float PICK_UP_DURATION = 10.0f;
    private static final float MINUTES_PER_MILE = 2.0f;
    
    
    private Household hhObj;
    
    private float[] distHomeSchool;
    private float[] timeHomeSchool;
    private float[] distSchoolHome;
    private float[] timeSchoolHome;
    
    //for each cheaffeur
    private float[] distHomeMandatory;
    private float[] timeHomeMandatory;    
    private float[] distMandatoryHome;
    private float[] timeMandatoryHome;    
    
    private float[][] distSchoolSchool;
    private float[][] distSchoolMandatory;
    private float[][] distMandatorySchool;
    
    private Person[] escortees;
    
    
    private int[] escorteeIds;
    private int[] escorteePnums;
    private int[] escorteeAge;

    private int[] escorteeSchoolLoc;
    private int[] escorteeSchoolAtHome;
    private int[] escorteeDepartForSchool;
    private int[] escorteeDepartFromSchool;
    private int numChildrenTravelingToSchool;

    private Person[] chauffers;
    private int[] chauffeurPnums;
    private int[] chauffeurPids;
    private int[] chauffeurAge;
    private int[] chauffeurGender;
    private int[] chauffeurPersonType;
    private int[] chauffeurDap;
    private int[] chauffeurMandatoryLoc;
    private int[] chauffeurDepartForMandatory;
    private int[] chauffeurDepartFromMandatory;
    private int numPotentialChauffeurs;

    private int[][][] chaufExtents;
    
    private int chosenObEscortType1;
    private int chosenObEscortType2;
    private int chosenObEscortType3;
    private int chosenObChauf1;    
    private int chosenObChauf2;    
    private int chosenObChauf3;    
    private int potentialObChauf1;
    private int potentialObChauf2;
    
    private int chosenIbEscortType1;
    private int chosenIbEscortType2;
    private int chosenIbEscortType3;
    private int chosenIbChauf1;    
    private int chosenIbChauf2;    
    private int chosenIbChauf3;    
    private int potentialIbChauf1;
    private int potentialIbChauf2;

    private MgraDataManager mgraDataManager;

    private double[][] distanceArray;
    
    private int[][] altBundleIncidence;
    
    private Map<String, Integer> methodIndexMap;

    

    /**
     * Create the DMU by passing in...
     * @param MgraDataManager mgraDataManager
     * @param Matrix distanceMatrix
     */
	public SchoolEscortingDmu(MgraDataManager mgraDataManager, double[][] distanceArray) {
		
		this.mgraDataManager = mgraDataManager;
		this.distanceArray = distanceArray;
		
		chauffeurPnums = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurPids = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurAge = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurGender = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurPersonType = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurDap = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurMandatoryLoc = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurDepartForMandatory = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		chauffeurDepartFromMandatory = new int[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		distHomeMandatory = new float[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		timeHomeMandatory = new float[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		distMandatoryHome = new float[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		timeMandatoryHome = new float[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		
		escorteeIds = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		escorteePnums = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		escorteeAge = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		escorteeSchoolLoc = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		escorteeSchoolAtHome = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		escorteeDepartForSchool = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];		
		escorteeDepartFromSchool = new int[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];		
		distHomeSchool = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		timeHomeSchool = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		distSchoolHome = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		timeSchoolHome = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		
		distSchoolSchool = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1][SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
		distSchoolMandatory = new float[SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1][SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1];
		distMandatorySchool = new float[SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1][SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1];
	
		setupMethodIndexMap();
	}
	

	public void setAltTableBundleIncidence( int[][] altBundleIncidence ) {
		
		this.altBundleIncidence = altBundleIncidence;
		
	}
	
	/**
	 * Set attributes of the potential chauffeurs
	 * @param numPotential  Number of potential chauffeurs (size of adults array)
	 * @param adults        Ordered array of chauffeurs
	 * @param mandatoryMazs      Array size of chauffeurs, holding MAZ of last mandatory tour
	 * @param mandatoryDeparts   Array size of chauffeurs, holding home departure period of last mandatory tour
	 * @param mandatoryReturns   Array size of chauffeurs, holding work departure period of last mandatory tour
	 * @param chaufExtents  Array size of chauffeurs, 
	 */
	public void setChaufferAttributes( int numPotential, Person[] adults, int[] mandatoryMazs, int[] mandatoryDeparts, int[] mandatoryReturns, int[][][] chaufExtents ) {
		
		this.chaufExtents = chaufExtents;
		
		chauffers = adults;
		numPotentialChauffeurs = numPotential;
		
		for ( int i=1; i < chauffers.length; i++ ) {
    		if ( chauffers[i] == null ) {
				chauffeurAge[i] = 0;
				chauffeurGender[i] = 0;
				chauffeurPersonType[i] = 0;
				chauffeurDap[i] = 0;
				chauffeurMandatoryLoc[i] = 0;
				chauffeurDepartForMandatory[i] = 0;
				chauffeurDepartFromMandatory[i] = 0;
				chauffeurPnums[i] = 0;
				chauffeurPids[i] = 0;
    		}
    		else  {
    			chauffeurPnums[i] = chauffers[i].getPersonNum();
				chauffeurPids[i] = chauffers[i].getPersonId();
    			chauffeurAge[i] = chauffers[i].getAge();
    			chauffeurGender[i] = chauffers[i].getGender();
    			chauffeurPersonType[i] = chauffers[i].getPersonTypeNumber();
    			chauffeurDap[i] = chauffers[i].getCdapIndex();
        		if ( mandatoryMazs[i] == 0 ) {
					chauffeurMandatoryLoc[i] = 0;
					chauffeurDepartForMandatory[i] = 0;
					chauffeurDepartFromMandatory[i] = 0;
        		}
        		else {
					chauffeurMandatoryLoc[i] = mandatoryMazs[i];
					chauffeurDepartForMandatory[i] = mandatoryDeparts[i];
					chauffeurDepartFromMandatory[i] = mandatoryReturns[i];
        		}
    		}
		}
	}
	
	/**
	 * Set attributes for escortees.
	 * 
	 * @param numPotential  Number of potential escortees (children traveling to school).
	 * @param children      Person array of potential escortees
	 * @param schoolAtHome  An array for each person indicating if they are schooled at home
	 * @param schoolMazs    An array of school MAZs for each person
	 * @param schoolDeparts An array of school tour outbound periods
	 * @param schoolReturns an array of school tour return periods
	 */
	public void setEscorteeAttributes( int numPotential, Person[] children, int[] schoolAtHome, int[] schoolMazs, int[] schoolDeparts, int[] schoolReturns ) {

		escortees = children;
		numChildrenTravelingToSchool = numPotential;
		
		for ( int i=1; i < escortees.length; i++ ) {
    		if ( escortees[i] == null || schoolMazs[i] == 0 ) {
    			escorteeIds[i] = 0;
    			escorteePnums[i] = 0;
    			escorteeAge[i] = 0;
    			escorteeSchoolLoc[i] = 0;
    			escorteeSchoolAtHome[i] = 0;
    			escorteeDepartForSchool[i] = 0;
    			escorteeDepartFromSchool[i] = 0;
    		}
    		else {
    			escorteeIds[i] = i;
    			escorteePnums[i] = escortees[i].getPersonNum();
    			escorteeAge[i] = escortees[i].getAge();
    			escorteeSchoolLoc[i] = schoolMazs[i];
    			escorteeSchoolAtHome[i] = schoolAtHome[i];
    			escorteeDepartForSchool[i] = schoolDeparts[i];
    			escorteeDepartFromSchool[i] = schoolReturns[i];
    		}

		}
		
	}

	/**
	 * Sets distance time attributes for combinations of chauffeur mandatory locations and escortee school locations.
	 * @param hhObj
	 * @param distanceArray
	 */
	public void setDistanceTimeAttributes( Household hhObj, double[][] distanceArray ) {

		this.hhObj = hhObj;
		
		int homeMaz = hhObj.getHhTaz();
		int homeTaz = mgraDataManager.getTaz(homeMaz);
		
		// compute times and distances from "home to work" and from "work to home" by traversing the chain of business locations for work tours
		// to/from the primary work location for work tours and the chain that may include a work location for school tours.
		
		//for each chauffeur
		for ( int i=1; i < chauffers.length; i++ ) {
    		if ( chauffers[i] == null ) {
				distHomeMandatory[i] = 0;
				timeHomeMandatory[i] = 0;
				distMandatoryHome[i] = 0;
				timeMandatoryHome[i] = 0;
    		}
    		else {
				if ( chauffeurMandatoryLoc[i] > 0 ) {
					
					distHomeMandatory[i] = 0;
					timeHomeMandatory[i] = 0;
					distMandatoryHome[i] = 0;
					timeMandatoryHome[i] = 0;

				    //the MAG model would traverse all the activities on the tour, skipping non-work and non-school tours, and skipping
					//non-work activities, and sum up the distance from home to each work activity. In the case of ORRAMP, only the work
					//primary destination is known, so the method has been re-written accordingly to use work primary destination for workers.
					
					int mandatoryMaz = chauffeurMandatoryLoc[i];
					int mandatoryTaz = mgraDataManager.getTaz(mandatoryMaz);
					
					distHomeMandatory[i] = (float) distanceArray[homeTaz][ mandatoryTaz];
					timeHomeMandatory[i] = MINUTES_PER_MILE * (float) distanceArray[homeTaz][mandatoryTaz];
					distMandatoryHome[i] = (float) distanceArray[mandatoryTaz][homeTaz];
					timeMandatoryHome[i] = MINUTES_PER_MILE * (float) distanceArray[mandatoryTaz][homeTaz];
					
				}
				else {
					distHomeMandatory[i] = 0;
					timeHomeMandatory[i] = 0;
					distMandatoryHome[i] = 0;
					timeMandatoryHome[i] = 0;
				}
    		}
		}

	    //iterating through potential escortees (i)
		for ( int i=1; i < escortees.length; i++ ) {
    		if ( escortees[i] == null ) {
				distHomeSchool[i] = 0;
				timeHomeSchool[i] = 0;
				distSchoolHome[i] = 0;
				timeSchoolHome[i] = 0;
				for ( int j=1; j < chauffeurPnums.length; j++ ) {
					distSchoolMandatory[i][j] = 0;
					distMandatorySchool[j][i] = 0;
				}
				for ( int j=1; j < escorteePnums.length; j++ )
					distSchoolSchool[i][j] = 0;
    		}
    		else {
    			if ( escorteeSchoolLoc[i] > 0 ) {
    				
    				int schoolTaz = mgraDataManager.getTaz(escorteeSchoolLoc[i]);
    				
    				distHomeSchool[i] = (float) distanceArray[homeTaz][schoolTaz];
    				timeHomeSchool[i] = MINUTES_PER_MILE * (float) distanceArray[homeTaz][schoolTaz];
    				distSchoolHome[i] = (float) distanceArray[schoolTaz][ homeTaz];
    				timeSchoolHome[i] = MINUTES_PER_MILE * (float) distanceArray[schoolTaz][homeTaz];
    				
    				//iterating through potential chauffeurs (j)
    				for ( int j=1; j < chauffeurPnums.length; j++ ) {
    					distSchoolMandatory[i][j] = 0;
    					distMandatorySchool[j][i] = 0;
    					if ( chauffeurMandatoryLoc[j] > 0 ) {
    						int mandatoryMaz = chauffeurMandatoryLoc[j];
    						int mandatoryTaz = mgraDataManager.getTaz(mandatoryMaz);
    						distSchoolMandatory[i][j] = (float) distanceArray[schoolTaz][mandatoryTaz];
    						distMandatorySchool[j][i] = (float) distanceArray[mandatoryTaz][schoolTaz];
    					}				
    				}
    				
    				for ( int j=1; j < escorteePnums.length; j++ ) {
    					distSchoolSchool[i][j] = 0;
    					if ( escorteeSchoolLoc[j] > 0 && escorteeSchoolLoc[j] != escorteeSchoolLoc[i]  ) {
    						int schoolTazJ = mgraDataManager.getTaz(escorteeSchoolLoc[j]);
    						distSchoolSchool[i][j] = (float) distanceArray[schoolTaz][schoolTazJ];
    					}				
    				}
    			}
    			else {
    				distHomeSchool[i] = 0;
    				timeHomeSchool[i] = 0;
    				distSchoolHome[i] = 0;
    				timeSchoolHome[i] = 0;
    				for ( int j=1; j < chauffeurPnums.length; j++ ) {
    					distSchoolMandatory[i][j] = 0;
    					distMandatorySchool[j][i] = 0;
    				}
    				for ( int j=1; j < escorteePnums.length; j++ )
    					distSchoolSchool[i][j] = 0;
    			}
    		}

		}

	}
	
	
	public void setOutboundEscortType1( int chosenObEscortType ) {
		chosenObEscortType1 = chosenObEscortType;
	}
	
	public void setOutboundEscortType2( int chosenObEscortType ) {
		chosenObEscortType2 = chosenObEscortType;
	}
	
	public void setOutboundEscortType3( int chosenObEscortType ) {
		chosenObEscortType3 = chosenObEscortType;
	}
	
	public void setOutboundChauffeur1( int chosenObChauf ) {
		chosenObChauf1 = chosenObChauf;
	}
	
	public void setOutboundChauffeur2( int chosenObChauf ) {
		chosenObChauf2 = chosenObChauf;
	}
	
	public void setOutboundChauffeur3( int chosenObChauf ) {
		chosenObChauf3 = chosenObChauf;
	}
	
	public void setOutboundPotentialChauffeur1( int chaufPnum ) {
		potentialObChauf1 = chaufPnum;
	}
	
	public void setOutboundPotentialChauffeur2( int chaufPnum ) {
		potentialObChauf2 = chaufPnum;
	}

	
	
	public void setInboundEscortType1( int chosenIbEscortType ) {
		chosenIbEscortType1 = chosenIbEscortType;
	}
	
	public void setInboundEscortType2( int chosenIbEscortType ) {
		chosenIbEscortType2 = chosenIbEscortType;
	}
	
	public void setInboundEscortType3( int chosenIbEscortType ) {
		chosenIbEscortType3 = chosenIbEscortType;
	}
	
	public void setInboundChauffeur1( int chosenIbChauf ) {
		chosenIbChauf1 = chosenIbChauf;
	}
	
	public void setInboundChauffeur2( int chosenIbChauf ) {
		chosenIbChauf2 = chosenIbChauf;
	}
	
	public void setInboundChauffeur3( int chosenIbChauf ) {
		chosenIbChauf3 = chosenIbChauf;
	}
		
	public void setInboundPotentialChauffeur1( int chaufPnum ) {
		potentialIbChauf1 = chaufPnum;
	}
	
	public void setInboundPotentialChauffeur2( int chaufPnum ) {
		potentialIbChauf2 = chaufPnum;
	}

	public int[] getChauffeurPnums() {
		return chauffeurPnums;
	}
	
	public int[] getChauffeurDepartForMandatory() {
		return chauffeurDepartForMandatory;
	}
	
	public int[] getChauffeurDepartFromMandatory() {
		return chauffeurDepartFromMandatory;
	}
	
	public int[] getEscorteePnums() {
		return escorteePnums;
	}
	
	public int[] getEscorteeDepartForSchool() {
		return escorteeDepartForSchool;
	}
	
	public int[] getEscorteeDepartFromSchool() {
		return escorteeDepartFromSchool;
	}
	
	public int[] getEscorteeSchoolAtHome() {
		return escorteeSchoolAtHome;
	}
	
	public int[] getEscorteeDistToSchool() {
		int[] tempDist = new int[distHomeSchool.length];
		for ( int i=1; i < distHomeSchool.length; i++ )
			tempDist[i] = (int)(distHomeSchool[i] * 100);
		return tempDist;
	}
	
	public int[] getEscorteeDistFromSchool() {
		int[] tempDist = new int[distSchoolHome.length];
		for ( int i=1; i < distSchoolHome.length; i++ )
			tempDist[i] = (int)(distSchoolHome[i] * 100);
		return tempDist;
	}
	

	
	private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();
        
        methodIndexMap.put( "getChild1Pnum", 1 );
        methodIndexMap.put( "getChild2Pnum", 2 );
        methodIndexMap.put( "getChild3Pnum", 3 );
        methodIndexMap.put( "getAdult1Pnum", 4 );
        methodIndexMap.put( "getAdult2Pnum", 5 );
        methodIndexMap.put( "getAgeChild1", 6 );
        methodIndexMap.put( "getAgeChild2", 7 );
        methodIndexMap.put( "getAgeChild3", 8 );
        methodIndexMap.put( "getSchoolMazChild1", 9 );
        methodIndexMap.put( "getSchoolMazChild2", 10 );
        methodIndexMap.put( "getSchoolMazChild3", 11 );
        methodIndexMap.put( "getDistHomeSchool1", 12 );
        methodIndexMap.put( "getDistHomeSchool2", 13 );
        methodIndexMap.put( "getDistHomeSchool3", 14 );
        methodIndexMap.put( "getDistSchoolHome1", 15 );
        methodIndexMap.put( "getDistSchoolHome2", 16 );
        methodIndexMap.put( "getDistSchoolHome3", 17 );
        methodIndexMap.put( "getTimeHomeSchool1", 18 );
        methodIndexMap.put( "getTimeHomeSchool2", 19 );
        methodIndexMap.put( "getTimeHomeSchool3", 20 );
        methodIndexMap.put( "getTimeSchoolHome1", 21 );
        methodIndexMap.put( "getTimeSchoolHome2", 22 );
        methodIndexMap.put( "getTimeSchoolHome3", 23 );
        methodIndexMap.put( "getDepartHomeSchool1", 24 );
        methodIndexMap.put( "getDepartHomeSchool2", 25 );
        methodIndexMap.put( "getDepartHomeSchool3", 26 );
        methodIndexMap.put( "getDepartSchoolHome1", 27 );
        methodIndexMap.put( "getDepartSchoolHome2", 28 );
        methodIndexMap.put( "getDepartSchoolHome3", 29 );
        methodIndexMap.put( "getGenderAdult1", 30 );
        methodIndexMap.put( "getGenderAdult2", 31 );
        methodIndexMap.put( "getPersonTypeAdult1", 32 );
        methodIndexMap.put( "getPersonTypeAdult2", 33 );
        methodIndexMap.put( "getAgeAdult1", 34 );
        methodIndexMap.put( "getAgeAdult2", 35 );
        methodIndexMap.put( "getDepartHomeWorkAdult1", 36 );
        methodIndexMap.put( "getDepartHomeWorkAdult2", 37 );
        methodIndexMap.put( "getDepartWorkHomeAdult1", 38 );
        methodIndexMap.put( "getDepartWorkHomeAdult2", 39 );
        methodIndexMap.put( "getDapAdult1", 40 );
        methodIndexMap.put( "getDapAdult2", 41 );
        methodIndexMap.put( "getDistHomeWork1", 42 );
        methodIndexMap.put( "getDistHomeWork2", 43 );
        methodIndexMap.put( "getTimeHomeWork1", 44 );
        methodIndexMap.put( "getTimeHomeWork2", 45 );
        methodIndexMap.put( "getDistWorkHome1", 46 );
        methodIndexMap.put( "getDistWorkHome2", 47 );
        methodIndexMap.put( "getTimeWorkHome1", 48 );
        methodIndexMap.put( "getTimeWorkHome2", 49 );
        methodIndexMap.put( "getDistSchool1School2", 50 );
        methodIndexMap.put( "getDistSchool1School3", 51 );
        methodIndexMap.put( "getDistSchool2School3", 52 );
        methodIndexMap.put( "getDistSchool1Work1", 53 );
        methodIndexMap.put( "getDistSchool1Work2", 54 );
        methodIndexMap.put( "getDistSchool2Work1", 55 );
        methodIndexMap.put( "getDistSchool2Work2", 56 );
        methodIndexMap.put( "getDistSchool3Work1", 57 );
        methodIndexMap.put( "getDistSchool3Work2", 58 );
        methodIndexMap.put( "getDistWork1School1", 59 );
        methodIndexMap.put( "getDistWork2School1", 60 );
        methodIndexMap.put( "getDistWork1School2", 61 );
        methodIndexMap.put( "getDistWork2School2", 62 );
        methodIndexMap.put( "getDistWork1School3", 63 );
        methodIndexMap.put( "getDistWork2School3", 64 );
        methodIndexMap.put( "getIncome", 65 );
        methodIndexMap.put( "getNumAutosInHH", 66 );
        methodIndexMap.put( "getNumWorkersInHH", 67 );
        methodIndexMap.put( "getNumChildrenWithSchoolOutsideOfHomeAndDap1", 68 );
        methodIndexMap.put( "getNumAdultsinHHDap12", 69 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild1Chauffeur1", 70 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild1Chauffeur2", 71 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild2Chauffeur1", 72 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild2Chauffeur2", 73 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild3Chauffeur1", 74 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild3Chauffeur2", 75 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild12Chauffeur1", 76 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild12Chauffeur2", 77 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild13Chauffeur1", 78 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild13Chauffeur2", 79 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild23Chauffeur1", 80 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild23Chauffeur2", 81 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild123Chauffeur1", 82 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceOutboundChild123Chauffeur2", 83 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild1Chauffeur1", 84 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild1Chauffeur2", 85 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild2Chauffeur1", 86 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild2Chauffeur2", 87 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild3Chauffeur1", 88 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild3Chauffeur2", 89 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild12Chauffeur1", 90 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild12Chauffeur2", 91 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild13Chauffeur1", 92 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild13Chauffeur2", 93 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild23Chauffeur1", 94 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild23Chauffeur2", 95 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild123Chauffeur1", 96 );
        methodIndexMap.put( "getAbsoluteDeviationDistanceInboundChild123Chauffeur2", 97 );
        methodIndexMap.put( "getInboundEscortType1", 98 );
        methodIndexMap.put( "getInboundEscortType2", 99 );
        methodIndexMap.put( "getInboundEscortType3", 100 );
        methodIndexMap.put( "getInboundChauffeur1", 101 );
        methodIndexMap.put( "getInboundChauffeur2", 102 );
        methodIndexMap.put( "getInboundChauffeur3", 103 );
        methodIndexMap.put( "getOutboundEscortType1", 104 );
        methodIndexMap.put( "getOutboundEscortType2", 105 );
        methodIndexMap.put( "getOutboundEscortType3", 106 );
        methodIndexMap.put( "getOutboundChauffeur1", 107 );
        methodIndexMap.put( "getOutboundChauffeur2", 108 );
        methodIndexMap.put( "getOutboundChauffeur3", 109 );
        methodIndexMap.put( "getInboundPotentialChauffeur1", 110 );
        methodIndexMap.put( "getInboundPotentialChauffeur2", 111 );
        methodIndexMap.put( "getOutboundPotentialChauffeur1", 112 );
        methodIndexMap.put( "getOutboundPotentialChauffeur2", 113 );
        methodIndexMap.put( "getTravelTimeWork1School1", 114 );
        methodIndexMap.put( "getTravelTimeWork2School1", 115 );
        methodIndexMap.put( "getTravelTimeWork1School2", 116 );
        methodIndexMap.put( "getTravelTimeWork2School2", 117 );
        methodIndexMap.put( "getTravelTimeWork1School3", 118 );
        methodIndexMap.put( "getTravelTimeWork2School3", 119 );
        methodIndexMap.put( "getTravelTimeWork1Home", 120 );
        methodIndexMap.put( "getTravelTimeWork2Home", 121 );
        methodIndexMap.put( "getAvailabilityForMultipleBundlesOutbound", 122 );
        methodIndexMap.put( "getAvailabilityForMultipleBundlesInbound", 123 );
        methodIndexMap.put( "getAvailabilityForInboundChauf1WithOutboundBundles", 124);
        methodIndexMap.put( "getAvailabilityForInboundChauf2WithOutboundBundles", 125);
        methodIndexMap.put( "getTravelTimeHomeSchool1", 126 );
        methodIndexMap.put( "getTravelTimeHomeSchool2", 127 );
        methodIndexMap.put( "getTravelTimeHomeSchool3", 128 );
        methodIndexMap.put( "getTravelTimeSchool1Home", 129 );
        methodIndexMap.put( "getTravelTimeSchool2Home", 130 );
        methodIndexMap.put( "getTravelTimeSchool3Home", 131 );
        methodIndexMap.put( "getAvailabilityForOutboundChauf1WithInboundBundles", 132);
        methodIndexMap.put( "getAvailabilityForOutboundChauf2WithInboundBundles", 133);

    }

	
	public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
	        case 1:
	        	return escorteePnums[1];
	        case 2:
	        	return escorteePnums[2];
	        case 3:
	        	return escorteePnums[3];
	        case 4:
	        	return chauffeurPnums[1];
	        case 5:
	        	return chauffeurPnums[2];
	        case 6:
	        	return escorteeAge[1];
	        case 7:
	        	return escorteeAge[2];
	        case 8:
	        	return escorteeAge[3];
	        case 9:
	        	return escorteeSchoolLoc[1];
	        case 10:
	        	return escorteeSchoolLoc[2];
	        case 11:
	        	return escorteeSchoolLoc[3];
	        case 12:
	        	return distHomeSchool[1];
	        case 13:
	        	return distHomeSchool[2];
	        case 14:
	        	return distHomeSchool[3];
	        case 15:
	        	return distSchoolHome[1];
	        case 16:
	        	return distSchoolHome[2];
	        case 17:
	        	return distSchoolHome[3];
	        case 18:
	        	return timeHomeSchool[1];
	        case 19:
	        	return timeHomeSchool[2];
	        case 20:
	        	return timeHomeSchool[3];
	        case 21:
	        	return timeSchoolHome[1];
	        case 22:
	        	return timeSchoolHome[2];
	        case 23:
	        	return timeSchoolHome[3];
	        case 24:
	        	return escorteeDepartForSchool[1];
	        case 25:
	        	return escorteeDepartForSchool[2];
	        case 26:
	        	return escorteeDepartForSchool[3];
	        case 27:
	        	return escorteeDepartFromSchool[1];
	        case 28:
	        	return escorteeDepartFromSchool[2];
	        case 29:
	        	return escorteeDepartFromSchool[3];
	        case 30:
	        	return chauffeurGender[1];
	        case 31:
	        	return chauffeurGender[2];
	        case 32:
	        	return chauffeurPersonType[1];
	        case 33:
	        	return chauffeurPersonType[2];
	        case 34:
	        	return chauffeurAge[1];
	        case 35:
	        	return chauffeurAge[2];
	        case 36:
	        	return chauffeurDepartForMandatory[1];
	        case 37:
	        	return chauffeurDepartForMandatory[2];
	        case 38:
	        	return chauffeurDepartFromMandatory[1];
	        case 39:
	        	return chauffeurDepartFromMandatory[2];
	        case 40:
	        	return chauffeurDap[1];
	        case 41:
	        	return chauffeurDap[2];
	        case 42:
	        	return distHomeMandatory[1];
	        case 43:
	        	return distHomeMandatory[2];
	        case 44:
	        	return timeHomeMandatory[1];
	        case 45:
	        	return timeHomeMandatory[2];
	        case 46:
	        	return distMandatoryHome[1];
	        case 47:
	        	return distMandatoryHome[2];
	        case 48:
	        	return timeMandatoryHome[1];
	        case 49:
	        	return timeMandatoryHome[2];
	        case 50:
	        	return distSchoolSchool[1][2];
	        case 51:
	        	return distSchoolSchool[1][3];
	        case 52:
	        	return distSchoolSchool[2][3];
	        case 53:
	        	return distSchoolMandatory[1][1];
	        case 54:
	        	return distSchoolMandatory[1][2];
	        case 55:
	        	return distSchoolMandatory[2][1];
	        case 56:
	        	return distSchoolMandatory[2][2];
	        case 57:
	        	return distSchoolMandatory[3][1];
	        case 58:
	        	return distSchoolMandatory[3][2];
	        case 59:
	        	return distMandatorySchool[1][1];
	        case 60:
	        	return distMandatorySchool[1][2];
	        case 61:
	        	return distMandatorySchool[1][3];
	        case 62:
	        	return distMandatorySchool[2][1];
	        case 63:
	        	return distMandatorySchool[2][2];
	        case 64:
	        	return distMandatorySchool[2][3];
	        case 65:
	        	return hhObj.getIncomeInDollars();
	        case 66:
	        	return hhObj.getAutoOwnershipModelResult();
	        case 67:
	        	return hhObj.getWorkers();
	        case 68:
	        	return numChildrenTravelingToSchool;
	        case 69:
	        	return numPotentialChauffeurs;
	        case 70:
	        	return Math.max( distHomeSchool[1] + distSchoolMandatory[1][1] - distHomeMandatory[1], 0 );
	        case 71:
	        	return Math.max( distHomeSchool[1] + distSchoolMandatory[1][2] - distHomeMandatory[2], 0 );
	        case 72:
	        	return Math.max( distHomeSchool[2] + distSchoolMandatory[2][1] - distHomeMandatory[1], 0 );
	        case 73:
	        	return Math.max( distHomeSchool[2] + distSchoolMandatory[2][2] - distHomeMandatory[2], 0 );
	        case 74:
	        	return Math.max( distHomeSchool[3] + distSchoolMandatory[3][1] - distHomeMandatory[1], 0 );
	        case 75:
	        	return Math.max( distHomeSchool[3] + distSchoolMandatory[3][2] - distHomeMandatory[2], 0 );
	        case 76:
	        	return getAbsoluteDeviationDistanceOutboundChild12Chauffeur1();
	        case 77:
	        	return getAbsoluteDeviationDistanceOutboundChild12Chauffeur2();
	        case 78:
	        	return getAbsoluteDeviationDistanceOutboundChild13Chauffeur1();
	        case 79:
	        	return getAbsoluteDeviationDistanceOutboundChild13Chauffeur2();
	        case 80:
	        	return getAbsoluteDeviationDistanceOutboundChild23Chauffeur1();
	        case 81:
	        	return getAbsoluteDeviationDistanceOutboundChild23Chauffeur2();
	        case 82:
	        	return getAbsoluteDeviationDistanceOutboundChild123Chauffeur1();
	        case 83:
	        	return getAbsoluteDeviationDistanceOutboundChild123Chauffeur2();
	        case 84:
	        	return Math.max( distMandatorySchool[1][1] + distSchoolHome[1] - distMandatoryHome[1], 0 );
	        case 85:
	        	return Math.max( distMandatorySchool[2][1] + distSchoolHome[1] - distMandatoryHome[2], 0 );
	        case 86:
	        	return Math.max( distMandatorySchool[1][2] + distSchoolHome[2] - distMandatoryHome[1], 0 );
	        case 87:
	        	return Math.max( distMandatorySchool[2][2] + distSchoolHome[2] - distMandatoryHome[2], 0 );
	        case 88:
	        	return Math.max( distMandatorySchool[1][3] + distSchoolHome[3] - distMandatoryHome[1], 0 );
	        case 89:
	        	return Math.max( distMandatorySchool[2][3] + distSchoolHome[3] - distMandatoryHome[2], 0 );
	        case 90:
	        	return getAbsoluteDeviationDistanceInboundChild12Chauffeur1();
	        case 91:
	        	return getAbsoluteDeviationDistanceInboundChild12Chauffeur2();
	        case 92:
	        	return getAbsoluteDeviationDistanceInboundChild13Chauffeur1();
	        case 93:
	        	return getAbsoluteDeviationDistanceInboundChild13Chauffeur2();
	        case 94:
	        	return getAbsoluteDeviationDistanceInboundChild23Chauffeur1();
	        case 95:
	        	return getAbsoluteDeviationDistanceInboundChild23Chauffeur2();
	        case 96:
	        	return getAbsoluteDeviationDistanceInboundChild123Chauffeur1();
	        case 97:
	        	return getAbsoluteDeviationDistanceInboundChild123Chauffeur2();
	        case 98:
	            return chosenIbEscortType1;
	        case 99:
	            return chosenIbEscortType2;
	        case 100:
	            return chosenIbEscortType3;
	        case 101:
	            return chosenIbChauf1;
	        case 102:
	            return chosenIbChauf2;
	        case 103:
	            return chosenIbChauf3;
	        case 104:
	            return chosenObEscortType1;
	        case 105:
	            return chosenObEscortType2;
	        case 106:
	            return chosenObEscortType3;
	        case 107:
	            return chosenObChauf1;
	        case 108:
	            return chosenObChauf2;
	        case 109:
	            return chosenObChauf3;
	        case 110:
	            return potentialIbChauf1;
	        case 111:
	            return potentialIbChauf2;
	        case 112:
	            return potentialObChauf1;
	        case 113:
	            return potentialObChauf2;
	        case 114:
	        	return (int)( ( ( distMandatorySchool[1][1] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 115:
	        	return (int)( ( ( distMandatorySchool[2][1] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 116:
	        	return (int)( ( ( distMandatorySchool[1][2] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 117:
	        	return (int)( ( ( distMandatorySchool[2][2] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 118:
	        	return (int)( ( ( distMandatorySchool[1][3] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 119:
	        	return (int)( ( ( distMandatorySchool[2][3] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 120:
	        	return (int)( ( ( distMandatoryHome[1] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 121:
	        	return (int)( ( ( distMandatoryHome[2] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 122:
	        	return getAvailabilityForMultipleBundlesOutbound( arrayIndex );
	        case 123:
	        	return getAvailabilityForMultipleBundlesInbound( arrayIndex );
	        case 124:
	        	return getAvailabilityForInboundChauf1WithOutboundBundles( arrayIndex );
	        case 125:
	        	return getAvailabilityForInboundChauf2WithOutboundBundles( arrayIndex );
	        case 126:
	        	return (int)( ( ( distHomeSchool[1] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 127:
	        	return (int)( ( ( distHomeSchool[2] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 128:
	        	return (int)( ( ( distHomeSchool[3] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 129:
	        	return (int)( ( ( distSchoolHome[1] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 130:
	        	return (int)( ( ( distSchoolHome[2] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 131:
	        	return (int)( ( ( distSchoolHome[3] * MINUTES_PER_MILE ) / ModelStructure.TOD_INTERVAL_IN_MINUTES ) + 0.99999 );
	        case 132:
	        	return getAvailabilityForOutboundChauf1WithInboundBundles( arrayIndex );
	        case 133:
	        	return getAvailabilityForOutboundChauf2WithInboundBundles( arrayIndex );
	            
           default:
               logger.error("method number = " + variableIndex + " not found");
               throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }
    
        
	private float getAbsoluteDeviationDistanceOutboundChild12Chauffeur1() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][2] + distSchoolMandatory[2][1];
		float d2 = distHomeSchool[2] + distSchoolSchool[2][1] + distSchoolMandatory[1][1];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceInboundChild12Chauffeur1() {
		float d1 = distMandatorySchool[1][1] + distSchoolSchool[1][2] + distSchoolHome[2];
		float d2 = distMandatorySchool[1][2] + distSchoolSchool[2][1] + distSchoolHome[1];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceOutboundChild12Chauffeur2() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][2] + distSchoolMandatory[2][2];
		float d2 = distHomeSchool[2] + distSchoolSchool[2][1] + distSchoolMandatory[1][2];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceInboundChild12Chauffeur2() {
		float d1 = distMandatorySchool[2][1] + distSchoolSchool[1][2] + distSchoolHome[2];
		float d2 = distMandatorySchool[2][2] + distSchoolSchool[2][1] + distSchoolHome[1];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceOutboundChild13Chauffeur1() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][3] + distSchoolMandatory[3][1];
		float d2 = distHomeSchool[3] + distSchoolSchool[3][1] + distSchoolMandatory[1][1];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceInboundChild13Chauffeur1() {
		float d1 = distMandatorySchool[1][1] + distSchoolSchool[1][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[1][3] + distSchoolSchool[3][1] + distSchoolHome[1];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceOutboundChild13Chauffeur2() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][3] + distSchoolMandatory[3][2];
		float d2 = distHomeSchool[3] + distSchoolSchool[3][1] - distSchoolMandatory[1][2];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceInboundChild13Chauffeur2() {
		float d1 = distMandatorySchool[2][1] + distSchoolSchool[1][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[2][3] + distSchoolSchool[3][1] + distSchoolHome[1];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceOutboundChild23Chauffeur1() {
		float d1 = distHomeSchool[2] + distSchoolSchool[2][3] + distSchoolMandatory[3][1];
		float d2 = distHomeSchool[3] + distSchoolSchool[3][2] - distSchoolMandatory[2][1];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceInboundChild23Chauffeur1() {
		float d1 = distMandatorySchool[1][2] + distSchoolSchool[2][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[1][3] + distSchoolSchool[3][2] + distSchoolHome[2];
		return Math.min( d1,  d2 ) - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceOutboundChild23Chauffeur2() {
		float d1 = distHomeSchool[2] + distSchoolSchool[2][3] + distSchoolMandatory[3][2];
		float d2 = distHomeSchool[3] + distSchoolSchool[3][2] - distSchoolMandatory[2][2];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceInboundChild23Chauffeur2() {
		float d1 = distMandatorySchool[2][2] + distSchoolSchool[2][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[2][3] + distSchoolSchool[3][2] + distSchoolHome[2];
		return Math.min( d1,  d2 ) - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceOutboundChild123Chauffeur1() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][2] + distSchoolSchool[2][3] + distSchoolMandatory[3][1];
		float d2 = distHomeSchool[1] + distSchoolSchool[1][3] + distSchoolSchool[3][2] + distSchoolMandatory[2][1];
		float d3 = distHomeSchool[2] + distSchoolSchool[2][1] + distSchoolSchool[1][3] + distSchoolMandatory[3][1];
		float d4 = distHomeSchool[2] + distSchoolSchool[2][3] + distSchoolSchool[3][1] + distSchoolMandatory[1][1];
		float d5 = distHomeSchool[3] + distSchoolSchool[3][1] + distSchoolSchool[1][2] + distSchoolMandatory[2][1];
		float d6 = distHomeSchool[3] + distSchoolSchool[3][2] + distSchoolSchool[2][1] + distSchoolMandatory[1][1];
		float d = Math.min( d1,  d2 );
		d = Math.min( d,  d3 );
		d = Math.min( d,  d4 );
		d = Math.min( d,  d5 );
		d = Math.min( d,  d6 );
		return d - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceInboundChild123Chauffeur1() {
		float d1 = distMandatorySchool[1][1] + distSchoolSchool[1][2] + distSchoolSchool[2][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[1][1] + distSchoolSchool[1][3] + distSchoolSchool[3][2] + distSchoolHome[2];
		float d3 = distMandatorySchool[1][2] + distSchoolSchool[2][1] + distSchoolSchool[1][3] + distSchoolHome[3];
		float d4 = distMandatorySchool[1][2] + distSchoolSchool[2][3] + distSchoolSchool[3][1] + distSchoolHome[1];
		float d5 = distMandatorySchool[1][3] + distSchoolSchool[3][1] + distSchoolSchool[1][2] + distSchoolHome[2];
		float d6 = distMandatorySchool[1][3] + distSchoolSchool[3][2] + distSchoolSchool[2][1] + distSchoolHome[1];
		float d = Math.min( d1,  d2 );
		d = Math.min( d,  d3 );
		d = Math.min( d,  d4 );
		d = Math.min( d,  d5 );
		d = Math.min( d,  d6 );
		return d - distHomeMandatory[1];
	}

	private float getAbsoluteDeviationDistanceOutboundChild123Chauffeur2() {
		float d1 = distHomeSchool[1] + distSchoolSchool[1][2] + distSchoolSchool[2][3] + distSchoolMandatory[3][2];
		float d2 = distHomeSchool[1] + distSchoolSchool[1][3] + distSchoolSchool[3][2] + distSchoolMandatory[2][2];
		float d3 = distHomeSchool[2] + distSchoolSchool[2][1] + distSchoolSchool[1][3] + distSchoolMandatory[3][2];
		float d4 = distHomeSchool[2] + distSchoolSchool[2][3] + distSchoolSchool[3][1] + distSchoolMandatory[1][2];
		float d5 = distHomeSchool[3] + distSchoolSchool[3][1] + distSchoolSchool[1][2] + distSchoolMandatory[2][2];
		float d6 = distHomeSchool[3] + distSchoolSchool[3][2] + distSchoolSchool[2][1] + distSchoolMandatory[1][2];
		float d = Math.min( d1,  d2 );
		d = Math.min( d,  d3 );
		d = Math.min( d,  d4 );
		d = Math.min( d,  d5 );
		d = Math.min( d,  d6 );
		return d - distHomeMandatory[2];
	}

	private float getAbsoluteDeviationDistanceInboundChild123Chauffeur2() {
		float d1 = distMandatorySchool[2][1] + distSchoolSchool[1][2] + distSchoolSchool[2][3] + distSchoolHome[3];
		float d2 = distMandatorySchool[2][1] + distSchoolSchool[1][3] + distSchoolSchool[3][2] + distSchoolHome[2];
		float d3 = distMandatorySchool[2][2] + distSchoolSchool[2][1] + distSchoolSchool[1][3] + distSchoolHome[3];
		float d4 = distMandatorySchool[2][2] + distSchoolSchool[2][3] + distSchoolSchool[3][1] + distSchoolHome[1];
		float d5 = distMandatorySchool[2][3] + distSchoolSchool[3][1] + distSchoolSchool[1][2] + distSchoolHome[2];
		float d6 = distMandatorySchool[2][3] + distSchoolSchool[3][2] + distSchoolSchool[2][1] + distSchoolHome[1];
		float d = Math.min( d1,  d2 );
		d = Math.min( d,  d3 );
		d = Math.min( d,  d4 );
		d = Math.min( d,  d5 );
		d = Math.min( d,  d6 );
		return d - distHomeMandatory[2];
	}

	
	/**
	 * This method should only be called for relevant alternatives - those with multiple bundles for a single chauffeur.
	 */
    private int getAvailabilityForMultipleBundlesOutbound( int alt ) {
    	
    	// set availability to 0 if unavailable, or 1 if available
    	int availabilityForMultipleBundles = 1;
    	
		List<SchoolEscortingBundle>[] altChaufBundles = SchoolEscortingBundle.constructAltBundles( alt, altBundleIncidence );
		
		//check the number of bundles 
		int chaufIndex = 0;
		if ( altChaufBundles[1].size() > 1 ) //more than one bundle for the first chauffeur
			chaufIndex = 1;
		else if ( altChaufBundles[2].size() > 1 ) //more than one bundle for the second chauffeur
			chaufIndex = 2;
		else {
			logger.fatal( "UEC method getAvailabilityForMultipleBundlesOutbound( alt=" + alt + " ) was called, but neither chauf has multiple escort bundles." );
			logger.fatal("Size of altChaufBundles[1] = "+altChaufBundles[1].size());
			logger.fatal("Size of altChaufBundles[2] = "+altChaufBundles[2].size());
			throw new RuntimeException( );
		}
			
	
		// set the bundle depart intervals for all bundles and arrive back home intervals for pure escort only
		for ( SchoolEscortingBundle bundleObj : altChaufBundles[chaufIndex] ) {

			int[] sortData = new int[ SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1 ];
			Arrays.fill( sortData, 999999999 );
			int[] children = bundleObj.getChildIds();
			for ( int j=0; j < children.length; j++ )
				sortData[children[j]] = escorteeDepartForSchool[children[j]];
			
			int[] childrenOrder = IndexSort.indexSort( sortData );

			int departHomeInterval = escorteeDepartForSchool[ childrenOrder[0] ];
			bundleObj.setDepartHome( departHomeInterval );
			
			if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
				float roundTripMinutes = getRoundTripMinutesFromHomeThruAllSchoolsToHome( childrenOrder, children.length, DROP_OFF_DURATION );
				float arriveBackHomeMinute = convertIntervalToMinutes( departHomeInterval ) + roundTripMinutes; 
				int arriveHomeInterval = convertMinutesToInterval( arriveBackHomeMinute );
				bundleObj.setArriveHome( arriveHomeInterval );
			}
			
		}
	
		
		int[] chaufBundlesOrder = getChaufBundlesOrderOutbound( altChaufBundles[chaufIndex] );
		
		for( int j=1; j < chaufBundlesOrder.length; j++ ) {
			SchoolEscortingBundle bundleSubsequent = altChaufBundles[chaufIndex].get( chaufBundlesOrder[j] );
			SchoolEscortingBundle bundlePrevious = altChaufBundles[chaufIndex].get( chaufBundlesOrder[j-1] );
			if ( bundleSubsequent.getDepartHome() <= bundlePrevious.getArriveHome() ) {
				availabilityForMultipleBundles = 0;
				break;
			}
		}
			
		return availabilityForMultipleBundles;
    			
    }

    float convertIntervalToMinutes(int interval){
    	
    	float minutes = interval * ModelStructure.TOD_INTERVAL_IN_MINUTES;
    	return minutes;
    }
    
    int convertMinutesToInterval(float minutes){
    	
    	int interval = (int) (minutes/ModelStructure.TOD_INTERVAL_IN_MINUTES);
    	interval = Math.min(interval,ModelStructure.MAX_TOD_INTERVAL);
    	return interval;
    	
    }
    
    
	/**
	 * This method should only be called for relevant alternatives - those with multiple bundles for a single chauffeur.
	 */
    private int getAvailabilityForMultipleBundlesInbound( int alt ) {
    	
    	// set availability to 0 if unavailable, or 1 if available
    	int availabilityForMultipleBundles = 1;
    	
		List<SchoolEscortingBundle>[] altChaufBundles = SchoolEscortingBundle.constructAltBundles( alt, altBundleIncidence );
		
		int chaufIndex = 0;
		if ( altChaufBundles[1].size() > 1 )
			chaufIndex = 1;
		else if ( altChaufBundles[2].size() > 1 )
			chaufIndex = 2;
		else {
			logger.error( "UEC method getAvailabilityForMultipleBundlesInbound( alt=" + alt + " ) was called, but neither chauf has multiple escort bundles." );
			throw new RuntimeException( );
		}
			
	
		// set the bundle arrive intervals for all bundles and depart from home intervals for pure escort only
		for ( SchoolEscortingBundle bundleObj : altChaufBundles[chaufIndex] ) {
			
			int[] sortData = new int[ SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1 ];
			Arrays.fill( sortData, 999999999 );
			int[] children = bundleObj.getChildIds();
			for ( int j=0; j < children.length; j++ )
				sortData[children[j]] = escorteeDepartFromSchool[children[j]];
			
			int[] childrenOrder = IndexSort.indexSort( sortData );
			
			int departFromFirstSchoolInterval = escorteeDepartFromSchool[childrenOrder[0]];
			
			float firstSchoolToHomeMinutes = getMinutesFromFirstSchoolToHome( childrenOrder, children.length, PICK_UP_DURATION );
			float arriveHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) + firstSchoolToHomeMinutes; 
			int arriveHomeInterval = convertMinutesToInterval( arriveHomeMinutes );
			bundleObj.setArriveHome( arriveHomeInterval );
									
			if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
				float homeToFirstSchoolMinutes = getMazToMazTimeInMinutes( hhObj.getHhMgra(), escorteeSchoolLoc[childrenOrder[0]] );
				float departFromHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) - homeToFirstSchoolMinutes; 
				int departHomeInterval = convertMinutesToInterval( departFromHomeMinutes );
				bundleObj.setDepartHome( departHomeInterval );
			}
			
		}
	
		
		int[] chaufBundlesOrder = getChaufBundlesOrderInbound( altChaufBundles[chaufIndex] );
		
		for( int j=1; j < chaufBundlesOrder.length; j++ ) {
			SchoolEscortingBundle bundleSubsequent = altChaufBundles[chaufIndex].get( chaufBundlesOrder[j] );
			SchoolEscortingBundle bundlePrevious = altChaufBundles[chaufIndex].get( chaufBundlesOrder[j-1] );
			if ( bundleSubsequent.getDepartHome() <= bundlePrevious.getArriveHome() ) {
				availabilityForMultipleBundles = 0;
				break;
			}
		}
			
		return availabilityForMultipleBundles;
    			
    }

    private int getAvailabilityForInboundChauf1WithOutboundBundles( int alt ) {
    	return getAvailabilityForChaufWithPreviousDirectionBundles( 1, SchoolEscortingModel.DIR_INBOUND, alt );
    }    
    
    private int getAvailabilityForInboundChauf2WithOutboundBundles( int alt ) {
    	return getAvailabilityForChaufWithPreviousDirectionBundles( 2, SchoolEscortingModel.DIR_INBOUND, alt );
    }    
    
    private int getAvailabilityForOutboundChauf1WithInboundBundles( int alt ) {
    	return getAvailabilityForChaufWithPreviousDirectionBundles( 1, SchoolEscortingModel.DIR_OUTBOUND, alt );
    }    
    
    private int getAvailabilityForOutboundChauf2WithInboundBundles( int alt ) {
    	return getAvailabilityForChaufWithPreviousDirectionBundles( 2, SchoolEscortingModel.DIR_OUTBOUND, alt );
    }    
    
	/**
	 * This method should only be called for for relevant alternatives - those with multiple bundles for a single chauffeur.
	 */
    private int getAvailabilityForChaufWithPreviousDirectionBundles( int chaufid, int dir, int alt ) {
    	
    	// set availability to 0 if unavailable, or 1 if available
    	int availability = 1;
    	
		List<SchoolEscortingBundle>[] altChaufBundles = SchoolEscortingBundle.constructAltBundles( alt, altBundleIncidence );
		
		// set the bundle depart and arrive intervals for all bundles for the alternative
		for ( SchoolEscortingBundle bundleObj : altChaufBundles[chaufid] ) {

			// order the children by earliest pickup time and set arriveHome
			int[] sortData = new int[ SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1 ];
			Arrays.fill( sortData, 999999999 );
			int[] children = bundleObj.getChildIds();
			for ( int j=0; j < children.length; j++ ) {
				sortData[children[j]] = escorteeDepartFromSchool[children[j]];
				if ( dir == SchoolEscortingModel.DIR_OUTBOUND )
					sortData[children[j]] = escorteeDepartForSchool[children[j]];
				else
					sortData[children[j]] = escorteeDepartFromSchool[children[j]];
			}

			int[] childrenOrder = IndexSort.indexSort( sortData );
			
			int chaufPnum = chauffeurPnums[chaufid];
			
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				
				// set OB depart to earliest child's depart from home or either pure escort or ride sharing
				int departHomeInterval = escorteeDepartForSchool[ childrenOrder[0] ];
				bundleObj.setDepartHome( departHomeInterval );

				if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
					
					float roundTripMinutes = getRoundTripMinutesFromHomeThruAllSchoolsToHome( childrenOrder, children.length, DROP_OFF_DURATION );
					float arriveBackHomeMinute = convertIntervalToMinutes( departHomeInterval ) + roundTripMinutes; 
					int arriveHomeInterval = convertMinutesToInterval( arriveBackHomeMinute );
					bundleObj.setArriveHome( arriveHomeInterval );
					
					// neither end of the alternative window can overlap the reserved window
					if ( ( departHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] && departHomeInterval <= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] ) ||
						( arriveHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] && arriveHomeInterval <= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] ) )
							availability = 0;
					// if the start of the alternative window is before the start of the reserved window, the end of the alternative window must also be before the start of the reserved window.
					else if ( departHomeInterval < chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] && arriveHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] )
						availability = 0;
					// the start of the alternative window cannot be after the start of the reserved window
					else if ( departHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] )
						availability = 0;
				}
				else {
					
					if ( chauffeurDap[chaufid] != 1 ) {
						availability = 0;						
					}
					else {

						float numMinutes = getTimeInMinutesFromHomeThruAllSchoolsToWork( chauffeurMandatoryLoc[chaufid], childrenOrder, children.length, DROP_OFF_DURATION );
						float arriveWorkMinute = convertIntervalToMinutes( departHomeInterval ) + numMinutes; 
						int arriveWorkInterval = convertMinutesToInterval( arriveWorkMinute );

						if ( ( departHomeInterval >= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][0] && departHomeInterval <= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][1] ) ||
							( arriveWorkInterval >= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][0] && arriveWorkInterval <= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][1] ) )
								availability = 0;
					}
				}
				
			}
			else {
			
				if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {

					int departFromFirstSchoolInterval = escorteeDepartFromSchool[childrenOrder[0]];
					
					float homeToFirstSchoolMinutes = getMazToMazTimeInMinutes( hhObj.getHhMgra(), escorteeSchoolLoc[childrenOrder[0]] );
					float departFromHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) - homeToFirstSchoolMinutes; 
					int departHomeInterval = convertMinutesToInterval( departFromHomeMinutes );

					float firstSchoolToHomeMinutes = getMinutesFromFirstSchoolToHome( childrenOrder, children.length, PICK_UP_DURATION );
					float arriveHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) + firstSchoolToHomeMinutes; 
					int arriveHomeInterval = convertMinutesToInterval( arriveHomeMinutes );
					
					// neither end of the alternative window can overlap the reserved window
					if ( ( departHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] && departHomeInterval <= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] ) ||
						( arriveHomeInterval >= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][0] && arriveHomeInterval <= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] ) )
							availability = 0;
					// the start of the alternative window cannot be before the end of the reserved window
					else if ( departHomeInterval <= chaufExtents[ModelStructure.PURE_ESCORTING_TYPE][chaufPnum][1] )
						availability = 0;
				}
				else {
					
					if ( chauffeurDap[chaufid] != 1 ) {
						availability = 0;						
					}
					else {

						int departFromFirstSchoolInterval = escorteeDepartFromSchool[childrenOrder[0]];
						
						float workToFirstSchoolMinutes = getMazToMazTimeInMinutes( chauffeurMandatoryLoc[chaufid], escorteeSchoolLoc[childrenOrder[0]] );
						float departWorkMinute = convertIntervalToMinutes( departFromFirstSchoolInterval ) - workToFirstSchoolMinutes; 
						int departWorkInterval = convertMinutesToInterval( departWorkMinute );

						float firstSchoolToHomeMinutes = getMinutesFromFirstSchoolToHome( childrenOrder, children.length, PICK_UP_DURATION );
						float arriveHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) + firstSchoolToHomeMinutes; 
						int arriveHomeInterval = convertMinutesToInterval( arriveHomeMinutes );
						
						if ( ( departWorkInterval >= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][0] && departWorkInterval <= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][1] ) ||
								( arriveHomeInterval >= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][0] && arriveHomeInterval <= chaufExtents[ModelStructure.RIDE_SHARING_TYPE][chaufPnum][1] ) )
									availability = 0;
					}
				}
			
			}

		}
	
		
		return availability;
    			
    }

    
    /**
     * This method creates and returns the chosen bundles given the attributes of the chauffeur and escortees on the bundle and the type of bundle selected (pure escort vs rideshare) 
     * and the direction for the bundle (outbound versus return). 
     * 
     * @param alt          The chosen alternative.
     * @param chaufIndex   The chauffeur to get the bundle for.
     * @param dir          Outbound or inbound.
     * @return             A fully coded bundle for the chauffeur given the chosen alternative.
     */
    public SchoolEscortingBundle[] getChosenBundles( int alt, int chaufIndex, int dir ) {
    	
    	//an arraylist of school escorting bundles, dimensioned by each chauffeur (2)
		List<SchoolEscortingBundle>[] altChaufBundles = SchoolEscortingBundle.constructAltBundles( alt, altBundleIncidence );
		
		// set the bundle depart intervals for all bundles and arrive back home intervals for pure escort only
		for ( SchoolEscortingBundle bundleObj : altChaufBundles[chaufIndex] ) {

			//if the bundle direction is outbound, sort the escortees by departure period, else sort by arrival period
			int[] sortData = new int[ SchoolEscortingModel.NUM_ESCORTEES_PER_HH+1 ];
			Arrays.fill( sortData, 999999999 );
			int[] altBundleChildIds = bundleObj.getChildIds();
			for ( int j=0; j < altBundleChildIds.length; j++ ) {
				if ( dir == SchoolEscortingModel.DIR_OUTBOUND )
					sortData[altBundleChildIds[j]] = escorteeDepartForSchool[altBundleChildIds[j]];
				else
					sortData[altBundleChildIds[j]] = escorteeDepartFromSchool[altBundleChildIds[j]];
			}
			int[] altBundleChildrenOrder = IndexSort.indexSort( sortData );
			
			//set the school locations and person numbers for each escortee in the order of escortees set above
			int[] altBundleChildSchools = new int[altBundleChildIds.length];
			int[] altBundleChildPnums = new int[altBundleChildIds.length];
			for ( int j=0; j < altBundleChildIds.length; j++ ) {
				int k = altBundleChildrenOrder[j];
				altBundleChildSchools[j] = escorteeSchoolLoc[k];
				altBundleChildPnums[j] = escorteePnums[k];
			}
			
			//set other elements of the bundle for this choice\household
			bundleObj.setDir( dir );
			bundleObj.setChaufPnum( chauffeurPnums[chaufIndex] );
			bundleObj.setChaufPid( chauffeurPids[chaufIndex] );
			bundleObj.setChaufPersType( chauffeurPersonType[chaufIndex] );
			bundleObj.setChildPnums( altBundleChildPnums );
			bundleObj.setSchoolMazs( altBundleChildSchools );
			bundleObj.setWorkOrSchoolMaz( chauffeurMandatoryLoc[chaufIndex] );
			
			
            //if the bundle is outbound
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND ) {
				
				//get an array of distances to each child's school starting from home.
				float[] altBundleSchoolDistances = getDistancesToSchools( mgraDataManager.getTaz( hhObj.getHhMgra() ), altBundleChildrenOrder, altBundleChildIds.length );
				bundleObj.setSchoolDists( altBundleSchoolDistances );

				// set OB depart to earliest child's depart from home for either pure escort or ride sharing
				int departHomeInterval = escorteeDepartForSchool[ altBundleChildrenOrder[0] ];
				bundleObj.setDepartHome( departHomeInterval );

				//if the bundle is pure escort, then the total trip time in minutes is from home through all passengers back to home and the arrival time back at home is the departure
				//period plus the time and some time for stops. Otherwise the time arriving to work is the time period departing home + travel time through all escortees plus dwell.
				if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {

					float roundTripMinutes = getRoundTripMinutesFromHomeThruAllSchoolsToHome( altBundleChildrenOrder, altBundleChildIds.length, DROP_OFF_DURATION );
					float arriveBackHomeMinute = convertIntervalToMinutes( departHomeInterval ) + roundTripMinutes; 
					int arriveHomeInterval = convertMinutesToInterval( arriveBackHomeMinute );
					bundleObj.setArriveHome( arriveHomeInterval );
				}
				else {
					float numMinutes = getTimeInMinutesFromHomeThruAllSchoolsToWork( chauffeurMandatoryLoc[chaufIndex], altBundleChildrenOrder, altBundleChildIds.length, DROP_OFF_DURATION );
					float arriveWorkMinute = convertIntervalToMinutes( departHomeInterval ) + numMinutes; 
					int arriveWorkInterval = convertMinutesToInterval( arriveWorkMinute );
					bundleObj.setArriveWork( arriveWorkInterval );
				}
				
			}
			else {
			
				//on the return direction, the departure time from the first school is the time for the first child being escorted.
				int departFromFirstSchoolInterval = escorteeDepartFromSchool[altBundleChildrenOrder[0]];
				
				// if the bundle is a pure escort, set the departure time period from home to the time that the first child departs from school minus the time required to get to school.
				if ( bundleObj.getEscortType() == ModelStructure.PURE_ESCORTING_TYPE ) {
					float homeToFirstSchoolMinutes = getMazToMazTimeInMinutes( hhObj.getHhMgra(), escorteeSchoolLoc[altBundleChildrenOrder[0]] );
					float departFromHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) - homeToFirstSchoolMinutes;
					int departHomeInterval = convertMinutesToInterval( departFromHomeMinutes );
					bundleObj.setDepartHome( departHomeInterval );

					float[] altBundleSchoolDistances = getDistancesToSchools( mgraDataManager.getTaz( hhObj.getHhMgra() ), altBundleChildrenOrder, altBundleChildIds.length );
					bundleObj.setSchoolDists( altBundleSchoolDistances );
				}
				else { //else if the bundle is rideshare, the time departing work is the time the first child leave school minus the time required to get to school from work.
					float workToFirstSchoolMinutes = getMazToMazTimeInMinutes( chauffeurMandatoryLoc[chaufIndex], escorteeSchoolLoc[altBundleChildrenOrder[0]] );
					float departWorkMinute = convertIntervalToMinutes( departFromFirstSchoolInterval ) - workToFirstSchoolMinutes;
					int departWorkInterval = convertMinutesToInterval( departWorkMinute );
					bundleObj.setDepartWork( departWorkInterval );

					float[] altBundleSchoolDistances = getDistancesToSchools( mgraDataManager.getTaz( chauffeurMandatoryLoc[chaufIndex] ), altBundleChildrenOrder, altBundleChildIds.length );
					bundleObj.setSchoolDists( altBundleSchoolDistances );
				}

				//on the return direction, the arrival time back home is the time from the first child's departure time plus the time required to get home plus dwell time for each escortee.
				float firstSchoolToHomeMinutes = getMinutesFromFirstSchoolToHome( altBundleChildrenOrder, altBundleChildIds.length, PICK_UP_DURATION );
				float arriveHomeMinutes = convertIntervalToMinutes( departFromFirstSchoolInterval ) + firstSchoolToHomeMinutes;
				int arriveHomeInterval = convertMinutesToInterval( arriveHomeMinutes );
				bundleObj.setArriveHome( arriveHomeInterval );

				bundleObj.setDepartPrimaryInterval( departFromFirstSchoolInterval );
			}
			
		}
	
		
		int[] chaufBundlesOrder = null;
		if ( altChaufBundles[chaufIndex].size() > 1 ) {
			if ( dir == SchoolEscortingModel.DIR_OUTBOUND )
				chaufBundlesOrder = getChaufBundlesOrderOutbound( altChaufBundles[chaufIndex] );
			else
				chaufBundlesOrder = getChaufBundlesOrderInbound( altChaufBundles[chaufIndex] );
		}
		else {
			chaufBundlesOrder = new int[]{ 0 };
		}
		
		SchoolEscortingBundle[] result = new SchoolEscortingBundle[chaufBundlesOrder.length];
		for( int j=0; j < chaufBundlesOrder.length; j++ )
			result[j] = altChaufBundles[chaufIndex].get( chaufBundlesOrder[j] );
			
		
		return result;
    			
    }


    /**
     * Get an array of distances to each child's school. 
     * 
     * @param origTaz  The originTaz is the origin of the first child's trip (home for outbound direction, primary destination for return direction)
     * @param childrenOrder The order of each escortee, can be by departure time
     * @param numChildren Number of children
     * @return A float array of distances to each child's school.
     */
    private float[] getDistancesToSchools( int origTaz, int[] childrenOrder, int numChildren ) {

    	float[] distances = new float[numChildren];

		for ( int j=0; j < numChildren; j++ ) {
			int k = childrenOrder[j];
			int schoolTaz = mgraDataManager.getTaz( escorteeSchoolLoc[k] );
			distances[j] = (float) distanceArray[origTaz][schoolTaz];
			origTaz = schoolTaz;
		}
		
		return distances;
    	
    }
    
	
    private float getMinutesFromFirstSchoolToHome( int[] childrenOrder, int numChildren, float minDuration ) {

		int homeTaz = mgraDataManager.getTaz( hhObj.getHhMgra() );
		int firstSchool = escorteeSchoolLoc[ childrenOrder[0] ];
		int firstSchoolTaz = mgraDataManager.getTaz( firstSchool );
		int originTaz = firstSchoolTaz;
		
		// distance is the cumulative distance from the school where the child is picked up to home, through other schools .
		float distance = 0;
		float duration = 0;
		
		for ( int j=1; j < numChildren; j++ ) {
			int k = childrenOrder[j];
			int schoolTaz = mgraDataManager.getTaz( escorteeSchoolLoc[k] );
			distance += (float) distanceArray[originTaz][ schoolTaz ];
			originTaz = schoolTaz;
			
			duration += minDuration;
		}
		
		distance += (float) distanceArray[originTaz][ homeTaz ];
		
		float timeInMinutes = distance * MINUTES_PER_MILE + duration;
		
		return timeInMinutes;
    	
    }
    
    
    private float getMazToMazTimeInMinutes( int fromMaz, int toMaz ) {

		int fromTaz = mgraDataManager.getTaz( fromMaz );
		int toTaz = mgraDataManager.getTaz( toMaz );
		
		float timeInMinutes = MINUTES_PER_MILE * (float) distanceArray[fromTaz][ toTaz];
		return timeInMinutes;
    	
    }
    
	
    private float getRoundTripMinutesFromHomeThruAllSchoolsToHome( int[] childrenOrder, int numChildren, float minDuration ) {

		int originTaz = mgraDataManager.getTaz( hhObj.getHhMgra() );
		
		// cumulative distance and duration
		float distance = 0;
		float duration = 0;

		for ( int j=0; j < numChildren; j++ ) {
			int k = childrenOrder[j];
			int schoolTaz = mgraDataManager.getTaz( escorteeSchoolLoc[k] );
			distance += (float) distanceArray[originTaz][  schoolTaz];
			originTaz =  schoolTaz ;
			
			duration += minDuration;
		}
		int destTaz = mgraDataManager.getTaz( hhObj.getHhMgra() );
		distance += (float) distanceArray[originTaz][  destTaz];
		
		float timeInMinutes = MINUTES_PER_MILE * distance + duration;
		return timeInMinutes;
    	
    }
    
    
    private float getTimeInMinutesFromHomeThruAllSchoolsToWork( int workMaz, int[] childrenOrder, int numChildren, float minDuration ) {

		int homeTaz = mgraDataManager.getTaz( hhObj.getHhMgra() );
		int workTaz = mgraDataManager.getTaz( workMaz );

		int originTaz = homeTaz;
		
		float distance = 0;
		float duration = 0;

		for ( int j=0; j < numChildren; j++ ) {
			int k = childrenOrder[j];
			int schoolTaz = mgraDataManager.getTaz( escorteeSchoolLoc[k] );
			distance += distanceArray[originTaz][ schoolTaz ];
			originTaz = schoolTaz;
			duration += minDuration;
		}
		
		distance += (float) distanceArray[originTaz][ workTaz ];
		
		float timeInMinutes = MINUTES_PER_MILE * distance + duration;
		return timeInMinutes;
    	
    }
    
	
	// Create an array to hold the list indices for the order in which escort activities should be performed.
    // This method only gets called while checking availability for a chauffeur to have multiple bundles, so the
    // list altChaufBundles has either 2 or 3 escort activities.
	private int[] getChaufBundlesOrderOutbound( List<SchoolEscortingBundle> altChaufBundles ) {
		
		int[] chaufBundlesOrder = null;

		// number of escort activities is 2.
		if ( altChaufBundles.size() == 2 ) {
			// [RS,PE]: if the first activity for the alternative is ride sharing, the second must be pure escort, and should be ordered before the ride sharing escort activity;
			if ( altChaufBundles.get( 0 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
				chaufBundlesOrder = new int[]{ 1, 0 };
			// [PE,RS]: likewise if the second activity is ride sharing - the first must be pure escort, and must be ordered first
			else if ( altChaufBundles.get( 1 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
				chaufBundlesOrder = new int[]{ 0, 1 };
			// [PE,PE]: otherwise, both activities are pure escort, and the depart times will determine the order
			else if ( altChaufBundles.get( 1 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
				chaufBundlesOrder = new int[]{ 1, 0 };
			else
				chaufBundlesOrder = new int[]{ 0, 1 };
		}
		// number of escort activities is 3.
		else {
			// [RS,PE,PE]: if the first activity for the alternative is ride sharing, the second and third must also be pure escort, and should be ordered before the ride sharing escort activity;
			if ( altChaufBundles.get( 0 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 2 ).getDepartHome() < altChaufBundles.get( 1 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 2, 1, 0 };
				else
					chaufBundlesOrder = new int[]{ 1, 2, 0 };
			}
			// [PE,RS,PE]: likewise if the second activity is ride sharing - the first and third must be pure escort, and must be ordered before ride sharing
			else if ( altChaufBundles.get( 1 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 2 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 2, 0, 1 };
				else
					chaufBundlesOrder = new int[]{ 0, 2, 1 };
			}
			// [PE,PE,RS]: likewise if the third activity is ride sharing - the first and second must be pure escort, and must be ordered before ride sharing
			else if ( altChaufBundles.get( 2 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 1 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 1, 0, 2 };
				else
					chaufBundlesOrder = new int[]{ 0, 1, 2 };
			}
			// [PE,PE,PE]: otherwise, all three activities are pure escort, and the depart times will determine the order
			else {
				int[] sortData = new int[]{ altChaufBundles.get( 0 ).getDepartHome(), altChaufBundles.get( 1 ).getDepartHome(), altChaufBundles.get( 2 ).getDepartHome() };
				chaufBundlesOrder = IndexSort.indexSort( sortData );
			}
		}
		
		return chaufBundlesOrder;
		
	}
	

	// Create an array to hold the list indices for the order in which escort activities should be performed.
    // This method only gets called while checking availability for a chauffeur to have multiple bundles, so the
    // list altChaufBundles has either 2 or 3 escort activities.
	private int[] getChaufBundlesOrderInbound( List<SchoolEscortingBundle> altChaufBundles ) {
		
		int[] chaufBundlesOrder = null;

		// number of escort activities is 2.
		if ( altChaufBundles.size() == 2 ) {
			// [RS,PE]: if the first activity for the alternative is ride sharing, the second must be pure escort, and should be ordered after the ride sharing escort activity;
			if ( altChaufBundles.get( 0 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
				chaufBundlesOrder = new int[]{ 0, 1 };
			// [PE,RS]: likewise if the second activity is ride sharing - the first must be pure escort, and must be ordered second
			else if ( altChaufBundles.get( 1 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE )
				chaufBundlesOrder = new int[]{ 1, 0 };
			// [PE,PE]: otherwise, both activities are pure escort, and the depart times will determine the order
			else if ( altChaufBundles.get( 1 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
				chaufBundlesOrder = new int[]{ 1, 0 };
			else
				chaufBundlesOrder = new int[]{ 0, 1 };
		}
		// number of escort activities is 3.
		else {
			// [RS,PE,PE]: if the first activity for the alternative is ride sharing, the second and third must also be pure escort, and should be ordered after the ride sharing escort activity;
			if ( altChaufBundles.get( 0 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 2 ).getDepartHome() < altChaufBundles.get( 1 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 0, 2, 1 };
				else
					chaufBundlesOrder = new int[]{ 0, 1, 2 };
			}
			// [PE,RS,PE]: likewise if the second activity is ride sharing - the first and third must be pure escort, and must be ordered after ride sharing
			else if ( altChaufBundles.get( 1 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 2 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 1, 2, 0 };
				else
					chaufBundlesOrder = new int[]{ 1, 0, 2 };
			}
			// [PE,PE,RS]: likewise if the third activity is ride sharing - the first and second must be pure escort, and must be ordered after ride sharing
			else if ( altChaufBundles.get( 2 ).getEscortType() == ModelStructure.RIDE_SHARING_TYPE ) {
				if ( altChaufBundles.get( 1 ).getDepartHome() < altChaufBundles.get( 0 ).getDepartHome() )
					chaufBundlesOrder = new int[]{ 2, 1, 0 };
				else
					chaufBundlesOrder = new int[]{ 2, 0, 1 };
			}
			// [PE,PE,PE]: otherwise, all three activities are pure escort, and the depart times will determine the order
			else {
				int[] sortData = new int[]{ altChaufBundles.get( 0 ).getDepartHome(), altChaufBundles.get( 1 ).getDepartHome(), altChaufBundles.get( 2 ).getDepartHome() };
				chaufBundlesOrder = IndexSort.indexSort( sortData );
			}
		}
		
		return chaufBundlesOrder;
		
	}
	

	
	
	
    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    
    	
}
