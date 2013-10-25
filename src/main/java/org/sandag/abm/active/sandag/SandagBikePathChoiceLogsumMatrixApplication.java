package org.sandag.abm.active.sandag;

import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class SandagBikePathChoiceLogsumMatrixApplication extends AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{

    private static final String[] MARKET_SEGMENT_NAMES = {"MaleMandatoryOutbound", "MaleMandatoryInbound", "MaleOther", "FemaleMandatoryOutbound", "FemaleMandatoryInbound", "FemaleOther"};
    private static final int[] MARKET_SEGMENT_FEMALE_VALUES = {0,0,0,1,1,1};
    private static final int[] MARKET_SEGMENT_TOUR_PURPOSE_INDICES = {1,1,4,1,1,4};
    private static final boolean[] MARKET_SEGMENT_INBOUND_TRIP_VALUES = {false,true,false,false,true,false};
    
    private ThreadLocal<SandagBikePathChoiceModel> model;
    private Person[] persons;
    private Tour[] tours;
    
    public SandagBikePathChoiceLogsumMatrixApplication(PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration)
    {
        super(configuration);
        model = new ThreadLocal<SandagBikePathChoiceModel>();
        persons = new Person[MARKET_SEGMENT_NAMES.length];
        tours = new Tour[MARKET_SEGMENT_NAMES.length];
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            // TODO: Check to ensure we can construct dummy persons and tours like this
            persons[i] = new Person(null, 0, null);
            persons[i].setPersGender(MARKET_SEGMENT_FEMALE_VALUES[i]);
            tours[i] = new Tour(null, 0, MARKET_SEGMENT_TOUR_PURPOSE_INDICES[i]);
        }
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        SandagBikePathAlternatives alts = new SandagBikePathAlternatives(alternativeList);
        double[] logsums = new double[MARKET_SEGMENT_NAMES.length]; 
        for (int i=0; i<MARKET_SEGMENT_NAMES.length; i++) {
            logsums[i] = model.get().getPathLogsums(persons[i], alts, MARKET_SEGMENT_INBOUND_TRIP_VALUES[i], tours[i]);
        }
        return logsums;    
    }
    
}
