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

# Finds an outgoing turn to a section with given HWYCOV_ID
def getTurn(section, toEid):
	eidAtt = model.getColumn("GKSection::HWYCOV_")
	node = section.getDestination()
	if node != None:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueInt(eidAtt) == toEid:
				return turn
	return None

def getLeftTurn(node, section):
	# first, try tu use xxLLB
	idAtt = model.getColumn("GKSection::HWYCOV_")
	aNodeAtt = model.getColumn("GKSection::AN")
	if section.getDataValueInt(aNodeAtt) == int(str(node.getExternalId())):
		llAtt = model.getColumn("GKSection::BALLB")
	else:
		llAtt = model.getColumn("GKSection::ABLLB")
	ll = section.getDataValueDouble(llAtt)
	if ll > 0:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueDouble(idAtt) == ll:
				return turn
	# otherwise, use the angle
	for turn in node.getFromTurnings(section):
		if turn.calcAngleSections() >= 45:
			return turn
	return None

def getThroughTurn(node, section):
	# first, try tu use xxTLB
	idAtt = model.getColumn("GKSection::HWYCOV_")
	aNodeAtt = model.getColumn("GKSection::AN")
	if section.getDataValueInt(aNodeAtt) == int(str(node.getExternalId())):
		tlAtt = model.getColumn("GKSection::BATLB")
	else:
		tlAtt = model.getColumn("GKSection::ABTLB")
	tl = section.getDataValueDouble(tlAtt)
	if tl > 0:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueDouble(idAtt) == tl:
				return turn
	# otherwise, use the angle
	for turn in node.getFromTurnings(section):
		if turn.calcAngleSections() > -45 and turn.calcAngleSections() < 45:
			return turn
	return None

def getRightTurn(node, section):
	# first, try tu use xxRLB
	idAtt = model.getColumn("GKSection::HWYCOV_")
	aNodeAtt = model.getColumn("GKSection::AN")
	if section.getDataValueInt(aNodeAtt) == int(str(node.getExternalId())):
		rlAtt = model.getColumn("GKSection::BARLB")
	else:
		rlAtt = model.getColumn("GKSection::ABRLB")
	rl = section.getDataValueDouble(rlAtt)
	if rl > 0:
		for turn in node.getFromTurnings(section):
			if turn.getDestination().getDataValueDouble(idAtt) == rl:
				return turn
	# otherwise, use the angle
	for turn in node.getFromTurnings(section):
		if turn.calcAngleSections() <= -45:
			return turn
	return None

# Finds the corresponding section in the oposite direction
# Identifies it by the fact it has the same External ID
def getOppositeSection(section):
	node = section.getDestination()
	if node != None:
		for toSection in node.getExitSections():
			if toSection.getExternalId() == section.getExternalId():
				return toSection
	return None

# Adds a lateral lane of given length to the given side of a section
# The section must not have already a lateral lane on that side (check before calling this function)
def addExitLateral(section, side, length):
	lane = GKSectionLane()
	lane.setOffsets(-length, 0.0)
	if side == "left":
		section.addLane(lane, 0)
		if section.getOrigin() != None:
			for turn in section.getOrigin().getToTurnings(section):
				turn.setDestinationLanes(turn.getDestinationFromLane() + 1, turn.getDestinationToLane() + 1)
				turn.updatePath(True)
		if section.getDestination() != None:
			for turn in section.getDestination().getFromTurnings(section):
				turn.setOriginLanes(turn.getOriginFromLane() + 1, turn.getOriginToLane() + 1)
				turn.updatePath(True)
		points = section.getPoints().getParallelPolyline(GK.eRoadLeft, section.getLaneWidth()/2.0)
	else:
		section.addLane(lane)
		points = section.getPoints().getParallelPolyline(GK.eRoadRight, section.getLaneWidth()/2.0)
	delta = GKPoint(points[0].x - section.getPoints()[0].x, points[0].y - section.getPoints()[0].y, points[0].z - section.getPoints()[0].z)
	section.translate(delta)

# Cuts a section at given distance from entrance and returns the downstream part
def cut(section, cutPos):
	cmd = section.getCutCmd(section.getExtremePointAtPos(cutPos, False), section.getExtremePointAtPos(cutPos, True), True)
	cmd.doit()
	cmd.setDone(True)
	section = section.getDestination().getFromTurnings(section)[0].getDestination()
	return section

