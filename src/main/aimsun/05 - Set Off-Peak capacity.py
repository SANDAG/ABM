sectionType = model.getType("GKSection")
opABCapacityAtt = model.getColumn("GKSection::ABCHO")
opBACapacityAtt = model.getColumn("GKSection::BACHO")
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	origin = section.getOrigin()
	if origin != None:
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueInt(opABCapacityAtt)
		else:
			capacity = section.getDataValueInt(opBACapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None:
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueInt(opBACapacityAtt)
			else:
				capacity = section.getDataValueInt(opABCapacityAtt)
	section.setCapacity(capacity)
print "Done!"