/*
 Travel Model Microsimulation library
 Copyright (C) 2005 PbConsult, JE Abraham and others


  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/



package org.sandag.cvm.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Random;

import org.apache.log4j.Logger;

public class LogitModel extends DiscreteChoiceModel implements Alternative {
	
	private static Logger logger = Logger.getLogger(LogitModel.class);

	public void allocateQuantity(double amount) {
        double[] probs = getChoiceProbabilities();
        for (int i=0;i<probs.length;i++) {
            Alternative a = alternativeAt(i);
            if (a instanceof AggregateAlternative) {
                ((AggregateAlternative) a).setAggregateQuantity(amount*probs[i],probs[i]*(1-probs[i])*dispersionParameter);
            }
        }
    }
    
    // was static Random myRandom = new Random(42);
    static Random myRandom = new Random();
    
    /** @param a the alternative to add into the choice set */
    public void addAlternative(Alternative a) {
        alternatives.add(a);
    }

    public LogitModel() {
      alternatives = new ArrayList<Alternative>();
           dispersionParameter = 1.0;
    }
    //use this constructor if you know how many alternatives
    public LogitModel(int numberOfAlternatives) {
         alternatives = new ArrayList<Alternative>(numberOfAlternatives);
           dispersionParameter = 1.0;
    }


    /** @return the composite utility (log sum value) of all the alternatives */
    public double getUtility() {
        double sum = 0;
        int i = 0;
        while (i<alternatives.size()) {
            sum += Math.exp(dispersionParameter * ((Alternative)alternatives.get(i)).getUtility());
            i++;
        }
        double bob = (1 / dispersionParameter) * Math.log(sum);
        if (Double.isNaN(bob)) {
           System.out.println("composite utility is NaN");
        }
        return bob+constantUtility;
    }

    public double getDispersionParameter() { return dispersionParameter; }

    public void setDispersionParameter(double dispersionParameter) { this.dispersionParameter = dispersionParameter; }

    public double[] getChoiceProbabilities() {
        synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            ListIterator<Alternative> it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
                Alternative a = (Alternative) it.next();
                double utility = a.getUtility();
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                  System.out.println("hmm, alternative "+a+" was such that LogitModel weight was NaN");
                  System.out.println("dispersionParameter ="+dispersionParameter+", utility ="+utility);
                  throw new Error("NAN in weight for alternative "+a);
                }
                sum += weights[i];
                i++;
            }
            if (sum!=0) {
                    for (i = 0; i < weights.length; i++) {
                         weights[i] /= sum;
                    }
               }
               return weights;
        }
    }

     public Alternative alternativeAt(int i) { return (Alternative) alternatives.get(i);}// should throw an error if out of range


    /** Picks one of the alternatives based on the logit model probabilities 
     * @throws ChoiceModelOverflowException */
    public Alternative monteCarloChoice() throws NoAlternativeAvailable, ChoiceModelOverflowException {
       // synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            Iterator<Alternative> it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
            	Alternative a = it.next();
                double utility = a.getUtility();
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                  System.out.println("hmm, alternative "+a+" was such that LogitModel weight was NaN");
                  System.out.println("dispersionParameter ="+dispersionParameter+", utility ="+utility);
                }
                sum += weights[i];
                i++;
            }
            if (Double.isInfinite(sum)) {
            	logger .fatal("Overflow error in choice model, list of alternatives follows");
            	it = alternatives.listIterator();
            	while (it.hasNext()) {
            		Alternative a = (Alternative) it.next();
                    double utility = a.getUtility();
                    System.out.println("  U:"+utility+", W:"+Math.exp(dispersionParameter * utility)+" for "+a);
            	}
            	
            	throw new ChoiceModelOverflowException("Infinite weight(s) in logit model choice function");
            }
            if (sum==0) throw new NoAlternativeAvailable();
            double selector = myRandom.nextDouble() * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++) {
                sum += weights[i];
                if (selector <= sum) return (Alternative)alternatives.get(i);
            }
            //yikes!
            System.out.println("Error: problem with logit model. sum is "+sum+", rand is "+selector);
            System.out.println("Alternative,weight");
            for (i=0; i < weights.length; i++){
            	System.out.println((Alternative)alternatives.get(i)+","+weights[i]);
            }
            throw new Error("Random Number Generator in Logit Model didn't return value between 0 and 1");
      //  }
    }
    
    /** Picks one of the alternatives based on the logit model probabilities;
          use this if you want to give method random number */
    public Alternative monteCarloChoice(double randomNumber) throws NoAlternativeAvailable {
        synchronized(alternatives) {
            double[] weights = new double[alternatives.size()];
            double sum = 0;
            Iterator<Alternative> it = alternatives.listIterator();
            int i = 0;
            while (it.hasNext()) {
                double utility = ((Alternative)it.next()).getUtility();
                weights[i] = Math.exp(dispersionParameter * utility);
                if (Double.isNaN(weights[i])) {
                  System.out.println("hmm, alternative was such that LogitModel weight was NaN");
                }
                sum += weights[i];
                i++;
            }
            if (sum==0) throw new NoAlternativeAvailable();
            double selector = randomNumber * sum;
            sum = 0;
            for (i = 0; i < weights.length; i++) {
                sum += weights[i];
                if (selector <= sum) return (Alternative)alternatives.get(i);
            }
            //yikes!
            System.out.println("Error: problem with logit model. sum is "+sum+", rand is "+randomNumber);
            System.out.println("Alternative,weight");
            for (i=0; i < weights.length; i++){
            	System.out.println((Alternative)alternatives.get(i)+","+weights[i]);
            }
            throw new Error("Random Number Generator in Logit Model didn't return value between 0 and 1");
        }
    }


    private double dispersionParameter;
    private double constantUtility=0;
    protected ArrayList<Alternative> alternatives;

    public String toString() {
        StringBuffer altsString = new StringBuffer();
    	int alternativeCounter = 0;
        if (alternatives.size() > 5) { altsString.append("LogitModel with " + alternatives.size() + "alternatives {"); }
        else altsString.append("LogitModel, choice between ");
        Iterator<Alternative> it = alternatives.iterator();
        while (it.hasNext() && alternativeCounter < 5) {
            altsString.append(it.next());
            altsString.append(",");
            alternativeCounter ++;
        }
        if (it.hasNext()) altsString.append("...}"); else altsString.append("}");
        return new String(altsString);
    }

    public double getConstantUtility(){ return constantUtility; }

    public void setConstantUtility(double constantUtility){ this.constantUtility = constantUtility; }

    /**
     * Method arrayCoefficientSimplifiedChoice.
     * @param theCoefficients
     * @param theAttributes
     * @return int
     */
    public static int arrayCoefficientSimplifiedChoice(
        double[][] theCoefficients,
        double[] theAttributes) {

		double[] utilities = new double[theCoefficients.length];    
		int alt;
    	for (alt =0; alt < theCoefficients.length; alt++){
    		utilities[alt] = 0;
    		for (int c=0;c<theAttributes.length;c++) {
    			utilities[alt]+=theCoefficients[alt][c]*theAttributes[c];
    		}
    	}
    	int denominator = 0;
    	for (alt=0;alt<utilities.length;alt++) {
    		utilities[alt] = Math.exp(utilities[alt]);
    		denominator+=utilities[alt];
    	}
    	double selector = Math.random()*denominator;
    	double cumulator = 0;
    	for (alt=0;alt<utilities.length;alt++) {
    		cumulator += utilities[alt];
    		if (selector <= cumulator) return alt;
    	}
        // shouldn't happen
        return utilities.length-1;
    }

    public int numberOfAlternatives() {
        return alternatives.size();
    }

	public Iterator<Alternative> getAlternativesIterator() {
		return alternatives.iterator();
	}

}


