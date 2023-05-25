package org.sandag.abm.reporting.emfac2011;

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
 * Modified by Wu.Sun@sanag.org 1/28/2014
 */
public class Emfac2011Properties
        extends PropertyDeluxe
{
    //database properties
	public static final String REPORTS_DATABASE_IPADDRESS_PROPERTY = "reports.database.ipaddress";
	public static final String REPORTS_DATABASE_PORT_PROPERTY = "reports.database.port";
	public static final String REPORTS_DATABASE_NAME_PROPERTY = "reports.database.name";
	public static final String REPORTS_DATABASE_USERNAME_PROPERTY = "reports.database.username";
	public static final String REPORTS_DATABASE_PASSWORD_PROPERTY = "reports.database.password";
	public static final String REPORTS_DATABASE_INSTANCE_PROPERTY = "reports.database.instance";	

	//San Diego Emfac2011 properties
    public static final String AREA_TYPE_PROPERTY                        = "emfac.2011.area.type";
    public static final String REGION_NAME_PROPERTY                      = "emfac.2011.region.name";
    public static final String AREAS_PROPERTY                            = "emfac.2011.area";
    public static final String SEASON_PROPERTY                           = "emfac.2011.season";
    public static final String YEAR_PROPERTY                             = "emfac.2011.year";
    public static final String EMFAC2011_INSTALLATION_DIR_PROPERTY       = "emfac.2011.installation.dir";
    public static final String OUTPUT_DIR_PROPERTY                       = "emfac.2011.output.dir";
    public static final String AQUAVIS_INTRAZONAL_FILE_PROPERTY          = "emfac.2011.aquavis.intrazonal";
    
	// Aquavis table creation templates
	public static final String CREATE_AQUAVIS_NETWORK_TEMPLATE_PROPERTY = "aquavis.network.sql.template";
	public static final String CREATE_AQUAVIS_TRIPS_TEMPLATE_PROPERTY = "aquavis.trips.sql.template";
	public static final String CREATE_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY = "aquavis.intrazonal.sql.template";

	// Aquavis table query templates
	public static final String QUERY_AQUAVIS_NETWORK_TEMPLATE_PROPERTY = "aquavis.network.query.template";
	public static final String QUERY_AQUAVIS_TRIPS_TEMPLATE_PROPERTY = "aquavis.trips.query.template";
	public static final String QUERY_AQUAVIS_INTRAZONAL_TEMPLATE_PROPERTY = "aquavis.intrazonal.query.template";

	// inputs, outputs, and tokens
	public static final String AQUAVIS_TEMPLATE_SCENARIOID_TOKEN_PROPERTY= "aquavis.template.scenarioId.token";
	public static final String SCENARIO_ID = "scenario.id";
	public static final String VEHICLE_CODE_MAPPING_FILE_PROPERTY = "emfac.2011.to.sandag.vehicle.code.mapping.file";
	
	// switch if EMFAC is executed
	public static final String EXECUTE_EMFAC="execute.emfac";

    /**
     * The property key for the boolean indicating if the (default) EMFAC
     * tNCVehicle fractions should be preserved in the EMFAC input file (value is
     * {@code true}), or if the model tNCVehicle fractions should be used (value is
     * {@code false}).
     */
    public static final String PRESERVE_EMFAC_VEHICLE_FRACTIONS_PROPERTY = "emfac.2011.preserve.emfac.vehicle.fractions";
    /**
     * The property key for the boolean indicating whether or not the model
     * (travel demand, not EMFAC) VMT includes totals for non-mutable tNCVehicle
     * types. If it does, then the VMT will be scaled before adjusting the EMFAC
     * input file.
     */
    public static final String MODEL_VMT_INCLUDES_NON_MUTABLES_PROPERTY  = "emfac.2011.model.vmt.includes.non.mutable.vehicles";

    /**
     * The property key for the location of the xls converter program. This
     * program converts the malformed EMFAC2011 reference files to a (strictly)
     * valid format, which can then be used by the rest of the model.
     */
    public static final String XLS_CONVERTER_PROGRAM_PROPERTY            = "emfac.2011.xls.converter.program";
    
    /**
     * The property key for the location (full path) of the AquaVis output
     * intrazonal file.
     */

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
    public Emfac2011Properties(String firstResource)
    {
        super(firstResource);
        System.out.println("first property="+firstResource);
    }
}
