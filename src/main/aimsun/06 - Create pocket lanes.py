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

def getTurn(section, toEid):
	eidAtt = model.getColumn("GKSection::HWYCOV_")
	node = section.getDestination()
	if node != None:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueInt(eidAtt) == toEid:
				return turn
	return None

def addExitLateral(section, side, length):
	lane = GKSectionLane()
	lane.setOffsets(-length, 0.0)
	if side == "left":
		section.addLane(lane, 0)
		if section.getOrigin() != None:
			for mturn in section.getOrigin().getToTurnings(section):
				mturn.setDestinationLanes(mturn.getDestinationFromLane() + 1, mturn.getDestinationToLane() + 1)
				mturn.updatePath(True)
		if section.getDestination() != None:
			for mturn in section.getDestination().getFromTurnings(section):
				mturn.setOriginLanes(mturn.getOriginFromLane() + 1, mturn.getOriginToLane() + 1)
				mturn.updatePath(True)
		# Expand leftmost turn
		toNode = section.getDestination()
		if toNode != None:
			exitTurns = toNode.getFromTurningsOrderedFromLeftToRight(section)
			exitTurns[0].setOriginLanes(exitTurns[0].getOriginFromLane()-1, exitTurns[0].getOriginToLane())
		#points = section.getPoints().getParallelPolyline(GK.eRoadLeft, section.getLaneWidth()/2.0)
	else:
		section.addLane(lane)
		# Expand rightmost turn
		toNode = section.getDestination()
		if toNode != None:
			exitTurns = toNode.getFromTurningsOrderedFromRightToLeft(section)
			exitTurns[0].setOriginLanes(exitTurns[0].getOriginFromLane(), exitTurns[0].getOriginToLane()+1)
		#points = section.getPoints().getParallelPolyline(GK.eRoadRight, section.getLaneWidth()/2.0)
	#delta = GKPoint(points[0].x - section.getPoints()[0].x, points[0].y - section.getPoints()[0].y, points[0].z - section.getPoints()[0].z)
	#section.translate(delta)
	section.increaseTick()

defaultBayLength = double(getPropertyValue("aimsun.gis.defaultBayLength"))
aNodeAtt = model.getColumn("GKSection::AN")
abLlAtt = model.getColumn("GKSection::ABLL")
abTlAtt = model.getColumn("GKSection::ABTL")
abRlAtt = model.getColumn("GKSection::ABRL")
baLlAtt = model.getColumn("GKSection::BALL")
baTlAtt = model.getColumn("GKSection::BATL")
baRlAtt = model.getColumn("GKSection::BARL")
abLlBAtt = model.getColumn("GKSection::ABLLB")
abTlBAtt = model.getColumn("GKSection::ABTLB")
abRlBAtt = model.getColumn("GKSection::ABRLB")
baLlBAtt = model.getColumn("GKSection::BALLB")
baTlBAtt = model.getColumn("GKSection::BATLB")
baRlBAtt = model.getColumn("GKSection::BARLB")

