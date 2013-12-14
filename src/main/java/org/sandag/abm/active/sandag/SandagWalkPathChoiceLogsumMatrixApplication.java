package org.sandag.abm.active.sandag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;

public class SandagWalkPathChoiceLogsumMatrixApplication extends AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal>
{
    
    private PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration;
    
    public SandagWalkPathChoiceLogsumMatrixApplication(PathAlternativeListGenerationConfiguration<SandagBikeNode,SandagBikeEdge,SandagBikeTraversal> configuration)
    {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        if ( alternativeList.getCount() > 1 ) {
            throw new UnsupportedOperationException("Walk logsums cannot be calculated for alternative lists containing multiple paths");
        }
        
        double utility = 0;
        SandagBikeNode parent = null;
        for (SandagBikeNode n : alternativeList.get(0)) {
            if ( parent != null ) {
                utility -= configuration.getNetwork().getEdge(parent,n).walkCost;
            }
            parent = n;
        }

        return new double[] {-utility};    
    }
    
    public static void main(String ... args) {
        String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        Map<String,String> propertyMap = new HashMap<String,String>();
        SandagBikeNetworkFactory factory;
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network;
        List<PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>> configurations = new ArrayList<>();
        SandagWalkPathChoiceLogsumMatrixApplication application;
        
        ResourceBundle rb = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
        propertyMap = new HashMap<>();
        Enumeration<String> keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertyMap.put(key, rb.getString(key));
        }
        factory = new SandagBikeNetworkFactory(propertyMap);
        network = factory.createNetwork();

        configurations.add(new SandagWalkMgraMgraPathAlternativeListGenerationConfiguration(propertyMap,network));
        configurations.add(new SandagWalkMgraTapPathAlternativeListGenerationConfiguration(propertyMap,network));
        configurations.add(new SandagWalkTapMgraPathAlternativeListGenerationConfiguration(propertyMap,network));
        
        List<Map<NodePair<SandagBikeNode>,double[]>> allMatrices = new ArrayList<>();
        
        DecimalFormat formatter = new DecimalFormat("#.##");
        
        for(int i=0; i<configurations.size(); i++)  {
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration  = configurations.get(i);
            application = new SandagWalkPathChoiceLogsumMatrixApplication(configuration);
            Map<NodePair<SandagBikeNode>,double[]> logsums = application.calculateMarketSegmentLogsums();
            allMatrices.add(logsums);
        }
        
        int asymmPairCount = 0;
        
        for ( NodePair<SandagBikeNode> mgraTapPair : allMatrices.get(1).keySet() ) {
            NodePair<SandagBikeNode> tapMgraPair = new NodePair<SandagBikeNode>(mgraTapPair.getToNode(),mgraTapPair.getFromNode());
            if ( ! allMatrices.get(2).containsKey(tapMgraPair) ) {
                allMatrices.get(2).put(tapMgraPair, allMatrices.get(1).get(mgraTapPair));
                asymmPairCount++;
            }
        }
        
        for ( NodePair<SandagBikeNode> tapMgraPair : allMatrices.get(2).keySet() ) {
            NodePair<SandagBikeNode> mgraTapPair = new NodePair<SandagBikeNode>(tapMgraPair.getToNode(),tapMgraPair.getFromNode());
            if ( ! allMatrices.get(1).containsKey(mgraTapPair) ) {
                allMatrices.get(1).put(mgraTapPair, allMatrices.get(2).get(tapMgraPair));
                asymmPairCount++;
            }
        }
        
        new File(configurations.get(0).getOutputDirectory()).mkdirs();
        
        System.out.println("Boarding or alighting times defaulted to transpose for " + asymmPairCount + " mgra tap pairs with missing asymmetrical information");
            
        String filename = configurations.get(0).getOutputDirectory() + "/" + propertyMap.get("active.logsum.matrix.file.walk.mgra");
            
        Map<Integer,Integer> originCentroids = configurations.get(0).getInverseOriginZonalCentroidIdMap();
        Map<Integer,Integer> destinationCentroids = configurations.get(0).getInverseDestinationZonalCentroidIdMap();
            
        try
        {
            FileWriter writer = new FileWriter(new File(filename));
            writer.write("i, j, value\n");
            for (NodePair<SandagBikeNode> od : allMatrices.get(0).keySet()) {
                double[] values = allMatrices.get(0).get(od);
                writer.write(originCentroids.get(od.getFromNode().getId()) + ", " + destinationCentroids.get(od.getToNode().getId()));
                for (double v : values) {
                    writer.write(", " + formatter.format(v));
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();  
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        filename = configurations.get(1).getOutputDirectory() + "/" + propertyMap.get("active.logsum.matrix.file.walk.mgratap");
        originCentroids = configurations.get(1).getInverseOriginZonalCentroidIdMap();
        destinationCentroids = configurations.get(1).getInverseDestinationZonalCentroidIdMap();
        
        try
        {
            FileWriter writer = new FileWriter(new File(filename));
            writer.write("mgra, tap, boarding, alighting\n");
            for (NodePair<SandagBikeNode> od : allMatrices.get(1).keySet()) {
                NodePair<SandagBikeNode> doPair = new NodePair<>(od.getToNode(), od.getFromNode());
                double[] mgraTapValues = allMatrices.get(1).get(od);
                double[] tapMgraValues = allMatrices.get(2).get(doPair);
                writer.write(originCentroids.get(od.getFromNode().getId()) + ", " + destinationCentroids.get(od.getToNode().getId()));
                for (double v : mgraTapValues) {
                    writer.write(", " + formatter.format(v));
                }
                for (double v : tapMgraValues) {
                    writer.write(", " + formatter.format(v));
                }
                writer.write("\n");
            }
            writer.flush();
            writer.close();  
        } catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
