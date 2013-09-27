package org.sandag.abm.crossborder;

/**
 * A DMU factory interface
 */
public interface CrossBorderDmuFactoryIf {

	public CrossBorderTourModeChoiceDMU getCrossBorderTourModeChoiceDMU();

	public CrossBorderStationDestChoiceDMU getCrossBorderStationChoiceDMU();

	public CrossBorderTripModeChoiceDMU getCrossBorderTripModeChoiceDMU();

	public CrossBorderStopLocationChoiceDMU getCrossBorderStopLocationChoiceDMU();

}
