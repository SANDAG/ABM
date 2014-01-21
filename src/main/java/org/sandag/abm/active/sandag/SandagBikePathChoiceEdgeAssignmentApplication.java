package org.sandag.abm.active.sandag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.sandag.abm.active.*;
import org.sandag.abm.ctramp.*;

public class SandagBikePathChoiceEdgeAssignmentApplication extends AbstractPathChoiceEdgeAssignmentApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    Stop[] stops;
    final static String[] TIME_PERIOD_LABLES = {"EA","AM","MD","PM","EV"};
    
    //TODO: replace with actual maximum period number for aggregate assignment time periods
    final static double[] TIME_PERIOD_BREAKS = {0,0,0,0,99};
    
    private ThreadLocal<SandagBikePathChoiceModel> model;
    
    public SandagBikePathChoiceEdgeAssignmentApplication(PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration, Stop[] stops, final Map<String,String> propertyMap)
    {
        super(configuration);
        this.stops = stops;
        model = new ThreadLocal<SandagBikePathChoiceModel>() {
            @Override
            protected SandagBikePathChoiceModel initialValue() {
                return new SandagBikePathChoiceModel((HashMap<String,String>) propertyMap);
            }
        };
    }

    @Override
    protected Map<SandagBikeEdge, double[]> assignTrip(int tripNum,PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        Stop stop = stops[tripNum];
        Tour tour = stop.getTour();
        SandagBikePathAlternatives paths =  new SandagBikePathAlternatives(alternativeList);
        double[] probs = model.get().getPathProbabilities(tour.getPersonObject(), paths, stop.isInboundStop(), tour, false); 
        double numPersons = 1;
        if (tour.getPersonNumArray() != null) {
            numPersons = tour.getPersonNumArray().length;
        }
        int periodIdx = findFirstIndexGreaterThan((double)stop.getStopPeriod(),TIME_PERIOD_BREAKS);
        
        Map<SandagBikeEdge, double[]> volumes = new HashMap<>();
        for (int pathIdx=0; pathIdx<probs.length; pathIdx++) {
            SandagBikeNode parent = null;
            for (SandagBikeNode node : alternativeList.get(pathIdx)) {
                if (parent != null) {
                    SandagBikeEdge edge = network.getEdge(parent,node);
                    double[] values;
                    if (volumes.containsKey(edge)) {
                        values = volumes.get(edge); 
                    } else {
                        values = new double[TIME_PERIOD_BREAKS.length];
                        Arrays.fill(values, 0.0);
                    }
                    values[periodIdx] += probs[pathIdx] * numPersons;
                    volumes.put(edge, values);
                }
                parent = node;
            }
        }
        
        return volumes;
    }

    @Override
    protected SandagBikeNode getOriginNode(int tripId)
    {
        return network.getNode(configuration.getOriginZonalCentroidIdMap().get(stops[tripId].getOrig()));
    }

    @Override
    protected SandagBikeNode getDestinationNode(int tripId)
    {
        return network.getNode(configuration.getDestinationZonalCentroidIdMap().get(stops[tripId].getDest()));
    }

}
