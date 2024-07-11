#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// initialize_matrices.py                                                ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
#
# Coordinates the initialization of all matrices.
# The matrix names are listed for each of the model components / steps,
# and the matrix IDs are assigned consistently from the set of matrices.
# In each of the model steps the matrices are only referenced by name,
# never by ID.
#
#
# Inputs:
#    components: A list of the model components / steps for which to initialize matrices
#                One or more of "traffic_demand", "transit_demand",
#                "traffic_skims", "transit_skims",  "external_internal_model",
#                "external_external_model", "truck_model", "commercial_vehicle_model"
#    periods: A list of periods for which to initialize matrices, "EA", "AM", "MD", "PM", "EV"
#    scenario: scenario to use for reference zone system and the emmebank in which
#              the matrices will be created
#
# Script example:
"""
    import os
    import inro.emme.database.emmebank as _eb
    modeller = inro.modeller.Modeller()
    main_directory = os.path.dirname(os.path.dirname(modeller.desktop.project.path))
    main_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    transit_emmebank = _eb.Emmebank(os.path.join(main_directory, "emme_project", "Database", "emmebank"))
    periods = ["EA", "AM", "MD", "PM", "EV"]
    traffic_components = [
        "traffic_demand", "traffic_skims", "external_internal_model",
        "external_external_model", "truck_model", "commercial_vehicle_model"]
    transit_components = ["transit_demand", "transit_skims"]
    base_scenario = main_emmebank.scenario(100)
    transit_scenario = transit_emmebank.scenario(100)
    initialize_matrices = modeller.tool("sandag.initialize.initialize_matrices")
    # Create / initialize matrices in the base, traffic emmebank
    initialize_matrices(traffic_components, periods, base_scenario)
    # Create / initialize matrices in the transit emmebank
    initialize_matrices(transit_components, periods, transit_scenario)
"""


TOOLBOX_ORDER = 9


import inro.modeller as _m
import traceback as _traceback

gen_utils = _m.Modeller().module("sandag.utilities.general")


