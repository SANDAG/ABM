# top ---------------------------------------------------------------------------------

## ------------------------------------------------------------------------------------
##
## Script name: cbm_data.R
##
## Purpose of script:
##    - Reads and cleans border crossing survey data (currently weekday only)
##    - Creates persons.csv file
##    - Creates households.csv file
##    - Creates places.csv file
##    - Creates trips.csv file
##    - Creates tours.csv file
##    - Creates summaries
##
## Author: Cundo Arellano (cundo.arellano@rsginc.com)
##
## Date Created: 2020-07-09
##
## ------------------------------------------------------------------------------------

# set working directory ---------------------------------------------------------------

setwd("E:/projects/clients/sandag/XBORDER/Tasks/T03 Data_Processing/survey_data_processing")

# load packages -----------------------------------------------------------------------

library(tidyverse)
library(haven)
library(purrr)
library(readxl)
library(Hmisc)
library(reshape2)
library(hms)
library(expss)
library(openxlsx)
library(lubridate)
library(ggplot2)
library(sf)
library(rgdal)
library(rhdf5)
library(omxr)

# inputs ------------------------------------------------------------------------------

# survey data
# TODO: read full survey and subset non part II respondents
cb_data <- read.csv('input/SANDAG Crossborder Study Completed Diaries no PII.csv')
cb_data_dictionary <- read_excel('input/SANDAG Crossborder Study Completed Diaries no PII_Data Dictionary.xlsx', skip = 1)
cb_data_dictionary_values <- read_excel('input/SANDAG Crossborder Study Completed Diaries no PII_Data Dictionary.xlsx', sheet = 2, skip = 1)

# 2011 survey data
old_cb_data <- read.csv('input/xborder_2011_data_persons.csv')

# average daily crossing volumes broken up by 10 target groups
# target group distinctions are POE (3) and lane cross type (4; 2 for Tecate POE)
adcv <- read.csv('input/adcv_mx.csv')

# shapefiles
mgra_shp <- sf::read_sf("input/shapefiles/mgra/SANDAG_MGRA.shp")
taz_shp <- sf::read_sf("input/shapefiles/taz/SANDAG_TAZ.shp")

# port of entry TAZs
san_ysidro <- 1
ped_west <- 1
otay_mesa <- 2
tecate <- 4

# wait time data
wait_time <- read_csv("input/border_wait_times_fall_2019_weekday.csv")

# distance travel skims (TAZ)
# from SANDAG ABM Output: SOV Midday Medium VOT
# scenario in SANDAG Server: C:\ABM_runs\maint_2019_RSG\Model\ABM2_14_2_0_final
dist_mat <- read_omx("input/traffic_skims_MD.omx", "MD_SOV_NT_M_DIST")

# generate csv outputs
csv_out <- TRUE

# generate summaries and plots
summaries <- TRUE

# create output directory (relative to current working directory)
if (!dir.exists('output')) {
  dir.create(file.path(getwd(), 'output'))
}

# add survey data labels --------------------------------------------------------------

# rename column and fill in NA values
colnames(cb_data_dictionary_values)[2] <- "Range"
cb_data_dictionary_values <- cb_data_dictionary_values %>% fill(Value)

# apply labels (i.e. survey questions) to each survey question
q_labels <- setNames(as.character(cb_data_dictionary$Label), 
                    cb_data_dictionary$Variable)
label(cb_data) <- as.list(q_labels[match(names(cb_data), names(q_labels))])

# create person files -----------------------------------------------------------------

p_dict <- c(respid = 'respid',
            age = "AGE",
            gender = 'GENDER',
            age_cat = 'AGECODE',
            student_status = 'q23',
            emp_status = 'EMPSTATUS',
            country_emp_status = 'q24')

p_dict_vals <- vector()
for (value in p_dict) {
  p_dict_vals <- c(p_dict_vals, value)
}

# subset survey data and rename columns
persons_df <- data.frame(cb_data[,p_dict_vals])
persons_df <- rename(persons_df, !!! p_dict)

# determine student status from employment status where employment status (q23) value:
# of 5 = college -> 2
# of 6 = high school -> 1
# anything else = not student -> 3
persons_df <- persons_df %>% 
  mutate(student_status = if_else(student_status == 5, 2, if_else(student_status == 6, 1, 3)))

# add person and household sequential IDs
persons_df$perid <- 1:nrow(persons_df)
persons_df$hhid <- 1:nrow(persons_df)

# output table
if (csv_out) {
  write_csv(persons_df, 'output/persons.csv')
}

# create household files --------------------------------------------------------------

h_dict <- c(respid = 'respid',
            residency = 'qsc1',
            ca_residency = 'q12',
            ca_county = 'q13',
            ca_city = 'q14',
            hworkers = 'q26',
            persons = 'q25',
            veh = 'q27',
            income = 'MONTHLYINCOME')

h_dict_vals <- vector()
for (value in h_dict) {
  h_dict_vals <- c(h_dict_vals, value)
}

# subset survey data and rename columns
households_df <- data.frame(cb_data[,h_dict_vals])
households_df <- rename(households_df, !!! h_dict)

# add household sequential IDs from persons dataframe
households_df <- merge(households_df, persons_df[, c('respid', 'hhid')])

# check if number of hh workers is greater than number of hh persons
# sum( households_df$hworkers > households_df$persons , na.rm = TRUE)

# output table
if (csv_out) {
  write_csv(households_df, 'output/households.csv')
}

# create places files -----------------------------------------------------------------

# number of places visited equals number of entries for each household
places_df <- data.frame(respid = cb_data$respid, plano = cb_data$NUMBER_OF_TRIPS)
places_df <- places_df %>% uncount(plano, .id = 'plano')

# diary data indexing starts at 0
places_df$plano <- places_df$plano - 1

# x and y coordinates for each place
x_coords <- cb_data[, grep(c('respid|LONG_'), names(cb_data))]
x_coords <- melt(x_coords, id = 'respid', na.rm = TRUE)
x_coords$variable <- gsub('LONG_', '', x_coords$variable)
y_coords <- cb_data[, grep(c('respid|LAT_'), names(cb_data))]
y_coords <- melt(y_coords, id = 'respid', na.rm = TRUE)
y_coords$variable <- gsub('LAT_', '', y_coords$variable)
coords <- merge(x_coords, y_coords, by = c('respid', 'variable'))
coords <- coords %>% rename(plano = variable, XCORD = value.x, YCORD = value.y)

# map x and y coordinates to their respective MGRA and TAZ
mgra_shp <- st_transform(mgra_shp, crs = 4326)
taz_shp <- st_transform(taz_shp, crs = 4326)
places_pnts <- st_as_sf(coords, coords = c('XCORD', 'YCORD'), crs = st_crs(mgra_shp))

places_maz_taz <- places_pnts %>% 
  mutate(MGRA = as.integer(st_intersects(geometry, mgra_shp)),
         TAZ = as.integer(st_intersects(geometry, taz_shp)))
places_maz_taz <- data.frame(places_maz_taz)
places_maz_taz <- places_maz_taz %>% select(-geometry)

# trip purpose for each place
purpose <- cb_data[, grep(c('respid|Q4_'), names(cb_data))]
purpose <- purpose[, -grep('_OTH_|DIARY', names(purpose))]
purpose <- melt(purpose, id = 'respid', na.rm = TRUE)
purpose$variable <- gsub('Q4_', '', purpose$variable)
purpose <- purpose %>% rename(plano = variable, TPURP = value)
purpose_labels <- cb_data_dictionary_values[cb_data_dictionary_values$Value == 'Q4_0', c('Range', 'Label')]
purpose <- merge(purpose, purpose_labels, by.x = 'TPURP', by.y = 'Range', all.x = TRUE)
purpose <- purpose %>% rename(PNAME = 'Label')

# trip purpose for each place according to CBM Specification Report
purpose_cbm <- data.frame(purpose)
purpose_cbm <- purpose_cbm %>% mutate(TPURP = case_when(TPURP %in% c(1, 5, 6, 7, 8, 10, 11, 12, 97) ~ 6,
                                                        TPURP == 2 ~ 1,
                                                        TPURP == 3 ~ 2,
                                                        TPURP == 4 ~ 4, 
                                                        TPURP == 9 ~ 5)) %>%
  rename(TPURP_CBM = 'TPURP', PNAME_CBM = 'PNAME')
purpose_cbm <- purpose_cbm %>% mutate(PNAME_CBM = case_when(TPURP_CBM == 1 ~ 'Work',
                                                        TPURP_CBM == 2 ~ 'School',
                                                        TPURP_CBM == 3 ~ 'Cargo',
                                                        TPURP_CBM == 4 ~ 'Shop',
                                                        TPURP_CBM == 5 ~ 'Visit',
                                                        TPURP_CBM == 6 ~ 'Other'))

# trip mode for each place
mode <- cb_data[, grep(c('respid|Q5_'), names(cb_data))]
mode <- mode[, -grep('_OTH_', names(mode))]
mode <- melt(mode, id = 'respid', na.rm = TRUE)
mode$variable <- gsub('Q5_', '', mode$variable)
mode <- mode %>% rename(plano = variable, MODE = value)

### OLD: may change SANDAG ABM to 48 half-hour time segments
# create time bins per SANDAG ABM
# Bin 1 is for time before 5:00 AM
# Bins 2 - 39 are for every half hour time slots between 5:00 AM and 12:00 AM
# Bin 40 is for time after 12:00 AM
#time_bins <- seq(as.POSIXct(paste(Sys.Date(), "03:00:00", sep = " ")),
#                     as.POSIXct(paste(Sys.Date() + 1, "00:00:00", sep = " ")), 
#                     by = 30*60) 
#time_bins <- time_bins[-(2:4)]

# create (potentially) new SANDAG ABM time bins
# 48 half hour time time segments starting from 3:00 AM
time_bins <- seq(as.POSIXct(paste(Sys.Date(), "03:00:00", sep = " ")),
                 as.POSIXct(paste(Sys.Date() + 1, "02:30:00:")),
                 by = 30*60)

# trip arrival time bins for each place
arr_bin <- cb_data[, grep(c('respid|Q2_'), names(cb_data))]
arr_bin <- arr_bin[, -grep('DIARY', names(arr_bin))]
arr_bin[, -1] <- lapply(2:ncol(arr_bin), function(x) as.POSIXct(arr_bin[[x]], format = "%H:%M:%S"))
arr_bin <- melt(arr_bin, id = 'respid', na.rm = TRUE)
arr_bin$variable <- gsub('Q2_', '', arr_bin$variable)
arr_bin <- arr_bin %>% rename(plano = variable, ARR_BIN = value)
arr_bin$ARR_BIN <- findInterval(arr_bin$ARR_BIN, time_bins)
  
# trip departure time bins for each place
dep_bin <- cb_data[, grep(c('respid|Q3_'), names(cb_data))]
dep_bin <- dep_bin[, -grep('DIARY', names(dep_bin))]
dep_bin[, -1] <- lapply(2:ncol(dep_bin), function(x) as.POSIXct(dep_bin[[x]], format = "%H:%M:%S"))
dep_bin <- melt(dep_bin, id = 'respid', na.rm = TRUE)
dep_bin$variable <- gsub('Q3_', '', dep_bin$variable)
dep_bin <- dep_bin %>% rename(plano = variable, DEP_BIN = value)
dep_bin$DEP_BIN <- findInterval(dep_bin$DEP_BIN, time_bins)

# trip arrival time for each place
arr <- cb_data[, grep(c('respid|Q2_'), names(cb_data))]
arr <- arr[, -grep('DIARY', names(arr))]
arr <- apply(arr, 2, function(x) gsub("^$|^ $", NA, x))
arr <- as.data.frame(arr)
arr <- arr %>% mutate(Q2_0 = as_hms(Q2_0))
arr <- arr %>% mutate(Q2_1 = as_hms(Q2_1))
arr <- arr %>% mutate(Q2_2 = as_hms(Q2_2))
arr <- arr %>% mutate(Q2_3 = as_hms(Q2_3))
arr <- arr %>% mutate(Q2_4 = as_hms(Q2_4))
arr <- arr %>% mutate(Q2_5 = as_hms(Q2_5))
arr <- arr %>% mutate(Q2_6 = as_hms(Q2_6))
arr <- arr %>% mutate(Q2_7 = as_hms(Q2_7))
arr <- arr %>% mutate(Q2_8 = as_hms(Q2_8))
arr <- arr %>% mutate(Q2_9 = as_hms(Q2_9))
arr <- arr %>% mutate(Q2_10 = as_hms(Q2_10))
arr <- arr %>% mutate(Q2_11 = as_hms(Q2_11))
arr <- melt(arr, id = 'respid', na.rm = TRUE)
arr$variable <- gsub('Q2_', '', arr$variable)
arr <- arr %>% rename(plano = variable, arr = value)
arr <- arr %>% separate(arr, c('ARR_HR', 'ARR_MIN', 'ARR_SEC'), sep = ':')
arr$ARR_SEC <- NULL

