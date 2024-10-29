#//////////////////////////////////////////////////////////////////////////////
#////                                                                       ///
#//// Copyright RSG, 2019-2020.                                            ///
#//// Rights to use and modify are granted to the                           ///
#//// San Diego Association of Governments and partner agencies.            ///
#//// This copyright notice must be preserved.                              ///
#////                                                                       ///
#//// import/run4Ds.py                                              ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#////                                                                       ///
#//////////////////////////////////////////////////////////////////////////////
#
# Generates density variables and adds in mgra socio economic variables
#
#
# Inputs:
#   path: path to the current scenario
#   ref_path: path to the comparison model scenario
#   int_radius: buffer radius for intersection counts
#   maps: default unchecked - means not generating spatial heat maps for 
#         intersection counts. This functionality requires 
#         following packages; geopandas, folium, and branca
#
# File referenced:
#   input\mgra13_based_input2016.csv
#   input\SANDAG_Bike_Net.dbf
#   input\SANDAG_Bike_Node.dbf
#   output\walkMgraEquivMinutes.csv
#
# Script example
# python C:\ABM_runs\maint_2019_RSG\Tasks\4ds\emme_toolbox\emme\toolbox\import\run4Ds.py 
#       0.65 r'C:\ABM_runs\maint_2019_RSG\Model\ABM2_14_2_0' r'C:\ABM_runs\maint_2019_RSG\Model\abm_test_fortran_4d' 


TOOLBOX_ORDER = 10

#import modules
import inro.modeller as _m
from simpledbf import Dbf5
import os
import pandas as pd, numpy as np
#import datetime
import matplotlib.pyplot as plt
import seaborn as sns
import warnings
import traceback as _traceback

warnings.filterwarnings("ignore")

_join = os.path.join
_dir = os.path.dirname

gen_utils = _m.Modeller().module("sandag.utilities.general")

