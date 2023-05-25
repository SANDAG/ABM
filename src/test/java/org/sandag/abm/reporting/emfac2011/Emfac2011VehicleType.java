package org.sandag.abm.reporting.emfac2011;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The {@code EmfacVehicleType} enum represents the EMFAC2011 tNCVehicle types.
 * 
 * @author crf Started 2/8/12 8:54 AM
 */
public enum Emfac2011VehicleType
{
    OTHER_BUSES_DSL("All Other Buses - DSL", "OBUS - DSL", false), LDA_DSL("LDA - DSL",
            "LDA - DSL", true), LDA_GAS("LDA - GAS", "LDA - GAS", true), LDT1_DSL("LDT1 - DSL",
            "LDT1 - DSL", true), LDT1_GAS("LDT1 - GAS", "LDT1 - GAS", true), LDT2_DSL("LDT2 - DSL",
            "LDT2 - DSL", true), LDT2_GAS("LDT2 - GAS", "LDT2 - GAS", true), LHD1_DSL("LHD1 - DSL",
            "LHDT1 - DSL", true), LHE1_GAS("LHD1 - GAS", "LHDT1 - GAS", true), LHD2_DSL(
            "LHD2 - DSL", "LHDT2 - DSL", true), LHD2_GAS("LHD2 - GAS", "LHDT2 - GAS", true), MCY_GAS(
            "MCY - GAS", "MCY - GAS", true), MDV_DSL("MDV - DSL", "MDV - DSL", true), MDV_GAS(
            "MDV - GAS", "MDV - GAS", true), MH_DSL("MH - DSL", "MH - DSL", false), MH_GAS(
            "MH - GAS", "MH - GAS", false), MOTOR_COACH_DSL("Motor Coach - DSL", "OBUS - DSL",
            false), OBUS_GAS("OBUS - GAS", "OBUS - GAS", false), PTO_DSL("PTO - DSL", "HHDT - DSL",
            false), SBUS_DSL("SBUS - DSL", "SBUS - DSL", false), SBUS_GAS("SBUS - GAS",
            "SBUS - GAS", false), T6_AG_DSL("T6 Ag - DSL", "MHDT - DSL", false), T6_CAIRP_HEAVY_DSL(
            "T6 CAIRP heavy - DSL", "MHDT - DSL", false), T6_CAIRP_SMALL_DSL(
            "T6 CAIRP small - DSL", "MHDT - DSL", false), T6_INSTATE_CONSTRUCTION_HEAVY_DSL(
            "T6 instate construction heavy - DSL", "MHDT - DSL", false), T6_INSTATE_CONSTRUCTION_SMALL_DSL(
            "T6 instate construction small - DSL", "MHDT - DSL", false), T6_INSTATE_HEAVY_DSL(
            "T6 instate heavy - DSL", "MHDT - DSL", false), T6_INSTATE_SMALL_DSL(
            "T6 instate small - DSL", "MHDT - DSL", false), T6_OOS_HEAVY_DSL("T6 OOS heavy - DSL",
            "MHDT - DSL", false), T6_OOS_SMALL_DSL("T6 OOS small - DSL", "MHDT - DSL", false), T6_PUBLIC_DSL(
            "T6 public - DSL", "MHDT - DSL", false), T6_UTILITY_DSL("T6 utility - DSL",
            "MHDT - DSL", false), T6TS_GAS("T6TS - GAS", "MHDT - GAS", false), T7_AG_DSL(
            "T7 Ag - DSL", "HHDT - DSL", false), T7_CAIRP_DSL("T7 CAIRP - DSL", "HHDT - DSL", false), T7_CAIRP_CONSTRUCTION_DSL(
            "T7 CAIRP construction - DSL", "HHDT - DSL", false), T7_NNOOS_DSL("T7 NNOOS - DSL",
            "HHDT - DSL", false), T7_NOOS_DSL("T7 NOOS - DSL", "HHDT - DSL", false), T7_OTHER_PORT_DSL(
            "T7 other port - DSL", "HHDT - DSL", false), T7_POAK_DSL("T7 POAK - DSL", "HHDT - DSL",
            false), T7_POLA_DSL("T7 POLA - DSL", "HHDT - DSL", false), T7_PUBLIC_DSL(
            "T7 public - DSL", "HHDT - DSL", false), T7_SINGLE_DSL("T7 Single - DSL", "HHDT - DSL",
            false), T7_SINGLE_CONSTRUCTION_DSL("T7 single construction - DSL", "HHDT - DSL", false), T7_SWCV_DSL(
            "T7 SWCV - DSL", "HHDT - DSL", false), T7_TRACTOR_DSL("T7 tractor - DSL", "HHDT - DSL",
            false), T7_TRACTOR_CONSTRUCTION_DSL("T7 tractor construction - DSL", "HHDT - DSL",
            false), T7_UTILITY_DSL("T7 utility - DSL", "HHDT - DSL", false), T7IS_GAS("T7IS - GAS",
            "HHDT - GAS", false), UBUS_DSL("UBUS - DSL", "UBUS - DSL", false), UBUS_GAS(
            "UBUS - GAS", "UBUS - GAS", false);

