print "Creating stop signs..."
sectionType = model.getType("GKSection")
abControlAtt = model.getColumn("GKSection::ABCNT")
baControlAtt = model.getColumn("GKSection::BACNT")
aNodeAtt = model.getColumn("GKSection::AN")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	control = 0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			control = section.getDataValueInt(abControlAtt)
		else:
			control = section.getDataValueInt(baControlAtt)
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				control = section.getDataValueInt(baControlAtt)
			else:
				control = section.getDataValueInt(abControlAtt)
	if control == 2 or control == 3: # all-way or two-way stop
		destination = section.getDestination()
		if destination != None:
			turns = destination.getFromTurnings(section)
			for turn in turns:
				turn.setWarningIndicator(GKTurning.eStop)
		section.increaseTick()
GKGUISystem.getGUISystem().getActiveGui().invalidateViews()
print "Stop signs created!"