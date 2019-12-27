### Paths
SYSTEM_APP_PATH       <- WORKING_DIR
SYSTEM_DATA_PATH      <- file.path(SYSTEM_APP_PATH, "data")
SYSTEM_SHP_PATH       <- file.path(SYSTEM_DATA_PATH, "SHP")
SYSTEM_TEMPLATES_PATH <- file.path(SYSTEM_APP_PATH, "templates")
SYSTEM_SCRIPTS_PATH   <- file.path(SYSTEM_APP_PATH, "scripts")
OUTPUT_PATH           <- file.path(SYSTEM_APP_PATH, "outputs")
RUNTIME_PATH          <- file.path(SYSTEM_APP_PATH, "runtime")
BASE_DATA_PATH        <- file.path(SYSTEM_DATA_PATH, "base")
BUILD_DATA_PATH       <- file.path(SYSTEM_DATA_PATH, "build")

### Names
if(IS_BASE_SURVEY=="Yes"){
  #   Surey Base
  BASE_SCENARIO_ALT     <- "HTS"
  DISTRICT_FLOW_CENSUS  <- "HTS"
  AO_CENSUS_SHORT       <- "HTS"
  AO_CENSUS_LONG        <- "HTS"
}else{
  #   Non-Survey Base
  BASE_SCENARIO_ALT     <- BASE_SCENARIO_NAME
  DISTRICT_FLOW_CENSUS  <- BASE_SCENARIO_NAME
  AO_CENSUS_SHORT       <- BASE_SCENARIO_NAME
  AO_CENSUS_LONG        <- BASE_SCENARIO_NAME
}

### Other Codes
person_type_codes     <- c(1, 2, 3, 4, 5, 6, 7, 8, "Total")
person_type_names     <- c("1.FT Worker", "2.PT Worker", "3.Univ Stud", "4.Non-Worker", "5.Retiree", "6.Driv Student", "7.Non-DrivStudent", "8.Pre-Schooler", "Total")
person_type_char      <- c("FT Worker", "PT Worker", "Univ Stud", "Non-Worker", "Retiree", "Driv Student", "Non-DrivStudent", "Pre-Schooler", "Total")
person_type_df        <- data.frame(code = person_type_codes, name = person_type_names, name_char = person_type_char)

purpose_type_codes    <- c("atwork", "esc", "esco", "idisc", "imain", "jdisc", "jmain", "sch", "schl", "univ", "work", "total", "Total")
purpose_type_names    <- c("At-Work", "Escorting", "Escorting", "Indi-Discretionary", "Indi-Maintenance", "Joint-Discretionary", "Joint-Maintenance", "School", "School", "University", "Work", "Total", "Total")
purpose_type_df      <- data.frame(code = purpose_type_codes, name = purpose_type_names)

mtf_codes             <- c(1, 2, 3, 4, 5)
mtf_names             <- c("1 Work", "2 Work", "1 School", "2 School", "1 Work & 1 School")
mtf_df                <- data.frame(code = mtf_codes, name = mtf_names)
dap_types             <- c("M", "N", "H")
jtf_alternatives      <- c("No Joint Tours", "1 Shopping", "1 Maintenance", "1 Eating Out", "1 Visiting", "1 Other Discretionary", 
                           "2 Shopping", "1 Shopping / 1 Maintenance", "1 Shopping / 1 Eating Out", "1 Shopping / 1 Visiting", 
                           "1 Shopping / 1 Other Discretionary", "2 Maintenance", "1 Maintenance / 1 Eating Out", 
                           "1 Maintenance / 1 Visiting", "1 Maintenance / 1 Other Discretionary", "2 Eating Out", "1 Eating Out / 1 Visiting", 
                           "1 Eating Out / 1 Other Discretionary", "2 Visiting", "1 Visiting / 1 Other Discretionary", "2 Other Discretionary")
todBins               <- c("03:00 AM to 05:00 AM","05:00 AM to 05:30 AM","05:30 AM to 06:00 AM","06:00 AM to 06:30 AM","06:30 AM to 07:00 AM",
                           "07:00 AM to 07:30 AM","07:30 AM to 08:00 AM","08:00 AM to 08:30 AM", "08:30 AM to 09:00 AM","09:00 AM to 09:30 AM", 
                           "09:30 AM to 10:00 AM","10:00 AM to 10:30 AM", "10:30 AM to 11:00 AM","11:00 AM to 11:30 AM", "11:30 AM to 12:00 PM",
                           "12:00 PM to 12:30 PM", "12:30 PM to 01:00 PM","01:00 PM to 01:30 PM", "01:30 PM to 02:00 PM","02:00 PM to 02:30 PM", 
                           "02:30 PM to 03:00 PM","03:00 PM to 03:30 PM", "03:30 PM to 04:00 PM","04:00 PM to 04:30 PM", "04:30 PM to 05:00 PM",
                           "05:00 PM to 05:30 PM", "05:30 PM to 06:00 PM","06:00 PM to 06:30 PM", "06:30 PM to 07:00 PM","07:00 PM to 07:30 PM", 
                           "07:30 PM to 08:00 PM","08:00 PM to 08:30 PM", "08:30 PM to 09:00 PM","09:00 PM to 09:30 PM", "09:30 PM to 10:00 PM",
                           "10:00 PM to 10:30 PM", "10:30 PM to 11:00 PM","11:00 PM to 11:30 PM", "11:30 PM to 12:00 AM","12:00 PM to 03:00 AM")
