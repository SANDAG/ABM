package org.sandag.abm.ctramp;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 9, 2008 Time: 3:13:17 PM To
 * change this template use File | Settings | File Templates.
 */
public interface CtrampDmuFactoryIf
{

    AutoOwnershipChoiceDMU getAutoOwnershipDMU();

    ParkingProvisionChoiceDMU getFreeParkingChoiceDMU();

    TelecommuteDMU getTelecommuteDMU();
    
    TransponderChoiceDMU getTransponderChoiceDMU();

    InternalExternalTripChoiceDMU getInternalExternalTripChoiceDMU();

    CoordinatedDailyActivityPatternDMU getCoordinatedDailyActivityPatternDMU();

    DcSoaDMU getDcSoaDMU();

    DestChoiceDMU getDestChoiceDMU();

    DestChoiceTwoStageModelDMU getDestChoiceSoaTwoStageDMU();

    DestChoiceTwoStageSoaTazDistanceUtilityDMU getDestChoiceSoaTwoStageTazDistUtilityDMU();

    TourModeChoiceDMU getModeChoiceDMU();

    IndividualMandatoryTourFrequencyDMU getIndividualMandatoryTourFrequencyDMU();

    TourDepartureTimeAndDurationDMU getTourDepartureTimeAndDurationDMU();

    AtWorkSubtourFrequencyDMU getAtWorkSubtourFrequencyDMU();

    JointTourModelsDMU getJointTourModelsDMU();

    IndividualNonMandatoryTourFrequencyDMU getIndividualNonMandatoryTourFrequencyDMU();

    StopFrequencyDMU getStopFrequencyDMU();

    StopLocationDMU getStopLocationDMU();

    TripModeChoiceDMU getTripModeChoiceDMU();

    ParkingChoiceDMU getParkingChoiceDMU();
    
    MicromobilityChoiceDMU getMicromobilityChoiceDMU();

}
