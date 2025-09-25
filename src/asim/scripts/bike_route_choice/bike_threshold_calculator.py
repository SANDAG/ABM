import sys, time, logging
import pandas as pd
import matplotlib.pyplot as plt
from bike_route_choice import load_settings, run_bike_route_choice

def plot_results(results):
    fig, ax1 = plt.subplots()
    ax2 = ax1.twinx()
    ax1.grid(True, which='both', axis='x', linestyle='-', color='#48484a', alpha=1,zorder=0)
    ax1.grid(True, which='both', axis='y', linestyle='-', color='#006fa1', alpha=1,zorder=0)
    ax2.grid(True, which='both', axis='y', linestyle='--', color='#f68b1f', alpha=0.8,zorder=0)
    ax1.set_facecolor('#dcddde')
    ax1.set_ylabel('99th %%ile Distance (mi)',color='#006fa1')
    ax1.tick_params(axis='y',labelcolor='#006fa1')

    ax2.set_ylabel('Runtime (sec)',color='#f68b1f',rotation=-90,labelpad=15)
    ax2.tick_params(axis='y',labelcolor='#f68b1f')

    ax1.set_xlabel('Utility Threshold (utiles)')
    dist_plot = ax1.scatter(results.index,results.dist,color='#006fa1',label='99th %%ile Distance',zorder=3)
    runtime_plot = ax2.scatter(results.index,results.runtime,color='#f68b1f',label='Runtime',zorder=3,marker='s',alpha=0.8)
    ymin, ymax= ax2.get_ylim()
    padding = 0.1*(ymax-ymin)
    ax2.set_ylim(ymin-padding,ymax+padding)
    legend = ax2.legend(handles=[dist_plot,runtime_plot],labels=['Distance','Runtime'],loc='upper left')
    legend.get_frame().set_facecolor('white')
    legend.get_frame().set_alpha(1)
    legend.get_frame().set_edgecolor('#48484a')

    plt.show()

if __name__=="__main__":
    assert len(sys.argv) > 2,"""Usage:
    python bike_threshold_calculator.py <settings filepath> <target distance> [target_margin [percentile [max_iterations]]]

    parameters:
        settings filepath:  path to YAML file containing bike model settings
        target distance:    the distance for which the search should aim (in miles)
        target margin:      the margin of error (< 1) allowed before termination (optional, default: 0.1)
        percentile:         the percentile of distance to compare against the target (optional, default: 0.99)
        max iterations:     the most bike model iterations that can be performed in the search (optional, default: 20)

    examples:
        
        python bike_threshold_calculator.py bike_route_choice_settings_taz.yaml 20 
        # the resulting 99th %%ile distance must be w/in 10%% of the 20-mile target distance
        # equivalent to:
            python bike_threshold_calculator.py bike_route_choice_settings_taz.yaml 20 0.1 0.99 20
        
        python bike_threshold_calculator.py bike_route_choice_settings_mgra.yaml 3 0.05
        # the resulting 99th %%ile distance must be w/in 5%% of the three-mile target distance
    """

    # pass settings file as command line argument
    settings_file = sys.argv[1]

    target_distance = float(sys.argv[2])
    
    if len(sys.argv) > 3:
        target_margin = float(sys.argv[3])
    else:
        target_margin = 10/100 # 10%
    
    if len(sys.argv) > 4:
        pctile = float(sys.argv[4])
    else:
        pctile = 0.99
    
    if len(sys.argv) > 5:
        max_iterations = int(sys.argv[5])
    else:
        max_iterations = 20

    logger = logging.getLogger(__name__)

    # load settings
    settings = load_settings(settings_file)

    # first x data point
    cur_threshold = settings.max_dijkstra_utility
    upper_bound = None
    lower_bound = None

    distances = {}
    times = {}

    iteration = 0
    while True:
        logger.info(f"Running w/ threshold {cur_threshold}")
        start_time = time.time()
        output = run_bike_route_choice(settings, logger)
        end_time = time.time()
        elapsed_time = end_time - start_time

        # record runtime
        times[cur_threshold] = elapsed_time

        # calculate the 99th %ile of distances
        cur_distance = output.distance.quantile(pctile) 
        logger.info(f"99th %ile distance: {cur_distance}")

        distances[cur_threshold] = cur_distance

        # termination condition
        if abs(cur_distance - target_distance)/target_distance < target_margin or iteration >= max_iterations:
            break

        else:
            if cur_distance > target_distance:
                # distance is too high, so reduce threshold

                upper_bound = cur_threshold

                # if we don't have a lower bound yet
                if lower_bound is None:
                    # just halve the current threshold
                    cur_threshold = cur_threshold / 2

                else:
                    # move halfway to the lower bound
                    cur_threshold = cur_threshold - (cur_threshold - lower_bound)/2
                

            else: # cur_distance < target_distance
                # distance is too low, so increase threshold

                lower_bound = cur_threshold

                # if we don't have an upper bound yet
                if upper_bound is None:
                    # just double the current threshold
                    cur_threshold = cur_threshold * 2
                
                else:
                    # move halfway to the upper bound
                    cur_threshold = cur_threshold + (upper_bound - cur_threshold)/2
            
            settings.max_dijkstra_utility = cur_threshold

        iteration +=1

    results = pd.DataFrame({'dist':distances,'runtime':times})
    results.index.name = 'threshold'

    plot_results(results)

    results.to_csv(settings.output_path+"/threshold_results.csv")