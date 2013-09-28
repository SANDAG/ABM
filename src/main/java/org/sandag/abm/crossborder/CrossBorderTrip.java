package org.sandag.abm.crossborder;

import org.sandag.abm.ctramp.ModelStructure;

public class CrossBorderTrip {

	private int originMgra;
	private int destinationMgra;
	private int originTAZ;
	private int destinationTAZ;
	private int tripMode;
	private byte originPurpose;
	private byte destinationPurpose;
	private byte period;
	private boolean inbound;
	private boolean firstTrip;
	private boolean lastTrip;
	private boolean originIsTourDestination;
	private boolean destinationIsTourDestination;

	// best tap pairs for transit path; dimensioned by ride mode, then boarding
	// (0) and alighting (1)
	private int[][] bestWtwTapPairs;
	private int[][] bestWtdTapPairs;
	private int[][] bestDtwTapPairs;

	/**
	 * Default constructor; nothing initialized.
	 */
	public CrossBorderTrip() {

	}

	/**
	 * Create a cross border trip from a tour leg (no stops).
	 * 
	 * @param tour
	 *            The tour.
	 * @param outbound
	 *            Outbound direction
	 */
	public CrossBorderTrip(CrossBorderTour tour, boolean outbound) {

		initializeFromTour(tour, outbound);

	}

	/**
	 * Initilize from the tour.
	 * 
	 * @param tour
	 *            The tour.
	 * @param outbound
	 *            Outbound direction.
	 */
	public void initializeFromTour(CrossBorderTour tour, boolean outbound) {
		// Note: mode is unknown
		if (outbound) {
			this.originMgra = tour.getOriginMGRA();
			this.destinationMgra = tour.getDestinationMGRA();
			this.originTAZ = tour.getOriginTAZ();
			this.destinationTAZ = tour.getDestinationTAZ();
			this.originPurpose = -1;
			this.destinationPurpose = tour.getPurpose();
			this.period = (byte) tour.getDepartTime();
			this.inbound = false;
			this.firstTrip = true;
			this.lastTrip = false;
			this.originIsTourDestination = false;
			this.destinationIsTourDestination = true;
			this.bestWtwTapPairs = tour.getBestWtwTapPairsOut();
			this.bestDtwTapPairs = tour.getBestDtwTapPairsOut();
			this.bestWtdTapPairs = tour.getBestWtdTapPairsOut();
		} else {
			this.originMgra = tour.getDestinationMGRA();
			this.destinationMgra = tour.getOriginMGRA();
			this.originTAZ = tour.getDestinationTAZ();
			this.destinationTAZ = tour.getOriginTAZ();
			this.originPurpose = tour.getPurpose();
			this.destinationPurpose = -1;
			this.period = (byte) tour.getArriveTime();
			this.inbound = true;
			this.firstTrip = false;
			this.lastTrip = true;
			this.originIsTourDestination = true;
			this.destinationIsTourDestination = false;
			this.bestWtwTapPairs = tour.getBestWtwTapPairsIn();
			this.bestDtwTapPairs = tour.getBestDtwTapPairsIn();
			this.bestWtdTapPairs = tour.getBestWtdTapPairsIn();
		}

	}

	/**
	 * Create a cross border trip from a tour\stop. Note: trip mode is unknown.
	 * Stop period is only known for first, last stop on tour.
	 * 
	 * @param tour
	 *            The tour.
	 * @param stop
	 *            The stop
	 */
	public CrossBorderTrip(CrossBorderTour tour, CrossBorderStop stop,
			boolean toStop) {

		initializeFromStop(tour, stop, toStop);
	}

