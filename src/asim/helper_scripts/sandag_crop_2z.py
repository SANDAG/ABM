import argparse
import os

import numpy as np
import openmatrix as omx
import pandas as pd

MAZ_OFFSET = 0
CROP_SKIMS = True
CROP_SURVEY_DATA = True
override_data_path = r'C:\ABM3_dev\estimation\survey_data'

segments = {
    "test": (492, 1100),  # includes univ
    "full": (0, 100000),
    # MAZs in zip code 92103 & 92101 (downtown San Diego)
    "series15": \
        list(pd.read_csv(os.path.join(override_data_path, 'downtown_zones.csv')).MGRA.values) + 
        [24322, 24323, 24324, 24325, 24326, 24327, 24328, 24329, 24330, 24331, 24332, 24333], # this row contains externals
    "new": list(np.arange(2097,3103)) + [6778,  6779,  6784,  6785,  6786,
        6787,  6788,  6789,  6790,  7066,  7088,  7090,  7091,  7113,
        7118,  7123, 21878, 21879, 21882, 21883, 21885, 21887, 21889,
       21891, 21895, 22542, 22564, 22588],
}

skim_list = [
    'traffic_skims_EA.omx',
    'traffic_skims_AM.omx',
    'traffic_skims_MD.omx',
    'traffic_skims_PM.omx',
    'traffic_skims_EV.omx',
    'transit_skims.omx'
    # 'transit_skims_ea.omx',
    # 'transit_skims_am.omx',
    # 'transit_skims_md.omx',
    # 'transit_skims_pm.omx',
    # 'transit_skims_ev.omx',
]

parser = argparse.ArgumentParser(description="crop SANDAG 2 zone raw_data")
parser.add_argument(
    "-s",
    "--segment_name",
    metavar="segment_name",
    type=str,
    nargs=1,
    help=f"geography segmentation (e.g. full)",
)

parser.add_argument(
    "-c",
    "--check_geography",
    default=False,
    action="store_true",
    help="check consistency of MAZ, TAZ zone_ids and foreign keys & write orphan_households file",
)

parser.add_argument(
    "-i", 
    "--input_folder",
    action = 'store',
    help = "input folder"
)

parser.add_argument(
    "-o", 
    "--output_folder",
    action = 'store',
    help = "output folder"
)

args = parser.parse_args()

segment_name = args.segment_name[0]
check_geography = args.check_geography

assert segment_name in segments.keys(), f"Unknown seg: {segment_name}"
#maz_min, maz_max = segments[segment_name]

input_dir = args.input_folder
output_dir = args.output_folder


print(f"segment_name {segment_name}")

print(f"input_dir {input_dir}")
print(f"output_dir {output_dir}")
#print(f"maz_min {maz_min}")
#print(f"maz_max {maz_max}")

print(f"check_geography {check_geography}")

if not os.path.isdir(output_dir):
    print(f"creating output directory {output_dir}")
    os.mkdir(output_dir)


def input_path(file_name):
    return os.path.join(input_dir, file_name)


def output_path(file_name):
    return os.path.join(output_dir, file_name)


def integerize_id_columns(df, table_name):
    columns = ["MAZ", "OMAZ", "DMAZ", "TAZ", "zone_id", "household_id", "HHID"]
    for c in df.columns:
        if c in columns:
            print(f"converting {table_name}.{c} to int")
            if df[c].isnull().any():
                print(df[c][df[c].isnull()])
            df[c] = df[c].astype(int)


def read_csv(file_name, integerize=True):
    df = pd.read_csv(input_path(file_name))

    print(f"read {file_name} {df.shape}")

    return df


def to_csv(df, file_name):
    print(f"writing {file_name} {df.shape} {output_path(file_name)}")
    df.to_csv(output_path(file_name), index=False)


print(f"output_dir {output_dir}")


