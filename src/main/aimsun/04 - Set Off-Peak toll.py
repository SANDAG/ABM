print "Setting Off-peak tolls..."
sectionType = model.getType("GKSection")
amTollAtt = model.getColumn("GKSection::ITOLLO")
for section in model.getCatalog().getObjectsByType(sectionType).itervalues():
	toll = float(section.getDataValueInt(amTollAtt))/100
	section.setUserDefinedCost(toll)
print "Off-peak tolls set!"
