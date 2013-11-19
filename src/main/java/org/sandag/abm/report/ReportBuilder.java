package org.sandag.abm.report;

import com.pb.common.util.ResourceUtil;
import com.sun.rowset.*;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import javax.sql.rowset.*;

/**
 * Write a series of queries to named ranges using a template Excel spreadsheet.
 *
 * @author Matt Keating
 *         11/19/2013 
 */
public class ReportBuilder {
    private static final Logger logger = Logger.getLogger(ReportBuilder.class);
    private static boolean first = true;

    public static void main(String ... args) throws IOException, SQLException {
        HashMap<String, String> sqlMap;

        ResourceBundle properties =  ResourceUtil.getResourceBundle("report");
        sqlMap = ResourceUtil.getResourceBundleAsHashMap("reportSql");
        String schemaKey = properties.getString("sql.scripts.schema.key");
        final String workbookName = properties.getString("workbook.name");
        final String worksheetName = properties.getString("worksheet.name");
        String schemas = properties.getString("database.schemas");
            
        Map<String,String> detokenizingMap = new HashMap<String,String>();
        for (String schema : schemas.trim().split(","))
            detokenizingMap.put(schema,schemaKey);
        
        ReportBuilder rb = new ReportBuilder();
        rb.writeReport(schemas,sqlMap,detokenizingMap,workbookName,worksheetName);
        logger.info("Finished writing report data!");
    }
    /**
    *
    * @param schemas String a comma-separated list of schemas to compare
    * @param sqlStatements Map<String,String> a series of named SQL statements
    * @param detokenizingMappings Map<String,String> a mapping from a generic token to schema names
    * @param workbookName String the template Excel workbook
    * @param worksheetName String the data tab to be modified
    */
    private void writeReport(String schemas, Map<String,String> sqlStatements, Map<String,String> detokenizingMappings, String workbookName, String worksheetName) 
            throws IOException, SQLException {
        
        clearExcelData(workbookName, workbookName, worksheetName);
        
        SqlConnect db = SqlConnect.getDbCon();
        
        for (Map.Entry<String,String> entry : sqlStatements.entrySet()) {
            Result result;
            ArrayList<Result> table = new ArrayList<Result>();
            String sqlName = entry.getKey();
            String sqlStatement = entry.getValue();
            String[] splitSqlName = sqlName.split("\\.");
            String sqlTemp;
            String sqlSheetName = splitSqlName[0];
            String criteriaLabel = splitSqlName[1];
            for (String schema : schemas.trim().split(",")) {
                logger.info("Executing " + sqlName + " for schema " + schema);
                sqlTemp = sqlStatement.replace(detokenizingMappings.get(schema),schema);
                result = getResult(db,sqlTemp,sqlName,sqlSheetName,criteriaLabel);
                table.add(result);
                
            }
            // if multiple columns, transpose as single row
            writeResultToExcel(table,workbookName,worksheetName,schemas);
        }
    }

    /**
    *
    * @param db SqlConnect an open database connection
    * @param sql the SQL statement to run
    * @param sqlName String the name of the SQL statement
    * @param sqlSheetName String the category for the criteria (usually displayed on it's own worksheet)
    * @oaram criteriaLabel String the label for the criteria
    * @return a Result
    */
    private Result getResult(SqlConnect db, String sql, String sqlName, String sqlSheetName, String criteriaLabel) throws SQLException {
        HashMap<String, String> sqlInMap;
        CachedRowSet rs = null;
        sqlInMap = ResourceUtil.getResourceBundleAsHashMap("reportSqlIn");
        String inString = sqlInMap.get(sqlName);
        Result result = new Result(sqlSheetName, criteriaLabel);
        rs = new CachedRowSetImpl();
        try {
                if (!sql.contains("%s")) {
                    rs = db.querySimple(sql);
                } else {
                    rs = db.queryIn(sql, inString);
                }
            ResultSetMetaData rsmd = rs.getMetaData();
            int row = 0;
            int cols = rsmd.getColumnCount(); 
            while (rs.next()) {
                result.addRow();
                int col;
                for (col = 0; col < cols; col = col + 1) {
                    String label = null;
                    result.addResult(row,col,rs.getFloat(col+1));
                    label = rsmd.getColumnLabel(col+1);
                    result.addResultLabel(label);
                }
                row += 1;
            } 
        } catch (Exception sqle) {
            logger.fatal(sqle);
        } finally {
            rs.close();
        }
        return result;
    }
    /**
    *
    * @param infileName String the name of the source workbook
    * @param outfileNmae String the name of the output notebook. (Usually the same as the input one)
    * @param worksheetName String the name of the worksheet to delete.
    */
    private void clearExcelData(String infileName, String outfileName, String worksheetName) {
        Workbook workbook = null;
        // Clear existing results from workbook
        try {
            FileInputStream infile = new FileInputStream(new File(infileName)); 
            workbook = new HSSFWorkbook(infile);
            workbook.removeSheetAt(workbook.getSheetIndex(worksheetName));
            workbook.createSheet(worksheetName);
            
            // delete existing named ranges
            int names = workbook.getNumberOfNames();
            for (int i = names; i > 0; i--){
                workbook.removeName(i-1);
            }

        } catch (IOException ioe) {
            logger.fatal(ioe);        
            }
        
        try {
            FileOutputStream outfile = new FileOutputStream(new File(outfileName));
            workbook.write(outfile);
            outfile.flush();
            outfile.close();
        } catch (IOException ioe) {
            logger.fatal(ioe);        
            }
    }

