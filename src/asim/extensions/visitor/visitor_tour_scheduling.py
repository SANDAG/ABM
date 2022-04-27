import itertools
import copy
import seaborn as sns
import matplotlib.pyplot as plt
from scipy.interpolate import Rbf
import os
import yaml
import pandas as pd


def create_tour_scheduling_probs(tod_probs, parameters):
    # convert CTRAMP 40-period to 48 period asim

    # Interpolate missing time periods
    tod_probs_extra = interpolate_tour_probs(tod_probs, parameters)

    # Concatenate entry/return periods into i_j formatted columns
    tod_probs_extra['period'] = tod_probs_extra[['EntryPeriod', 'ReturnPeriod']].astype(str).agg('_'.join, axis=1)
    # Reshape from long to wide
    tod_probs_extra = tod_probs_extra.pivot(index='Purpose', columns='period', values='Percent').reset_index()
    # Relabel cols/rows
    tod_probs_extra = tod_probs_extra.rename(columns={'Purpose': 'purpose_id'})

    # Create and save tour_departure_and_duration_alternatives
    pd.DataFrame(itertools.product(range(1, 49), repeat=2), columns=['start', 'end']).to_csv(
        os.path.join(
            parameters['config_dir'],
            parameters['output_fname']['tour_scheduling_alts'])
    )

    # Save to CSV
    tod_probs_extra.to_csv(os.path.join(
        parameters['config_dir'],
        parameters['output_fname']['tour_scheduling_probs'])
    )

    # Create associated yaml
    tod_probs_spec = {'PROBS_SPEC': 'tour_scheduling_probs.csv',
                      'PROBS_JOIN_COLS': ['purpose_id']}

    with open(os.path.join(parameters['config_dir'], 'tour_scheduling_probabilistic.yaml'), 'w') as file:
        yaml.dump(tod_probs_spec, file)

    return tod_probs_extra


def tod_aggregate_melt(todi):
    todiagg = []
    for x in ['EntryPeriod', 'ReturnPeriod']:
        agg = todi.groupby(x)['Percent'].sum().reset_index().rename(columns={x: 'Time Period'})
        agg['Period Type'] = x
        todiagg.append(agg)
    todiagg = pd.concat(todiagg)
    newperiods = (todiagg['Time Period'] == 1) | ((todiagg['Time Period'] >= 40) & (todiagg['Time Period'] <= 48))

    todiagg.loc[newperiods, 'Period Type'] = todiagg.loc[newperiods, 'Period Type'] + ' (extrapolated)'

    return todiagg.reset_index()


def tod_plots(purpose_id, probsxi, parameters):
    sns.heatmap(data=probsxi.pivot("EntryPeriod", "ReturnPeriod", "Percent"))
    plt.savefig(os.path.join(parameters['plot_dir'], 'tod_heatmap_{}.png'.format(purpose_id)))
    plt.show()
    # Aggregate view
    todiagg = tod_aggregate_melt(probsxi)
    sns.set_palette("Paired")
    # sns.barplot(data=todiagg, x='Time Period', y='Percent', hue='Period Type', dodge=False, alpha=0.3)
    sns.scatterplot(data=todiagg, x='Time Period', y='Percent', hue='Period Type')
    plt.xticks(list(range(0, 49, 4)), labels=list(range(0, 49, 4)))
    plt.savefig(os.path.join(parameters['plot_dir'], 'tod_{}.png'.format(purpose_id)))
    plt.show()


def expand_square(probs_clipped):
    # Inform Temporal Loop (i.e., tell the computer that 24hr day repeats and 41-48 starts back at 1)
    extend = {'diag': ['EntryPeriod', 'ReturnPeriod'], 'top': ['EntryPeriod'], 'right': ['ReturnPeriod']}

    # Expand to full 1-48 matrix size
    combos = itertools.product(range(2, 50), repeat=2)
    probs_base = pd.DataFrame(combos, columns=["EntryPeriod", "ReturnPeriod"])
    probs_base = probs_base.merge(probs_clipped.drop(columns='Purpose'), on=['EntryPeriod', 'ReturnPeriod'],
                                  how='outer')

    probsx = copy.deepcopy(probs_base)
    for side, cols in extend.items():
        probsy = copy.deepcopy(probs_base)
        probsy[cols] += 48
        probsx = pd.concat([probsx, probsy])

    return probsx


def interpolate_tour_probs(tod_probs, parameters):
    tod_probs_extra = []
    # purpose_id, probs = list(tod_probs.groupby('Purpose'))[0]
    for purpose_id, probs in tod_probs.groupby('Purpose'):

        # Remove 1 and 40 for fitting, storing for later
        probs_clipped = probs[~((probs.EntryPeriod == 1) | (probs.ReturnPeriod == 1)) &
                              ~((probs.EntryPeriod == 40) | (probs.ReturnPeriod == 40))]

        # Extract into x,y,z for interpolation
        x, y, z = [probs_clipped[col].values for col in ['EntryPeriod', 'ReturnPeriod', 'Percent']]
        # Radial Function Interpolation
        rbf = Rbf(x, y, z, function='gaussian')

        # Expand the grid to loop over 48 time periods to 96, interpolate the missing in between
        probsx = expand_square(probs_clipped)

        # Extrapolate the missing points
        probsxi = copy.deepcopy(probsx)
        nulls = probsxi.Percent.isnull()
        probsxi.loc[nulls, 'Percent'] = rbf(probsxi[nulls].EntryPeriod, probsxi[nulls].ReturnPeriod)
        # Set 49 as 1
        probsxi.loc[probsxi.EntryPeriod == 49, 'EntryPeriod'] = 1
        probsxi.loc[probsxi.ReturnPeriod == 49, 'ReturnPeriod'] = 1

        # Extract imputed tables, ditching the extra cycled data
        probsxi = probsxi[(probsxi.EntryPeriod <= 48) & (probsxi.ReturnPeriod <= 48)]

        # Set floor to 0 just in case any go below 0
        probsxi.loc[probsxi.Percent < 0, 'Percent'] = 0

        # Re-scale the 1st and 40th half hours for the interpolated 45-48 and 40-44 half hours
        first_sum = probs[(probs.EntryPeriod == 1) | (probs.ReturnPeriod == 1)].Percent.sum()
        last_sum = probs[(probs.EntryPeriod == 40) | (probs.ReturnPeriod == 40)].Percent.sum()

        first_filt = (probsxi.EntryPeriod.isin([1, 45, 46, 47, 48]) | probsxi.ReturnPeriod.isin([1, 45, 46, 47, 48]))
        last_filt = (probsxi.EntryPeriod.isin([40, 41, 42, 43, 44]) | probsxi.ReturnPeriod.isin([40, 41, 42, 43, 44]))

        # Normalize the target time periods to be out of 1
        probsxi.loc[first_filt, 'Percent'] /= probsxi.loc[first_filt, 'Percent'].sum()
        probsxi.loc[last_filt, 'Percent'] /= probsxi.loc[last_filt, 'Percent'].sum()

        # Scale to the original size that was in the 1 and 40th periods
        probsxi.loc[first_filt, 'Percent'] *= first_sum
        probsxi.loc[last_filt, 'Percent'] *= last_sum

        # Re-normalize to unit scale
        # probsxi['Percent'] = probsxi.Percent / probsxi.Percent.sum()

        if parameters['plot_figs']:
            tod_plots(purpose_id, probsxi, parameters)

        probsxi['Purpose'] = purpose_id

        tod_probs_extra.append(probsxi)

    # Concatenate the imputed tables
    tod_probs_extra = pd.concat(tod_probs_extra)

    return tod_probs_extra
