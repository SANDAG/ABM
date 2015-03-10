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

def readFile(fileName):
	items = list() # [(hwycovId, lane, start, duration)]
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		lane = int(fields[1])
		start = QTime(int(fields[2][0:2]),int(fields[2][2:4]),0)
		duration = GKTimeDuration(0,0,0).addSecs(3600 * float(fields[3]))
		items.append((fields[0].strip(), lane, start, duration))
	file.close()
	return items

def createCondition(start, duration):
	newCondition = GKSystem.getSystem().newObject("GKTrafficCondition", model)
	newCondition.setName("Lane closures from %s during %s" % (start.toString(), duration.toString()))
	newCondition.setActivationType(GKSimulationEvent.eTime)
	newCondition.setFromTime(start)
	newCondition.setDurationTime(duration)
	folder = model.getCreateRootFolder().findFolder("GKModel::trafficConditions")
	if folder == None:
		folder = GKSystem.getSystem().createFolder(model.getCreateRootFolder(), "GKModel::trafficConditions")
	folder.append(newCondition)
	return newCondition

def createClosure(section, lane):
	closure = GKSystem.getSystem().newObject("GKLaneClosingChange", model)
	closure.setSection(section)
	closure.setFromLane(lane)
	closure.setToLane(lane)
	return closure

fileName = getPropertyValue("aimsun.gis.todFile")
closures = readFile(fileName)
conditions = dict()
for (sectId, lane, start, duration) in closures:
	section = model.getCatalog().findObjectByExternalId(sectId, model.getType("GKSection"))
	if section != None:
		key = "%s %s" % (start.toString(), duration.toString())
		if key in conditions:
			condition = conditions[key]
		else:
			condition = createCondition(start, duration)
			conditions[key] = condition
		closure = createClosure(section, lane)
		condition.addChange(closure)
	else:
		model.getLog().addError("Section %s not found!" % sectId)
print "Done!"
