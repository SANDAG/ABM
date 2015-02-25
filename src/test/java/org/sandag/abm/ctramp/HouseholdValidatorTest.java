package org.sandag.abm.ctramp;

import static org.junit.Assert.assertEquals;
import java.util.Random;
import org.junit.Test;
import org.sandag.abm.application.SandagModelStructure;

public class HouseholdValidatorTest
{
    @Test
    public void testVailidateWorkLocChoices()
    {
        Household[] households = getHouseholdArray(100000);

        for (Household hh : households)
            assertEquals(validWorkLocationHousehold(hh),
                    HouseholdValidator.vailidateWorkLocChoices(hh));
    }

    @Test
    public void testVailidateSchoolLocChoices()
    {
        Household[] households = getHouseholdArray(100000);

        for (Household hh : households)
            assertEquals(validSchoolLocationHousehold(hh),
                    HouseholdValidator.vailidateSchoolLocChoices(hh));
    }

    @Test
    public void testValidateMandatoryDestinationChoicesWork()
    {
        int numTests = 1000;

        for (int i = 0; i < numTests; i++)
        {
            Household[] households = getHouseholdArray(100);
            boolean valid = true;
            for (Household hh : households)
            {
                if (!validWorkLocationHousehold(hh))
                {
                    valid = false;
                    break;
                }
            }

            assertEquals(valid,
                    HouseholdValidator.validateMandatoryDestinationChoices(households, "work"));
        }
    }

    @Test
    public void testValidateMandatoryDestinationChoicesSchool()
    {
        int numTests = 1000;

        for (int i = 0; i < numTests; i++)
        {
            Household[] households = getHouseholdArray(100);
            boolean valid = true;
            for (Household hh : households)
            {
                if (!validSchoolLocationHousehold(hh))
                {
                    valid = false;
                    break;
                }
            }

            assertEquals(valid,
                    HouseholdValidator.validateMandatoryDestinationChoices(households, "school"));
        }
    }

    private Household[] getHouseholdArray(int size)
    {
        ModelStructure structure = new SandagModelStructure();

        Random rnd = new Random();
        Household[] households = new Household[size];

        for (int i = 0; i < households.length; i++)
        {
            households[i] = new Household(structure);
            households[i].setHhSize(rnd.nextInt(4) + 1);

            for (int j = 1; j <= households[i].getHhSize(); j++)
            {
                Person p = households[i].getPerson(j);
                p.setWorkLocationSegmentIndex(rnd.nextInt(10) - 1);
                if (p.getWorkLocationSegmentIndex() >= 0) p.setWorkLocation(rnd.nextInt(10));

                p.setSchoolLocationSegmentIndex(rnd.nextInt(10) - 1);
                if (p.getSchoolLocationSegmentIndex() >= 0) p.setSchoolLoc(rnd.nextInt(10));
            }
        }

        return households;
    }

    private boolean validWorkLocationHousehold(Household hh)
    {
        boolean valid = true;

        for (int i = 1; i <= hh.getHhSize(); i++)
        {
            Person p = hh.getPerson(i);
            if (p.getWorkLocationSegmentIndex() >= 0 && p.getWorkLocation() <= 0)
            {
                System.out.println("Work Segment Index: " + p.getWorkLocationSegmentIndex()
                        + ", Work Location: " + p.getWorkLocation());
                valid = false;
                break;
            }
        }

        return valid;
    }

    private boolean validSchoolLocationHousehold(Household hh)
    {
        boolean valid = true;

        for (int i = 1; i <= hh.getHhSize(); i++)
        {
            Person p = hh.getPerson(i);
            if (p.getSchoolLocationSegmentIndex() >= 0 && p.getPersonSchoolLocationZone() <= 0)
            {
                System.out.println("School Segment Index: " + p.getSchoolLocationSegmentIndex()
                        + ", School Location: " + p.getPersonSchoolLocationZone());
                valid = false;
                break;
            }
        }

        return valid;
    }
}
