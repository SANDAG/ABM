package org.sandag.abm.active.sandag;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.sandag.abm.active.AbstractPathChoiceLogsumMatrixApplication;
import org.sandag.abm.active.IntrazonalCalculation;
import org.sandag.abm.active.IntrazonalCalculations;
import org.sandag.abm.active.Network;
import org.sandag.abm.active.NodePair;
import org.sandag.abm.active.PathAlternativeList;
import org.sandag.abm.active.PathAlternativeListGenerationConfiguration;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.BikeLogsum;
import org.sandag.abm.ctramp.Household;
import org.sandag.abm.ctramp.Person;
import org.sandag.abm.ctramp.Tour;
import com.pb.common.util.ResourceUtil;

public class SandagBikePathChoiceLogsumMatrixApplication
        extends
        AbstractPathChoiceLogsumMatrixApplication<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>
{
    private static final Logger                    logger                                     = Logger.getLogger(SandagBikePathChoiceLogsumMatrixApplication.class);

    public static final String                     PROPERTIES_DEBUG_ORIGIN                    = "active.debug.origin";
    public static final String                     PROPERTIES_DEBUG_DESTINATION               = "active.debug.destination";
    public static final String                     PROPERTIES_WRITE_DERIVED_BIKE_NETWORK      = "active.bike.write.derived.network";
    public static final String                     PROPERTIES_DERIVED_NETWORK_EDGES_FILE      = "active.bike.derived.network.edges";
    public static final String                     PROPERTIES_DERIVED_NETWORK_NODES_FILE      = "active.bike.derived.network.nodes";
    public static final String                     PROPERTIES_DERIVED_NETWORK_TRAVERSALS_FILE = "active.bike.derived.network.traversals";

    private static final String[]                  MARKET_SEGMENT_NAMES                       = {"logsum"};
    private static final int[]                     MARKET_SEGMENT_GENDER_VALUES               = {1};
    private static final int[]                     MARKET_SEGMENT_TOUR_PURPOSE_INDICES        = {1};
    private static final boolean[]                 MARKET_SEGMENT_INBOUND_TRIP_VALUES         = {false};

    private ThreadLocal<SandagBikePathChoiceModel> model;
    private Person[]                               persons;
    private Tour[]                                 tours;
    private final boolean                          runDebug;
    private final int                              debugOrigin;
    private final int                              debugDestination;

    public SandagBikePathChoiceLogsumMatrixApplication(
            PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration,
            final Map<String, String> propertyMap)
    {
        super(configuration);
        model = new ThreadLocal<SandagBikePathChoiceModel>()
        {
            @Override
            protected SandagBikePathChoiceModel initialValue()
            {
                return new SandagBikePathChoiceModel((HashMap<String, String>) propertyMap);
            }
        };
        persons = new Person[MARKET_SEGMENT_NAMES.length];
        tours = new Tour[MARKET_SEGMENT_NAMES.length];

        // for dummy person
        SandagModelStructure modelStructure = new SandagModelStructure();
        for (int i = 0; i < MARKET_SEGMENT_NAMES.length; i++)
        {
            //persons[i] = new Person(null, 1, modelStructure);
			Household hh = new Household(modelStructure);
			persons[i] = new Person(hh, 1, modelStructure);
            persons[i].setPersGender(MARKET_SEGMENT_GENDER_VALUES[i]);
            tours[i] = new Tour(persons[i], 1, MARKET_SEGMENT_TOUR_PURPOSE_INDICES[i]);
        }

        debugOrigin = propertyMap.containsKey(PROPERTIES_DEBUG_ORIGIN) ? Integer
                .parseInt(this.propertyMap.get(PROPERTIES_DEBUG_ORIGIN)) : -1;
        debugDestination = propertyMap.containsKey(PROPERTIES_DEBUG_DESTINATION) ? Integer
                .parseInt(this.propertyMap.get(PROPERTIES_DEBUG_DESTINATION)) : -1;
        runDebug = (debugOrigin > 0) && (debugDestination > 0);
    }

    @Override
    protected double[] calculateMarketSegmentLogsums(
            PathAlternativeList<SandagBikeNode, SandagBikeEdge> alternativeList)
    {
        SandagBikePathAlternatives alts = new SandagBikePathAlternatives(alternativeList);
        double[] logsums = new double[MARKET_SEGMENT_NAMES.length + 1];

        boolean debug = runDebug
                && (alternativeList.getODPair().getFromNode().getId() == debugOrigin)
                && (alternativeList.getODPair().getToNode().getId() == debugDestination);

        for (int i = 0; i < MARKET_SEGMENT_NAMES.length; i++)
            logsums[i] = model.get().getPathLogsums(persons[i], alts,
                    MARKET_SEGMENT_INBOUND_TRIP_VALUES[i], tours[i], debug);

        double[] probs = model.get().getPathProbabilities(persons[0], alts, false, tours[0], debug);
        double avgDist = 0;
        for (int i = 0; i < alts.getPathCount(); i++)
            avgDist += probs[i] * alts.getDistanceAlt(i);
        logsums[logsums.length - 1] = avgDist * configuration.getDefaultMinutesPerMile();

        return logsums;
    }

    @Override
    protected List<IntrazonalCalculation<SandagBikeNode>> getMarketSegmentIntrazonalCalculations()
    {
        List<IntrazonalCalculation<SandagBikeNode>> intrazonalCalculations = new ArrayList<>();
        IntrazonalCalculation<SandagBikeNode> logsumIntrazonalCalculation = IntrazonalCalculations
                .maxFactorIntrazonalCalculation(
                        IntrazonalCalculations.positiveNegativeFactorizer(0.5, 0, 2, 0), 1);
        // IntrazonalCalculations.maxFactorIntrazonalCalculation(IntrazonalCalculations.simpleFactorizer(1,Math.log(2)),1);
        for (int i = 0; i < MARKET_SEGMENT_NAMES.length; i++)
            intrazonalCalculations.add(logsumIntrazonalCalculation);
        // do distance
        intrazonalCalculations.add(IntrazonalCalculations
                .<SandagBikeNode>minFactorIntrazonalCalculation(
                        IntrazonalCalculations.simpleFactorizer(0.5, 0), 1));
        return intrazonalCalculations;
    }

    // these methods might be moved to the network classes...
    public static void writeDerivedNetworkEdges(
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network, Path outputFile)
    {
        try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
        {
            logger.info("Writing edges with derived attributes to " + outputFile.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("fromNode").append(",").append("toNode").append(",").append("bikeClass")
                    .append(",").append("lanes").append(",").append("functionalClass").append(",")
                    .append("centroidConnector").append(",").append("autosPermitted").append(",")
                    .append("cycleTrack").append(",").append("bikeBlvd").append(",")
                    .append("distance").append(",").append("gain").append(",").append("bikeCost")
                    .append(",").append("walkCost");
            writer.println(sb.toString());
            Iterator<SandagBikeEdge> it = network.edgeIterator();
            while (it.hasNext())
            {
                SandagBikeEdge edge = it.next();
                sb = new StringBuilder();
                sb.append(edge.getFromNode().getId()).append(",").append(edge.getToNode().getId())
                        .append(",").append(edge.bikeClass).append(",").append(edge.lanes)
                        .append(",").append(edge.functionalClass).append(",")
                        .append(edge.centroidConnector).append(",").append(edge.autosPermitted)
                        .append(",").append(edge.cycleTrack).append(",").append(edge.bikeBlvd)
                        .append(",").append(edge.distance).append(",").append(edge.gain)
                        .append(",").append(edge.bikeCost).append(",").append(edge.walkCost);
                writer.println(sb.toString());
            }
        } catch (IOException e)
        {
            logger.fatal(e);
            throw new RuntimeException(e);
        }
    }

    public static void writeDerivedNetworkNodes(
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network, Path outputFile)
    {
        try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
        {
            logger.info("Writing nodes with derived attributes to " + outputFile.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("id").append(",").append("x").append(",").append("y").append(",")
                    .append("mgra").append(",").append("taz").append(",").append("tap").append(",")
                    .append("signalized").append(",").append("centroid");
            writer.println(sb.toString());
            Iterator<SandagBikeNode> it = network.nodeIterator();
            while (it.hasNext())
            {
                SandagBikeNode node = it.next();
                sb = new StringBuilder();
                sb.append(node.getId()).append(",").append(node.x).append(",").append(node.y)
                        .append(",").append(node.mgra).append(",").append(node.taz).append(",")
                        .append(node.tap).append(",").append(node.signalized).append(",")
                        .append(node.centroid);
                writer.println(sb.toString());
            }
        } catch (IOException e)
        {
            logger.fatal(e);
            throw new RuntimeException(e);
        }
    }

    public static void writeDerivedNetworkTraversals(
            Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network, Path outputFile)
    {
        try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
        {
            logger.info("Writing traversals with derived attributes to " + outputFile.toString());
            StringBuilder sb = new StringBuilder();
            sb.append("start").append(",").append("thru").append(",").append("end").append(",")
                    .append("turnType").append(",").append("bikecost").append(",")
                    .append("thruCentroid").append(",").append("signalExclRight").append(",")
                    .append("unlfrma").append(",").append("unlfrmi").append(",").append("unxma")
                    .append(",").append("unxmi");
            writer.println(sb.toString());
            Iterator<SandagBikeTraversal> it = network.traversalIterator();
            while (it.hasNext())
            {
                SandagBikeTraversal traversal = it.next();
                sb = new StringBuilder();
                sb.append(traversal.getFromEdge().getFromNode().getId()).append(",")
                        .append(traversal.getFromEdge().getToNode().getId()).append(",")
                        .append(traversal.getToEdge().getToNode().getId()).append(",")
                        .append(traversal.turnType.getKey()).append(",").append(traversal.cost)
                        .append(",").append(traversal.thruCentroid).append(",")
                        .append(traversal.signalExclRightAndThruJunction).append(",")
                        .append(traversal.unsigLeftFromMajorArt).append(",")
                        .append(traversal.unsigLeftFromMinorArt).append(",")
                        .append(traversal.unsigCrossMajorArt).append(",")
                        .append(traversal.unsigCrossMinorArt);
                writer.println(sb.toString());
            }
        } catch (IOException e)
        {
            logger.fatal(e);
            throw new RuntimeException(e);
        }
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

        // String RESOURCE_BUNDLE_NAME = "sandag_abm_active_test";
        @SuppressWarnings("unchecked")
        // this is ok - the map will be String->String
        Map<String, String> propertyMap = (Map<String, String>) ResourceUtil
                .getResourceBundleAsHashMap(args[0]);
        DecimalFormat formatter = new DecimalFormat("#.###");

        SandagBikeNetworkFactory factory = new SandagBikeNetworkFactory(propertyMap);
        Network<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> network = factory
                .createNetwork();

        // order matters, taz first, then mgra, so use linked hash map
        Map<PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal>, String> configurationOutputMap = new LinkedHashMap<>();
        configurationOutputMap.put(new SandagBikeTazPathAlternativeListGenerationConfiguration(
                propertyMap, network), propertyMap.get(BikeLogsum.BIKE_LOGSUM_TAZ_FILE_PROPERTY));
        configurationOutputMap.put(new SandagBikeMgraPathAlternativeListGenerationConfiguration(
                propertyMap, network), propertyMap.get(BikeLogsum.BIKE_LOGSUM_MGRA_FILE_PROPERTY));

        for (PathAlternativeListGenerationConfiguration<SandagBikeNode, SandagBikeEdge, SandagBikeTraversal> configuration : configurationOutputMap
                .keySet())
        {
            Path outputDirectory = Paths.get(configuration.getOutputDirectory());
            Path outputFile = outputDirectory.resolve(configurationOutputMap.get(configuration));
            SandagBikePathChoiceLogsumMatrixApplication application = new SandagBikePathChoiceLogsumMatrixApplication(
                    configuration, propertyMap);

            Map<Integer, Integer> origins = configuration.getInverseOriginZonalCentroidIdMap();
            Map<Integer, Integer> dests = configuration.getInverseDestinationZonalCentroidIdMap();

            try
            {
                Files.createDirectories(outputDirectory);
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            Map<NodePair<SandagBikeNode>, double[]> logsums = application
                    .calculateMarketSegmentLogsums();

            try (PrintWriter writer = new PrintWriter(outputFile.toFile()))
            {
                StringBuilder sb = new StringBuilder("i,j");
                for (String segment : MARKET_SEGMENT_NAMES)
                    sb.append(",").append(segment);
                sb.append(",time");
                writer.println(sb.toString());
                for (NodePair<SandagBikeNode> od : logsums.keySet())
                {
                    sb = new StringBuilder();
                    sb.append(origins.get(od.getFromNode().getId())).append(",")
                            .append(dests.get(od.getToNode().getId()));
                    for (double value : logsums.get(od))
                        sb.append(",").append(formatter.format(value));
                    writer.println(sb.toString());
                }
            } catch (IOException e)
            {
                logger.fatal(e);
                throw new RuntimeException(e);
            }
        }

        if (Boolean.parseBoolean(propertyMap.get(PROPERTIES_WRITE_DERIVED_BIKE_NETWORK)))
        {
            Path outputDirectory = Paths.get(propertyMap
                    .get(SandagBikePathAlternativeListGenerationConfiguration.PROPERTIES_OUTPUT));
            writeDerivedNetworkEdges(network,
                    outputDirectory.resolve(propertyMap.get(PROPERTIES_DERIVED_NETWORK_EDGES_FILE)));
            writeDerivedNetworkNodes(network,
                    outputDirectory.resolve(propertyMap.get(PROPERTIES_DERIVED_NETWORK_NODES_FILE)));
            writeDerivedNetworkTraversals(network, outputDirectory.resolve(propertyMap
                    .get(PROPERTIES_DERIVED_NETWORK_TRAVERSALS_FILE)));
        }

    }
}
