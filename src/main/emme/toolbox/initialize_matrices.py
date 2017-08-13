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

TOOLBOX_ORDER = 111


import inro.modeller as _m
import traceback as _traceback

gen_utils = _m.Modeller().module("sandag.utilities.general")


class Initialize(_m.Tool(), gen_utils.Snapshot):

    components = _m.Attribute(_m.ListType)
    periods = _m.Attribute(_m.ListType)

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
        self.attributes = ["components", "periods"]
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
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            scenario = _m.Modeller().scenario
            self(self.components, self.periods, scenario)
            run_msg = "Tool completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    @_m.logbook_trace("Create and initialize matrices", save_arguments=True)
    def __call__(self, components, periods, scenario):
        attributes = {"components": components, "periods": periods}
        gen_utils.log_snapshot("Initialize matrices", str(self), attributes)

        self.scenario = scenario
        self._create_matrix_tool = _m.Modeller().tool(
            "inro.emme.data.matrix.create_matrix")
        if components == "all":
            components = self._all_components[:]
        if periods == "all":
            periods = self._all_periods[:]
        
        self._matrices = dict((name, dict((k, []) for k in self._all_periods + ["ALL"])) for name in self._all_components)
        self._count = {"ms": 1, "md": 100, "mo": 100, "mf": 100}

        for component in self._all_components:
            fcn = getattr(self, component)
            fcn()
        for component in components:
            self.create_matrices(component, periods)

    def traffic_demand(self):
        tmplt_matrices = [
            ("SOVGP",     "SOV GP-only demand"),
            ("SOVTOLL",   "SOV toll demand"),
            ("HOV2GP",    "HOV2 GP-only demand"),
            ("HOV2HOV",   "HOV2 HOV-lane demand"),
            ("HOV2TOLL",  "HOV2 toll demand"),
            ("HOV3GP",    "HOV3+ GP-only demand"),
            ("HOV3HOV",   "HOV3+ HOV-lane demand"),
            ("HOV3TOLL",  "HOV3+ toll demand"),
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
        self.add_matrices("traffic_demand", "ALL", [("ms", "zero", "zero")])

    def transit_demand(self):
        tmplt_matrices = [
            ("BUS",  "local bus demand"),
            ("LRT",  "LRT demand"),
            ("CMR",  "commuter rail demand"),
            ("EXP",  "express / premium bus demand"),
            ("BRT",  "BRT demand"),
        ]
        for period in self._all_periods:
            for a_name in ["WLK", "PNR", "KNR"]:
                self.add_matrices("transit_demand", period,
                    [("mf", "%s_%s%s" % (period, a_name, name), "%s %s access %s" % (period, a_name, desc)) 
                     for name, desc in tmplt_matrices])

    def traffic_skims(self):
        tmplt_matrices = [
            ("SOVGP_GENCOST",     "SOV GP total generalized cost"),
            ("SOVGP_TIME",        "SOV GP travel time"),
            ("SOVGP_DIST",        "SOV GP distance"),
            ("SOVTOLL_GENCOST",   "SOV Toll total generalized cost"),
            ("SOVTOLL_TIME",      "SOV Toll travel time"),
            ("SOVTOLL_DIST",      "SOV Toll distance"),
            ("SOVTOLL_MLCOST",    "SOV Toll managed lane cost $0.01"),
            ("SOVTOLL_TOLLCOST",  "SOV Toll toll cost $0.01"),
            ("SOVTOLL_TOLLDIST",  "SOV Toll distance on toll facility"),
            ("HOV2HOV_GENCOST",   "HOV2 HOV total generalized cost"),
            ("HOV2HOV_TIME",      "HOV2 HOV travel time"),
            ("HOV2HOV_DIST",      "HOV2 HOV distance"),
            ("HOV2HOV_HOVDIST",   "HOV2 HOV distance on HOV facility"),
            ("HOV2TOLL_GENCOST",  "HOV2 Toll total generalized cost"),
            ("HOV2TOLL_TIME",     "HOV2 Toll travel time"),
            ("HOV2TOLL_DIST",     "HOV2 Toll distance"),
            ("HOV2TOLL_MLCOST",   "HOV2 Toll managed lane cost $0.01"),
            ("HOV2TOLL_TOLLCOST", "HOV2 Toll toll cost $0.01"),
            ("HOV2TOLL_TOLLDIST", "HOV2 Toll distance on toll facility"),
            ("HOV3HOV_GENCOST",   "HOV3+ HOV total generalized cost"),
            ("HOV3HOV_TIME",      "HOV3+ HOV travel time"),
            ("HOV3HOV_DIST",      "HOV3+ HOV distance"),
            ("HOV3HOV_HOVDIST",   "HOV3+ HOV distance on HOV facility"),
            ("HOV3TOLL_GENCOST",  "HOV3+ Toll total generalized cost"),
            ("HOV3TOLL_TIME",     "HOV3+ Toll travel time"),
            ("HOV3TOLL_DIST",     "HOV3+ Toll distance"),
            ("HOV3TOLL_MLCOST",   "HOV3+ Toll managed lane cost $0.01"),
            ("HOV3TOLL_TOLLCOST", "HOV3+ Toll toll cost $0.01"),
            ("HOV3TOLL_TOLLDIST", "HOV3+ Toll distance on toll facility"),
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
            ("BUS_GENCOST",    "Local bus: total impedance"), 
            ("BUS_FIRSTWAIT",  "Local bus: first wait time"), 
            ("BUS_XFERWAIT",   "Local bus: transfer wait time"), 
            ("BUS_TOTALWAIT",  "Local bus: total wait time"), 
            ("BUS_FARE",       "Local bus: fare"), 
            ("BUS_XFERS",      "Local bus: num transfers"), 
            ("BUS_ACCWALK",    "Local bus: access walk time"), 
            ("BUS_XFERWALK",   "Local bus: transfer walk time"), 
            ("BUS_EGRWALK",    "Local bus: egress walk time"), 
            ("BUS_TOTALWALK",  "Local bus: total walk time"), 
            ("BUS_TOTALIVTT",  "Local bus: in-vehicle time"), 
            ("BUS_DWELLTIME",  "Local bus: dwell time"), 
            ("BUS_DIST",       "Local bus: IV distance"), 
            ("ALL_GENCOST",    "All modes: total impedance"), 
            ("ALL_FIRSTWAIT",  "All modes: first wait time"), 
            ("ALL_XFERWAIT",   "All modes: transfer wait time"), 
            ("ALL_TOTALWAIT",  "All modes: total wait time"), 
            ("ALL_FARE",       "All modes: fare"), 
            ("ALL_XFERS",      "All modes: num transfers"), 
            ("ALL_ACCWALK",    "All modes: access walk time"), 
            ("ALL_XFERWALK",   "All modes: transfer walk time"), 
            ("ALL_EGRWALK",    "All modes: egress walk time"), 
            ("ALL_TOTALWALK",  "All modes: total walk time"), 
            ("ALL_TOTALIVTT",  "All modes: in-vehicle time"), 
            ("ALL_DWELLTIME",  "All modes: dwell time"), 
            ("ALL_BUSIVTT",    "All modes: local bus in-vehicle time"),
            ("ALL_LRTIVTT",    "All modes: LRT in-vehicle time"),
            ("ALL_CMRIVTT",    "All modes: Rail in-vehicle time"),
            ("ALL_EXPIVTT",    "All modes: Express in-vehicle time"),
            ("ALL_LTDEXPIVTT", "All modes: Ltd exp bus in-vehicle time"),
            ("ALL_BRTREDIVTT", "All modes: BRT red in-vehicle time"),
            ("ALL_BRTYELIVTT", "All modes: BRT yellow in-vehicle time"),
            ("ALL_BUSDIST",    "All modes: Bus IV distance"), 
            ("ALL_LRTDIST",    "All modes: LRT IV distance"), 
            ("ALL_CMRDIST",    "All modes: Rail IV distance"), 
            ("ALL_EXPDIST",    "All modes: Express and Ltd IV distance"), 
            ("ALL_BRTDIST",    "All modes: BRT red and yel IV distance"), 
            ("ALL_MAINMODE",   "All modes: main mode of travel from IVTT"), 
        ]
        for period in self._all_periods:
            self.add_matrices("transit_skims", period,
                [("mf", period + "_" + name, period + " " + desc) 
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
            for period in periods + ["ALL"]:
                with _m.logbook_trace("For period %s" % (period)):
                    for ident, name, desc in self._matrices[component][period]:
                        self._create_matrix_tool(ident, name, desc, scenario=self.scenario, overwrite=True)

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg
