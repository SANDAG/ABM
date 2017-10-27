package org.sandag.abm.ctramp;

import java.io.Serializable;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AccessibilitiesTable;
import org.sandag.abm.accessibilities.BuildAccessibilities;
import org.sandag.abm.modechoice.MgraDataManager;
import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public abstract class DestChoiceDMU
        implements Serializable, VariableTable
{

    protected transient Logger         logger   = Logger.getLogger(DestChoiceDMU.class);

    protected HashMap<String, Integer> methodIndexMap;

    protected Household                hh;
    protected Person                   person;
    protected Tour                     tour;
    protected IndexValues              dmuIndex = null;

    protected double                   workAccessibility;
    protected double                   nonMandatoryAccessibility;

    protected double[]                 homeMgraNonMandatoryAccessibilityArray;
    protected double[]                 homeMgraTotalEmploymentAccessibilityArray;
    protected double[]                 homeMgraSizeArray;
    protected double[]                 homeMgraDistanceArray;
    protected double[]                 modeChoiceLogsums;
    protected double[]                 dcSoaCorrections;

    protected int                      toursLeftCount;

    protected ModelStructure           modelStructure;
    protected MgraDataManager          mgraManager;
    protected BuildAccessibilities     aggAcc;
    protected AccessibilitiesTable     accTable;

    public DestChoiceDMU(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
        initDmuObject();
    }

    public abstract void setMcLogsum(int mgra, double logsum);

    private void initDmuObject()
    {

        dmuIndex = new IndexValues();

        // create default objects - some choice models use these as place
        // holders for values
        person = new Person(null, -1, modelStructure);
        hh = new Household(modelStructure);

        mgraManager = MgraDataManager.getInstance();

        int maxMgra = mgraManager.getMaxMgra();

        modeChoiceLogsums = new double[maxMgra + 1];
        dcSoaCorrections = new double[maxMgra + 1];

    }

    public void setHouseholdObject(Household hhObject)
    {
        hh = hhObject;
    }

    public void setPersonObject(Person personObject)
    {
        person = personObject;
    }

    public void setTourObject(Tour tour)
    {
        this.tour = tour;
    }

    public void setAggAcc(BuildAccessibilities aggAcc)
    {
        this.aggAcc = aggAcc;
    }

    public void setAccTable(AccessibilitiesTable myAccTable)
    {
        accTable = myAccTable;
    }

    public void setDestChoiceSize(double[] homeMgraSizeArray)
    {
        this.homeMgraSizeArray = homeMgraSizeArray;
    }

    public void setDestChoiceDistance(double[] homeMgraDistanceArray)
    {
        this.homeMgraDistanceArray = homeMgraDistanceArray;
    }

    public void setDcSoaCorrections(int mgra, double correction)
    {
        dcSoaCorrections[mgra] = correction;
    }

    public void setNonMandatoryAccessibility(double nonMandatoryAccessibility)
    {
        this.nonMandatoryAccessibility = nonMandatoryAccessibility;
    }

    public void setToursLeftCount(int count)
    {
        toursLeftCount = count;
    }

    public void setDmuIndexValues(int hhId, int zoneId, int origTaz, int destTaz)
    {
        dmuIndex.setHHIndex(hhId);
        dmuIndex.setZoneIndex(zoneId);
        dmuIndex.setOriginZone(origTaz);
        dmuIndex.setDestZone(destTaz);

        dmuIndex.setDebug(false);
        dmuIndex.setDebugLabel("");
        if (hh.getDebugChoiceModels())
        {
            dmuIndex.setDebug(true);
            dmuIndex.setDebugLabel("Debug DC UEC");
        }

    }

    public IndexValues getDmuIndexValues()
    {
        return dmuIndex;
    }

    public Household getHouseholdObject()
    {
        return hh;
    }

    public Person getPersonObject()
    {
        return person;
    }

    // DMU methods - define one of these for every @var in the mode choice
    // control
    // file.

    protected int getToursLeftCount()
    {
        return toursLeftCount;
    }

    protected int getMaxContinuousAvailableWindow()
    {

        if (tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY)) return hh
                .getMaxJointTimeWindow(tour);
        else return person.getMaximumContinuousAvailableWindow();
    }

    protected double getDcSoaCorrectionsAlt(int alt)
    {
        return dcSoaCorrections[alt];
    }

    protected double getMcLogsumDestAlt(int mgra)
    {
        return modeChoiceLogsums[mgra];
    }

    protected double getPopulationDestAlt(int mgra)
    {
        return aggAcc.getMgraPopulation(mgra);
    }

    protected double getHouseholdsDestAlt(int mgra)
    {
        return aggAcc.getMgraHouseholds(mgra);
    }

    protected double getGradeSchoolEnrollmentDestAlt(int mgra)
    {
        return aggAcc.getMgraGradeSchoolEnrollment(mgra);
    }

    protected double getHighSchoolEnrollmentDestAlt(int mgra)
    {
        return aggAcc.getMgraHighSchoolEnrollment(mgra);
    }

    protected double getUniversityEnrollmentDestAlt(int mgra)
    {
        return aggAcc.getMgraUniversityEnrollment(mgra);
    }

    protected double getOtherCollegeEnrollmentDestAlt(int mgra)
    {
        return aggAcc.getMgraOtherCollegeEnrollment(mgra);
    }

    protected double getAdultSchoolEnrollmentDestAlt(int mgra)
    {
        return aggAcc.getMgraAdultSchoolEnrollment(mgra);
    }

    protected int getIncome()
    {
        return hh.getIncomeCategory();
    }

    protected int getIncomeInDollars()
    {
        return hh.getIncomeInDollars();
    }

    protected int getAutos()
    {
        return hh.getAutoOwnershipModelResult();
    }

    protected int getWorkers()
    {
        return hh.getWorkers();
    }

    protected int getNumberOfNonWorkingAdults()
    {
        return hh.getNumberOfNonWorkingAdults();
    }

    protected int getNumPreschool()
    {
        return hh.getNumPreschool();
    }

    public int getNumGradeSchoolStudents()
    {
        return hh.getNumGradeSchoolStudents();
    }

    public int getNumHighSchoolStudents()
    {
        return hh.getNumHighSchoolStudents();
    }

    protected int getNumChildrenUnder16()
    {
        return hh.getNumChildrenUnder16();
    }

    protected int getNumChildrenUnder19()
    {
        return hh.getNumChildrenUnder19();
    }

    protected int getAge()
    {
        return person.getAge();
    }

    protected int getFemaleWorker()
    {
        if (person.getPersonIsFemale() == 1) return 1;
        else return 0;
    }

    protected int getFemale()
    {
        if (person.getPersonIsFemale() == 1) return 1;
        else return 0;
    }

    protected int getFullTimeWorker()
    {
        if (person.getPersonIsFullTimeWorker() == 1) return 1;
        else return 0;
    }

    protected int getTypicalUniversityStudent()
    {
        return person.getPersonIsTypicalUniversityStudent();
    }

    protected int getPersonType()
    {
        return person.getPersonTypeNumber();
    }

    protected int getPersonHasBachelors()
    {
        return person.getHasBachelors();
    }

    protected int getPersonIsWorker()
    {
        return person.getPersonIsWorker();
    }

    protected int getWorkTaz()
    {
        return person.getWorkLocation();
    }

    protected int getWorkTourModeIsSOV()
    {
        boolean tourModeIsSov = modelStructure.getTourModeIsSov(tour.getTourModeChoice());
        if (tourModeIsSov) return 1;
        else return 0;
    }

    protected int getTourIsJoint()
    {
        return tour.getTourCategory().equalsIgnoreCase(ModelStructure.JOINT_NON_MANDATORY_CATEGORY) ? 1
                : 0;
    }

    protected double getTotEmpAccessibilityAlt(int alt)
    {
        return homeMgraTotalEmploymentAccessibilityArray[alt];
    }

    protected double getNonMandatoryAccessibilityAlt(int alt)
    {
        return accTable.getAggregateAccessibility("nonmotor", alt);
    }

    protected double getOpSovDistanceAlt(int alt)
    {
        return homeMgraDistanceArray[alt];
    }

    protected double getLnDcSizeAlt(int alt)
    {
        return Math.log(homeMgraSizeArray[alt] + 1);
    }

    protected double getDcSizeAlt(int alt)
    {
        return homeMgraSizeArray[alt];
    }

    protected void setWorkAccessibility(double accessibility)
    {
        workAccessibility = accessibility;
    }

    protected double getWorkAccessibility()
    {
        return workAccessibility;
    }

    protected double getNonMandatoryAccessibility()
    {
        return nonMandatoryAccessibility;
    }

    public int getIndexValue(String variableName)
    {
        return methodIndexMap.get(variableName);
    }

    public int getAssignmentIndexValue(String variableName)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex)
    {
        throw new UnsupportedOperationException();
    }

    public double getValueForIndex(int variableIndex, int arrayIndex)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(String variableName, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

    public void setValue(int variableIndex, double variableValue)
    {
        throw new UnsupportedOperationException();
    }

}
