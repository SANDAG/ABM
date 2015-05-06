print "Setting Off-peak capacity..."

sectionType = model.getType("GKSection")
opABCapacityAtt = model.getColumn("GKSection::ABCHO") # hourly capacity
opBACapacityAtt = model.getColumn("GKSection::BACHO") # hourly capacity
opABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXO") # 18-hour capacity
opBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXO") # 18-hour capacity
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	capacity = 0.0
	intersectionCapacity = 0.0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueDouble(opABCapacityAtt)
			intersectionCapacity = section.getDataValueDouble(opABIntersectionCapacityAtt)
		else:
			capacity = section.getDataValueDouble(opBACapacityAtt)
			intersectionCapacity = section.getDataValueDouble(opBAIntersectionCapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueDouble(opBACapacityAtt)
				intersectionCapacity = section.getDataValueDouble(opBAIntersectionCapacityAtt)
			else:
				capacity = section.getDataValueDouble(opABCapacityAtt)
				intersectionCapacity = section.getDataValueDouble(opABIntersectionCapacityAtt)

	intersectionFfTime = 0.0
	node = section.getDestination()
	if node != None:
		maxTurnLength = 0.0
		turns = node.getFromTurnings(section)
		for turn in turns:
			turnLength = turn.length3D()
			if turnLength > maxTurnLength:
				maxTurnLength = turnLength
				intersectionFfTime = 60.0 * (turnLength / 1000.0) / turn.getSpeed()

	section.setCapacity(capacity)
	section.setUserDefinedCost2(intersectionCapacity / 18.0) # convert to hourly capacity
	section.setUserDefinedCost3(intersectionFfTime)

print "Off-peak capacity set!"