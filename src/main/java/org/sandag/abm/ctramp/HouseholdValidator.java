package org.sandag.abm.ctramp;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagTourBasedModel;

public class HouseholdValidator
{
    private static Logger logger = Logger.getLogger(SandagTourBasedModel.class);

    public static boolean vailidateWorkLocChoices(Household hh)
    {
        boolean result = true;

        for (int j = 1; j <= hh.getHhSize(); j++)
        {
            Person p = hh.getPerson(j);
            int windex = p.getWorkLocationSegmentIndex();
            int wmgra = p.getWorkLocation();
            if (windex != -1 && wmgra == 0)
            {
                result = false;
                logger.info("Invalid work location choice for " + p.getPersonId() + " a "
                        + p.getAge() + " years old with work segment index "
                        + p.getWorkLocationSegmentIndex() + " resubmitting job......");
                break;
            }
        }
        return result;
    }

    public static boolean vailidateSchoolLocChoices(Household hh)
    {
        boolean result = true;

        for (int j = 1; j <= hh.getHhSize(); j++)
        {
            Person p = hh.getPerson(j);
            int sindex = p.getSchoolLocationSegmentIndex();
            int smgra = p.getPersonSchoolLocationZone();
            if (sindex != -1 && smgra == 0)
            {
                result = false;
                logger.fatal("Invalid school location choice for " + p.getPersonId() + " a "
                        + p.getAge() + " years old with school segment index "
                        + p.getSchoolLocationSegmentIndex() + " resubmitting job.....");
                break;
            }
        }
        return result;
    }

    public static boolean validateMandatoryDestinationChoices(Household[] householdArray,
            String type)
    {
        boolean result = true;
        if (type.equalsIgnoreCase("work"))
        {
            for (int i = 0; i < householdArray.length; i++)
            {
                if (!vailidateWorkLocChoices(householdArray[i]))
                {
                    result = false;
                    break;
                }
            }
        } else if (type.equalsIgnoreCase("school"))
        {
            for (int i = 0; i < householdArray.length; i++)
            {
                if (!vailidateSchoolLocChoices(householdArray[i]))
                {
                    result = false;
                    break;
                }
            }
        } else
        {
            logger.fatal("invalide mandatory destination choice type:" + type);
        }
        return result;
    }
}
