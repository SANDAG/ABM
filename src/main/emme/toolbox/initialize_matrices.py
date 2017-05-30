#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright INRO, 2016-2017.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// initialize.py                                                         ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////


import inro.modeller as _m
import traceback as _traceback


TOOLBOX_ORDER = 1


class Initialize(_m.Tool()):

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
        self._debug = False


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

    @_m.logbook_trace("Initialize matrices")
    def __call__(self, components, periods, scenario):
        self.scenario = scenario
        self._create_matrix_tool = _m.Modeller().tool(
            "inro.emme.data.matrix.create_matrix")
        if components == "all":
            components = self._all_components[:]
        if periods == "all":
            periods = self._all_periods[:]
        self.periods = periods

        for component in components:
            fcn = getattr(self, component)
            fcn()

        # traffic_demand:           101 to 200
        # transit_demand:           201 to 275
        # traffic_skims:            301 to 555
        # transit_skims:            561 to 720
        # truck_model:              730 to 756
        # commercial_vehicle_model: 757 to 774
        # external_internal_model:  775 to 790
        # external_external_model:  791 to 806

    @_m.logbook_trace("Traffic demand matrices")
    def traffic_demand(self):
        matrices = [
            ( 1, "SOVGP",     "SOV GP-only demand"),
            ( 2, "SOVTOLL",   "SOV toll demand"),
            ( 3, "HOV2GP",    "HOV2 GP-only demand"),
            ( 4, "HOV2HOV",   "HOV2 HOV-lane demand"),
            ( 5, "HOV2TOLL",  "HOV2 toll demand"),
            ( 6, "HOV3GP",    "HOV3+ GP-only demand"),
            ( 7, "HOV3HOV",   "HOV3+ HOV-lane demand"),
            ( 8, "HOV3TOLL",  "HOV3+ toll demand"),
            ( 9, "TRKHGP",    "Truck Heavy GP-only demand"),
            (10, "TRKHTOLL",  "Truck Heavy toll demand"),
            (11, "TRKLGP",    "Truck Light GP-only demand"),
            (12, "TRKLTOLL",  "Truck Light toll demand"),
            (13, "TRKMGP",    "Truck Medium GP-only demand"),
            (14, "TRKMTOLL",  "Truck Medium toll demand"),
        ]
        index_start = 100
        period_index = {"EA": 0, "AM": 20, "MD": 40, "PM": 60, "EV": 80}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = "mf%s" % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)
        self.create_matrix("ms1", "zero", "zero")

    @_m.logbook_trace("Transit demand matrices")
    def transit_demand(self):
        matrices = [
            ( 1, "BUS",  "local bus demand"),
            ( 2, "LRT",  "LRT demand"),
            ( 3, "CMR",  "commuter rail demand"),
            ( 4, "EXP",  "express / premium bus demand"),
            ( 5, "BRT",  "BRT demand"),
        ]
        index_start = 200
        access_index = [("WLK", 0), ("PNR", 5), ("KNR", 10)]
        period_index = {"EA": 0, "AM": 15, "MD": 30, "PM": 45, "EV": 60}
        for period in self.periods:
            p_index = period_index[period]
            for a_name, a_index in access_index:
                for num, m_name, m_desc in matrices:
                    ident = "mf%s" % (index_start + p_index + a_index + num)
                    name = "%s_%s%s" % (period, a_name, m_name)
                    desc = "%s %s access %s" % (period, a_name, m_desc)
                    self.create_matrix(ident, name, desc)

    @_m.logbook_trace("Traffic skim matrices")
    def traffic_skims(self):
        matrices = [
            ( 1, "SOVGP_GENCOST",     "SOV GP total generalized cost"),
            ( 2, "SOVGP_TIME",        "SOV GP travel time"),
            ( 3, "SOVGP_DIST",        "SOV GP distance"),
            ( 4, "SOVTOLL_GENCOST",   "SOV Toll total generalized cost"),
            ( 5, "SOVTOLL_TIME",      "SOV Toll travel time"),
            ( 6, "SOVTOLL_DIST",      "SOV Toll distance"),
            ( 7, "SOVTOLL_MLCOST",    "SOV Toll managed lane cost $0.01"),
            ( 8, "SOVTOLL_TOLLCOST",  "SOV Toll toll cost $0.01"),
            ( 9, "SOVTOLL_TOLLDIST",  "SOV Toll distance on toll facility"),
            (10, "HOV2HOV_GENCOST",   "HOV2 HOV total generalized cost"),
            (11, "HOV2HOV_TIME",      "HOV2 HOV travel time"),
            (12, "HOV2HOV_DIST",      "HOV2 HOV distance"),
            (13, "HOV2HOV_HOVDIST",   "HOV2 HOV distance on HOV facility"),
            (14, "HOV2TOLL_GENCOST",  "HOV2 Toll total generalized cost"),
            (15, "HOV2TOLL_TIME",     "HOV2 Toll travel time"),
            (16, "HOV2TOLL_DIST",     "HOV2 Toll distance"),
            (17, "HOV2TOLL_MLCOST",   "HOV2 Toll managed lane cost $0.01"),
            (18, "HOV2TOLL_TOLLCOST", "HOV2 Toll toll cost $0.01"),
            (19, "HOV2TOLL_TOLLDIST", "HOV2 Toll distance on toll facility"),
            (20, "HOV3HOV_GENCOST",   "HOV3+ HOV total generalized cost"),
            (21, "HOV3HOV_TIME",      "HOV3+ HOV travel time"),
            (22, "HOV3HOV_DIST",      "HOV3+ HOV distance"),
            (23, "HOV3HOV_HOVDIST",   "HOV3+ HOV distance on HOV facility"),
            (24, "HOV3TOLL_GENCOST",  "HOV3+ Toll total generalized cost"),
            (25, "HOV3TOLL_TIME",     "HOV3+ Toll travel time"),
            (26, "HOV3TOLL_DIST",     "HOV3+ Toll distance"),
            (27, "HOV3TOLL_MLCOST",   "HOV3+ Toll managed lane cost $0.01"),
            (28, "HOV3TOLL_TOLLCOST", "HOV3+ Toll toll cost $0.01"),
            (29, "HOV3TOLL_TOLLDIST", "HOV3+ Toll distance on toll facility"),
            (30, "TRKHGP_GENCOST",    "Truck Heavy GP total generalized cost"),
            (31, "TRKHGP_TIME",       "Truck Heavy GP travel time"),
            (32, "TRKHGP_DIST",       "Truck Heavy GP distance"),
            (33, "TRKHTOLL_GENCOST",  "Truck Heavy Toll total generalized cost"),
            (34, "TRKHTOLL_TIME",     "Truck Heavy Toll travel time"),
            (35, "TRKHTOLL_DIST",     "Truck Heavy Toll distance"),
            (36, "TRKHTOLL_TOLLCOST", "Truck Heavy Toll toll cost $0.01"),
            (37, "TRKLGP_GENCOST",    "Truck Light GP total generalized cost"),
            (38, "TRKLGP_TIME",       "Truck Light GP travel time"),
            (39, "TRKLGP_DIST",       "Truck Light GP distance"),
            (40, "TRKLTOLL_GENCOST",  "Truck Light Toll total generalized cost"),
            (41, "TRKLTOLL_TIME",     "Truck Light Toll travel time"),
            (42, "TRKLTOLL_DIST",     "Truck Light Toll distance"),
            (43, "TRKLTOLL_TOLLCOST", "Truck Light Toll toll cost $0.01"),
            (44, "TRKMGP_GENCOST",    "Truck Medium GP total generalized cost"),
            (45, "TRKMGP_TIME",       "Truck Medium GP travel time"),
            (46, "TRKMGP_DIST",       "Truck Medium GP distance"),
            (47, "TRKMTOLL_GENCOST",  "Truck Medium Toll total generalized cost"),
            (48, "TRKMTOLL_TIME",     "Truck Medium Toll travel time"),
            (49, "TRKMTOLL_DIST",     "Truck Medium Toll distance"),
            (50, "TRKMTOLL_TOLLCOST", "Truck Medium Toll toll cost $0.01"),
        ]
        index_start = 300
        period_index = [("EA", 0), ("AM", 50), ("MD", 100), ("PM", 150), ("EV", 200)]
        period_index = {"EA": 0, "AM": 50, "MD": 100, "PM": 150, "EV": 200}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = "mf%s" % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)

        if "MD" in self.periods:
            # TODO: we may not need all of these skims - only the TIME one is used
            matrices = [
                (551, "MD_TRK_GENCOST",  "MD Truck generic total generalized cost"),
                (552, "MD_TRK_TIME",     "MD Truck generic travel time"),
                (553, "MD_TRK_DIST",     "MD Truck generic distance"),
                (554, "MD_TRK_MLCOST",   "MD Truck generic managed lane cost $0.01"),
                (555, "MD_TRK_TOLLCOST", "MD Truck generic toll cost $0.01"),
            ]
            for num, m_name, m_desc in matrices:
                self.create_matrix("mf%s" % num, m_name, m_desc)

    @_m.logbook_trace("Transit skim matrices")
    def transit_skims(self):
        matrices = [
            ( 1, "BUS_GENCOST",    "Local bus: total impedance"), 
            ( 2, "BUS_FIRSTWAIT",  "Local bus: first wait time"), 
            ( 3, "BUS_XFERWAIT",   "Local bus: transfer wait time"), 
            ( 4, "BUS_TOTALWAIT",  "Local bus: total wait time"), 
            ( 5, "BUS_FARE",       "Local bus: fare"), 
            ( 6, "BUS_XFERS",      "Local bus: num transfers"), 
            ( 7, "BUS_ACCWALK",    "Local bus: access walk time"), 
            ( 8, "BUS_XFERWALK",   "Local bus: transfer walk time"), 
            ( 9, "BUS_EGRWALK",    "Local bus: egress walk time"), 
            (10, "BUS_TOTALWALK",  "Local bus: total walk time"), 
            (11, "BUS_TOTALIVTT",  "Local bus: in-vehicle time"), 
            (12, "BUS_DWELLTIME",  "Local bus: dwell time"), 
            (13, "ALL_GENCOST",    "All modes: total impedance"), 
            (14, "ALL_FIRSTWAIT",  "All modes: first wait time"), 
            (15, "ALL_XFERWAIT",   "All modes: transfer wait time"), 
            (16, "ALL_TOTALWAIT",  "All modes: total wait time"), 
            (17, "ALL_FARE",       "All modes: fare"), 
            (18, "ALL_XFERS",      "All modes: num transfers"), 
            (19, "ALL_ACCWALK",    "All modes: access walk time"), 
            (20, "ALL_XFERWALK",   "All modes: transfer walk time"), 
            (21, "ALL_EGRWALK",    "All modes: egress walk time"), 
            (22, "ALL_TOTALWALK",  "All modes: total walk time"), 
            (23, "ALL_TOTALIVTT",  "All modes: in-vehicle time"), 
            (24, "ALL_DWELLTIME",  "All modes: dwell time"), 
            (25, "ALL_BUSIVTT",    "All modes: local bus in-vehicle time"),
            (27, "ALL_LRTIVTT",    "All modes: LRT in-vehicle time"),
            (28, "ALL_CMRIVTT",    "All modes: Rail in-vehicle time"),
            (29, "ALL_EXPIVTT",    "All modes: Express in-vehicle time"),
            (30, "ALL_LTDEXPIVTT", "All modes: Ltd exp bus in-vehicle time"),
            (31, "ALL_BRTREDIVTT", "All modes: BRT red in-vehicle time"),
            (32, "ALL_BRTYELIVTT", "All modes: BRT yellow in-vehicle time"),
        ]
        index_start = 560
        period_index = {"EA": 0, "AM": 32, "MD": 64, "PM": 96, "EV": 128}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = "mf%s" % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)

    @_m.logbook_trace("Truck sub-model matrices")
    def truck_model(self):
        matrices = [
            ( 1, "TRKL",    "Light truck"), 
            ( 2, "TRKM",    "Medium truck"), 
            ( 3, "TRKH",    "Heavy truck"), 
            ( 4, "TRKEI",   "Truck external-internal"), 
            ( 5, "TRKIE",   "Truck internal-external"), 
        ]
        index_start = 730
        for num, m_name, m_desc in matrices:
            ident = (index_start + num)
            self.create_matrix('mo%s' % ident, m_name + '_PROD', m_desc + ' production')
            self.create_matrix('md%s' % ident, m_name + '_ATTR', m_desc + ' attraction')

        matrices = [
            ( 1, "TRKEE_DEMAND",     "Truck total external-external demand"), 
            ( 2, "TRKL_FRICTION",    "Light truck friction factors"), 
            ( 3, "TRKM_FRICTION",    "Medium truck friction factors"), 
            ( 4, "TRKH_FRICTION",    "Heavy truck friction factors"), 
            ( 5, "TRKIE_FRICTION",   "Truck internal-external friction factors"), 
            ( 6, "TRKEI_FRICTION",   "Truck external-internal friction factors"), 
            ( 7, "TRKL_DEMAND",      "Light truck total demand"), 
            ( 8, "TRKM_DEMAND",      "Medium truck total demand"), 
            ( 9, "TRKH_DEMAND",      "Heavy truck total demand"), 
            (10, "TRKIE_DEMAND",     "Truck internal-external total demand"), 
            (11, "TRKEI_DEMAND",     "Truck external-internal total demand"), 
        ]
        index_start = 730
        for num, m_name, m_desc in matrices:
            self.create_matrix('mf%s' % (index_start + num), m_name, m_desc)

        matrices = [
            ( 1, "TRKL",    "Light truck demand"), 
            ( 2, "TRKM",    "Medium truck demand"),
            ( 3, "TRKH",    "Heavy truck demand"), 
        ]
        index_start = 741
        period_index = {"EA": 0, "AM": 3, "MD": 6, "PM": 9, "EV": 12}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = 'mf%s' % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)

    @_m.logbook_trace("Commercial vehicle sub-model matrices")
    def commercial_vehicle_model(self):
        matrices = [
            ('mo757', 'COMMVEH_PROD',         'Commercial vehicle production'),
            ('md757', 'COMMVEH_ATTR',         'Commercial vehicle attraction'),
            ('mf757', 'COMMVEH_BLENDED_SKIM', 'Commercial vehicle blended skim'),
            ('mf758', 'COMMVEH_FRICTION',     'Commercial vehicle friction factors'),
            ('mf759', 'COMMVEH_TOTAL_DEMAND', 'Commercial vehicle total demand all periods'),
        ]
        for ident, name, desc in matrices:
            self.create_matrix(ident, name, desc)
        matrices = [
            ( 1, 'COMMVEH',    'Commerical vehicle total demand'),
            ( 2, 'COMVEHGP',   'Commerical vehicle Toll demand'),
            ( 3, 'COMVEHTOLL', 'Commerical vehicle GP demand'),
        ]
        index_start = 759
        period_index = {"EA": 0, "AM": 3, "MD": 6, "PM": 9, "EV": 12}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = 'mf%s' % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)

    @_m.logbook_trace("External-internal sub-model matrices")
    def external_internal_model(self):
        matrices = [
            ( 1, 'SOVTOLL_EIWORK',  'US to SD SOV Work TOLL demand'),
            ( 2, 'HOV2TOLL_EIWORK', 'US to SD HOV2 Work TOLL demand'),
            ( 3, 'HOV3TOLL_EIWORK', 'US to SD HOV3 Work TOLL demand'),
            ( 4, 'SOVGP_EIWORK',  'US to SD SOV Work GP demand'),
            ( 5, 'HOV2GP_EIWORK', 'US to SD HOV2 Work GP demand'),
            ( 6, 'HOV3GP_EIWORK', 'US to SD HOV3 Work GP demand'),
            ( 7, 'SOVTOLL_EINONWORK',  'US to SD SOV Non-Work TOLL demand'),
            ( 8, 'HOV2TOLL_EINONWORK', 'US to SD HOV2 Non-Work TOLL demand'),
            ( 9, 'HOV3TOLL_EINONWORK', 'US to SD HOV3 Non-Work TOLL demand'),
            (10, 'SOVGP_EINONWORK',  'US to SD SOV Non-Work GP demand'),
            (11, 'HOV2GP_EINONWORK', 'US to SD HOV2 Non-Work GP demand'),
            (12, 'HOV3GP_EINONWORK', 'US to SD HOV3 Non-Work GP demand'),
        ]
        index_start = 775
        period_index = {"EA": 0, "AM": 3, "MD": 6, "PM": 9, "EV": 12}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = 'mf%s' % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)


    @_m.logbook_trace("External-external sub-model matrices")
    def external_external_model(self):
        self.create_matrix(
            "mf791", "ALL_TOTAL_EETRIPS", 
            "All periods Total for all modes external-external trips")
        matrices = [
            ( 1, 'SOVGP_EETRIPS',    'SOVGP external-external demand'),
            ( 2, 'HOV2HOV_EETRIPS',  'HOV2HOV external-external demand'),
            ( 3, 'HOV3HOV_EETRIPS',  'HOV3HOV external-external demand'),
        ]
        index_start = 791
        period_index = {"EA": 0, "AM": 3, "MD": 6, "PM": 9, "EV": 12}
        for period in self.periods:
            p_index = period_index[period]
            for num, m_name, m_desc in matrices:
                ident = 'mf%s' % (index_start + p_index + num)
                self.create_matrix(ident, period + "_" + m_name, period + " " + m_desc)

    def create_matrix(self, ident, name, desc):
        if self._debug:
            mat = self.scenario.emmebank.matrix(ident)
            if mat and mat.name != name:
                raise Exception("Conflicting matrix ID / name %s, %s" % (ident, name))
        self._create_matrix_tool(ident, name, desc, scenario=self.scenario, overwrite=True)

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg