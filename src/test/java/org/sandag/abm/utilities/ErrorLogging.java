package org.sandag.abm.utilities;

import java.util.HashMap;
/**
 * ErrorLogging definitions
 * 
 * @author wsu
 * @mail Wu.Sun@sandag.org
 * @version 13.3.0
 * @since 2016-06-20
 *
 */
public class ErrorLogging {
    public static final String                          AtTapNotInTransitNetwork = "A TAP in AT network is not in TRANSIT network!";
    public static final String                          TransitTapNotInAt      = "A TAP in TRANSIT network is not in AT network!";
    public static final String                          InconsistentTapPostions = "Positions of a TAP are different in AT and TRANSIT networks!";
    public static final String                          NoAtTaps = "No valid TAPs in AT network!";    
    public static final String                          NoTransitTaps = "No valid TAPs in TRANSIT network!";    
    protected HashMap<String,String>                    atErrorIndexMap;
    
    public ErrorLogging()
    {

        atErrorIndexMap = new HashMap<String,String>();
        createAtErrorIndexMap();
    }
    private void createAtErrorIndexMap()
    {
    	atErrorIndexMap.put("AT1", AtTapNotInTransitNetwork);
    	atErrorIndexMap.put("AT2", TransitTapNotInAt );
    	atErrorIndexMap.put("AT3", InconsistentTapPostions);
    	atErrorIndexMap.put("AT4", NoAtTaps);
    	atErrorIndexMap.put("AT5", NoTransitTaps);
    }
    
    public String getAtError(String index)
    {
        return atErrorIndexMap.get(index);
    }    
    
}
