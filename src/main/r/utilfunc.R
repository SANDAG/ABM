
#read a file, save to Rdata, and remove from workspace
readSaveRdata <- function(filename,objname) {
  assign(objname,fread(filename))
  save(list=objname,file=paste0(filename,".Rdata"))
  rm(list=objname)
}

#load an RData object to an object name
assignLoad <- function(filename) {
  load(filename)
  get(ls()[ls() != "filename"])
}

regressionmodel<-function(myDF) {

model = lm(SpeedStdDev~NumLanes+VOC+CongSpeed+Ratio.FFTime.CongTime+IFC, data=myDF)
summary(model)

}

detectoutliers<-function(myDF) {
  # Detects outliers in a dataset
  #
  # Args:
  #   myDF: dataframe in list of dataframes
  #   type:   box plot to use - original or adjusted 
  #
  # Returns:
  #   mydata with outlier=1 for outliers
  type<-"adjusted"

  if (type=="original") {
    values<-boxplot(travel_time_sec ~ todcat, myDF, main = "Original Boxplot")
  } else {
    values<-adjbox(travel_time_sec ~ todcat, myDF, main = "Adjusted Boxplot")
  }
  
  if (length(table(myDF$todcat))<48) {
    warning(paste("tmc segment ", myDF$tmc_code[1], " is missing some data"))
  }
  
  for (cat in 1:length(unique(myDF$todcat))) {
    limit_lower<-values[["stats"]][1,cat]
    limit_upper<-values[["stats"]][5,cat]
    myDF[todcat==cat & (travel_time_sec<limit_lower|travel_time_sec>limit_upper), outlier:=1]
  }
  
}

myplot_sd<-function(myDF,datatype,numoutliers,field) {
  # Plots data points by facility type
  #
  # Args:
  #   myDF: dataframe in list of dataframes
  #   outputsDir: directory to save the plot 
  #
  # Returns:
  #   Nothing. saves a plot in JPEG format in OutputsDir
	
	# to change the order of variables in plot
	outputsDir="./INRIX/2012_10/"
	
	# include only segments that have data for the entire period
  if (length(table(myDF$todcat))==48) {

		myDF$tod<-factor(myDF$tod, levels = c("EV1","EA", "AM", "MD", "PM", "EV2"))
		IFC<-myDF$IFC[1]	
		
		#p<-ggplot(data=myDF,aes(todcat,value, colour=tmc_code),na.rm=TRUE) + geom_point()
		#p<-ggplot(data=myDF,aes(todcat,value),na.rm=TRUE) + geom_point()
	
    p<-ggplot(data=myDF,aes(todcat,value, group=tmc_code),na.rm=TRUE) + geom_line()+ geom_point()

		# add grids for TOD
		p<-p + facet_grid(. ~ tod, scales="free_x", space="free")

		# set space between facets 
		p<-p + theme_bw() + theme(panel.margin.x=unit(0,"lines"),panel.margin.y=unit(0.25,"lines"),
								plot.title = element_text(lineheight=.8, face="bold", vjust=2))      

		# set consistent axis and force axis to start at 0
		p<-p+expand_limits(y = 0)
		p<-p+scale_x_continuous(breaks = seq(0,48, by = 1), expand=c(0,0))+scale_y_continuous(expand = c(0, 0))
		
		# add title
		if (datatype=="raw") {
			outfile = paste(field, "_SD_rawdata_",IFC,".jpeg")
			maintitle<-paste(field," SD by TOD (raw data)")
		} else {
			outfile = paste("SD_per_mile_nooutliers_",IFC,".jpeg")
			maintitle<-paste(field, " SD by TOD (", numoutliers, " outliers removed)")
		}
		
		p<-p + labs(x="Time of Day Category (30 mins interval)",y=paste(field, " SD"), title=maintitle)
		p<-p+theme(plot.title = element_text(lineheight=.8, face="bold", vjust=2))

		#print(p)
		
		return(p)

		# save as JPEG
		#dev.copy(jpeg,filename=paste(outputsDir,outfile, sep = ""),width=1280, height=1280)
		#dev.off()	
		
  }	

}

