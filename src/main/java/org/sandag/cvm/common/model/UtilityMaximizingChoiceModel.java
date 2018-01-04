package org.sandag.cvm.common.model;

import java.util.ArrayList;

import org.apache.log4j.Logger;


public class UtilityMaximizingChoiceModel extends
        DiscreteChoiceModel {

    ArrayList<Alternative> alternatives = new ArrayList();
    
    private static Logger logger = Logger.getLogger(UtilityMaximizingChoiceModel.class);
    
    public UtilityMaximizingChoiceModel(RandomVariable myRandomVariable) {
        super();
    }

    @Override
    public void addAlternative(Alternative a) {
        alternatives.add(a);
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


    public Alternative monteCarloChoice() {
        int maxAlternative = 0;
        double maxUtility = Double.NEGATIVE_INFINITY;
        for (int i=0;i<alternatives.size();i++) {
            double utility = alternatives.get(i).getUtility();
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
