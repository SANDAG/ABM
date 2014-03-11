package org.sandag.abm.active.sandag;

import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import org.sandag.abm.ctramp.Stop;
import org.sandag.abm.ctramp.Household;
import java.util.*;
import java.io.*;

public class BikeAssignmentTripReader
{
    private String indivTripFileName, jointTripFileName, indivTourFileName, jointTourFileName, personFileName, hhFileName;
    private static final int DEFAULT_MANDATORY_PURPOSE_INDEX = 1;
    private static final int DEFAULT_NONMANDATORY_PURPOSE_INDEX = 4;
    private static final int BIKE_MODE_INDEX = 10;
    
    private static String PROPERTIES_HOUSEHOLD_FILENAME = "PopulationSynthesizer.InputToCTRAMP.HouseholdFile";
    private static String PROPERTIES_PERSON_FILENAME = "PopulationSynthesizer.InputToCTRAMP.PersonFile";
    private static String PROPERTIES_INDIV_TOUR_FILENAME = "Results.IndivTourDataFile";
    private static String PROPERTIES_JOINT_TOUR_FILENAME = "Results.JointTourDataFile";
    private static String PROPERTIES_INDIV_TRIP_FILENAME = "Results.IndivTripDataFile";
    private static String PROPERTIES_JOINT_TRIP_FILENAME = "Results.JointTripDataFile";
    
    public BikeAssignmentTripReader(Map<String,String> propertyMap)
    {
        this.indivTripFileName = propertyMap.get(PROPERTIES_INDIV_TRIP_FILENAME);
        this.jointTripFileName = propertyMap.get(PROPERTIES_JOINT_TRIP_FILENAME);
        this.indivTourFileName = propertyMap.get(PROPERTIES_INDIV_TOUR_FILENAME);
        this.jointTourFileName = propertyMap.get(PROPERTIES_JOINT_TOUR_FILENAME);
        this.personFileName = propertyMap.get(PROPERTIES_PERSON_FILENAME);
        this.hhFileName = propertyMap.get(PROPERTIES_HOUSEHOLD_FILENAME);      
    }
    
    public List<Stop> createTripList() {
        
        SandagModelStructure modelStructure = new SandagModelStructure();
        
        Map<Integer,Tour[]> indivTourMap = new HashMap<>();
        Map<Integer,List<Tour>> jointTourMap = new HashMap<>();
        Map<Integer,Household> hhMap =  new HashMap<>();
        List<Stop> stops = new ArrayList<>();
        
        try{
        
            String line;
            BufferedReader reader = new BufferedReader(new FileReader(hhFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                Household h = new Household(modelStructure);
                int hhSize = Integer.parseInt(row[8]);
                h.setHhSize(hhSize);
                int hhId = Integer.parseInt(row[0]);
                hhMap.put(hhId, h);
            }
            reader.close();
        
            reader = new BufferedReader(new FileReader(personFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                int hhId = Integer.parseInt(row[0]);
                int perId = Integer.parseInt(row[1]);
                int perNo = Integer.parseInt(row[3]);
                Household h = hhMap.get(hhId);
                Person p = h.getPerson(perNo);
                int gender = Integer.parseInt(row[5]);
                p.setPersGender(gender);
                p.setPersId(perId);
            }
            reader.close();
            
            reader = new BufferedReader(new FileReader(indivTourFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                int hhId = Integer.parseInt(row[0]);
                int perId = Integer.parseInt(row[1]);
                int perNo = Integer.parseInt(row[2]);
                int tourId = Integer.parseInt(row[4]);
                String tourCategory = row[5];
                int tourPurpose = DEFAULT_MANDATORY_PURPOSE_INDEX;
                if (tourCategory != "MANDATORY") {tourPurpose = DEFAULT_NONMANDATORY_PURPOSE_INDEX;}
                Tour t = new Tour(hhMap.get(hhId).getPerson(perNo),tourId,tourPurpose);
                if ( ! indivTourMap.containsKey(perId) ) {indivTourMap.put(perId, new Tour[20] );}
                indivTourMap.get(perId)[tourId] = t;
            }
            reader.close();
            
            reader = new BufferedReader(new FileReader(jointTourFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                int hhId = Integer.parseInt(row[0]);
                int tourId = Integer.parseInt(row[1]);
                String[] tourParty = row[5].split(" ");
                Household h = hhMap.get(hhId);
                int[] participantArray = new int[tourParty.length];
                for (int i=0; i<participantArray.length; i++) {
                    participantArray[i] = Integer.parseInt(tourParty[i]);
                }
                Tour t =  new Tour(h, "", modelStructure.JOINT_NON_MANDATORY_CATEGORY, DEFAULT_NONMANDATORY_PURPOSE_INDEX);
                t.setPersonObject(h.getPerson(participantArray[0]));
                t.setPersonNumArray(participantArray);
                t.setTourId(tourId);
                if ( ! jointTourMap.containsKey(hhId) ) {jointTourMap.put(hhId, new ArrayList<Tour>() );}
                jointTourMap.get(hhId).add(t);
            }
            reader.close();
            
            reader = new BufferedReader(new FileReader(indivTripFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                int tripMode = Integer.parseInt(row[13]);
                if ( tripMode == BIKE_MODE_INDEX ) { 
                    int perId = Integer.parseInt(row[1]);
                    int tourId = Integer.parseInt(row[3]);
                    boolean inbound = Boolean.parseBoolean(row[5]);
                    Tour t = indivTourMap.get(perId)[tourId];
                    Stop s = new Stop(t,"","",0,inbound,0);
                    int stopPeriod = Integer.parseInt(row[12]);
                    s.setStopPeriod(stopPeriod);
                    int oMgra = Integer.parseInt(row[9]);
                    int dMgra = Integer.parseInt(row[10]);
                    s.setOrig(oMgra);
                    s.setDest(dMgra);
                    stops.add(s);
                }
            }
            reader.close();
            
            reader = new BufferedReader(new FileReader(jointTripFileName));
            reader.readLine();
            while ( (line = reader.readLine()) != null) {
                String[] row = line.split(",");
                int tripMode = Integer.parseInt(row[11]);
                if ( tripMode == BIKE_MODE_INDEX ) { 
                    int hhId = Integer.parseInt(row[0]);
                    int tourId = Integer.parseInt(row[1]);
                    boolean inbound = Boolean.parseBoolean(row[3]);
                    Tour t = jointTourMap.get(hhId).get(tourId);
                    Stop s = new Stop(t,"","",0,inbound,0);
                    int stopPeriod = Integer.parseInt(row[10]);
                    s.setStopPeriod(stopPeriod);
                    int oMgra = Integer.parseInt(row[7]);
                    int dMgra = Integer.parseInt(row[8]);
                    s.setOrig(oMgra);
                    s.setDest(dMgra);
                    stops.add(s);
                }
            }
            reader.close();
            
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        
        return stops;
    }

}
