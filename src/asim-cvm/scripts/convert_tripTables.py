# to be run in python2
import openmatrix as omx, numpy as np, tables, os, sys

# get arguments
model_name = sys.argv[1]
output_dir = sys.argv[2]

# Convert trip tables

for period in ["EA", "AM", "MD", "PM", "EV"]:

    print("Working on auto %s_%s_%s" % (mode, period, vot))
    # rename the file
    # os.rename(output_dir + "/%s_%s_%s.omx" % (mode, period, vot), output_dir + "/%s_%s_%s_.omx" % (mode, period, vot))
    trips = omx.open_file(
        output_dir + "/" + model_name + "/trips_%s.omx" % (period), "r"
    )
    new_trips = omx.open_file(
        output_dir + "/assignment/%s_%s_%s.omx" % (mode, period, vot), "w"
    )

    for table in skim.list_matrices():
        new_skim.create_matrix(
            name=table,
            obj=np.array(skim[table]),
            shape=skim[table].shape,
            atom=tables.Atom.from_dtype(np.dtype("float64")),
        )

    skim.close()
    new_skim.close()
