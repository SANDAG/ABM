package org.sandag.abm.reporting;

import java.io.IOException;

public interface IExporter
{
    static final String[] TOD_TOKENS = {"EA", "AM", "MD", "PM", "EV"};
    static final String   TOD_TOKEN  = "${TOD_TOKEN}";

    void export() throws IOException;
}
