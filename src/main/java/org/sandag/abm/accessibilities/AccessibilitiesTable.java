package org.sandag.abm.accessibilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.pb.common.datafile.CSVFileWriter;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

/**
 * This class holds the accessibility table that is built, or reads it from a
 * previously written file.
 * 
 * @author Jim Hicks
 * @version May, 2011
 */
public final class AccessibilitiesTable
        implements Serializable
{

    protected transient Logger logger                                           = Logger.getLogger(AccessibilitiesTable.class);

    private static final int   NONMANDATORY_AUTO_ACCESSIBILITY_FIELD_NUMBER     = 1;
    private static final int   NONMANDATORY_TRANSIT_ACCESSIBILITY_FIELD_NUMBER  = 2;
    private static final int   NONMANDATORY_NONMOTOR_ACCESSIBILITY_FIELD_NUMBER = 3;
    private static final int   NONMANDATORY_SOV_0_ACCESSIBILITY_FIELD_NUMBER    = 4;
    private static final int   NONMANDATORY_SOV_1_ACCESSIBILITY_FIELD_NUMBER    = 5;
    private static final int   NONMANDATORY_SOV_2_ACCESSIBILITY_FIELD_NUMBER    = 6;
    private static final int   NONMANDATORY_HOV_0_ACCESSIBILITY_FIELD_NUMBER    = 7;
    private static final int   NONMANDATORY_HOV_1_ACCESSIBILITY_FIELD_NUMBER    = 8;
    private static final int   NONMANDATORY_HOV_2_ACCESSIBILITY_FIELD_NUMBER    = 9;
    private static final int   SHOP_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX        = 10;
    private static final int   SHOP_ACCESSIBILITY_HOV_SUFFICIENT_INDEX          = 11;
    private static final int   SHOP_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX      = 12;
    private static final int   MAINT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX       = 13;
    private static final int   MAINT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX         = 14;
    private static final int   MAINT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX     = 15;
    private static final int   EAT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX         = 16;
    private static final int   EAT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX           = 17;
    private static final int   EAT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX       = 18;
    private static final int   VISIT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX       = 19;
    private static final int   VISIT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX         = 20;
    private static final int   VISIT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX     = 21;
    private static final int   DISCR_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX       = 22;
    private static final int   DISCR_ACCESSIBILITY_HOV_SUFFICIENT_INDEX         = 23;
    private static final int   DISCR_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX     = 24;
    private static final int   ESCORT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX      = 25;
    private static final int   ESCORT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX        = 26;
    private static final int   ESCORT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX    = 27;
    private static final int   SHOP_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX        = 28;
    private static final int   SHOP_ACCESSIBILITY_SOV_SUFFICIENT_INDEX          = 29;
    private static final int   SHOP_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX      = 30;
    private static final int   MAINT_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX       = 31;
    private static final int   MAINT_ACCESSIBILITY_SOV_SUFFICIENT_INDEX         = 32;
    private static final int   MAINT_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX     = 33;
    private static final int   EAT_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX         = 34;
    private static final int   EAT_ACCESSIBILITY_SOV_SUFFICIENT_INDEX           = 35;
    private static final int   EAT_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX       = 36;
    private static final int   VISIT_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX       = 37;
    private static final int   VISIT_ACCESSIBILITY_SOV_SUFFICIENT_INDEX         = 38;
    private static final int   VISIT_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX     = 39;
    private static final int   DISCR_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX       = 40;
    private static final int   DISCR_ACCESSIBILITY_SOV_SUFFICIENT_INDEX         = 41;
    private static final int   DISCR_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX     = 42;
    private static final int   ATWORK_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX      = 43;
    private static final int   ATWORK_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX    = 44;
    private static final int   TOTAL_EMPLOYMENT_ACCESSIBILITY_INDEX             = 45;
    private static final int   ATWORK_ACCESSIBILITY_NMOT_INDEX                  = 46;
    private static final int   ALLHH_ACCESSIBILITY_TRANSIT_INDEX                = 47;
    private static final int   NONMANDATORY_MAAS_ACCESSIBILITY_FIELD_NUMBER     = 48;    

    // accessibilities by mgra, accessibility alternative
    private float[][]          accessibilities;

    /**
     * array of previously computed accessibilities
     * 
     * @param computedAccessibilities
     *            array of accessibilities
     * 
     *            use this constructor if the accessibilities were calculated as
     *            opposed to read from a file.
     */
    public AccessibilitiesTable(float[][] computedAccessibilities)
    {
        accessibilities = computedAccessibilities;
    }

    /**
     * file name for store accessibilities
     * 
     * @param accessibilitiesInputFileName
     *            path and filename of file to read
     * 
     *            use this constructor if the accessibilities are to be read
     *            from a file.
     */
    public AccessibilitiesTable(String accessibilitiesInputFileName)
    {
        readAccessibilityTableFromFile(accessibilitiesInputFileName);
    }

    private void readAccessibilityTableFromFile(String fileName)
    {

        File accFile = new File(fileName);

        // read in the csv table
        TableDataSet accTable;
        try
        {
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            accTable = reader.readFile(accFile);

        } catch (Exception e)
        {
            logger.fatal(String
                    .format("Exception occurred reading accessibility data file: %s into TableDataSet object.",
                            fileName));
            throw new RuntimeException();
        }

        // create accessibilities array as a 1-based array
        float[][] temp = accTable.getValues();
        accessibilities = new float[temp.length + 1][];
        for (int i = 0; i < temp.length; i++)
        {
            accessibilities[i + 1] = new float[temp[i].length];
            for (int j = 0; j < temp[i].length; j++)
            {
                accessibilities[i + 1][j] = temp[i][j];
            }
        }

    }

    public void writeAccessibilityTableToFile(String accFileName)
    {

        File accFile = new File(accFileName);

        // the accessibilities array is indexed by mgra values which might no be
        // consecutive.
        // create an arraylist of data table rows, with the last field being the
        // mgra value,
        // convert to a tabledataset, then write to a csv file.

        ArrayList<String> dataColumnHeadings = new ArrayList<String>();
        dataColumnHeadings.add("NONMAN_AUTO");
        dataColumnHeadings.add("NONMAN_TRANSIT");
        dataColumnHeadings.add("NONMAN_NONMOTOR");
        dataColumnHeadings.add("NONMAN_SOV_0");
        dataColumnHeadings.add("NONMAN_SOV_1");
        dataColumnHeadings.add("NONMAN_SOV_2");
        dataColumnHeadings.add("NONMAN_HOV_0");
        dataColumnHeadings.add("NONMAN_HOV_1");
        dataColumnHeadings.add("NONMAN_HOV_2");
        dataColumnHeadings.add("SHOP_HOV_0");
        dataColumnHeadings.add("SHOP_HOV_1");
        dataColumnHeadings.add("SHOP_HOV_2");
        dataColumnHeadings.add("MAINT_HOV_0");
        dataColumnHeadings.add("MAINT_HOV_1");
        dataColumnHeadings.add("MAINT_HOV_2");
        dataColumnHeadings.add("EAT_HOV_0");
        dataColumnHeadings.add("EAT_HOV_1");
        dataColumnHeadings.add("EAT_HOV_2");
        dataColumnHeadings.add("VISIT_HOV_0");
        dataColumnHeadings.add("VISIT_HOV_1");
        dataColumnHeadings.add("VISIT_HOV_2");
        dataColumnHeadings.add("DISCR_HOV_0");
        dataColumnHeadings.add("DISCR_HOV_1");
        dataColumnHeadings.add("DISCR_HOV_2");
        dataColumnHeadings.add("ESCORT_HOV_0");
        dataColumnHeadings.add("ESCORT_HOV_1");
        dataColumnHeadings.add("ESCORT_HOV_2");
        dataColumnHeadings.add("SHOP_SOV_0");
        dataColumnHeadings.add("SHOP_SOV_1");
        dataColumnHeadings.add("SHOP_SOV_2");
        dataColumnHeadings.add("MAINT_SOV_0");
        dataColumnHeadings.add("MAINT_SOV_1");
        dataColumnHeadings.add("MAINT_SOV_2");
        dataColumnHeadings.add("EAT_SOV_0");
        dataColumnHeadings.add("EAT_SOV_1");
        dataColumnHeadings.add("EAT_SOV_2");
        dataColumnHeadings.add("VISIT_SOV_0");
        dataColumnHeadings.add("VISIT_SOV_1");
        dataColumnHeadings.add("VISIT_SOV_2");
        dataColumnHeadings.add("DISCR_SOV_0");
        dataColumnHeadings.add("DISCR_SOV_1");
        dataColumnHeadings.add("DISCR_SOV_2");
        dataColumnHeadings.add("ATWORK_SOV_0");
        dataColumnHeadings.add("ATWORK_SOV_2");
        dataColumnHeadings.add("TOTAL_EMP");
        dataColumnHeadings.add("ATWORK_NM");
        dataColumnHeadings.add("ALL_HHS_TRANSIT");
        dataColumnHeadings.add("NONMAN_MAAS");
        dataColumnHeadings.add("MGRA");

        // copy accessibilities array into a 0-based array
        float[][] dataTableValues = new float[accessibilities.length - 1][];
        for (int r = 1; r < accessibilities.length; r++)
        {
            dataTableValues[r - 1] = new float[accessibilities[r].length];
            for (int c = 0; c < accessibilities[r].length; c++)
            {
                dataTableValues[r - 1][c] = accessibilities[r][c];
            }
        }

        TableDataSet accData = TableDataSet.create(dataTableValues, dataColumnHeadings);
        CSVFileWriter csv = new CSVFileWriter();
        try
        {
            csv.writeFile(accData, accFile);
        } catch (IOException e)
        {
            logger.error("Error trying to write accessiblities data file " + accFileName);
            throw new RuntimeException(e);
        }

    }

    public void writeLandUseAccessibilityTableToFile(String luAccFileName, float[][] luAccessibility)
    {

        File accFile = new File(luAccFileName);

        // the accessibilities array is indexed by mgra values which might no be
        // consecutive.
        // create an arraylist of data table rows, with the last field being the
        // mgra value,
        // convert to a tabledataset, then write to a csv file.

        ArrayList<float[]> dataTableRows = new ArrayList<float[]>();
        ArrayList<String> dataColumnHeadings = new ArrayList<String>();
        dataColumnHeadings.add("AM_WORK_1");
        dataColumnHeadings.add("AM_WORK_2");
        dataColumnHeadings.add("AM_WORK_3");
        dataColumnHeadings.add("AM_WORK_4");
        dataColumnHeadings.add("AM_WORK_5");
        dataColumnHeadings.add("AM_WORK_6");
        dataColumnHeadings.add("AM_SCHOOL_1");
        dataColumnHeadings.add("AM_SCHOOL_2");
        dataColumnHeadings.add("AM_SCHOOL_3");
        dataColumnHeadings.add("AM_SCHOOL_4");
        dataColumnHeadings.add("AM_SCHOOL_5");
        dataColumnHeadings.add("MD_NONMAN_LS0");
        dataColumnHeadings.add("MD_NONMAN_LS1");
        dataColumnHeadings.add("MD_NONMAN_LS2");
        dataColumnHeadings.add("LUZ");

        for (int r = 0; r < luAccessibility.length; r++)
        {

            if (luAccessibility[r] != null)
            {

                float[] values = new float[luAccessibility[r].length];
                for (int c = 0; c < luAccessibility[r].length; c++)
                    values[c] = luAccessibility[r][c];

                dataTableRows.add(values);

            }

        }

        float[][] dataTableValues = new float[dataTableRows.size()][];
        for (int r = 0; r < dataTableValues.length; r++)
            dataTableValues[r] = dataTableRows.get(r);

        TableDataSet accData = TableDataSet.create(dataTableValues, dataColumnHeadings);
        CSVFileWriter csv = new CSVFileWriter();
        try
        {
            csv.writeFile(accData, accFile);
        } catch (IOException e)
        {
            logger.error("Error trying to write land use accessiblities data file " + luAccFileName);
            throw new RuntimeException(e);
        }

    }

    public void writeLandUseLogsumTablesToFile(String luLogsumFileName, double[][][][] luLogsums)
    {

        File accFile = new File(luLogsumFileName);

        // the accessibilities array is indexed by mgra values which might no be
        // consecutive.
        // create an arraylist of data table rows, with the last field being the
        // mgra value,
        // convert to a tabledataset, then write to a csv file.

        ArrayList<float[]> dataTableRows = new ArrayList<float[]>();
        ArrayList<String> dataColumnHeadings = new ArrayList<String>();
        dataColumnHeadings.add("OrigLuz");
        dataColumnHeadings.add("DestLuz");
        dataColumnHeadings.add("AM_LS0");
        dataColumnHeadings.add("AM_LS1");
        dataColumnHeadings.add("AM_LS2");
        dataColumnHeadings.add("MD_LS0");
        dataColumnHeadings.add("MD_LS1");
        dataColumnHeadings.add("MD_LS2");

        for (int l = 1; l <= BuildAccessibilities.MAX_LUZ; l++)
        {
            for (int m = 1; m <= BuildAccessibilities.MAX_LUZ; m++)
            {
                float[] values = new float[8];
                values[0] = l;
                values[1] = m;
                values[2] = (float) luLogsums[0][0][l][m];
                values[3] = (float) luLogsums[0][1][l][m];
                values[4] = (float) luLogsums[0][2][l][m];
                values[5] = (float) luLogsums[1][0][l][m];
                values[6] = (float) luLogsums[1][1][l][m];
                values[7] = (float) luLogsums[1][2][l][m];
                dataTableRows.add(values);
            }
        }

        float[][] dataTableValues = new float[dataTableRows.size()][];
        for (int r = 0; r < dataTableValues.length; r++)
            dataTableValues[r] = dataTableRows.get(r);

        TableDataSet accData = TableDataSet.create(dataTableValues, dataColumnHeadings);
        CSVFileWriter csv = new CSVFileWriter();
        try
        {
            csv.writeFile(accData, accFile);
        } catch (IOException e)
        {
            logger.error("Error trying to write land use logsums data file " + luLogsumFileName);
            throw new RuntimeException(e);
        }

    }

    public float getAggregateAccessibility(String type, int homeMgra)
    {
        float returnValue = 0;

        if (type.equalsIgnoreCase("auto")) returnValue = accessibilities[homeMgra][NONMANDATORY_AUTO_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("transit")) returnValue = accessibilities[homeMgra][NONMANDATORY_TRANSIT_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("maas")) returnValue = accessibilities[homeMgra][NONMANDATORY_MAAS_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("nonmotor")) returnValue = accessibilities[homeMgra][NONMANDATORY_NONMOTOR_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("sov0")) returnValue = accessibilities[homeMgra][NONMANDATORY_SOV_0_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("sov1")) returnValue = accessibilities[homeMgra][NONMANDATORY_SOV_1_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("sov2")) returnValue = accessibilities[homeMgra][NONMANDATORY_SOV_2_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("hov0")) returnValue = accessibilities[homeMgra][NONMANDATORY_HOV_0_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("hov1")) returnValue = accessibilities[homeMgra][NONMANDATORY_HOV_1_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("hov2")) returnValue = accessibilities[homeMgra][NONMANDATORY_HOV_2_ACCESSIBILITY_FIELD_NUMBER - 1];
        else if (type.equalsIgnoreCase("shop0")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shop1")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shop2")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maint0")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maint1")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maint2")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("eatOut0")) returnValue = accessibilities[homeMgra][EAT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("eatOut1")) returnValue = accessibilities[homeMgra][EAT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("eatOut2")) returnValue = accessibilities[homeMgra][EAT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("visit0")) returnValue = accessibilities[homeMgra][VISIT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("visit1")) returnValue = accessibilities[homeMgra][VISIT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("visit2")) returnValue = accessibilities[homeMgra][VISIT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discr0")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discr1")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discr2")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("escort0")) returnValue = accessibilities[homeMgra][ESCORT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("escort1")) returnValue = accessibilities[homeMgra][ESCORT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("escort2")) returnValue = accessibilities[homeMgra][ESCORT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("totEmp")) returnValue = accessibilities[homeMgra][TOTAL_EMPLOYMENT_ACCESSIBILITY_INDEX - 1];
        else if (type.equalsIgnoreCase("shopSov0")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shopSov1")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_SOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shopSov2")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintSov0")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintSov1")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_SOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintSov2")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrSov0")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_SOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrSov1")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_SOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrSov2")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_SOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shopHov0")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shopHov1")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("shopHov2")) returnValue = accessibilities[homeMgra][SHOP_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintHov0")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintHov1")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("maintHov2")) returnValue = accessibilities[homeMgra][MAINT_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrHov0")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_INSUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrHov1")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_SUFFICIENT_INDEX - 1];
        else if (type.equalsIgnoreCase("discrHov2")) returnValue = accessibilities[homeMgra][DISCR_ACCESSIBILITY_HOV_OVERSUFFICIENT_INDEX - 1];
        else
        {
            logger.error("argument type = "
                    + type
                    + " is not valid");
            throw new RuntimeException();
        }

        return returnValue;

    }

}
