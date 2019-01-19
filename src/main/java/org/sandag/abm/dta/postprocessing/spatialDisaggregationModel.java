package org.sandag.abm.dta.postprocessing;

import java.io.PrintWriter;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.pb.common.datafile.TableDataSet;
import com.pb.common.math.MersenneTwister;
import org.sandag.abm.ctramp.Util;

public class spatialDisaggregationModel {

	private static final String PROPERTIES_RANDOMSEED             = "dta.postprocessing.RandomSeed";
	
    private HashMap<String,String> rbMap;
	private MersenneTwister random;
	private long randomSeed;
			
	public PrintWriter tripWriter;


    private transient Logger           logger                         = Logger.getLogger("postprocessModel");

	
	/**
	 * Default constructor.
	 */
	public spatialDisaggregationModel(HashMap<String,String> rbMap){
		
		this.rbMap = rbMap;
		randomSeed = Util.getIntegerValueFromPropertyMap(rbMap, PROPERTIES_RANDOMSEED);
		random = new MersenneTwister();
		random.setSeed(randomSeed);
 	}
			
	/**
	 * Read the probability by spatial data file, return an array of probabilities.
	 */
	public double[] getSpatialProbabilities(TableDataSet SpatialData, int numRecords, String inputField, String marketSegment){
	 	
		// read the spatial factors file
		double [] probabilities;
		probabilities = new double [numRecords];
		
		String fieldName = null;
		if (marketSegment==null){
			fieldName = inputField;
		}else{
			fieldName = marketSegment+inputField;
		}
		//fill in probabilities array
		for(int i = 0;i<numRecords;++i){
			double prob = (double) SpatialData.getValueAt(i+1,fieldName);
			probabilities[i] = prob;			
		}
		//return array of probabilities
		return probabilities;
	}
	
	/**
	 * Create an array of spatial values based on input data file and field name specified
	 */
	public int[] getSpatialMap(TableDataSet SpatialData, int numRecords, String fieldName){
	 	
		// read the spatial factors file
		int [] spatialMap;
		spatialMap = new int [numRecords];
					
		//fill in array of values based on the field name provided
		for(int i = 0;i<numRecords;++i){
			int fieldVal = (int) SpatialData.getValueAt(i+1, fieldName);
			spatialMap[i] = fieldVal;					
		}
		//return array of spatial values
		return spatialMap;
	}
	
	/**
	 * Select an mgra to assign to based on the trip TAZ
	 */
	public int selectMGRAfromTAZ(int Taz, int mgraMap[], int tazMap[], double probabilities[], boolean debug){
		int MGRA = -1;
		double probability = 0;
		double cumProbability = 0;
		double rn = random.nextDouble();
		
        // loop through the array of probabilities
		for (int i=0;i<probabilities.length;++i){
			
			// check if the taz for that probability is in the taz to be disaggregated
			if (tazMap[i]!=Taz)
				continue;
			probability = probabilities[i];
			cumProbability += probability;
			if(debug){
				logger.info(tazMap[i]
						+"     "+mgraMap[i]
						+"     "+probability
						+"     "+cumProbability
						+"     "+rn);
			}
			// choose the mgra if the cumulative probability has exceeded the random number
			if (rn<cumProbability){
				MGRA = mgraMap[i];
				break;
			}
		}
		return MGRA;
	}

	/**
	 * Select a node to assign to based on the trip mgra
	 */
	public int selectNodeFromMGRA(int mgra, int nodeMap[], int mgraMap[], double probabilities[], boolean debug){
		int Node = -1;
		double probability = 0;
		double cumProbability = 0;
		double rn = random.nextDouble();
		
        // loop through the array of probabilities
		for (int i=0;i<probabilities.length;++i){
			
			// check if the mgra for that probability is in the mgra to be disaggregated
			if (mgraMap[i]!=mgra)
				continue;
			probability = probabilities[i];
			cumProbability += probability;
			if(debug){
				logger.info(mgraMap[i]
						+"     "+nodeMap[i]
						+"     "+probability
						+"     "+cumProbability
						+"     "+rn);
			}
			// choose the node if the cumulative probability has exceeded the random number
			if (rn<cumProbability){
				Node = nodeMap[i];
				break;
			}
		}
		return Node;
	}
}