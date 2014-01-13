package org.sandag.abm.reporting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import com.pb.common.util.ResourceUtil;

/**
 * The {@code ReportBuilder} ...
 * 
 * @author crf Started 12/20/12 3:06 PM
 */
public class ReportBuilder
{
    private static final Logger logger = Logger.getLogger(ReportBuilder.class);

    public static void main(String... args)
    {
        ResourceBundle properties = ResourceUtil.getResourceBundle("sandag_reports");
        String scriptDirectory = properties.getString("sql.scripts.directory").trim();
        String sqlScripts = properties.getString("sql.scripts");
        String schemaKey = properties.getString("sql.scripts.schema.key");
        String worksheetTables = properties.getString("worksheet.tables");
        String worksheetNames = properties.getString("worksheet.names");
        String workbookName = properties.getString("workbook.name");
        String schema = properties.getString("database.schema");

        List<String> sql = new LinkedList<String>();
        for (String sqlScript : sqlScripts.trim().split(","))
            sql.add(new File(scriptDirectory, sqlScript).toString());

        String[] tables = worksheetTables.trim().split(",");
        String[] worksheets = worksheetNames.trim().split(",");
        Map<String, String> mapping = new LinkedHashMap<String, String>();
        for (int i = 0; i < tables.length; i++)
            mapping.put(tables[i], worksheets[i]);

        String ipaddress = properties.getString("database.ipaddress");
        int port = ResourceUtil.getIntegerProperty(properties, "database.port");
        String databaseName = properties.getString("database.name");
        String username = properties.containsKey("database.username") ? properties
                .getString("database.username") : null;
        String password = properties.containsKey("database.password") ? properties
                .getString("database.password") : null;
        String databaseServerInstance = properties.containsKey("database.instance") ? properties
                .getString("database.instance") : null;

        Map<String, String> detokenizingMap = new HashMap<String, String>();
        detokenizingMap.put(schemaKey, schema);

        ReportBuilder rb = new ReportBuilder(ipaddress, port, databaseName, username, password,
                databaseServerInstance);
        rb.buildReport(sql, detokenizingMap, schema, mapping, workbookName);
    }

    private final String connectionUrl;

    public ReportBuilder(String ipAddress, int port, String databaseName, String username,
            String password, String databaseServerInstance)
    {
        connectionUrl = formConnectionUrl(ipAddress, port, databaseName, username, password,
                databaseServerInstance);
    }

    public void buildReport(List<String> sqlFiles, Map<String, String> detokenizingMappings,
            String schema, Map<String, String> tableToWorksheetMapping, String workbookFile)
    {
        executeSqlFiles(sqlFiles, detokenizingMappings);
        writeTablesToExcel(schema, tableToWorksheetMapping, workbookFile);
    }

    private void executeSqlFiles(List<String> sqlFiles, Map<String, String> detokenizingMappings)
    {
        Map<String, String> sqlStatements = new LinkedHashMap<String, String>();
        for (String sqlFile : sqlFiles)
        {
            String sql = readFile(sqlFile);
            for (String key : detokenizingMappings.keySet())
                sql = sql.replace(key, detokenizingMappings.get(key));
            sqlStatements.put(sqlFile, sql);
        }
        for (String sqlFile : sqlStatements.keySet())
        {
            logger.info("Executing " + sqlFile);
            executeSql(sqlStatements.get(sqlFile));
        }
    }

    private void executeSql(String sql)
    {
        Connection connection = null;
        Statement statement = null;
        try
        {
            connection = getConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();
            statement.executeUpdate(sql);
            connection.commit();
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (statement != null)
            {
                try
                {
                    statement.close();
                } catch (SQLException e)
                {
                    // swallow
                }
            }
            if (connection != null)
            {
                try
                {
                    connection.close();
                } catch (SQLException e)
                {
                    // swallow
                }
            }
        }
    }

    private String readFile(String file)
    {
        FileReader reader = null;
        try
        {
            reader = new FileReader(file);
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[8192];
            int readCount;
            while ((readCount = reader.read(buffer, 0, buffer.length)) > 0)
                sb.append(buffer, 0, readCount);
            return sb.toString();
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                } catch (IOException e)
                {
                    // swallow
                }
            }
        }
    }

    private void writeTablesToExcel(String schema, Map<String, String> tableToWorksheetMapping,
            String workbookFile)
    {
        logger.info("building output workbook: " + workbookFile);
        Workbook workbook = new HSSFWorkbook();
        for (String table : tableToWorksheetMapping.keySet())
            writeTableToExcel(schema, table, workbook, tableToWorksheetMapping.get(table));
        OutputStream os = null;
        try
        {
            os = new FileOutputStream(workbookFile);
            workbook.write(os);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                } catch (IOException e)
                {
                    // swallow
                }
            }
        }
    }

    private void writeTableToExcel(String schema, String table, Workbook workbook, String worksheet)
    {
        logger.info("  transferring table '" + table + "' to worksheet '" + worksheet + "'");
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try
        {
            connection = getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery("SELECT * FROM " + schema + "." + table);
            writeResultSetToExcel(resultSet, workbook, worksheet);
        } catch (SQLException e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (resultSet != null)
            {
                try
                {
                    resultSet.close();
                } catch (SQLException e)
                {
                    // swallow
                }
            }
            if (statement != null)
            {
                try
                {
                    statement.close();
                } catch (SQLException e)
                {
                    // swallow
                }
            }
            if (connection != null)
            {
                try
                {
                    connection.close();
                } catch (SQLException e)
                {
                    // swallow
                }
            }
        }
    }

    private void writeResultSetToExcel(ResultSet resultSet, Workbook workbook, String worksheet)
            throws SQLException
    {
        Sheet sheet = workbook.createSheet(worksheet);
        ResultSetMetaData metaData = resultSet.getMetaData();

        CellStyle boldStyle = workbook.createCellStyle();
        Font f = workbook.createFont();
        f.setBoldweight(Font.BOLDWEIGHT_BOLD);
        boldStyle.setFont(f);

        int rowCounter = 0;
        Row row = sheet.createRow(rowCounter++);
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++)
        {
            Cell cell = row.createCell(i - 1);
            cell.setCellValue(metaData.getColumnName(i));
            cell.setCellStyle(boldStyle);
        }

        while (resultSet.next())
        {
            row = sheet.createRow(rowCounter++);
            for (int i = 0; i < columnCount; i++)
                writeValue(row.createCell(i), resultSet.getObject(i + 1));
        }

        for (int i = 0; i < columnCount; i++)
            sheet.autoSizeColumn(i);
    }

    private void writeValue(Cell cell, Object object)
    {
        if (object instanceof Number)
        {
            if (object instanceof Double) cell.setCellValue((Double) object);
            else if (object instanceof Float) cell.setCellValue((Float) object);
            else if (object instanceof Long) cell.setCellValue((Long) object);
            else if (object instanceof Integer) cell.setCellValue((Integer) object);
            else if (object instanceof Short) cell.setCellValue((Short) object);
            else if (object instanceof Byte) cell.setCellValue((Byte) object);
            else cell.setCellValue(((Number) object).doubleValue());
        } else
        {
            cell.setCellValue(object.toString());
        }
    }

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
            throw new RuntimeException(e);
        }
        return DriverManager.getConnection(connectionUrl);
    }
}
