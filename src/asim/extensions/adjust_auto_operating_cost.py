import logging

import numpy as np
import pandas as pd

from activitysim.core import(
    config,
    inject,
    pipeline,
)

logger = logging.getLogger(__name__)

@inject.step()
def adjust_auto_operating_cost(vehicles):
    """Adjusts the `auto_operating_cost` field in the vehicles table
    so that the average is a desired value set as costPerMile in the
    settings

    Parameters
    ----------
    vehicles : orca.DataFrameWrapper
    """
    target_auto_operating_cost = config.get_global_constants()["costPerMile"]
    vehicles = vehicles.to_frame()

    adjustment_factor = target_auto_operating_cost / vehicles["auto_operating_cost"].mean()
    logger.info("Adjusting auto operating costs in vehicles table by a factor of {}".format(adjustment_factor))
    vehicles["auto_operating_cost"] *= adjustment_factor

    pipeline.replace_table("vehicles", vehicles)