myplot1<-function(myDF) {
  # Plots data points by facility type
  #
  # Args:
  #   myDF: dataframe in list of dataframes
  #   outputsDir: directory to save the plot 
  #
  # Returns:
  #   Nothing. saves a plot in JPEG format in OutputsDir
	
	# to change the order of variables in plot
	outputsDir="./INRIX/2012_10/"
	myDF$tod<-factor(myDF$tod, levels = c("EV1","EA", "AM", "MD", "PM", "EV2"))
	IFC<-myDF$IFC[1]

	p<-ggplot(data=myDF,aes(totalhour,speed, colour=color.names)) + geom_point()
	p<-p + scale_colour_manual(breaks=myDF$color.names, values = unique(as.character(myDF$color.codes)))

	# add grids for TOD
	p<-p + facet_grid(. ~ tod, scales="free_x", space="free")

	# set space between facets 
	p<-p + theme_bw() + theme(panel.margin.x=unit(0,"lines"),panel.margin.y=unit(0.25,"lines"),
							plot.title = element_text(lineheight=.8, face="bold", vjust=2))      

	# set consistent axis and force axis to start at 0
	p<-p+expand_limits(y = 0)
	p<-p+scale_x_continuous(breaks = seq(0,24, by = 2), expand=c(0,0))+scale_y_continuous(expand = c(0, 0))
	
	numoutliers<-sum(myDF$outlier)
	# add title
	maintitle<-paste("Speed by TOD (outliers=",numoutliers,")")
	p<-p + labs(x="Time of Day (hour)",y="Speed (mph)", title=maintitle)
	p<-p+theme(plot.title = element_text(lineheight=.8, face="bold", vjust=2))

	print(p)
	# save as JPEG
	dev.copy(jpeg,filename=paste(outputsDir,"Speed_IFC_", IFC, "_outliers30mins.jpeg", sep = ""),width=1280, height=1280)
	dev.off()	

}

myplot<- function(indata,datatype,analysistype,xlbl,ylbl,plottitle,ymax) {
  # Makes a scatter plot
  #
  # Args:
  #  indata: dataset with datapoints
  #  datatype: all weekdays or one
  #  analysistype: travel time or speed analysis
  #  xlbl: x-axis label
  #  ylbl: y-axis label
  #  plottitle: plot title
  #  ymax: y-axis limit
  #
  # Returns:
  #  a scatter plot of datapoints in indata
  
  # setup a plot
  if (analysistype == "Time") {
    p<-ggplot(data=indata,aes(x=totalhour,y=travel_time_sec)) + geom_point(colour='black',size=2)
    #ymax<-max(indata$travel_time_sec)+100 # add 100sec to have some space on top
	ybreak <-200 # 200 seconds
  } else {
    p<-ggplot(data=indata,aes(x=totalhour,y=speed)) + geom_point(colour='black',size=2)
    #ymax<-max(indata$speed)+10 # add 10 mph to have some space on top
	ybreak <-20 # 20 mph
  }
  
  # set different colors for outliers
  p<-p + scale_colour_hue(breaks=indata$outlier)
  
  # add grids for TOD
  if (datatype=="All") {
    p<-p + facet_grid(wday ~ tod, scales="free_x", space="free")
  } else {
    p<-p + facet_grid(day ~ tod, scales="free_x", space="free")
  }
  
  # set space between facets 
  p<-p + theme_bw() + theme(panel.margin.x=unit(0,"lines"),panel.margin.y=unit(0.25,"lines"),
                                  plot.title = element_text(lineheight=.8, face="bold", vjust=2))      
  
  # set consistent axis and force axis to start at 0
  p<-p+expand_limits(y = 0)
  
  # modify x and y axis
  if (analysistype == "Time") {
    p<-p+scale_x_continuous(breaks = seq(0,24, by = 2), expand=c(0,0))+scale_y_continuous(expand = c(0, 0)) #breaks=seq(0,1000, by = ybreak), 
  } else {
    p<-p+scale_x_continuous(breaks = seq(0,24, by = 2), expand=c(0,0))+scale_y_continuous(breaks=seq(0,100, by = 20), expand = c(0, 0)) #breaks=seq(0,100, by = ybreak),
  }
  
  # add title
  p<-p + labs(x=xlbl,y=ylbl, title=plottitle)
  p<-p+theme(plot.title = element_text(lineheight=.8, face="bold", vjust=2))
  
  return(p)
}

# function to add multiple plots in a print area  
multiplot <- function(..., plotlist=NULL, file, cols=1, layout=NULL) {
  # Combines multiple plots in one print area
  #
  # Args:
  #  ...: plots seperated by commas. Provide as many plots as you want.
  #  plotlist: 
  #  file: 
  #  cols: number of columns in the plot area
  #  layout: if NULL then 'cols' is used to determine layout
  #
  # Returns:
  #  None. Displays the multiplot

  library(grid)
  
  # Make a list from the ... arguments and plotlist
  plots <- c(list(...), plotlist)
  
  numPlots = length(plots)
  
  # If layout is NULL, then use 'cols' to determine layout
  if (is.null(layout)) {
    # Make the panel
    # ncol: Number of columns of plots
    # nrow: Number of rows needed, calculated from # of cols
    layout <- matrix(seq(1, cols * ceiling(numPlots/cols)),
                     ncol = cols, nrow = ceiling(numPlots/cols))
  }
  
  if (numPlots==1) {
    print(plots[[1]])
    
  } else {
    # Set up the page
    grid.newpage()
    pushViewport(viewport(layout = grid.layout(nrow(layout), ncol(layout))))
    
    # Make each plot, in the correct location
    for (i in 1:numPlots) {
      # Get the i,j matrix positions of the regions that contain this subplot
      matchidx <- as.data.frame(which(layout == i, arr.ind = TRUE))
      
      print(plots[[i]], vp = viewport(layout.pos.row = matchidx$row,
                                      layout.pos.col = matchidx$col))
    }
  }
} 