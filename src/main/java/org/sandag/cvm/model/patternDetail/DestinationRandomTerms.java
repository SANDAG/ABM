package org.sandag.cvm.model.patternDetail;

import java.util.Random;

import org.apache.log4j.Logger;

import org.sandag.cvm.common.model.GumbelErrorTerm;

public class DestinationRandomTerms {

	protected static Logger logger = Logger.getLogger(DestinationRandomTerms.class);


    static final Random random = new Random();
    static final double log4pi = Math.log(4*Math.PI);

    final int normalPoolSize = 100000;
    /**
     * This is the pool of standard normals, used for zones with size < useGumbel 
     */
    double[] normalPool = null;
    
    final int uniformPoolSize = 100000;
    /**
     * This is the pool of uniform distributed numbers, used with size > useGumbel 
     */
    double[] uniformPool = null;
    
    // TODO set useGumbel to something > 1
    /**
     * if n > useGumbel, we'll use the Hall approximation to the Gumbel Distribution 
     */
    int useGumbel = 200;
    
    /**
     * Gets a extreme normal random variable for a zone.  If zone, poolOffset, poolSkip and n
     * are the same it will return the same value. 
     * @param zone the destination zone under consideration
     * @param poolOffset something stored in Preferences to distinguish decision makers
     * @param poolSkip something stored in Preferences to distinguish decision makers
     * @param n how large the population is in the zone
     * @param stdDev the variance of the underlying normal distribution
     * @return the sample from the extreme normal distribution
     */
    public double getExtremeNormal(int zone, int poolOffset, int poolSkip, int n, double stdDev) {
    	if (poolSkip ==0) {
    		logger.warn("poolSkip is zero, setting it to 1");
    		poolSkip = 1;
    	}
        checkPools();
        if (stdDev == 0) return 0;
        if (n<useGumbel) {
            double maxDouble = Double.NEGATIVE_INFINITY;
            for (int i=0;i<n;i++) {
                int place = (poolOffset+(poolSkip+i)*zone) % normalPoolSize;
                maxDouble = Math.max(maxDouble, normalPool[place]);
            }
            return maxDouble*stdDev;
        } else {
            int place = (poolOffset + poolSkip*zone)%uniformPoolSize;
            double uniformRandom = uniformPool[place];
            double logn = Math.log(n);
            double firstApproximation = Math.sqrt(2*logn);
            double correction = 0.5*(Math.log(logn)+log4pi)/firstApproximation;
            double distributionParameter = firstApproximation - correction;
            double standardValue = distributionParameter-Math.log(-Math.log(uniformRandom))/distributionParameter;
            return standardValue*stdDev;
        }
    }

    private void checkPools() {
        if (normalPool == null) {
            normalPool = new double[normalPoolSize];
            for (int i =0; i< normalPoolSize;i++) {
                normalPool[i]= random.nextGaussian();
            }
        }
        if (uniformPool == null) {
            uniformPool = new double[uniformPoolSize];
            for (int i =0; i< uniformPoolSize;i++) {
                uniformPool[i] = random.nextDouble();
            }
        }
    }
    
    static private DestinationRandomTerms globalDestinationRandomTerms;
    static public DestinationRandomTerms getGlobalRandomTerms() {
    	if (globalDestinationRandomTerms == null) {
    		globalDestinationRandomTerms = new DestinationRandomTerms();
    	}
    	return globalDestinationRandomTerms;
    }

	public double getGumbel(int alternativeNumber, int indexIntoArray,
			int skipValue, double dispersionParameter) {
		return GumbelErrorTerm.transformUniformToGumble(getUniform(alternativeNumber, indexIntoArray,skipValue),dispersionParameter);
	}

	private double getNormal(int alternativeNumber, int indexIntoArray,
			int skipValue) {
    	if (skipValue ==0) {
    		logger.warn("poolSkip is zero, setting it to 1");
    		skipValue = 1;
    	}
        checkPools();
        int place = (indexIntoArray + skipValue*alternativeNumber)%normalPoolSize;
        return normalPool[place];
	}

	private double getUniform(int alternativeNumber, int indexIntoArray,
			int skipValue) {
    	if (skipValue ==0) {
    		logger.warn("poolSkip is zero, setting it to 1");
    		skipValue = 1;
    	}
        checkPools();
        int place = (indexIntoArray + skipValue*alternativeNumber)%uniformPoolSize;
        return uniformPool[place];
	}

}
