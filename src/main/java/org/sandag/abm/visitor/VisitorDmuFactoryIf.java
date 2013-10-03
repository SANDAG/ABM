package org.sandag.abm.visitor;

/**
 * A DMU factory interface
 */
public interface VisitorDmuFactoryIf
{

    VisitorTourModeChoiceDMU getVisitorTourModeChoiceDMU();

    VisitorTourDestChoiceDMU getVisitorTourDestChoiceDMU();

    VisitorStopLocationChoiceDMU getVisitorStopLocationChoiceDMU();

    VisitorTripModeChoiceDMU getVisitorTripModeChoiceDMU();

}
