package org.sandag.abm.application;

import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import com.pb.common.util.ResourceUtil;

public final class SandagSamplePopulationGenerator
{

    private static Logger      logger                       = Logger
                                                                    .getLogger(SandagSamplePopulationGenerator.class);

    public static final String PROPERTIES_PROJECT_DIRECTORY = "Project.Directory";
    private ResourceBundle     rb;

    /**
     * 
     * @param rb , java.util.ResourceBundle containing environment settings from a
     *            properties file specified on the command line
     * @param baseName , String containing basename (without .properites) from which
     *            ResourceBundle was created.
     * @param globalIterationNumber , int iteration number for which the model is
     *            run, set by another process controlling a model stream with
     *            feedback.
     * @param iterationSampleRate , float percentage [0.0, 1.0] inicating the portion
     *            of all households to be modeled.
     * 
     *            This object defines the implementation of the ARC tour based,
     *            activity based travel demand model.
     */
    private SandagSamplePopulationGenerator(ResourceBundle rb)
    {
        this.rb = rb;
    }

    private void generateSampleFiles()
    {

        // new a ctramp application object
        HashMap<String, String> propertyMap = ResourceUtil.changeResourceBundleIntoHashMap(rb);

        // create a new local instance of the household array manager
        SandagHouseholdDataManager householdDataManager = new SandagHouseholdDataManager();
        householdDataManager.setPropertyFileValues(propertyMap);

        // have the household data manager read the synthetic population files and
        // apply its tables to objects mapping method.
        String inputHouseholdFileName = "data/inputs/hhfile.csv";
        String inputPersonFileName = "data/inputs/personfile.csv";
        householdDataManager.setHouseholdSampleRate(1.0f, 0);

        SandagModelStructure modelStructure = new SandagModelStructure();

        householdDataManager.setModelStructure(modelStructure);
        householdDataManager.readPopulationFiles(inputHouseholdFileName, inputPersonFileName);
        householdDataManager.mapTablesToHouseholdObjects();

        householdDataManager.createSamplePopulationFiles(
                "/jim/projects/sandag/data/inputs/hhfile.csv",
                "/jim/projects/sandag/data/inputs/personfile.csv",
                "/jim/projects/sandag/data/inputs/hhfile_1000.csv",
                "/jim/projects/sandag/data/inputs/personfile_1000.csv", 1000);
    }

    public static void main(String[] args)
    {

        ResourceBundle rb = null;

        if (args.length == 0)
        {
            logger
                    .error(String
                            .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
        }

        // create an instance of this class for main() to use.
        SandagSamplePopulationGenerator mainObject = new SandagSamplePopulationGenerator(rb);

        // run tour based models
        try
        {
            mainObject.generateSampleFiles();
        } catch (RuntimeException e)
        {
            logger
                    .error(
                            "RuntimeException caught in SandagSamplePopulationGenerator.main() -- exiting.",
                            e);
        }

        System.exit(0);
    }

}
