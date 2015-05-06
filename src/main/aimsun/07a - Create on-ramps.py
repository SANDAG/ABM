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

def getNbAuxLanes(section):
	aux = 0
	aNodeAtt = model.getColumn("GKSection::AN")
	abAuxAtt = model.getColumn("GKSection::ABAU")
	baAuxAtt = model.getColumn("GKSection::BAAU")
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			aux = section.getDataValueInt(abAuxAtt)
		else:
			aux = section.getDataValueInt(baAuxAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				aux = section.getDataValueInt(baAuxAtt)
			else:
				aux = section.getDataValueInt(abAuxAtt)
	return aux

def getSections(node):
	inLanes = 0
	outLanes = 0
	rampSection = None
	mainDownSection = None
	mainUpSection = None
	for section in node.getEntranceSections():
		inLanes += section.getNbLanesAtPos(section.length3D())
		if str(section.getRoadType().getName()) == "On/Off Ramp" and section.length3D() > 50: # exclude HOV ramps
			if rampSection == None:
				rampSection = section
			else:
				if mainUpSection == None:
					mainUpSection = section
		elif str(section.getRoadType().getName()) == "Freeway":
			mainUpSection = section
	for section in node.getExitSections():
		outLanes += section.getNbLanesAtPos(0)
		if str(section.getRoadType().getName()) == "Freeway":
			mainDownSection = section
	return (rampSection, mainDownSection, mainUpSection, inLanes, outLanes)

def cutIfNeeded(section, minimumRampLength, minimumResidualSectionLength, right):
	global model
	canCreateLane = True
	if right:
		extremeLane = section.getLanes()[len(section.getLanes()) - 1]
	else:
		extremeLane = section.getLanes()[0]
	if not extremeLane.isFullLane(): # downstream section has an exit lateral -> cut
		cutPos = min(section.length3D() / 2, section.length3D() + extremeLane.getInitialOffset() - minimumResidualSectionLength) # offset of exit lateral is negative from end
		if cutPos >= minimumRampLength + minimumResidualSectionLength: # check if the section can be cut
			cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
			#cmd.commandToBeDone()
			#cmd.doit()
			#cmd.commandDone()
			#cmd.setDone(True)
			model.getCommander().addCommand(cmd)
		else:
			canCreateLane = False
			model.getLog().addWarning("No space to add on-ramp to section %s!" % section.getExternalId())
	return canCreateLane

def createAccelerationLane(section, side, defaultRampLength, minimumResidualSectionLength):
	rampLength = min(defaultRampLength, section.length3D() - minimumResidualSectionLength)
	if rampLength < defaultRampLength:
		model.getLog().addWarning("Acceleration lane at section %s shorter than the default length!" % section.getExternalId())
	#lane = GKSectionLane()
	#lane.setOffsets(0.0, rampLength)
	#section.addLane(lane)
	cmd = GKSectionLateralNewCmd()
	cmd.setData(section, side, rampLength)
	#cmd.commandToBeDone()
	#cmd.doit()
	#cmd.commandDone()
	#cmd.setDone(True)
	model.getCommander().addCommand(cmd)

def offsetRampEndpoint(rampSection, mainSection, right):
	rampPoint = rampSection.getPoint(len(rampSection.getPoints())-1)
	mainPoint = mainSection.getPoint(len(mainSection.getPoints())-1)
	newPos = GKPoint()
	mainPoint.pointAtDistance(mainSection.getExtremePointAtPos(mainSection.length3D(), right), (mainSection.getLaneWidth() * mainSection.getNbLanesAtPos(mainSection.length3D()) + rampSection.getLaneWidth() * rampSection.getNbLanesAtPos(rampSection.length3D())) / 2.0, newPos)
	rampPoint.x = newPos.x
	rampPoint.y = newPos.y
	rampPoint.z = newPos.z
	rampSection.increaseTick()

def arrangeTurnLanes(section):
	minLane = section.getEntryLanes()[0]
	currLane = section.getEntryLanes()[1]
	for turn in section.getOrigin().getToTurningsOrderedFromRightToLeft(section):
		turnLanes = turn.getOriginToLane() - turn.getOriginFromLane() + 1
		turn.setDestinationLanes(max(minLane, currLane - turnLanes + 1), currLane)
		turn.updatePath(True)
		turn.curve()
		currLane = max(minLane, currLane - turnLanes)

print "Creating on-ramps..."
ftToM = float(getPropertyValue("aimsun.gis.ftToM"))
defaultRampLength = float(getPropertyValue("aimsun.gis.defaultRampLength")) * ftToM # m
minimumRampLength = 15 # m
minimumResidualSectionLength = 15 # m
type = model.getType("GKNode")
for node in model.getCatalog().getObjectsByType(type).itervalues():
	(rampSection, mainDownSection, mainUpSection, inLanes, outLanes) = getSections(node)
	if rampSection != None and mainDownSection != None and mainUpSection != None:
		refPoint = rampSection.getPoint(len(rampSection.getPoints()) - 2)
		if mainUpSection.getPoints().isPointAtRightArea(refPoint): # ramp enters from right
			if inLanes > outLanes and mainDownSection.getLane(mainDownSection.getEntryLanes()[1]).isFullLane(): # add an acceleration lane if needed and if one is not already present
				canCreateLane = cutIfNeeded(mainDownSection, minimumRampLength, minimumResidualSectionLength, True)
				if canCreateLane:
					createAccelerationLane(mainDownSection, GKSection.eRightIn, defaultRampLength, minimumResidualSectionLength)
					outLanes += 1
			offsetRampEndpoint(rampSection, mainUpSection, True)
		else: # ramp enters from left
			if inLanes > outLanes and mainDownSection.getLane(mainDownSection.getEntryLanes()[0]).isFullLane(): # add an acceleration lane
				canCreateLane = cutIfNeeded(mainDownSection, minimumRampLength, minimumResidualSectionLength, False)
				if canCreateLane:
					createAccelerationLane(mainDownSection, GKSection.eLeftIn, defaultRampLength, minimumResidualSectionLength)
					outLanes += 1
			offsetRampEndpoint(rampSection, mainUpSection, False)
		arrangeTurnLanes(mainDownSection)

GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "On-ramps created!"