sectionType = model.getType("GKSection")
sections = model.getCatalog().getObjectsByType(sectionType)
task = GKSystem.getSystem().createTask(model)
task.setName("Approach configuration")
task.setTotalSteps(len(sections))
step = 0
cancelled = False
task.start()
for section in sections.itervalues():
	if not cancelled:
		# Get values
		origin = section.getOrigin()
		if origin != None:
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				numLl = section.getDataValueInt(abLlAtt)
				numTl = section.getDataValueInt(abTlAtt)
				numRl = section.getDataValueInt(abRlAtt)
				lt = section.getDataValueInt(abLlBAtt)
				tt = section.getDataValueInt(abTlBAtt)
				rt = section.getDataValueInt(abRlBAtt)
			else:
				numLl = section.getDataValueInt(baLlAtt)
				numTl = section.getDataValueInt(baTlAtt)
				numRl = section.getDataValueInt(baRlAtt)
				lt = section.getDataValueInt(baLlBAtt)
				tt = section.getDataValueInt(baTlBAtt)
				rt = section.getDataValueInt(baRlBAtt)
		else:
			destination = section.getDestination()
			if destination != None:
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					numLl = section.getDataValueInt(baLlAtt)
					numTl = section.getDataValueInt(baTlAtt)
					numRl = section.getDataValueInt(baRlAtt)
					lt = section.getDataValueInt(baLlBAtt)
					tt = section.getDataValueInt(baTlBAtt)
					rt = section.getDataValueInt(baRlBAtt)
				else:
					numLl = section.getDataValueInt(abLlAtt)
					numTl = section.getDataValueInt(abTlAtt)
					numRl = section.getDataValueInt(abRlAtt)
					lt = section.getDataValueInt(abLlBAtt)
					tt = section.getDataValueInt(abTlBAtt)
					rt = section.getDataValueInt(abRlBAtt)

		if numLl + numTl + numRl > 0:
			# Determine shared
			sharedRight = False
			sharedLeft = False
			channelizedRight = False
			bannedRight = False
			bannedThrough = False
			bannedLeft = False
			if numRl == 9:
				numRl = 0
				sharedRight = True
			elif numRl == 8:
				numRl = 0
				bannedRight = True
			elif numRl == 7:
				numRl = 1
				channelizedRight = True
			if numTl == 9:
				numTl = 0
				if tt > 0:
					if channelizedRight == True:
						sharedLeft = True
					else:
						sharedRight = True
						#print "Through movement marked as shared at approach %s; by default it will be shared through-right" % section.getId()
			elif numTl == 8:
				numTl = 0
				bannedThrough = True
			if numLl == 9:
				numLl = 0
				sharedLeft = True
			elif numLl == 8:
				numLl = 0
				bannedLeft = True

			#print "Approach %i: lanes %i, midblock lanes %i" % (section.getId(), section.getNbFullLanes(), numLl + numTl + numRl)
			#print " -- %i left turn lanes; shared: %s; banned %s" % (numLl, sharedLeft, bannedLeft)
			#print " -- %i through lanes; shared left: %s; shared right: %s; banned %s" % (numTl, sharedLeft, sharedRight, bannedThrough)
			#print " -- %i right turn lanes; shared: %s; channelized: %s; banned %s" % (numRl, sharedRight, channelizedRight, bannedRight)
			
			approachSection = section
			sectionCut = False
			# Add pocket lanes
			if section.getNbFullLanes() < numLl + numTl + numRl:
				# We need to add pocket lanes
				addLanes = numLl + numTl + numRl - section.getNbFullLanes()
				for i in range(addLanes):
					if i % 2 == 0:
						# Add to the left
						leftmostLane = section.getLane(0)
						if leftmostLane.isFullLane():
							if sectionCut:
								cmd = GKSectionChangeNbLanesCmd()
								cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
								model.getCommander().addCommand(cmd)
							addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
						else:
							# We have to cut
							if leftmostLane.isAnExitLateral():
								# Cut 5m before the beginning of the lateral
								cutPos = section.length3D() + leftmostLane.getInitialOffset() - 5
								if cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									sectionCut = True
									approachSection = section.getDestination().getFromTurnings(section)[0].getDestination()
									cmd = GKSectionChangeNbLanesCmd()
									cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
									model.getCommander().addCommand(cmd)
									addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
									section.getDestination().getFromTurnings(section)[0].setOriginLanes(0, section.getNbLanesAtPos(section.length2D()) - 1)
								else:
									print "Section %i too short to be cut" % section.getId()
									# TODO: add to upstream section
							elif leftmostLane.isAnEntryLateral():
								# Cut 5m after the beginning of the lateral
								cutPos = leftmostLane.getFinalOffset() + 5
								if section.length3D() - cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									section = section.getDestination().getFromTurnings(section)[0].getDestination()
									addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
								else:
									print "Section %i too short to be cut" % section.getId()
					else:
						# Add to the right
						rightmostLane = section.getLane(len(section.getLanes()) - 1)
						if rightmostLane.isFullLane():
							if sectionCut:
								cmd = GKSectionChangeNbLanesCmd()
								cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
								model.getCommander().addCommand(cmd)
							addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
						else:
							# We have to cut
							if rightmostLane.isAnExitLateral():
								# Cut 5m before the beginning of the lateral
								cutPos = section.length3D() + rightmostLane.getInitialOffset() - 5
								if cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									sectionCut = True
									approachSection = section.getDestination().getFromTurnings(section)[0].getDestination()
									cmd = GKSectionChangeNbLanesCmd()
									cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
									model.getCommander().addCommand(cmd)
									addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
									section.getDestination().getFromTurnings(section)[0].setOriginLanes(0, section.getNbLanesAtPos(section.length2D()) - 1)
								else:
									print "Section %i too short to be cut" % section.getId()
									# TODO: add to upstream section
							elif rightmostLane.isAnEntryLateral():
								# Cut 5m after the beginning of the lateral
								cutPos = rightmostLane.getFinalOffset() + 5
								if section.length3D() - cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									section = section.getDestination().getFromTurnings(section)[0].getDestination()
									addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
								else:
									print "Section %i too short to be cut" % section.getId()

			# Assign turns to lanes
			maxLane = approachSection.getNbLanesAtPos(approachSection.length2D()) - 1
			currLane = 0
			leftTurn = getTurn(approachSection, lt)
			if bannedLeft:
				if leftTurn != None:
					cmd = leftTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if leftTurn != None:
					leftTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numLl - 1)))
					leftTurn.updatePath(True)
				if sharedLeft:
					currLane = currLane + max(0, numLl - 1)
				else:
					currLane += numLl
			if currLane >= maxLane:
					model.getLog().addError("Review lane assignment at approach %i" % approachSection.getId())
			throughTurn = getTurn(approachSection, tt)
			if bannedThrough:
				if throughTurn != None:
					cmd = throughTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if throughTurn != None:
					throughTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numTl - 1)))
					throughTurn.updatePath(True)
				if sharedRight:
					currLane = currLane + max(0, numTl - 1)
				else:
					currLane += numTl
			if currLane >= maxLane:
					currLane = maxLane
					model.getLog().addError("Review lane assignment at approach %i" % approachSection.getId())
			rightTurn = getTurn(approachSection, rt)
			if bannedRight:
				if rightTurn != None:
					cmd = rightTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if rightTurn != None:
					rightTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numRl - 1)))
					rightTurn.updatePath(True)

		step += 1
		cancelled = task.stepTask(step)
