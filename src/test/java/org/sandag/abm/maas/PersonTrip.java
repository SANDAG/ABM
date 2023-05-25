package org.sandag.abm.maas;

/**
 * A holder class for trips.
 * @author joel.freedman
 *
 */
class PersonTrip implements Comparable, Cloneable{
    protected String uniqueId;
    protected long hhid;		
    protected long personId;
    protected int personNumber;
    protected int tourid;
    protected int stopid;
    protected int inbound;
    protected int joint;
 
    protected int originMaz;
    protected int destinationMaz;
    protected short departPeriod;
    protected float departTime; //minutes after 3 AM
    protected float sampleRate;
    protected int mode;
    protected int parkingMaz;
    protected byte avAvailable;
    protected boolean rideSharer;
	protected int pickupMaz;
	protected int dropoffMaz;
	
	public PersonTrip(String uniqueId,long hhid,long personId,int personNumber, int tourid,int stopid,int inbound,int joint,int originMaz, int destinationMaz, int departPeriod, float departTime, float sampleRate, int mode, int boardingTap, int alightingTap, int set, boolean rideSharer){
       	this.uniqueId = uniqueId;
		this.hhid = hhid;		
       	this.personId = personId;
    	this.personNumber = personNumber;
    	this.tourid = tourid;
    	this.stopid =  stopid;
    	this.inbound = inbound;
    	this.joint = joint;

		this.originMaz = originMaz;
		this.destinationMaz = destinationMaz;
		this.departPeriod = (short) departPeriod;
		this.departTime = departTime;
		this.sampleRate = sampleRate;
		this.mode = mode;
		this.rideSharer = rideSharer;
		
		//set the pickup MAZ to the originMaz and dropoff MAZ to the destinationMaz
		this.pickupMaz = originMaz;
		this.dropoffMaz = destinationMaz;
		
		
		
		
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public long getHhid() {
		return hhid;
	}

	public void setHhid(long hhid) {
		this.hhid = hhid;
	}

	public long getPersonId() {
		return personId;
	}

	public void setPersonId(long personId) {
		this.personId = personId;
	}

	public int getPersonNumber() {
		return personNumber;
	}

	public void setPersonNumber(int personNumber) {
		this.personNumber = personNumber;
	}

	public int getTourid() {
		return tourid;
	}

	public void setTourid(int tourid) {
		this.tourid = tourid;
	}

	public int getStopid() {
		return stopid;
	}

	public void setStopid(int stopid) {
		this.stopid = stopid;
	}

	public int getInbound() {
		return inbound;
	}

	public void setInbound(int inbound) {
		this.inbound = inbound;
	}

	public int getJoint() {
		return joint;
	}

	public void setJoint(int joint) {
		this.joint = joint;
	}

	public int getOriginMaz() {
		return originMaz;
	}

	public void setOriginMaz(int originMaz) {
		this.originMaz = originMaz;
	}

	public int getPickupMaz() {
		return pickupMaz;
	}

	public void setPickupMaz(int pickupMaz) {
		this.pickupMaz = pickupMaz;
	}

	public int getDestinationMaz() {
		return destinationMaz;
	}

	public void setDestinationMaz(int destinationMaz) {
		this.destinationMaz = destinationMaz;
	}

	public int getDropoffMaz() {
		return dropoffMaz;
	}

	public void setDropoffMaz(int dropoffMaz) {
		this.dropoffMaz = dropoffMaz;
	}

	public short getDepartPeriod() {
		return departPeriod;
	}

	public void setDepartPeriod(short departPeriod) {
		this.departPeriod = departPeriod;
	}

	public float getDepartTime() {
		return departTime;
	}

	public void setDepartTime(float departTime) {
		this.departTime = departTime;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public void setSampleRate(float sampleRate) {
		this.sampleRate = sampleRate;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	
	public int getParkingMaz() {
		return parkingMaz;
	}

	public void setParkingMaz(int parkingMaz) {
		this.parkingMaz = parkingMaz;
	}

	public int getAvAvailable() {
		return avAvailable;
	}

	public void setAvAvailable(byte avAvailable) {
		this.avAvailable = avAvailable;
	}

	public boolean isRideSharer() {
		return rideSharer;
	}
	
	public void setRideSharer(boolean rideSharer) {
		this.rideSharer=rideSharer;
	}
	

	/**
	 * Compare based on departure time.
	 */
	public int compareTo(Object aThat) {
	    final int BEFORE = -1;
	    final int EQUAL = 0;
	    final int AFTER = 1;

	    final PersonTrip that = (PersonTrip)aThat;

	    //primitive numbers follow this form
	    if (this.departTime < that.departTime) return BEFORE;
	    if (this.departTime > that.departTime) return AFTER;

		return EQUAL;
	}
	
	/**
	 * Override equals.
	 */
	public boolean equals(Object aThat){
	    final PersonTrip that = (PersonTrip)aThat;

		if(this.uniqueId.compareTo(that.getUniqueId())==0)
			return true;
		
		return false;
	}
	
	
	/**
	 * If this trip and thatTrip are both joint trips from the same travel party, return true, else return false
	 * the comparison is done based on uniqueID, where the end of the unique id is the participant number,
	 * for example "_1" versus "_2". The method compares the part of the unique ID before the participant number,
	 * and returns true if they are the same.
	 * 
	 * @param thatTrip
	 * @return True if both from same party, else false
	 */
	public boolean sameParty(PersonTrip thatTrip) {
		
		if(joint==0)
			return false;
		
		if(thatTrip.getJoint()==0)
			return false;
		
		int lastIndex = uniqueId.lastIndexOf("_");
		String thisUniqueIdMinusParticipantID = uniqueId.substring(0, lastIndex);
		
		lastIndex = thatTrip.getUniqueId().lastIndexOf("_");
		String thatUniqueIdMinusParticipantID = thatTrip.getUniqueId().substring(0, lastIndex);
	
		if(thisUniqueIdMinusParticipantID.compareTo(thatUniqueIdMinusParticipantID)==0)
			return true;
		
		return false;
		
	}
	
	public Object clone() throws
    	CloneNotSupportedException 
	{ 
		PersonTrip trip = (PersonTrip) super.clone();
		trip.uniqueId= new String();
		return trip;
	} 
}
