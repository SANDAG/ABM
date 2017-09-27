###
# OBJECTIVE:
# Regression analysis for SANDAG INRIX travel time data
#
# INPUTS:
# travel time SD/ mean travel time in 15 mins (*.dbf) - "inrix2012_nooutliers_traveltime.dbf" (output of inrix outlier analysis)
# inrix to tcoved correspondence (created by Fehr and Peers) - "inrix_2012hwy.txt"
# ABM assignment results by 5 time periods - "hwyload_EA.csv", "hwyload_AM.csv", "hwyload_MD.csv", "hwyload_PM.csv", "hwyload_EV.csv"
# Major interchange distance from SANDAG ABM output folder - "MajorInterchangeDistance.csv"
# other inputs (not used in final analysis) - "InterchangeDistance.csv", "LaneChangeDistance.csv", "FreewayRampMeters.csv"

# DATA:
# IFC in the roadway network database is re-categorized into four facility classes
# Facility Class	IFC			Description
# Freeway			1			Freeways
# Arterial			2,3			Major arterials, prime arterials
# Ramp				8,9			Local ramps, freeway ramps
# Other				4,5,6,7		Collectors and local streets
# 80% for estimation (regression analysis) and 20% kept for validation
# low sample size for ramps and others - only freeway and arterial estimations are used

# REGRESSION VARIABLES (final):
#
# Dependent Variable: 
# 	Travel time per mile Std. Dev. per mean travel time in 15 mins time slices
#
# Independent Variables:
# 	Number of lanes categories (one, two, three, four, five and more)
# 	Level of service (LOS)
# 		LOSC+ = (V/C-0.69)* if (V/C>=0.70)
# 		LOSD+ = (V/C-0.79)*  if (V/C>=0.80)
# 		LOSE+ = (V/C-0.89)* if (V/C>=0.90)
# 		LOSF_LOW = (V/C-0.99)* if (V/C>=1.00)
# 		LOSF_MED = (V/C-1.09)* if (V/C>=1.10)
# 		LOSF_HIGH = (V/C-1.19)* if (V/C>=1.20)
# 	Speed
# 		ISPD70 (1 if posted speed is 70mph, 0 otherwise) for Freeways
# 		Posted speed categories (ISPD<35, ISPD=35, ISPD=40, ISPD=45, ISPD=50, ISPD>50) for Arterials
# 	Shift variables (BeforeAM.Shift, AfterAM.Shift, BeforePM.Shift, AfterPM.Shift)
# 	Control type (none, signal, stop, railroad, ramp meter)
# 	Upstream and downstream major interchange distance - from midpoint of a freeway segment. inverse distances are used in estimation

# IMPLEMENTATION (in SANDAG ABM):
#
# By facility type and by 5 time periods
# Estimations for freeways and arterials
# Ramp and other facility types are applied with arterial estimation
# Two reliability components
# 	LOS
# 	Static (speed, distance to/from interchanges, intersection type etc.)
# Sum of the two reliability components is multiplied by mean travel time and link length 
# Variable calculations are automated including distance to/from interchanges
# Reliability fields are added to highway network
# 	LOS: include only coefficients
# 	Static: sum of remaining (un)reliability including the intercept
# Skimming:
# 	Standard deviation is not additive but variance is
# 	A link (un)reliability = (MSA Cost – MSA Time)
# 	Skimmed variance (square of link (un)reliability)
# 	Final skims are square root of the skimmed value

# OTHER SCRIPTS USED:
# utilfunc.R

# by: nagendra.dhakar@rsginc.com
# for: SANDAG SHRP C04 - Tolling and Reliability 
#----------------------------------------------------

library(foreign)
library(stringr)
library(xtable)
library(reshape)
library(XLConnect)
library(descr)
library(Hmisc)
library(data.table)
library(plyr)
library(gtools)
library(vioplot)
library(lattice)
library(grid)
library(timeDate)
library(ggplot2)
library(robustbase)
library(readr)

# -------------------- 0. SOURCE FILES AND CONFIG SETTINGS -----------------------

# workspace and source files
setwd("E:/Projects/Clients/SANDAG")
source("./Data_2015/utilfunc.R")

# method of outlier removal - outlier analysis (this is used) or confidence interval 10
OUTLIER=TRUE

# input files 
inrixhwycorresp.file                  = "./Data_2015/TMC/inrix_2012hwy.txt"
AssignResultsEA.file                  = "./SandagReliability/ModelData/OldApp_OldPopSyn/AssignmentResults/hwyload_EA.csv"
AssignResultsAM.file                  = "./SandagReliability/ModelData/OldApp_OldPopSyn/AssignmentResults/hwyload_AM.csv"
AssignResultsMD.file                  = "./SandagReliability/ModelData/OldApp_OldPopSyn/AssignmentResults/hwyload_MD.csv"
AssignResultsPM.file                  = "./SandagReliability/ModelData/OldApp_OldPopSyn/AssignmentResults/hwyload_PM.csv"
AssignResultsEV.file                  = "./SandagReliability/ModelData/OldApp_OldPopSyn/AssignmentResults/hwyload_EV.csv"
InterchangeDistance.file              = "./Data_2015/InterchangeDistance.csv"
InterchangeDistanceHOV.file           = "./Data_2015/InterchangeDistance_HOV.csv"
MajorInterchangeDistance.file         = "./Data_2015/MajorInterchangeDistance.csv"
LaneChangeDistance.file               = "./Data_2015/LaneChangeDistance.csv"
FreewayRampMeters.file               = "./Data_2015/FreewayRampMeters.csv"

# travel time SD input file
if (OUTLIER) {
  StdDev.file                         = "./Data_2015/inrix2012_nooutliers_traveltime.dbf"
} else {
  StdDev.file                         = "./Data_2015/inrix2012_noCS10_traveltime.dbf"
}

# outputs directory
outputsDir="./Data_2015/Results"

# -------------------- 1. LOAD DATA ----------------------
# load SD file, assignment results, interchange distance etc.
StdDev<- read.dbf(StdDev.file)
AssignResultsEA <- read.csv(AssignResultsEA.file)
AssignResultsAM <- read.csv(AssignResultsAM.file)
AssignResultsMD <- read.csv(AssignResultsMD.file)
AssignResultsPM <- read.csv(AssignResultsPM.file)
AssignResultsEV <- read.csv(AssignResultsEV.file)
InterchangeDistance <- read.csv(InterchangeDistance.file)
InterchangeDistanceHOV <- read.csv(InterchangeDistanceHOV.file)
MajorInterchangeDistance <- read.csv(MajorInterchangeDistance.file)
LaneChangeDistance <- read.csv(LaneChangeDistance.file)  # LinkID,Length,LaneIncrease,LaneDrop,DeadEnd,RegionEnd,DownstreamDistance,ihov,BaseThruLanes,DownThruLanes,DownLinks,QueryDown
FreewayRampMeters <- read.csv(FreewayRampMeters.file)  # LinkID,Length,IsRampMeter,QueryNode

# -------------------- 2. SET UP DATA --------------

# read and load inrix to hwy correspondence data
readSaveRdata(inrixhwycorresp.file,"inrixhwycorresp")
inrixhwycorresp <- assignLoad(paste0(inrixhwycorresp.file,".Rdata"))

# add a new field tmc_code without first character ('-' or '+')
inrixhwycorresp[,tmc_code:=substr(TMC,2,nchar(TMC))]

# remove the segments that are flagged 
inrixhwycorresp<-subset(inrixhwycorresp,FLAG==0)

# get unique tmc_code with variables corresponding to maximum of TMCProp
df.orig <-inrixhwycorresp
df.agg<-aggregate(TMCProp~tmc_code,inrixhwycorresp,max)
df.max <- merge(df.agg, df.orig)

# Note: there could be cases where one hwcov_id is associated with multiple tmc_code

# facility type - different capacity fields for mid-link capacity (period fields)
tmc_ifc <-df.max[,c("tmc_code","IFC","HWYCOV_ID","NM","LENGTH","ISPD","ABLNO","ABLNA","ABLNP",
                    "BALNO","BALNA","BALNP","ABCPO","ABCPA","ABCPP","BACPO","BACPA","BACPP",
                    "ABCXO","ABCXA","ABCXP","BACXO","BACXA","BACXP","ABCNT","BACNT", 
                    "ABTL","ABRL","ABLL","BATL","BARL","BALL","ABGC","BAGC","IHOV","ABAU","BAAU"),]

tmc_ifc$LENGTH<-as.numeric(gsub(",","",tmc_ifc$LENGTH))
tmc_ifc$HWYCOV_ID<-as.numeric(gsub(",","",tmc_ifc$HWYCOV_ID))

# merge attributes to INRIX data
StdDev<-merge(StdDev,tmc_ifc,by="tmc_code")

# ----------------------------------------------------

# write to file
if (FALSE) {
  write.table(tmc_ifc,"./Data_2015/tmc_hwycov_atrributes.csv",sep=",",row.names=F,quote=F)
}

