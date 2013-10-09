package org.sandag.abm.active.sandag;

import static org.junit.Assert.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
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
    final static String PROPERTIES_OUTPUT_PATH = "active.output.path";
    final static String PROPERTIES_OUTPUT_LINK = "active.output.link";
    
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
        Map<NodePair<SandagBikeNode>, PathAlternativeList<SandagBikeNode, SandagBikeEdge>> alternativeLists = application.generateAlternativeLists();
        long time2 = System.currentTimeMillis();
        System.out.println("Count of od pairs: " + alternativeLists.size());
        
        try {
            PathAlternativeListWriter<SandagBikeNode, SandagBikeEdge> writer = new PathAlternativeListWriter<>(propertyMap.get(PROPERTIES_OUTPUT_PATH), propertyMap.get(PROPERTIES_OUTPUT_LINK));
            writer.writeHeaders();
            for ( NodePair<SandagBikeNode> odPair : alternativeLists.keySet() ) {
                if ( odPair.getFromNode().mgra % 1000 == 0 ) {
                    writer.write(alternativeLists.get(odPair));
                }
            }
            writer.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
        System.out.println("Time to generate (s): " + (time2-time1) / 1000 );
    }

}
