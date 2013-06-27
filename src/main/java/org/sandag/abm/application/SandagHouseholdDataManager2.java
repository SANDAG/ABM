package org.sandag.abm.application;

import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.HouseholdDataManager;
import org.sandag.abm.ctramp.Person;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.IndexSort;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from synthetic
 *         population files.
 */
public class SandagHouseholdDataManager2
        extends HouseholdDataManager
{

    public static final String HH_DATA_SERVER_NAME       = SandagHouseholdDataManager.class.getCanonicalName();
    public static final String HH_DATA_SERVER_ADDRESS    = "127.0.0.1";
    public static final int    HH_DATA_SERVER_PORT       = 1139;

    public static final String PROPERTIES_OCCUP_CODES    = "PopulationSynthesizer.OccupCodes";
    public static final String PROPERTIES_INDUSTRY_CODES = "PopulationSynthesizer.IndustryCodes";

    public SandagHouseholdDataManager2()
    {
        super();
    }

    /**
     * Associate data in hh and person TableDataSets read from synthetic population
     * files with Household objects and Person objects with Households.
     * 
     */
    public void mapTablesToHouseholdObjects()
    {

        logger.info("mapping popsyn household and person data records to objects.");

        int id = -1;

        int invalidPersonTypeCount1 = 0;
        int invalidPersonTypeCount2 = 0;
        int invalidPersonTypeCount3 = 0;

        // read the correspondence files for mapping persons to occupation and
        HashMap<String, Integer> occCodes = readOccupCorrespondenceData();
        int[] indCodes = readIndustryCorrespondenceData();

        // get the maximum HH id value to use to dimension the hhIndex correspondence
        // array.  The hhIndex array will store the hhArray index number for the given
        // hh index.
        int maxHhId = 0;
        int hhIDColumn = hhTable.getColumnPosition(HH_ID_FIELD_NAME);

        for (int r = 1; r <= hhTable.getRowCount(); r++)
        {
            id = (int) hhTable.getValueAt(r,hhIDColumn);
            if (id > maxHhId) maxHhId = id;
        }
        hhIndexArray = new int[maxHhId + 1];

        // get an index array for households sorted in random order - to remove the original order
        int[] firstSortedIndices = getRandomOrderHhIndexArray(hhTable.getRowCount());

        // get a second index array for households sorted in random order - to select a sample from the randomly ordered hhs
        int[] randomSortedIndices = getRandomOrderHhIndexArray(hhTable.getRowCount());

        hhs = null;

        int numHouseholdsInSample = (int) (hhTable.getRowCount() * sampleRate);
        Household[] hhArray = new Household[numHouseholdsInSample];


        
//        String outputFileName = "sample_hh_mgra_taz_seed_" + sampleSeed + ".csv";
//        PrintWriter outStream = null;
//        try {
//            outStream = new PrintWriter(new BufferedWriter(new FileWriter(new File(outputFileName))));
//            outStream.println("i,mgra,taz");
//        }
//        catch (IOException e) {
//            logger.fatal(String.format("Exception occurred opening output skims file: %s.", outputFileName));
//            throw new RuntimeException(e);
//        }
        
        int[] tempFreqArray = new int[40000];
        int[] hhOriginSortArray = new int[numHouseholdsInSample];
        for (int i = 0; i < numHouseholdsInSample; i++)
        {
            int r = firstSortedIndices[randomSortedIndices[i]] + 1;
//            int hhId = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_ID_FIELD_NAME));
            int hhMgra = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_HOME_MGRA_FIELD_NAME));
            int hhTaz = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_HOME_TAZ_FIELD_NAME));
            hhOriginSortArray[i] = hhMgra;
            tempFreqArray[hhMgra]++;

//            outStream.println(i + "," + hhMgra + "," + hhTaz);
        }

