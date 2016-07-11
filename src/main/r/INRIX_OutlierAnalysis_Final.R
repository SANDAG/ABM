###
# OBJECTIVE:
# outlier analysis for SANDAG INRIX travel time data
# travel time and speed plots
# std dev and mean

# INPUTS:
# inrix data - "2012_10.csv"
# inrix to tcoved correspondence (created by Fehr and Peers) - "inrix_2012hwy.txt" (also an input to regression analysis script)

# OUTPUTS:
# travel time SD/ mean travel time in 15 mins - "inrix2012_nooutliers_traveltime.dbf" (input to regression analysis script)

# INRIX DATA:
# october 2012 inrix data is used.
# the inrix data are in 1-mins increment and available for freeways, major/minor arterials, collectors and ramps.

# INRIX TO MODEL NETWORK CORRESPONDENCE:
# Doubtful if ramps are in the data, perhaps correspondence to ramps isn't correct.
# fehr and peers established an initial correspondence between the model network segments and INRIX TMC segments.
# the correspondence included one record for each model link-to-inrix segments. Some model links may corresponds to multiple TMCs, 
# a TMC proportion which is proportion of the TMC feature covered by the corresponding model network feature was also provided.
# The records that may not represent true correspondence, due to high frequency of link join, were flagged in the correspondence.

# DATA PROCESSING:
# While preparing the data, the records that were flagged in the correspondence file were removed. 
# Then, a TMC segment was assigned with one model link by finding the record with the highest TMC proportion. The characteristics of that model link were attached to the TMC segment.
# The analysis was restricted to weekdays. weekend’s data points were eliminated from the dataset
# plots of speeds and travel times are created
# outlier analysis using adjusted boxplot
# removed data points in period 4:15 am - 4:30 am (unexpected variation). also, removed based on reference_speed (if "reference_speed-speed>5")
# comparison of before and after outlier analysis plots
# travel time variability (standard deviation) is calculated for every 15 minutes time interval
# travel time unit = travel time per sec per mile (=travel_time_sec/seg.length)
# seg.length:=speed*travel_time_minutes/60
# for a tmc segment (travel time sd/travel time mean) is calculated over all weekdays in 15-mins bins

# OTHER SCRIPTS USED:
# utilfunc.R

# by: nagendra.dhakar@rsginc.com
# for: SANDAG SHRP C04 - Tolling and Reliability
###

library(foreign)
library (stringr)
library(xtable)
library(reshape)
library(reshape2)
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
setwd("E:/Projects/Clients/SANDAG/Data_2015")
source("utilfunc.R")

# input files
inrix_2012_file                           = "./INRIX/2012_10/2012_10.csv"
inrix_2014_file                           = "./INRIX/2014_10/october_2014.csv"
inrixhwycorresp_file                      = "./TMC/inrix_2012hwy.txt"

# config settings
year = "2012" # data year
interval<-15  # time bin interval in minutes
bptype<-"adjusted"  # box plot - original or adjusted
field="travel.time.sec.per.mile"
ByDOW<-FALSE  # by day of week - false, analysis is performed for all weekdays
OUTLIER<-TRUE # outlier analysis or confidence score (CS) removal

# -------------------- 1. LOAD DATA ----------------------

# read and load inrix data
if (year=="2012"){
	# read and load 2012 data
  outputsDir="./INRIX/2012_10/"
	readSaveRdata(inrix_2012_file,"inrix_2012")
	inrix_2012 <- assignLoad(paste0(inrix_2012_file,".Rdata"))
} else{
	# read and load 2014 data
  outputsDir="./INRIX/2014_10/"
	readSaveRdata(inrix_2014_file,"inrix_2014")
	inrix_2014 <- assignLoad(paste0(inrix_2014_file,".Rdata"))
}

# read and load inrix to hwy correspondence data
readSaveRdata(inrixhwycorresp_file,"inrixhwycorresp")
inrixhwycorresp <- assignLoad(paste0(inrixhwycorresp_file,".Rdata"))

# -------------------- 2. CLEAN/PROCESS DATA --------------

# add a new field tmc_code without first character ('-' or '+')
inrixhwycorresp[,tmc_code:=substr(TMC,2,nchar(TMC))]

# remove the segments that are flagged 
inrixhwycorresp<-subset(inrixhwycorresp,FLAG==0)