task.end()
GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "Done!"defaultBayLength = 20 # m

def getTurn(section, toEid):
	eidAtt = model.getColumn("GKSection::HWYCOV_")
	node = section.getDestination()
	if node != None:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueInt(eidAtt) == toEid:
				return turn
	return None

def addExitLateral(section, side, length):
	lane = GKSectionLane()
	lane.setOffsets(-length, 0.0)
	if side == "left":
		section.addLane(lane, 0)
		if section.getOrigin() != None:
			for mturn in section.getOrigin().getToTurnings(section):
				mturn.setDestinationLanes(mturn.getDestinationFromLane() + 1, mturn.getDestinationToLane() + 1)
				mturn.updatePath(True)
		if section.getDestination() != None:
			for mturn in section.getDestination().getFromTurnings(section):
				mturn.setOriginLanes(mturn.getOriginFromLane() + 1, mturn.getOriginToLane() + 1)
				mturn.updatePath(True)
		# Expand leftmost turn
		toNode = section.getDestination()
		if toNode != None:
			exitTurns = toNode.getFromTurningsOrderedFromLeftToRight(section)
			exitTurns[0].setOriginLanes(exitTurns[0].getOriginFromLane()-1, exitTurns[0].getOriginToLane())
		#points = section.getPoints().getParallelPolyline(GK.eRoadLeft, section.getLaneWidth()/2.0)
	else:
		section.addLane(lane)
		# Expand rightmost turn
		toNode = section.getDestination()
		if toNode != None:
			exitTurns = toNode.getFromTurningsOrderedFromRightToLeft(section)
			exitTurns[0].setOriginLanes(exitTurns[0].getOriginFromLane(), exitTurns[0].getOriginToLane()+1)
		#points = section.getPoints().getParallelPolyline(GK.eRoadRight, section.getLaneWidth()/2.0)
	#delta = GKPoint(points[0].x - section.getPoints()[0].x, points[0].y - section.getPoints()[0].y, points[0].z - section.getPoints()[0].z)
	#section.translate(delta)
	section.increaseTick()