# ID1 in the results is the same as HWYCOV_ID in the link file
AssignResultsEA <- AssignResultsEA[,c("ID1","AB_Time","BA_Time","AB_Flow","BA_Flow","AB_Speed","BA_Speed")]
AssignResultsAM <- AssignResultsAM[,c("ID1","AB_Time","BA_Time","AB_Flow","BA_Flow","AB_Speed","BA_Speed")]
AssignResultsMD <- AssignResultsMD[,c("ID1","AB_Time","BA_Time","AB_Flow","BA_Flow","AB_Speed","BA_Speed")]
AssignResultsPM <- AssignResultsPM[,c("ID1","AB_Time","BA_Time","AB_Flow","BA_Flow","AB_Speed","BA_Speed")]
AssignResultsEV <- AssignResultsEV[,c("ID1","AB_Time","BA_Time","AB_Flow","BA_Flow","AB_Speed","BA_Speed")]

# use 80% for estimation and 20% for validation - sample segments by facility type

# create new facility type - freeways, arterials, ramps, and others
StdDev$IFC_Est <- ifelse(StdDev$IFC==1,1,0) # freeways
StdDev$IFC_Est <- ifelse(StdDev$IFC==2 | StdDev$IFC==3,2,StdDev$IFC_Est) # arterials - major and prime
StdDev$IFC_Est <- ifelse(StdDev$IFC==8 | StdDev$IFC==9,3,StdDev$IFC_Est) # ramps - local ramps and freeways ramps
StdDev$IFC_Est <- ifelse(StdDev$IFC>=4 & StdDev$IFC<=7,4,StdDev$IFC_Est) # others - collectors and local streets

Sample.Rate<-0.8

# Freeways
StdDev.Freeways <- subset(StdDev, IFC_Est==1)
StdDev.Freeways.Seg <- unique(StdDev.Freeways$tmc_code)
StdDev.Freeways.Seg<-as.data.frame(StdDev.Freeways.Seg)

sample_size <- floor(Sample.Rate *nrow(StdDev.Freeways.Seg))
set.seed(123)
Est.Ind <- sample(seq_len(nrow(StdDev.Freeways.Seg)), size = sample_size)
StdDev.Freeways.Seg.Est <- StdDev.Freeways.Seg[Est.Ind,]
StdDev.Freeways.Seg.Val <- StdDev.Freeways.Seg[-Est.Ind,]

# Arterials
StdDev.Arterials <- subset(StdDev, IFC_Est==2)
StdDev.Arterials.Seg <- unique(StdDev.Arterials$tmc_code)
StdDev.Arterials.Seg<-as.data.frame(StdDev.Arterials.Seg)

sample_size <- floor(Sample.Rate *nrow(StdDev.Arterials.Seg))
set.seed(123)
Est.Ind <- sample(seq_len(nrow(StdDev.Arterials.Seg)), size = sample_size)
StdDev.Arterials.Seg.Est <- StdDev.Arterials.Seg[Est.Ind,]
StdDev.Arterials.Seg.Val <- StdDev.Arterials.Seg[-Est.Ind,]

# Ramps
StdDev.Ramps <- subset(StdDev, IFC_Est==3)
StdDev.Ramps.Seg <- unique(StdDev.Ramps$tmc_code)
StdDev.Ramps.Seg<-as.data.frame(StdDev.Ramps.Seg)

sample_size <- floor(Sample.Rate *nrow(StdDev.Ramps.Seg))
set.seed(123)
Est.Ind <- sample(seq_len(nrow(StdDev.Ramps.Seg)), size = sample_size)
StdDev.Ramps.Seg.Est <- StdDev.Ramps.Seg[Est.Ind,]
StdDev.Ramps.Seg.Val <- StdDev.Ramps.Seg[-Est.Ind,]

# Others
StdDev.Others <- subset(StdDev, IFC_Est==4)
StdDev.Others.Seg <- unique(StdDev.Others$tmc_code)
StdDev.Others.Seg<-as.data.frame(StdDev.Others.Seg)

sample_size <- floor(Sample.Rate *nrow(StdDev.Others.Seg))
set.seed(123)
Est.Ind <- sample(seq_len(nrow(StdDev.Others.Seg)), size = sample_size)
StdDev.Others.Seg.Est <- StdDev.Others.Seg[Est.Ind,]
StdDev.Others.Seg.Val <- StdDev.Others.Seg[-Est.Ind,]

#  ------------------ Estimation Dataset --------------------------------------

StdDev.Freeways.Seg.Est<-as.data.frame(StdDev.Freeways.Seg.Est)
StdDev.Arterials.Seg.Est<-as.data.frame(StdDev.Arterials.Seg.Est)
StdDev.Ramps.Seg.Est<-as.data.frame(StdDev.Ramps.Seg.Est)
StdDev.Others.Seg.Est<-as.data.frame(StdDev.Others.Seg.Est)

setnames(StdDev.Freeways.Seg.Est,"StdDev.Freeways.Seg.Est","tmc_code")
setnames(StdDev.Arterials.Seg.Est,"StdDev.Arterials.Seg.Est","tmc_code")
setnames(StdDev.Ramps.Seg.Est,"StdDev.Ramps.Seg.Est","tmc_code")
setnames(StdDev.Others.Seg.Est,"StdDev.Others.Seg.Est","tmc_code")

# combine dataframes into one
StdDev.Seg.Est <- do.call(rbind,list(StdDev.Freeways.Seg.Est,StdDev.Arterials.Seg.Est,StdDev.Ramps.Seg.Est,StdDev.Others.Seg.Est))

# merge data
StdDev.Est<-merge(StdDev,StdDev.Seg.Est,by="tmc_code")

# ---------------------------- Validation Dataset ------------------------------

StdDev.Freeways.Seg.Val<-as.data.frame(StdDev.Freeways.Seg.Val)
StdDev.Arterials.Seg.Val<-as.data.frame(StdDev.Arterials.Seg.Val)
StdDev.Ramps.Seg.Val<-as.data.frame(StdDev.Ramps.Seg.Val)
StdDev.Others.Seg.Val<-as.data.frame(StdDev.Others.Seg.Val)

setnames(StdDev.Freeways.Seg.Val,"StdDev.Freeways.Seg.Val","tmc_code")
setnames(StdDev.Arterials.Seg.Val,"StdDev.Arterials.Seg.Val","tmc_code")
setnames(StdDev.Ramps.Seg.Val,"StdDev.Ramps.Seg.Val","tmc_code")
setnames(StdDev.Others.Seg.Val,"StdDev.Others.Seg.Val","tmc_code")

# combine dataframes into one
StdDev.Seg.Val <- do.call(rbind,list(StdDev.Freeways.Seg.Val,StdDev.Arterials.Seg.Val,StdDev.Ramps.Seg.Val,StdDev.Others.Seg.Val))

# merge data
StdDev.Val<-merge(StdDev,StdDev.Seg.Val,by="tmc_code")

# ------------------- Add Interchange Distance -----------------------
if (FALSE) {
	# Interchange distance
	InterchangeDistance <-InterchangeDistance[,c("LinkID","upstream.distance","downstream.distance")]
	InterchangeDistanceHOV <-InterchangeDistanceHOV[,c("LinkID","Length","upstream.distance","downstream.distance")]

	temp<-merge(InterchangeDistance,InterchangeDistanceHOV,by="LinkID",all.x = TRUE)
	temp$upstream.distance <- ifelse(!is.na(temp$Length),temp$upstream.distance.y,temp$upstream.distance.x)
	temp$downstream.distance<-ifelse(!is.na(temp$Length),temp$downstream.distance.y,temp$downstream.distance.x)

	InterchangeDistance<-temp
	InterchangeDistance <-InterchangeDistance[,c("LinkID","upstream.distance","downstream.distance")]

	temp<-merge(StdDev.Est,InterchangeDistance,by.x ="HWYCOV_ID",by.y = "LinkID",all.x=TRUE)

	StdDev.Est<-temp
}

# Major Interchange distance
MajorInterchangeDistance <-MajorInterchangeDistance[,c("LinkID","upstream.distance","downstream.distance")]

temp<-merge(MajorInterchangeDistance,InterchangeDistanceHOV,by="LinkID",all.x = TRUE)
temp$majorupstream.distance <- ifelse(!is.na(temp$Length),temp$upstream.distance.y,temp$upstream.distance.x)
temp$majordownstream.distance<-ifelse(!is.na(temp$Length),temp$downstream.distance.y,temp$downstream.distance.x)

MajorInterchangeDistance<-temp
MajorInterchangeDistance <-MajorInterchangeDistance[,c("LinkID","majorupstream.distance","majordownstream.distance")]

temp<-merge(StdDev.Est,MajorInterchangeDistance,by.x ="HWYCOV_ID",by.y = "LinkID",all.x=TRUE)

StdDev.Est<-temp

# FF Time (seconds)
StdDev.Est$FF_Time <- as.numeric(StdDev.Est$LENGTH)*(1/5280)*(1/StdDev.Est$ISPD)*60
#test <- subset(StdDev.Est, is.na(StdDev.Est$FF_Time)) # just to see if there are any NA values

