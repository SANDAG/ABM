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

    industries: dict
    """The industries."""

    industry_groups: dict
    """The industry groups."""


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
        model_settings = state.filesystem.read_settings_file(model_settings_file_name)

    trace_label = "cvm_establishment_attractor"

    logger.info("Running %s with synthetic establishments", trace_label)

    # calculate the attractor
    land_use[model_settings.get("RESULT_COL_NAME")] = 0

    establishments_df = establishments.copy()
    establishments_df["attractions"] = 0

    # step 1 Binary logit model that predicts whether an establishment has at least one attraction.
    # Necessary because there are many which have none in the survey.
    logger.info("Running %s step 1 binary logit model", trace_label)
    # get the industry dictionary from model spec
    industry_dict = model_settings.get("industries")
    establishments_df["industry_group"] = establishments_df["industry_number"].map(
        industry_dict
    )
    establishments_df["industry_group"] = establishments_df["industry_group"].apply(
        lambda x: x.get("industry_group") if x is not None else 0
    )
    # get the industry group specific beta from model spec
    industry_group_dict = model_settings.get("industry_groups")
    _industry_group = establishments_df["industry_group"].map(industry_group_dict)
    establishments_df["beta_industry_group"] = _industry_group.apply(
        lambda x: x.get("beta_employment") if x is not None else 0
    )
    establishments_df["constant"] = _industry_group.apply(
        lambda x: x.get("constant") if x is not None else 0
    )

    # calculate the probability of having an attraction
    establishments_df["has_attraction_probability"] = 1 / (
        1
        + np.exp(
            -establishments_df["beta_industry_group"] * establishments_df["employees"]
            - establishments_df["constant"]
        )
    )
    # get random numbers for the binary logit model
    establishments_df["random"] = state.get_rn_generator().random_for_df(establishments)
    # calculate whether the establishment has an attraction
    establishments_df["has_attraction"] = (
        establishments_df["has_attraction_probability"] > establishments_df["random"]
    )

    establishments["has_attraction"] = establishments_df["has_attraction"]

    tracing.print_summary(
        "has_attraction",
        establishments["has_attraction"],
        value_counts=True,
    )

    # step 2 preliminary estimated # attractions for establishments with at least one attraction
    # takes the form of sqrt()
    logger.info("Running %s step 2 preliminary estimated # attractions", trace_label)
    beta_attraction = model_settings.get("CONSTANTS").get("beta_employment")
    establishments_df["attractions"] = np.where(
        establishments_df["has_attraction"],
        np.sqrt(establishments_df["employees"]) * beta_attraction,
        0,
    )

    # round to 4 decimal places
    establishments_df["attractions"] = establishments_df["attractions"].round(4)

    # step 3 Industry factors.
    # These are estimated industry specific factors
    # which modify the predictions from the second step to produce the final set of outputs.
    logger.info("Running %s step 3 apply industry factors", trace_label)
    # get the industry dictionary from model spec
    industry_dict = model_settings.get("industries")
    industry_factor = {}
    for industry_number, industry_info in industry_dict.items():
        industry_factor[industry_number] = industry_info.get("industry_factor")
    # apply the industry effect to the number of attractions
    establishments_df["attractions"] *= establishments_df["industry_number"].map(
        industry_factor
    )
    establishments_df["attractions"] = establishments_df["attractions"].fillna(0)

    establishments["attractions"] = establishments_df["attractions"]

    # write establishments table back to state
    state.add_table("establishments", establishments)

    # aggregate the number of attractions by zone
    logger.info("Running %s step 4 aggregate by zone", trace_label)
    land_use[model_settings.get("RESULT_COL_NAME")] = establishments_df.groupby(
        "zone_id"
    )["attractions"].sum()

    # aggregate the number of attractions by industry by zone
    # agrregate by zone and business type, then unstack to get a column for each business type
    # then rename the columns to be the business type
    # then fill na with 0
    logger.info("Running %s step 4 aggregate by zone and industry", trace_label)
    agg_df = establishments_df.groupby(["zone_id", "industry_name"])[
        "attractions"
    ].sum()
    agg_df = agg_df.unstack("industry_name")#.drop("industry_name")
    agg_df.columns = agg_df.columns.map(
        lambda x: "establishment_attraction_" + x.lower()
    )
    
    land_use = pd.concat([land_use, agg_df], axis=1)
    land_use[agg_df.columns] = land_use[agg_df.columns].fillna(0)

    # scale the number of attractions by the establishment sample rate
    logger.info(
        "Running %s step 5 scale by sample rate %s",
        trace_label,
        establishments_df["sample_rate"].iloc[0],
    )
    land_use[model_settings.get("RESULT_COL_NAME")] *= (
        1 / establishments_df["sample_rate"].iloc[0]
    )

    # fill na with 0
    land_use[model_settings.get("RESULT_COL_NAME")] = land_use[
        model_settings.get("RESULT_COL_NAME")
    ].fillna(0)

    # write land use table back to state
    state.add_table("land_use", land_use)