# get unique tmc_code with variables corresponding to maximum of TMCProp
df.orig <-inrixhwycorresp
df.agg<-aggregate(TMCProp~tmc_code,inrixhwycorresp,max)
df.max <- merge(df.agg, df.orig)

# facility type - keep only two columns
#tmc_ifc <-df.max[,c("tmc_code","IFC","HWYCOV_ID"),]
tmc_ifc <-df.max[,c("tmc_code","TMC_Len","IFC","HWYCOV_ID","NM","LENGTH","ISPD","ABLNO","ABLNA","ABLNP","BALNO","BALNA","BALNP","ABCNT","BACNT","IHOV","ABAU","BAAU"),]
tmc_ifc$LENGTH<-as.numeric(gsub(",","",tmc_ifc$LENGTH))
tmc_ifc$HWYCOV_ID<-as.numeric(gsub(",","",tmc_ifc$HWYCOV_ID))

# for plots
tmc_ifc_temp <-df.max[,c("tmc_code","IFC"),]

# merge INRIX travel time data with hwyinrix correspondence file
inrix_2012_ifc<-merge(inrix_2012,tmc_ifc,by="tmc_code")

# set data
alldata<-inrix_2012_ifc

# determine Day Of Week (DOW): weekday (1) or weekend (0)
alldata[,DOW:=ifelse(isWeekday(as.Date(measurement_tstamp)),1,0)]

# keep only weekdays
alldata<-alldata[DOW==1]

# calculate time stamp
alldata[,date:=substr(measurement_tstamp,0,10)]
alldata[,hour:=as.numeric(substr(measurement_tstamp, 12, 13))]
alldata[,min:=as.numeric(substr(measurement_tstamp, 15, 16))]
alldata[,totalmin:=hour*60+min]
alldata[,totalhour:=hour+(min/60)]

#travel time
alldata[,travel_time_sec:=travel_time_minutes*60]

# determine time of day (TOD)    
alldata[totalmin>=0 & totalmin<=209,tod:='EV1']
alldata[totalmin>=210 & totalmin<=359,tod:='EA']  #1-early AM
alldata[totalmin>=360 & totalmin<=539,tod:='AM']  #2-AM peak
alldata[totalmin>=540 & totalmin<=929,tod:='MD']  #3-Midday
alldata[totalmin>=930 & totalmin<=1139,tod:='PM'] #4-PM Peak
alldata[totalmin>=1140 & totalmin<=1440,tod:='EV2']

# create time intervals
alldata[,todcat:=findInterval(totalmin,seq(0,1440,by=interval))]

# --------------- 3. ANALYSIS (OUTLIER DETECTION) -------------------

if (OUTLIER) {
  # ---------------- outlier analysis --------------------
  # create a columns "outlier" with default set to 0
  alldata[,outlier:=0]
  
  # find tmc segments
  segmentslist<-unique(alldata$tmc_code)
  
  if (length(segmentslist)==0) {
    stop("no tmc segments in the dataset")
  }
  
  # create dataframes by tmc_code
  alldata_bytmc<-by(alldata,alldata$tmc_code, function(x) x)
  
  if (length(segmentslist)>length(alldata_bytmc)) {
    warning("not all tmc segments have dataframe")
  }
  
  # identify outliers for each dataframe
  lapply(alldata_bytmc, detectoutliers)
  
  # combine dataframes into one dataset
  alldata.outliers<-do.call(rbind,alldata_bytmc)
  
  # calculate length (mile)
  alldata.outliers[,seg.length:=speed*travel_time_minutes/60]
  alldata.outliers[,travel.time.sec.per.mile:=travel_time_sec/seg.length]
  
  #free-up memory
  #rm(alldata)
  rm(inrix_2012)
  gc()
  
  if (sum(alldata.outliers$outlier)==0) {
    stop("no outliers are detected")
  }
  
  outliers_byIFC<-c(0,0,0,0,0,0,0,0,0)
  outliers_byIFC[1]<-sum(alldata.outliers[IFC==1]$outlier)
  outliers_byIFC[2]<-sum(alldata.outliers[IFC==2]$outlier)
  outliers_byIFC[3]<-sum(alldata.outliers[IFC==3]$outlier)
  outliers_byIFC[4]<-sum(alldata.outliers[IFC==4]$outlier)
  outliers_byIFC[5]<-sum(alldata.outliers[IFC==5]$outlier)
  outliers_byIFC[6]<-sum(alldata.outliers[IFC==6]$outlier)
  outliers_byIFC[7]<-sum(alldata.outliers[IFC==7]$outlier)
  outliers_byIFC[8]<-sum(alldata.outliers[IFC==8]$outlier)
  outliers_byIFC[9]<-sum(alldata.outliers[IFC==9]$outlier)
 
  # also remove data points in period 4:15 am - 4:30 am. also, remove based on reference_speed
  alldata.nooutliers<-alldata.outliers[todcat==18 & reference_speed-speed>5,outlier:=1]  
  
  # remove outliers
  alldata.nooutliers<-alldata.outliers[outlier==0]
  
} else {
  # filter out data points based on confidence score
  # lose about 19% of the data points
  alldata.nooutliers <- alldata[alldata$confidence_score>10]
  alldata.nooutliers[,seg.length:=speed*travel_time_minutes/60]
  alldata.nooutliers[,travel.time.sec.per.mile:=travel_time_sec/seg.length]
}

