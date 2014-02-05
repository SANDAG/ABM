package org.sandag.abm.reporting;

import java.io.File;
import java.util.Properties;
import org.apache.log4j.Logger;

public abstract class AbstractCsvExporter
        implements IExporter
{
    private final File                file;
    private final MatrixServerWrapper mtxSvrWrapper;
    private final String              reportFolder = "report.path";

    protected static final Logger     LOGGER       = Logger.getLogger(AbstractCsvExporter.class);

    public AbstractCsvExporter(Properties properties, String aBaseFileName, MatrixServerWrapper aMatrixServerWrapper)
    {
        this.file = new File(properties.getProperty(reportFolder), aBaseFileName + ".csv");
        this.mtxSvrWrapper = aMatrixServerWrapper;
    }

    public File getFile()
    {
        return this.file;
    }

    public MatrixServerWrapper getMatrixServerWrapper()
    {
        return this.mtxSvrWrapper;
    }
}
