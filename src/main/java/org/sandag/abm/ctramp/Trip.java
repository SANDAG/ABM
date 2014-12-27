package org.sandag.abm.ctramp;

//fix todos
public class Trip {

    int     id;
    int     origMgra;
    int     destMgra;
    int     park;
    int     mode;
    byte     period;
    int     boardTap;
    int     alightTap;
    boolean inbound;

    boolean originInAlightingTapShed;
    boolean destinationInAlightingTapShed;
    
    String  origPurpose;
    String  destPurpose;

    Tour    parentTour;

	private boolean firstTrip;
	private boolean lastTrip;
	private boolean originIsTourDestination;
	private boolean destinationIsTourDestination;
	
	public final String WORK = "Work";
	public final String HOME = "Home";

	
   
    /**
     * Default constructor; nothing initialized.
     */
    public Trip(){
    	
    }
    
    /**
     * Create a trip from a tour leg (no stops).
     * @param tour  The tour.
     * @param outbound  Outbound direction
     */
    public Trip(Tour tour, boolean outbound){
    	
     	initializeFromTour(tour, outbound);
    }
    
    /**
     * Initilize from the tour.
     * @param tour  The tour.
     * @param outbound  Outbound direction.
     */
    public void initializeFromTour(Tour tour, boolean outbound){
       	//Note: mode is unknown
    	if(outbound){
    		this.origMgra = tour.getTourOrigMgra();
    		this.destMgra = tour.getTourDestMgra();
    		this.origPurpose = tour.getTourCategory().equalsIgnoreCase(
                    ModelStructure.AT_WORK_CATEGORY) ? WORK : HOME; 
    		this.destPurpose = tour.getTourPurpose();
    		this.period = (byte) tour.getTourDepartPeriod();
    		this.inbound = false;
    		this.firstTrip = true;
    		this.lastTrip = false;
    		this.originIsTourDestination = false;
    		this.destinationIsTourDestination=true;
     	}else{
    		this.origMgra = tour.getTourDestMgra();
    		this.destMgra = tour.getTourOrigMgra();
    		this.origPurpose = tour.getTourPurpose();
    		this.destPurpose = tour.getTourCategory().equalsIgnoreCase(
                    ModelStructure.AT_WORK_CATEGORY) ? WORK : HOME; 
    		this.period = (byte) tour.getTourArrivePeriod();
    		this.inbound = true;
    		this.firstTrip = false;
    		this.lastTrip = true;
    		this.originIsTourDestination = true;
    		this.destinationIsTourDestination=false;
     	}
	
    	
    }
    
    /**
     * Create a trip from a tour\stop.  Note:  trip mode 
     * is unknown.  Stop period is only known for first, last stop on tour.
     * 
     * @param tour  The tour.
     * @param stop  The stop
     */
    public Trip(Tour tour, Stop stop, boolean toStop){
    	
     	initializeFromStop(tour, stop, toStop);
    }
    
