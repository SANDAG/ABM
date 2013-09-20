package org.sandag.abm.ctramp;

import java.util.HashMap;
import java.util.Random;
import org.apache.log4j.Logger;
import org.sandag.abm.ctramp.Stop;

public class Household
        implements java.io.Serializable
{

    private boolean                   debugChoiceModels;

    private int                       hhId;
    private int                       hhIncome;
    private int                       hhIncomeInDollars;
    private int                       hhSize;
    private int                       hhType;
    private int                       unitType;
    private int                       hhBldgsz;
    private int                       hhWorkers;

    private int                       homeTaz;
    private int                       homeMgra;
    private int                       homeWalkSubzone;

    private Person[]                  persons;

    private Tour[]                    jointTours;

    private int                       aoModelAutos;
    private String                    cdapModelPattern;
    private int                       imtfModelPattern;
    private String                    jtfModelPattern;
    private int                       tpChoice;

    private Random                    hhRandom;
    private int                       randomCount = 0;
    private HashMap<Integer, Integer> uwslRandomCountList;
    private int                       preAoRandomCount;
    private int                       aoRandomCount;
    private int                       tpRandomCount;
    private int                       fpRandomCount;
    private int                       ieRandomCount;
    private int                       cdapRandomCount;
    private int                       imtfRandomCount;
    private int                       imtodRandomCount;
    private int                       awfRandomCount;
    private int                       awlRandomCount;
    private int                       awtodRandomCount;
    private int                       jtfRandomCount;
    private int                       jtlRandomCount;
    private int                       jtodRandomCount;
    private int                       inmtfRandomCount;
    private int                       inmtlRandomCount;
    private int                       inmtodRandomCount;
    private int                       stfRandomCount;
    private int                       stlRandomCount;

    private int                       maxAdultOverlaps;
    private int                       maxChildOverlaps;
    private int                       maxMixedOverlaps;

    private ModelStructure            modelStructure;

    public Household(ModelStructure modelStructure)
    {
        this.modelStructure = modelStructure;
        hhRandom = new Random();
        uwslRandomCountList = new HashMap<Integer, Integer>();
    }

    public Person[] getPersons()
    {
        return persons;
    }

    public void initializeWindows()
    {

        // loop through the person array (1-based)
        for (int i = 1; i < persons.length; ++i)
        {
            persons[i].initializeWindows();
        }

    }

    public void setDebugChoiceModels(boolean value)
    {
        debugChoiceModels = value;
    }

    public void setHhId(int id, int baseSeed)
    {
        hhId = id;
        randomCount = 0;
        hhRandom.setSeed(baseSeed + hhId);
    }

    public void setRandomObject(Random r)
    {
        hhRandom = r;
    }

    public void setHhRandomCount(int count)
    {
        randomCount = count;
    }

    // work/school location choice uses shadow pricing, so save randomCount per
    // iteration
    public void setUwslRandomCount(int iter, int count)
    {
        uwslRandomCountList.put(iter, count);
    }

    public void setPreAoRandomCount(int count)
    {
        preAoRandomCount = count;
    }

    public void setAoRandomCount(int count)
    {
        aoRandomCount = count;
    }

    public void setTpRandomCount(int count)
    {
        tpRandomCount = count;
    }

    public void setFpRandomCount(int count)
    {
        fpRandomCount = count;
    }

    public void setIeRandomCount(int count)
    {
        ieRandomCount = count;
    }

    public void setCdapRandomCount(int count)
    {
        cdapRandomCount = count;
    }

    public void setImtfRandomCount(int count)
    {
        imtfRandomCount = count;
    }

    public void setImtodRandomCount(int count)
    {
        imtodRandomCount = count;
    }

    public void setAwfRandomCount(int count)
    {
        awfRandomCount = count;
    }

    public void setAwlRandomCount(int count)
    {
        awlRandomCount = count;
    }

    public void setAwtodRandomCount(int count)
    {
        awtodRandomCount = count;
    }

    public void setJtfRandomCount(int count)
    {
        jtfRandomCount = count;
    }

    public void setJtlRandomCount(int count)
    {
        jtlRandomCount = count;
    }

    public void setJtodRandomCount(int count)
    {
        jtodRandomCount = count;
    }

    public void setInmtfRandomCount(int count)
    {
        inmtfRandomCount = count;
    }

    public void setInmtlRandomCount(int count)
    {
        inmtlRandomCount = count;
    }

    public void setInmtodRandomCount(int count)
    {
        inmtodRandomCount = count;
    }

    public void setStfRandomCount(int count)
    {
        stfRandomCount = count;
    }

    public void setStlRandomCount(int count)
    {
        stlRandomCount = count;
    }

    public void setHhTaz(int taz)
    {
        homeTaz = taz;
    }

    public void setHhMgra(int mgra)
    {
        homeMgra = mgra;
    }

    public void setHhWalkSubzone(int subzone)
    {
        homeWalkSubzone = subzone;
    }

    public void setHhAutos(int autos)
    {
        // this sets the variable that will be used in work/school location choice.
        // after auto ownership runs, this variable gets updated with number of autos
        // for result.
        aoModelAutos = autos;
    }

    public void setTpChoice(int value)
    {
        tpChoice = value;
    }

    public void setAutoOwnershipModelResult(int aoModelAlternativeChosen)
    {
        // store the number of autos owned by the household (AO model alternative -
        // 1).
        aoModelAutos = aoModelAlternativeChosen - 1;
    }

    /**
     * auto sufficiency: 1 if cars < workers, 2 if cars equal workers, 3 if cars > workers
     * 
     * @return auto sufficiency value
     */
    public int getAutoSufficiency()
    {
        if (aoModelAutos < hhWorkers) return 1;
        else if (aoModelAutos == hhWorkers) return 2;
        else return 3;
    }

    public int getAutoOwnershipModelResult()
    {
        return aoModelAutos;
    }

    public int getTpChoice()
    {
        return tpChoice;
    }

    public void setCoordinatedDailyActivityPatternResult(String pattern)
    {
        cdapModelPattern = pattern;
    }

    public String getCoordinatedDailyActivityPattern()
    {
        return cdapModelPattern;
    }

    public void setJointTourFreqResult(int altIndex, String altName)
    {
        jtfModelPattern = String.format("%d_%s", altIndex, altName);
    }

    public int getJointTourFreqChosenAlt()
    {
        int returnValue = 0;
        if (jtfModelPattern == null)
        {
            returnValue = 0;
        } else
        {
            int endIndex = jtfModelPattern.indexOf('_');
            returnValue = Integer.parseInt(jtfModelPattern.substring(0, endIndex));
        }
        return returnValue;
    }

    public String getJointTourFreqChosenAltName()
    {
        String returnValue = "none";
        if (jtfModelPattern != null)
        {
            int startIndex = jtfModelPattern.indexOf('_') + 1;
            returnValue = jtfModelPattern.substring(startIndex);
        }
        return returnValue;
    }

    public void setHhBldgsz(int code)
    {
        hhBldgsz = code;
    }

    public int getHhBldgsz()
    {
        return hhBldgsz;
    }

    public void setHhSize(int numPersons)
    {
        hhSize = numPersons;
        persons = new Person[numPersons + 1];
        for (int i = 1; i <= numPersons; i++)
            persons[i] = new Person(this, i, modelStructure);

    }

    public void setHhIncome(int category)
    {
        hhIncome = category;
    }

    public void setHhIncomeInDollars(int dollars)
    {
        hhIncomeInDollars = dollars;
    }

    public void setHhWorkers(int numWorkers)
    {
        hhWorkers = numWorkers;
    }

    public void setHhType(int type)
    {
        hhType = type;
    }

    // 0=Housing unit, 1=Institutional group quarters, 2=Noninstitutional group
    // quarters
    public void setUnitType(int type)
    {
        unitType = type;
    }

    public boolean getDebugChoiceModels()
    {
        return debugChoiceModels;
    }

    public int getHhSize()
    {
        return hhSize;
    }

    public int getNumTotalIndivTours()
    {
        int count = 0;
        for (int i = 1; i < persons.length; i++)
            count += persons[i].getNumTotalIndivTours();
        return count;
    }

    public int getNumberOfNonWorkingAdults()
    {
        int count = 0;
        for (int i = 1; i < persons.length; i++)
            count += persons[i].getPersonIsNonWorkingAdultUnder65()
                    + persons[i].getPersonIsNonWorkingAdultOver65();
        return count;
    }

    public int getIsNonFamilyHousehold()
    {

        if (hhType == HouseholdType.NON_FAMILY_MALE_ALONE.ordinal()) return (1);
        if (hhType == HouseholdType.NON_FAMILY_MALE_NOT_ALONE.ordinal()) return (1);
        if (hhType == HouseholdType.NON_FAMILY_FEMALE_ALONE.ordinal()) return (1);
        if (hhType == HouseholdType.NON_FAMILY_FEMALE_NOT_ALONE.ordinal()) return (1);

        return (0);
    }

    /**
     * unitType: 0=Housing unit, 1=Institutional group quarters, 2=Noninstitutional group quarters
     * 
     * @return 1 if household is group quarters, 0 for non-group quarters
     */
    public int getIsGroupQuarters()
    {
        if (unitType == 0) return 0;
        else return 1;
    }

    public int getNumStudents()
    {
        int count = 0;
        for (int i = 1; i < persons.length; ++i)
        {
            count += persons[i].getPersonIsStudent();
        }
        return (count);
    }

    public int getNumGradeSchoolStudents()
    {
        int count = 0;
        for (int i = 1; i < persons.length; ++i)
        {
            count += persons[i].getPersonIsGradeSchool();
        }
        return (count);
    }

    public int getNumHighSchoolStudents()
    {
        int count = 0;
        for (int i = 1; i < persons.length; ++i)
        {
            count += persons[i].getPersonIsHighSchool();
        }
        return (count);
    }

    public int getNumberOfChildren6To18WithoutMandatoryActivity()
    {

        int count = 0;

        for (int i = 1; i < persons.length; ++i)
        {
            count += persons[i].getPersonIsChild6To18WithoutMandatoryActivity();
        }

        return (count);
    }

    public int getNumberOfPreDrivingWithNonHomeActivity()
    {

        int count = 0;
        for (int i = 1; i < persons.length; ++i)
        {
            // count only predrving kids
            if (persons[i].getPersonIsStudentDriving() == 1)
            {
                // count only if CDAP is M or N (i.e. not H)
                if (!persons[i].getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN))
                    count++;
            }
        }

        return count;
    }

    public int getNumberOfPreschoolWithNonHomeActivity()
    {

        int count = 0;
        for (int i = 1; i < persons.length; ++i)
        {
            // count only predrving kids
            if (persons[i].getPersonIsPreschoolChild() == 1)
            {
                // count only if CDAP is M or N (i.e. not H)
                if (!persons[i].getCdapActivity().equalsIgnoreCase(ModelStructure.HOME_PATTERN))
                    count++;
            }
        }

        return count;
    }

    /**
     * return the number of school age students this household has for the purpose index.
     * 
     * @param purposeIndex
     *            is the DC purpose index to be compared to the usual school location index saved for this person upon reading synthetic population
     *            file.
     * @return num, a value of the number of school age students in the household for this purpose index.
     */
    public int getNumberOfDrivingAgedStudentsWithDcPurposeIndex(int segmentIndex)
    {
        int num = 0;
        for (int j = 1; j < persons.length; j++)
        {
            if (persons[j].getPersonIsStudentDriving() == 1
                    && persons[j].getSchoolLocationSegmentIndex() == segmentIndex) num++;
        }
        return num;
    }

    public int getNumberOfNonDrivingAgedStudentsWithDcPurposeIndex(int segmentIndex)
    {
        int num = 0;
        for (int j = 1; j < persons.length; j++)
        {
            if (persons[j].getPersonIsStudentNonDriving() == 1
                    || persons[j].getPersonIsPreschoolChild() == 1
                    && persons[j].getSchoolLocationSegmentIndex() == segmentIndex) num++;
        }
        return num;
    }

    public Person getPerson(int persNum)
    {
        if (persNum < 1 || persNum > hhSize)
        {
            throw new RuntimeException(String.format(
                    "persNum value = %d is out of range for hhSize = %d", persNum, hhSize));
        }

        return persons[persNum];
    }

    // methods DMU will use to get info from household object

    public int getHhId()
    {
        return hhId;
    }

    public Random getHhRandom()
    {
        randomCount++;
        return hhRandom;
    }

    public int getHhRandomCount()
    {
        return randomCount;
    }

    public int getUwslRandomCount(int iter)
    {
        return uwslRandomCountList.get(iter);
    }

    public int getPreAoRandomCount()
    {
        return preAoRandomCount;
    }

    public int getAoRandomCount()
    {
        return aoRandomCount;
    }

    public int getTpRandomCount()
    {
        return tpRandomCount;
    }

    public int getFpRandomCount()
    {
        return fpRandomCount;
    }

    public int getIeRandomCount()
    {
        return ieRandomCount;
    }

    public int getCdapRandomCount()
    {
        return cdapRandomCount;
    }

    public int getImtfRandomCount()
    {
        return imtfRandomCount;
    }

    public int getImtodRandomCount()
    {
        return imtodRandomCount;
    }

    public int getJtfRandomCount()
    {
        return jtfRandomCount;
    }

    public int getAwfRandomCount()
    {
        return awfRandomCount;
    }

    public int getAwlRandomCount()
    {
        return awlRandomCount;
    }

    public int getAwtodRandomCount()
    {
        return awtodRandomCount;
    }

    public int getJtlRandomCount()
    {
        return jtlRandomCount;
    }

    public int getJtodRandomCount()
    {
        return jtodRandomCount;
    }

    public int getInmtfRandomCount()
    {
        return inmtfRandomCount;
    }

    public int getInmtlRandomCount()
    {
        return inmtlRandomCount;
    }

    public int getInmtodRandomCount()
    {
        return inmtodRandomCount;
    }

    public int getStfRandomCount()
    {
        return stfRandomCount;
    }

    public int getStlRandomCount()
    {
        return stlRandomCount;
    }

    public int getHhTaz()
    {
        return homeTaz;
    }

    public int getHhMgra()
    {
        return homeMgra;
    }

    public int getHhWalkSubzone()
    {
        return homeWalkSubzone;
    }

    public int getIncome()
    {
        return hhIncome;
    }

    public int getIncomeInDollars()
    {
        return hhIncomeInDollars;
    }

    public int getWorkers()
    {
        return hhWorkers;
    }

    public int getDrivers()
    {
        return getNumPersons16plus();
    }

    public int getSize()
    {
        return hhSize;
    }

    public int getChildunder16()
    {
        if (getNumChildrenUnder16() > 0) return 1;
        else return 0;
    }

    public int getChild16plus()
    {
        if (getNumPersons16plus() > 0) return 1;
        else return 0;
    }

    public int getNumChildrenUnder16()
    {
        int numChildrenUnder16 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() < 16) numChildrenUnder16++;
        }
        return numChildrenUnder16;
    }

    public int getNumChildrenUnder19()
    {
        int numChildrenUnder19 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() < 19) numChildrenUnder19++;
        }
        return numChildrenUnder19;
    }

    public int getNumPersons0to4()
    {
        int numPersons0to4 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() < 5) numPersons0to4++;
        }
        return numPersons0to4;
    }

    /**
     * used in AO choice utility
     * 
     * @return number of persons age 6 to 15, inclusive
     */
    public int getNumPersons6to15()
    {
        int numPersons6to15 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 6 && persons[i].getAge() <= 15) numPersons6to15++;
        }
        return numPersons6to15;
    }

    /**
     * used in Stop Frequency choice utility
     * 
     * @return number of persons age 5 to 15, inclusive
     */
    public int getNumPersons5to15()
    {
        int numPersons5to15 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 5 && persons[i].getAge() <= 15) numPersons5to15++;
        }
        return numPersons5to15;
    }

    public int getNumPersons16to17()
    {
        int numPersons16to17 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 16 && persons[i].getAge() <= 17) numPersons16to17++;
        }
        return numPersons16to17;
    }

    public int getNumPersons16plus()
    {
        int numPersons16plus = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 16) numPersons16plus++;
        }
        return numPersons16plus;
    }

    public int getNumPersons18plus()
    {
        int numPersons18plus = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 18) numPersons18plus++;
        }
        return numPersons18plus;
    }

    public int getNumPersons80plus()
    {
        int numPersons80plus = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 80) numPersons80plus++;
        }
        return numPersons80plus;
    }

    public int getNumPersons18to24()
    {
        int numPersons18to24 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 18 && persons[i].getAge() <= 24) numPersons18to24++;
        }
        return numPersons18to24;
    }

    public int getNumPersons65to79()
    {
        int numPersons65to79 = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (persons[i].getAge() >= 65 && persons[i].getAge() <= 79) numPersons65to79++;
        }
        return numPersons65to79;
    }

    public int getNumFtWorkers()
    {
        int numFtWorkers = 0;
        for (int i = 1; i < persons.length; i++)
            numFtWorkers += persons[i].getPersonIsFullTimeWorker();
        return numFtWorkers;
    }

    public int getNumPtWorkers()
    {
        int numPtWorkers = 0;
        for (int i = 1; i < persons.length; i++)
            numPtWorkers += persons[i].getPersonIsPartTimeWorker();
        return numPtWorkers;
    }

    public int getNumUnivStudents()
    {
        int numUnivStudents = 0;
        for (int i = 1; i < persons.length; i++)
            numUnivStudents += persons[i].getPersonIsUniversityStudent();
        return numUnivStudents;
    }

    public int getNumNonWorkAdults()
    {
        int numNonWorkAdults = 0;
        for (int i = 1; i < persons.length; i++)
            numNonWorkAdults += persons[i].getPersonIsNonWorkingAdultUnder65();
        return numNonWorkAdults;
    }

    public int getNumAdults()
    {
        int numAdults = 0;
        for (int i = 1; i < persons.length; i++)
            numAdults += (persons[i].getPersonIsFullTimeWorker()
                    + persons[i].getPersonIsPartTimeWorker()
                    + persons[i].getPersonIsUniversityStudent()
                    + persons[i].getPersonIsNonWorkingAdultUnder65() + persons[i]
                    .getPersonIsNonWorkingAdultOver65());
        return numAdults;
    }

    public int getNumRetired()
    {
        int numRetired = 0;
        for (int i = 1; i < persons.length; i++)
            numRetired += persons[i].getPersonIsNonWorkingAdultOver65();
        return numRetired;
    }

    public int getNumDrivingStudents()
    {
        int numDrivingStudents = 0;
        for (int i = 1; i < persons.length; i++)
            numDrivingStudents += persons[i].getPersonIsStudentDriving();
        return numDrivingStudents;
    }

    public int getNumNonDrivingStudents()
    {
        int numNonDrivingStudents = 0;
        for (int i = 1; i < persons.length; i++)
            numNonDrivingStudents += persons[i].getPersonIsStudentNonDriving();
        return numNonDrivingStudents;
    }

    public int getNumPreschool()
    {
        int numPreschool = 0;
        for (int i = 1; i < persons.length; i++)
            numPreschool += persons[i].getPersonIsPreschoolChild();
        return numPreschool;
    }

    public int getNumHighSchoolGraduates()
    {
        int numGrads = 0;
        for (int i = 1; i < persons.length; i++)
            numGrads += persons[i].getPersonIsHighSchoolGraduate();
        return numGrads;
    }

    /**
     * joint tour frequency choice is not applied to a household unless it has: 2 or more persons, each with at least one out-of home activity, and at
     * least 1 of the persons not a pre-schooler.
     * */
    public int getValidHouseholdForJointTourFrequencyModel()
    {

        // return one of the following condition codes for this household producing
        // joint tours:
        // 1: household eligible for joint tour production
        // 2: household ineligible due to 1 person hh.
        // 3: household ineligible due to fewer than 2 persons traveling out of home
        // 4: household ineligible due to fewer than 1 non-preschool person traveling
        // out of home

        // no joint tours for single person household
        if (hhSize == 1) return 2;

        int leavesHome = 0;
        int nonPreSchoolerLeavesHome = 0;
        for (int i = 1; i < persons.length; i++)
        {
            if (!persons[i].getCdapActivity().equalsIgnoreCase("H"))
            {
                leavesHome++;
                if (persons[i].getPersonIsPreschoolChild() == 0) nonPreSchoolerLeavesHome++;
            }
        }

        // if the number of persons leaving home during the day is not at least 2, no
        // joint tours
        if (leavesHome < 2) return 3;

        // if the number of non-preschool persons leaving home during the day is not
        // at least 1, no joint tours
        if (nonPreSchoolerLeavesHome < 1) return 4;

        // if all conditions are met, we can apply joint tour frequency model to this
        // household
        return 1;

    }

    /**
     * return maximum periods of overlap between this person and other adult persons in the household.
     * 
     * @return the most number of periods mutually available between this person and other adult household members
     */
    public int getMaxAdultOverlaps()
    {
        return maxAdultOverlaps;
    }

    /**
     * return maximum periods of overlap between this person and other children in the household.
     * 
     * @return the most number of periods mutually available between this person and other child household members
     */
    public int getMaxChildOverlaps()
    {
        return maxChildOverlaps;
    }

    /**
     * return maximum periods of overlap between this person(adult/child) and other persons(child/adult) in the household.
     * 
     * @return the most number of periods mutually available between this person and other type household members
     */
    public int getMaxMixedOverlaps()
    {
        return maxMixedOverlaps;
    }

    public int getMaxJointTimeWindow(Tour t)
    {
        // get array of person array indices participating in joint tour
        int[] participatingPersonIndices = t.getPersonNumArray();

        // create an array to hold time window arrays for each participant
        int[][] personWindows = new int[participatingPersonIndices.length][];

        // get time window arrays for each participant
        int k = 0;
        for (int i : participatingPersonIndices)
            personWindows[k++] = persons[i].getTimeWindows();

        int count = 0;
        ;
        int maxCount = 0;
        // loop over time window intervals
        for (int w = 1; w < personWindows[0].length; w++)
        {

            // loop over party; determine if interval is available for everyone in party;
            boolean available = true;
            for (k = 0; k < personWindows.length; k++)
            {
                if (personWindows[k][w] > 0)
                {
                    available = false;
                    break;
                }
            }

            // if available for whole party, increment count; determine maximum continous time window available to whole party.
            if (available)
            {
                count++;
                if (count > maxCount) maxCount = count;
            } else
            {
                count = 0;
            }

        }

        return maxCount;
    }

    /**
     * @return number of adults in household with "M" or "N" activity pattern - that is, traveling adults.
     */
    public int getTravelActiveAdults()
    {

        int adultsStayingHome = 0;
        int adults = 0;
        for (int p = 1; p < persons.length; p++)
        {
            // person is an adult
            if (persons[p].getPersonIsAdult() == 1)
            {
                adults++;
                if (persons[p].getCdapActivity().equalsIgnoreCase("H")) adultsStayingHome++;
            }
        }

        // return the number of adults traveling = number of adults minus the number
        // of adults staying home.
        return adults - adultsStayingHome;

    }

    /**
     * @return number of children in household with "M" or "N" activity pattern - that is, traveling children.
     */
    public int getTravelActiveChildren()
    {

        int childrenStayingHome = 0;
        int children = 0;
        for (int p = 1; p < persons.length; p++)
        {
            // person is not an adult
            if (persons[p].getPersonIsAdult() == 0)
            {
                children++;
                if (persons[p].getCdapActivity().equalsIgnoreCase("H")) childrenStayingHome++;
            }
        }

        // return the number of adults traveling = number of adults minus the number
        // of adults staying home.
        return children - childrenStayingHome;

    }

    public void calculateTimeWindowOverlaps()
    {

        boolean pAdult;
        boolean qAdult;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;
        maxMixedOverlaps = 0;

        int[] maxAdultOverlapsP = new int[persons.length];
        int[] maxChildOverlapsP = new int[persons.length];

        // loop over persons in the household and count available time windows
        for (int p = 1; p < persons.length; p++)
        {

            // determine if person p is an adult -- that is, person is not any of the
            // three child types
            pAdult = persons[p].getPersonIsPreschoolChild() == 0
                    && persons[p].getPersonIsStudentNonDriving() == 0
                    && persons[p].getPersonIsStudentDriving() == 0;

            // loop over person indices to compute length of pairwise available time windows.
            for (int q = 1; q < persons.length; q++)
            {

                if (p == q) continue;

                // determine if person q is an adult -- that is, person is not any of the three child types
                qAdult = persons[q].getPersonIsPreschoolChild() == 0
                        && persons[q].getPersonIsStudentNonDriving() == 0
                        && persons[q].getPersonIsStudentDriving() == 0;

                // get the length of the maximum pairwise available time window between persons p and q.
                int maxWindow = persons[p].getMaximumContinuousPairwiseAvailableWindow(persons[q]
                        .getTimeWindows());

                // determine max time window overlap between adult pairs, children pairs, and mixed pairs in the household
                // for max windows in all pairs in hh, don't need to check q,p once we'alread done p,q, so skip q <= p.
                if (q > p)
                {
                    if (pAdult && qAdult)
                    {
                        if (maxWindow > maxAdultOverlaps) maxAdultOverlaps = maxWindow;
                    } else if (!pAdult && !qAdult)
                    {
                        if (maxWindow > maxChildOverlaps) maxChildOverlaps = maxWindow;
                    } else
                    {
                        if (maxWindow > maxMixedOverlaps) maxMixedOverlaps = maxWindow;
                    }
                }

                // determine the max time window overlap between this person and other household adults and children.
                if (qAdult)
                {
                    if (maxWindow > maxAdultOverlapsP[p]) maxAdultOverlapsP[p] = maxWindow;
                } else
                {
                    if (maxWindow > maxChildOverlapsP[p]) maxChildOverlapsP[p] = maxWindow;
                }

            } // end of person q

            // set person attributes
            persons[p].setMaxAdultOverlaps(maxAdultOverlapsP[p]);
            persons[p].setMaxChildOverlaps(maxChildOverlapsP[p]);

        } // end of person p

    }

    public boolean[] getAvailableJointTourTimeWindows(Tour t, int[] altStarts, int[] altEnds)
    {
        int[] participatingPersonIndices = t.getPersonNumArray();

        // availability array for each person
        boolean[][] availability = new boolean[participatingPersonIndices.length][];

        for (int i = 0; i < participatingPersonIndices.length; i++)
        {

            int personNum = participatingPersonIndices[i];
            Person person = persons[personNum];

            // availability array is 1-based indexing
            availability[i] = new boolean[altStarts.length + 1];

            for (int k = 1; k <= altStarts.length; k++)
            {
                int start = altStarts[k - 1];
                int end = altEnds[k - 1];
                availability[i][k] = person.isWindowAvailable(start, end);
            }

        }

        boolean[] jointAvailability = new boolean[availability[0].length];

        for (int k = 0; k < jointAvailability.length; k++)
        {
            jointAvailability[k] = true;
            for (int i = 0; i < participatingPersonIndices.length; i++)
            {
                if (!availability[i][k])
                {
                    jointAvailability[k] = false;
                    break;
                }
            }
        }

        return jointAvailability;

    }

    public void scheduleJointTourTimeWindows(Tour t, int start, int end)
    {
        int[] participatingPersonIndices = t.getPersonNumArray();
        for (int i : participatingPersonIndices)
        {
            Person person = persons[i];
            person.scheduleWindow(start, end);
        }
    }

    public void createJointTourArray()
    {
        jointTours = new Tour[0];
    }

    public void createJointTourArray(Tour tour1)
    {
        jointTours = new Tour[1];
        tour1.setTourOrigMgra(homeMgra);
        tour1.setTourDestMgra(0);
        jointTours[0] = tour1;
    }

    public void createJointTourArray(Tour tour1, Tour tour2)
    {
        jointTours = new Tour[2];
        tour1.setTourOrigMgra(homeMgra);
        tour1.setTourDestMgra(0);
        tour1.setTourId(0);
        tour2.setTourOrigMgra(homeMgra);
        tour2.setTourDestMgra(0);
        tour2.setTourId(1);
        jointTours[0] = tour1;
        jointTours[1] = tour2;
    }

    public Tour[] getJointTourArray()
    {
        return jointTours;
    }

    public void initializeForAoRestart()
    {
        jointTours = null;

        aoModelAutos = 0;
        cdapModelPattern = null;
        imtfModelPattern = 0;
        jtfModelPattern = null;

        tpRandomCount = 0;
        fpRandomCount = 0;
        ieRandomCount = 0;
        cdapRandomCount = 0;
        imtfRandomCount = 0;
        imtodRandomCount = 0;
        awfRandomCount = 0;
        awlRandomCount = 0;
        awtodRandomCount = 0;
        jtfRandomCount = 0;
        jtlRandomCount = 0;
        jtodRandomCount = 0;
        inmtfRandomCount = 0;
        inmtlRandomCount = 0;
        inmtodRandomCount = 0;
        stfRandomCount = 0;
        stlRandomCount = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;
        maxMixedOverlaps = 0;

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForAoRestart();

    }

    public void initializeForImtfRestart()
    {
        jointTours = null;

        imtfModelPattern = 0;
        jtfModelPattern = null;

        imtodRandomCount = 0;
        jtfRandomCount = 0;
        jtlRandomCount = 0;
        jtodRandomCount = 0;
        inmtfRandomCount = 0;
        inmtlRandomCount = 0;
        inmtodRandomCount = 0;
        awfRandomCount = 0;
        awlRandomCount = 0;
        awtodRandomCount = 0;
        stfRandomCount = 0;
        stlRandomCount = 0;

        maxAdultOverlaps = 0;
        maxChildOverlaps = 0;
        maxMixedOverlaps = 0;

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForImtfRestart();

    }

    public void initializeForJtfRestart()
    {

        jtfModelPattern = null;

        jtfRandomCount = 0;
        jtlRandomCount = 0;
        jtodRandomCount = 0;
        inmtfRandomCount = 0;
        inmtlRandomCount = 0;
        inmtodRandomCount = 0;
        awfRandomCount = 0;
        awlRandomCount = 0;
        awtodRandomCount = 0;
        stfRandomCount = 0;
        stlRandomCount = 0;

        initializeWindows();

        if (jointTours != null)
        {
            for (Tour t : jointTours)
            {
                t.clearStopModelResults();
            }
        }

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForJtfRestart();

        jointTours = null;

    }

    public void initializeForInmtfRestart()
    {

        inmtfRandomCount = 0;
        inmtlRandomCount = 0;
        inmtodRandomCount = 0;
        awfRandomCount = 0;
        awlRandomCount = 0;
        awtodRandomCount = 0;
        stfRandomCount = 0;
        stlRandomCount = 0;

        initializeWindows();

        if (jointTours != null)
        {
            for (Tour t : jointTours)
            {
                for (int i : t.getPersonNumArray())
                    persons[i].scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
                t.clearStopModelResults();
            }
        }

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForInmtfRestart();

    }

    public void initializeForAwfRestart()
    {

        awfRandomCount = 0;
        awlRandomCount = 0;
        awtodRandomCount = 0;
        stfRandomCount = 0;
        stlRandomCount = 0;

        initializeWindows();

        if (jointTours != null)
        {
            for (Tour t : jointTours)
            {
                for (int i : t.getPersonNumArray())
                    persons[i].scheduleWindow(t.getTourDepartPeriod(), t.getTourArrivePeriod());
                t.clearStopModelResults();
            }
        }

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForAwfRestart();

    }

    public void initializeForStfRestart()
    {

        stfRandomCount = 0;
        stlRandomCount = 0;

        for (int i = 1; i < persons.length; i++)
            persons[i].initializeForStfRestart();

    }

    public void logHouseholdObject(String titleString, Logger logger)
    {

        int totalChars = 72;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "H";

        logger.info(separater);
        logger.info(titleString);
        logger.info(separater);

        Household.logHelper(logger, "hhId: ", hhId, totalChars);
        Household.logHelper(logger, "debugChoiceModels: ", debugChoiceModels ? "True" : "False",
                totalChars);
        Household.logHelper(logger, "hhIncome: ", hhIncome, totalChars);
        Household.logHelper(logger, "hhIncomeInDollars: ", hhIncomeInDollars, totalChars);
        Household.logHelper(logger, "hhSize: ", hhSize, totalChars);
        Household.logHelper(logger, "hhType: ", hhType, totalChars);
        Household.logHelper(logger, "hhWorkers: ", hhWorkers, totalChars);
        Household.logHelper(logger, "homeTaz: ", homeTaz, totalChars);
        Household.logHelper(logger, "homeMgra: ", homeMgra, totalChars);
        Household.logHelper(logger, "homeWalkSubzone: ", homeWalkSubzone, totalChars);
        Household.logHelper(logger, "aoModelAutos: ", aoModelAutos, totalChars);
        Household.logHelper(logger, "cdapModelPattern: ", cdapModelPattern, totalChars);
        Household.logHelper(logger, "imtfModelPattern: ", imtfModelPattern, totalChars);
        Household.logHelper(logger, "jtfModelPattern: ", jtfModelPattern, totalChars);
        Household.logHelper(logger, "randomCount: ", randomCount, totalChars);
        if (uwslRandomCountList.size() > 0)
        {
            for (int i : uwslRandomCountList.keySet())
                Household.logHelper(logger, String.format("uwslRandomCount[%d]: ", i),
                        uwslRandomCountList.get(i), totalChars);
        } else
        {
            Household.logHelper(logger, "uwslRandomCount[0]: ", 0, totalChars);
        }
        Household.logHelper(logger, "aoRandomCount: ", aoRandomCount, totalChars);
        Household.logHelper(logger, "tpRandomCount: ", tpRandomCount, totalChars);
        Household.logHelper(logger, "fpRandomCount: ", fpRandomCount, totalChars);
        Household.logHelper(logger, "ieRandomCount: ", ieRandomCount, totalChars);
        Household.logHelper(logger, "cdapRandomCount: ", cdapRandomCount, totalChars);
        Household.logHelper(logger, "imtfRandomCount: ", imtfRandomCount, totalChars);
        Household.logHelper(logger, "imtodRandomCount: ", imtodRandomCount, totalChars);
        Household.logHelper(logger, "awfRandomCount: ", awfRandomCount, totalChars);
        Household.logHelper(logger, "awlRandomCount: ", awlRandomCount, totalChars);
        Household.logHelper(logger, "awtodRandomCount: ", awtodRandomCount, totalChars);
        Household.logHelper(logger, "jtfRandomCount: ", jtfRandomCount, totalChars);
        Household.logHelper(logger, "jtlRandomCount: ", jtlRandomCount, totalChars);
        Household.logHelper(logger, "jtodRandomCount: ", jtodRandomCount, totalChars);
        Household.logHelper(logger, "inmtfRandomCount: ", inmtfRandomCount, totalChars);
        Household.logHelper(logger, "inmtlRandomCount: ", inmtlRandomCount, totalChars);
        Household.logHelper(logger, "inmtodRandomCount: ", inmtodRandomCount, totalChars);
        Household.logHelper(logger, "stfRandomCount: ", stfRandomCount, totalChars);
        Household.logHelper(logger, "stlRandomCount: ", stlRandomCount, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "maxMixedOverlaps: ", maxMixedOverlaps, totalChars);

        String tempString = String.format("Joint Tours[%s]:",
                jointTours == null ? "" : String.valueOf(jointTours.length));
        logger.info(tempString);

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void logPersonObject(String titleString, Logger logger, Person person)
    {

        int totalChars = 114;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "P";

        logger.info(separater);
        logger.info(titleString);
        logger.info(separater);

        person.logPersonObject(logger, totalChars);

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void logTourObject(String titleString, Logger logger, Person person, Tour tour)
    {

        int totalChars = 119;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "T";

        logger.info(separater);
        logger.info(titleString);
        logger.info(separater);

        person.logTourObject(logger, totalChars, tour);

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void logStopObject(String titleString, Logger logger, Stop stop,
            ModelStructure modelStructure)
    {

        int totalChars = 119;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "S";

        logger.info(separater);
        logger.info(titleString);
        logger.info(separater);

        stop.logStopObject(logger, totalChars);

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public void logEntireHouseholdObject(String titleString, Logger logger)
    {

        int totalChars = 60;
        String separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "=";

        logger.info(separater);
        logger.info(titleString);
        logger.info(separater);

        separater = "";
        for (int i = 0; i < totalChars; i++)
            separater += "-";

        Household.logHelper(logger, "hhId: ", hhId, totalChars);
        Household.logHelper(logger, "debugChoiceModels: ", debugChoiceModels ? "True" : "False",
                totalChars);
        Household.logHelper(logger, "hhIncome: ", hhIncome, totalChars);
        Household.logHelper(logger, "hhIncomeInDollars: ", hhIncomeInDollars, totalChars);
        Household.logHelper(logger, "hhSize: ", hhSize, totalChars);
        Household.logHelper(logger, "hhType: ", hhType, totalChars);
        Household.logHelper(logger, "hhWorkers: ", hhWorkers, totalChars);
        Household.logHelper(logger, "homeTaz: ", homeTaz, totalChars);
        Household.logHelper(logger, "homeMgra: ", homeMgra, totalChars);
        Household.logHelper(logger, "homeWalkSubzone: ", homeWalkSubzone, totalChars);
        Household.logHelper(logger, "aoModelAutos: ", aoModelAutos, totalChars);
        Household.logHelper(logger, "cdapModelPattern: ", cdapModelPattern, totalChars);
        Household.logHelper(logger, "imtfModelPattern: ", imtfModelPattern, totalChars);
        Household.logHelper(logger, "jtfModelPattern: ", jtfModelPattern, totalChars);
        Household.logHelper(logger, "randomCount: ", randomCount, totalChars);
        if (uwslRandomCountList.size() > 0)
        {
            for (int i : uwslRandomCountList.keySet())
                Household.logHelper(logger, String.format("uwslRandomCount[%d]: ", i),
                        uwslRandomCountList.get(i), totalChars);
        } else
        {
            Household.logHelper(logger, "uwslRandomCount[0]: ", 0, totalChars);
        }
        Household.logHelper(logger, "aoRandomCount: ", aoRandomCount, totalChars);
        Household.logHelper(logger, "tpRandomCount: ", tpRandomCount, totalChars);
        Household.logHelper(logger, "fpRandomCount: ", fpRandomCount, totalChars);
        Household.logHelper(logger, "ieRandomCount: ", ieRandomCount, totalChars);
        Household.logHelper(logger, "cdapRandomCount: ", cdapRandomCount, totalChars);
        Household.logHelper(logger, "imtfRandomCount: ", imtfRandomCount, totalChars);
        Household.logHelper(logger, "imtodRandomCount: ", imtodRandomCount, totalChars);
        Household.logHelper(logger, "awfRandomCount: ", awfRandomCount, totalChars);
        Household.logHelper(logger, "awlRandomCount: ", awlRandomCount, totalChars);
        Household.logHelper(logger, "awtodRandomCount: ", awtodRandomCount, totalChars);
        Household.logHelper(logger, "jtfRandomCount: ", jtfRandomCount, totalChars);
        Household.logHelper(logger, "jtlRandomCount: ", jtlRandomCount, totalChars);
        Household.logHelper(logger, "jtodRandomCount: ", jtodRandomCount, totalChars);
        Household.logHelper(logger, "inmtfRandomCount: ", inmtfRandomCount, totalChars);
        Household.logHelper(logger, "inmtlRandomCount: ", inmtlRandomCount, totalChars);
        Household.logHelper(logger, "inmtodRandomCount: ", inmtodRandomCount, totalChars);
        Household.logHelper(logger, "stfRandomCount: ", stfRandomCount, totalChars);
        Household.logHelper(logger, "stlRandomCount: ", stlRandomCount, totalChars);
        Household.logHelper(logger, "maxAdultOverlaps: ", maxAdultOverlaps, totalChars);
        Household.logHelper(logger, "maxChildOverlaps: ", maxChildOverlaps, totalChars);
        Household.logHelper(logger, "maxMixedOverlaps: ", maxMixedOverlaps, totalChars);

        if (jointTours != null)
        {
            logger.info("Joint Tours:");
            if (jointTours.length > 0)
            {
                for (int i = 0; i < jointTours.length; i++)
                    jointTours[i].logEntireTourObject(logger);
            } else logger.info("     No joint tours");
        } else logger.info("     No joint tours");

        logger.info("Person Objects:");
        for (int i = 1; i < persons.length; i++)
            persons[i].logEntirePersonObject(logger);

        logger.info(separater);
        logger.info("");
        logger.info("");

    }

    public static void logHelper(Logger logger, String label, int value, int totalChars)
    {
        int labelChars = label.length() + 2;
        int remainingChars = totalChars - labelChars - 4;
        String formatString = String.format("     %%%ds %%%dd", label.length(), remainingChars);
        String logString = String.format(formatString, label, value);
        logger.info(logString);
    }

    public static void logHelper(Logger logger, String label, String value, int totalChars)
    {
        int labelChars = label.length() + 2;
        int remainingChars = totalChars - labelChars - 4;
        String formatString = String.format("     %%%ds %%%ds", label.length(), remainingChars);
        String logString = String.format(formatString, label, value);
        logger.info(logString);
    }

    public enum HouseholdType
    {
        nul, FAMILY_MARRIED, FAMILY_MALE_NO_WIFE, FAMILY_FEMALE_NO_HUSBAND, NON_FAMILY_MALE_ALONE, NON_FAMILY_MALE_NOT_ALONE, NON_FAMILY_FEMALE_ALONE, NON_FAMILY_FEMALE_NOT_ALONE
    }

}
