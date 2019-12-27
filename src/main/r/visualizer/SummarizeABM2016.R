#######################################################
### Script for summarizing SANDAG ABM Output
### Author: Binny M Paul, binny.paul@rsginc.com, Oct 2017
### Edited: Khademul Haque, khademul.haque@rsginc.com, Mar 2019
#######################################################

##### LIST OF ALL INPUT FILES #####
## 0. Path input data            : summ_inputs_abm.csv
## 1. household data             : householdData_3.csv
## 2. person data                : personData_3.csv
## 3. Individual tour data       : indivTourData_3.csv
## 4. Individual trip data       : indivTripData_3.csv
## 5. Joint tour data            : jointTripData_3.csv
## 6. Joint trip data            : jointTourData_3.csv
## 7. Work school location data  : wsLocResults_3.csv
## 8. Auto ownership data        : aoResults.csv
## 9. Auto ownership data        : aoResults_Pre.csv
## 10. Geographic crosswalk data : geographicXwalk_PMSA.csv
## 11. Distance skim             : traffic_skims_MD.omx -> MD_SOVTOLLH_DIST

start_time <- Sys.time()

library(plyr)
library(weights)
library(reshape)
library(data.table)
library(omxr)
showWarnings=FALSE

# Read Command Line Arguments
args          <- commandArgs(trailingOnly = TRUE)
inputs_File   <- args[1]

inputs        <- read.csv(inputs_File, header = TRUE)
WD            <- trimws(paste(inputs$Value[inputs$Key=="WD"]))
ABMOutputDir  <- trimws(paste(inputs$Value[inputs$Key=="ABMOutputDir"]))
geogXWalkDir  <- trimws(paste(inputs$Value[inputs$Key=="geogXWalkDir"]))
SkimDir       <- trimws(paste(inputs$Value[inputs$Key=="SkimDir"]))
MAX_ITER       <- trimws(paste(inputs$Value[inputs$Key=="MAX_ITER"]))

setwd(ABMOutputDir)
#full model run
hh                 <- fread(paste("householdData_",MAX_ITER,".csv", sep = ""))
per                <- fread(paste("personData_",MAX_ITER,".csv", sep = ""))
tours              <- fread(paste("indivTourData_",MAX_ITER,".csv", sep = ""))
trips              <- fread(paste("indivTripData_",MAX_ITER,".csv", sep = ""))
jtrips             <- fread(paste("jointTripData_",MAX_ITER,".csv", sep = ""))
unique_joint_tours <- fread(paste("jointTourData_",MAX_ITER,".csv", sep = ""))
wsLoc              <- fread(paste("wsLocResults_",MAX_ITER,".csv", sep = ""))
aoResults          <- fread("aoResults.csv")
aoResults_Pre      <- fread("aoResults_Pre.csv")

mazCorrespondence <- fread(paste(geogXWalkDir, "geographicXwalk_PMSA.csv", sep = "/"), stringsAsFactors = F)
districtList         <- sort(unique(mazCorrespondence$pmsa))

SkimFile <- paste(SkimDir, "traffic_skims_MD.omx", sep = "/")
DST_SKM <- read_omx(SkimFile, "MD_SOV_TR_H_DIST")
skimLookUp <- read_lookup(SkimFile, "zone_number")

pertypeCodes <- data.frame(code = c(1,2,3,4,5,6,7,8,"All"), 
                           name = c("FT Worker", "PT Worker", "Univ Stud", "Non Worker", "Retiree", "Driv Stud", "NonDriv Stud", "Pre-School", "All"))

#-------------------------------------------
# Prepare files for computing summary statistics
dir.create(WD, showWarnings = FALSE)
setwd(WD)

aoResults$HHVEH[aoResults$AO == 0] <- 0
aoResults$HHVEH[aoResults$AO == 1] <- 1
aoResults$HHVEH[aoResults$AO == 2] <- 2
aoResults$HHVEH[aoResults$AO == 3] <- 3
aoResults$HHVEH[aoResults$AO >= 4] <- 4

aoResults_Pre$HHVEH[aoResults_Pre$AO == 0] <- 0
aoResults_Pre$HHVEH[aoResults_Pre$AO == 1] <- 1
aoResults_Pre$HHVEH[aoResults_Pre$AO == 2] <- 2
aoResults_Pre$HHVEH[aoResults_Pre$AO == 3] <- 3
aoResults_Pre$HHVEH[aoResults_Pre$AO >= 4] <- 4

hh$HHVEH[hh$autos == 0] <- 0
hh$HHVEH[hh$autos == 1] <- 1
hh$HHVEH[hh$autos == 2] <- 2
hh$HHVEH[hh$autos == 3] <- 3
hh$HHVEH[hh$autos >= 4] <- 4

hh$VEH_NEWCAT[(hh$HVs == 0) & (hh$AVs) == 0] <- 1
hh$VEH_NEWCAT[(hh$HVs == 1) & (hh$AVs) == 0] <- 2
hh$VEH_NEWCAT[(hh$HVs == 0) & (hh$AVs) == 1] <- 3
hh$VEH_NEWCAT[(hh$HVs == 2) & (hh$AVs) == 0] <- 4
hh$VEH_NEWCAT[(hh$HVs == 0) & (hh$AVs) == 2] <- 5
hh$VEH_NEWCAT[(hh$HVs == 1) & (hh$AVs) == 1] <- 6
hh$VEH_NEWCAT[(hh$HVs == 3) & (hh$AVs) == 0] <- 7
hh$VEH_NEWCAT[(hh$HVs == 0) & (hh$AVs) == 3] <- 8
hh$VEH_NEWCAT[(hh$HVs == 2) & (hh$AVs) == 1] <- 9
hh$VEH_NEWCAT[(hh$HVs == 1) & (hh$AVs) == 2] <- 10
hh$VEH_NEWCAT[(hh$HVs == 4) & (hh$AVs) == 0] <- 11

#HH Size
hhsize <- count(per, c("hh_id"), "hh_id>0")
hh$HHSIZ <- hhsize$freq[match(hh$hh_id, hhsize$hh_id)]
hh$HHSIZE[hh$HHSIZ == 1] <- 1
hh$HHSIZE[hh$HHSIZ == 2] <- 2
hh$HHSIZE[hh$HHSIZ == 3] <- 3
hh$HHSIZE[hh$HHSIZ == 4] <- 4
hh$HHSIZE[hh$HHSIZ >= 5] <- 5

#Adults in the HH
adults <- count(per, c("hh_id"), "age>=18 & age<99")
hh$ADULTS <- adults$freq[match(hh$hh_id, adults$hh_id)]

per$PERTYPE[per$type=="Full-time worker"] <- 1
per$PERTYPE[per$type=="Part-time worker"] <- 2
per$PERTYPE[per$type=="University student"] <- 3
per$PERTYPE[per$type=="Non-worker"] <- 4
per$PERTYPE[per$type=="Retired"] <- 5
per$PERTYPE[per$type=="Student of driving age"] <- 6
per$PERTYPE[per$type=="Student of non-driving age"] <- 7
per$PERTYPE[per$type=="Child too young for school"] <- 8

# Districts are Pseudo MSA
wsLoc$HDISTRICT <- mazCorrespondence$pmsa[match(wsLoc$HomeMGRA, mazCorrespondence$mgra)]
wsLoc$WDISTRICT <- mazCorrespondence$pmsa[match(wsLoc$WorkLocation, mazCorrespondence$mgra)]

# Get home, work and school location TAZs
wsLoc$HHTAZ <- mazCorrespondence$taz[match(wsLoc$HomeMGRA, mazCorrespondence$mgra)]
wsLoc$WTAZ  <- mazCorrespondence$taz[match(wsLoc$WorkLocation, mazCorrespondence$mgra)]
wsLoc$STAZ  <- mazCorrespondence$taz[match(wsLoc$SchoolLocation, mazCorrespondence$mgra)]

wsLoc$oindex<-match(wsLoc$HHTAZ, skimLookUp$Lookup)
wsLoc$dindex<-match(wsLoc$WTAZ, skimLookUp$Lookup)
wsLoc$dindex2<-match(wsLoc$STAZ, skimLookUp$Lookup)
wsLoc$WorkLocationDistance<-DST_SKM[cbind(wsLoc$oindex, wsLoc$dindex)]
wsLoc$WorkLocationDistance[is.na(wsLoc$WorkLocationDistance)] <- 0

wsLoc$SchoolLocationDistance<-DST_SKM[cbind(wsLoc$oindex, wsLoc$dindex2)]
wsLoc$SchoolLocationDistance[is.na(wsLoc$SchoolLocationDistance)] <- 0

#--------Compute Summary Statistics-------
#*****************************************

# Auto ownership
autoOwnership_Pre <- count(aoResults_Pre, c("HHVEH"))
write.csv(autoOwnership_Pre, "autoOwnership_Pre.csv", row.names = TRUE)

autoOwnership <- count(aoResults, c("HHVEH"))
write.csv(autoOwnership, "autoOwnership.csv", row.names = TRUE)

autoOwnership_AV <- count(hh, c("AVs"))
write.csv(autoOwnership_AV, "autoOwnership_AV.csv", row.names = TRUE)

autoOwnership_new <- count(hh, c("VEH_NEWCAT"))
write.csv(autoOwnership_new, "autoOwnership_new.csv", row.names = TRUE)

# Zero auto HHs by TAZ
hh$HHTAZ <- mazCorrespondence$taz[match(hh$home_mgra, mazCorrespondence$mgra)]
hh$ZeroAutoWgt[hh$HHVEH==0] <- 1
hh$ZeroAutoWgt[is.na(hh$ZeroAutoWgt)] <- 0
zeroAutoByTaz <- aggregate(hh$ZeroAutoWgt, list(TAZ = hh$HHTAZ), sum)
write.csv(zeroAutoByTaz, "zeroAutoByTaz.csv", row.names = TRUE)

# Persons by person type
pertypeDistbn <- count(per, c("PERTYPE"))
write.csv(pertypeDistbn, "pertypeDistbn.csv", row.names = TRUE)

# Telecommute Freuency
teleCommute <- count(per, c("tele_choice"))
write.csv(teleCommute, "teleCommute_frequency.csv", row.names = TRUE)

# HH Transponder Ownership
transponder <- count(hh, c("transponder"))
write.csv(transponder, "transponder_ownership.csv", row.names = TRUE)


# Mandatory DC
workers <- wsLoc[wsLoc$WorkLocation > 0 & wsLoc$WorkLocation != 99999,]
students <- wsLoc[wsLoc$SchoolLocation > 0 & wsLoc$SchoolLocation != 88888,]

# code distance bins
workers$distbin <- cut(workers$WorkLocationDistance, breaks = c(seq(0,50, by=1), 9999), labels = F, right = F)
students$distbin <- cut(students$SchoolLocationDistance, breaks = c(seq(0,50, by=1), 9999), labels = F, right = F)

distBinCat <- data.frame(distbin = seq(1,51, by=1))
districtList_df <- data.frame(id = districtList)

# compute TLFDs by district and total
tlfd_work <- ddply(workers[,c("HDISTRICT", "distbin")], c("HDISTRICT", "distbin"), summarise, work = sum(HDISTRICT>0))
tlfd_work <- cast(tlfd_work, distbin~HDISTRICT, value = "work", sum)
work_ditbins <- tlfd_work$distbin
tlfd_work <- transpose(tlfd_work[,!colnames(tlfd_work) %in% c("distbin")])
tlfd_work$id <- row.names(tlfd_work)
tlfd_work <- merge(x = districtList_df, y = tlfd_work, by = "id", all.x = TRUE)
tlfd_work[is.na(tlfd_work)] <- 0
tlfd_work <- transpose(tlfd_work[,!colnames(tlfd_work) %in% c("id")])
tlfd_work <- cbind(data.frame(distbin = work_ditbins), tlfd_work)
tlfd_work$Total <- rowSums(tlfd_work[,!colnames(tlfd_work) %in% c("distbin")])
names(tlfd_work) <- sub("V", "District_", names(tlfd_work))
tlfd_work_df <- merge(x = distBinCat, y = tlfd_work, by = "distbin", all.x = TRUE)
tlfd_work_df[is.na(tlfd_work_df)] <- 0

tlfd_univ <- ddply(students[students$PersonType==3,c("HDISTRICT", "distbin")], c("HDISTRICT", "distbin"), summarise, univ = sum(HDISTRICT>0))
tlfd_univ <- cast(tlfd_univ, distbin~HDISTRICT, value = "univ", sum)
univ_ditbins <- tlfd_univ$distbin
tlfd_univ <- transpose(tlfd_univ[,!colnames(tlfd_univ) %in% c("distbin")])
tlfd_univ$id <- row.names(tlfd_univ)
tlfd_univ <- merge(x = districtList_df, y = tlfd_univ, by = "id", all.x = TRUE)
tlfd_univ[is.na(tlfd_univ)] <- 0
tlfd_univ <- transpose(tlfd_univ[,!colnames(tlfd_univ) %in% c("id")])
tlfd_univ <- cbind(data.frame(distbin = univ_ditbins), tlfd_univ)
tlfd_univ$Total <- rowSums(tlfd_univ[,!colnames(tlfd_univ) %in% c("distbin")])
names(tlfd_univ) <- sub("V", "District_", names(tlfd_univ))
tlfd_univ_df <- merge(x = distBinCat, y = tlfd_univ, by = "distbin", all.x = TRUE)
tlfd_univ_df[is.na(tlfd_univ_df)] <- 0

tlfd_schl <- ddply(students[students$PersonType>=6,c("HDISTRICT", "distbin")], c("HDISTRICT", "distbin"), summarise, schl = sum(HDISTRICT>0))
tlfd_schl <- cast(tlfd_schl, distbin~HDISTRICT, value = "schl", sum)
schl_ditbins <- tlfd_schl$distbin
tlfd_schl <- transpose(tlfd_schl[,!colnames(tlfd_schl) %in% c("distbin")])
tlfd_schl$id <- row.names(tlfd_schl)
tlfd_schl <- merge(x = districtList_df, y = tlfd_schl, by = "id", all.x = TRUE)
tlfd_schl[is.na(tlfd_schl)] <- 0
tlfd_schl <- transpose(tlfd_schl[,!colnames(tlfd_schl) %in% c("id")])
tlfd_schl <- cbind(data.frame(distbin = schl_ditbins), tlfd_schl)
tlfd_schl$Total <- rowSums(tlfd_schl[,!colnames(tlfd_schl) %in% c("distbin")])
names(tlfd_schl) <- sub("V", "District_", names(tlfd_schl))
tlfd_schl_df <- merge(x = distBinCat, y = tlfd_schl, by = "distbin", all.x = TRUE)
tlfd_schl_df[is.na(tlfd_schl_df)] <- 0

write.csv(tlfd_work_df, "workTLFD.csv", row.names = F)
write.csv(tlfd_univ_df, "univTLFD.csv", row.names = F)
write.csv(tlfd_schl_df, "schlTLFD.csv", row.names = F)

cat("\n Average distance to workplace (Total): ", mean(workers$WorkLocationDistance, na.rm = TRUE))
cat("\n Average distance to university (Total): ", mean(students$SchoolLocationDistance[students$PersonType==3], na.rm = TRUE))
cat("\n Average distance to school (Total): ", mean(students$SchoolLocationDistance[students$PersonType>=6], na.rm = TRUE))

## Output avg trip lengths for visualizer
workTripLengths <- ddply(workers[,c("HDISTRICT", "WorkLocationDistance")], c("HDISTRICT"), summarise, work = mean(WorkLocationDistance))
totalLength     <- data.frame("Total", mean(workers$WorkLocationDistance))
colnames(totalLength) <- colnames(workTripLengths)
workTripLengths <- rbind(workTripLengths, totalLength)

univTripLengths <- ddply(students[students$PersonType==3,c("HDISTRICT", "SchoolLocationDistance")], c("HDISTRICT"), summarise, univ = mean(SchoolLocationDistance))
totalLength     <- data.frame("Total", mean(students$SchoolLocationDistance[students$PersonType==3]))
colnames(totalLength) <- colnames(univTripLengths)
univTripLengths <- rbind(univTripLengths, totalLength)

schlTripLengths <- ddply(students[students$PersonType>=6,c("HDISTRICT", "SchoolLocationDistance")], c("HDISTRICT"), summarise, schl = mean(SchoolLocationDistance))
totalLength     <- data.frame("Total", mean(students$SchoolLocationDistance[students$PersonType>=6]))
colnames(totalLength) <- colnames(schlTripLengths)
schlTripLengths <- rbind(schlTripLengths, totalLength)

mandTripLengths <- cbind(workTripLengths, univTripLengths$univ, schlTripLengths$schl)
colnames(mandTripLengths) <- c("District", "Work", "Univ", "Schl")
write.csv(mandTripLengths, "mandTripLengths.csv", row.names = F)

# Work from home [for each district and total]
districtWorkers <- ddply(wsLoc[wsLoc$WorkLocation > 0,c("HDISTRICT")], c("HDISTRICT"), summarise, workers = sum(HDISTRICT>0))
districtWfh     <- ddply(wsLoc[wsLoc$WorkLocation==99999,c("HDISTRICT", "WorkLocation")], c("HDISTRICT"), summarise, wfh = sum(HDISTRICT>0))
wfh_summary     <- cbind(districtWorkers, districtWfh$wfh)
colnames(wfh_summary) <- c("District", "Workers", "WFH")
totalwfh        <- data.frame("Total", sum(wsLoc$WorkLocation>0), sum(wsLoc$WorkLocation==99999))
colnames(totalwfh) <- colnames(wfh_summary)
wfh_summary <- rbind(wfh_summary, totalwfh)
write.csv(wfh_summary, "wfh_summary.csv", row.names = F)
write.csv(totalwfh, "wfh_summary_region.csv", row.names = F)

