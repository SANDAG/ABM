package org.sandag.abm.validation;

import java.io.*;
import java.nio.file.Files;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class MainApplication {

	public static List<String> getHighwayFlowData(int scenario, ResourceBundle rb) throws SQLException {

		System.out.println("Getting highway flow data for scenario " + scenario + " from database ...");

		String query = "SELECT " + 
				" flow.scenario_id," + 
				"  link.hwycov_id " + 
				"  ,sum(CONVERT(bigint,(flow.flow + link_ab_tod.preload / 3.0))) as total_flow " + 
				"FROM " + 
				"  abm_13_2_3.abm.hwy_flow flow " + 
				"JOIN abm_13_2_3.abm.hwy_link_ab_tod link_ab_tod  " + 
				"  ON flow.scenario_id = link_ab_tod.scenario_id AND flow.hwy_link_ab_tod_id = link_ab_tod.hwy_link_ab_tod_id " + 
				"JOIN abm_13_2_3.abm.hwy_link_tod link_tod " + 
				"  ON link_ab_tod.scenario_id = link_tod.scenario_id AND link_ab_tod.hwy_link_tod_id = link_tod.hwy_link_tod_id " + 
				"JOIN abm_13_2_3.abm.hwy_link link " + 
				"  ON link_tod.scenario_id = link.scenario_id AND link_tod.hwy_link_id = link.hwy_link_id " + 
				"JOIN ref.ifc ifc " + 
				"  ON link.ifc = ifc.ifc " + 
				"WHERE " + 
				"  flow.scenario_id = " + scenario + 
				"GROUP BY " + 
				"  flow.scenario_id, link.hwycov_id " + 
				"ORDER BY " + 
				"  hwycov_id";

		System.out.println("");
		return getSqlData(query, rb);
	}

	public static List<String> getTransitBoardingByModeData(int scenario, ResourceBundle rb) throws SQLException {

		System.out.println("Getting transit boarding by mode data for scenario " + scenario + " from database ...");

		String query = "SELECT transit_mode_desc,transit_access_mode_desc,sum(boardings) " + 
				"FROM ws.dbo.transitBoardingSummary(" + scenario + ") " + 
				"GROUP BY transit_mode_desc,transit_access_mode_desc " + 
				"ORDER BY transit_mode_desc,transit_access_mode_desc";
		System.out.println("");
		return getSqlData(query, rb);
	}

	public static List<String> getTransitBoardingByRouteData(int scenario, ResourceBundle rb) throws SQLException {

		System.out.println("Getting transit boarding by route data for scenario " + scenario + " from database ...");

		String query = "SELECT rtt, sum(boardings) as boardings " + 
				"FROM " + 
				"(SELECT (config/1000) as rtt, boardings " + 
				"FROM ws.dbo.transitBoardingSummary(" + scenario + ")" + " AS boarding " + 
				"JOIN abm_13_2_3.abm.transit_route route " + 
				"  ON boarding.route_id = route.transit_route_id " + 
				"WHERE scenario_id = " + scenario + ") as t " + 
				"GROUP BY rtt " + 
				"ORDER BY rtt";
		System.out.println("");
		return getSqlData(query, rb);
	}

	public static List<String> getSqlData(String query, ResourceBundle rb) throws SQLException {

		String dbHost = getProperty(rb, "database.host", null);
		String dbName = getProperty(rb, "database.name", null);
		String user = getProperty(rb, "database.user", null);
		String password = getProperty(rb, "database.pwd", null);

		//		System.out.println( dbHost );
		//		System.out.println( user );
		//		System.out.println( password );
		//		System.out.println( dbName );

		Connection conn = null;
		conn = getConnectionToDatabase(dbHost, dbName, user, password);

		Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet rs = stmt.executeQuery(query);
		ResultSetMetaData rsmd = rs.getMetaData();

		int nCols = rsmd.getColumnCount();

		List<String> data = new ArrayList<String>();
		StringBuilder row = null;

		while(rs.next()){
			row = new StringBuilder();
			for(int c = 1; c <= nCols; c++){
				row.append(rs.getString(c));

				if(c < nCols)
					row.append("|");
			}
			data.add(row.toString());
		}

		conn.close();

		return data;
	}

	public static void printData(List<String> data){
		for(String str: data)
			System.out.println(str);
	}

	public static void printRow(String[] vals) {
		for (String i : vals) {
			System.out.print(i);
			System.out.print("\t");
		}
		System.out.println();
	}

	//Wu modified to allow writing out files to a different location other than input file location
	public static void writeHighwayDataToExcel(String input_file_name, String sheet_to_modify, List<String> data, int scenario, String outputDir) throws IOException {
		try {
			InputStream input_file = new FileInputStream(new File(input_file_name));
			XSSFWorkbook wb = new XSSFWorkbook(input_file);
			input_file.close();

			XSSFSheet ws = wb.getSheet(sheet_to_modify);
			int prevRows = ws.getLastRowNum();

			XSSFRow row = null;
			XSSFCell cell = null;

			// writing data to excel sheet 			
			int r = 1;
			for (String str : data) {
				row = ws.getRow(r);

				if(row == null){
					// this will happen when the number of links in the template sheet
					// are less than the number of highway links for the data obtained from database
					ws.createRow(r);
					row = ws.getRow(r);
				}

				//System.out.println(str);

				String[] vals = str.split("\\|");

				for(int c = 0; c < vals.length; c++){
					cell = row.getCell(c);

					if(cell == null){
						row.createCell(c);
						cell = row.getCell(c);
					}

					cell.setCellValue(Integer.valueOf(vals[c]));
				}
				r++;
			}

			// delete additional existing rows, if any
			// this is to remove excel rows if the highway links in the template sheet
			// are more than the number of highway links for the data obtained from database)
			for(int d = data.size() + 1; d < prevRows; d++){
				row = ws.getRow(d);
				ws.removeRow(row);
			}

			//update all formula calculation
			wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
			wb.setForceFormulaRecalculation(true);

			//			FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
			//
			//			for (int i = 0; i < wb.getNumberOfSheets(); i++)
			//			{
			//				Sheet sheet = wb.getSheetAt(i);
			//				System.out.println("Sheet name " + sheet.getSheetName());
			//				for (Row ro : sheet) {
			//					if(sheet.getSheetName().equalsIgnoreCase("all_nb")){
			//						System.out.println("row num " + ro.getRowNum());
			//					}
			//					for (Cell c : ro) {
			//						if (c.getCellTypeEnum() == CellType.FORMULA) {
			//							if(sheet.getSheetName().equalsIgnoreCase("all_nb")){
			//								System.out.println("col index " + c.getColumnIndex());
			//							}
			//							try {
			//								evaluator.evaluateFormulaCellEnum(c);
			//							} catch (Exception e) {
			//								// TODO Auto-generated catch block
			//								e.printStackTrace();
			//							}
			//						}
			//					}
			//				}
			//			}

			//forming output file name 
			String name = input_file_name.substring(0, input_file_name.lastIndexOf("."));
			String ext = input_file_name.substring(input_file_name.lastIndexOf(".") + 1);
			String output_file_name = outputDir+name + "_s" + String.valueOf(scenario) + "." + ext;

			//writing out revised excel file
			System.out.println("Writing highway data to excel file : " + output_file_name);

			File file = new File(output_file_name);
			Files.deleteIfExists(file.toPath());

			OutputStream output_file = new FileOutputStream(new File(output_file_name));
			wb.write(output_file);
			wb.close();
			output_file.close();
			System.out.println("");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeBoardingDataToExcel(String input_file_name, String sheet_to_modify, List<String> data, int scenario, String output_file_name) throws IOException {
		try {
			InputStream input_file = new FileInputStream(new File(input_file_name));
			XSSFWorkbook wb = new XSSFWorkbook(input_file);
			input_file.close();

			XSSFSheet ws = wb.getSheet(sheet_to_modify);
			int prevRows = ws.getLastRowNum();

			XSSFRow row = null;
			XSSFCell cell = null;

			boolean[] colIsNumeric = null;

			// writing data to excel sheet 			
			int r = 1;
			for (String str : data) {
				row = ws.getRow(r);

				if(row == null){
					// this will happen when the number of routes/modes in the template sheet
					// are less than the number of routes/modes for the data obtained from database
					ws.createRow(r);
					row = ws.getRow(r);
				}

				String[] vals = str.split("\\|");

				//identify columns as string or int/double value
				if(r == 1){
					colIsNumeric = new boolean[vals.length];
					Arrays.fill(colIsNumeric, Boolean.TRUE);

					for(int c = 0; c < vals.length; c++)
						colIsNumeric[c] = isNumeric(vals[c]);
				}

				for(int c = 0; c < vals.length; c++){

					cell = row.getCell(c);

					if(cell == null){
						row.createCell(c);
						cell = row.getCell(c);
					}
					if(colIsNumeric[c])
						cell.setCellValue(Double.valueOf(vals[c]).intValue());
					else
						cell.setCellValue(vals[c]);
				}
				r++;
			}

			// delete additional existing rows, if any
			// this is to remove excel rows if the transit routes/modes in the template sheet
			// are more than the number of transit routes/modes for the data obtained from database)
			for(int d = data.size() + 1; d < prevRows; d++){
				row = ws.getRow(d);
				ws.removeRow(row);
			}

			//update all formula calculation
			wb.getCreationHelper().createFormulaEvaluator().evaluateAll();
			wb.setForceFormulaRecalculation(true);

			//writing out revised excel file
			System.out.println("Writing boarding data to excel file : " + output_file_name);

			File file = new File(output_file_name);
			Files.deleteIfExists(file.toPath());

			OutputStream output_file = new FileOutputStream(new File(output_file_name));
			wb.write(output_file);
			output_file.close();
			System.out.println("");
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean isNumeric(String str)  
	{  
		try{  
			double d = Double.parseDouble(str);  
		}  
		catch(NumberFormatException nfe) {  
			return false;  
		}  
		return true;  
	}

	public static String getProperty(ResourceBundle rb, String keyName, String defaultValue) {

		String keyValue = defaultValue;

		try {
			keyValue = rb.getString(keyName);
		} catch (RuntimeException e) {
			//key was not found or resource bundle is null
			if(rb == null) throw new RuntimeException("ResourceBundle is null", e);
		}

		if(keyValue == null) return keyValue;  //you can't trim a null.

		return keyValue.trim();
	}

	/**
	 * @param dbHost is database server name
	 * @param dbName is the name of the database we want to use in the server
	 * @param user is the name of the user used to login to access the database.  Note for MS_SQL, user can be null,
	 * which indicates that standard MS Windows authentication will be used.  In this case sqljdbc_auth.dll must be in the java library path.
	 * @param password is the user's password - not necessary if user is null.
	 * @return the connection instance for the URL formed for the specific database server specified
	 */

	public static Connection getConnectionToDatabase(String dbHost, String dbName, String user, String password) {
		Connection conn = null;

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		} catch (ClassNotFoundException e1) {
			throw new RuntimeException("Class not found ", e1);
		}	

		try {
			String urlToConnect = "jdbc:sqlserver://" + dbHost + ";" + "database=" + dbName + ";" + 
					(user != null ? ("user=" + user + ";" + "password=" + password) : "integratedSecurity=True");

			//System.out.println("urlToConnect " + urlToConnect);
			conn = DriverManager.getConnection(urlToConnect);
		}
		catch (SQLException e) {
			throw new RuntimeException("Cannot connect to database ", e);
		}
		return conn;
	}

	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();


		//		Collection<String> supportedFuncs = WorkbookEvaluator.getSupportedFunctionNames();
		//		System.out.println(supportedFuncs);
		//		
		//		Collection<String> unsupportedFuncs = WorkbookEvaluator.getNotSupportedFunctionNames();
		//		System.err.println(unsupportedFuncs);
		//		System.exit(-1);

		if ( args.length < 2 ) {
			System.out.println( "invalid number of command line arguments." );
			System.out.println( "two argument must be specified, 1) basename of the properties file and 2) scenario number");
			System.exit(-1);
		}

		ResourceBundle rb = ResourceBundle.getBundle( args[0] );

		int scenario = Integer.valueOf(args[1]);
		
		//Wu modified to allow writing out files to a different location other than input file location		
		String outputDir=args[2];

		String sheet_to_modify = null;

		List<String> flow_data = getHighwayFlowData(scenario, rb);

		// revise summary by class workbook
		String summary_by_class_file = rb.getString("highway.summary.class.template");
		sheet_to_modify = rb.getString("sheet.to.modify.highway.summary");
		writeHighwayDataToExcel(summary_by_class_file, sheet_to_modify, flow_data, scenario,outputDir);

		// revise summary by corridor workbook
		String summary_by_corridor_file = rb.getString("highway.summary.corridor.template");
		sheet_to_modify = rb.getString("sheet.to.modify.highway.summary");
		writeHighwayDataToExcel(summary_by_corridor_file, sheet_to_modify, flow_data, scenario, outputDir);

		// revise transit validation workbook
		String transit_validation_file = rb.getString("transit.validation.template");

		//forming transit output file name 
		String name = transit_validation_file.substring(0, transit_validation_file.lastIndexOf("."));
		String ext = transit_validation_file.substring(transit_validation_file.lastIndexOf(".") + 1);
		String output_file_name = name + "_s" + String.valueOf(scenario) + "." + ext;
		String transit_output_file =outputDir+output_file_name;

		List<String> boarding_by_route_data = getTransitBoardingByRouteData(scenario, rb);
		sheet_to_modify = rb.getString("sheet.to.modify.transit.boarding.route");
		writeBoardingDataToExcel(transit_validation_file, sheet_to_modify, boarding_by_route_data, scenario, transit_output_file);

		List<String> boarding_by_mode_data = getTransitBoardingByModeData(scenario, rb); 
		sheet_to_modify = rb.getString("sheet.to.modify.transit.boarding.mode");
		writeBoardingDataToExcel(transit_output_file, sheet_to_modify, boarding_by_mode_data, scenario, transit_output_file);

		System.out.println( String.format( "%s%.1f%s", "Total Run Time - ", ( ( System.currentTimeMillis() - startTime ) / 1000.0 ), " seconds." ) );
	}

}