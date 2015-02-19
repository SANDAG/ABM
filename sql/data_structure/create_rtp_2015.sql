SET NOCOUNT ON;

-- Create rtp_2015 schema
IF NOT EXISTS (SELECT schema_name FROM information_schema.schemata WHERE schema_name='rtp_2015')
EXEC ('CREATE SCHEMA [rtp_2015]')
GO

-- Add metadata for [rtp_2015]
IF EXISTS(SELECT * FROM [db_meta].[data_dictionary] WHERE [ObjectType] = 'SCHEMA' AND [FullObjectName] = '[rtp_2015]' AND [PropertyName] = 'MS_Description')
EXECUTE [db_meta].[drop_xp] 'rtp_2015', 'MS_Description'
EXECUTE [db_meta].[add_xp] 'rtp_2015', 'MS_Description', 'schema to hold all objects associated with the 2015 rtp'
GO




-- Create Screenline Spatial Table
IF OBJECT_ID('rtp_2015.screenline','U') IS NOT NULL
DROP TABLE [rtp_2015].[screenline]
GO

CREATE TABLE 
	[rtp_2015].[screenline] (
		[screenline_id] tinyint NOT NULL,
		[shape] geometry NOT NULL,
		CONSTRAINT pk_screenline PRIMARY KEY ([screenline_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE);
GO

INSERT INTO
	[rtp_2015].[screenline]
VALUES
	(1, 0xB608000001140000006016C4574100000000FA1E3E41000000C0DDD057410000008036303E41),
	(2, 0xB6080000011400000080E71B584100000040DD963B41000000A006095841000000E0F17D3B41),
	(3, 0xB60800000114000000004B0C5841000000006BCE3B410000008086125841000000C016D73B41),
	(4, 0xB608000001140000008022FF5741000000804ADE3B41000000202E0958410000002066E73B41),
	(5, 0xB608000001140000002069E55741000000E0926B3C41000000E014EE5741000000E05D6F3C41),
	(6, 0xB6080000011400000080E900584100000000ECF83C4100000000F408584100000000ECF83C41),
	(7, 0xB60800000114000000E082DB5741000000E0BD3F3D410000008085E15741000000E09A493D41),
	(8, 0xB60800000114000000C007E457410000004083913E4100000060C2E9574100000060B3AF3E41),
	(9, 0xB60800000114000000E0610E58410000008072023E4100000040981758410000002042013E41),
	(10, 0xB608000001140000006014F657410000000020BE3C410000006014F6574100000060C7D43C41),
	(11, 0xB6080000010403000000000000E02F105841000000401D8F3C41000000603512584100000020CD2A3C4100000020DD12584100000000BC023C4101000000010000000001000000FFFFFFFF0000000002),
	(12, 0xB608000001140000002045B8574100000040AFAB3E4100000060E8BC57410000008066B93E41),
	(13, 0xB60800000114000000E0ACD45741000000605EDF3E41000000404AD557410000004094B93E41)
GO

-- Add metadata for [rtp_2015].[screenline]
EXECUTE [db_meta].[add_xp] 'rtp_2015.screenline', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.screenline', 'MS_Description', 'screenline geography'
GO




-- Create Series 13 TAZ freight distibution hub table
IF OBJECT_ID('rtp_2015.taz13_freight_distribute_hub','U') IS NOT NULL
DROP TABLE [rtp_2015].[taz13_freight_distribute_hub]

CREATE TABLE 
	#tt (
		[taz13] smallint,
		[category] tinyint
	)
	
INSERT INTO 
	#tt
VALUES	
	(736, 11),
	(772, 11),
	(785, 11),
	(792, 11),
	(812, 11),
	(814, 11),
	(820, 11),
	(822, 11),
	(824, 11),
	(830, 11),
	(832, 11),
	(834, 11),
	(836, 11),
	(839, 11),
	(844, 11),
	(845, 11),
	(847, 11),
	(848, 11),
	(850, 11),
	(856, 11),
	(857, 11),
	(858, 11),
	(860, 11),
	(862, 11),
	(866, 11),
	(867, 11),
	(868, 11),
	(871, 11),
	(874, 11),
	(877, 11),
	(881, 11),
	(883, 11),
	(884, 11),
	(887, 11),
	(889, 11),
	(890, 11),
	(892, 11),
	(893, 11),
	(894, 11),
	(897, 11),
	(898, 11),
	(899, 11),
	(904, 11),
	(907, 11),
	(908, 11),
	(911, 11),
	(915, 11),
	(916, 11),
	(918, 11),
	(920, 11),
	(922, 11),
	(923, 11),
	(925, 11),
	(927, 11),
	(928, 11),
	(930, 11),
	(931, 11),
	(934, 11),
	(936, 11),
	(937, 11),
	(938, 11),
	(939, 11),
	(940, 11),
	(941, 11),
	(942, 11),
	(944, 11),
	(946, 11),
	(947, 11),
	(950, 11),
	(951, 11),
	(953, 11),
	(956, 11),
	(957, 11),
	(958, 11),
	(961, 11),
	(962, 11),
	(963, 11),
	(964, 11),
	(965, 11),
	(968, 11),
	(969, 11),
	(970, 11),
	(973, 11),
	(974, 11),
	(976, 11),
	(978, 11),
	(981, 11),
	(982, 11),
	(983, 11),
	(985, 11),
	(986, 11),
	(987, 11),
	(988, 11),
	(989, 11),
	(991, 11),
	(992, 11),
	(993, 11),
	(994, 11),
	(995, 11),
	(996, 11),
	(997, 11),
	(998, 11),
	(1000, 11),
	(1001, 11),
	(1002, 11),
	(1006, 11),
	(1007, 11),
	(1010, 11),
	(1012, 11),
	(1013, 11),
	(1014, 11),
	(1016, 11),
	(1018, 11),
	(1020, 11),
	(1023, 11),
	(1024, 11),
	(1025, 11),
	(1026, 11),
	(1028, 11),
	(1029, 11),
	(1030, 11),
	(1031, 11),
	(1032, 11),
	(1033, 11),
	(1038, 11),
	(1039, 11),
	(1040, 11),
	(1041, 11),
	(1043, 11),
	(1044, 11),
	(1045, 11),
	(1046, 11),
	(1047, 11),
	(1049, 11),
	(1050, 11),
	(1051, 11),
	(1052, 11),
	(1055, 11),
	(1056, 11),
	(1057, 11),
	(1058, 11),
	(1060, 11),
	(1061, 11),
	(1065, 11),
	(1066, 11),
	(1068, 11),
	(1069, 11),
	(1070, 11),
	(1073, 11),
	(1075, 11),
	(1076, 11),
	(1077, 11),
	(1079, 11),
	(1080, 11),
	(1081, 11),
	(1082, 11),
	(1084, 11),
	(1086, 11),
	(1088, 11),
	(1090, 11),
	(1091, 11),
	(1093, 11),
	(1096, 11),
	(1097, 11),
	(1099, 11),
	(1100, 11),
	(1101, 11),
	(1103, 11),
	(1105, 11),
	(1109, 11),
	(1111, 11),
	(1113, 11),
	(1115, 11),
	(1121, 11),
	(1124, 11),
	(1125, 11),
	(1136, 11),
	(1138, 11),
	(1140, 11),
	(1143, 11),
	(1144, 11),
	(1145, 11),
	(1147, 11),
	(1150, 11),
	(1154, 11),
	(1155, 11),
	(1156, 11),
	(1157, 11),
	(1160, 11),
	(1161, 11),
	(1170, 11),
	(1173, 11),
	(1174, 11),
	(1175, 11),
	(1176, 11),
	(1181, 11),
	(1182, 11),
	(1184, 11),
	(1186, 11),
	(1187, 11),
	(1188, 11),
	(1193, 11),
	(1197, 11),
	(1201, 11),
	(1205, 11),
	(1206, 11),
	(1209, 11),
	(1212, 11),
	(1213, 11),
	(1221, 11),
	(1223, 11),
	(1230, 11),
	(1235, 11),
	(1240, 11),
	(1241, 11),
	(1275, 11),
	(1419, 10),
	(1458, 10),
	(1459, 10),
	(1497, 10),
	(1502, 10),
	(1504, 10),
	(1514, 10),
	(1552, 10),
	(1554, 10),
	(1572, 10),
	(1576, 10),
	(1577, 10),
	(1583, 10),
	(1585, 10),
	(1590, 10),
	(1600, 10),
	(1627, 10),
	(1632, 10),
	(1639, 10),
	(1645, 10),
	(1646, 10),
	(1650, 10),
	(1671, 10),
	(1674, 10),
	(1678, 10),
	(1680, 10),
	(1681, 10),
	(1683, 10),
	(1684, 10),
	(1687, 10),
	(1688, 10),
	(1691, 10),
	(1693, 10),
	(1695, 10),
	(1699, 10),
	(1702, 10),
	(1704, 10),
	(1711, 10),
	(1712, 10),
	(1716, 10),
	(1718, 10),
	(1719, 10),
	(1720, 10),
	(1726, 10),
	(1728, 10),
	(1729, 10),
	(1731, 10),
	(1740, 10),
	(1762, 10),
	(1898, 9),
	(1901, 9),
	(1906, 9),
	(1908, 9),
	(1911, 9),
	(1912, 9),
	(1916, 9),
	(1917, 9),
	(1922, 9),
	(1925, 9),
	(1926, 9),
	(1927, 9),
	(1928, 9),
	(1929, 9),
	(1930, 9),
	(1933, 9),
	(1937, 9),
	(1945, 9),
	(1948, 9),
	(1950, 9),
	(1952, 9),
	(1961, 9),
	(1964, 9),
	(1971, 9),
	(1979, 9),
	(1982, 9),
	(1984, 9),
	(1991, 9),
	(1992, 9),
	(1993, 9),
	(1997, 9),
	(2003, 9),
	(2004, 9),
	(2008, 9),
	(2014, 9),
	(2021, 9),
	(2022, 9),
	(2062, 8),
	(2069, 8),
	(2086, 8),
	(2091, 8),
	(2097, 8),
	(2098, 8),
	(2103, 8),
	(2104, 8),
	(2105, 8),
	(2107, 8),
	(2110, 8),
	(2111, 8),
	(2113, 8),
	(2114, 8),
	(2115, 8),
	(2116, 8),
	(2119, 8),
	(2120, 8),
	(2121, 8),
	(2122, 8),
	(2123, 8),
	(2126, 8),
	(2127, 8),
	(2129, 8),
	(2131, 8),
	(2132, 8),
	(2134, 8),
	(2135, 8),
	(2136, 8),
	(2137, 8),
	(2138, 8),
	(2142, 8),
	(2143, 8),
	(2144, 8),
	(2145, 8),
	(2148, 8),
	(2149, 8),
	(2150, 8),
	(2151, 8),
	(2152, 8),
	(2153, 8),
	(2155, 8),
	(2156, 8),
	(2157, 8),
	(2159, 7),
	(2161, 8),
	(2162, 8),
	(2164, 8),
	(2166, 8),
	(2167, 8),
	(2168, 8),
	(2170, 8),
	(2171, 8),
	(2172, 8),
	(2173, 8),
	(2175, 8),
	(2176, 8),
	(2177, 8),
	(2178, 8),
	(2179, 8),
	(2181, 8),
	(2182, 8),
	(2183, 8),
	(2186, 8),
	(2188, 8),
	(2189, 8),
	(2190, 8),
	(2191, 8),
	(2193, 8),
	(2194, 8),
	(2195, 8),
	(2196, 8),
	(2197, 8),
	(2198, 8),
	(2201, 8),
	(2202, 8),
	(2204, 8),
	(2207, 8),
	(2208, 8),
	(2209, 8),
	(2210, 8),
	(2212, 8),
	(2213, 8),
	(2215, 8),
	(2218, 8),
	(2222, 8),
	(2227, 8),
	(2228, 8),
	(2233, 8),
	(2234, 8),
	(2236, 8),
	(2242, 8),
	(2246, 8),
	(2247, 8),
	(2248, 8),
	(2249, 8),
	(2250, 8),
	(2252, 8),
	(2253, 8),
	(2254, 8),
	(2257, 8),
	(2258, 8),
	(2264, 8),
	(2265, 8),
	(2266, 8),
	(2269, 8),
	(2272, 8),
	(2275, 8),
	(2279, 7),
	(2282, 8),
	(2283, 8),
	(2284, 8),
	(2286, 8),
	(2300, 8),
	(2376, 6),
	(2377, 6),
	(2409, 6),
	(2433, 6),
	(2439, 6),
	(2441, 6),
	(2448, 6),
	(2449, 6),
	(2451, 6),
	(2452, 6),
	(2456, 7),
	(2460, 7),
	(2463, 7),
	(2470, 6),
	(2471, 7),
	(2473, 7),
	(2474, 6),
	(2475, 7),
	(2476, 7),
	(2485, 7),
	(2486, 7),
	(2492, 6),
	(2493, 6),
	(2494, 6),
	(2496, 6),
	(2497, 7),
	(2498, 6),
	(2500, 6),
	(2501, 6),
	(2510, 7),
	(2512, 7),
	(2517, 6),
	(2533, 7),
	(2537, 7),
	(2542, 6),
	(2545, 7),
	(2546, 7),
	(2549, 7),
	(2554, 7),
	(2557, 6),
	(2559, 7),
	(2560, 6),
	(2562, 7),
	(2563, 7),
	(2564, 7),
	(2568, 6),
	(2580, 6),
	(2581, 6),
	(2583, 6),
	(2586, 6),
	(2588, 7),
	(2589, 7),
	(2590, 7),
	(2591, 7),
	(2592, 7),
	(2594, 7),
	(2599, 6),
	(2601, 6),
	(2609, 7),
	(2610, 7),
	(2616, 6),
	(2618, 7),
	(2620, 6),
	(2624, 6),
	(2626, 7),
	(2628, 7),
	(2631, 6),
	(2637, 7),
	(2639, 7),
	(2643, 6),
	(2645, 6),
	(2646, 7),
	(2647, 7),
	(2648, 7),
	(2649, 6),
	(2655, 7),
	(2660, 6),
	(2661, 7),
	(2664, 7),
	(2667, 7),
	(2668, 6),
	(2670, 6),
	(2671, 7),
	(2680, 6),
	(2691, 6),
	(2693, 6),
	(2694, 6),
	(2698, 7),
	(2700, 7),
	(2701, 6),
	(2702, 6),
	(2710, 6),
	(2713, 6),
	(2721, 6),
	(2724, 6),
	(2725, 7),
	(2747, 6),
	(2748, 6),
	(2749, 6),
	(2756, 6),
	(2761, 6),
	(2762, 6),
	(2789, 6),
	(2792, 6),
	(2801, 6),
	(2802, 6),
	(2804, 6),
	(2805, 6),
	(2806, 6),
	(2807, 6),
	(2808, 6),
	(2812, 6),
	(2860, 6),
	(2861, 6),
	(2862, 6),
	(2863, 6),
	(2875, 6),
	(2876, 6),
	(2894, 6),
	(2900, 6),
	(2919, 6),
	(2921, 6),
	(2927, 6),
	(2928, 6),
	(2933, 6),
	(2962, 6),
	(2963, 6),
	(2965, 6),
	(2966, 6),
	(3215, 5),
	(3252, 5),
	(3253, 5),
	(3255, 5),
	(3259, 5),
	(3261, 5),
	(3272, 5),
	(3274, 5),
	(3277, 5),
	(3281, 5),
	(3289, 5),
	(3290, 5),
	(3311, 5),
	(3312, 5),
	(3313, 5),
	(3314, 5),
	(3315, 5),
	(3317, 5),
	(3337, 5),
	(3340, 5),
	(3342, 5),
	(3343, 5),
	(3347, 5),
	(3363, 5),
	(3375, 5),
	(3378, 5),
	(3379, 4),
	(3383, 5),
	(3385, 4),
	(3388, 4),
	(3390, 5),
	(3392, 5),
	(3393, 5),
	(3395, 5),
	(3396, 5),
	(3397, 5),
	(3398, 5),
	(3399, 5),
	(3400, 5),
	(3401, 4),
	(3402, 5),
	(3403, 4),
	(3412, 5),
	(3433, 5),
	(3445, 5),
	(3446, 5),
	(3457, 4),
	(3458, 4),
	(3461, 4),
	(3464, 5),
	(3469, 4),
	(3470, 4),
	(3479, 4),
	(3493, 5),
	(3497, 5),
	(3498, 5),
	(3501, 5),
	(3502, 5),
	(3504, 4),
	(3505, 5),
	(3514, 5),
	(3517, 4),
	(3520, 5),
	(3521, 5),
	(3523, 5),
	(3524, 4),
	(3527, 4),
	(3528, 4),
	(3535, 4),
	(3536, 5),
	(3537, 4),
	(3540, 5),
	(3543, 4),
	(3549, 4),
	(3556, 4),
	(3563, 4),
	(3569, 5),
	(3570, 5),
	(3576, 4),
	(3581, 4),
	(3592, 5),
	(3593, 4),
	(3595, 5),
	(3597, 5),
	(3606, 4),
	(3611, 4),
	(3628, 4),
	(3631, 4),
	(3643, 4),
	(3652, 4),
	(3660, 4),
	(3664, 4),
	(3668, 4),
	(3671, 4),
	(3675, 4),
	(3683, 4),
	(3692, 4),
	(3694, 4),
	(3695, 4),
	(3697, 4),
	(3723, 4),
	(3730, 4),
	(3738, 4),
	(3748, 4),
	(3760, 4),
	(3785, 4),
	(3789, 4),
	(3798, 3),
	(4079, 3),
	(4080, 3),
	(4081, 3),
	(4082, 3),
	(4083, 3),
	(4086, 3),
	(4087, 3),
	(4088, 3),
	(4089, 3),
	(4090, 3),
	(4091, 3),
	(4092, 3),
	(4093, 3),
	(4096, 3),
	(4097, 3),
	(4107, 3),
	(4108, 3),
	(4109, 3),
	(4110, 3),
	(4111, 3),
	(4112, 3),
	(4113, 3),
	(4114, 3),
	(4115, 3),
	(4116, 3),
	(4117, 3),
	(4118, 3),
	(4130, 3),
	(4131, 3),
	(4134, 3),
	(4135, 3),
	(4136, 3),
	(4137, 3),
	(4138, 3),
	(4139, 3),
	(4140, 3),
	(4141, 3),
	(4142, 3),
	(4143, 3),
	(4144, 3),
	(4145, 3),
	(4146, 3),
	(4153, 3),
	(4154, 3),
	(4158, 3),
	(4159, 3),
	(4160, 3),
	(4161, 3),
	(4162, 3),
	(4163, 3),
	(4164, 3),
	(4165, 3),
	(4166, 3),
	(4167, 3),
	(4172, 3),
	(4174, 3),
	(4176, 3),
	(4177, 3),
	(4178, 3),
	(4179, 3),
	(4180, 3),
	(4181, 3),
	(4182, 3),
	(4183, 3),
	(4185, 3),
	(4195, 3),
	(4196, 3),
	(4198, 3),
	(4199, 3),
	(4200, 3),
	(4201, 3),
	(4202, 3),
	(4203, 3),
	(4204, 3),
	(4205, 3),
	(4207, 3),
	(4212, 3),
	(4213, 3),
	(4214, 3),
	(4215, 3),
	(4216, 3),
	(4224, 3),
	(4227, 3),
	(4228, 3),
	(4231, 3),
	(4232, 3),
	(4233, 3),
	(4234, 3),
	(4235, 3),
	(4236, 3),
	(4237, 3),
	(4239, 3),
	(4241, 3),
	(4251, 3),
	(4255, 3),
	(4257, 3),
	(4258, 3),
	(4259, 3),
	(4274, 3),
	(4275, 3),
	(4279, 3),
	(4281, 3),
	(4285, 3),
	(4286, 3),
	(4287, 3),
	(4290, 3),
	(4292, 3),
	(4306, 3),
	(4314, 3),
	(4395, 2),
	(4414, 2),
	(4425, 2),
	(4426, 2),
	(4427, 2),
	(4431, 2),
	(4432, 2),
	(4439, 2),
	(4441, 2),
	(4445, 2),
	(4447, 2),
	(4451, 2),
	(4456, 2),
	(4458, 2),
	(4462, 2),
	(4465, 2),
	(4468, 2),
	(4470, 2),
	(4473, 2),
	(4474, 2),
	(4477, 2),
	(4479, 2),
	(4481, 2),
	(4484, 2),
	(4491, 2),
	(4492, 2),
	(4495, 2),
	(4501, 2),
	(4502, 2),
	(4503, 2),
	(4505, 2),
	(4507, 2),
	(4509, 2),
	(4518, 2),
	(4519, 2),
	(4523, 2),
	(4525, 2),
	(4526, 2),
	(4527, 2),
	(4528, 2),
	(4531, 2),
	(4532, 2),
	(4536, 2),
	(4539, 2),
	(4544, 2),
	(4550, 2),
	(4556, 2),
	(4834, 1),
	(4835, 1),
	(4837, 1),
	(4855, 1),
	(4862, 1),
	(4865, 1),
	(4867, 1),
	(4868, 1),
	(4870, 1),
	(4871, 1),
	(4873, 1),
	(4875, 1),
	(4889, 1),
	(4890, 1),
	(4891, 1),
	(4894, 1),
	(4895, 1),
	(4897, 1),
	(4900, 1),
	(4902, 1),
	(4903, 1),
	(4908, 1),
	(4909, 1),
	(4912, 1),
	(4913, 1),
	(4915, 1),
	(4916, 1),
	(4917, 1),
	(4918, 1),
	(4920, 1),
	(4921, 1),
	(4922, 1),
	(4923, 1),
	(4924, 1),
	(4926, 1),
	(4927, 1),
	(4928, 1),
	(4929, 1),
	(4930, 1),
	(4935, 1),
	(4936, 1),
	(4937, 1),
	(4938, 1),
	(4939, 1),
	(4940, 1),
	(4941, 1),
	(4942, 1),
	(4943, 1),
	(4947, 1),
	(4948, 1),
	(4951, 1),
	(4952, 1),
	(4953, 1),
	(4954, 1),
	(4955, 1),
	(4956, 1),
	(4957, 1),
	(4958, 1),
	(4959, 1),
	(4961, 1),
	(4962, 1),
	(4964, 1),
	(4965, 1),
	(4966, 1),
	(4971, 1),
	(4972, 1),
	(4973, 1),
	(4975, 1),
	(4978, 1),
	(4980, 1),
	(4981, 1),
	(4982, 1),
	(4983, 1),
	(4984, 1),
	(4987, 1),
	(4989, 1);

CREATE TABLE 
	[rtp_2015].[taz13_freight_distribute_hub] (
		[geography_zone_id] int,
		[category] tinyint,
		CONSTRAINT pk_taz13freight PRIMARY KEY ([geography_zone_id]),
		CONSTRAINT fk_taz13freight_zone FOREIGN KEY ([geography_zone_id]) REFERENCES [ref].[geography_zone] ([geography_zone_id])
	) 
ON 
	[ref_fg]
WITH 
	(DATA_COMPRESSION = PAGE)

INSERT INTO
	[rtp_2015].[taz13_freight_distribute_hub]
SELECT
	[geography_zone_id]
	,[category]
FROM
	#tt
INNER JOIN
	[ref].[geography_zone]
ON
	[geography_zone].[geography_type_id] = 34
	AND #tt.[taz13] = [geography_zone].[zone]

DROP TABLE
	#tt
GO

-- Add metadata for [rtp_2015].[taz13_freight_distribute_hub]
EXECUTE [db_meta].[add_xp] 'rtp_2015.taz13_freight_distribute_hub', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.taz13_freight_distribute_hub', 'MS_Description', 'series 13 taz freight distribution hub lookup table used in pm_4b'
GO




-- Create function for community of concern population by household geography zone
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[fn_coc_pop_zone]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
DROP FUNCTION [rtp_2015].[fn_coc_pop_zone]
GO

-- CoC population by household zone
CREATE FUNCTION [rtp_2015].[fn_coc_pop_zone] (
	@scenario_id smallint  
	)
RETURNS TABLE
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/18/2014 
	Description: CoC Population by household geography zone
*/

RETURN
(

SELECT  
	[lu_person].[scenario_id]
	,[geography_type_id]
	,[zone]
    ,SUM(CASE   WHEN [age] >= 75 THEN ISNULL(1 / NULLIF([sample_rate], 0), 0) ELSE 0 END) AS pop_senior
    ,SUM(CASE   WHEN [race_id] > 1  OR [hisp_id] > 1 THEN ISNULL(1 / NULLIF([sample_rate], 0), 0) ELSE 0 END) AS pop_minority
    ,SUM(CASE   WHEN ISNULL([poverty], 99) <= 2 THEN ISNULL(1 / NULLIF([sample_rate], 0), 0) ELSE 0 END) AS pop_low_inc
    ,SUM(ISNULL(1 / NULLIF([sample_rate], 0), 0)) AS tot_pop
FROM  
      [abm].[lu_person]
INNER JOIN
	[ref].[scenario]
ON
	[lu_person].[scenario_id] = [scenario].[scenario_id]
INNER JOIN 
      [abm].[lu_hh]
ON 
	[lu_person].[scenario_id] = [lu_hh].[scenario_id]
	AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id]
INNER JOIN
	[ref].[geography_zone]
ON
	[lu_hh].[geography_zone_id] = [geography_zone].[geography_zone_id]
WHERE 
	[lu_person].[scenario_id] = @scenario_id
	AND [lu_hh].[scenario_id] = @scenario_id
GROUP BY 
	[lu_person].[scenario_id]
	,[geography_type_id]
	,[zone]
)
GO

