package org.sandag.abm.reporting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.accessibilities.AutoTazSkimsCalculator;
import org.sandag.abm.accessibilities.BestTransitPathCalculator;
import org.sandag.abm.accessibilities.DriveTransitWalkSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitDriveSkimsCalculator;
import org.sandag.abm.accessibilities.WalkTransitWalkSkimsCalculator;
import org.sandag.abm.ctramp.CtrampApplication;
import org.sandag.abm.ctramp.MatrixDataServer;
import org.sandag.abm.ctramp.MatrixDataServerRmi;
import org.sandag.abm.ctramp.ModelStructure;
import org.sandag.abm.modechoice.MgraDataManager;
import org.sandag.abm.modechoice.Modes;
import org.sandag.abm.modechoice.TazDataManager;
import org.sandag.abm.modechoice.TransitDriveAccessDMU;
import org.sandag.abm.modechoice.TransitWalkAccessDMU;

import com.pb.common.calculator.MatrixDataManager;
import com.pb.common.calculator.MatrixDataServerIf;
import com.pb.common.util.ResourceUtil;

public class TransitTimeReporter {
	private static final Logger logger = Logger.getLogger(TransitTimeReporter.class);
    private BestTransitPathCalculator         bestPathCalculator;
    protected WalkTransitWalkSkimsCalculator  wtw;
    protected WalkTransitDriveSkimsCalculator wtd;
    protected DriveTransitWalkSkimsCalculator dtw;
    public static final int         MATRIX_DATA_SERVER_PORT        = 1171;
    public static final int         MATRIX_DATA_SERVER_PORT_OFFSET = 0;
    private MatrixDataServerRmi     ms;
    private MgraDataManager mgraManager;
    private TazDataManager tazManager;
    AutoTazSkimsCalculator tazDistanceCalculator;
    
    //skim locations in WalkTransitWalkSkims UEC
    private static final int WLK_WALKACCESSTIME = 0;
    private static final int WLK_WALKEGRESSTIME = 1;
    private static final int WLK_AUXWALKTIME	= 2;
    private static final int WLK_LOCALBUSIVT	= 3;
    private static final int WLK_EXPRESSBUSIVT	= 4;
    private static final int WLK_BRTIVT	        = 5;
    private static final int WLK_LRTIVT	        = 6;
    private static final int WLK_CRIVT	        = 7;
    private static final int WLK_FIRSTWAITTIME  = 8;	
    private static final int WLK_TRWAITTIME	    = 9;
    private static final int WLK_FARE	        = 10;
    private static final int WLK_TOTALIVT	    = 11;
    private static final int WLK_XFERS          = 12;

    //skim locations in DriveTransitWalkSkims UEC
    private static final int DRV_DRIVEACCESSTIME = 0;
    private static final int DRV_WALKEGRESSTIME = 1;
    private static final int DRV_AUXWALKTIME	= 2;
    private static final int DRV_LOCALBUSIVT	= 3;
    private static final int DRV_EXPRESSBUSIVT	= 4;
    private static final int DRV_BRTIVT	        = 5;
    private static final int DRV_LRTIVT	        = 6;
    private static final int DRV_CRIVT	        = 7;
    private static final int DRV_FIRSTWAITTIME  = 8;	
    private static final int DRV_TRWAITTIME	    = 9;
    private static final int DRV_FARE	        = 10;
    private static final int DRV_TOTALIVT	    = 11;
    private static final int DRV_XFERS          = 12;
    
    String period; //should be "AM" or "MD"
    float threshold; //tested at 30 minutes
    boolean inbound = false;
    
    private PrintWriter walkAccessWriter;
    private PrintWriter driveAccessWriter;
    private String outWalkFile;
    private String outDriveFile;
     
     public TransitTimeReporter(HashMap<String, String> propertyMap, float threshold, String period,String outWalkFileName,String outDriveFileName){
     	
     	startMatrixServer(propertyMap);
     	
     	this.threshold = threshold;
     	this.period = period;
     	this.outWalkFile = outWalkFileName;
     	this.outDriveFile = outDriveFileName;
     	
     	initialize(propertyMap);
     }
	 
     /**
      * Initialize best path builders.
      * 
      * @param propertyMap A property map with relevant properties.
      */
 	public void initialize(HashMap<String, String> propertyMap){
 		
 		logger.info("Initializing Transit Time Reporter");
 	    mgraManager = MgraDataManager.getInstance(propertyMap);
 	    tazManager = TazDataManager.getInstance(propertyMap);

         bestPathCalculator = new BestTransitPathCalculator(propertyMap);
         
         tazDistanceCalculator = new AutoTazSkimsCalculator(propertyMap);
         tazDistanceCalculator.computeTazDistanceArrays();

         wtw = new WalkTransitWalkSkimsCalculator(propertyMap);
         wtw.setup(propertyMap, logger, bestPathCalculator);
         wtd = new WalkTransitDriveSkimsCalculator(propertyMap);
         wtd.setup(propertyMap, logger, bestPathCalculator);
         dtw = new DriveTransitWalkSkimsCalculator(propertyMap);
         dtw.setup(propertyMap, logger, bestPathCalculator);
   
         walkAccessWriter = createOutputFile(outWalkFile);
         driveAccessWriter = createOutputFile(outDriveFile);

 	}
	
