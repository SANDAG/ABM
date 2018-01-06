package org.sandag.htm.processFAF;

import org.apache.log4j.Logger;
import com.pb.common.datafile.TableDataSet;
import com.pb.common.util.ResourceUtil;

import java.util.ResourceBundle;
import java.util.HashMap;

/**
 * This class reads FAF2 data and stores data in TableDataSets
 * User: Rolf Moeckel
 * Date: May 6, 2009
 */

public class readFAF2 {

    Logger logger = Logger.getLogger(readFAF2.class);
    static public TableDataSet domRegionList;
    static public TableDataSet rowRegionList;
    static public String[] FAFzones;
    static public String[] sctgCommodities;
    static public String[] stccCommodities;
    static private int highestDomRegion;
    static public HashMap<String, Float> SCTGtoSTCCconversion;
    static private HashMap<String, Integer> sctgCode;

    private boolean referenceListsAreRead = false;
    private TableDataSet domesticTonCommodityFlows;
    private TableDataSet intBorderTonCommodityFlows;
    private TableDataSet intSeaTonCommodityFlows;
    private TableDataSet intAirTonCommodityFlows;
    private TableDataSet domesticDollarCommodityFlows;
    private TableDataSet intBorderDollarCommodityFlows;
    private TableDataSet intSeaDollarCommodityFlows;
    private TableDataSet intAirDollarCommodityFlows;
    private int factor;


    public void readCommodityList(ResourceBundle appRb) {
        // read commodity names
        TableDataSet sctgComList = fafUtils.importTable(ResourceUtil.getProperty(appRb, "faf2.sctg.commodity.list"));
        sctgCommodities = new String[sctgComList.getRowCount()];
        for (int i = 1; i <= sctgComList.getRowCount(); i++) sctgCommodities[i-1] = sctgComList.getStringValueAt(i, "SCTG");
    }


    public void readAllFAF2dataSets(ResourceBundle appRb, String unit) {
        // read all FAF2 data into TableDataSets in unit (= tons or dollars)
        if (unit.equals("tons")) {
            domesticTonCommodityFlows = readDomesticCommodityFlows(appRb, unit);
            intBorderTonCommodityFlows = readInternationalCommodityFlowsThroughLandBorder(appRb, unit);
            intSeaTonCommodityFlows = readInternationalCommodityFlowsBySea(appRb, unit);
            intAirTonCommodityFlows = readInternationalCommodityFlowsByAir(appRb, unit);
            factor = 1000;    // tons are in 1,000s
        } else if (unit.equals("dollars")) {
            domesticDollarCommodityFlows = readDomesticCommodityFlows(appRb, unit);
            intBorderDollarCommodityFlows = readInternationalCommodityFlowsThroughLandBorder(appRb, unit);
            intSeaDollarCommodityFlows = readInternationalCommodityFlowsBySea(appRb, unit);
            intAirDollarCommodityFlows = readInternationalCommodityFlowsByAir(appRb, unit);
            factor = 1000000;  // dollars are in 1,000,000s
        } else {
            logger.error("Wrong token " + unit + " in method readAllFAF2dataSets. Use tons or dollars.");
        }
    }


