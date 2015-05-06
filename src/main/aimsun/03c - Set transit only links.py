print "Setting transit-only links..."
transitOnly = model.getCatalog().find(136550)
sectionType = model.getType("GKSection")
att = model.getColumn("GKSection::IFC")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	type = section.getDataValueInt(att)
	if type == 12:
		for lane in section.getLanes():
			lane.setLaneType(transitOnly)
print "Transit-only links set!"
