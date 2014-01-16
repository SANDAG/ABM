package org.sandag.abm.reporting.emfac2011;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pb.sawdust.io.FileUtil;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.util.ProcessUtil;
import com.pb.sawdust.util.exceptions.RuntimeIOException;

/**
 * The {@code Emfac2011Runner} class is used to generate an EMFAC2011 SG input
 * file adjusted for travel demand model results, and then run (via the end
 * user) the EMFAC2011 SG model using those inputs.
 * 
 * @author crf Started 2/9/12 9:17 AM
 */
public class Emfac2011Runner
{
    private static final Logger       LOGGER = LoggerFactory.getLogger(Emfac2011Runner.class);

    private final Emfac2011Properties properties;

    /**
     * Constructor specifying the resources used to build the properties used
     * for the EMFAC2011 SG run.
     * 
     * @param propertyResource
     *            The first properties resource.
     * 
     * @param additionalResources
     *            Any additional properties resources.
     */
    public Emfac2011Runner(String propertyResource, String... additionalResources)
    {
        properties = new Emfac2011Properties(propertyResource, additionalResources);
    }

    /**
     * Run the EMFAC2011 model. This method will process the model results (via
     * AquaVis outputs), create an adjusted EMFAC2011 input file, and initiate
     * the EMFAC2011 SG model. Because of the way it is set up, the user must
     * actually set up and run the EMFAC2011 SG model, but this method will
     * create a dialog window which will walk the user through the steps
     * required to do that.
     * 
     * @param emfac2011Data
     *            The {@code Emfac2011Data} instance corresponding to the model
     *            results/run.
     */
    public void runEmfac2011(Emfac2011Data emfac2011Data)
    {
        LOGGER.info("Processing aquavis data");
        DataTable data = emfac2011Data.processAquavisData(properties);
        LOGGER.info("Creating EMFAC2011 input file");
        Emfac2011InputFileCreator inputFileCreator = new Emfac2011InputFileCreator();
        Path inputfile = inputFileCreator.createInputFile(properties, data);
        LOGGER.info("Initiating EMFAC2011");
        RunEmfacDialog.createAndShowGUI(inputfile, this);
        LOGGER.info("EMFAC2011 run (presumably) finished");
    }

    void runEmfac2011Program()
    {
        final Path emfacInstallationDir = Paths.get(properties
                .getString(Emfac2011Properties.EMFAC2011_INSTALLATION_DIR_PROPERTY));
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.lnk");
        final List<Path> link = new LinkedList<>();
        FileVisitor<Path> visitor = new SimpleFileVisitor<Path>()
        {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                Path name = file.getFileName();
                if (name != null && matcher.matches(name))
                {
                    link.add(file);
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try
        {
            Files.walkFileTree(emfacInstallationDir, Collections.<FileVisitOption>emptySet(), 1,
                    visitor);
        } catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }

        if (link.size() == 0)
            throw new IllegalStateException("Cannot find Emfac2011 shortcut in "
                    + emfacInstallationDir);
        ProcessUtil.runProcess(Arrays.asList("cmd", "/c", link.get(0).toString()));
    }

    public Emfac2011Properties getProperties()
    {
        return properties;
    }

}