# model tod
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>0 & StdDev.Est$todcat<=14,'EV','') #3.5 hours
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>14 & StdDev.Est$todcat<=24,'EA',StdDev.Est$tod.model) #2.5 hours
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>24 & StdDev.Est$todcat<=36,'AM',StdDev.Est$tod.model) #3 hours
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>36 & StdDev.Est$todcat<=62,'MD',StdDev.Est$tod.model) #6.5 hours
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>62 & StdDev.Est$todcat<=76,'PM',StdDev.Est$tod.model) #3.5 hours
StdDev.Est$tod.model<-ifelse(StdDev.Est$todcat>76 & StdDev.Est$todcat<=96,'EV',StdDev.Est$tod.model) #5 hours

# convert to integer - mid link (*CPO, *CPA, *CPP) and intersection (*CXO, *CXA, *CXP) capacities
StdDev.Est$ABCPO<-as.integer(gsub(",","",StdDev.Est$ABCPO))
StdDev.Est$ABCPA<-as.integer(gsub(",","",StdDev.Est$ABCPA))
StdDev.Est$ABCPP<-as.integer(gsub(",","",StdDev.Est$ABCPP))
StdDev.Est$BACPO<-as.integer(gsub(",","",StdDev.Est$BACPO))
StdDev.Est$BACPA<-as.integer(gsub(",","",StdDev.Est$BACPA))
StdDev.Est$BACPP<-as.integer(gsub(",","",StdDev.Est$BACPP))
StdDev.Est$ABCXO<-as.integer(gsub(",","",StdDev.Est$ABCXO))
StdDev.Est$ABCXA<-as.integer(gsub(",","",StdDev.Est$ABCXA))
StdDev.Est$ABCXP<-as.integer(gsub(",","",StdDev.Est$ABCXP))
StdDev.Est$BACXO<-as.integer(gsub(",","",StdDev.Est$BACXO))
StdDev.Est$BACXA<-as.integer(gsub(",","",StdDev.Est$BACXA))
StdDev.Est$BACXP<-as.integer(gsub(",","",StdDev.Est$BACXP))

# set initial values to 0
StdDev.Est$NumLanes <-0
StdDev.Est$ICNT <-0  # intersection control
StdDev.Est$Flow <-0
StdDev.Est$CAP.MidLink <-0 # mid-link capacity for freeways and ramps
StdDev.Est$CAP.IntAppr <-0 # intersection-approach capacity for arterials and others
StdDev.Est$AuxLanes <-0
StdDev.Est$ThruLanes <-0
StdDev.Est$LeftLanes <-0
StdDev.Est$RightLanes <-0
StdDev.Est$GCRatio <-0
Default.Cap<-9999999 # set a very high

# TMC Codes
# External (between interchanges): '+' (NB or WB - positive direction), '-' (SB or EB - negative direction)
# Internal (within interchanges): 'P' (NB or WB), 'N' (SB or EB)

# -------------------------------- ESTIMATION ---------------------------------
# This section estimates regression equations using estimation dataset (StdDev.Est)

# the below is how SANDAG ABM (gisdk) converts capacities from three periods to 5 model periods:
# set capacity fields
# tod_fld    ={{"ABCP_EA"},{"ABCP_AM"},{"ABCP_MD"},{"ABCP_PM"},{"ABCP_EV"},  //BA link capacity
#  {"BACP_EA"},{"BACP_AM"},{"BACP_MD"},{"BACP_PM"},{"BACP_EV"},  //AB link capacity
#  {"ABCX_EA"},{"ABCX_AM"},{"ABCX_MD"},{"ABCX_PM"},{"ABCX_EV"},  //BA intersection capacity
#  {"BACX_EA"},{"BACX_AM"},{"BACX_MD"},{"BACX_PM"},{"BACX_EV"}}  //AB intersection capacity     

# org_fld    ={"ABCPO","ABCPA","ABCPO","ABCPP","ABCPO",                  
#  "BACPO","BACPA","BACPO","BACPP","BACPO", 
#  "ABCXO","ABCXA","ABCXO","ABCXP","ABCXO", 
#  "BACXO","BACXA","BACXO","BACXP","BACXO"}   

# factor     ={"3/12","1","6.5/12","3.5/3","8/12",             
#  "3/12","1","6.5/12","3.5/3","8/12", 
#  "3/12","1","6.5/12","3.5/3","8/12", 
#  "3/12","1","6.5/12","3.5/3","8/12"} 
#
# the capacity calculations below are consistent with the ABM gisdk

# MERGE assignment results to the estimation data

setnames(StdDev.Est,"HWYCOV_ID","ID1")
nrow(StdDev.Est)

# EA Period
temp2 <- merge(StdDev.Est,AssignResultsEA,by="ID1", all.x=TRUE)
temp2$Flow <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BA_Flow,temp2$AB_Flow),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$AB_Flow,temp2$BA_Flow)),temp2$Flow)
temp2$CAP.MidLink <- (ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACPO*3/12,temp2$ABCPO*3/12),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCPO*3/12,temp2$BACPO*3/12)),temp2$CAP.MidLink))
temp2$CAP.IntAppr <- (ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*3/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*3/12,Default.Cap)),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*3/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*3/12,Default.Cap))),temp2$CAP.IntAppr))
temp2$NumLanes <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALNO,temp2$ABLNO),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLNO,temp2$BALNO)),temp2$NumLanes)
temp2$ICNT <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACNT,temp2$ABCNT),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCNT,temp2$BACNT)),temp2$ICNT)
temp2$AuxLanes <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAAU,temp2$ABAU),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABAU,temp2$BAAU)),temp2$AuxLanes)
temp2$ThruLanes <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BATL,temp2$ABTL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABTL,temp2$BATL)),temp2$ThruLanes)
temp2$LeftLanes <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALL,temp2$ABLL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLL,temp2$BALL)),temp2$LeftLanes)
temp2$RightLanes <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BARL,temp2$ABRL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABRL,temp2$BARL)),temp2$RightLanes)
temp2$GCRatio <- ifelse(temp2$tod.model=='EA', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAGC,temp2$ABGC),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABGC,temp2$BAGC)),temp2$GCRatio)

temp2<-temp2[,-c(61:66)]
nrow(temp2)

# AM Period
temp2 <- merge(temp2,AssignResultsAM,by="ID1", all.x=TRUE)
temp2$Flow <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BA_Flow,temp2$AB_Flow),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$AB_Flow,temp2$BA_Flow)),temp2$Flow)
temp2$CAP.MidLink <- (ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACPA,temp2$ABCPA),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCPA,temp2$BACPA)),temp2$CAP.MidLink))    # AM capacity is for 3 hours
temp2$CAP.IntAppr <- (ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXA,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXA,Default.Cap)),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXA,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXA,Default.Cap))),temp2$CAP.IntAppr))
temp2$NumLanes <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALNA,temp2$ABLNA),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLNA,temp2$BALNA)),temp2$NumLanes)
temp2$ICNT <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACNT,temp2$ABCNT),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCNT,temp2$BACNT)),temp2$ICNT)
temp2$AuxLanes <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAAU,temp2$ABAU),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABAU,temp2$BAAU)),temp2$AuxLanes)
temp2$ThruLanes <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BATL,temp2$ABTL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABTL,temp2$BATL)),temp2$ThruLanes)
temp2$LeftLanes <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALL,temp2$ABLL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLL,temp2$BALL)),temp2$LeftLanes)
temp2$RightLanes <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BARL,temp2$ABRL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABRL,temp2$BARL)),temp2$RightLanes)
temp2$GCRatio <- ifelse(temp2$tod.model=='AM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAGC,temp2$ABGC),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABGC,temp2$BAGC)),temp2$GCRatio)

temp2<-temp2[,-c(61:66)]
nrow(temp2)

# MD Period
temp2 <- merge(temp2,AssignResultsMD,by="ID1", all.x=TRUE)
temp2$Flow <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BA_Flow,temp2$AB_Flow),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$AB_Flow,temp2$BA_Flow)),temp2$Flow) # the flow is for 9 am - 3:30 pm
temp2$CAP.MidLink <- (ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACPO*6.5/12,temp2$ABCPO*6.5/12),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCPO*6.5/12,temp2$BACPO*6.5/12)),temp2$CAP.MidLink))
temp2$CAP.IntAppr <- (ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*6.5/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*6.5/12,Default.Cap)),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*6.5/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*6.5/12,Default.Cap))),temp2$CAP.IntAppr))
temp2$NumLanes <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALNO,temp2$ABLNO),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLNO,temp2$BALNO)),temp2$NumLanes)
temp2$ICNT <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACNT,temp2$ABCNT),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCNT,temp2$BACNT)),temp2$ICNT)
temp2$AuxLanes <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAAU,temp2$ABAU),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABAU,temp2$BAAU)),temp2$AuxLanes)
temp2$ThruLanes <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BATL,temp2$ABTL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABTL,temp2$BATL)),temp2$ThruLanes)
temp2$LeftLanes <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALL,temp2$ABLL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLL,temp2$BALL)),temp2$LeftLanes)
temp2$RightLanes <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BARL,temp2$ABRL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABRL,temp2$BARL)),temp2$RightLanes)
temp2$GCRatio <- ifelse(temp2$tod.model=='MD', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAGC,temp2$ABGC),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABGC,temp2$BAGC)),temp2$GCRatio)