 	/**
	 * Create the output file.
	 */
	private PrintWriter createOutputFile(String fileName){
        
		logger.info("Creating file " + fileName);
		PrintWriter writer;
		try
        {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
        } catch (IOException e)
        {
            logger.fatal("Could not open file " + fileName + " for writing\n");
            throw new RuntimeException();
        }
		
		return writer;
       
	}

	
	/**
	 * Iterate through MAZs and write results
	 */
	private void run(){
		
		TransitWalkAccessDMU walkDmu =  new TransitWalkAccessDMU();
    	TransitDriveAccessDMU driveDmu  = new TransitDriveAccessDMU();
		double boardAccessTime;
		double alightAccessTime;
		
		int skimPeriod = -1;
		
		if(period.compareTo("EA")==0){
			skimPeriod=ModelStructure.EA_SKIM_PERIOD_INDEX;
			inbound = false;
		}else if(period.compareTo("AM")==0){
			skimPeriod=ModelStructure.AM_SKIM_PERIOD_INDEX;
			inbound = false;
		}else if(period.compareTo("MD")==0){
			skimPeriod=ModelStructure.MD_SKIM_PERIOD_INDEX;
			inbound = false;
		}else if(period.compareTo("PM")==0){
			skimPeriod=ModelStructure.PM_SKIM_PERIOD_INDEX;
			inbound = true;
		}else if(period.compareTo("EV")==0){
			skimPeriod=ModelStructure.EV_SKIM_PERIOD_INDEX;
			inbound = true;
		}else{
			logger.fatal("Skim period "+period+" not recognized");
			throw new RuntimeException();
		}
		
		//iterate through mazs and calculate time
		ArrayList<Integer> mazs = mgraManager.getMgras();
		
		//origins
		for(int originMaz: mazs ){
		
			if((originMaz<=100) || ((originMaz % 100) == 0))
				logger.info("Processing origin mgra "+originMaz);
			
			int originTaz = mgraManager.getTaz(originMaz);
			
			//for saving results
			String outWalkString = null;
			String outDriveString = null;

			//destinations
			for(int destinationMaz:mazs){
			
				int destinationTaz = mgraManager.getTaz(destinationMaz);
		
				float odDistance  = (float) tazDistanceCalculator.getTazToTazDistance(skimPeriod, originTaz, destinationTaz);
			
				//walk calculations
		    	double[][] bestWalkTaps = bestPathCalculator.getBestTapPairs(walkDmu, driveDmu, bestPathCalculator.WTW, originMaz, destinationMaz, skimPeriod, false, logger, odDistance);
				double[] bestWalkUtilities = bestPathCalculator.getBestUtilities();
			
			    //only look at best utility path; continue if MGRA isn't available by walk.
				if(bestWalkUtilities[0]>-500){
	           
					//Best walk TAP pair
					int boardTap = (int) bestWalkTaps[0][0];
					int alightTap = (int) bestWalkTaps[0][1];
					int set = (int) bestWalkTaps[0][2];
	        	
					// get walk skims
                    boardAccessTime = mgraManager.getWalkTimeFromMgraToTap(originMaz,boardTap);
                    alightAccessTime = mgraManager.getWalkTimeFromMgraToTap(destinationMaz,alightTap);
					double[] walkSkims = wtw.getWalkTransitWalkSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, skimPeriod, false); 

					//calculate total time
					double totalTime =  walkSkims[WLK_WALKACCESSTIME]   
					                  + walkSkims[WLK_WALKEGRESSTIME]
					                  + walkSkims[WLK_AUXWALKTIME]
				                      + walkSkims[WLK_FIRSTWAITTIME]	
				                      + walkSkims[WLK_TRWAITTIME]
				                      + walkSkims[WLK_TOTALIVT];
				    
					//if total time is less than or equal to the threshold, add the destination MAZ to the output string
					if(totalTime<=threshold){
						if(outWalkString==null)
							outWalkString = String.valueOf(originMaz);
						
						outWalkString += ","+destinationMaz; 
				
					}
				}
				
				//drive calculations
				double[][] bestDriveTaps = null;
				if(inbound==false){
					bestDriveTaps = bestPathCalculator.getBestTapPairs(walkDmu, driveDmu, bestPathCalculator.DTW, originMaz, destinationMaz, skimPeriod, false, logger, odDistance);
				}else{
					bestDriveTaps = bestPathCalculator.getBestTapPairs(walkDmu, driveDmu, bestPathCalculator.WTD, originMaz, destinationMaz, skimPeriod, false, logger, odDistance);
				}
				double[] bestDriveUtilities = bestPathCalculator.getBestUtilities();
			
			    //only look at best utility path; continue if MGRA isn't available by walk.
				if(bestDriveUtilities[0]>-500){
	           
					//best drive TAP pair
					int boardTap = (int) bestDriveTaps[0][0];
					int alightTap = (int) bestDriveTaps[0][1];
					int set = (int) bestDriveTaps[0][2];
	        	
					//skims for best drive pair
					double[] driveSkims = null;
					if(inbound==false){
						boardAccessTime = tazManager.getTimeToTapFromTaz(originTaz,boardTap,( Modes.AccessMode.PARK_N_RIDE ));
						alightAccessTime = mgraManager.getWalkTimeFromMgraToTap(destinationMaz,alightTap);
						driveSkims = dtw.getDriveTransitWalkSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, skimPeriod, false); 
					}else{
						boardAccessTime = mgraManager.getWalkTimeFromMgraToTap(originMaz,boardTap);
						alightAccessTime = tazManager.getTimeToTapFromTaz(destinationTaz,alightTap,( Modes.AccessMode.PARK_N_RIDE ));
						driveSkims = wtd.getWalkTransitDriveSkims(set, boardAccessTime, alightAccessTime, boardTap, alightTap, skimPeriod, false); 
						
					}
					//total drive-transit time
					double totalTime =  driveSkims[DRV_DRIVEACCESSTIME]   
					                  + driveSkims[DRV_WALKEGRESSTIME]
					                  + driveSkims[DRV_AUXWALKTIME]
				                      + driveSkims[DRV_FIRSTWAITTIME]	
				                      + driveSkims[DRV_TRWAITTIME]
				                      + driveSkims[DRV_TOTALIVT];
				    
					//if total time is less than or equal to the threshold, add the destination MAZ to the output string
					if(totalTime<=threshold){
						if(outDriveString==null)
							outDriveString = String.valueOf(originMaz);
						
						outDriveString += ","+destinationMaz; 
				
					}
				}
				
			} //end for destinations

