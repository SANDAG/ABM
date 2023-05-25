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

package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;


public class SchoolEscortingBundle implements Serializable {

	private static final long serialVersionUID = 1L;

	private int id;
	private int dir;
	private final int alt;
	private final int bundle;
	private final int type;
	private final int chaufId;
	private int chaufPnum;
	private int chaufPersType;
	private int chaufPid;
	private int[] childIds;
	private int[] childPnums;
	private int[] schoolMazs;
	private float[] schoolDists;
	private int workOrSchoolMaz;
	private int departHome;
	private int arriveWork;
	private int departWork;
	private int arriveHome;
	private int departPrimaryInterval = -1;

	private SchoolEscortingBundle( int alt, int bundle, int chaufId, int type, int[] childIds, int[] childPnums ) {
		this.alt = alt;
		this.bundle = bundle;
		this.chaufId = chaufId;
		this.type = type;
		this.childIds = childIds;
		this.childPnums = childPnums;
	}
	

	/**
	 * Get an Arraylist of SchoolEscortingBundles, dimensioned by:
	 *   0: max chauffeurs (2) 
	 *   1: max bundles (3)
	 *   
	 * @param alt The alternative number
	 * @param altBundleIncidence
	 * @return An array of SchoolEscortingBundles (dimensioned by 2, for each chauffeur)
	 */
	public static List<SchoolEscortingBundle>[] constructAltBundles( int alt, int[][] altBundleIncidence ) {

		//first dimension of results array is dimensioned by number of chauffeurs + 1
		List<SchoolEscortingBundle>[] results = new List[ SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH+1 ];
		
		//for each potential bundle (3)
		for ( int i=1; i <= SchoolEscortingModel.NUM_BUNDLES; i++ ) {
			 
			//for each potential chauffeur (2)
			for ( int j=1; j <= SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH; j++ ) {
				 
				// for each escort type (rideshare vs pure escort)
				for ( int k=1; k <= SchoolEscortingModel.NUM_ESCORT_TYPES; k++ ) {
					 
					//if an arraylist hasn't been created for this chauffeur, create one.
					if ( results[j] == null )
						results[j] = new ArrayList<SchoolEscortingBundle>(SchoolEscortingModel.NUM_BUNDLES);
					
					//childIdList initial capacity is max escortees (3); for each potential escortee
					List<Integer> childIdList = new ArrayList<Integer>(SchoolEscortingModel.NUM_ESCORTEES_PER_HH);
					for ( int l=1; l <= SchoolEscortingModel.NUM_ESCORTEES_PER_HH; l++ ) {
						int columnIndex = (i-1) * (SchoolEscortingModel.NUM_CHAUFFEURS_PER_HH * SchoolEscortingModel.NUM_ESCORT_TYPES * SchoolEscortingModel.NUM_ESCORTEES_PER_HH)
										+ (j-1) * (SchoolEscortingModel.NUM_ESCORT_TYPES * SchoolEscortingModel.NUM_ESCORTEES_PER_HH)
										+ (k-1) * (SchoolEscortingModel.NUM_ESCORTEES_PER_HH)
										+ (l-1) + 1;
						//if child number l belongs to this bundle\chauffeur\escort type combination, add l to the childIdList
						if ( altBundleIncidence[alt][columnIndex] > 0 )
							childIdList.add( l );
					}

					//if children are in this bundle\chauffeur\escort type combination
					if ( childIdList.size() > 0 ) {
						int[] childIds = new int[childIdList.size()];
						int[] childPnums = new int[childIdList.size()];
						for ( int l=0; l < childIdList.size(); l++ ) {
							childIds[l] = childIdList.get( l );
						}
						//add a new bundle to the chauffeur element. The bundle contains the number of the bundle, the number of the chauffeur, the
						//escort type (rideshare versus pure), the child ids (1 through 3) and an empty array of person numbers for each child.
						results[j].add( new SchoolEscortingBundle( alt, i, j, k, childIds, childPnums ) );
					}
					
				}

			}

		}
		 
		return results;
		
	}

