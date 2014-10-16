sectionType = model.getType("GKSection")
amABCapacityAtt = model.getColumn("GKSection::ABCHA")
amBACapacityAtt = model.getColumn("GKSection::BACHA")
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	origin = section.getOrigin()
	if origin != None:
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueInt(amABCapacityAtt)
		else:
			capacity = section.getDataValueInt(amBACapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None:
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueInt(amBACapacityAtt)
			else:
				capacity = section.getDataValueInt(amABCapacityAtt)
	section.setCapacity(capacity)
print "Done!"