			//write the walk access MAZs under 30 minutes - origin, dest1, dest2, dest3...etc
			if(outWalkString!=null){
				walkAccessWriter.print(outWalkString+"\n");
				walkAccessWriter.flush();
			}
			
			//write the drive access MAZs under 30 minutes - origin, dest1, dest2, dest3...etc
			if(outDriveString!=null){
				driveAccessWriter.print(outDriveString+"\n");
				driveAccessWriter.flush();
			}
			
		} //end for origins


	}
	
	
     private void startMatrixServer(HashMap<String, String> properties) {
	        String serverAddress = (String) properties.get("RunModel.MatrixServerAddress");
	        int serverPort = new Integer((String) properties.get("RunModel.MatrixServerPort"));
	        logger.info("connecting to matrix server " + serverAddress + ":" + serverPort);

	        try{

	            MatrixDataManager mdm = MatrixDataManager.getInstance();
	            MatrixDataServerIf ms = new MatrixDataServerRmi(serverAddress, serverPort, MatrixDataServer.MATRIX_DATA_SERVER_NAME);
	            ms.testRemote(Thread.currentThread().getName());
	            mdm.setMatrixDataServerObject(ms);

	        } catch (Exception e) {
	            logger.error("could not connect to matrix server", e);
	            throw new RuntimeException(e);

	        }

	    }

 	/**
 	 * Main run method
 	 * @param args
 	 */
 	public static void main(String[] args) {

         String propertiesFile = null;
         float threshold = 0;
         String period = null;
         String outWalkFileName = null;
         String outDriveFileName = null;
         
         HashMap<String, String> pMap;

         logger.info(String.format("Report MAZs within transit time threshold. Using CT-RAMP version ",
                 CtrampApplication.VERSION));
         
         if (args.length == 0)
         {
             logger.error(String
                     .format("no properties file base name (without .properties extension) was specified as an argument."));
             return;
         } else {
         	propertiesFile = args[0];

 	        for (int i = 1; i < args.length; ++i)
 	        {
 	             if (args[i].equalsIgnoreCase("-threshold"))
 	            {
 	                threshold = Float.valueOf(args[i + 1]);
 	            }
 	             
 	             if (args[i].equalsIgnoreCase("-period"))
 	            {
 	                period = args[i + 1];
 	            }

 	             if (args[i].equalsIgnoreCase("-outWalkFileName"))
 	            {
 	                outWalkFileName = args[i + 1];
 	            }

 	             if (args[i].equalsIgnoreCase("-outDriveFileName"))
 	            {
 	                outDriveFileName = args[i + 1];
 	            }

 	        }
         }
         
         pMap = ResourceUtil.getResourceBundleAsHashMap(propertiesFile);
         TransitTimeReporter transitTimeReporter = new TransitTimeReporter(pMap, threshold, period,outWalkFileName,outDriveFileName);

    
         transitTimeReporter.run();
 	}

}