	/**
	 * Initialize from stop attributes. A trip will be created to the stop if
	 * toStop is true, else a trip will be created from the stop. Use after all
	 * stop locations are known, or else reset the stop origin and destination
	 * mgras accordingly after using.
	 * 
	 * @param tour
	 * @param stop
	 * @param toStop
	 */
	public void initializeFromStop(CrossBorderTour tour, CrossBorderStop stop,
			boolean toStop) {

		this.inbound = stop.isInbound();
		this.destinationIsTourDestination = false;
		this.originIsTourDestination = false;

		// if trip to stop, destination is stop mgra; else origin is stop mgra
		if (toStop) {
			this.destinationMgra = stop.getMgra();
			this.destinationTAZ = stop.getTAZ();
			this.destinationPurpose = stop.getPurpose();
		} else {
			this.originMgra = stop.getMgra();
			this.originTAZ = stop.getTAZ();
			this.originPurpose = stop.getPurpose();
		}
		CrossBorderStop[] stops;

		if (!inbound)
			stops = tour.getOutboundStops();
		else
			stops = tour.getInboundStops();

		// if outbound, and trip is to stop
		if (!inbound && toStop) {

			// first trip on outbound journey, origin is tour origin
			if (stop.getId() == 0) {
				this.originMgra = tour.getOriginMGRA();
				this.originTAZ = tour.getOriginTAZ();
				this.originPurpose = -1;
				this.period = (byte) tour.getDepartTime();
			} else {
				// not first trip on outbound journey, origin is last stop
				this.originMgra = stops[stop.getId() - 1].getMgra(); // last
																		// stop
																		// location
				this.originTAZ = stops[stop.getId() - 1].getTAZ(); // last stop
																	// location
				this.originPurpose = stops[stop.getId() - 1].getPurpose(); // last
																			// stop
																			// location
				this.period = (byte) stops[stop.getId() - 1].getStopPeriod();
			}
		} else if (!inbound && !toStop) {
			// outbound and trip is from stop to either next stop or tour
			// destination.

			// last trip on outbound journey, destination is tour destination
			if (stop.getId() == (stops.length - 1)) {
				this.destinationMgra = tour.getDestinationMGRA();
				this.destinationTAZ = tour.getDestinationTAZ();
				this.destinationPurpose = tour.getPurpose();
				this.destinationIsTourDestination = true;
			} else {
				// not last trip on outbound journey, destination is next stop
				this.destinationMgra = stops[stop.getId() + 1].getMgra();
				this.destinationTAZ = stops[stop.getId() + 1].getTAZ();
				this.destinationPurpose = stops[stop.getId() + 1].getPurpose();
			}

			// the period for the trip is the origin for the trip
			if (stop.getId() == 0)
				this.period = (byte) tour.getDepartTime();
			else
				this.period = (byte) stops[stop.getId() - 1].getStopPeriod();

		} else if (inbound && toStop) {
			// inbound, trip is to stop from either tour destination or last
			// stop.

			// first inbound trip; origin is tour destination
			if (stop.getId() == 0) {
				this.originMgra = tour.getDestinationMGRA();
				this.originTAZ = tour.getDestinationTAZ();
				this.originPurpose = tour.getPurpose();
				this.originIsTourDestination = true;
			} else {
				// not first inbound trip; origin is last stop
				this.originMgra = stops[stop.getId() - 1].getMgra(); // last
																		// stop
																		// location
				this.originTAZ = stops[stop.getId() - 1].getTAZ(); // last stop
																	// location
				this.originPurpose = stops[stop.getId() - 1].getPurpose();
			}

			// the period for the trip is the destination for the trip
			if (stop.getId() == stops.length - 1)
				this.period = (byte) tour.getArriveTime();
			else
				this.period = (byte) stops[stop.getId() + 1].getStopPeriod();
		} else {
			// inbound, trip is from stop to either next stop or tour origin.

			// last trip, destination is back to tour origin
			if (stop.getId() == (stops.length - 1)) {
				this.destinationMgra = tour.getOriginMGRA();
				this.destinationTAZ = tour.getOriginTAZ();
				this.destinationPurpose = -1;
				this.period = (byte) tour.getArriveTime();
			} else {
				// not last trip, destination is next stop
				this.destinationMgra = stops[stop.getId() + 1].getMgra();
				this.destinationTAZ = stops[stop.getId() + 1].getTAZ();
				this.destinationPurpose = stops[stop.getId() + 1].getPurpose();
				this.period = (byte) stops[stop.getId() + 1].getStopPeriod();
			}
		}

		// code period for first trip on tour
		if (toStop && !inbound && stop.getId() == 0) {
			this.firstTrip = true;
			this.lastTrip = false;
			this.period = (byte) tour.getDepartTime();
		}
		// code period for last trip on tour
		if (!toStop && inbound && stop.getId() == (stops.length - 1)) {
			this.firstTrip = false;
			this.lastTrip = true;
			this.period = (byte) tour.getArriveTime();
		}

	}

	/**
	 * @return the period
	 */
	public byte getPeriod() {
		return period;
	}

	/**
	 * @param period
	 *            the period to set
	 */
	public void setPeriod(byte period) {
		this.period = period;
	}

	/**
	 * @return the origin purpose
	 */
	public byte getOriginPurpose() {
		return originPurpose;
	}

	/**
	 * @param purpose
	 *            the purpose to set
	 */
	public void setOriginPurpose(byte purpose) {
		this.originPurpose = purpose;
	}

	/**
	 * @return the destination purpose
	 */
	public byte getDestinationPurpose() {
		return destinationPurpose;
	}

	/**
	 * @param purpose
	 *            the purpose to set
	 */
	public void setDestinationPurpose(byte purpose) {
		this.destinationPurpose = purpose;
	}

	/**
	 * @return the originMgra
	 */
	public int getOriginMgra() {
		return originMgra;
	}