//        outStream.close();
//        System.exit(1);
        
        
        
        int mgrasInSample = 0;
        for (int i = 0; i < tempFreqArray.length; i++)
        {
            if ( tempFreqArray[i] > 0 )
                mgrasInSample++;
        }        
        logger.info( mgrasInSample + " unique MGRA values in the " + (sampleRate*100) + "% sample." );
        
        // get an index array for households sorted in order of home mgra
        int[] newOrder = new int[numHouseholdsInSample];
        int[] sortedIndices = IndexSort.indexSort(hhOriginSortArray);
        for (int i = 0; i < sortedIndices.length; i++)
        {
            int k = sortedIndices[i];
            newOrder[k] = i;
        }

        // for each household in the sample
        for (int i = 0; i < numHouseholdsInSample; i++)
        {
            int r = firstSortedIndices[randomSortedIndices[i]] + 1;
            try
            {
                // create a Household object
                Household hh = new Household(modelStructure);

                // get required values from table record and store in Household
                // object
                id = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_ID_FIELD_NAME));
                hh.setHhId(id, inputRandomSeed);

                // set the household in the hhIndexArray in random order
                int newIndex = newOrder[i];
                hhIndexArray[hh.getHhId()] = newIndex;

                int htaz = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_HOME_TAZ_FIELD_NAME));
                hh.setHhTaz(htaz);

                int hmgra = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_HOME_MGRA_FIELD_NAME));
                hh.setHhMgra(hmgra);

                // autos could be modeled or from PUMA
                int numAutos = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_AUTOS_FIELD_NAME));
                hh.setHhAutos(numAutos);

                // set the hhSize variable and create Person objects for each person
                int numPersons = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_SIZE_FIELD_NAME));
                hh.setHhSize(numPersons);

                int numWorkers = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_WORKERS_FIELD_NAME));
                hh.setHhWorkers(numWorkers);

                int incomeCat = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_INCOME_CATEGORY_FIELD_NAME));
                hh.setHhIncome(incomeCat);

                int incomeInDollars = (int) hhTable.getValueAt(r, hhTable.getColumnPosition(HH_INCOME_DOLLARS_FIELD_NAME));
                hh.setHhIncomeInDollars(incomeInDollars);

                // 0=Housing unit, 1=Institutional group quarters, 2=Noninstitutional
                // group quarters
                int unitType = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_UNITTYPE_FIELD_NAME));
                hh.setUnitType(unitType);

                // 1=Family household:married-couple, 2=Family household:male
                // householder,no wife present, 3=Family household:female
                // householder,no
                // husband present
                // 4=Nonfamily household:male householder, living alone, 5=Nonfamily
                // household:male householder, not living alone,
                // 6=Nonfamily household:female householder, living alone,
                // 7=Nonfamily household:female householder, not living alone
                int type = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_TYPE_FIELD_NAME));
                hh.setHhType(type);

                // 1=mobile home, 2=one-family house detached from any other house,
                // 3=one-family house attached to one or more houses,
                // 4=building with 2 apartments, 5=building with 3 or 4 apartments,
                // 6=building with 5 to 9 apartments,
                // 7=building with 10 to 19 apartments, 8=building with 20 to 49
                // apartments,
                // 9=building with 50 or more apartments, 10=Boat,RV,van,etc.
                int bldgsz = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(HH_BLDGSZ_FIELD_NAME));
                hh.setHhBldgsz(bldgsz);

                hh.initializeWindows();
                hhArray[newIndex] = hh;

            } catch (Exception e)
            {

                logger.fatal(String.format(
                    "exception caught mapping household data record to a Household object, r=%d, id=%d.",
                    r, id));
                throw new RuntimeException(e);

            }

        }

        int[] personHhStart = new int[maxHhId + 1];
        int[] personHhEnd = new int[maxHhId + 1];

        // get hhid for person record 1
        int hhid = (int) personTable.getValueAt(1, personTable
                .getColumnPosition(PERSON_HH_ID_FIELD_NAME));
        personHhStart[hhid] = 1;
        int oldHhid = hhid;

        for (int r = 1; r <= personTable.getRowCount(); r++)
        {

            // get the Household object for this person data to be stored in
            hhid = (int) personTable.getValueAt(r, personTable
                    .getColumnPosition(PERSON_HH_ID_FIELD_NAME));

            if (hhid != oldHhid)
            {
                personHhEnd[oldHhid] = r - 1;
                oldHhid = hhid;
                personHhStart[hhid] = r;
            }

        }
        personHhEnd[hhid] = personTable.getRowCount();

        int r = 0;
        int p = 0;
        int persId = 0;
        int persNum = 0;
        int fieldCount = 0;

        for (int i = 0; i < numHouseholdsInSample; i++)
        {

            try
            {

                r = firstSortedIndices[randomSortedIndices[i]] + 1;

                hhid = (int) hhTable.getValueAt(r, hhTable
                        .getColumnPosition(PERSON_HH_ID_FIELD_NAME));

                int index = hhIndexArray[hhid];
                Household hh = hhArray[index];

                persNum = 1;

                for (p = personHhStart[hhid]; p <= personHhEnd[hhid]; p++)
                {

                    fieldCount = 0;
                    
                    // get the Person object for this person data to be stored in
                    persId = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_PERSON_ID_FIELD_NAME));
                    Person person = hh.getPerson(persNum++);
                    person.setPersId(persId);
                    fieldCount++;

                    // get required values from table record and store in Person
                    // object
                    int age = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_AGE_FIELD_NAME));
                    person.setPersAge(age);
                    fieldCount++;

                    int gender = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_GENDER_FIELD_NAME));
                    person.setPersGender(gender);
                    fieldCount++;
