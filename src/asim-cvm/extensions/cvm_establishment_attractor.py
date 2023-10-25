from __future__ import annotations

import logging
from pathlib import Path

import pandas as pd
import numpy as np
from pydantic import validator

from activitysim.core import workflow, tracing
from activitysim.core.configuration.logit import LogitComponentSettings

logger = logging.getLogger(__name__)

class EstablishmentAttractorSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `household_attractor` component.
    """

    RESULT_COL_NAME: str
    """The name of the column in the route table that is created by this component."""

    industry_groups: dict
    """The industry groups."""

    establishment_size_groups: dict
    """The establishment size groups."""



logger = logging.getLogger(__name__)

@workflow.step
def establishment_attractor(
    state: workflow.State,
    land_use: pd.DataFrame,
    establishments: pd.DataFrame,
    model_settings: None = None,
    model_settings_file_name: str = "establishment_attractor.yaml",
    trace_label: str = "establishment_attractor",
) -> None:
    """
    Calculates establishment attractor for commercial vehicle model.

    Parameters
    ----------
    state : workflow.State
    land_use : DataFrame
    establishments : DataFrame
    model_settings : default None
    model_settings_file_name : str, default "establishment_attractor.yaml"
    trace_label : str, default "establishment_attractor"
    """
    
    if model_settings is None:
        model_settings = state.filesystem.read_settings_file(
            model_settings_file_name
        )
    
    trace_label = "cvm_establishment_attractor"

    logger.info("Running %s with synthetic establishments", trace_label)

    # calculate the attractor
    land_use[model_settings.get("RESULT_COL_NAME")] = 0
    establishments["attractors"] = 0

    # step 1 Binary logit model that predicts whether an establishment has at least one attraction.
    # Necessary because there are many which have none in the usrvey.
    logger.info("Running %s step 1 binary logit model", trace_label)
    beta_employment = model_settings.get("CONSTANTS").get("beta_employment")
    # calculate the probability of having an attraction
    establishments["has_attraction_probability"] = (
        1 / (1+np.exp(-beta_employment * establishments["employees"]))
    )
    # get random numbers for the binary logit model
    establishments["random"] = state.get_rn_generator().random_for_df(establishments)
    # calculate whether the establishment has an attraction
    establishments["has_attraction"] = establishments["has_attraction_probability"] > establishments["random"]

    tracing.print_summary(
        "has_attraction",
        establishments["has_attraction"],
        value_counts=True,
    )
    
    # step 2 Poisson model ( y = exp(X*b) )
    # the piece-wise regressors based on establishment size. 
    # This predicts the number of attractors, given there are at least one.
    logger.info("Running %s step 2 piece-wise attraction based on employment size", trace_label)
    for size_id, size_info in model_settings.get("establishment_size_groups").items():
        logger.info("Running %s size group %s", trace_label, size_id)
        if size_info.get("max_employees") == "inf":
            size_info["max_employees"] = np.inf
        # get the establishments that have at least one attraction
        establishments_with_attraction_and_size = establishments[establishments["has_attraction"]].copy()
        # get the establishments that have at least one attraction and are in the size group
        # max(min(employees - 4, 15), 0) is the piece-wise regressor
        establishments_with_attraction_and_size["employees"] = np.maximum(
            np.minimum(
                establishments_with_attraction_and_size["employees"] - size_info.get("min_employees") + 1, 
                size_info.get("max_employees") - size_info.get("min_employees") + 1
                ), 
            0
        )
        # calculate the number of attractors
        establishments_with_attraction_and_size["attractors"] = (
            establishments_with_attraction_and_size["employees"] * 
            size_info.get("attractors_per_employee")
        )
        # establishments["attractors" + str(size_id)] = establishments_with_attraction_and_size["attractors"]
        # sum the attraction for each establishment by matching index
        establishments["attractors"] += establishments_with_attraction_and_size["attractors"]
    
    establishments["attractors"] = np.where(
        establishments["has_attraction"],
        np.exp(establishments["attractors"].fillna(0)),
        0
    )

    # step 3 Industry factors.
    # These are estimated industry specific factors 
    # which modify the predictions from the second step to produce the final set of outputs.
    logger.info("Running %s step 3 apply industry factors", trace_label)
    # get the industry dictionary from model spec
    industry_dict = model_settings.get("industry_groups")
    industry_factor = {}
    for industry_number, industry_info in industry_dict.items():
        industry_factor[industry_number] = industry_info.get("industry_factor")
    # apply the industry effect to the number of attractors
    establishments["attractors"] *= establishments["industry_number"].map(industry_factor)
    establishments["attractors"] = establishments["attractors"].fillna(0)
    
    # write establishments table back to state
    state.add_table("establishments", establishments)    

    # aggregate the number of attractors by zone
    logger.info("Running %s step 4 aggregate by zone", trace_label)
    land_use[model_settings.get("RESULT_COL_NAME")] = establishments.groupby("zone_id")["attractors"].sum()

    # scale the number of attractors by the establishment sample rate
    logger.info("Running %s step 5 scale by sample rate %s", trace_label, establishments["sample_rate"].iloc[0])
    land_use[model_settings.get("RESULT_COL_NAME")] *= 1/establishments["sample_rate"].iloc[0]

    # write land use table back to state
    state.add_table("land_use", land_use)
