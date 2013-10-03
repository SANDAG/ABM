package org.sandag.abm.ctramp;

/**
 * Created by IntelliJ IDEA. User: Jim Date: Jul 1, 2008 Time: 9:58:21 AM
 * 
 * Interface for accessing zonal information used by CT-RAMP modules
 */
public interface TazDataIf
{

    String testRemote();

    int[] getAltToZoneArray();

    int[] getAltToSubZoneArray();

    int[] getIndexToZoneArray();

    int[] getZoneTableRowArray();

    int getZoneIsCbd(int taz);

    int getZoneIsUrban(int taz);

    int getZoneIsSuburban(int taz);

    int getZoneIsRural(int taz);

    float[] getPkAutoRetailAccessibity();

    float[] getPkAutoTotalAccessibity();

    float[] getPkTransitRetailAccessibity();

    float[] getPkTransitTotalAccessibity();

    float[] getOpAutoRetailAccessibity();

    float[] getOpAutoTotalAccessibity();

    float[] getOpTransitRetailAccessibity();

    float[] getOpTransitTotalAccessibity();

    float[] getNonMotorizedRetailAccessibity();

    float[] getNonMotorizedTotalAccessibity();

    /**
     * 
     * @param field
     *            is the field name to be checked against the column names in
     *            the zone data table.
     * @return true if field matches one of the zone data table column names,
     *         otherwise false.
     */
    boolean isValidZoneTableField(String field);

    /**
     * @return a String[] of the column labels in the zone data table
     */
    String[] getZoneDataTableColumnLabels();

    /**
     * @return an int value for the number of zones, i.e. rows in the zone data
     *         table
     */
    int getNumberOfZones();

    /**
     * @return an int value for the number of subZones, i.e. number of
     *         walkTransit accessible segments defined in model for zones.
     *         Typical value might be 3, "no walk access", "short walk access",
     *         "long walk access".
     */
    int getNumberOfSubZones();

    /**
     * @return a String[] for the subZone names, e.g. "no walk access",
     *         "short walk access", "long walk access".
     */
    String[] getSubZoneNames();

    /**
     * @param taz
     *            is the taz index for the zonalWalkPctArray which is
     *            dimensioned to ZONES+1, assuming taz index values range from 1
     *            to NUM_ZONES.
     * @return a double[], dimensioned to NUM_SIBZONES, with the subzone
     *         proportions for the TAZ passed in
     */
    double[] getZonalWalkPercentagesForTaz(int taz);

    /**
     * @param taz
     *            is the taz index for the zone data table which is dimensioned
     *            to ZONES+1, assuming taz index values range from 1 to
     *            NUM_ZONES.
     * @param fieldName
     *            is the column label in the zone data table.
     * @return a float value from the zone data table at the specified row index
     *         and column label.
     */
    float getZoneTableValue(int taz, String fieldName);

    int[] getZoneTableIntColumn(String fieldName);

    float[] getZoneTableFloatColumn(String fieldName);

    /**
     * @param tableRowNumber
     *            is the zone table row number
     * @return zone number for the table row.
     */
    int getTazNumber(int tableRowNumber);

    /**
     * @return area type from the zone data table for the zone index.
     */
    int[] getZonalAreaType();

    /**
     * @return district from the zone data table for the zone index.
     */
    int[] getZonalDistrict();

    /**
     * @return integer county value from the zone data table for the zone index.
     */
    int[] getZonalCounty();

    /**
     * @return the parking rate array
     */
    float[] getZonalParkRate();

    /**
     * @return the proportion of free parking array
     */
    float[] getZonalPropFree();

    /**
     * @return the number of long parking spots array
     */
    int[] getZonalParkLong();

    /**
     * @return the number of parking spots array
     */
    int[] getZonalParkTot();

}
