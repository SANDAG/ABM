package org.sandag.abm.reporting;

import java.io.File;
import java.util.Properties;
import org.apache.log4j.Logger;

public abstract class AbstractCsvExporter
        implements IExporter
{
    private final File            file;
    private final IMatrixDao      matrixDao;
    private final String          reportFolder = "report.path";

    protected static final Logger LOGGER       = Logger.getLogger(AbstractCsvExporter.class);

    public AbstractCsvExporter(Properties properties, IMatrixDao aMatrixDao, String aBaseFileName)
    {
        this.file = new File(properties.getProperty(reportFolder), aBaseFileName + ".csv");
        this.matrixDao = aMatrixDao;
    }

    public IMatrixDao getMatrixDao()
    {
        return this.matrixDao;
    }

    public File getFile()
    {
        return this.file;
    }

}
