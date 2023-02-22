import os
import inro.modeller as _m
import inro.emme.desktop.app as _app
import traceback as _traceback
modeller = _m.Modeller()
desktop = modeller.desktop


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

class CreateTransitConnector(_m.Tool(), gen_utils.Snapshot):

    create_connectors = _m.Attribute(bool)
    scenario =  _m.Attribute(_m.InstanceType)
    period = _m.Attribute(unicode)

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):

        self.create_connectors = False
        self.scenario = _m.Modeller().scenario
        self.period = _m.Attribute(unicode)
      
        #self.scenario = scenario
        self.line_haul_modes_local = ["l"] # local
        self.line_haul_mode_local_descr = ["local bus stops"]
        self.transit_modes_local = ['b']

        self.line_haul_modes_pr = ["e"] # Premium
        self.line_haul_mode_pr_descr = ["premium stops"]
        self.transit_modes_pr = ["celpry"]

        self.line_haul_modes = ["b", "celpry"] # Local (bus), premium modes
        self.line_haul_mode_descr = ["Local stops", "Premium stops"]

        self.transit_modes = ["b","celpry"]
        self.line_haul_mode_specs = ["@num_stops_l=1,99","@num_stops_e=1,99"]

        self.max_length_wlk = [1, 1.2] # length in miles
        self.max_length_knr = [5, 5]
        self.max_length_pnr = [10, 10]
        self.max_length_tnc = [5, 5]

        self.acc_modes = ["ufqQ", "uqQ", "fqQ", "qQ", "u", "f", "q", "Q"]
        
        self.attributes = [
           "create_connectors", "scenario", "period"]
        
    def from_snapshot(self, snapshot):
        super(CreateTransitConnector, self).from_snapshot(snapshot)
        # custom from_snapshot to load scenario and database objects
        self.scenario = _m.Modeller().emmebank.scenario(self.scenario)
        return self

    def page(self):
        
        pb = _m.ToolPageBuilder(self)
        pb.title = "Create Transit Connectors"
        pb.description = """
            Create Transit Connectors for 2zone model system."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)
        
        options = [("EA","Early AM"),
                   ("AM","AM peak"),
                   ("MD","Mid-day"),
                   ("PM","PM peak"),
                   ("EV","Evening")]

        pb.add_select("period", options, title="Period:")
        pb.add_select_scenario("scenario",
            title="Transit assignment scenario:")

        pb.add_checkbox("create_connectors", title=" ", label="Create connectors")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(self.period, self.scenario, self.create_connectors)
            run_msg = "Transit connectors created"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, period, scenario, create_connectors=False):

        attrs = {
            "period": period,
            "scenario": scenario.id,
            "create_tr_connectors": create_connectors,
            "self": str(self)
        }
        
        self.scenario = scenario
        self.create_tr_connectors(self)

    @_m.logbook_trace("Transit connector creator", save_arguments=True)
    def create_tr_connectors(self, create_connectors):

        create_connectors = _m.Modeller().tool("inro.emme.data.network.base.create_connectors")
        #change_scenario = _m.Modeller().tool("inro.emme.data.scenario.change_primary_scenario")
        #delete_nodes = _m.Modeller().tool("inro.emme.data.network.base.delete_nodes")
        create_extra = _m.Modeller().tool("inro.emme.data.extra_attribute.create_extra_attribute")
        netcalc = _m.Modeller().tool("inro.emme.network_calculation.network_calculator")
        export_basenet = _m.Modeller().tool("inro.emme.data.network.base.export_base_network")
        import_basenet = _m.Modeller().tool("inro.emme.data.network.base.base_network_transaction")
        # desktop = _app.start_dedicated(project=r"C:\ABM_runs\Emme_Setup\emme_project\emme_project.emp", visible=True, user_initials="AE")

        if create_connectors:
            # scenario = desktop.data_explorer().active_database().scenario_by_number(s['periodNum'])
            # desktop.data_explorer().replace_primary_scenario(self.scenario)
            
            for i in range(len(self.line_haul_modes_local)):

                # count number of stops at each node
                create_extra(extra_attribute_type="NODE",
                                    extra_attribute_name="@num_stops_%s" % self.line_haul_modes_local[i],
                                    extra_attribute_description="number of %s" % self.line_haul_mode_local_descr[i],
                                    overwrite=True, scenario=self.scenario)
                spec1={
                    "result": "@num_stops_%s" % self.line_haul_modes_local[i],
                    "expression": "(noboa==0).or.(noali==0)",
                    "aggregation": "+",
                    "selections": {
                        "link": "all",
                        "transit_line": "mode=%s" % self.transit_modes_local[i]
                    },
                    "type": "NETWORK_CALCULATION"
                }
                spec2={
                    "result": "@num_stops_%sj" % self.line_haul_modes_local[i],
                    "expression": "(noboan==0).or.(noalin==0)",
                    "aggregation": "+",
                    "selections": {
                        "link": "all",
                        "transit_line": "mode=%s" % self.transit_modes_local[i]
                    },
                    "type": "NETWORK_CALCULATION"
                }
                netcalc([spec1,spec2], scenario=self.scenario) 
                
            for i in range(len(self.line_haul_modes_pr)):
    
                # count number of stops at each node
                create_extra(extra_attribute_type="NODE",
                                    extra_attribute_name="@num_stops_%s" % self.line_haul_modes_pr[i],
                                    extra_attribute_description="number of %s" % self.line_haul_mode_pr_descr[i],
                                    overwrite=True, scenario=self.scenario)
                spec1={
                    "result": "@num_stops_%s" % self.line_haul_modes_pr[i],
                    "expression": "(noboa==0).or.(noali==0)",
                    "aggregation": "+",
                    "selections": {
                        "link": "all",
                        "transit_line": "mode=%s" % self.transit_modes_pr[i]
                    },
                    "type": "NETWORK_CALCULATION"
                }
                spec2={
                    "result": "@num_stops_%sj" % self.line_haul_modes_pr[i],
                    "expression": "(noboan==0).or.(noalin==0)",
                    "aggregation": "+",
                    "selections": {
                        "link": "all",
                        "transit_line": "mode=%s" % self.transit_modes_pr[i]
                    },
                    "type": "NETWORK_CALCULATION"
                }
                netcalc([spec1,spec2], scenario=self.scenario)


            for i in range(len(self.line_haul_modes)):
    # create connectors for each access and line haul mode and export connectors
                create_connectors(access_modes=["u"],
                                egress_modes=["k"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_wlk[i],
                                max_connectors=10,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_u" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)
                create_connectors(access_modes=["f"],
                                egress_modes=["g"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_pnr[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_f" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)                     
                create_connectors(access_modes=["q"],
                                egress_modes=["j"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_knr[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_q" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario) 
                create_connectors(access_modes=["Q"],
                                egress_modes=["J"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_tnc[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_Q" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)                     
                create_connectors(access_modes=["u", "q", "Q"],
                                egress_modes=["k", "j", "J"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_wlk[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_uqQ" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)                       
                create_connectors(access_modes=["f", "q", "Q"],
                                egress_modes=["g", "j", "J"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_knr[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_fqQ" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)
                create_connectors(access_modes=["q", "Q"],
                                egress_modes=["j", "J"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_knr[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_qQ" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)       
                create_connectors(access_modes=["u", "f", "q", "Q"],
                                egress_modes=["k", "g", "j", "J"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_wlk[i],
                                max_connectors=2,
                                min_angle=0,
                                scenario=self.scenario)
                export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                            "node": 'none'},
                            export_file = "connectors_ufqQ" + self.line_haul_modes[i] + ".out",
                            field_separator = " ", scenario=self.scenario)

                # import connectors; if a connector already exists, it's skipped because the connector with the most access modes is imported first
            for line_haul in self.line_haul_modes:
                for acc in self.acc_modes:
                    print("temp/" + "connectors_" + acc + line_haul + ".out")
                    import_basenet(transaction_file = "connectors_" + acc + line_haul + ".out", revert_on_error = False, scenario=self.scenario)
            # export all onnectors
            export_basenet(selection = {"link": 'i=1,4947 or j=1,4947',
                                        "node": 'none'},
                        export_file = "connectors.out",
                        field_separator = " ", scenario=self.scenario)
                #delete individial connector files by transit mode and access mode
            for line_haul in self.line_haul_modes:
                for acc in self.acc_modes:
                    try:
                        os.remove("connectors_" + acc + line_haul + ".out")
                    except:
                        pass

            import_basenet(transaction_file = "connectors.out", revert_on_error = False, scenario=self.scenario)
            print("Finished adding access and egress links")
