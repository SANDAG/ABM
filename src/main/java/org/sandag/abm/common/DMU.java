package org.sandag.abm.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import com.pb.common.calculator.IndexValues;

public abstract class DMU
{
    protected transient Logger         logger       = null;

    protected static final int         LB           = McLogsumsCalculator.LB;
    protected static final int         EB           = McLogsumsCalculator.EB;
    protected static final int         BRT          = McLogsumsCalculator.BRT;
    protected static final int         LR           = McLogsumsCalculator.LR;
    protected static final int         CR           = McLogsumsCalculator.CR;
    protected static final int         NUM_LOC_PREM = McLogsumsCalculator.NUM_LOC_PREM;

    protected static final int         WTW          = McLogsumsCalculator.WTW;
    protected static final int         WTD          = McLogsumsCalculator.WTD;
    protected static final int         DTW          = McLogsumsCalculator.DTW;
    protected static final int         NUM_ACC_EGR  = McLogsumsCalculator.NUM_ACC_EGR;

    protected static final int         LB_IVT       = McLogsumsCalculator.LB_IVT;
    protected static final int         EB_IVT       = McLogsumsCalculator.EB_IVT;
    protected static final int         BRT_IVT      = McLogsumsCalculator.BRT_IVT;
    protected static final int         LR_IVT       = McLogsumsCalculator.LR_IVT;
    protected static final int         CR_IVT       = McLogsumsCalculator.CR_IVT;
    protected static final int         ACC          = McLogsumsCalculator.ACC;
    protected static final int         EGR          = McLogsumsCalculator.EGR;
    protected static final int         AUX          = McLogsumsCalculator.AUX;
    protected static final int         FWAIT        = McLogsumsCalculator.FWAIT;
    protected static final int         XWAIT        = McLogsumsCalculator.XWAIT;
    protected static final int         FARE         = McLogsumsCalculator.FARE;
    protected static final int         XFERS        = McLogsumsCalculator.XFERS;
    protected static final int         NUM_SKIMS    = McLogsumsCalculator.NUM_SKIMS;

    protected static final int         OUT          = McLogsumsCalculator.OUT;
    protected static final int         IN           = McLogsumsCalculator.IN;
    protected static final int         NUM_DIR      = McLogsumsCalculator.NUM_DIR;

    protected IndexValues              dmuIndex;

    protected HashMap<String, Integer> methodIndexMap;
    protected HashMap<Integer, String> reverseMethodIndexMap;

    protected void CreateReverseMap()
    {
        reverseMethodIndexMap = new HashMap<Integer, String>();
        Iterator<String> i = methodIndexMap.keySet().iterator();
        while (i.hasNext())
        {
            String value = i.next();
            Integer key = methodIndexMap.get(value);
            reverseMethodIndexMap.put(key, value);
        }

    }

    protected double getValueForIndexLookup(int variableIndex, int arrayIndex)
    {
        if (variableIndex < 100)
        {
            try
            {
                Method method = this.getClass().getMethod(reverseMethodIndexMap.get(variableIndex));
                Object o = method.invoke(this);
                if (o instanceof Double) return (double) o;
                else if (o instanceof Float) return ((Float) o).doubleValue();
                else if (o instanceof Integer) return ((Integer) o).doubleValue();
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e)
            {
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");
            }
        }

        try
        {
            return getTransitSkimFromMethodName(reverseMethodIndexMap.get(variableIndex));
        } catch (Exception e)
        {
            logger.error("method number = " + variableIndex + " not found");
            throw new RuntimeException("method number = " + variableIndex + " not found");
        }
    }

    protected abstract double getTransitSkimFromMethodName(String string) throws Exception;
}