# County-County Flows
countyFlows <- xtabs(~HDISTRICT+WDISTRICT, data = workers)
countyFlows[is.na(countyFlows)] <- 0
countyFlows <- addmargins(as.table(countyFlows))
countyFlows <- as.data.frame.matrix(countyFlows)
colnames(countyFlows)[colnames(countyFlows)=="Sum"] <- "Total"
colnames(countyFlows) <- paste("District", colnames(countyFlows), sep = "_")
rownames(countyFlows)[rownames(countyFlows)=="Sum"] <- "Total"
rownames(countyFlows) <- paste("District", rownames(countyFlows), sep = "_")
write.csv(countyFlows, "countyFlows.csv", row.names = T)

# Process Tour file
#------------------
tours$PERTYPE <- tours$person_type
tours$DISTMILE <- tours$tour_distance
tours$HHVEH <- hh$HHVEH[match(tours$hh_id, hh$hh_id)]
tours$ADULTS <- hh$ADULTS[match(tours$hh_id, hh$hh_id)]
tours$AUTOSUFF[tours$HHVEH == 0] <- 0
tours$AUTOSUFF[tours$HHVEH < tours$ADULTS & tours$HHVEH > 0] <- 1
tours$AUTOSUFF[tours$HHVEH >= tours$ADULTS & tours$HHVEH > 0] <- 2

tours$num_tot_stops <- tours$num_ob_stops + tours$num_ib_stops

tours$OTAZ <- mazCorrespondence$taz[match(tours$orig_mgra, mazCorrespondence$mgra)]
tours$DTAZ <- mazCorrespondence$taz[match(tours$dest_mgra, mazCorrespondence$mgra)]

tours$oindex<-match(tours$OTAZ, skimLookUp$Lookup)
tours$dindex<-match(tours$DTAZ, skimLookUp$Lookup)
tours$SKIMDIST<-DST_SKM[cbind(tours$oindex, tours$dindex)]


unique_joint_tours$HHVEH <- hh$HHVEH[match(unique_joint_tours$hh_id, hh$hh_id)]
unique_joint_tours$ADULTS <- hh$ADULTS[match(unique_joint_tours$hh_id, hh$hh_id)]
unique_joint_tours$AUTOSUFF[unique_joint_tours$HHVEH == 0] <- 0
unique_joint_tours$AUTOSUFF[unique_joint_tours$HHVEH < unique_joint_tours$ADULTS & unique_joint_tours$HHVEH > 0] <- 1
unique_joint_tours$AUTOSUFF[unique_joint_tours$HHVEH >= unique_joint_tours$ADULTS] <- 2

#Code tour purposes
tours$TOURPURP[tours$tour_purpose=="Work"] <- 1
tours$TOURPURP[tours$tour_purpose=="University"] <- 2
tours$TOURPURP[tours$tour_purpose=="School"] <- 3
tours$TOURPURP[tours$tour_purpose=="Escort"] <- 4
tours$TOURPURP[tours$tour_purpose=="Shop"] <- 5
tours$TOURPURP[tours$tour_purpose=="Maintenance"] <- 6
tours$TOURPURP[tours$tour_purpose=="Eating Out"] <- 7
tours$TOURPURP[tours$tour_purpose=="Visiting"] <- 8
tours$TOURPURP[tours$tour_purpose=="Discretionary"] <- 9
tours$TOURPURP[tours$tour_purpose=="Work-Based"] <- 10

#[0:Mandatory, 1: Indi Non Mand, 2: At Work]
tours$TOURCAT[tours$tour_purpose=="Work"] <- 0
tours$TOURCAT[tours$tour_purpose=="University"] <- 0
tours$TOURCAT[tours$tour_purpose=="School"] <- 0
tours$TOURCAT[tours$tour_purpose=="Escort"] <- 1
tours$TOURCAT[tours$tour_purpose=="Shop"] <- 1
tours$TOURCAT[tours$tour_purpose=="Maintenance"] <- 1
tours$TOURCAT[tours$tour_purpose=="Eating Out"] <- 1
tours$TOURCAT[tours$tour_purpose=="Visiting"] <- 1
tours$TOURCAT[tours$tour_purpose=="Discretionary"] <- 1
tours$TOURCAT[tours$tour_purpose=="Work-Based"] <- 2

#compute duration
tours$tourdur <- tours$end_period - tours$start_period + 1 #[to match survey]

tours$TOURMODE <- tours$tour_mode
#tours$TOURMODE[tours$tour_mode==1] <- 1
#tours$TOURMODE[tours$tour_mode==2] <- 2
#tours$TOURMODE[tours$tour_mode==3] <- 3
#tours$TOURMODE[tours$tour_mode>=7 & tours$tour_mode<=13] <- tours$tour_mode[tours$tour_mode>=7 & tours$tour_mode<=13]-3
#tours$TOURMODE[tours$tour_mode>=14 & tours$tour_mode<=15] <- 11
#tours$TOURMODE[tours$tour_mode==16] <- 12

# exclude school escorting stop from ride sharing mandatory tours

unique_joint_tours$JOINT_PURP[unique_joint_tours$tour_purpose=='Shop'] <- 5
unique_joint_tours$JOINT_PURP[unique_joint_tours$tour_purpose=='Maintenance'] <- 6
unique_joint_tours$JOINT_PURP[unique_joint_tours$tour_purpose=='Eating Out'] <- 7
unique_joint_tours$JOINT_PURP[unique_joint_tours$tour_purpose=='Visiting'] <- 8
unique_joint_tours$JOINT_PURP[unique_joint_tours$tour_purpose=='Discretionary'] <- 9

unique_joint_tours$NUMBER_HH <- as.integer((nchar(as.character(unique_joint_tours$tour_participants))+1)/2)

# get participant IDs
unique_joint_tours$PER1[unique_joint_tours$NUMBER_HH>=1] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=1]), 1, 1)
unique_joint_tours$PER2[unique_joint_tours$NUMBER_HH>=2] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=2]), 3, 3)
unique_joint_tours$PER3[unique_joint_tours$NUMBER_HH>=3] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=3]), 5, 5)
unique_joint_tours$PER4[unique_joint_tours$NUMBER_HH>=4] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=4]), 7, 7)
unique_joint_tours$PER5[unique_joint_tours$NUMBER_HH>=5] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=5]), 9, 9)
unique_joint_tours$PER6[unique_joint_tours$NUMBER_HH>=6] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=6]), 11, 11)
unique_joint_tours$PER7[unique_joint_tours$NUMBER_HH>=7] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=7]), 13, 13)
unique_joint_tours$PER8[unique_joint_tours$NUMBER_HH>=8] <- substr(as.character(unique_joint_tours$tour_participants[unique_joint_tours$NUMBER_HH>=8]), 15, 15)

unique_joint_tours[is.na(unique_joint_tours)] <- 0

# get person type for each participant
unique_joint_tours$PTYPE1 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER1, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE2 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER2, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE3 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER3, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE4 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER4, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE5 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER5, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE6 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER6, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE7 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER7, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]
unique_joint_tours$PTYPE8 <- per$PERTYPE[match(paste(unique_joint_tours$hh_id,unique_joint_tours$PER8, sep = "-"), paste(per$hh_id,per$person_num, sep = "-"))]

unique_joint_tours[is.na(unique_joint_tours)] <- 0

unique_joint_tours$num_tot_stops <- unique_joint_tours$num_ob_stops + unique_joint_tours$num_ib_stops

unique_joint_tours$OTAZ <- mazCorrespondence$taz[match(unique_joint_tours$orig_mgra, mazCorrespondence$mgra)]
unique_joint_tours$DTAZ <- mazCorrespondence$taz[match(unique_joint_tours$dest_mgra, mazCorrespondence$mgra)]

#compute duration
unique_joint_tours$tourdur <- unique_joint_tours$end_period - unique_joint_tours$start_period + 1 #[to match survye]

unique_joint_tours$TOURMODE <- unique_joint_tours$tour_mode
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode<=2] <- 1
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode>=3 & unique_joint_tours$tour_mode<=4] <- 2
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode>=5 & unique_joint_tours$tour_mode<=6] <- 3
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode>=7 & unique_joint_tours$tour_mode<=13] <- unique_joint_tours$tour_mode[unique_joint_tours$tour_mode>=7 & unique_joint_tours$tour_mode<=13]-3
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode>=14 & unique_joint_tours$tour_mode<=15] <- 11
#unique_joint_tours$TOURMODE[unique_joint_tours$tour_mode==16] <- 12

# ----
# this part is added by nagendra.dhakar@rsginc.com from binny.paul@rsginc.com soabm summaries

# create a combined temp tour file for creating stop freq model summary
temp_tour1 <- tours[,c("TOURPURP","num_ob_stops","num_ib_stops")]
temp_tour2 <- unique_joint_tours[,c("JOINT_PURP","num_ob_stops","num_ib_stops")]
colnames(temp_tour2) <- colnames(temp_tour1)
temp_tour <- rbind(temp_tour1,temp_tour2)

# code stop frequency model alternatives
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==0] <- 1
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==1] <- 2
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops==2] <- 3
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==0 & temp_tour$num_ib_stops>=3] <- 4
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==0] <- 5
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==1] <- 6
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops==2] <- 7
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==1 & temp_tour$num_ib_stops>=3] <- 8
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==0] <- 9
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==1] <- 10
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops==2] <- 11
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops==2 & temp_tour$num_ib_stops>=3] <- 12
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==0] <- 13
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==1] <- 14
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops==2] <- 15
temp_tour$STOP_FREQ_ALT[temp_tour$num_ob_stops>=3 & temp_tour$num_ib_stops>=3] <- 16
temp_tour$STOP_FREQ_ALT[is.na(temp_tour$STOP_FREQ_ALT)] <- 0

stopFreqModel_summary <- xtabs(~STOP_FREQ_ALT+TOURPURP, data = temp_tour[temp_tour$TOURPURP<=10,])
write.csv(stopFreqModel_summary, "stopFreqModel_summary.csv", row.names = T)

# ------

# Process Trip file
#------------------
trips$TOURMODE <- trips$tour_mode
trips$TRIPMODE <- trips$trip_mode

#trips$TOURMODE[trips$tour_mode<=2] <- 1
#trips$TOURMODE[trips$tour_mode>=3 & trips$tour_mode<=4] <- 2
#trips$TOURMODE[trips$tour_mode>=5 & trips$tour_mode<=6] <- 3
#trips$TOURMODE[trips$tour_mode>=7 & trips$tour_mode<=13] <- trips$tour_mode[trips$tour_mode>=7 & trips$tour_mode<=13]-3
#trips$TOURMODE[trips$tour_mode>=14 & trips$tour_mode<=15] <- 11
#trips$TOURMODE[trips$tour_mode==16] <- 12
#
#trips$TRIPMODE[trips$trip_mode<=2] <- 1
#trips$TRIPMODE[trips$trip_mode>=3 & trips$trip_mode<=4] <- 2
#trips$TRIPMODE[trips$trip_mode>=5 & trips$trip_mode<=6] <- 3
#trips$TRIPMODE[trips$trip_mode>=7 & trips$trip_mode<=13] <- trips$trip_mode[trips$trip_mode>=7 & trips$trip_mode<=13]-3
#trips$TRIPMODE[trips$trip_mode>=14 & trips$trip_mode<=15] <- 11
#trips$TRIPMODE[trips$trip_mode==16] <- 12

#Code tour purposes
trips$TOURPURP[trips$tour_purpose=="Home"] <- 0
trips$TOURPURP[trips$tour_purpose=="Work"] <- 1
trips$TOURPURP[trips$tour_purpose=="University"] <- 2
trips$TOURPURP[trips$tour_purpose=="School"] <- 3
trips$TOURPURP[trips$tour_purpose=="Escort"] <- 4
trips$TOURPURP[trips$tour_purpose=="Shop"] <- 5
trips$TOURPURP[trips$tour_purpose=="Maintenance"] <- 6
trips$TOURPURP[trips$tour_purpose=="Eating Out"] <- 7
trips$TOURPURP[trips$tour_purpose=="Visiting"] <- 8
trips$TOURPURP[trips$tour_purpose=="Discretionary"] <- 9
trips$TOURPURP[trips$tour_purpose=="Work-Based" | trips$tour_purpose=="work related"] <- 10

trips$OPURP[trips$orig_purpose=="Home"] <- 0
trips$OPURP[trips$orig_purpose=="Work"] <- 1
trips$OPURP[trips$orig_purpose=="University"] <- 2
trips$OPURP[trips$orig_purpose=="School"] <- 3
trips$OPURP[trips$orig_purpose=="Escort"] <- 4
trips$OPURP[trips$orig_purpose=="Shop"] <- 5
trips$OPURP[trips$orig_purpose=="Maintenance"] <- 6
trips$OPURP[trips$orig_purpose=="Eating Out"] <- 7
trips$OPURP[trips$orig_purpose=="Visiting"] <- 8
trips$OPURP[trips$orig_purpose=="Discretionary"] <- 9
trips$OPURP[trips$orig_purpose=="Work-Based" | trips$orig_purpose=="work related"] <- 10

trips$DPURP[trips$dest_purpose=="Home"] <- 0
trips$DPURP[trips$dest_purpose=="Work"] <- 1
trips$DPURP[trips$dest_purpose=="University"] <- 2
trips$DPURP[trips$dest_purpose=="School"] <- 3
trips$DPURP[trips$dest_purpose=="Escort"] <- 4
trips$DPURP[trips$dest_purpose=="Shop"] <- 5
trips$DPURP[trips$dest_purpose=="Maintenance"] <- 6
trips$DPURP[trips$dest_purpose=="Eating Out"] <- 7
trips$DPURP[trips$dest_purpose=="Visiting"] <- 8
trips$DPURP[trips$dest_purpose=="Discretionary"] <- 9
trips$DPURP[trips$dest_purpose=="Work-Based" | trips$dest_purpose=="work related"] <- 10

#[0:Mandatory, 1: Indi Non Mand, 3: At Work]
trips$TOURCAT[trips$tour_purpose=="Work"] <- 0
trips$TOURCAT[trips$tour_purpose=="University"] <- 0
trips$TOURCAT[trips$tour_purpose=="School"] <- 0
trips$TOURCAT[trips$tour_purpose=="Escort"] <- 1
trips$TOURCAT[trips$tour_purpose=="Shop"] <- 1
trips$TOURCAT[trips$tour_purpose=="Maintenance"] <- 1
trips$TOURCAT[trips$tour_purpose=="Eating Out"] <- 1
trips$TOURCAT[trips$tour_purpose=="Visiting"] <- 1
trips$TOURCAT[trips$tour_purpose=="Discretionary"] <- 1
trips$TOURCAT[trips$tour_purpose=="Work-Based"] <- 2

#Mark stops and get other attributes
nr <- nrow(trips)
trips$inb_next <- 0
trips$inb_next[1:nr-1] <- trips$inbound[2:nr]
trips$stops[trips$DPURP>0 & ((trips$inbound==0 & trips$inb_next==0) | (trips$inbound==1 & trips$inb_next==1))] <- 1
trips$stops[is.na(trips$stops)] <- 0

trips$OTAZ <- mazCorrespondence$taz[match(trips$orig_mgra, mazCorrespondence$mgra)]
trips$DTAZ <- mazCorrespondence$taz[match(trips$dest_mgra, mazCorrespondence$mgra)]

trips$TOUROTAZ <- tours$OTAZ[match(trips$hh_id*1000+trips$person_num*100+trips$TOURCAT*10+trips$tour_id, 
                                   tours$hh_id*1000+tours$person_num*100+tours$TOURCAT*10+tours$tour_id)]
trips$TOURDTAZ <- tours$DTAZ[match(trips$hh_id*1000+trips$person_num*100+trips$TOURCAT*10+trips$tour_id, 
                                   tours$hh_id*1000+tours$person_num*100+tours$TOURCAT*10+tours$tour_id)]	

# trips$od_dist <- DST_SKM$dist[match(paste(trips$OTAZ, trips$DTAZ, sep = "-"), paste(DST_SKM$o, DST_SKM$d, sep = "-"))]
trips$oindex<-match(trips$OTAZ, skimLookUp$Lookup)
trips$dindex<-match(trips$DTAZ, skimLookUp$Lookup)
trips$od_dist<-DST_SKM[cbind(trips$oindex, trips$dindex)]

#create stops table
stops <- trips[trips$stops==1,]

stops$finaldestTAZ[stops$inbound==0] <- stops$TOURDTAZ[stops$inbound==0]
stops$finaldestTAZ[stops$inbound==1] <- stops$TOUROTAZ[stops$inbound==1]

stops$oindex<-match(stops$OTAZ, skimLookUp$Lookup)
stops$dindex<-match(stops$finaldestTAZ, skimLookUp$Lookup)
stops$od_dist <- DST_SKM[cbind(stops$oindex, stops$dindex)]

stops$oindex2<-match(stops$OTAZ, skimLookUp$Lookup)
stops$dindex2<-match(stops$DTAZ, skimLookUp$Lookup)
stops$os_dist <- DST_SKM[cbind(stops$oindex2, stops$dindex2)]

stops$oindex3<-match(stops$DTAZ, skimLookUp$Lookup)
stops$dindex3<-match(stops$finaldestTAZ, skimLookUp$Lookup)
stops$sd_dist <- DST_SKM[cbind(stops$oindex3, stops$dindex3)]
  
stops$out_dir_dist <- stops$os_dist + stops$sd_dist - stops$od_dist									

#joint trip

jtrips$TOURMODE <- jtrips$tour_mode
#jtrips$TOURMODE[jtrips$tour_mode<=2] <- 1
#jtrips$TOURMODE[jtrips$tour_mode>=3 & jtrips$tour_mode<=4] <- 2
#jtrips$TOURMODE[jtrips$tour_mode>=5 & jtrips$tour_mode<=6] <- 3
#jtrips$TOURMODE[jtrips$tour_mode>=7 & jtrips$tour_mode<=13] <- jtrips$tour_mode[jtrips$tour_mode>=7 & jtrips$tour_mode<=13]-3
#jtrips$TOURMODE[jtrips$tour_mode>=14 & jtrips$tour_mode<=15] <- 11
#jtrips$TOURMODE[jtrips$tour_mode==16] <- 12