	public void setId( int id ) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setDir( int dir ) {
		this.dir = dir;
	}

	public int getDir() {
		return dir;
	}

	public int getAlt() {
		return alt;
	}

	public int getBundle() {
		return bundle;
	}

	public int getChaufId() {
		return chaufId;
	}

	public void setChaufPnum( int pnum ) {
		chaufPnum = pnum;
	}

	public int getChaufPnum() {
		return chaufPnum;
	}

	public void setChaufPersType( int ptype ) {
		chaufPersType = ptype;
	}

	public int getChaufPersType() {
		return chaufPersType;
	}

	public void setChaufPid( int pid ) {
		chaufPid = pid;
	}

	public int getChaufPid() {
		return chaufPid;
	}

	public int getEscortType() {
		return type;
	}

	public void setSchoolMazs( int[] schoolMazs ) {
		this.schoolMazs = schoolMazs;
	}
	
	public int[] getSchoolMazs() {
		return schoolMazs;
	}
	
	public void setSchoolDists( float[] schoolDists ) {
		this.schoolDists = schoolDists;
	}
	
	public float[] getSchoolDists() {
		return schoolDists;
	}
	
	public void setChildIds( int[] childIds ) {
		this.childIds = childIds;
	}
	
	public int[] getChildIds() {
		return childIds;
	}
	
	public void setChildPnums( int[] childPnums ) {
		this.childPnums = childPnums;
	}
	
	public int[] getChildPnums() {
		return childPnums;
	}
	
	public void setDepartHome( int depart ) {
		departHome = depart;
	}

	public int getDepartHome() {
		return departHome;
	}

	/*
	public void setArriveHome( int arrive ) {
		arriveHome = Math.min( arrive, TourTodDmu.NUM_TOD_INTERVALS );
	}
*/
	/**
	 * Arrive home; modified JEF to remove taking the minimum of arrive and number of TOD intervals.
	 * @param arrive
	 */
	public void setArriveHome( int arrive ) {
		arriveHome = arrive;
	}

	
	public int getArriveHome() {
		return arriveHome;
	}

	public void setDepartWork( int depart ) {
		departWork = depart;
	}

	public int getDepartWork() {
		return departWork;
	}

	public void setArriveWork( int arrive ) {
		arriveWork = arrive;
	}

	public int getArriveWork() {
		return arriveWork;
	}

	public void setWorkOrSchoolMaz( int maz ) {
		workOrSchoolMaz = maz;
	}
	
	public int getWorkOrSchoolMaz() {
		return workOrSchoolMaz;
	}
	
	public void setDepartPrimaryInterval( int interval ) {
		departPrimaryInterval = interval;
	}
	
	public int getDepartPrimaryInterval() {
		return departPrimaryInterval;
	}
	
	
	public String toString() {
		
		String childIdString = "[";
		String childPnumString = "[";
		String schoolString = "[";
		String distsString = "[";
		if ( childIds.length > 0 ) {
			childIdString += childIds[0];
			childPnumString += childPnums[0];
			schoolString += schoolMazs[0];
			distsString += String.format( "%.5f", schoolDists[0] );
			for ( int i=1; i < childIds.length; i++ ) {
				childIdString += "," + childIds[i];
				childPnumString += "," + childPnums[i];
				schoolString += "," + schoolMazs[i];
				distsString += "," + String.format( "%.5f", schoolDists[i] );
			}
		}
		childIdString += "]";
		childPnumString += "]";
		schoolString += "]";
		distsString += "]";
		
		String outputString =
			"\tid = " + id + "\n" +
			"\tdir = " + (dir == SchoolEscortingModel.DIR_OUTBOUND ? "outbound" : "inbound" ) + "\n" +
			"\talt = " + alt + "\n" +
			"\tbundle = " + bundle + "\n" +
			"\tchaufPnum = " + chaufPnum + "\n" +
			"\tchaufPid = " + chaufPid + "\n" +
			"\tchaufPtype = " + chaufPersType + "\n" +
			"\tescort type = " + (type == ModelStructure.RIDE_SHARING_TYPE ? "ride sharing" : "pure escort" ) + "\n" +
			"\tchildIds = " + childIdString + "\n" +
			"\tchildPnums = " + childPnumString + "\n" +
			"\tschoolMazs = " + schoolString + "\n" +
			"\tschoolDists = " + distsString + "\n" +
			"\tdepartHome = " + departHome + "\n" +
			"\tarriveHome = " + arriveHome + "\n" +
			"\tdepartWork = " + departWork + "\n" +
			"\tarriveWork = " + arriveWork + "\n\n";
		
		return outputString;
		
	}
		