# free-up memory
rm(alldata_bytmc)
rm(alldata)
rm(alldata.outliers)
rm(inrix_2012_ifc)
gc()

alldata.nooutliers$dayofweek<-dayOfWeek(as.timeDate(alldata.nooutliers$date))

# ---------------- 4. OUTPUT ----------------------
if (OUTLIER){
  # write out the new dataset
  write.table(alldata.nooutliers,"inrix_2012_nooutliers_15mins.txt",sep=",",row.names=F,quote=F)
} else {
  # write out the new dataset - no confidence score 10
  write.table(alldata.nooutliers,"inrix_2012_noCS10_15mins.txt",sep=",",row.names=F,quote=F)
}


if (FALSE){
  # for test
  withnooutliers_file="./inrix_2012_nooutliers.txt"
  readSaveRdata(withnooutliers_file,"inrix_2012_nooutliers")
  inrix_2012_nooutliers <- assignLoad(paste0(withnooutliers_file,".Rdata"))
  alldata.nooutliers <- inrix_2012_nooutliers
  
  alldata.nooutliers[,seg.length:=speed*travel_time_minutes/60]
  alldata.nooutliers[,travel.time.sec.per.mile:=travel_time_sec/seg.length]
  
  # for test
  withoutliers_file="./inrix_2012_outliers.txt"
  readSaveRdata(withoutliers_file,"inrix_2012_outliers")
  inrix_2012_outliers <- assignLoad(paste0(withoutliers_file,".Rdata"))
  
  inrix_2012_outliers[,seg.length:=speed*travel_time_minutes/60]
  inrix_2012_outliers[,travel.time.sec.per.mile:=travel_time_sec/seg.length]
  
  alldata.outliers=inrix_2012_outliers
  alldata.nooutliers=inrix_2012_outliers[outlier==0]
  
}

# ----------------- 5. Statistics -----------------

# for raw data set - outliers are included (though identified)
if (FALSE){
  # calculate SD - all data points
  #temp<-cast(alldata.outliers,tmc_code~todcat,sd,value=field)
  temp<-dcast(alldata.outliers,tmc_code+date+todcat~field,fun.aggregate=sd)
  
  alldata.outliers.sd<-temp[complete.cases(temp),] # keeps only complete values - no missing values
  temp=alldata.outliers.sd
  temp<-merge(temp,tmc_ifc,by="tmc_code")
  
  #output SD file
  #outfile=paste("inrix2012_outliers_",field,"_sd.txt")
  #write.table(temp,outfile,sep="\t",row.names=F,quote=F)
  
  outfile=paste("inrix2012_outliers_",field,"_sd.dbf")
  write.dbf(temp,outfile)
}

# save data
if (OUTLIER) {
  # outlier method
  save(alldata.nooutliers,file = "alldatanooutliers.Rdata")
} else {
  # confidence score 10 method
  save(alldata.nooutliers,file = "alldatanoCS10.Rdata")
}

# load data
if (OUTLIER) {
  # outlier method
  alldata.nooutliers <- assignLoad("alldatanooutliers.Rdata")
} else {
  # confidence score 10 method
  alldata.nooutliers <- assignLoad("alldatanoCS10.Rdata")
}

# std. dev of all data points
if (!ByDOW){
  # std dev for all weekdays
  alldata.nooutliers.sd<-cast(alldata.nooutliers,tmc_code~todcat,sd,value=field)
  
  # reshape the dataset
  alldata.nooutliers.sd.melt<-melt(alldata.nooutliers.sd,id="tmc_code")
  setnames(alldata.nooutliers.sd.melt,"value","sd")
  
  # field name got changed to "value" due to cast in the previous step - set back to field name
  setnames(alldata.nooutliers,"value",field)
}

