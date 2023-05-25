package org.sandag.htm.processFAF;

import com.pb.common.datafile.TableDataSet;
import org.apache.log4j.Logger;

import java.util.ResourceBundle;

/**
 * Class to convert flows from tons into trucks by type
 * Author: Rolf Moeckel, PB Albuquerque
 * Data: 11 March 2013 (Santa Fe)
 */

public class convertTonsToTrucks {

    private static Logger logger = Logger.getLogger(convertTonsToTrucks.class);
    private String[] truckTypes = new String[] {"SingleUnit","TruckTrailer","CombinationSemitrailer","CombinationDoubleTriple"};
    private int[] fromMiles;
    private float[][] truckShareByDistance;
    private TableDataSet[] payloadByTruckType;
    private ResourceBundle appRb;

    public convertTonsToTrucks(ResourceBundle appRb) {
        this.appRb = appRb;
    }


    public void readData () {
        // read data to convert tons into trucks

        logger.info("  Reading FAF3 payload factors");
        // truck types by distance class
        TableDataSet truckTypeByDist = fafUtils.importTable(appRb.getString("truck.type.share.by.distance"));
        fromMiles = truckTypeByDist.getColumnAsInt("fromMiles");
        truckShareByDistance = new float[fromMiles.length][truckTypes.length];
        // todo: allow adjustment of SUT/MUT share
        for (int row = 1; row <= truckTypeByDist.getRowCount(); row++) {
            for (int col = 0; col < truckTypes.length; col++) {
                truckShareByDistance[row-1][col] = truckTypeByDist.getValueAt(row, truckTypes[col]);
            }
        }

        // truck body type by commodity
        payloadByTruckType = new TableDataSet[4];
        payloadByTruckType[0] = fafUtils.importTable(appRb.getString("truck.body.share.single.unit"));
        payloadByTruckType[0].buildIndex(payloadByTruckType[0].getColumnPosition("sctg"));
        payloadByTruckType[1] = fafUtils.importTable(appRb.getString("truck.body.share.tractor.trl"));
        payloadByTruckType[1].buildIndex(payloadByTruckType[1].getColumnPosition("sctg"));
        payloadByTruckType[2] = fafUtils.importTable(appRb.getString("truck.body.share.comb.semi.t"));
        payloadByTruckType[2].buildIndex(payloadByTruckType[2].getColumnPosition("sctg"));
        payloadByTruckType[3] = fafUtils.importTable(appRb.getString("truck.body.share.comb.dbl.tr"));
        payloadByTruckType[3].buildIndex(payloadByTruckType[3].getColumnPosition("sctg"));
    }


    public float[] convertThisFlowFromTonsToTrucks (String commodity, float distance, float flowInTons) {
        // convert commodity flow in tons into number of trucks by truck type

        // find correct distance class
        int distClass = 0;
        for (int i = 0; i < fromMiles.length; i++) if (distance > fromMiles[i]) distClass = i;

        // calculate trucks by truck type
        float[] trucksByType = new float[truckTypes.length];
        for (int i = 0; i < truckTypes.length; i++) trucksByType[i] = loadTruckShare(i, distClass, commodity, flowInTons);

        return trucksByType;
    }


    private float loadTruckShare (int truckType, int distClass, String commodity, float flowInTons) {
        // calculate payload for <truckType> with <distClass> for <commodity>

        int com = Integer.parseInt(commodity.substring(4));
        float tonsOnThisTruckType = flowInTons * truckShareByDistance[distClass][truckType];

        float trucks = 0;
        for (int col = 2; col <= payloadByTruckType[truckType].getColumnCount(); col++) {
            trucks += tonsOnThisTruckType * payloadByTruckType[truckType].getIndexedValueAt(com, col);
        }
        return trucks;
    }


    public double[] convertThisFlowFromTonsToTrucks (String commodity, float distance, double flowInTons) {
        // convert commodity flow in tons into number of trucks by truck type

        // find correct distance class
        int distClass = 0;
        for (int i = 0; i < fromMiles.length; i++) if (distance > fromMiles[i]) distClass = i;

        // calculate trucks by truck type
        double[] trucksByType = new double[truckTypes.length];
        for (int i = 0; i < truckTypes.length; i++) trucksByType[i] = loadTruckShare(i, distClass, commodity, flowInTons);

        return trucksByType;
    }


    private double loadTruckShare (int truckType, int distClass, String commodity, double flowInTons) {
        // calculate payload for <truckType> with <distClass> for <commodity>

        int com = Integer.parseInt(commodity.substring(4));
        double tonsOnThisTruckType = flowInTons * truckShareByDistance[distClass][truckType];

        double trucks = 0;
        for (int col = 2; col <= payloadByTruckType[truckType].getColumnCount(); col++) {
            trucks += tonsOnThisTruckType * payloadByTruckType[truckType].getIndexedValueAt(com, col);
        }
        return trucks;
    }

}
