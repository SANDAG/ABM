import re
import numpy as np
import pandas as pd
import geopandas as gpd
import folium

from IPython.display import display

import ipywidgets as widgets

np.warnings.filterwarnings('ignore')

CONFIG = {
    'autos': [0, 1, 2],
    'autos_labels': ['No autos', 'Autos > 0,  Less than adults', 'Autos >= Adults']
}


def calc_auto_sufficiency(hh_df, autos_col, hh_size):
    
    def sufficiency(a):
        if a < 1:
            return 0

        if a < hh_size:
            return 1

        return 2

    hh_df[autos_col] = hh_df[autos_col].map(sufficiency)

def create_configs():

    config_strs = [
        ('tour_file', 'Tour File -', 'output/indivTourData_6.csv'),
        ('hh_file', 'Household File -', 'output/householdData_6.csv'),
        ('hh_id_col', 'Tour/Household File HH ID columns (comma separated) -', 'hh_id,hh_id'),
        ('purpose_col', 'Purpose Column -', 'tour_purpose'),
        ('ozone_col', 'Origin Zone Column -', 'orig_mgra'),
        ('dzone_col', 'Destination Zone Column -', 'dest_mgra'),
        ('income_col', 'HH Income Column -', 'income'),
        ('income_labels', 'Income Labels (comma separated) -', 'Low,Medium,High'),
        ('autos_col', 'HH Autos Column -', 'autos'),
        ('hh_size', 'Household Size -', '2'),
        ('mode_labels', 'Mode Labels (comma separated) -', 'DRIVEALONE,SHARED2,SHARED3,WALK,BIKE,WALK_SET,PNR_SET,KNR_SET,TNC_SET,TAXI,TNC_SINGLE,TNC_SHARED,SCHOOLBUS'),
        ('prob_cols', 'Probability Columns (comma separated) -', 'prob_1,prob_2,prob_3,prob_4,prob_5,prob_6,prob_7,prob_8,prob_9,prob_10,prob_11,prob_12,prob_13'),
        ('util_cols', 'Utility Columns (comma separated) -', 'util_1,util_2,util_3,util_4,util_5,util_6,util_7,util_8,util_9,util_10,util_11,util_12,util_13'),
        ('shapefile', 'Shapefile -', 'syn_pop_and_shapefiles/shapefiles/mgra/SANDAG_MGRA.shp'),
        ('zone_col', 'Zone Column - ', 'MGRA'),
        ('zone_crs', 'Shapefile CRS -', 'ESRI:102646'),
        ('layer_shapefiles', 'Layer Shapefiles (optional)', 'output/rtcov.shp, output/tapcov.shp'),
        
    ]
    
    widges = []
    for name, label, default in config_strs:
        w = widgets.Text(description=label, value=default, layout={'width': 'initial'}, style={'description_width': 'initial'})
        w.name = name
        widges.append(w)
    
    inputs = widgets.VBox(widges)
    
    c_output = widgets.Output()
    m_output = widgets.Output()
    
    validate_button = widgets.Button(description='Validate Configs')
    buttons = widgets.HBox([validate_button])
    
    display(inputs, buttons, c_output, m_output)
    
    def validate_configs(b):

        global CONFIG
        for w in widges: 
            if w.name in ['hh_id_col', 'income_labels', 'mode_labels', 'prob_cols', 'util_cols', 'layer_shapefiles']:
                val = re.split(',\s?', w.value.strip())

                if '' in val:
                    val.remove('')

            else:
                val = w.value.strip()

            CONFIG[w.name] = val
        
        with c_output:

            c_output.clear_output()

            print(f"Reading {CONFIG['tour_file']}... ", end='')
            tour_df = pd.read_csv(CONFIG['tour_file'])
            print('Done.')

            print(f"Validating tour file columns... ", end='')
            assert CONFIG['hh_id_col'][0] in tour_df, f"{CONFIG['hh_id_col'][0]} not in tour file"
            assert CONFIG['purpose_col'] in tour_df, f"{CONFIG['purpose_col']} not in tour file"
            assert CONFIG['ozone_col'] in tour_df, f"{CONFIG['ozone_col']} not in tour file"
            assert CONFIG['dzone_col'] in tour_df, f"{CONFIG['dzone_col']} not in tour file"
            purposes = list(tour_df[CONFIG['purpose_col']].unique())

            assert len(CONFIG['prob_cols']) == len(CONFIG['mode_labels']), 'One column per mode is required'
            assert len(CONFIG['util_cols']) == len(CONFIG['mode_labels']), 'One column per mode is required'

            # store min/max of probs and utils for choropleth binning
            probs = tour_df[CONFIG['prob_cols']].values
            utils = tour_df[CONFIG['util_cols']].values

            ozone_list = sorted(list(tour_df[CONFIG['ozone_col']].unique()))
            dzone_list = sorted(list(tour_df[CONFIG['dzone_col']].unique()))
            print('Done.')

            print(f"Reading {CONFIG['hh_file']}... ", end='')
            hh_df = pd.read_csv(CONFIG['hh_file'], index_col=CONFIG['hh_id_col'][1], usecols=[CONFIG['hh_id_col'][1], CONFIG['income_col'], CONFIG['autos_col']])
            print('Done.')

            print('Joining household table to tour table... ', end='')
            calc_auto_sufficiency(hh_df, CONFIG['autos_col'], int(CONFIG['hh_size']))
            tour_df = tour_df.join(hh_df, on=CONFIG['hh_id_col'][0], how='left')

            incomes = sorted(list(tour_df[CONFIG['income_col']].unique()))
            assert len(incomes) == len(CONFIG['income_labels']), f'Income labels must match {incomes}'

            print('Done.')

            print(f"Reading {CONFIG['shapefile']}... ", end='')
            zone_df = gpd.read_file(CONFIG['shapefile'])
            zone_col = CONFIG['zone_col']

            assert zone_col in zone_df, f'{zone_col} not in shapefile'
            print('Done.')

            print('Re-projecting coordinate system... ', end='')
            zone_df.crs = CONFIG['zone_crs']
            zone_df = zone_df.to_crs('epsg:4326')
            center = [zone_df.geometry.centroid.y.mean(), zone_df.geometry.centroid.x.mean()]
            print('Done.')

            layer_dfs = []
            for f in CONFIG['layer_shapefiles']:

                print(f'Reading {f}... ', end='')
                df = gpd.read_file(f)
                df.crs = CONFIG['zone_crs']
                df = df.to_crs('epsg:4326')
                df.name = f
                layer_dfs.append(df)
                print('Done.')

            CONFIG.update({
                'tour_df': tour_df,
                'ozone_list': ozone_list,
                'dzone_list': dzone_list,
                'purposes': purposes,
                'incomes': incomes,
                'zone_df': zone_df,
                'layer_dfs': layer_dfs,
                'center': center,
                # use min/max of probability/utility values from tour df.
                # cap minimum utility at -10
                'prob_range': [probs.min().round(2), probs.max().round(2)],
                'util_range': [max(-9.99, utils.min().round(2)), utils.max().round(2)],
            })

        # display filter controls once configs are validated
        zone_interaction()

    validate_button.on_click(validate_configs)


