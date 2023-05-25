package org.sandag.cvm.activityTravel.cvm;

import org.sandag.cvm.activityTravel.CoefficientFormatError;
import org.sandag.cvm.activityTravel.DurationModel;

public class DurationModel2 extends DurationModel {
    
    int functionalForm;
    static boolean durationInMinutes = false;

    @Override
    public void addCoefficient(String alternative, String index1, String index2, String matrix, double coefficient) throws CoefficientFormatError {
        if(index1.equals("functionForm")) {
            if (index2.equals("power")) functionalForm =1;
            else if (index2.equals("cubic")) functionalForm = 2;
            else if (index2.equals("exponential")) functionalForm = 3;
            else if (index2.equals("addedexponential")) functionalForm = 0;
            else throw new CoefficientFormatError("functionalForm for tour start model must have index2 as \"power\", \"cubic\" or \"exponential\"");
        } else {
            super.addCoefficient(alternative, index1, index2, matrix, coefficient);
        }
    }

    @Override
    public double sampleValue() {
        double y=0;
        double x = Math.random();
        switch (functionalForm) {
        case 0:
            y = super.sampleValue();
            break;
        case 1:
            y = a * Math.pow(x,b)+c*Math.pow(x,d) + e * x + f;
            break;
        case 2:
            y = a+b*x+c*x*x+d*x*x*x;
            break;
        case 3:
            y = c*Math.exp(a*x+b)+d;
            break;
        default:
            throw new RuntimeException("Functional form for duration model must be 0,1,2 or 3");
        }
        if (durationInMinutes) y=y/60;
        if (y<0) y =0;
        if (y>24) y = 24;
        return y;
    }
}
