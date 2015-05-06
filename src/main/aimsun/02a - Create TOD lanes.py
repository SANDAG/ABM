print "Creating TOD and auxiliary lanes..."

aNodeAtt = model.getColumn("GKSection::AN")
ablnoAtt = model.getColumn("GKSection::ABLNO")
ablnaAtt = model.getColumn("GKSection::ABLNA")
ablnpAtt = model.getColumn("GKSection::ABLNP")
balnoAtt = model.getColumn("GKSection::BALNO")
balnaAtt = model.getColumn("GKSection::BALNA")
balnpAtt = model.getColumn("GKSection::BALNP")
abauAtt = model.getColumn("GKSection::ABAU")
baauAtt = model.getColumn("GKSection::BAAU")



sectionType = model.getType("GKSection")

for section in model.getCatalog().getObjectsByType(sectionType).itervalues():

	maxlanes = 0

	origin = section.getOrigin()

	if origin != None and str(origin.getExternalId()) > "":

		if section.getDataValueInt(aNodeAtt) == int(str(origin.getExternalId())):

			lna = section.getDataValueInt(ablnaAtt)

			lno = section.getDataValueInt(ablnoAtt)

			lnp = section.getDataValueInt(ablnpAtt)
			au = section.getDataValueInt(abauAtt)

		else:

			lna = section.getDataValueInt(balnaAtt)

			lno = section.getDataValueInt(balnoAtt)

			lnp = section.getDataValueInt(balnpAtt)
			au = section.getDataValueInt(baauAtt)

	else:

		destination = section.getDestination()

		if destination != None and str(destination.getExternalId()) > "":

			if section.getDataValueInt(aNodeAtt) == int(str(destination.getExternalId())):

				lna = section.getDataValueInt(balnaAtt)

				lno = section.getDataValueInt(balnoAtt)

				lnp = section.getDataValueInt(balnpAtt)
				au = section.getDataValueInt(baauAtt)

			else:

				lna = section.getDataValueInt(ablnaAtt)

				lno = section.getDataValueInt(ablnoAtt)

				lnp = section.getDataValueInt(ablnpAtt)
				au = section.getDataValueInt(abauAtt)

	if lna == 9:

		lna = 0

	if lno == 9:

		lno = 0

	if lnp == 9:

		lnp = 0

	maxlanes = max(lna, lno, lnp) + au

	if maxlanes != section.getNbFullLanes():

		#print "Section %i (%s): lanes %i, max %i" % (section.getId(), section.getExternalId(), section.getNbFullLanes(), maxlanes)

		cmd = GKSectionChangeNbLanesCmd()

		cmd.setData(section, maxlanes)

		model.getCommander().addCommand(cmd)



print "TOD and auxiliary lanes created!"