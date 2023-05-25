package org.sandag.abm.reporting;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;

public class CVMScaler {
	private static final Logger logger= Logger.getLogger(DataExporter.class);
    protected Properties        properties;
    protected String            projectPath;
    protected String            reportPath;
    protected String []         lightscalers;
    protected String []         mediumscalers;
    protected String []         heavyscalers;
    protected float         	lightshare;
    protected float         	mediumshare;
    protected float         	heavyshare;    
    
	public CVMScaler(Properties theProperties){
		this.properties = theProperties;
        projectPath = properties.getProperty("scenario.path");
		reportPath = properties.getProperty("report.path");
		String delims = "[,]";
		lightscalers = properties.getProperty("cvm.scale_light").split(delims);
		mediumscalers = properties.getProperty("cvm.scale_medium").split(delims);
		heavyscalers = properties.getProperty("cvm.scale_heavy").split(delims);
		lightshare = new Float(properties.getProperty("cvm.share.light"));
		mediumshare = new Float(properties.getProperty("cvm.share.medium"));
		heavyshare = new Float(properties.getProperty("cvm.share.heavy"));
	} 
	
	public void scale(){
		logger.info("Running CVM scaler ... ");
		String fileName = reportPath+"cvm_trips.csv";
		TableDataSet inData = readTableDataSet(fileName);
		int totalRows = inData.getRowCount();
		String[] timeCol=new String[totalRows];
		
		int tod=lightscalers.length;
		float [] lscaler=new float[tod];
		float [] mscaler=new float[tod];
		float [] hscaler=new float[tod];
				
		for(int i=0; i<tod; i++) {
			lscaler[i]=new Float(lightscalers[i].trim()).floatValue();
			mscaler[i]=new Float(mediumscalers[i].trim()).floatValue();
			hscaler[i]=new Float(heavyscalers[i].trim()).floatValue();
		}
		
		logger.info("Start writing cvm report");	
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(fileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
				
		String[] columnNames = inData.getColumnLabels();		
		String header=null;
		String colname;
		for (int col=1; col<=columnNames.length;++col) {
			colname = columnNames[col-1];
			if (col==1) header = colname;
			else header = header + "," + colname;
		}
		header = header + ",TRIPS";
		writer.println(header);
		
		for(int row = 1; row<=totalRows;++row){	
			timeCol[row-1]= inData.getStringValueAt(row,"TripTime");
			String str=timeCol[row-1];
			if(str.contains(":L")) {
				proceesRow(inData, columnNames, row, str, lscaler, lightshare, "L", writer);
			}else if(str.contains(":I")) {
				proceesRow(inData, columnNames, row, str, mscaler, 0, "I", writer);
			}else if(str.contains(":M")) {
				proceesRow(inData, columnNames, row, str, mscaler, mediumshare, "M", writer);
			}else if(str.contains(":H")) {
				proceesRow(inData, columnNames, row, str, hscaler, heavyshare, "H", writer);				
			}else {
				logger.info("Unrecognized CVM Vehicle type: "+ str);
			}
		}
			
		writer.close();
		logger.info("Finished cvm report");	
	}
	
	private void proceesRow(TableDataSet inData, String[] columnNames, int row, String str, float[] scalerArray, float share, String vehicle, PrintWriter writer) {
		float value_new = 0.0f;
		String colname;
		String value;
		float scaler;

		String line = null;
		String line_new = null;
		int totalCols = columnNames.length;
		for (int col=1; col<=totalCols;++col){
			
			colname = columnNames[col-1];
			value = inData.getStringValueAt(row, col);
			
			if (line==null) line = value;
			else line = line + "," + value;				
			
			if (share>0) {
				if (colname.equals("Mode")){
					value = value.replaceAll(vehicle, "I");
				}else if (colname.equals("TripTime")){
					value = value.replaceAll(":"+vehicle, ":I");
				}
				
				if (line_new==null) line_new = value;
				else line_new = line_new + "," + value;
			}
		}
		
		//write existing lines
		scaler = getScaler(scalerArray, str);
		value_new = scaler * (1-share);
		line = line + "," + Float.toString(value_new);
		writer.println(line.trim());
		
		//write new lines
		if (share>0) {
			value_new = scaler * (share);
			line_new = line_new + "," + Float.toString(value_new);
			writer.println(line_new.trim());
		}
	}
	
	private float getScaler(float [] scalerArray, String str) {
		float scaler = 1.0f;
		
		if(str.contains("_EA")){
			scaler = scalerArray[0];
		}else if(str.contains("_AM")){
			scaler = scalerArray[1];
		}else if(str.contains("_MD")){
			scaler = scalerArray[2];
		}else if(str.contains("_PM")){
			scaler = scalerArray[3];
		}else if(str.contains("_EV")){
			scaler = scalerArray[4];
		}else {
			scaler = 1.0f;
		}
		
		return scaler;
	
	}
	
	private TableDataSet readTableDataSet(String inputFile){
		
		logger.info("Begin reading the data in file " + inputFile);
		TableDataSet inputDataTable = null;
	    
	    try
	    {
	    	OLD_CSVFileReader csvFile = new OLD_CSVFileReader();
	        inputDataTable = csvFile.readFile(new File(inputFile));
	    } catch (IOException e)
	    {
	    	throw new RuntimeException(e);
        }
        logger.info("End reading the data in file " + inputFile);
        
        return inputDataTable;
	}
}
