print "Creating TOD lanes..."

aNodeAtt = model.getColumn("GKSection::AN")
ablnoAtt = model.getColumn("GKSection::ABLNO")
ablnaAtt = model.getColumn("GKSection::ABLNA")
ablnpAtt = model.getColumn("GKSection::ABLNP")
balnoAtt = model.getColumn("GKSection::BALNO")
balnaAtt = model.getColumn("GKSection::BALNA")
balnpAtt = model.getColumn("GKSection::BALNP")

sectionType = model.getType("GKSection")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	maxlanes = 0
	origin = section.getOrigin()
	if origin != None and str(origin.getExternalId()) > "":
		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):
			maxlanes = max(section.getDataValueInt(ablnoAtt), section.getDataValueInt(ablnaAtt), section.getDataValueInt(ablnpAtt))
		else:
			maxlanes = max(section.getDataValueInt(balnoAtt), section.getDataValueInt(balnaAtt), section.getDataValueInt(balnpAtt))
	else:
		destination = section.getDestination()
		if destination != None and str(destination.getExternalId()) > "":
			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):
				maxlanes = max(section.getDataValueInt(balnoAtt), section.getDataValueInt(balnaAtt), section.getDataValueInt(balnpAtt))
			else:
				maxlanes = max(section.getDataValueInt(ablnoAtt), section.getDataValueInt(ablnaAtt), section.getDataValueInt(ablnpAtt))
	if maxlanes > section.getNbFullLanes():
		#print "Section %i (%s): lanes %i, max %i" % (section.getId(), section.getExternalId(), section.getNbFullLanes(), maxlanes)
		cmd = GKSectionChangeNbLanesCmd()
		cmd.setData(section, len(section.getLanes())+(maxlanes - section.getNbFullLanes()))
		model.getCommander().addCommand(cmd)

print "TOD lanes created!"