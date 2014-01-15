package org.sandag.abm.common;

public abstract class ConditionalDMU extends DMU {

	protected double getTranistSkimFromMethodConditional(String methodName, boolean conditional) throws Exception 
	{
    	//getDt_lb_EgrTime
    	String[] parts = methodName.split("_");
    	String part1 = parts[0].replace("get", ""); 
    	String part2 = parts[1];
    	boolean part3HasUnderscore = (parts.length > 3);
    	String part3 = parts[2];
    	if (part3HasUnderscore)
    	{
    		part3 += "_" + parts[3];
    	}
    	part3 = part3.replace("Time", "").replace("Walk", "");
    	
    	int var1 = -1, var2 = -1, var3 = -1, var4 = -1;
    	
    	if (part1.equalsIgnoreCase("WTW"))
    		var1 = WTW;
    	else if (part1.equalsIgnoreCase("DT"))
    	{
    		if (conditional)
    		{
    			var1 = DTW;
    		}
    		else
    		{
    			var1 = WTD;		
    		}
    	}    	
    	
    	else
    		throw new Exception("First part of getTransitSkim is invalid with variable " + var1);
    	    	
    	if (part2.equalsIgnoreCase("LB"))
    		var2 = LB;
    	else if (part2.equalsIgnoreCase("EB"))
    		var2 = EB;
    	else if (part2.equalsIgnoreCase("BRT"))
    		var2 = BRT;
    	else if (part2.equalsIgnoreCase("LR"))
    		var2 = LR;
    	else if (part2.equalsIgnoreCase("CR"))
    		var2 = CR;
    	else
    		throw new Exception("Second part of getTransitSkim is invalid with variable " + var1);
    	
    	
    	if (var1 == DTW && part3.equalsIgnoreCase("ACC"))
    		return 0;
    	if (var1 == WTD && part3.equalsIgnoreCase("EGR"))
    		return 0;
    	if (part3.equalsIgnoreCase("FWAIT"))
    		var3 = FWAIT;
    	else if (part3.equalsIgnoreCase("XWAIT"))
    		var3 = XWAIT;
    	else if (part3.equalsIgnoreCase("ACC"))
    		var3 = ACC;
    	else if (part3.equalsIgnoreCase("EGR"))
    		var3 = EGR;
    	else if (part3.equalsIgnoreCase("AUX"))
    		var3 = AUX;
    	else if (part3.equalsIgnoreCase("FARE"))
    		var3 = FARE;
    	else if (part3.equalsIgnoreCase("XFERS"))
    		var3 = XFERS;
    	else if (part3.equalsIgnoreCase("LB_IVT"))
    		var3 = LB_IVT;
    	else if (part3.equalsIgnoreCase("EB_IVT"))
    		var3 = EB_IVT;
    	else if (part3.equalsIgnoreCase("BRT_IVT"))
    		var3 = BRT_IVT;
    	else if (part3.equalsIgnoreCase("LRT_IVT")) //Note difference
    		var3 = LR_IVT;
    	else if (part3.equalsIgnoreCase("CR_IVT"))
    		var3 = CR_IVT;
    	else if (var1 == DTW && part3.equalsIgnoreCase("DRV"))
    		var3 = ACC;
    	else if (var1 == WTD && part3.equalsIgnoreCase("DRV"))
    		var3 = EGR;
    	else
    		throw new Exception("Third part of getTransitSkim is invalid with variable " + var1);
    	
    	return getTransitSkim(var1, var2, var3);    	
	}
	
	protected abstract double getTransitSkim(int var1, int var2, int var3);
}
