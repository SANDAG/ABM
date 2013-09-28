/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.internalexternal;

import java.io.Serializable;

/**
 * ArcCtrampDmuFactory is a class that creates Visitor Model DMU objects
 * 
 * @author Joel Freedman
 */
public class InternalExternalDmuFactory implements
		InternalExternalDmuFactoryIf, Serializable {

	private InternalExternalModelStructure internalExternalModelStructure;

	public InternalExternalDmuFactory(
			InternalExternalModelStructure modelStructure) {
		this.internalExternalModelStructure = modelStructure;
	}

	public InternalExternalTourDestChoiceDMU getInternalExternalTourDestChoiceDMU() {
		return new InternalExternalTourDestChoiceDMU(
				internalExternalModelStructure);
	}

	public InternalExternalTripModeChoiceDMU getInternalExternalTripModeChoiceDMU() {
		return new InternalExternalTripModeChoiceDMU(
				internalExternalModelStructure);
	}
}