temp2<-temp2[,-c(61:66)]
nrow(temp2)

# PM Period
temp2 <- merge(temp2,AssignResultsPM,by="ID1", all.x=TRUE)
temp2$Flow <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BA_Flow,temp2$AB_Flow),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$AB_Flow,temp2$BA_Flow)),temp2$Flow)  # flow is from 3:30 pm to 7 pm
temp2$CAP.MidLink <- (ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACPP*3.5/3,temp2$ABCPP*3.5/3),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCPP*3.5/3,temp2$BACPP*3.5/3)),temp2$CAP.MidLink))   # PM capacity is for 3 hours
temp2$CAP.IntAppr <- (ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXP*3.5/3,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXP*3.5/3,Default.Cap)),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXP*3.5/3,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXP*3.5/3,Default.Cap))),temp2$CAP.IntAppr))
temp2$NumLanes <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALNP,temp2$ABLNP),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLNP,temp2$BALNP)),temp2$NumLanes)
temp2$ICNT <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACNT,temp2$ABCNT),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCNT,temp2$BACNT)),temp2$ICNT)
temp2$AuxLanes <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAAU,temp2$ABAU),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABAU,temp2$BAAU)),temp2$AuxLanes)
temp2$ThruLanes <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BATL,temp2$ABTL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABTL,temp2$BATL)),temp2$ThruLanes)
temp2$LeftLanes <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALL,temp2$ABLL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLL,temp2$BALL)),temp2$LeftLanes)
temp2$RightLanes <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BARL,temp2$ABRL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABRL,temp2$BARL)),temp2$RightLanes)
temp2$GCRatio <- ifelse(temp2$tod.model=='PM', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAGC,temp2$ABGC),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABGC,temp2$BAGC)),temp2$GCRatio)

temp2<-temp2[,-c(61:66)]
nrow(temp2)

# EV Period
temp2 <- merge(temp2,AssignResultsPM,by="ID1", all.x=TRUE)
temp2$Flow <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BA_Flow,temp2$AB_Flow),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$AB_Flow,temp2$BA_Flow)),temp2$Flow) # flow is from 7 pm to 3:30 am
temp2$CAP.MidLink <- (ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code), ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACPO*8/12,temp2$ABCPO*8/12),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCPO*8/12,temp2$BACPO*8/12)),temp2$CAP.MidLink))    # OP capacity is for 18 hours
temp2$CAP.IntAppr <- (ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*8/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*8/12,Default.Cap)),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,ifelse(temp2$BACNT==1,temp2$BACXO*8/12,Default.Cap),ifelse(temp2$ABCNT==1,temp2$ABCXO*8/12,Default.Cap))),temp2$CAP.IntAppr))
temp2$NumLanes <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALNO,temp2$ABLNO),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLNO,temp2$BALNO)),temp2$NumLanes)
temp2$ICNT <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BACNT,temp2$ABCNT),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABCNT,temp2$BACNT)),temp2$ICNT)
temp2$AuxLanes <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAAU,temp2$ABAU),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABAU,temp2$BAAU)),temp2$AuxLanes)
temp2$ThruLanes <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BATL,temp2$ABTL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABTL,temp2$BATL)),temp2$ThruLanes)
temp2$LeftLanes <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BALL,temp2$ABLL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABLL,temp2$BALL)),temp2$LeftLanes)
temp2$RightLanes <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BARL,temp2$ABRL),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABRL,temp2$BARL)),temp2$RightLanes)
temp2$GCRatio <- ifelse(temp2$tod.model=='EV', ifelse(grepl("\\+",temp2$tmc_code) | grepl("P",temp2$tmc_code),ifelse(is.na(temp2$AB_Flow) | temp2$AB_Flow==0,temp2$BAGC,temp2$ABGC),ifelse(is.na(temp2$BA_Flow) | temp2$BA_Flow==0,temp2$ABGC,temp2$BAGC)),temp2$GCRatio)

temp2<-temp2[,-c(61:66)]

nrow(temp2)

# intersection approach capacity, when not available set to 0
#temp2$CAP.IntAppr<-ifelse(temp2$CAP.IntAppr==9999999,0,temp2$CAP.IntAppr)
temp2$CAP.IntAppr<-ifelse(temp2$CAP.IntAppr==9999999,temp2$CAP.MidLink,temp2$CAP.IntAppr)

# values of greater and equal to 7 are for non-availability for some reasons, set them to 0
temp2$ThruLanes <-ifelse(temp2$ThruLanes>=7,0,temp2$ThruLanes)
temp2$LeftLanes <-ifelse(temp2$LeftLanes>=7,0,temp2$LeftLanes)
temp2$RightLanes <-ifelse(temp2$RightLanes>=7,0,temp2$RightLanes)

# recalculate GC ratio as per createhwynet.rsc (line 1045)
temp2$GCRatio<-ifelse(temp2$GCRatio>10,temp2$GCRatio/100,temp2$GCRatio)
temp2$GCRatio<-ifelse(temp2$GCRatio>1,1,temp2$GCRatio)

# ----------------------- Create Variables for Regression Models -----------------------------
EstDataSet<-temp2

# NOTE: Link ID (HWYCOV_ID)=29412 29413 31194 31202,40479 are not in the model network. So remove those for now
EstDataSet<-subset(EstDataSet, !is.na(EstDataSet$Flow))

# Capacity by facility type (12/07/2015)
# freeways and ramps - mid link capacity
# arterials and others - intersection approach
#EstDataSet$CAP<-ifelse(EstDataSet$IFC_Est==1 | EstDataSet$IFC_Est==3, EstDataSet$CAP.MidLink, EstDataSet$CAP.IntAppr)

EstDataSet$CAP<-EstDataSet$CAP.MidLink # mid link capacity for all
EstDataSet$VOC <- EstDataSet$Flow/EstDataSet$CAP

# control type - 0-none, 1-signal, 2-stop, 3-railroad
EstDataSet$ICNT.Est <-ifelse(EstDataSet$ICNT==1,"Signal","None") # signal
EstDataSet$ICNT.Est <-ifelse(EstDataSet$ICNT==2 | EstDataSet$ICNT==3,"Stop",EstDataSet$ICNT.Est) # all-way and two way stop
EstDataSet$ICNT.Est <-ifelse(EstDataSet$ICNT>3,"RailRoad",EstDataSet$ICNT.Est) # other-primarily rail-road crossing
#set the order of factors
EstDataSet$ICNT.Est<-factor(EstDataSet$ICNT.Est, levels = c("None","Signal", "Stop", "RailRoad"))

# for descriptives
EstDataSet$ICNT.Signal <-ifelse(EstDataSet$ICNT==1,1,0) # signal
EstDataSet$ICNT.Stop <-ifelse(EstDataSet$ICNT==2 | EstDataSet$ICNT==3,1,0) # all-way and two way stop
EstDataSet$ICNT.RailRoad <-ifelse(EstDataSet$ICNT>3,1,0) # other-primarily rail-road crossing
EstDataSet$ICNT.RampMeter <-ifelse(EstDataSet$ICNT==4 | EstDataSet$ICNT==5,1,0) # Ramp meter, ramp meter with HOV bypass (12/07/2015)

# Major and Minor Arterial - this would be used only for arterials
EstDataSet$MajorArterial <-ifelse(EstDataSet$IFC==2,1,0)

# I-15 (managed lanes) and SR-125
EstDataSet$I15<-ifelse(EstDataSet$IFC==1 & EstDataSet$IHOV==2 & str_sub(EstDataSet$NM,1,4)=="I-15",1,0) # I15
#EstDataSet$SR125<-ifelse(EstDataSet$IFC==1 & EstDataSet$ihov==4,1,0) # No SR125 facility in the dataset

# LOS variables - additive and multiplicative (12/07/2015)
EstDataSet$LOSC.Up <- ifelse(EstDataSet$VOC>=0.70,EstDataSet$VOC-0.69,0) # LOS C+
EstDataSet$LOSD.Up <- ifelse(EstDataSet$VOC>=0.80,EstDataSet$VOC-0.79,0) # LOS D+
EstDataSet$LOSE.Up <- ifelse(EstDataSet$VOC>=0.90,EstDataSet$VOC-0.89,0) # LOS E+
EstDataSet$LOSF.Low.Up <- ifelse(EstDataSet$VOC>=1.00,EstDataSet$VOC-0.99,0) # LOS F Low+
EstDataSet$LOSF.Med.Up <- ifelse(EstDataSet$VOC>=1.10,EstDataSet$VOC-1.09,0) # LOS F Med+
EstDataSet$LOSF.High.Up <- ifelse(EstDataSet$VOC>=1.20,EstDataSet$VOC-1.19,0) # LOS F High+

