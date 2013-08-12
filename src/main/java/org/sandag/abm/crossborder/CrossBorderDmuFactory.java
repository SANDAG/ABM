/*
 * Copyright 2005 PB Consult Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.sandag.abm.crossborder;

import java.io.Serializable;

/**
 * ArcCtrampDmuFactory is a class that creates Visitor Model DMU objects
 * 
 * @author Joel Freedman
 */
public class CrossBorderDmuFactory
        implements CrossBorderDmuFactoryIf, Serializable
{

    private CrossBorderModelStructure crossBorderModelStructure;

    public CrossBorderDmuFactory(CrossBorderModelStructure modelStructure)
    {
        this.crossBorderModelStructure = modelStructure;
    }

    public CrossBorderTourModeChoiceDMU getCrossBorderTourModeChoiceDMU()
    {
        return new CrossBorderTourModeChoiceDMU(crossBorderModelStructure);
    }

    public CrossBorderTripModeChoiceDMU getCrossBorderTripModeChoiceDMU()
    {
        return new CrossBorderTripModeChoiceDMU(crossBorderModelStructure);
    }

    public CrossBorderStationDestChoiceDMU getCrossBorderStationChoiceDMU()
    {
        return new CrossBorderStationDestChoiceDMU(crossBorderModelStructure);
    }

    public CrossBorderStopLocationChoiceDMU getCrossBorderStopLocationChoiceDMU()
    {
        return new CrossBorderStopLocationChoiceDMU(crossBorderModelStructure);
    }

}
