# ActivitySim
# See full license in LICENSE.txt.

import sys
import argparse
import os
import pandas as pd
import warnings

from activitysim.cli.run import add_run_args, run

import extensions

if __name__ == "__main__":

    warnings.simplefilter(action="ignore", category=pd.errors.PerformanceWarning)

    parser = argparse.ArgumentParser()
    add_run_args(parser)
    args = parser.parse_args()

    os.environ["MKL_NUM_THREADS"] = "1"
    os.environ["OMP_NUM_THREADS"] = "1"
    os.environ["OPENBLAS_NUM_THREADS"] = "1"
    os.environ["NUMBA_NUM_THREADS"] = "1"
    os.environ["VECLIB_MAXIMUM_THREADS"] = "1"
    os.environ["NUMEXPR_NUM_THREADS"] = "1"

    sys.exit(run(args))
