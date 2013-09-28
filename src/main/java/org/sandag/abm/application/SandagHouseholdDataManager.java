package org.sandag.abm.application;

import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.HouseholdDataManager;
import org.sandag.abm.ctramp.Person;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

/**
 * @author Jim Hicks
 * 
 *         Class for managing household and person object data read from
 *         synthetic population files.
 */
public class SandagHouseholdDataManager extends HouseholdDataManager {

	public static final String HH_DATA_SERVER_NAME = SandagHouseholdDataManager.class
			.getCanonicalName();
	public static final String HH_DATA_SERVER_ADDRESS = "127.0.0.1";
	public static final int HH_DATA_SERVER_PORT = 1139;

	public static final String PROPERTIES_OCCUP_CODES = "PopulationSynthesizer.OccupCodes";
	public static final String PROPERTIES_INDUSTRY_CODES = "PopulationSynthesizer.IndustryCodes";

	public SandagHouseholdDataManager() {
		super();
	}

	/**
	 * Associate data in hh and person TableDataSets read from synthetic
	 * population files with Household objects and Person objects with
	 * Households.
	 * 
	 */
	public void mapTablesToHouseholdObjects() {

		logger.info("mapping popsyn household and person data records to objects.");

		int id = -1;
		Household[] hhArray = new Household[hhTable.getRowCount()];

		int invalidPersonTypeCount1 = 0;
		int invalidPersonTypeCount2 = 0;
		int invalidPersonTypeCount3 = 0;

		// read the corrrespondence files for mapping persons to occupation and
		int[] occCodes = readOccupCorrespondenceData();
		int[] indCodes = readIndustryCorrespondenceData();

		// get the maximum HH id value to use to dimension the hhIndex
		// correspondence
		// array.
		// the hhIndex array will store the hhArray index number for the given
		// hh
		// index.
		int maxHhId = 0;
		for (int r = 1; r <= hhTable.getRowCount(); r++) {
			id = (int) hhTable.getValueAt(r,
					hhTable.getColumnPosition(HH_ID_FIELD_NAME));
			if (id > maxHhId)
				maxHhId = id;
		}
		hhIndexArray = new int[maxHhId + 1];
		int[] sortedIndices = getRandomOrderHhIndexArray(hhTable.getRowCount());

		// for each household table record
		for (int r = 1; r <= hhTable.getRowCount(); r++) {

			try {

				// create a Household object
				Household hh = new Household(modelStructure);

				// get required values from table record and store in Household
				// object
				id = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_ID_FIELD_NAME));
				hh.setHhId(id, inputRandomSeed);

				// set the household in the hhIndexArray in random order
				int index = sortedIndices[r - 1];
				hhIndexArray[hh.getHhId()] = index;

				int htaz = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_HOME_TAZ_FIELD_NAME));
				hh.setHhTaz(htaz);

				int hmgra = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_HOME_MGRA_FIELD_NAME));
				hh.setHhMgra(hmgra);

				double rn = hh.getHhRandom().nextDouble();
				int origWalkSubzone = getInitialOriginWalkSegment(htaz, rn);
				hh.setHhWalkSubzone(origWalkSubzone);

				// autos could be modeled or from PUMA
				int numAutos = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_AUTOS_FIELD_NAME));
				hh.setHhAutos(numAutos);

				// set the hhSize variable and create Person objects for each
				// person
				int numPersons = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_SIZE_FIELD_NAME));
				hh.setHhSize(numPersons);

				int numWorkers = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_WORKERS_FIELD_NAME));
				hh.setHhWorkers(numWorkers);

				int incomeCat = (int) hhTable.getValueAt(r, hhTable
						.getColumnPosition(HH_INCOME_CATEGORY_FIELD_NAME));
				hh.setHhIncome(incomeCat);

				int incomeInDollars = (int) hhTable
						.getValueAt(
								r,
								hhTable.getColumnPosition(HH_INCOME_DOLLARS_FIELD_NAME));
				hh.setHhIncomeInDollars(incomeInDollars);

				// 0=Housing unit, 1=Institutional group quarters,
				// 2=Noninstitutional
				// group quarters
				int unitType = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_UNITTYPE_FIELD_NAME));
				hh.setUnitType(unitType);

				// 1=Family household:married-couple, 2=Family household:male
				// householder,no wife present, 3=Family household:female
				// householder,no
				// husband present
				// 4=Nonfamily household:male householder, living alone,
				// 5=Nonfamily
				// household:male householder, not living alone,
				// 6=Nonfamily household:female householder, living alone,
				// 7=Nonfamily household:female householder, not living alone
				int type = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_TYPE_FIELD_NAME));
				hh.setHhType(type);

				// 1=mobile home, 2=one-family house detached from any other
				// house,
				// 3=one-family house attached to one or more houses,
				// 4=building with 2 apartments, 5=building with 3 or 4
				// apartments,
				// 6=building with 5 to 9 apartments,
				// 7=building with 10 to 19 apartments, 8=building with 20 to 49
				// apartments,
				// 9=building with 50 or more apartments, 10=Boat,RV,van,etc.
				int bldgsz = (int) hhTable.getValueAt(r,
						hhTable.getColumnPosition(HH_BLDGSZ_FIELD_NAME));
				hh.setHhBldgsz(bldgsz);

				hh.initializeWindows();
				hhArray[index] = hh;

			} catch (Exception e) {

				logger.fatal(String
						.format("exception caught mapping household data record to a Household object, r=%d, id=%d.",
								r, id));
				throw new RuntimeException(e);

			}

		}

		int hhid = -1;
		int oldHhid = -1;
		int i = -1;
		int persNum = -1;
		int persId = -1;
		int fieldCount = 0;

		// for each person table record
		for (int r = 1; r <= personTable.getRowCount(); r++) {

			try {

				// get the Household object for this person data to be stored in
				hhid = (int) personTable.getValueAt(r,
						personTable.getColumnPosition(PERSON_HH_ID_FIELD_NAME));
				int index = hhIndexArray[hhid];
				Household hh = hhArray[index];
				fieldCount = 1;

				if (oldHhid < hhid) {
					oldHhid = hhid;
					persNum = 1;
				}

				// get the Person object for this person data to be stored in
				persId = (int) personTable.getValueAt(r, personTable
						.getColumnPosition(PERSON_PERSON_ID_FIELD_NAME));
				Person person = hh.getPerson(persNum++);
				person.setPersId(persId);
				fieldCount++;

				// get required values from table record and store in Person
				// object
				int age = (int) personTable.getValueAt(r,
						personTable.getColumnPosition(PERSON_AGE_FIELD_NAME));
				person.setPersAge(age);
				fieldCount++;

				int gender = (int) personTable
						.getValueAt(r, personTable
								.getColumnPosition(PERSON_GENDER_FIELD_NAME));
				person.setPersGender(gender);
				fieldCount++;

				int occcen1 = (int) personTable.getValueAt(r, personTable
						.getColumnPosition(PERSON_OCCCEN1_FIELD_NAME));
				int pecasOccup = occCodes[occcen1];

				if (pecasOccup == 0)
					logger.warn("pecasOccup==0 for occcen1=" + occcen1);

				int indcen = (int) personTable
						.getValueAt(r, personTable
								.getColumnPosition(PERSON_INDCEN_FIELD_NAME));
				int activityCode = indCodes[indcen];

				if ((pecasOccup == 71)
						&& (activityCode == 2 || activityCode == 4
								|| activityCode == 6 || activityCode == 8 || activityCode == 29))
					activityCode++;

				if ((pecasOccup == 76)
						&& (activityCode == 3 || activityCode == 5
								|| activityCode == 7 || activityCode == 9 || activityCode == 30))
					activityCode--;

				if ((pecasOccup == 76) && (activityCode == 13))
					activityCode = 14;

				if ((pecasOccup == 71) && (activityCode == 14))
					activityCode = 13;

				if ((pecasOccup == 75) && (activityCode == 18))
					activityCode = 22;

				if ((pecasOccup == 71) && (activityCode == 22))
					activityCode = 18;

				if (activityCode == 28)
					pecasOccup = 77;

				person.setPersActivityCode(activityCode);
				fieldCount++;

				person.setPersPecasOccup(pecasOccup);
				fieldCount++;

				// Employment status (1-employed FT, 2-employed PT, 3-not
				// employed,
				// 4-under age 16)
				int empCat = (int) personTable
						.getValueAt(
								r,
								personTable
										.getColumnPosition(PERSON_EMPLOYMENT_CATEGORY_FIELD_NAME));
				person.setPersEmploymentCategory(empCat);
				fieldCount++;

				// Student status (1 - student in grade or high school; 2 -
				// student
				// in college or higher; 3 - not a student)
				int studentCat = (int) personTable.getValueAt(r, personTable
						.getColumnPosition(PERSON_STUDENT_CATEGORY_FIELD_NAME));
				person.setPersStudentCategory(studentCat);
				fieldCount++;

				// Person type (1-FT worker age 16+, 2-PT worker nonstudent age
				// 16+,
				// 3-university student, 4-nonworker nonstudent age 16-64,
				// 5-nonworker nonstudent age 65+,
				// 6-"age 16-19 student, not FT wrkr or univ stud", 7-age 6-15
				// schpred, 8 under age 6 presch
				int personType = (int) personTable.getValueAt(r, personTable
						.getColumnPosition(PERSON_TYPE_CATEGORY_FIELD_NAME));
				person.setPersonTypeCategory(personType);
				fieldCount++;

				// Person educational attainment level to determine high school
				// graduate status ( < 9 - not a graduate, 10+ - high school
				// graduate
				// and
				// beyond)
				int educ = (int) personTable
						.getValueAt(
								r,
								personTable
										.getColumnPosition(PERSON_EDUCATION_ATTAINMENT_FIELD_NAME));
				if (educ >= 9)
					person.setPersonIsHighSchoolGraduate(true);
				else
					person.setPersonIsHighSchoolGraduate(false);
				fieldCount++;

				// Person educational attainment level to determine higher
				// education
				// status ( > 12 - at least a bachelor's degree )
				if (educ >= 13)
					person.setPersonHasBachelors(true);
				else
					person.setPersonHasBachelors(false);
				fieldCount++;

				// Person grade enrolled in ( 0-"not enrolled", 1-"preschool",
				// 2-"Kindergarten", 3-"Grade 1 to grade 4",
				// 4-"Grade 5 to grade 8", 5-"Grade 9 to grade 12",
				// 6-"College undergraduate",
				// 7-"Graduate or professional school" )
				int grade = (int) personTable.getValueAt(r, personTable
						.getColumnPosition(PERSON_GRADE_ENROLLED_FIELD_NAME));
				person.setPersonIsGradeSchool(false);
				person.setPersonIsHighSchool(false);
				if (grade >= 2 && grade <= 4)
					person.setPersonIsGradeSchool(true);
				else if (grade == 5)
					person.setPersonIsHighSchool(true);
				fieldCount++;

				// if person is a university student but has school age student
				// category value, reset student category value
				if (personType == Person.PersonType.University_student
						.ordinal()
						&& studentCat != Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER
								.ordinal()) {
					studentCat = Person.StudentStatus.STUDENT_COLLEGE_OR_HIGHER
							.ordinal();
					person.setPersStudentCategory(studentCat);
					invalidPersonTypeCount1++;
					// if person is a student of any kind but has full-time
					// employment
					// status, reset student category value to non-student
				} else if (studentCat != Person.StudentStatus.NON_STUDENT
						.ordinal()
						&& empCat == Person.EmployStatus.FULL_TIME.ordinal()) {
					studentCat = Person.StudentStatus.NON_STUDENT.ordinal();
					person.setPersStudentCategory(studentCat);
					invalidPersonTypeCount2++;
				}
				fieldCount++;

				// check consistency of student category and person type
				if (studentCat == Person.StudentStatus.NON_STUDENT.ordinal()) {

					if (person.getPersonIsStudentNonDriving() == 1
							|| person.getPersonIsStudentDriving() == 1) {
						studentCat = Person.StudentStatus.STUDENT_HIGH_SCHOOL_OR_LESS
								.ordinal();
						person.setPersStudentCategory(studentCat);
						invalidPersonTypeCount3++;
					}

				}
				fieldCount++;

			} catch (Exception e) {

				logger.fatal("exception caught mapping person data record to a Person object, "
						+ String.format(
								"r=%d, i=%d, hhid=%d, persid=%d, persnum=%d, fieldCount=%d.",
								r, i, hhid, persId, persNum, fieldCount));
				throw new RuntimeException(e);

			}

		} // person loop

		hhs = hhArray;

		logger.warn(invalidPersonTypeCount1
				+ " person type = university and student category = non-student person records"
				+ " had their student category changed to university or higher.");
		logger.warn(invalidPersonTypeCount2
				+ " Student category = student and employment category = full-time worker person records"
				+ " had their student category changed to non-student.");
		logger.warn(invalidPersonTypeCount3
				+ " Student category = non-student and person type = student person records"
				+ " had their student category changed to student high school or less.");

	}

	/**
	 * if called, must be called after readData so that the size of the full
	 * population is known.
	 * 
	 * @param hhFileName
	 * @param persFileName
	 * @param numHhs
	 */
	public void createSamplePopulationFiles(String hhFileName,
			String persFileName, String newHhFileName, String newPersFileName,
			int numHhs) {

		int maximumHhId = 0;
		for (int i = 0; i < hhs.length; i++) {
			int id = hhs[i].getHhId();
			if (id > maximumHhId)
				maximumHhId = id;
		}

		int[] testHhs = new int[maximumHhId + 1];

		int[] sortedIndices = getRandomOrderHhIndexArray(hhs.length);

		for (int i = 0; i < numHhs; i++) {
			int k = sortedIndices[i];
			int hhId = hhs[k].getHhId();
			testHhs[hhId] = 1;
		}

		String hString = "";
		int hCount = 0;
		try {

			logger.info(String.format(
					"writing sample household file for %d households", numHhs));

			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(newHhFileName)));
			BufferedReader in = new BufferedReader(new FileReader(hhFileName));

			// read headers and write to output files
			hString = in.readLine();
			out.write(hString + "\n");
			hCount++;
			int count = 0;

			while ((hString = in.readLine()) != null) {
				hCount++;
				int endOfField = hString.indexOf(',');
				int hhId = Integer.parseInt(hString.substring(0, endOfField));

				// if it's a sample hh, write the hh and the person records
				if (testHhs[hhId] == 1) {
					out.write(hString + "\n");
					count++;
					if (count == numHhs)
						break;
				}
			}

			out.close();

		} catch (IOException e) {
			logger.fatal("IO Exception caught creating sample synpop household file.");
			logger.fatal(String.format(
					"reading hh file = %s, writing sample hh file = %s.",
					hhFileName, newHhFileName));
			logger.fatal(String.format("hString = %s, hCount = %d.", hString,
					hCount));
		}

		String pString = "";
		int pCount = 0;
		try {

			logger.info(String
					.format("writing sample person file for selected households"));

			PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(newPersFileName)));
			BufferedReader in = new BufferedReader(new FileReader(persFileName));

			// read headers and write to output files
			pString = in.readLine();
			out.write(pString + "\n");
			pCount++;
			int count = 0;
			int oldId = 0;
			while ((pString = in.readLine()) != null) {
				pCount++;
				int endOfField = pString.indexOf(',');
				int hhId = Integer.parseInt(pString.substring(0, endOfField));

				// if it's a sample hh, write the hh and the person records
				if (testHhs[hhId] == 1) {
					out.write(pString + "\n");
					if (hhId > oldId)
						count++;
				} else {
					if (count == numHhs)
						break;
				}

				oldId = hhId;

			}

			out.close();

		} catch (IOException e) {
			logger.fatal("IO Exception caught creating sample synpop person file.");
			logger.fatal(String
					.format("reading person file = %s, writing sample person file = %s.",
							persFileName, newPersFileName));
			logger.fatal(String.format("pString = %s, pCount = %d.", pString,
					pCount));
		}

	}

	public static void main(String[] args) throws Exception {

		String serverAddress = HH_DATA_SERVER_ADDRESS;
		int serverPort = HH_DATA_SERVER_PORT;

		// optional arguments
		for (int i = 0; i < args.length; i++) {
			if (args[i].equalsIgnoreCase("-hostname")) {
				serverAddress = args[i + 1];
			}

			if (args[i].equalsIgnoreCase("-port")) {
				serverPort = Integer.parseInt(args[i + 1]);
			}
		}

		Remote.config(serverAddress, HH_DATA_SERVER_PORT, null, 0);

		SandagHouseholdDataManager hhDataManager = new SandagHouseholdDataManager();

		ItemServer.bind(hhDataManager, HH_DATA_SERVER_NAME);

		System.out.println(String.format(
				"SandagHouseholdDataManager server class started on: %s:%d",
				serverAddress, serverPort));

	}

	public int[] getJointToursByHomeMgra(String purposeString) {
		// TODO Auto-generated method stub
		return null;
	}

	private int[] readOccupCorrespondenceData() {

		TableDataSet occTable = null;

		// construct input household file name from properties file values
		String occupFileName = propertyMap.get(PROPERTIES_OCCUP_CODES);
		String fileName = projectDirectory + "/" + occupFileName;

		try {
			logger.info("reading occupation codes data file for creating occupation segments.");
			OLD_CSVFileReader reader = new OLD_CSVFileReader();
			reader.setDelimSet("," + reader.getDelimSet());
			occTable = reader.readFile(new File(fileName));
		} catch (Exception e) {
			logger.fatal(String
					.format("Exception occurred occupation codes data file: %s into TableDataSet object.",
							fileName));
			throw new RuntimeException(e);
		}

		// get the array of indices from the TableDataSet
		int[] occcen1Col = occTable.getColumnAsInt("occcen1");
		int[] occupCol = occTable.getColumnAsInt("pecas_occ");

		// get the max index value, to use for array dimensions
		int maxOcc = 0;
		for (int occ : occcen1Col)
			if (occ > maxOcc)
				maxOcc = occ;

		int[] occcen1Occup = new int[maxOcc + 1];
		for (int i = 0; i < occcen1Col.length; i++) {
			int index = occcen1Col[i];
			int value = occupCol[i];
			occcen1Occup[index] = value;
		}

		return occcen1Occup;
	}

	private int[] readIndustryCorrespondenceData() {

		TableDataSet indTable = null;

		// construct input household file name from properties file values
		String indFileName = propertyMap.get(PROPERTIES_INDUSTRY_CODES);
		String fileName = projectDirectory + "/" + indFileName;

		try {
			logger.info("reading industry codes data file for creating industry segments.");
			OLD_CSVFileReader reader = new OLD_CSVFileReader();
			reader.setDelimSet("," + reader.getDelimSet());
			indTable = reader.readFile(new File(fileName));
		} catch (Exception e) {
			logger.fatal(String
					.format("Exception occurred reading indistry codes data file: %s into TableDataSet object.",
							fileName));
			throw new RuntimeException(e);
		}

		// get the array of indices from the TableDataSet
		int[] indcenCol = indTable.getColumnAsInt("indcen");
		int[] activityCol = indTable.getColumnAsInt("activity_code");

		// get the max index value, to use for array dimensions
		int maxInd = 0;
		for (int ind : indcenCol)
			if (ind > maxInd)
				maxInd = ind;

		int[] indcenIndustry = new int[maxInd + 1];
		for (int i = 0; i < indcenCol.length; i++) {
			int index = indcenCol[i];
			int value = activityCol[i];
			indcenIndustry[index] = value;
		}

		return indcenIndustry;
	}

}
