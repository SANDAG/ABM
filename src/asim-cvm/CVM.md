# Test CVM model alone
Set up the environment with [uv](https://docs.astral.sh/uv/) from this folder. The commands below create a project with Python 3.10, and any newer Python version also works.

```powershell
uv init --python 3.10
uv add activitysim pytest
```

Then run the model with three arguments: the data folder, the configs folder, and the output folder.

```powershell
uv run python run_cvm.py <path to data folder> <path to configs folder> <path to output folder>
```

# Run CVM as part of ABM3
ABM3 runs CVM through `src/main/resources/cvm.bat`. Refer to this file for more details.