    /**
     * Initialize from stop attributes.  A trip will be created to the stop if toStop is true,
     * else a trip will be created from the stop.  Use after all stop locations are known, or 
     * else reset the stop origin and destination mgras accordingly after using.
     * 
     * @param tour
     * @param stop
     * @param toStop
     */
    public void initializeFromStop(Tour tour, Stop stop, boolean toStop){
    	
       	this.inbound = stop.isInboundStop();
       	this.destinationIsTourDestination=false;
       	this.originIsTourDestination=false;
    	
    	
    	//if trip to stop, destination is stop mgra; else origin is stop mgra
    	if(toStop){
    		this.destMgra=stop.getOrig();
    		//todo: Fix
//    		this.destPurpose = stop.getPurpose();
    	}else{
    		this.origMgra= stop.getDest();
    		//todo: Fix
//     		this.origPurpose = stop.getPurpose();
    	}
    	Stop[] stops;
    	
    	if(!inbound)
    		stops = tour.getOutboundStops();
    	else
    		stops = tour.getInboundStops();
    	
    	//if outbound, and trip is to stop
    	if(!inbound && toStop){ 
    		
    		//first trip on outbound journey, origin is tour origin
    		if(stop.getStopId()==0){
    			this.origMgra = tour.getTourOrigMgra();
    			this.origPurpose = tour.getTourCategory().equalsIgnoreCase(
                        ModelStructure.AT_WORK_CATEGORY) ? WORK : HOME; 
    			this.period = (byte) tour.getTourDepartPeriod();
    		}else{
    		//not first trip on outbound journey, origin is last stop
        		//todo: Fix
    			//     			this.origMgra = stops[stop.getStopId()-1].getMgra();   //last stop location
        		//todo: Fix
    			//     			this.origPurpose = stops[stop.getStopId()-1].getPurpose();   //last stop location
    			this.period =  (byte) stops[stop.getStopId()-1].getStopPeriod();  
    		}
   		}else if(!inbound && !toStop){   
   		//outbound and trip is from stop to either next stop or tour destination. 
   			
   			//last trip on outbound journey, destination is tour destination
   			if(stop.getStopId()==(stops.length-1)){
   				this.destMgra = tour.getTourDestMgra();
   				this.destPurpose = tour.getTourPurpose();
   				this.destinationIsTourDestination = true;
   			}else{
   			//not last trip on outbound journey, destination is next stop
   	    		//todo: Fix
   			//    				this.destMgra = stops[stop.getStopId()+1].getMgra();
   	    		//todo: Fix
   			//    				this.destPurpose = stops[stop.getStopId()+1].getPurpose();
   			}
   			
   			//the period for the trip is the origin for the trip
   			if(stop.getStopId()==0)
   				this.period = (byte) tour.getTourDepartPeriod();
   			else
   				this.period = (byte) stops[stop.getStopId()-1].getStopPeriod();  
   		
   		}else if (inbound && toStop){    
   		//inbound, trip is to stop from either tour destination or last stop.	
   			
   			//first inbound trip; origin is tour destination
   			if(stop.getStopId()==0){
   				this.origMgra = tour.getTourDestMgra();
   				this.origPurpose = tour.getTourPurpose();
   				this.originIsTourDestination=true;
   			}else{
   			// not first inbound trip; origin is last stop
   	    		//todo: Fix
   			//   				this.origMgra = stops[stop.getStopId()-1].getMgra();   //last stop location
   	    		//todo: Fix
   			//    				this.origPurpose = stops[stop.getStopId()-1].getPurpose(); 
   			}
   			
   			//the period for the trip is the destination for the trip
   			if(stop.getStopId()==stops.length-1)
   				this.period = (byte) tour.getTourArrivePeriod();
   			else
   				this.period =  (byte) stops[stop.getStopId()+1].getStopPeriod(); 
    	}else{    
        //inbound, trip is from stop to either next stop or tour origin.	
  			
    		//last trip, destination is back to tour origin
    		if(stop.getStopId()==(stops.length-1)){
   				this.destMgra = tour.getTourOrigMgra();
   				this.destPurpose = tour.getTourCategory().equalsIgnoreCase(
   	                    ModelStructure.AT_WORK_CATEGORY) ? WORK : HOME; 
   				this.period = (byte) tour.getTourArrivePeriod();
  			}else{
  			//not last trip, destination is next stop		
  	    		//todo: Fix
  			//   				this.destMgra = stops[stop.getStopId()+1].getMgra();
  	    		//todo: Fix
  			//   				this.destPurpose = stops[stop.getStopId()+1].getPurpose();
   				this.period =  (byte) stops[stop.getStopId()+1].getStopPeriod(); 
  			}
    	}
	
    	//code period for first trip on tour
    	if(toStop && !inbound && stop.getStopId()==0){
    		this.firstTrip = true;
    		this.lastTrip = false;
    		this.period = (byte) tour.getTourDepartPeriod();
    	}
    	//code period for last trip on tour
    	if(!toStop && inbound && stop.getStopId()==(stops.length-1)){
    		this.firstTrip = false;
    		this.lastTrip = true;
    		this.period = (byte) tour.getTourArrivePeriod();
    	}
    
    }

    
}
