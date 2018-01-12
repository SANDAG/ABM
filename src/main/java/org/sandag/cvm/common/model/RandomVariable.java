package org.sandag.cvm.common.model;

public abstract class RandomVariable implements Cloneable {
    
    double value;
    
    private boolean validValue;

    public RandomVariable() {
        super();
        value = 0;
        validValue = false;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    abstract public double sample();
    
    public double sampleIfNecessaryAndSave() {
        if (validValue) {
            return value;
        }
        value = sample();
        validValue = true;
        return value;
    }
    
    public void setInvalid() {
        validValue = false;
    }
    
}
