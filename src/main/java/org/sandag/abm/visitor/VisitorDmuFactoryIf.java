package org.sandag.abm.visitor;

/**
 * A DMU factory interface
 */
public interface VisitorDmuFactoryIf
{

    public VisitorTourModeChoiceDMU getVisitorTourModeChoiceDMU();

    public VisitorTourDestChoiceDMU getVisitorTourDestChoiceDMU();

    public VisitorStopLocationChoiceDMU getVisitorStopLocationChoiceDMU();

    public VisitorTripModeChoiceDMU getVisitorTripModeChoiceDMU();

}
