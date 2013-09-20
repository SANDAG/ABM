package org.sandag.abm.ctramp;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 9, 2008 Time: 3:13:17 PM To change this template use File | Settings | File Templates.
 */
public interface CtrampDmuFactoryIf
{

    public AutoOwnershipChoiceDMU getAutoOwnershipDMU();

    public ParkingProvisionChoiceDMU getFreeParkingChoiceDMU();

    public TransponderChoiceDMU getTransponderChoiceDMU();

    public InternalExternalTripChoiceDMU getInternalExternalTripChoiceDMU();

    public CoordinatedDailyActivityPatternDMU getCoordinatedDailyActivityPatternDMU();

    public DcSoaDMU getDcSoaDMU();

    public DestChoiceDMU getDestChoiceDMU();

    public DestChoiceTwoStageModelDMU getDestChoiceSoaTwoStageDMU();

    public DestChoiceTwoStageSoaTazDistanceUtilityDMU getDestChoiceSoaTwoStageTazDistUtilityDMU();

    public TourModeChoiceDMU getModeChoiceDMU();

    public IndividualMandatoryTourFrequencyDMU getIndividualMandatoryTourFrequencyDMU();

    public TourDepartureTimeAndDurationDMU getTourDepartureTimeAndDurationDMU();

    public AtWorkSubtourFrequencyDMU getAtWorkSubtourFrequencyDMU();

    public JointTourModelsDMU getJointTourModelsDMU();

    public IndividualNonMandatoryTourFrequencyDMU getIndividualNonMandatoryTourFrequencyDMU();

    public StopFrequencyDMU getStopFrequencyDMU();

    public StopLocationDMU getStopLocationDMU();

    public TripModeChoiceDMU getTripModeChoiceDMU();

    public ParkingChoiceDMU getParkingChoiceDMU();

}