# LOS variables - additive and multiplicative - capping VOC
#EstDataSet$LOSC.Up <- ifelse(EstDataSet$VOC>=0.70 & EstDataSet$VOC<=1.00,EstDataSet$VOC-0.69,0) # LOS C+
#EstDataSet$LOSD.Up <- ifelse(EstDataSet$VOC>=0.80 & EstDataSet$VOC<=1.00,EstDataSet$VOC-0.79,0) # LOS D+
#EstDataSet$LOSE.Up <- ifelse(EstDataSet$VOC>=0.90 & EstDataSet$VOC<=1.00,EstDataSet$VOC-0.89,0) # LOS E+
#EstDataSet$LOSF.Low.Up <- ifelse(EstDataSet$VOC>=1.00,EstDataSet$VOC-0.99,0) # LOS F Low+
#EstDataSet$LOSF.Med.Up <- ifelse(EstDataSet$VOC>=1.10,EstDataSet$VOC-1.09,0) # LOS F Med+
#EstDataSet$LOSF.High.Up <- ifelse(EstDataSet$VOC>=1.20,EstDataSet$VOC-1.19,0) # LOS F High+

# LOS categories - for descriptives only
EstDataSet$LOS.Cat <-"LOSB"
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=0.70 & EstDataSet$VOC<0.80,"LOSC",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=0.80 & EstDataSet$VOC<0.90,"LOSD",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=0.90 & EstDataSet$VOC<1.00,"LOSE",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=1.00 & EstDataSet$VOC<1.10,"LOSF.Low",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=1.10 & EstDataSet$VOC<1.20,"LOSF.Med",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat <- ifelse(EstDataSet$VOC>=1.20,"LOSF.High",EstDataSet$LOS.Cat)
EstDataSet$LOS.Cat<-factor(EstDataSet$LOS.Cat, levels = c("LOSB","LOSC","LOSD", "LOSE", "LOSF.Low","LOSF.Med","LOSF.High"))

# NumLanes
EstDataSet$NumLanesCat <- ifelse(EstDataSet$NumLanes==1,"OneLane","NoLane")
EstDataSet$NumLanesCat <- ifelse(EstDataSet$NumLanes==2,"TwoLane",EstDataSet$NumLanesCat)
EstDataSet$NumLanesCat <- ifelse(EstDataSet$NumLanes==3,"ThreeLane",EstDataSet$NumLanesCat)
EstDataSet$NumLanesCat <- ifelse(EstDataSet$NumLanes==4,"FourLanes",EstDataSet$NumLanesCat)
EstDataSet$NumLanesCat <- ifelse(EstDataSet$NumLanes>=5,"FiveLanes+",EstDataSet$NumLanesCat)
#set order of factors
EstDataSet$NumLanesCat<-factor(EstDataSet$NumLanesCat, levels = c("NoLane","OneLane", "TwoLane", "ThreeLane","FourLanes", "FiveLanes+"))

# for descriptives
EstDataSet$OneLane <- ifelse(EstDataSet$NumLanes==1,1,0)
EstDataSet$TwoLane <- ifelse(EstDataSet$NumLanes==2,1,0)
EstDataSet$ThreeLane <- ifelse(EstDataSet$NumLanes==3,1,0)
EstDataSet$FourLane <- ifelse(EstDataSet$NumLanes==4,1,0)
EstDataSet$FiveMoreLane <- ifelse(EstDataSet$NumLanes>=5,1,0)

# calculate reference bins (shift variables) - two peaks (AM and PM) and one low (MD) - generic, for all facility types
#Pre-AM: AM Peak to start of day
#Post-AM: AM Peak to MD low
#Pre-PM: PM Peak to MD low (backward)
#Post-PM: PM Peak to end of day
if (TRUE) {
  EstDataSet.mean.mean<-cast(EstDataSet,todcat~IFC_Est,mean,value="mean")
  EstDataSet.mean.mean<-aggregate(EstDataSet$mean,by=list(EstDataSet$todcat),FUN=mean)
  
  temp<-subset(EstDataSet.mean.mean,EstDataSet.mean.mean$Group.1>=25 & EstDataSet.mean.mean$Group.1<=36)
  Peak_AM <- temp[which.max(temp$x),1] #32
  
  temp<-subset(EstDataSet.mean.mean,EstDataSet.mean.mean$Group.1>=37 & EstDataSet.mean.mean$Group.1<=62)
  Low_MD <- temp[which.min(temp$x),1] #41
  
  temp<-subset(EstDataSet.mean.mean,EstDataSet.mean.mean$Group.1>=63 & EstDataSet.mean.mean$Group.1<=76)
  Peak_PM <- temp[which.max(temp$x),1] #70
  
  # calculate before and after variables for AM and PM periods
  EstDataSet$BeforeAM <-ifelse(EstDataSet$todcat<=Peak_AM,Peak_AM-EstDataSet$todcat,99)
  EstDataSet$BeforePM <-ifelse(EstDataSet$todcat>=Low_MD & EstDataSet$todcat <= Peak_PM,Peak_PM-EstDataSet$todcat,99)
  
  EstDataSet$AfterAM <-ifelse(EstDataSet$todcat>=Peak_AM & EstDataSet$todcat<Low_MD,EstDataSet$todcat-Peak_AM,99)
  EstDataSet$AfterPM <-ifelse(EstDataSet$todcat>=Peak_PM,EstDataSet$todcat-Peak_PM,99)
 
}

EstDataSet$IsBeforeAM<- ifelse(EstDataSet$BeforeAM<99,1,0)
EstDataSet$IsAfterAM<- ifelse(EstDataSet$AfterAM<99,1,0)
EstDataSet$IsBeforePM<- ifelse(EstDataSet$BeforePM<99,1,0)
EstDataSet$IsAfterPM<- ifelse(EstDataSet$AfterPM<99,1,0)

# apply shift
EstDataSet$IsBeforeAM.Shift<- EstDataSet$IsBeforeAM*EstDataSet$BeforeAM
EstDataSet$IsAfterAM.Shift<- EstDataSet$IsAfterAM*EstDataSet$AfterAM
EstDataSet$IsBeforePM.Shift<- EstDataSet$IsBeforePM*EstDataSet$BeforePM
EstDataSet$IsAfterPM.Shift<- EstDataSet$IsAfterPM*EstDataSet$AfterPM

# piecewise functions for shift variables
# Before AM: 32 (Peak_AM) to 29, 29 to 26, 26 to 20, 20 to 1
# After AM: 32 to 36, 36 to 39, 39 to 41 (Low_MD)
# Before PM: 70 (Peak_PM) to 66, 66 to 62, 62 to 58, 58 to 41 (Low_MD)
# After PM: 70 (Peak_PM) to 71, 71 to 79, 79 to 96

EstDataSet$BeforeAM.Step1<-ifelse(EstDataSet$IsBeforeAM==1, EstDataSet$BeforeAM,0)
EstDataSet$BeforeAM.Step2<-ifelse(EstDataSet$IsBeforeAM==1 & EstDataSet$todcat<29,EstDataSet$BeforeAM-(Peak_AM-29),0)
EstDataSet$BeforeAM.Step3<-ifelse(EstDataSet$IsBeforeAM==1 & EstDataSet$todcat<26,EstDataSet$BeforeAM-(Peak_AM-26),0)
EstDataSet$BeforeAM.Step4<-ifelse(EstDataSet$IsBeforeAM==1 & EstDataSet$todcat<20,EstDataSet$BeforeAM-(Peak_AM-20),0)

EstDataSet$AfterAM.Step1<-ifelse(EstDataSet$IsAfterAM==1, EstDataSet$AfterAM,0)
EstDataSet$AfterAM.Step2<-ifelse(EstDataSet$IsAfterAM==1 & EstDataSet$todcat>36,EstDataSet$AfterAM-(36-Peak_AM),0)
EstDataSet$AfterAM.Step3<-ifelse(EstDataSet$IsAfterAM==1 & EstDataSet$todcat>39,EstDataSet$AfterAM-(39-Peak_AM),0)

EstDataSet$BeforePM.Step1<-ifelse(EstDataSet$IsBeforePM==1, EstDataSet$BeforePM,0)
EstDataSet$BeforePM.Step2<-ifelse(EstDataSet$IsBeforePM==1 & EstDataSet$todcat<66,EstDataSet$BeforePM-(Peak_PM-66),0)
EstDataSet$BeforePM.Step3<-ifelse(EstDataSet$IsBeforePM==1 & EstDataSet$todcat<62,EstDataSet$BeforePM-(Peak_PM-62),0)
EstDataSet$BeforePM.Step4<-ifelse(EstDataSet$IsBeforePM==1 & EstDataSet$todcat<58,EstDataSet$BeforePM-(Peak_PM-58),0)

EstDataSet$AfterPM.Step1<-ifelse(EstDataSet$IsAfterPM==1, EstDataSet$AfterPM,0)
EstDataSet$AfterPM.Step2<-ifelse(EstDataSet$IsAfterPM==1 & EstDataSet$todcat>71,EstDataSet$AfterPM-(71-Peak_PM),0)
EstDataSet$AfterPM.Step3<-ifelse(EstDataSet$IsAfterPM==1 & EstDataSet$todcat>79,EstDataSet$AfterPM-(79-Peak_PM),0)