-- Add metadata for [rtp_2015].[fn_coc_pop_zone]
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_coc_pop_zone', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_coc_pop_zone', 'MS_Description', 'coc population by household geography zone'
GO




-- Create function for household community of concern transportation cost
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[fn_pm_5a_coc_transp_cost]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
DROP FUNCTION [rtp_2015].[fn_pm_5a_coc_transp_cost]
GO

CREATE FUNCTION [rtp_2015].[fn_pm_5a_coc_transp_cost] (
	@scenario_id smallint
	,@ao_cost real -- $/mile for auto operating cost, year specific
	)
RETURNS TABLE
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/18/2014 
	Description: Used in Performance Measure 5a, Change in percent of income consumed by transportation cost
					(COC /Non COC population)
		returns a table of percent of income consumed by transportation cost for each household whose income > 0
		and the household status by lowincome, senior, minority and COC 
*/

RETURN
(

with hh_status as (
SELECT 
	[lu_hh].[scenario_id]
	,[lu_hh].[lu_hh_id]
	,MAX([hh_income]) AS [hh_income] -- same across all records when grouped by hh_id
    ,MAX(CASE WHEN ISNULL([poverty], 99) <= 2 THEN 1 ELSE 0 END) AS p_lowinc   
	,MAX(CASE WHEN [race_id] > 1 OR [hisp_id] > 1 THEN 1 ELSE 0 END) AS p_minority
	,MAX(CASE WHEN [age] >= 75  THEN 1 ELSE 0 END) AS p_senior	
	,MAX(CASE WHEN ISNULL([poverty], 99) <= 2 OR [race_id] > 1 OR [hisp_id] > 1 OR [age] >= 75 THEN 1 ELSE 0 END) AS coc_pop
	,MAX(CASE WHEN ISNULL([poverty], 99) > 2 AND [race_id] = 1 AND [hisp_id] = 1 AND [age] < 75 THEN 1 ELSE 0 END) AS non_coc_pop
FROM 
	[abm].[lu_hh] 
INNER JOIN 
	[abm].[lu_person]
ON 
	[lu_hh].[scenario_id] = [lu_person].[scenario_id]
	AND [lu_hh].[lu_hh_id] = [lu_person].[lu_hh_id]
WHERE 
	[lu_hh].[scenario_id] = @scenario_id
	AND [lu_person].[scenario_id] = @scenario_id
	AND [unit_type_id] = 0 -- Only households have income data, gq do not
GROUP BY 
	[lu_hh].[scenario_id]
	,[lu_hh].[lu_hh_id]
	),
-- Auto trips
-- Join trip data to list of persons on each tour
-- Joint trips will now appear more than once, take this into account in costs
-- Create sums of time, cost, distance*AOCost by household
auto_info AS (
SELECT
	[lu_hh_id]
	,ISNULL(trip_cost, 0) + ISNULL(auto_cost, 0) + ISNULL(parking_cost, 0) AS trip_cost
FROM (
	SELECT	
		[lu_person].[lu_hh_id]
		-- Sum Distance in miles * ao_cost to get auto operating cost for auto modes
		-- Shared ride trips split cost among participants
		-- For Shared ride joint trips dividing by partysize and then summing over joined person table will get back to original cost
		,SUM(CASE	WHEN [trip_ij].[mode_id] IN (1,2) THEN @ao_cost * [trip_distance]
					WHEN [tour_ij].[model_type_id] = 0 AND [trip_ij].[mode_id] IN (3,4,5) THEN @ao_cost * [trip_distance] / 2
					WHEN [tour_ij].[model_type_id] = 0 AND [trip_ij].[mode_id] IN (6,7,8) THEN @ao_cost * [trip_distance] / 3.34
					WHEN [tour_ij].[model_type_id] = 1 AND [trip_ij].[mode_id] IN (3,4,5,6,7,8) THEN @ao_cost * [trip_distance] / [party_size]
					ELSE 0
					END) AS auto_cost
		-- Sum Trip Cost
		-- Shared ride trips split cost among participants
		-- For Shared ride joint trips dividing by partysize and then summing over joined person table will get back to original cost 
		-- For transit trips, everyone pays the fare, not split
		,SUM(CASE	WHEN [trip_ij].[mode_id] IN (1,2) THEN [trip_cost]
					WHEN [tour_ij].[model_type_id] = 0 AND [trip_ij].[mode_id] IN (3,4,5) THEN [trip_cost] / 2
					WHEN [tour_ij].[model_type_id] = 0 AND [trip_ij].[mode_id] IN (6,7,8) THEN [trip_cost] / 3.34
					WHEN [tour_ij].[model_type_id] = 1 AND [trip_ij].[mode_id] IN (3,4,5,6,7,8) THEN [trip_cost] / [party_size]
					ELSE [trip_cost]
					END) AS trip_cost
		,SUM(CASE	WHEN [fp_choice_id] = 2 AND [dparkcost] > [mparkcost] THEN [mparkcost]
					WHEN [fp_choice_id] = 2 AND [dparkcost] < [mparkcost] THEN [dparkcost]
					WHEN [fp_choice_id] = 3 AND [reimb_pct] < 1.0 AND [dparkcost] > [mparkcost] THEN (1 - [reimb_pct]) * [mparkcost]
					WHEN [fp_choice_id] = 3 AND [reimb_pct] < 1.0 AND [dparkcost] < [mparkcost] THEN (1 - [reimb_pct]) * [dparkcost]
					ELSE 0
					END) AS parking_cost
	FROM 
		[abm].[trip_ij] -- only models tied to PopSyn population
	INNER JOIN
		[abm].[tour_ij]
	ON 
		[trip_ij].[scenario_id] = [tour_ij].[scenario_id]
		AND [trip_ij].[tour_ij_id] = [tour_ij].[tour_ij_id]
	INNER JOIN 
		[abm].[tour_ij_person]
	ON 
		[tour_ij].[scenario_id] = [tour_ij_person].[scenario_id]
		AND [tour_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
	INNER JOIN
		[abm].[lu_person]
	ON
		[tour_ij_person].[scenario_id] = [lu_person].[scenario_id]
		AND [tour_ij_person].[lu_person_id] = [lu_person].[lu_person_id]
	LEFT OUTER JOIN  
		[abm].[lu_mgra_input]
	ON 
		[trip_ij].[parking_geography_zone_id] = [lu_mgra_input].[geography_zone_id]
	LEFT OUTER JOIN  
		[abm].[lu_person_fp]
	ON
		[tour_ij_person].[scenario_id] = [lu_person_fp].[scenario_id]
		AND [tour_ij_person].[lu_person_id] = [lu_person_fp].[lu_person_id]
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
		AND [tour_ij].[scenario_id] = @scenario_id
		AND [tour_ij_person].[scenario_id] = @scenario_id
		AND [lu_person].[scenario_id] = @scenario_id
		AND [trip_ij].[mode_id] BETWEEN 1 AND 8 -- auto modes
	GROUP BY 
		[lu_person].[lu_hh_id]
		)tt
	),
-- Join transit trips to list of persons on each tour
--  the highest transit fare is selected by person and tour
transit_info AS (
SELECT
	[lu_hh_id]
	,SUM(CASE WHEN [age] >= 60 AND [mode_id] in (15,20,25) then trip_cost * 0.5 * 0.667
		                  WHEN [age] < 60 AND [mode_id] in (15,20,25) then trip_cost * 0.667
		                  ELSE trip_cost END
		                  ) AS trip_cost
FROM (		                  
	SELECT	
		[lu_person].[lu_hh_id]
		,MAX([age]) AS [age] -- same across all records when grouped by hh_id and pnum
		--for commuter rail and BRT mode, cap the trip cost at daily max of $12.0, for the rest cap at $5.0
		,CASE WHEN SUM([trip_cost]) > 12 AND MAX([mode_id]) IN (13, 15, 18, 20, 23, 25) THEN 12.0 
			  WHEN SUM([trip_cost]) > 5 AND MAX([mode_id]) NOT IN (13, 15, 18, 20, 23, 25) THEN 5.0
			  ELSE SUM([trip_cost])
			  END AS trip_cost
		,MAX([mode_id]) AS [mode_id]
	FROM 
		[abm].[trip_ij] -- only models tied to PopSyn population
	INNER JOIN 
		[abm].[tour_ij_person]
	ON 
		[trip_ij].[scenario_id] = [tour_ij_person].[scenario_id]
		AND [trip_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
	INNER JOIN
		[abm].[lu_person]
	ON 
		[tour_ij_person].[scenario_id] = [lu_person].[scenario_id]
		AND [tour_ij_person].[lu_person_id] = [lu_person].[lu_person_id]
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
		AND [tour_ij_person].[scenario_id] = @scenario_id
		AND [lu_person].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- transit modes
	GROUP BY 
		[lu_person].[lu_hh_id]
		,[lu_person].[pnum]
		) tt
GROUP BY
	[lu_hh_id]
	)
SELECT
	hh_status.[scenario_id]
	,hh_status.[lu_hh_id]
	,p_lowinc
	,p_minority
	,p_senior
	,coc_pop
	,non_coc_pop
	,[hh_income]
    --for household income = 0, set percent of transportation cost to 0
	,CASE	WHEN [hh_income] = 0 THEN 0.0 
			--Cap the anual percent of transportation cost at 100% if exceeding 100%
			WHEN [hh_income] > 0 AND 300.0 * ISNULL(avg_cost, 0) / (CAST([hh_income] AS real)) > 1 THEN 1.0
			ELSE 300.0 * ISNULL(avg_cost, 0) / (CAST([hh_income] AS real)) 
			END AS transp_pct_cost	
FROM
	hh_status
INNER JOIN ( -- only take households that travelled
	SELECT
		[lu_hh_id]
		,avg(trip_cost) AS avg_cost
	FROM (
		SELECT
			[lu_hh_id]
			,trip_cost
		FROM
			auto_info
		UNION ALL
		SELECT
			[lu_hh_id]
			,trip_cost
		FROM
			transit_info
			)tt1
	GROUP BY
		[lu_hh_id]
		) tt2
ON
	hh_status.[lu_hh_id] = tt2.[lu_hh_id]
WHERE
	[hh_income] > 0
)
GO

-- Add metadata for [rtp_2015].[fn_pm_coc_transp_cost]
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_pm_5a_coc_transp_cost', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_pm_5a_coc_transp_cost', 'MS_Description', 'household transportation cost by coc, used in pm_5a'
GO




-- Create function for work trips by mode and CoC
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[fn_pm_1a_7d_work_trips]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
DROP FUNCTION [rtp_2015].[fn_pm_1a_7d_work_trips]
GO

CREATE FUNCTION [rtp_2015].[fn_pm_1a_7d_work_trips] (
	@scenario_id smallint  
	,@inbound tinyint
	,@peak_period bit
	)
RETURNS TABLE
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/22/2014 
	Description: Used in Performance Measure 1a and 7d,
*/

RETURN
(

with raw_work_trips AS (
SELECT
	[trip_ij].[scenario_id] 
	,wtour.[tour_ij_id]
	,[trip_ij].[mode_id] AS trip_mode
	,[trip_time]
	,[trip_distance]
	,wtour.[model_type_id]
	,[tour_leg_dest_geography_zone_id]
	,wtour.[mode_id] AS tour_mode
	,wtour.low_inc
	,wtour.minority
	,wtour.senior
FROM 
	[abm].[trip_ij]
INNER JOIN (
-- work tour 
SELECT 
	[tour_ij].[scenario_id]
	,[tour_ij].[model_type_id]
	,[tour_ij].[tour_ij_id]
	,[lu_person].[lu_hh_id]
	,[lu_person].[pnum]
	,[tour_ij].[purpose_id]
	,[tour_ij].[mode_id]
	,CASE	WHEN @inbound = 0 THEN [dest_geography_zone_id]
			WHEN @inbound = 1 THEN [orig_geography_zone_id]
			ELSE NULL END AS [tour_leg_dest_geography_zone_id]
	,(CASE	WHEN ISNULL([poverty], 99) <= 2 THEN 1 
			ELSE 0 
			END) AS low_inc 
	,(CASE	WHEN [race_id] > 1 OR [hisp_id] > 1 THEN 1 
			ELSE 0 
			END) AS minority
	,(CASE	WHEN [age] >= 75 THEN 1 
			ELSE 0 
			END) AS senior
FROM 
	[abm].[tour_ij]
INNER JOIN 
	[abm].[tour_ij_person]
ON 
	[tour_ij].[scenario_id] = [tour_ij_person].[scenario_id] 
	AND [tour_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id] 
INNER JOIN 
	[abm].[lu_person]
ON 
	[tour_ij_person].[scenario_id] = [lu_person].[scenario_id] 
	AND [tour_ij_person].[lu_person_id] = [lu_person].[lu_person_id] 
INNER JOIN 
	[abm].[lu_hh]
ON 
	[lu_person].[scenario_id] = [lu_hh].[scenario_id] 
	AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id]
INNER JOIN
	[ref].[time_period]
ON
	[tour_ij].[start_time_period_id] = [time_period].[time_period_id]
INNER JOIN
	[ref].[purpose]
ON
	[tour_ij].[purpose_id] = [purpose].[purpose_id]
WHERE 
	[tour_ij].[scenario_id] = @scenario_id
	AND [tour_ij].[model_type_id] = 0 --individual only
	AND [tour_ij].[tour_cat_id] = 0 -- mandatory tour
	AND [purpose_number] = 0 -- work_purpose
	AND		(CASE	WHEN @peak_period = 1 AND [time_resolution_id] = 2 -- assumes ABM half hour time periods
						AND ([time_period_number] BETWEEN 4 AND 9 OR [time_period_number] BETWEEN 23 AND 29) THEN 1 -- peak period
					WHEN @peak_period = 0 THEN 1
					ELSE 0
					END) = 1
	) AS wtour
ON 
	[trip_ij].[scenario_id] = wtour.[scenario_id] 
	AND [trip_ij].[tour_ij_id] = wtour.[tour_ij_id] 
WHERE 
	[inbound] = @inbound
	),

/*
Determine the main tour mode (didn't just take tour_mode):
tours with only one unique trip_mode, take trip_mode
tours with all trip_mode <= 8, take trip_mode with max trip_time, if tied take tour_mode
tours with min of trip_mode <= 8 and  max of trip_mode BETWEEN 9 and 10, take minimum of trip_mode
tours with all trip_mode BETWEEN 9 and 10, take maximum of trip_mode
tours with all trip modes > 8, take maximum of trip_mode
tours with any trip modes > 10, take maximum of trip_mode

NOTE: this table creates NULL values of mode_category for the trips where 
max_trip_mode <= 8 AND ties = 1 AND max_trip_time != [trip_time]
*/
pm_data AS (
SELECT
	[raw_work_trips].[tour_ij_id]
	,CASE	WHEN num_tour_modes = 1 THEN trip_mode -- case statement stops at first true evaluation so this works
			WHEN max_trip_mode <= 8 AND ties = 1 AND max_trip_time = [trip_time]  THEN trip_mode
			WHEN max_trip_mode <= 8 AND ties > 1 THEN tour_mode
			WHEN min_trip_mode <= 8 AND max_trip_mode BETWEEN 9 AND 10 THEN min_trip_mode
			WHEN min_trip_mode > 8 THEN max_trip_mode
			WHEN max_trip_mode > 10 THEN max_trip_mode
			END AS mode_category
FROM
	raw_work_trips
LEFT OUTER JOIN ( -- bring in min and max trip modes by tour
	SELECT
		[tour_ij_id]
		,MIN(trip_mode) AS min_trip_mode
		,MAX(trip_mode) AS max_trip_mode
	FROM
		raw_work_trips
	GROUP BY
		[tour_ij_id]
	) min_max
ON
	raw_work_trips.[tour_ij_id] = min_max.[tour_ij_id]
LEFT OUTER JOIN ( -- bring in max trip time by tour
	SELECT 
		[tour_ij_id]
		,MAX([trip_time]) AS max_trip_time
	FROM 
		raw_work_trips
	GROUP BY
		[tour_ij_id]
	) find_max_time
ON
	raw_work_trips.[tour_ij_id] = find_max_time.[tour_ij_id]
LEFT OUTER JOIN ( -- bring in tours where the max trip time is shared by two trips
	SELECT
		raw_work_trips.[tour_ij_id]
		,COUNT(*) AS ties
	FROM
		raw_work_trips
	INNER JOIN (
		SELECT 
			[tour_ij_id]
			,MAX([trip_time]) AS max_trip_time
		FROM 
			raw_work_trips
		GROUP BY
			[tour_ij_id]
			) find_max_time_tt
	ON
		raw_work_trips.[tour_ij_id] = find_max_time_tt.[tour_ij_id]
		AND raw_work_trips.[trip_time] = find_max_time_tt.max_trip_time
	GROUP BY
		raw_work_trips.[tour_ij_id]
	) tied_trips
ON
	raw_work_trips.[tour_ij_id] = tied_trips.[tour_ij_id]
LEFT OUTER JOIN ( -- bring in tours with one trip mode
	SELECT
		[tour_ij_id]
		,COUNT(*) as num_tour_modes
	FROM (
		SELECT
			[tour_ij_id]
			,trip_mode
		FROM
			raw_work_trips
		GROUP BY
			[tour_ij_id]
			,trip_mode
		) tt
	GROUP BY
		[tour_ij_id]
	) count_tour_modes
ON
	raw_work_trips.[tour_ij_id] = count_tour_modes.[tour_ij_id]
	)

SELECT
	raw_work_trips.[scenario_id]
	,raw_work_trips.[tour_ij_id]
	,low_inc
	,minority
	,senior
	,CASE	WHEN mode_category BETWEEN 1 AND 2 THEN '1 drive_alone'
			WHEN mode_category BETWEEN 3 AND 8 THEN '2 sr'
			WHEN mode_category  = 9 THEN '5 walk'
			WHEN mode_category  = 10 THEN '4 bike'
			WHEN mode_category BETWEEN 11 AND 25 THEN '3 transit'
			ELSE '6 other'
			END AS mode_category
	,[geography_type_id]
	,[zone]
	,[trip_time]
	,[trip_distance]
FROM
	raw_work_trips
INNER JOIN ( -- Handle the null mode_category values
	SELECT
		[tour_ij_id]
		,MAX(mode_category) AS mode_category
	FROM
		pm_data
	WHERE
		mode_category IS NOT NULL
	GROUP BY
		[tour_ij_id]
	) tt
ON
	raw_work_trips.[tour_ij_id] = tt.[tour_ij_id]
INNER JOIN
	[ref].[geography_zone]
ON
	raw_work_trips.[tour_leg_dest_geography_zone_id] = [geography_zone].[geography_zone_id]
)
GO

-- Add metadata for [rtp_2015].[fn_pm_work_trips]
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_pm_1a_7d_work_trips', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_pm_1a_7d_work_trips', 'MS_Description', 'work trips by mode and coc, used in performance measures 1a and 7d'
GO




-- Function to return a list of unique stop nodes
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[fn_stop_nodes]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
DROP FUNCTION [rtp_2015].[fn_stop_nodes]
GO

CREATE FUNCTION [rtp_2015].[fn_stop_nodes] (
	@scenario_id smallint
	)
RETURNS TABLE
AS

/*	Author: Ziying Ouyang
	Date: Revised 9/18/2014 
	Description: Unique stop nodes
*/

RETURN
(

SELECT
	DISTINCT [transit_stop].[scenario_id], [near_node] AS stop_node
FROM
	[abm].[transit_stop]
INNER JOIN
	[abm].[transit_route]
ON
	[transit_stop].[scenario_id] = [transit_route].[scenario_id]
	AND [transit_stop].[transit_route_id] = [transit_route].[transit_route_id]
WHERE
	[transit_stop].[scenario_id] = @scenario_id
	AND [transit_route].[scenario_id] = @scenario_id
	AND ([am_headway] > 0 OR [op_headway] > 0 OR [pm_headway] > 0)
)
GO

-- Add metadata for [rtp_2015].[fn_stop_nodes]
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_stop_nodes', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_stop_nodes', 'MS_Description', 'function to return a list of unique stop nodes'
GO




-- Function to get unique high frequency stop nodes.
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[fn_stop_nodes_high_freq]') AND type in (N'FN', N'IF', N'TF', N'FS', N'FT'))
DROP FUNCTION [rtp_2015].[fn_stop_nodes_high_freq]
GO

CREATE FUNCTION [rtp_2015].[fn_stop_nodes_high_freq] (
	@scenario_id smallint
	,@frequency FLOAT
	)
RETURNS TABLE
AS

/*	Author: Ziying Ouyang
	Date: Revised 9/18/2014 
	Description: Unique high frequency stop nodes. Check to see if any nodes has the same route direction but different config.
				For these cases to use combined headways to to filter as high frequency stops.
*/

RETURN
(

with t as (
SELECT DISTINCT 
	[transit_stop].[scenario_id]
	,[near_node]
	,[config]
	,[config] / 1000 AS [route]
	,([config] - 1000 * ([config] / 1000)) / 100 AS direction 
	,[am_headway]
	,[pm_headway]
	,[op_headway]
	,CASE WHEN [am_headway] > 0 THEN 60 / [am_headway] ELSE 0 END AS num_vehicle_am
	,CASE WHEN [pm_headway] > 0 THEN 60 / [pm_headway] ELSE 0 END AS num_vehicle_pm
	,CASE WHEN [op_headway] > 0 THEN 60 / [op_headway] ELSE 0 END AS num_vehicle_op
FROM 
	[abm].[transit_stop]
INNER JOIN 
	[abm].[transit_route]
ON 
	[transit_stop].[scenario_id] = [transit_route].[scenario_id]
	AND [transit_stop].[transit_route_id] = [transit_route].[transit_route_id]
WHERE
	[transit_stop].[scenario_id] = @scenario_id
	AND [transit_route].[scenario_id] = @scenario_id
	)
SELECT
	DISTINCT
	[scenario_id]
	,[near_node] AS high_freq_stop_node
FROM (
	SELECT
		[scenario_id]
		,[near_node]
		,SUM(num_vehicle_op) AS num_vehicle_op
	FROM 
		t
	GROUP BY
		[scenario_id]
		,[near_node]
		,[route]
		,direction
		) tt
WHERE
	CASE WHEN num_vehicle_op > 0 THEN 60 / num_vehicle_op ELSE 0 END <= @frequency
	AND CASE WHEN num_vehicle_op > 0 THEN 60 / num_vehicle_op ELSE 0 END > 0
)
GO

-- Add metadata for [rtp_2015].[fn_stop_nodes_high_freq]
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_stop_nodes_high_freq', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.fn_stop_nodes_high_freq', 'MS_Description', 'Function to return a list of unique high frequency stop nodes'
GO




-- Create stored procedure for auto corridor time skim
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_corridor_time_skim_auto]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_corridor_time_skim_auto]
GO