if check_geography:

    # ######## check for orphan_households not in any maz in land_use
    land_use = read_csv("land_use.csv")
    land_use = land_use[["MAZ", "TAZ"]]  # King County
    land_use = land_use.sort_values(["TAZ", "MAZ"])

    households = read_csv("households.csv")
    orphan_households = households[~households.MAZ.isin(land_use.MAZ)]
    print(f"{len(orphan_households)} orphan_households")

    # write orphan_households to INPUT directory (since it doesn't belong in output)
    file_name = "orphan_households.csv"
    print(f"writing {file_name} {orphan_households.shape} to {input_path(file_name)}")
    orphan_households.to_csv(input_path(file_name), index=False)

    # ######## check that land_use and maz and taz tables have same MAZs and TAZs

    # could just build maz and taz files, but want to make sure PSRC data is right

    land_use = read_csv("land_use.csv")
    land_use = land_use.sort_values("MAZ")
    maz = read_csv("maz.csv").sort_values("MAZ")

    # ### FATAL ###
    if not land_use.MAZ.isin(maz.MAZ).all():
        print(
            f"land_use.MAZ not in maz.MAZ\n{land_use.MAZ[~land_use.MAZ.isin(maz.MAZ)]}"
        )
        raise RuntimeError(f"land_use.MAZ not in maz.MAZ")

    if not maz.MAZ.isin(land_use.MAZ).all():
        print(f"maz.MAZ not in land_use.MAZ\n{maz.MAZ[~maz.MAZ.isin(land_use.MAZ)]}")

    # ### FATAL ###
    if not land_use.TAZ.isin(maz.TAZ).all():
        print(
            f"land_use.TAZ not in maz.TAZ\n{land_use.TAZ[~land_use.TAZ.isin(maz.TAZ)]}"
        )
        raise RuntimeError(f"land_use.TAZ not in maz.TAZ")

    if not maz.TAZ.isin(land_use.TAZ).all():
        print(f"maz.TAZ not in land_use.TAZ\n{maz.TAZ[~maz.TAZ.isin(land_use.TAZ)]}")

    land_use = land_use.sort_values("TAZ")
    taz = read_csv("taz.csv").sort_values("TAZ")

    # ### FATAL ###
    if not land_use.TAZ.isin(taz.TAZ).all():
        print(
            f"land_use.TAZ not in taz.TAZ\n{land_use.TAZ[~land_use.TAZ.isin(taz.MAZ)]}"
        )
        raise RuntimeError(f"land_use.TAZ not in taz.TAZ")

    if not taz.TAZ.isin(land_use.TAZ).all():
        print(f"taz.TAZ not in land_use.TAZ\n{taz.TAZ[~taz.TAZ.isin(land_use.TAZ)]}")

    # #########s

#
# land_use
#
land_use = read_csv("land_use.csv")
land_use = land_use[land_use["MAZ"].isin(segments[segment_name])]
integerize_id_columns(land_use, "land_use")
land_use = land_use.sort_values("MAZ")

# make sure we have some HSENROLL and COLLFTE, even for very for small samples
# if land_use["HSENROLL"].sum() == 0:
    # assert segment_name != "full", f"land_use['HSENROLL'] is 0 for full sample!"
    # land_use["HSENROLL"] = land_use["AGE0519"]
    # print(f"\nWARNING: land_use.HSENROLL is 0, so backfilled with AGE0519\n")

# if land_use["COLLFTE"].sum() == 0:
    # assert segment_name != "full", f"land_use['COLLFTE'] is 0 for full sample!"
    # land_use["COLLFTE"] = land_use["HSENROLL"]
    # print(f"\nWARNING: land_use.COLLFTE is 0, so backfilled with HSENROLL\n")

# move MAZ and TAZ columns to front
land_use = land_use[
    ["MAZ", "TAZ"] + [c for c in land_use.columns if c not in ["MAZ", "TAZ"]]
]
to_csv(land_use, "land_use.csv")

#
# maz
#
# maz = read_csv("maz.csv").sort_values(["MAZ", "TAZ"])
# maz = maz[maz["MAZ"].isin(land_use.MAZ)]
# integerize_id_columns(maz, "maz")

# assert land_use.MAZ.isin(maz.MAZ).all()
# assert land_use.TAZ.isin(maz.TAZ).all()
# assert maz.TAZ.isin(land_use.TAZ).all()
maz = land_use[['MAZ']]
to_csv(land_use[['MAZ']], "maz.csv")

#
# taz
#
#taz = read_csv("taz.csv").sort_values(["TAZ"])
#taz = taz[taz["TAZ"].isin(land_use.TAZ)]
#integerize_id_columns(taz, "taz")

#assert land_use.TAZ.isin(taz.TAZ).all()
taz = pd.DataFrame({'TAZ': land_use['TAZ'].unique()})
to_csv(taz, "taz.csv")

# print(maz.shape)
# print(f"MAZ {len(maz.MAZ.unique())}")
# print(f"TAZ {len(maz.TAZ.unique())}")

