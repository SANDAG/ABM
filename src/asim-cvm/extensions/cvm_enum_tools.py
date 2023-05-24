from functools import reduce

import numpy as np
import pandas as pd


def interp_enum(e, value):
    if isinstance(value, int):
        return e(value)
    return reduce(lambda x, y: x | y, [getattr(e, v) for v in value.split("|")])


def get_enum_name(e, value):
    result = e._value2member_map_.get(value, f"_{value}")
    try:
        return result.name
    except AttributeError:
        return result


def as_int_enum(series, e, dtype=None, categorical=False):
    min_enum_value = int(min(e))
    max_enum_value = int(max(e))
    assert min_enum_value >= 0
    if dtype is None:
        if max_enum_value < 256 and min_enum_value >= 0:
            dtype = np.uint8
        else:
            dtype = np.int32
    if not isinstance(series, pd.Series):
        series = pd.Series(series)
    result = series.apply(lambda x: interp_enum(e, x)).astype(dtype)
    if categorical:
        categories = [get_enum_name(e, i) for i in range(max_enum_value + 1)]
        result = pd.Categorical.from_codes(codes=result, categories=categories)
    return result
