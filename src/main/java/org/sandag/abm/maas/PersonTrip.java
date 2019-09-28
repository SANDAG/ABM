package org.sandag.abm.maas;

/**
 * A holder class for trips.
 * @author joel.freedman
 *
 */
class PersonTrip implements Comparable{
    protected int uniqueId;
    protected long hhid;		
    protected long personId;
    protected int personNumber;
    protected int tourid;
    protected int stopid;
    protected int inbound;
    protected int joint;
    protected int numberParticipants;

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
	
	public PersonTrip(int uniqueId,long hhid,long personId,int personNumber, int tourid,int stopid,int inbound,int joint,int originMaz, int destinationMaz, int departPeriod, float departTime, float sampleRate, int mode, int boardingTap, int alightingTap, int set, boolean rideSharer){
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
		
		//set the pickup MAZ to the originMaz
		this.pickupMaz = originMaz;
		
		
	}

	public int getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(int uniqueId) {
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

	public int getNumberParticipants() {
		return numberParticipants;
	}

	public void setNumberParticipants(int numberParticipants) {
		this.numberParticipants = numberParticipants;
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

		if(this.uniqueId== that.getUniqueId())
			return true;
		
		return false;
	}
}