if (TRUE){
  # calculate mean SD in time slices for the four variables
  BeforeAM.sd.mean<-cast(EstDataSet,BeforeAM~IFC_Est,mean,value="sd")
  BeforePM.sd.mean<-cast(EstDataSet,BeforePM~IFC_Est,mean,value="sd")
  AfterAM.sd.mean<-cast(EstDataSet,AfterAM~IFC_Est,mean,value="sd")
  AfterPM.sd.mean<-cast(EstDataSet,AfterPM~IFC_Est,mean,value="sd")
  EstDataSet.sd.mean<-cast(EstDataSet,todcat~IFC_Est,mean,value="sd")
  
  setnames(BeforeAM.sd.mean, c("1","2","3","4"), c("Freeways.SD","Arterials.SD","Ramps.SD", "Others.SD"))
  setnames(BeforePM.sd.mean, c("1","2","3","4"), c("Freeways.SD","Arterials.SD","Ramps.SD", "Others.SD"))
  setnames(AfterAM.sd.mean, c("1","2","3","4"), c("Freeways.SD","Arterials.SD","Ramps.SD", "Others.SD"))
  setnames(AfterPM.sd.mean, c("1","2","3","4"), c("Freeways.SD","Arterials.SD","Ramps.SD", "Others.SD"))
  setnames(EstDataSet.sd.mean, c("1","2","3","4"), c("Freeways.SD","Arterials.SD","Ramps.SD", "Others.SD"))
  
  if (OUTLIER) {
	# outlier method
    write.table(BeforeAM.sd.mean,"BeforeAM_SDMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(BeforePM.sd.mean,"BeforePM_SDMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(AfterAM.sd.mean,"AfterAM_SDMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(AfterPM.sd.mean,"AfterPM_SDMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(EstDataSet.sd.mean,"EstDataSet_SDMean_nooutlier.csv",sep = ",",row.names = FALSE)
    
  } else {
	# confidence score 10 method
    write.table(BeforeAM.sd.mean,"BeforeAM_SDMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(BeforePM.sd.mean,"BeforePM_SDMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(AfterAM.sd.mean,"AfterAM_SDMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(AfterPM.sd.mean,"AfterPM_SDMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(EstDataSet.sd.mean,"EstDataSet_SDMean_noCS10.csv",sep = ",",row.names = FALSE)
    
  }
}

if (TRUE){
  # calculate mean SD in time slices for the four variables
  BeforeAM.mean.mean<-cast(EstDataSet,BeforeAM~IFC_Est,mean,value="mean")
  BeforePM.mean.mean<-cast(EstDataSet,BeforePM~IFC_Est,mean,value="mean")
  AfterAM.mean.mean<-cast(EstDataSet,AfterAM~IFC_Est,mean,value="mean")
  AfterPM.mean.mean<-cast(EstDataSet,AfterPM~IFC_Est,mean,value="mean")
  EstDataSet.mean.mean<-cast(EstDataSet,todcat~IFC_Est,mean,value="mean")
  
  setnames(BeforeAM.mean.mean, c("1","2","3","4"), c("Freeways.Mean","Arterials.Mean","Ramps.Mean", "Others.Mean"))
  setnames(BeforePM.mean.mean, c("1","2","3","4"), c("Freeways.Mean","Arterials.Mean","Ramps.Mean", "Others.Mean"))
  setnames(AfterAM.mean.mean, c("1","2","3","4"), c("Freeways.Mean","Arterials.Mean","Ramps.Mean", "Others.Mean"))
  setnames(AfterPM.mean.mean, c("1","2","3","4"), c("Freeways.Mean","Arterials.Mean","Ramps.Mean", "Others.Mean"))
  setnames(EstDataSet.mean.mean, c("1","2","3","4"), c("Freeways.Mean","Arterials.Mean","Ramps.Mean", "Others.Mean"))
  
  if (OUTLIER) {
	# outlier method
    write.table(BeforeAM.mean.mean,"BeforeAM_MeanMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(BeforePM.mean.mean,"BeforePM_MeanMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(AfterAM.mean.mean,"AfterAM_MeanMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(AfterPM.mean.mean,"AfterPM_MeanMean_nooutlier.csv",sep = ",",row.names = FALSE)
    write.table(EstDataSet.mean.mean,"EstDataSet_MeanMean_nooutlier.csv",sep = ",",row.names = FALSE)
    
  } else {
	# confidence score 10 method
    write.table(BeforeAM.mean.mean,"BeforeAM_MeanMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(BeforePM.mean.mean,"BeforePM_MeanMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(AfterAM.mean.mean,"AfterAM_MeanMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(AfterPM.mean.mean,"AfterPM_MeanMean_noCS10.csv",sep = ",",row.names = FALSE)
    write.table(EstDataSet.mean.mean,"EstDataSet_MeanMean_noCS10.csv",sep = ",",row.names = FALSE)
    
  }
}

# InterchangeDistance
#EstDataSet$Upstream <- EstDataSet$upstream.distance
#EstDataSet$Downstream <- EstDataSet$downstream.distance
EstDataSet$MajorUpstream <- EstDataSet$majorupstream.distance
EstDataSet$MajorDownstream <- EstDataSet$majordownstream.distance

if (FALSE){
	EstDataSet$AllInt.UpDist<-ifelse(EstDataSet$upstream.distance<0.5,"Short","Long")
	EstDataSet$AllInt.UpDist<-ifelse(EstDataSet$upstream.distance>=0.5 & EstDataSet$upstream.distance<=2,"Med",EstDataSet$AllInt.UpDist)
	EstDataSet$AllInt.DownDist<-ifelse(EstDataSet$downstream.distance<0.5,"Short","Long")
	EstDataSet$AllInt.DownDist<-ifelse(EstDataSet$downstream.distance>=0.5 & EstDataSet$downstream.distance<=2,"Med",EstDataSet$AllInt.DownDist)

	EstDataSet$AllInt.UpDist<-factor(EstDataSet$AllInt.UpDist, levels = c("Short","Med", "Long"))
	EstDataSet$AllInt.DownDist<-factor(EstDataSet$AllInt.DownDist, levels = c("Short","Med", "Long"))

	EstDataSet$MajInt.UpDist<-ifelse(EstDataSet$majorupstream.distance<0.5,"Short","Long")
	EstDataSet$MajInt.UpDist<-ifelse(EstDataSet$majorupstream.distance>=0.5 & EstDataSet$majorupstream.distance<=2,"Med",EstDataSet$MajInt.UpDist)
	EstDataSet$MajInt.DownDist<-ifelse(EstDataSet$majordownstream.distance<0.5,"Short","Long")
	EstDataSet$MajInt.DownDist<-ifelse(EstDataSet$majordownstream.distance>=0.5 & EstDataSet$majordownstream.distance<=2,"Med",EstDataSet$MajInt.DownDist)

	EstDataSet$MajInt.UpDist<-factor(EstDataSet$MajInt.UpDist, levels = c("Short","Med", "Long"))
	EstDataSet$MajInt.DownDist<-factor(EstDataSet$MajInt.DownDist, levels = c("Short","Med", "Long"))
}

EstDataSet$MajorUpstream.Inverse <- (1/EstDataSet$MajorUpstream)
EstDataSet$MajorDownstream.Inverse <- (1/EstDataSet$MajorDownstream)

EstDataSet$AuxLanesBinary <- ifelse(EstDataSet$AuxLanes>0,1,0)
EstDataSet$ISPD70 <- ifelse(EstDataSet$ISPD==70,1,0)

# subset by facility type - four for now (freeways, arterials, ramps, and others)
EstDataSet.freeways <- subset(EstDataSet,IFC_Est==1)
EstDataSet.arterials <- subset(EstDataSet,IFC_Est==2)
EstDataSet.ramps <- subset(EstDataSet,IFC_Est==3)
EstDataSet.others <- subset(EstDataSet,IFC_Est==4)

# ----------------------------- 1.FREEWAYS -----------------------------
# remove 1-lane freeways
EstDataSet.freeways <- subset(EstDataSet.freeways,NumLanes>1)

# add lane change distance (12/7/2015)
LaneChangeDistance <-LaneChangeDistance[,c("LinkID","LaneIncrease","LaneDrop","DeadEnd","RegionEnd","DownstreamDistance")]
temp<-merge(EstDataSet.freeways,LaneChangeDistance,by.x ="ID1",by.y = "LinkID",all.x=TRUE)

EstDataSet.freeways<-temp
EstDataSet.freeways$Down.Lane.Increase<-0
EstDataSet.freeways$Down.Lane.Drop<-0

EstDataSet.freeways$Down.Lane.Increase<-ifelse(!is.na(EstDataSet.freeways$LaneIncrease) & EstDataSet.freeways$LaneIncrease>0,EstDataSet.freeways$DownstreamDistance,0)
EstDataSet.freeways$Down.Lane.Drop<-ifelse(!is.na(EstDataSet.freeways$LaneDrop) & EstDataSet.freeways$LaneDrop>0,EstDataSet.freeways$DownstreamDistance,0)

EstDataSet.freeways$Down.Lane.Increase.Inv<-ifelse(!is.na(EstDataSet.freeways$LaneIncrease) & EstDataSet.freeways$LaneIncrease>0,1/EstDataSet.freeways$DownstreamDistance,0)
EstDataSet.freeways$Down.Lane.Drop.Inv<-ifelse(!is.na(EstDataSet.freeways$LaneDrop) & EstDataSet.freeways$LaneDrop>0,1/EstDataSet.freeways$DownstreamDistance,0)

# add upstream/downstream node ramp meter (12/16/2015) - only within the periods of 7am-9am and 4pm-6pm
FreewayRampMeters <-FreewayRampMeters[,c("LinkID","IsRampMeterUp","IsRampMeterDown")]
temp<-merge(EstDataSet.freeways,FreewayRampMeters,by.x ="ID1",by.y = "LinkID",all.x=TRUE)
EstDataSet.freeways<-temp

EstDataSet.freeways$DownNode.RampMeter<-0
EstDataSet.freeways$DownNode.RampMeter<-ifelse(!is.na(EstDataSet.freeways$IsRampMeterDown) & EstDataSet.freeways$IsRampMeterDown==1 
                                               & ((EstDataSet.freeways$todcat>=28 & EstDataSet.freeways$todcat<=36)
                                               | (EstDataSet.freeways$todcat>=64 & EstDataSet.freeways$todcat<=72)),1,0)

# Regression model
if (FALSE) {
  model.freeways = lm(sdpermean~LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.Med.Up+LOSF.High.Up+ISPD70+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                      +AfterAM.Step1+AfterAM.Step2+AfterAM.Step3+BeforePM.Step1+BeforePM.Step2+BeforePM.Step3+BeforePM.Step4
                      +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3
                      +MajorUpstream.Inverse+MajorDownstream.Inverse, data=EstDataSet.freeways)
}

# significant - remove ISPD70
model.freeways = lm(sdpermean~LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.High.Up+ISPD70+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                    +AfterAM.Step1+BeforePM.Step1+BeforePM.Step2+BeforePM.Step3
                    +AfterPM.Step1+AfterPM.Step3
                    +MajorUpstream.Inverse+MajorDownstream.Inverse, data=EstDataSet.freeways)

# no shift vars
model.freeways = lm(sdpermean~LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.High.Up+ISPD70
                    +MajorUpstream.Inverse+MajorDownstream.Inverse, data=EstDataSet.freeways)


summary(model.freeways)

# ---------------------------- 2.ARTERIALS -----------------------------
# Arterials - segment speed (12/7/2015)
EstDataSet.arterials$ISPD.Cat<-""
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD<35,"ISPD35Less",EstDataSet.arterials$ISPD.Cat)
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD==35,"ISPD35",EstDataSet.arterials$ISPD.Cat)
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD==40,"ISPD40",EstDataSet.arterials$ISPD.Cat)
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD==45,"ISPD45",EstDataSet.arterials$ISPD.Cat)
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD==50,"ISPD50",EstDataSet.arterials$ISPD.Cat)
EstDataSet.arterials$ISPD.Cat <- ifelse(EstDataSet.arterials$ISPD>50,"ISPD50More",EstDataSet.arterials$ISPD.Cat)
#set order of factors
EstDataSet.arterials$ISPD.Cat<-factor(EstDataSet.arterials$ISPD.Cat, levels = c("ISPD35Less","ISPD35", "ISPD40", "ISPD45","ISPD50", "ISPD50More"))

EstDataSet.arterials<-subset(EstDataSet.arterials,!is.na(EstDataSet.arterials$VOC))
# Regression model
if (FALSE) {
  model.arterials = lm(sdpermean~NumLanesCat+LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.Med.Up+LOSF.High.Up
                       +ISPD.Cat+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                       +AfterAM.Step1+AfterAM.Step2+AfterAM.Step3+BeforePM.Step1+BeforePM.Step2+BeforePM.Step3+BeforePM.Step4
                       +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3+ICNT.Est, data=EstDataSet.arterials)
}

model.arterials = lm(sdpermean~NumLanesCat+LOSC.Up+LOSF.Low.Up
                     +ISPD.Cat+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                     +AfterAM.Step1+BeforePM.Step1+BeforePM.Step3
                     +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3+ICNT.Est, data=EstDataSet.arterials)
# no shift vars
model.arterials = lm(sdpermean~NumLanesCat+LOSC.Up+LOSF.Low.Up
                     +ISPD.Cat
                     +ICNT.Est, data=EstDataSet.arterials)

# different measures of capacity
model.arterials = lm(sdpermen~GCRatio+RightLanes+LeftLanes
                     +ISPD.Cat+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                     +AfterAM.Step1+BeforePM.Step1+BeforePM.Step3
                     +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3+ICNT.Est, data=EstDataSet.arterials)

summary(model.arterials)

# significant - aggregate from lower LOS

# ---------------------------- 3.RAMPS ---------------------------------
model.ramps = lm(sdpermean~LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.Med.Up+LOSF.High.Up
                 +BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                 +AfterAM.Step1+AfterAM.Step2+AfterAM.Step3+BeforePM.Step1+BeforePM.Step2+BeforePM.Step3+BeforePM.Step4
                 +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3+ICNT.RampMeter, data=EstDataSet.ramps)

# only significant - don't include after LOSE.Up
model.ramps = lm(sdpermean~LOSC.Up+LOSE.Up
                 +BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                 +AfterAM.Step1+AfterAM.Step2+AfterAM.Step3+BeforePM.Step1+BeforePM.Step2+BeforePM.Step4
                 +AfterPM.Step1+AfterPM.Step3+ICNT.RampMeter, data=EstDataSet.ramps)

# no shift vars
model.ramps = lm(sdpermean~LOSC.Up+LOSE.Up
                 +ICNT.RampMeter, data=EstDataSet.ramps)


summary(model.ramps)

# ---------------------------- 4.OTHERS --------------------------------
# combine twolane and threelanes for others
EstDataSet.others$NumLanesCat <- ifelse(EstDataSet.others$NumLanes==1,"OneLane","NoLane")
EstDataSet.others$NumLanesCat <- ifelse(EstDataSet.others$NumLanes>=2,"TwoLane+",EstDataSet.others$NumLanesCat)
EstDataSet.others$NumLanesCat<-factor(EstDataSet.others$NumLanesCat, levels = c("NoLane","OneLane", "TwoLane+"))

EstDataSet.others$OneLane <- ifelse(EstDataSet.others$NumLanes==1,1,0)
EstDataSet.others$TwoLane <- ifelse(EstDataSet.others$NumLanes>=2,1,0)
EstDataSet.others$ThreeLane <- 0

# Regression model
model.others = lm(sdpermean~LOSC.Up+LOSD.Up+LOSE.Up+LOSF.Low.Up+LOSF.Med.Up+LOSF.High.Up
                  +ISPD+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                  +AfterAM.Step1+AfterAM.Step2+AfterAM.Step3+BeforePM.Step1+BeforePM.Step2+BeforePM.Step3+BeforePM.Step4
                  +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3, data=EstDataSet.others)

model.others = lm(sdpermean~ISPD+BeforeAM.Step1+BeforeAM.Step2+BeforeAM.Step3+BeforeAM.Step4
                  +AfterAM.Step1+BeforePM.Step1+BeforePM.Step3
                  +AfterPM.Step1+AfterPM.Step2+AfterPM.Step3, data=EstDataSet.others)

# no shift vars
model.others = lm(sdpermean~ISPD, data=EstDataSet.others)

# view results
summary(model.others)

# ---------------------------------- Correlation Matrix -------------------------------------
library(Hmisc)
library(corrplot)

# Freeways
# keep only a few variables
temp1<-EstDataSet.freeways[,c("sd","sdpermean",
                              "ISPD70","VOC","LOSC.Up","LOSD.Up","LOSE.Up","LOSF.Low.Up","LOSF.Med.Up","LOSF.High.Up",
                              "BeforeAM.Step1","BeforeAM.Step2","BeforeAM.Step3","BeforeAM.Step4","AfterAM.Step1","AfterAM.Step2","AfterAM.Step3",
                              "BeforePM.Step1","BeforePM.Step2","BeforePM.Step3","BeforePM.Step4","AfterPM.Step1","AfterPM.Step2","AfterPM.Step3",
                              "MajorUpstream.Inverse","MajorDownstream.Inverse")]
mcor<-rcorr(as.matrix(temp1))
# make plot and write the correlations to a csv file
corrplot(mcor$r,type="upper",tl.col="black",tl.srt=45)
write.table(mcor$r,"correlation_freeways.csv",row.names = FALSE, sep = ",")

# Arterials
temp2<-EstDataSet.arterials[,c("sd","sdpermean","NumLanes","OneLane","TwoLane","ThreeLane","FourLane",
                               "AuxLanesBinary","ISPD","VOC","LOSC","LOSD","LOSE",
                               "LOSF_Low","LOSF_Med","LOSF_High","ICNT.Signal","ICNT.Stop","ICNT.RailRoad",
                               "IsBeforeAM.Shift","IsAfterAM.Shift","IsBeforePM.Shift","IsAfterPM.Shift")]
mcor<-rcorr(as.matrix(temp2))
# make plot and write the correlations to a csv file
corrplot(mcor$r,type="upper",tl.col="black",tl.srt=45)
write.table(mcor$r,"correlation_arterials.csv",row.names = TRUE, sep = ",")

# Ramps
temp3<-EstDataSet.ramps[,c("sd","sdpermean","NumLanes","OneLane","TwoLane","ThreeLane","ISPD","VOC",
                           "IsBeforeAM.Shift","IsAfterAM.Shift","IsBeforePM.Shift","IsAfterPM.Shift")]
mcor<-rcorr(as.matrix(temp3))
# make plot and write the correlations to a csv file
corrplot(mcor$r,type="upper",tl.col="black",tl.srt=45)
write.table(mcor$r,"correlation_ramps.csv",row.names = TRUE, sep = ",")

# Others
temp4<-EstDataSet.others[,c("sd","sdpermean","NumLanes","OneLane","TwoLane","ISPD","VOC","LOSC","LOSD","LOSE",
                            "LOSF_Low","LOSF_Med",
                           "IsBeforeAM.Shift","IsAfterAM.Shift","IsBeforePM.Shift","IsAfterPM.Shift")]
mcor<-rcorr(as.matrix(temp4))
# make plot and write the correlations to a csv file
corrplot(mcor$r,type="upper",tl.col="black",tl.srt=45)
write.table(mcor$r,"correlation_others.csv",row.names = TRUE, sep = ",")

# ----------------------- PLOTS ------------
p1<-plot(EstDataSet.freeways$VOC,EstDataSet.freeways$sdpermean,type="p")
p2<-plot(EstDataSet.arterials$VOC,EstDataSet.arterials$sdpermean,type="p")
p3<-plot(EstDataSet.ramps$VOC,EstDataSet.ramps$sdpermean,type="p")
p4<-plot(EstDataSet.others$VOC,EstDataSet.others$sdpermean,type="p")

# Shift variable plot
EstDataSet.sdmean<-cast(EstDataSet,todcat~IFC_Est,mean,value="sd")
EstDataSet.meanmean<-cast(EstDataSet,todcat~IFC_Est,mean,value="mean")
setnames(EstDataSet.sdmean,c("1","2","3","4"),c("Freeways","Arterials","Ramps","Others"))
setnames(EstDataSet.meanmean,c("1","2","3","4"),c("Freeways","Arterials","Ramps","Others"))

EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>0 & EstDataSet.sdmean$todcat<=14,'EV1','')
EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>14 & EstDataSet.sdmean$todcat<=24,'EA',EstDataSet.sdmean$tod.model)
EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>24 & EstDataSet.sdmean$todcat<=36,'AM',EstDataSet.sdmean$tod.model)
EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>36 & EstDataSet.sdmean$todcat<=62,'MD',EstDataSet.sdmean$tod.model)
EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>62 & EstDataSet.sdmean$todcat<=76,'PM',EstDataSet.sdmean$tod.model)
EstDataSet.sdmean$tod.model<-ifelse(EstDataSet.sdmean$todcat>76 & EstDataSet.sdmean$todcat<=96,'EV2',EstDataSet.sdmean$tod.model)

EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>0 & EstDataSet.meanmean$todcat<=14,'EV1','')
EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>14 & EstDataSet.meanmean$todcat<=24,'EA',EstDataSet.meanmean$tod.model)
EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>24 & EstDataSet.meanmean$todcat<=36,'AM',EstDataSet.meanmean$tod.model)
EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>36 & EstDataSet.meanmean$todcat<=62,'MD',EstDataSet.meanmean$tod.model)
EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>62 & EstDataSet.meanmean$todcat<=76,'PM',EstDataSet.meanmean$tod.model)
EstDataSet.meanmean$tod.model<-ifelse(EstDataSet.meanmean$todcat>76 & EstDataSet.meanmean$todcat<=96,'EV2',EstDataSet.meanmean$tod.model)

EstDataSet.sdmean$tod.model<-factor(EstDataSet.sdmean$tod.model, levels = c("EV1","EA", "AM", "MD", "PM", "EV2"))
EstDataSet.meanmean$tod.model<-factor(EstDataSet.meanmean$tod.model, levels = c("EV1","EA", "AM", "MD", "PM", "EV2"))


Peak_AM <- 32
Low_MD <- 60
Peak_PM <-76

EstDataSet.sdmean$shift.period<-ifelse(EstDataSet.sdmean$todcat<=Peak_AM,1,0) #blue
EstDataSet.sdmean$shift.period<-ifelse(EstDataSet.sdmean$todcat>=Peak_AM & EstDataSet.sdmean$todcat<=Low_MD,2,EstDataSet.sdmean$shift.period) # darkgreen
EstDataSet.sdmean$shift.period<-ifelse(EstDataSet.sdmean$todcat>=Low_MD & EstDataSet.sdmean$todcat<=Peak_PM,3,EstDataSet.sdmean$shift.period) # darkorange4
EstDataSet.sdmean$shift.period<-ifelse(EstDataSet.sdmean$todcat>=Peak_PM,4,EstDataSet.sdmean$shift.period) #chocolate4

