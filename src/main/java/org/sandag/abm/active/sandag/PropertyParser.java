package org.sandag.abm.active.sandag;

import java.util.*;

public class PropertyParser
{
    public Map<String,String> propertyMap;
    
    public PropertyParser(Map<String,String> propertyMap)
    {
        this.propertyMap = propertyMap;
    }
    
    public List<String> parseStringPropertyList(String property)
    {
        return Arrays.asList(propertyMap.get(property.trim()).split("\\s*,\\s*"));
    }
    
    public List<Float> parseFloatPropertyList(String property)
    {
        List<String> stringList = Arrays.asList(propertyMap.get(property).split("\\s*,\\s*"));
        List<Float> floatList = new ArrayList<Float>();
        for (String str: stringList) {
            floatList.add(Float.parseFloat(str));
        }
        return floatList;
    }
    
    public List<Integer> parseIntPropertyList(String property)
    {
        List<String> stringList = Arrays.asList(propertyMap.get(property).split("\\s*,\\s*"));
        List<Integer> intList = new ArrayList<Integer>();
        for (String str: stringList) {
            intList.add(Integer.parseInt(str));
        }
        return intList;
    }
    
    public Map<String,String> mapStringPropertyListToStrings(String keyProperty, String stringValueProperty)
    {
        Map<String,String> map = new HashMap<String,String>();
        List<String> keys = parseStringPropertyList(keyProperty);
        List<String> values = parseStringPropertyList(stringValueProperty);
        for (int i=0; i<keys.size(); i+=1) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
    
    public Map<String,Float> mapStringPropertyListToFloats(String keyProperty, String stringValueProperty)
    {
        Map<String,Float> map = new HashMap<String,Float>();
        List<String> keys = parseStringPropertyList(keyProperty);
        List<Float> values = parseFloatPropertyList(stringValueProperty);
        for (int i=0; i<keys.size(); i+=1) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
    
    private boolean isIntValueIn(int value,List<Integer> referenceValues)
    {  
        for (int v : referenceValues) {
            if ( v == value ) {
                return true;
            }
        }  
        return false;
    }
    
    public boolean isIntValueInPropertyList(int value,String property)
    {
        List<Integer> referenceValues = parseIntPropertyList(property);
        return isIntValueIn(value, referenceValues);
    }
}