CREATE PROCEDURE [rtp_2015].[sp_corridor_time_skim_auto]
	@scenario_id smallint
	,@geography_type_id tinyint
	,@orig smallint
	,@dest smallint
	,@time_resolution_id tinyint
	,@time_period_number tinyint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/17/2014
	Description: Highway skim times
*/

SELECT
	[scenario_id]
	,orig.[geography_type_id]
	,orig.[zone]
	,dest.[zone]
	,[time_resolution_id]
	,[time_period_number]
	,[time_drive_alone_free]
	,[time_hov2_free]
	,[time_hov3_free]
FROM
	[abm].[hwy_skims]
INNER JOIN
	[ref].[geography_zone] AS orig
ON
	[hwy_skims].[orig_geography_zone_id] = orig.[geography_zone_id]
INNER JOIN
	[ref].[geography_zone] AS dest
ON
	[hwy_skims].[dest_geography_zone_id] = dest.[geography_zone_id]
INNER JOIN
	[ref].[time_period]
ON
	[hwy_skims].[time_period_id] = [time_period].[time_period_id]
WHERE
	[scenario_id] = @scenario_id
	AND orig.[geography_type_id] = @geography_type_id
	AND dest.[geography_type_id] = @geography_type_id
	AND orig.[zone] = @orig
	AND dest.[zone] = @dest
	AND [time_resolution_id] = @time_resolution_id
	AND [time_period_number] = @time_period_number
GO

-- Add metadata for [rtp_2015].[sp_corridor_time_skim_auto]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_corridor_time_skim_auto', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_corridor_time_skim_auto', 'MS_Description', 'finds specified free time skims for auto modes'
GO




-- Create stored procedure for transit corridor time skim
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_corridor_time_skim_transit]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_corridor_time_skim_transit]
GO

CREATE PROCEDURE [rtp_2015].[sp_corridor_time_skim_transit]
	@scenario_id smallint
	,@orig_tap smallint
	,@dest_tap smallint
	,@time_resolution_id tinyint
	,@time_period_number tinyint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/17/2014
	Description: Transit tap skim times
*/

SELECT  
	[transit_tap_skims].[scenario_id]
    ,orig.[tap]
    ,dest.[tap]
    ,[time_resolution_id]
    ,[time_period_number]
    ,[ivt_premium] + [walk_time_premium] + [transfer_time_premium] AS transit_travel_time
FROM 
	[abm].[transit_tap_skims]
INNER JOIN
	[abm].[transit_tap] AS orig
ON
	[transit_tap_skims].[scenario_id] = orig.[scenario_id]
	AND [transit_tap_skims].[orig_transit_tap_id] = orig.[transit_tap_id]
INNER JOIN
	[abm].[transit_tap] AS dest
ON
	[transit_tap_skims].[scenario_id] = dest.[scenario_id]
	AND [transit_tap_skims].[dest_transit_tap_id] = dest.[transit_tap_id]
INNER JOIN
	[ref].[time_period]
ON
	[transit_tap_skims].[time_period_id] = [time_period].[time_period_id]
WHERE
	[transit_tap_skims].[scenario_id] = @scenario_id
	AND orig.[scenario_id] = @scenario_id
	AND dest.[scenario_id] = @scenario_id
	AND orig.[tap] = @orig_tap
	AND dest.[tap] = @dest_tap
	AND [time_resolution_id] = @time_resolution_id
	AND [time_period_number] = @time_period_number
GO

-- Add metadata for [rtp_2015].[corridor_time_skim_transit]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_corridor_time_skim_transit', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_corridor_time_skim_transit', 'MS_Description', 'finds specified walk + ivt + transfer time tap skim'
GO




-- Create stored procedure for ped/bike miles traveled
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pmt_bmt]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pmt_bmt]
GO

CREATE PROCEDURE [rtp_2015].[sp_pmt_bmt]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang 
	Date: Revised 9/18/2014
	Description: Generates non-motorized miles traveled for PM 3a/b
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

SELECT 
	@scenario_id AS [scenario_id]
	 ,SUM(CASE	WHEN [mode_id] = 9 THEN ISNULL([party_size] * [trip_distance], 0)
				ELSE 0
				END) / @sample_rate AS pmt
	 ,SUM(CASE	WHEN [mode_id] = 10 THEN ISNULL([party_size] * [trip_distance], 0)
				ELSE 0 
				END) / @sample_rate AS bmt
FROM 
	[abm].[vi_trip_micro_simul] -- view
WHERE 
	[vi_trip_micro_simul].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 9 AND 10	
GO

-- Add metadata for [rtp_2015].[sp_pmt_bmt]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pmt_bmt', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pmt_bmt', 'MS_Description', 'pedestrian and bike miles traveled for pm_3a and pm_3b'
GO





-- Create stored procedure for transit flow by screenline
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_screenline_transit_peak_flow]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_screenline_transit_peak_flow]
GO

CREATE PROCEDURE [rtp_2015].[sp_screenline_transit_peak_flow]
	@scenario_id smallint
AS

/*	Author: Gregor Schroeder
	Date: Revised 9/25/2014
	Description: Get transit flow by screenline for the peak period
*/

SELECT
	@scenario_id AS [scenario_id]
	,[screenline_id]
	,SUM([transit_flow]) AS transit_flow
FROM
	[abm].[transit_aggflow]
INNER JOIN (
	SELECT
		[scenario_id]
		,[transit_link_id]
		,[screenline_id]
	FROM
		[abm].[transit_link]
	INNER JOIN
		[rtp_2015].[screenline]
	ON
		[transit_link].[shape].STIntersects([screenline].[shape]) = 1 -- Intersect screenline and trcov
	WHERE
		[transit_link].[scenario_id] = @scenario_id
		) tt
