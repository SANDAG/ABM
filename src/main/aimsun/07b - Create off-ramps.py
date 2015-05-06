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

print "Creating off-ramps..."
ftToM = float(getPropertyValue("aimsun.gis.ftToM"))
defaultRampLength = float(getPropertyValue("aimsun.gis.defaultRampLength")) * ftToM
sectionType = model.getType("GKSection")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	if str(section.getRoadType().getName()) == "On/Off Ramp" and section.length3D() > 50:
		fromNode = section.getOrigin()
		if fromNode != None:
			for turn in fromNode.getToTurnings(section):
				fromSection = turn.getOrigin()
				if str(fromSection.getRoadType().getName()) == "Freeway":
					refPoint = section.getPoints()[1]
					refSection = None
					for mturn in fromNode.getFromTurnings(fromSection):
						if str(mturn.getDestination().getRoadType().getName()) == "Freeway":
							refSection = mturn.getDestination()
					if refSection != None:
						aux = getNbAuxLanes(fromSection)
						if aux == 0:
							lane = GKSectionLane()
							lane.setOffsets(- min(defaultRampLength, fromSection.length3D()-5), 0.0)
						pocketCreated = False
						if refSection.getPoints().isPointAtRightArea(refPoint):
							if fromSection.getLane(fromSection.getExitLanes()[1]).isFullLane():
								rightmostLane = fromSection.getLanes()[len(fromSection.getLanes()) - 1]
								proceed = True
								if not rightmostLane.isFullLane() and aux == 0:
									# It's an entry lateral
									proceed = False
									offset = rightmostLane.getFinalOffset()
									#cutPos = offset + 5
									cutPos = fromSection.length3D() / 2
									#if cutPos + 15 <= fromSection.length3D():
									if cutPos + 15 <= fromSection.length3D() and cutPos > offset + 15:
										cmd = fromSection.getCutCmd(fromSection.getExtremePointAtPos(cutPos, False), fromSection.getExtremePointAtPos(cutPos, True), True)
										cmd.doit()
										cmd.setDone(True)
										fromSection = fromSection.getDestination().getFromTurnings(fromSection)[0].getDestination() # Get the second part
										proceed = True
										lane.setOffsets(- min(defaultRampLength, fromSection.length3D()-5), 0.0)
								if proceed:
									if aux == 0:
										fromSection.addLane(lane)
									points = fromSection.getPoints().getParallelPolyline(GK.eRoadRight, fromSection.getLaneWidth()/2.0)
									turn.setOriginLanes(max(fromSection.getExitLanes()[0], fromSection.getExitLanes()[1] - section.getNbLanesAtPos(0) + 1), fromSection.getExitLanes()[1])
									startPoint = section.getPoints()[0]
									refStartPoint = refSection.getPoints()[0]
									newPos = GKPoint()
									refStartPoint.pointAtDistance(refSection.getExtremePointAtPos(0, True), (refSection.getLaneWidth() * refSection.getNbLanesAtPos(0) + section.getLaneWidth() * section.getNbLanesAtPos(0)) / 2.0, newPos)
									startPoint.x = newPos.x
									startPoint.y = newPos.y
									startPoint.z = newPos.z
									section.increaseTick()
									pocketCreated = True
								else:
									print "No space to add off ramp to section %s" % fromSection.getId()
						else:
							if fromSection.getLane(fromSection.getExitLanes()[0]).isFullLane():
								leftmostLane = fromSection.getLanes()[0]
								proceed = True
								if not leftmostLane.isFullLane() and aux == 0:
									# It's an entry lateral
									proceed = False
									offset = rightmostLane.getFinalOffset()
									#cutPos = offset + 5
									cutPos = fromSection.length3D() / 2
									#if cutPos + 15 <= fromSection.length3D():
									if cutPos + 15 <= fromSection.length3D() and cutPos > offset + 15:
										cmd = fromSection.getCutCmd(fromSection.getExtremePointAtPos(cutPos, False), fromSection.getExtremePointAtPos(cutPos, True), True)
										cmd.doit()
										cmd.setDone(True)
										fromSection = fromSection.getDestination().getFromTurnings(fromSection)[0].getDestination() # Get the second part
										proceed = True
										lane.setOffsets(- min(defaultRampLength, fromSection.length3D()-5), 0.0)
								if proceed:
									if aux == 0:
										fromSection.addLane(lane, 0)
									points = fromSection.getPoints().getParallelPolyline(GK.eRoadLeft, fromSection.getLaneWidth()/2.0)
									for mturn in fromNode.getTurnings():
										mturn.setOriginLanes(mturn.getOriginFromLane() + 1, mturn.getOriginToLane() + 1)
									if fromSection.getOrigin() != None:
										for mturn in fromSection.getOrigin().getTurnings():
											mturn.setDestinationLanes(mturn.getDestinationFromLane() + 1, mturn.getDestinationToLane() + 1)
									turn.setOriginLanes(fromSection.getExitLanes()[0], min(fromSection.getExitLanes()[1], fromSection.getExitLanes()[0] + section.getNbLanesAtPos(0) - 1))
									startPoint = section.getPoints()[0]
									refStartPoint = refSection.getPoints()[0]
									newPos = GKPoint()
									refStartPoint.pointAtDistance(refSection.getExtremePointAtPos(0, False), (refSection.getLaneWidth() * refSection.getNbLanesAtPos(0) + section.getLaneWidth() * section.getNbLanesAtPos(0)) / 2.0, newPos)
									startPoint.x = newPos.x
									startPoint.y = newPos.y
									startPoint.z = newPos.z
									section.increaseTick()
									pocketCreated = True
								else:
									print "No space to add off ramp to section %s" % fromSection.getId()
						if pocketCreated:
							delta = GKPoint(points[0].x - fromSection.getPoints()[0].x, points[0].y - fromSection.getPoints()[0].y, points[0].z - fromSection.getPoints()[0].z)
							fromSection.translate(delta)
							fromSection.increaseTick()
							for mturn in fromNode.getTurnings():
								mturn.updatePath(True)
							if fromSection.getOrigin() != None:
								for mturn in fromSection.getOrigin().getTurnings():
									mturn.updatePath(True)

GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "Off-ramps created!"