	public static String getExportHeaderString() {
	    String header = "id,dir,alt,bundle,type,chaufId,chaufPnum,chaufPersType,chaufPid,departHome,arriveHome,departWork,arriveWork,childIds,childPnums,schoolMazs,schoolDists";            
	    return header;
	}
	
	public String getExportString() {

		String childIdString = "[";
		String childPnumString = "[";
		String schoolString = "[";
		String distsString = "[";
		if ( childIds.length > 0 ) {
			childIdString += childIds[0];
			childPnumString += childPnums[0];
			schoolString += schoolMazs[0];
			distsString += String.format( "%.5f", schoolDists[0] );
			for ( int i=1; i < childIds.length; i++ ) {
				childIdString += "," + childIds[i];
				childPnumString += "," + childPnums[i];
				schoolString += "," + schoolMazs[i];
				distsString += "," + String.format( "%.5f", schoolDists[i] );
			}
		}
		childIdString += "]";
		childPnumString += "]";
		schoolString += "]";
		distsString += "]";
		
		String outputString =
			id + "," +
			dir + "," +
			alt + "," +
			bundle + "," +
			type + "," +
			chaufId	+ "," +
			chaufPnum + "," +
			chaufPersType + "," +
			chaufPid + "," +
			departHome + "," +
			arriveHome + "," +
			departWork + "," +
			arriveWork + "," +
			childIdString + "," +
			childPnumString + "," +
			schoolString + "," +
			distsString;

		return outputString;
		
	}
	
