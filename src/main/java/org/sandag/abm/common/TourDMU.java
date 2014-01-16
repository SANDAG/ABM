package org.sandag.abm.common;

public abstract class TourDMU extends DMU {
	protected double getTransitSkimFromMethodName(String methodName)
			throws Exception {
		// "getWtw_lb_LB_ivt_out"
		String[] parts = methodName.split("_");
		String part1 = parts[0].replace("get", "");
		String part2 = parts[1];
		boolean part3HasUnderscore = (parts.length > 4);
		String part3 = parts[2];
		String part4 = parts[3];
		if (part3HasUnderscore) {
			part3 += "_" + parts[3];
			part4 = parts[4];
		}
		part3 = part3.replace("Time", "").replace("Walk", "");

		int var1 = -1, var2 = -1, var3 = -1, var4 = -1;

		if (part1.equalsIgnoreCase("WTW"))
			var1 = WTW;
		else if (part1.equalsIgnoreCase("WTD"))
			var1 = WTD;
		else if (part1.equalsIgnoreCase("DTW"))
			var1 = DTW;
		else
			throw new Exception(
					"First part of getTransitSkim is invalid with variable "
							+ var1);

		if (part2.equalsIgnoreCase("LB"))
			var2 = LB;
		else if (part2.equalsIgnoreCase("EB"))
			var2 = EB;
		else if (part2.equalsIgnoreCase("BRT"))
			var2 = BRT;
		else if (part2.equalsIgnoreCase("LR"))
			var2 = LR;
		else if (part2.equalsIgnoreCase("CR"))
			var2 = CR;
		else
			throw new Exception(
					"Second part of getTransitSkim is invalid with variable "
							+ var1);

		if (part3.equalsIgnoreCase("FWAIT"))
			var3 = FWAIT;
		else if (part3.equalsIgnoreCase("XWAIT"))
			var3 = XWAIT;
		else if (part3.equalsIgnoreCase("ACC"))
			var3 = ACC;
		else if (part3.equalsIgnoreCase("EGR"))
			var3 = EGR;
		else if (part3.equalsIgnoreCase("AUX"))
			var3 = AUX;
		else if (part3.equalsIgnoreCase("FARE"))
			var3 = FARE;
		else if (part3.equalsIgnoreCase("XFERS"))
			var3 = XFERS;
		else if (part3.equalsIgnoreCase("LB_IVT"))
			var3 = LB_IVT;
		else if (part3.equalsIgnoreCase("EB_IVT"))
			var3 = EB_IVT;
		else if (part3.equalsIgnoreCase("BRT_IVT"))
			var3 = BRT_IVT;
		else if (part3.equalsIgnoreCase("LRT_IVT")) // Note difference
			var3 = LR_IVT;
		else if (part3.equalsIgnoreCase("CR_IVT"))
			var3 = CR_IVT;
		else
			throw new Exception(
					"Third part of getTransitSkim is invalid with variable "
							+ var1);

		if (part4.equalsIgnoreCase("OUT"))
			var4 = OUT;
		else if (part4.equalsIgnoreCase("IN"))
			var4 = IN;
		else
			throw new Exception(
					"Fourth part of getTransitSkim is invalid with variable "
							+ var1);
		return getTransitSkim(var1, var2, var3, var4);
	}

	protected abstract double getTransitSkim(int var1, int var2, int var3,
			int var4);
}