# trip departure time for each place
dep <- cb_data[, grep(c('respid|Q3_'), names(cb_data))]
dep$Q3_DIARY <- NULL
dep <- apply(dep, 2, function(x) gsub("^$|^ $", NA, x))
dep <- as.data.frame(dep)
dep <- dep %>% mutate(Q3_0 = as_hms(Q3_0))
dep <- dep %>% mutate(Q3_1 = as_hms(Q3_1))
dep <- dep %>% mutate(Q3_2 = as_hms(Q3_2))
dep <- dep %>% mutate(Q3_3 = as_hms(Q3_3))
dep <- dep %>% mutate(Q3_4 = as_hms(Q3_4))
dep <- dep %>% mutate(Q3_5 = as_hms(Q3_5))
dep <- dep %>% mutate(Q3_6 = as_hms(Q3_6))
dep <- dep %>% mutate(Q3_7 = as_hms(Q3_7))
dep <- dep %>% mutate(Q3_8 = as_hms(Q3_8))
dep <- dep %>% mutate(Q3_9 = as_hms(Q3_9))
dep <- dep %>% mutate(Q3_10 = as_hms(Q3_10))
dep <- dep %>% mutate(Q3_11 = as_hms(Q3_11))
dep <- melt(dep, id = 'respid', na.rm = TRUE)
dep$variable <- gsub('Q3_', '', dep$variable)
dep <- dep %>% rename(plano = variable, dep = value)
dep <- dep %>% separate(dep, c('DEP_HR', 'DEP_MIN', 'DEP_SEC'), sep = ':')
dep$DEP_SEC <- NULL

# distance from previous place to current place
dist <- cb_data[, grep(c('respid|DIST_'), names(cb_data))]
dist <- dist[, -grep('_FINAL', names(dist))]
dist <- melt(dist, id = 'respid', na.rm = TRUE)
dist$variable <- gsub('DIST_', '', dist$variable)
dist <- dist %>% rename(plano = variable, SURVEY_DIST = value)

# point of entries used to enter and exit USA
# 1 = San Ysidro, 2 = PedWest, 3 = Otay Mesa, 4 = Tecate
poe <- cb_data[, c('respid', 'Q0_DIARY', 'Q3_DIARY')]
poe <- poe %>% rename(POE_IN = 'Q0_DIARY', POE_OUT = 'Q3_DIARY')

# total travelers (assumed constant over the day)
# q3: number of people in car
# q4: number of people traveling with (added 1 to obtain number of travelers)
# took maximum of the 2 values to obtain total travelers
tot_trav <- cb_data[, c('respid', 'q3', 'q11')] %>% mutate(q11 = q11 + 1)
tot_trav <- tot_trav %>% mutate(TOTTR = pmax(q3, q11, na.rm = TRUE)) %>% 
  replace_na(list(TOTTR = 1)) %>% select(-q3, -q11)
var_lab(tot_trav$TOTTR) <- 'Total travelers'

# traveler(s) returned to Mexico that day
return_mx <- cb_data[, c('respid', 'Q1_DIARY')]
return_mx <- return_mx %>% rename(RETURN_MX = 'Q1_DIARY')

# merge all data
keys <- c('respid', 'plano')
places_df <- merge(places_df, coords, by = keys, all.x = TRUE)
places_df <- merge(places_df, purpose, by = keys, all.x = TRUE)
places_df <- merge(places_df, mode, by = keys, all.x = TRUE)
places_df <- merge(places_df, arr, by = keys, all.x = TRUE)
places_df <- merge(places_df, dep, by = keys, all.x = TRUE)
places_df <- merge(places_df, dist, by = keys, all.x = TRUE)
places_df <- merge(places_df, poe, by = 'respid', all.x = TRUE)
places_df <- merge(places_df, tot_trav, by = 'respid', all.x = TRUE)
places_df <- merge(places_df, arr_bin, by = keys, all.x = TRUE)
places_df <- merge(places_df, dep_bin, by = keys, all.x = TRUE)
places_df <- merge(places_df, return_mx, by = 'respid', all.x = TRUE)
places_df <- merge(places_df, purpose_cbm, by = keys, all.x = TRUE)
places_df <- merge(places_df, places_maz_taz, by = keys, all.x = TRUE)

places_df <- places_df %>% rename(HH_ID = 'respid', PLANO = 'plano')

# diary data indexing starts at 0 (re-add 1)
places_df$PLANO <- places_df$PLANO + 1

# obtain return time (and bins) between last place and border
dep_mx <- cb_data[, c('respid', 'Q2_DIARY', 'Q1_DIARY')]
dep_mx$Q2_DIARY <- as.character(dep_mx$Q2_DIARY)
dep_mx$Q2_DIARY <- as.POSIXct(dep_mx$Q2_DIARY, format = "%H:%M:%S")
dep_mx <- dep_mx %>% filter(!is.na(Q2_DIARY), Q1_DIARY == 1) %>% select(-Q1_DIARY)
dep_mx <- dep_mx %>% rename(dep_mx = 'Q2_DIARY')
dep_mx_bin <- dep_mx %>% mutate(dep_mx = findInterval(dep_mx, time_bins)) %>% rename(dep_mx_bin = 'dep_mx')
dep_mx <- dep_mx %>% separate(dep_mx, c('dep_mx_date_hr', 'dep_mx_min', 'dep_mx_sec'), sep = ':') %>% 
  separate(dep_mx_date_hr, c('date', 'dep_mx_hr'), sep = " ")
dep_mx <- dep_mx %>% select(-date, -dep_mx_sec)
dep_mx <- merge(dep_mx, dep_mx_bin, by = 'respid', all.x = TRUE)

# obtain list of last places excluding records where mexican travelers did not return to Mexico
last_stop <- places_df[, c('RETURN_MX', 'PLANO', 'HH_ID')] %>% filter(RETURN_MX == 1) %>% 
  group_by(HH_ID) %>% top_n(1, PLANO) %>% select(-RETURN_MX)