tod_df                <- data.frame(id = seq(from=1, to=40), bin = todBins)
durBins               <- c("(0.5 hours)","(1 hours)","(1.5 hours)","(2 hours)","(2.5 hours)","(3 hours)","(3.5 hours)","(4 hours)","(4.5 hours)",
                           "(5 hours)","(5.5 hours)","(6 hours)","(6.5 hours)","(7 hours)","(7.5 hours)","(8 hours)","(8.5 hours)","(9 hours)",
                           "(9.5 hours)","(10 hours)","(10.5 hours)","(11 hours)","(11.5 hours)","(12 hours)","(12.5 hours)","(13 hours)",
                           "(13.5 hours)","(14 hours)","(14.5 hours)","(15 hours)","(15.5 hours)","(16 hours)","(16.5 hours)","(17 hours)",
                           "(17.5 hours)","(18 hours)","(18.5 hours)","(19 hours)","(19.5 hours)","(20 hours)")
dur_df                <- data.frame(id = seq(from=1, to=40), bin = durBins)
stopPurposes          <- c("Work","Univ","Schl","Esco","Shop","Main","Eati","Visi","Disc","Work-related")
outDirDist            <- c("< 0", "0-1", "1-2", "2-3", "3-4", "4-5", "5-6", "6-7", "7-8", "8-9", "9-10", "10-11", "11-12", "12-13", "13-14", "14-15", "15-16", "16-17", "17-18", "18-19", "19-20", "20-21", 
                           "21-22", "22-23", "23-24", "24-25", "25-26", "26-27", "27-28", "28-29", "29-30", "30-31", "31-32", "32-33", "33-34", "34-35", "35-36", "36-37", "37-38", "38-39", "39-40", "40p")  
# tourMode              <- c('Auto SOV','Auto 2 Person','Auto 3+ Person','Walk','Bike/Moped','Walk-Transit','PNR-Transit','KNR-Transit','School Bus')
# tripMode              <- c('Auto SOV','Auto 2 Person','Auto 3+ Person','Walk','Bike/Moped','Walk-Transit','PNR-Transit','KNR-Transit','School Bus')
tourMode              <- c('Auto SOV','Auto 2 Person','Auto 3+ Person','Walk','Bike/Moped','Walk-Transit','PNR-Transit','KNR-Transit','TNC-Transit','Taxi','TNC-Single','TNC-Shared','School Bus')
tripMode              <- c('Auto SOV','Auto 2 Person','Auto 3+ Person','Walk','Bike/Moped','Walk-Transit','PNR-Transit','KNR-Transit','TNC-Transit','Taxi','TNC-Single','TNC-Shared','School Bus')
sch_esc_types         <- c('Ride Share', 'Pure Escort', 'No Escort')
sch_esc_codes         <- c(1, 2, 3)
sch_esc_df            <- data.frame(code = sch_esc_codes, type = sch_esc_types)
facility_types        <- c('Interstate', 'Principal Arterial', 'Minor Arterial', 'Major Collector', 'Minor Collector', 'Local Road', 'Ramp')
facility_codes        <- c(1, 3, 4, 5, 6, 7, 30)
facility_df           <- data.frame(code = facility_codes, type = facility_types)
timePeriods           <- c("EV1","EA","AM","MD","PM","EV")
timePeriodBreaks      <- c(0,1,4,10,23,30, 41)
occp_type_codes       <- c("occ1", "occ2", "occ3", "occ4", "occ5", "occ6", "Total")
occp_type_names       <- c("Management", "Service", "Sales & Office", "Natural Resources", "Production", "Military", "Total")
occp_type_df          <- data.frame(code = occp_type_codes, name = occp_type_names)

### Functions
copyFile <- function(fileList, sourceDir, targetDir){
  error <- F
  setwd(sourceDir)
  for(file in fileList){
    full_file <- paste(sourceDir, file, sep = "/")
    ## check if file exists - copy if exists else error out
    if(file.exists(file)){
      file.copy(full_file, targetDir, overwrite = T, copy.date = T)
    }else{
      #winDialog("ok", paste(file, "does not exist in", sourceDir))
      write.table(paste(file, "does not exist in", sourceDir), paste(OUTPUT_PATH, "error.txt", sep = "/"))
      error <- T
    }
    if(error) break
  }
  return(error)
}





