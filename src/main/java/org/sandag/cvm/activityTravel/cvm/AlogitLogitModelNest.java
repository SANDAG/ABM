package org.sandag.cvm.activityTravel.cvm;

import org.sandag.cvm.common.model.LogitModel;

public class AlogitLogitModelNest extends LogitModel {
    
    double nestingCoefficient = 1.0;

    public void setAlogitNestingCoefficient(double coefficient) {
        nestingCoefficient = coefficient;
    }

    @Override
    public double getUtility() {
        if (getDispersionParameter()!=1.0) {
            throw new RuntimeException("Alogit nesting always needs a dispersion parameter of 1.0 in the lower level nests");
        }
        return super.getUtility()*nestingCoefficient;
    }

}
