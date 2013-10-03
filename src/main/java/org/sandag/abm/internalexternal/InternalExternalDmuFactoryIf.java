package org.sandag.abm.internalexternal;

/**
 * A DMU factory interface
 */
public interface InternalExternalDmuFactoryIf
{

    InternalExternalTourDestChoiceDMU getInternalExternalTourDestChoiceDMU();

    InternalExternalTripModeChoiceDMU getInternalExternalTripModeChoiceDMU();

}
