"""
Created on March 2020

@author: cliu
"""

TOOLBOX_ORDER = 105

import inro.modeller as _m
import traceback as _traceback
import inro.emme.database.emmebank as _eb
import inro.emme.desktop.app as _app
import inro.emme.core.exception as _except
from collections import OrderedDict
import os
import pandas as pd
import openpyxl
from functools import reduce


gen_utils = _m.Modeller().module("sandag.utilities.general")
dem_utils = _m.Modeller().module("sandag.utilities.demand")


format = lambda x: ("%.6f" % x).rstrip('0').rstrip(".")
id_format = lambda x: str(int(x))

class validation(_m.Tool(), gen_utils.Snapshot):

    main_directory = _m.Attribute(str)
    base_scenario_id = _m.Attribute(int)
    traffic_emmebank = _m.Attribute(str)
    #transit_emmebank = _m.Attribute(str)
    attributes = _m.Attribute(str)

    tool_run_msg = ""

    def __init__(self):
        project_dir = os.path.dirname(_m.Modeller().desktop.project.path)
        self.main_directory = os.path.dirname(project_dir)
        self.base_scenario_id = 100
        self.traffic_emmebank = os.path.join(project_dir, "Database", "emmebank")
        #self.transit_emmebank = os.path.join(project_dir, "Database_transit", "emmebank")
        self.attributes = ["main_directory", "traffic_emmebank", "transit_emmebank","base_scenario_id"]

    def page(self):
        pb = _m.ToolPageBuilder(self)
        pb.title = "Validation Procedure"
        pb.description = """
Export traffic flow to Excel files for base year validation."""
        pb.branding_text = "- SANDAG - Validation"
        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file('main_directory', 'directory',
                           title='Select main directory')

        pb.add_select_file('traffic_emmebank', 'file',
                           title='Select traffic emmebank')
        #pb.add_select_file('transit_emmebank', 'file',
        #                   title='Select transit emmebank')
        return pb.render()

    def run(self):
        self.tool_run_msg = ""
        try:
            results = self(self.main_directory, self.traffic_emmebank, self.base_scenario_id)
            #results = self(self.main_directory, self.traffic_emmebank, self.transit_emmebank, self.base_scenario_id)
            run_msg = "Export completed"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise
    @_m.logbook_trace("Export network data for base year Validation", save_arguments=True)

    def __call__(self, main_directory, traffic_emmebank, base_scenario_id):
    #def __call__(self, main_directory, traffic_emmebank, transit_emmebank, base_scenario_id):
        print("in validation module")
        attrs = {
            "main_directory": main_directory,
            "traffic_emmebank": str(traffic_emmebank),
            #"transit_emmebank": str(transit_emmebank),
            "base_scenario_id": base_scenario_id,
            "self": str(self)
        }

        gen_utils.log_snapshot("Validation procedure", str(self), attrs)

        traffic_emmebank = _eb.Emmebank(traffic_emmebank)
        #transit_emmebank = _eb.Emmebank(transit_emmebank)
        export_path = os.path.join(main_directory, "analysis/validation")
        # transitbank_path = os.path.join(main_directory, "emme_project/Database_transit/emmebank")
        source_file = os.path.join(export_path, "source_EMME.xlsx")
        df = pd.read_excel(source_file, header=None, sheet_name='raw')
        writer = pd.ExcelWriter(source_file, engine='openpyxl')
        book = openpyxl.load_workbook(source_file)
        writer.book = book
        writer.sheets = dict((ws.title, ws) for ws in book.worksheets)

        #periods = ["EA"]
        periods = ["EA", "AM", "MD", "PM", "EV"]

        period_scenario_ids = OrderedDict((v, i) for i, v in enumerate(periods, start=int(base_scenario_id) + 1))
        
        #-------export tranffic data--------
        dfHwycov = pd.read_excel(source_file, sheetname='raw', usecols ="A")
        for p, scen_id in period_scenario_ids.items():
            base_scenario = traffic_emmebank.scenario(scen_id)
            
            #create and calculate @trk_non_pce
            create_attribute = _m.Modeller().tool(
                "inro.emme.data.extra_attribute.create_extra_attribute")
            net_calculator = _m.Modeller().tool(
                "inro.emme.network_calculation.network_calculator") 
            try:
                att = create_attribute("LINK", "@trk_non_pce", "total trucs in non-Pce", 0, overwrite=True, scenario = base_scenario)
            except: #if "@trk_non_pce" has been created
                pass
            cal_spec = {"result": att.id,
                        "expression": "@trk_l_non_pce+@trk_m_non_pce+@trk_h_non_pce",
                        "aggregation": None,
                        "selections": {"link": "mode=d"},
                        "type": "NETWORK_CALCULATION"
                    }
            net_calculator(cal_spec, scenario = base_scenario)
            
            network = base_scenario.get_partial_network(["LINK"], include_attributes=True)

            df, dftr, dfsp = self.export_traffic(export_path, traffic_emmebank, scen_id, network, source_file, p, dfHwycov)
            dfsp[p + "_Speed"] = dfsp[p + "_Speed"].astype(int)
            if p == "EA":
                df_total = df
                dftr_total = dftr
                dfsp_total = dfsp
            else:
                df_total = df_total.join(df[p + "_Flow"])
                dftr_total = dftr_total.join(dftr[p + "_TruckFlow"])
                dfsp_total = dfsp_total.join(dfsp[p + "_Speed"])

        df_total.to_excel(writer, sheet_name='raw', header=True, index=False, startcol=0, startrow=0)
        dftr_total.to_excel(writer, sheet_name='raw', header=True, index=False, startcol=7, startrow=0)
        dfsp_total.to_excel(writer, sheet_name='raw', header=True, index=False, startcol=14, startrow=0)
        writer.save()
        
        #----------------------------------export transit data----------------------------------
        
        desktop = _m.Modeller().desktop
        data_explorer = desktop.data_explorer()

        # try:
        #     data_explorer.add_database(transitbank_path)
        # except:
        #     pass  #if transit database already included in the project
        # all_databases = data_explorer.databases()
        # for database in all_databases:
        #     if "transit" in database.name():
        #         database.open()
        #         break
        for p, scen_id in period_scenario_ids.items():
            try:
                data_explorer.add_database(os.path.join(main_directory, "emme_project", "Database_transit_" + p, "emmebank"))
            except:
                pass  #if transit database already included in the project
            all_databases = data_explorer.databases()
            for database in all_databases:
                if ("transit-" + p) in database.name():
                    database.open()
                    break
            scen = database.scenario_by_number(scen_id)
            data_explorer.replace_primary_scenario(scen)
            self.export_transit(export_path, desktop, p)
            # -----------------close or remove transit databack from the project-----------------
            database.close()
            if "T:" not in main_directory:
                data_explorer.remove_database(database)


        all_databases = data_explorer.databases()
        for database in all_databases:
            if "transit" not in database.name():
                database.open()
                break

        #------Combine into one datafram and write out
        routeDict = {'c':23, 'l':24, 'y':25, 'r':26, 'p':27, 'e':28, 'b':29}
        filenames = []
        for p in periods:
            file = os.path.join(export_path, "transit_" + p + ".csv")
            filenames.append(file)

        df_detail = []
        df_board = []
        df_passMile = []

        for f in filenames:
            df_detail.append(pd.read_csv(f))

        pnum = 0
        for datafm in df_detail:
            datafm['route'] = datafm.apply(lambda row: str(int(row.Line/1000))+row.Mode, axis = 1)
            df_board.append(datafm.groupby(['route'])['Pass.'].agg('sum').reset_index())
            df_board[pnum].rename(columns = {'Pass.':periods[pnum]+'_Board'}, inplace=True)

            df_passMile.append(datafm.groupby(['route'])['Pass. dist.'].agg('sum').reset_index())
            df_passMile[pnum].rename(columns = {'Pass. dist.':periods[pnum]+'_PsgrMile'}, inplace=True)

            pnum += 1

        frame1 = reduce(lambda x,y: pd.merge(x,y, on='route', how='outer'), df_board )
        frame2 = reduce(lambda x,y: pd.merge(x,y, on='route', how='outer'), df_passMile )
        frame = reduce(lambda x,y: pd.merge(x,y, on='route', how='outer'), [frame1,frame2])

        idx = 1
        frame.insert(loc=idx, column='mode_transit_route_id', value="")
        frame['mode_transit_route_id'] = frame['route'].apply(lambda x: routeDict[x[-1]])
        frame['route'] = frame['route'].apply(lambda x: int(x[:-1]))

        writer = pd.ExcelWriter(source_file, engine='openpyxl')
        book = openpyxl.load_workbook(source_file)
        writer.book = book
        writer.sheets = dict((ws.title, ws) for ws in book.worksheets)
        frame.to_excel(writer, sheet_name='transit_general', header=True, index=False, startcol=1, startrow=0)
        writer.save()

        for p in periods:
            file = os.path.join(export_path, "transit_" + p + ".csv")
            os.remove(file)
        
    @_m.logbook_trace("Export traffic load data by period - validaton")
    def export_traffic(self, export_path, traffic_emmebank, scen_id, network, filename, period, dfHwycov):
        def get_network_value(attrs, emmeAttrName, headerStr, df):
            reverse_link = link.reverse_link
            key, att = attrs[0]  # expected to be the link id
            values = [id_format(link[att])]
            reverse_link = link.reverse_link
            for key, att in attrs[1:]:
                if key == "AN":
                    values.append(link.i_node.id)
                elif key == "BN":
                    values.append(link.j_node.id)
                elif key.startswith("BA"):
                    print("line 148 key, att", key, att)
                    name, default = att
                    if reverse_link and (abs(link["@tcov_id"]) == abs(reverse_link["@tcov_id"])):
                        values.append(format(reverse_link[name]))
                    else:
                        values.append(default)
                elif att.startswith("#"):
                    values.append('"%s"' % link[att])
                else:
                    values.append(format(link[emmeAttrName]))
                df = df.append({'TCOVID': values[0], period + headerStr: values[1]}, ignore_index=True)
                df['TCOVID'] = df['TCOVID'].astype(float)
                df[period + headerStr] = df[period + headerStr].astype(float)
            return df

        # only the original forward direction links and auto links only
        hwyload_attrs = [("TCOVID", "@tcov_id"), (period + "_Flow", "@non_pce_flow")]
        trkload_attrs = [("TCOVID", "@tcov_id"), (period + "_TruckFlow", "@trk_non_pce")]
        speedload_attrs = [("TCOVID", "@tcov_id"), (period + "_Speed", "@speed")]
        df = pd.DataFrame(columns=['TCOVID', period + "_Flow"])
        dftr = pd.DataFrame(columns=['TCOVID', period + "_TruckFlow"])
        dfsp = pd.DataFrame(columns=['TCOVID', period + "_Speed"])
        print("scen_id", scen_id)
        auto_mode = network.mode("d")
        scenario = traffic_emmebank.scenario(scen_id)
        links = [l for l in network.links() if  (auto_mode in l.modes or (l.reverse_link and auto_mode in l.reverse_link.modes))]
        #links = [l for l in network.links() if l["@tcov_id"] > 0 and (auto_mode in l.modes or (l.reverse_link and auto_mode in l.reverse_link.modes))]
        links.sort(key=lambda l: l["@tcov_id"])
    
        for link in links:
            if link["@tcov_id"] in dfHwycov['TCOVID'].tolist():
                reverse_link = link.reverse_link
                df = get_network_value(hwyload_attrs, '@non_pce_flow', "_Flow", df)
                dftr = get_network_value(trkload_attrs, '@trk_non_pce', "_TruckFlow", dftr)
                dfsp = get_network_value(speedload_attrs, '@speed', "_Speed", dfsp)
        return df, dftr, dfsp

    @_m.logbook_trace("Export transit load data by period - validaton")
    def export_transit(self, export_path, desktop, p):
        project_table_db = desktop.project.data_tables()
        ws_path = ["General", "Results Analysis", "Transit", "Summaries", "Summary by line"]
        root_ws_f = desktop.root_worksheet_folder()
        table_item = root_ws_f.find_item(ws_path)
        transit_table = table_item.open()

        #for i in delete_table_list:
        #    transit_table.delete_column(i)
        transit_dt = transit_table.save_as_data_table("Transit_Summary", overwrite=True)
        transit_table.close()

        transit_data = transit_dt.get_data()
        project_path = r'T:\ABM\ABM_FY19\model_runs\ABM2Plus\SenTests4TAC'
        transit_filepath = os.path.join(export_path, "transit_"+ p +".csv")
        transit_data.export_to_csv(transit_filepath, separator=",")

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg