package org.sandag.abm.application;

import gnu.cajo.invoke.Remote;
import gnu.cajo.utils.ItemServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.sandag.abm.active.sandag.PropertyParser;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.Util;
import org.sandag.abm.modechoice.TapDataManager;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.reporting.CsvRow;
import org.sandag.abm.reporting.DataExporter;
import org.sandag.abm.reporting.OMXMatrixDao;

import com.pb.common.matrix.Matrix;
import com.pb.common.matrix.MatrixReader;
import com.pb.common.matrix.MatrixType;
import com.pb.common.util.ResourceUtil;

/**
 * @author malinovskiyy
 * Produces p&r and k&r access connections
 * input files:
 *		impdan_xx.mtx: SOV non toll xx peak period skim matrix (3 cores: *SCST_XX (generalized cost), length(Skim) (distance in mile), and *STM_XX (Skim) (single driver xx time in minutes)
 *   	tap.ptype: tap, lot id, parking type, taz, capacity, distance from lot to tap
 * output files
 *		access.prp
 */
public class SandagMGRAtoPNR {
	
    private static Logger           logger                  = Logger.getLogger("createPNRAccessFile");
    public static final int         MATRIX_DATA_SERVER_PORT = 1171;
    
    private TapDataManager          tapManager;
    private int[] taps;
    private int[] tazs;
    private float[][][] tapParkingInfo;
    private HashMap<Integer, Integer> tapMap;
    private HashMap<Integer, ArrayList<float[]>> bestTapsMap;
    private Matrix distanceMtx;
    private Matrix timeMtx;
    
    private final Properties        properties;
    private final OMXMatrixDao mtxDao;
	
    private static final String FORMAL_PREMIUM 		= "taps.formal.premium.maxDist";	//8.0f;
	private static final String FORMAL_EXPRESS 		= "taps.formal.express.maxDist";	//8.0f;
	private static final String FORMAL_LOCAL 		= "taps.formal.local.maxDist";		//4.0f;
	private static final String INFORMAL_PREMIUM 	= "taps.informal.premium.maxDist";	//4.0f;
	private static final String INFORMAL_EXPRESS 	= "taps.informal.express.maxDist";	//4.0f;
	private static final String INFORMAL_LOCAL 		= "taps.informal.local.maxDist";	//2.0f;
	private static final String PREMIUM_MODES 		= "taps.premium.modes"; 			//new ArrayList<Integer>(){{add(4); add(5); add(6); add(7);}};
    private static final String EXPRESS_MODES 		= "taps.express.modes"; 			//new ArrayList<Integer>(){{add(8); add(9);}};
    private static final String LOCAL_MODES 		= "taps.local.modes"; 				//new ArrayList<Integer>(){{add(10);}};
    
    private static final String TAPS_SKIM 			= "taps.skim";
    private static final String TAPS_SKIM_DIST 		= "taps.skim.dist";
    private static final String TAPS_SKIM_TIME 		= "taps.skim.time";
    
    private static final String EXTERNAL_TAZs 		= "external.tazs";
    
    private double formalPremiumMaxD;
    private double formalExpressMaxD;
    private double formalLocalMaxD;
    private double informalPremiumMaxD;
    private double informalExpressMaxD;
    private double informalLocalMaxD;
    
    private ArrayList<Integer> premiumModes;
    private ArrayList<Integer> expressModes;
    private ArrayList<Integer> localModes;
    private ArrayList<Integer> externalTAZs;
    
    private static final String     PROJECT_PATH_PROPERTY_TOKEN = "%project.folder%";
    