jtrips$TRIPMODE <- jtrips$trip_mode
#jtrips$TRIPMODE[jtrips$trip_mode<=2] <- 1
#jtrips$TRIPMODE[jtrips$trip_mode>=3 & jtrips$trip_mode<=4] <- 2
#jtrips$TRIPMODE[jtrips$trip_mode>=5 & jtrips$trip_mode<=6] <- 3
#jtrips$TRIPMODE[jtrips$trip_mode>=7 & jtrips$trip_mode<=13] <- jtrips$trip_mode[jtrips$trip_mode>=7 & jtrips$trip_mode<=13]-3
#jtrips$TRIPMODE[jtrips$trip_mode>=14 & jtrips$trip_mode<=15] <- 11
#jtrips$TRIPMODE[jtrips$trip_mode==16] <- 12

#Code joint tour purposes
jtrips$TOURPURP[jtrips$tour_purpose=="Work"] <- 1
jtrips$TOURPURP[jtrips$tour_purpose=="University"] <- 2
jtrips$TOURPURP[jtrips$tour_purpose=="School"] <- 3
jtrips$TOURPURP[jtrips$tour_purpose=="Escort"] <- 4
jtrips$TOURPURP[jtrips$tour_purpose=="Shop"] <- 5
jtrips$TOURPURP[jtrips$tour_purpose=="Maintenance"] <- 6
jtrips$TOURPURP[jtrips$tour_purpose=="Eating Out"] <- 7
jtrips$TOURPURP[jtrips$tour_purpose=="Visiting"] <- 8
jtrips$TOURPURP[jtrips$tour_purpose=="Discretionary"] <- 9
jtrips$TOURPURP[jtrips$tour_purpose=="Work-Based" | jtrips$tour_purpose=="work related"] <- 10

jtrips$OPURP[jtrips$orig_purpose=="Home"] <- 0
jtrips$OPURP[jtrips$orig_purpose=="Work"] <- 1
jtrips$OPURP[jtrips$orig_purpose=="University"] <- 2
jtrips$OPURP[jtrips$orig_purpose=="School"] <- 3
jtrips$OPURP[jtrips$orig_purpose=="Escort"] <- 4
jtrips$OPURP[jtrips$orig_purpose=="Shop"] <- 5
jtrips$OPURP[jtrips$orig_purpose=="Maintenance"] <- 6
jtrips$OPURP[jtrips$orig_purpose=="Eating Out"] <- 7
jtrips$OPURP[jtrips$orig_purpose=="Visiting"] <- 8
jtrips$OPURP[jtrips$orig_purpose=="Discretionary"] <- 9
jtrips$OPURP[jtrips$orig_purpose=="Work-Based" | jtrips$orig_purpose=="work related"] <- 10

jtrips$DPURP[jtrips$dest_purpose=="Home"] <- 0
jtrips$DPURP[jtrips$dest_purpose=="Work"] <- 1
jtrips$DPURP[jtrips$dest_purpose=="University"] <- 2
jtrips$DPURP[jtrips$dest_purpose=="School"] <- 3
jtrips$DPURP[jtrips$dest_purpose=="Escort"] <- 4
jtrips$DPURP[jtrips$dest_purpose=="Shop"] <- 5
jtrips$DPURP[jtrips$dest_purpose=="Maintenance"] <- 6
jtrips$DPURP[jtrips$dest_purpose=="Eating Out"] <- 7
jtrips$DPURP[jtrips$dest_purpose=="Visiting"] <- 8
jtrips$DPURP[jtrips$dest_purpose=="Discretionary"] <- 9
jtrips$DPURP[jtrips$dest_purpose=="Work-Based" | jtrips$dest_purpose=="work related"] <- 10

#[0:Mandatory, 1: Indi Non Mand, 3: At Work]
jtrips$TOURCAT[jtrips$tour_purpose=="Work"] <- 0
jtrips$TOURCAT[jtrips$tour_purpose=="University"] <- 0
jtrips$TOURCAT[jtrips$tour_purpose=="School"] <- 0
jtrips$TOURCAT[jtrips$tour_purpose=="Escort"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Shop"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Maintenance"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Eating Out"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Visiting"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Discretionary"] <- 1
jtrips$TOURCAT[jtrips$tour_purpose=="Work-Based"] <- 2

#Mark stops and get other attributes
nr <- nrow(jtrips)
jtrips$inb_next <- 0
jtrips$inb_next[1:nr-1] <- jtrips$inbound[2:nr]
jtrips$stops[jtrips$DPURP>0 & ((jtrips$inbound==0 & jtrips$inb_next==0) | (jtrips$inbound==1 & jtrips$inb_next==1))] <- 1
jtrips$stops[is.na(jtrips$stops)] <- 0

jtrips$OTAZ <- mazCorrespondence$taz[match(jtrips$orig_mgra, mazCorrespondence$mgra)]
jtrips$DTAZ <- mazCorrespondence$taz[match(jtrips$dest_mgra, mazCorrespondence$mgra)]

jtrips$TOUROTAZ <- unique_joint_tours$OTAZ[match(jtrips$hh_id*1000+jtrips$tour_id, 
                                                 unique_joint_tours$hh_id*1000+unique_joint_tours$tour_id)]
jtrips$TOURDTAZ <- unique_joint_tours$DTAZ[match(jtrips$hh_id*1000+jtrips$tour_id, 
                                                 unique_joint_tours$hh_id*1000+unique_joint_tours$tour_id)]	

#create stops table
jstops <- jtrips[jtrips$stops==1,]

jstops$finaldestTAZ[jstops$inbound==0] <- jstops$TOURDTAZ[jstops$inbound==0]
jstops$finaldestTAZ[jstops$inbound==1] <- jstops$TOUROTAZ[jstops$inbound==1]

jstops$oindex<-match(jstops$OTAZ, skimLookUp$Lookup)
jstops$dindex<-match(jstops$finaldestTAZ, skimLookUp$Lookup)
jstops$od_dist <- DST_SKM[cbind(jstops$oindex, jstops$dindex)]

jstops$oindex2<-match(jstops$OTAZ, skimLookUp$Lookup)
jstops$dindex2<-match(jstops$DTAZ, skimLookUp$Lookup)
jstops$os_dist <- DST_SKM[cbind(jstops$oindex2, jstops$dindex2)]

jstops$oindex3<-match(jstops$DTAZ, skimLookUp$Lookup)
jstops$dindex3<-match(jstops$finaldestTAZ, skimLookUp$Lookup)
jstops$sd_dist <- DST_SKM[cbind(jstops$oindex3, jstops$dindex3)]

jstops$out_dir_dist <- jstops$os_dist + jstops$sd_dist - jstops$od_dist		

#---------------------------------------------------------------------------

# Recode workrelated tours which are not at work subtour as work tour
#tours$TOURPURP[tours$TOURPURP == 10 & tours$IS_SUBTOUR == 0] <- 1

workCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 1") #[excluding at work subtours]
schlCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 2 | TOURPURP == 3")
inmCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP>=4 & TOURPURP<=9")

# -----------------------
# added for calibration by nagendra.dhakar@rsginc.com
# for indivudal NM tour generation
workCounts_temp <- workCounts
schlCounts_temp <- schlCounts
inmCounts_temp <- count(tours, c("hh_id", "person_num"), "TOURPURP>4 & TOURPURP<=9")  #excluding school escort tours
atWorkCounts_temp <- count(tours, c("hh_id", "person_num"), "TOURPURP == 10")
escortCounts_temp <- count(tours, c("hh_id", "person_num"), "TOURPURP==4")

  
colnames(workCounts_temp)[3] <- "freq_work"
colnames(schlCounts_temp)[3] <- "freq_schl"
colnames(inmCounts_temp)[3] <- "freq_inm"
colnames(atWorkCounts_temp)[3] <- "freq_atwork"
colnames(escortCounts_temp)[3] <- "freq_escort"

temp <- merge(workCounts_temp, schlCounts_temp, by = c("hh_id", "person_num"))
temp1 <- merge(temp, inmCounts_temp, by = c("hh_id", "person_num"))
temp1$freq_m <- temp1$freq_work + temp1$freq_schl
temp1$freq_itours <- temp1$freq_m+temp1$freq_inm

#joint tours
#identify persons that made joint tour
#temp_joint <- melt(unique_joint_tours[,c("hh_id","tour_id" ,"PER1", "PER2", "PER3", "PER4", "PER5", "PER6", "PER7", "PER8")], id = c("hh_id","tour_id"))
temp_joint <- melt(unique_joint_tours[,c("hh_id","tour_id" ,"PER1", "PER2", "PER3", "PER4", "PER5", "PER6", "PER7")], id = c("hh_id","tour_id"))
colnames(temp_joint) <- c("hh_id", "tour_id", "var", "person_num")
temp_joint <- as.data.frame(temp_joint)
temp_joint$person_num <- as.integer(temp_joint$person_num)
temp_joint$joint<- 0
temp_joint$joint[temp_joint$person_num>0] <- 1

temp_joint <- temp_joint[temp_joint$joint==1,]
person_unique_joint <- aggregate(joint~hh_id+person_num, temp_joint, sum)

temp2 <- merge(temp1, person_unique_joint, by = c("hh_id", "person_num"), all = T)
temp2 <- merge(temp2, atWorkCounts_temp, by = c("hh_id", "person_num"), all = T)
temp2 <- merge(temp2, escortCounts_temp, by = c("hh_id", "person_num"), all = T)
temp2[is.na(temp2)] <- 0

#add number of joint tours to non-mandatory
temp2$freq_nm <- temp2$freq_inm + temp2$joint

#get person type
temp2$PERTYPE <- per$PERTYPE[match(temp2$hh_id*10+temp2$person_num,per$hh_id*10+per$person_num)]

#total tours
temp2$total_tours <- temp2$freq_nm+temp2$freq_m+temp2$freq_atwork+temp2$freq_escort

persons_mand <- temp2[temp2$freq_m>0,]   #persons with atleast 1 mandatory tours
persons_nomand <- temp2[temp2$freq_m==0,] #active persons with 0 mandatory tours

freq_nmtours_mand <- count(persons_mand, c("PERTYPE","freq_nm"))
freq_nmtours_nomand <- count(persons_nomand, c("PERTYPE","freq_nm"))
test <- count(temp2, c("PERTYPE","freq_inm","freq_m","freq_nm","freq_atwork","freq_escort"))
write.csv(test, "tour_rate_debug.csv", row.names = F)
write.csv(temp2,"temp2.csv", row.names = F)

write.table("Non-Mandatory Tours for Persons with at-least 1 Mandatory Tour", "indivNMTourFreq.csv", sep = ",", row.names = F, append = F)
write.table(freq_nmtours_mand, "indivNMTourFreq.csv", sep = ",", row.names = F, append = T)
write.table("Non-Mandatory Tours for Active Persons with 0 Mandatory Tour", "indivNMTourFreq.csv", sep = ",", row.names = F, append = T)
write.table(freq_nmtours_nomand, "indivNMTourFreq.csv", sep = ",", row.names = F, append = TRUE)

# end of addition for calibration
# -----------------------


# ----------------------
# added for calibration by nagendra.dhakar@rsginc.com

i4tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 4")
i5tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 5")
i6tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 6")
i7tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 7")
i8tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 8")
i9tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP == 9")

# end of addition for calibration
# -----------------------

tourCounts <- count(tours, c("hh_id", "person_num"), "TOURPURP <= 9")  #number of tours per person [excluding at work subtours]
joint5 <- count(unique_joint_tours, c("hh_id"), "JOINT_PURP==5")
joint6 <- count(unique_joint_tours, c("hh_id"), "JOINT_PURP==6")
joint7 <- count(unique_joint_tours, c("hh_id"), "JOINT_PURP==7")
joint8 <- count(unique_joint_tours, c("hh_id"), "JOINT_PURP==8")
joint9 <- count(unique_joint_tours, c("hh_id"), "JOINT_PURP==9")

hh$joint5 <- joint5$freq[match(hh$hh_id, joint5$hh_id)]
hh$joint6 <- joint6$freq[match(hh$hh_id, joint6$hh_id)]
hh$joint7 <- joint7$freq[match(hh$hh_id, joint7$hh_id)]
hh$joint8 <- joint8$freq[match(hh$hh_id, joint8$hh_id)]
hh$joint9 <- joint9$freq[match(hh$hh_id, joint9$hh_id)]
hh$jtours <- hh$joint5+hh$joint6+hh$joint7+hh$joint8+hh$joint9

hh$joint5[is.na(hh$joint5)] <- 0
hh$joint6[is.na(hh$joint6)] <- 0
hh$joint7[is.na(hh$joint7)] <- 0
hh$joint8[is.na(hh$joint8)] <- 0
hh$joint9[is.na(hh$joint9)] <- 0
hh$jtours[is.na(hh$jtours)] <- 0

#joint tour indicator
hh$JOINT <- 0
hh$JOINT[substr(hh$cdap_pattern, nchar(as.character(hh$cdap_pattern)), nchar(as.character(hh$cdap_pattern)))=="j"] <- 1

# code JTF category
hh$jtf[hh$jtours==0] <- 1 
hh$jtf[hh$joint5==1] <- 2
hh$jtf[hh$joint6==1] <- 3
hh$jtf[hh$joint7==1] <- 4
hh$jtf[hh$joint8==1] <- 5
hh$jtf[hh$joint9==1] <- 6

hh$jtf[hh$joint5>=2] <- 7
hh$jtf[hh$joint6>=2] <- 8
hh$jtf[hh$joint7>=2] <- 9
hh$jtf[hh$joint8>=2] <- 10
hh$jtf[hh$joint9>=2] <- 11

hh$jtf[hh$joint5>=1 & hh$joint6>=1] <- 12
hh$jtf[hh$joint5>=1 & hh$joint7>=1] <- 13
hh$jtf[hh$joint5>=1 & hh$joint8>=1] <- 14
hh$jtf[hh$joint5>=1 & hh$joint9>=1] <- 15

hh$jtf[hh$joint6>=1 & hh$joint7>=1] <- 16
hh$jtf[hh$joint6>=1 & hh$joint8>=1] <- 17
hh$jtf[hh$joint6>=1 & hh$joint9>=1] <- 18

hh$jtf[hh$joint7>=1 & hh$joint8>=1] <- 19
hh$jtf[hh$joint7>=1 & hh$joint9>=1] <- 20

hh$jtf[hh$joint8>=1 & hh$joint9>=1] <- 21

per$workTours <- workCounts$freq[match(per$hh_id*10+per$person_num, workCounts$hh_id*10+workCounts$person_num)]
per$schlTours <- schlCounts$freq[match(per$hh_id*10+per$person_num, schlCounts$hh_id*10+schlCounts$person_num)]
per$inmTours <- inmCounts$freq[match(per$hh_id*10+per$person_num, inmCounts$hh_id*10+inmCounts$person_num)]
per$inmTours[is.na(per$inmTours)] <- 0
per$numTours <- tourCounts$freq[match(per$hh_id*10+per$person_num, tourCounts$hh_id*10+tourCounts$person_num)]
per$numTours[is.na(per$numTours)] <- 0

# ---------------------------------------------------
# added for calibration by nagendra.dhakar@rsginc.com

per$i4numTours <- i4tourCounts$freq[match(per$hh_id*10+per$person_num, i4tourCounts$hh_id*10+i4tourCounts$person_num)]
per$i4numTours[is.na(per$i4numTours)] <- 0
per$i5numTours <- i5tourCounts$freq[match(per$hh_id*10+per$person_num, i5tourCounts$hh_id*10+i5tourCounts$person_num)]
per$i5numTours[is.na(per$i5numTours)] <- 0
per$i6numTours <- i6tourCounts$freq[match(per$hh_id*10+per$person_num, i6tourCounts$hh_id*10+i6tourCounts$person_num)]
per$i6numTours[is.na(per$i6numTours)] <- 0
per$i7numTours <- i7tourCounts$freq[match(per$hh_id*10+per$person_num, i7tourCounts$hh_id*10+i7tourCounts$person_num)]
per$i7numTours[is.na(per$i7numTours)] <- 0
per$i8numTours <- i8tourCounts$freq[match(per$hh_id*10+per$person_num, i8tourCounts$hh_id*10+i8tourCounts$person_num)]
per$i8numTours[is.na(per$i8numTours)] <- 0
per$i9numTours <- i9tourCounts$freq[match(per$hh_id*10+per$person_num, i9tourCounts$hh_id*10+i9tourCounts$person_num)]
per$i9numTours[is.na(per$i9numTours)] <- 0

# end of addition for calibration
# ---------------------------------------------------

# Total tours by person type
per$numTours[is.na(per$numTours)] <- 0
toursPertypeDistbn <- count(tours[tours$PERTYPE>0 & tours$TOURPURP!=10,], c("PERTYPE"))
write.csv(toursPertypeDistbn, "toursPertypeDistbn.csv", row.names = TRUE)

# count joint tour fr each person type
temp_joint <- melt(unique_joint_tours[, c("hh_id","tour_id","PTYPE1","PTYPE2","PTYPE3","PTYPE4","PTYPE5","PTYPE6","PTYPE7","PTYPE8")], id = c("hh_id", "tour_id"))
names(temp_joint)[names(temp_joint)=="value"] <- "PERTYPE"
jtoursPertypeDistbn <- count(temp_joint[temp_joint$PERTYPE>0,], c("PERTYPE"))

# Total tours by person type for visualizer
totaltoursPertypeDistbn <- toursPertypeDistbn
totaltoursPertypeDistbn$freq <- totaltoursPertypeDistbn$freq + jtoursPertypeDistbn$freq
write.csv(totaltoursPertypeDistbn, "total_tours_by_pertype_vis.csv", row.names = F)

# Total indi NM tours by person type and purpose
tours_pertype_purpose <- count(tours[tours$TOURPURP>=4 & tours$TOURPURP<=9,], c("PERTYPE", "TOURPURP"))
write.csv(tours_pertype_purpose, "tours_pertype_purpose.csv", row.names = TRUE)

# ---------------------------------------------------
# added for calibration by nagendra.dhakar@rsginc.com

# code indi NM tour category
per$i4numTours[per$i4numTours>=2] <- 2
per$i5numTours[per$i5numTours>=2] <- 2
per$i6numTours[per$i6numTours>=2] <- 2
per$i7numTours[per$i7numTours>=1] <- 1
per$i8numTours[per$i8numTours>=1] <- 1
per$i9numTours[per$i9numTours>=2] <- 2

tours_pertype_esco <- count(per, c("PERTYPE", "i4numTours"))
tours_pertype_shop <- count(per, c("PERTYPE", "i5numTours"))
tours_pertype_main <- count(per, c("PERTYPE", "i6numTours"))
tours_pertype_eati <- count(per, c("PERTYPE", "i7numTours"))
tours_pertype_visi <- count(per, c("PERTYPE", "i8numTours"))
tours_pertype_disc <- count(per, c("PERTYPE", "i9numTours"))


colnames(tours_pertype_esco) <- c("PERTYPE","inumTours","freq")
colnames(tours_pertype_shop) <- c("PERTYPE","inumTours","freq")
colnames(tours_pertype_main) <- c("PERTYPE","inumTours","freq")
colnames(tours_pertype_eati) <- c("PERTYPE","inumTours","freq")
colnames(tours_pertype_visi) <- c("PERTYPE","inumTours","freq")
colnames(tours_pertype_disc) <- c("PERTYPE","inumTours","freq")

tours_pertype_esco$purpose <- 4
tours_pertype_shop$purpose <- 5
tours_pertype_main$purpose <- 6
tours_pertype_eati$purpose <- 7
tours_pertype_visi$purpose <- 8
tours_pertype_disc$purpose <- 9

indi_nm_tours_pertype <- rbind(tours_pertype_esco,tours_pertype_shop,tours_pertype_main,tours_pertype_eati,tours_pertype_visi,tours_pertype_disc)
write.csv(indi_nm_tours_pertype, "inmtours_pertype_purpose.csv", row.names = F)

# end of addition for calibration
# ---------------------------------------------------

tours_pertype_purpose <- xtabs(freq~PERTYPE+TOURPURP, tours_pertype_purpose)
tours_pertype_purpose[is.na(tours_pertype_purpose)] <- 0
tours_pertype_purpose <- addmargins(as.table(tours_pertype_purpose))
tours_pertype_purpose <- as.data.frame.matrix(tours_pertype_purpose)

totalPersons <- sum(pertypeDistbn$freq)
totalPersons_DF <- data.frame("Total", totalPersons)
colnames(totalPersons_DF) <- colnames(pertypeDistbn)
pertypeDF <- rbind(pertypeDistbn, totalPersons_DF)
nm_tour_rates <- tours_pertype_purpose/pertypeDF$freq
nm_tour_rates$pertype <- row.names(nm_tour_rates)
nm_tour_rates <- melt(nm_tour_rates, id = c("pertype"))
colnames(nm_tour_rates) <- c("pertype", "tour_purp", "tour_rate")
nm_tour_rates$pertype <- as.character(nm_tour_rates$pertype)
nm_tour_rates$tour_purp <- as.character(nm_tour_rates$tour_purp)
nm_tour_rates$pertype[nm_tour_rates$pertype=="Sum"] <- "All"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp=="Sum"] <- "All"
nm_tour_rates$pertype <- pertypeCodes$name[match(nm_tour_rates$pertype, pertypeCodes$code)]

nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==4] <- "Escorting"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==5] <- "Shopping"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==6] <- "Maintenance"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==7] <- "EatingOut"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==8] <- "Visiting"
nm_tour_rates$tour_purp[nm_tour_rates$tour_purp==9] <- "Discretionary"

write.csv(nm_tour_rates, "nm_tour_rates.csv", row.names = F)

# Total tours by purpose X tourtype
t1 <- hist(tours$TOURPURP[tours$TOURPURP<10], breaks = seq(1,10, by=1), freq = NULL, right=FALSE)
t3 <- hist(unique_joint_tours$JOINT_PURP, breaks = seq(1,10, by=1), freq = NULL, right=FALSE)
tours_purpose_type <- data.frame(t1$counts, t3$counts)
colnames(tours_purpose_type) <- c("indi", "joint")
write.csv(tours_purpose_type, "tours_purpose_type.csv", row.names = TRUE)

# DAP by pertype
# recode pattern type for at-work and home schooling persons.
# these person have DAP as M. They should be recoded to N or H.
per[per$activity_pattern == 'M' & per$imf_choice==0 & per$inmf_choice>0]$activity_pattern = 'N'
per[per$activity_pattern == 'M' & per$imf_choice==0 & per$inmf_choice==0]$activity_pattern = 'H'

dapSummary <- count(per, c("PERTYPE", "activity_pattern"))
write.csv(dapSummary, "dapSummary.csv", row.names = TRUE)

# Prepare DAP summary for visualizer
dapSummary_vis <- xtabs(freq~PERTYPE+activity_pattern, dapSummary)
dapSummary_vis <- addmargins(as.table(dapSummary_vis))
dapSummary_vis <- as.data.frame.matrix(dapSummary_vis)

