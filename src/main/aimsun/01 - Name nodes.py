print "Renaming nodes..."
cross1Att = model.getColumn("GKNode::XNM1")
cross2Att = model.getColumn("GKNode::XNM2")
nodeType = model.getType("GKNode")
for node in model.getCatalog().getObjectsByType(nodeType).itervalues():
	node.setName("%s @ %s" % (str(node.getDataValueString(cross1Att)), str(node.getDataValueString(cross2Att))))
print "Nodes renamed!"