/*
                    int occcen1 = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_SOC_FIELD_NAME));
                    int pecasOccup = occCodes[occcen1];
*/
                    int military = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_MILITARY_FIELD_NAME));
                    int pecasOccup=0;
                    
                    String occsoc = personTable.getStringValueAt(p, personTable.getColumnPosition(PERSON_SOC_FIELD_NAME));

                    int indcen = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_INDCEN_FIELD_NAME));
                    int activityCode = indCodes[indcen];

                    if(military==1)  // in active military
                    	pecasOccup=56;
                    else if (military!=1 &&  indcen>=967 && indcen<=991)  //not active military but military contractor
                    	pecasOccup=56;
                    else{
                    	if(!occCodes.containsKey(occsoc)){
                    		logger.fatal("Error:  Occupation code "+occsoc+" for hhid "+hhid+" person "+p+" not found in occupation file");
                            throw new RuntimeException();
                    	}
                    	pecasOccup = occCodes.get(occsoc);  //everyone else
                        if (pecasOccup == 0) logger.warn("pecasOccup==0 for occsoc==" + occsoc);
                    }

                    person.setPersActivityCode(activityCode);
                    fieldCount++;

                    person.setPersPecasOccup(pecasOccup);
                    fieldCount++;

                    /*  These are the old codes, based upon census occupation definitions
                    if ((pecasOccup == 71)
                            && (activityCode == 2 || activityCode == 4 || activityCode == 6
                                    || activityCode == 8 || activityCode == 29)) activityCode++;

                    if ((pecasOccup == 76)
                            && (activityCode == 3 || activityCode == 5 || activityCode == 7
                                    || activityCode == 9 || activityCode == 30)) activityCode--;

                    if ((pecasOccup == 76) && (activityCode == 13)) activityCode = 14;

                    if ((pecasOccup == 71) && (activityCode == 14)) activityCode = 13;

                    if ((pecasOccup == 75) && (activityCode == 18)) activityCode = 22;

                    if ((pecasOccup == 71) && (activityCode == 22)) activityCode = 18;

                    if (activityCode == 28) pecasOccup = 77;
                     */
                    

                    // Employment status (1-employed FT, 2-employed PT, 3-not
                    // employed, 4-under age 16)
                    int empCat = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME));
                    person.setPersEmploymentCategory(empCat);
                    fieldCount++;

                    // Student status (1 - student in grade or high school; 2 -
                    // student in college or higher; 3 - not a student)
                    int studentCat = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_STUDENT_CATEGORY_FIELD_NAME));
                    person.setPersStudentCategory(studentCat);
                    fieldCount++;

                    // Person type (1-FT worker age 16+, 2-PT worker nonstudent age
                    // 16+, 3-university student, 4-nonworker nonstudent age 16-64,
                    // 5-nonworker nonstudent age 65+,
                    // 6-"age 16-19 student, not FT wrkr or univ stud", 7-age 6-15
                    // schpred, 8 under age 6 presch
                    int personType = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_TYPE_CATEGORY_FIELD_NAME));
                    person.setPersonTypeCategory(personType);
                    fieldCount++;

                    // Person educational attainment level to determine high school
                    // graduate status ( < 9 - not a graduate, 10+ - high school
                    // graduate
                    // and beyond)
                    int educ = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_EDUCATION_ATTAINMENT_FIELD_NAME));
                    if (educ >= 9)
                        person.setPersonIsHighSchoolGraduate(true);
                    else
                        person.setPersonIsHighSchoolGraduate(false);
                    fieldCount++;

                    // Person educational attainment level to determine higher
                    // education status ( > 12 - at least a bachelor's degree )
                    if (educ >= 13)
                        person.setPersonHasBachelors(true);
                    else
                        person.setPersonHasBachelors(false);
                    fieldCount++;

                    // Person grade enrolled in ( 0-"not enrolled", 1-"preschool",
                    // 2-"Kindergarten", 3-"Grade 1 to grade 4",
                    // 4-"Grade 5 to grade 8", 5-"Grade 9 to grade 12",
                    // 6-"College undergraduate", 7-"Graduate or professional school"
                    // )
                    int grade = (int) personTable.getValueAt(p, personTable.getColumnPosition(PERSON_GRADE_ENROLLED_FIELD_NAME));
                    person.setPersonIsGradeSchool(false);
                    person.setPersonIsHighSchool(false);
                    if (grade >= 2 && grade <= 4) {
                        // change person type if person was 5 or under but enrolled in K-8.
                        if ( person.getPersonIsPreschoolChild() == 1 )
                            person.setPersonTypeCategory( Person.PersonType.Student_age_6_15_schpred.ordinal() );
                        
                        person.setPersonIsGradeSchool(true);
                    }
                    else if (grade == 5) {
                        person.setPersonIsHighSchool(true);
                    }
                    fieldCount++;

                    // if person is a university student but has school age student
                    // category value, reset student category value
                    if (personType == Person.PersonType.University_student.ordinal()
                            && studentCat != Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER
                                    .ordinal())
                    {
                        studentCat = Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER.ordinal();
                        person.setPersStudentCategory(studentCat);
                        invalidPersonTypeCount1++;
                    }
                    // if person is a student of any kind but has full-time
                    // employment status, reset student category value to non-student
                    else if (studentCat != Person.StudentStatus.NON_STUDENT.ordinal()
                            && empCat == Person.EmployStatus.FULL_TIME.ordinal())
                    {
                        studentCat = Person.StudentStatus.NON_STUDENT.ordinal();
                        person.setPersStudentCategory(studentCat);
                        invalidPersonTypeCount2++;
                    }
                    fieldCount++;

                    // check consistency of student category and person type
                    if (studentCat == Person.StudentStatus.NON_STUDENT.ordinal())
                    {

                        if (person.getPersonIsStudentNonDriving() == 1
                                || person.getPersonIsStudentDriving() == 1)
                        {
                            studentCat = Person.StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS.ordinal();
                            person.setPersStudentCategory(studentCat);
                            invalidPersonTypeCount3++;
                        }

                    }
                    fieldCount++;

                }

            } catch (Exception e)
            {

                logger.fatal(String.format(
                    "exception caught mapping person data record to a Person object, i=%d, r=%d, p=%d, hhid=%d, persid=%d, persnum=%d, fieldCount=%d.",
                    i, r, p, hhid, persId, persNum, fieldCount));
                throw new RuntimeException(e);

            }

        } // person loop

        hhs = hhArray;

        logger.warn(invalidPersonTypeCount1 + " person type = university and student category = non-student person records had their student category changed to university or higher.");
        logger.warn(invalidPersonTypeCount2 + " Student category = student and employment category = full-time worker person records had their student category changed to non-student.");
        logger.warn(invalidPersonTypeCount3 + " Student category = non-student and person type = student person records had their student category changed to student high school or less.");

        //logger.info("Setting distributed values of time. "); 
        //setDistributedValuesOfTime(); 
        
    }

    /**
     * if called, must be called after readData so that the size of the full
     * population is known.
     * 
     * @param hhFileName
     * @param persFileName
     * @param numHhs
     */
    public void createSamplePopulationFiles(String hhFileName, String persFileName,
            String newHhFileName, String newPersFileName, int numHhs)
    {

        int maximumHhId = 0;
        for (int i = 0; i < hhs.length; i++)
        {
            int id = hhs[i].getHhId();
            if (id > maximumHhId) maximumHhId = id;
        }

        int[] testHhs = new int[maximumHhId + 1];

        int[] sortedIndices = getRandomOrderHhIndexArray(hhs.length);

        for (int i = 0; i < numHhs; i++)
        {
            int k = sortedIndices[i];
            int hhId = hhs[k].getHhId();
            testHhs[hhId] = 1;
        }

        String hString = "";
        int hCount = 0;
        try
        {

            logger.info(String.format("writing sample household file for %d households", numHhs));

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(newHhFileName)));
            BufferedReader in = new BufferedReader(new FileReader(hhFileName));

            // read headers and write to output files
            hString = in.readLine();
            out.write(hString + "\n");
            hCount++;
            int count = 0;

            while ((hString = in.readLine()) != null)
            {
                hCount++;
                int endOfField = hString.indexOf(',');
                int hhId = Integer.parseInt(hString.substring(0, endOfField));

                // if it's a sample hh, write the hh and the person records
                if (testHhs[hhId] == 1)
                {
                    out.write(hString + "\n");
                    count++;
                    if (count == numHhs) break;
                }
            }

            out.close();

        } catch (IOException e)
        {
            logger.fatal("IO Exception caught creating sample synpop household file.");
            logger.fatal(String.format("reading hh file = %s, writing sample hh file = %s.",
                    hhFileName, newHhFileName));
            logger.fatal(String.format("hString = %s, hCount = %d.", hString, hCount));
        }

        String pString = "";
        int pCount = 0;
        try
        {

            logger.info(String.format("writing sample person file for selected households"));

            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(newPersFileName)));
            BufferedReader in = new BufferedReader(new FileReader(persFileName));

            // read headers and write to output files
            pString = in.readLine();
            out.write(pString + "\n");
            pCount++;
            int count = 0;
            int oldId = 0;
            while ((pString = in.readLine()) != null)
            {
                pCount++;
                int endOfField = pString.indexOf(',');
                int hhId = Integer.parseInt(pString.substring(0, endOfField));

                // if it's a sample hh, write the hh and the person records
                if (testHhs[hhId] == 1)
                {
                    out.write(pString + "\n");
                    if (hhId > oldId) count++;
                } else
                {
                    if (count == numHhs) break;
                }

                oldId = hhId;

            }

            out.close();

        } catch (IOException e)
        {
            logger.fatal("IO Exception caught creating sample synpop person file.");
            logger.fatal(String.format(
                    "reading person file = %s, writing sample person file = %s.", persFileName,
                    newPersFileName));
            logger.fatal(String.format("pString = %s, pCount = %d.", pString, pCount));
        }

    }

    public static void main(String args[]) throws Exception
    {

        String serverAddress = HH_DATA_SERVER_ADDRESS;
        int serverPort = HH_DATA_SERVER_PORT;

        // optional arguments
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].equalsIgnoreCase("-hostname"))
            {
                serverAddress = args[i + 1];
            }

            if (args[i].equalsIgnoreCase("-port"))
            {
                serverPort = Integer.parseInt(args[i + 1]);
            }
        }

        Remote.config(serverAddress, serverPort, null, 0);

        SandagHouseholdDataManager2 hhDataManager = new SandagHouseholdDataManager2();

        ItemServer.bind(hhDataManager, HH_DATA_SERVER_NAME);

        System.out.println(String.format( "SandagHouseholdDataManager2 server class started on: %s:%d", serverAddress, serverPort) );

    }

    public int[] getJointToursByHomeMgra(String purposeString)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * This method reads a cross-walk file between the occsoc code in Census and the PECAS occupation categories.
     * It stores the result in a HashMap and returns it.
     * 
     * @return
     */
    private HashMap<String, Integer> readOccupCorrespondenceData()
    {

        TableDataSet occTable = null;

        // construct input household file name from properties file values
        String occupFileName = propertyMap.get(PROPERTIES_OCCUP_CODES);
        String fileName = projectDirectory + "/" + occupFileName;

        try
        {
            logger.info("reading occupation codes data file for creating occupation segments.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            occTable = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String.format(
                    "Exception occurred occupation codes data file: %s into TableDataSet object.",
                    fileName));
            throw new RuntimeException(e);
        }

        HashMap<String, Integer> occMap = new HashMap<String, Integer>();
        
        for(int i=1;i<=occTable.getRowCount();++i){
        	
        	String soc = occTable.getStringValueAt(i, "occsoc5");
        	int occ = (int)occTable.getValueAt(i, "commodity_id");
        	occMap.put(soc,occ);
        }
        
        return occMap;
    }

    private int[] readIndustryCorrespondenceData()
    {

        TableDataSet indTable = null;

        // construct input household file name from properties file values
        String indFileName = propertyMap.get(PROPERTIES_INDUSTRY_CODES);
        String fileName = projectDirectory + "/" + indFileName;

        try
        {
            logger.info("reading industry codes data file for creating industry segments.");
            OLD_CSVFileReader reader = new OLD_CSVFileReader();
            reader.setDelimSet("," + reader.getDelimSet());
            indTable = reader.readFile(new File(fileName));
        } catch (Exception e)
        {
            logger.fatal(String.format("Exception occurred reading indistry codes data file: %s into TableDataSet object.", fileName));
            throw new RuntimeException(e);
        }

        // get the array of indices from the TableDataSet
        int[] indcenCol = indTable.getColumnAsInt("indcen");
        int[] activityCol = indTable.getColumnAsInt("activity_code");

        // get the max index value, to use for array dimensions
        int maxInd = 0;
        for (int ind : indcenCol)
            if (ind > maxInd) maxInd = ind;

        int[] indcenIndustry = new int[maxInd + 1];
        for (int i = 0; i < indcenCol.length; i++)
        {
            int index = indcenCol[i];
            int value = activityCol[i];
            indcenIndustry[index] = value;
        }

        return indcenIndustry;
    }

}
