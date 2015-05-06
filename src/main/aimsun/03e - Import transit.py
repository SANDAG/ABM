busType = model.getCatalog().find(58)

class Stop:
	def __init__(self, id, route, link, node, name):
		self.id = id
		self.route = route
		self.link = link
		self.node = node
		self.name = name

class Link:
	def __init__(self, route, link, direction):
		self.route = route
		self.link = link
		self.direction = direction

class Route:
	def __init__(self, id, name, mode, amHeadway, pmHeadway, opHeadway):
		self.id = id
		self.name = name
		self.mode = mode
		self.amHeadway = amHeadway
		self.pmHeadway = pmHeadway
		self.opHeadway = opHeadway

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

def readStopFile(fileName):
	map = dict() # {routeId:[stops]}
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		if fields[1].strip() not in map:
			map[fields[1].strip()] = list()
		map[fields[1].strip()].append(Stop(fields[0].strip(), fields[1].strip(), fields[2].strip(), fields[7].strip(), fields[9].strip()))
	file.close()
	return map

def readLinkFile(fileName):
	map = dict() # {routeId:[links]}
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		if fields[0].strip() not in map:
			map[fields[0].strip()] = list()
		map[fields[0].strip()].append(Link(fields[0].strip(), fields[1].strip(), fields[2].strip()))
	file.close()
	return map

def readRouteFile(fileName):
	map = dict() # {routeId:route}
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		map[fields[0].strip()] = Route(fields[0].strip(), fields[1].strip(), fields[2].strip(), int(float(fields[3].strip())), int(float(fields[4].strip())), int(float(fields[5].strip())))
	file.close()
	return map

def mapLinksDirection():
	map = dict() # {(extId, direction):section}
	aNodeAtt = model.getColumn("GKSection::AN")
	type = model.getType("GKSection")
	for section in model.getCatalog().getObjectsByType(type).itervalues():
		origin = section.getOrigin()
		direction = ""
		if origin != None and str(origin.getExternalId()) != "":
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				direction = "+"
			else:
				direction = "-"
		else:
			destination = section.getDestination()
			if destination != None and str(destination.getExternalId()) != "":
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					direction = "-"
				else:
					direction = "+"
		if direction != "":
			map[(str(section.getExternalId()), direction)] = section
	return map

def mapLinksEndnode():
	map = dict() # {(extId, toTrcovId):section}
	aNodeAtt = model.getColumn("GKSection::AN")
	aNodeTrcovAtt = model.getColumn("GKSection::AN_TRCOV_ID")
	bNodeTrcovAtt = model.getColumn("GKSection::BN_TRCOV_ID")
	type = model.getType("GKSection")
	for section in model.getCatalog().getObjectsByType(type).itervalues():
		origin = section.getOrigin()
		trcovid = -1
		if origin != None and str(origin.getExternalId()) != "":
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				trcovid = section.getDataValueInt(aNodeTrcovAtt)
			else:
				trcovid = section.getDataValueInt(bNodeTrcovAtt)
		else:
			destination = section.getDestination()
			if destination != None and str(destination.getExternalId()) != "":
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					trcovid = section.getDataValueInt(bNodeTrcovAtt)
				else:
					trcovid = section.getDataValueInt(aNodeTrcovAtt)
		if trcovid != -1:
			map[(str(section.getExternalId()), str(trcovid))] = section
	return map

def createLine(eid, name):
	line = GKSystem.getSystem().newObject("GKPublicLine", model)
	line.setExternalId(eid)
	line.setName(name)
	folderName = "GKModel::publicLines"
	folder = model.getCreateRootFolder().findFolder(folderName)
	if folder == None:
		folder = GKSystem.getSystem().createFolder(model.getCreateRootFolder(), folderName)
	folder.append(line)
	return line

def createStop(eid, name, section):
	stop = GKSystem.getSystem().newObject("GKBusStop", model)
	stop.setExternalId(eid)
	stop.setName(name)
	stop.setLanes(section.getLanesAtPos(section.length3D())[1], section.getLanesAtPos(section.length3D())[1])
	stop.setLength(20.0)
	stop.setPosition(section.length3D() - 30.0)
	section.addTopObject(stop)
	section.increaseTick()
	model.getGeoModel().add(section.getLayer(), stop)
	return stop

