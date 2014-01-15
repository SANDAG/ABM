package org.sandag.abm.reporting.emfac2011;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import com.pb.sawdust.tabledata.DataRow;
import com.pb.sawdust.tabledata.DataTable;
import com.pb.sawdust.tabledata.basic.RowDataTable;
import com.pb.sawdust.tabledata.read.CsvTableReader;
import com.pb.sawdust.tabledata.write.CsvTableWriter;
import com.pb.sawdust.util.Range;
import com.pb.sawdust.util.exceptions.RuntimeIOException;
import com.pb.sawdust.util.exceptions.RuntimeWrappingException;
import com.pb.sawdust.util.format.DelimitedDataFormat;
import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * The {@code SandagAquavisInputBuilder} ...
 * 
 * @author crf Started 12/7/12 11:08 AM
 */
public class SandagModelDataBuilder
{

    public static final String   AQUAVIS_NETWORK_TABLE                       = "aquavis_network";
    public static final String   AQUAVIS_TRIPS_TABLE                         = "aquavis_trips";
    public static final String   AQUAVIS_INTRAZONAL_TABLE                    = "aquavis_intrazonal";

    public static final String   REPORTS_DATABASE_IPADDRESS_PROPERTY         = "reports.database.ipaddress";
    public static final String   REPORTS_DATABASE_PORT_PROPERTY              = "reports.database.port";
    public static final String   REPORTS_DATABASE_NAME_PROPERTY              = "reports.database.name";
    public static final String   REPORTS_DATABASE_USERNAME_PROPERTY          = "reports.database.username";
    public static final String   REPORTS_DATABASE_PASSWORD_PROPERTY          = "reports.database.password";
    public static final String   REPORTS_DATABASE_INSTANCE_PROPERTY          = "reports.database.instance";

    public static final String   CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY    = "aquavis.network.sql.template";
    public static final String   CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY      = "aquavis.trips.sql.template";
    public static final String   CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY = "aquavis.intrazonal.sql.template";
    public static final String   AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY      = "aquavis.template.schema.token";
    public static final String   AQUAVIS_EXTERNAL_INTRAZONAL_TABLE_PROPERTY  = "aquavis.external.intrazonal.table";
    public static final String   SCHEMA_NAME_PROPERTY                        = "schema.name";
    private final PropertyDeluxe properties;
    private final String         connectionUrl;

    public SandagModelDataBuilder(PropertyDeluxe properties)
    {
        connectionUrl = formConnectionUrl(
                properties.getString(REPORTS_DATABASE_IPADDRESS_PROPERTY),
                properties.getInt(REPORTS_DATABASE_PORT_PROPERTY),
                properties.getString(REPORTS_DATABASE_NAME_PROPERTY),
                properties.hasKey(REPORTS_DATABASE_USERNAME_PROPERTY) ? properties
                        .getString(REPORTS_DATABASE_USERNAME_PROPERTY) : null,
                properties.hasKey(REPORTS_DATABASE_PASSWORD_PROPERTY) ? properties
                        .getString(REPORTS_DATABASE_PASSWORD_PROPERTY) : null,
                properties.hasKey(REPORTS_DATABASE_INSTANCE_PROPERTY) ? properties
                        .getString(REPORTS_DATABASE_INSTANCE_PROPERTY) : null);
        this.properties = properties;
    }

    public void createAquavisInputs(String aquavisNetworkPathProperty,
            String aquavisTripsPathProperty, String aquavisIntrazonalPathProperty)
    {
        createAquavisNetwork(properties.getPath(aquavisNetworkPathProperty));
        createAquavisTrips(properties.getPath(aquavisTripsPathProperty));
        createAquavisIntrazonal(properties.getPath(aquavisIntrazonalPathProperty));
    }