ON
	[transit_aggflow].[scenario_id] = tt.[scenario_id]
	AND [transit_aggflow].[transit_link_id] = tt.[transit_link_id]
INNER JOIN
	[ref].[time_period]
ON
	[transit_aggflow].[time_period_id] = [time_period].[time_period_id]
WHERE
	[transit_aggflow].[scenario_id] = @scenario_id
	AND [time_resolution_id] = 1
	AND [time_period_number] IN (2,4)
GROUP BY
	[screenline_id]
ORDER BY
	[screenline_id]
GO

-- Add metadata for [rtp_2015].[sp_screenline_transit_flow]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_screenline_transit_peak_flow', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_screenline_transit_peak_flow', 'MS_Description', 'transit flow by screenline for the peak period'
GO




-- Create stored procedure for auto flow by screenline
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_screenline_auto_peak_flow]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_screenline_auto_peak_flow]
GO

CREATE PROCEDURE [rtp_2015].[sp_screenline_auto_peak_flow]
	@scenario_id smallint
AS

/*	Author: Gregor Schroeder
	Date: Revised 9/25/2014
	Description: Get auto flow by screenline for the peak period
*/

SELECT
	@scenario_id AS [scenario_id]
	,[screenline_id]
	,SUM(CASE WHEN [mode_id] BETWEEN 1 AND 2 THEN [hwy_flow_mode].[flow] ELSE 0 END) AS drive_alone_flow
	,SUM(CASE WHEN [mode_id] BETWEEN 3 AND 8 THEN [hwy_flow_mode].[flow] ELSE 0 END) AS carpool_flow
	,SUM(CASE WHEN [mode_id] BETWEEN 32 AND 37 THEN [hwy_flow_mode].[flow] ELSE 0 END) AS truck_flow
	,SUM([hwy_flow_mode].[flow]) AS flow
FROM
	[abm].[hwy_flow_mode]
INNER JOIN
	[abm].[hwy_flow]
ON
	[hwy_flow_mode].[scenario_id] = [hwy_flow].[scenario_id]
	AND [hwy_flow_mode].[hwy_flow_id] = [hwy_flow].[hwy_flow_id]
INNER JOIN
	[abm].[hwy_link_ab_tod]
ON
	[hwy_flow].[scenario_id] = [hwy_link_ab_tod].[scenario_id]
	AND [hwy_flow].[hwy_link_ab_tod_id] = [hwy_link_ab_tod].[hwy_link_ab_tod_id]
INNER JOIN
	[abm].[hwy_link_tod]
ON
	[hwy_link_ab_tod].[scenario_id] = [hwy_link_tod].[scenario_id]
	AND [hwy_link_ab_tod].[hwy_link_tod_id] = [hwy_link_tod].[hwy_link_tod_id]
INNER JOIN (
	SELECT
		[scenario_id]
		,[hwy_link_id]
		,[screenline_id]
	FROM
		[abm].[hwy_link]
	INNER JOIN
		[rtp_2015].[screenline]
	ON
		[hwy_link].[shape].STIntersects([screenline].[shape]) = 1 -- Intersect screenline and hwycov
	WHERE
		[hwy_link].[scenario_id] = @scenario_id
		) tt
ON
	[hwy_link_tod].[scenario_id] = tt.[scenario_id]
	AND [hwy_link_tod].[hwy_link_id] = tt.[hwy_link_id]
INNER JOIN
	[ref].[time_period]
ON
	[hwy_link_tod].[time_period_id] = [time_period].[time_period_id]
WHERE
	[hwy_flow_mode].[scenario_id] = @scenario_id
	AND [hwy_flow].[scenario_id] = @scenario_id
	AND [hwy_link_ab_tod].[scenario_id] = @scenario_id
	AND [hwy_link_tod].[scenario_id] = @scenario_id
	AND [time_resolution_id] = 1
	AND [time_period_number] IN (2,4)
GROUP BY
	[screenline_id]
ORDER BY
	[screenline_id]
GO

-- Add metadata for [rtp_2015].[sp_screenline_auto_peak_flow]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_screenline_auto_peak_flow', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_screenline_auto_peak_flow', 'MS_Description', 'auto flow by screenline for the peak period'
GO




-- Create stored procedure for transit vmt, vht
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_transit_vmt_vht_summary]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_transit_vmt_vht_summary]
GO

CREATE PROCEDURE [rtp_2015].[sp_transit_vmt_vht_summary]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang 
	Date: Revised 9/18/2014
	Description: summarize total vehicle minutes traveled by tod
	,hourly number of vehicle by tod
*/

SELECT
	@scenario_id AS [scenario_id]
	,[transit_mode_id]
	,SUM(miles * ISNULL(frequency, 0)) AS vehicle_miles_travel
	,SUM((dwell_time + total_linktime) * ISNULL(frequency, 0)) AS vehicle_minutes_travel
FROM (
	-- transit flow aggregation joined to transit route
	SELECT
		tt1.[scenario_id]
		,[transit_route].[transit_mode_id]
		,miles
		,total_linktime
		,CASE	WHEN [transit_mode_id] = 10 THEN (number_stops - 1) * 0.3 
				WHEN [transit_mode_id] BETWEEN 6 AND 9 THEN (number_stops - 1) * 0.5
				ELSE 0 
				END AS dwell_time
		,CASE	WHEN [time_period_number] = 2 THEN ISNULL(180 / NULLIF([am_headway], 0) ,0) 
				WHEN [time_period_number] = 3 THEN ISNULL(390 / NULLIF([op_headway], 0) ,0) 
				WHEN [time_period_number] = 4 THEN ISNULL(210 / NULLIF([pm_headway], 0) ,0) 
				WHEN [time_period_number] = 5 AND ISNULL([nt_hour], 0) > 1 THEN ISNULL(([nt_hour] - 1) * 60 / NULLIF([nt_headway], 0) ,0) 
				END AS frequency
		,CASE	WHEN [time_period_number] = 2 THEN [am_headway]
				WHEN [time_period_number] = 3 THEN [op_headway]
				WHEN [time_period_number] = 4 THEN [pm_headway]
				WHEN [time_period_number] = 5 THEN [nt_headway]
				END AS headway
	FROM (
		-- transit flow aggregation to scenario, route, time
		SELECT
			[transit_flow].[scenario_id]
			,[transit_flow].[transit_route_id]
			,[time_resolution_id]
			,[time_period_number]
			,MAX([to_mp]) AS miles
			,SUM([baseivtt]) AS total_linktime
			,(MAX(to_stop_table.stop_id)- MIN(from_stop_table.stop_id))  + 1 AS number_stops
		FROM
			[abm].[transit_flow]
		INNER JOIN
			[abm].[transit_stop] AS from_stop_table
		ON
			[transit_flow].[from_transit_stop_id] = from_stop_table.[transit_stop_id]
		INNER JOIN
			[abm].[transit_stop] AS to_stop_table
		ON
			[transit_flow].[to_transit_stop_id] = to_stop_table.[transit_stop_id]
		INNER JOIN
			[ref].[time_period]
		ON
			[transit_flow].[time_period_id] = [time_period].[time_period_id]
		WHERE
			[transit_flow].[scenario_id] = @scenario_id
			AND from_stop_table.[scenario_id] = @scenario_id
			AND to_stop_table.[scenario_id] = @scenario_id
			AND [time_resolution_id] = 1
			AND [time_period_number] BETWEEN 1 AND 5
			AND [transit_flow].[transit_mode_id] = 5
			AND [transit_flow].[transit_access_mode_id] = 1
		GROUP BY
			[transit_flow].[scenario_id]
			,[transit_flow].[transit_route_id]
			,[time_resolution_id]
			,[time_period_number]) tt1
	INNER JOIN
		[abm].[transit_route]
	ON
		tt1.[scenario_id] = [transit_route].[scenario_id]
		AND tt1.[transit_route_id] = [transit_route].[transit_route_id]
	WHERE
		[transit_route].[scenario_id] = @scenario_id
		) tt2
WHERE 
	ISNULL(headway, 0) > 0 
GROUP BY
	[transit_mode_id]
ORDER BY
	[transit_mode_id]
GO

-- Add metadata for [rtp_2015].[sp_transit_vmt_vht_summary]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_transit_vmt_vht_summary', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_transit_vmt_vht_summary', 'MS_Description', 'transit vmt and vht summary'
GO




-- Create stored procedure for transit vmt, vht, by route and time period
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_transit_vmt_vht_summary_by_route_tod]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_transit_vmt_vht_summary_by_route_tod]
GO

CREATE PROCEDURE [rtp_2015].[sp_transit_vmt_vht_summary_by_route_tod]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang 
	Date: Revised 9/18/2014
	Description: summarize total vehicle minutes traveled by tod
	,hourly number of vehicle by tod
*/

SELECT
	[scenario_id]
	,[route_id]
	,route_config
	,[route]
	,dir
	,config
	,[transit_mode_id]
	,[time_resolution_id]
	,[time_period_number]
	,miles
	,total_linktime
	,number_stops
	,dwell_time
	,frequency
	,headway
	,miles * ISNULL(frequency, 0) AS vehicle_miles
	,dwell_time + total_linktime AS total_ivt
	,(dwell_time + total_linktime) * ISNULL(frequency, 0) AS vehicle_minutes_travel
	,CASE	WHEN ISNULL(headway, 0) > 0 THEN CEILING((dwell_time + total_linktime) / headway)
			ELSE 0 
			END AS hourly_num_of_vehicle
FROM (
	SELECT
		tt1.[scenario_id]
		,[route_id]
		,[config] AS route_config
		,[config] / 1000 AS [route]
		,([config] - 1000 * ([config]/1000))/100 AS dir
		,[config] - 100 * ([config] / 100) AS config
		,[transit_mode_id]
		,[time_resolution_id]
		,[time_period_number]
		,miles
		,total_linktime
		,number_stops 
		,CASE	WHEN [transit_mode_id] = 10 THEN (number_stops - 1) * 0.3 
				WHEN [transit_mode_id] BETWEEN 6 AND 9 THEN (number_stops - 1) * 0.5
				ELSE 0 
				END AS dwell_time
		,CASE	WHEN [time_period_number] = 2 THEN ISNULL(180 / NULLIF([am_headway], 0) ,0) 
				WHEN [time_period_number] = 3 THEN ISNULL(390 / NULLIF([op_headway], 0) ,0) 
				WHEN [time_period_number] = 4 THEN ISNULL(210 / NULLIF([pm_headway], 0) ,0) 
				WHEN [time_period_number] = 5 AND ISNULL([nt_hour], 0) > 1 THEN ISNULL(([nt_hour] - 1) * 60 / NULLIF([nt_headway], 0) ,0) 
				END AS frequency
		,CASE	WHEN [time_period_number] = 2 THEN [am_headway]
				WHEN [time_period_number] = 3 THEN [op_headway]
				WHEN [time_period_number] = 4 THEN [pm_headway]
				WHEN [time_period_number] = 5 THEN [nt_headway]
				END AS headway
	FROM (
		-- transit flow aggregation to scenario, route, time
		SELECT
			[transit_flow].[scenario_id]
			,[transit_flow].[transit_route_id]
			,[time_resolution_id]
			,[time_period_number]
			,MAX([to_mp]) AS miles
			,SUM([baseivtt]) AS total_linktime
			,(MAX(to_stop_table.[stop_id])- MIN(from_stop_table.[stop_id]))  + 1 AS number_stops
		FROM
			[abm].[transit_flow]
		INNER JOIN
			[abm].[transit_stop] AS from_stop_table
		ON
			[transit_flow].[from_transit_stop_id] = from_stop_table.[transit_stop_id]
		INNER JOIN
			[abm].[transit_stop] AS to_stop_table
		ON
			[transit_flow].[to_transit_stop_id] = to_stop_table.[transit_stop_id]
		INNER JOIN
			[ref].[time_period]
		ON
			[transit_flow].[time_period_id] = [time_period].[time_period_id]
		WHERE
			[transit_flow].[scenario_id] = @scenario_id
			AND from_stop_table.[scenario_id] = @scenario_id
			AND to_stop_table.[scenario_id] = @scenario_id 
			AND [time_resolution_id] = 1
			AND [time_period_number] BETWEEN 1 AND 5
			AND [transit_flow].[transit_mode_id] = 5
			AND [transit_flow].[transit_access_mode_id] = 1
		GROUP BY
			[transit_flow].[scenario_id]
			,[transit_flow].[transit_route_id]
			,[time_resolution_id]
			,[time_period_number]) tt1
	INNER JOIN
		[abm].[transit_route]
	ON
		tt1.[scenario_id] = [transit_route].[scenario_id]
		AND tt1.[transit_route_id] = [transit_route].[transit_route_id]
	WHERE
		[transit_route].[scenario_id] = @scenario_id
		) tt2
WHERE 
	ISNULL(headway, 0) > 0 
ORDER BY 
	[route_id]
	,[time_resolution_id]
	,[time_period_number]
GO

-- Add metadata for [rtp_2015].[sp_transit_vmt_vht_summary_by_route_tod]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_transit_vmt_vht_summary_by_route_tod', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_transit_vmt_vht_summary_by_route_tod', 'MS_Description', 'transit vmt and vht summary by route and time period'
GO




-- Create stored procedure for work from home share
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_work_from_home]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_work_from_home]
GO

CREATE PROCEDURE [rtp_2015].[sp_work_from_home]
	@scenario_id smallint
AS

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

-- total full and part time workers
DECLARE @tot_workers float
SET @tot_workers = (SELECT COUNT(*) AS workers FROM [abm].[lu_person] WHERE [pemploy_id] BETWEEN 1 AND 2 AND [scenario_id] = @scenario_id)

SELECT
	@scenario_id AS [scenario_id]
	,COUNT(*) / @sample_rate AS total_home_workers
	,COUNT(*) / @tot_workers AS share_of_workers
FROM
	[abm].[lu_person_lc]
INNER JOIN
	[ref].[loc_choice_segment]
ON
	[lu_person_lc].[loc_choice_segment_id] = [loc_choice_segment].[loc_choice_segment_id]
WHERE
	[lu_person_lc].[scenario_id] = @scenario_id
	AND [loc_choice_id] = 1 -- work location choice model result
	AND [loc_choice_segment_number] = 99 -- work from home choice
GO

-- Add metadata for [rtp_2015].[sp_work_from_home]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_work_from_home', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_work_from_home', 'MS_Description', 'work from home share'
GO




-- Create stored procedure for active transportation project evaluation criteria #1
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].sp_eval_vmt') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_vmt]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_vmt]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/22/2014
	Description: VMT for Evaluation Criteria
*/

SELECT 
	@scenario_id AS [scenario_id]
	,SUM(([flow] + [preload] / 3.0) * [length_mile]) AS vmt
	,SUM(CASE WHEN [ijur] = 1 THEN ([flow] + [preload] / 3.0) * [length_mile] ELSE 0 END) AS vmt_ijur1
	,SUM(flow_auto * [length_mile]) AS vmt_auto
	,SUM(flow_truck * [length_mile]) AS vmt_truck
	,SUM([preload] * [length_mile]) / 3.0 AS vmt_bus
FROM
	[abm].[hwy_link_ab_tod]
INNER JOIN 
	[abm].[hwy_link_tod]
ON 
	[hwy_link_ab_tod].[scenario_id] = [hwy_link_tod].[scenario_id] 
	AND [hwy_link_ab_tod].[hwy_link_tod_id] = [hwy_link_tod].[hwy_link_tod_id]
INNER JOIN 
	[abm].[hwy_link]
ON 
	[hwy_link_tod].[scenario_id] = [hwy_link].[scenario_id] 
	AND [hwy_link_tod].[hwy_link_id] = [hwy_link].[hwy_link_id]
INNER JOIN
	[abm].[hwy_flow]
ON
	[hwy_link_ab_tod].[scenario_id] = [hwy_flow].[scenario_id] 
	AND [hwy_link_ab_tod].[hwy_link_ab_tod_id] = [hwy_flow].[hwy_link_ab_tod_id]
LEFT OUTER JOIN (
	SELECT
		[scenario_id]
		,[hwy_flow_id]
		,SUM(CASE WHEN [mode_id] BETWEEN 1 AND 8 THEN [flow] ELSE 0 END) AS flow_auto
		,SUM(CASE WHEN [mode_id] BETWEEN 32 AND 37 THEN [flow] ELSE 0 END) AS flow_truck
	FROM
		[abm].[hwy_flow_mode]
	WHERE
		[scenario_id] = @scenario_id
	GROUP BY
		[scenario_id]
		,[hwy_flow_id]
	) tt
ON
	[hwy_flow].[scenario_id] = tt.[scenario_id] 
	AND [hwy_flow].[hwy_flow_id] = tt.[hwy_flow_id]
WHERE 
	[hwy_link_ab_tod].[scenario_id] = @scenario_id
	AND [hwy_link_tod].[scenario_id] = @scenario_id
	AND [hwy_link].[scenario_id] = @scenario_id
	AND [hwy_flow].[scenario_id] = @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_vmt]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_vmt', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_vmt', 'MS_Description', 'project evaluation criteria vmt'
GO




-- Create stored procedure for active transportation project evaluation criteria #1
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_at_1]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_at_1]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_at_1]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Active Transportation 1, Total person trips across bike,walk,walk/pnr/knr transit modes
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

SELECT 
	[trip_ij].[scenario_id]
	,SUM([party_size]) / @sample_rate AS person_trips
FROM 
	[abm].[trip_ij] --individual and joint models only
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 9 AND 25 -- no taxi,school bus, includes KNR and PNR
GROUP BY
	[trip_ij].[scenario_id]
GO

-- Add metadata for [rtp_2015].[sp_eval_at_1]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_1', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_1', 'MS_Description', 'project evaluation criteria active transportation 1'
GO




-- Create stored procedure for active transportation project evaluation criteria #4
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_at_4]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_at_4]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_at_4]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Active Transportation 4, See Transit 4
*/

