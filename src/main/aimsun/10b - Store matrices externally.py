folder = str(GKSystem.getSystem().convertVariablePath(preferences.getValue("CT-RAMP:ProjectFolder"), model)) + "/matrices"

print "Storing matrices externally..."

if target != None:
	matrices = target.getODMatrices()
	task = GKSystem.getSystem().createTask(model)
	task.setName("Store matrices externally")
	task.setTotalSteps(len(matrices))
	step = 0
	cancelled = False
	task.start()
	for matrix in matrices:
		if matrix.getStoreType() == GKODMatrix.eInternal and not cancelled:
			matrix.setEnableStore(True)
			matrix.setStoreType(GKODMatrix.eAsciiFile)
			matrix.setExtraLocationData( ",@AimsunBinary" )
			filePath = folder + "/" + str(target.getName()) + "/" + str(matrix.getName()) + ".bin"
			matrix.setLocation(filePath)
			matrix.storeExternalMatrix()
		step += 1
		cancelled = task.stepTask(step)
	task.end()

print "Matrices stored externally!"