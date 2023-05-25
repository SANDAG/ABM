package org.sandag.cvm.common.model;

import java.util.ArrayList;

import org.apache.log4j.Logger;


public class UtilityMaximizingChoiceModelWithErrorTermVector extends
        DiscreteChoiceModel {

    ArrayList<Alternative> alternatives = new ArrayList();
    double[] errorTerms = null;
    RandomVariable myRandomVariable;
    
    private static Logger logger = Logger.getLogger(UtilityMaximizingChoiceModelWithErrorTermVector.class);
    
    public UtilityMaximizingChoiceModelWithErrorTermVector(RandomVariable myRandomVariable) {
        super();
        this.myRandomVariable = myRandomVariable;
    }

    @Override
    public void addAlternative(Alternative a) {
        alternatives.add(a);
        errorTerms = null;
    }

    @Override
    public void allocateQuantity(double amount) {
        String msg = this.getClass().toString()+" can't allocate quantity amongst alternatives -- it is only for simulation";
        logger.fatal(msg);
        throw new RuntimeException(msg);
    }

    @Override
    public Alternative alternativeAt(int i) {
        return alternatives.get(i);
    }

    @Override
    public double[] getChoiceProbabilities() {
        String msg = this.getClass().toString()+" can't allocate quantity amongst alternatives -- it is only for simulation";
        logger.fatal(msg);
        throw new RuntimeException(msg);
    }

    @Override
    public Alternative monteCarloChoice() throws NoAlternativeAvailable {
        if (errorTerms !=null) {
            if (errorTerms.length != alternatives.size()) {
                errorTerms = null;
            }
        }
        if (errorTerms ==null) {
            errorTerms = new double[alternatives.size()];
            for (int i =0;i<errorTerms.length;i++) {
                errorTerms[i] = myRandomVariable.sample(); 
            }
        }
        return monteCarloChoice(errorTerms);
    }

    public Alternative monteCarloChoice(double[] errorTerms) {
        this.errorTerms = errorTerms; 
        int maxAlternative = 0;
        double maxUtility = Double.NEGATIVE_INFINITY;
        for (int i=0;i<alternatives.size();i++) {
            double utility = alternatives.get(i).getUtility()+errorTerms[i];
            if (utility>maxUtility) {
                maxUtility = utility;
                maxAlternative=i;
            }
        }
        return alternatives.get(maxAlternative);
    }

    @Override
    public Alternative monteCarloChoice(double r) throws NoAlternativeAvailable {
        String msg = this.getClass().toString()+" can't take a random number parameter";
        logger.fatal(msg);
        throw new RuntimeException(msg);
    }

}