EXECUTE [rtp_2015].[sp_eval_transit_4] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_at_4]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_4', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_4', 'MS_Description', 'project evaluation criteria active transportation 4'
GO




-- Create stored procedure for active transportation project evaluation criteria #8
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_at_8]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_at_8]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_at_8]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Active Transportation 8, See Highway 8
*/

EXECUTE [rtp_2015].[sp_eval_hwy_8] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_at_8]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_8', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_at_8', 'MS_Description', 'project evaluation criteria active transportation 8'
GO




-- Create stored procedure for freeway connector project evaluation criteria #1
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_fwycon_1]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_fwycon_1]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_fwycon_1]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Freeway Connector 1, See Highway 1a
*/

EXECUTE [rtp_2015].[sp_eval_hwy_1a] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_fwycon_1]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_fwycon_1', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_fwycon_1', 'MS_Description', 'project evaluation criteria freeway connector 1'
GO




-- Create stored procedure for freeway connector project evaluation criteria #6
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_fwycon_6]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_fwycon_6]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_fwycon_6]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Freeway Connector 6, See Highway 10
*/

EXECUTE [rtp_2015].[sp_eval_hwy_10] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_fwycon_6]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_fwycon_6', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_fwycon_6', 'MS_Description', 'project evaluation criteria freeway connector 6'
GO




-- Create stored procedure for hov project evaluation criteria #1
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hov_1]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hov_1]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hov_1]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria HOV 1, See Highway 1a
*/

EXECUTE [rtp_2015].[sp_eval_hwy_1a] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_fwycon_1]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hov_1', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hov_1', 'MS_Description', 'project evaluation criteria hov 1'
GO




-- Create stored procedure for highway project evaluation criteria #1a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_1a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_1a]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_1a]
	@scenario_id smallint
	,@all_models bit = 0
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Highway 1A, Total person hours spent travelling
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

IF @all_models = 0
BEGIN
	SELECT	
		@scenario_id AS scenario_id
		,SUM(([party_size] * [trip_time]) / 60.0) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_ij] -- individual and joint models only, over all modes, includes access time and individual time components
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
END
ELSE
BEGIN
	with ap AS (
	SELECT	
		SUM(ISNULL(([party_size] * [trip_time]) / 60.0, 0)) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_ap]
	WHERE 
		[trip_ap].[scenario_id] = @scenario_id
		),
	cb AS (
	SELECT	
		SUM(([party_size] * [trip_time]) / 60.0) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_cb]
	WHERE 
		[trip_cb].[scenario_id] = @scenario_id
		),
	ie AS (
	SELECT	
		SUM(([party_size] * [trip_time]) / 60.0) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_ie]
	WHERE 
		[trip_ie].[scenario_id] = @scenario_id
		),
	ij AS (
	SELECT	
		SUM(([party_size] * [trip_time]) / 60.0) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_ij]
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
		),
	vis AS (
	SELECT	
		SUM(([party_size] * [trip_time]) / 60.0) / @sample_rate AS person_hour
	FROM 
		[abm].[trip_vis]
	WHERE 
		[trip_vis].[scenario_id] = @scenario_id
		)
	SELECT
		@scenario_id AS scenario_id
		,SUM(ISNULL(ap.person_hour, 0) + ISNULL(cb.person_hour, 0) + ISNULL(ie.person_hour, 0) + ISNULL(ij.person_hour, 0) + ISNULL(vis.person_hour, 0)) AS person_hour
	FROM
		ap, cb, ie, ij, vis
END
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_1a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_1a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_1a', 'MS_Description', 'project evaluation criteria highway 1a'
GO




-- Create stored procedure for highway project evaluation criteria #1b
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_1b]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_1b]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_1b]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Highway 1B, Total person hours spent travelling by LIM
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

SELECT	
	@scenario_id AS [scenario_id]
	,SUM([trip_time] / 60.0) / @sample_rate AS person_hour_coc
FROM 
	[abm].[trip_ij]
INNER JOIN
	[abm].[tour_ij]
ON
	[trip_ij].[scenario_id] = [tour_ij].[scenario_id]
	AND [trip_ij].[tour_ij_id] = [tour_ij].[tour_ij_id]
INNER JOIN 
	[abm].[tour_ij_person]
ON 
	[tour_ij].[scenario_id] = [tour_ij_person].[scenario_id]
	AND [tour_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
INNER JOIN 
	[abm].[lu_person]
ON 
	[tour_ij_person].[scenario_id] = [lu_person].[scenario_id]
	AND [tour_ij_person].[lu_person_id] = [lu_person].[lu_person_id]
INNER JOIN 
	[abm].[lu_hh]
ON 
	[lu_person].[scenario_id] = [lu_hh].[scenario_id]
	AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [tour_ij].[scenario_id] = @scenario_id
	AND [tour_ij_person].[scenario_id] = @scenario_id
	AND [lu_person].[scenario_id] = @scenario_id
	AND [lu_hh].[scenario_id] = @scenario_id
	AND ([age] >= 75 OR [race_id] > 1 OR ISNULL([poverty], 99) <= 2 OR [hisp_id] > 1) -- LIM
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_1b]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_1b', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_1b', 'MS_Description', 'project evaluation criteria highway 1b'
GO




-- Create stored procedure for highway project evaluation criteria #4
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_4]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_4]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_4]
	@scenario_id NVARCHAR(5)
	,@project_list NVARCHAR(max) -- One list is fine as links are unique between hwy and transit, shared links have same id

AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Highway 4, Carpool flow on selected links added to transit flow on select links
*/

DECLARE @sql NVARCHAR(max)

SET @sql =
	N'WITH tt AS (
		SELECT 
			[hwycov_id]
			,[nm]
			,[length_mile]
			,SUM(CASE	WHEN [mode_id] BETWEEN 3 AND 5 THEN [hwy_flow_mode].[flow] * 2
						WHEN [mode_id] BETWEEN 6 AND 8 THEN [hwy_flow_mode].[flow] * 3.34
						ELSE 0
						END
					) AS carpool_person_vol
		FROM  
			[abm].[hwy_flow_mode]
		INNER JOIN
			[abm].[hwy_flow]
		ON
			[hwy_flow_mode].[scenario_id] = [hwy_flow].[scenario_id]
			AND [hwy_flow_mode].[hwy_flow_id] = [hwy_flow].[hwy_flow_id]
		INNER JOIN
			[abm].[hwy_link_ab_tod]
		ON
			[hwy_flow].[scenario_id] = [hwy_link_ab_tod].[scenario_id]
			AND [hwy_flow].[hwy_link_ab_tod_id] = [hwy_link_ab_tod].[hwy_link_ab_tod_id]
		INNER JOIN
			[abm].[hwy_link_tod]
		ON
			[hwy_link_ab_tod].[scenario_id] = [hwy_link_tod].[scenario_id]
			AND [hwy_link_ab_tod].[hwy_link_tod_id] = [hwy_link_tod].[hwy_link_tod_id]
		INNER JOIN 
			[abm].[hwy_link]
		ON 
			[hwy_link_tod].[scenario_id] = [hwy_link].[scenario_id]  
			AND [hwy_link_tod].[hwy_link_id]  = [hwy_link].[hwy_link_id]    
		WHERE  
			[hwy_flow_mode].[scenario_id] =  ' + @scenario_id + N'
			AND [hwy_flow].[scenario_id] =  ' + @scenario_id + N' 
			AND [hwy_link_ab_tod].[scenario_id] =  ' + @scenario_id + N' 
			AND [hwy_link_tod].[scenario_id] =  ' + @scenario_id + N' 
			AND [hwy_link].[scenario_id] =  ' + @scenario_id + N' 
			AND [iproj] in (' + @project_list + N')
			AND [ifc] = 1 
			AND [ihov] > 1
		GROUP BY  
			[hwycov_id]
			,[nm]
			,[length_mile]
		)
		SELECT 
			' + @scenario_id + N' AS [scenario_id]
			,2 * SUM(carpool_person_vol * [length_mile]) / sum([length_mile]) AS carpool_person_vol
		FROM 
			tt'
EXECUTE(@sql)
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_4]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_4', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_4', 'MS_Description', 'project evaluation criteria highway 4'
GO




-- Create stored procedure for highway project evaluation criteria #8
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_8]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_8]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_8]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014 by Gregor Schroeder
	Description: Evaluation Criteria Highway 8, Person hours across bike/walk modes and access/egress from transit walk time
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

-- Walk/Bike mode
-- Data exporter was appending wrong skims to walk/bike, java not using input file to find pairs
-- The original fix threw out pairs not in the walk threshold and used distance from MGRATOMGRA
-- Since fixed skim issue so this may not match older numbers
with walk_bike AS (
SELECT 
	SUM(([party_size] * [trip_time]) / 60) / @sample_rate AS walk_bike_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [trip_ij].[mode_id] IN (9,10) -- walk, bike
	),
--walk to transit access/egress time
walk_transit AS (
SELECT
	(SUM(ISNULL([party_size] * CAST(tap1.[time_boarding_actual] AS float) / 60, 0)) +
	SUM(ISNULL([party_size] * CAST(tap2.[time_alighting_actual] AS float) / 60, 0))) / @sample_rate AS walk_transit_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] AS tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] AS tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id]
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 11 AND 15 -- walk to transit
	),
-- knr/pnr egress time 
knr_pnr_egress AS (
SELECT	
	SUM(CASE	WHEN [inbound] = 1 THEN ISNULL([party_size] * CAST(tap1.[time_boarding_actual] AS float) / 60, 0)
				WHEN [inbound] = 0 THEN ISNULL([party_size] * CAST(tap2.[time_alighting_actual] AS float) / 60, 0)
				END
				) / @sample_rate AS knr_pnr_egress_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id]
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 16 AND 25 -- knr and pnr
	)
SELECT
	@scenario_id AS scenario_id
	,ISNULL(walk_bike_time, 0) + ISNULL(walk_transit_time, 0) + ISNULL(knr_pnr_egress_time, 0) AS person_hours
FROM 
	walk_bike, walk_transit, knr_pnr_egress
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_8]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_8', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_8', 'MS_Description', 'project evaluation criteria highway 8'
GO




-- Create stored procedure for highway project evaluation criteria #9a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_9a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_9a]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_9a]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Highway 9A, Destination TAZ employment, K-12 and college enrollment accessible 
													within 30 minutes during AM peak by Drive Alone Non-Toll from
													every origin TAZ weighted by origin population
*/

-- Get sum of destination employment and origin population
-- Only include origins with 
with taz_results AS (
	SELECT 
		[hwy_skims].[scenario_id]
		,MAX(orig.pop) AS pop
		,SUM(dest.emp_enroll) AS total_dest_emp_enroll
	FROM 
		[abm].[hwy_skims]
	INNER JOIN
		[ref].[time_period]
	ON
		[hwy_skims].[time_period_id] = [time_period].[time_period_id]
	INNER JOIN (	
		SELECT	
			[lu_person].[scenario_id]
			,[taz_geography_zone_id]
			,CAST(COUNT(*) AS float) AS pop
		FROM 
			[abm].[lu_person]  -- weighted by Popsyn pop
		INNER JOIN 
			[abm].[lu_hh] -- weighted by Popsyn pop
		ON 
			[lu_person].[scenario_id] = [lu_hh].[scenario_id]
			AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id]
		INNER JOIN 
			[abm].[lu_mgra_input]
		ON 
			[lu_hh].[scenario_id] = [lu_mgra_input].[scenario_id]
			AND [lu_hh].[geography_zone_id] = [lu_mgra_input].[geography_zone_id]
		INNER JOIN
			[ref].[mgra13_xref_taz13]
		ON
			[lu_hh].[geography_zone_id] = [mgra13_xref_taz13].[mgra_geography_zone_id]
		WHERE 
			[lu_person].[scenario_id] = @scenario_id
			AND [lu_hh].[scenario_id] = @scenario_id
			AND [lu_mgra_input].[scenario_id] = @scenario_id
		GROUP BY 
			[lu_person].[scenario_id]
			,[taz_geography_zone_id]
		) orig
		ON 
			[hwy_skims].[scenario_id] = orig.[scenario_id]
			AND [hwy_skims].[orig_geography_zone_id] = orig.[taz_geography_zone_id]
	INNER JOIN (	
		SELECT	
			[scenario_id]
			,[taz_geography_zone_id]
			,SUM([EMP_TOTAL] + [ENROLLGRADEKTO8] + [ENROLLGRADE9TO12] + [COLLEGEENROLL] + [OTHERCOLLEGEENROLL] + [ADULTSCHENRL]) AS emp_enroll
			-- includes adult school and other college enrollment as abm does place university people to those
		FROM 
			[abm].[lu_mgra_input]
		INNER JOIN
			[ref].[mgra13_xref_taz13]
		ON
			[lu_mgra_input].[geography_zone_id] = [mgra13_xref_taz13].[mgra_geography_zone_id]
		WHERE 
			[scenario_id] = @scenario_id
			AND ([emp_total] + [enrollgradekto8] + [enrollgrade9to12] + [collegeenroll] + [othercollegeenroll] + [adultschenrl]) > 0
			-- includes adult school and other college enrollment as abm does place university people to those
		GROUP BY 
			[scenario_id]
			,[taz_geography_zone_id]
		) dest
		ON 
			[hwy_skims].[scenario_id] = dest.[scenario_id]
			AND [hwy_skims].[dest_geography_zone_id] = dest.[taz_geography_zone_id]
	WHERE 
		[hwy_skims].[scenario_id] = @scenario_id
		AND [time_resolution_id] = 1
		AND [time_period_number] = 2
		AND [time_drive_alone_free] > 0 AND [time_drive_alone_free] <= 30
	GROUP BY 
		[hwy_skims].[scenario_id]
		,orig.[taz_geography_zone_id]
	)
SELECT
	@scenario_id AS [scenario_id]
	,SUM(pop * total_dest_emp_enroll) / SUM(pop) AS wgt_avg_job_sch_enroll
FROM
	taz_results
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_9a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_9a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_9a', 'MS_Description', 'project evaluation criteria highway 9a'
GO




-- Create stored procedure for highway project evaluation criteria #10
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_hwy_10]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_hwy_10]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_hwy_10]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Highway 10, Total hours travelled on network by MHD and HHD trucks
*/

SELECT
	@scenario_id AS [scenario_id]
	,SUM(ISNULL([hwy_flow_mode].[flow], 0) * ISNULL([time], 0) / 60) AS hours_travelled
FROM 
	[abm].[hwy_flow_mode] -- operates on the loaded highway network, includes all travel within the model
INNER JOIN
	[abm].[hwy_flow]
ON
	[hwy_flow_mode].[scenario_id] = [hwy_flow].[scenario_id]
	AND [hwy_flow_mode].[hwy_flow_id] = [hwy_flow].[hwy_flow_id]
WHERE 
	[hwy_flow_mode].[scenario_id] = @scenario_id
	AND [hwy_flow].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 34 AND 37
GO

-- Add metadata for [rtp_2015].[sp_eval_hwy_10]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_10', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_hwy_10', 'MS_Description', 'project evaluation criteria highway 10'
GO




-- Create stored procedure for transit evaluation criteria #2
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_2]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_2]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_2]
	@scenario_id smallint
	,@all_models bit = 0
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Transit 2, Total person trips across the walk/pnr/knr transit modes
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

IF @all_models = 0
BEGIN
	SELECT	
		@scenario_id AS scenario_id
		,SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_ij] -- individual and joint models only
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
END
ELSE
BEGIN
	with ap AS (
	SELECT	
		SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_ap]
	WHERE 
		[trip_ap].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
		),
	cb AS (
	SELECT	
		SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_cb]
	WHERE 
		[trip_cb].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
		),
	ie AS (
	SELECT	
		SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_ie]
	WHERE 
		[trip_ie].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
		),
	ij AS (
	SELECT	
		SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_ij]
	WHERE 
		[trip_ij].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
		),
	vis AS (
	SELECT	
		SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
	FROM 
		[abm].[trip_vis]
	WHERE 
		[trip_vis].[scenario_id] = @scenario_id
		AND [mode_id] BETWEEN 11 AND 25 -- just transit modes
		)
	SELECT
		@scenario_id AS scenario_id
		,SUM(ISNULL(ap.person_trips, 0) + ISNULL(cb.person_trips, 0) + ISNULL(ie.person_trips, 0) + ISNULL(ij.person_trips, 0) + ISNULL(vis.person_trips, 0)) AS person_trips
	FROM
		ap, cb, ie, ij, vis
END
GO

-- Add metadata for [rtp_2015].[sp_eval_transit_2]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_2', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_2', 'MS_Description', 'project evaluation criteria transit 2'
GO




-- Create stored procedure for transit evaluation criteria #4
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_4]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_4]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_4]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Transit 4, Total Passenger Mile divided by Total Seat Miles
*/

-- Sum of total transit flow multiplied by length of segment in miles to get passenger miles. 
-- Total seat miles gotten from route frequency for the 3 TOD periods above multiplied by total mileage of the route and the number of seats on the route. 

 --Total Passenger Miles
with pass_miles AS (
SELECT  
       [config] / 1000 AS [route_number]
       ,[transit_route].[transit_mode_id]
       ,SUM([transit_flow] * ([to_mp]-[from_mp])) AS total_passenger_miles
FROM 
       [abm].[transit_flow]
INNER JOIN
       [abm].[transit_route]
ON
       [transit_flow].[scenario_id] = [transit_route].[scenario_id]
       AND [transit_flow].[transit_route_id] = [transit_route].[transit_route_id]
INNER JOIN
       [ref].[time_period]
ON
       [transit_flow].[time_period_id] = [time_period].[time_period_id]
WHERE 
       [transit_flow].[scenario_id] = @scenario_id
       AND [transit_route].[scenario_id] = @scenario_id
       AND [time_resolution_id] = 1
       AND [time_period_number] BETWEEN 2 AND 4 -- day time
GROUP BY
       [config] / 1000
       ,[transit_route].[transit_mode_id]
       ),
       
