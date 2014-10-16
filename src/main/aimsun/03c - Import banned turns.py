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

def mapTurns():
	turnMap = dict() # {(eid, eid):GKTurning}
	turnType = model.getType("GKTurning")
	for turn in model.getCatalog().getObjectsByType(turnType).itervalues():
		turnMap[(str(turn.getOrigin().getExternalId()), str(turn.getDestination().getExternalId()))] = turn
	return turnMap

def readBans(fileName):
	banList = list() # [(eid, eid)]
	file = open(fileName, "r")
	file.readline() # Skip header
	for line in file.readlines():
		fields = line.rstrip("\n").split(",")
		banList.append((fields[0].strip(), fields[1].strip()))
	file.close()
	return banList

def createTrafficCondition():
	conditionType = model.getType("GKTrafficCondition")
	condition = model.getCatalog().findByName("Turn bans", conditionType)
	if condition == None:
		condition = GKSystem.getSystem().newObject("GKTrafficCondition", model)
		condition.setName("Turn bans")
		condition.setActivationType(GKSimulationEvent.eAlways)
		applyToWarmUpAtt = model.getColumn("GKPolicy::applyOnWarmupAtt")
		condition.setDataValue(applyToWarmUpAtt, QVariant(True))
		folder = model.getCreateRootFolder().findFolder("GKModel::trafficConditions")
		if folder == None:
			folder = GKSystem.getSystem().createFolder(model.getCreateRootFolder(), "GKModel::trafficConditions")
		folder.append(condition)
	else:
		delCmds = list()
		conditions = condition.getChanges()
		if conditions != None:
			for action in conditions:
				condition.removeChange(action)
				delCmds.append(condition.getDelCmd())
		for delCmd in delCmds:
			model.getCommander().addCommand(delCmd)
	return condition

def createTurnClosure(turn):
	effectAtt = model.getColumn("GKTurningClosingChange::localEffect")
	closure = GKSystem.getSystem().newObject("GKTurningClosingChange", model)
	closure.setFromSection(turn.getOrigin())
	closure.setToSection(turn.getDestination())
	closure.setDataValue(effectAtt, QVariant(False))
	return closure

fileName = getPropertyValue("aimsun.gis.turnPenaltyFile")
deleteBannedTurns = bool(getPropertyValue("aimsun.gis.deleteBannedTurns"))
turnBanFunc = model.getCatalog().find(136503)
turnPenaltyAtt = model.getColumn("GKTurning::turningPenaltyAtt")
turnMap = mapTurns()
banList = readBans(fileName)
print "%i turn bans defined" % len(banList)
if deleteBannedTurns == False:
	trafficCondition = createTrafficCondition()
delCmds = list()
for ban in banList:
	if ban in turnMap:
		if deleteBannedTurns == True:
			delCmds.append(turnMap[ban].getDelCmd())
		else:
			trafficCondition.addChange(createTurnClosure(turnMap[ban]))
			turnMap[ban].setDataValueObject(turnPenaltyAtt, turnBanFunc)
	else:
		model.getLog().addWarning("Turn %s not found!" % str(ban))
for delCmd in delCmds:
	model.getCommander().addCommand(delCmd)
print "Done!"