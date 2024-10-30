# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np
import pandas as pd

from activitysim.core import (
    config,
    tracing,
    workflow,
)

logger = logging.getLogger(__name__)


@workflow.step
def airport_returns(
    state: workflow.State,
    trips: pd.DataFrame,
    model_settings_file_name: str = "airport_returns.yaml",
    trace_label: str = "airport_returns",
    trace_hh_id: bool = False,
):
    """
    This model updates the airport trip list to include return trips for drop off
    passengers. The output is a larger trip list duplicating the trips which are dropped
    off at the airport to return to their origin.
    The main interface to the airport returns model is the airport_returns() function.
    """
    logger.info("Running %s with %d trips", trace_label, len(trips))

    model_settings = config.read_model_settings(model_settings_file_name)

    returning_modes = model_settings["RETURN_MODE_SEGMENTS"]
    trip_returns = trips.copy()
    trip_returns = trip_returns[trip_returns.trip_mode.isin(returning_modes)]
    trip_returns["return_origin"] = trip_returns["destination"]
    trip_returns["return_dest"] = trip_returns["origin"]
    trip_returns["origin"] = trip_returns["return_origin"]
    trip_returns["destination"] = trip_returns["return_dest"]
    trip_returns["outbound"] = ~trip_returns["outbound"]
    trip_returns["trip_num"] = 2
    trip_returns["trip_count"] = 2
    trip_returns["primary_purpose"] = trip_returns["primary_purpose"].map(
        lambda n: "{}_return".format(n)
    )
    trip_returns = trip_returns.drop(["return_origin", "return_dest"], axis=1)
    trip_returns["trip_id"] = np.arange(
        trips.index.max() + 1, trips.index.max() + 1 + len(trip_returns)
    )
    trip_returns = trip_returns.set_index("trip_id")
    trips = trips.append(trip_returns)

    state.replace_table("trips", trips)

    tracing.print_summary("airport_returns", trips.returns, value_counts=True)

    if state.settings.trace_hh_id:
        state.tracing.trace_df(trips, label=trace_label, warn_if_empty=True)