    /**
    *
    * @param table ArrayList<Result> a collection of result objects from multiple queries
    * @param workbookName String the name of the workbook to write to
    * @param worksheetName String the name of the worksheet to write to
    * @param schemas String a comma separated list of schemas to compare
    */
    private void writeResultToExcel(ArrayList<Result> table, String workbookName, String worksheetName, String schemas) {
        Workbook workbook = null;
        
        try {
            FileInputStream infile = new FileInputStream(new File(workbookName)); 
            workbook = new HSSFWorkbook(infile);
            Sheet sheet = workbook.getSheet(worksheetName);
            if (first) {
                // write header
                ArrayList<String> header = new ArrayList<String>();
                header.add("Criteria");
                for (String schema : schemas.trim().split(","))
                    header.add(schema);
                String endCol = null;
                sheet.createRow(0);
                Row row = sheet.getRow(0);
                for (int i = 0; i < header.size(); i++) {
                    row.createCell(i);
                    Cell cell = row.getCell(i);
                    cell.setCellValue(header.get(i));
                    endCol = CellReference.convertNumToColString(i);
                }             
                Name namedRange = workbook.createName();
                String rangeName = "schemas";
                
                String reference = "%s!$%s$%s:$%s$%s";
                reference = String.format(reference,worksheetName,"B",1,endCol,1);
                
                namedRange.setNameName(rangeName);
                namedRange.setRefersToFormula(reference);
                first = false;                
            }             
            int rowNum = sheet.getLastRowNum() + 1;
            // all results have same structure so take first as representative
            Result tmp = table.get(0);
            for (int c = 0; c < tmp.getAllResultLabels().size(); c++) {
                logger.info(String.format("Writing criteria %s %s %s",tmp.getWorksheetName(), tmp.getCriteriaLabel(), tmp.getResultLabel(c)));
                sheet.createRow(rowNum);
                Row row = sheet.getRow(rowNum);
                String rangeName = "%s.%s.%s";
                String reference = null;
                String startCol = null,endCol = null;
                int startRow = 0,endRow = 0;
                Name namedRange = workbook.createName();
                rangeName = String.format(rangeName,tmp.getWorksheetName(),tmp.getCriteriaLabel(),tmp.getResultLabel(c)); 
                ArrayList<Float> res = new ArrayList<Float>();
                for (Result result : table) {
                    for (int r = 0; r < result.results.size(); r++) {
                    if (r == 0) {
                        row.createCell(r);
                        Cell cell = row.getCell(r);
                        cell.setCellValue(rangeName);
                        startRow = rowNum+1;
                        startCol = CellReference.convertNumToColString(r+1);
                    }
                    res.add(result.getResult(r, c));
                    }
                }
                for (int i = 0; i < res.size(); i++) {
                    row.createCell(i + 1);
                    Cell cell = row.getCell(i + 1);
                    cell.setCellValue(res.get(i));
                    endCol = CellReference.convertNumToColString(i+1);
                }
                endRow = rowNum+1;
                reference = "%s!$%s$%s:$%s$%s";
                reference = String.format(reference,worksheetName,startCol,startRow,endCol,endRow);
                
                namedRange.setNameName(rangeName);
                namedRange.setRefersToFormula(reference);
                
                rowNum += 1;
            }
        int cols = countOccurrences(schemas,",".charAt(0));
        for (int i = 0; i < cols+2; i++){
            sheet.autoSizeColumn(i);
        }
        } catch (IOException ioe) {
            logger.fatal(ioe);        
        }
        
        try {
            FileOutputStream outfile = new FileOutputStream(new File(workbookName));
            workbook.write(outfile);
            outfile.flush();
            outfile.close();
        } catch (IOException ioe) {
            logger.fatal(ioe);        
        }        
    }
    
    public static int countOccurrences(String str, char substr)
    {
        int count = 0;
        for (int i=0; i < str.length(); i++)
        {
            if (str.charAt(i) == substr)
            {
                 count++;
            }
        }
        return count;
    }
}
