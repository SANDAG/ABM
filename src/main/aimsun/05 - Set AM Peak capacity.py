print "Setting AM Peak capacity..."

sectionType = model.getType("GKSection")
amABCapacityAtt = model.getColumn("GKSection::ABCHA") # hourly capacity
amBACapacityAtt = model.getColumn("GKSection::BACHA") # hourly capacity
amABIntersectionCapacityAtt = model.getColumn("GKSection::ABCXA") # 3-hour capacity
amBAIntersectionCapacityAtt = model.getColumn("GKSection::BACXA") # 3-hour capacity
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	capacity = 0.0
	intersectionCapacity = 0.0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueDouble(amABCapacityAtt)
			intersectionCapacity = section.getDataValueDouble(amABIntersectionCapacityAtt)
		else:
			capacity = section.getDataValueInt(amBACapacityAtt)
			intersectionCapacity = section.getDataValueDouble(amBAIntersectionCapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueDouble(amBACapacityAtt)
				intersectionCapacity = section.getDataValueDouble(amBAIntersectionCapacityAtt)
			else:
				capacity = section.getDataValueDouble(amABCapacityAtt)
				intersectionCapacity = section.getDataValueDouble(amABIntersectionCapacityAtt)

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

print "AM Peak capacity set!"