last_stop <- merge(last_stop, dep_mx, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
places_df <- merge(places_df, last_stop, by.x = c('HH_ID', 'PLANO'), all.x = TRUE)

# add return time between last place and border
places_df$DEP_HR <- coalesce(places_df$DEP_HR, places_df$dep_mx_hr)
places_df$DEP_MIN <- coalesce(places_df$DEP_MIN, places_df$dep_mx_min)
places_df$DEP_BIN <- coalesce(places_df$DEP_BIN, places_df$dep_mx_bin)
places_df <- places_df %>% select(-dep_mx_hr, -dep_mx_min, -dep_mx_bin)

# add missing columns
places_df <- places_df %>% add_column(PLACE_ID = places_df$PLANO, PER_NUM = 1)
places_df$BORDER_DIST <- 0
# reorder the columns
col_order <- c('HH_ID', 'PLACE_ID', 'PLANO', 'PER_NUM', 'MGRA', 'TAZ', 'TPURP', 'TPURP_CBM', 'MODE', 
               'ARR_HR', 'ARR_MIN', 'ARR_BIN', 'DEP_HR', 'DEP_MIN', 'DEP_BIN', 'TOTTR', 'SURVEY_DIST','BORDER_DIST',
               'POE_IN', 'POE_OUT', 'RETURN_MX', 'PNAME', 'PNAME_CBM', 'XCORD', 'YCORD')
places_df <- places_df[, col_order]

#if (csv_out) {
#  write_csv(places_df, 'output/places.csv')
#}

# add port of entries (entering/exiting) as places ------------------------------------
  
## port of entry (entering)
poe_in <- places_df %>% group_by(HH_ID) %>% slice(which.min(PLANO)) %>%
  mutate(PLACE_ID = PLANO - 1, PLANO = PLANO - 1, BORDER_DIST = 0, PNAME = 'Border',SURVEY_TPURP = 0, 
         SURVEY_TPURP2 = 0, TPURP = 0, SURVEY_DIST = 0, TPURP_CBM = 0, PNAME_CBM = 'Border', TAZ = NA, 
         MGRA = NA, XCORD = NA, YCORD = NA) %>%
  select(-ARR_HR, -ARR_MIN, -DEP_HR, -DEP_MIN, -ARR_BIN, -DEP_BIN)

# add POE TAZs
poe_in <- poe_in %>% mutate(TAZ = case_when(POE_IN == 1 ~ san_ysidro[1],
                                              POE_IN == 2 ~ ped_west[1],
                                              POE_IN == 3 ~ otay_mesa[1],
                                              POE_IN == 4 ~ tecate[1]))

# arrival/departure times when entering the US
poe_in_arr_dep <- cb_data[, c('respid', 'START_US_TRIPS')]
poe_in_arr_dep <- poe_in_arr_dep %>% separate(START_US_TRIPS, c('ARR_HR', 'ARR_MIN', 'ARR_SEC'), sep = ':')
poe_in_arr_dep$ARR_SEC <- NULL
poe_in_arr_dep <- poe_in_arr_dep %>% add_column(DEP_HR = poe_in_arr_dep$ARR_HR, DEP_MIN = poe_in_arr_dep$ARR_MIN)

# arrival/departure time bins when entering the US
poe_in_arr_dep_bin <- cb_data[, c('respid', 'START_US_TRIPS')]
poe_in_arr_dep_bin$START_US_TRIPS <- as.POSIXct(poe_in_arr_dep_bin$START_US_TRIPS, format = "%H:%M:%S")
poe_in_arr_dep_bin <- poe_in_arr_dep_bin %>% rename(ARR_BIN = 'START_US_TRIPS')
poe_in_arr_dep_bin$ARR_BIN <- findInterval(poe_in_arr_dep_bin$ARR_BIN, time_bins)
poe_in_arr_dep_bin <- poe_in_arr_dep_bin %>% add_column(DEP_BIN = poe_in_arr_dep_bin$ARR_BIN)

# merge attributes for port of entry (entering)
poe_in <- merge(poe_in, poe_in_arr_dep, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
poe_in <- merge(poe_in, poe_in_arr_dep_bin, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)

## port of entry (exiting)

# remove records of mexican travelers not returning to Mexico
poe_out <- places_df %>% filter(RETURN_MX == 1) %>% group_by(HH_ID) %>% top_n(1, PLANO) %>%
  mutate(PLACE_ID = PLANO + 1, PLANO = PLANO + 1, BORDER_DIST = 0, PNAME = 'Border', XCORD = NA, YCORD = NA, 
         SURVEY_TPURP = 0, SURVEY_TPURP2 = 0, TPURP = 0, TAZ = NA,TPURP_CBM = 0, PNAME_CBM = 'Border', 
         MGRA = NA, XCORD = NA, YCORD = NA) %>% 
  select(-ARR_HR, -ARR_MIN, -DEP_HR, -DEP_MIN, -SURVEY_DIST, -ARR_BIN, -DEP_BIN)

# add POE TAZs
poe_out <- poe_out %>% mutate(TAZ = case_when(POE_OUT == 1 ~ san_ysidro[1],
                                              POE_OUT == 2 ~ ped_west[1],
                                              POE_OUT == 3 ~ otay_mesa[1],
                                              POE_OUT == 4 ~ tecate[1]))

# distance between last stop and mexican border (poe exiting)
poe_out_dist_mx <- cb_data[, c('respid', 'DIST_FINAL')]
poe_out_dist_mx <- poe_out_dist_mx %>% rename(SURVEY_DIST = 'DIST_FINAL')

# arrival/departure times when exiting the US
poe_out_arr_dep <- cb_data[, c('respid', 'END_US_TRIPS')]
poe_out_arr_dep <- poe_out_arr_dep %>% separate(END_US_TRIPS , c('ARR_HR', 'ARR_MIN', 'ARR_SEC'), sep = ':')
poe_out_arr_dep$ARR_SEC <- NULL
poe_out_arr_dep <- poe_out_arr_dep %>% add_column(DEP_HR = poe_out_arr_dep$ARR_HR, DEP_MIN = poe_out_arr_dep$ARR_MIN)

# arrival/departure time bins when exiting the US
poe_out_arr_dep_bin <- cb_data[, c('respid', 'END_US_TRIPS')]
poe_out_arr_dep_bin$END_US_TRIPS <- as.POSIXct(poe_out_arr_dep_bin$END_US_TRIPS, format = "%H:%M:%S")
poe_out_arr_dep_bin <- poe_out_arr_dep_bin %>% rename(ARR_BIN = 'END_US_TRIPS')
poe_out_arr_dep_bin$ARR_BIN <- findInterval(poe_out_arr_dep_bin$ARR_BIN, time_bins)
poe_out_arr_dep_bin <- poe_out_arr_dep_bin %>% add_column(DEP_BIN = poe_out_arr_dep_bin$ARR_BIN)

# merge attributes for port of entry (exiting)
poe_out <- merge(poe_out, poe_out_dist_mx, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
poe_out <- merge(poe_out, poe_out_arr_dep, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
poe_out <- merge(poe_out, poe_out_arr_dep_bin, by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)

# combine poe_in and poe_out as border_df
border_df <- rbind(poe_in, poe_out)

# reorder columns and rows
border_df <- border_df[, col_order]

# add border_df to places_df
places_df <- rbind(places_df, border_df)
places_df <- places_df %>% arrange(HH_ID, PLACE_ID)

# duration at activity/place
places_df <- places_df %>% 
  mutate(PLACE_DUR = (as.integer(DEP_HR)*60 + as.integer(DEP_MIN)) - (as.integer(ARR_HR)*60 + as.integer(ARR_MIN)))

# shave off leading 0 from hour fields
places_df$DEP_HR <- substr(places_df$DEP_HR, regexpr("[^0]", places_df$DEP_HR), nchar(places_df$DEP_HR))
places_df$ARR_HR <- substr(places_df$ARR_HR, regexpr("[^0]", places_df$ARR_HR), nchar(places_df$ARR_HR))

places_df <- places_df[, c(col_order,'PLACE_DUR')]

# output table
if (csv_out) {
  write_csv(places_df, 'output/places.csv')
}
  
# create trips files ------------------------------------------------------------------

# origin and destination place numbers
od_plano <- places_df[, c('HH_ID', 'PLANO')]
od_plano <- od_plano %>% arrange(HH_ID, PLANO) %>% group_by(HH_ID) %>% 
  mutate(ORIG_PLACENO = PLANO, DEST_PLACENO = lead(PLANO, order_by = HH_ID))

# origin and destination coordinates
od_coords <- places_df[, c('HH_ID', 'PLANO', 'XCORD', 'YCORD')]
od_coords <- od_coords %>% group_by(HH_ID) %>%
  mutate(ORIG_X = XCORD, ORIG_Y = YCORD, 
         DEST_X = lead(XCORD, order_by = HH_ID), DEST_Y = lead(YCORD, order_by = HH_ID)) %>% 
  select(-XCORD, -YCORD)

# origin and destination trip purpose
od_trip_purp <- places_df[, c('HH_ID', 'PLANO', 'TPURP')]
od_trip_purp <- od_trip_purp %>% group_by(HH_ID) %>%
  mutate(ORIG_PURP = TPURP, DEST_PURP = lead(TPURP, order_by = HH_ID)) %>%
  select(-TPURP)

# origin and destination CBM trip purpose
od_trip_purp_cbm <- places_df[, c('HH_ID', 'PLANO', 'TPURP_CBM')]
od_trip_purp_cbm <- od_trip_purp_cbm %>% group_by(HH_ID) %>%
  mutate(ORIG_PURP_CBM = TPURP_CBM, DEST_PURP_CBM = lead(TPURP_CBM, order_by = HH_ID)) %>% 
  select(-TPURP_CBM)

# origin and destination arrival and departure times and bins
od_arr_dep <- places_df[, c('HH_ID', 'PLANO', 'ARR_HR', 'ARR_MIN', 'ARR_BIN', 'DEP_HR', 'DEP_MIN', 'DEP_BIN')]
od_arr_dep <- od_arr_dep %>% arrange(HH_ID, PLANO) %>% group_by(HH_ID) %>%
  mutate(ORIG_DEP_HR = DEP_HR, ORIG_DEP_MIN = DEP_MIN, ORIG_DEP_BIN = DEP_BIN, 
         DEST_ARR_HR = lead(ARR_HR, order_by = HH_ID), DEST_ARR_MIN = lead(ARR_MIN, order_by = HH_ID), 
         DEST_ARR_BIN = lead(ARR_BIN, order_by = HH_ID)) %>% 
  select(-ARR_HR, -ARR_MIN, -ARR_BIN, -DEP_HR, -DEP_MIN, -DEP_BIN)

# trip duration between origin and destination
od_trip_dur <- data.frame(od_arr_dep)
od_trip_dur <- od_trip_dur %>% select(-ORIG_DEP_BIN, -DEST_ARR_BIN)
od_trip_dur <- od_trip_dur %>% unite('ORIG_DEP', ORIG_DEP_HR:ORIG_DEP_MIN, sep = ':')
od_trip_dur <- od_trip_dur %>% unite('DEST_ARR', DEST_ARR_HR:DEST_ARR_MIN, sep = ':')
od_trip_dur$ORIG_DEP <- as.POSIXct(od_trip_dur$ORIG_DEP, format = "%H:%M")
od_trip_dur$DEST_ARR <- as.POSIXct(od_trip_dur$DEST_ARR, format = "%H:%M")
od_trip_dur <- od_trip_dur %>% mutate(TRIP_DUR = DEST_ARR - ORIG_DEP)
od_trip_dur$TRIP_DUR <- as_hms(od_trip_dur$TRIP_DUR)
od_trip_dur <- od_trip_dur %>% separate(TRIP_DUR, c('TRIP_DUR_HR', 'TRIP_DUR_MIN', 'TRIP_DUR_SEC'), sep = ":") %>%
  select(-TRIP_DUR_SEC)

# trip mode
od_trip_mode <- places_df[, c('HH_ID', 'PLANO', 'MODE')]
od_trip_mode <- od_trip_mode %>% arrange(HH_ID, PLANO) %>% group_by(HH_ID) %>% 
  mutate(TRIP_MODE = lead(MODE, order_by = HH_ID)) %>% select(-MODE)

# auto occupancy (assumed to remain constant for all trips)
tottr <- places_df[, c('HH_ID', 'PLANO', 'TOTTR')]

# trip mode per CBM Specification Report
od_trip_mode_cbm <- merge(od_trip_mode, tottr, by = c('HH_ID', 'PLANO'))
od_trip_mode_cbm <- od_trip_mode_cbm %>% 
  mutate(TRIP_MODE_CBM = case_when(
    TRIP_MODE == 0 ~ 7, 
    TRIP_MODE == 1 ~ 1,
    TRIP_MODE == 2 ~ 1,
    TRIP_MODE == 3 & TOTTR <= 2 ~ 2,
    TRIP_MODE == 3 & TOTTR > 2 ~ 3,
    TRIP_MODE == 4 ~ 3,
    TRIP_MODE == 5 & TOTTR <= 2 ~ 2,
    TRIP_MODE == 5 & TOTTR > 2 ~ 3,
    TRIP_MODE == 6 & TOTTR == 1 ~ 1,
    TRIP_MODE == 6 & TOTTR == 2 ~ 2,
    TRIP_MODE == 6 & TOTTR > 2 ~ 3,
    TRIP_MODE == 7 & TOTTR <= 2 ~ 2,
    TRIP_MODE == 7 & TOTTR > 2 ~ 3,
    TRIP_MODE == 8 ~ 5,
    TRIP_MODE == 9 ~ 5,
    TRIP_MODE == 10 ~ 5,
    TRIP_MODE == 11 ~ 6,
    TRIP_MODE == 12 ~ 6,
    TRIP_MODE == 13 ~ 6,
    TRIP_MODE == 14 ~ 7,
    TRIP_MODE == 15 ~ 4,
    TRIP_MODE == 16 ~ 4, 
    TRIP_MODE == 97 ~ 7
  )) %>% select(-TRIP_MODE, -TOTTR)

# tour mode 
# mode used to cross the border
tour_mode <- cb_data[, c('respid', 'q2')] %>% rename(HH_ID = 'respid', TOUR_MODE = 'q2')

# tour mode per CBM Specification Report
# mode used to cross the border
# LANE_TYPE = 1 (General), 2 (Ready Lane), 3 (SENTRI Lane)
tour_mode_cbm <- cb_data[, c('respid', 'q2', 'q3', 'q43')] %>% 
  rename(HH_ID = 'respid', TOUR_MODE = 'q2', NUM_TRAV = 'q3', LANE_TYPE = 'q43')
tour_mode_cbm <- tour_mode_cbm %>% 
  mutate(TOUR_MODE_CBM = case_when(
    NUM_TRAV == 1 & LANE_TYPE != 3 ~ 1,
    NUM_TRAV == 1 & LANE_TYPE == 3 ~ 2,
    NUM_TRAV == 2 & LANE_TYPE != 3 ~ 3,
    NUM_TRAV == 2 & LANE_TYPE == 3 ~ 4,
    NUM_TRAV > 2 & LANE_TYPE != 3 ~ 5,
    NUM_TRAV > 2 & LANE_TYPE == 3 ~ 6,
    TOUR_MODE %in% c(3, 4, 5) ~ 7,
    TOUR_MODE %in% 2 ~ 1)) %>%
  select(-TOUR_MODE, -NUM_TRAV, -LANE_TYPE)

# tour mode including Ready Lane
# mode used to cross the border
# LANE_TYPE = 1 (General), 2 (Ready Lane), 3 (SENTRI Lane)
tour_mode_cbm2 <- cb_data[, c('respid', 'q2', 'q3', 'q43')] %>% 
  rename(HH_ID = 'respid', TOUR_MODE = 'q2', NUM_TRAV = 'q3', LANE_TYPE = 'q43')
tour_mode_cbm2 <- tour_mode_cbm2 %>% 
  mutate(TOUR_MODE_CBM2 = case_when(
    NUM_TRAV == 1 & LANE_TYPE == 3 ~ 1,
    NUM_TRAV == 1 & LANE_TYPE == 2 ~ 2,
    NUM_TRAV == 1 & LANE_TYPE == 1 ~ 3,
    NUM_TRAV == 2 & LANE_TYPE == 3 ~ 4,
    NUM_TRAV == 2 & LANE_TYPE == 2 ~ 5,
    NUM_TRAV == 2 & LANE_TYPE == 1 ~ 6,
    NUM_TRAV > 2 & LANE_TYPE == 3 ~ 7,
    NUM_TRAV > 2 & LANE_TYPE == 2 ~ 8,
    NUM_TRAV > 2 & LANE_TYPE == 1 ~ 9,
    TOUR_MODE %in% c(3, 4, 5) ~ 10,
    TOUR_MODE %in% 2 ~ 3)) %>%
  select(-TOUR_MODE, -NUM_TRAV, -LANE_TYPE)

# merge all trip data
keys <- c('HH_ID', 'PLANO')
trips_df <- merge(od_plano, od_coords, by = keys, all = TRUE)
trips_df <- merge(trips_df, od_trip_purp, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, od_trip_purp_cbm, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, od_arr_dep, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, od_trip_dur, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, od_trip_mode, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, od_trip_mode_cbm, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, tottr, by = keys, all.x = TRUE)
trips_df <- merge(trips_df, tour_mode, by = 'HH_ID', all.x = TRUE)
trips_df <- merge(trips_df, tour_mode_cbm, by = 'HH_ID', all.x = TRUE)
trips_df <- merge(trips_df, tour_mode_cbm2, by = 'HH_ID', all.x = TRUE)
trips_df <- merge(trips_df, places_df[, c(keys, 'RETURN_MX')], by = keys, all.x = TRUE)
trips_df <- merge(trips_df, places_df[, c('HH_ID', 'PLANO', 'MGRA', 'TAZ')], 
                  by.x = c('HH_ID', 'ORIG_PLACENO'), by.y = c('HH_ID', 'PLANO'), all.x = TRUE) %>% 
  rename(ORIG_MAZ = 'MGRA', ORIG_TAZ = 'TAZ')
trips_df <- merge(trips_df, places_df[, c('HH_ID', 'PLANO', 'MGRA', 'TAZ')], 
                  by.x = c('HH_ID', 'DEST_PLACENO'), by.y = c('HH_ID', 'PLANO'), all.x = TRUE) %>%
  rename(DEST_MAZ = 'MGRA', DEST_TAZ = 'TAZ')
trips_df <- merge(trips_df, places_df[, c('HH_ID', 'PLANO','SURVEY_DIST')], 
                  by.x = c('HH_ID', 'DEST_PLACENO'), by.y = c('HH_ID', 'PLANO'), all.x = TRUE)
trips_df$TOUR_ID <- 1

# remove loose end records
trips_df <- trips_df %>% drop_na(DEST_PLACENO)

# trip and person id for each household
trips_df <- trips_df %>% group_by(HH_ID) %>% mutate(TRIP_ID = row_number(), PER_ID = 1)

# add travel distance from distance matrix
# between trip origin and destination TAZs
dist_mat_long <- gather_matrix(dist_mat) %>% rename(MAT_DIST = 'value')
trips_df <- merge(trips_df, dist_mat_long, by.x  = c('ORIG_TAZ', 'DEST_TAZ'), 
                  by.y = c('origin', 'destination'), all.x = TRUE)

# create tours files ------------------------------------------------------------------

'%notin%' <- Negate('%in%')

## tour purpose per CBM Specification Report

# determine activity respondent spent most time at
max_purp <- places_df[, c('HH_ID', 'PLACE_DUR', 'TPURP_CBM')]
max_purp <- max_purp %>% group_by(HH_ID, TPURP_CBM) %>% summarise(PURP_DUR = sum(PLACE_DUR, na.rm = TRUE)) %>%
  filter(TPURP_CBM != 0) %>% slice(which.max(PURP_DUR)) %>% rename(MAX_PURP_DUR = 'TPURP_CBM') %>%
  select(-PURP_DUR)

# stated purpose of 9: Pick-up/Drop-off cargo
stated_purp <- cb_data[, c('respid', 'q10')] %>% rename(HH_ID = 'respid', STATED_PURP = 'q10')

# tour purpose
tour_purp <- trips_df[, c('HH_ID', 'DEST_PURP_CBM')]
tour_purp <- merge(tour_purp, max_purp, by = 'HH_ID', all.x = TRUE)
tour_purp <- merge(tour_purp, stated_purp, by = 'HH_ID', all.x = TRUE)
tour_purp <- tour_purp %>% group_by(HH_ID) %>% 
  mutate(TOUR_PURP_CBM = case_when(1 %in% DEST_PURP_CBM ~ 1,
                                   2 %in% DEST_PURP_CBM & 1 %notin% DEST_PURP_CBM ~ 2,
                                   9 %in% STATED_PURP & 1 %notin% DEST_PURP_CBM & 2 %notin% DEST_PURP_CBM ~ 3,
                                   4 %in% DEST_PURP_CBM & 1 %notin% DEST_PURP_CBM & 2 %notin% DEST_PURP_CBM & 3 %notin% DEST_PURP_CBM & 
                                     4 == MAX_PURP_DUR ~ 4,
                                   5 %in% DEST_PURP_CBM & 1 %notin% DEST_PURP_CBM & 2 %notin% DEST_PURP_CBM & 3 %notin% DEST_PURP_CBM &
                                     5 == MAX_PURP_DUR ~ 5,
                                   6 %in% DEST_PURP_CBM & 1 %notin% DEST_PURP_CBM & 2 %notin% DEST_PURP_CBM & 3 %notin% DEST_PURP_CBM &
                                     6 == MAX_PURP_DUR ~ 6)) %>% 
  select(-MAX_PURP_DUR, -DEST_PURP_CBM, -STATED_PURP) %>% slice(1)

# tour purpose without Cargo
tour_purp2 <- data.frame(tour_purp)
tour_purp2 <- tour_purp2 %>% mutate(TOUR_PURP_CBM2 = case_when(TOUR_PURP_CBM == 3 ~ 4, 
                                                               TRUE ~ TOUR_PURP_CBM))
tour_purp2 <- tour_purp2 %>% select(-TOUR_PURP_CBM)

# o/d place numbers
#tour_od_plano <- trips_df[, c('HH_ID', 'ORIG_PLACENO', 'DEST_PLACENO')]
#tour_od_plano <- tour_od_plano %>% group_by(HH_ID) %>% 
#  mutate(ORIG_PLACENO = min(ORIG_PLACENO, na.rm = TRUE), DEST_PLACENO = max(DEST_PLACENO, na.rm = TRUE)) %>%
#  slice(which.min(HH_ID))

# anchor (i.e. origin/departure) locations arrival and departure
tour_arr_dep <- trips_df[, c('HH_ID', 'TRIP_ID', 'ORIG_DEP_HR', 'ORIG_DEP_MIN', 'ORIG_DEP_BIN', 
                             'DEST_ARR_HR', 'DEST_ARR_MIN', 'DEST_ARR_BIN', 'RETURN_MX')]
tour_arr_dep <- tour_arr_dep %>% group_by(HH_ID) %>% arrange(TRIP_ID) %>% filter(row_number() %in% c(1, n()))
tour_arr_dep <- tour_arr_dep %>% group_by(HH_ID) %>% 
  mutate(ANCHOR_DEPART_HR = ORIG_DEP_HR, ANCHOR_DEPART_MIN = ORIG_DEP_MIN, ANCHOR_DEPART_BIN = ORIG_DEP_BIN,
         ANCHOR_ARRIVE_HR = case_when(RETURN_MX == 0 ~ as.character(NA), TRUE ~ lead(DEST_ARR_HR)),
         ANCHOR_ARRIVE_MIN = case_when(RETURN_MX == 0 ~ as.character(NA), TRUE ~ lead(DEST_ARR_MIN)),
         ANCHOR_ARRIVE_BIN = case_when(RETURN_MX == 0 ~ as.integer(NA), TRUE ~ lead(DEST_ARR_BIN))) %>%
  select(-c('TRIP_ID', 'ORIG_DEP_HR', 'ORIG_DEP_MIN', 'ORIG_DEP_BIN', 'DEST_ARR_HR', 'DEST_ARR_MIN', 'DEST_ARR_BIN'))

# tour arriving (i.e. entering USA)
tour_arr <- tour_arr_dep[, c('HH_ID', 'ANCHOR_DEPART_HR', 'ANCHOR_DEPART_MIN', 'ANCHOR_DEPART_BIN')]
tour_arr <- tour_arr %>% slice(which.min(ANCHOR_DEPART_BIN))

# tour departing (i.e. returning to Mexico)
tour_dep <- tour_arr_dep[, c('HH_ID', 'ANCHOR_ARRIVE_HR', 'ANCHOR_ARRIVE_MIN', 'ANCHOR_ARRIVE_BIN')]
tour_dep <- tour_dep %>% drop_na(ANCHOR_ARRIVE_HR)

# tour duration hour and minute
tour_dur <- cb_data[, c('respid', 'USTRIP_DURATION')] %>% rename(HH_ID = 'respid', TOUR_DUR = 'USTRIP_DURATION')
tour_dur <- tour_dur %>% separate(TOUR_DUR, c('TOUR_DUR_HR', 'TOUR_DUR_MIN', 'TOUR_DUR_SEC'), sep = ":") %>% 
  select(-TOUR_DUR_SEC) 

# primary destination arrival and departure
prim_arr_dep <- merge(places_df[, c('HH_ID', 'PLANO', 'PLACE_DUR', 'TPURP_CBM', 'MGRA', 'TAZ')], tour_purp, by = 'HH_ID', all.x = TRUE)
prim_arr_dep <- prim_arr_dep %>% replace_na(list(PLACE_DUR = 0))
prim_arr_dep <- prim_arr_dep %>% filter(TPURP_CBM == TOUR_PURP_CBM) %>% group_by(HH_ID) %>% 
  slice(which.max(PLACE_DUR)) %>% select(-PLACE_DUR, -TPURP_CBM, -TOUR_PURP_CBM)
prim_arr_dep <- merge(prim_arr_dep, places_df[, c('HH_ID', 'PLANO', 'ARR_HR', 'ARR_MIN', 'ARR_BIN', 'DEP_HR', 'DEP_MIN', 'DEP_BIN')]) %>%
  rename(PRIMDEST_ARRIVE_HR = 'ARR_HR', PRIMDEST_ARRIVE_MIN = 'ARR_MIN', PRIMDEST_ARRIVE_BIN = 'ARR_BIN',
         PRIMDEST_DEPART_HR = 'DEP_HR', PRIMDEST_DEPART_MIN = 'DEP_MIN', PRIMDEST_DEPART_BIN = 'DEP_BIN',
         PRIMDEST_MGRA = 'MGRA', PRIMDEST_TAZ = 'TAZ', PRIMDEST_PLANO = 'PLANO')

# merge all tour data
tour_df <- merge(tour_purp, tour_mode, by = 'HH_ID')
tour_df <- merge(tour_df, tour_purp2, by = 'HH_ID')
tour_df <- merge(tour_df, tour_mode_cbm, by = 'HH_ID', all.x = TRUE)
tour_df <- merge(tour_df, tour_mode_cbm2, by = 'HH_ID', all.x = TRUE)
#tour_df <- merge(tour_df, tour_od_plano, by = 'HH_ID', all.x = TRUE)
tour_df <- merge(tour_df, tour_arr, by = 'HH_ID', all.x = TRUE)
tour_df <- merge(tour_df, tour_dep, by = 'HH_ID', all.x = TRUE)
tour_df <- merge(tour_df, tour_dur, by = 'HH_ID', all.x = TRUE)
tour_df <- merge(tour_df, prim_arr_dep, by = 'HH_ID', all.x = TRUE)
tour_df <- tour_df %>% left_join(distinct(places_df[, c('HH_ID', 'RETURN_MX')], HH_ID, .keep_all = T))
tour_df <- tour_df %>% left_join(distinct(places_df[, c('HH_ID', 'TOTTR')], HH_ID, .keep_all = T))

# add anchor (i.e.) TAZ
tour_df <- merge(tour_df, poe_in[, c('HH_ID', 'TAZ')], by = 'HH_ID', all.x = TRUE) %>% 
  rename(ANCHOR_TAZ = 'TAZ')

# add travel distance from distance matrix
# between tour origin (i.e. POE) and primary destination TAZs
tour_df <- merge(tour_df, dist_mat_long, by.x = c('ANCHOR_TAZ', 'PRIMDEST_TAZ'), 
                 by.y = c('origin', 'destination'), all.x = TRUE)

# add tour data to trips file ---------------------------------------------------------

# add tour purpose
# TOUR_PURP_CBM2: No Cargo as tour purpose
trips_df <- merge(trips_df, tour_df[,c('HH_ID', 'TOUR_PURP_CBM', 'TOUR_PURP_CBM2')], by = 'HH_ID', all.x = TRUE)

# calculate number of trips per tour (given 1 tour per respondent)
trips_df <- trips_df %>% group_by(HH_ID) %>% mutate(TRIPS_ON_TOUR = n())

# classify direction of trips relative to primary destination and border (as destination; i.e. last trip on tour)
stop_direction <- merge(trips_df[, c('HH_ID', 'DEST_PLACENO', 'DEST_PURP_CBM')], 
                        tour_df[, c('HH_ID', 'PRIMDEST_PLANO')], by = 'HH_ID', all.x = TRUE)
stop_direction <- stop_direction %>% group_by(HH_ID) %>% 
  mutate(STOP_DIRECTION = case_when(DEST_PLACENO < PRIMDEST_PLANO ~ "Outbound stop",
                               DEST_PLACENO == PRIMDEST_PLANO ~ "Primary stop",
                               DEST_PURP_CBM == 0 ~ "Border return", 
                               DEST_PLACENO > PRIMDEST_PLANO ~ "Inbound stop")) %>%
  select(-DEST_PURP_CBM, -PRIMDEST_PLANO)

trips_df <- merge(trips_df, stop_direction, by = c('HH_ID', 'DEST_PLACENO'), all.x = TRUE)

# order columns
trips_col_order <- c('HH_ID', 'PER_ID', 'TOUR_ID', 'TRIP_ID', 'ORIG_PLACENO', 'ORIG_X', 'ORIG_Y',
                     'ORIG_TAZ', 'ORIG_MAZ', 'DEST_PLACENO', 'DEST_X', 'DEST_Y', 'DEST_TAZ', 'DEST_MAZ', 
                     'MAT_DIST','SURVEY_DIST','ORIG_PURP', 'DEST_PURP', 'ORIG_PURP_CBM', 'DEST_PURP_CBM', 
                     'ORIG_DEP_HR', 'ORIG_DEP_MIN', 'ORIG_DEP_BIN', 'DEST_ARR_HR', 'DEST_ARR_MIN', 
                     'DEST_ARR_BIN', 'TRIP_DUR_HR', 'TRIP_DUR_MIN', 'TRIP_MODE', 'TRIP_MODE_CBM', 'TOUR_MODE', 
                     'TOUR_MODE_CBM', 'TOUR_MODE_CBM2', 'TOUR_PURP_CBM', 'TOUR_PURP_CBM2', 'TRIPS_ON_TOUR', 
                     'RETURN_MX', 'STOP_DIRECTION', 'TOTTR')
trips_df <- trips_df[, trips_col_order]
trips_df <- trips_df %>% arrange(HH_ID, TRIP_ID)

# add trips data to tour file ---------------------------------------------------------

# number of trips per tour
tour_df <- tour_df %>% left_join(distinct(trips_df[, c('HH_ID', 'TRIPS_ON_TOUR')], HH_ID, .keep_all = T))

# number of inbound and outbound trips
in_out_stops <- merge(trips_df[, c('HH_ID', 'DEST_PLACENO', 'DEST_PURP_CBM')], 
                      tour_df[, c('HH_ID', 'PRIMDEST_PLANO')], by = 'HH_ID', all.x = TRUE)
in_out_stops <- in_out_stops %>% group_by(HH_ID) %>% filter(DEST_PURP_CBM > 0) %>%
  summarise(IN_STOPS = sum(DEST_PLACENO > PRIMDEST_PLANO), OUT_STOPS = sum(DEST_PLACENO < PRIMDEST_PLANO))
tour_df <- merge(tour_df, in_out_stops, by = 'HH_ID', all.x = TRUE) 

# print trips and tour files ----------------------------------------------------------

if (csv_out) {
  write_csv(trips_df, 'output/trips.csv')
  write_csv(tour_df, 'output/tours.csv')
}

# survey data summary setup -----------------------------------------------------------

## summary attribute function (for obtaining variable labels form the original CB survey data)
# surv_att_name: attribute name from crossborder survey data
# str_att_name: name of new, stringed version of surv_att_name
# str_att_label: label for str_att_name
addSumAtt <- function(data, surv_att_name, str_att_name, str_att_label) {
  new_label <- cb_data_dictionary_values[cb_data_dictionary_values$Value==as.character(surv_att_name), c('Range', 'Label')]
  new_label <- new_label %>% rename_(.dots = setNames('Label',str_att_name))

  data <- merge(data, new_label, by.x = as.character(surv_att_name), by.y = 'Range', all.x = TRUE)

  var_lab(data[, as.character(str_att_name)]) <- as.character(str_att_label)
  
  return(data)
}

cb_data <- addSumAtt(cb_data, 'MONTHLYINCOME', 'monthly_income_str', 'Monthly income')
cb_data <- addSumAtt(cb_data, 'PRIMARYPURPOSE', 'primary_purpose_str', 'Stated primary purpose')
cb_data <- addSumAtt(cb_data, 'q43', 'lane_type_str', 'Lane type')
cb_data <- addSumAtt(cb_data, 'POE', 'poe_str', 'Port of entry')
cb_data <- addSumAtt(cb_data, 'q23', 'EMP_STATUS_STR', 'Employment status')

## create variable labels 

# tour purpose cbm (per CBM Specification Report variables)
tour_purp_cbm_lab <- data.frame(TOUR_PURP_CBM = c(1,2,3,4,5,6),
                               TOUR_PURP_CBM_STR = c(
                                 'Work', 'School', 'Cargo', 'Shop', 'Visit', 'Other'
                               ))

# tour purpose cbm without Cargo
tour_purp_cbm2_lab <- data.frame(TOUR_PURP_CBM2 = c(1,2,4,5,6),
                                TOUR_PURP_CBM2_STR = c(
                                  'Work', 'School', 'Shop', 'Visit', 'Other'
                                ))

# tour mode cbm (per CBM Specification Report variables)
tour_mode_cbm_lab <- data.frame(TOUR_MODE_CBM = c(1,2,3,4,5,6,7),
                                TOUR_MODE_CBM_STR = c(
                                  'Drive-Alone Non-SENTRI', 'Drive-Alone SENTRI', 'Shared-Ride 2 Non-SENTRI',
                                  'Shared-Ride 2 SENTRI', 'Shared-Ride 3+ Non-SENTRI', 'Shared-Ride 3+ SENTRI',
                                  'Walk/Bike'
                                ))

# tour mode cbm 2 (includes Ready Lane)
tour_mode_cbm2_lab <- data.frame(TOUR_MODE_CBM2 = c(1,2,3,4,5,6,7,8,9,10),
                                 TOUR_MODE_CBM2_STR = c(
                                   'Drive-Alone SENTRI', 'Drive-Alone Ready', 'Drive-Alone General',
                                   'Shared-Ride 2 SENTRI', 'Shared-Ride 2 Ready', 'Shared-Ride 2 General',
                                   'Shared-Ride 3+ SENTRI', 'Shared-Ride 3+ Ready', 'Shared-Ride 3+ General',
                                   'Walk/Bike'
                                 ))

# trip mode cbm (per CBM Specification Report variables)
trip_mode_cbm <- data.frame(TRIP_MODE_CBM = c(1,2,3,4,5,6,7),
                            TRIP_MODE_CBM_STR = c(
                              'Drive-Alone', 'Shared-Ride 2', 'Shared-Ride 3+', 'Walk', 'Walk-Bus', 'Walk-Rail', 'Other'
                            ))

# trip purpose cbm at destination (per CBM Specification Report)
trip_purp_cbm <- data.frame(DEST_PURP_CBM = c(0,1,2,3,4,5,6),
                            DEST_PURP_CBM_STR = c(
                              'Border', 'Work', 'School', 'Cargo', 'Shop', 'Visit', 'Other'
                            ))

# column and row orders for summary tables
trip_purp_order <- c('Trip Purpose|Border', 'Trip Purpose|Work', 'Trip Purpose|School', 'Trip Purpose|Cargo', 
                     'Trip Purpose|Shop', 'Trip Purpose|Visit', 'Trip Purpose|Other', 'Trip Purpose|#Total')

trip_mode_order <- c('Trip Mode|Drive-Alone', 'Trip Mode|Shared-Ride 2', 'Trip Mode|Shared-Ride 3+', 'Trip Mode|Walk', 
                     'Trip Mode|Walk-Bus', 'Trip Mode|Walk-Rail', 'Trip Mode|Other', 'Trip Mode|#Total')

trip_mode_order_col <- c('row_labels', 'Trip Mode|Drive-Alone', 'Trip Mode|Shared-Ride 2', 'Trip Mode|Shared-Ride 3+', 
                         'Trip Mode|Walk', 'Trip Mode|Walk-Bus', 'Trip Mode|Walk-Rail', 'Trip Mode|Other')

poe_order <- c("row_labels", "Port of entry|San Ysidro", "Port of entry|Otay Mesa", "Port of entry|Tecate")

tour_purp_order <- c('Tour Purpose|Work', 'Tour Purpose|School', 'Tour Purpose|Cargo', 'Tour Purpose|Shop', 
                     'Tour Purpose|Visit', 'Tour Purpose|Other', 'Tour Purpose|#Total')

tour_purp_order_col <- c('row_labels' ,'Tour Purpose|Work', 'Tour Purpose|School', 'Tour Purpose|Cargo', 
                         'Tour Purpose|Shop', 'Tour Purpose|Visit', 'Tour Purpose|Other')

tour_purp2_order <- c('Tour Purpose|Work', 'Tour Purpose|School', 'Tour Purpose|Shop', 
                     'Tour Purpose|Visit', 'Tour Purpose|Other', 'Tour Purpose|#Total')

tour_purp2_order_col <- c('row_labels' ,'Tour Purpose|Work', 'Tour Purpose|School', 
                         'Tour Purpose|Shop', 'Tour Purpose|Visit', 'Tour Purpose|Other')

emp_status_order <- c('Employment status|Employed full-time: at least 35 hours per week', 
                      'Employment status|Employed part-time: at least 35 hours per week', 
                      'Employment status|College Student',
                      'Employment status|High School Student', 'Employment status|Not employed, but looking for work', 
                      'Employment status|Retired, homemaker, not employed', 'Employment status|Prefer not to answer', 
                      'Employment status|#Total')

## add variable labels and category names

# tour df
tour_df <- merge(tour_df, tour_purp_cbm_lab, by = 'TOUR_PURP_CBM', all.x = TRUE)
tour_df <- merge(tour_df, tour_purp_cbm2_lab, by = 'TOUR_PURP_CBM2', all.x = TRUE)
tour_df <- merge(tour_df, tour_mode_cbm_lab, by = 'TOUR_MODE_CBM', all.x = TRUE)
tour_df <- merge(tour_df, tour_mode_cbm2_lab, by = 'TOUR_MODE_CBM2', all.x = TRUE)
tour_df <- merge(tour_df, cb_data[, c('respid', 'poe_str')], by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
tour_df <- apply_labels(tour_df,
                        TOUR_PURP_CBM_STR = 'Tour Purpose',
                        TOUR_PURP_CBM2_STR = 'Tour Purpose',
                        TOUR_MODE_CBM_STR = 'Tour Mode',
                        TOUR_MODE_CBM2_STR = 'Tour Mode (including Ready Lane)'
                        )

# trips df
trips_df <- merge(trips_df, trip_mode_cbm, by  = 'TRIP_MODE_CBM', all.x = TRUE)
trips_df <- merge(trips_df, tour_purp_cbm_lab, by = 'TOUR_PURP_CBM', all.x = TRUE)
trips_df <- merge(trips_df, tour_purp_cbm2_lab, by = 'TOUR_PURP_CBM2', all.x = TRUE)
trips_df <- merge(trips_df, trip_purp_cbm, by = 'DEST_PURP_CBM', all.x = TRUE)
trips_df <- apply_labels(trips_df,
                         TRIP_MODE_CBM_STR = 'Trip Mode',
                         TOUR_PURP_CBM_STR = 'Tour Purpose',
                         TOUR_PURP_CBM2_STR = 'Tour Purpose',
                         DEST_PURP_CBM_STR = 'Trip Purpose'
)

# add weights from CB survey
tour_df = merge(tour_df, cb_data[, c('respid', 'DIARYWEIGHT')], by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
trips_df = merge(trips_df, cb_data[, c('respid', 'DIARYWEIGHT')], by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)

# print out summaries and plots -------------------------------------------------------

if (summaries) {

# create expansion factor categories --------------------------------------------------

  # add expansion factor categories to tour file
  tour_df <- tour_df %>% 
    mutate(EXP_CAT = case_when(
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "SENTRI") ~ 1,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "Ready") ~ 2,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 3,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 4,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "SENTRI") ~ 5,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "Ready") ~ 6,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 7,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 8,
      str_detect(poe_str, "Tecate") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 9,
      str_detect(poe_str, "Tecate") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 10
    ))
  
  # add expansion factor categories to trips file
  trips_df <- merge(trips_df, tour_df[, c('HH_ID', 'poe_str', "TOUR_MODE_CBM2_STR")], by = 'HH_ID', all.x = TRUE)
  
  trips_df <- trips_df %>% 
    mutate(EXP_CAT = case_when(
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "SENTRI") ~ 1,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "Ready") ~ 2,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 3,
      str_detect(poe_str, "San Ysidro") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 4,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "SENTRI") ~ 5,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "Ready") ~ 6,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 7,
      str_detect(poe_str, "Otay Mesa") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 8,
      str_detect(poe_str, "Tecate") & str_detect(TOUR_MODE_CBM2_STR, "General") ~ 9,
      str_detect(poe_str, "Tecate") & str_detect(TOUR_MODE_CBM2_STR, "Walk") ~ 10
    ))
  
  # add average daily crossing volumes to tour file
  tour_df <- merge(tour_df, adcv, by = 'EXP_CAT', all.x = TRUE)
  
  # apply expansion factor to tour file
  tour_df <- tour_df %>% group_by(EXP_CAT) %>% 
    mutate(EXP_CAT_SURV_VOL = sum(TOTTR*DIARYWEIGHT))
  tour_df <- tour_df %>% group_by(EXP_CAT) %>%
    mutate(EXP_WEIGHT = (TARGET_MX/EXP_CAT_SURV_VOL))
  tour_df <- tour_df %>% mutate(EXP_WEIGHT_TOTTR = EXP_WEIGHT*TOTTR*DIARYWEIGHT)
  
  # expansion factors
  exp_factors <- data.frame(tour_df[, c('EXP_CAT', 'EXP_WEIGHT')])
  exp_factors <- unique(exp_factors[,c('EXP_CAT','EXP_WEIGHT')])
  
  # add tour expansion factors to trips file
  trips_df <- merge(trips_df, exp_factors, by = 'EXP_CAT', all.x = TRUE)
  trips_df <- trips_df %>% mutate(EXP_WEIGHT_TOTTR = EXP_WEIGHT*TOTTR)

