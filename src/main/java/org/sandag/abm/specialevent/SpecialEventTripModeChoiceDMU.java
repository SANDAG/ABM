package org.sandag.abm.specialevent;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.sandag.abm.application.SandagModelStructure;
import org.sandag.abm.ctramp.McLogsumsCalculator;
import org.sandag.abm.ctramp.TourModeChoiceDMU;
import org.sandag.abm.ctramp.ModelStructure;

import com.pb.common.calculator.IndexValues;
import com.pb.common.calculator.VariableTable;

public class SpecialEventTripModeChoiceDMU implements Serializable,
		VariableTable {

	protected transient Logger logger = Logger.getLogger("specialEventModel");

	protected static final int LB = McLogsumsCalculator.LB;
	protected static final int EB = McLogsumsCalculator.EB;
	protected static final int BRT = McLogsumsCalculator.BRT;
	protected static final int LR = McLogsumsCalculator.LR;
	protected static final int CR = McLogsumsCalculator.CR;
	protected static final int NUM_LOC_PREM = McLogsumsCalculator.NUM_LOC_PREM;

	protected static final int WTW = McLogsumsCalculator.WTW;
	protected static final int WTD = McLogsumsCalculator.WTD;
	protected static final int DTW = McLogsumsCalculator.DTW;
	protected static final int NUM_ACC_EGR = McLogsumsCalculator.NUM_ACC_EGR;

	protected static final int LB_IVT = McLogsumsCalculator.LB_IVT;
	protected static final int EB_IVT = McLogsumsCalculator.EB_IVT;
	protected static final int BRT_IVT = McLogsumsCalculator.BRT_IVT;
	protected static final int LR_IVT = McLogsumsCalculator.LR_IVT;
	protected static final int CR_IVT = McLogsumsCalculator.CR_IVT;
	protected static final int ACC = McLogsumsCalculator.ACC;
	protected static final int EGR = McLogsumsCalculator.EGR;
	protected static final int AUX = McLogsumsCalculator.AUX;
	protected static final int FWAIT = McLogsumsCalculator.FWAIT;
	protected static final int XWAIT = McLogsumsCalculator.XWAIT;
	protected static final int FARE = McLogsumsCalculator.FARE;
	protected static final int XFERS = McLogsumsCalculator.XFERS;
	protected static final int NUM_SKIMS = McLogsumsCalculator.NUM_SKIMS;

	protected static final int OUT = McLogsumsCalculator.OUT;
	protected static final int IN = McLogsumsCalculator.IN;
	protected static final int NUM_DIR = McLogsumsCalculator.NUM_DIR;

	protected HashMap<String, Integer> methodIndexMap;
	protected IndexValues dmuIndex;

	protected int tourDepartPeriod;
	protected int tourArrivePeriod;
	protected int tripPeriod;
	protected int tripOrigIsTourDest;
	protected int tripDestIsTourDest;
	protected float parkingCost;
	protected float parkingTime;
	protected int income;
	protected int partySize;

	protected double nmWalkTime;
	protected double nmBikeTime;

	protected double[][][] transitSkim;
	protected int outboundHalfTourDirection;

	public SpecialEventTripModeChoiceDMU(SandagModelStructure modelStructure) {
		setupMethodIndexMap();
		dmuIndex = new IndexValues();
		transitSkim = new double[McLogsumsCalculator.NUM_ACC_EGR][McLogsumsCalculator.NUM_LOC_PREM][McLogsumsCalculator.NUM_SKIMS];

	}

	/**
	 * Set this index values for this tour mode choice DMU object.
	 * 
	 * @param hhIndex
	 *            is the DMU household index
	 * @param zoneIndex
	 *            is the DMU zone index
	 * @param origIndex
	 *            is the DMU origin index
	 * @param destIndex
	 *            is the DMU desatination index
	 */
	public void setDmuIndexValues(int hhIndex, int zoneIndex, int origIndex,
			int destIndex, boolean debug) {
		dmuIndex.setHHIndex(hhIndex);
		dmuIndex.setZoneIndex(zoneIndex);
		dmuIndex.setOriginZone(origIndex);
		dmuIndex.setDestZone(destIndex);

		dmuIndex.setDebug(false);
		dmuIndex.setDebugLabel("");
		if (debug) {
			dmuIndex.setDebug(true);
			dmuIndex.setDebugLabel("Debug MC UEC");
		}

	}

	public IndexValues getDmuIndexValues() {
		return dmuIndex;
	}

	/**
	 * @return the tripPeriod
	 */
	public int getTripPeriod() {
		return tripPeriod;
	}

	/**
	 * @param tripPeriod
	 *            the tripPeriod to set
	 */
	public void setTripPeriod(int tripPeriod) {
		this.tripPeriod = tripPeriod;
	}

	/**
	 * @return the tripOrigIsTourDest
	 */
	public int getTripOrigIsTourDest() {
		return tripOrigIsTourDest;
	}

	/**
	 * @param tripOrigIsTourDest
	 *            the tripOrigIsTourDest to set
	 */
	public void setTripOrigIsTourDest(int tripOrigIsTourDest) {
		this.tripOrigIsTourDest = tripOrigIsTourDest;
	}

	/**
	 * @return the tripDestIsTourDest
	 */
	public int getTripDestIsTourDest() {
		return tripDestIsTourDest;
	}

	/**
	 * @param tripDestIsTourDest
	 *            the tripDestIsTourDest to set
	 */
	public void setTripDestIsTourDest(int tripDestIsTourDest) {
		this.tripDestIsTourDest = tripDestIsTourDest;
	}

	/**
	 * @return the outboundHalfTourDirection
	 */
	public int getOutboundHalfTourDirection() {
		return outboundHalfTourDirection;
	}

	/**
	 * @param outboundHalfTourDirection
	 *            the outboundHalfTourDirection to set
	 */
	public void setOutboundHalfTourDirection(int outboundHalfTourDirection) {
		this.outboundHalfTourDirection = outboundHalfTourDirection;
	}

	/**
	 * @return the tourDepartPeriod
	 */
	public int getTourDepartPeriod() {
		return tourDepartPeriod;
	}

	/**
	 * @param tourDepartPeriod
	 *            the tourDepartPeriod to set
	 */
	public void setTourDepartPeriod(int tourDepartPeriod) {
		this.tourDepartPeriod = tourDepartPeriod;
	}

	/**
	 * @param tourArrivePeriod
	 *            the tourArrivePeriod to set
	 */
	public void setTourArrivePeriod(int tourArrivePeriod) {
		this.tourArrivePeriod = tourArrivePeriod;
	}

	/**
	 * @return the tourArrivePeriod
	 */
	public int getTourArrivePeriod() {
		return tourArrivePeriod;
	}

	public double getNmWalkTime() {
		return nmWalkTime;
	}

	public void setNonMotorizedWalkTime(double nmWalkTime) {
		this.nmWalkTime = nmWalkTime;
	}

	public void setNonMotorizedBikeTime(double nmBikeTime) {
		this.nmBikeTime = nmBikeTime;
	}

	public double getNmBikeTime() {
		return nmBikeTime;
	}

	/**
	 * @return the parkingCost
	 */
	public float getParkingCost() {
		return parkingCost;
	}

	/**
	 * @param parkingCost
	 *            the parkingCost to set
	 */
	public void setParkingCost(float parkingCost) {
		this.parkingCost = parkingCost;
	}

	/**
	 * @return the parkingTime
	 */
	public float getParkingTime() {
		return parkingTime;
	}

	/**
	 * @param parkingTime
	 *            the parkingTime to set
	 */
	public void setParkingTime(float parkingTime) {
		this.parkingTime = parkingTime;
	}

	/**
	 * @return the income
	 */
	public int getIncome() {
		return income;
	}

	/**
	 * @param income
	 *            the income to set
	 */
	public void setIncome(int income) {
		this.income = income;
	}

	/**
	 * @return the partySize
	 */
	public int getPartySize() {
		return partySize;
	}

	/**
	 * @param partySize
	 *            the partySize to set
	 */
	public void setPartySize(int partySize) {
		this.partySize = partySize;
	}

	public void setTransitSkim(int accEgr, int lbPrem, int skimIndex,
			double value) {
		transitSkim[accEgr][lbPrem][skimIndex] = value;
	}

	public double getTransitSkim(int accEgr, int lbPrem, int skimIndex) {
		return transitSkim[accEgr][lbPrem][skimIndex];
	}

	private void setupMethodIndexMap() {
		methodIndexMap = new HashMap<String, Integer>();

		methodIndexMap.put("getTourDepartPeriod", 0);
		methodIndexMap.put("getTourArrivePeriod", 1);
		methodIndexMap.put("getTripPeriod", 2);
		methodIndexMap.put("getParkingCost", 3);
		methodIndexMap.put("getParkingTime", 4);
		methodIndexMap.put("getTripOrigIsTourDest", 5);
		methodIndexMap.put("getTripDestIsTourDest", 6);
		methodIndexMap.put("getIncome", 7);
		methodIndexMap.put("getPartySize", 8);

		methodIndexMap.put("getNm_walkTime", 90);
		methodIndexMap.put("getNm_bikeTime", 91);

		methodIndexMap.put("getWtw_lb_LB_ivt", 100);
		methodIndexMap.put("getWtw_lb_fwait", 101);
		methodIndexMap.put("getWtw_lb_xwait", 102);
		methodIndexMap.put("getWtw_lb_AccTime", 103);
		methodIndexMap.put("getWtw_lb_EgrTime", 104);
		methodIndexMap.put("getWtw_lb_WalkAuxTime", 105);
		methodIndexMap.put("getWtw_lb_fare", 106);
		methodIndexMap.put("getWtw_lb_xfers", 107);
		methodIndexMap.put("getWtw_eb_LB_ivt", 108);
		methodIndexMap.put("getWtw_eb_EB_ivt", 109);
		methodIndexMap.put("getWtw_eb_fwait", 110);
		methodIndexMap.put("getWtw_eb_xwait", 111);
		methodIndexMap.put("getWtw_eb_AccTime", 112);
		methodIndexMap.put("getWtw_eb_EgrTime", 113);
		methodIndexMap.put("getWtw_eb_WalkAuxTime", 114);
		methodIndexMap.put("getWtw_eb_fare", 115);
		methodIndexMap.put("getWtw_eb_xfers", 116);
		methodIndexMap.put("getWtw_brt_LB_ivt", 117);
		methodIndexMap.put("getWtw_brt_EB_ivt", 118);
		methodIndexMap.put("getWtw_brt_BRT_ivt", 119);
		methodIndexMap.put("getWtw_brt_fwait", 120);
		methodIndexMap.put("getWtw_brt_xwait", 121);
		methodIndexMap.put("getWtw_brt_AccTime", 122);
		methodIndexMap.put("getWtw_brt_EgrTime", 123);
		methodIndexMap.put("getWtw_brt_WalkAuxTime", 124);
		methodIndexMap.put("getWtw_brt_fare", 125);
		methodIndexMap.put("getWtw_brt_xfers", 126);
		methodIndexMap.put("getWtw_lr_LB_ivt", 127);
		methodIndexMap.put("getWtw_lr_EB_ivt", 128);
		methodIndexMap.put("getWtw_lr_BRT_ivt", 129);
		methodIndexMap.put("getWtw_lr_LRT_ivt", 130);
		methodIndexMap.put("getWtw_lr_fwait", 131);
		methodIndexMap.put("getWtw_lr_xwait", 132);
		methodIndexMap.put("getWtw_lr_AccTime", 133);
		methodIndexMap.put("getWtw_lr_EgrTime", 134);
		methodIndexMap.put("getWtw_lr_WalkAuxTime", 135);
		methodIndexMap.put("getWtw_lr_fare", 136);
		methodIndexMap.put("getWtw_lr_xfers", 137);
		methodIndexMap.put("getWtw_cr_LB_ivt", 138);
		methodIndexMap.put("getWtw_cr_EB_ivt", 139);
		methodIndexMap.put("getWtw_cr_BRT_ivt", 140);
		methodIndexMap.put("getWtw_cr_LRT_ivt", 141);
		methodIndexMap.put("getWtw_cr_CR_ivt", 142);
		methodIndexMap.put("getWtw_cr_fwait", 143);
		methodIndexMap.put("getWtw_cr_xwait", 144);
		methodIndexMap.put("getWtw_cr_AccTime", 145);
		methodIndexMap.put("getWtw_cr_EgrTime", 146);
		methodIndexMap.put("getWtw_cr_WalkAuxTime", 147);
		methodIndexMap.put("getWtw_cr_fare", 148);
		methodIndexMap.put("getWtw_cr_xfers", 149);

		methodIndexMap.put("getDt_lb_LB_ivt", 150);
		methodIndexMap.put("getDt_lb_fwait", 151);
		methodIndexMap.put("getDt_lb_xwait", 152);
		methodIndexMap.put("getDt_lb_AccTime", 153);
		methodIndexMap.put("getDt_lb_EgrTime", 154);
		methodIndexMap.put("getDt_lb_DrvTime", 155);
		methodIndexMap.put("getDt_lb_WalkAuxTime", 156);
		methodIndexMap.put("getDt_lb_fare", 157);
		methodIndexMap.put("getDt_lb_xfers", 158);
		methodIndexMap.put("getDt_eb_LB_ivt", 159);
		methodIndexMap.put("getDt_eb_EB_ivt", 160);
		methodIndexMap.put("getDt_eb_fwait", 161);
		methodIndexMap.put("getDt_eb_xwait", 162);
		methodIndexMap.put("getDt_eb_AccTime", 163);
		methodIndexMap.put("getDt_eb_EgrTime", 164);
		methodIndexMap.put("getDt_eb_DrvTime", 165);
		methodIndexMap.put("getDt_eb_WalkAuxTime", 166);
		methodIndexMap.put("getDt_eb_fare", 167);
		methodIndexMap.put("getDt_eb_xfers", 168);
		methodIndexMap.put("getDt_brt_LB_ivt", 169);
		methodIndexMap.put("getDt_brt_EB_ivt", 170);
		methodIndexMap.put("getDt_brt_BRT_ivt", 171);
		methodIndexMap.put("getDt_brt_fwait", 172);
		methodIndexMap.put("getDt_brt_xwait", 173);
		methodIndexMap.put("getDt_brt_AccTime", 174);
		methodIndexMap.put("getDt_brt_EgrTime", 175);
		methodIndexMap.put("getDt_brt_DrvTime", 176);
		methodIndexMap.put("getDt_brt_WalkAuxTime", 177);
		methodIndexMap.put("getDt_brt_fare", 178);
		methodIndexMap.put("getDt_brt_xfers", 179);
		methodIndexMap.put("getDt_lr_LB_ivt", 180);
		methodIndexMap.put("getDt_lr_EB_ivt", 181);
		methodIndexMap.put("getDt_lr_BRT_ivt", 182);
		methodIndexMap.put("getDt_lr_LRT_ivt", 183);
		methodIndexMap.put("getDt_lr_fwait", 184);
		methodIndexMap.put("getDt_lr_xwait", 185);
		methodIndexMap.put("getDt_lr_AccTime", 186);
		methodIndexMap.put("getDt_lr_EgrTime", 187);
		methodIndexMap.put("getDt_lr_DrvTime", 188);
		methodIndexMap.put("getDt_lr_WalkAuxTime", 189);
		methodIndexMap.put("getDt_lr_fare", 190);
		methodIndexMap.put("getDt_lr_xfers", 191);
		methodIndexMap.put("getDt_cr_LB_ivt", 192);
		methodIndexMap.put("getDt_cr_EB_ivt", 193);
		methodIndexMap.put("getDt_cr_BRT_ivt", 194);
		methodIndexMap.put("getDt_cr_LRT_ivt", 195);
		methodIndexMap.put("getDt_cr_CR_ivt", 196);
		methodIndexMap.put("getDt_cr_fwait", 197);
		methodIndexMap.put("getDt_cr_xwait", 198);
		methodIndexMap.put("getDt_cr_AccTime", 199);
		methodIndexMap.put("getDt_cr_EgrTime", 200);
		methodIndexMap.put("getDt_cr_DrvTime", 201);
		methodIndexMap.put("getDt_cr_WalkAuxTime", 202);
		methodIndexMap.put("getDt_cr_fare", 203);
		methodIndexMap.put("getDt_cr_xfers", 204);

	}

	public double getValueForIndex(int variableIndex, int arrayIndex) {

		double returnValue = -1;

		switch (variableIndex) {

		case 0:
			returnValue = getTourDepartPeriod();
			break;
		case 1:
			returnValue = getTourArrivePeriod();
			break;
		case 2:
			returnValue = getTripPeriod();
			break;
		case 3:
			returnValue = getParkingCost();
			break;
		case 4:
			returnValue = getParkingTime();
		case 5:
			returnValue = getTripOrigIsTourDest();
			break;
		case 6:
			returnValue = getTripDestIsTourDest();
			break;
		case 7:
			returnValue = getIncome();
			break;
		case 8:
			returnValue = getPartySize();
			break;

		case 90:
			returnValue = getNmWalkTime();
			break;
		case 91:
			returnValue = getNmBikeTime();
			break;
		case 100:
			returnValue = getTransitSkim(WTW, LB, LB_IVT);
			break;
		case 101:
			returnValue = getTransitSkim(WTW, LB, FWAIT);
			break;
		case 102:
			returnValue = getTransitSkim(WTW, LB, XWAIT);
			break;
		case 103:
			returnValue = getTransitSkim(WTW, LB, ACC);
			break;
		case 104:
			returnValue = getTransitSkim(WTW, LB, EGR);
			break;
		case 105:
			returnValue = getTransitSkim(WTW, LB, AUX);
			break;
		case 106:
			returnValue = getTransitSkim(WTW, LB, FARE);
			break;
		case 107:
			returnValue = getTransitSkim(WTW, LB, XFERS);
			break;
		case 108:
			returnValue = getTransitSkim(WTW, EB, LB_IVT);
			break;
		case 109:
			returnValue = getTransitSkim(WTW, EB, EB_IVT);
			break;
		case 110:
			returnValue = getTransitSkim(WTW, EB, FWAIT);
			break;
		case 111:
			returnValue = getTransitSkim(WTW, EB, XWAIT);
			break;
		case 112:
			returnValue = getTransitSkim(WTW, EB, ACC);
			break;
		case 113:
			returnValue = getTransitSkim(WTW, EB, EGR);
			break;
		case 114:
			returnValue = getTransitSkim(WTW, EB, AUX);
			break;
		case 115:
			returnValue = getTransitSkim(WTW, EB, FARE);
			break;
		case 116:
			returnValue = getTransitSkim(WTW, EB, XFERS);
			break;
		case 117:
			returnValue = getTransitSkim(WTW, BRT, LB_IVT);
			break;
		case 118:
			returnValue = getTransitSkim(WTW, BRT, EB_IVT);
			break;
		case 119:
			returnValue = getTransitSkim(WTW, BRT, BRT_IVT);
			break;
		case 120:
			returnValue = getTransitSkim(WTW, BRT, FWAIT);
			break;
		case 121:
			returnValue = getTransitSkim(WTW, BRT, XWAIT);
			break;
		case 122:
			returnValue = getTransitSkim(WTW, BRT, ACC);
			break;
		case 123:
			returnValue = getTransitSkim(WTW, BRT, EGR);
			break;
		case 124:
			returnValue = getTransitSkim(WTW, BRT, AUX);
			break;
		case 125:
			returnValue = getTransitSkim(WTW, BRT, FARE);
			break;
		case 126:
			returnValue = getTransitSkim(WTW, BRT, XFERS);
			break;
		case 127:
			returnValue = getTransitSkim(WTW, LR, LB_IVT);
			break;
		case 128:
			returnValue = getTransitSkim(WTW, LR, EB_IVT);
			break;
		case 129:
			returnValue = getTransitSkim(WTW, LR, BRT_IVT);
			break;
		case 130:
			returnValue = getTransitSkim(WTW, LR, LR_IVT);
			break;
		case 131:
			returnValue = getTransitSkim(WTW, LR, FWAIT);
			break;
		case 132:
			returnValue = getTransitSkim(WTW, LR, XWAIT);
			break;
		case 133:
			returnValue = getTransitSkim(WTW, LR, ACC);
			break;
		case 134:
			returnValue = getTransitSkim(WTW, LR, EGR);
			break;
		case 135:
			returnValue = getTransitSkim(WTW, LR, AUX);
			break;
		case 136:
			returnValue = getTransitSkim(WTW, LR, FARE);
			break;
		case 137:
			returnValue = getTransitSkim(WTW, LR, XFERS);
			break;
		case 138:
			returnValue = getTransitSkim(WTW, CR, LB_IVT);
			break;
		case 139:
			returnValue = getTransitSkim(WTW, CR, EB_IVT);
			break;
		case 140:
			returnValue = getTransitSkim(WTW, CR, BRT_IVT);
			break;
		case 141:
			returnValue = getTransitSkim(WTW, CR, LR_IVT);
			break;
		case 142:
			returnValue = getTransitSkim(WTW, CR, CR_IVT);
			break;
		case 143:
			returnValue = getTransitSkim(WTW, CR, FWAIT);
			break;
		case 144:
			returnValue = getTransitSkim(WTW, CR, XWAIT);
			break;
		case 145:
			returnValue = getTransitSkim(WTW, CR, ACC);
			break;
		case 146:
			returnValue = getTransitSkim(WTW, CR, EGR);
			break;
		case 147:
			returnValue = getTransitSkim(WTW, CR, AUX);
			break;
		case 148:
			returnValue = getTransitSkim(WTW, CR, FARE);
			break;
		case 149:
			returnValue = getTransitSkim(WTW, CR, XFERS);
			break;
		case 150:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, LB_IVT);
			else
				returnValue = getTransitSkim(WTD, LB, LB_IVT);
			break;
		case 151:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, FWAIT);
			else
				returnValue = getTransitSkim(WTD, LB, FWAIT);
			break;
		case 152:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, XWAIT);
			else
				returnValue = getTransitSkim(WTD, LB, XWAIT);
			break;
		case 153:
			if (outboundHalfTourDirection == 1)
				returnValue = 0;
			else
				returnValue = getTransitSkim(WTD, LB, ACC);
			break;
		case 154:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, EGR);
			else
				returnValue = 0;
			break;
		case 155:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, ACC);
			else
				returnValue = getTransitSkim(WTD, LB, EGR);
			break;
		case 156:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, AUX);
			else
				returnValue = getTransitSkim(WTD, LB, AUX);
			break;
		case 157:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, FARE);
			else
				returnValue = getTransitSkim(WTD, LB, FARE);
			break;
		case 158:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LB, XFERS);
			else
				returnValue = getTransitSkim(WTD, LB, XFERS);
			break;
		case 159:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, LB_IVT);
			else
				returnValue = getTransitSkim(WTD, EB, LB_IVT);
			break;
		case 160:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, EB_IVT);
			else
				returnValue = getTransitSkim(WTD, EB, EB_IVT);
			break;
		case 161:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, FWAIT);
			else
				returnValue = getTransitSkim(WTD, EB, FWAIT);
			break;
		case 162:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, XWAIT);
			else
				returnValue = getTransitSkim(WTD, EB, XWAIT);
			break;
		case 163:
			if (outboundHalfTourDirection == 1)
				returnValue = 0;
			else
				returnValue = getTransitSkim(WTD, EB, ACC);
			break;
		case 164:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, EGR);
			else
				returnValue = 0;
			break;
		case 165:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, ACC);
			else
				returnValue = getTransitSkim(WTD, EB, EGR);
			break;
		case 166:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, AUX);
			else
				returnValue = getTransitSkim(WTD, EB, AUX);
			break;
		case 167:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, FARE);
			else
				returnValue = getTransitSkim(WTD, EB, FARE);
			break;
		case 168:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, EB, XFERS);
			else
				returnValue = getTransitSkim(WTD, EB, XFERS);
			break;
		case 169:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, LB_IVT);
			else
				returnValue = getTransitSkim(WTD, BRT, LB_IVT);
			break;
		case 170:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, EB_IVT);
			else
				returnValue = getTransitSkim(WTD, BRT, EB_IVT);
			break;
		case 171:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, BRT_IVT);
			else
				returnValue = getTransitSkim(WTD, BRT, BRT_IVT);
			break;
		case 172:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, FWAIT);
			else
				returnValue = getTransitSkim(WTD, BRT, FWAIT);
			break;
		case 173:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, XWAIT);
			else
				returnValue = getTransitSkim(WTD, BRT, XWAIT);
			break;
		case 174:
			if (outboundHalfTourDirection == 1)
				returnValue = 0;
			else
				returnValue = getTransitSkim(WTD, BRT, ACC);
			break;
		case 175:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, EGR);
			else
				returnValue = 0;
			break;
		case 176:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, ACC);
			else
				returnValue = getTransitSkim(WTD, BRT, EGR);
			break;
		case 177:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, AUX);
			else
				returnValue = getTransitSkim(WTD, BRT, AUX);
			break;
		case 178:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, FARE);
			else
				returnValue = getTransitSkim(WTD, BRT, FARE);
			break;
		case 179:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, BRT, XFERS);
			else
				returnValue = getTransitSkim(WTD, BRT, XFERS);
			break;
		case 180:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, LB_IVT);
			else
				returnValue = getTransitSkim(WTD, LR, LB_IVT);
			break;
		case 181:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, EB_IVT);
			else
				returnValue = getTransitSkim(WTD, LR, EB_IVT);
			break;
		case 182:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, BRT_IVT);
			else
				returnValue = getTransitSkim(WTD, LR, BRT_IVT);
			break;
		case 183:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, LR_IVT);
			else
				returnValue = getTransitSkim(WTD, LR, LR_IVT);
			break;
		case 184:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, FWAIT);
			else
				returnValue = getTransitSkim(WTD, LR, FWAIT);
			break;
		case 185:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, XWAIT);
			else
				returnValue = getTransitSkim(WTD, LR, XWAIT);
			break;
		case 186:
			if (outboundHalfTourDirection == 1)
				returnValue = 0;
			else
				returnValue = getTransitSkim(WTD, LR, ACC);
			break;
		case 187:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, EGR);
			else
				returnValue = 0;
			break;
		case 188:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, ACC);
			else
				returnValue = getTransitSkim(WTD, LR, EGR);
			break;
		case 189:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, AUX);
			else
				returnValue = getTransitSkim(WTD, LR, AUX);
			break;
		case 190:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, FARE);
			else
				returnValue = getTransitSkim(WTD, LR, FARE);
			break;
		case 191:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, LR, XFERS);
			else
				returnValue = getTransitSkim(WTD, LR, XFERS);
			break;
		case 192:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, LB_IVT);
			else
				returnValue = getTransitSkim(WTD, CR, LB_IVT);
			break;
		case 193:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, EB_IVT);
			else
				returnValue = getTransitSkim(WTD, CR, EB_IVT);
			break;
		case 194:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, BRT_IVT);
			else
				returnValue = getTransitSkim(WTD, CR, BRT_IVT);
			break;
		case 195:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, LR_IVT);
			else
				returnValue = getTransitSkim(WTD, CR, LR_IVT);
			break;
		case 196:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, CR_IVT);
			else
				returnValue = getTransitSkim(WTD, CR, CR_IVT);
			break;
		case 197:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, FWAIT);
			else
				returnValue = getTransitSkim(WTD, CR, FWAIT);
			break;
		case 198:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, XWAIT);
			else
				returnValue = getTransitSkim(WTD, CR, XWAIT);
			break;
		case 199:
			if (outboundHalfTourDirection == 1)
				returnValue = 0;
			else
				returnValue = getTransitSkim(WTD, CR, ACC);
			break;
		case 200:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, EGR);
			else
				returnValue = 0;
			break;
		case 201:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, ACC);
			else
				returnValue = getTransitSkim(WTD, CR, EGR);
			break;
		case 202:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, AUX);
			else
				returnValue = getTransitSkim(WTD, CR, AUX);
			break;
		case 203:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, FARE);
			else
				returnValue = getTransitSkim(WTD, CR, FARE);
			break;
		case 204:
			if (outboundHalfTourDirection == 1)
				returnValue = getTransitSkim(DTW, CR, XFERS);
			else
				returnValue = getTransitSkim(WTD, CR, XFERS);
			break;

		default:
			logger.error("method number = " + variableIndex + " not found");
			throw new RuntimeException("method number = " + variableIndex
					+ " not found");

		}

		return returnValue;

	}

	public int getIndexValue(String variableName) {
		return methodIndexMap.get(variableName);
	}

	public int getAssignmentIndexValue(String variableName) {
		throw new UnsupportedOperationException();
	}

	public double getValueForIndex(int variableIndex) {
		throw new UnsupportedOperationException();
	}

	public void setValue(String variableName, double variableValue) {
		throw new UnsupportedOperationException();
	}

	public void setValue(int variableIndex, double variableValue) {
		throw new UnsupportedOperationException();
	}

}