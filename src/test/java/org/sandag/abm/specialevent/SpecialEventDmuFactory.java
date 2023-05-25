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
package org.sandag.abm.specialevent;

import java.io.Serializable;
import org.sandag.abm.application.SandagModelStructure;

/**
 * ArcCtrampDmuFactory is a class that creates Visitor Model DMU objects
 * 
 * @author Joel Freedman
 */
public class SpecialEventDmuFactory
        implements SpecialEventDmuFactoryIf, Serializable
{

    private SandagModelStructure sandagModelStructure;

    public SpecialEventDmuFactory(SandagModelStructure modelStructure)
    {
        this.sandagModelStructure = modelStructure;
    }

    public SpecialEventTripModeChoiceDMU getSpecialEventTripModeChoiceDMU()
    {
        return new SpecialEventTripModeChoiceDMU(sandagModelStructure, null);
    }

    public SpecialEventOriginChoiceDMU getSpecialEventOriginChoiceDMU()
    {
        return new SpecialEventOriginChoiceDMU(sandagModelStructure);
    }

}
