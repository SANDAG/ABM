package org.sandag.abm.reporting;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    
	public CVMScaler(Properties theProperties){
		this.properties = theProperties;
        projectPath = properties.getProperty("scenario.path");
		reportPath = properties.getProperty("report.path");
		String delims = "[,]";
		lightscalers = properties.getProperty("cvm.scale_light").split(delims);
		mediumscalers = properties.getProperty("cvm.scale_medium").split(delims);
		heavyscalers = properties.getProperty("cvm.scale_heavy").split(delims);
	} 
	
	public void scale(){
		
		String fileName = reportPath+"cvm_trips.csv";
		TableDataSet inData = readTableDataSet(fileName);
		int totalRows = inData.getRowCount();
		float[] tripsCol = new float[totalRows];
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
		
		for(int row = 1; row<=totalRows;++row){	
			timeCol[row-1]= inData.getStringValueAt(row,"Time");
			String str=timeCol[row-1];
			if(str.contains(":L")) {
				if(str.contains("_EA")){
					tripsCol[row-1] = lscaler[0];
				}else if(str.contains("_AM")){
					tripsCol[row-1] = lscaler[1];
				}else if(str.contains("_MD")){
					tripsCol[row-1] = lscaler[2];
				}else if(str.contains("_PM")){
					tripsCol[row-1] = lscaler[3];
				}else if(str.contains("_EV")){
					tripsCol[row-1] = lscaler[4];
				}else {
					tripsCol[row-1] =1.0f;
				}				
			}else if(str.contains(":M")) {
				if(str.contains("_EA")){
					tripsCol[row-1] = mscaler[0];
				}else if(str.contains("_AM")){
					tripsCol[row-1] = mscaler[1];
				}else if(str.contains("_MD")){
					tripsCol[row-1] = mscaler[2];
				}else if(str.contains("_PM")){
					tripsCol[row-1] = mscaler[3];
				}else if(str.contains("_EV")){
					tripsCol[row-1] = mscaler[4];
				}else {
					tripsCol[row-1] =1.0f;
				}					
			}else if(str.contains(":H")) {
				if(str.contains("_EA")){
					tripsCol[row-1] = hscaler[0];
				}else if(str.contains("_AM")){
					tripsCol[row-1] = hscaler[1];
				}else if(str.contains("_MD")){
					tripsCol[row-1] = hscaler[2];
				}else if(str.contains("_PM")){
					tripsCol[row-1] = hscaler[3];
				}else if(str.contains("_EV")){
					tripsCol[row-1] = hscaler[4];
				}else {
					tripsCol[row-1] =1.0f;
				}	
			}else {
				tripsCol[row-1] = 1.0f;					
			}
		}
		
		//append the columns
		inData.appendColumn(tripsCol, "TRIPS");	
		//write the data
		TableDataSet.writeFile(reportPath+"cvm_trips.csv", inData);
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
	/*
	 public static void main(String... args) throws Exception
	    {
	        Properties properties = new Properties();
	        properties.load(new FileInputStream("conf/sandag_abm.properties"));
	        CVMScaler cvmScaler = new CVMScaler(properties);
	        cvmScaler.scale();
	    }
	    */
}
