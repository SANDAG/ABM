print "Setting PM Peak capacity..."

sectionType = model.getType("GKSection")
pmABCapacityAtt = model.getColumn("GKSection::ABCHP") # hourly capacity
pmBACapacityAtt = model.getColumn("GKSection::BACHP") # hourly capacity
pmABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXP") # 3-hour capacity
pmBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXP") # 3-hour capacity
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	capacity = 0.0
	intersectionCapacity = 0.0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueDouble(pmABCapacityAtt)
			intersectionCapacity = section.getDataValueDouble(pmABIntersectionCapacityAtt)
		else:
			capacity = section.getDataValueDouble(pmBACapacityAtt)
			intersectionCapacity = section.getDataValueDouble(pmBAIntersectionCapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueDouble(pmBACapacityAtt)
				intersectionCapacity = section.getDataValueDouble(pmBAIntersectionCapacityAtt)
			else:
				capacity = section.getDataValueDouble(pmABCapacityAtt)
				intersectionCapacity = section.getDataValueDouble(pmABIntersectionCapacityAtt)

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
	section.setUserDefinedCost2(intersectionCapacity / 3.0) # convert to hourly capacity
	section.setUserDefinedCost3(intersectionFfTime)

print "PM Peak capacity set!"