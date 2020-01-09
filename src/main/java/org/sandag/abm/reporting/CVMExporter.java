package org.sandag.abm.reporting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.pb.common.datafile.DataTypes;
import com.pb.common.datafile.OLD_CSVFileReader;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.matrix.Matrix;

public class CVMExporter {
    private static final Logger     logger                      = Logger.getLogger(DataExporter.class);

	public final String[] cvmPeriodNames = {"OE","AM","MD","PM","OL"};
	public final String[] periodNames = {"EA","AM","MD","PM","EV"};
	public final String[] cvmClassNames = {"L","M","I","H"};
	public final String[] skimTollClassNames = {"SOV_NT_H","TRK_L","TRK_M","TRK_H"};
	public final String[] skimNonTollClassNames = {"SOV_TR_H","TRK_L","TRK_M","TRK_H"};
	public final String[] nonTollSkims = {"TIME","DIST"};
	public final String[] tollSkims = {"TIME","DIST","TOLLCOST"};
	public final String[] cvmModeNames = {"NT","T"};
	public final String[] modelModeNames = {"GP","TOLL"};
	
	public final String[] segmentNames = {"FA","RE","GO","IN","SV","WH","TH"};
	
    protected Properties        properties;
    protected String            projectPath;
    protected String            reportPath;
    protected HashMap<String,Matrix> cvmSkimMap;
    protected HashMap<String,String> periodMap; //lookup cvm period, return model period
    protected HashMap<String,String> tollClassMap;   //lookup cvm class, return toll skim class
    protected HashMap<String,String> nonTollClassMap;   //lookup cvm class, return non-toll skim class
    
    protected HashMap<String,String> modeMap; //lookup cvm mode, return model mode
    
    private final OMXMatrixDao mtxDao;
    protected float autoOperatingCost;
    
	
	public CVMExporter(Properties theProperties, OMXMatrixDao aMtxDao){
		this.properties = theProperties;
        this.mtxDao = aMtxDao;
        projectPath = properties.getProperty("scenario.path");
		reportPath = properties.getProperty("report.path");
	    float fuelCost = new Float(properties.getProperty("aoc.fuel"));
	    float mainCost = new Float(properties.getProperty("aoc.maintenance"));
	    autoOperatingCost = (fuelCost + mainCost)  * 0.01f;

	}
	
	private void createPeriodMap(){
		
		periodMap = new HashMap<String,String>();
		for(int i = 0; i<cvmPeriodNames.length;++i)
			periodMap.put(cvmPeriodNames[i],periodNames[i]);
	}
	
	private void createClassMap(){
		nonTollClassMap = new HashMap<String,String>();
		tollClassMap = new HashMap<String,String>();
		for(int i = 0; i<cvmClassNames.length;++i){
			nonTollClassMap.put(cvmClassNames[i],skimNonTollClassNames[i]);
			tollClassMap.put(cvmClassNames[i],skimTollClassNames[i]);
		}
	}
	
	private void createModeMap(){
		modeMap = new HashMap<String,String>();
		for (int i = 0; i < cvmModeNames.length;++i)
			modeMap.put(cvmModeNames[i], modelModeNames[i]);
	}
	
	public HashMap<String, Integer> export(){
		createPeriodMap();
		createClassMap();
		createModeMap();
		readSkims();
		TableDataSet inputData = readCVMTrips();
		int totalRows = inputData.getRowCount();
		float[] timeCol = new float[totalRows];
		float[] distCol = new float[totalRows];
		float[] aocCol  = new float[totalRows];
		float[] tollCol = new float[totalRows];
		
		HashMap<String, Integer> tripIndexMap = new HashMap<String, Integer>();
		
		for(int row = 1; row<=totalRows;++row){
			
			int otaz = (int) inputData.getValueAt(row, "I");
			int dtaz = (int) inputData.getValueAt(row, "J");
			String cvmPeriod = inputData.getStringValueAt(row,"OriginalTimePeriod");
			String cvmClass = inputData.getStringValueAt(row,"Mode");
			String cvmMode = inputData.getStringValueAt(row,"TripMode");
			String serialNo = inputData.getStringValueAt(row, "SerialNo");
			int tripId = (int) inputData.getValueAt(row, "Trip");
			
			//tripIndexMap is used CVMScaler to assign trip ids for new records
			if (tripIndexMap.containsKey(serialNo)){
				int value = tripIndexMap.get(serialNo);
				if (value>tripId) tripId = value;
			}
			tripIndexMap.put(serialNo, tripId);
			
			Matrix timeMatrix = null;
			Matrix distMatrix = null;
			Matrix tollMatrix = null;
			
			String modelPeriod = periodMap.get(cvmPeriod);
			String modelClass = null;
			if(cvmMode.equals("NT")){
				modelClass = nonTollClassMap.get(cvmClass);

			}else{
				modelClass = tollClassMap.get(cvmClass);
			}
			
			timeMatrix = cvmSkimMap.get(modelPeriod+"_"+modelClass+"_"+"TIME"); 
			distMatrix = cvmSkimMap.get(modelPeriod+"_"+modelClass+"_"+"DIST");
			if(cvmSkimMap.containsKey(modelPeriod+"_"+modelClass+"_"+"TOLLCOST"))
				tollMatrix = cvmSkimMap.get(modelPeriod+"_"+modelClass+"_"+"TOLLCOST");
			
			timeCol[row-1] = timeMatrix.getValueAt(otaz, dtaz);
			distCol[row-1] = distMatrix.getValueAt(otaz, dtaz);
			aocCol[row-1]  = distMatrix.getValueAt(otaz, dtaz) * autoOperatingCost;
			if(tollMatrix != null)
				tollCol[row-1] = tollMatrix.getValueAt(otaz, dtaz);
			
		}
		
		//append the columns
		inputData.appendColumn(timeCol, "TIME");
		inputData.appendColumn(distCol, "DIST");
		inputData.appendColumn(aocCol, "AOC");
		inputData.appendColumn(tollCol, "TOLLCOST");
		
		//write the data
		TableDataSet.writeFile(reportPath+"cvm_trips.csv", inputData);
		return tripIndexMap;
	}
	
