package org.sandag.abm.ctramp;

public class BikeLogsumSegment {
	private final int segmentId;
	
	public BikeLogsumSegment(boolean isFemale, boolean mandatory, boolean inbound) {
		segmentId = formSegmentId(isFemale,mandatory,inbound);
	}
	
	private int formSegmentId(boolean isFemale, boolean mandatory, boolean inbound) {
		//powers of 2 make a unique identifier
		return (isFemale ? 1 : 0) + 
			   (mandatory ? 2 : 0) + 
			   ((mandatory && inbound) ? 2 : 0); //this last one is 2 not 4 because mandatory is a requirement
	}
	
	public int getSegmentId() {
		return segmentId;
	}

	public static int segmentWidth() {
		return 6;
	}
	
	public static BikeLogsumSegment[] getTourSegments(boolean isFemale, boolean mandatory) {
		return new BikeLogsumSegment[] {new BikeLogsumSegment(isFemale,mandatory,true),
										new BikeLogsumSegment(isFemale,mandatory,true)};
	}
	
	public static BikeLogsumSegment[] getTourSegments(boolean mandatory) {
		return new BikeLogsumSegment[] {new BikeLogsumSegment(true,mandatory,true),
										new BikeLogsumSegment(true,mandatory,true),
										new BikeLogsumSegment(false,mandatory,true),
										new BikeLogsumSegment(false,mandatory,true)};
	}
	
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
