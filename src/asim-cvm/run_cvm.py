import sys
import importlib
from pathlib import Path
import pytest
import activitysim.abm  # register components # noqa: F401
from activitysim.core import mp_tasks
from activitysim.cli.run import INJECTABLES

def main():
    # TODO: should this just be its own installable package?
    ext_dir = str(Path(__file__).parents)
    sys.path.insert(0, ext_dir)
    extensions = importlib.import_module("extensions")
    sys.path = sys.path[1:]

    arguments = sys.argv

    data_dir = tuple(arguments[1].split(","))
    configs_dir = tuple(arguments[2].split(","))
    output_dir = arguments[3]

    state = extensions.cvm_state.State.make_default(
        __file__, configs_dir=configs_dir, data_dir=data_dir, output_dir=output_dir
    )

    state.import_extensions("extensions")
    state.logging.config_logger()

    if state.settings.multiprocess:
        injectables = {}
        for k in INJECTABLES:
            try:
                injectables[k] = state.get_injectable(k)
            except KeyError:
                pass
        injectables["settings"] = state.settings
        mp_tasks.run_multiprocess(state, injectables)
    else:
        state.run(models=state.settings.models)

if __name__ == "__main__":
    main()