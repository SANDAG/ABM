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

print "Creating on-ramps..."
ftToM = float(getPropertyValue("aimsun.gis.ftToM"))
defaultRampLength = float(getPropertyValue("aimsun.gis.defaultRampLength")) * ftToM
sectionType = model.getType("GKSection")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	if str(section.getRoadType().getName()) == "On/Off Ramp" and section.length3D() > 50:
		toNode = section.getDestination()
		if toNode != None:
			for turn in toNode.getFromTurnings(section):
				toSection = turn.getDestination()
				if str(toSection.getRoadType().getName()) == "Freeway":
					refPoint = section.getPoints()[len(section.getPoints())-2]
					refSection = None
					for mturn in toNode.getToTurnings(toSection):
						if str(mturn.getOrigin().getRoadType().getName()) == "Freeway":
							refSection = mturn.getOrigin()
					if refSection != None:
						aux = getNbAuxLanes(toSection)
						lane = GKSectionLane()
						if aux == 0:
							lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
						pocketCreated = False
						if refSection.getPoints().isPointAtRightArea(refPoint):
							if toSection.getLane(toSection.getEntryLanes()[1]).isFullLane():
								position = len(toSection.getLanes())
								rightmostLane = toSection.getLanes()[len(toSection.getLanes()) - 1]
								proceed = True
								if not rightmostLane.isFullLane():
									# It's an exit lateral
									if aux > 0:
										position -= 1
									else:
										proceed = False
										offset = - rightmostLane.getInitialOffset()
										#cutPos = toSection.length3D() - offset - 5
										cutPos = toSection.length3D() / 2
										#if cutPos > 15:
										if cutPos > 15 and cutPos > toSection.length3D() - offset - 15:
											cmd = toSection.getCutCmd(toSection.getExtremePointAtPos(cutPos, False), toSection.getExtremePointAtPos(cutPos, True), True)
											cmd.doit()
											cmd.setDone(True)
											proceed = True
											lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
											position = len(toSection.getLanes())
								if proceed:
									toSection.addLane(lane, position)
									points = toSection.getPoints().getParallelPolyline(GK.eRoadRight, toSection.getLaneWidth()/2.0)
									turn.setDestinationLanes(max(toSection.getEntryLanes()[0], toSection.getEntryLanes()[1] - section.getNbLanesAtPos(section.length3D()) + 1), toSection.getEntryLanes()[1])
									endPoint = section.getPoints()[len(section.getPoints())-1]
									refEndPoint = refSection.getPoints()[len(refSection.getPoints())-1]
									newPos = GKPoint()
									refEndPoint.pointAtDistance(refSection.getExtremePointAtPos(refSection.length3D(), True), (refSection.getLaneWidth() * refSection.getNbLanesAtPos(refSection.length3D()) + section.getLaneWidth() * section.getNbLanesAtPos(section.length3D())) / 2.0, newPos)
									endPoint.x = newPos.x
									endPoint.y = newPos.y
									endPoint.z = newPos.z
									section.increaseTick()
									pocketCreated = True
								else:
									print "No space to add on-ramp to section %s" % toSection.getId()
						else:
							if toSection.getLane(toSection.getEntryLanes()[0]).isFullLane():
								position = 0
								leftmostLane = toSection.getLanes()[0]
								proceed = True
								if not leftmostLane.isFullLane():
									# It's an exit lateral
									if aux > 0:
										position += 1
									else:
										proceed = False
										offset = - leftmostLane.getInitialOffset()
										#cutPos = toSection.length3D() - offset - 5
										cutPos = toSection.length3D() / 2
										#if cutPos > 15:
										if cutPos > 15 and cutPos > toSection.length3D() - offset - 15:
											cmd = toSection.getCutCmd(toSection.getExtremePointAtPos(cutPos, False), toSection.getExtremePointAtPos(cutPos, True), True)
											cmd.doit()
											cmd.setDone(True)
											proceed = True
											lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
								if proceed:
									toSection.addLane(lane, position)
									points = toSection.getPoints().getParallelPolyline(GK.eRoadLeft, toSection.getLaneWidth()/2.0)
									for mturn in toNode.getToTurningsOrderedFromLeftToRight(toSection):
										mturn.setDestinationLanes(mturn.getDestinationFromLane() + 1, mturn.getDestinationToLane() + 1)
									if toSection.getDestination() != None:
										for mturn in toSection.getDestination().getFromTurnings(toSection):
											if aux == 0:
												mturn.setOriginLanes(mturn.getOriginFromLane() + 1, mturn.getOriginToLane() + 1)
									turn.setDestinationLanes(toSection.getEntryLanes()[0], min(toSection.getEntryLanes()[1], toSection.getEntryLanes()[0] + section.getNbLanesAtPos(0)-1))
									endPoint = section.getPoints()[len(section.getPoints())-1]
									refEndPoint = refSection.getPoints()[len(refSection.getPoints())-1]
									newPos = GKPoint()
									refEndPoint.pointAtDistance(refSection.getExtremePointAtPos(refSection.length3D(), False), (refSection.getLaneWidth() * refSection.getNbLanesAtPos(refSection.length3D()) + section.getLaneWidth() * section.getNbLanesAtPos(section.length3D())) / 2.0, newPos)
									endPoint.x = newPos.x
									endPoint.y = newPos.y
									endPoint.z = newPos.z
									section.increaseTick()
									pocketCreated = True
						if pocketCreated:
							delta = GKPoint(points[0].x - toSection.getPoints()[0].x, points[0].y - toSection.getPoints()[0].y, points[0].z - toSection.getPoints()[0].z)
							toSection.translate(delta)
							toSection.increaseTick()
							for mturn in toNode.getTurnings():
								mturn.updatePath(True)
							if toSection.getDestination() != None:
								for mturn in toSection.getDestination().getTurnings():
									mturn.updatePath(True)

GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "On-ramps created!"
