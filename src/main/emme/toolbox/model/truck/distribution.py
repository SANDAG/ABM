#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// model/truck/distribution.py                                           ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# Runs the truck distribution step. Distributes truck trips with congested
# skims and splits by time of day.
# The distribution is based on the mid-day travel time for the "generic"
# truck skim "mfMD_TRK_TIME". Applies truck toll diversion model with
# toll and non-toll skims.
#
# Inputs:
#    input_directory: source directory for input files
#    num_processors: Number of processors to use, either as a number or "MAX-#"
#    scenario: traffic scenario to use for reference zone system
#
# Files referenced:
#    Note: YEAR is replaced by truck.FFyear in the conf/sandag_abm.properties file
#    input/TruckTripRates.csv
#    input/mgra13_based_inputYEAR.csv
#    input/specialGenerators.csv
#
# Matrix inputs:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    moTRKL_PROD, moTRKM_PROD, moTRKH_PROD, moTRKEI_PROD, moTRKIE_PROD
#    mdTRKL_ATTR, mdTRKM_ATTR, mdTRKH_ATTR, mdTRKEI_ATTR, mdTRKIE_ATTR
#    mfTRKEE_DEMAND
#    mfMD_TRK_TIME
#    mfpp_TRKLGP_TIME, mfpp_TRKLTOLL_TIME, mfpp_TRKLTOLL_TOLLCOST
#    mfpp_TRKMGP_TIME, mfpp_TRKMTOLL_TIME, mfpp_TRKMTOLL_TOLLCOST
#    mfpp_TRKHGP_TIME, mfpp_TRKHTOLL_TIME, mfpp_TRKHTOLL_TOLLCOST
#
# Matrix intermediates (only used internally):
#    mfTRKEI_FRICTION, mfTRKIE_FRICTION, mfTRKL_FRICTION, mfTRKM_FRICTION, mfTRKH_FRICTION
#
# Matrix results:
#    Note: pp is time period, one of EA, AM, MD, PM, EV
#    mfpp_TRKLGP_VEH, mfpp_TRKMGP_VEH, mfpp_TRKHGP_VEH
#    mfpp_TRKLTOLL_VEH, mfpp_TRKMTOLL_VEH, mfpp_TRKHTOLL_VEH
#
# Script example:
"""
    import os
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    input_dir = os.path.join(main_directory, "input")
    num_processors = "MAX-1"
    base_scenario = modeller.scenario
    distribution = modeller.tool("sandag.model.truck.distribution")
    distribution(input_dir, num_processors, base_scenario)
"""


TOOLBOX_ORDER = 43

import traceback as _traceback
import pandas as pd
import numpy as np
import os

import inro.modeller as _m


gen_utils = _m.Modeller().module('sandag.utilities.general')
dem_utils = _m.Modeller().module('sandag.utilities.demand')


