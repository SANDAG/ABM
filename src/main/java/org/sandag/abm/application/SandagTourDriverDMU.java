package org.sandag.abm.application;

import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.TourDriverDMU;


public class SandagTourDriverDMU extends TourDriverDMU {

    protected transient Logger logger = Logger.getLogger(SandagTourDriverDMU.class);
    
    public SandagTourDriverDMU(ModelStructure modelStructure ){
    	super ( modelStructure );
    }
        
}