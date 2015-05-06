nodeListFile = "S:/Projects/SANDAG DTA/Intersections/list.csv"
outputFolder = "S:/Projects/SANDAG DTA/Intersections"
zoom = 500
xRes = 1400
yRes = 1400

gui = GKGUISystem.getGUISystem().getActiveGui()
view = gui.getActiveView()
nodeType = model.getType("GKNode")
file = open(nodeListFile, "r")
lines = file.readlines()
task = GKSystem.getSystem().createTask(model)
task.setName("Screenshot creation")
task.setTotalSteps( len(lines) )
cancelled = False
step = 0
task.start()
for line in lines:
	if not cancelled:
		nodeId = line.rstrip("\n")
		node = model.getCatalog().findObjectByExternalId(nodeId, nodeType)
		if node != None and node.isA("GKNode"):
			position = node.getPosition()
			view.pan(position)
			view.zoom(zoom, position)
			view.saveSnapshot(outputFolder+"/"+str(nodeId), "png", GKBBox(0, 0, 0, 0), xRes, yRes, False)
		else:
			model.getLog().addError("Node %s not found!" % nodeId)
		step += 1
		cancelled = task.stepTask(step)
		if step >= 4000:
			break
task.end()
file.close()
print "Done!"