	/**
	 * @param originMgra
	 *            the originMgra to set
	 */
	public void setOriginMgra(int originMgra) {
		this.originMgra = originMgra;
	}

	/**
	 * @return the destinationMgra
	 */
	public int getDestinationMgra() {
		return destinationMgra;
	}

	/**
	 * @param destinationMgra
	 *            the destinationMgra to set
	 */
	public void setDestinationMgra(int destinationMgra) {
		this.destinationMgra = destinationMgra;
	}

	public int getOriginTAZ() {
		return originTAZ;
	}

	public void setOriginTAZ(int originTAZ) {
		this.originTAZ = originTAZ;
	}

	public int getDestinationTAZ() {
		return destinationTAZ;
	}

	public void setDestinationTAZ(int destinationTAZ) {
		this.destinationTAZ = destinationTAZ;
	}

	/**
	 * @return the tripMode
	 */
	public int getTripMode() {
		return tripMode;
	}

	/**
	 * @param tripMode
	 *            the tripMode to set
	 */
	public void setTripMode(int tripMode) {
		this.tripMode = tripMode;
	}

	/**
	 * @return the bestWtwTapPairs
	 */
	public int[][] getBestWtwTapPairs() {
		return bestWtwTapPairs;
	}

	/**
	 * Return an array of boarding and alighting tap for the ride mode
	 * 
	 * @param rideMode
	 * @return
	 */
	public int[] getWtwTapPair(int rideMode) {
		return bestWtwTapPairs[rideMode];
	}

	/**
	 * @param bestWtwTapPairs
	 *            the bestWtwTapPairs to set
	 */
	public void setBestWtwTapPairs(int[][] bestWtwTapPairs) {
		this.bestWtwTapPairs = bestWtwTapPairs;
	}

	/**
	 * @return the bestWtdTapPairs
	 */
	public int[][] getBestWtdTapPairs() {
		return bestWtdTapPairs;
	}

	/**
	 * Return an array of boarding and alighting tap for the ride mode
	 * 
	 * @param rideMode
	 * @return
	 */
	public int[] getWtdTapPair(int rideMode) {
		return bestWtdTapPairs[rideMode];
	}

	/**
	 * @param bestWtdTapPairs
	 *            the bestWtdTapPairs to set
	 */
	public void setBestWtdTapPairs(int[][] bestWtdTapPairs) {
		this.bestWtdTapPairs = bestWtdTapPairs;
	}

	/**
	 * @return the bestDtwTapPairs
	 */
	public int[][] getBestDtwTapPairs() {
		return bestDtwTapPairs;
	}

	/**
	 * Return an array of boarding and alighting tap for the ride mode
	 * 
	 * @param rideMode
	 * @return
	 */
	public int[] getDtwTapPair(int rideMode) {
		return bestDtwTapPairs[rideMode];
	}

	/**
	 * @param bestDtwTapPairs
	 *            the bestDtwTapPairs to set
	 */
	public void setBestDtwTapPairs(int[][] bestDtwTapPairs) {
		this.bestDtwTapPairs = bestDtwTapPairs;
	}

	/**
	 * @return the inbound
	 */
	public boolean isInbound() {
		return inbound;
	}

	/**
	 * @param inbound
	 *            the inbound to set
	 */
	public void setInbound(boolean inbound) {
		this.inbound = inbound;
	}

	/**
	 * @return the firstTrip
	 */
	public boolean isFirstTrip() {
		return firstTrip;
	}

	/**
	 * @param firstTrip
	 *            the firstTrip to set
	 */
	public void setFirstTrip(boolean firstTrip) {
		this.firstTrip = firstTrip;
	}

	/**
	 * @return the lastTrip
	 */
	public boolean isLastTrip() {
		return lastTrip;
	}

	/**
	 * @param lastTrip
	 *            the lastTrip to set
	 */
	public void setLastTrip(boolean lastTrip) {
		this.lastTrip = lastTrip;
	}

	/**
	 * @return the originIsTourDestination
	 */
	public boolean isOriginIsTourDestination() {
		return originIsTourDestination;
	}

	/**
	 * @param originIsTourDestination
	 *            the originIsTourDestination to set
	 */
	public void setOriginIsTourDestination(boolean originIsTourDestination) {
		this.originIsTourDestination = originIsTourDestination;
	}

	/**
	 * @return the destinationIsTourDestination
	 */
	public boolean isDestinationIsTourDestination() {
		return destinationIsTourDestination;
	}

	/**
	 * @param destinationIsTourDestination
	 *            the destinationIsTourDestination to set
	 */
	public void setDestinationIsTourDestination(
			boolean destinationIsTourDestination) {
		this.destinationIsTourDestination = destinationIsTourDestination;
	}

}
