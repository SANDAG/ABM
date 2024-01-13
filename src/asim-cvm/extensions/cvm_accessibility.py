from __future__ import annotations

import logging

import pandas as pd

from activitysim.abm.models.accessibility import (
    AccessibilitySettings,
    compute_accessibility,
)
from activitysim.core import los, workflow
from activitysim.core.input import read_input_table

logger = logging.getLogger(__name__)


@workflow.step
def cvm_accessibility(
    state: workflow.State,
    land_use: pd.DataFrame,
    commercial_accessibility: pd.DataFrame,
    network_los: los.Network_LOS,
    model_settings: AccessibilitySettings | None = None,
    model_settings_file_name: str = "cvm_accessibility.yaml",
    trace_label: str = "cvm_accessibility",
    output_table_name: str = "commercial_accessibility",
) -> None:
    """
    Aggregate accessibility for commercial vehicle model.

    Parameters
    ----------
    state : workflow.State
    land_use : DataFrame
    commercial_accessibility : DataFrame
    network_los : los.Network_LOS
    model_settings : AccessibilitySettings, default None
    model_settings_file_name : str, default "cvm_accessibility.yaml"
    trace_label : str, default "cvm_accessibility"
    output_table_name : str, default "commercial_accessibility"
    """
    return compute_accessibility(
        state=state,
        land_use=land_use,
        accessibility=commercial_accessibility,
        network_los=network_los,
        model_settings=model_settings,
        model_settings_file_name=model_settings_file_name,
        trace_label=trace_label,
        output_table_name=output_table_name,
    )


@workflow.table
def commercial_accessibility(state: workflow.State):
    """
    If 'commercial_accessibility' is in input_tables list, then read it in,
    otherwise create skeleton table with same index as landuse.

    This allows loading of pre-computed accessibility table.
    """

    land_use = state.get_dataframe("land_use")
    accessibility_df = read_input_table(
        state, "commercial_accessibility", required=False
    )

    if accessibility_df is None:
        accessibility_df = pd.DataFrame(index=land_use.index)
        logger.debug(f"created commercial_accessibility table {accessibility_df.shape}")
    else:
        try:
            assert accessibility_df.sort_index().index.equals(
                land_use.sort_index().index
            ), (
                "loaded commercial_accessibility table index does not match "
                "index of land_use table"
            )
        except AssertionError:
            land_use_index = land_use.index
            if f"_original_{land_use_index.name}" in land_use:
                land_use_zone_ids = land_use[f"_original_{land_use_index.name}"]
                remapper = dict(zip(land_use_zone_ids, land_use_zone_ids.index))
                accessibility_df.index = accessibility_df.index.map(remapper.get)
                assert accessibility_df.sort_index().index.equals(
                    land_use.sort_index().index
                ), (
                    "loaded commercial_accessibility table index does not match "
                    "index of land_use table"
                )
            else:
                raise
        logger.info(f"loaded land_use {accessibility_df.shape}")

    # replace table function with dataframe
    state.add_table("commercial_accessibility", accessibility_df)

    return accessibility_df
