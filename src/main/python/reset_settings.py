import os
import sys
from shutil import copy2, copytree

run_directory = sys.argv[0]
property_file = os.path.join(run_directory, "conf", "sandag_abm.properties")

with open(property_file, "r") as f:
    data = f.read()
    f.close()

release_directory = data.split("release = ")[1].split("\n")[0]

copy2(
    os.path.join(release_directory, "common", "conf", "sandag_abm.properties"),
    os.path.join(run_directory, "conf", "sandag_abm.properties")
)
copytree(
    os.path.join(release_directory, "src", "asim", "configs"),
    os.path.join(run_directory, "src", "asim", "configs")
)
copytree(
    os.path.join(release_directory, "src", "asim-cvm", "configs"),
    os.path.join(run_directory, "src", "asim-cvm", "configs")
)

print("Settings have been reset!")