    private final String  name;
    private final String  emfac2007Name;
    private final boolean isMutableType;

    private Emfac2011VehicleType(String name, String emfac2007Name, boolean isMutableType)
    {
        this.name = name;
        this.emfac2007Name = emfac2007Name;
        this.isMutableType = isMutableType;
    }

    /**
     * Get the name of the tNCVehicle type. This is the name used by the EMFAC2011
     * model.
     * 
     * @return the name of the tNCVehicle type.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get this tNCVehicle type's equivalent EMFAC2007 tNCVehicle type name.
     * 
     * @return the EMFAC2007 tNCVehicle type corresponding to this tNCVehicle type.
     */
    public String getEmfac2007Name()
    {
        return emfac2007Name;
    }

    /**
     * Determine if this tNCVehicle type is mutable. If a tNCVehicle type is mutable,
     * then its EMFAC2011 SG inputs may be adjusted to account for travel demand
     * model results.
     * 
     * @return {@code true} if this tNCVehicle type is mutable, {@code false} if
     *         not.
     */
    public boolean isMutableType()
    {
        return mutableTypes.contains(this);
    }

    private static Set<Emfac2011VehicleType> mutableTypes;

    static
    {
        Set<Emfac2011VehicleType> set = EnumSet.noneOf(Emfac2011VehicleType.class);
        for (Emfac2011VehicleType type : Emfac2011VehicleType.values())
            if (type.isMutableType) set.add(type);
        setMutableTypes(set);
    }

    /**
     * Set the mutable tNCVehicle types. This need only be called if the default
     * mutable types are unsatisfactory for the particular application; however,
     * if it needs to be called, then it should be before any of the
     * EMFAC2011/Aquavis processing commences.
     * 
     * @param mutableTypes
     *            The set of mutable tNCVehicle types for processing.
     */
    public static void setMutableTypes(Set<Emfac2011VehicleType> mutableTypes)
    {
        Emfac2011VehicleType.mutableTypes = Collections.unmodifiableSet(mutableTypes);
    }

    /**
     * Get the tNCVehicle type corresponding to the given (EMFAC2011) name.
     * 
     * @param name
     *            The tNCVehicle type name used in the EMFAC2011 tNCVehicle type.
     * 
     * @return the tNCVehicle type corresponding to {@code name}.
     * 
     * @throws IllegalArgumentException
     *             if {@code name} does not correspond to any tNCVehicle type.
     */
    public static Emfac2011VehicleType getVehicleType(String name)
    {
        for (Emfac2011VehicleType type : values())
            if (type.getName().equals(name)) return type;
        throw new IllegalArgumentException("No EMFAC tNCVehicle type corresponding to: " + name);
    }

    /**
     * Get the set of EMFAC2011 tNCVehicle types which are mutable. If a tNCVehicle
     * type is mutable, then its EMFAC2011 SG inputs may be adjusted to account
     * for travel demand model results.
     * 
     * @return a set holding the mutable EMFAC2011 tNCVehicle types.
     */
    public static Set<Emfac2011VehicleType> getMutableVehicleTypes()
    {
        return mutableTypes;
    }
}