# CBM summaries -----------------------------------------------------------------------
  
  # create summaries output directory (relative to current working directory)
  if (!dir.exists('output/summaries')) {
    dir.create(file.path(getwd(), 'output/summaries'))
  }

## ------------------------------------------------------------------------------------
  
  ### Table 1 from CBM Specification Report: tours by purpose
  
  # raw survey data
  table1A_raw <- cro(tour_df$TOUR_PURP_CBM_STR, weight = tour_df$DIARYWEIGHT, total_statistic = 'w_cases', total_label = "Total")
  table1B_raw <- cro(trips_df$TOUR_PURP_CBM_STR, weight = trips_df$DIARYWEIGHT, total_statistic = 'w_cases', total_label = "Total")
  table1_raw <- table1A_raw %merge% table1B_raw
  colnames(table1_raw)[2] <- 'Number of Tours (Raw)'
  colnames(table1_raw)[3] <- 'Number of Trips on Tour (Raw)'
  
  # raw survey data as percentage
  table1A_raw_pct <- cro_cpct(tour_df$TOUR_PURP_CBM_STR, weight = tour_df$DIARYWEIGHT, total_statistic = 'w_cpct', total_label = "Total")
  table1B_raw_pct <- cro_cpct(trips_df$TOUR_PURP_CBM_STR, weight = trips_df$DIARYWEIGHT, total_statistic = 'w_cpct', total_label = "Total")
  table1_raw_pct <- table1A_raw_pct %merge% table1B_raw_pct
  colnames(table1_raw_pct)[2] <- 'Number of Tours (Raw)'
  colnames(table1_raw_pct)[3] <- 'Number of Trips on Tour (Raw)'
  
  # expanded survey data
  table1A_exp <- cro(tour_df$TOUR_PURP_CBM_STR, weight = tour_df$EXP_WEIGHT_TOTTR, total_statistic = 'w_cases', total_label = "Total")
  table1B_exp <- cro(trips_df$TOUR_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, total_statistic = 'w_cases', total_label = "Total")
  table1_exp <- table1A_exp %merge% table1B_exp
  colnames(table1_exp)[2] <- 'Number of Tours (Expanded)'
  colnames(table1_exp)[3] <- 'Number of Trips on Tour (Expanded)'
  
  # expanded survey data as percentage
  table1A_exp_pct <- cro_cpct(tour_df$TOUR_PURP_CBM_STR, weight = tour_df$EXP_WEIGHT_TOTTR, total_statistic = 'w_cpct', total_label = "Total")
  table1B_exp_pct <- cro_cpct(trips_df$TOUR_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, total_statistic = 'w_cpct', total_label = "Total")
  table1_exp_pct <- table1A_exp_pct %merge% table1B_exp_pct
  colnames(table1_exp_pct)[2] <- 'Number of Tours (Expanded)'
  colnames(table1_exp_pct)[3] <- 'Number of Trips on Tour (Expanded)'
  
  # merge table 1
  table1 <- table1_raw %merge% table1_exp
  table1_pct <- table1_raw_pct %merge% table1_exp_pct
  
  # fix row order
  table1 <- table1 %>% slice(match(tour_purp_order, row_labels))
  table1_pct <- table1_pct %>% slice(match(tour_purp_order, row_labels))
  
