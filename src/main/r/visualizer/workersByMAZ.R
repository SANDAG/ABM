##########################################################
### Script to summarize workers by MAZ and Occupation Type

##### LIST OF ALL INPUT FILES #####
## 0. Path input data            : parameters.csv
## 1. person data                : personData_3.csv
## 2. Work school location data  : wsLocResults_3.csv
## 3. MAZ data                   : mgra13_based_input2016.csv
## 4. Occupation factors data    : occFactors.csv
## 5. Geographic crosswalk data  : geographicXwalk_PMSA.csv

### Read Command Line Arguments
args                <- commandArgs(trailingOnly = TRUE)
Parameters_File     <- args[1]
REF                 <- args[2]

SYSTEM_REPORT_PKGS <- c("reshape", "dplyr", "data.table")
lib_sink <- suppressWarnings(suppressMessages(lapply(SYSTEM_REPORT_PKGS, library, character.only = TRUE))) 

### Read parameters file
parameters          <- read.csv(Parameters_File, header = TRUE)

### Read parameters from Parameters_File (REF)
PROJECT_DIR         <- trimws(paste(parameters$Value[parameters$Key=="PROJECT_DIR"]))
if(REF){
  WD                  <- trimws(paste(parameters$Value[parameters$Key=="BASE_SUMMARY_DIR"]))
  ABMOutputDir        <- trimws(paste(parameters$Value[parameters$Key=="REF_DIR"]))
  ABMInputDir         <- trimws(paste(parameters$Value[parameters$Key=="REF_DIR_INP"]))
  BUILD_SAMPLE_RATE   <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BASE_SAMPLE_RATE"])))
} else {
  WD                  <- trimws(paste(parameters$Value[parameters$Key=="BUILD_SUMMARY_DIR"]))

  ABMOutputDir        <- file.path(PROJECT_DIR, "output")
  ABMInputDir         <- file.path(PROJECT_DIR, "input")
  BUILD_SAMPLE_RATE   <- as.numeric(trimws(paste(parameters$Value[parameters$Key=="BUILD_SAMPLE_RATE"])))
}

MAX_ITER              <- trimws(paste(parameters$Value[parameters$Key=="MAX_ITER"]))
WORKING_DIR           <- trimws(paste(parameters$Value[parameters$Key=="WORKING_DIR"]))
geogXWalkDir          <- trimws(paste(parameters$Value[parameters$Key=="geogXWalkDir"]))
mazFile          	  <- trimws(paste(parameters$Value[parameters$Key=="mgraInputFile"]))
factorDir             <- file.path(WORKING_DIR, "data")

# read data
per                   <- read.csv(paste(ABMOutputDir, paste("personData_",MAX_ITER, ".csv", sep = ""), sep = "/"), as.is = T)
wsLoc                 <- read.csv(paste(ABMOutputDir, paste("wsLocResults_",MAX_ITER, ".csv", sep = ""), sep = "/"), as.is = T)
mazData               <- read.csv(paste(ABMInputDir, basename(mazFile), sep = "/"), as.is = T)
occFac                <- read.csv(paste(factorDir, "occFactors.csv", sep = "/"), as.is = T)
mazCorrespondence     <- fread(paste(geogXWalkDir, "geographicXwalk_PMSA.csv", sep = "/"), stringsAsFactors = F)

# workers by occupation type
workersbyMAZ <- wsLoc[wsLoc$PersonType<=3 & wsLoc$WorkLocation>0 & wsLoc$WorkSegment %in% c(0,1,2,3,4,5),] %>%
  mutate(weight = 1/BUILD_SAMPLE_RATE) %>%
  group_by(WorkLocation, WorkSegment) %>%
  mutate(num_workers = sum(weight)) %>%
  select(WorkLocation, WorkSegment, num_workers)

ABM_Summary <- cast(workersbyMAZ, WorkLocation~WorkSegment, value = "num_workers", fun.aggregate = max)
ABM_Summary$`0`[is.infinite(ABM_Summary$`0`)] <- 0
ABM_Summary$`1`[is.infinite(ABM_Summary$`1`)] <- 0
ABM_Summary$`2`[is.infinite(ABM_Summary$`2`)] <- 0
ABM_Summary$`3`[is.infinite(ABM_Summary$`3`)] <- 0
ABM_Summary$`4`[is.infinite(ABM_Summary$`4`)] <- 0
ABM_Summary$`5`[is.infinite(ABM_Summary$`5`)] <- 0

colnames(ABM_Summary) <- c("mgra", "occ1", "occ2", "occ3", "occ4", "occ5", "occ6")


# compute jobs by occupation type
empCat <- colnames(occFac)[colnames(occFac)!="emp_code"]

mazData$occ1 <- 0
mazData$occ2 <- 0
mazData$occ3 <- 0
mazData$occ4 <- 0
mazData$occ5 <- 0
mazData$occ6 <- 0

for(cat in empCat){
  mazData$occ1 <- mazData$occ1 + mazData[,c(cat)]*occFac[1,c(cat)]
  mazData$occ2 <- mazData$occ2 + mazData[,c(cat)]*occFac[2,c(cat)]
  mazData$occ3 <- mazData$occ3 + mazData[,c(cat)]*occFac[3,c(cat)]
  mazData$occ4 <- mazData$occ4 + mazData[,c(cat)]*occFac[4,c(cat)]
  mazData$occ5 <- mazData$occ5 + mazData[,c(cat)]*occFac[5,c(cat)]
  mazData$occ6 <- mazData$occ6 + mazData[,c(cat)]*occFac[6,c(cat)]
}

### get df in right format before outputting
df1 <- mazData[,c("mgra", "hhs")] %>%
  left_join(ABM_Summary, by = c("mgra"="mgra")) %>%
  select(-hhs)

df1[is.na(df1)] <- 0
df1$Total <- rowSums(df1[,!colnames(df1) %in% c("mgra")])
df1[is.na(df1)] <- 0
df1 <- melt(df1, id = c("mgra"))
colnames(df1) <- c("mgra", "occp", "value")

df2 <- mazData[,c("mgra","occ1", "occ2", "occ3", "occ4", "occ5", "occ6")]
df2[is.na(df2)] <- 0
df2$Total <- rowSums(df2[,!colnames(df2) %in% c("mgra")])
df2[is.na(df2)] <- 0
df2 <- melt(df2, id = c("mgra"))
colnames(df2) <- c("mgra", "occp", "value")

df <- cbind(df1, df2$value)
colnames(df) <- c("mgra", "occp", "workers", "jobs")

df$DISTRICT <- mazCorrespondence$pmsa[match(df$mgra, mazCorrespondence$mgra)]

### Write outputs
write.csv(df, paste(WD, "job_worker_summary.csv", sep = "/"), row.names = F)

# finish