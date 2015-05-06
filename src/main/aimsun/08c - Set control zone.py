print "Setting control zone..."
quicnetAtt = model.getColumn("GKNode::QUICNETID")
zoneAtt = model.getColumn("GKNode::ControlZone")

nodeType = model.getType("GKNode")
i = 2
for node in model.getCatalog().getObjectsByType(nodeType).itervalues():
	if node.getDataValueString(quicnetAtt) > "":
		node.setDataValue(zoneAtt, QVariant(i))
		i += 1
print "Control zone set!"