class TruckModel(_m.Tool(), gen_utils.Snapshot):

    input_directory = _m.Attribute(str)
    num_processors = _m.Attribute(str)

    tool_run_msg = ""

    @_m.method(return_type=_m.UnicodeType)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.input_directory = os.path.join(os.path.dirname(project_dir), "input")
        self.num_processors = "MAX-1"
        self.attributes = ["input_directory", "num_processors"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Truck distribution"
        pb.description = """
<div style="text-align:left">
    Distributes truck trips with congested skims and splits by time of day.
    The distribution is based on the mid-day travel time for the "generic" truck
    skim "mfMD_TRK_TIME".
    <br>
    Applies truck toll diversion model with toll and non-toll skims,
    and generates truck vehicle trips.
    <br>
    Note that the truck vehicle trips must be converted to PCE values by the Import auto
    demand tool and stored in matrices without the _VEH ending for the auto assignment.
</div>
        """
        pb.branding_text = "- SANDAG - Model - Truck"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('input_directory', 'directory',
                           title='Select input directory')
        dem_utils.add_select_processors("num_processors", pb, self)
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.input_directory, self.num_processors, scenario)
            run_msg = "Truck trip distribution complete."
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace('Truck distribution')
    def __call__(self, input_directory, num_processors, scenario):
        attributes = {
            "input_directory": input_directory,
            "num_processors": num_processors
        }
        gen_utils.log_snapshot("Truck distribution", str(self), attributes)
        self.scenario = scenario
        self.num_processors = num_processors

        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(
            os.path.join(os.path.dirname(input_directory), "conf", "sandag_abm.properties"))

        with _m.logbook_trace('Daily demand matrices'):
            coefficents = [0.045, 0.03, 0.03, 0.03, 0.03]
            truck_list = ['L', 'M', 'H', 'IE', 'EI']
            # distribution based on the "generic" truck MD time only
            time_skim = scenario.emmebank.matrix('mf"MD_TRK_TIME"')
            for truck_type, coeff in zip(truck_list, coefficents):
                with _m.logbook_trace('Create %s daily demand matrix' % truck_type):
                    self.calc_friction_factors(truck_type, time_skim, coeff)
                    self.matrix_balancing(truck_type)

        self.split_external_demand()
        self.split_into_time_of_day()
        self.toll_diversion()

        with _m.logbook_trace('Reduce matrix precision'):
            precision = props['RunModel.MatrixPrecision']
            matrices = []
            for t, pce in [('L', 1.3), ('M', 1.5), ('H', 2.5)]:
                for p in ['EA', 'AM', 'MD', 'PM', 'EV']:
                    matrices.append('mf%s_TRK%sGP_VEH' % (p, t))
                    matrices.append('mf%s_TRK%sTOLL_VEH' % (p, t))
            dem_utils.reduce_matrix_precision(matrices, precision, num_processors, scenario)

    @_m.logbook_trace('Create friction factors matrix')
    def calc_friction_factors(self, truck_type, impedance, coeff):
        matrix_calc = dem_utils.MatrixCalculator(self.scenario, self.num_processors)
        matrix_calc.run_single('mfTRK%s_FRICTION' % truck_type,
            'exp(-%s*%s)' % (coeff, impedance.named_id))
        return

    def matrix_balancing(self, truck_type):
        matrix_calc = dem_utils.MatrixCalculator(self.scenario, self.num_processors)
        emmebank = self.scenario.emmebank
        with _m.logbook_trace('Matrix balancing for %s' % truck_type):
            if truck_type == 'IE':
                with gen_utils.temp_matrices(emmebank, "DESTINATION") as (temp_md,):
                    temp_md.name = 'TRKIE_ROWTOTAL'
                    matrix_calc.add('md"TRKIE_ROWTOTAL"', 'mf"TRKIE_FRICTION"', aggregation={"origins": "+", "destinations": None})
                    matrix_calc.add('mf"TRKIE_DEMAND"', 'mf"TRKIE_FRICTION" * md"TRKIE_ATTR" / md"TRKIE_ROWTOTAL"',
                        constraint=['md"TRKIE_ROWTOTAL"', 0, 0, "EXCLUDE"])
                    matrix_calc.run()

            elif truck_type == 'EI':
                with gen_utils.temp_matrices(emmebank, "ORIGIN") as (temp_mo,):
                    temp_mo.name = 'TRKEI_COLTOTAL'
                    matrix_calc.add('mo"TRKEI_COLTOTAL"', 'mf"TRKEI_FRICTION"', aggregation={"origins": None, "destinations": "+"})
                    matrix_calc.add('mf"TRKEI_DEMAND"', 'mf"TRKEI_FRICTION" * mo"TRKEI_PROD" / mo"TRKEI_COLTOTAL"',
                        constraint=['mo"TRKEI_COLTOTAL"', 0, 0, "EXCLUDE"])
                    matrix_calc.run()
            else:
                matrix_balancing = _m.Modeller().tool(
                    'inro.emme.matrix_calculation.matrix_balancing')
                spec = {
                    "type": "MATRIX_BALANCING",
                    "od_values_to_balance": 'mf"TRK%s_FRICTION"' % truck_type,
                    "origin_totals": 'mo"TRK%s_PROD"' % truck_type,
                    "destination_totals": 'md"TRK%s_ATTR"' % truck_type,
                    "results": {
                        "od_balanced_values": 'mf"TRK%s_DEMAND"' % truck_type,
                    },
                    "max_iterations": 100,
                    "max_relative_error": 0.01
                }
                matrix_balancing(spec, self.scenario)

    @_m.logbook_trace('Split cross-regional demand by truck type')
    def split_external_demand(self):
        matrix_calc = dem_utils.MatrixCalculator(self.scenario, self.num_processors)

        truck_types = ['L', 'M', 'H']
        truck_share = [0.307, 0.155, 0.538]
        for t_type, share in zip(truck_types, truck_share):
            matrix_calc.add('mf"TRK%s_DEMAND"' % (t_type),
                '%s * (mf"TRKEI_DEMAND" + mf"TRKIE_DEMAND" + mf"TRKEE_DEMAND")' % (share))
            # Set intrazonal truck trips to 0
            matrix_calc.add('mf"TRK%s_DEMAND"' % (t_type), 'mf"TRK%s_DEMAND" * (p.ne.q)' % (t_type))
            matrix_calc.run()

    @_m.logbook_trace('Distribute daily demand into time of day')
    def split_into_time_of_day(self):
        matrix_calc = dem_utils.MatrixCalculator(self.scenario, self.num_processors)
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        time_share = [0.1018, 0.1698, 0.4284, 0.1543, 0.1457]
        border_time_share = [0.0188, 0.1812, 0.4629, 0.2310, 0.1061]
        border_correction = [bs/s for bs, s in zip(border_time_share, time_share)]

        truck_types = ['L', 'M', 'H']
        truck_names = {"L": "light trucks", "M": "medium trucks", "H": "heavy trucks"}

        for period, share, border_corr in zip(periods, time_share, border_correction):
            for t in truck_types:
                with matrix_calc.trace_run('Calculate %s demand matrix for %s' % (period, truck_names[t])):
                    tod_demand = 'mf"%s_TRK%s"' % (period, t)
                    matrix_calc.add(tod_demand, 'mf"TRK%s_DEMAND"' % (t))
                    matrix_calc.add(tod_demand, '%s_TRK%s * %s' % (period, t, share))
                    matrix_calc.add(tod_demand, 'mf%s_TRK%s * %s' % (period, t, border_corr),
                        {"origins": "1-5", "destinations": "1-9999"})
                    matrix_calc.add(tod_demand, 'mf%s_TRK%s * %s' % (period, t, border_corr),
                        {"origins": "1-9999", "destinations": "1-5"})

    @_m.logbook_trace('Toll diversion')
    def toll_diversion(self):
        matrix_calc = dem_utils.MatrixCalculator(self.scenario, self.num_processors)
        nest_factor = 10
        vot = 0.02 # cent/min
        periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        truck_types = ['L', 'M', 'H']
        truck_toll_factors = [1, 1.03, 2.33]

        for period in periods:
            for truck, toll_factor in zip(truck_types, truck_toll_factors):
                with matrix_calc.trace_run('Toll diversion for period %s, truck type %s' % (period, truck) ):
                    # Define the utility expression
                    utility = """
                    (
                        (mf"%(p)s_TRK%(t)sGP_TIME" - mf"%(p)s_TRK%(t)sTOLL_TIME")
                        - %(vot)s * mf"%(p)s_TRK%(t)sTOLL_TOLLCOST" * %(t_fact)s
                    )
                    / %(n_fact)s
                     """ % {
                        'p': period,
                        't': truck,
                        'vot': vot,
                        't_fact': toll_factor,
                        'n_fact': nest_factor
                    }
                    # If there is no toll probability of using toll is 0
                    matrix_calc.add('mf"%s_TRK%sTOLL_VEH"' % (period, truck), '0')
                    # If there is a non-zero toll value compute the share of
                    # toll-available passengers using the utility expression defined earlier
                    matrix_calc.add('mf"%s_TRK%sTOLL_VEH"' % (period, truck),
                        'mf"%(p)s_TRK%(t)s" * (1/(1 + exp(- %(u)s)))' % {'p': period, 't': truck, 'u': utility},
                        ['mf"%s_TRK%sTOLL_TOLLCOST"' % (period, truck), 0, 0 , "EXCLUDE"])
                    # if non-toll path is not available (GP time=0), set all demand to toll
                    matrix_calc.add('mf"%s_TRK%sTOLL_VEH"' % (period, truck),
                                    'mf"%(p)s_TRK%(t)s"'  % {'p': period, 't': truck},
                                    ['mf"%(p)s_TRK%(t)sGP_TIME"' % {'p': period, 't': truck}, 0, 0 , "INCLUDE"])
                    # Compute the truck demand for non toll
                    matrix_calc.add('mf"%s_TRK%sGP_VEH"' % (period, truck),
                        '(mf"%(p)s_TRK%(t)s" - mf"%(p)s_TRK%(t)sTOLL_VEH").max.0' % {'p': period, 't': truck})
