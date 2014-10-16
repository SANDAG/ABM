def getPropertyValue(propertyKey):
	value = ""
	preferences = model.getPreferences()
	filePath = str(preferences.getValue("CT-RAMP:PropertyFile"))
	file = open(filePath, "r")
	for line in file.readlines():
		if not line[0] == "#":
			tokens = line.rstrip("\n").split("=")
			if tokens[0].strip() == propertyKey:
				value = tokens[1].strip()
	file.close()
	return value

defaultRampLength = double(getPropertyValue("aimsun.gis.defaultRampLength"))
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
						lane = GKSectionLane()
						lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
						pocketCreated = False
						if refSection.getPoints().isPointAtRightArea(refPoint):
							if toSection.getLane(toSection.getEntryLanes()[1]).isFullLane():
								rightmostLane = toSection.getLanes()[len(toSection.getLanes()) - 1]
								proceed = True
								if not rightmostLane.isFullLane():
									# It's an exit lateral
									proceed = False
									offset = - rightmostLane.getInitialOffset()
									cutPos = toSection.length3D() - offset - 5
									if cutPos > 15:
										cmd = toSection.getCutCmd(toSection.getExtremePointAtPos(cutPos, False), toSection.getExtremePointAtPos(cutPos, True), True)
										cmd.doit()
										cmd.setDone(True)
										proceed = True
										lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
								if proceed:
									toSection.addLane(lane)
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
									print "No space to add on ramp to section %s" % toSection.getId()
						else:
							if toSection.getLane(toSection.getEntryLanes()[0]).isFullLane():
								leftmostLane = toSection.getLanes()[0]
								proceed = True
								if not leftmostLane.isFullLane():
									# It's an exit lateral
									proceed = False
									offset = - leftmostLane.getInitialOffset()
									cutPos = toSection.length3D() - offset - 5
									if cutPos > 15:
										cmd = toSection.getCutCmd(toSection.getExtremePointAtPos(cutPos, False), toSection.getExtremePointAtPos(cutPos, True), True)
										cmd.doit()
										cmd.setDone(True)
										proceed = True
										lane.setOffsets(0.0, min(defaultRampLength, toSection.length3D()-5))
								if proceed:
									toSection.addLane(lane, 0)
									points = toSection.getPoints().getParallelPolyline(GK.eRoadLeft, toSection.getLaneWidth()/2.0)
									for mturn in toNode.getTurnings():
										mturn.setDestinationLanes(mturn.getDestinationFromLane() + 1, mturn.getDestinationToLane() + 1)
									if toSection.getDestination() != None:
										for mturn in toSection.getDestination().getTurnings():
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
print "Done!"