class Initialize(_m.Tool(), gen_utils.Snapshot):

    components = _m.Attribute(_m.ListType)
    periods = _m.Attribute(_m.ListType)
    delete_all_existing = _m.Attribute(bool)

    tool_run_msg = ""

    def __init__(self):
        self._all_components = [
            "traffic_demand",
            "transit_demand",
            "traffic_skims",
            "transit_skims",
            "external_internal_model",
            "external_external_model",
            "truck_model",
            "commercial_vehicle_model",
        ]
        self._all_periods = ['EA', 'AM', 'MD', 'PM', 'EV']
        self.components = self._all_components[:]
        self.periods = self._all_periods[:]
        self.attributes = ["components", "periods", "delete_all_existing"]
        self._matrices = {}
        self._count = {}

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Initialize matrices"
        pb.description = """Creates and initializes the required matrices
            for the selected components / sub-models.
            Includes all components by default."""
        pb.branding_text = "- SANDAG"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select("components", keyvalues=[(k,k) for k in self._all_components],
            title="Select components:")
        pb.add_select("periods", keyvalues=[(k,k) for k in self._all_periods],
            title="Select periods:")
        pb.add_checkbox("delete_all_existing", label="Delete all existing matrices")
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.components, self.periods, scenario, self.delete_all_existing)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Create and initialize matrices", save_arguments=True)
    def __call__(self, components, periods, scenario, delete_all_existing=False):
        attributes = {
            "components": components,
            "periods": periods,
            "delete_all_existing": delete_all_existing
        }
        gen_utils.log_snapshot("Initialize matrices", str(self), attributes)

        self.scenario = scenario
        emmebank = scenario.emmebank
        self._create_matrix_tool = _m.Modeller().tool(
            "inro.emme.data.matrix.create_matrix")
        if components == "all":
            components = self._all_components[:]
        if periods == "all":
            periods = self._all_periods[:]
        if delete_all_existing:
            with _m.logbook_trace("Delete all existing matrices"):
                for matrix in emmebank.matrices():
                    emmebank.delete_matrix(matrix)
        self.generate_matrix_list(self.scenario)
        matrices = []
        for component in components:
            matrices.extend(self.create_matrices(component, periods))
        # Note: matrix is also created in import_network
        self._create_matrix_tool("ms1", "zero", "zero", scenario=self.scenario, overwrite=True)
        return matrices

    def generate_matrix_list(self, scenario):
        self._matrices = dict(
            (name, dict((k, []) for k in self._all_periods + ["ALL"]))
            for name in self._all_components)
        self._count = {"ms": 2, "md": 100, "mo": 100, "mf": 100}

        for component in self._all_components:
            fcn = getattr(self, component)
            fcn()
        # check dimensions can fit full set of matrices
        type_names = [
            ('mf', 'full_matrices'),
            ('mo', 'origin_matrices'),
            ('md', 'destination_matrices'),
            ('ms', 'scalar_matrices')]
        dims = scenario.emmebank.dimensions
        for prefix, name in type_names:
            if self._count[prefix] > dims[name]:
                raise Exception("emmebank capacity error, increase %s to at least %s" % (name, self._count[prefix]))

    def traffic_demand(self):
        tmplt_matrices = [
            ("SOV_NT_L",  "SOV non-transponder demand low VOT"),
            ("SOV_TR_L",  "SOV transponder demand low VOT"),
            ("HOV2_L",    "HOV2 demand low VOT"),
            ("HOV3_L",    "HOV3+ demand low VOT"),
            ("SOV_NT_M",  "SOV non-transponder demand medium VOT"),
            ("SOV_TR_M",  "SOV transponder demand medium VOT"),
            ("HOV2_M",    "HOV2 demand medium VOT"),
            ("HOV3_M",    "HOV3+ demand medium VOT"),
            ("SOV_NT_H", "SOV non-transponder demand high VOT"),
            ("SOV_TR_H",  "SOV transponder demand high VOT"),
            ("HOV2_H",    "HOV2 demand high VOT"),
            ("HOV3_H",    "HOV3+ demand high VOT"),
            ("TRK_H",     "Truck Heavy PCE demand"),
            ("TRK_L",     "Truck Light PCE demand"),
            ("TRK_M",     "Truck Medium PCE demand"),
        ]
        for period in self._all_periods:
            self.add_matrices("traffic_demand", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def transit_demand(self):
        tmplt_matrices = [
            ("LOC",  "local bus demand"),
            ("PRM", "Premium modes demand"),
            ("MIX",  "all modes xfer pen demand"),
        ]
        for period in self._all_periods:
            for a_name in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN", "TNCOUT", "TNCIN"]:
                self.add_matrices("transit_demand", period,
                    [("mf", "%s_%s_%s" % (period, a_name, name), "%s %s access %s" % (period, a_name, desc))
                     for name, desc in tmplt_matrices])

    def traffic_skims(self):
        tp_desc = {"TR": "transponder", "NT": "non-transponder"}
        vot_desc = {"L": "low", "M": "medium", "H": "high"}
        truck_desc = {"L": "light", "M": "medium", "H": "heavy"}

        sov_tmplt_matrices = [
            ("TIME",        "SOV %s travel time"),
            ("DIST",        "SOV %s distance"),
            ("REL",         "SOV %s reliability skim"),
            ("TOLLCOST",    "SOV %s toll cost $0.01"),
            ("TOLLDIST",    "SOV %s distance on toll facility"),
        ]
        hov_tmplt_matrices = [
            ("TIME",        "HOV%s travel time"),
            ("DIST",        "HOV%s distance"),
            ("REL",         "HOV%s reliability skim"),
            ("TOLLCOST",    "HOV%s toll cost $0.01"),
            ("TOLLDIST",    "HOV%s distance on toll facility"),
            ("HOVDIST",     "HOV%s HOV distance on HOV facility")
        ]
        truck_tmplt_matrices = [
            ("TIME",     "Truck %s travel time"),
            ("DIST",     "Truck %s distance"),
            ("TOLLCOST", "Truck %s toll cost $0.01")
        ]
        for period in self._all_periods:
            for vot_type in "L", "M", "H":
                for tp_type in "NT", "TR":
                    cls_name = "SOV_" + tp_type + "_" + vot_type
                    cls_desc = tp_desc[tp_type] + " " + vot_desc[vot_type] + " VOT"
                    self.add_matrices("traffic_skims", period,
                        [("mf", cls_name + "_" + name + "__" + period, period + " " + desc % cls_desc) for name, desc in sov_tmplt_matrices])
                for hov_type in "2", "3":
                    cls_name = "HOV" + hov_type + "_" + vot_type
                    cls_desc = hov_type + " " + vot_desc[vot_type] + " VOT"
                    self.add_matrices("traffic_skims", period,
                        [("mf", cls_name + "_" + name + "__" + period,
                            period + " " + desc % cls_desc)
                         for name, desc in hov_tmplt_matrices])
            for truck_type in "L", "M", "H":
                cls_name = "TRK" + "_" + truck_type
                cls_desc = truck_desc[truck_type]
                self.add_matrices("traffic_skims", period,
                    [("mf", cls_name + "_" + name + "__" + period,
                      period + " " + desc % cls_desc)
                     for name, desc in truck_tmplt_matrices])

        self.add_matrices("traffic_skims", "MD",
            [("mf", "TRK_TIME__MD", "MD Truck generic travel time")])

    def transit_skims(self):
        tmplt_matrices_mix = [
            # ("GENCOST",    "total impedance"),
            ("FIRSTWAIT",  "first wait time"),
            ("XFERWAIT",   "transfer wait time"),
            ("TOTALWAIT",  "total wait time"),
            ("FARE",       "fare"),
            ("XFERS",      "num transfers"),
            ("ACC",    "access time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGR",    "egress time"),
            ("TOTALWALK",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("BUSIVTT",    "local bus in-vehicle time"),
            ("LRTIVTT",    "LRT in-vehicle time"),
            ("CMRIVTT",    "Rail in-vehicle time"),
            ("EXPIVTT",    "Express in-vehicle time"),
            ("LTDEXPIVTT", "Ltd exp bus in-vehicle time"),
            ("BRTIVTT", "BRT in-vehicle time"),
            # ("BRTREDIVTT", "BRT red in-vehicle time"),
            # ("BRTYELIVTT", "BRT yellow in-vehicle time"),
            ("TIER1IVTT",    "Tier1 in-vehicle time"),
            # ("BUSDIST",    "Bus IV distance"),
            # ("LRTDIST",    "LRT IV distance"),
            # ("CMRDIST",    "Rail IV distance"),
            # ("EXPDIST",    "Express and Ltd IV distance"),
            # ("BRTDIST",    "BRT red and yel IV distance"),
            # ("TIER1DIST",    "Tier1 distance"),
            # ("TOTDIST",    "Total transit distance")
        ]
        tmplt_matrices_prm = [
            # ("GENCOST",    "total impedance"),
            ("FIRSTWAIT",  "first wait time"),
            ("XFERWAIT",   "transfer wait time"),
            ("TOTALWAIT",  "total wait time"),
            ("FARE",       "fare"),
            ("XFERS",      "num transfers"),
            ("ACC",    "access time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGR",    "egress time"),
            ("TOTALWALK",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("LRTIVTT",    "LRT in-vehicle time"),
            ("CMRIVTT",    "Rail in-vehicle time"),
            ("EXPIVTT",    "Express in-vehicle time"),
            ("LTDEXPIVTT", "Ltd exp bus in-vehicle time"),
            ("BRTIVTT", "BRT in-vehicle time"),
            # ("BRTREDIVTT", "BRT red in-vehicle time"),
            # ("BRTYELIVTT", "BRT yellow in-vehicle time"),
            ("TIER1IVTT",    "Tier1 in-vehicle time"),
            # ("BUSDIST",    "Bus IV distance"),
            # ("LRTDIST",    "LRT IV distance"),
            # ("CMRDIST",    "Rail IV distance"),
            # ("EXPDIST",    "Express and Ltd IV distance"),
            # ("BRTDIST",    "BRT red and yel IV distance"),
            # ("TIER1DIST",    "Tier1 distance"),
            # ("TOTDIST",    "Total transit distance")
        ]
        tmplt_matrices_loc = [
            # ("GENCOST",    "total impedance"),
            ("FIRSTWAIT",  "first wait time"),
            ("XFERWAIT",   "transfer wait time"),
            ("TOTALWAIT",  "total wait time"),
            ("FARE",       "fare"),
            ("XFERS",      "num transfers"),
            ("ACC",    "access time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGR",    "egress time"),
            ("TOTALWALK",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("BUSIVTT",    "local bus in-vehicle time"),
            # ("BUSDIST",    "Bus IV distance"),
            # ("LRTDIST",    "LRT IV distance"),
            # ("CMRDIST",    "Rail IV distance"),
            # ("EXPDIST",    "Express and Ltd IV distance"),
            # ("BRTDIST",    "BRT red and yel IV distance"),
            # ("TIER1DIST",    "Tier1 distance"),
            # ("TOTDIST",    "Total transit distance")
        ]
        skim_sets = [
            ("LOC",    "Local bus only"),
            ("PRM",   "Premium modes only"),
            ("MIX", "All w/ xfer pen")
        ]
        for period in self._all_periods:
            for amode in ["WALK", "PNROUT", "PNRIN", "KNROUT", "KNRIN"]:
                for set_name, set_desc in skim_sets:
                    if set_name == 'LOC':
                        self.add_matrices("transit_skims", period,
                            [("mf", amode + "_" + set_name + "_" + name + "__" + period,
                            period + " " + set_desc + ": " + desc)
                            for name, desc in tmplt_matrices_loc])

                    elif set_name == 'PRM':
                        self.add_matrices("transit_skims", period,
                        [("mf", amode + "_" + set_name + "_" + name + "__" + period,
                        period + " " + set_desc + ": " + desc)
                        for name, desc in tmplt_matrices_prm])

                    else:
                        self.add_matrices("transit_skims", period,
                        [("mf", amode + "_" + set_name + "_" + name + "__" + period,
                        period + " " + set_desc + ": " + desc)
                        for name, desc in tmplt_matrices_mix])

    def truck_model(self):
        tmplt_matrices = [
            ("TRKL",    "Truck Light"),
            ("TRKM",    "Truck Medium"),
            ("TRKH",    "Truck Heavy"),
            ("TRKEI",   "Truck external-internal"),
            ("TRKIE",   "Truck internal-external"),
        ]
        self.add_matrices("truck_model", "ALL",
                [("mo", name + '_PROD', desc + ' production')
                 for name, desc in tmplt_matrices])
        self.add_matrices("truck_model", "ALL",
                [("md", name + '_ATTR', desc + ' attraction')
                 for name, desc in tmplt_matrices])

        tmplt_matrices = [
            ("TRKEE_DEMAND",     "Truck total external-external demand"),
            ("TRKL_FRICTION",    "Truck Light friction factors"),
            ("TRKM_FRICTION",    "Truck Medium friction factors"),
            ("TRKH_FRICTION",    "Truck Heavy friction factors"),
            ("TRKIE_FRICTION",   "Truck internal-external friction factors"),
            ("TRKEI_FRICTION",   "Truck external-internal friction factors"),
            ("TRKL_DEMAND",      "Truck Light total demand"),
            ("TRKM_DEMAND",      "Truck Medium total demand"),
            ("TRKH_DEMAND",      "Truck Heavy total demand"),
            ("TRKIE_DEMAND",     "Truck internal-external total demand"),
            ("TRKEI_DEMAND",     "Truck external-internal total demand"),
        ]
        self.add_matrices("truck_model", "ALL",
                [("mf", name, desc) for name, desc in tmplt_matrices])

        # TODO: remove GP and TOLL matrices, no longer used
        tmplt_matrices = [
            ("TRK_L_VEH",            "Truck Light demand"),
            ("TRKLGP_VEH",      "Truck Light GP-only vehicle demand"),
            ("TRKLTOLL_VEH",    "Truck Light toll vehicle demand"),
            ("TRK_M_VEH",            "Truck Medium demand"),
            ("TRKMGP_VEH",      "Truck Medium GP-only vehicle demand"),
            ("TRKMTOLL_VEH",    "Truck Medium toll vehicle demand"),
            ("TRK_H_VEH",            "Truck Heavy demand"),
            ("TRKHGP_VEH",      "Truck Heavy GP-only vehicle demand"),
            ("TRKHTOLL_VEH",    "Truck Heavy toll vehicle demand"),
        ]
        for period in self._all_periods:
            self.add_matrices("truck_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def commercial_vehicle_model(self):
        # TODO : remove commercial vehicle matrices, no longer used
        tmplt_matrices = [
            ('mo', 'COMVEH_PROD',         'Commercial vehicle production'),
            ('md', 'COMVEH_ATTR',         'Commercial vehicle attraction'),
            ('mf', 'COMVEH_BLENDED_SKIM', 'Commercial vehicle blended skim'),
            ('mf', 'COMVEH_FRICTION',     'Commercial vehicle friction factors'),
            ('mf', 'COMVEH_TOTAL_DEMAND', 'Commercial vehicle total demand all periods'),
        ]
        self.add_matrices("commercial_vehicle_model", "ALL",
                [(ident, name, desc) for ident, name, desc in tmplt_matrices])

        tmplt_matrices = [
            ('COMVEH',    'Commerical vehicle total demand'),
            ('COMVEHGP',   'Commerical vehicle GP demand'),
            ('COMVEHTOLL', 'Commerical vehicle Toll demand'),
        ]
        for period in self._all_periods:
            self.add_matrices("commercial_vehicle_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def external_internal_model(self):
        tmplt_matrices = [
            ('SOVTOLL_EIWORK',  'US to SD SOV Work TOLL demand'),
            ('HOV2TOLL_EIWORK', 'US to SD HOV2 Work TOLL demand'),
            ('HOV3TOLL_EIWORK', 'US to SD HOV3 Work TOLL demand'),
            ('SOVGP_EIWORK',  'US to SD SOV Work GP demand'),
            ('HOV2HOV_EIWORK', 'US to SD HOV2 Work HOV demand'),
            ('HOV3HOV_EIWORK', 'US to SD HOV3 Work HOV demand'),
            ('SOVTOLL_EINONWORK',  'US to SD SOV Non-Work TOLL demand'),
            ('HOV2TOLL_EINONWORK', 'US to SD HOV2 Non-Work TOLL demand'),
            ('HOV3TOLL_EINONWORK', 'US to SD HOV3 Non-Work TOLL demand'),
            ('SOVGP_EINONWORK',  'US to SD SOV Non-Work GP demand'),
            ('HOV2HOV_EINONWORK', 'US to SD HOV2 Non-Work HOV demand'),
            ('HOV3HOV_EINONWORK', 'US to SD HOV3 Non-Work HOV demand'),
        ]
        for period in self._all_periods:
            self.add_matrices("external_internal_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def external_external_model(self):
        self.add_matrices("external_external_model", "ALL",
                [("mf", "ALL_TOTAL_EETRIPS",  "All periods Total for all modes external-external trips")])
        tmplt_matrices = [
            ('SOV_EETRIPS',   'SOV external-external demand'),
            ('HOV2_EETRIPS',  'HOV2 external-external demand'),
            ('HOV3_EETRIPS',  'HOV3 external-external demand'),
        ]
        for period in self._all_periods:
            self.add_matrices("external_external_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def add_matrices(self, component, period, matrices):
        for ident, name, desc in matrices:
            self._matrices[component][period].append([ident+str(self._count[ident]), name, desc])
            self._count[ident] += 1

    def create_matrices(self, component, periods):
        with _m.logbook_trace("Create matrices for component %s" % (component.replace("_", " "))):
            emmebank = self.scenario.emmebank
            matrices = []
            for period in periods + ["ALL"]:
                with _m.logbook_trace("For period %s" % (period)):
                    for ident, name, desc in self._matrices[component][period]:
                        existing_matrix = emmebank.matrix(name)
                        if existing_matrix and (existing_matrix.id != ident):
                            raise Exception("Matrix name conflict '%s', with id %s instead of %s. Delete all matrices first."
                                % (name, existing_matrix.id, ident))
                        matrices.append(self._create_matrix_tool(ident, name, desc, scenario=self.scenario, overwrite=True))
        return matrices

    def get_matrix_names(self, component, periods, scenario):
        self.generate_matrix_list(scenario)
        matrices = []
        for period in periods:
            matrices.extend([m[1] for m in self._matrices[component][period]])
        return matrices

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
