package org.sandag.htm.processFAF;

/**
 * Defines reporting method
 *      internationalByEntryPoint reports international flows
 *      internat_domesticPart      = International flow from port of entry to final domestic destination or from
 *                                   domestic origin to port of exit
 *      internat_internationalPart = International flow from foreign origin to port of entry or from port of exit to
 *                                   foreign destination
 *      internatOrigToDest         = International flow from foreign origin to domestic destination or from domestic
 *                                   origin to foreign destination
 *      internatOrigToBorderToDest = International flow from origin to port of entry/port of exit to destination (2 flows)
 *
 * User: Rolf Moeckel, PB New York
 * Date: May 7, 2009
 */

public enum reportFormat {
    internat_domesticPart,
    internat_internationalPart,
    internatOrigToDest,
    internatOrigToBorderToDest,
}
