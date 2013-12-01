package org.sandag.abm.active.sandag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
        String[] fileProperties = new String[] {"active.logsum.matrix.file.walk.mgra","active.logsum.matrix.file.walk.mgraToTap","active.logsum.matrix.file.walk.tapToMgra"};
        
        for(int i=0; i<configurations.size(); i++)  {
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration  = configurations.get(i);
            String filename = configuration.getOutputDirectory() + propertyMap.get(fileProperties[i]);
            application = new SandagWalkPathChoiceLogsumMatrixApplication(configuration);
            
            new File(configuration.getOutputDirectory()).mkdirs();
            
            Map<NodePair<SandagBikeNode>,double[]> logsums = application.calculateMarketSegmentLogsums();
            Map<Integer,Integer> originCentroids = configuration.getInverseOriginZonalCentroidIdMap();
            Map<Integer,Integer> destinationCentroids = configuration.getInverseDestinationZonalCentroidIdMap();
            
            try
            {
                FileWriter writer = new FileWriter(new File(filename));
                writer.write("i, j, value\n");
                for (NodePair<SandagBikeNode> od : logsums.keySet()) {
                    writer.write(originCentroids.get(od.getFromNode().getId()) + ", " + destinationCentroids.get(od.getToNode().getId()) + ", " + Arrays.toString(logsums.get(od)).substring(1).replaceFirst("]", "") + "\n" );
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
    
}
