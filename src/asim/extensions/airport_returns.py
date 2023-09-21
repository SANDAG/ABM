# ActivitySim
# See full license in LICENSE.txt.
import logging

import numpy as np

from activitysim.core import tracing
from activitysim.core import config
from activitysim.core import pipeline
from activitysim.core import simulate
from activitysim.core import inject
from activitysim.core import expressions

from activitysim.abm.models.util import estimation

logger = logging.getLogger(__name__)


@inject.step()
def airport_returns(trips, chunk_size, trace_hh_id):
    """
    This model updates the airport trip list to include return trips for drop off
    passengers. The output is a larger trip list duplicating the trips which are dropped
    off at the airport to return to their origin.
    The main interface to the airport returns model is the airport_returns() function.
    """

    trace_label = "airport_returns"
    model_settings_file_name = "airport_returns.yaml"

    trip_list = trips.to_frame()
    logger.info("Running %s with %d trips", trace_label, len(trip_list))

    model_settings = config.read_model_settings(model_settings_file_name)

    returning_modes = model_settings["RETURN_MODE_SEGMENTS"]
    print(trips.trip_mode.unique())
    trip_returns = trip_list.copy()
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
    trip_returns['trip_id'] = np.arange(trip_list.index.max() +1, trip_list.index.max() +1 + len(trip_returns))
    trip_returns = trip_returns.set_index('trip_id')
    trip_list = trip_list.append(trip_returns)

    pipeline.replace_table("trips", trip_list)

    # tracing.print_summary('airport_returns', trips.returns, value_counts=True)

    if trace_hh_id:
        tracing.trace_df(trip_list, label=trace_label, warn_if_empty=True)
