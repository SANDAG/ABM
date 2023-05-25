/*
* The school-escort model was designed by PB (Gupta, Vovsha, et al)
* as part of the Maricopa Association of Governments (MAG)
* Activity-based Travel Model Development project.  
* 
* This source code, which implements the school escort model,
* was written exclusively for and funded by MAG as part of the 
* same project; therefore, per their contract, the 
* source code belongs to MAG and can only be used with their 
* permission.      
*
* It is being adapted for the Southern Oregon ABM by PB & RSG
* with permission from MAG and all references to
* the school escort model as well as source code adapted from this 
* original code should credit MAG's role in its development.
*
* The escort model and source code should not be transferred to or 
* adapted for other agencies or used in other projects without 
* expressed permission from MAG.  
* 
* The source code has been substantially revised to fit within the 
* SANDAG\MTC\ODOT CT-RAMP model structure by RSG (2015).
*/

package org.sandag.abm.ctramp;

import java.io.Serializable;

public class SchoolEscortChauffeurResult implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private final int pid;
	private final short dir;
	private final short bundle;
	private final short escortType;
	private final short[] childPnums;
    	
   	public SchoolEscortChauffeurResult( int pid, short dir, short bundle, short escortType, short[] childPnums ) {
   		this.pid = pid;
   		this.dir = dir;
   		this.bundle = bundle;
   		this.escortType = escortType;
   		this.childPnums = childPnums;
    }
    
   	public int getPid() {
   		return pid;
   	}
   	
   	public short getDirection() {
   		return dir;
   	}
   	
   	public short getBundle() {
   		return bundle;
   	}
   	
   	public short getEscortType() {
   		return escortType;
   	}
   	
   	public short[] getChildPnums() {
   		return childPnums;
   	}
   	
}
