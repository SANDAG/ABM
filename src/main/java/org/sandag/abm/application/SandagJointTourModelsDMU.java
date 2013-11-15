package org.sandag.abm.application;

import java.util.HashMap;
import org.sandag.abm.ctramp.JointTourModelsDMU;
import org.sandag.abm.ctramp.ModelStructure;

public class SandagJointTourModelsDMU
        extends JointTourModelsDMU
{

    public SandagJointTourModelsDMU(ModelStructure modelStructure)
    {
        super();
        setupMethodIndexMap();
    }

    private void setupMethodIndexMap()
    {
        methodIndexMap = new HashMap<String, Integer>();

        methodIndexMap.put("getActiveCountFullTimeWorkers", 1);
        methodIndexMap.put("getActiveCountPartTimeWorkers", 2);
        methodIndexMap.put("getActiveCountUnivStudents", 3);
        methodIndexMap.put("getActiveCountNonWorkers", 4);
        methodIndexMap.put("getActiveCountRetirees", 5);
        methodIndexMap.put("getActiveCountDrivingAgeSchoolChildren", 6);
        methodIndexMap.put("getActiveCountPreDrivingAgeSchoolChildren", 7);
        methodIndexMap.put("getActiveCountPreSchoolChildren", 8);
        methodIndexMap.put("getMaxPairwiseAdultOverlapsHh", 9);
        methodIndexMap.put("getMaxPairwiseChildOverlapsHh", 10);
        methodIndexMap.put("getMaxPairwiseMixedOverlapsHh", 11);
        methodIndexMap.put("getMaxPairwiseOverlapOtherAdults", 12);
        methodIndexMap.put("getMaxPairwiseOverlapOtherChildren", 13);
        methodIndexMap.put("getTravelActiveAdults", 14);
        methodIndexMap.put("getTravelActiveChildren", 15);
        methodIndexMap.put("getPersonStaysHome", 16);
        methodIndexMap.put("getIncomeLessThan30K", 17);
        methodIndexMap.put("getIncome30Kto60K", 18);
        methodIndexMap.put("getIncomeMoreThan100K", 19);
        methodIndexMap.put("getNumAdults", 20);
        methodIndexMap.put("getNumChildren", 21);
        methodIndexMap.put("getHhWorkers", 22);
        methodIndexMap.put("getAutoOwnership", 23);
        methodIndexMap.put("getTourPurposeIsMaint", 24);
        methodIndexMap.put("getTourPurposeIsEat", 25);
        methodIndexMap.put("getTourPurposeIsVisit", 26);
        methodIndexMap.put("getTourPurposeIsDiscr", 27);
        methodIndexMap.put("getPersonType", 28);
        methodIndexMap.put("getJointTourComposition", 29);
        methodIndexMap.put("getJTours", 30);
        methodIndexMap.put("getShopHOVAccessibility", 31);
        methodIndexMap.put("getMaintHOVAccessibility", 32);
        methodIndexMap.put("getDiscrHOVAccessibility", 33);

    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {

        switch (variableIndex)
        {
            case 1:
                return getActiveCountFullTimeWorkers();
            case 2:
                return getActiveCountPartTimeWorkers();
            case 3:
                return getActiveCountUnivStudents();
            case 4:
                return getActiveCountNonWorkers();
            case 5:
                return getActiveCountRetirees();
            case 6:
                return getActiveCountDrivingAgeSchoolChildren();
            case 7:
                return getActiveCountPreDrivingAgeSchoolChildren();
            case 8:
                return getActiveCountPreSchoolChildren();
            case 9:
                return getMaxPairwiseAdultOverlapsHh();
            case 10:
                return getMaxPairwiseChildOverlapsHh();
            case 11:
                return getMaxPairwiseMixedOverlapsHh();
            case 12:
                return getMaxPairwiseOverlapOtherAdults();
            case 13:
                return getMaxPairwiseOverlapOtherChildren();
            case 14:
                return getTravelActiveAdults();
            case 15:
                return getTravelActiveChildren();
            case 16:
                return getPersonStaysHome();
            case 17:
                return getIncomeLessThan30K();
            case 18:
                return getIncome30Kto60K();
            case 19:
                return getIncomeMoreThan100K();
            case 20:
                return getNumAdults();
            case 21:
                return getNumChildren();
            case 22:
                return getHhWorkers();
            case 23:
                return getAutoOwnership();
            case 24:
                return getTourPurposeIsMaint();
            case 25:
                return getTourPurposeIsEat();
            case 26:
                return getTourPurposeIsVisit();
            case 27:
                return getTourPurposeIsDiscr();
            case 28:
                return getPersonType();
            case 29:
                return getJointTourComposition();
            case 30:
                return getJTours();
            case 31:
                return getShopHOVAccessibility();
            case 32:
                return getMaintHOVAccessibility();
            case 33:
                return getDiscrHOVAccessibility();

            default:
                logger.error("method number = " + variableIndex + " not found");
                throw new RuntimeException("method number = " + variableIndex + " not found");

        }

    }

}