def createTurn(node, fromSection, toSection):
	if node == None:
		node = GKSystem.getSystem().newObject("GKNode", model)
		model.getGeoModel().add(fromSection.getLayer(), node)
		aNodeAtt = model.getColumn("GKSection::AN")
		bNodeAtt = model.getColumn("GKSection::BN")
		origin = fromSection.getOrigin()
		if origin != None and str(origin.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				eid = section.getDataValueInt(bNodeAtt)
			else:
				eid = section.getDataValueInt(aNodeAtt)
		else:
			destination = toSection.getDestination()
			if destination != None and str(destination.getExternalId()) > "":
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					eid = toSection.getDataValueInt(bNodeAtt)
				else:
					eid = toSection.getDataValueInt(aNodeAtt)
		node.setExternalId(str(eid))
	turn = GKSystem.getSystem().newObject("GKTurning", model)
	turn.setConnection(fromSection, toSection)
	turn.setOriginLanes(fromSection.getExitLanes()[0], fromSection.getExitLanes()[0])
	turn.setDestinationLanes(toSection.getEntryLanes()[0], toSection.getEntryLanes()[0])
	turn.updatePath(True)
	turn.curve()
	node.addTurning(turn, True)

print "Importing transit..."

stopFileName = getPropertyValue("aimsun.gis.trstopFile")
linkFileName = getPropertyValue("aimsun.gis.trlinkFile")
routeFileName = getPropertyValue("aimsun.gis.trrtFile")
stops = readStopFile(stopFileName)
links = readLinkFile(linkFileName)
routes = readRouteFile(routeFileName)
linkMapDirection = mapLinksDirection()
linkMapEndnode = mapLinksEndnode()
ifcAtt = model.getColumn("GKSection::IFC")
for route in routes.itervalues():
	# 4 = Commuter Rail Line
	# 5 = Light Rail Transit (LRT) or Streetcar Line
	# 6 = Bus Rapid Transit (BRT)
	# 7 = Rapid Bus
	# 8 = Limited Express Bus
	# 9 = Express Bus
	# 10 = Local Bus
	if route.mode == "6" or route.mode == "7" or route.mode == "8" or route.mode == "9" or route.mode == "10":
		line = createLine(route.id, route.name)
		sections = list()
		for link in links[route.id]:
			if (link.link, link.direction) in linkMapDirection:
				section = linkMapDirection[(link.link, link.direction)]
				if len(sections) > 0: # Check continuity
					previousSection = sections[len(sections)-1]
					node = section.getOrigin()
					if node != None:
						if not node.existTurning(previousSection, section):
							if previousSection.getDestination() == node and (section.getDataValueInt(ifcAtt) == 12 or previousSection.getDataValueInt(ifcAtt) == 12):
								createTurn(node, previousSection, section)
								model.getLog().addInfo("U-turn created between sections %s and %s when processing route %s (%s)!" % (previousSection.getExternalId(), section.getExternalId(), route.id, route.name))
							else:
								model.getLog().addError("Route %s (%s) broken between sections %s and %s!" % (route.id, route.name, previousSection.getExternalId(), section.getExternalId()))
					else:
						if previousSection.getExternalId() == section.getExternalId() and (section.getDataValueInt(ifcAtt) == 12 or previousSection.getDataValueInt(ifcAtt) == 12):
							createTurn(None, previousSection, section)
							model.getLog().addInfo("U-turn created between sections %s and %s when processing route %s (%s)!" % (previousSection.getExternalId(), section.getExternalId(), route.id, route.name))
						else:
							model.getLog().addError("Unconnected section %s when processing route %s (%s)!" % (section.getExternalId(), route.id, route.name))
				sections.append(section)
			else:
				model.getLog().addError("Link %s direction %s not found when processing route %s (%s)!" % (link.link, link.direction, route.id, route.name))
		newStops = list()
		for stop in stops[route.id]:
			if (stop.link, stop.node) in linkMapEndnode:
				section = linkMapEndnode[(stop.link, stop.node)]
				newStop = None
				objs = section.getTopObjects()
				if objs != None:
					for obj in objs:
						if obj.isA("GKBusStop"):
							newStop = obj
				if newStop == None:
					newStop = createStop(stop.id, stop.name, section)
				newStops.append(newStop)
		line.createPTSections(sections, newStops)
		timeTable = GKSystem.getSystem().newObject("GKPublicLineTimeTable", model)
		timeTable.setName("Day")
		schedule = timeTable.createNewSchedule()
		schedule.setTime(QTime(6,0,0))
		schedule.setDuration(GKTimeDuration(3,0,0))
		departure = GKPublicLineTimeTableScheduleDeparture()
		schedule.setDepartureType(GKPublicLineTimeTableSchedule.eInterval)
		headway = route.amHeadway # min
		departure.setMeanTime(GKTimeDuration(headway / 60, headway % 60, 0))
		departure.setVehicle(busType)
		schedule.addDepartureTime(departure)
		timeTable.addSchedule(schedule)
		schedule = timeTable.createNewSchedule()
		schedule.setTime(QTime(9,0,0))
		schedule.setDuration(GKTimeDuration(6,0,0))
		departure = GKPublicLineTimeTableScheduleDeparture()
		schedule.setDepartureType(GKPublicLineTimeTableSchedule.eInterval)
		headway = route.pmHeadway # min
		departure.setMeanTime(GKTimeDuration(headway / 60, headway % 60, 0))
		departure.setVehicle(busType)
		schedule.addDepartureTime(departure)
		timeTable.addSchedule(schedule)
		schedule = timeTable.createNewSchedule()
		schedule.setTime(QTime(15,0,0))
		schedule.setDuration(GKTimeDuration(3,0,0))
		departure = GKPublicLineTimeTableScheduleDeparture()
		schedule.setDepartureType(GKPublicLineTimeTableSchedule.eInterval)
		headway = route.opHeadway # min
		departure.setMeanTime(GKTimeDuration(headway / 60, headway % 60, 0))
		departure.setVehicle(busType)
		schedule.addDepartureTime(departure)
		timeTable.addSchedule(schedule)
		line.addTimeTable(timeTable)

print "Transit imported!"