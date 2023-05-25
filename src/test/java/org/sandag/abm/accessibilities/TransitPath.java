package org.sandag.abm.accessibilities;

public class TransitPath implements Comparable<TransitPath>{
	    
    public int              oMaz;
    public int              dMaz;
    public int              pTap;
    public int              aTap;
    public int              set;
    public int              accEgr;
    public float            accUtil;
    public float            tapTapUtil;
    public float            egrUtil;
    public static final int NA = -999;
        
	public TransitPath(int oMaz, int dMaz, int pTap, int aTap, int set, int accEgr, float accUtil, float tapTapUtil, float egrUtil) {
		this.oMaz = oMaz;
		this.dMaz = dMaz;
		this.pTap = pTap;
		this.aTap = aTap;
		this.set = set;
		this.accEgr = accEgr;
		this.accUtil = accUtil;
		this.tapTapUtil = tapTapUtil;
		this.egrUtil = egrUtil;
	}
	
	public float getTotalUtility() {
		return(accUtil + tapTapUtil + egrUtil);
	}
	
	@Override
	public int compareTo(TransitPath o) {
					
		//return compareTo value
	    if ( getTotalUtility() < o.getTotalUtility() ) {
	    	return -1;
	    } else if (getTotalUtility() == o.getTotalUtility()) {
	    	return 0;
	    } else {
	    	return 1;
	    }		
	}
	
}