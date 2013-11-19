package org.sandag.abm.crossborder;

/**
 * A DMU factory interface
 */
public interface CrossBorderDmuFactoryIf
{

    CrossBorderTourModeChoiceDMU getCrossBorderTourModeChoiceDMU();

    CrossBorderStationDestChoiceDMU getCrossBorderStationChoiceDMU();

    CrossBorderTripModeChoiceDMU getCrossBorderTripModeChoiceDMU();

    CrossBorderStopLocationChoiceDMU getCrossBorderStopLocationChoiceDMU();

}