	/**
	 * Read data into inputDataTable tabledataset.
	 * 
	 */
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
	
	private void readSkims(){
		
		cvmSkimMap = new HashMap<String, Matrix>();
		
		for(String period: periodNames){
			
			String fileName = "traffic_skims_"+period+".omx";

			for(String nonTollClass: skimNonTollClassNames){
				
			    for(String skim:nonTollSkims){
			
			    	String skimName = period+"_"+nonTollClass+"_"+skim;
			    	
                    Matrix m = mtxDao.getMatrix(fileName, skimName);
                    cvmSkimMap.put(skimName, m);

			    }
			}
			for(String tollClass: skimTollClassNames){
			    for(String skim:tollSkims){
			    	String skimName = period+"_"+tollClass+"_"+skim;
			    	
                    Matrix m = mtxDao.getMatrix(fileName, skimName);
                    cvmSkimMap.put(skimName, m);

			    }
			}
		}
		
		
		
		
	}
	
	/**
	 * Helper method to read in all the CVM files and concatenate into one TableDataSet.
	 * 
	 * @return the concatenated data
	 */
	private TableDataSet readCVMTrips(){
		
		int tables = 0;
		String[] header = null;
		int[] columnType = null;
		HashMap<String, ArrayList<Float>> floatCols = new HashMap<String, ArrayList<Float>>();
		HashMap<String, ArrayList<String>> stringCols = new HashMap<String, ArrayList<String>>();
		
		//first read all the data, and store arraylists of data in the two hashmaps
		for(String segment: segmentNames){
			
			for(String period:cvmPeriodNames){
				
				String fileName = projectPath+"output\\Trip_"+segment+"_"+period+".csv";
				TableDataSet inData = readTableDataSet(fileName);
				if(tables==0){
					columnType = inData.getColumnType();
					header = inData.getColumnLabels();
				}
				++tables;
				for(int i = 0; i< inData.getColumnCount();++i){
					
					String colName = header[i];
					if(columnType[i]==DataTypes.NUMBER){
						float[] data = inData.getColumnAsFloat(colName);
						ArrayList<Float> colArray = null;
						if(floatCols.containsKey(colName))
							colArray = floatCols.get(colName);
						else
							colArray = new ArrayList<Float>();
						
						for(int j=0;j<data.length;++j)
							colArray.add(data[j]);
						
						floatCols.put(colName, colArray);
					}else{
						String[] data = inData.getColumnAsString(colName);
						ArrayList<String> colArray = null;
						if(stringCols.containsKey(colName))
							colArray = stringCols.get(colName);
						else
							colArray = new ArrayList<String>();
						
						for(int j=0;j<data.length;++j)
							colArray.add(data[j]);
						
						stringCols.put(colName, colArray);
					}						
				}
	
			}
			
		}

		//now all the data is read into the two hashmaps. Construct the tableDataSet with the
		//float data, then append the string data
		Set<String> keySet = floatCols.keySet();
		String[] colNames = new String[keySet.size()];
		float[][] data = null;
		int colNumber=0;
		for(String colName:keySet){
			colNames[colNumber] = colName;
			ArrayList<Float> col = floatCols.get(colName);
			if(colNumber==0){
			
				data = new float[col.size()][colNames.length];	
			}
			for(int i = 0; i < col.size();++i){
				data[i][colNumber] = col.get(i);
			}
			++colNumber;
		}
		
		TableDataSet allTrips = TableDataSet.create(data,colNames);
		
		//logger.info("Created table data set with "+ allTrips.getColumnCount()+" columns");
		//String outputColNames = "";
		//for(String colName : allTrips.getColumnLabels())
		//	outputColNames += (colName + " ");
		//logger.info("Columns: "+outputColNames);
		
		//now append the string columns
		keySet = stringCols.keySet();
		colNames = new String[keySet.size()];
		colNumber=0;
		for(String colName:keySet){
			colNames[colNumber] = colName;
			ArrayList<String> col = stringCols.get(colName);
			String[] stringData = new String[col.size()];
			stringData = col.toArray(stringData);
			logger.info("Appending string column "+colName+" with "+stringData.length+" elements");
			allTrips.appendColumn(stringData, colName);
		}
		
		//logger.info("Final table data set has "+ allTrips.getColumnCount()+" columns");
		//outputColNames = "";
		//for(String colName : allTrips.getColumnLabels())
		//	outputColNames += (colName + " ");
		//logger.info("Columns: "+outputColNames);

		return allTrips;
			
			
	}
	

}