    public void adjustTruckFAF2data(ResourceBundle appRb, String mode, int[] years) {
        // adjust FAF2 growth to exogenous adjustment
        String fileName = appRb.getString("adjustment.of.faf." + mode);
        logger.info("Adjusting FAF2 " + mode + " forecast to growth rate set in " + fileName);
        TableDataSet adjust = fafUtils.importTable(fileName);
        adjust.buildIndex(adjust.getColumnPosition("Year"));
        for (int yr: years) {
            float fafForecast = adjust.getIndexedValueAt(yr, "FAF2forecast");
            float adjForecast = adjust.getIndexedValueAt(yr, "AdjustedNumber");
            if (fafForecast == adjForecast) continue;
            for (int row = 1; row <= domesticTonCommodityFlows.getRowCount(); row++) {
                if (!domesticTonCommodityFlows.getStringValueAt(row, "Mode").equalsIgnoreCase(mode)) continue;
                float value = domesticTonCommodityFlows.getValueAt(row, Integer.toString(yr));
                value = value * adjForecast / fafForecast;
                domesticTonCommodityFlows.setValueAt(row, Integer.toString(yr), value);
            }
            for (int row = 1; row <= intBorderTonCommodityFlows.getRowCount(); row++) {
                if (!intBorderTonCommodityFlows.getStringValueAt(row, "Mode").equalsIgnoreCase(mode)) continue;
                float value = intBorderTonCommodityFlows.getValueAt(row, Integer.toString(yr));
                value = value * adjForecast / fafForecast;
                intBorderTonCommodityFlows.setValueAt(row, Integer.toString(yr), value);
            }
            for (int row = 1; row <= intSeaTonCommodityFlows.getRowCount(); row++) {
                if (!intSeaTonCommodityFlows.getStringValueAt(row, "Mode").equalsIgnoreCase(mode)) continue;
                float value = intSeaTonCommodityFlows.getValueAt(row, Integer.toString(yr));
                value = value * adjForecast / fafForecast;
                intSeaTonCommodityFlows.setValueAt(row, Integer.toString(yr), value);
            }
            for (int row = 1; row <= intAirTonCommodityFlows.getRowCount(); row++) {
                if (!intAirTonCommodityFlows.getStringValueAt(row, "Mode").equalsIgnoreCase("Air & " + mode)) continue;
                float value = intAirTonCommodityFlows.getValueAt(row, Integer.toString(yr));
                value = value * adjForecast / fafForecast;
                intAirTonCommodityFlows.setValueAt(row, Integer.toString(yr), value);
            }
        }
    }


    public TableDataSet readDomesticCommodityFlows(ResourceBundle appRb, String unit) {
        // read domestic FAF2 flows in unit (tons or dollars)
        logger.info ("Reading domestic FAF2 data in " + unit);

        String fileName = ResourceUtil.getProperty(appRb, ("faf2.data.domestic." + unit));
        TableDataSet flows = fafUtils.importTable(fileName);
        if (!referenceListsAreRead) {
            readFAF2ReferenceLists(appRb);
            referenceListsAreRead = true;
        }
        int[] originCodes = new int[flows.getRowCount()];
        int[] destinationCodes = new int[flows.getRowCount()];
        for (int i = 1; i <= flows.getRowCount(); i++) {
            //find Origin and Destination codes
            originCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Origin"));
            destinationCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Destination"));
        }
        flows.appendColumn(originCodes, "OriginCode");
        flows.appendColumn(destinationCodes, "DestinationCode");
        return flows;
    }


    public static void readFAF2ReferenceLists(ResourceBundle appRb) {
        // read reference lists for zones and commodities
        domRegionList = fafUtils.importTable(ResourceUtil.getProperty(appRb, "faf2.region.list"));
        rowRegionList = fafUtils.importTable(ResourceUtil.getProperty(appRb, "faf2.row.region.list"));

        highestDomRegion = 0;
        for (int k = 1; k <= domRegionList.getRowCount(); k++)
            highestDomRegion = Math.max((int) domRegionList.getValueAt(k, "RegionCode"), highestDomRegion);
        int highestRegionOverAll = highestDomRegion;
        for (int k = 1; k <= rowRegionList.getRowCount(); k++)
            highestRegionOverAll = Math.max((int) rowRegionList.getValueAt(k, "ROWCode"), highestRegionOverAll);
        FAFzones = new String[highestRegionOverAll + 1];
        for (int k = 1; k <= domRegionList.getRowCount(); k++)
            FAFzones[(int) domRegionList.getValueAt(k, "RegionCode")] = domRegionList.getStringValueAt(k, "RegionName");
        for (int k = 1; k <= rowRegionList.getRowCount(); k++)
            FAFzones[(int) rowRegionList.getValueAt(k, "ROWCode")] = rowRegionList.getStringValueAt(k, "ROWRegionName");

        domRegionList.buildIndex(1);
        rowRegionList.buildIndex(1);

        TableDataSet sctgNumber = fafUtils.importTable(ResourceUtil.getProperty(appRb, "faf2.commodity.reference"));
        sctgCode = new HashMap<String, Integer>();
        for (int i = 1; i <= sctgNumber.getRowCount(); i++)
            sctgCode.put(sctgNumber.getStringValueAt(i, "FlowTableCategories"), (int) sctgNumber.getValueAt(i, "SCTG"));

    }


