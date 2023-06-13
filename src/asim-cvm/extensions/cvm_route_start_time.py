import pandas as pd
import numpy as np
from pathlib import Path
from activitysim.core import workflow

from .cvm_enum_tools import as_int_enum
from .cvm_enum import RoutePurposes, VehicleTypes, CustomerTypes, BusinessTypes
from .cvm_route_purpose_and_vehicle import cross_choice_maker

@workflow.step
def route_start_time(
    state: workflow.State,
    routes: pd.DataFrame,
    probability_file: Path = "route_start_times.csv",
) -> None:
    """
    Simulate purpose and vehicle type for routes.

    Parameters
    ----------
    state : State
    routes : pandas.DataFrame
    probability_file

    Returns
    -------

    """
    probability_file_ = state.filesystem.get_config_file_path(probability_file)
    probs = pd.read_csv(probability_file_, header=1)

    probs["route_purpose"] = as_int_enum(
        probs["route_purpose"], RoutePurposes, categorical=True
    )
    probs["vehicle_type"] = as_int_enum(
        probs["vehicle_type"], VehicleTypes, categorical=True
    )
    probs["customer_type"] = as_int_enum(
        probs["customer_type"], CustomerTypes, categorical=True
    )
    probs["business_type"] = as_int_enum(
        probs["business_type"], BusinessTypes, categorical=True
    )

    # rescale probs to ensure they total 1.0
    probs_float_cols = probs.select_dtypes(include=np.number).columns
    probs[probs_float_cols] = probs[probs_float_cols].div(
        probs[probs_float_cols].sum(axis=1), axis=0
    )

    i_keys = ['route_purpose', 'vehicle_type', 'customer_type', 'business_type']
    probs = probs.set_index(i_keys)

    start_times = []

    for routes_keys, routes_chunk in routes.groupby(i_keys):
        r = np.random.default_rng().random(size=len(routes_chunk))
        routes_c_choices = cross_choice_maker(probs.loc[routes_keys].values, r)
        start_times.append(
            pd.Series(
                routes_c_choices, index=routes_chunk.index
            )
        )

    routes = routes.assign(
        start_time=pd.concat(start_times),
    )
    state.add_table("routes", routes)