## ------------------------------------------------------------------------------------
  
  ### Table 2 from CBM Specification Report: tour mode per POE
  
  # raw survey data
  table2_raw <- cro(tour_df$TOUR_MODE_CBM_STR, tour_df$poe_str, weight = tour_df$DIARYWEIGHT, 
                    total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  table2_exp <- cro(tour_df$TOUR_MODE_CBM_STR, tour_df$poe_str, weight = tour_df$EXP_WEIGHT_TOTTR,
                    total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  table2_exp_pct <- cro_cpct(tour_df$TOUR_MODE_CBM_STR, tour_df$poe_str, weight = tour_df$EXP_WEIGHT_TOTTR,
                    total_statistic = 'w_cpct', total_label = "Total")
  
  # fix column order
  table2_exp <- table2_exp[, poe_order]
  table2_exp_pct <- table2_exp_pct[, poe_order]
  table2_raw <- table2_raw[, poe_order]
  
## ------------------------------------------------------------------------------------  
  
  ### Table 2B: tour mode per POE including Ready Lane
  
  # raw survey data
  table2B_raw <- cro(tour_df$TOUR_MODE_CBM2_STR, tour_df$poe_str, weight = tour_df$DIARYWEIGHT, 
                     total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  table2B_exp <- cro(tour_df$TOUR_MODE_CBM2_STR, tour_df$poe_str, weight = tour_df$EXP_WEIGHT_TOTTR,
                   total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  table2B_exp_pct <- cro_cpct(tour_df$TOUR_MODE_CBM2_STR, tour_df$poe_str, weight = tour_df$EXP_WEIGHT_TOTTR,
                   total_statistic = 'w_cpct', total_label = "Total")
  
  # fix column order
  table2B_exp <- table2B_exp[, poe_order]
  table2B_exp_pct <- table2B_exp_pct[, poe_order]
  table2B_raw <- table2B_raw[, poe_order]
  
## ------------------------------------------------------------------------------------
  
  ### Table 3 from CBM Specification Report: trips by tour purpose and trip mode
  
  # raw survey data
  table3_raw <- cro(trips_df$TRIP_MODE_CBM_STR, trips_df$TOUR_PURP_CBM_STR, weight = trips_df$DIARYWEIGHT, 
                    total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  table3_exp <- cro(trips_df$TRIP_MODE_CBM_STR, trips_df$TOUR_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                    total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  table3_exp_pct <- cro_cpct(trips_df$TRIP_MODE_CBM_STR, trips_df$TOUR_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                    total_statistic = 'w_cpct', total_label = "Total")
  
  # fix row order
  table3_exp <- table3_exp %>% slice(match(trip_mode_order, row_labels))
  table3_exp_pct <- table3_exp_pct %>% slice(match(trip_mode_order, row_labels))
  table3_raw <- table3_raw %>% slice(match(trip_mode_order, row_labels))

  # fix column order
  table3_exp <- table3_exp[, tour_purp_order_col]
  table3_exp_pct <- table3_exp_pct[, tour_purp_order_col]
  table3_raw <- table3_raw[, tour_purp_order_col]
   
## ------------------------------------------------------------------------------------  
   
  ### party size
  
  # add number of travelers (label) to tour df
  var_lab(tour_df$TOTTR) <- 'Total travelers'
  
  # raw survey data
  tottr_raw <- cro(tour_df$TOTTR, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$DIARYWEIGHT, 
                   total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  tottr_exp <- cro(tour_df$TOTTR, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR,
                   total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  tottr_pct <- cro_cpct(tour_df$TOTTR, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR,
                        total_statistic = 'w_cpct', total_label = "Total")
  
  # fix column order
  tottr_exp <- tottr_exp[, tour_purp2_order_col]
  tottr_pct <- tottr_pct[, tour_purp2_order_col]
  tottr_raw <- tottr_raw[, tour_purp2_order_col]
      
## ------------------------------------------------------------------------------------
  
  ### party size by trip mode. may not need this anymore.
  
  # # add number of travelers (label) to trips df
  # var_lab(trips_df$TOTTR) <- 'Total travelers'
  # 
  # # raw survey data
  # tottr_trip_mode_raw <- cro(trips_df$TOTTR, trips_df$TRIP_MODE_CBM_STR, weight = trips_df$DIARYWEIGHT, 
  #                  total_statistic = 'w_cases', total_label = "Total")
  # 
  # # expanded survey data
  # tottr_trip_mode_exp <- cro(trips_df$TOTTR, trips_df$TRIP_MODE_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR,
  #                  total_statistic = 'w_cases', total_label = "Total")
  # 
  # # expanded survey data as percentage
  # tottr_trip_mode_exp_pct <- cro_cpct(trips_df$TOTTR, trips_df$TRIP_MODE_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR,
  #                       total_statistic = 'w_cpct', total_label = "Total")
  # 
  # # fix column order
  # tottr_trip_mode_raw <- tottr_trip_mode_raw[, trip_mode_order_col]
  # tottr_trip_mode_exp <- tottr_trip_mode_exp[, trip_mode_order_col]
  # tottr_trip_mode_exp_pct <- tottr_trip_mode_exp_pct[, trip_mode_order_col]
    
## ------------------------------------------------------------------------------------
  
  ### tour purpose by employment status. may not need this anymore.
  
  # # add employment status to tour df
  # tour_df <- merge(tour_df, cb_data[, c('respid', 'EMP_STATUS_STR')], by.x = 'HH_ID', by.y = 'respid', all.x = TRUE)
  # 
  # # raw survey data
  # emp_tour_raw <- cro(tour_df$EMP_STATUS_STR, tour_df$TOUR_PURP_CBM_STR, weight = tour_df$DIARYWEIGHT, 
  #                     total_statistic = 'w_cases', total_label = "Total")
  # 
  # # expanded survey data
  # emp_tour_exp <- cro(tour_df$EMP_STATUS_STR, tour_df$TOUR_PURP_CBM_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
  #                     total_statistic = 'w_cases', total_label = "Total")
  # 
  # # expanded survey data as percentage
  # emp_tour_pct <- cro_cpct(tour_df$EMP_STATUS_STR, tour_df$TOUR_PURP_CBM_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
  #                          total_statistic = 'w_cpct', total_label = "Total")
  # 
  # # fix column order
  # emp_tour_raw <- emp_tour_raw[, tour_purp2_order_col]
  # emp_tour_exp <- emp_tour_exp[, tour_purp2_order_col]
  # emp_tour_pct <- emp_tour_pct[, tour_purp2_order_col]
  # 
  # # fix row order
  # emp_tour_raw <- emp_tour_raw %>% slice(match(emp_status_order, row_labels))
  # emp_tour_exp <- emp_tour_exp %>% slice(match(emp_status_order, row_labels))
  # emp_tour_pct <- emp_tour_pct %>% slice(match(emp_status_order, row_labels))
      
## ------------------------------------------------------------------------------------
  
  ### TOD arrival and departure
  
  # determine number of arrivals and departures by TOD
  tod <- data.frame(TOD = seq(0, 23, 1))
  
  # raw survey data
  # anchor depart hr: hr leaving border after having crossed into USA
  # anchor arrive hr: hr arriving to border before having crossed back to Mexico
  tod_arr_raw <- tour_df %>% group_by(ANCHOR_DEPART_HR) %>% 
    tally(name = 'ARRIVING', wt = DIARYWEIGHT) %>% rename(TOD = 'ANCHOR_DEPART_HR')
  tod_dep_raw <- tour_df %>% group_by(ANCHOR_ARRIVE_HR) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% rename(TOD = 'ANCHOR_ARRIVE_HR')
  tod_arr_dep_raw <- merge(tod, tod_arr_raw, by = 'TOD', all.x = TRUE)
  tod_arr_dep_raw  <- merge(tod_arr_dep_raw , tod_dep_raw, by = 'TOD', all.x = TRUE)
  tod_arr_dep_raw  <- tod_arr_dep_raw  %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  
  # expanded survey data
  tod_arr_exp <- tour_df %>% group_by(ANCHOR_DEPART_HR) %>% 
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>% rename(TOD = 'ANCHOR_DEPART_HR')
  tod_dep_exp <- tour_df %>% group_by(ANCHOR_ARRIVE_HR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% rename(TOD = 'ANCHOR_ARRIVE_HR')
  tod_arr_dep_exp <- merge(tod, tod_arr_exp, by = 'TOD', all.x = TRUE)
  tod_arr_dep_exp <- merge(tod_arr_dep_exp, tod_dep_exp, by = 'TOD', all.x = TRUE)
  tod_arr_dep_exp <- tod_arr_dep_exp %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  
  # expanded survey data as percentage
  tod_arr_exp_pct <- tour_df %>% group_by(ANCHOR_DEPART_HR) %>% 
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>%
    mutate(ARRIVING_PCT = ARRIVING / sum(ARRIVING)) %>%
    rename(TOD = 'ANCHOR_DEPART_HR') %>% select(-ARRIVING)
  tod_dep_exp_pct <- tour_df %>% group_by(ANCHOR_ARRIVE_HR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% 
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>%
    rename(TOD = 'ANCHOR_ARRIVE_HR') %>% select(-DEPARTING)
  tod_arr_dep_exp_pct <- merge(tod, tod_arr_exp_pct, by = 'TOD', all.x = TRUE)
  tod_arr_dep_exp_pct <- merge(tod_arr_dep_exp_pct, tod_dep_exp_pct, by = 'TOD', all.x = TRUE)
  tod_arr_dep_exp_pct <- tod_arr_dep_exp_pct %>% replace_na(list(ARRIVING_PCT = 0, DEPARTING_PCT = 0))
    
## ------------------------------------------------------------------------------------
  
  ### arrival and departure by ABM time bins
  
  # determine number of arrivals and departures by ABM time bins
  abm_time_bins <- data.frame(TIME_BIN = seq(1, 48, 1))
  
  # raw survey data
  # anchor depart bin: time bin leaving border after having crossed into USA
  # anchor arrive bin: time bin arriving to border before having crossed back to Mexico
  bin_arr_raw <- tour_df %>% group_by(ANCHOR_DEPART_BIN) %>%
    tally(name = 'ARRIVING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_raw <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_raw <- merge(abm_time_bins, bin_arr_raw, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_raw <- merge(bin_arr_dep_raw, bin_dep_raw, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_raw <- bin_arr_dep_raw %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  
  # expanded survey data
  bin_arr_exp <- tour_df %>% group_by(ANCHOR_DEPART_BIN) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_exp <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_exp <- merge(abm_time_bins, bin_arr_exp, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_exp <- merge(bin_arr_dep_exp, bin_dep_exp, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_exp <- bin_arr_dep_exp %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  
  # expanded survey data as percentage
  bin_arr_exp_pct <- tour_df %>% group_by(ANCHOR_DEPART_BIN) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>%
    mutate(ARRIVING_PCT = ARRIVING / sum(ARRIVING)) %>%
    rename(TIME_BIN = 'ANCHOR_DEPART_BIN') %>% select(-ARRIVING)
  bin_dep_exp_pct <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>%
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>%
    rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN') %>% select(-DEPARTING)
  bin_arr_dep_exp_pct <- merge(abm_time_bins, bin_arr_exp_pct, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_exp_pct <- merge(bin_arr_dep_exp_pct, bin_dep_exp_pct, by = 'TIME_BIN', all.x = TRUE)
  bin_arr_dep_exp_pct <- bin_arr_dep_exp_pct %>% replace_na(list(ARRIVING_PCT = 0, DEPARTING_PCT = 0))
  
## ------------------------------------------------------------------------------------
  
  ### tour arrival and departure by ABM time bins and tour purpose
  
  # determine number of arrivals and departures by ABM time bins
  abm_time_bins <- data.frame(TIME_BIN = seq(1, 48, 1))
  abm_time_bins_purp <- abm_time_bins %>% group_by(TIME_BIN) %>% 
    expand(TOUR_PURP_CBM2_STR = unique(tour_df$TOUR_PURP_CBM2_STR))
  
  # raw survey data
  # anchor depart bin: time bin leaving border after having crossed into USA
  # anchor arrive bin: time bin arriving to border before having crossed back to Mexico
  bin_arr_purp_raw <- tour_df %>% group_by(ANCHOR_DEPART_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'ARRIVING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_purp_raw <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_purp_raw <- merge(abm_time_bins_purp, bin_arr_purp_raw, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_raw <- merge(bin_arr_dep_purp_raw, bin_dep_purp_raw, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_raw <- bin_arr_dep_purp_raw %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  bin_arr_dep_purp_raw <- arrange(bin_arr_dep_purp_raw, desc(TOUR_PURP_CBM2_STR), group_by = TIME_BIN)
  
  # expanded survey data
  bin_arr_purp_exp <- tour_df %>% group_by(ANCHOR_DEPART_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_purp_exp <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_purp_exp <- merge(abm_time_bins_purp, bin_arr_purp_exp, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_exp <- merge(bin_arr_dep_purp_exp, bin_dep_purp_exp, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_exp <- bin_arr_dep_purp_exp %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  bin_arr_dep_purp_exp <- arrange(bin_arr_dep_purp_exp, desc(TOUR_PURP_CBM2_STR), group_by = TIME_BIN)
  
  # expanded survey data as percentage
  bin_arr_purp_exp_pct <- tour_df %>% group_by(ANCHOR_DEPART_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR)
  bin_arr_purp_exp_pct <- bin_arr_purp_exp_pct %>% group_by(TOUR_PURP_CBM2_STR) %>% 
    mutate(ARRIVING_PCT = ARRIVING / sum(ARRIVING)) %>%
    rename(TIME_BIN = 'ANCHOR_DEPART_BIN') %>% select(-ARRIVING)
  bin_dep_purp_exp_pct <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, TOUR_PURP_CBM2_STR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR)
  bin_dep_purp_exp_pct <- bin_dep_purp_exp_pct %>% filter(!is.na(ANCHOR_ARRIVE_BIN)) %>%
    group_by(TOUR_PURP_CBM2_STR) %>% 
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>%
    rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN') %>% select(-DEPARTING)
  bin_arr_dep_purp_exp_pct <- merge(abm_time_bins_purp, bin_arr_purp_exp_pct, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_exp_pct <- merge(bin_arr_dep_purp_exp_pct, bin_dep_purp_exp_pct, 
                                by = c('TIME_BIN', 'TOUR_PURP_CBM2_STR'), all.x = TRUE)
  bin_arr_dep_purp_exp_pct <- bin_arr_dep_purp_exp_pct %>% replace_na(list(ARRIVING_PCT = 0, DEPARTING_PCT = 0))
  bin_arr_dep_purp_exp_pct <- arrange(bin_arr_dep_purp_exp_pct, desc(TOUR_PURP_CBM2_STR), group_by = TIME_BIN)
  
## ------------------------------------------------------------------------------------
  
  ### tour arrival and departure by ABM time bins and port of entry
  
  # determine number of arrivals and departures by ABM time bins
  abm_time_bins <- data.frame(TIME_BIN = seq(1, 48, 1))
  abm_time_bins_poe <- abm_time_bins %>% group_by(TIME_BIN) %>% 
    expand(poe_str = unique(tour_df$poe_str))
  
  # raw survey data
  # anchor depart bin: time bin leaving border after having crossed into USA
  # anchor arrive bin: time bin arriving to border before having crossed back to Mexico
  bin_arr_poe_raw <- tour_df %>% group_by(ANCHOR_DEPART_BIN, poe_str) %>%
    tally(name = 'ARRIVING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_poe_raw <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, poe_str) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_poe_raw <- merge(abm_time_bins_poe, bin_arr_poe_raw, 
                                by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_raw <- merge(bin_arr_dep_poe_raw, bin_dep_poe_raw, 
                                by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_raw <- bin_arr_dep_poe_raw %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  bin_arr_dep_poe_raw <- arrange(bin_arr_dep_poe_raw, desc(poe_str), group_by = TIME_BIN)
  
  # expanded survey data
  bin_arr_poe_exp <- tour_df %>% group_by(ANCHOR_DEPART_BIN, poe_str) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_DEPART_BIN')
  bin_dep_poe_exp <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, poe_str) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN')
  bin_arr_dep_poe_exp <- merge(abm_time_bins_poe, bin_arr_poe_exp, 
                                by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_exp <- merge(bin_arr_dep_poe_exp, bin_dep_poe_exp, 
                                by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_exp <- bin_arr_dep_poe_exp %>% replace_na(list(ARRIVING = 0, DEPARTING = 0))
  bin_arr_dep_poe_exp <- arrange(bin_arr_dep_poe_exp, desc(poe_str), group_by = TIME_BIN)
  
  # expanded survey data as percentage
  bin_arr_poe_exp_pct <- tour_df %>% group_by(ANCHOR_DEPART_BIN, poe_str) %>%
    tally(name = 'ARRIVING', wt = EXP_WEIGHT_TOTTR)
  bin_arr_poe_exp_pct <- bin_arr_poe_exp_pct %>% group_by(poe_str) %>% 
    mutate(ARRIVING_PCT = ARRIVING / sum(ARRIVING)) %>%
    rename(TIME_BIN = 'ANCHOR_DEPART_BIN') %>% select(-ARRIVING)
  bin_dep_poe_exp_pct <- tour_df %>% group_by(ANCHOR_ARRIVE_BIN, poe_str) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR)
  bin_dep_poe_exp_pct <- bin_dep_poe_exp_pct %>% filter(!is.na(ANCHOR_ARRIVE_BIN)) %>%
    group_by(poe_str) %>% 
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>%
    rename(TIME_BIN = 'ANCHOR_ARRIVE_BIN') %>% select(-DEPARTING)
  bin_arr_dep_poe_exp_pct <- merge(abm_time_bins_poe, bin_arr_poe_exp_pct, 
                                    by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_exp_pct <- merge(bin_arr_dep_poe_exp_pct, bin_dep_poe_exp_pct, 
                                    by = c('TIME_BIN', 'poe_str'), all.x = TRUE)
  bin_arr_dep_poe_exp_pct <- bin_arr_dep_poe_exp_pct %>% replace_na(list(ARRIVING_PCT = 0, DEPARTING_PCT = 0))
  bin_arr_dep_poe_exp_pct <- arrange(bin_arr_dep_poe_exp_pct, desc(poe_str), group_by = TIME_BIN)
  
## ------------------------------------------------------------------------------------
  
  ### inbound and outbound stop frequency
  
  # add label to in and out stops attributes
  var_lab(tour_df$IN_STOPS) <- "Inbound stops"
  var_lab(tour_df$OUT_STOPS) <- "Outbound stops"
  
  # raw survey data
  in_raw <- cro(tour_df$IN_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$DIARYWEIGHT, 
                total_statistic = 'w_cases', total_label = "Total")
  out_raw <- cro(tour_df$OUT_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$DIARYWEIGHT, 
                 total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  in_exp <- cro(tour_df$IN_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
                total_statistic = 'w_cases', total_label = "Total")
  out_exp <- cro(tour_df$OUT_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
                 total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  in_exp_pct <- cro_cpct(tour_df$IN_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
                         total_statistic = 'w_cpct', total_label = "Total")
  out_exp_pct <- cro_cpct(tour_df$OUT_STOPS, tour_df$TOUR_PURP_CBM2_STR, weight = tour_df$EXP_WEIGHT_TOTTR, 
                 total_statistic = 'w_cpct', total_label = "Total")
  
  # fix column order
  in_raw <- in_raw[, tour_purp2_order_col]
  out_raw <- out_raw[, tour_purp2_order_col]
  in_exp <- in_exp[, tour_purp2_order_col]
  out_exp <- out_exp[, tour_purp2_order_col]
  in_exp_pct <- in_exp_pct[, tour_purp2_order_col]
  out_exp_pct <- out_exp_pct[, tour_purp2_order_col]
  
## ------------------------------------------------------------------------------------  
  
  ### number of stops per trip purpose
  
  # raw survey data
  num_trips_purp_raw <- cro(trips_df$DEST_PURP_CBM_STR, weight = trips_df$DIARYWEIGHT, 
                            total_statistic = 'w_cases', total_label = "Total")
  colnames(num_trips_purp_raw)[2] <- 'Number of Trips (Raw)'
  
  # expanded suvey data
  num_trips_purp_exp <- cro(trips_df$DEST_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                            total_statistic = 'w_cases', total_label = "Total")
  colnames(num_trips_purp_exp)[2] <- 'Number of Trips (Expanded)'
  
  # raw survey data as percentage
  num_trips_purp_raw_pct <- cro_cpct(trips_df$DEST_PURP_CBM_STR, weight = trips_df$DIARYWEIGHT, 
                                     total_statistic = 'w_cpct', total_label = "Total")
  colnames(num_trips_purp_raw_pct)[2] <- 'Number of Trips (Raw)'
  
  # expanded suvey data as percentage
  num_trips_purp_exp_pct <- cro_cpct(trips_df$DEST_PURP_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                                     total_statistic = 'w_cpct', total_label = "Total")
  colnames(num_trips_purp_exp_pct)[2] <- 'Number of Trips (Expanded)'
  
  num_trips <- num_trips_purp_raw %merge% num_trips_purp_exp
  num_trips_pct <- num_trips_purp_raw_pct %merge% num_trips_purp_exp_pct
  
  # fix row order
  num_trips <- num_trips %>% slice(match(trip_purp_order, row_labels))
  num_trips_pct <- num_trips_pct %>% slice(match(trip_purp_order, row_labels))
  
## ------------------------------------------------------------------------------------  
  
  ### TOD trip departure
  
  tod <- data.frame(TOD = seq(0, 23, 1))
  
  # raw survey data
  tod_trip_dep_raw <- trips_df %>% group_by(ORIG_DEP_HR) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% rename(TOD = 'ORIG_DEP_HR')
  tod_trip_raw <- merge(tod, tod_trip_dep_raw, by = 'TOD', all.x = TRUE)
  tod_trip_raw <- tod_trip_raw %>% replace_na(list(DEPARTING = 0)) %>% 
    rename('Number of Trips (Raw)' = 'DEPARTING')
  
  # expanded survey data
  tod_trip_dep_exp <- trips_df %>% group_by(ORIG_DEP_HR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% rename(TOD = 'ORIG_DEP_HR')
  tod_trip_exp <- merge(tod, tod_trip_dep_exp, by = 'TOD', all.x = TRUE)
  tod_trip_exp <- tod_trip_exp %>% replace_na(list(DEPARTING = 0)) %>%
    rename('Number of Trips (Expanded)' = 'DEPARTING')
  
  # raw survey data as percentage
  tod_trip_dep_raw_pct <- trips_df %>% group_by(ORIG_DEP_HR) %>%
    tally(name = 'DEPARTING', wt = DIARYWEIGHT) %>% 
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>%
    rename(TOD = 'ORIG_DEP_HR') %>% select(-DEPARTING)
  tod_trip_raw_pct <- merge(tod, tod_trip_dep_raw_pct, by = 'TOD', all.x = TRUE)
  tod_trip_raw_pct <- tod_trip_raw_pct %>% replace_na(list(DEPARTING_PCT = 0)) %>% 
    rename('Number of Trips (Raw)' = 'DEPARTING_PCT')
  
  # expanded survey data as percentage
  tod_trip_dep_exp_pct <- trips_df %>% group_by(ORIG_DEP_HR) %>%
    tally(name = 'DEPARTING', wt = EXP_WEIGHT_TOTTR) %>% 
    mutate(DEPARTING_PCT = DEPARTING / sum(DEPARTING)) %>% 
    rename(TOD = 'ORIG_DEP_HR') %>% select(-DEPARTING)
  tod_trip_exp_pct <- merge(tod, tod_trip_dep_exp_pct, by = 'TOD', all.x = TRUE)
  tod_trip_exp_pct <- tod_trip_exp_pct %>% replace_na(list(DEPARTING_PCT = 0)) %>%
    rename('Number of Trips (Expanded)' = 'DEPARTING_PCT')
  
  tod_trip <- tod_trip_raw %merge% tod_trip_exp
  tod_trip_pct <- tod_trip_raw_pct %merge% tod_trip_exp_pct
  
## ------------------------------------------------------------------------------------  
  
  ### primary destination (TAZ)
  
  prim_dest <- tour_df[,c('HH_ID', 'PRIMDEST_MGRA', 'PRIMDEST_TAZ', 'TOUR_MODE_CBM_STR', 
                          'TOUR_MODE_CBM2_STR', 'TOUR_PURP_CBM_STR', 'poe_str', 'RETURN_MX',
                          'DIARYWEIGHT', 'EXP_WEIGHT_TOTTR')]
  
## ------------------------------------------------------------------------------------  
  
  ### previous CBM 2011 data comparison
  
  # filter to respondents who completed Part 2 Travel Diary
  old_cb_data <- old_cb_data %>% filter(STATUS == 2)
  
  # add POE labels
  old_poe_lab <- data.frame(POE = c(1,2,3),
                            POE_STR = c('San Ysidro', 'Otay Mesa', 'Tecate'))
  old_cb_data <- merge(old_cb_data, old_poe_lab, by = 'POE', all.x = TRUE)
  var_lab(old_cb_data$POE_STR) <- 'Port of entry'
  
  # new survey data
  new_cb_data_poe <- cro(cb_data$poe_str)
  new_cb_data_poe_pct <- cro_cpct(cb_data$poe_str, total_statistic = 'w_cpct')
  
  # old survey data
  old_cb_data_poe <- cro(old_cb_data$POE_STR)
  old_cb_data_poe_pct <- cro_cpct(old_cb_data$POE_STR, total_statistic = 'w_cpct')
  
## ------------------------------------------------------------------------------------
  
  ### trips by trip mode
  
  # raw survey data
  trips_mode_raw <- cro(trips_df$TRIP_MODE_CBM_STR, weight = trips_df$DIARYWEIGHT, 
                        total_statistic = 'w_cases', total_label = "Total")
  colnames(trips_mode_raw)[2] <- 'Number of Trips (Raw)'
  
  # expanded survey data
  trips_mode_exp <- cro(trips_df$TRIP_MODE_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                        total_statistic = 'w_cases', total_label = "Total")
  colnames(trips_mode_exp)[2] <- 'Number of Trips (Expanded)'
  
  # expanded survey data as percentage
  trips_mode_exp_pct <- cro_cpct(trips_df$TRIP_MODE_CBM_STR, weight = trips_df$EXP_WEIGHT_TOTTR, 
                             total_statistic = 'w_cpct', total_label = "Total")
  colnames(trips_mode_exp_pct)[2] <- 'Percentage of Trips'
  
  # merge
  trips_mode <- trips_mode_raw %merge% trips_mode_exp %merge% trips_mode_exp_pct
  
  # fix row order
  trips_mode <- trips_mode %>% slice(match(trip_mode_order, row_labels))
  
## ------------------------------------------------------------------------------------

  ### stop purpose by tour purpose
  
  # raw survey data
  trip_tour_purp_raw <- cro(trips_df$DEST_PURP_CBM_STR, trips_df$TOUR_PURP_CBM2_STR, weight = trips_df$DIARYWEIGHT,
                            total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data
  trip_tour_purp_exp <- cro(trips_df$DEST_PURP_CBM_STR, trips_df$TOUR_PURP_CBM2_STR, weight = trips_df$EXP_WEIGHT_TOTTR,
                            total_statistic = 'w_cases', total_label = "Total")
  
  # expanded survey data as percentage
  trip_tour_purp_exp_pct <- cro_cpct(trips_df$DEST_PURP_CBM_STR, trips_df$TOUR_PURP_CBM2_STR, weight = trips_df$DIARYWEIGHT,
                                total_statistic = 'w_cpct', total_label = "Total")
  
## ------------------------------------------------------------------------------------

  ### average wait time by average wait times by (1) port of entry, (2) crossing type and (3) time of day
    
  # wait time data
  border_wait_times <- data.frame(wait_time)
  
  # check for and remove weekend data
  border_wait_times <- border_wait_times %>% mutate(day_week = weekdays(as.Date(date, '%m/%d/%Y')))
  border_wait_times <- border_wait_times %>% filter(day_week != 'Saturday', day_week != 'Sunday')
  
  # remove unwanted port of entries
  border_wait_times <- border_wait_times %>% filter(port %in% c('Otay Mesa Passenger', 'San Ysidro', 'Tecate'))
  
  # calculate average wait times 
  border_wait_times_avg <- border_wait_times %>% group_by(port, crossing_type, hour) %>% 
    summarise(avg_wait = mean(delay))
  border_wait_times_avg <- border_wait_times_avg %>% arrange(port, crossing_type, hour)
  
  # melt results
  border_wait_times_avg_cast <- dcast(border_wait_times_avg, port + hour ~ crossing_type, value.var = 'avg_wait')
  border_wait_times_avg_cast$`Commercial - Standard` <- NULL
  
## ------------------------------------------------------------------------------------  
  
  ### tour length frequency distribution
  
  tlfd <- tour_df[, c('HH_ID', 'ANCHOR_TAZ', 'PRIMDEST_TAZ', 'MAT_DIST', 'poe_str', 
                      'TOUR_PURP_CBM2_STR', 'EXP_WEIGHT_TOTTR', 'DIARYWEIGHT')]
  
  # add intervals
  tlfd <- tlfd %>% mutate(MAT_DIST_FLOOR = floor(MAT_DIST))
  
  # group 50+ distances
  tlfd <- tlfd %>% mutate(DIST_BIN = case_when(MAT_DIST_FLOOR < 50 ~ MAT_DIST_FLOOR,
                                               MAT_DIST_FLOOR >= 50 ~ 50))
  
  var_lab(tlfd$DIST_BIN) <- 'Distance'
  
  tlfd_raw <- cro(tlfd$DIST_BIN, tlfd$TOUR_PURP_CBM2_STR, weight = tlfd$DIARYWEIGHT, 
                  total_statistic = 'w_cases', total_label = 'Total')
  tlfd_exp <- cro(tlfd$DIST_BIN, tlfd$TOUR_PURP_CBM2_STR, weight = tlfd$EXP_WEIGHT_TOTTR, 
                  total_statistic = 'w_cases', total_label = 'Total')
  tlfd_exp_pct <- cro_cpct(tlfd$DIST_BIN, tlfd$TOUR_PURP_CBM2_STR, weight = tlfd$EXP_WEIGHT_TOTTR, 
                           total_statistic = 'w_cpct', total_label = 'Total')
  
  # fix column order
  tlfd_raw <- tlfd_raw[, tour_purp2_order_col]
  tlfd_exp <- tlfd_exp[, tour_purp2_order_col]
  tlfd_exp_pct <- tlfd_exp_pct[, tour_purp2_order_col]
  
  # average distance by tour purpose
  avg_dist_purp <- tlfd %>% group_by(TOUR_PURP_CBM2_STR) %>% 
    summarise(WT_MEAN = weighted.mean(MAT_DIST, EXP_WEIGHT_TOTTR, na.rm = TRUE)) %>%
    rename(TOUR_PURP = 'TOUR_PURP_CBM2_STR')
  
  # fix row order
  avg_dist_purp <- avg_dist_purp %>% slice(match(c('Work', 'School', 'Shop', 'Visit', 'Other'), TOUR_PURP))
  
## ------------------------------------------------------------------------------------
  
  # write out summaries
  
  if (csv_out) {
    write_labelled_xlsx(table1, 'output/summaries/tours_by_purpose.xlsx')
    write_labelled_xlsx(table1_pct, 'output/summaries/tours_by_purpose_pct.xlsx')
    write_labelled_xlsx(table2_exp, 'output/summaries/tour_mode_by_poe_exp.xlsx')
    write_labelled_xlsx(table2_exp_pct, 'output/summaries/tour_mode_by_poe_exp_pct.xlsx')
    write_labelled_xlsx(table2B_exp, 'output/summaries/tour_mode_by_poe_exp_B.xlsx')
    write_labelled_xlsx(table2B_exp_pct, 'output/summaries/tour_mode_by_poe_exp_B_pct.xlsx')
    write_labelled_xlsx(table2_raw, 'output/summaries/tour_mode_by_poe_raw.xlsx')
    write_labelled_xlsx(table2B_raw, 'output/summaries/tour_mode_by_poe_raw_B.xlsx')
    write_labelled_xlsx(table3_exp, 'output/summaries/trips_by_tour_purp_and_trip_mode_exp.xlsx')
    write_labelled_xlsx(table3_exp_pct, 'output/summaries/trips_by_tour_purp_and_trip_mode_exp_pct.xlsx')
    write_labelled_xlsx(in_raw, 'output/summaries/inbound_stop_frequency_raw.xlsx')
    write_labelled_xlsx(out_raw, 'output/summaries/outbound_stop_frequency_raw.xlsx')
    write_labelled_xlsx(in_exp, 'output/summaries/inbound_stop_frequency_exp.xlsx')
    write_labelled_xlsx(out_exp, 'output/summaries/outbound_stop_frequency_exp.xlsx')
    write_labelled_xlsx(in_exp_pct, 'output/summaries/inbound_stop_frequency_exp_pct.xlsx')
    write_labelled_xlsx(out_exp_pct, 'output/summaries/outbound_stop_frequency_exp_pct.xlsx')
    write_labelled_xlsx(tod_arr_dep_raw, 'output/summaries/tod_arr_dep_raw.xlsx')
    write_labelled_xlsx(tod_arr_dep_exp, 'output/summaries/tod_arr_dep_exp.xlsx')
    write_labelled_xlsx(tod_arr_dep_exp_pct, 'output/summaries/tod_arr_dep_exp_pct.xlsx')
    write_labelled_xlsx(bin_arr_dep_raw, 'output/summaries/bin_arr_dep_raw.xlsx')
    write_labelled_xlsx(bin_arr_dep_exp, 'output/summaries/bin_arr_dep_exp.xlsx')
    write_labelled_xlsx(bin_arr_dep_exp_pct, 'output/summaries/bin_arr_dep_exp_pct.xlsx')
    write_labelled_xlsx(bin_arr_dep_purp_raw, 'output/summaries/bin_arr_dep_purp_raw.xlsx')
    write_labelled_xlsx(bin_arr_dep_purp_exp, 'output/summaries/bin_arr_dep_purp_exp.xlsx')
    write_labelled_xlsx(bin_arr_dep_purp_exp_pct, 'output/summaries/bin_arr_dep_purp_exp_pct.xlsx')
    write_labelled_xlsx(bin_arr_dep_poe_raw, 'output/summaries/bin_arr_dep_poe_raw.xlsx')
    write_labelled_xlsx(bin_arr_dep_poe_exp, 'output/summaries/bin_arr_dep_poe_exp.xlsx')
    write_labelled_xlsx(bin_arr_dep_poe_exp_pct, 'output/summaries/bin_arr_dep_poe_exp_pct.xlsx')
    write_labelled_xlsx(tottr_raw, 'output/summaries/num_travelers_raw.xlsx')
    write_labelled_xlsx(tottr_exp, 'output/summaries/num_travelers_exp.xlsx')
    write_labelled_xlsx(tottr_pct, 'output/summaries/num_travelers_exp_pct.xlsx')
    write_labelled_xlsx(num_trips, 'output/summaries/num_trips_by_trip_purp.xlsx')
    write_labelled_xlsx(num_trips_pct, 'output/summaries/num_trips_by_trip_purp_pct.xlsx')
    write_labelled_xlsx(tod_trip, 'output/summaries/tod_trip_departures.xlsx')
    write_labelled_xlsx(tod_trip_pct, 'output/summaries/tod_trip_departures_pct.xlsx')
    write_labelled_xlsx(trips_mode, 'output/summaries/trips_mode.xlsx')
    write_labelled_xlsx(trip_tour_purp_raw, 'output/summaries/trip_purp_by_tour_purp_raw.xlsx')
    write_labelled_xlsx(trip_tour_purp_exp, 'output/summaries/trip_purp_by_tour_purp_exp.xlsx')
    write_labelled_xlsx(trip_tour_purp_exp_pct, 'output/summaries/trip_purp_by_tour_purp_pct.xlsx')
    write_labelled_xlsx(tlfd_raw, 'output/summaries/tlfd_raw.xlsx')
    write_labelled_xlsx(tlfd_exp, 'output/summaries/tlfd_exp.xlsx')
    write_labelled_xlsx(tlfd_exp_pct, 'output/summaries/tlfd_exp_pct.xlsx')
    write_csv(prim_dest, 'output/summaries/primary_destination.csv')
    write_csv(border_wait_times_avg_cast, 'output/summaries/border_wait_times.csv')
    write_csv(tlfd, 'output/summaries/tlfd.csv')
  }
  
}

# end ---------------------------------------------------------------------------------