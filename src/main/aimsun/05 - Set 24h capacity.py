sectionType = model.getType("GKSection")
amABCapacityAtt = model.getColumn("GKSection::ABCPA")
opABCapacityAtt = model.getColumn("GKSection::ABCPO")
pmABCapacityAtt = model.getColumn("GKSection::ABCPP")
amBACapacityAtt = model.getColumn("GKSection::BACPA")
opBACapacityAtt = model.getColumn("GKSection::BACPO")
pmBACapacityAtt = model.getColumn("GKSection::BACPP")
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	origin = section.getOrigin()
	if origin != None:
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueInt(amABCapacityAtt) + section.getDataValueInt(opABCapacityAtt) + section.getDataValueInt(pmABCapacityAtt)
		else:
			capacity = section.getDataValueInt(amBACapacityAtt) + section.getDataValueInt(opBACapacityAtt) + section.getDataValueInt(pmBACapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None:
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueInt(amBACapacityAtt) + section.getDataValueInt(opBACapacityAtt) + section.getDataValueInt(pmBACapacityAtt)
			else:
				capacity = section.getDataValueInt(amABCapacityAtt) + section.getDataValueInt(opABCapacityAtt) + section.getDataValueInt(pmABCapacityAtt)
	section.setCapacity(float(capacity)/24)
print "Done!"