    public SandagMGRAtoPNR(Properties theProperties, OMXMatrixDao aMtxDao, String projectPath, HashMap<String, String> rbMap)
    {
        this.properties = theProperties;
        this.mtxDao = aMtxDao;
        
        tapManager = TapDataManager.getInstance(rbMap);
        //tazManager = TazDataManager.getInstance(rbMap);
        this.taps = tapManager.getTaps();
        this.tapParkingInfo = tapManager.getTapParkingInfo();
        this.tapMap = getTAPMap();
        this.bestTapsMap = new HashMap<Integer, ArrayList<float[]>>();
        this.distanceMtx = aMtxDao.getMatrix((String)properties.get(TAPS_SKIM),(String)properties.get(TAPS_SKIM_DIST));
        this.timeMtx = aMtxDao.getMatrix((String)properties.get(TAPS_SKIM),(String)properties.get(TAPS_SKIM_TIME));
        this.tazs = this.distanceMtx.getExternalRowNumbers();
       
        
        formalPremiumMaxD = Double.parseDouble((String) properties.get(FORMAL_PREMIUM));
        formalExpressMaxD = Double.parseDouble((String) properties.get(FORMAL_EXPRESS));
        formalLocalMaxD = Double.parseDouble((String) properties.get(FORMAL_LOCAL));
        informalPremiumMaxD = Double.parseDouble((String) properties.get(INFORMAL_PREMIUM));
        informalExpressMaxD = Double.parseDouble((String) properties.get(INFORMAL_EXPRESS));
        informalLocalMaxD = Double.parseDouble((String) properties.get(INFORMAL_LOCAL));
        
        List<String> stringList = Arrays.asList(((String) properties.get(PREMIUM_MODES)).split("\\s*,\\s*"));
        premiumModes = new ArrayList<Integer>();
        for (int i = 0; i < stringList.size(); i++){
        	premiumModes.add(Integer.parseInt(stringList.get(i)));
        }
        
        stringList = Arrays.asList(((String) properties.get(EXPRESS_MODES)).split("\\s*,\\s*"));
        expressModes = new ArrayList<Integer>();
        for (int i = 0; i < stringList.size(); i++){
        	expressModes.add(Integer.parseInt(stringList.get(i)));
        }
        
        stringList = Arrays.asList(((String) properties.get(LOCAL_MODES)).split("\\s*,\\s*"));
        localModes = new ArrayList<Integer>();
        for (int i = 0; i < stringList.size(); i++){
        	localModes.add(Integer.parseInt(stringList.get(i)));
        }
        
        stringList = Arrays.asList(((String) properties.get(EXTERNAL_TAZs)).split("\\s*,\\s*"));
        externalTAZs = new ArrayList<Integer>();
        for (int i = 0; i < stringList.size(); i++){
        	externalTAZs.add(Integer.parseInt(stringList.get(i)));
        }
    }
    
	
	public HashMap<Integer, Integer> getTAPMap(){
		HashMap<Integer, Integer> tm = new HashMap<Integer, Integer>();
		for(int i = 0; i < taps.length; i++){
			int tap = taps[i];
			if(tap < tapParkingInfo.length && tapParkingInfo[tap] != null && tapParkingInfo[tap][0] != null){
				int taz = (int) tapParkingInfo[tap][1][0];
				tm.put(tap, taz);
			}
		}
		return tm;
	}	
	
	public void nearestTAPs(){
		for(int i = 1; i < tazs.length; i++) {
		    int currTAZ = tazs[i];
		    
		    //Skip externals
		    if(externalTAZs.contains(currTAZ))
		    	continue;
		    
		    ArrayList<float[]> reachableTAPs = new ArrayList<float[]>();
		    bestTapsMap.put(currTAZ, reachableTAPs);
		    
		    HashMap<Integer, float[]> modeMap = new HashMap<Integer, float[]>();
		    ArrayList<Integer> addedTaps = new ArrayList<Integer>();
		    
		    for(Integer j : tapMap.keySet()){
		    	int tapTAZ = tapMap.get(j);
		    	//distance to taz with the current tap
		    	float dist = distanceMtx.getValueAt(currTAZ, tapTAZ);
		    	float time = timeMtx.getValueAt(currTAZ, tapTAZ);
		    	int lotType = (int) tapParkingInfo[j][3][0];
		    	int mode = (int) tapParkingInfo[j][5][0];
		    	//dist = (float) (dist + (tapParkingInfo[j][4][0] / 5280.0));
		    	if(!modeMap.containsKey(mode) || modeMap.get(mode)[2] > dist){
		    		float[] vals = new float[4];
		    		vals[0] = j;
		    		vals[1] = time;
		    		vals[2] = dist;
		    		vals[3] = mode;
		    		modeMap.put(mode, vals);
		    	}
		    	
		    		//formal, premium, less than 8 miles
		    	if(	(lotType == 1 && premiumModes.contains(mode) && dist < formalPremiumMaxD) ||
		    		//formal, express, less than 8 miles
		    		(lotType == 1 && expressModes.contains(mode) && dist < formalExpressMaxD)	||
		    		//formal, local, less than 4 miles
		    		(lotType == 1 && localModes.contains(mode) && dist < formalLocalMaxD) ||
		    		//informal, premium, less than 4 miles
		    		(lotType > 1 && premiumModes.contains(mode) && dist < informalPremiumMaxD) ||
		    		//informal, express, less than 4 miles
		    		(lotType > 1 && expressModes.contains(mode) && dist < informalExpressMaxD) ||
		    		//informal, local, less than 2 miles
		    		(lotType > 1 && localModes.contains(mode) && dist < informalLocalMaxD) ){
			    		float[] vals = new float[4];
			    		vals[0] = j;
			    		vals[1] = time;
			    		vals[2] = dist;
			    		vals[3] = mode;
			    		bestTapsMap.get(currTAZ).add(vals);
			    		addedTaps.add(j);
		    	}
		    }
		    for(Integer m : modeMap.keySet()){
		    	float[] closestTAPvals = modeMap.get(m);
		    	int tap = (int) closestTAPvals[0];
		    	if(!addedTaps.contains(tap)){  //Put best taps by mode into bestTapsMap if they are not already there
		    		bestTapsMap.get(currTAZ).add(closestTAPvals);
		    		addedTaps.add(tap);
		    	}
		    }
		}
	}
	
