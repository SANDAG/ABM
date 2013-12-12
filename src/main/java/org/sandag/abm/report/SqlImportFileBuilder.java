package org.sandag.abm.report;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * The {@code SqlImportFileBuilder} ...
 *
 * @author crf
 *         Started 10/18/12 8:43 AM
 */
public class SqlImportFileBuilder {
    public static final String DATABASE_NAME = "SANDAGABM";

    public SqlImportFileBuilder() { }

    public void buildImportFile(String directory, List<String> tables, String outputFileName, String schema) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new File(directory,outputFileName));
            //build prefix
            writer.println("USE MASTER;");
            writer.println("IF db_id('" + DATABASE_NAME + "') IS NULL");
            writer.println("    CREATE DATABASE " + DATABASE_NAME + ";");
            writer.println("GO");
            writer.println("ALTER DATABASE " + DATABASE_NAME + " SET RECOVERY SIMPLE;");
            writer.println("USE " + DATABASE_NAME + ";");
            writer.println("IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata");
            writer.println("    WHERE schema_name='" + schema + "')");
            writer.println("        EXEC sp_executesql N'CREATE SCHEMA " + schema + "';");
            writer.println("GO");


            for (String table : tables) {
                BufferedReader reader = null;
                File sqlFile = new File(directory,table + ".sql");
                File csvFile = new File(directory,table + ".csv");
                if (csvFile.exists() && sqlFile.exists()) {
                    writer.println("IF OBJECT_ID('" + schema + "." + table.toUpperCase() + "','U') IS NOT NULL");
                    writer.println("    DROP TABLE " + schema + "." + table.toUpperCase() + ";");
                    try {
                            reader = new BufferedReader(new FileReader(sqlFile));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                writer.println(addSchemaInformation(line,schema));
                            }
                            writer.println();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                //swallow
                            }
                        }
                    }
                    writer.println("GO");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);

        } finally {
            if (writer != null)
                writer.close();
        }
    }

    private String addSchemaInformation(String line, String schema) {
        return line.replace("CREATE TABLE ","CREATE TABLE " + schema + ".")
                   .replace("BULK INSERT ","BULK INSERT " + schema + ".");
    }

    public static void main(String ... args) {
        //usage: input_dir schema_name
        String sourceDir = args[0];
        List<String> tables = new LinkedList<String>();
        File[] sqlFiles = new File(sourceDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("sql");
            }
        });
        File[] csvFiles = new File(sourceDir).listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("csv");
            }
        });
        for (File f : sqlFiles) {
            String csv = f.getName().replace(".sql",".csv");
            for (File ff : csvFiles) {
                if (ff.getName().equalsIgnoreCase(csv)) {
                    tables.add(csv.replace(".csv","").toLowerCase());
                    break;
                }
            }
        }

        new SqlImportFileBuilder().buildImportFile(sourceDir,tables,"sql_table_import.sql",args[1]);
    }
}
