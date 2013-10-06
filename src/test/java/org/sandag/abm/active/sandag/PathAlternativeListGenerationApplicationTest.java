package org.sandag.abm.active.sandag;

import static org.junit.Assert.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import org.junit.Before;
import org.junit.Test;
import org.sandag.abm.active.*;

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
        try {
            Map<NodePair<SandagBikeNode>, PathAlternativeList<SandagBikeNode, SandagBikeEdge>> alternativeLists = application.generateAlternativeLists();
        } catch (DestinationNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

}
