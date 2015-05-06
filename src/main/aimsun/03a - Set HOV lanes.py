print "Setting HOV lanes..."

hov2 = model.getCatalog().find(136477)
hov3 = model.getCatalog().find(136478)
toll = model.getCatalog().find(136488)

sectionType = model.getType("GKSection")
hovAtt = model.getColumn("GKSection::IHOV")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	lane = section.getDataValueInt(hovAtt)
	if lane == 2:
		for lane in section.getLanes():
			lane.setLaneType(hov2)
	elif lane == 3:
		for lane in section.getLanes():
			lane.setLaneType(hov3)
	elif lane == 4:
		for lane in section.getLanes():
			lane.setLaneType(toll)

print "HOV lanes set!"