--if daily, need to get the night hour column added to trrt.bin file.
--seat capacity by route type-------------
--Heavy Rail 130/car (5 car trains)
--Trolley 64/car (3 car trains)
--SPRINTER 136/car (2 car trains) 
--Circulator 29/vehicle        -> mode 10 San Marcos Shuttle, Super loop
--Bus 37/vehicle               -> mode 7, 9, 10 & streetcar (mode 5, route 551-559 + 565)
--Bus Rapid Transit 53/vehicle, Limited Express -> mode 6 and mode 8

--Total Seat Miles
seat_miles AS (
SELECT 
       [config] / 1000 AS [route_number]
       ,[transit_mode_id]
       ,SUM(capacity * (frequency_tod_2 + frequency_tod_3 + frequency_tod_4) * route_mile) AS total_seat_miles
FROM (
       SELECT 
              [config]
              ,[transit_route].[transit_mode_id]
              ,route_mile
              ,ISNULL(180 / NULLIF([am_headway], 0), 0) AS frequency_tod_2
              ,ISNULL(390 / NULLIF([op_headway], 0) ,0) AS frequency_tod_3
              ,ISNULL(210 / NULLIF([pm_headway], 0), 0) AS frequency_tod_4
              ,CASE  WHEN [transit_mode_id] = 4 THEN 130 * 5 --coaster and heavy rail
                           WHEN [transit_mode_id] = 5 AND [config]/1000 > 500 AND [config]/1000 < 551  THEN 64 * 3 --trolley
                           WHEN [transit_mode_id] = 5 AND [config]/1000 = 399 THEN 136 * 2 --sprinter
                           WHEN [transit_mode_id] = 5 AND (([config] / 1000 >= 551 AND [config] / 1000 <= 559) OR [config] / 1000 = 565) THEN 37 --streetcar
                           WHEN [transit_mode_id] = 6 OR [transit_mode_id] = 8 THEN 53 --BRT 
                           WHEN [transit_mode_id] IN (7, 9, 10) THEN 37 --bus
                           ELSE 0
                           END AS capacity
       FROM 
              [abm].[transit_route]
       INNER JOIN (
              SELECT 
                     [scenario_id]
                     ,[transit_route_id]
                     ,MAX([to_mp]) as route_mile  --route mile
              FROM 
                     [abm].[transit_flow]
              WHERE 
                     [scenario_id] = @scenario_id
              GROUP BY 
                     [scenario_id]
                     ,[transit_route_id]
                     ) rte_mile 
       ON 
              [transit_route].[scenario_id] = rte_mile.[scenario_id]
              AND [transit_route].[transit_route_id] = rte_mile.[transit_route_id]
       WHERE 
              [transit_route].[scenario_id] = @scenario_id
       ) AS tt
GROUP BY
       [config] / 1000
       ,[transit_mode_id]
)
SELECT 
       @scenario_id AS [scenario_id]
       ,pass_miles.[route_number]
       ,pass_miles.[transit_mode_id]
       ,total_passenger_miles
       ,total_seat_miles
       ,ISNULL(total_passenger_miles / NULLIF(total_seat_miles, 0), 0) AS passenger_over_seat_miles
FROM 
       pass_miles
INNER JOIN
       seat_miles
ON
       pass_miles.[route_number] = seat_miles.[route_number]
       AND pass_miles.[transit_mode_id] = seat_miles.[transit_mode_id]

UNION ALL

SELECT 
       @scenario_id AS [scenario_id]
       ,NULL
       ,NULL
       ,SUM(total_passenger_miles) AS total_passenger_miles
       ,SUM(total_seat_miles) AS total_passenger_miles
       ,ISNULL(SUM(total_passenger_miles) / NULLIF(SUM(total_seat_miles), 0), 0) AS passenger_over_seat_miles
FROM 
       pass_miles
INNER JOIN
       seat_miles
ON
       pass_miles.[route_number] = seat_miles.[route_number]
       AND pass_miles.[transit_mode_id] = seat_miles.[transit_mode_id]
ORDER BY
       [route_number]
       ,[transit_mode_id]

GO

-- Add metadata for [rtp_2015].[sp_eval_transit_4]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_4', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_4', 'MS_Description', 'project evaluation criteria transit 4'
GO




-- Create stored procedure for transit evaluation criteria #7
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_7]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_7]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_7]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Transit 7, See Highway 8
*/

EXECUTE [rtp_2015].[sp_eval_hwy_8] @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_eval_transit_7]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_7', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_7', 'MS_Description', 'project evaluation criteria transit 7'
GO




-- Create stored procedure for transit evaluation criteria #8a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_8a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_8a]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_8a]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Transit 8A, Total person trips across the walk/pnr/knr transit modes and the work, school, university purposes
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

SELECT
	@scenario_id AS scenario_id
	,COUNT(*) / @sample_rate AS person_trips -- works because individual model only
FROM (
	SELECT
		[trip_ij].[scenario_id] 
		,[trip_ij].[tour_ij_id]
		,[inbound]
	FROM 
		[abm].[trip_ij]
	INNER JOIN 
		[abm].[tour_ij]
	ON
		[trip_ij].[scenario_id] = [tour_ij].[scenario_id]
		AND [trip_ij].[tour_ij_id] = [tour_ij].[tour_ij_id]
	WHERE
		[trip_ij].[scenario_id] = @scenario_id
		AND [tour_ij].[scenario_id] = @scenario_id
		AND [tour_ij].[model_type_id] = 0 -- Only individual model has requisite purposes
		AND [tour_ij].[tour_cat_id] = 0 -- Mandatory tours (work and school)
		AND [trip_ij].[mode_id] BETWEEN 11 AND 25 -- Only want transit trips
		AND [tour_ij].[mode_id] BETWEEN 11 AND 25 -- Only want transit tours
	GROUP BY
		[trip_ij].[scenario_id] 
		,[trip_ij].[tour_ij_id]
		,[inbound]
	) tt
GO

-- Add metadata for [rtp_2015].[sp_eval_transit_8a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8a', 'MS_Description', 'project evaluation criteria transit 8a'
GO




-- Create stored procedure for transit evaluation criteria #8c
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_8c]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_8c]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_8c]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/8/2014
	Description: Evaluation Criteria Transit 8C, Total LIM person trips across the walk/pnr/knr transit modes and the work, school, university purposes
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

SELECT
	@scenario_id AS scenario_id
	,COUNT(*) / @sample_rate AS coc_person_trips -- works because individual model only
FROM (
	SELECT 
		[trip_ij].[scenario_id]
		,[trip_ij].[tour_ij_id]
		,[inbound]
	FROM 
		[abm].[trip_ij]
	INNER JOIN 
		[abm].[tour_ij]
	ON
		[trip_ij].[scenario_id] = [tour_ij].[scenario_id]
		AND [trip_ij].[tour_ij_id] = [tour_ij].[tour_ij_id]
	INNER JOIN 
		[abm].[tour_ij_person]
	ON 
		[trip_ij].[scenario_id] = [tour_ij_person].[scenario_id]
		AND [trip_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
	INNER JOIN 
		[abm].[lu_person]
	ON 
		[tour_ij_person].[scenario_id] = [lu_person].[scenario_id]
		AND [tour_ij_person].[lu_person_id] = [lu_person].[lu_person_id]
	INNER JOIN 
		[abm].[lu_hh]
	ON 
		[lu_person].[scenario_id] = [lu_hh].[scenario_id]
		AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id]
	WHERE
		[trip_ij].[scenario_id] = @scenario_id
		AND [tour_ij].[scenario_id] = @scenario_id
		AND [tour_ij_person].[scenario_id] = @scenario_id
		AND [lu_person].[scenario_id] = @scenario_id
		AND [lu_hh].[scenario_id] = @scenario_id
		AND [tour_ij].[model_type_id] = 0 -- Only individual model has requisite purposes
		AND [tour_ij].[tour_cat_id] = 0 -- Mandatory tours (work and school)
		AND [trip_ij].[mode_id] BETWEEN 11 AND 25 -- Only want transit trips
		AND [tour_ij].[mode_id] BETWEEN 11 AND 25 -- Only want transit tours
		AND ([age] >= 75 OR ISNULL([race_id], 0) > 1 OR ISNULL([poverty], 99) <= 2 OR ISNULL([hisp_id], 0) > 1) -- LIM
	GROUP BY
		[trip_ij].[scenario_id]
		,[trip_ij].[tour_ij_id]
		,[inbound]
	) tt
GO

-- Add metadata for [rtp_2015].[sp_eval_transit_8c]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8c', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8c', 'MS_Description', 'project evaluation criteria transit 8c'
GO




-- Create stored procedure for transit evaluation criteria #8e
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_eval_transit_8e]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_eval_transit_8e]
GO

CREATE PROCEDURE [rtp_2015].[sp_eval_transit_8e]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/10/2014
	Description: Evaluation Criteria Transit 8E, Total person trips across the walk/pnr/knr transit modes with a destination MGRA whose centroid lies within an indian reservation
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

with xref AS (
	SELECT  
		[mgra]
		FROM 
			OPENQUERY(
			[pila\sdgintdb],	'SELECT 
									[mgra] 
								FROM 
									[lis].[gis].[INDIANRES],[lis].[gis].[MGRA13PT]
								WHERE 
									[INDIANRES].[Shape].STContains([MGRA13PT].[Shape]) = 1'
									)
			)
SELECT 
	@scenario_id AS scenario_id
	,SUM(ISNULL([party_size], 0)) / @sample_rate AS person_trips
FROM 
	[abm].[trip_ij] -- individual and joint models only
INNER JOIN
	[ref].[geography_zone] AS orig_geography
ON
	[trip_ij].[orig_geography_zone_id] = orig_geography.[geography_zone_id]
INNER JOIN
	[ref].[geography_zone] AS dest_geography
ON
	[trip_ij].[dest_geography_zone_id] = dest_geography.[geography_zone_id]
LEFT OUTER JOIN 
	xref x1 
ON 
	orig_geography.[zone] = x1.[mgra]
LEFT OUTER JOIN 
	xref x2 
ON 
	dest_geography.[zone] = x2.[mgra]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND orig_geography.[geography_type_id] = 90 -- assumes trip_ij uses mgra13
	AND dest_geography.[geography_type_id] = 90 -- assumes trip_ij uses mgra13
	AND (x1.[mgra] > 0 OR x2.[mgra] > 0)
	AND [mode_id] BETWEEN 11 AND 25 -- transit mode only
GO

-- Add metadata for [rtp_2015].[sp_eval_transit_8e]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8e', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_eval_transit_8e', 'MS_Description', 'project evaluation criteria transit 8e'
GO




-- Create stored procedure for performance metric #1a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_1a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_1a]
GO

CREATE procedure [rtp_2015].[sp_pm_1a]  
    @scenario_id smallint 
    ,@inbound tinyint
    ,@low_income bit = 0
	,@minority bit = 0
	,@senior bit = 0
	,@coc bit = 0
AS

/*	Author: Ziying Ouyang, Yang Wang, and Gregor Schroeder
	Date: Revised 9/22/2014
	Description: PM 1a, 
*/

IF (SELECT COUNT(NULLIF(@low_income, 0)) + COUNT(NULLIF(@minority, 0)) + COUNT(NULLIF(@senior, 0)) +
	COUNT(NULLIF(@coc, 0))) > 1
BEGIN
	print 'WARNING: Please choose only one household segmentation'
	RETURN
END

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

IF @low_income = 1
BEGIN
SELECT
	@scenario_id AS scenario_id
	,CASE WHEN [low_inc] IS NULL THEN 'total' ELSE CAST([low_inc] AS nvarchar) END AS [low_inc]
	,CASE WHEN [mode_category] IS NULL THEN 'total' ELSE [mode_category] END AS [mode_category]
	,avg_tour_time
	,avg_tour_distance
	,avg_tour_trips
	,tours / @sample_rate AS tours
	,ISNULL(CAST(tours AS float) / NULLIF(SUM(CASE WHEN [mode_category] IS NOT NULL AND [low_inc] IS NOT NULL THEN tours ELSE 0 END) OVER (PARTITION BY [low_inc]), 0), 1) AS mode_share
FROM (
	SELECT
		[low_inc]
		,[mode_category]	
		,AVG(tour_time) AS avg_tour_time
		,AVG(tour_distance) AS avg_tour_distance
		,AVG(CAST(tour_trips AS float)) AS avg_tour_trips
		,COUNT(*) AS tours
	FROM (
		SELECT 
			[tour_ij_id]
			,[low_inc]
			,[mode_category]
			,SUM([trip_time]) as tour_time
			,SUM([trip_distance]) as tour_distance 
			,COUNT(*) as tour_trips
		FROM  
			[rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id, @inbound, 1)
		GROUP BY
			[tour_ij_id]
			,[mode_category]
			,[low_inc]
		) tt1
	GROUP BY
		[low_inc]
		,[mode_category]
	WITH ROLLUP
	) tt2
ORDER BY
	[low_inc]
	,[mode_category]

OPTION(RECOMPILE)
END

ELSE IF @minority = 1
BEGIN
SELECT
	@scenario_id AS scenario_id
	,CASE WHEN [minority] IS NULL THEN 'total' ELSE CAST([minority] AS nvarchar) END AS [minority]
	,CASE WHEN [mode_category] IS NULL THEN 'total' ELSE [mode_category] END AS [mode_category]
	,avg_tour_time
	,avg_tour_distance
	,avg_tour_trips
	,tours / @sample_rate AS tours
	,ISNULL(CAST(tours AS float) / NULLIF(SUM(CASE WHEN [mode_category] IS NOT NULL AND [minority] IS NOT NULL THEN tours ELSE 0 END) OVER (PARTITION BY [minority]), 0), 1) AS mode_share
FROM (
	SELECT
		[minority]
		,[mode_category]	
		,AVG(tour_time) AS avg_tour_time
		,AVG(tour_distance) AS avg_tour_distance
		,AVG(CAST(tour_trips AS float)) AS avg_tour_trips
		,COUNT(*) AS tours
	FROM (
		SELECT 
			[tour_ij_id]
			,[minority]
			,[mode_category]
			,SUM([trip_time]) as tour_time
			,SUM([trip_distance]) as tour_distance 
			,COUNT(*) as tour_trips
		FROM  
			[rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id, @inbound, 1)
		GROUP BY
			[tour_ij_id]
			,[mode_category]
			,[minority]
		) tt1
	GROUP BY
		[minority]
		,[mode_category]
	WITH ROLLUP
	) tt2
ORDER BY
	[minority]
	,[mode_category]

OPTION(RECOMPILE)
END

ELSE IF @senior = 1
BEGIN
SELECT
	@scenario_id AS scenario_id
	,CASE WHEN [senior] IS NULL THEN 'total' ELSE CAST([senior] AS nvarchar) END AS [senior]
	,CASE WHEN [mode_category] IS NULL THEN 'total' ELSE [mode_category] END AS [mode_category]
	,avg_tour_time
	,avg_tour_distance
	,avg_tour_trips
	,tours / @sample_rate AS tours
	,ISNULL(CAST(tours AS float) / NULLIF(SUM(CASE WHEN [mode_category] IS NOT NULL AND [senior] IS NOT NULL THEN tours ELSE 0 END) OVER (PARTITION BY [senior]), 0), 1) AS mode_share
FROM (
	SELECT
		[senior]
		,[mode_category]	
		,AVG(tour_time) AS avg_tour_time
		,AVG(tour_distance) AS avg_tour_distance
		,AVG(CAST(tour_trips AS float)) AS avg_tour_trips
		,COUNT(*) AS tours
	FROM (
		SELECT 
			[tour_ij_id]
			,[senior]
			,[mode_category]
			,SUM([trip_time]) as tour_time
			,SUM([trip_distance]) as tour_distance 
			,COUNT(*) as tour_trips
		FROM  
			[rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id, @inbound, 1)
		GROUP BY
			[tour_ij_id]
			,[mode_category]
			,[senior]
		) tt1
	GROUP BY
		[senior]
		,[mode_category]
	WITH ROLLUP
	) tt2
ORDER BY
	[senior]
	,[mode_category]

OPTION(RECOMPILE)
END

ELSE IF @coc = 1
BEGIN
SELECT
	@scenario_id AS scenario_id
	,CASE WHEN [coc] IS NULL THEN 'total' ELSE CAST([coc] AS nvarchar) END AS [coc]
	,CASE WHEN [mode_category] IS NULL THEN 'total' ELSE [mode_category] END AS [mode_category]
	,avg_tour_time
	,avg_tour_distance
	,avg_tour_trips
	,tours / @sample_rate AS tours
	,ISNULL(CAST(tours AS float) / NULLIF(SUM(CASE WHEN [mode_category] IS NOT NULL AND [coc] IS NOT NULL THEN tours ELSE 0 END) OVER (PARTITION BY [coc]), 0), 1) AS mode_share
FROM (
	SELECT
		[coc]
		,[mode_category]	
		,AVG(tour_time) AS avg_tour_time
		,AVG(tour_distance) AS avg_tour_distance
		,AVG(CAST(tour_trips AS float)) AS avg_tour_trips
		,COUNT(*) AS tours
	FROM (
		SELECT 
			[tour_ij_id]
			,CASE	WHEN low_inc = 1 OR minority = 1 OR senior = 1 THEN 1
					ELSE 0
					END AS coc
			,[mode_category]
			,SUM([trip_time]) as tour_time
			,SUM([trip_distance]) as tour_distance 
			,COUNT(*) as tour_trips
		FROM  
			[rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id, @inbound, 1)
		GROUP BY
			[tour_ij_id]
			,[mode_category]
			,CASE	WHEN low_inc = 1 OR minority = 1 OR senior = 1 THEN 1
					ELSE 0
					END
		) tt1
	GROUP BY
		[coc]
		,[mode_category]
	WITH ROLLUP
	) tt2
ORDER BY
	[coc]
	,[mode_category]

OPTION(RECOMPILE)
END

ELSE
BEGIN 
SELECT
	@scenario_id AS scenario_id
	,CASE WHEN [mode_category] IS NULL THEN 'total' ELSE [mode_category] END AS [mode_category]
	,avg_tour_time
	,avg_tour_distance
	,avg_tour_trips
	,tours / @sample_rate AS tours
	,CAST(tours AS float) / SUM(CASE WHEN [mode_category] IS NOT NULL THEN tours ELSE 0 END) OVER () AS mode_share
