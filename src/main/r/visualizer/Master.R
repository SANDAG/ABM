#############################################################################################################################
# Master script to render final HTML file from R Markdown file
# Loads all required packages from the dependencies folder
#
# Make sure the 'plyr' is not loaded after 'dplyr' library in the same R session
# Under such case, the group_by features of dplyr library does not work. Restart RStudio and make sure
# plyr library is not loaded while generating dashboard
# For more info on this issue: 
# https://stackoverflow.com/questions/26923862/why-are-my-dplyr-group-by-summarize-not-working-properly-name-collision-with
#
#############################################################################################################################

##### LIST OF ALL INPUT FILES #####
## 0. Path input data                   : parameters.csv
## 1. Base scenario summary file names  : summaryFilesNames_survey.csv
## 2. Build scenario summary file names : summaryFilesNames.csv
## 3. Model area shapefile              : summaryFilesNames.csv
## 4. All REF and ABM summary output files

### Read Command Line Arguments
args                <- commandArgs(trailingOnly = TRUE)
Parameters_File     <- args[1]
showWarnings=FALSE

### Read parameters from Parameters_File
parameters          <- read.csv(Parameters_File, header = TRUE)
WORKING_DIR         <- trimws(paste(parameters$Value[parameters$Key=="WORKING_DIR"]))	
BASE_SUMMARY_DIR    <- trimws(paste(parameters$Value[parameters$Key=="BASE_SUMMARY_DIR"]))
BUILD_SUMMARY_DIR   <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SUMMARY_DIR"]))
BASE_SCENARIO_NAME  <- trimws(paste(parameters$Value[parameters$Key=="BASE_SCENARIO_NAME"]))
BUILD_SCENARIO_NAME <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SCENARIO_NAME"]))
BASE_SAMPLE_RATE    <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BASE_SAMPLE_RATE"])))
BUILD_SAMPLE_RATE   <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BUILD_SAMPLE_RATE"])))
R_LIBRARY           <- trimws(paste(parameters$Value[parameters$Key=="R_LIBRARY"]))
OUTPUT_HTML_NAME    <- trimws(paste(parameters$Value[parameters$Key=="OUTPUT_HTML_NAME"]))
SHP_FILE_NAME       <- trimws(paste(parameters$Value[parameters$Key=="SHP_FILE_NAME"]))
IS_BASE_SURVEY      <- trimws(paste(parameters$Value[parameters$Key=="IS_BASE_SURVEY"]))

### Initialization
# Load global variables
.libPaths(R_LIBRARY)
source(paste(WORKING_DIR, "scripts/_SYSTEM_VARIABLES.R", sep = "/"))

###create directories
dir.create(BASE_DATA_PATH)
dir.create(BUILD_DATA_PATH)

### Copy summary CSVs
base_CSV_list <- ifelse(IS_BASE_SURVEY=="Yes", "summaryFilesNames_survey.csv", "summaryFilesNames.csv")
summaryFileList_base <- read.csv(paste(SYSTEM_TEMPLATES_PATH, base_CSV_list, sep = '/'), as.is = T)
summaryFileList_base <- as.list(summaryFileList_base$summaryFile)
retVal <- copyFile(summaryFileList_base, sourceDir = BASE_SUMMARY_DIR, targetDir = BASE_DATA_PATH)
if(retVal) q(save = "no", status = 11)
summaryFileList_build <- read.csv(paste(SYSTEM_TEMPLATES_PATH, "summaryFilesNames.csv", sep = '/'), as.is = T)
summaryFileList_build <- as.list(summaryFileList_build$summaryFile)
retVal <- copyFile(summaryFileList_build, sourceDir = BUILD_SUMMARY_DIR, targetDir = BUILD_DATA_PATH)
if(retVal) q(save = "no", status = 11)

### Load required libraries
SYSTEM_REPORT_PKGS <- c("DT", "flexdashboard", "leaflet", "geojsonio", "htmltools", "htmlwidgets", "kableExtra",
                        "knitr", "mapview", "plotly", "RColorBrewer", "rgdal", "rgeos", "crosstalk","treemap", "htmlTable",
                        "rmarkdown", "scales", "stringr", "jsonlite", "pander", "ggplot2", "reshape", "raster", "dplyr")

lapply(SYSTEM_REPORT_PKGS, library, character.only = TRUE)

### Read Target and Output SUmmary files
currDir <- getwd()
setwd(BASE_DATA_PATH)
base_csv = list.files(pattern="*.csv")
base_data <- lapply(base_csv, read.csv)
base_csv_names <- unlist(lapply(base_csv, function (x) {gsub(".csv", "", x)}))

setwd(BUILD_DATA_PATH)
build_csv = list.files(pattern="*.csv")
build_data <- lapply(build_csv, read.csv)
build_csv_names <- unlist(lapply(build_csv, function (x) {gsub(".csv", "", x)}))

## Read SHP file
setwd(SYSTEM_SHP_PATH)
zone_shp <- shapefile(SHP_FILE_NAME)
zone_shp <- spTransform(zone_shp, CRS("+proj=longlat +ellps=GRS80"))

setwd(currDir)

### Generate dashboard
rmarkdown::render(file.path(SYSTEM_TEMPLATES_PATH, "template.Rmd"),
                  output_dir = RUNTIME_PATH,
                  intermediates_dir = RUNTIME_PATH, quiet = TRUE)
template.html <- readLines(file.path(RUNTIME_PATH, "template.html"))
idx <- which(template.html == "window.FlexDashboardComponents = [];")[1]
template.html <- append(template.html, "L_PREFER_CANVAS = true;", after = idx)
writeLines(template.html, file.path(OUTPUT_PATH, paste(OUTPUT_HTML_NAME, ".html", sep = "")))

# finish