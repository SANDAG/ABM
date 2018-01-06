package org.sandag.htm.processFAF;

import com.pb.common.datafile.TableDataSet;

import java.util.HashMap;
import java.util.ResourceBundle;

/**
 * This class disaggregates and aggregates FAF flows from FAF zones to model zones (which may be smaller or larger than FAF zones)
 * User: Rolf Moeckel, PB Albuquerque
 * Date: November 17, 2011 (Santa Fe, NM)
 */

// todo: Started this class for NCSTM, but then realized that FAF zones do not nest in RMZ of NCSTM. Some FAF zones
// todo: would have to be split in parts and aggregated in other parts.
// todo: May be useful in other projects.

public class disaggregateAndAggregateFlows {

    private ResourceBundle appRb;
    private HashMap<Integer, int[]> zonesToDisaggregate;
    private int[] zonesToAggregate;

    public disaggregateAndAggregateFlows(ResourceBundle appRb) {
        this.appRb = appRb;
    }


    public void defineZonesToAggregate(String fileName) {
        // define which FAF zones need to be aggregated to larger model zones

        TableDataSet fafZones = fafUtils.importTable(fileName);
        int highestFAF = -1;
        for (int row = 1; row <= fafZones.getRowCount(); row++) {
            if (fafZones.getValueAt(row, "modelZone") != -1) highestFAF = (int) Math.max(highestFAF, fafZones.getValueAt(row, "modelZone"));
        }
        zonesToAggregate = new int[highestFAF + 1];
        for (int row = 1; row <= fafZones.getRowCount(); row++) {
            if (fafZones.getValueAt(row, "modelZone") == -1) {
                zonesToAggregate[(int) fafZones.getValueAt(row, "ZoneID")] = -1;
            } else {
                zonesToAggregate[(int) fafZones.getValueAt(row, "ZoneID")] = (int) fafZones.getValueAt(row, "modelZone");
            }
        }
    }


    public void defineZonesToDisaggregate(TableDataSet zoneSystem) {
        // define which FAF zones need to be disaggregated to smaller model zones

        zonesToDisaggregate = new HashMap<>();
        for (int row = 1; row <= zoneSystem.getRowCount(); row++) {
            int taz = (int) zoneSystem.getValueAt(row, "TAZ");
            int faf = (int) zoneSystem.getValueAt(row, "FAFzone");
            if (!zoneSystem.getBooleanValueAt(row, "disaggregate")) continue;
            if (zonesToDisaggregate.containsKey(faf)) {
                int[] zones = zonesToDisaggregate.get(faf);
                int[] zonesNew = new int[zones.length + 1];
                System.arraycopy(zones, 0, zonesNew, 0, zones.length);
                zonesNew[zones.length] = taz;
                zonesToDisaggregate.put(faf, zonesNew);
            } else {
                zonesToDisaggregate.put(faf, new int[]{taz});
            }
        }
    }
}