    private int findZoneCode(String strZone) {
        // find code of zone with name strZone
        int zoneCode = -1;
        // Domestic FAF zones
        for (int k = 1; k <= domRegionList.getRowCount(); k++)
            if (domRegionList.getStringValueAt(k, "RegionName").equals(strZone))
                zoneCode = (int) domRegionList.getValueAt(k, "RegionCode");
        // International FAF zones
        if (zoneCode == -1) {
            for (int k = 1; k <= rowRegionList.getRowCount(); k++)
                if (rowRegionList.getStringValueAt(k, "ROWRegionName").equals(strZone))
                    zoneCode = (int) rowRegionList.getValueAt(k, "ROWCode");
        }
        if (zoneCode == -1) {
            logger.error ("Unknown Zone in FAF2 data: " + strZone);
            System.exit(1);
        }
        return zoneCode;
    }


    public HashMap<String, Float> getDomesticFlows(String mode, commodityClassType comClass, int yr, String unit) {
        // extract domestic FAF2 flows
        TableDataSet flowTbl;
        if (unit.equals("tons")) {
            flowTbl = domesticTonCommodityFlows;
        } else {
            flowTbl = domesticDollarCommodityFlows;
        }

        HashMap<String, Float> hshFlows = new HashMap<String, Float>();
        for (int i = 1; i <= flowTbl.getRowCount(); i++) {
            String thisMode = flowTbl.getStringValueAt(i, "Mode");
            if (!mode.equals("all") && !thisMode.equalsIgnoreCase(mode)) continue;
            int origCode = (int) flowTbl.getValueAt(i, "OriginCode");
            int destCode = (int) flowTbl.getValueAt(i, "DestinationCode");
            float flows = flowTbl.getValueAt(i, Integer.toString(yr)) * factor;
            String fafDataCommodity = flowTbl.getStringValueAt(i, "Commodity");
            int intSctgCommmodity = sctgCode.get(fafDataCommodity);
            String sctgCommodity;
            if (intSctgCommmodity <= 9) sctgCommodity = "SCTG0" + intSctgCommmodity;
            else sctgCommodity = "SCTG" + intSctgCommmodity;
            // report flows by STCC commodity classification
            if (comClass.equals(commodityClassType.STCC)) {
                for (String com: stccCommodities) {
                    float flowShare = SCTGtoSTCCconversion.get(sctgCommodity + "_" + com) * flows;
                    String code;
                    if (flowShare > 0) {
                        if (mode.equals("all")) {
                            code = origCode + "_" + destCode + "_" + com + "_" + thisMode;
                        } else {
                            code = origCode + "_" + destCode + "_" + com;
                        }

                        if (hshFlows.containsKey(code)) flowShare += hshFlows.get(code);
                        hshFlows.put(code, flowShare);
                    }
                }
            } else {
                // report flows by SCTG commodity classification
                String code;
                if (mode.equals("all")) {
                    code = origCode + "_" + destCode + "_" + sctgCommodity + "_" + thisMode;
                } else {
                    code = origCode + "_" + destCode + "_" + sctgCommodity;
                }
                if (hshFlows.containsKey(code)) flows += hshFlows.get(code);
                hshFlows.put(code, flows);
            }
        }
        return hshFlows;
    }


