library(stringr)
library(xtable)
library(foreign)
library(data.table)

setwd("C:/Projects/SANDAG_PricingAndReliability/data/toll facility data")

tripdata <- read.csv("RequestedNumbers.csv",header=TRUE)
nrow(tripdata)
tripdata<-data.table(tripdata)

#start time
tripdata[, starthour := as.numeric(substr(descr,1,2))]
tripdata[, startmin := as.numeric(substr(descr,4,5))]
tripdata[,starttime:=as.numeric(starthour*60+startmin)]

# end time
tripdata[, endhour := as.numeric(substr(descr,9,10))]
tripdata[, endmin := as.numeric(substr(descr,12,13))]
tripdata[,endtime:=as.numeric(endhour*60+endmin)]

tripdata[,midtime:=(starttime+endtime)/2]

# time periods (mins from midnight)
#Early AM (EA) 210 - 359
#AM Peak (AM)	 360 - 539
#Midday (MD)	 540 - 929
#PM Peak (PM)	 930 - 1139
#Evening (EV)	 1140 - 1440 and 0 - 209

tripdata$tod=5 # 5-evening
tripdata[midtime>=210 & midtime<=359,tod:=1]  #1-early AM
tripdata[midtime>=360 & midtime<=539,tod:=2]  #2-AM peak
tripdata[midtime>=540 & midtime<=929,tod:=3]  #3-Midday
tripdata[midtime>=930 & midtime<=1139,tod:=4] #4-PM Peak

tripdata$ea<-ifelse(tripdata$tod==1,1,0)
tripdata$am<-ifelse(tripdata$tod==2,1,0)
tripdata$md<-ifelse(tripdata$tod==3,1,0)
tripdata$pm<-ifelse(tripdata$tod==4,1,0)
tripdata$ev<-ifelse(tripdata$tod==5,1,0)

#ea trips
tripdata[,ea_FT2Axle:=FT2Axle*ea]
tripdata[,ea_CashCC2Axle:=CashCC2Axle*ea]
tripdata[,ea_FT3PlusAxle:=FT3PlusAxle*ea]
tripdata[,ea_CashCC3PlusAxle:=CashCC3PlusAxle*ea]

#am trips
tripdata[,am_FT2Axle:=FT2Axle*am]
tripdata[,am_CashCC2Axle:=CashCC2Axle*am]
tripdata[,am_FT3PlusAxle:=FT3PlusAxle*am]
tripdata[,am_CashCC3PlusAxle:=CashCC3PlusAxle*am]

#md trips
tripdata[,md_FT2Axle:=FT2Axle*md]
tripdata[,md_CashCC2Axle:=CashCC2Axle*md]
tripdata[,md_FT3PlusAxle:=FT3PlusAxle*md]
tripdata[,md_CashCC3PlusAxle:=CashCC3PlusAxle*md]

#pm trips
tripdata[,pm_FT2Axle:=FT2Axle*pm]
tripdata[,pm_CashCC2Axle:=CashCC2Axle*pm]
tripdata[,pm_FT3PlusAxle:=FT3PlusAxle*pm]
tripdata[,pm_CashCC3PlusAxle:=CashCC3PlusAxle*pm]

#ev trips
tripdata[,ev_FT2Axle:=FT2Axle*ev]
tripdata[,ev_CashCC2Axle:=CashCC2Axle*ev]
tripdata[,ev_FT3PlusAxle:=FT3PlusAxle*ev]
tripdata[,ev_CashCC3PlusAxle:=CashCC3PlusAxle*ev]

#ea
trips_FT2Axle<-aggregate(x=tripdata$ea_FT2Axle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$ea_CashCC2Axle,by=list(key=tripdata$key),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$ea_FT3PlusAxle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$ea_CashCC3PlusAxle,by=list(key=tripdata$key),FUN=sum)

trips_ea<-data.frame(key=trips_FT2Axle$key,FT2Axle=trips_FT2Axle$x)
trips_ea$CashCC2Axle<-trips_CashCC2Axle$x
trips_ea$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_ea$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

#am
trips_FT2Axle<-aggregate(x=tripdata$am_FT2Axle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$am_CashCC2Axle,by=list(key=tripdata$key),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$am_FT3PlusAxle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$am_CashCC3PlusAxle,by=list(key=tripdata$key),FUN=sum)

trips_am<-data.frame(key=trips_FT2Axle$key,FT2Axle=trips_FT2Axle$x)
trips_am$CashCC2Axle<-trips_CashCC2Axle$x
trips_am$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_am$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

#md
trips_FT2Axle<-aggregate(x=tripdata$md_FT2Axle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$md_CashCC2Axle,by=list(key=tripdata$key),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$md_FT3PlusAxle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$md_CashCC3PlusAxle,by=list(key=tripdata$key),FUN=sum)

trips_md<-data.frame(key=trips_FT2Axle$key,FT2Axle=trips_FT2Axle$x)
trips_md$CashCC2Axle<-trips_CashCC2Axle$x
trips_md$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_md$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

#pm
trips_FT2Axle<-aggregate(x=tripdata$pm_FT2Axle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$pm_CashCC2Axle,by=list(key=tripdata$key),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$pm_FT3PlusAxle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$pm_CashCC3PlusAxle,by=list(key=tripdata$key),FUN=sum)

trips_pm<-data.frame(key=trips_FT2Axle$key,FT2Axle=trips_FT2Axle$x)
trips_pm$CashCC2Axle<-trips_CashCC2Axle$x
trips_pm$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_pm$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

#ev
trips_FT2Axle<-aggregate(x=tripdata$ev_FT2Axle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$ev_CashCC2Axle,by=list(key=tripdata$key),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$ev_FT3PlusAxle,by=list(key=tripdata$key),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$ev_CashCC3PlusAxle,by=list(key=tripdata$key),FUN=sum)

trips_ev<-data.frame(key=trips_FT2Axle$key,FT2Axle=trips_FT2Axle$x)
trips_ev$CashCC2Axle<-trips_CashCC2Axle$x
trips_ev$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_ev$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

#all
trips_FT2Axle<-aggregate(x=tripdata$FT2Axle,by=list(tod=tripdata$tod),FUN=sum)
trips_CashCC2Axle<-aggregate(x=tripdata$CashCC2Axle,by=list(tod=tripdata$tod),FUN=sum)
trips_FT3PlusAxle<-aggregate(x=tripdata$FT3PlusAxle,by=list(tod=tripdata$tod),FUN=sum)
trips_CashCC3PlusAxle<-aggregate(x=tripdata$CashCC3PlusAxle,by=list(tod=tripdata$tod),FUN=sum)

trips_all<-data.frame(tod=trips_FT2Axle$tod,FT2Axle=trips_FT2Axle$x)
trips_all$CashCC2Axle<-trips_CashCC2Axle$x
trips_all$FT3PlusAxle<-trips_FT3PlusAxle$x
trips_all$CashCC3PlusAxle<-trips_CashCC3PlusAxle$x

write.table(trips_all,"sr125_summary.csv",row.names=F,quote=F,sep = ",")

rm(tripdata)