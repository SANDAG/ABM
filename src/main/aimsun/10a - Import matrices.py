driveAloneToll = model.getCatalog().find(136455)
driveAloneNoToll = model.getCatalog().find(136457)
hov2Toll = model.getCatalog().find(136459)
hov2NoToll = model.getCatalog().find(136460)
hov3Toll = model.getCatalog().find(136463)
hov3NoToll = model.getCatalog().find(136464)
lightToll = model.getCatalog().find(136466)
lightNoToll = model.getCatalog().find(136467)
mediumToll = model.getCatalog().find(136468)
mediumNoToll = model.getCatalog().find(136469)
heavyToll = model.getCatalog().find(136470)
heavyNoToll = model.getCatalog().find(136471)

def getPropertyValue(propertyKey):
	value = ""
	preferences = model.getPreferences()
	projectFolder = str(GKSystem.getSystem().convertVariablePath(preferences.getValue("CT-RAMP:ProjectFolder"), model))
	filePath = "%s/conf/sandag_abm.properties" % projectFolder
	file = open(filePath, "r")
	for line in file.readlines():
		if not line[0] == "#":
			tokens = line.rstrip("\n").split("=")
			if tokens[0].strip() == propertyKey:
				value = tokens[1].strip().replace("%project.folder%", projectFolder)
	file.close()
	return value

def mapCentroids(centConf):
	map = dict()
	for centroid in centConf.getCentroids():
		map[str(centroid.getExternalId())] = centroid
	return map

print "Importing matrices..."
if target != None:
	if "MGRA" in str(target.getName()).upper():
		originPos = 6
		destinationPos = 7
	else:
		originPos = 4
		destinationPos = 5
	matrices = dict()
	centroids = mapCentroids(target)
	# folder = str(GKSystem.getSystem().convertVariablePath(preferences.getValue("CT-RAMP:ProjectFolder"), model)) + "/matrices"
	fileName = getPropertyValue("aimsun.demand.tripFile")
	file = open(fileName, "r")
	file.readline() # skip header
	lines = file.readlines()
	task = GKSystem.getSystem().createTask(model)
	task.setName("Import demand")
	task.setTotalSteps(len(lines))
	step = 0
	cancelled = False
	task.start()
	for line in lines:
		fields = line.rstrip("\n").split(",")
		if not cancelled:
			vehicle = None
			if fields[10] == "passengerCar":
				if fields[11] == "1":
					if fields[12] == "1":
						vehicle = driveAloneToll
					else:
						vehicle = driveAloneNoToll
				elif fields[11] == "2":
					if fields[12] == "1":
						vehicle = hov2Toll
					else:
						vehicle = hov2NoToll
				else:
					if fields[12] == "1":
						vehicle = hov3Toll
					else:
						vehicle = hov3NoToll
			if fields[10] == "heavy-light":
				if fields[12] == "1":
					vehicle = lightToll
				else:
					vehicle = lightNoToll
			if fields[10] == "heavy-medium":
				if fields[12] == "1":
					vehicle = mediumToll
				else:
					vehicle = mediumNoToll
			if fields[10] == "heavy-heavy":
				if fields[12] == "1":
					vehicle = heavyToll
				else:
					vehicle = heavyNoToll
			if vehicle != None:
				if vehicle not in matrices:
					matrices[vehicle] = dict()
				if fields[originPos] in centroids:
					origin = centroids[fields[originPos]]
					if fields[destinationPos] in centroids:
						destination = centroids[fields[destinationPos]]
						period = int(fields[16])
						if period not in matrices[vehicle]:
							matrix = GKSystem.getSystem().newObject( "GKODMatrix", model )
							min = 180 + 5 * (period -1)
							matrix.setFrom(QTime((min / 60) % 24, min % 60,0))
							matrix.setDuration(GKTimeDuration(0,5,0))
							matrix.setVehicle(vehicle)
							matrix.setName("%s %s %s %s" %(fields[10], fields[11], fields[12], period))
							target.addODMatrix(matrix)

							#matrix.setStoreType(GKODMatrix.eAsciiFile)
							#matrix.setExtraLocationData( ",@AimsunBinary" )
							#matrixPath = folder + "/" + str(target.getName()) + "/" + str(matrix.getName()) + ".bin"
							#matrix.setLocation(matrixPath)
							#matrix.setEnableStore(True)
							#matrix.storeExternalMatrix()

							matrices[vehicle][period] = matrix
						else:
							matrix = matrices[vehicle][period]
						matrix.setTrips(origin, destination, matrix.getTrips(origin, destination) + 1)
					else:
						model.getLog().addError("Centroid %s not found!" % fields[destinationPos])
				else:
					model.getLog().addError("Centroid %s not found!" % fields[originPos])
			step += 1
			cancelled = task.stepTask(step)
	task.end()
	file.close()
	print "Matrices imported!"
else:
	model.reportError("Import matrices", "Execute the script from the context menu of a centroid configuration")