package org.sandag.abm.reporting;

import java.io.File;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import com.pb.common.util.ResourceUtil;

public abstract class AbstractCsvExporter
        implements IExporter
{
    private final File                file;
    private final MatrixServerWrapper mtxSvrWrapper;

    protected static final Logger     LOGGER = Logger.getLogger(AbstractCsvExporter.class);

    public AbstractCsvExporter(String aBaseFileName, MatrixServerWrapper aMatrixServerWrapper)
    {
        ResourceBundle properties = ResourceUtil.getResourceBundle("sandag_data_export");
        String outputFolder = properties.getString("output.folder");
        this.file = new File(outputFolder, aBaseFileName + ".csv");
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