if (FALSE) {
  # avg speed for Wu - 05/09/2016
  alldata.nooutliers.mean<-aggregate(alldata.nooutliers$speed,by=list(alldata.nooutliers$tmc_code, alldata.nooutliers$todcat),FUN = mean, na.rm=TRUE, na.action = na.pass)
  tmc_length<-aggregate(alldata.nooutliers$seg.length,by=list(alldata.nooutliers$tmc_code),FUN = mean, na.rm=TRUE, na.action = na.pass)  
  
  setnames(alldata.nooutliers.mean,c("Group.1","Group.2","x"),c("tmc_code","todcat","avg_speed"))
  setnames(tmc_length,c("Group.1","x"),c("tmc_code","avg_tmc_length"))
  
  alldata.nooutliers.mean.hwycov<-merge(alldata.nooutliers.mean,tmc_ifc,by="tmc_code")
  alldata.nooutliers.mean.hwycov<-merge(alldata.nooutliers.mean.hwycov,tmc_length,by="tmc_code")
  
  alldata.nooutliers.mean.hwycov$TMC_Len<-as.numeric(gsub(",","",alldata.nooutliers.mean.hwycov$TMC_Len))
  write.table(alldata.nooutliers.mean.hwycov,"inrix_2012_avgspeed_wu.csv",sep=",",row.names=F,quote=F)
  
}

# std. dev of all datapoints of a weekday (ex. all monday, all tuesday,.., all friday) - not used
if (ByDOW) {
  alldata.nooutliers.sd<-dcast(alldata.nooutliers,tmc_code+todcat~dayofweek,value.var=field,fun.aggregate=sd,na.rm=TRUE)
  
  if (FALSE){
    
    if (field=="speed"){
      alldata.nooutliers.sd<-aggregate(alldata.nooutliers$speed,by=list(alldata.nooutliers$tmc_code,alldata.nooutliers$dayofweek,alldata.nooutliers$todcat),FUN = sd)
    } else {
      alldata.nooutliers.sd<-aggregate(alldata.nooutliers$travel.time.sec.per.mile,by=list(alldata.nooutliers$tmc_code,alldata.nooutliers$dayofweek,alldata.nooutliers$todcat),FUN = sd)
    }
  }
  # reshape the dataset - not needed in aggregate method
  alldata.nooutliers.sd.melt<-melt(alldata.nooutliers.sd,id=c("tmc_code","todcat"))
  setnames(alldata.nooutliers.sd.melt,c("variable","value"),c("dayofweek","sd"))
  
}

#alldata.nooutliers.sd<-alldata.nooutliers.sd[complete.cases(alldata.nooutliers.sd),]  

# For Shift Variables - now they are calculated in regression analysis script
# calculate mean

# all data points
if (!ByDOW){
  # calculate mean
  alldata.nooutliers.mean<-dcast(alldata.nooutliers,tmc_code~todcat,value.var=field,fun.aggregate=mean,na.rm=TRUE)
  alldata.nooutliers.mean.melt<-melt(alldata.nooutliers.mean,id="tmc_code")
  setnames(alldata.nooutliers.mean.melt,c("variable","value"),c("todcat","mean"))
}

# data points by weekday
if (ByDOW){
  alldata.nooutliers.mean<-dcast(alldata.nooutliers,tmc_code+todcat~dayofweek,value.var=field,fun.aggregate=mean,na.rm=TRUE)
  alldata.nooutliers.mean.melt<-melt(alldata.nooutliers.mean,id=c("tmc_code","todcat"))
  
  # set column names
  setnames(alldata.nooutliers.mean.melt,c("variable","value"),c("dayofweek","mean"))
  
}

# five model time periods
alldata.nooutliers.mean.melt$todcat.int <- as.integer(alldata.nooutliers.mean.melt$todcat)

alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>0 & alldata.nooutliers.mean.melt$todcat.int<=14,'EV','')
alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>14 & alldata.nooutliers.mean.melt$todcat.int<=24,'EA',alldata.nooutliers.mean.melt$tod)
alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>24 & alldata.nooutliers.mean.melt$todcat.int<=36,'AM',alldata.nooutliers.mean.melt$tod)
alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>36 & alldata.nooutliers.mean.melt$todcat.int<=62,'MD',alldata.nooutliers.mean.melt$tod)
alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>62 & alldata.nooutliers.mean.melt$todcat.int<=76,'PM',alldata.nooutliers.mean.melt$tod)
alldata.nooutliers.mean.melt$tod<-ifelse(alldata.nooutliers.mean.melt$todcat.int>76 & alldata.nooutliers.mean.melt$todcat.int<=96,'EV',alldata.nooutliers.mean.melt$tod)

if (FALSE)
{
  # max time by tmc_code and dayofweek
  #MaxTime.AM<-do.call(rbind,lapply(split(MeanTime.AM,list(MeanTime.AM$tmc_code,MeanTime.AM$dayofweek)), function(x) x[which.max(x$mean),]))
  #MaxTime.AM<-MaxTime.AM[,c("tmc_code","dayofweek","todcat")]
  #setnames(MaxTime.AM,"todcat","MaxAMtod")
  
  MaxTime.AM<-do.call(rbind,lapply(split(MeanTime.AM,list(MeanTime.AM$tmc_code)), function(x) x[which.max(x$mean),]))
  MaxTime.AM<-MaxTime.AM[,c("tmc_code","todcat.int")]
  setnames(MaxTime.AM,"todcat.int","MaxAMtod")
  
  #MaxTime.PM<-do.call(rbind,lapply(split(MeanTime.PM,list(MeanTime.PM$tmc_code,MeanTime.PM$dayofweek)), function(x) x[which.max(x$mean),]))
  #MaxTime.PM<-MaxTime.PM[,c("tmc_code","dayofweek","todcat")]
  #setnames(MaxTime.PM,"todcat","MaxPMtod")
  
  MaxTime.PM<-do.call(rbind,lapply(split(MeanTime.PM,list(MeanTime.PM$tmc_code)), function(x) x[which.max(x$mean),]))
  MaxTime.PM<-MaxTime.PM[,c("tmc_code","todcat.int")]
  setnames(MaxTime.PM,"todcat.int","MaxPMtod") 
  
  # merge
  #temp<-merge(alldata.nooutliers.sd.melt,MaxTime.AM,by=c("tmc_code","dayofweek"), all.x=TRUE)
  #temp1<-merge(temp,MaxTime.PM,by=c("tmc_code","dayofweek"), all.x=TRUE)
  
  temp<-merge(alldata.nooutliers.sd.melt,MaxTime.AM,by="tmc_code", all.x=TRUE)
  temp1<-merge(temp,MaxTime.PM,by="tmc_code", all.x=TRUE)
}

# merge sd and mean values
temp<-merge(alldata.nooutliers.sd.melt,alldata.nooutliers.mean.melt,by=c("tmc_code","todcat"))
temp$sdpermean<-temp$sd/temp$mean

# estimation dataset
data.est <- temp
data.est <- data.est[,c("tmc_code","todcat","sd","mean","todcat.int","tod","sdpermean")]

# save data
if (OUTLIER) {
  save(data.est,file = "data.est.nooutliers.Rdata")
} else {
  save(data.est,file = "data.est.noCS10.Rdata")
}

# load data
if (OUTLIER) {
  data.est <- assignLoad("data.est.nooutliers.Rdata")
} else {
  data.est <- assignLoad("data.est.noCS10.Rdata")
}

data.est<-na.omit(data.est)

# don't merge attributes here
#data.est<-merge(data.est,tmc_ifc,by="tmc_code")

if (OUTLIER) {
  # output without outliers SD file - in DBF format
  outfile="inrix2012_nooutliers_traveltime.dbf"
} else {
  outfile="inrix2012_noCS10_traveltime.dbf"
}

# write to dbf file
write.dbf(data.est,outfile)

# output without outliers SD file - in text format
if (FALSE){
  outfile=paste("inrix2012_nooutliers_",field,"_sd.txt")
  write.table(temp,outfile,sep="\t",row.names=F,quote=F)
}

rm(alldata.nooutliers.mean)
gc()

# ----------------- 6. Plots -----------------

# arrange data
alldata.outliers.sd.melt<-melt(alldata.outliers.sd,id="tmc_code")
alldata.nooutliers.sd.melt<-melt(alldata.nooutliers.sd,id="tmc_code")

# determine time of day (TOD)    

alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=0 & alldata.outliers.sd.melt$todcat<=7,'EV1','')
alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=7 & alldata.outliers.sd.melt$todcat<=12,'EA',alldata.outliers.sd.melt$tod)
alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=12 & alldata.outliers.sd.melt$todcat<=18,'AM',alldata.outliers.sd.melt$tod)
alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=18 & alldata.outliers.sd.melt$todcat<=31,'MD',alldata.outliers.sd.melt$tod)
alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=31 & alldata.outliers.sd.melt$todcat<=38,'PM',alldata.outliers.sd.melt$tod)
alldata.outliers.sd.melt$tod<-ifelse(alldata.outliers.sd.melt$todcat>=38 & alldata.outliers.sd.melt$todcat<=48,'EV2',alldata.outliers.sd.melt$tod)

alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=0 & alldata.nooutliers.sd.melt$todcat<7,'EV1','')
alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=7 & alldata.nooutliers.sd.melt$todcat<12,'EA',alldata.nooutliers.sd.melt$tod)
alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=12 & alldata.nooutliers.sd.melt$todcat<18,'AM',alldata.nooutliers.sd.melt$tod)
alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=18 & alldata.nooutliers.sd.melt$todcat<31,'MD',alldata.nooutliers.sd.melt$tod)
alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=31 & alldata.nooutliers.sd.melt$todcat<38,'PM',alldata.nooutliers.sd.melt$tod)
alldata.nooutliers.sd.melt$tod<-ifelse(alldata.nooutliers.sd.melt$todcat>=38 & alldata.nooutliers.sd.melt$todcat<=48,'EV2',alldata.nooutliers.sd.melt$tod)

# arrange data
alldata.outliers.sd.melt<-merge(alldata.outliers.sd.melt,tmc_ifc_temp,by="tmc_code")
alldata.nooutliers.sd.melt<-merge(alldata.nooutliers.sd.melt,tmc_ifc_temp,by="tmc_code")

# segment by facility type (IFC)
alldata.outliers.sd.melt.byifc<-by(alldata.outliers.sd.melt,alldata.outliers.sd.melt$IFC, function(x) x)
alldata.nooutliers.sd.melt.byifc<-by(alldata.nooutliers.sd.melt,alldata.nooutliers.sd.melt$IFC, function(x) x)

# make plots
for (p in 1:length(alldata.outliers.sd.melt.byifc)) {
  
  plot1<-myplot_sd(alldata.outliers.sd.melt.byifc[[p]],"raw",outliers_byIFC[p],field)
  plot2<-myplot_sd(alldata.nooutliers.sd.melt.byifc[[p]],"nooutliers",outliers_byIFC[p],field)
  
  # save as JPEG
  print(multiplot(plot1,plot2),cols=1)
  dev.copy(jpeg,filename=paste(outputsDir,field, "_SD_","IFC_",p,".jpeg", sep = ""),width=1280, height=1280)
  dev.off()  
  
}

#debug
#myplot_sd(alldata.nooutliers.sd.melt.byifc[["9"]])

myplot_sd(alldata.outliers.sd.melt,"SD_per_mile_outliers.jpeg")
myplot_sd(alldata.nooutliers.sd.melt,"SD_per_mile_nooutliers.jpeg")

# ---------------- 5. PLTOS ----------------------
if(FALSE) {
  alldata.outliers.day<-alldata.outliers[date=="2012-10-01"]
  
  alldata.outliers.day[outlier==0, color.codes:="#000000"] #black
  alldata.outliers.day[outlier==1, color.codes:="#FF0000"] #red
  
  alldata.outliers.day[outlier==0, color.names:="valid"]
  alldata.outliers.day[outlier==1, color.names:="outlier"]
  
  # all freeways of a day data points
  alldata.facility.type<-by(alldata.outliers.day,alldata.outliers.day$IFC, function(x) x)
  
  lapply(alldata.facility.type, myplot1)
  
  alldata.nooutliers.day<-alldata.nooutliers[date=="2012-10-01"]
  
  alldata.nooutliers.day[outlier==0, color.codes:="#000000"] #black
  alldata.nooutliers.day[outlier==1, color.codes:="#FF0000"] #red
  
  alldata.nooutliers.day[outlier==0, color.names:="valid"]
  alldata.nooutliers.day[outlier==1, color.names:="outlier"]
  
  alldata.facility.type<-by(alldata.nooutliers.day,alldata.nooutliers.day$IFC, function(x) x)
  
  lapply(alldata.facility.type, myplot1)  
}


