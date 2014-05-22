package org.sandag.abm.active.sandag;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.IntrazonalCalculation;
import org.sandag.abm.active.IntrazonalCalculations;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import com.pb.common.util.ResourceUtil;

public class SandagWalkPathChoiceLogsumMatrixApplication
        extends
        AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>
{
    private static final Logger                                                                                   logger                                   = Logger.getLogger(SandagWalkPathChoiceLogsumMatrixApplication.class);

    public static final String                                                                                    WALK_LOGSUM_SKIM_MGRA_MGRA_FILE_PROPERTY = "active.logsum.matrix.file.walk.mgra";
    public static final String                                                                                    WALK_LOGSUM_SKIM_MGRA_TAP_FILE_PROPERTY  = "active.logsum.matrix.file.walk.mgratap";

    private final PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration;

    public SandagWalkPathChoiceLogsumMatrixApplication(
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration)
    {
        super(configuration);
        this.configuration = configuration;
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(
            PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        if (alternativeList.getCount() > 1)
        {
            throw new UnsupportedOperationException(
                    "Walk logsums cannot be calculated for alternative lists containing multiple paths");
        }

        double utility = 0;
        double distance = 0;
        SandagBikeNode parent = null;
        for (SandagBikeNode n : alternativeList.get(0))
        {
            if (parent != null)
            {
                utility += configuration.getNetwork().getEdge(parent, n).walkCost;
                distance += configuration.getNetwork().getEdge(parent, n).distance;
            }
            parent = n;
        }

        return new double[] {utility, distance * configuration.getDefaultMinutesPerMile()};
    }

    @Override
    protected List<IntrazonalCalculation<SandagBikeNode>> getMarketSegmentIntrazonalCalculations()
    {
        IntrazonalCalculation<SandagBikeNode> logsumIntrazonalCalculation = IntrazonalCalculations
                .<SandagBikeNode>minFactorIntrazonalCalculation(
                        IntrazonalCalculations.simpleFactorizer(0.5, 0), 1);
        // do time then distance
        return Arrays.asList(logsumIntrazonalCalculation, logsumIntrazonalCalculation);
    }

    public static void main(String... args)
    {
        if (args.length == 0)
        {
            logger.error(String
                    .format("no properties file base name (without .properties extension) was specified as an argument."));
            return;
        }
        logger.info("loading property file: "
                + ClassLoader.getSystemClassLoader().getResource(args[0] + ".properties").getFile()
                        .toString());

        logger.info("Building walk skims");
        // String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        @SuppressWarnings("unchecked")
        // this is ok - the map will be String->String
        Map<String, String> propertyMap = (Map<String, String>) ResourceUtil
                .getResourceBundleAsHashMap(args[0]);

        SandagBikeNetworkFactory factory = new SandagBikeNetworkFactory(propertyMap);
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network = factory
                .createNetwork();

        DecimalFormat formatter = new DecimalFormat("#.###");

        logger.info("Generating mgra->mgra walk skims");
        // mgra->mgra
        PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration = new SandagWalkMgraMgraPathAlternativeListGenerationConfiguration(
                propertyMap, network);
        SandagWalkPathChoiceLogsumMatrixApplication application = new SandagWalkPathChoiceLogsumMatrixApplication(
                configuration);
        Map<NodePair<SandagBikeNode>, double[]> logsums = application
                .calculateMarketSegmentLogsums();

        Path outputDirectory = Paths.get(configuration.getOutputDirectory());
        Path outputFile = outputDirectory.resolve(propertyMap
                .get(WALK_LOGSUM_SKIM_MGRA_MGRA_FILE_PROPERTY));

        try
        {
            Files.createDirectories(outputDirectory);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        Map<Integer, Integer> originCentroids = configuration.getInverseOriginZonalCentroidIdMap();
        Map<Integer, Integer> destinationCentroids = configuration
                .getInverseDestinationZonalCentroidIdMap();

        try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
        {
            writer.println("i,j,percieved,actual");
            StringBuilder sb;
            for (NodePair<SandagBikeNode> od : new TreeSet<>(logsums.keySet()))
            { // sort them so the output "looks nice"
                sb = new StringBuilder();
                sb.append(originCentroids.get(od.getFromNode().getId())).append(",");
                sb.append(destinationCentroids.get(od.getToNode().getId())).append(",");
                double[] values = logsums.get(od);
                sb.append(formatter.format(values[0])).append(","); // percieved
                                                                    // time
                sb.append(formatter.format(values[1])); // actual time
                writer.println(sb.toString());
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        logger.info("Generating mgra->tap walk skims");
        // mgra->tap
        configuration = new SandagWalkMgraTapPathAlternativeListGenerationConfiguration(
                propertyMap, network);
        application = new SandagWalkPathChoiceLogsumMatrixApplication(configuration);
        Map<NodePair<SandagBikeNode>, double[]> mgraTapLogsums = application
                .calculateMarketSegmentLogsums();

        // for later - get from the first configuration
        outputDirectory = Paths.get(configuration.getOutputDirectory());
        outputFile = outputDirectory.resolve(propertyMap
                .get(WALK_LOGSUM_SKIM_MGRA_TAP_FILE_PROPERTY));
        originCentroids = configuration.getInverseOriginZonalCentroidIdMap();
        destinationCentroids = configuration.getInverseDestinationZonalCentroidIdMap();

        // tap->mgra
        configuration = new SandagWalkTapMgraPathAlternativeListGenerationConfiguration(
                propertyMap, network);
        application = new SandagWalkPathChoiceLogsumMatrixApplication(configuration);
        Map<NodePair<SandagBikeNode>, double[]> tapMgraLogsums = application
                .calculateMarketSegmentLogsums();

        // resolve if not a pair
        int initialSize = mgraTapLogsums.size() + tapMgraLogsums.size();

        for (NodePair<SandagBikeNode> mgraTapPair : mgraTapLogsums.keySet())
        {
            NodePair<SandagBikeNode> tapMgraPair = new NodePair<SandagBikeNode>(
                    mgraTapPair.getToNode(), mgraTapPair.getFromNode());
            if (!tapMgraLogsums.containsKey(tapMgraPair))
                tapMgraLogsums.put(tapMgraPair, mgraTapLogsums.get(mgraTapPair));
        }

        for (NodePair<SandagBikeNode> tapMgraPair : tapMgraLogsums.keySet())
        {
            NodePair<SandagBikeNode> mgraTapPair = new NodePair<SandagBikeNode>(
                    tapMgraPair.getToNode(), tapMgraPair.getFromNode());
            if (!mgraTapLogsums.containsKey(mgraTapPair))
                mgraTapLogsums.put(mgraTapPair, tapMgraLogsums.get(tapMgraPair));
        }
        int asymmPairCount = initialSize - (mgraTapLogsums.size() + tapMgraLogsums.size());
        if (asymmPairCount > 0)
            logger.info("Boarding or alighting times defaulted to transpose for " + asymmPairCount
                    + " mgra tap pairs with missing asymmetrical information");

        try
        {
            Files.createDirectories(outputDirectory);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
        {
            writer.println("mgra,tap,boarsingPerceived,boardingActual,alightingPerceived,alightingActual");
            StringBuilder sb;
            for (NodePair<SandagBikeNode> od : new TreeSet<>(mgraTapLogsums.keySet()))
            { // sort them so the output "looks nice"
                sb = new StringBuilder();
                sb.append(originCentroids.get(od.getFromNode().getId())).append(",");
                sb.append(destinationCentroids.get(od.getToNode().getId())).append(",");
                double[] boardingValues = mgraTapLogsums.get(od);
                sb.append(formatter.format(boardingValues[0])).append(","); // boarding
                                                                            // percieved
                sb.append(formatter.format(boardingValues[1])).append(","); // boarding
                                                                            // actual
                double[] alightingValues = tapMgraLogsums.get(new NodePair<>(od.getToNode(), od
                        .getFromNode()));
                sb.append(formatter.format(alightingValues[0])).append(","); // alighting
                                                                             // percieved
                sb.append(formatter.format(alightingValues[1])); // alighting
                                                                 // actual
                writer.println(sb.toString());
            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
