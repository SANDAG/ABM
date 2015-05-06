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
	if node.getNumExitSections() == 2 and node.getNumEntranceSections() == 1:
		for section in node.getExitSections():
			outLanes += section.getNbLanesAtPos(0)
			if str(section.getRoadType().getName()) == "On/Off Ramp" and section.length3D() > 50: # exclude HOV ramps
				if rampSection == None:
					rampSection = section
				else:
					if mainDownSection == None:
						mainDownSection = section
			elif str(section.getRoadType().getName()) == "Freeway":
				mainDownSection = section
		for section in node.getEntranceSections():
			inLanes += section.getNbLanesAtPos(section.length3D())
			if str(section.getRoadType().getName()) == "Freeway":
				mainUpSection = section
	return (rampSection, mainDownSection, mainUpSection, inLanes, outLanes)

def cutIfNeeded(section, minimumRampLength, minimumResidualSectionLength, right):
	global model
	canCreateLane = True
	if right:
		extremeLane = section.getLanes()[len(section.getLanes()) - 1]
	else:
		extremeLane = section.getLanes()[0]
	if not extremeLane.isFullLane(): # upstream section has an entry lateral -> cut
		cutPos = max(section.length3D() / 2, extremeLane.getFinalOffset() + minimumResidualSectionLength) # offset of entry lateral is positive from beginning
		if section.length3D() - cutPos >= minimumRampLength + minimumResidualSectionLength: # check if the section can be cut
			cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
			#cmd.commandToBeDone()
			#cmd.doit()
			#cmd.commandDone()
			#cmd.setDone(True)
			model.getCommander().addCommand(cmd)
			section = section.getDestination().getFromTurnings(section)[0].getDestination() # Get the second part
		else:
			canCreateLane = False
			model.getLog().addWarning("No space to add on-ramp to section %s!" % section.getExternalId())
	return (section, canCreateLane)

def createDecelerationLane(section, side, defaultRampLength, minimumResidualSectionLength):
	rampLength = min(defaultRampLength, section.length3D() - minimumResidualSectionLength)
	if rampLength < defaultRampLength:
		model.getLog().addWarning("Deceleration lane at section %s shorter than the default length!" % section.getExternalId())
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

def offsetRampStartpoint(rampSection, mainSection, right):
	rampPoint = rampSection.getPoint(0)
	mainPoint = mainSection.getPoint(0)
	newPos = GKPoint()
	mainPoint.pointAtDistance(mainSection.getExtremePointAtPos(0, right), (mainSection.getLaneWidth() * mainSection.getNbLanesAtPos(0) + rampSection.getLaneWidth() * rampSection.getNbLanesAtPos(0)) / 2.0, newPos)
	rampPoint.x = newPos.x
	rampPoint.y = newPos.y
	rampPoint.z = newPos.z
	rampSection.increaseTick()

def arrangeTurnLanes(section, inLanes, outLanes):
	minLane = section.getExitLanes()[0]
	currLane = section.getExitLanes()[1]
	turn = None
	for turn in section.getDestination().getFromTurningsOrderedFromRightToLeft(section):
		turnLanes = turn.getDestinationToLane() - turn.getDestinationFromLane() + 1
		turn.setOriginLanes(max(minLane, currLane - turnLanes + 1), currLane)
		turn.updatePath(True)
		turn.curve()
		currLane = max(minLane, currLane - turnLanes)
	if inLanes > outLanes: # extend the leftmost turn to cover all origin lanes
		turn.setOriginLanes(turn.getOriginFromLane() - (inLanes - outLanes), turn.getOriginToLane())
		turn.updatePath(True)
		turn.curve()

print "Creating off-ramps..."
ftToM = float(getPropertyValue("aimsun.gis.ftToM"))
defaultRampLength = float(getPropertyValue("aimsun.gis.defaultRampLength")) * ftToM # m
minimumRampLength = 15 # m
minimumResidualSectionLength = 15 # m
type = model.getType("GKNode")
for node in model.getCatalog().getObjectsByType(type).itervalues():
	(rampSection, mainDownSection, mainUpSection, inLanes, outLanes) = getSections(node)
	if rampSection != None and mainDownSection != None and mainUpSection != None:
		refPoint = rampSection.getPoint(1)
		if mainDownSection.getPoints().isPointAtRightArea(refPoint): # ramp exits from right
			if outLanes > inLanes and mainUpSection.getLane(mainUpSection.getEntryLanes()[1]).isFullLane(): # add a deceleration lane if needed and if one is not already present
				(mainUpSection, canCreateLane) = cutIfNeeded(mainUpSection, minimumRampLength, minimumResidualSectionLength, True)
				if canCreateLane:
					createDecelerationLane(mainUpSection, GKSection.eRightOut, defaultRampLength, minimumResidualSectionLength)
					inLanes += 1
			offsetRampStartpoint(rampSection, mainDownSection, True)
		else: # ramp exits from left
			if outLanes > inLanes and mainUpSection.getLane(mainUpSection.getEntryLanes()[0]).isFullLane(): # add a deceleration lane if needed and if one is not already present
				canCreateLane = cutIfNeeded(mainUpSection, minimumRampLength, minimumResidualSectionLength, False)
				if canCreateLane:
					createDecelerationLane(mainUpSection, GKSection.eLeftOut, defaultRampLength, minimumResidualSectionLength)
					outLanes += 1
			offsetRampStartpoint(rampSection, mainDownSection, False)
		arrangeTurnLanes(mainUpSection, inLanes, outLanes)

GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "Off-ramps created!"