# Adds a turning bay of given length to the given side of a section
# Cuts the section if there is already a lateral lane on that side
# Has to be called from longer to shorter pockets (from inside to outside)
def addTurningBay(section, side, length):
	error = False
	if length > 0:
		if side == "left":
			extremeLane = section.getLane(0) # leftmost lane
		else:
			extremeLane = section.getLane(len(section.getLanes()) - 1) # rightmost lane
		if not extremeLane.isFullLane():
			if extremeLane.isAnExitLateral():
				cutPos = section.length3D() - length / 2 + extremeLane.getInitialOffset() / 2
			else: # entrance lateral
				cutPos = section.length3D() / 2 - length / 2 + extremeLane.getFinalOffset() / 2
			if ( ( extremeLane.isAnExitLateral() and cutPos > section.length3D() + extremeLane.getInitialOffset() + 2 ) or ( extremeLane.isAnEntryLateral() and cutPos > extremeLane.getFinalOffset() + 2 ) ):
				section = cut(section, cutPos)
				if side == "left":
					extremeLane = section.getLane(0) # leftmost lane
				else:
					extremeLane = section.getLane(len(section.getLanes()) - 1) # rightmost lane
			else:
				error = True
				print "Section %i too short to be cut" % section.getId()
		if extremeLane.isFullLane():
			addExitLateral(section, side, min(length, section.length3D()-2))
	return (error, section)

# Determines the number of shared lanes based on the old convention
# 7 -> channelized
# 8 -> banned or non existent
# 9 -> shared
def convertNumLanes(numLl, numTl, numRl):
    numStl = 0
    numStr = 0
    numSlr = 0
    numStlr = 0
    sharedLeft = False
    sharedRight = False

    if numLl == 9: # Shared
        sharedLeft = True
        numLl = 0
    elif numLl == 8: # Banned
        numLl = 0
    elif numLl == 7: # This shouldn't happen
        numLl = 1
        
    if numRl == 9: # Shared
        sharedRight = True
        numRl = 0
    elif numRl == 8: # Banned
        numRl = 0
    elif numRl == 7: # Channelized
        numRl = 1

    if numTl == 9:
        numTl = 0
        # if numTl = 9 and numLl = 9 -> only right turn allowed
        # if numTl = 9 and numRl = 9 -> only left turn allowed (but this shouldn't happen)
        # numTl = 9 and numLl = 9 and numRl = 9 -> this shouldn't happen
        if not (sharedLeft or sharedRight):
            if numLl > 0 and numRl > 0:
                numStlr = 1
                numLl -= 1
                numRl -= 1
            elif numLl > 0:
                numStl = 1
                numLl -= 1
            elif numRl > 0:
                numStr = 1
                numRl -= 1
    elif numTl == 8:
        numTl = 0
        if sharedLeft or sharedRight:
            numSlr = 1
            if not sharedLeft:
                numLl -= 1
            if not sharedRight:
                numRl -= 1
    else:
        if numTl == 7: # This shouldn't happen
            numTl = 1
        if sharedLeft and sharedRight and numTl == 1:
            numStlr = 1
            numTl -= 1
        else:
            if sharedLeft:
                numStl = 1
                numTl -= 1
            if sharedRight:
                numStr = 1
                numTl -= 1
            
    return(numLl, numTl, numRl, numStl, numStr, numSlr, numStlr)

print "Configuring intersection approaches..."

defaultBayLength = float(getPropertyValue("aimsun.gis.defaultBayLength"))
ftToM = float(getPropertyValue("aimsun.gis.ftToM"))

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
abStlrAtt = model.getColumn("GKSection::ABSTLR")
abStlAtt = model.getColumn("GKSection::ABSTL")
abStrAtt = model.getColumn("GKSection::ABSTR")
abSlrAtt = model.getColumn("GKSection::ABSLR")
baStlrAtt = model.getColumn("GKSection::BASTLR")
baStlAtt = model.getColumn("GKSection::BASTL")
baStrAtt = model.getColumn("GKSection::BASTR")
baSlrAtt = model.getColumn("GKSection::BASLR")
abRtbl1Att = model.getColumn("GKSection::ABRTBL1")
abRtbl2Att = model.getColumn("GKSection::ABRTBL2")
abRtbl3Att = model.getColumn("GKSection::ABRTBL3")
abLtbl1Att = model.getColumn("GKSection::ABLTBL1")
abLtbl2Att = model.getColumn("GKSection::ABLTBL2")
abLtbl3Att = model.getColumn("GKSection::ABLTBL3")
baRtbl1Att = model.getColumn("GKSection::BARTBL1")
baRtbl2Att = model.getColumn("GKSection::BARTBL2")
baRtbl3Att = model.getColumn("GKSection::BARTBL3")
baLtbl1Att = model.getColumn("GKSection::BALTBL1")
baLtbl2Att = model.getColumn("GKSection::BALTBL2")
baLtbl3Att = model.getColumn("GKSection::BALTBL3")
abUtAtt = model.getColumn("GKSection::ABUT")
abNtorAtt = model.getColumn("GKSection::ABNTOR")
baUtAtt = model.getColumn("GKSection::BAUT")
baNtorAtt = model.getColumn("GKSection:BANTOR")
abControlAtt = model.getColumn("GKSection::ABCNT")
baControlAtt = model.getColumn("GKSection::BACNT")