class FourDs(_m.Tool()):

    path = _m.Attribute(str)
    ref_path = _m.Attribute(str)
    int_radius = _m.Attribute(float)
    maps = _m.Attribute(bool)

    tool_run_msg = ""

    @_m.method(return_type=str)
    def tool_run_msg_status(self):
        return self.tool_run_msg    
    
    def __init__(self):
        self._log = []
        self._error = []
        project_dir = _dir(_m.Modeller().desktop.project.path)
        self.path = _dir(project_dir)
        self.mgradata_file = ''
        self.equivmins_file = ''
        self.inNet = ''
        self.inNode = ''
        self.ref_path = ''
        self.maps = False
        self.int_radius = 0.65 #mile
        self.oth_radius = self.int_radius #same as intersection radius
        self.new_cols = ['totint','duden','empden','popden','retempden','totintbin','empdenbin','dudenbin','PopEmpDenPerMi']
        self.continuous_fields = ['totint', 'popden', 'empden', 'retempden']
        self.discrete_fields = ['totintbin', 'empdenbin', 'dudenbin']
        self.mgra_shape_file = ''
        self.base = pd.DataFrame()
        self.build = pd.DataFrame()
        self.mgra_data = pd.DataFrame()
        self.base_cols = []
        self.attributes = ["path", "int_radius", "ref_path"]

    def page(self):
        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(self.path, "conf", "sandag_abm.properties"))
        self.ref_path = props["visualizer.reference.path"]
                    
        pb = _m.ToolPageBuilder(self)
        pb.title = "Run 4Ds"
        pb.description = """
        Generate Density Variables. 
        Generated from MGRA socio economic file and active transportation (AT) network. 
        <br>
        <div style="text-align:left">
        The following files are used:
        <br>
            <ul>
                <li>input\mgra13_based_input2016.csv</li>
                <li>input\SANDAG_Bike_Net.dbf</li>
                <li>input\SANDAG_Bike_Node.dbf</li>
                <li>output\walkMgraEquivMinutes.csv</li>
            </ul>
        </div>
        """
        pb.branding_text = "SANDAG - Run 4Ds"

        if self.tool_run_msg != "":
            pb.tool_run_status(self.tool_run_msg_status)

        pb.add_select_file("path", window_type="directory", file_filter="", 
                           title="Source directory:",)

        pb.add_text_box("int_radius", size=6, title="Buffer size (miles):")
        #pb.add_checkbox("maps", title=" ", label="Generate 4D maps")
        pb.add_select_file("ref_path", window_type="directory", file_filter="", title="Reference directory for comparison")

        return pb.render()
  
    def run(self):
        self.tool_run_msg = ""
        try:
            self(path=self.path, int_radius=self.int_radius, ref_path=self.ref_path)
            run_msg = "Run 4Ds complete"
            self.tool_run_msg = _m.PageBuilder.format_info(run_msg, escape=False)
        except Exception as error:
            self.tool_run_msg = _m.PageBuilder.format_exception(
                error, _traceback.format_exc(error))
            raise
        
    def __call__(self, path= "", 
                 int_radius = 0.65,
                 ref_path = ""):
        _m.logbook_write("Started running 4Ds ...")

        self.path = path
        self.ref_path = ref_path
        self.int_radius = int_radius
        #self.maps = maps

        load_properties = _m.Modeller().tool('sandag.utilities.properties')
        props = load_properties(_join(self.path, "conf", "sandag_abm.properties"))

		
        self.mgradata_file = props["mgra.socec.file"] #input/filename
        self.syn_households_file = props["PopulationSynthesizer.InputToCTRAMP.HouseholdFile"] #input/filename
        self.equivmins_file = props["active.logsum.matrix.file.walk.mgra"] #filename
        self.inNet = os.path.basename(props["active.edge.file"])  #filename
        self.inNode = os.path.basename(props["active.node.file"])  #filename

        attributes = {
            "path": self.path,
            "ref_path": self.ref_path,
            "int_radius": self.int_radius,
            "maps": self.maps,
        }
        gen_utils.log_snapshot("Run 4Ds", str(self), attributes)
        
        file_paths = [_join(self.path, self.mgradata_file),_join(self.path, self.syn_households_file),_join(self.path, "output", self.equivmins_file),  _join(self.path, "input", self.inNet),  _join(self.path, "input", self.inNode)]
        for path in file_paths:
            if not os.path.exists(path):
                raise Exception("missing file '%s'" % (path))
        
        self.mgra_data = pd.read_csv(os.path.join(self.path,self.mgradata_file))
        self.base_cols = self.mgra_data.columns.tolist()
           
        _m.logbook_write("Tagging intersections to mgra")
        self.get_intersection_count()
        
        _m.logbook_write("Generating density variables")
        self.get_density()
        
        # _m.logbook_write("Creating comparison plots")
        # self.make_plots()
        
        _m.logbook_write("Finished running 4Ds")

    def get_intersection_count(self):
        links = Dbf5(_join(self.path, "input", self.inNet))
        links = links.to_dataframe()

        nodes = Dbf5(_join(self.path, "input", self.inNode))
        nodes = nodes.to_dataframe()

        nodes_int = nodes.loc[(nodes.NodeLev_ID < 100000000)]

        #links
        #remove taz, mgra, and tap connectors
        links = links.loc[(links.A <100000000) & (links.B <100000000)]

        #remove freeways (Func_Class=1), ramps (Func_Class=2), and others (Func_Class =0  or -1)
        links = links.loc[(links.Func_Class > 2)]
        links['link_count'] = 1

        #aggregate by Node A and Node B
        links_nodeA = links[['A', 'link_count']].groupby('A').sum().reset_index()
        links_nodeB = links[['B', 'link_count']].groupby('B').sum().reset_index()

        #merge the two and keep all records from both dataframes (how='outer')
        nodes_linkcount = pd.merge(links_nodeA, links_nodeB, left_on='A', right_on='B', how = 'outer')
        nodes_linkcount = nodes_linkcount.fillna(0)
        nodes_linkcount['link_count'] = nodes_linkcount['link_count_x'] + nodes_linkcount['link_count_y']

        #get node id from both dataframes
        nodes_linkcount['N']=0
        nodes_linkcount['N'][nodes_linkcount.A>0] = nodes_linkcount['A']
        nodes_linkcount['N'][nodes_linkcount.B>0] = nodes_linkcount['B']
        nodes_linkcount['N']=nodes_linkcount['N'].astype(float)
        nodes_linkcount = nodes_linkcount[['N','link_count']]

        #keep nodes with 3+ link count
        intersections_temp = nodes_linkcount.loc[nodes_linkcount.link_count>=3]

        #get node X and Y
        intersections = pd.merge(intersections_temp,nodes_int[['NodeLev_ID','XCOORD','YCOORD']], left_on = 'N', right_on = 'NodeLev_ID', how = 'left')
        intersections = intersections[['N','XCOORD','YCOORD']]
        intersections = intersections.rename(columns = {'XCOORD': 'X', 'YCOORD': 'Y'})

        mgra_nodes = nodes[nodes.MGRA > 0][['MGRA','XCOORD','YCOORD']]
        mgra_nodes.columns = ['mgra','x','y']
        int_dict = {}
        for int in intersections.iterrows():
            mgra_nodes['dist'] = np.sqrt((int[1][1] - mgra_nodes['x'])**2+(int[1][2] - mgra_nodes['y'])**2)
            int_dict[int[1][0]] = mgra_nodes.loc[mgra_nodes['dist'] == mgra_nodes['dist'].min()]['mgra'].values[0]
            
        intersections['near_mgra'] = intersections['N'].map(int_dict)
        intersections = intersections.groupby('near_mgra', as_index = False).count()[['near_mgra','N']].rename(columns = {'near_mgra':'mgra','N':'icnt'})
        try:
            self.mgra_data = self.mgra_data.drop('icnt',axis = 1).merge(intersections, how = 'outer', on = "mgra")
        except:
            self.mgra_data = self.mgra_data.merge(intersections, how = 'outer', on = "mgra")

    def get_density(self):
        if len(self.mgra_data) == 0:
           mgra_landuse = pd.read_csv(os.path.join(self.path, self.mgradata_file))
        else:
           mgra_landuse = self.mgra_data
        
		# get population from synthetic population instead of mgra data file
        syn_pop = pd.read_csv(os.path.join(self.path, self.syn_households_file))
        syn_pop = syn_pop.rename(columns = {'MGRA':'mgra'})[['persons','mgra']].groupby('mgra',as_index = False).sum()
        #remove if 4D columns exist
        for col in self.new_cols:
            if col in self.base_cols:
                self.base_cols.remove(col)
                mgra_landuse = mgra_landuse.drop(col,axis=1)
        
		#merge syntetic population to landuse
        mgra_landuse = mgra_landuse.merge(syn_pop, how = 'left', on = 'mgra')
        #all street distance  
        equiv_min = pd.read_csv(_join(self.path, "output", self.equivmins_file))
        print("MGRA input landuse: " + self.mgradata_file)
        
        def density_function(mgra_in):
            eqmn = equiv_min[equiv_min['i'] == mgra_in]
            mgra_circa_int = eqmn[eqmn['DISTWALK'] < self.int_radius]['j'].unique()
            mgra_circa_oth = eqmn[eqmn['DISTWALK'] < self.oth_radius]['j'].unique()
            totEmp = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_oth)]['emp_total'].sum()
            totRet = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_oth)]['emp_ret'].sum()
            totHH = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_oth)]['hh'].sum()
            totPop = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_oth)]['persons'].sum()
            totAcres = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_oth)]['land_acres'].sum()
            totInt = mgra_landuse[mgra_landuse.mgra.isin(mgra_circa_int)]['icnt'].sum()
            if(totAcres>0):
                empDen = totEmp/totAcres
                retDen = totRet/totAcres
                duDen = totHH/totAcres
                popDen = totPop/totAcres
                popEmpDenPerMi = (totEmp+totPop)/(totAcres/640) #Acres to miles
                tot_icnt = totInt
            else:
                empDen = 0
                retDen = 0
                duDen = 0
                popDen = 0
                popEmpDenPerMi = 0
                tot_icnt = 0
            return tot_icnt,duDen,empDen,popDen,retDen,popEmpDenPerMi
    
        #new_cols = [0-'totint',1-'duden',2-'empden',3-'popden',4-'retempden',5-'totintbin',6-'empdenbin',7-'dudenbin',8-'PopEmpDenPerMi']
        mgra_landuse[self.new_cols[0]],mgra_landuse[self.new_cols[1]],mgra_landuse[self.new_cols[2]],mgra_landuse[self.new_cols[3]],mgra_landuse[self.new_cols[4]],mgra_landuse[self.new_cols[8]] = zip(*mgra_landuse['mgra'].map(density_function))

        mgra_landuse = mgra_landuse.fillna(0)
        mgra_landuse[self.new_cols[5]] = np.where(mgra_landuse[self.new_cols[0]] < 80, 1, np.where(mgra_landuse[self.new_cols[0]] < 130, 2, 3))
        mgra_landuse[self.new_cols[6]] = np.where(mgra_landuse[self.new_cols[2]] < 10, 1, np.where(mgra_landuse[self.new_cols[2]] < 30, 2,3))
        mgra_landuse[self.new_cols[7]] = np.where(mgra_landuse[self.new_cols[1]] < 5, 1, np.where(mgra_landuse[self.new_cols[1]] < 10, 2,3))

        mgra_landuse[self.base_cols+self.new_cols].to_csv(os.path.join(self.path, self.mgradata_file), index = False, float_format='%.4f' )
        
        self.mgra_data = mgra_landuse
        print( "*** Finished ***")

    #plot comparisons of build and old density values and create heat maps
    def make_plots(self):
        if len(self.mgra_data) == 0:
            self.build = pd.read_csv(os.path.join(self.path, self.mgradata_file))
        else:
            self.build = self.mgra_data
                
        def plot_continuous(field):
            #colors
            rsg_orange = '#f68b1f'
            rsg_marine = '#006fa1'
            #rsg_leaf   = '#63af5e'
            #rsg_grey   = '#48484a'
            #rsg_mist   = '#dcddde'
                        
            max = self.base[field].max() + self.base[field].max()%5
            div = max/5 if max/5 >= 10 else max/2
            bins = np.linspace(0,max,div)
            plt.hist(self.base[field], bins, density = True, alpha = 0.5, label = 'Base', color = rsg_marine)
            plt.hist(self.build[field], bins, density = True, alpha = 0.5, label = 'Build', color = rsg_orange)
            mean_base = self.base[field].mean()
            mean = self.build[field].mean()
            median_base = self.base[field].median()
            median = self.build[field].median()
            plt.axvline(mean_base, color = 'b', linestyle = '-', label = 'Base Mean')
            plt.axvline(median_base, color = 'b', linestyle = '--', label = 'Base Median')
            plt.axvline(mean, color = 'r', linestyle = '-', label = 'Build Mean')
            plt.axvline(median, color = 'r', linestyle = '--',label = 'Build Median')
            plt.legend(loc = 'upper right')
            ylims = plt.ylim()[1]
            plt.text(mean_base + div/4, ylims-ylims/32, "mean: {:0.2f}".format(mean_base), color = 'b')
            plt.text(mean_base + div/4, ylims - 5*ylims/32, "median: {:0.0f}".format(median_base), color = 'b')
            plt.text(mean_base + div/4, ylims-2*ylims/32, "mean: {:0.2f}".format(mean), size = 'medium',color = 'r')
            plt.text(mean_base + div/4, ylims-6*ylims/32, "median: {:0.0f}".format(median), color = 'r')
            plt.text(self.base[field].min() , ylims/32, "min: {:0.0f}".format(self.base[field].min()), color = 'b')
            plt.text(self.base[field].max()-div , ylims/32, "max: {:0.0f}".format(self.base[field].max()), color = 'b')
            plt.text(self.build[field].min() , 2*ylims/32, "min: {:0.0f}".format(self.build[field].min()), color = 'r')
            plt.text(self.base[field].max()-div , 2*ylims/32, "max: {:0.0f}".format(self.build[field].max()), color = 'r')

            plt.xlabel(field)
            plt.ylabel("MGRA's")
            plt.title(field.replace('den','') + ' Density')
            outfile = _join(self.path, "output", '4Ds_{}_plot.png'.format(field))
            if os.path.isfile(outfile):
                os.remove(outfile)
            plt.savefig(outfile)
            plt.clf()

        def plot_discrete(field):
            fig, ax = plt.subplots()
            df1 = discretedf_base.groupby(field, as_index = False).agg({'mgra':'count','type':'first'})
            df2 = discretedf_build.groupby(field, as_index = False).agg({'mgra':'count','type':'first'})
            df = df1.append(df2)
            ax = sns.barplot(x=field, y = 'mgra', hue = 'type', data = df)
            ax.set_title(field)
            outfile = _join(self.path, "output", '4Ds_{}_plot.png'.format(field))
            if os.path.isfile(outfile):
                    os.remove(outfile)
            ax.get_figure().savefig(outfile)

        self.base = pd.read_csv(os.path.join(self.ref_path, self.mgradata_file))
        self.base['type'] = 'base'
        self.build['type'] = 'build'

        discretedf_base = self.base[['mgra','type']+self.discrete_fields]
        discretedf_build = self.build[['mgra','type']+self.discrete_fields]
        
        for f in self.continuous_fields:
            plot_continuous(f)            
        for f in self.discrete_fields:
            plot_discrete(f)
            
        if self.maps:
            import geopandas as gpd
            import folium
            from branca.colormap import linear
            compare_int = self.base.merge(self.build, how = 'outer', on = 'mgra', suffixes = ['_base','_build'])
            compare_int['diff'] = compare_int['TotInt'] - compare_int['totint']

            compare_int = gpd.read_file(self.mgra_shape_file).rename(columns = {'MGRA':'mgra'}).merge(compare_int, how = 'left', on = 'mgra')
            compare_int = compare_int.to_crs({'init': 'epsg:4326'})

            colormap = linear.OrRd_09.scale(
                    compare_int.TotInt.min(),
                    compare_int.TotInt.max())
            colormapA = linear.RdBu_04.scale(
                    compare_int['diff'].min(),
                    compare_int['diff'].min()*-1)

            compare_int['colordiff'] = compare_int['diff'].map(lambda n: colormapA(n))
            compare_int['colororig'] = compare_int['TotInt'].map(lambda n: colormap(n))
            compare_int['colornew'] = compare_int['totint'].map(lambda n: colormap(n))
            
            def makeheatmap(self,df, colormp,color_field,caption):
                mapname = folium.Map(location=[32.76, -117.15], zoom_start = 13.459)
                folium.GeoJson(compare_int,
                        style_function=lambda feature: {
                        'fillColor': feature['properties'][color_field],
                        'color' : rsg_marine,
                        'weight' : 0,
                        'fillOpacity' : 0.75,
                        }).add_to(mapname)
        
                colormp.caption = caption
                colormp.add_to(mapname)
                return mapname
                        
            makeheatmap(compare_int,colormapA,'colordiff','Intersection Diff (base - build)').save('diff_intersections.html')   
            makeheatmap(compare_int,colormap,'colororig','Intersections').save('base_intersections.html')   
            makeheatmap(compare_int,colormap,'colororig','Intersections').save('build_intersections.html')   
