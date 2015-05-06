print "Setting 24h capacity..."

sectionType = model.getType("GKSection")
amABCapacityAtt = model.getColumn("GKSection::ABCPA") # 3-hour capacity
amBACapacityAtt = model.getColumn("GKSection::BACPA") # 3-hour capacity
opABCapacityAtt = model.getColumn("GKSection::ABCPO") # 18-hour capacity
opBACapacityAtt = model.getColumn("GKSection::BACPO") # 18-hour capacity
pmABCapacityAtt = model.getColumn("GKSection::ABCPP") # 3-hour capacity
pmBACapacityAtt = model.getColumn("GKSection::BACPP") # 3-hour capacity
amABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXA") # 3-hour capacity
amBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXA") # 3-hour capacity
opABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXO") # 18-hour capacity
opBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXO") # 18-hour capacity
pmABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXP") # 3-hour capacity
pmBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXP") # 3-hour capacity
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	capacity = 0.0
	intersectionCapacity = 0.0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueDouble(amABCapacityAtt) + section.getDataValueDouble(opABCapacityAtt) + section.getDataValueDouble(pmABCapacityAtt)
			intersectionCapacity = section.getDataValueDouble(amABIntersectionCapacityAtt) + section.getDataValueDouble(opABIntersectionCapacityAtt) + section.getDataValueDouble(pmABIntersectionCapacityAtt)
		else:
			capacity = section.getDataValueDouble(amBACapacityAtt) + section.getDataValueDouble(opBACapacityAtt) + section.getDataValueDouble(pmBACapacityAtt)
			intersectionCapacity = section.getDataValueDouble(amBAIntersectionCapacityAtt) + section.getDataValueDouble(opBAIntersectionCapacityAtt) + section.getDataValueDouble(pmBAIntersectionCapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueDouble(amBACapacityAtt) + section.getDataValueDouble(opBACapacityAtt) + section.getDataValueDouble(pmBACapacityAtt)
				intersectionCapacity = section.getDataValueDouble(amBAIntersectionCapacityAtt) + section.getDataValueDouble(opBAIntersectionCapacityAtt) + section.getDataValueDouble(pmBAIntersectionCapacityAtt)
			else:
				capacity = section.getDataValueDouble(amABCapacityAtt) + section.getDataValueDouble(opABCapacityAtt) + section.getDataValueDouble(pmABCapacityAtt)
				intersectionCapacity = section.getDataValueDouble(amABIntersectionCapacityAtt) + section.getDataValueDouble(opABIntersectionCapacityAtt) + section.getDataValueDouble(pmABIntersectionCapacityAtt)

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

	section.setCapacity(capacity / 24.0) # convert to hourly capacity
	section.setUserDefinedCost2(intersectionCapacity / 24.0) # convert to hourly capacity
	section.setUserDefinedCost3(intersectionFfTime)

print "24h capacity set!"
