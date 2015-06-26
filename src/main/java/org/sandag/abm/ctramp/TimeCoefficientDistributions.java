package org.sandag.abm.ctramp;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.Set;

import org.apache.log4j.Logger;

import com.pb.common.math.MersenneTwister;
import com.pb.common.util.ResourceUtil;

import umontreal.iro.lecuyer.probdist.LognormalDist;

public class TimeCoefficientDistributions {

    private static Logger           logger   = Logger.getLogger(TimeCoefficientDistributions.class);
	protected LognormalDist timeDistributionWork; 
    protected LognormalDist timeDistributionNonWork;


    /**
     * Constructor; doesn't do anything (call @createTimeDistributions method next)
     */
    public TimeCoefficientDistributions(){
    	
    }
	
	/**
	 *  Reads the propertyMap and finds values for properties 
	 *    timeDistributionMean.work, and
	 *    timeDistributionStandardDistribution.work. 
	 *    Creates and stores umontreal.iro.lecuyer.probdist.LognormalDist 
	 *    workDistribution for work tours & trips. 
	 *  Reads the propertyMap and finds values for properties 
	 *    timeDistributionMean.nonwork,
	 *    timeDistributionStandardDistribution.nonwork. 
	 *    Creates and stores a umontreal.iro.lecuyer.probdist.LognormalDist 
	 *    nonworkDistribution for non-work tours & trips.
	 * @param propertyMap
	 */

	public void createTimeDistributions(HashMap<String, String> propertyMap){
		
		double meanWork = new Double(propertyMap.get("timeDistribution.mean.work" ));
		double sdWork = new Double(propertyMap.get("timeDistribution.standardDeviation.work" ));

		double locationWork = calculateLocation(meanWork, sdWork);
		double scaleWork = calculateScale(meanWork, sdWork);
		
        timeDistributionWork = new LognormalDist(locationWork, scaleWork); 

        double meanNonWork = new Double(propertyMap.get("timeDistribution.mean.nonWork" ));
        double sdNonWork = new Double(propertyMap.get("timeDistribution.standardDeviation.nonWork" ));

		double locationNonWork = calculateLocation(meanNonWork, sdNonWork);
		double scaleNonWork = calculateScale(meanNonWork, sdNonWork);

		timeDistributionNonWork = new LognormalDist(locationNonWork, scaleNonWork); 

	}
	
	/**
	 * Calculate the lognormal distribution location given 
	 * the mean and standard deviation of the distribution 
	 * according to the formula:
	 * 
	 *  location = ln(mean/sqrt(1 + variance/mean^2))
	 * 
	 * @param mean 
	 * @param standardDeviation
	 * @return Location variable (u)
	 */
	public double calculateLocation(double mean, double standardDeviation){
		
		double variance = standardDeviation * standardDeviation;
		double meanSquared = mean  * mean;
		double denom = Math.sqrt(1.0 + (variance/meanSquared));
		double location = mean/denom;
		if(location<=0){
			logger.error("Error: Trying to calculation location for mean "+mean
					+" and standard deviation "+standardDeviation);
			throw new RuntimeException();
		}
		
		return Math.log(location);
		
	}

	/**
	 * Calculate the lognormal distribution scale given 
	 * the mean and standard deviation of the distribution 
	 * according to the formula:
	 *  
	 *  scale = sqrt(ln(1 + variance/mean^2));
	 * 
	 * @param mean 
	 * @param standardDeviation
	 * @return Scale variable (sigma)
	 */
	public double calculateScale(double mean, double standardDeviation){
		
		double variance = standardDeviation * standardDeviation;
		double meanSquared = mean  * mean;
		return Math.sqrt(Math.log(1 + variance/meanSquared));
		
		
	}

	/**
	 * Sample from the work distribution and return the factor to apply to work
	 * travel time coefficient.
	 * @param rnum A unit-distributed random number.
	 * @return The sampled time factor for work tours & trips.
	 */
	public double sampleFromWorkDistribution(double rnum){
		
		return timeDistributionWork.inverseF(rnum);
	}
	
	/**
	 * Sample from the non-work distribution and return the factor to apply
	 * to the non non-work travel time coefficient.
	 * 
	 * @param rnum A unit-distributed random number.
	 * @return The sampled time factor for non-work tours and trips.
	 */
	public double sampleFromNonWorkDistribution(double rnum){
		
		return timeDistributionNonWork.inverseF(rnum);
	}
    
	/**
	 * Get the time distribution for work.
	 * 
	 * @return The lognormal distribution for work.
	 */
	public LognormalDist getTimeDistributionWork() {
		return timeDistributionWork;
	}

	/**
	 * Get the time distribution for non-work.
	 * 
	 * @return The lognormal distribution for non-work.
	 */
	public LognormalDist getTimeDistributionNonWork() {
		return timeDistributionNonWork;
	}


	/**
	 * This method reads the input person file, samples from the lognormal
	 * time distributions for work and for non-work tours and trips,  and 
	 * appends the two fields for each person on the person file, over-writing the
	 * input person file with the results. If the fields already exist, nothing is
	 * done. The fields added to the person file are:
	 *   
	 *    timeFactorWork
	 *    timeFactorNonWork
	 *    
	 * @param propertyMap A property map with the following properties:
	 *   Project.Directory: the path to directory to read the person file from.
	 *   PopulationSynthesizer.InputToCTRAMP.PersonFile: the input person file
	 *   timeDistribution.randomSeed: a random seed for sampling from the distributions for each person
	 */
	public void appendTimeDistributionsOnPersonFile(HashMap<String, String> propertyMap){
		
		logger.info("Appending time factors to person file");
		String directory = propertyMap.get("Project.Directory");
		String personFile = directory + propertyMap.get("PopulationSynthesizer.InputToCTRAMP.PersonFile");
		
		long seed = new Long(propertyMap.get("timeDistribution.randomSeed"));
		MersenneTwister random = new MersenneTwister(seed);
		
		Charset ENCODING = StandardCharsets.UTF_8;
		
		logger.info("");
		logger.info("Reading person file "+personFile);
	    Path path = Paths.get(personFile);
	    ArrayList<String> personData = new ArrayList<String>(5000000);
	    String header = null;
	    
	    
	    int persons = 0;
	    
	    
	    try (Scanner scanner =  new Scanner(path, ENCODING.name())){
	    	
	    	header = scanner.nextLine();
	      
			// does first row of person file contain field names for time factors?
	    	if(header.contains("timeFactorWork")){
	    	  
	    		logger.info("File "+personFile+ " contains time factor fields already");
	    		scanner.close();
	    		return;
	    	  
	    	}else{
	    		while (scanner.hasNextLine()){
	    			//add the row to the person data array list
	    			personData.add(scanner.nextLine());
	    		  	++persons;
	    		  	
	    		  	if(persons % 100000 == 0)
	    		  		logger.info("Reading person file line "+persons);
	    		}
	    	}      
	    	scanner.close();
	    }catch(Exception e){
	    	logger.fatal("Error while reading "+personFile);
	    	throw new RuntimeException();
		}
     
	    logger.info("Appending time factors to person file "+personFile);
	    header = header +",timeFactorWork,timeFactorNonWork";
	 
	    try (BufferedWriter writer = Files.newBufferedWriter(path, ENCODING)){
		 
	    	//write the header with the additional fields
	    	writer.write(header);
	    	writer.newLine();
		 
	    	//write each person line, sampling from the work and non-work distributions for each and
	    	//appending the results onto the initial data
	    	for(int person = 0; person<persons;++person){
			 
    		  	if(person % 100000 == 0)
    		  		logger.info("Writing person file line "+person);

	    		double rnum = random.nextDouble();
	    		double workFactor = sampleFromWorkDistribution(rnum);
	    		double nonWorkFactor = sampleFromNonWorkDistribution(rnum);
			 
	    		String personLine = personData.get(person);
	    		personLine = personLine +","+String.format("%4.2f,%4.2f",workFactor,nonWorkFactor);
	    		writer.write(personLine);
	    		writer.newLine();
	    	}
	    	writer.close();
	    }catch(Exception e){
	    	logger.fatal("Error while writing "+personFile);
	    	throw new RuntimeException();
	    }
	 
	}
	
	/**
	 * Main method creates class, opens person file and appends time factors to file
	 * if they do not already exist.
	 * 
	 * @param args First argument is property map.
	 */
	public static void main(String args[]){
		
        ResourceBundle rb = null;
        HashMap<String, String> pMap;

        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        } else
        {
            rb = ResourceBundle.getBundle(args[0]);
            pMap = ResourceUtil.getResourceBundleAsHashMap(args[0]);
        }
        
        TimeCoefficientDistributions timeDistributions = new TimeCoefficientDistributions();
        timeDistributions.createTimeDistributions(pMap);
        timeDistributions.appendTimeDistributionsOnPersonFile(pMap);
		
        logger.info("All done!");
	}

}
