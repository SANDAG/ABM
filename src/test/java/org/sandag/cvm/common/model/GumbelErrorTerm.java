/**
 * 
 */
package org.sandag.cvm.common.model;

import java.util.Random;

/**
 * This class is used to sample and store Weibull distributed values 
 * @author John Abraham
 *
 */
public class GumbelErrorTerm extends RandomVariable {

    static final double eulersConstant = 0.5772156649015328606;

    /**
     * Static method to sample and return 
     * @param dispersionParameter
     * @return a sample value from the Gumbel distribution with mean 0.
     */
    public static double sample(double dispersionParameter) {
        double sample = Math.random();
        return eulersConstant-dispersionParameter*Math.log(-Math.log(sample));
    }
    
    /**
     * Static method to sample and return 
     * @param dispersionParameter
     * @param r The random number generator to use
     * @return a sample value from the Gumbel distribution with mean 0.
     */
    public static double sample(double dispersionParameter, Random r) {
        double sample = r.nextDouble();
        return eulersConstant-dispersionParameter*Math.log(-Math.log(sample));
    }
    
    private double dispersionParameter;
     
    /**
     * Constructor
     */
    public GumbelErrorTerm(double dispersionParameter) {
        value = 0;
        this.dispersionParameter = dispersionParameter;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
    
    public double getDispersionParameter() {
        return dispersionParameter;
    }

    /** (non-Javadoc)
     * @see org.sandag.cvm.common.model.RandomVariable#sample()
     * Changes the value to a new random term by sampling from the Probability
     * Density Function z*exp(-z)/beta where z=exp(-(x)/beta))
     */
    @Override
    public double sample() {
        double sample = Math.random();
        value = eulersConstant-dispersionParameter*Math.log(-Math.log(sample));
        return value;
    }
    
    public static double transformUniformToGumble(double uniform, double dispersionParameter) {
    	return eulersConstant - dispersionParameter*Math.log(-Math.log(uniform));
    }

    public void setDispersionParameter(double dispersionParameter) {
        this.dispersionParameter = dispersionParameter;
    }

}
