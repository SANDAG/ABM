/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.airport;

import java.io.Serializable;
import org.sandag.abm.ctramp.AtWorkSubtourFrequencyDMU;
import org.sandag.abm.ctramp.AutoOwnershipChoiceDMU;
import org.sandag.abm.ctramp.CoordinatedDailyActivityPatternDMU;
import org.sandag.abm.ctramp.CtrampDmuFactoryIf;
import org.sandag.abm.ctramp.DcSoaDMU;
import org.sandag.abm.ctramp.DestChoiceTwoStageSoaTazDistanceUtilityDMU;
import org.sandag.abm.ctramp.DestChoiceDMU;
import org.sandag.abm.ctramp.DestChoiceTwoStageModelDMU;
import org.sandag.abm.ctramp.ParkingProvisionChoiceDMU;
import org.sandag.abm.ctramp.IndividualMandatoryTourFrequencyDMU;
import org.sandag.abm.ctramp.IndividualNonMandatoryTourFrequencyDMU;
import org.sandag.abm.ctramp.JointTourModelsDMU;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.ParkingChoiceDMU;
import org.sandag.abm.ctramp.StopFrequencyDMU;
import org.sandag.abm.ctramp.StopLocationDMU;
import org.sandag.abm.ctramp.TourDepartureTimeAndDurationDMU;
import org.sandag.abm.ctramp.TransponderChoiceDMU;
import org.sandag.abm.ctramp.TripModeChoiceDMU;

/**
 * ArcCtrampDmuFactory is a class that creates Airport Model DMU objects
 * 
 * @author Joel Freedman
 */
public class AirportDmuFactory implements AirportDmuFactoryIf, Serializable {

	private AirportModelStructure airportModelStructure;

	public AirportDmuFactory(AirportModelStructure modelStructure) {
		this.airportModelStructure = modelStructure;
	}

	public AirportModelDMU getAirportModelDMU() {
		return new AirportModelDMU();
	}
}
