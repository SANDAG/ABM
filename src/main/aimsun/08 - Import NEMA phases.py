import math

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

def mapNodes():
	map = dict() # {eid:GKNode}
	type = model.getType("GKNode")
	for obj in model.getCatalog().getObjectsByType(type).itervalues():
		map[str(obj.getExternalId())] = obj
	return map

def readFile(fileName):
	map = dict() # {node HWYCOV ID:{section HWYCOV ID:phase(s)}}
	file = open(fileName, "r")
	file.readline()
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split("\t")
		data = dict()
		for i in (13,33,53,73,93):
			if fields[i] > '""' and fields[i+4] > '""' and fields[i+4] != 'n/a':
				data[fields[i]] = fields[i+4].strip('""')
		map[fields[4]] = data
	file.close()
	return map

def getColumn():
	att = model.getColumn("GKSection::NEMAPhases")
	if att == None:
		att = model.getType("GKSection").addColumn("GKSection::NEMAPhases", "NEMA Phases", GKColumn.String, GKColumn.eExternal)
	return att

def mapSections():
	map = dict()
	idAtt = model.getColumn("GKSection::HWYCOV_")
	sectionType = model.getType("GKSection")
	for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
		id = section.getDataValueDouble(idAtt)
		if id > 0:
			map[id] = section
	return map

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

def getUTurn(node, section):
	for turn in node.getFromTurnings(section):
		if str(turn.getDestination().getExternalId()) == str(section.getExternalId()):
			return turn
	return None

def getNextAnticlockwiseSection(node, section):
	res = None
	minAngle = 1E400
	for candidate in node.getEntranceSections():
		if candidate.getId() != section.getId():
			angle = candidate.getExitAngle() - section.getExitAngle()
			if angle > 0 and angle < minAngle:
				res = candidate
				minAngle = angle
	if res == None:
		for candidate in node.getEntranceSections():
			if candidate.getId() != section.getId():
				angle = candidate.getExitAngle() - section.getExitAngle() + 2 * math.pi
				if angle > 0 and angle < minAngle:
					res = candidate
					minAngle = angle
	return (res, minAngle)

def completePhases(node, data):
	update = dict()
	for (sectionEid, phases) in data.iteritems():
		section = None
		for candidate in node.getEntranceSections():
			if str(candidate.getExternalId()) == sectionEid:
				section = candidate
		if section != None:
			nextEid = 0
			while nextEid not in data:
				(nextSection, angle) = getNextAnticlockwiseSection(node, section)
				nextEid = str(nextSection.getExternalId())
				if nextEid not in data:
					if angle < 3.0/4.0 * math.pi:
						if "2" in phases or "5" in phases:
							update[nextEid] = "8"
						if "4" in phases or "7" in phases:
							update[nextEid] = "2"
						if "6" in phases or "1" in phases:
							update[nextEid] = "4"
						if "8" in phases or "3" in phases:
							update[nextEid] = "6"
					elif angle >= 3.0/4.0 * math.pi and angle < 5.0/4.0 * math.pi:
						if "2" in phases or "5" in phases:
							update[nextEid] = "6"
						if "4" in phases or "7" in phases:
							update[nextEid] = "8"
						if "6" in phases or "1" in phases:
							update[nextEid] = "2"
						if "8" in phases or "3" in phases:
							update[nextEid] = "4"
					else:
						if "2" in phases or "5" in phases:
							update[nextEid] = "4"
						if "4" in phases or "7" in phases:
							update[nextEid] = "6"
						if "6" in phases or "1" in phases:
							update[nextEid] = "8"
						if "8" in phases or "3" in phases:
							update[nextEid] = "2"
					section = nextSection
					phases = update[nextEid]
	data.update(update)
	return data

