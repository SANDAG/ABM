# Installation
To install the ActivitySim version that works with CVM, follow the steps below in the user's package manager like conda:

```bash
mkdir workspace
cd workspace
git clone https://github.com/ActivitySim/sharrow.git
git clone https://github.com/camsys/activitysim.git
cd activitysim
git switch time-settings
cd ..
conda env create -p C:/Anaconda3/envs/asim_140 --file activitysim/conda-environments/activitysim-dev-base.yml
conda activate C:/Anaconda3/envs/asim_140
pip install -e ./sharrow
pip install -e ./activitysim
```

# Test CVM model alone
Activate `asim_140`, change directory to this folder, run

```bash
python run_cvm.py
```

# Run CVM as part of ABM3
ABM3 calls the src/main/resources/cvm.bat file to run CVM. The file has the path to the python environment pointed to C:/Anaconda3/envs/asim_140, hence the installation needs to create the python env under that folder path. This is consistent with ABM3's resident model setting.