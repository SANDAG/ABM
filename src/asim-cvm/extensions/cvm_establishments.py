# ActivitySim
# See full license in LICENSE.txt.
from __future__ import annotations

import io
import logging

import pandas as pd

from activitysim.core import tracing, workflow
from activitysim.core.input import read_input_table

from .cvm_state import State

logger = logging.getLogger(__name__)


@workflow.table
def establishments(state: State) -> pd.DataFrame:
    establishments_sample_size = state.settings.establishments_sample_size
    trace_establishment_id = state.settings.trace_establishment_id

    df_full = read_input_table(state, "establishments")
    n_total_establishments = df_full.shape[0]

    logger.info(f"full establishment table has {n_total_establishments} rows")

    establishments_sliced = False

    # if we are tracing hh exclusively
    if trace_establishment_id and establishments_sample_size == 1:
        # df contains only trace_hh (or empty if not in full store)
        df = tracing.slice_ids(df_full, trace_establishment_id)
        establishments_sliced = True

    # if we need a subset of full store
    elif n_total_establishments > establishments_sample_size > 0:
        logger.info(
            f"sampling {establishments_sample_size} of {n_total_establishments} establishments"
        )

        """
        Because random seed is set differently for each step, sampling of establishments using
        Random.global_rng would sample differently depending upon which step it was called from.
        We use a one-off rng seeded with the pseudo step name 'sample_households' to provide
        repeatable sampling no matter when the table is loaded.

        Note that the external_rng is also seeded with base_seed so the sample will (rightly) change
        if the pipeline rng's base_seed is changed
        """

        prng = state.get_rn_generator().get_external_rng("sample_establishments")
        df = df_full.take(
            prng.choice(len(df_full), size=establishments_sample_size, replace=False)
        ).sort_index()
        establishments_sliced = True

        # if tracing and we missed trace_hh in sample, but it is in full store
        if (
            trace_establishment_id
            and trace_establishment_id not in df.index
            and trace_establishment_id in df_full.index
        ):
            # replace first hh in sample with trace_hh
            logger.debug(
                f"replacing establishment {df.index[0]} with "
                f"{trace_establishment_id} in establishment sample"
            )
            df_hh = df_full.loc[[trace_establishment_id]]
            df = pd.concat([df_hh, df[1:]])

    else:
        df = df_full

    state.set("establishments_sliced", establishments_sliced)

    if "sample_rate" not in df.columns:
        if establishments_sample_size == 0:
            sample_rate = 1
        else:
            sample_rate = round(establishments_sample_size / n_total_establishments, 5)

        df["sample_rate"] = sample_rate

    logger.info("loaded establishments %s" % (df.shape,))
    buffer = io.StringIO()
    df.info(buf=buffer)
    logger.debug("establishments.info:\n" + buffer.getvalue())

    # replace table function with dataframe
    state.add_table("establishments", df)

    state.get_rn_generator().add_channel("establishments", df)

    state.tracing.register_traceable_table("establishments", df)
    if trace_establishment_id:
        state.tracing.trace_df(df, "raw.establishments", warn_if_empty=True)

    return df