fileName = getPropertyValue("aimsun.gis.nemaFile")
nodeMap = mapNodes()
phases = readFile(fileName)
att = getColumn()
for (eid, data) in phases.iteritems():
	if eid in nodeMap:
		node = nodeMap[eid]
		newSignals = dict()
		for signal in node.getSignals():
			node.removeSignal(signal)
		for section in node.getEntranceSections():
			section.setDataValue(att, QVariant(""))
			sectionEid = str(section.getExternalId())
			if sectionEid in data:
				section.setDataValue(att, QVariant(data[sectionEid]))
		data = completePhases(node, data)
		for section in node.getEntranceSections():
			found = False
			sectionEid = str(section.getExternalId())
			leftTurn = getLeftTurn(node, section)
			throughTurn = getThroughTurn(node, section)
			rightTurn = getRightTurn(node, section)
			uTurn = getUTurn(node, section)
			if sectionEid in data:
				found = True
				phases = data[sectionEid]
				if "2" in phases or "5" in phases:
					throughPhase = "2"
					leftPhase = "5"
				if "4" in phases or "7" in phases:
					throughPhase = "4"
					leftPhase = "7"
				if "6" in phases or "1" in phases:
					throughPhase = "6"
					leftPhase = "1"
				if "8" in phases or "3" in phases:
					throughPhase = "8"
					leftPhase = "3"
			else: # derive phase number from another approach
				if throughTurn != None:
					oppositeEid = str(throughTurn.getDestination().getExternalId())
					if oppositeEid in data:
						oppositePhases = data[oppositeEid]
						if "2" in oppositePhases or "5" in oppositePhases:
							throughPhase = "6"
							leftPhase = "1"
							found = True
						if "4" in oppositePhases or "7" in oppositePhases:
							throughPhase = "8"
							leftPhase = "3"
							found = True
						if "6" in oppositePhases or "1" in oppositePhases:
							throughPhase = "2"
							leftPhase = "5"
							found = True
						if "8" in oppositePhases or "3" in oppositePhases:
							throughPhase = "4"
							leftPhase = "7"
							found = True
				if not found and leftTurn != None:
					oppositeEid = str(leftTurn.getDestination().getExternalId())
					if oppositeEid in data:
						oppositePhases = data[oppositeEid]
						if "2" in oppositePhases or "5" in oppositePhases:
							throughPhase = "8"
							leftPhase = "3"
							found = True
						if "4" in oppositePhases or "7" in oppositePhases:
							throughPhase = "2"
							leftPhase = "5"
							found = True
						if "6" in oppositePhases or "1" in oppositePhases:
							throughPhase = "4"
							leftPhase = "7"
							found = True
						if "8" in oppositePhases or "3" in oppositePhases:
							throughPhase = "6"
							leftPhase = "1"
							found = True
				if not found and rightTurn != None:
					oppositeEid = str(rightTurn.getDestination().getExternalId())
					if oppositeEid in data:
						oppositePhases = data[oppositeEid]
						if "2" in oppositePhases or "5" in oppositePhases:
							throughPhase = "4"
							leftPhase = "7"
							found = True
						if "4" in oppositePhases or "7" in oppositePhases:
							throughPhase = "6"
							leftPhase = "1"
							found = True
						if "6" in oppositePhases or "1" in oppositePhases:
							throughPhase = "8"
							leftPhase = "3"
							found = True
						if "8" in oppositePhases or "3" in oppositePhases:
							throughPhase = "2"
							leftPhase = "5"
							found = True
			if found:
				usedTurns = list()
				if leftTurn != None:
					if leftPhase in newSignals:
						signal = newSignals[leftPhase]
					else:
						signal = GKSystem.getSystem().newObject("GKControlPlanSignal", model)
						signal.setName(leftPhase)
						node.addSignal(signal)
						newSignals[leftPhase] = signal
					signal.addTurning(leftTurn)
					usedTurns.append(leftTurn.getId())
					if uTurn != None:
						signal.addTurning(uTurn)
						usedTurns.append(uTurn.getId())
				if throughTurn != None or rightTurn != None:
					if throughPhase in newSignals:
						signal = newSignals[throughPhase]
					else:
						signal = GKSystem.getSystem().newObject("GKControlPlanSignal", model)
						signal.setName(throughPhase)
						node.addSignal(signal)
						newSignals[throughPhase] = signal
					if throughTurn != None:
						signal.addTurning(throughTurn)
						usedTurns.append(throughTurn.getId())
					if rightTurn != None:
						signal.addTurning(rightTurn)
						usedTurns.append(rightTurn.getId())
				for turn in node.getFromTurnings(section):
					if turn.getId() not in usedTurns:
						model.getLog().addError("No NEMA phase for turn %i at approach %s to node %s!" % (turn.getId(), sectionEid, eid))
			else:
				model.getLog().addError("No NEMA phase for approach %s to node %s!" % (sectionEid, eid))
	else:
		model.getLog().addError("Node %s not found!" % eid)
print "Done! Import NEMA phases"