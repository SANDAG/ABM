package org.sandag.abm.dta.postprocessing;

import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import org.sandag.abm.ctramp.Util;

public class todDisaggregationModel {

	private static final String PROPERTIES_RANDOMSEED             = "dta.postprocessing.RandomSeed";
	
    private HashMap<String,String> rbMap;
	private MersenneTwister random;
	private long randomSeed;
	
		
	public PrintWriter tripWriter;


    private transient Logger           logger                         = Logger.getLogger("postprocessModel");

	
	/**
	 * Default constructor.
	 */
	public todDisaggregationModel(HashMap<String,String> rbMap){
		this.rbMap = rbMap;
		randomSeed = Util.getIntegerValueFromPropertyMap(rbMap, PROPERTIES_RANDOMSEED);
		random = new MersenneTwister();
		random.setSeed(randomSeed);
 	}
			
	/**
	 * Read the probability by tod data file, return an array of probabilities.
	 */
	public double[] getTODProbabilities(TableDataSet TODData, int numPeriods, String marketSegment){
	 	
		// read the tod factors file
		double [] probabilities;
		probabilities = new double [numPeriods];
					
		//fill in probabilities array
		for(int i = 0;i<numPeriods;++i){
			if (marketSegment==null){
				double prob = (double) TODData.getValueAt(i+1, "Factors");
				probabilities[i] = prob;
			}else{
				double prob = (double) TODData.getValueAt(i+1, marketSegment);
				probabilities[i] = prob;
			}
			
								
		}
		//return array of probabilities
		return probabilities;
	}
	
	/**
	 * Read the probability by tod data file, return an array of aggregate TOD values.
	 */
	public int[] getTODMap(TableDataSet TODData, int numPeriods){
	 	
		// read the tod factors file
		int [] TODMap;
		TODMap = new int [numPeriods];
					
		//fill in array of more aggregate time period values
		for(int i = 0;i<numPeriods;++i){
			int tod = (int) TODData.getValueAt(i+1, "TOD");
			TODMap[i] = tod;					
		}
		//return array of aggregate time period values
		return TODMap;
	}
	
	/**
	 * Return a disaggregate time period from an aggregate time period
	 */
	public int calculateDisaggregateTOD(int period, int[] TODMap, double[] probabilities, boolean debug){
		int dtaPeriod = -1;
		double probability = 0;
		double cumProbability = 0;
		double rn = random.nextDouble();
		int startLoc = 0;
		
		if (period>1)
			startLoc = (period-1)*6 + 18;	
				
        // loop through the array of probabilities
		for (int i=startLoc;i<probabilities.length;++i){
			
			// check if the time period for that probability is in the period to be disaggregated
			if (TODMap[i]!=period)
				continue;
			probability = probabilities[i];
			cumProbability += probability;
			if(debug){
				logger.info(period
						+"     "+(i+1)
						+"     "+probability
						+"     "+cumProbability
						+"     "+rn);
			}
			// choose the dta period if the cumulative probability has exceeded the random number
			//jef added equal to
			if (rn<=cumProbability){
				dtaPeriod = i+1;
				break;
			}
		}
		return dtaPeriod;
	}
    
}

