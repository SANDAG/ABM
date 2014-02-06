package org.sandag.abm.reporting;

import java.io.IOException;
import java.util.Properties;

public class AbstractCsvExporterMock
        extends AbstractCsvExporter
{

    public AbstractCsvExporterMock(Properties properties, IMatrixDao aMatrixDao,
            String aBaseFileName)
    {
        super(properties, aMatrixDao, aBaseFileName);
    }

    @Override
    public void export() throws IOException
    {
        throw new IOException("Mock Object. Method not implemented");
    }
}
