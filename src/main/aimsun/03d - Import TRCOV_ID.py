import sys
if not "C:/Python27/Lib/site-packages" in sys.path:
	sys.path.append("C:/Python27/Lib/site-packages")
import dbf

def getPropertyValue(propertyKey):
	value = ""
	preferences = model.getPreferences()
	projectFolder = str(GKSystem.getSystem().convertVariablePath(preferences.getValue("CT-RAMP:ProjectFolder"), model))
	filePath = "%s/conf/sandag_abm.properties" % projectFolder
	file = open(filePath, "r")
	for line in file.readlines():
		if not line[0] == "#":
			tokens = line.rstrip("\n").split("=")
			if tokens[0].strip() == propertyKey:
				value = tokens[1].strip().replace("%project.folder%", projectFolder)
	file.close()
	return value

def readFile(fileName):
	map = dict() # {hwycovId:trcovId}
	table = dbf.Table(fileName)
	table.open()
	for record in table:
		if record["hnode"] != 0:
			map[record["hnode"]] = record["trcov_id"]
	table.close()
	return map

fileName = getPropertyValue("aimsun.gis.tcovFile")
tags = readFile(fileName)
type = model.getType("GKNode")
att = type.addColumn("GKNode::TRCOV_ID", "TRCOV_ID", GKColumn.Int, GKColumn.eExternal)
for node in model.getCatalog().getObjectsByType(type).itervalues():
	if int(str(node.getExternalId())) in tags:
		node.setDataValue(att, QVariant(tags[int(str(node.getExternalId()))]))

print "Importing TRCOV ID..."

type = model.getType("GKSection")
aNodeAtt = model.getColumn("GKSection::AN")
bNodeAtt = model.getColumn("GKSection::BN")
aatt = type.addColumn("GKSection::AN_TRCOV_ID", "AN_TRCOV_ID", GKColumn.Int, GKColumn.eExternal)
batt = type.addColumn("GKSection::BN_TRCOV_ID", "BN_TRCOV_ID", GKColumn.Int, GKColumn.eExternal)
for section in model.getCatalog().getObjectsByType(type).itervalues():
	if section.getDataValueInt(aNodeAtt) in tags:
		section.setDataValue(aatt, QVariant(tags[section.getDataValueInt(aNodeAtt)]))
	if section.getDataValueInt(bNodeAtt) in tags:
		section.setDataValue(batt, QVariant(tags[section.getDataValueInt(bNodeAtt)]))

print "TRCOV ID imported!"