aNodeAtt = model.getColumn("GKSection::AN")
abLlAtt = model.getColumn("GKSection::ABLL")
abTlAtt = model.getColumn("GKSection::ABTL")
abRlAtt = model.getColumn("GKSection::ABRL")
baLlAtt = model.getColumn("GKSection::BALL")
baTlAtt = model.getColumn("GKSection::BATL")
baRlAtt = model.getColumn("GKSection::BARL")
abLlBAtt = model.getColumn("GKSection::ABLLB")
abTlBAtt = model.getColumn("GKSection::ABTLB")
abRlBAtt = model.getColumn("GKSection::ABRLB")
baLlBAtt = model.getColumn("GKSection::BALLB")
baTlBAtt = model.getColumn("GKSection::BATLB")
baRlBAtt = model.getColumn("GKSection::BARLB")

sectionType = model.getType("GKSection")
sections = model.getCatalog().getObjectsByType(sectionType)
task = GKSystem.getSystem().createTask(model)
task.setName("Approach configuration")
task.setTotalSteps(len(sections))
step = 0
cancelled = False
task.start()
for section in sections.itervalues():
	if not cancelled:
		# Get values
		origin = section.getOrigin()
		if origin != None:
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				numLl = section.getDataValueInt(abLlAtt)
				numTl = section.getDataValueInt(abTlAtt)
				numRl = section.getDataValueInt(abRlAtt)
				lt = section.getDataValueInt(abLlBAtt)
				tt = section.getDataValueInt(abTlBAtt)
				rt = section.getDataValueInt(abRlBAtt)
			else:
				numLl = section.getDataValueInt(baLlAtt)
				numTl = section.getDataValueInt(baTlAtt)
				numRl = section.getDataValueInt(baRlAtt)
				lt = section.getDataValueInt(baLlBAtt)
				tt = section.getDataValueInt(baTlBAtt)
				rt = section.getDataValueInt(baRlBAtt)
		else:
			destination = section.getDestination()
			if destination != None:
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					numLl = section.getDataValueInt(baLlAtt)
					numTl = section.getDataValueInt(baTlAtt)
					numRl = section.getDataValueInt(baRlAtt)
					lt = section.getDataValueInt(baLlBAtt)
					tt = section.getDataValueInt(baTlBAtt)
					rt = section.getDataValueInt(baRlBAtt)
				else:
					numLl = section.getDataValueInt(abLlAtt)
					numTl = section.getDataValueInt(abTlAtt)
					numRl = section.getDataValueInt(abRlAtt)
					lt = section.getDataValueInt(abLlBAtt)
					tt = section.getDataValueInt(abTlBAtt)
					rt = section.getDataValueInt(abRlBAtt)

		if numLl + numTl + numRl > 0:
			# Determine shared
			sharedRight = False
			sharedLeft = False
			channelizedRight = False
			bannedRight = False
			bannedThrough = False
			bannedLeft = False
			if numRl == 9:
				numRl = 0
				sharedRight = True
			elif numRl == 8:
				numRl = 0
				bannedRight = True
			elif numRl == 7:
				numRl = 1
				channelizedRight = True
			if numTl == 9:
				numTl = 0
				if tt > 0:
					if channelizedRight == True:
						sharedLeft = True
					else:
						sharedRight = True
						#print "Through movement marked as shared at approach %s; by default it will be shared through-right" % section.getId()
			elif numTl == 8:
				numTl = 0
				bannedThrough = True
			if numLl == 9:
				numLl = 0
				sharedLeft = True
			elif numLl == 8:
				numLl = 0
				bannedLeft = True

			#print "Approach %i: lanes %i, midblock lanes %i" % (section.getId(), section.getNbFullLanes(), numLl + numTl + numRl)
			#print " -- %i left turn lanes; shared: %s; banned %s" % (numLl, sharedLeft, bannedLeft)
			#print " -- %i through lanes; shared left: %s; shared right: %s; banned %s" % (numTl, sharedLeft, sharedRight, bannedThrough)
			#print " -- %i right turn lanes; shared: %s; channelized: %s; banned %s" % (numRl, sharedRight, channelizedRight, bannedRight)
			
			approachSection = section
			sectionCut = False
			# Add pocket lanes
			if section.getNbFullLanes() < numLl + numTl + numRl:
				# We need to add pocket lanes
				addLanes = numLl + numTl + numRl - section.getNbFullLanes()
				for i in range(addLanes):
					if i % 2 == 0:
						# Add to the left
						leftmostLane = section.getLane(0)
						if leftmostLane.isFullLane():
							if sectionCut:
								cmd = GKSectionChangeNbLanesCmd()
								cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
								model.getCommander().addCommand(cmd)
							addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
						else:
							# We have to cut
							if leftmostLane.isAnExitLateral():
								# Cut 5m before the beginning of the lateral
								cutPos = section.length3D() + leftmostLane.getInitialOffset() - 5
								if cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									sectionCut = True
									approachSection = section.getDestination().getFromTurnings(section)[0].getDestination()
									cmd = GKSectionChangeNbLanesCmd()
									cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
									model.getCommander().addCommand(cmd)
									addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
									section.getDestination().getFromTurnings(section)[0].setOriginLanes(0, section.getNbLanesAtPos(section.length2D()) - 1)
								else:
									print "Section %i too short to be cut" % section.getId()
									# TODO: add to upstream section
							elif leftmostLane.isAnEntryLateral():
								# Cut 5m after the beginning of the lateral
								cutPos = leftmostLane.getFinalOffset() + 5
								if section.length3D() - cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									section = section.getDestination().getFromTurnings(section)[0].getDestination()
									addExitLateral(section, "left", min(defaultBayLength, section.length3D()-5))
								else:
									print "Section %i too short to be cut" % section.getId()
					else:
						# Add to the right
						rightmostLane = section.getLane(len(section.getLanes()) - 1)
						if rightmostLane.isFullLane():
							if sectionCut:
								cmd = GKSectionChangeNbLanesCmd()
								cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
								model.getCommander().addCommand(cmd)
							addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
						else:
							# We have to cut
							if rightmostLane.isAnExitLateral():
								# Cut 5m before the beginning of the lateral
								cutPos = section.length3D() + rightmostLane.getInitialOffset() - 5
								if cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									sectionCut = True
									approachSection = section.getDestination().getFromTurnings(section)[0].getDestination()
									cmd = GKSectionChangeNbLanesCmd()
									cmd.setData(approachSection, approachSection.getDataValueIntByID(GKSection.nblanesAtt) + 1)
									model.getCommander().addCommand(cmd)
									addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
									section.getDestination().getFromTurnings(section)[0].setOriginLanes(0, section.getNbLanesAtPos(section.length2D()) - 1)
								else:
									print "Section %i too short to be cut" % section.getId()
									# TODO: add to upstream section
							elif rightmostLane.isAnEntryLateral():
								# Cut 5m after the beginning of the lateral
								cutPos = rightmostLane.getFinalOffset() + 5
								if section.length3D() - cutPos > 15:
									cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
									cmd.doit()
									cmd.setDone(True)
									section = section.getDestination().getFromTurnings(section)[0].getDestination()
									addExitLateral(section, "right", min(defaultBayLength, section.length3D()-5))
								else:
									print "Section %i too short to be cut" % section.getId()

			# Assign turns to lanes
			maxLane = approachSection.getNbLanesAtPos(approachSection.length2D()) - 1
			currLane = 0
			leftTurn = getTurn(approachSection, lt)
			if bannedLeft:
				if leftTurn != None:
					cmd = leftTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if leftTurn != None:
					leftTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numLl - 1)))
					leftTurn.updatePath(True)
				if sharedLeft:
					currLane = currLane + max(0, numLl - 1)
				else:
					currLane += numLl
			if currLane >= maxLane:
					model.getLog().addError("Review lane assignment at approach %i" % approachSection.getId())
			throughTurn = getTurn(approachSection, tt)
			if bannedThrough:
				if throughTurn != None:
					cmd = throughTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if throughTurn != None:
					throughTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numTl - 1)))
					throughTurn.updatePath(True)
				if sharedRight:
					currLane = currLane + max(0, numTl - 1)
				else:
					currLane += numTl
			if currLane >= maxLane:
					currLane = maxLane
					model.getLog().addError("Review lane assignment at approach %i" % approachSection.getId())
			rightTurn = getTurn(approachSection, rt)
			if bannedRight:
				if rightTurn != None:
					cmd = rightTurn.getDelCmd()
					model.getCommander().addCommand(cmd)
			else:
				if rightTurn != None:
					rightTurn.setOriginLanes(min(maxLane, currLane), min(maxLane, currLane + max(0, numRl - 1)))
					rightTurn.updatePath(True)

		step += 1
		cancelled = task.stepTask(step)
task.end()
GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "Done!"