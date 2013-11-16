package org.sandag.abm.ctramp;

/**
 * The {@code BikeLogsumSegment} class provides a segmentation for bicycle logsums used in the SANDAG model. The segmentation is currently
 * based on three variables:
 * <p>
 * <ul>
 *     <li>sex (male/female)</li>
 *     <li>tour type (mandatory/non-mandatory)</li>
 *     <li>trip direction (outbound/inbound) - only used if tour type is mandatory</li>
 * </ul>
 * <p>
 * This segmentation maps the 6 possible unique combinations into the integer indices 0 through 5, which can then be used as a lookup to a
 * zero-based array or list data structure. 
 */
public class BikeLogsumSegment {
	private final int segmentId;
	
	/**
	 * Constructor specifying the segment parameters.
	 * 
	 * @param isFemale
	 *        {@code true} if the sex is female.
	 *        
	 * @param mandatory
	 *        {@code true} if the tour type is mandatory.
	 *        
	 * @param inbound
	 *        {@code true} if the trip direction is inbound.
	 */
	public BikeLogsumSegment(boolean isFemale, boolean mandatory, boolean inbound) {
		segmentId = formSegmentId(isFemale,mandatory,inbound);
	}
	
	private int formSegmentId(boolean isFemale, boolean mandatory, boolean inbound) {
		//powers of 2 make a unique identifier
		return (isFemale ? 1 : 0) + 
			   (mandatory ? 2 : 0) + 
			   ((mandatory && inbound) ? 2 : 0); //this last one is 2 not 4 because mandatory is a requirement
	}
	
	/**
	 * Get the segment index (0-5) for this segment.
	 * 
	 * @return return this segment's index id.
	 */
	public int getSegmentId() {
		return segmentId;
	}

	/**
	 * Get the total number of segments available from this class. All segment index ids will be less than this value.
	 * 
	 * @return the number of unique segments provided by this class.
	 */
	public static int segmentWidth() {
		return 6;
	}
	
	/**
	 * Get the segments corresponding to the tour-specific segment values. This will give all permutations which can then be combined to form a
	 * tour-level (as opposed to trip-level) composite logsum.
	 * 
	 * @param isFemale
	 *        {@code true} if the sex is female.
	 *        
	 * @param mandatory
	 *        {@code true} if the tour type is mandatory.
	 *        
	 * @return an array of all possible segments with {@code isFemale} and {@code mandatory}.
	 */
	public static BikeLogsumSegment[] getTourSegments(boolean isFemale, boolean mandatory) {
		return new BikeLogsumSegment[] {new BikeLogsumSegment(isFemale,mandatory,true),
										new BikeLogsumSegment(isFemale,mandatory,false)};
	}
	
	/**
	 * Get the segments corresponding to the tour-specific, person-inspecific segment values. This will give all permutations which can then be 
	 * combined to form a tour- and household-level (as opposed to trip-level) composite logsum.
	 *        
	 * @param mandatory
	 *        {@code true} if the tour type is mandatory.
	 *        
	 * @return an array of all possible segments with {@code mandatory}.
	 */
	public static BikeLogsumSegment[] getTourSegments(boolean mandatory) {
		return new BikeLogsumSegment[] {new BikeLogsumSegment(true,mandatory,true),
										new BikeLogsumSegment(true,mandatory,false),
										new BikeLogsumSegment(false,mandatory,true),
										new BikeLogsumSegment(false,mandatory,false)};
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("<BikeLogsumSegment: ");
		boolean isFemale = (segmentId & 1) > 0;
		boolean isMandatory = (segmentId & 2) > 0;
		boolean inbound = (segmentId & 4) > 0;
		sb.append(isFemale ? "female" : "male");
		sb.append(",").append(isMandatory ? "mandatory" : "non-mandatory");
		if (isMandatory)
			sb.append(",").append(inbound ? "inbound" : "outbound");
		sb.append(">");
		return sb.toString();
	}
	
}
