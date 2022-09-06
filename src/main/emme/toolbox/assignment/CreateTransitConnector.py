import os
import inro.modeller as _m
import inro.emme.desktop.app as _app
import traceback as _traceback


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")

class CreateTransitConnector(_m.Tool(), gen_utils.Snapshot):

    create_connectors = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=unicode)
    def tool_run_msg_status(self):
        return self.tool_run_msg

    def __init__(self):

        self.create_connectors = False
      
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

        self.max_length_wlk = [0.55, 1.2, 1.2] # length in miles
        self.max_length_knr = [5, 5, 5]
        self.max_length_pnr = [10, 15, 15]

        self.acc_modes = ["ufq", "uq", "fq", "u", "f", "q"]
        
        self.attributes = [
           "create_connectors"]

    def page(self):
        
        pb = _m.ToolPageBuilder(self)
        pb.title = "Create Transit Connectors"
        pb.description = """
            Create Transit Connectors for 2zone model system."""
        pb.branding_text = "- SANDAG - "
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_checkbox("create_connectors", title=" ", label="Create connectors")

        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(self.create_connectors)
            run_msg = "Transit connectors created"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise

    def __call__(self, create_connectors=False):

        #self.generate_connectors = create_connectors

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

        if create_connectors:
            
            for i in range(len(self.line_haul_modes_local)):

                # count number of stops at each node
                create_extra(extra_attribute_type="NODE",
                                    extra_attribute_name="@num_stops_%s" % self.line_haul_modes_local[i],
                                    extra_attribute_description="number of %s" % self.line_haul_mode_local_descr[i],
                                    overwrite=True)
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
                netcalc([spec1,spec2]) 
                
            for i in range(len(self.line_haul_modes_pr)):
    
                # count number of stops at each node
                create_extra(extra_attribute_type="NODE",
                                    extra_attribute_name="@num_stops_%s" % self.line_haul_modes_pr[i],
                                    extra_attribute_description="number of %s" % self.line_haul_mode_pr_descr[i],
                                    overwrite=True)
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
                netcalc([spec1,spec2])


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
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,4996 or j=1,4996',
                                            "node": 'none'},
                            export_file = "connectors_u" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")
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
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,4996 or j=1,4996',
                                            "node": 'none'},
                            export_file = "connectors_f" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")                     
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
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,4996 or j=1,4996',
                                            "node": 'none'},
                            export_file = "connectors_q" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")                     
                create_connectors(access_modes=["u", "q"],
                                egress_modes=["k", "j"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_wlk[i],
                                max_connectors=2,
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,4996 or j=1,4996',
                                            "node": 'none'},
                            export_file = "connectors_uq" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")                       
                create_connectors(access_modes=["f", "q"],
                                egress_modes=["g", "j"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_knr[i],
                                max_connectors=2,
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                            "node": 'none'},
                            export_file = "connectors_fq" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")     
                create_connectors(access_modes=["u", "f", "q"],
                                egress_modes=["k", "g", "j"],
                                delete_existing=True,
                                selection={
                                    "centroid":"all",
                                    "node": "%s" % self.line_haul_mode_specs[i],
                                    "link":"none",
                                    "exclude_split_links":False,
                                    "only_midblock_nodes": False},
                                max_length=self.max_length_wlk[i],
                                max_connectors=2,
                                min_angle=0)
                export_basenet(selection = {"link": 'i=1,4996 or j=1,4996',
                                            "node": 'none'},
                            export_file = "connectors_ufq" + self.line_haul_modes[i] + ".out",
                            field_separator = " ")

                # import connectors; if a connector already exists, it's skipped because the connector with the most access modes is imported first
            for line_haul in self.line_haul_modes:
                for acc in self.acc_modes:
                    print("temp/" + "connectors_" + acc + line_haul + ".out")
                    import_basenet(transaction_file = "connectors_" + acc + line_haul + ".out", revert_on_error = False)
            # export all onnectors
            export_basenet(selection = {"link": 'i=1,3649 or j=1,3649',
                                        "node": 'none'},
                        export_file = "connectors.out",
                        field_separator = " ")
                #delete individial connector files by transit mode and access mode
            for line_haul in self.line_haul_modes:
                for acc in self.acc_modes:
                    try:
                        os.remove("connectors_" + acc + line_haul + ".out")
                    except:
                        pass

            import_basenet(transaction_file = "connectors.out", revert_on_error = False)
            print("Finished adding access and egress links")
