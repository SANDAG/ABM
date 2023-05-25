package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.ctramp.StopFrequencyDMU;

public class SandagStopFrequencyDMU
        extends StopFrequencyDMU
{

    // the SANDAG UEC worksheet numbers defined below are used to associate
    // worksheet
    // pages to CTRAMP purpose indices
    private static final int          WORK_MODEL_SHEET       = 1;
    private static final int          UNIVERSITY_MODEL_SHEET = 2;
    private static final int          SCHOOL_MODEL_SHEET     = 3;
    private static final int          ESCORT_MODEL_SHEET     = 4;
    private static final int          SHOPPING_MODEL_SHEET   = 5;
    private static final int          MAINT_MODEL_SHEET      = 6;
    private static final int          EAT_OUT_MODEL_SHEET    = 7;
    private static final int          VISITING_MODEL_SHEET   = 8;
    private static final int          DISCR_MODEL_SHEET      = 9;
    private static final int          WORK_BASED_MODEL_SHEET = 10;

    private HashMap<Integer, Integer> tourPurposeModelSheetMap;
    private HashMap<Integer, Integer> tourPurposeChoiceModelIndexMap;
    private int[]                     modelSheetValues;

    public SandagStopFrequencyDMU(ModelStructure modelStructure)
    {
        super(modelStructure);
        setupModelIndexMappings();
        setupMethodIndexMap();

        // set names used in SANDAG stop purpose file
        STOP_PURPOSE_FILE_WORK_NAME = "Work";
        STOP_PURPOSE_FILE_UNIVERSITY_NAME = "University";
        STOP_PURPOSE_FILE_SCHOOL_NAME = "School";
        STOP_PURPOSE_FILE_ESCORT_NAME = "Escort";
        STOP_PURPOSE_FILE_SHOPPING_NAME = "Shop";
        STOP_PURPOSE_FILE_MAINT_NAME = "Maintenance";
        STOP_PURPOSE_FILE_EAT_OUT_NAME = "Eating Out";
        STOP_PURPOSE_FILE_VISIT_NAME = "Visiting";
        STOP_PURPOSE_FILE_DISCR_NAME = "Discretionary";
        STOP_PURPOSE_FILE_WORK_BASED_NAME = "Work-Based";
    }

    private void setupModelIndexMappings()
    {

        // setup the mapping from tour primary purpose indices to the worksheet
        // page
        // indices
        tourPurposeModelSheetMap = new HashMap<Integer, Integer>();
        tourPurposeModelSheetMap.put(ModelStructure.WORK_PRIMARY_PURPOSE_INDEX, WORK_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.UNIVERSITY_PRIMARY_PURPOSE_INDEX,
                UNIVERSITY_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.SCHOOL_PRIMARY_PURPOSE_INDEX,
                SCHOOL_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.ESCORT_PRIMARY_PURPOSE_INDEX,
                ESCORT_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.SHOPPING_PRIMARY_PURPOSE_INDEX,
                SHOPPING_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.OTH_MAINT_PRIMARY_PURPOSE_INDEX,
                MAINT_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.EAT_OUT_PRIMARY_PURPOSE_INDEX,
                EAT_OUT_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.VISITING_PRIMARY_PURPOSE_INDEX,
                VISITING_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.OTH_DISCR_PRIMARY_PURPOSE_INDEX,
                DISCR_MODEL_SHEET);
        tourPurposeModelSheetMap.put(ModelStructure.WORK_BASED_PRIMARY_PURPOSE_INDEX,
                WORK_BASED_MODEL_SHEET);

        // setup a mapping between primary tour purpose indices and
        // ChoiceModelApplication array indices
        // so that only as many ChoiceModelApplication objects are created in
        // the
        // Stop Frequency model implementation
        // as there are worksheet model pages.
        tourPurposeChoiceModelIndexMap = new HashMap<Integer, Integer>();

        int modelIndex = 0;
        HashMap<Integer, Integer> modelSheetIndexMap = new HashMap<Integer, Integer>();
        for (int modelPurposeKey : tourPurposeModelSheetMap.keySet())
        {

            // get the sheet number associated with the tour purpose
            int modelSheetKey = tourPurposeModelSheetMap.get(modelPurposeKey);

            // if the sheet number already exists in the sheet index to choice
            // model
            // index mapping, get that index
            // and use it for the purpose to model index mapping
            if (modelSheetIndexMap.containsKey(modelSheetKey))
            {
                int index = modelSheetIndexMap.get(WORK_MODEL_SHEET);
                tourPurposeChoiceModelIndexMap.put(modelPurposeKey, index);
            } else
            {
                // otherwise add this sheet number to the model index mapping
                // and use
                // it
                // for the purpose to model index mapping.
                modelSheetIndexMap.put(modelSheetKey, modelIndex);
                tourPurposeChoiceModelIndexMap.put(modelPurposeKey, modelIndex);
                modelIndex++;
            }
        }

        modelSheetValues = new int[modelIndex];
        int i = 0;
        for (int sheet : modelSheetIndexMap.keySet())
            modelSheetValues[i++] = sheet;

    }

    /**
     * @return the array of unique worksheet model sheet values for whic a
     *         ChoiceModelApplication object will be created. The size of this
     *         array determines the number of ChoiceModelApplication objects.
     */
    public int[] getModelSheetValuesArray()
    {
        return modelSheetValues;
    }

    /**
     * @return the HashMap<Integer, Integer> that relates primary tour purpose
     *         indices to ChoiceModelApplication array indices.
     */
    public HashMap<Integer, Integer> getTourPurposeChoiceModelIndexMap()
    {
        return tourPurposeChoiceModelIndexMap;
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getNumFtWorkers", 0);
        methodIndexMap.put("getNumPtWorkers", 1);
        methodIndexMap.put("getIncomeInDollars", 2);
        methodIndexMap.put("getPersonType", 3);
        methodIndexMap.put("getHhSize", 4);
        methodIndexMap.put("getNumHhDrivingStudents", 5);
        methodIndexMap.put("getNumHhNonDrivingStudents", 6);
        methodIndexMap.put("getNumHhPreschool", 7);
        methodIndexMap.put("getWorkTours", 8);
        methodIndexMap.put("getTotalTours", 9);
        methodIndexMap.put("getTotalHouseholdTours", 10);
        methodIndexMap.put("getWorkLocationDistance", 11);
        methodIndexMap.put("getSchoolLocationDistance", 12);
        methodIndexMap.put("getAge", 13);
        methodIndexMap.put("getSchoolTours", 14);
        methodIndexMap.put("getEscortTours", 15);
        methodIndexMap.put("getShoppingTours", 16);
        methodIndexMap.put("getMaintenanceTours", 17);
        methodIndexMap.put("getEatTours", 18);
        methodIndexMap.put("getVisitTours", 19);
        methodIndexMap.put("getDiscretionaryTours", 20);
        methodIndexMap.put("getShoppingAccessibility", 21);
        methodIndexMap.put("getMaintenanceAccessibility", 22);
        methodIndexMap.put("getDiscretionaryAccessibility", 23);
        methodIndexMap.put("getIsJoint", 24);
        methodIndexMap.put("getTourDurationHours", 25);
        methodIndexMap.put("getTourModeIsAuto", 26);
        methodIndexMap.put("getTourModeIsTransit", 27);
        methodIndexMap.put("getTourModeIsNonMotorized", 28);
        methodIndexMap.put("getTourModeIsSchoolBus", 29);
        methodIndexMap.put("getTourDepartPeriod", 30);
        methodIndexMap.put("getTourArrivePeriod", 31);
        methodIndexMap.put("getTelecommuteFrequency", 32);
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 0:
                return getNumFtWorkers();
            case 1:
                return getNumPtWorkers();
            case 2:
                return getIncomeInDollars();
            case 3:
                return getPersonType();
            case 4:
                return getHhSize();
            case 5:
                return getNumHhDrivingStudents();
            case 6:
                return getNumHhNonDrivingStudents();
            case 7:
                return getNumHhPreschool();
            case 8:
                return getWorkTours();
            case 9:
                return getTotalTours();
            case 10:
                return getTotalHouseholdTours();
            case 11:
                return getWorkLocationDistance();
            case 12:
                return getSchoolLocationDistance();
            case 13:
                return getAge();
            case 14:
                return getSchoolTours();
            case 15:
                return getEscortTours();
            case 16:
                return getShoppingTours();
            case 17:
                return getMaintenanceTours();
            case 18:
                return getEatTours();
            case 19:
                return getVisitTours();
            case 20:
                return getDiscretionaryTours();
            case 21:
                return getShoppingAccessibility();
            case 22:
                return getMaintenanceAccessibility();
            case 23:
                return getDiscretionaryAccessibility();
            case 24:
                return getTourIsJoint();
            case 25:
                return getTourDurationInHours();
            case 26:
                return getTourModeIsAuto();
            case 27:
                return getTourModeIsTransit();
            case 28:
                return getTourModeIsNonMotorized();
            case 29:
                return getTourModeIsSchoolBus();
            case 30:
            	return getTourDepartPeriod();
            case 31:
            	return getTourArrivePeriod();
            case 32:
            	return getTelecommuteFrequency();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }
    }

}