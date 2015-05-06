print "Setting turn prohibitions..."

hhdtExcluded = model.getCatalog().find(136539)
mhdtHhdtExcluded = model.getCatalog().find(136543)
lhdtMhdtHhdtExcluded = model.getCatalog().find(136541)
hhdtOnly = model.getCatalog().find(136540)
mhdtHhdtOnly = model.getCatalog().find(136544)
lhdtMhdtHhdtOnly = model.getCatalog().find(136542)

sectionType = model.getType("GKSection")
truckAtt = model.getColumn("GKSection::ITRUCK")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	lane = section.getDataValueInt(truckAtt)
	if lane == 2:
		for lane in section.getLanes():
			lane.setLaneType(hhdtExcluded)
	elif lane == 3:
		for lane in section.getLanes():
			lane.setLaneType(mhdtHhdtExcluded)
	elif lane == 4:
		for lane in section.getLanes():
			lane.setLaneType(lhdtMhdtHhdtExcluded)
	elif lane == 5:
		for lane in section.getLanes():
			lane.setLaneType(hhdtOnly)
	elif lane == 6:
		for lane in section.getLanes():
			lane.setLaneType(mhdtHhdtOnly)
	elif lane == 7:
		for lane in section.getLanes():
			lane.setLaneType(lhdtMhdtHhdtOnly)

print "Turn prohibitions set!"
