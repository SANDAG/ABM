import argparse
import os

import numpy as np
import openmatrix as omx
import pandas as pd

MAZ_OFFSET = 0

segments = {
    "test": (492, 1100),  # includes univ
    "full": (0, 100000),
    "new": [2097,  2098,  2099,  2100,  2101,  2102,  2103,  2104,  2105,
        2106,  2107,  2108,  2109,  2110,  2111,  2112,  2113,  2114,
        2115,  2116,  2117,  2118,  2119,  2120,  2121,  2122,  2123,
        2124,  2125,  2126,  2127,  2128,  2129,  2130,  2131,  2132,
        2133,  2147,  2149,  2153,  2154,  2155,  2156,  2157,  2158,
        2159,  2160,  2161,  2162,  2163,  2164,  2165,  2166,  2167,
        2168,  2169,  2170,  2171,  2172,  2173,  2174,  2175,  2176,
        2177,  2178,  2179,  2180,  2181,  2182,  2183,  2184,  2185,
        2186,  2187,  2188,  2189,  2190,  2191,  2192,  2193,  2194,
        2195,  2196,  2197,  2198,  2199,  2200,  2201,  2202,  2203,
        2204,  2205,  2206,  2207,  2208,  2209,  2210,  2211,  2212,
        2213,  2214,  2215,  2216,  2217,  2218,  2219,  2220,  2221,
        2222,  2223,  2224,  2226,  2227,  2338,  2339,  2340,  2341,
        2342,  2343,  2344,  2345,  2346,  2347,  2348,  2349,  2350,
        2351,  2352,  2353,  2354,  2355,  2356,  2357,  2358,  2359,
        2360,  2361,  2362,  2363,  2364,  2365,  2366,  2367,  2368,
        2369,  2370,  2371,  2372,  2373,  2374,  2375,  2376,  2377,
        2378,  2379,  2380,  2381,  2382,  2383,  2384,  2385,  2386,
        2387,  2388,  2389,  2390,  2391,  2392,  2393,  2394,  2395,
        2396,  2397,  2398,  2399,  2400,  2401,  2402,  2403,  2404,
        2405,  2406,  2407,  2408,  2409,  2410,  2411,  2412,  2413,
        2414,  2415,  2416,  2417,  2418,  2419,  2420,  2421,  2422,
        2423,  2424,  2425,  2426,  2427,  2428,  2429,  2430,  2431,
        2432,  2433,  2434,  2435,  2437,  2439,  2447,  2461,  2462,
        2463,  2464,  2465,  2466,  2467,  2468,  2469,  2470,  2471,
        2472,  2473,  2474,  2475,  2476,  2477,  2478,  2479,  2480,
        2481,  2482,  2483,  2484,  2485,  2486,  2487,  2488,  2489,
        2490,  2491,  2492,  2493,  2494,  2495,  2496,  2497,  2498,
        2499,  2500,  2501,  2502,  2503,  2504,  2505,  2506,  2507,
        2509,  2515,  2516,  2517,  2518,  2519,  2520,  2521,  2522,
        2523,  2525,  2526,  2527,  2528,  2529,  2530,  2531,  2532,
        2533,  2534,  2535,  2536,  2537,  2538,  2539,  2540,  2541,
        2542,  2543,  2544,  2545,  2546,  2547,  2548,  2549,  2550,
        2551,  2552,  2553,  2554,  2555,  2556,  2557,  2558,  2559,
        2560,  2561,  2562,  2563,  2564,  2565,  2566,  2567,  2568,
        2569,  2570,  2571,  2572,  2573,  2574,  2575,  2576,  2577,
        2578,  2579,  2580,  2581,  2582,  2583,  2584,  2585,  2586,
        2587,  2588,  2589,  2590,  2591,  2592,  2593,  2594,  2595,
        2596,  2597,  2598,  2599,  2600,  2601,  2602,  2603,  2604,
        2605,  2606,  2607,  2608,  2609,  2610,  2611,  2612,  2613,
        2614,  2615,  2616,  2617,  2618,  2619,  2620,  2621,  2622,
        2623,  2624,  2625,  2626,  2627,  2628,  2629,  2630,  2631,
        2632,  2633,  2634,  2635,  2636,  2637,  2638,  2639,  2640,
        2641,  2642,  2643,  2644,  2645,  2646,  2647,  2648,  2649,
        2650,  2651,  2652,  2653,  2654,  2655,  2656,  2657,  2658,
        2659,  2660,  2661,  2662,  2663,  2664,  2665,  2666,  2667,
        2668,  2669,  2670,  2671,  2672,  2673,  2674,  2675,  2676,
        2677,  2678,  2679,  2680,  2681,  2682,  2683,  2684,  2685,
        2686,  2687,  2688,  2689,  2690,  2691,  2692,  2693,  2694,
        2695,  2696,  2697,  2698,  2699,  2700,  2701,  2702,  2703,
        2704,  2705,  2706,  2707,  2708,  2709,  2710,  2711,  2712,
        2713,  2714,  2715,  2716,  2717,  2718,  2719,  2720,  2721,
        2722,  2723,  2724,  2728,  2729,  2730,  2731,  2732,  2733,
        2734,  2735,  2736,  2737,  2738,  2739,  2740,  2741,  2742,
        2743,  2744,  2745,  2746,  2747,  2748,  2766,  2767,  2768,
        2769,  2770,  2771,  2772,  2773,  2774,  2775,  2776,  2777,
        2800,  2801,  2802,  2803,  2804,  2805,  2806,  2807,  2808,
        2809,  2810,  2811,  2812,  2813,  2814,  2815,  2816,  2817,
        2818,  2819,  2820,  2821,  2822,  2823,  2824,  2825,  2826,
        2827,  2828,  2829,  2830,  2831,  2832,  2833,  2834,  2835,
        2836,  2837,  2838,  2839,  2840,  2841,  2842,  2843,  2844,
        2845,  2972,  2973,  2974,  2975,  2976,  2977,  2978,  2979,
        2980,  2981,  2982,  2994,  2995,  2996,  2997,  2998,  2999,
        3000,  3001,  3002,  3003,  3004,  3005,  3006,  3007,  3008,
        3009,  3010,  3011,  3012,  3013,  3014,  3015,  3016,  3017,
        3089,  3090,  3091,  3092,  3095,  3096,  3097,  3098,  3099,
        3100,  3101,  3102,  3103,  6778,  6779,  6784,  6785,  6786,
        6787,  6788,  6789,  6790,  7066,  7088,  7090,  7091,  7113,
        7118,  7123, 21878, 21879, 21882, 21883, 21885, 21887, 21889,
       21891, 21895, 22542, 22564, 22588],
            }

skim_list = [
    'traffic_skims_processed_EA.omx',
    'traffic_skims_processed_AM.omx',
    'traffic_skims_processed_MD.omx',
    'traffic_skims_processed_PM.omx',
    'traffic_skims_processed_EV.omx',
    'transit_skims_ea.omx',
    'transit_skims_am.omx',
    'transit_skims_md.omx',
    'transit_skims_pm.omx',
    'transit_skims_ev.omx',
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
land_use = land_use[land_use["MAZ"].isin(segments['new'])]
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

#
# households
#
households = read_csv("households.csv")
households = households[households["mgra"].isin(maz.MAZ)]
integerize_id_columns(households, "households")

to_csv(households, "households.csv")

#
# persons
#
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