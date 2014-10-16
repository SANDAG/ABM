sectionType = model.getType("GKSection")
pmABCapacityAtt = model.getColumn("GKSection::ABCHP")
pmBACapacityAtt = model.getColumn("GKSection::BACHP")
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	origin = section.getOrigin()
	if origin != None:
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			capacity = section.getDataValueInt(pmABCapacityAtt)
		else:
			capacity = section.getDataValueInt(pmBACapacityAtt)
	else:
		destination = section.getDestination()
		if destination != None:
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				capacity = section.getDataValueInt(pmBACapacityAtt)
			else:
				capacity = section.getDataValueInt(pmABCapacityAtt)
	section.setCapacity(capacity)
print "Done!"