	/*The file has five columns: TAZ, TAP, travel time (min) *100, distance (mile) *100 and mode.
	 */
	public void writeResults(String filename) throws IOException{
		BufferedWriter writer = null;
		try{
			writer = new BufferedWriter(new FileWriter(new File(filename)));
		            
			for(int i = 1; i < tazs.length; i++) {
			    int currTAZ = tazs[i];
			    
			    //Skip externals
			    if(externalTAZs.contains(currTAZ))
			    	continue;
			    
			    if(bestTapsMap.get(currTAZ).size() > 0){
				    //NEW CSV FORMAT (tabular: TAZ, TAP, TIME, DIST, MODE)
				    for(int k = 0; k < bestTapsMap.get(currTAZ).size(); k++){
				    	writer.write(	(int)(currTAZ) + "," + 
				    					(int)(bestTapsMap.get(currTAZ).get(k)[0]) + "," + 
				    					(double)(bestTapsMap.get(currTAZ).get(k)[1]) + "," + 
				    					(double)(bestTapsMap.get(currTAZ).get(k)[2]) + "," +
				    					(int)(bestTapsMap.get(currTAZ).get(k)[3]) + "\n");
				    }
			    }
			}
		}finally{
            if (writer != null) writer.close();
        }
	}

	
    /**
     * @param args
     */
	 public static void main(String... args) throws Exception
	    {
		 HashMap<String, String> pMap;
		 String propertiesFile = null;
		 
		 logger.info("Generating access**.prp files");
		 if (args.length == 0)
		 {
			 logger.error(String.format("no properties file base name (without .properties extension) was specified as an argument."));
			 return;
		 } else propertiesFile = args[0];
	        
	        
		 Properties properties = new Properties();
		 properties.load(new FileInputStream("conf/sandag_abm.properties"));

		 List<String> definedTables = new ArrayList<String>();
		 for (String table : properties.getProperty("Report.tables").trim().split(","))
			 definedTables.add(table.trim().toLowerCase());

		 String path = ClassLoader.getSystemResource("").getPath();
		 path = path.substring(1, path.length() - 2);
		 String appPath = path.substring(0, path.lastIndexOf("/"));

		 for (Object key : properties.keySet())
		 {
			 String value = (String) properties.get(key);
			 properties.setProperty((String) key, value.replace(PROJECT_PATH_PROPERTY_TOKEN, appPath));
		 }

		 OMXMatrixDao mtxDao = new OMXMatrixDao(properties);
		 pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
		 SandagMGRAtoPNR accessWriter = new SandagMGRAtoPNR(properties, mtxDao, appPath, pMap);   
		 accessWriter.nearestTAPs();
		 accessWriter.writeResults(properties.getProperty("taz.driveaccess.taps.file"));
    }
}
			    
