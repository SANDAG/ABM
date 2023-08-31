from __future__ import annotations

import logging
from pathlib import Path

import pandas as pd
import numpy as np
from pydantic import validator

from activitysim.core import workflow
from activitysim.core.configuration.logit import LogitComponentSettings

class EstablishmentAttractorSettings(LogitComponentSettings, extra="forbid"):
    """
    Settings for the `household_attractor` component.
    """

    RESULT_COL_NAME: str
    """The name of the column in the route table that is created by this component."""

    INDUSTRY_GROUP_COL_NAME: str
    """The name of the column in the land use table that contains the industry group."""

    ESTABLISHMENT_SIZE_GROUP_COL_NAME: str
    """The name of the column in the land use table that contains the establishment size group."""

    RATE_COL_NAME: str
    """The name of the column in the rates table that contains the rate."""

    industry_groups: dict
    """The industry groups."""

    establishment_size_groups: dict
    """The establishment size groups."""



logger = logging.getLogger(__name__)

@workflow.step
def establishment_attractor(
    state: workflow.State,
    land_use: pd.DataFrame,
    model_settings: EstablishmentAttractorSettings| None = None,
    model_settings_file_name: str = "establishment_attractor.yaml",
    trace_label: str = "establishment_attractor",
) -> None:
    """
    Calculates establishment attractor for commercial vehicle model.

    Parameters
    ----------
    state : workflow.State
    land_use : DataFrame
    model_settings : default None
    model_settings_file_name : str, default "establishment_attractor.yaml"
    trace_label : str, default "establishment_attractor"
    """
    
    if model_settings is None:
        model_settings = EstablishmentAttractorSettings.read_settings_file(
            state.filesystem,
            model_settings_file_name
        )
    
    trace_label = "cvm_establishment_attractor"

    logger.info("Running %s with %d zones", trace_label, len(land_use))

    # get the rates table by industry, by establishment size
    if isinstance(model_settings.SPEC, Path):
        file_name = str(model_settings.SPEC)
    assert isinstance(file_name, str)
    if not file_name.lower().endswith(".csv"):
        file_name = f"{file_name}.csv"

    file_path = state.filesystem.get_config_file_path(file_name)

    rates_df = pd.read_csv(file_path, comment="#")

    # validate the specification matches the rates
    assert model_settings.RATE_COL_NAME in rates_df.columns

    assert model_settings.INDUSTRY_GROUP_COL_NAME in rates_df.columns

    assert model_settings.ESTABLISHMENT_SIZE_GROUP_COL_NAME in rates_df.columns

    def get_establishment_group(x):
        for size_id, size_info in model_settings.establishment_size_groups.items():
            if size_info.get("max_employees") == "inf":
                size_info["max_employees"] = np.inf
            if (x >= size_info.get("min_employees")) & (x <= size_info.get("max_employees")):
                return size_id
        return None

    # calculate the attractor
    land_use[model_settings.RESULT_COL_NAME] = 0
    ## loop over the industry groups
    for industry_group_id, industry_group_info in model_settings.industry_groups.items():
        land_use_df = land_use.copy()
        logger.info(f"Calculating establishment attractor for industry group {industry_group_info.get('name')}")
        land_use_df[model_settings.INDUSTRY_GROUP_COL_NAME] = industry_group_id
        # calculates the number of employees in each industry group
        land_use_df["industry_emp"] = land_use_df[industry_group_info.get("columns")].sum(axis = 1)
        # assign the establishment size group
        land_use_df[model_settings.ESTABLISHMENT_SIZE_GROUP_COL_NAME] = land_use_df["industry_emp"].apply(lambda x: get_establishment_group(x))

        # join the rates table
        land_use_df = land_use_df.reset_index().merge(
            rates_df, 
            how = "left", 
            on = [model_settings.INDUSTRY_GROUP_COL_NAME, model_settings.ESTABLISHMENT_SIZE_GROUP_COL_NAME]
        ).set_index("zone_id")

        # calculate the attractor
        land_use_df[model_settings.RESULT_COL_NAME] = land_use_df["industry_emp"] * land_use_df[model_settings.RATE_COL_NAME]
        
        # log total attractor for industry group
        logger.info(f"Total attractor for industry group {industry_group_info.get('name')}: {land_use_df[model_settings.RESULT_COL_NAME].sum()}")

        # update the attractor in land use table
        land_use[model_settings.RESULT_COL_NAME] += land_use_df[model_settings.RESULT_COL_NAME]

    # write land use table back to state
    state.add_table("land_use", land_use)    
