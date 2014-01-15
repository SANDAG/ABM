package org.sandag.abm.common;

public abstract class OutboundHalfTourDMU
        extends ConditionalDMU
{
    public int outboundHalfTourDirection;

    protected double getTransitSkimFromMethodName(String methodName) throws Exception
    {
        return getTranistSkimFromMethodConditional(methodName, outboundHalfTourDirection == 1);
    }
}