FROM (
	SELECT
		[mode_category]
		,AVG(tour_time) AS avg_tour_time
		,AVG(tour_distance) AS avg_tour_distance
		,AVG(CAST(tour_trips AS float)) AS avg_tour_trips
		,COUNT(*) AS tours
	FROM (
		SELECT 
			[tour_ij_id]
			,[mode_category]
			,SUM([trip_time]) as tour_time
			,SUM([trip_distance]) as tour_distance 
			,COUNT(*) as tour_trips
		FROM  
			[rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id, @inbound, 1)
		GROUP BY
			[tour_ij_id]
			,[mode_category]
		) tt1
	GROUP BY
		[mode_category]
	WITH ROLLUP
	) tt2
ORDER BY
	[mode_category]

OPTION(RECOMPILE)
END
GO

-- Add metadata for [rtp_2015].[sp_pm_1a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_1a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_1a', 'MS_Description', 'performance metric 1a'
GO




-- Create stored procedure for performance metric #1b
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_1b]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_1b]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_1b]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/11/2014
	Description: Performance Measure 1B, Vehicle Delay per Capita
											Sum of link level vehicle flows multiplied by difference 
											between congested and free flow travel time and then divided
											by total synthetic population
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

/* Get total synthetic population */
DECLARE @pop float
SET @pop = (SELECT 
				COUNT(*) / @sample_rate
			FROM 
				[abm].[lu_person] 
			WHERE 
				[lu_person].[scenario_id] = @scenario_id
			)

SELECT 
	[hwy_flow].[scenario_id]
	,SUM(([time] - ([tm] + [tx])) * [flow])/@pop AS veh_delay_per_capita
FROM 
	[abm].[hwy_flow]
INNER JOIN 
	[abm].[hwy_link_ab_tod]
ON 
	[hwy_flow].[scenario_id] = [hwy_link_ab_tod].[scenario_id]
	AND [hwy_flow].[hwy_link_ab_tod_id] = [hwy_link_ab_tod].[hwy_link_ab_tod_id]
WHERE 
	[hwy_flow].[scenario_id] = @scenario_id
	AND [hwy_link_ab_tod].[scenario_id] = @scenario_id
	AND [time] - ([tm] + [tx]) >= 0
GROUP BY
	[hwy_flow].[scenario_id]
GO

-- Add metadata for [rtp_2015].[sp_pm_1b]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_1b', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_1b', 'MS_Description', 'performance metric 1b'
GO




-- Create stored procedure for performance metric #2a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_2a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_2a]
GO

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/17/2014
	Description: Performance Measure 2A, Increase in walk, bike, transit, carpool mode share
										Total walk, bike, transit, and carpool person trips over total person trips
										includes all micro-simulated models
*/

CREATE PROCEDURE [rtp_2015].[sp_pm_2a]
	@scenario_id smallint
	,@peak_period bit = 0
AS

DECLARE @simulTrips float

-- peak period assumes all micro simulated trips are operating on abm half hour period
IF @peak_period = 1
BEGIN
/* Get total number of micro-simulated person trips */
SET @simulTrips = (SELECT 
						SUM(ISNULL([party_size], 0))
					FROM 
						[abm].[vi_trip_micro_simul] 
					INNER JOIN
						[ref].[time_period]
					ON
						[vi_trip_micro_simul].[time_period_id] = [time_period].[time_period_id]
					WHERE 
						[vi_trip_micro_simul].[scenario_id] = @scenario_id 
						AND [mode_id] > 0 -- no airport passing through trips
						AND [time_resolution_id] = 2 -- abm half hour period
						AND ([time_period_number] BETWEEN 4 AND 9 -- AM peak period
							OR [time_period_number] BETWEEN 23 AND 29) -- PM peak period
					)

SELECT
	@scenario_id AS [scenario_id]
	,SUM(CASE WHEN [mode_id] BETWEEN 1 AND 2 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS drive_alone
	,SUM(CASE WHEN [mode_id] BETWEEN 3 AND 8 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS sr
	,SUM(CASE WHEN [mode_id] BETWEEN 11 AND 25 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS transit
	,SUM(CASE WHEN [mode_id] = 9 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS walk
	,SUM(CASE WHEN [mode_id] = 10 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS bike
	,SUM(CASE WHEN [mode_id] BETWEEN 26 AND 27 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS bus_taxi
FROM
	[abm].[vi_trip_micro_simul] -- view
INNER JOIN
	[ref].[time_period]
ON
	[vi_trip_micro_simul].[time_period_id] = [time_period].[time_period_id]
WHERE
	[vi_trip_micro_simul].[scenario_id] = @scenario_id 
	AND [mode_id] > 0 -- no airport passing through trips
	AND [time_resolution_id] = 2 -- abm half hour period
	AND ([time_period_number] BETWEEN 4 AND 9 -- AM peak period
	OR [time_period_number] BETWEEN 23 AND 29) -- PM peak period
END

ELSE
BEGIN
/* Get total number of micro-simulated person trips */
SET @simulTrips = (SELECT 
						SUM(ISNULL([party_size], 0)) 
					FROM 
						[abm].[vi_trip_micro_simul] 
					INNER JOIN
						[ref].[scenario]
					ON
						[vi_trip_micro_simul].[scenario_id] = [scenario].[scenario_id]
					WHERE 
						[vi_trip_micro_simul].[scenario_id] = @scenario_id 
						AND [mode_id] > 0) -- no airport passing through trips

SELECT
	@scenario_id AS [scenario_id]
	,SUM(CASE WHEN [mode_id] BETWEEN 1 AND 2 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS drive_alone
	,SUM(CASE WHEN [mode_id] BETWEEN 3 AND 8 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS sr
	,SUM(CASE WHEN [mode_id] BETWEEN 11 AND 25 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS transit
	,SUM(CASE WHEN [mode_id] = 9 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS walk
	,SUM(CASE WHEN [mode_id] = 10 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS bike
	,SUM(CASE WHEN [mode_id] BETWEEN 26 AND 27 THEN ISNULL([party_size], 0) ELSE 0 END) / @simulTrips AS bus_taxi
FROM
	[abm].[vi_trip_micro_simul] -- view
WHERE
	[vi_trip_micro_simul].[scenario_id] = @scenario_id 
	AND [mode_id] > 0 -- no airport passing through trips
END
GO

-- Add metadata for [rtp_2015].[sp_pm_2a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_2a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_2a', 'MS_Description', 'performance metric 2a'
GO




-- Create stored procedure for performance metric #4b
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_4b]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_4b]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_4b]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/12/2014
	Description: Performance Measure 4B, Average truck/commercial vehicle travel time to and around regioanal gateways/distribution hubs (minutes)
										
*/

SELECT 
	@scenario_id AS [scenario_id]
	,SUM([trips] * [time_drive_alone_free]) / SUM([trips]) AS avg_trip_time -- just using drive alone fee, no tolls or truck
	,SUM([trips]) AS trips
FROM
	[abm].[trip_agg]
INNER JOIN
	[abm].[hwy_skims]
ON
	[trip_agg].[scenario_id] = [hwy_skims].[scenario_id] 
	AND [trip_agg].[orig_geography_zone_id] = [hwy_skims].[orig_geography_zone_id] 
	AND [trip_agg].[dest_geography_zone_id] = [hwy_skims].[dest_geography_zone_id] 
	AND [trip_agg].[time_period_id] = [hwy_skims].[time_period_id]
WHERE
	[trip_agg].[scenario_id] = @scenario_id
	AND [hwy_skims].[scenario_id] = @scenario_id
	AND [model_type_id] IN (6,9) -- truck and commercial vehicle models
	-- filter on taz13 freight_distribute_hub, assumes TAZ geography
	AND ([hwy_skims].[orig_geography_zone_id] IN (select [geography_zone_id] FROM [rtp_2015].[taz13_freight_distribute_hub]) OR [hwy_skims].[orig_geography_zone_id] BETWEEN 1 AND 12
		OR [hwy_skims].[dest_geography_zone_id] IN (select [geography_zone_id] FROM [rtp_2015].[taz13_freight_distribute_hub]) OR [hwy_skims].[dest_geography_zone_id] BETWEEN 1 AND 12)
GO

-- Add metadata for [rtp_2015].[sp_pm_4b]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_4b', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_4b', 'MS_Description', 'performance metric 4b'
GO




-- Create stored procedure for performance metric #5a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_5a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_5a]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_5a]
	@scenario_id smallint
	,@ao_cost real
	,@low_income bit = 0
	,@minority bit = 0
	,@senior bit = 0
	,@coc bit = 0
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/19/2014 by Gregor Schroeder
	Description: Performance Measure 5A, percent of income consumed by transportation costs (COC/non COC pop)
*/

IF (SELECT COUNT(NULLIF(@low_income, 0)) + COUNT(NULLIF(@minority, 0)) + COUNT(NULLIF(@senior, 0)) +
	COUNT(NULLIF(@coc, 0))) > 1
BEGIN
	print 'WARNING: Please choose only one household segmentation'
	RETURN
END

IF @low_income = 1
BEGIN
SELECT 
	scenario_id
	,p_lowinc
	,AVG(transp_pct_cost) AS transp_pct_cost
FROM  
	[rtp_2015].[fn_pm_5a_coc_transp_cost](@scenario_id,@ao_cost)
GROUP BY
	scenario_id
	,p_lowinc
END

ELSE IF @minority = 1
BEGIN
SELECT 
	scenario_id
	,p_minority
	,AVG(transp_pct_cost) AS transp_pct_cost
FROM  
	[rtp_2015].[fn_pm_5a_coc_transp_cost](@scenario_id,@ao_cost)
GROUP BY
	scenario_id
	,p_minority
END

ELSE IF @senior = 1
BEGIN
SELECT 
	scenario_id
	,p_senior
	,AVG(transp_pct_cost) AS transp_pct_cost
FROM  
	[rtp_2015].[fn_pm_5a_coc_transp_cost](@scenario_id,@ao_cost)
GROUP BY
	scenario_id
	,p_senior
END

ELSE IF @coc = 1
BEGIN
SELECT 
	scenario_id
	,coc_pop
	,AVG(transp_pct_cost) AS transp_pct_cost
FROM  
	[rtp_2015].[fn_pm_5a_coc_transp_cost](@scenario_id,@ao_cost)
GROUP BY
	scenario_id
	,coc_pop
END

ELSE
BEGIN
SELECT 
	scenario_id
	,AVG(transp_pct_cost) AS transp_pct_cost
FROM  
	[rtp_2015].[fn_pm_5a_coc_transp_cost](@scenario_id,@ao_cost)
GROUP BY
	scenario_id
END
GO

-- Add metadata for [rtp_2015].[sp_pm_5a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_5a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_5a', 'MS_Description', 'performance metric 5a'
GO




-- Create stored procedure for performance metric #6a
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_6a]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_6a]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_6a]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/12/2014
	Description: Performance Measure 6A, Average travel time to/from tribal lands
											Average travel time of micro-simulated trips with origin or destination MGRAs whose centroids are within the tribal lands
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

-- assumes micro-simulated trips are mgra series 13 based
with xref AS (
	SELECT  
		[mgra]
	FROM 
		OPENQUERY(
			[pila\sdgintdb],'SELECT [mgra] FROM [lis].[gis].[INDIANRES], [lis].[gis].[MGRA13PT]
								WHERE [INDIANRES].[Shape].STContains([MGRA13PT].[Shape]) = 1')
)
SELECT 
	@scenario_id AS [scenario_id]
	,SUM(ISNULL([party_size] * [trip_time], 0)) / SUM(ISNULL([party_size], 0)) AS avg_travel_time
	,SUM(ISNULL([party_size], 0)) / @sample_rate AS trips
FROM 
	[abm].[vi_trip_micro_simul] -- all micro-simulated trips view
INNER JOIN
	[ref].[geography_zone] AS orig_geography
ON
	[vi_trip_micro_simul].[orig_geography_zone_id] = orig_geography.[geography_zone_id]
INNER JOIN
	[ref].[geography_zone] AS dest_geography
ON
	[vi_trip_micro_simul].[dest_geography_zone_id] = dest_geography.[geography_zone_id]
LEFT OUTER JOIN 
	xref x1 
ON 
	orig_geography.[zone] = x1.[mgra]
LEFT OUTER JOIN 
	xref x2 
ON 
	dest_geography.[zone] = x2.[mgra]
WHERE 
	[vi_trip_micro_simul].[scenario_id] = @scenario_id
	AND orig_geography.[geography_type_id] = 90 -- assumes vi_trip_micro_simul uses mgra13
	AND dest_geography.[geography_type_id] = 90 -- assumes vi_trip_micro_simul uses mgra13
	AND (x1.[mgra] > 0 OR x2.[mgra] > 0)
GO

-- Add metadata for [rtp_2015].[sp_pm_6a]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6a', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6a', 'MS_Description', 'performance metric 6a'
GO




-- Create stored procedure for performance metric #6b
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_6b]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_6b]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_6b]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/12/2014
	Description: Performance Measure 6B, Average travel time to/from Mexico
											Average travel time of micro-simulated trips with origin or destination MGRAs correspond to the POE of external zone 1 to 5
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id)

-- assumes cross border model O-Ds are series 13 mgra
-- a trip from one poe mgra to another would take the origin poe as the poe of note
SELECT
	@scenario_id AS [scenario_id]
	,ISNULL(CASE	WHEN orig_poe.[poe_desc] IS NULL THEN dest_poe.[poe_desc]
					ELSE orig_poe.[poe_desc]
					END, 'total') AS [poe_desc]
	,SUM([party_size] * [trip_time]) / SUM([party_size]) AS avg_travel_time
	,SUM([party_size]) / @sample_rate AS trips
FROM
	[abm].[trip_cb] -- just cross border model trips
LEFT OUTER JOIN
	[ref].[poe] orig_poe
ON
	[trip_cb].[orig_geography_zone_id] = orig_poe.[mgra_entry_geography_zone_id]
LEFT OUTER JOIN
	[ref].[poe] dest_poe
ON
	[trip_cb].[dest_geography_zone_id] = dest_poe.[mgra_return_geography_zone_id]
WHERE
	[trip_cb].[scenario_id] = @scenario_id
	AND ([trip_cb].[orig_geography_zone_id] IN (SELECT [mgra_entry_geography_zone_id] FROM [ref].[poe])
		OR [trip_cb].[dest_geography_zone_id] IN (SELECT [mgra_return_geography_zone_id] FROM [ref].[poe]))
GROUP BY
	CASE	WHEN orig_poe.[poe_desc] IS NULL THEN dest_poe.[poe_desc]
			ELSE orig_poe.[poe_desc]
			END
WITH ROLLUP
GO

-- Add metadata for [rtp_2015].[sp_pm_6b]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6b', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6b', 'MS_Description', 'performance metric 6b'
GO




-- Create stored procedure for performance metric #6c
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_6c]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_6c]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_6c]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang 
	Date: Revised 9/15/2014
	Description: Performance Measure 6C, Average travel time to/from neighboring counties(Imperial, Orange, Riverside) (minutes)
		         only for auto travel, no transit
*/

-- Only looks at drive alone free skim, no tolls or anything else
SELECT
	@scenario_id AS [scenario_id]
	,SUM([trips] * [time_drive_alone_free]) / SUM([trips]) AS avg_travel_time
	,SUM([trips]) AS trips
FROM
	[abm].[trip_agg]
INNER JOIN
	[ref].[geography_zone] AS orig_geography
ON
	[trip_agg].[orig_geography_zone_id] = orig_geography.[geography_zone_id]
INNER JOIN
	[ref].[geography_zone] AS dest_geography
ON
	[trip_agg].[dest_geography_zone_id] = dest_geography.[geography_zone_id]
INNER JOIN
	[abm].[hwy_skims]
ON
	[trip_agg].[scenario_id] = [hwy_skims].[scenario_id]
	AND [trip_agg].[orig_geography_zone_id] = [hwy_skims].[orig_geography_zone_id]
	AND [trip_agg].[dest_geography_zone_id] = [hwy_skims].[dest_geography_zone_id]
	AND [trip_agg].[time_period_id] = [hwy_skims].[time_period_id]
WHERE
	[trip_agg].[scenario_id] = @scenario_id
	AND [hwy_skims].[scenario_id] = @scenario_id
	AND [model_type_id] BETWEEN 6 AND 9
	-- ASSUMES trip_agg is TAZ BASED
	AND orig_geography.[geography_type_id] = 34
	AND dest_geography.[geography_type_id] = 34
	AND (orig_geography.[zone] BETWEEN 6 AND 12
		OR dest_geography.[zone] BETWEEN 6 AND 12)
GO

-- Add metadata for [rtp_2015].[sp_pm_6c]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6c', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6c', 'MS_Description', 'performance metric 6c'
GO




-- Create stored procedure for performance metric #6d
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_6d]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_6d]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_6d]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang 
	Date: Revised 9/15/2014
	Description: Performance Measure 6d, Average travel time to/from military bases/installations
											Average travel time of micro-simulated trips with origin or destination MGRAs whose centroids are within the military bases/installations
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

with xref AS (
	SELECT  
		[mgra]
	FROM 
		OPENQUERY(
		
		
			[pila\sdgintdb],'SELECT [mgra]
			FROM [lis].[gis].[OWNERSHIP], [lis].[gis].[MGRA13PT]
			WHERE [Own] = 41  AND
			[OWNERSHIP].[Shape].STContains([MGRA13PT].[Shape]) = 1'			
			)
)
SELECT 
	@scenario_id AS [scenario_id]
	,SUM(ISNULL([party_size] * [trip_time], 0)) / SUM(ISNULL([party_size], 0)) AS avg_travel_time
	,SUM(ISNULL([party_size], 0)) / @sample_rate AS trips
FROM 
	[abm].[vi_trip_micro_simul] -- view
INNER JOIN
	[ref].[geography_zone] AS orig_geography