def filter_tours(zone_num, choice_col, group_col,
                 purpose=None, income=None, autos=None):
    zone_df = CONFIG['zone_df']
    tour_df = CONFIG['tour_df']
    zone_col = CONFIG['zone_col']
    purpose_col = CONFIG['purpose_col']
    income_col = CONFIG['income_col']
    autos_col = CONFIG['autos_col']
    filter_cols = CONFIG['prob_cols'] + CONFIG['util_cols']

    assert zone_num in zone_df[zone_col], f'{zone_col} {zone_num} not in shapefile'

    filtered_tours = tour_df[(tour_df[choice_col] == zone_num)]

    if purpose:
        filtered_tours = filtered_tours[filtered_tours[purpose_col] == purpose]

    if income:
        filtered_tours = filtered_tours[filtered_tours[income_col] == income]

    if autos:
        filtered_tours = filtered_tours[filtered_tours[autos_col] == autos]

    ft = filtered_tours.groupby([group_col]).mean()[filter_cols].round(3)

    return ft


def val_from_label(label_list, value_list, label):

    if label == 'All':
        return None

    idx = label_list.index(label)
    
    return value_list[idx]


FILTER_MAP = None
FILTER_NAME = None

def zone_interaction():

    if 'zone_df' not in CONFIG:
        return

    zone_df = CONFIG['zone_df']
    zone_col = CONFIG['zone_col']
    ozone_col = CONFIG['ozone_col']
    dzone_col = CONFIG['dzone_col']

    direction_labels = {
        'Plot all origins to a single destination zone': {
            'cols': (dzone_col, ozone_col),
            'nickname': 'Destination'},
        'Plot all destinations from a single origin zone': {
            'cols': (ozone_col, dzone_col),
            'nickname': 'Origin'},
    }

    direction = widgets.RadioButtons(options=list(direction_labels.keys()), description='Direction - ', layout={'width': 'max-content'},)
    zone_num = widgets.Dropdown(options=CONFIG['dzone_list'], description='D Zone -')
    
    def handle_direction_change(change):
        nickname = direction_labels[change.new]['nickname']
        if nickname == 'Origin':
            zone_num.options = CONFIG['ozone_list']

        else:
            zone_num.options = CONFIG['dzone_list']

        zone_num.description = f'{nickname[0]} Zone -'

    direction.observe(handle_direction_change, names='value')

    shade = widgets.RadioButtons(options=['Probability', 'Utility'], description='Shade by -')
    bin_range = widgets.FloatRangeSlider(
        value=[0, 1],
        min=0,
        max=1.0,
        step=0.02,
        description='Bin Range -',
        disabled=False,
        continuous_update=True,
        orientation='horizontal',
        readout=True,
        readout_format='.2f',
    ) 

    def handle_shade_change(change):
        if change.new == 'Utility':
            bin_range.min, bin_range.max = CONFIG['util_range']
            bin_range.value = CONFIG['util_range']

        else:
            bin_range.min, bin_range.max = CONFIG['prob_range']
            bin_range.value = CONFIG['prob_range']
            
    shade.observe(handle_shade_change, names='value')

    bin_num = widgets.BoundedIntText(
        value=6,
        min=1,
        max=10,
        step=1,
        description='Bin Count -',
        disabled=False
    )
    mode = widgets.Dropdown(options=CONFIG['mode_labels'], description='Mode -')
    purpose = widgets.Dropdown(options=['All'] + CONFIG['purposes'], description='Purpose -')
    income = widgets.Dropdown(options=['All'] + list(CONFIG['income_labels']), description='Income -')
    autos = widgets.Dropdown(options=['All'] + list(CONFIG['autos_labels']), description='Autos -')

    output = widgets.Output()

    map_button = widgets.Button(description='Show Map')
    save_button = widgets.Button(description='Save Map')

    selectors = widgets.VBox([
        direction,
        zone_num,
        shade,
        bin_range,
        bin_num,
        mode,
        purpose,
        income,
        autos,
    ])
    
    buttons = widgets.HBox([map_button, save_button])
    display(selectors, buttons, output)

    def update_map(b):
        with output:

            map_button.description = 'Updating...'

            choice_col, group_col = direction_labels[direction.value].get('cols')
            direction_type = direction_labels[direction.value].get('nickname')
            purpose_value = None if purpose.value == 'All' else purpose.value

            shade_list = CONFIG['util_cols'] if shade.value == 'Utility' else CONFIG['prob_cols']
            mode_value = val_from_label(CONFIG['mode_labels'], shade_list, mode.value)
            income_value = val_from_label(CONFIG['income_labels'], CONFIG['incomes'], income.value)
            autos_value = val_from_label(CONFIG['autos_labels'], CONFIG['autos'], autos.value)

            ft = filter_tours(
                zone_num.value,
                choice_col,
                group_col,
                purpose=purpose_value,
                income=income_value,
                autos=autos_value)

            ft = ft[(ft[mode_value] >= bin_range.value[0]) &
                    (ft[mode_value] <= bin_range.value[1])]

            bins = pd.cut(bin_range.value, bin_num.value, retbins=True)[1] 

            if ft.empty:
                output.clear_output()
                print('No results.')
                map_button.description = 'Update Map'
                return

            centroid = zone_df[zone_df[zone_col] == zone_num.value].geometry.centroid.iloc[0]
            coords = [float(centroid.y), float(centroid.x)]

            filter_map = folium.Map(location=coords, zoom_start=12, tiles='cartodbpositron')
            marker = folium.Marker(coords, tooltip=f"{direction_type} {zone_col} {zone_num.value}").add_to(filter_map)

    #         filter_zones = ZONE_DF[ZONE_DF.index.isin(list(ft.index) + [zone_num.value])]
            filter_zones = zone_df.join(ft, how='right', on=zone_col)

            # scope: zone_interaction
            filter_name = re.sub("a single .* zone", f'{zone_col} {zone_num.value}', direction.value).replace('Plot ', '')
            filter_name += f', {shade.value}'
            filter_name += f', {mode.value}'
            filter_name = filter_name.title()

            if purpose_value:
                filter_name += f', {purpose.value}'

            if income_value:
                filter_name += f', {income.value}'

            if autos_value:
                filter_name += f', {autos.value}'

            ft.reset_index(inplace=True)

            choropleth = folium.Choropleth(
                geo_data=filter_zones.to_json(),
                name=filter_name,
                data=ft,
                columns=[group_col, mode_value],
                key_on=f'feature.properties.{zone_col}',
                fill_color='BuPu',
                nan_fill_color='white',
                bins=bins,
                fill_opacity=0.7,
                line_opacity=0.1,
                highlight=True,
                smooth_factor=1.0,
                legend_name=shade.value,
            )

            choropleth.add_to(filter_map)

            style_function = "font-size: 15px"
            choropleth.geojson.add_child(
                folium.features.GeoJsonTooltip(
                    fields=[zone_col] + shade_list,
                    style=style_function,
                    aliases=['zone'] + list(CONFIG['mode_labels']),
                    labels=True))

            # add additional map layers, if present
            for df in CONFIG['layer_dfs']:
                if all(df.geometry.type == 'Point'):

                    layer = folium.FeatureGroup(name=df.name)
                    for point in df.geometry:
                        folium.CircleMarker(
                            [point.y, point.x],
                            radius=4,
                            weight=2,
                            fill_color='red',
                            fill_opacity=0.7,
                        ).add_to(layer)

                    layer.add_to(filter_map)

                else:
                    folium.GeoJson(
                        data=df['geometry'],
                        name=df.name,
                    ).add_to(filter_map)

            folium.LayerControl().add_to(filter_map)

            output.clear_output()
            print(f'{len(ft)} results')
            global FILTER_MAP
            FILTER_MAP = filter_map
            global FILTER_NAME
            FILTER_NAME = filter_name
            display(filter_map)
            map_button.description = 'Update Map'
            save_button.description = 'Save Map'

    def save_map(b):
        save_button.description = 'Saving...'
        FILTER_MAP.save(f"{FILTER_NAME.lower().replace(',', '').replace(' ', '-')}.html")
        save_button.description = 'Saved!'

    map_button.on_click(update_map)
    save_button.on_click(save_map)

if __name__ == '__main__':
    create_configs()
