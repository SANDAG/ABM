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
        attributes = {"components": components, "periods": periods}
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
            ("SOVGPL",     "SOV GP-only demand LVOT"),
            ("SOVTOLLL",   "SOV toll demand LVOT"),
            ("HOV2GPL",    "HOV2 GP-only demand LVOT"),
            ("HOV2HOVL",   "HOV2 HOV-lane demand LVOT"),
            ("HOV2TOLLL",  "HOV2 toll demand LVOT"),
            ("HOV3GPL",    "HOV3+ GP-only demand LVOT"),
            ("HOV3HOVL",   "HOV3+ HOV-lane demand LVOT"),
            ("HOV3TOLLL",  "HOV3+ toll demand LVOT"),
            ("SOVGPM",     "SOV GP-only demand MVOT"),
            ("SOVTOLLM",   "SOV toll demand MVOT"),
            ("HOV2GPM",    "HOV2 GP-only demand MVOT"),
            ("HOV2HOVM",   "HOV2 HOV-lane demand MVOT"),
            ("HOV2TOLLM",  "HOV2 toll demand MVOT"),
            ("HOV3GPM",    "HOV3+ GP-only demand MVOT"),
            ("HOV3HOVM",   "HOV3+ HOV-lane demand MVOT"),
            ("HOV3TOLLM",  "HOV3+ toll demand MVOT"),
            ("SOVGPH",     "SOV GP-only demand HVOT"),
            ("SOVTOLLH",   "SOV toll demand HVOT"),
            ("HOV2GPH",    "HOV2 GP-only demand HVOT"),
            ("HOV2HOVH",   "HOV2 HOV-lane demand HVOT"),
            ("HOV2TOLLH",  "HOV2 toll demand HVOT"),
            ("HOV3GPH",    "HOV3+ GP-only demand HVOT"),
            ("HOV3HOVH",   "HOV3+ HOV-lane demand HVOT"),
            ("HOV3TOLLH",  "HOV3+ toll demand HVOT"),
            ("TRKHGP",    "Truck Heavy GP-only PCE demand"),
            ("TRKHTOLL",  "Truck Heavy toll PCE demand"),
            ("TRKLGP",    "Truck Light GP-only PCE demand"),
            ("TRKLTOLL",  "Truck Light toll PCE demand"),
            ("TRKMGP",    "Truck Medium GP-only PCE demand"),
            ("TRKMTOLL",  "Truck Medium toll PCE demand"),
        ]
        for period in self._all_periods:
            self.add_matrices("traffic_demand", period,
                [("mf", period + "_" + name, period + " " + desc) 
                 for name, desc in tmplt_matrices])

    def transit_demand(self):
        tmplt_matrices = [
            ("BUS",  "local bus demand"),
            ("ALL",  "all modes demand"),
            ("ALLPEN",  "all modes xfer pen demand"),
        ]
        for period in self._all_periods:
            for a_name in ["WLK", "PNR", "KNR"]:
                self.add_matrices("transit_demand", period,
                    [("mf", "%s_%s%s" % (period, a_name, name), "%s %s access %s" % (period, a_name, desc)) 
                     for name, desc in tmplt_matrices])

    def traffic_skims(self):
        tmplt_matrices = [
            ("SOVGPL_GENCOST",     "SOV LVOT GP total generalized cost"),
            ("SOVGPL_TIME",        "SOV LVOT GP travel time"),
            ("SOVGPL_DIST",        "SOV LVOT GP distance"),
            ("SOVGPL_REL",         "SOV LVOT GP reliability skim"),
            ("SOVTOLLL_GENCOST",   "SOV LVOT Toll total generalized cost"),
            ("SOVTOLLL_TIME",      "SOV LVOT Toll travel time"),
            ("SOVTOLLL_DIST",      "SOV LVOT Toll distance"),
            ("SOVTOLLL_MLCOST",    "SOV LVOT Toll managed lane cost $0.01"),
            ("SOVTOLLL_TOLLCOST",  "SOV LVOT Toll toll cost $0.01"),
            ("SOVTOLLL_TOLLDIST",  "SOV LVOT Toll distance on toll facility"),
            ("SOVTOLLL_REL",       "SOV LVOT GP reliability skim"),
            ("HOV2HOVL_GENCOST",   "HOV2 LVOT HOV total generalized cost"),
            ("HOV2HOVL_TIME",      "HOV2 LVOT HOV travel time"),
            ("HOV2HOVL_DIST",      "HOV2 LVOT HOV distance"),
            ("HOV2HOVL_HOVDIST",   "HOV2 LVOT HOV distance on HOV facility"),
            ("HOV2HOVL_REL",       "HOV2 LVOT HOV reliability skim"),
            ("HOV2TOLLL_GENCOST",  "HOV2 LVOT Toll total generalized cost"),
            ("HOV2TOLLL_TIME",     "HOV2 LVOT Toll travel time"),
            ("HOV2TOLLL_DIST",     "HOV2 LVOT Toll distance"),
            ("HOV2TOLLL_MLCOST",   "HOV2 LVOT Toll managed lane cost $0.01"),
            ("HOV2TOLLL_TOLLCOST", "HOV2 LVOT Toll toll cost $0.01"),
            ("HOV2TOLLL_TOLLDIST", "HOV2 LVOT Toll distance on toll facility"),
            ("HOV2TOLLL_REL",      "HOV2 LVOT Toll reliability skim"),
            ("HOV3HOVL_GENCOST",   "HOV3+ LVOT HOV total generalized cost"),
            ("HOV3HOVL_TIME",      "HOV3+ LVOT HOV travel time"),
            ("HOV3HOVL_DIST",      "HOV3+ LVOT HOV distance"),
            ("HOV3HOVL_HOVDIST",   "HOV3+ LVOT HOV distance on HOV facility"),
            ("HOV3HOVL_REL",       "HOV3+ LVOT HOV reliability skim"),
            ("HOV3TOLLL_GENCOST",  "HOV3+ LVOT Toll total generalized cost"),
            ("HOV3TOLLL_TIME",     "HOV3+ LVOT Toll travel time"),
            ("HOV3TOLLL_DIST",     "HOV3+ LVOT Toll distance"),
            ("HOV3TOLLL_MLCOST",   "HOV3+ LVOT Toll managed lane cost $0.01"),
            ("HOV3TOLLL_TOLLCOST", "HOV3+ LVOT Toll toll cost $0.01"),
            ("HOV3TOLLL_TOLLDIST", "HOV3+ LVOT Toll distance on toll facility"),
            ("HOV3TOLLL_REL",      "HOV3+ LVOT Toll reliability skim"),
            ("SOVGPM_GENCOST",     "SOV MVOT GP total generalized cost"),
            ("SOVGPM_TIME",        "SOV MVOT GP travel time"),
            ("SOVGPM_DIST",        "SOV MVOT GP distance"),
            ("SOVGPM_REL",         "SOV MVOT GP reliability skim"),
            ("SOVTOLLM_GENCOST",   "SOV MVOT Toll total generalized cost"),
            ("SOVTOLLM_TIME",      "SOV MVOT Toll travel time"),
            ("SOVTOLLM_DIST",      "SOV MVOT Toll distance"),
            ("SOVTOLLM_MLCOST",    "SOV MVOT Toll managed lane cost $0.01"),
            ("SOVTOLLM_TOLLCOST",  "SOV MVOT Toll toll cost $0.01"),
            ("SOVTOLLM_TOLLDIST",  "SOV MVOT Toll distance on toll facility"),
            ("SOVTOLLM_REL",       "SOV MVOT Toll reliability skim"),
            ("HOV2HOVM_GENCOST",   "HOV2 MVOT HOV total generalized cost"),
            ("HOV2HOVM_TIME",      "HOV2 MVOT HOV travel time"),
            ("HOV2HOVM_DIST",      "HOV2 MVOT HOV distance"),
            ("HOV2HOVM_HOVDIST",   "HOV2 MVOT HOV distance on HOV facility"),
            ("HOV2HOVM_REL",       "HOV2 MVOT HOV reliability skim"),
            ("HOV2TOLLM_GENCOST",  "HOV2 MVOT Toll total generalized cost"),
            ("HOV2TOLLM_TIME",     "HOV2 MVOT Toll travel time"),
            ("HOV2TOLLM_DIST",     "HOV2 MVOT Toll distance"),
            ("HOV2TOLLM_MLCOST",   "HOV2 MVOT Toll managed lane cost $0.01"),
            ("HOV2TOLLM_TOLLCOST", "HOV2 MVOT Toll toll cost $0.01"),
            ("HOV2TOLLM_TOLLDIST", "HOV2 MVOT Toll distance on toll facility"),
            ("HOV2TOLLM_REL",      "HOV2 MVOT Toll reliability skim"),
            ("HOV3HOVM_GENCOST",   "HOV3+ MVOT HOV total generalized cost"),
            ("HOV3HOVM_TIME",      "HOV3+ MVOT HOV travel time"),
            ("HOV3HOVM_DIST",      "HOV3+ MVOT HOV distance"),
            ("HOV3HOVM_HOVDIST",   "HOV3+ MVOT HOV distance on HOV facility"),
            ("HOV3HOVM_REL",       "HOV3+ MVOT HOV reliability skim"),
            ("HOV3TOLLM_GENCOST",  "HOV3+ MVOT Toll total generalized cost"),
            ("HOV3TOLLM_TIME",     "HOV3+ MVOT Toll travel time"),
            ("HOV3TOLLM_DIST",     "HOV3+ MVOT Toll distance"),
            ("HOV3TOLLM_MLCOST",   "HOV3+ MVOT Toll managed lane cost $0.01"),
            ("HOV3TOLLM_TOLLCOST", "HOV3+ MVOT Toll toll cost $0.01"),
            ("HOV3TOLLM_TOLLDIST", "HOV3+ MVOT Toll distance on toll facility"),
            ("HOV3TOLLM_REL",      "HOV3+ MVOT Toll reliability skim"),
            ("SOVGPH_GENCOST",     "SOV HVOT GP total generalized cost"),
            ("SOVGPH_TIME",        "SOV HVOT GP travel time"),
            ("SOVGPH_DIST",        "SOV HVOT GP distance"),
            ("SOVGPH_REL",         "SOV HVOT GP reliability skim"),
            ("SOVTOLLH_GENCOST",   "SOV HVOT Toll total generalized cost"),
            ("SOVTOLLH_TIME",      "SOV HVOT Toll travel time"),
            ("SOVTOLLH_DIST",      "SOV HVOT Toll distance"),
            ("SOVTOLLH_MLCOST",    "SOV HVOT Toll managed lane cost $0.01"),
            ("SOVTOLLH_TOLLCOST",  "SOV HVOT Toll toll cost $0.01"),
            ("SOVTOLLH_TOLLDIST",  "SOV HVOT Toll distance on toll facility"),
            ("SOVTOLLH_REL",       "SOV HVOT Toll reliability skim"),
            ("HOV2HOVH_GENCOST",   "HOV2 HVOT HOV total generalized cost"),
            ("HOV2HOVH_TIME",      "HOV2 HVOT HOV travel time"),
            ("HOV2HOVH_DIST",      "HOV2 HVOT HOV distance"),
            ("HOV2HOVH_HOVDIST",   "HOV2 HVOT HOV distance on HOV facility"),
            ("HOV2HOVH_REL",       "HOV2 HVOT HOV reliability skim"),
            ("HOV2TOLLH_GENCOST",  "HOV2 HVOT Toll total generalized cost"),
            ("HOV2TOLLH_TIME",     "HOV2 HVOT Toll travel time"),
            ("HOV2TOLLH_DIST",     "HOV2 HVOT Toll distance"),
            ("HOV2TOLLH_MLCOST",   "HOV2 HVOT Toll managed lane cost $0.01"),
            ("HOV2TOLLH_TOLLCOST", "HOV2 HVOT Toll toll cost $0.01"),
            ("HOV2TOLLH_TOLLDIST", "HOV2 HVOT Toll distance on toll facility"),
            ("HOV2TOLLH_REL",      "HOV2 HVOT Toll reliability skim"),
            ("HOV3HOVH_GENCOST",   "HOV3+ HVOT HOV total generalized cost"),
            ("HOV3HOVH_TIME",      "HOV3+ HVOT HOV travel time"),
            ("HOV3HOVH_DIST",      "HOV3+ HVOT HOV distance"),
            ("HOV3HOVH_HOVDIST",   "HOV3+ HVOT HOV distance on HOV facility"),
            ("HOV3HOVH_REL",       "HOV3+ HVOT HOV reliability skim"),
            ("HOV3TOLLH_GENCOST",  "HOV3+ HVOT Toll total generalized cost"),
            ("HOV3TOLLH_TIME",     "HOV3+ HVOT Toll travel time"),
            ("HOV3TOLLH_DIST",     "HOV3+ HVOT Toll distance"),
            ("HOV3TOLLH_MLCOST",   "HOV3+ HVOT Toll managed lane cost $0.01"),
            ("HOV3TOLLH_TOLLCOST", "HOV3+ HVOT Toll toll cost $0.01"),
            ("HOV3TOLLH_TOLLDIST", "HOV3+ HVOT Toll distance on toll facility"),
            ("HOV3TOLLH_REL",      "HOV3+ HVOT Toll reliability skim"),
            ("TRKHGP_GENCOST",    "Truck Heavy GP total generalized cost"),
            ("TRKHGP_TIME",       "Truck Heavy GP travel time"),
            ("TRKHGP_DIST",       "Truck Heavy GP distance"),
            ("TRKHTOLL_GENCOST",  "Truck Heavy Toll total generalized cost"),
            ("TRKHTOLL_TIME",     "Truck Heavy Toll travel time"),
            ("TRKHTOLL_DIST",     "Truck Heavy Toll distance"),
            ("TRKHTOLL_TOLLCOST", "Truck Heavy Toll toll cost $0.01"),
            ("TRKLGP_GENCOST",    "Truck Light GP total generalized cost"),
            ("TRKLGP_TIME",       "Truck Light GP travel time"),
            ("TRKLGP_DIST",       "Truck Light GP distance"),
            ("TRKLTOLL_GENCOST",  "Truck Light Toll total generalized cost"),
            ("TRKLTOLL_TIME",     "Truck Light Toll travel time"),
            ("TRKLTOLL_DIST",     "Truck Light Toll distance"),
            ("TRKLTOLL_TOLLCOST", "Truck Light Toll toll cost $0.01"),
            ("TRKMGP_GENCOST",    "Truck Medium GP total generalized cost"),
            ("TRKMGP_TIME",       "Truck Medium GP travel time"),
            ("TRKMGP_DIST",       "Truck Medium GP distance"),
            ("TRKMTOLL_GENCOST",  "Truck Medium Toll total generalized cost"),
            ("TRKMTOLL_TIME",     "Truck Medium Toll travel time"),
            ("TRKMTOLL_DIST",     "Truck Medium Toll distance"),
            ("TRKMTOLL_TOLLCOST", "Truck Medium Toll toll cost $0.01"),
        ]
        for period in self._all_periods:
            self.add_matrices("traffic_skims", period,
                [("mf", period + "_" + name, period + " " + desc) 
                 for name, desc in tmplt_matrices])

        tmplt_matrices = [
            ("MD_TRK_GENCOST",  "MD Truck generic total generalized cost"),
            ("MD_TRK_TIME",     "MD Truck generic travel time"),
            ("MD_TRK_DIST",     "MD Truck generic distance"),
            ("MD_TRK_MLCOST",   "MD Truck generic managed lane cost $0.01"),
            ("MD_TRK_TOLLCOST", "MD Truck generic toll cost $0.01"),
        ]
        self.add_matrices("traffic_skims", "MD",
            [("mf", name, desc) for name, desc in tmplt_matrices])

    def transit_skims(self):
        tmplt_matrices = [
            ("GENCOST",    "total impedance"),
            ("FIRSTWAIT",  "first wait time"),
            ("XFERWAIT",   "transfer wait time"),
            ("TOTALWAIT",  "total wait time"),
            ("FARE",       "fare"),
            ("XFERS",      "num transfers"),
            ("ACCWALK",    "access walk time"),
            ("XFERWALK",   "transfer walk time"),
            ("EGRWALK",    "egress walk time"),
            ("TOTALWALK",  "total walk time"),
            ("TOTALIVTT",  "in-vehicle time"),
            ("DWELLTIME",  "dwell time"),
            ("BUSIVTT",    "local bus in-vehicle time"),
            ("LRTIVTT",    "LRT in-vehicle time"),
            ("CMRIVTT",    "Rail in-vehicle time"),
            ("EXPIVTT",    "Express in-vehicle time"),
            ("LTDEXPIVTT", "Ltd exp bus in-vehicle time"),
            ("BRTREDIVTT", "BRT red in-vehicle time"),
            ("BRTYELIVTT", "BRT yellow in-vehicle time"),
            ("BUSDIST",    "Bus IV distance"),
            ("LRTDIST",    "LRT IV distance"),
            ("CMRDIST",    "Rail IV distance"),
            ("EXPDIST",    "Express and Ltd IV distance"),
            ("BRTDIST",    "BRT red and yel IV distance"),
        ]
        skim_sets = [
            ("BUS", "Local bus"), 
            ("ALL", "All modes"), 
            ("ALLPEN", "All w/ xfer pen")
        ]
        for period in self._all_periods:
            for set_name, set_desc in skim_sets:
                self.add_matrices("transit_skims", period,
                    [("mf", period + "_" + set_name + "_" + name, 
                      period + " " + set_desc + ": " + desc) 
                     for name, desc in tmplt_matrices])

    def truck_model(self):
        tmplt_matrices = [
            ("TRKL",    "Light truck"), 
            ("TRKM",    "Medium truck"), 
            ("TRKH",    "Heavy truck"), 
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
            ("TRKL_FRICTION",    "Light truck friction factors"), 
            ("TRKM_FRICTION",    "Medium truck friction factors"), 
            ("TRKH_FRICTION",    "Heavy truck friction factors"), 
            ("TRKIE_FRICTION",   "Truck internal-external friction factors"), 
            ("TRKEI_FRICTION",   "Truck external-internal friction factors"), 
            ("TRKL_DEMAND",      "Light truck total demand"), 
            ("TRKM_DEMAND",      "Medium truck total demand"), 
            ("TRKH_DEMAND",      "Heavy truck total demand"), 
            ("TRKIE_DEMAND",     "Truck internal-external total demand"), 
            ("TRKEI_DEMAND",     "Truck external-internal total demand"), 
        ]
        self.add_matrices("truck_model", "ALL",
                [("mf", name, desc) for name, desc in tmplt_matrices])

        tmplt_matrices = [
            ("TRKL",    "Light truck demand"), 
            ("TRKM",    "Medium truck demand"),
            ("TRKH",    "Heavy truck demand"), 
        ]
        for period in self._all_periods:
            self.add_matrices("truck_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def commercial_vehicle_model(self):
        tmplt_matrices = [
            ('mo', 'COMMVEH_PROD',         'Commercial vehicle production'),
            ('md', 'COMMVEH_ATTR',         'Commercial vehicle attraction'),
            ('mf', 'COMMVEH_BLENDED_SKIM', 'Commercial vehicle blended skim'),
            ('mf', 'COMMVEH_FRICTION',     'Commercial vehicle friction factors'),
            ('mf', 'COMMVEH_TOTAL_DEMAND', 'Commercial vehicle total demand all periods'),
        ]
        self.add_matrices("commercial_vehicle_model", "ALL",
                [(ident, name, desc) for ident, name, desc in tmplt_matrices])

        tmplt_matrices = [
            ('COMMVEH',    'Commerical vehicle total demand'),
            ('COMVEHGP',   'Commerical vehicle Toll demand'),
            ('COMVEHTOLL', 'Commerical vehicle GP demand'),
        ]
        for period in self._all_periods:
            self.add_matrices("commercial_vehicle_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def external_internal_model(self):
        tmplt_matrices = [
            ('SOVTOLLL_EIWORK',  'US to SD SOV Work TOLL demand'),
            ('HOV2TOLLL_EIWORK', 'US to SD HOV2 Work TOLL demand'),
            ('HOV3TOLLL_EIWORK', 'US to SD HOV3 Work TOLL demand'),
            ('SOVGPL_EIWORK',  'US to SD SOV Work GP demand'),
            ('HOV2HOVL_EIWORK', 'US to SD HOV2 Work HOV demand'),
            ('HOV3HOVL_EIWORK', 'US to SD HOV3 Work HOV demand'),
            ('SOVTOLLL_EINONWORK',  'US to SD SOV Non-Work TOLL demand'),
            ('HOV2TOLLL_EINONWORK', 'US to SD HOV2 Non-Work TOLL demand'),
            ('HOV3TOLLL_EINONWORK', 'US to SD HOV3 Non-Work TOLL demand'),
            ('SOVGPL_EINONWORK',  'US to SD SOV Non-Work GP demand'),
            ('HOV2HOVL_EINONWORK', 'US to SD HOV2 Non-Work HOV demand'),
            ('HOV3HOVL_EINONWORK', 'US to SD HOV3 Non-Work HOV demand'),
            ('SOVTOLLM_EIWORK',  'US to SD SOV Work TOLL demand'),
            ('HOV2TOLLM_EIWORK', 'US to SD HOV2 Work TOLL demand'),
            ('HOV3TOLLM_EIWORK', 'US to SD HOV3 Work TOLL demand'),
            ('SOVGPM_EIWORK',  'US to SD SOV Work GP demand'),
            ('HOV2HOVM_EIWORK', 'US to SD HOV2 Work HOV demand'),
            ('HOV3HOVM_EIWORK', 'US to SD HOV3 Work HOV demand'),
            ('SOVTOLLM_EINONWORK',  'US to SD SOV Non-Work TOLL demand'),
            ('HOV2TOLLM_EINONWORK', 'US to SD HOV2 Non-Work TOLL demand'),
            ('HOV3TOLLM_EINONWORK', 'US to SD HOV3 Non-Work TOLL demand'),
            ('SOVGPM_EINONWORK',  'US to SD SOV Non-Work GP demand'),
            ('HOV2HOVM_EINONWORK', 'US to SD HOV2 Non-Work HOV demand'),
            ('HOV3HOVM_EINONWORK', 'US to SD HOV3 Non-Work HOV demand'),
            ('SOVTOLLH_EIWORK',  'US to SD SOV Work TOLL demand'),
            ('HOV2TOLLH_EIWORK', 'US to SD HOV2 Work TOLL demand'),
            ('HOV3TOLLH_EIWORK', 'US to SD HOV3 Work TOLL demand'),
            ('SOVGPH_EIWORK',  'US to SD SOV Work GP demand'),
            ('HOV2HOVH_EIWORK', 'US to SD HOV2 Work HOV demand'),
            ('HOV3HOVH_EIWORK', 'US to SD HOV3 Work HOV demand'),
            ('SOVTOLLH_EINONWORK',  'US to SD SOV Non-Work TOLL demand'),
            ('HOV2TOLLH_EINONWORK', 'US to SD HOV2 Non-Work TOLL demand'),
            ('HOV3TOLLH_EINONWORK', 'US to SD HOV3 Non-Work TOLL demand'),
            ('SOVGPH_EINONWORK',  'US to SD SOV Non-Work GP demand'),
            ('HOV2HOVH_EINONWORK', 'US to SD HOV2 Non-Work HOV demand'),
            ('HOV3HOVH_EINONWORK', 'US to SD HOV3 Non-Work HOV demand'),
        ]
        for period in self._all_periods:
            self.add_matrices("external_internal_model", period,
                [("mf", period + "_" + name, period + " " + desc)
                 for name, desc in tmplt_matrices])

    def external_external_model(self):
        self.add_matrices("external_external_model", "ALL",
                [("mf", "ALL_TOTAL_EETRIPS",  "All periods Total for all modes external-external trips")])
        tmplt_matrices = [
            ('SOVGP_EETRIPS',    'SOVGP external-external demand'),
            ('HOV2HOV_EETRIPS',  'HOV2HOV external-external demand'),
            ('HOV3HOV_EETRIPS',  'HOV3HOV external-external demand'),
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
            matrices = []
            for period in periods + ["ALL"]:
                with _m.logbook_trace("For period %s" % (period)):
                    for ident, name, desc in self._matrices[component][period]:
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