ON
	[vi_trip_micro_simul].[orig_geography_zone_id] = orig_geography.[geography_zone_id]
INNER JOIN
	[ref].[geography_zone] AS dest_geography
ON
	[vi_trip_micro_simul].[dest_geography_zone_id] = dest_geography.[geography_zone_id]
LEFT OUTER JOIN 
	xref x1 
ON 
	orig_geography.[zone] = x1.[mgra]
LEFT OUTER JOIN 
	xref x2 
ON 
	dest_geography.[zone] = x2.[mgra]
WHERE 
	[vi_trip_micro_simul].[scenario_id] = @scenario_id
	AND orig_geography.[geography_type_id] = 90 -- assumes vi_trip_micro_simul uses mgra13
	AND dest_geography.[geography_type_id] = 90 -- assumes vi_trip_micro_simul uses mgra13
	AND (x1.[mgra] > 0 OR x2.[mgra] > 0)
GO

-- Add metadata for [rtp_2015].[sp_pm_6d]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6d', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_6d', 'MS_Description', 'performance metric 6d'
GO




-- Create stored procedure for performance metric #7d
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_7d]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_7d]
GO

CREATE procedure [rtp_2015].[sp_pm_7d]  
    @scenario_id int, 
    @inbound tinyint 
AS

/*	Author: Ziying Ouyang, Yang Wang, and Gregor Schroeder
	Date: Revised 9/10/2014
	Description: PM 7d, 
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

SELECT 
      @scenario_id AS [scenario_id]
      ,ISNULL([mode_category], 'total') AS [mode_category]
      ,AVG([tour_distance]) as avg_tour_distance 
      ,COUNT(*) / @sample_rate as trips

FROM (
      SELECT 
            [tour_ij_id]
            ,[mode_category]
            ,SUM([trip_time]) as tour_time
            ,SUM([trip_distance]) as tour_distance 
            ,COUNT(*) as tour_trips
      FROM  
            [rtp_2015].[fn_pm_1a_7d_work_trips](@scenario_id,@inbound,0)
      GROUP BY
            [tour_ij_id]
            ,[mode_category]
      ) tt

GROUP BY
      [mode_category]
WITH ROLLUP



GO

-- Add metadata for [rtp_2015].[sp_pm_7d]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7d', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7d', 'MS_Description', 'performance metric 7d'
GO




-- Create stored procedure for performance metric #7e
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_7e]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_7e]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_7e]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/15/2014 by Gregor Schroeder
	Description: Performance Measure 7E, Person trip time in hours across bike/walk modes and access/egress walk time in hours across walk/pnr/knr transit modes
											Same as project evaluation criteria HWY 8 but in minutes instead of hours, change made in final select
*/

-- scenario sample rate
DECLARE @sample_rate decimal(6,4)
SET @sample_rate = (SELECT [sample_rate] FROM [ref].[scenario] WHERE [scenario_id] = @scenario_id);

-- Walk/Bike mode
-- Data exporter was appending wrong skims to walk/bike, java not using input file to find pairs
-- The original fix threw out pairs not in the walk threshold and used distance from MGRATOMGRA
-- So this may not match older scenarios
with walk_bike AS (
SELECT 
	SUM(([party_size] * [trip_time]) / 60) / @sample_rate AS walk_bike_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [trip_ij].[mode_id] IN (9,10) -- walk, bike
	),
--walk to transit access/egress time
walk_transit AS (
SELECT
	(SUM(ISNULL([party_size] * CAST(tap1.[time_boarding_actual] AS float) / 60, 0)) +
	SUM(ISNULL([party_size] * CAST(tap2.[time_alighting_actual] AS float) / 60, 0))) / @sample_rate AS walk_transit_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id] 
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 11 AND 15 -- walk to transit
	),
-- knr/pnr egress time 
kpnr_egress AS (
SELECT	
	SUM(CASE	WHEN [inbound] = 1 THEN ISNULL([party_size] * CAST(tap1.[time_boarding_actual] AS float) / 60, 0)
				WHEN [inbound] = 0 THEN ISNULL([party_size] * CAST(tap2.[time_alighting_actual] AS float) / 60, 0)
				END
				) / @sample_rate AS knr_pnr_egress_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id] 
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 16 AND 25 -- knr and pnr
	)
SELECT	
	@scenario_id AS scenario_id
	,ISNULL(walk_bike_time, 0) * 60 AS walk_bike_time
	,ISNULL(walk_transit_time, 0) * 60 AS walk_transit_time
	,ISNULL(knr_pnr_egress_time, 0) * 60 AS knr_pnr_egress_time
	,(ISNULL(walk_bike_time, 0) + ISNULL(walk_transit_time, 0) + ISNULL(knr_pnr_egress_time, 0)) * 60 AS total
FROM 
	walk_bike, walk_transit, kpnr_egress
GO

-- Add metadata for [rtp_2015].[sp_pm_7e]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7e', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7e', 'MS_Description', 'performance metric 7e'
GO




-- Create stored procedure for performance metric #7f
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_7f]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_7f]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_7f]
	@scenario_id smallint
AS

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 5/19/2014 by Gregor Schroeder
	Description: Performance Measure 7F, Percent of Population engaging in more than 20 minutes of daily transporation related physical activity
*/

-- Walk/Bike mode
-- Data exporter was appending wrong skims to walk/bike, java not using input file to find pairs
-- The original fix threw out pairs not in the walk threshold and used distance from MGRATOMGRA
-- So this may not match older scenarios
with walk_bike AS (
SELECT	
	@scenario_id AS scenario_id
	,[lu_person_id]
	,SUM([trip_time]) AS walk_bike_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
INNER JOIN 
	[abm].[tour_ij_person]
ON 
	[trip_ij].[scenario_id] = [tour_ij_person].[scenario_id]
	AND [trip_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [tour_ij_person].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 9 AND 10 -- walk, bike modes
GROUP BY 
	[lu_person_id]
	),
	
--walk to transit access/egress time
walk_transit AS (
SELECT	
	@scenario_id AS scenario_id
	,[lu_person_id]
	,SUM(ISNULL((CAST(tap1.[time_boarding_actual] AS float) + CAST(tap2.[time_alighting_actual] AS float)), 0)) AS walk_transit_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] AS tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] AS tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id] 
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
INNER JOIN 
	[abm].[tour_ij_person]
ON 
	[trip_ij].[scenario_id] = [tour_ij_person].[scenario_id]
	AND [trip_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id] 
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [tour_ij_person].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 11 AND 15 -- walk to transit modes
GROUP BY 
	[trip_ij].[scenario_id]
	,[lu_person_id]
	),

-- knr/pnr egress time 
kpnr_egress AS (
SELECT	
	@scenario_id AS scenario_id
	,[lu_person_id]
	,SUM(CASE	WHEN [inbound] = 1 THEN ISNULL([party_size] * CAST(tap1.[time_boarding_actual] AS float), 0)
				WHEN [inbound] = 0 THEN ISNULL([party_size] * CAST(tap2.[time_alighting_actual] AS float), 0)
				END
				) AS kpnr_egress_time
FROM 
	[abm].[trip_ij] -- individual and joint models only
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap1 
ON 
	[trip_ij].[scenario_id] = tap1.[scenario_id]
	AND [trip_ij].[orig_geography_zone_id] = tap1.[geography_zone_id]
	AND [trip_ij].[board_transit_tap_id] = tap1.[transit_tap_id]
LEFT OUTER JOIN 
	[abm].[transit_tap_walk] tap2
ON 
	[trip_ij].[scenario_id] = tap2.[scenario_id]
	AND [trip_ij].[dest_geography_zone_id] = tap2.[geography_zone_id] 
	AND [trip_ij].[alight_transit_tap_id] = tap2.[transit_tap_id]
INNER JOIN 
	[abm].[tour_ij_person]
ON 
	[trip_ij].[scenario_id] = [tour_ij_person].[scenario_id]
	AND [trip_ij].[tour_ij_id] = [tour_ij_person].[tour_ij_id]
WHERE 
	[trip_ij].[scenario_id] = @scenario_id
	AND [tour_ij_person].[scenario_id] = @scenario_id
	AND [mode_id] BETWEEN 16 AND 25 -- knr and pnr modes
GROUP BY 
	[trip_ij].[scenario_id]
	,[lu_person_id]
	)

SELECT	
	@scenario_id AS [scenario_id]
	,sum(ISNULL(walk_bike_time, 0) + ISNULL(walk_transit_time, 0) + ISNULL(kpnr_egress_time, 0)) AS at_time
	,1.0* SUM(CASE	WHEN ISNULL(walk_bike_time, 0) + ISNULL(walk_transit_time, 0) + ISNULL(kpnr_egress_time, 0) > 20 THEN 1
					ELSE 0
					END) / COUNT(*) AS pct_pop
	,COUNT(*) AS pop_at			
FROM 
	[abm].[lu_person]
LEFT OUTER JOIN 
	walk_bike
ON 
	[lu_person].[scenario_id] = walk_bike.[scenario_id]
	AND [lu_person].[lu_person_id] = walk_bike.[lu_person_id]
LEFT OUTER JOIN 
	walk_transit
ON 
	[lu_person].[scenario_id] = walk_transit.[scenario_id]
	AND [lu_person].[lu_person_id] = walk_transit.[lu_person_id]
LEFT OUTER JOIN 
	kpnr_egress
ON 
	[lu_person].[scenario_id] = kpnr_egress.[scenario_id]
	AND [lu_person].[lu_person_id] = kpnr_egress.[lu_person_id]
WHERE 
	[lu_person].[scenario_id] = @scenario_id
GO

-- Add metadata for [rtp_2015].[sp_pm_7f]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7f', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_7f', 'MS_Description', 'performance metric 7f'
GO




-- Create stored procedure for performance metric #8ab_auto
IF  EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[rtp_2015].[sp_pm_8ab_auto]') AND type in (N'P', N'PC'))
DROP PROCEDURE [rtp_2015].[sp_pm_8ab_auto]
GO

CREATE PROCEDURE [rtp_2015].[sp_pm_8ab_auto]
	@scenario_id smallint
	,@max_time_skim_filter int
	,@beach bit = 0
	,@health bit = 0
	,@job_college bit = 0
	,@park bit = 0
	,@retail bit = 0
AS 

/*	Author: Ziying Ouyang and Gregor Schroeder
	Date: Revised 9/17/2014 by Gregor Schroeder
	Description: Performance Measure 8a and b, 
*/

IF (SELECT COUNT(NULLIF(@beach, 0)) + COUNT(NULLIF(@health, 0)) + COUNT(NULLIF(@job_college, 0)) +
	COUNT(NULLIF(@park, 0)) + COUNT(NULLIF(@retail, 0))) = 0
BEGIN
	print 'WARNING: Please choose a skim segmentation'
	RETURN
END

IF (SELECT COUNT(NULLIF(@beach, 0)) + COUNT(NULLIF(@health, 0)) + COUNT(NULLIF(@job_college, 0)) +
	COUNT(NULLIF(@park, 0)) + COUNT(NULLIF(@retail, 0))) > 1
BEGIN
	print 'WARNING: Please choose only one skim segmentation'
	RETURN
END

-- Get origin and destination series 13 tazs that match skim and enrollment+employment conditions
SELECT 
	DISTINCT([orig_geography_zone_id]) AS orig_taz
INTO
	#tt -- hold this in a local temporary table, auto dropped when stored procedure finishes
FROM 
	[abm].[hwy_skims]
INNER JOIN
	[ref].[time_period]
ON
	[hwy_skims].[time_period_id] = [time_period].[time_period_id]
INNER JOIN (
	SELECT 
		34 AS geography_type_id -- series 13 taz id
		,[taz_geography_zone_id]
	FROM 
		[abm].[lu_mgra_input]
	INNER JOIN
		[ref].[mgra13_xref_taz13]
	ON
		[lu_mgra_input].[geography_zone_id] = [mgra13_xref_taz13].[mgra_geography_zone_id]
	WHERE 
		[scenario_id] = @scenario_id 
	GROUP BY 
		[taz_geography_zone_id]
	HAVING 
		CASE	WHEN @beach = 1 AND SUM([beachactive]) > 0.5 THEN 1
				WHEN @health = 1 AND SUM([emp_health]) > 1 THEN 1
				WHEN @job_college = 1 AND SUM([collegeenroll] + [othercollegeenroll] + [emp_total]) > 0 THEN 1
				WHEN @park = 1 AND SUM([parkactive]) > 0.5 THEN 1
				WHEN @retail = 1 AND SUM([emp_retail]) > 1 THEN 1
				ELSE 0
				END = 1
		) dest
ON 
	[hwy_skims].[dest_geography_zone_id] = dest.[taz_geography_zone_id]
WHERE 
	[scenario_id] = @scenario_id 
	AND [time_resolution_id] = 1 -- ABM 5 time of day
	AND [time_period_number] = 2 -- AM peak
	and ([time_drive_alone_free] <= @max_time_skim_filter AND [time_drive_alone_free] > 0) 

-- Get populations of interest in each series 13 taz
SELECT 
	[lu_person].[scenario_id]
	,[taz_geography_zone_id]
	,COUNT(*) as taz_tot_pop
	,SUM(CASE WHEN ISNULL([poverty], 99) <= 2 THEN 1 ELSE 0 END) AS taz_pop_lowInc
	,SUM(CASE WHEN ISNULL([poverty], 99) > 2 THEN 1 ELSE 0 END) AS taz_pop_nonlowInc
	,SUM(CASE WHEN ISNULL([race_id], 0) > 1 OR ISNULL([hisp_id], 1) > 1 THEN 1 ELSE 0 END) AS taz_pop_minority
	,SUM(CASE WHEN ISNULL([race_id], 0) = 1 AND ISNULL([hisp_id], 1) = 1 THEN 1 ELSE 0 END) AS taz_pop_nonminority	
	,SUM(CASE WHEN AGE >= 75  THEN 1 ELSE 0 END) AS taz_pop_senior
	,SUM(CASE WHEN AGE < 75  THEN 1 ELSE 0 END) AS taz_pop_nonsenior
INTO
	#pop_seg_by_taz
FROM 
	[abm].[lu_person]
INNER JOIN 
	[abm].[lu_hh]
ON 
	[lu_person].[scenario_id] = [lu_hh].[scenario_id]
	AND [lu_person].[lu_hh_id] = [lu_hh].[lu_hh_id] 
INNER JOIN
	[ref].[mgra13_xref_taz13]
ON
	[lu_hh].[geography_zone_id] = [mgra13_xref_taz13].[mgra_geography_zone_id]
WHERE
	[lu_person].[scenario_id] = @scenario_id
	AND [lu_hh].[scenario_id] = @scenario_id
GROUP BY 
	[lu_person].[scenario_id]
	,[taz_geography_zone_id];

-- Get total population and population of interest from the above two temp tables
with total_pop AS (
SELECT
	#pop_seg_by_taz.[scenario_id]
	,sum(taz_tot_pop) as tot_pop
	,sum(taz_pop_lowInc) as pop_lowInc
	,sum(taz_pop_nonlowInc) as pop_nonlowInc
	,sum(taz_pop_minority) as pop_minority
	,sum(taz_pop_nonminority) as pop_nonminority
	,sum(taz_pop_senior) as pop_senior
	,sum(taz_pop_nonsenior) as pop_nonsenior 
FROM 
	#pop_seg_by_taz
GROUP BY
	#pop_seg_by_taz.[scenario_id]
),
pop_of_interest AS (
SELECT
	#pop_seg_by_taz.[scenario_id]
	,sum(taz_tot_pop) as tot_pop
	,sum(taz_pop_lowInc) as pop_lowInc
	,sum(taz_pop_nonlowInc) as pop_nonlowInc
	,sum(taz_pop_minority) as pop_minority
	,sum(taz_pop_nonminority) as pop_nonminority
	,sum(taz_pop_senior) as pop_senior
	,sum(taz_pop_nonsenior) as pop_nonsenior 
FROM 
	#pop_seg_by_taz
INNER JOIN
	#tt
ON 
	#pop_seg_by_taz.[taz_geography_zone_id] = #tt.orig_taz
GROUP BY
	#pop_seg_by_taz.[scenario_id]
)
-- and output results
SELECT
	total_pop.scenario_id, 
	ISNULL(pop_of_interest.tot_pop / NULLIF([sample_rate], 0), 0) AS tot_pop, 
	1.0* pop_of_interest.tot_pop / total_pop.tot_pop as pct_pop, 
	ISNULL(pop_of_interest.pop_lowInc / NULLIF([sample_rate], 0), 0) AS pop_lowInc, 
	1.0 * pop_of_interest.pop_lowInc / total_pop.pop_lowInc as pct_lowinc, 
	1.0 * pop_of_interest.pop_nonlowInc / total_pop.pop_nonlowInc as pct_nonlowinc, 
	ISNULL(pop_of_interest.pop_minority / NULLIF([sample_rate], 0), 0) AS pop_minority, 
	1.0 * pop_of_interest.pop_minority / total_pop.pop_minority as pct_minor, 
	1.0 * pop_of_interest.pop_nonminority / total_pop.pop_nonminority as pct_nonminor, 
	ISNULL(pop_of_interest.pop_senior / NULLIF([sample_rate], 0), 0) AS pop_senior, 
	1.0 * pop_of_interest.pop_senior / total_pop.pop_senior as pct_senior, 
	1.0 * pop_of_interest.pop_nonsenior / total_pop.pop_nonsenior as pct_nonsenior 
FROM 
	total_pop
INNER JOIN
	[ref].[scenario]
ON
	total_pop.[scenario_id] = [scenario].[scenario_id]
INNER JOIN 
	pop_of_interest 
ON 
	total_pop.scenario_id = pop_of_interest.scenario_id
GO

-- Add metadata for [rtp_2015].[sp_pm_8a_auto_job_college]
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_8ab_auto', 'SUBSYSTEM', 'rtp_2015'
EXECUTE [db_meta].[add_xp] 'rtp_2015.sp_pm_8ab_auto', 'MS_Description', 'performance metric 8a and 8b for autos using highway skims and mgra-based input file'
GO