/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.sandag.abm.application;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sandag.abm.ctramp.AtWorkSubtourFrequencyDMU;
import org.sandag.abm.ctramp.AutoOwnershipChoiceDMU;
import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.CoordinatedDailyActivityPatternDMU;
import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.DcSoaDMU;
import org.sandag.abm.ctramp.DestChoiceDMU;
import org.sandag.abm.ctramp.DestChoiceTwoStageModelDMU;
import org.sandag.abm.ctramp.DestChoiceTwoStageSoaTazDistanceUtilityDMU;
import org.sandag.abm.ctramp.IndividualMandatoryTourFrequencyDMU;
import org.sandag.abm.ctramp.IndividualNonMandatoryTourFrequencyDMU;
import org.sandag.abm.ctramp.InternalExternalTripChoiceDMU;
import org.sandag.abm.ctramp.JointTourModelsDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.ParkingChoiceDMU;
import org.sandag.abm.ctramp.ParkingProvisionChoiceDMU;
import org.sandag.abm.ctramp.StopFrequencyDMU;
import org.sandag.abm.ctramp.StopLocationDMU;
import org.sandag.abm.ctramp.TelecommuteDMU;
import org.sandag.abm.ctramp.TourDepartureTimeAndDurationDMU;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.TransponderChoiceDMU;
import org.sandag.abm.ctramp.TripModeChoiceDMU;
import org.sandag.abm.ctramp.TourDriverDMU;

/**
 * ArcCtrampDmuFactory is a class that ...
 * 
 * @author Kimberly Grommes
 * @version 1.0, Jul 17, 2008 Created by IntelliJ IDEA.
 */
public class SandagCtrampDmuFactory
        implements CtrampDmuFactoryIf, Serializable
{

    private ModelStructure modelStructure;
    private Map<String, String> propertyMap;

    public SandagCtrampDmuFactory(ModelStructure modelStructure, Map<String, String> propertyMap)
    {
        this.modelStructure = modelStructure;
        this.propertyMap = propertyMap;
    }

    public AutoOwnershipChoiceDMU getAutoOwnershipDMU()
    {
        return new SandagAutoOwnershipChoiceDMU();
    }

    public TransponderChoiceDMU getTransponderChoiceDMU()
    {
        return new SandagTransponderChoiceDMU();
    }

    public TelecommuteDMU getTelecommuteDMU()
    {
        return new SandagTelecommuteDMU();
    }

    public InternalExternalTripChoiceDMU getInternalExternalTripChoiceDMU()
    {
        return new SandagInternalExternalTripChoiceDMU();
    }

    public ParkingProvisionChoiceDMU getFreeParkingChoiceDMU()
    {
        return new SandagParkingProvisionChoiceDMU();
    }

    public CoordinatedDailyActivityPatternDMU getCoordinatedDailyActivityPatternDMU()
    {
        return new SandagCoordinatedDailyActivityPatternDMU();
    }

    public DcSoaDMU getDcSoaDMU()
    {
        return new SandagDcSoaDMU();
    }

    public DestChoiceDMU getDestChoiceDMU()
    {
        return new SandagDestChoiceDMU(modelStructure);
    }

    public DestChoiceTwoStageModelDMU getDestChoiceSoaTwoStageDMU()
    {
        return new SandagDestChoiceSoaTwoStageModelDMU(modelStructure);
    }

    public DestChoiceTwoStageSoaTazDistanceUtilityDMU getDestChoiceSoaTwoStageTazDistUtilityDMU()
    {
        return new SandagDestChoiceSoaTwoStageTazDistUtilityDMU();
    }

    public TourModeChoiceDMU getModeChoiceDMU()
    {
        SandagTourModeChoiceDMU dmu = new SandagTourModeChoiceDMU(modelStructure,null);
        dmu.setBikeLogsum(BikeLogsum.getBikeLogsum(propertyMap));
        return dmu;
    }

    public IndividualMandatoryTourFrequencyDMU getIndividualMandatoryTourFrequencyDMU()
    {
        return new SandagIndividualMandatoryTourFrequencyDMU();
    }

    public TourDepartureTimeAndDurationDMU getTourDepartureTimeAndDurationDMU()
    {
        return new SandagTourDepartureTimeAndDurationDMU(modelStructure);
    }

    public AtWorkSubtourFrequencyDMU getAtWorkSubtourFrequencyDMU()
    {
        return new SandagAtWorkSubtourFrequencyDMU(modelStructure);
    }

    public JointTourModelsDMU getJointTourModelsDMU()
    {
        return new SandagJointTourModelsDMU(modelStructure);
    }

    public IndividualNonMandatoryTourFrequencyDMU getIndividualNonMandatoryTourFrequencyDMU()
    {
        return new SandagIndividualNonMandatoryTourFrequencyDMU();
    }

    public StopFrequencyDMU getStopFrequencyDMU()
    {
        return new SandagStopFrequencyDMU(modelStructure);
    }

    public TourDriverDMU getTourDriverDMU() 
    {
    	return new SandagTourDriverDMU(modelStructure);
    }
    
    public StopLocationDMU getStopLocationDMU()
    {
        return new SandagStopLocationDMU(modelStructure,propertyMap);
    }

    public TripModeChoiceDMU getTripModeChoiceDMU()
    {
        SandagTripModeChoiceDMU dmu = new SandagTripModeChoiceDMU(modelStructure,null);
        dmu.setBikeLogsum(BikeLogsum.getBikeLogsum(propertyMap));
        return dmu;
    }

    public ParkingChoiceDMU getParkingChoiceDMU()
    {
        return new SandagParkingChoiceDMU();
    }

}
