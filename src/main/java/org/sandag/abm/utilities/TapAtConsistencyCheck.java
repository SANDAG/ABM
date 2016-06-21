package org.sandag.abm.utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;

/**
 * The TapAtConsistencyCheck program implements consistency checkng between TAPs in AT and Transit networks.
 * There are three types of inconsistencies:
 * 1) A TAP in AT network is not in Tranist network
 * 2) A TAP in Transit network is not in AT network
 * 3) Positions of a TAP are different in AT and Transit networks.
 * 
 * @author wsu  
 * @mail Wu.Sun@sandag.org
 * @version 13.3.0
 * @since 2016-06-20
 *
 */
public class TapAtConsistencyCheck {
    private static Logger      logger                          = Logger.getLogger(TapAtConsistencyCheck.class);
    private HashMap <Integer,ArrayList<Float>> atMap;
    private HashMap <Integer,ArrayList<Double>> tranMap;   
	private float xThreshold;
	private float yThreshold;
	private String message;
    
	public TapAtConsistencyCheck(ResourceBundle aRb){
		xThreshold=Float.parseFloat(aRb.getString("AtTransitConsistency.xThreshold"));
		yThreshold=Float.parseFloat(aRb.getString("AtTransitConsistency.yThreshold"));
		readAtTaps();
		readTranTaps();
	}
	
	public void validate(){
		message=compareMap(atMap,tranMap);
	}

	
	private void readAtTaps(){
		atMap=new HashMap<Integer,ArrayList<Float>>();
	    Object [] atTapObjects;

	    try {
		    InputStream  inputStream  = new FileInputStream("T:\\projects\\sr13\\develop\\military_aztec\\input\\SANDAG_Bike_Node.dbf"); 
		    DBFReader atTapReader = new DBFReader( inputStream); 
			while( (atTapObjects = atTapReader.nextRecord()) != null) {
	            double tap_at = (double)atTapObjects[3];
	            if(tap_at>0){
	            	ArrayList<Float> xy=new ArrayList<Float>();
	            	xy.add((float)atTapObjects[4]);
	            	xy.add((float)atTapObjects[5]);
		            atMap.put((int)tap_at, xy);
	            }
	      }
		  inputStream.close();
		}catch( DBFException e) {
			System.out.println( e.getMessage());
			logger.fatal(e.getMessage());
			System.exit(-1);
	    }catch( IOException e) {
	    	System.out.println( e.getMessage());
			logger.fatal(e.getMessage());
			System.exit(-1);
	    }
	}
	
	private void readTranTaps(){
		tranMap=new HashMap<Integer,ArrayList<Double>>();
	    Object [] tranTapObjects;

	    try {
		    InputStream  inputStream  = new FileInputStream("T:\\projects\\sr13\\develop\\military_aztec\\input\\tapcov.dbf"); 
		    DBFReader tranTapReader = new DBFReader( inputStream); 
			while( (tranTapObjects = tranTapReader.nextRecord()) != null) {
	            double tap_tran = (double)tranTapObjects[16];	            
            	ArrayList<Double> xy=new ArrayList<Double>();
            	xy.add((double)tranTapObjects[7]);
            	xy.add((double)tranTapObjects[8]);
	            tranMap.put((int)tap_tran, xy);
	        }
		    inputStream.close();
		}catch( DBFException e) {
			System.out.println( e.getMessage());
			logger.fatal(e.getMessage());
			System.exit(-1);
	    }catch( IOException e) {
	    	System.out.println( e.getMessage());
	    	logger.fatal(e.getMessage());
	    	System.exit(-1);
	    }
	}
	 
public String compareMap(HashMap<Integer, ArrayList<Float>> map1, HashMap<Integer, ArrayList<Double>> map2) {

	String message=null;
	
    if (map1.size()==0){
    	message=new ErrorLogging().getAtError("AT4");   	
    }
    	
    if (map2.size()==0){
    	message=new ErrorLogging().getAtError("AT5");   	
    }

    for (Integer ch1 : map1.keySet()) {
    	Float x1=map1.get(ch1).get(0);
    	Float y1=map1.get(ch1).get(1);
    	
    	if(map2.get(ch1)==null||map2.get(ch1)==null){
    		message=new ErrorLogging().getAtError("AT1")+"(in SANDAG_Bike_Node.dbf "+"TAP="+ch1+" x_at="+x1+" y_at="+y1+")";
    		System.out.println(message);
    		break;    		
    	}else{
	    	Double x2=map2.get(ch1).get(0);
	    	Double y2=map2.get(ch1).get(1);
	    	System.out.println("TAP="+ch1+" x_at="+x1+" y_at="+y1+" x_tran="+x2+" y_tran="+y2);
	    	if((Math.abs(x1-x2)>xThreshold)&&(Math.abs(y1-y2)>yThreshold)){
	    		message=new ErrorLogging().getAtError("AT3")+"("+"TAP="+ch1+" x_at="+x1+" y_at="+y1+" x_tran="+x2+" y_tran="+y2+")";
	    		System.out.println(message);
	    		break;
	    	}else{
	    		message="OK";
	    	}
    	}
    	
    }
    
    for (Integer ch2 : map2.keySet()) {

    	Double x2=map2.get(ch2).get(0);
    	Double y2=map2.get(ch2).get(1);
    	
    	if(map1.get(ch2)==null||map1.get(ch2)==null){
    		message=new ErrorLogging().getAtError("AT2")+"(in tapcov.dbf "+"TAP="+ch2+" x_tran="+x2+" y_tran="+y2+")";
    		System.out.println(message);
    		break;    		
    	}else{
        	Float x1=map1.get(ch2).get(0);
        	Float y1=map1.get(ch2).get(1);
	    	System.out.println("TAP="+ch2+" x_at="+x1+" y_at="+y1+" x_tran="+x2+" y_tran="+y2);
	    	if((Math.abs(x1-x2)>xThreshold)&&(Math.abs(y1-y2)>yThreshold)){
	    		message=new ErrorLogging().getAtError("AT3")+"("+"TAP="+ch2+" x_at="+x1+" y_at="+y1+" x_tran="+x2+" y_tran="+y2+")";
	    		System.out.println(message);
	    		break;
	    	}else{
	    		message="OK";
	    	}
    	}
    }
    
    return message;
} 
	
    public static void main(String[] args)
    {
        ResourceBundle rb = null;
        logger.info("Checking AT and Transit Network Consistency...");

        if (args.length == 0)
        {
            logger.error(String.format("no properties file base name (without .properties extension) was specified as an argument."));
            System.exit(-1);
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
            TapAtConsistencyCheck mainObject = new TapAtConsistencyCheck(rb);
            mainObject.validate();
            if(!mainObject.message.equalsIgnoreCase("OK")){
            	logger.fatal(mainObject.message);
            	System.exit(-1);
            }
        }
    }
}
