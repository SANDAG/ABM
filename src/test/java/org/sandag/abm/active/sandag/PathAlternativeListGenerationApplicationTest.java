package org.sandag.abm.active.sandag;

import static org.junit.Assert.*;
import java.util.Enumeration;
import java.util.*;
import java.util.ResourceBundle;
import org.junit.*;
import org.sandag.abm.active.*;
import java.io.*;

public class PathAlternativeListGenerationApplicationTest
{
    final static String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
    Map<String,String> propertyMap = new HashMap<String,String>();
    SandagBikeNetworkFactory factory;
    Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
    PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration;
    PathAlternativeListGenerationApplication<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> application;
    
    @Before
    public void setUp() {
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();
        configuration = new SandagBikeMgraPathAlternativeListGenerationConfiguration(propertyMap, network);
        application =  new PathAlternativeListGenerationApplication<>(configuration);
    }

    @Test
    public void testGenerateAlternativeLists()
    {
        long time1 = System.currentTimeMillis();
        Map<NodePair<SandagBikeNode>, PathAlternativeList<SandagBikeNode,SandagBikeEdge>> alternativeLists = application.generateAlternativeLists();
        long time2 = System.currentTimeMillis();
        
        System.out.println("Time to generate (s): " + (time2-time1) / 1000 );
        
        PropertyParser parser =  new PropertyParser(propertyMap);
        String outputDir = propertyMap.get("active.sample.output");
        
        List<Integer> traceOrigins = parser.parseIntPropertyList("active.trace.origins");
        
        for (Integer origin : traceOrigins) {
            
            try {

                if ( configuration.getNearbyZonalDistanceMap().containsKey(origin) ) {
                    PathAlternativeListWriter<SandagBikeNode,SandagBikeEdge> writer = new PathAlternativeListWriter<>(outputDir + "paths_" + origin + ".csv", outputDir + "links_" + origin + ".csv");
                    writer.writeHeaders();
                
                    for (int dest : configuration.getNearbyZonalDistanceMap().get(origin).keySet() ) {
                        NodePair<SandagBikeNode> odPair = new NodePair<>(network.getNode(configuration.getZonalCentroidIdMap().get(origin)), network.getNode(configuration.getZonalCentroidIdMap().get(dest)));
                        writer.write(alternativeLists.get(odPair));
                    } 
                
                    writer.close();
                }
                
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
            
        }
    }

}