sectionType = model.getType("GKSection")
sections = model.getCatalog().getObjectsByType(sectionType)
task = GKSystem.getSystem().createTask(model)
task.setName("Approach configuration")
task.setTotalSteps(len(sections))
step = 0
cancelled = False
task.start()
for section in sections.itervalues():
	if not cancelled and section.getDestination() != None:
		toNodeExtId = str(section.getDestination().getExternalId())
		# Get attribute values
		origin = section.getOrigin()
		if origin != None and str(origin.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
				numLl = section.getDataValueInt(abLlAtt)
				numTl = section.getDataValueInt(abTlAtt)
				numRl = section.getDataValueInt(abRlAtt)
				numStlr = section.getDataValueInt(abStlrAtt)
				numStl = section.getDataValueInt(abStlAtt)
				numStr = section.getDataValueInt(abStrAtt)
				numSlr = section.getDataValueInt(abSlrAtt)
				lenRtb1= section.getDataValueDouble(abRtbl1Att)
				lenRtb2= section.getDataValueDouble(abRtbl2Att)
				lenRtb3= section.getDataValueDouble(abRtbl3Att)
				lenLtb1= section.getDataValueDouble(abLtbl1Att)
				lenLtb2= section.getDataValueDouble(abLtbl2Att)
				lenLtb3= section.getDataValueDouble(abLtbl3Att)
				lt = section.getDataValueInt(abLlBAtt)
				tt = section.getDataValueInt(abTlBAtt)
				rt = section.getDataValueInt(abRlBAtt)
				ut = section.getDataValueInt(abUtAtt)
				ntor = section.getDataValueInt(abNtorAtt)
				control = section.getDataValueInt(abControlAtt)
			else:
				numLl = section.getDataValueInt(baLlAtt)
				numTl = section.getDataValueInt(baTlAtt)
				numRl = section.getDataValueInt(baRlAtt)
				numStlr = section.getDataValueInt(baStlrAtt)
				numStl = section.getDataValueInt(baStlAtt)
				numStr = section.getDataValueInt(baStrAtt)
				numSlr = section.getDataValueInt(baSlrAtt)
				lenRtb1= section.getDataValueDouble(baRtbl1Att)
				lenRtb2= section.getDataValueDouble(baRtbl2Att)
				lenRtb3= section.getDataValueDouble(baRtbl3Att)
				lenLtb1= section.getDataValueDouble(baLtbl1Att)
				lenLtb2= section.getDataValueDouble(baLtbl2Att)
				lenLtb3= section.getDataValueDouble(baLtbl3Att)
				lt = section.getDataValueInt(baLlBAtt)
				tt = section.getDataValueInt(baTlBAtt)
				rt = section.getDataValueInt(baRlBAtt)
				ut = section.getDataValueInt(baUtAtt)
				ntor = section.getDataValueInt(baNtorAtt)
				control = section.getDataValueInt(baControlAtt)
		else:
			destination = section.getDestination()
			if destination != None and str(destination.getExternalId()) > "":
				if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
					numLl = section.getDataValueInt(baLlAtt)
					numTl = section.getDataValueInt(baTlAtt)
					numRl = section.getDataValueInt(baRlAtt)
					numStlr = section.getDataValueInt(baStlrAtt)
					numStl = section.getDataValueInt(baStlAtt)
					numStr = section.getDataValueInt(baStrAtt)
					numSlr = section.getDataValueInt(baSlrAtt)
					lenRtb1= section.getDataValueDouble(baRtbl1Att)
					lenRtb2= section.getDataValueDouble(baRtbl2Att)
					lenRtb3= section.getDataValueDouble(baRtbl3Att)
					lenLtb1= section.getDataValueDouble(baLtbl1Att)
					lenLtb2= section.getDataValueDouble(baLtbl2Att)
					lenLtb3= section.getDataValueDouble(baLtbl3Att)
					lt = section.getDataValueInt(baLlBAtt)
					tt = section.getDataValueInt(baTlBAtt)
					rt = section.getDataValueInt(baRlBAtt)
					ut = section.getDataValueInt(baUtAtt)
					ntor = section.getDataValueInt(baNtorAtt)
					control = section.getDataValueInt(baControlAtt)
				else:
					numLl = section.getDataValueInt(abLlAtt)
					numTl = section.getDataValueInt(abTlAtt)
					numRl = section.getDataValueInt(abRlAtt)
					numStlr = section.getDataValueInt(abStlrAtt)
					numStl = section.getDataValueInt(abStlAtt)
					numStr = section.getDataValueInt(abStrAtt)
					numSlr = section.getDataValueInt(abSlrAtt)
					lenRtb1= section.getDataValueDouble(abRtbl1Att)
					lenRtb2= section.getDataValueDouble(abRtbl2Att)
					lenRtb3= section.getDataValueDouble(abRtbl3Att)
					lenLtb1= section.getDataValueDouble(abLtbl1Att)
					lenLtb2= section.getDataValueDouble(abLtbl2Att)
					lenLtb3= section.getDataValueDouble(abLtbl3Att)
					lt = section.getDataValueInt(abLlBAtt)
					tt = section.getDataValueInt(abTlBAtt)
					rt = section.getDataValueInt(abRlBAtt)
					ut = section.getDataValueInt(abUtAtt)
					ntor = section.getDataValueInt(abNtorAtt)
					control = section.getDataValueInt(abControlAtt)

		error = False
		leftTbLengths = list()
		rightTbLengths = list()
		# if one of the new attributes has a value, apply the new logic
		if numStlr or numStl or numSlr or numStr or lenLtb1 or lenLtb2 or lenLtb3 or lenRtb1 or lenRtb2 or lenRtb3 or ut or ntor:

			# Check data consistency
			numBays = 0
			if lenLtb1 > 0:
				leftTbLengths.append(lenLtb1)
				numBays += 1
			if lenLtb2 > 0:
				leftTbLengths.append(lenLtb2)
				numBays += 1
			if lenLtb3 > 0:
				leftTbLengths.append(lenLtb3)
				numBays += 1
			if lenRtb1 > 0:
				rightTbLengths.append(lenRtb1)
				numBays += 1
			if lenRtb2 > 0:
				rightTbLengths.append(lenRtb2)
				numBays += 1
			if lenRtb3 > 0:
				rightTbLengths.append(lenRtb3)
				numBays += 1
			if numLl + numStlr + numStl + numSlr + numTl + numStr + numRl != section.getNbLanesAtPos(section.length2D()) + numBays:
				error = True
				print "Error at approach %s to node %s (new data): mid lanes %i, bays %i; lanes left %i, shared through-left-right %i, shared through-left %i, shared left-right %i, through %i, shared through-right %i, right %i" % (section.getExternalId(), toNodeExtId, section.getNbLanesAtPos(section.length2D()), numBays, numLl, numStlr, numStl, numSlr, numTl, numStr, numRl)

		else: # Apply the old logic

			if numLl + numTl + numRl > 0: # otherwise it is a join or a single lane with STLR, which is already correct

				# Check data consistency
				if (numLl == 8 and lt <> 0) or (numLl <> 8 and numLl <> 0 and lt == 0) or (numTl == 8 and tt <> 0) or (numTl <> 8 and numTl <> 0 and tt == 0) or (numRl == 8 and rt <> 0) or (numRl <> 8 and numRl <> 0 and rt == 0):
					error = True
					print "Error at approach %s to node %s (old data): lanes left %i, left turn to %i; lanes through %i, through turn %i; lanes right %i, right turn %i" % (section.getExternalId(), toNodeExtId, numLl, lt, numTl, tt, numRl, rt)
				else:

					# Determine shared
					(numLl, numTl, numRl, numStl, numStr, numSlr, numStlr) = convertNumLanes(numLl, numTl, numRl)

					# Determine bays
					addLanes = numLl + numStlr + numStl + numSlr + numTl + numStr + numRl - section.getNbLanesAtPos(section.length2D())
					for i in range(addLanes):
						if i % 2 == 0:
							leftTbLengths.append(defaultBayLength * ((i / 2) + 1))
						else:
							rightTbLengths.append(defaultBayLength * ((i / 2) + 1))

		if not error and numLl + numStlr + numStl + numSlr + numTl + numStr + numRl > 0:

			# Add pocket lanes
			tbLengths = list()
			for lenTb in leftTbLengths:
				if lenTb not in tbLengths:
					tbLengths.append(lenTb)
			for lenTb in rightTbLengths:
				if lenTb not in tbLengths:
					tbLengths.append(lenTb)
			for lenTb in sorted(tbLengths, reverse = True):
				if lenTb in leftTbLengths:
					(error, section) = addTurningBay(section, "left", lenTb * ftToM)
				if lenTb in rightTbLengths:
					(error, section) = addTurningBay(section, "right", lenTb * ftToM)
			if not error:
				# Assign turns to lanes
				currLane = 0
				leftTurn = getTurn(section, lt)
				#if leftTurn == None:
				#	leftTurn = getLeftTurn(section)
				if leftTurn != None:
					if numLl + numStlr + numStl + numSlr > 0:
						leftTurn.setOriginLanes(currLane, currLane + numLl + numStlr + numStl + numSlr - 1)
						leftTurn.updatePath(True)
						leftTurn.curve()
						if control == 2 or control == 3: # all-way or two-way stop
							leftTurn.setWarningIndicator(GKTurning.eStop)
						elif control == 1: # signalised
							leftTurn.setWarningIndicator(GKTurning.eGiveway) # in case it is permitted instead of protected
					else: # banned left
						#print "Error at approach %s to node %s: left turn exists, but no turn lanes specified" % (section.getExternalId(), toNodeExtId)
						cmd = leftTurn.getDelCmd()
						model.getCommander().addCommand(cmd)
				else:
					if numLl + numStlr + numStl + numSlr > 0:
						print "Warning at approach %s to node %s: left turn lanes specified, but the turn doesn't exist" % (section.getExternalId(), toNodeExtId)
				currLane += numLl
				throughTurn = getTurn(section, tt)
				#if throughTurn == None:
				#	throughTurn = getThroughTurn(section)
				if throughTurn != None:
					if numTl + numStlr + numStl + numStr > 0:
						throughTurn.setOriginLanes(currLane, currLane + numTl + numStlr + numStl + numStr - 1)
						throughTurn.updatePath(True)
						throughTurn.curve()
						if control == 2 or control == 3: # all-way or two-way stop
							throughTurn.setWarningIndicator(GKTurning.eStop)
					else: # banned through
						#print "Error at approach %s to node %s: through turn exists, but no turn lanes specified" % (section.getExternalId(), toNodeExtId)
						cmd = throughTurn.getDelCmd()
						model.getCommander().addCommand(cmd)
				else:
					if numTl + numStlr + numStl + numStr > 0:
						print "Warning at approach %s to node %s: through lanes specified, but the turn doesn't exist" % (section.getExternalId(), toNodeExtId)
				currLane += numStl + numTl
				rightTurn = getTurn(section, rt)
				#if rightTurn == None:
				#	rightTurn = getRightTurn(section)
				if rightTurn != None:
					if numRl + numStlr + numStr + numSlr > 0:
						rightTurn.setOriginLanes(currLane, currLane + numRl + numStlr + numStr + numSlr - 1)
						rightTurn.updatePath(True)
						rightTurn.curve()
						if control == 2 or control == 3: # all-way or two-way stop
							rightTurn.setWarningIndicator(GKTurning.eStop)
						elif control == 1 and not ntor: # Signalised intersection without no turn on red
							rightTurn.setWarningIndicator(GKTurning.eRTOR)
					else: # banned right
						#print "Error at approach %s to node %s: right turn exists, but no turn lanes specified" % (section.getExternalId(), toNodeExtId)
						cmd = rightTurn.getDelCmd()
						model.getCommander().addCommand(cmd)
				else:
					if numRl + numStlr + numStr + numSlr > 0:
						print "Warning at approach %s to node %s: right turn lanes specified, but the turn doesn't exist" % (section.getExternalId(), toNodeExtId)
				if ut:
					toSection = getOppositeSection(section)
					if toSection != None:
						uTurn = GKSystem.getSystem().newObject("GKTurning", model)
						uTurn.setConnection(section, toSection)
						uTurn.setOriginLanes(0, 0)
						uTurn.setDestinationLanes(toSection.getLanesAtPos(0)[0], toSection.getLanesAtPos(0)[0])
						uTurn.updatePath(True)
						uTurn.curve()
						section.getDestination().addTurning(uTurn, True)
		section.increaseTick()
		step += 1
		cancelled = task.stepTask(step)
task.end()
GKGUISystem.getGUISystem().getActiveGui().invalidateViews()

print "Intersection approaches configured!"