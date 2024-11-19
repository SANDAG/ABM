import logging

import numpy as np
import pandas as pd

from activitysim.core import workflow

logger = logging.getLogger(__name__)


@workflow.step
def adjust_auto_operating_cost(state: workflow.State, vehicles: pd.DataFrame):
    """
    Adjusts the `auto_operating_cost` field in the vehicles table
    so that the average is a desired value set as costPerMile in the
    settings

    Parameters
    ----------
    vehicles : pd.DataFrame
    """
    target_auto_operating_cost = state.get_global_constants()["costPerMile"]

    adjustment_factor = (
        target_auto_operating_cost / vehicles["auto_operating_cost"].mean()
    )
    logger.info(
        "Adjusting auto operating costs in vehicles table by a factor of {}".format(
            adjustment_factor
        )
    )
    vehicles["auto_operating_cost"] *= adjustment_factor

    state.add_table("vehicles", vehicles)
