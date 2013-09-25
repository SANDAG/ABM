package org.sandag.abm.active;
import java.util.*;

public enum TurnType
{
    NONE(0),
    LEFT(1),
    RIGHT(2),
    REVERSAL(3);
    
    private int key;
    private static Map<Integer, TurnType> map = new HashMap<Integer, TurnType>();
    
    static {
        for (TurnType t : TurnType.values()) {
            map.put(t.key, t);
        }
    }
    
    private TurnType(final int key) { this.key = key; }
    
    public static TurnType valueOf(int key)
    {
        return map.get(key);
    }
    
    public int getKey()
    {
        return key;
    }
}
