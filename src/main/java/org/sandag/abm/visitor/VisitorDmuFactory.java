/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.visitor;

import java.io.Serializable;

/**
 * ArcCtrampDmuFactory is a class that creates Visitor Model DMU objects
 * 
 * @author Joel Freedman
 */
public class VisitorDmuFactory implements VisitorDmuFactoryIf, Serializable {

	private VisitorModelStructure visitorModelStructure;

	public VisitorDmuFactory(VisitorModelStructure modelStructure) {
		this.visitorModelStructure = modelStructure;
	}

	public VisitorTourModeChoiceDMU getVisitorTourModeChoiceDMU() {
		return new VisitorTourModeChoiceDMU(visitorModelStructure);
	}

	public VisitorTourDestChoiceDMU getVisitorTourDestChoiceDMU() {
		return new VisitorTourDestChoiceDMU(visitorModelStructure);
	}

	public VisitorStopLocationChoiceDMU getVisitorStopLocationChoiceDMU() {
		return new VisitorStopLocationChoiceDMU(visitorModelStructure);
	}

	public VisitorTripModeChoiceDMU getVisitorTripModeChoiceDMU() {
		return new VisitorTripModeChoiceDMU(visitorModelStructure);
	}
}