EstDataSet.meanmean$shift.period<-ifelse(EstDataSet.meanmean$todcat<=Peak_AM,1,0) #blue
EstDataSet.meanmean$shift.period<-ifelse(EstDataSet.meanmean$todcat>=Peak_AM & EstDataSet.meanmean$todcat<=Low_MD,2,EstDataSet.meanmean$shift.period) # darkgreen
EstDataSet.meanmean$shift.period<-ifelse(EstDataSet.meanmean$todcat>=Low_MD & EstDataSet.meanmean$todcat<=Peak_PM,3,EstDataSet.meanmean$shift.period) # darkorange4
EstDataSet.meanmean$shift.period<-ifelse(EstDataSet.meanmean$todcat>=Peak_PM,4,EstDataSet.meanmean$shift.period) #chocolate4

#colors
EstDataSet.sdmean$shift.period.color<-ifelse(EstDataSet.sdmean$todcat<=Peak_AM,"#0000FF",0)
EstDataSet.sdmean$shift.period.color<-ifelse(EstDataSet.sdmean$todcat>=Peak_AM & EstDataSet.sdmean$todcat<=Low_MD,"#006400",EstDataSet.sdmean$shift.period.color)
EstDataSet.sdmean$shift.period.color<-ifelse(EstDataSet.sdmean$todcat>=Low_MD & EstDataSet.sdmean$todcat<=Peak_PM,"#BB4500",EstDataSet.sdmean$shift.period.color)
EstDataSet.sdmean$shift.period.color<-ifelse(EstDataSet.sdmean$todcat>=Peak_PM,"#BB4513",EstDataSet.sdmean$shift.period.color)

EstDataSet.meanmean$shift.period.color<-ifelse(EstDataSet.meanmean$todcat<=Peak_AM,"#0000FF",0)
EstDataSet.meanmean$shift.period.color<-ifelse(EstDataSet.meanmean$todcat>=Peak_AM & EstDataSet.meanmean$todcat<=Low_MD,"#006400",EstDataSet.meanmean$shift.period.color)
EstDataSet.meanmean$shift.period.color<-ifelse(EstDataSet.meanmean$todcat>=Low_MD & EstDataSet.meanmean$todcat<=Peak_PM,"#BB4500",EstDataSet.meanmean$shift.period.color)
EstDataSet.meanmean$shift.period.color<-ifelse(EstDataSet.meanmean$todcat>=Peak_PM,"#BB4513",EstDataSet.meanmean$shift.period.color)

#plot
p1<-ggplot(data=EstDataSet.sdmean,aes(x=todcat,y=Freeways)) + geom_point() + geom_line(aes(colour=EstDataSet.sdmean$shift.period.color)) + theme_bw()
p2<-ggplot(data=EstDataSet.meanmean,aes(x=todcat,y=Freeways)) + geom_point() + geom_line(aes(colour=EstDataSet.meanmean$shift.period.color)) + theme_bw()  %+replace% theme(panel.background = element_rect(fill = NA))

p<-p + facet_grid( . ~ tod.model, scales="free_x", space="free")

#p<-p + geom_line(colour=EstDataSet.sdmean$shift.period)
#p<-p + scale_colour_manual(breaks=EstDataSet.sdmean$shift.period, values = unique(as.character(EstDataSet.sdmean$shift.period.color)))

p<-p + theme_bw() + theme(panel.margin.x=unit(0,"lines"),panel.margin.y=unit(0.25,"lines"),
                          plot.title = element_text(lineheight=.8, face="bold", vjust=2))
p<-p+expand_limits(y = 0)

p<-p+scale_x_continuous(breaks = seq(0,96, by = 1), expand=c(0,0))+scale_y_continuous(expand = c(0, 0))

p<-p + labs(x="Time of Day Bins",y="Travel Time SD", title="Travel Time Reliability")
p<-p+theme(plot.title = element_text(lineheight=.8, face="bold", vjust=2))

