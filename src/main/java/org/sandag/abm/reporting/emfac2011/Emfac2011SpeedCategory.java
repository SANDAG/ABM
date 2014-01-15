package org.sandag.abm.reporting.emfac2011;

/**
 * The {@code EmfacSpeedClass} enum represents the speed categories used in the
 * EMFAC2011 model.
 * 
 * @author crf Started 2/9/12 5:58 AM
 */
public enum Emfac2011SpeedCategory
{
    SPEED_0_5(5, "5MPH"), SPEED_5_10(10, "10MPH"), SPEED_10_15(15, "15MPH"), SPEED_15_20(20,
            "20MPH"), SPEED_20_25(25, "25MPH"), SPEED_25_30(30, "30MPH"), SPEED_30_35(35, "35MPH"), SPEED_35_40(
            40, "40MPH"), SPEED_40_45(45, "45MPH"), SPEED_45_50(50, "50MPH"), SPEED_50_55(55,
            "55MPH"), SPEED_55_60(60, "60MPH"), SPEED_60_65(65, "65MPH"), SPEED_65_70plus(1000,
            "70MPH"); // big upper bound

    private final String name;
    private final double upperBound;

    private Emfac2011SpeedCategory(double upperBound, String name)
    {
        this.upperBound = upperBound;
        this.name = name;
    }

    /**
     * Get the EMFAC name for this speed category.
     * 
     * @return this speed category's name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Get the {@code SpeedCategory} for a given speed.
     * 
     * @param speed
     *            The speed in question.
     * 
     * @return the {@code SpeedCategory} for the given speed.
     * 
     * @throws IllegalArgumentException
     *             if {@code speed} < 0.
     */
    public static Emfac2011SpeedCategory getSpeedCategory(double speed)
    {
        if (speed < 0)
            throw new IllegalArgumentException("Negative speeds are not allowed: " + speed);
        for (Emfac2011SpeedCategory sc : values())
            if (speed <= sc.upperBound) return sc;
        throw new IllegalStateException("Couldn't find speed category for " + speed);
    }

    /**
     * Get the {@code SpeedCategory} corresponding to an EMFAC name.
     * 
     * @param name
     *            The EMFAC speed category name.
     * 
     * @return the {@code SpeedCategory} corresponding to {@code name}.
     * 
     * @throws IllegalArgumentException
     *             if {@code name} is not a valid EMFAC speed category name.
     */
    public static Emfac2011SpeedCategory getTypeForName(String name)
    {
        for (Emfac2011SpeedCategory type : values())
            if (type.name.equals(name)) return type;
        throw new IllegalArgumentException("Type for name not found: " + name);
    }
}
