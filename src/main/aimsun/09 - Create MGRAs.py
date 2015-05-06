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
	map = dict() # {mgraId:[nodeExternalId]}
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		if fields[0] not in map:
			map[fields[0]] = list()
		map[fields[0]].append(fields[1])
	file.close()
	return map

def mapNodes():
	nodes = dict()
	nodeType = model.getType("GKNode")
	for node in model.getCatalog().getObjectsByType(nodeType).itervalues():
		id = str(node.getExternalId())
		if id != "":
			nodes[id] = node
	return nodes

def mapSections():
	entranceSections = dict()
	exitSections = dict()
	sectionType = model.getType("GKSection")
	aNodeAtt = model.getColumn("GKSection::AN")
	bNodeAtt = model.getColumn("GKSection::BN")
	for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
		origin = section.getOrigin()
		destination = section.getDestination()
		if destination != None and origin == None and str(destination.getExternalId()) != "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				id = str(section.getDataValueInt(bNodeAtt))
			elif section.getDataValueInt(bNodeAtt) == int(str(destination.getExternalId())):
				id = str(section.getDataValueInt(aNodeAtt))
			entranceSections[id] = section
		if origin != None and destination == None and str(origin.getExternalId()) != "":
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				id = str(section.getDataValueInt(bNodeAtt))
			elif section.getDataValueInt(bNodeAtt) == int(str(origin.getExternalId())):
				id = str(section.getDataValueInt(aNodeAtt))
			exitSections[id] = section
	return (entranceSections, exitSections)

def createCentroidConfiguration(name):
	newCentroidConfiguration = GKSystem.getSystem().newObject("GKCentroidConfiguration", model)
	newCentroidConfiguration.setName(name)
	folder = model.getCreateRootFolder().findFolder("GKModel::centroidsConf")
	if folder == None:
		folder = GKSystem.getSystem().createFolder( model.getCreateRootFolder(), "GKModel::centroidsConf")
	folder.append(newCentroidConfiguration)
	return newCentroidConfiguration

def createCentroid(externalId, position, centroidConfiguration):
	newCentroid = GKSystem.getSystem().newObject("GKCentroid", model)
	newCentroid.setExternalId(externalId)
	newCentroid.setFromPosition(position)
	layer = model.getGeoModel().getActiveLayer()
	model.getGeoModel().add(layer, newCentroid)
	centroidConfiguration.addCentroid(newCentroid)
	return newCentroid

def createConnection(centroid, object, direction):
	connection = GKSystem.getSystem().newObject("GKCenConnection", model)
	connection.setConnectionObject(object)
	connection.setConnectionType(direction)
	centroid.addConnection(connection)

print "Creating MGRAs..."
fileName = getPropertyValue("aimsun.gis.mgraTagFile")
centroidConfiguration = createCentroidConfiguration("MGRAs")
mgras = readFile(fileName)
nodeMap = mapNodes()
(entranceSectionMap, exitSectionMap) = mapSections()
task = GKSystem.getSystem().createTask(model)
task.setName("Centroid creation")
task.setTotalSteps(len(mgras))
step = 0
cancelled = False
task.start()
for (mgraId, nodeEids) in mgras.iteritems():
	points = GKPoints()
	nodes = list()
	entranceSections = list()
	exitSections = list()
	for nodeEid in nodeEids:
		found = False
		if nodeEid in nodeMap:
			node = nodeMap[nodeEid]
			nodes.append(node)
			points.append(node.getPosition())
			found = True
		if nodeEid in entranceSectionMap:
			section = entranceSectionMap[nodeEid]
			entranceSections.append(section)
			points.append(section.getPoint(0))
			found = True
		if nodeEid in exitSectionMap:
			section = exitSectionMap[nodeEid]
			exitSections.append(section)
			points.append(section.getPoint(len(section.getPoints())-1))
			found = True
		if not found:
			model.getLog().addError("Node %s not found!" % nodeEid)
	if len(points) > 0:
		position = points.center()
		centroid = createCentroid(mgraId, position, centroidConfiguration)
		for node in nodes:
			createConnection(centroid, node, GK.eFrom)
			createConnection(centroid, node, GK.eTo)
		for section in entranceSections:
			createConnection(centroid, section, GK.eTo)
		for section in exitSections:
			createConnection(centroid, section, GK.eFrom)
	step += 1
	cancelled = task.stepTask(step)
task.end()
GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
model.getCommander().addCommand( None )
print "MGRAs created!"
