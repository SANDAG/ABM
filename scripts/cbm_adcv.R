## ------------------------------------------------------------------------------------
##
## Script name: cbm_expansion_factors.R
##
## Purpose of script: 
##    - creates expansion factors for 10 target groups using the crossing volumes found 
##        in the Crossings (SYS, Otay Mesa, Tecate).xlsx file where target groups are 
##        composed of varying POEs (3) and crossing lane types (4; 2 for Tecate POE)
##
## Author: Cundo Arellano (cundo.arellano@rsginc.com)
##
## Date Created: 2020-08-12
##
## ------------------------------------------------------------------------------------

# set working directory ---------------------------------------------------------------

setwd("E:/projects/clients/sandag/XBORDER/Tasks/T03 Data_Processing/survey_data_processing")

# load packages -----------------------------------------------------------------------

library(tidyverse)
library(openxlsx)

# inputs ------------------------------------------------------------------------------

# 2019 full border crossing survey residency distribution
res_dist <- read_csv("input/survey_residency_distribution.csv")

# crossing volumes data
sy_veh_pax <- read_csv("input/observed_crossings/san_ysidro_veh_pax.csv")
sy_ped <- read_csv("input/observed_crossings/san_ysidro_ped.csv")

otay_veh_pax <- read_csv("input/observed_crossings/otay_veh_pax.csv")
otay_ped <- read_csv("input/observed_crossings/otay_ped.csv")

tecate_veh_pax <- read_csv("input/observed_crossings/tecate_veh_pax.csv")
tecate_ped <- read_csv("input/observed_crossings/tecate_ped.csv")

# determine average daily pedestrian traffic ------------------------------------------

# note: weekend data was manually removed

sy_ped <- sy_ped %>% 
  replace(is.na(.), 0) %>% 
  group_by(Date) %>%
  summarise(Ped_Sum = sum(Count, na.rm = TRUE)) %>%
  summarise(Ped_Mean = mean(Ped_Sum))

otay_ped <- otay_ped %>% 
  replace(is.na(.), 0) %>% 
  group_by(Date) %>%
  summarise(Ped_Sum = sum(Count, na.rm = TRUE)) %>%
  summarise(Ped_Mean = mean(Ped_Sum))

tecate_ped <- tecate_ped %>% 
  replace(is.na(.), 0) %>% 
  group_by(Date) %>%
  summarise(Ped_Sum = sum(Count, na.rm = TRUE)) %>%
  summarise(Ped_Mean = mean(Ped_Sum))

# determine average daily vehicle pax traffic -----------------------------------------

# remove weekend data
sy_veh_pax <- sy_veh_pax %>% filter(!str_detect(Date, 'Saturday|Sunday'))
otay_veh_pax <- otay_veh_pax %>% filter(!str_detect(Date, 'Saturday|Sunday'))
tecate_veh_pax <- tecate_veh_pax %>% filter(!str_detect(Date, 'Saturday|Sunday'))

# san ysidro
sy_veh_pax <- sy_veh_pax %>%
  replace(is.na(.), 0) %>% 
  group_by(Date) %>%
  summarise(Ready_Sum = sum(Ready_Total), 
            SENTRI_Sum = sum(SENTRI_Total),
            General_Sum = sum(General_Total)) %>%
  summarise(Ready_Mean = mean(Ready_Sum),
            SENTRI_Mean = mean(SENTRI_Sum),
            General_Mean = mean(General_Sum))

# otay mesa
otay_veh_pax <- otay_veh_pax %>% 
  replace(is.na(.), 0) %>% 
  group_by(Date) %>%
  summarise(Ready_Sum = sum(Ready_Total), 
            SENTRI_Sum = sum(SENTRI_Total),
            General_Sum = sum(General_Total)) %>%
  summarise(Ready_Mean = mean(Ready_Sum),
            SENTRI_Mean = mean(SENTRI_Sum),
            General_Mean = mean(General_Sum))
  
# tecate
tecate_veh_pax <- tecate_veh_pax %>% 
  replace(is.na(.), 0) %>% 
  group_by(Date) %>% 
  summarise(General_Sum = sum(General_Total)) %>%
  summarise(General_Mean = mean(General_Sum))

# merge all data ----------------------------------------------------------------------

exp_out_pax <- data.frame(EXP_CAT = c(1,2,3,4,5,6,7,8,9,10),
                      OBS_VOL = c(
                       sy_veh_pax[,"SENTRI_Mean"][[1]],
                       sy_veh_pax[,"Ready_Mean"][[1]],
                       sy_veh_pax[,"General_Mean"][[1]],
                       sy_ped[,"Ped_Mean"][[1]],
                       otay_veh_pax[,"SENTRI_Mean"][[1]],
                       otay_veh_pax[,"Ready_Mean"][[1]],
                       otay_veh_pax[,"General_Mean"][[1]],
                       otay_ped[,"Ped_Mean"][[1]],
                       tecate_veh_pax[,"General_Mean"][[1]],
                       tecate_ped[,"Ped_Mean"][[1]]
                      ))

exp_out_pax_usc <- data.frame(EXP_CAT = c(1,2,3,4,5,6,7,8,9,10),
                          OBS_VOL = c(
                            sy_veh_pax[,"SENTRI_Mean"][[1]],
                            sy_veh_pax[,"Ready_Mean"][[1]],
                            sy_veh_pax[,"General_Mean"][[1]],
                            sy_ped[,"Ped_Mean"][[1]],
                            otay_veh_pax[,"SENTRI_Mean"][[1]],
                            otay_veh_pax[,"Ready_Mean"][[1]],
                            otay_veh_pax[,"General_Mean"][[1]],
                            otay_ped[,"Ped_Mean"][[1]],
                            tecate_veh_pax[,"General_Mean"][[1]],
                            tecate_ped[,"Ped_Mean"][[1]]
                          ))

# apply residency distribution --------------------------------------------------------

exp_out_pax <- merge(exp_out_pax, res_dist[1:10,], by = 'EXP_CAT')
exp_out_pax <- exp_out_pax %>% mutate(TARGET_MX = OBS_VOL*DIST)

exp_out_pax_usc <- exp_out_pax_usc %>% mutate(EXP_CAT = EXP_CAT + 10)
exp_out_pax_usc <- merge(exp_out_pax_usc, res_dist[11:20,], by = 'EXP_CAT')
exp_out_pax_usc <- exp_out_pax_usc %>% mutate(TARGET_US = OBS_VOL*DIST)

# print -------------------------------------------------------------------------------

write_csv(exp_out_pax, "input/adcv_mx.csv")
write_csv(exp_out_pax_usc, 'input/adcv_us.csv')
