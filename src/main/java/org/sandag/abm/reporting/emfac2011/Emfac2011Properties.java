package org.sandag.abm.reporting.emfac2011;

import java.util.Arrays;
import java.util.Map;
import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * The {@code Emfac2011Properties} holds the properties used by the various
 * classes in this package. The property keys listed in this class (as
 * {@code public static final} constants) must all be present in the
 * instantiated properties object, or it will throw an error during
 * construction. Additionally, {@link #AREAS_PROPERTY} must be typed correctly
 * as a map from a string to a list holding strings (see its documentation for
 * more information).
 * 
 * @author crf Started 2/9/12 8:40 AM
 */
public class Emfac2011Properties
        extends PropertyDeluxe
{
    /**
     * The property key for the EMFAC2011 area type name.
     */
    public static final String AREA_TYPE_PROPERTY                        = "emfac.2011.area.type";
    /**
     * The property key for the EMFAC2011 region name.
     */
    public static final String REGION_NAME_PROPERTY                      = "emfac.2011.region.name";
    /**
     * The property key for the mapping from EMFAC2011 areas to model districts
     * used for the model run. These areas must be specified as a parsable map
     * from a string key to a string list. That is, the property entry should
     * look something like the following:
     * 
     * <pre>
     * <code>
     *     emfac.2011.area(MSLS) = {El Dorado (LT):[El],Placer (LT):[Pl]}
     * </code>
     * </pre>
     */
    public static final String AREAS_PROPERTY                            = "emfac.2011.area";
    /**
     * The property key for the EMFAC2011 season.
     */
    public static final String SEASON_PROPERTY                           = "emfac.2011.season";
    /**
     * The property key for the model run year. This must be parsable as an
     * integer.
     */
    public static final String YEAR_PROPERTY                             = "emfac.2011.year";
    /**
     * The property key for the installation directory of the EMFAC2011 model.
     * This directory is the one which holds the EMFAC2011 access database and
     * the shortcut used to start it.
     */
    public static final String EMFAC2011_INSTALLATION_DIR_PROPERTY       = "emfac.2011.installation.dir";
    /**
     * The property key for the boolean indicating if the (default) EMFAC
     * vehicle fractions should be preserved in the EMFAC input file (value is
     * {@code true}), or if the model vehicle fractions should be used (value is
     * {@code false}).
     */
    public static final String PRESERVE_EMFAC_VEHICLE_FRACTIONS_PROPERTY = "emfac.2011.preserve.emfac.vehicle.fractions";
    /**
     * The property key for the boolean indicating whether or not the model
     * (travel demand, not EMFAC) VMT includes totals for non-mutable vehicle
     * types. If it does, then the VMT will be scaled before adjusting the EMFAC
     * input file.
     */
    public static final String MODEL_VMT_INCLUDES_NON_MUTABLES_PROPERTY  = "emfac.2011.model.vmt.includes.non.mutable.vehicles";
    /**
     * The property key for the output directory to use for the EMFAC2011 setup
     * and model run.
     */
    public static final String OUTPUT_DIR_PROPERTY                       = "emfac.2011.output.dir";
    /**
     * The property key for the location of the xls converter program. This
     * program converts the malformed EMFAC2011 reference files to a (strictly)
     * valid format, which can then be used by the rest of the model.
     */
    public static final String XLS_CONVERTER_PROGRAM_PROPERTY            = "emfac.2011.xls.converter.program";
    /**
     * The property key for the location (full path) of the AquaVis output
     * network file.
     */
    public static final String AQUAVIS_NETWORK_FILE_PROPERTY             = "emfac.2011.aquavis.network";
    /**
     * The property key for the location (full path) of the AquaVis output
     * intrazonal file.
     */
    public static final String AQUAVIS_INTRAZONAL_FILE_PROPERTY          = "emfac.2011.aquavis.intrazonal";
    /**
     * The property key for the location (full path) of the AquaVis output trips
     * file.
     */
    public static final String AQUAVIS_TRIPS_FILE_PROPERTY               = "emfac.2011.aquavis.trips";

    /**
     * Constructor specifying the resources used to build the properties.
     * 
     * @param firstResource
     *            The first properties resource.
     * 
     * @param additionalResources
     *            Any additional properties resources.
     * 
     * @throws IllegalArgumentException
     *             if any of the required properties is missing, or if they are
     *             typed incorrectly.
     */
    public Emfac2011Properties(String firstResource, String... additionalResources)
    {
        super(firstResource, additionalResources);
        System.out.println(firstResource);
        for (String s : additionalResources)
            System.out.println(s);
        for (String key : Arrays.asList(AREA_TYPE_PROPERTY, REGION_NAME_PROPERTY, AREAS_PROPERTY,
                SEASON_PROPERTY, YEAR_PROPERTY, EMFAC2011_INSTALLATION_DIR_PROPERTY,
                OUTPUT_DIR_PROPERTY, XLS_CONVERTER_PROGRAM_PROPERTY, AQUAVIS_NETWORK_FILE_PROPERTY,
                AQUAVIS_INTRAZONAL_FILE_PROPERTY, AQUAVIS_TRIPS_FILE_PROPERTY))
        {
            if (!hasKey(key))
                throw new IllegalArgumentException("Required property not found for key: " + key);
        }
        if (!(getProperty(AREAS_PROPERTY) instanceof Map))
            throw new IllegalArgumentException(
                    "Property value for "
                            + AREAS_PROPERTY
                            + " must be typed as a map between strings and a string list. "
                            + "Make sure the property entry has \"(MSLS)\" appended to the key, and the map appropriately typed. e.g.:\n\t"
                            + "emfac.2011.area(LS) = {El Dorado (LT):[El],Placer (LT):[Pl]}");
    }
}
