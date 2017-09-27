package org.sandag.abm.ctramp;


import java.util.HashMap;

import junit.framework.Assert;

import org.junit.Test;

import umontreal.iro.lecuyer.probdist.LognormalDist;
public class TimeCoefficientDistributionsTest
{
    @Test
    public void testTimeCoefficientDistributions()
    {
    	HashMap<String, String> propMap = new HashMap<String, String>();
    	
    	String meanWork = "1.0";
		String standardDeviationWork = "0.8";

     	String meanNonWork = "1.0";
		String standardDeviationNonWork = "1.0";
		
		propMap.put("timeDistribution.mean.work", meanWork);
		propMap.put("timeDistribution.standardDeviation.work", standardDeviationWork);
		
		propMap.put("timeDistribution.mean.nonWork",meanNonWork);
		propMap.put("timeDistribution.standardDeviation.nonWork", standardDeviationNonWork);

		TimeCoefficientDistributions coefficientDistributions = new TimeCoefficientDistributions();
		coefficientDistributions.createTimeDistributions(propMap);
		
		LognormalDist work = coefficientDistributions.getTimeDistributionWork();
		LognormalDist nonWork = coefficientDistributions.getTimeDistributionNonWork();
		
		double actualMeanWork = work.getMean();
		double actualSDWork = work.getStandardDeviation();
		
	    Assert.assertEquals( meanWork, String.format("%.1f",actualMeanWork));
	    Assert.assertEquals( standardDeviationWork, String.format("%.1f",actualSDWork) );

		double actualMeanNonWork = nonWork.getMean();
		double actualSDNonWork = nonWork.getStandardDeviation();

		Assert.assertEquals( meanNonWork, String.format("%.1f",actualMeanNonWork));
	    Assert.assertEquals(standardDeviationNonWork, String.format("%.1f",actualSDNonWork));

    
    }

  
}