    private void createAquavisNetwork(Path outputFile)
    {
        String schema = properties.getString(SCHEMA_NAME_PROPERTY);
        detemplifyAndRunScript(properties.getPath(CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY),
                schema, properties.getString(AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
        //writeTableToCsv(AQUAVIS_NETWORK_TABLE, schema, outputFile);
    }

    private void createAquavisTrips(Path outputFile)
    {
        String schema = properties.getString(SCHEMA_NAME_PROPERTY);
        detemplifyAndRunScript(properties.getPath(CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY), schema,
                properties.getString(AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
        //writeTableToCsv(AQUAVIS_TRIPS_TABLE, schema, outputFile);
    }

    private void createAquavisIntrazonal(Path outputFile)
    {
        String schema = properties.getString(SCHEMA_NAME_PROPERTY);
        detemplifyAndRunScript(properties.getPath(CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY),
                schema, properties.getString(AQUAVIS_TEMPLATE_SCHEMA_TOKEN_PROPERTY));
        //writeTableToCsv(AQUAVIS_INTRAZONAL_TABLE, schema, outputFile);
        addExternalIntrazonalData(outputFile.toString(),
                properties.getString(AQUAVIS_EXTERNAL_INTRAZONAL_TABLE_PROPERTY));
    }

    private void addExternalIntrazonalData(String intrazonalTablePath,
            String externalIntrazonalTablePath)
    {
        DataTable table = new RowDataTable(new CsvTableReader(intrazonalTablePath));
        DataTable externalTable = new RowDataTable(new CsvTableReader(externalIntrazonalTablePath));
        for (DataRow row : externalTable)
            table.addRow(row);
        new CsvTableWriter(intrazonalTablePath).writeTable(table);
    }

    private void detemplifyAndRunScript(Path script, String schema, String schemaToken)
    {
        String s = readFile(script).replace(schemaToken, schema);
        try (Connection connection = getConnection();
                Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);
            statement.execute(s);
            connection.commit();
        } catch (SQLException e)
        {
            throw new RuntimeWrappingException(e);
        }
    }

    private String readFile(Path file)
    {
        try (FileReader reader = new FileReader(file.toFile()))
        {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int readCount;
            while ((readCount = reader.read(buffer, 0, buffer.length)) > 0)
                sb.append(buffer, 0, readCount);
            return sb.toString();
        } catch (IOException e)
        {
            throw new RuntimeIOException(e);
        }
    }
/*
    public void writeTableToCsv(String table, String schema, Path outputFile)
    { // convenient to use outside of this...
        DelimitedDataFormat formatter = new DelimitedDataFormat(',');
        String query = "SELECT * FROM " + schema + "." + table + ";";
        try (PrintWriter writer = new PrintWriter(outputFile.toFile());
                Connection connection = getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query))
        {

            ResultSetMetaData metaData = resultSet.getMetaData();
            List<String> header = new LinkedList<>();
            int columnCount = metaData.getColumnCount();
            Range range = new Range(1, 1 + columnCount);
            for (int i : range)
                header.add(metaData.getColumnName(i));
            writer.println(formatter.format(header.toArray(new Object[columnCount])));
            while (resultSet.next())
            {
                Object[] data = new Object[columnCount];
                for (int i : range)
                    data[i - 1] = resultSet.getObject(i);
                writer.println(formatter.format(data));
            }
        } catch (FileNotFoundException e)
        {
            throw new RuntimeIOException(e);
        } catch (SQLException e)
        {
            throw new RuntimeWrappingException(e);
        }
    }
*/
    private String formConnectionUrl(String ipAddress, int port, String databaseName,
            String username, String password, String instance)
    {
        String url = "jdbc:jtds:sqlserver://" + ipAddress + ":" + port + "/" + databaseName;
        if (username != null) url += ";user=" + username + ";" + password; // not
                                                                           // super
                                                                           // secure,
                                                                           // btu
                                                                           // ok
                                                                           // for
                                                                           // now
                                                                           // -
                                                                           // probably
                                                                           // will
                                                                           // use
                                                                           // SSO
                                                                           // normally
        if (instance != null) url += ";instance=" + instance;
        return url;
    }

    private Connection getConnection() throws SQLException
    {
        try
        {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
        } catch (ClassNotFoundException e)
        {
            throw new RuntimeWrappingException(e);
        }
        return DriverManager.getConnection(connectionUrl);
    }
}
