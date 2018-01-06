package com.pb.sandag_tm;

import org.sandag.htm.processFAF.disaggregateFlows;
import org.sandag.htm.processFAF.readFAF3;
import org.sandag.htm.processFAF.ReadFAF4;

import org.apache.log4j.Logger;

/**
 * Program to model SANDAG external truck flows based on FAF3 data
 * Author: Rolf Moeckel, PB Albuquerque
 * Date:   6 March 2013 (Santa Fe, NM)
 * Version 1.0
 * 
 * Modified 2017-12-28 to use FAF4 data by JEF, RSG
 */

public class sandag_tm {

    private static Logger logger = Logger.getLogger(sandag_tm.class);


    public static void main(String[] args) {

        long startTime = System.currentTimeMillis();

        ReadFAF4 faf4 = new ReadFAF4();
        disaggregateFlows df = new disaggregateFlows();
        utilities.truckModelInitialization(args, faf4, df);

        if (utilities.getModel().equalsIgnoreCase("counties")) {
            SandagCountyModel scm = new SandagCountyModel(faf4, df);
            scm.runSandagCountyModel();
        } else {
            sandagZonalModel szm = new sandagZonalModel();
            szm.runSandagZonalModel();
        }

        logger.info("Finished SANDAG Truck Model for year " + utilities.getYear());
        float endTime = utilities.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes.");
    }
}