dapSummary_vis$id <- row.names(dapSummary_vis)
dapSummary_vis <- melt(dapSummary_vis, id = c("id"))
colnames(dapSummary_vis) <- c("PERTYPE", "DAP", "freq")
dapSummary_vis$PERTYPE <- as.character(dapSummary_vis$PERTYPE)
dapSummary_vis$DAP <- as.character(dapSummary_vis$DAP)
dapSummary_vis <- dapSummary_vis[dapSummary_vis$DAP!="Sum",]
dapSummary_vis$PERTYPE[dapSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(dapSummary_vis, "dapSummary_vis.csv", row.names = TRUE)

# HHSize X Joint
hhsizeJoint <- count(hh[hh$HHSIZE>=2,], c("HHSIZE", "JOINT"))
write.csv(hhsizeJoint, "hhsizeJoint.csv", row.names = TRUE)

#mandatory tour frequency
mtfSummary <- count(per[per$imf_choice > 0,], c("PERTYPE", "imf_choice"))
write.csv(mtfSummary, "mtfSummary.csv")
#write.csv(tours, "tours_test.csv")

# Prepare MTF summary for visualizer
mtfSummary_vis <- xtabs(freq~PERTYPE+imf_choice, mtfSummary)
mtfSummary_vis <- addmargins(as.table(mtfSummary_vis))
mtfSummary_vis <- as.data.frame.matrix(mtfSummary_vis)

mtfSummary_vis$id <- row.names(mtfSummary_vis)
mtfSummary_vis <- melt(mtfSummary_vis, id = c("id"))
colnames(mtfSummary_vis) <- c("PERTYPE", "MTF", "freq")
mtfSummary_vis$PERTYPE <- as.character(mtfSummary_vis$PERTYPE)
mtfSummary_vis$MTF <- as.character(mtfSummary_vis$MTF)
mtfSummary_vis <- mtfSummary_vis[mtfSummary_vis$MTF!="Sum",]
mtfSummary_vis$PERTYPE[mtfSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(mtfSummary_vis, "mtfSummary_vis.csv")

# indi NM summary
inm0Summary <- count(per[per$inmTours==0,], c("PERTYPE"))
inm1Summary <- count(per[per$inmTours==1,], c("PERTYPE"))
inm2Summary <- count(per[per$inmTours==2,], c("PERTYPE"))
inm3Summary <- count(per[per$inmTours>=3,], c("PERTYPE"))

inmSummary <- data.frame(PERTYPE = c(1,2,3,4,5,6,7,8))
inmSummary$tour0 <- inm0Summary$freq[match(inmSummary$PERTYPE, inm0Summary$PERTYPE)]
inmSummary$tour1 <- inm1Summary$freq[match(inmSummary$PERTYPE, inm1Summary$PERTYPE)]
inmSummary$tour2 <- inm2Summary$freq[match(inmSummary$PERTYPE, inm2Summary$PERTYPE)]
inmSummary$tour3pl <- inm3Summary$freq[match(inmSummary$PERTYPE, inm3Summary$PERTYPE)]

write.table(inmSummary, "innmSummary.csv", col.names=TRUE, sep=",")

# prepare INM summary for visualizer
inmSummary_vis <- melt(inmSummary, id=c("PERTYPE"))
inmSummary_vis$variable <- as.character(inmSummary_vis$variable)
inmSummary_vis$variable[inmSummary_vis$variable=="tour0"] <- "0"
inmSummary_vis$variable[inmSummary_vis$variable=="tour1"] <- "1"
inmSummary_vis$variable[inmSummary_vis$variable=="tour2"] <- "2"
inmSummary_vis$variable[inmSummary_vis$variable=="tour3pl"] <- "3pl"
inmSummary_vis <- xtabs(value~PERTYPE+variable, inmSummary_vis)
inmSummary_vis <- addmargins(as.table(inmSummary_vis))
inmSummary_vis <- as.data.frame.matrix(inmSummary_vis)

inmSummary_vis$id <- row.names(inmSummary_vis)
inmSummary_vis <- melt(inmSummary_vis, id = c("id"))
colnames(inmSummary_vis) <- c("PERTYPE", "nmtours", "freq")
inmSummary_vis$PERTYPE <- as.character(inmSummary_vis$PERTYPE)
inmSummary_vis$nmtours <- as.character(inmSummary_vis$nmtours)
inmSummary_vis <- inmSummary_vis[inmSummary_vis$nmtours!="Sum",]
inmSummary_vis$PERTYPE[inmSummary_vis$PERTYPE=="Sum"] <- "Total"
write.csv(inmSummary_vis, "inmSummary_vis.csv")

# Joint Tour Frequency and composition
jtfSummary <- count(hh[!is.na(hh$jtf),], c("jtf"))
jointComp <- count(unique_joint_tours, c("tour_composition"))
jointPartySize <- count(unique_joint_tours, c("NUMBER_HH"))
jointCompPartySize <- count(unique_joint_tours, c("tour_composition","NUMBER_HH"))

hh$jointCat[hh$jtours==0] <- 0
hh$jointCat[hh$jtours==1] <- 1
hh$jointCat[hh$jtours>=2] <- 2

jointToursHHSize <- count(hh[!is.na(hh$HHSIZE) & !is.na(hh$jointCat),], c("HHSIZE", "jointCat"))

write.table(jtfSummary, "jtfSummary.csv", col.names=TRUE, sep=",")
write.table(jointComp, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
write.table(jointPartySize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
write.table(jointCompPartySize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)
write.table(jointToursHHSize, "jtfSummary.csv", col.names=TRUE, sep=",", append=TRUE)

#cap joint party size to 5+
jointPartySize$freq[jointPartySize$NUMBER_HH==5] <- sum(jointPartySize$freq[jointPartySize$NUMBER_HH>=5])
jointPartySize <- jointPartySize[jointPartySize$NUMBER_HH<=5, ]

jtf <- data.frame(jtf_code = seq(from = 1, to = 21), 
                  alt_name = c("No Joint Tours", "1 Shopping", "1 Maintenance", "1 Eating Out", "1 Visiting", "1 Other Discretionary", 
                               "2 Shopping", "1 Shopping / 1 Maintenance", "1 Shopping / 1 Eating Out", "1 Shopping / 1 Visiting", 
                               "1 Shopping / 1 Other Discretionary", "2 Maintenance", "1 Maintenance / 1 Eating Out", 
                               "1 Maintenance / 1 Visiting", "1 Maintenance / 1 Other Discretionary", "2 Eating Out", "1 Eating Out / 1 Visiting", 
                               "1 Eating Out / 1 Other Discretionary", "2 Visiting", "1 Visiting / 1 Other Discretionary", "2 Other Discretionary"))
jtf$freq <- jtfSummary$freq[match(jtf$jtf_code, jtfSummary$jtf)]
jtf[is.na(jtf)] <- 0

jointComp$tour_composition[jointComp$tour_composition==1] <- "All Adult"
jointComp$tour_composition[jointComp$tour_composition==2] <- "All Children"
jointComp$tour_composition[jointComp$tour_composition==3] <- "Mixed"

jointToursHHSizeProp <- xtabs(freq~jointCat+HHSIZE, jointToursHHSize[jointToursHHSize$HHSIZE>1,])
jointToursHHSizeProp <- addmargins(as.table(jointToursHHSizeProp))
jointToursHHSizeProp <- jointToursHHSizeProp[-4,]  #remove last row 
jointToursHHSizeProp <- prop.table(jointToursHHSizeProp, margin = 2)
jointToursHHSizeProp <- as.data.frame.matrix(jointToursHHSizeProp)
jointToursHHSizeProp <- jointToursHHSizeProp*100
jointToursHHSizeProp$jointTours <- row.names(jointToursHHSizeProp)
jointToursHHSizeProp <- melt(jointToursHHSizeProp, id = c("jointTours"))
colnames(jointToursHHSizeProp) <- c("jointTours", "hhsize", "freq")
jointToursHHSizeProp$hhsize <- as.character(jointToursHHSizeProp$hhsize)
jointToursHHSizeProp$hhsize[jointToursHHSizeProp$hhsize=="Sum"] <- "Total"

jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==1] <- "All Adult"
jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==2] <- "All Children"
jointCompPartySize$tour_composition[jointCompPartySize$tour_composition==3] <- "Mixed"

jointCompPartySizeProp <- xtabs(freq~tour_composition+NUMBER_HH, jointCompPartySize)
jointCompPartySizeProp <- addmargins(as.table(jointCompPartySizeProp))
jointCompPartySizeProp <- jointCompPartySizeProp[,-6]  #remove last row 
jointCompPartySizeProp <- prop.table(jointCompPartySizeProp, margin = 1)
jointCompPartySizeProp <- as.data.frame.matrix(jointCompPartySizeProp)
jointCompPartySizeProp <- jointCompPartySizeProp*100
jointCompPartySizeProp$comp <- row.names(jointCompPartySizeProp)
jointCompPartySizeProp <- melt(jointCompPartySizeProp, id = c("comp"))
colnames(jointCompPartySizeProp) <- c("comp", "partysize", "freq")
jointCompPartySizeProp$comp <- as.character(jointCompPartySizeProp$comp)
jointCompPartySizeProp$comp[jointCompPartySizeProp$comp=="Sum"] <- "Total"

# Cap joint comp party size at 5
jointCompPartySizeProp <- jointCompPartySizeProp[jointCompPartySizeProp$partysize!="Sum",]
jointCompPartySizeProp$partysize <- as.numeric(as.character(jointCompPartySizeProp$partysize))
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Adult" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Adult" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Children" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="All Children" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Mixed" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Mixed" & jointCompPartySizeProp$partysize>=5])
jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Total" & jointCompPartySizeProp$partysize==5] <- 
  sum(jointCompPartySizeProp$freq[jointCompPartySizeProp$comp=="Total" & jointCompPartySizeProp$partysize>=5])

jointCompPartySizeProp <- jointCompPartySizeProp[jointCompPartySizeProp$partysize<=5,]


write.csv(jtf, "jtf.csv", row.names = F)
write.csv(jointComp, "jointComp.csv", row.names = F)
write.csv(jointPartySize, "jointPartySize.csv", row.names = F)
write.csv(jointCompPartySizeProp, "jointCompPartySize.csv", row.names = F)
write.csv(jointToursHHSizeProp, "jointToursHHSize.csv", row.names = F)

# TOD Profile
#work.dep <- table(cut(tours$ANCHOR_DEPART_BIN[!is.na(tours$ANCHOR_DEPART_BIN)], seq(1,48, by=1), right=FALSE))

tod1 <- hist(tours$start_period[tours$TOURPURP==1], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod1_2 <- hist(tours$start_period[tours$TOURPURP==1 & tours$PERTYPE==2], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod2 <- hist(tours$start_period[tours$TOURPURP==2], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod3 <- hist(tours$start_period[tours$TOURPURP==3], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod4 <- hist(tours$start_period[tours$TOURPURP==4], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi56 <- hist(tours$start_period[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi789 <- hist(tours$start_period[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod6 <- hist(tours$start_period[tours$TOURPURP==6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod7 <- hist(tours$start_period[tours$TOURPURP==7], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod8 <- hist(tours$start_period[tours$TOURPURP==8], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod9 <- hist(tours$start_period[tours$TOURPURP==9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todj56 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todj789 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod11 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP==6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod12 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP==7], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod13 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP==8], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod14 <- hist(unique_joint_tours$start_period[unique_joint_tours$JOINT_PURP==9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod15 <- hist(tours$start_period[tours$TOURPURP==10], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)

todDepProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todDepProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todDepProfile, "todDepProfile.csv")

# prepare input for visualizer
todDepProfile_vis <- todDepProfile
todDepProfile_vis$id <- row.names(todDepProfile_vis)
todDepProfile_vis <- melt(todDepProfile_vis, id = c("id"))
colnames(todDepProfile_vis) <- c("id", "purpose", "freq_dep")

todDepProfile_vis$purpose <- as.character(todDepProfile_vis$purpose)
todDepProfile_vis <- xtabs(freq_dep~id+purpose, todDepProfile_vis)
todDepProfile_vis <- addmargins(as.table(todDepProfile_vis))
todDepProfile_vis <- as.data.frame.matrix(todDepProfile_vis)
todDepProfile_vis$id <- row.names(todDepProfile_vis)
todDepProfile_vis <- melt(todDepProfile_vis, id = c("id"))
colnames(todDepProfile_vis) <- c("timebin", "PURPOSE", "freq")
todDepProfile_vis$PURPOSE <- as.character(todDepProfile_vis$PURPOSE)
todDepProfile_vis$timebin <- as.character(todDepProfile_vis$timebin)
todDepProfile_vis <- todDepProfile_vis[todDepProfile_vis$timebin!="Sum",]
todDepProfile_vis$PURPOSE[todDepProfile_vis$PURPOSE=="Sum"] <- "Total"
todDepProfile_vis$timebin <- as.numeric(todDepProfile_vis$timebin)

tod1 <- hist(tours$end_period[tours$TOURPURP==1], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod1_2 <- hist(tours$end_period[tours$TOURPURP==1 & tours$PERTYPE==2], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod2 <- hist(tours$end_period[tours$TOURPURP==2], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod3 <- hist(tours$end_period[tours$TOURPURP==3], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod4 <- hist(tours$end_period[tours$TOURPURP==4], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi56 <- hist(tours$end_period[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi789 <- hist(tours$end_period[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod8 <- hist(tours$end_period[tours$TOURPURP==8], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod9 <- hist(tours$end_period[tours$TOURPURP==9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todj56 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todj789 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod11 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP==6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod12 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP==7], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod13 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP==8], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod14 <- hist(unique_joint_tours$end_period[unique_joint_tours$JOINT_PURP==9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod15 <- hist(tours$end_period[tours$TOURPURP==10], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)

todArrProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todArrProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todArrProfile, "todArrProfile.csv")


##stops by direction, purpose and model tod

tours$start_tod <- 5 # EA: 3 am - 6 am
tours$start_tod <- ifelse(tours$start_period>=4 & tours$start_period<=9, 1, tours$start_tod)   # AM: 6 am - 9 am
tours$start_tod <- ifelse(tours$start_period>=10 & tours$start_period<=22, 2, tours$start_tod) # MD: 9 am - 3:30 pm
tours$start_tod <- ifelse(tours$start_period>=23 & tours$start_period<=29, 3, tours$start_tod) # PM: 3:30 pm - 7 pm
tours$start_tod <- ifelse(tours$start_period>=30 & tours$start_period<=40, 4, tours$start_tod) # EV: 7 pm - 3 am

tours$end_tod <- 5 # EA: 3 am - 6 am
tours$end_tod <- ifelse(tours$end_period>=4 & tours$end_period<=9, 1, tours$end_tod)   # AM: 6 am - 9 am
tours$end_tod <- ifelse(tours$end_period>=10 & tours$end_period<=22, 2, tours$end_tod) # MD: 9 am - 3:30 pm
tours$end_tod <- ifelse(tours$end_period>=23 & tours$end_period<=29, 3, tours$end_tod) # PM: 3:30 pm - 7 pm
tours$end_tod <- ifelse(tours$end_period>=30 & tours$end_period<=40, 4, tours$end_tod) # EV: 7 pm - 3 am

stops_ib_tod <- aggregate(num_ib_stops~tour_purpose+start_tod+end_tod, data=tours, FUN = sum)
stops_ob_tod <- aggregate(num_ob_stops~tour_purpose+start_tod+end_tod, data=tours, FUN = sum)
write.csv(stops_ib_tod, "todStopsIB.csv", row.names = F)
write.csv(stops_ob_tod, "todStopsOB.csv", row.names = F)

#joint tours
unique_joint_tours$start_tod <- 5 # EA: 3 am - 6 am
unique_joint_tours$start_tod <- ifelse(unique_joint_tours$start_period>=4 & unique_joint_tours$start_period<=9, 1, unique_joint_tours$start_tod)   # AM: 6 am - 9 am
unique_joint_tours$start_tod <- ifelse(unique_joint_tours$start_period>=10 & unique_joint_tours$start_period<=22, 2, unique_joint_tours$start_tod) # MD: 9 am - 3:30 pm
unique_joint_tours$start_tod <- ifelse(unique_joint_tours$start_period>=23 & unique_joint_tours$start_period<=29, 3, unique_joint_tours$start_tod) # PM: 3:30 pm - 7 pm
unique_joint_tours$start_tod <- ifelse(unique_joint_tours$start_period>=30 & unique_joint_tours$start_period<=40, 4, unique_joint_tours$start_tod) # EV: 7 pm - 3 am

unique_joint_tours$end_tod <- 5 # EA: 3 am - 6 am
unique_joint_tours$end_tod <- ifelse(unique_joint_tours$end_period>=4 & unique_joint_tours$end_period<=9, 1, unique_joint_tours$end_tod)   # AM: 6 am - 9 am
unique_joint_tours$end_tod <- ifelse(unique_joint_tours$end_period>=10 & unique_joint_tours$end_period<=22, 2, unique_joint_tours$end_tod) # MD: 9 am - 3:30 pm
unique_joint_tours$end_tod <- ifelse(unique_joint_tours$end_period>=23 & unique_joint_tours$end_period<=29, 3, unique_joint_tours$end_tod) # PM: 3:30 pm - 7 pm
unique_joint_tours$end_tod <- ifelse(unique_joint_tours$end_period>=30 & unique_joint_tours$end_period<=40, 4, unique_joint_tours$end_tod) # EV: 7 pm - 3 am

jstops_ib_tod <- aggregate(num_ib_stops~tour_purpose+start_tod+end_tod, data=unique_joint_tours, FUN = sum)
jstops_ob_tod <- aggregate(num_ob_stops~tour_purpose+start_tod+end_tod, data=unique_joint_tours, FUN = sum)
write.csv(jstops_ib_tod, "todStopsIB_joint.csv", row.names = F)
write.csv(jstops_ob_tod, "todStopsOB_joint.csv", row.names = F)

# prepare input for visualizer
todArrProfile_vis <- todArrProfile
todArrProfile_vis$id <- row.names(todArrProfile_vis)
todArrProfile_vis <- melt(todArrProfile_vis, id = c("id"))
colnames(todArrProfile_vis) <- c("id", "purpose", "freq_arr")

todArrProfile_vis$purpose <- as.character(todArrProfile_vis$purpose)
todArrProfile_vis <- xtabs(freq_arr~id+purpose, todArrProfile_vis)
todArrProfile_vis <- addmargins(as.table(todArrProfile_vis))
todArrProfile_vis <- as.data.frame.matrix(todArrProfile_vis)
todArrProfile_vis$id <- row.names(todArrProfile_vis)
todArrProfile_vis <- melt(todArrProfile_vis, id = c("id"))
colnames(todArrProfile_vis) <- c("timebin", "PURPOSE", "freq")
todArrProfile_vis$PURPOSE <- as.character(todArrProfile_vis$PURPOSE)
todArrProfile_vis$timebin <- as.character(todArrProfile_vis$timebin)
todArrProfile_vis <- todArrProfile_vis[todArrProfile_vis$timebin!="Sum",]
todArrProfile_vis$PURPOSE[todArrProfile_vis$PURPOSE=="Sum"] <- "Total"
todArrProfile_vis$timebin <- as.numeric(todArrProfile_vis$timebin)


tod1 <- hist(tours$tourdur[tours$TOURPURP==1], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod1_2 <- hist(tours$tourdur[tours$TOURPURP==1 & tours$PERTYPE==2], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
tod2 <- hist(tours$tourdur[tours$TOURPURP==2], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod3 <- hist(tours$tourdur[tours$TOURPURP==3], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
tod4 <- hist(tours$tourdur[tours$TOURPURP==4], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi56 <- hist(tours$tourdur[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todi789 <- hist(tours$tourdur[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod6 <- hist(tours$tourdur[tours$TOURPURP==6], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod7 <- hist(tours$tourdur[tours$TOURPURP==7], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod8 <- hist(tours$tourdur[tours$TOURPURP==8], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod9 <- hist(tours$tourdur[tours$TOURPURP==9], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
todj56 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
todj789 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)
#tod11 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP==6], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod12 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP==7], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod13 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP==8], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
#tod14 <- hist(unique_joint_tours$tourdur[unique_joint_tours$JOINT_PURP==9], breaks = seq(0,41, by=1), freq = NULL, right=FALSE)
tod15 <- hist(tours$tourdur[tours$TOURPURP==10], breaks = seq(1,41, by=1), freq = NULL, right=FALSE)

todDurProfile <- data.frame(tod1$counts, tod2$counts, tod3$counts, tod4$counts, todi56$counts, todi789$counts
                            , todj56$counts, todj789$counts, tod15$counts)
colnames(todDurProfile) <- c("work", "univ", "sch", "esc", "imain", "idisc", 
                             "jmain", "jdisc", "atwork")
write.csv(todDurProfile, "todDurProfile.csv")

# prepare input for visualizer
todDurProfile_vis <- todDurProfile
todDurProfile_vis$id <- row.names(todDurProfile_vis)
todDurProfile_vis <- melt(todDurProfile_vis, id = c("id"))
colnames(todDurProfile_vis) <- c("id", "purpose", "freq_dur")

todDurProfile_vis$purpose <- as.character(todDurProfile_vis$purpose)
todDurProfile_vis <- xtabs(freq_dur~id+purpose, todDurProfile_vis)
todDurProfile_vis <- addmargins(as.table(todDurProfile_vis))
todDurProfile_vis <- as.data.frame.matrix(todDurProfile_vis)
todDurProfile_vis$id <- row.names(todDurProfile_vis)
todDurProfile_vis <- melt(todDurProfile_vis, id = c("id"))
colnames(todDurProfile_vis) <- c("timebin", "PURPOSE", "freq")
todDurProfile_vis$PURPOSE <- as.character(todDurProfile_vis$PURPOSE)
todDurProfile_vis$timebin <- as.character(todDurProfile_vis$timebin)
todDurProfile_vis <- todDurProfile_vis[todDurProfile_vis$timebin!="Sum",]
todDurProfile_vis$PURPOSE[todDurProfile_vis$PURPOSE=="Sum"] <- "Total"
todDurProfile_vis$timebin <- as.numeric(todDurProfile_vis$timebin)

todDepProfile_vis <- todDepProfile_vis[order(todDepProfile_vis$timebin, todDepProfile_vis$PURPOSE), ]
todArrProfile_vis <- todArrProfile_vis[order(todArrProfile_vis$timebin, todArrProfile_vis$PURPOSE), ]
todDurProfile_vis <- todDurProfile_vis[order(todDurProfile_vis$timebin, todDurProfile_vis$PURPOSE), ]
todProfile_vis <- data.frame(todDepProfile_vis, todArrProfile_vis$freq, todDurProfile_vis$freq)
colnames(todProfile_vis) <- c("id", "purpose", "freq_dep", "freq_arr", "freq_dur")
write.csv(todProfile_vis, "todProfile_vis.csv", row.names = F)

# Tour Mode X Auto Suff (seq changed from 10 to 13 due to increase in number of modes, changed by Khademul.haque@rsginc.com)
tmode1_as0 <- hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode2_as0 <- hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode3_as0 <- hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode4_as0 <- hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode5_as0 <- hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode6_as0 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=4 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode7_as0 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode8_as0 <- hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==0], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tmodeAS0Profile <- data.frame(tmode1_as0$counts, tmode2_as0$counts, tmode3_as0$counts, tmode4_as0$counts,
                              tmode5_as0$counts, tmode6_as0$counts, tmode7_as0$counts, tmode8_as0$counts)
colnames(tmodeAS0Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS0Profile, "tmodeAS0Profile.csv")

# Prepeare data for visualizer (changed from 9 to 12)
tmodeAS0Profile_vis <- tmodeAS0Profile[1:13,]
tmodeAS0Profile_vis$id <- row.names(tmodeAS0Profile_vis)
tmodeAS0Profile_vis <- melt(tmodeAS0Profile_vis, id = c("id"))
colnames(tmodeAS0Profile_vis) <- c("id", "purpose", "freq_as0")

tmodeAS0Profile_vis <- xtabs(freq_as0~id+purpose, tmodeAS0Profile_vis)
tmodeAS0Profile_vis[is.na(tmodeAS0Profile_vis)] <- 0
tmodeAS0Profile_vis <- addmargins(as.table(tmodeAS0Profile_vis))
tmodeAS0Profile_vis <- as.data.frame.matrix(tmodeAS0Profile_vis)

tmodeAS0Profile_vis$id <- row.names(tmodeAS0Profile_vis)
tmodeAS0Profile_vis <- melt(tmodeAS0Profile_vis, id = c("id"))
colnames(tmodeAS0Profile_vis) <- c("id", "purpose", "freq_as0")
tmodeAS0Profile_vis$id <- as.character(tmodeAS0Profile_vis$id)
tmodeAS0Profile_vis$purpose <- as.character(tmodeAS0Profile_vis$purpose)
tmodeAS0Profile_vis <- tmodeAS0Profile_vis[tmodeAS0Profile_vis$id!="Sum",]
tmodeAS0Profile_vis$purpose[tmodeAS0Profile_vis$purpose=="Sum"] <- "Total"

# (seq changed from 10 to 13 due to increase in number of modes, changed by Khademul.haque@rsginc.com)
tmode1_as1 <- hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode2_as1 <- hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode3_as1 <- hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode4_as1 <- hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode5_as1 <- hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode6_as1 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=4 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode7_as1 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode8_as1 <- hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tmodeAS1Profile <- data.frame(tmode1_as1$counts, tmode2_as1$counts, tmode3_as1$counts, tmode4_as1$counts,
                              tmode5_as1$counts, tmode6_as1$counts, tmode7_as1$counts, tmode8_as1$counts)
colnames(tmodeAS1Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS1Profile, "tmodeAS1Profile.csv")

# Prepeare data for visualizer (changed from 9 to 12)
tmodeAS1Profile_vis <- tmodeAS1Profile[1:13,]
tmodeAS1Profile_vis$id <- row.names(tmodeAS1Profile_vis)
tmodeAS1Profile_vis <- melt(tmodeAS1Profile_vis, id = c("id"))
colnames(tmodeAS1Profile_vis) <- c("id", "purpose", "freq_as1")

tmodeAS1Profile_vis <- xtabs(freq_as1~id+purpose, tmodeAS1Profile_vis)
tmodeAS1Profile_vis[is.na(tmodeAS1Profile_vis)] <- 0
tmodeAS1Profile_vis <- addmargins(as.table(tmodeAS1Profile_vis))
tmodeAS1Profile_vis <- as.data.frame.matrix(tmodeAS1Profile_vis)

tmodeAS1Profile_vis$id <- row.names(tmodeAS1Profile_vis)
tmodeAS1Profile_vis <- melt(tmodeAS1Profile_vis, id = c("id"))
colnames(tmodeAS1Profile_vis) <- c("id", "purpose", "freq_as1")
tmodeAS1Profile_vis$id <- as.character(tmodeAS1Profile_vis$id)
tmodeAS1Profile_vis$purpose <- as.character(tmodeAS1Profile_vis$purpose)
tmodeAS1Profile_vis <- tmodeAS1Profile_vis[tmodeAS1Profile_vis$id!="Sum",]
tmodeAS1Profile_vis$purpose[tmodeAS1Profile_vis$purpose=="Sum"] <- "Total"

# (seq changed from 10 to 13 due to increase in number of modes, changed by Khademul.haque@rsginc.com)
tmode1_as2 <- hist(tours$TOURMODE[tours$TOURPURP==1 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode2_as2 <- hist(tours$TOURMODE[tours$TOURPURP==2 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode3_as2 <- hist(tours$TOURMODE[tours$TOURPURP==3 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode4_as2 <- hist(tours$TOURMODE[tours$TOURPURP>=4 & tours$TOURPURP<=6 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode5_as2 <- hist(tours$TOURMODE[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode6_as2 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=4 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode7_as2 <- hist(unique_joint_tours$TOURMODE[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tmode8_as2 <- hist(tours$TOURMODE[tours$TOURPURP==10 & tours$AUTOSUFF==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tmodeAS2Profile <- data.frame(tmode1_as2$counts, tmode2_as2$counts, tmode3_as2$counts, tmode4_as2$counts,
                              tmode5_as2$counts, tmode6_as2$counts, tmode7_as2$counts, tmode8_as2$counts)
colnames(tmodeAS2Profile) <- c("work", "univ", "sch", "imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(tmodeAS2Profile, "tmodeAS2Profile.csv")

# Prepeare data for visualizer (changed from 9 to 12)
tmodeAS2Profile_vis <- tmodeAS2Profile[1:13,]
tmodeAS2Profile_vis$id <- row.names(tmodeAS2Profile_vis)
tmodeAS2Profile_vis <- melt(tmodeAS2Profile_vis, id = c("id"))
colnames(tmodeAS2Profile_vis) <- c("id", "purpose", "freq_as2")

tmodeAS2Profile_vis <- xtabs(freq_as2~id+purpose, tmodeAS2Profile_vis)
tmodeAS2Profile_vis[is.na(tmodeAS2Profile_vis)] <- 0
tmodeAS2Profile_vis <- addmargins(as.table(tmodeAS2Profile_vis))
tmodeAS2Profile_vis <- as.data.frame.matrix(tmodeAS2Profile_vis)

tmodeAS2Profile_vis$id <- row.names(tmodeAS2Profile_vis)
tmodeAS2Profile_vis <- melt(tmodeAS2Profile_vis, id = c("id"))
colnames(tmodeAS2Profile_vis) <- c("id", "purpose", "freq_as2")
tmodeAS2Profile_vis$id <- as.character(tmodeAS2Profile_vis$id)
tmodeAS2Profile_vis$purpose <- as.character(tmodeAS2Profile_vis$purpose)
tmodeAS2Profile_vis <- tmodeAS2Profile_vis[tmodeAS2Profile_vis$id!="Sum",]
tmodeAS2Profile_vis$purpose[tmodeAS2Profile_vis$purpose=="Sum"] <- "Total"


# Combine three AS groups
tmodeProfile_vis <- data.frame(tmodeAS0Profile_vis, tmodeAS1Profile_vis$freq_as1, tmodeAS2Profile_vis$freq_as2)
colnames(tmodeProfile_vis) <- c("id", "purpose", "freq_as0", "freq_as1", "freq_as2")
tmodeProfile_vis$freq_all <- tmodeProfile_vis$freq_as0 + tmodeProfile_vis$freq_as1 + tmodeProfile_vis$freq_as2
write.csv(tmodeProfile_vis, "tmodeProfile_vis.csv", row.names = F)


# Non Mand Tour lengths
tourdist4 <- hist(tours$tour_distance[tours$TOURPURP==4], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
tourdisti56 <- hist(tours$tour_distance[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
tourdisti789 <- hist(tours$tour_distance[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
tourdistj56 <- hist(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
tourdistj789 <- hist(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
tourdist10 <- hist(tours$tour_distance[tours$TOURPURP==10], breaks = c(seq(0,40, by=1), 9999), freq = NULL, right=FALSE)

tourDistProfile <- data.frame(tourdist4$counts, tourdisti56$counts, tourdisti789$counts, tourdistj56$counts, tourdistj789$counts, tourdist10$counts)

colnames(tourDistProfile) <- c("esco", "imain", "idisc", "jmain", "jdisc", "atwork")

write.csv(tourDistProfile, "nonMandTourDistProfile.csv")

#prepare input for visualizer
tourDistProfile_vis <- tourDistProfile
tourDistProfile_vis$id <- row.names(tourDistProfile_vis)
tourDistProfile_vis <- melt(tourDistProfile_vis, id = c("id"))
colnames(tourDistProfile_vis) <- c("id", "purpose", "freq")

tourDistProfile_vis <- xtabs(freq~id+purpose, tourDistProfile_vis)
tourDistProfile_vis <- addmargins(as.table(tourDistProfile_vis))
tourDistProfile_vis <- as.data.frame.matrix(tourDistProfile_vis)
tourDistProfile_vis$id <- row.names(tourDistProfile_vis)
tourDistProfile_vis <- melt(tourDistProfile_vis, id = c("id"))
colnames(tourDistProfile_vis) <- c("distbin", "PURPOSE", "freq")
tourDistProfile_vis$PURPOSE <- as.character(tourDistProfile_vis$PURPOSE)
tourDistProfile_vis$distbin <- as.character(tourDistProfile_vis$distbin)
tourDistProfile_vis <- tourDistProfile_vis[tourDistProfile_vis$distbin!="Sum",]
tourDistProfile_vis$PURPOSE[tourDistProfile_vis$PURPOSE=="Sum"] <- "Total"
tourDistProfile_vis$distbin <- as.numeric(tourDistProfile_vis$distbin)

write.csv(tourDistProfile_vis, "tourDistProfile_vis.csv", row.names = F)

cat("\n Average Tour Distance [esco]: ", mean(tours$tour_distance[tours$TOURPURP==4], na.rm = TRUE))
cat("\n Average Tour Distance [imain]: ", mean(tours$tour_distance[tours$TOURPURP>=5 & tours$TOURPURP<=6], na.rm = TRUE))
cat("\n Average Tour Distance [idisc]: ", mean(tours$tour_distance[tours$TOURPURP>=7 & tours$TOURPURP<=9], na.rm = TRUE))
cat("\n Average Tour Distance [jmain]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], na.rm = TRUE))
cat("\n Average Tour Distance [jdisc]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], na.rm = TRUE))
cat("\n Average Tour Distance [atwork]: ", mean(tours$tour_distance[tours$TOURPURP==10], na.rm = TRUE))

## Retirees
#cat("\n Average Tour Distance [esco]: ", mean(tours$tour_distance[tours$TOURPURP==4 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [imain]: ", mean(tours$tour_distance[tours$TOURPURP>=5 & tours$TOURPURP<=6 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [idisc]: ", mean(tours$tour_distance[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [jmain]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [jdisc]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$PERTYPE==5], na.rm = TRUE))
#cat("\n Average Tour Distance [atwork]: ", mean(tours$tour_distance[tours$TOURPURP==10 & tours$PERTYPE==5], na.rm = TRUE))
#
## Non-reitrees
#cat("\n Average Tour Distance [esco]: ", mean(tours$tour_distance[tours$TOURPURP==4 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [imain]: ", mean(tours$tour_distance[tours$TOURPURP>=5 & tours$TOURPURP<=6 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [idisc]: ", mean(tours$tour_distance[tours$TOURPURP>=7 & tours$TOURPURP<=9 & tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [jmain]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6 & unique_joint_tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [jdisc]: ", mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9 & unique_joint_tours$PERTYPE!=5], na.rm = TRUE))
#cat("\n Average Tour Distance [atwork]: ", mean(tours$tour_distance[tours$TOURPURP==10 & tours$PERTYPE!=5], na.rm = TRUE))
#

## Output average trips lengths for visualizer

avgTripLengths <- c(mean(tours$tour_distance[tours$TOURPURP==4], na.rm = TRUE),
                    mean(tours$tour_distance[tours$TOURPURP>=5 & tours$TOURPURP<=6], na.rm = TRUE),
                    mean(tours$tour_distance[tours$TOURPURP>=7 & tours$TOURPURP<=9], na.rm = TRUE),
                    mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], na.rm = TRUE),
                    mean(unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], na.rm = TRUE),
                    mean(tours$tour_distance[tours$TOURPURP==10], na.rm = TRUE))

totAvgNonMand <- mean(c(tours$tour_distance[tours$TOURPURP %in% c(4,5,6,7,8,9,10)], 
                        unique_joint_tours$tour_distance[unique_joint_tours$JOINT_PURP %in% c(5,6,7,8,9)]), 
                      na.rm = T)
avgTripLengths <- c(avgTripLengths, totAvgNonMand)

nonMandTourPurpose <- c("esco", "imain", "idisc", "jmain", "jdisc", "atwork", "Total") 

nonMandTripLengths <- data.frame(purpose = nonMandTourPurpose, avgTripLength = avgTripLengths)

write.csv(nonMandTripLengths, "nonMandTripLengths.csv", row.names = F)

# STop Frequency
#Outbound
stopfreq1 <- hist(tours$num_ob_stops[tours$TOURPURP==1], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(tours$num_ob_stops[tours$TOURPURP==2], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(tours$num_ob_stops[tours$TOURPURP==3], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(tours$num_ob_stops[tours$TOURPURP==4], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(tours$num_ob_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(tours$num_ob_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(unique_joint_tours$num_ob_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(unique_joint_tours$num_ob_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(tours$num_ob_stops[tours$TOURPURP==10], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqOutProfile.csv")

# prepare stop frequency input for visualizer
stopFreqout_vis <- stopFreq
stopFreqout_vis$id <- row.names(stopFreqout_vis)
stopFreqout_vis <- melt(stopFreqout_vis, id = c("id"))
colnames(stopFreqout_vis) <- c("id", "purpose", "freq")

stopFreqout_vis <- xtabs(freq~purpose+id, stopFreqout_vis)
stopFreqout_vis <- addmargins(as.table(stopFreqout_vis))
stopFreqout_vis <- as.data.frame.matrix(stopFreqout_vis)
stopFreqout_vis$id <- row.names(stopFreqout_vis)
stopFreqout_vis <- melt(stopFreqout_vis, id = c("id"))
colnames(stopFreqout_vis) <- c("purpose", "nstops", "freq")
stopFreqout_vis$purpose <- as.character(stopFreqout_vis$purpose)
stopFreqout_vis$nstops <- as.character(stopFreqout_vis$nstops)
stopFreqout_vis <- stopFreqout_vis[stopFreqout_vis$nstops!="Sum",]
stopFreqout_vis$purpose[stopFreqout_vis$purpose=="Sum"] <- "Total"

#Inbound
stopfreq1 <- hist(tours$num_ib_stops[tours$TOURPURP==1], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(tours$num_ib_stops[tours$TOURPURP==2], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(tours$num_ib_stops[tours$TOURPURP==3], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(tours$num_ib_stops[tours$TOURPURP==4], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(tours$num_ib_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(tours$num_ib_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(unique_joint_tours$num_ib_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(unique_joint_tours$num_ib_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(tours$num_ib_stops[tours$TOURPURP==10], breaks = c(seq(0,3, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqInbProfile.csv")

# prepare stop frequency input for visualizer
stopFreqinb_vis <- stopFreq
stopFreqinb_vis$id <- row.names(stopFreqinb_vis)
stopFreqinb_vis <- melt(stopFreqinb_vis, id = c("id"))
colnames(stopFreqinb_vis) <- c("id", "purpose", "freq")

stopFreqinb_vis <- xtabs(freq~purpose+id, stopFreqinb_vis)
stopFreqinb_vis <- addmargins(as.table(stopFreqinb_vis))
stopFreqinb_vis <- as.data.frame.matrix(stopFreqinb_vis)
stopFreqinb_vis$id <- row.names(stopFreqinb_vis)
stopFreqinb_vis <- melt(stopFreqinb_vis, id = c("id"))
colnames(stopFreqinb_vis) <- c("purpose", "nstops", "freq")
stopFreqinb_vis$purpose <- as.character(stopFreqinb_vis$purpose)
stopFreqinb_vis$nstops <- as.character(stopFreqinb_vis$nstops)
stopFreqinb_vis <- stopFreqinb_vis[stopFreqinb_vis$nstops!="Sum",]
stopFreqinb_vis$purpose[stopFreqinb_vis$purpose=="Sum"] <- "Total"


stopfreqDir_vis <- data.frame(stopFreqout_vis, stopFreqinb_vis$freq)
colnames(stopfreqDir_vis) <- c("purpose", "nstops", "freq_out", "freq_inb")
write.csv(stopfreqDir_vis, "stopfreqDir_vis.csv", row.names = F)


#Total
stopfreq1 <- hist(tours$num_tot_stops[tours$TOURPURP==1], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(tours$num_tot_stops[tours$TOURPURP==2], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(tours$num_tot_stops[tours$TOURPURP==3], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(tours$num_tot_stops[tours$TOURPURP==4], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(tours$num_tot_stops[tours$TOURPURP>=5 & tours$TOURPURP<=6], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(tours$num_tot_stops[tours$TOURPURP>=7 & tours$TOURPURP<=9], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(unique_joint_tours$num_tot_stops[unique_joint_tours$JOINT_PURP>=5 & unique_joint_tours$JOINT_PURP<=6], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(unique_joint_tours$num_tot_stops[unique_joint_tours$JOINT_PURP>=7 & unique_joint_tours$JOINT_PURP<=9], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(tours$num_tot_stops[tours$TOURPURP==10], breaks = c(seq(0,6, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopFreqTotProfile.csv")

# prepare stop frequency input for visualizer
stopFreq_vis <- stopFreq
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("id", "purpose", "freq")

stopFreq_vis <- xtabs(freq~purpose+id, stopFreq_vis)
stopFreq_vis <- addmargins(as.table(stopFreq_vis))
stopFreq_vis <- as.data.frame.matrix(stopFreq_vis)
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("purpose", "nstops", "freq")
stopFreq_vis$purpose <- as.character(stopFreq_vis$purpose)
stopFreq_vis$nstops <- as.character(stopFreq_vis$nstops)
stopFreq_vis <- stopFreq_vis[stopFreq_vis$nstops!="Sum",]
stopFreq_vis$purpose[stopFreq_vis$purpose=="Sum"] <- "Total"

write.csv(stopFreq_vis, "stopfreq_total_vis.csv", row.names = F)

#STop purpose X TourPurpose
stopfreq1 <- hist(stops$DPURP[stops$TOURPURP==1], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(stops$DPURP[stops$TOURPURP==2], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(stops$DPURP[stops$TOURPURP==3], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(stops$DPURP[stops$TOURPURP==4], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(stops$DPURP[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(stops$DPURP[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(jstops$DPURP[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(jstops$DPURP[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(stops$DPURP[stops$TOURPURP==10], breaks = c(seq(1,10, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopPurposeByTourPurpose.csv")

# prepare stop frequency input for visualizer
stopFreq_vis <- stopFreq
stopFreq_vis$id <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("id"))
colnames(stopFreq_vis) <- c("stop_purp", "purpose", "freq")

stopFreq_vis <- xtabs(freq~purpose+stop_purp, stopFreq_vis)
stopFreq_vis <- addmargins(as.table(stopFreq_vis))
stopFreq_vis <- as.data.frame.matrix(stopFreq_vis)
stopFreq_vis$purpose <- row.names(stopFreq_vis)
stopFreq_vis <- melt(stopFreq_vis, id = c("purpose"))
colnames(stopFreq_vis) <- c("purpose", "stop_purp", "freq")
stopFreq_vis$purpose <- as.character(stopFreq_vis$purpose)
stopFreq_vis$stop_purp <- as.character(stopFreq_vis$stop_purp)
stopFreq_vis <- stopFreq_vis[stopFreq_vis$stop_purp!="Sum",]
stopFreq_vis$purpose[stopFreq_vis$purpose=="Sum"] <- "Total"

write.csv(stopFreq_vis, "stoppurpose_tourpurpose_vis.csv", row.names = F)

#Out of direction - Stop Location
stopfreq1 <- hist(stops$out_dir_dist[stops$TOURPURP==1], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(stops$out_dir_dist[stops$TOURPURP==2], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(stops$out_dir_dist[stops$TOURPURP==3], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(stops$out_dir_dist[stops$TOURPURP==4], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(stops$out_dir_dist[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(stops$out_dir_dist[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(jstops$out_dir_dist[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(jstops$out_dir_dist[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(stops$out_dir_dist[stops$TOURPURP==10], breaks = c(-9999,seq(0,40, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopOutOfDirectionDC.csv")

# prepare stop location input for visualizer
stopDC_vis <- stopFreq
stopDC_vis$id <- row.names(stopDC_vis)
stopDC_vis <- melt(stopDC_vis, id = c("id"))
colnames(stopDC_vis) <- c("id", "purpose", "freq")

stopDC_vis <- xtabs(freq~id+purpose, stopDC_vis)
stopDC_vis <- addmargins(as.table(stopDC_vis))
stopDC_vis <- as.data.frame.matrix(stopDC_vis)
stopDC_vis$id <- row.names(stopDC_vis)
stopDC_vis <- melt(stopDC_vis, id = c("id"))
colnames(stopDC_vis) <- c("distbin", "PURPOSE", "freq")
stopDC_vis$PURPOSE <- as.character(stopDC_vis$PURPOSE)
stopDC_vis$distbin <- as.character(stopDC_vis$distbin)
stopDC_vis <- stopDC_vis[stopDC_vis$distbin!="Sum",]
stopDC_vis$PURPOSE[stopDC_vis$PURPOSE=="Sum"] <- "Total"
stopDC_vis$distbin <- as.numeric(stopDC_vis$distbin)

write.csv(stopDC_vis, "stopDC_vis.csv", row.names = F)

# compute average out of dir distance for visualizer
avgDistances <- c(mean(stops$out_dir_dist[stops$TOURPURP==1], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP==2], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP==3], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP==4], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP>=5 & stops$TOURPURP<=6], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP>=7 & stops$TOURPURP<=9], na.rm = TRUE),
                  mean(jstops$out_dir_dist[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], na.rm = TRUE),
                  mean(jstops$out_dir_dist[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], na.rm = TRUE),
                  mean(stops$out_dir_dist[stops$TOURPURP==10], na.rm = TRUE),
                  mean(stops$out_dir_dist, na.rm = TRUE))

purp <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork", "total")

avgStopOutofDirectionDist <- data.frame(purpose = purp, avgDist = avgDistances)

write.csv(avgStopOutofDirectionDist, "avgStopOutofDirectionDist_vis.csv", row.names = F)

#Stop Departure Time
stopfreq1 <- hist(stops$stop_period[stops$TOURPURP==1], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(stops$stop_period[stops$TOURPURP==2], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(stops$stop_period[stops$TOURPURP==3], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(stops$stop_period[stops$TOURPURP==4], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(stops$stop_period[stops$TOURPURP>=5 & stops$TOURPURP<=6], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(stops$stop_period[stops$TOURPURP>=7 & stops$TOURPURP<=9], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(jstops$stop_period[jstops$TOURPURP>=5 & jstops$TOURPURP<=6], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(jstops$stop_period[jstops$TOURPURP>=7 & jstops$TOURPURP<=9], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(stops$stop_period[stops$TOURPURP==10], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "stopDeparture.csv")

# prepare stop departure input for visualizer
stopDep_vis <- stopFreq
stopDep_vis$id <- row.names(stopDep_vis)
stopDep_vis <- melt(stopDep_vis, id = c("id"))
colnames(stopDep_vis) <- c("id", "purpose", "freq_stop")

stopDep_vis$purpose <- as.character(stopDep_vis$purpose)
stopDep_vis <- xtabs(freq_stop~id+purpose, stopDep_vis)
stopDep_vis <- addmargins(as.table(stopDep_vis))
stopDep_vis <- as.data.frame.matrix(stopDep_vis)
stopDep_vis$id <- row.names(stopDep_vis)
stopDep_vis <- melt(stopDep_vis, id = c("id"))
colnames(stopDep_vis) <- c("timebin", "PURPOSE", "freq")
stopDep_vis$PURPOSE <- as.character(stopDep_vis$PURPOSE)
stopDep_vis$timebin <- as.character(stopDep_vis$timebin)
stopDep_vis <- stopDep_vis[stopDep_vis$timebin!="Sum",]
stopDep_vis$PURPOSE[stopDep_vis$PURPOSE=="Sum"] <- "Total"
stopDep_vis$timebin <- as.numeric(stopDep_vis$timebin)

#Trip Departure Time
stopfreq1 <- hist(trips$stop_period[trips$TOURPURP==1], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq2 <- hist(trips$stop_period[trips$TOURPURP==2], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq3 <- hist(trips$stop_period[trips$TOURPURP==3], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq4 <- hist(trips$stop_period[trips$TOURPURP==4], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi56 <- hist(trips$stop_period[trips$TOURPURP>=5 & trips$TOURPURP<=6], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqi789 <- hist(trips$stop_period[trips$TOURPURP>=7 & trips$TOURPURP<=9], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj56 <- hist(jtrips$stop_period[jtrips$TOURPURP>=5 & jtrips$TOURPURP<=6], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreqj789 <- hist(jtrips$stop_period[jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)
stopfreq10 <- hist(trips$stop_period[trips$TOURPURP==10], breaks = c(seq(1,40, by=1), 9999), freq = NULL, right=FALSE)

stopFreq <- data.frame(stopfreq1$counts, stopfreq2$counts, stopfreq3$counts, stopfreq4$counts, stopfreqi56$counts
                       , stopfreqi789$counts, stopfreqj56$counts, stopfreqj789$counts, stopfreq10$counts)
colnames(stopFreq) <- c("work", "univ", "sch", "esco","imain", "idisc", "jmain", "jdisc", "atwork")
write.csv(stopFreq, "tripDeparture.csv")

# prepare stop departure input for visualizer
tripDep_vis <- stopFreq
tripDep_vis$id <- row.names(tripDep_vis)
tripDep_vis <- melt(tripDep_vis, id = c("id"))
colnames(tripDep_vis) <- c("id", "purpose", "freq_trip")

tripDep_vis$purpose <- as.character(tripDep_vis$purpose)
tripDep_vis <- xtabs(freq_trip~id+purpose, tripDep_vis)
tripDep_vis <- addmargins(as.table(tripDep_vis))
tripDep_vis <- as.data.frame.matrix(tripDep_vis)
tripDep_vis$id <- row.names(tripDep_vis)
tripDep_vis <- melt(tripDep_vis, id = c("id"))
colnames(tripDep_vis) <- c("timebin", "PURPOSE", "freq")
tripDep_vis$PURPOSE <- as.character(tripDep_vis$PURPOSE)
tripDep_vis$timebin <- as.character(tripDep_vis$timebin)
tripDep_vis <- tripDep_vis[tripDep_vis$timebin!="Sum",]
tripDep_vis$PURPOSE[tripDep_vis$PURPOSE=="Sum"] <- "Total"
tripDep_vis$timebin <- as.numeric(tripDep_vis$timebin)

stopTripDep_vis <- data.frame(stopDep_vis, tripDep_vis$freq)
colnames(stopTripDep_vis) <- c("id", "purpose", "freq_stop", "freq_trip")
write.csv(stopTripDep_vis, "stopTripDep_vis.csv", row.names = F)

#Trip Mode Summary (added 3 lines due to change in mode codes, changed seq 9 to 13, Khademul Haque)
#Work
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==1 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_Work.csv")

# Prepare data for visualizer (changed from 9 to 12)
tripModeProfile1_vis <- tripModeProfile[1:13,]
tripModeProfile1_vis$id <- row.names(tripModeProfile1_vis)
tripModeProfile1_vis <- melt(tripModeProfile1_vis, id = c("id"))
colnames(tripModeProfile1_vis) <- c("id", "purpose", "freq1")

tripModeProfile1_vis <- xtabs(freq1~id+purpose, tripModeProfile1_vis)
tripModeProfile1_vis[is.na(tripModeProfile1_vis)] <- 0
tripModeProfile1_vis <- addmargins(as.table(tripModeProfile1_vis))
tripModeProfile1_vis <- as.data.frame.matrix(tripModeProfile1_vis)

tripModeProfile1_vis$id <- row.names(tripModeProfile1_vis)
tripModeProfile1_vis <- melt(tripModeProfile1_vis, id = c("id"))
colnames(tripModeProfile1_vis) <- c("id", "purpose", "freq1")
tripModeProfile1_vis$id <- as.character(tripModeProfile1_vis$id)
tripModeProfile1_vis$purpose <- as.character(tripModeProfile1_vis$purpose)
tripModeProfile1_vis <- tripModeProfile1_vis[tripModeProfile1_vis$id!="Sum",]
tripModeProfile1_vis$purpose[tripModeProfile1_vis$purpose=="Sum"] <- "Total"


#University (added 3 lines due to change in mode codes, changed seq 9 to 13, Khademul Haque)
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==2 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)


tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_Univ.csv")

tripModeProfile2_vis <- tripModeProfile[1:13,]
tripModeProfile2_vis$id <- row.names(tripModeProfile2_vis)
tripModeProfile2_vis <- melt(tripModeProfile2_vis, id = c("id"))
colnames(tripModeProfile2_vis) <- c("id", "purpose", "freq2")

tripModeProfile2_vis <- xtabs(freq2~id+purpose, tripModeProfile2_vis)
tripModeProfile2_vis[is.na(tripModeProfile2_vis)] <- 0
tripModeProfile2_vis <- addmargins(as.table(tripModeProfile2_vis))
tripModeProfile2_vis <- as.data.frame.matrix(tripModeProfile2_vis)

tripModeProfile2_vis$id <- row.names(tripModeProfile2_vis)
tripModeProfile2_vis <- melt(tripModeProfile2_vis, id = c("id"))
colnames(tripModeProfile2_vis) <- c("id", "purpose", "freq2")
tripModeProfile2_vis$id <- as.character(tripModeProfile2_vis$id)
tripModeProfile2_vis$purpose <- as.character(tripModeProfile2_vis$purpose)
tripModeProfile2_vis <- tripModeProfile2_vis[tripModeProfile2_vis$id!="Sum",]
tripModeProfile2_vis$purpose[tripModeProfile2_vis$purpose=="Sum"] <- "Total"

#School
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==3 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)


tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_Schl.csv")

tripModeProfile3_vis <- tripModeProfile[1:13,]
tripModeProfile3_vis$id <- row.names(tripModeProfile3_vis)
tripModeProfile3_vis <- melt(tripModeProfile3_vis, id = c("id"))
colnames(tripModeProfile3_vis) <- c("id", "purpose", "freq3")

tripModeProfile3_vis <- xtabs(freq3~id+purpose, tripModeProfile3_vis)
tripModeProfile3_vis[is.na(tripModeProfile3_vis)] <- 0
tripModeProfile3_vis <- addmargins(as.table(tripModeProfile3_vis))
tripModeProfile3_vis <- as.data.frame.matrix(tripModeProfile3_vis)

tripModeProfile3_vis$id <- row.names(tripModeProfile3_vis)
tripModeProfile3_vis <- melt(tripModeProfile3_vis, id = c("id"))
colnames(tripModeProfile3_vis) <- c("id", "purpose", "freq3")
tripModeProfile3_vis$id <- as.character(tripModeProfile3_vis$id)
tripModeProfile3_vis$purpose <- as.character(tripModeProfile3_vis$purpose)
tripModeProfile3_vis <- tripModeProfile3_vis[tripModeProfile3_vis$id!="Sum",]
tripModeProfile3_vis$purpose[tripModeProfile3_vis$purpose=="Sum"] <- "Total"

#iMain
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=4 & trips$TOURPURP<=6 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                               tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_iMain.csv")

tripModeProfile4_vis <- tripModeProfile[1:13,]
tripModeProfile4_vis$id <- row.names(tripModeProfile4_vis)
tripModeProfile4_vis <- melt(tripModeProfile4_vis, id = c("id"))
colnames(tripModeProfile4_vis) <- c("id", "purpose", "freq4")

tripModeProfile4_vis <- xtabs(freq4~id+purpose, tripModeProfile4_vis)
tripModeProfile4_vis[is.na(tripModeProfile4_vis)] <- 0
tripModeProfile4_vis <- addmargins(as.table(tripModeProfile4_vis))
tripModeProfile4_vis <- as.data.frame.matrix(tripModeProfile4_vis)

tripModeProfile4_vis$id <- row.names(tripModeProfile4_vis)
tripModeProfile4_vis <- melt(tripModeProfile4_vis, id = c("id"))
colnames(tripModeProfile4_vis) <- c("id", "purpose", "freq4")
tripModeProfile4_vis$id <- as.character(tripModeProfile4_vis$id)
tripModeProfile4_vis$purpose <- as.character(tripModeProfile4_vis$purpose)
tripModeProfile4_vis <- tripModeProfile4_vis[tripModeProfile4_vis$id!="Sum",]
tripModeProfile4_vis$purpose[tripModeProfile4_vis$purpose=="Sum"] <- "Total"

#iDisc
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP>=7 & trips$TOURPURP<=9 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_iDisc.csv")

tripModeProfile5_vis <- tripModeProfile[1:13,]
tripModeProfile5_vis$id <- row.names(tripModeProfile5_vis)
tripModeProfile5_vis <- melt(tripModeProfile5_vis, id = c("id"))
colnames(tripModeProfile5_vis) <- c("id", "purpose", "freq5")

tripModeProfile5_vis <- xtabs(freq5~id+purpose, tripModeProfile5_vis)
tripModeProfile5_vis[is.na(tripModeProfile5_vis)] <- 0
tripModeProfile5_vis <- addmargins(as.table(tripModeProfile5_vis))
tripModeProfile5_vis <- as.data.frame.matrix(tripModeProfile5_vis)

tripModeProfile5_vis$id <- row.names(tripModeProfile5_vis)
tripModeProfile5_vis <- melt(tripModeProfile5_vis, id = c("id"))
colnames(tripModeProfile5_vis) <- c("id", "purpose", "freq5")
tripModeProfile5_vis$id <- as.character(tripModeProfile5_vis$id)
tripModeProfile5_vis$purpose <- as.character(tripModeProfile5_vis$purpose)
tripModeProfile5_vis <- tripModeProfile5_vis[tripModeProfile5_vis$id!="Sum",]
tripModeProfile5_vis$purpose[tripModeProfile5_vis$purpose=="Sum"] <- "Total"

#jMain
tripmode1 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=4 & jtrips$TOURPURP<=6 & jtrips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_jMain.csv")

tripModeProfile6_vis <- tripModeProfile[1:13,]
tripModeProfile6_vis$id <- row.names(tripModeProfile6_vis)
tripModeProfile6_vis <- melt(tripModeProfile6_vis, id = c("id"))
colnames(tripModeProfile6_vis) <- c("id", "purpose", "freq6")

tripModeProfile6_vis <- xtabs(freq6~id+purpose, tripModeProfile6_vis)
tripModeProfile6_vis[is.na(tripModeProfile6_vis)] <- 0
tripModeProfile6_vis <- addmargins(as.table(tripModeProfile6_vis))
tripModeProfile6_vis <- as.data.frame.matrix(tripModeProfile6_vis)

tripModeProfile6_vis$id <- row.names(tripModeProfile6_vis)
tripModeProfile6_vis <- melt(tripModeProfile6_vis, id = c("id"))
colnames(tripModeProfile6_vis) <- c("id", "purpose", "freq6")
tripModeProfile6_vis$id <- as.character(tripModeProfile6_vis$id)
tripModeProfile6_vis$purpose <- as.character(tripModeProfile6_vis$purpose)
tripModeProfile6_vis <- tripModeProfile6_vis[tripModeProfile6_vis$id!="Sum",]
tripModeProfile6_vis$purpose[tripModeProfile6_vis$purpose=="Sum"] <- "Total"

#jDisc
tripmode1 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURPURP>=7 & jtrips$TOURPURP<=9 & jtrips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_jDisc.csv")

tripModeProfile7_vis <- tripModeProfile[1:13,]
tripModeProfile7_vis$id <- row.names(tripModeProfile7_vis)
tripModeProfile7_vis <- melt(tripModeProfile7_vis, id = c("id"))
colnames(tripModeProfile7_vis) <- c("id", "purpose", "freq7")

tripModeProfile7_vis <- xtabs(freq7~id+purpose, tripModeProfile7_vis)
tripModeProfile7_vis[is.na(tripModeProfile7_vis)] <- 0
tripModeProfile7_vis <- addmargins(as.table(tripModeProfile7_vis))
tripModeProfile7_vis <- as.data.frame.matrix(tripModeProfile7_vis)

tripModeProfile7_vis$id <- row.names(tripModeProfile7_vis)
tripModeProfile7_vis <- melt(tripModeProfile7_vis, id = c("id"))
colnames(tripModeProfile7_vis) <- c("id", "purpose", "freq7")
tripModeProfile7_vis$id <- as.character(tripModeProfile7_vis$id)
tripModeProfile7_vis$purpose <- as.character(tripModeProfile7_vis$purpose)
tripModeProfile7_vis <- tripModeProfile7_vis[tripModeProfile7_vis$id!="Sum",]
tripModeProfile7_vis$purpose[tripModeProfile7_vis$purpose=="Sum"] <- "Total"

#At work
tripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
tripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURPURP==10 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(tripmode1$counts, tripmode2$counts, tripmode3$counts, tripmode4$counts,
                              tripmode5$counts, tripmode6$counts, tripmode7$counts, tripmode8$counts, tripmode9$counts,
                              tripmode10$counts, tripmode11$counts, tripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_AtWork.csv")

tripModeProfile8_vis <- tripModeProfile[1:13,]
tripModeProfile8_vis$id <- row.names(tripModeProfile8_vis)
tripModeProfile8_vis <- melt(tripModeProfile8_vis, id = c("id"))
colnames(tripModeProfile8_vis) <- c("id", "purpose", "freq8")

tripModeProfile8_vis <- xtabs(freq8~id+purpose, tripModeProfile8_vis)
tripModeProfile8_vis[is.na(tripModeProfile8_vis)] <- 0
tripModeProfile8_vis <- addmargins(as.table(tripModeProfile8_vis))
tripModeProfile8_vis <- as.data.frame.matrix(tripModeProfile8_vis)

tripModeProfile8_vis$id <- row.names(tripModeProfile8_vis)
tripModeProfile8_vis <- melt(tripModeProfile8_vis, id = c("id"))
colnames(tripModeProfile8_vis) <- c("id", "purpose", "freq8")
tripModeProfile8_vis$id <- as.character(tripModeProfile8_vis$id)
tripModeProfile8_vis$purpose <- as.character(tripModeProfile8_vis$purpose)
tripModeProfile8_vis <- tripModeProfile8_vis[tripModeProfile8_vis$id!="Sum",]
tripModeProfile8_vis$purpose[tripModeProfile8_vis$purpose=="Sum"] <- "Total"

#iTotal
itripmode1 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode2 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode3 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode4 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode5 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode6 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode7 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode8 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode9 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode10 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode11 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
itripmode12 <- hist(trips$TRIPMODE[trips$TRIPMODE>0 & trips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

#jTotal
jtripmode1 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==1], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode2 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==2], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode3 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==3], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode4 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==4], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode5 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==5], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode6 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==6], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode7 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==7], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode8 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==8], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode9 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==9], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode10 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==10], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode11 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==11], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)
jtripmode12 <- hist(jtrips$TRIPMODE[jtrips$TRIPMODE>0 & jtrips$TOURMODE==12], breaks = seq(1,14, by=1), freq = NULL, right=FALSE)

tripModeProfile <- data.frame(itripmode1$counts+jtripmode1$counts, itripmode2$counts+jtripmode2$counts, itripmode3$counts+jtripmode3$counts, itripmode4$counts+jtripmode4$counts,
                              itripmode5$counts+jtripmode5$counts, itripmode6$counts+jtripmode6$counts, itripmode7$counts+jtripmode7$counts, itripmode8$counts+jtripmode8$counts, 
                              itripmode9$counts+jtripmode9$counts, itripmode10$counts+jtripmode10$counts, itripmode11$counts+jtripmode11$counts, itripmode12$counts+jtripmode12$counts)
colnames(tripModeProfile) <- c("tourmode1", "tourmode2", "tourmode3", "tourmode4", "tourmode5", "tourmode6", "tourmode7", "tourmode8", "tourmode9", "tourmode10", "tourmode11", "tourmode12")
write.csv(tripModeProfile, "tripModeProfile_Total.csv")

tripModeProfile9_vis <- tripModeProfile[1:13,]
tripModeProfile9_vis$id <- row.names(tripModeProfile9_vis)
tripModeProfile9_vis <- melt(tripModeProfile9_vis, id = c("id"))
colnames(tripModeProfile9_vis) <- c("id", "purpose", "freq9")

tripModeProfile9_vis <- xtabs(freq9~id+purpose, tripModeProfile9_vis)
tripModeProfile9_vis[is.na(tripModeProfile9_vis)] <- 0
tripModeProfile9_vis <- addmargins(as.table(tripModeProfile9_vis))
tripModeProfile9_vis <- as.data.frame.matrix(tripModeProfile9_vis)

tripModeProfile9_vis$id <- row.names(tripModeProfile9_vis)
tripModeProfile9_vis <- melt(tripModeProfile9_vis, id = c("id"))
colnames(tripModeProfile9_vis) <- c("id", "purpose", "freq9")
tripModeProfile9_vis$id <- as.character(tripModeProfile9_vis$id)
tripModeProfile9_vis$purpose <- as.character(tripModeProfile9_vis$purpose)
tripModeProfile9_vis <- tripModeProfile9_vis[tripModeProfile9_vis$id!="Sum",]
tripModeProfile9_vis$purpose[tripModeProfile9_vis$purpose=="Sum"] <- "Total"


# combine all tripmode profile for visualizer
tripModeProfile_vis <- data.frame(tripModeProfile1_vis, tripModeProfile2_vis$freq2, tripModeProfile3_vis$freq3
                                  , tripModeProfile4_vis$freq4, tripModeProfile5_vis$freq5, tripModeProfile6_vis$freq6
                                  , tripModeProfile7_vis$freq7, tripModeProfile8_vis$freq8, tripModeProfile9_vis$freq9)
colnames(tripModeProfile_vis) <- c("tripmode", "tourmode", "work", "univ", "schl", "imain", "idisc", "jmain", "jdisc", "atwork", "total")

temp <- melt(tripModeProfile_vis, id = c("tripmode", "tourmode"))
#tripModeProfile_vis <- cast(temp, tripmode+variable~tourmode)
#write.csv(tripModeProfile_vis, "tripModeProfile_vis.csv", row.names = F)
temp$grp_var <- paste(temp$variable, temp$tourmode, sep = "")

# rename tour mode to standard names
temp$tourmode[temp$tourmode=="tourmode1"] <- 'Auto SOV'
temp$tourmode[temp$tourmode=="tourmode2"] <- 'Auto 2 Person'
temp$tourmode[temp$tourmode=="tourmode3"] <- 'Auto 3+ Person'
temp$tourmode[temp$tourmode=="tourmode4"] <- 'Walk'
temp$tourmode[temp$tourmode=="tourmode5"] <- 'Bike/Moped'
temp$tourmode[temp$tourmode=="tourmode6"] <- 'Walk-Transit'
temp$tourmode[temp$tourmode=="tourmode7"] <- 'PNR-Transit'
temp$tourmode[temp$tourmode=="tourmode8"] <- 'KNR-Transit_PERS'
temp$tourmode[temp$tourmode=="tourmode9"] <- 'KNR-Transit_TNC'
temp$tourmode[temp$tourmode=="tourmode10"] <- 'Taxi'
temp$tourmode[temp$tourmode=="tourmode11"] <- 'TNC'
temp$tourmode[temp$tourmode=="tourmode12"] <- 'School Bus'

colnames(temp) <- c("tripmode","tourmode","purpose","value","grp_var")

write.csv(temp, "tripModeProfile_vis.csv", row.names = F)


###
#trip mode by time period
#calculate time of day
trips$tod <- 5 # EA: 3 am - 6 am
trips$tod <- ifelse(trips$stop_period>=4 & trips$stop_period<=9, 1, trips$tod)   # AM: 6 am - 9 am
trips$tod <- ifelse(trips$stop_period>=10 & trips$stop_period<=22, 2, trips$tod) # MD: 9 am - 3:30 pm
trips$tod <- ifelse(trips$stop_period>=23 & trips$stop_period<=29, 3, trips$tod) # PM: 3:30 pm - 7 pm
trips$tod <- ifelse(trips$stop_period>=30 & trips$stop_period<=40, 4, trips$tod) # EV: 7 pm - 3 am
trips$num_trips <- 1

jtrips$tod <- 5 # EA: 3 am - 6 am
jtrips$tod <- ifelse(jtrips$stop_period>=4 & jtrips$stop_period<=9, 1, jtrips$tod)   # AM: 6 am - 9 am
jtrips$tod <- ifelse(jtrips$stop_period>=10 & jtrips$stop_period<=22, 2, jtrips$tod) # MD: 9 am - 3:30 pm
jtrips$tod <- ifelse(jtrips$stop_period>=23 & jtrips$stop_period<=29, 3, jtrips$tod) # PM: 3:30 pm - 7 pm
jtrips$tod <- ifelse(jtrips$stop_period>=30 & jtrips$stop_period<=40, 4, jtrips$tod) # EV: 7 pm - 3 am
jtrips$num_trips <- 1
#jtrips$num_trips <- jtrips$num_participants

itrips_summary <- aggregate(num_trips~tod+TOURPURP+TOURMODE+TRIPMODE, data=trips, FUN=sum)
jtrips_summary <- aggregate(num_trips~tod+TOURPURP+TOURMODE+TRIPMODE, data=jtrips, FUN=sum)

write.csv(itrips_summary, "itrips_tripmode_summary.csv", row.names = F)
write.csv(jtrips_summary, "jtrips_tripmode_summary.csv", row.names = F)

###




# Total number of stops, trips & tours
cat("Total number of stops : ", nrow(stops) + nrow(jstops))
cat("Total number of trips : ", nrow(trips) + nrow(jtrips))
cat("Total number of tours : ", nrow(tours) + sum(unique_joint_tours$NUMBER_HH))


# output total numbers in a file
total_population <- sum(pertypeDistbn$freq)
total_households <- nrow(hh)
total_tours <- nrow(tours) + sum(unique_joint_tours$NUMBER_HH)
total_trips <- nrow(trips) + nrow(jtrips)
total_stops <- nrow(stops) + nrow(jstops)

trips$num_travel[trips$TRIPMODE==1] <- 1    #sov
trips$num_travel[trips$TRIPMODE==2] <- 2    #hov2
trips$num_travel[trips$TRIPMODE==3] <- 3.5  #hov3
trips$num_travel[trips$TRIPMODE==10] <- 1.1 #taxi
trips$num_travel[trips$TRIPMODE==11] <- 1.2 #tnc single
trips$num_travel[trips$TRIPMODE==12] <- 2.0 #tnc shared
trips$num_travel[is.na(trips$num_travel)] <- 0

total_vmt <- sum((trips$od_dist[trips$TRIPMODE<=3])/trips$num_travel[trips$TRIPMODE<=3]) + sum((trips$od_dist[trips$TRIPMODE>=10 & trips$TRIPMODE<=12])/trips$num_travel[trips$TRIPMODE>=10 & trips$TRIPMODE<=12])

totals_var <- c("total_population", "total_households", "total_tours", "total_trips", "total_stops", "total_vmt")
totals_val <- c(total_population,total_households, total_tours, total_trips, total_stops, total_vmt)

totals_df <- data.frame(name = totals_var, value = totals_val)

write.csv(totals_df, "totals.csv", row.names = F)

# HH Size distribution
hhSizeDist <- count(hh, c("HHSIZE"))
write.csv(hhSizeDist, "hhSizeDist.csv", row.names = F)

# Persons by person type
actpertypeDistbn <- count(per[per$activity_pattern!="H"], c("PERTYPE"))
write.csv(actpertypeDistbn, "activePertypeDistbn.csv", row.names = TRUE)


### Generate school escorting summaries

# detach plyr and load dplyr
#detach("package:plyr", unload=TRUE)
if (!"dplyr" %in% installed.packages()) install.packages("dplyr", repos='http://cran.us.r-project.org')
library(dplyr)

# get driver person type
tours$out_chauffuer_ptype <- per$PERTYPE[match(tours$hh_id*100+tours$driver_num_out, 
                                               per$hh_id*100+per$person_num)]
tours$inb_chauffuer_ptype <- per$PERTYPE[match(tours$hh_id*100+tours$driver_num_in, 
                                               per$hh_id*100+per$person_num)]

#tours$out_chauffuer_dap <- per$activity_pattern[match(tours$hh_id*100+tours$driver_num_out, per$hh_id*100+per$person_num)]
#tours$inb_chauffuer_dap <- per$activity_pattern[match(tours$hh_id*100+tours$driver_num_in, per$hh_id*100+per$person_num)]


tours[is.na(tours)] <- 0

tours_sample <- select(tours, hh_id, person_id, person_num, tour_id, tour_purpose, escort_type_out, escort_type_in, 
                       driver_num_out, driver_num_in, person_type)

tours_sample <- tours[tours$tour_purpose=="School" & tours$person_type>=6, ]

# Code no escort as "3" to be same as OHAS data
tours_sample$escort_type_out[tours_sample$escort_type_out==0] <- 3
tours_sample$escort_type_in[tours_sample$escort_type_in==0] <- 3


# School tours by Escort Type X Child Type
out_table1 <- table(tours_sample$escort_type_out, tours_sample$person_type)
inb_table1 <- table(tours_sample$escort_type_in, tours_sample$person_type)

# School tours by Escort Type X Chauffuer Type
out_sample2 <- filter(tours_sample, out_chauffuer_ptype>0)
inb_sample2 <- filter(tours_sample, inb_chauffuer_ptype>0)
out_table2 <- table(out_sample2$escort_type_out, out_sample2$out_chauffuer_ptype)
inb_table2 <- table(inb_sample2$escort_type_in, inb_sample2$inb_chauffuer_ptype)

## Workers summary
# summary of worker with a child which went to school
# by escort type, can be separated by outbound and inbound direction

#get list of active workers with at least one work tour
active_workers <- tours %>%
  filter(tour_purpose %in% c("Work","Work-Based")) %>%   #work and work-related
  filter(person_type %in% c(1,2)) %>%  #full and part-time worker
  group_by(hh_id, person_num) %>%
  summarise(person_type=max(person_type)) %>%
  ungroup()

workers <- per[per$PERTYPE %in% c(1,2), ]

#get list of students with at least one school tour
active_students <- tours %>%
  filter(tour_purpose %in% c("School")) %>%   #school tour
  filter(person_type %in% c(6,7,8)) %>%  #all school students
  group_by(hh_id, person_num) %>%
  summarise(person_type=max(person_type)) %>%
  ungroup()

students <- per[per$PERTYPE %in% c(6,7,8), ] 

hh_active_student <- active_students %>%
  group_by(hh_id) %>%
  mutate(active_student=1) %>%
  summarise(active_student = max(active_student)) %>%
  ungroup()

#tag active workers with active students in household
active_workers <- active_workers %>%
  left_join(hh_active_student, by = c("hh_id")) %>%
  mutate(active_student=ifelse(is.na(active_student), 0, active_student))


#list of workers who did ride share or pure escort for school student
out_rs_workers <- tours %>%
  select(hh_id, person_num, tour_id, tour_purpose,  
         escort_type_out, driver_num_out, out_chauffuer_ptype) %>%
  filter(tour_purpose=="School" & escort_type_out==1) %>%
  group_by(hh_id, driver_num_out) %>%
  mutate(num_escort = 1) %>%
  summarise(out_rs_escort = sum(num_escort))

out_pe_workers <- tours %>%
  select(hh_id, person_num, tour_id, tour_purpose, 
         escort_type_out, driver_num_out, out_chauffuer_ptype) %>%
  filter(tour_purpose=="School" & escort_type_out==2) %>%
  group_by(hh_id, driver_num_out) %>%
  mutate(num_escort = 1) %>%
  summarise(out_pe_escort = sum(num_escort))

inb_rs_workers <- tours %>%
  select(hh_id, person_num, tour_id, tour_purpose, 
         escort_type_in, driver_num_in, inb_chauffuer_ptype) %>%
  filter(tour_purpose=="School" & escort_type_in==1) %>%
  group_by(hh_id, driver_num_in) %>%
  mutate(num_escort = 1) %>%
  summarise(inb_rs_escort = sum(num_escort))

inb_pe_workers <- tours %>%
  select(hh_id, person_num, tour_id, tour_purpose, 
         escort_type_in, driver_num_in, inb_chauffuer_ptype) %>%
  filter(tour_purpose=="School" & escort_type_in==2) %>%
  group_by(hh_id, driver_num_in) %>%
  mutate(num_escort = 1) %>%
  summarise(inb_pe_escort = sum(num_escort))

active_workers <- active_workers %>%
  left_join(out_rs_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_out")) %>%
  left_join(out_pe_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_out")) %>%
  left_join(inb_rs_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_in")) %>%
  left_join(inb_pe_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_in"))

active_workers[is.na(active_workers)] <- 0

#workers <- workers %>%
#  left_join(out_rs_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_out")) %>%
#  left_join(out_pe_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_out")) %>%
#  left_join(inb_rs_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_in")) %>%
#  left_join(inb_pe_workers, by = c("hh_id"="hh_id", "person_num"="driver_num_in"))
#
#workers[is.na(workers)] <- 0

active_workers <- active_workers %>%
  mutate(out_escort_type = 3) %>%
  mutate(out_escort_type = ifelse(out_rs_escort>0, 1, out_escort_type)) %>%
  mutate(out_escort_type = ifelse(out_pe_escort>0, 2, out_escort_type)) %>%
  mutate(inb_escort_type = 3) %>%
  mutate(inb_escort_type = ifelse(inb_rs_escort>0, 1, inb_escort_type)) %>%
  mutate(inb_escort_type = ifelse(inb_pe_escort>0, 2, inb_escort_type))

temp <- filter(active_workers, active_student==1)
worker_table <- table(temp$out_escort_type, temp$inb_escort_type)

## add marginal totals to all final tables
out_table1   <- addmargins(as.table(out_table1))
inb_table1   <- addmargins(as.table(inb_table1))
out_table2   <- addmargins(as.table(out_table2))
inb_table2   <- addmargins(as.table(inb_table2))
worker_table <- addmargins(as.table(worker_table))

## reshape data in required form for visualizer
out_table1 <- as.data.frame.matrix(out_table1)
out_table1$id <- row.names(out_table1)
out_table1 <- melt(out_table1, id = c("id"))
colnames(out_table1) <- c("esc_type", "child_type", "freq_out")
out_table1$esc_type <- as.character(out_table1$esc_type)
out_table1$child_type <- as.character(out_table1$child_type)
out_table1 <- out_table1[out_table1$esc_type!="Sum",]
out_table1$child_type[out_table1$child_type=="Sum"] <- "Total"

inb_table1 <- as.data.frame.matrix(inb_table1)
inb_table1$id <- row.names(inb_table1)
inb_table1 <- melt(inb_table1, id = c("id"))
colnames(inb_table1) <- c("esc_type", "child_type", "freq_inb")
inb_table1$esc_type <- as.character(inb_table1$esc_type)
inb_table1$child_type <- as.character(inb_table1$child_type)
inb_table1 <- inb_table1[inb_table1$esc_type!="Sum",]
inb_table1$child_type[inb_table1$child_type=="Sum"] <- "Total"

table1 <- out_table1
table1$freq_inb <- inb_table1$freq_inb
table1$esc_type[table1$esc_type=='1'] <- "Ride Share"
table1$esc_type[table1$esc_type=='2'] <- "Pure Escort"
table1$esc_type[table1$esc_type=='3'] <- "No Escort"
table1$child_type[table1$child_type=='6'] <- 'Driv Student'
table1$child_type[table1$child_type=='7'] <- 'Non-DrivStudent'
table1$child_type[table1$child_type=='8'] <- 'Pre-Schooler'


out_table2 <- as.data.frame.matrix(out_table2)
out_table2$id <- row.names(out_table2)
out_table2 <- melt(out_table2, id = c("id"))
colnames(out_table2) <- c("esc_type", "chauffeur", "freq_out")
out_table2$esc_type <- as.character(out_table2$esc_type)
out_table2$chauffeur <- as.character(out_table2$chauffeur)
out_table2 <- out_table2[out_table2$esc_type!="Sum",]
out_table2$chauffeur[out_table2$chauffeur=="Sum"] <- "Total"

inb_table2 <- as.data.frame.matrix(inb_table2)
inb_table2$id <- row.names(inb_table2)
inb_table2 <- melt(inb_table2, id = c("id"))
colnames(inb_table2) <- c("esc_type", "chauffeur", "freq_inb")
inb_table2$esc_type <- as.character(inb_table2$esc_type)
inb_table2$chauffeur <- as.character(inb_table2$chauffeur)
inb_table2 <- inb_table2[inb_table2$esc_type!="Sum",]
inb_table2$chauffeur[inb_table2$chauffeur=="Sum"] <- "Total"

table2 <- out_table2
table2$freq_inb <- inb_table2$freq_inb
table2$esc_type[table2$esc_type=="1"] <- "Ride Share"
table2$esc_type[table2$esc_type=="2"] <- "Pure Escort"
table2$esc_type[table2$esc_type=="3"] <- "No Escort"
table2$chauffeur[table2$chauffeur=='1'] <- "FT Worker"
table2$chauffeur[table2$chauffeur=='2'] <- "PT Worker"
table2$chauffeur[table2$chauffeur=='3'] <- "Univ Stud"
table2$chauffeur[table2$chauffeur=='4'] <- "Non-Worker"
table2$chauffeur[table2$chauffeur=='5'] <- "Retiree"
table2$chauffeur[table2$chauffeur=='6'] <- "Driv Student"

worker_table <- as.data.frame.matrix(worker_table)
colnames(worker_table) <- c("Ride Share", "Pure Escort", "No Escort", "Total")
worker_table$DropOff <- row.names(worker_table)
worker_table$DropOff[worker_table$DropOff=="1"] <- "Ride Share"
worker_table$DropOff[worker_table$DropOff=="2"] <- "Pure Escort"
worker_table$DropOff[worker_table$DropOff=="3"] <- "No Escort"
worker_table$DropOff[worker_table$DropOff=="Sum"] <- "Total"

worker_table <- worker_table[, c("DropOff", "Ride Share","Pure Escort","No Escort","Total")]

## write outputs
write.csv(table1, "esctype_by_childtype.csv", row.names = F)
write.csv(table2, "esctype_by_chauffeurtype.csv", row.names = F)
write.csv(worker_table, "worker_school_escorting.csv", row.names = F)

detach("package:dplyr", unload=TRUE)


#District level summary of transit tours and trips
#segment by Walk, PNR, and KNR
# tour mode/trip mode
# 9-Walk to Transit
# 10-PNR
# 11-KNR

#tours
tours$ODISTRICT <- mazCorrespondence$pmsa[match(tours$orig_mgra, mazCorrespondence$mgra)]
tours$DDISTRICT <- mazCorrespondence$pmsa[match(tours$dest_mgra, mazCorrespondence$mgra)]
tours_transit <- tours[tours$tour_mode>=9 & tours$tour_mode<=12,]
tours_transit <- tours_transit[,c("ODISTRICT","DDISTRICT","tour_mode")]
tours_transit$NUMBER_HH <- 1

unique_joint_tours$ODISTRICT <- mazCorrespondence$pmsa[match(unique_joint_tours$orig_mgra, mazCorrespondence$mgra)]
unique_joint_tours$DDISTRICT <- mazCorrespondence$pmsa[match(unique_joint_tours$dest_mgra, mazCorrespondence$mgra)]
unique_joint_tours_transit <- unique_joint_tours[unique_joint_tours$tour_mode>=9 & unique_joint_tours$tour_mode<=12,]
unique_joint_tours_transit <- unique_joint_tours_transit[,c("ODISTRICT","DDISTRICT","tour_mode", "NUMBER_HH")]

tours_transit_all <- rbind(tours_transit, unique_joint_tours_transit)

district_flow_tours <- xtabs(NUMBER_HH~tour_mode+ODISTRICT+DDISTRICT, data=tours_transit_all)
write.csv(district_flow_tours, "district_flow_transit_tours.csv")

#trips
trips$ODISTRICT <- mazCorrespondence$pmsa[match(trips$orig_mgra, mazCorrespondence$mgra)]
trips$DDISTRICT <- mazCorrespondence$pmsa[match(trips$dest_mgra, mazCorrespondence$mgra)]
trips_transit <- trips[trips$trip_mode>=9 & trips$trip_mode<=12,]
trips_transit <- trips_transit[,c("ODISTRICT","DDISTRICT","trip_mode")]
trips_transit$num_participants <- 1

jtrips$ODISTRICT <- mazCorrespondence$pmsa[match(jtrips$orig_mgra, mazCorrespondence$mgra)]
jtrips$DDISTRICT <- mazCorrespondence$pmsa[match(jtrips$dest_mgra, mazCorrespondence$mgra)]
jtrips_transit <- jtrips[jtrips$trip_mode>=9 & jtrips$trip_mode<=12,]
jtrips_transit <- jtrips_transit[,c("ODISTRICT","DDISTRICT","trip_mode","num_participants")]

trips_transit_all <- rbind(trips_transit, jtrips_transit)

district_flow_trips <- xtabs(num_participants~trip_mode+ODISTRICT+DDISTRICT, data=trips_transit_all)
write.csv(district_flow_trips, "district_flow_transit_trips.csv")
                               
# finish

end_time <- Sys.time()
end_time - start_time
