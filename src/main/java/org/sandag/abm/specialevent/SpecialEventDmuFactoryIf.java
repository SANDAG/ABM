package org.sandag.abm.specialevent;

/**
 * A DMU factory interface
 */
public interface SpecialEventDmuFactoryIf
{

    SpecialEventTripModeChoiceDMU getSpecialEventTripModeChoiceDMU();

    SpecialEventOriginChoiceDMU getSpecialEventOriginChoiceDMU();

}