if CROP_SURVEY_DATA:
    # want to crop override_*.csv files from infer.py script.
    # need to check and make sure all tour and trip destinations are inside cropped area
    trips  = pd.read_csv(os.path.join(override_data_path, "override_trips.csv"))
    tours  = pd.read_csv(os.path.join(override_data_path, "override_tours.csv"))
    persons  = pd.read_csv(os.path.join(override_data_path, "override_persons.csv"))
    households  = pd.read_csv(os.path.join(override_data_path, "override_households.csv"))
    joint_tour_participants  = pd.read_csv(os.path.join(override_data_path, "override_joint_tour_participants.csv"))

    # checking to make sure all tours & trips from the household are in the cropped area
    trips['in_cropped_area'] = (trips['origin'].isin(maz.MAZ) & trips['destination'].isin(maz.MAZ))
    tours_in_cropped_area = trips.loc[trips.groupby('tour_id')['in_cropped_area'].transform('all'), 'tour_id'].unique()
    tours['in_cropped_area'] = (tours['origin'].isin(maz.MAZ) & tours['destination'].isin(maz.MAZ) & tours['tour_id'].isin(tours_in_cropped_area))
    households_with_valid_tours = tours.loc[tours.groupby('household_id')['in_cropped_area'].transform('all'), 'household_id']
    persons['in_cropped_area'] = (((persons['workplace_zone_id'].isin(maz.MAZ)) | (persons['workplace_zone_id'] == -1)) 
                                & ((persons['school_zone_id'].isin(maz.MAZ)) | (persons['school_zone_id'] == -1)))
    households_with_valid_persons = persons.loc[persons.groupby('household_id')['in_cropped_area'].transform('all'), 'household_id']
    valid_households = households.loc[
        households['home_zone_id'].isin(maz.MAZ) 
        & households['household_id'].isin(households_with_valid_tours)
        & households['household_id'].isin(households_with_valid_persons), 'household_id']

    trips = trips[trips.household_id.isin(valid_households)]
    tours = tours[tours.household_id.isin(valid_households)]
    persons = persons[persons.household_id.isin(valid_households)]
    joint_tour_participants = joint_tour_participants[joint_tour_participants.household_id.isin(valid_households)]
    households = households[households.household_id.isin(valid_households)]

    assert len(trips) > 0, "No valid households in the cropped area."
    print("Number of cropped households: ", len(households))
    print("Number of cropped persons: ", len(persons))
    print("Number of cropped tours: ", len(tours))
    print("Number of cropped joint_tour_participants: ", len(joint_tour_participants))
    print("Number of cropped trips: ", len(trips))

    trips = to_csv(trips, "override_trips.csv")
    tours = to_csv(tours, "override_tours.csv")
    persons = to_csv(persons, "override_persons.csv")
    joint_tour_participants = to_csv(joint_tour_participants, "override_joint_tour_participants.csv")
    households = to_csv(households, "override_households.csv")

# households
households = read_csv("households.csv")
households = households[households["mgra"].isin(maz.MAZ)]
integerize_id_columns(households, "households")

to_csv(households, "households.csv")

# persons
persons = read_csv("persons.csv")
persons = persons[persons["hhid"].isin(households.hhid)]
integerize_id_columns(persons, "persons")

to_csv(persons, "persons.csv")

#
# maz_to_maz_walk and maz_to_maz_bike
#
for file_name in ["maz_maz_walk.csv", "maz_maz_bike.csv"]:
    m2m = read_csv(file_name)
    m2m = m2m[m2m.OMAZ.isin(maz.MAZ) & m2m.DMAZ.isin(maz.MAZ)]
    integerize_id_columns(m2m, file_name)
    to_csv(m2m, file_name)

#
# skims
#
if CROP_SKIMS:
    taz = taz.sort_values("TAZ")
    taz.index = taz.TAZ - 1
    tazs_indexes = taz.index.tolist()  # index of TAZ in skim (zero-based, no mapping)
    taz_labels = taz.TAZ.tolist()  # TAZ zone_ids in omx index order

    for f in skim_list:
        omx_infile_name = f
        skim_data_type = np.float32

        omx_in = omx.open_file(input_path(omx_infile_name))
        print(f"omx_in shape {omx_in.shape()}")

        # create
        num_outfiles = 1 #6 if segment_name == "full" else 1
        if num_outfiles == 1:
            omx_out = [omx.open_file(output_path(f"{f}"), "w")]
        else:
            omx_out = [
                omx.open_file(output_path(f"skims{i+1}.omx"), "w") for i in range(num_outfiles)
            ]

        for omx_file in omx_out:
            omx_file.create_mapping("ZONE", taz_labels)

        iskim = 0
        for mat_name in omx_in.list_matrices():

            # make sure we have a vanilla numpy array, not a CArray
            m = np.asanyarray(omx_in[mat_name]).astype(skim_data_type)
            m = m[tazs_indexes, :][:, tazs_indexes]
            print(f"{mat_name} {m.shape}")

            omx_file = omx_out[iskim % num_outfiles]
            omx_file[mat_name] = m
            iskim += 1


        omx_in.close()
        for omx_file in omx_out:
            omx_file.close()