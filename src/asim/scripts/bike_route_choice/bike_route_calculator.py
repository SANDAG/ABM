import os
import logging
import pandas as pd
import numpy as np
import warnings

from bike_route_utilities import BikeRouteChoiceSettings, load_settings, read_file

def calculate_utilities(
    settings: BikeRouteChoiceSettings,
    choosers: pd.DataFrame,
    spec: pd.DataFrame,
    trace_label: str,
    logger: logging.Logger,
) -> pd.DataFrame:
    """
    Calculate utilities for choosers using the provided specifications.
    Modeled after ActivitySim's core.simulate.eval_utilities.

    Parameters:
        settings: BikeRouteChoiceSettings - settings for the bike route choice model
        choosers: pd.DataFrame - DataFrame of choosers (edges or traversals)
        spec: pd.Series - DataFrame with index as utility expressions and values as coefficients
        trace_label: str - label for tracing
    Returns:
        pd.DataFrame - DataFrame of calculated utilities with same index as choosers
    """

    assert isinstance(
        spec, pd.Series
    ), "Spec must be a pandas Series with utility expressions as index and coefficients as values"

    globals_dict = {}
    locals_dict = {
        "np": np,
        "pd": pd,
        "df": choosers,
    }

    expression_values = np.empty((spec.shape[0], choosers.shape[0]))

    for i, expr in enumerate(spec.index):
        try:
            with warnings.catch_warnings(record=True) as w:
                # Cause all warnings to always be triggered.
                warnings.simplefilter("always")
                if expr.startswith("@"):
                    expression_value = eval(expr[1:], globals_dict, locals_dict)
                else:
                    expression_value = choosers.eval(expr)

                if len(w) > 0:
                    for wrn in w:
                        logger.warning(
                            f"{trace_label} - {type(wrn).__name__} ({wrn.message}) evaluating: {str(expr)}"
                        )

        except Exception as err:
            logger.exception(
                f"{trace_label} - {type(err).__name__} ({str(err)}) evaluating: {str(expr)}"
            )
            raise err

        expression_values[i] = expression_value

    # - compute_utilities
    utilities = np.dot(expression_values.transpose(), spec.astype(np.float64).values)
    utilities = pd.DataFrame(utilities, index=choosers.index, columns=["utility"])

    if settings.trace_bike_utilities:
        # trace entire utility calculation
        logger.info(f"Saving utility calculations to {trace_label}.csv")
        expressions = pd.DataFrame(
            data=expression_values.transpose(), index=choosers.index, columns=spec.index
        )
        expressions = pd.concat([choosers, expressions], axis=1)
        expressions["utility"] = utilities["utility"]
        expressions.to_csv(os.path.join(os.path.expanduser(settings.output_path), f"{trace_label}.csv"))

    assert utilities.index.equals(
        choosers.index
    ), "Index mismatch between utilities and choosers"

    return utilities


def calculate_utilities_from_spec(
    settings: BikeRouteChoiceSettings,
    choosers: pd.DataFrame,
    spec_file: str,
    trace_label: str,
    logger: logging.Logger,
    randomize: bool = False,
) -> pd.DataFrame:
    """
    Calculate utilities from a specification file (edge or traversal).

    Args:
        settings: BikeRouteChoiceSettings
        choosers: pd.DataFrame (edges or traversals)
        spec_file: str (path to the specification file)
        trace_label: str (label for tracing/logging)
        randomize: bool (whether to randomize coefficients)

    Returns:
        pd.DataFrame with a 'utility' column
    """
    # read specification file
    spec = read_file(settings, spec_file)

    # Optionally randomize coefficients
    if randomize:
        # randomize coefficients between 1 +/- settings.random_scale_coef, expect for unavailiability (-999)
        spec["Coefficient"] = spec["Coefficient"].apply(
            lambda x: x
            * (
                1
                + np.random.uniform(
                    0 - settings.random_scale_coef, settings.random_scale_coef
                )
            )
            if x > -990
            else x
        )

    # calculate utilities
    utilities = calculate_utilities(
        settings=settings,
        choosers=choosers,
        spec=spec.set_index("Expression")["Coefficient"],
        trace_label=trace_label,
        logger=logger,
    )

    # Optionally also add a random component across all utilities
    if randomize:
        utilities.loc[utilities.utility > -999,"utility"] = utilities.loc[utilities.utility > -999,"utility"] * (
            np.random.choice(
                [(1-settings.random_scale_link),(1+settings.random_scale_link)],
                size=len(utilities.loc[utilities.utility > -999])
            )
        )

    # check that all utilities are less than or equal to zero
    if (utilities["utility"] > 0).any():
        logger.warning(
            f"{trace_label} utilities contain positive values, which may indicate an issue with the utility calculation."
        )
        logger.info(f"Utilities: {utilities['utility'].describe()}")
        logger.info(
            f"Percentage of positive utilities: {(utilities['utility'] > 0).mean() * 100:.2f}%"
        )
        logger.info(
            "Capping the positive utilities to zero to avoid issues in Dijkstra's algorithm."
        )
        utilities.loc[utilities["utility"] > 0, "utility"] = 0
    assert (
        ~utilities.utility.isna()
    ).all(), f"{trace_label} utilities should not contain any NaN values"

    return utilities