	public void logBundle(Logger logger){
		
		String childIdString = "[";
		String childPnumString = "[";
		String schoolString = "[";
		String distsString = "[";
		if ( childIds.length > 0 ) {
			childIdString += childIds[0];
			childPnumString += childPnums[0];
			schoolString += schoolMazs[0];
			distsString += String.format( "%.5f", schoolDists[0] );
			for ( int i=1; i < childIds.length; i++ ) {
				childIdString += "," + childIds[i];
				childPnumString += "," + childPnums[i];
				schoolString += "," + schoolMazs[i];
				distsString += "," + String.format( "%.5f", schoolDists[i] );
			}
		}
		childIdString += "]";
		childPnumString += "]";
		schoolString += "]";
		distsString += "]";
		logger.info("***********************************************");
		logger.info("id = " + id);
		logger.info("dir = " + (dir == SchoolEscortingModel.DIR_OUTBOUND ? "outbound" : "inbound" ) );
		logger.info("alt = " + alt  );
		logger.info("bundle = " + bundle );
		logger.info("chaufPnum = " + chaufPnum  );
		logger.info("chaufPid = " + chaufPid  );
		logger.info("chaufPtype = " + chaufPersType  );
		logger.info("escort type = " + (type == ModelStructure.RIDE_SHARING_TYPE ? "ride sharing" : "pure escort" )  );
		logger.info("childIds = " + childIdString  );
		logger.info("childPnums = " + childPnumString  );
		logger.info("schoolMazs = " + schoolString  );
		logger.info("schoolDists = " + distsString  );
		logger.info("departHome = " + departHome  );
		logger.info("arriveHome = " + arriveHome  );
		logger.info("departWork = " + departWork  );
		logger.info("arriveWork = " + arriveWork  );
		logger.info("***********************************************");	
		
		
	}
	
/*
	public static SchoolEscortingBundle restoreSchoolEscortingBundleFromExportString( String exportString ) throws Exception {
		
        StringTokenizer st = new StringTokenizer( exportString, "," );
        
       	String stringValue = st.nextToken().trim();
       	int idValue = Integer.parseInt( stringValue );

       	stringValue = st.nextToken().trim();
       	int dirValue = Integer.parseInt( stringValue );
       	
       	stringValue = st.nextToken().trim();
       	int altValue = Integer.parseInt( stringValue );
       	
       	stringValue = st.nextToken().trim();
       	int bundleValue = Integer.parseInt( stringValue );
       	
       	stringValue = st.nextToken().trim();
       	int typeValue = Integer.parseInt( stringValue );
       	
       	stringValue = st.nextToken().trim();
       	int chaufIdValue = Integer.parseInt( stringValue );
       	
      	stringValue = st.nextToken().trim();
       	int chaufPnumValue = Integer.parseInt( stringValue );

      	stringValue = st.nextToken().trim();
       	int chaufPtypeValue = Integer.parseInt( stringValue );

      	stringValue = st.nextToken().trim();
       	int chaufPidValue = Integer.parseInt( stringValue );

       	stringValue = st.nextToken().trim();
       	int departHomeValue = Integer.parseInt( stringValue );

       	stringValue = st.nextToken().trim();
       	int arriveHomeValue = Integer.parseInt( stringValue );

       	stringValue = st.nextToken().trim();
       	int departWorkValue = Integer.parseInt( stringValue );

       	stringValue = st.nextToken().trim();
       	int arriveWorkValue = Integer.parseInt( stringValue );

       	int startCharIndex = exportString.indexOf("[") + 1;
       	int endCharIndex = exportString.indexOf("]");
       	String valuesOnlyString = exportString.substring( startCharIndex, endCharIndex );
       	int[] childIdValues = Parsing.getOneDimensionalIntArrayValuesFromExportString( valuesOnlyString );
       	Integer.par
       	startCharIndex = exportString.indexOf("[", endCharIndex) + 1;
       	endCharIndex = exportString.indexOf("]", startCharIndex);
       	valuesOnlyString = exportString.substring( startCharIndex, endCharIndex );
       	int[] childPnumValues = Parsing.getOneDimensionalIntArrayValuesFromExportString( valuesOnlyString );
       	
       	startCharIndex = exportString.indexOf("[", endCharIndex) + 1;
       	endCharIndex = exportString.indexOf("]", startCharIndex);
       	valuesOnlyString = exportString.substring( startCharIndex, endCharIndex );
       	int[] schoolMazsValues = Parsing.getOneDimensionalIntArrayValuesFromExportString( valuesOnlyString );

       	startCharIndex = exportString.indexOf("[", endCharIndex) + 1;
       	endCharIndex = exportString.indexOf("]", startCharIndex);
       	valuesOnlyString = exportString.substring( startCharIndex, endCharIndex );
       	float[] schoolDistValues = Parsing.getOneDimensionalFloatArrayValuesFromExportString( valuesOnlyString );


       	SchoolEscortingBundle newBundle = new SchoolEscortingBundle( altValue, bundleValue, chaufIdValue, typeValue, childIdValues, childPnumValues );
       	newBundle.setId( idValue );
       	newBundle.setDir( dirValue );
       	newBundle.setChaufPnum( chaufPnumValue );
       	newBundle.setChaufPersType( chaufPtypeValue );
       	newBundle.setChaufPid( chaufPidValue );
       	newBundle.setSchoolMazs( schoolMazsValues );
       	newBundle.setSchoolDists( schoolDistValues );
       	newBundle.setDepartHome( departHomeValue );
       	newBundle.setArriveHome( arriveHomeValue );
       	newBundle.setDepartWork( departWorkValue );
       	newBundle.setArriveWork( arriveWorkValue );

   		return newBundle;
   		
	}
	*/
}
