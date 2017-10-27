package org.sandag.abm.modechoice;

import java.io.Serializable;

/**
 * This class is used for specifying modes.
 * 
 * @author Christi Willison
 * @version Sep 8, 2008
 *          <p/>
 *          Created by IntelliJ IDEA.
 */
public final class Modes
        implements Serializable
{

    public enum AutoMode
    {
        DRIVE_ALONE_TOLL("dat"), DRIVE_ALONE_NONTOLL("dan"), TWO_NONTOLL("2nt"), TWO_TOLL(
                "2t"), THREEPLUS_NONTOLL("3+nt"), THREEPLUS_TOLL(
                "3+t");

        private final String name;

        AutoMode(String s)
        {
            this.name = s;
        }

        public AutoMode[] getAutoModes()
        {
            return AutoMode.values();
        }

        public String toString()
        {
            return name;
        }
    }

    public enum TransitMode
    {
        COMMUTER_RAIL("cr", true), // label and true = premium
        LIGHT_RAIL("lr", true), BRT("brt", true), EXPRESS_BUS("eb", true), LOCAL_BUS("lb", false);

        private final String  name;
        private final boolean premium;

        TransitMode(String name, boolean premium)
        {
            this.name = name;
            this.premium = premium;
        }

        public TransitMode[] getTransitModes()
        {
            return TransitMode.values();
        }

        public boolean isPremiumMode(TransitMode transitMode)
        {
            return transitMode.premium;
        }

        public String toString()
        {
            return name;
        }

    }

    public enum AccessMode
    {
        WALK("WLK"), PARK_N_RIDE("PNR"), KISS_N_RIDE("KNR");
        private final String name;

        AccessMode(String name)
        {
            this.name = name;
        }

        public AccessMode[] getAccessModes()
        {
            return AccessMode.values();
        }

        public String toString()
        {
            return name;
        }

    }

    public enum NonMotorizedMode
    {
        WALK, BIKE
    }

    public enum OtherMode
    {
        SCHOOL_BUS
    }

    private Modes()
    {
        // Not implemented in utility classes
    }

    public static int getTransitModeIndex(String modeName)
    {
        if (modeName.equals("cr") || modeName.equals("CR"))
            return TransitMode.COMMUTER_RAIL.ordinal();
        if (modeName.equals("lr") || modeName.equals("LR"))
            return TransitMode.LIGHT_RAIL.ordinal();
        if (modeName.equals("brt") || modeName.equals("BRT")) return TransitMode.BRT.ordinal();
        if (modeName.equals("eb") || modeName.equals("EB"))
            return TransitMode.EXPRESS_BUS.ordinal();
        if (modeName.equals("lb") || modeName.equals("LB")) return TransitMode.LOCAL_BUS.ordinal();
        return -1;
    }

    public static Boolean getIsPremiumTransit(int modeIndex)
    {
        if (modeIndex == TransitMode.COMMUTER_RAIL.ordinal()) return TransitMode.COMMUTER_RAIL.premium;
        else if (modeIndex == TransitMode.LIGHT_RAIL.ordinal()) return TransitMode.LIGHT_RAIL.premium;
        else if (modeIndex == TransitMode.BRT.ordinal()) return TransitMode.BRT.premium;
        else if (modeIndex == TransitMode.EXPRESS_BUS.ordinal()) return TransitMode.EXPRESS_BUS.premium;
        else if (modeIndex == TransitMode.LOCAL_BUS.ordinal()) return TransitMode.LOCAL_BUS.premium;
        else return null;
    }

    public static void main(String[] args)
    {
        System.out.println(AutoMode.DRIVE_ALONE_NONTOLL.toString());

    }
}