    public TableDataSet readInternationalCommodityFlowsThroughLandBorder(ResourceBundle appRb, String unit) {
        // read international FAF2 flows that cross the U.S. border by land in unit (tons or dollars)
        logger.info ("Reading international FAF2 data crossing borders by land in " + unit);

        String fileName = ResourceUtil.getProperty(appRb, ("faf2.data.border." + unit));
        TableDataSet flows = fafUtils.importTable(fileName);
        if (!referenceListsAreRead) {
            readFAF2ReferenceLists(appRb);
            referenceListsAreRead = true;
        }
        int[] originCodes = new int[flows.getRowCount()];
        int[] portOfEntryCodes = new int[flows.getRowCount()];
        int[] destinationCodes = new int[flows.getRowCount()];
        for (int i = 1; i <= flows.getRowCount(); i++) {
            //find Origin and Destination codes
            originCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Origin"));
            portOfEntryCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "PortOfEntryExit"));
            destinationCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Destination"));
        }
        flows.appendColumn(originCodes, "OriginCode");
        flows.appendColumn(portOfEntryCodes, "PortOfEntryCode");
        flows.appendColumn(destinationCodes, "DestinationCode");
        return flows;
    }


    public HashMap<String, Float> getIntBorderFlows(reportFormat repform, String mode, commodityClassType comClass,
                                                    int yr, String unit) {
        // extract international FAF2 flows (international mode truck or train)

        TableDataSet flowTbl;
        if (unit.equals("tons")) {
            flowTbl = intBorderTonCommodityFlows;
        } else {
            flowTbl = intBorderDollarCommodityFlows;
        }

        HashMap<String, Float> hshFlows = new HashMap<String, Float>();
        for (int i = 1; i <= flowTbl.getRowCount(); i++) {
            String thisMode = flowTbl.getStringValueAt(i, "Mode");
            if (!mode.equals("all") && !thisMode.equals(mode)) continue;
            int origCode = (int) flowTbl.getValueAt(i, "OriginCode");
            int destCode = (int) flowTbl.getValueAt(i, "DestinationCode");
            String strOrig = flowTbl.getStringValueAt(i, "Origin");
            String strDest = flowTbl.getStringValueAt(i, "Destination");
            int borderCode = (int) flowTbl.getValueAt(i, "PortOfEntryCode");
            // if flow goes from WA Blain to Canada with port of exit WA Blain, don't report domestic part. - Changed my mind, do report this flow
//            if (repform == reportFormat.internat_domesticPart && (destCode == borderCode || origCode == borderCode)) continue;
            if (repform == reportFormat.internat_domesticPart) {
                boolean isCanadaOrMexico = checkIfMexicoOrCanada(strOrig);
                if (isCanadaOrMexico) origCode = borderCode;
                isCanadaOrMexico = checkIfMexicoOrCanada(strDest);
                if (isCanadaOrMexico) destCode = borderCode;
            } else if (repform == reportFormat.internat_internationalPart) {
                boolean isCanadaOrMexico = checkIfMexicoOrCanada(strOrig);
                if (isCanadaOrMexico) destCode = borderCode;
                isCanadaOrMexico = checkIfMexicoOrCanada(strDest);
                if (isCanadaOrMexico) origCode = borderCode;
            }
            // note that border flows are done differently than sea and air flows. Oregon has port and airports, that's
            // why those flows are extracted separately. Oregon has no international border, therefore border flows are
            // added to the US FAF region where the goods cross the border (unless reportFormat.internatOrigToDest has
            // been chosen).
            float flows = flowTbl.getValueAt(i, Integer.toString(yr)) * factor;
            String fafDataCommodity = flowTbl.getStringValueAt(i, "Commodity");
            int intSctgCommmodity = sctgCode.get(fafDataCommodity);
            String sctgCommodity;
            if (intSctgCommmodity <= 9) sctgCommodity = "SCTG0" + intSctgCommmodity;
            else sctgCommodity = "SCTG" + intSctgCommmodity;
            if (comClass.equals(commodityClassType.STCC)) {
                // report by STCC commodity classification
                for (String com: stccCommodities) {
                    float flowShare = SCTGtoSTCCconversion.get(sctgCommodity + "_" + com) * flows;
                    if (flowShare > 0) {
                        String code;
                        if (mode.equals("all")) {
                            code = origCode + "_" + destCode + "_" + sctgCommodity + "_" + thisMode;
                        } else {
                            code = origCode + "_" + destCode + "_" + sctgCommodity;
                        }
                        if (hshFlows.containsKey(code)) flowShare += hshFlows.get(code);
                        hshFlows.put(code, flowShare);
                    }
                }
            } else {
                // report by SCTG commodity classification
                String code;
                if (mode.equals("all")) {
                    code = origCode + "_" + destCode + "_" + sctgCommodity + "_" + thisMode;
                } else {
                    code = origCode + "_" + destCode + "_" + sctgCommodity;
                }
                if (hshFlows.containsKey(code)) flows += hshFlows.get(code);
                hshFlows.put(code, flows);
            }
        }
        return hshFlows;
    }


    public TableDataSet readInternationalCommodityFlowsBySea(ResourceBundle appRb, String unit) {
        // read international FAF2 flows that cross the U.S. border by sea in unit (tons or dollars)
        logger.info ("Reading international FAF2 data by sea in " + unit);

        String fileName = ResourceUtil.getProperty(appRb, ("faf2.data.sea." + unit));
        TableDataSet flows = fafUtils.importTable(fileName);
        if (!referenceListsAreRead) {
            readFAF2ReferenceLists(appRb);
            referenceListsAreRead = true;
        }
        int[] originCodes = new int[flows.getRowCount()];
        int[] portOfEntryCodes = new int[flows.getRowCount()];
        int[] destinationCodes = new int[flows.getRowCount()];
        for (int i = 1; i <= flows.getRowCount(); i++) {
            //find Origin and Destination codes
            originCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Origin"));
            portOfEntryCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Port"));
            destinationCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Destination"));
        }
        flows.appendColumn(originCodes, "OriginCode");
        flows.appendColumn(portOfEntryCodes, "PortCode");
        flows.appendColumn(destinationCodes, "DestinationCode");
        return flows;
    }


    public HashMap<String, Float> getIntSeaFlows (reportFormat repform, String mode, commodityClassType comClass,
                                                 int yr, String unit) {
        // extract international FAF2 flows (international mode sea)
        // If repform is set to internat_domesticPart port entry and exit points are set as negative origin or destination codes

        TableDataSet flowTbl;
        if (unit.equals("tons")) {
            flowTbl = intSeaTonCommodityFlows;
        } else {
            flowTbl = intSeaDollarCommodityFlows;
        }

        HashMap<String, Float> hshFlows = new HashMap<String, Float>();
        for (int i = 1; i <= flowTbl.getRowCount(); i++) {
            String thisMode = flowTbl.getStringValueAt(i, "Mode");
            if (!mode.equals("all") && !thisMode.equalsIgnoreCase(mode) &&
                    repform != reportFormat.internat_internationalPart) continue;
            if (!mode.equals("all") && !mode.equalsIgnoreCase("Water") &&
                    repform == reportFormat.internat_internationalPart) continue;
            int origCode = (int) flowTbl.getValueAt(i, "OriginCode");
            int destCode = (int) flowTbl.getValueAt(i, "DestinationCode");
            int portCode = -1 * (int) flowTbl.getValueAt(i, "PortCode");
            // if flow goes from Savannah GA to Europe with port of exit Savannah GA, don't report domestic part - changed my mind: do report this flow
//            if (repform == reportFormat.internat_domesticPart && (destCode == -portCode || origCode == -portCode)) continue;
            if (repform == reportFormat.internat_domesticPart) {
                if (origCode > highestDomRegion && destCode <= highestDomRegion)
                    // from abroad to U.S. -> set origin to entry port
                    origCode = portCode;
                else if (origCode <= highestDomRegion && destCode > highestDomRegion)
                    // from U.S. to abroad -> set destination to exit port
                    destCode = portCode;
                else if (origCode > highestDomRegion && destCode > highestDomRegion) {
                    // from abroad through U.S. port to abroad
                    boolean origMexCan = checkIfMexicoOrCanada(flowTbl.getStringValueAt(i, "Origin"));
                    // from Mexico or Canada through U.S. port to Overseas -> set destination to exit port
                    if (origMexCan) destCode = portCode;
                        // from Overseas through U.S. port to Mexico or Canada -> set origin to entry port
                    else origCode = portCode;
                    // Note: if both origin and destination are MEX/CAN or both origin and destination are overseas,
                    // it is impossible to determine which part of the trips was made by mode mode, therefore it is
                    // reported as from abroad to abroad without noting the entry/exit port [seems not to exist in FAF2 data]
                }
            } else if (repform == reportFormat.internat_internationalPart) {
                if (origCode > highestDomRegion && destCode <= highestDomRegion)
                    // from abroad to U.S. -> set destination to entry port
                    destCode = portCode;
                else if (origCode <= highestDomRegion && destCode > highestDomRegion)
                    // from U.S. to abroad -> set origin to exit port
                    origCode = portCode;
                else if (origCode > highestDomRegion && destCode > highestDomRegion) {
                    // from abroad through U.S. port to abroad
                    boolean origMexCan = checkIfMexicoOrCanada(flowTbl.getStringValueAt(i, "Origin"));
                    // from Mexico or Canada through U.S. port to Overseas -> set origin to exit port
                    if (origMexCan) origCode = portCode;
                    // from Overseas through U.S. port to Mexico or Canada -> set destination to entry port
                    else destCode = portCode;
                    // Note: if both origin and destination are MEX/CAN or both origin and destination are overseas,
                    // it is impossible to determine which part of the trips was made by mode mode, therefore it is
                    // reported as from abroad to abroad without noting the entry/exit port [seems not to exist in FAF2 data]
                }
            }
            float flows = flowTbl.getValueAt(i, Integer.toString(yr)) * factor;
            String fafDataCommodity = flowTbl.getStringValueAt(i, "Commodity");
            int intSctgCommmodity = sctgCode.get(fafDataCommodity);
            String sctgCommodity;
            if (intSctgCommmodity <= 9) sctgCommodity = "SCTG0" + intSctgCommmodity;
            else sctgCommodity = "SCTG" + intSctgCommmodity;
            if (comClass.equals(commodityClassType.STCC)) {
                for (String com: stccCommodities) {
                    float flowShare = SCTGtoSTCCconversion.get(sctgCommodity + "_" + com) * flows;
                    if (flowShare > 0) {
                        String code;
                        if (mode.equals("all")) {
                            code = origCode + "_" + destCode + "_" + sctgCommodity + "_" + thisMode;
                        } else {
                            code = origCode + "_" + destCode + "_" + sctgCommodity;
                        }
                        if (hshFlows.containsKey(code)) flowShare += hshFlows.get(code);
                        hshFlows.put(code, flowShare);
                    }
                }
            } else {
                // report by SCTG commodity classification
                String code;
                if (mode.equals("all")) {
                    code = origCode + "_" + destCode + "_" + sctgCommodity + "_" + thisMode;
                } else {
                    code = origCode + "_" + destCode + "_" + sctgCommodity;
                }
                if (hshFlows.containsKey(code)) flows += hshFlows.get(code);
                hshFlows.put(code, flows);
            }
        }
        return hshFlows;
    }


    private boolean checkIfMexicoOrCanada (String name) {
        // check if name is Canada or Mexico and return true or false
        boolean contains = false;
        if (name.equals("Canada") || name.equals("Mexico")) contains = true;
        return contains;
    }


    public TableDataSet readInternationalCommodityFlowsByAir(ResourceBundle appRb, String unit) {
        // read international FAF2 flows that cross the U.S. border by air in unit (tons or dollars)
        logger.info ("Reading international FAF2 data by air in " + unit);

        String fileName = ResourceUtil.getProperty(appRb, ("faf2.data.air." + unit));
        TableDataSet flows = fafUtils.importTable(fileName);
        if (!referenceListsAreRead) {
            readFAF2ReferenceLists(appRb);
            referenceListsAreRead = true;
        }
        int[] originCodes = new int[flows.getRowCount()];
        int[] portOfEntryCodes = new int[flows.getRowCount()];
        int[] destinationCodes = new int[flows.getRowCount()];
        for (int i = 1; i <= flows.getRowCount(); i++) {
            //find Origin and Destination codes
            originCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Origin"));
            portOfEntryCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Coast"));
            destinationCodes[i-1] = findZoneCode(flows.getStringValueAt(i, "Destination"));
        }
        flows.appendColumn(originCodes, "OriginCode");
        flows.appendColumn(portOfEntryCodes, "PortCode");
        flows.appendColumn(destinationCodes, "DestinationCode");
        return flows;
    }


    public HashMap<String, Float> getIntAirFlows(reportFormat repform, String mode, commodityClassType comClass,
                                                 int yr, String unit) {
        // extract international FAF2 flows (international mode air)
        // If repform is set to internat_domesticPart port entry and exit points are set as negative origin or destination codes

        TableDataSet flowTbl;
        if (unit.equals("tons")) {
            flowTbl = intAirTonCommodityFlows;
        } else {
            flowTbl = intAirDollarCommodityFlows;
        }

        HashMap<String, Float> hshFlows = new HashMap<String, Float>();
        for (int i = 1; i <= flowTbl.getRowCount(); i++) {
            // Note: Only mode "Air & Truck" is available in this data set
            if (repform == reportFormat.internat_domesticPart &&
                    !flowTbl.getStringValueAt(i, "Mode").equals(mode)) continue;
            if (repform == reportFormat.internat_internationalPart &&
                    !flowTbl.getStringValueAt(i, "Mode").equals("Air & Truck")) continue;
            int origCode = (int) flowTbl.getValueAt(i, "OriginCode");
            int destCode = (int) flowTbl.getValueAt(i, "DestinationCode");
            int portCode = -1 * (int) flowTbl.getValueAt(i, "PortCode");
            // if flow goes from Houston TX to Europe with port of exit Houston TX, don't report domestic part - changed my mind: do report this flow
//            if (repform == reportFormat.internat_domesticPart && (destCode == -portCode || origCode == -portCode)) continue;
            if (origCode <= highestDomRegion || destCode <= highestDomRegion) {
                // this should be the case for every record, there should be no international to international records
                if (repform == reportFormat.internat_domesticPart) {
                    if (origCode > highestDomRegion) origCode = portCode;
                    if (destCode > highestDomRegion) destCode = portCode;
                } else if (repform == reportFormat.internat_internationalPart) {
                    if (origCode > highestDomRegion) destCode = portCode;
                    if (destCode > highestDomRegion) origCode = portCode;
                }
            }
            float flows = flowTbl.getValueAt(i, Integer.toString(yr)) * factor;
            String fafDataCommodity = flowTbl.getStringValueAt(i, "Commodity");
            int intSctgCommmodity = sctgCode.get(fafDataCommodity);
            String sctgCommodity;
            if (intSctgCommmodity <= 9) sctgCommodity = "SCTG0" + intSctgCommmodity;
            else sctgCommodity = "SCTG" + intSctgCommmodity;
            if (comClass.equals(commodityClassType.STCC)) {
                for (String com: stccCommodities) {
                    float flowShare = SCTGtoSTCCconversion.get(sctgCommodity + "_" + com) * flows;
                    if (flowShare > 0) {
                        String code = origCode + "_" + destCode + "_" + com;
                        if (hshFlows.containsKey(code)) flowShare += hshFlows.get(code);
                        hshFlows.put(code, flowShare);
                    }
                }
            } else {
                // report by SCTG commodity classification
                String code = origCode + "_" + destCode + "_" + sctgCommodity;
                if (hshFlows.containsKey(code)) flows += hshFlows.get(code);
                hshFlows.put(code, flows);
            }

        }
        return hshFlows;
    }
}
